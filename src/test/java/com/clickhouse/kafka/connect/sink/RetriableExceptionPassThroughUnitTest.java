package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.db.InMemoryDBWriter;
import com.clickhouse.kafka.connect.sink.dlq.ErrorReporter;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import com.clickhouse.kafka.connect.sink.helper.SchemalessTestData;
import com.clickhouse.kafka.connect.sink.util.ServerExceptions;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import com.clickhouse.kafka.connect.util.Utils;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free pins that a {@code RetriableException}, as the exception itself or anywhere in its cause chain,
 * comes out of {@code Utils.handleException} retriable under both {@code errors.tolerance} values, directly and
 * through {@code ProxySinkTask.put} and {@code ChunkFlusher.putDirect}. The {@code _CONTROL} shapes are
 * unchanged by the pass-through.
 *
 * <p>The expectations are literal. A changed row is a decision, not an update.
 *
 * <p>Extends nothing and carries no tag so it stays in {@code testUnit}.
 */
class RetriableExceptionPassThroughUnitTest {

    private static final String TOPIC = "retriable_pass_through";
    private static final String HOSTNAME_VALUE = "localhost";
    private static final String QUERY_TEXT = "pass-through-query";
    private static final String FAILURE_MESSAGE = "pass-through failure";
    private static final String NUMBER_OF_RECORDS_PREFIX = "Number of records: ";
    private static final int RECORD_COUNT = 3;
    private static final int SERVER_HTTP_STATUS = 500;
    private static final int NON_RETRIABLE_SERVER_CODE = 60;
    private static final int RETRIABLE_SERVER_CODE = 241;

    private static final String DIRECT_NAME = "{displayName}[{index}] shape={0} errors.tolerance={1} expected={2}";
    private static final String PUT_NAME = "{displayName}[{index}] shape={0} path={1} errors.tolerance={2}";

    private static final String OUTCOME_MESSAGE = "outcome of ";
    private static final String SAME_INSTANCE_MESSAGE = "the input instance is rethrown for ";
    private static final String WRAPPER_CLASS_MESSAGE = "wrapper class for ";
    private static final String WRAPPER_CAUSE_MESSAGE = "wrapper cause is the input for ";
    private static final String WRAPPER_TEXT_MESSAGE = "wrapper text for ";
    private static final String NOTHING_THROWN_MESSAGE = "nothing is thrown for ";
    private static final String DLQ_SIZE_MESSAGE = "dead letter queue size for ";

    enum Outcome {
        RETRIABLE_SAME,
        RETRIABLE_WRAPPED,
        RUNTIME_WRAP,
        DATA_EXCEPTION,
        RETURNS
    }

    enum Shape {
        BARE_RETRIABLE(() -> new RetriableException(FAILURE_MESSAGE)),
        BARE_RETRIABLE_OVER_TIMEOUT(() -> new RetriableException(new TimeoutException(FAILURE_MESSAGE))),
        WRAPPED_INSERT_TIMEOUT(
                () -> new RuntimeException(QUERY_TEXT, new RetriableException(new TimeoutException(FAILURE_MESSAGE)))),
        WRAPPED_DDL_PROPAGATION_TIMEOUT(
                () -> new RuntimeException(QUERY_TEXT, new RetriableException(FAILURE_MESSAGE))),
        DATA_EXCEPTION_OVER_RETRIABLE(() -> new DataException(FAILURE_MESSAGE, new RetriableException(FAILURE_MESSAGE))),
        BARE_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE(() -> new RetriableException(
                ServerExceptions.create(NON_RETRIABLE_SERVER_CODE, FAILURE_MESSAGE, SERVER_HTTP_STATUS))),
        WRAPPED_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE(() -> new RuntimeException(
                QUERY_TEXT,
                new RetriableException(
                        ServerExceptions.create(NON_RETRIABLE_SERVER_CODE, FAILURE_MESSAGE, SERVER_HTTP_STATUS)))),
        RETRIABLE_FROM_FIRST_HANDLING(() -> new RetriableException(
                new RuntimeException(QUERY_TEXT, new SocketTimeoutException(FAILURE_MESSAGE)))),
        RETRIABLE_OVER_NUMBER_OF_RECORDS_WRAP(() -> new RetriableException(new RuntimeException(
                NUMBER_OF_RECORDS_PREFIX + RECORD_COUNT,
                new RuntimeException(QUERY_TEXT, new IllegalStateException(FAILURE_MESSAGE))))),
        WRAPPED_ILLEGAL_STATE_CONTROL(
                () -> new RuntimeException(QUERY_TEXT, new IllegalStateException(FAILURE_MESSAGE))),
        WRAPPED_RETRIABLE_SERVER_CODE_CONTROL(() -> new RuntimeException(
                QUERY_TEXT, ServerExceptions.create(RETRIABLE_SERVER_CODE, FAILURE_MESSAGE, SERVER_HTTP_STATUS)));

