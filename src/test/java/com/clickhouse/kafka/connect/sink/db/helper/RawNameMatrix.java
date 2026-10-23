package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullability;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The NAME-02 catalog of the Phase 28 spike: one entry per identifier character the server escapes
 * in its DESCRIBE JSON, the DDL builders for a names table and an enum table per entry, the
 * escapers that are the test oracle of the server's two layers (SQL, then JSON), the server
 * premises that hold before and after the name decode, and the cell renderer that records, per
 * shape, the server's JSON text, the form discovery yields under each client and the decoded form.
 * Static members only, no state. Every file it writes or reads names UTF-8; non-ASCII literals are
 * escapes.
 */
public final class RawNameMatrix {

    /** One spiked character: its key, the raw identifier fragment and a one-line description. */
    public record Entry(String key, String fragment, String description) {
        @Override
        public String toString() {
            return key;
        }
    }

    public static final String CONTROL_KEY = "ctl";
    public static final String RESOURCE_DIRECTORY = "raw-name-matrix";
    public static final String NAMES_SUFFIX = "-names.jsonl";
    public static final String ENUM_SUFFIX = "-enum.jsonl";
    public static final String SERVER_VERSION_FILE = "SERVER_VERSION.txt";
    public static final String CAPTURE_DIRECTORY = "build/raw-name-matrix";
    public static final int ROOT_DEFAULT = 99;

    public static final String SHAPE_ROOT = "root";
    public static final String SHAPE_TUPLE = "tuple";
    public static final String SHAPE_ARRAY_TUPLE = "array_tuple";
    public static final String SHAPE_NULLABLE_TUPLE = "nullable_tuple";
    public static final String SHAPE_MAP_TUPLE = "map_tuple";
    public static final String SHAPE_ENUM_LABEL = "enum_label";
    public static final List<String> SHAPES = List.of(
            SHAPE_ROOT, SHAPE_TUPLE, SHAPE_ARRAY_TUPLE, SHAPE_NULLABLE_TUPLE, SHAPE_MAP_TUPLE, SHAPE_ENUM_LABEL);

    public static final String ID_COLUMN = "id";
    public static final String TUPLE_COLUMN = "t";
    public static final String ARRAY_COLUMN = "a";
    public static final String NULLABLE_COLUMN = "n";
    public static final String MAP_COLUMN = "m";
    public static final String ENUM_COLUMN = "e";
    public static final String PLAIN_ELEMENT = "d";
    public static final String OTHER_LABEL = "z";

    public static final String UNMAPPED = "UNMAPPED";
    public static final String ABSENT = "ABSENT";
    public static final String ERROR_PREFIX = "ERROR:";
    public static final String CELL = "CELL";
    public static final String WARNS = "WARNS";
    public static final String KIND_NAMES = "names";
    public static final String KIND_ENUM = "enum";
    public static final String CLIENT_V2 = "V2";
    public static final String CLIENT_V1 = "V1";

    private static final String WARN_LEVEL = "WARN";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPE = "type";
    private static final String COLUMN_SEPARATOR = "\t";
    private static final char NAME_SEPARATOR = '.';
    private static final String ELEMENT_SUFFIX = NAME_SEPARATOR + PLAIN_ELEMENT;
    private static final Set<String> CONTAINER_COLUMNS =
            Set.of(ID_COLUMN, TUPLE_COLUMN, ARRAY_COLUMN, NULLABLE_COLUMN, MAP_COLUMN);
    private static final String NAMES_DDL_FORMAT = "CREATE TABLE %%s (%s UInt32, %s Int32 DEFAULT %d, "
            + "t Tuple(%s), a Array(Tuple(%s)), n Nullable(Tuple(%s)), m Map(String, Tuple(%s))) "
            + "ENGINE = MergeTree ORDER BY id";
    private static final String ENUM_DDL_FORMAT =
            "CREATE TABLE %%s (id UInt32, e Enum8(%s = 1, '" + OTHER_LABEL + "' = 2)) ENGINE = MergeTree ORDER BY id";
    private static final String ELEMENT_DDL_FORMAT = "%s Int32, " + PLAIN_ELEMENT + " String";
    private static final String UNICODE_ESCAPE_FORMAT = "\\u%04x";
    private static final String FIELD_LITERAL_FORMAT = "\"%s\":\"((?:[^\"\\\\]|\\\\.)*)\"";
    private static final ObjectMapper READER = new ObjectMapper();
    private static final ObjectMapper ASCII_WRITER =
            JsonMapper.builder().enable(JsonWriteFeature.ESCAPE_NON_ASCII).build();

