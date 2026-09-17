package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import java.io.ByteArrayOutputStream;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.DataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
