package transforms;

import com.clickhouse.kafka.connect.transforms.HeaderToValue;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
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

    @Test
    @DisplayName("HDR-01: configure() throws ConfigException when the fields key is absent")
    public void testConfigureThrowsConfigExceptionWhenFieldsAbsent() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("headers", "headerKey1");
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Assertions.assertThrows(ConfigException.class, () -> headerToValue.configure(configs));
        }
    }

    @Test
    @DisplayName("HDR-01: configure() throws ConfigException when the headers key is absent")
    public void testConfigureThrowsConfigExceptionWhenHeadersAbsent() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("fields", "field1");
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Assertions.assertThrows(ConfigException.class, () -> headerToValue.configure(configs));
        }
    }

    @Test
    @DisplayName("HDR-01: configure() throws DataException when fields outnumber headers")
    public void testConfigureThrowsDataExceptionWhenFieldsOutnumberHeaders() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("fields", "a,b");
        configs.put("headers", "h1");
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            DataException ex =
                    Assertions.assertThrows(DataException.class, () -> headerToValue.configure(configs));
            Assertions.assertTrue(ex.getMessage().contains("must match the number of headers"));
        }
    }

    @Test
    @DisplayName("HDR-01: configure() throws DataException when headers outnumber fields")
    public void testConfigureThrowsDataExceptionWhenHeadersOutnumberFields() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("fields", "a");
        configs.put("headers", "h1,h2");
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            DataException ex =
                    Assertions.assertThrows(DataException.class, () -> headerToValue.configure(configs));
            Assertions.assertTrue(ex.getMessage().contains("must match the number of headers"));
        }
    }

    @Test
    @DisplayName("HDR-06: non-string header values are coerced via toString() on the schema path")
    public void testNonStringHeaderValuesCoercedSchemaPath() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "n_int,n_bool");
            configs.put("headers", "h-int,h-bool");
            headerToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.addInt("h-int", 123);
            headers.addBoolean("h-bool", true);

            SinkRecord record = TransformTestHelpers.schemaRecord("topic1", 0, headers);
            SinkRecord newRecord = headerToValue.apply(record);
            Struct newValue = (Struct) newRecord.value();

            Assertions.assertEquals("123", newValue.get("n_int"));
            Assertions.assertEquals("true", newValue.get("n_bool"));
        }
    }

    @Test
    @DisplayName("HDR-06: non-string header values are coerced via toString() on the schemaless path")
    public void testNonStringHeaderValuesCoercedSchemalessPath() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "n_int,n_bool");
            configs.put("headers", "h-int,h-bool");
            headerToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.addInt("h-int", 123);
            headers.addBoolean("h-bool", true);

            SinkRecord record = TransformTestHelpers.schemalessRecord("topic1", 0, headers);
            SinkRecord newRecord = headerToValue.apply(record);
            Map<String, Object> value = (Map<String, Object>) newRecord.value();

            Assertions.assertEquals("123", value.get("n_int"));
            Assertions.assertEquals("true", value.get("n_bool"));
        }
    }

    @Test
    @DisplayName("HDR-06: empty-string and zero-valued headers are copied, not treated as missing")
    public void testEmptyStringAndZeroValuedHeadersAreCopied() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "empty_field,zero_field");
            configs.put("headers", "h-empty,h-zero");
            headerToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.addString("h-empty", "");
            headers.addInt("h-zero", 0);

            SinkRecord record = TransformTestHelpers.schemaRecord("topic1", 0, headers);
            SinkRecord newRecord = headerToValue.apply(record);
            Struct newValue = (Struct) newRecord.value();

            Assertions.assertEquals("", newValue.get("empty_field"));
            Assertions.assertEquals("0", newValue.get("zero_field"));
        }
    }

    // Escaped-unicode literal for the Cyrillic-plus-CJK test value, built from int[] code points
    // at runtime rather than backslash-u escape text in source, because the editing tool chain
    // used to author this file silently decodes literal backslash-u escape sequences into real
    // non-ASCII bytes.
    private static final String CYRILLIC_CJK_LITERAL =
            new String(
                    new int[] {0x041f, 0x0440, 0x0438, 0x0432, 0x0435, 0x0442, 0x0020, 0x4e2d, 0x6587},
                    0,
                    9);

    @Test
    @DisplayName("HDR-06: an escaped-unicode header value is copied byte-identically via toString()")
    public void testEscapedUnicodeHeaderValueCopiedUnchanged() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "trace_id");
            configs.put("headers", "trace-id");
            headerToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.addString("trace-id", CYRILLIC_CJK_LITERAL);

            SinkRecord record = TransformTestHelpers.schemaRecord("topic1", 0, headers);
            SinkRecord newRecord = headerToValue.apply(record);
            Struct newValue = (Struct) newRecord.value();

            Assertions.assertEquals(CYRILLIC_CJK_LITERAL, newValue.get("trace_id"));
        }
    }

    @Test
    @DisplayName("HDR-06: config() returns the shared CONFIG_DEF instance")
    public void testConfigReturnsSharedConfigDefInstance() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Assertions.assertSame(HeaderToValue.CONFIG_DEF, headerToValue.config());
        }
    }

    @Test
    @DisplayName("HDR-06: close() can be called repeatedly without throwing")
    public void testCloseCanBeCalledRepeatedly() {
        HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>();
        Map<String, Object> configs = new HashMap<>();
        configs.put("fields", "field1");
        configs.put("headers", "headerKey1");
        headerToValue.configure(configs);

        Assertions.assertDoesNotThrow(
                () -> {
                    headerToValue.close();
                    headerToValue.close();
                    headerToValue.close();
                });
    }

    @Test
    @DisplayName("HDR-02: a header added three times copies its last value on the schema path")
    public void testMultiValuedHeaderLastWinsSchemaPath() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "trace_id");
            configs.put("headers", "trace-id");
            headerToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.addString("trace-id", "first");
            headers.addString("trace-id", "second");
            headers.addString("trace-id", "third");

            SinkRecord record = TransformTestHelpers.schemaRecord("topic1", 0, headers);
            SinkRecord newRecord = headerToValue.apply(record);
            Struct newValue = (Struct) newRecord.value();

            Assertions.assertEquals("third", newValue.get("trace_id"));
        }
    }

    @Test
    @DisplayName("HDR-02: a header added three times copies its last value on the schemaless path")
    public void testMultiValuedHeaderLastWinsSchemalessPath() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "trace_id");
            configs.put("headers", "trace-id");
            headerToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.addString("trace-id", "first");
            headers.addString("trace-id", "second");
            headers.addString("trace-id", "third");

            SinkRecord record = TransformTestHelpers.schemalessRecord("topic1", 0, headers);
            SinkRecord newRecord = headerToValue.apply(record);
            Map<String, Object> value = (Map<String, Object>) newRecord.value();

            Assertions.assertEquals("third", value.get("trace_id"));
        }
    }

    @Test
    @DisplayName("HDR-03: a missing header leaves a newly added field present and null on the schema path")
    public void testMissingHeaderLeavesNewFieldPresentAndNull() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "trace_id");
            configs.put("headers", "trace-id");
            headerToValue.configure(configs);

            SinkRecord record = TransformTestHelpers.schemaRecord("topic1", 0, new ConnectHeaders());
            SinkRecord newRecord = headerToValue.apply(record);
            Struct newValue = (Struct) newRecord.value();

            Assertions.assertNotNull(newValue.schema().field("trace_id"));
            Assertions.assertNull(newValue.get("trace_id"));
        }
    }

    @Test
    @DisplayName("HDR-03: a missing header leaves a pre-existing field with its original value on the schema path")
    public void testMissingHeaderLeavesExistingFieldUnchanged() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "str");
            configs.put("headers", "trace-id");
            headerToValue.configure(configs);

            SinkRecord record = TransformTestHelpers.schemaRecord("topic1", 0, new ConnectHeaders());
            SinkRecord newRecord = headerToValue.apply(record);
            Struct newValue = (Struct) newRecord.value();

            Assertions.assertEquals("row-0", newValue.get("str"));
        }
    }

    @Test
    @DisplayName("HDR-03: a null-valued header leaves the target field untouched on the schema path")
    public void testNullValuedHeaderLeavesFieldUntouched() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "str");
            configs.put("headers", "trace-id");
            headerToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.add("trace-id", null, null);

            SinkRecord record = TransformTestHelpers.schemaRecord("topic1", 0, headers);
            SinkRecord newRecord = headerToValue.apply(record);
            Struct newValue = (Struct) newRecord.value();

            Assertions.assertEquals("row-0", newValue.get("str"));
        }
    }

    @Test
    @DisplayName("HDR-03: a missing or null-valued header leaves the target key absent on the schemaless path")
    public void testMissingOrNullHeaderLeavesKeyAbsentSchemalessPath() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "trace_id");
            configs.put("headers", "trace-id");
            headerToValue.configure(configs);

            SinkRecord missingHeaderRecord =
                    TransformTestHelpers.schemalessRecord("topic1", 0, new ConnectHeaders());
            SinkRecord missingResult = headerToValue.apply(missingHeaderRecord);
            Map<String, Object> missingValue = (Map<String, Object>) missingResult.value();
            Assertions.assertFalse(missingValue.containsKey("trace_id"));

            ConnectHeaders nullHeaders = new ConnectHeaders();
            nullHeaders.add("trace-id", null, null);
            SinkRecord nullHeaderRecord = TransformTestHelpers.schemalessRecord("topic1", 1, nullHeaders);
            SinkRecord nullResult = headerToValue.apply(nullHeaderRecord);
            Map<String, Object> nullValue = (Map<String, Object>) nullResult.value();
            Assertions.assertFalse(nullValue.containsKey("trace_id"));
        }
    }

    @Test
    @DisplayName("HDR-04: a configured field absent from the original schema is added as an optional string")
    public void testAddedFieldIsOptionalString() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "trace_id");
            configs.put("headers", "trace-id");
            headerToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.addString("trace-id", "trace-value");

            SinkRecord record = TransformTestHelpers.schemaRecord("topic1", 0, headers);
            SinkRecord newRecord = headerToValue.apply(record);
            Schema newSchema = newRecord.valueSchema();

            Assertions.assertNotNull(newSchema.field("trace_id"));
            Assertions.assertEquals(Schema.Type.STRING, newSchema.field("trace_id").schema().type());
            Assertions.assertTrue(newSchema.field("trace_id").schema().isOptional());
            Assertions.assertEquals(Schema.Type.INT16, newSchema.field("off16").schema().type());
        }
    }

    @Test
    @DisplayName("HDR-04: the added field stays present and null when the header is missing")
    public void testAddedFieldPresentAndNullWhenHeaderMissing() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "trace_id");
            configs.put("headers", "trace-id");
            headerToValue.configure(configs);

            SinkRecord record = TransformTestHelpers.schemaRecord("topic1", 0, new ConnectHeaders());
            SinkRecord newRecord = headerToValue.apply(record);
            Struct newValue = (Struct) newRecord.value();

            Assertions.assertNotNull(newValue.schema().field("trace_id"));
            Assertions.assertNull(newValue.get("trace_id"));
        }
    }

    @Test
    @DisplayName("HDR-04: field-name matching against the original schema is exact case-sensitive equality")
    public void testFieldNameMatchingIsCaseSensitive() {
        try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
            Map<String, Object> configs = new HashMap<>();
            configs.put("fields", "trace_id,Trace_Id");
            configs.put("headers", "trace-id,Trace-Id");
            headerToValue.configure(configs);

            ConnectHeaders headers = new ConnectHeaders();
            headers.addString("trace-id", "lower-value");
            headers.addString("Trace-Id", "upper-value");

            SinkRecord record = TransformTestHelpers.schemaRecord("topic1", 0, headers);
            SinkRecord newRecord = headerToValue.apply(record);
            Schema newSchema = newRecord.valueSchema();

            Assertions.assertNotNull(newSchema.field("trace_id"));
            Assertions.assertNotNull(newSchema.field("Trace_Id"));
            Assertions.assertEquals(Schema.OPTIONAL_STRING_SCHEMA, newSchema.field("trace_id").schema());
            Assertions.assertEquals(Schema.OPTIONAL_STRING_SCHEMA, newSchema.field("Trace_Id").schema());
        }
    }
}
