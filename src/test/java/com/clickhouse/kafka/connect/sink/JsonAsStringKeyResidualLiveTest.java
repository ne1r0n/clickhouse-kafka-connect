package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.ClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.dlq.ErrorReporter;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.clickhouse.kafka.connect.sink.util.ThrowableTexts;
import com.clickhouse.kafka.connect.util.WithheldMessageException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Live pin of a Jackson failure on the opt-in JSON-as-string route. With {@code
 * input_format_binary_read_json_as_string=1} the writer serializes a Struct with Jackson, and a
 * value Jackson cannot serialize makes it add {@code (through reference chain: ...)} to its
 * message, which can name a {@code Map} key. The writer withholds that message like any other
 * conversion failure, so the key must reach none of the destinations an operator reads: the task
 * status cause chain, the DLQ headers, the {@code Error inserting records} throwable and the {@code
 * Error writing value of} line. The test drives {@code ClickHouseSinkTask.put} against the real
 * writer.
 *
 * <p>The matrix holds Connect-typed values only. The failure trigger is a content-free {@code
 * java.lang.Object} put into the Map after both {@code Struct.put} calls, because Connect validates
 * a Struct on {@code put}. A bean whose own exception text carries data is not a Connect value type
 * and is out of scope.
 *
 * <p>Every case that claims the Jackson route was reached asserts the Jackson exception class in
 * the cause chain, directly or as the original class of a withheld copy, so a green result cannot
 * come from a failure that never reached Jackson. The control without the setting asserts the
 * opposite route: schema validation refuses the JSON column first, so the {@code Writing JSON in
 * binary is not supported yet} exception inside the writer's JSON branch is not what put()
 * surfaces (measured), and the asserted text is the validation message.
 */
