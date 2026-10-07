package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.sink.SinkRecord;
import org.json.JSONObject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

/**
 * Proves the {@link ClickHouseBase} test harness contract (HARN-01..03, D-01, D-02, D-05, D-07,
 * D-08, D-09a, D-10): a single shared {@code latest} container per test JVM fork with an opt-out
 * seam that leaves the shared-start counter untouched, a fresh, isolated database per test class
 * dropped at class teardown, the {@code @AfterEach} MBean leak sweep covering both task-level and
 * per-topic MBeans, a safe unique connector-name helper, and UTF-8 helper decoding. This class is
 * the proof of the harness contract: D-07 knowingly moves both the contract and this canary in
 * the same phase, so the proof now lives in the assertions below rather than in a zero diff
 * against the pre-merge shape.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClickHouseTestHarnessTest extends ClickHouseBase {

    private static final String LEAK_PROBE_CONNECTOR = "harness-mbean-leak-probe";
    private static final String PER_TOPIC_LEAK_PROBE_CONNECTOR = "harness-per-topic-mbean-leak-probe";

    /** Matches the established per-file convention (e.g. {@code SmtSinkLandingTest}). */
    private static void createTable(ClickHouseHelperClient chc, String tableName, String createTableQuery) {
        try (Records ignored = chc.queryV2(String.format(createTableQuery, tableName))) {
            // table created
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    /**
     * D-08: single query site for every MBean registered under a given connector name - both the
     * task-level MBean and any per-topic MBeans it owns, since the trailing {@code ,*} wildcard
     * matches both. {@link #mbeanCountForConnector(String)} is expressed in terms of this method
     * so there is one query site rather than two.
     */
    private static Set<ObjectName> mbeanNamesForConnector(String connectorName) {
        try {
            return ManagementFactory.getPlatformMBeanServer()
                    .queryNames(
                            new ObjectName(
                                    "com.clickhouse:type=ClickHouseKafkaConnector,connector=" + connectorName + ",*"),
                            null);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    private static int mbeanCountForConnector(String connectorName) {
        return mbeanNamesForConnector(connectorName).size();
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

        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);

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
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        // ClickHouseWriter.start() requires at least one table in the class database to build
        // its table mapping; the class database starts empty (proven by Order(2)).
        createTable(
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

        Map<String, String> props = getBaseProps();
        ClickHouseTestHelpers.createClient(props);
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

    /**
     * D-11 proof: {@code ClickHouseTestHelpers} decodes ClickHouse HTTP responses as UTF-8, so
     * non-ASCII strings survive the round trip even when the test JVM runs under
     * {@code file.encoding=windows-1252}. The insert itself uses ASCII-only SQL ({@code unhex})
     * so the insert path's own encoding never taints this result — only the read path (the
     * subject of D-11) is exercised.
     */
    @Test
    @Order(5)
    public void helpersDecodeResponsesAsUtf8() {
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(getBaseProps());
        String table = createTopicName("utf8_helper_test");

        ClickHouseTestHelpers.dropTable(chc, table);
        createTable(chc, table, "CREATE TABLE %s (id UInt8, s String) ENGINE = MergeTree ORDER BY id");
        ClickHouseTestHelpers.executeQueryIgnoreResult(
                chc,
                String.format(
                        "INSERT INTO `%s` VALUES (1, unhex('D09FD180D0B8D0B2D0B5D18220E4B8ADE69687'))", table));

        List<JSONObject> rows = ClickHouseTestHelpers.getAllRowsAsJson(chc, table);
        if (rows.isEmpty()) {
            fail("Expected exactly one row from " + table);
        }
        // Expected value: Привет 中文 -- the Java string literal below spells this out in escaped
        // unicode code points only, so this source file's own encoding is never the thing under test.
        assertEquals("\u041f\u0440\u0438\u0432\u0435\u0442 \u4e2d\u6587", rows.get(0).getString("s"));
    }

    /**
     * D-08 proof: driving a real record through a started {@link ClickHouseSinkTask} registers a
     * per-topic MBean via {@code SinkTaskStatistics.createTopicStatistics}, composed from the
     * task-level name with a trailing {@code ",topic="} key rather than a second independent
     * format string. No {@code finally} here on purpose, exactly like {@link
     * #testFailingBetweenStartAndStopLeaksAnMBean()} does at the task level - this simulates the
     * failure the {@code @AfterEach} sweep must clean up.
     */
    @Test
    @Order(6)
    @ExtendWith(SwallowDeliberateMidTestFailure.class)
    public void perTopicMBeanLeakedByAFailingTest() {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String topic = createTopicName("harness_per_topic_mbean_probe");
        createTable(chc, topic, "CREATE TABLE IF NOT EXISTS %s (off16 Int16) ENGINE = MergeTree ORDER BY off16");
        props.put("name", PER_TOPIC_LEAK_PROBE_CONNECTOR);

        ClickHouseSinkTask chst = new ClickHouseSinkTask();
        chst.start(props);

        Schema schema = SchemaBuilder.struct().field("off16", Schema.INT16_SCHEMA).build();
        Struct value = new Struct(schema).put("off16", (short) 1);
        SinkRecord record = new SinkRecord(
                topic,
                0,
                Schema.STRING_SCHEMA,
                "key-1",
                schema,
                value,
                1L,
                System.currentTimeMillis(),
                TimestampType.CREATE_TIME,
                new ConnectHeaders());
        chst.put(List.of(record));

        // Guard: a failed insert and a missing MBean are otherwise indistinguishable.
        List<JSONObject> rows = ClickHouseTestHelpers.getAllRowsAsJson(chc, topic);
        if (rows.isEmpty()) {
            fail("Expected the probe record to land in " + topic + " before asserting on its MBean");
        }

        boolean hasPerTopicMBean = mbeanNamesForConnector(PER_TOPIC_LEAK_PROBE_CONNECTOR).stream()
                .anyMatch(name -> name.getKeyProperty("topic") != null);
        assertTrue(hasPerTopicMBean, "Expected a per-topic MBean registered for " + topic);

        throw new DeliberateMidTestFailure();
    }

    /**
     * D-08/HARN-02 proof: the {@code @AfterEach} sweep's {@code
     * com.clickhouse:type=ClickHouseKafkaConnector,*} pattern already matches the per-topic name
     * without modification, because {@code SinkTaskStatistics.getTopicMBeanName(topic)} is
     * composed from {@code getMBeanName()} plus a {@code ",topic="} suffix. If this pair ever goes
     * red, widening the sweep's pattern is the fix - not something to do pre-emptively.
     */
    @Test
    @Order(7)
    public void perTopicMBeanWasSwept() {
        Set<ObjectName> remaining = mbeanNamesForConnector(PER_TOPIC_LEAK_PROBE_CONNECTOR);
        assertTrue(remaining.isEmpty(), "Expected no MBeans left for " + PER_TOPIC_LEAK_PROBE_CONNECTOR + " after the sweep");
    }
}
