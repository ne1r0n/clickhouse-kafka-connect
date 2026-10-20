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

    /**
     * SQL text of a Tuple whose first element is named with a literal backslash and an {@code n}.
     * Captured live on ClickHouse latest (23-EVIDENCE.md R-02); the D-12 live premise checks it.
     */
    public static final String BACKSLASH_N_NAME_DDL = "Tuple(`a\\\\nb` Int32, d String)";

    /**
     * What DESCRIBE returns for {@link #BACKSLASH_N_NAME_DDL}: the server line breaks stay a lone
     * backslash-n and the name's backslash is doubled by the JSON layer. Captured live on ClickHouse
     * latest (23-EVIDENCE.md R-02); the D-12 live premise checks it.
     */
    public static final String BACKSLASH_N_NAME_RAW = "Tuple(\\n    `a\\\\\\\\nb` Int32,\\n    d String)";

    /**
     * SQL text of a Tuple whose first element name holds a real newline (SQL escape backslash-n).
     * Captured live on ClickHouse latest (23-EVIDENCE.md R-02); the D-12 live premise checks it.
     */
    public static final String REAL_NEWLINE_NAME_DDL = "Tuple(`a\\nb` Int32, d String)";

    /**
     * What DESCRIBE returns for {@link #REAL_NEWLINE_NAME_DDL}.
     * Captured live on ClickHouse latest (23-EVIDENCE.md R-02); the D-12 live premise checks it.
     */
    public static final String REAL_NEWLINE_NAME_RAW = "Tuple(\\n    `a\\\\nb` Int32,\\n    d String)";

    /**
     * SQL text of a Tuple whose Enum8 label holds a literal backslash-n.
     * Captured live on ClickHouse latest (23-EVIDENCE.md R-02); the D-12 live premise checks it.
     */
    public static final String BACKSLASH_N_ENUM_LABEL_DDL = "Tuple(e Enum8('x\\\\ny' = 1, 'z' = 2), d String)";

    /**
     * What DESCRIBE returns for {@link #BACKSLASH_N_ENUM_LABEL_DDL}.
     * Captured live on ClickHouse latest (23-EVIDENCE.md R-02); the D-12 live premise checks it.
     */
    public static final String BACKSLASH_N_ENUM_LABEL_RAW = "Tuple(\\n    e Enum8('x\\\\\\\\ny' = 1, 'z' = 2),\\n    d String)";

    /**
     * SQL text of a Tuple with a literal backslash-n in the names of two nesting levels.
     * Captured live on ClickHouse latest (23-EVIDENCE.md R-02); the D-12 live premise checks it.
     */
    public static final String BACKSLASH_N_NESTED_NAMES_DDL = "Tuple(`p\\\\nq` Tuple(`r\\\\ns` Int32, t Nullable(String)), d String)";

    /**
     * What DESCRIBE returns for {@link #BACKSLASH_N_NESTED_NAMES_DDL}.
     * Captured live on ClickHouse latest (23-EVIDENCE.md R-02); the D-12 live premise checks it.
     */
    public static final String BACKSLASH_N_NESTED_NAMES_RAW = "Tuple(\\n    `p\\\\\\\\nq` Tuple(\\n        `r\\\\\\\\ns` Int32,\\n        t Nullable(String)),\\n    d String)";

    /**
     * SQL text of a Tuple with one name ending in a backslash and one ending in backslash-n.
     * Captured live on ClickHouse latest (23-EVIDENCE.md R-02); the D-12 live premise checks it.
     */
    public static final String TRAILING_BACKSLASH_NAMES_DDL = "Tuple(`e\\\\` Int32, `f\\\\n` Int32)";

    /**
     * What DESCRIBE returns for {@link #TRAILING_BACKSLASH_NAMES_DDL}.
     * Captured live on ClickHouse latest (23-EVIDENCE.md R-02); the D-12 live premise checks it.
     */
    public static final String TRAILING_BACKSLASH_NAMES_RAW = "Tuple(\\n    `e\\\\\\\\` Int32,\\n    `f\\\\\\\\n` Int32)";

    /**
     * The sub-column suffix of {@link #REAL_NEWLINE_NAME_RAW}'s first element: one backslash and an {@code n}.
     * Captured live on ClickHouse latest (23-EVIDENCE.md R-02); the D-12 live premise checks it.
     */
    public static final String REAL_NEWLINE_ELEMENT = "a\\nb";

    /** SQL text of the D-12 live table's column: {@link #BACKSLASH_N_NAME_DDL} wrapped in Nullable (R-02). */
    public static final String NULLABLE_BACKSLASH_N_NAME_DDL = "Nullable(Tuple(`a\\\\nb` Int32, d String))";

    /**
     * DESCRIBE text of {@link #NULLABLE_BACKSLASH_N_NAME_DDL}; a Nullable Tuple has no line break.
     * Captured live on ClickHouse latest (23-EVIDENCE.md R-02); the D-12 live premise checks it.
     */
    public static final String NULLABLE_BACKSLASH_N_NAME_RAW = "Nullable(Tuple(`a\\\\\\\\nb` Int32, d String))";

    /**
     * The sub-column suffix of {@link #BACKSLASH_N_NAME_RAW}'s first element: {@code fromJsonRow}
     * doubles the server's single backslash, so a Kafka field must use this form too (backlog 999.5).
     * Captured live on ClickHouse latest (23-EVIDENCE.md R-02).
     */
    public static final String BACKSLASH_N_ELEMENT = "a\\\\nb";

    private DeclaredNullabilityFixtures() {}

    /**
     * Asserts the column slot of the fallback WARN: the delimited segment, not a bare substring, so
     * that a mutation which moves another argument into the column slot fails here.
     */
    public static void assertWarnNamesColumn(String message, String column) {
        assertTrue(message.contains(String.format(COLUMN_SEGMENT_FORMAT, column)), COLUMN_SEGMENT_FAILURE + message);
    }
}
