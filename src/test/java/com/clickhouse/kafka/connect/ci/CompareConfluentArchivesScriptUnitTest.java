package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free pin for the archive compare: it runs {@code .github/scripts/compare_confluent_archives.py}
 * with {@code python3 -I} on synthetic archives built with {@code ZipOutputStream} in a {@code
 * TempDir}, and asserts the exit codes 0, 1 and 2 and the report lines. It fails rather than skips
 * when {@code python3} cannot start, because the release runners and the development machine both
 * have it.
 *
 * <p>The child's output is read as UTF-8 explicitly, because the test JVM runs with {@code
 * file.encoding=windows-1252}. Deliberately has no superclass - deriving from {@code ClickHouseBase}
 * would inherit its {@code clickhouse} tag and drop this class out of {@code testUnit}.
 */
class CompareConfluentArchivesScriptUnitTest {
    private static final Path SCRIPT =
            Path.of(".github", "scripts", "compare_confluent_archives.py");
    private static final String PYTHON = "python3";
    private static final long TIMEOUT_SECONDS = 60;
    private static final String TOP = "clickhouse-kafka-connect-v1/";
    private static final String INNER_PREFIX = TOP + "lib/inner";
    private static final String INNER_SUFFIX = "-confluent.jar";
    private static final String OUTER_MANIFEST = "manifest.json";
    private static final String MANIFEST_MF = "META-INF/MANIFEST.MF";
    private static final String CLASS_A = "a/A.class";
    private static final String CLASS_B = "b/B.class";
    private static final String CLASS_C = "c/C.class";
    private static final String DEEP_CLASS = "a/deep/A.class";
    private static final String NESTED_JAR = "lib/dep.jar";
    private static final String REASON = "expected change";
    private static final String PASS = "RESULT: PASS";
    private static final String FAIL = "RESULT: FAIL";
    private static final String UNALLOWED = "UNALLOWED ";
    private static final String ERROR_PREFIX = "ERROR:";
    private static final String USAGE = "usage:";
    private static final String STARS = "stars";
    private static final String LITERAL_PREFIX = "literal path prefix";
    private static final Path ALLOW_LISTS = Path.of(".planning", "scripts");
    private static final String ALLOW_LIST_PREFIX = "confluent-archive-allowlist-";
    private static final String COMMENT_ONLY = "# no differences are expected\n";
    private static final String TOTALS_IDENTICAL =
            "totals files-a 3 files-b 3 only-a 0 only-b 0 changed 0 same 3";

    private static final Pattern UNSAFE_REPORT_CHARS =
            Pattern.compile("[\\x00-\\x1f\\x7f-\\x9f\\u2028\\u2029]");
    private static final String FORGED_ERROR = "::error title=forged::boom";
    private static final String SAFE_LINE_DRIVER =
            String.join(
                    "\n",
                    "import importlib.util, sys",
                    "spec = importlib.util.spec_from_file_location('compare', sys.argv[1])",
                    "module = importlib.util.module_from_spec(spec)",
                    "spec.loader.exec_module(module)",
                    "for text in ['::error::x', '  ::add-mask::x', '##[error]x', ' ##[group]x',",
                    "             'ONLY-A a\\\\b', 'ONLY-A plain']:",
                    "    print(module.safe_line(text))");

    @TempDir Path temp;

    private record Run(int exitCode, String stdout, String stderr) {
        boolean hasLine(String prefix) {
            return stdout.lines().anyMatch(line -> line.startsWith(prefix));
        }

        String describe() {
            return "stdout:\n" + stdout + "stderr:\n" + stderr;
        }
    }

