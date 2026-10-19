package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.ClickHouseException;
import com.clickhouse.data.format.BinaryStreamUtils;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.clickhouse.kafka.connect.sink.util.ThrowableTexts;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The Docker-free layer of the WRT-13 proof (D-12). A unique sentinel is used as the record value.
 * Every case asserts both directions (Phase 18 D-12): the value is absent from the thrown chain,
 * from its rendered stack trace and from every captured log event, while the column name, the
 * ClickHouse {@code Type} token and the original exception class name are present, so a deleted
 * message cannot pass. Each value-free case also checks that the raw library or JDK message for
 * the same input embeds the value, so the absence check is not vacuous.
 *
 * <p>The class extends nothing, so it stays in {@code testUnit}. It asserts on rendered text only
 * and names no production symbol that WRT-13 added, so it compiles against the unchanged writer
 * for the red-before runs. The {@code @DisplayName} prefix of each case is a contract: {@code
 * WRT-13 value-free}, {@code D-04 V1} and {@code D-14} cases assert new behaviour and fail on the
 * unchanged writer, while {@code WRT-13 pass-through} and {@code D-04 V2 unchanged} cases assert
 * unchanged behaviour and pass on it.
 */
public class WriterValueFreeFailureUnitTest {

    private static final String SENTINEL = "SENTINELq7Zx41kV";
    // At most 9 characters: BigInteger's NumberFormatException truncates a longer value, which would make
    // the absence check pass on the unchanged writer (22-RESEARCH F4).
    private static final String NUMERIC_SENTINEL = "Zq7x41kV";
    private static final String DECIMAL_VALUE = "123456789012345.12";
    private static final String DECIMAL_DIGITS = "123456789012345";
    // A UUID longer than 36 characters fails with a text that has no value in it, so the record text
    // of the D-04 cases stays within 36 characters: "ClickHouseException: Code: 131 Zq7x" is 35.
    private static final String REFRESH_TRIGGER_FORMAT = "ClickHouseException: Code: %s Zq7x";
    private static final String REFRESH_TRIGGER_TAIL = "Zq7x";
    private static final String REFRESH_TRIGGER_INFIX = "ClickHouseException: Code: ";
    private static final String JSON_BINARY_HINT =
            "Writing JSON in binary is not supported yet. Use `input_format_binary_read_json_as_string=1` in clickhouse settings to allow writing as string";
    private static final String JSON_AS_STRING_SETTING = "input_format_binary_read_json_as_string=1";
    private static final String UNSUPPORTED_JSON_FIELD_TYPE = "Unsupported field type: INT32 for column type [JSON]";
    private static final String NESTED_NULL_PREFIX = "column 't' (";
    private static final String NO_ENUM_CONTENT_MESSAGE = "No content in enum";
    private static final String INT128_COLUMN = "c_int128";
    private static final String TUPLE_COLUMN = "t";
    private static final String TUPLE_FIELD = "a";
    private static final String DECIMAL_COLUMN = "c_decimal";
    private static final String JSON_COLUMN = "c_json";
    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String NULL_TOKEN = "null";
    private static final String NESTED_NULL_RUNTIME_FORMAT = "An attempt to write null into not nullable column '%s'";
    private static final String GRIN_EMOJI = "😀";
    private static final String VALUE_FREE_MESSAGE_FORMAT =
            "Failed to write column `%s`: Kafka value of type %s (value class: %s) could not be written as ClickHouse type %s: %s";
    private static final String WITHHELD_CAUSE_LINE_FORMAT = "Caused by: %s (message withheld: may contain record data)";
    private static final String WRITE_FAILURE_LINE_FORMAT =
            "Error writing value of %s to the column `%s` of type %s: exception %s";
    private static final String INSERT_FAILURE_MESSAGE = "Error inserting records";
    private static final String SERVER_EXCEPTION_CLASS = "com.clickhouse.client.api.ServerException";
    private static final String UUID_COLUMN = "c_uuid";
    private static final String STRING_KAFKA_TYPE = "STRING";
    private static final String STRING_VALUE_CLASS = String.class.getName();
    private static final String ERROR_LEVEL = "ERROR";
    private static final String TOPIC = "wrt13_topic";
    private static final String QUERY_ID = "wrt13-query";

