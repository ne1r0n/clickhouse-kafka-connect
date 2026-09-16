package transforms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.sink.SinkRecord;

/**
 * Shared, fork-only record builders and assertions for the SMT unit tests in this package (D-12).
 * <p>
 * Package-private and Docker-free: it extends nothing, carries no JUnit tag, and every test that
 * uses it stays inside the {@code testUnit} task ({@code build.gradle.kts}'s
 * {@code excludeTags("clickhouse")}).
 */
final class TransformTestHelpers {

    private static final int PARTITION = 0;
    private static final long TIMESTAMP = 1715376000123L;
    private static final int DATE_DATE = 1715376000;

    private TransformTestHelpers() {
        // static helpers only
    }

    /** Generalizes the duplicated {@code generateSampleRecord} from HeadersToValueTest / HeaderToValueTest. */
    static SinkRecord schemaRecord(String topic, long offset, ConnectHeaders headers) {
        Schema schema =
                SchemaBuilder.struct()
                        .field("off16", Schema.INT16_SCHEMA)
                        .field("timestamp_int64", Schema.INT64_SCHEMA)
                        .field("date_date", Schema.INT32_SCHEMA)
                        .field("str", Schema.STRING_SCHEMA)
                        .build();
        Struct value =
                new Struct(schema)
                        .put("off16", (short) offset)
                        .put("timestamp_int64", TIMESTAMP)
                        .put("date_date", DATE_DATE)
                        .put("str", "row-" + offset);
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

    /** Same metadata as {@link #schemaRecord}, but a deliberately different value schema — proves the D-02 stale-schema guard. */
    static SinkRecord schemaRecordAlternateSchema(String topic, long offset, ConnectHeaders headers) {
        Schema schema =
                SchemaBuilder.struct()
                        .field("off16", Schema.INT16_SCHEMA)
                        .field("extra_str", Schema.STRING_SCHEMA)
                        .build();
        Struct value =
                new Struct(schema).put("off16", (short) offset).put("extra_str", "extra-" + offset);
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

    /** Schemaless (Map-valued) equivalent of {@link #schemaRecord}, same keys and values. */
    static SinkRecord schemalessRecord(String topic, long offset, ConnectHeaders headers) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("off16", (short) offset);
        value.put("timestamp_int64", TIMESTAMP);
        value.put("date_date", DATE_DATE);
        value.put("str", "row-" + offset);
        return new SinkRecord(
                topic,
                PARTITION,
                Schema.STRING_SCHEMA,
                "key-" + offset,
                null,
                value,
                offset,
                TIMESTAMP,
                TimestampType.CREATE_TIME,
                headers);
    }

    /** Null value and null value schema, per HDRS-01. */
    static SinkRecord tombstone(String topic, long offset) {
        return new SinkRecord(
                topic,
                PARTITION,
                Schema.STRING_SCHEMA,
                "key-" + offset,
                null,
                null,
                offset,
                TIMESTAMP,
                TimestampType.CREATE_TIME,
                new ConnectHeaders());
    }

    /** The Debezium envelope shape {@code TableOperationFilter} is written against. */
    static SinkRecord debeziumRecord(String topic, long offset, String op, String db, String table) {
        Schema sourceSchema =
                SchemaBuilder.struct()
                        .field("db", Schema.STRING_SCHEMA)
                        .field("table", Schema.STRING_SCHEMA)
                        .build();
        Struct source = new Struct(sourceSchema).put("db", db).put("table", table);

        Schema recordSchema =
                SchemaBuilder.struct()
                        .field("id", Schema.INT32_SCHEMA)
                        .field("op", Schema.STRING_SCHEMA)
                        .field("source", sourceSchema)
                        .build();
        Struct value = new Struct(recordSchema).put("id", (int) offset).put("op", op).put("source", source);

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

    /** Same envelope as {@link #debeziumRecord}, as a nested {@code Map} with a null value schema, for TOF-04. */
    static SinkRecord schemalessDebeziumRecord(String topic, long offset, String op, String db, String table) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("db", db);
        source.put("table", table);

        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", (int) offset);
        value.put("op", op);
        value.put("source", source);

        return new SinkRecord(
                topic,
                PARTITION,
                Schema.STRING_SCHEMA,
                "key-" + offset,
                null,
                value,
                offset,
                TIMESTAMP,
                TimestampType.CREATE_TIME,
                new ConnectHeaders());
    }

    /** The HDRS-06 / HDR-05 / CONV-04 metadata-preservation assertion, written once. */
    static void assertMetadataPreserved(SinkRecord in, SinkRecord out) {
        assertEquals(in.topic(), out.topic());
        assertEquals(in.kafkaPartition(), out.kafkaPartition());
        assertEquals(in.key(), out.key());
        assertEquals(in.keySchema(), out.keySchema());
        assertEquals(in.timestamp(), out.timestamp());
        assertEquals(in.headers(), out.headers());
    }
}
