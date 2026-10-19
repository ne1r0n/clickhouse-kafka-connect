package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.db.ClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.CountingHelperClient;
import com.clickhouse.kafka.connect.sink.db.ScriptedWriteFailureWriter;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * WRT-14 (D-11): pins what {@code Utils.handleException} decides for every failure kind that
 * reaches the generic catch of {@code ClickHouseWriter.doWriteColValue}. Each case drives the real
 * path: {@code ProxySinkTask.put} (shape A) or {@code ChunkFlusher.putDirect} (shape B),
 * {@code Processing.doLogic}, the real {@code ClickHouseWriter.doInsert}, {@code doInsertBatch} and
 * {@code doInsertRawBinary}, then the handler. It asserts the outcome only (retriable, data
 * exception, runtime wrap or return) plus two reach markers that WRT-13 leaves unchanged, so a
 * provocation that misses the catch fails instead of passing vacuously. It never inspects an
 * exception's text or chain and never builds an exception for the handler, so it must pass
 * unmodified before and after WRT-13. It extends nothing and carries no tag, so it runs without
 * Docker.
 */
public class WriterFailureClassificationUnitTest {

    enum Outcome {
        RETRIABLE,
        DATA_EXCEPTION,
        RUNTIME_WRAP,
        RETURNS
    }

    enum Entry {
        PROXY_SINK_TASK_PUT,
        CHUNK_FLUSHER_PUT_DIRECT
    }

    enum RetryClass {
        NON_RETRIABLE,
        RETRIABLE,
        SOCKET
    }

    /** One scripted write: the column and value handed to the real {@code doWriteColValue}. */
    private record Scenario(
            Column column,
            Data value,
            Supplier<OutputStream> stream,
            RuntimeException primitiveFailure,
            RuntimeException nestedFailure) {}

    enum FailureKind {
        INVALID_UUID(RetryClass.NON_RETRIABLE, () -> bytes(column("c_uuid", Type.UUID), string("not-a-uuid")));

        private final RetryClass retryClass;
        private final Supplier<Scenario> scenario;

        FailureKind(RetryClass retryClass, Supplier<Scenario> scenario) {
            this.retryClass = retryClass;
            this.scenario = scenario;
        }

        RetryClass retryClass() {
            return retryClass;
        }

        Scenario scenario() {
            return scenario.get();
        }
    }

    private static final String CONNECTOR_NAME = "wrt14-pin";
    private static final String TOPIC = "wrt14_topic";
    private static final String ERRORS_TOLERANCE_ALL = "all";
    private static final String ERRORS_TOLERANCE_NONE = "none";
    private static final String WRITE_VALUE_FAILURE_PREFIX = "Error writing value of ";
    private static final String INSERT_FAILURE_MESSAGE = "Error inserting records";
    private static final String ERROR_LEVEL = "ERROR";
    private static final int BATCH_SIZE = 3;

    private static Column column(String name, Type type) {
        return Column.builder().name(name).type(type).build();
    }

    private static Data string(String text) {
        return new Data(Schema.STRING_SCHEMA, text);
    }

    private static Scenario bytes(Column column, Data value) {
        return new Scenario(column, value, ByteArrayOutputStream::new, null, null);
    }

    private static Schema recordSchema() {
        return SchemaBuilder.struct()
                .field("id", Schema.INT32_SCHEMA)
                .field("name", Schema.STRING_SCHEMA)
                .build();
    }

    private static List<SinkRecord> batch() {
        Schema schema = recordSchema();
        List<SinkRecord> records = new ArrayList<>();
        for (int offset = 0; offset < BATCH_SIZE; offset++) {
            Struct struct = new Struct(schema).put("id", offset).put("name", "n" + offset);
            records.add(new SinkRecord(TOPIC, 0, null, null, schema, struct, offset));
        }
        return records;
    }

