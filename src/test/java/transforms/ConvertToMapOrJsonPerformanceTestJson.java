package transforms;

import static org.junit.jupiter.api.Assertions.*;

import com.clickhouse.kafka.connect.transforms.ConvertToMapOrJson;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConvertToMapOrJsonPerformanceTestJson {
    private static final int DEEP_NESTING_LEVEL = 1000;
    private static final int WIDE_FIELDS_COUNT = 50000;
    private static final int BYTE_ARRAY_SIZE = 16384;
    private static final int ITERATIONS = 100;

    private Random random;
    private byte[] sampleBytes;

    @BeforeEach
    void setUp() {
        random = new Random(42);
        sampleBytes = new byte[BYTE_ARRAY_SIZE];
        random.nextBytes(sampleBytes);
    }

    private record PerformanceResult(double avgTime, double minTime, double maxTime, long usedMemory) {}

    private PerformanceResult measurePerformance(String label, Function<SinkRecord, SinkRecord> transform, SinkRecord record) {
        long totalTime = 0;
        long maxTime = Long.MIN_VALUE;
        long minTime = Long.MAX_VALUE;
        Runtime rt = Runtime.getRuntime();

        // Warm-up
        for (int i = 0; i < 10; i++) {
            transform.apply(record);
        }

        // Measure memory before
        System.gc();
        long memBefore = rt.totalMemory() - rt.freeMemory();

        // Main test
        for (int i = 0; i < ITERATIONS; i++) {
            long startTime = System.nanoTime();
            SinkRecord result = transform.apply(record);
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            totalTime += duration;
            maxTime = Math.max(maxTime, duration);
            minTime = Math.min(minTime, duration);
            assertTrue(result.value() instanceof String);
        }

        System.gc();
        long memAfter = rt.totalMemory() - rt.freeMemory();

        double avgTime = totalTime / (double) ITERATIONS / 1_000_000.0;
        double minTimeMs = minTime / 1_000_000.0;
        double maxTimeMs = maxTime / 1_000_000.0;
        long usedMemory = Math.max(memAfter - memBefore, 0);

        System.out.printf("%s performance:%n", label);
        System.out.printf("Avg time: %.2f ms, Min: %.2f ms, Max: %.2f ms, Used memory: %d bytes%n", avgTime, minTimeMs, maxTimeMs, usedMemory);

        return new PerformanceResult(avgTime, minTimeMs, maxTimeMs, usedMemory);
    }

    @Test
    public void testDeepNestedStructPerformance() {
        Schema currentSchema = SchemaBuilder.struct()
                .field("value", Schema.STRING_SCHEMA)
                .field("bytes", Schema.BYTES_SCHEMA)
                .build();
        Struct currentStruct = new Struct(currentSchema)
                .put("value", "leaf")
                .put("bytes", sampleBytes);
        for (int i = 0; i < DEEP_NESTING_LEVEL; i++) {
            Schema newSchema = SchemaBuilder.struct()
                    .field("nested", currentSchema)
                    .field("value", Schema.STRING_SCHEMA)
                    .field("bytes", Schema.BYTES_SCHEMA)
                    .build();
            currentStruct = new Struct(newSchema)
                    .put("nested", currentStruct)
                    .put("value", "level_" + i)
                    .put("bytes", sampleBytes);
            currentSchema = newSchema;
        }
        SinkRecord record = new SinkRecord("topic", 0, null, null, currentSchema, currentStruct, 0L);

        for (String lib : new String[]{"jackson", "gson"}) {
            Map<String, Object> config = new HashMap<>();
            config.put("bytes.to.base64", true);
            config.put("output.json.enabled", true);
            config.put("output.json.library", lib);
            try (ConvertToMapOrJson<SinkRecord> transform = new ConvertToMapOrJson<>()) {
                transform.configure(config);
                measurePerformance("Deep nesting, JSON with " + lib, transform::apply, record);
            }
        }
    }

    @Test
    public void testWideStructPerformance() {
        SchemaBuilder builder = SchemaBuilder.struct();
        for (int i = 0; i < WIDE_FIELDS_COUNT; i++) {
            builder.field("field_" + i, Schema.STRING_SCHEMA);
            if (i % 10 == 0) {
                builder.field("bytes_" + i, Schema.BYTES_SCHEMA);
            }
        }
        Schema schema = builder.build();
        Struct struct = new Struct(schema);
        for (int i = 0; i < WIDE_FIELDS_COUNT; i++) {
            struct.put("field_" + i, "value_" + i);
            if (i % 10 == 0) {
                struct.put("bytes_" + i, sampleBytes);
            }
        }
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);

        for (String lib : new String[]{"jackson", "gson"}) {
            Map<String, Object> config = new HashMap<>();
            config.put("bytes.to.base64", true);
            config.put("output.json.enabled", true);
            config.put("output.json.library", lib);
            try (ConvertToMapOrJson<SinkRecord> transform = new ConvertToMapOrJson<>()) {
                transform.configure(config);
                measurePerformance("Wide struct, JSON with " + lib, transform::apply, record);
            }
        }
    }
}
