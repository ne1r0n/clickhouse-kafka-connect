package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.ClickHouseException;
import com.clickhouse.client.api.ServerException;
import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.clickhouse.kafka.connect.sink.util.ThrowableTexts;
import com.clickhouse.kafka.connect.util.Utils;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Pins the value-free rebuild of every Jackson chain that {@code doWriteColValue} would pass
 * through. A Jackson message can quote a record value (a reference chain names a Map key), so a
 * chain holding a Jackson link must reach the caller without it while every fact retry
 * classification reads stays: the top's IOException or RuntimeException category, the kind and
 * error code of each RetriableException, DataException, ConnectException and server-error link,
 * and the type family and timeout prefix of the classified root.
 *
 * <p>Each of the 21 chains is driven through the real {@code doWriteColValue} of a writer whose
 * primitive write throws the scripted exception. The class extends nothing, carries no tag and uses
 * no mocking library, so it runs without Docker.
 */
public class JacksonOverIoValueFreeUnitTest {

    private static final String SENTINEL = "RECORD-VALUE-SENTINEL-45";
    private static final String JACKSON_TEXT = "reference chain: Map[\"" + SENTINEL + "\"]";
    private static final String READ_TIMEOUT_PREFIX = "Read timed out after";
    private static final String WRITE_TIMEOUT_PREFIX = "Write timed out after";
    private static final String TIMED_OUT = "timed out";
    private static final String STREAM_CLOSED = "stream closed by test";
    private static final String SCRIPTED = "scripted";
    private static final String COLUMN_NAME = "c_int";
    private static final String QUERY_ID_TEXT = "jackson-chain-query";
    private static final String WITHHELD_NOTE = " (message withheld: may contain record data)";
    private static final String VALUE_FREE_MESSAGE_FORMAT =
            "Failed to write column `%s`: Kafka value of type %s (value class: %s) could not be written as ClickHouse type %s: %s";
    private static final int SERVER_ERROR_CODE = 210;

    private static JsonMappingException jackson(String text) {
        return new JsonMappingException(text);
    }

    private static JsonMappingException jackson(String text, Throwable cause) {
        return new JsonMappingException(text, cause);
    }

    private static SocketTimeoutException readTimeout() {
        return new SocketTimeoutException(READ_TIMEOUT_PREFIX + " 30000 ms");
    }

    private static ServerException serverErrorOver(Throwable cause) {
        ServerException server = new ServerException(SERVER_ERROR_CODE, "server says", 0, "query-1");
        server.initCause(cause);
        return server;
    }

