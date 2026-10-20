package com.clickhouse.kafka.connect.sink;

import static com.clickhouse.kafka.connect.sink.NullableRoundTrip.DEFAULT_VALUE_TEXT;
import static com.clickhouse.kafka.connect.sink.NullableRoundTrip.FALSE_TEXT;
import static com.clickhouse.kafka.connect.sink.NullableRoundTrip.TRUE_TEXT;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.BACKSLASH_N_ELEMENT;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.BACKSLASH_N_ENUM_LABEL_DDL;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.BACKSLASH_N_ENUM_LABEL_RAW;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.BACKSLASH_N_NAME_DDL;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.BACKSLASH_N_NAME_RAW;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.BACKSLASH_N_NESTED_NAMES_DDL;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.BACKSLASH_N_NESTED_NAMES_RAW;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.NULLABLE_BACKSLASH_N_NAME_DDL;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.NULLABLE_BACKSLASH_N_NAME_RAW;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.REAL_NEWLINE_NAME_DDL;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.REAL_NEWLINE_NAME_RAW;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.TRAILING_BACKSLASH_NAMES_DDL;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.TRAILING_BACKSLASH_NAMES_RAW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.data.ClickHouseColumn;
import com.clickhouse.kafka.connect.sink.NullableRoundTrip.Cell;
import com.clickhouse.kafka.connect.sink.NullableRoundTrip.Twin;
import com.clickhouse.kafka.connect.sink.db.CapturingClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseFieldDescriptor;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.ColumnNullabilityPaths;
import com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullability;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.helper.RowSnapshots;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * COLS-09 spike for backlog 999.1 (D-05..D-07), COLS-10 coverage since plan 19-04: which {@code
 * Nullable(Tuple(...))} shapes the connector maps correctly. Since COLS-10 side (a) asserts that
 * every deciding row (C1-C3) round-trips through the real sink task against the table as DESCRIBE
 * describes it, and that the DESCRIBE-derived Column tree carries the same per-node nullability as
 * the hand-built declared tree. The X cells are pinned as observed after the fix: X2 and X3
 * round-trip, and X1's NULL element is refused with a DataException. Side (b) and the control stay
 * as the byte-level evidence that the unchanged writer round-trips once the declared element
 * nullability is right and fails when it is doubled. Deciding cells are C1-C3, X cells are
 * informational. The DDL deliberately sets no server setting.
 *
 * <p>Records hold synthetic ASCII literals only and the class logs catalog metadata (table key,
 * column, raw type, server version), never record values.
 */
