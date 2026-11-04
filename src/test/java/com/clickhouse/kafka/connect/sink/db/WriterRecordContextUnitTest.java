package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.data.SchemaType;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.clickhouse.kafka.connect.sink.util.ThrowableTexts;
import com.clickhouse.kafka.connect.util.Utils;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Pins what {@code writeRecordsRowBinary} does when one column of a record cannot be serialized:
 * it rethrows exactly the exception {@code doWriteCol} produced, so retry classification sees the
 * same chain with or without the record loop, and it logs one ERROR line that names the record's
 * topic, partition and offset, the column and the exception class, never a value, an exception
 * message or a stack trace.
 *
 * <p>Each of the 15 failure shapes is measured twice: a direct {@code doWriteCol} call (BEFORE) and
 * the record loop (AFTER). The class extends nothing, carries no tag and uses no mocking library,
 * so it runs without Docker.
 */
public class WriterRecordContextUnitTest {

    private static final String SENTINEL = "RECORD-VALUE-SENTINEL-45";
    private static final String TOPIC = "probe-topic";
    private static final int PARTITION = 3;
    private static final long OFFSET = 42L;
    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String JACKSON_TEXT = "reference chain: Map[\"" + SENTINEL + "\"]";
    private static final String QUERY_ID_TEXT = "record-context-query";
    private static final String COORDINATE_LINE_PREFIX = "Record serialization failed: ";
    private static final String COORDINATE_LINE_FORMAT =
            COORDINATE_LINE_PREFIX + "topic [%s] partition [%d] offset [%d] column `%s`: exception %s";
    private static final String ERROR_LEVEL = "ERROR";
    private static final String UUID_COLUMN = "c_uuid";

    enum RetryClass {
        NON_RETRIABLE,
        RETRIABLE,
        SOCKET
    }

    enum Outcome {
        RETRIABLE,
        DATA_EXCEPTION,
        RUNTIME_WRAP,
        RETURNS
    }

    /** One scripted failure: the column and value, and where the failure is injected. */
    private record Scenario(
            Column column,
            Data value,
            boolean supportDefaults,
            IOException streamFailure,
            RuntimeException primitiveFailure,
            RetriableException columnFailure) {

        Object injected() {
            if (streamFailure != null) {
                return streamFailure;
            }
            return columnFailure;
        }
    }

    private static Column column(String name, Type type) {
        return Column.builder().name(name).type(type).build();
    }

    private static Data string(String text) {
        return new Data(Schema.STRING_SCHEMA, text);
    }

    private static Scenario valueFailure(Column column, Data value) {
        return new Scenario(column, value, false, null, null, null);
    }

    private static Scenario streamFailure(IOException failure) {
        return new Scenario(column(UUID_COLUMN, Type.UUID), string(VALID_UUID), false, failure, null, null);
    }

    private static Column tupleColumn() {
        return Column.builder()
                .name("t")
                .type(Type.TUPLE)
                .tupleFields(List.of(column("t.a", Type.INT32)))
                .build();
    }

    private static Data tupleValueWithNullField() {
        Schema schema = SchemaBuilder.struct().field("a", Schema.OPTIONAL_INT32_SCHEMA).build();
        return new Data(schema, new Struct(schema).put("a", null));
    }

    /** The 15 shapes of the #857 measurement. */
    enum Shape {
        INVALID_UUID(RetryClass.NON_RETRIABLE, () -> valueFailure(column(UUID_COLUMN, Type.UUID), string(SENTINEL))),
        UNPARSEABLE_INT128(
                RetryClass.NON_RETRIABLE, () -> valueFailure(column("c_int128", Type.INT128), string(SENTINEL))),
        TYPE_MISMATCH(RetryClass.NON_RETRIABLE, () -> valueFailure(column("c_u64", Type.UINT64), string(SENTINEL))),
        UNKNOWN_ENUM_LABEL(
                RetryClass.NON_RETRIABLE,
                () -> valueFailure(
                        Column.builder().name("c_enum").type(Type.Enum8).enumValues(Map.of("a", 1)).build(),
                        string(SENTINEL))),
        NULL_IN_NON_NULLABLE(
                RetryClass.NON_RETRIABLE,
                () -> new Scenario(
                        column("c_i32", Type.INT32),
                        new Data(Schema.OPTIONAL_INT32_SCHEMA, null),
                        true,
                        null,
                        null,
                        null)),
        NESTED_NULL_IN_TUPLE(
                RetryClass.NON_RETRIABLE, () -> valueFailure(tupleColumn(), tupleValueWithNullField())),
        SCRIPTED_ARITHMETIC(
                RetryClass.NON_RETRIABLE,
                () -> new Scenario(
                        column("c_int", Type.INT32),
                        new Data(Schema.INT32_SCHEMA, 1),
                        false,
                        null,
                        new ArithmeticException(SENTINEL),
                        null)),
        IO_PLAIN(RetryClass.NON_RETRIABLE, () -> streamFailure(new IOException("stream closed by test"))),
        IO_READ_TIMEOUT_PREFIX(
                RetryClass.RETRIABLE, () -> streamFailure(new IOException("Read timed out after 30000 ms"))),
        SOCKET_TIMEOUT(RetryClass.RETRIABLE, () -> streamFailure(new SocketTimeoutException("timed out"))),
        SOCKET_EXCEPTION(RetryClass.SOCKET, () -> streamFailure(new SocketException("Connection reset"))),
        UNKNOWN_HOST(RetryClass.RETRIABLE, () -> streamFailure(new UnknownHostException("clickhouse.invalid"))),
        JACKSON_ALONE(RetryClass.NON_RETRIABLE, () -> streamFailure(new JsonMappingException(JACKSON_TEXT))),
        JACKSON_OVER_SOCKET_TIMEOUT(
                RetryClass.RETRIABLE,
                () -> streamFailure(new JsonMappingException(JACKSON_TEXT, new SocketTimeoutException("timed out")))),
        RETRIABLE_FROM_COLUMN_WRITER(
                RetryClass.NON_RETRIABLE,
                () -> new Scenario(
                        column("c_i64", Type.INT64),
                        new Data(Schema.INT64_SCHEMA, 1L),
                        false,
                        null,
                        null,
                        new RetriableException("server is restarting")));

