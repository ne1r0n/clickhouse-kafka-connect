package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.config.types.Password;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pin for CFG-04: the {@code username} ConfigDef default must agree with the runtime
 * default. Deliberately extends nothing - extending {@link ClickHouseBase} would inherit {@code
 * @Tag("clickhouse")} and drop the class out of {@code testUnit}.
 */
class ClickHouseSinkConfigUsernameDefaultUnitTest {

    private static final String CONFIG_DEF_DISPLAY_NOTE =
            "Connect's config validation and REST API display the ConfigDef default, while the runtime reads usernameDefault";

    private static Map<String, String> propsWithoutUsername() {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConfig.HOSTNAME, "localhost");
        props.put(ClickHouseSinkConfig.PORT, "8123");
        props.put(ClickHouseSinkConfig.DATABASE, "default");
        props.put(ClickHouseSinkConfig.PASSWORD, "");
        return props;
    }

    @Test
    @DisplayName("CFG-04: the USERNAME ConfigDef default is \"default\" and equals usernameDefault")
    void usernameConfigDefIsTheRuntimeDefault() {
        Object configDefDefault = ClickHouseSinkConfig.CONFIG
                .configKeys()
                .get(ClickHouseSinkConfig.USERNAME)
                .defaultValue;

        assertEquals("default", configDefDefault, CONFIG_DEF_DISPLAY_NOTE);
        assertEquals(ClickHouseSinkConfig.usernameDefault, configDefDefault, CONFIG_DEF_DISPLAY_NOTE);
    }

    @Test
    @DisplayName("CFG-04: props without a username still resolve to \"default\" at runtime")
    void runtimeUsernameDefaultIsUnchanged() {
        ClickHouseSinkConfig config = new ClickHouseSinkConfig(propsWithoutUsername());

        assertEquals(
                "default",
                config.getUsername(),
                "The runtime default must stay \"default\"; only the ConfigDef display value changed");
    }

    @Test
    @DisplayName("CFG-04: the PASSWORD ConfigDef default is still the empty string")
    void passwordConfigDefDefaultIsStillEmpty() {
        Object defaultValue = ClickHouseSinkConfig.CONFIG
                .configKeys()
                .get(ClickHouseSinkConfig.PASSWORD)
                .defaultValue;

        Password password = assertInstanceOf(Password.class, defaultValue);
        assertEquals(ClickHouseSinkConfig.passwordDefault, password.value(), "PASSWORD must keep its empty default");
    }
}
