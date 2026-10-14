package com.clickhouse.kafka.connect.sink.db.mapping;

import java.util.LinkedHashMap;
import java.util.Map;

/** Test helper: flattens a Column tree into {@code path -> isNullable}. */
public final class ColumnNullabilityPaths {

    public static final String PATH_SEPARATOR = ".";
    public static final String ARRAY_ELEMENT_PATH = "[]";
    public static final String MAP_VALUE_PATH = "{}";

    private ColumnNullabilityPaths() {}

    /**
     * Tuple fields are keyed by the element name left after the tuple's own name, the array element
     * and the map value by a fixed marker.
     */
    public static Map<String, Boolean> byPath(String path, Column column) {
        Map<String, Boolean> into = new LinkedHashMap<>();
        collect(path, column, into);
        return into;
    }

    private static void collect(String path, Column column, Map<String, Boolean> into) {
        into.put(path, column.isNullable());
        switch (column.getType()) {
            case TUPLE -> {
                for (Column field : column.getTupleFields()) {
                    String element = field.getName().substring(column.getName().length() + 1);
                    collect(path + PATH_SEPARATOR + element, field, into);
                }
            }
            case ARRAY -> collect(path + ARRAY_ELEMENT_PATH, column.getArrayType(), into);
            case MAP -> {
                if (column.getMapValueType() != null) {
                    collect(path + MAP_VALUE_PATH, column.getMapValueType(), into);
                }
            }
            default -> {}
        }
    }
}
