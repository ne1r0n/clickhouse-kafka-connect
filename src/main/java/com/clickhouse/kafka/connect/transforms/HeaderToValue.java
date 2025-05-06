package com.clickhouse.kafka.connect.transforms;

import java.util.List;
import java.util.Map;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.transforms.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeaderToValue<R extends ConnectRecord<R>> implements Transformation<R> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderToValue.class.getName());

    private HeaderToValueConfig config;
    private Schema valueSchema;
    private List<String> fields;
    private List<String> headers;
    private Map<String, Integer> fieldToHeaderIdx;

    @Override
    public void configure(Map<String, ?> configs) {
        this.config = new HeaderToValueConfig(configs);
        this.fields = config.fields();
        this.headers = config.headers();
        if (fields.size() != headers.size()) {
            throw new IllegalArgumentException("The number of fields must match the number of headers.");
        }
        // Cache indices for fast access
        this.fieldToHeaderIdx = new java.util.HashMap<>(fields.size());
        for (int i = 0; i < fields.size(); i++) {
            fieldToHeaderIdx.put(fields.get(i), i);
        }
    }

    @Override
    public R apply(R record) {
        if (fields.size() != headers.size()) {
            throw new IllegalArgumentException("The number of fields must match the number of headers.");
        }
        if (record.valueSchema() == null) {
            return applySchemaless(record);
        } else {
            return applyWithSchema(record);
        }
    }

    private R applySchemaless(R record) {
        if (!(record.value() instanceof Map)) {
            throw new IllegalArgumentException("Schemaless record value must be a Map - make sure you're using the JSON Converter for value.");
        }
        final Map<String, Object> value = (Map<String, Object>) record.value();
        for (int i = 0; i < headers.size(); i++) {
            Header header = record.headers().lastWithName(headers.get(i));
            if (header != null && header.value() != null) {
                value.put(fields.get(i), header.value().toString());
            }
        }
        return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(), record.key(), record.valueSchema(), value, record.timestamp());
    }

    private R applyWithSchema(R record) {
        final Struct oldValue = (Struct) record.value();
        if (valueSchema == null) {
            valueSchema = buildSchema(oldValue.schema());
        }
        Struct newValue = new Struct(valueSchema);
        for (Field field : valueSchema.fields()) {
            Integer idx = fieldToHeaderIdx.get(field.name());
            if (idx != null) {
                Header header = record.headers().lastWithName(headers.get(idx));
                if (header != null && header.value() != null) {
                    newValue.put(field, header.value().toString());
                    continue;
                }
            }
            newValue.put(field, oldValue.get(field));
        }
        return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(), record.key(), valueSchema, newValue, record.timestamp());
    }

    private Schema buildSchema(Schema oldSchema) {
        SchemaBuilder builder = SchemaBuilder.struct();
        builder.name(oldSchema.name());
        builder.version(oldSchema.version());
        builder.doc(oldSchema.doc());
        java.util.Set<String> added = new java.util.HashSet<>();
        for (Field field : oldSchema.fields()) {
            builder.field(field.name(), field.schema());
            added.add(field.name());
        }
        for (String field : fields) {
            if (!added.contains(field)) {
                builder.field(field, Schema.STRING_SCHEMA);
            }
        }
        return builder.build();
    }

    @Override
    public ConfigDef config() {
        return HeaderToValueConfig.config();
    }

    @Override
    public void close() {
        valueSchema = null;
        fieldToHeaderIdx = null;
    }
}