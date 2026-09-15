package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.lang.management.ManagementFactory;
import java.util.Map;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

/**
 * Proves the {@link ClickHouseBase} test harness contract (INFRA-03, INFRA-04, INFRA-05, D-05,
 * D-08, D-09, D-10): a single shared {@code latest} container per test JVM fork, a fresh,
 * isolated database per test class, MBean leak cleanup after a deliberate mid-test failure, and
 * a safe unique connector-name helper.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClickHouseTestHarnessTest extends ClickHouseBase {

    private static final String LEAK_PROBE_CONNECTOR = "harness-mbean-leak-probe";

    /** Thrown deliberately between {@code start()} and {@code stop()} to prove D-09's sweep. */
    static final class DeliberateMidTestFailure extends RuntimeException {}

    /**
     * Swallows only {@link DeliberateMidTestFailure} so JUnit records the test as passed while
     * {@code @AfterEach} still runs the MBean sweep; any other throwable is rethrown.
     */
    public static class SwallowDeliberateMidTestFailure implements TestExecutionExceptionHandler {
        @Override
        public void handleTestExecutionException(
                org.junit.jupiter.api.extension.ExtensionContext context, Throwable throwable) throws Throwable {
            if (!(throwable instanceof DeliberateMidTestFailure)) {
                throw throwable;
            }
        }
    }

    private static int mbeanCountForConnector(String connectorName) {
        try {
            return ManagementFactory.getPlatformMBeanServer()
                    .queryNames(
                            new ObjectName(
                                    "com.clickhouse:type=ClickHouseKafkaConnector,connector=" + connectorName + ",*"),
                            null)
                    .size();
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(1)
    public void sharedContainerIsStartedOncePerJvmAndIsTheRequestedImage() {
        Assumptions.assumeFalse(isCloud, "No shared container to inspect against ClickHouse Cloud");

        assertEquals(1, sharedContainerStarts());
        assertEquals(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE, db.getDockerImageName());
    }

    @Test
    @Order(2)
    public void classGetsFreshIsolatedDatabase() {
        Assumptions.assumeFalse(isCloud, "No shared container to inspect against ClickHouse Cloud");

        Map<String, String> props = createProps();
        ClickHouseHelperClient chc = createClient(props);

        assertTrue(database.startsWith("kafka_connect_test_"));

        assertEquals(
                1L,
                chc.getClient()
                        .queryAll(String.format(
                                "SELECT toInt64(count()) AS c FROM system.databases WHERE name = '%s'", database))
                        .get(0)
                        .getLong("c"));
        assertEquals(
                0L,
                chc.getClient()
                        .queryAll(String.format(
                                "SELECT toInt64(count()) AS c FROM system.tables WHERE database = '%s'", database))
                        .get(0)
                        .getLong("c"));
        assertEquals(
                0L,
                chc.getClient()
                        .queryAll(String.format(
                                "SELECT toInt64(count()) AS c FROM system.databases WHERE name LIKE 'kafka_connect_test_%%' AND name != '%s'",
                                database))
                        .get(0)
                        .getLong("c"));
    }

    /**
     * D-09 proof: a test that fails between {@code start()} and {@code stop()} leaks an MBean.
     * No {@code finally} here on purpose — this simulates the failure the @AfterEach sweep must
     * clean up. {@link SwallowDeliberateMidTestFailure} lets JUnit record this test as passed
     * while still running the sweep afterwards.
     */
    @Test
    @Order(3)
    @ExtendWith(SwallowDeliberateMidTestFailure.class)
    public void testFailingBetweenStartAndStopLeaksAnMBean() {
        Map<String, String> props = createProps();
        ClickHouseHelperClient chc = createClient(props);
        // ClickHouseWriter.start() requires at least one table in the class database to build
        // its table mapping; the class database starts empty (proven by Order(2)).
        ClickHouseTestHelpers.createTable(
                chc, "harness_mbean_probe_table", "CREATE TABLE IF NOT EXISTS %s (id UInt8) ENGINE = MergeTree ORDER BY id");
        props.put("name", LEAK_PROBE_CONNECTOR);

        ClickHouseSinkTask chst = new ClickHouseSinkTask();
        chst.start(props);

        assertEquals(1, mbeanCountForConnector(LEAK_PROBE_CONNECTOR));

        throw new DeliberateMidTestFailure();
    }

    /**
     * D-09/D-10 proof: the previous test's leaked MBean was swept by {@code @AfterEach}, and
     * {@link #uniqueConnectorName(TestInfo)} produces safe, distinct names that a properly
     * try/finally-guarded start/stop never leaks.
     */
    @Test
    @Order(4)
    public void leakedMBeanWasSweptAndUniqueNamesAreSafe(TestInfo testInfo) {
        assertEquals(0, mbeanCountForConnector(LEAK_PROBE_CONNECTOR));

        String name1 = uniqueConnectorName(testInfo);
        String name2 = uniqueConnectorName(testInfo);
        assertNotEquals(name1, name2);
        assertTrue(name1.matches("^[A-Za-z0-9_$-]+$"), () -> name1 + " must be a safe ObjectName token");
        assertTrue(name2.matches("^[A-Za-z0-9_$-]+$"), () -> name2 + " must be a safe ObjectName token");

        Map<String, String> props = createProps();
        createClient(props);
        props.put("name", name1);

        ClickHouseSinkTask chst = new ClickHouseSinkTask();
        try {
            chst.start(props);
            assertEquals(1, mbeanCountForConnector(name1));
        } finally {
            chst.stop();
        }
        assertEquals(0, mbeanCountForConnector(name1));
    }
}
