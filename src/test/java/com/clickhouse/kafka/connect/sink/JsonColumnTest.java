package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.*;

import com.clickhouse.kafka.connect.ClickHouseSinkConnector;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.transforms.ConvertToMapOrJson;
import java.util.*;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class JsonColumnTest extends ClickHouseBase {
    private static final int RECORDS = 10000;
    private static final int BYTE_ARRAY_SIZE = 128;
    private byte[] sampleBytes;
    private String tableName;

    @BeforeEach
    void setUp() {
        sampleBytes = new byte[BYTE_ARRAY_SIZE];
        new Random(42).nextBytes(sampleBytes);

        tableName = createTopicName("json_test");
    }

    private List<SinkRecord> createTestRecords(String topic) {
        List<SinkRecord> records = new ArrayList<>();
        for (int i = 0; i < RECORDS; i++) {
            // First create inner structure that will be stored as json_data
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("nested_str", "test" + i);
            jsonData.put("nested_int", i);
            jsonData.put("nested_bytes", sampleBytes);

            // Create outer structure matching table schema
            Map<String, Object> value = new HashMap<>();
            value.put("id", i);
            value.put("json_data", jsonData);

            // Create record with the complete structure
            records.add(new SinkRecord(topic, 0, null, null, null, value, i));
        }
        return records;
    }

    private void runJsonTest(Map<String, String> props, Map<String, Object> transformConfig, String testLabel) {
        props.put(ClickHouseSinkConnector.CLIENT_VERSION, "V2");
        ClickHouseHelperClient chc = createClient(props);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        ClickHouseTestHelpers.createTable(chc, tableName,
            "CREATE TABLE %s (id UInt32, json_data JSON) ENGINE = MergeTree ORDER BY id");

        List<SinkRecord> sourceRecords = createTestRecords(tableName);
        
        try (ConvertToMapOrJson<SinkRecord> transform = new ConvertToMapOrJson<>()) {
            transform.configure(transformConfig);

            // Apply transformation to the full record value
            List<SinkRecord> transformedRecords = sourceRecords.stream()
                .map(transform::apply)
                .toList();

            long startTime = System.nanoTime();

            // Insert records
            ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
            sinkTask.start(props);
            sinkTask.put(transformedRecords);
            sinkTask.stop();

            // Verify results and data points
            assertEquals(RECORDS, ClickHouseTestHelpers.countRows(chc, tableName));

            // // Verify some data points
            // List<Map<String, Object>> rows = ClickHouseTestHelpers.getAllRowsAsMap(chc, tableName);
            // for (int i = 0; i < Math.min(10, rows.size()); i++) {
            //     Map<String, Object> row = rows.get(i);
            //     assertEquals(i, ((Number) row.get("id")).intValue());
                
            //     Object jsonData = row.get("json_data");
            //     if (transformConfig.getOrDefault("output.json.enabled", false).equals(true)) {
            //         assertTrue(jsonData instanceof String);
            //         String json = (String) jsonData;
            //         assertTrue(json.contains("\"nested_str\":\"test" + i + "\""));
            //         assertTrue(json.contains("\"nested_int\":" + i));
            //     } else {
            //         assertTrue(jsonData instanceof Map);
            //         Map<?, ?> jsonMap = (Map<?, ?>) jsonData;
            //         assertEquals("test" + i, jsonMap.get("nested_str"));
            //         assertEquals(i, ((Number) jsonMap.get("nested_int")).intValue());
            //     }
            // }
        }
    }

    @Test
    public void testBytesNoBase64NoJson() {
        Map<String, String> props = createProps();
        Map<String, Object> transformConfig = Map.of(
            "bytes.to.base64", false,
            "output.json.enabled", false
        );
        runJsonTest(props, transformConfig, "No Base64, No JSON");
    }

    @Test
    public void testBytesBase64NoJson() {
        Map<String, String> props = createProps();
        Map<String, Object> transformConfig = Map.of(
            "bytes.to.base64", true,
            "output.json.enabled", false
        );
        runJsonTest(props, transformConfig, "Base64, No JSON");
    }

    @Test
    public void testBytesNoBase64JsonWithJackson() {
        Map<String, String> props = createProps();
        Map<String, Object> transformConfig = Map.of(
            "bytes.to.base64", false,
            "output.json.enabled", true,
            "output.json.library", "jackson"
        );
        runJsonTest(props, transformConfig, "No Base64, JSON (Jackson)");
    }

    @Test
    public void testBytesNoBase64JsonWithGson() {
        Map<String, String> props = createProps();
        Map<String, Object> transformConfig = Map.of(
            "bytes.to.base64", false,
            "output.json.enabled", true,
            "output.json.library", "gson"
        );
        runJsonTest(props, transformConfig, "No Base64, JSON (Gson)");
    }

    @Test
    public void testBytesBase64JsonWithJackson() {
        Map<String, String> props = createProps();
        Map<String, Object> transformConfig = Map.of(
            "bytes.to.base64", true,
            "output.json.enabled", true,
            "output.json.library", "jackson"
        );
        runJsonTest(props, transformConfig, "Base64, JSON (Jackson)");
    }

    @Test
    public void testBytesBase64JsonWithGson() {
        Map<String, String> props = createProps();
        Map<String, Object> transformConfig = Map.of(
            "bytes.to.base64", true,
            "output.json.enabled", true,
            "output.json.library", "gson"
        );
        runJsonTest(props, transformConfig, "Base64, JSON (Gson)");
    }

    @Disabled("Disabled because it not suitable for unit tests")
    @Test
    public void compareAllConfigurations() {
        List<Map<String, Object>> configs = List.of(
            Map.of("bytes.to.base64", false, "output.json.enabled", false),
            Map.of("bytes.to.base64", true, "output.json.enabled", false),
            Map.of("bytes.to.base64", false, "output.json.enabled", true, "output.json.library", "jackson"),
            Map.of("bytes.to.base64", false, "output.json.enabled", true, "output.json.library", "gson"),
            Map.of("bytes.to.base64", true, "output.json.enabled", true, "output.json.library", "jackson"),
            Map.of("bytes.to.base64", true, "output.json.enabled", true, "output.json.library", "gson")
        );

        List<PerformanceResult> results = new ArrayList<>();

        for (Map<String, Object> config : configs) {
            Map<String, String> props = createProps();

            String label = String.format("base64=%s, json=%s%s",
                config.get("bytes.to.base64"),
                config.get("output.json.enabled"),
                config.get("output.json.enabled").equals(true)
                    ? ", lib=" + config.get("output.json.library")
                    : "");

            long startTime = System.nanoTime();
            runJsonTest(props, config, label);
            long endTime = System.nanoTime();

            results.add(new PerformanceResult(
                label,
                (endTime - startTime) / 1_000_000.0,
                RECORDS / ((endTime - startTime) / 1_000_000_000.0)
            ));
        }

        System.out.println("\nPerformance Comparison:");
        System.out.println("=======================");
        results.sort((a, b) -> Double.compare(a.timeMs, b.timeMs));

        PerformanceResult baseline = results.get(0);
        for (PerformanceResult result : results) {
            System.out.printf("%s:%n", result.label);
            System.out.printf("  Time: %.2f ms (%.2fx baseline)%n",
                result.timeMs, result.timeMs / baseline.timeMs);
            System.out.printf("  Throughput: %.2f records/sec%n",
                result.recordsPerSec);
            System.out.println();
        }
    }

    private record PerformanceResult(String label, double timeMs, double recordsPerSec) {}
}
