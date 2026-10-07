package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.client.config.ClickHouseClientOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pin for D-17: every client the connector builds sends {@code
 * network_compression_method=lz4}. ClickHouse 26.9 defaults that setting to ZSTD, which client
 * 0.9.6 cannot decode, so a sync that silently drops the setting breaks the connector against
 * every 26.9+ server. Deliberately extends nothing - extending {@code ClickHouseBase} would
 * inherit {@code @Tag("clickhouse")} and exclude this class from {@code testUnit}.
 *
 * <p>Clients are built against an unreachable endpoint: construction never contacts the server,
 * so only configuration is asserted.
 */
class NetworkCompressionPinUnitTest {
    private static final String SETTING = "network_compression_method";
    private static final String V2_SETTING_KEY = "clickhouse_setting_" + SETTING;
    private static final String OPERATOR_VALUE = "zstd";

    private static ClickHouseHelperClient buildClient(String jdbcConnectionProperties) {
        return new ClickHouseHelperClient.ClickHouseClientBuilder("localhost", 1, null, null, -1)
                .setJdbcConnectionProperties(jdbcConnectionProperties)
                .useClientV2(true)
                .build();
    }

    @Test
    @DisplayName("D-17: V2 client carries server setting network_compression_method=lz4")
    void v2ClientCarriesLz4() {
        try (ClickHouseHelperClient chc = buildClient("")) {
            assertEquals("lz4", chc.getClient().getConfiguration().get(V2_SETTING_KEY));
        }
    }

    @Test
    @DisplayName("D-17: V1 default client options carry network_compression_method=lz4")
    void v1DefaultOptionsCarryLz4() {
        try (ClickHouseHelperClient chc = buildClient("")) {
            assertEquals(
                    SETTING + "=lz4",
                    chc.getDefaultClientOptions().get(ClickHouseClientOption.CUSTOM_SETTINGS));
        }
    }

    @Test
    @DisplayName("D-17: an operator's explicit server setting overrides the connector default on V2")
    void operatorValueWinsOnV2() {
        try (ClickHouseHelperClient chc = buildClient("?" + V2_SETTING_KEY + "=" + OPERATOR_VALUE)) {
            assertEquals(OPERATOR_VALUE, chc.getClient().getConfiguration().get(V2_SETTING_KEY));
        }
    }

    @Test
    @DisplayName("D-17: the connector default still applies when the operator sets other options")
    void unrelatedOperatorOptionsKeepLz4() {
        try (ClickHouseHelperClient chc = buildClient("?socket_timeout=12345")) {
            assertEquals("lz4", chc.getClient().getConfiguration().get(V2_SETTING_KEY));
            assertEquals("12345", chc.getClient().getConfiguration().get("socket_timeout"));
        }
    }
}
