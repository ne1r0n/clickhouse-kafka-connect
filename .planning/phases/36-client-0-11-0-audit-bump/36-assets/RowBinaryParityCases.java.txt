package com.clickhouse.kafka.connect.sink;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Date;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Timestamp;

/**
 * The 48 column-type cases whose RowBinary body {@link RowBinaryClientParityLiveTest} pins: scalars,
 * dates, decimals, enums, every Nullable, LowCardinality and container shape the writer
 * serialises, including the nested Tuple and Map nullable markers. A case is one column of
 * {@code ddlType} plus the Connect schema and values the connector would convert. Non-ASCII text is
 * written as unicode escapes so the bytes do not depend on a source or default encoding. Types the
 * connector does not serialise on any client line (a time-zoned DateTime and a Variant fed plain
 * text) are left out on purpose.
 */
final class RowBinaryParityCases {

    record Case(String name, String ddlType, Schema schema, List<Object> values) {}

    private static final Schema I8 = Schema.INT8_SCHEMA;
    private static final Schema I16 = Schema.INT16_SCHEMA;
    private static final Schema I32 = Schema.INT32_SCHEMA;
    private static final Schema I64 = Schema.INT64_SCHEMA;
    private static final Schema F32 = Schema.FLOAT32_SCHEMA;
    private static final Schema F64 = Schema.FLOAT64_SCHEMA;
    private static final Schema BOOL = Schema.BOOLEAN_SCHEMA;
    private static final Schema STR = Schema.STRING_SCHEMA;
    private static final String UUID_TEXT = "123e4567-e89b-12d3-a456-426614174000";
    private static final long DAY_MS = 86_400_000L;
    private static final java.util.Date EPOCH = new java.util.Date(0L);
    private static final java.util.Date SAMPLE_INSTANT = new java.util.Date(1_700_000_000_123L);
    private static final Schema NULLABLE_TUPLE_AB = SchemaBuilder.struct()
            .field("a", I32)
            .field("b", optional(STR))
            .optional()
            .build();

    private RowBinaryParityCases() {}

    private static Schema optional(Schema s) {
        return new SchemaBuilder(s.type()).optional().build();
    }

    private static Schema optionalLogical(Schema s) {
        SchemaBuilder b = new SchemaBuilder(s.type()).name(s.name()).optional();
        if (s.parameters() != null) {
            b.parameters(s.parameters());
        }
        return b.build();
    }

    private static Schema tupleAb() {
        return SchemaBuilder.struct().field("a", I32).field("b", STR).build();
    }

    private static Struct ab(int a, String b) {
        Struct s = new Struct(tupleAb());
        s.put("a", a);
        s.put("b", b);
        return s;
    }

    private static Struct nullableAb(int a, String b) {
        Struct s = new Struct(NULLABLE_TUPLE_AB);
        s.put("a", a);
        s.put("b", b);
        return s;
    }

    private static Case c(String name, String ddlType, Schema schema, Object... values) {
        return new Case(name, ddlType, schema, Arrays.asList(values));
    }

    private static Map<String, Object> map(Object... keysAndValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            m.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return m;
    }

