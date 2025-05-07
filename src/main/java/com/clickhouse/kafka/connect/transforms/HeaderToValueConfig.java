package com.clickhouse.kafka.connect.transforms;

import java.util.List;
import java.util.Map;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

/**
 * Configuration for the HeaderToValue transformation.
 * <p>
 * This class manages the configuration options for mapping Kafka header values
 * to record value fields.
 */
public class HeaderToValueConfig extends AbstractConfig {
    
    // Configuration keys
    public static final String FIELDS_CONFIG = "fields";
    public static final String HEADERS_CONFIG = "headers";
    
    // Configuration definition
    private static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(FIELDS_CONFIG, 
                    ConfigDef.Type.LIST, 
                    ConfigDef.NO_DEFAULT_VALUE, 
                    ConfigDef.Importance.HIGH, 
                    "A comma-separated list of field names that the transform moves or copies to the record value.")
            .define(HEADERS_CONFIG, 
                    ConfigDef.Type.LIST, 
                    ConfigDef.NO_DEFAULT_VALUE, 
                    ConfigDef.Importance.HIGH, 
                    "A comma-separated list of header names. The number of headers listed must be the same as the number of fields listed. The headers listed must be in the same order as the fields listed.");
    
    /**
     * Creates a new configuration with the specified values.
     *
     * @param originals Map of configuration values
     */
    public HeaderToValueConfig(Map<String, ?> originals) {
        super(config(), originals);
    }
    
    /**
     * Returns the configuration definition.
     *
     * @return The ConfigDef for this transformation
     */
    public static ConfigDef config() {
        return CONFIG_DEF;
    }
    
    /**
     * Returns the list of field names to be created or overwritten.
     *
     * @return List of field names
     */
    public List<String> fields() {
        return getList(FIELDS_CONFIG);
    }
    
    /**
     * Returns the list of header names whose values will be copied.
     *
     * @return List of header names
     */
    public List<String> headers() {
        return getList(HEADERS_CONFIG);
    }
}
