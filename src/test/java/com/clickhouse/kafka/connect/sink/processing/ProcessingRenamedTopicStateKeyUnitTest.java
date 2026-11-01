package com.clickhouse.kafka.connect.sink.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.helper.RecordingDBWriter;
import com.clickhouse.kafka.connect.sink.helper.RenamedTopicRecords;
import com.clickhouse.kafka.connect.sink.state.State;
import com.clickhouse.kafka.connect.sink.state.StateRecord;
import com.clickhouse.kafka.connect.sink.state.provider.InMemoryState;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free tripwire for BUF-03: the exactly-once state key is the transformed topic and
 * partition, and any future change of that key stops for a user decision because it orphans the
 * state rows of existing exactly-once deployments. The class extends nothing so it stays in {@code
 * testUnit}. It pins only what must not move (the key, and the equal-range many-to-one case that is
 * safe today); the loss of records of a lower-offset source that shares a key is a recorded defect,
 * not a pin.
 */
class ProcessingRenamedTopicStateKeyUnitTest {

    private static final int STATISTICS_ID = 9400;
    private static final String TARGET_TOPIC = "t";
    private static final String SOURCE_TOPIC = "t-dev";
    private static final String OTHER_SOURCE_TOPIC = "b-dev";
    private static final String FIRST_SOURCE_TOPIC = "a-dev";
    private static final int PARTITION = 0;
    private static final String DEFAULT_DATABASE = "default";
    private static final int SINGLE_SOURCE_RECORDS = 10;
    private static final int BATCH_RECORDS = 100;

    private SinkTaskStatistics statistics;
    private InMemoryState state;
    private RecordingDBWriter writer;
    private Processing processing;

    @BeforeEach
    void setUp() {
        statistics = new SinkTaskStatistics(null, STATISTICS_ID);
        statistics.registerMBean();
        state = new InMemoryState();
        writer = new RecordingDBWriter();
        processing = new Processing(state, writer, null, new ClickHouseSinkConfig(new HashMap<>()), statistics);
    }

    @AfterEach
    void tearDown() {
        statistics.unregisterMBean();
    }

    private static List<SinkRecord> renamed(String source, long firstOffset, int count) {
        return RenamedTopicRecords.renamed(
                RenamedTopicRecords.schemaless(source, PARTITION, firstOffset, count), TARGET_TOPIC);
    }

    private static List<SinkRecord> interleaved(long firstOffset) {
        List<SinkRecord> first = renamed(FIRST_SOURCE_TOPIC, firstOffset, BATCH_RECORDS);
        List<SinkRecord> second = renamed(OTHER_SOURCE_TOPIC, firstOffset, BATCH_RECORDS);
        List<SinkRecord> mixed = new ArrayList<>();
        for (int i = 0; i < BATCH_RECORDS; i++) {
            mixed.add(first.get(i));
            mixed.add(second.get(i));
        }
        return mixed;
    }

    private void process(List<SinkRecord> sinkRecords) {
        List<Record> records = new ArrayList<>();
        for (SinkRecord sinkRecord : sinkRecords) {
            records.add(Record.convert(sinkRecord, false, ".", DEFAULT_DATABASE, false));
        }
        processing.doLogic(records);
    }

    private void assertState(String topic, State expectedState, String message) {
        assertEquals(expectedState, state.getStateRecord(topic, PARTITION).getState(), message);
    }

    @Test
    @DisplayName("BUF-03: Processing keeps the exactly-once state of renamed records under the transformed topic and partition")
    void stateOfRenamedRecordsIsKeyedByTransformedTopic() {
        process(renamed(SOURCE_TOPIC, 0, SINGLE_SOURCE_RECORDS));

        assertEquals(SINGLE_SOURCE_RECORDS, writer.inserted().size(), "records written");
        assertState(TARGET_TOPIC, State.AFTER_PROCESSING, "state under the transformed topic");
        StateRecord stored = state.getStateRecord(TARGET_TOPIC, PARTITION);
        assertEquals(0, stored.getMinOffset(), "minimum offset of the stored range");
        assertEquals(SINGLE_SOURCE_RECORDS - 1, stored.getMaxOffset(), "maximum offset of the stored range");
        assertState(SOURCE_TOPIC, State.NONE, "no state under the original topic");
    }

    @Test
    @DisplayName("BUF-03: two renamed sources with equal offset ranges are written in full under one shared state record")
    void equalRangeSourcesShareOneStateRecord() {
        process(interleaved(0));
        process(interleaved(BATCH_RECORDS));

        assertEquals(4 * BATCH_RECORDS, writer.inserted().size(), "records written");
        assertState(TARGET_TOPIC, State.AFTER_PROCESSING, "shared state under the transformed topic");
        assertEquals(2 * BATCH_RECORDS - 1, state.getStateRecord(TARGET_TOPIC, PARTITION).getMaxOffset(),
                "maximum offset of the shared range");
        assertState(FIRST_SOURCE_TOPIC, State.NONE, "no state under the first original topic");
        assertState(OTHER_SOURCE_TOPIC, State.NONE, "no state under the second original topic");
    }
}
