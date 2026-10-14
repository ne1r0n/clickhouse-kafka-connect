package com.clickhouse.kafka.connect.sink.db;

import static com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers.newDescriptor;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * COLS-08 Docker-free pins for the {@code Nullable(Enum8)} and {@code Nullable(Enum16)} mapping and
 * its RowBinary bytes (backlog 999.1). Before Phase 19 the Enum branch of {@code
 * Column.extractColumn} dropped {@code Nullable}, so these slots were mapped non-nullable and the
 * writer left out their null marker. This fork-only class sits beside the upstream-identical {@code
 * ColumnTest}, extends nothing and needs no Docker, so it runs in {@code testUnit}. Labels are
 * synthetic ASCII; nothing here logs a record value.
 */
class NullableEnumWriteTest {

    private static final String ENUM8_DEFINITION = "Enum8('a' = 1, 'b' = 2)";
    private static final String ENUM16_DEFINITION = "Enum16('a' = 1, 'k' = 1000)";
    private static final String NULLABLE_ENUM8 = "Nullable(" + ENUM8_DEFINITION + ")";
    private static final String NULLABLE_ENUM16 = "Nullable(" + ENUM16_DEFINITION + ")";
    private static final String UNKNOWN_LABEL = "not_in_the_enum";
    private static final String NOT_NULLABLE_TEXT = "An attempt to write null into not nullable column";
    private static final boolean WITH_DEFAULTS = true;
    private static final boolean WITHOUT_DEFAULTS = false;

    private static ClickHouseWriter newWriter() {
        return new ClickHouseWriter(new SinkTaskStatistics(null, 0));
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }

    private static Data label(String text) {
        return new Data(Schema.OPTIONAL_STRING_SCHEMA, text);
    }

