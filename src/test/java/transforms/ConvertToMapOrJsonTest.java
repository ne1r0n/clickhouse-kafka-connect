package transforms;

import static org.junit.jupiter.api.Assertions.*;

import com.clickhouse.kafka.connect.transforms.ConvertToMapOrJson;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @Test
    @DisplayName("CONV-02: Struct elements inside an ARRAY field convert to Maps, preserving order")
    public void testArrayOfStructsConvertedToListOfMaps() {
        Schema nestedSchema = SchemaBuilder.struct()
                .field("x", Schema.STRING_SCHEMA)
                .field("n", Schema.INT32_SCHEMA)
                .build();
        Schema schema = SchemaBuilder.struct()
                .field("items", SchemaBuilder.array(nestedSchema).build())
                .build();
        Struct s1 = new Struct(nestedSchema).put("x", "a").put("n", 1);
        Struct s2 = new Struct(nestedSchema).put("x", "b").put("n", 2);
        Struct s3 = new Struct(nestedSchema).put("x", "c").put("n", 3);
        List<Struct> items = List.of(s1, s2, s3);
        Struct struct = new Struct(schema).put("items", items);
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        xform.configure(new HashMap<>());
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof Map);
        Map<?, ?> map = (Map<?, ?>) result.value();
        Object itemsOut = map.get("items");
        assertTrue(itemsOut instanceof List);
        List<?> list = (List<?>) itemsOut;
        assertEquals(3, list.size());
        String[] expectedX = {"a", "b", "c"};
        Integer[] expectedN = {1, 2, 3};
        for (int i = 0; i < list.size(); i++) {
            Object elem = list.get(i);
            assertFalse(elem instanceof Struct);
            assertTrue(elem instanceof Map);
            Map<?, ?> elemMap = (Map<?, ?>) elem;
            assertEquals(expectedX[i], elemMap.get("x"));
            assertEquals(expectedN[i], elemMap.get("n"));
        }
    }

    @Test
    @DisplayName("CONV-02: Struct values inside a MAP field convert to Maps, preserving key order")
    public void testMapOfStructsConvertedToMapOfMaps() {
        Schema nestedSchema = SchemaBuilder.struct()
                .field("x", Schema.STRING_SCHEMA)
                .field("n", Schema.INT32_SCHEMA)
                .build();
        Schema schema = SchemaBuilder.struct()
                .field("byKey", SchemaBuilder.map(Schema.STRING_SCHEMA, nestedSchema).build())
                .build();
        Struct sA = new Struct(nestedSchema).put("x", "alpha").put("n", 10);
        Struct sB = new Struct(nestedSchema).put("x", "beta").put("n", 20);
        Map<String, Struct> byKey = new LinkedHashMap<>();
        byKey.put("keyB", sB);
        byKey.put("keyA", sA);
        Struct struct = new Struct(schema).put("byKey", byKey);
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        xform.configure(new HashMap<>());
        SinkRecord result = xform.apply(record);
        Map<?, ?> map = (Map<?, ?>) result.value();
        Object byKeyOut = map.get("byKey");
        assertTrue(byKeyOut instanceof Map);
        Map<?, ?> byKeyMap = (Map<?, ?>) byKeyOut;
        List<Object> outKeys = new ArrayList<>(byKeyMap.keySet());
        assertEquals(List.of("keyB", "keyA"), outKeys, "MAP entry-insertion order must be preserved");
        for (Map.Entry<?, ?> e : byKeyMap.entrySet()) {
            assertFalse(e.getValue() instanceof Struct);
            assertTrue(e.getValue() instanceof Map);
        }
        Map<?, ?> aMap = (Map<?, ?>) byKeyMap.get("keyA");
        assertEquals("alpha", aMap.get("x"));
        assertEquals(10, aMap.get("n"));
        Map<?, ?> bMap = (Map<?, ?>) byKeyMap.get("keyB");
        assertEquals("beta", bMap.get("x"));
        assertEquals(20, bMap.get("n"));
    }

    @Test
    @DisplayName("CONV-02: byte[] nested inside an ARRAY field and a MAP value both base64-encode correctly")
    public void testNestedBytesInArrayAndMapBase64Encoded() {
        byte[] arrayBytes = new byte[] {1, 2, 3};
        byte[] mapBytes = new byte[] {4, 5, 6};
        Schema schema = SchemaBuilder.struct()
                .field("byteList", SchemaBuilder.array(Schema.BYTES_SCHEMA).build())
                .field("byteMap", SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.BYTES_SCHEMA).build())
                .build();
        List<byte[]> byteList = List.of(arrayBytes);
        Map<String, byte[]> byteMap = new LinkedHashMap<>();
        byteMap.put("k", mapBytes);
        Struct struct = new Struct(schema).put("byteList", byteList).put("byteMap", byteMap);
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("bytes.to.base64", true);
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        Map<?, ?> map = (Map<?, ?>) result.value();
        List<?> listOut = (List<?>) map.get("byteList");
        assertEquals(Base64.getEncoder().encodeToString(arrayBytes), listOut.get(0));
        Map<?, ?> mapOut = (Map<?, ?>) map.get("byteMap");
        assertEquals(Base64.getEncoder().encodeToString(mapBytes), mapOut.get("k"));
    }

    @Test
    @DisplayName("CONV-02: empty ARRAY and MAP fields convert to empty collections, not null")
    public void testEmptyArrayAndMapFieldsConvertToEmptyCollections() {
        Schema nestedSchema = SchemaBuilder.struct().field("x", Schema.STRING_SCHEMA).build();
        Schema schema = SchemaBuilder.struct()
                .field("items", SchemaBuilder.array(nestedSchema).build())
                .field("byKey", SchemaBuilder.map(Schema.STRING_SCHEMA, nestedSchema).build())
                .build();
        Struct struct = new Struct(schema)
                .put("items", new ArrayList<Struct>())
                .put("byKey", new LinkedHashMap<String, Struct>());
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        xform.configure(new HashMap<>());
        SinkRecord result = xform.apply(record);
        Map<?, ?> map = (Map<?, ?>) result.value();
        Object itemsOut = map.get("items");
        assertNotNull(itemsOut);
        assertTrue(itemsOut instanceof List);
        assertEquals(0, ((List<?>) itemsOut).size());
        Object byKeyOut = map.get("byKey");
        assertNotNull(byKeyOut);
        assertTrue(byKeyOut instanceof Map);
        assertEquals(0, ((Map<?, ?>) byKeyOut).size());
    }

    @Test
    @DisplayName("CONV-02: adjacent structurally equal Structs in an ARRAY stay two distinct, equal maps")
    public void testAdjacentEqualStructsInArrayStayDistinct() {
        Schema nestedSchema = SchemaBuilder.struct().field("x", Schema.STRING_SCHEMA).build();
        Schema schema = SchemaBuilder.struct()
                .field("items", SchemaBuilder.array(nestedSchema).build())
                .build();
        Struct s1 = new Struct(nestedSchema).put("x", "same");
        Struct s2 = new Struct(nestedSchema).put("x", "same");
        assertEquals(s1, s2);
        assertNotSame(s1, s2);
        List<Struct> items = List.of(s1, s2);
        Struct struct = new Struct(schema).put("items", items);
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        xform.configure(new HashMap<>());
        SinkRecord result = xform.apply(record);
        Map<?, ?> map = (Map<?, ?>) result.value();
        List<?> listOut = (List<?>) map.get("items");
        assertEquals(2, listOut.size());
        assertEquals(listOut.get(0), listOut.get(1));
        assertNotSame(listOut.get(0), listOut.get(1));
    }

    @Test
    @DisplayName("CONV-04: metadata is preserved for the map-output configuration")
    public void testMetadataPreservedMapOutput() {
        ConnectHeaders headers = new ConnectHeaders();
        headers.addString("h1", "v1");
        SinkRecord input = TransformTestHelpers.schemaRecord("topic", 5L, headers);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        xform.configure(new HashMap<>());
        SinkRecord output = xform.apply(input);
        TransformTestHelpers.assertMetadataPreserved(input, output);
    }

    @Test
    @DisplayName("CONV-04: metadata is preserved for the JSON-output configuration")
    public void testMetadataPreservedJsonOutput() {
        ConnectHeaders headers = new ConnectHeaders();
        headers.addString("h1", "v1");
        SinkRecord input = TransformTestHelpers.schemaRecord("topic", 6L, headers);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("output.json.enabled", true);
        xform.configure(config);
        SinkRecord output = xform.apply(input);
        TransformTestHelpers.assertMetadataPreserved(input, output);
    }

    @Test
    @DisplayName("CONV-01: preserve.null.values=false omits the null field from the output map")
    public void testPreserveNullValuesFalseOmitsNullFieldFromMap() {
        Schema schema = SchemaBuilder.struct()
                .field("nullable", Schema.OPTIONAL_STRING_SCHEMA)
                .field("present", Schema.STRING_SCHEMA)
                .build();
        Struct struct = new Struct(schema).put("present", "abc");
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("preserve.null.values", false);
        config.put("output.json.enabled", false);
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        Map<?, ?> map = (Map<?, ?>) result.value();
        assertFalse(map.containsKey("nullable"));
        assertTrue(map.containsKey("present"));
        assertEquals("abc", map.get("present"));
    }

    @ParameterizedTest(name = "CONV-01: {0} omits the null field key from JSON when preserve.null.values=false")
    @ValueSource(strings = {"jackson", "gson"})
    @DisplayName("CONV-01: preserve.null.values=false omits the null field key from JSON, both libraries")
    public void testPreserveNullValuesFalseOmitsNullFieldFromJson(String library) {
        Schema schema = SchemaBuilder.struct()
                .field("nullable", Schema.OPTIONAL_STRING_SCHEMA)
                .field("present", Schema.STRING_SCHEMA)
                .build();
        Struct struct = new Struct(schema).put("present", "abc");
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("output.json.enabled", true);
        config.put("output.json.library", library);
        config.put("preserve.null.values", false);
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        String json = (String) result.value();
        assertFalse(json.contains("\"nullable\""), "library=" + library + " json=" + json);
        assertTrue(json.contains("\"present\":\"abc\""), "library=" + library + " json=" + json);
    }

    @Test
    @DisplayName("CONV-01: default preserve.null.values keeps the null field present in the map")
    public void testDefaultPreserveNullValuesKeepsNullFieldInMap() {
        Schema schema = SchemaBuilder.struct()
                .field("nullable", Schema.OPTIONAL_STRING_SCHEMA)
                .field("present", Schema.STRING_SCHEMA)
                .build();
        Struct struct = new Struct(schema).put("present", "abc");
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        xform.configure(new HashMap<>());
        SinkRecord result = xform.apply(record);
        Map<?, ?> map = (Map<?, ?>) result.value();
        assertTrue(map.containsKey("nullable"));
        assertNull(map.get("nullable"));
        assertTrue(map.containsKey("present"));
    }

    @Test
    @DisplayName("CONV-03: a bare Long value passes through unchanged when JSON output is disabled")
    public void testLongValuePassthroughWhenJsonDisabled() {
        SinkRecord record = new SinkRecord("topic", 0, null, null, null, 42L, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("output.json.enabled", false);
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        assertSame(record, result);
    }

    @Test
    @DisplayName("CONV-03: a String value passes through unchanged when JSON output is disabled")
    public void testStringValuePassthroughWhenJsonDisabled() {
        SinkRecord record = new SinkRecord("topic", 0, null, null, null, "just-a-string", 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("output.json.enabled", false);
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        assertSame(record, result);
    }

    @Test
    @DisplayName("CONV-03: a Map value passes through unchanged when JSON output is disabled")
    public void testMapValuePassthroughWhenJsonDisabled() {
        Map<String, Object> value = new HashMap<>();
        value.put("k", "v");
        SinkRecord record = new SinkRecord("topic", 0, null, null, null, value, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("output.json.enabled", false);
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        assertSame(record, result);
    }

    @Test
    @DisplayName("CONV-03: a null value passes through unchanged")
    public void testNullValuePassthrough() {
        SinkRecord record = new SinkRecord("topic", 0, null, null, null, null, 0L);
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        xform.configure(new HashMap<>());
        SinkRecord result = xform.apply(record);
        assertSame(record, result);
    }

    @Test
    @DisplayName("CONV-07: config() returns the shared CONFIG_DEF instance")
    public void testConfigReturnsSharedConfigDefInstance() {
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        assertSame(ConvertToMapOrJson.CONFIG_DEF, xform.config());
    }

    @Test
    @DisplayName("CONV-07: close() is repeatable and non-destructive")
    public void testCloseIsRepeatableAndNonDestructive() {
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        xform.configure(new HashMap<>());
        assertDoesNotThrow(() -> {
            xform.close();
            xform.close();
            xform.close();
        });
        Schema schema = SchemaBuilder.struct().field("foo", Schema.STRING_SCHEMA).build();
        Struct struct = new Struct(schema).put("foo", "abc");
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof Map);
    }

}
