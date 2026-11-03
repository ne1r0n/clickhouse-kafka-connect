package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.ClientException;
import com.clickhouse.client.api.ServerException;
import com.clickhouse.client.api.insert.InsertSettings;
import com.clickhouse.client.config.ClickHouseProxyType;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.kafka.connect.util.Utils;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.kafka.connect.errors.RetriableException;

/**
 * What the connector sees when the real V2 client meets an HTTP error, one pinned row per client
 * line. Shared by the Docker-free pin and the live pin class, so the rows exist once. A bump that
 * changes a row fails the pin and the row is edited on purpose; an unknown client line fails with a
 * message that asks for a new row.
 */
final class ClientErrorShapes {

    enum Line {
        V0_9_6,
        V0_11_0
    }

    record Shape(
            String deepestCause,
            int serverCode,
            int attempts,
            boolean retriable,
            boolean retriableOnSocketException) {}

    private static final Pattern IMPLEMENTATION_VERSION = Pattern.compile("^client-v2 (\\d+\\.\\d+\\.\\d+)");
    private static final String LOOPBACK = "127.0.0.1";
    private static final String DATABASE = "pin";
    private static final String TABLE = "t";

    private ClientErrorShapes() {}

    static Line line() {
        String implementation = Client.class.getPackage().getImplementationVersion();
        Matcher m = IMPLEMENTATION_VERSION.matcher(String.valueOf(implementation));
        assertFalse(!m.find(), "unreadable client-v2 Implementation-Version: " + implementation);
        return switch (m.group(1)) {
            case "0.9.6" -> Line.V0_9_6;
            case "0.11.0" -> Line.V0_11_0;
            default -> throw new AssertionError(
                    "client-v2 " + m.group(1) + " has no pinned error-shape row: add a Line");
        };
    }

    static String clientVersion() {
        return line() == Line.V0_9_6 ? "0.9.6" : "0.11.0";
    }

    static boolean classifies(Exception e, boolean retryOnSocketException) {
        try {
            Utils.handleException(e, false, retryOnSocketException, Set.of(), new ArrayList<>());
            return false;
        } catch (RetriableException r) {
            return true;
        } catch (RuntimeException r) {
            return false;
        }
    }

    private static Throwable deepest(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    private static Shape observe(StubClickHouseServer.Responder responder) throws Exception {
        try (StubClickHouseServer stub = new StubClickHouseServer(responder);
                ClickHouseHelperClient chc = new ClickHouseHelperClient.ClickHouseClientBuilder(
                                LOOPBACK, stub.port(), ClickHouseProxyType.IGNORE, null, -1)
                        .setDatabase(DATABASE)
                        .useClientV2(true)
                        .build()) {
            Exception thrown = null;
            try {
                chc.getClient()
                        .insert(TABLE, new ByteArrayInputStream(new byte[] {1}), ClickHouseFormat.RowBinary, new InsertSettings())
                        .get();
            } catch (Exception e) {
                thrown = e;
            }
            assertNotNull(thrown, "the stub answered an error, so the insert must fail");
            Throwable root = deepest(thrown);
            int code = root instanceof ServerException se ? se.getCode() : -1;
            return new Shape(
                    root.getClass().getSimpleName(),
                    code,
                    stub.requests().size(),
                    classifies(thrown, false),
                    classifies(thrown, true));
        }
    }

    private static void assertShape(Shape v096, Shape v0110, StubClickHouseServer.Responder responder)
            throws Exception {
        assertEquals(line() == Line.V0_9_6 ? v096 : v0110, observe(responder));
    }

    static void assertBadGateway() throws Exception {
        assertShape(
                new Shape("ClientException", -1, 1, false, false),
                new Shape("ConnectException", -1, 4, false, true),
                StubClickHouseServer.status(502, "Bad Gateway"));
    }

    static void assertServiceUnavailable() throws Exception {
        assertShape(
                new Shape("ServerException", 0, 1, false, false),
                new Shape("ConnectException", -1, 4, false, true),
                StubClickHouseServer.status(503, "Service Unavailable"));
    }

    static void assertUnexpectedStatus() throws Exception {
        assertShape(
                new Shape("ServerException", 0, 1, false, false),
                new Shape("ClientException", -1, 1, false, false),
                StubClickHouseServer.status(418, "teapot"));
    }

    static void assertTimeoutExceeded() throws Exception {
        String body = "Code: 159. DB::Exception: Timeout exceeded. (TIMEOUT_EXCEEDED)";
        assertShape(
                new Shape("ServerException", 159, 4, true, true),
                new Shape("ServerException", 159, 1, true, true),
                StubClickHouseServer.serverException(159, body));
    }

    static void assertTooManyParts() throws Exception {
        String body = "Code: 252. DB::Exception: Too many parts. (TOO_MANY_PARTS)";
        Shape both = new Shape("ServerException", 252, 4, true, true);
        assertShape(both, both, StubClickHouseServer.serverException(252, body));
    }

    static void assertUnknownTable() throws Exception {
        String body = "Code: 60. DB::Exception: Table does not exist. (UNKNOWN_TABLE)";
        Shape both = new Shape("ServerException", 60, 1, false, false);
        assertShape(both, both, StubClickHouseServer.serverException(60, body));
    }

    static void assertClientExceptionNeverRetriable() {
        Exception e = new ClientException("unexpected");
        assertFalse(classifies(e, false));
        assertFalse(classifies(e, true));
    }
}
