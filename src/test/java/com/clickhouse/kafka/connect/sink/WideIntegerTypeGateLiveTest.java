package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.helper.RowSnapshots;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * ClickHouse-backed proof for TYPE-08: a non-nullable, no-default {@code Int256} or {@code UInt256}
 * column fails fast through {@code validateDataSchema} with the actionable message and writes no
 * row, while {@code Int128} and {@code UInt128} columns fed from INT64 fields still round-trip.
 * New fork-only class: it does not extend or modify any upstream test class.
 *
 * <p>Every assertion is a typed SQL read-back through {@link RowSnapshots}, never a row-count
 * helper.
 */
public class WideIntegerTypeGateLiveTest extends ClickHouseBase {

    private static final String ROW_ID_COLUMN = "id";
    private static final String WIDE_COLUMN = "big";
    private static final String INT128_COLUMN = "i128";
    private static final String UINT128_COLUMN = "u128";
    private static final String ESCAPE_HATCH_INSERT_FORMAT = "insertFormat=json";
    private static final String ESCAPE_HATCH_BYPASS_ROW_BINARY = "bypassRowBinary=true";
    private static final String GENERIC_VALIDATION_MESSAGE = "Data schema validation failed.";
    private static final String SELECT_ID_QUERY = "SELECT id FROM %s";

    private static void createTable(ClickHouseHelperClient chc, String tableName, String createTableQuery) {
        try (Records ignored = chc.queryV2(String.format(createTableQuery, tableName))) {
            // table created
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void putAndStop(Map<String, String> props, List<SinkRecord> records) {
        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        try {
            sinkTask.start(props);
            sinkTask.put(records);
        } finally {
            sinkTask.stop();
        }
    }

    private static Schema idAndInt64Schema(String... int64Columns) {
        SchemaBuilder builder = SchemaBuilder.struct().field(ROW_ID_COLUMN, Schema.INT32_SCHEMA);
        for (String column : int64Columns) {
            builder.field(column, Schema.INT64_SCHEMA);
        }
        return builder.build();
    }

    /** Walks the cause chain, returning the first DataException and recording every message on the way. */
    private static DataException findDataException(Throwable top, StringBuilder causeChain) {
        DataException found = null;
        Throwable current = top;
        while (current != null) {
            if (current.getMessage() != null) {
                causeChain.append(current.getMessage()).append(" | ");
            }
            if (found == null && current instanceof DataException) {
                found = (DataException) current;
            }
            current = current.getCause();
        }
        return found;
    }

    private void assertWideIntegerPutFailsFast(TestInfo testInfo, String clickHouseType, String suffix)
            throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName("wide_integer_gate_" + suffix);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        // Create the table BEFORE start() - ClickHouseWriter.start() fails against an empty database.
        createTable(
                chc,
                tableName,
                "CREATE TABLE %s (id UInt32, big " + clickHouseType + ") ENGINE = MergeTree ORDER BY id");
        props.put("name", uniqueConnectorName(testInfo));

        try {
            Schema schema = idAndInt64Schema(WIDE_COLUMN);
            Struct value = new Struct(schema).put(ROW_ID_COLUMN, 1).put(WIDE_COLUMN, 42L);
            SinkRecord record = new SinkRecord(tableName, 0, null, null, schema, value, 1);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> putAndStop(props, List.of(record)));

            // A bare assertThrows(RuntimeException.class, ...) would pass identically before the gate
            // existed, so assert on the content of the cause chain.
            StringBuilder causeChain = new StringBuilder();
            DataException dataException = findDataException(ex, causeChain);
            assertTrue(dataException instanceof DataException, causeChain.toString());
            String message = dataException.getMessage();
            assertTrue(message.contains(WIDE_COLUMN), message);
            assertTrue(message.contains(clickHouseType.toUpperCase()), message);
            assertTrue(message.contains(ESCAPE_HATCH_INSERT_FORMAT), message);
            assertTrue(message.contains(ESCAPE_HATCH_BYPASS_ROW_BINARY), message);
            assertFalse(message.contains("42"), message);
            assertFalse(causeChain.toString().contains(GENERIC_VALIDATION_MESSAGE), causeChain.toString());

            List<Map<String, String>> rows =
                    RowSnapshots.select(chc, String.format(SELECT_ID_QUERY, tableName), ROW_ID_COLUMN);
            assertEquals(0, rows.size(), "no row may be written for a rejected record");
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @Test
    @DisplayName("TYPE-08: put() into a non-nullable Int256 column fails with the actionable DataException and writes no row")
    void putIntoInt256ColumnFailsFastAndWritesNoRow(TestInfo testInfo) throws Exception {
        assertWideIntegerPutFailsFast(testInfo, "Int256", "int256");
    }

    @Test
    @DisplayName("TYPE-08: put() into a non-nullable UInt256 column fails with the actionable DataException and writes no row")
    void putIntoUInt256ColumnFailsFastAndWritesNoRow(TestInfo testInfo) throws Exception {
        assertWideIntegerPutFailsFast(testInfo, "UInt256", "uint256");
    }

    @Test
    @DisplayName("TYPE-08: Int128 and UInt128 columns fed from INT64 fields still round-trip Long.MAX_VALUE, Long.MIN_VALUE and 0 by typed read-back")
    void int128AndUInt128ColumnsRoundTripInt64Extremes(TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName("wide_integer_gate_128");
        ClickHouseTestHelpers.dropTable(chc, tableName);
        createTable(
                chc,
                tableName,
                "CREATE TABLE %s (id UInt32, i128 Int128, u128 UInt128) ENGINE = MergeTree ORDER BY id");
        props.put("name", uniqueConnectorName(testInfo));

        try {
            Schema schema = idAndInt64Schema(INT128_COLUMN, UINT128_COLUMN);
            SinkRecord first = new SinkRecord(
                    tableName, 0, null, null, schema,
                    new Struct(schema).put(ROW_ID_COLUMN, 1).put(INT128_COLUMN, Long.MAX_VALUE).put(UINT128_COLUMN, 0L),
                    1);
            SinkRecord second = new SinkRecord(
                    tableName, 0, null, null, schema,
                    new Struct(schema)
                            .put(ROW_ID_COLUMN, 2)
                            .put(INT128_COLUMN, Long.MIN_VALUE)
                            .put(UINT128_COLUMN, Long.MAX_VALUE),
                    2);

            putAndStop(props, List.of(first, second));

            List<Map<String, String>> rows = RowSnapshots.select(
                    chc,
                    "SELECT id, toString(i128) AS i128_text, toString(u128) AS u128_text FROM " + tableName
                            + " ORDER BY id",
                    ROW_ID_COLUMN, "i128_text", "u128_text");
            assertEquals(2, rows.size());
            assertEquals(
                    Map.of(ROW_ID_COLUMN, "1", "i128_text", Long.toString(Long.MAX_VALUE), "u128_text", "0"),
                    rows.get(0));
            assertEquals(
                    Map.of(
                            ROW_ID_COLUMN, "2",
                            "i128_text", Long.toString(Long.MIN_VALUE),
                            "u128_text", Long.toString(Long.MAX_VALUE)),
                    rows.get(1));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }
}
