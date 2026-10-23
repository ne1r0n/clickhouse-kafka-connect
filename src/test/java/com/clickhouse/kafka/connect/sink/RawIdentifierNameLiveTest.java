package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.kafka.connect.sink.NullableRoundTrip.Cell;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.db.helper.RawNameMatrix;
import com.clickhouse.kafka.connect.sink.db.helper.RawNameMatrix.Entry;
import com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullability;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * NAME-05 (D-05): the live pins of the two name forms for every character of the {@link
 * RawNameMatrix} spike. Each case creates the matrix's names table, puts Kafka records through the
 * real {@link ClickHouseSinkTask} on the default RowBinary route and reads the row back typed.
 *
 * <p>Raw form: a record that names the root field and the Tuple elements as written in {@code
 * CREATE TABLE} lands in the root column and in a Tuple, an Array(Tuple), a Nullable(Tuple) and a
 * Map-value Tuple, and discovery logs no WARN. Doubled form (the server's JSON escaping of the
 * name, which the connector matched before the fix): a root field named that way is ignored and
 * the column's DEFAULT is stored, while an element named that way fails the put and leaves no row.
 * The class logs no record value; the one line per failing element case holds the exception
 * class and the first line of the root cause's message, which names a field, not a value.
 */
public class RawIdentifierNameLiveTest extends ClickHouseBase {

    static final String LOG_PREFIX = "NAME05_DOUBLED_ELEMENT";

    private static final String CONNECTOR_NAME_PROP = "name";
    private static final String TABLE_PREFIX = "rin_";
    private static final String WARN_LEVEL = "WARN";
    private static final String ROOT_ALIAS = "root_v";
    private static final String TO_STRING_FORMAT = "toString(%s)";
    private static final String FAILURE_FORMAT = LOG_PREFIX + " key=%s container=%s class=%s root_cause=%s message=%s";
    private static final String LINE_BREAK_PATTERN = "\\R";
    private static final String NO_MESSAGE = "none";
    private static final String MISSING_COLUMN_FORMAT = "column '%s'";
    private static final Map<String, String> ELEMENT_PATH_PREFIXES = Map.of(
            RawNameMatrix.SHAPE_TUPLE, RawNameMatrix.TUPLE_COLUMN + ".",
            RawNameMatrix.SHAPE_ARRAY_TUPLE, RawNameMatrix.ARRAY_COLUMN + ".",
            RawNameMatrix.SHAPE_NULLABLE_TUPLE, RawNameMatrix.NULLABLE_COLUMN + ".",
            RawNameMatrix.SHAPE_MAP_TUPLE, RawNameMatrix.MAP_COLUMN + ".values.");

    private static final String ENUM_TABLE_PREFIX = "rin_enum_";
    private static final String ENUM_CODE_ALIAS = "code_v";
    private static final String ENUM_MATCH_ALIAS = "match_v";
    private static final String ENUM_CODE_EXPRESSION = "CAST(" + RawNameMatrix.ENUM_COLUMN + ", 'Int8')";
    private static final String ENUM_MATCH_FORMAT = RawNameMatrix.ENUM_COLUMN + " = %s";
    private static final int ENUM_ROW_ID = 1;
    private static final String ENUM_FIRST_CODE = "1";

    private static final int RAW_ROW_ID = 1;
    private static final int DOUBLED_ROOT_ROW_ID = 2;
    private static final int DOUBLED_ELEMENT_ROW_ID = 3;
    private static final int RAW_ROOT_VALUE = 7;
    private static final int IGNORED_ROOT_VALUE = 5;

    private static final String TUPLE_TEXT = "t";
    private static final String ARRAY_TEXT = "a";
    private static final String NULLABLE_TEXT = "n";
    private static final String MAP_TEXT = "m";
    private static final String MAP_KEY = "k";
    private static final int TUPLE_NUMBER = 1;
    private static final int ARRAY_NUMBER = 2;
    private static final int NULLABLE_NUMBER = 3;
    private static final int MAP_NUMBER = 4;

    private static final List<String> TUPLE_CONTAINERS = List.of(
            RawNameMatrix.SHAPE_TUPLE,
            RawNameMatrix.SHAPE_ARRAY_TUPLE,
            RawNameMatrix.SHAPE_NULLABLE_TUPLE,
            RawNameMatrix.SHAPE_MAP_TUPLE);

    /** The field names of one record: the root field and the element name inside each container. */
    private record FieldNames(String root, String tuple, String array, String nullable, String map) {

