package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Docker-free coverage of the CI-20 archive rules, run on synthetic archives so every {@code
 * testUnit} run re-proves each rejection class that {@code verifyConfluentArchive} enforces on the
 * real archive. It extends nothing, because extending {@code ClickHouseBase} would inherit its
 * {@code clickhouse} tag and drop this class out of {@code testUnit}.
 */
class ConfluentArchiveCheckUnitTest {
    private static final String CONNECTOR_CLASS =
            "com/clickhouse/kafka/connect/ClickHouseSinkConnector.class";
    private static final String CLIENT_CLASS = "com/clickhouse/client/api/Client.class";
    private static final String ARCHIVE_ROOT = "clickhouse-kafka-connect-test/";
    private static final String LIB_JAR_PREFIX = ARCHIVE_ROOT + "lib/inner-";
    private static final List<String> HEALTHY_ENTRIES =
            List.of("com/clickhouse/", CONNECTOR_CLASS, CLIENT_CLASS);

    @TempDir Path tempDir;

    private static byte[] innerJar(List<String> entryNames) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (String name : entryNames) {
                zip.putNextEntry(new ZipEntry(name));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private Path archive(String fileName, List<List<String>> innerJars) throws IOException {
        Path archive = tempDir.resolve(fileName);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry(ARCHIVE_ROOT + "manifest.json"));
            zip.write("{}".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (int i = 0; i < innerJars.size(); i++) {
                zip.putNextEntry(new ZipEntry(LIB_JAR_PREFIX + i + "-confluent.jar"));
                zip.write(innerJar(innerJars.get(i)));
                zip.closeEntry();
            }
        }
        return archive;
    }

    private List<String> violations(String fileName, List<List<String>> innerJars) throws IOException {
        return ConfluentArchiveCheck.check(archive(fileName, innerJars)).violations();
    }

    private static List<String> healthyPlus(String... extraEntries) {
        List<String> entries = new ArrayList<>(HEALTHY_ENTRIES);
        entries.addAll(List.of(extraEntries));
        return entries;
    }

