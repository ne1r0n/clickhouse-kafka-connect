package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.helper.RowSnapshots;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Time;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Live RowBinary-versus-JSON parity for the metadata-free JSON route (JSONP-06) and the pins for
 * its null semantics and option interactions (JSONP-07). New fork-only class: it does not extend or
 * modify any upstream test.
 *
 * <p>Method: two tables with identical DDL ("twin tables") receive the same {@code Struct} records,
 * one through a sink task with {@code insertFormat=json} (the metadata-free route) and one through a
 * default RowBinary task. Both tables are then read back with the same typed SELECT and compared
 * column by column. RowBinary is the reference (D-01), so the comparison carries no hand-written
 * expected values; literal anchors sit on the reference side only, and are checked before the
 * cross-route comparison so the equality cannot pass vacuously (both tables empty, or both
 * defaulted). No ClickHouse insert setting is forced on either route (D-02).
 *
 * <p>Byte cells are compared through {@code hex(...)} and time cells through {@code toString(...)}
 * or an epoch function, never through a Java String decoded with the JVM default charset (the test
 * JVM runs windows-1252).
 */
public class MetadataFreeJsonTypeParityLiveTest extends ClickHouseBase {

    private static final String JSON_SUFFIX = "_json";
    private static final String ROWBINARY_SUFFIX = "_rb";

    private static final int MATRIX_RECORD_COUNT = 7;
    private static final int MATRIX_REPEATED_ID = 7;
    private static final int MATRIX_REPEATED_SOURCE_INDEX = 4;
    private static final long MILLIS_PER_DAY = 86_400_000L;

    // Boundary epochs: 0, 1 ms, a 5-digit-seconds value, a 9-digit-seconds value, a 2024 value and
    // year 2100. Index = id - 1; id 7 repeats the index of id 5.
    private static final long[] TIMESTAMP_MS = {
        0L, 1L, 86400123L, 645453296789L, 1704067200123L, 4102444800123L
    };
    private static final long[] DATE_DAYS = {0L, 1L, 19723L, 40000L, 19723L, 1L};
    private static final int[] TIME_MS = {0, 1, 45296789, 86399999, 45296789, 0};

    private static final String[] DEC_18_4 = {
        "12.3456", "0.0000", "-12.3456", "99999999999999.9999", "12.3456", "0.0001"
    };
    private static final String[] DEC_9_0 = {"42", "0", "-42", "999999999", "42", "-999999999"};
    private static final String[] DEC_18_7 = {
        "0.0000001", "0.0000000", "-1234567.1234567", "99999999999.9999999", "1234567.1234567", "0.0000010"
    };
    private static final String[] DEC_38_10 = {
        "12345678901234567890.0123456789",
        "0.0000000000",
        "-12345678901234567890.0123456789",
        "1.0000000001",
        "12345678901234567890.0123456789",
        "-0.0000000001"
    };
    // Kafka scale 6 into a Decimal(18,4) column: the rounding contract is whatever RowBinary applies.
    private static final String[] DEC_ROUND = {
        "1.234567", "-1.234567", "0.000049", "0.000050", "0.000051", "2.500000"
    };

    private static final long[][] ARRAY_TIMESTAMP_MS = {
        {}, {0L}, {1L, 86400123L}, {645453296789L}, {1704067200123L, 1704067200000L}, {4102444800123L}
    };
    private static final String[][] MAP_DATE_KEYS = {{}, {"a"}, {"a", "b"}, {"z"}, {"a", "b"}, {"k"}};
    private static final long[][] MAP_DATE_DAYS = {{}, {0L}, {19723L, 40000L}, {1L}, {19723L, 1L}, {40000L}};
    private static final String[][] MAP_NINT_KEYS = {{"a", "b"}, {}, {"x"}, {"n"}, {"a", "b"}, {"m"}};
    private static final Integer[][] MAP_NINT_VALUES = {
        {1, null}, {}, {-5}, {null}, {1, null}, {Integer.MAX_VALUE}
    };

    private static final int RANDOM_BYTES_LENGTH = 300000;

    // One cell per id 1..7: empty, "abc", all 256 byte values, three invalid UTF-8 sequences, the
    // bytes 22 5C 2F (quote, backslash, slash), a non-ASCII UTF-8 string, and a seeded random array.
    private static final byte[][] BYTE_CELLS = byteCells();

    private static final Schema KAFKA_DATE = org.apache.kafka.connect.data.Date.SCHEMA;

    private static final Schema TUPLE_SCHEMA = SchemaBuilder.struct()
            .field("ts", Timestamp.SCHEMA)
            .field("amount", Decimal.schema(4))
            .build();

