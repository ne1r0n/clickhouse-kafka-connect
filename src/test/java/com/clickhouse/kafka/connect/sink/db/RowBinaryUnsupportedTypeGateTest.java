package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.data.SchemaType;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.DataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Docker-free unit pins for TYPE-06/WRT-02: the RowBinary write path ({@code doWriteColValue})
 * and the schema-validation gate ({@code validateDataSchema}) both throw an actionable {@link
 * DataException} naming the column, its type, and the two escape hatches ({@code
 * insertFormat=json}, {@code bypassRowBinary=true}) for unsupported column types ({@code
 * Type.DYNAMIC}, {@code Type.UNKNOWN}), instead of silently writing zero bytes into a RowBinary
 * stream the caller keeps filling. {@code Type.JSON} is deliberately excluded from that gate per
 * the milestone's locked "RowBinary + JSON" decision: upstream's own flag-gated {@code case
 * JSON:} write path and its matching {@code validateDataSchema} branch now own JSON exclusively,
 * so this class also asserts upstream's exception type/message for JSON under a default
 * (flag-off) configuration. New fork-only class (D-16): it does not extend or modify any
 * upstream test class, and it extends nothing (not {@code ClickHouseBase}) so it needs no Docker
 * and is included in {@code testUnit}.
 */
public class RowBinaryUnsupportedTypeGateTest {

    private static final String WRITER_SINK_CONFIG_FIELD = "csc";

    private static Field writerSinkConfigField() throws NoSuchFieldException {
        Field field = ClickHouseWriter.class.getDeclaredField(WRITER_SINK_CONFIG_FIELD);
        field.setAccessible(true);
        return field;
    }

    private static void setSinkConfig(ClickHouseWriter writer, ClickHouseSinkConfig csc) throws Exception {
        writerSinkConfigField().set(writer, csc);
    }

    // The fork's JSON RowBinary fail-fast was retired per the milestone's locked "RowBinary +
    // JSON" decision (WRT-02): upstream's own flag-gated case JSON: write path and its matching
    // validateDataSchema branch now own JSON exclusively, so this test asserts upstream's
    // exception type/message under a default (flag-off) configuration instead of the fork's
    // DataException. DYNAMIC/UNKNOWN keep the fork's DataException gate below.
    @Test
    @DisplayName(
            "TYPE-06: doWriteColValue throws upstream's RuntimeException for a JSON column under a default (flag-off) configuration")
    void doWriteColValueThrowsUpstreamRuntimeExceptionForJsonColumnUnderDefaultConfig() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(0));
        setSinkConfig(writer, new ClickHouseSinkConfig(new HashMap<>()));
        Column column = Column.builder().name("json_col").type(Type.JSON).build();
        Data data = new Data(Schema.STRING_SCHEMA, "irrelevant_value");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        RuntimeException ex =
                assertThrows(
                        RuntimeException.class, () -> writer.doWriteColValue(column, out, data, true));

        assertTrue(ex.getMessage().contains("Writing JSON in binary is not supported yet"), ex.getMessage());
        assertTrue(ex.getMessage().contains("input_format_binary_read_json_as_string"), ex.getMessage());
    }

    @Test
    @DisplayName(
            "TYPE-06: validateDataSchema returns false (not a throw) for a JSON column under a default (flag-off) configuration, and never leaks the record's field value")
    void validateDataSchemaReturnsFalseForJsonColumnUnderDefaultConfigAndNeverLeaksTheValue() throws Exception {
        Column column = Column.builder().name("json_col").type(Type.JSON).isNullable(false).hasDefault(false).build();
        Table table = new Table("test_db", "test_table", false, List.of(column), 1);

        String secretFieldValue = "distinctive_secret_value_should_never_leak_into_logs_or_dlq";
        Map<String, Data> jsonMap = new HashMap<>();
        jsonMap.put("json_col", new Data(Schema.STRING_SCHEMA, secretFieldValue));

        // WR-03: validateDataSchema's default: branch looks up each column's source Schema via
        // record.getFields() (keyed by field name) before comparing type names. An empty fields
        // list here - like the sibling DYNAMIC/UNKNOWN test below - used to NPE on
        // Schema#name() because that lookup returns null with no matching Field; the objSchema
        // null-check in ClickHouseWriter now handles this without a throw, so no matching Field
        // needs to be constructed to avoid it.
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

        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(0));
        setSinkConfig(writer, new ClickHouseSinkConfig(new HashMap<>()));

        boolean validSchema = writer.validateDataSchema(table, record, false);

        assertFalse(validSchema);
    }

    @ParameterizedTest(name = "TYPE-06: doWriteColValue throws the actionable message for {0}")
    @EnumSource(
            value = Type.class,
            names = {"DYNAMIC", "UNKNOWN"})
    @DisplayName(
            "TYPE-06: doWriteColValue throws the actionable message for DYNAMIC and UNKNOWN columns (D-03's whole point)")
    void doWriteColValueThrowsForEveryUnsupportedType(Type type) throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(0));
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
            names = {"DYNAMIC", "UNKNOWN"})
    @DisplayName(
            "TYPE-06: validateDataSchema throws the same actionable message for DYNAMIC and UNKNOWN root columns, and never leaks the record's field value")
    void validateDataSchemaThrowsForEveryUnsupportedTypeAndNeverLeaksTheValue(Type type) {
        Column column =
                Column.builder().name("unsupported_col").type(type).isNullable(false).hasDefault(false).build();
        Table table = new Table("test_db", "test_table", false, List.of(column), 1);

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

        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(0));
        DataException ex =
                assertThrows(DataException.class, () -> writer.validateDataSchema(table, record, false));

        assertTrue(ex.getMessage().contains("insertFormat=json"), ex.getMessage());
        assertTrue(ex.getMessage().contains("bypassRowBinary=true"), ex.getMessage());
        assertTrue(ex.getMessage().contains("unsupported_col"), ex.getMessage());
        assertFalse(ex.getMessage().contains(secretFieldValue), ex.getMessage());
    }
}
