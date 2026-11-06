package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.ClickHouseSinkConnector;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.data.SchemaType;
import com.clickhouse.kafka.connect.sink.db.ClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.CountingHelperClient;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.db.helper.StubClickHouseServer;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;

/**
 * Shared pieces of the insert-bound pin classes: the three insert routes, small and large records, the
 * table, the connector config, a writer over a scripted helper client, and the checks on the failure a
 * bounded insert leaves behind. Large records carry a sentinel that must never reach an exception message or
 * a stack trace. It uses no mocking library and only APIs that exist before the bound is added, so the pin
 * classes can be run red against the unchanged production tree.
 */
final class InsertBoundPinFixtures {

    static final String SENTINEL = "INSERT-BOUND-SENTINEL-46";
    static final String LOOPBACK = "127.0.0.1";
    static final String DATABASE = "default";
    static final String SUPPORTED_VERSION = "26.10.3.7";
    static final String ID_FIELD = "id";
    static final String ID_TYPE = "Int32";
    static final String TEXT_FIELD = "s";
    static final String TEXT_TYPE = "String";
    static final int LARGE_PAYLOAD_BYTES = 1024 * 1024;

    private static final int ID_VALUE = 1;
    private static final int COLUMN_COUNT = 2;
    private static final String SMALL_TEXT = "short text";
    private static final char PADDING_CHAR = 'x';
    private static final String JSON_ROW_FORMAT = "{\"id\":%d,\"s\":\"%s\"}";
    private static final String POST = "POST";
    private static final String CLIENT_PHRASE_FORMAT = "client %s";
    private static final String BOUND_PHRASE_FORMAT = "clickhouseClientInsertTimeoutMs=%d";
    private static final String MESSAGE_CLIENT = "the message names the client version";
    private static final String MESSAGE_SETTING = "the message names the setting and the bound";
    private static final String MESSAGE_QUERY_ID = "the message names the query id";
    private static final String SENTINEL_IN_MESSAGE = "a record value must not reach a message in the failure chain";
    private static final String SENTINEL_IN_TRACE = "a record value must not reach a stack trace line";
    private static final Schema ROW_SCHEMA = SchemaBuilder.struct()
            .field(ID_FIELD, Schema.INT32_SCHEMA)
            .field(TEXT_FIELD, Schema.STRING_SCHEMA)
            .build();

    private InsertBoundPinFixtures() {}

    enum Route {
        JSON(SchemaType.SCHEMA_LESS),
        STRING(SchemaType.STRING_SCHEMA),
        ROW_BINARY(SchemaType.SCHEMA);

        private final SchemaType schemaType;

        Route(SchemaType schemaType) {
            this.schemaType = schemaType;
        }

        SchemaType schemaType() {
            return schemaType;
        }
    }

    static String topic(String prefix, String caseTag, Route route) {
        return prefix + caseTag + "_" + route.name().toLowerCase(Locale.ROOT);
    }

    static SinkRecord smallRecord(Route route, String topic) {
        return sinkRecord(route, topic, 0L, ID_VALUE, SMALL_TEXT);
    }

    /** {@code count} records at offsets 0..count-1; each text is the sentinel padded to {@link #LARGE_PAYLOAD_BYTES}. */
    static List<SinkRecord> largeRecords(Route route, String topic, int count) {
        String payload = SENTINEL + String.valueOf(PADDING_CHAR).repeat(LARGE_PAYLOAD_BYTES - SENTINEL.length());
        List<SinkRecord> records = new ArrayList<>(count);
        for (int offset = 0; offset < count; offset++) {
            records.add(sinkRecord(route, topic, offset, offset, payload));
        }
        return records;
    }

    private static SinkRecord sinkRecord(Route route, String topic, long offset, long idValue, String text) {
        int id = (int) idValue;
        return switch (route) {
            case JSON -> new SinkRecord(
                    topic, 0, null, null, null, Map.of(ID_FIELD, id, TEXT_FIELD, text), offset);
            case STRING -> new SinkRecord(
                    topic, 0, null, null, Schema.STRING_SCHEMA, String.format(JSON_ROW_FORMAT, id, text), offset);
            case ROW_BINARY -> new SinkRecord(
                    topic,
                    0,
                    null,
                    null,
                    ROW_SCHEMA,
                    new Struct(ROW_SCHEMA).put(ID_FIELD, id).put(TEXT_FIELD, text),
                    offset);
        };
    }

