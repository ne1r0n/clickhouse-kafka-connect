package transforms;

import static org.junit.jupiter.api.Assertions.*;

import com.clickhouse.kafka.connect.transforms.RecursiveStructToMap;
import com.clickhouse.kafka.connect.transforms.StructToMap;
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

public class StructToMapPerformanceTest {
    private static final int DEEP_NESTING_LEVEL = 1500; // Test with deeper nesting
    private static final int WIDE_FIELDS_COUNT = 50000; // Test with more fields
    private static final int BYTE_ARRAY_SIZE = 16384; // 16KB byte arrays
    private static final int ITERATIONS = 100;
    
    private Random random;
    private byte[] sampleBytes;

    @BeforeEach
    void setUp() {
        random = new Random(42); // Fixed seed for reproducibility
        sampleBytes = new byte[BYTE_ARRAY_SIZE];
        random.nextBytes(sampleBytes);
    }

    private record PerformanceResult(double avgTime, double minTime, double maxTime) {}

    private PerformanceResult measurePerformance(String label, Function<SinkRecord, SinkRecord> transform, SinkRecord record) {
        long totalTime = 0;
        long maxTime = Long.MIN_VALUE;
        long minTime = Long.MAX_VALUE;

        // Прогрев JVM
        for (int i = 0; i < 10; i++) {
            transform.apply(record);
        }

        // Основной тест
        for (int i = 0; i < ITERATIONS; i++) {
            long startTime = System.nanoTime();
            SinkRecord result = transform.apply(record);
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            
            totalTime += duration;
            maxTime = Math.max(maxTime, duration);
            minTime = Math.min(minTime, duration);
            
            // Проверяем корректность преобразования
            assertTrue(result.value() instanceof Map);
        }

        double avgTime = totalTime / (double) ITERATIONS / 1_000_000.0; // в миллисекундах
        double minTimeMs = minTime / 1_000_000.0;
        double maxTimeMs = maxTime / 1_000_000.0;

        System.out.printf("%s performance:%n", label);
        System.out.printf("Avg time: %.2f ms%n", avgTime);
        System.out.printf("Min time: %.2f ms%n", minTimeMs);
        System.out.printf("Max time: %.2f ms%n", maxTimeMs);
        System.out.println();

        return new PerformanceResult(avgTime, minTimeMs, maxTimeMs);
    }

    @Test
    public void testDeepNestedStructPerformance() {
        // Создаем глубоко вложенную структуру
        Schema currentSchema = SchemaBuilder.struct()
                .field("value", Schema.STRING_SCHEMA)
                .field("bytes", Schema.BYTES_SCHEMA)
                .build();
        
        Struct currentStruct = new Struct(currentSchema)
                .put("value", "leaf")
                .put("bytes", sampleBytes);

        // Создаем deep nesting
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
        Map<String, Object> config = new HashMap<>();
        config.put("bytes.to.base64", true);

        // Тестируем рекурсивный подход
        try (RecursiveStructToMap<SinkRecord> recursiveXform = new RecursiveStructToMap<>()) {
            recursiveXform.configure(config);
            PerformanceResult recursiveResult = measurePerformance(
                "Recursive approach (Deep nesting " + DEEP_NESTING_LEVEL + " levels)",
                recursiveXform::apply,
                record
            );

            // Тестируем новый подход с Jackson Streaming
            try (StructToMap<SinkRecord> streamingXform = new StructToMap<>()) {
                streamingXform.configure(config);
                PerformanceResult streamingResult = measurePerformance(
                    "Streaming approach (Deep nesting " + DEEP_NESTING_LEVEL + " levels)",
                    streamingXform::apply,
                    record
                );

                // Сравниваем результаты
                double speedup = recursiveResult.avgTime() / streamingResult.avgTime();
                System.out.printf("Performance improvement: %.2fx faster%n", speedup);
            }
        }
    }

    @Test
    public void testWideStructPerformance() {
        // Создаем структуру с большим количеством полей на одном уровне
        SchemaBuilder builder = SchemaBuilder.struct();
        for (int i = 0; i < WIDE_FIELDS_COUNT; i++) {
            builder.field("field_" + i, Schema.STRING_SCHEMA);
            if (i % 10 == 0) { // Каждое 10-е поле - байтовое
                builder.field("bytes_" + i, Schema.BYTES_SCHEMA);
            }
        }
        Schema schema = builder.build();

        // Создаем структуру с данными
        Struct struct = new Struct(schema);
        for (int i = 0; i < WIDE_FIELDS_COUNT; i++) {
            struct.put("field_" + i, "value_" + i);
            if (i % 10 == 0) {
                struct.put("bytes_" + i, sampleBytes);
            }
        }

        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        Map<String, Object> config = new HashMap<>();
        config.put("bytes.to.base64", true);

        // Тестируем рекурсивный подход
        try (RecursiveStructToMap<SinkRecord> recursiveXform = new RecursiveStructToMap<>()) {
            recursiveXform.configure(config);
            PerformanceResult recursiveResult = measurePerformance(
                "Recursive approach (Wide structure " + WIDE_FIELDS_COUNT + " fields)",
                recursiveXform::apply,
                record
            );

            // Тестируем новый подход с Jackson Streaming
            try (StructToMap<SinkRecord> streamingXform = new StructToMap<>()) {
                streamingXform.configure(config);
                PerformanceResult streamingResult = measurePerformance(
                    "Streaming approach (Wide structure " + WIDE_FIELDS_COUNT + " fields)",
                    streamingXform::apply,
                    record
                );

                // Сравниваем результаты
                double speedup = recursiveResult.avgTime() / streamingResult.avgTime();
                System.out.printf("Performance improvement: %.2fx faster%n", speedup);
            }
        }
    }
}
