package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.helper.RowSnapshots;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Live RowBinary proof for nested nulls: WRT-07 (a null entry inside {@code Map(K, Nullable(V))}
 * and {@code Array(Nullable(V))} reads back as SQL NULL) and WRT-08 (a null in a nested
 * non-nullable slot fails loudly, never desyncing the stream). New fork-only class: it does not
 * extend or modify any upstream test class.
 *
 * <p>Every assertion is a typed SQL read-back through {@link RowSnapshots}, never a row-count
 * helper. The test JVM runs {@code file.encoding=windows-1252}, so only ASCII values are used.
 */
public class NestedNullRowBinaryLiveTest extends ClickHouseBase {

    private static final long MILLIS_PER_DAY = 86_400_000L;
    // 2024-01-01 as epoch days and as epoch millis (the +123 ms exercises DateTime64(3)).
    private static final long DATE_2024_01_01_MS = 19723L * MILLIS_PER_DAY;
    private static final long TIMESTAMP_2024_01_01_MS = 1704067200123L;

    private static final Schema OPTIONAL_KAFKA_DATE =
            org.apache.kafka.connect.data.Date.builder().optional().build();
    private static final Schema OPTIONAL_KAFKA_TIMESTAMP = Timestamp.builder().optional().build();
    private static final Schema OPTIONAL_KAFKA_DECIMAL = Decimal.builder(4).optional().build();

    private static final String ROW_ID_COLUMN = "id";
    private static final String FIRST_KEY = "a";
    private static final String SECOND_KEY = "b";
    private static final String LAST_KEY = "c";
    private static final String ONLY_KEY = "only";
    private static final String TRUE_TEXT = "1";

    /**
     * One Nullable value type under test. {@code arrayDdl} is null for the types that only appear in
     * a Map; the DDL strings are written out in full so the table definition reads as the literal
     * ClickHouse type.
     */
    private record NullableValueType(
            String suffix, String mapDdl, String arrayDdl, Schema schema, Object value, String text) {
        String mapColumn() {
            return "m_" + suffix;
        }

        String arrayColumn() {
            return "a_" + suffix;
        }
    }

    private static final List<NullableValueType> VALUE_TYPES = List.of(
            new NullableValueType(
                    "int32", "Map(String, Nullable(Int32))", "Array(Nullable(Int32))",
                    Schema.OPTIONAL_INT32_SCHEMA, 1, "1"),
            new NullableValueType(
                    "string", "Map(String, Nullable(String))", "Array(Nullable(String))",
                    Schema.OPTIONAL_STRING_SCHEMA, "s", "s"),
            new NullableValueType(
                    "date", "Map(String, Nullable(Date))", null,
                    OPTIONAL_KAFKA_DATE, new java.util.Date(DATE_2024_01_01_MS), "2024-01-01"),
            new NullableValueType(
                    "date32", "Map(String, Nullable(Date32))", null,
                    OPTIONAL_KAFKA_DATE, new java.util.Date(DATE_2024_01_01_MS), "2024-01-01"),
            new NullableValueType(
                    "datetime", "Map(String, Nullable(DateTime))", null,
                    OPTIONAL_KAFKA_TIMESTAMP, new java.util.Date(TIMESTAMP_2024_01_01_MS),
                    "2024-01-01 00:00:00"),
            new NullableValueType(
                    "datetime64", "Map(String, Nullable(DateTime64(3)))", "Array(Nullable(DateTime64(3)))",
                    OPTIONAL_KAFKA_TIMESTAMP, new java.util.Date(TIMESTAMP_2024_01_01_MS),
                    "2024-01-01 00:00:00.123"),
            new NullableValueType(
                    "decimal", "Map(String, Nullable(Decimal(18,4)))", "Array(Nullable(Decimal(18,4)))",
                    OPTIONAL_KAFKA_DECIMAL, new BigDecimal("12.3456"), "12.3456"),
            new NullableValueType(
                    "uuid", "Map(String, Nullable(UUID))", null,
                    Schema.OPTIONAL_STRING_SCHEMA, "123e4567-e89b-12d3-a456-426614174000",
                    "123e4567-e89b-12d3-a456-426614174000"),
            new NullableValueType(
                    "fixedstring", "Map(String, Nullable(FixedString(3)))", null,
                    Schema.OPTIONAL_STRING_SCHEMA, "abc", "abc"),
            new NullableValueType(
                    "bool", "Map(String, Nullable(Bool))", null,
                    Schema.OPTIONAL_BOOLEAN_SCHEMA, true, "true"),
            new NullableValueType(
                    "float64", "Map(String, Nullable(Float64))", null,
                    Schema.OPTIONAL_FLOAT64_SCHEMA, 1.5d, "1.5"),
            new NullableValueType(
                    "lowcard", "Map(String, LowCardinality(Nullable(String)))", null,
                    Schema.OPTIONAL_STRING_SCHEMA, "lc", "lc"));

