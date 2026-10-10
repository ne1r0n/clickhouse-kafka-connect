package com.clickhouse.kafka.connect.sink;

import com.clickhouse.client.config.ClickHouseClientOption;
import com.clickhouse.kafka.connect.ClickHouseSinkConnector;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseCluster;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.util.jmx.MBeanServerUtils;
import com.google.crypto.tink.internal.Random;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.MountableFile;

@Tag("clickhouse")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClickHouseBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseBase.class);
    private static final AtomicLong CONNECTOR_NAME_COUNTER = new AtomicLong();
    private static final String DROP_FAILURE_MESSAGE_FORMAT =
            "Test class %s failed to drop its database %s on the shared ClickHouse container; "
                    + "later classes in this JVM fork may see its tables";

    /**
     * [D-01] Counts how many times the shared container was actually started in this test JVM
     * fork - not how many classes reused it. Exactly one increment site exists in this file
     * (the synchronized first-start branch of {@code setup()}), which is what makes {@code
     * sharedContainerStarts()} assertable independent of fork assignment.
     */
    private static final AtomicInteger SHARED_CONTAINER_STARTS = new AtomicInteger();

    /** [D-01] The one container shared by every class in this fork that does not opt out. */
    private static volatile ClickHouseContainer sharedDb;

    protected ClickHouseContainer db;
    protected ClickHouseCluster cluster;
    protected static boolean isCloud = ClickHouseTestHelpers.isCloud();
    protected static boolean isCluster = ClickHouseTestHelpers.isCluster();
    protected String database = ClickHouseTestHelpers.DATABASE_DEFAULT;

    /**
     * [D-02] Opt-out seam: an overriding class gets its own container (and therefore its own
     * {@link Network}) instead of the shared one. An overriding class's container start must not
     * move the shared-start counter - the two Toxiproxy classes both bind to the same network
     * alias and would collide if they landed on the shared container's network, and the counter
     * rule only holds if opting out is invisible to it.
     */
    protected boolean usesSharedContainer() {
        return true;
    }

    public static int sharedContainerStarts() {
        return SHARED_CONTAINER_STARTS.get();
    }

    /** [D-08] Exposes the shared container for identity assertions, without reflection. */
    protected static ClickHouseContainer sharedContainer() {
        return sharedDb;
    }

    @BeforeAll
    public void setup() throws IOException {
        Assertions.assertFalse(isCluster && isCloud, String.format("Invalid configuration: both %s=<> and %s=cloud are set. Please set only one or the other.", ClickHouseTestHelpers.CLICKHOUSE_CLUSTER_NAME, ClickHouseTestHelpers.CLICKHOUSE_VERSION));
        if (isCluster) {
            cluster = ClickHouseCluster.getClusterFromEnvVarOrThrow();
            cluster.start();
        } else if (!isCloud) {
            if (usesSharedContainer()) {
                synchronized (ClickHouseBase.class) {
                    if (sharedDb == null) {
                        setupContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE);
                        sharedDb = this.db;
                        SHARED_CONTAINER_STARTS.incrementAndGet();
                        LOGGER.info("Started shared ClickHouse container {} for this test JVM fork", ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE);
                    } else {
                        this.db = sharedDb;
                    }
                }
            } else {
                setupContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE);
            }
        }

        try (var tmpClient = ClickHouseTestHelpers.createClient(getBaseProps())) {
            setDatabase(String.format("kafka_connect_test_%d_%s", Math.abs(Random.randInt()), System.currentTimeMillis()));
            ClickHouseTestHelpers.createDatabase(database, tmpClient);
            tmpClient.ping();
        }
    }

    public void setupContainer(String clickhouseDockerImage) {
        Network network = Network.newNetwork();

        ClickHouseContainer container = new ClickHouseContainer(clickhouseDockerImage)
                .withNetwork(network)
                .withNetworkAliases(ClickHouseTestHelpers.CLICKHOUSE_DB_NETWORK_ALIAS)
                .withPassword("test_password")
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.getHostConfig().withMemory(1024 * 1024 * 1024 * 2L);
                })
                .withExposedPorts(8123)
                .withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1")
                // Enable embedded clickhouse-keeper + keeper_map_path_prefix so EO tests
                // can use the KeeperMap-backed connect_state table on standalone CH.
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("clickhouse_keeper_map.xml"),
                        "/etc/clickhouse-server/config.d/keeper_map.xml");
        // D-05: re-pull only a floating tag. Re-pulling a version the user deliberately pinned via
        // CLICKHOUSE_VERSION is pure waste and breaks offline runs for no benefit.
        if (ClickHouseTestHelpers.CLICKHOUSE_VERSION_LATEST.equals(ClickHouseTestHelpers.getClickhouseVersion())) {
            container = container.withImagePullPolicy(PullPolicy.alwaysPull());
        }
        this.db = container;
        db.start();
    }

    @AfterAll
    protected void tearDown() {
        if (isCluster) {
            cluster.stop();
        } else if (!isCloud) {
            ClickHouseContainer ch = getDb();
            if (ch != null) {
                copyServerLogs(ch);
                if (usesSharedContainer()) {
                    // D-01: the container survives for the next class in this fork - dropping the
                    // database is the only isolation mechanism left between classes sharing it.
                    // A failed drop now fails this class (DEBT-04, D-14).
                    dropSharedDatabaseOrAttribute(getClass().getSimpleName(), database, () -> {
                        try (var tmpClient = ClickHouseTestHelpers.createClient(getBaseProps())) {
                            ClickHouseTestHelpers.dropDatabase(tmpClient, database);
                        }
                    });
                } else {
                    LOGGER.info("Stopping db container: id={}, port={}", ch.getContainerId(), ch.getMappedPort(8123));
                    ch.stop();
                }
            }
        }
    }

    /** The drop of a per-class database, as an action that may throw. */
    @FunctionalInterface
    interface DatabaseDrop {
        void run() throws Exception;
    }

    /**
     * [DEBT-04, D-14] Runs the per-class database drop and, on failure, rethrows it naming the
     * leaking class and database so JUnit reports the failure against the class that caused it
     * instead of a later class tripping over the leftover tables.
     */
    static void dropSharedDatabaseOrAttribute(String testClassName, String database, DatabaseDrop drop) {
        try {
            drop.run();
        } catch (Exception e) {
            String message = String.format(DROP_FAILURE_MESSAGE_FORMAT, testClassName, database);
            LOGGER.error(message, e);
            throw new IllegalStateException(message, e);
        }
    }

    /**
     * [D-03] Copies server logs out of the still-running container, keyed by this test class's
     * simple name rather than a mapped port that up to 27 classes now share. Diagnostic
     * convenience only - a failed extraction is logged at WARN and swallowed so it can never fail
     * a class's teardown.
     */
    private void copyServerLogs(ClickHouseContainer ch) {
        try {
            ch.copyFileFromContainer("/var/log/clickhouse-server/clickhouse-server.log",
                    "./build/reports/tests/server_" + getClass().getSimpleName() + ".log");
            ch.copyFileFromContainer("/var/log/clickhouse-server/clickhouse-server.err.log",
                    "./build/reports/tests/server-err_" + getClass().getSimpleName() + ".log");
        } catch (Exception e) {
            LOGGER.warn("Unable to extract ClickHouse server logs", e);
        }
    }

    /**
     * [D-09a, HARN-02] Safety net: unregisters any {@code com.clickhouse:type=ClickHouseKafkaConnector}
     * MBean still registered after a test method, e.g. one that failed between {@code start()}
     * and {@code stop()}. Logs each at WARN and continues - this exists to stop a leak crossing
     * into the next test, not to police the test that leaked it.
     */
    @AfterEach
    protected void cleanupLeakedMBeans() {
        try {
            Set<ObjectName> leaked = ManagementFactory.getPlatformMBeanServer()
                    .queryNames(new ObjectName("com.clickhouse:type=ClickHouseKafkaConnector,*"), null);
            for (ObjectName name : leaked) {
                LOGGER.warn("Unregistering leaked MBean left by a test: {}", name);
                MBeanServerUtils.unregisterMBean(name.toString());
            }
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    public  ClickHouseContainer getDb() {
        return db;
    }

    public  void setDb(ClickHouseContainer db) {
        this.db = db;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    protected Map<String, String> getBaseProps() {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConnector.CLIENT_VERSION, ClickHouseTestHelpers.extractClientVersion());
        if (isCloud) {
            props.put(ClickHouseSinkConnector.HOSTNAME, System.getenv(ClickHouseTestHelpers.CLICKHOUSE_CLOUD_HOST));
            props.put(ClickHouseSinkConnector.PORT, ClickHouseTestHelpers.HTTPS_PORT);
            props.put(ClickHouseSinkConnector.DATABASE, database);
            props.put(ClickHouseSinkConnector.USERNAME, ClickHouseTestHelpers.USERNAME_DEFAULT);
            props.put(ClickHouseSinkConnector.PASSWORD, System.getenv(ClickHouseTestHelpers.CLICKHOUSE_CLOUD_PASSWORD));
            props.put(ClickHouseSinkConnector.SSL_ENABLED, "true");
            props.put(String.valueOf(ClickHouseClientOption.CONNECTION_TIMEOUT), "60000");
            props.put(ClickHouseSinkConfig.CLICKHOUSE_SETTINGS, "insert_quorum=3");
        } else if (isCluster) {
            props.putAll(cluster.getClusterProps(database));
        } else {
            props.put(ClickHouseSinkConnector.HOSTNAME, getDb().getHost());
            props.put(ClickHouseSinkConnector.PORT, getDb().getMappedPort(8123).toString());
            props.put(ClickHouseSinkConnector.DATABASE, database);
            props.put(ClickHouseSinkConnector.USERNAME, getDb().getUsername());
            props.put(ClickHouseSinkConnector.PASSWORD, getDb().getPassword());
            props.put(ClickHouseSinkConnector.SSL_ENABLED, "false");
        }
        return props;
    }

    protected String createTopicName(String name) {
        return String.format("%s_%d", name, System.currentTimeMillis());
    }

    protected String createTestUsername(String baseName) {
        return String.format("%s_%d", baseName, System.currentTimeMillis());
    }

    /**
     * Opt-in unique connector name for new/touched tests (D-10). {@code createProps()} stays
     * unchanged, so existing tests keep their current {@code connector=null} MBean names.
     */
    protected String uniqueConnectorName(TestInfo testInfo) {
        String methodName = testInfo.getTestMethod().map(java.lang.reflect.Method::getName).orElse("unknown");
        return getClass().getSimpleName() + "-" + methodName + "-" + CONNECTOR_NAME_COUNTER.incrementAndGet();
    }
}
