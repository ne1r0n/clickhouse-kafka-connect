package com.clickhouse.kafka.connect.sink.db.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free pins for {@link Column#extractColumn}'s Array recursion (COLS-07). Fork-only class
 * because {@code ColumnTest} is an upstream file and editing it would conflict with future syncs.
 * Deliberately extends nothing, so it stays in {@code testUnit}.
 */
class ColumnArrayRecursionTest {

    static Stream<Arguments> flagCombinations() {
        return Stream.of(
                Arguments.of(true, true),
                Arguments.of(true, false),
                Arguments.of(false, true),
                Arguments.of(false, false));
    }

    private static void assertFlags(Column column, boolean hasDefault, boolean isSubColumn, String where) {
        assertEquals(hasDefault, column.hasDefault(), where + ": hasDefault");
        assertEquals(isSubColumn, column.isSubColumn(), where + ": isSubColumn");
    }

    @ParameterizedTest(name = "COLS-07: hasDefault={0} isSubColumn={1} reach the Array element column unchanged")
    @MethodSource("flagCombinations")
    @DisplayName("COLS-07: Array element column carries its parent's hasDefault and isSubColumn")
    void arrayElementColumnInheritsParentFlags(boolean hasDefault, boolean isSubColumn) {
        Column array = Column.extractColumn("arr", "Array(Int32)", false, hasDefault, isSubColumn);

        assertEquals(Type.ARRAY, array.getType());
        assertFlags(array, hasDefault, isSubColumn, "array root");
        Column element = array.getArrayType();
        assertEquals(Type.INT32, element.getType());
        assertEquals(1, element.getArrayDepth());
        assertFlags(element, hasDefault, isSubColumn, "array element");
    }

    @Test
    @DisplayName("COLS-07: Array(Array(Int32)) propagates the flags in the right order at depth 1 and depth 2")
    void nestedArrayPropagatesFlagsAtBothDepths() {
        Column outer = Column.extractColumn("arr", "Array(Array(Int32))", false, true, false);

        assertEquals(0, outer.getArrayDepth());
        assertFlags(outer, true, false, "outer array");
        Column inner = outer.getArrayType();
        assertEquals(Type.ARRAY, inner.getType());
        assertEquals(1, inner.getArrayDepth());
        assertFlags(inner, true, false, "inner array");
        Column element = inner.getArrayType();
        assertEquals(Type.INT32, element.getType());
        assertEquals(2, element.getArrayDepth());
        assertFlags(element, true, false, "innermost element");
    }

    @ParameterizedTest(name = "COLS-07: non-array Int32 keeps hasDefault={0} isSubColumn={1}")
    @MethodSource("flagCombinations")
    @DisplayName("COLS-07: a non-array column is unaffected")
    void nonArrayColumnKeepsItsFlags(boolean hasDefault, boolean isSubColumn) {
        Column column = Column.extractColumn("n", "Int32", false, hasDefault, isSubColumn);

        assertEquals(Type.INT32, column.getType());
        assertEquals(0, column.getArrayDepth());
        assertFlags(column, hasDefault, isSubColumn, "non-array column");
    }
}