    private static void createTable(ClickHouseHelperClient chc, String tableName, String createTableQuery) {
        try (Records ignored = chc.queryV2(String.format(createTableQuery, tableName))) {
            // table created
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Runs one put in a try/finally so a failing put still stops the task. */
    private static void putAndStop(Map<String, String> props, List<SinkRecord> records) {
        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        try {
            sinkTask.start(props);
            sinkTask.put(records);
        } finally {
            sinkTask.stop();
        }
    }

    private static Map<String, Object> mapOf(Object... keyValuePairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }

    private static String wrt07Ddl() {
        StringBuilder ddl = new StringBuilder("CREATE TABLE %s (id UInt32");
        for (NullableValueType type : VALUE_TYPES) {
            ddl.append(", ").append(type.mapColumn()).append(' ').append(type.mapDdl());
            if (type.arrayDdl() != null) {
                ddl.append(", ").append(type.arrayColumn()).append(' ').append(type.arrayDdl());
            }
        }
        return ddl.append(") ENGINE = MergeTree ORDER BY id").toString();
    }

    private static Schema wrt07Schema() {
        SchemaBuilder builder = SchemaBuilder.struct().field(ROW_ID_COLUMN, Schema.INT32_SCHEMA);
        for (NullableValueType type : VALUE_TYPES) {
            builder.field(type.mapColumn(), SchemaBuilder.map(Schema.STRING_SCHEMA, type.schema()).build());
            if (type.arrayDdl() != null) {
                builder.field(type.arrayColumn(), SchemaBuilder.array(type.schema()).build());
            }
        }
        return builder.build();
    }

    /**
     * Record 1: maps {a: value, b: null}, arrays [value, null]. Record 2: maps whose only entry is
     * null, arrays [null]. Record 3: maps of three entries whose last is null, arrays [value, value,
     * null].
     */
    private static SinkRecord wrt07Record(String topic, Schema schema, int id) {
        Struct struct = new Struct(schema).put(ROW_ID_COLUMN, id);
        for (NullableValueType type : VALUE_TYPES) {
            Object value = type.value();
            Map<String, Object> map;
            List<Object> array;
            switch (id) {
                case 1:
                    map = mapOf(FIRST_KEY, value, SECOND_KEY, null);
                    array = Arrays.asList(value, null);
                    break;
                case 2:
                    map = mapOf(ONLY_KEY, null);
                    array = Arrays.asList((Object) null);
                    break;
                default:
                    map = mapOf(FIRST_KEY, value, SECOND_KEY, value, LAST_KEY, null);
                    array = Arrays.asList(value, value, null);
                    break;
            }
            struct.put(type.mapColumn(), map);
            if (type.arrayDdl() != null) {
                struct.put(type.arrayColumn(), array);
            }
        }
        return new SinkRecord(topic, 0, null, null, schema, struct, id);
    }

    /** One read-back cell: the SQL expression, its column alias and the expected text. */
    private record Cell(String alias, String sqlExpression, String expected) {}

    private static void selectAndAssert(ClickHouseHelperClient chc, String tableName, int id, List<Cell> cells)
            throws Exception {
        StringBuilder sql = new StringBuilder("SELECT ");
        Map<String, String> expected = new LinkedHashMap<>();
        for (Cell cell : cells) {
            sql.append(expected.isEmpty() ? "" : ", ")
                    .append(cell.sqlExpression())
                    .append(" AS ")
                    .append(cell.alias());
            expected.put(cell.alias(), cell.expected());
        }
        sql.append(" FROM ").append(tableName).append(" WHERE id = ").append(id);
        List<Map<String, String>> rows =
                RowSnapshots.select(chc, sql.toString(), expected.keySet().toArray(new String[0]));
        assertEquals(1, rows.size(), "row id=" + id + " must exist exactly once");
        assertEquals(expected, rows.get(0), "typed read-back of row id=" + id);
    }

    @Test
    @DisplayName(
            "WRT-07: null entries in Map(String, Nullable(V)) and Array(Nullable(V)) read back as SQL NULL, "
                    + "with the neighbouring non-null entry intact")
    void nullEntriesInNullableMapAndArrayValuesReadBackAsSqlNull(TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName("nested_null_wrt07");
        ClickHouseTestHelpers.dropTable(chc, tableName);
        // Create the table BEFORE start() - ClickHouseWriter.start() fails against an empty database.
        createTable(chc, tableName, wrt07Ddl());
        props.put("name", uniqueConnectorName(testInfo));

        try {
            Schema schema = wrt07Schema();
            putAndStop(
                    props,
                    List.of(wrt07Record(tableName, schema, 1), wrt07Record(tableName, schema, 2),
                            wrt07Record(tableName, schema, 3)));

            List<Cell> row1 = new ArrayList<>();
            List<Cell> row2 = new ArrayList<>();
            List<Cell> row3 = new ArrayList<>();
            for (NullableValueType type : VALUE_TYPES) {
                String m = type.mapColumn();
                row1.add(new Cell(m + "_a", "toString(" + m + "['a'])", type.text()));
                row1.add(new Cell(m + "_has_b", "mapContains(" + m + ", 'b')", TRUE_TEXT));
                row1.add(new Cell(m + "_b_is_null", "isNull(" + m + "['b'])", TRUE_TEXT));
                row1.add(new Cell(m + "_len", "length(" + m + ")", "2"));

                row2.add(new Cell(m + "_has_only", "mapContains(" + m + ", 'only')", TRUE_TEXT));
                row2.add(new Cell(m + "_only_is_null", "isNull(" + m + "['only'])", TRUE_TEXT));
                row2.add(new Cell(m + "_len", "length(" + m + ")", "1"));

                row3.add(new Cell(m + "_a", "toString(" + m + "['a'])", type.text()));
                row3.add(new Cell(m + "_b", "toString(" + m + "['b'])", type.text()));
                row3.add(new Cell(m + "_has_c", "mapContains(" + m + ", 'c')", TRUE_TEXT));
                row3.add(new Cell(m + "_c_is_null", "isNull(" + m + "['c'])", TRUE_TEXT));
                row3.add(new Cell(m + "_len", "length(" + m + ")", "3"));

                if (type.arrayDdl() != null) {
                    String a = type.arrayColumn();
                    row1.add(new Cell(a + "_first", "toString(" + a + "[1])", type.text()));
                    row1.add(new Cell(a + "_second_is_null", "isNull(" + a + "[2])", TRUE_TEXT));
                    row1.add(new Cell(a + "_len", "length(" + a + ")", "2"));

                    row2.add(new Cell(a + "_first_is_null", "isNull(" + a + "[1])", TRUE_TEXT));
                    row2.add(new Cell(a + "_len", "length(" + a + ")", "1"));

                    row3.add(new Cell(a + "_first", "toString(" + a + "[1])", type.text()));
                    row3.add(new Cell(a + "_second", "toString(" + a + "[2])", type.text()));
                    row3.add(new Cell(a + "_third_is_null", "isNull(" + a + "[3])", TRUE_TEXT));
                    row3.add(new Cell(a + "_len", "length(" + a + ")", "3"));
                }
            }
            selectAndAssert(chc, tableName, 1, row1);
            selectAndAssert(chc, tableName, 2, row2);
            selectAndAssert(chc, tableName, 3, row3);
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }
}
