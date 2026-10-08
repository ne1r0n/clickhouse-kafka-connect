package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Re-enabled per the JSONP-01 KEEP decision (Phase 9, D-01/D-10): the metadata-free {@code
 * insertFormat=json} bypass these tests exercise was re-applied to {@code src/main} in Phase 9;
 * every assertion below is unchanged from the parked version.
 *
 * <p>Both tests use the try/finally lifecycle, so a failed put still unregisters the MBean and
 * drops the table (DEBT-03).
 */
public class ClickHouseSinkTaskJsonInsertFormatTest extends ClickHouseBase {
    private static final String TABLE_SCHEMA = "CREATE TABLE %s ( `off16` Int16, `str` String ) Engine = MergeTree ORDER BY off16";
    private static final int RECORD_COUNT = 10;

    private Map<String, String> createJsonInsertProps() {
        Map<String, String> props = getBaseProps();
        props.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        return props;
    }

    private Collection<SinkRecord> createJsonRecords(String topic, int partition, int totalRecords) {
        Gson gson = new Gson();
        java.lang.reflect.Type gsonType = new TypeToken<HashMap>() {}.getType();
        List<SinkRecord> records = new ArrayList<>();
        LongStream.range(0, totalRecords).forEachOrdered(n -> {
            Map<String, Object> value = new HashMap<>();
            value.put("off16", (short) n);
            value.put("str", "num" + n);
            String json = gson.toJson(value, gsonType);
            SinkRecord record = new SinkRecord(
                    topic,
                    partition,
                    null,
                    null,
                    null,
                    json,
                    n,
                    System.currentTimeMillis(),
                    TimestampType.CREATE_TIME
            );
            records.add(record);
        });
        return records;
    }

    private void createTable(ClickHouseHelperClient chc, String tableName) {
        try (Records ignored = chc.queryV2(String.format(TABLE_SCHEMA, tableName))) {
            // table created
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("JSON-05: explicitJsonInsertSkipsTableCheckOnStart asserts typed column values, not just a row count")
    public void explicitJsonInsertSkipsTableCheckOnStart() throws Exception {
        Map<String, String> props = createJsonInsertProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);

        String topic = createTopicName("json_insert_skip_mapping_test");
        String tableName = topic;
        ClickHouseTestHelpers.dropTable(chc, tableName);

        ClickHouseSinkTask chst = new ClickHouseSinkTask();
        try {
            assertDoesNotThrow(() -> chst.start(props));

            createTable(chc, tableName);
            Collection<SinkRecord> records = createJsonRecords(topic, 0, RECORD_COUNT);

            chst.put(records);

            assertEquals(records.size(), ClickHouseTestHelpers.countRows(chc, tableName));

            List<Map<String, String>> rows = allRows(chc,
                    String.format("SELECT off16, str FROM `%s` ORDER BY off16", tableName), "off16", "str");
            assertEquals(RECORD_COUNT, rows.size());
            for (int i = 0; i < RECORD_COUNT; i++) {
                assertEquals(String.valueOf(i), rows.get(i).get("off16"));
                assertEquals("num" + i, rows.get(i).get("str"));
            }
        } finally {
            chst.stop();
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @Test
    @DisplayName("JSON-05: explicitJsonInsertIgnoresTargetTableFilter asserts typed column values, not just a row count")
    public void explicitJsonInsertIgnoresTargetTableFilter() throws Exception {
        Map<String, String> props = createJsonInsertProps();
        props.put(ClickHouseSinkConfig.TARGET_TABLE_FILTER, "^filtered_.*");
        props.put(ClickHouseSinkConfig.TABLE_MAPPING, "json_topic=unfiltered_table");
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);

        String topic = "json_topic";
        String tableName = "unfiltered_table";
        ClickHouseTestHelpers.dropTable(chc, tableName);

        ClickHouseSinkTask chst = new ClickHouseSinkTask();
        try {
            createTable(chc, tableName);

            Collection<SinkRecord> records = createJsonRecords(topic, 0, RECORD_COUNT);

            chst.start(props);
            chst.put(records);

            assertEquals(records.size(), ClickHouseTestHelpers.countRows(chc, tableName));

            List<Map<String, String>> rows = allRows(chc,
                    String.format("SELECT off16, str FROM `%s` ORDER BY off16", tableName), "off16", "str");
            assertEquals(RECORD_COUNT, rows.size());
            for (int i = 0; i < RECORD_COUNT; i++) {
                assertEquals(String.valueOf(i), rows.get(i).get("off16"));
                assertEquals("num" + i, rows.get(i).get("str"));
            }
        } finally {
            chst.stop();
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
