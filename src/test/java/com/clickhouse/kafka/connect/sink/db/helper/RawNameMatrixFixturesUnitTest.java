package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.data.ClickHouseColumn;
import com.clickhouse.kafka.connect.sink.db.helper.RawNameMatrix.Entry;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullability;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private static final String WARN_LEVEL = "WARN";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_DEFAULT_EXPRESSION = "default_expression";
    private static final String FIELD_IS_SUBCOLUMN = "is_subcolumn";
    private static final String DESCRIBED_DATABASE = "matrix_db";
    private static final String DESCRIBED_TABLE = "matrix_table";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String COMMENT_SENTINEL = "zzCOMMENTq7";
    private static final String MALFORMED_ROW_FORMAT = "{\"name\":\"x\",\"type\":\"Int32\",\"default_type\":\"\","
            + "\"default_expression\":\"\",\"comment\":\"%s\",\"codec_expression\":\"\","
            + "\"ttl_expression\":\"\",\"is_subcolumn\":?}";

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

    private static void assertRowDecodes(Entry entry, String line) throws IOException {
        ClickHouseFieldDescriptor descriptor = ClickHouseFieldDescriptor.fromJsonRow(line);
        String where = "entry " + entry.key() + " row " + line;
        assertEquals(RawNameMatrix.decodedField(line, FIELD_NAME), descriptor.getName(), where + ": name");
        assertEquals(
                RawNameMatrix.innerLiteral(line, FIELD_TYPE),
                descriptor.getType(),
                where + ": the type text keeps the server's JSON escapes");
        assertEquals(
                RawNameMatrix.decodedField(line, FIELD_TYPE),
                JSON.readValue("\"" + descriptor.getType() + "\"", String.class),
                where + ": the JSON decode of the type text is the decoded type");
        assertEquals(
                RawNameMatrix.innerLiteral(line, FIELD_DEFAULT_EXPRESSION),
                descriptor.getDefaultExpression(),
                where + ": the default expression keeps the server's JSON escapes");
        JsonNode subcolumn = JSON.readTree(line).get(FIELD_IS_SUBCOLUMN);
        assertEquals(subcolumn.asInt() == 1, descriptor.isSubcolumn(), where + ": is_subcolumn");
    }

    @ParameterizedTest(name = "NAME-03: fromJsonRow yields the raw name and keeps the type text for entry {0}")
    @MethodSource("entries")
    void fromJsonRowYieldsTheRawNameAndKeepsTheTypeText(Entry entry) throws IOException {
        for (String line : committed(entry.key() + RawNameMatrix.NAMES_SUFFIX)) {
            assertRowDecodes(entry, line);
        }
        for (String line : committed(entry.key() + RawNameMatrix.ENUM_SUFFIX)) {
            assertRowDecodes(entry, line);
        }
    }

    @ParameterizedTest(name = "NAME-04: discovery pairs under the raw name without a WARN for entry {0}")
    @MethodSource("entries")
    void discoveryPairsUnderTheRawName(Entry entry) throws IOException {
        List<String> lines = committed(entry.key() + RawNameMatrix.NAMES_SUFFIX);
        Table table;
        List<LogCapture.CapturedEvent> warnings = new ArrayList<>();
        try (LogCapture helperLog = LogCapture.attach(ClickHouseHelperClient.class);
                LogCapture nullabilityLog = LogCapture.attach(DeclaredNullability.class)) {
            DescribeTableBuilder builder = new DescribeTableBuilder(DESCRIBED_DATABASE, DESCRIBED_TABLE);
            for (String line : lines) {
                assertTrue(builder.addRow(line), "entry " + entry.key() + " row " + line + ": the row is kept");
            }
            table = builder.build();
            warnings.addAll(helperLog.events(WARN_LEVEL));
            warnings.addAll(nullabilityLog.events(WARN_LEVEL));
        }
        RawNameMatrix.assertDiscoveredUnderRawNames(entry, table, warnings);

        String tupleType = lines.stream()
                .filter(line -> RawNameMatrix.TUPLE_COLUMN.equals(RawNameMatrix.decodedField(line, FIELD_NAME)))
                .map(line -> RawNameMatrix.innerLiteral(line, FIELD_TYPE))
                .findFirst()
                .orElseThrow();
        ClickHouseColumn declared =
                ClickHouseColumn.of(RawNameMatrix.TUPLE_COLUMN, DeclaredNullability.normalise(tupleType));
        assertEquals(
                RawNameMatrix.elementName(entry),
                DeclaredNullability.elementKey(declared.getNestedColumns().get(0), 0),
                "entry " + entry.key() + ": the declared element key is the raw element name");
    }

    @ParameterizedTest(name = "NAME-06: the enum keys are the raw labels for entry {0}")
    @MethodSource("entries")
    void enumKeysAreTheRawLabels(Entry entry) throws IOException {
        String line = committed(entry.key() + RawNameMatrix.ENUM_SUFFIX).stream()
                .filter(row -> RawNameMatrix.ENUM_COLUMN.equals(RawNameMatrix.decodedField(row, FIELD_NAME)))
                .findFirst()
                .orElseThrow();

        Column column = Column.extractColumn(ClickHouseFieldDescriptor.fromJsonRow(line));

        assertEquals(
                Map.of(RawNameMatrix.label(entry), 1, RawNameMatrix.OTHER_LABEL, 2),
                column.getEnumValues(),
                "entry " + entry.key() + ": the enum keys are the labels as written in CREATE TABLE");
    }

    @Test
    @DisplayName("NAME-03: a malformed row fails without echoing its comment")
    void aMalformedRowFailsWithoutEchoingItsComment() {
        String line = String.format(MALFORMED_ROW_FORMAT, COMMENT_SENTINEL);

        JsonProcessingException failure =
                assertThrows(JsonProcessingException.class, () -> ClickHouseFieldDescriptor.fromJsonRow(line));

        for (Throwable t = failure; t != null; t = t.getCause()) {
            assertFalse(
                    String.valueOf(t.getMessage()).contains(COMMENT_SENTINEL),
                    "the failure must not carry the row: " + t.getClass().getSimpleName());
        }
    }
}
