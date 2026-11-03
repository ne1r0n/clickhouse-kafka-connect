package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.dlq.ErrorReporter;
import com.clickhouse.kafka.connect.sink.helper.RecordingDBWriter;
import com.clickhouse.kafka.connect.sink.helper.RecordingDBWriter.Inserted;
import com.clickhouse.kafka.connect.sink.helper.RenamedTopicRecords;
import com.clickhouse.kafka.connect.sink.processing.Processing;
import com.clickhouse.kafka.connect.sink.state.State;
import com.clickhouse.kafka.connect.sink.state.StateRecord;
import com.clickhouse.kafka.connect.sink.state.provider.InMemoryState;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pins for the consumer's topic in the inserted-offset key of {@code
 * Processing.doLogic}: the direct delivery path must report, and a revoked partition must clear,
 * the offsets under the topic Kafka Connect assigned, while the exactly-once state key stays on the
 * transformed topic. The two cases named {@code limit...} characterize configurations outside the
 * supported limits (a many-to-one rename and a partition-changing SMT); they record what happens
 * there, not behaviour anyone should rely on. The class extends nothing, so it stays in {@code
 * testUnit}.
 */
class OriginalTopicInsertedOffsetsUnitTest {

    private static final String SOURCE_TOPIC = "orig-dev";
    private static final String TARGET_TOPIC = "orig";
    private static final String FIRST_SOURCE_TOPIC = "orig-a-dev";
    private static final String SECOND_SOURCE_TOPIC = "orig-b-dev";
    private static final String PLAIN_TOPIC = "orig-plain";
    private static final int PARTITION = 0;
    private static final int MOVED_FROM_PARTITION = 3;
    private static final int BATCH = 10;
    private static final long SECOND_SOURCE_FIRST_OFFSET = 100L;
    private static final int STATISTICS_ID = 9420;
    private static final String DEFAULT_DATABASE = "default";
    private static final ErrorReporter NO_ERROR_REPORTER = null;

    private static final String GET_FORMAT = "get %s-%d";
    private static final String SET_FORMAT = "set %s %s %d..%d";
    private static final String SET_PREFIX = "set ";
    private static final String GET_PREFIX = "get ";
    private static final String STATE_KEY = "orig-0";
    private static final int GUARD_LAST_OFFSET = 24;

    private static final String INSERT_COUNT_MESSAGE = "records written";
    private static final String TABLE_TOPIC_MESSAGE = "table topic of every written record";
    private static final String REPORTED_MESSAGE = "inserted offsets reported to Connect";
    private static final String CLEARED_MESSAGE = "inserted offsets left after the partition was revoked";
    private static final String SET_SEQUENCE_MESSAGE = "state records stored, in order";
    private static final String GET_KEY_MESSAGE = "every state record is read under the transformed key";
    private static final String NO_CONSUMER_TOPIC_MESSAGE = "no state call names the consumer's topic";
    private static final String WRITTEN_OFFSETS_MESSAGE = "offsets written, in order";
    private static final String NO_STATE_UNDER_CONSUMER_TOPIC_MESSAGE = "no state under the consumer's topic";
    private static final String SHARED_MAXIMUM_MESSAGE =
            "many-to-one rename: the shared range maximum under the first source topic";
    private static final String TRANSFORMED_PARTITION_MESSAGE =
            "partition-changing SMT: the transformed partition in the key";
    private static final String KEPT_ENTRY_MESSAGE = "entry of the consumer's topic with the transformed partition after close";
    private static final String PLAIN_REPORTED_MESSAGE = "records without a rename keep their own topic";
    private static final String PLAIN_CLEARED_MESSAGE = "records without a rename are cleared on close";

    /** State provider that records the order of the state calls {@code Processing} makes. */
    private static final class RecordingState extends InMemoryState {
        private final List<String> calls = new ArrayList<>();

        @Override
        public StateRecord getStateRecord(String topic, int partition) {
            calls.add(String.format(GET_FORMAT, topic, partition));
            return super.getStateRecord(topic, partition);
        }

        @Override
        public void setStateRecord(StateRecord stateRecord) {
            calls.add(String.format(
                    SET_FORMAT,
                    stateRecord.getTopicAndPartitionKey(),
                    stateRecord.getState(),
                    stateRecord.getMinOffset(),
                    stateRecord.getMaxOffset()));
            super.setStateRecord(stateRecord);
        }

        List<String> calls() {
            return new ArrayList<>(calls);
        }
    }

    private final List<ProxySinkTask> startedTasks = new ArrayList<>();

    @AfterEach
    void stopTasks() {
        // stop() unregisters the task MBeans; without it later tests hit InstanceAlreadyExistsException.
        startedTasks.forEach(ProxySinkTask::stop);
        startedTasks.clear();
    }

