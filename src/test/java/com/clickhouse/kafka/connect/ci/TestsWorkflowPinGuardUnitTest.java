package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
 * Docker-free pin for 14-SECURITY T-14-16, CI-11 to CI-13, CI-15, CI-18, CI-19, CI-20, DEBT-13 and
 * DEBT-14: the {@code tests.yaml} hardening, its Dependabot entry, the fork-count step and task, and
 * the Confluent archive step and task. A regression fails {@code testUnit} before any CI run.
 * Deliberately extends nothing - extending {@code ClickHouseBase} would inherit its {@code
 * clickhouse} tag and drop this class out of {@code testUnit}.
 *
 * <p>The workflow files are read as UTF-8 explicitly, because the test JVM runs with {@code
 * file.encoding=windows-1252}. No YAML library is involved: every rule is a line-level check on
 * the non-comment lines, and each assertion message names the offending line.
 */
class TestsWorkflowPinGuardUnitTest {
    private static final Path WORKFLOW = Path.of(".github", "workflows", "tests.yaml");
    static final Path DEPENDABOT = Path.of(".github", "dependabot.yml");
    private static final Path BUILD_SCRIPT = Path.of("build.gradle.kts");

    static final Pattern USES_LINE = Pattern.compile("^\\s*(?:-\\s+)?uses:\\s.*$");
    static final Pattern PINNED_USES =
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
    private static final Pattern REGISTER_FORKS =
            Pattern.compile("^tasks\\.register\\(\"reportTestForks\"\\)\\s*\\{\\s*$");
    private static final Pattern REGISTER_VERIFY_ARCHIVE =
            Pattern.compile("^tasks\\.register<JavaExec>\\(\"verifyConfluentArchive\"\\)\\s*\\{\\s*$");
    private static final Pattern REGISTER_CREATE_ARCHIVE =
            Pattern.compile("^tasks\\.register<Zip>\\(\"createConfluentArchive\"\\)\\s*\\{\\s*$");
    static final Pattern CONTINUE_ON_ERROR =
            Pattern.compile("^\\s*continue-on-error:\\s*+(?!false\\b).*$");
    // YAML and shell both need whitespace before '#', so "ubuntu-24.04#x" keeps its tail.
    static final Pattern TRAILING_COMMENT = Pattern.compile("\\s#.*$");
    private static final Pattern FORKS_PROVIDER_DECLARATION = Pattern.compile("^\\s*val\\s+(\\w+)\\s*=.*$");
    private static final Pattern EXCLUDE_ITEM =
            Pattern.compile("^\\s*-\\s*[\"']?([^\"'#]+?)[\"']?\\s*(?:#.*)?$");

    private static final String EXPECTED_RUNNER = "ubuntu-24.04";
    private static final String FLOATING_RUNNER = "ubuntu-latest";
    private static final int MIN_PINNED_ACTIONS = 5;
    private static final String TOP_LEVEL_PERMISSIONS = "permissions:";
    private static final String READ_ONLY_CONTENTS = "  contents: read";
    private static final String CHECKOUT = "actions/checkout@";
    private static final String NO_PERSISTED_CREDENTIALS = "persist-credentials: false";
    private static final Pattern GITHUB_ACTIONS_ENTRY = ecosystemEntry("github-actions");
    private static final Pattern GRADLE_ENTRY_PATTERN = ecosystemEntry("gradle");
    private static final String EXCLUDE_PATHS_KEY = "exclude-paths:";
    static final String WORKFLOWS_DIR = ".github/workflows/";
    private static final List<String> UPSTREAM_WORKFLOWS =
            List.of(WORKFLOWS_DIR + "integrationTests.yaml");
    private static final String FORK_STEP_NAME = "Report test fork count";
    private static final String FORK_TASK = "reportTestForks";
    private static final String ARCHIVE_STEP = "Build and verify Confluent archive";
    private static final String ARCHIVE_TASK = "createConfluentArchive";
    private static final String VERIFY_TASK = "verifyConfluentArchive";
    // Taken from the class itself, so renaming or moving it moves the pin with it (CI-20, D-09).
    private static final String ARCHIVE_CHECK_MAIN = ConfluentArchiveCheck.class.getName();
    private static final String TEST_TASK_READ = "tasks.named<Test>(\"test\")";
    private static final String MAX_PARALLEL_FORKS = "maxParallelForks";
    private static final String RAW_QUOTE = "\"\"\"";
    private static final String MSG_LINE_PREFIX = "msg=";
    private static final String NPROC_CONTEXT = "nproc=$(nproc)";

