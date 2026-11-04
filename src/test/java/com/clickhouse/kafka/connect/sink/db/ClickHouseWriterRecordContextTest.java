package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.data.SchemaType;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the per-record context that {@code ClickHouseWriter} attaches to serialization
 * failures. None of these need a running ClickHouse instance: the failure happens while the block
 * is being built, before a single byte reaches the server.
 */
public class ClickHouseWriterRecordContextTest {

    private static final String TOPIC = "events-topic";
    private static final int PARTITION = 3;
    private static final long OFFSET = 42L;
    private static final String COLUMN_NAME = "effective_at";

    @Test
    public void serializationFailureNamesTopicPartitionOffsetAndColumn() {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        // A UInt64 column cannot take a String, so the block cannot be built and the writer has to
        // report which record failed instead of failing the whole task with a bare cast error.
        Record record = record(new Data(Schema.STRING_SCHEMA, "definitely-not-a-number"));

        // The fork keeps the writer's own exception and logs the coordinates value-free (docs/FORK-DELTA.md).
        try (LogCapture log = LogCapture.attach(ClickHouseWriter.class)) {
            DataException thrown = assertThrows(DataException.class,
                    () -> writer.writeRecordsRowBinary(List.of(record), table(), new ByteArrayOutputStream(), false));

            List<LogCapture.CapturedEvent> coordinateLines = log.events("ERROR").stream()
                    .filter(event -> event.message().startsWith("Record serialization failed: "))
                    .collect(Collectors.toList());
            assertEquals(1, coordinateLines.size(), "should log one coordinate line");
            String line = coordinateLines.get(0).message();
            assertTrue(line.contains("topic [" + TOPIC + "]"), "should name the topic: " + line);
            assertTrue(line.contains("partition [" + PARTITION + "]"), "should name the partition: " + line);
            assertTrue(line.contains("offset [" + OFFSET + "]"), "should name the offset: " + line);
            assertFalse(coordinateLines.get(0).hasThrowable(), "the coordinate line should carry no throwable");

            String message = thrown.getMessage();
            assertTrue(message.contains("`" + COLUMN_NAME + "`"), "should name the column: " + message);
            // DataException, not RetriableException: the framework must be able to route the offending
            // record to the DLQ instead of redelivering the batch forever.
            assertInstanceOf(DataException.class, thrown);
            assertTrue(thrown.getCause() != null, "should keep the underlying failure as the cause");
        }
    }

    @Test
    public void retriableFailureFromTheColumnWriterIsRethrownUntouched() {
        RetriableException retriable = new RetriableException("server is restarting");
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0)) {
            @Override
            protected void doWriteCol(Data value, boolean fieldExists, Column col, OutputStream stream,
                    boolean defaultsSupport) {
                throw retriable;
            }
        };

        RetriableException thrown = assertThrows(RetriableException.class,
                () -> writer.writeRecordsRowBinary(List.of(record(new Data(Schema.INT64_SCHEMA, 1L))), table(),
                        new ByteArrayOutputStream(), false));

        // A retriable failure is not a serialization problem: wrapping it into a DataException would
        // turn a transient server condition into a permanently failed record.
        assertSame(retriable, thrown, "the original RetriableException must propagate as-is");
    }

    private static Table table() {
        Column column = Column.extractColumn(COLUMN_NAME, "UInt64", false, false, false);
        return new Table("default", "events", false, List.of(column), 1);
    }

    private static Record record(Data value) {
        Map<String, Data> jsonMap = new HashMap<>();
        jsonMap.put(COLUMN_NAME, value);
        SinkRecord sinkRecord = new SinkRecord(TOPIC, PARTITION, null, null, Schema.STRING_SCHEMA, "value", OFFSET);
        return Record.newRecord(SchemaType.SCHEMA_LESS, TOPIC, PARTITION, OFFSET, Collections.emptyList(), jsonMap,
                "default", sinkRecord);
    }
}
