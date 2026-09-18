package com.clickhouse.kafka.connect.sink.db.helper;

import static com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers.newDescriptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free unit pins for the fork's table-discovery helpers extracted in
 * {@link ClickHouseHelperClient}: {@code buildTableFilterClause} (FILT-01..04) and
 * {@code shouldSkipDescribeRow} (DESC-01..03). Deliberately extends nothing — extending
 * {@code ClickHouseBase} would inherit {@code @Tag("clickhouse")} and exclude this class
 * from {@code testUnit} (D-16).
 */
class ClickHouseHelperClientForkUnitTest {

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
}