    // The same quote on both sides or none, and an optional whitespace-preceded trailing comment.
    private static Pattern ecosystemEntry(String ecosystem) {
        return Pattern.compile(
                "^\\s*-\\s+package-ecosystem:\\s*([\"']?)" + Pattern.quote(ecosystem) + "\\1\\s*(?:\\s#.*)?$");
    }

    private static boolean isEntry(String line, Pattern entry) {
        return entry.matcher(line).matches();
    }

    static List<String> nonCommentLines(Path file) throws IOException {
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
            if (TRAILING_COMMENT.matcher(line).replaceFirst("").contains(FLOATING_RUNNER)) {
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
        if (lines.stream().noneMatch(line -> isEntry(line, GITHUB_ACTIONS_ENTRY))) {
            return Optional.of("Missing a package-ecosystem: github-actions entry in " + DEPENDABOT);
        }
        if (lines.stream().noneMatch(line -> isEntry(line, GRADLE_ENTRY_PATTERN))) {
            return Optional.of(
                    "The existing package-ecosystem: gradle entry must stay in " + DEPENDABOT);
        }
        return Optional.empty();
    }

    private static Pattern stepHeader(String stepName) {
        return Pattern.compile("^(\\s*)-\\s+name:\\s*" + Pattern.quote(stepName) + "\\s*(?:\\s#.*)?$");
    }

    private static Pattern gradlewCall(String task) {
        // Command position only: after an optional "run:" key and an optional "name=$(" prefix, so a
        // no-op (": ./gradlew"), a guard ("false && ./gradlew"), an echo or a comment is not the call.
        return Pattern.compile(
                "^\\s*(?:run:\\s*)?(?:\\w+=\"?\\$\\()?\\./gradlew\\s.*(?<![\\w-])"
                        + Pattern.quote(task)
                        + "(?![\\w-]).*$");
    }

    /** The lines after the named step's header up to the first non-blank line at or below its indent. */
    static Optional<List<String>> stepBlock(List<String> lines, String stepName) {
        Pattern header = stepHeader(stepName);
        for (int i = 0; i < lines.size(); i++) {
            Matcher step = header.matcher(lines.get(i));
            if (!step.matches()) {
                continue;
            }
            int stepIndent = step.group(1).length();
            List<String> block = new ArrayList<>();
            for (int j = i + 1; j < lines.size(); j++) {
                String line = lines.get(j);
                if (!line.isBlank() && indent(line) <= stepIndent) {
                    break;
                }
                block.add(line);
            }
            return Optional.of(block);
        }
        return Optional.empty();
    }

    static Optional<String> gradlewStepProblem(List<String> lines, String stepName, String task) {
        Optional<List<String>> block = stepBlock(lines, stepName);
        if (block.isEmpty()) {
            return Optional.of("No step named '" + stepName + "' in " + WORKFLOW);
        }
        Pattern call = gradlewCall(task);
        boolean called =
                block.get().stream()
                        .map(line -> TRAILING_COMMENT.matcher(line).replaceFirst(""))
                        .anyMatch(code -> call.matcher(code).matches());
        if (!called) {
            return Optional.of(
                    "Step '" + stepName + "' in " + WORKFLOW + " must call './gradlew ... " + task
                            + "' in its run block");
        }
        return Optional.empty();
    }

    static Optional<String> forkCountStepProblem(List<String> lines) {
        Optional<String> problem = gradlewStepProblem(lines, FORK_STEP_NAME, FORK_TASK);
        return problem.isPresent() ? problem : nprocProblem(lines);
    }

    static Optional<String> archiveStepProblem(List<String> lines) {
        Optional<String> problem = gradlewStepProblem(lines, ARCHIVE_STEP, ARCHIVE_TASK);
        if (problem.isPresent()) {
            return problem;
        }
        boolean continuesOnError =
                stepBlock(lines, ARCHIVE_STEP).orElse(List.of()).stream()
                        .anyMatch(line -> CONTINUE_ON_ERROR.matcher(line).matches());
        if (continuesOnError) {
            return Optional.of(
                    "Step '" + ARCHIVE_STEP + "' in " + WORKFLOW + " must not set continue-on-error");
        }
        return Optional.empty();
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
            String provider = null;
            int end = i + 1;
            while (end < lines.size() && !lines.get(end).equals("}")) {
                String line = lines.get(end++);
                readsTestTask |= line.contains(TEST_TASK_READ);
                readsForks |= line.contains(MAX_PARALLEL_FORKS);
                Matcher declaration = FORKS_PROVIDER_DECLARATION.matcher(line);
                if (declaration.matches()
                        && line.contains(TEST_TASK_READ)
                        && line.contains(MAX_PARALLEL_FORKS)) {
                    provider = declaration.group(1);
                }
            }
            if (!readsTestTask || !readsForks) {
                return Optional.of(
                        "reportTestForks in " + BUILD_SCRIPT + " must read " + MAX_PARALLEL_FORKS
                                + " from " + TEST_TASK_READ);
            }
            Pattern printsProvider =
                    Pattern.compile(
                            "\\bprint(?:ln)?\\(\\s*" + Pattern.quote(String.valueOf(provider)) + "\\.get\\(\\)");
            if (provider == null
                    || lines.subList(i + 1, end).stream()
                            .noneMatch(line -> printsProvider.matcher(line).find())) {
                return Optional.of(
                        "reportTestForks in " + BUILD_SCRIPT + " must print the " + MAX_PARALLEL_FORKS
                                + " provider it reads from " + TEST_TASK_READ);
            }
            return Optional.empty();
        }
        return Optional.of(
                "tasks.register(\"reportTestForks\") not found in " + BUILD_SCRIPT + " outside comments");
    }

