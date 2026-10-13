package com.clickhouse.kafka.connect.sink.db.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseFieldDescriptor;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pins for {@link DeclaredNullability} (COLS-10). Rows are DESCRIBE JSON lines in the
 * form the server sends, read through {@code fromJsonRow}, turned into Columns with {@code
 * Column.extractColumn} and paired by {@link Table}, exactly as discovery does before the pass
 * runs. Deliberately extends nothing, so the class stays in {@code testUnit}.
 */
class DeclaredNullabilityTest {

    private static final String ROW_FORMAT = "{\"name\":\"%s\",\"type\":\"%s\",\"default_type\":\"\","
            + "\"default_expression\":\"\",\"comment\":\"\",\"codec_expression\":\"\","
            + "\"ttl_expression\":\"\",\"is_subcolumn\":%s}";
    private static final String ROOT = "c";
    private static final String PATH_SEPARATOR = ".";
    private static final String ARRAY_ELEMENT_PATH = "[]";
    private static final String MAP_VALUE_PATH = "{}";
    // The two characters backslash and n that the server's escaped newline becomes in a JSON line.
    private static final String SERVER_NEWLINE = "\\n";

    /** One DESCRIBE row; a name holding a dot is a sub-column. */
    private static String row(String name, String type) {
        return String.format(ROW_FORMAT, name, type, name.contains(PATH_SEPARATOR));
    }

    /** Discovery up to the point where the pass runs: Columns paired by an upstream Table. */
    private static Table describe(String... jsonRows) throws Exception {
        List<Column> columns = new ArrayList<>();
        int numColumns = 0;
        for (String jsonRow : jsonRows) {
            ClickHouseFieldDescriptor descriptor = ClickHouseFieldDescriptor.fromJsonRow(jsonRow);
            columns.add(Column.extractColumn(descriptor));
            if (!descriptor.isSubcolumn()) {
                numColumns++;
            }
        }
        return new Table("db", "t", false, columns, numColumns);
    }

    private static Column root(Table table) {
        return table.getRootColumnsMap().get(ROOT);
    }

    /**
     * Flattens a Column tree into {@code path -> isNullable}: tuple fields by the element name left
     * after the tuple's own name, the array element and the map value by a fixed marker.
     */
    private static Map<String, Boolean> nullabilityByPath(Column column) {
        Map<String, Boolean> into = new LinkedHashMap<>();
        collect(column.getName(), column, into);
        return into;
    }

    private static void collect(String path, Column column, Map<String, Boolean> into) {
        into.put(path, column.isNullable());
        switch (column.getType()) {
            case TUPLE -> {
                for (Column field : column.getTupleFields()) {
                    String element = field.getName().substring(column.getName().length() + 1);
                    collect(path + PATH_SEPARATOR + element, field, into);
                }
            }
            case ARRAY -> collect(path + ARRAY_ELEMENT_PATH, column.getArrayType(), into);
            case MAP -> {
                if (column.getMapValueType() != null) {
                    collect(path + MAP_VALUE_PATH, column.getMapValueType(), into);
                }
            }
            default -> {}
        }
    }

