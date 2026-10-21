package com.clickhouse.kafka.connect.sink.db.helper;

import com.clickhouse.client.api.query.QueryResponse;
import com.clickhouse.client.api.query.QuerySettings;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The committed DDL list (DESC-06, D-05) behind the DESCRIBE fixtures of Phase 26, and the one
 * reader and renderer that the capture harness, the drift premise and the live round-trip tests
 * share. Every fixture is a table whose single data column (beside {@code id}) has the stated
 * shape; the {@code .jsonl} next to it in {@code src/test/resources/describe-fixtures} holds the
 * server's raw DESCRIBE rows and the {@code .columns.txt} the Column snapshot of the production
 * {@code describeTable}. Keys and column names are backslash-free by construction. Static members
 * only, no state.
 */
public final class DescribeFixtureCatalog {

    public static final String RESOURCE_DIRECTORY = "describe-fixtures";
    public static final String JSONL_SUFFIX = ".jsonl";
    public static final String COLUMNS_SUFFIX = ".columns.txt";
    public static final String SERVER_VERSION_FILE = "SERVER_VERSION.txt";

    private static final String DESCRIBE_QUERY_FORMAT = "DESCRIBE TABLE `%s`.`%s`";
    private static final String DESCRIBE_INCLUDE_SUBCOLUMNS = "describe_include_subcolumns";
    private static final String CREATE_TABLE_FORMAT =
            "CREATE TABLE %s (id Int32, %s) ENGINE = MergeTree ORDER BY id";
    private static final String RESOURCE_PATH_FORMAT = "/" + RESOURCE_DIRECTORY + "/%s";
    private static final String NULL_TABLE_LINE = "table=null";
    private static final String TREE_INDENT = "  ";
    private static final String ARRAY_MARKER = "[] ";
    private static final String MAP_MARKER = "{} ";
    private static final String NO_MARKER = "";

    /** Where a fixture comes from, so the readers of the evidence know its provenance. */
    public enum Source {
        E15_AFFECTED,
        E15_LOOKALIKE,
        E15_OVERFLAG,
        SPIKE_SHAPE,
        EXTRA_SHAPE,
        R01_CRAFTED
    }

    /** One fixture: the table name, its provenance and the column DDL that follows {@code id Int32,}. */
    public record Fixture(String key, Source source, String columnsDdl) {}

    public static final List<Fixture> FIXTURES = List.of(
            new Fixture("s4_a01", Source.E15_AFFECTED, "c Tuple(size UInt16, v String)"),
            new Fixture("s4_a02", Source.E15_AFFECTED, "c Tuple(size0 UInt8, v String)"),
            new Fixture("s4_a03", Source.E15_AFFECTED, "c Nullable(Tuple(size UInt8, v String))"),
            new Fixture("s4_a04", Source.E15_AFFECTED, "c Array(Tuple(size UInt16, v String))"),
            new Fixture("s4_a05", Source.E15_AFFECTED, "c Map(String, Tuple(size UInt8, v String))"),
            new Fixture("s4_a06", Source.E15_AFFECTED, "c Tuple(x Tuple(size UInt8, v String))"),
            new Fixture("s4_a07", Source.E15_AFFECTED, "c Tuple(x Array(Tuple(size0 UInt8)))"),
            new Fixture("s4_a08", Source.E15_AFFECTED, "c Array(Tuple(size1 UInt16, v String))"),
            new Fixture("s4_a09", Source.E15_AFFECTED, "c Array(Tuple(size12 UInt8, v String))"),
            new Fixture("s4_a10", Source.E15_AFFECTED, "c Array(Array(Tuple(size1 UInt8, v String)))"),
            new Fixture("s4_a11", Source.E15_AFFECTED, "c Tuple(x Array(Tuple(size1 UInt8, v String)))"),
            new Fixture("s4_a12", Source.E15_AFFECTED, "c Array(Tuple(x Tuple(size1 UInt8, v String)))"),
            new Fixture("s4_a13", Source.E15_AFFECTED, "c Array(Tuple(v String, size1 UInt16))"),
            new Fixture("s4_u01", Source.E15_LOOKALIKE, "c Tuple(size1 UInt8, v String)"),
            new Fixture("s4_u02", Source.E15_LOOKALIKE, "c Nullable(Tuple(size1 UInt8, v String))"),
            new Fixture("s4_u03", Source.E15_LOOKALIKE, "c Map(String, Tuple(size1 UInt8, v String))"),
            new Fixture("s4_u04", Source.E15_LOOKALIKE, "c Tuple(a Array(UInt8), size1 UInt8)"),
            new Fixture("s4_u05", Source.E15_LOOKALIKE, "c Tuple(sizes UInt8, v String)"),
            new Fixture("s4_u06", Source.E15_LOOKALIKE, "c Tuple(size_x UInt8, v String)"),
            new Fixture("s4_u07", Source.E15_LOOKALIKE, "c Tuple(size_1 UInt8, v String)"),
            new Fixture("s4_u08", Source.E15_LOOKALIKE, "c Tuple(sizeA UInt8, v String)"),
            new Fixture("s4_u09", Source.E15_LOOKALIKE, "c Tuple(Size UInt8, v String)"),
            new Fixture("s4_u10", Source.E15_LOOKALIKE, "c Tuple(xsize UInt8, v String)"),
            new Fixture("s4_u11", Source.E15_LOOKALIKE, "c Tuple(`my size` UInt8, v String)"),
            new Fixture("s4_u12", Source.E15_LOOKALIKE, "size UInt8"),
            new Fixture("s4_u13", Source.E15_LOOKALIKE, "size0 UInt8"),
            new Fixture("s4_u14", Source.E15_LOOKALIKE, "size Array(UInt8)"),
            new Fixture("s4_u15", Source.E15_LOOKALIKE, "n Nested(size UInt8, v String)"),
            new Fixture("s4_u16", Source.E15_LOOKALIKE, "c Enum8('size' = 1, 'size0' = 2)"),
            new Fixture("s4_u17", Source.E15_LOOKALIKE, "c Array(Tuple(e Enum8('x, size1 UInt8' = 1), v String))"),
            new Fixture("s4_u18", Source.E15_LOOKALIKE, "c Tuple(e Enum8('x, size UInt8' = 1), v String)"),
            new Fixture("s4_u19", Source.E15_LOOKALIKE, "c Tuple(UInt8, String)"),
            new Fixture("s4_o01", Source.E15_OVERFLAG, "c Array(Map(String, Tuple(size1 UInt8)))"),
            new Fixture("n01", Source.SPIKE_SHAPE, "c Array(Array(Tuple(size1 UInt8, size0 UInt8)))"),
            new Fixture("n02", Source.SPIKE_SHAPE, "c Array(Tuple(size0 Tuple(a UInt8), v String))"),
            new Fixture("n03", Source.SPIKE_SHAPE, "c Array(Tuple(size0 String, size String))"),
            new Fixture("n04", Source.SPIKE_SHAPE, "c Array(Map(String, Tuple(size UInt8, size0 UInt8)))"),
            new Fixture("n05", Source.SPIKE_SHAPE, "c Tuple(keys UInt8, `values` String)"),
            new Fixture("x01", Source.EXTRA_SHAPE, "c Tuple(size Tuple(a UInt8, b String))"),
            new Fixture("x02", Source.EXTRA_SHAPE, "c Array(Tuple(size Tuple(a UInt8), v String))"),
            new Fixture("x03", Source.EXTRA_SHAPE, "size Tuple(size UInt8, size0 String)"),
            new Fixture("x04", Source.EXTRA_SHAPE, "c Array(Tuple(size1 UInt64, v String))"),
            new Fixture("x05", Source.EXTRA_SHAPE, "c Array(Array(Tuple(size1 UInt64, size0 UInt64)))"),
            new Fixture("x06", Source.EXTRA_SHAPE, "c Array(Nullable(Tuple(size0 UInt8, v String)))"),
            new Fixture("x07", Source.EXTRA_SHAPE, "c Map(String, Array(Tuple(size UInt8, v String)))"),
            new Fixture("x08", Source.EXTRA_SHAPE, "j JSON(a Tuple(size UInt8, v String))"),
            new Fixture("r01", Source.R01_CRAFTED, "c Tuple(size UInt32, v String)"));

