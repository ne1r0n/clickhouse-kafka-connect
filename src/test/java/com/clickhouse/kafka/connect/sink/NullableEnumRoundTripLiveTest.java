package com.clickhouse.kafka.connect.sink;

import static com.clickhouse.kafka.connect.sink.NullableRoundTrip.DEFAULT_VALUE_TEXT;
import static com.clickhouse.kafka.connect.sink.NullableRoundTrip.FALSE_TEXT;
import static com.clickhouse.kafka.connect.sink.NullableRoundTrip.TRUE_TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.kafka.connect.sink.NullableRoundTrip.Cell;
import com.clickhouse.kafka.connect.sink.NullableRoundTrip.Twin;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * COLS-08 live round trips for {@code Nullable(Enum8)} and {@code Nullable(Enum16)} (backlog 999.1).
 * Before Phase 19 the Enum branch of {@code Column.extractColumn} dropped {@code Nullable}, so the
 * writer left out the null marker for these slots. Every assertion is a typed SQL read-back (label
 * and numeric code) on both twin tables: the plain table (RowBinary) and the table with a DEFAULT
 * column (RowBinaryWithDefaults). Uses {@link NullableRoundTrip}. Values are synthetic ASCII labels
 * and nothing here logs a record value.
 */
public class NullableEnumRoundTripLiveTest extends ClickHouseBase {

    private static final String ROW_ID_COLUMN = "id";
    private static final String ENUM_COLUMN = "ne";
    private static final String DEFAULT_COLUMN = "d";
    private static final String CONNECTOR_NAME_PROP = "name";
    private static final String TOP_LEVEL_TABLE_PREFIX = "ne_top_";

    private static final String WIDE_TABLE_PREFIX = "ne_wide_";
    private static final String OMITTED_TABLE_PREFIX = "ne_omit_";
    private static final String ENUM8_CODE_TYPE = "Int8";
    private static final String ENUM16_CODE_TYPE = "Int16";
    private static final String LABEL_SUFFIX = "_label";
    private static final String CODE_SUFFIX = "_code";
    private static final String NULL_SUFFIX = "_is_null";
    private static final String LENGTH_SUFFIX = "_len";

    static final String TOP_LEVEL_DDL = "ne Nullable(Enum8('a' = 1, 'b' = 2))";
    // Boundary codes: Enum8 -128 and 127, Enum16 -32768, 1000 (a two-byte little-endian code) and 32767.
    static final String WIDE_DDL = "e8 Nullable(Enum8('lo' = -128, 'a' = 1, 'b' = 2, 'hi' = 127)), "
            + "e16 Nullable(Enum16('lo' = -32768, 'a' = 1, 'k' = 1000, 'hi' = 32767)), "
            + "ae8 Array(Nullable(Enum8('a' = 1, 'b' = 2))), "
            + "ae16 Array(Nullable(Enum16('a' = 1, 'k' = 1000))), "
            + "me8 Map(String, Nullable(Enum8('a' = 1, 'b' = 2))), "
            + "me16 Map(String, Nullable(Enum16('a' = 1, 'k' = 1000)))";
    static final String OMITTED_DDL =
            "e8 Nullable(Enum8('a' = 1, 'b' = 2)), e16 Nullable(Enum16('a' = 1, 'k' = 1000))";

    private static Cell cell(String alias, String sqlExpression, String expected) {
        return new Cell(alias, sqlExpression, expected);
    }

    private static List<Cell> withDefaultColumn(List<Cell> cells, Twin twin) {
        if (!twin.hasDefaults()) {
            return cells;
        }
        List<Cell> withDefault = new ArrayList<>(cells);
        withDefault.add(cell(DEFAULT_COLUMN, DEFAULT_COLUMN, DEFAULT_VALUE_TEXT));
        return withDefault;
    }

    private static SinkRecord sinkRecord(String topic, Schema schema, long offset, Struct value) {
        return new SinkRecord(topic, 0, null, null, schema, value, offset);
    }

    private static Map<String, String> mapOf(String... keysAndValues) {
        Map<String, String> entries = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            entries.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return entries;
    }

    private static Cell nullCell(String alias, String sqlExpression, boolean expectedNull) {
        return cell(alias + NULL_SUFFIX, "isNull(" + sqlExpression + ")", expectedNull ? TRUE_TEXT : FALSE_TEXT);
    }

    private static Cell lengthCell(String alias, String column, int expected) {
        return cell(alias + LENGTH_SUFFIX, "length(" + column + ")", String.valueOf(expected));
    }

    /** The label (toString) and the numeric code (CAST to the Int type of the same width) of one slot. */
    private static List<Cell> valueCells(String alias, String sqlExpression, String codeType, String label, int code) {
        return List.of(
                cell(alias + LABEL_SUFFIX, "toString(" + sqlExpression + ")", label),
                cell(alias + CODE_SUFFIX, "CAST(" + sqlExpression + " AS Nullable(" + codeType + "))", String.valueOf(code)));
    }

