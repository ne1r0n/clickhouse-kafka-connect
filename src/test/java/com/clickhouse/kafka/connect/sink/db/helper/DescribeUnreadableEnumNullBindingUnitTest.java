package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.config.ClickHouseProxyType;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Ties the {@code null} that {@code describeTable} returns for an unreadable enum type to the enum
 * scan that refused it. {@link DescribeUnreadableEnumMockServerTest} asserts the {@code null}, but a
 * {@code null} also comes from any other failure inside the describe, so that assertion alone would
 * stay green if the enum refusal were replaced by an unrelated error. Here the ERROR event that the
 * describe writes must carry the enum refusal, and an unparseable row that is not an enum must not.
 * The server is a plain HTTP stub, so no Docker is needed.
 */
class DescribeUnreadableEnumNullBindingUnitTest {

    private static final String DATABASE = "default";
    private static final String UNREADABLE_TABLE = "unreadable_enum";
    private static final String MALFORMED_TABLE = "malformed_row";
    // An escape the server never writes, so the enum label scan refuses the type.
    private static final String UNREADABLE_ENUM_ROW =
            "{\"name\":\"e\",\"type\":\"Enum8('\\\\q' = 1)\",\"default_type\":\"\",\"default_expression\":\"\","
                    + "\"comment\":\"\",\"is_subcolumn\":false}\n";
    // No closing brace: the row cannot be parsed, and its type is not an enum.
    private static final String MALFORMED_ROW = "{\"name\":\"e\",\"type\":\"Int32\",\"is_subcolumn\":false\n";
    private static final String ERROR_LEVEL = "ERROR";
    private static final String ENUM_CAUSE = "Enum type could not be read";
    private static final String NAME = "{displayName}[{index}] clientV2={0}";
    private static final String DESCRIBE_IS_NULL = "describeTable must answer null";
    private static final String ENUM_CAUSE_LOGGED =
            "an ERROR event of the describe must carry the enum refusal as its throwable text";
    private static final String DESCRIBE_FAILURE_LOGGED = "the describe failure must be logged at ERROR with its throwable";
    private static final String ENUM_CAUSE_ABSENT =
            "a failure that is not an enum refusal must not carry the enum refusal text";

    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String request = body + exchange.getRequestURI();
            byte[] response = (request.contains(UNREADABLE_TABLE) ? UNREADABLE_ENUM_ROW : MALFORMED_ROW)
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

    private List<LogCapture.CapturedEvent> describeToNullAndCaptureErrors(boolean useClientV2, String table) {
        try (LogCapture log = LogCapture.attach(ClickHouseHelperClient.class);
                ClickHouseHelperClient chc = client(useClientV2)) {
            assertNull(chc.describeTable(DATABASE, table), DESCRIBE_IS_NULL);
            return log.events(ERROR_LEVEL);
        }
    }

    private static boolean anyThrowableTextHasTheEnumCause(List<LogCapture.CapturedEvent> errors) {
        return errors.stream()
                .anyMatch(e -> e.throwableTexts().stream().anyMatch(t -> t.contains(ENUM_CAUSE)));
    }

    @ParameterizedTest(name = NAME)
    @ValueSource(booleans = {false, true})
    void unreadableEnumTableIsNullBecauseTheEnumScanRefusedIt(boolean useClientV2) {
        List<LogCapture.CapturedEvent> errors = describeToNullAndCaptureErrors(useClientV2, UNREADABLE_TABLE);
        assertTrue(anyThrowableTextHasTheEnumCause(errors), ENUM_CAUSE_LOGGED);
    }

    @ParameterizedTest(name = NAME)
    @ValueSource(booleans = {false, true})
    void aNonEnumDescribeFailureIsAlsoNullButNamesNoEnumCause(boolean useClientV2) {
        List<LogCapture.CapturedEvent> errors = describeToNullAndCaptureErrors(useClientV2, MALFORMED_TABLE);
        assertTrue(errors.stream().anyMatch(LogCapture.CapturedEvent::hasThrowable), DESCRIBE_FAILURE_LOGGED);
        assertFalse(anyThrowableTextHasTheEnumCause(errors), ENUM_CAUSE_ABSENT);
    }
}
