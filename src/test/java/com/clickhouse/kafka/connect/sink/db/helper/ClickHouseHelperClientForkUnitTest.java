package com.clickhouse.kafka.connect.sink.db.helper;

import static com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers.newDescriptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullability;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free unit pins for the fork's table-discovery helpers extracted in
 * {@link ClickHouseHelperClient}: {@code buildTableFilterClause} (FILT-01..04) and
 * {@code shouldSkipDescribeRow} (DESC-01..03). Deliberately extends nothing — extending
 * {@code ClickHouseBase} would inherit {@code @Tag("clickhouse")} and exclude this class
 * from {@code testUnit} (D-16).
 */
class ClickHouseHelperClientForkUnitTest {

    private static final String DATABASE = "db";
    private static final String TABLE = "t";
    private static final String DISCOVERED_TABLE = DATABASE + "." + TABLE;
    private static final String WARN_LEVEL = "WARN";

    @Test
    @DisplayName("FILT-01: empty filter produces the empty clause")
    void emptyFilterProducesEmptyClause() {
        assertEquals("", ClickHouseHelperClient.buildTableFilterClause(""));
    }

    @Test
    @DisplayName("FILT-01: a single-space filter is not treated as empty (exact equality, not a trim)")
    void singleSpaceFilterProducesNonEmptyClause() {
        assertFalse(ClickHouseHelperClient.buildTableFilterClause(" ").isEmpty());
    }

    @Test
    @DisplayName("FILT-03: a single quote in the filter is escaped by doubling")
    void singleQuoteFilterIsEscapedExactly() {
        assertEquals(
                " AND match(table, 'o''brien')",
                ClickHouseHelperClient.buildTableFilterClause("o'brien"));
    }

    @Test
    @DisplayName("FILT-03: multiple single quotes in the filter are each escaped by doubling")
    void multipleSingleQuotesAreEachEscaped() {
        assertEquals(
                " AND match(table, 'a''b''c')",
                ClickHouseHelperClient.buildTableFilterClause("a'b'c"));
    }

    @Test
    @DisplayName("FILT-04: a backslash character class passes through unchanged (D-19, no doubling)")
    void backslashClassPassesThroughUnchanged() {
        assertEquals(
                " AND match(table, 'tbl_\\d+')",
                ClickHouseHelperClient.buildTableFilterClause("tbl_\\d+"));
    }

    @Test
    @DisplayName(
            "CR-01: a backslash immediately followed by a quote no longer lets the quote-doubling breakout"
                    + " (a backslash right before a quote is padded to an even run before the quote is doubled)")
    void backslashImmediatelyBeforeQuoteIsNeutralizedBeforeDoubling() {
        // Naive quote-only doubling turns this into: ' AND match(table, 'x\'' OR 1=1-- ')
        // ClickHouse then parses \'' as (escaped quote) + (unescaped quote), closing the string
        // literal early and exposing " OR 1=1-- " as raw SQL (CR-01). The fix must pad the
        // single backslash to an even-length run BEFORE doubling the quote, so ClickHouse's own
        // left-to-right backslash decoding always treats the pair as one literal backslash and
        // leaves the doubled quote to close as one literal quote character - never the literal.
        assertEquals(
                " AND match(table, 'x\\\\'' OR 1=1-- ')",
                ClickHouseHelperClient.buildTableFilterClause("x\\' OR 1=1-- "));
    }

    @Test
    @DisplayName("CR-01: a trailing backslash (with no following quote in the filter itself) is"
            + " neutralized against the closing quote this method appends")
    void trailingBackslashIsNeutralizedAgainstTheClosingQuote() {
        // A single trailing backslash would otherwise escape the closing quote this method
        // appends, leaving the ClickHouse string literal unterminated.
        assertEquals(
                " AND match(table, 'foo\\\\')",
                ClickHouseHelperClient.buildTableFilterClause("foo\\"));
    }

    @Test
    @DisplayName("CR-01: an already-even run of backslashes before a quote is left untouched"
            + " (self-cancelling, not re-doubled)")
    void evenBackslashRunBeforeQuoteIsUntouched() {
        assertEquals(
                " AND match(table, 'x\\\\''y')",
                ClickHouseHelperClient.buildTableFilterClause("x\\\\'y"));
    }

