package transforms;

import static org.junit.jupiter.api.Assertions.*;

import com.clickhouse.kafka.connect.transforms.ConvertToMapOrJson;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;

public class ConvertToMapOrJsonTest {

    @Test
    public void testDateTypesJsonJacksonVsGson() {
        java.util.Date date = new java.util.Date(1715376000000L); // 2024-05-11T00:00:00Z
        java.sql.Timestamp timestamp = new java.sql.Timestamp(1715376000123L); // 2024-05-11T00:00:00.123Z

        Map<String, Object> map = new HashMap<>();
        map.put("date", date);
        map.put("timestamp", timestamp);
        SinkRecord record = new SinkRecord("topic", 0, null, null, null, map, 0L);

        // Jackson
        ConvertToMapOrJson<SinkRecord> xformJackson = new ConvertToMapOrJson<>();
        Map<String, Object> configJackson = new HashMap<>();
        configJackson.put("output.json.enabled", true);
        configJackson.put("output.json.library", "jackson");
        xformJackson.configure(configJackson);
        SinkRecord resultJackson = xformJackson.apply(record);
        assertTrue(resultJackson.value() instanceof String);
        String jsonJackson = (String) resultJackson.value();

        // Gson
        ConvertToMapOrJson<SinkRecord> xformGson = new ConvertToMapOrJson<>();
        Map<String, Object> configGson = new HashMap<>();
        configGson.put("output.json.enabled", true);
        configGson.put("output.json.library", "gson");
        xformGson.configure(configGson);
        SinkRecord resultGson = xformGson.apply(record);
        assertTrue(resultGson.value() instanceof String);
        String jsonGson = (String) resultGson.value();

        // Both must contain the same numeric values for date fields

        assertTrue(jsonJackson.contains("\"date\":" + date.getTime()));
        assertTrue(jsonJackson.contains("\"timestamp\":" + timestamp.getTime()));

        assertTrue(jsonGson.contains("\"date\":" + date.getTime()));
        assertTrue(jsonGson.contains("\"timestamp\":" + timestamp.getTime()));

        // The JSONs should be structurally equal (ignoring field order)
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            Map<String, Object> mapJackson = mapper.readValue(jsonJackson, Map.class);
            Map<String, Object> mapGson = mapper.readValue(jsonGson, Map.class);
            assertEquals(mapJackson, mapGson);
        } catch (Exception e) {
            fail("Failed to parse JSON: " + e.getMessage());
        }
    }
    @Test
    public void testStructToMapSimple() {
        Schema schema = SchemaBuilder.struct()
                .field("foo", Schema.STRING_SCHEMA)
                .field("bar", Schema.INT32_SCHEMA)
                .build();
        Struct struct = new Struct(schema)
                .put("foo", "abc")
                .put("bar", 123);
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        xform.configure(new HashMap<>());
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof Map);
        Map<?,?> map = (Map<?,?>) result.value();
        assertEquals("abc", map.get("foo"));
        assertEquals(123, map.get("bar"));
    }

    @Test
    public void testStructToMapNested() {
        Schema nestedSchema = SchemaBuilder.struct()
                .field("x", Schema.STRING_SCHEMA)
                .build();
        Schema schema = SchemaBuilder.struct()
                .field("foo", Schema.STRING_SCHEMA)
                .field("nested", nestedSchema)
                .build();
        Struct nested = new Struct(nestedSchema).put("x", "val");
        Struct struct = new Struct(schema)
                .put("foo", "abc")
                .put("nested", nested);
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        xform.configure(new HashMap<>());
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof Map);
        Map<?,?> map = (Map<?,?>) result.value();
        assertEquals("abc", map.get("foo"));
        assertTrue(map.get("nested") instanceof Map);
        Map<?,?> nestedMap = (Map<?,?>) map.get("nested");
        assertEquals("val", nestedMap.get("x"));
    }

    @Test
    public void testNonStructValue() {
        SinkRecord record = new SinkRecord("topic", 0, null, null, null, "string", 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        xform.configure(new HashMap<>());
        SinkRecord result = xform.apply(record);
        assertEquals("string", result.value());
    }

    @Test
    public void testStructToMapBytesToBase64() {
        Schema schema = SchemaBuilder.struct()
                .field("foo", Schema.BYTES_SCHEMA)
                .field("bar", Schema.STRING_SCHEMA)
                .build();
        byte[] bytes = new byte[] {1, 2, 3, 4};
        Struct struct = new Struct(schema)
                .put("foo", bytes)
                .put("bar", "abc");
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("bytes.to.base64", true);
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof Map);
        Map<?,?> map = (Map<?,?>) result.value();
        assertEquals(Base64.getEncoder().encodeToString(bytes), map.get("foo"));
        assertEquals("abc", map.get("bar"));
    }

    @Test
    public void testStructToMapByteBufferToBase64() {
        Schema schema = SchemaBuilder.struct()
                .field("foo", Schema.BYTES_SCHEMA)
                .build();
        byte[] bytes = new byte[] {10, 20, 30};
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(bytes);
        Struct struct = new Struct(schema)
                .put("foo", buf);
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("bytes.to.base64", true);
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof Map);
        Map<?,?> map = (Map<?,?>) result.value();
        assertEquals(Base64.getEncoder().encodeToString(bytes), map.get("foo"));
    }

    @Test
    public void testStructToMapJsonJackson() {
        Schema schema = SchemaBuilder.struct()
                .field("foo", Schema.STRING_SCHEMA)
                .field("bar", Schema.INT32_SCHEMA)
                .build();
        Struct struct = new Struct(schema)
                .put("foo", "abc")
                .put("bar", 123);
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("output.json.enabled", true);
        config.put("output.json.library", "jackson");
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof String);
        String json = (String) result.value();
        assertTrue(json.contains("\"foo\":\"abc\""));
        assertTrue(json.contains("\"bar\":123"));
    }

    @Test
    public void testStructToMapJsonGson() {
        Schema schema = SchemaBuilder.struct()
                .field("foo", Schema.STRING_SCHEMA)
                .field("bar", Schema.INT32_SCHEMA)
                .build();
        Struct struct = new Struct(schema)
                .put("foo", "abc")
                .put("bar", 123);
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("output.json.enabled", true);
        config.put("output.json.library", "gson");
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof String);
        String json = (String) result.value();
        assertTrue(json.contains("\"foo\":\"abc\""));
        assertTrue(json.contains("\"bar\":123"));
    }

    @Test
    public void testStructToMapJsonDefaultLibrary() {
        Schema schema = SchemaBuilder.struct()
                .field("foo", Schema.STRING_SCHEMA)
                .field("bar", Schema.INT32_SCHEMA)
                .build();
        Struct struct = new Struct(schema)
                .put("foo", "abc")
                .put("bar", 123);
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("output.json.enabled", true);
        // library not set, should use default (jackson)
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof String);
        String json = (String) result.value();
        assertTrue(json.contains("\"foo\":\"abc\""));
        assertTrue(json.contains("\"bar\":123"));
    }

    @Test
    public void testStructToMapJsonDisabled() {
        Schema schema = SchemaBuilder.struct()
                .field("foo", Schema.STRING_SCHEMA)
                .field("bar", Schema.INT32_SCHEMA)
                .build();
        Struct struct = new Struct(schema)
                .put("foo", "abc")
                .put("bar", 123);
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("output.json.enabled", false);
        config.put("output.json.library", "gson");
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof Map);
        Map<?,?> map = (Map<?,?>) result.value();
        assertEquals("abc", map.get("foo"));
        assertEquals(123, map.get("bar"));
    }

    @Test
    public void testStructToMapJsonBytesToBase64() {
        Schema schema = SchemaBuilder.struct()
                .field("foo", Schema.BYTES_SCHEMA)
                .field("bar", Schema.STRING_SCHEMA)
                .build();
        byte[] bytes = new byte[] {1, 2, 3, 4};
        Struct struct = new Struct(schema)
                .put("foo", bytes)
                .put("bar", "abc");
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("bytes.to.base64", true);
        config.put("output.json.enabled", true);
        config.put("output.json.library", "jackson");
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof String);
        String json = (String) result.value();
        assertTrue(json.contains("\"foo\":\"" + Base64.getEncoder().encodeToString(bytes) + "\""));
        assertTrue(json.contains("\"bar\":\"abc\""));
    }

    @Test
    public void testStructToMapJsonBytesNoBase64Jackson() {
        Schema schema = SchemaBuilder.struct()
                .field("foo", Schema.BYTES_SCHEMA)
                .field("bar", Schema.STRING_SCHEMA)
                .build();
        byte[] bytes = new byte[] {9, 8, 7, 6};
        Struct struct = new Struct(schema)
                .put("foo", bytes)
                .put("bar", "abc");
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("bytes.to.base64", false);
        config.put("output.json.enabled", true);
        config.put("output.json.library", "jackson");
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof String);
        String json = (String) result.value();
        // Jackson serializes byte[] as base64 string even if bytes.to.base64 is false
        assertTrue(json.contains("\"foo\":\"" + Base64.getEncoder().encodeToString(bytes) + "\""));
        assertTrue(json.contains("\"bar\":\"abc\""));
    }

    @Test
    public void testStructToMapJsonBytesNoBase64Gson() {
        Schema schema = SchemaBuilder.struct()
                .field("foo", Schema.BYTES_SCHEMA)
                .field("bar", Schema.STRING_SCHEMA)
                .build();
        byte[] bytes = new byte[] {4, 3, 2, 1};
        Struct struct = new Struct(schema)
                .put("foo", bytes)
                .put("bar", "xyz");
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("bytes.to.base64", false);
        config.put("output.json.enabled", true);
        config.put("output.json.library", "gson");
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof String);
        String json = (String) result.value();
        // Gson serializes byte[] as array of numbers
        assertTrue(json.contains("\"foo\":[4,3,2,1]"));
        assertTrue(json.contains("\"bar\":\"xyz\""));
    }

}
