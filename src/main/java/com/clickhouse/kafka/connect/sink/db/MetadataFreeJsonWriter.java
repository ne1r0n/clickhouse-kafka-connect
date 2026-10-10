package com.clickhouse.kafka.connect.sink.db;

import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.util.DataJson;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Time;
import org.apache.kafka.connect.data.Timestamp;

/**
 * Schema-aware JSONEachRow row writer, used only on the metadata-free route (an explicit {@code
 * insertFormat=json}), where no ClickHouse column metadata is available to correct Jackson's
 * defaults for Kafka logical types.
 *
 * <p>Each call writes one JSON object for one record. The output is a pure function of the
 * record's value and schema, so a retried batch is byte-identical, which the exactly-once
 * protocol requires. The class holds no mutable state and never mutates the shared {@link
 * DataJson#OBJECT_MAPPER}: the upstream {@code bypassRowBinary=true} route keeps using that
 * mapper unchanged.
 *
 * <p>Encodings are numeric and time-zone free so ClickHouse stores what RowBinary would store
 * without any forced insert setting: a Timestamp is decimal seconds with three fractional digits,
 * a Date is an integer count of epoch days, a Time is its integer milliseconds, a Decimal is
 * Jackson's BigDecimal number, and bytes are raw bytes inside a JSON string (never base64). A
 * Debezium {@code Timestamp}, {@code MicroTimestamp} and {@code NanoTimestamp} are decimal seconds
 * with three, six and nine fractional digits; a {@code ZonedTimestamp} string passes through
 * unchanged.
 */
final class MetadataFreeJsonWriter {

    private static final String MODULE_NAME = "MetadataFreeJsonWriter";

    private static final String KAFKA_TIMESTAMP = Timestamp.LOGICAL_NAME;
    private static final String KAFKA_DATE = org.apache.kafka.connect.data.Date.LOGICAL_NAME;
    private static final String KAFKA_TIME = Time.LOGICAL_NAME;
    private static final String KAFKA_DECIMAL = Decimal.LOGICAL_NAME;
    private static final String DEBEZIUM_DATE = "io.debezium.time.Date";
    private static final String DEBEZIUM_TIMESTAMP = "io.debezium.time.Timestamp";
    private static final String DEBEZIUM_MICRO_TIMESTAMP = "io.debezium.time.MicroTimestamp";
    private static final String DEBEZIUM_NANO_TIMESTAMP = "io.debezium.time.NanoTimestamp";

    private static final long MILLIS_PER_DAY = 86_400_000L;
    private static final int MILLIS_SCALE = 3;
    private static final int MICROS_SCALE = 6;
    private static final int NANOS_SCALE = 9;

    // A private copy: registering the row serializers on the shared mapper would change the bytes
    // of every other route that uses it.
    private static final ObjectMapper MAPPER = createMapper();

    private MetadataFreeJsonWriter() {}

    private record StructRow(Struct value) {}

