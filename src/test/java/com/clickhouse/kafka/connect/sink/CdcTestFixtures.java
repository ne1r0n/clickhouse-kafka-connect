package com.clickhouse.kafka.connect.sink;

import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.sink.SinkRecord;

/**
 * Debezium envelope fixtures, canonical batches and the snapshot read-back, shared by the JSONP-09
 * live and Docker-free tests. {@code ExplicitJsonInsertPathLiveTest} reaches them through static
 * imports.
 */
public final class CdcTestFixtures {

    private CdcTestFixtures() {}

    public static final Schema CDC_ROW_SCHEMA = SchemaBuilder.struct().optional()
            .field("id", Schema.INT32_SCHEMA)
            .field("ts", Timestamp.SCHEMA)
            .field("amount", Decimal.schema(4))
            .field("name", Schema.STRING_SCHEMA)
            .build();

    public static final Schema CDC_SOURCE_SCHEMA = SchemaBuilder.struct().optional()
            .field("lsn", Schema.OPTIONAL_INT64_SCHEMA)
            .build();

    public static final Schema CDC_ENVELOPE_SCHEMA = SchemaBuilder.struct().name("server.db.orders.Envelope")
            .field("op", Schema.STRING_SCHEMA)
            .field("before", CDC_ROW_SCHEMA)
            .field("after", CDC_ROW_SCHEMA)
            .field("source", CDC_SOURCE_SCHEMA)
            .build();

    public static Struct cdcRow(int id, String name) {
        return new Struct(CDC_ROW_SCHEMA)
                .put("id", id)
                .put("ts", new Date(1_704_067_200_123L))
                .put("amount", new BigDecimal("12.3456"))
                .put("name", name);
    }

    public static Struct cdcEnvelope(String op, Struct before, Struct after, long lsn) {
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

    /** Wraps the envelopes in sink records of one topic partition with consecutive offsets. */
    public static List<SinkRecord> cdcSinkRecords(
            String topic, int partition, long firstOffset, Struct... envelopes) {
        List<SinkRecord> records = new ArrayList<>(envelopes.length);
        for (int i = 0; i < envelopes.length; i++) {
            records.add(new SinkRecord(
                    topic, partition, null, null, CDC_ENVELOPE_SCHEMA, envelopes[i], firstOffset + i));
        }
        return records;
    }

    /** Two real rows (a create and a delete) and two records that carry no values (op=t, op=u). */
    public static Struct[] mixedBatchEnvelopes() {
        return new Struct[] {
            cdcEnvelope("c", null, cdcRow(1, "created"), 100L),
            cdcEnvelope("t", null, null, 110L),
            cdcEnvelope("u", null, null, 120L),
            cdcEnvelope("d", cdcRow(2, "deleted"), null, 101L),
        };
    }

    /** A batch made only of records that carry no values. */
    public static Struct[] allEmptyEnvelopes() {
        return new Struct[] {
            cdcEnvelope("t", null, null, 130L),
            cdcEnvelope("u", null, null, 131L),
            cdcEnvelope("d", null, null, 132L),
        };
    }

    /** An empty record followed by a real row. */
    public static Struct[] emptyFirstEnvelopes() {
        return new Struct[] {
            cdcEnvelope("t", null, null, 140L),
            cdcEnvelope("c", null, cdcRow(3, "created-3"), 141L),
        };
    }

    /**
     * Snapshots each row's requested columns into a plain {@code Map<String, String>} while the
     * row is still the iterator's current element. The client-v2 {@link GenericRecord} returned
     * by {@link Records}'s iterator is a mutable, reused view over the underlying binary reader
     * buffer - collecting the {@code GenericRecord} references themselves (rather than reading
     * their values immediately) leaves every collected reference pointing at whatever row the
     * iterator most recently advanced to, silently aliasing every "row" to the last one read.
     */
    public static List<Map<String, String>> allRows(ClickHouseHelperClient chc, String sql, String... columns)
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