    private Run run(String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(PYTHON, "-I", SCRIPT.toString()));
        command.addAll(List.of(arguments));
        Path out = Files.createTempFile(temp, "stdout", ".txt");
        Path err = Files.createTempFile(temp, "stderr", ".txt");
        ProcessBuilder builder =
                new ProcessBuilder(command).redirectOutput(out.toFile()).redirectError(err.toFile());
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new AssertionError("python3 is required to run the archive compare script", e);
        }
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("The compare script did not finish within " + TIMEOUT_SECONDS + " seconds");
        }
        return new Run(
                process.exitValue(),
                Files.readString(out, StandardCharsets.UTF_8),
                Files.readString(err, StandardCharsets.UTF_8));
    }

    private static String manifestMf(String manifestVersion, String implementationVersion) {
        return "Manifest-Version: "
                + manifestVersion
                + "\nImplementation-Version: "
                + implementationVersion
                + "\n";
    }

    private static String manifestJson(String releaseDate, String version, String name) {
        return "{\n  \"release_date\": \""
                + releaseDate
                + "\",\n  \"version\": \""
                + version
                + "\",\n  \"name\": \""
                + name
                + "\"\n}\n";
    }

    private static Map<String, String> baseInner() {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(MANIFEST_MF, manifestMf("1.0", "v1"));
        entries.put(CLASS_A, "A1");
        entries.put(CLASS_B, "B1");
        return entries;
    }

    private static Map<String, String> baseWith(String name, String content) {
        Map<String, String> entries = baseInner();
        entries.put(name, content);
        return entries;
    }

    private static Map<String, String> baseWithout(String name) {
        Map<String, String> entries = baseInner();
        entries.remove(name);
        return entries;
    }

    private static void put(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    private static byte[] innerJar(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                put(zip, entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8));
            }
        }
        return bytes.toByteArray();
    }

    private Path archive(
            String fileName,
            Map<String, String> innerEntries,
            Map<String, String> outerEntries,
            int innerJarCount)
            throws IOException {
        Map<String, String> outer = new LinkedHashMap<>();
        outer.put(OUTER_MANIFEST, manifestJson("2026-01-01", "v1", "clickhouse-kafka-connect"));
        outer.putAll(outerEntries);
        Path archive = temp.resolve(fileName);
        byte[] inner = innerJar(innerEntries);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (int i = 0; i < innerJarCount; i++) {
                put(zip, INNER_PREFIX + i + INNER_SUFFIX, inner);
            }
            for (Map.Entry<String, String> entry : outer.entrySet()) {
                put(zip, TOP + entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8));
            }
        }
        return archive;
    }

    private Path archive(String fileName, Map<String, String> innerEntries) throws IOException {
        return archive(fileName, innerEntries, Map.of(), 1);
    }

    private Path allowList(String text) throws IOException {
        return Files.writeString(temp.resolve("allow.txt"), text, StandardCharsets.UTF_8);
    }

    private static String row(String glob, String reason) {
        return glob + "\t" + reason + "\n";
    }

    private Run compare(Path a, Path b, String allowListText) throws IOException, InterruptedException {
        return run(a.toString(), b.toString(), allowList(allowListText).toString());
    }

    private Run compare(Map<String, String> a, Map<String, String> b, String allowListText)
            throws IOException, InterruptedException {
        return compare(archive("a.zip", a), archive("b.zip", b), allowListText);
    }

    private static void assertExit(Run run, int exitCode) {
        assertEquals(exitCode, run.exitCode(), run::describe);
    }

    private static void assertHasLine(Run run, String prefix) {
        assertTrue(run.hasLine(prefix), () -> "no line starts with '" + prefix + "'\n" + run.describe());
    }

    private static void assertUnusableInput(Run run, String stderrPart) {
        assertExit(run, 2);
        assertEquals("", run.stdout(), run::describe);
        assertTrue(
                run.stderr().startsWith(ERROR_PREFIX) || run.stderr().contains(USAGE), run::describe);
        if (stderrPart != null) {
            assertTrue(run.stderr().contains(stderrPart), run::describe);
        }
    }

    private static void assertPass(Run run) {
        assertExit(run, 0);
        assertHasLine(run, PASS);
    }

    private static void assertFail(Run run) {
        assertExit(run, 1);
        assertHasLine(run, FAIL);
    }

    // exit 0

    @Test
    @DisplayName("CIH-05: identical archives with a comment-only allow-list pass and report the totals")
    void identicalArchivesPassWithACommentOnlyAllowList() throws Exception {
        Run run = compare(baseInner(), baseInner(), COMMENT_ONLY);
        assertPass(run);
        assertHasLine(run, TOTALS_IDENTICAL);
    }

    @Test
    @DisplayName("CIH-05: a changed entry named by an exact row passes and is still reported")
    void aChangedEntryNamedByAnExactRowPasses() throws Exception {
        Run run = compare(baseInner(), baseWith(CLASS_A, "A2"), row(CLASS_A, REASON));
        assertPass(run);
        assertHasLine(run, "CHANGED " + CLASS_A);
        assertFalse(run.hasLine(UNALLOWED), run::describe);
    }

    @Test
    @DisplayName("CIH-05: a star in a row crosses slashes")
    void aStarRowCoversAChangeAcrossSlashes() throws Exception {
        Run run =
                compare(
                        baseWith(DEEP_CLASS, "D1"),
                        baseWith(DEEP_CLASS, "D2"),
                        row("a/*", REASON));
        assertPass(run);
        assertHasLine(run, "CHANGED " + DEEP_CLASS);
    }

    @Test
    @DisplayName("CIH-05: manifest.json differing only in release_date and version lines passes")
    void manifestJsonDifferingOnlyInReleaseDateAndVersionPasses() throws Exception {
        Path a =
                archive(
                        "a.zip",
                        baseInner(),
                        Map.of(OUTER_MANIFEST, manifestJson("2026-01-01", "v1", "connector")),
                        1);
        Path b =
                archive(
                        "b.zip",
                        baseInner(),
                        Map.of(OUTER_MANIFEST, manifestJson("2026-02-02", "v2", "connector")),
                        1);
        assertPass(compare(a, b, COMMENT_ONLY));
    }

    @Test
    @DisplayName("CIH-05: MANIFEST.MF differing only in Implementation-Version passes when named")
    void aManifestMfDifferingOnlyInImplementationVersionPasses() throws Exception {
        Run run =
                compare(
                        baseInner(),
                        baseWith(MANIFEST_MF, manifestMf("1.0", "v2")),
                        row(MANIFEST_MF, REASON));
        assertPass(run);
        assertHasLine(run, "MANIFEST.MF: differs only in Implementation-Version");
    }

    @Test
    @DisplayName("CIH-05: comments, blank lines and one matching row in an allow-list pass")
    void commentsBlankLinesAndOneMatchingRowPass() throws Exception {
        String allow = "# header\n\n   \n" + row(CLASS_A, REASON) + "# trailing comment\n";
        assertPass(compare(baseInner(), baseWith(CLASS_A, "A2"), allow));
    }

    // exit 1

    @Test
    @DisplayName("CIH-05: a changed entry with an empty allow-list is UNALLOWED CHANGED")
    void aChangedEntryWithAnEmptyAllowListFails() throws Exception {
        Run run = compare(baseInner(), baseWith(CLASS_A, "A2"), "");
        assertFail(run);
        assertHasLine(run, UNALLOWED + "CHANGED " + CLASS_A);
    }

    @Test
    @DisplayName("CIH-05: an entry only in the candidate archive is UNALLOWED ONLY-B")
    void anEntryOnlyInTheCandidateFails() throws Exception {
        Run run = compare(baseInner(), baseWith(CLASS_C, "C1"), "");
        assertFail(run);
        assertHasLine(run, UNALLOWED + "ONLY-B " + CLASS_C);
    }

    @Test
    @DisplayName("CIH-05: an entry only in the baseline archive is UNALLOWED ONLY-A")
    void anEntryOnlyInTheBaselineFails() throws Exception {
        Run run = compare(baseInner(), baseWithout(CLASS_B), "");
        assertFail(run);
        assertHasLine(run, UNALLOWED + "ONLY-A " + CLASS_B);
    }

    @Test
    @DisplayName("CIH-05: a row that names no difference is a STALE-ROW and fails")
    void aRowThatNamesNoDifferenceIsStale() throws Exception {
        Run run = compare(baseInner(), baseInner(), row("zz/*", REASON));
        assertFail(run);
        assertHasLine(run, "STALE-ROW zz/*");
    }

    @Test
    @DisplayName("CIH-05: MANIFEST.MF differing beyond Implementation-Version fails even when named")
    void aManifestMfDifferingBeyondImplementationVersionFails() throws Exception {
        Run run =
                compare(
                        baseInner(),
                        baseWith(MANIFEST_MF, manifestMf("2.0", "v1")),
                        row(MANIFEST_MF, REASON));
        assertFail(run);
        assertTrue(run.stdout().lines().anyMatch("MANIFEST.MF: differs"::equals), run::describe);
    }

    @Test
    @DisplayName("CIH-05: a nested jar in the candidate fails even when named")
    void aNestedJarInTheCandidateFails() throws Exception {
        Run run = compare(baseInner(), baseWith(NESTED_JAR, "J1"), row(NESTED_JAR, REASON));
        assertFail(run);
        assertHasLine(run, "nested jars in b: 1");
    }

    @Test
    @DisplayName("CIH-05: manifest.json differing in its name line is UNALLOWED CHANGED")
    void manifestJsonDifferingInItsNameLineFails() throws Exception {
        Path a =
                archive(
                        "a.zip",
                        baseInner(),
                        Map.of(OUTER_MANIFEST, manifestJson("2026-01-01", "v1", "one")),
                        1);
        Path b =
                archive(
                        "b.zip",
                        baseInner(),
                        Map.of(OUTER_MANIFEST, manifestJson("2026-01-01", "v1", "two")),
                        1);
        Run run = compare(a, b, "");
        assertFail(run);
        assertHasLine(run, UNALLOWED + "CHANGED outer/" + OUTER_MANIFEST);
    }

    @Test
    @DisplayName("CIH-05: one allowed and one unallowed difference together fail on the unallowed one")
    void aMixedAllowedAndUnallowedPairFails() throws Exception {
        Map<String, String> candidate = baseWith(CLASS_A, "A2");
        candidate.put(CLASS_B, "B2");
        Run run = compare(baseInner(), candidate, row(CLASS_A, REASON));
        assertFail(run);
        assertHasLine(run, UNALLOWED + "CHANGED " + CLASS_B);
        assertFalse(
                run.stdout().lines().anyMatch(l -> l.startsWith(UNALLOWED) && l.contains(CLASS_A)),
                run::describe);
    }

    // exit 2

    @Test
    @DisplayName("CIH-05: no argument is a usage error")
    void noArgumentIsAUsageError() throws Exception {
        assertUnusableInput(run(), USAGE);
    }

    @Test
    @DisplayName("CIH-05: four arguments are a usage error")
    void fourArgumentsAreAUsageError() throws Exception {
        Path a = archive("a.zip", baseInner());
        Path b = archive("b.zip", baseInner());
        assertUnusableInput(
                run(a.toString(), b.toString(), allowList(COMMENT_ONLY).toString(), "extra"), USAGE);
    }

    @Test
    @DisplayName("CIH-05: a missing allow-list file is unusable input")
    void aMissingAllowListFileIsUnusableInput() throws Exception {
        Path a = archive("a.zip", baseInner());
        Path b = archive("b.zip", baseInner());
        assertUnusableInput(run(a.toString(), b.toString(), temp.resolve("absent.txt").toString()), null);
    }

    @Test
    @DisplayName("CIH-05: an allow-list row without a TAB is unusable input")
    void anAllowListRowWithoutATabIsUnusableInput() throws Exception {
        assertUnusableInput(
                compare(baseInner(), baseInner(), CLASS_A + " no tab separates the reason\n"),
                "reason");
    }

    @Test
    @DisplayName("CIH-05: an allow-list row with an empty reason is unusable input")
    void anAllowListRowWithAnEmptyReasonIsUnusableInput() throws Exception {
        assertUnusableInput(compare(baseInner(), baseInner(), CLASS_A + "\t\n"), "reason");
    }

    @Test
    @DisplayName("CIH-05: a row of one star is refused as a catch-all")
    void aRowOfOneStarIsRefused() throws Exception {
        assertUnusableInput(compare(baseInner(), baseInner(), row("*", REASON)), STARS);
    }

    @Test
    @DisplayName("CIH-05: a row of two stars is refused as a catch-all")
    void aRowOfTwoStarsIsRefused() throws Exception {
        assertUnusableInput(compare(baseInner(), baseInner(), row("**", REASON)), STARS);
    }

    @Test
    @DisplayName("CIH-05: a baseline archive that is not a zip is unusable input")
    void aBaselineThatIsNotAZipIsUnusableInput() throws Exception {
        Path notZip = Files.writeString(temp.resolve("a.txt"), "this is not a zip file");
        assertUnusableInput(compare(notZip, archive("b.zip", baseInner()), COMMENT_ONLY), null);
    }

    @Test
    @DisplayName("CIH-05: a candidate archive that is not a zip is unusable input")
    void aCandidateThatIsNotAZipIsUnusableInput() throws Exception {
        Path notZip = Files.writeString(temp.resolve("b.txt"), "this is not a zip file");
        assertUnusableInput(compare(archive("a.zip", baseInner()), notZip, COMMENT_ONLY), null);
    }

    @Test
    @DisplayName("CIH-05: a baseline archive that does not exist is unusable input")
    void aMissingBaselineArchiveIsUnusableInput() throws Exception {
        Path absent = temp.resolve("absent.zip");
        assertUnusableInput(compare(absent, archive("b.zip", baseInner()), COMMENT_ONLY), null);
    }

    @Test
    @DisplayName("CIH-05: an outer archive without a lib confluent jar is unusable input")
    void anArchiveWithoutAnInnerJarIsUnusableInput() throws Exception {
        Path none = archive("a.zip", baseInner(), Map.of(), 0);
        assertUnusableInput(compare(none, archive("b.zip", baseInner()), COMMENT_ONLY), "exactly one");
    }

    @Test
    @DisplayName("CIH-05: an outer archive with two lib confluent jars is unusable input")
    void anArchiveWithTwoInnerJarsIsUnusableInput() throws Exception {
        Path two = archive("b.zip", baseInner(), Map.of(), 2);
        assertUnusableInput(compare(archive("a.zip", baseInner()), two, COMMENT_ONLY), "exactly one");
    }

    @Test
    @DisplayName("CIH-05: an inner jar with no file entry is unusable input")
    void anInnerJarWithNoFileEntryIsUnusableInput() throws Exception {
        Path empty = archive("a.zip", Map.of("a/", ""));
        assertUnusableInput(
                compare(empty, archive("b.zip", baseInner()), COMMENT_ONLY), "zero file entries");
    }

    @ParameterizedTest(name = "WR-02: the row \"{0}\" can match every path and is refused")
    @ValueSource(
            strings = {"*?", "**/*", "*/*", "[!x]*", "?*", "[a-z]/*", "*.class", "*.*", "*/A.class"})
    void aRowThatStartsWithAWildcardIsRefused(String glob) throws Exception {
        assertUnusableInput(compare(baseInner(), baseInner(), row(glob, REASON)), LITERAL_PREFIX);
    }

    @ParameterizedTest(name = "WR-02: the row \"{0}\" starts with a literal and still excuses the changed class")
    @ValueSource(
            strings = {"a/A.class", "a/*", "a/*.class", "a/?.class", "a/[A].class", "a/[!x].class", "a*"})
    void aRowWithALiteralPrefixIsStillAccepted(String glob) throws Exception {
        Run run = compare(baseInner(), baseWith(CLASS_A, "A2"), row(glob, REASON));
        assertPass(run);
        assertHasLine(run, "CHANGED " + CLASS_A);
    }

    @Test
    @DisplayName("WR-02: every allow-list shipped under .planning/scripts loads (a stale row is exit 1, never exit 2)")
    void everyShippedAllowListStillLoads() throws Exception {
        List<Path> lists;
        try (Stream<Path> files = Files.list(ALLOW_LISTS)) {
            lists =
                    files.filter(f -> f.getFileName().toString().startsWith(ALLOW_LIST_PREFIX))
                            .sorted()
                            .toList();
        }
        assertTrue(lists.size() >= 3, () -> "the shipped allow-lists were not found: " + lists);
        Path a = archive("a.zip", baseInner());
        Path b = archive("b.zip", baseInner());
        for (Path list : lists) {
            Run run = run(a.toString(), b.toString(), list.toString());
            assertFail(run);
            assertEquals("", run.stderr(), () -> list + " must load\n" + run.describe());
            assertHasLine(run, "STALE-ROW ");
        }
    }

    // UF-37-1: entry names are untrusted text that the workflow prints to the job log

    private static void assertLogSafe(Run run) {
        for (String line : run.stdout().split("\n", -1)) {
            assertFalse(line.startsWith("::"), () -> "a report line starts a workflow command\n" + run.describe());
            assertFalse(line.startsWith("##["), () -> "a report line starts a log command\n" + run.describe());
        }
        assertFalse(
                UNSAFE_REPORT_CHARS.matcher(run.stdout().replace("\n", "")).find(),
                () -> "the report holds a raw control character\n" + run.describe());
    }

    private static long linesStartingWith(Run run, String prefix) {
        return Arrays.stream(run.stdout().split("\n", -1))
                .filter(line -> line.startsWith(prefix))
                .count();
    }

    @ParameterizedTest(
            name = "UF-37-1: an added entry named \"{0}\" cannot put a workflow command at the start of a report line")
    @ValueSource(
            strings = {
                "a/Evil.class\n" + FORGED_ERROR,
                "a/Evil.class\n::add-mask::secret",
                "a/Evil.class\n::stop-commands::token",
                "a/Evil.class\n##[error]forged",
                "a/Evil.class\r\n::error::crlf",
                "::error::leading"
            })
    void anEntryNameCannotForgeAWorkflowCommand(String hostileName) throws Exception {
        Run run = compare(baseInner(), baseWith(hostileName, "X"), COMMENT_ONLY);

        assertFail(run);
        assertLogSafe(run);
        assertEquals(1, linesStartingWith(run, "ONLY-B "), run::describe);
        assertEquals(1, linesStartingWith(run, UNALLOWED), run::describe);
    }

    @Test
    @DisplayName("UF-37-1: the forged line is still visible in the report, as escaped text on the entry's own line")
    void theForgedTextStaysVisibleAsEscapedText() throws Exception {
        Run run = compare(baseInner(), baseWith("a/Evil.class\n" + FORGED_ERROR, "X"), COMMENT_ONLY);

        assertFail(run);
        assertTrue(run.hasLine("ONLY-B a/Evil.class\\n" + FORGED_ERROR + " "), run::describe);
        assertTrue(run.hasLine(UNALLOWED + "ONLY-B a/Evil.class\\n" + FORGED_ERROR), run::describe);
    }

    @Test
    @DisplayName("UF-37-1: control characters, DEL, C1, U+2028 and U+2029 in an entry name are escaped and the rest is untouched")
    void controlCharactersInAnEntryNameAreEscaped() throws Exception {
        String hostileName = "a/ctl\u001b[31m\r\u0007\u007f\u0085\u2028\u2029\tdir\\x.class";
        Run run = compare(baseInner(), baseWith(hostileName, "X"), COMMENT_ONLY);

        assertFail(run);
        assertLogSafe(run);
        assertTrue(
                run.hasLine(
                        UNALLOWED
                                + "ONLY-B a/ctl\\x1b[31m\\r\\x07\\x7f\\x85\\u2028\\u2029\\tdir\\\\x.class"),
                run::describe);
    }

    @Test
    @DisplayName("UF-37-1: a hostile name that an allow-list row covers still passes, the changed entry is reported escaped")
    void aHostileNameCoveredByARowStillPasses() throws Exception {
        String hostileName = "a/ok\n::add-mask::secret.class";
        Run run =
                compare(
                        baseWith(hostileName, "1"),
                        baseWith(hostileName, "2"),
                        row("a/ok*", REASON));

        assertPass(run);
        assertLogSafe(run);
        assertEquals(1, linesStartingWith(run, "CHANGED "), run::describe);
        assertFalse(run.hasLine(UNALLOWED), run::describe);
    }

    @Test
    @DisplayName("UF-37-1: an allow-list glob that holds a control character is printed escaped on its STALE-ROW line")
    void aStaleRowGlobWithAControlCharacterIsEscaped() throws Exception {
        Run run = compare(baseInner(), baseInner(), row("zz/\u001b[31mNothing.class", REASON));

        assertFail(run);
        assertLogSafe(run);
        assertTrue(run.hasLine("STALE-ROW zz/\\x1b[31mNothing.class"), run::describe);
    }

    @Test
    @DisplayName("UF-37-1: safe_line escapes a line that would still begin with a workflow command, and doubles a backslash")
    void safeLineNeutralisesACommandStart() throws Exception {
        Process process =
                new ProcessBuilder(PYTHON, "-I", "-B", "-c", SAFE_LINE_DRIVER, SCRIPT.toString())
                        .redirectErrorStream(true)
                        .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(0, process.exitValue(), output);
        assertEquals(
                List.of(
                        "\\::error::x",
                        "\\  ::add-mask::x",
                        "\\##[error]x",
                        "\\ ##[group]x",
                        "ONLY-A a\\\\b",
                        "ONLY-A plain"),
                output.lines().toList());
    }
}