    private static Map<String, String> props(
            boolean errorsTolerance, boolean retryOnSocketException, boolean ignorePartitions) {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConfig.HOSTNAME, "localhost");
        props.put(ClickHouseSinkConfig.PORT, "8123");
        props.put(ClickHouseSinkConfig.DATABASE, "default");
        props.put(ClickHouseSinkConfig.USERNAME, "default");
        props.put(ClickHouseSinkConfig.PASSWORD, "");
        props.put(ClickHouseSinkConfig.ERRORS_TOLERANCE, errorsTolerance ? ERRORS_TOLERANCE_ALL : ERRORS_TOLERANCE_NONE);
        props.put(ClickHouseSinkConfig.RETRY_ON_SOCKET_EXCEPTION, String.valueOf(retryOnSocketException));
        props.put(ClickHouseSinkConfig.IGNORE_PARTITIONS_WHEN_BATCHING, String.valueOf(ignorePartitions));
        return props;
    }

    static Outcome expected(RetryClass retryClass, boolean errorsTolerance, boolean retryOnSocketException) {
        if (retryClass == RetryClass.RETRIABLE || (retryClass == RetryClass.SOCKET && retryOnSocketException)) {
            return Outcome.RETRIABLE;
        }
        return errorsTolerance ? Outcome.RETURNS : Outcome.RUNTIME_WRAP;
    }

    private static Outcome invoke(ProxySinkTask task, ClickHouseSinkConfig csc, InMemoryDLQ dlq, Entry entry) {
        try {
            if (entry == Entry.PROXY_SINK_TASK_PUT) {
                task.put(batch());
            } else {
                new ChunkFlusher(task, csc, dlq).putDirect(batch());
            }
            return Outcome.RETURNS;
        } catch (RetriableException e) {
            return Outcome.RETRIABLE;
        } catch (DataException e) {
            return Outcome.DATA_EXCEPTION;
        } catch (Exception e) {
            return Outcome.RUNTIME_WRAP;
        }
    }

    static Outcome run(
            FailureKind kind,
            boolean clientV2,
            boolean errorsTolerance,
            boolean retryOnSocketException,
            Entry entry,
            boolean ignorePartitions)
            throws Exception {
        Scenario scenario = kind.scenario();
        ClickHouseSinkConfig csc =
                new ClickHouseSinkConfig(props(errorsTolerance, retryOnSocketException, ignorePartitions));
        InMemoryDLQ dlq = new InMemoryDLQ();
        ProxySinkTask task = new ProxySinkTask(csc, dlq, CONNECTOR_NAME);
        try (CountingHelperClient chc = new CountingHelperClient(clientV2)) {
            ScriptedWriteFailureWriter writer =
                    new ScriptedWriteFailureWriter(csc, chc, scenario.column(), scenario.value(), scenario.stream());
            if (scenario.primitiveFailure() != null) {
                writer.failPrimitiveWith(scenario.primitiveFailure());
            }
            if (scenario.nestedFailure() != null) {
                writer.failNestedColumnWith(scenario.nestedFailure());
            }
            writer.installInto(task);

            try (LogCapture log = LogCapture.attach(ClickHouseWriter.class)) {
                Outcome outcome = invoke(task, csc, dlq, entry);

                List<LogCapture.CapturedEvent> errors = log.events(ERROR_LEVEL);
                String backticked = "`" + scenario.column().getName() + "`";
                assertTrue(
                        errors.stream().anyMatch(e -> e.message().startsWith(WRITE_VALUE_FAILURE_PREFIX)
                                && e.message().contains(backticked)),
                        kind + " missed the doWriteColValue catch: no '" + WRITE_VALUE_FAILURE_PREFIX
                                + "' ERROR event naming " + backticked);
                assertTrue(
                        errors.stream().anyMatch(e -> e.message().equals(INSERT_FAILURE_MESSAGE) && e.hasThrowable()),
                        kind + " missed the doInsertRawBinary catch: no '" + INSERT_FAILURE_MESSAGE
                                + "' ERROR event with a throwable");
                assertEquals(1, writer.hookCalls(), kind + " must reach the RowBinary hook exactly once");
                assertEquals(
                        outcome == Outcome.RETURNS ? BATCH_SIZE : 0,
                        dlq.size(),
                        kind + " DLQ count for outcome " + outcome);
                return outcome;
            }
        } finally {
            task.stop();
        }
    }

    static Stream<Arguments> matrix() {
        List<Arguments> cases = new ArrayList<>();
        for (FailureKind kind : FailureKind.values()) {
            for (boolean clientV2 : new boolean[] {false, true}) {
                for (boolean errorsTolerance : new boolean[] {false, true}) {
                    for (Entry entry : Entry.values()) {
                        cases.add(Arguments.of(kind, clientV2, errorsTolerance, false, entry));
                        if (kind.retryClass() == RetryClass.SOCKET) {
                            cases.add(Arguments.of(kind, clientV2, errorsTolerance, true, entry));
                        }
                    }
                }
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "WRT-14: {0} clientV2={1} errorsTolerance={2} retryOnSocketException={3} via {4}")
    @MethodSource("matrix")
    void classificationOutcomeIsPinned(
            FailureKind kind, boolean clientV2, boolean errorsTolerance, boolean retryOnSocketException, Entry entry)
            throws Exception {
        assertEquals(
                expected(kind.retryClass(), errorsTolerance, retryOnSocketException),
                run(kind, clientV2, errorsTolerance, retryOnSocketException, entry, false));
    }
}
