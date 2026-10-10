package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.*;

import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.helper.RowSnapshots;
import com.clickhouse.kafka.connect.test.junit.extension.FromVersionConditionExtension;
import com.clickhouse.kafka.connect.test.junit.extension.SinceClickHouseVersion;
import com.clickhouse.kafka.connect.transforms.ConvertToMapOrJson;
import java.util.*;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(FromVersionConditionExtension.class)
public class JsonColumnTest extends ClickHouseBase {
    private static final int RECORDS = 10000;
    private static final int BYTE_ARRAY_SIZE = 128;
    private byte[] sampleBytes;
    private String tableName;

    /**
     * How the sample bytes land in {@code json_data.nested_bytes}. {@code bytes.to.base64} only
     * affects {@code Struct} input and these tests send {@code Map} values, so the rendering is
     * decided by the JSON library: Gson serializes a {@code byte[]} as an array of signed numbers,
     * every other configuration stores the Base64 text.
     */
    private enum BytesRendering {
        BASE64_STRING,
        SIGNED_BYTE_ARRAY
    }

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

    private void runJsonTest(
            Map<String, String> props,
            Map<String, Object> transformConfig,
            String testLabel,
            BytesRendering bytesRendering)
            throws Exception {
        // props.put(ClickHouseSinkConnector.CLIENT_VERSION, "V2");
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        try (Records ignored = chc.queryV2(
                String.format("CREATE TABLE %s (id UInt32, json_data JSON) ENGINE = MergeTree ORDER BY id", tableName))) {
            // table created
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<SinkRecord> sourceRecords = createTestRecords(tableName);
        
        try (ConvertToMapOrJson<SinkRecord> transform = new ConvertToMapOrJson<>()) {
            transform.configure(transformConfig);

            // Apply transformation to the full record value
            List<SinkRecord> transformedRecords = sourceRecords.stream()
                .map(transform::apply)
                .toList();

            // Insert records
            ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
            try {
                sinkTask.start(props);
                sinkTask.put(transformedRecords);
            } finally {
                sinkTask.stop();
            }

            // Verify results and data points
            assertEquals(RECORDS, ClickHouseTestHelpers.countRows(chc, tableName));
            assertPayloadValues(chc, bytesRendering);
        }
    }

    private void assertPayloadValues(ClickHouseHelperClient chc, BytesRendering bytesRendering)
            throws Exception {
        String expectedBytes;
        String expectedBytesType;
        if (bytesRendering == BytesRendering.BASE64_STRING) {
            expectedBytes = Base64.getEncoder().encodeToString(sampleBytes);
            expectedBytesType = "String";
        } else {
            StringJoiner signed = new StringJoiner(",", "[", "]");
            for (byte b : sampleBytes) {
                signed.add(Byte.toString(b));
            }
            expectedBytes = signed.toString();
            expectedBytesType = "Array(";
        }

        List<Map<String, String>> rows = RowSnapshots.select(
                chc,
                String.format(
                        "SELECT id, toString(json_data.nested_str) AS s, toString(json_data.nested_int) AS n, "
                                + "dynamicType(json_data.nested_bytes) AS bt, toString(json_data.nested_bytes) AS b "
                                + "FROM %s ORDER BY id",
                        tableName),
                "id", "s", "n", "bt", "b");
        assertEquals(RECORDS, rows.size());
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            assertEquals(String.valueOf(i), row.get("id"));
            assertEquals("test" + i, row.get("s"), "nested_str of row " + i);
            assertEquals(String.valueOf(i), row.get("n"), "nested_int of row " + i);
            if (bytesRendering == BytesRendering.BASE64_STRING) {
                assertEquals(expectedBytesType, row.get("bt"), "nested_bytes dynamic type of row " + i);
            } else {
                assertTrue(
                        row.get("bt").startsWith(expectedBytesType),
                        "nested_bytes dynamic type of row " + i + " was " + row.get("bt"));
            }
            assertEquals(expectedBytes, row.get("b"), "nested_bytes of row " + i);
        }
    }

    @Test
    @SinceClickHouseVersion("25.3")
    public void testBytesNoBase64NoJson() throws Exception {
        Map<String, String> props = getBaseProps();
        Map<String, Object> transformConfig = Map.of(
            "bytes.to.base64", false,
            "output.json.enabled", false
        );
        runJsonTest(props, transformConfig, "No Base64, No JSON", BytesRendering.BASE64_STRING);
    }

    @Test
    @SinceClickHouseVersion("25.3")
    public void testBytesBase64NoJson() throws Exception {
        Map<String, String> props = getBaseProps();
        Map<String, Object> transformConfig = Map.of(
            "bytes.to.base64", true,
            "output.json.enabled", false
        );
        runJsonTest(props, transformConfig, "Base64, No JSON", BytesRendering.BASE64_STRING);
    }

    @Test
    @SinceClickHouseVersion("25.3")
    public void testBytesNoBase64JsonWithJackson() throws Exception {
        Map<String, String> props = getBaseProps();
        Map<String, Object> transformConfig = Map.of(
            "bytes.to.base64", false,
            "output.json.enabled", true,
            "output.json.library", "jackson"
        );
        runJsonTest(props, transformConfig, "No Base64, JSON (Jackson)", BytesRendering.BASE64_STRING);
    }

    @Test
    @SinceClickHouseVersion("25.3")
    public void testBytesNoBase64JsonWithGson() throws Exception {
        Map<String, String> props = getBaseProps();
        Map<String, Object> transformConfig = Map.of(
            "bytes.to.base64", false,
            "output.json.enabled", true,
            "output.json.library", "gson"
        );
        runJsonTest(props, transformConfig, "No Base64, JSON (Gson)", BytesRendering.SIGNED_BYTE_ARRAY);
    }

    @Test
    @SinceClickHouseVersion("25.3")
    public void testBytesBase64JsonWithJackson() throws Exception {
        Map<String, String> props = getBaseProps();
        Map<String, Object> transformConfig = Map.of(
            "bytes.to.base64", true,
            "output.json.enabled", true,
            "output.json.library", "jackson"
        );
        runJsonTest(props, transformConfig, "Base64, JSON (Jackson)", BytesRendering.BASE64_STRING);
    }

    @Test
    @SinceClickHouseVersion("25.3")
    public void testBytesBase64JsonWithGson() throws Exception {
        Map<String, String> props = getBaseProps();
        Map<String, Object> transformConfig = Map.of(
            "bytes.to.base64", true,
            "output.json.enabled", true,
            "output.json.library", "gson"
        );
        runJsonTest(props, transformConfig, "Base64, JSON (Gson)", BytesRendering.SIGNED_BYTE_ARRAY);
    }

    @Disabled("Disabled because it not suitable for unit tests")
    @Test
    @SinceClickHouseVersion("25.3")
    public void compareAllConfigurations() throws Exception {
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
            Map<String, String> props = getBaseProps();

            String label = String.format("base64=%s, json=%s%s",
                config.get("bytes.to.base64"),
                config.get("output.json.enabled"),
                config.get("output.json.enabled").equals(true)
                    ? ", lib=" + config.get("output.json.library")
                    : "");

            long startTime = System.nanoTime();
            runJsonTest(
                    props,
                    config,
                    label,
                    "gson".equals(config.get("output.json.library"))
                            ? BytesRendering.SIGNED_BYTE_ARRAY
                            : BytesRendering.BASE64_STRING);
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
