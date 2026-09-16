package transforms;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.clickhouse.kafka.connect.transforms.TableOperationFilter;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TableOperationFilterTest {

    private TableOperationFilter<SinkRecord> filter;
    private Map<String, String> configs;

    @BeforeEach
    public void setUp() {
        filter = new TableOperationFilter<>();
        configs = new HashMap<>();
        configs.put("tables", "mydb.mytable");
        configs.put("skipped.operations", "d"); // Skip delete operations
        filter.configure(configs);
    }

    @Test
    public void testNullValueRecord() {
        SinkRecord record = new SinkRecord("topic", 0, null, null, null, null, 0L);
        assertEquals(record, filter.apply(record), "Record with null value should pass through unchanged");
    }

    @Test
    public void testNonStructValueRecord() {
        SinkRecord record = new SinkRecord("topic", 0, null, null, null, "non-struct value", 0L);
        assertEquals(record, filter.apply(record), "Record with non-Struct value should pass through unchanged");
    }

    @Test
    public void testRecordWithoutOpField() {
        Schema schema = SchemaBuilder.struct().build();
        Struct value = new Struct(schema);
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, value, 0L);
        assertEquals(record, filter.apply(record), "Record without op field should pass through unchanged");
    }

    @Test
    public void testRecordWithoutSourceField() {
        Schema schema = SchemaBuilder.struct()
                .field("op", Schema.STRING_SCHEMA)
                .build();
        Struct value = new Struct(schema).put("op", "d");
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, value, 0L);
        assertEquals(record, filter.apply(record), "Record without source field should pass through unchanged");
    }

    @Test
    public void testRecordWithoutDbOrTableInSource() {
        Schema sourceSchema = SchemaBuilder.struct()
                .field("other_field", Schema.STRING_SCHEMA)
                .build();
        Struct source = new Struct(sourceSchema)
                .put("other_field", "value");

        Schema recordSchema = SchemaBuilder.struct()
                .field("op", Schema.STRING_SCHEMA)
                .field("source", sourceSchema)
                .build();
        Struct value = new Struct(recordSchema)
                .put("op", "d")
                .put("source", source);

        SinkRecord record = new SinkRecord("topic", 0, null, null, recordSchema, value, 0L);
        assertEquals(record, filter.apply(record), "Record without db/table in source should pass through unchanged");
    }

    @Test
    public void testRecordWithOperationTypeNotInSkippedOperations() {
        Schema sourceSchema = SchemaBuilder.struct()
                .field("db", Schema.STRING_SCHEMA)
                .field("table", Schema.STRING_SCHEMA)
                .build();
        Struct source = new Struct(sourceSchema)
                .put("db", "mydb")
                .put("table", "mytable");

        Schema recordSchema = SchemaBuilder.struct()
                .field("op", Schema.STRING_SCHEMA)
                .field("source", sourceSchema)
                .build();
        Struct value = new Struct(recordSchema)
                .put("op", "c")  // Create operation, which is not in skipped.operations
                .put("source", source);

        SinkRecord record = new SinkRecord("topic", 0, null, null, recordSchema, value, 0L);
        assertEquals(record, filter.apply(record), "Record with non-skipped operation should pass through unchanged");
    }

    @Test
    public void testRecordWithTableNotInTables() {
        Schema sourceSchema = SchemaBuilder.struct()
                .field("db", Schema.STRING_SCHEMA)
                .field("table", Schema.STRING_SCHEMA)
                .build();
        Struct source = new Struct(sourceSchema)
                .put("db", "mydb")
                .put("table", "other_table");  // Not in configured tables list

        Schema recordSchema = SchemaBuilder.struct()
                .field("op", Schema.STRING_SCHEMA)
                .field("source", sourceSchema)
                .build();
        Struct value = new Struct(recordSchema)
                .put("op", "d")
                .put("source", source);

        SinkRecord record = new SinkRecord("topic", 0, null, null, recordSchema, value, 0L);
        assertEquals(record, filter.apply(record), "Record with non-matching table should pass through unchanged");
    }

    @Test
    public void testRecordWithOperationInSkippedOperationsAndTableInTables() {
        Schema sourceSchema = SchemaBuilder.struct()
                .field("db", Schema.STRING_SCHEMA)
                .field("table", Schema.STRING_SCHEMA)
                .build();
        Struct source = new Struct(sourceSchema)
                .put("db", "mydb")
                .put("table", "mytable");

        Schema recordSchema = SchemaBuilder.struct()
                .field("op", Schema.STRING_SCHEMA)
                .field("source", sourceSchema)
                .build();
        Struct value = new Struct(recordSchema)
                .put("op", "d")  // Delete operation, which should be skipped
                .put("source", source);

        SinkRecord record = new SinkRecord("topic", 0, null, null, recordSchema, value, 0L);
        assertNull(filter.apply(record), "Record with skipped operation and matching table should be filtered out");
    }

    @Test
    public void testConfigurationPropertiesParsing() {
        Map<String, String> testConfigs = new HashMap<>();
        testConfigs.put("tables", "db1.table1,db2.table2");  // Test multiple tables
        testConfigs.put("skipped.operations", "c,u,d");  // Test multiple operations

        TableOperationFilter<SinkRecord> testFilter = new TableOperationFilter<>();
        testFilter.configure(testConfigs);

        // Create a record that should be filtered based on the new configuration
        Schema sourceSchema = SchemaBuilder.struct()
                .field("db", Schema.STRING_SCHEMA)
                .field("table", Schema.STRING_SCHEMA)
                .build();
        Struct source = new Struct(sourceSchema)
                .put("db", "db2")
                .put("table", "table2");

        Schema recordSchema = SchemaBuilder.struct()
                .field("op", Schema.STRING_SCHEMA)
                .field("source", sourceSchema)
                .build();
        Struct value = new Struct(recordSchema)
                .put("op", "u")  // Update operation should be filtered
                .put("source", source);

        SinkRecord record = new SinkRecord("topic", 0, null, null, recordSchema, value, 0L);
        assertNull(testFilter.apply(record), "Record should be filtered with new configuration");
    }

    @Test
    @DisplayName("TOF-05: config() returns the shared CONFIG_DEF instance")
    public void testConfigReturnsSharedConfigDefInstance() {
        assertSame(TableOperationFilter.CONFIG_DEF, filter.config());
    }

    @Test
    @DisplayName("TOF-05: close() can be called repeatedly without throwing")
    public void testCloseCanBeCalledRepeatedly() {
        assertDoesNotThrow(
                () -> {
                    filter.close();
                    filter.close();
                    filter.close();
                });
    }

    @Test
    @DisplayName("TOF-05: reconfiguring an instance replaces the prior configuration (D-03)")
    public void testReconfigureReplacesPriorConfiguration() {
        TableOperationFilter<SinkRecord> reconfigurable = new TableOperationFilter<>();
        Map<String, String> firstConfig = new HashMap<>();
        firstConfig.put("tables", "mydb.mytable");
        firstConfig.put("skipped.operations", "d");
        reconfigurable.configure(firstConfig);

        SinkRecord record = TransformTestHelpers.debeziumRecord("topic", 0L, "d", "mydb", "mytable");
        assertNull(reconfigurable.apply(record), "Record should be dropped under the first configuration");

        Map<String, String> secondConfig = new HashMap<>();
        secondConfig.put("tables", "otherdb.othertable");
        secondConfig.put("skipped.operations", "c");
        reconfigurable.configure(secondConfig);

        assertSame(
                record,
                reconfigurable.apply(record),
                "Record should pass through after reconfiguration replaces the prior filter set");
    }

    @Test
    @DisplayName("TOF-01: truncate operations are dropped while other operations pass through")
    public void testTruncateDroppedOtherOperationsPassThrough() {
        TableOperationFilter<SinkRecord> f = new TableOperationFilter<>();
        Map<String, String> cfg = new HashMap<>();
        cfg.put("tables", "mydb.mytable");
        cfg.put("skipped.operations", "t");
        f.configure(cfg);

        SinkRecord truncate = TransformTestHelpers.debeziumRecord("topic", 0L, "t", "mydb", "mytable");
        assertNull(f.apply(truncate), "Truncate record for a listed table should be dropped");

        SinkRecord create = TransformTestHelpers.debeziumRecord("topic", 1L, "c", "mydb", "mytable");
        assertSame(create, f.apply(create), "Create record should pass through unchanged");

        SinkRecord update = TransformTestHelpers.debeziumRecord("topic", 2L, "u", "mydb", "mytable");
        assertSame(update, f.apply(update), "Update record should pass through unchanged");

        SinkRecord delete = TransformTestHelpers.debeziumRecord("topic", 3L, "d", "mydb", "mytable");
        assertSame(delete, f.apply(delete), "Delete record should pass through unchanged");

        // "r" can never enter skippedOperations (configure() accepts only single characters from
        // "cudt"), so a record with op=r must always pass through regardless of table.
        SinkRecord replicate = TransformTestHelpers.debeziumRecord("topic", 4L, "r", "mydb", "mytable");
        assertSame(replicate, f.apply(replicate), "Record with op=r should always pass through unchanged");
    }

    @Test
    @DisplayName("TOF-01: an empty tables list means nothing is filtered")
    public void testEmptyTablesListMeansNoFiltering() {
        TableOperationFilter<SinkRecord> f = new TableOperationFilter<>();
        Map<String, String> cfg = new HashMap<>();
        cfg.put("tables", "");
        cfg.put("skipped.operations", "t");
        f.configure(cfg);

        SinkRecord record = TransformTestHelpers.debeziumRecord("topic", 0L, "t", "mydb", "mytable");
        assertSame(record, f.apply(record), "With no configured tables, records pass through unfiltered");
    }

    @Test
    @DisplayName("TOF-01: table matching handles non-ASCII names by exact string equality")
    public void testNonAsciiTableNameMatchesByExactEquality() {
        // Built from raw Unicode code points, never a literal non-ASCII byte, since
        // compileTestJava does not pin a source encoding. Code points: db is Cyrillic
        // U+0431 U+0430 U+0437 U+0430; table is Cyrillic U+0442 U+0430 U+0431 U+043B U+0438 U+0446 U+0430.
        String db = new String(new int[] {0x0431, 0x0430, 0x0437, 0x0430}, 0, 4);
        String table = new String(new int[] {0x0442, 0x0430, 0x0431, 0x043B, 0x0438, 0x0446, 0x0430}, 0, 7);

        TableOperationFilter<SinkRecord> f = new TableOperationFilter<>();
        Map<String, String> cfg = new HashMap<>();
        cfg.put("tables", db + "." + table);
        cfg.put("skipped.operations", "t");
        f.configure(cfg);

        SinkRecord record = TransformTestHelpers.debeziumRecord("topic", 0L, "t", db, table);
        assertNull(f.apply(record), "Non-ASCII table name matching the config exactly should be dropped");
    }

    @Test
    @DisplayName("TOF-01: table matching is case-sensitive")
    public void testTableMatchingIsCaseSensitive() {
        TableOperationFilter<SinkRecord> f = new TableOperationFilter<>();
        Map<String, String> cfg = new HashMap<>();
        cfg.put("tables", "mydb.mytable");
        cfg.put("skipped.operations", "t");
        f.configure(cfg);

        SinkRecord record = TransformTestHelpers.debeziumRecord("topic", 0L, "t", "MyDb", "MyTable");
        assertSame(record, f.apply(record), "Case-different table name should not match a lowercase config entry");
    }

    @Test
    @DisplayName("TOF-02: multiple configured tables filter independently")
    public void testMultipleTablesFilterIndependently() {
        TableOperationFilter<SinkRecord> f = new TableOperationFilter<>();
        Map<String, String> cfg = new HashMap<>();
        cfg.put("tables", "db1.t1,db2.t2");
        cfg.put("skipped.operations", "d");
        f.configure(cfg);

        SinkRecord r1 = TransformTestHelpers.debeziumRecord("topic", 0L, "d", "db1", "t1");
        assertNull(f.apply(r1), "db1.t1 delete should be dropped");

        SinkRecord r2 = TransformTestHelpers.debeziumRecord("topic", 1L, "d", "db2", "t2");
        assertNull(f.apply(r2), "db2.t2 delete should be dropped");

        SinkRecord r3 = TransformTestHelpers.debeziumRecord("topic", 2L, "d", "db3", "t3");
        assertSame(r3, f.apply(r3), "db3.t3 delete should pass through unchanged");
    }

    @Test
    @DisplayName("TOF-02: surrounding whitespace in the tables config is trimmed")
    public void testTablesConfigWhitespaceIsTrimmed() {
        TableOperationFilter<SinkRecord> f = new TableOperationFilter<>();
        Map<String, String> cfg = new HashMap<>();
        cfg.put("tables", " db1.t1 , db2.t2 ");
        cfg.put("skipped.operations", "d");
        f.configure(cfg);

        SinkRecord r1 = TransformTestHelpers.debeziumRecord("topic", 0L, "d", "db1", "t1");
        assertNull(f.apply(r1), "Whitespace-padded table entries should still match after trim");
    }

    @ParameterizedTest(name = "TOF-03: token ''{0}'' is ignored")
    @ValueSource(strings = {"x", "r", "", "   ", "cu"})
    @DisplayName("TOF-03: invalid, empty and whitespace tokens are ignored")
    public void testInvalidSkippedOperationsTokenIsIgnored(String token) {
        TableOperationFilter<SinkRecord> f = new TableOperationFilter<>();
        Map<String, String> cfg = new HashMap<>();
        cfg.put("tables", "mydb.mytable");
        cfg.put("skipped.operations", token);
        assertDoesNotThrow(() -> f.configure(cfg));

        // The empty and whitespace-only tokens are not meaningful Debezium operations, so use "d"
        // for those cases; otherwise use the token itself, trimmed and lowercased.
        String trimmed = token.trim();
        String op = trimmed.isEmpty() ? "d" : trimmed.toLowerCase();
        SinkRecord record = TransformTestHelpers.debeziumRecord("topic", 0L, op, "mydb", "mytable");
        assertSame(
                record,
                f.apply(record),
                "Record with op '" + op + "' should pass through unchanged since '" + token
                        + "' is not a valid skipped.operations token");
    }

    @Test
    @DisplayName("TOF-03: a mixed skipped.operations list keeps only its valid token")
    public void testMixedSkippedOperationsListKeepsOnlyValidToken() {
        TableOperationFilter<SinkRecord> f = new TableOperationFilter<>();
        Map<String, String> cfg = new HashMap<>();
        cfg.put("tables", "mydb.mytable");
        cfg.put("skipped.operations", "d,x, ,r");
        f.configure(cfg);

        SinkRecord deleteRecord = TransformTestHelpers.debeziumRecord("topic", 0L, "d", "mydb", "mytable");
        assertNull(f.apply(deleteRecord), "The valid 'd' token should still filter delete records");

        SinkRecord replicateRecord = TransformTestHelpers.debeziumRecord("topic", 1L, "r", "mydb", "mytable");
        assertSame(
                replicateRecord,
                f.apply(replicateRecord),
                "'r' is never a valid skipped.operations token, so it must pass through");
    }

    @Test
    @DisplayName("TOF-04: schemaless Debezium envelopes pass through unfiltered")
    public void testSchemalessDebeziumEnvelopePassesThroughUnfiltered() {
        TableOperationFilter<SinkRecord> f = new TableOperationFilter<>();
        Map<String, String> cfg = new HashMap<>();
        cfg.put("tables", "mydb.mytable");
        cfg.put("skipped.operations", "d");
        f.configure(cfg);

        SinkRecord record = TransformTestHelpers.schemalessDebeziumRecord("topic", 0L, "d", "mydb", "mytable");
        assertSame(
                record,
                f.apply(record),
                "Schemaless (Map-valued) records are not Structs, so apply() must pass them through unfiltered");
    }

    @Test
    @DisplayName("TOF-04: schemaless Debezium envelopes with a nested source map pass through unfiltered")
    public void testSchemalessDebeziumEnvelopeWithNestedSourcePassesThroughUnfiltered() {
        TableOperationFilter<SinkRecord> f = new TableOperationFilter<>();
        Map<String, String> cfg = new HashMap<>();
        cfg.put("tables", "mydb.mytable");
        cfg.put("skipped.operations", "d");
        f.configure(cfg);

        SinkRecord record = TransformTestHelpers.schemalessDebeziumRecord("topic", 1L, "d", "mydb", "mytable");
        assertSame(
                record,
                f.apply(record),
                "The nested-Map source shape must also pass through unfiltered");
    }
}
