package com.clickhouse.kafka.connect.sink.processing;

import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.allEmptyEnvelopes;
import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.cdcSinkRecords;
import static com.clickhouse.kafka.connect.sink.CdcTestFixtures.mixedBatchEnvelopes;
import static com.clickhouse.kafka.connect.sink.db.CapturingClickHouseWriter.convertAll;
import static com.clickhouse.kafka.connect.sink.db.CapturingClickHouseWriter.stubCdcTable;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.db.CapturingClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.CapturingClickHouseWriter.Capture;
import com.clickhouse.kafka.connect.sink.db.InMemoryDBWriter;
import com.clickhouse.kafka.connect.sink.state.State;
import com.clickhouse.kafka.connect.sink.state.StateProvider;
import com.clickhouse.kafka.connect.sink.state.StateRecord;
import com.clickhouse.kafka.connect.sink.state.provider.InMemoryState;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import org.apache.kafka.connect.data.Struct;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Pins that the empty-CDC skip leaves exactly-once state whole (JSONP-09, D-03): the batch range,
 * the final state and the dedup token cover every record, empty ones included, and a retried batch
 * rebuilds the identical body. Uses the real {@code Processing} with {@code InMemoryState}, and
 * either {@code InMemoryDBWriter} or a real {@code ClickHouseWriter} with only its network hooks
 * replaced; the writer is never mocked.
 */
public class EmptyCdcExactlyOnceStateTest {

    private static final String TOPIC = "cdc_topic";
    private static final int PARTITION = 0;
    private static final long FIRST_OFFSET = 10;

    private SinkTaskStatistics statistics;

    @BeforeEach
    void setUp() {
        statistics = new SinkTaskStatistics(null, 0);
        statistics.registerMBean();
    }

    @AfterEach
    void tearDown() {
        statistics.unregisterMBean();
        statistics = null;
    }

    /** Label, whole batch, the real-only records of that batch, and the last offset. */
    static Stream<Arguments> batches() {
        Struct[] mixed = mixedBatchEnvelopes();
        return Stream.of(
                Arguments.of("mixed batch", mixed, new Struct[] {mixed[0], mixed[3]}, 13L),
                Arguments.of("all-empty batch", allEmptyEnvelopes(), new Struct[] {}, 12L));
    }

    private static List<Record> records(Struct... envelopes) {
        return convertAll(cdcSinkRecords(TOPIC, PARTITION, FIRST_OFFSET, envelopes));
    }

    private Processing processing(StateProvider state, com.clickhouse.kafka.connect.sink.db.DBWriter writer) {
        return new Processing(state, writer, null, new ClickHouseSinkConfig(new HashMap<>()), statistics);
    }

    private static CapturingClickHouseWriter capturingWriter() {
        return new CapturingClickHouseWriter(stubCdcTable(true), new ClickHouseSinkConfig(new HashMap<>()));
    }

    private static void assertAfterProcessing(StateProvider state, long lastOffset) {
        StateRecord record = state.getStateRecord(TOPIC, PARTITION);
        assertEquals(State.AFTER_PROCESSING, record.getState());
        assertEquals(FIRST_OFFSET, record.getMinOffset());
        assertEquals(lastOffset, record.getMaxOffset());
    }

    private static byte[] realOnlyBody(Struct[] real) throws Exception {
        if (real.length == 0) {
            return new byte[0];
        }
        CapturingClickHouseWriter writer = capturingWriter();
        writer.doInsert(
                records(real), new QueryIdentifier(TOPIC, PARTITION, FIRST_OFFSET, FIRST_OFFSET + real.length - 1, "q"));
        return writer.captures().get(0).body();
    }

    @ParameterizedTest(name = "JSONP-09: Processing ends AFTER_PROCESSING over the full range and hands every record to the writer for the {0}")
    @MethodSource("batches")
    void stateCoversTheFullBatchWithTheInMemoryWriter(String label, Struct[] envelopes, Struct[] real, long lastOffset)
            throws Exception {
        StateProvider state = new InMemoryState();
        InMemoryDBWriter writer = new InMemoryDBWriter();
        List<Record> batch = records(envelopes);

        processing(state, writer).doLogic(batch);

        assertAfterProcessing(state, lastOffset);
        assertEquals(batch.size(), writer.recordsInserted());
    }

    @ParameterizedTest(name = "JSONP-09: Processing over the real writer ends AFTER_PROCESSING with the full-range token and only the real rows in the body for the {0}")
    @MethodSource("batches")
    void stateAndTokenCoverTheFullBatchWithTheRealWriter(String label, Struct[] envelopes, Struct[] real, long lastOffset)
            throws Exception {
        StateProvider state = new InMemoryState();
        CapturingClickHouseWriter writer = capturingWriter();

        processing(state, writer).doLogic(records(envelopes));

        assertAfterProcessing(state, lastOffset);
        assertEquals(1, writer.captures().size());
        Capture capture = writer.captures().get(0);
        assertEquals(TOPIC + "-0-10-" + lastOffset, capture.deduplicationToken());
        assertArrayEquals(realOnlyBody(real), capture.body());
    }

    @Test
    @DisplayName("JSONP-09: a batch retried from BEFORE_PROCESSING with the same range rebuilds the identical body and dedup token")
    void retriedBatchRebuildsTheIdenticalBodyAndToken() throws Exception {
        List<Record> batch = records(mixedBatchEnvelopes());
        CapturingClickHouseWriter firstWriter = capturingWriter();
        processing(new InMemoryState(), firstWriter).doLogic(batch);

        StateProvider retryState = new InMemoryState();
        retryState.setStateRecord(new StateRecord(TOPIC, PARTITION, 13, FIRST_OFFSET, State.BEFORE_PROCESSING, TOPIC));
        CapturingClickHouseWriter retryWriter = capturingWriter();
        processing(retryState, retryWriter).doLogic(batch);

        assertEquals(1, firstWriter.captures().size());
        assertEquals(1, retryWriter.captures().size());
        Capture first = firstWriter.captures().get(0);
        Capture retry = retryWriter.captures().get(0);
        assertTrue(first.body().length > 0);
        assertArrayEquals(first.body(), retry.body());
        assertEquals(TOPIC + "-0-10-13", retry.deduplicationToken());
        assertEquals(first.deduplicationToken(), retry.deduplicationToken());
        assertAfterProcessing(retryState, 13);
    }
}
