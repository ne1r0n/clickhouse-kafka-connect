package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.db.ClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.dlq.ErrorReporter;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.clickhouse.kafka.connect.sink.util.ThrowableTexts;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Fork-only put()-level pins for WRT-11, D-05 and WRT-10. The failure is injected by reflectively
 * replacing {@code Processing.dbWriter} (a non-final field) inside the started task with a started
 * {@code ClickHouseWriter} subclass whose protected RowBinary hooks throw. The real {@code
 * doInsertRawBinary} catch, the {@code Processing.doInsert} wrapping, the {@code
 * ProxySinkTask}/{@code ChunkFlusher} handling and {@code Utils.handleException} all run. The
 * assertions walk the cause chain because {@code Processing.doInsert} wraps every failure.
 */
public class WriterErrorPathLiveTest extends ClickHouseBase {

    private static final String TABLE_DDL =
            "CREATE TABLE %s (id Int32, name String) ENGINE = MergeTree ORDER BY id";
    private static final String PROXY_SINK_TASK_FIELD = "proxySinkTask";
    private static final String PROCESSING_FIELD = "processing";
    private static final String DB_WRITER_FIELD = "dbWriter";
    private static final int BATCH_SIZE = 3;
    private static final String ERRORS_TOLERANCE_ALL = "all";
    private static final String CONNECTOR_NAME_PROP = "name";
    private static final String TOPIC_PREFIX = "wrt11_null_message";
    private static final String TUPLE_TABLE_DDL =
            "CREATE TABLE %s (id Int32, at Array(Tuple(a Int32, b String))) ENGINE = MergeTree ORDER BY id";
    private static final String SENTINEL = "SENTINELq7Zx41kV";
    private static final String UUID_TABLE_DDL =
            "CREATE TABLE %s (id Int32, u UUID) ENGINE = MergeTree ORDER BY id";
    private static final String SERVER_EXCEPTION_CLASS = "com.clickhouse.client.api.ServerException";
    private static final String INSERT_FAILURE_MESSAGE = "Error inserting records";
    private static final String ERROR_LEVEL = "ERROR";
    private static final String INT128_TABLE_DDL =
            "CREATE TABLE %s (id Int32, n Nullable(Int128)) ENGINE = MergeTree ORDER BY id";
    private static final String NUMERIC_SENTINEL = "Zq7x41kV";
    private static final String MISSING_VALUE_HINT =
            "): no value provided for a non-nullable column without a default; add the field, make the column Nullable, or give it a DEFAULT";

    /**
     * A conversion failure that happens client-side while the connector serializes a value. Each
     * case carries the table, the failing column, the {@code Type} token and the exception class the
     * rendered text must name, and the value that must never appear.
     */
    enum LiveCase {
        INVALID_UUID(
                UUID_TABLE_DDL,
                "u",
                String.valueOf(Type.UUID),
                IllegalArgumentException.class.getName(),
                SENTINEL,
                WriterErrorPathLiveTest::uuidBatch),
        UNPARSEABLE_INT128(
                INT128_TABLE_DDL,
                "n",
                String.valueOf(Type.INT128),
                NumberFormatException.class.getName(),
                NUMERIC_SENTINEL,
                WriterErrorPathLiveTest::int128Batch);

        final String ddl;
        final String column;
        final String typeToken;
        final String originalClass;
        final String value;
        final Function<String, List<SinkRecord>> batch;

        LiveCase(
                String ddl,
                String column,
                String typeToken,
                String originalClass,
                String value,
                Function<String, List<SinkRecord>> batch) {
            this.ddl = ddl;
            this.column = column;
            this.typeToken = typeToken;
            this.originalClass = originalClass;
            this.value = value;
            this.batch = batch;
        }

        /** The column, type and class text the rendered failure must carry. */
        List<String> expectedRenderedFragments() {
            return ValueFreeDlqHeadersUnitTest.renderedFragments(column, typeToken, originalClass);
        }
    }

    /**
     * Throws the stored instance itself from both RowBinary hooks, never a wrapper, so the instance
     * reaches the cause chain by identity.
     */
    static final class FailingInsertWriter extends ClickHouseWriter {
        private final Exception failure;

        FailingInsertWriter(SinkTaskStatistics statistics, Exception failure) {
            super(statistics);
            if (!(failure instanceof IOException) && !(failure instanceof RuntimeException)) {
                throw new IllegalArgumentException(
                        "only IOException or RuntimeException can be injected, got "
                                + failure.getClass().getName());
            }
            this.failure = failure;
        }

        private void failFromHook() throws IOException {
            if (failure instanceof IOException) {
                throw (IOException) failure;
            }
            throw (RuntimeException) failure;
        }

