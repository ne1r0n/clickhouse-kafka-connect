package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.transforms.HeaderToValue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.sink.SinkRecord;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * ClickHouse-backed round-trips for TYPE-03/04/05/07: {@code JSON}, parameterized
 * {@code JSON(...)} and {@code Dynamic} columns through both D-13 insert routes - schemaless
 * {@code JSONEachRow} and {@code bypassRowBinary=true} Struct records - with typed SQL read-back
 * (D-14) rather than a row count, plus non-ASCII values under the {@code windows-1252} test JVM
 * (D-15/D-18). New fork-only class (D-16): it does not extend or modify any upstream test class.
 */
public class JsonDynamicRoundTripTest extends ClickHouseBase {

    private static void createTable(ClickHouseHelperClient chc, String tableName, String createTableQuery) {
        try (Records ignored = chc.queryV2(String.format(createTableQuery, tableName))) {
            // table created
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("TYPE-03: JSON and Dynamic columns round-trip through the schemaless JSONEachRow route with typed read-back")
    void jsonAndDynamicRoundTripThroughSchemalessRoute(TestInfo testInfo) throws Exception {
        String tableName = createTopicName("json_dynamic_schemaless");
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        createTable(chc, tableName,
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
        Map<String, String> props = getBaseProps();
        props.put("bypassRowBinary", "true");
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        createTable(chc, tableName,
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
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        createTable(chc, tableName,
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
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        createTable(chc, tableName,
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

    @Test
    @DisplayName("TYPE-07: Cyrillic, CJK, non-BMP and empty values round-trip byte-identically through String, JSON and Dynamic columns")
    void nonAsciiAndBoundaryValuesRoundTripThroughAllColumnKinds(TestInfo testInfo) throws Exception {
        String tableName = createTopicName("type07_values");
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        createTable(chc, tableName,
                "CREATE TABLE %s (id UInt32, str_col String, json_col JSON, dynamic_col Dynamic) ENGINE = MergeTree ORDER BY id");
        props.put("name", uniqueConnectorName(testInfo));

        // Every non-ASCII fixture is built at runtime from raw Unicode code points (D-18) - never
        // a Unicode escape literal and never a raw non-ASCII byte in this source file.
        String cyrillic = new String(new int[] {0x043F, 0x0440, 0x0438, 0x0432, 0x0435, 0x0442}, 0, 6); // "privet" (Cyrillic)
        String cjk = new String(new int[] {0x4F60, 0x597D}, 0, 2); // "nihao" (CJK)
        String nonBmp = new String(new int[] {0x1F600}, 0, 1); // single non-BMP code point - a Java surrogate pair
        String quoteAndBackslash = "a\"b\\c"; // ASCII only - T-03-10's JSON-escaping mitigation
        String empty = "";

        try {
            // ClickHouse's JSON type requires an object at the root (a bare scalar is rejected with
            // "JSON object should start with '{'"), so every non-empty json_col fixture below wraps
            // its value under a "value" key; the empty-object edge (row 4) stays a true root `{}`.
            Map<String, Object> value1 = new LinkedHashMap<>();
            value1.put("id", 1);
            value1.put("str_col", cyrillic);
            value1.put("json_col", Map.of("value", cyrillic));
            value1.put("dynamic_col", cyrillic);

            Map<String, Object> value2 = new LinkedHashMap<>();
            value2.put("id", 2);
            value2.put("str_col", cjk);
            value2.put("json_col", Map.of("value", cjk));
            value2.put("dynamic_col", cjk);

            Map<String, Object> value3 = new LinkedHashMap<>();
            value3.put("id", 3);
            value3.put("str_col", nonBmp);
            value3.put("json_col", Map.of("value", nonBmp));
            value3.put("dynamic_col", nonBmp);

            Map<String, Object> value4 = new LinkedHashMap<>();
            value4.put("id", 4);
            value4.put("str_col", empty);
            value4.put("json_col", new LinkedHashMap<>()); // JSON object with no keys - TYPE-07 empty edge
            value4.put("dynamic_col", empty);

            Map<String, Object> value5 = new LinkedHashMap<>();
            value5.put("id", 5);
            value5.put("str_col", quoteAndBackslash);
            value5.put("json_col", Map.of("value", quoteAndBackslash));
            value5.put("dynamic_col", quoteAndBackslash);

            List<SinkRecord> records = List.of(
                    new SinkRecord(tableName, 0, null, null, null, value1, 1),
                    new SinkRecord(tableName, 0, null, null, null, value2, 2),
                    new SinkRecord(tableName, 0, null, null, null, value3, 3),
                    new SinkRecord(tableName, 0, null, null, null, value4, 4),
                    new SinkRecord(tableName, 0, null, null, null, value5, 5));

            ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
            sinkTask.start(props);
            sinkTask.put(records);
            sinkTask.stop();

            assertEquals(5, ClickHouseTestHelpers.countRows(chc, tableName));

            // Whole-row read-back through the UTF-8-safe helper (Phase 1 D-11).
            List<JSONObject> rows = ClickHouseTestHelpers.getAllRowsAsJson(chc, tableName);
            Map<Integer, JSONObject> byId = new LinkedHashMap<>();
            for (JSONObject row : rows) {
                byId.put(row.getInt("id"), row);
            }
            assertEquals(5, byId.size());

            assertExactRoundTrip(cyrillic, byId.get(1).getString("str_col"));
            assertExactRoundTrip(cyrillic, byId.get(1).getJSONObject("json_col").getString("value"));
            assertExactRoundTrip(cyrillic, byId.get(1).getString("dynamic_col"));

            assertExactRoundTrip(cjk, byId.get(2).getString("str_col"));
            assertExactRoundTrip(cjk, byId.get(2).getJSONObject("json_col").getString("value"));
            assertExactRoundTrip(cjk, byId.get(2).getString("dynamic_col"));

            assertExactRoundTrip(nonBmp, byId.get(3).getString("str_col"));
            assertExactRoundTrip(nonBmp, byId.get(3).getJSONObject("json_col").getString("value"));
            assertExactRoundTrip(nonBmp, byId.get(3).getString("dynamic_col"));

            assertExactRoundTrip(empty, byId.get(4).getString("str_col"));
            assertEquals(0, byId.get(4).getJSONObject("json_col").length());
            assertExactRoundTrip(empty, byId.get(4).getString("dynamic_col"));

            assertExactRoundTrip(quoteAndBackslash, byId.get(5).getString("str_col"));
            assertExactRoundTrip(quoteAndBackslash, byId.get(5).getJSONObject("json_col").getString("value"));
            assertExactRoundTrip(quoteAndBackslash, byId.get(5).getString("dynamic_col"));

            // Independent typed-SELECT confirmation for the plain-String columns (D-14 typed read-back,
            // not just the JSONEachRow-format helper above).
            List<Map<String, String>> typedRows = allRows(chc, String.format(
                    "SELECT id, str_col, dynamic_col FROM `%s` ORDER BY id", tableName),
                    "id", "str_col", "dynamic_col");
            assertEquals(5, typedRows.size());
            assertExactRoundTrip(cyrillic, typedRows.get(0).get("str_col"));
            assertExactRoundTrip(cyrillic, typedRows.get(0).get("dynamic_col"));
            assertExactRoundTrip(cjk, typedRows.get(1).get("str_col"));
            assertExactRoundTrip(nonBmp, typedRows.get(2).get("str_col"));
            assertExactRoundTrip(empty, typedRows.get(3).get("str_col"));
            assertExactRoundTrip(quoteAndBackslash, typedRows.get(4).get("str_col"));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    @Test
    @DisplayName("TYPE-07: a Cyrillic SMT header value round-trips byte-identically through HeaderToValue into a String column")
    void nonAsciiHeaderValueRoundTripsThroughHeaderToValueSmt() throws Exception {
        String tableName = createTopicName("type07_header");
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        createTable(chc, tableName,
                "CREATE TABLE %s (id UInt32, header_value String) ENGINE = MergeTree ORDER BY id");

        // Cyrillic code points U+0437 U+0430 U+0433 U+043E U+043B U+043E U+0432 U+043E U+043A spell
        // the word for "header" ("zagolovok"), built at runtime per D-18.
        String cyrillicHeaderValue = new String(
                new int[] {0x0437, 0x0430, 0x0433, 0x043E, 0x043B, 0x043E, 0x0432, 0x043E, 0x043A}, 0, 9);

        try (HeaderToValue<SinkRecord> transform = new HeaderToValue<>()) {
            Map<String, Object> transformConfig = new HashMap<>();
            transformConfig.put(HeaderToValue.FIELDS_CONFIG, "header_value");
            transformConfig.put(HeaderToValue.HEADERS_CONFIG, "x-source");
            transform.configure(transformConfig);

            ConnectHeaders headers = new ConnectHeaders();
            headers.addString("x-source", cyrillicHeaderValue);

            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", 1);

            SinkRecord record = new SinkRecord(
                    tableName, 0, null, null, null, value, 1,
                    System.currentTimeMillis(), TimestampType.CREATE_TIME, headers);

            SinkRecord transformed = transform.apply(record);

            ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
            sinkTask.start(props);
            sinkTask.put(List.of(transformed));
            sinkTask.stop();

            assertEquals(1, ClickHouseTestHelpers.countRows(chc, tableName));

            List<JSONObject> rows = ClickHouseTestHelpers.getAllRowsAsJson(chc, tableName);
            assertEquals(1, rows.size());
            assertExactRoundTrip(cyrillicHeaderValue, rows.get(0).getString("header_value"));
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

    private static void assertExactRoundTrip(String expected, String actual) {
        assertEquals(expected, actual);
        assertEquals(expected.codePointCount(0, expected.length()), actual.codePointCount(0, actual.length()));
    }
}
