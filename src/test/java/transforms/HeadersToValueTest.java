package transforms;

import com.clickhouse.kafka.connect.transforms.HeadersToValue;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Assertions;
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

            Map<String, Object> originalValue = new HashMap<>();
            originalValue.put("existing", "value");

            SinkRecord record = new SinkRecord(UUID.randomUUID().toString(), 0, null, null, null, originalValue, 0L, 0L,
                    TimestampType.NO_TIMESTAMP_TYPE, headers);
            SinkRecord newRecord = headersToValue.apply(record);

            Assertions.assertInstanceOf(Map.class, newRecord.value());
            Map<?, ?> value = (Map<?, ?>) newRecord.value();
            Assertions.assertEquals("value", value.get("existing"));

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

            SinkRecord record = generateSampleRecord(UUID.randomUUID().toString(), 0, 0, headers);
            SinkRecord newRecord = headersToValue.apply(record);

            Assertions.assertInstanceOf(Struct.class, newRecord.value());
            Struct newValue = (Struct) newRecord.value();
            Assertions.assertNotNull(newValue.schema().field("headers_map"));

            Map<?, ?> headersMap = (Map<?, ?>) newValue.get("headers_map");
            Assertions.assertEquals("headerValue1", headersMap.get("headerKey1"));
            Assertions.assertEquals("headerValue2", headersMap.get("headerKey2"));
        }
    }

    private SinkRecord generateSampleRecord(String topic, int partition, long offset, ConnectHeaders headers) {
        Schema schema = SchemaBuilder.struct()
                .field("off16", Schema.INT16_SCHEMA)
                .field("timestamp_int64", Schema.INT64_SCHEMA)
                .field("date_date", Schema.INT32_SCHEMA)
                .build();
        Struct valueStruct = new Struct(schema)
                .put("off16", (short) new Random().nextInt(Short.MAX_VALUE + 1))
                .put("timestamp_int64", new Date().getTime())
                .put("date_date", (int) (new Date().getTime() / 1000));
        return new SinkRecord(topic, partition, null, null, schema, valueStruct, offset, 0L, TimestampType.NO_TIMESTAMP_TYPE, headers);
    }
}
