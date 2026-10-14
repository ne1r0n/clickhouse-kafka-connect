package com.clickhouse.kafka.connect.sink.db;

import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.connect.sink.SinkRecord;

/**
 * A real {@link ClickHouseWriter} whose two network insert methods are replaced by byte capture. It
 * is not a mock: routing ({@code doInsertBatch}), table lookup and the serialization loops run
 * unchanged, and only {@code doInsertJson} and {@code doInsertRawBinary} are overridden, so the
 * helper client is never touched. It exists for the places where AGENTS.md forbids mocking the
 * writer (state-machine tests) and for byte-level assertions on the insert body. It deliberately
 * uses no Mockito: inline-mocking {@code ClickHouseHelperClient} crashed the Semeru/OpenJ9 test
 * JVM natively inside {@code retransformClasses} in the full suite. {@code public} so tests in
 * other packages can use it.
 */
public final class CapturingClickHouseWriter extends ClickHouseWriter {

    public static final String ROUTE_ROWBINARY = "rowbinary";
    public static final String ROUTE_JSON = "json";

    /** One insert that reached a network hook: its route, exact body, dedup token and format flag. */
    public record Capture(String route, byte[] body, String deduplicationToken, boolean supportDefaults) {}

    private static final Method WRITE_ROWBINARY = serializer(
            "writeRecordsRowBinary", List.class, Table.class, OutputStream.class, boolean.class);
    private static final Method WRITE_JSON =
            serializer("writeRecordsJson", List.class, Table.class, OutputStream.class);

    private final Table table;
    private final List<Capture> captures = new ArrayList<>();

    public CapturingClickHouseWriter(Table table, ClickHouseSinkConfig csc) {
        super(new SinkTaskStatistics(null, 0));
        this.table = table;
        try {
            setField("csc", csc);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("ClickHouseWriter fields changed; update this test seam", e);
        }
    }

    private static Method serializer(String name, Class<?>... parameterTypes) {
        try {
            Method method = ClickHouseWriter.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private void setField(String name, Object value) throws ReflectiveOperationException {
        java.lang.reflect.Field field = ClickHouseWriter.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(this, value);
    }

    /**
     * The stub table of the empty-CDC tests: the columns a converted Debezium record carries
     * (id INT32, name STRING, plus the injected _version UINT64 and is_deleted UINT8). With {@code
     * hasDefaults} the writer takes the RowBinaryWithDefaults route.
     */
    public static Table stubCdcTable(boolean hasDefaults) {
        List<Column> columns = new ArrayList<>();
        columns.add(column("id", Type.INT32, hasDefaults));
        columns.add(column("name", Type.STRING, hasDefaults));
        columns.add(column("_version", Type.UINT64, hasDefaults));
        columns.add(column("is_deleted", Type.UINT8, hasDefaults));
        return new Table("default", "cdc_topic", hasDefaults, columns, columns.size());
    }

    private static Column column(String name, Type type, boolean hasDefault) {
        return Column.builder().name(name).type(type).hasDefault(hasDefault).build();
    }

    /** Converts the sink records the way the sink task does with {@code debeziumCDCEnabled=true}. */
    public static List<Record> convertAll(List<SinkRecord> sinkRecords) {
        List<Record> records = new ArrayList<>(sinkRecords.size());
        for (SinkRecord sinkRecord : sinkRecords) {
            records.add(Record.convert(sinkRecord, false, ".", "default", true));
        }
        return records;
    }

    public List<Capture> captures() {
        return Collections.unmodifiableList(captures);
    }

    @Override
    protected Table getTable(String database, String topic) {
        return table;
    }

    @Override
    protected void doInsertRawBinary(
            List<Record> records,
            Table table,
            QueryIdentifier queryId,
            boolean supportDefaults,
            boolean retry)
            throws IOException {
        byte[] body = serialize(stream -> WRITE_ROWBINARY.invoke(this, records, table, stream, supportDefaults));
        captures.add(new Capture(ROUTE_ROWBINARY, body, queryId.getDeduplicationToken(), supportDefaults));
    }

    @Override
    protected void doInsertJson(List<Record> records, Table table, QueryIdentifier queryId)
            throws IOException {
        byte[] body = serialize(stream -> WRITE_JSON.invoke(this, records, table, stream));
        captures.add(new Capture(ROUTE_JSON, body, queryId.getDeduplicationToken(), false));
    }

    @FunctionalInterface
    private interface BodyWriter {
        void writeTo(OutputStream stream) throws ReflectiveOperationException;
    }

    private static byte[] serialize(BodyWriter writer) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        try {
            writer.writeTo(body);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return body.toByteArray();
    }
}
