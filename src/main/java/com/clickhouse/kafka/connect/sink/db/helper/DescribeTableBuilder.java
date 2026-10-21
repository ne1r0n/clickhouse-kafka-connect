package com.clickhouse.kafka.connect.sink.db.helper;

import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
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

    private final String database;
    private final String tableName;
    private final Set<String> skippedCols = new HashSet<>();
    private final List<Column> columns = new ArrayList<>();
    private final Map<String, Column> columnsByName = new HashMap<>();
    private final Map<String, String> declaredTypes = new HashMap<>();
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

        if (ClickHouseHelperClient.shouldSkipDescribeRow(fieldDescriptor, columnsByName)) {
            LOGGER.debug("Skipping structural sub-column {}", fieldDescriptor.getName());
            skippedCols.add(fieldDescriptor.getName());
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
        columns.add(column);
        columnsByName.put(column.getName(), column);
        return true;
    }

    /** Builds the {@link Table} from the rows added so far. */
    public Table build() {
        return ClickHouseHelperClient.buildDescribedTable(
                database, tableName, hasDefaults, columns, numColumns, declaredTypes);
    }
}
