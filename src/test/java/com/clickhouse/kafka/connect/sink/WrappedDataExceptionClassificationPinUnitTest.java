package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.db.InMemoryDBWriter;
import com.clickhouse.kafka.connect.sink.dlq.ErrorReporter;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import com.clickhouse.kafka.connect.sink.helper.SchemalessTestData;
import com.clickhouse.kafka.connect.sink.util.ServerExceptions;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free pin for PIN-02: today's outcome of a writer failure at the {@link ProxySinkTask#put} layer,
 * under both {@code errors.tolerance} values. {@code Processing.doInsert} wraps every writer failure in
 * {@code RuntimeException(queryId, e)}, so {@code Utils.handleException} never sees a bare writer
 * {@code DataException}; that is why a socket-timeout-rooted {@code DataException} is retriable here under
 * both tolerances, while {@code AdditionalRetriableErrorCodesPinUnitTest} pins the direct
 * {@code Utils.handleException} layer.
 *
 * <p>The expectations are literal. A changed row is a behaviour change to decide on, not an expectation
 * to update.
 *
 * <p>Extends nothing and carries no tag so it stays in {@code testUnit}.
 */
class WrappedDataExceptionClassificationPinUnitTest {

    private static final String TOPIC = "pin02_wrapped_data_exception";
    private static final String HOSTNAME_VALUE = "localhost";
    private static final String DATA_ERROR_MESSAGE = "pin02 data error";
    private static final String SERVER_ERROR_MESSAGE = "pin02 server error";
    private static final String SOCKET_TIMEOUT_MESSAGE = "pin02 socket timeout";
    private static final String UNKNOWN_HOST_MESSAGE = "pin02 unknown host";
    private static final String NUMBER_OF_RECORDS_PREFIX = "Number of records: ";
    private static final int RECORD_COUNT = 3;
    private static final int SERVER_HTTP_STATUS = 500;
    private static final int RETRIABLE_SERVER_CODE = 241;
    private static final int NON_RETRIABLE_SERVER_CODE = 60;
    private static final int UNCONVERTIBLE_VALUE = 5;

    enum Outcome {
        RETRIABLE,
        FAILS_TASK,
        DLQ,
        ESCAPES_RAW,
        DROPPED
    }

    enum Shape {
        WRAPPED_DATA_EXCEPTION(() -> new DataException(DATA_ERROR_MESSAGE), false),
        RETRIABLE_SERVER_CODE_IN_CHAIN(() -> dataExceptionWithServerCode(RETRIABLE_SERVER_CODE), false),
        SOCKET_TIMEOUT_ROOT(
                () -> new DataException(DATA_ERROR_MESSAGE, new SocketTimeoutException(SOCKET_TIMEOUT_MESSAGE)),
                false),
        NON_RETRIABLE_SERVER_CODE_CONTROL(() -> dataExceptionWithServerCode(NON_RETRIABLE_SERVER_CODE), false),
        UNKNOWN_HOST_ROOT_CONTROL(
                () -> new DataException(DATA_ERROR_MESSAGE, new UnknownHostException(UNKNOWN_HOST_MESSAGE)), false),
        RUNTIME_EXCEPTION_SOCKET_TIMEOUT_CONTROL(
                () -> new RuntimeException(DATA_ERROR_MESSAGE, new SocketTimeoutException(SOCKET_TIMEOUT_MESSAGE)),
                false),
        // The writer is never reached: Record.convert rejects the Integer value before Processing.
        CONVERSION_DATA_EXCEPTION_CONTROL(() -> new DataException(DATA_ERROR_MESSAGE), true);

        private final Supplier<RuntimeException> failure;
        private final boolean atConversion;

        Shape(Supplier<RuntimeException> failure, boolean atConversion) {
            this.failure = failure;
            this.atConversion = atConversion;
        }
    }

    private static DataException dataExceptionWithServerCode(int code) {
        return new DataException(
                DATA_ERROR_MESSAGE, ServerExceptions.create(code, SERVER_ERROR_MESSAGE, SERVER_HTTP_STATUS));
    }

    private static final class FailingDBWriter extends InMemoryDBWriter {
        private final Supplier<RuntimeException> failure;
        private int calls;
        private RuntimeException lastThrown;

        FailingDBWriter(Supplier<RuntimeException> failure) {
            this.failure = failure;
        }

        @Override
        public void doInsert(List<Record> records, QueryIdentifier queryId, ErrorReporter errorReporter) {
            calls++;
            lastThrown = failure.get();
            throw lastThrown;
        }
    }