    public static final List<Entry> ENTRIES = List.of(
            new Entry(CONTROL_KEY, "_", "control with no escaped character"),
            new Entry("bs", "\\", "backslash"),
            new Entry("bsn", "\\n", "backslash followed by a literal n"),
            new Entry("lf", "\n", "line feed"),
            new Entry("tab", "\t", "tab"),
            new Entry("cr", "\r", "carriage return"),
            new Entry("sl", "/", "slash"),
            new Entry("dq", "\"", "double quote"),
            new Entry("bt", "`", "backtick"),
            new Entry("na", "\u00e9\u65e5\u672c", "non-ASCII text"),
            new Entry("ap", "'", "apostrophe"),
            new Entry("btbsn", "`\\n", "backtick followed by a literal backslash-n (23-REVIEW WR-01)"));

    private RawNameMatrix() {}

    /** The three captured outcomes of one table under one client: the Table, or the failure, and the WARN count. */
    public record Outcome(Table table, String error, int warnCount) {}

    /** The four outcomes of one entry: the names table and the enum table under each client. */
    public record Results(Outcome namesV2, Outcome namesV1, Outcome enumV2, Outcome enumV1) {}

    public static String rootName(Entry e) {
        return "r" + e.fragment() + "z";
    }

    public static String elementName(Entry e) {
        return "x" + e.fragment() + "y";
    }

    public static String label(Entry e) {
        return elementName(e);
    }

    /** The raw text as a backtick-quoted SQL identifier. */
    public static String backtickSql(String raw) {
        return sqlQuoted(raw, '`');
    }

    /** The raw text as a single-quoted SQL string literal. */
    public static String quotedSql(String raw) {
        return sqlQuoted(raw, '\'');
    }

    private static String sqlQuoted(String raw, char quote) {
        StringBuilder quoted = new StringBuilder().append(quote);
        for (char c : raw.toCharArray()) {
            if (c == '\\' || c == quote) {
                quoted.append('\\').append(c);
            } else if (c == '\n') {
                quoted.append("\\n");
            } else if (c == '\t') {
                quoted.append("\\t");
            } else if (c == '\r') {
                quoted.append("\\r");
            } else {
                quoted.append(c);
            }
        }
        return quoted.append(quote).toString();
    }

