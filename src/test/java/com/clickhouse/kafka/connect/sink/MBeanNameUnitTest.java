package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free unit pins for {@link ProxySinkTask#buildMBeanName(String, int, String)} - JMX-01,
 * JMX-02 and JMX-03. New fork-only class (D-16): it deliberately extends nothing, so it stays in
 * {@code testUnit} - extending {@code ClickHouseBase} would inherit its {@code @Tag("clickhouse")}
 * and drop this class out of the Docker-free loop (Phase 1 D-12).
 */
class MBeanNameUnitTest {

    @Test
    @DisplayName("JMX-01: a connector name that is set renders connector=<name> in the full MBean name")
    void connectorNamePresentRendersFullMBeanName() {
        // Whole-string equality pins the extraction as byte-for-byte identical to the previous
        // inline logic, and simultaneously pins the ObjectName key order: type, connector, name,
        // version.
        assertEquals(
                "com.clickhouse:type=ClickHouseKafkaConnector,connector=my-connector,name=SinkTask3,version=0.9.6",
                ProxySinkTask.buildMBeanName("my-connector", 3, "0.9.6"));
    }
}
