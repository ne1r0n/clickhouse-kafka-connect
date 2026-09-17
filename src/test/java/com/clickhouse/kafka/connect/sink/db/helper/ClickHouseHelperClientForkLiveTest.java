package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.clickhouse.kafka.connect.sink.ClickHouseBase;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ClickHouse-backed pins for the fork's table-discovery helpers: {@code targetTableFilter}
 * (FILT-01..04), {@code describeTable}'s subcolumn skip rule (DESC-01..04), and the TYPE-07
 * identifier round-trip half. Runs against ClickHouse {@code latest} with client V2.
 */
class ClickHouseHelperClientForkLiveTest extends ClickHouseBase {
    ClickHouseHelperClient chc = null;

    @BeforeEach
    void setUp() {
        Map<String, String> props = createProps();
        chc = createClient(props);
    }

    @Test
    @DisplayName("DESC-01: describeTable on Nullable/Array/String/JSON/parameterized-JSON/Dynamic columns returns exactly the declared root columns")
    void describeTableReturnsDeclaredColumnsForJsonAndDynamic() {
        String topic = createTopicName("desc01_json_dynamic_test");
        ClickHouseTestHelpers.createTable(chc, topic,
                "CREATE TABLE %s ( `id` UInt32, `nullable_col` Nullable(String), `array_col` Array(String), "
                        + "`plain_str` String, `json_col` JSON, `json_param_col` JSON(max_dynamic_paths=10), "
                        + "`dynamic_col` Dynamic) Engine = MergeTree ORDER BY id");
        try {
            Table table = chc.describeTable(chc.getDatabase(), topic);
            assertNotNull(table);

            Set<String> expected = Set.of(
                    "id", "nullable_col", "array_col", "plain_str", "json_col", "json_param_col", "dynamic_col");
            Set<String> actual = table.getRootColumnsList().stream()
                    .map(Column::getName)
                    .collect(Collectors.toSet());

            assertEquals(expected, actual);
        } finally {
            ClickHouseTestHelpers.dropTable(chc, topic);
        }
    }
}
