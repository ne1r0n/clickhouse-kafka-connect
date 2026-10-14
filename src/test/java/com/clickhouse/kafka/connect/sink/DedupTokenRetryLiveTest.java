package com.clickhouse.kafka.connect.sink;

import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.allRows;
import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.cdcSinkRecords;
import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.emptyFirstEnvelopes;
import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.mixedBatchEnvelopes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.insert.InsertResponse;
import com.clickhouse.client.api.insert.InsertSettings;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.helper.RowSnapshots;
import com.clickhouse.kafka.connect.sink.state.State;
import com.clickhouse.kafka.connect.sink.state.StateRecord;
import com.clickhouse.kafka.connect.sink.state.provider.KeeperStateProvider;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
    private static final String BYPASS_ROW_BINARY_PROP = "bypassRowBinary";
    private static final String ID_COLUMN = "id";
    private static final String NAME_COLUMN = "name";
    private static final String COUNT_COLUMN = "n";
    private static final String AMOUNT_COLUMN = "amount";
    private static final String VERSION_COLUMN = "v";
    private static final String DELETED_COLUMN = "del";
    private static final String SETTING_NAME_COLUMN = "name";
    private static final String SETTING_VALUE_COLUMN = "value";
    private static final String REPLICATED_WINDOW_SETTING = "replicated_deduplication_window";
    private static final String REPLICATED_WINDOW_SECONDS_SETTING = "replicated_deduplication_window_seconds";
    private static final String NON_REPLICATED_WINDOW_SETTING = "non_replicated_deduplication_window";

    private static final String ID_NAME_COLUMNS_DDL = "id Int32, name String";
    private static final String CDC_COLUMNS_DDL = "id Int32, ts DateTime64(3, 'UTC'), amount Decimal(18,4), "
            + "name String, _version UInt64, is_deleted UInt8";
    private static final String CREATE_TABLE_FORMAT = "CREATE TABLE `%s` (%s) ENGINE = %s";
    private static final String MERGE_TREE_SETTINGS_QUERY =
            "SELECT name, value FROM system.merge_tree_settings WHERE name IN ('" + REPLICATED_WINDOW_SETTING
                    + "', '" + REPLICATED_WINDOW_SECONDS_SETTING + "', '" + NON_REPLICATED_WINDOW_SETTING + "')";
    // Grouping makes a row stored twice visible as n = 2 instead of being hidden by a distinct read.
    private static final String GROUPED_READ_BACK_FORMAT =
            "SELECT id, name, toString(count()) AS n FROM `%s` GROUP BY id, name ORDER BY id, name";
    // ts is not read back: the bypassRowBinary route does not store the source instant, and
    // EmptyCdcSkipRouteLiveTest leaves it out of its read-back for that route too.
    private static final String CDC_READ_BACK_FORMAT = "SELECT id, toString(amount) AS amount, name, "
            + "toString(_version) AS v, toString(is_deleted) AS del FROM `%s` ORDER BY id";
    private static final String[] CDC_READ_BACK_COLUMNS = {
        ID_COLUMN, AMOUNT_COLUMN, NAME_COLUMN, VERSION_COLUMN, DELETED_COLUMN
    };
    private static final String ID_NAME_JSON_FORMAT = "{\"id\":%s,\"name\":\"%s\"}\n";
    private static final String CDC_JSON_FORMAT =
            "{\"id\":%s,\"amount\":\"%s\",\"name\":\"%s\",\"_version\":%s,\"is_deleted\":%s}\n";

    private static final Schema ID_NAME_SCHEMA = SchemaBuilder.struct()
            .field(ID_COLUMN, Schema.INT32_SCHEMA)
            .field(NAME_COLUMN, Schema.STRING_SCHEMA)
            .build();

    private static final String CHANGED_NAME_PREFIX = "changed-";
    private static final String ORIGINAL_NAME_PREFIX = "name-";
    private static final String EO_FALSE_CASE_PREFIX = "eo-false-";
    private static final String EO_TRUE_LANDED_CASE = "eo-true-landed";
    private static final String EO_TRUE_NOT_LANDED_CASE = "eo-true-not-landed";
    private static final String SERVER_MATRIX_CASE_PREFIX = "server-";
    private static final int PARTITION = 0;

    // The values the pre-fix connector stored for an empty CDC record (backlog ledger L-30).
    private static final String REAL_AMOUNT = "12.3456";
    private static final String PHANTOM_AMOUNT = "0";
    private static final String CREATED_NAME = "created";
    private static final String DELETED_NAME = "deleted";
    private static final String CREATED_THREE_NAME = "created-3";
    private static final long FIRST_CDC_OFFSET = 1;
    private static final long LAST_CDC_OFFSET = 4;
    private static final long NEXT_CDC_OFFSET = 5;

    /** The engine clause of the table, with the unique replication path filled in. */
    enum Engine {
        REPLICATED("ReplicatedMergeTree('/clickhouse/tables/%s/%s', 'r1') ORDER BY id", true),
        NON_REPLICATED_WINDOW("MergeTree ORDER BY id SETTINGS non_replicated_deduplication_window = 100", true),
        PLAIN("MergeTree ORDER BY id", false);

        private final String clauseFormat;
        private final boolean deduplicates;

        Engine(String clauseFormat, boolean deduplicates) {
            this.clauseFormat = clauseFormat;
            this.deduplicates = deduplicates;
        }

        String clause(String database, String table) {
            return String.format(clauseFormat, database, table);
        }

        String key() {
            return name().toLowerCase().replace('_', '-');
        }
    }

    /** One server-matrix insert: the block, its token and whether a deduplicating table keeps it. */
    private record Step(List<Map<String, String>> block, String token, boolean landsOnDeduplicatingTable) {}

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

    @ParameterizedTest(name = "D-13: same-token and different-token inserts on {0}")
    @EnumSource(Engine.class)
    void sameTokenAndDifferentTokenInserts(Engine engine) throws Exception {
        String table = createTopicName("dedup_matrix_" + engine.name().toLowerCase());
        String caseKey = SERVER_MATRIX_CASE_PREFIX + engine.key();
        // Tokens come from the connector's own QueryIdentifier, never from a copied format string.
        String firstToken = tokenFor(table, 0, 1);
        String otherToken = tokenFor(table, 2, 3);
        List<Map<String, String>> block = groupedRows(List.of(ORIGINAL_NAME_PREFIX + 1, ORIGINAL_NAME_PREFIX + 2), 1);
        List<Map<String, String>> changedBlock = List.of(row(1, CHANGED_NAME_PREFIX + 1, 1));
        List<Step> steps = List.of(
                new Step(block, firstToken, true),
                new Step(block, firstToken, false),
                new Step(changedBlock, firstToken, false),
                new Step(block, otherToken, true));

        try (ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps())) {
            createTable(chc, table, ID_NAME_COLUMNS_DDL, engine);
            try {
                List<List<Map<String, String>>> landed = new ArrayList<>();
                int stepNumber = 0;
                for (Step step : steps) {
                    stepNumber++;
                    insertJson(chc, table, idNameJson(step.block()), step.token());
                    if (!engine.deduplicates || step.landsOnDeduplicatingTable()) {
                        landed.add(step.block());
                    }
                    assertGroupedAndLog(chc, table, caseKey, stepNumber, merge(landed));
                }
            } finally {
                ClickHouseTestHelpers.dropTable(chc, table);
            }
        }
    }

    @Test
    @DisplayName("D-14: with exactlyOnce=false a re-delivered range on ReplicatedMergeTree is dropped by its token, identical or changed")
    void redeliveredRangeOnReplicatedTableIsDroppedWithoutExactlyOnce(TestInfo testInfo) throws Exception {
        assertRedeliveredRangeOutcome(testInfo, Engine.REPLICATED);
    }

    @Test
    @DisplayName("D-14: a re-delivered range on a plain MergeTree is stored a second time, identical or changed")
    void redeliveredRangeOnPlainTableIsStoredTwice(TestInfo testInfo) throws Exception {
        assertRedeliveredRangeOutcome(testInfo, Engine.PLAIN);
    }

    @Test
    @DisplayName("D-13: an exactlyOnce retry of a landed range is dropped and keeps the earlier rows")
    void exactlyOnceRetryOfLandedRangeKeepsEarlierRows(TestInfo testInfo) throws Exception {
        assertExactlyOnceRetry(testInfo, EO_TRUE_LANDED_CASE, true);
    }

    @Test
    @DisplayName("D-13: an exactlyOnce retry of a range that never landed stores only the real rows")
    void exactlyOnceRetryOfUnlandedRangeStoresOnlyRealRows(TestInfo testInfo) throws Exception {
        assertExactlyOnceRetry(testInfo, EO_TRUE_NOT_LANDED_CASE, false);
    }

    /**
     * Three puts through three fresh tasks (an empty in-memory state each) of the same offsets 0-2:
     * the original rows, the same offsets with every name changed, and the original rows again. A
     * deduplicating engine keeps only what the first put left; a plain table keeps every put.
     */
    private void assertRedeliveredRangeOutcome(TestInfo testInfo, Engine engine) throws Exception {
        Map<String, String> props = new HashMap<>(getBaseProps());
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        String caseKey = EO_FALSE_CASE_PREFIX + engine.key();
        String table = createTopicName(caseKey.replace('-', '_'));
        List<String> originalNames = List.of(ORIGINAL_NAME_PREFIX + 1, ORIGINAL_NAME_PREFIX + 2, ORIGINAL_NAME_PREFIX + 3);
        List<String> changedNames = List.of(CHANGED_NAME_PREFIX + 1, CHANGED_NAME_PREFIX + 2, CHANGED_NAME_PREFIX + 3);
        List<Map<String, String>> original = groupedRows(originalNames, 1);
        List<Map<String, String>> changed = groupedRows(changedNames, 1);

        try (ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props)) {
            createTable(chc, table, ID_NAME_COLUMNS_DDL, engine);
            try {
                List<List<Map<String, String>>> landed = new ArrayList<>();
                landed.add(original);
                NullableRoundTrip.putAndStop(props, idNameRecords(table, originalNames));
                assertGroupedAndLog(chc, table, caseKey, 1, merge(landed));

                if (!engine.deduplicates) {
                    landed.add(changed);
                }
                NullableRoundTrip.putAndStop(props, idNameRecords(table, changedNames));
                assertGroupedAndLog(chc, table, caseKey, 2, merge(landed));

                if (!engine.deduplicates) {
                    landed.add(original);
                }
                NullableRoundTrip.putAndStop(props, idNameRecords(table, originalNames));
                assertGroupedAndLog(chc, table, caseKey, 3, merge(landed));
            } finally {
                ClickHouseTestHelpers.dropTable(chc, table);
            }
        }
    }

    /**
     * A range left BEFORE_PROCESSING is retried by a task that skips the empty CDC records. When the
     * earlier attempt landed it is simulated as the pre-fix connector wrote it (the two real rows
     * plus an all-defaults phantom row for each empty record) under the token the connector sends.
     */
    private void assertExactlyOnceRetry(TestInfo testInfo, String caseKey, boolean earlierAttemptLanded)
            throws Exception {
        String id = "dedup_eo_" + System.nanoTime();
        String topic = createTopicName(caseKey.replace('-', '_'));
        Map<String, String> props = new HashMap<>(getBaseProps());
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        props.put(ClickHouseSinkConfig.EXACTLY_ONCE, "true");
        props.put(ClickHouseSinkConfig.ZK_PATH, "/" + id);
        // zkDatabase names the KeeperMap TABLE; tables sharing a path share data, so it is unique per test.
        props.put(ClickHouseSinkConfig.ZK_DATABASE, id);
        props.put(ClickHouseSinkConfig.DEBEZIUM_CDC_ENABLED, "true");
        props.put(BYPASS_ROW_BINARY_PROP, "true");

        List<Map<String, String>> realRows = List.of(
                cdcRow(1, REAL_AMOUNT, CREATED_NAME, 100, 0), cdcRow(2, REAL_AMOUNT, DELETED_NAME, 101, 1));
        Map<String, String> phantom = cdcRow(0, PHANTOM_AMOUNT, "", 0, 0);
        List<Map<String, String>> earlierAttemptRows = List.of(phantom, phantom, realRows.get(0), realRows.get(1));
        List<Map<String, String>> expectedAfterRetry = earlierAttemptLanded ? earlierAttemptRows : realRows;

        try (ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props)) {
            createTable(chc, topic, CDC_COLUMNS_DDL, Engine.REPLICATED);
            ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
            try {
                if (earlierAttemptLanded) {
                    insertJson(chc, topic, cdcJson(earlierAttemptRows), tokenFor(topic, FIRST_CDC_OFFSET, LAST_CDC_OFFSET));
                }
                // StateRecord takes maxOffset BEFORE minOffset.
                new KeeperStateProvider(new ClickHouseSinkConfig(props))
                        .setStateRecord(new StateRecord(
                                topic, PARTITION, LAST_CDC_OFFSET, FIRST_CDC_OFFSET, State.BEFORE_PROCESSING, topic));
                sinkTask.start(props);

                sinkTask.put(cdcSinkRecords(topic, PARTITION, FIRST_CDC_OFFSET, mixedBatchEnvelopes()));
                assertCdcAndLog(chc, topic, caseKey, 1, expectedAfterRetry);
                StateRecord afterRetry = new KeeperStateProvider(new ClickHouseSinkConfig(props))
                        .getStateRecord(topic, PARTITION);
                assertEquals(State.AFTER_PROCESSING, afterRetry.getState());
                assertEquals(FIRST_CDC_OFFSET, afterRetry.getMinOffset());
                assertEquals(LAST_CDC_OFFSET, afterRetry.getMaxOffset());

                // The next range is new: it lands only its real row.
                sinkTask.put(cdcSinkRecords(topic, PARTITION, NEXT_CDC_OFFSET, emptyFirstEnvelopes()));
                List<Map<String, String>> withNextRow = new ArrayList<>(expectedAfterRetry);
                withNextRow.add(cdcRow(3, REAL_AMOUNT, CREATED_THREE_NAME, 141, 0));
                assertCdcAndLog(chc, topic, caseKey, 2, withNextRow);
            } finally {
                sinkTask.stop();
                ClickHouseTestHelpers.dropTable(chc, topic);
                ClickHouseTestHelpers.dropTable(chc, id);
            }
        }
    }

    private static void createTable(ClickHouseHelperClient chc, String table, String columnsDdl, Engine engine) {
        NullableRoundTrip.createTable(
                chc,
                table,
                String.format(CREATE_TABLE_FORMAT, "%s", columnsDdl, engine.clause(chc.getDatabase(), table)));
    }

    private static String tokenFor(String topic, long minOffset, long maxOffset) {
        return new QueryIdentifier(topic, PARTITION, minOffset, maxOffset, UUID.randomUUID().toString())
                .getDeduplicationToken();
    }

    private static void insertJson(ClickHouseHelperClient chc, String table, String body, String token)
            throws Exception {
        try (InsertResponse ignored = chc.getClient()
                .insert(
                        table,
                        new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)),
                        ClickHouseFormat.JSONEachRow,
                        new InsertSettings().setDeduplicationToken(token))
                .get()) {
            // inserted
        }
    }

    private static String idNameJson(List<Map<String, String>> rows) {
        StringBuilder body = new StringBuilder();
        for (Map<String, String> row : rows) {
            body.append(String.format(ID_NAME_JSON_FORMAT, row.get(ID_COLUMN), row.get(NAME_COLUMN)));
        }
        return body.toString();
    }

    private static String cdcJson(List<Map<String, String>> rows) {
        StringBuilder body = new StringBuilder();
        for (Map<String, String> row : rows) {
            body.append(String.format(
                    CDC_JSON_FORMAT,
                    row.get(ID_COLUMN),
                    row.get(AMOUNT_COLUMN),
                    row.get(NAME_COLUMN),
                    row.get(VERSION_COLUMN),
                    row.get(DELETED_COLUMN)));
        }
        return body.toString();
    }

    /** One schema'd record per name, ids 1..n, on offsets 0..n-1 of the one partition. */
    private static List<SinkRecord> idNameRecords(String topic, List<String> names) {
        List<SinkRecord> records = new ArrayList<>(names.size());
        for (int i = 0; i < names.size(); i++) {
            Struct value = new Struct(ID_NAME_SCHEMA).put(ID_COLUMN, i + 1).put(NAME_COLUMN, names.get(i));
            records.add(new SinkRecord(topic, PARTITION, null, null, ID_NAME_SCHEMA, value, i));
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

    private static Map<String, String> cdcRow(int id, String amount, String name, int version, int deleted) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put(ID_COLUMN, String.valueOf(id));
        row.put(AMOUNT_COLUMN, amount);
        row.put(NAME_COLUMN, name);
        row.put(VERSION_COLUMN, String.valueOf(version));
        row.put(DELETED_COLUMN, String.valueOf(deleted));
        return row;
    }

    /** The grouped rows the landed blocks add up to, ordered as the read-back orders them (id, name). */
    private static List<Map<String, String>> merge(List<List<Map<String, String>>> landedBlocks) {
        Map<String, Map<String, String>> byKey = new TreeMap<>();
        for (List<Map<String, String>> block : landedBlocks) {
            for (Map<String, String> row : block) {
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

    private static void assertGroupedAndLog(
            ClickHouseHelperClient chc, String table, String caseKey, int step, List<Map<String, String>> expected)
            throws Exception {
        List<Map<String, String>> actual = RowSnapshots.select(
                chc, String.format(GROUPED_READ_BACK_FORMAT, table), ID_COLUMN, NAME_COLUMN, COUNT_COLUMN);
        int totalRows =
                actual.stream().mapToInt(row -> Integer.parseInt(row.get(COUNT_COLUMN))).sum();
        LOGGER.info("{} outcome case={} step={} rows={}", LOG_PREFIX, caseKey, step, totalRows);
        assertEquals(expected, actual, "typed read-back of case " + caseKey + " after step " + step);
    }

    private static void assertCdcAndLog(
            ClickHouseHelperClient chc, String table, String caseKey, int step, List<Map<String, String>> expected)
            throws Exception {
        List<Map<String, String>> actual =
                allRows(chc, String.format(CDC_READ_BACK_FORMAT, table), CDC_READ_BACK_COLUMNS);
        LOGGER.info("{} outcome case={} step={} rows={}", LOG_PREFIX, caseKey, step, actual.size());
        assertEquals(expected, actual, "typed read-back of case " + caseKey + " after step " + step);
    }
}
