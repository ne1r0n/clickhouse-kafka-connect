package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

    private static final String JOBS_KEY = "jobs:";
    private static final String JOB_IF_PREFIX = "    if: ";
    private static final String IF_KEY = "if:";
    private static final int JOB_KEY_INDENT = 2;
    private static final int JOB_ATTRIBUTE_INDENT = 4;
    private static final Pattern JOB_KEY = Pattern.compile("^ {2}[A-Za-z0-9_-]+:\\s*$");

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

    /** The lines after the column-0 {@code jobs:} line up to the next non-blank column-0 line. */
    private static List<String> jobsSection(List<String> lines) {
        int start = lines.indexOf(JOBS_KEY) + 1;
        int end = start;
        while (end < lines.size() && (lines.get(end).isBlank() || indent(lines.get(end)) > 0)) {
            end++;
        }
        return lines.subList(start, end);
    }

    static Optional<String> gateProblem(List<String> lines) {
        if (lines.stream().filter(JOBS_KEY::equals).count() != 1) {
            return Optional.of("Exactly one column-0 '" + JOBS_KEY + "' line is required");
        }
        List<String> section = jobsSection(lines);
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
        if (!conditions.get(0).equals(JOB_IF_PREFIX + GATE)) {
            return Optional.of(
                    "The job condition must be exactly '" + GATE + "', found: '" + conditions.get(0).strip() + "'");
        }
        return Optional.empty();
    }

    static Optional<String> summaryStepProblem(List<String> lines) {
        Optional<List<String>> block = TestsWorkflowPinGuardUnitTest.stepBlock(lines, SUMMARY_STEP);
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
}
