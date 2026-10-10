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
import com.clickhouse.kafka.connect.ClickHouseSinkConnector;
import com.clickhouse.kafka.connect.sink.ClickHouseBase;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkTask;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.sink.SinkRecord;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Live pin for CMP-01/CMP-02 against the default {@code latest} image (D-17 retired in Phase
 * 12). ClickHouse 26.9 answers {@code compress=1} requests with ZSTD native blocks unless
 * {@code network_compression_method=lz4} is in effect, and client 0.9.6 only decodes LZ4 ({@code
 * Invalid LZ4 magic byte: '-112'}, surfacing as {@code Connection to ClickHouse is not active}).
 * With the connector's controls in effect (upstream's {@code setCompressionMethod}, default true,
 * adds the lz4 setting, and {@code compress=false} is set unconditionally), queries round-trip on
 * both clients. Against 26.9.12.8, {@code compress=false} alone suffices on both clients, and the
 * lz4 setting alone suffices on V2 when an operator re-enables response compression; removing both
 * on V2 with {@code compress=true} fails with the magic-byte error above. An operator's {@code
 * compress=true} in {@code jdbcConnectionProperties} re-enables response compression on V2
 * (Recorded Facts 25 and 26).
 */
class NetworkCompressionPinLiveTest extends ClickHouseBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkCompressionPinLiveTest.class);
    private static final String TYPED_QUERY =
            "SELECT toUInt8(1) AS n, version() AS v, getSetting('network_compression_method') AS m";

    // lz4hc frames blocks with the same LZ4 method byte, so client 0.9.6 still decodes it
    private static final String OPERATOR_METHOD = "lz4hc";

    private ClickHouseHelperClient buildClient(boolean useClientV2) {
        return buildClient(useClientV2, "");
    }

    private ClickHouseHelperClient buildClient(boolean useClientV2, String jdbcConnectionProperties) {
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
                .setJdbcConnectionProperties(jdbcConnectionProperties)
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
    @DisplayName("D-17: an operator's V2 server setting overrides the lz4 default on the wire")
    void operatorOverrideReachesServerOnV2() throws Exception {
        try (ClickHouseHelperClient chc =
                buildClient(true, "?clickhouse_setting_network_compression_method=" + OPERATOR_METHOD)) {
            try (Records records = chc.queryV2(TYPED_QUERY)) {
                for (GenericRecord record : records) {
                    assertEquals(OPERATOR_METHOD, record.getString("m"));
                }
            }
        }
    }

    @Test
    @DisplayName("D-17: an operator's V1 custom_settings overrides the lz4 default and merges other keys")
    void operatorOverrideReachesServerOnV1() throws Exception {
        String customSettings = "custom_settings=network_compression_method%3D" + OPERATOR_METHOD + ",max_threads%3D3";
        try (ClickHouseHelperClient chc = buildClient(false, "?" + customSettings);
                ClickHouseClient client = ClickHouseClient.builder()
                        .options(chc.getDefaultClientOptions())
                        .nodeSelector(ClickHouseNodeSelector.of(ClickHouseProtocol.HTTP))
                        .build();
                ClickHouseResponse response = client.read(chc.getServer())
                        .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
                        .query("SELECT getSetting('network_compression_method'), toString(getSetting('max_threads'))")
                        .executeAndWait()) {
            var record = response.firstRecord();
            assertEquals(OPERATOR_METHOD, record.getValue(0).asString());
            assertEquals("3", record.getValue(1).asString());
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

    @ParameterizedTest(name = "client={0} setCompressionMethod={1}")
    @CsvSource({"V2,true", "V2,false", "V1,true", "V1,false"})
    @DisplayName("CMP-03: a sink-task insert reads back intact with setCompressionMethod true and false on V2 and V1")
    void sinkTaskInsertAndReadBackWithSetCompressionMethod(
            String clientVersion, boolean setCompressionMethod, TestInfo testInfo) throws Exception {
        String tableName = createTopicName("cmp03_" + clientVersion.toLowerCase() + "_" + setCompressionMethod);
        Map<String, String> props = getBaseProps();
        // Read-back client is built before the overrides so reading never depends on the variable under test.
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        try {
            ClickHouseTestHelpers.executeQueryIgnoreResult(
                    chc, String.format("CREATE TABLE `%s` (id UInt32, str String) ENGINE = MergeTree ORDER BY id", tableName));
            props.put(ClickHouseSinkConnector.CLIENT_VERSION, clientVersion);
            props.put(ClickHouseSinkConfig.SET_COMPRESSION_METHOD, String.valueOf(setCompressionMethod));
            props.put("name", uniqueConnectorName(testInfo));

            List<SinkRecord> records = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("id", i);
                value.put("str", "cmp03-" + i);
                records.add(new SinkRecord(tableName, 0, null, null, null, value, i));
            }

            ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
            sinkTask.start(props);
            try {
                sinkTask.put(records);
            } finally {
                sinkTask.stop();
            }

            List<JSONObject> rows = new ArrayList<>(ClickHouseTestHelpers.getAllRowsAsJson(chc, tableName));
            rows.sort(Comparator.comparingInt(row -> row.getInt("id")));
            assertEquals(5, rows.size());
            for (int i = 0; i < 5; i++) {
                int id = i + 1;
                assertEquals(id, rows.get(i).getInt("id"));
                assertEquals("cmp03-" + id, rows.get(i).getString("str"));
            }
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
            chc.close();
        }
    }
}
