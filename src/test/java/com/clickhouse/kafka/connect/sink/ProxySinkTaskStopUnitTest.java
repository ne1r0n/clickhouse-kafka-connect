package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.db.InMemoryDBWriter;
import com.clickhouse.kafka.connect.sink.dlq.ErrorReporter;
import com.clickhouse.kafka.connect.sink.state.provider.InMemoryState;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;

/**
 * Pins what {@link ProxySinkTask#stop()} does when a step fails and in which order its steps run: the
 * writer stops first, then the task MBean goes away, then the state provider is closed. The provider
 * is swapped in by reflection, so no constructor is added and the class stays in the Docker-free
 * {@code testUnit} loop.
 */
class ProxySinkTaskStopUnitTest {

    private static final String WRITER_STOP_FAILURE = "scripted writer stop failure";
    private static final String STOP_FAILURE_CONNECTOR = "proxy-stop-writer-failure";
    private static final String STOP_ORDER_CONNECTOR = "proxy-stop-order";
    private static final String PROVIDER_FAILURE_CONNECTOR = "proxy-stop-provider-failure";
    private static final String BOTH_FAIL_CONNECTOR = "proxy-stop-both-fail";
    private static final String PROVIDER_CLOSE_FAILURE = "scripted state provider close failure";
    private static final String WRITER_STOPPED_ONCE = "the writer must have been stopped once";
    private static final String FAILURE_SUPPRESSED = "the later failure must ride along as suppressed";
    private static final String STATE_PROVIDER_FIELD = "stateProvider";
    private static final String CLOSE_COUNT = "close count of the state provider";
    private static final String MBEAN_GONE = "the task MBean must be unregistered after stop";
    private static final String WRITER_STOPPED_FIRST = "the writer must have stopped before the state provider closed";
    private static final ErrorReporter NO_ERROR_REPORTER = null;

    private static final class StopFailingDBWriter extends InMemoryDBWriter {

        private IllegalStateException lastThrown;

        @Override
        public void stop() {
            lastThrown = new IllegalStateException(WRITER_STOP_FAILURE);
            throw lastThrown;
        }
    }

    private static final class RecordingDBWriter extends InMemoryDBWriter {

        private int stopCalls = 0;

        @Override
        public void stop() {
            stopCalls++;
        }
    }

    private static final class CountingStateProvider extends InMemoryState {

        private int closeCalls = 0;
        private int writerStopsAtClose = -1;
        private boolean mBeanRegisteredAtClose = true;
        private IntSupplier writerStops = () -> -1;
        private BooleanSupplier mBeanRegistered = () -> true;
        private IllegalStateException failure = null;

        void failOnClose(IllegalStateException failure) {
            this.failure = failure;
        }

        void observe(IntSupplier writerStops, BooleanSupplier mBeanRegistered) {
            this.writerStops = writerStops;
            this.mBeanRegistered = mBeanRegistered;
        }

        @Override
        public void close() {
            closeCalls++;
            writerStopsAtClose = writerStops.getAsInt();
            mBeanRegisteredAtClose = mBeanRegistered.getAsBoolean();
            if (failure != null) {
                throw failure;
            }
        }
    }

    private static ProxySinkTask taskWith(String connectorName, InMemoryDBWriter writer, CountingStateProvider provider)
            throws ReflectiveOperationException {
        ProxySinkTask task =
                new ProxySinkTask(new ClickHouseSinkConfig(new HashMap<>()), NO_ERROR_REPORTER, connectorName, writer);
        Field field = ProxySinkTask.class.getDeclaredField(STATE_PROVIDER_FIELD);
        field.setAccessible(true);
        field.set(task, provider);
        return task;
    }

    private static SinkTaskStatistics statisticsOf(String connectorName, ProxySinkTask task) {
        return new SinkTaskStatistics(connectorName, task.getId());
    }