        private final RetryClass retryClass;
        private final Supplier<Scenario> scenario;

        Shape(RetryClass retryClass, Supplier<Scenario> scenario) {
            this.retryClass = retryClass;
            this.scenario = scenario;
        }

        Scenario scenario() {
            return scenario.get();
        }
    }

    /** A real writer with the scripted failure injected at the primitive write or the column write. */
    private static final class ScriptedWriter extends ClickHouseWriter {
        private final Scenario scenario;

        ScriptedWriter(Scenario scenario) {
            super(new SinkTaskStatistics(null, 0));
            this.scenario = scenario;
            try {
                Field field = ClickHouseWriter.class.getDeclaredField("csc");
                field.setAccessible(true);
                field.set(this, new ClickHouseSinkConfig(minimalProps()));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("ClickHouseWriter field 'csc' changed; update this test seam", e);
            }
        }

        @Override
        protected void doWritePrimitive(
                Type columnType, Schema.Type dataType, OutputStream out, Object primitive, Column col)
                throws IOException {
            if (scenario.primitiveFailure() != null) {
                throw scenario.primitiveFailure();
            }
            super.doWritePrimitive(columnType, dataType, out, primitive, col);
        }

        @Override
        protected void doWriteCol(
                Data fieldValue, boolean fieldExists, Column col, OutputStream out, boolean defaultsSupport)
                throws IOException {
            if (scenario.columnFailure() != null) {
                throw scenario.columnFailure();
            }
            super.doWriteCol(fieldValue, fieldExists, col, out, defaultsSupport);
        }
    }

