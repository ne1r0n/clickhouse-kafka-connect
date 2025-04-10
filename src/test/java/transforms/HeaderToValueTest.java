package transforms;

import com.clickhouse.kafka.connect.transforms.HeaderToValue;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

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

            SinkRecord record = new SinkRecord(UUID.randomUUID().toString(), 0, null, null, null, new HashMap<>(), 0L, 0L, TimestampType.NO_TIMESTAMP_TYPE, headers);
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

            SinkRecord record = generateSampleRecord(UUID.randomUUID().toString(), 0, 0, headers);
            SinkRecord newRecord = headerToValue.apply(record);

            Assertions.assertInstanceOf(Struct.class, newRecord.value());
            Struct newValue = (Struct) newRecord.value();
            Assertions.assertEquals("headerValue1", newValue.get("field1"));
            Assertions.assertEquals("headerValue2", newValue.get("field2"));
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