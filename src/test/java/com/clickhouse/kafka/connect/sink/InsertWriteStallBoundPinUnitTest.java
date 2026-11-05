package com.clickhouse.kafka.connect.sink;

import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.assertBoundMessage;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.assertNoSentinel;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.chainText;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.config;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.convert;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.largeRecords;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.liveThreadsNamed;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.posts;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.retriableInChain;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.smallRecord;
import static com.clickhouse.kafka.connect.sink.InsertBoundPinFixtures.topic;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.clickhouse.kafka.connect.util.Utils;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * Docker-free pins for the client V2 write side of {@code clickhouseClientInsertTimeoutMs}. A real
 * {@code ClickHouseWriter} on client V2 inserts into raw loopback stubs that stop reading the request body,
 * read it slowly, or stream progress headers, and each insert must end within a preemptive bound, or run to its
 * natural end where the setting leaves it alone. The operator's {@code socket_timeout} keeps ownership of the
 * transport limits.
 */
class InsertWriteStallBoundPinUnitTest {

    private static final String ROUTE_NAME = "{displayName}[{index}] route={0}";
    private static final String ROUTE_AND_TOLERANCE_NAME = "{displayName}[{index}] route={0} errors.tolerance={1}";
    private static final String TOLERANCE_NAME = "{displayName}[{index}] errors.tolerance={0}";
    private static final String CLIENT_V2 = "V2";
    private static final String TOPIC_PREFIX = "insert_write_stall_";
    private static final String QUERY_ID = "insert-write-stall-q1";
    private static final String SECOND_QUERY_ID = "insert-write-stall-q2";
    private static final int LARGE_RECORDS = 64;
    private static final String INSERT_TIMEOUT_MS = "1000";
    private static final long INSERT_BOUND_MS = 1000L;
    private static final String PROGRESS_INSERT_TIMEOUT_MS = "1500";
    private static final String LATE_FIRE_INSERT_TIMEOUT_MS = "300";
    private static final String OPERATOR_LONG = "socket_timeout=60000";
    private static final String OPERATOR_SHORT = "socket_timeout=200";
    private static final String INSERT_TIMEOUT_LONG_MS = "20000";
    private static final Duration GUARD = Duration.ofSeconds(10);
    private static final Duration PROGRESS_GUARD = Duration.ofSeconds(15);
    private static final Duration OPERATOR_SHORT_GUARD = Duration.ofSeconds(3);
    private static final Duration RUNNING_OBSERVATION = Duration.ofSeconds(3);
    private static final Duration RELEASE_WAIT = Duration.ofSeconds(5);
    private static final Duration PROGRESS_INTERVAL = Duration.ofMillis(100);
    private static final Duration PROGRESS_TOTAL = Duration.ofSeconds(6);
    private static final Duration LATE_FIRE_PAUSE = Duration.ofMillis(600);
    private static final long PROGRESS_MIN_ELAPSED_MS = 5000L;
    private static final int SLOW_BYTES_PER_TICK = 65536;
    private static final Duration SLOW_TICK = Duration.ofMillis(20);
    private static final String WATCHDOG_THREAD_NAME = "clickhouse-insert-watchdog";
    private static final int EXPECTED_POSTS = 1;
    private static final int EXPECTED_POSTS_AFTER_TWO_INSERTS = 2;
    private static final int EXPECTED_POSTS_AFTER_A_RETRY = 2;
    private static final int RETRY_RECORDS = 16;

    private static final String BOUND_PHRASE = "clickhouseClientInsertTimeoutMs=" + INSERT_BOUND_MS;
    private static final String DEADLINE_IN_CHAIN = "the bound's own message is in the failure chain";
    private static final String GUARD_MESSAGE = "a client V2 insert must end within the bound";
    private static final String ROUTE_PREMISE = "the record must take this route's insert method";
    private static final String RETRIABLE = "a body not sent within the bound is retriable";
    private static final String ELAPSED = "the insert must not end before the bound";
    private static final String ROOT_CAUSE = "a swallowed body would end in a read timeout, which is not this bound";
    private static final String ONE_POST = "POST count at the stub; a second one would be a client-side retry";
    private static final String DLQ_RECORDS = "DLQ record count";
    private static final String UPLOAD_PROGRESSING = "the upload must have been progressing when the bound cut it";
    private static final String PROGRESS_RUNS_PAST = "a response that streams progress after the body is sent keeps running";
    private static final String PROGRESS_ELAPSED = "the insert ran past the bound";
    private static final String OPERATOR_STILL_RUNNING = "the operator's longer socket_timeout leaves the write stall alone";
    private static final String OPERATOR_NO_CONNECTOR_BOUND = "no connector bound fires when the operator named socket_timeout";
    private static final String OPERATOR_SHORT_MESSAGE = "the operator's shorter socket_timeout must end the stall first";
    private static final String WATCHDOG_CREATED = "one armed insert creates exactly one watchdog thread";
    private static final String WATCHDOG_DAEMON = "the watchdog thread must be a daemon";
    private static final String WATCHDOG_ENDED = "stop() must end the watchdog thread";
    private static final String FIRST_INSERT = "the first insert succeeds";
    private static final String SECOND_INSERT = "a second insert on the same writer after the first one's bound succeeds";
    private static final String RETRY_HAPPENED =
            "POST count at the stub; two means the client retried the insert and the retry is what was cut";

