package transforms;

import static org.junit.jupiter.api.Assertions.*;

import com.clickhouse.kafka.connect.transforms.StripConfluentPrefix;
import com.clickhouse.kafka.connect.transforms.StripConfluentPrefixConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StripConfluentPrefixTest {

    private StripConfluentPrefix<SinkRecord> xform;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        xform = new StripConfluentPrefix<>();
        objectMapper = new ObjectMapper(); // Initialize ObjectMapper for JSON parsing in tests
    }

    @AfterEach
    public void tearDown() {
        xform.close();
    }

    private byte[] addConfluentPrefix(byte[] payload, int schemaId) {
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + payload.length);
        buffer.put((byte) 0); // Magic byte
        buffer.putInt(schemaId);
        buffer.put(payload);
        return buffer.array();
    }

    @Test
    public void testStripPrefixNoHeader() {
        Map<String, Object> props = new HashMap<>();
        props.put(StripConfluentPrefixConfig.ADD_SCHEMA_ID_HEADER_CONFIG, "false");
        xform.configure(props);

        String originalJson = "{\"field\":\"value\"}";
        byte[] originalPayload = originalJson.getBytes(StandardCharsets.UTF_8);
        int schemaId = 123;
        byte[] prefixedPayload = addConfluentPrefix(originalPayload, schemaId);

        SinkRecord originalRecord = new SinkRecord("test-topic", 0, null, null, null, prefixedPayload, 100L);
        SinkRecord transformedRecord = xform.apply(originalRecord);

        assertNotNull(transformedRecord);
        assertArrayEquals(originalPayload, (byte[]) transformedRecord.value());
        assertEquals(originalJson, new String((byte[]) transformedRecord.value(), StandardCharsets.UTF_8));
        assertTrue(transformedRecord.headers().isEmpty()); // No header should be added
    }

    @Test
    public void testStripPrefixAddHeader() {
        Map<String, Object> props = new HashMap<>();
        props.put(StripConfluentPrefixConfig.ADD_SCHEMA_ID_HEADER_CONFIG, "true");
        props.put(StripConfluentPrefixConfig.SCHEMA_ID_HEADER_NAME_CONFIG, "mySchemaIdHeader");
        xform.configure(props);

        String originalJson = "{\"another\":\"data\"}";
        byte[] originalPayload = originalJson.getBytes(StandardCharsets.UTF_8);
        int schemaId = 456;
        byte[] prefixedPayload = addConfluentPrefix(originalPayload, schemaId);

        SinkRecord originalRecord = new SinkRecord("test-topic", 0, null, null, null, prefixedPayload, 101L);
        SinkRecord transformedRecord = xform.apply(originalRecord);

        assertNotNull(transformedRecord);
        assertArrayEquals(originalPayload, (byte[]) transformedRecord.value());
        assertEquals(originalJson, new String((byte[]) transformedRecord.value(), StandardCharsets.UTF_8));

        Header schemaIdHeader = transformedRecord.headers().lastWithName("mySchemaIdHeader");
        assertNotNull(schemaIdHeader, "Schema ID header should be present");
        // Headers.addInt stores as 4-byte integer
        assertEquals(Schema.INT32_SCHEMA, schemaIdHeader.schema());
        assertEquals(schemaId, schemaIdHeader.value());

    }

     @Test
    public void testStripPrefixAddHeaderDefaultName() {
        Map<String, Object> props = new HashMap<>();
        props.put(StripConfluentPrefixConfig.ADD_SCHEMA_ID_HEADER_CONFIG, "true");
        // Use default header name: confluentSchemaId
        xform.configure(props);

        String originalJson = "{\"key\": 1}";
        byte[] originalPayload = originalJson.getBytes(StandardCharsets.UTF_8);
        int schemaId = 789;
        byte[] prefixedPayload = addConfluentPrefix(originalPayload, schemaId);

        SinkRecord originalRecord = new SinkRecord("test-topic", 0, null, null, null, prefixedPayload, 102L);
        SinkRecord transformedRecord = xform.apply(originalRecord);

        assertNotNull(transformedRecord);
        assertArrayEquals(originalPayload, (byte[]) transformedRecord.value());

        Header schemaIdHeader = transformedRecord.headers().lastWithName("confluentSchemaId"); // Default name
        assertNotNull(schemaIdHeader, "Schema ID header should be present with default name");
        assertEquals(Schema.INT32_SCHEMA, schemaIdHeader.schema());
        assertEquals(schemaId, schemaIdHeader.value());
    }


    @Test
    public void testNullValue() {
        xform.configure(Collections.emptyMap());
        SinkRecord originalRecord = new SinkRecord("test-topic", 0, null, null, null, null, 103L);
        SinkRecord transformedRecord = xform.apply(originalRecord);
        assertSame(originalRecord, transformedRecord); // Should return original record unmodified
        assertNull(transformedRecord.value());
    }

    @Test
    public void testNonByteArrayValue() {
        xform.configure(Collections.emptyMap());
        SinkRecord originalRecord = new SinkRecord("test-topic", 0, null, null, Schema.STRING_SCHEMA, "not bytes", 104L);
        SinkRecord transformedRecord = xform.apply(originalRecord);
        assertSame(originalRecord, transformedRecord); // Should return original record unmodified
        assertEquals("not bytes", transformedRecord.value());
    }

    @Test
    public void testValueTooShort() {
        xform.configure(Collections.emptyMap());
        byte[] shortPayload = new byte[]{0x0, 0x1, 0x2, 0x3}; // Only 4 bytes, less than prefix size 5
        SinkRecord originalRecord = new SinkRecord("test-topic", 0, null, null, null, shortPayload, 105L);
        SinkRecord transformedRecord = xform.apply(originalRecord);
        assertSame(originalRecord, transformedRecord); // Should return original record unmodified
        assertArrayEquals(shortPayload, (byte[]) transformedRecord.value());
    }

    @Test
    public void testIncorrectMagicByte() {
        xform.configure(Collections.emptyMap());
        byte[] payload = "{\"data\":1}".getBytes(StandardCharsets.UTF_8);
        byte[] badPrefixPayload = addConfluentPrefix(payload, 99);
        badPrefixPayload[0] = 0x1; // Incorrect magic byte

        SinkRecord originalRecord = new SinkRecord("test-topic", 0, null, null, null, badPrefixPayload, 106L);
        SinkRecord transformedRecord = xform.apply(originalRecord);
        assertSame(originalRecord, transformedRecord); // Should return original record unmodified
        assertArrayEquals(badPrefixPayload, (byte[]) transformedRecord.value());
    }

    // --- Tests for adding schema ID to JSON field ---

    @Test
    public void testStripPrefixAddField() throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put(StripConfluentPrefixConfig.ADD_SCHEMA_ID_FIELD_CONFIG, "true");
        props.put(StripConfluentPrefixConfig.SCHEMA_ID_FIELD_NAME_CONFIG, "customSchemaIdField");
        props.put(StripConfluentPrefixConfig.ADD_SCHEMA_ID_HEADER_CONFIG, "false"); // Ensure header is off
        xform.configure(props);

        String originalJson = "{\"field\":\"value\", \"nested\": {\"key\": 1}}";
        byte[] originalPayload = originalJson.getBytes(StandardCharsets.UTF_8);
        int schemaId = 555;
        byte[] prefixedPayload = addConfluentPrefix(originalPayload, schemaId);

        SinkRecord originalRecord = new SinkRecord("test-topic", 0, null, null, null, prefixedPayload, 107L);
        SinkRecord transformedRecord = xform.apply(originalRecord);

        assertNotNull(transformedRecord);
        assertTrue(transformedRecord.value() instanceof byte[], "Value should be byte[]");
        assertTrue(transformedRecord.headers().isEmpty(), "Headers should be empty");

        // Deserialize the result and check
        Map<String, Object> resultJson = objectMapper.readValue((byte[]) transformedRecord.value(), new TypeReference<Map<String, Object>>() {});

        assertEquals("value", resultJson.get("field"));
        assertTrue(resultJson.get("nested") instanceof Map);
        assertEquals(1, ((Map<?, ?>) resultJson.get("nested")).get("key"));
        assertEquals(schemaId, resultJson.get("customSchemaIdField")); // Check custom field name and value
        assertEquals(3, resultJson.size()); // Original fields + schema ID field
    }

    @Test
    public void testStripPrefixAddFieldDefaultName() throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put(StripConfluentPrefixConfig.ADD_SCHEMA_ID_FIELD_CONFIG, "true");
        // Use default field name: _confluentSchemaId
        xform.configure(props);

        String originalJson = "{\"foo\":\"bar\"}";
        byte[] originalPayload = originalJson.getBytes(StandardCharsets.UTF_8);
        int schemaId = 666;
        byte[] prefixedPayload = addConfluentPrefix(originalPayload, schemaId);

        SinkRecord originalRecord = new SinkRecord("test-topic", 0, null, null, null, prefixedPayload, 108L);
        SinkRecord transformedRecord = xform.apply(originalRecord);

        assertNotNull(transformedRecord);
        assertTrue(transformedRecord.value() instanceof byte[], "Value should be byte[]");

        Map<String, Object> resultJson = objectMapper.readValue((byte[]) transformedRecord.value(), new TypeReference<Map<String, Object>>() {});

        assertEquals("bar", resultJson.get("foo"));
        assertEquals(schemaId, resultJson.get("_confluentSchemaId")); // Check default field name
        assertEquals(2, resultJson.size());
    }

    @Test
    public void testStripPrefixAddFieldAndHeader() throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put(StripConfluentPrefixConfig.ADD_SCHEMA_ID_FIELD_CONFIG, "true");
        props.put(StripConfluentPrefixConfig.SCHEMA_ID_FIELD_NAME_CONFIG, "jsonSchemaId");
        props.put(StripConfluentPrefixConfig.ADD_SCHEMA_ID_HEADER_CONFIG, "true");
        props.put(StripConfluentPrefixConfig.SCHEMA_ID_HEADER_NAME_CONFIG, "headerSchemaId");
        xform.configure(props);

        String originalJson = "{\"data\": true}";
        byte[] originalPayload = originalJson.getBytes(StandardCharsets.UTF_8);
        int schemaId = 777;
        byte[] prefixedPayload = addConfluentPrefix(originalPayload, schemaId);

        SinkRecord originalRecord = new SinkRecord("test-topic", 0, null, null, null, prefixedPayload, 109L);
        SinkRecord transformedRecord = xform.apply(originalRecord);

        assertNotNull(transformedRecord);
        assertTrue(transformedRecord.value() instanceof byte[], "Value should be byte[]");

        // Check field in JSON
        Map<String, Object> resultJson = objectMapper.readValue((byte[]) transformedRecord.value(), new TypeReference<Map<String, Object>>() {});
        assertEquals(true, resultJson.get("data"));
        assertEquals(schemaId, resultJson.get("jsonSchemaId"));
        assertEquals(2, resultJson.size());

        // Check header
        Header schemaIdHeader = transformedRecord.headers().lastWithName("headerSchemaId");
        assertNotNull(schemaIdHeader, "Schema ID header should be present");
        assertEquals(Schema.INT32_SCHEMA, schemaIdHeader.schema());
        assertEquals(schemaId, schemaIdHeader.value());
    }

     @Test
    public void testAddFieldToNonJsonObject() {
        // Test case where the payload is not a JSON object (e.g., array or primitive)
        // The transform should ideally log a warning and not attempt to add the field.
        Map<String, Object> props = new HashMap<>();
        props.put(StripConfluentPrefixConfig.ADD_SCHEMA_ID_FIELD_CONFIG, "true");
        props.put(StripConfluentPrefixConfig.SCHEMA_ID_FIELD_NAME_CONFIG, "schemaIdField");
        xform.configure(props);

        String originalJsonArray = "[1, 2, 3]"; // Not a JSON object
        byte[] originalPayload = originalJsonArray.getBytes(StandardCharsets.UTF_8);
        int schemaId = 888;
        byte[] prefixedPayload = addConfluentPrefix(originalPayload, schemaId);

        SinkRecord originalRecord = new SinkRecord("test-topic", 0, null, null, null, prefixedPayload, 110L);
        SinkRecord transformedRecord = xform.apply(originalRecord);

        assertNotNull(transformedRecord);
        // Expect the value to be the stripped original payload, without the added field
        assertArrayEquals(originalPayload, (byte[]) transformedRecord.value());
        assertEquals(originalJsonArray, new String((byte[]) transformedRecord.value(), StandardCharsets.UTF_8));
        // We expect a log warning in the actual implementation here
    }

     @Test
    public void testAddFieldWithInvalidJson() {
        // Test case where the payload is invalid JSON after stripping
        Map<String, Object> props = new HashMap<>();
        props.put(StripConfluentPrefixConfig.ADD_SCHEMA_ID_FIELD_CONFIG, "true");
        props.put(StripConfluentPrefixConfig.SCHEMA_ID_FIELD_NAME_CONFIG, "schemaIdField");
        xform.configure(props);

        String invalidJson = "{\"key\": value_without_quotes}"; // Invalid JSON
        byte[] originalPayload = invalidJson.getBytes(StandardCharsets.UTF_8);
        int schemaId = 999;
        byte[] prefixedPayload = addConfluentPrefix(originalPayload, schemaId);

        SinkRecord originalRecord = new SinkRecord("test-topic", 0, null, null, null, prefixedPayload, 111L);
        SinkRecord transformedRecord = xform.apply(originalRecord);

        assertNotNull(transformedRecord);
        // Expect the value to be the stripped original payload, as parsing/modification fails
        assertArrayEquals(originalPayload, (byte[]) transformedRecord.value());
        assertEquals(invalidJson, new String((byte[]) transformedRecord.value(), StandardCharsets.UTF_8));
         // We expect a log warning/error in the actual implementation here
    }
}