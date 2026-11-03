package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.ClickHouseSinkConnector;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.db.ClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.CountingHelperClient;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Counts the helper clients {@link ClickHouseWriter} opens and closes through its {@code
 * createHelperClient} factory, on client V1 and V2, with scripted server answers and no network
 * except the loopback stub of the batch cases. Each helper is a {@link CountingHelperClient}, so no
 * Mockito is involved and the class stays in the Docker-free {@code testUnit} loop.
 */
class WriterHelperClientClosingUnitTest {

    private static final String PARAMETERIZED_NAME = "{displayName}[{index}] client_version={0}";
    private static final String LOOPBACK = "127.0.0.1";
    private static final String UNUSED_PORT = "8123";
    private static final String DATABASE = "default";
    private static final String TABLE_NAME = "writer_closing";
    private static final String SUPPORTED_VERSION = "26.10.3.7";
    private static final String ONE_PART_VERSION = "26";
    private static final String TOO_OLD_VERSION = "23.2";
    private static final String VERSION_FAILURE = "scripted version lookup failure";
    private static final String MAPPING_FAILURE = "scripted table mapping failure";
    private static final String CLOSE_FAILURE = "scripted helper client close failure";
    private static final String TABLE_REFRESH_SECONDS = "1";
    private static final String SCHEDULED_EXECUTOR_FIELD = "scheduledExecutor";
    private static final String OPEN_COUNT = "open count of helper clients";
    private static final String CLOSE_COUNT = "close count of the writer's helper client";
    private static final String PER_BATCH_CLOSE_COUNT = "close count of a per-batch helper client";
    private static final String EXECUTOR_NOT_STARTED = "the table sync executor must exist after a clean start";
    private static final String EXECUTOR_TERMINATED = "the table sync executor had not terminated when the helper client closed";
    private static final String TOPIC = "writer_closing_topic";
    private static final String QUERY_ID_FIRST = "writer-closing-q1";
    private static final String QUERY_ID_SECOND = "writer-closing-q2";
    private static final String QUERY_ID_V2 = "writer-closing-q3";
    private static final String CLIENT_V1 = "V1";
    private static final String CLIENT_V2 = "V2";

    private static final class CountingWriter extends ClickHouseWriter {

        private final UnaryOperator<CountingHelperClient> script;
        private final SinkTaskStatistics statistics;
        private final List<CountingHelperClient> created = new ArrayList<>();

        CountingWriter(UnaryOperator<CountingHelperClient> script) {
            this(script, new SinkTaskStatistics(null, 0));
        }

        private CountingWriter(UnaryOperator<CountingHelperClient> script, SinkTaskStatistics statistics) {
            super(statistics);
            this.script = script;
            this.statistics = statistics;
        }

        @Override
        protected ClickHouseHelperClient createHelperClient(ClickHouseHelperClient.ClickHouseClientBuilder builder) {
            CountingHelperClient helper = script.apply(new CountingHelperClient(builder));
            created.add(helper);
            return helper;
        }

        List<CountingHelperClient> created() {
            return created;
        }

        ScheduledExecutorService tableSyncExecutor() throws ReflectiveOperationException {
            Field field = ClickHouseWriter.class.getDeclaredField(SCHEDULED_EXECUTOR_FIELD);
            field.setAccessible(true);
            return (ScheduledExecutorService) field.get(this);
        }

        void insertJsonV1(List<Record> records, Table table, QueryIdentifier queryId) throws Exception {
            doInsertJsonV1(records, table, queryId);
        }

        void insertJsonV2(List<Record> records, Table table, QueryIdentifier queryId) throws Exception {
            doInsertJsonV2(records, table, queryId);
        }

        void unregisterStatistics() {
            statistics.unregisterMBean();
        }

        void closeLeftoverHelpers() {
            for (CountingHelperClient helper : created) {
                if (helper.closeCalls() == 0) {
                    helper.close();
                }
            }
        }
    }

