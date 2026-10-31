package com.clickhouse.kafka.connect.ci;

import static com.clickhouse.kafka.connect.ci.TestsWorkflowPinGuardUnitTest.TRAILING_COMMENT;
import static com.clickhouse.kafka.connect.ci.TestsWorkflowPinGuardUnitTest.checkoutCredentialsProblem;
import static com.clickhouse.kafka.connect.ci.TestsWorkflowPinGuardUnitTest.nonCommentLines;
import static com.clickhouse.kafka.connect.ci.TestsWorkflowPinGuardUnitTest.runsOnProblem;
import static com.clickhouse.kafka.connect.ci.TestsWorkflowPinGuardUnitTest.stepBlock;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free pin for CIH-04 and the Phase 30 review rows WR-02 and IN-03: the safety gates of the
 * fork-owned {@code release.yaml} and {@code pre-release.yaml}. Both files are read line by line,
 * and every rule names the file and the offending text: one read scope at the top and the two job
 * scopes, the pinned runner, a checkout that persists no credentials and fetches the full history,
 * the publish condition and every {@code PUBLISH:} value, the complementary dry-run conditions, the
 * compare chain in order before the tag gate and the publish step, a compare that is neither
 * conditional nor advisory, the report upload that always runs, the concurrency group, no {@code
 * continue-on-error}, and the equality of the two files once their name, flag and expectation lines
 * are normalised. The generic action-pin rule stays in {@code WorkflowActionPinGuardUnitTest}.
 *
 * <p>The files are read as UTF-8 through {@code TestsWorkflowPinGuardUnitTest.nonCommentLines}, because
 * the test JVM runs with {@code file.encoding=windows-1252}. No YAML library is involved. Deliberately
 * has no superclass - deriving from {@code ClickHouseBase} would inherit its {@code clickhouse} tag
 * and drop this class out of {@code testUnit}.
 *
 * <p>Every rule is also run on fixtures: {@link #VALID} must be accepted, and each reject fixture is
 * one edit of it, built by helpers that throw when their anchor is missing, so a fixture can never
 * silently equal the valid list.
 */
class ReleaseWorkflowGuardUnitTest {
    private static final Path WORKFLOWS = Path.of(".github", "workflows");
    private static final String RELEASE = "release.yaml";
    private static final String PRE_RELEASE = "pre-release.yaml";

    private static final String PUBLISH_CONDITION =
            "!inputs.dry_run && github.ref == 'refs/heads/custom_build'";
    private static final String DRY_RUN_CONDITION =
            "inputs.dry_run || github.ref != 'refs/heads/custom_build'";
    private static final String PUBLISH_IF = "if: ${{ " + PUBLISH_CONDITION + " }}";
    private static final String PUBLISH_ENV = "PUBLISH: ${{ " + PUBLISH_CONDITION + " }}";
    private static final String PUBLISH_PREFIX = "PUBLISH:";
    private static final String DRY_RUN_IF = "if: ${{ " + DRY_RUN_CONDITION + " }}";
    private static final String DELETE_IF = "if: ${{ always() && (" + DRY_RUN_CONDITION + ") }}";
    private static final String GROUP_LINE =
            "group: ${{ ("
                    + DRY_RUN_CONDITION
                    + ") && format('release-dry-run-{0}', github.run_id) || 'release-publish' }}";
    private static final String CANCEL_LINE = "cancel-in-progress: false";
    private static final String CONCURRENCY = "concurrency:";
    private static final String TOP_PERMISSIONS = "permissions:";
    private static final String JOB_PERMISSIONS = "    permissions:";
    private static final int JOB_PERMISSIONS_INDENT = 4;
    private static final String READ_CONTENTS = "  contents: read";
    private static final List<String> JOB_SCOPES = List.of("contents: write", "actions: read");
    private static final String WRITE_SUFFIX = ": write";
    private static final String WRITE_ALL_SUFFIX = ": write-all";
    private static final String FETCH_FULL_HISTORY = "fetch-depth: 0";
    private static final String CONTINUE_ON_ERROR = "continue-on-error";
    private static final String IF_PREFIX = "if:";
    private static final String ALWAYS_IF = "if: always()";
    private static final String UPLOAD_USES = "uses: actions/upload-artifact@";
    private static final String REPORT_NAME = "name: archive-compare-report";
    private static final String COMPARE_CALL =
            "python3 -I .github/scripts/compare_confluent_archives.py \"$BASELINE_ARCHIVE\" \"$ARCHIVE\""
                    + " \"$ALLOWLIST\"";
    private static final String COMPARE_REDIRECTION = " > \"$report\" 2>&1 || status=$?";
    private static final String COMPARE_EXIT = "exit \"$status\"";
    private static final String TAG_LISTING = "git tag --merged HEAD";
    private static final String BASELINE_SCRIPT = "baseline_tag.py \"$RELEASE_VERSION\"";
    private static final String ALLOWLIST_PATH =
            ".planning/scripts/confluent-archive-allowlist-${RELEASE_VERSION}.txt";
    private static final String WORKTREE_ADD = "git worktree add --detach";
    private static final String TAG_REF = "refs/tags/";
    private static final String BASELINE_WRAPPER = "\"$baseline_dir/gradlew\"";

    private static final String CHECKOUT_STEP = "Checkout code";
    private static final String ARCHIVE_CHECK_STEP = "Check archive";
    private static final String SELECT_STEP = "Select the baseline tag";
    private static final String BASELINE_STEP = "Build the baseline archive from the previous tag";
    private static final String COMPARE_STEP = "Compare the archive with the previous release";
    private static final String UPLOAD_STEP = "Upload the archive compare report";
    private static final String TESTS_GATE_STEP = "Check tests.yaml is green on this SHA";
    private static final String TAG_GATE_STEP = "Check the release tag is absent";
    private static final String PUBLISH_STEP = "Publish release";
    private static final String DRAFT_STEP = "Create draft release (dry run)";
    private static final String DRAFT_CHECK_STEP = "Check the draft holds the archive (dry run)";
    private static final String DRAFT_TAG_STEP = "Check no tag exists for the draft (dry run)";
    private static final String DELETE_STEP = "Delete draft release (dry run)";
    private static final List<String> CHAIN =
            List.of(
                    ARCHIVE_CHECK_STEP,
                    SELECT_STEP,
                    BASELINE_STEP,
                    COMPARE_STEP,
                    UPLOAD_STEP,
                    TESTS_GATE_STEP,
                    PUBLISH_STEP,
                    DRAFT_STEP);
    private static final List<String> DRY_RUN_STEPS =
            List.of(DRAFT_STEP, DRAFT_CHECK_STEP, DRAFT_TAG_STEP);

    private static final String RULE_PERMISSIONS = "permissions";
    private static final String RULE_RUNNER = "runner";
    private static final String RULE_CHECKOUT = "checkout";
    private static final String RULE_PUBLISH = "publish gate";
    private static final String RULE_DRY_RUN = "dry-run gate";
    private static final String RULE_CHAIN = "compare chain order";
    private static final String RULE_COMPARE = "compare strictness";
    private static final String RULE_STEPS = "compare chain steps";
    private static final String RULE_ADVISORY = "no continue-on-error";
    private static final String RULE_CONCURRENCY = "concurrency";
    private static final String RULE_TWIN = "twin equality";

    private static final Pattern STEP_HEADER = Pattern.compile("^\\s*-\\s+name:\\s*(.*?)\\s*$");

    private static final String PRE_RELEASE_NAME = "Pre-release";
    private static final String RELEASE_NAME = "Release";
    private static final String PRERELEASE_FLAG = " --prerelease";
    private static final String PRERELEASE_EXPECTATION = "true true ${GITHUB_SHA}";
    private static final String RELEASE_EXPECTATION = "true false ${GITHUB_SHA}";
    private static final String PRERELEASE_NOTICE = "prerelease=true";
    private static final String RELEASE_NOTICE = "prerelease=false";

    private static final String UPLOAD_LINE =
            "        uses: actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9 # v7.0.2";
    private static final String CHECKOUT_USES_LINE =
            "        uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1";
    private static final String FETCH_LINE =
            "          fetch-depth: 0 # the baseline tag is built from its own worktree";
    private static final String PERSIST_LINE = "          persist-credentials: false";
    private static final String COMPARE_LINE = "          " + COMPARE_CALL + COMPARE_REDIRECTION;
    private static final String EXIT_LINE = "            " + COMPARE_EXIT;
    private static final String GROUP_FULL_LINE = "  " + GROUP_LINE;
    private static final String CANCEL_FULL_LINE = "  " + CANCEL_LINE;
    private static final String RUNNER_LINE = "    runs-on: ubuntu-24.04";
    private static final String JOB_SCOPE_WRITE_LINE = "      contents: write";
    private static final String PUBLISH_ENV_LINE = "          " + PUBLISH_ENV;

    private static String header(String step) {
        return "      - name: " + step;
    }

    /** The non-comment lines of a release workflow; the pre-release twin differs in four tokens. */
    private static List<String> workflow(boolean pre) {
        String name = pre ? PRE_RELEASE_NAME : RELEASE_NAME;
        String flag = pre ? PRERELEASE_FLAG : "";
        String expectation = pre ? PRERELEASE_EXPECTATION : RELEASE_EXPECTATION;
        String notice = pre ? PRERELEASE_NOTICE : RELEASE_NOTICE;
        return List.of(
                "name: " + name,
                "run-name: " + name + " ${{ github.ref_name }}",
                "",
                "on:",
                "  workflow_dispatch:",
                "    inputs:",
                "      dry_run:",
                "        type: boolean",
                "        default: false",
                "",
                TOP_PERMISSIONS,
                READ_CONTENTS,
                "",
                CONCURRENCY,
                GROUP_FULL_LINE,
                CANCEL_FULL_LINE,
                "",
                "jobs:",
                "  build_release:",
                "    name: build_release",
                RUNNER_LINE,
                "    timeout-minutes: 30",
                JOB_PERMISSIONS,
                JOB_SCOPE_WRITE_LINE,
                "      actions: read",
                "    steps:",
                header(CHECKOUT_STEP),
                CHECKOUT_USES_LINE,
                "        with:",
                PERSIST_LINE,
                FETCH_LINE,
                "",
                header(ARCHIVE_CHECK_STEP),
                "        run: |",
                "          test -s \"$ARCHIVE\"",
                "",
                header(SELECT_STEP),
                "        run: |",
                "          baseline=\"$(" + TAG_LISTING + " --list 'v*' | python3 -I .github/scripts/"
                        + BASELINE_SCRIPT + ")\" || {",
                "            exit 1",
                "          }",
                "          allowlist=\"" + ALLOWLIST_PATH + "\"",
                "",
                header(BASELINE_STEP),
                "        run: |",
                "          " + WORKTREE_ADD + " \"$baseline_dir\" \"" + TAG_REF + "${BASELINE_TAG}\"",
                "          " + BASELINE_WRAPPER + " -p \"$baseline_dir\" createConfluentArchive",
                "",
                header(COMPARE_STEP),
                "        run: |",
                "          status=0",
                COMPARE_LINE,
                "          if [ \"$status\" -ne 0 ]; then",
                EXIT_LINE,
                "          fi",
                "",
                header(UPLOAD_STEP),
                "        " + ALWAYS_IF,
                UPLOAD_LINE,
                "        with:",
                "          " + REPORT_NAME,
                "",
                header(TESTS_GATE_STEP),
                "        env:",
                PUBLISH_ENV_LINE,
                "        run: |",
                "          echo \"tests.yaml\"",
                "",
                header(TAG_GATE_STEP),
                "        env:",
                PUBLISH_ENV_LINE,
                "        run: |",
                "          echo \"tag\"",
                "",
                header(PUBLISH_STEP),
                "        " + PUBLISH_IF,
                "        run: gh release create \"$RELEASE_VERSION\" \"$ARCHIVE\" --target \"$GITHUB_SHA\""
                        + flag,
                "",
                header(DRAFT_STEP),
                "        " + DRY_RUN_IF,
                "        run: gh release create \"$DRY_TAG\" \"$ARCHIVE\" --draft" + flag,
                "",
                header(DRAFT_CHECK_STEP),
                "        " + DRY_RUN_IF,
                "        run: |",
                "          expected=\"" + expectation + " 1\"",
                "          echo \"draft=true, " + notice + "\"",
                "",
                header(DRAFT_TAG_STEP),
                "        " + DRY_RUN_IF,
                "        run: |",
                "          echo \"no tag\"",
                "",
                header(DELETE_STEP),
                "        " + DELETE_IF,
                "        run: |",
                "          echo \"cleaned\"");
    }

    private static final List<String> VALID = workflow(false);
    private static final List<String> VALID_PRE = workflow(true);

    private static int indent(String line) {
        return line.length() - line.stripLeading().length();
    }

    private static String code(String line) {
        return TRAILING_COMMENT.matcher(line).replaceFirst("").strip();
    }

    private static Optional<String> problem(String rule, String detail) {
        return Optional.of(rule + ": " + detail);
    }

    private static List<String> read(String file) throws IOException {
        return nonCommentLines(WORKFLOWS.resolve(file));
    }

    private static void assertNoProblem(String file, Optional<String> problem) {
        problem.ifPresent(message -> fail(file + ": " + message));
    }

    private static int headerIndex(List<String> lines, String step) {
        for (int i = 0; i < lines.size(); i++) {
            Matcher header = STEP_HEADER.matcher(lines.get(i));
            if (header.matches() && code(header.group(1)).equals(step)) {
                return i;
            }
        }
        return -1;
    }

    private static List<String> codeOf(List<String> block) {
        return block.stream().map(ReleaseWorkflowGuardUnitTest::code).toList();
    }

    static Optional<String> permissionsProblem(List<String> lines) {
        int top = lines.indexOf(TOP_PERMISSIONS);
        if (lines.stream().filter(TOP_PERMISSIONS::equals).count() != 1
                || top + 1 >= lines.size()
                || !READ_CONTENTS.equals(lines.get(top + 1))) {
            return problem(
                    RULE_PERMISSIONS,
                    "one column-0 'permissions:' followed by '" + READ_CONTENTS.strip() + "'");
        }
        int job = lines.indexOf(JOB_PERMISSIONS);
        if (lines.stream().filter(JOB_PERMISSIONS::equals).count() != 1) {
            return problem(RULE_PERMISSIONS, "exactly one job-level 'permissions:' block");
        }
        List<String> entries = new ArrayList<>();
        for (int i = job + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank() || indent(line) <= JOB_PERMISSIONS_INDENT) {
                break;
            }
            entries.add(code(line));
        }
        if (!entries.equals(JOB_SCOPES)) {
            return problem(
                    RULE_PERMISSIONS,
                    "the job scopes must be exactly " + JOB_SCOPES + " but are " + entries);
        }
        long writes =
                codeOf(lines).stream()
                        .filter(line -> line.endsWith(WRITE_SUFFIX) || line.endsWith(WRITE_ALL_SUFFIX))
                        .count();
        if (writes != 1) {
            return problem(RULE_PERMISSIONS, "exactly one line may grant a write scope but " + writes + " do");
        }
        return Optional.empty();
    }

    static Optional<String> runnerProblem(List<String> lines) {
        return runsOnProblem(lines).flatMap(message -> problem(RULE_RUNNER, message));
    }

    static Optional<String> checkoutProblem(List<String> lines) {
        Optional<String> credentials = checkoutCredentialsProblem(lines);
        if (credentials.isPresent()) {
            return problem(RULE_CHECKOUT, credentials.get());
        }
        Optional<List<String>> block = stepBlock(lines, CHECKOUT_STEP);
        if (block.isEmpty()) {
            return problem(RULE_CHECKOUT, "no step named '" + CHECKOUT_STEP + "'");
        }
        if (!codeOf(block.get()).contains(FETCH_FULL_HISTORY)) {
            return problem(
                    RULE_CHECKOUT,
                    "the checkout must hold '" + FETCH_FULL_HISTORY + "' so the baseline tag can be built");
        }
        return Optional.empty();
    }

    static Optional<String> publishGateProblem(List<String> lines) {
        Optional<List<String>> publish = stepBlock(lines, PUBLISH_STEP);
        if (publish.isEmpty()) {
            return problem(RULE_PUBLISH, "no step named '" + PUBLISH_STEP + "'");
        }
        if (!codeOf(publish.get()).contains(PUBLISH_IF)) {
            return problem(RULE_PUBLISH, "the step '" + PUBLISH_STEP + "' must hold the line " + PUBLISH_IF);
        }
        List<String> values =
                lines.stream().map(String::strip).filter(line -> line.startsWith(PUBLISH_PREFIX)).toList();
        if (values.size() < 2) {
            return problem(
                    RULE_PUBLISH, "at least two '" + PUBLISH_PREFIX + "' values are expected but " + values.size() + " found");
        }
        for (String value : values) {
            if (!value.equals(PUBLISH_ENV)) {
                return problem(RULE_PUBLISH, "every value must be '" + PUBLISH_ENV + "' but found '" + value + "'");
            }
        }
        return Optional.empty();
    }

    static Optional<String> dryRunGateProblem(List<String> lines) {
        for (String step : DRY_RUN_STEPS) {
            Optional<String> missing = stepHoldsLine(lines, step, DRY_RUN_IF, RULE_DRY_RUN);
            if (missing.isPresent()) {
                return missing;
            }
        }
        return stepHoldsLine(lines, DELETE_STEP, DELETE_IF, RULE_DRY_RUN);
    }

    private static Optional<String> stepHoldsLine(
            List<String> lines, String step, String expected, String rule) {
        Optional<List<String>> block = stepBlock(lines, step);
        if (block.isEmpty()) {
            return problem(rule, "no step named '" + step + "'");
        }
        if (!codeOf(block.get()).contains(expected)) {
            return problem(rule, "the step '" + step + "' must hold the line " + expected);
        }
        return Optional.empty();
    }

    static Optional<String> chainOrderProblem(List<String> lines) {
        int previous = -1;
        String previousStep = "";
        for (String step : CHAIN) {
            int index = headerIndex(lines, step);
            if (index < 0) {
                return problem(RULE_CHAIN, "no step named '" + step + "'");
            }
            if (index <= previous) {
                return problem(RULE_CHAIN, "'" + step + "' must come after '" + previousStep + "'");
            }
            previous = index;
            previousStep = step;
        }
        return Optional.empty();
    }

    static Optional<String> compareStrictnessProblem(List<String> lines) {
        Optional<List<String>> block = stepBlock(lines, COMPARE_STEP);
        if (block.isEmpty()) {
            return problem(RULE_COMPARE, "no step named '" + COMPARE_STEP + "'");
        }
        for (String line : block.get()) {
            String stripped = code(line);
            if (stripped.startsWith(IF_PREFIX)) {
                return problem(RULE_COMPARE, "the compare must not be conditional: " + stripped);
            }
            if (stripped.contains(CONTINUE_ON_ERROR)) {
                return problem(RULE_COMPARE, "the compare must not be advisory: " + stripped);
            }
        }
        List<String> calls =
                block.get().stream()
                        .map(String::strip)
                        .map(
                                line ->
                                        line.endsWith(COMPARE_REDIRECTION)
                                                ? line.substring(0, line.length() - COMPARE_REDIRECTION.length())
                                                : line)
                        .toList();
        if (!calls.contains(COMPARE_CALL)) {
            return problem(
                    RULE_COMPARE,
                    "the step must hold the call '"
                            + COMPARE_CALL
                            + "' with no more than the report redirection and the status capture after it");
        }
        if (!codeOf(block.get()).contains(COMPARE_EXIT)) {
            return problem(RULE_COMPARE, "the step must fail the job with the line '" + COMPARE_EXIT + "'");
        }
        return Optional.empty();
    }

    private static Optional<String> stepHoldsText(
            List<String> lines, String step, String text, String rule) {
        Optional<List<String>> block = stepBlock(lines, step);
        if (block.isEmpty()) {
            return problem(rule, "no step named '" + step + "'");
        }
        if (block.get().stream().noneMatch(line -> line.contains(text))) {
            return problem(rule, "the step '" + step + "' must hold '" + text + "'");
        }
        return Optional.empty();
    }

    static Optional<String> chainStepsProblem(List<String> lines) {
        return firstProblem(
                        stepHoldsText(lines, SELECT_STEP, TAG_LISTING, RULE_STEPS),
                        stepHoldsText(lines, SELECT_STEP, BASELINE_SCRIPT, RULE_STEPS),
                        stepHoldsText(lines, SELECT_STEP, ALLOWLIST_PATH, RULE_STEPS),
                        stepHoldsText(lines, BASELINE_STEP, WORKTREE_ADD, RULE_STEPS),
                        stepHoldsText(lines, BASELINE_STEP, TAG_REF, RULE_STEPS),
                        stepHoldsText(lines, BASELINE_STEP, BASELINE_WRAPPER, RULE_STEPS),
                        stepHoldsLine(lines, UPLOAD_STEP, ALWAYS_IF, RULE_STEPS),
                        stepHoldsText(lines, UPLOAD_STEP, UPLOAD_USES, RULE_STEPS),
                        stepHoldsLine(lines, UPLOAD_STEP, REPORT_NAME, RULE_STEPS));
    }

    @SafeVarargs
    private static Optional<String> firstProblem(Optional<String>... problems) {
        return Arrays.stream(problems).filter(Optional::isPresent).findFirst().orElse(Optional.empty());
    }

    static Optional<String> advisoryProblem(List<String> lines) {
        for (String line : lines) {
            if (code(line).contains(CONTINUE_ON_ERROR)) {
                return problem(RULE_ADVISORY, line.strip());
            }
        }
        return Optional.empty();
    }

    static Optional<String> concurrencyProblem(List<String> lines) {
        int block = lines.indexOf(CONCURRENCY);
        if (lines.stream().filter(CONCURRENCY::equals).count() != 1 || block + 2 >= lines.size()) {
            return problem(RULE_CONCURRENCY, "exactly one column-0 'concurrency:' block");
        }
        if (!GROUP_FULL_LINE.equals(lines.get(block + 1))) {
            return problem(
                    RULE_CONCURRENCY,
                    "the group must be '" + GROUP_LINE + "' but is '" + lines.get(block + 1).strip() + "'");
        }
        if (!CANCEL_FULL_LINE.equals(lines.get(block + 2))) {
            return problem(
                    RULE_CONCURRENCY,
                    "the line after the group must be '" + CANCEL_LINE + "' but is '"
                            + lines.get(block + 2).strip() + "'");
        }
        return Optional.empty();
    }

    private static String normalised(String line) {
        return line.replace(PRE_RELEASE_NAME, RELEASE_NAME)
                .replace(PRERELEASE_FLAG, "")
                .replace(PRERELEASE_EXPECTATION, RELEASE_EXPECTATION)
                .replace(PRERELEASE_NOTICE, RELEASE_NOTICE);
    }

    static Optional<String> twinProblem(List<String> release, List<String> preRelease) {
        List<String> normalisedTwin = preRelease.stream().map(ReleaseWorkflowGuardUnitTest::normalised).toList();
        int common = Math.min(release.size(), normalisedTwin.size());
        for (int i = 0; i < common; i++) {
            if (!release.get(i).equals(normalisedTwin.get(i))) {
                return problem(
                        RULE_TWIN,
                        "line "
                                + (i + 1)
                                + " of the non-comment lines differs: '"
                                + release.get(i).strip()
                                + "' against '"
                                + normalisedTwin.get(i).strip()
                                + "'");
            }
        }
        if (release.size() != normalisedTwin.size()) {
            return problem(
                    RULE_TWIN,
                    "the files have "
                            + release.size()
                            + " and "
                            + normalisedTwin.size()
                            + " non-comment lines");
        }
        return Optional.empty();
    }

    private static List<String> replacing(List<String> lines, String line, String... replacements) {
        int index = lines.indexOf(line);
        if (index < 0 || index != lines.lastIndexOf(line)) {
            throw new IllegalStateException("fixture anchor must occur exactly once: " + line);
        }
        List<String> edited = new ArrayList<>(lines);
        edited.remove(index);
        edited.addAll(index, Arrays.asList(replacements));
        return edited;
    }

    private static List<String> after(List<String> lines, String anchor, String... added) {
        String[] replacement = new String[added.length + 1];
        replacement[0] = anchor;
        System.arraycopy(added, 0, replacement, 1, added.length);
        return replacing(lines, anchor, replacement);
    }

    /** Replaces the first line of the named step whose stripped text is {@code line}. */
    private static List<String> replacingInStep(
            List<String> lines, String step, String line, String... replacements) {
        int start = headerIndex(lines, step);
        if (start < 0) {
            throw new IllegalStateException("fixture step must exist: " + step);
        }
        for (int i = start + 1; i < lines.size() && !STEP_HEADER.matcher(lines.get(i)).matches(); i++) {
            if (lines.get(i).strip().equals(line.strip())) {
                List<String> edited = new ArrayList<>(lines);
                edited.remove(i);
                edited.addAll(i, Arrays.asList(replacements));
                return edited;
            }
        }
        throw new IllegalStateException("fixture line must exist in step '" + step + "': " + line);
    }

    private static List<String> swappedHeaders(List<String> lines, String first, String second) {
        int a = headerIndex(lines, first);
        int b = headerIndex(lines, second);
        if (a < 0 || b < 0) {
            throw new IllegalStateException("fixture steps must exist: " + first + ", " + second);
        }
        List<String> edited = new ArrayList<>(lines);
        edited.set(a, lines.get(b));
        edited.set(b, lines.get(a));
        return edited;
    }

    private static Executable accepts(Function<List<String>, Optional<String>> rule, List<String> lines) {
        return () -> {
            Optional<String> problem = rule.apply(lines);
            assertTrue(problem.isEmpty(), () -> "expected the fixture to pass but: " + problem.orElse(""));
        };
    }

    private static Executable rejects(
            Function<List<String>, Optional<String>> rule, List<String> lines, String rulePhrase) {
        return () -> {
            Optional<String> problem = rule.apply(lines);
            assertTrue(problem.isPresent(), "expected the fixture to be rejected by " + rulePhrase);
            assertTrue(
                    problem.get().contains(rulePhrase),
                    () -> "expected the rule '" + rulePhrase + "' but: " + problem.get());
        };
    }

    private static Executable twinRejected(List<String> release, List<String> preRelease) {
        return () -> {
            Optional<String> problem = twinProblem(release, preRelease);
            assertTrue(problem.isPresent(), "expected the twins to differ");
            assertTrue(problem.get().contains(RULE_TWIN), () -> "expected the twin rule but: " + problem.get());
        };
    }

    @ParameterizedTest(name = "CIH-04: {0} has one read scope at the top and only the two job scopes")
    @ValueSource(strings = {RELEASE, PRE_RELEASE})
    void workflowGrantsOnlyTheJobScopes(String file) throws IOException {
        assertNoProblem(file, permissionsProblem(read(file)));
    }

    @ParameterizedTest(name = "CIH-04: {0} runs on the pinned runner")
    @ValueSource(strings = {RELEASE, PRE_RELEASE})
    void workflowRunsOnThePinnedRunner(String file) throws IOException {
        assertNoProblem(file, runnerProblem(read(file)));
    }

    @ParameterizedTest(name = "CIH-04: {0} checks out the full history and persists no credentials")
    @ValueSource(strings = {RELEASE, PRE_RELEASE})
    void workflowChecksOutTheFullHistoryWithoutCredentials(String file) throws IOException {
        assertNoProblem(file, checkoutProblem(read(file)));
    }

    @ParameterizedTest(name = "CIH-04: the publish step of {0} runs only from custom_build and every PUBLISH value agrees")
    @ValueSource(strings = {RELEASE, PRE_RELEASE})
    void workflowPublishesOnlyFromCustomBuild(String file) throws IOException {
        assertNoProblem(file, publishGateProblem(read(file)));
    }

    @ParameterizedTest(name = "CIH-04: the dry-run steps of {0} carry the complement of the publish condition")
    @ValueSource(strings = {RELEASE, PRE_RELEASE})
    void workflowDryRunStepsCarryTheComplement(String file) throws IOException {
        assertNoProblem(file, dryRunGateProblem(read(file)));
    }

    @ParameterizedTest(name = "CIH-04: the compare chain of {0} runs in order before the tag gate and the publish step")
    @ValueSource(strings = {RELEASE, PRE_RELEASE})
    void workflowRunsTheCompareChainInOrder(String file) throws IOException {
        assertNoProblem(file, chainOrderProblem(read(file)));
    }

    @ParameterizedTest(name = "CIH-04: the compare step of {0} has no condition and does not swallow its exit status")
    @ValueSource(strings = {RELEASE, PRE_RELEASE})
    void workflowCompareIsStrict(String file) throws IOException {
        assertNoProblem(file, compareStrictnessProblem(read(file)));
    }

    @ParameterizedTest(name = "CIH-04: the baseline, tag selection and report upload steps of {0} hold their calls")
    @ValueSource(strings = {RELEASE, PRE_RELEASE})
    void workflowChainStepsHoldTheirCalls(String file) throws IOException {
        assertNoProblem(file, chainStepsProblem(read(file)));
    }

    @ParameterizedTest(name = "CIH-04: {0} has no continue-on-error")
    @ValueSource(strings = {RELEASE, PRE_RELEASE})
    void workflowHasNoContinueOnError(String file) throws IOException {
        assertNoProblem(file, advisoryProblem(read(file)));
    }

    @ParameterizedTest(name = "CIH-04: {0} gives a dry run its own concurrency group and queues a publish")
    @ValueSource(strings = {RELEASE, PRE_RELEASE})
    void workflowSeparatesTheDryRunConcurrencyGroup(String file) throws IOException {
        assertNoProblem(file, concurrencyProblem(read(file)));
    }

    @Test
    @DisplayName("CIH-04: release.yaml and pre-release.yaml are equal once name, flag and expectation lines are normalised")
    void workflowTwinsAreEqualAfterNormalisation() throws IOException {
        assertNoProblem(RELEASE + " and " + PRE_RELEASE, twinProblem(read(RELEASE), read(PRE_RELEASE)));
    }

    @Test
    @DisplayName("CIH-04: fixture: the permissions rule accepts the valid scopes and rejects an extra write scope, a top-level write and a second block")
    void fixturePermissionsRule() {
        assertAll(
                accepts(ReleaseWorkflowGuardUnitTest::permissionsProblem, VALID),
                rejects(
                        ReleaseWorkflowGuardUnitTest::permissionsProblem,
                        after(VALID, JOB_SCOPE_WRITE_LINE, "      issues: write"),
                        RULE_PERMISSIONS),
                rejects(
                        ReleaseWorkflowGuardUnitTest::permissionsProblem,
                        replacing(VALID, READ_CONTENTS, "  contents: write"),
                        RULE_PERMISSIONS),
                rejects(
                        ReleaseWorkflowGuardUnitTest::permissionsProblem,
                        replacing(VALID, JOB_SCOPE_WRITE_LINE, "      contents: write-all"),
                        RULE_PERMISSIONS),
                rejects(
                        ReleaseWorkflowGuardUnitTest::permissionsProblem,
                        after(VALID, RUNNER_LINE, JOB_PERMISSIONS, "      packages: read"),
                        RULE_PERMISSIONS));
    }

    @Test
    @DisplayName("CIH-04: fixture: the runner rule accepts the pinned runner and rejects the floating label")
    void fixtureRunnerRule() {
        assertAll(
                accepts(ReleaseWorkflowGuardUnitTest::runnerProblem, VALID),
                rejects(
                        ReleaseWorkflowGuardUnitTest::runnerProblem,
                        replacing(VALID, RUNNER_LINE, "    runs-on: ubuntu-latest"),
                        RULE_RUNNER));
    }

    @Test
    @DisplayName("CIH-04: fixture: the checkout rule accepts the valid step and rejects persisted credentials, a shallow fetch and a missing fetch depth")
    void fixtureCheckoutRule() {
        assertAll(
                accepts(ReleaseWorkflowGuardUnitTest::checkoutProblem, VALID),
                rejects(
                        ReleaseWorkflowGuardUnitTest::checkoutProblem,
                        replacing(VALID, PERSIST_LINE, "          persist-credentials: true"),
                        RULE_CHECKOUT),
                rejects(
                        ReleaseWorkflowGuardUnitTest::checkoutProblem,
                        replacing(VALID, FETCH_LINE, "          fetch-depth: 1"),
                        RULE_CHECKOUT),
                rejects(
                        ReleaseWorkflowGuardUnitTest::checkoutProblem,
                        replacing(VALID, FETCH_LINE),
                        RULE_CHECKOUT));
    }

    @Test
    @DisplayName("CIH-04: fixture: the publish rule accepts the valid gate and rejects a widened condition, a drifted PUBLISH value and a single PUBLISH value")
    void fixturePublishRule() {
        assertAll(
                accepts(ReleaseWorkflowGuardUnitTest::publishGateProblem, VALID),
                rejects(
                        ReleaseWorkflowGuardUnitTest::publishGateProblem,
                        replacing(VALID, "        " + PUBLISH_IF, "        if: ${{ !inputs.dry_run }}"),
                        RULE_PUBLISH),
                rejects(
                        ReleaseWorkflowGuardUnitTest::publishGateProblem,
                        replacingInStep(VALID, TAG_GATE_STEP, PUBLISH_ENV, "          PUBLISH: true"),
                        RULE_PUBLISH),
                rejects(
                        ReleaseWorkflowGuardUnitTest::publishGateProblem,
                        replacingInStep(VALID, TAG_GATE_STEP, PUBLISH_ENV),
                        RULE_PUBLISH));
    }

    @Test
    @DisplayName("CIH-04: fixture: the dry-run rule accepts the valid conditions and rejects a missing condition and a cleanup that does not always run")
    void fixtureDryRunRule() {
        assertAll(
                accepts(ReleaseWorkflowGuardUnitTest::dryRunGateProblem, VALID),
                rejects(
                        ReleaseWorkflowGuardUnitTest::dryRunGateProblem,
                        replacingInStep(VALID, DRAFT_STEP, DRY_RUN_IF),
                        RULE_DRY_RUN),
                rejects(
                        ReleaseWorkflowGuardUnitTest::dryRunGateProblem,
                        replacingInStep(VALID, DRAFT_TAG_STEP, DRY_RUN_IF, "        " + PUBLISH_IF),
                        RULE_DRY_RUN),
                rejects(
                        ReleaseWorkflowGuardUnitTest::dryRunGateProblem,
                        replacingInStep(
                                VALID, DELETE_STEP, DELETE_IF, "        " + DRY_RUN_IF),
                        RULE_DRY_RUN));
    }

    @Test
    @DisplayName("CIH-04: fixture: the chain rule accepts the valid order and rejects a swapped pair and a missing step")
    void fixtureChainOrderRule() {
        assertAll(
                accepts(ReleaseWorkflowGuardUnitTest::chainOrderProblem, VALID),
                rejects(
                        ReleaseWorkflowGuardUnitTest::chainOrderProblem,
                        swappedHeaders(VALID, COMPARE_STEP, TESTS_GATE_STEP),
                        RULE_CHAIN),
                rejects(
                        ReleaseWorkflowGuardUnitTest::chainOrderProblem,
                        swappedHeaders(VALID, SELECT_STEP, BASELINE_STEP),
                        RULE_CHAIN),
                rejects(
                        ReleaseWorkflowGuardUnitTest::chainOrderProblem,
                        replacing(VALID, header(UPLOAD_STEP), "      - name: Keep the report"),
                        RULE_CHAIN));
    }

    @Test
    @DisplayName("CIH-04: fixture: the compare rule accepts the valid step and rejects a condition, continue-on-error, a swallowed status and a missing exit")
    void fixtureCompareStrictnessRule() {
        assertAll(
                accepts(ReleaseWorkflowGuardUnitTest::compareStrictnessProblem, VALID),
                rejects(
                        ReleaseWorkflowGuardUnitTest::compareStrictnessProblem,
                        after(VALID, header(COMPARE_STEP), "        if: ${{ !inputs.dry_run }}"),
                        RULE_COMPARE),
                rejects(
                        ReleaseWorkflowGuardUnitTest::compareStrictnessProblem,
                        after(VALID, header(COMPARE_STEP), "        continue-on-error: true"),
                        RULE_COMPARE),
                rejects(
                        ReleaseWorkflowGuardUnitTest::compareStrictnessProblem,
                        replacing(
                                VALID,
                                COMPARE_LINE,
                                "          " + COMPARE_CALL + " > \"$report\" 2>&1 || true"),
                        RULE_COMPARE),
                rejects(
                        ReleaseWorkflowGuardUnitTest::compareStrictnessProblem,
                        replacing(VALID, EXIT_LINE, "            echo \"compare failed\""),
                        RULE_COMPARE),
                rejects(
                        ReleaseWorkflowGuardUnitTest::compareStrictnessProblem,
                        replacing(VALID, COMPARE_LINE, "          python3 .github/scripts/other.py"),
                        RULE_COMPARE));
    }

    @Test
    @DisplayName("CIH-04: fixture: the chain steps rule accepts the valid steps and rejects a missing baseline script, worktree, wrapper, always condition and report name")
    void fixtureChainStepsRule() {
        assertAll(
                accepts(ReleaseWorkflowGuardUnitTest::chainStepsProblem, VALID),
                rejects(
                        ReleaseWorkflowGuardUnitTest::chainStepsProblem,
                        replacingInStep(
                                VALID,
                                SELECT_STEP,
                                "baseline=\"$(" + TAG_LISTING + " --list 'v*' | python3 -I .github/scripts/"
                                        + BASELINE_SCRIPT + ")\" || {",
                                "          baseline=\"$(git describe --tags)\" || {"),
                        RULE_STEPS),
                rejects(
                        ReleaseWorkflowGuardUnitTest::chainStepsProblem,
                        replacingInStep(
                                VALID,
                                BASELINE_STEP,
                                WORKTREE_ADD + " \"$baseline_dir\" \"" + TAG_REF + "${BASELINE_TAG}\"",
                                "          git checkout \"${BASELINE_TAG}\""),
                        RULE_STEPS),
                rejects(
                        ReleaseWorkflowGuardUnitTest::chainStepsProblem,
                        replacingInStep(
                                VALID,
                                BASELINE_STEP,
                                BASELINE_WRAPPER + " -p \"$baseline_dir\" createConfluentArchive",
                                "          ./gradlew createConfluentArchive"),
                        RULE_STEPS),
                rejects(
                        ReleaseWorkflowGuardUnitTest::chainStepsProblem,
                        replacingInStep(VALID, UPLOAD_STEP, ALWAYS_IF),
                        RULE_STEPS),
                rejects(
                        ReleaseWorkflowGuardUnitTest::chainStepsProblem,
                        replacingInStep(VALID, UPLOAD_STEP, REPORT_NAME, "          name: report"),
                        RULE_STEPS));
    }

    @Test
    @DisplayName("CIH-04: fixture: the advisory rule accepts a comment that names continue-on-error and rejects the key on any step")
    void fixtureAdvisoryRule() {
        assertAll(
                accepts(
                        ReleaseWorkflowGuardUnitTest::advisoryProblem,
                        after(VALID, RUNNER_LINE, "    # never continue-on-error here")),
                rejects(
                        ReleaseWorkflowGuardUnitTest::advisoryProblem,
                        after(VALID, header(PUBLISH_STEP), "        continue-on-error: false"),
                        RULE_ADVISORY),
                rejects(
                        ReleaseWorkflowGuardUnitTest::advisoryProblem,
                        after(VALID, RUNNER_LINE, "    continue-on-error: true"),
                        RULE_ADVISORY));
    }

    @Test
    @DisplayName("CIH-04: fixture: the concurrency rule accepts the valid group and rejects the shared group and a cancelling queue")
    void fixtureConcurrencyRule() {
        assertAll(
                accepts(ReleaseWorkflowGuardUnitTest::concurrencyProblem, VALID),
                rejects(
                        ReleaseWorkflowGuardUnitTest::concurrencyProblem,
                        replacing(VALID, GROUP_FULL_LINE, "  group: release-publish"),
                        RULE_CONCURRENCY),
                rejects(
                        ReleaseWorkflowGuardUnitTest::concurrencyProblem,
                        replacing(VALID, CANCEL_FULL_LINE, "  cancel-in-progress: true"),
                        RULE_CONCURRENCY));
    }

    @Test
    @DisplayName("CIH-04: fixture: the twin rule accepts files that differ only in the four normalised tokens")
    void fixtureTwinRuleAcceptsTheFourTokens() {
        assertAll(
                accepts(lines -> twinProblem(VALID, lines), VALID_PRE),
                accepts(lines -> twinProblem(VALID, lines), VALID));
    }

    @Test
    @DisplayName("CIH-04: fixture: the twin rule rejects an extra step in one file")
    void fixtureTwinRuleRejectsAnExtraStep() {
        List<String> extra = after(VALID_PRE, header(DELETE_STEP), "        run: echo extra");
        assertAll(
                twinRejected(VALID, extra),
                twinRejected(extra, VALID_PRE),
                twinRejected(VALID, VALID_PRE.subList(0, VALID_PRE.size() - 2)));
    }

    @Test
    @DisplayName("CIH-04: fixture: the twin rule rejects a gate line that differs in one file")
    void fixtureTwinRuleRejectsADifferingGate() {
        assertAll(
                twinRejected(VALID, replacing(VALID_PRE, "        " + PUBLISH_IF, "        if: ${{ !inputs.dry_run }}")),
                twinRejected(VALID, replacing(VALID_PRE, RUNNER_LINE, "    runs-on: ubuntu-22.04")),
                twinRejected(VALID, replacing(VALID_PRE, FETCH_LINE)));
    }
}
