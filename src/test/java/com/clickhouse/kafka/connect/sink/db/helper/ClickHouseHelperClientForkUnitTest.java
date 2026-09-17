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
    @DisplayName("DESC-03: subcolumn of a registered JSON parent is skipped")
    void jsonParentSubcolumnIsSkipped() {
        Table table = new Table("db", "t1");
        table.addColumn(Column.extractColumn(newDescriptor("json_col", "JSON")));

        boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(
                newDescriptor("json_col.a", "Dynamic"), table);

        assertTrue(skipped);
    }
}