        private final Supplier<Exception> failure;

        Shape(Supplier<Exception> failure) {
            this.failure = failure;
        }
    }

    static Stream<Arguments> directMatrix() {
        String none = ClickHouseSinkConfig.ERROR_TOLERANCE_NONE;
        String all = ClickHouseSinkConfig.ERROR_TOLERANCE_ALL;
        return Stream.of(
                Arguments.of(Shape.BARE_RETRIABLE, none, Outcome.RETRIABLE_SAME),
                Arguments.of(Shape.BARE_RETRIABLE, all, Outcome.RETRIABLE_SAME),
                Arguments.of(Shape.BARE_RETRIABLE_OVER_TIMEOUT, none, Outcome.RETRIABLE_SAME),
                Arguments.of(Shape.BARE_RETRIABLE_OVER_TIMEOUT, all, Outcome.RETRIABLE_SAME),
                Arguments.of(Shape.WRAPPED_INSERT_TIMEOUT, none, Outcome.RETRIABLE_WRAPPED),
                Arguments.of(Shape.WRAPPED_INSERT_TIMEOUT, all, Outcome.RETRIABLE_WRAPPED),
                Arguments.of(Shape.WRAPPED_DDL_PROPAGATION_TIMEOUT, none, Outcome.RETRIABLE_WRAPPED),
                Arguments.of(Shape.WRAPPED_DDL_PROPAGATION_TIMEOUT, all, Outcome.RETRIABLE_WRAPPED),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_RETRIABLE, none, Outcome.RETRIABLE_WRAPPED),
                Arguments.of(Shape.DATA_EXCEPTION_OVER_RETRIABLE, all, Outcome.RETRIABLE_WRAPPED),
                Arguments.of(Shape.BARE_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE, none, Outcome.RETRIABLE_SAME),
                Arguments.of(Shape.BARE_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE, all, Outcome.RETRIABLE_SAME),
                Arguments.of(Shape.WRAPPED_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE, none, Outcome.RETRIABLE_WRAPPED),
                Arguments.of(Shape.WRAPPED_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE, all, Outcome.RETRIABLE_WRAPPED),
                Arguments.of(Shape.RETRIABLE_FROM_FIRST_HANDLING, none, Outcome.RETRIABLE_SAME),
                Arguments.of(Shape.RETRIABLE_FROM_FIRST_HANDLING, all, Outcome.RETRIABLE_SAME),
                Arguments.of(Shape.RETRIABLE_OVER_NUMBER_OF_RECORDS_WRAP, none, Outcome.RETRIABLE_SAME),
                Arguments.of(Shape.RETRIABLE_OVER_NUMBER_OF_RECORDS_WRAP, all, Outcome.RETRIABLE_SAME),
                Arguments.of(Shape.WRAPPED_ILLEGAL_STATE_CONTROL, none, Outcome.RUNTIME_WRAP),
                Arguments.of(Shape.WRAPPED_ILLEGAL_STATE_CONTROL, all, Outcome.RETURNS),
                Arguments.of(Shape.WRAPPED_RETRIABLE_SERVER_CODE_CONTROL, none, Outcome.RETRIABLE_WRAPPED),
                Arguments.of(Shape.WRAPPED_RETRIABLE_SERVER_CODE_CONTROL, all, Outcome.RETRIABLE_WRAPPED));
    }

    private static Outcome classify(RuntimeException thrown, Exception input) {
        if (thrown instanceof RetriableException) {
            return thrown == input ? Outcome.RETRIABLE_SAME : Outcome.RETRIABLE_WRAPPED;
        }
        if (thrown instanceof DataException) {
            return Outcome.DATA_EXCEPTION;
        }
        return Outcome.RUNTIME_WRAP;
    }

    @ParameterizedTest(name = DIRECT_NAME)
    @MethodSource("directMatrix")
    void handleExceptionOutcome(Shape shape, String tolerance, Outcome expected) {
        List<SinkRecord> records = SchemalessTestData.createPrimitiveTypes(TOPIC, 0, RECORD_COUNT);
        Exception input = shape.failure.get();
        String context = shape + " under errors.tolerance=" + tolerance;

        Outcome actual;
        RuntimeException thrown = null;
        try {
            Utils.handleException(
                    input, ClickHouseSinkConfig.ERROR_TOLERANCE_ALL.equals(tolerance), false, Set.of(), records);
            actual = Outcome.RETURNS;
        } catch (RuntimeException e) {
            thrown = e;
            actual = classify(e, input);
        }

        assertEquals(expected, actual, OUTCOME_MESSAGE + context);
        switch (expected) {
            case RETRIABLE_SAME, DATA_EXCEPTION -> assertSame(input, thrown, SAME_INSTANCE_MESSAGE + context);
            case RETRIABLE_WRAPPED -> {
                assertEquals(RetriableException.class, thrown.getClass(), WRAPPER_CLASS_MESSAGE + context);
                assertSame(input, thrown.getCause(), WRAPPER_CAUSE_MESSAGE + context);
            }
            case RUNTIME_WRAP -> {
                assertEquals(RuntimeException.class, thrown.getClass(), WRAPPER_CLASS_MESSAGE + context);
                assertEquals(
                        NUMBER_OF_RECORDS_PREFIX + RECORD_COUNT, thrown.getMessage(), WRAPPER_TEXT_MESSAGE + context);
                assertSame(input, thrown.getCause(), WRAPPER_CAUSE_MESSAGE + context);
            }
            case RETURNS -> assertNull(thrown, NOTHING_THROWN_MESSAGE + context);
        }
    }

    enum WriterShape {
        ASYNC_INSERT_TIMEOUT(() -> new RetriableException(new TimeoutException(FAILURE_MESSAGE))),
        DDL_PROPAGATION_TIMEOUT(() -> new RetriableException(FAILURE_MESSAGE)),
        SOCKET_TIMEOUT_CONTROL(() -> new RetriableException(new SocketTimeoutException(FAILURE_MESSAGE))),
        DATA_EXCEPTION_OVER_RETRIABLE(() -> new DataException(FAILURE_MESSAGE, new RetriableException(FAILURE_MESSAGE)));

        private final Supplier<RuntimeException> failure;

        WriterShape(Supplier<RuntimeException> failure) {
            this.failure = failure;
        }
    }

    enum PutPath {
        PROXY_PUT,
        CHUNK_FLUSHER_PUT_DIRECT
    }

    enum PutOutcome {
        RETRIABLE,
        FAILS_TASK,
        DLQ,
        DROPPED
    }

    private static final class FailingDBWriter extends InMemoryDBWriter {
        private final Supplier<RuntimeException> failure;

        FailingDBWriter(Supplier<RuntimeException> failure) {
            this.failure = failure;
        }

        @Override
        public void doInsert(List<Record> records, QueryIdentifier queryId, ErrorReporter errorReporter) {
            throw failure.get();
        }
    }

    static Stream<Arguments> putMatrix() {
        return Stream.of(WriterShape.values())
                .flatMap(shape -> Stream.of(PutPath.values()).flatMap(path -> Stream.of(
                                ClickHouseSinkConfig.ERROR_TOLERANCE_NONE, ClickHouseSinkConfig.ERROR_TOLERANCE_ALL)
                        .map(tolerance -> Arguments.of(shape, path, tolerance))));
    }

    @ParameterizedTest(name = PUT_NAME)
    @MethodSource("putMatrix")
    void writerFailureOutcomeThroughPut(WriterShape shape, PutPath path, String tolerance) throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConfig.HOSTNAME, HOSTNAME_VALUE);
        props.put(ClickHouseSinkConfig.ERRORS_TOLERANCE, tolerance);
        ClickHouseSinkConfig config = new ClickHouseSinkConfig(props);
        InMemoryDLQ dlq = new InMemoryDLQ();
        List<SinkRecord> records = SchemalessTestData.createPrimitiveTypes(TOPIC, 0, RECORD_COUNT);
        ProxySinkTask task = new ProxySinkTask(config, dlq, new FailingDBWriter(shape.failure));

        PutOutcome actual;
        try {
            if (path == PutPath.PROXY_PUT) {
                task.put(records);
            } else {
                new ChunkFlusher(task, config, dlq).putDirect(records);
            }
            actual = dlq.size() == records.size() ? PutOutcome.DLQ : PutOutcome.DROPPED;
        } catch (RetriableException e) {
            actual = PutOutcome.RETRIABLE;
        } catch (RuntimeException e) {
            actual = PutOutcome.FAILS_TASK;
        } finally {
            task.stop();
        }

        String context = shape + " through " + path + " under errors.tolerance=" + tolerance;
        assertEquals(PutOutcome.RETRIABLE, actual, OUTCOME_MESSAGE + context);
        assertEquals(0, dlq.size(), DLQ_SIZE_MESSAGE + context);
    }
}
