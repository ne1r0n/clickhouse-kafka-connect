package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

/**
 * Docker-free pin for SWP-03: protobuf stays on 3.x, so the {@code gradle} entry of {@code
 * dependabot.yml} ignores semver-major updates of {@code com.google.protobuf:protobuf-java} and of
 * {@code com.google.protobuf:protoc}, the code generator {@code build.gradle.kts} declares on the
 * same {@code libprotobuf} version ref. The {@code github-actions} entry carries no ignore. The
 * reason is that Dependabot PR #10 failed {@code ProtobufIngestTest} with a {@code VerifyError}
 * against the Confluent 7.9.1 test stack; the re-open trigger is FUT-03, recorded in {@code
 * docs/FORK-DELTA.md}. Both names are exact, so the protobuf Gradle plugin stays updatable.
 *
 * <p>The file is read through {@link TestsWorkflowPinGuardUnitTest#nonCommentLines}, as are the
 * fixtures, so a commented-out ignore does not count. No YAML library is involved. Deliberately
 * extends nothing - extending {@code ClickHouseBase} would inherit its {@code clickhouse} tag and
 * drop this class out of {@code testUnit}.
 */
class DependabotProtobufIgnoreGuardUnitTest {
    private static final String FILE = ".github/dependabot.yml";
    private static final String PROTOBUF_JAVA = "com.google.protobuf:protobuf-java";
    private static final String PROTOC = "com.google.protobuf:protoc";
    private static final List<String> REQUIRED_NAMES = List.of(PROTOBUF_JAVA, PROTOC);
    private static final String SEMVER_MAJOR = "version-update:semver-major";
    private static final String SEMVER_MINOR = "version-update:semver-minor";
    private static final String GRADLE = "gradle";
    private static final String GITHUB_ACTIONS = "github-actions";
    private static final String IGNORE_KEY = "ignore:";
    private static final String DEPENDENCY_NAME_KEY = "dependency-name";
    private static final String UPDATE_TYPES_KEY = "update-types";
    private static final String VERSIONS_KEY = "versions";
    private static final String PROTOBUF_PLUGIN = "protobufPlugin";
    private static final String LIBPROTOBUF = "libprotobuf";
    private static final String LIST_DASH = "- ";

    private static final String NO_GRADLE_ENTRY = "has no gradle package-ecosystem entry";
    private static final String NO_GITHUB_ACTIONS_ENTRY = "has no github-actions package-ecosystem entry";
    private static final String NO_IGNORE = "has no `ignore:` list";
    private static final String WILDCARD = "wildcard";
    private static final String NO_ITEM = "has no item for";
    private static final String UPDATE_TYPES_PROBLEM = "update-types";
    private static final String VERSIONS_PROBLEM = "`versions:`";
    private static final String GRADLE_ENTRY_BELONGS = "gradle entry";

    private static final Pattern GRADLE_ENTRY = ecosystemEntry(GRADLE);
    private static final Pattern GITHUB_ACTIONS_ENTRY = ecosystemEntry(GITHUB_ACTIONS);
    private static final Pattern KEY_VALUE = Pattern.compile("^([\\w-]+):\\s*(.*)$");
    // The bare `ignore:` key, tolerating indentation and a trailing " # comment" like ecosystemEntry.
    private static final Pattern IGNORE_KEY_LINE = Pattern.compile("^\\s*ignore:(?:\\s+#.*)?\\s*$");

    private static final String FIXTURE_FILE = "dependabot-fixture.yml";
    private static final String FLOW_MAJOR = "    update-types: [\"" + SEMVER_MAJOR + "\"]";

    @TempDir Path tempDir;

    private static Pattern ecosystemEntry(String ecosystem) {
        return Pattern.compile(
                "^\\s*-\\s+package-ecosystem:\\s*([\"']?)" + Pattern.quote(ecosystem) + "\\1\\s*(?:\\s#.*)?$");
    }

    private static int indent(String line) {
        return line.length() - line.stripLeading().length();
    }

    private static String unquote(String value) {
        String stripped = value.strip();
        if (stripped.length() >= 2
                && (stripped.charAt(0) == '"' || stripped.charAt(0) == '\'')
                && stripped.charAt(stripped.length() - 1) == stripped.charAt(0)) {
            return stripped.substring(1, stripped.length() - 1);
        }
        return stripped;
    }

