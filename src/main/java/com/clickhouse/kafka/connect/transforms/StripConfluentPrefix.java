package com.clickhouse.kafka.connect.transforms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.transforms.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import static org.apache.kafka.connect.transforms.util.Requirements.requireSinkRecord;

public class StripConfluentPrefix<R extends ConnectRecord<R>> implements Transformation<R> {

    private static final Logger log = LoggerFactory.getLogger(StripConfluentPrefix.class);
    private static final byte MAGIC_BYTE = 0x0;
    private static final int SCHEMA_ID_SIZE = 4; // bytes
    private static final int PREFIX_SIZE = 1 + SCHEMA_ID_SIZE; // magic byte + schema ID

    private StripConfluentPrefixConfig config;
    // private Cache<Schema, Schema> schemaUpdateCache; // Currently unused, kept for potential future schema handling
    private ObjectMapper objectMapper;

    @Override
    public void configure(Map<String, ?> props) {
        this.config = new StripConfluentPrefixConfig(props);
        this.objectMapper = new ObjectMapper();
        // this.schemaUpdateCache = new SynchronizedCache<>(new LRUCache<>(16));
    }

    @Override
    public R apply(R record) {
        // requireSinkRecord(record, "StripConfluentPrefix"); // Primarily for sink connectors, but applicable logic

        if (record.value() == null) {
            log.trace("Record value is null, skipping transformation.");
            return record;
        }

        if (!(record.value() instanceof byte[])) {
            log.warn("Expected byte[] value, but got {}. Skipping transformation.", record.value().getClass().getName());
            return record;
        }

        byte[] valueBytes = (byte[]) record.value();

        if (valueBytes.length < PREFIX_SIZE) {
            log.warn("Value byte array length ({}) is less than the expected Confluent prefix size ({}), cannot strip prefix. Skipping transformation.", valueBytes.length, PREFIX_SIZE);
            return record;
        }

        ByteBuffer buffer = ByteBuffer.wrap(valueBytes);

        byte magic = buffer.get();
        if (magic != MAGIC_BYTE) {
            log.warn("Expected magic byte {} but found {}. Skipping transformation.", MAGIC_BYTE, magic);
            return record; // Or throw an exception, depending on desired strictness
        }

        int schemaId = buffer.getInt(); // Read the 4-byte schema ID

        // Extract the actual payload
        byte[] payloadBytes = Arrays.copyOfRange(valueBytes, PREFIX_SIZE, valueBytes.length);

        byte[] finalPayloadBytes = payloadBytes; // Start with the stripped payload

        // Optionally add schema ID to the JSON field
        if (config.addSchemaIdField()) {
            try {
                JsonNode rootNode = objectMapper.readTree(payloadBytes);
                if (rootNode.isObject()) {
                    ObjectNode objectNode = (ObjectNode) rootNode;
                    String fieldName = config.schemaIdFieldName();
                    objectNode.put(fieldName, schemaId);
                    log.trace("Adding schema ID {} as field '{}' to JSON object", schemaId, fieldName);
                    finalPayloadBytes = objectMapper.writeValueAsBytes(objectNode); // Update payload with modified JSON
                } else {
                    log.warn("Value is valid JSON but not a JSON object. Cannot add schema ID field '{}'. Skipping field addition.", config.schemaIdFieldName());
                }
            } catch (IOException e) {
                log.warn("Failed to parse JSON value or add schema ID field '{}'. Skipping field addition. Error: {}", config.schemaIdFieldName(), e.getMessage());
                // Keep original stripped payload (finalPayloadBytes already holds payloadBytes)
            }
        }

        R newRecord = record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                record.keySchema(),
                record.key(),
                null, // Value schema remains null as we output raw bytes
                finalPayloadBytes, // Use the potentially modified payload
                record.timestamp(),
                record.headers() // Keep original headers, add schema ID header below if needed
        );

        // Optionally add schema ID as a header (can be done in addition to adding it to the field)
        if (config.addSchemaIdHeader()) {
            String headerName = config.schemaIdHeaderName();
            log.trace("Adding schema ID {} as header '{}'", schemaId, headerName);
            newRecord.headers().addInt(headerName, schemaId);
        }

        return newRecord;
    }


    @Override
    public ConfigDef config() {
        return StripConfluentPrefixConfig.CONFIG_DEF;
    }

    @Override
    public void close() {
        // schemaUpdateCache.clear();
    }

}