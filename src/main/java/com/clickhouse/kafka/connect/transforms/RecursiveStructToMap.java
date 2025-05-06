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

public class RecursiveStructToMap<R extends ConnectRecord<R>> implements Transformation<R> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecursiveStructToMap.class);

    public static final String BYTES_TO_BASE64_CONFIG = "bytes.to.base64";
    public static final boolean BYTES_TO_BASE64_DEFAULT = false;
    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(BYTES_TO_BASE64_CONFIG, ConfigDef.Type.BOOLEAN, BYTES_TO_BASE64_DEFAULT, ConfigDef.Importance.LOW, "Convert bytes fields to base64 strings");

    private boolean bytesToBase64 = BYTES_TO_BASE64_DEFAULT;

    @Override
    public void configure(Map<String, ?> configs) {
        Object param = configs.get(BYTES_TO_BASE64_CONFIG);
        if (param instanceof Boolean) {
            bytesToBase64 = (Boolean) param;
        } else if (param instanceof String) {
            bytesToBase64 = Boolean.parseBoolean((String) param);
        }
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
        Map<String, Object> result = new HashMap<>();
        for (Field field : struct.schema().fields()) {
            Object value = struct.get(field);
            if (value instanceof Struct nested) {
                result.put(field.name(), structToMap(nested, bytesToBase64));
            } else if (value instanceof byte[] bytes) {
                if (bytesToBase64) {
                    result.put(field.name(), java.util.Base64.getEncoder().encodeToString(bytes));
                } else {
                    result.put(field.name(), bytes);
                }
            } else if (value instanceof java.nio.ByteBuffer buf) {
                if (bytesToBase64) {
                    byte[] bytes;
                    if (buf.hasArray()) {
                        bytes = buf.array();
                    } else {
                        bytes = new byte[buf.remaining()];
                        buf.duplicate().get(bytes);
                    }
                    result.put(field.name(), java.util.Base64.getEncoder().encodeToString(bytes));
                } else {
                    result.put(field.name(), buf);
                }
            } else {
                result.put(field.name(), value);
            }
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
