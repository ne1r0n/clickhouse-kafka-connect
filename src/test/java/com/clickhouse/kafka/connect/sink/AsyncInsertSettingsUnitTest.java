package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free unit pins for ASYNC-01 and ASYNC-02: the fork no longer forces {@code
 * async_insert=0} on every connector - upstream's forced-default line
 * ({@code this.addClickHouseSetting("async_insert", "0", false);}) is commented out, so the
 * whole change is observable directly in the settings map the constructor builds. New fork-only
 * class (D-16): it deliberately extends nothing, so it stays in {@code testUnit} - extending
 * {@code ClickHouseBase} would inherit its {@code @Tag("clickhouse")} and drop this class out of
 * the Docker-free loop (Phase 1 D-12).
 */
class AsyncInsertSettingsUnitTest {

    private static Map<String, String> minimalProps() {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConfig.HOSTNAME, "localhost");
        props.put(ClickHouseSinkConfig.PORT, "8123");
        props.put(ClickHouseSinkConfig.DATABASE, "default");
        props.put(ClickHouseSinkConfig.USERNAME, "default");
        props.put(ClickHouseSinkConfig.PASSWORD, "");
        return props;
    }

    @Test
    @DisplayName("ASYNC-01: a default config sends no async_insert setting, and still sends the other forced settings")
    void defaultConfigHasNoAsyncInsertButHasTheOtherForcedSettings() {
        ClickHouseSinkConfig csc = new ClickHouseSinkConfig(minimalProps());
        Map<String, String> settings = csc.getClickhouseSettings();

        assertFalse(settings.containsKey("async_insert"), settings.toString());

        // Positive control: without asserting the OTHER forced settings are present, the negative
        // assertion above would still pass against a config that built no settings at all, which
        // would certify nothing about the fork's actual change.
        assertTrue(settings.containsKey("input_format_skip_unknown_fields"), settings.toString());
        assertTrue(settings.containsKey("wait_end_of_query"), settings.toString());
        assertTrue(settings.containsKey("send_progress_in_http_headers"), settings.toString());
    }

    @Test
    @DisplayName("ASYNC-02: user-supplied async_insert=1 and wait_for_async_insert=1 both round-trip to exactly \"1\"")
    void userSuppliedAsyncInsertAndWaitForAsyncInsertRoundTripToOne() {
        Map<String, String> props = minimalProps();
        props.put(ClickHouseSinkConfig.CLICKHOUSE_SETTINGS, "async_insert=1,wait_for_async_insert=1");
        ClickHouseSinkConfig csc = new ClickHouseSinkConfig(props);
        Map<String, String> settings = csc.getClickhouseSettings();

        assertEquals("1", settings.get("async_insert"));
        assertEquals("1", settings.get("wait_for_async_insert"));
    }

    @Test
    @DisplayName("ASYNC-02: user-supplied async_insert=0 round-trips to exactly \"0\" (verbatim pass-through, not \"any truthy value\")")
    void userSuppliedAsyncInsertZeroRoundTripsToZero() {
        Map<String, String> props = minimalProps();
        props.put(ClickHouseSinkConfig.CLICKHOUSE_SETTINGS, "async_insert=0");
        ClickHouseSinkConfig csc = new ClickHouseSinkConfig(props);
        Map<String, String> settings = csc.getClickhouseSettings();

        assertEquals("0", settings.get("async_insert"));
    }
}
