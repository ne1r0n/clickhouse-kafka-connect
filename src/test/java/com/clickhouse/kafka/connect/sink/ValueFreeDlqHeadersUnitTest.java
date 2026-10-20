package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.db.CountingHelperClient;
import com.clickhouse.kafka.connect.sink.db.ScriptedWriteFailureWriter;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * WRT-13, D-15: the DLQ exception headers for a value-conversion failure under {@code
 * errors.tolerance=all}. {@code ProxySinkTask.put} hands the DLQ the {@code RuntimeException}
 * that {@code Processing.doInsert} builds from the {@code QueryIdentifier} text, so the message
 * header is unchanged by WRT-13 and carries no column text. The column, the ClickHouse type and the
 * original exception class are in the stack-trace header, and the record value is in neither. The
 * headers are rendered as Kafka Connect 4.1.1 renders them ({@link InMemoryDLQ#connectHeaders}).
 * It runs the real {@code ProxySinkTask}, {@code Processing} and writer code without Docker. The
 * live twin is {@code WriterErrorPathLiveTest}, which shares the assertions below.
 */
public class ValueFreeDlqHeadersUnitTest {

    /** Processing's {@code QueryIdentifier.toString()} text, the DLQ message header (D-15). */
    static final String QUERY_ID_MESSAGE =
            "Topic: \\[[^\\]]+\\], Partition: \\[\\d+\\], MinOffset: \\[\\d+\\], MaxOffset: \\[\\d+\\], \\(QueryId: \\[[^\\]]+\\]\\)";

    private static final String COLUMN_PREFIX = "Failed to write column ";
    private static final String WITHHELD_CAUSE_NOTE = " (message withheld: may contain record data)";
    private static final String RUNTIME_EXCEPTION_CLASS = RuntimeException.class.getName();
    private static final String CONNECTOR_NAME = "wrt13-dlq-headers";
    private static final String TOPIC = "wrt13_dlq_topic";
    private static final String UUID_COLUMN = "c_uuid";
    private static final String SENTINEL = "SENTINELq7Zx41kV";
    private static final int BATCH_SIZE = 3;

    /** The column, type and class text the rendered failure must carry (stack-trace header, task status). */
    static List<String> renderedFragments(String column, String typeToken, String originalClass) {
        return List.of(
                COLUMN_PREFIX + "`" + column + "`: ",
                "could not be written as ClickHouse type " + typeToken + ": " + originalClass,
                "Caused by: " + originalClass + WITHHELD_CAUSE_NOTE);
    }

    /**
     * Asserts the three D-15 facts for every record in {@code dlq}: the class-name header is
     * {@code java.lang.RuntimeException}, the message header fully matches {@link #QUERY_ID_MESSAGE}
     * with neither the value nor column text, and the stack-trace header names the column, the type
     * and the class and not the value.
     */
    static void assertValueFreeDlqHeaders(
            InMemoryDLQ dlq, int expectedRecords, List<String> stacktraceFragments, String value) {
        assertEquals(expectedRecords, dlq.size(), "every record of the batch must reach the DLQ");
        assertEquals(expectedRecords, dlq.exceptions().size());
        for (Throwable reported : dlq.exceptions()) {
            InMemoryDLQ.ConnectDlqHeaders headers = InMemoryDLQ.connectHeaders(reported);

            assertEquals(RUNTIME_EXCEPTION_CLASS, headers.exceptionClassName());
            assertTrue(
                    headers.exceptionMessage() != null && headers.exceptionMessage().matches(QUERY_ID_MESSAGE),
                    "the message header must be Processing's QueryIdentifier text: " + headers.exceptionMessage());
            assertFalse(headers.exceptionMessage().contains(value), "the message header carries the value");
            assertFalse(headers.exceptionMessage().contains(COLUMN_PREFIX), "the message header carries column text");

            stacktraceFragments.forEach(fragment -> assertTrue(
                    headers.exceptionStacktrace().contains(fragment),
                    "the stack-trace header lacks '" + fragment + "':\n" + headers.exceptionStacktrace()));
            assertFalse(headers.exceptionStacktrace().contains(value), "the stack-trace header carries the value");
        }
    }

    private static Map<String, String> props() {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConfig.HOSTNAME, "localhost");
        props.put(ClickHouseSinkConfig.PORT, "8123");
        props.put(ClickHouseSinkConfig.DATABASE, "default");
        props.put(ClickHouseSinkConfig.USERNAME, "default");
        props.put(ClickHouseSinkConfig.PASSWORD, "");
        props.put(ClickHouseSinkConfig.ERRORS_TOLERANCE, "all");
        return props;
    }

    private static List<SinkRecord> batch() {
        Schema schema = SchemaBuilder.struct()
                .field("id", Schema.INT32_SCHEMA)
                .field("name", Schema.STRING_SCHEMA)
                .build();
        List<SinkRecord> records = new ArrayList<>();
        for (int offset = 0; offset < BATCH_SIZE; offset++) {
            Struct struct = new Struct(schema).put("id", offset).put("name", "n" + offset);
            records.add(new SinkRecord(TOPIC, 0, null, null, schema, struct, offset));
        }
        return records;
    }

    @ParameterizedTest(name = "WRT-13 DLQ headers: tolerance all, clientV2={0}")
    @ValueSource(booleans = {false, true})
    void dlqHeadersForAConversionFailureAreValueFree(boolean useClientV2) throws Exception {
        ClickHouseSinkConfig csc = new ClickHouseSinkConfig(props());
        InMemoryDLQ dlq = new InMemoryDLQ();
        ProxySinkTask task = new ProxySinkTask(csc, dlq, CONNECTOR_NAME);
        try (CountingHelperClient chc = new CountingHelperClient(useClientV2)) {
            Column column = Column.builder().name(UUID_COLUMN).type(Type.UUID).build();
            Data value = new Data(Schema.STRING_SCHEMA, SENTINEL);
            new ScriptedWriteFailureWriter(csc, chc, column, value, ByteArrayOutputStream::new).installInto(task);

            task.put(batch());

            assertValueFreeDlqHeaders(
                    dlq,
                    BATCH_SIZE,
                    renderedFragments(UUID_COLUMN, String.valueOf(Type.UUID), IllegalArgumentException.class.getName()),
                    SENTINEL);
        } finally {
            task.stop();
        }
    }
}
