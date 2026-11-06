package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.db.CapturingClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.ClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import com.clickhouse.kafka.connect.sink.processing.Processing;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.clickhouse.kafka.connect.sink.util.ThrowableTexts;
import com.clickhouse.kafka.connect.util.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Drives a batch through the production path ({@code ProxySinkTask.put}, {@code Processing}, the
 * real {@code ClickHouseWriter} serialization loop with only its network hooks replaced) and checks
 * every surface a record value could leave through: the DLQ headers Kafka Connect would write, the
 * exception thrown to the worker and the connector's log lines. Each record of the batch holds the
 * sentinel as its field value, and each value-carrying failure must name the first record's
 * coordinates and column in one log line without it.
 *
 * <p>The class extends nothing, carries no tag and uses no mocking library, so it runs without
 * Docker.
 */
public class WriterRecordContextDlqUnitTest {

    private static final String SENTINEL = "RECORD-VALUE-SENTINEL-45";
    private static final String TOPIC = "ctx-topic";
    private static final int PARTITION = 0;
    private static final int BATCH_SIZE = 3;
    private static final String CONNECTOR_NAME = "record-context-pin";
    private static final String ERROR_LEVEL = "ERROR";
    private static final String COORDINATE_LINE_PREFIX = "Record serialization failed: ";
    private static final String ERRORS_TOLERANCE_ALL = "all";
    private static final String ERRORS_TOLERANCE_NONE = "none";

    /** A value-carrying failure: one column and the type its STRING field cannot be written as. */
    enum Shape {
        INVALID_UUID("c_uuid", Type.UUID),
        UNPARSEABLE_INT128("c_int128", Type.INT128);

        private final String column;
        private final Type type;

        Shape(String column, Type type) {
            this.column = column;
            this.type = type;
        }
    }

    private static ClickHouseSinkConfig config(String errorsTolerance) {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConfig.HOSTNAME, "localhost");
        props.put(ClickHouseSinkConfig.PORT, "8123");
        props.put(ClickHouseSinkConfig.DATABASE, "default");
        props.put(ClickHouseSinkConfig.USERNAME, "default");
        props.put(ClickHouseSinkConfig.PASSWORD, "");
        props.put(ClickHouseSinkConfig.ERRORS_TOLERANCE, errorsTolerance);
        return new ClickHouseSinkConfig(props);
    }

    private static List<SinkRecord> batch(Shape shape) {
        Schema schema = SchemaBuilder.struct().field(shape.column, Schema.STRING_SCHEMA).build();
        List<SinkRecord> records = new ArrayList<>();
        for (int offset = 0; offset < BATCH_SIZE; offset++) {
            Struct struct = new Struct(schema).put(shape.column, SENTINEL);
            records.add(new SinkRecord(TOPIC, PARTITION, null, null, schema, struct, offset));
        }
        return records;
    }

    private static CapturingClickHouseWriter writer(Shape shape, ClickHouseSinkConfig csc) {
        Column column = Column.builder().name(shape.column).type(shape.type).build();
        return new CapturingClickHouseWriter(new Table("default", TOPIC, false, List.of(column), 1), csc);
    }

    /** What one {@code put} threw and the log texts the connector wrote meanwhile. */
    private record Run(Throwable thrown, InMemoryDLQ dlq, List<LogCapture.CapturedEvent> writerEvents, List<String> logTexts) {}

    private static Run put(Shape shape, String errorsTolerance) throws Exception {
        ClickHouseSinkConfig csc = config(errorsTolerance);
        InMemoryDLQ dlq = new InMemoryDLQ();
        ProxySinkTask task = new ProxySinkTask(csc, dlq, writer(shape, csc));
        try (LogCapture writerLog = LogCapture.attach(ClickHouseWriter.class);
                LogCapture processingLog = LogCapture.attach(Processing.class);
                LogCapture taskLog = LogCapture.attach(ProxySinkTask.class);
                LogCapture utilsLog = LogCapture.attach(Utils.class)) {
            Throwable thrown = null;
            try {
                task.put(batch(shape));
            } catch (Throwable t) {
                thrown = t;
            }
            List<String> texts = new ArrayList<>();
            texts.addAll(writerLog.allTexts());
            texts.addAll(processingLog.allTexts());
            texts.addAll(taskLog.allTexts());
            texts.addAll(utilsLog.allTexts());
            return new Run(thrown, dlq, writerLog.events(), texts);
        } finally {
            task.stop();
        }
    }

    private static void assertOneCoordinateLine(Shape shape, Run run) {
        List<LogCapture.CapturedEvent> lines = new ArrayList<>();
        for (LogCapture.CapturedEvent event : run.writerEvents()) {
            if (ERROR_LEVEL.equals(event.level()) && event.message().startsWith(COORDINATE_LINE_PREFIX)) {
                lines.add(event);
            }
        }
        assertEquals(1, lines.size(), "exactly one coordinate line for the first failing record");
        String message = lines.get(0).message();
        assertTrue(message.contains("topic [" + TOPIC + "]"), message);
        assertTrue(message.contains("partition [" + PARTITION + "]"), message);
        assertTrue(message.contains("offset [0]"), message);
        assertTrue(message.contains("column `" + shape.column + "`"), message);
        assertFalse(lines.get(0).hasThrowable(), "the coordinate line carries no throwable");
    }

    private static void assertNoSentinel(Run run) {
        for (String text : run.logTexts()) {
            assertFalse(String.valueOf(text).contains(SENTINEL), "a log text carries the value");
        }
    }

    @ParameterizedTest(name = "dlqHeadersAndLogsCarryNoRecordValue errors.tolerance=all {0}")
    @EnumSource(Shape.class)
    void dlqHeadersAndLogsCarryNoRecordValue(Shape shape) throws Exception {
        Run run = put(shape, ERRORS_TOLERANCE_ALL);

        assertNull(run.thrown(), "errors.tolerance=all returns from put");
        assertEquals(BATCH_SIZE, run.dlq().size());
        for (Throwable reported : run.dlq().exceptions()) {
            assertNotNull(reported);
            InMemoryDLQ.ConnectDlqHeaders headers = InMemoryDLQ.connectHeaders(reported);
            assertFalse(String.valueOf(headers.exceptionMessage()).contains(SENTINEL), "DLQ exception message");
            assertFalse(headers.exceptionStacktrace().contains(SENTINEL), "DLQ exception stacktrace");
        }
        assertOneCoordinateLine(shape, run);
        assertNoSentinel(run);
    }

    @ParameterizedTest(name = "thrownChainAndLogsCarryNoRecordValue errors.tolerance=none {0}")
    @EnumSource(Shape.class)
    void thrownChainAndLogsCarryNoRecordValue(Shape shape) throws Exception {
        Run run = put(shape, ERRORS_TOLERANCE_NONE);

        Throwable thrown = run.thrown();
        assertNotNull(thrown, "errors.tolerance=none fails the task");
        assertFalse(thrown instanceof RetriableException, "a serialization failure is not retriable");
        for (Throwable link : ThrowableTexts.causeChain(thrown)) {
            assertFalse(String.valueOf(link.getMessage()).contains(SENTINEL), "a message of the thrown chain");
        }
        for (String text : ThrowableTexts.allTexts(thrown)) {
            assertFalse(text.contains(SENTINEL), "a thrown text");
        }
        assertEquals(0, run.dlq().size());
        assertOneCoordinateLine(shape, run);
        assertNoSentinel(run);
    }
}