    static Stream<Arguments> routesAndTolerances() {
        return Stream.of(ClickHouseSinkConfig.ERROR_TOLERANCE_NONE, ClickHouseSinkConfig.ERROR_TOLERANCE_ALL)
                .flatMap(tolerance -> Arrays.stream(Route.values()).map(route -> Arguments.of(route, tolerance)));
    }

    private static List<Record> records(Route route, String topic, List<SinkRecord> sinkRecords) {
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

    private static void assertDeadlineFailure(Exception thrown) {
        RetriableException retriable = retriableInChain(thrown);
        assertNotNull(retriable, RETRIABLE);
        assertBoundMessage(retriable, CLIENT_V2, INSERT_BOUND_MS, QUERY_ID);
        assertFalse(Utils.getRootCause(thrown, true) instanceof SocketTimeoutException, ROOT_CAUSE);
        assertNoSentinel(thrown);
    }

    private static void putMustBeRetriableWithNoDlqRecord(
            Route route, String tolerance, StubClickHouseServer stub, PinWriter writer, String topic)
            throws Exception {
        List<SinkRecord> sinkRecords = largeRecords(route, topic, LARGE_RECORDS);
        records(route, topic, sinkRecords);
        InMemoryDLQ dlq = new InMemoryDLQ();
        ClickHouseSinkConfig config = config(
                stub, CLIENT_V2, INSERT_TIMEOUT_MS, Map.of(ClickHouseSinkConfig.ERRORS_TOLERANCE, tolerance));
        ProxySinkTask task = new ProxySinkTask(config, dlq, writer);
        try {
            task.start();
            RetriableException thrown = assertTimeoutPreemptively(
                    GUARD,
                    () -> assertThrows(RetriableException.class, () -> task.put(sinkRecords), RETRIABLE),
                    GUARD_MESSAGE);
            assertEquals(0, dlq.size(), DLQ_RECORDS);
            assertTrue(chainText(thrown).contains(BOUND_PHRASE), DEADLINE_IN_CHAIN);
            assertEquals(EXPECTED_POSTS, posts(stub).size(), ONE_POST);
            assertNoSentinel(thrown);
        } finally {
            stub.close();
            task.stop();
        }
    }

    @ParameterizedTest(name = ROUTE_NAME)
    @EnumSource(Route.class)
    void writeStalledV2InsertEndsWithinTheBoundAsRetriable(Route route) throws Exception {
        String topic = topic(TOPIC_PREFIX, "stall", route);
        List<Record> records = records(route, topic, largeRecords(route, topic, LARGE_RECORDS));

        PinWriter writer = new PinWriter(topic);
        try (StubClickHouseServer stub = StubClickHouseServer.notReadingBody()) {
            assertTrue(writer.start(config(stub, CLIENT_V2, INSERT_TIMEOUT_MS, Map.of())));

            long started = System.nanoTime();
            Exception thrown = insertFailure(writer, records, topic, GUARD);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

            assertDeadlineFailure(thrown);
            assertTrue(elapsedMs >= INSERT_BOUND_MS, ELAPSED);
            assertEquals(EXPECTED_POSTS, posts(stub).size(), ONE_POST);
        } finally {
            writer.finish();
        }
    }

    @ParameterizedTest(name = ROUTE_AND_TOLERANCE_NAME)
    @MethodSource("routesAndTolerances")
    void writeStalledV2InsertThroughPutIsRetriableWithNoDlqRecord(Route route, String tolerance) throws Exception {
        String topic = topic(TOPIC_PREFIX, "put_stall_" + tolerance, route);
        PinWriter writer = new PinWriter(topic);
        try (StubClickHouseServer stub = StubClickHouseServer.notReadingBody()) {
            putMustBeRetriableWithNoDlqRecord(route, tolerance, stub, writer, topic);
        } finally {
            writer.closeLeftoverHelpers();
            writer.unregisterStatistics();
        }
    }

    @Test
    void slowV2UploadStillSendingAtTheBoundFailsAsRetriable() throws Exception {
        Route route = Route.JSON;
        String topic = topic(TOPIC_PREFIX, "slow_upload", route);
        List<Record> records = records(route, topic, largeRecords(route, topic, LARGE_RECORDS));

        PinWriter writer = new PinWriter(topic);
        try (StubClickHouseServer stub = StubClickHouseServer.readingBodySlowly(SLOW_BYTES_PER_TICK, SLOW_TICK)) {
            assertTrue(writer.start(config(stub, CLIENT_V2, INSERT_TIMEOUT_MS, Map.of())));

            Exception thrown = insertFailure(writer, records, topic, GUARD);

            assertDeadlineFailure(thrown);
            assertTrue(stub.bodyBytesRead() > 0, UPLOAD_PROGRESSING);
        } finally {
            writer.finish();
        }
    }

    @ParameterizedTest(name = TOLERANCE_NAME)
    @ValueSource(strings = {ClickHouseSinkConfig.ERROR_TOLERANCE_NONE, ClickHouseSinkConfig.ERROR_TOLERANCE_ALL})
    void slowV2UploadThroughPutIsRetriableWithNoDlqRecord(String tolerance) throws Exception {
        Route route = Route.JSON;
        String topic = topic(TOPIC_PREFIX, "put_slow_" + tolerance, route);
        PinWriter writer = new PinWriter(topic);
        try (StubClickHouseServer stub = StubClickHouseServer.readingBodySlowly(SLOW_BYTES_PER_TICK, SLOW_TICK)) {
            putMustBeRetriableWithNoDlqRecord(route, tolerance, stub, writer, topic);
        } finally {
            writer.closeLeftoverHelpers();
            writer.unregisterStatistics();
        }
    }

    @Test
    void progressStreamedWhileTheBodyIsStillSentIsCutAtTheBound() throws Exception {
        Route route = Route.JSON;
        String topic = topic(TOPIC_PREFIX, "progress_while_sending", route);
        List<Record> records = records(route, topic, largeRecords(route, topic, LARGE_RECORDS));

        PinWriter writer = new PinWriter(topic);
        try (StubClickHouseServer stub =
                StubClickHouseServer.progressWhileReadingBodySlowly(SLOW_BYTES_PER_TICK, SLOW_TICK, PROGRESS_INTERVAL)) {
            assertTrue(writer.start(config(stub, CLIENT_V2, INSERT_TIMEOUT_MS, Map.of())));

            Exception thrown = insertFailure(writer, records, topic, GUARD);

            assertDeadlineFailure(thrown);
        } finally {
            writer.finish();
        }
    }

    @Test
    void progressStreamingV2InsertRunsPastTheBoundOnceTheBodyIsSent() throws Exception {
        Route route = Route.JSON;
        String topic = topic(TOPIC_PREFIX, "progress_after_sent", route);
        List<Record> records = records(route, topic, List.of(smallRecord(route, topic)));

        PinWriter writer = new PinWriter(topic);
        try (StubClickHouseServer stub = StubClickHouseServer.progressHeaders(PROGRESS_INTERVAL, PROGRESS_TOTAL)) {
            assertTrue(writer.start(config(stub, CLIENT_V2, PROGRESS_INSERT_TIMEOUT_MS, Map.of())));

            long started = System.nanoTime();
            assertTimeoutPreemptively(
                    PROGRESS_GUARD,
                    () -> assertDoesNotThrow(
                            () -> writer.doInsert(records, new QueryIdentifier(topic, QUERY_ID)), PROGRESS_RUNS_PAST),
                    GUARD_MESSAGE);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

            assertTrue(elapsedMs >= PROGRESS_MIN_ELAPSED_MS, PROGRESS_ELAPSED);
            assertEquals(EXPECTED_POSTS, posts(stub).size(), ONE_POST);
        } finally {
            writer.finish();
        }
    }

    @Test
    void operatorSocketTimeoutLeavesAWriteStallToTheOperator() throws Exception {
        Route route = Route.JSON;
        String topic = topic(TOPIC_PREFIX, "operator_long", route);
        List<Record> records = records(route, topic, largeRecords(route, topic, LARGE_RECORDS));

        PinWriter writer = new PinWriter(topic);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (StubClickHouseServer stub = StubClickHouseServer.notReadingBody()) {
            assertTrue(writer.start(config(
                    stub,
                    CLIENT_V2,
                    INSERT_TIMEOUT_MS,
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
    void operatorShorterSocketTimeoutEndsAV2QuietStallThroughPut(String tolerance) throws Exception {
        Route route = Route.JSON;
        String topic = topic(TOPIC_PREFIX, "operator_short_" + tolerance, route);
        SinkRecord sinkRecord = smallRecord(route, topic);

        InMemoryDLQ dlq = new InMemoryDLQ();
        PinWriter writer = new PinWriter(topic);
        try (StubClickHouseServer stub = StubClickHouseServer.stalling()) {
            ClickHouseSinkConfig config = config(
                    stub,
                    CLIENT_V2,
                    INSERT_TIMEOUT_LONG_MS,
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

    @Test
    void insertWatchdogIsADaemonThreadEndedByStop() throws Exception {
        Route route = Route.JSON;
        String topic = topic(TOPIC_PREFIX, "watchdog", route);
        List<Record> records = records(route, topic, List.of(smallRecord(route, topic)));
        Set<Thread> before = new HashSet<>(liveThreadsNamed(WATCHDOG_THREAD_NAME));

        PinWriter writer = new PinWriter(topic);
        try (StubClickHouseServer stub = new StubClickHouseServer(StubClickHouseServer.ok())) {
            assertTrue(writer.start(config(stub, CLIENT_V2, INSERT_TIMEOUT_MS, Map.of())));
            writer.doInsert(records, new QueryIdentifier(topic, QUERY_ID));

            List<Thread> created = liveThreadsNamed(WATCHDOG_THREAD_NAME).stream()
                    .filter(thread -> !before.contains(thread))
                    .toList();
            assertEquals(1, created.size(), WATCHDOG_CREATED);
            assertTrue(created.get(0).isDaemon(), WATCHDOG_DAEMON);

            writer.stop();
            created.get(0).join(RELEASE_WAIT.toMillis());
            assertFalse(created.get(0).isAlive(), WATCHDOG_ENDED);
        } finally {
            writer.finish();
        }
    }

    /**
     * Pins that a writer still inserts after the bound of its first insert has elapsed. It does not pin a late
     * fire against a finished request: {@code disarm()} cancels the task at the end of every insert, so no late
     * fire happens that a test could observe, and the mutation that never disarms leaves this case green.
     */
    @Test
    void secondInsertAfterTheFirstInsertsBoundHasElapsedSucceeds() throws Exception {
        Route route = Route.JSON;
        String topic = topic(TOPIC_PREFIX, "late_fire", route);
        List<Record> records = records(route, topic, List.of(smallRecord(route, topic)));

        PinWriter writer = new PinWriter(topic);
        try (StubClickHouseServer stub = new StubClickHouseServer(StubClickHouseServer.ok())) {
            assertTrue(writer.start(config(stub, CLIENT_V2, LATE_FIRE_INSERT_TIMEOUT_MS, Map.of())));

            assertDoesNotThrow(() -> writer.doInsert(records, new QueryIdentifier(topic, QUERY_ID)), FIRST_INSERT);
            Thread.sleep(LATE_FIRE_PAUSE.toMillis());
            assertDoesNotThrow(
                    () -> writer.doInsert(records, new QueryIdentifier(topic, SECOND_QUERY_ID)), SECOND_INSERT);

            assertEquals(EXPECTED_POSTS_AFTER_TWO_INSERTS, posts(stub).size(), ONE_POST);
        } finally {
            writer.finish();
        }
    }

    @Test
    void clientInternalRetryOfAnInsertGetsAFreshWriteDeadline() throws Exception {
        Route route = Route.JSON;
        String topic = topic(TOPIC_PREFIX, "retry_stall", route);
        List<Record> records = records(route, topic, largeRecords(route, topic, RETRY_RECORDS));

        PinWriter writer = new PinWriter(topic);
        try (StubClickHouseServer stub = StubClickHouseServer.closingFirstPostAfterItsBodyThenNotReadingBody()) {
            assertTrue(writer.start(config(stub, CLIENT_V2, INSERT_TIMEOUT_MS, Map.of())));

            Exception thrown = insertFailure(writer, records, topic, GUARD);

            assertDeadlineFailure(thrown);
            assertEquals(EXPECTED_POSTS_AFTER_A_RETRY, posts(stub).size(), RETRY_HAPPENED);
        } finally {
            writer.finish();
        }
    }
}
