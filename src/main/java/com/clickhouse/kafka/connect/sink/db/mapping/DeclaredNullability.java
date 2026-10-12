package com.clickhouse.kafka.connect.sink.db.mapping;

import com.clickhouse.data.ClickHouseColumn;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fork-only (COLS-10): takes the nullability of every Tuple, Tuple element and nested Tuple from the
 * declared column type instead of from DESCRIBE.
 *
 * <p>Upstream's {@link Column#extractColumn} drops the nullability of a Tuple, and {@code DESCRIBE ...
 * describe_include_subcolumns=1} reports every element of a {@code Nullable(Tuple(...))} as {@code
 * Nullable(...)} although the DDL declares it plain. The writer emits a null marker for each slot
 * its Column reports nullable, so both errors shift the bytes. The declared type string is the only
 * correct source. {@link Table} stays upstream-identical, so this pass runs after the Table has
 * paired the sub-columns with their parents, once per root column per DESCRIBE and never per record.
 *
 * <p>It fails open: a declared type that {@link ClickHouseColumn#of} rejects, or a Column tree that
 * does not pair with the parsed tree, leaves the root exactly as DESCRIBE reported it and logs one
 * WARN with metadata only.
 */
public final class DeclaredNullability {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeclaredNullability.class);

    /** The two characters backslash and n that {@code fromJsonRow} leaves for a server-escaped newline. */
    static final String ESCAPED_NEWLINE = "\\n";

    static final String FALLBACK_WARNING = "Could not take element nullability for column {} from its declared "
            + "type {} ({}); keeping the nullability DESCRIBE reported for it";

    private static final String SPACE = " ";
    private static final String NAME_SEPARATOR = ".";
    private static final String STRUCTURE_MISMATCH = "structure mismatch";

    private DeclaredNullability() {}

    /** One nullability decision, applied only when the whole root pairs. */
    private record Assignment(Column column, boolean nullable) {}

    /** The multi-line form of a non-nullable Tuple type does not parse until the escape is a space. */
    static String normalise(String declaredType) {
        return declaredType.replace(ESCAPED_NEWLINE, SPACE);
    }

    /** True when {@code column} is, or holds as an Array element or Map value, a Tuple. */
    public static boolean containsTuple(Column column) {
        if (column == null) {
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
     *     (no declared type, a type that does not parse, or trees that do not pair)
     */
    public static boolean apply(Column root, String declaredType) {
        if (declaredType == null) {
            return false;
        }
        ClickHouseColumn parsed;
        try {
            parsed = ClickHouseColumn.of(root.getName(), normalise(declaredType));
        } catch (RuntimeException e) {
            // The exception text can echo the type, so only its class name is logged.
            LOGGER.warn(FALLBACK_WARNING, root.getName(), declaredType, e.getClass().getSimpleName());
            return false;
        }
        List<Assignment> assignments = new ArrayList<>();
        if (!collect(root, parsed, assignments)) {
            LOGGER.warn(FALLBACK_WARNING, root.getName(), declaredType, STRUCTURE_MISMATCH);
            return false;
        }
        for (Assignment assignment : assignments) {
            assignment.column().setNullable(assignment.nullable());
        }
        return true;
    }

    private static boolean collect(Column column, ClickHouseColumn parsed, List<Assignment> into) {
        switch (column.getType()) {
            case TUPLE:
                return collectTuple(column, parsed, into);
            case ARRAY:
                return parsed.isArray()
                        && column.getArrayType() != null
                        && collect(column.getArrayType(), parsed.getNestedColumns().get(0), into);
            case MAP:
                if (!parsed.isMap()) {
                    return false;
                }
                return column.getMapValueType() == null
                        || collect(column.getMapValueType(), parsed.getNestedColumns().get(1), into);
            default:
                if (parsed.isTuple() || parsed.isArray() || parsed.isMap()) {
                    return false;
                }
                into.add(new Assignment(column, parsed.isNullable()));
                return true;
        }
    }

    private static boolean collectTuple(Column tuple, ClickHouseColumn parsed, List<Assignment> into) {
        if (!parsed.isTuple()) {
            return false;
        }
        into.add(new Assignment(tuple, parsed.isNullable()));

        List<ClickHouseColumn> elements = parsed.getNestedColumns();
        Map<String, ClickHouseColumn> byKey = new HashMap<>();
        for (int i = 0; i < elements.size(); i++) {
            String name = elements.get(i).getColumnName();
            byKey.put(name == null || name.isEmpty() ? String.valueOf(i + 1) : name, elements.get(i));
        }

        String prefix = tuple.getName() + NAME_SEPARATOR;
        List<Column> fields = tuple.getTupleFields() == null ? List.of() : tuple.getTupleFields();
        for (Column field : fields) {
            if (!field.getName().startsWith(prefix)) {
                return false;
            }
            ClickHouseColumn declared = byKey.get(field.getName().substring(prefix.length()));
            if (declared == null || !collect(field, declared, into)) {
                return false;
            }
        }
        return true;
    }
}
