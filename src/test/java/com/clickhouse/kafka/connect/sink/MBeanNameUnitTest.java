package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free unit pins for {@link SinkTaskStatistics#buildMBeanName(String, int, String)} - JMX-01,
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
                SinkTaskStatistics.buildMBeanName("my-connector", 3, "0.9.6"));
    }

    @Test
    @DisplayName("JMX-02: a null connector name renders the literal connector=null")
    void nullConnectorNameRendersLiteralConnectorNull() {
        // ClickHouseSinkTask.start() does a plain props.get("name"), so an absent `name` prop
        // passes null straight into String.format, whose %s conversion of a null argument is the
        // four-character string "null" - this is a pin, not a bug report.
        assertEquals(
                "com.clickhouse:type=ClickHouseKafkaConnector,connector=null,name=SinkTask0,version=0.9.6",
                SinkTaskStatistics.buildMBeanName(null, 0, "0.9.6"));
    }

    @Test
    @DisplayName("JMX-02: an empty connector name renders connector= followed immediately by name= (absent vs. empty are distinct)")
    void emptyConnectorNameRendersEmptyConnectorValue() {
        assertEquals(
                "com.clickhouse:type=ClickHouseKafkaConnector,connector=,name=SinkTask0,version=0.9.6",
                SinkTaskStatistics.buildMBeanName("", 0, "0.9.6"));
    }

    @Test
    @DisplayName("JMX-03: a null version renders version=unknown")
    void nullVersionRendersVersionUnknown() {
        assertEquals(
                "com.clickhouse:type=ClickHouseKafkaConnector,connector=c,name=SinkTask1,version=unknown",
                SinkTaskStatistics.buildMBeanName("c", 1, null));
    }

    @ParameterizedTest(name = "JMX-03: colon(s) in version {0} are rendered as underscores")
    @ValueSource(strings = {"1.2:3", "a:b:c"})
    @DisplayName("JMX-03: every colon in the version is replaced by an underscore")
    void colonInVersionIsReplacedByUnderscore(String versionWithColons) {
        String expectedVersionSuffix = "version=" + versionWithColons.replace(':', '_');
        String name = SinkTaskStatistics.buildMBeanName("c", 2, versionWithColons);
        // endsWith, not "contains no colon" for the whole string - the ObjectName domain
        // separator "com.clickhouse:" is itself a colon, so a whole-string colon ban would be
        // wrong.
        assertTrue(name.endsWith(expectedVersionSuffix), name);
    }

    // D-05: ObjectName-hostile connector names (containing a comma, an equals sign, or a quote)
    // are deliberately NOT covered here. MBeanServerUtils.registerMBean swallows
    // MalformedObjectNameException with a WARN, so such a connector runs perfectly while silently
    // publishing no JMX metrics; quoting the name would change names operators already scrape and
    // needs its own decision.
}
