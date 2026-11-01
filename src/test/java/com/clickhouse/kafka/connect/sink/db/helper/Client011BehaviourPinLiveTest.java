package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseNodeSelector;
import com.clickhouse.client.ClickHouseProtocol;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.client.api.query.QuerySettings;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.kafka.connect.sink.ClickHouseBase;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.util.Utils;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.connect.errors.RetriableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Live pins of the behaviours the client 0.11.0 bump changes (CLT-04), against the real server. An
 * expectation that differs between the client lines is keyed by the resolved client-v2 version, so
 * the class stays green if the bump is reverted and fails on a client line it has no row for.
 */
class Client011BehaviourPinLiveTest extends ClickHouseBase {

    private static final String ZSTD_ROWS_QUERY =
            "SELECT getSetting('network_compression_method') AS m, number AS n, repeat('x', 100) AS s FROM numbers(2000)";
    private static final int ZSTD_ROWS = 2000;
    private static final String SLOW_QUERY = "SELECT count() FROM numbers(100000000000)";
    private static final String MAX_EXECUTION_TIME = "max_execution_time";
    private static final String ONE_SECOND = "1";
    private static final String V1_ONLY_PROPERTIES = "?ssl=true&sslmode=strict";
    private static final String COMPRESS_REQUEST = "?compress=true";
    private static final String LZ4_MAGIC_FAILURE = "Invalid LZ4 magic byte";
    private static final String V1_MAGIC_FAILURE = "Magic is not correct";

    private ClickHouseHelperClient build(boolean setCompressionMethod, String jdbcConnectionProperties) {
        ClickHouseSinkConfig csc = new ClickHouseSinkConfig(getBaseProps());
        return new ClickHouseHelperClient.ClickHouseClientBuilder(
                        csc.getHostname(), csc.getPort(), csc.getProxyType(), csc.getProxyHost(), csc.getProxyPort())
                .setDatabase(csc.getDatabase())
                .setUsername(csc.getUsername())
                .setPassword(csc.getPassword())
                .sslEnable(csc.isSslEnabled())
                .setTimeout(csc.getTimeout())
                .setRetry(csc.getRetry())
                .useClientV2(true)
                .setCompressionMethod(setCompressionMethod)
                .setJdbcConnectionProperties(jdbcConnectionProperties)
                .build();
    }

    @Test
    @DisplayName("CLT-04: a real server error 159 reaches Utils.handleException as a RetriableException")
    void timeoutExceededIsRetriable() throws Exception {
        try (ClickHouseHelperClient chc = build(true, "")) {
            Exception thrown = assertThrows(Exception.class, () -> chc.getClient()
                    .query(SLOW_QUERY, new QuerySettings().serverSetting(MAX_EXECUTION_TIME, ONE_SECOND))
                    .get());
            assertThrows(
                    RetriableException.class,
                    () -> Utils.handleException(thrown, false, false, Set.of(), new ArrayList<>()));
        }
    }

    @Test
    @DisplayName("CLT-04: a 502 and a 503 answered to the real client keep their pinned shapes with and without retryOnSocketException")
    void gatewayStatusesKeepTheirShapes() throws Exception {
        ClientErrorShapes.assertBadGateway();
        ClientErrorShapes.assertServiceUnavailable();
    }

    @Test
    @DisplayName("CLT-04: a ClientException and an unexpected status are never retriable with or without retryOnSocketException")
    void clientExceptionIsNeverRetriable() throws Exception {
        ClientErrorShapes.assertClientExceptionNeverRetriable();
        ClientErrorShapes.assertUnexpectedStatus();
    }

    @Test
    @DisplayName("CLT-04: ssl=true and sslmode=strict still ping and query a real server on V2")
    void v1OnlyJdbcPropertiesStillConnect() {
        try (ClickHouseHelperClient chc = build(true, V1_ONLY_PROPERTIES)) {
            assertTrue(chc.ping());
            assertNotNull(chc.version());
        }
    }

    @Test
    @DisplayName("CLT-04: V2 decodes ZSTD responses with setCompressionMethod false and compress true on 0.11.0 and fails the known way on 0.9.6")
    void zstdResponseDecodes() throws Exception {
        try (ClickHouseHelperClient chc = build(false, COMPRESS_REQUEST)) {
            if (ClientErrorShapes.line() == ClientErrorShapes.Line.V0_11_0) {
                try (Records records = chc.queryV2(ZSTD_ROWS_QUERY)) {
                    int rows = 0;
                    for (GenericRecord record : records) {
                        rows++;
                        assertEquals("ZSTD", record.getString("m"));
                    }
                    assertEquals(ZSTD_ROWS, rows);
                }
            } else {
                RuntimeException e = assertThrows(RuntimeException.class, () -> {
                    try (Records records = chc.queryV2(ZSTD_ROWS_QUERY)) {
                        records.forEach(r -> {});
                    }
                });
                Throwable root = e;
                while (root.getCause() != null) {
                    root = root.getCause();
                }
                assertTrue(root.getMessage().contains(LZ4_MAGIC_FAILURE), root.getMessage());
            }
        }
    }

    @Test
    @DisplayName("CLT-05: client V1 still cannot decode a ZSTD response on either client line")
    void v1StillCannotDecodeZstd() throws Exception {
        ClickHouseSinkConfig csc = new ClickHouseSinkConfig(getBaseProps());
        String url = String.format("http://%s:%d/%s", csc.getHostname(), csc.getPort(), csc.getDatabase());
        ClickHouseNode node = ClickHouseNode.of(
                url, Map.of("compress", "true", "user", csc.getUsername(), "password", csc.getPassword()));
        Exception e = assertThrows(Exception.class, () -> {
            try (ClickHouseClient client = ClickHouseClient.builder()
                            .nodeSelector(ClickHouseNodeSelector.of(ClickHouseProtocol.HTTP))
                            .build();
                    ClickHouseResponse response = client.read(node)
                            .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
                            .query(ZSTD_ROWS_QUERY)
                            .executeAndWait()) {
                response.records().forEach(r -> {});
            }
        });
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertTrue(root.getMessage().contains(V1_MAGIC_FAILURE), root.getMessage());
    }
}
