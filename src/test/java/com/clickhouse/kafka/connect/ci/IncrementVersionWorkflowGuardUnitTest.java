package com.clickhouse.kafka.connect.ci;

import static com.clickhouse.kafka.connect.ci.TestsWorkflowPinGuardUnitTest.TRAILING_COMMENT;
import static com.clickhouse.kafka.connect.ci.TestsWorkflowPinGuardUnitTest.nonCommentLines;
import static com.clickhouse.kafka.connect.ci.TestsWorkflowPinGuardUnitTest.stepBlock;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Docker-free pin for CIH-02 and the Phase 30 review rows IN-01 and IN-02: the fork-owned {@code
 * increment-version.yml}. It runs the bump script, takes the dispatch input through {@code env:},
 * keeps every {@code ${{ }}} expression out of the run blocks, installs no npm package and runs no
 * Node, adds {@code VERSION} together with the anchors file, grants the token scopes on the job and
 * offers exactly four bumps. The generic action-pin rule stays in {@code
 * WorkflowActionPinGuardUnitTest}, which discovers the file from the directory.
 *
 * <p>The file is read as UTF-8 through {@code TestsWorkflowPinGuardUnitTest.nonCommentLines}, because
 * the test JVM runs with {@code file.encoding=windows-1252}. No YAML library is involved: every rule
 * is a line-level check, and each problem names the rule and the offending line. Deliberately has no
 * superclass - deriving from {@code ClickHouseBase} would inherit its {@code clickhouse} tag and drop
 * this class out of {@code testUnit}.
 *
 * <p>Every rule is also run on fixtures: {@link #VALID} must be accepted, and each reject fixture is
 * exactly one edit of it, built by helpers that throw when their anchor line is missing, so a fixture
 * can never silently equal the valid list.
 */
class IncrementVersionWorkflowGuardUnitTest {
    private static final Path WORKFLOW = Path.of(".github", "workflows", "increment-version.yml");
    private static final String INCREMENT_STEP = "Increment version";
    private static final String CHECKOUT_STEP = "Check out code";
    private static final String SCRIPT_CALL =
            "NEW_VERSION=\"$(python3 -I .github/scripts/bump_version.py \"$BUMP\")\"";
    private static final String ENV_KEY = "env:";
    private static final List<String> BUMP_ENV_LINES =
            List.of("BUMP: ${{ inputs.version }}", "BUMP: ${{ github.event.inputs.version }}");
    private static final String EXPRESSION_OPENER = "${{";
    private static final List<String> FORBIDDEN_TOKENS = List.of("setup-node", "npm ", "node ", "semver");
    private static final String GIT_ADD = "git add VERSION .planning/scripts/fork-anchors.txt";
    private static final String GIT_COMMIT_PREFIX = "git commit -m";
    private static final String GIT_COMMIT = "git commit -m \"Increment version to ${NEW_VERSION}\"";
    private static final String TOP_LEVEL_PERMISSIONS = "permissions:";
    private static final String JOB_LEVEL_PERMISSIONS = "    permissions:";
    private static final String READ_CONTENTS = "  contents: read";
    private static final List<String> JOB_WRITE_SCOPES =
            List.of("contents: write", "pull-requests: write");
    private static final String WRITE_SUFFIX = ": write";
    private static final String WRITE_ALL_SUFFIX = ": write-all";
    private static final String VERSION_INPUT = "version:";
    private static final String CHOICE_TYPE = "type: choice";
    private static final String OPTIONS_KEY = "options:";
    private static final String LIST_ITEM_PREFIX = "- ";
    private static final List<String> OPTIONS = List.of("patch", "minor", "major", "gg");
    private static final String HEADER_START = "# FORK MODIFICATION";
    private static final List<String> FORBIDDEN_HEADER_TEXT = List.of("left unfixed", "vnull");
    private static final String CHECKOUT_USES =
            "uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1";
    /**
     * The shape of the pin, not its value: Dependabot moves the SHA and the version comment together in
     * its grouped monthly pull request, so the guard accepts any 40-hex SHA with a {@code # vX.Y.Z}
     * comment and still refuses a tag, a branch, a short or upper-case SHA and a missing comment.
     */
    private static final Pattern CHECKOUT_PIN =
            Pattern.compile("^uses: actions/checkout@[0-9a-f]{40} # v\\d+\\.\\d+\\.\\d+$");
    private static final String USES_PREFIX = "uses:";
    private static final String PERSIST_CREDENTIALS = "persist-credentials";
    private static final Pattern RUN_KEY = Pattern.compile("^\\s*(?:-\\s+)?run:(.*)$");

    private static final String RULE_SCRIPT_CALL = "script call";
    private static final String RULE_RUN_EXPRESSION = "no expression in a run block";
    private static final String RULE_NODE = "no Node or npm";
    private static final String RULE_COMMIT = "commit step";
    private static final String RULE_PERMISSIONS = "permissions";
    private static final String RULE_OPTIONS = "dispatch options";
    private static final String RULE_HEADER = "header";
    private static final String RULE_CHECKOUT = "checkout pin";

    private static final String STEPS_LINE = "    steps:";
    private static final String ECHO_NEW_VERSION = "        echo \"New version is ${NEW_VERSION}\"";
    private static final String COMMIT_LINE = "        " + GIT_COMMIT;
    private static final String ADD_LINE = "        " + GIT_ADD;
    private static final String CALL_LINE = "        " + SCRIPT_CALL;
    private static final String BUMP_LINE = "        BUMP: ${{ inputs.version }}";
    private static final String CHECKOUT_LINE = "      " + CHECKOUT_USES;
    private static final String FETCH_DEPTH_LINE =
            "          fetch-depth: 0 # this is required to see all branches";

    /** The non-comment lines of the workflow as committed by plan 37-01. */
    private static final List<String> VALID =
            List.of(
                    "name: Increment Version",
                    "",
                    "on:",
                    "  workflow_dispatch:",
                    "    inputs:",
                    "        version:",
                    "          type: choice",
                    "          description: 'Version - patch, minor, major, or gg (the _ggN counter)'",
                    "          required: true",
                    "          options:",
                    "          - patch",
                    "          - minor",
                    "          - major",
                    "          - gg",
                    "",
                    "permissions:",
                    "  contents: read",
                    "",
                    "jobs:",
                    "  build:",
                    "    runs-on: ubuntu-24.04",
                    "    timeout-minutes: 10",
                    "    permissions:",
                    "      contents: write",
                    "      pull-requests: write",
                    STEPS_LINE,
                    "    - name: " + CHECKOUT_STEP,
                    CHECKOUT_LINE,
                    "      with:",
                    FETCH_DEPTH_LINE,
                    "    - name: Configure Git",
                    "      run: |",
                    "        git config --global user.email \"actions@github.com\"",
                    "        git config --global user.name \"GitHub Action\"",
                    "        git config --global --add --bool push.autoSetupRemote true",
                    "    - name: " + INCREMENT_STEP,
                    "      env:",
                    BUMP_LINE,
                    "      run: |",
                    CALL_LINE,
                    "        echo \"NEW_VERSION=${NEW_VERSION}\" >> \"$GITHUB_ENV\"",
                    ECHO_NEW_VERSION,
                    "    - name: Commit and push",
                    "      env:",
                    "        GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}",
                    "      run: |",
                    "        git checkout -b \"update-version-${NEW_VERSION}\"",
                    ADD_LINE,
                    COMMIT_LINE,
                    "        git push",
                    "        gh pr create --title \"Automatic version increment ${NEW_VERSION}\" --body \"This"
                            + " is an automated PR to increment the version.\"");

    private static final List<String> VALID_HEADER =
            List.of(
                    "# FORK MODIFICATION: the bump is a script, not an inline expression.",
                    "# It refuses and writes nothing when either file is not as expected.",
                    "name: Increment Version");

    private static int indent(String line) {
        return line.length() - line.stripLeading().length();
    }

    private static String code(String line) {
        return TRAILING_COMMENT.matcher(line).replaceFirst("").strip();
    }

    private static Optional<String> problem(String rule, String detail) {
        return Optional.of(WORKFLOW + ": " + rule + ": " + detail);
    }

    private static List<String> readAll() throws IOException {
        return Files.readAllLines(WORKFLOW, StandardCharsets.UTF_8);
    }

    private static void assertNoProblem(Optional<String> problem) {
        problem.ifPresent(message -> fail(message));
    }

    static Optional<String> scriptCallProblem(List<String> lines) {
        Optional<List<String>> block = stepBlock(lines, INCREMENT_STEP);
        if (block.isEmpty()) {
            return problem(RULE_SCRIPT_CALL, "no step named '" + INCREMENT_STEP + "'");
        }
        List<String> stepCode = block.get().stream().map(IncrementVersionWorkflowGuardUnitTest::code).toList();
        if (!stepCode.contains(SCRIPT_CALL)) {
            return problem(RULE_SCRIPT_CALL, "the step must hold the line " + SCRIPT_CALL);
        }
        if (!stepCode.contains(ENV_KEY) || stepCode.stream().noneMatch(BUMP_ENV_LINES::contains)) {
            return problem(
                    RULE_SCRIPT_CALL,
                    "the step must take BUMP from env: as one of " + BUMP_ENV_LINES);
        }
        return Optional.empty();
    }

    static Optional<String> runExpressionProblem(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            Matcher key = RUN_KEY.matcher(lines.get(i));
            if (!key.matches()) {
                continue;
            }
            if (key.group(1).contains(EXPRESSION_OPENER)) {
                return problem(RULE_RUN_EXPRESSION, lines.get(i).strip());
            }
            int keyColumn = lines.get(i).indexOf("run:");
            for (int j = i + 1; j < lines.size(); j++) {
                String next = lines.get(j);
                if (!next.isBlank() && indent(next) <= keyColumn) {
                    break;
                }
                if (next.contains(EXPRESSION_OPENER)) {
                    return problem(RULE_RUN_EXPRESSION, next.strip());
                }
            }
        }
        return Optional.empty();
    }

    static Optional<String> nodeProblem(List<String> lines) {
        for (String line : lines) {
            String lower = code(line).toLowerCase(Locale.ROOT);
            for (String token : FORBIDDEN_TOKENS) {
                if (lower.contains(token)) {
                    return problem(RULE_NODE, "'" + token.strip() + "' in " + line.strip());
                }
            }
        }
        return Optional.empty();
    }

    static Optional<String> commitProblem(List<String> lines) {
        List<String> stepCode = lines.stream().map(IncrementVersionWorkflowGuardUnitTest::code).toList();
        if (!stepCode.contains(GIT_ADD)) {
            return problem(RULE_COMMIT, "a line must add VERSION with the anchors file: " + GIT_ADD);
        }
        List<String> commits =
                stepCode.stream().filter(line -> line.startsWith(GIT_COMMIT_PREFIX)).toList();
        if (!commits.equals(List.of(GIT_COMMIT))) {
            return problem(RULE_COMMIT, "the one commit line must be " + GIT_COMMIT + " but is " + commits);
        }
        return Optional.empty();
    }

    static Optional<String> permissionsProblem(List<String> lines) {
        long topLevel = lines.stream().filter(TOP_LEVEL_PERMISSIONS::equals).count();
        int topIndex = lines.indexOf(TOP_LEVEL_PERMISSIONS);
        if (topLevel != 1 || topIndex + 1 >= lines.size() || !READ_CONTENTS.equals(lines.get(topIndex + 1))) {
            return problem(RULE_PERMISSIONS, "one column-0 'permissions:' followed by '" + READ_CONTENTS.strip() + "'");
        }
        List<String> writes =
                lines.stream()
                        .map(IncrementVersionWorkflowGuardUnitTest::code)
                        .filter(line -> line.endsWith(WRITE_SUFFIX) || line.endsWith(WRITE_ALL_SUFFIX))
                        .toList();
        if (!writes.equals(JOB_WRITE_SCOPES)) {
            return problem(RULE_PERMISSIONS, "the write scopes must be exactly " + JOB_WRITE_SCOPES + " but are " + writes);
        }
        int jobIndex = lines.indexOf(JOB_LEVEL_PERMISSIONS);
        if (jobIndex < 0
                || jobIndex + JOB_WRITE_SCOPES.size() >= lines.size()
                || !JOB_WRITE_SCOPES.equals(
                        lines.subList(jobIndex + 1, jobIndex + 1 + JOB_WRITE_SCOPES.size()).stream()
                                .map(IncrementVersionWorkflowGuardUnitTest::code)
                                .toList())) {
            return problem(RULE_PERMISSIONS, "the write scopes must sit under the job's own 'permissions:'");
        }
        return Optional.empty();
    }

    static Optional<String> optionsProblem(List<String> lines) {
        int input = -1;
        for (int i = 0; i < lines.size() && input < 0; i++) {
            if (code(lines.get(i)).equals(VERSION_INPUT)) {
                input = i;
            }
        }
        if (input < 0) {
            return problem(RULE_OPTIONS, "no 'version:' input");
        }
        boolean choice = false;
        boolean inOptions = false;
        List<String> options = new ArrayList<>();
        for (int i = input + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String code = code(line);
            if (code.isEmpty()) {
                continue;
            }
            if (inOptions) {
                if (!code.startsWith(LIST_ITEM_PREFIX)) {
                    break;
                }
                options.add(code.substring(LIST_ITEM_PREFIX.length()).strip());
            } else if (code.equals(CHOICE_TYPE)) {
                choice = true;
            } else if (code.equals(OPTIONS_KEY)) {
                inOptions = true;
            } else if (indent(line) <= indent(lines.get(input))) {
                break;
            }
        }
        if (!choice || !options.equals(OPTIONS)) {
            return problem(
                    RULE_OPTIONS,
                    "the version input must be a choice of exactly " + OPTIONS + " but is "
                            + (choice ? "a choice of " : "not a choice, with ") + options);
        }
        return Optional.empty();
    }

    /** Takes every line of the file, comments included. */
    static Optional<String> headerProblem(List<String> allLines) {
        if (allLines.isEmpty() || !allLines.get(0).startsWith(HEADER_START)) {
            return problem(RULE_HEADER, "the first line must start with '" + HEADER_START + "'");
        }
        for (String line : allLines) {
            for (String text : FORBIDDEN_HEADER_TEXT) {
                if (line.contains(text)) {
                    return problem(RULE_HEADER, "'" + text + "' in " + line.strip());
                }
            }
        }
        return Optional.empty();
    }

    static Optional<String> checkoutProblem(List<String> lines) {
        Optional<List<String>> block = stepBlock(lines, CHECKOUT_STEP);
        if (block.isEmpty()) {
            return problem(RULE_CHECKOUT, "no step named '" + CHECKOUT_STEP + "'");
        }
        List<String> uses =
                block.get().stream().map(String::strip).filter(line -> line.startsWith(USES_PREFIX)).toList();
        if (uses.size() != 1 || !CHECKOUT_PIN.matcher(uses.get(0)).matches()) {
            return problem(
                    RULE_CHECKOUT,
                    "the step '"
                            + CHECKOUT_STEP
                            + "' must have one line of the shape "
                            + CHECKOUT_PIN.pattern()
                            + " but has "
                            + uses);
        }
        for (String line : block.get()) {
            if (line.contains(PERSIST_CREDENTIALS)) {
                return problem(
                        RULE_CHECKOUT,
                        "the push step needs the persisted credentials, so no '"
                                + PERSIST_CREDENTIALS
                                + "' key: "
                                + line.strip());
            }
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

    @Test
    @DisplayName("CIH-02: increment-version.yml calls the bump script with the dispatch input taken from env")
    void workflowCallsTheBumpScriptWithTheInputFromEnv() throws IOException {
        assertNoProblem(scriptCallProblem(nonCommentLines(WORKFLOW)));
    }

    @Test
    @DisplayName("CIH-02: increment-version.yml has no expression inside any run block")
    void workflowHasNoExpressionInsideARunBlock() throws IOException {
        assertNoProblem(runExpressionProblem(nonCommentLines(WORKFLOW)));
    }

    @Test
    @DisplayName("CIH-02: increment-version.yml has no Node and no npm step")
    void workflowHasNoNodeOrNpmStep() throws IOException {
        assertNoProblem(nodeProblem(nonCommentLines(WORKFLOW)));
    }

    @Test
    @DisplayName("CIH-02: increment-version.yml adds VERSION together with the anchors file and commits with the new version")
    void workflowAddsVersionTogetherWithTheAnchorsFile() throws IOException {
        assertNoProblem(commitProblem(nonCommentLines(WORKFLOW)));
    }

    @Test
    @DisplayName("CIH-02: increment-version.yml reads at the top and writes contents and pull requests on the job only")
    void workflowGrantsTheWriteScopesOnTheJobOnly() throws IOException {
        assertNoProblem(permissionsProblem(nonCommentLines(WORKFLOW)));
    }

    @Test
    @DisplayName("CIH-02: increment-version.yml offers exactly the choices patch, minor, major and gg")
    void workflowOffersExactlyTheFourBumps() throws IOException {
        assertNoProblem(optionsProblem(nonCommentLines(WORKFLOW)));
    }

    @Test
    @DisplayName("CIH-02: increment-version.yml starts with the fork header and no longer carries the defect note")
    void workflowHeaderCarriesNoDefectNote() throws IOException {
        assertNoProblem(headerProblem(readAll()));
    }

    @Test
    @DisplayName("CIH-02: increment-version.yml keeps the full-SHA checkout pin and the persisted credentials")
    void workflowKeepsTheCheckoutPinAndCredentials() throws IOException {
        assertNoProblem(checkoutProblem(nonCommentLines(WORKFLOW)));
    }

    @Test
    @DisplayName("CIH-02: fixture: the script call rule accepts the valid workflow and rejects a call without -I, a literal BUMP and a missing env")
    void fixtureScriptCallRule() {
        assertAll(
                accepts(IncrementVersionWorkflowGuardUnitTest::scriptCallProblem, VALID),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::scriptCallProblem,
                        replacing(
                                VALID,
                                CALL_LINE,
                                "        NEW_VERSION=\"$(python3 .github/scripts/bump_version.py \"$BUMP\")\""),
                        RULE_SCRIPT_CALL),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::scriptCallProblem,
                        replacing(VALID, BUMP_LINE, "        BUMP: patch"),
                        RULE_SCRIPT_CALL),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::scriptCallProblem,
                        replacing(VALID, BUMP_LINE, "        OTHER: ${{ inputs.version }}"),
                        RULE_SCRIPT_CALL));
    }

    @Test
    @DisplayName("CIH-02: fixture: the run block rule accepts env values and rejects an expression in a block or an inline run")
    void fixtureRunExpressionRule() {
        assertAll(
                accepts(IncrementVersionWorkflowGuardUnitTest::runExpressionProblem, VALID),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::runExpressionProblem,
                        replacing(
                                VALID,
                                ECHO_NEW_VERSION,
                                "        echo \"New version is ${{ env.NEW_VERSION }}\""),
                        RULE_RUN_EXPRESSION),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::runExpressionProblem,
                        after(VALID, STEPS_LINE, "    - run: echo \"${{ inputs.version }}\""),
                        RULE_RUN_EXPRESSION));
    }

    @Test
    @DisplayName("CIH-02: fixture: the Node rule accepts a comment that names node and rejects setup-node, npm and semver")
    void fixtureNodeRule() {
        assertAll(
                accepts(IncrementVersionWorkflowGuardUnitTest::nodeProblem, VALID),
                accepts(
                        IncrementVersionWorkflowGuardUnitTest::nodeProblem,
                        replacing(VALID, FETCH_DEPTH_LINE, "          fetch-depth: 0 # not node 24")),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::nodeProblem,
                        after(
                                VALID,
                                STEPS_LINE,
                                "    - uses: actions/setup-node@aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa # v4.0.0"),
                        RULE_NODE),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::nodeProblem,
                        after(VALID, ECHO_NEW_VERSION, "        npm install --no-save left-pad"),
                        RULE_NODE),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::nodeProblem,
                        after(VALID, ECHO_NEW_VERSION, "        VERSION=Semver"),
                        RULE_NODE));
    }

    @Test
    @DisplayName("CIH-02: fixture: the commit rule accepts the valid step and rejects a VERSION-only add, an expression message and no commit")
    void fixtureCommitRule() {
        assertAll(
                accepts(IncrementVersionWorkflowGuardUnitTest::commitProblem, VALID),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::commitProblem,
                        replacing(VALID, ADD_LINE, "        git add VERSION"),
                        RULE_COMMIT),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::commitProblem,
                        replacing(
                                VALID,
                                COMMIT_LINE,
                                "        git commit -m \"Increment version to ${{ env.NEW_VERSION }}\""),
                        RULE_COMMIT),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::commitProblem,
                        replacing(VALID, COMMIT_LINE),
                        RULE_COMMIT));
    }

    @Test
    @DisplayName("CIH-02: fixture: the permissions rule accepts the valid scopes and rejects an extra scope, a top-level write and write-all")
    void fixturePermissionsRule() {
        assertAll(
                accepts(IncrementVersionWorkflowGuardUnitTest::permissionsProblem, VALID),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::permissionsProblem,
                        after(VALID, "      pull-requests: write", "      actions: write"),
                        RULE_PERMISSIONS),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::permissionsProblem,
                        replacing(VALID, READ_CONTENTS, "  contents: write"),
                        RULE_PERMISSIONS),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::permissionsProblem,
                        replacing(VALID, "      pull-requests: write", "      pull-requests: write-all"),
                        RULE_PERMISSIONS));
    }

    @Test
    @DisplayName("CIH-02: fixture: the options rule accepts the four choices and rejects another order, a missing gg and a string input")
    void fixtureOptionsRule() {
        assertAll(
                accepts(IncrementVersionWorkflowGuardUnitTest::optionsProblem, VALID),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::optionsProblem,
                        after(replacing(VALID, "          - minor"), "          - major", "          - minor"),
                        RULE_OPTIONS),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::optionsProblem,
                        replacing(VALID, "          - gg"),
                        RULE_OPTIONS),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::optionsProblem,
                        replacing(VALID, "          type: choice", "          type: string"),
                        RULE_OPTIONS));
    }

    @Test
    @DisplayName("CIH-02: fixture: the header rule accepts the fork header and rejects the defect note, a null version text and a missing header")
    void fixtureHeaderRule() {
        assertAll(
                accepts(IncrementVersionWorkflowGuardUnitTest::headerProblem, VALID_HEADER),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::headerProblem,
                        after(VALID_HEADER, VALID_HEADER.get(1), "# Known and left unfixed: the bump."),
                        RULE_HEADER),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::headerProblem,
                        after(VALID_HEADER, VALID_HEADER.get(1), "# a dispatch wrote vnull into VERSION."),
                        RULE_HEADER),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::headerProblem,
                        VALID,
                        RULE_HEADER));
    }

    private static final String PIN_PREFIX = "      uses: actions/checkout@";
    private static final String OTHER_SHA = "0123456789abcdef0123456789abcdef01234567";

    @Test
    @DisplayName("WR-03: fixture: the checkout rule accepts a bumped SHA and version and still rejects every pin that is not a 40-hex SHA with a vX.Y.Z comment")
    void fixtureCheckoutRuleAcceptsABumpAndRejectsLoosePins() {
        Function<String, List<String>> withPin = pin -> replacing(VALID, CHECKOUT_LINE, pin);
        assertAll(
                accepts(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        withPin.apply(PIN_PREFIX + OTHER_SHA + " # v7.1.0")),
                accepts(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        withPin.apply(PIN_PREFIX + OTHER_SHA + " # v10.12.3")),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        withPin.apply(PIN_PREFIX + "v7.0.1"),
                        RULE_CHECKOUT),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        withPin.apply(PIN_PREFIX + "main"),
                        RULE_CHECKOUT),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        withPin.apply(PIN_PREFIX + OTHER_SHA.substring(1) + " # v7.1.0"),
                        RULE_CHECKOUT),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        withPin.apply(PIN_PREFIX + OTHER_SHA + "0 # v7.1.0"),
                        RULE_CHECKOUT),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        withPin.apply(PIN_PREFIX + OTHER_SHA.toUpperCase(Locale.ROOT) + " # v7.1.0"),
                        RULE_CHECKOUT),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        withPin.apply(PIN_PREFIX + OTHER_SHA),
                        RULE_CHECKOUT),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        withPin.apply(PIN_PREFIX + OTHER_SHA + " # main"),
                        RULE_CHECKOUT),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        withPin.apply(PIN_PREFIX + OTHER_SHA + " # v7"),
                        RULE_CHECKOUT),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        withPin.apply("      uses: actions/checkout-fork@" + OTHER_SHA + " # v7.1.0"),
                        RULE_CHECKOUT),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        withPin.apply("      uses: other/checkout@" + OTHER_SHA + " # v7.1.0"),
                        RULE_CHECKOUT),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        after(VALID, CHECKOUT_LINE, PIN_PREFIX + OTHER_SHA + " # v7.1.0"),
                        RULE_CHECKOUT),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        replacing(VALID, CHECKOUT_LINE),
                        RULE_CHECKOUT));
    }

    @Test
    @DisplayName("CIH-02: fixture: the checkout rule accepts the SHA pin and rejects a tag pin, persist-credentials and a missing step")
    void fixtureCheckoutRule() {
        assertAll(
                accepts(IncrementVersionWorkflowGuardUnitTest::checkoutProblem, VALID),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        replacing(VALID, CHECKOUT_LINE, "      uses: actions/checkout@v7"),
                        RULE_CHECKOUT),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        after(VALID, FETCH_DEPTH_LINE, "          persist-credentials: false"),
                        RULE_CHECKOUT),
                rejects(
                        IncrementVersionWorkflowGuardUnitTest::checkoutProblem,
                        replacing(VALID, "    - name: " + CHECKOUT_STEP, "    - name: Get the code"),
                        RULE_CHECKOUT));
    }
}