    static List<Record> convert(List<SinkRecord> sinkRecords) {
        List<Record> records = new ArrayList<>(sinkRecords.size());
        for (SinkRecord sinkRecord : sinkRecords) {
            records.add(Record.convert(sinkRecord, false, ".", DATABASE, false));
        }
        return records;
    }

    static Table table(String topic) {
        return new Table(
                DATABASE,
                topic,
                false,
                List.of(
                        Column.extractColumn(ID_FIELD, ID_TYPE, false, false, false),
                        Column.extractColumn(TEXT_FIELD, TEXT_TYPE, false, false, false)),
                COLUMN_COUNT);
    }

    static ClickHouseSinkConfig config(
            StubClickHouseServer stub, String clientVersion, String insertTimeoutMs, Map<String, String> extra) {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConnector.HOSTNAME, LOOPBACK);
        props.put(ClickHouseSinkConnector.PORT, String.valueOf(stub.port()));
        props.put(ClickHouseSinkConnector.SSL_ENABLED, "false");
        props.put(ClickHouseSinkConnector.CLIENT_VERSION, clientVersion);
        props.put(ClickHouseSinkConfig.BYPASS_FIELD_CLEANUP, "true");
        props.put(ClickHouseSinkConfig.CLICKHOUSE_CLIENT_INSERT_TIMEOUT_MS, insertTimeoutMs);
        props.putAll(extra);
        return new ClickHouseSinkConfig(props);
    }

    static List<StubClickHouseServer.Request> posts(StubClickHouseServer stub) {
        return stub.requests().stream()
                .filter(request -> POST.equals(request.method()))
                .toList();
    }

    /** A real writer over a helper client with scripted ping, version and table, so no server answers them. */
    static final class PinWriter extends ClickHouseWriter {

        private final String topic;
        private final SinkTaskStatistics statistics;
        private final List<CountingHelperClient> created = new ArrayList<>();

        PinWriter(String topic) {
            this(topic, new SinkTaskStatistics(null, 0));
        }

        private PinWriter(String topic, SinkTaskStatistics statistics) {
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

        /** Stops the writer, then releases what a stopped writer leaves behind. */
        void finish() {
            stop();
            closeLeftoverHelpers();
            unregisterStatistics();
        }
    }

    /** The first {@link RetriableException} in the cause chain, the throwable itself included, or null. */
    static RetriableException retriableInChain(Throwable thrown) {
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            if (cause instanceof RetriableException retriable) {
                return retriable;
            }
        }
        return null;
    }

    /** Every message in the cause chain, newline separated. */
    static String chainText(Throwable thrown) {
        StringBuilder text = new StringBuilder();
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            text.append(cause.getMessage()).append('\n');
        }
        return text.toString();
    }

    /**
     * Checks that a bound failure names the client version, the setting with its bound in milliseconds and,
     * when {@code queryId} is not null, the query id.
     */
    static void assertBoundMessage(RetriableException retriable, String clientVersion, long bound, String queryId) {
        String message = String.valueOf(retriable.getMessage());
        assertTrue(message.contains(String.format(CLIENT_PHRASE_FORMAT, clientVersion)), MESSAGE_CLIENT);
        assertTrue(message.contains(String.format(BOUND_PHRASE_FORMAT, bound)), MESSAGE_SETTING);
        if (queryId != null) {
            assertTrue(message.contains(queryId), MESSAGE_QUERY_ID);
        }
    }

    static void assertNoSentinel(Throwable thrown) {
        assertFalse(chainText(thrown).contains(SENTINEL), SENTINEL_IN_MESSAGE);
        StringWriter trace = new StringWriter();
        thrown.printStackTrace(new PrintWriter(trace));
        assertFalse(trace.toString().contains(SENTINEL), SENTINEL_IN_TRACE);
    }

    static List<Thread> liveThreadsNamed(String name) {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> name.equals(thread.getName()) && thread.isAlive())
                .toList();
    }
}
