package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.kafka.connect.sink.db.helper.RawNameMatrix.Entry;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free guards over the committed name-matrix rows of Phase 28 (NAME-02): the resource
 * directory holds exactly the files of the catalog, and the committed rows still satisfy the
 * server premises (the DESCRIBE JSON carries the raw identifier) that the name decode rests on.
 * Deliberately extends nothing: extending {@code ClickHouseBase} would exclude this class from
 * {@code testUnit}.
 */
class RawNameMatrixFixturesUnitTest {

    private static final Path MATRIX_DIRECTORY =
            Paths.get("src", "test", "resources", RawNameMatrix.RESOURCE_DIRECTORY);

    static Stream<Entry> entries() {
        return RawNameMatrix.ENTRIES.stream();
    }

    private static List<String> committed(String fileName) {
        return DescribeFixtureCatalog.resourceLines(RawNameMatrix.RESOURCE_DIRECTORY, fileName);
    }

    @Test
    @DisplayName("NAME-02 fixtures: the resource directory holds exactly the catalog files")
    void resourceDirectoryHoldsExactlyTheCatalogFiles() throws IOException {
        Set<String> expected = new TreeSet<>();
        expected.add(RawNameMatrix.SERVER_VERSION_FILE);
        for (Entry entry : RawNameMatrix.ENTRIES) {
            expected.add(entry.key() + RawNameMatrix.NAMES_SUFFIX);
            expected.add(entry.key() + RawNameMatrix.ENUM_SUFFIX);
        }
        Set<String> actual = new TreeSet<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(MATRIX_DIRECTORY)) {
            for (Path file : files) {
                actual.add(file.getFileName().toString());
            }
        }
        assertEquals(expected, actual, "the matrix resource directory must hold exactly the catalog files");
    }

    @ParameterizedTest(name = "NAME-02 fixtures: the committed rows of entry {0} carry the raw identifier in the JSON")
    @MethodSource("entries")
    void committedRowsCarryTheRawIdentifierInTheJson(Entry entry) {
        RawNameMatrix.assertServerPremises(
                entry,
                committed(entry.key() + RawNameMatrix.NAMES_SUFFIX),
                committed(entry.key() + RawNameMatrix.ENUM_SUFFIX),
                committed(RawNameMatrix.CONTROL_KEY + RawNameMatrix.NAMES_SUFFIX));
    }
}
