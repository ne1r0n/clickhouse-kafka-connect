package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.errors.RetriableException;
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

    @Test
    @DisplayName(
            "COLS-02: the default empty jsonInsertColumns list fills every column from the payload with correct values")
    void defaultEmptyListFillsEveryColumnFromPayload(TestInfo testInfo) throws Exception {
        // D-11: this is a confirmatory test, not a discovery test. Phase 3's
        // JsonDynamicRoundTripTest already drove doInsertJsonV2 with the default empty
        // jsonInsertColumns list and was green, so the roadmap's empty-list research flag was
        // withdrawn - COLS-02 simply asserts the already-established behavior again, explicitly,
        // as its own pin.
        Map<String, String> props = createProps();
        String tableName = createTopicName("cols02_default_list");

        ClickHouseHelperClient chc = createClient(props);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        ClickHouseTestHelpers.createTable(chc, tableName, FOUR_COLUMN_DDL);

        props.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        // JSON_INSERT_COLUMNS deliberately NOT SET.
        props.put("name", uniqueConnectorName(testInfo));

        ClickHouseSinkTask chst = new ClickHouseSinkTask();
        try {
            chst.start(props);

            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", 1);
            value.put("listed", 7);
            value.put("extra", 999);
            value.put("tag", "provided");

            List<SinkRecord> records = List.of(new SinkRecord(tableName, 0, null, null, null, value, 1L));

            chst.put(records);

            List<Map<String, String>> rows = allRows(chc,
                    String.format("SELECT id, listed, extra, tag FROM `%s` ORDER BY id", tableName),
                    "id", "listed", "extra", "tag");
            assertEquals(1, rows.size());
            assertEquals("1", rows.get(0).get("id"));
            assertEquals("7", rows.get(0).get("listed"));
            assertEquals("999", rows.get(0).get("extra"));
            assertEquals("provided", rows.get(0).get("tag"));
        } finally {
            chst.stop();
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @Test
    @DisplayName(
            "COLS-04: a payload key that is a real column outside the configured list, and a payload key that is not a column at all, both leave the insert successful and the unlisted real column at its DEFAULT")
    void extraPayloadKeysOutsideListDoNotBreakInsertAndStayAtDefault(TestInfo testInfo) throws Exception {
        Map<String, String> props = createProps();
        String tableName = createTopicName("cols04_extra_keys");

        ClickHouseHelperClient chc = createClient(props);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        ClickHouseTestHelpers.createTable(chc, tableName, FOUR_COLUMN_DDL);

        props.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        props.put(ClickHouseSinkConfig.JSON_INSERT_COLUMNS, "id,listed");
        props.put("name", uniqueConnectorName(testInfo));

        ClickHouseSinkTask chst = new ClickHouseSinkTask();
        try {
            chst.start(props);

            // Insertion order deliberately differs from the configured list order (tag, listed,
            // extra, id, bogus_key) - the JSONEachRow body is keyed by name, so payload order
            // never decides which column receives which value.
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("tag", "should_not_land");
            value.put("listed", 9);
            value.put("extra", 777);
            value.put("id", 1);
            value.put("bogus_key", "not_a_column_at_all");

            List<SinkRecord> records = List.of(new SinkRecord(tableName, 0, null, null, null, value, 1L));

            assertDoesNotThrow(() -> chst.put(records));

            List<Map<String, String>> rows = allRows(chc,
                    String.format("SELECT id, listed, extra, tag FROM `%s` ORDER BY id", tableName),
                    "id", "listed", "extra", "tag");
            assertEquals(1, rows.size());
            // The whole point: the JSONEachRow body content and the INSERT column clause are
            // independent gates, and only the column clause decides what is written.
            assertEquals("9", rows.get(0).get("listed"));
            assertEquals("42", rows.get(0).get("extra"));
            assertEquals("unset", rows.get(0).get("tag"));
        } finally {
            chst.stop();
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @Test
    @DisplayName(
            "COLS-05: a jsonInsertColumns entry naming a nonexistent column makes put() throw a non-retriable RuntimeException with no partial insert and no NullPointerException")
    void nonexistentColumnThrowsNonRetriableWithNoPartialInsert(TestInfo testInfo) throws Exception {
        Map<String, String> props = createProps();
        String tableName = createTopicName("cols05_bad_column");

        ClickHouseHelperClient chc = createClient(props);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        ClickHouseTestHelpers.createTable(chc, tableName, FOUR_COLUMN_DDL);

        props.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        props.put(ClickHouseSinkConfig.JSON_INSERT_COLUMNS, "id,no_such_column");
        props.put("name", uniqueConnectorName(testInfo));

        ClickHouseSinkTask chst = new ClickHouseSinkTask();
        try {
            chst.start(props);

            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", 1);

            List<SinkRecord> records = List.of(new SinkRecord(tableName, 0, null, null, null, value, 1L));

            RuntimeException ex = assertThrows(RuntimeException.class, () -> chst.put(records));
            assertTrue(messageChainContains(ex, "no_such_column"), "message chain: " + ex);
            assertEquals(0, ClickHouseTestHelpers.countRows(chc, tableName));

            // Walk the cause chain and assert no NullPointerException appears anywhere in it.
            Throwable walk = ex;
            while (walk != null) {
                assertFalse(walk instanceof NullPointerException, "NPE found in cause chain: " + ex);
                walk = walk.getCause();
            }

            // D-10 classification pin: this must NOT be a RetriableException - a typo'd column
            // name retrying forever in production would be exactly the failure mode this
            // assertion exists to catch.
            assertFalse(ex instanceof RetriableException, "classification: " + ex.getClass());

            // Observed classification (this session's live run, ClickHouse latest 26.8.6.5,
            // client V2): the outer exception is java.lang.RuntimeException (two layers - the
            // Utils.handleException wrap "Number of records: 1", cause is another RuntimeException
            // "Topic: [...] (QueryId: [...])"); the root cause is
            // com.clickhouse.client.api.ServerException (the client-V2 API exception type, NOT
            // com.clickhouse.client.ClickHouseException, which is why
            // Utils.handleException's `rootCause instanceof ClickHouseException` switch never
            // matches under client V2 and this always falls through to the non-retriable
            // RuntimeException wrap regardless of the switch's case list), carrying ClickHouse
            // error code 16 (NO_SUCH_COLUMN_IN_TABLE), message "Code: 16. DB::Exception: No such
            // column no_such_column in table <db>.<table> (...). (NO_SUCH_COLUMN_IN_TABLE)
            // (version 26.8.6.5 (official build))". Recorded verbatim in the plan SUMMARY per
            // D-10.
        } finally {
            chst.stop();
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @Test
    @DisplayName(
            "COLS-06: on the non-explicit JSON path, jsonInsertColumns still fills the listed columns, a real-but-unlisted column keeps its DEFAULT despite cleanupExtraFields keeping its payload key, and a non-column key is stripped")
    void nonExplicitPathHonoursColumnListAndCleanupExtraFieldsInteraction(TestInfo testInfo) throws Exception {
        Map<String, String> props = createProps();
        String tableName = createTopicName("cols06_non_explicit");

        ClickHouseHelperClient chc = createClient(props);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        ClickHouseTestHelpers.createTable(chc, tableName, FOUR_COLUMN_DDL);

        // INSERT_FORMAT deliberately NOT SET: jsonInsertFormatExplicit stays false,
        // cleanupExtraFields runs, and the table mapping is fetched normally.
        props.put(ClickHouseSinkConfig.JSON_INSERT_COLUMNS, "id,listed");
        props.put("name", uniqueConnectorName(testInfo));

        ClickHouseSinkTask chst = new ClickHouseSinkTask();
        try {
            chst.start(props);

            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", 1);
            value.put("listed", 5);
            value.put("extra", 777);
            value.put("bogus_key", "not_a_column_at_all");

            List<SinkRecord> records = List.of(new SinkRecord(tableName, 0, null, null, null, value, 1L));

            assertDoesNotThrow(() -> chst.put(records));

            List<Map<String, String>> rows = allRows(chc,
                    String.format("SELECT id, listed, extra, tag FROM `%s` ORDER BY id", tableName),
                    "id", "listed", "extra", "tag");
            assertEquals(1, rows.size());
            assertEquals("5", rows.get(0).get("listed"));
            // Mechanism (D-13): cleanupExtraFields KEPT the "extra" key because it is a real root
            // column, so "extra" was serialized into the JSONEachRow body - only the INSERT
            // column clause excluded it from the write. "bogus_key" was stripped by
            // cleanupExtraFields before serialization ever happened. Two different gates doing
            // two different jobs.
            assertEquals("42", rows.get(0).get("extra"));
            assertEquals("unset", rows.get(0).get("tag"));
        } finally {
            chst.stop();
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    /** Walks the {@code getCause()} chain looking for a message containing {@code substring} (D-17: substring, never full-string equality). */
    private static boolean messageChainContains(Throwable t, String substring) {
        while (t != null) {
            String message = t.getMessage();
            if (message != null && message.contains(substring)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
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
