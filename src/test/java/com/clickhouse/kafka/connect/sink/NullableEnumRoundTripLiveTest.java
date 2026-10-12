package com.clickhouse.kafka.connect.sink;

import static com.clickhouse.kafka.connect.sink.NullableRoundTrip.DEFAULT_VALUE_TEXT;
import static com.clickhouse.kafka.connect.sink.NullableRoundTrip.FALSE_TEXT;
import static com.clickhouse.kafka.connect.sink.NullableRoundTrip.TRUE_TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.kafka.connect.sink.NullableRoundTrip.Cell;
import com.clickhouse.kafka.connect.sink.NullableRoundTrip.Twin;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * COLS-08 live round trips for {@code Nullable(Enum8)} and {@code Nullable(Enum16)} (backlog 999.1).
 * Before Phase 19 the Enum branch of {@code Column.extractColumn} dropped {@code Nullable}, so the
 * writer left out the null marker for these slots. Every assertion is a typed SQL read-back (label
 * and numeric code) on both twin tables: the plain table (RowBinary) and the table with a DEFAULT
 * column (RowBinaryWithDefaults). Uses {@link NullableRoundTrip}. Values are synthetic ASCII labels
 * and nothing here logs a record value.
 */
public class NullableEnumRoundTripLiveTest extends ClickHouseBase {

    private static final String ROW_ID_COLUMN = "id";
    private static final String ENUM_COLUMN = "ne";
    private static final String DEFAULT_COLUMN = "d";
    private static final String CONNECTOR_NAME_PROP = "name";
    private static final String TOP_LEVEL_TABLE_PREFIX = "ne_top_";

    static final String TOP_LEVEL_DDL = "ne Nullable(Enum8('a' = 1, 'b' = 2))";

    private static Cell cell(String alias, String sqlExpression, String expected) {
        return new Cell(alias, sqlExpression, expected);
    }

    private static List<Cell> withDefaultColumn(List<Cell> cells, Twin twin) {
        if (!twin.hasDefaults()) {
            return cells;
        }
        List<Cell> withDefault = new ArrayList<>(cells);
        withDefault.add(cell(DEFAULT_COLUMN, DEFAULT_COLUMN, DEFAULT_VALUE_TEXT));
        return withDefault;
    }

    @ParameterizedTest(name = "COLS-08 top-level Nullable(Enum8) {0}")
    @EnumSource(Twin.class)
    void topLevelNullableEnum8RoundTrips(Twin twin, TestInfo testInfo) throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName(TOP_LEVEL_TABLE_PREFIX + twin);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        // Create the table BEFORE start() - ClickHouseWriter.start() fails against an empty database.
        NullableRoundTrip.createTable(chc, tableName, twin.ddl(TOP_LEVEL_DDL));
        props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
        try {
            assertEquals(
                    twin.hasDefaults(),
                    chc.describeTable(chc.getDatabase(), tableName).hasDefaults(),
                    "twin precondition: DESCRIBE must report a default exactly on the DEFAULT twin");
            Schema schema = SchemaBuilder.struct()
                    .field(ROW_ID_COLUMN, Schema.INT32_SCHEMA)
                    .field(ENUM_COLUMN, Schema.OPTIONAL_STRING_SCHEMA)
                    .build();
            List<SinkRecord> records = List.of(
                    new SinkRecord(
                            tableName,
                            0,
                            null,
                            null,
                            schema,
                            new Struct(schema).put(ROW_ID_COLUMN, 1).put(ENUM_COLUMN, "b"),
                            1),
                    new SinkRecord(
                            tableName,
                            0,
                            null,
                            null,
                            schema,
                            new Struct(schema).put(ROW_ID_COLUMN, 2).put(ENUM_COLUMN, null),
                            2));
            NullableRoundTrip.putAndStop(props, records);
            NullableRoundTrip.selectAndAssert(
                    chc,
                    tableName,
                    1,
                    withDefaultColumn(
                            List.of(
                                    cell("ne_label", "toString(ne)", "b"),
                                    cell("ne_code", "CAST(ne AS Nullable(Int8))", "2"),
                                    cell("ne_is_null", "isNull(ne)", FALSE_TEXT)),
                            twin));
            NullableRoundTrip.selectAndAssert(
                    chc,
                    tableName,
                    2,
                    withDefaultColumn(List.of(cell("ne_is_null", "isNull(ne)", TRUE_TEXT)), twin));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }
}
