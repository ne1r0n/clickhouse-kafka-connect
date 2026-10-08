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
import java.util.Date;
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
        Map<String, String> jsonProps = new LinkedHashMap<>(extraProps);
        jsonProps.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        writeThroughTask(testInfo, JSON_SUFFIX, topic, jsonTable, records, jsonProps);
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

    /**
     * Reads both tables with the same SELECT, runs {@code referenceAnchors} on the RowBinary rows
     * first, then asserts the JSON table holds the same rows value for value.
     */
    private static void assertRoutesAgree(
            ClickHouseHelperClient chc,
            Projection projection,
            String jsonTable,
            String rbTable,
            Consumer<List<Map<String, String>>> referenceAnchors)
            throws Exception {
        List<Map<String, String>> reference =
                RowSnapshots.select(chc, String.format(projection.sqlTemplate(), rbTable), projection.aliases());
        referenceAnchors.accept(reference);

        List<Map<String, String>> actual =
                RowSnapshots.select(chc, String.format(projection.sqlTemplate(), jsonTable), projection.aliases());
        assertEquals(reference.size(), actual.size(), "JSON route row count differs from RowBinary");
        for (int i = 0; i < reference.size(); i++) {
            String id = reference.get(i).get("id");
            for (String alias : projection.aliases()) {
                assertEquals(
                        reference.get(i).get(alias),
                        actual.get(i).get(alias),
                        "JSON route differs from RowBinary in column " + alias + " of id " + id);
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
}
