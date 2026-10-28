package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.db.ClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.DBWriter;
import com.clickhouse.kafka.connect.sink.db.InMemoryDBWriter;
import com.clickhouse.kafka.connect.sink.dlq.ErrorReporter;
import com.clickhouse.kafka.connect.sink.helper.SchemalessTestData;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pin for TAL-02: the fork's {@code connectorName} constructor (ORCH-02 JMX naming) and
 * upstream's {@code DBWriter} test seam coexist on {@link ProxySinkTask} by delegating to one
 * four-argument core, so no caller passes an ambiguous null third argument. New fork-only class: it
 * deliberately extends nothing, so it stays in {@code testUnit} - extending {@code ClickHouseBase}
 * would inherit its {@code @Tag("clickhouse")} and drop this class out of the Docker-free loop.
 */
class ProxySinkTaskConstructorUnitTest {

    private static final String TOPIC = "tal02_constructor_pins";
    private static final String CORE_CONNECTOR_NAME = "tal02-core-connector";
    private static final String PUBLIC_CONNECTOR_NAME = "tal02-public-connector";
    private static final String PROCESSING_FIELD = "processing";
    private static final String DB_WRITER_FIELD = "dbWriter";
    private static final String MBEAN_NAME_KEY = "name";
    private static final String MBEAN_NAME_PREFIX = "SinkTask";
    private static final int RECORD_COUNT = 10;
    private static final ErrorReporter NO_ERROR_REPORTER = null;

    private static final List<Class<?>> CONNECTOR_NAME_SIGNATURE =
            List.of(ClickHouseSinkConfig.class, ErrorReporter.class, String.class);
    private static final List<Class<?>> SEAM_SIGNATURE =
            List.of(ClickHouseSinkConfig.class, ErrorReporter.class, DBWriter.class);
    private static final List<Class<?>> CORE_SIGNATURE =
            List.of(ClickHouseSinkConfig.class, ErrorReporter.class, String.class, DBWriter.class);

    private static ClickHouseSinkConfig config() {
        return new ClickHouseSinkConfig(new HashMap<>());
    }

    private static Object fieldValue(Object target, String fieldName) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Set<ObjectName> mBeansOfConnector(String connectorName) throws MalformedObjectNameException {
        return ManagementFactory.getPlatformMBeanServer()
                .queryNames(
                        new ObjectName("com.clickhouse:type=ClickHouseKafkaConnector,connector=" + connectorName + ",*"),
                        null);
    }

    private static boolean isPackagePrivate(Constructor<?> constructor) {
        int modifiers = constructor.getModifiers();
        return !Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers) && !Modifier.isPrivate(modifiers);
    }

    @Test
    @DisplayName("TAL-02: ProxySinkTask declares exactly the public connectorName constructor, the package-private DBWriter seam and the package-private four-argument core")
    void declaresExactlyTheConnectorNameConstructorTheDbWriterSeamAndTheFourArgumentCore() {
        Constructor<?>[] constructors = ProxySinkTask.class.getDeclaredConstructors();

        assertEquals(3, constructors.length);
        assertEquals(
                Set.of(CONNECTOR_NAME_SIGNATURE, SEAM_SIGNATURE, CORE_SIGNATURE),
                Arrays.stream(constructors)
                        .map(constructor -> Arrays.<Class<?>>asList(constructor.getParameterTypes()))
                        .collect(Collectors.toSet()));
        for (Constructor<?> constructor : constructors) {
            List<Class<?>> signature = Arrays.asList(constructor.getParameterTypes());
            assertFalse(constructor.getParameterCount() == 2, "A two-argument constructor must stay gone: " + signature);
            if (signature.equals(CONNECTOR_NAME_SIGNATURE)) {
                assertTrue(Modifier.isPublic(constructor.getModifiers()), "The connectorName constructor must stay public");
            } else {
                assertTrue(isPackagePrivate(constructor), "The seam and the core must stay package-private: " + signature);
            }
        }
    }

    @Test
    @DisplayName("TAL-02: the DBWriter seam constructor wires the supplied writer into Processing and routes inserts to it")
    void dbWriterSeamConstructorWiresTheSuppliedWriterIntoProcessingAndRoutesInsertsToIt() throws Exception {
        InMemoryDBWriter writer = new InMemoryDBWriter();
        ProxySinkTask task = new ProxySinkTask(config(), NO_ERROR_REPORTER, writer);
        List<SinkRecord> records = SchemalessTestData.createPrimitiveTypes(TOPIC, 0, RECORD_COUNT);

        assertSame(writer, fieldValue(task, DB_WRITER_FIELD));
        assertSame(writer, fieldValue(fieldValue(task, PROCESSING_FIELD), DB_WRITER_FIELD));

        task.put(records);

        assertEquals(RECORD_COUNT, writer.recordsInserted());
    }

    @Test
    @DisplayName("TAL-02: the connectorName constructor builds its own ClickHouseWriter and hands the same instance to Processing")
    void connectorNameConstructorBuildsItsOwnClickHouseWriter() throws Exception {
        ProxySinkTask task = new ProxySinkTask(config(), NO_ERROR_REPORTER, PUBLIC_CONNECTOR_NAME);

        Object writer = fieldValue(task, DB_WRITER_FIELD);

        assertInstanceOf(ClickHouseWriter.class, writer);
        assertSame(writer, fieldValue(fieldValue(task, PROCESSING_FIELD), DB_WRITER_FIELD));
    }

    @Test
    @DisplayName("TAL-02: the four-argument core registers the connector-named MBean on start and removes it on stop")
    void fourArgumentCoreRegistersTheConnectorNamedMBeanOnStartAndRemovesItOnStop() throws Exception {
        ProxySinkTask task = new ProxySinkTask(config(), NO_ERROR_REPORTER, CORE_CONNECTOR_NAME, new InMemoryDBWriter());
        assertTrue(mBeansOfConnector(CORE_CONNECTOR_NAME).isEmpty(), "No MBean may exist before start");

        task.start();
        try {
            Set<ObjectName> registered = mBeansOfConnector(CORE_CONNECTOR_NAME);

            assertEquals(1, registered.size(), "Expected exactly one MBean for the connector, saw: " + registered);
            assertEquals(
                    MBEAN_NAME_PREFIX + task.getId(),
                    registered.iterator().next().getKeyProperty(MBEAN_NAME_KEY));
        } finally {
            task.stop();
        }

        assertTrue(mBeansOfConnector(CORE_CONNECTOR_NAME).isEmpty(), "The MBean must be gone after stop");
    }
}
