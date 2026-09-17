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
 * ClickHouse-backed round-trip for TYPE-03: {@code JSON} and {@code Dynamic} columns through the
 * schemaless {@code JSONEachRow} insert route, with typed SQL read-back (D-14) rather than a row
 * count. New fork-only class (D-16): it does not extend or modify any upstream test class.
 */
public class JsonDynamicRoundTripTest extends ClickHouseBase {

    @Test
    @DisplayName("TYPE-03: JSON and Dynamic columns round-trip through the schemaless JSONEachRow route with typed read-back")
    void jsonAndDynamicRoundTripThroughSchemalessRoute(TestInfo testInfo) throws Exception {
        String tableName = createTopicName("json_dynamic_schemaless");
        Map<String, String> props = createProps();
        ClickHouseHelperClient chc = createClient(props);
        ClickHouseTestHelpers.createTable(chc, tableName,
                "CREATE TABLE %s (id UInt32, json_col JSON, dynamic_col Dynamic) ENGINE = MergeTree ORDER BY id");
        props.put("name", uniqueConnectorName(testInfo));

        try {
            Map<String, Object> jsonLeaf1 = new LinkedHashMap<>();
            jsonLeaf1.put("b", 1);
            Map<String, Object> json1 = new LinkedHashMap<>();
            json1.put("a", jsonLeaf1);
            Map<String, Object> value1 = new LinkedHashMap<>();
            value1.put("id", 1);
            value1.put("json_col", json1);
            value1.put("dynamic_col", 42);

            Map<String, Object> jsonLeaf2 = new LinkedHashMap<>();
            jsonLeaf2.put("b", 2);
            Map<String, Object> json2 = new LinkedHashMap<>();
            json2.put("a", jsonLeaf2);
            Map<String, Object> value2 = new LinkedHashMap<>();
            value2.put("id", 2);
            value2.put("json_col", json2);
            value2.put("dynamic_col", "text_value");

            List<SinkRecord> records = List.of(
                    new SinkRecord(tableName, 0, null, null, null, value1, 1),
                    new SinkRecord(tableName, 0, null, null, null, value2, 2));

            ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
            sinkTask.start(props);
            sinkTask.put(records);
            sinkTask.stop();

            assertEquals(2, ClickHouseTestHelpers.countRows(chc, tableName));

            List<Map<String, String>> leafRows = allRows(chc, String.format(
                    "SELECT id, json_col.a.b AS leaf, toTypeName(json_col.a.b) AS leaf_type FROM `%s` ORDER BY id",
                    tableName), "id", "leaf", "leaf_type");
            assertEquals(2, leafRows.size());
            assertEquals("1", leafRows.get(0).get("leaf"));
            // A plain JSON leaf reports ClickHouse's own internal Dynamic representation - expected,
            // not a bug (Assumption A3, 03-RESEARCH.md).
            assertEquals("Dynamic", leafRows.get(0).get("leaf_type"));
            assertEquals("2", leafRows.get(1).get("leaf"));
            assertEquals("Dynamic", leafRows.get(1).get("leaf_type"));

            List<Map<String, String>> dynRows = allRows(chc, String.format(
                    "SELECT id, dynamicType(dynamic_col) AS dyn_type, dynamic_col AS dyn_val FROM `%s` ORDER BY id",
                    tableName), "id", "dyn_type", "dyn_val");
            assertEquals(2, dynRows.size());
            assertEquals("Int64", dynRows.get(0).get("dyn_type"));
            assertEquals("42", dynRows.get(0).get("dyn_val"));
            assertEquals("String", dynRows.get(1).get("dyn_type"));
            assertEquals("text_value", dynRows.get(1).get("dyn_val"));
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
