package com.clickhouse.kafka.connect.transforms;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.transforms.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transforms Kafka Connect records by converting Struct values to Maps or JSON strings.
 * <p>
 * This transformation can:
 * <ul>
 *   <li>Convert Struct objects to Maps</li>
 *   <li>Convert byte arrays to Base64 strings (optional)</li>
 *   <li>Convert Maps to JSON strings (optional)</li>
 * </ul>
 *
 * @param <R> The type of ConnectRecord
 */
public class ConvertToMapOrJson<R extends ConnectRecord<R>> implements Transformation<R> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertToMapOrJson.class);
    
    // Metrics for monitoring
    private final AtomicLong transformedRecords = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    
    // Configuration constants
    public static final String BYTES_TO_BASE64_CONFIG = "bytes.to.base64";
    public static final boolean BYTES_TO_BASE64_DEFAULT = false;

    public static final String OUTPUT_JSON_ENABLED_CONFIG = "output.json.enabled";
    public static final boolean OUTPUT_JSON_ENABLED_DEFAULT = false;

    public static final String OUTPUT_JSON_LIBRARY_CONFIG = "output.json.library";
    public static final String OUTPUT_JSON_LIBRARY_DEFAULT = "jackson";
    
    public static final String PRESERVE_NULL_VALUES_CONFIG = "preserve.null.values";
    public static final boolean PRESERVE_NULL_VALUES_DEFAULT = true;
    
    // Configuration definition
    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(BYTES_TO_BASE64_CONFIG, 
                    ConfigDef.Type.BOOLEAN, 
                    BYTES_TO_BASE64_DEFAULT, 
                    ConfigDef.Importance.MEDIUM, 
                    "Convert byte arrays and ByteBuffers to Base64 strings")
            .define(OUTPUT_JSON_ENABLED_CONFIG, 
                    ConfigDef.Type.BOOLEAN, 
                    OUTPUT_JSON_ENABLED_DEFAULT, 
                    ConfigDef.Importance.MEDIUM, 
                    "Convert struct/map to JSON string")
            .define(OUTPUT_JSON_LIBRARY_CONFIG, 
                    ConfigDef.Type.STRING, 
                    OUTPUT_JSON_LIBRARY_DEFAULT, 
                    ConfigDef.ValidString.in("jackson", "gson"),
                    ConfigDef.Importance.LOW, 
                    "Library for JSON conversion: jackson, gson")
            .define(PRESERVE_NULL_VALUES_CONFIG,
                    ConfigDef.Type.BOOLEAN,
                    PRESERVE_NULL_VALUES_DEFAULT,
                    ConfigDef.Importance.LOW,
                    "Whether to include null values in the output map or JSON");

    // Configuration state
    private boolean bytesToBase64;
    private boolean outputJson;
    private boolean preserveNullValues;
    private JsonLibrary selectedLibrary;
    private Function<Map<?, ?>, String> jsonEncoder;

    // Reusable objects via ThreadLocal to avoid allocation overhead
    private static final ThreadLocal<java.util.Base64.Encoder> ENCODER =
            ThreadLocal.withInitial(java.util.Base64::getEncoder);

    // JSON library enum
    private enum JsonLibrary { JACKSON, GSON }

    @Override
    public void configure(Map<String, ?> configs) {
        final AbstractConfig config = new AbstractConfig(CONFIG_DEF, configs);
        
        // Extract configuration values
        bytesToBase64 = config.getBoolean(BYTES_TO_BASE64_CONFIG);
        outputJson = config.getBoolean(OUTPUT_JSON_ENABLED_CONFIG);
        preserveNullValues = config.getBoolean(PRESERVE_NULL_VALUES_CONFIG);
        
        String lib = config.getString(OUTPUT_JSON_LIBRARY_CONFIG).toLowerCase();
        try {
            selectedLibrary = JsonLibrary.valueOf(lib.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DataException("Invalid JSON library: " + lib + ". Must be one of: jackson, gson", e);
        }
        
        // Initialize the appropriate JSON encoder based on selected library
        initializeJsonEncoder();
        
        LOGGER.debug("Configured ConvertToMapOrJson with bytesToBase64={}, outputJson={}, jsonLibrary={}",
                bytesToBase64, outputJson, selectedLibrary);
    }

    private void initializeJsonEncoder() {
        if (!outputJson) {
            return;
        }
        
        switch (selectedLibrary) {
            case JACKSON -> {
                ObjectWriter writer = createJacksonWriter();
                jsonEncoder = map -> {
                    try {
                        return writer.writeValueAsString(map);
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        throw new DataException("Jackson JSON serialization failed", e);
                    }
                };
            }
            case GSON -> {
                Gson gson = createGsonInstance();
                jsonEncoder = map -> {
                    try {
                        return gson.toJson(map);
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        throw new DataException("Gson JSON serialization failed", e);
                    }
                };
            }
            default -> throw new DataException("Unsupported JSON library: " + selectedLibrary);
        }
    }
    
    private ObjectWriter createJacksonWriter() {
        ObjectMapper mapper = JacksonHolder.getMapper().copy();
        if (!preserveNullValues) {
            mapper = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
        return mapper.writer();
    }
    
    private Gson createGsonInstance() {
        GsonBuilder builder = new GsonBuilder();
        if (preserveNullValues) {
            builder.serializeNulls();
        }
        return builder.create();
    }

    @Override
    public R apply(R record) {
        Object value = record.value();
        if (value == null) {
            return record;
        }
        
        Object transformedValue;
        
        try {
            if (value instanceof Struct struct) {
                Map<String, Object> mapValue = structToMap(struct);
                transformedValue = outputJson ? jsonEncoder.apply(mapValue) : mapValue;
            } else if (value instanceof Map<?, ?> map && outputJson) {
                transformedValue = jsonEncoder.apply(map);
            } else if (value instanceof String && outputJson) {
                // Already a string, no transformation needed if we're expecting JSON output
                transformedValue = value;
            } else {
                // No transformation needed
                return record;
            }
            
            transformedRecords.incrementAndGet();
            
            return record.newRecord(
                    record.topic(),
                    record.kafkaPartition(),
                    record.keySchema(),
                    record.key(),
                    null, // Schema is null since we're converting to a Map or JSON string
                    transformedValue,
                    record.timestamp(),
                    record.headers()
            );
        } catch (Exception e) {
            errorCount.incrementAndGet();
            LOGGER.error("Error transforming record: {}", e.getMessage(), e);
            throw new DataException("Failed to transform record", e);
        }
    }

    /**
     * Converts a Struct to a Map recursively.
     *
     * @param struct The Struct to convert
     * @return A Map representation of the Struct
     */
    private Map<String, Object> structToMap(Struct struct) {
        Schema schema = struct.schema();
        int size = schema.fields().size();
        Map<String, Object> result = new LinkedHashMap<>(size * 4 / 3 + 1); // Optimize initial capacity
        
        for (Field field : schema.fields()) {
            String fieldName = field.name();
            Object value = struct.get(field);
            
            if (value == null) {
                if (preserveNullValues) {
                    result.put(fieldName, null);
                }
                continue;
            }
            
            Object convertedValue = convertFieldValue(value);
            result.put(fieldName, convertedValue);
        }
        
        return result;
    }
    
    /**
     * Converts a field value based on its type.
     *
     * @param value The field value to convert
     * @return The converted value
     */
    private Object convertFieldValue(Object value) {
        if (value instanceof Struct nestedStruct) {
            return structToMap(nestedStruct);
        }
        
        if (bytesToBase64) {
            if (value instanceof byte[] bytes) {
                return ENCODER.get().encodeToString(bytes);
            }
            
            if (value instanceof ByteBuffer buffer) {
                return encodeByteBuffer(buffer);
            }
        }
        
        return value;
    }
    
    /**
     * Efficiently encodes a ByteBuffer to a Base64 string.
     *
     * @param buffer The ByteBuffer to encode
     * @return Base64 encoded string
     */
    private String encodeByteBuffer(ByteBuffer buffer) {
        byte[] bytes;
        
        // Avoid unnecessary byte array creation when possible
        if (buffer.hasArray() && buffer.arrayOffset() == 0 && buffer.limit() == buffer.array().length) {
            bytes = buffer.array();
        } else {
            ByteBuffer duplicate = buffer.duplicate();
            bytes = new byte[duplicate.remaining()];
            duplicate.get(bytes);
        }
        
        return ENCODER.get().encodeToString(bytes);
    }

    /**
     * Thread-safe holder for Jackson ObjectMapper.
     */
    private static class JacksonHolder {
        private static final ObjectMapper MAPPER = new ObjectMapper();
        
        public static ObjectMapper getMapper() {
            return MAPPER;
        }
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public void close() {
        LOGGER.debug("Closing ConvertToMapOrJson: processed {} records with {} errors", 
                transformedRecords.get(), errorCount.get());
        ENCODER.remove();
    }
    
    /**
     * Returns metrics about the transformation operations.
     *
     * @return Map containing metric values
     */
    public Map<String, Object> metrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("transformed_records", transformedRecords.get());
        metrics.put("error_count", errorCount.get());
        return metrics;
    }
}