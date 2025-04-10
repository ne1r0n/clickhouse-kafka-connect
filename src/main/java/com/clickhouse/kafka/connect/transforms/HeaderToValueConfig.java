package com.clickhouse.kafka.connect.transforms;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

class HeaderToValueConfig extends AbstractConfig {
    public static final String FIELDS_CONFIG = "fields";
    private static final String FIELDS_DOC = "A comma-separated list of field names that the transform moves or copies to the record value.";

    public static final String HEADERS_CONFIG = "headers";
    private static final String HEADERS_DOC = "A comma-separated list of header names. The number of headers listed must be the same as the number of fields listed. The headers listed must be in the same order as the fields listed.";

    HeaderToValueConfig(final Map<?, ?> originals) {
        super(config(), originals);
    }

    static ConfigDef config() {
        return new ConfigDef()
                .define(FIELDS_CONFIG, ConfigDef.Type.LIST, ConfigDef.Importance.HIGH, FIELDS_DOC)
                .define(HEADERS_CONFIG, ConfigDef.Type.LIST, ConfigDef.Importance.HIGH, HEADERS_DOC);
    }

    List<String> fields() {
        return getList(FIELDS_CONFIG);
    }

    List<String> headers() {
        return getList(HEADERS_CONFIG);
    }
}