package com.clickhouse.kafka.connect.transforms;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.transforms.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class HeaderToValue<R extends ConnectRecord<R>> implements Transformation<R> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderToValue.class.getName());

    private HeaderToValueConfig config;
    private Schema valueSchema;

    @Override
    public void configure(Map<String, ?> configs) {
        this.config = new HeaderToValueConfig(configs);
    }

    @Override
    public R apply(R record) {
        Header header = record.headers().lastWithName(config.headerName());

        if (header == null || header.value() == null) {
            if (config.skipMissingOrNull()) {
                return record;
            } else {
                throw new IllegalArgumentException("Header value is missing or null and skip.missing.or.null is false.");
            }
        }

        String headerStringValue = (String) header.value();

        if (record.valueSchema() == null) {
            return applySchemaless(record, headerStringValue);
        } else {
            return applyWithSchema(record, headerStringValue);
        }
    }

    private R applySchemaless(R record, String headerStringValue) {
        if (!(record.value() instanceof Map)) {
            throw new IllegalArgumentException("Schemaless record value must be a Map - make sure you're using the JSON Converter for value.");
        }

        final Map<String, Object> value = (Map<String, Object>) record.value();
        value.put(config.fieldName(), headerStringValue);
        return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(), record.key(), record.valueSchema(), value, record.timestamp());
    }

    private R applyWithSchema(R record, String headerStringValue) {
        final Struct oldValue = (Struct) record.value();

        if (valueSchema == null) {
            valueSchema = buildSchema(oldValue.schema());
        }

        Struct newValue = new Struct(valueSchema);
        for (Field field : valueSchema.fields()) {
            if (field.name().equals(config.fieldName())) {
                newValue.put(field, headerStringValue);
            } else {
                newValue.put(field, oldValue.get(field));
            }
        }
        return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(), record.key(), valueSchema, newValue, record.timestamp());
    }

    private Schema buildSchema(Schema oldSchema) {
        SchemaBuilder builder = SchemaBuilder.struct();
        builder.name(oldSchema.name());
        builder.version(oldSchema.version());
        builder.doc(oldSchema.doc());
        for (Field field : oldSchema.fields()) {
            builder.field(field.name(), field.schema());
        }
        builder.field(config.fieldName(), Schema.STRING_SCHEMA);
        return builder.build();
    }

    @Override
    public ConfigDef config() {
        return HeaderToValueConfig.config();
    }

    @Override
    public void close() {
        valueSchema = null;
    }
}