    private static ClickHouseSinkConfig reportingConfig() {
        return new ClickHouseSinkConfig(
                new HashMap<>(Map.of(ClickHouseSinkConfig.REPORT_INSERTED_OFFSETS, Boolean.TRUE.toString())));
    }

    private ProxySinkTask newTask(ClickHouseSinkConfig config, RecordingDBWriter writer) {
        ProxySinkTask task = new ProxySinkTask(config, NO_ERROR_REPORTER, writer);
        startedTasks.add(task);
        return task;
    }

    private static DirectDeliveryStrategy direct(ProxySinkTask task, ClickHouseSinkConfig config) {
        return new DirectDeliveryStrategy(task, config, new ChunkFlusher(task, config, NO_ERROR_REPORTER));
    }

    private static List<SinkRecord> renamed(String source, long firstOffset, int count) {
        return RenamedTopicRecords.renamed(
                RenamedTopicRecords.schemaless(source, PARTITION, firstOffset, count), TARGET_TOPIC);
    }

    private static TopicPartition partition(String topic, int partition) {
        return new TopicPartition(topic, partition);
    }

    private static Map<TopicPartition, OffsetAndMetadata> offsets(Map<TopicPartition, Long> nextOffsets) {
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        nextOffsets.forEach((tp, offset) -> offsets.put(tp, new OffsetAndMetadata(offset)));
        return offsets;
    }

    private static Set<String> tableTopics(RecordingDBWriter writer) {
        return writer.inserted().stream().map(Inserted::tableTopic).collect(Collectors.toSet());
    }

    private static void process(Processing processing, List<SinkRecord> sinkRecords) {
        List<Record> records = new ArrayList<>();
        for (SinkRecord sinkRecord : sinkRecords) {
            records.add(Record.convert(sinkRecord, false, ".", DEFAULT_DATABASE, false));
        }
        processing.doLogic(records);
    }

    @Test
    void directDeliveryReportsTheConsumersTopicUnderAnInjectiveRename() {
        ClickHouseSinkConfig config = reportingConfig();
        RecordingDBWriter writer = new RecordingDBWriter();
        ProxySinkTask task = newTask(config, writer);
        DirectDeliveryStrategy strategy = direct(task, config);

        strategy.put(renamed(SOURCE_TOPIC, 0, BATCH));

        assertEquals(BATCH, writer.inserted().size(), INSERT_COUNT_MESSAGE);
        assertEquals(Set.of(TARGET_TOPIC), tableTopics(writer), TABLE_TOPIC_MESSAGE);
        assertEquals(
                offsets(Map.of(partition(SOURCE_TOPIC, PARTITION), (long) BATCH)),
                strategy.preCommit(offsets(Map.of(partition(SOURCE_TOPIC, PARTITION), (long) BATCH))),
                REPORTED_MESSAGE);
    }

    @Test
    void closeClearsTheInsertedOffsetOfTheRevokedConsumerPartition() {
        ClickHouseSinkConfig config = reportingConfig();
        ProxySinkTask task = newTask(config, new RecordingDBWriter());

        direct(task, config).put(renamed(SOURCE_TOPIC, 0, BATCH));
        task.onPartitionRemoved(List.of(partition(SOURCE_TOPIC, PARTITION)));

        assertEquals(Map.of(), task.getInsertedOffsetsSnapshot(), CLEARED_MESSAGE);
    }

    @Test
    void stateKeyAndFlagSequenceStayOnTheTransformedTopicUnderAnInjectiveRename() {
        SinkTaskStatistics statistics = new SinkTaskStatistics(null, STATISTICS_ID);
        statistics.registerMBean();
        try {
            RecordingState state = new RecordingState();
            RecordingDBWriter writer = new RecordingDBWriter();
            Processing processing = new Processing(
                    state, writer, NO_ERROR_REPORTER, new ClickHouseSinkConfig(new HashMap<>()), statistics);

            process(processing, renamed(SOURCE_TOPIC, 0, BATCH));
            process(processing, renamed(SOURCE_TOPIC, 0, BATCH));
            process(processing, renamed(SOURCE_TOPIC, 5, BATCH));
            process(processing, renamed(SOURCE_TOPIC, 15, BATCH));

            List<String> calls = state.calls();
            assertEquals(
                    List.of(
                            "set orig-0 BEFORE_PROCESSING 0..9",
                            "set orig-0 AFTER_PROCESSING 0..9",
                            "set orig-0 BEFORE_PROCESSING 10..14",
                            "set orig-0 AFTER_PROCESSING 10..14",
                            "set orig-0 BEFORE_PROCESSING 15..24",
                            "set orig-0 AFTER_PROCESSING 15..24"),
                    calls.stream().filter(call -> call.startsWith(SET_PREFIX)).collect(Collectors.toList()),
                    SET_SEQUENCE_MESSAGE);
            assertTrue(
                    calls.stream()
                            .filter(call -> call.startsWith(GET_PREFIX))
                            .allMatch(call -> call.equals(GET_PREFIX + STATE_KEY)),
                    GET_KEY_MESSAGE);
            assertFalse(calls.stream().anyMatch(call -> call.contains(SOURCE_TOPIC)), NO_CONSUMER_TOPIC_MESSAGE);
            assertEquals(
                    LongStream.rangeClosed(0, GUARD_LAST_OFFSET).boxed().collect(Collectors.toList()),
                    writer.inserted().stream().map(Inserted::offset).collect(Collectors.toList()),
                    WRITTEN_OFFSETS_MESSAGE);
            assertEquals(Set.of(TARGET_TOPIC), tableTopics(writer), TABLE_TOPIC_MESSAGE);
            assertEquals(
                    State.NONE,
                    state.getStateRecord(SOURCE_TOPIC, PARTITION).getState(),
                    NO_STATE_UNDER_CONSUMER_TOPIC_MESSAGE);
        } finally {
            statistics.unregisterMBean();
        }
    }

