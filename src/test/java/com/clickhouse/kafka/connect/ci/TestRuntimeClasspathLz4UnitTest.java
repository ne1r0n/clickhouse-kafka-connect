package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pin for backlog 999.2 (DEBT-07). The capability rule that selects the maintained
 * {@code at.yawk.lz4} fork does not fire on the test runtime classpath, so the Confluent 7.9.1 test
 * stack's {@code org.lz4:lz4-java:1.8.0} rode along with the same {@code net.jpountz.lz4} classes.
 * {@code Lz4RoundTripLiveTest} checks the code source of the first jar only, so this class counts
 * every jar that holds the class. It extends nothing, because extending {@code ClickHouseBase} would
 * inherit its {@code clickhouse} tag and drop this class out of {@code testUnit}.
 */
class TestRuntimeClasspathLz4UnitTest {
    private static final String LZ4_FACTORY_RESOURCE = "net/jpountz/lz4/LZ4Factory.class";
    private static final String MAINTAINED_LZ4_JAR_MARKER = "at.yawk.lz4";

    @Test
    @DisplayName("DEBT-07: LZ4Factory is on the test runtime classpath exactly once, from at.yawk.lz4")
    void lz4FactoryIsOnTheTestClasspathExactlyOnceFromTheMaintainedFork() throws IOException {
        List<URL> locations =
                Collections.list(getClass().getClassLoader().getResources(LZ4_FACTORY_RESOURCE));

        assertEquals(1, locations.size(), "LZ4Factory must be on the test classpath once, but was found at " + locations);
        assertTrue(
                locations.get(0).toString().contains(MAINTAINED_LZ4_JAR_MARKER),
                "LZ4Factory must come from the at.yawk.lz4 jar, but was found at " + locations.get(0));
    }
}
