package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Docker-free pin for D-10's failed-test summary: it runs {@code .github/scripts/failed_tests_summary.py}
 * with {@code python3 -I} on fixture JUnit XML reports and reads the Markdown summary file and the
 * workflow commands it produces. It fails rather than skips when {@code python3} cannot start,
 * because the nightly runner and the development machine both have it.
 *
 * <p>The child's output is read as UTF-8 explicitly, because the test JVM runs with {@code
 * file.encoding=windows-1252}. Deliberately extends nothing - extending {@code ClickHouseBase} would
 * inherit its {@code clickhouse} tag and drop this class out of {@code testUnit}.
 */
class FailedTestsSummaryScriptUnitTest {
    private static final Path SCRIPT = Path.of(".github", "scripts", "failed_tests_summary.py");
    private static final String PYTHON = "python3";
    private static final long TIMEOUT_SECONDS = 60;
    private static final String SUMMARY_ENV = "GITHUB_STEP_SUMMARY";
    private static final String COMMAND_PREFIX = "::";
    private static final String ERROR_PREFIX = "::error title=Integration tests failed::";
    private static final String NOTICE_PREFIX = "::notice title=Integration tests::";
    private static final String WARNING_PREFIX = "::warning title=Integration test reports::";
    private static final String HEADING = "### Integration tests (ClickHouse latest, client V2)";
    private static final String NO_FAILURES = "No failed tests.";
    private static final int ANNOTATION_BODY_LIMIT = 1100;
    private static final int MANY_FAILURES = 200;
    private static final String SMOKE_CLASS = "com.example.SmokeTest";

    @TempDir Path temp;

    private record Run(int exitCode, List<String> stdout, String stderr) {}