    /** The lines after the first line matching {@code register}, up to the first line equal to "}". */
    private static Optional<List<String>> taskBlock(List<String> lines, Pattern register) {
        for (int i = 0; i < lines.size(); i++) {
            if (!register.matcher(lines.get(i)).matches()) {
                continue;
            }
            int end = i + 1;
            while (end < lines.size() && !lines.get(end).equals("}")) {
                end++;
            }
            return Optional.of(lines.subList(i + 1, end));
        }
        return Optional.empty();
    }

    static Optional<String> archiveTaskProblem(List<String> lines) {
        Optional<List<String>> verify = taskBlock(lines, REGISTER_VERIFY_ARCHIVE);
        if (verify.isEmpty()) {
            return Optional.of(
                    "tasks.register<JavaExec>(\"" + VERIFY_TASK + "\") not found in " + BUILD_SCRIPT
                            + " outside comments");
        }
        String mainClassLine = "mainClass.set(\"" + ARCHIVE_CHECK_MAIN + "\")";
        if (verify.get().stream().noneMatch(line -> line.strip().equals(mainClassLine))) {
            return Optional.of(
                    VERIFY_TASK + " in " + BUILD_SCRIPT + " must run " + ARCHIVE_CHECK_MAIN
                            + ", the class ConfluentArchiveCheckUnitTest exercises");
        }
        Optional<List<String>> create = taskBlock(lines, REGISTER_CREATE_ARCHIVE);
        if (create.isEmpty()) {
            return Optional.of(
                    "tasks.register<Zip>(\"" + ARCHIVE_TASK + "\") not found in " + BUILD_SCRIPT
                            + " outside comments");
        }
        String finalizer = "finalizedBy(\"" + VERIFY_TASK + "\")";
        if (create.get().stream().noneMatch(line -> line.strip().equals(finalizer))) {
            return Optional.of(
                    ARCHIVE_TASK + " in " + BUILD_SCRIPT + " must be finalizedBy(\"" + VERIFY_TASK + "\")");
        }
        return Optional.empty();
    }