    /** What one call left behind: the thrown exception and the log events written meanwhile. */
    private static final class Captured {
        private final Throwable thrown;
        private final List<LogCapture.CapturedEvent> events;
        private final List<String> logTexts;

        private Captured(Throwable thrown, List<LogCapture.CapturedEvent> events, List<String> logTexts) {
            this.thrown = thrown;
            this.events = events;
            this.logTexts = logTexts;
        }

        private long errorCount(String message) {
            return events.stream()
                    .filter(e -> ERROR_LEVEL.equals(e.level()) && message.equals(e.message()))
                    .count();
        }

        private List<LogCapture.CapturedEvent> errorEvents(String message) {
            return events.stream()
                    .filter(e -> ERROR_LEVEL.equals(e.level()) && message.equals(e.message()))
                    .collect(Collectors.toList());
        }
    }

    private static Captured capture(Executable action) {
        try (LogCapture log = LogCapture.attach(ClickHouseWriter.class)) {
            Throwable thrown = null;
            try {
                action.execute();
            } catch (Throwable t) {
                thrown = t;
            }
            return new Captured(thrown, log.events(), log.allTexts());
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

    private static ClickHouseSinkConfig config() {
        return new ClickHouseSinkConfig(minimalProps());
    }

    private static ClickHouseWriter newWriter(ClickHouseSinkConfig csc) throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        Field field = ClickHouseWriter.class.getDeclaredField("csc");
        field.setAccessible(true);
        field.set(writer, csc);
        return writer;
    }

    private static Column column(String name, Type type) {
        return Column.builder().name(name).type(type).build();
    }

    private static Data stringData(String value) {
        return new Data(Schema.STRING_SCHEMA, value);
    }

    private static Captured writeColValue(Column column, Data value) throws Exception {
        return writeColValue(config(), column, value);
    }

    private static Captured writeColValue(ClickHouseSinkConfig csc, Column column, Data value) throws Exception {
        ClickHouseWriter writer = newWriter(csc);
        return capture(() -> writer.doWriteColValue(column, new ByteArrayOutputStream(), value, false));
    }

    /** The expected message, built here and independently of production. */
    private static String valueFreeMessage(
            String columnName, String kafkaType, String valueClass, Type type, Class<?> exceptionClass) {
        return String.format(
                VALUE_FREE_MESSAGE_FORMAT, columnName, kafkaType, valueClass, String.valueOf(type), exceptionClass.getName());
    }

    private static void assertNoneContains(List<String> texts, String forbidden, String what) {
        for (String text : texts) {
            assertFalse(String.valueOf(text).contains(forbidden), what + " must carry no record value");
        }
    }

    private static void assertNoSentinelAnywhere(Throwable thrown, Captured captured, String forbidden) {
        assertNoneContains(ThrowableTexts.allTexts(thrown), forbidden, "a thrown text");
        assertNoneContains(captured.logTexts, forbidden, "a captured log text");
    }

    private static void assertValueFreeWriterFailure(Captured captured, String forbidden, String expectedMessage) {
        DataException thrown = assertInstanceOf(DataException.class, captured.thrown);
        assertEquals(expectedMessage, thrown.getMessage());
        assertNoSentinelAnywhere(thrown, captured, forbidden);
    }

    private static void assertRawUuidMessageEmbeds(String value) {
        IllegalArgumentException raw = assertThrows(IllegalArgumentException.class, () -> UUID.fromString(value));
        assertTrue(raw.getMessage().contains(value), "self-check: the JDK UUID message embeds the value");
    }

    /** One {@code doInsertRawBinary} call: what it threw and logged, and how often the seams were hit. */
    private static final class InsertRun {
        private final Captured captured;
        private final int hookCalls;
        private final int pinCalls;
        private final int updateMappingCalls;

        private InsertRun(Captured captured, int hookCalls, int pinCalls, int updateMappingCalls) {
            this.captured = captured;
            this.hookCalls = hookCalls;
            this.pinCalls = pinCalls;
            this.updateMappingCalls = updateMappingCalls;
        }
    }

    private static InsertRun insertRawBinary(Column column, Data value, boolean useClientV2) throws Exception {
        try (CountingHelperClient chc = new CountingHelperClient(useClientV2)) {
            ScriptedWriteFailureWriter writer = new ScriptedWriteFailureWriter(
                    config(), chc, column, value, ByteArrayOutputStream::new);
            QueryIdentifier queryId = new QueryIdentifier(TOPIC, 0, 0, 0, QUERY_ID);
            Captured captured =
                    capture(() -> writer.doInsertRawBinary(List.of(), writer.stubTable(), queryId, false, true));
            return new InsertRun(captured, writer.hookCalls(), chc.pinCalls(), writer.updateMappingCalls());
        }
    }

    /**
     * The shared D-12/D-13 assertions for a conversion failure that travels through {@code
     * doInsertRawBinary}: the value-free DataException is in the chain, nothing leaks, the generic
     * {@code Error inserting records} log names the column, type and class, and the ServerException
     * branch is not reached.
     */
    private static void assertValueFreeInsert(
            Column column,
            Data value,
            String forbidden,
            boolean useClientV2,
            String expectedMessage,
            Class<?> exceptionClass)
            throws Exception {
        InsertRun run = insertRawBinary(column, value, useClientV2);
        Captured captured = run.captured;

        assertNotNull(captured.thrown);
        if (useClientV2) {
            assertInstanceOf(ClickHouseException.class, captured.thrown);
        }
        DataException dataException = ThrowableTexts.causeChain(captured.thrown).stream()
                .filter(DataException.class::isInstance)
                .map(DataException.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no DataException in " + captured.thrown));
        assertEquals(expectedMessage, dataException.getMessage());
        assertNoSentinelAnywhere(captured.thrown, captured, forbidden);

        List<LogCapture.CapturedEvent> insertFailures = captured.errorEvents(INSERT_FAILURE_MESSAGE);
        assertEquals(1, insertFailures.size(), captured.logTexts.toString());
        String insertFailureTexts = String.join("\n", insertFailures.get(0).throwableTexts());
        assertTrue(insertFailureTexts.contains(column.getName()), insertFailureTexts);
        assertTrue(insertFailureTexts.contains(String.valueOf(column.getType())), insertFailureTexts);
        assertTrue(insertFailureTexts.contains(exceptionClass.getName()), insertFailureTexts);
        for (LogCapture.CapturedEvent event : captured.events) {
            for (String line : event.throwableTexts()) {
                assertFalse(line.startsWith(SERVER_EXCEPTION_CLASS), "the ServerException branch must stay unreachable: " + line);
            }
        }
        assertEquals(1, run.hookCalls, "the insert hook runs once");
        assertEquals(0, run.pinCalls, "no mapping refresh may be started");
    }

    @Test
    @DisplayName("WRT-13 value-free: an invalid UUID string through doWriteColValue throws a DataException naming the column, the type and the exception class, with no value in the chain, the rendering or the logs")
    void invalidUuidThroughDoWriteColValueIsValueFree() throws Exception {
        Captured captured = writeColValue(column(UUID_COLUMN, Type.UUID), stringData(SENTINEL));

        String expected =
                valueFreeMessage(UUID_COLUMN, STRING_KAFKA_TYPE, STRING_VALUE_CLASS, Type.UUID, IllegalArgumentException.class);
        assertValueFreeWriterFailure(captured, SENTINEL, expected);
        String printed = ThrowableTexts.printedStackTrace(captured.thrown);
        assertTrue(
                printed.contains(String.format(WITHHELD_CAUSE_LINE_FORMAT, IllegalArgumentException.class.getName())),
                printed);
        assertTrue(printed.contains("java.util.UUID.fromString"), "the original frames are kept: " + printed);
        String generic = String.format(WRITE_FAILURE_LINE_FORMAT, STRING_KAFKA_TYPE, UUID_COLUMN, Type.UUID, IllegalArgumentException.class.getName());
        assertEquals(1, captured.errorCount(generic), captured.logTexts.toString());
        assertRawUuidMessageEmbeds(SENTINEL);
    }

    @ParameterizedTest(name = "WRT-13 value-free: an invalid UUID through doInsertRawBinary, clientV2={0}")
    @ValueSource(booleans = {false, true})
    void invalidUuidThroughDoInsertRawBinaryIsValueFree(boolean useClientV2) throws Exception {
        String expected =
                valueFreeMessage(UUID_COLUMN, STRING_KAFKA_TYPE, STRING_VALUE_CLASS, Type.UUID, IllegalArgumentException.class);

        assertValueFreeInsert(
                column(UUID_COLUMN, Type.UUID), stringData(SENTINEL), SENTINEL, useClientV2, expected, IllegalArgumentException.class);
        assertRawUuidMessageEmbeds(SENTINEL);
    }

    @Test
    @DisplayName("WRT-13 value-free: an invalid UUID ending in U+1F600 leaves neither the value nor the emoji in any text")
    void invalidUuidWithNonAsciiSuffixIsValueFree() throws Exception {
        String value = SENTINEL + GRIN_EMOJI;
        Captured captured = writeColValue(column(UUID_COLUMN, Type.UUID), stringData(value));

        String expected =
                valueFreeMessage(UUID_COLUMN, STRING_KAFKA_TYPE, STRING_VALUE_CLASS, Type.UUID, IllegalArgumentException.class);
        assertValueFreeWriterFailure(captured, SENTINEL, expected);
        assertNoSentinelAnywhere(captured.thrown, captured, GRIN_EMOJI);
        assertRawUuidMessageEmbeds(value);
    }

    private static Column tupleColumn(Column field) {
        return Column.builder().name(TUPLE_COLUMN).type(Type.TUPLE).tupleFields(List.of(field)).build();
    }

    private static Data tupleData(Schema fieldSchema, Object fieldValue) {
        Schema schema = SchemaBuilder.struct().field(TUPLE_FIELD, fieldSchema).build();
        return new Data(schema, new Struct(schema).put(TUPLE_FIELD, fieldValue));
    }

    private static void assertRawBigIntegerMessageEmbeds(String value) {
        NumberFormatException raw = assertThrows(NumberFormatException.class, () -> new BigInteger(value));
        assertTrue(raw.getMessage().contains(value), "self-check: the JDK BigInteger message embeds the whole value");
    }

    @Test
    @DisplayName("WRT-13 value-free: an unparseable INT128 string through doWriteColValue throws a DataException naming INT128 and NumberFormatException, with no value")
    void unparseableInt128IsValueFree() throws Exception {
        Captured captured = writeColValue(column(INT128_COLUMN, Type.INT128), stringData(NUMERIC_SENTINEL));

        String expected = valueFreeMessage(
                INT128_COLUMN, STRING_KAFKA_TYPE, STRING_VALUE_CLASS, Type.INT128, NumberFormatException.class);
        assertValueFreeWriterFailure(captured, NUMERIC_SENTINEL, expected);
        assertRawBigIntegerMessageEmbeds(NUMERIC_SENTINEL);
    }

    @ParameterizedTest(name = "WRT-13 value-free: an unparseable INT128 through doInsertRawBinary, clientV2={0}")
    @ValueSource(booleans = {false, true})
    void unparseableInt128ThroughDoInsertRawBinaryIsValueFree(boolean useClientV2) throws Exception {
        String expected = valueFreeMessage(
                INT128_COLUMN, STRING_KAFKA_TYPE, STRING_VALUE_CLASS, Type.INT128, NumberFormatException.class);

        assertValueFreeInsert(
                column(INT128_COLUMN, Type.INT128),
                stringData(NUMERIC_SENTINEL),
                NUMERIC_SENTINEL,
                useClientV2,
                expected,
                NumberFormatException.class);
        assertRawBigIntegerMessageEmbeds(NUMERIC_SENTINEL);
    }

    @Test
    @DisplayName("WRT-13 value-free: a Decimal(9,2) value out of range throws a DataException naming Decimal and IllegalArgumentException, with no digits of the value and no value length")
    void decimalOutOfRangeIsValueFree() throws Exception {
        Column decimal =
                Column.builder().name(DECIMAL_COLUMN).type(Type.Decimal).precision(9).scale(2).build();
        Captured captured = writeColValue(decimal, stringData(DECIMAL_VALUE));

        String expected = valueFreeMessage(
                DECIMAL_COLUMN, STRING_KAFKA_TYPE, STRING_VALUE_CLASS, Type.Decimal, IllegalArgumentException.class);
        assertValueFreeWriterFailure(captured, DECIMAL_DIGITS, expected);
        assertFalse(
                captured.thrown.getMessage().chars().anyMatch(Character::isDigit),
                "D-08: the message carries no value length: " + captured.thrown.getMessage());
        IllegalArgumentException raw = assertThrows(
                IllegalArgumentException.class,
                () -> BinaryStreamUtils.writeDecimal(new ByteArrayOutputStream(), new BigDecimal(DECIMAL_VALUE), 9, 2));
        assertTrue(raw.getMessage().contains(DECIMAL_DIGITS), "self-check: the library message embeds the digits");
    }

    @Test
    @DisplayName("WRT-13 value-free: a failure inside a Tuple child names the innermost column and one generic-catch line is logged per level")
    void nestedTupleChildNamesTheInnermostColumn() throws Exception {
        Captured captured = writeColValue(tupleColumn(column(TUPLE_FIELD, Type.UUID)), tupleData(Schema.STRING_SCHEMA, SENTINEL));

        String expected = valueFreeMessage(
                TUPLE_FIELD, STRING_KAFKA_TYPE, STRING_VALUE_CLASS, Type.UUID, IllegalArgumentException.class);
        assertValueFreeWriterFailure(captured, SENTINEL, expected);
        String innerLine = String.format(
                WRITE_FAILURE_LINE_FORMAT, STRING_KAFKA_TYPE, TUPLE_FIELD, Type.UUID, IllegalArgumentException.class.getName());
        String outerLine = String.format(
                WRITE_FAILURE_LINE_FORMAT, Schema.Type.STRUCT, TUPLE_COLUMN, Type.TUPLE, DataException.class.getName());
        assertEquals(1, captured.errorCount(innerLine), captured.logTexts.toString());
        assertEquals(1, captured.errorCount(outerLine), captured.logTexts.toString());
        assertRawUuidMessageEmbeds(SENTINEL);
    }

    @Test
    @DisplayName("WRT-13 value-free: a null Data on a UUID column uses the null tokens and names NullPointerException")
    void nullDataOnUuidColumnUsesNullTokens() throws Exception {
        Captured captured = writeColValue(column(UUID_COLUMN, Type.UUID), null);

        String expected =
                valueFreeMessage(UUID_COLUMN, NULL_TOKEN, NULL_TOKEN, Type.UUID, NullPointerException.class);
        DataException thrown = assertInstanceOf(DataException.class, captured.thrown);
        assertEquals(expected, thrown.getMessage());
    }

    @Test
    @DisplayName("WRT-13 pass-through: the upstream JSON hint arrives as the same exact RuntimeException text")
    void jsonHintRuntimeExceptionPassesThrough() throws Exception {
        Captured captured = writeColValue(column(JSON_COLUMN, Type.JSON), stringData("{}"));

        assertEquals(RuntimeException.class, captured.thrown.getClass());
        assertEquals(JSON_BINARY_HINT, captured.thrown.getMessage());
    }

    @Test
    @DisplayName("WRT-13 pass-through: an unsupported field type for a JSON column arrives as the same exact RuntimeException text")
    void unsupportedJsonFieldTypePassesThrough() throws Exception {
        Map<String, String> props = minimalProps();
        props.put(ClickHouseSinkConfig.CLICKHOUSE_SETTINGS, JSON_AS_STRING_SETTING);
        Captured captured = writeColValue(
                new ClickHouseSinkConfig(props), column(JSON_COLUMN, Type.JSON), new Data(Schema.INT32_SCHEMA, 1));

        assertEquals(RuntimeException.class, captured.thrown.getClass());
        assertEquals(UNSUPPORTED_JSON_FIELD_TYPE, captured.thrown.getMessage());
    }

    @Test
    @DisplayName("WRT-13 pass-through: a scripted nested L-12 RuntimeException leaves the Tuple catch as the same instance")
    void scriptedL12RuntimeExceptionPassesThroughTheTupleCatch() throws Exception {
        Column tuple = tupleColumn(column(TUPLE_FIELD, Type.INT32));
        Data value = tupleData(Schema.INT32_SCHEMA, 1);
        RuntimeException l12 = new RuntimeException(String.format(NESTED_NULL_RUNTIME_FORMAT, TUPLE_FIELD));
        ScriptedWriteFailureWriter writer = new ScriptedWriteFailureWriter(
                        config(), null, tuple, value, ByteArrayOutputStream::new)
                .failNestedColumnWith(l12);

        Captured captured = capture(() -> writer.doWriteColValue(tuple, new ByteArrayOutputStream(), value, false));

        assertSame(l12, captured.thrown);
    }

    @Test
    @DisplayName("WRT-13 pass-through: an IOException from the stream is rethrown as the same instance")
    void streamIoExceptionPassesThrough() throws Exception {
        IOException failure = new IOException("stream closed by test");
        ClickHouseWriter writer = newWriter(config());
        Column uuid = column(UUID_COLUMN, Type.UUID);

        Captured captured = capture(() -> writer.doWriteColValue(
                uuid, new ScriptedWriteFailureWriter.FailingOutputStream(failure, 0), stringData(VALID_UUID), false));

        assertSame(failure, captured.thrown);
    }

    @Test
    @DisplayName("WRT-13 pass-through: an IOException inside a Tuple field keeps the exact RuntimeException wrapper and its cause")
    void ioInsideTupleKeepsTheLambdaWrapper() throws Exception {
        IOException failure = new IOException("stream closed by test");
        ClickHouseWriter writer = newWriter(config());
        Column tuple = tupleColumn(column(TUPLE_FIELD, Type.INT32));

        Captured captured = capture(() -> writer.doWriteColValue(
                tuple,
                new ScriptedWriteFailureWriter.FailingOutputStream(failure, 0),
                tupleData(Schema.INT32_SCHEMA, 1),
                false));

        assertEquals(RuntimeException.class, captured.thrown.getClass());
        assertSame(failure, captured.thrown.getCause());
    }

    @Test
    @DisplayName("WRT-13 pass-through: a null in a non-nullable Tuple field arrives as the WRT-08 DataException")
    void nestedNullDataExceptionPassesThrough() throws Exception {
        Captured captured =
                writeColValue(tupleColumn(column(TUPLE_FIELD, Type.INT32)), tupleData(Schema.OPTIONAL_INT32_SCHEMA, null));

        DataException thrown = assertInstanceOf(DataException.class, captured.thrown);
        assertTrue(thrown.getMessage().startsWith(NESTED_NULL_PREFIX), thrown.getMessage());
    }

    @ParameterizedTest(name = "D-04 V1: a record value containing ClickHouseException: Code: {0} no longer triggers the mapping refresh")
    @ValueSource(strings = {"33", "131"})
    void recordTextWithKnownCodeNoLongerTriggersRefresh(String code) throws Exception {
        String value = String.format(REFRESH_TRIGGER_FORMAT, code);
        IllegalArgumentException raw = assertThrows(IllegalArgumentException.class, () -> UUID.fromString(value));
        assertTrue(
                raw.getMessage().contains(REFRESH_TRIGGER_INFIX + code),
                "self-check: the raw JDK message carries the refresh trigger text");

        InsertRun run = insertRawBinary(column(UUID_COLUMN, Type.UUID), stringData(value), false);

        assertEquals(1, run.hookCalls, "the insert is not retried");
        assertEquals(0, run.pinCalls);
        assertEquals(0, run.updateMappingCalls);
        assertInstanceOf(DataException.class, run.captured.thrown);
        assertNoSentinelAnywhere(run.captured.thrown, run.captured, REFRESH_TRIGGER_TAIL);
        assertNoSentinelAnywhere(run.captured.thrown, run.captured, REFRESH_TRIGGER_INFIX);
    }

    @ParameterizedTest(name = "D-04 V2 unchanged: a record value containing ClickHouseException: Code: {0} is never refreshed on client V2")
    @ValueSource(strings = {"33", "131"})
    void recordTextWithKnownCodeNeverRefreshedOnV2(String code) throws Exception {
        InsertRun run = insertRawBinary(
                column(UUID_COLUMN, Type.UUID), stringData(String.format(REFRESH_TRIGGER_FORMAT, code)), true);

        assertEquals(1, run.hookCalls);
        assertEquals(0, run.pinCalls);
        assertEquals(0, run.updateMappingCalls);
        assertInstanceOf(ClickHouseException.class, run.captured.thrown);
    }

    @Test
    @DisplayName("D-14: an Enum8 column without enum values throws the fixed text with no label in any text")
    void enumWithoutValuesThrowsFixedTextWithoutTheLabel() throws Exception {
        Column enumColumn = Column.builder().name("c_enum").type(Type.Enum8).build();
        Captured captured = writeColValue(enumColumn, stringData(NUMERIC_SENTINEL));

        assertEquals(RuntimeException.class, captured.thrown.getClass());
        assertEquals(NO_ENUM_CONTENT_MESSAGE, captured.thrown.getMessage());
        assertNoSentinelAnywhere(captured.thrown, captured, NUMERIC_SENTINEL);
    }
}
