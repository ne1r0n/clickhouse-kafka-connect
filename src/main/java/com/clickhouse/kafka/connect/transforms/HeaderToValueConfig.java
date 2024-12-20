package com.clickhouse.kafka.connect.transforms;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;

class HeaderToValueConfig extends AbstractConfig {
    public static final String FIELD_NAME_CONFIG = "field";
    private static final String FIELD_NAME_DOC = "Field name on the record value to extract the header into.";

    public static final String SKIP_MISSING_OR_NULL_CONFIG = "skip.missing.or.null";
    private static final String SKIP_MISSING_OR_NULL_DOC = "In case the header is null or missing, should a record be silently passed without transformation.";

    public static final String HEADER_NAME_CONFIG = "header";
    private static final String HEADER_NAME_DOC = "Header name to extract value from.";

    HeaderToValueConfig(final Map<?, ?> originals) {
        super(config(), originals);
    }

    static ConfigDef config() {
        return new ConfigDef()
                .define(FIELD_NAME_CONFIG, ConfigDef.Type.STRING, "_header", ConfigDef.Importance.LOW, FIELD_NAME_DOC)
                .define(SKIP_MISSING_OR_NULL_CONFIG, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.LOW, SKIP_MISSING_OR_NULL_DOC)
                .define(HEADER_NAME_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, HEADER_NAME_DOC);
    }

    String fieldName() {
        return getString(FIELD_NAME_CONFIG);
    }

    boolean skipMissingOrNull() {
        return getBoolean(SKIP_MISSING_OR_NULL_CONFIG);
    }

    String headerName() {
        return getString(HEADER_NAME_CONFIG);
    }
}