package transforms;

import com.clickhouse.kafka.connect.transforms.HeadersToValue;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HeadersToValueTest {
    @Test
    public void applySchemalessTest() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("headerKey1", "headerValue1", null);
            headers.add("headerKey2", 123, null);

            SinkRecord record = TransformTestHelpers.schemalessRecord("test-topic", 0, headers);
            SinkRecord newRecord = headersToValue.apply(record);

            Assertions.assertInstanceOf(Map.class, newRecord.value());
            Map<?, ?> value = (Map<?, ?>) newRecord.value();
            Assertions.assertEquals("row-0", value.get("str"));

            Assertions.assertInstanceOf(Map.class, value.get("headers_map"));
            Map<?, ?> headersMap = (Map<?, ?>) value.get("headers_map");
            Assertions.assertEquals("headerValue1", headersMap.get("headerKey1"));
            Assertions.assertEquals("123", headersMap.get("headerKey2"));
        }
    }

    @Test
    public void applyWithSchemaTest() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("headerKey1", "headerValue1", null);
            headers.add("headerKey2", "headerValue2", null);

            SinkRecord record = TransformTestHelpers.schemaRecord("test-topic", 0, headers);
            SinkRecord newRecord = headersToValue.apply(record);

            Assertions.assertInstanceOf(Struct.class, newRecord.value());
            Struct newValue = (Struct) newRecord.value();
            Assertions.assertNotNull(newValue.schema().field("headers_map"));

            Map<?, ?> headersMap = (Map<?, ?>) newValue.get("headers_map");
            Assertions.assertEquals("headerValue1", headersMap.get("headerKey1"));
            Assertions.assertEquals("headerValue2", headersMap.get("headerKey2"));
        }
    }

    @Test
    @DisplayName("HDRS-08: stale valueSchema cache is rebuilt when the incoming record's schema changes")
    public void testStaleSchemaCacheRebuildsOnSchemaChange() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            SinkRecord first = TransformTestHelpers.schemaRecord("test-topic", 0, new ConnectHeaders());
            headersToValue.apply(first);

            ConnectHeaders secondHeaders = new ConnectHeaders();
            secondHeaders.add("headerKey1", "headerValue1", null);
            SinkRecord second =
                    TransformTestHelpers.schemaRecordAlternateSchema("test-topic", 1, secondHeaders);
            SinkRecord newRecord = headersToValue.apply(second);

            Struct newValue = (Struct) newRecord.value();
            Assertions.assertEquals("extra-1", newValue.get("extra_str"));
            Assertions.assertNull(newValue.schema().field("date_date"));

            Map<?, ?> headersMap = (Map<?, ?>) newValue.get("headers_map");
            Assertions.assertEquals("headerValue1", headersMap.get("headerKey1"));
        }
    }

    @Test
    @DisplayName("HDRS-06: the output record preserves metadata on the schema path")
    public void testMetadataPreservedSchemaPath() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("headerKey1", "headerValue1", null);
            SinkRecord record = TransformTestHelpers.schemaRecord("test-topic", 0, headers);
            SinkRecord newRecord = headersToValue.apply(record);

            TransformTestHelpers.assertMetadataPreserved(record, newRecord);
        }
    }

    @Test
    @DisplayName("HDRS-06: the output record preserves metadata on the schemaless path")
    public void testMetadataPreservedSchemalessPath() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("headerKey1", "headerValue1", null);
            SinkRecord record = TransformTestHelpers.schemalessRecord("test-topic", 0, headers);
            SinkRecord newRecord = headersToValue.apply(record);

            TransformTestHelpers.assertMetadataPreserved(record, newRecord);
        }
    }
}