    @Test
    @DisplayName("DESC-01: .null subcolumn of a registered Nullable parent is skipped")
    void nullSubcolumnOverNullableParentIsSkipped() {
        Map<String, Column> columns = new HashMap<>();
        Column nullableCol = Column.extractColumn(newDescriptor("nullable_col", "Nullable(String)"));
        columns.put(nullableCol.getName(), nullableCol);

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(
                newDescriptor("nullable_col.null", "UInt8"), columns);

        assertTrue(skipped);
    }

    @ParameterizedTest(name = "DESC-02: {0} subcolumn of an Array parent is skipped")
    @ValueSource(strings = {".size0", ".size"})
    @DisplayName("DESC-02: array-length subcolumn suffixes (.size0 and the D-20 bare .size) are skipped")
    void arrayLengthSuffixesAreSkipped(String suffix) {
        Map<String, Column> columns = new HashMap<>();
        Column arrayCol = Column.extractColumn(newDescriptor("array_col", "Array(String)"));
        columns.put(arrayCol.getName(), arrayCol);

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(
                newDescriptor("array_col" + suffix, "UInt64"), columns);

        assertTrue(skipped);
    }

    @Test
    @DisplayName("DESC-02: a root column literally named size0 (isSubcolumn=false) is never skipped")
    void rootColumnNamedSize0IsNotSkipped() {
        Map<String, Column> columns = new HashMap<>();

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(newDescriptor("size0", "UInt64"), columns);

        assertFalse(skipped);
    }

    @Test
    @DisplayName("DESC-02: a subcolumn name beginning with a dot does not throw and is not skipped")
    void leadingDotSubcolumnNameDoesNotThrow() {
        Map<String, Column> columns = new HashMap<>();
        ClickHouseFieldDescriptor leadingDot = ClickHouseFieldDescriptor.builder()
                .name(".foo")
                .type("UInt64")
                .isSubcolumn(true)
                .build();

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(leadingDot, columns);

        assertFalse(skipped);
    }

    @Test
    @DisplayName("DESC-02: a subcolumn whose parent has not yet been registered returns false, not a throw")
    void subcolumnOverUnregisteredParentReturnsFalse() {
        Map<String, Column> columns = new HashMap<>(); // parent "json_col" intentionally never added

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(newDescriptor("json_col.a", "Dynamic"), columns);

        assertFalse(skipped);
    }

    @Test
    @DisplayName("DESC-03: subcolumn of a registered JSON parent is skipped")
    void jsonParentSubcolumnIsSkipped() {
        Map<String, Column> columns = new HashMap<>();
        Column jsonCol = Column.extractColumn(newDescriptor("json_col", "JSON"));
        columns.put(jsonCol.getName(), jsonCol);

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(
                newDescriptor("json_col.a", "Dynamic"), columns);

        assertTrue(skipped);
    }

    @Test
    @DisplayName("DESC-03: subcolumn of a registered Dynamic parent is skipped")
    void dynamicParentSubcolumnIsSkipped() {
        Map<String, Column> columns = new HashMap<>();
        Column dynamicCol = Column.extractColumn(newDescriptor("dynamic_col", "Dynamic"));
        columns.put(dynamicCol.getName(), dynamicCol);

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(
                newDescriptor("dynamic_col.a", "String"), columns);

        assertTrue(skipped);
    }

    @Test
    @DisplayName("DESC-03: a JSON parent's shared-data subcolumn with no trailing dot is still skipped (fixes the trailing-dot defect)")
    void jsonSharedDataSubcolumnWithoutTrailingDotIsSkipped() {
        Map<String, Column> columns = new HashMap<>();
        Column jsonCol = Column.extractColumn(newDescriptor("json_col", "JSON"));
        columns.put(jsonCol.getName(), jsonCol);

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(
                newDescriptor("json_col.object_shared_data", "String"), columns);

        assertTrue(skipped);
    }

    @Test
    @DisplayName("DESC-03: a root column whose name merely contains the shared-data text is never skipped (fixes the operator-precedence defect)")
    void rootColumnContainingSharedDataTextIsNotSkipped() {
        Map<String, Column> columns = new HashMap<>();
        ClickHouseFieldDescriptor rootWithSharedDataText = ClickHouseFieldDescriptor.builder()
                .name("json_col.object_shared_data")
                .type("String")
                .isSubcolumn(false)
                .build();

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(rootWithSharedDataText, columns);

        assertFalse(skipped);
    }

