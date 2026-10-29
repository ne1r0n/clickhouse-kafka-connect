package com.clickhouse.kafka.connect.sink.helper;

import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.db.InMemoryDBWriter;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.connect.sink.SinkRecord;

/**
 * {@link InMemoryDBWriter} that remembers every inserted record in insertion order. The parent keys
 * its store by offset only, so two partitions with equal offsets collide there; this writer is the
 * source of truth for what was written.
 */
public final class RecordingDBWriter extends InMemoryDBWriter {

    /** Consumer position and table topic of one inserted record. */
    public record Inserted(String originalTopic, int originalPartition, long offset, String tableTopic) {}

    private final List<Inserted> inserted = new ArrayList<>();

    @Override
    public void doInsert(List<Record> records, QueryIdentifier queryId) {
        for (Record record : records) {
            SinkRecord sinkRecord = record.getSinkRecord();
            inserted.add(new Inserted(
                    sinkRecord.originalTopic(),
                    sinkRecord.originalKafkaPartition(),
                    sinkRecord.kafkaOffset(),
                    record.getTopic()));
        }
        super.doInsert(records, queryId);
    }

    public List<Inserted> inserted() {
        return Collections.unmodifiableList(new ArrayList<>(inserted));
    }
}
