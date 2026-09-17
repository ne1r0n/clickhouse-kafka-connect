package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.data.SchemaType;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.DataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Docker-free unit pins for TYPE-06: the RowBinary write path ({@code doWriteColValue}) and the
 * schema-validation gate ({@code validateDataSchema}) both throw an actionable {@link
 * DataException} naming the column, its type, and the two escape hatches ({@code
 * insertFormat=json}, {@code bypassRowBinary=true}) for unsupported column types ({@code
 * Type.JSON}, {@code Type.DYNAMIC}, {@code Type.UNKNOWN}), instead of silently writing zero
 * bytes into a RowBinary stream the caller keeps filling. New fork-only class (D-16): it does
 * not extend or modify any upstream test class, and it extends nothing (not {@code
 * ClickHouseBase}) so it needs no Docker and is included in {@code testUnit}.
 */
public class RowBinaryUnsupportedTypeGateTest {

    @Test
    @DisplayName(
            "TYPE-06: doWriteColValue throws an actionable DataException for a JSON column and writes nothing to the stream first")
    void doWriteColValueThrowsActionableExceptionForJsonColumnAndWritesNothing() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter();
        Column column = Column.builder().name("json_col").type(Type.JSON).build();
        Data data = new Data(Schema.STRING_SCHEMA, "irrelevant_value");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataException ex =
                assertThrows(
                        DataException.class, () -> writer.doWriteColValue(column, out, data, true));

        assertTrue(ex.getMessage().contains("insertFormat=json"), ex.getMessage());
        assertTrue(ex.getMessage().contains("bypassRowBinary=true"), ex.getMessage());
        assertTrue(ex.getMessage().contains("json_col"), ex.getMessage());

        // The stream-desync proof (D-02/D-03): nothing was written to the output stream before
        // doWriteColValue threw, so a caller streaming subsequent columns can never desync.
        assertEquals(0, out.size());
    }

    @ParameterizedTest(name = "TYPE-06: doWriteColValue throws the actionable message for {0}")
    @EnumSource(
            value = Type.class,
            names = {"JSON", "DYNAMIC", "UNKNOWN"})
    @DisplayName(
            "TYPE-06: doWriteColValue throws the actionable message for JSON, DYNAMIC and UNKNOWN columns (D-03's whole point)")
    void doWriteColValueThrowsForEveryUnsupportedType(Type type) throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter();
        Column column = Column.builder().name("unsupported_col").type(type).build();
        Data data = new Data(Schema.STRING_SCHEMA, "irrelevant_value");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataException ex =
                assertThrows(
                        DataException.class, () -> writer.doWriteColValue(column, out, data, true));

        assertTrue(ex.getMessage().contains("insertFormat=json"), ex.getMessage());
        assertTrue(ex.getMessage().contains("bypassRowBinary=true"), ex.getMessage());
        assertTrue(ex.getMessage().contains("unsupported_col"), ex.getMessage());
        assertEquals(0, out.size());
    }

    @ParameterizedTest(name = "TYPE-06: validateDataSchema throws the actionable message for {0}")
    @EnumSource(
            value = Type.class,
            names = {"JSON", "DYNAMIC", "UNKNOWN"})
    @DisplayName(
            "TYPE-06: validateDataSchema throws the same actionable message for JSON, DYNAMIC and UNKNOWN root columns, and never leaks the record's field value")
    void validateDataSchemaThrowsForEveryUnsupportedTypeAndNeverLeaksTheValue(Type type) {
        Table table = new Table("test_db", "test_table");
        Column column =
                Column.builder().name("unsupported_col").type(type).isNullable(false).hasDefault(false).build();
        table.addColumn(column);

        // A distinctive literal so the "message never contains the record's field value"
        // assertion below has teeth (privacy prohibition, T-03-21).
        String secretFieldValue = "distinctive_secret_value_should_never_leak_into_logs_or_dlq";
        Map<String, Data> jsonMap = new HashMap<>();
        jsonMap.put("unsupported_col", new Data(Schema.STRING_SCHEMA, secretFieldValue));

        Record record =
                Record.newRecord(
                        SchemaType.SCHEMA,
                        "test_topic",
                        0,
                        0L,
                        Collections.emptyList(),
                        jsonMap,
                        "test_db",
                        null);

        ClickHouseWriter writer = new ClickHouseWriter();
        DataException ex =
                assertThrows(DataException.class, () -> writer.validateDataSchema(table, record, false));

        assertTrue(ex.getMessage().contains("insertFormat=json"), ex.getMessage());
        assertTrue(ex.getMessage().contains("bypassRowBinary=true"), ex.getMessage());
        assertTrue(ex.getMessage().contains("unsupported_col"), ex.getMessage());
        assertFalse(ex.getMessage().contains(secretFieldValue), ex.getMessage());
    }
}