    // The lines under an ecosystem entry, up to the first non-blank line at or above its indent.
    private static Optional<List<String>> entryBlock(List<String> lines, Pattern entry) {
        for (int i = 0; i < lines.size(); i++) {
            if (!entry.matcher(lines.get(i)).matches()) {
                continue;
            }
            int entryIndent = indent(lines.get(i));
            List<String> block = new ArrayList<>();
            for (int j = i + 1; j < lines.size(); j++) {
                String line = lines.get(j);
                if (!line.isBlank() && indent(line) <= entryIndent) {
                    break;
                }
                block.add(line);
            }
            return Optional.of(block);
        }
        return Optional.empty();
    }

    // Reads "key: value" into the item; the value is a scalar or a flow list, empty for a block list.
    private static String putKeyValue(Map<String, List<String>> item, String text) {
        Matcher keyValue = KEY_VALUE.matcher(text);
        if (!keyValue.matches()) {
            return null;
        }
        String value = TestsWorkflowPinGuardUnitTest.TRAILING_COMMENT.matcher(keyValue.group(2))
                .replaceFirst("")
                .strip();
        if (value.startsWith("#")) {
            value = "";
        }
        List<String> values = new ArrayList<>();
        if (value.startsWith("[") && value.endsWith("]")) {
            for (String part : value.substring(1, value.length() - 1).split(",")) {
                String unquoted = unquote(part);
                if (!unquoted.isEmpty()) {
                    values.add(unquoted);
                }
            }
        } else if (!value.isEmpty()) {
            values.add(unquote(value));
        }
        item.put(keyValue.group(1), values);
        return keyValue.group(1);
    }

    private static Optional<List<Map<String, List<String>>>> ignoreItems(List<String> block) {
        int key = -1;
        for (int i = 0; i < block.size() && key < 0; i++) {
            if (IGNORE_KEY_LINE.matcher(block.get(i)).matches()) {
                key = i;
            }
        }
        if (key < 0) {
            return Optional.empty();
        }
        int keyIndent = indent(block.get(key));
        List<Map<String, List<String>>> items = new ArrayList<>();
        int itemIndent = -1;
        Map<String, List<String>> item = null;
        String currentKey = null;
        for (int j = key + 1; j < block.size(); j++) {
            String line = block.get(j);
            if (line.isBlank()) {
                continue;
            }
            String text = line.strip();
            boolean listItem = text.startsWith(LIST_DASH);
            int lineIndent = indent(line);
            // A sequence may start at its key's own indent; anything else at or below it ends the list.
            if (lineIndent < keyIndent || (lineIndent == keyIndent && !listItem)) {
                break;
            }
            if (listItem && (itemIndent < 0 || lineIndent == itemIndent)) {
                itemIndent = lineIndent;
                item = new LinkedHashMap<>();
                items.add(item);
                currentKey = putKeyValue(item, text.substring(LIST_DASH.length()).strip());
            } else if (listItem) {
                if (currentKey != null) {
                    String value =
                            TestsWorkflowPinGuardUnitTest.TRAILING_COMMENT.matcher(
                                            text.substring(LIST_DASH.length()))
                                    .replaceFirst("");
                    item.get(currentKey).add(unquote(value));
                }
            } else if (item != null) {
                String parsedKey = putKeyValue(item, text);
                currentKey = parsedKey != null ? parsedKey : currentKey;
            }
        }
        return Optional.of(items);
    }

    private static String nameOf(Map<String, List<String>> item) {
        List<String> names = item.getOrDefault(DEPENDENCY_NAME_KEY, List.of());
        return names.isEmpty() ? "" : names.get(0);
    }

