package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.clickhouse.kafka.connect.sink.dlq.ErrorReporter;
import com.clickhouse.kafka.connect.sink.helper.RecordingDBWriter;
import com.clickhouse.kafka.connect.sink.helper.RecordingDBWriter.Inserted;
import com.clickhouse.kafka.connect.sink.helper.RenamedTopicRecords;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Docker-free pins for the buffered offset contract under topic-renaming SMTs (upstream #777): the
 * offsets a buffered strategy returns from {@code preCommit} and the partitions it drops on {@code
 * close} must be the consumer's original ones, because Kafka Connect only accepts and revokes
 * those, while table routing keeps following the renamed topic. Records are built through {@code
 * SinkRecord.newRecord}, as a renaming SMT does. The class deliberately extends nothing, so it
 * stays in {@code testUnit}: extending {@code ClickHouseBase} would inherit its clickhouse tag and
 * drop it out of the Docker-free loop.
 */
class BufferedOffsetCommitUnitTest {

    private static final String ORIGINAL_TOPIC = "buf35-dev";
    private static final String RENAMED_TOPIC = "buf35";
    private static final String PLAIN_TOPIC = "buf35-plain";
    private static final int BUFFER_COUNT = 10;
    private static final int TRANSFORMED_PARTITION = 0;
    private static final int MOVED_PARTITION = 3;
    private static final long MOVED_FIRST_OFFSET = 500L;
    private static final int REVOKED_ORIGINAL_PARTITION = 1;
    private static final int KEPT_ORIGINAL_PARTITION = 2;
    private static final long KEPT_FIRST_OFFSET = 20L;
    private static final ErrorReporter NO_ERROR_REPORTER = null;

    enum StrategyKind {
        AT_LEAST_ONCE,
        EXACTLY_ONCE
    }

    private final List<ProxySinkTask> startedTasks = new ArrayList<>();

    @AfterEach
    void stopTasks() {
        // stop() unregisters the task and topic MBeans; without it later tests hit InstanceAlreadyExistsException.
        startedTasks.forEach(ProxySinkTask::stop);
        startedTasks.clear();
    }

    private static ClickHouseSinkConfig config(Map<String, String> props) {
        return new ClickHouseSinkConfig(new HashMap<>(props));
    }

    private static TopicPartition partition(String topic, int partition) {
        return new TopicPartition(topic, partition);
    }

    private static Map<TopicPartition, Long> nextOffsets(Map<TopicPartition, OffsetAndMetadata> committed) {
        Map<TopicPartition, Long> offsets = new HashMap<>();
        committed.forEach((tp, offset) -> offsets.put(tp, offset.offset()));
        return offsets;
    }

    private static List<SinkRecord> concat(List<SinkRecord> first, List<SinkRecord> second) {
        List<SinkRecord> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }

    private static List<SinkRecord> renamedRecords(int partition, long firstOffset, int count) {
        return RenamedTopicRecords.renamed(
                RenamedTopicRecords.schemaless(ORIGINAL_TOPIC, partition, firstOffset, count), RENAMED_TOPIC);
    }

    /**
     * Records of one original partition as an SMT that renames the topic and moves every record onto
     * {@code TRANSFORMED_PARTITION} leaves them. The transformed offsets are unique across calls so the
     * records of different original partitions never look like a redelivery to the state provider.
     */
    private static List<SinkRecord> movedRecords(
            int originalPartition, long firstOffset, int count, long transformedFirstOffset) {
        List<SinkRecord> consumed = RenamedTopicRecords.schemaless(ORIGINAL_TOPIC, originalPartition, firstOffset, count);
        List<SinkRecord> moved = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            moved.add(RenamedTopicRecords.withTransformedPosition(
                    consumed.get(i), RENAMED_TOPIC, TRANSFORMED_PARTITION, transformedFirstOffset + i));
        }
        return moved;
    }

    private static Set<Inserted> expectedInserted(
            String originalTopic, String tableTopic, int partition, long firstOffset, int count) {
        Set<Inserted> expected = new HashSet<>();
        for (long offset = firstOffset; offset < firstOffset + count; offset++) {
            expected.add(new Inserted(originalTopic, partition, offset, tableTopic));
        }
        return expected;
    }

    private static void assertWritten(Set<Inserted> expected, RecordingDBWriter writer, String message) {
        List<Inserted> written = writer.inserted();
        assertEquals(expected.size(), written.size(), message + ": number of records written, " + written);
        assertEquals(expected, new HashSet<>(written), message + ": records written");
    }

    private ClickHouseSinkConfig bufferedConfig() {
        return config(Map.of(ClickHouseSinkConfig.BUFFER_COUNT, String.valueOf(BUFFER_COUNT)));
    }

    private ProxySinkTask newTask(ClickHouseSinkConfig config, RecordingDBWriter writer) {
        ProxySinkTask task = new ProxySinkTask(config, NO_ERROR_REPORTER, writer);
        startedTasks.add(task);
        return task;
    }

    /** One buffered strategy over a recording writer; {@code EXACTLY_ONCE} only selects the buffer strategy. */
    private final class Harness {
        final RecordingDBWriter writer = new RecordingDBWriter();
        private final DeliveryStrategy strategy;

        Harness(StrategyKind kind) {
            ClickHouseSinkConfig config = bufferedConfig();
            ChunkFlusher flusher = new ChunkFlusher(newTask(config, writer), config, NO_ERROR_REPORTER);
            strategy = kind == StrategyKind.AT_LEAST_ONCE
                    ? new AtLeastOnceBufferStrategy(flusher, BUFFER_COUNT, 0L)
                    : new ExactlyOnceBufferStrategy(flusher, BUFFER_COUNT);
        }

        void put(List<SinkRecord> records) {
            strategy.put(records);
        }

        void close(TopicPartition... revoked) {
            strategy.close(List.of(revoked));
        }

        Map<TopicPartition, OffsetAndMetadata> preCommit() {
            return strategy.preCommit(Map.of());
        }
    }

    @ParameterizedTest(name = "BUF-01: preCommit keys the flushed offsets by the original topic and partition at the original offset plus one [{0}]")
    @EnumSource(StrategyKind.class)
    void preCommitKeysTheFlushedOffsetsByTheOriginalTopicAndPartition(StrategyKind kind) {
        Harness harness = new Harness(kind);
        harness.put(concat(renamedRecords(0, 40, 10), renamedRecords(1, 100, 10)));

        Map<TopicPartition, OffsetAndMetadata> committed = harness.preCommit();

        Map<TopicPartition, Long> expected =
                Map.of(partition(ORIGINAL_TOPIC, 0), 50L, partition(ORIGINAL_TOPIC, 1), 110L);
        assertEquals(expected, nextOffsets(committed), "preCommit must be keyed by the consumer's topic and partition");
        assertEquals(
                committed,
                RenamedTopicRecords.acceptedByConnect(committed, expected.keySet()),
                "Kafka Connect must accept every returned offset");
    }

    @ParameterizedTest(name = "BUF-01: preCommit follows the original partition and offset when the SMT changes them as well [{0}]")
    @EnumSource(StrategyKind.class)
    void preCommitFollowsTheOriginalPartitionAndOffsetWhenTheSmtChangesThemAsWell(StrategyKind kind) {
        Harness harness = new Harness(kind);
        List<SinkRecord> consumed =
                RenamedTopicRecords.schemaless(ORIGINAL_TOPIC, MOVED_PARTITION, MOVED_FIRST_OFFSET, BUFFER_COUNT);
        List<SinkRecord> transformed = new ArrayList<>();
        for (int i = 0; i < consumed.size(); i++) {
            transformed.add(RenamedTopicRecords.withTransformedPosition(
                    consumed.get(i), RENAMED_TOPIC, TRANSFORMED_PARTITION, i));
        }
        harness.put(transformed);

        Map<TopicPartition, OffsetAndMetadata> committed = harness.preCommit();

        Map<TopicPartition, Long> expected =
                Map.of(partition(ORIGINAL_TOPIC, MOVED_PARTITION), MOVED_FIRST_OFFSET + BUFFER_COUNT);
        assertEquals(expected, nextOffsets(committed), "preCommit must report the consumer's partition and offset");
        assertEquals(
                committed,
                RenamedTopicRecords.acceptedByConnect(committed, expected.keySet()),
                "Kafka Connect must accept every returned offset");
    }

    @Test
    @DisplayName("BUF-02: at-least-once close drops the buffered records of the revoked original partition and keeps the others")
    void atLeastOnceCloseDropsTheBufferedRecordsOfTheRevokedOriginalPartition() {
        Harness harness = new Harness(StrategyKind.AT_LEAST_ONCE);
        harness.put(concat(renamedRecords(0, 0, 3), renamedRecords(1, 0, 3)));
        harness.close(partition(ORIGINAL_TOPIC, 1));

        harness.put(renamedRecords(0, 3, 7));

        assertWritten(
                expectedInserted(ORIGINAL_TOPIC, RENAMED_TOPIC, 0, 0, BUFFER_COUNT),
                harness.writer,
                "Only the records of the partition that stayed assigned may be flushed");
    }

    @Test
    @DisplayName("BUF-02: exactly-once buffer groups by the original partition so a revoked partition leaves no stale records in a later chunk")
    void exactlyOnceBufferGroupsByTheOriginalPartition() {
        Harness harness = new Harness(StrategyKind.EXACTLY_ONCE);
        harness.put(concat(renamedRecords(0, 0, 3), renamedRecords(1, 0, 3)));
        harness.close(partition(ORIGINAL_TOPIC, 1));

        harness.put(concat(renamedRecords(0, 3, 7), renamedRecords(1, 0, 7)));

        assertWritten(
                expectedInserted(ORIGINAL_TOPIC, RENAMED_TOPIC, 0, 0, BUFFER_COUNT),
                harness.writer,
                "The re-delivered partition must stay buffered without its stale records");
    }

    @ParameterizedTest(name = "BUF-01: close forgets the flushed offsets of the revoked original partition only [{0}]")
    @EnumSource(StrategyKind.class)
    void closeForgetsTheFlushedOffsetsOfTheRevokedOriginalPartitionOnly(StrategyKind kind) {
        Harness harness = new Harness(kind);
        harness.put(concat(renamedRecords(0, 40, 10), renamedRecords(1, 100, 10)));
        harness.close(partition(ORIGINAL_TOPIC, 0));

        Map<TopicPartition, OffsetAndMetadata> committed = harness.preCommit();

        assertEquals(
                Map.of(partition(ORIGINAL_TOPIC, 1), 110L),
                nextOffsets(committed),
                "Only the partition that stayed assigned may still be committed");
    }

    @ParameterizedTest(name = "BUF-02: close and put key the buffer by the original partition when the SMT moves every record onto one transformed partition [{0}]")
    @EnumSource(StrategyKind.class)
    void closeAndPutKeyTheBufferByTheOriginalPartitionWhenTheSmtMovesThePartition(StrategyKind kind) {
        Harness harness = new Harness(kind);
        harness.put(concat(
                movedRecords(REVOKED_ORIGINAL_PARTITION, 0, 3, 0),
                movedRecords(KEPT_ORIGINAL_PARTITION, KEPT_FIRST_OFFSET, 3, 3)));
        harness.close(partition(ORIGINAL_TOPIC, REVOKED_ORIGINAL_PARTITION));

        harness.put(movedRecords(KEPT_ORIGINAL_PARTITION, KEPT_FIRST_OFFSET + 3, 7, 6));

        List<Inserted> written = harness.writer.inserted();
        assertEquals(BUFFER_COUNT, written.size(), "Records written, " + written);
        assertEquals(
                Set.of(KEPT_ORIGINAL_PARTITION),
                written.stream().map(Inserted::originalPartition).collect(Collectors.toSet()),
                "Only records of the original partition that stayed assigned may be flushed");
        assertEquals(
                Map.of(partition(ORIGINAL_TOPIC, KEPT_ORIGINAL_PARTITION), KEPT_FIRST_OFFSET + BUFFER_COUNT),
                nextOffsets(harness.preCommit()),
                "preCommit must report the original partition that stayed assigned");
    }

    @ParameterizedTest(name = "BUF-04: records whose topic was not renamed commit and drop exactly as before [{0}]")
    @EnumSource(StrategyKind.class)
    void recordsWhoseTopicWasNotRenamedCommitAndDropExactlyAsBefore(StrategyKind kind) {
        Harness harness = new Harness(kind);
        harness.put(concat(
                RenamedTopicRecords.schemaless(PLAIN_TOPIC, 0, 40, 10),
                RenamedTopicRecords.schemaless(PLAIN_TOPIC, 1, 100, 10)));

        assertEquals(
                Map.of(partition(PLAIN_TOPIC, 0), 50L, partition(PLAIN_TOPIC, 1), 110L),
                nextOffsets(harness.preCommit()),
                "Offsets of records that were never renamed");

        harness.put(concat(
                RenamedTopicRecords.schemaless(PLAIN_TOPIC, 0, 50, 3),
                RenamedTopicRecords.schemaless(PLAIN_TOPIC, 1, 110, 3)));
        harness.close(partition(PLAIN_TOPIC, 1));
        harness.put(RenamedTopicRecords.schemaless(PLAIN_TOPIC, 0, 53, 7));

        Set<Inserted> expected = expectedInserted(PLAIN_TOPIC, PLAIN_TOPIC, 0, 40, 20);
        expected.addAll(expectedInserted(PLAIN_TOPIC, PLAIN_TOPIC, 1, 100, 10));
        assertWritten(expected, harness.writer, "Records written before and after the revocation");
        assertEquals(
                Map.of(partition(PLAIN_TOPIC, 0), 60L),
                nextOffsets(harness.preCommit()),
                "Offsets after the revocation");
    }

    @Test
    @DisplayName("BUF-05: the direct delivery strategy returns the current offsets unchanged while reportInsertedOffsets is off")
    void directDeliveryStrategyReturnsTheCurrentOffsetsUnchanged() {
        ClickHouseSinkConfig config = config(Map.of(ClickHouseSinkConfig.REPORT_INSERTED_OFFSETS, "false"));
        RecordingDBWriter writer = new RecordingDBWriter();
        ProxySinkTask task = newTask(config, writer);
        DirectDeliveryStrategy strategy =
                new DirectDeliveryStrategy(task, config, new ChunkFlusher(task, config, NO_ERROR_REPORTER));

        strategy.put(renamedRecords(0, 0, 10));
        Map<TopicPartition, OffsetAndMetadata> currentOffsets =
                Map.of(partition(ORIGINAL_TOPIC, 0), new OffsetAndMetadata(10));

        assertEquals(10, writer.inserted().size(), "Direct delivery inserts every put at once");
        assertSame(currentOffsets, strategy.preCommit(currentOffsets), "Direct delivery hands Connect its own offsets back");
        assertFalse(ClickHouseSinkTask.isBufferingEnabled(config), "bufferCount is unset");
    }

    @ParameterizedTest(name = "BUF-02: flushed chunks are still inserted under the renamed topic [{0}]")
    @EnumSource(StrategyKind.class)
    void flushedChunksAreStillInsertedUnderTheRenamedTopic(StrategyKind kind) {
        Harness harness = new Harness(kind);
        harness.put(renamedRecords(0, 0, BUFFER_COUNT));

        List<Inserted> written = harness.writer.inserted();

        assertEquals(BUFFER_COUNT, written.size(), "Records written");
        assertEquals(
                Set.of(RENAMED_TOPIC),
                written.stream().map(Inserted::tableTopic).collect(Collectors.toSet()),
                "Table routing must follow the renamed topic");
        assertEquals(
                Set.of(ORIGINAL_TOPIC),
                written.stream().map(Inserted::originalTopic).collect(Collectors.toSet()),
                "The consumer's topic must stay available on the written records");
    }
}