        static FieldNames raw(Entry entry) {
            String element = RawNameMatrix.elementName(entry);
            return new FieldNames(RawNameMatrix.rootName(entry), element, element, element, element);
        }

        FieldNames withRoot(String name) {
            return new FieldNames(name, tuple, array, nullable, map);
        }

        FieldNames withElement(String container, String name) {
            switch (container) {
                case RawNameMatrix.SHAPE_TUPLE:
                    return new FieldNames(root, name, array, nullable, map);
                case RawNameMatrix.SHAPE_ARRAY_TUPLE:
                    return new FieldNames(root, tuple, name, nullable, map);
                case RawNameMatrix.SHAPE_NULLABLE_TUPLE:
                    return new FieldNames(root, tuple, array, name, map);
                case RawNameMatrix.SHAPE_MAP_TUPLE:
                    return new FieldNames(root, tuple, array, nullable, name);
                default:
                    throw new IllegalArgumentException("unknown container " + container);
            }
        }
    }

    // ---- schema and value helpers ----

    private static SchemaBuilder elementBuilder(String elementName) {
        return SchemaBuilder.struct()
                .field(elementName, Schema.INT32_SCHEMA)
                .field(RawNameMatrix.PLAIN_ELEMENT, Schema.STRING_SCHEMA);
    }

    private static Schema recordSchema(FieldNames names) {
        return SchemaBuilder.struct()
                .field(RawNameMatrix.ID_COLUMN, Schema.INT32_SCHEMA)
                .field(names.root(), Schema.INT32_SCHEMA)
                .field(RawNameMatrix.TUPLE_COLUMN, elementBuilder(names.tuple()).build())
                .field(
                        RawNameMatrix.ARRAY_COLUMN,
                        SchemaBuilder.array(elementBuilder(names.array()).build()).build())
                .field(
                        RawNameMatrix.NULLABLE_COLUMN,
                        elementBuilder(names.nullable()).optional().build())
                .field(
                        RawNameMatrix.MAP_COLUMN,
                        SchemaBuilder.map(
                                        Schema.STRING_SCHEMA,
                                        elementBuilder(names.map()).build())
                                .build())
                .build();
    }

    private static Struct elementValue(Schema schema, String elementName, int number, String text) {
        return new Struct(schema).put(elementName, number).put(RawNameMatrix.PLAIN_ELEMENT, text);
    }

    private static SinkRecord record(String topic, FieldNames names, int id, int rootValue) {
        Schema schema = recordSchema(names);
        Schema tupleSchema = schema.field(RawNameMatrix.TUPLE_COLUMN).schema();
        Schema arraySchema = schema.field(RawNameMatrix.ARRAY_COLUMN).schema();
        Schema nullableSchema = schema.field(RawNameMatrix.NULLABLE_COLUMN).schema();
        Schema mapSchema = schema.field(RawNameMatrix.MAP_COLUMN).schema();
        Struct value = new Struct(schema)
                .put(RawNameMatrix.ID_COLUMN, id)
                .put(names.root(), rootValue)
                .put(RawNameMatrix.TUPLE_COLUMN, elementValue(tupleSchema, names.tuple(), TUPLE_NUMBER, TUPLE_TEXT))
                .put(
                        RawNameMatrix.ARRAY_COLUMN,
                        List.of(elementValue(arraySchema.valueSchema(), names.array(), ARRAY_NUMBER, ARRAY_TEXT)))
                .put(
                        RawNameMatrix.NULLABLE_COLUMN,
                        elementValue(nullableSchema, names.nullable(), NULLABLE_NUMBER, NULLABLE_TEXT))
                .put(
                        RawNameMatrix.MAP_COLUMN,
                        Map.of(MAP_KEY, elementValue(mapSchema.valueSchema(), names.map(), MAP_NUMBER, MAP_TEXT)));
        return new SinkRecord(topic, 0, null, null, schema, value, id);
    }

    private static Cell toStringCell(String column, String expected) {
        return new Cell(column + "_v", String.format(TO_STRING_FORMAT, column), expected);
    }

