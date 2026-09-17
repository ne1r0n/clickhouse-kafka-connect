package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free pins for COLS-01: the {@code jsonInsertColumns} parsing contract in {@link
 * ClickHouseSinkConfig}'s constructor - unset, empty, or whitespace-only parses to an empty
 * {@link List} that is never {@code null}; empty entries between adjacent commas (and a leading
 * or trailing comma) are dropped; surrounding whitespace on each entry is trimmed; source order is
 * preserved exactly (no sort, no dedup). New fork-only class (D-16): it does not extend or modify
 * any upstream test class, and it extends nothing (not {@code ClickHouseBase}) so it needs no
 * Docker and stays included in {@code testUnit} (the inherited {@code @Tag("clickhouse")} on
 * {@code ClickHouseBase} would otherwise drop it out, per Phase 1 D-12).
 */
public class JsonInsertColumnsUnitTest {

    private static Map<String, String> minimalProps() {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConfig.HOSTNAME, "localhost");
        props.put(ClickHouseSinkConfig.PORT, "8123");
        props.put(ClickHouseSinkConfig.DATABASE, "default");
        props.put(ClickHouseSinkConfig.USERNAME, "default");
        props.put(ClickHouseSinkConfig.PASSWORD, "");
        return props;
    }

    private static List<String> columnsFor(String raw) {
        Map<String, String> props = minimalProps();
        if (raw != null) {
            props.put(ClickHouseSinkConfig.JSON_INSERT_COLUMNS, raw);
        }
        return new ClickHouseSinkConfig(props).getJsonInsertColumns();
    }

    @ParameterizedTest(name = "COLS-01: blank/whitespace-only input \"{0}\" parses to an empty list")
    @ValueSource(strings = {"", " ", "   ", ","})
    @DisplayName("COLS-01: an empty, whitespace-only, or lone-comma jsonInsertColumns value parses to an empty list that is never null")
    void blankOrWhitespaceOnlyValueParsesToEmptyList(String raw) {
        List<String> columns = columnsFor(raw);
        assertNotNull(columns);
        assertEquals(List.of(), columns);
    }

    @Test
    @DisplayName("COLS-01: an UNSET jsonInsertColumns key (absent from the map entirely) parses to the same empty, non-null list as a blank value")
    void unsetKeyParsesToEmptyList() {
        // Distinct from the blank/whitespace cases above: the key is entirely absent from the
        // props map, not present with an empty/blank value. Both must yield the same result.
        List<String> columns = columnsFor(null);
        assertNotNull(columns);
        assertEquals(List.of(), columns);
    }

    @Test
    @DisplayName("COLS-01: surrounding whitespace is stripped and an empty entry between two adjacent commas is dropped")
    void paddingStrippedAndAdjacentEmptyEntryDropped() {
        assertEquals(List.of("col_a", "col_b", "col_c"), columnsFor("  col_a, , col_b ,col_c  "));
    }

    @Test
    @DisplayName("COLS-01: a leading comma, a doubled interior comma, and a trailing comma are all dropped in one input")
    void leadingDoubledAndTrailingCommasAreDropped() {
        assertEquals(List.of("col_a", "col_b"), columnsFor(",col_a,,col_b,"));
    }

    @Test
    @DisplayName("COLS-01: source order is preserved exactly - the parser sorts nothing")
    void sourceOrderIsPreservedExactly() {
        assertEquals(List.of("zeta", "alpha", "mid"), columnsFor("zeta,alpha,mid"));
    }

    @Test
    @DisplayName("COLS-01: a single-element value parses to a single-element list")
    void singleElementValueParsesToSingleElementList() {
        assertEquals(List.of("only_one"), columnsFor("only_one"));
    }

    // Deliberately NOT covered (Claude's discretion per CONTEXT.md Deferred Ideas): duplicate
    // entries, backticked or quoted column names, and names containing spaces. None are required
    // by COLS-01, and normalizing, validating, or deduplicating any of them in main code would
    // escalate under D-01 (jsonInsertColumns validation is locked Out of Scope in PROJECT.md).
}
