package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Test
    @DisplayName("ORCH-03: the composed per-topic name equals the task-level name plus ,topic=<topic>, in key order type,connector,name,version,topic")
    void topicNameWholeStringShapeMatchesComposedLiteral() {
        // Uses the pure static plus a hand-supplied version - this case never moves when the
        // build's artifact version moves, unlike the instance-based cases below.
        assertEquals(
                "com.clickhouse:type=ClickHouseKafkaConnector,connector=my-connector,name=SinkTask3,version=0.9.6,topic=orders",
                SinkTaskStatistics.buildMBeanName("my-connector", 3, "0.9.6") + ",topic=orders");
    }

    @Test
    @DisplayName("ORCH-03: getTopicMBeanName(topic) is composed from getMBeanName() + \",topic=\" + topic, not a second format string (anti-drift)")
    void topicNameIsComposedFromTaskLevelName() {
        SinkTaskStatistics statistics = new SinkTaskStatistics("my-connector", 5);
        String topic = "orders";
        // Written relative to getMBeanName() itself, never a hardcoded artifact-version value -
        // this is what makes the two names structurally incapable of drifting in null handling,
        // colon handling or key order.
        assertEquals(statistics.getMBeanName() + ",topic=" + topic, statistics.getTopicMBeanName(topic));
    }

    @Test
    @DisplayName("ORCH-03: an empty topic renders getMBeanName() + \",topic=\" with an empty value, staying strictly longer than the task-level name")
    void emptyTopicRendersPresentButEmptyTopicValue() {
        SinkTaskStatistics statistics = new SinkTaskStatistics("my-connector", 6);
        assertEquals(statistics.getMBeanName() + ",topic=", statistics.getTopicMBeanName(""));
    }

    @Test
    @DisplayName("ORCH-02/ORCH-03: a null connector name renders connector=null in the topic name too, inherited from getMBeanName() by construction")
    void nullConnectorNameTopicNameInheritsNullHandling() {
        SinkTaskStatistics statistics = new SinkTaskStatistics(null, 7);
        assertTrue(statistics.getMBeanName().contains("connector=null"));
        assertEquals(statistics.getMBeanName() + ",topic=orders", statistics.getTopicMBeanName("orders"));
    }

    @Test
    @DisplayName("ORCH-04: a non-ASCII connector name and topic render verbatim, with the topic name a strict-prefix extension of the task name")
    void nonAsciiConnectorAndTopicRenderVerbatimAsStrictPrefixExtension() {
        // Unicode escapes, not literal non-ASCII characters: the test JVM runs windows-1252 by
        // deliberate default and the compiler's source encoding is not pinned, so a literal
        // non-ASCII character in the source is a portability hazard. The escapes keep the .java
        // file pure ASCII while the runtime string still holds the real characters.
        String connector = "\u043f\u0440\u0438\u0432\u0435\u0442"; // Cyrillic "privet"
        String topic = "\u4e3b\u9898"; // CJK "topic"
        SinkTaskStatistics statistics = new SinkTaskStatistics(connector, 8);
        String taskName = statistics.getMBeanName();
        String topicName = statistics.getTopicMBeanName(topic);
        assertTrue(topicName.startsWith(taskName + ","), topicName);
        assertNotEquals(taskName, topicName);
        assertTrue(taskName.contains(connector), taskName);
        assertTrue(topicName.contains(topic), topicName);
    }

    // D-05: ObjectName-hostile connector names (containing a comma, an equals sign, or a quote)
    // are deliberately NOT covered here. MBeanServerUtils.registerMBean swallows
    // MalformedObjectNameException with a WARN, so such a connector runs perfectly while silently
    // publishing no JMX metrics; quoting the name would change names operators already scrape and
    // needs its own decision.
}
