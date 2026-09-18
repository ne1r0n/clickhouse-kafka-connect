package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
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
 * {@code SinkTask<N>} (D-04) - only relative claims (distinctness, "this one disappeared after
 * stop()") are valid.
 */
public class MBeanRegistrationLiveTest extends ClickHouseBase {

    private static void createTable(ClickHouseHelperClient chc, String tableName, String createTableQuery) {
        try (Records ignored = chc.queryV2(String.format(createTableQuery, tableName))) {
            // table created
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    /**
     * Returns the single element that is present in {@code after} but not in {@code before}.
     * Used to identify the exact ObjectName a task just registered, without ever reading the
     * task's private {@code id} or asserting an absolute name (D-04).
     */
    private static ObjectName newlyAdded(Set<ObjectName> before, Set<ObjectName> after) {
        Set<ObjectName> diff = new HashSet<>(after);
        diff.removeAll(before);
        assertEquals(1, diff.size(), "expected exactly one newly registered MBean, saw: " + diff);
        return diff.iterator().next();
    }

    @Test
    @DisplayName("JMX-04: a started task registers its MBean on the platform server and stop() unregisters exactly that one")
    void startedTaskRegistersAndStopUnregistersExactlyOne(TestInfo testInfo) throws Exception {
        String connectorName = uniqueConnectorName(testInfo);
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);

        String tableName = createTopicName("jmx04_fixture");
        createTable(chc, tableName, "CREATE TABLE %s (id UInt32) ENGINE = MergeTree ORDER BY id");
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

    @Test
    @DisplayName("JMX-04: four tasks across two connector names register four distinct MBeans and stopping one unregisters exactly that one")
    void fourTasksAcrossTwoConnectorsRegisterDistinctMBeansAndSelectiveStopUnregistersOne(TestInfo testInfo)
            throws Exception {
        String base = uniqueConnectorName(testInfo);
        String nameA = base + "-A";
        String nameB = base + "-B";
        Set<String> both = Set.of(nameA, nameB);

        // getBaseProps() already points at this class's per-class test database (the instance
        // `database` field, set in @BeforeAll setup()), so every per-task props map below is a
        // copy of this one already-correct base map, ensuring ClickHouseWriter.start() finds the
        // fixture table created below rather than an empty "default" database.
        Map<String, String> baseProps = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(baseProps);
        String tableName = createTopicName("jmx04_multi");
        createTable(chc, tableName, "CREATE TABLE %s (id UInt32) ENGINE = MergeTree ORDER BY id");

        ClickHouseSinkTask taskA1 = new ClickHouseSinkTask();
        ClickHouseSinkTask taskA2 = new ClickHouseSinkTask();
        ClickHouseSinkTask taskB1 = new ClickHouseSinkTask();
        ClickHouseSinkTask taskB2 = new ClickHouseSinkTask();
        try {
            Set<ObjectName> beforeAny = registeredFor(both);

            Map<String, String> propsA1 = new HashMap<>(baseProps);
            propsA1.put("name", nameA);
            taskA1.start(propsA1);
            Set<ObjectName> afterA1Set = registeredFor(both);
            ObjectName a1Name = newlyAdded(beforeAny, afterA1Set);

            Map<String, String> propsA2 = new HashMap<>(baseProps);
            propsA2.put("name", nameA);
            taskA2.start(propsA2);
            Set<ObjectName> afterA2Set = registeredFor(both);
            ObjectName a2Name = newlyAdded(afterA1Set, afterA2Set);

            Map<String, String> propsB1 = new HashMap<>(baseProps);
            propsB1.put("name", nameB);
            taskB1.start(propsB1);
            Set<ObjectName> afterB1Set = registeredFor(both);
            ObjectName b1Name = newlyAdded(afterA2Set, afterB1Set);

            Map<String, String> propsB2 = new HashMap<>(baseProps);
            propsB2.put("name", nameB);
            taskB2.start(propsB2);
            Set<ObjectName> afterB2Set = registeredFor(both);
            ObjectName b2Name = newlyAdded(afterB1Set, afterB2Set);

            // Distinctness across all four, and the shared-connector / distinct-name relationship
            // (the "just touching versus equal" case for MBean identity, E-28/E-34).
            assertEquals(4, afterB2Set.size(), "exactly four MBeans should be registered after all four starts");
            assertNotEquals(a1Name, a2Name);
            assertNotEquals(b1Name, b2Name);
            assertEquals(nameA, a1Name.getKeyProperty("connector"));
            assertEquals(nameA, a2Name.getKeyProperty("connector"));
            assertNotEquals(a1Name.getKeyProperty("name"), a2Name.getKeyProperty("name"));
            assertEquals(nameB, b1Name.getKeyProperty("connector"));
            assertEquals(nameB, b2Name.getKeyProperty("connector"));
            assertNotEquals(b1Name.getKeyProperty("name"), b2Name.getKeyProperty("name"));

            taskA2.stop();
            // queryNames returns an unordered Set - assert set membership/difference only, never
            // iteration order (E-35).
            Set<ObjectName> afterStopA2 = registeredFor(both);
            assertEquals(3, afterStopA2.size());
            assertTrue(!afterStopA2.contains(a2Name), "A2's MBean should be gone after its stop()");
            assertTrue(afterStopA2.contains(a1Name), "A1's MBean should still be registered");
            assertTrue(afterStopA2.contains(b1Name), "B1's MBean should still be registered");
            assertTrue(afterStopA2.contains(b2Name), "B2's MBean should still be registered");
        } finally {
            taskA1.stop();
            // A2 was already stopped mid-test; a second stop() must not throw (idempotency guard).
            assertDoesNotThrow(taskA2::stop);
            taskB1.stop();
            taskB2.stop();
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }
}
