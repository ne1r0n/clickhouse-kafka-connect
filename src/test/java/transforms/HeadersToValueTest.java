package transforms;

import com.clickhouse.kafka.connect.transforms.HeadersToValue;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Schema;
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

    @Test
    @DisplayName("HDRS-01: a tombstone record is returned as the identical object")
    public void testTombstonePassesThroughUnchanged() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            SinkRecord record = TransformTestHelpers.tombstone("test-topic", 0L);
            SinkRecord newRecord = headersToValue.apply(record);

            Assertions.assertSame(record, newRecord);
        }
    }

    @Test
    @DisplayName("HDRS-01: a tombstone carrying headers is still returned as the identical object")
    public void testTombstoneWithHeadersPassesThroughUnchanged() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("headerKey1", "headerValue1", null);
            SinkRecord record = tombstoneWithHeaders("test-topic", 0L, headers);
            SinkRecord newRecord = headersToValue.apply(record);

            Assertions.assertSame(record, newRecord);
        }
    }

    @Test
    @DisplayName("HDRS-02: a record with no headers gets an empty map, schema path")
    public void testNoHeadersYieldsEmptyMapSchemaPath() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            SinkRecord record = TransformTestHelpers.schemaRecord("test-topic", 0, new ConnectHeaders());
            SinkRecord newRecord = headersToValue.apply(record);

            Struct newValue = (Struct) newRecord.value();
            Object headersMapObj = newValue.get("headers_map");
            Assertions.assertInstanceOf(Map.class, headersMapObj);
            Assertions.assertEquals(0, ((Map<?, ?>) headersMapObj).size());
        }
    }

    @Test
    @DisplayName("HDRS-02: a record with no headers gets an empty map, schemaless path")
    public void testNoHeadersYieldsEmptyMapSchemalessPath() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            SinkRecord record =
                    TransformTestHelpers.schemalessRecord("test-topic", 0, new ConnectHeaders());
            SinkRecord newRecord = headersToValue.apply(record);

            Map<?, ?> value = (Map<?, ?>) newRecord.value();
            Object headersMapObj = value.get("headers_map");
            Assertions.assertInstanceOf(Map.class, headersMapObj);
            Assertions.assertEquals(0, ((Map<?, ?>) headersMapObj).size());
        }
    }

    @Test
    @DisplayName("HDRS-03: a null-valued header is stored as a present, null map entry, schema path")
    public void testNullValuedHeaderStoredAsNullEntrySchemaPath() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("nullHeader", null, null);
            headers.add("normalHeader", "normalValue", null);

            SinkRecord record = TransformTestHelpers.schemaRecord("test-topic", 0, headers);
            SinkRecord newRecord = headersToValue.apply(record);

            Struct newValue = (Struct) newRecord.value();
            Map<?, ?> headersMap = (Map<?, ?>) newValue.get("headers_map");
            Assertions.assertTrue(headersMap.containsKey("nullHeader"));
            Assertions.assertNull(headersMap.get("nullHeader"));
            Assertions.assertEquals("normalValue", headersMap.get("normalHeader"));
        }
    }

    @Test
    @DisplayName("HDRS-03: a null-valued header is stored as a present, null map entry, schemaless path")
    public void testNullValuedHeaderStoredAsNullEntrySchemalessPath() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("nullHeader", null, null);
            headers.add("normalHeader", "normalValue", null);

            SinkRecord record = TransformTestHelpers.schemalessRecord("test-topic", 0, headers);
            SinkRecord newRecord = headersToValue.apply(record);

            Map<?, ?> value = (Map<?, ?>) newRecord.value();
            Map<?, ?> headersMap = (Map<?, ?>) value.get("headers_map");
            Assertions.assertTrue(headersMap.containsKey("nullHeader"));
            Assertions.assertNull(headersMap.get("nullHeader"));
            Assertions.assertEquals("normalValue", headersMap.get("normalHeader"));
        }
    }

    @Test
    @DisplayName("HDRS-07: a header added three times resolves to its last value, schema path")
    public void testDuplicateHeaderLastWinsSchemaPath() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("dup", "first", null);
            headers.add("dup", "second", null);
            headers.add("dup", "third", null);

            SinkRecord record = TransformTestHelpers.schemaRecord("test-topic", 0, headers);
            SinkRecord newRecord = headersToValue.apply(record);

            Struct newValue = (Struct) newRecord.value();
            Map<?, ?> headersMap = (Map<?, ?>) newValue.get("headers_map");
            Assertions.assertEquals("third", headersMap.get("dup"));
        }
    }

    @Test
    @DisplayName("HDRS-07: a header added three times resolves to its last value, schemaless path")
    public void testDuplicateHeaderLastWinsSchemalessPath() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("dup", "first", null);
            headers.add("dup", "second", null);
            headers.add("dup", "third", null);

            SinkRecord record = TransformTestHelpers.schemalessRecord("test-topic", 0, headers);
            SinkRecord newRecord = headersToValue.apply(record);

            Map<?, ?> value = (Map<?, ?>) newRecord.value();
            Map<?, ?> headersMap = (Map<?, ?>) value.get("headers_map");
            Assertions.assertEquals("third", headersMap.get("dup"));
        }
    }

    @Test
    @DisplayName("HDRS-07: non-string header values are coerced via toString()")
    public void testNonStringHeaderValuesCoerced() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("intHeader", 123, null);
            headers.add("boolHeader", true, null);

            SinkRecord record = TransformTestHelpers.schemaRecord("test-topic", 0, headers);
            SinkRecord newRecord = headersToValue.apply(record);

            Struct newValue = (Struct) newRecord.value();
            Map<?, ?> headersMap = (Map<?, ?>) newValue.get("headers_map");
            Assertions.assertEquals("123", headersMap.get("intHeader"));
            Assertions.assertEquals("true", headersMap.get("boolHeader"));
        }
    }

    @Test
    @DisplayName("HDRS-07: an empty-string header value is stored as an empty string, distinguishable from a null entry")
    public void testEmptyStringHeaderValueStoredDistinctFromNull() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("emptyHeader", "", null);
            headers.add("nullHeader", null, null);

            SinkRecord record = TransformTestHelpers.schemaRecord("test-topic", 0, headers);
            SinkRecord newRecord = headersToValue.apply(record);

            Struct newValue = (Struct) newRecord.value();
            Map<?, ?> headersMap = (Map<?, ?>) newValue.get("headers_map");
            Assertions.assertTrue(headersMap.containsKey("emptyHeader"));
            Assertions.assertEquals("", headersMap.get("emptyHeader"));
            Assertions.assertTrue(headersMap.containsKey("nullHeader"));
            Assertions.assertNull(headersMap.get("nullHeader"));
        }
    }

    @Test
    @DisplayName("HDRS-07: an escaped-unicode header value survives apply() unchanged")
    public void testEscapedUnicodeHeaderValueUnchanged() {
        try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("field", "headers_map");
            headersToValue.configure(configs);

            String cyrillicAndCjk = cyrillicAndCjkValue();
            ConnectHeaders headers = new ConnectHeaders();
            headers.add("unicodeHeader", cyrillicAndCjk, null);

            SinkRecord record = TransformTestHelpers.schemaRecord("test-topic", 0, headers);
            SinkRecord newRecord = headersToValue.apply(record);

            Struct newValue = (Struct) newRecord.value();
            Map<?, ?> headersMap = (Map<?, ?>) newValue.get("headers_map");
            Assertions.assertEquals(cyrillicAndCjk, headersMap.get("unicodeHeader"));
        }
    }

    /** Same tombstone shape as {@link TransformTestHelpers#tombstone}, but carrying headers, per HDRS-01. */
    private static SinkRecord tombstoneWithHeaders(String topic, long offset, ConnectHeaders headers) {
        return new SinkRecord(
                topic,
                0,
                Schema.STRING_SCHEMA,
                "key-" + offset,
                null,
                null,
                offset,
                1715376000123L,
                TimestampType.CREATE_TIME,
                headers);
    }

    /**
     * Cyrillic ("Privet") plus CJK ("Chinese") test value, built from Unicode code points at
     * runtime rather than escape-sequence literals in source: this codebase's editing tool chain
     * has been observed to silently rewrite literal backslash-u escape text into real non-ASCII
     * bytes, and compileTestJava pins no source encoding.
     */
    private static String cyrillicAndCjkValue() {
        int[] codePoints = {0x041F, 0x0440, 0x0438, 0x0432, 0x0435, 0x0442, 0x0020, 0x4E2D, 0x6587};
        return new String(codePoints, 0, codePoints.length);
    }
}