    @Test
    @DisplayName("DESC-03: Map key/value and Tuple field subcolumns are never skipped, so Table.updateParent keeps consuming them")
    void mapAndTupleSubcolumnsAreNotSkipped() {
        Map<String, Column> columns = new HashMap<>();
        Column mapCol = Column.extractColumn(newDescriptor("map_col", "Map(String, String)"));
        Column tupleCol = Column.extractColumn(newDescriptor("tuple_col", "Tuple(a String, b String)"));
        columns.put(mapCol.getName(), mapCol);
        columns.put(tupleCol.getName(), tupleCol);

        assertFalse(ClickHouseHelperClient.shouldSkipDescribeRow(newDescriptor("map_col.keys", "String"), columns));
        assertFalse(ClickHouseHelperClient.shouldSkipDescribeRow(newDescriptor("map_col.values", "String"), columns));
        assertFalse(ClickHouseHelperClient.shouldSkipDescribeRow(newDescriptor("tuple_col.a", "String"), columns));
    }

    private static Map<String, Column> columnsSoFar(String... nameAndTypePairs) {
        Map<String, Column> columns = new HashMap<>();
        for (int i = 0; i < nameAndTypePairs.length; i += 2) {
            Column column = Column.extractColumn(newDescriptor(nameAndTypePairs[i], nameAndTypePairs[i + 1]));
            columns.put(column.getName(), column);
        }
        return columns;
    }

    @ParameterizedTest(name = "DESC-05: {0} is skipped because a JSON or Dynamic ancestor exists")
    @ValueSource(strings = {"j.a", "j.b.c", "j.d.x", "j.d.y", "d.a.b", "tj.jj.k"})
    @DisplayName("DESC-05: a row at any depth below a JSON or Dynamic ancestor is skipped")
    void rowsAtAnyDepthBelowJsonOrDynamicAreSkipped(String rowName) {
        Map<String, Column> columns = columnsSoFar(
                "j", "JSON",
                "d", "Dynamic",
                "tj", "Tuple(jj JSON)",
                "tj.jj", "JSON");

        assertTrue(ClickHouseHelperClient.shouldSkipDescribeRow(newDescriptor(rowName, "String"), columns));
    }

    @ParameterizedTest(name = "DESC-05: {0} is kept because no ancestor is JSON or Dynamic")
    @ValueSource(strings = {"nt.a.b", "aj.k", "m.values", "t.a"})
    @DisplayName("DESC-05: the ancestor walk does not widen to Tuple-in-Tuple, Array(JSON), Map or Tuple rows")
    void rowsWithoutJsonOrDynamicAncestorAreNotSkipped(String rowName) {
        Map<String, Column> columns = columnsSoFar(
                "nt", "Tuple(a Tuple(b Int32))",
                "nt.a", "Tuple(b Int32)",
                "aj", "Array(JSON)",
                "m", "Map(String, Int32)",
                "t", "Tuple(a Int32)");

        assertFalse(ClickHouseHelperClient.shouldSkipDescribeRow(newDescriptor(rowName, "Int32"), columns));
    }

    // ---- COLS-10 (D-11 flips): the discovery WARN is gone, the same inputs now assert the new truth ----

    /**
     * Discovery as {@code describeTableV2} runs it for the given rows: one Column per row, the raw
     * type of each root row recorded, then {@code buildDescribedTable}.
     */
    private static Table discover(Map<String, String> declaredTypes, String... nameAndTypePairs) {
        List<Column> columns = new ArrayList<>();
        int numColumns = 0;
        for (int i = 0; i < nameAndTypePairs.length; i += 2) {
            ClickHouseFieldDescriptor descriptor = newDescriptor(nameAndTypePairs[i], nameAndTypePairs[i + 1]);
            columns.add(Column.extractColumn(descriptor));
            if (!descriptor.isSubcolumn()) {
                numColumns++;
            }
        }
        return ClickHouseHelperClient.buildDescribedTable(DATABASE, TABLE, false, columns, numColumns, declaredTypes);
    }

    /** Discovery where every root row's own type is its declared type, as DESCRIBE reports it. */
    private static Table discover(String... nameAndTypePairs) {
        Map<String, String> declaredTypes = new HashMap<>();
        for (int i = 0; i < nameAndTypePairs.length; i += 2) {
            if (!nameAndTypePairs[i].contains(".")) {
                declaredTypes.put(nameAndTypePairs[i], nameAndTypePairs[i + 1]);
            }
        }
        return discover(declaredTypes, nameAndTypePairs);
    }

