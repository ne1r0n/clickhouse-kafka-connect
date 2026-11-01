package com.clickhouse.kafka.connect.ci;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Docker-free table test of {@code .github/scripts/baseline_tag.py}: it runs the script with {@code
 * python3 -I}, feeds the tag names on standard input from a file in a temporary directory and reads
 * the exit code and both output streams back. It fails rather than skips when {@code python3}
 * cannot start, because the release runners and the development machine both have it.
 *
 * <p>The child's streams are read as UTF-8 explicitly, because the test JVM runs with {@code
 * file.encoding=windows-1252}. Deliberately has no superclass - deriving from {@code ClickHouseBase}
 * would inherit its {@code clickhouse} tag and drop this class out of {@code testUnit}.
 */
class BaselineTagScriptUnitTest {
    private static final Path SCRIPT = Path.of(".github", "scripts", "baseline_tag.py");
    private static final String PYTHON = "python3";
    private static final long TIMEOUT_SECONDS = 60;
    private static final String ENCODING_ENV = "PYTHONIOENCODING";
    private static final String ERROR_PREFIX = "ERROR:";
    private static final String USAGE = "usage:";
    private static final String GG3 = "v1.7.0_gg3";
    private static final String GG4 = "v1.7.0_gg4";
    private static final String GG5 = "v1.7.0_gg5";
    private static final String GG6 = "v1.7.0_gg6";
    private static final String PLAIN = "v1.7.0";
    private static final String ARABIC_INDIC_ONE = "١";

    @TempDir Path temp;

    private record Run(int exitCode, String stdout, String stderr) {
        String describe() {
            return "stdout:\n" + stdout + "stderr:\n" + stderr;
        }
    }

    private static String lines(String... names) {
        return String.join("\n", names) + "\n";
    }

    private Run run(String stdinText, String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(PYTHON, "-I", SCRIPT.toString()));
        command.addAll(List.of(arguments));
        Path in = Files.createTempFile(temp, "stdin", ".txt");
        Files.writeString(in, stdinText, UTF_8);
        Path out = Files.createTempFile(temp, "stdout", ".txt");
        Path err = Files.createTempFile(temp, "stderr", ".txt");
        ProcessBuilder builder =
                new ProcessBuilder(command)
                        .redirectInput(in.toFile())
                        .redirectOutput(out.toFile())
                        .redirectError(err.toFile());
        builder.environment().put(ENCODING_ENV, UTF_8.name());
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new AssertionError("python3 is required to run the baseline tag script", e);
        }
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("The baseline tag script did not finish within " + TIMEOUT_SECONDS + " seconds");
        }
        return new Run(
                process.exitValue(), Files.readString(out, UTF_8), Files.readString(err, UTF_8));
    }

    private static Stream<Arguments> printedTags() {
        return Stream.of(
                Arguments.of(
                        "the previous counter is chosen over the same and an older one",
                        GG5,
                        lines(GG4, GG5, GG3),
                        GG4),
                Arguments.of("the counter below a later release", GG6, lines(GG5, GG4), GG5),
                Arguments.of(
                        "counters compare numerically and not as text",
                        "v1.7.0_gg10",
                        lines("v1.7.0_gg9", "v1.7.0_gg2"),
                        "v1.7.0_gg9"),
                Arguments.of(
                        "a tag without a suffix sorts below the first counter",
                        "v1.7.0_gg1",
                        lines(PLAIN, "v1.6.0_gg3"),
                        PLAIN),
                Arguments.of(
                        "a suffixed older tag beats its suffix-less sibling",
                        PLAIN,
                        lines("v1.6.0_gg1", "v1.6.0"),
                        "v1.6.0_gg1"),
                Arguments.of(
                        "the minor and patch numbers outrank the counter",
                        "v1.8.0_gg1",
                        lines("v1.7.9_gg9", "v1.7.0_gg20"),
                        "v1.7.9_gg9"),
                Arguments.of(
                        "a larger major number is a later release",
                        "v2.0.0_gg1",
                        lines("v1.99.99_gg99", PLAIN),
                        "v1.99.99_gg99"),
                Arguments.of(
                        "names that are not release tags are ignored",
                        GG5,
                        lines(
                                "v1.7.0_gg4-hardening",
                                "pre-v1.7.0_gg4",
                                "baseline-v1.7.0_gg4",
                                "v1.7.0_gg4_x",
                                GG3),
                        GG3),
                Arguments.of(
                        "a list with carriage return line endings is read",
                        GG5,
                        String.join("\r\n", GG4, GG3) + "\r\n",
                        GG4),
                Arguments.of(
                        "a tag with an Arabic-Indic digit is ignored",
                        GG5,
                        lines("v" + ARABIC_INDIC_ONE + ".7.0_gg4", GG3),
                        GG3));
    }

    private static Stream<Arguments> noBaselineLists() {
        return Stream.of(
                Arguments.of("the list holds only the tag of the version itself", lines(GG5)),
                Arguments.of("the list holds only later tags", lines(GG6, "v1.8.0")),
                Arguments.of("the list is empty", ""));
    }

    private static Stream<Arguments> invalidVersions() {
        return Stream.of(
                Arguments.of("the counter zero", "v1.7.0_gg0"),
                Arguments.of("a version without the v prefix", "1.7.0"),
                Arguments.of("the empty version", ""),
                Arguments.of("a version with a trailing line feed", GG5 + "\n"));
    }

    @ParameterizedTest(name = "CIH-04: {0}")
    @MethodSource("printedTags")
    void printsTheGreatestReleaseTagBelowTheVersion(
            String label, String version, String tags, String expected) throws Exception {
        Run run = run(tags, version);
        assertEquals(0, run.exitCode(), run::describe);
        assertEquals(expected + "\n", run.stdout(), run::describe);
        assertEquals("", run.stderr(), run::describe);
    }

    @ParameterizedTest(name = "CIH-04: no baseline when {0}")
    @MethodSource("noBaselineLists")
    void failsWhenNoReleaseTagSortsBelowTheVersion(String label, String tags) throws Exception {
        Run run = run(tags, GG5);
        assertEquals(1, run.exitCode(), run::describe);
        assertEquals("", run.stdout(), run::describe);
        assertTrue(run.stderr().startsWith(ERROR_PREFIX), run::describe);
    }

    @ParameterizedTest(name = "CIH-04: an invalid version is refused: {0}")
    @MethodSource("invalidVersions")
    void refusesAnInvalidVersion(String label, String version) throws Exception {
        Run run = run(lines(GG4), version);
        assertEquals(2, run.exitCode(), run::describe);
        assertEquals("", run.stdout(), run::describe);
        assertTrue(run.stderr().startsWith(ERROR_PREFIX), run::describe);
    }

    @Test
    @DisplayName("CIH-04: no argument is a usage error")
    void noArgumentIsAUsageError() throws Exception {
        Run run = run(lines(GG4));
        assertEquals(2, run.exitCode(), run::describe);
        assertEquals("", run.stdout(), run::describe);
        assertTrue(run.stderr().contains(USAGE), run::describe);
    }

    @Test
    @DisplayName("CIH-04: two arguments are a usage error")
    void twoArgumentsAreAUsageError() throws Exception {
        Run run = run(lines(GG4), GG5, GG6);
        assertEquals(2, run.exitCode(), run::describe);
        assertEquals("", run.stdout(), run::describe);
        assertTrue(run.stderr().contains(USAGE), run::describe);
    }
}
