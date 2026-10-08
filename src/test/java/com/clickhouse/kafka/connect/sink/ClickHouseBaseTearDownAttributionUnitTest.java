package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pin for DEBT-04 (D-14): under the shared container the per-class database drop is
 * the only isolation between classes, so a failed drop has to fail the class that leaked it
 * rather than surface later as an unattributable error in an innocent class. This class
 * deliberately extends nothing - not {@link ClickHouseBase}, whose inherited
 * {@code @Tag("clickhouse")} would drop it out of {@code testUnit}.
 */
public class ClickHouseBaseTearDownAttributionUnitTest {

    private static final String LEAKY_CLASS = "SomeLeakyTest";
    private static final String LEAKED_DATABASE = "kafka_connect_test_1";

    @Test
    @DisplayName("DEBT-04: a failing drop throws IllegalStateException naming the test class and the database, with the original exception as its cause")
    void failingDropIsAttributedToTheLeakingClass() {
        RuntimeException boom = new RuntimeException("boom");

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> ClickHouseBase.dropSharedDatabaseOrAttribute(LEAKY_CLASS, LEAKED_DATABASE, () -> {
                    throw boom;
                }));

        assertTrue(thrown.getMessage().contains(LEAKY_CLASS), thrown.getMessage());
        assertTrue(thrown.getMessage().contains(LEAKED_DATABASE), thrown.getMessage());
        assertSame(boom, thrown.getCause());
    }

    @Test
    @DisplayName("DEBT-04: a succeeding drop runs exactly once and throws nothing")
    void succeedingDropThrowsNothing() {
        AtomicInteger drops = new AtomicInteger();

        assertDoesNotThrow(
                () -> ClickHouseBase.dropSharedDatabaseOrAttribute(LEAKY_CLASS, LEAKED_DATABASE, drops::incrementAndGet));

        assertEquals(1, drops.get());
    }
}
