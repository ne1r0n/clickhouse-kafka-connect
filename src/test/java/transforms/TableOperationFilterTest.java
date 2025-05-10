package transforms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.clickhouse.kafka.connect.transforms.TableOperationFilter;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    public void testMetrics() {
        // Process a record that should pass through
        Schema recordSchema = SchemaBuilder.struct()
                .field("op", Schema.STRING_SCHEMA)
                .build();
        Struct value = new Struct(recordSchema)
                .put("op", "c"); // Create operation, not in skipped.operations
        SinkRecord record = new SinkRecord("topic", 0, null, null, recordSchema, value, 0L);
        filter.apply(record);

        // Process a record that should be filtered
        Schema sourceSchema = SchemaBuilder.struct()
                .field("db", Schema.STRING_SCHEMA)
                .field("table", Schema.STRING_SCHEMA)
                .build();
        Struct source = new Struct(sourceSchema)
                .put("db", "mydb")
                .put("table", "mytable");

        Schema filteredRecordSchema = SchemaBuilder.struct()
                .field("op", Schema.STRING_SCHEMA)
                .field("source", sourceSchema)
                .build();
        Struct filteredValue = new Struct(filteredRecordSchema)
                .put("op", "d")  // Delete operation should be filtered
                .put("source", source);

        SinkRecord filteredRecord = new SinkRecord("topic", 0, null, null, filteredRecordSchema, filteredValue, 0L);
        filter.apply(filteredRecord);

        Map<String, Object> metrics = filter.metrics();
        assertEquals(2L, metrics.get("processed_records"), "Should have processed 2 records");
        assertEquals(1L, metrics.get("filtered_records"), "Should have filtered 1 record");
    }
}
