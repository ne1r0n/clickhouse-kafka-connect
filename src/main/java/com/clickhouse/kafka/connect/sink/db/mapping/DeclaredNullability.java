package com.clickhouse.kafka.connect.sink.db.mapping;

import com.clickhouse.data.ClickHouseColumn;
import com.clickhouse.data.ClickHouseDataType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fork-only: takes the nullability of every Tuple, Tuple element and nested Tuple from the
 * declared column type instead of from DESCRIBE.
 *
 * <p>Upstream's {@link Column#extractColumn} drops the nullability of a Tuple, and {@code DESCRIBE ...
 * describe_include_subcolumns=1} reports every element of a {@code Nullable(Tuple(...))} as {@code
 * Nullable(...)} although the DDL declares it plain. The writer emits a null marker for each slot
 * its Column reports nullable, so both errors shift the bytes. The declared type string is the only
 * correct source. {@link Table} stays upstream-identical, so this pass runs after the Table has
 * paired the sub-columns with their parents, once per root column per DESCRIBE and never per record.
 *
 * <p>It fails open: a declared type that {@link ClickHouseColumn#of} rejects, a Column tree that
 * does not pair with the parsed tree, or any other unexpected exception in the pass leaves the root
 * exactly as DESCRIBE reported it and logs one WARN with metadata only (column, database, table, raw
 * type and, for an exception, its class name). A null anywhere in the Column tree (a type, a {@code
 * tupleFields} entry, a field or Tuple name) counts as a tree that does not pair, and a null root
 * returns false without a log.
 *
 * <p>A Tuple Column never legitimately lacks a declared element; when one is missing, discovery
 * dropped it (for example a bare {@code size} or {@code size0} element whose parent's declared type
 * could not be read, see {@code DescribeTableBuilder}). That is
 * not a mismatch that fails open: the pass logs one metadata-only WARN naming the unpaired elements
 * and still applies the declared nullability to the elements that paired, because falling back
 * cannot repair the missing element's bytes and would bring back the double flag.
 */
public final class DeclaredNullability {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeclaredNullability.class);

    /** The two characters backslash and n that {@code fromJsonRow} leaves for a server-escaped newline. */
    public static final String ESCAPED_NEWLINE = "\\n";

    static final String FALLBACK_WARNING = "Could not take element nullability for column {} of table {}.{} from its "
            + "declared type {} ({}); keeping the nullability DESCRIBE reported for it";

    static final String UNPAIRED_ELEMENT_WARNING = "Declared Tuple element(s) {} of column {} of table {}.{} "
            + "(declared type {}) have no matching DESCRIBE sub-column; applying the declared nullability to "
            + "the elements that paired";

    private static final String SPACE = " ";
    private static final char BACKSLASH = '\\';
    private static final char BACKTICK = '`';
    private static final char SINGLE_QUOTE = '\'';
    private static final char NO_QUOTE = 0;
    private static final String NAME_SEPARATOR = ".";
    private static final String STRUCTURE_MISMATCH = "structure mismatch";

    private DeclaredNullability() {}

    /** One nullability decision, applied only when the whole root pairs. */
    private record Assignment(Column column, boolean nullable) {}

    /**
     * The multi-line form of a non-nullable Tuple type does not parse until the server's line break
     * is a space. Public so tests normalise exactly as production does.
     *
     * <p>{@code fromJsonRow} keeps JSON escapes as text, and the JSON layer doubles every backslash
     * inside a quoted name or label. So the line break is the only backslash-n outside quotes, and a
     * backslash-n inside a backtick-quoted name or a single-quoted label is part of that name and is
     * kept. Inside quotes a backslash run is copied with the next character, which closes the quote
     * only when half the run length is even. A type with no backslash inside quotes normalises
     * exactly as the plain replace of the line break did. One linear pass; an unterminated quote
     * leaves the rest of the type unchanged.
     */
    public static String normalise(String declaredType) {
        int length = declaredType.length();
        StringBuilder normalised = new StringBuilder(length);
        char quote = NO_QUOTE;
        int i = 0;
        while (i < length) {
            char c = declaredType.charAt(i);
            if (c == BACKSLASH) {
                int end = i;
                while (end < length && declaredType.charAt(end) == BACKSLASH) {
                    end++;
                }
                int run = end - i;
                if (quote == NO_QUOTE && run == 1 && declaredType.startsWith(ESCAPED_NEWLINE, i)) {
                    normalised.append(SPACE);
                    i += ESCAPED_NEWLINE.length();
                    continue;
                }
                normalised.append(declaredType, i, end);
                i = end;
                if (quote != NO_QUOTE && i < length) {
                    char escaped = declaredType.charAt(i);
                    normalised.append(escaped);
                    i++;
                    if (escaped == quote && (run / 2) % 2 == 0) {
                        quote = NO_QUOTE;
                    }
                }
                continue;
            }
            if (quote == NO_QUOTE && (c == BACKTICK || c == SINGLE_QUOTE)) {
                quote = c;
            } else if (c == quote) {
                quote = NO_QUOTE;
            }
            normalised.append(c);
            i++;
        }
        return normalised.toString();
    }

    /** True when {@code column} is, or holds as an Array element or Map value, a Tuple. */
    public static boolean containsTuple(Column column) {
        if (column == null || column.getType() == null) {
            return false;
        }
        switch (column.getType()) {
            case TUPLE:
                return true;
            case ARRAY:
                return containsTuple(column.getArrayType());
            case MAP:
                return containsTuple(column.getMapValueType());
            default:
                return false;
        }
    }

    /**
     * Overwrites {@code isNullable} on the Tuple nodes and leaves of {@code root} from {@code
     * declaredType}, all or none.
     *
     * @return true when the nullability was applied; false when it was left as DESCRIBE reported it
     *     (no root or no declared type, a type that does not parse, trees that do not pair or hold a
     *     null, or an unexpected exception)
     */
    public static boolean apply(Column root, String declaredType, String database, String table) {
        if (root == null || declaredType == null) {
            return false;
        }
        String columnName = root.getName();
        try {
            if (columnName == null) {
                LOGGER.warn(FALLBACK_WARNING, columnName, database, table, declaredType, STRUCTURE_MISMATCH);
                return false;
            }
            ClickHouseColumn parsed = ClickHouseColumn.of(columnName, normalise(declaredType));
            List<Assignment> assignments = new ArrayList<>();
            List<String> unpaired = new ArrayList<>();
            if (!collect(root, parsed, assignments, unpaired)) {
                LOGGER.warn(FALLBACK_WARNING, columnName, database, table, declaredType, STRUCTURE_MISMATCH);
                return false;
            }
            for (Assignment assignment : assignments) {
                assignment.column().setNullable(assignment.nullable());
            }
            if (!unpaired.isEmpty()) {
                LOGGER.warn(UNPAIRED_ELEMENT_WARNING, unpaired, columnName, database, table, declaredType);
            }
            return true;
        } catch (RuntimeException e) {
            // The exception text can echo the type, so only its class name is logged. The assignments
            // are applied only after collect succeeded, so the root is never left half-updated.
            LOGGER.warn(
                    FALLBACK_WARNING, columnName, database, table, declaredType, e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * {@link Column#extractColumn} reads through {@code SimpleAggregateFunction} to the type it holds
     * at every depth, so the declared tree has to be read the same way: the wrapper node itself
     * reports non-nullable even when the type it holds is {@code Nullable(T)}.
     */
    static ClickHouseColumn unwrapSimpleAggregate(ClickHouseColumn parsed) {
        ClickHouseColumn node = parsed;
        while (node.getDataType() == ClickHouseDataType.SimpleAggregateFunction
                && !node.getNestedColumns().isEmpty()) {
            node = node.getNestedColumns().get(0);
        }
        return node;
    }

    private static boolean collect(
            Column column, ClickHouseColumn declared, List<Assignment> into, List<String> unpaired) {
        if (column == null || column.getType() == null) {
            return false;
        }
        ClickHouseColumn parsed = unwrapSimpleAggregate(declared);
        switch (column.getType()) {
            case TUPLE:
                return collectTuple(column, parsed, into, unpaired);
            case ARRAY:
                return parsed.isArray()
                        && column.getArrayType() != null
                        && collect(column.getArrayType(), parsed.getNestedColumns().get(0), into, unpaired);
            case MAP:
                if (!parsed.isMap()) {
                    return false;
                }
                return column.getMapValueType() == null
                        || collect(column.getMapValueType(), parsed.getNestedColumns().get(1), into, unpaired);
            default:
                if (parsed.isTuple() || parsed.isArray() || parsed.isMap()) {
                    return false;
                }
                into.add(new Assignment(column, parsed.isNullable()));
                return true;
        }
    }

    /**
     * The one definition of a Tuple element's key: its declared name, or its one-based position when
     * it is unnamed. It is shared by {@code collectTuple}, discovery's classifier and a future
     * raw-name pairing, so all of them agree on which declared element a DESCRIBE sub-column is.
     *
     * @param element the element as parsed from the declared Tuple type
     * @param index the zero-based position of {@code element} in its Tuple
     */
    public static String elementKey(ClickHouseColumn element, int index) {
        String name = element.getColumnName();
        return name == null || name.isEmpty() ? String.valueOf(index + 1) : name;
    }

    private static boolean collectTuple(
            Column tuple, ClickHouseColumn parsed, List<Assignment> into, List<String> unpaired) {
        if (!parsed.isTuple() || tuple.getName() == null) {
            return false;
        }
        into.add(new Assignment(tuple, parsed.isNullable()));

        List<ClickHouseColumn> elements = parsed.getNestedColumns();
        Map<String, ClickHouseColumn> byKey = new LinkedHashMap<>();
        for (int i = 0; i < elements.size(); i++) {
            byKey.put(elementKey(elements.get(i), i), elements.get(i));
        }

        String prefix = tuple.getName() + NAME_SEPARATOR;
        List<Column> fields = tuple.getTupleFields() == null ? List.of() : tuple.getTupleFields();
        Set<String> paired = new HashSet<>();
        for (Column field : fields) {
            if (field == null || field.getName() == null || !field.getName().startsWith(prefix)) {
                return false;
            }
            String key = field.getName().substring(prefix.length());
            ClickHouseColumn declared = byKey.get(key);
            if (declared == null || !collect(field, declared, into, unpaired)) {
                return false;
            }
            paired.add(key);
        }
        for (String key : byKey.keySet()) {
            if (!paired.contains(key)) {
                unpaired.add(prefix + key);
            }
        }
        return true;
    }
}
