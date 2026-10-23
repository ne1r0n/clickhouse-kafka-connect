package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.data.ClickHouseColumn;
import com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureCatalog.Fixture;
import com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullability;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free guards over the committed DESCRIBE fixtures (DESC-06, research assumption A4): the
 * resource directory matches the catalog exactly, every committed row parses the way discovery
 * parses it, and the raw type of every parent of a size-named sub-column row parses, because a
 * parent that does not parse would take the undecidable path of the classifier, which keeps the
 * earlier behaviour (drops a bare {@code size} or {@code size0} element) instead of deciding. Deliberately
 * extends nothing: extending {@code ClickHouseBase} would exclude this class from {@code testUnit}.
 */
class DescribeFixturesUnitTest {

    private static final Path FIXTURE_DIRECTORY =
            Paths.get("src", "test", "resources", DescribeFixtureCatalog.RESOURCE_DIRECTORY);
    private static final String BUILD_SCRIPT = "build.gradle.kts";
    private static final Pattern DRIFT_TAG_DECLARATION =
            Pattern.compile("val describeFixtureDriftTag = \"([^\"]+)\"");
    private static final Pattern SIZE_NAMED = Pattern.compile("size[0-9]*");
    private static final char BACKSLASH = '\\';
    private static final char NAME_SEPARATOR = '.';

    static Stream<String> fixtureKeys() {
        return DescribeFixtureCatalog.FIXTURES.stream().map(Fixture::key);
    }

    private static List<ClickHouseFieldDescriptor> rows(String key) throws IOException {
        List<ClickHouseFieldDescriptor> rows = new ArrayList<>();
        for (String line : DescribeFixtureCatalog.resourceLines(key + DescribeFixtureCatalog.JSONL_SUFFIX)) {
            rows.add(ClickHouseFieldDescriptor.fromJsonRow(line));
        }
        return rows;
    }

    @Test
    @DisplayName("WR-02 drift tag: build.gradle.kts and DescribeFixtureDriftLiveTest name the same tag, once in the build")
    void theDriftTagInTheBuildEqualsTheTagOfTheDriftTest() throws IOException {
        String build = Files.readString(Paths.get(BUILD_SCRIPT));
        Matcher declaration = DRIFT_TAG_DECLARATION.matcher(build);
        assertTrue(declaration.find(), BUILD_SCRIPT + " must declare describeFixtureDriftTag");

        assertEquals(DescribeFixtureDriftLiveTest.DRIFT_TAG, declaration.group(1), "the Gradle tag and the Java tag");
        assertEquals(
                1,
                build.split(Pattern.quote("\"" + declaration.group(1) + "\""), -1).length - 1,
                "the tag string is written once in the build; the tasks must use describeFixtureDriftTag");
        assertTrue(
                Stream.of(DescribeFixtureDriftLiveTest.class.getAnnotationsByType(Tag.class))
                        .map(Tag::value)
                        .anyMatch(declaration.group(1)::equals),
                "DescribeFixtureDriftLiveTest carries the tag the build excludes from test");
    }

    @Test
    @DisplayName("DESC-06 fixture set: every catalog key has its DESCRIBE rows and Column snapshot")
    void everyCatalogKeyHasItsFiles() throws IOException {
        Set<String> keys = new HashSet<>();
        Set<String> expectedFiles = new HashSet<>();
        expectedFiles.add(DescribeFixtureCatalog.SERVER_VERSION_FILE);
        for (Fixture fixture : DescribeFixtureCatalog.FIXTURES) {
            assertTrue(keys.add(fixture.key()), "duplicate catalog key " + fixture.key());
            assertEquals(
                    -1, fixture.key().indexOf(BACKSLASH), "key " + fixture.key() + " must be backslash-free");
            assertEquals(
                    -1,
                    fixture.columnsDdl().indexOf(BACKSLASH),
                    "DDL of " + fixture.key() + " must be backslash-free");
            expectedFiles.add(fixture.key() + DescribeFixtureCatalog.JSONL_SUFFIX);
            expectedFiles.add(fixture.key() + DescribeFixtureCatalog.COLUMNS_SUFFIX);
        }
        Set<String> actualFiles = new HashSet<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(FIXTURE_DIRECTORY)) {
            for (Path file : files) {
                actualFiles.add(file.getFileName().toString());
            }
        }
        assertEquals(
                expectedFiles,
                actualFiles,
                "the fixture directory must hold exactly the catalog's files; re-capture with captureDescribeFixtures");
        assertTrue(
                DescribeFixtureCatalog.resourceLines(DescribeFixtureCatalog.SERVER_VERSION_FILE).stream()
                        .anyMatch(line -> line.startsWith("version=")),
                "SERVER_VERSION.txt must name the server version");
    }

    @ParameterizedTest(name = "DESC-06 rows parse: {0}")
    @MethodSource("fixtureKeys")
    void committedRowsParseThroughFromJsonRow(String key) throws IOException {
        List<ClickHouseFieldDescriptor> rows = assertDoesNotThrow(() -> rows(key), "a committed row of " + key);
        assertTrue(
                rows.stream().anyMatch(row -> !row.isSubcolumn()),
                key + " must hold at least one root row");
    }

    @ParameterizedTest(name = "DESC-06 parent types parse: {0}")
    @MethodSource("fixtureKeys")
    void parentTypesOfSizeNamedRowsParse(String key) throws IOException {
        List<ClickHouseFieldDescriptor> rows = rows(key);
        Set<String> parents = rows.stream()
                .filter(ClickHouseFieldDescriptor::isSubcolumn)
                .map(ClickHouseFieldDescriptor::getName)
                .filter(name -> SIZE_NAMED.matcher(name.substring(name.lastIndexOf(NAME_SEPARATOR) + 1)).matches())
                .map(name -> name.substring(0, name.lastIndexOf(NAME_SEPARATOR)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (String parent : parents) {
            for (ClickHouseFieldDescriptor row : rows) {
                if (row.getName().equals(parent)) {
                    String rawType = row.getType();
                    assertDoesNotThrow(
                            () -> ClickHouseColumn.of(parent, DeclaredNullability.normalise(rawType)),
                            "fixture " + key + ": parent " + parent + " with raw type " + rawType
                                    + " does not parse");
                }
            }
        }
        assertFalse(rows.isEmpty(), key + " must hold rows");
    }
}
