package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.transforms.ConvertToMapOrJson;
import com.clickhouse.kafka.connect.transforms.HeaderToValue;
import com.clickhouse.kafka.connect.transforms.HeadersToValue;
import com.clickhouse.kafka.connect.transforms.TableOperationFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.transforms.Transformation;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Sink-level (ClickHouse {@code latest}, client V2) proof that each of the four fork SMTs -
 * {@code TableOperationFilter}, {@code HeadersToValue}, {@code HeaderToValue} and
 * {@code ConvertToMapOrJson} - puts transformed records where they belong once a real
 * {@link ClickHouseSinkTask} drives them into the database. Covers TOF-06, HDRS-09, HDR-07 and
 * CONV-08. {@code @Tag("clickhouse")} is inherited from {@link ClickHouseBase}, which is what
 * keeps this class out of the Docker-free {@code testUnit} task.
 */
public class SmtSinkLandingTest extends ClickHouseBase {

    private static final int PARTITION = 0;
    private static final long TIMESTAMP = 1715376000123L;

    /**
     * Cyrillic + CJK content built only from escaped-unicode literals (D-14/Pitfall 2, so this
     * file's own encoding is never the thing under test): spells "Privet Zhongwen" (Russian
     * greeting, space, "Chinese language" in Chinese). Read back only through {@link
     * ClickHouseTestHelpers#getAllRowsAsJson}, which decodes explicit UTF-8.
     */
    private static final String NON_ASCII_SAMPLE = "\u041f\u0440\u0438\u0432\u0435\u0442 \u4e2d\u6587";

