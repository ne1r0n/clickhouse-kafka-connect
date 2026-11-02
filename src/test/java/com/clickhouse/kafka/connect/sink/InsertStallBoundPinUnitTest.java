package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.ClientConfigProperties;
import com.clickhouse.kafka.connect.ClickHouseSinkConnector;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.data.SchemaType;
import com.clickhouse.kafka.connect.sink.db.ClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.CountingHelperClient;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.db.helper.StubClickHouseServer;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import com.clickhouse.kafka.connect.util.Utils;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free stall pins for PIN-01, TMO-01 and TMO-02. A real {@link ClickHouseWriter} on client V2
 * with {@code async=false} inserts into a loopback stub that stops answering, and each insert must end
 * within a preemptive bound. The writer starts against a {@link CountingHelperClient} with scripted
 * ping, version and table, so no server is needed.
 */
class InsertStallBoundPinUnitTest {

    private static final String ROUTE_NAME = "{displayName}[{index}] route={0}";
    private static final String ROUTE_AND_TOLERANCE_NAME = "{displayName}[{index}] route={0} errors.tolerance={1}";
    private static final String LOOPBACK = "127.0.0.1";
    private static final String DATABASE = "default";
    private static final String CLIENT_V2 = "V2";
    private static final String SUPPORTED_VERSION = "26.10.3.7";
    private static final String ID_FIELD = "id";
    private static final String ID_TYPE = "Int32";
    private static final int ID_VALUE = 1;
    private static final String JSON_ROW = "{\"id\":1}";
    private static final String TOPIC_PREFIX = "insert_stall_";
    private static final String QUERY_ID = "insert-stall-q1";
    private static final String INSERT_TIMEOUT_MS = "300";
    private static final Duration STALL_BOUND = Duration.ofSeconds(10);
    private static final String STALL_BOUND_MESSAGE = "a stalled client V2 insert must end within the bound";
    private static final String ROUTE_PREMISE = "the record must take this route's insert method";
    private static final String ROOT_CAUSE = "root cause of the stalled insert";
    private static final String RETRIABLE = "classification of the stalled insert";
    private static final String ONE_POST = "POST count at the stub; a second one would be a client-side retry";
    private static final String DLQ_RECORDS = "DLQ record count";
    private static final String NO_WIRE_OPTION = "the per-insert socket_timeout must not reach the server";
    private static final String OPERATOR_LONG = "socket_timeout=6000";
    private static final Duration ANSWER_DELAY = Duration.ofMillis(1000);
    private static final String OPERATOR_KEPT = "the operator's socket_timeout must outlast the delayed answer";
    private static final String OPERATOR_SHORT = "socket_timeout=200";
    private static final String INSERT_TIMEOUT_LONG_MS = "20000";
    private static final Duration OPERATOR_SHORT_BOUND = Duration.ofSeconds(3);
    private static final String OPERATOR_SHORT_MESSAGE = "the operator's shorter socket_timeout must end the stall first";
    private static final String SOCKET_TIMEOUT_KEY = ClientConfigProperties.SOCKET_OPERATION_TIMEOUT.getKey();

    enum Route {
        JSON(SchemaType.SCHEMA_LESS),
        STRING(SchemaType.STRING_SCHEMA),
        ROW_BINARY(SchemaType.SCHEMA);

        private final SchemaType schemaType;

        Route(SchemaType schemaType) {
            this.schemaType = schemaType;
        }
    }

    private static SinkRecord sinkRecord(Route route, String topic) {
        return switch (route) {
            case JSON -> new SinkRecord(topic, 0, null, null, null, Map.of(ID_FIELD, ID_VALUE), 0L);
            case STRING -> new SinkRecord(topic, 0, null, null, Schema.STRING_SCHEMA, JSON_ROW, 0L);
            case ROW_BINARY -> {
                Schema schema = SchemaBuilder.struct().field(ID_FIELD, Schema.INT32_SCHEMA).build();
                yield new SinkRecord(topic, 0, null, null, schema, new Struct(schema).put(ID_FIELD, ID_VALUE), 0L);
            }
        };
    }

    private static String topic(String caseTag, Route route) {
        return TOPIC_PREFIX + caseTag + "_" + route.name().toLowerCase(Locale.ROOT);
    }

    private static Table table(String topic) {
        return new Table(
                DATABASE, topic, false, List.of(Column.extractColumn(ID_FIELD, ID_TYPE, false, false, false)), 1);
    }

