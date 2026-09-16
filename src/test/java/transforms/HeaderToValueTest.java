package transforms;

import com.clickhouse.kafka.connect.transforms.HeaderToValue;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HeaderToValueTest {
    @Test
    public void applySchemalessTest() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "field1,field2");
            configs.put("headers", "headerKey1,headerKey2");
            headerToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("headerKey1", "headerValue1", null);
            headers.add("headerKey2", "headerValue2", null);

            SinkRecord record = TransformTestHelpers.schemalessRecord("topic1", 0, headers);
            SinkRecord newRecord = headerToValue.apply(record);

            Assertions.assertInstanceOf(Map.class, newRecord.value());
            Map<String, Object> value = (Map<String, Object>) newRecord.value();
            Assertions.assertEquals("headerValue1", value.get("field1"));
            Assertions.assertEquals("headerValue2", value.get("field2"));
        }
    }

    @Test
    public void applyWithSchemaTest() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "field1,field2");
            configs.put("headers", "headerKey1,headerKey2");
            headerToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("headerKey1", "headerValue1", null);
            headers.add("headerKey2", "headerValue2", null);

            SinkRecord record = TransformTestHelpers.schemaRecord("topic1", 0, headers);
            SinkRecord newRecord = headerToValue.apply(record);

            Assertions.assertInstanceOf(Struct.class, newRecord.value());
            Struct newValue = (Struct) newRecord.value();
            Assertions.assertEquals("headerValue1", newValue.get("field1"));
            Assertions.assertEquals("headerValue2", newValue.get("field2"));
        }
    }

    @Test
    @DisplayName("HDR-06: stale valueSchema cache is rebuilt when the incoming record's schema changes")
    public void testStaleSchemaCacheRebuildsOnSchemaChange() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "trace_id");
            configs.put("headers", "trace-id");
            headerToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("trace-id", "trace-value", null);

            SinkRecord first = TransformTestHelpers.schemaRecord("topic1", 0, headers);
            headerToValue.apply(first);

            SinkRecord second = TransformTestHelpers.schemaRecordAlternateSchema("topic1", 1, headers);
            SinkRecord newSecond = headerToValue.apply(second);
            Struct newValue = (Struct) newSecond.value();

            Assertions.assertEquals("extra-1", newValue.get("extra_str"));
            Assertions.assertEquals("trace-value", newValue.get("trace_id"));
            Assertions.assertNull(newValue.schema().field("date_date"));
        }
    }

    @Test
    @DisplayName("HDR-05: record metadata survives apply() on the schema path")
    public void testMetadataPreservedSchemaPath() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "trace_id");
            configs.put("headers", "trace-id");
            headerToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("trace-id", "trace-value", null);

            SinkRecord record = TransformTestHelpers.schemaRecord("topic1", 0, headers);
            SinkRecord newRecord = headerToValue.apply(record);

            TransformTestHelpers.assertMetadataPreserved(record, newRecord);
        }
    }

    @Test
    @DisplayName("HDR-05: record metadata survives apply() on the schemaless path")
    public void testMetadataPreservedSchemalessPath() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "trace_id");
            configs.put("headers", "trace-id");
            headerToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("trace-id", "trace-value", null);

            SinkRecord record = TransformTestHelpers.schemalessRecord("topic1", 0, headers);
            SinkRecord newRecord = headerToValue.apply(record);

            TransformTestHelpers.assertMetadataPreserved(record, newRecord);
        }
    }
}