    static Optional<String> gradleProtobufIgnoreProblem(List<String> lines) {
        Optional<List<String>> block = entryBlock(lines, GRADLE_ENTRY);
        if (block.isEmpty()) {
            return Optional.of(FILE + " " + NO_GRADLE_ENTRY);
        }
        Optional<List<Map<String, List<String>>>> parsed = ignoreItems(block.get());
        if (parsed.isEmpty()) {
            return Optional.of(
                    FILE + ": the gradle entry " + NO_IGNORE + "; it must ignore " + SEMVER_MAJOR
                            + " updates of " + String.join(" and ", REQUIRED_NAMES));
        }
        List<Map<String, List<String>>> items = parsed.get();
        for (Map<String, List<String>> item : items) {
            if (nameOf(item).contains("*")) {
                return Optional.of(
                        FILE + ": the gradle ignore item '" + nameOf(item) + "' is a " + WILDCARD
                                + "; a wildcard would also hold the protobuf Gradle plugin ("
                                + PROTOBUF_PLUGIN + "), which must stay updatable");
            }
        }
        for (String required : REQUIRED_NAMES) {
            Optional<Map<String, List<String>>> item =
                    items.stream().filter(candidate -> nameOf(candidate).equals(required)).findFirst();
            if (item.isEmpty()) {
                String shared =
                        required.equals(PROTOC)
                                ? "; protoc shares the " + LIBPROTOBUF + " version ref with protobuf-java"
                                        + " (build.gradle.kts), so an un-ignored protoc could still move"
                                        + " both to 4.x"
                                : "";
                return Optional.of(
                        FILE + ": the gradle entry's ignore list " + NO_ITEM + " '" + required + "'"
                                + shared);
            }
            List<String> updateTypes = item.get().getOrDefault(UPDATE_TYPES_KEY, List.of());
            if (!updateTypes.equals(List.of(SEMVER_MAJOR))) {
                return Optional.of(
                        FILE + ": the gradle ignore item '" + required + "' has " + UPDATE_TYPES_PROBLEM
                                + " " + updateTypes + " but must be exactly [" + SEMVER_MAJOR + "]");
            }
            if (item.get().containsKey(VERSIONS_KEY)) {
                return Optional.of(
                        FILE + ": the gradle ignore item '" + required + "' carries a " + VERSIONS_PROBLEM
                                + " key, which narrows the ignore below every major version");
            }
        }
        return Optional.empty();
    }

    static Optional<String> githubActionsIgnoreProblem(List<String> lines) {
        Optional<List<String>> block = entryBlock(lines, GITHUB_ACTIONS_ENTRY);
        if (block.isEmpty()) {
            return Optional.of(FILE + " " + NO_GITHUB_ACTIONS_ENTRY);
        }
        if (block.get().stream().anyMatch(line -> IGNORE_KEY_LINE.matcher(line).matches())) {
            return Optional.of(
                    FILE + ": the github-actions entry carries an `ignore:` list; the protobuf decision"
                            + " belongs to the " + GRADLE_ENTRY_BELONGS + " only");
        }
        return Optional.empty();
    }

    private static List<String> realFile() throws IOException {
        return TestsWorkflowPinGuardUnitTest.nonCommentLines(TestsWorkflowPinGuardUnitTest.DEPENDABOT);
    }

    private static String quoted(String name) {
        return "\"" + name + "\"";
    }

    private static List<String> item(String name, String... updateTypeLines) {
        List<String> lines = new ArrayList<>();
        lines.add("  - " + DEPENDENCY_NAME_KEY + ": " + name);
        lines.addAll(List.of(updateTypeLines));
        return lines;
    }

    private static List<String> ignore(List<List<String>> items) {
        List<String> lines = new ArrayList<>();
        lines.add(IGNORE_KEY);
        items.forEach(lines::addAll);
        return lines;
    }

    private static List<String> canonicalIgnore() {
        return ignore(
                List.of(
                        item(quoted(PROTOBUF_JAVA), FLOW_MAJOR),
                        item(quoted(PROTOC), FLOW_MAJOR)));
    }

    // A complete two-entry file; each line list is placed at the entries' key indent (4 spaces).
    private List<String> fixtureLines(List<String> gradleLines, List<String> actionsLines)
            throws IOException {
        List<String> file = new ArrayList<>();
        file.addAll(
                List.of(
                        "version: 2",
                        "updates:",
                        "  - package-ecosystem: \"" + GRADLE + "\"",
                        "    directory: \"/\""));
        gradleLines.forEach(line -> file.add("    " + line));
        file.addAll(
                List.of(
                        "    schedule:",
                        "      interval: \"monthly\"",
                        "  - package-ecosystem: \"" + GITHUB_ACTIONS + "\"",
                        "    directory: \"/\""));
        actionsLines.forEach(line -> file.add("    " + line));
        file.addAll(List.of("    schedule:", "      interval: \"monthly\""));
        Path fixture = tempDir.resolve(FIXTURE_FILE);
        Files.writeString(fixture, String.join("\n", file) + "\n", StandardCharsets.UTF_8);
        return TestsWorkflowPinGuardUnitTest.nonCommentLines(fixture);
    }

