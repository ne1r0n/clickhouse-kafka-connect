package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Proves the {@link ClickHouseBase} test harness contract (INFRA-03, INFRA-04, D-05, D-08):
 * a single shared {@code latest} container per test JVM fork, and a fresh, isolated database
 * per test class.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClickHouseTestHarnessTest extends ClickHouseBase {

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
}