    private static final Schema MATRIX_SCHEMA = SchemaBuilder.struct()
            .field("id", Schema.INT32_SCHEMA)
            .field("ts_dt", Timestamp.SCHEMA)
            .field("ts_dt64_0", Timestamp.SCHEMA)
            .field("ts_dt64_3", Timestamp.SCHEMA)
            .field("ts_dt64_6", Timestamp.SCHEMA)
            .field("ts_dt64_9", Timestamp.SCHEMA)
            .field("d_date", KAFKA_DATE)
            .field("d_date32", KAFKA_DATE)
            .field("t_ms", Time.SCHEMA)
            .field("dec_18_4", Decimal.schema(4))
            .field("dec_9_0", Decimal.schema(0))
            .field("dec_18_7", Decimal.schema(7))
            .field("dec_38_10", Decimal.schema(10))
            .field("dec_round", Decimal.schema(6))
            .field("b_str", Schema.BYTES_SCHEMA)
            .field("b_buf", Schema.BYTES_SCHEMA)
            .field("arr_dt64", SchemaBuilder.array(Timestamp.SCHEMA).build())
            .field("map_date", SchemaBuilder.map(Schema.STRING_SCHEMA, KAFKA_DATE).build())
            .field("tup", TUPLE_SCHEMA)
            .field("map_nint", SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.OPTIONAL_INT32_SCHEMA).build())
            .build();

    private static final String MATRIX_DDL = "CREATE TABLE %s (id UInt32, "
            + "ts_dt DateTime, ts_dt64_0 DateTime64(0), ts_dt64_3 DateTime64(3), ts_dt64_6 DateTime64(6), "
            + "ts_dt64_9 DateTime64(9), "
            + "d_date Date, d_date32 Date32, t_ms Int32, "
            + "dec_18_4 Decimal(18,4), dec_9_0 Decimal(9,0), dec_18_7 Decimal(18,7), "
            + "dec_38_10 Decimal(38,10), dec_round Decimal(18,4), "
            + "b_str String, b_buf String, "
            + "arr_dt64 Array(DateTime64(3)), map_date Map(String, Date), "
            + "tup Tuple(ts DateTime64(3), amount Decimal(18,4)), map_nint Map(String, Nullable(Int32))) "
            + "ENGINE = MergeTree ORDER BY id";

    private static final Schema NEGATIVE_SCHEMA = SchemaBuilder.struct()
            .field("id", Schema.INT32_SCHEMA)
            .field("ts0", Timestamp.SCHEMA)
            .field("ts3", Timestamp.SCHEMA)
            .field("ts9", Timestamp.SCHEMA)
            .field("d32", KAFKA_DATE)
            .build();

    private static final String NEGATIVE_DDL = "CREATE TABLE %s (id UInt32, ts0 DateTime64(0), "
            + "ts3 DateTime64(3), ts9 DateTime64(9), d32 Date32) ENGINE = MergeTree ORDER BY id";

    private static byte[][] byteCells() {
        byte[] allValues = new byte[256];
        for (int i = 0; i < allValues.length; i++) {
            allValues[i] = (byte) i;
        }
        byte[] random = new byte[RANDOM_BYTES_LENGTH];
        new Random(42).nextBytes(random);
        return new byte[][] {
            new byte[0],
            "abc".getBytes(StandardCharsets.US_ASCII),
            allValues,
            {(byte) 0xC3, 0x28, (byte) 0xED, (byte) 0xA0, (byte) 0x80, (byte) 0xC0, (byte) 0x80},
            {0x22, 0x5C, 0x2F},
            "Grüße ✓".getBytes(StandardCharsets.UTF_8),
            random
        };
    }

    private void createTable(ClickHouseHelperClient chc, String tableName, String createTableQuery) {
        try (Records ignored = chc.queryV2(String.format(createTableQuery, tableName))) {
            // table created
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Starts one sink task mapped to {@code table}, puts {@code records} and stops it. The table must
     * already exist, so a RowBinary task can fetch its metadata at start.
     */
    private void writeThroughTask(
            TestInfo testInfo,
            String connectorSuffix,
            String topic,
            String table,
            List<SinkRecord> records,
            Map<String, String> extraProps)
            throws Exception {
        Map<String, String> props = getBaseProps();
        props.putAll(extraProps);
        props.put(ClickHouseSinkConfig.TABLE_MAPPING, topic + "=" + table);
        props.put("name", uniqueConnectorName(testInfo) + connectorSuffix);

        ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
        try {
            sinkTask.start(props);
            sinkTask.put(records);
        } finally {
            sinkTask.stop();
        }
    }

    /** Writes {@code records} through one sink task on the metadata-free route ({@code insertFormat=json}). */
    private void writeThroughJsonRoute(
            TestInfo testInfo,
            String connectorSuffix,
            String topic,
            String table,
            List<SinkRecord> records,
            Map<String, String> extraProps)
            throws Exception {
        Map<String, String> jsonProps = new LinkedHashMap<>(extraProps);
        jsonProps.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        writeThroughTask(testInfo, connectorSuffix, topic, table, records, jsonProps);
    }

    /**
     * Writes the same records through the JSON route first and the RowBinary route second, so that a
     * mutation of a shared {@code ByteBuffer} by the JSON path would surface as a RowBinary
     * difference.
     */
    private void writeThroughBothRoutes(
            TestInfo testInfo,
            String topic,
            String jsonTable,
            String rbTable,
            List<SinkRecord> records,
            Map<String, String> extraProps)
            throws Exception {
        writeThroughJsonRoute(testInfo, JSON_SUFFIX, topic, jsonTable, records, extraProps);
        writeThroughTask(testInfo, ROWBINARY_SUFFIX, topic, rbTable, records, extraProps);
    }

    /** Typed read-back projection: each added column becomes one compared alias. */
    private static final class Projection {
        private final List<String> selects = new ArrayList<>(List.of("id"));
        private final List<String> aliases = new ArrayList<>(List.of("id"));

        /** Compares {@code toString(col)}. Aliases differ from the column names on purpose. */
        Projection strings(String... columns) {
            for (String column : columns) {
                expression(column + "_s", "toString(" + column + ")");
            }
            return this;
        }

        /** Compares {@code hex(col)}, which is exact for arbitrary bytes. */
        Projection hexes(String... columns) {
            for (String column : columns) {
                expression(column + "_hex", "hex(" + column + ")");
            }
            return this;
        }

        Projection expression(String alias, String expression) {
            selects.add(expression + " AS " + alias);
            aliases.add(alias);
            return this;
        }

        /** A SELECT with one {@code %s} for the table name. */
        String sqlTemplate() {
            return "SELECT " + String.join(", ", selects) + " FROM `%s` ORDER BY id";
        }

        String[] aliases() {
            return aliases.toArray(new String[0]);
        }
    }

    private static Map<String, String> rowWithId(List<Map<String, String>> rows, int id) {
        return rows.stream()
                .filter(row -> String.valueOf(id).equals(row.get("id")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row with id " + id + " in " + rows.size() + " rows"));
    }

    private static List<Map<String, String>> selectRows(
            ClickHouseHelperClient chc, Projection projection, String table) throws Exception {
        return RowSnapshots.select(chc, String.format(projection.sqlTemplate(), table), projection.aliases());
    }

    /**
     * Reads both tables with the same SELECT, runs {@code referenceAnchors} on the reference rows
     * first, then asserts the candidate table holds the same rows value for value.
     */
    private static void assertRoutesAgree(
            ClickHouseHelperClient chc,
            Projection projection,
            String candidateTable,
            String referenceTable,
            Consumer<List<Map<String, String>>> referenceAnchors)
            throws Exception {
        List<Map<String, String>> reference = selectRows(chc, projection, referenceTable);
        referenceAnchors.accept(reference);

        List<Map<String, String>> candidate = selectRows(chc, projection, candidateTable);
        assertEquals(reference.size(), candidate.size(), candidateTable + " row count differs from " + referenceTable);
        for (int i = 0; i < reference.size(); i++) {
            String id = reference.get(i).get("id");
            for (String alias : projection.aliases()) {
                assertEquals(
                        reference.get(i).get(alias),
                        candidate.get(i).get(alias),
                        candidateTable + " differs from " + referenceTable + " in column " + alias + " of id " + id);
            }
        }
    }

    private static Date timestamp(long epochMillis) {
        return new Date(epochMillis);
    }

    private static Date kafkaDate(long epochDays) {
        return new Date(epochDays * MILLIS_PER_DAY);
    }

    private static Struct matrixRow(int id) {
        int index = id == MATRIX_REPEATED_ID ? MATRIX_REPEATED_SOURCE_INDEX : id - 1;
        Date ts = timestamp(TIMESTAMP_MS[index]);

        List<Date> timestamps = new ArrayList<>();
        for (long ms : ARRAY_TIMESTAMP_MS[index]) {
            timestamps.add(timestamp(ms));
        }
        Map<String, Date> dates = new LinkedHashMap<>();
        for (int i = 0; i < MAP_DATE_KEYS[index].length; i++) {
            dates.put(MAP_DATE_KEYS[index][i], kafkaDate(MAP_DATE_DAYS[index][i]));
        }
        Map<String, Integer> nullableInts = new LinkedHashMap<>();
        for (int i = 0; i < MAP_NINT_KEYS[index].length; i++) {
            nullableInts.put(MAP_NINT_KEYS[index][i], MAP_NINT_VALUES[index][i]);
        }
        Struct tuple = new Struct(TUPLE_SCHEMA).put("ts", ts).put("amount", new BigDecimal(DEC_18_4[index]));

        // b_buf is the full backing array of the next cell: RowBinary's Struct.getBytes writes the
        // whole array, so a ByteBuffer is only comparable when it spans its array.
        byte[] bufCell = BYTE_CELLS[id % BYTE_CELLS.length];
        return new Struct(MATRIX_SCHEMA)
                .put("id", id)
                .put("ts_dt", ts)
                .put("ts_dt64_0", ts)
                .put("ts_dt64_3", ts)
                .put("ts_dt64_6", ts)
                .put("ts_dt64_9", ts)
                .put("d_date", kafkaDate(DATE_DAYS[index]))
                .put("d_date32", kafkaDate(DATE_DAYS[index]))
                .put("t_ms", new Date(TIME_MS[index]))
                .put("dec_18_4", new BigDecimal(DEC_18_4[index]))
                .put("dec_9_0", new BigDecimal(DEC_9_0[index]))
                .put("dec_18_7", new BigDecimal(DEC_18_7[index]))
                .put("dec_38_10", new BigDecimal(DEC_38_10[index]))
                .put("dec_round", new BigDecimal(DEC_ROUND[index]))
                .put("b_str", BYTE_CELLS[id - 1])
                .put("b_buf", ByteBuffer.wrap(bufCell))
                .put("arr_dt64", timestamps)
                .put("map_date", dates)
                .put("tup", tuple)
                .put("map_nint", nullableInts);
    }

    private static List<SinkRecord> sinkRecords(String topic, Schema schema, List<Struct> rows) {
        List<SinkRecord> records = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            records.add(new SinkRecord(topic, 0, null, null, schema, rows.get(i), i + 1L));
        }
        return records;
    }

    @Test
    @DisplayName(
            "JSONP-06: every D-04 logical type, its boundaries and the D-07 nested cells store the RowBinary value on the metadata-free JSON route")
    void logicalTypesReachRowBinaryParityOnTheMetadataFreeRoute(TestInfo testInfo) throws Exception {
        String topic = createTopicName("jsonp06_matrix");
        String jsonTable = topic + JSON_SUFFIX;
        String rbTable = topic + ROWBINARY_SUFFIX;

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        ClickHouseTestHelpers.dropTable(chc, jsonTable);
        ClickHouseTestHelpers.dropTable(chc, rbTable);
        createTable(chc, jsonTable, MATRIX_DDL);
        createTable(chc, rbTable, MATRIX_DDL);
        try {
            List<Struct> rows = new ArrayList<>();
            for (int id = 1; id <= MATRIX_RECORD_COUNT; id++) {
                rows.add(matrixRow(id));
            }
            writeThroughBothRoutes(testInfo, topic, jsonTable, rbTable, sinkRecords(topic, MATRIX_SCHEMA, rows), Map.of());

            Projection projection = new Projection()
                    .strings(
                            "ts_dt",
                            "ts_dt64_0",
                            "ts_dt64_3",
                            "ts_dt64_6",
                            "ts_dt64_9",
                            "d_date",
                            "d_date32",
                            "t_ms",
                            "dec_18_4",
                            "dec_9_0",
                            "dec_18_7",
                            "dec_38_10",
                            "dec_round",
                            "arr_dt64",
                            "map_date",
                            "tup",
                            "map_nint")
                    .hexes("b_str", "b_buf")
                    .expression("ts_dt_unix", "toUnixTimestamp(ts_dt)")
                    .expression("ts_dt64_3_ms", "toUnixTimestamp64Milli(ts_dt64_3)");

            assertRoutesAgree(chc, projection, jsonTable, rbTable, reference -> {
                assertEquals(MATRIX_RECORD_COUNT, reference.size(), "RowBinary reference row count");
                assertEquals("2024-01-01 00:00:00.123", rowWithId(reference, 5).get("ts_dt64_3_s"));
                assertEquals("1704067200123", rowWithId(reference, 5).get("ts_dt64_3_ms"));
                assertEquals("4102444800123", rowWithId(reference, 6).get("ts_dt64_3_ms"));
                assertEquals("2024-01-01", rowWithId(reference, 3).get("d_date_s"));
                assertEquals("12.3456", rowWithId(reference, 1).get("dec_18_4_s"));
                assertEquals("616263", rowWithId(reference, 2).get("b_str_hex"));
                assertEquals(RANDOM_BYTES_LENGTH * 2, rowWithId(reference, 7).get("b_str_hex").length());
            });
        } finally {
            ClickHouseTestHelpers.dropTable(chc, jsonTable);
            ClickHouseTestHelpers.dropTable(chc, rbTable);
        }
    }

    @Test
    @DisplayName(
            "JSONP-06: negative epochs reach RowBinary parity in DateTime64(0/3/9) and Date32 on the metadata-free JSON route")
    void negativeEpochsReachRowBinaryParityInDateTime64AndDate32(TestInfo testInfo) throws Exception {
        String topic = createTopicName("jsonp06_negative");
        String jsonTable = topic + JSON_SUFFIX;
        String rbTable = topic + ROWBINARY_SUFFIX;

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        ClickHouseTestHelpers.dropTable(chc, jsonTable);
        ClickHouseTestHelpers.dropTable(chc, rbTable);
        createTable(chc, jsonTable, NEGATIVE_DDL);
        createTable(chc, rbTable, NEGATIVE_DDL);
        try {
            // DateTime and Date are left out on purpose: both routes clamp or wrap negatives there.
            long[] epochMillis = {-1500L, -1L};
            long[] epochDays = {-365L, -1L};
            List<Struct> rows = new ArrayList<>();
            for (int i = 0; i < epochMillis.length; i++) {
                Date ts = timestamp(epochMillis[i]);
                rows.add(new Struct(NEGATIVE_SCHEMA)
                        .put("id", i + 1)
                        .put("ts0", ts)
                        .put("ts3", ts)
                        .put("ts9", ts)
                        .put("d32", kafkaDate(epochDays[i])));
            }
            writeThroughBothRoutes(testInfo, topic, jsonTable, rbTable, sinkRecords(topic, NEGATIVE_SCHEMA, rows), Map.of());

            Projection projection = new Projection()
                    .strings("ts0", "ts3", "ts9", "d32")
                    .expression("ts3_ms", "toUnixTimestamp64Milli(ts3)");

            assertRoutesAgree(chc, projection, jsonTable, rbTable, reference -> {
                assertEquals(2, reference.size(), "RowBinary reference row count");
                assertEquals("-1500", rowWithId(reference, 1).get("ts3_ms"));
                assertEquals("-1", rowWithId(reference, 2).get("ts3_ms"));
                assertEquals("1969-01-01", rowWithId(reference, 1).get("d32_s"));
            });
        } finally {
            ClickHouseTestHelpers.dropTable(chc, jsonTable);
            ClickHouseTestHelpers.dropTable(chc, rbTable);
        }
    }

    private static final Schema CDC_ROW_SCHEMA = SchemaBuilder.struct()
            .optional()
            .field("id", Schema.INT32_SCHEMA)
            .field("ts", Timestamp.SCHEMA)
            .field("amount", Decimal.schema(4))
            .build();

    private static final Schema CDC_SOURCE_SCHEMA = SchemaBuilder.struct()
            .optional()
            .field("lsn", Schema.OPTIONAL_INT64_SCHEMA)
            .build();

    private static final String CDC_DDL = "CREATE TABLE %s (id Int32, ts DateTime64(3, 'UTC'), "
            + "amount Decimal(18,4), _version UInt64, is_deleted UInt8) ENGINE = MergeTree ORDER BY id";

    private static Schema cdcEnvelopeSchema(Schema rowSchema) {
        return SchemaBuilder.struct()
                .name("server.db.orders.Envelope")
                .field("op", Schema.STRING_SCHEMA)
                .field("before", rowSchema)
                .field("after", rowSchema)
                .field("source", CDC_SOURCE_SCHEMA)
                .build();
    }

    private static Struct cdcCreateEnvelope(Schema envelopeSchema, Struct after, long lsn) {
        return new Struct(envelopeSchema)
                .put("op", "c")
                .put("after", after)
                .put("source", new Struct(CDC_SOURCE_SCHEMA).put("lsn", lsn));
    }

    private static Struct cdcRow(int id, long epochMillis, String amount) {
        return new Struct(CDC_ROW_SCHEMA)
                .put("id", id)
                .put("ts", timestamp(epochMillis))
                .put("amount", new BigDecimal(amount));
    }

    @Test
    @DisplayName(
            "JSONP-06/D-06: a Debezium CDC envelope with a Connect Timestamp and a Decimal stores the RowBinary value on the metadata-free JSON route")
    void cdcEnvelopeReachesRowBinaryParityOnTheMetadataFreeRoute(TestInfo testInfo) throws Exception {
        String topic = createTopicName("jsonp06_cdc_parity");
        String jsonTable = topic + JSON_SUFFIX;
        String rbTable = topic + ROWBINARY_SUFFIX;

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        ClickHouseTestHelpers.dropTable(chc, jsonTable);
        ClickHouseTestHelpers.dropTable(chc, rbTable);
        // The UTC zone on ts keeps toString(ts) independent of the server's default zone.
        createTable(chc, jsonTable, CDC_DDL);
        createTable(chc, rbTable, CDC_DDL);
        try {
            Schema envelopeSchema = cdcEnvelopeSchema(CDC_ROW_SCHEMA);
            List<Struct> envelopes = List.of(
                    cdcCreateEnvelope(envelopeSchema, cdcRow(1, 1704067200123L, "12.3456"), 100L),
                    cdcCreateEnvelope(envelopeSchema, cdcRow(2, 86400123L, "-0.0001"), 101L));
            writeThroughBothRoutes(
                    testInfo,
                    topic,
                    jsonTable,
                    rbTable,
                    sinkRecords(topic, envelopeSchema, envelopes),
                    Map.of(ClickHouseSinkConfig.DEBEZIUM_CDC_ENABLED, "true"));

            Projection projection = new Projection()
                    .strings("ts", "amount", "_version", "is_deleted")
                    .expression("ts_ms", "toUnixTimestamp64Milli(ts)");

            assertRoutesAgree(chc, projection, jsonTable, rbTable, reference -> {
                assertEquals(2, reference.size(), "RowBinary reference row count");
                assertEquals("2024-01-01 00:00:00.123", rowWithId(reference, 1).get("ts_s"));
                assertEquals("12.3456", rowWithId(reference, 1).get("amount_s"));
                assertEquals("100", rowWithId(reference, 1).get("_version_s"));
                assertEquals("86400123", rowWithId(reference, 2).get("ts_ms"));
            });
        } finally {
            ClickHouseTestHelpers.dropTable(chc, jsonTable);
            ClickHouseTestHelpers.dropTable(chc, rbTable);
        }
    }

    @Test
    @DisplayName(
            "D-01 (13-REVIEW WR-02): io.debezium.time.Timestamp into DateTime64(3) and io.debezium.time.NanoTimestamp into DateTime64(9) store the RowBinary value on the metadata-free JSON route")
    void debeziumTimestampAndNanoTimestampReachRowBinaryParityOnTheMetadataFreeRoute(TestInfo testInfo)
            throws Exception {
        String topic = createTopicName("d01_debezium_timestamps");
        String jsonTable = topic + JSON_SUFFIX;
        String rbTable = topic + ROWBINARY_SUFFIX;
        String ddl = "CREATE TABLE %s (id Int32, ts DateTime64(3, 'UTC'), nts DateTime64(9, 'UTC'), "
                + "_version UInt64, is_deleted UInt8) ENGINE = MergeTree ORDER BY id";

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        ClickHouseTestHelpers.dropTable(chc, jsonTable);
        ClickHouseTestHelpers.dropTable(chc, rbTable);
        createTable(chc, jsonTable, ddl);
        createTable(chc, rbTable, ddl);
        try {
            Schema rowSchema = SchemaBuilder.struct()
                    .optional()
                    .field("id", Schema.INT32_SCHEMA)
                    .field("ts", SchemaBuilder.int64().name("io.debezium.time.Timestamp").build())
                    .field("nts", SchemaBuilder.int64().name("io.debezium.time.NanoTimestamp").build())
                    .build();
            Schema envelopeSchema = cdcEnvelopeSchema(rowSchema);
            long[][] cells = {{1_704_067_200_123L, 1_704_067_200_123_456_789L}, {0L, 0L}, {1L, 1L}, {-1L, -1L}};
            List<Struct> envelopes = new ArrayList<>();
            for (int i = 0; i < cells.length; i++) {
                Struct after = new Struct(rowSchema)
                        .put("id", i + 1)
                        .put("ts", cells[i][0])
                        .put("nts", cells[i][1]);
                envelopes.add(cdcCreateEnvelope(envelopeSchema, after, 100L + i));
            }
            writeThroughBothRoutes(
                    testInfo,
                    topic,
                    jsonTable,
                    rbTable,
                    sinkRecords(topic, envelopeSchema, envelopes),
                    Map.of(ClickHouseSinkConfig.DEBEZIUM_CDC_ENABLED, "true"));

            Projection projection = new Projection()
                    .strings("ts", "nts")
                    .expression("ts_ms", "toUnixTimestamp64Milli(ts)")
                    .expression("nts_ns", "toUnixTimestamp64Nano(nts)");

            assertRoutesAgree(chc, projection, jsonTable, rbTable, reference -> {
                assertEquals(4, reference.size(), "RowBinary reference row count");
                assertEquals("2024-01-01 00:00:00.123", rowWithId(reference, 1).get("ts_s"));
                assertEquals("1704067200123456789", rowWithId(reference, 1).get("nts_ns"));
                assertEquals("-1", rowWithId(reference, 4).get("ts_ms"));
            });
        } finally {
            ClickHouseTestHelpers.dropTable(chc, jsonTable);
            ClickHouseTestHelpers.dropTable(chc, rbTable);
        }
    }

    @Test
    @DisplayName(
            "D-18: io.debezium.time.Date and io.debezium.time.MicroTimestamp after-fields read back as the calendar date and the microsecond datetime on the metadata-free JSON route")
    void debeziumDateAndMicroTimestampReadBackAsLiterals(TestInfo testInfo) throws Exception {
        String topic = createTopicName("d18_debezium_literals");

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        ClickHouseTestHelpers.dropTable(chc, topic);
        createTable(chc, topic,
                "CREATE TABLE %s (id Int32, d Date, mts DateTime64(6, 'UTC'), _version UInt64, is_deleted UInt8) "
                        + "ENGINE = MergeTree ORDER BY id");
        try {
            Schema rowSchema = SchemaBuilder.struct()
                    .optional()
                    .field("id", Schema.INT32_SCHEMA)
                    .field("d", SchemaBuilder.int32().name("io.debezium.time.Date").build())
                    .field("mts", SchemaBuilder.int64().name("io.debezium.time.MicroTimestamp").build())
                    .build();
            Schema envelopeSchema = cdcEnvelopeSchema(rowSchema);
            Struct after = new Struct(rowSchema).put("id", 1).put("d", 19723).put("mts", 1704067200123456L);
            writeThroughJsonRoute(
                    testInfo,
                    JSON_SUFFIX,
                    topic,
                    topic,
                    sinkRecords(topic, envelopeSchema, List.of(cdcCreateEnvelope(envelopeSchema, after, 100L))),
                    Map.of(ClickHouseSinkConfig.DEBEZIUM_CDC_ENABLED, "true"));

            // Literals, not a RowBinary comparison: RowBinary's Date path divides local-midnight
            // millis, so its result depends on the JVM time zone (RESEARCH Pitfall 3, ledger L-05).
            List<Map<String, String>> rows = selectRows(chc, new Projection().strings("d", "mts"), topic);
            assertEquals(1, rows.size());
            assertEquals("2024-01-01", rows.get(0).get("d_s"));
            assertEquals("2024-01-01 00:00:00.123456", rows.get(0).get("mts_s"));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, topic);
        }
    }

    @Test
    @DisplayName(
            "JSONP-07: with every optional field null the metadata-free JSON route stores NULL in Nullable columns and the DEFAULT or type default elsewhere")
    void nullFieldsStoreNullInNullableColumnsAndDefaultsElsewhere(TestInfo testInfo) throws Exception {
        String topic = createTopicName("jsonp07_null_semantics");

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        ClickHouseTestHelpers.dropTable(chc, topic);
        createTable(chc, topic,
                "CREATE TABLE %s (id UInt32, n Nullable(Int32), nd Nullable(Int32) DEFAULT 5, nn Int32, "
                        + "ndf Int32 DEFAULT 7, s String, a Array(Int32), m Map(String,String), "
                        + "dt Nullable(DateTime64(3))) ENGINE = MergeTree ORDER BY id");
        try {
            Schema schema = SchemaBuilder.struct()
                    .field("id", Schema.INT32_SCHEMA)
                    .field("n", Schema.OPTIONAL_INT32_SCHEMA)
                    .field("nd", Schema.OPTIONAL_INT32_SCHEMA)
                    .field("nn", Schema.OPTIONAL_INT32_SCHEMA)
                    .field("ndf", Schema.OPTIONAL_INT32_SCHEMA)
                    .field("s", Schema.OPTIONAL_STRING_SCHEMA)
                    .field("a", SchemaBuilder.array(Schema.INT32_SCHEMA).optional().build())
                    .field("m", SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.STRING_SCHEMA).optional().build())
                    .field("dt", Timestamp.builder().optional().build())
                    .build();
            writeThroughJsonRoute(
                    testInfo,
                    JSON_SUFFIX,
                    topic,
                    topic,
                    sinkRecords(topic, schema, List.of(new Struct(schema).put("id", 1))),
                    Map.of());

            // JSONP-07: the writer omits top-level nulls, so a Nullable column with a non-NULL DEFAULT
            // (nd) takes that DEFAULT instead of NULL; a non-nullable column takes its DEFAULT or
            // type default.
            Projection projection = new Projection()
                    .expression("n_null", "isNull(n)")
                    .expression("nd_null", "isNull(nd)")
                    .expression("dt_null", "isNull(dt)")
                    .strings("nd", "nn", "ndf", "s", "a", "m");
            List<Map<String, String>> rows = selectRows(chc, projection, topic);
            assertEquals(1, rows.size());
            Map<String, String> row = rows.get(0);
            assertEquals("1", row.get("n_null"));
            assertEquals("0", row.get("nd_null"));
            assertEquals("5", row.get("nd_s"));
            assertEquals("0", row.get("nn_s"));
            assertEquals("7", row.get("ndf_s"));
            assertEquals("", row.get("s_s"));
            assertEquals("[]", row.get("a_s"));
            assertEquals("{}", row.get("m_s"));
            assertEquals("1", row.get("dt_null"));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, topic);
        }
    }

    @Test
    @DisplayName(
            "JSONP-07: nulls inside arrays, maps and nested structs are coerced to the element default or stored as NULL by the metadata-free JSON route")
    void nestedNullsAreCoercedInsideContainers(TestInfo testInfo) throws Exception {
        String topic = createTopicName("jsonp07_nested_nulls");

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        ClickHouseTestHelpers.dropTable(chc, topic);
        createTable(chc, topic,
                "CREATE TABLE %s (id UInt32, a2 Array(Int32), m2 Map(String,String), m3 Map(String, Nullable(Int32)), "
                        + "t Tuple(x Nullable(Int32), y Int32)) ENGINE = MergeTree ORDER BY id");
        try {
            Schema tupleSchema = SchemaBuilder.struct()
                    .field("x", Schema.OPTIONAL_INT32_SCHEMA)
                    .field("y", Schema.OPTIONAL_INT32_SCHEMA)
                    .build();
            Schema schema = SchemaBuilder.struct()
                    .field("id", Schema.INT32_SCHEMA)
                    .field("a2", SchemaBuilder.array(Schema.OPTIONAL_INT32_SCHEMA).build())
                    .field("m2", SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.OPTIONAL_STRING_SCHEMA).build())
                    .field("m3", SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.OPTIONAL_INT32_SCHEMA).build())
                    .field("t", tupleSchema)
                    .build();
            Map<String, String> nullString = new LinkedHashMap<>();
            nullString.put("k", null);
            Map<String, Integer> nullInt = new LinkedHashMap<>();
            nullInt.put("k", null);
            Struct value = new Struct(schema)
                    .put("id", 1)
                    .put("a2", Arrays.asList(1, null))
                    .put("m2", nullString)
                    .put("m3", nullInt)
                    .put("t", new Struct(tupleSchema));
            writeThroughJsonRoute(testInfo, JSON_SUFFIX, topic, topic, sinkRecords(topic, schema, List.of(value)), Map.of());

            // RowBinary rejects these same nested nulls in non-nullable slots after plan 13-03, so this
            // is a deliberate cross-route asymmetry (ledger L-07).
            List<Map<String, String>> rows = selectRows(chc, new Projection().strings("a2", "m2", "m3", "t"), topic);
            assertEquals(1, rows.size());
            assertEquals("[1,0]", rows.get(0).get("a2_s"));
            assertEquals("{'k':''}", rows.get(0).get("m2_s"));
            assertEquals("{'k':NULL}", rows.get(0).get("m3_s"));
            assertEquals("(NULL,0)", rows.get(0).get("t_s"));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, topic);
        }
    }

    @Test
    @DisplayName(
            "JSONP-07/Phase 9 D-06: jsonInsertColumns=id,listed on a Struct fills the listed column and leaves unlisted columns at their DEFAULT")
    void jsonInsertColumnsLeavesUnlistedColumnsAtDefaultForStructRecords(TestInfo testInfo) throws Exception {
        String topic = createTopicName("jsonp07_insert_columns");

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        ClickHouseTestHelpers.dropTable(chc, topic);
        createTable(chc, topic,
                "CREATE TABLE %s (id UInt32, listed Int32, extra Int32 DEFAULT 42, tag String DEFAULT 'unset') "
                        + "ENGINE = MergeTree ORDER BY id");
        try {
            Schema schema = SchemaBuilder.struct()
                    .field("id", Schema.INT32_SCHEMA)
                    .field("listed", Schema.INT32_SCHEMA)
                    .field("extra", Schema.INT32_SCHEMA)
                    .field("tag", Schema.STRING_SCHEMA)
                    .field("bogus", Schema.STRING_SCHEMA)
                    .build();
            Struct value = new Struct(schema)
                    .put("id", 1)
                    .put("listed", 9)
                    .put("extra", 777)
                    .put("tag", "should_not_land")
                    .put("bogus", "x");
            writeThroughJsonRoute(
                    testInfo,
                    JSON_SUFFIX,
                    topic,
                    topic,
                    sinkRecords(topic, schema, List.of(value)),
                    Map.of(ClickHouseSinkConfig.JSON_INSERT_COLUMNS, "id,listed"));

            List<Map<String, String>> rows = selectRows(chc, new Projection().strings("listed", "extra", "tag"), topic);
            assertEquals(1, rows.size());
            assertEquals("9", rows.get(0).get("listed_s"));
            assertEquals("42", rows.get(0).get("extra_s"));
            assertEquals("unset", rows.get(0).get("tag_s"));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, topic);
        }
    }

    @Test
    @DisplayName(
            "JSONP-07: bypassFieldCleanup true and false land identical rows for a Struct with a non-column field on the metadata-free JSON route")
    void bypassFieldCleanupDoesNotChangeWhatLandsOnTheMetadataFreeRoute(TestInfo testInfo) throws Exception {
        String topic = createTopicName("jsonp07_bypass_cleanup");
        String bypassTable = topic + "_bypass";
        String cleanupTable = topic + "_cleanup";
        String ddl = "CREATE TABLE %s (id UInt32, str String) ENGINE = MergeTree ORDER BY id";

        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        ClickHouseTestHelpers.dropTable(chc, bypassTable);
        ClickHouseTestHelpers.dropTable(chc, cleanupTable);
        createTable(chc, bypassTable, ddl);
        createTable(chc, cleanupTable, ddl);
        try {
            Schema schema = SchemaBuilder.struct()
                    .field("id", Schema.INT32_SCHEMA)
                    .field("str", Schema.STRING_SCHEMA)
                    .field("not_a_column", Schema.STRING_SCHEMA)
                    .build();
            String text = "Grüße ✓";
            List<SinkRecord> records = sinkRecords(
                    topic, schema, List.of(new Struct(schema).put("id", 1).put("str", text).put("not_a_column", "x")));
            writeThroughJsonRoute(
                    testInfo, "_bypass", topic, bypassTable, records,
                    Map.of(ClickHouseSinkConfig.BYPASS_FIELD_CLEANUP, "true"));
            writeThroughJsonRoute(
                    testInfo, "_cleanup", topic, cleanupTable, records,
                    Map.of(ClickHouseSinkConfig.BYPASS_FIELD_CLEANUP, "false"));

            Projection projection = new Projection().hexes("str");
            assertRoutesAgree(chc, projection, cleanupTable, bypassTable, reference -> {
                assertEquals(1, reference.size(), "bypassFieldCleanup=true row count");
                assertEquals(
                        HexFormat.of().withUpperCase().formatHex(text.getBytes(StandardCharsets.UTF_8)),
                        reference.get(0).get("str_hex"));
            });
        } finally {
            ClickHouseTestHelpers.dropTable(chc, bypassTable);
            ClickHouseTestHelpers.dropTable(chc, cleanupTable);
        }
    }
}