    private static boolean isRegistered(SinkTaskStatistics statistics) {
        try {
            return ManagementFactory.getPlatformMBeanServer().isRegistered(new ObjectName(statistics.getMBeanName()));
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void writerStopFailureStillUnregistersTheMBeanAndClosesTheStateProvider() throws Exception {
        StopFailingDBWriter writer = new StopFailingDBWriter();
        CountingStateProvider provider = new CountingStateProvider();
        ProxySinkTask task = taskWith(STOP_FAILURE_CONNECTOR, writer, provider);
        SinkTaskStatistics statistics = statisticsOf(STOP_FAILURE_CONNECTOR, task);
        try {
            task.start();
            IllegalStateException thrown = assertThrows(IllegalStateException.class, task::stop);
            assertEquals(1, provider.closeCalls, CLOSE_COUNT);
            assertSame(writer.lastThrown, thrown);
            assertFalse(isRegistered(statistics), MBEAN_GONE);
        } finally {
            statistics.unregisterMBean();
        }
    }

    @Test
    void stopClosesTheStateProviderOnceAfterTheWriterAndTheMBean() throws Exception {
        RecordingDBWriter writer = new RecordingDBWriter();
        CountingStateProvider provider = new CountingStateProvider();
        ProxySinkTask task = taskWith(STOP_ORDER_CONNECTOR, writer, provider);
        SinkTaskStatistics statistics = statisticsOf(STOP_ORDER_CONNECTOR, task);
        provider.observe(() -> writer.stopCalls, () -> isRegistered(statistics));
        try {
            task.start();
            task.stop();
            assertEquals(1, provider.closeCalls, CLOSE_COUNT);
            assertEquals(1, provider.writerStopsAtClose, WRITER_STOPPED_FIRST);
            assertFalse(provider.mBeanRegisteredAtClose, MBEAN_GONE);
            assertFalse(isRegistered(statistics), MBEAN_GONE);
        } finally {
            statistics.unregisterMBean();
        }
    }

    @Test
    void stateProviderCloseFailureReachesTheCallerAfterTheWriterStoppedAndTheMBeanWentAway() throws Exception {
        RecordingDBWriter writer = new RecordingDBWriter();
        CountingStateProvider provider = new CountingStateProvider();
        IllegalStateException closeFailure = new IllegalStateException(PROVIDER_CLOSE_FAILURE);
        provider.failOnClose(closeFailure);
        ProxySinkTask task = taskWith(PROVIDER_FAILURE_CONNECTOR, writer, provider);
        SinkTaskStatistics statistics = statisticsOf(PROVIDER_FAILURE_CONNECTOR, task);
        try {
            task.start();
            IllegalStateException thrown = assertThrows(IllegalStateException.class, task::stop);
            assertSame(closeFailure, thrown);
            assertEquals(1, writer.stopCalls, WRITER_STOPPED_ONCE);
            assertEquals(1, provider.closeCalls, CLOSE_COUNT);
            assertFalse(isRegistered(statistics), MBEAN_GONE);
        } finally {
            statistics.unregisterMBean();
        }
    }

    @Test
    void writerStopFailureStaysTheThrownExceptionWhenTheStateProviderCloseAlsoFails() throws Exception {
        StopFailingDBWriter writer = new StopFailingDBWriter();
        CountingStateProvider provider = new CountingStateProvider();
        IllegalStateException closeFailure = new IllegalStateException(PROVIDER_CLOSE_FAILURE);
        provider.failOnClose(closeFailure);
        ProxySinkTask task = taskWith(BOTH_FAIL_CONNECTOR, writer, provider);
        SinkTaskStatistics statistics = statisticsOf(BOTH_FAIL_CONNECTOR, task);
        try {
            task.start();
            IllegalStateException thrown = assertThrows(IllegalStateException.class, task::stop);
            assertSame(writer.lastThrown, thrown);
            assertTrue(Arrays.asList(thrown.getSuppressed()).contains(closeFailure), FAILURE_SUPPRESSED);
            assertEquals(1, provider.closeCalls, CLOSE_COUNT);
            assertFalse(isRegistered(statistics), MBEAN_GONE);
        } finally {
            statistics.unregisterMBean();
        }
    }
}
