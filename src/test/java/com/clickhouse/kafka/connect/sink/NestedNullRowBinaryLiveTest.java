package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.dlq.ErrorReporter;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.helper.RowSnapshots;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.errors.DataException;
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
    private static final String SECRET_ELEMENT = "distinctive_secret_element";
    private static final String SECRET_MAP_KEY = "distinctive_secret_map_key";
    private static final String NESTED_NULL_TEXT = "null in non-nullable slot";
    private static final String ERRORS_TOLERANCE = "errors.tolerance";
    private static final String ERRORS_TOLERANCE_ALL = "all";
    private static final String NO_ROWS_QUERY = "SELECT id FROM %s";

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
        putAndStop(props, records, null);
    }

    /** The error reporter must be set before {@code start}. */
    private static void putAndStop(Map<String, String> props, List<SinkRecord> records, ErrorReporter reporter) {
        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        if (reporter != null) {
            sinkTask.setErrorReporter(reporter);
        }
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

    // ---- WRT-08: nested non-nullable nulls fail loudly, D-09 coercion, D-16, DLQ, #820 ----

    private static final Schema TAGS_SCHEMA = SchemaBuilder.struct()
            .field(ROW_ID_COLUMN, Schema.INT32_SCHEMA)
            .field("tags", SchemaBuilder.array(Schema.OPTIONAL_STRING_SCHEMA).build())
            .build();

    private static final String TAGS_COLUMNS = "tags Array(String)";

    private static SinkRecord tagsRecord(String topic, int id, List<String> tags) {
        return new SinkRecord(
                topic, 0, null, null, TAGS_SCHEMA,
                new Struct(TAGS_SCHEMA).put(ROW_ID_COLUMN, id).put("tags", tags), id);
    }

    /** Creates the table before start(), then runs {@code records} and returns how the put ended. */
    private RuntimeException putExpectingFailure(
            TestInfo testInfo, String topicPrefix, String columnsDdl,
            Function<String, List<SinkRecord>> recordsForTopic, Map<String, String> extraProps) throws Exception {
        Map<String, String> props = getBaseProps();
        props.putAll(extraProps);
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName(topicPrefix);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        createTable(
                chc, tableName, "CREATE TABLE %s (id UInt32, " + columnsDdl + ") ENGINE = MergeTree ORDER BY id");
        props.put("name", uniqueConnectorName(testInfo));
        try {
            RuntimeException ex = assertThrows(
                    RuntimeException.class, () -> putAndStop(props, recordsForTopic.apply(tableName)));
            assertEquals(
                    List.of(),
                    RowSnapshots.select(chc, String.format(NO_ROWS_QUERY, tableName), ROW_ID_COLUMN),
                    "a failed nested-null batch must leave no row behind");
            return ex;
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    /**
     * Through {@code put} the DataException is the root cause of the RuntimeException that {@code
     * Processing.doInsert} wraps around every failure, so walk the cause chain.
     */
    private static void assertNestedNullFailure(
            RuntimeException ex, String column, String slotKind, String... sentinels) {
        StringBuilder chain = new StringBuilder();
        boolean sawDataException = false;
        for (Throwable current = ex; current != null; current = current.getCause()) {
            sawDataException |= current instanceof DataException;
            if (current.getMessage() != null) {
                chain.append(current.getMessage()).append(" | ");
            }
        }
        String messages = chain.toString();
        assertTrue(sawDataException, "no DataException in the cause chain: " + messages);
        assertTrue(messages.contains("'" + column + "'"), messages);
        assertTrue(messages.contains(slotKind), messages);
        assertTrue(messages.contains(NESTED_NULL_TEXT), messages);
        for (String sentinel : sentinels) {
            assertFalse(messages.contains(sentinel), "record data leaked into the failure: " + messages);
        }
    }

    @Test
    @DisplayName("WRT-08: a null element in Array(String) fails with a DataException root cause, no data in the message and no row")
    void nullArrayElementFailsWithDataExceptionAndNoRow(TestInfo testInfo) throws Exception {
        RuntimeException ex = putExpectingFailure(
                testInfo, "nested_null_array", TAGS_COLUMNS,
                topic -> List.of(tagsRecord(topic, 1, Arrays.asList(SECRET_ELEMENT, null))), Map.of());

        assertNestedNullFailure(ex, "tags", "element", SECRET_ELEMENT);
    }

    @Test
    @DisplayName("WRT-08: a null value in Map(String, String) fails with a DataException root cause, without naming the key, and no row")
    void nullMapValueFailsWithDataExceptionAndNoRow(TestInfo testInfo) throws Exception {
        Schema schema = SchemaBuilder.struct()
                .field(ROW_ID_COLUMN, Schema.INT32_SCHEMA)
                .field("m", SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.OPTIONAL_STRING_SCHEMA).build())
                .build();
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(SECRET_MAP_KEY, null);

        RuntimeException ex = putExpectingFailure(
                testInfo, "nested_null_map", "m Map(String, String)",
                topic -> List.of(new SinkRecord(
                        topic, 0, null, null, schema,
                        new Struct(schema).put(ROW_ID_COLUMN, 1).put("m", entries), 1)),
                Map.of());

        assertNestedNullFailure(ex, "m", "map value", SECRET_MAP_KEY);
    }

    @Test
    @DisplayName("WRT-08: a null field in a non-nullable Tuple slot fails with a DataException root cause naming the field, and no row")
    void nullTupleFieldFailsWithDataExceptionAndNoRow(TestInfo testInfo) throws Exception {
        Schema tupleSchema = SchemaBuilder.struct()
                .field("a", Schema.OPTIONAL_INT32_SCHEMA)
                .field("b", Schema.OPTIONAL_STRING_SCHEMA)
                .build();
        Schema schema = SchemaBuilder.struct()
                .field(ROW_ID_COLUMN, Schema.INT32_SCHEMA)
                .field("c", tupleSchema)
                .build();

        RuntimeException ex = putExpectingFailure(
                testInfo, "nested_null_tuple", "c Tuple(a Int32, b String)",
                topic -> List.of(new SinkRecord(
                        topic, 0, null, null, schema,
                        new Struct(schema)
                                .put(ROW_ID_COLUMN, 1)
                                .put("c", new Struct(tupleSchema).put("a", 1).put("b", null)),
                        1)),
                Map.of());

        assertNestedNullFailure(ex, "c", "tuple field 'b'");
    }

    @Test
    @DisplayName("D-16: a null element in Array(Nullable(Tuple(a Int32))) now fails with a DataException, because Column reports the Tuple non-nullable")
    void nullElementInArrayOfNullableTupleFailsWithDataException(TestInfo testInfo) throws Exception {
        Schema elementSchema = SchemaBuilder.struct()
                .optional()
                .field("a", Schema.OPTIONAL_INT32_SCHEMA)
                .build();
        Schema schema = SchemaBuilder.struct()
                .field(ROW_ID_COLUMN, Schema.INT32_SCHEMA)
                .field("at", SchemaBuilder.array(elementSchema).build())
                .build();

        RuntimeException ex = putExpectingFailure(
                testInfo, "nested_null_array_tuple", "at Array(Nullable(Tuple(a Int32)))",
                topic -> List.of(new SinkRecord(
                        topic, 0, null, null, schema,
                        new Struct(schema)
                                .put(ROW_ID_COLUMN, 1)
                                .put("at", Arrays.asList(new Struct(elementSchema).put("a", 5), null)),
                        1)),
                Map.of());

        assertNestedNullFailure(ex, "at", "element");
    }

    private static List<SinkRecord> tagsBatchWithNestedNullInLastRecord(String topic) {
        return List.of(
                tagsRecord(topic, 1, List.of("x")),
                tagsRecord(topic, 2, List.of("y")),
                tagsRecord(topic, 3, Arrays.asList(SECRET_ELEMENT, null)));
    }

    @Test
    @DisplayName("WRT-08: a 3-record batch whose only nested null is in the last record writes zero rows")
    void nestedNullInLastRecordFailsTheWholeBatch(TestInfo testInfo) throws Exception {
        RuntimeException ex = putExpectingFailure(
                testInfo, "nested_null_batch", TAGS_COLUMNS,
                NestedNullRowBinaryLiveTest::tagsBatchWithNestedNullInLastRecord, Map.of());

        assertNestedNullFailure(ex, "tags", "element", SECRET_ELEMENT);
    }

    @Test
    @DisplayName("WRT-08: with errors.tolerance=all the whole 3-record batch goes to the DLQ and no row is written")
    void nestedNullUnderToleranceAllSendsWholeBatchToDlq(TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        props.put(ERRORS_TOLERANCE, ERRORS_TOLERANCE_ALL);
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName("nested_null_dlq");
        ClickHouseTestHelpers.dropTable(chc, tableName);
        createTable(
                chc, tableName,
                "CREATE TABLE %s (id UInt32, " + TAGS_COLUMNS + ") ENGINE = MergeTree ORDER BY id");
        props.put("name", uniqueConnectorName(testInfo));
        InMemoryDLQ dlq = new InMemoryDLQ();

        try {
            List<SinkRecord> batch = tagsBatchWithNestedNullInLastRecord(tableName);
            putAndStop(props, batch, dlq);

            assertEquals(batch.size(), dlq.size(), "the whole batch, not only the bad record, must reach the DLQ");
            assertEquals(
                    List.of(), RowSnapshots.select(chc, String.format(NO_ROWS_QUERY, tableName), ROW_ID_COLUMN));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    private static final String NESTED_CONTAINERS_COLUMNS =
            "aa Array(Array(Int32)), ma Map(String, Array(Int32)), am Array(Map(String, Int32))";

    private void assertNullContainersCoercedToEmpty(TestInfo testInfo, String topicPrefix, String extraColumns)
            throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName(topicPrefix);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        createTable(
                chc, tableName,
                "CREATE TABLE %s (id UInt32, " + NESTED_CONTAINERS_COLUMNS + extraColumns
                        + ") ENGINE = MergeTree ORDER BY id");
        props.put("name", uniqueConnectorName(testInfo));

        Schema innerArray = SchemaBuilder.array(Schema.OPTIONAL_INT32_SCHEMA).optional().build();
        Schema innerMap = SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.OPTIONAL_INT32_SCHEMA).optional().build();
        Schema schema = SchemaBuilder.struct()
                .field(ROW_ID_COLUMN, Schema.INT32_SCHEMA)
                .field("aa", SchemaBuilder.array(innerArray).build())
                .field("ma", SchemaBuilder.map(Schema.STRING_SCHEMA, innerArray).build())
                .field("am", SchemaBuilder.array(innerMap).build())
                .build();
        Map<String, Object> nullArrayValue = new LinkedHashMap<>();
        nullArrayValue.put("k", null);
        Map<String, Integer> presentMap = new LinkedHashMap<>();
        presentMap.put("a", 1);
        Struct struct = new Struct(schema)
                .put(ROW_ID_COLUMN, 1)
                .put("aa", Arrays.asList(List.of(1), null))
                .put("ma", nullArrayValue)
                .put("am", Arrays.asList(presentMap, null));

        try {
            putAndStop(props, List.of(new SinkRecord(tableName, 0, null, null, schema, struct, 1)));

            List<Map<String, String>> rows = RowSnapshots.select(
                    chc,
                    "SELECT toString(aa) AS aa, toString(ma) AS ma, toString(am) AS am FROM " + tableName,
                    "aa", "ma", "am");
            assertEquals(
                    List.of(Map.of("aa", "[[1],[]]", "ma", "{'k':[]}", "am", "[{'a':1},{}]")), rows);
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @Test
    @DisplayName("D-09: null inner containers are coerced to empty in a table without DEFAULT columns (RowBinary)")
    void nullContainersAreCoercedToEmptyWithoutDefaults(TestInfo testInfo) throws Exception {
        assertNullContainersCoercedToEmpty(testInfo, "nested_null_coerce_nodef", "");
    }

    @Test
    @DisplayName("D-09: null inner containers are coerced to empty in a table with a DEFAULT column (RowBinaryWithDefaults)")
    void nullContainersAreCoercedToEmptyWithDefaults(TestInfo testInfo) throws Exception {
        assertNullContainersCoercedToEmpty(testInfo, "nested_null_coerce_def", ", d Int32 DEFAULT 1");
    }

    @Test
    @DisplayName("#820: a top-level null Array and null Map in non-nullable columns still read back as [] and {}")
    void topLevelNullArrayAndMapStillReadBackEmpty(TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName("nested_null_top_level");
        ClickHouseTestHelpers.dropTable(chc, tableName);
        createTable(
                chc, tableName,
                "CREATE TABLE %s (id UInt32, arr Array(String), mp Map(String, String)) "
                        + "ENGINE = MergeTree ORDER BY id");
        props.put("name", uniqueConnectorName(testInfo));
        Schema schema = SchemaBuilder.struct()
                .field(ROW_ID_COLUMN, Schema.INT32_SCHEMA)
                .field("arr", SchemaBuilder.array(Schema.STRING_SCHEMA).optional().build())
                .field("mp", SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.STRING_SCHEMA).optional().build())
                .build();
        Struct struct = new Struct(schema).put(ROW_ID_COLUMN, 1).put("arr", null).put("mp", null);

        try {
            putAndStop(props, List.of(new SinkRecord(tableName, 0, null, null, schema, struct, 1)));

            List<Map<String, String>> rows = RowSnapshots.select(
                    chc, "SELECT toString(arr) AS arr, toString(mp) AS mp FROM " + tableName, "arr", "mp");
            assertEquals(List.of(Map.of("arr", "[]", "mp", "{}")), rows);
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }
}
