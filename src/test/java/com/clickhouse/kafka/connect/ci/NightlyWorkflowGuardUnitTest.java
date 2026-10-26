package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free pin for NIT-01, D-01, D-04, D-05 and D-10: the fork-owned nightly integration workflow
 * {@code nightly-integration-tests.yaml}. Action pins are not checked here, because {@code
 * WorkflowActionPinGuardUnitTest} discovers the file from the directory and covers it unchanged.
 * The runner, token scope, checkout credentials and Gradle-step rules are reused from {@code
 * TestsWorkflowPinGuardUnitTest} instead of copied.
 *
 * <p>The file is read as UTF-8 through {@code TestsWorkflowPinGuardUnitTest.nonCommentLines},
 * because the test JVM runs with {@code file.encoding=windows-1252}. No YAML library is involved:
 * every rule is a line-level check on the non-comment lines, and each assertion message names the
 * offending line. Deliberately extends nothing - extending {@code ClickHouseBase} would inherit its
 * {@code clickhouse} tag and drop this class out of {@code testUnit}.
 *
 * <p>Every rule is also run on fixtures: {@link #VALID_NIGHTLY} must be accepted, and each reject
 * fixture is exactly one edit of it, built by helpers that throw when their anchor line is missing,
 * so a fixture can never silently equal the valid list.
 */
class NightlyWorkflowGuardUnitTest {
    static final Path NIGHTLY = Path.of(".github", "workflows", "nightly-integration-tests.yaml");
    static final String GATE =
            "github.event_name == 'workflow_dispatch' || vars.NIGHTLY_ENABLED == 'true'";
    static final Path SUMMARY_SCRIPT = Path.of(".github", "scripts", "failed_tests_summary.py");
    static final String SUMMARY_STEP = "Summarise failed tests";
    static final String SUMMARY_RUN =
            "run: python3 -I .github/scripts/failed_tests_summary.py build/test-results/integrationTest";
    static final String GRADLE_STEP = "Run integration tests";
    static final String INTEGRATION_TASK = "integrationTest";

    private static final String RESULTS_STEP = "Upload integration test results";
    private static final String REPORTS_STEP = "Upload integration test reports";
    private static final String RESULTS_PATH = "path: build/test-results/integrationTest/";
    private static final String REPORTS_PATH = "path: build/reports/tests/integrationTest/";
    private static final String ALWAYS = "if: always()";
    private static final String RETENTION = "retention-days: 14";
    private static final String JOBS_KEY = "jobs:";
    private static final String ON_KEY = "on:";
    private static final String SCHEDULE_KEY = "  schedule:";
    private static final String DISPATCH_KEY = "  workflow_dispatch:";
    private static final String IF_KEY = "if:";
    private static final String TIMEOUT_KEY = "timeout-minutes:";
    private static final String CLICKHOUSE_VERSION = "CLICKHOUSE_VERSION: latest";
    private static final String CLIENT_VERSION = "CLIENT_VERSION: V2";
    private static final String NIGHTLY_VARIABLE = "NIGHTLY_ENABLED";
    private static final String SECRET_REFERENCE = "secrets.";
    private static final String EXPRESSION_OPENER = "${{";
    private static final String VARIABLE_REFERENCE = "vars.";
    private static final String CLOUD = "cloud";
    private static final List<String> FORBIDDEN_GRADLE_TOKENS =
            List.of("--tests", "-Dclickhouse.", "CLICKHOUSE_CLUSTER_NAME");
    private static final List<String> TRIGGER_KEYS = List.of(SCHEDULE_KEY, DISPATCH_KEY);
    private static final int JOB_KEY_INDENT = 2;
    private static final int JOB_ATTRIBUTE_INDENT = 4;
    private static final int TRIGGER_INDENT = 2;
    private static final int MAX_JOB_MINUTES = 360;
    private static final Pattern JOB_KEY = Pattern.compile("^ {2}[A-Za-z0-9_-]+:\\s*$");
    private static final Pattern CRON_LINE =
            Pattern.compile("^ {4}- cron: '([0-9*/,-]+ ){4}[0-9*/,-]+'$");
    private static final Pattern TIMEOUT_VALUE = Pattern.compile("^timeout-minutes:\\s*(\\d{1,9})$");
    private static final Pattern VARIABLE_NAME = Pattern.compile("vars\\.([A-Za-z0-9_]+)");

    private static final String JOB_IF_PREFIX = "    if: ";
    private static final String STEP_ITEM_PREFIX = "      - name: ";
    private static final String JOB_GATE_LINE = JOB_IF_PREFIX + GATE;
    private static final String JOB_NAME_LINE = "    name: Integration tests, ClickHouse latest, client V2";
    private static final String JOB_TIMEOUT_LINE = "    timeout-minutes: 80";
    private static final String STEP_TIMEOUT_LINE = "        timeout-minutes: 70";
    private static final String CLICKHOUSE_VERSION_LINE = "          " + CLICKHOUSE_VERSION;
    private static final String CLIENT_VERSION_LINE = "          " + CLIENT_VERSION;
    private static final String GRADLE_RUN_LINE = "        run: ./gradlew integrationTest";
    private static final String STEP_ALWAYS_LINE = "        " + ALWAYS;
    private static final String SUMMARY_RUN_LINE = "        " + SUMMARY_RUN;
    private static final Pattern JOB_TIMEOUT_SOURCE = Pattern.compile("^ {4}timeout-minutes: \\d+$");
    private static final Pattern STEP_TIMEOUT_SOURCE = Pattern.compile("^ {8}timeout-minutes: \\d+$");

    /** The real file's non-comment lines, with the timeouts fixed so fixtures do not follow a re-measurement. */
    private static final List<String> VALID_NIGHTLY = validNightly();

    private static List<String> validNightly() {
        try {
            return TestsWorkflowPinGuardUnitTest.nonCommentLines(NIGHTLY).stream()
                    .map(
                            line ->
                                    JOB_TIMEOUT_SOURCE.matcher(line).matches()
                                            ? JOB_TIMEOUT_LINE
                                            : STEP_TIMEOUT_SOURCE.matcher(line).matches()
                                                    ? STEP_TIMEOUT_LINE
                                                    : line)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + NIGHTLY, e);
        }
    }

    private static int indent(String line) {
        return line.length() - line.stripLeading().length();
    }

    private static void assertNoProblem(Optional<String> problem) {
        problem.ifPresent(message -> fail(NIGHTLY + ": " + message));
    }

    private static List<String> nightlyLines() throws IOException {
        assertTrue(Files.isRegularFile(NIGHTLY), () -> "The nightly workflow does not exist: " + NIGHTLY);
        return TestsWorkflowPinGuardUnitTest.nonCommentLines(NIGHTLY);
    }

    /** The lines after the header up to the next non-blank column-0 line. */
    private static List<String> sectionAfter(List<String> lines, int header) {
        int start = header + 1;
        int end = start;
        while (end < lines.size() && (lines.get(end).isBlank() || indent(lines.get(end)) > 0)) {
            end++;
        }
        return lines.subList(start, end);
    }

    /** The non-blank lines deeper than the key at {@code index}, up to the next line at or below it. */
    private static List<String> childrenOf(List<String> section, int index) {
        int keyIndent = indent(section.get(index));
        List<String> children = new ArrayList<>();
        for (int i = index + 1; i < section.size(); i++) {
            String line = section.get(i);
            if (line.isBlank()) {
                continue;
            }
            if (indent(line) <= keyIndent) {
                break;
            }
            children.add(line);
        }
        return children;
    }

    private static Optional<List<String>> stepLines(List<String> lines, String step) {
        return TestsWorkflowPinGuardUnitTest.stepBlock(lines, step);
    }

    private static OptionalInt minutes(String line) {
        Matcher value = TIMEOUT_VALUE.matcher(line.strip());
        return value.matches() ? OptionalInt.of(Integer.parseInt(value.group(1))) : OptionalInt.empty();
    }

    static Optional<String> gateProblem(List<String> lines) {
        if (lines.stream().filter(JOBS_KEY::equals).count() != 1) {
            return Optional.of("Exactly one column-0 '" + JOBS_KEY + "' line is required");
        }
        List<String> section = sectionAfter(lines, lines.indexOf(JOBS_KEY));
        long jobs =
                section.stream()
                        .filter(line -> indent(line) == JOB_KEY_INDENT && JOB_KEY.matcher(line).matches())
                        .count();
        if (jobs != 1) {
            return Optional.of("Exactly one job is allowed under '" + JOBS_KEY + "', found " + jobs);
        }
        List<String> conditions =
                section.stream()
                        .filter(
                                line ->
                                        indent(line) == JOB_ATTRIBUTE_INDENT
                                                && line.stripLeading().startsWith(IF_KEY))
                        .toList();
        if (conditions.size() != 1) {
            return Optional.of(
                    "The job must carry exactly one job-level 'if:' line, found " + conditions.size());
        }
        if (!conditions.get(0).equals(JOB_GATE_LINE)) {
            return Optional.of(
                    "The job condition must be exactly '"
                            + GATE
                            + "', found: '"
                            + conditions.get(0).strip()
                            + "'");
        }
        return Optional.empty();
    }

    static Optional<String> summaryStepProblem(List<String> lines) {
        Optional<List<String>> block = stepLines(lines, SUMMARY_STEP);
        if (block.isEmpty()) {
            return Optional.of("No step named '" + SUMMARY_STEP + "'");
        }
        if (block.get().stream().noneMatch(line -> line.strip().equals(SUMMARY_RUN))) {
            return Optional.of("Step '" + SUMMARY_STEP + "' must hold the line '" + SUMMARY_RUN + "'");
        }
        if (!Files.isRegularFile(SUMMARY_SCRIPT)) {
            return Optional.of("The summary script does not exist: " + SUMMARY_SCRIPT);
        }
        return Optional.empty();
    }

    static Optional<String> triggerProblem(List<String> lines) {
        List<String> roots =
                lines.stream().filter(line -> indent(line) == 0 && line.startsWith(ON_KEY)).toList();
        if (roots.size() != 1 || !roots.get(0).equals(ON_KEY)) {
            return Optional.of(
                    "Exactly one column-0 '" + ON_KEY + "' block is required, found: " + roots);
        }
        List<String> section = sectionAfter(lines, lines.indexOf(ON_KEY));
        List<String> keys =
                section.stream()
                        .filter(line -> !line.isBlank() && indent(line) == TRIGGER_INDENT)
                        .sorted()
                        .toList();
        if (!keys.equals(TRIGGER_KEYS)) {
            return Optional.of(
                    "The triggers must be exactly schedule and workflow_dispatch, found: " + keys);
        }
        List<String> dispatchChildren = childrenOf(section, section.indexOf(DISPATCH_KEY));
        if (!dispatchChildren.isEmpty()) {
            return Optional.of(
                    "workflow_dispatch takes no inputs, found: '" + dispatchChildren.get(0).strip() + "'");
        }
        List<String> scheduleChildren = childrenOf(section, section.indexOf(SCHEDULE_KEY));
        if (scheduleChildren.size() != 1 || !CRON_LINE.matcher(scheduleChildren.get(0)).matches()) {
            return Optional.of("schedule must hold exactly one cron line, found: " + scheduleChildren);
        }
        return Optional.empty();
    }

    static Optional<String> timeoutProblem(List<String> lines) {
        List<String> jobTimeouts =
                lines.stream()
                        .filter(
                                line ->
                                        indent(line) == JOB_ATTRIBUTE_INDENT
                                                && line.stripLeading().startsWith(TIMEOUT_KEY))
                        .toList();
        if (jobTimeouts.size() != 1) {
            return Optional.of(
                    "The job must carry exactly one job-level '"
                            + TIMEOUT_KEY
                            + "' line, found "
                            + jobTimeouts.size());
        }
        Optional<List<String>> block = stepLines(lines, GRADLE_STEP);
        if (block.isEmpty()) {
            return Optional.of("No step named '" + GRADLE_STEP + "'");
        }
        List<String> stepTimeouts =
                block.get().stream().filter(line -> line.stripLeading().startsWith(TIMEOUT_KEY)).toList();
        if (stepTimeouts.size() != 1) {
            return Optional.of(
                    "Step '"
                            + GRADLE_STEP
                            + "' must carry exactly one '"
                            + TIMEOUT_KEY
                            + "' line, found "
                            + stepTimeouts.size());
        }
        OptionalInt job = minutes(jobTimeouts.get(0));
        OptionalInt step = minutes(stepTimeouts.get(0));
        if (job.isEmpty() || step.isEmpty()) {
            return Optional.of(
                    "Both timeouts must be integers, found: '"
                            + jobTimeouts.get(0).strip()
                            + "' and '"
                            + stepTimeouts.get(0).strip()
                            + "'");
        }
        if (step.getAsInt() <= 0 || step.getAsInt() >= job.getAsInt() || job.getAsInt() > MAX_JOB_MINUTES) {
            return Optional.of(
                    "The timeouts must satisfy 0 < step < job <= "
                            + MAX_JOB_MINUTES
                            + ", found step "
                            + step.getAsInt()
                            + " and job "
                            + job.getAsInt());
        }
        return Optional.empty();
    }

    static Optional<String> gradleStepEnvProblem(List<String> lines) {
        Optional<List<String>> block = stepLines(lines, GRADLE_STEP);
        if (block.isEmpty()) {
            return Optional.of("No step named '" + GRADLE_STEP + "'");
        }
        for (String required : List.of(CLICKHOUSE_VERSION, CLIENT_VERSION)) {
            if (block.get().stream().filter(line -> line.strip().equals(required)).count() != 1) {
                return Optional.of("Step '" + GRADLE_STEP + "' must hold exactly one '" + required + "'");
            }
        }
        for (String line : block.get()) {
            if (line.stripLeading().startsWith(IF_KEY)
                    || TestsWorkflowPinGuardUnitTest.CONTINUE_ON_ERROR.matcher(line).matches()) {
                return Optional.of(
                        "Step '" + GRADLE_STEP + "' must be unconditional and not tolerate errors: '"
                                + line.strip()
                                + "'");
            }
        }
        for (String line : lines) {
            String code = TestsWorkflowPinGuardUnitTest.TRAILING_COMMENT.matcher(line).replaceFirst("");
            for (String token : FORBIDDEN_GRADLE_TOKENS) {
                if (code.contains(token)) {
                    return Optional.of(
                            "The nightly must not use '" + token + "': '" + line.strip() + "'");
                }
            }
        }
        return Optional.empty();
    }

    static Optional<String> cloudReferenceProblem(List<String> lines) {
        for (String line : lines) {
            String shown = "'" + line.strip() + "'";
            if (line.contains(SECRET_REFERENCE) || line.contains(EXPRESSION_OPENER)) {
                return Optional.of("No secret or expression block is allowed: " + shown);
            }
            if (line.toLowerCase(Locale.ROOT).contains(CLOUD)) {
                return Optional.of("No cloud target is allowed: " + shown);
            }
            Matcher variable = VARIABLE_NAME.matcher(line);
            while (variable.find()) {
                if (!NIGHTLY_VARIABLE.equals(variable.group(1))) {
                    return Optional.of("Only vars." + NIGHTLY_VARIABLE + " may be read: " + shown);
                }
            }
            if (line.contains(VARIABLE_REFERENCE) && !line.equals(JOB_GATE_LINE)) {
                return Optional.of("vars. may appear only in the job condition: " + shown);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> alwaysStepProblem(
            List<String> lines, String step, String requiredLine) {
        Optional<List<String>> block = stepLines(lines, step);
        if (block.isEmpty()) {
            return Optional.of("No step named '" + step + "'");
        }
        List<String> stripped = block.get().stream().map(String::strip).toList();
        for (String required : List.of(ALWAYS, requiredLine)) {
            if (!stripped.contains(required)) {
                return Optional.of("Step '" + step + "' must hold the line '" + required + "'");
            }
        }
        return Optional.empty();
    }

    static Optional<String> alwaysStepsProblem(List<String> lines) {
        Optional<String> problem = alwaysStepProblem(lines, SUMMARY_STEP, SUMMARY_RUN);
        for (String[] upload :
                new String[][] {{RESULTS_STEP, RESULTS_PATH}, {REPORTS_STEP, REPORTS_PATH}}) {
            if (problem.isPresent()) {
                return problem;
            }
            problem = alwaysStepProblem(lines, upload[0], upload[1]);
            if (problem.isEmpty()
                    && !stepLines(lines, upload[0]).orElseThrow().stream()
                            .map(String::strip)
                            .toList()
                            .contains(RETENTION)) {
                problem = Optional.of("Step '" + upload[0] + "' must hold the line '" + RETENTION + "'");
            }
        }
        return problem;
    }

    private static int indexOfLine(List<String> base, String line) {
        int index = base.indexOf(line);
        if (index < 0) {
            throw new IllegalArgumentException("The fixture anchor line is not in the valid nightly: '" + line + "'");
        }
        return index;
    }

    private static int indexInStep(List<String> base, String step, String line) {
        int header = indexOfLine(base, STEP_ITEM_PREFIX + step);
        int index = base.subList(header, base.size()).indexOf(line);
        if (index < 0) {
            throw new IllegalArgumentException(
                    "The fixture anchor line is not in step '" + step + "': '" + line + "'");
        }
        return header + index;
    }

    private static List<String> splice(List<String> base, int index, int removeCount, String... added) {
        List<String> edited = new ArrayList<>(base);
        edited.subList(index, index + removeCount).clear();
        edited.addAll(index, Arrays.asList(added));
        return List.copyOf(edited);
    }

    private static List<String> replaced(List<String> base, String line, String... with) {
        return splice(base, indexOfLine(base, line), 1, with);
    }

    private static List<String> replaced(String line, String... with) {
        return replaced(VALID_NIGHTLY, line, with);
    }

    private static List<String> inserted(List<String> base, String after, String... added) {
        return splice(base, indexOfLine(base, after) + 1, 0, added);
    }

    private static List<String> inserted(String after, String... added) {
        return inserted(VALID_NIGHTLY, after, added);
    }

    private static List<String> removed(List<String> base, String line) {
        return splice(base, indexOfLine(base, line), 1);
    }

    private static List<String> removed(String line) {
        return removed(VALID_NIGHTLY, line);
    }

    private static List<String> replacedIn(String step, String line, String... with) {
        return splice(VALID_NIGHTLY, indexInStep(VALID_NIGHTLY, step, line), 1, with);
    }

    private static List<String> removedIn(String step, String line) {
        return replacedIn(step, line);
    }

    static Stream<Arguments> gateRejects() {
        String expression = "    if: " + EXPRESSION_OPENER + " " + GATE + " }}";
        return Stream.of(
                Arguments.of("a job without the gate", removed(JOB_GATE_LINE)),
                Arguments.of("the gate inside an expression block", replaced(JOB_GATE_LINE, expression)),
                Arguments.of(
                        "and instead of or",
                        replaced(
                                JOB_GATE_LINE,
                                JOB_IF_PREFIX
                                        + "github.event_name == 'workflow_dispatch' && vars.NIGHTLY_ENABLED == 'true'")),
                Arguments.of(
                        "a negated variable check",
                        replaced(
                                JOB_GATE_LINE,
                                JOB_IF_PREFIX
                                        + "github.event_name == 'workflow_dispatch' || vars.NIGHTLY_ENABLED != 'true'")),
                Arguments.of(
                        "a second job",
                        inserted(JOB_NAME_LINE, "  other:", "    runs-on: ubuntu-24.04")),
                Arguments.of(
                        "the gate on a step only",
                        inserted(removed(JOB_GATE_LINE), STEP_TIMEOUT_LINE, "        if: " + GATE)),
                Arguments.of("two gate lines", inserted(JOB_GATE_LINE, JOB_GATE_LINE)));
    }

    static Stream<Arguments> triggerRejects() {
        return Stream.of(
                Arguments.of("a push trigger", inserted(DISPATCH_KEY, "  push:")),
                Arguments.of("a pull_request trigger", inserted(DISPATCH_KEY, "  pull_request:")),
                Arguments.of(
                        "a pull_request_target trigger", inserted(DISPATCH_KEY, "  pull_request_target:")),
                Arguments.of("a workflow_run trigger", inserted(DISPATCH_KEY, "  workflow_run:")),
                Arguments.of(
                        "a repository_dispatch trigger", inserted(DISPATCH_KEY, "  repository_dispatch:")),
                Arguments.of("a workflow_call trigger", inserted(DISPATCH_KEY, "  workflow_call:")),
                Arguments.of(
                        "a flow list of triggers", replaced(ON_KEY, "on: [schedule, workflow_dispatch]")),
                Arguments.of("a dispatch input", inserted(DISPATCH_KEY, "    inputs:")),
                Arguments.of("no schedule", removed(SCHEDULE_KEY)),
                Arguments.of("no workflow_dispatch", removed(DISPATCH_KEY)));
    }

    static Stream<Arguments> timeoutRejects() {
        return Stream.of(
                Arguments.of("no job timeout", removed(JOB_TIMEOUT_LINE)),
                Arguments.of("no Gradle-step timeout", removed(STEP_TIMEOUT_LINE)),
                Arguments.of(
                        "a step timeout equal to the job's",
                        replaced(STEP_TIMEOUT_LINE, "        timeout-minutes: 80")),
                Arguments.of(
                        "a step timeout above the job's",
                        replaced(STEP_TIMEOUT_LINE, "        timeout-minutes: 90")),
                Arguments.of(
                        "a timeout that is not a number",
                        replaced(JOB_TIMEOUT_LINE, "    timeout-minutes: soon")),
                Arguments.of(
                        "a job timeout above 360", replaced(JOB_TIMEOUT_LINE, "    timeout-minutes: 361")));
    }

    static Stream<Arguments> gradleStepRejects() {
        return Stream.of(
                Arguments.of("no ClickHouse version", removed(CLICKHOUSE_VERSION_LINE)),
                Arguments.of(
                        "the cloud ClickHouse version",
                        replaced(CLICKHOUSE_VERSION_LINE, "          CLICKHOUSE_VERSION: cloud")),
                Arguments.of("no client version", removed(CLIENT_VERSION_LINE)),
                Arguments.of(
                        "another client version", replaced(CLIENT_VERSION_LINE, "          CLIENT_VERSION: V1")),
                Arguments.of(
                        "a test filter",
                        replaced(GRADLE_RUN_LINE, GRADLE_RUN_LINE + " --tests 'com.example.SomeTest'")),
                Arguments.of(
                        "a ClickHouse system property",
                        replaced(GRADLE_RUN_LINE, GRADLE_RUN_LINE + " -Dclickhouse.port=8443")),
                Arguments.of(
                        "a cluster name",
                        inserted(
                                CLIENT_VERSION_LINE,
                                "          CLICKHOUSE_CLUSTER_NAME: one_shard_three_replicas")),
                Arguments.of(
                        "error tolerance on the Gradle step",
                        inserted(STEP_TIMEOUT_LINE, "        continue-on-error: true")),
                Arguments.of(
                        "a condition on the Gradle step", inserted(STEP_TIMEOUT_LINE, "        if: success()")));
    }

    static Stream<Arguments> cloudReferenceRejects() {
        return Stream.of(
                Arguments.of(
                        "a secret in the step env",
                        inserted(
                                CLIENT_VERSION_LINE,
                                "          PASSWORD: " + EXPRESSION_OPENER + " secrets.PASSWORD }}")),
                Arguments.of(
                        "another variable in a step condition",
                        inserted(STEP_TIMEOUT_LINE, "        if: vars.OTHER == 'true'")),
                Arguments.of(
                        "an expression block in a run line",
                        replaced(
                                GRADLE_RUN_LINE,
                                GRADLE_RUN_LINE + " " + EXPRESSION_OPENER + " github.sha }}")),
                Arguments.of(
                        "a cloud host property",
                        replaced(GRADLE_RUN_LINE, GRADLE_RUN_LINE + " -Dclickhouse.cloud.host=example")),
                Arguments.of(
                        "a job name mentioning ClickHouse Cloud",
                        replaced(JOB_NAME_LINE, "    name: Integration tests, ClickHouse Cloud")));
    }

    static Stream<Arguments> alwaysStepRejects() {
        return Stream.of(
                Arguments.of(
                        "a summary step without always()", removedIn(SUMMARY_STEP, STEP_ALWAYS_LINE)),
                Arguments.of(
                        "a results upload without always()", removedIn(RESULTS_STEP, STEP_ALWAYS_LINE)),
                Arguments.of(
                        "a report upload without always()", removedIn(REPORTS_STEP, STEP_ALWAYS_LINE)),
                Arguments.of(
                        "an upload only on failure",
                        replacedIn(RESULTS_STEP, STEP_ALWAYS_LINE, "        if: failure()")),
                Arguments.of(
                        "the wrong results path",
                        replacedIn(
                                RESULTS_STEP,
                                "          " + RESULTS_PATH,
                                "          path: build/test-results/test/")),
                Arguments.of(
                        "the wrong report path",
                        replacedIn(
                                REPORTS_STEP,
                                "          " + REPORTS_PATH,
                                "          path: build/reports/tests/test/")),
                Arguments.of(
                        "a summary of another directory",
                        replaced(
                                SUMMARY_RUN_LINE,
                                "        run: python3 -I .github/scripts/failed_tests_summary.py"
                                        + " build/test-results/test")));
    }

    private static void assertAccepts(Function<List<String>, Optional<String>> rule) {
        Optional<String> problem = rule.apply(VALID_NIGHTLY);
        assertTrue(problem.isEmpty(), () -> "must accept the valid nightly but the rule said: " + problem.orElse(""));
    }

    private static void assertRejects(
            Function<List<String>, Optional<String>> rule, String fixture, List<String> lines) {
        assertTrue(rule.apply(lines).isPresent(), () -> "must reject '" + fixture + "' but the rule accepted it");
    }

    @Test
    @DisplayName(
            "the nightly workflow exists and passes the shared tests.yaml rules: ubuntu-24.04 runner,"
                    + " read-only token, checkout without persisted credentials, ./gradlew integrationTest in"
                    + " command position (NIT-01)")
    void nightlyPassesTheSharedWorkflowRules() throws IOException {
        List<String> lines = nightlyLines();
        assertNoProblem(TestsWorkflowPinGuardUnitTest.runsOnProblem(lines));
        assertNoProblem(TestsWorkflowPinGuardUnitTest.writeScopeProblem(lines));
        assertNoProblem(TestsWorkflowPinGuardUnitTest.permissionsBlockProblem(lines));
        assertNoProblem(TestsWorkflowPinGuardUnitTest.checkoutCredentialsProblem(lines));
        assertNoProblem(
                TestsWorkflowPinGuardUnitTest.gradlewStepProblem(lines, GRADLE_STEP, INTEGRATION_TASK));
    }

    @Test
    @DisplayName(
            "the nightly job is gated: a manual dispatch always runs and the schedule runs only when"
                    + " NIGHTLY_ENABLED is 'true' (D-01)")
    void nightlyJobIsGated() throws IOException {
        assertNoProblem(gateProblem(nightlyLines()));
    }

    @Test
    @DisplayName(
            "the failed-test summary step runs the stdlib script on the integrationTest results, and the"
                    + " script exists (D-10)")
    void failedTestSummaryStepRunsTheScript() throws IOException {
        assertNoProblem(summaryStepProblem(nightlyLines()));
    }

    @Test
    @DisplayName(
            "the nightly triggers are exactly schedule and workflow_dispatch, with no dispatch inputs"
                    + " (NIT-01, D-01)")
    void nightlyTriggersAreScheduleAndInputFreeDispatch() throws IOException {
        assertNoProblem(triggerProblem(nightlyLines()));
    }

    @Test
    @DisplayName("the nightly has a job timeout and a smaller timeout on the Gradle step (D-05)")
    void nightlyHasJobAndStepTimeouts() throws IOException {
        assertNoProblem(timeoutProblem(nightlyLines()));
    }

    @Test
    @DisplayName(
            "the Gradle step targets ClickHouse latest with client V2, runs the whole integrationTest task,"
                    + " and cannot pass on error (D-04, NIT-01)")
    void gradleStepRunsTheWholeTaskOnLatestWithClientV2() throws IOException {
        assertNoProblem(gradleStepEnvProblem(nightlyLines()));
    }

    @Test
    @DisplayName(
            "the nightly references no secret, no cloud target and no variable other than NIGHTLY_ENABLED,"
                    + " and holds no expression block (NIT-01)")
    void nightlyReferencesNoSecretCloudOrOtherVariable() throws IOException {
        assertNoProblem(cloudReferenceProblem(nightlyLines()));
    }

    @Test
    @DisplayName(
            "the summary and both uploads run always, and the uploads carry the XML results and the HTML"
                    + " report (D-10)")
    void summaryAndUploadsRunAlways() throws IOException {
        assertNoProblem(alwaysStepsProblem(nightlyLines()));
    }

    @Test
    @DisplayName("fixture: the gate rule accepts the valid nightly (D-01)")
    void gateRuleAcceptsTheValidNightly() {
        assertAccepts(NightlyWorkflowGuardUnitTest::gateProblem);
    }

    @Test
    @DisplayName("fixture: the trigger rule accepts the valid nightly (NIT-01, D-01)")
    void triggerRuleAcceptsTheValidNightly() {
        assertAccepts(NightlyWorkflowGuardUnitTest::triggerProblem);
    }

    @Test
    @DisplayName("fixture: the timeout rule accepts the valid nightly (D-05)")
    void timeoutRuleAcceptsTheValidNightly() {
        assertAccepts(NightlyWorkflowGuardUnitTest::timeoutProblem);
    }

    @Test
    @DisplayName("fixture: the Gradle step rule accepts the valid nightly (D-04, NIT-01)")
    void gradleStepRuleAcceptsTheValidNightly() {
        assertAccepts(NightlyWorkflowGuardUnitTest::gradleStepEnvProblem);
    }

    @Test
    @DisplayName("fixture: the cloud reference rule accepts the valid nightly (NIT-01)")
    void cloudReferenceRuleAcceptsTheValidNightly() {
        assertAccepts(NightlyWorkflowGuardUnitTest::cloudReferenceProblem);
    }

    @Test
    @DisplayName("fixture: the always-run step rule accepts the valid nightly (D-10)")
    void alwaysStepRuleAcceptsTheValidNightly() {
        assertAccepts(NightlyWorkflowGuardUnitTest::alwaysStepsProblem);
    }

    @ParameterizedTest(name = "fixture: the gate rule rejects {0} (D-01)")
    @MethodSource("gateRejects")
    void gateRuleRejects(String fixture, List<String> lines) {
        assertRejects(NightlyWorkflowGuardUnitTest::gateProblem, fixture, lines);
    }

    @ParameterizedTest(name = "fixture: the trigger rule rejects {0} (NIT-01, D-01)")
    @MethodSource("triggerRejects")
    void triggerRuleRejects(String fixture, List<String> lines) {
        assertRejects(NightlyWorkflowGuardUnitTest::triggerProblem, fixture, lines);
    }

    @ParameterizedTest(name = "fixture: the timeout rule rejects {0} (D-05)")
    @MethodSource("timeoutRejects")
    void timeoutRuleRejects(String fixture, List<String> lines) {
        assertRejects(NightlyWorkflowGuardUnitTest::timeoutProblem, fixture, lines);
    }

    @ParameterizedTest(name = "fixture: the Gradle step rule rejects {0} (D-04, NIT-01)")
    @MethodSource("gradleStepRejects")
    void gradleStepRuleRejects(String fixture, List<String> lines) {
        assertRejects(NightlyWorkflowGuardUnitTest::gradleStepEnvProblem, fixture, lines);
    }

    @ParameterizedTest(name = "fixture: the cloud reference rule rejects {0} (NIT-01)")
    @MethodSource("cloudReferenceRejects")
    void cloudReferenceRuleRejects(String fixture, List<String> lines) {
        assertRejects(NightlyWorkflowGuardUnitTest::cloudReferenceProblem, fixture, lines);
    }

    @ParameterizedTest(name = "fixture: the always-run step rule rejects {0} (D-10)")
    @MethodSource("alwaysStepRejects")
    void alwaysStepRuleRejects(String fixture, List<String> lines) {
        assertRejects(NightlyWorkflowGuardUnitTest::alwaysStepsProblem, fixture, lines);
    }
}
