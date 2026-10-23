package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free pin for the AGENTS.md comment rule applied to shipped code: planning files are not
 * shipped, so an identifier pointing into them explains nothing to a reader of {@code src/main}. A
 * regression fails {@code testUnit} before any review. Deliberately extends nothing - extending
 * {@code ClickHouseBase} would inherit its {@code clickhouse} tag and drop this class out of {@code
 * testUnit}.
 *
 * <p>The sources are read as UTF-8 explicitly, because the test JVM runs with {@code
 * file.encoding=windows-1252}. The pattern is checked against planning samples and ordinary tokens
 * so that a typo in it cannot turn the scan into a vacuous pass.
 */
class SourcePlanningIdGuardUnitTest {
    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");
    private static final int MIN_SOURCE_FILES = 60;
    private static final String JAVA_SUFFIX = ".java";

    private static final Pattern PLANNING_REFERENCE =
            Pattern.compile(
                    "\\b(?:D|L|R|F|A|E|T|IN|WR|CR|RF)-\\d{1,3}\\b"
                            + "|\\b(?:WRT|COLS|DESC|TYPE|HLP|DEBT|NAME|JSONP|CMP|CFG|ORCH|MAP|BLD|HARN|CI|DEPS|OPS|ROLL|SEC|SYNC)-\\d{1,3}\\b"
                            + "|\\bPhase \\d+"
                            + "|\\bPitfall \\d+"
                            + "|\\bRESEARCH\\b"
                            + "|\\.planning/"
                            + "|\\b\\d{2}-(?:RESEARCH|CONTEXT|REVIEW|PLAN|UAT|SECURITY|VERIFICATION|SUMMARY)\\b"
                            + "|\\b[Bb]acklog \\d"
                            + "|\\b999\\.\\d+\\b");

    @Test
    @DisplayName("src/main carries no planning identifier on any line")
    void sourceTreeCarriesNoPlanningIdentifier() throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(SOURCE_ROOT)) {
            files =
                    walk.filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(JAVA_SUFFIX))
                            .sorted()
                            .collect(Collectors.toList());
        }
        assertTrue(
                files.size() >= MIN_SOURCE_FILES,
                "expected at least " + MIN_SOURCE_FILES + " source files under " + SOURCE_ROOT
                        + " but found " + files.size());

        List<String> hits = new ArrayList<>();
        for (Path file : files) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                if (PLANNING_REFERENCE.matcher(lines.get(i)).find()) {
                    hits.add(file + ":" + (i + 1) + ": " + lines.get(i).trim());
                }
            }
        }
        assertEquals(
                List.of(),
                hits,
                "planning identifiers in shipped source, drop them and keep the invariant:\n"
                        + String.join("\n", hits));
    }

    @ParameterizedTest(name = "the pattern matches the planning reference {0}")
    @ValueSource(
            strings = {
                "(D-01)",
                "WRT-13",
                "COLS-10",
                "Phase 28's",
                "22-RESEARCH Pitfall 8",
                "RESEARCH",
                "backlog 999.4",
                "CR-01",
                ".planning/ROADMAP.md",
                "T-27-01"
            })
    void patternMatchesPlanningReferences(String sample) {
        assertTrue(PLANNING_REFERENCE.matcher(sample).find(), sample);
    }

    @ParameterizedTest(name = "the pattern leaves the ordinary token {0} alone")
    @ValueSource(
            strings = {
                "UTF-8",
                "UTF-16",
                "upstream #820",
                "SHA-256",
                "1E-10",
                "ISO-8601",
                "issue #729"
            })
    void patternLeavesOrdinaryTokensAlone(String sample) {
        assertFalse(PLANNING_REFERENCE.matcher(sample).find(), sample);
    }
}
