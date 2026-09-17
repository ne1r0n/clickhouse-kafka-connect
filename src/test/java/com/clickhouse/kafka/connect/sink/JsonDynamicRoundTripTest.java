package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * ClickHouse-backed round-trips for TYPE-03/04/05: {@code JSON}, parameterized {@code JSON(...)}
 * and {@code Dynamic} columns through both D-13 insert routes - schemaless {@code JSONEachRow}
 * and {@code bypassRowBinary=true} Struct records - with typed SQL read-back (D-14) rather than a
 * row count. New fork-only class (D-16): it does not extend or modify any upstream test class.
 */
public class JsonDynamicRoundTripTest extends ClickHouseBase {

    @Test
    @DisplayName("TYPE-03: JSON and Dynamic columns round-trip through the schemaless JSONEachRow route with typed read-back")
    void jsonAndDynamicRoundTripThroughSchemalessRoute(TestInfo testInfo) throws Exception {
        String tableName = createTopicName("json_dynamic_schemaless");
        Map<String, String> props = createProps();
        ClickHouseHelperClient chc = createClient(props);
        ClickHouseTestHelpers.createTable(chc, tableName,
                "CREATE TABLE %s (id UInt32, json_col JSON, dynamic_col Dynamic) ENGINE = MergeTree ORDER BY id");
        props.put("name", uniqueConnectorName(testInfo));

        try {
            Map<String, Object> jsonLeaf1 = new LinkedHashMap<>();
            jsonLeaf1.put("b", 1);
            Map<String, Object> json1 = new LinkedHashMap<>();
            json1.put("a", jsonLeaf1);
            Map<String, Object> value1 = new LinkedHashMap<>();
            value1.put("id", 1);
            value1.put("json_col", json1);
            value1.put("dynamic_col", 42);

            Map<String, Object> jsonLeaf2 = new LinkedHashMap<>();
            jsonLeaf2.put("b", 2);
            Map<String, Object> json2 = new LinkedHashMap<>();
            json2.put("a", jsonLeaf2);
            Map<String, Object> value2 = new LinkedHashMap<>();
            value2.put("id", 2);
            value2.put("json_col", json2);
            value2.put("dynamic_col", "text_value");

            List<SinkRecord> records = List.of(
                    new SinkRecord(tableName, 0, null, null, null, value1, 1),
                    new SinkRecord(tableName, 0, null, null, null, value2, 2));

            ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
            sinkTask.start(props);
            sinkTask.put(records);
            sinkTask.stop();

            assertEquals(2, ClickHouseTestHelpers.countRows(chc, tableName));

            List<Map<String, String>> leafRows = allRows(chc, String.format(
                    "SELECT id, json_col.a.b AS leaf, toTypeName(json_col.a.b) AS leaf_type FROM `%s` ORDER BY id",
                    tableName), "id", "leaf", "leaf_type");
            assertEquals(2, leafRows.size());
            assertEquals("1", leafRows.get(0).get("leaf"));
            // A plain JSON leaf reports ClickHouse's own internal Dynamic representation - expected,
            // not a bug (Assumption A3, 03-RESEARCH.md).
            assertEquals("Dynamic", leafRows.get(0).get("leaf_type"));
            assertEquals("2", leafRows.get(1).get("leaf"));
            assertEquals("Dynamic", leafRows.get(1).get("leaf_type"));

            List<Map<String, String>> dynRows = allRows(chc, String.format(
                    "SELECT id, dynamicType(dynamic_col) AS dyn_type, dynamic_col AS dyn_val FROM `%s` ORDER BY id",
                    tableName), "id", "dyn_type", "dyn_val");
            assertEquals(2, dynRows.size());
            assertEquals("Int64", dynRows.get(0).get("dyn_type"));
            assertEquals("42", dynRows.get(0).get("dyn_val"));
            assertEquals("String", dynRows.get(1).get("dyn_type"));
            assertEquals("text_value", dynRows.get(1).get("dyn_val"));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @Test
    @DisplayName("TYPE-03: JSON and Dynamic columns round-trip through the bypassRowBinary Struct record route (D-05 escape hatch proof)")
    void jsonAndDynamicRoundTripThroughStructRoute(TestInfo testInfo) throws Exception {
        String tableName = createTopicName("json_dynamic_struct");
        Map<String, String> props = createProps();
        props.put("bypassRowBinary", "true");
        ClickHouseHelperClient chc = createClient(props);
        ClickHouseTestHelpers.createTable(chc, tableName,
                "CREATE TABLE %s (id UInt32, json_col JSON, dynamic_col Dynamic) ENGINE = MergeTree ORDER BY id");
        props.put("name", uniqueConnectorName(testInfo));

        try {
            Schema nestedLeafSchema = SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.INT32_SCHEMA).build();
            Schema jsonFieldSchema = SchemaBuilder.map(Schema.STRING_SCHEMA, nestedLeafSchema).build();

            Schema schema1 = SchemaBuilder.struct()
                    .field("id", Schema.INT32_SCHEMA)
                    .field("json_col", jsonFieldSchema)
                    .field("dynamic_col", Schema.INT64_SCHEMA)
                    .build();
            Struct struct1 = new Struct(schema1)
                    .put("id", 1)
                    .put("json_col", Map.of("a", Map.of("b", 5)))
                    .put("dynamic_col", 99L);

            Schema schema2 = SchemaBuilder.struct()
                    .field("id", Schema.INT32_SCHEMA)
                    .field("json_col", jsonFieldSchema)
                    .field("dynamic_col", Schema.STRING_SCHEMA)
                    .build();
            Struct struct2 = new Struct(schema2)
                    .put("id", 2)
                    .put("json_col", Map.of("a", Map.of("b", 6)))
                    .put("dynamic_col", "struct_text");

            List<SinkRecord> records = List.of(
                    new SinkRecord(tableName, 0, null, null, schema1, struct1, 1),
                    new SinkRecord(tableName, 0, null, null, schema2, struct2, 2));

            ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
            sinkTask.start(props);
            sinkTask.put(records);
            sinkTask.stop();

            assertEquals(2, ClickHouseTestHelpers.countRows(chc, tableName));

            List<Map<String, String>> leafRows = allRows(chc, String.format(
                    "SELECT id, json_col.a.b AS leaf, toTypeName(json_col.a.b) AS leaf_type FROM `%s` ORDER BY id",
                    tableName), "id", "leaf", "leaf_type");
            assertEquals(2, leafRows.size());
            assertEquals("5", leafRows.get(0).get("leaf"));
            assertEquals("Dynamic", leafRows.get(0).get("leaf_type"));
            assertEquals("6", leafRows.get(1).get("leaf"));
            assertEquals("Dynamic", leafRows.get(1).get("leaf_type"));

            List<Map<String, String>> dynRows = allRows(chc, String.format(
                    "SELECT id, dynamicType(dynamic_col) AS dyn_type, dynamic_col AS dyn_val FROM `%s` ORDER BY id",
                    tableName), "id", "dyn_type", "dyn_val");
            assertEquals(2, dynRows.size());
            assertEquals("Int64", dynRows.get(0).get("dyn_type"));
            assertEquals("99", dynRows.get(0).get("dyn_val"));
            assertEquals("String", dynRows.get(1).get("dyn_type"));
            assertEquals("struct_text", dynRows.get(1).get("dyn_val"));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @Test
    @DisplayName("TYPE-04: dynamicType(dynamic_col) is stable across repeated reads and does not depend on read order")
    void dynamicColumnTypeIsStableAcrossRepeatedReads(TestInfo testInfo) throws Exception {
        String tableName = createTopicName("dynamic_idempotency");
        Map<String, String> props = createProps();
        ClickHouseHelperClient chc = createClient(props);
        ClickHouseTestHelpers.createTable(chc, tableName,
                "CREATE TABLE %s (id UInt32, dynamic_col Dynamic) ENGINE = MergeTree ORDER BY id");
        props.put("name", uniqueConnectorName(testInfo));

        try {
            Map<String, Object> value1 = new LinkedHashMap<>();
            value1.put("id", 1);
            value1.put("dynamic_col", 7);

            Map<String, Object> value2 = new LinkedHashMap<>();
            value2.put("id", 2);
            value2.put("dynamic_col", "idempotent_text");

            Map<String, Object> value3 = new LinkedHashMap<>();
            value3.put("id", 3);
            value3.put("dynamic_col", 3.5);

            List<SinkRecord> records = List.of(
                    new SinkRecord(tableName, 0, null, null, null, value1, 1),
                    new SinkRecord(tableName, 0, null, null, null, value2, 2),
                    new SinkRecord(tableName, 0, null, null, null, value3, 3));

            ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
            sinkTask.start(props);
            sinkTask.put(records);
            sinkTask.stop();

            String sql = String.format(
                    "SELECT id, dynamicType(dynamic_col) AS dyn_type FROM `%s` ORDER BY id", tableName);
            List<Map<String, String>> firstRead = allRows(chc, sql, "id", "dyn_type");
            List<Map<String, String>> secondRead = allRows(chc, sql, "id", "dyn_type");

            assertEquals(3, firstRead.size());
            assertEquals(3, secondRead.size());
            for (int i = 0; i < firstRead.size(); i++) {
                assertEquals(firstRead.get(i).get("dyn_type"), secondRead.get(i).get("dyn_type"));
            }
            assertEquals("Int64", firstRead.get(0).get("dyn_type"));
            assertEquals("String", firstRead.get(1).get("dyn_type"));
            assertEquals("Float64", firstRead.get(2).get("dyn_type"));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @Test
    @DisplayName("TYPE-05: a mixed table with Dynamic, String, UInt32, JSON, Float64 and Decimal columns round-trips through both insert routes regardless of column position")
    void mixedTableRoundTripsThroughBothInsertRoutes(TestInfo testInfo) throws Exception {
        String tableName = createTopicName("json_dynamic_mixed");
        Map<String, String> props = createProps();
        ClickHouseHelperClient chc = createClient(props);
        ClickHouseTestHelpers.createTable(chc, tableName,
                "CREATE TABLE %s ("
                        + "id UInt32, "
                        + "dyn_first Dynamic, "
                        + "str_col String, "
                        + "uint_col UInt32, "
                        + "json_col JSON(max_dynamic_paths=10), "
                        + "float_col Float64, "
                        + "dec_col Decimal(18, 4), "
                        + "dyn_last Dynamic"
                        + ") ENGINE = MergeTree ORDER BY id");

        try {
            // Route A: schemaless JSONEachRow, disjoint id range 1-2.
            Map<String, Object> jsonLeafA1 = new LinkedHashMap<>();
            jsonLeafA1.put("b", 123456789012345L); // large integer - distinguishes Int from Float
            Map<String, Object> jsonA1 = new LinkedHashMap<>();
            jsonA1.put("a", jsonLeafA1);
            Map<String, Object> valueA1 = new LinkedHashMap<>();
            valueA1.put("id", 1);
            valueA1.put("dyn_first", 10);
            valueA1.put("str_col", "route_a_row1");
            valueA1.put("uint_col", 100);
            valueA1.put("json_col", jsonA1);
            valueA1.put("float_col", 3.5);
            valueA1.put("dec_col", new BigDecimal("12345.6789"));
            valueA1.put("dyn_last", "route_a_last1");

            Map<String, Object> jsonLeafA2 = new LinkedHashMap<>();
            jsonLeafA2.put("b", 42);
            Map<String, Object> jsonA2 = new LinkedHashMap<>();
            jsonA2.put("a", jsonLeafA2);
            Map<String, Object> valueA2 = new LinkedHashMap<>();
            valueA2.put("id", 2);
            valueA2.put("dyn_first", "route_a_dyn2");
            valueA2.put("str_col", "route_a_row2");
            valueA2.put("uint_col", 200);
            valueA2.put("json_col", jsonA2);
            valueA2.put("float_col", 7.25);
            valueA2.put("dec_col", new BigDecimal("999.0001"));
            valueA2.put("dyn_last", 55);

            List<SinkRecord> schemalessRecords = List.of(
                    new SinkRecord(tableName, 0, null, null, null, valueA1, 1),
                    new SinkRecord(tableName, 0, null, null, null, valueA2, 2));

            Map<String, String> schemalessProps = new HashMap<>(props);
            schemalessProps.put("name", uniqueConnectorName(testInfo));
            ClickHouseSinkTask schemalessTask = new ClickHouseSinkTask();
            schemalessTask.start(schemalessProps);
            schemalessTask.put(schemalessRecords);
            schemalessTask.stop();

            // Route B: bypassRowBinary Struct records, disjoint id range 101-102.
            Map<String, String> structProps = new HashMap<>(props);
            structProps.put("bypassRowBinary", "true");
            structProps.put("name", uniqueConnectorName(testInfo));

            Schema nestedLeafSchema = SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.INT64_SCHEMA).build();
            Schema jsonFieldSchema = SchemaBuilder.map(Schema.STRING_SCHEMA, nestedLeafSchema).build();

            Schema structSchema1 = SchemaBuilder.struct()
                    .field("id", Schema.INT32_SCHEMA)
                    .field("dyn_first", Schema.INT64_SCHEMA)
                    .field("str_col", Schema.STRING_SCHEMA)
                    .field("uint_col", Schema.INT32_SCHEMA)
                    .field("json_col", jsonFieldSchema)
                    .field("float_col", Schema.FLOAT64_SCHEMA)
                    .field("dec_col", Decimal.schema(4))
                    .field("dyn_last", Schema.STRING_SCHEMA)
                    .build();
            Struct structB1 = new Struct(structSchema1)
                    .put("id", 101)
                    .put("dyn_first", 11L)
                    .put("str_col", "route_b_row1")
                    .put("uint_col", 300)
                    .put("json_col", Map.of("a", Map.of("b", 987654321098765L)))
                    .put("float_col", 1.25)
                    .put("dec_col", new BigDecimal("4321.9999"))
                    .put("dyn_last", "route_b_last1");

            Schema structSchema2 = SchemaBuilder.struct()
                    .field("id", Schema.INT32_SCHEMA)
                    .field("dyn_first", Schema.STRING_SCHEMA)
                    .field("str_col", Schema.STRING_SCHEMA)
                    .field("uint_col", Schema.INT32_SCHEMA)
                    .field("json_col", jsonFieldSchema)
                    .field("float_col", Schema.FLOAT64_SCHEMA)
                    .field("dec_col", Decimal.schema(4))
                    .field("dyn_last", Schema.INT64_SCHEMA)
                    .build();
            Struct structB2 = new Struct(structSchema2)
                    .put("id", 102)
                    .put("dyn_first", "route_b_dyn2")
                    .put("str_col", "route_b_row2")
                    .put("uint_col", 400)
                    .put("json_col", Map.of("a", Map.of("b", 77L)))
                    .put("float_col", 9.75)
                    .put("dec_col", new BigDecimal("1.2345"))
                    .put("dyn_last", 88L);

            List<SinkRecord> structRecords = List.of(
                    new SinkRecord(tableName, 0, null, null, structSchema1, structB1, 101),
                    new SinkRecord(tableName, 0, null, null, structSchema2, structB2, 102));

            ClickHouseSinkTask structTask = new ClickHouseSinkTask();
            structTask.start(structProps);
            structTask.put(structRecords);
            structTask.stop();

            assertEquals(4, ClickHouseTestHelpers.countRows(chc, tableName));

            String selectTemplate = "SELECT id, str_col, uint_col, float_col, dec_col, "
                    + "json_col.a.b AS leaf, toTypeName(json_col.a.b) AS leaf_type, "
                    + "dynamicType(dyn_first) AS dyn_first_type, dyn_first AS dyn_first_val, "
                    + "dynamicType(dyn_last) AS dyn_last_type, dyn_last AS dyn_last_val "
                    + "FROM `%s` WHERE id IN (%s) ORDER BY id";
            String[] columns = {
                "id", "str_col", "uint_col", "float_col", "dec_col", "leaf", "leaf_type",
                "dyn_first_type", "dyn_first_val", "dyn_last_type", "dyn_last_val"
            };

            List<Map<String, String>> routeARows = allRows(
                    chc, String.format(selectTemplate, tableName, "1, 2"), columns);
            assertEquals(2, routeARows.size());

            Map<String, String> a1 = routeARows.get(0);
            assertEquals("route_a_row1", a1.get("str_col"));
            assertEquals(100, Integer.parseInt(a1.get("uint_col")));
            assertEquals(3.5, Double.parseDouble(a1.get("float_col")), 0.0);
            assertEquals(0, new BigDecimal("12345.6789").compareTo(new BigDecimal(a1.get("dec_col"))));
            assertEquals("123456789012345", a1.get("leaf"));
            assertNotEquals("Float64", a1.get("leaf_type"));
            assertEquals("Int64", a1.get("dyn_first_type"));
            assertEquals("10", a1.get("dyn_first_val"));
            assertEquals("String", a1.get("dyn_last_type"));
            assertEquals("route_a_last1", a1.get("dyn_last_val"));

            Map<String, String> a2 = routeARows.get(1);
            assertEquals("route_a_row2", a2.get("str_col"));
            assertEquals(200, Integer.parseInt(a2.get("uint_col")));
            assertEquals(7.25, Double.parseDouble(a2.get("float_col")), 0.0);
            assertEquals(0, new BigDecimal("999.0001").compareTo(new BigDecimal(a2.get("dec_col"))));
            assertEquals("42", a2.get("leaf"));
            assertEquals("String", a2.get("dyn_first_type"));
            assertEquals("route_a_dyn2", a2.get("dyn_first_val"));
            assertEquals("Int64", a2.get("dyn_last_type"));
            assertEquals("55", a2.get("dyn_last_val"));

            List<Map<String, String>> routeBRows = allRows(
                    chc, String.format(selectTemplate, tableName, "101, 102"), columns);
            assertEquals(2, routeBRows.size());

            Map<String, String> b1 = routeBRows.get(0);
            assertEquals("route_b_row1", b1.get("str_col"));
            assertEquals(300, Integer.parseInt(b1.get("uint_col")));
            assertEquals(1.25, Double.parseDouble(b1.get("float_col")), 0.0);
            assertEquals(0, new BigDecimal("4321.9999").compareTo(new BigDecimal(b1.get("dec_col"))));
            assertEquals("987654321098765", b1.get("leaf"));
            assertNotEquals("Float64", b1.get("leaf_type"));
            assertEquals("Int64", b1.get("dyn_first_type"));
            assertEquals("11", b1.get("dyn_first_val"));
            assertEquals("String", b1.get("dyn_last_type"));
            assertEquals("route_b_last1", b1.get("dyn_last_val"));

            Map<String, String> b2 = routeBRows.get(1);
            assertEquals("route_b_row2", b2.get("str_col"));
            assertEquals(400, Integer.parseInt(b2.get("uint_col")));
            assertEquals(9.75, Double.parseDouble(b2.get("float_col")), 0.0);
            assertEquals(0, new BigDecimal("1.2345").compareTo(new BigDecimal(b2.get("dec_col"))));
            assertEquals("77", b2.get("leaf"));
            assertEquals("String", b2.get("dyn_first_type"));
            assertEquals("route_b_dyn2", b2.get("dyn_first_val"));
            assertEquals("Int64", b2.get("dyn_last_type"));
            assertEquals("88", b2.get("dyn_last_val"));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    /**
     * Snapshots each row's requested columns into a plain {@code Map<String, String>} while the
     * row is still the iterator's current element. The client-v2 {@link GenericRecord} returned
     * by {@link Records}'s iterator is a mutable, reused view over the underlying binary reader
     * buffer - collecting the {@code GenericRecord} references themselves (rather than reading
     * their values immediately) leaves every collected reference pointing at whatever row the
     * iterator most recently advanced to, silently aliasing every "row" to the last one read.
     */
    private static List<Map<String, String>> allRows(ClickHouseHelperClient chc, String sql, String... columns)
            throws Exception {
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
