package com.clickhouse.kafka.connect.sink.db;

import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.allEmptyEnvelopes;
import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.cdcEnvelope;
import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.cdcSinkRecords;
import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.mixedBatchEnvelopes;
import static com.clickhouse.kafka.connect.sink.db.CapturingClickHouseWriter.convertAll;
import static com.clickhouse.kafka.connect.sink.db.CapturingClickHouseWriter.stubCdcTable;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.data.SchemaType;
import com.clickhouse.kafka.connect.sink.db.CapturingClickHouseWriter.Capture;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free byte-level pins for the empty-CDC skip (JSONP-09, D-02, D-03). Every route runs the
 * real {@link ClickHouseWriter} through {@link CapturingClickHouseWriter}, so the assertions are on
 * the exact insert body and the dedup token, never on row counts. Extends nothing, so it stays in
 * {@code testUnit}.
 */
public class EmptyCdcSkipUnitTest {

    private static final String TOPIC = "cdc_topic";
    private static final int PARTITION = 0;
    private static final long FIRST_OFFSET = 10;
    private static final String ROUTE_EXPLICIT_JSON = "explicit-json";
    private static final String ROUTE_BYPASS_JSON = "bypass-json";
    private static final String ROUTE_ROWBINARY_DEFAULTS = "rowbinary-defaults";
    private static final String ROUTE_ROWBINARY_PLAIN = "rowbinary-plain";
    private static final String SKIP_LOG_PREFIX = "Skipping empty CDC record (no values)";

    static Stream<String> routes() {
        return Stream.of(
                ROUTE_EXPLICIT_JSON, ROUTE_BYPASS_JSON, ROUTE_ROWBINARY_DEFAULTS, ROUTE_ROWBINARY_PLAIN);
    }

    private static CapturingClickHouseWriter writer(String route) {
        Map<String, String> props = new HashMap<>();
        switch (route) {
            case ROUTE_EXPLICIT_JSON -> props.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
            case ROUTE_BYPASS_JSON -> props.put("bypassRowBinary", "true");
            default -> {}
        }
        return new CapturingClickHouseWriter(
                stubCdcTable(route.equals(ROUTE_ROWBINARY_DEFAULTS)), new ClickHouseSinkConfig(props));
    }

    private static String expectedRoute(String route) {
        return route.endsWith("json")
                ? CapturingClickHouseWriter.ROUTE_JSON
                : CapturingClickHouseWriter.ROUTE_ROWBINARY;
    }

    private static List<Record> records(Struct... envelopes) {
        return convertAll(cdcSinkRecords(TOPIC, PARTITION, FIRST_OFFSET, envelopes));
    }

    private static void insert(CapturingClickHouseWriter writer, List<Record> records) throws Exception {
        long lastOffset = FIRST_OFFSET + records.size() - 1;
        writer.doInsert(records, new QueryIdentifier(TOPIC, PARTITION, FIRST_OFFSET, lastOffset, "q"));
    }

    /** Inserts through a fresh writer and returns its only capture. */
    private static Capture insertOnce(String route, List<Record> records) throws Exception {
        CapturingClickHouseWriter writer = writer(route);
        insert(writer, records);
        assertEquals(1, writer.captures().size(), "the insert hook must run exactly once on " + route);
        Capture capture = writer.captures().get(0);
        assertEquals(expectedRoute(route), capture.route());
        return capture;
    }

    @ParameterizedTest(name = "JSONP-09: the mixed batch body equals the real-only body in batch order on {0}")
    @MethodSource("routes")
    void mixedBatchBodyEqualsRealOnlyBody(String route) throws Exception {
        Struct[] mixed = mixedBatchEnvelopes();
        Capture withEmpties = insertOnce(route, records(mixed));
        Capture realOnly = insertOnce(route, records(mixed[0], mixed[3]));

        assertTrue(withEmpties.body().length > 0, "the real rows must still be sent");
        assertArrayEquals(realOnly.body(), withEmpties.body());
        assertEquals(TOPIC + "-0-10-13", withEmpties.deduplicationToken());
    }

    @ParameterizedTest(name = "JSONP-09: an all-empty batch still opens the insert with a zero-length body on {0}")
    @MethodSource("routes")
    void allEmptyBatchStillOpensTheInsertWithAnEmptyBody(String route) throws Exception {
        Capture capture = insertOnce(route, records(allEmptyEnvelopes()));

        assertEquals(0, capture.body().length);
        assertEquals(TOPIC + "-0-10-12", capture.deduplicationToken());
    }

