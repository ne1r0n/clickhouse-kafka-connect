package com.clickhouse.kafka.connect.sink.db.mapping;

import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.BACKSLASH_N_ELEMENT;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.BACKSLASH_N_ELEMENT_NAME;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.BACKSLASH_N_ENUM_LABEL_RAW;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.BACKSLASH_N_NAME_RAW;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.BACKSLASH_N_NESTED_NAMES_RAW;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.NULLABLE_BACKSLASH_N_NAME_RAW;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.REAL_NEWLINE_ELEMENT;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.REAL_NEWLINE_ELEMENT_NAME;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.REAL_NEWLINE_NAME_RAW;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.ROOT_SENTINEL;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.TRAILING_BACKSLASH_NAMES_RAW;
import static com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityFixtures.assertWarnNamesColumn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.data.ClickHouseColumn;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseFieldDescriptor;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free pins for {@link DeclaredNullability} (COLS-10). Rows are DESCRIBE JSON lines in the
 * form the server sends, read through {@code fromJsonRow}, turned into Columns with {@code
 * Column.extractColumn} and paired by {@link Table}, exactly as discovery does before the pass
 * runs. Deliberately extends nothing, so the class stays in {@code testUnit}.
 */
class DeclaredNullabilityTest {

    private static final String ROW_FORMAT = "{\"name\":\"%s\",\"type\":\"%s\",\"default_type\":\"\","
            + "\"default_expression\":\"\",\"comment\":\"\",\"codec_expression\":\"\","
            + "\"ttl_expression\":\"\",\"is_subcolumn\":%s}";
    private static final String ROOT = "c";
    private static final String DATABASE = "db";
    private static final String TABLE = "t";
    private static final String QUALIFIED_TABLE = "db.t";
    private static final String PATH_SEPARATOR = ".";
    private static final String STRUCTURE_MISMATCH_REASON = "(structure mismatch)";
    private static final String FALLBACK_KEEPING_CLAUSE = "keeping the nullability DESCRIBE reported";

    /** One DESCRIBE row; a name holding a dot is a sub-column. */
    private static String row(String name, String type) {
        return String.format(ROW_FORMAT, name, type, name.contains(PATH_SEPARATOR));
    }

    /** Discovery up to the point where the pass runs: Columns paired by an upstream Table. */
    private static Table describe(String... jsonRows) throws Exception {
        List<Column> columns = new ArrayList<>();
        int numColumns = 0;
        for (String jsonRow : jsonRows) {
            ClickHouseFieldDescriptor descriptor = ClickHouseFieldDescriptor.fromJsonRow(jsonRow);
            columns.add(Column.extractColumn(descriptor));
            if (!descriptor.isSubcolumn()) {
                numColumns++;
            }
        }
        return new Table(DATABASE, TABLE, false, columns, numColumns);
    }

    private static Column root(Table table) {
        return root(table, ROOT);
    }

    private static Column root(Table table, String name) {
        return table.getRootColumnsMap().get(name);
    }

    private static Map<String, Boolean> nullabilityByPath(Column column) {
        return ColumnNullabilityPaths.byPath(column.getName(), column);
    }

    /** A hand-built node that DESCRIBE would have reported nullable, so a pass that ran would flip it. */
    private static Column.ColumnBuilder nullableNode(String name) {
        return Column.builder().name(name).isNullable(true);
    }

    /** The flags of hand-built nodes, which {@link ColumnNullabilityPaths} cannot walk once a type is null. */
    private static List<Boolean> flags(Column... nodes) {
        return Arrays.stream(nodes).map(Column::isNullable).toList();
    }

    private static LogCapture.CapturedEvent onlyWarning(LogCapture log) {
        List<LogCapture.CapturedEvent> warnings = log.events("WARN");
        assertEquals(1, warnings.size());
        return warnings.get(0);
    }

    /** The rest of a structure-mismatch WARN, after the column slot was asserted. */
    private static void assertMismatchWarning(LogCapture.CapturedEvent warning, String type, LogCapture log) {
        String message = warning.message();
        assertTrue(message.contains(QUALIFIED_TABLE), message);
        assertTrue(message.contains(type), message);
        assertTrue(message.contains(STRUCTURE_MISMATCH_REASON), message);
        assertFalse(warning.hasThrowable(), "the exception message is never logged");
        assertEquals(1, log.events().size());
    }

    /** The single unpaired-element WARN: delimited column, bracketed element list, type and table, no throwable. */
    private static void assertOnlyUnpairedWarning(LogCapture log, String type, String unpairedList) {
        LogCapture.CapturedEvent warning = onlyWarning(log);
        String message = warning.message();
        assertWarnNamesColumn(message, ROOT_SENTINEL);
        assertTrue(message.contains(unpairedList), message);
        assertTrue(message.contains(type), message);
        assertTrue(message.contains(QUALIFIED_TABLE), message);
        assertFalse(message.contains(FALLBACK_KEEPING_CLAUSE), message);
        assertFalse(warning.hasThrowable(), "the WARN names metadata only and carries no throwable");
        assertEquals(1, log.events().size());
    }

