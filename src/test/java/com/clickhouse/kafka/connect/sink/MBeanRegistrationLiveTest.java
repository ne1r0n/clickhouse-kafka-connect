package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * ClickHouse-backed pin for JMX-04: multi-task, multi-connector MBean registration and selective
 * unregistration on the real platform MBean server. New fork-only class (D-16), extending
 * {@code ClickHouseBase} because registration only happens through a live {@code ProxySinkTask}
 * constructor. {@code ProxySinkTask.id} comes from a static {@code NEXT_ID} counter that never
 * resets inside a test JVM fork, so this class never asserts an absolute name such as
 * {@code SinkTask0} (D-04) - only relative claims (distinctness, "this one disappeared after
 * stop()") are valid.
 */
public class MBeanRegistrationLiveTest extends ClickHouseBase {

    /**
     * Queries the platform MBean server for every registered
     * {@code com.clickhouse:type=ClickHouseKafkaConnector,*} MBean and returns only those whose
     * {@code connector} key is in the supplied set. Filtering to this test's own connector names
     * is mandatory (D-04): {@code ClickHouseBase}'s {@code @AfterEach} sweep queries the same
     * server, and other tests' MBeans can transiently coexist, so a global count would be flaky.
     */
    private static Set<ObjectName> registeredFor(Set<String> connectorNames) throws MalformedObjectNameException {
        Set<ObjectName> all = ManagementFactory.getPlatformMBeanServer()
                .queryNames(new ObjectName("com.clickhouse:type=ClickHouseKafkaConnector,*"), null);
        Set<ObjectName> filtered = new HashSet<>();
        for (ObjectName name : all) {
            if (connectorNames.contains(name.getKeyProperty("connector"))) {
                filtered.add(name);
            }
        }
        return filtered;
    }

    @Test
    @DisplayName("JMX-04: a started task registers its MBean on the platform server and stop() unregisters exactly that one")
    void startedTaskRegistersAndStopUnregistersExactlyOne(TestInfo testInfo) throws Exception {
        String connectorName = uniqueConnectorName(testInfo);
        Map<String, String> props = createProps();
        ClickHouseHelperClient chc = createClient(props);

        String tableName = createTopicName("jmx04_fixture");
        ClickHouseTestHelpers.createTable(chc, tableName, "CREATE TABLE %s (id UInt32) ENGINE = MergeTree ORDER BY id");
        props.put("name", connectorName);

        Set<String> mine = Set.of(connectorName);
        try {
            assertTrue(registeredFor(mine).isEmpty(), "no MBean should be registered before start()");

            ClickHouseSinkTask task = new ClickHouseSinkTask();
            task.start(props);
            try {
                Set<ObjectName> afterStart = registeredFor(mine);
                assertEquals(1, afterStart.size(), "exactly one MBean should be registered after start()");
                assertEquals(connectorName, afterStart.iterator().next().getKeyProperty("connector"));

                task.stop();
                assertTrue(registeredFor(mine).isEmpty(), "the MBean should be unregistered after stop()");
            } finally {
                // MBeanServerUtils.unregisterMBean guards on isRegistered, so a second stop() on an
                // already-stopped task must not throw (JMX-04 idempotency).
                assertDoesNotThrow(task::stop);
            }
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }
}
