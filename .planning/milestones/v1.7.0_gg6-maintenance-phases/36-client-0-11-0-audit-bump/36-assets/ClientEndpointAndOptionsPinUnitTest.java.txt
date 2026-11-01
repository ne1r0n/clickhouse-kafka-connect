package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.insert.InsertSettings;
import com.clickhouse.client.config.ClickHouseProxyType;
import com.clickhouse.data.ClickHouseFormat;
import java.io.ByteArrayInputStream;
import java.net.URI;
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
}