    @Test
    @DisplayName("COLS-10: C1 Nullable(Tuple(a Int32, b String)) takes a nullable tuple and non-nullable elements")
    void plainNullableTuplePairsToDeclaredFlags() throws Exception {
        String type = "Nullable(Tuple(a Int32, b String))";
        Column column = root(describe(
                row("c", type), row("c.a", "Nullable(Int32)"), row("c.b", "Nullable(String)")));

        assertTrue(DeclaredNullability.apply(column, type));

        assertEquals(Map.of("c", true, "c.a", false, "c.b", false), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: C2 keeps a declared nullable element beside a non-nullable one in the same tuple")
    void nullableElementBesideNonNullableKeepsEachDeclaredFlag() throws Exception {
        String type = "Nullable(Tuple(a Nullable(Int32), b String))";
        Column column = root(describe(
                row("c", type), row("c.a", "Nullable(Int32)"), row("c.b", "Nullable(String)")));

        assertTrue(DeclaredNullability.apply(column, type));

        assertEquals(Map.of("c", true, "c.a", true, "c.b", false), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: C3 Array(Nullable(Tuple(...))) takes a nullable element tuple with declared element flags")
    void arrayOfNullableTuplePairsToDeclaredFlags() throws Exception {
        String type = "Array(Nullable(Tuple(a Int32, b Nullable(String))))";
        Column column = root(describe(
                row("c", type),
                row("c.a", "Array(Nullable(Int32))"),
                row("c.b", "Array(Nullable(String))")));

        assertTrue(DeclaredNullability.apply(column, type));

        assertEquals(
                Map.of("c", false, "c[]", true, "c[].a", false, "c[].b", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: Map(String, Nullable(Tuple(...))) takes a nullable value tuple with non-nullable elements")
    void mapOfNullableTuplePairsToDeclaredFlags() throws Exception {
        String type = "Map(String, Nullable(Tuple(a Int32, b String)))";
        Column column = root(describe(
                row("c", type),
                row("c.keys", "Array(String)"),
                row("c.values", "Array(Nullable(Tuple(a Int32, b String)))"),
                row("c.values.a", "Array(Nullable(Int32))"),
                row("c.values.b", "Array(Nullable(String))")));

        assertTrue(DeclaredNullability.apply(column, type));

        assertEquals(
                Map.of("c", false, "c{}", true, "c{}.a", false, "c{}.b", false), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: a Tuple declared non-nullable under a Nullable(Tuple) parent stays non-nullable although DESCRIBE wraps it")
    void nestedTupleDeclaredNonNullableIgnoresTheDescribeWrapper() throws Exception {
        String type = "Nullable(Tuple(a Int32, n Tuple(d String, e Nullable(Int32))))";
        Column column = root(describe(
                row("c", type),
                row("c.a", "Nullable(Int32)"),
                row("c.n", "Nullable(Tuple(d String, e Nullable(Int32)))"),
                row("c.n.d", "Nullable(String)"),
                row("c.n.e", "Nullable(Int32)")));
        assertTrue(nullabilityByPath(column).get("c.n"), "premise: DESCRIBE reports the nested tuple as Nullable");

        assertTrue(DeclaredNullability.apply(column, type));

        assertEquals(
                Map.of("c", true, "c.a", false, "c.n", false, "c.n.d", false, "c.n.e", true),
                nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: the multi-line non-nullable Tuple form (backslash plus n) applies after normalisation without a fallback WARN")
    void multiLineTupleFormAppliesWithoutFallback() throws Exception {
        String serverType = "Tuple(" + SERVER_NEWLINE + "    a Int32," + SERVER_NEWLINE + "    b Nullable(String))";
        String rawType = ClickHouseFieldDescriptor.fromJsonRow(row("c", serverType)).getType();
        assertTrue(rawType.contains(SERVER_NEWLINE), "premise: discovery sees the escaped multi-line form");
        Column column = root(describe(row("c", serverType), row("c.a", "Int32"), row("c.b", "Nullable(String)")));

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertTrue(DeclaredNullability.apply(column, rawType));

            assertEquals(List.of(), log.events("WARN"));
        }
        assertEquals(Map.of("c", false, "c.a", false, "c.b", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: containsTuple is false for roots without a Tuple and true for Array or Map roots holding one")
    void containsTupleFollowsTheColumnTree() throws Exception {
        assertFalse(DeclaredNullability.containsTuple(
                root(describe(row("c", "Array(Nullable(Int32))")))));
        assertFalse(DeclaredNullability.containsTuple(root(describe(row("c", "Nullable(String)")))));
        assertTrue(DeclaredNullability.containsTuple(root(describe(
                row("c", "Array(Nullable(Tuple(a Int32)))"), row("c.a", "Array(Nullable(Int32))")))));
        assertTrue(DeclaredNullability.containsTuple(root(describe(
                row("c", "Map(String, Nullable(Tuple(a Int32)))"),
                row("c.keys", "Array(String)"),
                row("c.values", "Array(Nullable(Tuple(a Int32)))"),
                row("c.values.a", "Array(Nullable(Int32))")))));
    }

    @Test
    @DisplayName("COLS-10: unnamed Tuple elements pair by 1-based position")
    void unnamedTupleElementsPairByPosition() throws Exception {
        String type = "Nullable(Tuple(Int32, Nullable(String)))";
        Column column = root(describe(row("c", type), row("c.1", "Nullable(Int32)"), row("c.2", "Nullable(String)")));

        assertTrue(DeclaredNullability.apply(column, type));

        assertEquals(Map.of("c", true, "c.1", false, "c.2", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: backtick element names holding a space or a comma pair by name")
    void backtickElementNamesPairByName() throws Exception {
        String type = "Nullable(Tuple(`a b` Int32, `c,d` Nullable(String)))";
        Column column = root(describe(row("c", type), row("c.a b", "Nullable(Int32)"), row("c.c,d", "Nullable(String)")));

        assertTrue(DeclaredNullability.apply(column, type));

        assertEquals(Map.of("c", true, "c.a b", false, "c.c,d", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: a Tuple Column that lacks one declared element still pairs the others by name")
    void missingDeclaredElementDoesNotShiftPairing() throws Exception {
        String type = "Nullable(Tuple(a Int32, b Nullable(String)))";
        Column column = root(describe(row("c", type), row("c.b", "Nullable(String)")));

        assertTrue(DeclaredNullability.apply(column, type));

        assertEquals(Map.of("c", true, "c.b", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: a declared type that does not parse fails open with one metadata-only WARN")
    void unparsableDeclaredTypeFailsOpenWithOneMetadataWarning() throws Exception {
        String sentinel = "SENTINEL_ELEMENT_NAME";
        String type = "Nullable(Tuple(" + sentinel + " NoSuchType))";
        Column column = root(describe(
                row("c", "Nullable(Tuple(a Int32))"), row("c.a", "Nullable(Int32)")));
        Map<String, Boolean> before = nullabilityByPath(column);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, type));

            List<LogCapture.CapturedEvent> warnings = log.events("WARN");
            assertEquals(1, warnings.size());
            String message = warnings.get(0).message();
            assertTrue(message.contains(ROOT), message);
            assertTrue(message.contains(type), message);
            assertFalse(warnings.get(0).hasThrowable(), "the exception text can echo the type and is never logged");
            assertEquals(1, log.events().size());
        }
        assertEquals(before, nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: a Tuple Column whose declared type is not a Tuple fails open and changes nothing")
    void structureMismatchFailsOpenWithOneWarning() throws Exception {
        String type = "Array(Int32)";
        Column column = root(describe(
                row("c", "Nullable(Tuple(a Int32))"), row("c.a", "Nullable(Int32)")));
        Map<String, Boolean> before = nullabilityByPath(column);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, type));

            List<LogCapture.CapturedEvent> warnings = log.events("WARN");
            assertEquals(1, warnings.size());
            assertTrue(warnings.get(0).message().contains(ROOT), warnings.get(0).message());
            assertTrue(warnings.get(0).message().contains(type), warnings.get(0).message());
            assertFalse(warnings.get(0).hasThrowable());
        }
        assertEquals(before, nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: one unmatched element leaves the whole root untouched, the elements that did pair included")
    void unmatchedElementAppliesNothing() throws Exception {
        String declared = "Tuple(a Nullable(Int32), z String)";
        Column column = root(describe(
                row("c", "Nullable(Tuple(a Int32, b String))"),
                row("c.a", "Nullable(Int32)"),
                row("c.b", "Nullable(String)")));
        Map<String, Boolean> before = nullabilityByPath(column);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, declared));

            assertEquals(1, log.events("WARN").size());
        }
        assertEquals(before, nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: containsTuple is false for a Map or Array root of Enum values and for a null Column")
    void containsTupleIsFalseWithoutATupleAnywhere() throws Exception {
        assertFalse(DeclaredNullability.containsTuple(root(describe(
                row("c", "Map(String, Nullable(Enum8('a' = 1)))"),
                row("c.keys", "Array(String)"),
                row("c.values", "Array(Nullable(Enum8('a' = 1)))")))));
        assertFalse(DeclaredNullability.containsTuple(null));
    }

    @Test
    @DisplayName("COLS-10 CR-01: a Tuple element declared SimpleAggregateFunction(f, Nullable(T)) keeps its nullable flag")
    void simpleAggregateFunctionNullableElementKeepsItsNullability() throws Exception {
        String type = "Tuple(a SimpleAggregateFunction(sum, Nullable(Int64)), b Nullable(Int32))";
        Column column = root(describe(
                row("c", type),
                row("c.a", "SimpleAggregateFunction(sum, Nullable(Int64))"),
                row("c.b", "Nullable(Int32)")));
        assertTrue(
                nullabilityByPath(column).get("c.a"),
                "premise: Column.extractColumn reads through the wrapper to the nullable argument type");

        assertTrue(DeclaredNullability.apply(column, type));

        assertEquals(Map.of("c", false, "c.a", true, "c.b", true), nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10 CR-01: a root SimpleAggregateFunction over a Tuple pairs through the pass and flips no flag")
    void rootSimpleAggregateFunctionOverTuplePairsWithoutFlippingFlags() throws Exception {
        String type = "SimpleAggregateFunction(anyLast, Tuple(a Nullable(Int32), b Int32))";
        Column column = root(describe(
                row("c", type), row("c.a", "Nullable(Int32)"), row("c.b", "Int32")));
        Map<String, Boolean> before = nullabilityByPath(column);
        assertEquals(Type.TUPLE, column.getType(), "premise: the wrapper root is a TUPLE Column");
        assertTrue(DeclaredNullability.containsTuple(column), "premise: discovery routes the root into the pass");
        assertEquals(Map.of("c", false, "c.a", true, "c.b", false), before);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertTrue(DeclaredNullability.apply(column, type));

            assertEquals(List.of(), log.events("WARN"));
        }
        assertEquals(before, nullabilityByPath(column));
    }

    @Test
    @DisplayName("COLS-10: no declared type returns false, changes nothing and logs nothing; normalise leaves other text alone")
    void nullDeclaredTypeIsANoOpAndNormaliseKeepsPlainText() throws Exception {
        Column column = root(describe(
                row("c", "Nullable(Tuple(a Int32))"), row("c.a", "Nullable(Int32)")));
        Map<String, Boolean> before = nullabilityByPath(column);

        try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
            assertFalse(DeclaredNullability.apply(column, null));

            assertEquals(List.of(), log.events());
        }
        assertEquals(before, nullabilityByPath(column));
        String plain = "Nullable(Tuple(a Int32, b String))";
        assertEquals(plain, DeclaredNullability.normalise(plain));
        assertEquals("Tuple( a Int32, b String)", DeclaredNullability.normalise("Tuple(" + SERVER_NEWLINE + "a Int32, b String)"));
    }
}
