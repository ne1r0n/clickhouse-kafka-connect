package com.clickhouse.kafka.connect.ci;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free table test of {@code .github/scripts/bump_version.py}: it runs the script with {@code
 * python3 -I} on fixture {@code VERSION} and anchors files in a temporary directory and reads the
 * exit code, both output streams and both files back. It fails rather than skips when {@code
 * python3} cannot start, because the workflow runner and the development machine both have it.
 *
 * <p>The child's output is read as UTF-8 explicitly, because the test JVM runs with {@code
 * file.encoding=windows-1252}. Deliberately extends nothing - extending {@code ClickHouseBase} would
 * inherit its {@code clickhouse} tag and drop this class out of {@code testUnit}.
 */
class BumpVersionScriptUnitTest {
    private static final Path SCRIPT = Path.of(".github", "scripts", "bump_version.py");
    private static final String PYTHON = "python3";
    private static final long TIMEOUT_SECONDS = 60;
    private static final String TAB = "\t";
    private static final String ERROR_PREFIX = "ERROR:";
    private static final String USAGE_PREFIX = "usage:";
    private static final String NULL_TEXT = "null";
    private static final String BUMP_GG = "gg";
    private static final String BUMP_PATCH = "patch";
    private static final String BUMP_MINOR = "minor";
    private static final String BUMP_MAJOR = "major";
    private static final String VERSION_FILE_NAME = "VERSION";
    private static final String ANCHORS_FILE_NAME = "fork-anchors.txt";
    private static final String GG5 = "v1.7.0_gg5";
    private static final String GG6 = "v1.7.0_gg6";
    private static final String GG4 = "v1.7.0_gg4";
    private static final String EMPTY_LABEL = "<empty>";
    private static final String ANCHORS_HEADER = "# fixture anchors\n";
    private static final String TEMP_PREFIX = ".bump-";
    private static final String KEPT_IN = "its original is kept in ";

    /**
     * Runs the script's own functions with {@code os.replace} made to fail for chosen targets, because a
     * failing second rename cannot be produced through the command line: staging and replacing need the
     * same directory permission. It prints one {@code replaced NAME} line per rename that went through,
     * so a test can see that the first file really was replaced before the failure.
     */
    private static final String DRIVER =
            """
            import importlib.util
            import os
            import sys

            script, mode, first, second = sys.argv[1:5]
            spec = importlib.util.spec_from_file_location("bump_version", script)
            module = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(module)
            real_replace = os.replace
            calls_to_first = []

            def replace(source, target, *args, **kwargs):
                if target == second and mode != "staging-write-error":
                    raise OSError("injected failure for " + os.path.basename(target))
                if target == first and mode == "restore-fails":
                    calls_to_first.append(source)
                    if len(calls_to_first) == 2:
                        raise OSError("injected restore failure")
                real_replace(source, target, *args, **kwargs)
                print("replaced", os.path.basename(target))

            os.replace = replace
            try:
                if mode == "main":
                    print("EXIT", module.main(["gg", "--version-file", second, "--anchors-file", first]))
                else:
                    text = chr(0xD800) if mode == "staging-write-error" else "NEW\\n"
                    module.write_both((first, "NEW\\n"), (second, text))
                    print("NO-ERROR")
            except module.BumpError as exc:
                print("BUMP-ERROR", exc)
            """;
    private static final String EXAMPLE_ROW = row("src/main/java/Example.java", "foo", "1", "X-01");
    private static final String GRADLE_ROW = row("build.gradle.kts", "windows-1252", "2", "BLD-05");

    @TempDir Path temp;

    private record Run(int exitCode, String stdout, String stderr) {}

    private static String row(String path, String pattern, String count, String backsId) {
        return String.join(TAB, path, pattern, count, backsId) + "\n";
    }

    private static String versionRow(String pattern) {
        return row(VERSION_FILE_NAME, pattern, "1", "BLD-07");
    }