    private record CdcRow(Map<String, Data> values) {}

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = DataJson.OBJECT_MAPPER.copy();
        SimpleModule module = new SimpleModule(MODULE_NAME);
        module.addSerializer(StructRow.class, new StructRowSerializer());
        module.addSerializer(CdcRow.class, new CdcRowSerializer());
        mapper.registerModule(module);
        return mapper;
    }

    /**
     * Serializes a record value to one JSON object. {@code writeValueAsBytes} is required: it uses
     * Jackson's UTF-8 byte generator, which the raw-bytes encoding depends on.
     */
    static byte[] structRow(Struct value) throws JsonProcessingException {
        return MAPPER.writeValueAsBytes(new StructRow(value));
    }

    /** Serializes the converted values of a Debezium CDC record to one JSON object. */
    static byte[] cdcRow(Map<String, Data> jsonMap) throws JsonProcessingException {
        return MAPPER.writeValueAsBytes(new CdcRow(jsonMap));
    }

    private static final class StructRowSerializer extends JsonSerializer<StructRow> {
        @Override
        public void serialize(StructRow row, JsonGenerator gen, SerializerProvider provider) throws IOException {
            Struct struct = row.value();
            gen.writeStartObject();
            for (Field field : struct.schema().fields()) {
                Object value = struct.get(field);
                if (value == null) {
                    continue;
                }
                gen.writeFieldName(field.name());
                writeValue(gen, field.schema(), value, provider);
            }
            gen.writeEndObject();
        }
    }

    private static final class CdcRowSerializer extends JsonSerializer<CdcRow> {
        @Override
        public void serialize(CdcRow row, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            for (Map.Entry<String, Data> entry : row.values().entrySet()) {
                Data data = entry.getValue();
                if (data == null || data.getObject() == null) {
                    continue;
                }
                gen.writeFieldName(entry.getKey());
                writeValue(gen, data.getSchema(), data.getObject(), provider);
            }
            gen.writeEndObject();
        }
    }

    private static void writeValue(JsonGenerator gen, Schema schema, Object value, SerializerProvider provider)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (schema == null) {
            provider.defaultSerializeValue(value, gen);
            return;
        }
        if (schema.name() != null && writeLogicalValue(gen, schema.name(), value)) {
            return;
        }
        switch (schema.type()) {
            case STRUCT:
                if (value instanceof Struct nested) {
                    writeNestedStruct(gen, nested, provider);
                    return;
                }
                break;
            case ARRAY:
                if (value instanceof List<?> list) {
                    gen.writeStartArray();
                    for (Object element : list) {
                        writeValue(gen, schema.valueSchema(), element, provider);
                    }
                    gen.writeEndArray();
                    return;
                }
                break;
            case MAP:
                if (value instanceof Map<?, ?> map) {
                    gen.writeStartObject();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        gen.writeFieldName(String.valueOf(entry.getKey()));
                        writeValue(gen, schema.valueSchema(), entry.getValue(), provider);
                    }
                    gen.writeEndObject();
                    return;
                }
                break;
            case BYTES:
                if (writeBytes(gen, value)) {
                    return;
                }
                break;
            default:
                break;
        }
        provider.defaultSerializeValue(value, gen);
    }

    private static void writeNestedStruct(JsonGenerator gen, Struct nested, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        for (Field field : nested.schema().fields()) {
            gen.writeFieldName(field.name());
            writeValue(gen, field.schema(), nested.get(field), provider);
        }
        gen.writeEndObject();
    }

    /**
     * Writes a logical-type value when {@code logicalName} and the runtime class of {@code value}
     * both match. Anything else, including ZonedTimestamp, returns false and falls through to the
     * caller's default rules.
     */
    private static boolean writeLogicalValue(JsonGenerator gen, String logicalName, Object value) throws IOException {
        switch (logicalName) {
            case KAFKA_TIMESTAMP:
                if (value instanceof java.util.Date timestamp) {
                    gen.writeNumber(BigDecimal.valueOf(timestamp.getTime(), MILLIS_SCALE));
                    return true;
                }
                return false;
            case KAFKA_DATE:
                if (value instanceof java.util.Date date) {
                    gen.writeNumber(Math.floorDiv(date.getTime(), MILLIS_PER_DAY));
                    return true;
                }
                return false;
            case KAFKA_TIME:
                if (value instanceof java.util.Date time) {
                    gen.writeNumber(time.getTime());
                    return true;
                }
                return false;
            case KAFKA_DECIMAL:
                if (value instanceof BigDecimal decimal) {
                    gen.writeNumber(decimal);
                    return true;
                }
                return false;
            case DEBEZIUM_DATE:
                // java.sql.Date is local midnight, so only its LocalDate is time-zone independent.
                if (value instanceof java.sql.Date sqlDate) {
                    gen.writeNumber(sqlDate.toLocalDate().toEpochDay());
                    return true;
                }
                return false;
            case DEBEZIUM_TIMESTAMP:
                if (value instanceof Number millis) {
                    gen.writeNumber(BigDecimal.valueOf(millis.longValue(), MILLIS_SCALE));
                    return true;
                }
                return false;
            case DEBEZIUM_MICRO_TIMESTAMP:
                if (value instanceof Number micros) {
                    gen.writeNumber(BigDecimal.valueOf(micros.longValue(), MICROS_SCALE));
                    return true;
                }
                return false;
            case DEBEZIUM_NANO_TIMESTAMP:
                if (value instanceof Number nanos) {
                    gen.writeNumber(BigDecimal.valueOf(nanos.longValue(), NANOS_SCALE));
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    /**
     * Writes {@code byte[]} or the remaining bytes of a {@link ByteBuffer} as raw bytes inside a JSON
     * string. {@code writeUTF8String} escapes only quote, backslash and control bytes and passes
     * every other byte through, which is exact on the UTF-8 byte generator {@code writeValueAsBytes}
     * uses. The caller's buffer position and limit are never moved.
     *
     * @return false when {@code value} is neither, e.g. a CDC {@code _version} BigInteger
     */
    private static boolean writeBytes(JsonGenerator gen, Object value) throws IOException {
        if (value instanceof byte[] bytes) {
            gen.writeUTF8String(bytes, 0, bytes.length);
            return true;
        }
        if (value instanceof ByteBuffer buffer) {
            if (buffer.hasArray()) {
                gen.writeUTF8String(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
            } else {
                byte[] copy = new byte[buffer.remaining()];
                buffer.duplicate().get(copy);
                gen.writeUTF8String(copy, 0, copy.length);
            }
            return true;
        }
        return false;
    }
}
