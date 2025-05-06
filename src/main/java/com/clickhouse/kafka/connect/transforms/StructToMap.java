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

public class StructToMap<R extends ConnectRecord<R>> implements Transformation<R> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructToMap.class);

    public static final String BYTES_TO_BASE64_CONFIG = "bytes.to.base64";
    public static final boolean BYTES_TO_BASE64_DEFAULT = false;
    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(BYTES_TO_BASE64_CONFIG, ConfigDef.Type.BOOLEAN, BYTES_TO_BASE64_DEFAULT, ConfigDef.Importance.LOW, "Convert bytes fields to base64 strings");

    private boolean bytesToBase64 = BYTES_TO_BASE64_DEFAULT;

    // ThreadLocal encoder for multithreaded usage
    private static final ThreadLocal<java.util.Base64.Encoder> ENCODER =
            ThreadLocal.withInitial(java.util.Base64::getEncoder);

    @Override
    public void configure(Map<String, ?> configs) {
        Object param = configs.get(BYTES_TO_BASE64_CONFIG);
        bytesToBase64 = param instanceof Boolean ? (Boolean) param
                : param instanceof String ? Boolean.parseBoolean((String) param)
                : BYTES_TO_BASE64_DEFAULT;
    }

    @Override
    public R apply(R record) {
        Object value = record.value();
        if (value instanceof Struct struct) {
            Map<String, Object> mapValue = structToMap(struct, bytesToBase64);
            return record.newRecord(
                    record.topic(),
                    record.kafkaPartition(),
                    record.keySchema(),
                    record.key(),
                    null, // schemaless
                    mapValue,
                    record.timestamp(),
                    record.headers()
            );
        }
        return record;
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