    private static ClickHouseSinkConfig config(StubClickHouseServer stub, Map<String, String> extra) {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConnector.HOSTNAME, LOOPBACK);
        props.put(ClickHouseSinkConnector.PORT, String.valueOf(stub.port()));
        props.put(ClickHouseSinkConnector.SSL_ENABLED, "false");
        props.put(ClickHouseSinkConnector.CLIENT_VERSION, CLIENT_V2);
        props.put(ClickHouseSinkConfig.BYPASS_FIELD_CLEANUP, "true");
        props.put(ClickHouseSinkConfig.CLICKHOUSE_CLIENT_INSERT_TIMEOUT_MS, INSERT_TIMEOUT_MS);
        props.putAll(extra);
        return new ClickHouseSinkConfig(props);
    }

    private static List<StubClickHouseServer.Request> posts(StubClickHouseServer stub) {
        return stub.requests().stream()
                .filter(request -> "POST".equals(request.method()))
                .toList();
    }

    private static final class StallPinWriter extends ClickHouseWriter {

        private final String topic;
        private final SinkTaskStatistics statistics;
        private final List<CountingHelperClient> created = new ArrayList<>();

        StallPinWriter(String topic) {
            this(topic, new SinkTaskStatistics(null, 0));
        }

        private StallPinWriter(String topic, SinkTaskStatistics statistics) {
            super(statistics);
            this.topic = topic;
            this.statistics = statistics;
        }

        @Override
        protected ClickHouseHelperClient createHelperClient(ClickHouseHelperClient.ClickHouseClientBuilder builder) {
            CountingHelperClient helper = new CountingHelperClient(builder)
                    .pingReturns(true)
                    .versionReturns(SUPPORTED_VERSION)
                    .tablesReturn(List.of(table(topic)));
            created.add(helper);
            return helper;
        }

        void closeLeftoverHelpers() {
            for (CountingHelperClient helper : created) {
                if (helper.closeCalls() == 0) {
                    helper.close();
                }
            }
        }

        void unregisterStatistics() {
            statistics.unregisterMBean();
        }
    }

    @ParameterizedTest(name = ROUTE_NAME)
    @EnumSource(Route.class)
    void stalledV2InsertEndsWithinTheBoundWithARetriableRootCause(Route route) throws Exception {
        String topic = topic("bound", route);
        SinkRecord sinkRecord = sinkRecord(route, topic);
        Record record = Record.convert(sinkRecord, false, ".", DATABASE, false);
        assertEquals(route.schemaType, record.getSchemaType(), ROUTE_PREMISE);

        StallPinWriter writer = new StallPinWriter(topic);
        try (StubClickHouseServer stub = StubClickHouseServer.stalling()) {
            ClickHouseSinkConfig config = config(stub, Map.of());
            assertTrue(writer.start(config));

            Exception thrown = assertTimeoutPreemptively(
                    STALL_BOUND,
                    () -> assertThrows(
                            Exception.class,
                            () -> writer.doInsert(List.of(record), new QueryIdentifier(topic, QUERY_ID))),
                    STALL_BOUND_MESSAGE);

            assertInstanceOf(SocketTimeoutException.class, Utils.getRootCause(thrown, true), ROOT_CAUSE);
            assertThrows(
                    RetriableException.class,
                    () -> Utils.handleException(
                            thrown,
                            false,
                            config.isRetryOnSocketException(),
                            config.getAdditionalRetriableErrorCodes(),
                            List.of(sinkRecord)),
                    RETRIABLE);
            assertEquals(1, posts(stub).size(), ONE_POST);
            assertFalse(posts(stub).get(0).rawUri().contains(SOCKET_TIMEOUT_KEY), NO_WIRE_OPTION);
        } finally {
            writer.stop();
            writer.closeLeftoverHelpers();
            writer.unregisterStatistics();
        }
    }

    static Stream<Arguments> routesAndTolerances() {
        return Stream.of(ClickHouseSinkConfig.ERROR_TOLERANCE_NONE, ClickHouseSinkConfig.ERROR_TOLERANCE_ALL)
                .flatMap(tolerance -> Arrays.stream(Route.values()).map(route -> Arguments.of(route, tolerance)));
    }

    @ParameterizedTest(name = ROUTE_AND_TOLERANCE_NAME)
    @MethodSource("routesAndTolerances")
    void stalledV2InsertThroughPutIsRetriableWithNoDlqRecord(Route route, String tolerance) throws Exception {
        String topic = topic("put_" + tolerance, route);
        SinkRecord sinkRecord = sinkRecord(route, topic);
        Record record = Record.convert(sinkRecord, false, ".", DATABASE, false);
        assertEquals(route.schemaType, record.getSchemaType(), ROUTE_PREMISE);

        InMemoryDLQ dlq = new InMemoryDLQ();
        StallPinWriter writer = new StallPinWriter(topic);
        try (StubClickHouseServer stub = StubClickHouseServer.stalling()) {
            ClickHouseSinkConfig config = config(stub, Map.of(ClickHouseSinkConfig.ERRORS_TOLERANCE, tolerance));
            ProxySinkTask task = new ProxySinkTask(config, dlq, writer);
            try {
                task.start();

                RetriableException thrown = assertTimeoutPreemptively(
                        STALL_BOUND,
                        () -> assertThrows(RetriableException.class, () -> task.put(List.of(sinkRecord)), RETRIABLE),
                        STALL_BOUND_MESSAGE);

                assertEquals(0, dlq.size(), DLQ_RECORDS);
                assertInstanceOf(SocketTimeoutException.class, Utils.getRootCause(thrown, true), ROOT_CAUSE);
                assertEquals(1, posts(stub).size(), ONE_POST);
            } finally {
                stub.close();
                task.stop();
            }
        } finally {
            writer.closeLeftoverHelpers();
            writer.unregisterStatistics();
        }
    }

    @ParameterizedTest(name = ROUTE_NAME)
    @EnumSource(Route.class)
    void operatorSocketTimeoutLongerThanTheInsertTimeoutStillApplies(Route route) throws Exception {
        String topic = topic("operator_long", route);
        Record record = Record.convert(sinkRecord(route, topic), false, ".", DATABASE, false);

        StallPinWriter writer = new StallPinWriter(topic);
        try (StubClickHouseServer stub = StubClickHouseServer.okAfter(ANSWER_DELAY)) {
            ClickHouseSinkConfig config =
                    config(stub, Map.of(ClickHouseSinkConfig.JDBC_CONNECTION_PROPERTIES, OPERATOR_LONG));
            assertTrue(writer.start(config));

            assertTimeoutPreemptively(
                    STALL_BOUND,
                    () -> assertDoesNotThrow(
                            () -> writer.doInsert(List.of(record), new QueryIdentifier(topic, QUERY_ID)),
                            OPERATOR_KEPT),
                    STALL_BOUND_MESSAGE);

            assertEquals(1, posts(stub).size(), ONE_POST);
        } finally {
            writer.stop();
            writer.closeLeftoverHelpers();
            writer.unregisterStatistics();
        }
    }

    @ParameterizedTest(name = ROUTE_NAME)
    @EnumSource(Route.class)
    void operatorSocketTimeoutShorterThanTheInsertTimeoutStillApplies(Route route) throws Exception {
        String topic = topic("operator_short", route);
        Record record = Record.convert(sinkRecord(route, topic), false, ".", DATABASE, false);

        StallPinWriter writer = new StallPinWriter(topic);
        try (StubClickHouseServer stub = StubClickHouseServer.stalling()) {
            ClickHouseSinkConfig config = config(
                    stub,
                    Map.of(
                            ClickHouseSinkConfig.JDBC_CONNECTION_PROPERTIES,
                            OPERATOR_SHORT,
                            ClickHouseSinkConfig.CLICKHOUSE_CLIENT_INSERT_TIMEOUT_MS,
                            INSERT_TIMEOUT_LONG_MS));
            assertTrue(writer.start(config));

            Exception thrown = assertTimeoutPreemptively(
                    OPERATOR_SHORT_BOUND,
                    () -> assertThrows(
                            Exception.class,
                            () -> writer.doInsert(List.of(record), new QueryIdentifier(topic, QUERY_ID))),
                    OPERATOR_SHORT_MESSAGE);

            assertInstanceOf(SocketTimeoutException.class, Utils.getRootCause(thrown, true), ROOT_CAUSE);
            assertEquals(1, posts(stub).size(), ONE_POST);
        } finally {
            writer.stop();
            writer.closeLeftoverHelpers();
            writer.unregisterStatistics();
        }
    }
}
