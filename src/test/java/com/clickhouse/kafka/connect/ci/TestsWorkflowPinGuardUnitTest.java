package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pin for 14-SECURITY T-14-16 and CI-11 to CI-13, plus the Dependabot entry of CI-15,
 * so a regression of the {@code tests.yaml} hardening fails {@code testUnit} before any CI run.
 * Deliberately extends nothing - extending {@code ClickHouseBase} would inherit its {@code
 * clickhouse} tag and drop this class out of {@code testUnit}.
 *
 * <p>The workflow files are read as UTF-8 explicitly, because the test JVM runs with {@code
 * file.encoding=windows-1252}. No YAML library is involved: every rule is a line-level check on
 * the non-comment lines, and each assertion message names the offending line.
 */
class TestsWorkflowPinGuardUnitTest {
    private static final Path WORKFLOW = Path.of(".github", "workflows", "tests.yaml");
    private static final Path DEPENDABOT = Path.of(".github", "dependabot.yml");

    private static final Pattern USES_LINE = Pattern.compile("^\\s*(?:-\\s+)?uses:\\s.*$");
    private static final Pattern PINNED_USES =
            Pattern.compile(
                    "^\\s*(?:-\\s+)?uses:\\s+[\\w.-]+/[\\w./-]+@[0-9a-f]{40}\\s+#\\s+v\\d+\\.\\d+\\.\\d+\\s*$");
    private static final Pattern RUNS_ON = Pattern.compile("^\\s*runs-on:\\s*(.*?)\\s*$");
    private static final Pattern PERMISSIONS_KEY = Pattern.compile("^\\s*permissions:.*$");
    private static final Pattern WRITE_SCOPE = Pattern.compile(".*:\\s*write\\b.*");
    private static final Pattern LIST_ITEM = Pattern.compile("^\\s*-\\s.*$");

    private static final String EXPECTED_RUNNER = "ubuntu-24.04";
    private static final String FLOATING_RUNNER = "ubuntu-latest";
    private static final int MIN_PINNED_ACTIONS = 5;
    private static final String TOP_LEVEL_PERMISSIONS = "permissions:";
    private static final String READ_ONLY_CONTENTS = "  contents: read";
    private static final String CHECKOUT = "actions/checkout@";
    private static final String NO_PERSISTED_CREDENTIALS = "persist-credentials: false";
    private static final String GITHUB_ACTIONS_ECOSYSTEM = "- package-ecosystem: \"github-actions\"";
    private static final String GRADLE_ECOSYSTEM = "- package-ecosystem: \"gradle\"";

    private static List<String> nonCommentLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.stripLeading().startsWith("#"))
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("every action in tests.yaml is pinned to a full commit SHA with a version comment")
    void everyActionIsPinnedToAFullCommitShaWithAVersionComment() throws IOException {
        List<String> uses =
                nonCommentLines(WORKFLOW).stream()
                        .filter(line -> USES_LINE.matcher(line).matches())
                        .collect(Collectors.toList());

        assertTrue(
                uses.size() >= MIN_PINNED_ACTIONS,
                "Expected at least " + MIN_PINNED_ACTIONS + " 'uses:' lines in " + WORKFLOW
                        + " but found " + uses.size() + ": an empty or unreadable workflow must not pass");
        for (String line : uses) {
            assertTrue(
                    PINNED_USES.matcher(line).matches(),
                    "Action is not pinned to a 40-hex commit with a '# vX.Y.Z' comment: '"
                            + line.strip()
                            + "'");
        }
    }

    @Test
    @DisplayName("every job in tests.yaml runs on the pinned ubuntu-24.04 runner")
    void everyJobRunsOnUbuntu2404() throws IOException {
        List<String> lines = nonCommentLines(WORKFLOW);
        List<String> runners =
                lines.stream()
                        .map(RUNS_ON::matcher)
                        .filter(Matcher::matches)
                        .map(m -> m.group(1))
                        .collect(Collectors.toList());

        assertFalse(runners.isEmpty(), "No 'runs-on:' line found in " + WORKFLOW);
        for (String runner : runners) {
            assertEquals(
                    EXPECTED_RUNNER,
                    runner,
                    "runs-on must be the pinned runner, not '" + runner + "'");
        }
        for (String line : lines) {
            assertFalse(
                    line.contains(FLOATING_RUNNER),
                    "The floating runner label must not appear outside comments: '"
                            + line.strip()
                            + "'");
        }
    }

    @Test
    @DisplayName("the workflow token is read-only: one top-level permissions block, contents read")
    void workflowTokenIsReadOnly() throws IOException {
        List<String> lines = nonCommentLines(WORKFLOW);

        int blocks = 0;
        for (String line : lines) {
            if (PERMISSIONS_KEY.matcher(line).matches()) {
                blocks++;
            }
            assertFalse(
                    WRITE_SCOPE.matcher(line).matches(),
                    "No line may grant a write scope: '" + line.strip() + "'");
        }
        assertEquals(
                1,
                blocks,
                "Exactly one 'permissions:' line is allowed, at the top level, so every job inherits it");

        int top = lines.indexOf(TOP_LEVEL_PERMISSIONS);
        assertTrue(top >= 0, "Missing a column-0 '" + TOP_LEVEL_PERMISSIONS + "' line in " + WORKFLOW);
        String next =
                lines.subList(top + 1, lines.size()).stream()
                        .filter(line -> !line.isBlank())
                        .findFirst()
                        .orElse("");
        assertEquals(
                READ_ONLY_CONTENTS,
                next,
                "The top-level permissions block must hold only 'contents: read', found: '" + next + "'");
    }

    @Test
    @DisplayName("checkout does not persist the token in .git/config")
    void checkoutDoesNotPersistCredentials() throws IOException {
        List<String> lines = nonCommentLines(WORKFLOW);

        int checkouts = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).contains(CHECKOUT)) {
                continue;
            }
            checkouts++;
            boolean persistsNothing = false;
            for (int j = i + 1; j < lines.size() && !LIST_ITEM.matcher(lines.get(j)).matches(); j++) {
                persistsNothing |= lines.get(j).strip().equals(NO_PERSISTED_CREDENTIALS);
            }
            assertTrue(
                    persistsNothing,
                    "Checkout step must set '"
                            + NO_PERSISTED_CREDENTIALS
                            + "': '"
                            + lines.get(i).strip()
                            + "'");
        }
        assertTrue(checkouts >= 1, "No '" + CHECKOUT + "' step found in " + WORKFLOW);
    }

    @Test
    @DisplayName("dependabot.yml updates the github-actions ecosystem next to gradle")
    void dependabotUpdatesTheGithubActionsEcosystem() throws IOException {
        List<String> stripped =
                nonCommentLines(DEPENDABOT).stream()
                        .map(String::strip)
                        .collect(Collectors.toList());

        assertTrue(
                stripped.contains(GITHUB_ACTIONS_ECOSYSTEM),
                "Missing '" + GITHUB_ACTIONS_ECOSYSTEM + "' in " + DEPENDABOT);
        assertTrue(
                stripped.contains(GRADLE_ECOSYSTEM),
                "The existing '" + GRADLE_ECOSYSTEM + "' entry must stay in " + DEPENDABOT);
    }
}
