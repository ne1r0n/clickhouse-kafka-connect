package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pin for DEBT-17: the fork coverage class list exists twice, as {@code
 * forkChangedClasses} in {@code build.gradle.kts} (what the JaCoCo fork report includes) and as
 * {@code FORK_CLASSES_IN_ORDER} in {@code .github/scripts/jacoco_summary.py} (what the CI summary
 * tabulates). A class missing from either copy fails {@code testUnit} instead of vanishing from the
 * coverage view. Deliberately extends nothing - extending {@code ClickHouseBase} would inherit its
 * {@code clickhouse} tag and drop this class out of {@code testUnit}.
 *
 * <p>Both files are read as UTF-8 explicitly, because the test JVM runs with {@code
 * file.encoding=windows-1252}.
 */
class ForkCoverageScopeUnitTest {
    private static final Path BUILD_SCRIPT = Path.of("build.gradle.kts");
    private static final Path SUMMARY_SCRIPT = Path.of(".github", "scripts", "jacoco_summary.py");
    private static final Path MAIN_SOURCES = Path.of("src", "main", "java");

    private static final String GRADLE_LIST_START = "val forkChangedClasses = listOf(";
    private static final String GRADLE_LIST_END = ")";
    private static final String PYTHON_LIST_START = "FORK_CLASSES_IN_ORDER = [";
    private static final String PYTHON_LIST_END = "]";
    private static final String CLASS_SUFFIX = ".class";
    private static final String INNER_CLASS_GLOB = "\\$*.class";
    private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"");

    private static final String PACKAGE = "com/clickhouse/kafka/connect/";
    private static final List<String> CLASSES_ADDED_BY_THIS_MILESTONE =
            List.of(
                    PACKAGE + "sink/db/mapping/DeclaredNullability",
                    PACKAGE + "util/WithheldMessageException",
                    PACKAGE + "sink/db/helper/DescribeTableBuilder",
                    PACKAGE + "sink/db/mapping/ParentShape",
                    PACKAGE + "sink/db/helper/ClickHouseFieldDescriptor",
                    PACKAGE + "sink/db/MetadataFreeJsonWriter");

    @Test
    @DisplayName("DEBT-17: jacoco_summary.py lists the same fork classes as build.gradle.kts, in order")
    void jacocoSummaryListsTheSameForkClassesInOrder() throws IOException {
        List<String> gradleClasses = baseClasses(gradleEntries());
        List<String> summaryClasses = quotedBetween(SUMMARY_SCRIPT, PYTHON_LIST_START, PYTHON_LIST_END);

        assertEquals(
                gradleClasses,
                summaryClasses,
                "FORK_CLASSES_IN_ORDER in "
                        + SUMMARY_SCRIPT
                        + " must equal the class entries of forkChangedClasses in "
                        + BUILD_SCRIPT
                        + ", in order");
    }

    @Test
    @DisplayName("DEBT-17: every fork class has its inner-class glob and a source file")
    void everyForkClassHasItsGlobAndASourceFile() throws IOException {
        List<String> entries = gradleEntries();
        List<String> baseClasses = baseClasses(entries);
        Set<String> globs = new LinkedHashSet<>();
        for (String entry : entries) {
            if (entry.endsWith(INNER_CLASS_GLOB)) {
                globs.add(entry.substring(0, entry.length() - INNER_CLASS_GLOB.length()));
            }
        }

        List<String> withoutGlob = new ArrayList<>(baseClasses);
        withoutGlob.removeAll(globs);
        Set<String> globsWithoutClass = new LinkedHashSet<>(globs);
        globsWithoutClass.removeAll(baseClasses);
        List<String> withoutSource = new ArrayList<>();
        for (String baseClass : baseClasses) {
            if (!Files.isRegularFile(MAIN_SOURCES.resolve(baseClass + ".java"))) {
                withoutSource.add(baseClass);
            }
        }

        assertTrue(!baseClasses.isEmpty(), "forkChangedClasses in " + BUILD_SCRIPT + " holds no class entry");
        assertTrue(withoutGlob.isEmpty(), "fork classes without an inner-class glob: " + withoutGlob);
        assertTrue(globsWithoutClass.isEmpty(), "inner-class globs without a class entry: " + globsWithoutClass);
        assertTrue(withoutSource.isEmpty(), "fork classes without a source file under src/main/java: " + withoutSource);
    }

    @Test
    @DisplayName(
            "DEBT-17: the fork list holds DeclaredNullability, WithheldMessageException and the classes this milestone added")
    void theForkListHoldsTheClassesThisMilestoneAdded() throws IOException {
        List<String> missing = new ArrayList<>(CLASSES_ADDED_BY_THIS_MILESTONE);
        missing.removeAll(baseClasses(gradleEntries()));

        assertTrue(missing.isEmpty(), "forkChangedClasses in " + BUILD_SCRIPT + " lacks: " + missing);
    }

    private static List<String> gradleEntries() throws IOException {
        return quotedBetween(BUILD_SCRIPT, GRADLE_LIST_START, GRADLE_LIST_END);
    }

    private static List<String> baseClasses(List<String> entries) {
        List<String> baseClasses = new ArrayList<>();
        for (String entry : entries) {
            if (entry.endsWith(CLASS_SUFFIX) && !entry.endsWith(INNER_CLASS_GLOB)) {
                baseClasses.add(entry.substring(0, entry.length() - CLASS_SUFFIX.length()));
            }
        }
        return baseClasses;
    }

    /** The quoted strings after the line that starts with {@code start} up to the next line equal to {@code end}. */
    private static List<String> quotedBetween(Path file, String start, String end) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> quoted = new ArrayList<>();
        boolean inside = false;
        boolean closed = false;
        for (String line : lines) {
            if (!inside) {
                inside = line.startsWith(start);
                continue;
            }
            if (line.equals(end)) {
                closed = true;
                break;
            }
            Matcher matcher = QUOTED.matcher(line);
            while (matcher.find()) {
                quoted.add(matcher.group(1));
            }
        }
        assertTrue(inside && closed, file + " has no list opened by '" + start + "' and closed by a line '" + end + "'");
        return quoted;
    }
}
