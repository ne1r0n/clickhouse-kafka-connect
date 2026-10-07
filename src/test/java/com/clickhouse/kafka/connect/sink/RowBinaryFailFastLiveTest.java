package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * ClickHouse-backed proof for TYPE-06: a {@code JSON}-column table driven by a Struct-schema
 * record through {@link ClickHouseSinkTask#put} under the default (non-bypass) RowBinary path
 * fails with the D-05 actionable message and leaves zero rows behind - the "no silent data
 * loss" half only a live test can prove - plus the honest proof that the {@code
 * bypassRowBinary=true} escape hatch the message advertises actually works. New fork-only class
 * (D-16): it does not extend or modify any upstream test class.
 */
public class RowBinaryFailFastLiveTest extends ClickHouseBase {

    private static void createTable(ClickHouseHelperClient chc, String tableName, String createTableQuery) {
        try (Records ignored = chc.queryV2(String.format(createTableQuery, tableName))) {
            // table created
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName(
            "TYPE-06: put() on a JSON column under default config fails with the actionable message and leaves zero rows")
    void putFailsWithActionableMessageAndLeavesZeroRows(TestInfo testInfo) {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName("rowbinary_failfast_negative");

        // Create the table BEFORE start() - ClickHouseWriter.start() fails against an empty
        // database (Phase 1 rule).
        createTable(
                chc, tableName, "CREATE TABLE %s (id UInt32, json_col JSON) ENGINE = MergeTree ORDER BY id");
        // Deliberately NOT setting bypassRowBinary or bypassSchemaValidation - the real
        // production default path, the one an operator actually hits.
        props.put("name", uniqueConnectorName(testInfo));

        try {
            // A genuine JSON-object value (ClickHouse's JSON type rejects a root scalar via
            // JSONEachRow with "JSON object should start with '{'", matching 03-02's finding).
            Schema nestedLeafSchema = SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.INT32_SCHEMA).build();
            Schema jsonFieldSchema = SchemaBuilder.map(Schema.STRING_SCHEMA, nestedLeafSchema).build();
            Schema schema =
                    SchemaBuilder.struct()
                            .field("id", Schema.INT32_SCHEMA)
                            .field("json_col", jsonFieldSchema)
                            .build();
            Struct value = new Struct(schema).put("id", 1).put("json_col", Map.of("a", Map.of("b", 1)));
            SinkRecord record = new SinkRecord(tableName, 0, null, null, schema, value, 1);

            ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
            sinkTask.start(props);
            try {
                RuntimeException ex =
                        assertThrows(RuntimeException.class, () -> sinkTask.put(List.of(record)));

                // Walk the getCause() chain to the root, accumulating every message on the way,
                // and assert on content - a bare assertThrows(RuntimeException.class, ...) would
                // have passed identically before this phase's fix (RESEARCH Pitfall 4).
                StringBuilder causeChain = new StringBuilder();
                Throwable current = ex;
                while (current != null) {
                    if (current.getMessage() != null) {
                        causeChain.append(current.getMessage()).append(" | ");
                    }
                    current = current.getCause();
                }
                assertTrue(causeChain.toString().contains("insertFormat=json"), causeChain.toString());
            } finally {
                sinkTask.stop();
            }

            // The "no silent data loss" half - only provable live.
            assertEquals(0, ClickHouseTestHelpers.countRows(chc, tableName));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @Test
    @DisplayName(
            "TYPE-06: the same insert succeeds once bypassRowBinary=true is set (D-05 escape hatch proof)")
    void bypassRowBinaryEscapeHatchMakesTheSameInsertSucceed(TestInfo testInfo) {
        Map<String, String> props = getBaseProps();
        props.put("bypassRowBinary", "true"); // D-05: the escape hatch the new message advertises.
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String tableName = createTopicName("rowbinary_failfast_positive");

        createTable(
                chc, tableName, "CREATE TABLE %s (id UInt32, json_col JSON) ENGINE = MergeTree ORDER BY id");
        props.put("name", uniqueConnectorName(testInfo));

        try {
            Schema nestedLeafSchema = SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.INT32_SCHEMA).build();
            Schema jsonFieldSchema = SchemaBuilder.map(Schema.STRING_SCHEMA, nestedLeafSchema).build();
            Schema schema =
                    SchemaBuilder.struct()
                            .field("id", Schema.INT32_SCHEMA)
                            .field("json_col", jsonFieldSchema)
                            .build();
            Struct value = new Struct(schema).put("id", 1).put("json_col", Map.of("a", Map.of("b", 1)));
            SinkRecord record = new SinkRecord(tableName, 0, null, null, schema, value, 1);

            ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
            sinkTask.start(props);
            sinkTask.put(List.of(record));
            sinkTask.stop();

            assertEquals(1, ClickHouseTestHelpers.countRows(chc, tableName));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }
}
