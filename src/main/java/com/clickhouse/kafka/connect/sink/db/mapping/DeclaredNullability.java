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
    private static final char LINE_FEED = '\n';
    private static final char TAB = '\t';
    private static final char CARRIAGE_RETURN = '\r';
    private static final String SIMPLE_JSON_ESCAPES = "\\\"/bfnrt";
    private static final String SIMPLE_JSON_ESCAPE_CHARS = "\\\"/\b\f\n\r\t";
    private static final int UNICODE_DIGITS = 4;
    private static final int HEX_RADIX = 16;
    private static final char MAX_ASCII = 0x7f;
    private static final String UNNAMED_COLUMN = "<unnamed>";
    private static final String NAME_SEPARATOR = ".";
    private static final String STRUCTURE_MISMATCH = "structure mismatch";

    private DeclaredNullability() {}

    /** One nullability decision, applied only when the whole root pairs. */
    private record Assignment(Column column, boolean nullable) {}

    /**
     * Turns the type text that {@code fromJsonRow} returns into one that {@link ClickHouseColumn#of}
     * parses, with the element names as the identifiers written in {@code CREATE TABLE}.
     *
     * <p>The text carries two escape layers: the server's JSON escaping of the whole value, and the
     * SQL escaping of each quoted name or label inside it. The first is decoded here, by {@link
     * #unescapeJsonLayer}. Then, outside quotes, the line break of the multi-line form of a
     * non-nullable Tuple type becomes a space. Inside a backtick or single quote, a backslash-n,
     * backslash-t or backslash-r (the SQL escapes of a line feed, a tab and a carriage return) become
     * those characters, because the column parser does not read them; every other backslash pair
     * stays for {@code ClickHouseColumn.of}, and a pair is consumed together so an escaped quote never
     * closes the quote.
     *
     * <p>Total: one linear pass over each layer, no exception path. A backslash with nothing after
     * it, an escape the JSON layer does not know and an unterminated quote are copied unchanged. Public
     * so tests normalise exactly as production does.
     */
    public static String normalise(String declaredType) {
        String decoded = unescapeJsonLayer(declaredType);
        int length = decoded.length();
        StringBuilder normalised = new StringBuilder(length);
        char quote = NO_QUOTE;
        int i = 0;
        while (i < length) {
            char c = decoded.charAt(i);
            if (quote == NO_QUOTE) {
                if (c == LINE_FEED) {
                    normalised.append(SPACE);
                } else {
                    if (c == BACKTICK || c == SINGLE_QUOTE) {
                        quote = c;
                    }
                    normalised.append(c);
                }
                i++;
            } else if (c == BACKSLASH && i + 1 < length) {
                normalised.append(quotedEscape(decoded.charAt(i + 1), c));
                i += 2;
            } else {
                if (c == quote) {
                    quote = NO_QUOTE;
                }
                normalised.append(c);
                i++;
            }
        }
        return normalised.toString();
    }

    /** The text of a SQL escape inside a quote: the character for n, t and r, the pair otherwise. */
    private static String quotedEscape(char escaped, char backslash) {
        switch (escaped) {
            case 'n':
                return String.valueOf(LINE_FEED);
            case 't':
                return String.valueOf(TAB);
            case 'r':
                return String.valueOf(CARRIAGE_RETURN);
            default:
                return String.valueOf(backslash) + escaped;
        }
    }

    /**
     * Decodes the JSON escapes of {@code text}: the escaped backslash, double quote and slash, the
     * escapes b, f, n, r and t, and the u escape with four hex digits. Any other or truncated escape is
     * copied as it is. Total and linear. Package-private so that the other parsers of a declared type
     * decode the same layer the same way.
     */
    static String unescapeJsonLayer(String text) {
        if (text.indexOf(BACKSLASH) < 0) {
            return text;
        }
        int length = text.length();
        StringBuilder decoded = new StringBuilder(length);
        int i = 0;
        while (i < length) {
            char c = text.charAt(i);
            if (c != BACKSLASH || i + 1 >= length) {
                decoded.append(c);
                i++;
                continue;
            }
            char escaped = text.charAt(i + 1);
            int simple = SIMPLE_JSON_ESCAPES.indexOf(escaped);
            if (simple >= 0) {
                decoded.append(SIMPLE_JSON_ESCAPE_CHARS.charAt(simple));
                i += 2;
            } else if (escaped == 'u' && isUnicodeEscape(text, i + 2)) {
                decoded.append((char) Integer.parseInt(text.substring(i + 2, i + 2 + UNICODE_DIGITS), HEX_RADIX));
                i += 2 + UNICODE_DIGITS;
            } else {
                decoded.append(c).append(escaped);
                i += 2;
            }
        }
        return decoded.toString();
    }

    private static boolean isUnicodeEscape(String text, int digitsStart) {
        if (digitsStart + UNICODE_DIGITS > text.length()) {
            return false;
        }
        for (int i = digitsStart; i < digitsStart + UNICODE_DIGITS; i++) {
            char digit = text.charAt(i);
            if (digit > MAX_ASCII || Character.digit(digit, HEX_RADIX) < 0) {
                return false;
            }
        }
        return true;
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
     * Overwrites {@code isNullable} on the Tuple nodes and leaves that pair with {@code declaredType}; a
     * structure mismatch applies nothing, while declared Tuple elements missing from the Column tree
     * are logged and skipped.
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
        if (columnName == null) {
            LOGGER.warn(FALLBACK_WARNING, UNNAMED_COLUMN, database, table, declaredType, STRUCTURE_MISMATCH);
            return false;
        }
        try {
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
