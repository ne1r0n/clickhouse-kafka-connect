package com.clickhouse.kafka.connect.sink.db.mapping;

import static com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers.newDescriptor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free pins for TYPE-01/TYPE-02: {@link Column#extractColumn} already maps {@code JSON} /
 * {@code JSON(...)} to {@link Type#JSON} and {@code Dynamic} / {@code Dynamic(...)} to
 * {@link Type#DYNAMIC} via a plain {@code startsWith} prefix check on the type string
 * (Column.java:152-156) - no main-code change ships with this plan (03-RESEARCH.md).
 *
 * <p>Extends nothing so this class stays outside the {@code @Tag("clickhouse")} hierarchy and is
 * included in the Docker-free {@code testUnit} Gradle task (D-16 test-organization convention).
 */
class JsonDynamicTypeMappingTest {

    @ParameterizedTest(name = "TYPE-01: {0} maps to Type.JSON")
    @ValueSource(strings = {"JSON", "JSON(max_dynamic_paths=10)"})
    @DisplayName("TYPE-01: JSON and parameterized JSON(...) type strings map to Type.JSON")
    void jsonTypeStringsMapToJson(String typeString) {
        Column col = Column.extractColumn(newDescriptor(typeString));
        assertEquals(Type.JSON, col.getType());
    }

    @ParameterizedTest(name = "TYPE-02: {0} maps to Type.DYNAMIC")
    @ValueSource(strings = {"Dynamic", "Dynamic(max_types=8)"})
    @DisplayName("TYPE-02: Dynamic and parameterized Dynamic(...) type strings map to Type.DYNAMIC")
    void dynamicTypeStringsMapToDynamic(String typeString) {
        Column col = Column.extractColumn(newDescriptor(typeString));
        assertEquals(Type.DYNAMIC, col.getType());
    }

    @Test
    @DisplayName("TYPE-02: a Dynamic column with a non-ASCII name still maps to Type.DYNAMIC (dispatch is over the type string only)")
    void dynamicTypeDispatchIgnoresNonAsciiColumnName() {
        // Built at runtime from Cyrillic code points (D-18) - never a Unicode escape literal in source.
        // Cyrillic code points U+0434 U+0438 U+043D U+0430 U+043C U+0438 U+043A U+0430 spell the
        // word for "dynamics" ("dinamika").
        String nonAsciiName = new String(
                new int[] {0x0434, 0x0438, 0x043D, 0x0430, 0x043C, 0x0438, 0x043A, 0x0430}, 0, 8);
        Column col = Column.extractColumn(newDescriptor(nonAsciiName, "Dynamic"));
        assertEquals(Type.DYNAMIC, col.getType());
    }
}
