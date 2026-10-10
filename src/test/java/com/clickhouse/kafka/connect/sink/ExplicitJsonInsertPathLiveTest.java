package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Timestamp;
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
 * test class. Most records built in this class are schemaless {@code Map} values. {@code Struct}
 * records now land through the JSON writer under {@code insertFormat=json} as well (JSONP-05,
 * formerly the R-01 silent-loss pin); the dedicated test for that route sits at the end of the
 * class.
 *
 * <p>Re-enabled per the JSONP-01 KEEP decision (Phase 9, D-01/D-10): the metadata-free {@code
 * insertFormat=json} bypass this class exercises was re-applied to {@code src/main} in Phase 9;
 * every assertion below is unchanged from the parked version.
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
     * Added at the 04-01 Task 4 checkpoint to pin R-01's silent loss ({@code insertFormat=json} with
     * a {@code Struct} record routed to RowBinary against a column-less synthetic table, so every
     * record serialized to zero bytes). Flipped in Phase 13: R-01 is fixed by routing, per the
     * milestone's locked decision.
     */
    @Test
    @DisplayName(
            "JSONP-05: insertFormat=json + a Struct record + bypassRowBinary left unset lands every record through the JSON writer (formerly the R-01 pin)")
    void explicitJsonStructRecordWithDefaultBypassRowBinaryLandsEveryRecord(TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        String topic = createTopicName("json01_r01_struct_pin");
        props.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        // bypassRowBinary is intentionally left UNSET (defaults to false): the combination R-01
        // used to lose silently.
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
            List<String> strings = List.of("row1", "row2", "row3", "row4", "Gr\u00fc\u00dfe \u2713");
            List<SinkRecord> records = new ArrayList<>();
            for (int i = 1; i <= strings.size(); i++) {
                Struct value = new Struct(schema).put("id", i).put("str", strings.get(i - 1));
                records.add(new SinkRecord(topic, 0, null, null, schema, value, i));
            }

            assertDoesNotThrow(() -> sinkTask.put(records));

            // hex() keeps the comparison byte-exact under the test JVM's windows-1252 default.
            List<Map<String, String>> rows = allRows(chc,
                    String.format("SELECT id, hex(str) AS str_hex FROM `%s` ORDER BY id", topic),
                    "id", "str_hex");
            assertEquals(strings.size(), rows.size());
            for (int i = 0; i < strings.size(); i++) {
                assertEquals(String.valueOf(i + 1), rows.get(i).get("id"));
                assertEquals(
                        HexFormat.of().withUpperCase().formatHex(strings.get(i).getBytes(StandardCharsets.UTF_8)),
                        rows.get(i).get("str_hex"));
            }
        } finally {
            sinkTask.stop();
            ClickHouseTestHelpers.dropTable(chc, topic);
        }
    }

    private static final Schema CDC_ROW_SCHEMA = SchemaBuilder.struct().optional()
            .field("id", Schema.INT32_SCHEMA)
            .field("ts", Timestamp.SCHEMA)
            .field("amount", Decimal.schema(4))
            .field("name", Schema.STRING_SCHEMA)
            .build();

    private static final Schema CDC_SOURCE_SCHEMA = SchemaBuilder.struct().optional()
            .field("lsn", Schema.OPTIONAL_INT64_SCHEMA)
            .build();

    private static final Schema CDC_ENVELOPE_SCHEMA = SchemaBuilder.struct().name("server.db.orders.Envelope")
            .field("op", Schema.STRING_SCHEMA)
            .field("before", CDC_ROW_SCHEMA)
            .field("after", CDC_ROW_SCHEMA)
            .field("source", CDC_SOURCE_SCHEMA)
            .build();

    private static Struct cdcRow(int id, String name) {
        return new Struct(CDC_ROW_SCHEMA)
                .put("id", id)
                .put("ts", new Date(1_704_067_200_123L))
                .put("amount", new BigDecimal("12.3456"))
                .put("name", name);
    }

    private static Struct cdcEnvelope(String op, Struct before, Struct after, long lsn) {
        Struct envelope = new Struct(CDC_ENVELOPE_SCHEMA)
                .put("op", op)
                .put("source", new Struct(CDC_SOURCE_SCHEMA).put("lsn", lsn));
        if (before != null) {
            envelope.put("before", before);
        }
        if (after != null) {
            envelope.put("after", after);
        }
        return envelope;
    }

    @Test
    @DisplayName(
            "JSONP-05/D-19: a Debezium CDC batch on the metadata-free route lands typed rows and skips records that carry no values")
    void metadataFreeRouteLandsCdcRecordsAndSkipsEmptyOnes(TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        String topic = createTopicName("jsonp05_cdc_route");
        props.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        props.put(ClickHouseSinkConfig.DEBEZIUM_CDC_ENABLED, "true");
        props.put("name", uniqueConnectorName(testInfo));

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        ClickHouseTestHelpers.dropTable(chc, topic);
        // The UTC time zone on ts keeps toString(ts) independent of the server's default zone.
        createTable(chc, topic,
                "CREATE TABLE %s (id Int32, ts DateTime64(3, 'UTC'), amount Decimal(18,4), name String, "
                        + "_version UInt64, is_deleted UInt8) ENGINE = MergeTree ORDER BY id");

        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        try {
            sinkTask.start(props);

            // op=t and op=u with a null after struct convert to an empty value map: without D-19
            // each would land as an all-defaults row with id 0.
            List<SinkRecord> records = new ArrayList<>();
            Struct[] envelopes = {
                cdcEnvelope("c", null, cdcRow(1, "created"), 100L),
                cdcEnvelope("t", null, null, 110L),
                cdcEnvelope("u", null, null, 120L),
                cdcEnvelope("d", cdcRow(2, "deleted"), null, 101L),
            };
            for (int i = 0; i < envelopes.length; i++) {
                records.add(new SinkRecord(topic, 0, null, null, CDC_ENVELOPE_SCHEMA, envelopes[i], i + 1));
            }

            assertDoesNotThrow(() -> sinkTask.put(records));

            List<Map<String, String>> rows = allRows(chc,
                    String.format(
                            "SELECT id, toString(ts) AS ts, toString(amount) AS amount, name, "
                                    + "toString(_version) AS v, toString(is_deleted) AS del FROM `%s` ORDER BY id",
                            topic),
                    "id", "ts", "amount", "name", "v", "del");
            assertEquals(2, rows.size());

            Map<String, String> created = rows.get(0);
            assertEquals("1", created.get("id"));
            assertEquals("2024-01-01 00:00:00.123", created.get("ts"));
            assertEquals("12.3456", created.get("amount"));
            assertEquals("created", created.get("name"));
            assertEquals("100", created.get("v"));
            assertEquals("0", created.get("del"));

            Map<String, String> deleted = rows.get(1);
            assertEquals("2", deleted.get("id"));
            assertEquals("2024-01-01 00:00:00.123", deleted.get("ts"));
            assertEquals("12.3456", deleted.get("amount"));
            assertEquals("deleted", deleted.get("name"));
            assertEquals("101", deleted.get("v"));
            assertEquals("1", deleted.get("del"));
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