    static Optional<String> dependabotExcludeProblem(List<String> lines, Predicate<String> exists) {
        if (lines.stream().noneMatch(line -> isEntry(line, GITHUB_ACTIONS_ENTRY))) {
            return Optional.of("No package-ecosystem: github-actions entry in " + DEPENDABOT);
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
            if (isEntry(lines.get(i), GITHUB_ACTIONS_ENTRY)) {
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
            // A sequence may start at its key's own indent; anything else at or below it ends the list.
            if (indent(line) < keyIndent
                    || (indent(line) == keyIndent && !LIST_ITEM.matcher(line).matches())) {
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

    static final String SHA = "3d3c42e5aac5ba805825da76410c181273ba90b1";

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

    private static final List<String> UPSTREAM = UPSTREAM_WORKFLOWS;
    private static final Set<String> EXISTING_WORKFLOWS =
            Set.copyOf(
                    plus(
                            UPSTREAM,
                            WORKFLOWS_DIR + "increment-version.yml",
                            WORKFLOWS_DIR + "pre-release.yaml",
                            WORKFLOWS_DIR + "release.yaml",
                            WORKFLOWS_DIR + "tests.yaml"));

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
            "dependabot.yml excludes upstream's integrationTests.yaml from github-actions updates and"
                    + " lists only existing workflow files (17-REVIEW WR-01, D-10)")
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
            "fixture: exclude-paths rule accepts the upstream-identical integrationTests.yaml"
                    + " (17-REVIEW WR-01, D-10)")
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
                rejects(
                        rule,
                        dependabot(
                                List.of(),
                                List.of(
                                        WORKFLOWS_DIR + "increment-version.yml",
                                        WORKFLOWS_DIR + "pre-release.yaml",
                                        WORKFLOWS_DIR + "release.yaml"))),
                rejects(
                        rule,
                        dependabot(List.of(), List.of(".github/workflows/integrationTest.yaml"))),
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

    private static final String ARCHIVE_STEP_HEADER = "      - name: " + ARCHIVE_STEP;
    private static final String ARCHIVE_CALL = "        run: ./gradlew createConfluentArchive";
    private static final String FINALIZED_BY = "    finalizedBy(\"verifyConfluentArchive\")";
    private static final String CHECK_MAIN_LINE = "    mainClass.set(\"" + ARCHIVE_CHECK_MAIN + "\")";
    private static final String CREATE_ARCHIVE_OPEN = "tasks.register<Zip>(\"createConfluentArchive\") {";
    private static final String VERIFY_ARCHIVE_OPEN = "tasks.register<JavaExec>(\"verifyConfluentArchive\") {";
    private static final String CREATE_ARCHIVE_BLOCK =
            String.join(
                    "\n",
                    CREATE_ARCHIVE_OPEN,
                    "    group = \"Confluent\"",
                    "    dependsOn(\"prepareConfluentArchive\")",
                    FINALIZED_BY,
                    "}");
    private static final String VERIFY_ARCHIVE_BLOCK =
            String.join(
                    "\n",
                    VERIFY_ARCHIVE_OPEN,
                    "    group = \"verification\"",
                    CHECK_MAIN_LINE,
                    "    argumentProviders.add(CommandLineArgumentProvider { listOf(\"a\") })",
                    "}");
    private static final String ARCHIVE_TASKS = CREATE_ARCHIVE_BLOCK + "\n\n" + VERIFY_ARCHIVE_BLOCK;

    private static List<String> archiveStep(String... body) {
        List<String> lines = new ArrayList<>(List.of("    steps:", ARCHIVE_STEP_HEADER));
        lines.addAll(Arrays.asList(body));
        return lines;
    }

    private static Executable archiveTasksAccept(String text) {
        return accepts(text.replace("\n", " / "), archiveTaskProblem(ktsCode(text)));
    }

    private static Executable archiveTasksReject(String text) {
        return rejects(text.replace("\n", " / "), archiveTaskProblem(ktsCode(text)));
    }

    @Test
    @DisplayName(
            "tests.yaml builds and verifies the Confluent archive in a named step that calls ./gradlew"
                    + " createConfluentArchive (CI-20)")
    void archiveStepBuildsAndVerifiesTheConfluentArchive() throws IOException {
        assertNoProblem(archiveStepProblem(nonCommentLines(WORKFLOW)));
    }

    @Test
    @DisplayName(
            "build.gradle.kts registers verifyConfluentArchive running ConfluentArchiveCheck and"
                    + " finalizes createConfluentArchive with it (CI-20)")
    void buildScriptFinalizesCreateConfluentArchiveWithItsVerification() throws IOException {
        assertNoProblem(
                archiveTaskProblem(ktsCode(Files.readString(BUILD_SCRIPT, StandardCharsets.UTF_8))));
    }

    @Test
    @DisplayName("fixture: Confluent archive rules accept the real step and task shapes (CI-20)")
    void archiveRulesAcceptTheRealShapes() {
        Function<List<String>, Optional<String>> rule =
                TestsWorkflowPinGuardUnitTest::archiveStepProblem;
        assertAll(
                accepts(rule, archiveStep(ARCHIVE_CALL)),
                accepts(rule, archiveStep("        run: |", "          ./gradlew --console=plain createConfluentArchive")),
                accepts(
                        rule,
                        plus(
                                archiveStep(ARCHIVE_CALL),
                                "",
                                "      - name: Run tests",
                                "        run: ./gradlew test")),
                accepts(rule, archiveStep(ARCHIVE_CALL, "        continue-on-error: false")),
                archiveTasksAccept(ARCHIVE_TASKS),
                archiveTasksAccept(VERIFY_ARCHIVE_BLOCK + "\n\n" + CREATE_ARCHIVE_BLOCK),
                archiveTasksAccept(
                        "// finalizedBy(\"other\")\n" + ARCHIVE_TASKS + "\n/* tasks.register<Zip>(\"x\") {\n*/"));
    }

    @Test
    @DisplayName(
            "fixture: Confluent archive rules reject a missing or renamed step, a renamed task, an echoed"
                    + " call, continue-on-error, another main class and a dropped or misplaced finalizedBy"
                    + " (CI-20)")
    void archiveRulesRejectMissingRenamedOrUnfinalizedShapes() {
        Function<List<String>, Optional<String>> rule =
                TestsWorkflowPinGuardUnitTest::archiveStepProblem;
        assertAll(
                rejects(rule, "    steps:", "      - name: Run tests", "        run: ./gradlew test"),
                rejects(
                        rule,
                        "    steps:",
                        "      - name: Build Confluent archive",
                        "        run: ./gradlew createConfluentArchive"),
                rejects(rule, archiveStep("        run: ./gradlew confluentJar")),
                rejects(rule, archiveStep("        run: |", "          echo \"./gradlew createConfluentArchive\"")),
                rejects(rule, archiveStep(ARCHIVE_CALL, "        continue-on-error: true")),
                archiveTasksReject(CREATE_ARCHIVE_BLOCK),
                archiveTasksReject(
                        ARCHIVE_TASKS.replace(
                                VERIFY_ARCHIVE_OPEN,
                                "tasks.register<JavaExec>(\"verifyConfluentArchives\") {")),
                archiveTasksReject(
                        ARCHIVE_TASKS.replace(
                                ARCHIVE_CHECK_MAIN, "com.clickhouse.kafka.connect.ci.OtherCheck")),
                archiveTasksReject(ARCHIVE_TASKS.replace(FINALIZED_BY + "\n", "")),
                archiveTasksReject(
                        ARCHIVE_TASKS.replace(FINALIZED_BY, "    // finalizedBy(\"verifyConfluentArchive\")")),
                archiveTasksReject(
                        "tasks.register<Copy>(\"prepareConfluentArchive\") {\n"
                                + FINALIZED_BY
                                + "\n}\n\n"
                                + CREATE_ARCHIVE_BLOCK.replace(FINALIZED_BY + "\n", "")
                                + "\n\n"
                                + VERIFY_ARCHIVE_BLOCK),
                archiveTasksReject(VERIFY_ARCHIVE_BLOCK));
    }

    private static final String ACTIONS_ENTRY = "  - package-ecosystem: \"github-actions\"";
    private static final String GRADLE_ENTRY = "  - package-ecosystem: \"gradle\"";

    private static List<String> withEntry(List<String> lines, String original, String replacement) {
        return lines.stream()
                .map(line -> line.equals(original) ? replacement : line)
                .collect(Collectors.toList());
    }

    private static List<String> actionsEntryWithItems(int keyIndent, int itemIndent, List<String> paths) {
        List<String> lines =
                new ArrayList<>(
                        List.of(
                                "version: 2",
                                "updates:",
                                GRADLE_ENTRY,
                                "    directory: \"/\"",
                                ACTIONS_ENTRY,
                                "    directory: \"/\"",
                                " ".repeat(keyIndent) + "exclude-paths:"));
        paths.forEach(path -> lines.add(" ".repeat(itemIndent) + "- \"" + path + "\""));
        lines.addAll(List.of("    schedule:", "      interval: \"monthly\""));
        return lines;
    }

    @Test
    @DisplayName(
            "fixture: runs-on rule accepts a pinned runner whose trailing comment names the floating"
                    + " label (DEBT-14, 21-REVIEW WR-01)")
    void runsOnRuleAcceptsACommentThatNamesTheFloatingLabel() {
        Function<List<String>, Optional<String>> rule = TestsWorkflowPinGuardUnitTest::runsOnProblem;
        assertAll(
                accepts(rule, "    runs-on: ubuntu-24.04 # was ubuntu-latest"),
                rejects(rule, "    runs-on: ubuntu-latest # x"),
                rejects(rule, "    runs-on: ubuntu-24.04#x"),
                rejects(rule, "    runs-on: ubuntu-24.04", "      - run: echo ubuntu-latest"));
    }

    @Test
    @DisplayName(
            "fixture: gradle-step rules reject a no-op, guarded or commented-out gradle call (DEBT-14,"
                    + " 21-REVIEW WR-03)")
    void gradleStepRulesRejectNoOpGuardedOrCommentedCalls() {
        Function<List<String>, Optional<String>> fork =
                TestsWorkflowPinGuardUnitTest::forkCountStepProblem;
        Function<List<String>, Optional<String>> archive =
                TestsWorkflowPinGuardUnitTest::archiveStepProblem;
        assertAll(
                accepts(fork, forkStep(STEP_BODY)),
                accepts(archive, archiveStep(ARCHIVE_CALL)),
                rejects(fork, forkStep(List.of("          : ./gradlew reportTestForks"))),
                rejects(fork, forkStep(List.of("          false && ./gradlew reportTestForks"))),
                rejects(fork, forkStep(List.of("          x=1 # ./gradlew reportTestForks"))),
                rejects(fork, forkStep(List.of("          # ./gradlew reportTestForks"))),
                rejects(fork, forkStep(List.of("          echo \"./gradlew reportTestForks\""))),
                rejects(archive, archiveStep("        run: : ./gradlew createConfluentArchive")),
                rejects(archive, archiveStep("        run: true || ./gradlew createConfluentArchive")));
    }

    @Test
    @DisplayName(
            "fixture: reportTestForks task rule rejects a task that declares the forks provider but"
                    + " prints something else (DEBT-14, 21-REVIEW WR-03)")
    void forkTaskRuleRejectsATaskThatDoesNotPrintItsProvider() {
        assertAll(
                ktsAccepts(REGISTER_BLOCK),
                ktsRejects(REGISTER_BLOCK.replace("println(forks.get())", "println(6)")),
                ktsRejects(REGISTER_BLOCK.replace("println(forks.get())", "println(other.get())")));
    }

    @Test
    @DisplayName(
            "fixture: dependabot rules accept single quotes, a trailing comment, unquoted values and list"
                    + " items at the key's indent (DEBT-14, 21-REVIEW IN-01)")
    void dependabotRulesAcceptValidYamlSpellings() {
        Function<List<String>, Optional<String>> ecosystem =
                TestsWorkflowPinGuardUnitTest::dependabotEcosystemProblem;
        Function<List<String>, Optional<String>> exclude = TestsWorkflowPinGuardUnitTest::excludeProblem;
        List<String> base = dependabot(List.of(), UPSTREAM);
        List<String> single = withEntry(base, ACTIONS_ENTRY, "  - package-ecosystem: 'github-actions'");
        List<String> unquoted = withEntry(base, ACTIONS_ENTRY, "  - package-ecosystem: github-actions");
        List<String> commented =
                withEntry(
                        base, ACTIONS_ENTRY, ACTIONS_ENTRY + " # keeps pins current");
        List<String> gradleSingle = withEntry(base, GRADLE_ENTRY, "  - package-ecosystem: 'gradle'");
        List<String> atKeyIndent = actionsEntryWithItems(4, 4, UPSTREAM);
        List<String> reversed =
                actionsEntryWithItems(4, 6, List.of(WORKFLOWS_DIR + "tests.yaml", UPSTREAM.get(0)));
        assertAll(
                accepts(ecosystem, single),
                accepts(exclude, single),
                accepts(ecosystem, unquoted),
                accepts(exclude, unquoted),
                accepts(ecosystem, commented),
                accepts(exclude, commented),
                accepts(ecosystem, gradleSingle),
                accepts(ecosystem, atKeyIndent),
                accepts(exclude, atKeyIndent),
                accepts(exclude, reversed));
    }

    @Test
    @DisplayName(
            "fixture: dependabot rules still reject mismatched quotes, an empty list and a list at the"
                    + " entry's indent (DEBT-14, 21-REVIEW IN-01)")
    void dependabotRulesStillRejectMismatchedQuotesAndMisplacedLists() {
        Function<List<String>, Optional<String>> ecosystem =
                TestsWorkflowPinGuardUnitTest::dependabotEcosystemProblem;
        Function<List<String>, Optional<String>> exclude = TestsWorkflowPinGuardUnitTest::excludeProblem;
        List<String> base = dependabot(List.of(), UPSTREAM);
        List<String> mismatched = withEntry(base, ACTIONS_ENTRY, "  - package-ecosystem: \"github-actions'");
        assertAll(
                rejects(ecosystem, mismatched),
                rejects(exclude, mismatched),
                rejects(exclude, actionsEntryWithItems(4, 4, List.of())),
                rejects(exclude, actionsEntryWithItems(4, 2, UPSTREAM)),
                rejects(exclude, actionsEntryWithItems(6, 4, UPSTREAM)));
    }

    @Test
    @DisplayName(
            "fixture list: the upstream workflow fixtures are the production constant (DEBT-14,"
                    + " 21-REVIEW IN-02)")
    void upstreamFixtureIsTheProductionConstant() {
        assertSame(
                UPSTREAM_WORKFLOWS,
                UPSTREAM,
                "the accept fixtures must be built from the rule's own UPSTREAM_WORKFLOWS constant");
    }
}
