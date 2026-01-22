package com.clickhouse.kafka.connect.transforms;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.transforms.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transforms Kafka Connect records by copying all header values into record values
 * under a single field.
 *
 * @param <R> The type of ConnectRecord
 */
public class HeadersToValue<R extends ConnectRecord<R>> implements Transformation<R> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeadersToValue.class);

    // Configuration constants
    public static final String FIELD_CONFIG = "field";

    // Configuration definition
    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(FIELD_CONFIG,
                    ConfigDef.Type.STRING,
                    ConfigDef.NO_DEFAULT_VALUE,
                    ConfigDef.Importance.HIGH,
                    "Field name to store all record headers as a map");

    // Configuration state
    private String fieldName;
    private Schema valueSchema;
    private Schema headersSchema;

    @Override
    public void configure(Map<String, ?> configs) {
        final AbstractConfig config = new AbstractConfig(CONFIG_DEF, configs);

        String configuredField = config.getString(FIELD_CONFIG);
        if (configuredField == null || configuredField.trim().isEmpty()) {
            throw new DataException("Field name for headers map must be specified and non-empty.");
        }
        fieldName = configuredField.trim();
        headersSchema = SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.OPTIONAL_STRING_SCHEMA)
                .optional()
                .build();

        LOGGER.debug("Configured HeadersToValue with field name '{}'", fieldName);
    }

    @Override
    public R apply(R record) {
        try {
            if (record.value() == null) {
                return record;
            }
            if (record.valueSchema() == null) {
                return applySchemaless(record);
            }
            return applyWithSchema(record);
        } catch (Exception e) {
            LOGGER.error("Error transforming record: topic={}, partition={}, error={}",
                    record.topic(), record.kafkaPartition(), e.getMessage(), e);
            throw new DataException("Failed to transform record", e);
        }
    }

    /**
     * Apply transformation to schemaless records (values are Maps).
     *
     * @param record The record to transform
     * @return The transformed record
     */
    private R applySchemaless(R record) {
        if (!(record.value() instanceof Map)) {
            throw new DataException("Schemaless record value must be a Map - make sure you're using the JSON Converter for value.");
        }

        @SuppressWarnings("unchecked")
        final Map<String, Object> value = new HashMap<>((Map<String, Object>) record.value());
        value.put(fieldName, extractHeaders(record));

        return record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                record.keySchema(),
                record.key(),
                record.valueSchema(),
                value,
                record.timestamp()
        );
    }

    /**
     * Apply transformation to schema-based records (values are Structs).
     *
     * @param record The record to transform
     * @return The transformed record
     */
    private R applyWithSchema(R record) {
        final Struct oldValue = (Struct) record.value();

        if (valueSchema == null) {
            valueSchema = buildSchema(oldValue.schema());
        }

        Struct newValue = new Struct(valueSchema);

        for (Field field : valueSchema.fields()) {
            String name = field.name();
            if (name.equals(fieldName)) {
                newValue.put(name, extractHeaders(record));
            } else if (oldValue.schema().field(name) != null) {
                newValue.put(name, oldValue.get(name));
            }
        }

        return record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                record.keySchema(),
                record.key(),
                valueSchema,
                newValue,
                record.timestamp()
        );
    }

    /**
     * Extracts headers into a map of header name to string value.
     *
     * @param record The record to extract headers from
     * @return Map of header values
     */
    private Map<String, String> extractHeaders(R record) {
        int size = record.headers().size();
        Map<String, String> headers = new LinkedHashMap<>(size * 4 / 3 + 1);
        for (Header header : record.headers()) {
            Object value = header.value();
            headers.put(header.key(), value == null ? null : value.toString());
        }
        return headers;
    }

    /**
     * Builds a schema that includes all fields from the original schema
     * plus the headers map field if missing.
     *
     * @param oldSchema The original schema
     * @return A new schema with the headers map field
     */
    private Schema buildSchema(Schema oldSchema) {
        SchemaBuilder builder = SchemaBuilder.struct();

        if (oldSchema.name() != null) {
            builder.name(oldSchema.name());
        }
        if (oldSchema.version() != null) {
            builder.version(oldSchema.version());
        }
        if (oldSchema.doc() != null) {
            builder.doc(oldSchema.doc());
        }

        for (Field field : oldSchema.fields()) {
            builder.field(field.name(), field.schema());
        }

        if (oldSchema.field(fieldName) == null) {
            builder.field(fieldName, headersSchema);
        }

        return builder.build();
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public void close() {
        LOGGER.debug("Closing HeadersToValue");
        valueSchema = null;
        headersSchema = null;
    }
}
