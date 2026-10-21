package com.clickhouse.kafka.connect.sink.db.helper;

import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.ParentShape;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fork-only (DESC-07): builds the {@link Table} of one {@code DESCRIBE TABLE} pass from the raw
 * JSONEachRow lines the server returns, one line per {@link #addRow(String)} call.
 *
 * <p>Both describe paths (client V1 and client V2) feed the same instance type, so the per-row
 * skip rules, the column list and the declared types are defined once, and later changes to how a
 * DESCRIBE row becomes a {@link Column} touch a single entry point. An instance is single-use and
 * not thread-safe: create one per DESCRIBE pass.
 */
public final class DescribeTableBuilder {

    // Keeps the logger name of the loop this class replaced, so log configuration and LogCapture
    // attachments that target ClickHouseHelperClient keep matching.
    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseHelperClient.class);

    static final String UNREADABLE_PARENT_WARNING = "Could not read the declared type {} of column {} of table "
            + "{}.{} ({}); its sub-columns named size, size0 or size followed by digits are treated as structural, "
            + "as earlier releases did";

    private static final String NULL_SUFFIX = ".null";
    private static final String SIZE = "size";
    private static final String SIZE_ZERO = "size0";
    private static final char NAME_SEPARATOR = '.';
    private static final String NO_DECLARED_TYPE = "no declared type";
    private static final String UNREADABLE_REASON = "unreadable";
    private static final String ABSENT = "-";

    /**
     * What to do with one DESCRIBE row (DESC-08). A sub-column named {@code size}, {@code size0} or
     * {@code size<digits>} is either a declared Tuple element or a structural row (Array offsets,
     * String length) that ClickHouse names the same way; only the parent's declared type tells them
     * apart.
     */
    enum RowDecision {
        /** Not decided by name: kept as it always was. */
        KEEP,
        /** A declared Tuple element with a structural name: kept and marked for {@link Table}. */
        KEEP_DECLARED,
        /** A structural {@code size<digits>} row: kept for {@link Table} to ignore, as in earlier releases. */
        KEEP_STRUCTURAL_SIZE,
        /** A {@code size<digits>} row whose parent cannot be read: kept unmarked, as in earlier releases. */
        KEEP_UNDECIDABLE,
        /** A row that is never a Column: the null map, or anything below a JSON or Dynamic column. */
        SKIP_STRUCTURAL,
        /** A structural {@code size} or {@code size0} row: dropped, but never added to the skip set. */
        SKIP_STRUCTURAL_SIZE,
        /** A {@code size} or {@code size0} row whose parent cannot be read: dropped, as in earlier releases. */
        SKIP_UNDECIDABLE;

        boolean skipsRow() {
            return this == SKIP_STRUCTURAL || this == SKIP_STRUCTURAL_SIZE || this == SKIP_UNDECIDABLE;
        }

        /** Only rows whose own children are structural too; a size-named twin must not hide its declared sibling's children. */
        boolean entersSkipSet() {
            return this == SKIP_STRUCTURAL || this == SKIP_UNDECIDABLE;
        }

        boolean registersRawType() {
            return this == KEEP || this == KEEP_DECLARED;
        }

        boolean isUndecidable() {
            return this == KEEP_UNDECIDABLE || this == SKIP_UNDECIDABLE;
        }
    }

    private final String database;
    private final String tableName;
    private final Set<String> skippedCols = new HashSet<>();
    private final List<Column> columns = new ArrayList<>();
    private final Map<String, Column> columnsByName = new HashMap<>();
    private final Map<String, String> declaredTypes = new HashMap<>();
    // The raw DESCRIBE type of every kept row, not only of roots: a size-named row is decided by its parent's type.
    private final Map<String, String> rawTypes = new HashMap<>();
    private final Set<String> warnedParents = new HashSet<>();
    private boolean hasDefaults = false;
    private int numColumns = 0;

    public DescribeTableBuilder(String database, String tableName) {
        this.database = database;
        this.tableName = tableName;
    }

    /**
     * Consumes one raw DESCRIBE row.
     *
     * @return false when the row describes a column the connector cannot handle, in which case the
     *     whole table must be ignored; true otherwise, including for skipped rows
     * @throws JsonProcessingException when the line is not a well-formed DESCRIBE row
     */
    public boolean addRow(String jsonLine) throws JsonProcessingException {
        ClickHouseFieldDescriptor fieldDescriptor = ClickHouseFieldDescriptor.fromJsonRow(jsonLine);
        // Count what system.columns counts: top-level columns, including the ones skipped below
        if (!fieldDescriptor.isSubcolumn()) {
            numColumns++;
        }

        if (fieldDescriptor.isAlias() || fieldDescriptor.isMaterialized() || fieldDescriptor.isEphemeral()) {
            LOGGER.debug(
                    "Skipping column {} as it is either an alias, materialized view, or ephemeral",
                    fieldDescriptor.getName());
            skippedCols.add(fieldDescriptor.getName());
            return true;
        }
        if (fieldDescriptor.isSubcolumn()) {
            String parentName = fieldDescriptor.getName().substring(0, fieldDescriptor.getName().lastIndexOf("."));
            if (skippedCols.contains(parentName)) {
                LOGGER.debug(
                        "Skipping subcolumn {} as its parent column is either an alias, materialized view, or ephemeral",
                        fieldDescriptor.getName());
                skippedCols.add(fieldDescriptor.getName());
                return true;
            }
        }

        RowDecision decision = decide(fieldDescriptor, columnsByName, rawTypes);
        if (decision.isUndecidable()) {
            warnUnreadableParent(fieldDescriptor.getName());
        }
        if (decision.skipsRow()) {
            LOGGER.debug("Skipping structural sub-column {}", fieldDescriptor.getName());
            if (decision.entersSkipSet()) {
                skippedCols.add(fieldDescriptor.getName());
            }
            return true;
        }

        if (!fieldDescriptor.isSubcolumn()) {
            declaredTypes.put(fieldDescriptor.getName(), fieldDescriptor.getType());
        }

        if (fieldDescriptor.hasDefault()) {
            hasDefaults = true;
        }

        Column column = Column.extractColumn(fieldDescriptor);
        // If we run into a rare column we can't handle, just ignore the table and warn the user
        if (column == null) {
            LOGGER.error("Unable to handle column: {}", fieldDescriptor.getName());
            return false;
        }
        if (decision == RowDecision.KEEP_DECLARED) {
            LOGGER.debug(
                    "Keeping declared Tuple element {} of column {}",
                    fieldDescriptor.getName(),
                    parentOf(fieldDescriptor.getName()));
            column.setDeclaredElement(true);
        }
        if (decision.registersRawType()) {
            rawTypes.put(fieldDescriptor.getName(), fieldDescriptor.getType());
        }
        columns.add(column);
        columnsByName.put(column.getName(), column);
        return true;
    }

    /**
     * Classifies one DESCRIBE row. Pure: it logs nothing and never throws for a name it cannot split.
     *
     * @param columnsSoFar the Columns kept so far, for the JSON and Dynamic ancestor walk (DESC-05)
     * @param rawTypes the raw DESCRIBE type of every kept row, keyed by name, for a size-named row's parent
     */
    static RowDecision decide(
            ClickHouseFieldDescriptor row, Map<String, Column> columnsSoFar, Map<String, String> rawTypes) {
        if (!row.isSubcolumn()) {
            return RowDecision.KEEP;
        }
        String name = row.getName();
        if (name.endsWith(NULL_SUFFIX) || hasJsonOrDynamicAncestor(name, columnsSoFar)) {
            return RowDecision.SKIP_STRUCTURAL;
        }
        int lastDot = name.lastIndexOf(NAME_SEPARATOR);
        if (lastDot < 0) {
            return RowDecision.KEEP;
        }
        String segment = name.substring(lastDot + 1);
        if (!isSizeName(segment)) {
            return RowDecision.KEEP;
        }
        boolean bare = segment.equals(SIZE) || segment.equals(SIZE_ZERO);
        String parent = parentOf(name);
        String parentType = parent == null ? null : rawTypes.get(parent);
        if (parentType == null) {
            return undecidable(bare);
        }
        // A parent whose type text lacks the segment cannot declare it, so it is never parsed.
        if (!parentType.contains(segment)) {
            return structural(bare);
        }
        ParentShape shape;
        try {
            shape = ParentShape.parse(parent, parentType);
        } catch (RuntimeException e) {
            return undecidable(bare);
        }
        if (!shape.declaresElement(segment)) {
            return structural(bare);
        }
        if (segment.equals(SIZE) || !shape.isOffsetsRow(segment, row.getType())) {
            return RowDecision.KEEP_DECLARED;
        }
        return structural(bare);
    }

    private static RowDecision structural(boolean bare) {
        return bare ? RowDecision.SKIP_STRUCTURAL_SIZE : RowDecision.KEEP_STRUCTURAL_SIZE;
    }

    private static RowDecision undecidable(boolean bare) {
        return bare ? RowDecision.SKIP_UNDECIDABLE : RowDecision.KEEP_UNDECIDABLE;
    }

    /** True for {@code size} and for {@code size} followed by one or more ASCII digits. */
    private static boolean isSizeName(String segment) {
        if (!segment.startsWith(SIZE)) {
            return false;
        }
        for (int i = SIZE.length(); i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private static String parentOf(String name) {
        int lastDot = name.lastIndexOf(NAME_SEPARATOR);
        return lastDot > 0 ? name.substring(0, lastDot) : null;
    }

    private static boolean hasJsonOrDynamicAncestor(String name, Map<String, Column> columnsSoFar) {
        for (int dot = name.lastIndexOf(NAME_SEPARATOR); dot > 0; dot = name.lastIndexOf(NAME_SEPARATOR, dot - 1)) {
            Column ancestor = columnsSoFar.get(name.substring(0, dot));
            if (ancestor != null && (ancestor.getType() == Type.JSON || ancestor.getType() == Type.DYNAMIC)) {
                return true;
            }
        }
        return false;
    }

    /** Metadata only: no throwable, no exception message and no value ever reaches this log line. */
    private void warnUnreadableParent(String rowName) {
        String parent = parentOf(rowName);
        String parentKey = parent == null ? ABSENT : parent;
        if (!warnedParents.add(parentKey)) {
            return;
        }
        String parentType = parent == null ? null : rawTypes.get(parent);
        String reason = NO_DECLARED_TYPE;
        if (parentType != null) {
            reason = UNREADABLE_REASON;
            try {
                ParentShape.parse(parent, parentType);
            } catch (RuntimeException e) {
                reason = e.getClass().getSimpleName();
            }
        }
        LOGGER.warn(
                UNREADABLE_PARENT_WARNING,
                parentType == null ? ABSENT : parentType,
                parentKey,
                database,
                tableName,
                reason);
    }

    /** Builds the {@link Table} from the rows added so far. */
    public Table build() {
        return ClickHouseHelperClient.buildDescribedTable(
                database, tableName, hasDefaults, columns, numColumns, declaredTypes);
    }
}