    private static ClickHouseSinkConfig config(String clientVersion, Map<String, String> extra) {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConnector.HOSTNAME, LOOPBACK);
        props.put(ClickHouseSinkConnector.PORT, UNUSED_PORT);
        props.put(ClickHouseSinkConnector.SSL_ENABLED, "false");
        props.put(ClickHouseSinkConnector.CLIENT_VERSION, clientVersion);
        props.putAll(extra);
        return new ClickHouseSinkConfig(props);
    }

    private static UnaryOperator<CountingHelperClient> startsCleanly() {
        return helper -> helper.pingReturns(true)
                .versionReturns(SUPPORTED_VERSION)
                .tablesReturn(List.of(new Table(DATABASE, TABLE_NAME, false, List.of(), 0)));
    }

    private static void assertFailedStartClosesOnce(String clientVersion, UnaryOperator<CountingHelperClient> script) {
        CountingWriter writer = new CountingWriter(script);
        try {
            assertFalse(writer.start(config(clientVersion, Map.of())));
            assertEquals(1, writer.created().size(), OPEN_COUNT);
            CountingHelperClient helper = writer.created().get(0);
            assertEquals(1, helper.closeCalls(), CLOSE_COUNT);
            writer.stop();
            assertEquals(1, helper.closeCalls(), CLOSE_COUNT);
        } finally {
            writer.closeLeftoverHelpers();
        }
    }

    private static Record schemalessRecord(int id) {
        SinkRecord sinkRecord = new SinkRecord(TOPIC, 0, null, null, null, Map.of("id", id), id);
        return Record.convert(sinkRecord, false, ".", DATABASE, false);
    }

    private static Map<String, String> stubProps(StubClickHouseServer stub) {
        return Map.of(
                ClickHouseSinkConnector.PORT, String.valueOf(stub.port()),
                ClickHouseSinkConfig.BYPASS_FIELD_CLEANUP, "true");
    }

    private static ClickHouseSinkConfig stubConfig(String clientVersion, StubClickHouseServer stub) {
        return config(clientVersion, stubProps(stub));
    }

    private static Table batchTable() {
        return new Table(DATABASE, TABLE_NAME, false, List.of(), 0);
    }

    private static List<StubClickHouseServer.Request> posts(StubClickHouseServer stub) {
        return stub.requests().stream().filter(request -> "POST".equals(request.method())).toList();
    }

    private static String body(StubClickHouseServer.Request request) {
        return new String(request.body(), StandardCharsets.UTF_8);
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(strings = {CLIENT_V1, CLIENT_V2})
    void successfulStartThenStopClosesTheHelperClientOnce(String clientVersion) {
        CountingWriter writer = new CountingWriter(startsCleanly());
        try {
            assertTrue(writer.start(config(clientVersion, Map.of())));
            assertEquals(1, writer.created().size(), OPEN_COUNT);
            CountingHelperClient helper = writer.created().get(0);
            assertEquals(CLIENT_V2.equals(clientVersion), helper.isUseClientV2());
            writer.stop();
            assertEquals(1, helper.closeCalls(), CLOSE_COUNT);
        } finally {
            writer.closeLeftoverHelpers();
        }
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(strings = {CLIENT_V1, CLIENT_V2})
    void secondStopNeitherThrowsNorClosesAgain(String clientVersion) {
        CountingWriter writer = new CountingWriter(startsCleanly());
        try {
            assertTrue(writer.start(config(clientVersion, Map.of())));
            writer.stop();
            writer.stop();
            assertEquals(1, writer.created().size(), OPEN_COUNT);
            assertEquals(1, writer.created().get(0).closeCalls(), CLOSE_COUNT);
        } finally {
            writer.closeLeftoverHelpers();
        }
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(strings = {CLIENT_V1, CLIENT_V2})
    void stopWithoutStartCreatesNoHelperClient(String clientVersion) {
        CountingWriter writer = new CountingWriter(startsCleanly());
        writer.stop();
        assertEquals(0, writer.created().size(), OPEN_COUNT);
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(strings = {CLIENT_V1, CLIENT_V2})
    void failedPingClosesTheHelperClientAndALaterStopClosesNothingMore(String clientVersion) {
        assertFailedStartClosesOnce(clientVersion, helper -> helper.pingReturns(false));
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(strings = {CLIENT_V1, CLIENT_V2})
    void unreadableServerVersionClosesTheHelperClient(String clientVersion) {
        assertFailedStartClosesOnce(clientVersion, helper -> helper.pingReturns(true).versionReturns(ONE_PART_VERSION));
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(strings = {CLIENT_V1, CLIENT_V2})
    void serverOlderThanTheMinimumVersionClosesTheHelperClient(String clientVersion) {
        assertFailedStartClosesOnce(clientVersion, helper -> helper.pingReturns(true).versionReturns(TOO_OLD_VERSION));
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(strings = {CLIENT_V1, CLIENT_V2})
    void versionLookupFailureClosesTheHelperClient(String clientVersion) {
        assertFailedStartClosesOnce(
                clientVersion,
                helper -> helper.pingReturns(true).versionThrows(new IllegalStateException(VERSION_FAILURE)));
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(strings = {CLIENT_V1, CLIENT_V2})
    void noTablesWithoutDbTopicSplitClosesTheHelperClient(String clientVersion) {
        assertFailedStartClosesOnce(
                clientVersion,
                helper -> helper.pingReturns(true).versionReturns(SUPPORTED_VERSION).tablesReturn(List.of()));
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(strings = {CLIENT_V1, CLIENT_V2})
    void tableMappingFailureClosesTheHelperClientAndPropagatesTheSameException(String clientVersion) {
        IllegalStateException failure = new IllegalStateException(MAPPING_FAILURE);
        CountingWriter writer = new CountingWriter(
                helper -> helper.pingReturns(true).versionReturns(SUPPORTED_VERSION).tablesThrow(failure));
        try {
            IllegalStateException thrown =
                    assertThrows(IllegalStateException.class, () -> writer.start(config(clientVersion, Map.of())));
            assertEquals(1, writer.created().size(), OPEN_COUNT);
            CountingHelperClient helper = writer.created().get(0);
            assertEquals(1, helper.closeCalls(), CLOSE_COUNT);
            assertSame(failure, thrown);
            writer.stop();
            assertEquals(1, helper.closeCalls(), CLOSE_COUNT);
        } finally {
            writer.closeLeftoverHelpers();
        }
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(strings = {CLIENT_V1, CLIENT_V2})
    void stopClosesTheHelperClientOnlyAfterTheTableSyncExecutorTerminated(String clientVersion) throws Exception {
        CountingWriter writer = new CountingWriter(startsCleanly());
        try {
            assertTrue(writer.start(config(clientVersion, Map.of(ClickHouseSinkConfig.TABLE_REFRESH_INTERVAL, TABLE_REFRESH_SECONDS))));
            ScheduledExecutorService executor = writer.tableSyncExecutor();
            assertNotNull(executor, EXECUTOR_NOT_STARTED);
            CountingHelperClient helper = writer.created().get(0);
            AtomicReference<Boolean> terminatedAtClose = new AtomicReference<>();
            helper.onClose(() -> terminatedAtClose.set(executor.isTerminated()));
            writer.stop();
            assertEquals(1, helper.closeCalls(), CLOSE_COUNT);
            assertEquals(Boolean.TRUE, terminatedAtClose.get(), EXECUTOR_TERMINATED);
        } finally {
            writer.closeLeftoverHelpers();
        }
    }

    @Test
    void clientV1BatchesCloseTheirPerBatchHelperClientAndTheNodeStillServesTheInsert() throws Exception {
        try (StubClickHouseServer stub = new StubClickHouseServer(StubClickHouseServer.ok())) {
            CountingWriter writer = new CountingWriter(startsCleanly());
            try {
                assertTrue(writer.start(stubConfig(CLIENT_V1, stub)));
                writer.insertJsonV1(List.of(schemalessRecord(1)), batchTable(), new QueryIdentifier(TOPIC, QUERY_ID_FIRST));
                writer.insertJsonV1(List.of(schemalessRecord(2)), batchTable(), new QueryIdentifier(TOPIC, QUERY_ID_SECOND));
                assertEquals(3, writer.created().size(), OPEN_COUNT);
                assertEquals(1, writer.created().get(1).closeCalls(), PER_BATCH_CLOSE_COUNT);
                assertEquals(1, writer.created().get(2).closeCalls(), PER_BATCH_CLOSE_COUNT);
                assertEquals(0, writer.created().get(0).closeCalls(), CLOSE_COUNT);
                List<StubClickHouseServer.Request> posts = posts(stub);
                assertEquals(2, posts.size());
                assertTrue(posts.get(0).rawUri().contains("query_id=" + QUERY_ID_FIRST), posts.get(0).rawUri());
                assertTrue(posts.get(1).rawUri().contains("query_id=" + QUERY_ID_SECOND), posts.get(1).rawUri());
                assertTrue(body(posts.get(0)).contains("{\"id\":1}"));
                assertTrue(body(posts.get(1)).contains("{\"id\":2}"));
            } finally {
                writer.stop();
                writer.closeLeftoverHelpers();
                writer.unregisterStatistics();
            }
        }
    }

    @Test
    void clientV1BatchStillInsertsWhenClosingItsPerBatchHelperClientThrows() throws Exception {
        try (StubClickHouseServer stub = new StubClickHouseServer(StubClickHouseServer.ok())) {
            AtomicInteger built = new AtomicInteger();
            AtomicInteger closeAttempts = new AtomicInteger();
            CountingWriter writer = new CountingWriter(helper -> {
                if (built.getAndIncrement() == 0) {
                    return startsCleanly().apply(helper);
                }
                return helper.onClose(() -> {
                    if (closeAttempts.getAndIncrement() == 0) {
                        throw new IllegalStateException(CLOSE_FAILURE);
                    }
                });
            });
            try {
                assertTrue(writer.start(stubConfig(CLIENT_V1, stub)));
                writer.insertJsonV1(List.of(schemalessRecord(1)), batchTable(), new QueryIdentifier(TOPIC, QUERY_ID_FIRST));
                assertEquals(2, writer.created().size(), OPEN_COUNT);
                assertEquals(1, writer.created().get(1).closeCalls(), PER_BATCH_CLOSE_COUNT);
                List<StubClickHouseServer.Request> posts = posts(stub);
                assertEquals(1, posts.size());
                assertTrue(body(posts.get(0)).contains("{\"id\":1}"));
            } finally {
                writer.stop();
                // The scripted failure skipped the real close of the per-batch client, so close it again here.
                writer.created().get(1).close();
                writer.closeLeftoverHelpers();
                writer.unregisterStatistics();
            }
        }
    }

    @Test
    void clientV2BatchBuildsNoPerBatchHelperClient() throws Exception {
        try (StubClickHouseServer stub = new StubClickHouseServer(StubClickHouseServer.ok())) {
            CountingWriter writer = new CountingWriter(startsCleanly());
            try {
                assertTrue(writer.start(stubConfig(CLIENT_V2, stub)));
                writer.insertJsonV2(List.of(schemalessRecord(1)), batchTable(), new QueryIdentifier(TOPIC, QUERY_ID_V2));
                assertEquals(1, writer.created().size(), OPEN_COUNT);
                assertEquals(1, posts(stub).size());
            } finally {
                writer.stop();
                writer.closeLeftoverHelpers();
                writer.unregisterStatistics();
            }
        }
    }
}