    /**
     * The server's JSON string escaping of the raw text: the name that today's {@code fromJsonRow}
     * returns for it, because the doubling of every backslash returns the JSON literal's inner text.
     */
    public static String doubledForm(String raw) {
        StringBuilder escaped = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (c == '\\') {
                escaped.append("\\\\");
            } else if (c == '"') {
                escaped.append("\\\"");
            } else if (c == '/') {
                escaped.append("\\/");
            } else if (c == '\n') {
                escaped.append("\\n");
            } else if (c == '\t') {
                escaped.append("\\t");
            } else if (c == '\r') {
                escaped.append("\\r");
            } else if (c < ' ') {
                escaped.append(String.format(UNICODE_ESCAPE_FORMAT, (int) c));
            } else {
                escaped.append(c);
            }
        }
        return escaped.toString();
    }

    /** The key that today's enum label extraction builds for a label: the JSON escaping of its SQL text. */
    public static String legacyLabelKey(String raw) {
        String sql = quotedSql(raw);
        return doubledForm(sql.substring(1, sql.length() - 1));
    }

    /** The CREATE TABLE format (one {@code %s} for the table name) of an entry's names table. */
    public static String namesTableDdl(Entry e) {
        String element = String.format(ELEMENT_DDL_FORMAT, backtickSql(elementName(e)));
        return String.format(
                NAMES_DDL_FORMAT, ID_COLUMN, backtickSql(rootName(e)), ROOT_DEFAULT, element, element, element, element);
    }

    /** The CREATE TABLE format (one {@code %s} for the table name) of an entry's enum table. */
    public static String enumTableDdl(Entry e) {
        return String.format(ENUM_DDL_FORMAT, quotedSql(label(e)));
    }

    public static void createTable(ClickHouseHelperClient chc, String ddlFormat, String tableName) {
        try (Records ignored = chc.queryV2(String.format(ddlFormat, tableName))) {
            // table created
        } catch (Exception e) {
            throw new IllegalStateException("could not create matrix table " + tableName, e);
        }
    }

    /** The Jackson-decoded string value of a field of one DESCRIBE row. */
    public static String decodedField(String line, String field) {
        JsonNode value = parse(line).get(field);
        if (value == null) {
            throw new IllegalStateException("DESCRIBE row has no field " + field);
        }
        return value.asText();
    }

    /** The server's own text between the quotes of a field of one DESCRIBE row (a test oracle only). */
    public static String innerLiteral(String line, String field) {
        Matcher matcher = Pattern.compile(String.format(FIELD_LITERAL_FORMAT, field)).matcher(line);
        if (!matcher.find()) {
            throw new IllegalStateException("DESCRIBE row has no string field " + field);
        }
        return matcher.group(1);
    }

    /** The text as a JSON string with every non-ASCII character escaped. */
    public static String render(String text) {
        try {
            return ASCII_WRITER.writeValueAsString(text);
        } catch (IOException e) {
            throw new IllegalStateException("could not render a cell form", e);
        }
    }

    private static JsonNode parse(String line) {
        try {
            return READER.readTree(line);
        } catch (IOException e) {
            throw new IllegalStateException("DESCRIBE row is not JSON", e);
        }
    }

    private static List<String> decodedNames(List<String> lines) {
        return lines.stream().map(line -> decodedField(line, FIELD_NAME)).collect(Collectors.toList());
    }

    private static Set<String> withFragmentReplaced(Entry e, List<String> lines) {
        return decodedNames(lines).stream()
                .map(name -> name.replace(e.fragment(), "_"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * The server premises that hold before and after the name decode: the DESCRIBE JSON carries the
     * raw identifier. P1: the decoded row names equal the control's once the fragment is replaced by
     * an underscore; P2: the root row and the Tuple element row decode to the raw names; P3: the
     * decoded type of the enum row carries the label in the server's single-quote escaping.
     */
    public static void assertServerPremises(
            Entry e, List<String> namesLines, List<String> enumLines, List<String> controlNamesLines) {
        assertEquals(
                new LinkedHashSet<>(decodedNames(controlNamesLines)),
                withFragmentReplaced(e, namesLines),
                "entry " + e.key() + " premise P1: the decoded row names carry the raw fragment");
        List<String> names = decodedNames(namesLines);
        assertEquals(
                1,
                names.stream().filter(rootName(e)::equals).count(),
                "entry " + e.key() + " premise P2: one row decodes to the raw root name");
        assertEquals(
                1,
                names.stream().filter((TUPLE_COLUMN + NAME_SEPARATOR + elementName(e))::equals).count(),
                "entry " + e.key() + " premise P2: one row decodes to the raw Tuple element name");
        String enumType = enumLines.stream()
                .filter(line -> ENUM_COLUMN.equals(decodedField(line, FIELD_NAME)))
                .map(line -> decodedField(line, FIELD_TYPE))
                .findFirst()
                .orElse("");
        assertTrue(
                enumType.contains(quotedSql(label(e))),
                "entry " + e.key() + " premise P3: the decoded enum type carries the label");
    }

    /** Describes a table under a client and counts the WARN events the discovery logs. */
    public static Outcome describe(ClickHouseHelperClient chc, String tableName) {
        try (LogCapture helperLog = LogCapture.attach(ClickHouseHelperClient.class);
                LogCapture nullabilityLog = LogCapture.attach(DeclaredNullability.class)) {
            Table table;
            String error = null;
            try {
                table = chc.describeTable(chc.getDatabase(), tableName);
            } catch (Throwable t) {
                table = null;
                error = t.getClass().getSimpleName();
            }
            return new Outcome(
                    table,
                    error,
                    helperLog.events(WARN_LEVEL).size() + nullabilityLog.events(WARN_LEVEL).size());
        }
    }

    /** The CELL lines (one per shape) and WARNS lines (one per table and client) of an entry. */
    public static List<String> renderCells(Entry e, List<String> namesLines, List<String> enumLines, Results results) {
        List<String> out = new ArrayList<>();
        for (String shape : SHAPES) {
            boolean enumShape = SHAPE_ENUM_LABEL.equals(shape);
            Optional<String> serverRow = serverRow(e, shape, enumShape ? enumLines : namesLines);
            String field = enumShape ? FIELD_TYPE : FIELD_NAME;
            out.add(String.join(
                    COLUMN_SEPARATOR,
                    CELL,
                    e.key(),
                    shape,
                    serverRow.map(row -> escapeNonAscii(innerLiteral(row, field))).orElse(ABSENT),
                    serverRow.map(row -> decodedCell(shape, row)).orElse(ABSENT),
                    cellForm(shape, enumShape ? results.enumV2() : results.namesV2()),
                    cellForm(shape, enumShape ? results.enumV1() : results.namesV1())));
        }
        out.add(warnsLine(e, KIND_NAMES, CLIENT_V2, results.namesV2()));
        out.add(warnsLine(e, KIND_NAMES, CLIENT_V1, results.namesV1()));
        out.add(warnsLine(e, KIND_ENUM, CLIENT_V2, results.enumV2()));
        out.add(warnsLine(e, KIND_ENUM, CLIENT_V1, results.enumV1()));
        return out;
    }

    private static String warnsLine(Entry e, String kind, String client, Outcome outcome) {
        return String.join(COLUMN_SEPARATOR, WARNS, e.key(), kind, client, String.valueOf(outcome.warnCount()));
    }

    private static Optional<String> serverRow(Entry e, String shape, List<String> lines) {
        return lines.stream()
                .filter(line -> rowMatches(e, shape, decodedField(line, FIELD_NAME)))
                .findFirst();
    }

    private static boolean rowMatches(Entry e, String shape, String decodedName) {
        switch (shape) {
            case SHAPE_ROOT:
                return decodedName.equals(rootName(e));
            case SHAPE_TUPLE:
                return decodedName.equals(TUPLE_COLUMN + NAME_SEPARATOR + elementName(e));
            case SHAPE_ARRAY_TUPLE:
                return decodedName.equals(ARRAY_COLUMN + NAME_SEPARATOR + elementName(e));
            case SHAPE_NULLABLE_TUPLE:
                return decodedName.equals(NULLABLE_COLUMN + NAME_SEPARATOR + elementName(e));
            case SHAPE_MAP_TUPLE:
                return decodedName.startsWith(MAP_COLUMN + NAME_SEPARATOR)
                        && decodedName.endsWith(NAME_SEPARATOR + elementName(e));
            default:
                return decodedName.equals(ENUM_COLUMN);
        }
    }

    /** The decoded form of one server row: the raw column or element name, or the raw first enum label. */
    private static String decodedCell(String shape, String row) {
        if (SHAPE_ENUM_LABEL.equals(shape)) {
            return firstSqlLabel(decodedField(row, FIELD_TYPE)).map(RawNameMatrix::render).orElse(ABSENT);
        }
        String name = decodedField(row, FIELD_NAME);
        return render(SHAPE_ROOT.equals(shape) ? name : name.substring(name.lastIndexOf(NAME_SEPARATOR) + 1));
    }

    /** The SQL-unescaped text of the first single-quoted label of a decoded enum type. */
    private static Optional<String> firstSqlLabel(String decodedType) {
        int start = decodedType.indexOf('\'');
        if (start < 0) {
            return Optional.empty();
        }
        StringBuilder label = new StringBuilder();
        for (int i = start + 1; i < decodedType.length(); i++) {
            char c = decodedType.charAt(i);
            if (c == '\'') {
                return Optional.of(label.toString());
            }
            if (c == '\\' && i + 1 < decodedType.length()) {
                char next = decodedType.charAt(++i);
                label.append(next == 'n' ? '\n' : next == 't' ? '\t' : next == 'r' ? '\r' : next);
            } else {
                label.append(c);
            }
        }
        return Optional.empty();
    }

    private static String cellForm(String shape, Outcome outcome) {
        if (outcome.error() != null) {
            return ERROR_PREFIX + outcome.error();
        }
        if (outcome.table() == null) {
            return UNMAPPED;
        }
        return formOf(shape, outcome.table()).map(RawNameMatrix::render).orElse(ABSENT);
    }

    private static Optional<String> formOf(String shape, Table table) {
        switch (shape) {
            case SHAPE_ROOT:
                return exactlyOne(table.getRootColumnsList().stream()
                        .map(Column::getName)
                        .filter(name -> !CONTAINER_COLUMNS.contains(name))
                        .collect(Collectors.toList()));
            case SHAPE_TUPLE:
                return elementForm(rootNamed(table, TUPLE_COLUMN).map(Column::getTupleFields));
            case SHAPE_ARRAY_TUPLE:
                return elementForm(rootNamed(table, ARRAY_COLUMN)
                        .map(Column::getArrayType)
                        .map(Column::getTupleFields));
            case SHAPE_NULLABLE_TUPLE:
                return elementForm(rootNamed(table, NULLABLE_COLUMN).map(Column::getTupleFields));
            case SHAPE_MAP_TUPLE:
                return elementForm(rootNamed(table, MAP_COLUMN)
                        .map(Column::getMapValueType)
                        .map(Column::getTupleFields));
            default:
                return rootNamed(table, ENUM_COLUMN)
                        .map(Column::getEnumValues)
                        .flatMap(values -> exactlyOne(values.keySet().stream()
                                .filter(key -> !OTHER_LABEL.equals(key))
                                .collect(Collectors.toList())));
        }
    }

    private static Optional<Column> rootNamed(Table table, String name) {
        return table.getRootColumnsList().stream()
                .filter(column -> name.equals(column.getName()))
                .findFirst();
    }

    private static Optional<String> elementForm(Optional<List<Column>> fields) {
        return fields.flatMap(columns -> exactlyOne(columns.stream()
                        .map(Column::getName)
                        .filter(name -> !name.endsWith(ELEMENT_SUFFIX))
                        .collect(Collectors.toList())))
                .map(name -> name.substring(name.lastIndexOf(NAME_SEPARATOR) + 1));
    }

    private static Optional<String> exactlyOne(List<String> candidates) {
        return candidates.size() == 1 ? Optional.of(candidates.get(0)) : Optional.empty();
    }

    private static String escapeNonAscii(String text) {
        StringBuilder escaped = new StringBuilder();
        for (char c : text.toCharArray()) {
            escaped.append(c > 0x7f ? String.format(UNICODE_ESCAPE_FORMAT, (int) c) : String.valueOf(c));
        }
        return escaped.toString();
    }

    /** Writes the lines under the capture directory as UTF-8, one per element and a final line break. */
    public static void writeCapture(String fileName, List<String> lines) throws IOException {
        Path directory = Paths.get(CAPTURE_DIRECTORY);
        Files.createDirectories(directory);
        String content = lines.isEmpty() ? "" : String.join("\n", lines) + "\n";
        Files.write(directory.resolve(fileName), content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The discovery assertion of the matrix, for the tests that run after the name decode: the table
     * is described under the raw names (no fallback warnings, one root column named as written, and
     * every Tuple holding the raw element beside {@code d}, with the nullability the DDL declares).
     */
    public static void assertDiscoveredUnderRawNames(Entry e, Table table, List<LogCapture.CapturedEvent> warnings) {
        assertNotNull(table, "entry " + e.key() + " step table: discovery must map the table");
        assertEquals(
                List.of(),
                warnings.stream().map(LogCapture.CapturedEvent::message).collect(Collectors.toList()),
                "entry " + e.key() + " step warnings: discovery must not warn");
        assertEquals(
                1,
                table.getRootColumnsList().stream()
                        .filter(column -> rootName(e).equals(column.getName()))
                        .count(),
                "entry " + e.key() + " step root: one root column carries the raw name");
        assertTuple(e, "t", rootNamed(table, TUPLE_COLUMN).orElse(null), false);
        assertTuple(
                e,
                "a",
                rootNamed(table, ARRAY_COLUMN).map(Column::getArrayType).orElse(null),
                false);
        assertTuple(e, "n", rootNamed(table, NULLABLE_COLUMN).orElse(null), true);
        assertTuple(
                e,
                "m",
                rootNamed(table, MAP_COLUMN).map(Column::getMapValueType).orElse(null),
                false);
    }

    private static void assertTuple(Entry e, String step, Column tuple, boolean nullable) {
        String where = "entry " + e.key() + " step " + step + ": ";
        assertNotNull(tuple, where + "the Tuple column exists");
        List<Column> fields = tuple.getTupleFields();
        assertNotNull(fields, where + "the column is a Tuple");
        assertEquals(2, fields.size(), where + "the Tuple has exactly two fields");
        assertEquals(
                1,
                fields.stream()
                        .filter(field -> field.getName().endsWith(NAME_SEPARATOR + elementName(e)))
                        .count(),
                where + "one field carries the raw element name");
        assertEquals(
                1,
                fields.stream()
                        .filter(field -> field.getName().endsWith(ELEMENT_SUFFIX))
                        .count(),
                where + "one field is the plain element");
        if (nullable) {
            assertTrue(tuple.isNullable(), where + "the Tuple is nullable");
        }
        assertTrue(
                fields.stream().noneMatch(Column::isNullable), where + "no field of the Tuple is nullable");
    }
}