    @Test
    @DisplayName(
            "CI-20: the archive check accepts one lib jar holding the connector and client classes"
                    + " and reports its inner entry count")
    void acceptsOneLibJarWithTheConnectorAndClientClasses() throws IOException {
        Path archive = archive("healthy.zip", List.of(HEALTHY_ENTRIES));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ConfluentArchiveCheck.Result result = ConfluentArchiveCheck.check(archive);
        int exit =
                ConfluentArchiveCheck.run(
                        new String[] {archive.toString()},
                        new PrintStream(out, true, StandardCharsets.UTF_8),
                        new PrintStream(err, true, StandardCharsets.UTF_8));

        assertAll(
                () -> assertEquals(List.of(), result.violations()),
                () -> assertEquals(3, result.innerEntries()),
                () -> assertEquals(0, exit),
                () ->
                        assertEquals(
                                "verifyConfluentArchive: 3 inner entries, no nested jar, no Kafka API class"
                                        + System.lineSeparator(),
                                out.toString(StandardCharsets.UTF_8)),
                () -> assertEquals("", err.toString(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("CI-20: the archive check rejects a jar nested inside the inner jar")
    void rejectsAJarNestedInsideTheInnerJar() throws IOException {
        assertAll(
                () ->
                        assertEquals(
                                List.of("nested jar: dependency.jar"),
                                violations("root-jar.zip", List.of(healthyPlus("dependency.jar")))),
                () ->
                        assertEquals(
                                List.of("nested jar: lib/dependency.jar"),
                                violations(
                                        "lib-jar.zip", List.of(healthyPlus("lib/dependency.jar")))));
    }

    @Test
    @DisplayName(
            "CI-20: the archive check rejects Kafka Connect API and kafka-clients entries under"
                    + " org/apache/kafka/")
    void rejectsKafkaApiEntriesUnderOrgApacheKafka() throws IOException {
        assertAll(
                () ->
                        assertEquals(
                                List.of("Kafka API class: org/apache/kafka/connect/sink/SinkTask.class"),
                                violations(
                                        "connect-api.zip",
                                        List.of(
                                                healthyPlus(
                                                        "org/apache/kafka/connect/sink/SinkTask.class")))),
                () ->
                        assertEquals(
                                List.of(
                                        "Kafka API class: org/apache/kafka/common/config/ConfigDef.class"),
                                violations(
                                        "kafka-clients.zip",
                                        List.of(
                                                healthyPlus(
                                                        "org/apache/kafka/common/config/ConfigDef.class")))),
                () ->
                        assertEquals(
                                List.of(),
                                violations(
                                        "relocated.zip",
                                        List.of(healthyPlus("shaded/org/apache/kafka/Relocated.class")))));
    }

    @Test
    @DisplayName("CI-20: the archive check rejects an archive that does not hold exactly one lib jar")
    void rejectsAnArchiveWithoutExactlyOneLibJar() throws IOException {
        assertAll(
                () ->
                        assertEquals(
                                List.of(
                                        "expected exactly one jar under lib/ in the archive, found 0",
                                        "no inner jar entries read from "
                                                + tempDir.resolve("no-jar.zip"),
                                        "missing class: " + CONNECTOR_CLASS,
                                        "missing class: " + CLIENT_CLASS),
                                violations("no-jar.zip", List.of())),
                () ->
                        assertEquals(
                                List.of("expected exactly one jar under lib/ in the archive, found 2"),
                                violations("two-jars.zip", List.of(HEALTHY_ENTRIES, HEALTHY_ENTRIES))));
    }

    @Test
    @DisplayName(
            "CI-20: the archive check rejects an inner jar that is empty or lacks the connector or"
                    + " client class")
    void rejectsAnEmptyInnerJarOrOneMissingTheConnectorOrClientClass() throws IOException {
        assertAll(
                () ->
                        assertEquals(
                                List.of(
                                        "no inner jar entries read from " + tempDir.resolve("empty.zip"),
                                        "missing class: " + CONNECTOR_CLASS,
                                        "missing class: " + CLIENT_CLASS),
                                violations("empty.zip", List.of(List.of()))),
                () ->
                        assertEquals(
                                List.of("missing class: " + CLIENT_CLASS),
                                violations(
                                        "no-client.zip",
                                        List.of(List.of("com/clickhouse/", CONNECTOR_CLASS)))),
                () ->
                        assertEquals(
                                List.of("missing class: " + CONNECTOR_CLASS),
                                violations(
                                        "no-connector.zip", List.of(List.of("com/clickhouse/", CLIENT_CLASS)))));
    }

    @Test
    @DisplayName(
            "CI-20: a failing archive check exits 1 with the violation and entry counts and lists at"
                    + " most 20 violations")
    void failingCheckExitsOneWithCountsAndAtMostTwentyViolations() throws IOException {
        List<String> entries = new ArrayList<>(List.of(CONNECTOR_CLASS, CLIENT_CLASS));
        for (int i = 0; i < 25; i++) {
            entries.add("lib/nested-" + i + ".jar");
        }
        Path archive = archive("nested.zip", List.of(entries));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exit =
                ConfluentArchiveCheck.run(
                        new String[] {archive.toString()},
                        new PrintStream(out, true, StandardCharsets.UTF_8),
                        new PrintStream(err, true, StandardCharsets.UTF_8));
        List<String> errLines = err.toString(StandardCharsets.UTF_8).lines().toList();

        assertAll(
                () -> assertEquals(1, exit),
                () -> assertEquals("", out.toString(StandardCharsets.UTF_8)),
                () ->
                        assertEquals(
                                "Confluent archive check failed (25 violations of 27 entries):",
                                errLines.get(0)),
                () -> assertEquals(21, errLines.size(), "header plus exactly 20 listed violations"),
                () ->
                        assertTrue(
                                errLines.stream().skip(1).allMatch(line -> line.startsWith("nested jar: ")),
                                "every listed line is a nested jar: " + errLines));
    }
}