    private static String anchorsFor(String versionPattern) {
        return ANCHORS_HEADER + EXAMPLE_ROW + versionRow(versionPattern) + GRADLE_ROW;
    }

    private static String printable(String text) {
        if (text.isEmpty()) {
            return EMPTY_LABEL;
        }
        StringBuilder shown = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                shown.append("\\n");
            } else if (c == '\r') {
                shown.append("\\r");
            } else if (c <= ' ' || c > '~') {
                shown.append(String.format("\\u%04x", (int) c));
            } else {
                shown.append(c);
            }
        }
        return shown.toString();
    }

    private Path versionFile() {
        return temp.resolve(VERSION_FILE_NAME);
    }

    private Path anchorsFile() {
        return temp.resolve(ANCHORS_FILE_NAME);
    }

    private static void write(Path file, String text) throws IOException {
        Files.writeString(file, text, UTF_8);
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, UTF_8);
    }

    private Run run(String bump, Path versionFile, Path anchorsFile)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(PYTHON, "-I", SCRIPT.toString()));
        if (bump != null) {
            command.add(bump);
        }
        command.addAll(
                List.of("--version-file", versionFile.toString(), "--anchors-file", anchorsFile.toString()));
        Path out = Files.createTempFile(temp, "stdout", ".txt");
        Path err = Files.createTempFile(temp, "stderr", ".txt");
        ProcessBuilder builder =
                new ProcessBuilder(command).redirectOutput(out.toFile()).redirectError(err.toFile());
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new AssertionError("python3 is required to run the version bump script", e);
        }
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("The bump script did not finish within " + TIMEOUT_SECONDS + " seconds");
        }
        return new Run(process.exitValue(), read(out), read(err));
    }

    /** Writes both fixture files, runs the script with {@code gg} and expects a refusal that writes nothing. */
    private Run refusedWithGg(String versionText, String anchorsText)
            throws IOException, InterruptedException {
        write(versionFile(), versionText);
        write(anchorsFile(), anchorsText);

        Run run = run(BUMP_GG, versionFile(), anchorsFile());

        assertEquals(1, run.exitCode(), run.stderr());
        assertEquals("", run.stdout());
        assertTrue(run.stderr().startsWith(ERROR_PREFIX), run.stderr());
        assertEquals(versionText, read(versionFile()));
        assertEquals(anchorsText, read(anchorsFile()));
        return run;
    }

    private void assertUsageErrorWritesNothing(String bump) throws IOException, InterruptedException {
        String versionText = GG5 + "\n";
        String anchorsText = anchorsFor(GG5);
        write(versionFile(), versionText);
        write(anchorsFile(), anchorsText);

        Run run = run(bump, versionFile(), anchorsFile());

        assertEquals(2, run.exitCode(), run.stderr());
        assertEquals("", run.stdout());
        assertTrue(run.stderr().startsWith(USAGE_PREFIX), run.stderr());
        assertEquals(versionText, read(versionFile()));
        assertEquals(anchorsText, read(anchorsFile()));
    }

    private static Arguments accepted(String oldVersion, String bump, String expected) {
        return Arguments.of(oldVersion, bump, expected, oldVersion + "\n");
    }

    static Stream<Arguments> acceptedBumps() {
        return Stream.of(
                accepted(GG5, BUMP_GG, GG6),
                accepted("v1.7.0_gg9", BUMP_GG, "v1.7.0_gg10"),
                accepted("v1.7.0_gg99", BUMP_GG, "v1.7.0_gg100"),
                accepted("v1.7.0", BUMP_GG, "v1.7.0_gg1"),
                accepted(GG5, BUMP_PATCH, "v1.7.1_gg1"),
                accepted(GG5, BUMP_MINOR, "v1.8.0_gg1"),
                accepted(GG5, BUMP_MAJOR, "v2.0.0_gg1"),
                accepted("v1.7.0", BUMP_PATCH, "v1.7.1_gg1"),
                accepted("v1.7.0", BUMP_MINOR, "v1.8.0_gg1"),
                accepted("v1.7.0", BUMP_MAJOR, "v2.0.0_gg1"),
                accepted("v1.9.9_gg2", BUMP_PATCH, "v1.9.10_gg1"),
                accepted("v0.0.0", BUMP_GG, "v0.0.0_gg1"),
                accepted("v01.02.03_gg4", BUMP_GG, "v1.2.3_gg5"),
                Arguments.of(GG5 + " without a final newline", BUMP_GG, GG6, GG5));
    }

    private static Arguments refused(String versionText, String bump) {
        return Arguments.of(printable(versionText), bump, versionText);
    }

    static Stream<Arguments> refusedVersionTexts() {
        Stream<Arguments> withGg =
                Stream.of(
                                "v1.7.0_gg0",
                                "1.7.0",
                                "v1.7",
                                "v1.7.0_gg",
                                "v1.7.0-rc1",
                                "vnull",
                                "",
                                "v\u0661.7.0",
                                GG5 + "\n",
                                GG5 + "\r",
                                "v1.7.0 ",
                                "v1.7.0_gg05",
                                "V1.7.0",
                                "v1.7.0_GG5")
                        .map(text -> refused(text, BUMP_GG));
        Stream<Arguments> withNumericBumps =
                Stream.of(BUMP_PATCH, BUMP_MINOR, BUMP_MAJOR).map(bump -> refused("vnull", bump));
        return Stream.concat(withGg, withNumericBumps);
    }

    static Stream<Arguments> unknownBumps() {
        return Stream.of("bogus", "", "GG", "Patch")
                .map(bump -> Arguments.of(bump.isEmpty() ? EMPTY_LABEL : bump, bump));
    }

    @Test
    @DisplayName("CIH-01: gg raises the counter and rewrites the VERSION row of the anchors file")
    void ggRaisesTheCounterAndRewritesTheVersionRow() throws IOException, InterruptedException {
        write(versionFile(), GG5 + "\n");
        write(anchorsFile(), anchorsFor(GG5));

        Run run = run(BUMP_GG, versionFile(), anchorsFile());

        assertEquals(0, run.exitCode(), run.stderr());
        assertEquals(GG6 + "\n", run.stdout());
        assertEquals(GG6 + "\n", read(versionFile()));
        assertEquals(anchorsFor(GG6), read(anchorsFile()));
        assertFalse(read(versionFile()).contains(NULL_TEXT));
        assertFalse(read(anchorsFile()).contains(NULL_TEXT));
        assertTrue(run.stderr().isEmpty(), run.stderr());
    }

    @Test
    @DisplayName("CIH-03: a VERSION row that does not match VERSION refuses and writes nothing")
    void aVersionRowThatDoesNotMatchVersionRefusesAndWritesNothing()
            throws IOException, InterruptedException {
        String versionText = GG5 + "\n";
        String anchorsText = anchorsFor(GG4);
        write(versionFile(), versionText);
        write(anchorsFile(), anchorsText);

        Run run = run(BUMP_GG, versionFile(), anchorsFile());

        assertEquals(1, run.exitCode(), run.stderr());
        assertEquals("", run.stdout());
        assertTrue(run.stderr().startsWith(ERROR_PREFIX), run.stderr());
        assertTrue(run.stderr().contains(GG4), run.stderr());
        assertTrue(run.stderr().contains(GG5), run.stderr());
        assertEquals(versionText, read(versionFile()));
        assertEquals(anchorsText, read(anchorsFile()));
    }

    @ParameterizedTest(name = "CIH-01: {0} with {1} gives {2}")
    @MethodSource("acceptedBumps")
    void acceptedBumpWritesTheNewVersionToBothFiles(
            String label, String bump, String expected, String versionFileText)
            throws IOException, InterruptedException {
        String oldVersion =
                versionFileText.endsWith("\n")
                        ? versionFileText.substring(0, versionFileText.length() - 1)
                        : versionFileText;
        write(versionFile(), versionFileText);
        write(anchorsFile(), anchorsFor(oldVersion));

        Run run = run(bump, versionFile(), anchorsFile());

        assertEquals(0, run.exitCode(), run.stderr());
        assertEquals(expected + "\n", run.stdout());
        assertEquals(expected + "\n", read(versionFile()));
        assertEquals(anchorsFor(expected), read(anchorsFile()));
        assertFalse(read(versionFile()).contains(NULL_TEXT));
        assertFalse(read(anchorsFile()).contains(NULL_TEXT));
    }

    @ParameterizedTest(name = "CIH-01: the VERSION text {0} with {1} is refused and nothing is written")
    @MethodSource("refusedVersionTexts")
    void refusedVersionTextWritesNothing(String shown, String bump, String versionText)
            throws IOException, InterruptedException {
        String versionFileText = versionText + "\n";
        String anchorsText = anchorsFor(versionText);
        write(versionFile(), versionFileText);
        write(anchorsFile(), anchorsText);

        Run run = run(bump, versionFile(), anchorsFile());

        assertEquals(1, run.exitCode(), run.stderr());
        assertEquals("", run.stdout());
        assertTrue(run.stderr().startsWith(ERROR_PREFIX), run.stderr());
        assertEquals(versionFileText, read(versionFile()));
        assertEquals(anchorsText, read(anchorsFile()));
    }

    @ParameterizedTest(name = "CIH-01: the bump {0} is a usage error and nothing is written")
    @MethodSource("unknownBumps")
    void unknownBumpIsAUsageErrorAndWritesNothing(String shown, String bump)
            throws IOException, InterruptedException {
        assertUsageErrorWritesNothing(bump);
    }

    @Test
    @DisplayName("CIH-01: a run without any bump argument is a usage error and nothing is written")
    void missingBumpArgumentIsAUsageErrorAndWritesNothing()
            throws IOException, InterruptedException {
        assertUsageErrorWritesNothing(null);
    }

    @Test
    @DisplayName("CIH-03: an anchors file without a VERSION row refuses and writes nothing")
    void anchorsFileWithoutAVersionRowRefuses() throws IOException, InterruptedException {
        Run run = refusedWithGg(GG5 + "\n", ANCHORS_HEADER + EXAMPLE_ROW + GRADLE_ROW);

        assertTrue(run.stderr().contains("found 0"), run.stderr());
    }

    @Test
    @DisplayName("CIH-03: an anchors file with two VERSION rows refuses and writes nothing")
    void anchorsFileWithTwoVersionRowsRefuses() throws IOException, InterruptedException {
        Run run = refusedWithGg(GG5 + "\n", anchorsFor(GG5) + versionRow(GG5));

        assertTrue(run.stderr().contains("found 2"), run.stderr());
    }

    @Test
    @DisplayName("CIH-03: a missing anchors file refuses and creates no anchors file")
    void missingAnchorsFileRefusesAndCreatesNothing() throws IOException, InterruptedException {
        String versionText = GG5 + "\n";
        write(versionFile(), versionText);

        Run run = run(BUMP_GG, versionFile(), anchorsFile());

        assertEquals(1, run.exitCode(), run.stderr());
        assertEquals("", run.stdout());
        assertTrue(run.stderr().startsWith(ERROR_PREFIX), run.stderr());
        assertEquals(versionText, read(versionFile()));
        assertFalse(Files.exists(anchorsFile()), "the script created an anchors file");
    }

    @Test
    @DisplayName("CIH-03: a missing VERSION file refuses and creates no VERSION file")
    void missingVersionFileRefusesAndCreatesNothing() throws IOException, InterruptedException {
        String anchorsText = anchorsFor(GG5);
        write(anchorsFile(), anchorsText);

        Run run = run(BUMP_GG, versionFile(), anchorsFile());

        assertEquals(1, run.exitCode(), run.stderr());
        assertEquals("", run.stdout());
        assertTrue(run.stderr().startsWith(ERROR_PREFIX), run.stderr());
        assertEquals(anchorsText, read(anchorsFile()));
        assertFalse(Files.exists(versionFile()), "the script created a VERSION file");
    }

    @Test
    @DisplayName("CIH-03: a VERSION row whose count is 2 refuses and writes nothing")
    void versionRowWithCountTwoRefuses() throws IOException, InterruptedException {
        refusedWithGg(
                GG5 + "\n",
                ANCHORS_HEADER + EXAMPLE_ROW + row(VERSION_FILE_NAME, GG5, "2", "BLD-07") + GRADLE_ROW);
    }

    @Test
    @DisplayName("CIH-03: a VERSION row with three fields refuses and writes nothing")
    void versionRowWithThreeFieldsRefuses() throws IOException, InterruptedException {
        refusedWithGg(
                GG5 + "\n",
                ANCHORS_HEADER
                        + EXAMPLE_ROW
                        + String.join(TAB, VERSION_FILE_NAME, GG5, "1")
                        + "\n"
                        + GRADLE_ROW);
    }

    @Test
    @DisplayName("CIH-03: a commented-out VERSION row is not a row and refuses")
    void commentedOutVersionRowRefuses() throws IOException, InterruptedException {
        Run run =
                refusedWithGg(
                        GG5 + "\n", ANCHORS_HEADER + EXAMPLE_ROW + "#" + versionRow(GG5) + GRADLE_ROW);

        assertTrue(run.stderr().contains("found 0"), run.stderr());
    }

    @Test
    @DisplayName("CIH-03: a row for VERSION.md is not the VERSION row and refuses")
    void decoyRowForAnotherPathRefuses() throws IOException, InterruptedException {
        Run run =
                refusedWithGg(
                        GG5 + "\n",
                        ANCHORS_HEADER + EXAMPLE_ROW + row("VERSION.md", GG5, "1", "BLD-07") + GRADLE_ROW);

        assertTrue(run.stderr().contains("found 0"), run.stderr());
    }

    @Test
    @DisplayName("CIH-03: a decoy row holding the same text is left byte-identical beside the real row")
    void decoyRowBesideTheRealRowIsLeftAlone() throws IOException, InterruptedException {
        String decoy = row("build.gradle.kts", GG5, "1", "BLD-09");
        write(versionFile(), GG5 + "\n");
        write(anchorsFile(), anchorsFor(GG5) + decoy);

        Run run = run(BUMP_GG, versionFile(), anchorsFile());

        assertEquals(0, run.exitCode(), run.stderr());
        assertEquals(GG6 + "\n", read(versionFile()));
        assertEquals(anchorsFor(GG6) + decoy, read(anchorsFile()));
    }

    @Test
    @DisplayName("CIH-03: an anchors file with CRLF line endings keeps every CRLF and changes only the pattern")
    void crlfAnchorsFileKeepsItsLineEndings() throws IOException, InterruptedException {
        write(versionFile(), GG5 + "\n");
        write(anchorsFile(), anchorsFor(GG5).replace("\n", "\r\n"));

        Run run = run(BUMP_GG, versionFile(), anchorsFile());

        assertEquals(0, run.exitCode(), run.stderr());
        assertEquals(GG6 + "\n", read(versionFile()));
        assertEquals(anchorsFor(GG6).replace("\n", "\r\n"), read(anchorsFile()));
    }

    private Run drive(String mode) throws IOException, InterruptedException {
        List<String> command =
                List.of(
                        PYTHON,
                        "-I",
                        "-B",
                        "-c",
                        DRIVER,
                        SCRIPT.toString(),
                        mode,
                        anchorsFile().toString(),
                        versionFile().toString());
        Path out = Files.createTempFile(temp, "stdout", ".txt");
        Path err = Files.createTempFile(temp, "stderr", ".txt");
        Process process =
                new ProcessBuilder(command)
                        .redirectOutput(out.toFile())
                        .redirectError(err.toFile())
                        .start();
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("The bump driver did not finish within " + TIMEOUT_SECONDS + " seconds");
        }
        return new Run(process.exitValue(), read(out), read(err));
    }

    private List<String> strayTempFiles() throws IOException {
        try (Stream<Path> files = Files.list(temp)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.startsWith(TEMP_PREFIX))
                    .toList();
        }
    }

    private void writeFixtures() throws IOException {
        write(versionFile(), GG5 + "\n");
        write(anchorsFile(), anchorsFor(GG5));
    }

    @Test
    @DisplayName("WR-04: a failing second replace puts the first file back, leaves both files unchanged and no temp file")
    void aFailingSecondReplaceLeavesBothFilesUnchanged() throws IOException, InterruptedException {
        writeFixtures();

        Run run = drive("second-replace-fails");

        assertEquals(0, run.exitCode(), run.stderr());
        assertTrue(run.stdout().contains("replaced " + ANCHORS_FILE_NAME), run.stdout());
        assertTrue(run.stdout().contains("BUMP-ERROR"), run.stdout());
        assertTrue(run.stdout().contains("injected failure"), run.stdout());
        assertEquals(GG5 + "\n", read(versionFile()));
        assertEquals(anchorsFor(GG5), read(anchorsFile()));
        assertEquals(List.of(), strayTempFiles());
    }

    @Test
    @DisplayName("WR-04: a failing second replace through the command line contract is exit 1 with an ERROR line and unchanged files")
    void aFailingSecondReplaceThroughMainIsARefusal() throws IOException, InterruptedException {
        writeFixtures();

        Run run = drive("main");

        assertEquals(0, run.exitCode(), run.stderr());
        assertTrue(run.stdout().contains("replaced " + ANCHORS_FILE_NAME), run.stdout());
        assertTrue(run.stdout().contains("EXIT 1"), run.stdout());
        assertTrue(run.stderr().startsWith(ERROR_PREFIX), run.stderr());
        assertEquals(GG5 + "\n", read(versionFile()));
        assertEquals(anchorsFor(GG5), read(anchorsFile()));
        assertEquals(List.of(), strayTempFiles());
    }

    @Test
    @DisplayName("WR-04: when the first file cannot be put back the error names the kept backup, which holds the original")
    void aFailedRestoreKeepsTheBackupAndNamesIt() throws IOException, InterruptedException {
        writeFixtures();

        Run run = drive("restore-fails");

        assertEquals(0, run.exitCode(), run.stderr());
        String line =
                run.stdout().lines().filter(l -> l.startsWith("BUMP-ERROR")).findFirst().orElse("");
        assertTrue(line.contains(KEPT_IN), run.stdout());
        Path backup = Path.of(line.substring(line.indexOf(KEPT_IN) + KEPT_IN.length()).strip());
        assertEquals(anchorsFor(GG5), read(backup));
        assertEquals(GG5 + "\n", read(versionFile()));
        assertEquals(1, strayTempFiles().size(), strayTempFiles().toString());
    }

    @Test
    @DisplayName("WR-04: an error that is not an OSError while staging leaves both files unchanged, no temp file and a BumpError")
    void aNonOsErrorWhileStagingLeavesNoStrayFile() throws IOException, InterruptedException {
        writeFixtures();

        Run run = drive("staging-write-error");

        assertEquals(0, run.exitCode(), run.stderr());
        assertTrue(run.stdout().contains("BUMP-ERROR"), run.stdout());
        assertFalse(run.stdout().contains("replaced"), run.stdout());
        assertEquals(GG5 + "\n", read(versionFile()));
        assertEquals(anchorsFor(GG5), read(anchorsFile()));
        assertEquals(List.of(), strayTempFiles());
    }
}
