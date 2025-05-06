package com.clickhouse.kafka.connect.transforms;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Universal transformation: converts Struct to Map, or Map to JSON string if output.json.enabled=true.
 * If value is already a Map and output.json.enabled is true, converts Map to JSON string.
 * If value is Struct, always converts to Map (and then to JSON if enabled).
 * Otherwise, returns value as is.
 */
public class ConvertToMapOrJson<R extends ConnectRecord<R>> implements Transformation<R> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertToMapOrJson.class);

    public static final String BYTES_TO_BASE64_CONFIG = "bytes.to.base64";
    public static final boolean BYTES_TO_BASE64_DEFAULT = false;
    public static final String OUTPUT_JSON_ENABLED_CONFIG = "output.json.enabled";
    public static final boolean OUTPUT_JSON_ENABLED_DEFAULT = false;
    public static final String OUTPUT_JSON_LIBRARY_CONFIG = "output.json.library";
    public static final String OUTPUT_JSON_LIBRARY_DEFAULT = "jackson"; // options: jackson, gson

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(BYTES_TO_BASE64_CONFIG, ConfigDef.Type.BOOLEAN, BYTES_TO_BASE64_DEFAULT, ConfigDef.Importance.LOW, "Convert bytes fields to base64 strings")
            .define(OUTPUT_JSON_ENABLED_CONFIG, ConfigDef.Type.BOOLEAN, OUTPUT_JSON_ENABLED_DEFAULT, ConfigDef.Importance.LOW, "Convert struct/map to JSON string")
            .define(OUTPUT_JSON_LIBRARY_CONFIG, ConfigDef.Type.STRING, OUTPUT_JSON_LIBRARY_DEFAULT, ConfigDef.Importance.LOW, "Library for JSON conversion: jackson, gson");

    private boolean bytesToBase64 = BYTES_TO_BASE64_DEFAULT;
    private boolean outputJson = OUTPUT_JSON_ENABLED_DEFAULT;
    private String jsonLibrary = OUTPUT_JSON_LIBRARY_DEFAULT;

    // ThreadLocal encoder for multithreaded usage
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
        Object libParam = configs.get(OUTPUT_JSON_LIBRARY_CONFIG);
        jsonLibrary = libParam != null ? libParam.toString().toLowerCase() : OUTPUT_JSON_LIBRARY_DEFAULT;
    }

    @Override
    public R apply(R record) {
        Object value = record.value();
        Object newValue = value;
        if (value instanceof Struct struct) {
            Map<String, Object> mapValue = structToMap(struct, bytesToBase64);
            newValue = outputJson ? toJsonString(mapValue, jsonLibrary) : mapValue;
        } else if (value instanceof Map && outputJson) {
            newValue = toJsonString((Map<?, ?>) value, jsonLibrary);
        }
        return record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                record.keySchema(),
                record.key(),
                null, // schemaless
                newValue,
                record.timestamp(),
                record.headers()
        );
    }

    private static String toJsonString(Map<?, ?> map, String lib) {
        try {
            switch (lib) {
                case "jackson":
                    return JacksonHolder.mapper.writeValueAsString(map);
                case "gson":
                    return GsonHolder.gson.toJson(map);
                default:
                    throw new IllegalArgumentException("Unknown JSON library: " + lib);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert map to JSON string", e);
        }
    }

    // Jackson and Gson holders for lazy singleton init
    private static class JacksonHolder {
        static final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    }
    private static class GsonHolder {
        static final com.google.gson.Gson gson = new com.google.gson.Gson();
    }

    private static Map<String, Object> structToMap(Struct struct, boolean bytesToBase64) {
        int size = struct.schema().fields().size();
        Map<String, Object> result = new HashMap<>(size * 4 / 3 + 1);
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

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public void close() {
        // nothing to close
    }
}