    /** The scripted chains; each call of {@code script()} builds fresh instances. */
    enum Shape {
        OVER_SOCKET_TIMEOUT(() -> jackson(JACKSON_TEXT, new SocketTimeoutException(TIMED_OUT))),
        OVER_READ_TIMEOUT_TEXT(() -> jackson(JACKSON_TEXT, new IOException(READ_TIMEOUT_PREFIX + " 30000 ms"))),
        OVER_UNKNOWN_HOST(() -> jackson(JACKSON_TEXT, new UnknownHostException("clickhouse.invalid"))),
        OVER_SOCKET_EXCEPTION(() -> jackson(JACKSON_TEXT, new SocketException("Connection reset"))),
        OVER_PLAIN_IO(() -> jackson(JACKSON_TEXT, new IOException(STREAM_CLOSED))),
        OVER_RETRIABLE_OVER_PLAIN_IO(
                () -> jackson(JACKSON_TEXT, new RetriableException(SCRIPTED, new IOException(STREAM_CLOSED)))),
        OVER_CONNECT_OVER_SOCKET_TIMEOUT(
                () -> jackson(JACKSON_TEXT, new ConnectException(SCRIPTED, new SocketTimeoutException(TIMED_OUT)))),
        PLAIN_IO_OVER_JACKSON_ROOT(() -> new IOException(STREAM_CLOSED, jackson(JACKSON_TEXT))),
        PLAIN_IO_OVER_TIMEOUT_PREFIXED_JACKSON_ROOT(
                () -> new IOException(STREAM_CLOSED, jackson(READ_TIMEOUT_PREFIX + " 30000 ms " + JACKSON_TEXT))),
        PLAIN_IO_OVER_WRITE_TIMEOUT_PREFIXED_JACKSON_ROOT(
                () -> new IOException(
                        STREAM_CLOSED,
                        jackson(Utils.CLICKHOUSE_CLIENT_ERROR_WRITE_TIMEOUT_MSG + " 30000 ms " + JACKSON_TEXT))),
        RUNTIME_OVER_JACKSON_ROOT(() -> new RuntimeException(jackson(JACKSON_TEXT))),
        RUNTIME_OVER_JACKSON_OVER_READ_TIMEOUT_SOCKET(
                () -> new RuntimeException(jackson(JACKSON_TEXT, readTimeout()))),
        RETRIABLE_OVER_JACKSON_ROOT(() -> new RetriableException(SCRIPTED, jackson(JACKSON_TEXT))),
        JACKSON_OVER_JACKSON_OVER_IO(
                () -> jackson(JACKSON_TEXT, jackson(JACKSON_TEXT, new SocketTimeoutException(TIMED_OUT)))),
        IO_OVER_V1_SERVER_ERROR_OVER_JACKSON(
                () -> new IOException(STREAM_CLOSED, new ClickHouseException(SERVER_ERROR_CODE, jackson(JACKSON_TEXT), null))),
        IO_OVER_V2_SERVER_ERROR_OVER_JACKSON(
                () -> new IOException(STREAM_CLOSED, serverErrorOver(jackson(JACKSON_TEXT)))),
        IO_OVER_RUNTIME_OVER_JACKSON(
                () -> new IOException(STREAM_CLOSED, new RuntimeException(jackson(JACKSON_TEXT)))),
        IO_OVER_IO_OVER_JACKSON(() -> new IOException(STREAM_CLOSED, new IOException(jackson(JACKSON_TEXT)))),
        IO_OVER_CONNECT_OVER_JACKSON(
                () -> new IOException(STREAM_CLOSED, new ConnectException(jackson(JACKSON_TEXT)))),
        IO_OVER_DATA_OVER_JACKSON(() -> new IOException(STREAM_CLOSED, new DataException(jackson(JACKSON_TEXT)))),
        JACKSON_OVER_ILLEGAL_ARGUMENT_OVER_IO(
                () -> jackson(JACKSON_TEXT, new IllegalArgumentException(SENTINEL, new IOException(STREAM_CLOSED))));

        private final Supplier<Exception> script;

        Shape(Supplier<Exception> script) {
            this.script = script;
        }

        Exception script() {
            return script.get();
        }
    }

    enum Outcome {
        RETRIABLE,
        DATA_EXCEPTION,
        RUNTIME_WRAP,
        RETURNS
    }

    /** What one {@code doWriteColValue} call threw and the log texts written meanwhile. */
    private record Captured(Throwable thrown, List<String> logTexts) {}

