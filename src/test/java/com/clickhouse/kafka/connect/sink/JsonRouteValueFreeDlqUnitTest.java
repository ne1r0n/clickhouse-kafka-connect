package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.clickhouse.kafka.connect.sink.db.CapturingClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.ClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.JsonRouteValueFreeUnitTest;
import com.clickhouse.kafka.connect.sink.db.JsonRouteValueFreeUnitTest.Scripted;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import com.clickhouse.kafka.connect.sink.processing.Processing;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.clickhouse.kafka.connect.sink.util.ThrowableTexts;
import com.clickhouse.kafka.connect.util.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Drives schemaless batches through the production path ({@code ProxySinkTask.put}, {@code
 * Processing}, the real {@code ClickHouseWriter} JSON serialization loop with only its network hooks
 * replaced) and checks every surface a record value could leave through when Jackson fails on a
 * value: the DLQ headers Kafka Connect would write, the exception thrown to the worker and the
 * connector's log lines.
 *
 * <p>The class extends nothing, carries no tag and uses no mocking library, so it runs without
 * Docker.
 */
public class JsonRouteValueFreeDlqUnitTest {

    private static final String SENTINEL = JsonRouteValueFreeUnitTest.SENTINEL;
    private static final String TOPIC = JsonRouteValueFreeUnitTest.TOPIC;
    private static final String COLUMN = JsonRouteValueFreeUnitTest.COLUMN;
    private static final int BATCH_SIZE = 3;
    private static final String ERRORS_TOLERANCE_ALL = "all";
    private static final String ERRORS_TOLERANCE_NONE = "none";

    /** Where the value reaches Jackson: the scripted exception's message, or the Map key of its reference chain. */
    enum Shape {
        JSON_KEY_IN_REFERENCE_CHAIN(SENTINEL, JsonRouteValueFreeUnitTest.BOOM),
        JSON_SCRIPTED_RUNTIME(JsonRouteValueFreeUnitTest.SCRIPTED_KEY, SENTINEL);

        private final String key;
        private final String failureMessage;

        Shape(String key, String failureMessage) {
            this.key = key;
            this.failureMessage = failureMessage;
        }
    }

    private static List<SinkRecord> batch(Shape shape) {
        List<SinkRecord> records = new ArrayList<>();
        for (int offset = 0; offset < BATCH_SIZE; offset++) {
            Map<String, Object> inner = new HashMap<>();
            inner.put(shape.key, new Scripted(new RuntimeException(shape.failureMessage)));
            Map<String, Object> value = new HashMap<>();
            value.put(COLUMN, inner);
            records.add(new SinkRecord(TOPIC, 0, null, null, null, value, offset));
        }
        return records;
    }

    /** What one {@code put} threw and the log texts the connector wrote meanwhile. */
    private record Run(Throwable thrown, InMemoryDLQ dlq, List<String> logTexts) {}

    private static Run put(Shape shape, String errorsTolerance) throws Exception {
        ClickHouseSinkConfig csc =
                JsonRouteValueFreeUnitTest.config(JsonRouteValueFreeUnitTest.Route.SCHEMALESS, errorsTolerance);
        InMemoryDLQ dlq = new InMemoryDLQ();
        ProxySinkTask task =
                new ProxySinkTask(csc, dlq, new CapturingClickHouseWriter(JsonRouteValueFreeUnitTest.table(), csc));
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
            return new Run(thrown, dlq, texts);
        } finally {
            task.stop();
        }
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
        assertNoSentinel(run);
    }
}
