package com.clickhouse.kafka.connect.sink.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.sink.SinkRecord;

/**
 * Builds {@link SinkRecord}s as a topic-renaming SMT leaves them: the current topic is the renamed
 * one while {@code originalTopic()}, {@code originalKafkaPartition()} and {@code
 * originalKafkaOffset()} still name the position the consumer read from. Kafka Connect validates
 * the offsets a task returns from {@code preCommit} against that consumer position.
 */
public final class RenamedTopicRecords {

    private static final String ID_FIELD = "id";
    private static final String PARTITION_FIELD = "part";
    private static final String STRING_FIELD = "str";
    private static final String STRING_VALUE_FORMAT = "p%d-o%d";
    // newRecord(..., timestamp()) throws a NullPointerException on a null timestamp.
    private static final long TIMESTAMP = 1_700_000_000_000L;

    private RenamedTopicRecords() {}

    /** Schemaless records of one partition at offsets {@code firstOffset} to {@code firstOffset + count - 1}. */
    public static List<SinkRecord> schemaless(String topic, int partition, long firstOffset, int count) {
        List<SinkRecord> records = new ArrayList<>(count);
        for (long offset = firstOffset; offset < firstOffset + count; offset++) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put(ID_FIELD, offset);
            value.put(PARTITION_FIELD, partition);
            value.put(STRING_FIELD, String.format(STRING_VALUE_FORMAT, partition, offset));
            records.add(new SinkRecord(
                    topic, partition, null, null, null, value, offset, TIMESTAMP, TimestampType.CREATE_TIME));
        }
        return records;
    }

    /** What a topic-renaming SMT does: {@code newRecord} with another topic, keeping partition and payload. */
    public static List<SinkRecord> renamed(List<SinkRecord> records, String renamedTopic) {
        List<SinkRecord> renamed = new ArrayList<>(records.size());
        for (SinkRecord record : records) {
            renamed.add(record.newRecord(
                    renamedTopic,
                    record.kafkaPartition(),
                    record.keySchema(),
                    record.key(),
                    record.valueSchema(),
                    record.value(),
                    record.timestamp()));
        }
        return renamed;
    }

    /** A record whose transformed topic, partition and offset all differ from the consumer position it keeps. */
    public static SinkRecord withTransformedPosition(SinkRecord record, String topic, int partition, long offset) {
        return new SinkRecord(
                topic,
                partition,
                record.keySchema(),
                record.key(),
                record.valueSchema(),
                record.value(),
                offset,
                record.timestamp(),
                record.timestampType(),
                record.headers(),
                record.originalTopic(),
                record.originalKafkaPartition(),
                record.originalKafkaOffset());
    }

    /** The entries Kafka Connect accepts from {@code preCommit}: it ignores offsets of partitions it did not assign. */
    public static Map<TopicPartition, OffsetAndMetadata> acceptedByConnect(
            Map<TopicPartition, OffsetAndMetadata> returned, Collection<TopicPartition> assigned) {
        Map<TopicPartition, OffsetAndMetadata> accepted = new LinkedHashMap<>();
        returned.forEach((partition, offset) -> {
            if (assigned.contains(partition)) {
                accepted.put(partition, offset);
            }
        });
        return accepted;
    }
}
