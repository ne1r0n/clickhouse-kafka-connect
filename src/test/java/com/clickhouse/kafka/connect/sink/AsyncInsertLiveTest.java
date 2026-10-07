package com.clickhouse.kafka.connect.sink;

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
 * ClickHouse-backed pin for ASYNC-03: a sink insert configured with {@code async_insert=1} and
 * {@code wait_for_async_insert=1} completes and its data is present immediately after {@code
 * put()}. With {@code async_insert=1} alone the server acknowledges before the data is
 * queryable, so a read immediately after {@code put()} can legitimately see zero rows (D-15).
 * {@code wait_for_async_insert=1} makes the insert return only once the buffer is flushed, which
 * keeps this assertion deterministic with no polling loop. New fork-only class (D-16): it
 * extends {@code ClickHouseBase} because it needs a live ClickHouse insert.
 */
public class AsyncInsertLiveTest extends ClickHouseBase {

    private static void createTable(ClickHouseHelperClient chc, String tableName, String createTableQuery) {
        try (Records ignored = chc.queryV2(String.format(createTableQuery, tableName))) {
            // table created
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("ASYNC-03: a sink insert with async_insert=1 and wait_for_async_insert=1 completes and its data is present")
    void asyncInsertWithWaitForAsyncInsertCompletesAndDataIsPresent(TestInfo testInfo) throws Exception {
        String tableName = createTopicName("async03_insert");
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        createTable(chc, tableName, "CREATE TABLE %s (id UInt32, str String) ENGINE = MergeTree ORDER BY id");
        props.put(ClickHouseSinkConfig.CLICKHOUSE_SETTINGS, "async_insert=1,wait_for_async_insert=1");
        props.put("name", uniqueConnectorName(testInfo));

        try {
            List<SinkRecord> records = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("id", i);
                value.put("str", "async" + i);
                records.add(new SinkRecord(tableName, 0, null, null, null, value, i));
            }

            ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
            sinkTask.start(props);
            try {
                sinkTask.put(records);
            } finally {
                sinkTask.stop();
            }

            // No sleep, no retry loop - wait_for_async_insert=1 is exactly what removes the race a
            // bare async_insert=1 would introduce (D-15).
            assertEquals(5, ClickHouseTestHelpers.countRows(chc, tableName));

            List<Map<String, String>> rows = allRows(chc,
                    String.format("SELECT id, str FROM `%s` ORDER BY id", tableName), "id", "str");
            assertEquals(5, rows.size());
            for (int i = 0; i < 5; i++) {
                int id = i + 1;
                assertEquals(String.valueOf(id), rows.get(i).get("id"));
                assertEquals("async" + id, rows.get(i).get("str"));
            }
        } finally {
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
