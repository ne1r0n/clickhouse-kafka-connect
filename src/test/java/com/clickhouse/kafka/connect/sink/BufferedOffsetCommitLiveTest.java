package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.helper.CreateTableStatement;
import com.clickhouse.kafka.connect.sink.helper.RenamedTopicRecords;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * ClickHouse-backed pin for BUF-05 and BUF-02: a buffered sink under a topic-renaming SMT inserts
 * every row and commits the end offset of every original partition it owns. It extends {@code
 * ClickHouseBase} because the chunk flush must reach a live ClickHouse insert before the offsets
 * are meaningful. It observes the offsets {@code ClickHouseSinkTask.preCommit} returns, which Kafka
 * Connect commits after dropping the keys outside its assignment; that drop is applied by {@link
 * RenamedTopicRecords#acceptedByConnect} and asserted to remove nothing.
 */
public class BufferedOffsetCommitLiveTest extends ClickHouseBase {

    private static final String CONNECTOR_NAME_PROPERTY = "name";
    private static final String DEV_SUFFIX = "-dev";
    private static final String STR_COLUMN = "str";
    private static final int PARTITION_COUNT = 3;
    private static final int RECORDS_PER_PARTITION = 100;
    private static final long FIRST_OFFSET = 1000;
    private static final long END_OFFSET = FIRST_OFFSET + RECORDS_PER_PARTITION;
    private static final String ALL_PARTITIONS_BUFFER_COUNT = String.valueOf(PARTITION_COUNT * RECORDS_PER_PARTITION);

    private static final String REVOCATION_BUFFER_COUNT = "400";
    private static final int REVOKED_PARTITION = 1;
    private static final int KEPT_PARTITION = 0;
    private static final int IDLE_PARTITION = 2;
    private static final int FLUSHING_RECORDS = 200;

    private static final Supplier<CreateTableStatement> TABLE = () -> CreateTableStatement.create()
            .column("id", "Int64")
            .column("part", "Int32")
            .column(STR_COLUMN, "String")
            .orderByColumn("id");

    /** The connector configuration, ClickHouse client and table one test works with. */
    private record Scenario(Map<String, String> props, ClickHouseHelperClient chc, String table) {}

    @FunctionalInterface
    private interface TaskBody {
        void run(ClickHouseSinkTask task);
    }

    @Test
    @DisplayName("BUF-05: buffered sink under a topic-renaming SMT commits the end offset of every assigned original partition and inserts every row")
    void bufferedRenamedSinkCommitsEndOffsetOfEveryAssignedPartition(TestInfo testInfo) {
        Scenario scenario = scenario(testInfo, "buf05_renamed", ALL_PARTITIONS_BUFFER_COUNT);
        String source = scenario.table() + DEV_SUFFIX;
        List<SinkRecord> records = allPartitions(source, scenario.table());
        Map<TopicPartition, OffsetAndMetadata> expected = endOffsets(source, PARTITION_COUNT, END_OFFSET);

        runScenario(scenario, records, task -> {
            task.put(records);

            Map<TopicPartition, OffsetAndMetadata> committed = task.preCommit(new HashMap<>(expected));
            assertEquals(expected, committed, "preCommit must be keyed by the original topic and partition");
            assertEquals(committed, RenamedTopicRecords.acceptedByConnect(committed, expected.keySet()),
                    "Connect must ignore none of the returned offsets");
        });
    }

    @Test
    @DisplayName("BUF-02: buffered sink under a topic-renaming SMT drops the revoked partition and commits only the partitions it still owns")
    void bufferedRenamedSinkDropsRevokedPartition(TestInfo testInfo) {
        Scenario scenario = scenario(testInfo, "buf02_revoked", REVOCATION_BUFFER_COUNT);
        String source = scenario.table() + DEV_SUFFIX;
        List<SinkRecord> firstPut = new ArrayList<>();
        for (int partition = 0; partition < PARTITION_COUNT; partition++) {
            firstPut.addAll(partitionRecords(source, scenario.table(), partition, 0, RECORDS_PER_PARTITION));
        }
        List<SinkRecord> flushingPut =
                partitionRecords(source, scenario.table(), KEPT_PARTITION, RECORDS_PER_PARTITION, FLUSHING_RECORDS);

        List<SinkRecord> expectedRows = new ArrayList<>();
        for (SinkRecord record : firstPut) {
            if (record.originalKafkaPartition() != REVOKED_PARTITION) {
                expectedRows.add(record);
            }
        }
        expectedRows.addAll(flushingPut);
        Map<TopicPartition, OffsetAndMetadata> expected = new HashMap<>();
        expected.put(new TopicPartition(source, KEPT_PARTITION),
                new OffsetAndMetadata(RECORDS_PER_PARTITION + FLUSHING_RECORDS));
        expected.put(new TopicPartition(source, IDLE_PARTITION), new OffsetAndMetadata(RECORDS_PER_PARTITION));

        runScenario(scenario, expectedRows, task -> {
            task.put(firstPut);
            assertEquals(0, ClickHouseTestHelpers.countRows(scenario.chc(), scenario.table()),
                    "300 of 400 records must still be buffered");

            task.close(List.of(new TopicPartition(source, REVOKED_PARTITION)));
            task.put(flushingPut);

            Map<TopicPartition, OffsetAndMetadata> committed = task.preCommit(new HashMap<>(expected));
            assertEquals(expected, committed, "preCommit must hold only the partitions the task still owns");
            assertEquals(committed, RenamedTopicRecords.acceptedByConnect(committed, expected.keySet()),
                    "Connect must ignore none of the returned offsets");
        });
    }

    @Test
    @DisplayName("BUF-05: buffered sink whose topic is not renamed commits the same end offsets as before")
    void bufferedSinkWithoutRenameCommitsEndOffsets(TestInfo testInfo) {
        Scenario scenario = scenario(testInfo, "buf05_plain", ALL_PARTITIONS_BUFFER_COUNT);
        List<SinkRecord> records = allPartitions(scenario.table(), null);
        Map<TopicPartition, OffsetAndMetadata> expected = endOffsets(scenario.table(), PARTITION_COUNT, END_OFFSET);

        runScenario(scenario, records, task -> {
            task.put(records);

            Map<TopicPartition, OffsetAndMetadata> committed = task.preCommit(new HashMap<>(expected));
            assertEquals(expected, committed, "preCommit of records that were not renamed must be unchanged");
        });
    }

    @Test
    @DisplayName("BUF-05: non-buffered sink under a topic-renaming SMT returns the current offsets unchanged and inserts every row")
    void nonBufferedRenamedSinkReturnsCurrentOffsets(TestInfo testInfo) {
        Scenario scenario = scenario(testInfo, "buf05_direct", null);
        String source = scenario.table() + DEV_SUFFIX;
        List<SinkRecord> records = allPartitions(source, scenario.table());
        Map<TopicPartition, OffsetAndMetadata> current = endOffsets(source, PARTITION_COUNT, END_OFFSET);

        runScenario(scenario, records, task -> {
            task.put(records);

            assertSame(current, task.preCommit(current), "the direct path must hand Connect its own offsets back");
        });
    }

    private Scenario scenario(TestInfo testInfo, String tablePrefix, String bufferCount) {
        Map<String, String> props = getBaseProps();
        props.put(CONNECTOR_NAME_PROPERTY, uniqueConnectorName(testInfo));
        if (bufferCount != null) {
            props.put(ClickHouseSinkConfig.BUFFER_COUNT, bufferCount);
        }
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String table = createTopicName(tablePrefix);
        ClickHouseTestHelpers.dropTable(chc, table);
        TABLE.get().tableName(table).execute(chc);
        return new Scenario(props, chc, table);
    }

    private static void runScenario(Scenario scenario, Collection<SinkRecord> expectedRows, TaskBody body) {
        try (ClickHouseHelperClient chc = scenario.chc()) {
            try {
                ClickHouseSinkTask task = new ClickHouseSinkTask();
                try {
                    task.start(scenario.props());
                    body.run(task);
                } finally {
                    task.stop();
                }
                assertRowsEqual(chc, scenario.table(), expectedRows);
            } finally {
                ClickHouseTestHelpers.dropTable(chc, scenario.table());
            }
        }
    }

    private static void assertRowsEqual(
            ClickHouseHelperClient chc, String table, Collection<SinkRecord> expectedRecords) {
        List<JSONObject> rows = ClickHouseTestHelpers.getAllRowsAsJson(chc, table);
        Set<String> actual = new HashSet<>();
        for (JSONObject row : rows) {
            actual.add(row.getString(STR_COLUMN));
        }
        assertEquals(expectedRecords.size(), rows.size(), "row count read back from ClickHouse");
        assertEquals(strValues(expectedRecords), actual, "rows read back from ClickHouse");
    }

    private static Set<String> strValues(Collection<SinkRecord> records) {
        Set<String> values = new HashSet<>();
        for (SinkRecord record : records) {
            values.add((String) ((Map<?, ?>) record.value()).get(STR_COLUMN));
        }
        return values;
    }

    private static List<SinkRecord> partitionRecords(
            String topic, String renamedTopic, int partition, long firstOffset, int count) {
        List<SinkRecord> records = RenamedTopicRecords.schemaless(topic, partition, firstOffset, count);
        return renamedTopic == null ? records : RenamedTopicRecords.renamed(records, renamedTopic);
    }

    private static List<SinkRecord> allPartitions(String topic, String renamedTopic) {
        List<SinkRecord> records = new ArrayList<>();
        for (int partition = 0; partition < PARTITION_COUNT; partition++) {
            records.addAll(partitionRecords(topic, renamedTopic, partition, FIRST_OFFSET, RECORDS_PER_PARTITION));
        }
        return records;
    }

    private static Map<TopicPartition, OffsetAndMetadata> endOffsets(String topic, int partitions, long endOffset) {
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        for (int partition = 0; partition < partitions; partition++) {
            offsets.put(new TopicPartition(topic, partition), new OffsetAndMetadata(endOffset));
        }
        return offsets;
    }
}