    static List<Case> all() {
        List<Case> l = new ArrayList<>();
        l.add(c("int8", "Int8", I8, (byte) -5, (byte) 127, (byte) -128));
        l.add(c("int16", "Int16", I16, (short) -5, (short) 32767, (short) -32768));
        l.add(c("int32", "Int32", I32, -5, Integer.MAX_VALUE, Integer.MIN_VALUE));
        l.add(c("int64", "Int64", I64, -5L, Long.MAX_VALUE, Long.MIN_VALUE));
        l.add(c("uint8", "UInt8", I16, (short) 0, (short) 200, (short) 255));
        l.add(c("uint16", "UInt16", I32, 0, 40000, 65535));
        l.add(c("uint32", "UInt32", I64, 0L, 3_000_000_000L, 4_294_967_295L));
        l.add(c("uint64", "UInt64", I64, 0L, Long.MAX_VALUE, 1_234_567_890_123L));
        l.add(c("float32", "Float32", F32, 1.5f, -0.0f, Float.NaN, Float.MAX_VALUE));
        l.add(c("float64", "Float64", F64, 1.5d, -0.0d, Double.NaN, Double.MIN_VALUE));
        l.add(c("bool", "Bool", BOOL, true, false));
        l.add(c("string", "String", STR, "", "hello", "unicodé 中文 😀", "a\u0000b"));
        l.add(c("fixedstring8", "FixedString(8)", STR, "abc", "12345678", "é"));
        l.add(c("uuid", "UUID", STR, UUID_TEXT, "00000000-0000-0000-0000-000000000000"));
        l.add(c("enum8", "Enum8('a' = 1, 'b' = 2)", STR, "a", "b"));
        l.add(c("enum16", "Enum16('x' = 1000, 'y' = -2)", STR, "x", "y"));
        l.add(c("date", "Date", Date.SCHEMA, EPOCH, new java.util.Date(DAY_MS * 19_000)));
        l.add(c("date32", "Date32", Date.SCHEMA, new java.util.Date(-DAY_MS * 3), new java.util.Date(DAY_MS * 40_000)));
        l.add(c("datetime", "DateTime", Timestamp.SCHEMA, EPOCH, SAMPLE_INSTANT));
        l.add(c("datetime64_3", "DateTime64(3)", Timestamp.SCHEMA, EPOCH, SAMPLE_INSTANT, new java.util.Date(-1500L)));
        l.add(c("datetime64_6", "DateTime64(6)", Timestamp.SCHEMA, SAMPLE_INSTANT));
        l.add(c("datetime64_9", "DateTime64(9, 'UTC')", Timestamp.SCHEMA, SAMPLE_INSTANT));
        l.add(c("decimal_10_2", "Decimal(10, 2)", Decimal.schema(2), new BigDecimal("12.34"), new BigDecimal("-0.01")));
        l.add(c("decimal_38_10", "Decimal(38, 10)", Decimal.schema(10), new BigDecimal("1234567890123456789.0123456789")));
        l.add(c("nullable_int32", "Nullable(Int32)", optional(I32), 1, null, 3));
        l.add(c("nullable_string", "Nullable(String)", optional(STR), "x", null, ""));
        l.add(c("nullable_dt64", "Nullable(DateTime64(3))", optionalLogical(Timestamp.SCHEMA), new java.util.Date(5L), null));
        l.add(c("nullable_decimal", "Nullable(Decimal(10, 2))",
                new SchemaBuilder(Schema.Type.BYTES).name(Decimal.LOGICAL_NAME).parameter(Decimal.SCALE_FIELD, "2").optional().build(),
                new BigDecimal("1.50"), null));
        l.add(c("nullable_uuid", "Nullable(UUID)", optional(STR), UUID_TEXT, null));
        l.add(c("lowcard_string", "LowCardinality(String)", STR, "a", "b", "a"));
        l.add(c("lowcard_nullable_string", "LowCardinality(Nullable(String))", optional(STR), "a", null));
        l.add(c("array_int32", "Array(Int32)", SchemaBuilder.array(I32).build(), List.of(1, 2, 3), List.of()));
        l.add(c("array_nullable_string", "Array(Nullable(String))", SchemaBuilder.array(optional(STR)).build(), Arrays.asList("a", null, "c"), List.of()));
        l.add(c("array_array_int32", "Array(Array(Int32))", SchemaBuilder.array(SchemaBuilder.array(I32).build()).build(), List.of(List.of(1, 2), List.of()), List.of()));
        l.add(c("array_tuple", "Array(Tuple(a Int32, b String))", SchemaBuilder.array(tupleAb()).build(), List.of(ab(1, "x"), ab(2, "y")), List.of()));
        l.add(c("array_nullable_tuple", "Array(Nullable(Tuple(a Int32, b Nullable(String))))", SchemaBuilder.array(NULLABLE_TUPLE_AB).build(),
                Arrays.asList(nullableAb(1, "x"), null, nullableAb(3, null)), List.of()));
        l.add(c("map_string_int32", "Map(String, Int32)", SchemaBuilder.map(STR, I32).build(), map("k", 1), map()));
        Map<Integer, String> intKeyed = new LinkedHashMap<>();
        intKeyed.put(1, "v");
        l.add(c("map_int32_string", "Map(Int32, String)", SchemaBuilder.map(I32, STR).build(), intKeyed, new LinkedHashMap<>()));
        l.add(c("map_string_nullable_string", "Map(String, Nullable(String))", SchemaBuilder.map(STR, optional(STR)).build(), map("a", "x", "b", null), map()));
        l.add(c("map_string_array", "Map(String, Array(Int32))", SchemaBuilder.map(STR, SchemaBuilder.array(I32).build()).build(), map("k", List.of(1, 2)), map()));
        l.add(c("map_string_map", "Map(String, Map(String, String))", SchemaBuilder.map(STR, SchemaBuilder.map(STR, STR).build()).build(), map("o", map("i", "v")), map()));
        l.add(c("map_string_tuple", "Map(String, Tuple(a Int32, b String))", SchemaBuilder.map(STR, tupleAb()).build(), map("k", ab(1, "x")), map()));
        l.add(c("map_string_nullable_tuple", "Map(String, Nullable(Tuple(a Int32, b Nullable(String))))", SchemaBuilder.map(STR, NULLABLE_TUPLE_AB).build(),
                map("a", nullableAb(1, "x"), "b", null), map()));
        l.add(c("tuple", "Tuple(a Int32, b String)", tupleAb(), ab(1, "x"), ab(-2, "")));
        l.add(nestedTuple());
        l.add(nullableTuple());
        l.add(tupleWithNullableTuple());
        l.add(c("simple_agg", "SimpleAggregateFunction(sum, UInt64)", I64, 1L, 99L));
        return l;
    }

    private static Case nestedTuple() {
        Schema inner = SchemaBuilder.struct().field("d", STR).field("e", optional(I32)).build();
        Schema outer = SchemaBuilder.struct().field("a", I32).field("n", inner).build();
        Struct first = new Struct(outer);
        first.put("a", 1);
        first.put("n", new Struct(inner).put("d", "z").put("e", 5));
        Struct second = new Struct(outer);
        second.put("a", 2);
        second.put("n", new Struct(inner).put("d", "w").put("e", null));
        return c("tuple_nested", "Tuple(a Int32, n Tuple(d String, e Nullable(Int32)))", outer, first, second);
    }

    private static Case nullableTuple() {
        Schema schema = SchemaBuilder.struct().field("a", I32).field("b", STR).optional().build();
        Struct present = new Struct(schema);
        present.put("a", 7);
        present.put("b", "q");
        return c("nullable_tuple", "Nullable(Tuple(a Int32, b String))", schema, present, null);
    }

    private static Case tupleWithNullableTuple() {
        Schema inner = SchemaBuilder.struct().field("d", STR).field("e", optional(I32)).optional().build();
        Schema outer = SchemaBuilder.struct().field("a", I32).field("n", inner).build();
        Struct first = new Struct(outer);
        first.put("a", 1);
        first.put("n", new Struct(inner).put("d", "z").put("e", null));
        Struct second = new Struct(outer);
        second.put("a", 2);
        second.put("n", null);
        return c("tuple_nullable_tuple", "Tuple(a Int32, n Nullable(Tuple(d String, e Nullable(Int32))))", outer, first, second);
    }
}