public class NullableTupleMatrixLiveTest extends ClickHouseBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(NullableTupleMatrixLiveTest.class);

    static final String MATRIX_LOG_PREFIX = "NULLABLE_TUPLE_MATRIX";

    private static final String ROW_ID_COLUMN = "id";
    private static final String CELL_COLUMN = "c";
    private static final String DEFAULT_COLUMN = "d";
    private static final String SELECT_IDS_FORMAT = "SELECT id FROM %s";
    private static final String CONNECTOR_NAME_PROP = "name";
    private static final String TABLE_PREFIX = "tm_";
    private static final String SIDE_A_SUFFIX = "_a";
    private static final String SIDE_B_SUFFIX = "_b";
    private static final String QUERY_ID_PREFIX = "tm-";
    private static final String C1_DDL_TYPE = "Nullable(Tuple(a Int32, b String))";
    private static final String P1_DDL_TYPE = "Tuple(a Int32, b String)";
    private static final String P2_DDL_TYPE = "Tuple(a Int32, n Tuple(d String, e Nullable(Int32)))";
    // Unnamed elements, matched by 1-based position.
    private static final String P6_DDL_TYPE = "Nullable(Tuple(Int32, Nullable(String)))";
    // Backtick-quoted element names holding a space and a comma.
    private static final String P8_DDL_TYPE = "Nullable(Tuple(`a b` Int32, `c,d` Nullable(String)))";
    private static final String PATH_SEPARATOR = ".";
    // The second element of the backslash-n Tuple; it lives at c.d beside the DEFAULT twin's own column d.
    private static final String TUPLE_STRING_ELEMENT = "d";
    private static final String PREMISE_MESSAGE =
            "premise (IN-03 re-open trigger): the server's escaping must equal the Docker-free pin input";
    // The server errors for a body that is shorter than the doubled null flags make it read.
    private static final List<String> READ_PAST_END_CODES =
            List.of("ATTEMPT_TO_READ_AFTER_EOF", "CANNOT_READ_ALL_DATA");

    /**
     * One record of a cell: the Kafka value of column {@code c} and what a correct read-back shows.
     * {@code declaredWriterRejects} is non-null only for a row the writer must refuse before any
     * insert once the declared nullability is right; it holds a substring of the refusal message.
     */
    private record RowCase(int id, Object value, List<Cell> readBack, String declaredWriterRejects) {

        RowCase(int id, Object value, List<Cell> readBack) {
            this(id, value, readBack, null);
        }
    }

    /**
     * The pinned outcome of one row put through today's DESCRIBE-derived mapping. {@code
     * phantomRows} counts rows that appear in the table under ids no record of the cell carries
     * (misaligned bytes read as further rows).
     */
    private record Expect(Kind kind, List<String> causeSubstrings, List<Cell> observed, int phantomRows) {

        enum Kind {
            ROUND_TRIPS,
            FAILS,
            STORES_WRONG
        }

        static Expect roundTrips() {
            return new Expect(Kind.ROUND_TRIPS, List.of(), List.of(), 0);
        }

        static Expect fails(String... causeSubstrings) {
            return new Expect(Kind.FAILS, List.of(causeSubstrings), List.of(), 0);
        }

        static Expect storesWrong(List<Cell> observed) {
            return storesWrong(observed, 0);
        }

        static Expect storesWrong(List<Cell> observed, int phantomRows) {
            return new Expect(Kind.STORES_WRONG, List.of(), observed, phantomRows);
        }
    }

    /**
     * One matrix cell. {@code declaredColumn} builds the hand-built root Column named by its argument,
     * carrying the nullability the DDL declares; {@code currentOutcome} maps each twin and row id to
     * side (a)'s pinned expectation.
     */
    private record MatrixCell(
            String key,
            boolean deciding,
            String ddlType,
            Schema fieldSchema,
            Function<String, Column> declaredColumn,
            List<RowCase> rows,
            Map<Twin, Map<Integer, Expect>> currentOutcome) {}

    // ---- cells ----

    private static Column element(String parent, String name, Type type, boolean nullable) {
        return Column.builder()
                .name(parent + PATH_SEPARATOR + name)
                .type(type)
                .isNullable(nullable)
                .build();
    }

    private static Cell cell(String alias, String sqlExpression, String expected) {
        return new Cell(alias, sqlExpression, expected);
    }

    private static Map<Twin, Map<Integer, Expect>> onBothTwins(Map<Integer, Expect> outcome) {
        return Map.of(Twin.PLAIN, outcome, Twin.DEFAULT, outcome);
    }

    /** C1 {@code Nullable(Tuple(a Int32, b String))}: the plain nullable tuple of ROADMAP criterion 1. */
    private static MatrixCell c1() {
        Schema fieldSchema = SchemaBuilder.struct()
                .optional()
                .field("a", Schema.INT32_SCHEMA)
                .field("b", Schema.STRING_SCHEMA)
                .build();
        List<RowCase> rows = List.of(
                new RowCase(
                        1,
                        new Struct(fieldSchema).put("a", 7).put("b", "x"),
                        List.of(
                                cell("c_is_null", "isNull(c)", FALSE_TEXT),
                                cell("c_a", "tupleElement(c, 'a')", "7"),
                                cell("c_b", "tupleElement(c, 'b')", "x"))),
                new RowCase(2, null, List.of(cell("c_is_null", "isNull(c)", TRUE_TEXT))));
        Map<Twin, Map<Integer, Expect>> currentOutcome =
                onBothTwins(Map.of(1, Expect.roundTrips(), 2, Expect.roundTrips()));
        return new MatrixCell(
                "c1",
                true,
                C1_DDL_TYPE,
                fieldSchema,
                name -> Column.builder()
                        .name(name)
                        .type(Type.TUPLE)
                        .isNullable(true)
                        .tupleFields(List.of(
                                element(name, "a", Type.INT32, false), element(name, "b", Type.STRING, false)))
                        .build(),
                rows,
                currentOutcome);
    }

    /** C2 {@code Nullable(Tuple(a Nullable(Int32), b String))}: a nullable element beside a plain one. */
    private static MatrixCell c2() {
        Schema fieldSchema = SchemaBuilder.struct()
                .optional()
                .field("a", Schema.OPTIONAL_INT32_SCHEMA)
                .field("b", Schema.STRING_SCHEMA)
                .build();
        List<RowCase> rows = List.of(
                new RowCase(
                        1,
                        new Struct(fieldSchema).put("a", 5).put("b", "y"),
                        List.of(
                                cell("c_is_null", "isNull(c)", FALSE_TEXT),
                                cell("c_a", "tupleElement(c, 'a')", "5"),
                                cell("c_b", "tupleElement(c, 'b')", "y"))),
                new RowCase(2, null, List.of(cell("c_is_null", "isNull(c)", TRUE_TEXT))),
                new RowCase(
                        3,
                        new Struct(fieldSchema).put("a", null).put("b", "z"),
                        List.of(
                                cell("c_is_null", "isNull(c)", FALSE_TEXT),
                                cell("c_a_is_null", "isNull(tupleElement(c, 'a'))", TRUE_TEXT),
                                cell("c_b", "tupleElement(c, 'b')", "z"))));
        return new MatrixCell(
                "c2",
                true,
                "Nullable(Tuple(a Nullable(Int32), b String))",
                fieldSchema,
                name -> Column.builder()
                        .name(name)
                        .type(Type.TUPLE)
                        .isNullable(true)
                        .tupleFields(List.of(
                                element(name, "a", Type.INT32, true), element(name, "b", Type.STRING, false)))
                        .build(),
                rows,
                onBothTwins(Map.of(1, Expect.roundTrips(), 2, Expect.roundTrips(), 3, Expect.roundTrips())));
    }

    /** C3 {@code Array(Nullable(Tuple(a Int32, b Nullable(String))))}: a tuple as a nullable array element. */
    private static MatrixCell c3() {
        Schema elementSchema = SchemaBuilder.struct()
                .optional()
                .field("a", Schema.INT32_SCHEMA)
                .field("b", Schema.OPTIONAL_STRING_SCHEMA)
                .build();
        Schema fieldSchema = SchemaBuilder.array(elementSchema).build();
        List<RowCase> rows = List.of(
                new RowCase(
                        1,
                        Arrays.asList(
                                new Struct(elementSchema).put("a", 1).put("b", "p"),
                                null,
                                new Struct(elementSchema).put("a", 2).put("b", null)),
                        List.of(
                                cell("c_len", "length(c)", "3"),
                                cell("c1_a", "tupleElement(c[1], 'a')", "1"),
                                cell("c1_b", "tupleElement(c[1], 'b')", "p"),
                                cell("c2_is_null", "isNull(c[2])", TRUE_TEXT),
                                cell("c3_a", "tupleElement(c[3], 'a')", "2"),
                                cell("c3_b_is_null", "isNull(tupleElement(c[3], 'b'))", TRUE_TEXT))),
                new RowCase(2, List.of(), List.of(cell("c_len", "length(c)", "0"))),
                new RowCase(
                        3,
                        Arrays.asList((Struct) null),
                        List.of(cell("c_len", "length(c)", "1"), cell("c1_is_null", "isNull(c[1])", TRUE_TEXT))));
        return new MatrixCell(
                "c3",
                true,
                "Array(Nullable(Tuple(a Int32, b Nullable(String))))",
                fieldSchema,
                name -> {
                    Column tuple = Column.builder()
                            .name(name)
                            .type(Type.TUPLE)
                            .isNullable(true)
                            .tupleFields(List.of(
                                    element(name, "a", Type.INT32, false), element(name, "b", Type.STRING, true)))
                            .build();
                    Column array =
                            Column.builder().name(name).type(Type.ARRAY).arrayType(tuple).build();
                    tuple.setParent(array);
                    return array;
                },
                rows,
                onBothTwins(Map.of(1, Expect.roundTrips(), 2, Expect.roundTrips(), 3, Expect.roundTrips())));
    }

    /** X1 (informational) {@code Nullable(Tuple(a Int32))}: the single-field tuple. */
    private static MatrixCell x1() {
        Schema fieldSchema = SchemaBuilder.struct()
                .optional()
                .field("a", Schema.OPTIONAL_INT32_SCHEMA)
                .build();
        List<RowCase> rows = List.of(
                new RowCase(
                        1,
                        new Struct(fieldSchema).put("a", 7),
                        List.of(cell("c_is_null", "isNull(c)", FALSE_TEXT), cell("c_a", "tupleElement(c, 'a')", "7"))),
                new RowCase(2, null, List.of(cell("c_is_null", "isNull(c)", TRUE_TEXT))),
                // The DDL declares `a` non-nullable, so no read-back exists: the declared writer refuses it.
                new RowCase(3, new Struct(fieldSchema).put("a", null), List.of(), "tuple field 'a'"));
        return new MatrixCell(
                "x1",
                false,
                "Nullable(Tuple(a Int32))",
                fieldSchema,
                name -> Column.builder()
                        .name(name)
                        .type(Type.TUPLE)
                        .isNullable(true)
                        .tupleFields(List.of(element(name, "a", Type.INT32, false)))
                        .build(),
                rows,
                onBothTwins(Map.of(
                        1, Expect.roundTrips(),
                        2, Expect.roundTrips(),
                        // Before COLS-10 a NULL `a` landed silently as a NULL tuple; the declared `a` is
                        // non-nullable, so the writer now refuses the batch (ledger L-38).
                        3, Expect.fails("tuple field 'a'"))));
    }

    /** X2 (informational) {@code Map(String, Nullable(Tuple(a Int32, b String)))}: a tuple as a map value. */
    private static MatrixCell x2() {
        Schema valueSchema = SchemaBuilder.struct()
                .optional()
                .field("a", Schema.INT32_SCHEMA)
                .field("b", Schema.STRING_SCHEMA)
                .build();
        Schema fieldSchema = SchemaBuilder.map(Schema.STRING_SCHEMA, valueSchema).build();
        Map<String, Struct> entries = new LinkedHashMap<>();
        entries.put("k1", new Struct(valueSchema).put("a", 1).put("b", "m"));
        entries.put("k2", null);
        List<RowCase> rows = List.of(
                new RowCase(
                        1,
                        entries,
                        List.of(
                                cell("c_len", "length(mapKeys(c))", "2"),
                                cell("k1_a", "tupleElement(c['k1'], 'a')", "1"),
                                cell("k1_b", "tupleElement(c['k1'], 'b')", "m"),
                                cell("k2_is_null", "isNull(c['k2'])", TRUE_TEXT))),
                new RowCase(2, new LinkedHashMap<String, Struct>(), List.of(cell("c_len", "length(mapKeys(c))", "0"))));
        return new MatrixCell(
                "x2",
                false,
                "Map(String, Nullable(Tuple(a Int32, b String)))",
                fieldSchema,
                name -> {
                    String valueName = name + PATH_SEPARATOR + "values";
                    Column value = Column.builder()
                            .name(valueName)
                            .type(Type.TUPLE)
                            .isNullable(true)
                            .tupleFields(List.of(
                                    element(valueName, "a", Type.INT32, false),
                                    element(valueName, "b", Type.STRING, false)))
                            .build();
                    return Column.builder()
                            .name(name)
                            .type(Type.MAP)
                            .mapKeyType(Type.STRING)
                            .mapValueType(value)
                            .build();
                },
                rows,
                onBothTwins(Map.of(1, Expect.roundTrips(), 2, Expect.roundTrips())));
    }

    /** X3 (informational) {@code Nullable(Tuple(a Int32, n Tuple(d String, e Nullable(Int32))))}: a tuple in a tuple. */
    private static MatrixCell x3() {
        Schema innerSchema = SchemaBuilder.struct()
                .field("d", Schema.STRING_SCHEMA)
                .field("e", Schema.OPTIONAL_INT32_SCHEMA)
                .build();
        Schema fieldSchema = SchemaBuilder.struct()
                .optional()
                .field("a", Schema.INT32_SCHEMA)
                .field("n", innerSchema)
                .build();
        List<RowCase> rows = List.of(
                new RowCase(
                        1,
                        new Struct(fieldSchema)
                                .put("a", 1)
                                .put("n", new Struct(innerSchema).put("d", "q").put("e", null)),
                        List.of(
                                cell("c_a", "tupleElement(c, 'a')", "1"),
                                cell("n_d", "tupleElement(tupleElement(c, 'n'), 'd')", "q"),
                                cell("n_e_is_null", "isNull(tupleElement(tupleElement(c, 'n'), 'e'))", TRUE_TEXT))),
                new RowCase(2, null, List.of(cell("c_is_null", "isNull(c)", TRUE_TEXT))));
        return new MatrixCell(
                "x3",
                false,
                "Nullable(Tuple(a Int32, n Tuple(d String, e Nullable(Int32))))",
                fieldSchema,
                name -> {
                    String innerName = name + PATH_SEPARATOR + "n";
                    Column inner = Column.builder()
                            .name(innerName)
                            .type(Type.TUPLE)
                            .tupleFields(List.of(
                                    element(innerName, "d", Type.STRING, false),
                                    element(innerName, "e", Type.INT32, true)))
                            .build();
                    return Column.builder()
                            .name(name)
                            .type(Type.TUPLE)
                            .isNullable(true)
                            .tupleFields(List.of(element(name, "a", Type.INT32, false), inner))
                            .build();
                },
                rows,
                onBothTwins(Map.of(1, Expect.roundTrips(), 2, Expect.roundTrips())));
    }

    /** Deciding cells first, then the informational extras. */
    private static List<MatrixCell> cells() {
        return List.of(c1(), c2(), c3(), x1(), x2(), x3());
    }

    static Stream<Arguments> cellTwins() {
        List<Arguments> arguments = new ArrayList<>();
        for (MatrixCell matrixCell : cells()) {
            for (Twin twin : Twin.values()) {
                arguments.add(Arguments.of(matrixCell.key() + "/" + twin, matrixCell, twin));
            }
        }
        return arguments.stream();
    }

    // ---- shared plumbing ----

    private static Schema recordSchema(MatrixCell matrixCell) {
        return SchemaBuilder.struct()
                .field(ROW_ID_COLUMN, Schema.INT32_SCHEMA)
                .field(CELL_COLUMN, matrixCell.fieldSchema())
                .build();
    }

    private static SinkRecord sinkRecord(String topic, Schema schema, RowCase row) {
        Struct struct = new Struct(schema).put(ROW_ID_COLUMN, row.id());
        if (row.value() != null) {
            struct.put(CELL_COLUMN, row.value());
        }
        return new SinkRecord(topic, 0, null, null, schema, struct, row.id());
    }

    private static List<SinkRecord> sinkRecords(String topic, MatrixCell matrixCell, List<RowCase> rows) {
        Schema schema = recordSchema(matrixCell);
        List<SinkRecord> records = new ArrayList<>();
        for (RowCase row : rows) {
            records.add(sinkRecord(topic, schema, row));
        }
        return records;
    }

    private static List<Cell> withDefaultColumn(List<Cell> cells, Twin twin) {
        if (!twin.hasDefaults()) {
            return cells;
        }
        List<Cell> withDefault = new ArrayList<>(cells);
        withDefault.add(cell(DEFAULT_COLUMN, DEFAULT_COLUMN, DEFAULT_VALUE_TEXT));
        return withDefault;
    }

    private static Table handBuiltTable(String database, String tableName, Twin twin, Column cellColumn) {
        List<Column> columns = new ArrayList<>();
        columns.add(Column.builder().name(ROW_ID_COLUMN).type(Type.UINT32).build());
        columns.add(cellColumn);
        if (twin.hasDefaults()) {
            columns.add(Column.builder()
                    .name(DEFAULT_COLUMN)
                    .type(Type.UINT32)
                    .hasDefault(true)
                    .build());
        }
        return new Table(database, tableName, twin.hasDefaults(), columns, columns.size());
    }

    private static CapturingClickHouseWriter.Capture captureBatch(
            Table table, ClickHouseSinkConfig config, List<SinkRecord> records, String key) throws Exception {
        CapturingClickHouseWriter writer = new CapturingClickHouseWriter(table, config);
        int firstId = ((Struct) records.get(0).value()).getInt32(ROW_ID_COLUMN);
        int lastId = ((Struct) records.get(records.size() - 1).value()).getInt32(ROW_ID_COLUMN);
        writer.doInsert(
                CapturingClickHouseWriter.convertAll(records),
                new QueryIdentifier(table.getCleanName(), 0, firstId, lastId, QUERY_ID_PREFIX + key));
        assertEquals(1, writer.captures().size(), "one insert must reach the capture seam");
        return writer.captures().get(0);
    }

    /** Rows in the table whose id belongs to no record of the cell. */
    private static int phantomRowCount(ClickHouseHelperClient chc, String tableName, MatrixCell matrixCell)
            throws Exception {
        String ids = matrixCell.rows().stream()
                .map(row -> String.valueOf(row.id()))
                .collect(Collectors.joining(","));
        String count = RowSnapshots.select(
                        chc,
                        "SELECT count() AS n FROM " + tableName + " WHERE id NOT IN (" + ids + ")",
                        "n")
                .get(0)
                .get("n");
        return Integer.parseInt(count);
    }

    /** The declared writer must refuse the row before any insert reaches the capture seam. */
    private static void assertWriterRejects(
            Table table, ClickHouseSinkConfig config, List<SinkRecord> records, RowCase row) {
        CapturingClickHouseWriter writer = new CapturingClickHouseWriter(table, config);
        RuntimeException failure = assertThrows(
                RuntimeException.class,
                () -> writer.doInsert(
                        CapturingClickHouseWriter.convertAll(records),
                        new QueryIdentifier(table.getCleanName(), 0, row.id(), row.id(), QUERY_ID_PREFIX + "rejects")));
        boolean refused = false;
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            refused |= cause instanceof DataException
                    && cause.getMessage() != null
                    && cause.getMessage().contains(row.declaredWriterRejects());
        }
        assertTrue(refused, "expected a DataException naming '" + row.declaredWriterRejects() + "' in: " + failure);
        assertEquals(List.of(), writer.captures(), "a refused row must not reach the insert");
    }

    // ---- tests ----

    @ParameterizedTest(name = "COLS-09 side (a) {0}")
    @MethodSource("cellTwins")
    void sideAPinsTheDescribeDerivedOutcome(String label, MatrixCell matrixCell, Twin twin, TestInfo testInfo)
            throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName(TABLE_PREFIX + matrixCell.key() + "_" + twin + SIDE_A_SUFFIX);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        // Create the table BEFORE start() - ClickHouseWriter.start() fails against an empty database.
        NullableRoundTrip.createTable(chc, tableName, twin.ddl(CELL_COLUMN + " " + matrixCell.ddlType()));
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        try {
            assertEquals(
                    twin.hasDefaults(),
                    chc.describeTable(chc.getDatabase(), tableName).hasDefaults(),
                    "twin precondition: DESCRIBE must report a default exactly on the DEFAULT twin");
            assertEquals(
                    ColumnNullabilityPaths.byPath(CELL_COLUMN, matrixCell.declaredColumn().apply(CELL_COLUMN)),
                    ColumnNullabilityPaths.byPath(
                            CELL_COLUMN,
                            chc.describeTable(chc.getDatabase(), tableName)
                                    .getRootColumnsMap()
                                    .get(CELL_COLUMN)),
                    "COLS-10: the DESCRIBE-derived Column tree must carry the declared per-node nullability");
            for (ClickHouseFieldDescriptor row : NullableRoundTrip.describeRows(chc, tableName)) {
                LOGGER.info(
                        "{} describe {} {} | {} | subcolumn={}",
                        MATRIX_LOG_PREFIX,
                        label,
                        row.getName(),
                        row.getType(),
                        row.isSubcolumn());
            }
            for (RowCase row : matrixCell.rows()) {
                List<SinkRecord> records = sinkRecords(tableName, matrixCell, List.of(row));
                Expect expect = matrixCell.currentOutcome().get(twin).get(row.id());
                assertNotNull(expect, "no pinned outcome for row id=" + row.id());
                switch (expect.kind()) {
                    case ROUND_TRIPS -> {
                        NullableRoundTrip.putAndStop(props, records);
                        NullableRoundTrip.selectAndAssert(
                                chc, tableName, row.id(), withDefaultColumn(row.readBack(), twin));
                    }
                    case FAILS -> NullableRoundTrip.assertPutFails(
                            props, records, chc, tableName, expect.causeSubstrings().toArray(new String[0]));
                    case STORES_WRONG -> {
                        NullableRoundTrip.putAndStop(props, records);
                        NullableRoundTrip.selectAndAssert(
                                chc, tableName, row.id(), withDefaultColumn(expect.observed(), twin));
                        assertEquals(
                                expect.phantomRows(),
                                phantomRowCount(chc, tableName, matrixCell),
                                "rows under ids no record of the cell carries");
                    }
                }
            }
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @ParameterizedTest(name = "COLS-09 side (b) {0}")
    @MethodSource("cellTwins")
    void sideBRoundTripsWithDeclaredNullability(String label, MatrixCell matrixCell, Twin twin) throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName(TABLE_PREFIX + matrixCell.key() + "_" + twin + SIDE_B_SUFFIX);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        NullableRoundTrip.createTable(chc, tableName, twin.ddl(CELL_COLUMN + " " + matrixCell.ddlType()));
        try {
            Table table = handBuiltTable(
                    chc.getDatabase(), tableName, twin, matrixCell.declaredColumn().apply(CELL_COLUMN));
            ClickHouseSinkConfig config = new ClickHouseSinkConfig(getBaseProps());
            List<RowCase> accepted = matrixCell.rows().stream()
                    .filter(row -> row.declaredWriterRejects() == null)
                    .toList();
            CapturingClickHouseWriter.Capture capture =
                    captureBatch(table, config, sinkRecords(tableName, matrixCell, accepted), matrixCell.key());
            assertEquals(twin.hasDefaults(), capture.supportDefaults(), "the writer must take the twin's format");
            NullableRoundTrip.insertCaptured(chc, tableName, capture);
            for (RowCase row : accepted) {
                NullableRoundTrip.selectAndAssert(
                        chc, tableName, row.id(), withDefaultColumn(row.readBack(), twin));
            }
            for (RowCase row : matrixCell.rows()) {
                if (row.declaredWriterRejects() != null) {
                    assertWriterRejects(table, config, sinkRecords(tableName, matrixCell, List.of(row)), row);
                }
            }
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @ParameterizedTest(name = "COLS-09 control {0}")
    @EnumSource(Twin.class)
    void controlWithBothFieldsNullableFailsToInsert(Twin twin) throws Exception {
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        String tableName = createTopicName(TABLE_PREFIX + "control_" + twin);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        MatrixCell matrixCell = c1();
        NullableRoundTrip.createTable(chc, tableName, twin.ddl(CELL_COLUMN + " " + matrixCell.ddlType()));
        try {
            // The double flag DESCRIBE produces today: nullable tuple and nullable elements.
            Column doubleFlagged = Column.builder()
                    .name(CELL_COLUMN)
                    .type(Type.TUPLE)
                    .isNullable(true)
                    .tupleFields(List.of(
                            element(CELL_COLUMN, "a", Type.INT32, true),
                            element(CELL_COLUMN, "b", Type.STRING, true)))
                    .build();
            Table table = handBuiltTable(chc.getDatabase(), tableName, twin, doubleFlagged);
            CapturingClickHouseWriter.Capture capture = captureBatch(
                    table,
                    new ClickHouseSinkConfig(getBaseProps()),
                    sinkRecords(tableName, matrixCell, matrixCell.rows()),
                    "control");

            Exception failure =
                    assertThrows(Exception.class, () -> NullableRoundTrip.insertCaptured(chc, tableName, capture));
            String causeChain = NullableRoundTrip.causeChainMessages(failure);
            String matchedCode = READ_PAST_END_CODES.stream()
                    .filter(causeChain::contains)
                    .findFirst()
                    .orElse(null);
            assertNotNull(
                    matchedCode,
                    "the doubled null flags must fail with a read-past-end server code " + READ_PAST_END_CODES
                            + " but the cause chain was: " + causeChain);
            LOGGER.info("{} control {} failed with {}", MATRIX_LOG_PREFIX, twin, matchedCode);
            assertEquals(
                    List.of(),
                    RowSnapshots.select(chc, String.format(SELECT_IDS_FORMAT, tableName), ROW_ID_COLUMN),
                    "a failed control insert must leave no row behind");
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    /**
     * Flattens a parsed type into {@code path -> isNullable}: the root keeps its column name, each
     * nested column is appended by element name, or by its 1-based position when it has none
     * (unnamed tuples, and the element or key/value slots of Array and Map).
     */
    private static void flatten(String path, ClickHouseColumn column, Map<String, Boolean> into) {
        into.put(path, column.isNullable());
        int position = 0;
        for (ClickHouseColumn child : column.getNestedColumns()) {
            position++;
            String name = child.getColumnName();
            String element = name == null || name.isEmpty() ? String.valueOf(position) : name;
            flatten(path + PATH_SEPARATOR + element, child, into);
        }
    }

    private static Map<String, Boolean> nullability(Object... pathsAndFlags) {
        Map<String, Boolean> expected = new LinkedHashMap<>();
        for (int i = 0; i < pathsAndFlags.length; i += 2) {
            expected.put((String) pathsAndFlags[i], (Boolean) pathsAndFlags[i + 1]);
        }
        return expected;
    }

    @Test
    @DisplayName(
            "COLS-09 approach: declared Tuple types parse with ClickHouseColumn.of after normalisation and carry the DDL's element nullability")
    void declaredTupleTypesParseWithDdlNullability() throws Exception {
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        String tableName = createTopicName(TABLE_PREFIX + "approach");
        ClickHouseTestHelpers.dropTable(chc, tableName);
        // Root column -> DDL type, then the nullability the DDL declares for every node of that type.
        Map<String, String> ddlTypes = new LinkedHashMap<>();
        Map<String, Map<String, Boolean>> expected = new LinkedHashMap<>();
        for (MatrixCell matrixCell : cells()) {
            ddlTypes.put(matrixCell.key(), matrixCell.ddlType());
        }
        expected.put("c1", nullability("c1", true, "c1.a", false, "c1.b", false));
        expected.put("c2", nullability("c2", true, "c2.a", true, "c2.b", false));
        expected.put("c3", nullability("c3", false, "c3.1", true, "c3.1.a", false, "c3.1.b", true));
        expected.put("x1", nullability("x1", true, "x1.a", false));
        expected.put("x2", nullability("x2", false, "x2.1", false, "x2.2", true, "x2.2.a", false, "x2.2.b", false));
        expected.put(
                "x3", nullability("x3", true, "x3.a", false, "x3.n", false, "x3.n.d", false, "x3.n.e", true));
        ddlTypes.put("p1", P1_DDL_TYPE);
        expected.put("p1", nullability("p1", false, "p1.a", false, "p1.b", false));
        ddlTypes.put("p2", P2_DDL_TYPE);
        expected.put("p2", nullability("p2", false, "p2.a", false, "p2.n", false, "p2.n.d", false, "p2.n.e", true));
        ddlTypes.put("p6", P6_DDL_TYPE);
        expected.put("p6", nullability("p6", true, "p6.1", false, "p6.2", true));
        ddlTypes.put("p8", P8_DDL_TYPE);
        expected.put("p8", nullability("p8", true, "p8.a b", false, "p8.c,d", true));
        String columnsDdl = ddlTypes.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
        NullableRoundTrip.createTable(chc, tableName, Twin.PLAIN.ddl(columnsDdl));
        try {
            Map<String, Map<String, Boolean>> parsed = new LinkedHashMap<>();
            Map<String, String> rawTypes = new LinkedHashMap<>();
            for (ClickHouseFieldDescriptor row : NullableRoundTrip.describeRows(chc, tableName)) {
                if (row.isSubcolumn() || !expected.containsKey(row.getName())) {
                    continue;
                }
                String normalised = DeclaredNullability.normalise(row.getType());
                LOGGER.info(
                        "{} approach {} | raw={} | normalised={}",
                        MATRIX_LOG_PREFIX,
                        row.getName(),
                        row.getType(),
                        normalised);
                rawTypes.put(row.getName(), row.getType());
                Map<String, Boolean> flat = new LinkedHashMap<>();
                flatten(row.getName(), ClickHouseColumn.of(row.getName(), normalised), flat);
                parsed.put(row.getName(), flat);
            }
            assertEquals(expected, parsed, "parsed declared nullability must equal the DDL's");
            for (String multiLine : List.of("p1", "p2")) {
                assertTrue(
                        rawTypes.get(multiLine).contains(DeclaredNullability.ESCAPED_NEWLINE),
                        "the non-nullable " + multiLine + " must reach the parser as the escaped multi-line form");
            }
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @ParameterizedTest(name = "COLS-10 omitted Nullable(Tuple) field stores NULL {0}")
    @EnumSource(Twin.class)
    void omittedNullableTupleFieldStoresNull(Twin twin, TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName(TABLE_PREFIX + "omitted_" + twin);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        NullableRoundTrip.createTable(chc, tableName, twin.ddl(CELL_COLUMN + " " + C1_DDL_TYPE));
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        try {
            Schema schema = SchemaBuilder.struct()
                    .field(ROW_ID_COLUMN, Schema.INT32_SCHEMA)
                    .build();
            int id = 1;
            SinkRecord record = new SinkRecord(
                    tableName, 0, null, null, schema, new Struct(schema).put(ROW_ID_COLUMN, id), id);

            NullableRoundTrip.putAndStop(props, List.of(record));

            NullableRoundTrip.selectAndAssert(
                    chc,
                    tableName,
                    id,
                    withDefaultColumn(List.of(cell("c_is_null", "isNull(c)", TRUE_TEXT)), twin));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @ParameterizedTest(
            name = "COLS-13 (D-12) live: a backslash-n in a Tuple element name round-trips without a fallback WARN {0}")
    @EnumSource(Twin.class)
    void backslashNInTupleElementNameRoundTripsWithoutFallback(Twin twin, TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName(TABLE_PREFIX + "bsn_" + twin);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        NullableRoundTrip.createTable(chc, tableName, twin.ddl(CELL_COLUMN + " " + NULLABLE_BACKSLASH_N_NAME_DDL));
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        try {
            for (ClickHouseFieldDescriptor row : NullableRoundTrip.describeRows(chc, tableName)) {
                if (CELL_COLUMN.equals(row.getName())) {
                    assertEquals(NULLABLE_BACKSLASH_N_NAME_RAW, row.getType(), PREMISE_MESSAGE);
                }
            }
            try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
                Table table = chc.describeTable(chc.getDatabase(), tableName);

                assertEquals(List.of(), log.events("WARN"), "the declared-nullability pass must not fall back");
                Map<String, Column> columns = table.getAllColumnsMap();
                assertTrue(columns.get(CELL_COLUMN).isNullable());
                assertFalse(columns
                        .get(CELL_COLUMN + PATH_SEPARATOR + BACKSLASH_N_ELEMENT)
                        .isNullable());
            }

            Schema cellSchema = SchemaBuilder.struct()
                    .optional()
                    .field(BACKSLASH_N_ELEMENT, Schema.INT32_SCHEMA)
                    .field(TUPLE_STRING_ELEMENT, Schema.STRING_SCHEMA)
                    .build();
            Schema schema = SchemaBuilder.struct()
                    .field(ROW_ID_COLUMN, Schema.INT32_SCHEMA)
                    .field(CELL_COLUMN, cellSchema)
                    .build();
            int id = 1;
            Struct cellValue = new Struct(cellSchema).put(BACKSLASH_N_ELEMENT, 7).put(TUPLE_STRING_ELEMENT, "nm");
            SinkRecord record = new SinkRecord(
                    tableName, 0, null, null, schema, new Struct(schema).put(ROW_ID_COLUMN, id).put(CELL_COLUMN, cellValue), id);

            NullableRoundTrip.putAndStop(props, List.of(record));

            NullableRoundTrip.selectAndAssert(
                    chc,
                    tableName,
                    id,
                    withDefaultColumn(
                            List.of(
                                    cell("c_a", "tupleElement(c, 1)", "7"),
                                    cell("c_d", "tupleElement(c, 2)", "nm")),
                            twin));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @Test
    @DisplayName(
            "COLS-13 (D-12) live: DESCRIBE returns the captured escaping for every Docker-free backslash-n literal, and quoted names pair without a fallback WARN")
    void describeReturnsTheCapturedBackslashNEscapingAndPairs() throws Exception {
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        String tableName = createTopicName(TABLE_PREFIX + "bsn_drift");
        ClickHouseTestHelpers.dropTable(chc, tableName);
        // Root column -> DDL type, and the DESCRIBE text each Docker-free literal claims for it.
        Map<String, String> ddlTypes = new LinkedHashMap<>();
        Map<String, String> expectedRawTypes = new LinkedHashMap<>();
        ddlTypes.put("p", BACKSLASH_N_NAME_DDL);
        expectedRawTypes.put("p", BACKSLASH_N_NAME_RAW);
        ddlTypes.put("r", REAL_NEWLINE_NAME_DDL);
        expectedRawTypes.put("r", REAL_NEWLINE_NAME_RAW);
        ddlTypes.put("q", BACKSLASH_N_ENUM_LABEL_DDL);
        expectedRawTypes.put("q", BACKSLASH_N_ENUM_LABEL_RAW);
        ddlTypes.put("n", BACKSLASH_N_NESTED_NAMES_DDL);
        expectedRawTypes.put("n", BACKSLASH_N_NESTED_NAMES_RAW);
        ddlTypes.put("s", TRAILING_BACKSLASH_NAMES_DDL);
        expectedRawTypes.put("s", TRAILING_BACKSLASH_NAMES_RAW);
        // Wrappers: no raw literal was captured for them, so only their pairing is checked.
        ddlTypes.put("w", "Array(Tuple(`a\\\\nb` Int32, d Nullable(String)))");
        ddlTypes.put("m", "Map(String, Tuple(`a\\\\nb` Int32, d Nullable(String)))");
        String columnsDdl = ddlTypes.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
        NullableRoundTrip.createTable(chc, tableName, Twin.PLAIN.ddl(columnsDdl));
        try {
            Map<String, String> rawTypes = new LinkedHashMap<>();
            for (ClickHouseFieldDescriptor row : NullableRoundTrip.describeRows(chc, tableName)) {
                if (!row.isSubcolumn() && expectedRawTypes.containsKey(row.getName())) {
                    rawTypes.put(row.getName(), row.getType());
                }
            }
            assertEquals(expectedRawTypes, rawTypes, PREMISE_MESSAGE);

            try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
                Table table = chc.describeTable(chc.getDatabase(), tableName);

                assertEquals(List.of(), log.events("WARN"), "the declared-nullability pass must not fall back");
                assertTrue(table.getAllColumnsMap().get("n.p\\\\nq.t").isNullable());
            }
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @Test
    @DisplayName("COLS-09: server version is recorded")
    void serverVersionIsRecorded() throws Exception {
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        String version = NullableRoundTrip.serverVersion(chc);
        LOGGER.info("{} server version={}", MATRIX_LOG_PREFIX, version);
        assertTrue(version != null && !version.isEmpty());
        assertFalse(version.isBlank());
    }
}
