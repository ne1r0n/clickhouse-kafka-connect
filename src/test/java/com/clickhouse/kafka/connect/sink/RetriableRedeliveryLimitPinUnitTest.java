package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clickhouse.client.ClickHouseException;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.db.InMemoryDBWriter;
import com.clickhouse.kafka.connect.sink.dlq.ErrorReporter;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import com.clickhouse.kafka.connect.sink.helper.SchemalessTestData;
import com.clickhouse.kafka.connect.sink.util.ServerExceptions;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free pin for RTY-01: what the connector does at the {@link ProxySinkTask#put} layer with a writer
 * failure, under both {@code errors.tolerance} values, and that a retriable one is redelivered with no bound
 * at the connector layer. {@code errors.retry.timeout} and {@code retryCount} are set in every case and bound
 * nothing: each retriable shape throws a {@code RetriableException} on every one of {@link #REDELIVERIES}
 * consecutive redeliveries of the same batch, through {@code ProxySinkTask.put} and through
 * {@code ChunkFlusher.putDirect}, with the writer called every time and nothing sent to the dead letter queue.
 *
 * <p>What Kafka Connect does with a {@code RetriableException} from {@code put} is documented in
 * {@code docs/FORK-DELTA.md} and not tested here.
 *
 * <p>The expectations are literal. A changed row is a behaviour change to decide on, not an expectation to
 * update.
 *
 * <p>Extends nothing and carries no tag so it stays in {@code testUnit}.
 */
class RetriableRedeliveryLimitPinUnitTest {

    private static final String TOPIC = "rty01_redelivery_limit";
    private static final String HOSTNAME_VALUE = "localhost";
    private static final String CONNECTOR_RETRY_TIMEOUT_VALUE = "60";
    private static final String RETRY_COUNT_VALUE = "3";
    private static final String MESSAGE = "rty01 failure";
    private static final String READ_TIMED_OUT_MESSAGE = "Read timed out after 30000 ms";
    private static final String NUMBER_OF_RECORDS_PREFIX = "Number of records: ";
    private static final String TRUE_VALUE = "true";
    private static final String FALSE_VALUE = "false";
    private static final String NON_RETRIABLE_CODE_VALUE = "60";
    private static final int RECORD_COUNT = 3;
    private static final int SERVER_HTTP_STATUS = 500;
    private static final int RETRIABLE_SERVER_CODE = 241;
    private static final int NON_RETRIABLE_SERVER_CODE = 60;
    private static final int V1_TIMEOUT_EXCEEDED_CODE = 159;
    private static final int UNCONVERTIBLE_VALUE = 5;
    // A retry budget below this count fails the redelivery rows.
    private static final int REDELIVERIES = 500;

    private static final String FIRST_PUT_NAME = "{displayName}[{index}] shape={0} errors.tolerance={1} expected={2}";
    private static final String REDELIVERY_NAME = "{displayName}[{index}] shape={0} path={1} errors.tolerance={2}";

    private static final String OUTCOME_MESSAGE = "outcome of ";
    private static final String WRITER_CALLS_MESSAGE = "writer calls for ";
    private static final String DLQ_SIZE_MESSAGE = "dead letter queue size for ";
    private static final String FAILURE_TEXT_MESSAGE = "failure text for ";
    private static final String REDELIVERY_MESSAGE_PREFIX = "redelivery ";

    enum Outcome {
        RETRIABLE,
        FAILS_TASK,
        DLQ,
        ESCAPES_RAW,
        DROPPED
    }

    enum PutPath {
        PROXY_PUT,
        CHUNK_FLUSHER_PUT_DIRECT
    }

    enum Shape {
        PLAIN_DATA_EXCEPTION(() -> new DataException(MESSAGE)),
        DATA_EXCEPTION_OVER_RETRIABLE_SERVER_CODE(() -> dataExceptionOverServerCode(RETRIABLE_SERVER_CODE)),
        DATA_EXCEPTION_OVER_SOCKET_TIMEOUT(() -> new DataException(MESSAGE, new SocketTimeoutException(MESSAGE))),
        DATA_EXCEPTION_OVER_UNKNOWN_HOST(() -> new DataException(MESSAGE, new UnknownHostException(MESSAGE))),
        DATA_EXCEPTION_OVER_RETRIABLE(() -> new DataException(MESSAGE, new RetriableException(MESSAGE))),
        DATA_EXCEPTION_OVER_NON_RETRIABLE_SERVER_CODE(() -> dataExceptionOverServerCode(NON_RETRIABLE_SERVER_CODE)),
        DATA_EXCEPTION_OVER_ADDITIONAL_RETRIABLE_CODE(
                () -> dataExceptionOverServerCode(NON_RETRIABLE_SERVER_CODE),
                Map.of(ClickHouseSinkConfig.ADDITIONAL_RETRIABLE_ERROR_CODES, NON_RETRIABLE_CODE_VALUE)),
        DATA_EXCEPTION_OVER_SOCKET_EXCEPTION_RETRY_ON(
                () -> new DataException(MESSAGE, new SocketException(MESSAGE)),
                Map.of(ClickHouseSinkConfig.RETRY_ON_SOCKET_EXCEPTION, TRUE_VALUE)),
        DATA_EXCEPTION_OVER_SOCKET_EXCEPTION_RETRY_OFF(
                () -> new DataException(MESSAGE, new SocketException(MESSAGE)),
                Map.of(ClickHouseSinkConfig.RETRY_ON_SOCKET_EXCEPTION, FALSE_VALUE)),
        DATA_EXCEPTION_OVER_READ_TIMED_OUT(
                () -> new DataException(MESSAGE, new IOException(READ_TIMED_OUT_MESSAGE))),
        DATA_EXCEPTION_OVER_PLAIN_IO(() -> new DataException(MESSAGE, new IOException(MESSAGE))),
        DATA_EXCEPTION_OVER_V1_TIMEOUT_EXCEEDED(() -> new DataException(
                MESSAGE, new ClickHouseException(V1_TIMEOUT_EXCEEDED_CODE, MESSAGE, (ClickHouseNode) null))),
        RETRIABLE_OVER_RUNTIME_EXCEPTION(() -> new RetriableException(MESSAGE, new RuntimeException(MESSAGE))),
        RETRIABLE_OVER_TIMEOUT_EXCEPTION(() -> new RetriableException(new TimeoutException(MESSAGE))),
        RUNTIME_EXCEPTION_OVER_SOCKET_TIMEOUT_CONTROL(
                () -> new RuntimeException(MESSAGE, new SocketTimeoutException(MESSAGE))),
        RUNTIME_EXCEPTION_OVER_ILLEGAL_STATE_CONTROL(
                () -> new RuntimeException(MESSAGE, new IllegalStateException(MESSAGE))),
        DATA_EXCEPTION_OVER_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE(() -> new DataException(
                MESSAGE,
                new RetriableException(
                        ServerExceptions.create(NON_RETRIABLE_SERVER_CODE, MESSAGE, SERVER_HTTP_STATUS)))),
        // The writer is never reached: Record.convert rejects the Integer value before Processing.
        CONVERSION_DATA_EXCEPTION_CONTROL(() -> new DataException(MESSAGE), Map.of(), true),
        BARE_RETRIABLE(() -> new RetriableException(MESSAGE)),
        RETRIABLE_OVER_SOCKET_TIMEOUT(() -> new RetriableException(new SocketTimeoutException(MESSAGE))),
        RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE(() -> new RetriableException(
                ServerExceptions.create(NON_RETRIABLE_SERVER_CODE, MESSAGE, SERVER_HTTP_STATUS))),
        BARE_RETRIABLE_SERVER_CODE(
                () -> ServerExceptions.create(RETRIABLE_SERVER_CODE, MESSAGE, SERVER_HTTP_STATUS));

        private final Supplier<RuntimeException> failure;
        private final Map<String, String> extraProps;
        private final boolean atConversion;

        Shape(Supplier<RuntimeException> failure) {
            this(failure, Map.of(), false);
        }

        Shape(Supplier<RuntimeException> failure, Map<String, String> extraProps) {
            this(failure, extraProps, false);
        }

        Shape(Supplier<RuntimeException> failure, Map<String, String> extraProps, boolean atConversion) {
            this.failure = failure;
            this.extraProps = extraProps;
            this.atConversion = atConversion;
        }
    }

    private static DataException dataExceptionOverServerCode(int code) {
        return new DataException(MESSAGE, ServerExceptions.create(code, MESSAGE, SERVER_HTTP_STATUS));
    }

    private static final class CountingFailingDBWriter extends InMemoryDBWriter {
        private final Supplier<RuntimeException> failure;
        private int calls;

        CountingFailingDBWriter(Supplier<RuntimeException> failure) {
            this.failure = failure;
        }

        @Override
        public void doInsert(List<Record> records, QueryIdentifier queryId, ErrorReporter errorReporter) {
            calls++;
            throw failure.get();
        }
    }

    private static ClickHouseSinkConfig configFor(Shape shape, String tolerance) {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConfig.HOSTNAME, HOSTNAME_VALUE);
        props.put(ClickHouseSinkConfig.ERRORS_TOLERANCE, tolerance);
        props.put(ClickHouseSinkConfig.CONNECTOR_RETRY_TIMEOUT, CONNECTOR_RETRY_TIMEOUT_VALUE);
        props.put(ClickHouseSinkConfig.RETRY_COUNT, RETRY_COUNT_VALUE);
        props.putAll(shape.extraProps);
        return new ClickHouseSinkConfig(props);
    }

    private static List<SinkRecord> batchFor(Shape shape) {
        return shape.atConversion
                ? List.of(new SinkRecord(TOPIC, 0, null, null, Schema.INT32_SCHEMA, UNCONVERTIBLE_VALUE, 0L))
                : SchemalessTestData.createPrimitiveTypes(TOPIC, 0, RECORD_COUNT);
    }

    static Stream<Arguments> firstPutMatrix() {
        String none = ClickHouseSinkConfig.ERROR_TOLERANCE_NONE;
        String all = ClickHouseSinkConfig.ERROR_TOLERANCE_ALL;
        return Stream.of(
                Arguments.of(Shape.PLAIN_DATA_EXCEPTION, none, Outcome.FAILS_TASK),
                Arguments.of(Shape.PLAIN_DATA_EXCEPTION, all, Outcome.DLQ),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_RETRIABLE_SERVER_CODE, none, Outcome.RETRIABLE),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_RETRIABLE_SERVER_CODE, all, Outcome.RETRIABLE),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_SOCKET_TIMEOUT, none, Outcome.RETRIABLE),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_SOCKET_TIMEOUT, all, Outcome.RETRIABLE),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_UNKNOWN_HOST, none, Outcome.RETRIABLE),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_UNKNOWN_HOST, all, Outcome.RETRIABLE),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_RETRIABLE, none, Outcome.RETRIABLE),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_RETRIABLE, all, Outcome.RETRIABLE),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_NON_RETRIABLE_SERVER_CODE, none, Outcome.FAILS_TASK),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_NON_RETRIABLE_SERVER_CODE, all, Outcome.DLQ),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_ADDITIONAL_RETRIABLE_CODE, none, Outcome.RETRIABLE),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_ADDITIONAL_RETRIABLE_CODE, all, Outcome.RETRIABLE),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_SOCKET_EXCEPTION_RETRY_ON, none, Outcome.RETRIABLE),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_SOCKET_EXCEPTION_RETRY_ON, all, Outcome.RETRIABLE),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_SOCKET_EXCEPTION_RETRY_OFF, none, Outcome.FAILS_TASK),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_SOCKET_EXCEPTION_RETRY_OFF, all, Outcome.DLQ),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_READ_TIMED_OUT, none, Outcome.RETRIABLE),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_READ_TIMED_OUT, all, Outcome.RETRIABLE),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_PLAIN_IO, none, Outcome.FAILS_TASK),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_PLAIN_IO, all, Outcome.DLQ),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_V1_TIMEOUT_EXCEEDED, none, Outcome.RETRIABLE),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_V1_TIMEOUT_EXCEEDED, all, Outcome.RETRIABLE),
                Arguments.of(Shape.RETRIABLE_OVER_RUNTIME_EXCEPTION, none, Outcome.RETRIABLE),
                Arguments.of(Shape.RETRIABLE_OVER_RUNTIME_EXCEPTION, all, Outcome.RETRIABLE),
                Arguments.of(Shape.RETRIABLE_OVER_TIMEOUT_EXCEPTION, none, Outcome.RETRIABLE),
                Arguments.of(Shape.RETRIABLE_OVER_TIMEOUT_EXCEPTION, all, Outcome.RETRIABLE),
                Arguments.of(Shape.RUNTIME_EXCEPTION_OVER_SOCKET_TIMEOUT_CONTROL, none, Outcome.RETRIABLE),
                Arguments.of(Shape.RUNTIME_EXCEPTION_OVER_SOCKET_TIMEOUT_CONTROL, all, Outcome.RETRIABLE),
                Arguments.of(Shape.RUNTIME_EXCEPTION_OVER_ILLEGAL_STATE_CONTROL, none, Outcome.FAILS_TASK),
                Arguments.of(Shape.RUNTIME_EXCEPTION_OVER_ILLEGAL_STATE_CONTROL, all, Outcome.DLQ),
                Arguments.of(
                        Shape.DATA_EXCEPTION_OVER_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE, none, Outcome.RETRIABLE),
                Arguments.of(
                        Shape.DATA_EXCEPTION_OVER_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE, all, Outcome.RETRIABLE),
                Arguments.of(Shape.CONVERSION_DATA_EXCEPTION_CONTROL, none, Outcome.ESCAPES_RAW),
                Arguments.of(Shape.CONVERSION_DATA_EXCEPTION_CONTROL, all, Outcome.ESCAPES_RAW),
                Arguments.of(Shape.BARE_RETRIABLE, none, Outcome.RETRIABLE),
                Arguments.of(Shape.BARE_RETRIABLE, all, Outcome.RETRIABLE),
                Arguments.of(Shape.RETRIABLE_OVER_SOCKET_TIMEOUT, none, Outcome.RETRIABLE),
                Arguments.of(Shape.RETRIABLE_OVER_SOCKET_TIMEOUT, all, Outcome.RETRIABLE),
                Arguments.of(Shape.RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE, none, Outcome.RETRIABLE),
                Arguments.of(Shape.RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE, all, Outcome.RETRIABLE),
                Arguments.of(Shape.BARE_RETRIABLE_SERVER_CODE, none, Outcome.RETRIABLE),
                Arguments.of(Shape.BARE_RETRIABLE_SERVER_CODE, all, Outcome.RETRIABLE));
    }

    @ParameterizedTest(name = FIRST_PUT_NAME)
    @MethodSource("firstPutMatrix")
    void firstPutOutcomeThroughProxySinkTaskPut(Shape shape, String tolerance, Outcome expected) throws Exception {
        InMemoryDLQ dlq = new InMemoryDLQ();
        CountingFailingDBWriter writer = new CountingFailingDBWriter(shape.failure);
        List<SinkRecord> records = batchFor(shape);
        ProxySinkTask task = new ProxySinkTask(configFor(shape, tolerance), dlq, writer);

        Outcome actual;
        Throwable caught = null;
        try {
            task.put(records);
            actual = dlq.size() == records.size() ? Outcome.DLQ : Outcome.DROPPED;
        } catch (RetriableException e) {
            caught = e;
            actual = Outcome.RETRIABLE;
        } catch (DataException e) {
            caught = e;
            actual = Outcome.ESCAPES_RAW;
        } catch (RuntimeException e) {
            caught = e;
            actual = Outcome.FAILS_TASK;
        } finally {
            task.stop();
        }

        String context = shape + " under errors.tolerance=" + tolerance;
        assertEquals(expected, actual, OUTCOME_MESSAGE + context);
        switch (expected) {
            case RETRIABLE -> {
                assertEquals(0, dlq.size(), DLQ_SIZE_MESSAGE + context);
                assertEquals(1, writer.calls, WRITER_CALLS_MESSAGE + context);
            }
            case FAILS_TASK -> {
                assertEquals(
                        NUMBER_OF_RECORDS_PREFIX + records.size(), caught.getMessage(), FAILURE_TEXT_MESSAGE + context);
                assertEquals(0, dlq.size(), DLQ_SIZE_MESSAGE + context);
                assertEquals(1, writer.calls, WRITER_CALLS_MESSAGE + context);
            }
            case DLQ -> {
                assertEquals(records.size(), dlq.size(), DLQ_SIZE_MESSAGE + context);
                assertEquals(1, writer.calls, WRITER_CALLS_MESSAGE + context);
            }
            case ESCAPES_RAW -> {
                assertEquals(0, dlq.size(), DLQ_SIZE_MESSAGE + context);
                assertEquals(0, writer.calls, WRITER_CALLS_MESSAGE + context);
            }
            default -> throw new AssertionError("unexpected expectation " + expected);
        }
    }

    static Stream<Arguments> redeliveryMatrix() {
        return Stream.of(
                        Shape.DATA_EXCEPTION_OVER_RETRIABLE_SERVER_CODE,
                        Shape.DATA_EXCEPTION_OVER_SOCKET_TIMEOUT,
                        Shape.DATA_EXCEPTION_OVER_UNKNOWN_HOST,
                        Shape.DATA_EXCEPTION_OVER_RETRIABLE,
                        Shape.DATA_EXCEPTION_OVER_ADDITIONAL_RETRIABLE_CODE,
                        Shape.DATA_EXCEPTION_OVER_SOCKET_EXCEPTION_RETRY_ON,
                        Shape.DATA_EXCEPTION_OVER_READ_TIMED_OUT,
                        Shape.DATA_EXCEPTION_OVER_V1_TIMEOUT_EXCEEDED,
                        Shape.RETRIABLE_OVER_RUNTIME_EXCEPTION,
                        Shape.RETRIABLE_OVER_TIMEOUT_EXCEPTION,
                        Shape.RUNTIME_EXCEPTION_OVER_SOCKET_TIMEOUT_CONTROL,
                        Shape.DATA_EXCEPTION_OVER_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE,
                        Shape.BARE_RETRIABLE,
                        Shape.RETRIABLE_OVER_SOCKET_TIMEOUT,
                        Shape.RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE,
                        Shape.BARE_RETRIABLE_SERVER_CODE)
                .flatMap(shape -> Stream.of(PutPath.values()).flatMap(path -> Stream.of(
                                ClickHouseSinkConfig.ERROR_TOLERANCE_NONE, ClickHouseSinkConfig.ERROR_TOLERANCE_ALL)
                        .map(tolerance -> Arguments.of(shape, path, tolerance))));
    }

    @ParameterizedTest(name = REDELIVERY_NAME)
    @MethodSource("redeliveryMatrix")
    void retriableShapeIsRedeliveredWithoutBound(Shape shape, PutPath path, String tolerance) throws Exception {
        ClickHouseSinkConfig config = configFor(shape, tolerance);
        InMemoryDLQ dlq = new InMemoryDLQ();
        CountingFailingDBWriter writer = new CountingFailingDBWriter(shape.failure);
        List<SinkRecord> records = batchFor(shape);
        ProxySinkTask task = new ProxySinkTask(config, dlq, writer);
        ChunkFlusher flusher = new ChunkFlusher(task, config, dlq);

        try {
            for (int i = 1; i <= REDELIVERIES; i++) {
                assertThrows(
                        RetriableException.class,
                        () -> {
                            if (path == PutPath.PROXY_PUT) {
                                task.put(records);
                            } else {
                                flusher.putDirect(records);
                            }
                        },
                        REDELIVERY_MESSAGE_PREFIX + i);
            }
        } finally {
            task.stop();
        }

        String context = shape + " through " + path + " under errors.tolerance=" + tolerance;
        assertEquals(REDELIVERIES, writer.calls, WRITER_CALLS_MESSAGE + context);
        assertEquals(0, dlq.size(), DLQ_SIZE_MESSAGE + context);
    }
}