    private static int warnCount(LogCapture... captures) {
        int count = 0;
        for (LogCapture capture : captures) {
            count += capture.events(WARN_LEVEL).size();
        }
        return count;
    }

    static Stream<Arguments> formerlyFlaggedTypes() {
        return Stream.of(
                Arguments.of(
                        "Nullable(Tuple(a Int32))", false, new String[] {"c", "Nullable(Tuple(a Int32))", "c.a", "Nullable(Int32)"}),
                Arguments.of(
                        "Array(Nullable(Tuple(a Int32, b String)))",
                        true,
                        new String[] {
                            "c", "Array(Nullable(Tuple(a Int32, b String)))",
                            "c.a", "Array(Nullable(Int32))",
                            "c.b", "Array(Nullable(String))"
                        }));
    }

    // COLS-10 flip (D-11): this case asserted that these two raw types were flagged for the discovery
    // WARN. The WARN was deleted with the Tuple fix (D-10); the inputs are kept and now assert that
    // discovery stays silent and the Columns carry the declared nullability: the Tuple nullable, its
    // elements not, although DESCRIBE reported the sub-columns as Nullable.
    @ParameterizedTest(name = "COLS-10 (D-11 flip): {0} discovers with declared nullability and no warning")
    @MethodSource("formerlyFlaggedTypes")
    @DisplayName("COLS-10 (D-11 flip): formerly flagged Nullable(Tuple) types discover with declared nullability and no warning")
    void formerlyFlaggedTupleTypesDiscoverWithDeclaredNullabilityAndNoWarning(
            String rawType, boolean arrayWrapped, String[] rows) {
        try (LogCapture helperLog = LogCapture.attach(ClickHouseHelperClient.class);
                LogCapture nullabilityLog = LogCapture.attach(DeclaredNullability.class)) {
            Column root = discover(rows).getRootColumnsMap().get("c");

            assertEquals(0, warnCount(helperLog, nullabilityLog), rawType);
            Column tuple = arrayWrapped ? root.getArrayType() : root;
            assertEquals(Type.TUPLE, tuple.getType());
            assertTrue(tuple.isNullable(), "the Tuple is declared Nullable");
            assertFalse(tuple.getTupleFields().isEmpty());
            for (Column field : tuple.getTupleFields()) {
                assertFalse(field.isNullable(), field.getName() + " is declared non-nullable");
            }
        }
    }

    // COLS-10 flip (D-11): this case asserted that these ten raw types were not flagged. With the
    // WARN gone the inputs are kept and assert that a root row alone discovers silently, with the
    // root nullability exactly as Column.extractColumn gives it.
    @ParameterizedTest(name = "COLS-10 (D-11 flip): {0} discovers without a warning")
    @ValueSource(
            strings = {
                "Array(Nullable(Int32))",
                "Enum8('a' = 1)",
                "Tuple(a Nullable(Int32))",
                "Nullable(String)",
                "Map(String, Enum16('b' = 2))",
                "LowCardinality(Nullable(String))",
                "Array(Nullable(Enum8('x' = 1)))",
                "Map(String, Nullable(Enum16('y' = 1)))",
                "Nullable(Enum8('z' = 1))",
                "Tuple(a Nullable(Enum8('q' = 1)))"
            })
    @DisplayName("COLS-10 (D-11 flip): formerly unflagged types discover without a warning and keep the root nullability")
    void otherTypesDiscoverWithoutWarningAndUnchangedRootNullability(String rawType) {
        try (LogCapture helperLog = LogCapture.attach(ClickHouseHelperClient.class);
                LogCapture nullabilityLog = LogCapture.attach(DeclaredNullability.class)) {
            Column root = discover("c", rawType).getRootColumnsMap().get("c");

            assertEquals(0, warnCount(helperLog, nullabilityLog), rawType);
            assertEquals(
                    Column.extractColumn(newDescriptor("c", rawType)).isNullable(), root.isNullable(), rawType);
        }
    }

