package com.clickhouse.kafka.connect.sink.db.helper;

import static com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureCatalog.COLUMNS_SUFFIX;
import static com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureCatalog.JSONL_SUFFIX;
import static com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureCatalog.renderColumns;
import static com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureCatalog.resourceLines;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free characterization of {@link DescribeTableBuilder} (DESC-07): it feeds the server's
 * raw DESCRIBE rows of every committed fixture to the builder and compares the Column snapshot
 * with the {@code .columns.txt} that the pre-extraction production {@code describeTable} wrote.
 * Deliberately extends nothing, so the class stays in {@code testUnit}.
 */
class DescribeFixtureSnapshotUnitTest {

    private static final String DB = "db";
    private static final String TABLE = "t";

    static Stream<String> fixtureKeys() {
        return DescribeFixtureCatalog.FIXTURES.stream().map(DescribeFixtureCatalog.Fixture::key);
    }

    @ParameterizedTest(name = "DESC-07 snapshot: {0}")
    @MethodSource("fixtureKeys")
    @DisplayName("DESC-07 snapshot: the builder over a committed fixture renders the pre-extraction Column snapshot")
    void builderRendersThePreExtractionSnapshot(String key) throws Exception {
        DescribeTableBuilder builder = new DescribeTableBuilder(DB, TABLE);
        for (String line : resourceLines(key + JSONL_SUFFIX)) {
            assertTrue(builder.addRow(line), key + ": every committed row is a column the connector can handle");
        }
        Table table = builder.build();

        assertEquals(
                resourceLines(key + COLUMNS_SUFFIX),
                renderColumns(table),
                key + ": the builder must reproduce the snapshot the pre-extraction production describeTable wrote");
    }
}