    @SafeVarargs
    private static List<Cell> concat(List<Cell>... groups) {
        List<Cell> all = new ArrayList<>();
        for (List<Cell> group : groups) {
            all.addAll(group);
        }
        return all;
    }

    @ParameterizedTest(name = "COLS-08 top-level Nullable(Enum8) {0}")
    @EnumSource(Twin.class)
    void topLevelNullableEnum8RoundTrips(Twin twin, TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName(TOP_LEVEL_TABLE_PREFIX + twin);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        // Create the table BEFORE start() - ClickHouseWriter.start() fails against an empty database.
        NullableRoundTrip.createTable(chc, tableName, twin.ddl(TOP_LEVEL_DDL));
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        try {
            assertEquals(
                    twin.hasDefaults(),
                    chc.describeTable(chc.getDatabase(), tableName).hasDefaults(),
                    "twin precondition: DESCRIBE must report a default exactly on the DEFAULT twin");
            Schema schema = SchemaBuilder.struct()
                    .field(ROW_ID_COLUMN, Schema.INT32_SCHEMA)
                    .field(ENUM_COLUMN, Schema.OPTIONAL_STRING_SCHEMA)
                    .build();
            List<SinkRecord> records = List.of(
                    new SinkRecord(
                            tableName,
                            0,
                            null,
                            null,
                            schema,
                            new Struct(schema).put(ROW_ID_COLUMN, 1).put(ENUM_COLUMN, "b"),
                            1),
                    new SinkRecord(
                            tableName,
                            0,
                            null,
                            null,
                            schema,
                            new Struct(schema).put(ROW_ID_COLUMN, 2).put(ENUM_COLUMN, null),
                            2));
            NullableRoundTrip.putAndStop(props, records);
            NullableRoundTrip.selectAndAssert(
                    chc,
                    tableName,
                    1,
                    withDefaultColumn(
                            List.of(
                                    cell("ne_label", "toString(ne)", "b"),
                                    cell("ne_code", "CAST(ne AS Nullable(Int8))", "2"),
                                    cell("ne_is_null", "isNull(ne)", FALSE_TEXT)),
                            twin));
            NullableRoundTrip.selectAndAssert(
                    chc,
                    tableName,
                    2,
                    withDefaultColumn(List.of(cell("ne_is_null", "isNull(ne)", TRUE_TEXT)), twin));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @ParameterizedTest(name = "COLS-08 Nullable(Enum8|Enum16) top level, Array and Map round-trip {0}")
    @EnumSource(Twin.class)
    void nullableEnumsRoundTripAtTopLevelAndNested(Twin twin, TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName(WIDE_TABLE_PREFIX + twin);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        NullableRoundTrip.createTable(chc, tableName, twin.ddl(WIDE_DDL));
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        try {
            assertEquals(
                    twin.hasDefaults(),
                    chc.describeTable(chc.getDatabase(), tableName).hasDefaults(),
                    "twin precondition: DESCRIBE must report a default exactly on the DEFAULT twin");
            Schema elements = Schema.OPTIONAL_STRING_SCHEMA;
            Schema schema = SchemaBuilder.struct()
                    .field(ROW_ID_COLUMN, Schema.INT32_SCHEMA)
                    .field("e8", elements)
                    .field("e16", elements)
                    .field("ae8", SchemaBuilder.array(elements).build())
                    .field("ae16", SchemaBuilder.array(elements).build())
                    .field("me8", SchemaBuilder.map(Schema.STRING_SCHEMA, elements).build())
                    .field("me16", SchemaBuilder.map(Schema.STRING_SCHEMA, elements).build())
                    .build();
            List<SinkRecord> records = List.of(
                    sinkRecord(
                            tableName,
                            schema,
                            1,
                            new Struct(schema)
                                    .put(ROW_ID_COLUMN, 1)
                                    .put("e8", "lo")
                                    .put("e16", "hi")
                                    .put("ae8", Arrays.asList("a", null, "b"))
                                    .put("ae16", Arrays.asList("k", null, "a"))
                                    .put("me8", mapOf("a", "a", "b", null))
                                    .put("me16", mapOf("k", "k", "n", null))),
                    sinkRecord(
                            tableName,
                            schema,
                            2,
                            new Struct(schema)
                                    .put(ROW_ID_COLUMN, 2)
                                    .put("e8", "hi")
                                    .put("e16", "lo")
                                    .put("ae8", List.of())
                                    .put("ae16", Arrays.asList((String) null))
                                    .put("me8", mapOf())
                                    .put("me16", mapOf("only", null))),
                    sinkRecord(
                            tableName,
                            schema,
                            3,
                            new Struct(schema)
                                    .put(ROW_ID_COLUMN, 3)
                                    .put("e8", null)
                                    .put("e16", null)
                                    .put("ae8", Arrays.asList("a", "a", null))
                                    .put("ae16", List.of("k", "k"))
                                    .put("me8", mapOf("x", "b"))
                                    .put("me16", mapOf("y", "a"))));
            NullableRoundTrip.putAndStop(props, records);

            NullableRoundTrip.selectAndAssert(
                    chc,
                    tableName,
                    1,
                    withDefaultColumn(
                            concat(
                                    valueCells("e8", "e8", ENUM8_CODE_TYPE, "lo", -128),
                                    valueCells("e16", "e16", ENUM16_CODE_TYPE, "hi", 32767),
                                    List.of(lengthCell("ae8", "ae8", 3)),
                                    valueCells("ae8_1", "ae8[1]", ENUM8_CODE_TYPE, "a", 1),
                                    List.of(nullCell("ae8_2", "ae8[2]", true)),
                                    valueCells("ae8_3", "ae8[3]", ENUM8_CODE_TYPE, "b", 2),
                                    List.of(lengthCell("ae16", "ae16", 3)),
                                    valueCells("ae16_1", "ae16[1]", ENUM16_CODE_TYPE, "k", 1000),
                                    List.of(nullCell("ae16_2", "ae16[2]", true)),
                                    valueCells("ae16_3", "ae16[3]", ENUM16_CODE_TYPE, "a", 1),
                                    List.of(lengthCell("me8", "me8", 2)),
                                    valueCells("me8_a", "me8['a']", ENUM8_CODE_TYPE, "a", 1),
                                    List.of(
                                            nullCell("me8_b", "me8['b']", true),
                                            cell("me8_b_present", "mapContains(me8, 'b')", TRUE_TEXT),
                                            lengthCell("me16", "me16", 2)),
                                    valueCells("me16_k", "me16['k']", ENUM16_CODE_TYPE, "k", 1000),
                                    List.of(nullCell("me16_n", "me16['n']", true))),
                            twin));
            NullableRoundTrip.selectAndAssert(
                    chc,
                    tableName,
                    2,
                    withDefaultColumn(
                            concat(
                                    valueCells("e8", "e8", ENUM8_CODE_TYPE, "hi", 127),
                                    valueCells("e16", "e16", ENUM16_CODE_TYPE, "lo", -32768),
                                    List.of(
                                            lengthCell("ae8", "ae8", 0),
                                            lengthCell("ae16", "ae16", 1),
                                            nullCell("ae16_1", "ae16[1]", true),
                                            lengthCell("me8", "me8", 0),
                                            lengthCell("me16", "me16", 1),
                                            nullCell("me16_only", "me16['only']", true),
                                            cell("me16_only_present", "mapContains(me16, 'only')", TRUE_TEXT))),
                            twin));
            NullableRoundTrip.selectAndAssert(
                    chc,
                    tableName,
                    3,
                    withDefaultColumn(
                            concat(
                                    List.of(
                                            nullCell("e8", "e8", true),
                                            nullCell("e16", "e16", true),
                                            lengthCell("ae8", "ae8", 3)),
                                    valueCells("ae8_1", "ae8[1]", ENUM8_CODE_TYPE, "a", 1),
                                    valueCells("ae8_2", "ae8[2]", ENUM8_CODE_TYPE, "a", 1),
                                    List.of(
                                            nullCell("ae8_3", "ae8[3]", true),
                                            lengthCell("ae16", "ae16", 2)),
                                    valueCells("ae16_1", "ae16[1]", ENUM16_CODE_TYPE, "k", 1000),
                                    valueCells("ae16_2", "ae16[2]", ENUM16_CODE_TYPE, "k", 1000),
                                    List.of(lengthCell("me8", "me8", 1)),
                                    valueCells("me8_x", "me8['x']", ENUM8_CODE_TYPE, "b", 2),
                                    List.of(lengthCell("me16", "me16", 1)),
                                    valueCells("me16_y", "me16['y']", ENUM16_CODE_TYPE, "a", 1)),
                            twin));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @ParameterizedTest(name = "COLS-08 omitted Nullable(Enum) fields store NULL {0}")
    @EnumSource(Twin.class)
    void omittedNullableEnumFieldsStoreNull(Twin twin, TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName(OMITTED_TABLE_PREFIX + twin);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        NullableRoundTrip.createTable(chc, tableName, twin.ddl(OMITTED_DDL));
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        try {
            assertEquals(
                    twin.hasDefaults(),
                    chc.describeTable(chc.getDatabase(), tableName).hasDefaults(),
                    "twin precondition: DESCRIBE must report a default exactly on the DEFAULT twin");
            Schema schema = SchemaBuilder.struct()
                    .field(ROW_ID_COLUMN, Schema.INT32_SCHEMA)
                    .build();
            NullableRoundTrip.putAndStop(
                    props, List.of(sinkRecord(tableName, schema, 1, new Struct(schema).put(ROW_ID_COLUMN, 1))));
            NullableRoundTrip.selectAndAssert(
                    chc,
                    tableName,
                    1,
                    withDefaultColumn(List.of(nullCell("e8", "e8", true), nullCell("e16", "e16", true)), twin));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }
}
