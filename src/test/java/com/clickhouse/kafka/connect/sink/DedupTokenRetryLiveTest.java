package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.helper.RowSnapshots;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Live pins of what ClickHouse does with the connector's {@code insert_deduplication_token} when a
 * batch is retried (ROLL-02). The token is {@code <topic>-<partition>-<min>-<max>}, so a retry of
 * the same offset range carries the same token even when its bytes differ. Every outcome below was
 * observed on the server before it was pinned; none is assumed. Every assertion is a typed SQL
 * read-back. Logs carry the server version, the window settings, case keys and row counts only,
 * never a record value.
 */
public class DedupTokenRetryLiveTest extends ClickHouseBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(DedupTokenRetryLiveTest.class);

    static final String LOG_PREFIX = "DEDUP_TOKEN_RETRY";

    private static final String CONNECTOR_NAME_PROP = "name";
    private static final String ID_COLUMN = "id";
    private static final String NAME_COLUMN = "name";
    private static final String COUNT_COLUMN = "n";
    private static final String SETTING_NAME_COLUMN = "name";
    private static final String SETTING_VALUE_COLUMN = "value";
    private static final String REPLICATED_WINDOW_SETTING = "replicated_deduplication_window";
    private static final String REPLICATED_WINDOW_SECONDS_SETTING = "replicated_deduplication_window_seconds";
    private static final String NON_REPLICATED_WINDOW_SETTING = "non_replicated_deduplication_window";

    private static final String ID_NAME_COLUMNS_DDL = "id Int32, name String";
    private static final String CREATE_TABLE_FORMAT = "CREATE TABLE `%s` (%s) ENGINE = %s";
    private static final String MERGE_TREE_SETTINGS_QUERY =
            "SELECT name, value FROM system.merge_tree_settings WHERE name IN ('" + REPLICATED_WINDOW_SETTING
                    + "', '" + REPLICATED_WINDOW_SECONDS_SETTING + "', '" + NON_REPLICATED_WINDOW_SETTING + "')";
    // Grouping makes a row stored twice visible as n = 2 instead of being hidden by a distinct read.
    private static final String GROUPED_READ_BACK_FORMAT =
            "SELECT id, name, toString(count()) AS n FROM `%s` GROUP BY id, name ORDER BY id, name";

    private static final Schema ID_NAME_SCHEMA = SchemaBuilder.struct()
            .field(ID_COLUMN, Schema.INT32_SCHEMA)
            .field(NAME_COLUMN, Schema.STRING_SCHEMA)
            .build();

    private static final String EO_FALSE_REPLICATED_CASE = "eo-false-replicated";
    private static final String CHANGED_NAME_PREFIX = "changed-";
    private static final String ORIGINAL_NAME_PREFIX = "name-";
    private static final int PARTITION = 0;

    /** The engine clause of the table, with the unique replication path filled in. */
    enum Engine {
        REPLICATED("ReplicatedMergeTree('/clickhouse/tables/%s/%s', 'r1') ORDER BY id"),
        NON_REPLICATED_WINDOW("MergeTree ORDER BY id SETTINGS non_replicated_deduplication_window = 100"),
        PLAIN("MergeTree ORDER BY id");

        private final String clauseFormat;

        Engine(String clauseFormat) {
            this.clauseFormat = clauseFormat;
        }

        String clause(String database, String table) {
            return String.format(clauseFormat, database, table);
        }
    }

    @Test
    @DisplayName("D-04: the server version and the deduplication windows are logged and the replicated window is above zero")
    void serverVersionAndWindowsAreLogged() throws Exception {
        try (ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps())) {
            LOGGER.info("{} server version={}", LOG_PREFIX, NullableRoundTrip.serverVersion(chc));
            Map<String, String> windows = new HashMap<>();
            for (Map<String, String> row :
                    RowSnapshots.select(chc, MERGE_TREE_SETTINGS_QUERY, SETTING_NAME_COLUMN, SETTING_VALUE_COLUMN)) {
                windows.put(row.get(SETTING_NAME_COLUMN), row.get(SETTING_VALUE_COLUMN));
            }
            LOGGER.info(
                    "{} settings {}={} {}={} {}={}",
                    LOG_PREFIX,
                    REPLICATED_WINDOW_SETTING,
                    windows.get(REPLICATED_WINDOW_SETTING),
                    REPLICATED_WINDOW_SECONDS_SETTING,
                    windows.get(REPLICATED_WINDOW_SECONDS_SETTING),
                    NON_REPLICATED_WINDOW_SETTING,
                    windows.get(NON_REPLICATED_WINDOW_SETTING));
            assertTrue(windows.containsKey(REPLICATED_WINDOW_SETTING), "system.merge_tree_settings lists " + windows);
            assertTrue(
                    Long.parseLong(windows.get(REPLICATED_WINDOW_SETTING)) > 0,
                    "a replicated table only deduplicates while " + REPLICATED_WINDOW_SETTING + " is above zero");
        }
    }

    @Test
    @DisplayName("D-14: with exactlyOnce=false a re-delivered range on ReplicatedMergeTree is dropped by its token, identical or changed")
    void redeliveredRangeOnReplicatedTableIsDroppedWithoutExactlyOnce(TestInfo testInfo) throws Exception {
        assertRedeliveredRangeOutcome(testInfo, Engine.REPLICATED, EO_FALSE_REPLICATED_CASE, true);
    }

    /**
     * Three puts through three fresh tasks (an empty in-memory state each) of the same offsets 0-2:
     * the original rows, the same offsets with every name changed, and the original rows again.
     * {@code dropped} says whether the table's engine deduplicates, so the table must read back as
     * the first put left it after every step, or hold each row of each put.
     */
    private void assertRedeliveredRangeOutcome(TestInfo testInfo, Engine engine, String caseKey, boolean dropped)
            throws Exception {
        Map<String, String> props = new HashMap<>(getBaseProps());
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        String table = createTopicName(caseKey.replace('-', '_'));
        try (ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props)) {
            createTable(chc, table, ID_NAME_COLUMNS_DDL, engine);
            try {
                List<String> original = List.of(ORIGINAL_NAME_PREFIX + 1, ORIGINAL_NAME_PREFIX + 2, ORIGINAL_NAME_PREFIX + 3);
                List<String> changed = List.of(CHANGED_NAME_PREFIX + 1, CHANGED_NAME_PREFIX + 2, CHANGED_NAME_PREFIX + 3);

                NullableRoundTrip.putAndStop(props, idNameRecords(table, 0, original));
                List<Map<String, String>> afterFirst = groupedRows(original, 1);
                assertAndLog(chc, table, caseKey, 1, afterFirst);

                NullableRoundTrip.putAndStop(props, idNameRecords(table, 0, changed));
                assertAndLog(chc, table, caseKey, 2, dropped ? afterFirst : merge(afterFirst, groupedRows(changed, 1)));

                NullableRoundTrip.putAndStop(props, idNameRecords(table, 0, original));
                List<Map<String, String>> afterThird =
                        dropped ? afterFirst : merge(afterFirst, groupedRows(changed, 1), groupedRows(original, 1));
                assertAndLog(chc, table, caseKey, 3, afterThird);
            } finally {
                ClickHouseTestHelpers.dropTable(chc, table);
            }
        }
    }

    private static void createTable(ClickHouseHelperClient chc, String table, String columnsDdl, Engine engine) {
        NullableRoundTrip.createTable(
                chc, table, String.format(CREATE_TABLE_FORMAT, "%s", columnsDdl, engine.clause(chc.getDatabase(), table)));
    }

    /** One schema'd record per name, ids 1..n, on offsets firstOffset.. of the one partition. */
    private static List<SinkRecord> idNameRecords(String topic, long firstOffset, List<String> names) {
        List<SinkRecord> records = new ArrayList<>(names.size());
        for (int i = 0; i < names.size(); i++) {
            Struct value = new Struct(ID_NAME_SCHEMA).put(ID_COLUMN, i + 1).put(NAME_COLUMN, names.get(i));
            records.add(new SinkRecord(topic, PARTITION, null, null, ID_NAME_SCHEMA, value, firstOffset + i));
        }
        return records;
    }

    /** The grouped read-back rows a put of these names leaves, each stored {@code count} times. */
    private static List<Map<String, String>> groupedRows(List<String> names, int count) {
        List<Map<String, String>> rows = new ArrayList<>(names.size());
        for (int i = 0; i < names.size(); i++) {
            rows.add(row(i + 1, names.get(i), count));
        }
        return rows;
    }

    private static Map<String, String> row(int id, String name, int count) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put(ID_COLUMN, String.valueOf(id));
        row.put(NAME_COLUMN, name);
        row.put(COUNT_COLUMN, String.valueOf(count));
        return row;
    }

    /** The union of grouped rows, ordered the way the read-back orders them (id, then name). */
    @SafeVarargs
    private static List<Map<String, String>> merge(List<Map<String, String>>... parts) {
        Map<String, Map<String, String>> byKey = new TreeMap<>();
        for (List<Map<String, String>> part : parts) {
            for (Map<String, String> row : part) {
                byKey.merge(
                        String.format("%010d|%s", Integer.parseInt(row.get(ID_COLUMN)), row.get(NAME_COLUMN)),
                        new LinkedHashMap<>(row),
                        (left, right) -> {
                            left.put(
                                    COUNT_COLUMN,
                                    String.valueOf(Integer.parseInt(left.get(COUNT_COLUMN))
                                            + Integer.parseInt(right.get(COUNT_COLUMN))));
                            return left;
                        });
            }
        }
        return new ArrayList<>(byKey.values());
    }

    private static void assertAndLog(
            ClickHouseHelperClient chc, String table, String caseKey, int step, List<Map<String, String>> expected)
            throws Exception {
        List<Map<String, String>> actual = RowSnapshots.select(
                chc, String.format(GROUPED_READ_BACK_FORMAT, table), ID_COLUMN, NAME_COLUMN, COUNT_COLUMN);
        LOGGER.info("{} outcome case={} step={} rows={}", LOG_PREFIX, caseKey, step, totalRows(actual));
        assertEquals(expected, actual, "typed read-back of case " + caseKey + " after step " + step);
    }

    private static int totalRows(List<Map<String, String>> grouped) {
        return grouped.stream().mapToInt(row -> Integer.parseInt(row.get(COUNT_COLUMN))).sum();
    }
}
