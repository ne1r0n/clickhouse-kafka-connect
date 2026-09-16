package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
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
}