    static Stream<Arguments> classificationMatrix() {
        String none = ClickHouseSinkConfig.ERROR_TOLERANCE_NONE;
        String all = ClickHouseSinkConfig.ERROR_TOLERANCE_ALL;
        return Stream.of(
                Arguments.of(Shape.WRAPPED_DATA_EXCEPTION, none, Outcome.FAILS_TASK),
                Arguments.of(Shape.WRAPPED_DATA_EXCEPTION, all, Outcome.DLQ),
                Arguments.of(Shape.RETRIABLE_SERVER_CODE_IN_CHAIN, none, Outcome.RETRIABLE),
                Arguments.of(Shape.RETRIABLE_SERVER_CODE_IN_CHAIN, all, Outcome.RETRIABLE),
                Arguments.of(Shape.SOCKET_TIMEOUT_ROOT, none, Outcome.RETRIABLE),
                Arguments.of(Shape.SOCKET_TIMEOUT_ROOT, all, Outcome.RETRIABLE),
                Arguments.of(Shape.NON_RETRIABLE_SERVER_CODE_CONTROL, none, Outcome.FAILS_TASK),
                Arguments.of(Shape.NON_RETRIABLE_SERVER_CODE_CONTROL, all, Outcome.DLQ),
                Arguments.of(Shape.UNKNOWN_HOST_ROOT_CONTROL, none, Outcome.RETRIABLE),
                Arguments.of(Shape.UNKNOWN_HOST_ROOT_CONTROL, all, Outcome.RETRIABLE),
                Arguments.of(Shape.RUNTIME_EXCEPTION_SOCKET_TIMEOUT_CONTROL, none, Outcome.RETRIABLE),
                Arguments.of(Shape.RUNTIME_EXCEPTION_SOCKET_TIMEOUT_CONTROL, all, Outcome.RETRIABLE),
                Arguments.of(Shape.CONVERSION_DATA_EXCEPTION_CONTROL, none, Outcome.ESCAPES_RAW),
                Arguments.of(Shape.CONVERSION_DATA_EXCEPTION_CONTROL, all, Outcome.ESCAPES_RAW));
    }

    private static Throwable parentOf(Throwable top, Throwable target) {
        for (Throwable current = top; current != null; current = current.getCause()) {
            if (current.getCause() == target) {
                return current;
            }
        }
        return null;
    }

    private static boolean chainContains(Throwable top, Throwable target) {
        for (Throwable current = top; current != null; current = current.getCause()) {
            if (current == target) {
                return true;
            }
        }
        return false;
    }

    @ParameterizedTest(name = "[{index}] {0} under errors.tolerance={1} -> {2}")
    @MethodSource("classificationMatrix")
    @DisplayName("PIN-02: a writer DataException shape and errors.tolerance yield today's outcome through"
            + " ProxySinkTask.put (put layer, not Utils.handleException directly)")
    void wrappedDataExceptionOutcomeThroughProxySinkTaskPut(Shape shape, String tolerance, Outcome expected)
            throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConfig.HOSTNAME, HOSTNAME_VALUE);
        props.put(ClickHouseSinkConfig.ERRORS_TOLERANCE, tolerance);
        InMemoryDLQ dlq = new InMemoryDLQ();
        FailingDBWriter writer = new FailingDBWriter(shape.failure);
        List<SinkRecord> records = shape.atConversion
                ? List.of(new SinkRecord(TOPIC, 0, null, null, Schema.INT32_SCHEMA, UNCONVERTIBLE_VALUE, 0L))
                : SchemalessTestData.createPrimitiveTypes(TOPIC, 0, RECORD_COUNT);

        ProxySinkTask task = new ProxySinkTask(new ClickHouseSinkConfig(props), dlq, writer);
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

        String context = shape + " under errors.tolerance=" + tolerance + ", caught "
                + (caught == null ? "nothing" : caught.getClass().getName() + ": " + caught.getMessage());
        assertEquals(expected, actual, context);

        switch (expected) {
            case RETRIABLE -> {
                assertEquals(0, dlq.size(), context);
                assertTrue(chainContains(caught, writer.lastThrown), context);
                Throwable wrapper = parentOf(caught, writer.lastThrown);
                assertNotNull(wrapper, context);
                assertSame(writer.lastThrown, wrapper.getCause(), context);
                assertEquals(RuntimeException.class, wrapper.getClass(), context);
            }
            case FAILS_TASK -> {
                assertEquals(NUMBER_OF_RECORDS_PREFIX + records.size(), caught.getMessage(), context);
                assertFalse(caught instanceof RetriableException, context);
                assertEquals(0, dlq.size(), context);
                assertTrue(chainContains(caught, writer.lastThrown), context);
                Throwable wrapper = parentOf(caught, writer.lastThrown);
                assertNotNull(wrapper, context);
                assertEquals(RuntimeException.class, wrapper.getClass(), context);
            }
            case DLQ -> {
                assertTrue(writer.calls >= 1, context);
                for (Throwable reported : dlq.exceptions()) {
                    assertTrue(chainContains(reported, writer.lastThrown), context);
                }
            }
            case ESCAPES_RAW -> {
                assertEquals(DataException.class, caught.getClass(), context);
                assertEquals(0, dlq.size(), context);
                assertEquals(0, writer.calls, context);
            }
            default -> throw new AssertionError("unexpected expectation " + expected);
        }
    }
}
