package com.clickhouse.kafka.connect.transforms;

import java.util.Map;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.transforms.Transformation;

public class DropValueSchema<R extends ConnectRecord<R>> implements Transformation<R> {
    public static final ConfigDef CONFIG_DEF = new ConfigDef();

    @Override
    public void configure(Map<String, ?> configs) {
        // No-op: no parameters to configure
    }

    @Override
    public R apply(R record) {
        // Just drop value schema, keep everything else
        return record.newRecord(
            record.topic(),
            record.kafkaPartition(),
            record.keySchema(),
            record.key(),
            null, // drop value schema
            record.value(),
            record.timestamp(),
            record.headers()
        );
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
