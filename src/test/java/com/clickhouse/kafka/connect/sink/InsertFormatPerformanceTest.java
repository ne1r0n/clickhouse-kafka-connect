package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.*;

import com.clickhouse.kafka.connect.ClickHouseSinkConnector;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.transforms.ConvertToMapOrJson;
import java.util.*;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class InsertFormatPerformanceTest extends ClickHouseBase {
    private static final int RECORDS = 100_000;
    private static final int ITERATIONS = 5;

    private List<SinkRecord> createTestRecords(String topic) {
        Schema schema = SchemaBuilder.struct()
            .field("id", Schema.INT32_SCHEMA)
            .field("timestamp", Schema.INT64_SCHEMA)
            .field("name", Schema.STRING_SCHEMA)
            .field("value", Schema.FLOAT64_SCHEMA)
            .field("active", Schema.BOOLEAN_SCHEMA)
            .build();

        List<SinkRecord> records = new ArrayList<>(RECORDS);
        Random random = new Random(42);

        for (int i = 0; i < RECORDS; i++) {
            Struct struct = new Struct(schema)
                .put("id", i)
                .put("timestamp", System.currentTimeMillis())
                .put("name", "test_" + i)
                .put("value", random.nextDouble() * 1000)
                .put("active", random.nextBoolean());

            records.add(new SinkRecord(topic, 0, null, null, schema, struct, i));
        }
        return records;
    }

    @Disabled("Disabled because it not suitable for unit tests")
    @Test
    public void compareInsertPerformance() {
        String topic = createTopicName("insert_format_perf_test");
        Map<String, String> props = createProps();

        props.put(ClickHouseSinkConnector.CLIENT_VERSION, "V2");

        // Prepare JSON transformer
        ConvertToMapOrJson<SinkRecord> jsonTransform = new ConvertToMapOrJson<>();
        Map<String, Object> jsonConfig = Map.of(
            "output.json.enabled", true,
            "output.json.library", "jackson"
        );
        jsonTransform.configure(jsonConfig);

        // Prepare Map transformer
        ConvertToMapOrJson<SinkRecord> mapTransform = new ConvertToMapOrJson<>();
        Map<String, Object> mapConfig = Map.of(
            "output.json.enabled", false
        );
        mapTransform.configure(mapConfig);

        List<PerformanceResult> results = new ArrayList<>();

        // Run performance tests
        for (int i = 0; i < ITERATIONS; i++) {
            List<SinkRecord> records = createTestRecords(topic);

            // Test JSON insert performance
            testInsertFormat(topic, records, props, jsonTransform, "JSON format", results);

            // Test Map insert performance
            testInsertFormat(topic, records, props, mapTransform, "Map format", results);
        }

        // Output results
        printResults(results);
    }

    private void testInsertFormat(String topic, List<SinkRecord> sourceRecords, 
            Map<String, String> props, ConvertToMapOrJson<SinkRecord> transform, 
            String label, List<PerformanceResult> results) {
        
        ClickHouseHelperClient chc = createClient(props);
        ClickHouseTestHelpers.dropTable(chc, topic);
        ClickHouseTestHelpers.createTable(chc, topic,
            "CREATE TABLE %s (" +
            "id Int32, " +
            "timestamp Int64, " +
            "name String, " +
            "value Float64, " +
            "active Bool" +
            ") ENGINE = MergeTree ORDER BY id");

        List<SinkRecord> transformedRecords = sourceRecords.stream()
            .map(transform::apply)
            .toList();

        long startTime = System.nanoTime();

        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        sinkTask.start(props);
        sinkTask.put(transformedRecords);
        sinkTask.stop();

        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;

        assertEquals(sourceRecords.size(), ClickHouseTestHelpers.countRows(chc, topic));

        results.add(new PerformanceResult(
            label,
            durationMs,
            RECORDS / (durationMs / 1000.0)
        ));
    }

    private void printResults(List<PerformanceResult> results) {
        Map<String, List<Double>> timesByFormat = new HashMap<>();
        
        // Group results by format
        for (PerformanceResult result : results) {
            timesByFormat.computeIfAbsent(result.label, k -> new ArrayList<>())
                .add(result.timeMs);
        }

        System.out.println("\nPerformance Comparison Results:"); 
        System.out.println("==============================");
        
        // Calculate and output statistics for each format
        timesByFormat.forEach((format, times) -> {
            DoubleSummaryStatistics stats = times.stream().mapToDouble(t -> t).summaryStatistics();
            System.out.printf("%s:%n", format);
            System.out.printf("  Avg time: %.2f ms%n", stats.getAverage());
            System.out.printf("  Min time: %.2f ms%n", stats.getMin());
            System.out.printf("  Max time: %.2f ms%n", stats.getMax());
            System.out.printf("  Avg throughput: %.2f records/sec%n", 
                RECORDS / (stats.getAverage() / 1000.0));
            System.out.println();
        });
    }

    private record PerformanceResult(String label, double timeMs, double recordsPerSec) {}
}
