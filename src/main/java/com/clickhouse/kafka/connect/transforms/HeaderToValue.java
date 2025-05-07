package com.clickhouse.kafka.connect.transforms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
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
 * Transforms Kafka Connect records by copying header values into record values.
 * <p>
 * This transformation can:
 * <ul>
 *   <li>Add header values to schemaless record values (Maps)</li>
 *   <li>Add header values to schema-based record values (Structs)</li>
 * </ul>
 *
 * @param <R> The type of ConnectRecord
 */
public class HeaderToValue<R extends ConnectRecord<R>> implements Transformation<R> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderToValue.class);
    
    // Metrics for monitoring
    private final AtomicLong transformedRecords = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    
    // Configuration constants
    public static final String FIELDS_CONFIG = "fields";
    public static final String HEADERS_CONFIG = "headers";
    
    // Configuration state
    private List<String> fields;
    private List<String> headers;
    private Map<String, Integer> fieldToHeaderIdx;
    private Schema valueSchema;
    
    // Configuration definition
    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(FIELDS_CONFIG, 
                    ConfigDef.Type.LIST, 
                    ConfigDef.NO_DEFAULT_VALUE, 
                    ConfigDef.Importance.HIGH, 
                    "List of field names to create or overwrite with header values")
            .define(HEADERS_CONFIG, 
                    ConfigDef.Type.LIST, 
                    ConfigDef.NO_DEFAULT_VALUE, 
                    ConfigDef.Importance.HIGH, 
                    "List of header names to copy to record values (must match fields list length)");

    @Override
    public void configure(Map<String, ?> configs) {
        final AbstractConfig config = new AbstractConfig(CONFIG_DEF, configs);
        
        // Extract configuration values
        this.fields = config.getList(FIELDS_CONFIG);
        this.headers = config.getList(HEADERS_CONFIG);
        
        if (fields.size() != headers.size()) {
            throw new DataException("The number of fields (" + fields.size() + 
                                    ") must match the number of headers (" + headers.size() + ").");
        }
        
        // Cache indices for fast lookup
        this.fieldToHeaderIdx = new HashMap<>(fields.size() * 4 / 3 + 1); // Optimize initial capacity
        for (int i = 0; i < fields.size(); i++) {
            fieldToHeaderIdx.put(fields.get(i), i);
        }
        
        LOGGER.debug("Configured HeaderToValue with {} field-header mappings", fields.size());
    }

    @Override
    public R apply(R record) {
        try {
            if (record.value() == null) {
                return record;
            }
            
            R result;
            if (record.valueSchema() == null) {
                result = applySchemaless(record);
            } else {
                result = applyWithSchema(record);
            }
            
            transformedRecords.incrementAndGet();
            return result;
        } catch (Exception e) {
            errorCount.incrementAndGet();
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
        
        final Map<String, Object> value = new HashMap<>((Map<String, Object>) record.value());
        
        for (int i = 0; i < headers.size(); i++) {
            Header header = record.headers().lastWithName(headers.get(i));
            if (header != null && header.value() != null) {
                value.put(fields.get(i), header.value().toString());
            }
        }
        
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
        
        // Lazily initialize the schema
        if (valueSchema == null) {
            valueSchema = buildSchema(oldValue.schema());
        }
        
        Struct newValue = new Struct(valueSchema);
        
        // Copy all existing fields
        for (Field field : valueSchema.fields()) {
            String fieldName = field.name();
            Integer headerIdx = fieldToHeaderIdx.get(fieldName);
            
            if (headerIdx != null) {
                // Try to get value from header
                Header header = record.headers().lastWithName(headers.get(headerIdx));
                if (header != null && header.value() != null) {
                    newValue.put(fieldName, header.value().toString());
                    continue;
                }
            }
            
            // If field exists in old value, copy it
            if (oldValue.schema().field(fieldName) != null) {
                newValue.put(fieldName, oldValue.get(fieldName));
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
     * Builds a schema that includes all fields from the original schema
     * plus any new fields needed for headers.
     *
     * @param oldSchema The original schema
     * @return A new schema with all necessary fields
     */
    private Schema buildSchema(Schema oldSchema) {
        SchemaBuilder builder = SchemaBuilder.struct();
        
        // Copy schema metadata
        if (oldSchema.name() != null) {
            builder.name(oldSchema.name());
        }
        if (oldSchema.version() != null) {
            builder.version(oldSchema.version());
        }
        if (oldSchema.doc() != null) {
            builder.doc(oldSchema.doc());
        }
        
        // Track which fields have been added
        Set<String> added = new HashSet<>(oldSchema.fields().size() + fields.size());
        
        // Copy all existing fields
        for (Field field : oldSchema.fields()) {
            builder.field(field.name(), field.schema());
            added.add(field.name());
        }
        
        // Add new fields for headers
        for (String field : fields) {
            if (!added.contains(field)) {
                builder.field(field, Schema.OPTIONAL_STRING_SCHEMA);
                added.add(field);
            }
        }
        
        return builder.build();
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public void close() {
        LOGGER.debug("Closing HeaderToValue: processed {} records with {} errors", 
                transformedRecords.get(), errorCount.get());
        valueSchema = null;
        fieldToHeaderIdx = null;
    }
    
    /**
     * Returns metrics about the transformation operations.
     *
     * @return Map containing metric values
     */
    public Map<String, Object> metrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("transformed_records", transformedRecords.get());
        metrics.put("error_count", errorCount.get());
        return metrics;
    }
}
