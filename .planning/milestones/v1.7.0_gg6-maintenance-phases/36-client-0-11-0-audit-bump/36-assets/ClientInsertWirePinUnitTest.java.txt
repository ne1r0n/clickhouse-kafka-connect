package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.insert.InsertSettings;
import com.clickhouse.client.config.ClickHouseProxyType;
import com.clickhouse.data.ClickHouseFormat;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pin for CLT-06 at the wire: an insert replayed after a client change reaches the
 * server with the same body and the same {@code insert_deduplication_token}. The token has the
 * connector's {@code topic-partition-minOffset-maxOffset} shape. The only header allowed to differ
 * between client lines is {@code User-Agent}, and only in its client segment.
 */
class ClientInsertWirePinUnitTest {

    private static final String LOOPBACK = "127.0.0.1";
    private static final String DEDUP_TOKEN = "wire-topic-0-5-9";
    private static final String QUERY_ID = "wire-query-1";
    private static final String TOKEN_PARAMETER = "insert_deduplication_token=" + DEDUP_TOKEN;
    private static final String CLIENT_SEGMENT = " clickhouse-java-v2/";
    private static final int BODY_BYTES = 100_000;
    private static final long BODY_SEED = 36L;

    private static StubClickHouseServer.Request insertBody(byte[] body) throws Exception {
        try (StubClickHouseServer stub = new StubClickHouseServer(StubClickHouseServer.ok());
                ClickHouseHelperClient chc = new ClickHouseHelperClient.ClickHouseClientBuilder(
                                LOOPBACK, stub.port(), ClickHouseProxyType.IGNORE, null, -1)
                        .setDatabase("wire")
                        .useClientV2(true)
                        .build()) {
            InsertSettings settings = new InsertSettings().setQueryId(QUERY_ID).setDeduplicationToken(DEDUP_TOKEN);
            chc.getClient()
                    .insert("t", new ByteArrayInputStream(body), ClickHouseFormat.RowBinary, settings)
                    .get();
            assertEquals(1, stub.requests().size());
            return stub.requests().get(0);
        }
    }

    @Test
    @DisplayName("CLT-06: the insert body reaches the server byte for byte with the deduplication token in the query string and no Content-Encoding")
    void bodyAndTokenReachTheServerUnchanged() throws Exception {
        byte[] body = new byte[BODY_BYTES];
        new Random(BODY_SEED).nextBytes(body);
        StubClickHouseServer.Request request = insertBody(body);
        assertEquals("POST", request.method());
        assertTrue(URI.create(request.rawUri()).getRawQuery().contains(TOKEN_PARAMETER), request.rawUri());
        assertFalse(request.headers().containsKey("Content-Encoding"), request.headers().toString());
        assertArrayEquals(body, request.body());
    }

    @Test
    @DisplayName("CLT-06: the User-Agent names the connector and the resolved client-v2 version")
    void userAgentNamesTheConnectorAndTheClientVersion() throws Exception {
        String userAgent = insertBody(new byte[] {1}).headers().get("User-Agent");
        assertTrue(
                userAgent.startsWith(
                        ClickHouseHelperClient.CONNECT_CLIENT_NAME + CLIENT_SEGMENT + ClientErrorShapes.clientVersion()),
                userAgent);
    }
}
