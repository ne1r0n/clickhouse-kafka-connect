package com.clickhouse.kafka.connect.transforms;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectWriter;

public class ConvertToMapOrJson<R extends ConnectRecord<R>> implements Transformation<R> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertToMapOrJson.class);

    public static final String BYTES_TO_BASE64_CONFIG = "bytes.to.base64";
    public static final boolean BYTES_TO_BASE64_DEFAULT = false;

    public static final String OUTPUT_JSON_ENABLED_CONFIG = "output.json.enabled";
    public static final boolean OUTPUT_JSON_ENABLED_DEFAULT = false;

    public static final String OUTPUT_JSON_LIBRARY_CONFIG = "output.json.library";
    public static final String OUTPUT_JSON_LIBRARY_DEFAULT = "jackson";

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(BYTES_TO_BASE64_CONFIG, ConfigDef.Type.BOOLEAN, BYTES_TO_BASE64_DEFAULT, ConfigDef.Importance.LOW, "Convert bytes fields to base64 strings")
            .define(OUTPUT_JSON_ENABLED_CONFIG, ConfigDef.Type.BOOLEAN, OUTPUT_JSON_ENABLED_DEFAULT, ConfigDef.Importance.LOW, "Convert struct/map to JSON string")
            .define(OUTPUT_JSON_LIBRARY_CONFIG, ConfigDef.Type.STRING, OUTPUT_JSON_LIBRARY_DEFAULT, ConfigDef.Importance.LOW, "Library for JSON conversion: jackson, gson");

    private boolean bytesToBase64 = BYTES_TO_BASE64_DEFAULT;
    private boolean outputJson = OUTPUT_JSON_ENABLED_DEFAULT;

    private enum JsonLibrary { JACKSON, GSON }

    private JsonLibrary selectedLibrary;
    private Function<Map<?, ?>, String> jsonEncoder;

    private static final ThreadLocal<java.util.Base64.Encoder> ENCODER =
            ThreadLocal.withInitial(java.util.Base64::getEncoder);

    @Override
    public void configure(Map<String, ?> configs) {
        Object param = configs.get(BYTES_TO_BASE64_CONFIG);
        bytesToBase64 = param instanceof Boolean ? (Boolean) param
                : param instanceof String ? Boolean.parseBoolean((String) param)
                : BYTES_TO_BASE64_DEFAULT;

        Object jsonParam = configs.get(OUTPUT_JSON_ENABLED_CONFIG);
        outputJson = jsonParam instanceof Boolean ? (Boolean) jsonParam
                : jsonParam instanceof String ? Boolean.parseBoolean((String) jsonParam)
                : OUTPUT_JSON_ENABLED_DEFAULT;

        Object libObj = configs.get(OUTPUT_JSON_LIBRARY_CONFIG);
        String lib = (libObj != null ? String.valueOf(libObj) : OUTPUT_JSON_LIBRARY_DEFAULT).toLowerCase();
        selectedLibrary = switch (lib) {
            case "jackson" -> JsonLibrary.JACKSON;
            case "gson" -> JsonLibrary.GSON;
            default -> throw new IllegalArgumentException("Unknown JSON library: " + lib);
        };

        jsonEncoder = switch (selectedLibrary) {
            case JACKSON -> {
                ObjectWriter writer = JacksonHolder.mapper
                        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        .writer();
                yield map -> {
                    try {
                        return writer.writeValueAsString(map);
                    } catch (Exception e) {
                        throw new RuntimeException("JSON serialization failed", e);
                    }
                };
            }
            case GSON -> map -> GsonHolder.gson.toJson(map);
        };
    }

    @Override
    public R apply(R record) {
        Object value = record.value();
        Object newValue = value;

        if (value instanceof Struct struct) {
            Map<String, Object> mapValue = structToMap(struct, bytesToBase64);
            newValue = outputJson ? jsonEncoder.apply(mapValue) : mapValue;
        } else if (value instanceof Map<?, ?> map && outputJson) {
            newValue = jsonEncoder.apply(map);
        } else if (value instanceof String str && outputJson) {
            newValue = str; // предполагаем, что уже сериализовано
        }

        return record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                record.keySchema(),
                record.key(),
                null,
                newValue,
                record.timestamp(),
                record.headers()
        );
    }

    private static Map<String, Object> structToMap(Struct struct, boolean bytesToBase64) {
        int size = struct.schema().fields().size();
        Map<String, Object> result = new LinkedHashMap<>(size * 4 / 3 + 1);
        java.util.Base64.Encoder encoder = bytesToBase64 ? ENCODER.get() : null;

        for (Field field : struct.schema().fields()) {
            Object value = struct.get(field);
            if (value == null) {
                result.put(field.name(), null);
                continue;
            }
            if (value instanceof Struct nested) {
                result.put(field.name(), structToMap(nested, bytesToBase64));
                continue;
            }
            if (value instanceof byte[] bytes) {
                result.put(field.name(), bytesToBase64 ? encoder.encodeToString(bytes) : bytes);
                continue;
            }
            if (value instanceof java.nio.ByteBuffer buf) {
                if (bytesToBase64) {
                    byte[] bytes;
                    if (buf.hasArray() && buf.arrayOffset() == 0 && buf.limit() == buf.capacity()) {
                        bytes = buf.array();
                    } else {
                        bytes = new byte[buf.remaining()];
                        buf.duplicate().get(bytes);
                    }
                    result.put(field.name(), encoder.encodeToString(bytes));
                } else {
                    result.put(field.name(), buf);
                }
                continue;
            }
            result.put(field.name(), value);
        }
        return result;
    }

    private static class JacksonHolder {
        static final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    }

    private static class GsonHolder {
        static final com.google.gson.Gson gson = new com.google.gson.Gson();
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public void close() {
        // No resources to close
    }
}
