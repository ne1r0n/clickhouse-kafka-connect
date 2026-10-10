package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free unit pins for CFG-03: pins all thirteen of upstream v1.6.0's new {@link
 * ClickHouseSinkConfig} options at their upstream defaults, for a config built from a props map
 * holding only hostname/port/database/username/password - none of the thirteen keys is present.
 *
 * <p>Twelve of the thirteen options are getter-backed and asserted directly against the
 * constructed config's getter. The thirteenth, {@code errors.retry.timeout} ({@link
 * ClickHouseSinkConfig#CONNECTOR_RETRY_TIMEOUT}), has no {@code private final} field, no
 * Lombok-generated getter, and no {@code configDef.define} entry at all (PD-02) - it is covered
 * instead by asserting its key constant's exact string and that construction with the key absent
 * completes without throwing.
 *
 * <p>Purpose: fail loudly, not silently, if a future upstream sync drops one of these thirteen
 * {@code configDef.define} blocks. New fork-only class: it deliberately extends nothing, so it
 * stays in {@code testUnit} - extending {@code ClickHouseBase} would inherit its {@code
 * @Tag("clickhouse")} and drop this class out of the Docker-free loop.
 */
class NewUpstreamConfigDefaultsUnitTest {

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
    @DisplayName(
            "CFG-03: the twelve getter-backed new upstream config options reach their getters at upstream defaults when unset")
    void twelveGetterBackedOptionsReachUpstreamDefaults() {
        Map<String, String> props = minimalProps();

        // Edge case (D-15/CFG-03): none of the thirteen keys is present in the props map - this
        // is exactly the case a silently-dropped configDef.define on a future sync would change
        // without this test noticing. Naming every key constant here also guarantees an option
        // cannot be omitted from the pin without the file itself failing to name it.
        assertFalse(props.containsKey(ClickHouseSinkConfig.AUTO_EVOLVE));
        assertFalse(props.containsKey(ClickHouseSinkConfig.AUTO_EVOLVE_DDL_REFRESH_RETRIES));
        assertFalse(props.containsKey(ClickHouseSinkConfig.AUTO_EVOLVE_STRUCT_TO_JSON));
        assertFalse(props.containsKey(ClickHouseSinkConfig.BUFFER_COUNT));
        assertFalse(props.containsKey(ClickHouseSinkConfig.BUFFER_FLUSH_TIME));
        assertFalse(props.containsKey(ClickHouseSinkConfig.CLICKHOUSE_CLIENT_INSERT_TIMEOUT_MS));
        assertFalse(props.containsKey(ClickHouseSinkConfig.CLUSTER_NAME));
        assertFalse(props.containsKey(ClickHouseSinkConfig.DEBEZIUM_CDC_ENABLED));
        assertFalse(props.containsKey(ClickHouseSinkConfig.ENABLE_REPLICA_PINNING));
        assertFalse(props.containsKey(ClickHouseSinkConfig.REPORT_INSERTED_OFFSETS));
        assertFalse(props.containsKey(ClickHouseSinkConfig.RETRY_ON_SOCKET_EXCEPTION));
        assertFalse(props.containsKey(ClickHouseSinkConfig.SSL_SOCKET_SNI));
        assertFalse(props.containsKey(ClickHouseSinkConfig.CONNECTOR_RETRY_TIMEOUT));

        ClickHouseSinkConfig csc = new ClickHouseSinkConfig(props);

        // auto.evolve family: no public default constant exists for these three - upstream's
        // constructor reads inline literal defaults directly, so the test asserts the literals.
        assertFalse(csc.isAutoEvolve());
        assertEquals(3, csc.getAutoEvolveDdlRefreshRetries());
        assertFalse(csc.isAutoEvolveStructToJson());

        // Options with a named public default constant: assert against the constant, never a
        // re-typed literal, so a future change to the constant surfaces as a deliberate decision.
        assertEquals(ClickHouseSinkConfig.bufferCountDefault.intValue(), csc.getBufferCount());
        assertEquals(ClickHouseSinkConfig.bufferFlushTimeDefault.longValue(), csc.getBufferFlushTime());
        assertEquals(
                ClickHouseSinkConfig.clickhouseClientInsertTimeoutMsDefault.longValue(),
                csc.getClickhouseClientInsertTimeoutMs());
        assertEquals(ClickHouseSinkConfig.reportInsertedOffsetsDefault, csc.isReportInsertedOffsets());
        assertEquals(ClickHouseSinkConfig.enableReplicaPinningDefault, csc.isEnableReplicaPinning());

        // Options whose default is an inline literal in the constructor with no named constant.
        assertEquals("", csc.getClusterName());
        assertFalse(csc.isDebeziumCDCEnabled());
        assertFalse(csc.isRetryOnSocketException());
        assertEquals("", csc.getSslSocketSni());
    }

    @Test
    @DisplayName(
            "CFG-03/PD-02: errors.retry.timeout has no getter and no configDef entry; its key constant is exact and construction with the key absent completes without throwing or a warning-path failure")
    void retryTimeoutKeyIsExactAndConstructionWithoutItSucceeds() {
        assertEquals("errors.retry.timeout", ClickHouseSinkConfig.CONNECTOR_RETRY_TIMEOUT);
        assertDoesNotThrow(() -> new ClickHouseSinkConfig(minimalProps()));
    }

    @Test
    @DisplayName(
            "CFG-03: the ordering edge - constructing from the minimal props map raises no configuration exception, so this phase's two new configDef.define blocks did not collide with upstream's existing definitions")
    void constructionRaisesNoConfigurationException() {
        assertNotNull(ClickHouseSinkConfig.CONFIG);
        assertDoesNotThrow(() -> new ClickHouseSinkConfig(minimalProps()));
    }

    @Test
    @DisplayName(
            "CFG-03 / CMP-01: the fourteenth upstream option setCompressionMethod defaults to true, with canonical key and snake_case alias")
    void setCompressionMethodFourteenthOptionDefaultsTrueWithCanonicalKeyAndAlias() {
        Map<String, String> props = minimalProps();
        assertFalse(props.containsKey(ClickHouseSinkConfig.SET_COMPRESSION_METHOD));

        ClickHouseSinkConfig csc = new ClickHouseSinkConfig(props);
        assertTrue(csc.isSetCompressionMethod());
        assertEquals(ClickHouseSinkConfig.setCompressionMethodDefault, csc.isSetCompressionMethod());
        assertEquals("setCompressionMethod", ClickHouseSinkConfig.SET_COMPRESSION_METHOD);

        Map<String, String> canonical = minimalProps();
        canonical.put(ClickHouseSinkConfig.SET_COMPRESSION_METHOD, "false");
        assertFalse(new ClickHouseSinkConfig(canonical).isSetCompressionMethod());

        Map<String, String> alias = minimalProps();
        alias.put("set_compression_method", "false");
        assertFalse(new ClickHouseSinkConfig(alias).isSetCompressionMethod());

        assertEquals(
                true,
                ClickHouseSinkConfig.CONFIG
                        .configKeys()
                        .get(ClickHouseSinkConfig.SET_COMPRESSION_METHOD)
                        .defaultValue);
    }
}