public class JsonAsStringKeyResidualLiveTest extends ClickHouseBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonAsStringKeyResidualLiveTest.class);

    private static final String LOG_PREFIX = "JSON_AS_STRING_RESIDUAL";
    private static final String TABLE_DDL = "CREATE TABLE %s (id Int32, j JSON) ENGINE = MergeTree ORDER BY id";
    private static final String TOPIC_PREFIX = "json_as_string_residual";
    private static final String CONNECTOR_NAME_PROP = "name";
    private static final String ERRORS_TOLERANCE_ALL = "all";
    private static final String JSON_AS_STRING_SETTING = "input_format_binary_read_json_as_string=1";
    private static final String SETTINGS_SEPARATOR = ",";
    private static final String KEY_SENTINEL = "SENTKEYa1b2";
    private static final String VALUE_SENTINEL = "SENTVALc3d4";
    private static final String STRING_SENTINEL = "SENTSTRe5f6";
    private static final String LIST_SENTINEL = "SENTLSTg7h8";
    private static final List<String> VALUE_SENTINELS = List.of(VALUE_SENTINEL, STRING_SENTINEL, LIST_SENTINEL);
    private static final List<String> ALL_SENTINELS =
            List.of(KEY_SENTINEL, VALUE_SENTINEL, STRING_SENTINEL, LIST_SENTINEL);
    private static final String ORDINARY_KEY = "ordinary";
    private static final String ID_FIELD = "id";
    private static final String JSON_FIELD = "j";
    private static final String MAP_FIELD = "m";
    private static final String LIST_FIELD = "l";
    private static final String STRING_FIELD = "s";
    private static final String INSERT_FAILURE_MESSAGE = "Error inserting records";
    private static final String WRITE_VALUE_FAILURE_PREFIX = "Error writing value of";
    private static final String ERROR_LEVEL = "ERROR";
    private static final String JACKSON_FAILURE_CLASS = "com.fasterxml.jackson.databind.exc.InvalidDefinitionException";
    private static final String JACKSON_WITHHELD_LINE = JACKSON_FAILURE_CLASS + " (message withheld: may contain record data)";
    private static final String UNSUPPORTED_JSON_TEXT = "has unsupported type [JSON] for RowBinary serialization";
    private static final String SERVER_VERSION_LOG_FORMAT = "{} server version={}";

    private static final Schema MAP_INNER_SCHEMA = SchemaBuilder.struct()
            .field(MAP_FIELD, SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.STRING_SCHEMA).build())
            .field(STRING_FIELD, Schema.STRING_SCHEMA)
            .build();
    private static final Schema LIST_INNER_SCHEMA = SchemaBuilder.struct()
            .field(LIST_FIELD, SchemaBuilder.array(Schema.STRING_SCHEMA).build())
            .field(STRING_FIELD, Schema.STRING_SCHEMA)
            .build();

    private static Schema outerSchema(Schema innerSchema) {
        return SchemaBuilder.struct()
                .field(ID_FIELD, Schema.INT32_SCHEMA)
                .field(JSON_FIELD, innerSchema)
                .build();
    }

    private static List<SinkRecord> record(String topic, Schema innerSchema, Struct inner) {
        Schema schema = outerSchema(innerSchema);
        Struct outer = new Struct(schema).put(ID_FIELD, 1).put(JSON_FIELD, inner);
        return List.of(new SinkRecord(topic, 0, null, null, schema, outer, 0));
    }

    /** A Map whose ordinary entry holds a value sentinel and whose failing entry is keyed by the key sentinel. */
    private static List<SinkRecord> mapBatch(String topic) {
        Map<String, Object> entries = new HashMap<>();
        entries.put(ORDINARY_KEY, VALUE_SENTINEL);
        Struct inner = new Struct(MAP_INNER_SCHEMA).put(MAP_FIELD, entries).put(STRING_FIELD, STRING_SENTINEL);
        List<SinkRecord> batch = record(topic, MAP_INNER_SCHEMA, inner);
        entries.put(KEY_SENTINEL, new Object());
        return batch;
    }

    private static List<SinkRecord> listBatch(String topic) {
        List<Object> items = new ArrayList<>();
        items.add(LIST_SENTINEL);
        Struct inner = new Struct(LIST_INNER_SCHEMA).put(LIST_FIELD, items).put(STRING_FIELD, STRING_SENTINEL);
        List<SinkRecord> batch = record(topic, LIST_INNER_SCHEMA, inner);
        items.add(new Object());
        return batch;
    }

    private static void createTable(ClickHouseHelperClient chc, String tableName) {
        try (Records ignored = chc.queryV2(String.format(TABLE_DDL, tableName))) {
            // table created
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates the table, starts a task on the real writer, puts the batch and checks that no row
     * landed.
     *
     * @return the Throwable thrown by put, or null when put returned
     */
    private Throwable putBatch(
            TestInfo testInfo,
            boolean jsonAsString,
            boolean toleranceAll,
            ErrorReporter reporter,
            Function<String, List<SinkRecord>> batchForTopic)
            throws Exception {
        Map<String, String> props = getBaseProps();
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        if (jsonAsString) {
            props.merge(
                    ClickHouseSinkConfig.CLICKHOUSE_SETTINGS,
                    JSON_AS_STRING_SETTING,
                    (existing, added) -> existing + SETTINGS_SEPARATOR + added);
        }
        if (toleranceAll) {
            props.put(ClickHouseSinkConfig.ERRORS_TOLERANCE, ERRORS_TOLERANCE_ALL);
        }
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String topic = createTopicName(TOPIC_PREFIX);
        ClickHouseTestHelpers.dropTable(chc, topic);
        createTable(chc, topic);

        ClickHouseSinkTask task = new ClickHouseSinkTask();
        if (reporter != null) {
            task.setErrorReporter(reporter);
        }
        try {
            task.start(props);
            try {
                task.put(batchForTopic.apply(topic));
                return null;
            } catch (Exception thrown) {
                return thrown;
            }
        } finally {
            task.stop();
            try {
                assertEquals(0, ClickHouseTestHelpers.countRows(chc, topic), "a failed batch must write no rows");
            } finally {
                ClickHouseTestHelpers.dropTable(chc, topic);
            }
        }
    }

    private static void assertJacksonRouteReached(Throwable thrown) {
        List<Throwable> chain = ThrowableTexts.causeChain(thrown);
        assertTrue(
                chain.stream()
                        .anyMatch(t -> JACKSON_FAILURE_CLASS.equals(t.getClass().getName())
                                || t instanceof WithheldMessageException w
                                        && JACKSON_FAILURE_CLASS.equals(w.getOriginalClassName())),
                "the Jackson route was not reached; the cause chain is: "
                        + chain.stream()
                                .map(t -> t instanceof WithheldMessageException w
                                        ? t.getClass().getName() + " (original " + w.getOriginalClassName() + ")"
                                        : t.getClass().getName())
                                .toList());
    }

    private static void assertNoneOf(String destination, String text, List<String> sentinels) {
        for (String sentinel : sentinels) {
            assertFalse(text.contains(sentinel), destination + " carries " + sentinel + ":\n" + text);
        }
    }

    private static List<LogCapture.CapturedEvent> insertErrors(LogCapture log) {
        return log.events(ERROR_LEVEL).stream()
                .filter(e -> INSERT_FAILURE_MESSAGE.equals(e.message()) && e.hasThrowable())
                .toList();
    }

    private static List<LogCapture.CapturedEvent> writeValueFailureLines(LogCapture log) {
        return log.events(ERROR_LEVEL).stream()
                .filter(e -> e.message() != null && e.message().startsWith(WRITE_VALUE_FAILURE_PREFIX))
                .toList();
    }

    /** Runs after {@code ClickHouseBase.setup()}, so the container and the database exist. */
    @BeforeAll
    void logServerVersion() throws Exception {
        try (ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps())) {
            LOGGER.info(SERVER_VERSION_LOG_FORMAT, LOG_PREFIX, NullableRoundTrip.serverVersion(chc));
        }
    }

    @Test
    @DisplayName("errors.tolerance=none keeps the Map key out of the task status chain and the Error inserting records throwable")
    void mapKeyStaysOutOfTaskStatusAndInsertErrorUnderToleranceNone(TestInfo testInfo) throws Exception {
        try (LogCapture log = LogCapture.attach(ClickHouseWriter.class)) {
            Throwable thrown =
                    putBatch(testInfo, true, false, null, JsonAsStringKeyResidualLiveTest::mapBatch);

            assertNotNull(thrown, "put must fail when errors.tolerance=none");
            assertJacksonRouteReached(thrown);
            // Connect's task status prints the trace of the whole chain, so rendering the exception
            // put() throws is the faithful approximation.
            String rendering = ThrowableTexts.printedStackTrace(thrown);
            assertNoneOf("task status", rendering, ALL_SENTINELS);
            assertTrue(rendering.contains(JACKSON_WITHHELD_LINE), "task status lacks the withheld Jackson line:\n" + rendering);

            List<LogCapture.CapturedEvent> insertErrors = insertErrors(log);
            assertFalse(insertErrors.isEmpty(), "no '" + INSERT_FAILURE_MESSAGE + "' ERROR event with a throwable");
            insertErrors.forEach(e ->
                    assertNoneOf(INSERT_FAILURE_MESSAGE + " throwable", String.join("\n", e.throwableTexts()), ALL_SENTINELS));

            List<LogCapture.CapturedEvent> writeLines = writeValueFailureLines(log);
            assertFalse(writeLines.isEmpty(), "no '" + WRITE_VALUE_FAILURE_PREFIX + "' ERROR line");
            writeLines.forEach(e -> assertNoneOf(WRITE_VALUE_FAILURE_PREFIX + " line", e.message(), ALL_SENTINELS));
            log.allTexts().forEach(text -> assertNoneOf("a log text", text, ALL_SENTINELS));
        }
    }

    @Test
    @DisplayName("errors.tolerance=all keeps the Map key out of both DLQ exception headers")
    void mapKeyStaysOutOfBothDlqHeadersUnderToleranceAll(TestInfo testInfo) throws Exception {
        InMemoryDLQ dlq = new InMemoryDLQ();

        Throwable thrown = putBatch(testInfo, true, true, dlq, JsonAsStringKeyResidualLiveTest::mapBatch);

        assertNull(thrown, "put must return when errors.tolerance=all");
        assertEquals(1, dlq.size(), "the record must reach the DLQ");
        for (Throwable reported : dlq.exceptions()) {
            assertJacksonRouteReached(reported);
            InMemoryDLQ.ConnectDlqHeaders headers = InMemoryDLQ.connectHeaders(reported);
            assertTrue(
                    headers.exceptionMessage() != null
                            && headers.exceptionMessage().matches(ValueFreeDlqHeadersUnitTest.QUERY_ID_MESSAGE),
                    "the message header must be Processing's QueryIdentifier text: " + headers.exceptionMessage());
            assertNoneOf("the message header", headers.exceptionMessage(), ALL_SENTINELS);
            assertNoneOf("the stack-trace header", headers.exceptionStacktrace(), ALL_SENTINELS);
            assertTrue(
                    headers.exceptionStacktrace().contains(JACKSON_WITHHELD_LINE),
                    "the stack-trace header lacks the withheld Jackson line:\n" + headers.exceptionStacktrace());
        }
    }

    @Test
    @DisplayName("a List element that Jackson cannot serialize is named by its index, so no record text reaches any destination")
    void listElementFailureCarriesNoRecordTextUnderToleranceNone(TestInfo testInfo) throws Exception {
        try (LogCapture log = LogCapture.attach(ClickHouseWriter.class)) {
            Throwable thrown =
                    putBatch(testInfo, true, false, null, JsonAsStringKeyResidualLiveTest::listBatch);

            assertNotNull(thrown, "put must fail when errors.tolerance=none");
            assertJacksonRouteReached(thrown);
            ThrowableTexts.allTexts(thrown).forEach(text -> assertNoneOf("the thrown chain", text, ALL_SENTINELS));
            assertFalse(insertErrors(log).isEmpty(), "no '" + INSERT_FAILURE_MESSAGE + "' ERROR event with a throwable");
            log.allTexts().forEach(text -> assertNoneOf("a log text", text, ALL_SENTINELS));
        }
    }

    @Test
    @DisplayName("without the setting the write fails before Jackson with the unsupported-type text and no record text")
    void settingAbsentFailsBeforeJacksonWithNoRecordText(TestInfo testInfo) throws Exception {
        try (LogCapture log = LogCapture.attach(ClickHouseWriter.class)) {
            Throwable thrown =
                    putBatch(testInfo, false, false, null, JsonAsStringKeyResidualLiveTest::mapBatch);

            assertNotNull(thrown, "put must fail when the JSON column has no binary route");
            String rendered = ThrowableTexts.printedStackTrace(thrown);
            assertTrue(rendered.contains(UNSUPPORTED_JSON_TEXT), "the rendering lacks the unsupported-type text:\n" + rendered);
            assertFalse(
                    rendered.contains(JACKSON_FAILURE_CLASS),
                    "the control must not reach Jackson, or it proves nothing about the setting:\n" + rendered);
            ThrowableTexts.allTexts(thrown).forEach(text -> assertNoneOf("the thrown chain", text, ALL_SENTINELS));
            log.allTexts().forEach(text -> assertNoneOf("a log text", text, ALL_SENTINELS));
        }
    }
}
