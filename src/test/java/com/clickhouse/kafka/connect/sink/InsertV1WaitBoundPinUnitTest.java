package com.clickhouse.kafka.connect.sink;

import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.assertBoundMessage;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.assertNoSentinel;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.chainText;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.config;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.convert;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.largeRecords;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.posts;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.retriableInChain;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.smallRecord;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.topic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.PinWriter;
import com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.Route;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.db.helper.StubClickHouseServer;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free pins for the client V1 wait of {@code clickhouseClientInsertTimeoutMs}. A real {@code
 * ClickHouseWriter} on client V1 inserts into raw loopback stubs that stop reading the request body, read it and
 * never answer, or stream progress headers, and each insert must end within a preemptive bound as a retriable
 * failure. The operator's {@code socket_timeout} keeps ownership of the V1 limits.
 */
class InsertV1WaitBoundPinUnitTest {

    private static final String ROUTE_NAME = "{displayName}[{index}] route={0}";
    private static final String SHAPE_NAME = "{displayName}[{index}] shape={0} route={1} errors.tolerance={2}";
    private static final String TOLERANCE_NAME = "{displayName}[{index}] errors.tolerance={0}";
    private static final String CLIENT_V1 = "V1";
    private static final String TOPIC_PREFIX = "insert_v1_wait_";
    private static final String QUERY_ID = "insert-v1-wait-q1";
    private static final int V1_LARGE_RECORDS = 64;
    private static final String OPERATOR_LONG = "socket_timeout=60000";
    private static final String OPERATOR_SHORT = "socket_timeout=200";
    private static final String OPERATOR_INSERT_TIMEOUT_MS = "20000";
    private static final String OPERATOR_WRITE_INSERT_TIMEOUT_MS = "1000";
    private static final Duration OPERATOR_SHORT_GUARD = Duration.ofSeconds(3);
    private static final Duration RUNNING_OBSERVATION = Duration.ofSeconds(3);
    private static final Duration RELEASE_WAIT = Duration.ofSeconds(5);
    private static final Duration RELEASE_POLL = Duration.ofMillis(100);
    private static final Duration PROGRESS_INTERVAL = Duration.ofMillis(100);
    private static final Duration PROGRESS_TOTAL = Duration.ofSeconds(6);
    private static final int EXPECTED_POSTS = 1;
    private static final String SOCKET_WRITE_WORD = "write";
    private static final String NIO_SOCKET_PACKAGE = "sun.nio.ch.";
    private static final String JAVA_SOCKET_PREFIX = "java.net.Socket";
    private static final String CLIENT_PACKAGE = "com.clickhouse.";
    private static final String STUB_CLASS_PART = "StubClickHouseServer";
    private static final String BOUND_PHRASE_PREFIX = "clickhouseClientInsertTimeoutMs=";
    private static final String PUT_CASE_PREFIX = "put_";
    private static final String CASE_TAG_SEPARATOR = "_";

    private static final String GUARD_MESSAGE = "a client V1 insert must end within the bound";
    private static final String ROUTE_PREMISE = "the record must take this route's insert method";
    private static final String RETRIABLE = "an insert not answered within the bound is retriable";
    private static final String TIMEOUT_CAUSE = "the retriable failure wraps the wait's TimeoutException";
    private static final String BOUND_IN_CHAIN = "the bound's own message is in the failure chain";
    private static final String ONE_POST = "POST count at the stub; a second one would be a client-side retry";
    private static final String DLQ_RECORDS = "DLQ record count";
    private static final String NO_BLOCKED_WRITER = "no client thread may stay blocked in a socket write after the bound";
    private static final String OPERATOR_STILL_RUNNING = "the operator's longer socket_timeout leaves the write stall alone";
    private static final String OPERATOR_NO_CONNECTOR_BOUND = "no connector bound fires when the operator named socket_timeout";
    private static final String OPERATOR_SHORT_MESSAGE = "the operator's shorter socket_timeout must end the stall first";