    @Test
    void limitManyToOneRenameReportsTheSharedMaximumForTheFirstSourceTopic() {
        ClickHouseSinkConfig config = reportingConfig();
        ProxySinkTask task = newTask(config, new RecordingDBWriter());
        DirectDeliveryStrategy strategy = direct(task, config);
        List<SinkRecord> both = new ArrayList<>(renamed(FIRST_SOURCE_TOPIC, 0, BATCH));
        both.addAll(renamed(SECOND_SOURCE_TOPIC, SECOND_SOURCE_FIRST_OFFSET, BATCH));

        strategy.put(both);

        // Pins a limit, not a wanted behaviour: the first source topic is reported past its own next offset.
        long sharedNext = SECOND_SOURCE_FIRST_OFFSET + BATCH;
        assertEquals(
                offsets(Map.of(partition(FIRST_SOURCE_TOPIC, PARTITION), sharedNext)),
                strategy.preCommit(offsets(Map.of(
                        partition(FIRST_SOURCE_TOPIC, PARTITION), (long) BATCH,
                        partition(SECOND_SOURCE_TOPIC, PARTITION), sharedNext))),
                SHARED_MAXIMUM_MESSAGE);
    }

    @Test
    void limitPartitionChangingSmtKeepsTheTransformedPartitionInTheKey() {
        ClickHouseSinkConfig config = reportingConfig();
        ProxySinkTask task = newTask(config, new RecordingDBWriter());
        DirectDeliveryStrategy strategy = direct(task, config);
        List<SinkRecord> moved = new ArrayList<>();
        for (SinkRecord consumed :
                RenamedTopicRecords.schemaless(SOURCE_TOPIC, MOVED_FROM_PARTITION, 0, BATCH)) {
            moved.add(RenamedTopicRecords.withTransformedPosition(
                    consumed, TARGET_TOPIC, PARTITION, consumed.kafkaOffset()));
        }

        strategy.put(moved);

        // Pins a limit, not a wanted behaviour: the entry carries the transformed partition, not the consumer's.
        Map<TopicPartition, OffsetAndMetadata> expected =
                offsets(Map.of(partition(SOURCE_TOPIC, PARTITION), (long) BATCH));
        assertEquals(
                expected,
                strategy.preCommit(offsets(Map.of(
                        partition(SOURCE_TOPIC, PARTITION), 0L,
                        partition(SOURCE_TOPIC, MOVED_FROM_PARTITION), (long) BATCH))),
                TRANSFORMED_PARTITION_MESSAGE);
        task.onPartitionRemoved(List.of(partition(SOURCE_TOPIC, MOVED_FROM_PARTITION)));
        assertEquals(expected, task.getInsertedOffsetsSnapshot(), KEPT_ENTRY_MESSAGE);
    }

    @Test
    void noSmtRecordsKeepTheirTopicAndAreClearedOnClose() {
        ClickHouseSinkConfig config = reportingConfig();
        ProxySinkTask task = newTask(config, new RecordingDBWriter());
        DirectDeliveryStrategy strategy = direct(task, config);

        strategy.put(RenamedTopicRecords.schemaless(PLAIN_TOPIC, PARTITION, 0, BATCH));

        assertEquals(
                offsets(Map.of(partition(PLAIN_TOPIC, PARTITION), (long) BATCH)),
                strategy.preCommit(offsets(Map.of(partition(PLAIN_TOPIC, PARTITION), (long) BATCH))),
                PLAIN_REPORTED_MESSAGE);
        task.onPartitionRemoved(List.of(partition(PLAIN_TOPIC, PARTITION)));
        assertEquals(Map.of(), task.getInsertedOffsetsSnapshot(), PLAIN_CLEARED_MESSAGE);
    }
}
