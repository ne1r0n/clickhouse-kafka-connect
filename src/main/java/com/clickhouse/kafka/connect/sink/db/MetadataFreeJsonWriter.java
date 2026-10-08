package com.clickhouse.kafka.connect.sink.db;

import com.clickhouse.kafka.connect.util.DataJson;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;

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
 */
final class MetadataFreeJsonWriter {

    private static final String MODULE_NAME = "MetadataFreeJsonWriter";

    // A private copy: registering the row serializers on the shared mapper would change the bytes
    // of every other route that uses it.
    private static final ObjectMapper MAPPER = createMapper();

    private MetadataFreeJsonWriter() {}

    private record StructRow(Struct value) {}

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = DataJson.OBJECT_MAPPER.copy();
        SimpleModule module = new SimpleModule(MODULE_NAME);
        module.addSerializer(StructRow.class, new StructRowSerializer());
        mapper.registerModule(module);
        return mapper;
    }

    /**
     * Serializes a record value to one JSON object. {@code writeValueAsBytes} is required: it uses
     * Jackson's UTF-8 byte generator, which exact byte handling depends on.
     */
    static byte[] structRow(Struct value) throws JsonProcessingException {
        return MAPPER.writeValueAsBytes(new StructRow(value));
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
}
