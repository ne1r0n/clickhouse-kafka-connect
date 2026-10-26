package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free pin for CIG-01 and D-10: every fork-owned workflow under {@code .github/workflows/}
 * pins every action to a full commit SHA with a {@code # vX.Y.Z} comment, and Dependabot keeps those
 * workflows current. The files are discovered from the directory, so a new workflow is covered
 * without editing this class. Only {@code integrationTests.yaml} is skipped, because it stays
 * byte-identical to upstream.
 *
 * <p>Composite actions under {@code .github/actions/} are not read (D-12): no fork workflow calls
 * {@code use-clickhouse-java}. The rule therefore fails on every local {@code ./} or {@code
 * docker://} {@code uses:} value, which keeps an unread composite or image out of every guarded
 * workflow.
 *
 * <p>The files are read as UTF-8 explicitly, because the test JVM runs with {@code
 * file.encoding=windows-1252}. No YAML library is involved: the rule is a line-level check, and each
 * problem names the file, the line number and the offending text. Deliberately extends nothing -
 * extending {@code ClickHouseBase} would inherit its {@code clickhouse} tag and drop this class out
 * of {@code testUnit}.
 */
class WorkflowActionPinGuardUnitTest {
    private static final Path WORKFLOWS = Path.of(".github", "workflows");
    private static final String UPSTREAM_IDENTICAL = "integrationTests.yaml";
    private static final List<String> WORKFLOW_SUFFIXES = List.of(".yml", ".yaml");
    private static final List<String> FORK_WORKFLOWS =
            List.of("increment-version.yml", "pre-release.yaml", "release.yaml", "tests.yaml");

    // A uses: key inside a flow mapping such as "- { uses: actions/checkout@v4 }".
    private static final Pattern FLOW_USES = Pattern.compile("^[^#]*[{,]\\s*uses:\\s.*$");
    private static final Pattern USES_VALUE = Pattern.compile("uses:\\s*[\"']?([^\\s\"']+)");

    private static final String LOCAL_ACTION = "./";
    private static final String DOCKER_ACTION = "docker://";
    private static final String NOT_PINNED =
            "not pinned to a 40-hex commit SHA with a '# vX.Y.Z' comment";
    private static final String LOCAL_OR_DOCKER =
            "a local or docker:// action is not allowed, because this guard does not read composites"
                    + " or images";
    private static final String FIXTURE_FILE = "fixture.yml";
    private static final String FIXTURE_LINE_NUMBER = FIXTURE_FILE + ":5: ";
    private static final String CHECKOUT_PREFIX = "        uses: actions/checkout@";
    private static final String VERSION_COMMENT = " # v7.0.1";
    private static final String SHA = TestsWorkflowPinGuardUnitTest.SHA;

    @TempDir Path tempDir;

    static List<Path> guardedWorkflows(Path dir) throws IOException {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.filter(Files::isRegularFile)
                    .filter(WorkflowActionPinGuardUnitTest::isGuardedName)
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static boolean isGuardedName(Path file) {
        String name = file.getFileName().toString();
        return WORKFLOW_SUFFIXES.stream().anyMatch(name::endsWith) && !name.equals(UPSTREAM_IDENTICAL);
    }

    static boolean isUsesLine(String line) {
        if (line.stripLeading().startsWith("#")) {
            return false;
        }
        return TestsWorkflowPinGuardUnitTest.USES_LINE.matcher(line).matches()
                || FLOW_USES.matcher(line).matches();
    }

    static long usesLineCount(List<String> lines) {
        return lines.stream().filter(WorkflowActionPinGuardUnitTest::isUsesLine).count();
    }

    private static String reason(String line) {
        Matcher value = USES_VALUE.matcher(line);
        String action = value.find() ? value.group(1) : "";
        return action.startsWith(LOCAL_ACTION) || action.startsWith(DOCKER_ACTION)
                ? LOCAL_OR_DOCKER
                : NOT_PINNED;
    }

    static Optional<String> unpinnedUsesProblem(String file, List<String> lines) {
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (isUsesLine(line)
                    && !TestsWorkflowPinGuardUnitTest.PINNED_USES.matcher(line).matches()) {
                problems.add(file + ":" + (i + 1) + ": " + reason(line) + ": '" + line.strip() + "'");
            }
        }
        return problems.isEmpty() ? Optional.empty() : Optional.of(String.join("\n", problems));
    }

    private static List<String> names(List<Path> files) {
        return files.stream()
                .map(file -> file.getFileName().toString())
                .collect(Collectors.toList());
    }

    private static List<String> fixture(String form) {
        return List.of("name: fixture", "# a full-line comment", "", "jobs:", form);
    }

    static Stream<Arguments> unpinnedForms() {
        return Stream.of(
                Arguments.of("a major tag", "      - uses: actions/checkout@v4", NOT_PINNED),
                Arguments.of("a full version tag", "      - uses: actions/checkout@v4.2.0", NOT_PINNED),
                Arguments.of("a branch", "        uses: actions/checkout@main", NOT_PINNED),
                Arguments.of(
                        "a short SHA", "        uses: actions/checkout@abcdef0 # v4.0.0", NOT_PINNED),
                Arguments.of(
                        "a 39-hex SHA", CHECKOUT_PREFIX + SHA.substring(0, 39) + VERSION_COMMENT, NOT_PINNED),
                Arguments.of("a 41-hex SHA", CHECKOUT_PREFIX + SHA + "0" + VERSION_COMMENT, NOT_PINNED),
                Arguments.of(
                        "an upper-case SHA",
                        CHECKOUT_PREFIX + SHA.toUpperCase(Locale.ROOT) + VERSION_COMMENT,
                        NOT_PINNED),
                Arguments.of("a SHA without a comment", CHECKOUT_PREFIX + SHA, NOT_PINNED),
                Arguments.of("a SHA with a branch comment", CHECKOUT_PREFIX + SHA + " # main", NOT_PINNED),
                Arguments.of(
                        "a SHA with a short version comment", CHECKOUT_PREFIX + SHA + " # v7", NOT_PINNED),
                Arguments.of(
                        "a local action",
                        "        uses: ./.github/actions/use-clickhouse-java",
                        LOCAL_OR_DOCKER),
                Arguments.of("a docker image tag", "        uses: docker://alpine:3.20", LOCAL_OR_DOCKER),
                Arguments.of(
                        "a docker image digest",
                        "        uses: docker://alpine@sha256:" + "0".repeat(64),
                        LOCAL_OR_DOCKER),
                Arguments.of(
                        "a quoted value",
                        "        uses: \"actions/checkout@" + SHA + "\"" + VERSION_COMMENT,
                        NOT_PINNED),
                Arguments.of(
                        "a flow mapping", "      - { uses: actions/checkout@v4 }", NOT_PINNED),
                Arguments.of(
                        "an action without an owner",
                        "        uses: checkout@" + SHA + VERSION_COMMENT,
                        NOT_PINNED));
    }

    @Test
    @DisplayName(
            "every fork-owned workflow under .github/workflows pins every action to a full commit SHA"
                    + " with a version comment (CIG-01)")
    void everyForkWorkflowPinsEveryActionToAFullCommitShaWithAVersionComment() throws IOException {
        List<Path> files = guardedWorkflows(WORKFLOWS);
        List<String> found = names(files);
        assertTrue(
                found.containsAll(FORK_WORKFLOWS) && !found.contains(UPSTREAM_IDENTICAL),
                () ->
                        "expected the guarded workflows to include "
                                + FORK_WORKFLOWS
                                + " and not "
                                + UPSTREAM_IDENTICAL
                                + ", found "
                                + found);
        List<String> problems = new ArrayList<>();
        for (Path file : files) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            assertTrue(
                    usesLineCount(lines) >= 1,
                    () -> file + " has no uses: line, so the guard would pass it without reading an action");
            unpinnedUsesProblem(file.toString(), lines).ifPresent(problems::add);
        }
        if (!problems.isEmpty()) {
            fail(String.join("\n", problems));
        }
    }

    @Test
    @DisplayName(
            "dependabot.yml excludes only the upstream-identical integrationTests.yaml, so Dependabot"
                    + " keeps every guarded workflow current (D-10)")
    void dependabotExcludesOnlyTheUpstreamIdenticalWorkflow() throws IOException {
        assertEquals(
                List.of(TestsWorkflowPinGuardUnitTest.WORKFLOWS_DIR + UPSTREAM_IDENTICAL),
                TestsWorkflowPinGuardUnitTest.excludePaths(
                        TestsWorkflowPinGuardUnitTest.nonCommentLines(
                                TestsWorkflowPinGuardUnitTest.DEPENDABOT)),
                "exclude-paths of the github-actions entry must list only the upstream-identical"
                        + " workflow; a guarded workflow must not be excluded from Dependabot");
    }

    @Test
    @DisplayName(
            "fixture: discovery reads .yml and .yaml files from the directory and skips only"
                    + " integrationTests.yaml (CIG-01)")
    void discoveryReadsYmlAndYamlFilesAndSkipsOnlyTheUpstreamIdenticalOne() throws IOException {
        for (String name :
                List.of("a.yml", "b.yaml", "integrationTests.yaml", "integrationTests.yml", "notes.txt")) {
            Files.writeString(tempDir.resolve(name), "x", StandardCharsets.UTF_8);
        }
        Files.createDirectory(tempDir.resolve("dir.yaml"));
        Files.createDirectory(tempDir.resolve("sub"));
        Files.writeString(tempDir.resolve("sub").resolve("c.yml"), "x", StandardCharsets.UTF_8);
        assertEquals(
                List.of("a.yml", "b.yaml", "integrationTests.yml"), names(guardedWorkflows(tempDir)));
    }

    @Test
    @DisplayName(
            "fixture: the pin rule accepts full-SHA pins with a version comment in step, list and job"
                    + " form and skips comment lines (CIG-01)")
    void pinRuleAcceptsFullShaPinsInEveryForm() {
        List<String> lines =
                List.of(
                        "      - uses: actions/checkout@" + SHA + VERSION_COMMENT,
                        "        uses: gradle/actions/setup-gradle@" + SHA + " # v6.4.0",
                        "    uses: ne1r0n/shared/.github/workflows/build.yml@" + SHA + " # v1.0.0",
                        "      - uses: actions/setup-node@" + SHA + "   #   v7.1.0   ",
                        "      # - uses: actions/checkout@v3",
                        "        run: echo \"uses of this step are pinned\"");
        assertEquals(Optional.empty(), unpinnedUsesProblem(FIXTURE_FILE, lines));
        assertEquals(4, usesLineCount(lines));
    }

    @ParameterizedTest(name = "fixture: the pin rule rejects {0}, naming file and line (CIG-01)")
    @MethodSource("unpinnedForms")
    void pinRuleRejectsEveryUnpinnedForm(String label, String form, String expectedReason) {
        Optional<String> problem = unpinnedUsesProblem(FIXTURE_FILE, fixture(form));
        assertTrue(problem.isPresent(), () -> "must reject " + label + " '" + form.strip() + "'");
        String message = problem.get();
        assertTrue(message.contains(FIXTURE_LINE_NUMBER), message);
        assertTrue(message.contains(form.strip()), message);
        assertTrue(message.contains(expectedReason), message);
        String other = expectedReason.equals(NOT_PINNED) ? LOCAL_OR_DOCKER : NOT_PINNED;
        assertFalse(message.contains(other), message);
    }

    @Test
    @DisplayName(
            "fixture: a problem names the file and the line of every unpinned uses: line (CIG-01)")
    void problemNamesFileAndLineOfEveryUnpinnedUsesLine() {
        List<String> lines =
                List.of(
                        "name: fixture",
                        "jobs:",
                        "      - uses: actions/checkout@v4",
                        "      # - uses: actions/checkout@v3",
                        "      - uses: actions/checkout@" + SHA + VERSION_COMMENT,
                        "",
                        "        uses: ./local");
        String[] problem = unpinnedUsesProblem(FIXTURE_FILE, lines).orElse("").split("\n");
        assertEquals(2, problem.length, String.join(" | ", problem));
        assertTrue(problem[0].startsWith(FIXTURE_FILE + ":3: "), problem[0]);
        assertTrue(problem[1].startsWith(FIXTURE_FILE + ":7: "), problem[1]);
    }
}