    /** The typed read-back of one row: the root column and the four containers as the record wrote them. */
    private static List<Cell> readBack(Entry entry, int expectedRoot) {
        return List.of(
                new Cell(ROOT_ALIAS, RawNameMatrix.backtickSql(RawNameMatrix.rootName(entry)), String.valueOf(expectedRoot)),
                toStringCell(RawNameMatrix.TUPLE_COLUMN, "(" + TUPLE_NUMBER + ",'" + TUPLE_TEXT + "')"),
                toStringCell(RawNameMatrix.ARRAY_COLUMN, "[(" + ARRAY_NUMBER + ",'" + ARRAY_TEXT + "')]"),
                toStringCell(RawNameMatrix.NULLABLE_COLUMN, "(" + NULLABLE_NUMBER + ",'" + NULLABLE_TEXT + "')"),
                toStringCell(
                        RawNameMatrix.MAP_COLUMN,
                        "{'" + MAP_KEY + "':(" + MAP_NUMBER + ",'" + MAP_TEXT + "')}"));
    }

    private static SinkRecord enumRecord(String topic, String label) {
        Schema schema = SchemaBuilder.struct()
                .field(RawNameMatrix.ID_COLUMN, Schema.INT32_SCHEMA)
                .field(RawNameMatrix.ENUM_COLUMN, Schema.STRING_SCHEMA)
                .build();
        Struct value = new Struct(schema)
                .put(RawNameMatrix.ID_COLUMN, ENUM_ROW_ID)
                .put(RawNameMatrix.ENUM_COLUMN, label);
        return new SinkRecord(topic, 0, null, null, schema, value, ENUM_ROW_ID);
    }

    // ---- method sources ----

    static Stream<Entry> allEntries() {
        return RawNameMatrix.ENTRIES.stream();
    }

    /** The entries whose doubled form differs from the raw form; ctl, bt, na and ap have no second form. */
    static Stream<Entry> doubledEntries() {
        return RawNameMatrix.ENTRIES.stream().filter(entry -> !doubledRoot(entry).equals(RawNameMatrix.rootName(entry)));
    }

    static Stream<Arguments> doubledElementCases() {
        return doubledEntries()
                .flatMap(entry -> TUPLE_CONTAINERS.stream().map(container -> Arguments.of(entry, container)));
    }

    /** The entries whose pre-fix label key differs from the raw label; ctl, bt and na have no such key. */
    static Stream<Entry> legacyLabelEntries() {
        return RawNameMatrix.ENTRIES.stream()
                .filter(entry -> !RawNameMatrix.legacyLabelKey(RawNameMatrix.label(entry))
                        .equals(RawNameMatrix.label(entry)));
    }

    private static String doubledRoot(Entry entry) {
        return RawNameMatrix.doubledForm(RawNameMatrix.rootName(entry));
    }

    // ---- the pins ----

