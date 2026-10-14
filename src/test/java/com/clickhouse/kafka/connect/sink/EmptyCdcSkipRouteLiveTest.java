package com.clickhouse.kafka.connect.sink;

import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.allEmptyEnvelopes;
import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.allRows;
import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.cdcSinkRecords;
import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.emptyFirstEnvelopes;
import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.mixedBatchEnvelopes;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * JSONP-09 live pins, one per route. Each route runs three batches through one task with strictly
 * increasing offsets (a mixed batch, a batch made only of empty records, and a batch that starts
 * with an empty record), reading the table back through a snapshot helper after each put.
 */
public class EmptyCdcSkipRouteLiveTest extends ClickHouseBase {

    private static final String DDL_PLAIN =
            "CREATE TABLE %s (id Int32, ts DateTime64(3, 'UTC'), amount Decimal(18,4), name String, "
                    + "_version UInt64, is_deleted UInt8) ENGINE = MergeTree ORDER BY id";

    private static final String DDL_DEFAULTS =
            "CREATE TABLE %s (id Int32 DEFAULT 0, ts DateTime64(3, 'UTC') DEFAULT 0, "
                    + "amount Decimal(18,4) DEFAULT 0, name String DEFAULT '', "
                    + "_version UInt64 DEFAULT 0, is_deleted UInt8 DEFAULT 0) ENGINE = MergeTree ORDER BY id";

    // The UTC time zone on ts keeps toString(ts) independent of the server's default zone.
    private static final String READ_BACK_WITH_TS =
            "SELECT id, toString(ts) AS ts, toString(amount) AS amount, name, "
                    + "toString(_version) AS v, toString(is_deleted) AS del FROM `%s` ORDER BY id";

    private static final String READ_BACK_WITHOUT_TS =
            "SELECT id, toString(amount) AS amount, name, "
                    + "toString(_version) AS v, toString(is_deleted) AS del FROM `%s` ORDER BY id";

    private static final String EXPECTED_TS = "2024-01-01 00:00:00.123";
    private static final String EXPECTED_AMOUNT = "12.3456";

    @Test
    @DisplayName("JSONP-09: RowBinary route with DEFAULT columns lands only the real rows of a Debezium batch and accepts an all-empty batch")
    void rowBinaryRouteWithDefaultColumnsSkipsEmptyCdcRecords(TestInfo testInfo) throws Exception {
        assertRouteSkipsEmptyCdcRecords(testInfo, "jsonp09_rowbinary_defaults", Map.of(), DDL_DEFAULTS, true);
    }

    @Test
    @DisplayName("JSONP-09: bypassRowBinary JSON route lands only the real rows of a Debezium batch and accepts an all-empty batch")
    void bypassJsonRouteSkipsEmptyCdcRecords(TestInfo testInfo) throws Exception {
        assertRouteSkipsEmptyCdcRecords(
                testInfo, "jsonp09_bypass", Map.of("bypassRowBinary", "true"), DDL_PLAIN, false);
    }

    @Test
    @DisplayName("JSONP-09: explicit insertFormat=json route lands only the real rows of a Debezium batch and accepts an all-empty batch")
    void explicitJsonRouteSkipsEmptyCdcRecords(TestInfo testInfo) throws Exception {
        assertRouteSkipsEmptyCdcRecords(
                testInfo, "jsonp09_explicit", Map.of(ClickHouseSinkConfig.INSERT_FORMAT, "json"), DDL_PLAIN, true);
    }

    @Test
    @DisplayName("JSONP-09: RowBinary route on a plain non-null table lands the real rows of a Debezium batch instead of failing the whole batch")
    void rowBinaryRouteOnPlainTableSkipsEmptyCdcRecords(TestInfo testInfo) throws Exception {
        assertRouteSkipsEmptyCdcRecords(testInfo, "jsonp09_rowbinary_plain", Map.of(), DDL_PLAIN, true);
    }

    private void assertRouteSkipsEmptyCdcRecords(
            TestInfo testInfo, String topicPrefix, Map<String, String> routeProps, String ddl, boolean assertTs)
            throws Exception {
        Map<String, String> props = new HashMap<>(getBaseProps());
        props.putAll(routeProps);
        props.put(ClickHouseSinkConfig.DEBEZIUM_CDC_ENABLED, "true");
        props.put("name", uniqueConnectorName(testInfo));
        String topic = createTopicName(topicPrefix);
        String readBack = String.format(assertTs ? READ_BACK_WITH_TS : READ_BACK_WITHOUT_TS, topic);
        String[] columns = assertTs
                ? new String[] {"id", "ts", "amount", "name", "v", "del"}
                : new String[] {"id", "amount", "name", "v", "del"};

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        ClickHouseTestHelpers.dropTable(chc, topic);
        ClickHouseTestHelpers.executeQueryIgnoreResult(chc, String.format(ddl, topic));

        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        try {
            sinkTask.start(props);

            // Offsets 1-4: op=t and op=u with a null after struct convert to an empty value map.
            List<SinkRecord> mixed = cdcSinkRecords(topic, 0, 1, mixedBatchEnvelopes());
            assertDoesNotThrow(() -> sinkTask.put(mixed));
            List<Map<String, String>> rows = allRows(chc, readBack, columns);
            assertEquals(2, rows.size(), "rows after the mixed batch: " + rows);
            assertRow(rows.get(0), assertTs, "1", "created", "100", "0");
            assertRow(rows.get(1), assertTs, "2", "deleted", "101", "1");

            // Offsets 5-7: nothing but empty records; the insert still opens, with an empty body.
            List<SinkRecord> allEmpty = cdcSinkRecords(topic, 0, 5, allEmptyEnvelopes());
            assertDoesNotThrow(() -> sinkTask.put(allEmpty));
            rows = allRows(chc, readBack, columns);
            assertEquals(2, rows.size(), "rows after the all-empty batch: " + rows);
            assertRow(rows.get(0), assertTs, "1", "created", "100", "0");
            assertRow(rows.get(1), assertTs, "2", "deleted", "101", "1");

            // Offsets 8-9: an empty first record must not hide the real row behind it.
            List<SinkRecord> emptyFirst = cdcSinkRecords(topic, 0, 8, emptyFirstEnvelopes());
            assertDoesNotThrow(() -> sinkTask.put(emptyFirst));
            rows = allRows(chc, readBack, columns);
            assertEquals(3, rows.size(), "rows after the empty-first batch: " + rows);
            assertRow(rows.get(0), assertTs, "1", "created", "100", "0");
            assertRow(rows.get(1), assertTs, "2", "deleted", "101", "1");
            assertRow(rows.get(2), assertTs, "3", "created-3", "141", "0");
        } finally {
            sinkTask.stop();
            ClickHouseTestHelpers.dropTable(chc, topic);
        }
    }

    private static void assertRow(
            Map<String, String> row, boolean assertTs, String id, String name, String version, String deleted) {
        assertEquals(id, row.get("id"));
        if (assertTs) {
            assertEquals(EXPECTED_TS, row.get("ts"));
        }
        assertEquals(EXPECTED_AMOUNT, row.get("amount"));
        assertEquals(name, row.get("name"));
        assertEquals(version, row.get("v"));
        assertEquals(deleted, row.get("del"));
    }
}
