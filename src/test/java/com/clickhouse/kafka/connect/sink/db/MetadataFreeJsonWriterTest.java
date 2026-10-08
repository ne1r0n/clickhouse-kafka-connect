package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.util.DataJson;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Time;
import org.apache.kafka.connect.data.Timestamp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Docker-free, byte-level pins for {@link MetadataFreeJsonWriter} (JSONP-06; D-02, D-03, D-05, D-07,
 * D-18). Every assertion compares output bytes: a String decoded with the JVM default charset is
 * never used (the test JVM runs {@code file.encoding=windows-1252}), and {@link #latin1} maps each
 * byte to one char, so a String comparison through it is byte-exact. Deliberately extends nothing,
 * so it stays inside {@code testUnit}.
 */
public class MetadataFreeJsonWriterTest {

    private static final long MILLIS_PER_DAY = 86_400_000L;
    private static final long JAN_1_2024_EPOCH_DAY = 19_723L;
    private static final String DEBEZIUM_DATE = "io.debezium.time.Date";
    private static final String DEBEZIUM_MICRO_TIMESTAMP = "io.debezium.time.MicroTimestamp";
    private static final String DEBEZIUM_TIMESTAMP = "io.debezium.time.Timestamp";
    private static final String DEBEZIUM_ZONED_TIMESTAMP = "io.debezium.time.ZonedTimestamp";

    private static String latin1(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static Struct single(String name, Schema schema, Object value) {
        Schema structSchema = SchemaBuilder.struct().field(name, schema).build();
        return new Struct(structSchema).put(name, value);
    }

    private static byte[] row(String name, Schema schema, Object value) throws Exception {
        return MetadataFreeJsonWriter.structRow(single(name, schema, value));
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }

    private static byte[] bytesRow(byte[] content) throws Exception {
        return row("b", Schema.BYTES_SCHEMA, content);
    }

    private static byte[] expectedBytesRow(byte[] escapedContent) {
        return concat(ascii("{\"b\":\""), escapedContent, ascii("\"}"));
    }

    @ParameterizedTest(name = "JSONP-06: Timestamp {0} ms is written as {1}")
    @CsvSource({
        "1704067200123, 1704067200.123",
        "0, 0.000",
        "1, 0.001",
        "-1500, -1.500",
    })
    @DisplayName("JSONP-06: a Kafka Timestamp is an unquoted decimal-seconds number with exactly three fractional digits")
    void timestampIsUnquotedDecimalSecondsWithThreeFractionDigits(long millis, String expectedSeconds) throws Exception {
        byte[] actual = row("ts", Timestamp.SCHEMA, new Date(millis));
        assertEquals("{\"ts\":" + expectedSeconds + "}", latin1(actual));
    }

    @ParameterizedTest(name = "JSONP-06: Date {0} ms is written as day {1}")
    @CsvSource({
        "1704067200000, 19723",
        "0, 0",
        "-86400000, -1",
    })
    @DisplayName("JSONP-06: a Kafka Date is an unquoted integer count of epoch days")
    void dateIsUnquotedIntegerEpochDays(long millis, String expectedDays) throws Exception {
        byte[] actual = row("d", org.apache.kafka.connect.data.Date.SCHEMA, new Date(millis));
        assertEquals("{\"d\":" + expectedDays + "}", latin1(actual));
    }

    @Test
    @DisplayName("JSONP-06: a Kafka Time is written as its integer milliseconds")
    void timeIsIntegerMilliseconds() throws Exception {
        byte[] actual = row("t", Time.SCHEMA, new Date(45_296_789L));
        assertEquals("{\"t\":45296789}", latin1(actual));
    }

    @ParameterizedTest(name = "JSONP-06: Decimal {0} at scale {1} matches the shared mapper")
    @CsvSource({
        "12.3456, 4",
        "1E-7, 7",
        "0.0000, 4",
        "-12.3456, 4",
        "12345678901234567890.0123456789, 10",
    })
    @DisplayName("JSONP-06: a Decimal is written exactly as the shared mapper renders the same BigDecimal")
    void decimalMatchesTheSharedMapperRendering(String text, int scale) throws Exception {
        BigDecimal value = new BigDecimal(text);
        byte[] actual = row("amount", Decimal.schema(scale), value);
        String sharedRendering = DataJson.OBJECT_MAPPER.writeValueAsString(value);
        assertEquals("{\"amount\":" + sharedRendering + "}", latin1(actual));
    }

    @Test
    @DisplayName("JSONP-06: byte[] is written as its raw bytes inside a JSON string, never base64")
    void byteArrayIsRawBytes() throws Exception {
        assertEquals("{\"b\":\"abc\"}", latin1(bytesRow(new byte[] {0x61, 0x62, 0x63})));
    }

    @Test
    @DisplayName("JSONP-06: every byte 0x80..0xFF appears verbatim and unescaped")
    void highBytesAppearVerbatim() throws Exception {
        byte[] high = new byte[128];
        for (int i = 0; i < high.length; i++) {
            high[i] = (byte) (0x80 + i);
        }
        assertArrayEquals(expectedBytesRow(high), bytesRow(high));
    }

    @Test
    @DisplayName("JSONP-06: quote and backslash are backslash-escaped")
    void quoteAndBackslashAreEscaped() throws Exception {
        byte[] actual = bytesRow(ascii("a\"b\\c"));
        assertEquals("{\"b\":\"a\\\"b\\\\c\"}", latin1(actual));
    }

    @Test
    @DisplayName("JSONP-06: no raw byte below 0x20 survives in the output")
    void controlBytesNeverAppearRaw() throws Exception {
        byte[] control = new byte[0x20];
        for (int i = 0; i < control.length; i++) {
            control[i] = (byte) i;
        }
        byte[] actual = bytesRow(control);
        for (byte b : actual) {
            assertTrue((b & 0xFF) >= 0x20, "raw control byte " + (b & 0xFF) + " in " + latin1(actual));
        }
    }

    @Test
    @DisplayName("JSONP-06: invalid UTF-8 sequences C3 28, ED A0 80 and C0 80 appear verbatim")
    void invalidUtf8SequencesAppearVerbatim() throws Exception {
        byte[] invalid = {
            (byte) 0xC3, 0x28, (byte) 0xED, (byte) 0xA0, (byte) 0x80, (byte) 0xC0, (byte) 0x80
        };
        assertArrayEquals(expectedBytesRow(invalid), bytesRow(invalid));
    }

    @Test
    @DisplayName("JSONP-06: an empty byte[] is an empty JSON string")
    void emptyByteArrayIsEmptyString() throws Exception {
        assertEquals("{\"b\":\"\"}", latin1(bytesRow(new byte[0])));
    }

    @Test
    @DisplayName("JSONP-06: a ByteBuffer emits exactly its remaining bytes and keeps position and limit")
    void byteBufferEmitsRemainingBytesAndIsNotMutated() throws Exception {
        byte[] backing = {0x41, 0x42, 0x43, 0x44, 0x45, 0x46};

        ByteBuffer wrapped = ByteBuffer.wrap(backing, 2, 3);
        assertArrayEquals(expectedBytesRow(ascii("CDE")), row("b", Schema.BYTES_SCHEMA, wrapped));
        assertEquals(2, wrapped.position());
        assertEquals(5, wrapped.limit());
        assertEquals(3, wrapped.remaining());

        ByteBuffer direct = ByteBuffer.allocateDirect(3);
        direct.put(ascii("CDE")).flip();
        assertArrayEquals(expectedBytesRow(ascii("CDE")), row("b", Schema.BYTES_SCHEMA, direct));
        assertEquals(0, direct.position());
        assertEquals(3, direct.limit());

        ByteBuffer sliced = ByteBuffer.wrap(backing);
        sliced.position(3);
        ByteBuffer slice = sliced.slice();
        assertArrayEquals(expectedBytesRow(ascii("DEF")), row("b", Schema.BYTES_SCHEMA, slice));
        assertEquals(0, slice.position());
        assertEquals(3, slice.remaining());
    }

    @Test
    @DisplayName("JSONP-06: logical types are converted inside Array elements, Map values and nested Struct fields")
    void logicalTypesAreConvertedAtEveryDepth() throws Exception {
        assertEquals(
                "{\"a\":[1704067200.123,0.001]}",
                latin1(row(
                        "a",
                        SchemaBuilder.array(Timestamp.SCHEMA).build(),
                        List.of(new Date(1_704_067_200_123L), new Date(1L)))));

        assertEquals(
                "{\"m\":{\"a\":19723}}",
                latin1(row(
                        "m",
                        SchemaBuilder.map(Schema.STRING_SCHEMA, org.apache.kafka.connect.data.Date.SCHEMA).build(),
                        Map.of("a", new Date(JAN_1_2024_EPOCH_DAY * MILLIS_PER_DAY)))));

        Schema inner = SchemaBuilder.struct()
                .field("ts", Timestamp.SCHEMA)
                .field("amount", Decimal.schema(4))
                .build();
        Struct nested = new Struct(inner)
                .put("ts", new Date(1_704_067_200_123L))
                .put("amount", new BigDecimal("12.3456"));
        assertEquals("{\"s\":{\"ts\":1704067200.123,\"amount\":12.3456}}", latin1(row("s", inner, nested)));
    }

    @Test
    @DisplayName("JSONP-06: nested Struct null fields, Array null elements and Map null values are explicit JSON null")
    void nestedNullsAreExplicit() throws Exception {
        Schema inner = SchemaBuilder.struct().field("x", Schema.OPTIONAL_STRING_SCHEMA).build();
        assertEquals("{\"s\":{\"x\":null}}", latin1(row("s", inner, new Struct(inner))));

        assertEquals(
                "{\"a\":[\"v\",null]}",
                latin1(row("a", SchemaBuilder.array(Schema.OPTIONAL_STRING_SCHEMA).build(), Arrays.asList("v", null))));

        Map<String, String> withNull = new LinkedHashMap<>();
        withNull.put("k", null);
        assertEquals(
                "{\"m\":{\"k\":null}}",
                latin1(row("m", SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.OPTIONAL_STRING_SCHEMA).build(), withNull)));
    }

    @Test
    @DisplayName("JSONP-06: a top-level null field is omitted and fields follow schema order")
    void topLevelNullFieldIsOmittedAndSchemaOrderIsKept() throws Exception {
        Schema schema = SchemaBuilder.struct()
                .field("z", Schema.INT32_SCHEMA)
                .field("gone", Schema.OPTIONAL_STRING_SCHEMA)
                .field("a", Schema.INT32_SCHEMA)
                .build();
        Struct value = new Struct(schema).put("z", 1).put("a", 2);
        assertEquals("{\"z\":1,\"a\":2}", latin1(MetadataFreeJsonWriter.structRow(value)));
    }

    @Test
    @DisplayName("JSONP-06: serialising the same Struct twice yields byte-identical output")
    void structRowIsDeterministic() throws Exception {
        Schema schema = SchemaBuilder.struct()
                .field("ts", Timestamp.SCHEMA)
                .field("b", Schema.BYTES_SCHEMA)
                .field("m", SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.INT32_SCHEMA).build())
                .build();
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        Struct value = new Struct(schema)
                .put("ts", new Date(1_704_067_200_123L))
                .put("b", new byte[] {1, 2, (byte) 0xFF})
                .put("m", map);
        assertArrayEquals(MetadataFreeJsonWriter.structRow(value), MetadataFreeJsonWriter.structRow(value));
    }

    @Test
    @DisplayName("D-18: a CDC Timestamp value is written as decimal seconds")
    void cdcConnectTimestampIsDecimalSeconds() throws Exception {
        Map<String, Data> values = new LinkedHashMap<>();
        values.put("ts", new Data(Timestamp.SCHEMA, new Date(1_704_067_200_123L)));
        assertEquals("{\"ts\":1704067200.123}", latin1(MetadataFreeJsonWriter.cdcRow(values)));
    }

    @ParameterizedTest(name = "D-18: io.debezium.time.Date 2024-01-01 is day 19723 under JVM time zone {0}")
    @CsvSource({"Europe/Riga", "America/Los_Angeles", "UTC"})
    @DisplayName("D-18: a Debezium Date (java.sql.Date) is the same epoch day under any JVM default time zone")
    void cdcDebeziumDateIsEpochDayUnderAnyTimeZone(String zone) throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            Schema dateSchema = SchemaBuilder.int32().name(DEBEZIUM_DATE).build();
            Map<String, Data> values = new LinkedHashMap<>();
            values.put("d", new Data(dateSchema, java.sql.Date.valueOf(LocalDate.of(2024, 1, 1))));
            assertEquals("{\"d\":" + JAN_1_2024_EPOCH_DAY + "}", latin1(MetadataFreeJsonWriter.cdcRow(values)));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    @DisplayName("D-18: MicroTimestamp is decimal seconds with six fractional digits; Timestamp and ZonedTimestamp pass through")
    void cdcMicroTimestampConvertsAndOtherDebeziumTimesPassThrough() throws Exception {
        Map<String, Data> values = new LinkedHashMap<>();
        values.put("micro", new Data(SchemaBuilder.int64().name(DEBEZIUM_MICRO_TIMESTAMP).build(), 1_704_067_200_123_456L));
        values.put("millis", new Data(SchemaBuilder.int64().name(DEBEZIUM_TIMESTAMP).build(), 1_704_067_200_123L));
        values.put("zoned", new Data(SchemaBuilder.string().name(DEBEZIUM_ZONED_TIMESTAMP).build(), "2024-01-01T00:00:00Z"));
        assertEquals(
                "{\"micro\":1704067200.123456,\"millis\":1704067200123,\"zoned\":\"2024-01-01T00:00:00Z\"}",
                latin1(MetadataFreeJsonWriter.cdcRow(values)));
    }

    @Test
    @DisplayName("D-18: the synthetic _version BigInteger is a JSON number and a Data with a null object is omitted")
    void cdcSyntheticColumnsAndNullDataAreHandled() throws Exception {
        Map<String, Data> values = new LinkedHashMap<>();
        values.put("id", new Data(Schema.INT32_SCHEMA, 7));
        values.put("gone", new Data(Schema.OPTIONAL_STRING_SCHEMA, null));
        values.put("_version", new Data(Schema.BYTES_SCHEMA, BigInteger.valueOf(100)));
        values.put("is_deleted", new Data(Schema.INT8_SCHEMA, (byte) 0));
        assertEquals("{\"id\":7,\"_version\":100,\"is_deleted\":0}", latin1(MetadataFreeJsonWriter.cdcRow(values)));
    }

    @Test
    @DisplayName("JSONP-06: serialising the same CDC map twice yields byte-identical output")
    void cdcRowIsDeterministic() throws Exception {
        Map<String, Data> values = new LinkedHashMap<>();
        values.put("ts", new Data(Timestamp.SCHEMA, new Date(1_704_067_200_123L)));
        values.put("name", new Data(Schema.STRING_SCHEMA, "n"));
        assertArrayEquals(MetadataFreeJsonWriter.cdcRow(values), MetadataFreeJsonWriter.cdcRow(values));
    }

    @Test
    @DisplayName("D-05: loading MetadataFreeJsonWriter leaves the shared mapper writing epoch millis and base64")
    void sharedMapperIsNeverMutated() throws Exception {
        MetadataFreeJsonWriter.structRow(single("ts", Timestamp.SCHEMA, new Date(1L)));

        assertEquals(
                "{\"ts\":1704067200123}",
                DataJson.OBJECT_MAPPER.writeValueAsString(Map.of("ts", new Date(1_704_067_200_123L))));
        assertEquals(
                "{\"b\":\"YWJj\"}",
                DataJson.OBJECT_MAPPER.writeValueAsString(Map.of("b", new byte[] {0x61, 0x62, 0x63})));
    }

    @Test
    @DisplayName("JSONP-06: MetadataFreeJsonWriter holds no mutable static state")
    void writerHasOnlyFinalStaticFields() {
        for (java.lang.reflect.Field field : MetadataFreeJsonWriter.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                assertTrue(Modifier.isFinal(field.getModifiers()), "static field is not final: " + field.getName());
            }
        }
    }
}
