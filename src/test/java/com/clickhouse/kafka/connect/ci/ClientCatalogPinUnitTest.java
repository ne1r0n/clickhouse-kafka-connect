package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pin for CLT-02: the four ClickHouse client artifacts share one version ref, and that
 * ref is the fork's override of upstream's 0.9.6, marked so the next upstream sync sees it. It
 * extends nothing, so it stays in {@code testUnit}. Reverting the bump removes this class together
 * with the catalog line.
 */
class ClientCatalogPinUnitTest {

    private static final Path CATALOG = Path.of("gradle", "libs.versions.toml");
    private static final String VERSION_LINE = "clickHouseDriver = \"0.11.0\"  # FORK OVERRIDE (upstream 0.9.6)";
    private static final String VERSION_KEY = "clickHouseDriver";
    private static final String REF = "version.ref = \"clickHouseDriver\"";
    private static final List<String> CLIENT_MODULES = List.of(
            "module = \"com.clickhouse:clickhouse-client\"",
            "module = \"com.clickhouse:clickhouse-http-client\"",
            "module = \"com.clickhouse:clickhouse-data\"",
            "module = \"com.clickhouse:client-v2\"");

    @Test
    @DisplayName("CLT-02: clickHouseDriver is 0.11.0 and carries the FORK OVERRIDE marker naming upstream 0.9.6")
    void versionLineIsTheMarkedOverride() throws IOException {
        List<String> lines = Files.readAllLines(CATALOG);
        assertEquals(1, lines.stream().filter(l -> l.equals(VERSION_LINE)).count());
        assertEquals(1, lines.stream().filter(l -> l.startsWith(VERSION_KEY)).count());
    }

    @Test
    @DisplayName("CLT-02: all four client modules ride the one clickHouseDriver ref")
    void everyClientModuleUsesTheRef() throws IOException {
        List<String> lines = Files.readAllLines(CATALOG);
        for (String module : CLIENT_MODULES) {
            assertTrue(lines.stream().anyMatch(l -> l.contains(module) && l.contains(REF)), module);
        }
    }
}