    private Run run(Path summary, String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(PYTHON, "-I", SCRIPT.toString()));
        command.addAll(List.of(arguments));
        Path out = Files.createTempFile(temp, "stdout", ".txt");
        Path err = Files.createTempFile(temp, "stderr", ".txt");
        ProcessBuilder builder =
                new ProcessBuilder(command).redirectOutput(out.toFile()).redirectError(err.toFile());
        builder.environment().remove(SUMMARY_ENV);
        if (summary != null) {
            builder.environment().put(SUMMARY_ENV, summary.toString());
        }
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new AssertionError("python3 is required to run the failed-test summary script", e);
        }
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("The summary script did not finish within " + TIMEOUT_SECONDS + " seconds");
        }
        return new Run(
                process.exitValue(),
                Files.readAllLines(out, StandardCharsets.UTF_8),
                Files.readString(err, StandardCharsets.UTF_8));
    }

    private Path reportsDirectory() throws IOException {
        return Files.createDirectory(temp.resolve("reports"));
    }

    private static void writeReport(Path directory, String fileName, String cases) throws IOException {
        Files.writeString(
                directory.resolve(fileName),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testsuite name=\"suite\">\n"
                        + cases
                        + "</testsuite>\n",
                StandardCharsets.UTF_8);
    }

    private static String passing(String className, String name) {
        return "<testcase name=\"" + name + "\" classname=\"" + className + "\"/>\n";
    }

    private static String failing(String className, String name, String kind, String message) {
        return "<testcase name=\""
                + name
                + "\" classname=\""
                + className
                + "\"><"
                + kind
                + " message=\""
                + message
                + "\">trace</"
                + kind
                + "></testcase>\n";
    }

    private static String skipped(String className, String name) {
        return "<testcase name=\"" + name + "\" classname=\"" + className + "\"><skipped/></testcase>\n";
    }

    private static List<String> commands(Run run) {
        return run.stdout().stream().filter(line -> line.startsWith(COMMAND_PREFIX)).toList();
    }

    private String summaryOf(Path summary) throws IOException {
        assertTrue(Files.isRegularFile(summary), () -> "The script wrote no summary file: " + summary);
        return Files.readString(summary, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName(
            "a failure and an error across two report files are listed by class#method in the summary and in"
                    + " one error annotation (D-10)")
    void failureAndErrorAcrossTwoFilesAreListed() throws IOException, InterruptedException {
        Path reports = reportsDirectory();
        writeReport(
                reports,
                "TEST-a.ClassA.xml",
                passing("a.ClassA", "passes()")
                        + failing("a.ClassA", "fails()", "failure", "expected 1 but was 2&#10;second line"));
        writeReport(
                reports,
                "TEST-b.ClassB.xml",
                failing("b.ClassB", "breaks()", "error", "boom"));
        Path summary = temp.resolve("summary.md");

        Run run = run(summary, reports.toString());

        assertEquals(0, run.exitCode(), run.stderr());
        assertEquals(List.of(ERROR_PREFIX + "2 failed: a.ClassA#fails(), b.ClassB#breaks()"), commands(run));
        String markdown = summaryOf(summary);
        assertTrue(markdown.contains(HEADING), markdown);
        assertTrue(markdown.contains("tests=3 failures=1 errors=1 skipped=0"), markdown);
        assertTrue(markdown.contains("| a.ClassA#fails() | failure | expected 1 but was 2 |"), markdown);
        assertTrue(markdown.contains("| b.ClassB#breaks() | error | boom |"), markdown);
        assertFalse(markdown.contains("a.ClassA#passes()"), markdown);
    }

    @Test
    @DisplayName(
            "a run without failures writes a notice and 'No failed tests.', also without GITHUB_STEP_SUMMARY"
                    + " (D-10)")
    void runWithoutFailuresWritesNoticeWithAndWithoutSummaryFile()
            throws IOException, InterruptedException {
        Path reports = reportsDirectory();
        writeReport(
                reports,
                "TEST-ok.xml",
                passing(SMOKE_CLASS, "first()") + passing(SMOKE_CLASS, "second()"));
        Path summary = temp.resolve("summary.md");

        Run withSummary = run(summary, reports.toString());
        Run withoutSummary = run(null, reports.toString());

        for (Run run : List.of(withSummary, withoutSummary)) {
            assertEquals(0, run.exitCode(), run.stderr());
            assertEquals(List.of(NOTICE_PREFIX + "tests=2 failures=0 errors=0 skipped=0"), commands(run));
            assertTrue(run.stdout().contains(NO_FAILURES), run.stdout().toString());
        }
        String markdown = summaryOf(summary);
        assertTrue(markdown.contains(NO_FAILURES), markdown);
        assertTrue(markdown.contains("tests=2 failures=0 errors=0 skipped=0"), markdown);
    }

    @Test
    @DisplayName("a missing results directory exits 0 with a warning annotation and a summary line (D-10)")
    void missingResultsDirectoryWarnsAndExitsZero() throws IOException, InterruptedException {
        Path absent = temp.resolve("absent");
        Path empty = Files.createDirectory(temp.resolve("empty"));
        Path summary = temp.resolve("summary.md");

        for (Path directory : List.of(absent, empty)) {
            Run run = run(summary, directory.toString());

            assertEquals(0, run.exitCode(), run.stderr());
            List<String> commands = commands(run);
            assertEquals(1, commands.size(), commands.toString());
            assertTrue(commands.get(0).startsWith(WARNING_PREFIX + "no JUnit XML reports under "), commands.get(0));
            assertTrue(
                    run.stdout()
                            .contains(
                                    "No JUnit XML reports found under "
                                            + directory
                                            + "; nothing to summarise."),
                    run.stdout().toString());
        }
        String markdown = summaryOf(summary);
        assertTrue(markdown.contains(HEADING), markdown);
        assertTrue(markdown.contains("nothing to summarise."), markdown);
    }

    @Test
    @DisplayName("a malformed report file is named in a warning and the other files are still read (D-10)")
    void malformedReportIsNamedAndTheOthersAreStillRead() throws IOException, InterruptedException {
        Path reports = reportsDirectory();
        Files.writeString(reports.resolve("TEST-bad.xml"), "<testsuite><testcase", StandardCharsets.UTF_8);
        writeReport(
                reports,
                "TEST-good.xml",
                failing(SMOKE_CLASS, "fails()", "failure", "expected 1 but was 2"));
        Path summary = temp.resolve("summary.md");

        Run run = run(summary, reports.toString());

        assertEquals(0, run.exitCode(), run.stderr());
        assertEquals(
                List.of(
                        WARNING_PREFIX + "unreadable report files: TEST-bad.xml",
                        ERROR_PREFIX + "1 failed: " + SMOKE_CLASS + "#fails()"),
                commands(run));
        assertTrue(run.stdout().contains("Unreadable report files: TEST-bad.xml"), run.stdout().toString());
        String markdown = summaryOf(summary);
        assertTrue(markdown.contains("tests=1 failures=1 errors=0 skipped=0"), markdown);
        assertTrue(markdown.contains("Unreadable report files: TEST-bad.xml"), markdown);
    }

    @Test
    @DisplayName(
            "percent, CR, LF, pipe and backtick in a test name are escaped for the annotation and the Markdown"
                    + " table (D-10)")
    void specialCharactersInTestNamesAreEscaped() throws IOException, InterruptedException {
        Path reports = reportsDirectory();
        writeReport(
                reports,
                "TEST-escape.xml",
                failing("com.example.Esc", "50% done&#10;a|b`c", "failure", "message with | and `tick`"));
        Path summary = temp.resolve("summary.md");

        Run run = run(summary, reports.toString());

        assertEquals(0, run.exitCode(), run.stderr());
        List<String> annotations =
                run.stdout().stream().filter(line -> line.startsWith(ERROR_PREFIX)).toList();
        assertEquals(1, annotations.size(), run.stdout().toString());
        assertEquals(List.of(annotations.get(0)), commands(run));
        assertTrue(annotations.get(0).contains("%25"), annotations.get(0));
        assertTrue(annotations.get(0).contains("%0A"), annotations.get(0));
        assertTrue(
                run.stdout().stream().noneMatch(line -> line.startsWith("a|b")),
                "a raw line break in a name must not start a new stdout line: " + run.stdout());
        List<String> rows =
                summaryOf(summary).lines().filter(line -> line.startsWith("| com.example.Esc#")).toList();
        assertEquals(1, rows.size(), rows.toString());
        assertTrue(rows.get(0).contains("\\|"), rows.get(0));
        assertFalse(rows.get(0).contains("`"), rows.get(0));
    }

    @Test
    @DisplayName(
            "more failing names than the cap keep the annotation to one capped line, and the summary lists"
                    + " every name (D-10)")
    void manyFailuresKeepOneCappedAnnotationAndAFullSummary()
            throws IOException, InterruptedException {
        Path reports = reportsDirectory();
        writeReport(
                reports,
                "TEST-many.xml",
                IntStream.rangeClosed(1, MANY_FAILURES)
                        .mapToObj(i -> failing("com.example.Many", name(i), "failure", "broken"))
                        .collect(Collectors.joining()));
        Path summary = temp.resolve("summary.md");

        Run run = run(summary, reports.toString());

        assertEquals(0, run.exitCode(), run.stderr());
        List<String> annotations =
                run.stdout().stream().filter(line -> line.startsWith(ERROR_PREFIX)).toList();
        assertEquals(1, annotations.size(), run.stdout().toString());
        String annotation = annotations.get(0);
        assertTrue(
                annotation.length() <= ERROR_PREFIX.length() + ANNOTATION_BODY_LIMIT,
                "annotation length " + annotation.length());
        assertTrue(annotation.contains("(+"), annotation);
        String markdown = summaryOf(summary);
        for (int i = 1; i <= MANY_FAILURES; i++) {
            String expected = "com.example.Many#" + name(i);
            assertTrue(markdown.contains(expected), "the summary lacks " + expected);
        }
    }

    private static String name(int index) {
        return String.format("failure%04d()", index);
    }

    @Test
    @DisplayName("no argument exits 2 with a usage line on stderr")
    void noArgumentIsAUsageError() throws IOException, InterruptedException {
        Run run = run(null);

        assertEquals(2, run.exitCode(), run.stderr());
        assertTrue(run.stderr().contains("usage:"), run.stderr());
        assertTrue(run.stdout().isEmpty(), run.stdout().toString());
    }

    @Test
    @DisplayName("skipped tests are counted, never listed as failed (D-04)")
    void skippedTestsAreCountedNotListed() throws IOException, InterruptedException {
        Path reports = reportsDirectory();
        writeReport(
                reports,
                "TEST-skipped.xml",
                passing(SMOKE_CLASS, "passes()") + skipped(SMOKE_CLASS, "skippedOne()") + skipped(SMOKE_CLASS, "skippedTwo()"));
        Path summary = temp.resolve("summary.md");

        Run run = run(summary, reports.toString());

        assertEquals(0, run.exitCode(), run.stderr());
        assertEquals(List.of(NOTICE_PREFIX + "tests=3 failures=0 errors=0 skipped=2"), commands(run));
        String markdown = summaryOf(summary);
        assertTrue(markdown.contains("tests=3 failures=0 errors=0 skipped=2"), markdown);
        assertTrue(markdown.contains(NO_FAILURES), markdown);
        assertFalse(markdown.contains("skippedOne"), markdown);
        assertFalse(markdown.contains("skippedTwo"), markdown);
    }
}