    /** {@code apply} on a sentinel root built from {@code rows}, with the one WARN it must log. */
    private static Column applyOnSentinelRoot(String type, String unpairedList, String... jsonRows) throws Exception {
        Column column = root(describe(jsonRows), ROOT_SENTINEL);
        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            assertOnlyUnpairedWarning(log, type, unpairedList);
        }
        return column;
    }

    @Test
    @DisplayName("COLS-10: C1 Nullable(Tuple(a Int32, b String)) takes a nullable tuple and non-nullable elements")
    void plainNullableTuplePairsToDeclaredFlags() throws Exception {
        String type = "Nullable(Tuple(a Int32, b String))";
        Column column = root(describe(
                row("c", type), row("c.a", "Nullable(Int32)"), row("c.b", "Nullable(String)")));

        assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

        assertEquals(Map.of("c", true, "c.a", false, "c.b", false), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: C2 keeps a declared nullable element beside a non-nullable one in the same tuple")
    void nullableElementBesideNonNullableKeepsEachDeclaredFlag() throws Exception {
        String type = "Nullable(Tuple(a Nullable(Int32), b String))";
        Column column = root(describe(
                row("c", type), row("c.a", "Nullable(Int32)"), row("c.b", "Nullable(String)")));

        assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

        assertEquals(Map.of("c", true, "c.a", true, "c.b", false), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: C3 Array(Nullable(Tuple(...))) takes a nullable element tuple with declared element flags")
    void arrayOfNullableTuplePairsToDeclaredFlags() throws Exception {
        String type = "Array(Nullable(Tuple(a Int32, b Nullable(String))))";
        Column column = root(describe(
                row("c", type),
                row("c.a", "Array(Nullable(Int32))"),
                row("c.b", "Array(Nullable(String))")));

        assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

        assertEquals(
                Map.of("c", false, "c[]", true, "c[].a", false, "c[].b", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: Map(String, Nullable(Tuple(...))) takes a nullable value tuple with non-nullable elements")
    void mapOfNullableTuplePairsToDeclaredFlags() throws Exception {
        String type = "Map(String, Nullable(Tuple(a Int32, b String)))";
        Column column = root(describe(
                row("c", type),
                row("c.keys", "Array(String)"),
                row("c.values", "Array(Nullable(Tuple(a Int32, b String)))"),
                row("c.values.a", "Array(Nullable(Int32))"),
                row("c.values.b", "Array(Nullable(String))")));

        assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

        assertEquals(
                Map.of("c", false, "c{}", true, "c{}.a", false, "c{}.b", false), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: a Tuple declared non-nullable under a Nullable(Tuple) parent stays non-nullable although DESCRIBE wraps it")
    void nestedTupleDeclaredNonNullableIgnoresTheDescribeWrapper() throws Exception {
        String type = "Nullable(Tuple(a Int32, n Tuple(d String, e Nullable(Int32))))";
        Column column = root(describe(
                row("c", type),
                row("c.a", "Nullable(Int32)"),
                row("c.n", "Nullable(Tuple(d String, e Nullable(Int32)))"),
                row("c.n.d", "Nullable(String)"),
                row("c.n.e", "Nullable(Int32)")));
        assertTrue(nullabilityByPath(column).get("c.n"), "premise: DESCRIBE reports the nested tuple as Nullable");

        assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

        assertEquals(
                Map.of("c", true, "c.a", false, "c.n", false, "c.n.d", false, "c.n.e", true),
                nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: the multi-line non-nullable Tuple form (backslash plus n) applies after normalisation without a fallback WARN")
    void multiLineTupleFormAppliesWithoutFallback() throws Exception {
        String serverType = "Tuple(" + DeclaredNullability.ESCAPED_NEWLINE + "    a Int32," + DeclaredNullability.ESCAPED_NEWLINE + "    b Nullable(String))";
        String rawType = ClickHouseFieldDescriptor.fromJsonRow(row("c", serverType)).getType();
        assertTrue(rawType.contains(DeclaredNullability.ESCAPED_NEWLINE), "premise: discovery sees the escaped multi-line form");
        Column column = root(describe(row("c", serverType), row("c.a", "Int32"), row("c.b", "Nullable(String)")));

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertTrue(DeclaredNullability.apply(column, rawType, DATABASE, TABLE));

            assertEquals(List.of(), log.events("WARN"));
        }
        assertEquals(Map.of("c", false, "c.a", false, "c.b", true), nullabilityByPath(column));
    }

    /**
     * One captured shape end to end: discovery keeps the raw type verbatim, {@code normalise} gives
     * the expected text (a literal, never computed with a replace, which would repeat the defect),
     * and the pass pairs with no WARN. {@code subRows} are the DESCRIBE sub-column rows of the root.
     */
    private static void assertCapturedShapePairs(
            String rawType, String expectedNormalised, Map<String, Boolean> expectedFlags, String... subRows)
            throws Exception {
        assertEquals(
                rawType,
                ClickHouseFieldDescriptor.fromJsonRow(row(ROOT, rawType)).getType(),
                "premise: discovery keeps the captured text verbatim");
        assertEquals(expectedNormalised, DeclaredNullability.normalise(rawType));
        List<String> rows = new ArrayList<>();
        rows.add(row(ROOT, rawType));
        rows.addAll(Arrays.asList(subRows));
        Column column = root(describe(rows.toArray(String[]::new)));

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertTrue(DeclaredNullability.apply(column, rawType, DATABASE, TABLE));

            assertEquals(List.of(), log.events("WARN"));
        }
        assertEquals(expectedFlags, nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-13 (D-10): a literal backslash-n inside a backtick-quoted element name survives normalise and pairs without a fallback WARN")
    void backslashNInsideABacktickNameSurvivesNormaliseAndPairs() throws Exception {
        assertCapturedShapePairs(
                BACKSLASH_N_NAME_RAW,
                "Tuple(     `a\\\\nb` Int32,     d String)",
                Map.of("c", false, "c." + BACKSLASH_N_ELEMENT_NAME, false, "c.d", false),
                row("c." + BACKSLASH_N_ELEMENT, "Int32"),
                row("c.d", "String"));
    }

    @Test
    @DisplayName("COLS-13 (D-10): a real newline inside a backtick-quoted element name survives normalise and pairs without a fallback WARN")
    void realNewlineInsideABacktickNameSurvivesNormaliseAndPairs() throws Exception {
        assertCapturedShapePairs(
                REAL_NEWLINE_NAME_RAW,
                "Tuple(     `a\nb` Int32,     d String)",
                Map.of("c", false, "c." + REAL_NEWLINE_ELEMENT_NAME, false, "c.d", false),
                row("c." + REAL_NEWLINE_ELEMENT, "Int32"),
                row("c.d", "String"));
    }

    @Test
    @DisplayName("COLS-13 (D-10): a literal backslash-n inside an Enum8 label is kept by normalise and the tuple pairs without a fallback WARN")
    void backslashNInsideAnEnumLabelIsKeptAndPairs() throws Exception {
        assertCapturedShapePairs(
                BACKSLASH_N_ENUM_LABEL_RAW,
                "Tuple(     e Enum8('x\\\\ny' = 1, 'z' = 2),     d String)",
                Map.of("c", false, "c.e", false, "c.d", false),
                row("c.e", "Enum8('x\\\\\\\\ny' = 1, 'z' = 2)"),
                row("c.d", "String"));
    }

    @Test
    @DisplayName("COLS-13 (D-10): literal backslash-n names on two nesting levels survive normalise and pair without a fallback WARN")
    void backslashNInNamesOnTwoLevelsSurvivesNormaliseAndPairs() throws Exception {
        String outer = "c.p\\\\nq";
        String outerName = "c.p\\nq";
        assertCapturedShapePairs(
                BACKSLASH_N_NESTED_NAMES_RAW,
                "Tuple(     `p\\\\nq` Tuple(         `r\\\\ns` Int32,         t Nullable(String)),     d String)",
                Map.of(
                        "c", false,
                        outerName, false,
                        outerName + ".r\\ns", false,
                        outerName + ".t", true,
                        "c.d", false),
                row(outer, "Tuple(\\n    `r\\\\\\\\ns` Int32,\\n    t Nullable(String))"),
                row(outer + ".r\\\\ns", "Int32"),
                row(outer + ".t", "Nullable(String)"),
                row("c.d", "String"));
    }

    @Test
    @DisplayName("COLS-13 (D-10): names ending in a backslash and in backslash-n survive normalise and pair without a fallback WARN")
    void namesEndingInABackslashSurviveNormaliseAndPair() throws Exception {
        assertCapturedShapePairs(
                TRAILING_BACKSLASH_NAMES_RAW,
                "Tuple(     `e\\\\` Int32,     `f\\\\n` Int32)",
                Map.of("c", false, "c.e\\", false, "c.f\\n", false),
                row("c.e\\\\", "Int32"),
                row("c.f\\\\n", "Int32"));
    }

    @Test
    @DisplayName("COLS-13 (D-10): the Nullable(Tuple) literal of the live premise has no line break, loses one escape layer and pairs without a fallback WARN")
    void nullableTupleWithABackslashNNameIsUnchangedAndPairs() throws Exception {
        assertCapturedShapePairs(
                NULLABLE_BACKSLASH_N_NAME_RAW,
                "Nullable(Tuple(`a\\\\nb` Int32, d String))",
                Map.of("c", true, "c." + BACKSLASH_N_ELEMENT_NAME, false, "c.d", false),
                row("c." + BACKSLASH_N_ELEMENT, "Nullable(Int32)"),
                row("c.d", "Nullable(String)"));
    }

    @Test
    @DisplayName("COLS-13 (D-10) edges: an empty type, an unterminated backtick and a non-ASCII letter next to a backslash-n in a name")
    void normaliseIsTotalAndKeepsNonAsciiNames() {
        assertEquals("", DeclaredNullability.normalise(""));
        String unterminated = "Tuple(`a" + DeclaredNullability.ESCAPED_NEWLINE;
        assertEquals("Tuple(`a\n", DeclaredNullability.normalise(unterminated));
        // U+00E9 (é) is written as a Java escape so the string does not depend on the compiler's source encoding.
        assertEquals(
                "Tuple(     `\u00e9\\\\n` Int32)",
                DeclaredNullability.normalise("Tuple(\\n    `\u00e9\\\\\\\\n` Int32)"));
    }

    @Test
    @DisplayName("NAME-01: an escaped backtick inside a quoted name keeps the quote open, so the following line break is still replaced")
    void escapedBacktickInsideANameKeepsTheQuoteOpen() {
        // Runtime text: a backtick-quoted name of a, an escaped backtick and a literal backslash-n pair, between two server line breaks.
        String raw = "Tuple(\\n    `a\\\\`\\\\\\\\nb` Int32,\\n    d String)";
        assertEquals("Tuple(     `a\\`\\\\nb` Int32,     d String)", DeclaredNullability.normalise(raw));
    }

    @Test
    @DisplayName("NAME-01: two backslashes before n outside quotes are not a line break")
    void twoBackslashesBeforeNOutsideQuotesAreNotALineBreak() {
        String type = "Tuple(a Int32,\\\\n b String)";
        assertEquals("Tuple(a Int32,\\n b String)", DeclaredNullability.normalise(type));
    }

    @Test
    @DisplayName("NAME-04: an element named with an escaped backtick and a backslash-n pairs without a fallback WARN")
    void escapedBacktickElementPairsWithoutAFallbackWarning() throws Exception {
        assertCapturedShapePairs(
                "Tuple(\\n    `a\\\\`\\\\\\\\nb` Int32,\\n    d String)",
                "Tuple(     `a\\`\\\\nb` Int32,     d String)",
                Map.of("c", false, "c.a`\\nb", false, "c.d", false),
                row("c.a`\\\\nb", "Int32"),
                row("c.d", "String"));
    }

    @ParameterizedTest(name = "COLS-13 (D-10) unchanged: {0} normalises exactly as the plain replace did")
    @ValueSource(strings = {
        "Tuple(\\n    a Int32,\\n    b Nullable(String))",
        "Tuple(\\n    size UInt32,\\n    name String)",
        "Array(Tuple(\\n    size UInt8,\\n    v String))",
        "Nullable(Tuple(size0 Int32, other String))",
        "Tuple(\\n    `a b` Int32,\\n    `c,d` Nullable(String))",
        "Tuple(\\n    e Enum8('a' = 1, 'b' = 2),\\n    d String)"
    })
    void typesWithoutABackslashInsideQuotesNormaliseAsBefore(String type) {
        assertEquals(type.replace(DeclaredNullability.ESCAPED_NEWLINE, " "), DeclaredNullability.normalise(type));
    }

    @Test
    @DisplayName("COLS-10: containsTuple is false for roots without a Tuple and true for Array or Map roots holding one")
    void containsTupleFollowsTheColumnTree() throws Exception {
        assertFalse(DeclaredNullability.containsTuple(
                root(describe(row("c", "Array(Nullable(Int32))")))));
        assertFalse(DeclaredNullability.containsTuple(root(describe(row("c", "Nullable(String)")))));
        assertTrue(DeclaredNullability.containsTuple(root(describe(
                row("c", "Array(Nullable(Tuple(a Int32)))"), row("c.a", "Array(Nullable(Int32))")))));
        assertTrue(DeclaredNullability.containsTuple(root(describe(
                row("c", "Map(String, Nullable(Tuple(a Int32)))"),
                row("c.keys", "Array(String)"),
                row("c.values", "Array(Nullable(Tuple(a Int32)))"),
                row("c.values.a", "Array(Nullable(Int32))")))));
    }

    @Test
    @DisplayName("COLS-10: unnamed Tuple elements pair by 1-based position")
    void unnamedTupleElementsPairByPosition() throws Exception {
        String type = "Nullable(Tuple(Int32, Nullable(String)))";
        Column column = root(describe(row("c", type), row("c.1", "Nullable(Int32)"), row("c.2", "Nullable(String)")));

        assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

        assertEquals(Map.of("c", true, "c.1", false, "c.2", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: backtick element names holding a space or a comma pair by name")
    void backtickElementNamesPairByName() throws Exception {
        String type = "Nullable(Tuple(`a b` Int32, `c,d` Nullable(String)))";
        Column column = root(describe(row("c", type), row("c.a b", "Nullable(Int32)"), row("c.c,d", "Nullable(String)")));

        assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

        assertEquals(Map.of("c", true, "c.a b", false, "c.c,d", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: a Tuple Column that lacks one declared element still pairs the others by name")
    void missingDeclaredElementDoesNotShiftPairing() throws Exception {
        String type = "Nullable(Tuple(a Int32, b Nullable(String)))";
        Column column = root(describe(row("c", type), row("c.b", "Nullable(String)")));

        assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

        assertEquals(Map.of("c", true, "c.b", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-14 unpaired-element WARN (D-01, D-02): a Tuple Column that lacks a declared element logs one metadata-only WARN naming it and still applies the declared nullability")
    void missingDeclaredElementLogsOneUnpairedElementWarningAndStillApplies() throws Exception {
        String type = "Nullable(Tuple(a Int32, b Nullable(String)))";
        Column column = root(
                describe(row(ROOT_SENTINEL, type), row(ROOT_SENTINEL + ".b", "Nullable(String)")), ROOT_SENTINEL);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            LogCapture.CapturedEvent warning = onlyWarning(log);
            String message = warning.message();
            assertWarnNamesColumn(message, ROOT_SENTINEL);
            assertTrue(message.contains("[" + ROOT_SENTINEL + ".a]"), message);
            assertTrue(message.contains(type), message);
            assertTrue(message.contains(QUALIFIED_TABLE), message);
            assertFalse(message.contains(FALLBACK_KEEPING_CLAUSE), message);
            assertFalse(warning.hasThrowable(), "the WARN names metadata only and carries no throwable");
            assertEquals(1, log.events().size());
        }
        assertEquals(
                Map.of(ROOT_SENTINEL, true, ROOT_SENTINEL + ".b", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-14 unpaired-element WARN (D-01): unpaired elements are listed in declaration order")
    void unpairedElementsAreListedInDeclarationOrder() throws Exception {
        String type = "Tuple(a Int32, b String, c Nullable(Int32))";

        Column column = applyOnSentinelRoot(
                type, "[" + ROOT_SENTINEL + ".a, " + ROOT_SENTINEL + ".c]", row(ROOT_SENTINEL, type), row(ROOT_SENTINEL + ".b", "String"));

        assertEquals(Map.of(ROOT_SENTINEL, false, ROOT_SENTINEL + ".b", false), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-14 unpaired-element WARN (D-01): an element missing inside a nested Tuple is named by its full path")
    void nestedUnpairedElementIsNamedByItsFullPath() throws Exception {
        String type = "Tuple(a Int32, n Tuple(x Int32, y Nullable(String)))";

        Column column = applyOnSentinelRoot(
                type,
                "[" + ROOT_SENTINEL + ".n.x]",
                row(ROOT_SENTINEL, type),
                row(ROOT_SENTINEL + ".a", "Int32"),
                row(ROOT_SENTINEL + ".n", "Tuple(x Int32, y Nullable(String))"),
                row(ROOT_SENTINEL + ".n.y", "Nullable(String)"));

        assertEquals(
                Map.of(ROOT_SENTINEL, false, ROOT_SENTINEL + ".a", false, ROOT_SENTINEL + ".n", false, ROOT_SENTINEL + ".n.y", true),
                nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-14 unpaired-element WARN (D-01): an element missing inside Array(Nullable(Tuple)) is named and the paired ones apply")
    void unpairedElementInsideAnArrayOfNullableTupleIsNamed() throws Exception {
        String type = "Array(Nullable(Tuple(a Int32, b Nullable(String))))";

        Column column = applyOnSentinelRoot(
                type, "[" + ROOT_SENTINEL + ".a]", row(ROOT_SENTINEL, type), row(ROOT_SENTINEL + ".b", "Array(Nullable(String))"));

        assertEquals(
                Map.of(ROOT_SENTINEL, false, ROOT_SENTINEL + "[]", true, ROOT_SENTINEL + "[].b", true),
                nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-14 unpaired-element WARN (D-01): an element missing from a Map value Tuple is named by the value Tuple's path")
    void unpairedElementInsideAMapValueTupleIsNamed() throws Exception {
        String type = "Map(String, Tuple(a Int32, b Nullable(String)))";
        Column column = root(
                describe(
                        row(ROOT_SENTINEL, type),
                        row(ROOT_SENTINEL + ".keys", "Array(String)"),
                        row(ROOT_SENTINEL + ".values", "Array(Tuple(a Int32, b Nullable(String)))"),
                        row(ROOT_SENTINEL + ".values.b", "Array(Nullable(String))")),
                ROOT_SENTINEL);
        String valueTupleName = column.getMapValueType().getName();

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            assertOnlyUnpairedWarning(log, type, "[" + valueTupleName + ".a]");
        }
        assertEquals(
                Map.of(ROOT_SENTINEL, false, ROOT_SENTINEL + "{}", false, ROOT_SENTINEL + "{}.b", true),
                nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-14 unpaired-element WARN (D-01): an element missing under a root SimpleAggregateFunction over a Tuple is named")
    void unpairedElementUnderARootSimpleAggregateFunctionIsNamed() throws Exception {
        String type = "SimpleAggregateFunction(anyLast, Tuple(a Nullable(Int32), b Int32))";

        Column column = applyOnSentinelRoot(
                type, "[" + ROOT_SENTINEL + ".a]", row(ROOT_SENTINEL, type), row(ROOT_SENTINEL + ".b", "Int32"));

        assertEquals(Map.of(ROOT_SENTINEL, false, ROOT_SENTINEL + ".b", false), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-14 unpaired-element WARN (D-01): an unnamed (positional) Tuple element is named by its position")
    void unpairedUnnamedElementIsNamedByPosition() throws Exception {
        String type = "Tuple(Int32, Nullable(String))";

        Column column = applyOnSentinelRoot(
                type, "[" + ROOT_SENTINEL + ".1]", row(ROOT_SENTINEL, type), row(ROOT_SENTINEL + ".2", "Nullable(String)"));

        assertEquals(Map.of(ROOT_SENTINEL, false, ROOT_SENTINEL + ".2", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-14 unpaired-element WARN (D-01): a Tuple root row with no element rows lists every declared element in one WARN")
    void tupleRootRowWithoutElementRowsListsEveryDeclaredElement() throws Exception {
        String type = "Tuple(a Nullable(Int32), b String)";

        Column column = applyOnSentinelRoot(
                type, "[" + ROOT_SENTINEL + ".a, " + ROOT_SENTINEL + ".b]", row(ROOT_SENTINEL, type));

        assertEquals(Map.of(ROOT_SENTINEL, false), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-14 unpaired-element WARN (D-01): a Tuple Column whose tupleFields is null lists every declared element in one WARN")
    void tupleColumnWithNullTupleFieldsListsEveryDeclaredElement() {
        String type = "Tuple(a Int32)";
        Column column = nullableNode(ROOT_SENTINEL).type(Type.TUPLE).build();

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            assertOnlyUnpairedWarning(log, type, "[" + ROOT_SENTINEL + ".a]");
        }
        assertEquals(List.of(false), flags(column));
    }

    @Test
    @DisplayName("COLS-14 unpaired-element WARN (D-02): the WARN repeats on every apply, with no dedup")
    void unpairedElementWarnRepeatsOnEveryApply() throws Exception {
        String type = "Nullable(Tuple(a Int32, b Nullable(String)))";
        Column column = root(
                describe(row(ROOT_SENTINEL, type), row(ROOT_SENTINEL + ".b", "Nullable(String)")), ROOT_SENTINEL);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));
            assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            List<LogCapture.CapturedEvent> warnings = log.events("WARN");
            assertEquals(2, warnings.size());
            for (LogCapture.CapturedEvent warning : warnings) {
                assertWarnNamesColumn(warning.message(), ROOT_SENTINEL);
                assertTrue(warning.message().contains("[" + ROOT_SENTINEL + ".a]"), warning.message());
            }
            assertEquals(2, log.events().size());
        }
        assertEquals(
                Map.of(ROOT_SENTINEL, true, ROOT_SENTINEL + ".b", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: a declared type that does not parse fails open with one metadata-only WARN")
    void unparsableDeclaredTypeFailsOpenWithOneMetadataWarning() throws Exception {
        String sentinel = "SENTINEL_ELEMENT_NAME";
        String type = "Nullable(Tuple(" + sentinel + " NoSuchType))";
        Column column = root(
                describe(row(ROOT_SENTINEL, "Nullable(Tuple(a Int32))"), row(ROOT_SENTINEL + ".a", "Nullable(Int32)")),
                ROOT_SENTINEL);
        Map<String, Boolean> before = nullabilityByPath(column);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            List<LogCapture.CapturedEvent> warnings = log.events("WARN");
            assertEquals(1, warnings.size());
            String message = warnings.get(0).message();
            assertWarnNamesColumn(message, ROOT_SENTINEL);
            assertTrue(message.contains(type), message);
            assertTrue(message.contains(QUALIFIED_TABLE), message);
            assertFalse(warnings.get(0).hasThrowable(), "the exception text can echo the type and is never logged");
            assertEquals(1, log.events().size());
        }
        assertEquals(before, nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: a Tuple Column whose declared type is not a Tuple fails open and changes nothing")
    void structureMismatchFailsOpenWithOneWarning() throws Exception {
        String type = "Array(Int32)";
        Column column = root(
                describe(row(ROOT_SENTINEL, "Nullable(Tuple(a Int32))"), row(ROOT_SENTINEL + ".a", "Nullable(Int32)")),
                ROOT_SENTINEL);
        Map<String, Boolean> before = nullabilityByPath(column);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            List<LogCapture.CapturedEvent> warnings = log.events("WARN");
            assertEquals(1, warnings.size());
            assertWarnNamesColumn(warnings.get(0).message(), ROOT_SENTINEL);
            assertTrue(warnings.get(0).message().contains(type), warnings.get(0).message());
            assertTrue(warnings.get(0).message().contains(QUALIFIED_TABLE), warnings.get(0).message());
            assertFalse(warnings.get(0).hasThrowable());
        }
        assertEquals(before, nullabilityByPath(column));
    }

    @Test
    @DisplayName("DEBT-10: the sentinel root occurs in no fixed WARN text, is plain ASCII and differs from the database and table")
    void sentinelRootOccursInNoFixedWarnText() {
        assertFalse(DeclaredNullability.FALLBACK_WARNING.contains(ROOT_SENTINEL), DeclaredNullability.FALLBACK_WARNING);
        assertFalse(
                DeclaredNullability.UNPAIRED_ELEMENT_WARNING.contains(ROOT_SENTINEL),
                DeclaredNullability.UNPAIRED_ELEMENT_WARNING);
        assertFalse(
                DeclaredNullability.UNPAIRED_ELEMENT_WARNING.contains(FALLBACK_KEEPING_CLAUSE),
                DeclaredNullability.UNPAIRED_ELEMENT_WARNING);
        assertTrue(ROOT_SENTINEL.matches("[a-z_]+"), ROOT_SENTINEL);
        assertNotEquals(DATABASE, ROOT_SENTINEL);
        assertNotEquals(TABLE, ROOT_SENTINEL);
    }

    @Test
    @DisplayName("COLS-10: one unmatched element leaves the whole root untouched, the elements that did pair included")
    void unmatchedElementAppliesNothing() throws Exception {
        String declared = "Tuple(a Nullable(Int32), z String)";
        Column column = root(describe(
                row("c", "Nullable(Tuple(a Int32, b String))"),
                row("c.a", "Nullable(Int32)"),
                row("c.b", "Nullable(String)")));
        Map<String, Boolean> before = nullabilityByPath(column);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, declared, DATABASE, TABLE));

            List<LogCapture.CapturedEvent> warnings = log.events("WARN");
            assertEquals(1, warnings.size());
            assertTrue(warnings.get(0).message().contains(QUALIFIED_TABLE), warnings.get(0).message());
        }
        assertEquals(before, nullabilityByPath(column));
    }

    // COLS-12 flip (D-08): this case asserted that the WARN named the NullPointerException provoked
    // through a Column with a null type. collect gaining a null check is 19-REVIEW IN-02's re-open
    // trigger. The input is kept and the contract (fail open with one metadata-only WARN) is
    // unchanged, so the case now asserts the structure mismatch reason.
    // AGENTS.md exemption (never delete or weaken existing tests): granted by
    // user decision D-08 (ROADMAP SC1). Not a weakening: the method, its null-type input and the
    // fail-open assertions (false, one WARN naming the table, no throwable, one event) are kept, and
    // the NullPointerException class name is replaced by the equally specific `(structure mismatch)`
    // reason, with the sentinel column assertion added.
    @Test
    @DisplayName("COLS-12 (D-08 flip): a Column with a null type falls back as a structure mismatch with one metadata-only WARN naming the table")
    void unexpectedExceptionInThePassFailsOpenWithOneMetadataWarning() throws Exception {
        String type = "Tuple(a Int32)";
        Column column = Column.builder().name(ROOT_SENTINEL).build();

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            List<LogCapture.CapturedEvent> warnings = log.events("WARN");
            assertEquals(1, warnings.size());
            String message = warnings.get(0).message();
            assertWarnNamesColumn(message, ROOT_SENTINEL);
            assertTrue(message.contains(QUALIFIED_TABLE), message);
            assertTrue(message.contains(type), message);
            assertTrue(message.contains("(structure mismatch)"), message);
            assertFalse(warnings.get(0).hasThrowable(), "the exception message is never logged");
            assertEquals(1, log.events().size());
        }
    }

    @Test
    @DisplayName("COLS-12 (D-06): a null root returns false before the pass and logs nothing")
    void nullRootReturnsFalseAndLogsNothing() {
        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(null, "Tuple(a Int32)", DATABASE, TABLE));

            assertEquals(List.of(), log.events());
        }
    }

    @Test
    @DisplayName("COLS-12 (D-05): containsTuple is false for a Column with a null type at the root, as an Array element or as a Map value")
    void containsTupleIsFalseForANullTypeAtAnyDepth() {
        Column noType = Column.builder().name(ROOT_SENTINEL).build();
        Column arrayOfNoType = Column.builder()
                .name(ROOT_SENTINEL)
                .type(Type.ARRAY)
                .arrayType(Column.builder().name(ROOT_SENTINEL + "[]").build())
                .build();
        Column mapOfNoType = Column.builder()
                .name(ROOT_SENTINEL)
                .type(Type.MAP)
                .mapKeyType(Type.STRING)
                .mapValueType(Column.builder().name(ROOT_SENTINEL + "{}").build())
                .build();

        assertFalse(DeclaredNullability.containsTuple(noType));
        assertFalse(DeclaredNullability.containsTuple(arrayOfNoType));
        assertFalse(DeclaredNullability.containsTuple(mapOfNoType));
    }

    @Test
    @DisplayName("COLS-12 (D-07) structure mismatch: an Array element with a null type")
    void arrayElementWithANullTypeIsAStructureMismatch() {
        String type = "Array(Tuple(a Int32))";
        Column element = nullableNode(ROOT_SENTINEL + "[]").build();
        Column column = nullableNode(ROOT_SENTINEL).type(Type.ARRAY).arrayType(element).build();
        List<Boolean> before = flags(column, element);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            LogCapture.CapturedEvent warning = onlyWarning(log);
            assertWarnNamesColumn(warning.message(), ROOT_SENTINEL);
            assertMismatchWarning(warning, type, log);
        }
        assertEquals(before, flags(column, element));
    }

    @Test
    @DisplayName("COLS-12 (D-07) structure mismatch: a Map value with a null type")
    void mapValueWithANullTypeIsAStructureMismatch() {
        String type = "Map(String, Tuple(a Int32))";
        Column value = nullableNode(ROOT_SENTINEL + "{}").build();
        Column column = nullableNode(ROOT_SENTINEL)
                .type(Type.MAP)
                .mapKeyType(Type.STRING)
                .mapValueType(value)
                .build();
        List<Boolean> before = flags(column, value);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            LogCapture.CapturedEvent warning = onlyWarning(log);
            assertWarnNamesColumn(warning.message(), ROOT_SENTINEL);
            assertMismatchWarning(warning, type, log);
        }
        assertEquals(before, flags(column, value));
    }

    @Test
    @DisplayName("COLS-12 (D-07) structure mismatch: a null tupleFields entry")
    void nullTupleFieldsEntryIsAStructureMismatch() {
        String type = "Tuple(a Int32)";
        Column column = nullableNode(ROOT_SENTINEL)
                .type(Type.TUPLE)
                .tupleFields(Arrays.asList((Column) null))
                .build();
        List<Boolean> before = flags(column);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            LogCapture.CapturedEvent warning = onlyWarning(log);
            assertWarnNamesColumn(warning.message(), ROOT_SENTINEL);
            assertMismatchWarning(warning, type, log);
        }
        assertEquals(before, flags(column));
    }

    @Test
    @DisplayName("COLS-12 (D-07) structure mismatch: a field with a null name")
    void fieldWithANullNameIsAStructureMismatch() {
        String type = "Tuple(a Int32)";
        Column field = Column.builder().type(Type.INT32).isNullable(true).build();
        Column column = nullableNode(ROOT_SENTINEL)
                .type(Type.TUPLE)
                .tupleFields(List.of(field))
                .build();
        List<Boolean> before = flags(column, field);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            LogCapture.CapturedEvent warning = onlyWarning(log);
            assertWarnNamesColumn(warning.message(), ROOT_SENTINEL);
            assertMismatchWarning(warning, type, log);
        }
        assertEquals(before, flags(column, field));
    }

    @Test
    @DisplayName("COLS-12 (D-07) structure mismatch: a field with a null type")
    void fieldWithANullTypeIsAStructureMismatch() {
        String type = "Tuple(a Int32)";
        Column field = nullableNode(ROOT_SENTINEL + ".a").build();
        Column column = nullableNode(ROOT_SENTINEL)
                .type(Type.TUPLE)
                .tupleFields(List.of(field))
                .build();
        List<Boolean> before = flags(column, field);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            LogCapture.CapturedEvent warning = onlyWarning(log);
            assertWarnNamesColumn(warning.message(), ROOT_SENTINEL);
            assertMismatchWarning(warning, type, log);
        }
        assertEquals(before, flags(column, field));
    }

    @Test
    @DisplayName("COLS-12 (D-07) structure mismatch: a root with a null name")
    void rootWithANullNameIsAStructureMismatch() {
        String type = "Tuple(a Int32)";
        Column column = Column.builder()
                .type(Type.TUPLE)
                .isNullable(true)
                .tupleFields(List.of())
                .build();
        List<Boolean> before = flags(column);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            LogCapture.CapturedEvent warning = onlyWarning(log);
            // the null name is the input under test, so the column slot reads the fixed placeholder
            assertWarnNamesColumn(warning.message(), "<unnamed>");
            assertMismatchWarning(warning, type, log);
        }
        assertEquals(before, flags(column));
    }

    @Test
    @DisplayName("COLS-12 (D-07) structure mismatch: an element Tuple with a null name never pairs a field literally named null.a")
    void elementTupleWithANullNameIsAStructureMismatch() {
        String type = "Array(Tuple(a Int32))";
        Column field = nullableNode("null.a").type(Type.INT32).build();
        Column elementTuple = Column.builder()
                .type(Type.TUPLE)
                .isNullable(true)
                .tupleFields(List.of(field))
                .build();
        Column column = nullableNode(ROOT_SENTINEL).type(Type.ARRAY).arrayType(elementTuple).build();
        List<Boolean> before = flags(column, elementTuple, field);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            LogCapture.CapturedEvent warning = onlyWarning(log);
            assertWarnNamesColumn(warning.message(), ROOT_SENTINEL);
            assertMismatchWarning(warning, type, log);
        }
        assertEquals(before, flags(column, elementTuple, field));
    }

    @Test
    @DisplayName("COLS-12 (D-07) structure mismatch: a second call on the same null-type root warns again and leaves it untouched")
    void secondCallOnTheSameNullTypeRootWarnsAgain() {
        String type = "Tuple(a Int32)";
        Column field = nullableNode(ROOT_SENTINEL + ".a").build();
        Column column = nullableNode(ROOT_SENTINEL)
                .type(Type.TUPLE)
                .tupleFields(List.of(field))
                .build();
        List<Boolean> before = flags(column, field);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, type, DATABASE, TABLE));
            assertFalse(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            List<LogCapture.CapturedEvent> warnings = log.events("WARN");
            assertEquals(2, warnings.size());
            for (LogCapture.CapturedEvent warning : warnings) {
                assertWarnNamesColumn(warning.message(), ROOT_SENTINEL);
                assertTrue(warning.message().contains(STRUCTURE_MISMATCH_REASON), warning.message());
                assertFalse(warning.hasThrowable());
            }
            assertEquals(warnings.get(0).message(), warnings.get(1).message());
            assertEquals(2, log.events().size());
        }
        assertEquals(before, flags(column, field));
    }

    @Test
    @DisplayName("COLS-12 (D-07) structure mismatch (kept): an Array root with no element Column")
    void arrayRootWithoutAnElementColumnIsAStructureMismatch() {
        String type = "Array(Int32)";
        Column column = nullableNode(ROOT_SENTINEL).type(Type.ARRAY).build();

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            LogCapture.CapturedEvent warning = onlyWarning(log);
            assertWarnNamesColumn(warning.message(), ROOT_SENTINEL);
            assertMismatchWarning(warning, type, log);
        }
        assertEquals(List.of(true), flags(column));
    }

    @Test
    @DisplayName("COLS-12 (D-07) kept, pairs: a Map root with no value Column pairs and logs nothing")
    void mapRootWithoutAValueColumnPairsAndLogsNothing() {
        Column column = nullableNode(ROOT_SENTINEL).type(Type.MAP).mapKeyType(Type.STRING).build();

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertTrue(DeclaredNullability.apply(column, "Map(String, Int32)", DATABASE, TABLE));

            assertEquals(List.of(), log.events());
        }
    }

    @Test
    @DisplayName("COLS-12 (D-09): a declared type ClickHouseColumn.of rejects falls back with one WARN ending in the thrown class name")
    void rejectedDeclaredTypeFallsBackWithTheThrownClassName() throws Exception {
        String type = "Nullable(Tuple(SENTINEL_ELEMENT_NAME NoSuchType))";
        RuntimeException rejection =
                assertThrows(RuntimeException.class, () -> ClickHouseColumn.of(ROOT_SENTINEL, type));
        String reason = "(" + rejection.getClass().getSimpleName() + ")";
        Column column = root(
                describe(row(ROOT_SENTINEL, "Nullable(Tuple(a Int32))"), row(ROOT_SENTINEL + ".a", "Nullable(Int32)")),
                ROOT_SENTINEL);
        Map<String, Boolean> before = nullabilityByPath(column);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            LogCapture.CapturedEvent warning = onlyWarning(log);
            assertWarnNamesColumn(warning.message(), ROOT_SENTINEL);
            assertTrue(warning.message().contains(QUALIFIED_TABLE), warning.message());
            assertTrue(warning.message().contains(type), warning.message());
            assertTrue(warning.message().contains(reason), warning.message());
            assertFalse(warning.hasThrowable(), "the exception text can echo the type and is never logged");
            assertEquals(1, log.events().size());
        }
        assertEquals(before, nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: containsTuple is false for a Map or Array root of Enum values and for a null Column")
    void containsTupleIsFalseWithoutATupleAnywhere() throws Exception {
        assertFalse(DeclaredNullability.containsTuple(root(describe(
                row("c", "Map(String, Nullable(Enum8('a' = 1)))"),
                row("c.keys", "Array(String)"),
                row("c.values", "Array(Nullable(Enum8('a' = 1)))")))));
        assertFalse(DeclaredNullability.containsTuple(null));
    }

    @Test
    @DisplayName("COLS-10 CR-01: a Tuple element declared SimpleAggregateFunction(f, Nullable(T)) keeps its nullable flag")
    void simpleAggregateFunctionNullableElementKeepsItsNullability() throws Exception {
        String type = "Tuple(a SimpleAggregateFunction(sum, Nullable(Int64)), b Nullable(Int32))";
        Column column = root(describe(
                row("c", type),
                row("c.a", "SimpleAggregateFunction(sum, Nullable(Int64))"),
                row("c.b", "Nullable(Int32)")));
        assertTrue(
                nullabilityByPath(column).get("c.a"),
                "premise: Column.extractColumn reads through the wrapper to the nullable argument type");

        assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

        assertEquals(Map.of("c", false, "c.a", true, "c.b", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10 CR-01: a root SimpleAggregateFunction over a Tuple pairs through the pass and flips no flag")
    void rootSimpleAggregateFunctionOverTuplePairsWithoutFlippingFlags() throws Exception {
        String type = "SimpleAggregateFunction(anyLast, Tuple(a Nullable(Int32), b Int32))";
        Column column = root(describe(
                row("c", type), row("c.a", "Nullable(Int32)"), row("c.b", "Int32")));
        Map<String, Boolean> before = nullabilityByPath(column);
        assertEquals(Type.TUPLE, column.getType(), "premise: the wrapper root is a TUPLE Column");
        assertTrue(DeclaredNullability.containsTuple(column), "premise: discovery routes the root into the pass");
        assertEquals(Map.of("c", false, "c.a", true, "c.b", false), before);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            assertEquals(List.of(), log.events("WARN"));
        }
        assertEquals(before, nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: no declared type returns false, changes nothing and logs nothing; normalise leaves other text alone")
    void nullDeclaredTypeIsANoOpAndNormaliseKeepsPlainText() throws Exception {
        Column column = root(describe(
                row("c", "Nullable(Tuple(a Int32))"), row("c.a", "Nullable(Int32)")));
        Map<String, Boolean> before = nullabilityByPath(column);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, null, DATABASE, TABLE));

            assertEquals(List.of(), log.events());
        }
        assertEquals(before, nullabilityByPath(column));
        String plain = "Nullable(Tuple(a Int32, b String))";
        assertEquals(plain, DeclaredNullability.normalise(plain));
        assertEquals("Tuple( a Int32, b String)", DeclaredNullability.normalise("Tuple(" + DeclaredNullability.ESCAPED_NEWLINE + "a Int32, b String)"));
    }

    @Test
    @DisplayName("COLS-10: the parser reads LowCardinality(Nullable(T)) as nullable and LowCardinality(T) as non-nullable, also as the SimpleAggregateFunction argument")
    void parserReadsLowCardinalityNullableAsNullable() {
        ClickHouseColumn nullable = ClickHouseColumn.of("p", "LowCardinality(Nullable(String))");
        assertTrue(nullable.isNullable());
        assertTrue(nullable.isLowCardinality());
        assertFalse(ClickHouseColumn.of("p", "LowCardinality(String)").isNullable());
        ClickHouseColumn wrapped =
                ClickHouseColumn.of("p", "SimpleAggregateFunction(anyLast, LowCardinality(Nullable(String)))");
        assertTrue(wrapped.getNestedColumns().get(0).isNullable());
    }

    @Test
    @DisplayName("COLS-10: a LowCardinality(Nullable(String)) Tuple element stays nullable beside a LowCardinality(String) element DESCRIBE wrapped in Nullable")
    void lowCardinalityNullableTupleElementKeepsDeclaredFlags() throws Exception {
        String type = "Nullable(Tuple(a LowCardinality(Nullable(String)), b LowCardinality(String)))";
        Column column = root(describe(
                row("c", type),
                row("c.a", "LowCardinality(Nullable(String))"),
                row("c.b", "LowCardinality(Nullable(String))")));
        assertTrue(nullabilityByPath(column).get("c.b"), "premise: DESCRIBE reports the plain element as nullable");

        assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

        assertEquals(Map.of("c", true, "c.a", true, "c.b", false), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: an Array element LowCardinality(Nullable(String)) is nullable and LowCardinality(String) is not")
    void lowCardinalityNullableArrayElementIsNullable() throws Exception {
        String nullableType = "Array(LowCardinality(Nullable(String)))";
        Column nullableElement = root(describe(row("c", nullableType)));
        String plainType = "Array(LowCardinality(String))";
        Column plainElement = root(describe(row("c", plainType)));

        assertTrue(DeclaredNullability.apply(nullableElement, nullableType, DATABASE, TABLE));
        assertTrue(DeclaredNullability.apply(plainElement, plainType, DATABASE, TABLE));

        assertEquals(Map.of("c", false, "c[]", true), nullabilityByPath(nullableElement));
        assertEquals(Map.of("c", false, "c[]", false), nullabilityByPath(plainElement));
    }

    @Test
    @DisplayName("COLS-10: a Map value LowCardinality(Nullable(String)) is nullable")
    void lowCardinalityNullableMapValueIsNullable() throws Exception {
        String type = "Map(String, LowCardinality(Nullable(String)))";
        Column column = root(describe(
                row("c", type),
                row("c.keys", "Array(String)"),
                row("c.values", "Array(LowCardinality(Nullable(String)))")));

        assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

        assertEquals(Map.of("c", false, "c{}", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: a Tuple element SimpleAggregateFunction(sum, Int64) stays non-nullable")
    void simpleAggregateFunctionNonNullableElementStaysNonNullable() throws Exception {
        String type = "Tuple(a SimpleAggregateFunction(sum, Int64), b Nullable(Int32))";
        Column column = root(describe(
                row("c", type),
                row("c.a", "SimpleAggregateFunction(sum, Int64)"),
                row("c.b", "Nullable(Int32)")));

        assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

        assertEquals(Map.of("c", false, "c.a", false, "c.b", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: a Tuple element SimpleAggregateFunction(anyLast, LowCardinality(Nullable(String))) is nullable")
    void simpleAggregateFunctionOverLowCardinalityNullableIsNullable() throws Exception {
        String type = "Tuple(a SimpleAggregateFunction(anyLast, LowCardinality(Nullable(String))), b Int32)";
        Column column = root(describe(
                row("c", type),
                row("c.a", "SimpleAggregateFunction(anyLast, LowCardinality(Nullable(String)))"),
                row("c.b", "Int32")));

        assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

        assertEquals(Map.of("c", false, "c.a", true, "c.b", false), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: a Tuple element SimpleAggregateFunction(anyLast, Array(Nullable(Int32))) pairs without a fallback WARN")
    void simpleAggregateFunctionOverArrayElementPairsWithoutFallback() throws Exception {
        String type = "Tuple(a SimpleAggregateFunction(anyLast, Array(Nullable(Int32))), b Int32)";
        Column column = root(describe(
                row("c", type),
                row("c.a", "SimpleAggregateFunction(anyLast, Array(Nullable(Int32)))"),
                row("c.b", "Int32")));

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));

            assertEquals(List.of(), log.events("WARN"));
        }
        assertEquals(
                Map.of("c", false, "c.a", false, "c.a[]", true, "c.b", false), nullabilityByPath(column));
    }

    @ParameterizedTest(name = "DESC-08 elementKey: {0}")
    @CsvSource(
            delimiter = '|',
            value = {
                "a named element|Tuple(size UInt8, v String)|0|size",
                "the first unnamed element|Tuple(UInt8, String)|0|1",
                "the second unnamed element|Tuple(UInt8, String)|1|2",
                "a backtick-named element|Tuple(`my size` UInt8, v String)|0|my size"
            })
    @DisplayName("DESC-08 elementKey: a Tuple element's key is its name, or its one-based position when unnamed")
    void elementKeyIsTheNameOrTheOneBasedPosition(String label, String type, int index, String expectedKey) {
        ClickHouseColumn parsed = ClickHouseColumn.of(ROOT, type);

        assertEquals(expectedKey, DeclaredNullability.elementKey(parsed.getNestedColumns().get(index), index), label);
    }
}