    private DescribeFixtureCatalog() {}

    public static Fixture fixture(String key) {
        return FIXTURES.stream()
                .filter(fixture -> fixture.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown DESCRIBE fixture key: " + key));
    }

    public static String createTableSql(Fixture fixture, String qualifiedTableName) {
        return String.format(CREATE_TABLE_FORMAT, qualifiedTableName, fixture.columnsDdl());
    }

    /**
     * Reads DESCRIBE exactly as {@code describeTableV2} does (JSONEachRow, sub-columns included,
     * UTF-8 explicitly) and returns the lines as read, in the server's order.
     */
    public static List<String> describeRawLines(ClickHouseHelperClient chc, String tableName) throws Exception {
        QuerySettings settings = new QuerySettings().setFormat(ClickHouseFormat.JSONEachRow);
        settings.serverSetting(DESCRIBE_INCLUDE_SUBCOLUMNS, "1");
        settings.setDatabase(chc.getDatabase());
        List<String> lines = new ArrayList<>();
        try (QueryResponse response = chc.getClient()
                        .query(String.format(DESCRIBE_QUERY_FORMAT, chc.getDatabase(), tableName), settings)
                        .get();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * The Column snapshot of a described table: the header, the flat list in order and the Column
     * tree of every root. It prints name, type and nullability only, so a later flag that does not
     * change the wire shape does not change a snapshot.
     */
    public static List<String> renderColumns(Table table) {
        if (table == null) {
            return List.of(NULL_TABLE_LINE);
        }
        List<String> lines = new ArrayList<>();
        lines.add("numColumns=" + table.getNumColumns() + " hasDefaults=" + table.hasDefaults());
        for (Column column : table.getAllColumnsList()) {
            lines.add("all " + column.getName() + " " + column.getType());
        }
        for (Column root : table.getRootColumnsList()) {
            renderTree(root, 0, NO_MARKER, lines);
        }
        return lines;
    }

    private static void renderTree(Column column, int depth, String marker, List<String> lines) {
        lines.add("tree " + TREE_INDENT.repeat(depth) + marker + column.getName() + " " + column.getType()
                + " nullable=" + column.isNullable());
        if (column.getArrayType() != null) {
            renderTree(column.getArrayType(), depth + 1, ARRAY_MARKER, lines);
        }
        if (column.getMapValueType() != null) {
            renderTree(column.getMapValueType(), depth + 1, MAP_MARKER, lines);
        }
        if (column.getTupleFields() != null) {
            for (Column field : column.getTupleFields()) {
                renderTree(field, depth + 1, NO_MARKER, lines);
            }
        }
    }

    /** The lines of one committed fixture file, read from the classpath as UTF-8. */
    public static List<String> resourceLines(String fileName) {
        String path = String.format(RESOURCE_PATH_FORMAT, fileName);
        try (InputStream stream = DescribeFixtureCatalog.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("DESCRIBE fixture resource is absent: " + path);
            }
            List<String> lines = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (IOException e) {
            throw new IllegalStateException("DESCRIBE fixture resource is unreadable: " + path, e);
        }
    }
}
