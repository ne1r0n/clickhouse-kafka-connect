package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clickhouse.kafka.connect.sink.db.InMemoryDBWriter;
import com.clickhouse.kafka.connect.sink.dlq.ErrorReporter;
import com.clickhouse.kafka.connect.sink.helper.SchemalessTestData;
import com.clickhouse.kafka.connect.sink.state.provider.InMemoryState;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Pins what {@link ClickHouseSinkTask#stop()} does around the delivery strategy: the strategy stops first,
 * and the proxy task (the writer, the task MBean and the state provider) is stopped afterwards even when the
 * strategy's {@code stop()} throws. The task's private {@code proxySinkTask} and {@code deliveryStrategy}
 * fields are set by reflection, and the package-private {@link DeliveryStrategy} is implemented by a stub,
 * so the class needs no mocking library, no new production constructor and no Docker.
 */
class ClickHouseSinkTaskStopUnitTest {

    private static final String PROXY_SINK_TASK_FIELD = "proxySinkTask";
    private static final String DELIVERY_STRATEGY_FIELD = "deliveryStrategy";
    private static final String STATE_PROVIDER_FIELD = "stateProvider";
    private static final String AT_LEAST_ONCE_BUFFER_FIELD = "buffer";
    private static final String EXACTLY_ONCE_BUFFER_FIELD = "perPartitionBuffer";

    private static final String STRATEGY_STOP_FAILURE = "scripted strategy stop failure";
    private static final String WRITER_STOP_FAILURE = "scripted writer stop failure";
    private static final String STRATEGY_FAILS_CONNECTOR = "task-stop-strategy-fails";
    private static final String BOTH_FAIL_CONNECTOR = "task-stop-both-fail";
    private static final String NO_STRATEGY_CONNECTOR = "task-stop-no-strategy";
    private static final String SHIPPED_CONNECTOR = "task-stop-shipped";
    private static final String TOPIC = "task-stop-topic";
    private static final String SHIPPED_STRATEGY_NAME = "{displayName}[{index}] strategy={0}";

    private static final int PARTITION = 0;
    private static final int BUFFER_COUNT = 10;
    private static final long NO_FLUSH_TIME = 0L;
    private static final int BUFFERED_RECORDS = 3;
    private static final int ONE_CALL = 1;
    private static final ErrorReporter NO_ERROR_REPORTER = null;

    private static final String WRITER_STOPPED_ONCE = "the writer must have been stopped once";
    private static final String CLOSE_COUNT = "close count of the state provider";
    private static final String MBEAN_GONE = "the task MBean must be unregistered after stop";
    private static final String STRATEGY_STOPPED_ONCE = "the delivery strategy must have been stopped once";
    private static final String STRATEGY_STOPS_FIRST = "the strategy must stop before the writer";
    private static final String STRATEGY_FAILURE_REACHES_CALLER = "the strategy's own failure must reach the caller";
    private static final String BUFFERED_BEFORE_STOP = "the records must be buffered before stop";
    private static final String NOTHING_INSERTED_BEFORE_STOP = "buffering must not insert before stop";
    private static final String BUFFER_DISCARDED = "stop must discard the buffered records";
    private static final String STOP_DOES_NOT_FLUSH = "stop must discard the buffered records, not insert them";

    private enum Shipped {
        DIRECT,
        AT_LEAST_ONCE_BUFFERED,
        EXACTLY_ONCE_BUFFERED
    }

    /** A strategy whose {@code stop()} fails, and which records how many writer stops had happened by then. */
    private static final class ThrowingStrategy implements DeliveryStrategy {

        private final IllegalStateException failure = new IllegalStateException(STRATEGY_STOP_FAILURE);
        private final IntSupplier writerStops;
        private int stopCalls = 0;
        private int writerStopsSeen = -1;

        ThrowingStrategy(IntSupplier writerStops) {
            this.writerStops = writerStops;
        }

        @Override
        public void put(Collection<SinkRecord> records) {}

        @Override
        public Map<TopicPartition, OffsetAndMetadata> preCommit(Map<TopicPartition, OffsetAndMetadata> currentOffsets) {
            return currentOffsets;
        }

        @Override
        public void close(Collection<TopicPartition> partitions) {}

        @Override
        public void stop() {
            stopCalls++;
            writerStopsSeen = writerStops.getAsInt();
            throw failure;
        }
    }

    private static final class CountingDBWriter extends InMemoryDBWriter {

        private int stopCalls = 0;
        private boolean failOnStop = false;

        @Override
        public void stop() {
            stopCalls++;
            if (failOnStop) {
                throw new IllegalArgumentException(WRITER_STOP_FAILURE);
            }
        }
    }

    private static final class CountingStateProvider extends InMemoryState {

        private int closeCalls = 0;

        @Override
        public void close() {
            closeCalls++;
        }
    }

    /** A started proxy task wrapped by a task whose two private fields are set the way {@code start()} sets them. */
    private static final class Harness implements AutoCloseable {

        private final CountingDBWriter writer = new CountingDBWriter();
        private final CountingStateProvider provider = new CountingStateProvider();
        private final ClickHouseSinkConfig config = new ClickHouseSinkConfig(new HashMap<>());
        private final ClickHouseSinkTask task = new ClickHouseSinkTask();
        private final ProxySinkTask proxy;
        private final SinkTaskStatistics statistics;

        Harness(String connectorName) {
            proxy = new ProxySinkTask(config, NO_ERROR_REPORTER, connectorName, writer);
            statistics = new SinkTaskStatistics(connectorName, proxy.getId());
            setField(proxy, STATE_PROVIDER_FIELD, provider);
            setField(task, PROXY_SINK_TASK_FIELD, proxy);
            proxy.start();
        }

        void useStrategy(DeliveryStrategy strategy) {
            setField(task, DELIVERY_STRATEGY_FIELD, strategy);
        }

        boolean mBeanRegistered() {
            try {
                return ManagementFactory.getPlatformMBeanServer().isRegistered(new ObjectName(statistics.getMBeanName()));
            } catch (MalformedObjectNameException e) {
                throw new IllegalStateException(e);
            }
        }

        void assertProxyStoppedOnce() {
            assertEquals(ONE_CALL, writer.stopCalls, WRITER_STOPPED_ONCE);
            assertEquals(ONE_CALL, provider.closeCalls, CLOSE_COUNT);
            assertFalse(mBeanRegistered(), MBEAN_GONE);
        }

        @Override
        public void close() {
            statistics.unregisterMBean();
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object getField(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static DeliveryStrategy shippedStrategy(Shipped shipped, Harness harness) {
        ChunkFlusher flusher = new ChunkFlusher(harness.proxy, harness.config, NO_ERROR_REPORTER);
        return switch (shipped) {
            case DIRECT -> new DirectDeliveryStrategy(harness.proxy, harness.config, flusher);
            case AT_LEAST_ONCE_BUFFERED -> new AtLeastOnceBufferStrategy(flusher, BUFFER_COUNT, NO_FLUSH_TIME);
            case EXACTLY_ONCE_BUFFERED -> new ExactlyOnceBufferStrategy(flusher, BUFFER_COUNT);
        };
    }

    private static int bufferedRecords(Shipped shipped, DeliveryStrategy strategy) {
        return switch (shipped) {
            case DIRECT -> 0;
            case AT_LEAST_ONCE_BUFFERED -> ((List<?>) getField(strategy, AT_LEAST_ONCE_BUFFER_FIELD)).size();
            case EXACTLY_ONCE_BUFFERED -> ((Map<?, ?>) getField(strategy, EXACTLY_ONCE_BUFFER_FIELD))
                    .values().stream().mapToInt(bucket -> ((Collection<?>) bucket).size()).sum();
        };
    }

    @Test
    void strategyStopFailureStillStopsTheProxyTask() {
        try (Harness harness = new Harness(STRATEGY_FAILS_CONNECTOR)) {
            ThrowingStrategy strategy = new ThrowingStrategy(() -> harness.writer.stopCalls);
            harness.useStrategy(strategy);
            RuntimeException thrown = assertThrows(RuntimeException.class, harness.task::stop);
            harness.assertProxyStoppedOnce();
            assertSame(strategy.failure, thrown, STRATEGY_FAILURE_REACHES_CALLER);
            assertEquals(ONE_CALL, strategy.stopCalls, STRATEGY_STOPPED_ONCE);
            assertEquals(0, strategy.writerStopsSeen, STRATEGY_STOPS_FIRST);
        }
    }

    @Test
    void bothStopsRunWhenTheStrategyAndTheProxyTaskThrow() {
        try (Harness harness = new Harness(BOTH_FAIL_CONNECTOR)) {
            harness.writer.failOnStop = true;
            ThrowingStrategy strategy = new ThrowingStrategy(() -> harness.writer.stopCalls);
            harness.useStrategy(strategy);
            assertThrows(RuntimeException.class, harness.task::stop);
            harness.assertProxyStoppedOnce();
            assertEquals(ONE_CALL, strategy.stopCalls, STRATEGY_STOPPED_ONCE);
        }
    }

    @Test
    void stopWithoutADeliveryStrategyStillStopsTheProxyTask() {
        try (Harness harness = new Harness(NO_STRATEGY_CONNECTOR)) {
            assertDoesNotThrow(harness.task::stop);
            harness.assertProxyStoppedOnce();
        }
    }

    @ParameterizedTest(name = SHIPPED_STRATEGY_NAME)
    @EnumSource(Shipped.class)
    void stopReachesTheProxyTaskAfterEachShippedStrategy(Shipped shipped) {
        try (Harness harness = new Harness(SHIPPED_CONNECTOR + "-" + shipped)) {
            DeliveryStrategy strategy = shippedStrategy(shipped, harness);
            harness.useStrategy(strategy);
            if (shipped != Shipped.DIRECT) {
                strategy.put(SchemalessTestData.createPrimitiveTypes(TOPIC, PARTITION, BUFFERED_RECORDS));
                assertEquals(BUFFERED_RECORDS, bufferedRecords(shipped, strategy), BUFFERED_BEFORE_STOP);
                assertEquals(0L, harness.writer.recordsInserted(), NOTHING_INSERTED_BEFORE_STOP);
            }
            assertDoesNotThrow(harness.task::stop);
            harness.assertProxyStoppedOnce();
            assertEquals(0, bufferedRecords(shipped, strategy), BUFFER_DISCARDED);
            assertEquals(0L, harness.writer.recordsInserted(), STOP_DOES_NOT_FLUSH);
        }
    }
}
