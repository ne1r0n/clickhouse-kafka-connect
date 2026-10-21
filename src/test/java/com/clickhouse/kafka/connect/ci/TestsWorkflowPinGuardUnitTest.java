package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

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
    private static final Path BUILD_SCRIPT = Path.of("build.gradle.kts");

    private static final Pattern USES_LINE = Pattern.compile("^\\s*(?:-\\s+)?uses:\\s.*$");
    private static final Pattern PINNED_USES =
            Pattern.compile(
                    "^\\s*(?:-\\s+)?uses:\\s+[\\w.-]+/[\\w./-]+@[0-9a-f]{40}\\s+#\\s+v\\d+\\.\\d+\\.\\d+\\s*$");
    // YAML needs whitespace before '#', so "ubuntu-24.04#x" keeps its tail and stays a wrong runner.
    private static final Pattern RUNS_ON =
            Pattern.compile("^\\s*runs-on:\\s*(.*?)\\s*(?:\\s#.*)?$");
    private static final Pattern PERMISSIONS_KEY = Pattern.compile("^\\s*permissions:.*$");
    // A key whose whole value is write or write-all, quoted or not. name: and run: carry free text.
    private static final Pattern WRITE_SCALAR =
            Pattern.compile("^\\s*(?!name:|run:)[\\w-]+:\\s*[\"']?write(?:-all)?[\"']?\\s*(?:\\s#.*)?$");
    private static final Pattern WRITE_FLOW =
            Pattern.compile("^\\s*permissions:\\s*\\{[^}]*:\\s*write\\b[^}]*\\}.*$");
    private static final Pattern LIST_ITEM = Pattern.compile("^\\s*-\\s.*$");
    private static final Pattern FORK_STEP =
            Pattern.compile("^(\\s*)-\\s+name:\\s*Report test fork count\\s*(?:\\s#.*)?$");
    private static final Pattern GRADLEW_REPORT =
            Pattern.compile("^(?!\\s*(?:echo|printf)\\b).*\\./gradlew\\s.*\\breportTestForks\\b.*$");
    private static final Pattern REGISTER_FORKS =
            Pattern.compile("^tasks\\.register\\(\"reportTestForks\"\\)\\s*\\{\\s*$");
    private static final Pattern EXCLUDE_ITEM =
            Pattern.compile("^\\s*-\\s*[\"']?([^\"'#]+?)[\"']?\\s*(?:#.*)?$");

    private static final String EXPECTED_RUNNER = "ubuntu-24.04";
    private static final String FLOATING_RUNNER = "ubuntu-latest";
    private static final int MIN_PINNED_ACTIONS = 5;
    private static final String TOP_LEVEL_PERMISSIONS = "permissions:";
    private static final String READ_ONLY_CONTENTS = "  contents: read";
    private static final String CHECKOUT = "actions/checkout@";
    private static final String NO_PERSISTED_CREDENTIALS = "persist-credentials: false";
    private static final String GITHUB_ACTIONS_ECOSYSTEM = "- package-ecosystem: \"github-actions\"";
    private static final String GRADLE_ECOSYSTEM = "- package-ecosystem: \"gradle\"";
    private static final String EXCLUDE_PATHS_KEY = "exclude-paths:";
    private static final String WORKFLOWS_DIR = ".github/workflows/";
    private static final List<String> UPSTREAM_WORKFLOWS =
            List.of(
                    WORKFLOWS_DIR + "increment-version.yml",
                    WORKFLOWS_DIR + "integrationTests.yaml",
                    WORKFLOWS_DIR + "pre-release.yaml",
                    WORKFLOWS_DIR + "release.yaml");
    private static final String TEST_TASK_READ = "tasks.named<Test>(\"test\")";
    private static final String MAX_PARALLEL_FORKS = "maxParallelForks";
    private static final String RAW_QUOTE = "\"\"\"";
    private static final String MSG_LINE_PREFIX = "msg=";
    private static final String NPROC_CONTEXT = "nproc=$(nproc)";

    private static List<String> nonCommentLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.stripLeading().startsWith("#"))
                .collect(Collectors.toList());
    }

    private static int indent(String line) {
        return line.length() - line.stripLeading().length();
    }

    private static void assertNoProblem(Optional<String> problem) {
        problem.ifPresent(message -> fail(message));
    }

    static Optional<String> pinnedUsesProblem(List<String> lines) {
        List<String> uses =
                lines.stream()
                        .filter(line -> USES_LINE.matcher(line).matches())
                        .collect(Collectors.toList());

        if (uses.size() < MIN_PINNED_ACTIONS) {
            return Optional.of(
                    "Expected at least " + MIN_PINNED_ACTIONS + " 'uses:' lines in " + WORKFLOW
                            + " but found " + uses.size() + ": an empty or unreadable workflow must not pass");
        }
        for (String line : uses) {
            if (!PINNED_USES.matcher(line).matches()) {
                return Optional.of(
                        "Action is not pinned to a 40-hex commit with a '# vX.Y.Z' comment: '"
                                + line.strip()
                                + "'");
            }
        }
        return Optional.empty();
    }

    static Optional<String> runsOnProblem(List<String> lines) {
        List<String> runners =
                lines.stream()
                        .map(RUNS_ON::matcher)
                        .filter(Matcher::matches)
                        .map(m -> m.group(1))
                        .collect(Collectors.toList());

        if (runners.isEmpty()) {
            return Optional.of("No 'runs-on:' line found in " + WORKFLOW);
        }
        for (String runner : runners) {
            if (!EXPECTED_RUNNER.equals(runner)) {
                return Optional.of("runs-on must be the pinned runner, not '" + runner + "'");
            }
        }
        for (String line : lines) {
            if (line.contains(FLOATING_RUNNER)) {
                return Optional.of(
                        "The floating runner label must not appear outside comments: '"
                                + line.strip()
                                + "'");
            }
        }
        return Optional.empty();
    }

    static Optional<String> writeScopeProblem(List<String> lines) {
        for (String line : lines) {
            if (WRITE_SCALAR.matcher(line).matches() || WRITE_FLOW.matcher(line).matches()) {
                return Optional.of("No line may grant a write scope: '" + line.strip() + "'");
            }
        }
        return Optional.empty();
    }

    static Optional<String> permissionsBlockProblem(List<String> lines) {
        long blocks = lines.stream().filter(line -> PERMISSIONS_KEY.matcher(line).matches()).count();
        if (blocks != 1) {
            return Optional.of(
                    "Exactly one 'permissions:' line is allowed, at the top level, so every job inherits it");
        }

        int top = lines.indexOf(TOP_LEVEL_PERMISSIONS);
        if (top < 0) {
            return Optional.of("Missing a column-0 '" + TOP_LEVEL_PERMISSIONS + "' line in " + WORKFLOW);
        }
        String next =
                lines.subList(top + 1, lines.size()).stream()
                        .filter(line -> !line.isBlank())
                        .findFirst()
                        .orElse("");
        if (!READ_ONLY_CONTENTS.equals(next)) {
            return Optional.of(
                    "The top-level permissions block must hold only 'contents: read', found: '" + next + "'");
        }
        return Optional.empty();
    }

    static Optional<String> checkoutCredentialsProblem(List<String> lines) {
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
            if (!persistsNothing) {
                return Optional.of(
                        "Checkout step must set '"
                                + NO_PERSISTED_CREDENTIALS
                                + "': '"
                                + lines.get(i).strip()
                                + "'");
            }
        }
        if (checkouts < 1) {
            return Optional.of("No '" + CHECKOUT + "' step found in " + WORKFLOW);
        }
        return Optional.empty();
    }

    static Optional<String> dependabotEcosystemProblem(List<String> lines) {
        List<String> stripped = lines.stream().map(String::strip).collect(Collectors.toList());

        if (!stripped.contains(GITHUB_ACTIONS_ECOSYSTEM)) {
            return Optional.of("Missing '" + GITHUB_ACTIONS_ECOSYSTEM + "' in " + DEPENDABOT);
        }
        if (!stripped.contains(GRADLE_ECOSYSTEM)) {
            return Optional.of(
                    "The existing '" + GRADLE_ECOSYSTEM + "' entry must stay in " + DEPENDABOT);
        }
        return Optional.empty();
    }

    static Optional<String> forkCountStepProblem(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            Matcher step = FORK_STEP.matcher(lines.get(i));
            if (!step.matches()) {
                continue;
            }
            int stepIndent = step.group(1).length();
            for (int j = i + 1; j < lines.size(); j++) {
                String line = lines.get(j);
                if (!line.isBlank() && indent(line) <= stepIndent) {
                    break;
                }
                if (GRADLEW_REPORT.matcher(line).matches()) {
                    return nprocProblem(lines);
                }
            }
            return Optional.of(
                    "Step 'Report test fork count' in " + WORKFLOW
                            + " must call './gradlew ... reportTestForks' in its run block");
        }
        return Optional.of("No step named 'Report test fork count' in " + WORKFLOW);
    }

    static Optional<String> nprocProblem(List<String> lines) {
        for (String line : lines) {
            if (!line.contains("nproc")) {
                continue;
            }
            String stripped = line.strip();
            if (stripped.startsWith(MSG_LINE_PREFIX)
                    && !stripped.replace(NPROC_CONTEXT, "").contains("nproc")) {
                continue;
            }
            return Optional.of(
                    "nproc may appear only in the msg= context line of " + WORKFLOW + ", found: '"
                            + stripped + "'");
        }
        return Optional.empty();
    }

    static Optional<String> forkTaskProblem(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (!REGISTER_FORKS.matcher(lines.get(i)).matches()) {
                continue;
            }
            boolean readsTestTask = false;
            boolean readsForks = false;
            for (int j = i + 1; j < lines.size() && !lines.get(j).equals("}"); j++) {
                readsTestTask |= lines.get(j).contains(TEST_TASK_READ);
                readsForks |= lines.get(j).contains(MAX_PARALLEL_FORKS);
            }
            if (readsTestTask && readsForks) {
                return Optional.empty();
            }
            return Optional.of(
                    "reportTestForks in " + BUILD_SCRIPT + " must read " + MAX_PARALLEL_FORKS
                            + " from " + TEST_TASK_READ);
        }
        return Optional.of(
                "tasks.register(\"reportTestForks\") not found in " + BUILD_SCRIPT + " outside comments");
    }

    static Optional<String> dependabotExcludeProblem(List<String> lines, Predicate<String> exists) {
        if (lines.stream().noneMatch(line -> line.strip().equals(GITHUB_ACTIONS_ECOSYSTEM))) {
            return Optional.of("No '" + GITHUB_ACTIONS_ECOSYSTEM + "' entry in " + DEPENDABOT);
        }
        List<String> paths = excludePaths(lines);
        if (paths.isEmpty()) {
            return Optional.of(
                    "The github-actions entry in " + DEPENDABOT + " has no exclude-paths list");
        }
        for (String upstream : UPSTREAM_WORKFLOWS) {
            if (!paths.contains(upstream)) {
                return Optional.of("exclude-paths of the github-actions entry must list '" + upstream + "'");
            }
        }
        for (String path : paths) {
            if (!path.startsWith(WORKFLOWS_DIR) || !exists.test(path)) {
                return Optional.of(
                        "exclude-paths lists '" + path
                                + "', which is not an existing file under " + WORKFLOWS_DIR);
            }
        }
        return Optional.empty();
    }

    private enum KtsState {
        CODE,
        STRING,
        CHAR,
        RAW_STRING,
        LINE_COMMENT,
        BLOCK_COMMENT
    }

    /**
     * Returns the code of a Kotlin build script with comments removed and line structure kept: the
     * result has as many lines as {@code text.lines()}, and a line that held only a comment is blank.
     * Recognised: string and char literals with backslash escapes, raw (triple-quoted) strings, line
     * comments including trailing ones, and nested block comments. A comment opener inside a literal
     * is therefore text, not a comment.
     *
     * <p>Not supported: a quote nested inside the braces of a string template ({@code "${f("x")}"}).
     * The current script has none ({@code grep -nE '\$\{[^}]*"' build.gradle.kts} is empty).
     */
    static List<String> ktsCode(String text) {
        StringBuilder code = new StringBuilder(text.length());
        KtsState state = KtsState.CODE;
        int depth = 0;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            switch (state) {
                case CODE:
                    if (text.startsWith(RAW_QUOTE, i)) {
                        code.append(RAW_QUOTE);
                        i += RAW_QUOTE.length();
                        state = KtsState.RAW_STRING;
                    } else if (c == '"') {
                        code.append(c);
                        i++;
                        state = KtsState.STRING;
                    } else if (c == '\'') {
                        code.append(c);
                        i++;
                        state = KtsState.CHAR;
                    } else if (text.startsWith("//", i)) {
                        i += 2;
                        state = KtsState.LINE_COMMENT;
                    } else if (text.startsWith("/*", i)) {
                        i += 2;
                        depth = 1;
                        state = KtsState.BLOCK_COMMENT;
                    } else {
                        code.append(c);
                        i++;
                    }
                    break;
                case STRING:
                case CHAR:
                    code.append(c);
                    i++;
                    if (c == '\\' && i < text.length()) {
                        code.append(text.charAt(i));
                        i++;
                    } else if (c == '\n' || c == (state == KtsState.STRING ? '"' : '\'')) {
                        state = KtsState.CODE;
                    }
                    break;
                case RAW_STRING:
                    if (text.startsWith(RAW_QUOTE, i)) {
                        code.append(RAW_QUOTE);
                        i += RAW_QUOTE.length();
                        while (i < text.length() && text.charAt(i) == '"') {
                            code.append('"');
                            i++;
                        }
                        state = KtsState.CODE;
                    } else {
                        code.append(c);
                        i++;
                    }
                    break;
                case LINE_COMMENT:
                    if (c == '\n') {
                        code.append(c);
                        state = KtsState.CODE;
                    }
                    i++;
                    break;
                case BLOCK_COMMENT:
                    if (text.startsWith("/*", i)) {
                        depth++;
                        i += 2;
                    } else if (text.startsWith("*/", i)) {
                        depth--;
                        i += 2;
                        if (depth == 0) {
                            state = KtsState.CODE;
                        }
                    } else {
                        if (c == '\n') {
                            code.append(c);
                        }
                        i++;
                    }
                    break;
            }
        }
        return code.toString().lines().collect(Collectors.toList());
    }

    static List<String> excludePaths(List<String> lines) {
        int entry = -1;
        for (int i = 0; i < lines.size() && entry < 0; i++) {
            if (lines.get(i).strip().equals(GITHUB_ACTIONS_ECOSYSTEM)) {
                entry = i;
            }
        }
        List<String> paths = new ArrayList<>();
        if (entry < 0) {
            return paths;
        }
        int entryIndent = indent(lines.get(entry));
        int keyIndent = -1;
        for (int j = entry + 1; j < lines.size(); j++) {
            String line = lines.get(j);
            if (line.isBlank()) {
                continue;
            }
            if (indent(line) <= entryIndent) {
                break;
            }
            if (keyIndent < 0) {
                if (line.strip().equals(EXCLUDE_PATHS_KEY)) {
                    keyIndent = indent(line);
                }
                continue;
            }
            if (indent(line) <= keyIndent) {
                break;
            }
            Matcher item = EXCLUDE_ITEM.matcher(line);
            if (item.matches()) {
                paths.add(item.group(1));
            }
        }
        return paths;
    }

    private static String label(List<String> lines) {
        return lines.isEmpty()
                ? "(empty list)"
                : lines.stream().map(String::strip).collect(Collectors.joining(" / "));
    }

    private static Executable accepts(String fixture, Optional<String> problem) {
        return () ->
                assertTrue(
                        problem.isEmpty(),
                        () -> "must accept '" + fixture + "' but the rule said: " + problem.orElse(""));
    }

    private static Executable rejects(String fixture, Optional<String> problem) {
        return () ->
                assertTrue(
                        problem.isPresent(), () -> "must reject '" + fixture + "' but the rule accepted it");
    }

    private static Executable accepts(
            Function<List<String>, Optional<String>> rule, List<String> lines) {
        return accepts(label(lines), rule.apply(lines));
    }

    private static Executable accepts(Function<List<String>, Optional<String>> rule, String... lines) {
        return accepts(rule, Arrays.asList(lines));
    }

    private static Executable rejects(
            Function<List<String>, Optional<String>> rule, List<String> lines) {
        return rejects(label(lines), rule.apply(lines));
    }

    private static Executable rejects(Function<List<String>, Optional<String>> rule, String... lines) {
        return rejects(rule, Arrays.asList(lines));
    }

    private static Executable ktsAccepts(String text) {
        return accepts(text.replace("\n", " / "), forkTaskProblem(ktsCode(text)));
    }

    private static Executable ktsRejects(String text) {
        return rejects(text.replace("\n", " / "), forkTaskProblem(ktsCode(text)));
    }

    private static final String SHA = "3d3c42e5aac5ba805825da76410c181273ba90b1";

    private static String pinnedUses(String action, String version) {
        return "      - uses: " + action + "@" + SHA + " # " + version;
    }

    private static List<String> plus(List<String> base, String... more) {
        List<String> all = new ArrayList<>(base);
        all.addAll(Arrays.asList(more));
        return all;
    }

    private static final List<String> FIVE_PINNED =
            List.of(
                    pinnedUses("actions/checkout", "v7.0.1"),
                    pinnedUses("actions/setup-java", "v6.0.1"),
                    pinnedUses("gradle/actions/setup-gradle", "v6.4.0"),
                    pinnedUses("actions/upload-artifact", "v4.6.2"),
                    "        uses: actions/cache@" + SHA + " # v4.2.0");

    private static final List<String> JOB_LEVEL_WRITE_BLOCK =
            List.of(
                    "permissions:",
                    "  contents: read",
                    "jobs:",
                    "  build:",
                    "    permissions:",
                    "      contents: write");

    private static final String STEP_HEADER = "      - name: Report test fork count";
    private static final String NEXT_STEP = "      - name: Compile integration-test sources";
    private static final String FORK_CALL =
            "          forks=\"$(./gradlew -q --console=plain reportTestForks | tail -n 1)\"";
    private static final String NPROC_MSG =
            "          msg=\"runner nproc=$(nproc), Gradle maxParallelForks=${forks}"
                    + " (read from the test task in build.gradle.kts)\"";
    private static final List<String> STEP_BODY =
            List.of(
                    FORK_CALL,
                    "          case \"$forks\" in",
                    "            ''|*[!0-9]*)",
                    "              echo \"::error::reportTestForks did not print an integer: '${forks}'\"",
                    "              exit 1",
                    "              ;;",
                    "          esac",
                    NPROC_MSG,
                    "          echo \"$msg\"");

    private static List<String> forkStep(List<String> body) {
        List<String> lines = new ArrayList<>(List.of("    steps:", STEP_HEADER, "        run: |"));
        lines.addAll(body);
        return lines;
    }

    private static final String REGISTER_BLOCK =
            String.join(
                    "\n",
                    "tasks.register(\"reportTestForks\") {",
                    "    group = \"verification\"",
                    "    description = \"Prints the effective maxParallelForks of the test task\"",
                    "    val forks = tasks.named<Test>(\"test\").map { it.maxParallelForks }",
                    "    doLast {",
                    "        println(forks.get())",
                    "    }",
                    "}");

    private static final List<String> UPSTREAM =
            List.of(
                    ".github/workflows/increment-version.yml",
                    ".github/workflows/integrationTests.yaml",
                    ".github/workflows/pre-release.yaml",
                    ".github/workflows/release.yaml");
    private static final Set<String> EXISTING_WORKFLOWS =
            Set.copyOf(plus(UPSTREAM, ".github/workflows/tests.yaml"));

    private static void appendExcludes(List<String> lines, List<String> paths) {
        if (paths.isEmpty()) {
            return;
        }
        lines.add("    exclude-paths:");
        paths.forEach(path -> lines.add("      - \"" + path + "\""));
    }

    private static List<String> dependabot(List<String> gradleExcludes, List<String> actionsExcludes) {
        List<String> lines =
                new ArrayList<>(
                        List.of(
                                "version: 2",
                                "updates:",
                                "  - package-ecosystem: \"gradle\"",
                                "    directory: \"/\""));
        appendExcludes(lines, gradleExcludes);
        lines.addAll(
                List.of(
                        "    schedule:",
                        "      interval: \"monthly\"",
                        "  - package-ecosystem: \"github-actions\"",
                        "    directory: \"/\""));
        appendExcludes(lines, actionsExcludes);
        lines.addAll(
                List.of(
                        "    schedule:",
                        "      interval: \"monthly\"",
                        "    labels:",
                        "      - \"dependencies\""));
        return lines;
    }

    private static Optional<String> excludeProblem(List<String> lines) {
        return dependabotExcludeProblem(lines, EXISTING_WORKFLOWS::contains);
    }

    @Test
    @DisplayName("every action in tests.yaml is pinned to a full commit SHA with a version comment")
    void everyActionIsPinnedToAFullCommitShaWithAVersionComment() throws IOException {
        assertNoProblem(pinnedUsesProblem(nonCommentLines(WORKFLOW)));
    }

    @Test
    @DisplayName("every job in tests.yaml runs on the pinned ubuntu-24.04 runner")
    void everyJobRunsOnUbuntu2404() throws IOException {
        assertNoProblem(runsOnProblem(nonCommentLines(WORKFLOW)));
    }

    @Test
    @DisplayName("the workflow token is read-only: one top-level permissions block, contents read")
    void workflowTokenIsReadOnly() throws IOException {
        List<String> lines = nonCommentLines(WORKFLOW);

        assertNoProblem(writeScopeProblem(lines));
        assertNoProblem(permissionsBlockProblem(lines));
    }

    @Test
    @DisplayName("checkout does not persist the token in .git/config")
    void checkoutDoesNotPersistCredentials() throws IOException {
        assertNoProblem(checkoutCredentialsProblem(nonCommentLines(WORKFLOW)));
    }

    @Test
    @DisplayName("dependabot.yml updates the github-actions ecosystem next to gradle")
    void dependabotUpdatesTheGithubActionsEcosystem() throws IOException {
        assertNoProblem(dependabotEcosystemProblem(nonCommentLines(DEPENDABOT)));
    }

    @Test
    @DisplayName(
            "the Report test fork count step calls ./gradlew reportTestForks and tests.yaml holds no"
                    + " nproc formula (CI-18)")
    void forkCountStepCallsReportTestForks() throws IOException {
        assertNoProblem(forkCountStepProblem(nonCommentLines(WORKFLOW)));
    }

    @Test
    @DisplayName(
            "build.gradle.kts registers reportTestForks reading maxParallelForks from the test task"
                    + " (CI-18)")
    void reportTestForksReadsMaxParallelForksFromTheTestTask() throws IOException {
        assertNoProblem(
                forkTaskProblem(ktsCode(Files.readString(BUILD_SCRIPT, StandardCharsets.UTF_8))));
    }

    @Test
    @DisplayName(
            "dependabot.yml excludes the four upstream workflows from github-actions updates and lists"
                    + " only existing workflow files (17-REVIEW WR-01)")
    void dependabotExcludesTheUpstreamWorkflowsAndOnlyExistingFiles() throws IOException {
        assertNoProblem(
                dependabotExcludeProblem(
                        nonCommentLines(DEPENDABOT), path -> Files.isRegularFile(Path.of(path))));
    }

    @Test
    @DisplayName("fixture: pinned-uses rule accepts full-SHA pins with a version comment")
    void pinnedUsesRuleAcceptsFullShaPins() {
        assertAll(
                accepts(TestsWorkflowPinGuardUnitTest::pinnedUsesProblem, FIVE_PINNED),
                accepts(
                        TestsWorkflowPinGuardUnitTest::pinnedUsesProblem,
                        plus(FIVE_PINNED, "        run: ./gradlew test", "", "    steps:")));
    }

    @Test
    @DisplayName(
            "fixture: pinned-uses rule rejects tag pins, short SHAs, a missing comment and too few"
                    + " actions")
    void pinnedUsesRuleRejectsUnpinnedOrTooFewActions() {
        Function<List<String>, Optional<String>> rule = TestsWorkflowPinGuardUnitTest::pinnedUsesProblem;
        assertAll(
                rejects(rule, plus(FIVE_PINNED, "      - uses: actions/cache@v4")),
                rejects(rule, plus(FIVE_PINNED, "      - uses: actions/cache@abcdef0 # v4.0.0")),
                rejects(rule, plus(FIVE_PINNED, "      - uses: actions/cache@" + SHA)),
                rejects(rule, FIVE_PINNED.subList(0, 4)),
                rejects(rule));
    }

    @Test
    @DisplayName("fixture: runs-on rule accepts ubuntu-24.04 with a trailing comment (CI-19)")
    void runsOnRuleAcceptsTrailingComment() {
        Function<List<String>, Optional<String>> rule = TestsWorkflowPinGuardUnitTest::runsOnProblem;
        assertAll(
                accepts(rule, "    runs-on: ubuntu-24.04"),
                accepts(rule, "    runs-on: ubuntu-24.04 # pinned"),
                accepts(rule, "    runs-on: ubuntu-24.04    # padded   "));
    }

    @Test
    @DisplayName(
            "fixture: runs-on rule rejects floating, other, commented-out and empty runners (D-10)")
    void runsOnRuleRejectsOtherRunners() {
        Function<List<String>, Optional<String>> rule = TestsWorkflowPinGuardUnitTest::runsOnProblem;
        assertAll(
                rejects(rule, "    runs-on: ubuntu-latest # x"),
                rejects(rule, "    runs-on: ubuntu-latest"),
                rejects(rule, "    runs-on: ubuntu-22.04 # x"),
                rejects(rule, "    runs-on: ubuntu-24.04#x"),
                rejects(rule, "    runs-on: # ubuntu-24.04"),
                rejects(rule, "    runs-on:"),
                rejects(rule, "    runs-on: ${{ matrix.os }}"),
                rejects(rule));
    }

    @Test
    @DisplayName("fixture: write-scope rule ignores step name and run text (CI-19)")
    void writeScopeRuleIgnoresStepText() {
        Function<List<String>, Optional<String>> rule = TestsWorkflowPinGuardUnitTest::writeScopeProblem;
        assertAll(
                accepts(rule, "      - name: write summary"),
                accepts(rule, "        name: write summary"),
                accepts(rule, "      - name: Write summary: write"),
                accepts(rule, "        run: echo \"mode: write\""),
                accepts(rule, "          echo \"contents: write\" >> \"$GITHUB_STEP_SUMMARY\""),
                accepts(rule, "        run: write"),
                accepts(rule, "      - run: echo write-all"));
    }

    @Test
    @DisplayName(
            "fixture: write-scope rule rejects contents write, write-all, quoted write and a"
                    + " job-level block (D-10)")
    void writeScopeRuleRejectsWriteGrants() {
        Function<List<String>, Optional<String>> rule = TestsWorkflowPinGuardUnitTest::writeScopeProblem;
        assertAll(
                rejects(rule, "  contents: write"),
                rejects(rule, "permissions: write-all"),
                rejects(rule, "      contents: write"),
                rejects(rule, "    permissions: write-all"),
                rejects(rule, "  contents: \"write\""),
                rejects(rule, "  contents: 'write'"),
                rejects(rule, "  packages: write # why"),
                rejects(rule, "  id-token: write"),
                rejects(rule, "permissions: { contents: write }"),
                rejects(rule, "permissions: {contents: write, issues: read}"),
                rejects(rule, JOB_LEVEL_WRITE_BLOCK));
    }

    @Test
    @DisplayName(
            "fixture: permissions-block rule accepts one top-level block holding contents read")
    void permissionsBlockRuleAcceptsTopLevelContentsRead() {
        assertAll(
                accepts(
                        TestsWorkflowPinGuardUnitTest::permissionsBlockProblem,
                        "permissions:",
                        "  contents: read",
                        "",
                        "on:"));
    }

    @Test
    @DisplayName(
            "fixture: permissions-block rule rejects a job-level block, a missing block and a block"
                    + " not holding contents read")
    void permissionsBlockRuleRejectsJobLevelOrMissingBlock() {
        Function<List<String>, Optional<String>> rule =
                TestsWorkflowPinGuardUnitTest::permissionsBlockProblem;
        assertAll(
                rejects(rule, JOB_LEVEL_WRITE_BLOCK),
                rejects(rule, "on:", "jobs:", "  build:", "    runs-on: ubuntu-24.04"),
                rejects(rule, "permissions:", "  contents: write"));
    }

    @Test
    @DisplayName("fixture: checkout rule accepts persist-credentials false")
    void checkoutRuleAcceptsPersistCredentialsFalse() {
        assertAll(
                accepts(
                        TestsWorkflowPinGuardUnitTest::checkoutCredentialsProblem,
                        pinnedUses("actions/checkout", "v7.0.1"),
                        "        with:",
                        "          persist-credentials: false",
                        "",
                        "      - name: Set up JDK 17"));
    }

    @Test
    @DisplayName("fixture: checkout rule rejects a checkout without persist-credentials false")
    void checkoutRuleRejectsPersistedToken() {
        Function<List<String>, Optional<String>> rule =
                TestsWorkflowPinGuardUnitTest::checkoutCredentialsProblem;
        assertAll(
                rejects(rule, pinnedUses("actions/checkout", "v7.0.1"), "", "      - name: Set up JDK 17"),
                rejects(
                        rule,
                        pinnedUses("actions/checkout", "v7.0.1"),
                        "      - name: Set up JDK 17",
                        "        with:",
                        "          persist-credentials: false"),
                rejects(rule));
    }

    @Test
    @DisplayName("fixture: dependabot ecosystem rule accepts gradle next to github-actions")
    void dependabotEcosystemRuleAcceptsBothEcosystems() {
        assertAll(
                accepts(
                        TestsWorkflowPinGuardUnitTest::dependabotEcosystemProblem,
                        dependabot(List.of(), UPSTREAM)));
    }

    @Test
    @DisplayName("fixture: dependabot ecosystem rule rejects a missing ecosystem")
    void dependabotEcosystemRuleRejectsAMissingEcosystem() {
        Function<List<String>, Optional<String>> rule =
                TestsWorkflowPinGuardUnitTest::dependabotEcosystemProblem;
        assertAll(
                rejects(
                        rule,
                        "version: 2",
                        "updates:",
                        "  - package-ecosystem: \"gradle\"",
                        "    directory: \"/\""),
                rejects(
                        rule,
                        "version: 2",
                        "updates:",
                        "  - package-ecosystem: \"github-actions\"",
                        "    directory: \"/\""),
                rejects(rule));
    }

    @Test
    @DisplayName(
            "fixture: fork-count step rule accepts the ./gradlew reportTestForks call (CI-18)")
    void forkCountStepRuleAcceptsTheGradleCall() {
        Function<List<String>, Optional<String>> rule =
                TestsWorkflowPinGuardUnitTest::forkCountStepProblem;
        assertAll(
                accepts(
                        rule,
                        plus(
                                forkStep(STEP_BODY),
                                "",
                                NEXT_STEP,
                                "        run: ./gradlew compileIntegrationTestJava")),
                accepts(rule, forkStep(STEP_BODY)));
    }

    @Test
    @DisplayName(
            "fixture: fork-count step rule rejects a shell nproc formula, an echoed or misplaced call"
                    + " and a renamed step (CI-18)")
    void forkCountStepRuleRejectsShellFormulas() {
        Function<List<String>, Optional<String>> rule =
                TestsWorkflowPinGuardUnitTest::forkCountStepProblem;
        assertAll(
                rejects(
                        rule,
                        forkStep(
                                List.of(
                                        "          forks=$(( $(nproc) / 2 ))",
                                        "          echo \"::error::reportTestForks did not print an integer\""))),
                rejects(
                        rule,
                        plus(
                                forkStep(List.of("          echo hello")),
                                "",
                                NEXT_STEP,
                                "        run: ./gradlew -q reportTestForks")),
                rejects(rule, forkStep(List.of("          echo \"./gradlew reportTestForks\""))),
                rejects(rule, forkStep(plus(STEP_BODY, "          forks=$(( $(nproc) / 2 ))"))),
                rejects(
                        rule,
                        "      - name: Count forks",
                        "        run: |",
                        FORK_CALL),
                rejects(rule),
                rejects(rule, forkStep(List.of(FORK_CALL, "          msg=\"$(( $(nproc) / 2 ))\""))));
    }

    @Test
    @DisplayName("fixture: reportTestForks task rule accepts the registered task (CI-18)")
    void forkTaskRuleAcceptsTheRegisteredTask() {
        assertAll(
                ktsAccepts(REGISTER_BLOCK),
                ktsAccepts(
                        "// CI reads the fork count from reportTestForks\n"
                                + "/* a block comment\n   spanning two lines */\n"
                                + REGISTER_BLOCK));
    }

    @Test
    @DisplayName(
            "fixture: reportTestForks task rule rejects a deleted, renamed, commented-out or constant"
                    + " task (CI-18)")
    void forkTaskRuleRejectsMissingOrConstantTasks() {
        assertAll(
                ktsRejects("// reportTestForks"),
                ktsRejects(REGISTER_BLOCK.replace("reportTestForks", "reportForks")),
                ktsRejects("tasks.register(\"reportTestForks\") {\n    println(6)\n}"),
                ktsRejects(REGISTER_BLOCK.replace("<Test>(\"test\")", "<Test>(\"testUnit\")")),
                ktsRejects("/*\n" + REGISTER_BLOCK + "\n*/"),
                ktsRejects(
                        "// tasks.register(\"reportTestForks\") {\n"
                                + "tasks.register(\"other\") {\n"
                                + "    val forks = tasks.named<Test>(\"test\").map { it.maxParallelForks }\n"
                                + "}"));
    }

    @Test
    @DisplayName(
            "fixture: exclude-paths rule accepts the four existing upstream workflows (17-REVIEW"
                    + " WR-01)")
    void dependabotExcludeRuleAcceptsUpstreamWorkflows() {
        assertAll(
                accepts(
                        TestsWorkflowPinGuardUnitTest::excludeProblem, dependabot(List.of(), UPSTREAM)));
    }

    @Test
    @DisplayName(
            "fixture: exclude-paths rule rejects a missing, misspelt, stale or misplaced path"
                    + " (17-REVIEW WR-01)")
    void dependabotExcludeRuleRejectsMissingOrStalePaths() {
        Function<List<String>, Optional<String>> rule = TestsWorkflowPinGuardUnitTest::excludeProblem;
        assertAll(
                rejects(rule, dependabot(List.of(), UPSTREAM.subList(0, 3))),
                rejects(
                        rule,
                        dependabot(
                                List.of(),
                                List.of(
                                        UPSTREAM.get(0),
                                        ".github/workflows/integrationTest.yaml",
                                        UPSTREAM.get(2),
                                        UPSTREAM.get(3)))),
                rejects(
                        rule,
                        dependabot(
                                List.of(),
                                plus(UPSTREAM, ".github/workflows/gone.yml"))),
                rejects(rule, dependabot(UPSTREAM, List.of())),
                rejects(rule));
    }

    private static final String SHADOW_BANNER = "/*\n * ShadowJar\n */";

    private static String withBanner(String... head) {
        return String.join("\n", head) + "\n" + REGISTER_BLOCK + "\n" + SHADOW_BANNER;
    }

    @Test
    @DisplayName(
            "DEBT-13: ktsCode keeps string literals, raw strings and line structure, and drops line,"
                    + " trailing and nested block comments")
    void ktsCodeKeepsStringsAndLineStructure() {
        String glob = "val excludes = \"org/apache/kafka/connect/**\"";
        String rawBoth = "val note = \"\"\"// line /* block\"\"\"";
        String escaped = "val quoted = \"a\\\"/*\"";
        String threeLineBlock = "a\n/* x\ny\nz */\nval raw = \"\"\"\nq\n\"\"\"\nb";
        assertAll(
                () -> assertEquals(List.of(glob), ktsCode(glob)),
                () -> assertEquals(List.of(rawBoth), ktsCode(rawBoth)),
                () -> assertEquals(List.of(escaped, "val after = 1"), ktsCode(escaped + "\nval after = 1")),
                () -> assertEquals(List.of("val x = 1 "), ktsCode("val x = 1 // see /* here")),
                () -> assertEquals(List.of("a  b"), ktsCode("a /* one /* two */ still */ b")),
                () -> assertEquals(threeLineBlock.lines().count(), ktsCode(threeLineBlock).size()),
                forkTaskEmptyInput(),
                () ->
                        assertEquals(
                                List.of("val c = '\"'; val d = 2 "),
                                ktsCode("val c = '\"'; val d = 2 // gone")),
                () ->
                        assertEquals(
                                List.of("val c = '\\''; val d = 2 "),
                                ktsCode("val c = '\\''; val d = 2 /* gone */")));
    }

    @Test
    @DisplayName(
            "fixture: reportTestForks task rule accepts the task after a slash-star inside a string, a raw"
                    + " string or a line comment (DEBT-13, 21-REVIEW WR-02)")
    void forkTaskRuleAcceptsTaskAfterSlashStarInStringOrComment() {
        assertAll(
                ktsAccepts(withBanner("val shadowExcludes = \"org/apache/kafka/connect/**\"")),
                ktsAccepts(withBanner("// the archive holds lib/* entries only")),
                ktsAccepts(withBanner("val skip = 1 // see /* here")),
                ktsAccepts(withBanner("val note = \"\"\"// line /* block\"\"\"")));
    }

    private static Executable forkTaskEmptyInput() {
        return () -> {
            assertEquals(List.of(), ktsCode(""));
            assertEquals(
                    Optional.of(
                            "tasks.register(\"reportTestForks\") not found in " + BUILD_SCRIPT
                                    + " outside comments"),
                    forkTaskProblem(ktsCode("")));
        };
    }

    @Test
    @DisplayName(
            "fixture: reportTestForks task rule rejects a read inside a trailing comment and a task"
                    + " inside a nested block comment (DEBT-13, 21-REVIEW WR-02, WR-03)")
    void forkTaskRuleRejectsTrailingCommentReadAndNestedCommentedTask() {
        assertAll(
                ktsRejects(
                        String.join(
                                "\n",
                                "tasks.register(\"reportTestForks\") {",
                                "    doLast { println(6) } // tasks.named<Test>(\"test\").maxParallelForks",
                                "}")),
                ktsRejects("/*\n/* inner */\n" + REGISTER_BLOCK + "\n*/"));
    }
}