    // COLS-10 flip (D-11): this case asserted that a null raw type is not flagged. The input is kept
    // as a root with no declared type: discovery keeps the nullability DESCRIBE reported, logs
    // nothing and does not throw.
    @Test
    @DisplayName("COLS-10 (D-11 flip): a root with no declared type keeps the DESCRIBE nullability and logs nothing")
    void missingDeclaredTypeKeepsDescribeNullabilityWithoutWarning() {
        String[] rows = {"c", "Nullable(Tuple(a Int32))", "c.a", "Nullable(Int32)"};
        Table reference = discover(Map.of("c", "Nullable(Tuple(a Int32))"), rows);
        try (LogCapture helperLog = LogCapture.attach(ClickHouseHelperClient.class);
                LogCapture nullabilityLog = LogCapture.attach(DeclaredNullability.class)) {
            Column root = discover(new HashMap<>(), rows).getRootColumnsMap().get("c");

            assertEquals(0, helperLog.events().size() + nullabilityLog.events().size());
            assertTrue(root.isNullable(), "extractColumn carries the Tuple's Nullable");
            assertEquals(1, root.getTupleFields().size());
            assertTrue(
                    root.getTupleFields().get(0).isNullable(),
                    "the element keeps the Nullable DESCRIBE reported, since no declared type was recorded");
            assertFalse(
                    reference.getRootColumnsMap().get("c").getTupleFields().get(0).isNullable(),
                    "control: with the declared type the same element is non-nullable");
        }
    }

    @Test
    @DisplayName("COLS-10 IN-01: the fallback WARN raised during discovery names the database and the table")
    void fallbackWarningNamesDatabaseAndTable() {
        try (LogCapture nullabilityLog = LogCapture.attach(DeclaredNullability.class)) {
            discover(
                    Map.of("c", "Nullable(Tuple(a NoSuchType))"),
                    "c", "Nullable(Tuple(a Int32))",
                    "c.a", "Nullable(Int32)");

            List<LogCapture.CapturedEvent> warnings = nullabilityLog.events(WARN_LEVEL);
            assertEquals(1, warnings.size());
            String message = warnings.get(0).message();
            assertTrue(message.contains(DISCOVERED_TABLE), message);
            assertTrue(message.contains("c"), message);
        }
    }

    // COLS-10 flip (D-11): this case asserted a once-per-column-and-type dedup of the WARN. There is
    // no WARN left to dedup; the three identical inputs now assert that repeated discovery of the
    // same table, and of the same column with another type, never warns and always maps the Tuple
    // nullable.
    @Test
    @DisplayName("COLS-10 (D-11 flip): repeated discovery never warns")
    void repeatedDiscoveryNeverWarns() {
        try (LogCapture helperLog = LogCapture.attach(ClickHouseHelperClient.class);
                LogCapture nullabilityLog = LogCapture.attach(DeclaredNullability.class)) {
            for (String rawType : new String[] {
                "Nullable(Tuple(a Int32))", "Nullable(Tuple(a Int32))", "Nullable(Tuple(a Int64))"
            }) {
                Column root = discover("t", rawType).getRootColumnsMap().get("t");
                assertTrue(root.isNullable(), rawType);
            }

            assertEquals(0, warnCount(helperLog, nullabilityLog));
        }
    }

    // COLS-10 flip (D-11): this case asserted that sub-column rows and unflagged roots never warn.
    // The rows are kept; it now asserts the same silence and the declared nullability of the map
    // value Tuple and of the Enum root.
    @Test
    @DisplayName("COLS-10 (D-11 flip): sub-column rows never warn and the map value Tuple takes its declared nullability")
    void subColumnRowsNeverWarn() {
        try (LogCapture helperLog = LogCapture.attach(ClickHouseHelperClient.class);
                LogCapture nullabilityLog = LogCapture.attach(DeclaredNullability.class)) {
            Table table = discover(
                    "m", "Map(String, Nullable(Tuple(a Int32)))",
                    "m.values", "Array(Nullable(Tuple(a Int32)))",
                    "m.values.a", "Array(Nullable(Int32))",
                    "n", "Array(Nullable(Int32))",
                    "e", "Nullable(Enum8('a' = 1))");

            assertEquals(0, warnCount(helperLog, nullabilityLog));
            Column valueTuple = table.getRootColumnsMap().get("m").getMapValueType();
            assertTrue(valueTuple.isNullable(), "the map value is a declared Nullable(Tuple)");
            assertEquals(1, valueTuple.getTupleFields().size());
            assertFalse(valueTuple.getTupleFields().get(0).isNullable());
            assertTrue(table.getRootColumnsMap().get("e").isNullable());
        }
    }
}
