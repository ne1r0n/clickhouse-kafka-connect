package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;

public class ClickHouseSinkTaskJsonInsertFormatTest extends ClickHouseBase {
    private static final String TABLE_SCHEMA = "CREATE TABLE %s ( `off16` Int16, `str` String ) Engine = MergeTree ORDER BY off16";
    private static final int RECORD_COUNT = 10;

    private Map<String, String> createJsonInsertProps() {
        Map<String, String> props = createProps();
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
        ClickHouseTestHelpers.createTable(chc, tableName, TABLE_SCHEMA);
    }

    @Test
    public void explicitJsonInsertSkipsTableCheckOnStart() {
        Map<String, String> props = createJsonInsertProps();
        ClickHouseHelperClient chc = createClient(props);

        String topic = createTopicName("json_insert_skip_mapping_test");
        String tableName = topic;
        ClickHouseTestHelpers.dropTable(chc, tableName);

        ClickHouseSinkTask chst = new ClickHouseSinkTask();
        assertDoesNotThrow(() -> chst.start(props));

        createTable(chc, tableName);
        Collection<SinkRecord> records = createJsonRecords(topic, 0, RECORD_COUNT);

        chst.put(records);
        chst.stop();

        assertEquals(records.size(), ClickHouseTestHelpers.countRows(chc, tableName));
    }

    @Test
    public void explicitJsonInsertIgnoresTargetTableFilter() {
        Map<String, String> props = createJsonInsertProps();
        props.put(ClickHouseSinkConfig.TARGET_TABLE_FILTER, "^filtered_.*");
        props.put(ClickHouseSinkConfig.TABLE_MAPPING, "json_topic=unfiltered_table");
        ClickHouseHelperClient chc = createClient(props);

        String topic = "json_topic";
        String tableName = "unfiltered_table";
        ClickHouseTestHelpers.dropTable(chc, tableName);
        createTable(chc, tableName);

        Collection<SinkRecord> records = createJsonRecords(topic, 0, RECORD_COUNT);

        ClickHouseSinkTask chst = new ClickHouseSinkTask();
        chst.start(props);
        chst.put(records);
        chst.stop();

        assertEquals(records.size(), ClickHouseTestHelpers.countRows(chc, tableName));
    }
}
