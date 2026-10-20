package com.clickhouse.kafka.connect.sink;

import static com.clickhouse.kafka.connect.sink.NullableRoundTrip.DEFAULT_VALUE_TEXT;
import static com.clickhouse.kafka.connect.sink.NullableRoundTrip.FALSE_TEXT;
import static com.clickhouse.kafka.connect.sink.NullableRoundTrip.TRUE_TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.NullableRoundTrip.Cell;
import com.clickhouse.kafka.connect.sink.NullableRoundTrip.Twin;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseFieldDescriptor;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullability;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * COLS-10 / CR-01 live round trips for Tuple elements declared as {@code SimpleAggregateFunction}.
 * Before the fix {@code DeclaredNullability.collect} read the nullability of the wrapper node
 * instead of the type it holds, flipped a nullable element to non-nullable, and the writer left out
 * its null marker, so the RowBinary row misaligned. Every assertion is a typed SQL read-back on both
 * twin tables: the plain table (RowBinary) and the table with a DEFAULT column
 * (RowBinaryWithDefaults). Values are synthetic integers; the class logs DESCRIBE metadata only.
 */
public class SimpleAggregateTupleRoundTripLiveTest extends ClickHouseBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleAggregateTupleRoundTripLiveTest.class);

    private static final String LOG_PREFIX = "SAF_TUPLE";
    private static final String TABLE_PREFIX = "saf_tuple_";
    private static final String CONNECTOR_NAME_PROP = "name";
    private static final String ROW_ID_COLUMN = "id";
    private static final String DEFAULT_COLUMN = "d";

    private static final String SUM_COLUMN = "c";
    private static final String ARRAY_COLUMN = "g";
    private static final String WRAPPED = "a";
    private static final String SIBLING = "b";
    private static final String SUM_WRAPPED_PATH = "c.a";
    private static final String SUM_SIBLING_PATH = "c.b";
    private static final String ARRAY_WRAPPED_PATH = "g.a";
    private static final String ARRAY_SIBLING_PATH = "g.b";

    static final String TUPLE_DDL = "c Tuple(a SimpleAggregateFunction(sum, Nullable(Int64)), b Nullable(Int32)), "
            + "g Tuple(a SimpleAggregateFunction(anyLast, Array(Nullable(Int32))), b Int32)";

    private static final String SUM_WRAPPED_SQL = "tupleElement(c, 'a')";
    private static final String SUM_SIBLING_SQL = "tupleElement(c, 'b')";
    private static final String ARRAY_WRAPPED_SQL = "tupleElement(g, 'a')";
    private static final String ARRAY_SIBLING_SQL = "tupleElement(g, 'b')";

    private static final long LARGE_INT64 = 9007199254740993L;

    private static Cell cell(String alias, String sqlExpression, String expected) {
        return new Cell(alias, sqlExpression, expected);
    }

    private static Cell nullCell(String alias, String sqlExpression, boolean expectedNull) {
        return cell(alias + "_is_null", "isNull(" + sqlExpression + ")", expectedNull ? TRUE_TEXT : FALSE_TEXT);
    }

    private static List<Cell> withDefaultColumn(List<Cell> cells, Twin twin) {
        if (!twin.hasDefaults()) {
            return cells;
        }
        List<Cell> withDefault = new ArrayList<>(cells);
        withDefault.add(cell(DEFAULT_COLUMN, DEFAULT_COLUMN, DEFAULT_VALUE_TEXT));
        return withDefault;
    }

    private static Struct sumTuple(Schema schema, Long wrapped, Integer sibling) {
        return new Struct(schema).put(WRAPPED, wrapped).put(SIBLING, sibling);
    }

    private static Struct arrayTuple(Schema schema, List<Integer> wrapped, int sibling) {
        return new Struct(schema).put(WRAPPED, wrapped).put(SIBLING, sibling);
    }

    @ParameterizedTest(name = "COLS-10 CR-01 SimpleAggregateFunction Tuple elements round-trip {0}")
    @EnumSource(Twin.class)
    void simpleAggregateFunctionTupleElementsRoundTrip(Twin twin, TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName(TABLE_PREFIX + twin);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        // Create the table BEFORE start() - ClickHouseWriter.start() fails against an empty database.
        NullableRoundTrip.createTable(chc, tableName, twin.ddl(TUPLE_DDL));
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        try {
            for (ClickHouseFieldDescriptor row : NullableRoundTrip.describeRows(chc, tableName)) {
                LOGGER.info(
                        "{} describe {} {} | {} | subcolumn={}",
                        LOG_PREFIX,
                        twin,
                        row.getName(),
                        row.getType(),
                        row.isSubcolumn());
                if (SUM_COLUMN.equals(row.getName()) || ARRAY_COLUMN.equals(row.getName())) {
                    assertTrue(
                            row.getType().contains(DeclaredNullability.ESCAPED_NEWLINE),
                            "premise: the non-nullable Tuple root reaches the pass in the multi-line form");
                }
            }

            Table table;
            try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
                table = chc.describeTable(chc.getDatabase(), tableName);
                assertEquals(List.of(), log.events("WARN"), "the declared-nullability pass must not fall back");
            }
            assertEquals(
                    twin.hasDefaults(),
                    table.hasDefaults(),
                    "twin precondition: DESCRIBE must report a default exactly on the DEFAULT twin");
            Map<String, Column> columns = table.getAllColumnsMap();
            assertFalse(columns.get(SUM_COLUMN).isNullable());
            assertTrue(columns.get(SUM_WRAPPED_PATH).isNullable(), "the wrapped Nullable(Int64) element");
            assertTrue(columns.get(SUM_SIBLING_PATH).isNullable());
            assertEquals(Type.ARRAY, columns.get(ARRAY_WRAPPED_PATH).getType());
            assertTrue(columns.get(ARRAY_WRAPPED_PATH).getArrayType().isNullable());
            assertFalse(columns.get(ARRAY_SIBLING_PATH).isNullable());

            Schema sumSchema = SchemaBuilder.struct()
                    .field(WRAPPED, Schema.OPTIONAL_INT64_SCHEMA)
                    .field(SIBLING, Schema.OPTIONAL_INT32_SCHEMA)
                    .optional()
                    .build();
            Schema arraySchema = SchemaBuilder.struct()
                    .field(WRAPPED, SchemaBuilder.array(Schema.OPTIONAL_INT32_SCHEMA).build())
                    .field(SIBLING, Schema.INT32_SCHEMA)
                    .build();
            Schema schema = SchemaBuilder.struct()
                    .field(ROW_ID_COLUMN, Schema.INT32_SCHEMA)
                    .field(SUM_COLUMN, sumSchema)
                    .field(ARRAY_COLUMN, arraySchema)
                    .build();
            List<SinkRecord> records = List.of(
                    record(tableName, schema, 1, sumTuple(sumSchema, LARGE_INT64, 7),
                            arrayTuple(arraySchema, Arrays.asList(1, null, 3), 9)),
                    record(tableName, schema, 2, sumTuple(sumSchema, null, 7),
                            arrayTuple(arraySchema, List.of(), 8)),
                    record(tableName, schema, 3, sumTuple(sumSchema, 5L, null),
                            arrayTuple(arraySchema, Arrays.asList((Integer) null), 7)));
            NullableRoundTrip.putAndStop(props, records);

            NullableRoundTrip.selectAndAssert(
                    chc,
                    tableName,
                    1,
                    withDefaultColumn(
                            List.of(
                                    cell("c_a", "toString(" + SUM_WRAPPED_SQL + ")", String.valueOf(LARGE_INT64)),
                                    nullCell("c_a", SUM_WRAPPED_SQL, false),
                                    cell("c_b", SUM_SIBLING_SQL, "7"),
                                    cell("g_a_len", "length(" + ARRAY_WRAPPED_SQL + ")", "3"),
                                    cell("g_a_1", ARRAY_WRAPPED_SQL + "[1]", "1"),
                                    nullCell("g_a_2", ARRAY_WRAPPED_SQL + "[2]", true),
                                    cell("g_a_3", ARRAY_WRAPPED_SQL + "[3]", "3"),
                                    cell("g_b", ARRAY_SIBLING_SQL, "9")),
                            twin));
            NullableRoundTrip.selectAndAssert(
                    chc,
                    tableName,
                    2,
                    withDefaultColumn(
                            List.of(
                                    nullCell("c_a", SUM_WRAPPED_SQL, true),
                                    nullCell("c_b", SUM_SIBLING_SQL, false),
                                    cell("c_b", SUM_SIBLING_SQL, "7"),
                                    cell("g_a_len", "length(" + ARRAY_WRAPPED_SQL + ")", "0"),
                                    cell("g_b", ARRAY_SIBLING_SQL, "8")),
                            twin));
            NullableRoundTrip.selectAndAssert(
                    chc,
                    tableName,
                    3,
                    withDefaultColumn(
                            List.of(
                                    cell("c_a", "toString(" + SUM_WRAPPED_SQL + ")", "5"),
                                    nullCell("c_a", SUM_WRAPPED_SQL, false),
                                    nullCell("c_b", SUM_SIBLING_SQL, true),
                                    cell("g_a_len", "length(" + ARRAY_WRAPPED_SQL + ")", "1"),
                                    nullCell("g_a_1", ARRAY_WRAPPED_SQL + "[1]", true),
                                    cell("g_b", ARRAY_SIBLING_SQL, "7")),
                            twin));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    private static SinkRecord record(
            String topic, Schema schema, int id, Struct sumValue, Struct arrayValue) {
        Struct value = new Struct(schema)
                .put(ROW_ID_COLUMN, id)
                .put(SUM_COLUMN, sumValue)
                .put(ARRAY_COLUMN, arrayValue);
        return new SinkRecord(topic, 0, null, null, schema, value, id);
    }
}