        @Override
        protected void doInsertRawBinaryV1(
                List<Record> records, Table table, QueryIdentifier queryId, boolean supportDefaults)
                throws IOException {
            failFromHook();
        }

        @Override
        protected void doInsertRawBinaryV2(
                List<Record> records, Table table, QueryIdentifier queryId, boolean supportDefaults)
                throws IOException {
            failFromHook();
        }
    }

    private static void createTable(ClickHouseHelperClient chc, String tableName, String createTableQuery) {
        try (Records ignored = chc.queryV2(String.format(createTableQuery, tableName))) {
            // table created
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Schema recordSchema() {
        return SchemaBuilder.struct()
                .field("id", Schema.INT32_SCHEMA)
                .field("name", Schema.STRING_SCHEMA)
                .build();
    }

    private static List<SinkRecord> batch(String topic) {
        Schema schema = recordSchema();
        List<SinkRecord> records = new ArrayList<>();
        for (int offset = 0; offset < BATCH_SIZE; offset++) {
            Struct struct = new Struct(schema).put("id", offset).put("name", "n" + offset);
            records.add(new SinkRecord(topic, 0, null, null, schema, struct, offset));
        }
        return records;
    }

    private static List<SinkRecord> tupleBatchMissingField(String topic) {
        Schema elementSchema = SchemaBuilder.struct().field("b", Schema.STRING_SCHEMA).build();
        Schema schema = SchemaBuilder.struct()
                .field("id", Schema.INT32_SCHEMA)
                .field("at", SchemaBuilder.array(elementSchema).build())
                .build();
        Struct element = new Struct(elementSchema).put("b", SENTINEL);
        Struct struct = new Struct(schema).put("id", 1).put("at", List.of(element));
        return List.of(new SinkRecord(topic, 0, null, null, schema, struct, 0));
    }

    private static List<SinkRecord> uuidBatch(String topic) {
        Schema schema = SchemaBuilder.struct()
                .field("id", Schema.INT32_SCHEMA)
                .field("u", Schema.STRING_SCHEMA)
                .build();
        List<SinkRecord> records = new ArrayList<>();
        for (int offset = 0; offset < BATCH_SIZE; offset++) {
            Struct struct = new Struct(schema).put("id", offset).put("u", SENTINEL);
            records.add(new SinkRecord(topic, 0, null, null, schema, struct, offset));
        }
        return records;
    }

    /**
     * The column is Nullable on purpose: {@code validateDataSchema} skips a nullable root column, so
     * the STRING field reaches the Int128 conversion instead of failing schema validation first.
     */
    private static List<SinkRecord> int128Batch(String topic) {
        Schema schema = SchemaBuilder.struct()
                .field("id", Schema.INT32_SCHEMA)
                .field("n", Schema.OPTIONAL_STRING_SCHEMA)
                .build();
        List<SinkRecord> records = new ArrayList<>();
        for (int offset = 0; offset < BATCH_SIZE; offset++) {
            Struct struct = new Struct(schema).put("id", offset).put("n", NUMERIC_SENTINEL);
            records.add(new SinkRecord(topic, 0, null, null, schema, struct, offset));
        }
        return records;
    }

    private static Object readField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void writeField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static List<Throwable> causeChain(Throwable top) {
        List<Throwable> chain = new ArrayList<>();
        for (Throwable t = top; t != null && !chain.contains(t); t = t.getCause()) {
            chain.add(t);
        }
        return chain;
    }

    private static boolean chainContainsInstance(Throwable top, Throwable expected) {
        return causeChain(top).stream().anyMatch(t -> t == expected);
    }

    private static boolean chainContainsType(Throwable top, Class<? extends Throwable> type) {
        return causeChain(top).stream().anyMatch(type::isInstance);
    }

    /**
     * Runs one put of {@link #BATCH_SIZE} records against a started task whose writer always fails
     * with {@code failure}.
     *
     * @return the Throwable thrown by put, or null when put returned
     */
    private Throwable putWithInjectedFailure(
            TestInfo testInfo, boolean toleranceAll, Exception failure, ErrorReporter reporter) throws Exception {
        return putBatch(testInfo, TABLE_DDL, toleranceAll, reporter, failure, WriterErrorPathLiveTest::batch);
    }

    /**
     * Creates the table, starts a task, optionally swaps in a writer that always fails with {@code
     * failure} (null keeps the real writer), puts the batch and checks that no row landed.
     *
     * @return the Throwable thrown by put, or null when put returned
     */
    private Throwable putBatch(
            TestInfo testInfo,
            String tableDdl,
            boolean toleranceAll,
            ErrorReporter reporter,
            Exception failure,
            Function<String, List<SinkRecord>> batchForTopic)
            throws Exception {
        Map<String, String> props = getBaseProps();
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        if (toleranceAll) {
            props.put(ClickHouseSinkConfig.ERRORS_TOLERANCE, ERRORS_TOLERANCE_ALL);
        }
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String topic = createTopicName(TOPIC_PREFIX);
        ClickHouseTestHelpers.dropTable(chc, topic);
        createTable(chc, topic, tableDdl);

        ClickHouseSinkTask task = new ClickHouseSinkTask();
        if (reporter != null) {
            task.setErrorReporter(reporter);
        }
        FailingInsertWriter writer =
                failure == null ? null : new FailingInsertWriter(new SinkTaskStatistics(null, 0), failure);
        try {
            task.start(props);
            if (writer != null) {
                assertTrue(writer.start(new ClickHouseSinkConfig(props)), "the injected writer must start");
                Object proxySinkTask = readField(task, PROXY_SINK_TASK_FIELD);
                Object processing = readField(proxySinkTask, PROCESSING_FIELD);
                writeField(processing, DB_WRITER_FIELD, writer);
            }
            try {
                task.put(batchForTopic.apply(topic));
                return null;
            } catch (Exception thrown) {
                return thrown;
            }
        } finally {
            if (writer != null) {
                writer.stop();
            }
            task.stop();
            try {
                assertEquals(0, ClickHouseTestHelpers.countRows(chc, topic), "a failed batch must write no rows");
            } finally {
                ClickHouseTestHelpers.dropTable(chc, topic);
            }
        }
    }

    @Test
    @DisplayName("WRT-11: errors.tolerance=none surfaces a message-less writer failure as itself, with no NullPointerException in the chain")
    void messageLessFailureSurfacesAsItselfUnderToleranceNone(TestInfo testInfo) throws Exception {
        RuntimeException failure = new RuntimeException();

        Throwable thrown = putWithInjectedFailure(testInfo, false, failure, null);

        assertNotNull(thrown, "put must fail when errors.tolerance=none");
        assertTrue(chainContainsInstance(thrown, failure), "the injected instance must be in the cause chain");
        assertFalse(chainContainsType(thrown, NullPointerException.class), "no NullPointerException may replace it");
        assertFalse(thrown instanceof RetriableException, "a message-less RuntimeException is not retriable");
    }

    @Test
    @DisplayName("WRT-11: errors.tolerance=all sends the whole batch to the DLQ for a message-less writer failure")
    void messageLessFailureUnderToleranceAllGoesToDlq(TestInfo testInfo) throws Exception {
        InMemoryDLQ dlq = new InMemoryDLQ();

        Throwable thrown = putWithInjectedFailure(testInfo, true, new RuntimeException(), dlq);

        assertNull(thrown, "put must return when errors.tolerance=all");
        assertEquals(BATCH_SIZE, dlq.size(), "the whole batch must reach the DLQ");
    }

    @Test
    @DisplayName("D-05: errors.tolerance=none keeps a message-less IOException from the writer in the chain, with no NullPointerException")
    void messageLessIoExceptionFailsPutUnderToleranceNone(TestInfo testInfo) throws Exception {
        IOException failure = new IOException();

        Throwable thrown = putWithInjectedFailure(testInfo, false, failure, null);

        assertNotNull(thrown, "put must fail when errors.tolerance=none");
        assertTrue(chainContainsInstance(thrown, failure), "the injected instance must be in the cause chain");
        assertFalse(chainContainsType(thrown, NullPointerException.class), "no NullPointerException may replace it");
    }

    @Test
    @DisplayName("D-05: errors.tolerance=all sends the whole batch to the DLQ for a message-less IOException")
    void messageLessIoExceptionUnderToleranceAllGoesToDlq(TestInfo testInfo) throws Exception {
        InMemoryDLQ dlq = new InMemoryDLQ();

        Throwable thrown = putWithInjectedFailure(testInfo, true, new IOException(), dlq);

        assertNull(thrown, "put must return when errors.tolerance=all");
        assertEquals(BATCH_SIZE, dlq.size(), "the whole batch must reach the DLQ");
    }

    @Test
    @DisplayName("WRT-11: a message-less SocketTimeoutException from the writer makes put throw RetriableException")
    void messageLessSocketTimeoutIsRetriable(TestInfo testInfo) throws Exception {
        SocketTimeoutException failure = new SocketTimeoutException();

        Throwable thrown = putWithInjectedFailure(testInfo, false, failure, null);

        assertNotNull(thrown, "put must fail with a retriable exception");
        assertInstanceOf(RetriableException.class, thrown);
        assertTrue(chainContainsInstance(thrown, failure), "the injected instance must be in the cause chain");
    }

    @Test
    @DisplayName("WRT-10: a record whose Array(Tuple) element lacks a non-nullable field fails put with a DataException that names the dotted column and carries no record value")
    void missingTupleFieldFailsPutWithColumnNamingDataException(TestInfo testInfo) throws Exception {
        Throwable thrown = putBatch(
                testInfo, TUPLE_TABLE_DDL, false, null, null, WriterErrorPathLiveTest::tupleBatchMissingField);

        assertNotNull(thrown, "put must fail when errors.tolerance=none");
        List<Throwable> chain = causeChain(thrown);
        DataException dataException = chain.stream()
                .filter(DataException.class::isInstance)
                .map(DataException.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no DataException in the cause chain: " + chain));
        assertTrue(
                dataException.getMessage().startsWith("column 'at.a' ("),
                "the dotted path of the field must be named: " + dataException.getMessage());
        assertTrue(dataException.getMessage().endsWith(MISSING_VALUE_HINT), dataException.getMessage());
        chain.forEach(t -> assertFalse(
                String.valueOf(t.getMessage()).contains(SENTINEL), "no chain message may carry a record value"));
    }

    @ParameterizedTest(name = "WRT-13 live: errors.tolerance=none fails put value-free for {0}")
    @EnumSource(LiveCase.class)
    void conversionFailureFailsPutValueFreeUnderToleranceNone(LiveCase liveCase, TestInfo testInfo)
            throws Exception {
        try (LogCapture log = LogCapture.attach(ClickHouseWriter.class)) {
            Throwable thrown = putBatch(testInfo, liveCase.ddl, false, null, null, liveCase.batch);

            assertNotNull(thrown, "put must fail when errors.tolerance=none");
            assertFalse(thrown instanceof RetriableException, "a conversion failure is not retriable");
            ThrowableTexts.allTexts(thrown)
                    .forEach(text -> assertFalse(text.contains(liveCase.value), "the thrown chain carries the value"));
            // Connect's task status wraps put()'s exception in its own value-free ConnectException
            // ("Exiting WorkerSinkTask due to unrecoverable exception.") and prints the trace of the
            // whole chain, so rendering the exception put() throws is the faithful approximation
            // (22-RESEARCH Pitfall 8).
            String rendered = ThrowableTexts.printedStackTrace(thrown);
            liveCase.expectedRenderedFragments().forEach(fragment -> assertTrue(
                    rendered.contains(fragment), "the rendering lacks '" + fragment + "':\n" + rendered));
            ThrowableTexts.causeChain(thrown)
                    .forEach(t -> assertFalse(
                            SERVER_EXCEPTION_CLASS.equals(t.getClass().getName()),
                            "a client-side conversion failure must not surface as a ServerException (D-13)"));

            List<LogCapture.CapturedEvent> insertErrors = log.events(ERROR_LEVEL).stream()
                    .filter(e -> INSERT_FAILURE_MESSAGE.equals(e.message()) && e.hasThrowable())
                    .toList();
            assertFalse(insertErrors.isEmpty(), "no '" + INSERT_FAILURE_MESSAGE + "' ERROR event with a throwable");
            assertTrue(
                    insertErrors.stream().anyMatch(e -> {
                        String text = String.join("\n", e.throwableTexts());
                        return liveCase.expectedRenderedFragments().stream().allMatch(text::contains);
                    }),
                    "the logged throwable must name the column, the type and the class");
            log.allTexts().forEach(text -> assertFalse(text.contains(liveCase.value), "a log text carries the value"));
            log.events().stream()
                    .flatMap(e -> e.throwableTexts().stream())
                    .forEach(line -> assertFalse(
                            line.startsWith(SERVER_EXCEPTION_CLASS), "a logged throwable line is a ServerException"));
        }
    }

    @ParameterizedTest(name = "WRT-13 live: errors.tolerance=all sends value-free DLQ headers for {0}")
    @EnumSource(LiveCase.class)
    void conversionFailureDlqHeadersAreValueFreeUnderToleranceAll(LiveCase liveCase, TestInfo testInfo)
            throws Exception {
        InMemoryDLQ dlq = new InMemoryDLQ();

        Throwable thrown = putBatch(testInfo, liveCase.ddl, true, dlq, null, liveCase.batch);

        assertNull(thrown, "put must return when errors.tolerance=all");
        ValueFreeDlqHeadersUnitTest.assertValueFreeDlqHeaders(
                dlq, BATCH_SIZE, liveCase.expectedRenderedFragments(), liveCase.value);
    }
}
