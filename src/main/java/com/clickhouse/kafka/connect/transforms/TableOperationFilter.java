package com.clickhouse.kafka.connect.transforms;

import com.clickhouse.kafka.connect.util.jmx.MBeanServerUtils;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.transforms.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters out specified operation types for designated tables in Debezium change events.
 * <p>
 * This transformation enables selective filtering of operations (CREATE, UPDATE, DELETE, TRUNCATE)
 * based on table names, while allowing other operations to pass through. This is useful
 * for scenarios where different tables require different CDC event handling.
 * <p>
 * For example, some tables may need all operations (INSERT, UPDATE, DELETE) while
 * others may only require a subset of operations (e.g., only INSERT and UPDATE).
 *
 * @param <R> The type of ConnectRecord
 */
public class TableOperationFilter<R extends ConnectRecord<R>> implements Transformation<R>, TableOperationFilterMBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(TableOperationFilter.class);
    private static final AtomicLong NEXT_ID = new AtomicLong();
    
    // Metrics for monitoring
    private final AtomicLong filteredRecords = new AtomicLong(0);
    private final AtomicLong processedRecords = new AtomicLong(0);
    private final long id;
    private String connectorName;

    public TableOperationFilter() {
        this.id = NEXT_ID.getAndIncrement();
    }

    @Override
    public long getProcessedRecords() {
        return processedRecords.get();
    }

    @Override
    public long getFilteredRecords() {
        return filteredRecords.get();
    }
    
    // Configuration constants
    public static final String TABLES_CONFIG = "tables";
    public static final String TABLES_DEFAULT = "";
    
    public static final String SKIPPED_OPERATIONS_CONFIG = "skipped.operations";
    public static final String SKIPPED_OPERATIONS_DEFAULT = "d";
    
    // Configuration definition
    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(TABLES_CONFIG, 
                    ConfigDef.Type.STRING, 
                    TABLES_DEFAULT, 
                    ConfigDef.Importance.HIGH, 
                    "Comma-separated list of tables to filter operations from in format 'database.table'")
            .define(SKIPPED_OPERATIONS_CONFIG, 
                    ConfigDef.Type.STRING, 
                    SKIPPED_OPERATIONS_DEFAULT, 
                    ConfigDef.Importance.HIGH, 
                    "Comma-separated list of operations to skip: c (create/insert), u (update), d (delete), t (truncate)");
    
    // Configuration state
    private Set<String> targetTables = new HashSet<>();
    private Set<String> skippedOperations = new HashSet<>();
    
    private String getMBeanName() {
        return String.format(
            "com.clickhouse.kafka.connect.transforms:type=TableOperationFilter,connector=%s,task=%d",
            connectorName, id
        );
    }

    @Override
    public void configure(Map<String, ?> configs) {
        final AbstractConfig config = new AbstractConfig(CONFIG_DEF, configs);
        
        // Extract configuration values
        String tablesStr = config.getString(TABLES_CONFIG);
        String operationsStr = config.getString(SKIPPED_OPERATIONS_CONFIG);
        
        // Get connector name from configs
        Object nameConfig = configs.get("name");
        this.connectorName = nameConfig != null ? nameConfig.toString() : "unknown";
        
        // Register MBean
        MBeanServerUtils.registerMBean(this, getMBeanName());
        
        // Parse and store the list of tables
        if (!tablesStr.isEmpty()) {
            String[] tables = tablesStr.split(",");
            for (String table : tables) {
                targetTables.add(table.trim());
            }
        }
        
        // Parse and store the operations to skip
        if (!operationsStr.isEmpty()) {
            String[] operations = operationsStr.split(",");
            for (String operation : operations) {
                String op = operation.trim().toLowerCase();
                if (op.length() == 1 && "cudt".contains(op)) {
                    skippedOperations.add(op);
                } else {
                    LOGGER.warn("Invalid operation type '{}' specified in skipped.operations. " +
                            "Valid values are: c, u, d, t", op);
                }
            }
        }
        
        LOGGER.debug("Configured TableOperationFilter with targetTables={}, skippedOperations={}",
                targetTables, skippedOperations);
    }

    @Override
    public R apply(R record) {
        if (record.value() == null) {
            return record;
        }
        
        processedRecords.incrementAndGet();
        
        try {
            // Check if this is a Debezium change event with proper structure
            if (!(record.value() instanceof Struct)) {
                return record;
            }
            
            Struct value = (Struct) record.value();
            
            // Check if this record has the expected Debezium format with 'op' field
            if (!hasField(value, "op")) {
                return record;
            }
            
            String op = value.getString("op");
            
            // If operation is not in our list of operations to check, pass through
            if (!skippedOperations.contains(op)) {
                return record;
            }
            
            // For skipped operations, check if we should filter based on table name
            if (!hasField(value, "source")) {
                return record;
            }
            
            Struct source = value.getStruct("source");
            if (!hasField(source, "db") || !hasField(source, "table")) {
                return record;
            }
            
            // Extract the full table name (db.table)
            String db = source.getString("db");
            String table = source.getString("table");
            String fullTableName = db + "." + table;
            
            // Check if this operation should be filtered for this table
            boolean shouldFilter = targetTables.contains(fullTableName);
            
            if (shouldFilter) {
                String operationName = getOperationName(op);
                LOGGER.trace("Filtering {} operation for table: {}", operationName, fullTableName);
                filteredRecords.incrementAndGet();
                return null; // Return null to remove the record from the stream
            }
            
            return record;
            
        } catch (Exception e) {
            LOGGER.error("Error processing record in TableOperationFilter: {}", e.getMessage(), e);
            throw new DataException("Failed to process record in TableOperationFilter", e);
        }
    }
    
    /**
     * Safely checks if a Struct has a specific field.
     *
     * @param struct The Struct to check
     * @param fieldName The field name to look for
     * @return true if the field exists, false otherwise
     */
    private boolean hasField(Struct struct, String fieldName) {
        return struct.schema() != null && struct.schema().field(fieldName) != null;
    }
    
    /**
     * Converts operation code to readable name for logging.
     *
     * @param op Operation code (c, u, d, t)
     * @return Human-readable operation name
     */
    private String getOperationName(String op) {
        return switch(op) {
            case "c" -> "CREATE";
            case "u" -> "UPDATE";
            case "d" -> "DELETE";
            case "t" -> "TRUNCATE";
            default -> op;
        };
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public void close() {
        MBeanServerUtils.unregisterMBean(getMBeanName());
        LOGGER.debug("Closing TableOperationFilter: processed {} records, filtered {} operations", 
                processedRecords.get(), filteredRecords.get());
    }
    
    /**
     * Returns metrics about the filtering operations.
     *
     * @return Map containing metric values
     */
    public Map<String, Object> metrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("processed_records", processedRecords.get());
        metrics.put("filtered_records", filteredRecords.get());
        return metrics;
    }
}