    @ParameterizedTest(name = "JSONP-09: the same mixed batch twice gives byte-identical bodies and tokens on {0}")
    @MethodSource("routes")
    void sameBatchTwiceIsByteIdentical(String route) throws Exception {
        CapturingClickHouseWriter writer = writer(route);
        List<Record> batch = records(mixedBatchEnvelopes());
        insert(writer, batch);
        insert(writer, batch);

        assertEquals(2, writer.captures().size());
        Capture first = writer.captures().get(0);
        Capture second = writer.captures().get(1);
        assertTrue(first.body().length > 0);
        assertArrayEquals(first.body(), second.body());
        assertEquals(first.deduplicationToken(), second.deduplicationToken());
    }

    @ParameterizedTest(name = "JSONP-09: a CDC record with a null value map serializes to zero bytes on {0}")
    @MethodSource("routes")
    void nullValueMapSerializesToNothing(String route) throws Exception {
        SinkRecord sinkRecord =
                cdcSinkRecords(TOPIC, PARTITION, FIRST_OFFSET, cdcEnvelope("c", null, null, 1L)).get(0);
        Record nullMap = Record.newRecord(
                SchemaType.DEBEZIUM_CDC, TOPIC, PARTITION, FIRST_OFFSET, null, null, "default", sinkRecord);

        Capture capture = insertOnce(route, List.of(nullMap));

        assertEquals(0, capture.body().length);
    }

    @Test
    @DisplayName("JSONP-09: the mixed batch logs one DEBUG skip line per empty record, identical on every route")
    void skipLogLineIsIdenticalOnEveryRoute() throws Exception {
        List<String> expected = List.of(
                SKIP_LOG_PREFIX + " topic [cdc_topic] partition [0] offset [11]",
                SKIP_LOG_PREFIX + " topic [cdc_topic] partition [0] offset [12]");
        for (String route : routes().collect(Collectors.toList())) {
            try (LogCapture logs = LogCapture.attach(ClickHouseWriter.class)) {
                insertOnce(route, records(mixedBatchEnvelopes()));

                List<LogCapture.CapturedEvent> skips = logs.events("DEBUG").stream()
                        .filter(e -> e.message().startsWith(SKIP_LOG_PREFIX))
                        .collect(Collectors.toList());
                assertEquals(
                        expected,
                        skips.stream().map(LogCapture.CapturedEvent::message).collect(Collectors.toList()),
                        route);
            }
        }
    }

    @Test
    @DisplayName("JSONP-09: on the bypass route a schemaless record whose fields are all unknown still writes the defaults-only row {}")
    void schemalessRecordWithOnlyUnknownFieldsStillWritesDefaultsRow() throws Exception {
        SinkRecord sinkRecord = new SinkRecord(TOPIC, PARTITION, null, null, null, Map.of("unknown_a", 1), FIRST_OFFSET);
        Record schemaless = Record.convert(sinkRecord, false, ".", "default", true);
        assertEquals(SchemaType.SCHEMA_LESS, schemaless.getSchemaType());

        Capture capture = insertOnce(ROUTE_BYPASS_JSON, List.of(schemaless));

        assertArrayEquals("{}".getBytes(StandardCharsets.UTF_8), capture.body());
    }

    @Test
    @DisplayName("JSONP-09: on the bypass route a CDC record that holds only fields the table lacks is not skipped and writes {}")
    void cdcRecordWithOnlyUnknownFieldsIsNotSkipped() throws Exception {
        SinkRecord sinkRecord =
                cdcSinkRecords(TOPIC, PARTITION, FIRST_OFFSET, cdcEnvelope("c", null, null, 1L)).get(0);
        Map<String, Data> unknownOnly = Map.of("other", new Data(Schema.STRING_SCHEMA, "x"));
        Record record = Record.newRecord(
                SchemaType.DEBEZIUM_CDC, TOPIC, PARTITION, FIRST_OFFSET, null, unknownOnly, "default", sinkRecord);

        Capture capture = insertOnce(ROUTE_BYPASS_JSON, List.of(record));

        assertArrayEquals("{}".getBytes(StandardCharsets.UTF_8), capture.body());
    }
}
