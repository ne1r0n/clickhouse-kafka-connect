package com.clickhouse.kafka.connect.util.jmx;

import com.clickhouse.kafka.connect.sink.ProxySinkTask;
import com.clickhouse.kafka.connect.sink.Version;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.connect.sink.SinkRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SinkTaskStatistics implements SinkTaskStatisticsMBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxySinkTask.class);

    private final AtomicLong receivedRecords;
    private final AtomicLong recordProcessingTime;
    private final AtomicLong taskProcessingTime;
    private final AtomicLong insertedRecords;
    private final AtomicLong failedRecords;
    private final AtomicLong receivedBatches;
    private final AtomicLong insertedBytes;
    private final AtomicLong sentToDLQ;
    private final SimpleMovingAverage receiveLag;
    private final String connectorName;
    private final int taskId;
    private final Map<String, TopicStatistics> topicStatistics;
    private final Deque<String> topicMBeans;


    public SinkTaskStatistics(String connectorName, int taskId) {
        this.connectorName = connectorName;
        this.taskId = taskId;
        this.topicMBeans = new ConcurrentLinkedDeque<>();
        this.topicStatistics = new ConcurrentHashMap<>();

        this.receivedRecords = new AtomicLong(0);
        this.recordProcessingTime = new AtomicLong(0);
        this.taskProcessingTime = new AtomicLong(0);
        this.insertedRecords = new AtomicLong(0);
        this.receivedBatches = new AtomicLong(0);
        this.failedRecords = new AtomicLong(0);
        this.receiveLag = new SimpleMovingAverage(SimpleMovingAverage.DEFAULT_WINDOW_SIZE);
        this.insertedBytes = new AtomicLong(0);
        this.sentToDLQ = new AtomicLong(0);
    }

    public void registerMBean() {
        String name = getMBeanName();
        LOGGER.info("Register MBean [{}]", name);
        MBeanServerUtils.registerMBean(this, name);
    }

    public void unregisterMBean() {
        String name = getMBeanName();
        LOGGER.info("Unregister MBean [{}]", name);
        MBeanServerUtils.unregisterMBean(name);
        for (String topicMBean : topicMBeans) {
            LOGGER.info("Unregister topic MBean [{}]", topicMBean);
            MBeanServerUtils.unregisterMBean(topicMBean);
        }
    }

    @Override
    public long getReceivedRecords() {
        return receivedRecords.get();
    }

    @Override
    public long getRecordProcessingTime() {
        return recordProcessingTime.get();
    }

    @Override
    public long getTaskProcessingTime() {
        return taskProcessingTime.get();
    }

    @Override
    public long getInsertedRecords() {
        return insertedRecords.get();
    }

    @Override
    public long getFailedRecords() {
        return failedRecords.get();
    }

    @Override
    public long getReceivedBatches() {
        return receivedBatches.get();
    }


    @Override
    public long getMeanReceiveLag() {
        return Double.valueOf(receiveLag.get()).longValue();
    }

    @Override
    public long getInsertedBytes() {
        return insertedBytes.get();
    }

    @Override
    public long getMessagesSentToDLQ() {
        return sentToDLQ.get();
    }

    public void receivedRecords(final Collection<SinkRecord> records) {
        this.receivedRecords.addAndGet(records.size());
        this.receivedBatches.addAndGet(1);

        try {
            long receiveTime = System.currentTimeMillis();
            long eventTime = receiveTime;
            Optional<SinkRecord> first = records.stream().findFirst();
            if (first.isPresent()) {
                eventTime = first.get().timestamp();
            }
            receiveLag.add(receiveTime - eventTime);
        } catch (Exception e) {
            LOGGER.warn("Failed to calculate receive lag", e);
        }
    }

    public void recordProcessingTime(ExecutionTimer timer) {
        this.recordProcessingTime.addAndGet(timer.nanosElapsed());
    }

    public void taskProcessingTime(ExecutionTimer timer) {
        this.taskProcessingTime.addAndGet(timer.nanosElapsed());
    }


    public void recordTopicStats(int n, String table, long eventReceiveLag) {
        insertedRecords.addAndGet(n);
        topicStatistics.computeIfAbsent(table, this::createTopicStatistics).recordsInserted(n);
        topicStatistics.computeIfAbsent(table, this::createTopicStatistics).batchInserted(1);
    }

    public void recordTopicStatsOnFailure(int numOfRecords, String table, long eventReceiveLag) {
        failedRecords.addAndGet(numOfRecords);
        topicStatistics.computeIfAbsent(table, this::createTopicStatistics).recordsFailed(numOfRecords);
        topicStatistics.computeIfAbsent(table, this::createTopicStatistics).batchesFailed(1);
    }

    public void bytesInserted(long n) {
        insertedBytes.addAndGet(n);
    }

    private TopicStatistics createTopicStatistics(String topic) {
        TopicStatistics topicStatistics = new TopicStatistics();
        String topicMBeanName = getTopicMBeanName(topic);
        LOGGER.info("Register topic MBean [{}]", topicMBeanName);
        topicMBeans.add(topicMBeanName);
        return MBeanServerUtils.registerMBean(topicStatistics, topicMBeanName);
    }

    /**
     * Byte-for-byte restoration of the pre-merge {@code ProxySinkTask.buildMBeanName} carrier
     * relocated here now that {@code SinkTaskStatistics} owns MBean naming. Pure
     * function - reads no field - so it can be pinned by {@code MBeanNameUnitTest} without
     * constructing an instance. The null-version and colon-sanitization branches are unreachable
     * from production (Version.ARTIFACT_VERSION is a non-null compile-time literal with no colon)
     * but are retained on purpose: buildMBeanName is a general formatter and the sanitization
     * contract stays pinned regardless of what the build currently feeds it.
     */
    public static String buildMBeanName(String connectorName, int taskId, String version) {
        String safeVersion = version != null ? version.replace(':', '_') : "unknown";
        return String.format(
                "com.clickhouse:type=ClickHouseKafkaConnector,connector=%s,name=SinkTask%d,version=%s",
                connectorName, taskId, safeVersion);
    }

    public String getMBeanName() {
        return buildMBeanName(connectorName, taskId, Version.ARTIFACT_VERSION);
    }

    /**
     * Composed from {@link #getMBeanName()}, not a second format string - the two names
     * share a literal prefix and cannot drift in null handling, colon handling or key order.
     */
    public String getTopicMBeanName(String topic) {
        return getMBeanName() + ",topic=" + topic;
    }

    public void insertTime(long t, String topic) {
        topicStatistics.computeIfAbsent(topic, this::createTopicStatistics).insertTime(t);
    }

    public void sentToDLQ(long n) {
        sentToDLQ.addAndGet(n);
    }
}