    /**
     * Mirrors what the Kafka Connect worker does with a configured transform: apply it to every
     * record and keep only the non-null results, because a null return means the SMT filtered
     * that record out of the stream (D-05).
     */
    private Collection<SinkRecord> transform(Transformation<SinkRecord> xform, Collection<SinkRecord> records) {
        List<SinkRecord> out = new ArrayList<>(records.size());
        for (SinkRecord r : records) {
            SinkRecord t = xform.apply(r);
            if (t != null) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * MBean-safe start/put/stop: every test in this class goes through this method so an
     * assertion failure can never leak an MBean into a later test.
     */
    private void runSink(Map<String, String> props, Collection<SinkRecord> records, TestInfo testInfo) {
        props.put("name", uniqueConnectorName(testInfo));
        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        try {
            sinkTask.start(props);
            sinkTask.put(records);
        } finally {
            sinkTask.stop();
        }
    }

    /** The Debezium envelope shape {@code TableOperationFilter} is written against. */
    private SinkRecord debeziumRecord(String topic, long offset, String op, String db, String table) {
        Schema sourceSchema = SchemaBuilder.struct()
                .field("db", Schema.STRING_SCHEMA)
                .field("table", Schema.STRING_SCHEMA)
                .build();
        Struct source = new Struct(sourceSchema).put("db", db).put("table", table);

        Schema recordSchema = SchemaBuilder.struct()
                .field("id", Schema.INT32_SCHEMA)
                .field("op", Schema.STRING_SCHEMA)
                .field("source", sourceSchema)
                .build();
        Struct value = new Struct(recordSchema)
                .put("id", (int) offset)
                .put("op", op)
                .put("source", source);

        return new SinkRecord(
                topic,
                PARTITION,
                Schema.STRING_SCHEMA,
                "key-" + offset,
                recordSchema,
                value,
                offset,
                TIMESTAMP,
                TimestampType.CREATE_TIME,
                new ConnectHeaders());
    }

    /** A simple two-field Struct fixture (off16, str) carrying the given headers. */
    private SinkRecord headerFixtureRecord(String topic, long offset, ConnectHeaders headers) {
        Schema schema = SchemaBuilder.struct()
                .field("off16", Schema.INT16_SCHEMA)
                .field("str", Schema.STRING_SCHEMA)
                .build();
        Struct value = new Struct(schema).put("off16", (short) offset).put("str", "row-" + offset);

        return new SinkRecord(
                topic,
                PARTITION,
                Schema.STRING_SCHEMA,
                "key-" + offset,
                schema,
                value,
                offset,
                TIMESTAMP,
                TimestampType.CREATE_TIME,
                headers);
    }

    /** Indexes rows returned by {@link ClickHouseTestHelpers#getAllRowsAsJson} by their off16 value. */
    private Map<Integer, JSONObject> indexByOff16(List<JSONObject> rows) {
        Map<Integer, JSONObject> byOffset = new HashMap<>();
        for (JSONObject row : rows) {
            byOffset.put(row.getInt("off16"), row);
        }
        return byOffset;
    }

    /**
     * A four-field Struct fixture (off16, str, p_int32, nested) for CONV-08, where {@code nested}
     * is itself a Struct with optional {@code n_str}/{@code n_int} fields. Passing {@code null} for
     * both nested values models the empty-edge row (all-null nested fields, dropped by {@code
     * preserve.null.values=false} into an empty JSON object rather than a missing nested value).
     */
    private SinkRecord jsonFixtureRecord(
            String topic, long offset, String strVal, int pInt32, String nStr, Integer nInt) {
        Schema nestedSchema = SchemaBuilder.struct()
                .field("n_str", Schema.OPTIONAL_STRING_SCHEMA)
                .field("n_int", Schema.OPTIONAL_INT32_SCHEMA)
                .optional()
                .build();
        Schema schema = SchemaBuilder.struct()
                .field("off16", Schema.INT16_SCHEMA)
                .field("str", Schema.STRING_SCHEMA)
                .field("p_int32", Schema.INT32_SCHEMA)
                .field("nested", nestedSchema)
                .build();
        Struct nested = new Struct(nestedSchema).put("n_str", nStr).put("n_int", nInt);
        Struct value = new Struct(schema)
                .put("off16", (short) offset)
                .put("str", strVal)
                .put("p_int32", pInt32)
                .put("nested", nested);

        return new SinkRecord(
                topic,
                PARTITION,
                Schema.STRING_SCHEMA,
                "key-" + offset,
                schema,
                value,
                offset,
                TIMESTAMP,
                TimestampType.CREATE_TIME,
                new ConnectHeaders());
    }

    /** Builds a configured {@link ConvertToMapOrJson} for CONV-08: JSON output via Jackson, nulls dropped. */
    private ConvertToMapOrJson<SinkRecord> newConvertToJsonTransform() {
        ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
        Map<String, Object> config = new HashMap<>();
        config.put("output.json.enabled", true);
        config.put("output.json.library", "jackson");
        config.put("preserve.null.values", false);
        xform.configure(config);
        return xform;
    }

    @Test
    @DisplayName("HDRS-09: headers land in a Map(String, Nullable(String)) column, including the empty and non-ASCII header edges")
    void hdrs09HeadersLandInMapColumn(TestInfo testInfo) {
        Map<String, String> props = createProps();
        ClickHouseHelperClient chc = createClient(props);
        String table = createTopicName("hdrs09_landing");

        ClickHouseTestHelpers.dropTable(chc, table);
        ClickHouseTestHelpers.createTable(
                chc,
                table,
                "CREATE TABLE %s (`off16` Int16, `str` String, `headers_map` Map(String, Nullable(String))) ENGINE = MergeTree ORDER BY off16");

        ConnectHeaders headers1 = new ConnectHeaders();
        headers1.addString("h_a", "alpha");
        headers1.addString("h_b", "beta");

        ConnectHeaders headers2 = new ConnectHeaders(); // empty edge: zero headers

        // No header with a null value in this fixture: writing a NULL into a
        // Map(String, Nullable(String)) exercises ClickHouseWriter's MAP branch, which is
        // outside transforms/ and therefore outside this phase's inline-fix scope; the
        // null-map-entry contract is already pinned at unit level by HDRS-03.
        ConnectHeaders headers3 = new ConnectHeaders();
        headers3.addString("h_empty", "");
        headers3.addString("h_uni", NON_ASCII_SAMPLE);

        List<SinkRecord> records = new ArrayList<>();
        records.add(headerFixtureRecord(table, 1L, headers1));
        records.add(headerFixtureRecord(table, 2L, headers2));
        records.add(headerFixtureRecord(table, 3L, headers3));

        Collection<SinkRecord> transformed;
        try (HeadersToValue<SinkRecord> xform = new HeadersToValue<>()) {
            Map<String, Object> config = new HashMap<>();
            config.put("field", "headers_map");
            xform.configure(config);
            transformed = transform(xform, records);
        }

        runSink(props, transformed, testInfo);

        assertEquals(3, ClickHouseTestHelpers.countRows(chc, table));

        Map<Integer, JSONObject> byOffset = indexByOff16(ClickHouseTestHelpers.getAllRowsAsJson(chc, table));

        JSONObject map1 = byOffset.get(1).getJSONObject("headers_map");
        assertEquals("alpha", map1.getString("h_a"));
        assertEquals("beta", map1.getString("h_b"));

        assertEquals(0, byOffset.get(2).getJSONObject("headers_map").length());

        JSONObject map3 = byOffset.get(3).getJSONObject("headers_map");
        assertEquals("", map3.getString("h_empty"));
        assertEquals(NON_ASCII_SAMPLE, map3.getString("h_uni"));
    }

    @Test
    @DisplayName("HDR-07: a header-derived field lands in its String column")
    void hdr07HeaderDerivedFieldLandsInColumn(TestInfo testInfo) {
        Map<String, String> props = createProps();
        ClickHouseHelperClient chc = createClient(props);
        String table = createTopicName("hdr07_landing");

        ClickHouseTestHelpers.dropTable(chc, table);
        ClickHouseTestHelpers.createTable(
                chc,
                table,
                "CREATE TABLE %s (`off16` Int16, `str` String, `trace_id` String) ENGINE = MergeTree ORDER BY off16");

        ConnectHeaders headers1 = new ConnectHeaders();
        headers1.addString("trace-id", "trace-aaa");
        ConnectHeaders headers2 = new ConnectHeaders();
        headers2.addString("trace-id", "trace-bbb");

        List<SinkRecord> records = new ArrayList<>();
        records.add(headerFixtureRecord(table, 1L, headers1));
        records.add(headerFixtureRecord(table, 2L, headers2));

        Collection<SinkRecord> transformed;
        try (HeaderToValue<SinkRecord> xform = new HeaderToValue<>()) {
            Map<String, Object> config = new HashMap<>();
            config.put("fields", "trace_id");
            config.put("headers", "trace-id");
            xform.configure(config);
            transformed = transform(xform, records);
        }

        runSink(props, transformed, testInfo);

        assertEquals(2, ClickHouseTestHelpers.countRows(chc, table));

        Map<Integer, JSONObject> byOffset = indexByOff16(ClickHouseTestHelpers.getAllRowsAsJson(chc, table));
        assertEquals("trace-aaa", byOffset.get(1).getString("trace_id"));
        assertEquals("row-1", byOffset.get(1).getString("str"));
        assertEquals("trace-bbb", byOffset.get(2).getString("trace_id"));
        assertEquals("row-2", byOffset.get(2).getString("str"));
    }

    @Test
    @DisplayName("TOF-06: filtered Debezium operations never reach ClickHouse; unfiltered ones land, including the nested Tuple column")
    void tof06FilteredOperationsNeverLandUnfilteredDo(TestInfo testInfo) {
        Map<String, String> props = createProps();
        ClickHouseHelperClient chc = createClient(props);
        String table = createTopicName("tof06_landing");

        ClickHouseTestHelpers.dropTable(chc, table);
        ClickHouseTestHelpers.createTable(
                chc,
                table,
                "CREATE TABLE %s (`id` Int32, `op` String, `source` Tuple(db String, `table` String)) ENGINE = MergeTree ORDER BY id");

        List<SinkRecord> records = new ArrayList<>();
        records.add(debeziumRecord(table, 1L, "c", "mydb", "mytable"));
        records.add(debeziumRecord(table, 2L, "u", "mydb", "mytable"));
        records.add(debeziumRecord(table, 3L, "d", "mydb", "mytable"));
        records.add(debeziumRecord(table, 4L, "t", "mydb", "mytable"));
        records.add(debeziumRecord(table, 5L, "d", "otherdb", "othertable"));
        records.add(debeziumRecord(table, 6L, "r", "mydb", "mytable"));

        Collection<SinkRecord> transformed;
        try (TableOperationFilter<SinkRecord> filter = new TableOperationFilter<>()) {
            Map<String, String> filterConfig = new HashMap<>();
            filterConfig.put("tables", "mydb.mytable");
            filterConfig.put("skipped.operations", "d,t");
            filter.configure(filterConfig);
            transformed = transform(filter, records);
        }
        assertEquals(4, transformed.size());

        runSink(props, transformed, testInfo);

        assertEquals(4, ClickHouseTestHelpers.countRows(chc, table));

        TreeSet<Integer> ids = new TreeSet<>();
        for (JSONObject row : ClickHouseTestHelpers.getAllRowsAsJson(chc, table)) {
            ids.add(row.getInt("id"));
        }
        assertEquals(new TreeSet<>(List.of(1, 2, 5, 6)), ids);

        long sourceDbCount = chc.getClient()
                .queryAll(String.format("SELECT toInt64(count()) AS c FROM `%s` WHERE source.db = 'mydb'", table))
                .get(0)
                .getLong("c");
        assertEquals(3L, sourceDbCount);
    }

    @Test
    @DisplayName("CONV-08: JSON-string output lands across typed columns and a JSON column")
    void conv08JsonStringOutputLandsAcrossColumns(TestInfo testInfo) {
        Map<String, String> props = createProps();
        ClickHouseHelperClient chc = createClient(props);
        String table = createTopicName("conv08_landing");

        ClickHouseTestHelpers.dropTable(chc, table);
        ClickHouseTestHelpers.createTable(
                chc,
                table,
                "CREATE TABLE %s (`off16` Int16, `str` String, `p_int32` Int32, `nested` JSON) ENGINE = MergeTree ORDER BY off16");

        List<SinkRecord> records = new ArrayList<>();
        records.add(jsonFixtureRecord(table, 1L, "row-1", 100, "nested-1", 7));
        // Empty edge: preserve.null.values=false + an all-null nested Struct must land `{}`, not
        // NULL and not a missing row.
        records.add(jsonFixtureRecord(table, 2L, "row-2", 200, null, null));
        // Encoding edge: escaped-unicode content in both a top-level and a nested field.
        records.add(jsonFixtureRecord(table, 3L, NON_ASCII_SAMPLE, 300, NON_ASCII_SAMPLE, 9));

        Collection<SinkRecord> transformed;
        try (ConvertToMapOrJson<SinkRecord> xform = newConvertToJsonTransform()) {
            transformed = transform(xform, records);
        }

        runSink(props, transformed, testInfo);

        assertEquals(3, ClickHouseTestHelpers.countRows(chc, table));

        Map<Integer, JSONObject> byOffset = indexByOff16(ClickHouseTestHelpers.getAllRowsAsJson(chc, table));

        JSONObject row1 = byOffset.get(1);
        assertEquals("row-1", row1.getString("str"));
        assertEquals(100, row1.getInt("p_int32"));
        JSONObject nested1 = row1.getJSONObject("nested");
        assertEquals("nested-1", nested1.getString("n_str"));
        assertEquals(7, nested1.getInt("n_int"));

        JSONObject row2 = byOffset.get(2);
        assertEquals(0, row2.getJSONObject("nested").length());

        JSONObject row3 = byOffset.get(3);
        assertEquals(NON_ASCII_SAMPLE, row3.getString("str"));
        assertEquals(NON_ASCII_SAMPLE, row3.getJSONObject("nested").getString("n_str"));
    }

    @Test
    @DisplayName("CONV-08: a single-record batch lands exactly one row")
    void conv08SingleRecordBatchLandsOneRow(TestInfo testInfo) {
        Map<String, String> props = createProps();
        ClickHouseHelperClient chc = createClient(props);
        String table = createTopicName("conv08_single");

        ClickHouseTestHelpers.dropTable(chc, table);
        ClickHouseTestHelpers.createTable(
                chc,
                table,
                "CREATE TABLE %s (`off16` Int16, `str` String, `p_int32` Int32, `nested` JSON) ENGINE = MergeTree ORDER BY off16");

        List<SinkRecord> records = new ArrayList<>();
        records.add(jsonFixtureRecord(table, 1L, "solo", 42, "solo-nested", 1));

        Collection<SinkRecord> transformed;
        try (ConvertToMapOrJson<SinkRecord> xform = newConvertToJsonTransform()) {
            transformed = transform(xform, records);
        }

        runSink(props, transformed, testInfo);

        assertEquals(1, ClickHouseTestHelpers.countRows(chc, table));
    }
}
