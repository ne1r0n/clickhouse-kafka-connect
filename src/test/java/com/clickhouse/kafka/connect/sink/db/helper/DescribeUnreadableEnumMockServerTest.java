package com.clickhouse.kafka.connect.sink.db.helper;

import com.clickhouse.client.config.ClickHouseProxyType;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Both clients answer an undescribable table with null, so that the mapping of the other tables of
 * the database goes on. The server here is a plain HTTP stub, so no Docker is needed.
 */
class DescribeUnreadableEnumMockServerTest {

    private static final String DATABASE = "default";
    private static final String UNREADABLE_TABLE = "unreadable_enum";
    private static final String READABLE_TABLE = "readable";
    // An escape the server never writes, so the enum label scan refuses the type.
    private static final String UNREADABLE_ENUM_ROW =
            "{\"name\":\"e\",\"type\":\"Enum8('\\\\q' = 1)\",\"default_type\":\"\",\"default_expression\":\"\","
                    + "\"comment\":\"\",\"is_subcolumn\":false}\n";
    private static final String READABLE_ROW =
            "{\"name\":\"id\",\"type\":\"Int32\",\"default_type\":\"\",\"default_expression\":\"\","
                    + "\"comment\":\"\",\"is_subcolumn\":false}\n";

    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String request = body + exchange.getRequestURI();
            byte[] response = (request.contains(UNREADABLE_TABLE) ? UNREADABLE_ENUM_ROW : READABLE_ROW)
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private ClickHouseHelperClient client(boolean useClientV2) {
        return new ClickHouseHelperClient.ClickHouseClientBuilder(
                        "127.0.0.1", server.getAddress().getPort(), ClickHouseProxyType.IGNORE, null, -1)
                .setDatabase(DATABASE)
                .useClientV2(useClientV2)
                .setJdbcConnectionProperties("compress=0&decompress=0")
                .build();
    }

    private void assertUnreadableTableIsSkippedAndTheNextOneIsMapped(ClickHouseHelperClient chc) {
        assertNull(chc.describeTable(DATABASE, UNREADABLE_TABLE));
        assertNotNull(chc.describeTable(DATABASE, READABLE_TABLE));
    }

    @Test
    void clientV1ReturnsNullForAnUnreadableEnumTypeAndKeepsDescribingLaterTables() {
        assertUnreadableTableIsSkippedAndTheNextOneIsMapped(client(false));
    }

    @Test
    void clientV2ReturnsNullForAnUnreadableEnumTypeAndKeepsDescribingLaterTables() {
        assertUnreadableTableIsSkippedAndTheNextOneIsMapped(client(true));
    }
}