    private static Captured drive(Exception scripted) {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0)) {
            @Override
            protected void doWritePrimitive(
                    Type columnType, Schema.Type dataType, OutputStream out, Object primitive, Column col)
                    throws IOException {
                if (scripted instanceof IOException io) {
                    throw io;
                }
                throw (RuntimeException) scripted;
            }
        };
        Column column = Column.builder().name(COLUMN_NAME).type(Type.INT32).build();
        Data value = new Data(Schema.INT32_SCHEMA, 1);
        try (LogCapture log = LogCapture.attach(ClickHouseWriter.class)) {
            Throwable thrown = null;
            try {
                writer.doWriteColValue(column, new ByteArrayOutputStream(), value, false);
            } catch (Throwable t) {
                thrown = t;
            }
            assertNotNull(thrown, "the scripted failure must leave doWriteColValue");
            return new Captured(thrown, log.allTexts());
        }
    }

    private static String expectedTopMessage(Exception scripted) {
        return String.format(
                VALUE_FREE_MESSAGE_FORMAT,
                COLUMN_NAME,
                Schema.Type.INT32,
                Integer.class.getName(),
                Type.INT32,
                scripted.getClass().getName());
    }

    private static String withheld(Throwable link) {
        return link.getClass().getName() + WITHHELD_NOTE;
    }

    private static boolean isTrustedIoLink(Throwable link) {
        return link instanceof IOException && !(link instanceof JacksonException);
    }

    /** The first link of the longest run of non-Jackson IOExceptions at the bottom of the chain, or null. */
    private static Throwable trustedSuffixStart(List<Throwable> chain) {
        Throwable start = null;
        for (int i = chain.size() - 1; i >= 0 && isTrustedIoLink(chain.get(i)); i--) {
            start = chain.get(i);
        }
        return start;
    }

    private static boolean anyMessageContains(Throwable top, String text) {
        for (Throwable link : ThrowableTexts.causeChain(top)) {
            if (link.getMessage() != null && link.getMessage().contains(text)) {
                return true;
            }
        }
        return false;
    }

    @ParameterizedTest(name = "rawChainCarriesTheValue {0}")
    @EnumSource(Shape.class)
    void rawChainCarriesTheValue(Shape shape) {
        assertTrue(anyMessageContains(shape.script(), SENTINEL), "self-check: the scripted chain quotes the value");
    }

    @ParameterizedTest(name = "rebuildIsValueFreeAndKeepsTheSuffix {0}")
    @EnumSource(Shape.class)
    void rebuildIsValueFreeAndKeepsTheSuffix(Shape shape) {
        Exception scripted = shape.script();
        List<Throwable> original = ThrowableTexts.causeChain(scripted);

        Throwable thrown = drive(scripted).thrown();
        List<Throwable> rebuilt = ThrowableTexts.causeChain(thrown);

        for (Throwable link : rebuilt) {
            assertFalse(link instanceof JacksonException, "no Jackson link may be left in the thrown chain");
        }
        assertEquals(expectedTopMessage(scripted), thrown.getMessage());
        Throwable suffixStart = trustedSuffixStart(original);
        if (suffixStart != null) {
            int at = -1;
            for (int i = 0; i < rebuilt.size(); i++) {
                if (rebuilt.get(i) == suffixStart) {
                    at = i;
                }
            }
            assertTrue(at > 0, "the first link of the trusted suffix must be kept as the same object, below the top");
            Set<Throwable> originals = Collections.newSetFromMap(new IdentityHashMap<>());
            originals.addAll(original);
            for (int i = 0; i < at; i++) {
                assertFalse(originals.contains(rebuilt.get(i)), "a link above the kept suffix must be a stand-in");
            }
        } else {
            Throwable originalRoot = original.get(original.size() - 1);
            assertTrue(originalRoot instanceof JacksonException, "without a trusted suffix the root is a Jackson link");
            Throwable root = rebuilt.get(rebuilt.size() - 1);
            assertSame(IOException.class, root.getClass());
            assertNull(root.getCause());
            String message = originalRoot.getMessage();
            String prefix = "";
            if (message.startsWith(READ_TIMEOUT_PREFIX)) {
                prefix = READ_TIMEOUT_PREFIX + " ";
            } else if (message.startsWith(WRITE_TIMEOUT_PREFIX)) {
                prefix = WRITE_TIMEOUT_PREFIX + " ";
            }
            assertEquals(prefix + withheld(originalRoot), root.getMessage());
        }
    }

    /** The classification-read link kinds of a chain, top first. */
    private static List<String> classifiedKinds(Throwable top) {
        List<String> kinds = new ArrayList<>();
        for (Throwable link : ThrowableTexts.causeChain(top)) {
            if (link instanceof RetriableException) {
                kinds.add("Retriable");
            } else if (link instanceof DataException) {
                kinds.add("Data");
            } else if (link instanceof ConnectException) {
                kinds.add("Connect");
            } else if (link instanceof ClickHouseException v1) {
                kinds.add("V1:" + v1.getErrorCode());
            } else if (link instanceof ServerException v2) {
                kinds.add("V2:" + v2.getCode());
            }
        }
        return kinds;
    }

    /** The classified root's type family and its timeout-prefix fact, as handleException reads them. */
    private static String rootFacts(Exception top) {
        Exception root = Utils.getRootCause(top, true);
        String family;
        if (root instanceof ClickHouseException v1) {
            family = "V1:" + v1.getErrorCode();
        } else if (root instanceof ServerException v2) {
            family = "V2:" + v2.getCode();
        } else if (root instanceof SocketTimeoutException) {
            family = "SocketTimeout";
        } else if (root instanceof UnknownHostException) {
            family = "UnknownHost";
        } else if (root instanceof SocketException) {
            family = "Socket";
        } else if (root instanceof IOException) {
            family = "IO";
        } else {
            family = "Other";
        }
        String message = root == null ? null : root.getMessage();
        boolean timeoutPrefix = message != null
                && (message.indexOf(READ_TIMEOUT_PREFIX) == 0 || message.indexOf(WRITE_TIMEOUT_PREFIX) == 0);
        return family + " timeoutPrefix=" + timeoutPrefix;
    }

    @ParameterizedTest(name = "keepsTheClassifiedLinks {0}")
    @EnumSource(Shape.class)
    void keepsTheClassifiedLinks(Shape shape) {
        Exception scripted = shape.script();

        Throwable thrown = drive(scripted).thrown();

        assertEquals(scripted instanceof IOException, thrown instanceof IOException);
        assertEquals(scripted instanceof RuntimeException, thrown instanceof RuntimeException);
        assertEquals(classifiedKinds(scripted), classifiedKinds(thrown));
        assertEquals(rootFacts(scripted), rootFacts((Exception) thrown));
    }

    @ParameterizedTest(name = "carriesNoRecordValue {0}")
    @EnumSource(Shape.class)
    void carriesNoRecordValue(Shape shape) {
        Captured captured = drive(shape.script());
        Throwable thrown = captured.thrown();

        assertFalse(anyMessageContains(thrown, SENTINEL), "no message of the thrown chain may carry the value");
        for (String text : ThrowableTexts.allTexts(thrown)) {
            assertFalse(text.contains(SENTINEL), "a thrown text must carry no record value");
        }
        InMemoryDLQ.ConnectDlqHeaders headers = InMemoryDLQ.connectHeaders(new RuntimeException(QUERY_ID_TEXT, thrown));
        assertFalse(String.valueOf(headers.exceptionMessage()).contains(SENTINEL));
        assertFalse(headers.exceptionStacktrace().contains(SENTINEL));
        for (String text : captured.logTexts()) {
            assertFalse(String.valueOf(text).contains(SENTINEL), "a log text must carry no record value");
        }
    }

    /** What Utils.handleException decides for a failure wrapped the way Processing wraps it. */
    private static Outcome outcome(Throwable writerFailure, boolean clientV2, boolean errorsTolerance, boolean retryOnSocket) {
        Throwable seenByProcessing = writerFailure;
        if (clientV2 && writerFailure instanceof RuntimeException) {
            seenByProcessing = new com.clickhouse.client.api.ClickHouseException(
                    ScriptedWriteFailureWriter.V2_INSERT_FAILED_MESSAGE, writerFailure);
        }
        try {
            Utils.handleException(
                    new RuntimeException(QUERY_ID_TEXT, seenByProcessing),
                    errorsTolerance,
                    retryOnSocket,
                    Set.of(),
                    new ArrayList<>());
            return Outcome.RETURNS;
        } catch (RetriableException e) {
            return Outcome.RETRIABLE;
        } catch (DataException e) {
            return Outcome.DATA_EXCEPTION;
        } catch (Exception e) {
            return Outcome.RUNTIME_WRAP;
        }
    }

    @ParameterizedTest(name = "keepsTheRetryOutcome {0}")
    @EnumSource(Shape.class)
    void keepsTheRetryOutcome(Shape shape) {
        Exception scripted = shape.script();

        Throwable thrown = drive(scripted).thrown();

        for (boolean clientV2 : new boolean[] {false, true}) {
            for (boolean errorsTolerance : new boolean[] {false, true}) {
                for (boolean retryOnSocket : new boolean[] {false, true}) {
                    assertEquals(
                            outcome(scripted, clientV2, errorsTolerance, retryOnSocket),
                            outcome(thrown, clientV2, errorsTolerance, retryOnSocket),
                            "clientV2=" + clientV2 + " errorsTolerance=" + errorsTolerance
                                    + " retryOnSocketException=" + retryOnSocket);
                }
            }
        }
    }

    /**
     * The two chains whose retriable outcome rests on a classified-root fact the rebuild must carry
     * over: a Jackson root whose message starts with the client write-timeout prefix, and the
     * Map/Tuple/Array lambda shape (a RuntimeException over a Jackson link over a socket timeout).
     */
    @ParameterizedTest(name = "staysRetriableAndValueFree {0}")
    @EnumSource(
            value = Shape.class,
            names = {
                "PLAIN_IO_OVER_WRITE_TIMEOUT_PREFIXED_JACKSON_ROOT",
                "RUNTIME_OVER_JACKSON_OVER_READ_TIMEOUT_SOCKET"
            })
    void staysRetriableAndValueFree(Shape shape) {
        Exception scripted = shape.script();
        Captured captured = drive(scripted);
        Throwable thrown = captured.thrown();

        for (boolean clientV2 : new boolean[] {false, true}) {
            for (boolean errorsTolerance : new boolean[] {false, true}) {
                assertEquals(
                        Outcome.RETRIABLE,
                        outcome(scripted, clientV2, errorsTolerance, false),
                        "self-check: the scripted chain is retriable without retryOnSocketException");
                assertEquals(
                        Outcome.RETRIABLE,
                        outcome(thrown, clientV2, errorsTolerance, false),
                        "clientV2=" + clientV2 + " errorsTolerance=" + errorsTolerance);
            }
        }
        for (Throwable link : ThrowableTexts.causeChain(thrown)) {
            assertFalse(link instanceof JacksonException, "no Jackson link may be left in the thrown chain");
        }
        assertFalse(anyMessageContains(thrown, SENTINEL), "no message of the thrown chain may carry the value");
        InMemoryDLQ.ConnectDlqHeaders headers = InMemoryDLQ.connectHeaders(new RuntimeException(QUERY_ID_TEXT, thrown));
        assertFalse(String.valueOf(headers.exceptionMessage()).contains(SENTINEL));
        assertFalse(headers.exceptionStacktrace().contains(SENTINEL));
        for (String text : captured.logTexts()) {
            assertFalse(String.valueOf(text).contains(SENTINEL), "a log text must carry no record value");
        }
    }

    @Test
    void writeTimeoutPrefixOfAJacksonRootSurvivesOnTheRebuiltRoot() {
        Throwable thrown = drive(Shape.PLAIN_IO_OVER_WRITE_TIMEOUT_PREFIXED_JACKSON_ROOT.script()).thrown();

        List<Throwable> rebuilt = ThrowableTexts.causeChain(thrown);
        Throwable root = rebuilt.get(rebuilt.size() - 1);
        assertTrue(root.getMessage().startsWith(Utils.CLICKHOUSE_CLIENT_ERROR_WRITE_TIMEOUT_MSG + " "));
    }

    @Test
    void mapLambdaShapeDropsTheJacksonLinkAndKeepsTheTimeoutAsTheDirectCause() {
        SocketTimeoutException timeout = readTimeout();

        Throwable thrown = drive(new RuntimeException(jackson(JACKSON_TEXT, timeout))).thrown();

        assertSame(RuntimeException.class, thrown.getClass());
        assertSame(timeout, thrown.getCause());
    }
}
