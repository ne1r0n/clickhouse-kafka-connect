package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.clickhouse.kafka.connect.sink.util.ThrowableTexts;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.DataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Docker-free log-capture pins for WRT-12 (13-SECURITY T-13-19, Phase 18 D-08, D-09, D-12). A
 * unique sentinel is used as the record value, and every captured event (rendered message,
 * throwable string representation and every message down the cause chain) must be free of it,
 * while the column name must be present, so that a deleted log line cannot pass the test. Each
 * masking test also checks that the raw JDK exception for the same input embeds the sentinel, so
 * the absence check is not vacuous. The class extends nothing, so that it stays in {@code
 * testUnit}.
 */
public class WriterLogMaskingUnitTest {

    private static final String SENTINEL = "SENTINELq7Zx41kV";
    private static final String COLUMN = "event_time";
    private static final String UUID_COLUMN = "order_uuid";
    private static final String ERROR_LEVEL = "ERROR";
    private static final String PARSE_EXCEPTION_CLASS = DateTimeParseException.class.getName();
    private static final String PARSE_FAILURE_LINE_FORMAT =
            "Error parsing date time string for column '%s' (ClickHouse type %s, Kafka type STRING): exception %s, value length %d";
    private static final String CONVERSION_MESSAGE_FORMAT =
            "(Potentially) Not implemented conversion from STRING to %s for column '%s'";
    private static final String WRITE_FAILURE_LINE_FORMAT =
            "Error writing value of STRING to the column `%s` of type %s: exception %s";
    private static final String GRIN_EMOJI = "😀";

    private static final class Captured {
        private final Throwable thrown;
        private final List<LogCapture.CapturedEvent> events;
        private final List<String> allTexts;

        private Captured(Throwable thrown, List<LogCapture.CapturedEvent> events, List<String> allTexts) {
            this.thrown = thrown;
            this.events = events;
            this.allTexts = allTexts;
        }

        private List<String> errorMessages() {
            return events.stream()
                    .filter(e -> ERROR_LEVEL.equals(e.level()))
                    .map(LogCapture.CapturedEvent::message)
                    .collect(Collectors.toList());
        }

        private long errorCount(String message) {
            return errorMessages().stream().filter(message::equals).count();
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

    /** The DateTime64 branch reads {@code csc}, which upstream's writer keeps private with no setter. */
    private static ClickHouseWriter newWriter() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        Field csc = ClickHouseWriter.class.getDeclaredField("csc");
        csc.setAccessible(true);
        csc.set(writer, new ClickHouseSinkConfig(minimalProps()));
        return writer;
    }

    private static Data stringData(String value) {
        return new Data(Schema.STRING_SCHEMA, value);
    }

    private static Captured writeDate(Type type, int precision, String value) throws Exception {
        ClickHouseWriter writer = newWriter();
        return capture(() -> writer.doWriteDates(type, new ByteArrayOutputStream(), stringData(value), precision, COLUMN));
    }

    private static String parseFailureLine(Type type, int valueLength) {
        return String.format(PARSE_FAILURE_LINE_FORMAT, COLUMN, type, PARSE_EXCEPTION_CLASS, valueLength);
    }

    private static void assertNoSentinel(Captured captured) {
        for (String text : captured.allTexts) {
            assertFalse(String.valueOf(text).contains(SENTINEL), "a captured log text must carry no record value");
        }
    }

    private static void assertRawJdkExceptionEmbedsSentinel() {
        DateTimeParseException raw = assertThrows(DateTimeParseException.class, () -> ZonedDateTime.parse(SENTINEL));
        assertTrue(raw.getMessage().contains(SENTINEL), "self-check: the JDK parse message embeds the value");
    }

