package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.ClientMisconfigurationException;
import com.clickhouse.client.api.insert.InsertSettings;
import com.clickhouse.client.config.ClickHouseProxyType;
import com.clickhouse.data.ClickHouseFormat;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pins for CLT-03: the V2 client is built with {@code protocol://host:port} only, so the
 * request path stays "/", and {@code jdbcConnectionProperties} keys that only client V1 knows
 * ({@code ssl}, {@code sslmode}) do not stop the client from being built or reach the server.
 */
class ClientEndpointAndOptionsPinUnitTest {

    private static final String DATABASE = "pin_db";
    private static final String DATABASE_HEADER = "X-ClickHouse-Database";
    private static final String V1_ONLY_PROPERTIES = "?ssl=true&sslmode=strict";
    private static final String OTHER_V1_ONLY_PROPERTIES =
            "?load_balancing_policy=random&health_check_interval=5000&failover=2";
    private static final String IGNORE_KEY = "ignore_unknown_config_key";
    private static final String LOOPBACK = "127.0.0.1";
    private static final String CUSTOM_KEYS_PROPERTIES = "?custom_settings=max_threads%3D3&custom_myparam=7&ssl=true";
    private static final String CUSTOM_SPACE_PROPERTY = "?custom_a=b%20c";
    private static final String CUSTOM_PREFIX = "custom_";
    private static final String CLIENT_VERSION_TRIPWIRE =
            "client-version tripwire: this pins what the ClickHouse client does with custom_* keys of"
                    + " jdbcConnectionProperties; when the client changes, re-read docs/ROLLOUT-v1.7.0_gg6.md row D7"
                    + " and change this expectation on purpose. ";

    private static StubClickHouseServer.Request insertOnce(String jdbcConnectionProperties) throws Exception {
        try (StubClickHouseServer stub = new StubClickHouseServer(StubClickHouseServer.ok());
                ClickHouseHelperClient chc = new ClickHouseHelperClient.ClickHouseClientBuilder(
                                LOOPBACK, stub.port(), ClickHouseProxyType.IGNORE, null, -1)
                        .setDatabase(DATABASE)
                        .useClientV2(true)
                        .setJdbcConnectionProperties(jdbcConnectionProperties)
                        .build()) {
            chc.getClient()
                    .insert("t", new ByteArrayInputStream(new byte[] {1, 2, 3}), ClickHouseFormat.RowBinary, new InsertSettings())
                    .get();
            assertEquals(1, stub.requests().size());
            return stub.requests().get(0);
        }
    }

    /** Raw (still percent-encoded) query values; 0.11.0 does not keep the order of query parameters stable. */
    private static Map<String, String> rawQueryMap(String rawUri) {
        Map<String, String> query = new TreeMap<>();
        String rawQuery = URI.create(rawUri).getRawQuery();
        if (rawQuery == null) {
            return query;
        }
        for (String pair : rawQuery.split("&")) {
            String[] keyAndValue = pair.split("=", 2);
            query.put(keyAndValue[0], keyAndValue.length > 1 ? keyAndValue[1] : "");
        }
        return query;
    }

    @Test
    @DisplayName("CLT-03: the request path is slash and the database travels in the X-ClickHouse-Database header")
    void endpointHasNoPath() throws Exception {
        StubClickHouseServer.Request request = insertOnce("");
        assertEquals("/", URI.create(request.rawUri()).getPath());
        assertEquals(DATABASE, request.headers().get(DATABASE_HEADER));
    }

    @Test
    @DisplayName("CLT-03: ssl=true and sslmode=strict build a working V2 client and never reach the server")
    void v1OnlyPropertiesAreAccepted() throws Exception {
        StubClickHouseServer.Request request = insertOnce(V1_ONLY_PROPERTIES);
        assertEquals("/", URI.create(request.rawUri()).getPath());
        assertFalse(request.rawUri().contains("sslmode"), request.rawUri());
        assertFalse(request.rawUri().contains(IGNORE_KEY), request.rawUri());
    }

    @Test
    @DisplayName("CLT-03: the V1-only keys of the live jdbcConnectionProperties pins are accepted too")
    void otherV1OnlyKeysAreAccepted() throws Exception {
        StubClickHouseServer.Request request = insertOnce(OTHER_V1_ONLY_PROPERTIES);
        assertTrue(request.rawUri().startsWith("/?"), request.rawUri());
    }

    @Test
    @DisplayName("CLT-03: an operator's ignore_unknown_config_key=false in jdbcConnectionProperties makes a V1-only key fail the build on 0.11.0 and only warn on 0.9.6")
    void operatorIgnoreFlagWins() throws Exception {
        String properties = "?" + IGNORE_KEY + "=false&ssl=true";
        if (ClientErrorShapes.line() == ClientErrorShapes.Line.V0_11_0) {
            ClientMisconfigurationException e =
                    assertThrows(ClientMisconfigurationException.class, () -> insertOnce(properties));
            assertTrue(e.getMessage().contains("ssl"), e.getMessage());
        } else {
            // 0.9.6 only warns about an unknown key, whatever the flag says, so the client is built and inserts.
            StubClickHouseServer.Request request = insertOnce(properties);
            assertEquals("/", URI.create(request.rawUri()).getPath());
        }
    }

    @Test
    @DisplayName("PIN-03 client-version tripwire: client 0.11.0 forwards every custom_* key of jdbcConnectionProperties"
            + " with the operator's percent-encoding doubled; 0.9.6 forwards none")
    void customKeysAreForwardedWithDoubledEncodingClientVersionTripwire() throws Exception {
        ClientErrorShapes.Line line = assertDoesNotThrow(ClientErrorShapes::line,
                CLIENT_VERSION_TRIPWIRE + "this client-v2 line has no pinned row");
        StubClickHouseServer.Request keys = insertOnce(CUSTOM_KEYS_PROPERTIES);
        StubClickHouseServer.Request space = insertOnce(CUSTOM_SPACE_PROPERTY);
        Map<String, String> keysQuery = rawQueryMap(keys.rawUri());
        Map<String, String> spaceQuery = rawQueryMap(space.rawUri());
        String context = CLIENT_VERSION_TRIPWIRE + "client-v2 " + ClientErrorShapes.clientVersion() + ", URIs: "
                + keys.rawUri() + " | " + space.rawUri();

        if (line == ClientErrorShapes.Line.V0_11_0) {
            assertEquals("7", keysQuery.get("custom_myparam"), context);
            assertEquals("max_threads%253D3", keysQuery.get("custom_settings"), context);
            assertEquals("b%2520c", spaceQuery.get("custom_a"), context);
            assertFalse(keysQuery.containsKey("ssl"), context);
            assertTrue(keysQuery.containsKey("query"), context);
        } else {
            // 0.9.6 ignores these keys (measured in Phase 36 E-11, not re-measured here).
            assertTrue(keysQuery.keySet().stream().noneMatch(key -> key.startsWith(CUSTOM_PREFIX)), context);
            assertTrue(spaceQuery.keySet().stream().noneMatch(key -> key.startsWith(CUSTOM_PREFIX)), context);
        }
    }
}
