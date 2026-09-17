package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.ClickHouseBase;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkTask;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.helper.SchemalessTestData;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

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

    @Test
    @DisplayName("FILT-01: no targetTableFilter maps every table this test class created")
    void noFilterMapsAllTablesThisClassCreated() {
        String stem = createTopicName("filt01");
        String t1 = stem + "_a";
        String t2 = stem + "_b";
        String t3 = stem + "_c";
        String ddl = "CREATE TABLE %s ( `id` UInt32 ) Engine = MergeTree ORDER BY id";
        ClickHouseTestHelpers.createTable(chc, t1, ddl);
        ClickHouseTestHelpers.createTable(chc, t2, ddl);
        ClickHouseTestHelpers.createTable(chc, t3, ddl);
        try {
            List<Table> tables = chc.showTables(chc.getDatabase());
            Set<String> names = tables.stream().map(Table::getCleanName).collect(Collectors.toSet());
            assertTrue(names.containsAll(Set.of(t1, t2, t3)));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, t1);
            ClickHouseTestHelpers.dropTable(chc, t2);
            ClickHouseTestHelpers.dropTable(chc, t3);
        }
    }

    @Test
    @DisplayName("FILT-02: filter matching nothing makes start() throw regardless of suppressTableExistenceException")
    void filterMatchingNothingFailsRegardlessOfSuppressFlag(TestInfo testInfo) {
        String topic = createTopicName("filt02a");
        ClickHouseTestHelpers.createTable(chc, topic, "CREATE TABLE %s ( `id` UInt32 ) Engine = MergeTree ORDER BY id");
        try {
            for (String suppress : new String[] {"true", "false"}) {
                Map<String, String> props = createProps();
                props.put("name", uniqueConnectorName(testInfo));
                props.put(ClickHouseSinkConfig.TARGET_TABLE_FILTER, "no_such_table_prefix_zzz");
                props.put(ClickHouseSinkConfig.SUPPRESS_TABLE_EXISTENCE_EXCEPTION, suppress);
                createClient(props); // mutates props to point at this class's per-class test database
                ClickHouseSinkTask chst = new ClickHouseSinkTask();
                RuntimeException ex = assertThrows(RuntimeException.class, () -> chst.start(props));
                assertTrue(
                        messageChainContains(ex, "Connection to ClickHouse is not active."),
                        "suppress=" + suppress + " message chain: " + ex);
            }
        } finally {
            ClickHouseTestHelpers.dropTable(chc, topic);
        }
    }

    @Test
    @DisplayName("FILT-02: filter matching another table but not the target is governed by suppressTableExistenceException")
    void filterMatchingOtherTableGovernsSuppressBehavior(TestInfo testInfo) {
        String stem = createTopicName("filt02b");
        String matchedTable = stem + "_matched";
        String targetTable = stem + "_target";
        String ddl = "CREATE TABLE %s ( `off16` Int16, `str` String, `p_int8` Int8, `p_int16` Int16, `p_int32` Int32, "
                + "`p_int64` Int64, `p_float32` Float32, `p_float64` Float64, `p_bool` Bool) Engine = MergeTree ORDER BY off16";
        ClickHouseTestHelpers.createTable(chc, matchedTable, ddl);
        ClickHouseTestHelpers.createTable(chc, targetTable, ddl);
        try {
            // suppress=true: put() completes, no rows land in the unmatched target table.
            Map<String, String> suppressedProps = createProps();
            suppressedProps.put("name", uniqueConnectorName(testInfo));
            suppressedProps.put(ClickHouseSinkConfig.TARGET_TABLE_FILTER, "^" + matchedTable + "$");
            suppressedProps.put(ClickHouseSinkConfig.SUPPRESS_TABLE_EXISTENCE_EXCEPTION, "true");
            createClient(suppressedProps); // mutates props to point at this class's per-class test database
            ClickHouseSinkTask suppressedTask = new ClickHouseSinkTask();
            suppressedTask.start(suppressedProps);
            try {
                Collection<SinkRecord> records = SchemalessTestData.createPrimitiveTypes(targetTable, 1, 1);
                suppressedTask.put(records);
            } finally {
                suppressedTask.stop();
            }
            assertEquals(0, ClickHouseTestHelpers.countRows(chc, targetTable));

            // suppress=false: put() throws, message chain names the missing target table.
            Map<String, String> throwingProps = createProps();
            throwingProps.put("name", uniqueConnectorName(testInfo));
            throwingProps.put(ClickHouseSinkConfig.TARGET_TABLE_FILTER, "^" + matchedTable + "$");
            throwingProps.put(ClickHouseSinkConfig.SUPPRESS_TABLE_EXISTENCE_EXCEPTION, "false");
            createClient(throwingProps); // mutates props to point at this class's per-class test database
            ClickHouseSinkTask throwingTask = new ClickHouseSinkTask();
            throwingTask.start(throwingProps);
            try {
                Collection<SinkRecord> records = SchemalessTestData.createPrimitiveTypes(targetTable, 1, 1);
                RuntimeException ex = assertThrows(RuntimeException.class, () -> throwingTask.put(records));
                assertTrue(messageChainContains(ex, "does not exist"), "message chain: " + ex);
            } finally {
                throwingTask.stop();
            }
        } finally {
            ClickHouseTestHelpers.dropTable(chc, matchedTable);
            ClickHouseTestHelpers.dropTable(chc, targetTable);
        }
    }

    @Test
    @DisplayName("FILT-03: a targetTableFilter containing a single quote parses without a ClickHouse syntax error")
    void singleQuoteFilterParsesWithoutSyntaxError() {
        String topic = createTopicName("filt03");
        ClickHouseTestHelpers.createTable(chc, topic, "CREATE TABLE %s ( `id` UInt32 ) Engine = MergeTree ORDER BY id");
        try {
            Map<String, String> props = createProps();
            props.put(ClickHouseSinkConfig.TARGET_TABLE_FILTER, "o'brien");
            ClickHouseHelperClient filteredChc = createClient(props);
            List<Table> tables = filteredChc.showTables(filteredChc.getDatabase());
            assertNotNull(tables);
            assertTrue(tables.isEmpty());
        } finally {
            ClickHouseTestHelpers.dropTable(chc, topic);
        }
    }

    @Test
    @DisplayName("FILT-04: unanchored \\d+ also matches the substring case; anchored form excludes it (D-19)")
    void backslashDigitPatternSelectsByExactIdentity() {
        String stem = createTopicName("filt04");
        String tbl1 = stem + "_tbl_1";
        String tbl22 = stem + "_tbl_22";
        String tblx = stem + "_tblx";
        String xtbl1y = "x" + stem + "_tbl_1y";
        String ddl = "CREATE TABLE %s ( `id` UInt32 ) Engine = MergeTree ORDER BY id";
        ClickHouseTestHelpers.createTable(chc, tbl1, ddl);
        ClickHouseTestHelpers.createTable(chc, tbl22, ddl);
        ClickHouseTestHelpers.createTable(chc, tblx, ddl);
        ClickHouseTestHelpers.createTable(chc, xtbl1y, ddl);
        try {
            Map<String, String> unanchoredProps = createProps();
            unanchoredProps.put(ClickHouseSinkConfig.TARGET_TABLE_FILTER, stem + "_tbl_\\d+");
            ClickHouseHelperClient unanchoredChc = createClient(unanchoredProps);
            Set<String> unanchored = unanchoredChc.showTables(unanchoredChc.getDatabase()).stream()
                    .map(Table::getCleanName)
                    .collect(Collectors.toSet());

            Map<String, String> anchoredProps = createProps();
            anchoredProps.put(ClickHouseSinkConfig.TARGET_TABLE_FILTER, "^" + stem + "_tbl_\\d+$");
            ClickHouseHelperClient anchoredChc = createClient(anchoredProps);
            Set<String> anchored = anchoredChc.showTables(anchoredChc.getDatabase()).stream()
                    .map(Table::getCleanName)
                    .collect(Collectors.toSet());

            assertTrue(unanchored.contains(tbl1));
            assertTrue(unanchored.contains(tbl22));
            assertTrue(unanchored.contains(xtbl1y));
            assertFalse(unanchored.contains(tblx));

            assertTrue(anchored.contains(tbl1));
            assertTrue(anchored.contains(tbl22));
            assertFalse(anchored.contains(xtbl1y));
            assertFalse(anchored.contains(tblx));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tbl1);
            ClickHouseTestHelpers.dropTable(chc, tbl22);
            ClickHouseTestHelpers.dropTable(chc, tblx);
            ClickHouseTestHelpers.dropTable(chc, xtbl1y);
        }
    }

    /** Walks the {@code getCause()} chain looking for a message containing {@code substring} (D-17: substring, never full-string equality). */
    private static boolean messageChainContains(Throwable t, String substring) {
        while (t != null) {
            String message = t.getMessage();
            if (message != null && message.contains(substring)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
