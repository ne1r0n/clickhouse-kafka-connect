package com.clickhouse.kafka.connect.sink.db;

import static com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers.newDescriptor;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free unit pins for CR-02: {@code doWriteColValue}'s MAP/TUPLE/VARIANT cases must not NPE
 * when reached with a null {@link Data#getObject()} - the situation a nullable child of an
 * ARRAY/MAP recurses into for a genuine {@code null} entry (e.g. {@code
 * Array(Nullable(Tuple(...)))}, {@code Map(String, Nullable(Map(String, String)))}). The
 * primitive/date/decimal/array cases already null-check and write the RowBinary null marker
 * instead of dereferencing; these three pins prove MAP/TUPLE/VARIANT now do the same instead of
 * throwing a NullPointerException mid-stream. New fork-only class: it does not extend or modify
 * any upstream test class, and it extends nothing (not {@code ClickHouseBase}) so it needs no
 * Docker and is included in {@code testUnit}.
 */
class NestedNullableCompositeWriteTest {

    private static final byte NULL_MARKER = 1;
    private static final byte VARIANT_NULL_DISCRIMINATOR = (byte) 255;

    @Test
    @DisplayName("CR-02: a null MAP value writes the RowBinary null marker instead of NPE-ing on value.getObject()")
    void nullMapValueWritesNullMarker() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        Column column = Column.builder().name("nested_map").type(Type.MAP).isNullable(true).build();
        Data nullData = new Data(Schema.OPTIONAL_STRING_SCHEMA, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertDoesNotThrow(() -> writer.doWriteColValue(column, out, nullData, true));

        assertArrayEquals(new byte[] {NULL_MARKER}, out.toByteArray());
    }

    @Test
    @DisplayName("CR-02: a null TUPLE value writes the RowBinary null marker instead of NPE-ing on value.getObject()")
    void nullTupleValueWritesNullMarker() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        Column column = Column.builder().name("nested_tuple").type(Type.TUPLE).isNullable(true).build();
        Data nullData = new Data(Schema.OPTIONAL_STRING_SCHEMA, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertDoesNotThrow(() -> writer.doWriteColValue(column, out, nullData, true));

        assertArrayEquals(new byte[] {NULL_MARKER}, out.toByteArray());
    }

    @Test
    @DisplayName("CR-02: a null VARIANT value writes the native null discriminator instead of NPE-ing on value.getObject()")
    void nullVariantValueWritesNullDiscriminator() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        Column column = Column.builder().name("nested_variant").type(Type.VARIANT).isNullable(true).build();
        Data nullData = new Data(Schema.OPTIONAL_STRING_SCHEMA, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertDoesNotThrow(() -> writer.doWriteColValue(column, out, nullData, true));

        assertArrayEquals(new byte[] {VARIANT_NULL_DISCRIMINATOR}, out.toByteArray());
    }

    @Test
    @DisplayName("WRT-06/WRT-09: a null Map into a non-nullable MAP column writes upstream #820's empty map in RowBinary")
    void nonNullableMapNullWritesUpstream820EmptyMapInRowBinary() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        Column column = Column.builder().name("m").type(Type.MAP).isNullable(false).build();
        Data nullMap = new Data(Schema.OPTIONAL_STRING_SCHEMA, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        writer.doWriteCol(nullMap, true, column, out, false);

        // 2026-10-07 user-approved tightening of the CR-02 pins (REQUIREMENTS.md, "CR-02 pin tests"):
        // a non-nullable Map takes upstream #820's empty-map semantics, the single size byte
        // doWriteCol writes; only the nullable case keeps the null marker (WRT-06).
        assertArrayEquals(new byte[] {0x00}, out.toByteArray());
    }

    @Test
    @DisplayName("WRT-06/WRT-09: a null Map into a non-nullable MAP column writes upstream #820's empty map in RowBinaryWithDefaults")
    void nonNullableMapNullWritesUpstream820EmptyMapInRowBinaryWithDefaults() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        Column column = Column.builder().name("m").type(Type.MAP).isNullable(false).build();
        Data nullMap = new Data(Schema.OPTIONAL_STRING_SCHEMA, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        writer.doWriteCol(nullMap, true, column, out, true);

        // 2026-10-07 user-approved tightening of the CR-02 pins (REQUIREMENTS.md, "CR-02 pin tests"):
        // the no-default marker, then varint 0 for the empty map, per upstream #820; the nullable
        // case keeps the null marker and is pinned above (WRT-06).
        assertArrayEquals(new byte[] {0x00, 0x00}, out.toByteArray());
    }

    @Test
    @DisplayName("WRT-06/WRT-09: a null Map into a nullable MAP column still writes the null marker through doWriteCol")
    void nullableMapNullThroughDoWriteColKeepsNullMarkerInRowBinary() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        Column column = Column.builder().name("m").type(Type.MAP).isNullable(true).build();
        Data nullMap = new Data(Schema.OPTIONAL_STRING_SCHEMA, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        writer.doWriteCol(nullMap, true, column, out, false);

        // 2026-10-07 user-approved tightening of the CR-02 pins (REQUIREMENTS.md, "CR-02 pin tests"):
        // the nullable case is unchanged and keeps the null marker, while the non-nullable cases
        // above take #820's empty map; the guard scope is WRT-06.
        assertArrayEquals(new byte[] {NULL_MARKER}, out.toByteArray());
    }

    private static final String SECRET_ELEMENT = "distinctive_secret_element";
    private static final String SECRET_MAP_KEY = "distinctive_secret_map_key";
    private static final String NESTED_NULL_TEXT = "null in non-nullable slot";

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

    private static byte[] writeColumnValue(Column col, Data data, boolean defaultsSupport) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        newWriter().doWriteColValue(col, out, data, defaultsSupport);
        return out.toByteArray();
    }

    private static DataException assertNestedNullThrows(Column col, Data data, boolean defaultsSupport) {
        return assertThrows(DataException.class, () -> writeColumnValue(col, data, defaultsSupport));
    }

    private static Column stringArrayColumn() {
        return Column.extractColumn(newDescriptor("tags", "Array(String)"));
    }

    private static Data stringArray(String... elements) {
        return new Data(SchemaBuilder.array(Schema.OPTIONAL_STRING_SCHEMA).build(), Arrays.asList(elements));
    }

    private static Column stringMapColumn() {
        Column valueColumn = Column.builder().name("m.values").type(Type.STRING).build();
        return Column.builder().name("m").type(Type.MAP).mapKeyType(Type.STRING).mapValueType(valueColumn).build();
    }

    private static Data stringMap(Map<String, String> entries) {
        return new Data(SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.OPTIONAL_STRING_SCHEMA).build(), entries);
    }

    private static Column int32Column(String name) {
        return Column.builder().name(name).type(Type.INT32).build();
    }

    private static Column tupleColumn(boolean nullable) {
        return Column.builder()
                .name("c")
                .type(Type.TUPLE)
                .isNullable(nullable)
                .tupleFields(List.of(
                        int32Column("c.a"),
                        Column.builder().name("c.b").type(Type.STRING).build()))
                .build();
    }

    @Test
    @DisplayName("WRT-08: a null element in a non-nullable Array(String) throws a DataException naming column, type and slot, never the data")
    void nullElementInNonNullableArrayThrowsDataExceptionWithoutData() {
        for (boolean defaultsSupport : new boolean[] {true, false}) {
            DataException ex =
                    assertNestedNullThrows(stringArrayColumn(), stringArray(SECRET_ELEMENT, null), defaultsSupport);

            assertEquals("column 'tags' (Array(STRING)) element: " + NESTED_NULL_TEXT, ex.getMessage());
            assertFalse(ex.getMessage().contains(SECRET_ELEMENT));
        }
    }

    @Test
    @DisplayName("WRT-08: the nested-null message is identical whether the null element is first or last, and a lone null throws")
    void nullElementMessageCarriesNoPosition() {
        DataException last = assertNestedNullThrows(stringArrayColumn(), stringArray("x", null), true);
        DataException first = assertNestedNullThrows(stringArrayColumn(), stringArray(null, "x"), true);
        DataException only = assertNestedNullThrows(stringArrayColumn(), stringArray(new String[] {null}), false);

        assertEquals(last.getMessage(), first.getMessage());
        assertEquals(last.getMessage(), only.getMessage());
    }

    @Test
    @DisplayName("WRT-08: an empty Array(String) and an empty Map(String, String) write only their zero size")
    void emptyContainersWriteZeroSizeWithoutThrowing() throws Exception {
        assertArrayEquals(bytes(0x00), writeColumnValue(stringArrayColumn(), stringArray(), false));
        assertArrayEquals(bytes(0x00), writeColumnValue(stringMapColumn(), stringMap(new LinkedHashMap<>()), false));
    }

    @Test
    @DisplayName("WRT-08: a null map value in a non-nullable Map(String, String) throws a DataException without naming the key")
    void nullValueInNonNullableMapThrowsDataExceptionWithoutKey() {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(SECRET_MAP_KEY, null);

        for (boolean defaultsSupport : new boolean[] {true, false}) {
            DataException ex = assertNestedNullThrows(stringMapColumn(), stringMap(entries), defaultsSupport);

            assertTrue(ex.getMessage().contains("column 'm'"), ex.getMessage());
            assertTrue(ex.getMessage().contains("map value"), ex.getMessage());
            assertTrue(ex.getMessage().contains(NESTED_NULL_TEXT), ex.getMessage());
            assertFalse(ex.getMessage().contains(SECRET_MAP_KEY));
        }
    }

    @Test
    @DisplayName("WRT-08: a null field in a non-nullable Tuple slot throws a DataException naming the field, not a RuntimeException")
    void nullNonNullableTupleFieldThrowsDataException() {
        Schema structSchema = SchemaBuilder.struct()
                .field("a", Schema.OPTIONAL_INT32_SCHEMA)
                .field("b", Schema.OPTIONAL_STRING_SCHEMA)
                .build();
        Struct struct = new Struct(structSchema).put("a", 1).put("b", null);

        DataException ex = assertNestedNullThrows(tupleColumn(false), new Data(structSchema, struct), false);

        assertTrue(ex.getMessage().contains("column 'c'"), ex.getMessage());
        assertTrue(ex.getMessage().contains("tuple field 'b'"), ex.getMessage());
        assertTrue(ex.getMessage().contains(NESTED_NULL_TEXT), ex.getMessage());
    }

    @Test
    @DisplayName("D-10: a null reaching a non-nullable TUPLE column throws a DataException, while a nullable one still writes the null marker")
    void nullTupleFollowsColumnNullability() throws Exception {
        Data nullData = new Data(Schema.OPTIONAL_STRING_SCHEMA, null);

        DataException ex = assertNestedNullThrows(tupleColumn(false), nullData, true);

        assertTrue(ex.getMessage().contains("column 'c'"), ex.getMessage());
        assertTrue(ex.getMessage().contains(NESTED_NULL_TEXT), ex.getMessage());
        assertArrayEquals(bytes(NULL_MARKER), writeColumnValue(tupleColumn(true), nullData, true));
    }

    @Test
    @DisplayName("D-16: a null element in Array(Nullable(Tuple(a Int32))) now throws, because Column reports the Tuple non-nullable")
    void nullElementInArrayOfNullableTupleThrowsBecauseColumnDropsTupleNullability() {
        Column arrayColumn = Column.extractColumn(newDescriptor("at", "Array(Nullable(Tuple(a Int32)))"));
        arrayColumn.getArrayType().getTupleFields().add(Column.extractColumn(newDescriptor("at.a", "Nullable(Int32)")));
        assertFalse(arrayColumn.getArrayType().isNullable(), "premise: extractColumn drops Tuple nullability");
        Schema elementSchema = SchemaBuilder.struct()
                .optional()
                .field("a", Schema.OPTIONAL_INT32_SCHEMA)
                .build();
        Map<String, Data> present = new LinkedHashMap<>();
        present.put("a", new Data(Schema.OPTIONAL_INT32_SCHEMA, 5));
        Data data = new Data(SchemaBuilder.array(elementSchema).build(), Arrays.asList(present, null));

        DataException ex = assertNestedNullThrows(arrayColumn, data, false);

        assertTrue(ex.getMessage().contains("column 'at'"), ex.getMessage());
        assertTrue(ex.getMessage().contains("element"), ex.getMessage());
    }

    @Test
    @DisplayName("D-09: a null inner Array in Array(Array(Int32)) is written as an empty array whatever defaultsSupport says")
    void nullInnerArrayIsCoercedToEmptyArray() throws Exception {
        Column column = Column.extractColumn(newDescriptor("aa", "Array(Array(Int32))"));
        Schema innerSchema = SchemaBuilder.array(Schema.OPTIONAL_INT32_SCHEMA).optional().build();
        Data data = new Data(SchemaBuilder.array(innerSchema).build(), Arrays.asList(Arrays.asList(1), null));

        for (boolean defaultsSupport : new boolean[] {true, false}) {
            assertArrayEquals(
                    bytes(0x02, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00), writeColumnValue(column, data, defaultsSupport));
        }
    }

    @Test
    @DisplayName("D-09: a null Array value in Map(String, Array(Int32)) is written as an empty array whatever defaultsSupport says")
    void nullArrayValueInMapIsCoercedToEmptyArray() throws Exception {
        Column valueColumn = Column.builder()
                .name("ma.values")
                .type(Type.ARRAY)
                .arrayType(int32Column("ma.values"))
                .build();
        Column column = Column.builder()
                .name("ma")
                .type(Type.MAP)
                .mapKeyType(Type.STRING)
                .mapValueType(valueColumn)
                .build();
        Map<String, Object> entries = new LinkedHashMap<>();
        entries.put("k", null);
        Schema valueSchema = SchemaBuilder.array(Schema.OPTIONAL_INT32_SCHEMA).optional().build();
        Data data = new Data(SchemaBuilder.map(Schema.STRING_SCHEMA, valueSchema).build(), entries);

        for (boolean defaultsSupport : new boolean[] {true, false}) {
            assertArrayEquals(bytes(0x01, 0x01, 0x6B, 0x00), writeColumnValue(column, data, defaultsSupport));
        }
    }

    @Test
    @DisplayName("D-09: a null Map element in Array(Map(String, Int32)) is written as an empty map whatever defaultsSupport says")
    void nullMapElementInArrayIsCoercedToEmptyMap() throws Exception {
        Column mapColumn = Column.builder()
                .name("am")
                .type(Type.MAP)
                .mapKeyType(Type.STRING)
                .mapValueType(int32Column("am.values"))
                .build();
        Column column = Column.builder().name("am").type(Type.ARRAY).arrayType(mapColumn).build();
        Map<String, Integer> present = new LinkedHashMap<>();
        present.put("a", 1);
        Schema mapSchema = SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.OPTIONAL_INT32_SCHEMA).optional().build();
        Data data = new Data(SchemaBuilder.array(mapSchema).build(), Arrays.asList(present, null));

        for (boolean defaultsSupport : new boolean[] {true, false}) {
            assertArrayEquals(
                    bytes(0x02, 0x01, 0x01, 0x61, 0x01, 0x00, 0x00, 0x00, 0x00),
                    writeColumnValue(column, data, defaultsSupport));
        }
    }

    @Test
    @DisplayName("D-09: nullable nested slots and Variant elements keep writing their null marker or discriminator")
    void nullableNestedSlotsKeepTheirNullEncoding() throws Exception {
        Column nullableStringArray = Column.extractColumn(newDescriptor("t", "Array(Nullable(String))"));
        assertArrayEquals(bytes(0x01, 0x01), writeColumnValue(nullableStringArray, stringArray(new String[] {null}), false));

        Column nullableInt32Map = Column.builder()
                .name("n")
                .type(Type.MAP)
                .mapKeyType(Type.STRING)
                .mapValueType(Column.builder().name("n.values").type(Type.INT32).isNullable(true).build())
                .build();
        Map<String, Integer> entries = new LinkedHashMap<>();
        entries.put("b", null);
        Data mapData = new Data(SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.OPTIONAL_INT32_SCHEMA).build(), entries);
        assertArrayEquals(bytes(0x01, 0x01, 0x62, 0x01), writeColumnValue(nullableInt32Map, mapData, false));

        Column variantArray = Column.extractColumn(newDescriptor("v", "Array(Variant(String, Int64))"));
        assertArrayEquals(
                bytes(0x01, VARIANT_NULL_DISCRIMINATOR), writeColumnValue(variantArray, stringArray(new String[] {null}), false));
    }

    @Test
    @DisplayName("D-11: a top-level null in a non-nullable scalar column keeps upstream's RuntimeException, which is not a DataException")
    void topLevelNullInNonNullableScalarKeepsUpstreamRuntimeException() {
        Column column = Column.builder().name("s").type(Type.STRING).build();
        Data nullData = new Data(Schema.OPTIONAL_STRING_SCHEMA, null);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> newWriter().doWriteCol(nullData, true, column, new ByteArrayOutputStream(), false));

        assertFalse(ex instanceof DataException);
        assertTrue(ex.getMessage().contains("An attempt to write null into not nullable column"), ex.getMessage());
    }
}
