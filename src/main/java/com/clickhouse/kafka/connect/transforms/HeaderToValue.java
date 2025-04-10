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

import java.util.List;
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
        List<String> fields = config.fields();
        List<String> headers = config.headers();

        if (fields.size() != headers.size()) {
            throw new IllegalArgumentException("The number of fields must match the number of headers.");
        }

        if (record.valueSchema() == null) {
            return applySchemaless(record, fields, headers);
        } else {
            return applyWithSchema(record, fields, headers);
        }
    }

    private R applySchemaless(R record, List<String> fields, List<String> headers) {
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

    private R applyWithSchema(R record, List<String> fields, List<String> headers) {
        final Struct oldValue = (Struct) record.value();

        if (valueSchema == null) {
            valueSchema = buildSchema(oldValue.schema(), fields);
        }

        Struct newValue = new Struct(valueSchema);
        for (Field field : valueSchema.fields()) {
            if (fields.contains(field.name())) {
                int index = fields.indexOf(field.name());
                Header header = record.headers().lastWithName(headers.get(index));
                if (header != null && header.value() != null) {
                    newValue.put(field, header.value().toString());
                }
            } else {
                newValue.put(field, oldValue.get(field));
            }
        }
        return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(), record.key(), valueSchema, newValue, record.timestamp());
    }

    private Schema buildSchema(Schema oldSchema, List<String> fields) {
        SchemaBuilder builder = SchemaBuilder.struct();
        builder.name(oldSchema.name());
        builder.version(oldSchema.version());
        builder.doc(oldSchema.doc());
        for (Field field : oldSchema.fields()) {
            builder.field(field.name(), field.schema());
        }
        for (String field : fields) {
            builder.field(field, Schema.STRING_SCHEMA);
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
    }
}