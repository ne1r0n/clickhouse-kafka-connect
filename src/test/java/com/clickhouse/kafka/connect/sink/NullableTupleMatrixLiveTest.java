package com.clickhouse.kafka.connect.sink;

import static com.clickhouse.kafka.connect.sink.NullableRoundTrip.DEFAULT_VALUE_TEXT;
import static com.clickhouse.kafka.connect.sink.NullableRoundTrip.FALSE_TEXT;
import static com.clickhouse.kafka.connect.sink.NullableRoundTrip.TRUE_TEXT;
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
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.helper.RowSnapshots;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
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
 * COLS-09 spike for backlog 999.1 (D-05..D-07): which {@code Nullable(Tuple(...))} shapes the
 * connector maps correctly. Side (a) pins what the DESCRIBE-derived mapping does today; side (b)
 * proves the unchanged writer round-trips once the declared element nullability is right. On go
 * (plan 19-04) side (a)'s expectations flip to round-trips and the class becomes COLS-10 coverage;
 * on no-go it stays as the record of what fails. Deciding cells are C1-C3, X cells are
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
    private static final String PATH_SEPARATOR = ".";
    // Server error name when the bytes of a doubly flagged tuple run short of what the type reads.
    private static final String SERVER_CANNOT_READ_ALL_DATA = "CANNOT_READ_ALL_DATA";
    private static final String WRITER_NULL_INTO_NOT_NULLABLE = "An attempt to write null into not nullable column";

    /** One record of a cell: the Kafka value of column {@code c} and what a correct read-back shows. */
    private record RowCase(int id, Object value, List<Cell> readBack) {}

    /** The pinned outcome of one row put through today's DESCRIBE-derived mapping. */
    private record Expect(Kind kind, List<String> causeSubstrings, List<Cell> observed) {

        enum Kind {
            ROUND_TRIPS,
            FAILS,
            STORES_WRONG
        }

        static Expect roundTrips() {
            return new Expect(Kind.ROUND_TRIPS, List.of(), List.of());
        }

        static Expect fails(String... causeSubstrings) {
            return new Expect(Kind.FAILS, List.of(causeSubstrings), List.of());
        }

        static Expect storesWrong(List<Cell> observed) {
            return new Expect(Kind.STORES_WRONG, List.of(), observed);
        }
    }

    /**
     * One matrix cell. {@code declaredColumn} builds the hand-built root Column named by its argument,
     * carrying the nullability the DDL declares; {@code currentOutcome} maps each row id to side
     * (a)'s pinned expectation.
     */
    private record MatrixCell(
            String key,
            boolean deciding,
            String ddlType,
            Schema fieldSchema,
            Function<String, Column> declaredColumn,
            List<RowCase> rows,
            Map<Integer, Expect> currentOutcome) {}

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
        Map<Integer, Expect> currentOutcome = Map.of(
                1, Expect.fails(SERVER_CANNOT_READ_ALL_DATA),
                2, Expect.fails(WRITER_NULL_INTO_NOT_NULLABLE));
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

    private static List<MatrixCell> cells() {
        return List.of(c1());
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
                Expect expect = matrixCell.currentOutcome().get(row.id());
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
                        NullableRoundTrip.selectAndAssert(chc, tableName, row.id(), expect.observed());
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
            CapturingClickHouseWriter.Capture capture = captureBatch(
                    table,
                    new ClickHouseSinkConfig(getBaseProps()),
                    sinkRecords(tableName, matrixCell, matrixCell.rows()),
                    matrixCell.key());
            assertEquals(twin.hasDefaults(), capture.supportDefaults(), "the writer must take the twin's format");
            NullableRoundTrip.insertCaptured(chc, tableName, capture);
            for (RowCase row : matrixCell.rows()) {
                NullableRoundTrip.selectAndAssert(
                        chc, tableName, row.id(), withDefaultColumn(row.readBack(), twin));
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
            LOGGER.info("{} control {} failed as expected", MATRIX_LOG_PREFIX, twin);
            assertNotNull(failure);
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

    @Test
    @DisplayName(
            "COLS-09 approach: declared Tuple types parse with ClickHouseColumn.of after normalisation and carry the DDL's element nullability")
    void declaredTupleTypesParseWithDdlNullability() throws Exception {
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        String tableName = createTopicName(TABLE_PREFIX + "approach");
        ClickHouseTestHelpers.dropTable(chc, tableName);
        Map<String, Map<String, Boolean>> expected = new LinkedHashMap<>();
        expected.put("c1", Map.of("c1", true, "c1.a", false, "c1.b", false));
        expected.put("p1", Map.of("p1", false, "p1.a", false, "p1.b", false));
        NullableRoundTrip.createTable(
                chc, tableName, Twin.PLAIN.ddl("c1 " + C1_DDL_TYPE + ", p1 " + P1_DDL_TYPE));
        try {
            Map<String, Map<String, Boolean>> parsed = new LinkedHashMap<>();
            for (ClickHouseFieldDescriptor row : NullableRoundTrip.describeRows(chc, tableName)) {
                if (row.isSubcolumn() || !expected.containsKey(row.getName())) {
                    continue;
                }
                String normalised = NullableRoundTrip.normaliseDeclaredType(row.getType());
                LOGGER.info(
                        "{} approach {} | raw={} | normalised={}",
                        MATRIX_LOG_PREFIX,
                        row.getName(),
                        row.getType(),
                        normalised);
                Map<String, Boolean> flat = new LinkedHashMap<>();
                flatten(row.getName(), ClickHouseColumn.of(row.getName(), normalised), flat);
                parsed.put(row.getName(), flat);
            }
            assertEquals(expected, parsed, "parsed declared nullability must equal the DDL's");
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
