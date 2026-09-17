package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * ClickHouse-backed pins for {@code jsonInsertColumns}'s insert-time behavior: COLS-02 (the
 * default empty list), COLS-03 (an explicit subset against non-trivial DEFAULTs), COLS-04 (extra
 * payload keys outside the configured list), COLS-05 (a nonexistent column and its exception
 * classification), and COLS-06 (the non-explicit path's interaction with {@code
 * cleanupExtraFields}). New fork-only class (D-16): it does not extend or modify any upstream
 * test class. Every record built in this class is a schemaless {@code Map} value, never a {@code
 * Struct}, because {@code doInsertJsonV2} is the only route that passes {@code
 * csc.getJsonInsertColumns()} to {@code client.insert(...)} - the String route ({@code
 * doInsertStringV2}) uses the 4-arg overload with no column list at all, so {@code
 * jsonInsertColumns} is unreachable from a String-record test.
 */
public class JsonInsertColumnsLiveTest extends ClickHouseBase {

    private static final String FOUR_COLUMN_DDL =
            "CREATE TABLE %s (id UInt32, listed Int32, extra Int32 DEFAULT 42, tag String DEFAULT 'unset') ENGINE = MergeTree ORDER BY id";

    @Test
    @DisplayName(
            "COLS-03: an explicit jsonInsertColumns subset fills the listed columns and leaves every unlisted column at its DEFAULT")
    void explicitSubsetFillsListedColumnsAndLeavesUnlistedAtDefault(TestInfo testInfo) throws Exception {
        Map<String, String> props = createProps();
        String tableName = createTopicName("cols03_subset");

        ClickHouseHelperClient chc = createClient(props);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        ClickHouseTestHelpers.createTable(chc, tableName, FOUR_COLUMN_DDL);

        props.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        // Deliberately padded: the parser's trim (COLS-01) must survive all the way to the
        // server's column clause. This is COLS-01's contract observed live.
        props.put(ClickHouseSinkConfig.JSON_INSERT_COLUMNS, "  id , listed  ");
        props.put("name", uniqueConnectorName(testInfo));

        ClickHouseSinkTask chst = new ClickHouseSinkTask();
        try {
            chst.start(props);

            Map<String, Object> value1 = new LinkedHashMap<>();
            value1.put("id", 1);
            value1.put("listed", 7);

            Map<String, Object> value2 = new LinkedHashMap<>();
            value2.put("id", 2);
            value2.put("listed", 8);

            List<SinkRecord> records = List.of(
                    new SinkRecord(tableName, 0, null, null, null, value1, 1L),
                    new SinkRecord(tableName, 0, null, null, null, value2, 2L));

            chst.put(records);

            List<Map<String, String>> rows = allRows(chc,
                    String.format("SELECT id, listed, extra, tag FROM `%s` ORDER BY id", tableName),
                    "id", "listed", "extra", "tag");
            assertEquals(2, rows.size());
            assertEquals("7", rows.get(0).get("listed"));
            assertEquals("42", rows.get(0).get("extra"));
            assertEquals("unset", rows.get(0).get("tag"));
            assertEquals("8", rows.get(1).get("listed"));
            assertEquals("42", rows.get(1).get("extra"));
            assertEquals("unset", rows.get(1).get("tag"));
        } finally {
            chst.stop();
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    /**
     * Snapshots each row's requested columns into a plain {@code Map<String, String>} while the
     * row is still the iterator's current element. The client-v2 {@link GenericRecord} returned
     * by {@link Records}'s iterator is a mutable, reused view over the underlying binary reader
     * buffer - collecting the {@code GenericRecord} references themselves (rather than reading
     * their values immediately) leaves every collected reference pointing at whatever row the
     * iterator most recently advanced to, silently aliasing every "row" to the last one read.
     */
    private static List<Map<String, String>> allRows(ClickHouseHelperClient chc, String sql, String... columns)
            throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        try (Records records = chc.getClient().queryRecords(sql).get()) {
            for (GenericRecord r : records) {
                Map<String, String> row = new LinkedHashMap<>();
                for (String column : columns) {
                    row.put(column, r.getString(column));
                }
                rows.add(row);
            }
        }
        return rows;
    }
}