    private static Map<String, String> minimalProps() {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConfig.HOSTNAME, "localhost");
        props.put(ClickHouseSinkConfig.PORT, "8123");
        props.put(ClickHouseSinkConfig.DATABASE, "default");
        props.put(ClickHouseSinkConfig.USERNAME, "default");
        props.put(ClickHouseSinkConfig.PASSWORD, "");
        return props;
    }

    private static OutputStream stream(Scenario scenario) {
        return scenario.streamFailure() == null
                ? new ByteArrayOutputStream()
                : new ScriptedWriteFailureWriter.FailingOutputStream(scenario.streamFailure(), 0);
    }

    private static Record record(Scenario scenario) {
        Map<String, Data> jsonMap = new HashMap<>();
        jsonMap.put(scenario.column().getName(), scenario.value());
        SinkRecord sinkRecord = new SinkRecord(TOPIC, PARTITION, null, null, Schema.STRING_SCHEMA, "value", OFFSET);
        return Record.newRecord(
                SchemaType.SCHEMA, TOPIC, PARTITION, OFFSET, Collections.emptyList(), jsonMap, "default", sinkRecord);
    }

    /** What the failure looked like without the record loop. */
    private static Throwable before(Shape shape) {
        Scenario scenario = shape.scenario();
        try {
            new ScriptedWriter(scenario)
                    .doWriteCol(scenario.value(), true, scenario.column(), stream(scenario), scenario.supportDefaults());
        } catch (Throwable t) {
            return t;
        }
        throw new AssertionError(shape + " must fail");
    }

    /** What the record loop threw and logged, and the scenario that was injected. */
    private record After(Throwable thrown, List<LogCapture.CapturedEvent> events, List<String> logTexts, Scenario scenario) {}

    private static After after(Shape shape) {
        Scenario scenario = shape.scenario();
        ScriptedWriter writer = new ScriptedWriter(scenario);
        Table table = new Table("default", "probe_table", scenario.supportDefaults(), List.of(scenario.column()), 1);
        try (LogCapture log = LogCapture.attach(ClickHouseWriter.class)) {
            Throwable thrown = null;
            try {
                writer.writeRecordsRowBinary(List.of(record(scenario)), table, stream(scenario), scenario.supportDefaults());
            } catch (Throwable t) {
                thrown = t;
            }
            assertNotNull(thrown, shape + " must fail in the record loop");
            return new After(thrown, log.events(), log.allTexts(), scenario);
        }
    }

    private static List<String> causeClassNames(Throwable top) {
        List<String> names = new ArrayList<>();
        for (Throwable link : ThrowableTexts.causeChain(top)) {
            names.add(link.getClass().getName());
        }
        return names;
    }

    private static List<LogCapture.CapturedEvent> coordinateLines(After after) {
        List<LogCapture.CapturedEvent> lines = new ArrayList<>();
        for (LogCapture.CapturedEvent event : after.events()) {
            if (ERROR_LEVEL.equals(event.level()) && event.message().startsWith(COORDINATE_LINE_PREFIX)) {
                lines.add(event);
            }
        }
        return lines;
    }

    @ParameterizedTest(name = "rethrowsWhatDoWriteColThrew {0}")
    @EnumSource(Shape.class)
    void rethrowsWhatDoWriteColThrew(Shape shape) {
        Throwable before = before(shape);
        After after = after(shape);

        assertEquals(before.getClass(), after.thrown().getClass());
        assertEquals(before.getMessage(), after.thrown().getMessage());
        assertEquals(causeClassNames(before), causeClassNames(after.thrown()));
        switch (shape) {
            case IO_PLAIN, IO_READ_TIMEOUT_PREFIX, SOCKET_TIMEOUT, SOCKET_EXCEPTION, UNKNOWN_HOST, RETRIABLE_FROM_COLUMN_WRITER ->
                    assertSame(after.scenario().injected(), after.thrown());
            case JACKSON_OVER_SOCKET_TIMEOUT ->
                    assertSame(after.scenario().streamFailure().getCause(), after.thrown().getCause());
            default -> {}
        }
    }

    @ParameterizedTest(name = "logsOneValueFreeCoordinateLine {0}")
    @EnumSource(Shape.class)
    void logsOneValueFreeCoordinateLine(Shape shape) {
        After after = after(shape);

        List<LogCapture.CapturedEvent> lines = coordinateLines(after);
        if (shape == Shape.RETRIABLE_FROM_COLUMN_WRITER) {
            assertTrue(lines.isEmpty(), "a RetriableException is rethrown without the coordinate line");
            return;
        }
        assertEquals(1, lines.size());
        assertEquals(
                String.format(
                        COORDINATE_LINE_FORMAT,
                        TOPIC,
                        PARTITION,
                        OFFSET,
                        after.scenario().column().getName(),
                        after.thrown().getClass().getName()),
                lines.get(0).message());
        assertFalse(lines.get(0).hasThrowable(), "the coordinate line carries no throwable");
    }

    @ParameterizedTest(name = "carriesNoRecordValue {0}")
    @EnumSource(Shape.class)
    void carriesNoRecordValue(Shape shape) {
        After after = after(shape);

        for (Throwable link : ThrowableTexts.causeChain(after.thrown())) {
            assertFalse(String.valueOf(link.getMessage()).contains(SENTINEL), "a message of the thrown chain carries the value");
        }
        for (String text : ThrowableTexts.allTexts(after.thrown())) {
            assertFalse(text.contains(SENTINEL), "a thrown text carries the value");
        }
        InMemoryDLQ.ConnectDlqHeaders headers =
                InMemoryDLQ.connectHeaders(new RuntimeException(QUERY_ID_TEXT, after.thrown()));
        assertFalse(String.valueOf(headers.exceptionMessage()).contains(SENTINEL));
        assertFalse(headers.exceptionStacktrace().contains(SENTINEL));
        for (String text : after.logTexts()) {
            assertFalse(String.valueOf(text).contains(SENTINEL), "a log text carries the value");
        }
    }

    private static Outcome outcome(Throwable writerFailure, boolean errorsTolerance, boolean retryOnSocket) {
        try {
            Utils.handleException(
                    new RuntimeException(QUERY_ID_TEXT, writerFailure),
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

    private static Outcome expected(RetryClass retryClass, boolean errorsTolerance, boolean retryOnSocket) {
        if (retryClass == RetryClass.RETRIABLE || (retryClass == RetryClass.SOCKET && retryOnSocket)) {
            return Outcome.RETRIABLE;
        }
        return errorsTolerance ? Outcome.RETURNS : Outcome.RUNTIME_WRAP;
    }

    @ParameterizedTest(name = "keepsTheRetryOutcome {0}")
    @EnumSource(
            value = Shape.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = "RETRIABLE_FROM_COLUMN_WRITER")
    void keepsTheRetryOutcome(Shape shape) {
        Throwable before = before(shape);
        After after = after(shape);

        for (boolean errorsTolerance : new boolean[] {false, true}) {
            for (boolean retryOnSocket : new boolean[] {false, true}) {
                Outcome outcomeAfter = outcome(after.thrown(), errorsTolerance, retryOnSocket);
                assertEquals(outcome(before, errorsTolerance, retryOnSocket), outcomeAfter);
                assertEquals(expected(shape.retryClass, errorsTolerance, retryOnSocket), outcomeAfter);
            }
        }
    }
}
