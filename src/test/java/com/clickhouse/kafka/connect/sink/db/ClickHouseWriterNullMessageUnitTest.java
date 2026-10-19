package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.data.SchemaType;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.kafka.OffsetContainer;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import org.apache.kafka.connect.data.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free pins for WRT-11 (13-REVIEW WR-04). Upstream read the failure message inside the
 * stream filter before its own null check, so a message-less failure became a NullPointerException
 * that hid the real error. This class drives the real {@code doInsertRawBinary} catch through the
 * protected insert hooks, so no ClickHouse server is needed. It extends nothing, so that it stays
 * in {@code testUnit}: {@code ClickHouseBase} carries {@code @Tag("clickhouse")}. The helper client
 * is the Mockito-free {@link CountingHelperClient} (HARN-08).
 */
public class ClickHouseWriterNullMessageUnitTest {

    private static final String WRITER_HELPER_CLIENT_FIELD = "chc";
    private static final String TOPIC = "wrt11_topic";
    private static final String DATABASE = "default";
    private static final String RETRY_TRIGGER_MESSAGE =
            "Insert failed: ClickHouseException: Code: 33. DB::Exception: Unknown column";

    /**
     * Throws the stored instance itself from both insert hooks, never a wrapper, so that {@code
     * assertSame} can see the object that reaches the catch block. The hooks declare only {@code
     * throws IOException, ExecutionException, InterruptedException}, so a plain {@code Exception}
     * cannot be thrown from them.
     */
    private static final class ScriptedInsertWriter extends ClickHouseWriter {
        private final Table stubTable;
        private final Exception failure;
        int hookCalls = 0;

        ScriptedInsertWriter(SinkTaskStatistics statistics, Table stubTable, Exception failure) {
            super(statistics);
            if (!(failure instanceof IOException) && !(failure instanceof RuntimeException)) {
                throw new IllegalArgumentException(
                        "only IOException or RuntimeException can be thrown from the insert hooks, got "
                                + failure.getClass().getName());
            }
            this.stubTable = stubTable;
            this.failure = failure;
        }

        private void failFromHook() throws IOException {
            hookCalls++;
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

        @Override
        public boolean updateMapping(String database) {
            return true;
        }

        @Override
        protected Table getTable(String database, String topic) {
            return stubTable;
        }
    }

    private static Table stubTable() {
        return new Table(
                DATABASE, TOPIC, false, List.of(Column.builder().name("id").type(Type.INT32).build()), 1);
    }

    private static List<Record> oneRecord() {
        org.apache.kafka.connect.data.Field field =
                new org.apache.kafka.connect.data.Field("id", 0, Schema.INT32_SCHEMA);
        return List.of(new Record(
                SchemaType.SCHEMA, new OffsetContainer(TOPIC, 0, 0), List.of(field), null, DATABASE, null));
    }

    private static QueryIdentifier queryId() {
        return new QueryIdentifier(TOPIC, 0, 0, 0, "wrt11-query");
    }

    private static CountingHelperClient installHelperClient(ClickHouseWriter writer, boolean useClientV2)
            throws Exception {
        CountingHelperClient chc = new CountingHelperClient(useClientV2);
        Field field = ClickHouseWriter.class.getDeclaredField(WRITER_HELPER_CLIENT_FIELD);
        field.setAccessible(true);
        field.set(writer, chc);
        return chc;
    }

    @ParameterizedTest(name = "WRT-11: clientV2={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("WRT-11: a message-less RuntimeException from the RowBinary hook is rethrown as itself, with no NullPointerException and no retry")
    void messageLessFailureIsRethrownAsItself(boolean useClientV2) throws Exception {
        RuntimeException failure = new RuntimeException();
        ScriptedInsertWriter writer = new ScriptedInsertWriter(new SinkTaskStatistics(null, 0), stubTable(), failure);
        try (CountingHelperClient chc = installHelperClient(writer, useClientV2)) {
            Exception thrown =
                    assertThrows(Exception.class, () -> writer.doInsertRawBinary(oneRecord(), stubTable(), queryId(), false, true));

            assertSame(failure, thrown);
            assertEquals(1, writer.hookCalls);
            assertEquals(0, chc.pinCalls());
        }
    }

    @Test
    @DisplayName("WRT-11: an empty-string message is rethrown as itself with one hook call")
    void emptyMessageIsRethrownAsItself() throws Exception {
        RuntimeException failure = new RuntimeException("");
        ScriptedInsertWriter writer = new ScriptedInsertWriter(new SinkTaskStatistics(null, 0), stubTable(), failure);
        try (CountingHelperClient chc = installHelperClient(writer, true)) {
            Exception thrown =
                    assertThrows(Exception.class, () -> writer.doInsertRawBinary(oneRecord(), stubTable(), queryId(), false, true));

            assertSame(failure, thrown);
            assertEquals(1, writer.hookCalls);
            assertEquals(0, chc.pinCalls());
        }
    }

    @Test
    @DisplayName("WRT-11: a message containing ClickHouseException: Code: 33 inside longer text still refreshes the mapping and retries exactly once")
    void knownCodeInsideLongerTextRetriesOnce() throws Exception {
        RuntimeException failure = new RuntimeException(RETRY_TRIGGER_MESSAGE);
        ScriptedInsertWriter writer = new ScriptedInsertWriter(new SinkTaskStatistics(null, 0), stubTable(), failure);
        try (CountingHelperClient chc = installHelperClient(writer, true)) {
            Exception thrown =
                    assertThrows(Exception.class, () -> writer.doInsertRawBinary(oneRecord(), stubTable(), queryId(), false, true));

            assertSame(failure, thrown);
            assertEquals(2, writer.hookCalls);
            assertEquals(1, chc.pinCalls());
            assertEquals(1, chc.unpinCalls());
        }
    }

    @Test
    @DisplayName("WRT-11: a message containing ClickHouseException: Code: 33 with retry=false is not retried")
    void knownCodeWithoutRetryIsNotRetried() throws Exception {
        RuntimeException failure = new RuntimeException(RETRY_TRIGGER_MESSAGE);
        ScriptedInsertWriter writer = new ScriptedInsertWriter(new SinkTaskStatistics(null, 0), stubTable(), failure);
        try (CountingHelperClient chc = installHelperClient(writer, true)) {
            Exception thrown =
                    assertThrows(Exception.class, () -> writer.doInsertRawBinary(oneRecord(), stubTable(), queryId(), false, false));

            assertSame(failure, thrown);
            assertEquals(1, writer.hookCalls);
            assertEquals(0, chc.pinCalls());
        }
    }
}