    private static Executable accepts(String fixture, Optional<String> problem) {
        return () -> assertTrue(problem.isEmpty(), fixture + " must be accepted but: " + problem.orElse(""));
    }

    private static Executable rejects(String fixture, Optional<String> problem, String cause) {
        return () -> {
            assertTrue(problem.isPresent(), fixture + " must be rejected");
            assertTrue(
                    problem.get().contains(cause),
                    fixture + " must be rejected for '" + cause + "' but: " + problem.get());
        };
    }

    @Test
    @DisplayName(
            "dependabot.yml's gradle entry ignores com.google.protobuf:protobuf-java and"
                    + " com.google.protobuf:protoc major versions (SWP-03)")
    void gradleEntryIgnoresProtobufJavaAndProtocMajorVersions() throws IOException {
        gradleProtobufIgnoreProblem(realFile()).ifPresent(message -> fail(message));
    }

    @Test
    @DisplayName("dependabot.yml's github-actions entry carries no ignore (SWP-03)")
    void githubActionsEntryCarriesNoIgnore() throws IOException {
        githubActionsIgnoreProblem(realFile()).ifPresent(message -> fail(message));
    }

    @Test
    @DisplayName("fixture: the canonical two-item ignore list is accepted (SWP-03)")
    void fixtureCanonicalIgnoreIsAccepted() throws IOException {
        List<String> lines = fixtureLines(canonicalIgnore(), List.of());
        assertAll(
                accepts("canonical gradle entry", gradleProtobufIgnoreProblem(lines)),
                accepts("github-actions entry without ignore", githubActionsIgnoreProblem(lines)));
    }

    @Test
    @DisplayName(
            "fixture: a block-list update-types, single-quoted names and unquoted names are accepted"
                    + " (SWP-03)")
    void fixtureBlockListAndQuoteVariantsAreAccepted() throws IOException {
        List<String> blockList =
                ignore(
                        List.of(
                                item(
                                        quoted(PROTOBUF_JAVA),
                                        "    update-types:",
                                        "      - \"" + SEMVER_MAJOR + "\""),
                                item(quoted(PROTOC), FLOW_MAJOR)));
        List<String> singleQuoted =
                ignore(
                        List.of(
                                item("'" + PROTOBUF_JAVA + "'", FLOW_MAJOR),
                                item("'" + PROTOC + "'", "    update-types: ['" + SEMVER_MAJOR + "']")));
        List<String> unquoted =
                ignore(
                        List.of(
                                item(PROTOBUF_JAVA, "    update-types: [" + SEMVER_MAJOR + "]"),
                                item(PROTOC, "    update-types:", "      - " + SEMVER_MAJOR)));
        assertAll(
                accepts("block-list update-types", gradleProtobufIgnoreProblem(fixtureLines(blockList, List.of()))),
                accepts("single-quoted names", gradleProtobufIgnoreProblem(fixtureLines(singleQuoted, List.of()))),
                accepts("unquoted names", gradleProtobufIgnoreProblem(fixtureLines(unquoted, List.of()))));
    }

    @Test
    @DisplayName("fixture: a gradle entry without an ignore list is rejected, naming the missing ignore")
    void fixtureGradleEntryWithoutIgnoreIsRejected() throws IOException {
        List<String> lines = fixtureLines(List.of(), List.of());
        assertAll(rejects("gradle entry without ignore", gradleProtobufIgnoreProblem(lines), NO_IGNORE));
    }

    @Test
    @DisplayName("fixture: an ignore list without the protoc item is rejected, naming protoc and libprotobuf")
    void fixtureMissingProtocItemIsRejected() throws IOException {
        List<String> lines =
                fixtureLines(ignore(List.of(item(quoted(PROTOBUF_JAVA), FLOW_MAJOR))), List.of());
        Optional<String> problem = gradleProtobufIgnoreProblem(lines);
        assertAll(
                rejects("ignore list without protoc", problem, NO_ITEM + " '" + PROTOC + "'"),
                rejects("ignore list without protoc", problem, LIBPROTOBUF));
    }