    @Test
    @DisplayName("WRT-12: LogCapture records message, throwable texts down the cause chain and the throwable flag, and records nothing after close")
    void logCaptureRecordsEventsAndDetachesOnClose() {
        Logger logger = LoggerFactory.getLogger(ClickHouseWriter.class);
        Throwable failure = new IllegalStateException("outer", new IllegalArgumentException("inner-cause"));
        LogCapture log = LogCapture.attach(ClickHouseWriter.class);
        try {
            logger.error("marker {}", "m1", failure);
        } finally {
            log.close();
        }
        logger.error("after close {}", "m2");

        assertEquals(1, log.events().size(), "nothing may be captured after close");
        LogCapture.CapturedEvent event = log.events().get(0);
        assertEquals(ERROR_LEVEL, event.level());
        assertEquals("marker m1", event.message());
        assertTrue(event.hasThrowable());
        assertTrue(event.throwableTexts().contains("outer"));
        assertTrue(event.throwableTexts().contains("inner-cause"));
        assertEquals(1, log.events(ERROR_LEVEL).size());
        assertTrue(log.allTexts().contains("inner-cause"));
    }

    @Test
    @DisplayName("WRT-12: an unparseable DateTime string logs the column, types, exception class and value length, never the value")
    void dateTimeParseFailureLogsMetadataOnly() throws Exception {
        Captured captured = writeDate(Type.DateTime, 0, SENTINEL);

        assertEquals(1, captured.errorCount(parseFailureLine(Type.DateTime, SENTINEL.length())), captured.errorMessages().toString());
        assertEquals(16, SENTINEL.length());
        assertNoSentinel(captured);
        assertRawJdkExceptionEmbedsSentinel();
    }

    @Test
    @DisplayName("WRT-12: an unparseable DateTime64 string logs the same masked shape, and the class named is DateTimeParseException, not a NullPointerException")
    void dateTime64ParseFailureLogsMetadataOnly() throws Exception {
        Captured captured = writeDate(Type.DateTime64, 3, SENTINEL);

        assertEquals(1, captured.errorCount(parseFailureLine(Type.DateTime64, SENTINEL.length())), captured.errorMessages().toString());
        assertNoSentinel(captured);
        assertRawJdkExceptionEmbedsSentinel();
    }

    @Test
    @DisplayName("WRT-12: the unsupported-conversion DataException keeps its prefix, names the column and carries no value")
    void conversionDataExceptionNamesColumnWithoutValue() throws Exception {
        Captured captured = writeDate(Type.DateTime, 0, SENTINEL);

        assertNotNull(captured.thrown);
        DataException ex = assertThrows(DataException.class, () -> {
            throw captured.thrown;
        });
        assertEquals(String.format(CONVERSION_MESSAGE_FORMAT, Type.DateTime, COLUMN), ex.getMessage());
        assertEquals(1, captured.errorCount(ex.getMessage()), "the same text is logged once");
        assertNoSentinel(captured);
    }

    @Test
    @DisplayName("WRT-12: an empty string logs value length 0 and no value text")
    void emptyStringLogsLengthZero() throws Exception {
        Captured captured = writeDate(Type.DateTime, 0, "");

        assertEquals(1, captured.errorCount(parseFailureLine(Type.DateTime, 0)), captured.errorMessages().toString());
    }

    @Test
    @DisplayName("WRT-12: the value length counts UTF-16 code units, so the sentinel followed by U+1F600 logs value length 18")
    void valueLengthCountsUtf16CodeUnits() throws Exception {
        String value = SENTINEL + GRIN_EMOJI;
        Captured captured = writeDate(Type.DateTime, 0, value);

        assertEquals(18, value.length());
        assertEquals(1, captured.errorCount(parseFailureLine(Type.DateTime, 18)), captured.errorMessages().toString());
        assertNoSentinel(captured);
    }

    @Test
    @DisplayName("WRT-12: a null date value writes the null marker byte 0x01 and logs no ERROR")
    void nullValueWritesNullMarkerAndLogsNoError() throws Exception {
        ClickHouseWriter writer = newWriter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Captured captured = capture(() -> writer.doWriteDates(Type.DateTime, out, new Data(Schema.STRING_SCHEMA, null), 0, COLUMN));

        assertNull(captured.thrown);
        assertArrayEquals(new byte[] {0x01}, out.toByteArray());
        assertTrue(captured.errorMessages().isEmpty(), captured.errorMessages().toString());
    }

