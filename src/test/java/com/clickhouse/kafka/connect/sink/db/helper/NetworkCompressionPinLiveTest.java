package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseNodeSelector;
import com.clickhouse.client.ClickHouseProtocol;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.kafka.connect.sink.ClickHouseBase;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Live pin for D-17 against the default {@code latest} image: ClickHouse 26.9 answers {@code
 * compress=1} requests with ZSTD native blocks unless the connector forces {@code
 * network_compression_method=lz4}, and client 0.9.6 only decodes LZ4 ({@code Invalid LZ4 magic
 * byte: '-112'}, surfacing as {@code Connection to ClickHouse is not active}).
 */
class NetworkCompressionPinLiveTest extends ClickHouseBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkCompressionPinLiveTest.class);
    private static final String TYPED_QUERY =
            "SELECT toUInt8(1) AS n, version() AS v, getSetting('network_compression_method') AS m";

    private ClickHouseHelperClient buildClient(boolean useClientV2) {
        ClickHouseSinkConfig csc = new ClickHouseSinkConfig(getBaseProps());
        return new ClickHouseHelperClient.ClickHouseClientBuilder(
                        csc.getHostname(), csc.getPort(), csc.getProxyType(), csc.getProxyHost(), csc.getProxyPort())
                .setDatabase(csc.getDatabase())
                .setUsername(csc.getUsername())
                .setPassword(csc.getPassword())
                .sslEnable(csc.isSslEnabled())
                .setTimeout(csc.getTimeout())
                .setRetry(csc.getRetry())
                .useClientV2(useClientV2)
                .build();
    }

    @Test
    @DisplayName("D-17: V2 ping succeeds and a V2 query round-trips typed data with lz4 in effect")
    void v2PingAndQueryRoundTrip() throws Exception {
        try (ClickHouseHelperClient chc = buildClient(true)) {
            assertTrue(chc.ping());
            try (Records records = chc.queryV2(TYPED_QUERY)) {
                int rows = 0;
                for (GenericRecord record : records) {
                    rows++;
                    assertEquals(1, record.getInteger("n"));
                    assertNotNull(record.getString("v"));
                    assertEquals("lz4", record.getString("m"));
                    LOGGER.info("V2 server version: {}", record.getString("v"));
                }
                assertEquals(1, rows);
            }
        }
    }

    @Test
    @DisplayName("D-17: V1 ping succeeds and a V1 query round-trips typed data with lz4 in effect")
    void v1PingAndQueryRoundTrip() throws Exception {
        try (ClickHouseHelperClient chc = buildClient(false)) {
            assertTrue(chc.ping());
            try (ClickHouseClient client = ClickHouseClient.builder()
                            .options(chc.getDefaultClientOptions())
                            .nodeSelector(ClickHouseNodeSelector.of(ClickHouseProtocol.HTTP))
                            .build();
                    ClickHouseResponse response = client.read(chc.getServer())
                            .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
                            .query(TYPED_QUERY)
                            .executeAndWait()) {
                var record = response.firstRecord();
                assertEquals(1, record.getValue(0).asInteger());
                assertNotNull(record.getValue(1).asString());
                assertEquals("lz4", record.getValue(2).asString());
                LOGGER.info("V1 server version: {}", record.getValue(1).asString());
            }
        }
    }

    @Test
    @DisplayName("D-17: version() works on both clients against the shared container")
    void versionOnBothClients() {
        String expectedFormat = "^\\d+\\.\\d+\\.\\d+\\.\\d+$";
        try (ClickHouseHelperClient v2 = buildClient(true);
                ClickHouseHelperClient v1 = buildClient(false)) {
            String v2Version = v2.version();
            String v1Version = v1.version();
            LOGGER.info(
                    "Server version via V2: {}, via V1: {} (image: {})",
                    v2Version,
                    v1Version,
                    ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE);
            assertNotNull(v2Version);
            assertTrue(v2Version.matches(expectedFormat));
            assertEquals(v2Version, v1Version);
        }
    }
}
