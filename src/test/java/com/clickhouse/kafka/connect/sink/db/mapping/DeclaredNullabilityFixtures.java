package com.clickhouse.kafka.connect.sink.db.mapping;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared fixtures of the declared-nullability tests (Phase 23, D-16). It must never hold a copy of
 * {@code DeclaredNullability.ESCAPED_NEWLINE}; tests read the production constant.
 */
public final class DeclaredNullabilityFixtures {

    /** A column name that no declared type, database or table of these tests contains. */
    public static final String ROOT_SENTINEL = "nullability_root";

    public static final String COLUMN_SEGMENT_FORMAT = "column %s of table";
    public static final String COLUMN_SEGMENT_FAILURE = "WR-02 column segment: ";

    private DeclaredNullabilityFixtures() {}

    /**
     * Asserts the column slot of the fallback WARN: the delimited segment, not a bare substring, so
     * that a mutation which moves another argument into the column slot fails here.
     */
    public static void assertWarnNamesColumn(String message, String column) {
        assertTrue(message.contains(String.format(COLUMN_SEGMENT_FORMAT, column)), COLUMN_SEGMENT_FAILURE + message);
    }
}
