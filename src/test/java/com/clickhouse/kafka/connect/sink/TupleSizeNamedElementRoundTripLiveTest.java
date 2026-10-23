package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.kafka.connect.sink.NullableRoundTrip.Cell;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureCatalog;
import com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureCatalog.Fixture;
import com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullability;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.helper.RowSnapshots;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
 * DESC-09 (backlog 999.4): the live half of the size-named Tuple element fix. Every table of the
 * committed DESCRIBE catalog that the sink can write (all but {@value #EXCLUDED_KEY}) is created
 * before {@code start()}, receives its records through the real {@link ClickHouseSinkTask} on the
 * default RowBinary route, and is read back typed: the whole value text ({@code toString} of the
 * column) must equal what was written, so a dropped, swapped or misplaced element fails the case.
 * The affected shapes (a declared element named {@code size}, {@code size0} or {@code size<digits>})
 * must now store and match; the look-alikes, the over-flag shape {@code s4_o01} and the structural
 * {@code n05} must still store and match. {@code r01} holds the crafted value of the gg3 review (a
 * string whose bytes shift a short write into a wrong size) and is compared by hex.
 *
 * <p>Each case also asserts that discovery of its table, during the sink task and in a second direct
 * {@code describeTable}, logs no WARN on the helper or {@link DeclaredNullability} loggers (D-10).
 * Every case prints one metadata-only line ({@value #LOG_PREFIX}: key, outcome, server code name,
 * row count), never a record value, so the same class can be run unchanged on an older tree to
 * measure what that tree did.
 */
public class TupleSizeNamedElementRoundTripLiveTest extends ClickHouseBase {

    static final String LOG_PREFIX = "TUPLE_SIZE_ROUND_TRIP";

    /** A JSON column: DESC-05 drops its typed-path rows by design, so there is no tuple to round-trip. */
    static final String EXCLUDED_KEY = "x08";

    private static final String EXCLUDED_REASON = "JSON column (DESC-05), pinned by DescribeTableBuilderUnitTest and the "
            + "DESC-05 rows of ClickHouseHelperClientForkLiveTest; its insert route is outside backlog 999.4";
    private static final String ROW_ID_COLUMN = "id";
    private static final String CONNECTOR_NAME_PROP = "name";
    private static final String WARN_LEVEL = "WARN";
    private static final String ROW_COUNT_ALIAS = "n";
    private static final String ROW_COUNT_QUERY_FORMAT = "SELECT count() AS " + ROW_COUNT_ALIAS + " FROM `%s`";
    private static final String CREATE_TABLE_FORMAT_PLACEHOLDER = "`%s`";
    private static final String ALIAS_PREFIX = "v_";
    private static final String TO_STRING_FORMAT = "toString(`%s`)";
    private static final String OUTCOME_FAILED_FORMAT = LOG_PREFIX + " key=%s outcome=failed code=%s rows=%s";
    private static final String OUTCOME_MISMATCH_FORMAT = LOG_PREFIX + " key=%s outcome=stored match=no";
    private static final String OUTCOME_MATCH_FORMAT = LOG_PREFIX + " key=%s outcome=stored match=yes rows=%s";
    private static final String UNKNOWN_ROWS = "unreadable";
    private static final List<String> READ_PAST_END_CODES =
            List.of("ATTEMPT_TO_READ_AFTER_EOF", "CANNOT_READ_ALL_DATA");

    private static final int FIRST_ID = 1;
    private static final int SECOND_ID = 2;
    private static final String COLUMN = "c";
    private static final String NAME = "nm";
    private static final String MAP_KEY = "k";
    private static final String PAIR_TEXT = "(7,'nm')";
    // UInt8, UInt16 and UInt32 elements travel as Connect INT32, UInt64 as INT64.
    private static final Schema NARROW = Schema.INT32_SCHEMA;
    private static final Schema WIDE = Schema.INT64_SCHEMA;
    private static final Schema TEXT = Schema.STRING_SCHEMA;
    private static final Schema ROOT_UINT8 = Schema.INT8_SCHEMA;

    /** The crafted R-01 string: its bytes make a short write shift the size by the length prefix. */
    private static final String CRAFTED_STRING = "abc" + '\u0003' + "xyz";

    private static final String CRAFTED_STRING_HEX = "6162630378797A";

    /** One record of a case: the id, the value struct and what a correct typed read-back shows. */
    private record CaseRecord(int id, Struct value, List<Cell> readBack) {}

    /** One catalog table, the schema of its records and the records themselves. */
    private record RoundTripCase(String key, Schema valueSchema, List<CaseRecord> records) {}

    // ---- schema and value helpers ----

    private static Schema struct(Object... nameAndSchema) {
        SchemaBuilder builder = SchemaBuilder.struct();
        for (int i = 0; i < nameAndSchema.length; i += 2) {
            builder.field((String) nameAndSchema[i], (Schema) nameAndSchema[i + 1]);
        }
        return builder.build();
    }

    private static Schema array(Schema element) {
        return SchemaBuilder.array(element).build();
    }

    private static Schema map(Schema value) {
        return SchemaBuilder.map(Schema.STRING_SCHEMA, value).build();
    }

    /** The record schema: the id plus the fixture's own columns. */
    private static Schema table(Object... columnAndSchema) {
        Object[] withId = new Object[columnAndSchema.length + 2];
        withId[0] = ROW_ID_COLUMN;
        withId[1] = Schema.INT32_SCHEMA;
        System.arraycopy(columnAndSchema, 0, withId, 2, columnAndSchema.length);
        return struct(withId);
    }

    private static Struct value(Schema schema, Object... nameAndValue) {
        Struct struct = new Struct(schema);
        for (int i = 0; i < nameAndValue.length; i += 2) {
            struct.put((String) nameAndValue[i], nameAndValue[i + 1]);
        }
        return struct;
    }

    private static Cell cell(String column, String expectedText) {
        return new Cell(
                ALIAS_PREFIX + column.replace('.', '_'), String.format(TO_STRING_FORMAT, column), expectedText);
    }

    /** One record from triples of (column, Connect value, expected {@code toString} of the column). */
    private static CaseRecord record(Schema schema, int id, Object... columnValueText) {
        Struct struct = new Struct(schema).put(ROW_ID_COLUMN, id);
        List<Cell> cells = new ArrayList<>();
        for (int i = 0; i < columnValueText.length; i += 3) {
            String column = (String) columnValueText[i];
            struct.put(column, columnValueText[i + 1]);
            cells.add(cell(column, (String) columnValueText[i + 2]));
        }
        return new CaseRecord(id, struct, cells);
    }

    /** The single column {@code c} of most fixtures: one record, value and expected text. */
    private static CaseRecord single(Schema schema, int id, Object value, String text) {
        return record(schema, id, COLUMN, value, text);
    }

    private static RoundTripCase caseOf(String key, Schema schema, CaseRecord... records) {
        return new RoundTripCase(key, schema, List.of(records));
    }

    // ---- the 46 cases ----

    private static List<RoundTripCase> allCases() {
        List<RoundTripCase> cases = new ArrayList<>();
        cases.addAll(affectedCases());
        cases.addAll(lookAlikeCases());
        cases.addAll(spikeAndExtraCases());
        return cases;
    }

    private static List<RoundTripCase> affectedCases() {
        List<RoundTripCase> cases = new ArrayList<>();

        Schema sizeV = struct("size", NARROW, "v", TEXT);
        Schema a01 = table(COLUMN, sizeV);
        cases.add(caseOf(
                "s4_a01",
                a01,
                // 300 does not fit a UInt8, so a narrowed element would read back differently (precision).
                single(a01, FIRST_ID, value(sizeV, "size", 300, "v", NAME), "(300,'nm')"),
                single(a01, SECOND_ID, value(sizeV, "size", 0, "v", ""), "(0,'')")));

        Schema size0V = struct("size0", NARROW, "v", TEXT);
        Schema a02 = table(COLUMN, size0V);
        cases.add(caseOf("s4_a02", a02, single(a02, FIRST_ID, value(size0V, "size0", 7, "v", NAME), PAIR_TEXT)));

        // s4_a03 is Nullable(Tuple(size UInt8, v String)): the record schema is the same as s4_a01's.
        Schema a03 = table(COLUMN, sizeV);
        cases.add(caseOf("s4_a03", a03, single(a03, FIRST_ID, value(sizeV, "size", 7, "v", NAME), PAIR_TEXT)));

        Schema a04 = table(COLUMN, array(sizeV));
        cases.add(caseOf(
                "s4_a04",
                a04,
                single(a04, FIRST_ID, List.of(value(sizeV, "size", 7, "v", NAME)), "[(7,'nm')]"),
                single(a04, SECOND_ID, List.of(), "[]")));

        Schema a05 = table(COLUMN, map(sizeV));
        cases.add(caseOf(
                "s4_a05",
                a05,
                single(a05, FIRST_ID, Map.of(MAP_KEY, value(sizeV, "size", 7, "v", NAME)), "{'k':(7,'nm')}")));

        Schema xSizeV = struct("x", sizeV);
        Schema a06 = table(COLUMN, xSizeV);
        cases.add(caseOf(
                "s4_a06",
                a06,
                single(a06, FIRST_ID, value(xSizeV, "x", value(sizeV, "size", 7, "v", NAME)), "((7,'nm'))")));

        Schema inner0 = struct("size0", NARROW);
        Schema xArray = struct("x", array(inner0));
        Schema a07 = table(COLUMN, xArray);
        cases.add(caseOf(
                "s4_a07",
                a07,
                single(a07, FIRST_ID, value(xArray, "x", List.of(value(inner0, "size0", 7))), "([(7)])")));

        Schema size1V = struct("size1", NARROW, "v", TEXT);
        Schema a08 = table(COLUMN, array(size1V));
        cases.add(caseOf(
                "s4_a08",
                a08,
                single(a08, FIRST_ID, List.of(value(size1V, "size1", 7, "v", NAME)), "[(7,'nm')]")));

        Schema size12V = struct("size12", NARROW, "v", TEXT);
        Schema a09 = table(COLUMN, array(size12V));
        cases.add(caseOf(
                "s4_a09",
                a09,
                single(a09, FIRST_ID, List.of(value(size12V, "size12", 7, "v", NAME)), "[(7,'nm')]")));

        Schema a10 = table(COLUMN, array(array(size1V)));
        cases.add(caseOf(
                "s4_a10",
                a10,
                single(a10, FIRST_ID, List.of(List.of(value(size1V, "size1", 7, "v", NAME))), "[[(7,'nm')]]")));

        Schema xSize1Array = struct("x", array(size1V));
        Schema a11 = table(COLUMN, xSize1Array);
        cases.add(caseOf(
                "s4_a11",
                a11,
                single(
                        a11,
                        FIRST_ID,
                        value(xSize1Array, "x", List.of(value(size1V, "size1", 7, "v", NAME))),
                        "([(7,'nm')])")));

        Schema xSize1 = struct("x", size1V);
        Schema a12 = table(COLUMN, array(xSize1));
        cases.add(caseOf(
                "s4_a12",
                a12,
                single(
                        a12,
                        FIRST_ID,
                        List.of(value(xSize1, "x", value(size1V, "size1", 7, "v", NAME))),
                        "[((7,'nm'))]")));

        Schema vSize1 = struct("v", TEXT, "size1", NARROW);
        Schema a13 = table(COLUMN, array(vSize1));
        cases.add(caseOf(
                "s4_a13",
                a13,
                single(a13, FIRST_ID, List.of(value(vSize1, "v", NAME, "size1", 7)), "[('nm',7)]")));
        return cases;
    }

    private static List<RoundTripCase> lookAlikeCases() {
        List<RoundTripCase> cases = new ArrayList<>();

        Schema size1V = struct("size1", NARROW, "v", TEXT);
        Schema u01 = table(COLUMN, size1V);
        cases.add(caseOf("s4_u01", u01, single(u01, FIRST_ID, value(size1V, "size1", 7, "v", NAME), PAIR_TEXT)));
        cases.add(caseOf("s4_u02", u01, single(u01, FIRST_ID, value(size1V, "size1", 7, "v", NAME), PAIR_TEXT)));

        Schema u03 = table(COLUMN, map(size1V));
        cases.add(caseOf(
                "s4_u03",
                u03,
                single(u03, FIRST_ID, Map.of(MAP_KEY, value(size1V, "size1", 7, "v", NAME)), "{'k':(7,'nm')}")));

        Schema aSize1 = struct("a", array(NARROW), "size1", NARROW);
        Schema u04 = table(COLUMN, aSize1);
        cases.add(caseOf(
                "s4_u04", u04, single(u04, FIRST_ID, value(aSize1, "a", List.of(9), "size1", 7), "([9],7)")));

        String[] lookAlikeNames = {"sizes", "size_x", "size_1", "sizeA", "Size", "xsize", "my size"};
        for (int i = 0; i < lookAlikeNames.length; i++) {
            Schema lookAlike = struct(lookAlikeNames[i], NARROW, "v", TEXT);
            Schema schema = table(COLUMN, lookAlike);
            cases.add(caseOf(
                    String.format("s4_u%02d", 5 + i),
                    schema,
                    single(schema, FIRST_ID, value(lookAlike, lookAlikeNames[i], 7, "v", NAME), PAIR_TEXT)));
        }

        // A root column is checked against the Connect type width by validateDataSchema, so a UInt8
        // column takes INT8 here; the INT32 used inside tuples would be refused at the root.
        Schema u12 = table("size", ROOT_UINT8);
        cases.add(caseOf("s4_u12", u12, record(u12, FIRST_ID, "size", (byte) 7, "7")));
        Schema u13 = table("size0", ROOT_UINT8);
        cases.add(caseOf("s4_u13", u13, record(u13, FIRST_ID, "size0", (byte) 7, "7")));
        Schema u14 = table("size", array(NARROW));
        cases.add(caseOf("s4_u14", u14, record(u14, FIRST_ID, "size", List.of(7), "[7]")));
        Schema u15 = table("n.size", array(NARROW), "n.v", array(TEXT));
        cases.add(caseOf(
                "s4_u15", u15, record(u15, FIRST_ID, "n.size", List.of(7), "[7]", "n.v", List.of(NAME), "['nm']")));
        Schema u16 = table(COLUMN, TEXT);
        cases.add(caseOf("s4_u16", u16, single(u16, FIRST_ID, "size", "size")));

        Schema eV = struct("e", TEXT, "v", TEXT);
        Schema u17 = table(COLUMN, array(eV));
        cases.add(caseOf(
                "s4_u17",
                u17,
                single(u17, FIRST_ID, List.of(value(eV, "e", "x, size1 UInt8", "v", NAME)), "[('x, size1 UInt8','nm')]")));
        Schema u18 = table(COLUMN, eV);
        cases.add(caseOf(
                "s4_u18", u18, single(u18, FIRST_ID, value(eV, "e", "x, size UInt8", "v", NAME), "('x, size UInt8','nm')")));

        Schema positional = struct("1", NARROW, "2", TEXT);
        Schema u19 = table(COLUMN, positional);
        cases.add(caseOf("s4_u19", u19, single(u19, FIRST_ID, value(positional, "1", 7, "2", NAME), PAIR_TEXT)));

        Schema size1Only = struct("size1", NARROW);
        Schema o01 = table(COLUMN, array(map(size1Only)));
        cases.add(caseOf(
                "s4_o01",
                o01,
                single(o01, FIRST_ID, List.of(Map.of(MAP_KEY, value(size1Only, "size1", 7))), "[{'k':(7)}]")));
        return cases;
    }

    private static List<RoundTripCase> spikeAndExtraCases() {
        List<RoundTripCase> cases = new ArrayList<>();

        Schema size1Size0 = struct("size1", NARROW, "size0", NARROW);
        Schema n01 = table(COLUMN, array(array(size1Size0)));
        cases.add(caseOf(
                "n01",
                n01,
                single(n01, FIRST_ID, List.of(List.of(value(size1Size0, "size1", 7, "size0", 8))), "[[(7,8)]]")));

        Schema aOnly = struct("a", NARROW);
        Schema size0Tuple = struct("size0", aOnly, "v", TEXT);
        Schema n02 = table(COLUMN, array(size0Tuple));
        cases.add(caseOf(
                "n02",
                n02,
                single(
                        n02,
                        FIRST_ID,
                        List.of(value(size0Tuple, "size0", value(aOnly, "a", 7), "v", NAME)),
                        "[((7),'nm')]")));

        Schema size0Size = struct("size0", TEXT, "size", TEXT);
        Schema n03 = table(COLUMN, array(size0Size));
        cases.add(caseOf(
                "n03",
                n03,
                single(n03, FIRST_ID, List.of(value(size0Size, "size0", "ab", "size", "cd")), "[('ab','cd')]")));

        Schema sizeSize0 = struct("size", NARROW, "size0", NARROW);
        Schema n04 = table(COLUMN, array(map(sizeSize0)));
        cases.add(caseOf(
                "n04",
                n04,
                single(
                        n04,
                        FIRST_ID,
                        List.of(Map.of(MAP_KEY, value(sizeSize0, "size", 7, "size0", 8))),
                        "[{'k':(7,8)}]")));

        Schema keysValues = struct("keys", NARROW, "values", TEXT);
        Schema n05 = table(COLUMN, keysValues);
        cases.add(caseOf("n05", n05, single(n05, FIRST_ID, value(keysValues, "keys", 7, "values", NAME), PAIR_TEXT)));

        Schema abTuple = struct("a", NARROW, "b", TEXT);
        Schema sizeAb = struct("size", abTuple);
        Schema x01 = table(COLUMN, sizeAb);
        cases.add(caseOf(
                "x01",
                x01,
                single(x01, FIRST_ID, value(sizeAb, "size", value(abTuple, "a", 7, "b", NAME)), "((7,'nm'))")));

        Schema sizeA = struct("size", aOnly, "v", TEXT);
        Schema x02 = table(COLUMN, array(sizeA));
        cases.add(caseOf(
                "x02",
                x02,
                single(
                        x02,
                        FIRST_ID,
                        List.of(value(sizeA, "size", value(aOnly, "a", 7), "v", NAME)),
                        "[((7),'nm')]")));

        Schema sizeSize0Text = struct("size", NARROW, "size0", TEXT);
        Schema x03 = table("size", sizeSize0Text);
        cases.add(caseOf(
                "x03",
                x03,
                record(x03, FIRST_ID, "size", value(sizeSize0Text, "size", 7, "size0", NAME), PAIR_TEXT)));

        // 4294967296 does not fit 32 bits: the same decimal text proves the UInt64 element kept its width.
        Schema size1WideV = struct("size1", WIDE, "v", TEXT);
        Schema x04 = table(COLUMN, array(size1WideV));
        cases.add(caseOf(
                "x04",
                x04,
                single(
                        x04,
                        FIRST_ID,
                        List.of(value(size1WideV, "size1", 4294967296L, "v", NAME)),
                        "[(4294967296,'nm')]")));

        Schema wideSizes = struct("size1", WIDE, "size0", WIDE);
        Schema x05 = table(COLUMN, array(array(wideSizes)));
        cases.add(caseOf(
                "x05",
                x05,
                single(x05, FIRST_ID, List.of(List.of(value(wideSizes, "size1", 7L, "size0", 8L))), "[[(7,8)]]")));

        Schema size0V = struct("size0", NARROW, "v", TEXT);
        Schema x06 = table(COLUMN, array(size0V));
        cases.add(caseOf(
                "x06",
                x06,
                single(x06, FIRST_ID, List.of(value(size0V, "size0", 7, "v", NAME)), "[(7,'nm')]")));

        Schema sizeV = struct("size", NARROW, "v", TEXT);
        Schema x07 = table(COLUMN, map(array(sizeV)));
        cases.add(caseOf(
                "x07",
                x07,
                single(x07, FIRST_ID, Map.of(MAP_KEY, List.of(value(sizeV, "size", 7, "v", NAME))), "{'k':[(7,'nm')]}")));

        Schema r01 = table(COLUMN, sizeV);
        cases.add(caseOf(
                "r01",
                r01,
                new CaseRecord(
                        FIRST_ID,
                        new Struct(r01)
                                .put(ROW_ID_COLUMN, FIRST_ID)
                                .put(COLUMN, value(sizeV, "size", 7, "v", CRAFTED_STRING)),
                        List.of(
                                new Cell("v_size", "tupleElement(c, 'size')", "7"),
                                new Cell("v_v_hex", "hex(tupleElement(c, 'v'))", CRAFTED_STRING_HEX)))));
        return cases;
    }

    /** Every catalog key but the excluded one, in catalog order; a key without a case is an error. */
    static Stream<Arguments> cases() {
        Map<String, RoundTripCase> byKey = new LinkedHashMap<>();
        for (RoundTripCase roundTripCase : allCases()) {
            if (byKey.put(roundTripCase.key(), roundTripCase) != null) {
                throw new IllegalStateException("two round-trip cases for catalog key " + roundTripCase.key());
            }
        }
        List<Arguments> arguments = new ArrayList<>();
        for (Fixture fixture : DescribeFixtureCatalog.FIXTURES) {
            if (EXCLUDED_KEY.equals(fixture.key())) {
                continue;
            }
            RoundTripCase roundTripCase = byKey.remove(fixture.key());
            if (roundTripCase == null) {
                throw new IllegalStateException("catalog key " + fixture.key() + " has no round-trip case; the only"
                        + " exclusion is " + EXCLUDED_KEY + ": " + EXCLUDED_REASON);
            }
            arguments.add(Arguments.of(fixture.key(), roundTripCase));
        }
        if (!byKey.isEmpty()) {
            throw new IllegalStateException("round-trip cases for keys that are not in the catalog: " + byKey.keySet());
        }
        return arguments.stream();
    }

    // ---- the test ----

    @ParameterizedTest(name = "DESC-09 round-trip: {0}")
    @MethodSource("cases")
    void everyCatalogShapeStoresAndReadsBackEqualWithoutAWarning(
            String key, RoundTripCase roundTripCase, TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        Fixture fixture = DescribeFixtureCatalog.fixture(key);
        try (ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props)) {
            try {
                ClickHouseTestHelpers.dropTable(chc, key);
                // The table must exist before start(): ClickHouseWriter.start() fails against an empty database.
                NullableRoundTrip.createTable(
                        chc, key, DescribeFixtureCatalog.createTableSql(fixture, CREATE_TABLE_FORMAT_PLACEHOLDER));
                props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
                try (LogCapture helperLog = LogCapture.attach(ClickHouseHelperClient.class);
                        LogCapture nullabilityLog = LogCapture.attach(DeclaredNullability.class)) {
                    putAll(props, chc, roundTripCase);
                    assertReadBackPrintingOutcome(chc, roundTripCase);

                    chc.describeTable(chc.getDatabase(), key);
                    assertReadBack(chc, roundTripCase);

                    assertEquals(
                            List.of(), warnMessages(helperLog), "discovery of " + key + " must not warn (helper logger)");
                    assertEquals(
                            List.of(),
                            warnMessages(nullabilityLog),
                            "discovery of " + key + " must not warn (nullability)");
                }
            } finally {
                ClickHouseTestHelpers.dropTable(chc, key);
            }
        }
    }

    private static void putAll(Map<String, String> props, ClickHouseHelperClient chc, RoundTripCase roundTripCase)
            throws Exception {
        List<SinkRecord> records = roundTripCase.records().stream()
                .map(record -> new SinkRecord(
                        roundTripCase.key(),
                        0,
                        null,
                        null,
                        roundTripCase.valueSchema(),
                        record.value(),
                        record.id()))
                .collect(Collectors.toList());
        try {
            NullableRoundTrip.putAndStop(props, records);
        } catch (RuntimeException | Error failure) {
            System.out.println(String.format(
                    OUTCOME_FAILED_FORMAT, roundTripCase.key(), failureCode(failure), rowCount(chc, roundTripCase.key())));
            throw failure;
        }
    }

    private static void assertReadBackPrintingOutcome(ClickHouseHelperClient chc, RoundTripCase roundTripCase)
            throws Exception {
        try {
            assertReadBack(chc, roundTripCase);
        } catch (AssertionError mismatch) {
            System.out.println(String.format(OUTCOME_MISMATCH_FORMAT, roundTripCase.key()));
            throw mismatch;
        }
        System.out.println(
                String.format(OUTCOME_MATCH_FORMAT, roundTripCase.key(), rowCount(chc, roundTripCase.key())));
    }

    private static void assertReadBack(ClickHouseHelperClient chc, RoundTripCase roundTripCase) throws Exception {
        for (CaseRecord record : roundTripCase.records()) {
            NullableRoundTrip.selectAndAssert(chc, roundTripCase.key(), record.id(), record.readBack());
        }
    }

    private static List<String> warnMessages(LogCapture capture) {
        return capture.events(WARN_LEVEL).stream()
                .map(LogCapture.CapturedEvent::message)
                .collect(Collectors.toList());
    }

    private static String rowCount(ClickHouseHelperClient chc, String key) {
        try {
            return RowSnapshots.select(chc, String.format(ROW_COUNT_QUERY_FORMAT, key), ROW_COUNT_ALIAS)
                    .get(0)
                    .get(ROW_COUNT_ALIAS);
        } catch (Exception e) {
            return UNKNOWN_ROWS;
        }
    }

    /** The server code the failure names, else the root exception's simple class name. */
    private static String failureCode(Throwable failure) {
        String messages = NullableRoundTrip.causeChainMessages(failure);
        for (String code : READ_PAST_END_CODES) {
            if (messages.contains(code)) {
                return code;
            }
        }
        Throwable root = failure;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName();
    }
}
