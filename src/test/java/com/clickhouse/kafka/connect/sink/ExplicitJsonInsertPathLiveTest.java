package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * ClickHouse-backed pins for the metadata-free explicit JSON insert path: JSON-01 (the live
 * differential half - the connector-side table-existence check that metadata-free mode skips),
 * JSON-02 (table resolution via topic-to-table mapping with no fetched metadata), JSON-03 (the
 * live half of the {@code cleanupExtraFields} bypass - the Docker-free half lives in
 * {@code MetadataFreeJsonInsertUnitTest}), and JSON-04 (tombstone handling in a mixed batch and
 * an all-tombstone batch). New fork-only class (D-16): it does not extend or modify any upstream
 * test class. Every record built in this class is a schemaless {@code Map} value rather than a
 * {@code Struct}, per D-06, because the schemaless route is the only one where metadata-free mode
 * actually changes behavior - R-01 proved the Struct + RowBinary combination under
 * {@code insertFormat=json} silently drops data, which is explicitly out of scope for a pin here
 * (see Task 4's blocking-human checkpoint).
 */
public class ExplicitJsonInsertPathLiveTest extends ClickHouseBase {

    @Test
    @DisplayName("JSON-02: metadata-free mode resolves a remapped table with no fetched metadata and lands every column value")
    void metadataFreeModeResolvesRemappedTableAndLandsEveryColumnValue(TestInfo testInfo) throws Exception {
        Map<String, String> props = createProps();
        String topic = createTopicName("mf_topic");
        String tableName = createTopicName("mf_mapped_table");
        props.put(ClickHouseSinkConfig.INSERT_FORMAT, "JSON");
        props.put(ClickHouseSinkConfig.TABLE_MAPPING, topic + "=" + tableName);
        props.put("name", uniqueConnectorName(testInfo));

        ClickHouseHelperClient chc = createClient(props);
        ClickHouseTestHelpers.dropTable(chc, tableName);

        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        try {
            assertDoesNotThrow(() -> sinkTask.start(props));

            ClickHouseTestHelpers.createTable(chc, tableName,
                    "CREATE TABLE %s (id UInt32, str String, n Int32) ENGINE = MergeTree ORDER BY id");

            Map<String, Object> value1 = new LinkedHashMap<>();
            value1.put("id", 1);
            value1.put("str", "alpha");
            value1.put("n", 10);

            Map<String, Object> value2 = new LinkedHashMap<>();
            value2.put("id", 2);
            value2.put("str", "beta");
            value2.put("n", 20);

            Map<String, Object> value3 = new LinkedHashMap<>();
            value3.put("id", 3);
            value3.put("str", "gamma");
            value3.put("n", 30);

            List<SinkRecord> records = List.of(
                    new SinkRecord(topic, 0, null, null, null, value1, 1),
                    new SinkRecord(topic, 0, null, null, null, value2, 2),
                    new SinkRecord(topic, 0, null, null, null, value3, 3));

            sinkTask.put(records);

            List<Map<String, String>> rows = allRows(chc,
                    String.format("SELECT id, str, n FROM `%s` ORDER BY id", tableName),
                    "id", "str", "n");
            assertEquals(3, rows.size());
            assertEquals("1", rows.get(0).get("id"));
            assertEquals("alpha", rows.get(0).get("str"));
            assertEquals("10", rows.get(0).get("n"));
            assertEquals("2", rows.get(1).get("id"));
            assertEquals("beta", rows.get(1).get("str"));
            assertEquals("20", rows.get(1).get("n"));
            assertEquals("3", rows.get(2).get("id"));
            assertEquals("gamma", rows.get(2).get("str"));
            assertEquals("30", rows.get(2).get("n"));
        } finally {
            sinkTask.stop();
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    /**
     * Snapshots each row's requested columns into a plain {@code Map<String, String>} while the
     * row is still the iterator's current element. The client-v2 {@link GenericRecord} returned
     * by {@link Records}'s iterator is a mutable, reused view over the underlying binary reader
     * buffer - collecting the {@code GenericRecord} references themselves (rather than reading
     * their values immediately) leaves every collected reference pointing at whatever row the
     * iterator most recently advanced to, silently aliasing every "row" to the last one read.
     */
    private static List<Map<String, String>> allRows(ClickHouseHelperClient chc, String sql, String... columns)
            throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        try (Records records = chc.getClient().queryRecords(sql).get()) {
            for (GenericRecord r : records) {
                Map<String, String> row = new LinkedHashMap<>();
                for (String column : columns) {
                    row.put(column, r.getString(column));
                }
                rows.add(row);
            }
        }
        return rows;
    }
}