    /** The three client V1 outcomes the bound ends, each with its own stub, body, bound and preemptive guard. */
    enum Shape {
        WRITE_STALL(1000L, Duration.ofSeconds(10), V1_LARGE_RECORDS),
        QUIET_STALL(300L, Duration.ofSeconds(10), 0),
        PROGRESS(1500L, Duration.ofSeconds(5), 0);

        private final long boundMs;
        private final Duration guard;
        private final int largeRecords;

        Shape(long boundMs, Duration guard, int largeRecords) {
            this.boundMs = boundMs;
            this.guard = guard;
            this.largeRecords = largeRecords;
        }

        String insertTimeoutMs() {
            return String.valueOf(boundMs);
        }

        StubClickHouseServer stub() throws IOException {
            return switch (this) {
                case WRITE_STALL -> StubClickHouseServer.notReadingBody();
                case QUIET_STALL -> StubClickHouseServer.stalling();
                case PROGRESS -> StubClickHouseServer.progressHeaders(PROGRESS_INTERVAL, PROGRESS_TOTAL);
            };
        }

        List<SinkRecord> sinkRecords(Route route, String topic) {
            return largeRecords > 0 ? largeRecords(route, topic, largeRecords) : List.of(smallRecord(route, topic));
        }
    }

    static Stream<Arguments> shapesRoutesAndTolerances() {
        return Stream.of(ClickHouseSinkConfig.ERROR_TOLERANCE_NONE, ClickHouseSinkConfig.ERROR_TOLERANCE_ALL)
                .flatMap(tolerance -> Stream.concat(
                        Arrays.stream(Route.values()).map(route -> Arguments.of(Shape.WRITE_STALL, route, tolerance)),
                        Stream.of(
                                Arguments.of(Shape.QUIET_STALL, Route.JSON, tolerance),
                                Arguments.of(Shape.PROGRESS, Route.JSON, tolerance))));
    }

    private static List<Record> records(Route route, List<SinkRecord> sinkRecords) {
        List<Record> records = convert(sinkRecords);
        assertEquals(route.schemaType(), records.get(0).getSchemaType(), ROUTE_PREMISE);
        return records;
    }

    private static Exception insertFailure(PinWriter writer, List<Record> records, String topic, Duration guard) {
        return assertTimeoutPreemptively(
                guard,
                () -> assertThrows(
                        Exception.class, () -> writer.doInsert(records, new QueryIdentifier(topic, QUERY_ID))),
                GUARD_MESSAGE);
    }

    private static RetriableException assertV1BoundFailure(Exception thrown, long boundMs) {
        RetriableException retriable = retriableInChain(thrown);
        assertNotNull(retriable, RETRIABLE);
        assertBoundMessage(retriable, CLIENT_V1, boundMs, QUERY_ID);
        assertInstanceOf(TimeoutException.class, retriable.getCause(), TIMEOUT_CAUSE);
        assertNoSentinel(thrown);
        return retriable;
    }

    private static void boundedDirectInsert(Shape shape, Route route, String caseTag, boolean oneStubPost)
            throws Exception {
        String topic = topic(TOPIC_PREFIX, caseTag, route);
        List<Record> records = records(route, shape.sinkRecords(route, topic));

        PinWriter writer = new PinWriter(topic);
        try (StubClickHouseServer stub = shape.stub()) {
            assertTrue(writer.start(config(stub, CLIENT_V1, shape.insertTimeoutMs(), Map.of())));

            Exception thrown = insertFailure(writer, records, topic, shape.guard);

            assertV1BoundFailure(thrown, shape.boundMs);
            if (oneStubPost) {
                assertEquals(EXPECTED_POSTS, posts(stub).size(), ONE_POST);
            }
        } finally {
            writer.finish();
        }
    }

