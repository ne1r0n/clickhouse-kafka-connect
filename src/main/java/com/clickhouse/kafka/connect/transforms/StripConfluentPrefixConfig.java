package com.clickhouse.kafka.connect.transforms;

import java.util.Map;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

public class StripConfluentPrefixConfig extends AbstractConfig {

    public static final String ADD_SCHEMA_ID_HEADER_CONFIG = "add.schema.id.header";
    private static final String ADD_SCHEMA_ID_HEADER_DOC = "Whether to add the extracted Confluent Schema ID as a header.";
    private static final boolean ADD_SCHEMA_ID_HEADER_DEFAULT = false;

    public static final String SCHEMA_ID_HEADER_NAME_CONFIG = "schema.id.header.name";
    private static final String SCHEMA_ID_HEADER_NAME_DOC = "The name of the header to add the schema ID to, if enabled.";
    private static final String SCHEMA_ID_HEADER_NAME_DEFAULT = "confluentSchemaId";

    public static final String ADD_SCHEMA_ID_FIELD_CONFIG = "add.schema.id.field";
    private static final String ADD_SCHEMA_ID_FIELD_DOC = "Whether to add the extracted Confluent Schema ID as a field in the JSON value.";
    private static final boolean ADD_SCHEMA_ID_FIELD_DEFAULT = false;

    public static final String SCHEMA_ID_FIELD_NAME_CONFIG = "schema.id.field.name";
    private static final String SCHEMA_ID_FIELD_NAME_DOC = "The name of the field to add the schema ID to in the JSON value, if enabled.";
    private static final String SCHEMA_ID_FIELD_NAME_DEFAULT = "_confluentSchemaId";


    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(ADD_SCHEMA_ID_HEADER_CONFIG, Type.BOOLEAN, ADD_SCHEMA_ID_HEADER_DEFAULT, Importance.MEDIUM, ADD_SCHEMA_ID_HEADER_DOC)
            .define(SCHEMA_ID_HEADER_NAME_CONFIG, Type.STRING, SCHEMA_ID_HEADER_NAME_DEFAULT, Importance.MEDIUM, SCHEMA_ID_HEADER_NAME_DOC)
            .define(ADD_SCHEMA_ID_FIELD_CONFIG, Type.BOOLEAN, ADD_SCHEMA_ID_FIELD_DEFAULT, Importance.MEDIUM, ADD_SCHEMA_ID_FIELD_DOC)
            .define(SCHEMA_ID_FIELD_NAME_CONFIG, Type.STRING, SCHEMA_ID_FIELD_NAME_DEFAULT, Importance.MEDIUM, SCHEMA_ID_FIELD_NAME_DOC);

    public StripConfluentPrefixConfig(Map<?, ?> originals) {
        super(CONFIG_DEF, originals);
    }

    public boolean addSchemaIdHeader() {
        return getBoolean(ADD_SCHEMA_ID_HEADER_CONFIG);
    }

    public String schemaIdHeaderName() {
        return getString(SCHEMA_ID_HEADER_NAME_CONFIG);
    }

    public boolean addSchemaIdField() {
        return getBoolean(ADD_SCHEMA_ID_FIELD_CONFIG);
    }

    public String schemaIdFieldName() {
        return getString(SCHEMA_ID_FIELD_NAME_CONFIG);
    }
}