    @ParameterizedTest(
            name = "NAME-05 raw form: a record using the raw names lands and reads back typed for entry {0}")
    @MethodSource("allEntries")
    void rawNamesLandAndReadBackTyped(Entry entry, TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        String table = createTopicName(TABLE_PREFIX + entry.key());
        try (ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props)) {
            try {
                NullableRoundTrip.createTable(chc, table, RawNameMatrix.namesTableDdl(entry));
                try (LogCapture helperLog = LogCapture.attach(ClickHouseHelperClient.class);
                        LogCapture nullabilityLog = LogCapture.attach(DeclaredNullability.class)) {
                    chc.describeTable(chc.getDatabase(), table);
                    NullableRoundTrip.putAndStop(
                            props, List.of(record(table, FieldNames.raw(entry), RAW_ROW_ID, RAW_ROOT_VALUE)));

                    assertEquals(List.of(), warnMessages(helperLog), "discovery must not warn (helper logger)");
                    assertEquals(List.of(), warnMessages(nullabilityLog), "discovery must not warn (nullability)");
                }
                NullableRoundTrip.selectAndAssert(chc, table, RAW_ROW_ID, readBack(entry, RAW_ROOT_VALUE));
            } finally {
                ClickHouseTestHelpers.dropTable(chc, table);
            }
        }
    }

    @ParameterizedTest(
            name = "NAME-05 doubled form: a root field named in the doubled form is ignored and the DEFAULT is"
                    + " stored for entry {0}")
    @MethodSource("doubledEntries")
    void doubledRootNameStoresTheDefault(Entry entry, TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        String table = createTopicName(TABLE_PREFIX + entry.key());
        try (ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props)) {
            try {
                NullableRoundTrip.createTable(chc, table, RawNameMatrix.namesTableDdl(entry));
                FieldNames names = FieldNames.raw(entry).withRoot(doubledRoot(entry));
                NullableRoundTrip.putAndStop(
                        props, List.of(record(table, names, DOUBLED_ROOT_ROW_ID, IGNORED_ROOT_VALUE)));

                NullableRoundTrip.selectAndAssert(
                        chc, table, DOUBLED_ROOT_ROW_ID, readBack(entry, RawNameMatrix.ROOT_DEFAULT));
            } finally {
                ClickHouseTestHelpers.dropTable(chc, table);
            }
        }
    }

    @ParameterizedTest(name = "NAME-05 doubled form: a {1} element named in the doubled form fails loudly for entry {0}")
    @MethodSource("doubledElementCases")
    void doubledElementNameFailsLoudly(Entry entry, String container, TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        String table = createTopicName(TABLE_PREFIX + container + "_" + entry.key());
        try (ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props)) {
            try {
                NullableRoundTrip.createTable(chc, table, RawNameMatrix.namesTableDdl(entry));
                FieldNames names = FieldNames.raw(entry)
                        .withElement(container, RawNameMatrix.doubledForm(RawNameMatrix.elementName(entry)));

                RuntimeException failure = NullableRoundTrip.assertPutFails(
                        props,
                        List.of(record(table, names, DOUBLED_ELEMENT_ROW_ID, RAW_ROOT_VALUE)),
                        chc,
                        table,
                        missingColumnText(entry, container));
                System.out.println(String.format(
                        FAILURE_FORMAT,
                        entry.key(),
                        container,
                        failure.getClass().getSimpleName(),
                        rootCause(failure).getClass().getSimpleName(),
                        RawNameMatrix.render(firstLine(rootCause(failure).getMessage()))));
            } finally {
                ClickHouseTestHelpers.dropTable(chc, table);
            }
        }
    }

    @ParameterizedTest(
            name = "NAME-06 raw form: an Enum8 value using the raw label lands and reads back typed for entry {0}")
    @MethodSource("allEntries")
    void rawEnumLabelLandsAndReadsBackTyped(Entry entry, TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        String table = createTopicName(ENUM_TABLE_PREFIX + entry.key());
        try (ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props)) {
            try {
                NullableRoundTrip.createTable(chc, table, RawNameMatrix.enumTableDdl(entry));
                NullableRoundTrip.putAndStop(props, List.of(enumRecord(table, RawNameMatrix.label(entry))));

                NullableRoundTrip.selectAndAssert(chc, table, ENUM_ROW_ID, List.of(
                        new Cell(ENUM_CODE_ALIAS, ENUM_CODE_EXPRESSION, ENUM_FIRST_CODE),
                        new Cell(
                                ENUM_MATCH_ALIAS,
                                String.format(ENUM_MATCH_FORMAT, RawNameMatrix.quotedSql(RawNameMatrix.label(entry))),
                                ENUM_FIRST_CODE)));
            } finally {
                ClickHouseTestHelpers.dropTable(chc, table);
            }
        }
    }

    @ParameterizedTest(
            name = "NAME-06 legacy form: an Enum8 value using the pre-fix label key fails loudly for entry {0}")
    @MethodSource("legacyLabelEntries")
    void legacyEnumLabelKeyFailsLoudly(Entry entry, TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        String table = createTopicName(ENUM_TABLE_PREFIX + "legacy_" + entry.key());
        try (ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props)) {
            try {
                NullableRoundTrip.createTable(chc, table, RawNameMatrix.enumTableDdl(entry));
                String legacyKey = RawNameMatrix.legacyLabelKey(RawNameMatrix.label(entry));

                NullableRoundTrip.assertPutFails(props, List.of(enumRecord(table, legacyKey)), chc, table);
            } finally {
                ClickHouseTestHelpers.dropTable(chc, table);
            }
        }
    }

    /**
     * The writer's report of the element that found no value, as it names the column of the container
     * under test in the raw form. Without it the case would also pass on the code before the fix,
     * where the three raw-named containers of the same record fail instead.
     */
    private static String missingColumnText(Entry entry, String container) {
        return String.format(MISSING_COLUMN_FORMAT, ELEMENT_PATH_PREFIXES.get(container) + RawNameMatrix.elementName(entry));
    }

    private static List<String> warnMessages(LogCapture capture) {
        return capture.events(WARN_LEVEL).stream()
                .map(LogCapture.CapturedEvent::message)
                .collect(Collectors.toList());
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String firstLine(String message) {
        return message == null ? NO_MESSAGE : message.split(LINE_BREAK_PATTERN, 2)[0];
    }
}
