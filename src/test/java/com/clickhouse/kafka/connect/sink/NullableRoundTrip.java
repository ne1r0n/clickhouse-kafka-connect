package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.insert.InsertResponse;
import com.clickhouse.client.api.insert.InsertSettings;
import com.clickhouse.client.api.query.QueryResponse;
import com.clickhouse.client.api.query.QuerySettings;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.kafka.connect.sink.db.CapturingClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseFieldDescriptor;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullability;
import com.clickhouse.kafka.connect.sink.helper.RowSnapshots;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;

/**
 * Shared live round-trip helpers for the Phase 19 classes (COLS-08, COLS-09), generalised from the
 * private helpers of {@code NestedNullRowBinaryLiveTest}. Values are ASCII and every assertion is a
 * typed SQL read-back through {@link RowSnapshots}, never a row count. Static methods only, no
 * state.
 */
final class NullableRoundTrip {

    static final String DEFAULT_COLUMN_DDL = "d UInt32 DEFAULT 42";
    static final String DEFAULT_VALUE_TEXT = "42";
    static final String TRUE_TEXT = "1";
    static final String FALSE_TEXT = "0";

    private static final String ROW_ID_COLUMN = "id";
    private static final String DESCRIBE_QUERY_FORMAT = "DESCRIBE TABLE `%s`.`%s`";
    private static final String DESCRIBE_INCLUDE_SUBCOLUMNS = "describe_include_subcolumns";
    private static final String CAUSE_SEPARATOR = " | ";
    private static final String SERVER_VERSION_QUERY = "SELECT version() AS v";
    private static final String SERVER_VERSION_ALIAS = "v";

    private NullableRoundTrip() {}

    /**
     * The same table twice: PLAIN has no DEFAULT column (the writer takes RowBinary), DEFAULT adds an
     * unrelated column {@code d} with a DEFAULT of 42, so DESCRIBE reports a default and the writer
     * takes RowBinaryWithDefaults.
     */
    enum Twin {
        PLAIN(false),
        DEFAULT(true);

        private final boolean hasDefaults;

        Twin(boolean hasDefaults) {
            this.hasDefaults = hasDefaults;
        }

        boolean hasDefaults() {
            return hasDefaults;
        }

        /** The CREATE TABLE format (one {@code %s} for the table name) around {@code columnsDdl}. */
        String ddl(String columnsDdl) {
            return "CREATE TABLE %s (id UInt32, " + columnsDdl
                    + (hasDefaults ? ", " + DEFAULT_COLUMN_DDL : "")
                    + ") ENGINE = MergeTree ORDER BY id";
        }
    }

    /** One read-back cell: the SQL expression, its column alias and the expected text. */
    record Cell(String alias, String sqlExpression, String expected) {}

    static void createTable(ClickHouseHelperClient chc, String tableName, String ddlFormat) {
        try (Records ignored = chc.queryV2(String.format(ddlFormat, tableName))) {
            // table created
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Runs one put in a try/finally so a failing put still stops the task. */
    static void putAndStop(Map<String, String> props, List<SinkRecord> records) {
        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        try {
            sinkTask.start(props);
            sinkTask.put(records);
        } finally {
            sinkTask.stop();
        }
    }

    /** Exactly one row for the id, and its aliases equal the expected texts (null allowed). */
    static void selectAndAssert(ClickHouseHelperClient chc, String tableName, int id, List<Cell> cells)
            throws Exception {
        StringBuilder sql = new StringBuilder("SELECT ");
        Map<String, String> expected = new LinkedHashMap<>();
        for (Cell cell : cells) {
            sql.append(expected.isEmpty() ? "" : ", ")
                    .append(cell.sqlExpression())
                    .append(" AS ")
                    .append(cell.alias());
            expected.put(cell.alias(), cell.expected());
        }
        sql.append(" FROM ").append(tableName).append(" WHERE id = ").append(id);
        List<Map<String, String>> rows =
                RowSnapshots.select(chc, sql.toString(), expected.keySet().toArray(new String[0]));
        assertEquals(1, rows.size(), "row id=" + id + " must exist exactly once");
        assertEquals(expected, rows.get(0), "typed read-back of row id=" + id);
    }

    /** Every message of the cause chain, joined, for substring assertions. */
    static String causeChainMessages(Throwable throwable) {
        StringBuilder chain = new StringBuilder();
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current.getMessage() != null) {
                chain.append(current.getMessage()).append(CAUSE_SEPARATOR);
            }
        }
        return chain.toString();
    }

    /**
     * The put must throw a RuntimeException whose cause chain holds every substring, and the failed
     * batch must leave no row behind. Returns the exception for further inspection.
     */
    static RuntimeException assertPutFails(
            Map<String, String> props,
            List<SinkRecord> records,
            ClickHouseHelperClient chc,
            String tableName,
            String... causeSubstrings)
            throws Exception {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> putAndStop(props, records));
        String messages = causeChainMessages(failure);
        for (String substring : causeSubstrings) {
            assertTrue(messages.contains(substring), "expected '" + substring + "' in: " + messages);
        }
        String ids = records.stream()
                .map(record -> String.valueOf(((Struct) record.value()).getInt32(ROW_ID_COLUMN)))
                .collect(Collectors.joining(","));
        assertEquals(
                List.of(),
                RowSnapshots.select(
                        chc, "SELECT id FROM " + tableName + " WHERE id IN (" + ids + ")", ROW_ID_COLUMN),
                "a failed put must leave no row behind");
        return failure;
    }

    /** Inserts the body the real writer captured, with the format the writer chose for it. */
    static void insertCaptured(
            ClickHouseHelperClient chc, String tableName, CapturingClickHouseWriter.Capture capture)
            throws Exception {
        ClickHouseFormat format =
                capture.supportDefaults() ? ClickHouseFormat.RowBinaryWithDefaults : ClickHouseFormat.RowBinary;
        try (InsertResponse ignored = chc.getClient()
                .insert(tableName, new ByteArrayInputStream(capture.body()), format, new InsertSettings())
                .get()) {
            // inserted
        }
    }

    /**
     * Reads DESCRIBE exactly as {@code describeTableV2} does (JSONEachRow, sub-columns included,
     * UTF-8 explicitly, rows through {@code fromJsonRow}), so the raw types are what discovery sees.
     */
    static List<ClickHouseFieldDescriptor> describeRows(ClickHouseHelperClient chc, String tableName)
            throws Exception {
        QuerySettings settings = new QuerySettings().setFormat(ClickHouseFormat.JSONEachRow);
        settings.serverSetting(DESCRIBE_INCLUDE_SUBCOLUMNS, "1");
        settings.setDatabase(chc.getDatabase());
        List<ClickHouseFieldDescriptor> rows = new ArrayList<>();
        try (QueryResponse response = chc.getClient()
                        .query(String.format(DESCRIBE_QUERY_FORMAT, chc.getDatabase(), tableName), settings)
                        .get();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                rows.add(ClickHouseFieldDescriptor.fromJsonRow(line));
            }
        }
        return rows;
    }

    /** Production's normalisation, so the approach test exercises the code discovery runs. */
    static String normaliseDeclaredType(String rawType) {
        return DeclaredNullability.normalise(rawType);
    }

    static String serverVersion(ClickHouseHelperClient chc) throws Exception {
        return RowSnapshots.select(chc, SERVER_VERSION_QUERY, SERVER_VERSION_ALIAS)
                .get(0)
                .get(SERVER_VERSION_ALIAS);
    }
}