    private static byte[] writeColumn(Column column, Data data, boolean defaultsSupport) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        newWriter().doWriteCol(data, true, column, out, defaultsSupport);
        return out.toByteArray();
    }

    private static Column column(String type) {
        return Column.extractColumn(newDescriptor("c", type));
    }

    @Test
    @DisplayName("COLS-08: Nullable(Enum8) maps to a nullable Enum8 Column that keeps its enum values")
    void nullableEnum8MapsToNullableColumn() {
        Column column = column(NULLABLE_ENUM8);

        assertEquals(Type.Enum8, column.getType());
        assertTrue(column.isNullable());
        assertEquals(Map.of("a", 1, "b", 2), column.getEnumValues());
    }

    @Test
    @DisplayName("COLS-08: Nullable(Enum16) maps to a nullable Enum16 Column that keeps its enum values")
    void nullableEnum16MapsToNullableColumn() {
        Column column = column(NULLABLE_ENUM16);

        assertEquals(Type.Enum16, column.getType());
        assertTrue(column.isNullable());
        assertEquals(Map.of("a", 1, "k", 1000), column.getEnumValues());
    }

    @Test
    @DisplayName("COLS-08: a non-nullable Enum8 and Enum16 still map to non-nullable Columns")
    void nonNullableEnumsStayNonNullable() {
        assertFalse(column(ENUM8_DEFINITION).isNullable());
        assertFalse(column(ENUM16_DEFINITION).isNullable());
    }

    @Test
    @DisplayName("COLS-08: the element of Array(Nullable(Enum16)) is a nullable Enum16 Column")
    void arrayElementOfNullableEnumIsNullable() {
        Column column = column("Array(" + NULLABLE_ENUM16 + ")");

        assertEquals(Type.ARRAY, column.getType());
        assertEquals(Type.Enum16, column.getArrayType().getType());
        assertTrue(column.getArrayType().isNullable());
    }

    @Test
    @DisplayName("COLS-08: the value of Map(String, Nullable(Enum8)) is a nullable Enum8 Column once the .values sub-column is attached")
    void mapValueOfNullableEnumIsNullable() {
        List<Column> columns = List.of(
                Column.extractColumn(newDescriptor("me", "Map(String, " + NULLABLE_ENUM8 + ")")),
                Column.extractColumn(newDescriptor("me.keys", "Array(String)")),
                Column.extractColumn(newDescriptor("me.values", "Array(" + NULLABLE_ENUM8 + ")")));

        Table table = new Table("db", "t", false, columns, columns.size());

        Column mapValue = table.getRootColumnsMap().get("me").getMapValueType();
        assertNotNull(mapValue);
        assertEquals(Type.Enum8, mapValue.getType());
        assertTrue(mapValue.isNullable());
    }

    @Test
    @DisplayName("COLS-08: a Nullable(Enum8) field of a non-nullable Tuple maps to a nullable Enum8 under a non-nullable Tuple")
    void tupleFieldOfNullableEnumIsNullableUnderNonNullableTuple() {
        List<Column> columns = List.of(
                Column.extractColumn(newDescriptor("t", "Tuple(a Nullable(Enum8('q' = 1)))")),
                Column.extractColumn(newDescriptor("t.a", "Nullable(Enum8('q' = 1))")));

        Table table = new Table("db", "t", false, columns, columns.size());

        Column tuple = table.getRootColumnsMap().get("t");
        assertEquals(Type.TUPLE, tuple.getType());
        assertFalse(tuple.isNullable());
        Column field = tuple.getTupleFields().get(0);
        assertEquals(Type.Enum8, field.getType());
        assertTrue(field.isNullable());
    }

    @Test
    @DisplayName("COLS-08: a nullable Enum8 value writes 00 02 without defaults and 00 00 02 with defaults")
    void nullableEnum8ValueBytes() throws IOException {
        Column column = column(NULLABLE_ENUM8);

        assertArrayEquals(bytes(0x00, 0x02), writeColumn(column, label("b"), WITHOUT_DEFAULTS));
        assertArrayEquals(bytes(0x00, 0x00, 0x02), writeColumn(column, label("b"), WITH_DEFAULTS));
    }

    @Test
    @DisplayName("COLS-08: a NULL in a nullable Enum8 slot writes 01 without defaults and 00 01 with defaults")
    void nullableEnum8NullBytes() throws IOException {
        Column column = column(NULLABLE_ENUM8);

        assertArrayEquals(bytes(0x01), writeColumn(column, label(null), WITHOUT_DEFAULTS));
        assertArrayEquals(bytes(0x00, 0x01), writeColumn(column, label(null), WITH_DEFAULTS));
    }

    @Test
    @DisplayName("COLS-08: an Enum16 value with code 1000 writes the marker and the little-endian code 00 E8 03")
    void nullableEnum16ValueBytes() throws IOException {
        Column column = column(NULLABLE_ENUM16);

        assertArrayEquals(bytes(0x00, 0xE8, 0x03), writeColumn(column, label("k"), WITHOUT_DEFAULTS));
        assertArrayEquals(bytes(0x00, 0x00, 0xE8, 0x03), writeColumn(column, label("k"), WITH_DEFAULTS));
    }

    @Test
    @DisplayName("COLS-08: a NULL in a nullable Enum16 slot writes 01 without defaults and 00 01 with defaults")
    void nullableEnum16NullBytes() throws IOException {
        Column column = column(NULLABLE_ENUM16);

        assertArrayEquals(bytes(0x01), writeColumn(column, label(null), WITHOUT_DEFAULTS));
        assertArrayEquals(bytes(0x00, 0x01), writeColumn(column, label(null), WITH_DEFAULTS));
    }

    @Test
    @DisplayName("COLS-08: the boundary codes -128, 127, -32768 and 32767 are written as their two's complement bytes")
    void boundaryCodeBytes() throws IOException {
        Column enum8 = column("Nullable(Enum8('lo' = -128, 'hi' = 127))");
        Column enum16 = column("Nullable(Enum16('lo' = -32768, 'hi' = 32767))");

        assertArrayEquals(bytes(0x00, 0x80), writeColumn(enum8, label("lo"), WITHOUT_DEFAULTS));
        assertArrayEquals(bytes(0x00, 0x7F), writeColumn(enum8, label("hi"), WITHOUT_DEFAULTS));
        assertArrayEquals(bytes(0x00, 0x00, 0x80), writeColumn(enum16, label("lo"), WITHOUT_DEFAULTS));
        assertArrayEquals(bytes(0x00, 0xFF, 0x7F), writeColumn(enum16, label("hi"), WITHOUT_DEFAULTS));
    }

    @Test
    @DisplayName("COLS-08: Array(Nullable(Enum8)) [a, NULL] writes length 2, a marked element and a null marker: 02 00 01 01")
    void arrayOfNullableEnumWithNullEntryBytes() throws IOException {
        Column column = column("Array(" + NULLABLE_ENUM8 + ")");
        Data data = new Data(SchemaBuilder.array(Schema.OPTIONAL_STRING_SCHEMA).build(), Arrays.asList("a", null));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        newWriter().doWriteColValue(column, out, data, WITH_DEFAULTS);

        assertArrayEquals(bytes(0x02, 0x00, 0x01, 0x01), out.toByteArray());
    }

    @Test
    @DisplayName("COLS-08: a label missing from a nullable Enum8 definition still throws, as on a non-nullable column")
    void unknownLabelStillThrows() {
        Column nullable = column(NULLABLE_ENUM8);
        Column nonNullable = column(ENUM8_DEFINITION);

        assertThrows(RuntimeException.class, () -> writeColumn(nullable, label(UNKNOWN_LABEL), WITHOUT_DEFAULTS));
        assertThrows(RuntimeException.class, () -> writeColumn(nonNullable, label(UNKNOWN_LABEL), WITHOUT_DEFAULTS));
    }

    @Test
    @DisplayName("COLS-08: a NULL in a non-nullable Enum8 slot still fails with the not-nullable message")
    void nullInNonNullableEnumStillFails() {
        Column nonNullable = column(ENUM8_DEFINITION);

        for (boolean defaultsSupport : new boolean[] {WITHOUT_DEFAULTS, WITH_DEFAULTS}) {
            RuntimeException failure =
                    assertThrows(RuntimeException.class, () -> writeColumn(nonNullable, label(null), defaultsSupport));
            assertTrue(failure.getMessage().contains(NOT_NULLABLE_TEXT), failure.getMessage());
        }
    }
}
