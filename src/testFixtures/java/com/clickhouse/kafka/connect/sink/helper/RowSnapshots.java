package com.clickhouse.kafka.connect.sink.helper;

import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Typed read-back helper shared by the live tests. It snapshots each row's requested columns into a
 * plain {@code Map<String, String>} while the row is still the iterator's current element: the
 * client-v2 {@link GenericRecord} returned by {@link Records}'s iterator is a mutable, reused view
 * over the underlying binary reader buffer, so collecting the {@code GenericRecord} references
 * themselves leaves every reference pointing at whatever row the iterator most recently advanced
 * to, silently aliasing every "row" to the last one read.
 *
 * <p>Callers that need to compare bytes should select {@code hex(...)} in SQL and compare the hex
 * text, rather than decoding with the JVM default charset: the test JVM runs with {@code
 * file.encoding=windows-1252}.
 */
public final class RowSnapshots {

    private RowSnapshots() {}

    /** Returns one map per row, in query order, holding each requested column as a string. */
    public static List<Map<String, String>> select(
            ClickHouseHelperClient chc, String sql, String... columns) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        try (Records records = chc.getClient().queryRecords(sql).get()) {
            for (GenericRecord r : records) {
                Map<String, String> row = new LinkedHashMap<>();
                for (String column : columns) {
                    row.put(column, r.getString(column));
                }
                rows.add(row);
            }
        }
        return rows;
    }
}