    private static Captured writeColValue(Column column, String value) throws Exception {
        ClickHouseWriter writer = newWriter();
        return capture(() -> writer.doWriteColValue(column, new ByteArrayOutputStream(), stringData(value), false));
    }

    private static Column column(String name, Type type) {
        return Column.builder().name(name).type(type).build();
    }

    @Test
    @DisplayName("WRT-12: an invalid UUID string logs one generic-catch line with the column, types and exception class, and no throwable")
    void uuidWriteFailureLogsMetadataWithoutThrowable() throws Exception {
        Captured captured = writeColValue(column(UUID_COLUMN, Type.UUID), SENTINEL);

        String expected = String.format(WRITE_FAILURE_LINE_FORMAT, UUID_COLUMN, Type.UUID, IllegalArgumentException.class.getName());
        assertEquals(1, captured.errorCount(expected), captured.errorMessages().toString());
        for (LogCapture.CapturedEvent event : captured.events) {
            assertFalse(event.hasThrowable(), "no log event may carry a throwable: " + event.message());
        }
        assertNoSentinel(captured);
        IllegalArgumentException raw = assertThrows(IllegalArgumentException.class, () -> UUID.fromString(SENTINEL));
        assertTrue(raw.getMessage().contains(SENTINEL), "self-check: the JDK UUID message embeds the value");
    }

    @Test
    @DisplayName("WRT-13: the generic catch throws a value-free DataException naming the column, the ClickHouse type and the exception class (backlog 999.3 delivered)")
    void uuidWriteFailureThrowsValueFreeDataException() throws Exception {
        ClickHouseWriter writer = newWriter();
        Column uuid = column(UUID_COLUMN, Type.UUID);

        // This test pinned the Phase 18 D-09 residual: the original IllegalArgumentException was
        // rethrown with the record value in its message, which reached Connect status, the DLQ headers
        // and the Error inserting records logs. WRT-13 (backlog 999.3) flips it. The raw-JDK check
        // below keeps the absence check meaningful: the same input really embeds the value.
        DataException ex = assertThrows(
                DataException.class,
                () -> writer.doWriteColValue(uuid, new ByteArrayOutputStream(), stringData(SENTINEL), false));

        assertTrue(ex.getMessage().contains(UUID_COLUMN));
        assertTrue(ex.getMessage().contains(String.valueOf(Type.UUID)));
        assertTrue(ex.getMessage().contains(IllegalArgumentException.class.getName()));
        for (String text : ThrowableTexts.allTexts(ex)) {
            assertFalse(text.contains(SENTINEL), "a thrown text must carry no record value");
        }
        IllegalArgumentException raw = assertThrows(IllegalArgumentException.class, () -> UUID.fromString(SENTINEL));
        assertTrue(raw.getMessage().contains(SENTINEL), "self-check: the JDK UUID message embeds the value");
    }

    @Test
    @DisplayName("WRT-12: a DateTime failure reached through doWriteColValue logs only data-free ERROR events and none carries a throwable")
    void dateTimeFailureThroughWriteColValueLogsNoValueAndNoThrowable() throws Exception {
        Captured captured = writeColValue(column(COLUMN, Type.DateTime), SENTINEL);

        assertEquals(1, captured.errorCount(parseFailureLine(Type.DateTime, SENTINEL.length())), captured.errorMessages().toString());
        String generic = String.format(WRITE_FAILURE_LINE_FORMAT, COLUMN, Type.DateTime, DataException.class.getName());
        assertEquals(1, captured.errorCount(generic), captured.errorMessages().toString());
        for (LogCapture.CapturedEvent event : captured.events) {
            assertFalse(event.hasThrowable(), "no log event may carry a throwable: " + event.message());
        }
        assertNoSentinel(captured);
    }
}
