package com.clickhouse.kafka.connect.sink.db;

import com.clickhouse.client.api.ClickHouseException;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.ProxySinkTask;
import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;
import org.apache.kafka.connect.data.Schema;

/**
 * A real {@link ClickHouseWriter} whose two RowBinary network hooks run the real {@code
 * doWriteColValue} on one scripted column, value and stream. The real {@code doInsert}, {@code
 * doInsertBatch} and {@code doInsertRawBinary} (and its catch blocks) run unchanged, so a failure
 * provoked inside {@code doWriteColValue} travels the same path as in production. No mocking
 * library is used, because inline mocking crashed the Semeru/OpenJ9 test JVM natively inside
 * {@code retransformClasses} (HARN-08).
 *
 * <p>It is shared by the WRT-14 classification pin and by the later WRT-13 and WRT-15 plans of
 * phase 22, and it is part of the pin, so it changes only together with the pin.
 */
public final class ScriptedWriteFailureWriter extends ClickHouseWriter {

    /**
     * The wrapper text client-v2 0.9.6 puts on a non-IO failure leaving its insert call (measured
     * in 22-RESEARCH D-13).
     */
    public static final String V2_INSERT_FAILED_MESSAGE = "Insert failed (Attempt: 1/4 - Duration: PT0S)";

    private static final String STUB_TABLE_NAME = "wrt14_scripted_table";
    private static final String SINK_CONFIG_FIELD = "csc";
    private static final String HELPER_CLIENT_FIELD = "chc";
    private static final String PROCESSING_FIELD = "processing";
    private static final String DB_WRITER_FIELD = "dbWriter";

    private final Column column;
    private final Data value;
    private final Supplier<OutputStream> stream;
    private final Table stubTable;
    private RuntimeException primitiveFailure;
    private RuntimeException nestedColumnFailure;
    private int hookCalls = 0;
    private int updateMappingCalls = 0;

    public ScriptedWriteFailureWriter(
            ClickHouseSinkConfig csc,
            ClickHouseHelperClient chc,
            Column column,
            Data value,
            Supplier<OutputStream> stream) {
        super(new SinkTaskStatistics(null, 0));
        this.column = column;
        this.value = value;
        this.stream = stream;
        try {
            setWriterField(SINK_CONFIG_FIELD, csc);
            setWriterField(HELPER_CLIENT_FIELD, chc);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "ClickHouseWriter fields '" + SINK_CONFIG_FIELD + "' / '" + HELPER_CLIENT_FIELD
                            + "' changed; update this test seam",
                    e);
        }
        this.stubTable = new Table(csc.getDatabase(), STUB_TABLE_NAME, false, List.of(column), 1);
    }

    private void setWriterField(String name, Object fieldValue) throws ReflectiveOperationException {
        Field field = ClickHouseWriter.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(this, fieldValue);
    }

    /** Makes the primitive write of every column throw this failure instead of writing. */
    public ScriptedWriteFailureWriter failPrimitiveWith(RuntimeException failure) {
        this.primitiveFailure = failure;
        return this;
    }

    /** Makes the nested-column write (the Tuple field path) throw this failure instead of writing. */
    public ScriptedWriteFailureWriter failNestedColumnWith(RuntimeException failure) {
        this.nestedColumnFailure = failure;
        return this;
    }

    public int hookCalls() {
        return hookCalls;
    }

    public int updateMappingCalls() {
        return updateMappingCalls;
    }

    public Table stubTable() {
        return stubTable;
    }

    /** Replaces the writer inside the task's {@code Processing}, which keeps it in a non-final field. */
    public void installInto(ProxySinkTask task) {
        try {
            Field processingField = ProxySinkTask.class.getDeclaredField(PROCESSING_FIELD);
            processingField.setAccessible(true);
            Object processing = processingField.get(task);
            Field writerField = processing.getClass().getDeclaredField(DB_WRITER_FIELD);
            writerField.setAccessible(true);
            writerField.set(processing, this);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "ProxySinkTask." + PROCESSING_FIELD + " / Processing." + DB_WRITER_FIELD
                            + " changed; update this test seam",
                    e);
        }
    }

    @Override
    protected void doInsertRawBinaryV1(
            List<Record> records, Table table, QueryIdentifier queryId, boolean supportDefaults)
            throws IOException {
        hookCalls++;
        doWriteColValue(column, stream.get(), value, false);
    }

    /**
     * Rethrows an IOException unchanged and wraps any RuntimeException once, as client-v2 does: the
     * real client keeps an IOException as the deepest cause, so the root-cause classification is the
     * same either way.
     */
    @Override
    protected void doInsertRawBinaryV2(
            List<Record> records, Table table, QueryIdentifier queryId, boolean supportDefaults)
            throws IOException {
        hookCalls++;
        try {
            doWriteColValue(column, stream.get(), value, false);
        } catch (RuntimeException e) {
            throw new ClickHouseException(V2_INSERT_FAILED_MESSAGE, e);
        }
    }

    @Override
    public boolean updateMapping(String database) {
        updateMappingCalls++;
        return true;
    }

    @Override
    protected Table getTable(String database, String topic) {
        return stubTable;
    }

    @Override
    protected void doWritePrimitive(
            Type columnType, Schema.Type dataType, OutputStream out, Object primitive, Column col)
            throws IOException {
        if (primitiveFailure != null) {
            throw primitiveFailure;
        }
        super.doWritePrimitive(columnType, dataType, out, primitive, col);
    }

    @Override
    protected void doWriteCol(
            Data fieldValue, boolean fieldExists, Column col, OutputStream out, boolean defaultsSupport)
            throws IOException {
        if (nestedColumnFailure != null) {
            throw nestedColumnFailure;
        }
        super.doWriteCol(fieldValue, fieldExists, col, out, defaultsSupport);
    }

    /**
     * Accepts {@code bytesBeforeFailure} bytes, then throws the same IOException instance from every
     * write. A write that does not fit in the remaining budget is refused whole.
     */
    public static final class FailingOutputStream extends OutputStream {
        private final IOException failure;
        private int remaining;

        public FailingOutputStream(IOException failure, int bytesBeforeFailure) {
            this.failure = failure;
            this.remaining = bytesBeforeFailure;
        }

        @Override
        public void write(int b) throws IOException {
            if (remaining < 1) {
                throw failure;
            }
            remaining--;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (remaining < len) {
                throw failure;
            }
            remaining -= len;
        }
    }
}