    /** Names of live threads blocked in a socket write that sit under a client frame, the stub's own threads aside. */
    private static List<String> clientThreadsInASocketWrite() {
        List<String> blocked = new ArrayList<>();
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            boolean socketWrite = false;
            boolean clientFrame = false;
            boolean stubFrame = false;
            for (StackTraceElement frame : entry.getValue()) {
                String className = frame.getClassName();
                if ((className.startsWith(NIO_SOCKET_PACKAGE) || className.startsWith(JAVA_SOCKET_PREFIX))
                        && frame.getMethodName().toLowerCase(Locale.ROOT).contains(SOCKET_WRITE_WORD)) {
                    socketWrite = true;
                }
                if (className.startsWith(CLIENT_PACKAGE)) {
                    clientFrame = true;
                }
                if (className.contains(STUB_CLASS_PART)) {
                    stubFrame = true;
                }
            }
            if (socketWrite && clientFrame && !stubFrame && entry.getKey().isAlive()) {
                blocked.add(entry.getKey().getName());
            }
        }
        return blocked;
    }

    @ParameterizedTest(name = ROUTE_NAME)
    @EnumSource(Route.class)
    void writeStalledV1InsertEndsWithinTheBoundAsRetriable(Route route) throws Exception {
        boundedDirectInsert(Shape.WRITE_STALL, route, "stall", true);
    }

    @ParameterizedTest(name = ROUTE_NAME)
    @EnumSource(Route.class)
    void quietV1InsertEndsWithinTheBoundAsRetriable(Route route) throws Exception {
        boundedDirectInsert(Shape.QUIET_STALL, route, "quiet", true);
    }

    @Test
    void progressStreamingV1InsertEndsWithinTheBoundAsRetriable() throws Exception {
        boundedDirectInsert(Shape.PROGRESS, Route.JSON, "progress", false);
    }

    @ParameterizedTest(name = SHAPE_NAME)
    @MethodSource("shapesRoutesAndTolerances")
    void boundedV1InsertThroughPutIsRetriableWithNoDlqRecord(Shape shape, Route route, String tolerance)
            throws Exception {
        String topic = topic(
                TOPIC_PREFIX,
                PUT_CASE_PREFIX + shape.name().toLowerCase(Locale.ROOT) + CASE_TAG_SEPARATOR + tolerance,
                route);
        List<SinkRecord> sinkRecords = shape.sinkRecords(route, topic);
        records(route, sinkRecords);

        InMemoryDLQ dlq = new InMemoryDLQ();
        PinWriter writer = new PinWriter(topic);
        try (StubClickHouseServer stub = shape.stub()) {
            ClickHouseSinkConfig config = config(
                    stub, CLIENT_V1, shape.insertTimeoutMs(), Map.of(ClickHouseSinkConfig.ERRORS_TOLERANCE, tolerance));
            ProxySinkTask task = new ProxySinkTask(config, dlq, writer);
            try {
                task.start();

                RetriableException thrown = assertTimeoutPreemptively(
                        shape.guard,
                        () -> assertThrows(RetriableException.class, () -> task.put(sinkRecords), RETRIABLE),
                        GUARD_MESSAGE);

                assertEquals(0, dlq.size(), DLQ_RECORDS);
                assertTrue(chainText(thrown).contains(BOUND_PHRASE_PREFIX + shape.boundMs), BOUND_IN_CHAIN);
                assertEquals(EXPECTED_POSTS, posts(stub).size(), ONE_POST);
                assertNoSentinel(thrown);
            } finally {
                stub.close();
                task.stop();
            }
        } finally {
            writer.closeLeftoverHelpers();
            writer.unregisterStatistics();
        }
    }

    @Test
    void timedOutV1InsertLeavesNoClientThreadBlockedInASocketWrite() throws Exception {
        Shape shape = Shape.WRITE_STALL;
        Route route = Route.JSON;
        String topic = topic(TOPIC_PREFIX, "release", route);
        List<Record> records = records(route, shape.sinkRecords(route, topic));

        PinWriter writer = new PinWriter(topic);
        try (StubClickHouseServer stub = shape.stub()) {
            assertTrue(writer.start(config(stub, CLIENT_V1, shape.insertTimeoutMs(), Map.of())));

            Exception thrown = insertFailure(writer, records, topic, shape.guard);
            assertNotNull(retriableInChain(thrown), RETRIABLE);

            long deadline = System.nanoTime() + RELEASE_WAIT.toNanos();
            List<String> blocked = clientThreadsInASocketWrite();
            while (!blocked.isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(RELEASE_POLL.toMillis());
                blocked = clientThreadsInASocketWrite();
            }
            assertTrue(blocked.isEmpty(), NO_BLOCKED_WRITER + ": " + blocked);
        } finally {
            writer.finish();
        }
    }

    @Test
    void operatorSocketTimeoutLeavesTheV1WaitToTheOperator() throws Exception {
        Shape shape = Shape.WRITE_STALL;
        Route route = Route.JSON;
        String topic = topic(TOPIC_PREFIX, "operator_long", route);
        List<Record> records = records(route, shape.sinkRecords(route, topic));

        PinWriter writer = new PinWriter(topic);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (StubClickHouseServer stub = shape.stub()) {
            assertTrue(writer.start(config(
                    stub,
                    CLIENT_V1,
                    OPERATOR_WRITE_INSERT_TIMEOUT_MS,
                    Map.of(ClickHouseSinkConfig.JDBC_CONNECTION_PROPERTIES, OPERATOR_LONG))));

            Future<Object> call = executor.submit(() -> {
                writer.doInsert(records, new QueryIdentifier(topic, QUERY_ID));
                return null;
            });
            assertThrows(
                    TimeoutException.class,
                    () -> call.get(RUNNING_OBSERVATION.toMillis(), TimeUnit.MILLISECONDS),
                    OPERATOR_STILL_RUNNING);

            stub.close();
            ExecutionException ended = assertThrows(
                    ExecutionException.class,
                    () -> call.get(RELEASE_WAIT.toMillis(), TimeUnit.MILLISECONDS),
                    GUARD_MESSAGE);
            assertNull(retriableInChain(ended), OPERATOR_NO_CONNECTOR_BOUND);
        } finally {
            executor.shutdownNow();
            writer.finish();
        }
    }

    @ParameterizedTest(name = TOLERANCE_NAME)
    @ValueSource(strings = {ClickHouseSinkConfig.ERROR_TOLERANCE_NONE, ClickHouseSinkConfig.ERROR_TOLERANCE_ALL})
    void operatorShorterSocketTimeoutEndsAV1QuietStallThroughPut(String tolerance) throws Exception {
        Route route = Route.JSON;
        String topic = topic(TOPIC_PREFIX, "operator_short_" + tolerance, route);
        SinkRecord sinkRecord = smallRecord(route, topic);

        InMemoryDLQ dlq = new InMemoryDLQ();
        PinWriter writer = new PinWriter(topic);
        try (StubClickHouseServer stub = StubClickHouseServer.stalling()) {
            ClickHouseSinkConfig config = config(
                    stub,
                    CLIENT_V1,
                    OPERATOR_INSERT_TIMEOUT_MS,
                    Map.of(
                            ClickHouseSinkConfig.JDBC_CONNECTION_PROPERTIES,
                            OPERATOR_SHORT,
                            ClickHouseSinkConfig.ERRORS_TOLERANCE,
                            tolerance));
            ProxySinkTask task = new ProxySinkTask(config, dlq, writer);
            try {
                task.start();

                assertTimeoutPreemptively(
                        OPERATOR_SHORT_GUARD,
                        () -> assertThrows(RetriableException.class, () -> task.put(List.of(sinkRecord)), RETRIABLE),
                        OPERATOR_SHORT_MESSAGE);

                assertEquals(0, dlq.size(), DLQ_RECORDS);
            } finally {
                stub.close();
                task.stop();
            }
        } finally {
            writer.closeLeftoverHelpers();
            writer.unregisterStatistics();
        }
    }
}
