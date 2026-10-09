package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.insert.InsertResponse;
import com.clickhouse.client.api.insert.InsertSettings;
import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.kafka.connect.sink.ClickHouseBase;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Live LZ4 round trip for BLD-10. Since Phase 12 the connector's own clients never run lz4-java,
 * because of {@code compress=false} and {@code compressServerResponse(false)}. Only a client that
 * compresses both legs proves that client-v2 0.9.6 works with the catalog's lz4-java, so this test
 * owns its client instead of using {@code ClickHouseHelperClient}.
 *
 * <p>It sets {@code network_compression_method=lz4} because ClickHouse 26.9 answers {@code
 * compress=1} with ZSTD (Recorded Facts 25 and 26). It names no lz4-java version, so it guards
 * every later lz4 or client bump unchanged. It pins the jar origin because {@code
 * testRuntimeClasspath} also carries the old {@code org.lz4} artifact, which provides the same
 * {@code net.jpountz.lz4} classes (14-RESEARCH Pitfall 2).
 */
class Lz4RoundTripLiveTest extends ClickHouseBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(Lz4RoundTripLiveTest.class);

    private static final String NETWORK_COMPRESSION_METHOD = "network_compression_method";
    private static final String LZ4 = "lz4";
    private static final String MAINTAINED_LZ4_JAR_MARKER = "at.yawk.lz4";
    private static final int ROWS = 2000;
    private static final int PAYLOAD_REPEATS = 60;
    private static final long MIN_BODY_BYTES = 2L * 1024 * 1024;
    private static final int MIN_PAYLOAD_BYTES = 1024;
    private static final Instant BASE_INSTANT = Instant.parse("2026-01-01T00:00:00Z");
    private static final DateTimeFormatter DATE_TIME_64_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

    private Client buildLz4Client() {
        ClickHouseSinkConfig csc = new ClickHouseSinkConfig(getBaseProps());
        String protocol = csc.isSslEnabled() ? "https" : "http";
        return new Client.Builder()
                .addEndpoint(String.format("%s://%s:%d", protocol, csc.getHostname(), csc.getPort()))
                .setUsername(csc.getUsername())
                .setPassword(csc.getPassword())
                .setDefaultDatabase(csc.getDatabase())
                .compressClientRequest(true)
                .compressServerResponse(true)
                .useHttpCompression(false)
                .serverSetting(NETWORK_COMPRESSION_METHOD, LZ4)
                .build();
    }

    private static String payload(int id) {
        return ("payload-" + id + "-lorem ipsum dolor sit amet ").repeat(PAYLOAD_REPEATS);
    }

    private static BigDecimal amount(int id) {
        return new BigDecimal(id + ".2500");
    }

    private static Instant timestamp(int id) {
        return BASE_INSTANT.plusMillis(id);
    }

    private static byte[] buildJsonEachRowBody() {
        StringBuilder sb = new StringBuilder();
        for (int id = 1; id <= ROWS; id++) {
            String payload = payload(id);
            assertTrue(payload.getBytes(StandardCharsets.UTF_8).length >= MIN_PAYLOAD_BYTES);
            sb.append("{\"id\":")
                    .append(id)
                    .append(",\"payload\":\"")
                    .append(payload)
                    .append("\",\"amount\":")
                    .append(amount(id).toPlainString())
                    .append(",\"ts\":\"")
                    .append(DATE_TIME_64_FORMAT.format(timestamp(id)))
                    .append("\"}\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("BLD-10: net.jpountz.lz4 loads from the maintained at.yawk.lz4 jar")
    void lz4FactoryLoadsFromTheMaintainedFork() {
        URL location = net.jpountz.lz4.LZ4Factory.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();
        LOGGER.info("LZ4Factory code source: {}", location);
        assertNotNull(location);
        assertTrue(
                location.toString().contains(MAINTAINED_LZ4_JAR_MARKER),
                "LZ4Factory must come from the at.yawk.lz4 jar, but was loaded from " + location);
    }

    @Test
    @DisplayName("BLD-10: client-v2 inserts and reads back over LZ4 on both legs")
    void insertAndReadBackOverLz4OnBothLegs() throws Exception {
        String table = createTopicName("lz4_round_trip");
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        try (Client client = buildLz4Client()) {
            ClickHouseTestHelpers.executeQueryIgnoreResult(
                    chc,
                    String.format(
                            "CREATE TABLE `%s` (id UInt32, payload String, amount Decimal(18, 4), "
                                    + "ts DateTime64(3, 'UTC')) ENGINE = MergeTree ORDER BY id",
                            table));

            GenericRecord setting =
                    client.queryAll("SELECT getSetting('" + NETWORK_COMPRESSION_METHOD + "') AS m, version() AS v")
                            .get(0);
            assertEquals(LZ4, setting.getString("m"));
            LOGGER.info("{}={}, server version {}", NETWORK_COMPRESSION_METHOD, setting.getString("m"), setting.getString("v"));

            byte[] body = buildJsonEachRowBody();
            assertTrue(body.length >= MIN_BODY_BYTES, "request body too small to span several LZ4 blocks: " + body.length);
            LOGGER.info("Inserting {} rows, {} bytes", ROWS, body.length);

            String queryId = UUID.randomUUID().toString();
            try (InsertResponse ignored = client.insert(
                            table,
                            new ByteArrayInputStream(body),
                            ClickHouseFormat.JSONEachRow,
                            new InsertSettings().setQueryId(queryId))
                    .get()) {
                // inserted
            }

            List<GenericRecord> rows = client.queryAll(
                    String.format("SELECT id, payload, amount, ts FROM `%s` ORDER BY id", table));
            assertEquals(ROWS, rows.size());
            for (int i = 0; i < ROWS; i++) {
                int id = i + 1;
                GenericRecord record = rows.get(i);
                assertEquals(id, record.getLong("id"));
                assertEquals(payload(id), record.getString("payload"));
                BigDecimal expected = amount(id);
                BigDecimal actual = record.getBigDecimal("amount");
                assertEquals(
                        0,
                        expected.compareTo(actual),
                        "amount mismatch for id " + id + ": expected " + expected + " but was " + actual);
                // client-v2 0.9.6 getInstant() casts a DateTime64 with a timezone to LocalDateTime and throws
                assertEquals(timestamp(id), record.getZonedDateTime("ts").toInstant());
            }

            client.execute("SYSTEM FLUSH LOGS").get().close();
            List<GenericRecord> logRows = client.queryAll(String.format(
                    "SELECT toInt64(ProfileEvents['CompressedReadBufferBlocks']) AS blocks "
                            + "FROM system.query_log WHERE query_id = '%s' AND type = 'QueryFinish'",
                    queryId));
            assertEquals(1, logRows.size());
            long blocks = logRows.get(0).getLong("blocks");
            LOGGER.info("CompressedReadBufferBlocks for the insert: {}", blocks);
            assertTrue(blocks > 0, "the insert request body was not read as natively compressed blocks");
        } finally {
            ClickHouseTestHelpers.dropTable(chc, table);
            chc.close();
        }
    }
}
