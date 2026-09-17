package com.clickhouse.kafka.connect.sink.db.helper;

import static com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers.newDescriptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
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
        Table table = new Table("db", "t1");
        table.addColumn(Column.extractColumn(newDescriptor("nullable_col", "Nullable(String)")));

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(
                newDescriptor("nullable_col.null", "UInt8"), table);

        assertTrue(skipped);
    }

    @ParameterizedTest(name = "DESC-02: {0} subcolumn of an Array parent is skipped")
    @ValueSource(strings = {".size0", ".size"})
    @DisplayName("DESC-02: array-length subcolumn suffixes (.size0 and the D-20 bare .size) are skipped")
    void arrayLengthSuffixesAreSkipped(String suffix) {
        Table table = new Table("db", "t1");
        table.addColumn(Column.extractColumn(newDescriptor("array_col", "Array(String)")));

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(
                newDescriptor("array_col" + suffix, "UInt64"), table);

        assertTrue(skipped);
    }

    @Test
    @DisplayName("DESC-02: a root column literally named size0 (isSubcolumn=false) is never skipped")
    void rootColumnNamedSize0IsNotSkipped() {
        Table table = new Table("db", "t1");

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(newDescriptor("size0", "UInt64"), table);

        assertFalse(skipped);
    }

    @Test
    @DisplayName("DESC-02: a subcolumn name beginning with a dot does not throw and is not skipped")
    void leadingDotSubcolumnNameDoesNotThrow() {
        Table table = new Table("db", "t1");
        ClickHouseFieldDescriptor leadingDot = ClickHouseFieldDescriptor.builder()
                .name(".foo")
                .type("UInt64")
                .isSubcolumn(true)
                .build();

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(leadingDot, table);

        assertFalse(skipped);
    }

    @Test
    @DisplayName("DESC-02: a subcolumn whose parent has not yet been registered returns false, not a throw")
    void subcolumnOverUnregisteredParentReturnsFalse() {
        Table table = new Table("db", "t1"); // parent "json_col" intentionally never added

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(newDescriptor("json_col.a", "Dynamic"), table);

        assertFalse(skipped);
    }

    @Test
    @DisplayName("DESC-03: subcolumn of a registered JSON parent is skipped")
    void jsonParentSubcolumnIsSkipped() {
        Table table = new Table("db", "t1");
        table.addColumn(Column.extractColumn(newDescriptor("json_col", "JSON")));

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(
                newDescriptor("json_col.a", "Dynamic"), table);

        assertTrue(skipped);
    }

    @Test
    @DisplayName("DESC-03: subcolumn of a registered Dynamic parent is skipped")
    void dynamicParentSubcolumnIsSkipped() {
        Table table = new Table("db", "t1");
        table.addColumn(Column.extractColumn(newDescriptor("dynamic_col", "Dynamic")));

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(
                newDescriptor("dynamic_col.a", "String"), table);

        assertTrue(skipped);
    }

    @Test
    @DisplayName("DESC-03: a JSON parent's shared-data subcolumn with no trailing dot is still skipped (fixes the trailing-dot defect)")
    void jsonSharedDataSubcolumnWithoutTrailingDotIsSkipped() {
        Table table = new Table("db", "t1");
        table.addColumn(Column.extractColumn(newDescriptor("json_col", "JSON")));

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(
                newDescriptor("json_col.object_shared_data", "String"), table);

        assertTrue(skipped);
    }

    @Test
    @DisplayName("DESC-03: a root column whose name merely contains the shared-data text is never skipped (fixes the operator-precedence defect)")
    void rootColumnContainingSharedDataTextIsNotSkipped() {
        Table table = new Table("db", "t1");
        ClickHouseFieldDescriptor rootWithSharedDataText = ClickHouseFieldDescriptor.builder()
                .name("json_col.object_shared_data")
                .type("String")
                .isSubcolumn(false)
                .build();

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(rootWithSharedDataText, table);

        assertFalse(skipped);
    }

    @Test
    @DisplayName("DESC-03: Map key/value and Tuple field subcolumns are never skipped, so Table.updateParent keeps consuming them")
    void mapAndTupleSubcolumnsAreNotSkipped() {
        Table table = new Table("db", "t1");
        table.addColumn(Column.extractColumn(newDescriptor("map_col", "Map(String, String)")));
        table.addColumn(Column.extractColumn(newDescriptor("tuple_col", "Tuple(a String, b String)")));

        assertFalse(ClickHouseHelperClient.shouldSkipDescribeRow(newDescriptor("map_col.keys", "String"), table));
        assertFalse(ClickHouseHelperClient.shouldSkipDescribeRow(newDescriptor("map_col.values", "String"), table));
        assertFalse(ClickHouseHelperClient.shouldSkipDescribeRow(newDescriptor("tuple_col.a", "String"), table));
    }
}
