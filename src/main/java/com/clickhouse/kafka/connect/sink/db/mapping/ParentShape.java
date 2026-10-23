package com.clickhouse.kafka.connect.sink.db.mapping;

import com.clickhouse.data.ClickHouseColumn;
import java.util.List;

/**
 * Fork-only: reads the declared type of a DESCRIBE parent row to tell a declared Tuple
 * element from a structural sub-column.
 *
 * <p>ClickHouse prints the offsets of an Array ({@code <parent>.size0}), the length of a String
 * ({@code <parent>.size}) and the like as sub-columns named exactly like a Tuple element called
 * {@code size}, {@code size0} or {@code size<digits>}. Only the parent's declared type can say which
 * one a row is: whether the parent is, through its Array wrappers, a Tuple that declares the name,
 * and how many Array levels it has. An instance is immutable.
 */
public final class ParentShape {

    private static final String SIZE_PREFIX = "size";
    private static final String ARRAY_OPEN = "Array(";
    private static final String ARRAY_CLOSE = ")";
    private static final String OFFSETS_ELEMENT_TYPE = "UInt64";

    private final int arrayDepth;
    private final ClickHouseColumn element;

    private ParentShape(int arrayDepth, ClickHouseColumn element) {
        this.arrayDepth = arrayDepth;
        this.element = element;
    }

    /**
     * Parses the parent's raw DESCRIBE type, reading through {@code SimpleAggregateFunction} and every
     * Array level to the type they hold (Nullable and LowCardinality are flags on the parsed node).
     *
     * @throws RuntimeException when the type cannot be parsed; the caller decides what that means
     */
    public static ParentShape parse(String parentName, String rawType) {
        ClickHouseColumn node = DeclaredNullability.unwrapSimpleAggregate(
                ClickHouseColumn.of(parentName, DeclaredNullability.normalise(rawType)));
        int depth = 0;
        while (node.isArray() && !node.getNestedColumns().isEmpty()) {
            depth++;
            node = DeclaredNullability.unwrapSimpleAggregate(node.getNestedColumns().get(0));
        }
        return new ParentShape(depth, node);
    }

    /** The number of Array levels between the parent and the type that holds its elements. */
    public int arrayDepth() {
        return arrayDepth;
    }

    /** True when the parent is a Tuple, under its Array levels, with an element whose key is {@code key}. */
    public boolean declaresElement(String key) {
        if (!element.isTuple()) {
            return false;
        }
        List<ClickHouseColumn> elements = element.getNestedColumns();
        for (int i = 0; i < elements.size(); i++) {
            if (DeclaredNullability.elementKey(elements.get(i), i).equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when a row named {@code segment} of type {@code rowType} is the offsets of one of the
     * parent's Array levels: {@code size<k>} for a level k below {@link #arrayDepth()}, typed {@code
     * UInt64} wrapped in k Array levels. Both sides are built as text, never parsed as numbers, so
     * {@code size01} is never an offsets row.
     */
    public boolean isOffsetsRow(String segment, String rowType) {
        for (int k = 0; k < arrayDepth; k++) {
            if (segment.equals(SIZE_PREFIX + k)
                    && rowType.equals(ARRAY_OPEN.repeat(k) + OFFSETS_ELEMENT_TYPE + ARRAY_CLOSE.repeat(k))) {
                return true;
            }
        }
        return false;
    }
}
