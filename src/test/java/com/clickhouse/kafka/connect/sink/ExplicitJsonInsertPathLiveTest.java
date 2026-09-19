package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * ClickHouse-backed pins for the metadata-free explicit JSON insert path: JSON-01 (the live
 * differential half - the connector-side table-existence check that metadata-free mode skips),
 * JSON-02 (table resolution via topic-to-table mapping with no fetched metadata), JSON-03 (the
 * live half of the {@code cleanupExtraFields} bypass - the Docker-free half lives in
 * {@code MetadataFreeJsonInsertUnitTest}), and JSON-04 (tombstone handling in a mixed batch and
 * an all-tombstone batch). New fork-only class (D-16): it does not extend or modify any upstream
 * test class. Every record built in this class is a schemaless {@code Map} value rather than a
 * {@code Struct}, per D-06, because the schemaless route is the only one where metadata-free mode
 * actually changes behavior - R-01 proved the Struct + RowBinary combination under
 * {@code insertFormat=json} silently drops data, which is explicitly out of scope for a pin here
 * (see Task 4's blocking-human checkpoint).
 *
 * <p>Disabled pending the JSONP-01 keep/drop decision (Phase 9, D-11): the metadata-free
 * {@code insertFormat=json} bypass this class pins, including the R-01 labelled loss test, was
 * dropped by the mechanical v1.6.0 merge and has not been re-applied to {@code src/main} in this
 * phase. Every assertion below is left intact so Phase 9 can re-enable this class unchanged if
 * the decision is keep.
 */
public class ExplicitJsonInsertPathLiveTest extends ClickHouseBase {

    private void createTable(ClickHouseHelperClient chc, String tableName, String createTableQuery) {
        try (Records ignored = chc.queryV2(String.format(createTableQuery, tableName))) {
            // table created
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("JSON-02: metadata-free mode resolves a remapped table with no fetched metadata and lands every column value")
    void metadataFreeModeResolvesRemappedTableAndLandsEveryColumnValue(TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        String topic = createTopicName("mf_topic");
        String tableName = createTopicName("mf_mapped_table");
        props.put(ClickHouseSinkConfig.INSERT_FORMAT, "JSON");
        props.put(ClickHouseSinkConfig.TABLE_MAPPING, topic + "=" + tableName);
        props.put("name", uniqueConnectorName(testInfo));

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        ClickHouseTestHelpers.dropTable(chc, tableName);

        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        try {
            assertDoesNotThrow(() -> sinkTask.start(props));

            createTable(chc, tableName,
                    "CREATE TABLE %s (id UInt32, str String, n Int32) ENGINE = MergeTree ORDER BY id");

            Map<String, Object> value1 = new LinkedHashMap<>();
            value1.put("id", 1);
            value1.put("str", "alpha");
            value1.put("n", 10);

            Map<String, Object> value2 = new LinkedHashMap<>();
            value2.put("id", 2);
            value2.put("str", "beta");
            value2.put("n", 20);

            Map<String, Object> value3 = new LinkedHashMap<>();
            value3.put("id", 3);
            value3.put("str", "gamma");
            value3.put("n", 30);

            List<SinkRecord> records = List.of(
                    new SinkRecord(topic, 0, null, null, null, value1, 1),
                    new SinkRecord(topic, 0, null, null, null, value2, 2),
                    new SinkRecord(topic, 0, null, null, null, value3, 3));

            sinkTask.put(records);

            List<Map<String, String>> rows = allRows(chc,
                    String.format("SELECT id, str, n FROM `%s` ORDER BY id", tableName),
                    "id", "str", "n");
            assertEquals(3, rows.size());
            assertEquals("1", rows.get(0).get("id"));
            assertEquals("alpha", rows.get(0).get("str"));
            assertEquals("10", rows.get(0).get("n"));
            assertEquals("2", rows.get(1).get("id"));
            assertEquals("beta", rows.get(1).get("str"));
            assertEquals("20", rows.get(1).get("n"));
            assertEquals("3", rows.get(2).get("id"));
            assertEquals("gamma", rows.get(2).get("str"));
            assertEquals("30", rows.get(2).get("n"));
        } finally {
            sinkTask.stop();
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @Test
    @DisplayName("JSON-01: with insertFormat unset, the connector-side table-existence check still throws for an absent table (metadata-free mode is what skips it)")
    void unsetInsertFormatStillThrowsOnAbsentTable(TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        String decoyTable = createTopicName("json01_decoy");
        String absentTopic = createTopicName("json01_absent");
        props.put("name", uniqueConnectorName(testInfo));

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        ClickHouseTestHelpers.dropTable(chc, decoyTable);
        // A decoy table so updateMapping() finds at least one table and start() returns true;
        // the record below targets a different, never-created table.
        createTable(chc, decoyTable,
                "CREATE TABLE %s (id UInt32) ENGINE = MergeTree ORDER BY id");

        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        try {
            sinkTask.start(props);

            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", 1);
            List<SinkRecord> records = List.of(new SinkRecord(absentTopic, 0, null, null, null, value, 1));

            RuntimeException ex = assertThrows(RuntimeException.class, () -> sinkTask.put(records));
            assertTrue(messageChainContains(ex, absentTopic), "message chain: " + ex);
            assertTrue(messageChainContains(ex, "does not exist"), "message chain: " + ex);
        } finally {
            sinkTask.stop();
            ClickHouseTestHelpers.dropTable(chc, decoyTable);
        }
    }

    @Test
    @DisplayName("JSON-03: metadata-free mode inserts a payload key that is not a table column without throwing, and known columns land with correct values")
    void metadataFreeModeInsertsUnknownFieldWithoutThrowing(TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        String topic = createTopicName("json03_unknown_field");
        props.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        props.put("name", uniqueConnectorName(testInfo));

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        ClickHouseTestHelpers.dropTable(chc, topic);
        createTable(chc, topic,
                "CREATE TABLE %s (id UInt32, str String) ENGINE = MergeTree ORDER BY id");

        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        try {
            sinkTask.start(props);

            Map<String, Object> value1 = new LinkedHashMap<>();
            value1.put("id", 1);
            value1.put("str", "first");
            value1.put("not_a_column", "some_distinctive_value");

            Map<String, Object> value2 = new LinkedHashMap<>();
            value2.put("id", 2);
            value2.put("str", "second");
            value2.put("not_a_column", "another_distinctive_value");

            List<SinkRecord> records = List.of(
                    new SinkRecord(topic, 0, null, null, null, value1, 1),
                    new SinkRecord(topic, 0, null, null, null, value2, 2));

            assertDoesNotThrow(() -> sinkTask.put(records));

            // This test does not assert on the unknown key itself: the connector always sends
            // input_format_skip_unknown_fields=1 (ClickHouseSinkConfig.java:207), so live evidence
            // cannot distinguish the connector stripping the key from the server merely tolerating
            // it. That distinction is MetadataFreeJsonInsertUnitTest's Docker-free assertion (D-08).
            List<Map<String, String>> rows = allRows(chc,
                    String.format("SELECT id, str FROM `%s` ORDER BY id", topic), "id", "str");
            assertEquals(2, rows.size());
            assertEquals("1", rows.get(0).get("id"));
            assertEquals("first", rows.get(0).get("str"));
            assertEquals("2", rows.get(1).get("id"));
            assertEquals("second", rows.get(1).get("str"));
        } finally {
            sinkTask.stop();
            ClickHouseTestHelpers.dropTable(chc, topic);
        }
    }

    @Test
    @DisplayName("JSON-04: a mixed batch of schemaless records and a tombstone inserts exactly the non-null records with correct values")
    void mixedBatchWithTombstoneInsertsOnlyNonNullRecords(TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        String topic = createTopicName("json04_mixed_batch");
        props.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        props.put("name", uniqueConnectorName(testInfo));

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        ClickHouseTestHelpers.dropTable(chc, topic);
        createTable(chc, topic,
                "CREATE TABLE %s (id UInt32, str String) ENGINE = MergeTree ORDER BY id");

        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        try {
            sinkTask.start(props);

            Map<String, Object> value1 = new LinkedHashMap<>();
            value1.put("id", 1);
            value1.put("str", "first");

            Map<String, Object> value3 = new LinkedHashMap<>();
            value3.put("id", 3);
            value3.put("str", "third");

            List<SinkRecord> records = List.of(
                    new SinkRecord(topic, 0, null, null, null, value1, 1L),
                    new SinkRecord(topic, 0, null, null, null, null, 2L),
                    new SinkRecord(topic, 0, null, null, null, value3, 3L));

            assertDoesNotThrow(() -> sinkTask.put(records));

            assertEquals(2, ClickHouseTestHelpers.countRows(chc, topic));
            List<Map<String, String>> rows = allRows(chc,
                    String.format("SELECT id, str FROM `%s` ORDER BY id", topic), "id", "str");
            assertEquals(2, rows.size());
            assertEquals("1", rows.get(0).get("id"));
            assertEquals("first", rows.get(0).get("str"));
            assertEquals("3", rows.get(1).get("id"));
            assertEquals("third", rows.get(1).get("str"));
        } finally {
            sinkTask.stop();
            ClickHouseTestHelpers.dropTable(chc, topic);
        }
    }

    @Test
    @DisplayName("JSON-04: [R-01 note] an all-tombstone batch pins its actual observed outcome rather than a predicted one")
    void allTombstoneBatchPinsObservedOutcome(TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        String topic = createTopicName("json04_all_tombstone");
        props.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        props.put("name", uniqueConnectorName(testInfo));

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        ClickHouseTestHelpers.dropTable(chc, topic);
        createTable(chc, topic,
                "CREATE TABLE %s (id UInt32, str String) ENGINE = MergeTree ORDER BY id");

        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        try {
            sinkTask.start(props);

            List<SinkRecord> records = List.of(
                    new SinkRecord(topic, 0, null, null, null, null, 1L),
                    new SinkRecord(topic, 0, null, null, null, null, 2L),
                    new SinkRecord(topic, 0, null, null, null, null, 3L));

            // Observed outcome (live run against ClickHouse latest, this session): put() does not
            // throw and an empty JSONEachRow body (produced when every record in the batch is a
            // tombstone) is accepted by the server as a clean zero-row insert. This matches the
            // research session's inference (04-RESEARCH.md Pitfall 3 / Assumption A1), now
            // empirically confirmed rather than assumed, per D-09.
            assertDoesNotThrow(() -> sinkTask.put(records));
            assertEquals(0, ClickHouseTestHelpers.countRows(chc, topic));
            List<Map<String, String>> rows = allRows(chc,
                    String.format("SELECT id, str FROM `%s`", topic), "id", "str");
            assertEquals(0, rows.size());
        } finally {
            sinkTask.stop();
            ClickHouseTestHelpers.dropTable(chc, topic);
        }
    }

    /**
     * R-01 PIN - NOT AN ENDORSEMENT.
     *
     * <p>This test records CONFIRMED SILENT DATA LOSS for {@code insertFormat=json}
     * (metadata-free mode) combined with a {@code Struct} (schema'd) record and {@code
     * bypassRowBinary} left at its default ({@code false}). {@code doInsert} routes {@code
     * SCHEMA} records to {@code doInsertRawBinaryV2}, whose per-column write loop iterates
     * {@code table.getRootColumnsList()} - which is empty for the synthetic, column-less {@code
     * Table} that metadata-free mode builds - so every record silently serializes to zero bytes.
     * {@code put()} returns normally, no exception is thrown anywhere in the call chain, and 0 of
     * the batch's records land. This was empirically confirmed both in 04-RESEARCH.md's session
     * (client's own {@code InsertResponse} reported {@code Written Rows: [0]} for a 5-record
     * batch, server {@code 26.8.6.5}) and re-confirmed by this test.
     *
     * <p>This test was added at the 04-01 Task 4 blocking-human checkpoint (the R-01 escalation)
     * at the user's EXPLICIT direction (the answer was {@code pin-current-behavior}), specifically
     * so a future change to this behavior does not go unnoticed by the suite. A fail-fast throw
     * for this combination is deliberately NOT in scope here: 04-CONTEXT.md D-01 and D-07 do not
     * pre-approve it, because it would break - loudly, at startup of the next batch - any
     * production pipeline currently running {@code insertFormat=json} with a Struct converter.
     * That pipeline is losing data today but is not failing today; turning this into a loud error
     * is a separate decision that needs its own plan, not something this test or this plan
     * performs.
     */
    @Test
    @DisplayName(
            "JSON-01: [R-01 pin] insertFormat=json + a Struct record + bypassRowBinary left unset silently drops the whole batch - confirmed data loss, not fixed in this phase (see checkpoint)")
    void explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch(TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        String topic = createTopicName("json01_r01_struct_pin");
        props.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        // bypassRowBinary is intentionally left UNSET (defaults to false) - this is exactly the
        // combination R-01 probed and the user directed be pinned at the Task 4 checkpoint.
        props.put("name", uniqueConnectorName(testInfo));

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        ClickHouseTestHelpers.dropTable(chc, topic);
        createTable(chc, topic,
                "CREATE TABLE %s (id UInt32, str String) ENGINE = MergeTree ORDER BY id");

        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        try {
            sinkTask.start(props);

            Schema schema = SchemaBuilder.struct()
                    .field("id", Schema.INT32_SCHEMA)
                    .field("str", Schema.STRING_SCHEMA)
                    .build();
            List<SinkRecord> records = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                Struct value = new Struct(schema).put("id", i).put("str", "row" + i);
                records.add(new SinkRecord(topic, 0, null, null, schema, value, i));
            }

            // Observed outcome (live run against ClickHouse latest, this session, matching
            // 04-RESEARCH.md's R-01 finding against server 26.8.6.5): put() does not throw and
            // every one of the 5 records is silently discarded - 0 rows land. This is ground
            // truth for the current fork behavior, recorded here exactly as observed, not as
            // predicted.
            assertDoesNotThrow(() -> sinkTask.put(records));
            assertEquals(0, ClickHouseTestHelpers.countRows(chc, topic));
        } finally {
            sinkTask.stop();
            ClickHouseTestHelpers.dropTable(chc, topic);
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
