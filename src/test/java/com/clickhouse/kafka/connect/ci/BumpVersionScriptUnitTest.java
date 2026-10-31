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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    private static final String NULL_TEXT = "null";
    private static final String BUMP_GG = "gg";
    private static final String VERSION_FILE_NAME = "VERSION";
    private static final String ANCHORS_FILE_NAME = "fork-anchors.txt";
    private static final String GG5 = "v1.7.0_gg5";
    private static final String GG6 = "v1.7.0_gg6";
    private static final String GG4 = "v1.7.0_gg4";
    private static final String ANCHORS_TEMPLATE =
            "# fixture anchors\n"
                    + "src/main/java/Example.java"
                    + TAB
                    + "foo"
                    + TAB
                    + "1"
                    + TAB
                    + "X-01\n"
                    + "VERSION"
                    + TAB
                    + "%s"
                    + TAB
                    + "1"
                    + TAB
                    + "BLD-07\n"
                    + "build.gradle.kts"
                    + TAB
                    + "windows-1252"
                    + TAB
                    + "2"
                    + TAB
                    + "BLD-05\n";

    @TempDir Path temp;

    private record Run(int exitCode, String stdout, String stderr) {}

    private static String anchorsFor(String versionPattern) {
        return ANCHORS_TEMPLATE.formatted(versionPattern);
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
}