    @Test
    @DisplayName("fixture: a wildcard item is rejected even when both exact items are present")
    void fixtureWildcardDependencyNameIsRejected() throws IOException {
        List<String> lines =
                fixtureLines(
                        ignore(
                                List.of(
                                        item(quoted(PROTOBUF_JAVA), FLOW_MAJOR),
                                        item(quoted(PROTOC), FLOW_MAJOR),
                                        item(quoted("com.google.protobuf:*"), FLOW_MAJOR))),
                        List.of());
        Optional<String> problem = gradleProtobufIgnoreProblem(lines);
        assertAll(
                rejects("wildcard item", problem, WILDCARD),
                rejects("wildcard item", problem, PROTOBUF_PLUGIN));
    }

    @Test
    @DisplayName("fixture: update-types widened beyond semver-major is rejected")
    void fixtureWidenedUpdateTypesAreRejected() throws IOException {
        String widened = "    update-types: [\"" + SEMVER_MAJOR + "\", \"" + SEMVER_MINOR + "\"]";
        List<String> lines =
                fixtureLines(
                        ignore(
                                List.of(
                                        item(quoted(PROTOBUF_JAVA), widened),
                                        item(quoted(PROTOC), FLOW_MAJOR))),
                        List.of());
        Optional<String> problem = gradleProtobufIgnoreProblem(lines);
        assertAll(
                rejects("widened update-types", problem, UPDATE_TYPES_PROBLEM),
                rejects("widened update-types", problem, PROTOBUF_JAVA));
    }

    @Test
    @DisplayName("fixture: a versions range on a protobuf item is rejected")
    void fixtureVersionsRangeOnTheItemIsRejected() throws IOException {
        List<String> lines =
                fixtureLines(
                        ignore(
                                List.of(
                                        item(
                                                quoted(PROTOBUF_JAVA),
                                                FLOW_MAJOR,
                                                "    " + VERSIONS_KEY + ": [\">= 4\"]"),
                                        item(quoted(PROTOC), FLOW_MAJOR))),
                        List.of());
        assertAll(rejects("versions range", gradleProtobufIgnoreProblem(lines), VERSIONS_PROBLEM));
    }

    @Test
    @DisplayName("fixture: both items placed only in the github-actions entry are rejected by both rules")
    void fixtureIgnoreInGithubActionsEntryIsRejected() throws IOException {
        List<String> lines = fixtureLines(List.of(), canonicalIgnore());
        assertAll(
                rejects("ignore only in github-actions", gradleProtobufIgnoreProblem(lines), NO_IGNORE),
                rejects(
                        "ignore only in github-actions",
                        githubActionsIgnoreProblem(lines),
                        GRADLE_ENTRY_BELONGS));
    }

    @Test
    @DisplayName("fixture: an ignore that is only present as commented lines is rejected")
    void fixtureCommentedOutIgnoreIsRejected() throws IOException {
        List<String> commented = new ArrayList<>();
        canonicalIgnore().forEach(line -> commented.add("# " + line));
        List<String> lines = fixtureLines(commented, List.of());
        assertAll(rejects("commented-out ignore", gradleProtobufIgnoreProblem(lines), NO_IGNORE));
    }

    @Test
    @DisplayName(
            "fixture: an ignore key with a trailing comment is read as the ignore list by both rules"
                    + " (SWP-03)")
    void fixtureIgnoreKeyWithTrailingCommentIsRecognised() throws IOException {
        List<String> commentedKey = canonicalIgnore();
        commentedKey.set(0, IGNORE_KEY + "   # FORK: see docs/FORK-DELTA.md");
        List<String> gradleCommented = fixtureLines(commentedKey, List.of());
        List<String> actionsCommented = fixtureLines(List.of(), commentedKey);
        assertAll(
                accepts("gradle `ignore:` with trailing comment", gradleProtobufIgnoreProblem(gradleCommented)),
                rejects(
                        "github-actions `ignore:` with trailing comment",
                        githubActionsIgnoreProblem(actionsCommented),
                        GRADLE_ENTRY_BELONGS),
                rejects(
                        "gradle entry lacking the list when only github-actions has `ignore:  # note`",
                        gradleProtobufIgnoreProblem(actionsCommented),
                        NO_IGNORE));
    }
}
