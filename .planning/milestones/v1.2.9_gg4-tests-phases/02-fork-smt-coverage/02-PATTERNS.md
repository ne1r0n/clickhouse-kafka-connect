# Phase 2: Fork SMT Coverage - Pattern Map

**Mapped:** 2026-09-16
**Files analyzed:** 11 (4 main-code fixes, 4 growing test files, 1 new shared test helper, 1 new sink-level test class, 1 doc fix)
**Analogs found:** 11 / 11

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `src/main/java/com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson.java` (D-01 fix) | transform (SMT) | transform | itself (`convertFieldValue()` line 238) | exact — self-modification, existing method extended |
| `src/main/java/com/clickhouse/kafka/connect/transforms/HeadersToValue.java` (D-02 fix) | transform (SMT) | transform | itself (`applyWithSchema()` line 109) + `HeaderToValue.java` (same fix pattern) | exact — self-modification |
| `src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java` (D-02 fix) | transform (SMT) | transform | itself (`applyWithSchema()` line 137) + `HeadersToValue.java` (same fix pattern) | exact — self-modification |
| `src/main/java/com/clickhouse/kafka/connect/transforms/TableOperationFilter.java` (D-03 fix) | transform (SMT) | transform | itself (`configure()` line 55) | exact — self-modification |
| `src/test/java/transforms/TableOperationFilterTest.java` (grows) | test (unit) | request-response (apply/assert) | itself (178 lines / 9 tests today) | exact — same file grows |
| `src/test/java/transforms/HeadersToValueTest.java` (grows) | test (unit) | request-response | itself (85 lines / 2 tests today) | exact — same file grows |
| `src/test/java/transforms/HeaderToValueTest.java` (grows) | test (unit) | request-response | itself (75 lines / 2 tests today) | exact — same file grows |
| `src/test/java/transforms/ConvertToMapOrJsonTest.java` (grows) | test (unit) | request-response | itself (323 lines / 13 tests today) | exact — same file grows |
| `src/test/java/transforms/TransformTestHelpers.java` (NEW, D-12) | utility (test helper) | transform (record builders + assertion) | `HeadersToValueTest.generateSampleRecord()` (lines 72-83) + `HeaderToValueTest.generateSampleRecord()` (lines 63-74, byte-identical duplicate) | role-match — generalizing an existing duplicated helper |
| `src/test/java/com/clickhouse/kafka/connect/sink/<NewSinkSmtTest>.java` (NEW, D-05..D-08) | test (ClickHouse-backed, sink-level) | request-response (build records → sink task → ClickHouse → query) | `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java` (lifecycle/try-finally/uniqueConnectorName shape) + `src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java` (record-building + transform-before-insert shape, but NOT its missing try/finally) | role-match — no existing sink test both applies an SMT and uses correct lifecycle discipline; composite of two analogs |
| `.planning/REQUIREMENTS.md` (HDRS-04 wording fix, D-14) | config/doc | transform (text edit) | itself (line 38, `## Transforms` section) | exact — same file, one line corrected |

## Pattern Assignments

### `src/main/java/com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson.java` (D-01 — CONV-02 fix)

**Analog:** itself — `convertFieldValue()`, `ConvertToMapOrJson.java:238-254`

**Current (buggy) core pattern:**
```java
// ConvertToMapOrJson.java:238-254
private Object convertFieldValue(Object value) {
    if (value instanceof Struct nestedStruct) {
        return structToMap(nestedStruct);
    }

    if (bytesToBase64) {
        if (value instanceof byte[] bytes) {
            return ENCODER.get().encodeToString(bytes);
        }

        if (value instanceof ByteBuffer buffer) {
            return encodeByteBuffer(buffer);
        }
    }

    return value;
}
```

**Fix to apply (D-01, locked verbatim in CONTEXT.md):** add `List`/`Collection` and `Map` recursion branches above the existing `Struct` branch, applying `convertFieldValue()` recursively to each element/value so both the `Struct→Map` conversion and the `bytes→base64` conversion happen at every depth. Needed imports: `java.util.ArrayList`, `java.util.LinkedHashMap` (both already imported or trivially added — `LinkedHashMap` is already imported at line 9).

**Error handling:** unchanged — the caller (`structToMap()`, line 209-230) is already inside `apply()`'s try/catch (lines 174-200) which wraps everything as `DataException("Failed to transform record", e)` (line 199).

---

### `src/main/java/com/clickhouse/kafka/connect/transforms/HeadersToValue.java` (D-02 — stale schema cache fix)

**Analog:** itself — `applyWithSchema()`, `HeadersToValue.java:109-136`, paired with the identical pattern in `HeaderToValue.java:137-176`

**Current (buggy) core pattern:**
```java
// HeadersToValue.java:109-114
private R applyWithSchema(R record) {
    final Struct oldValue = (Struct) record.value();

    if (valueSchema == null) {
        valueSchema = buildSchema(oldValue.schema());
    }
    ...
```

**Fix to apply (D-02, locked verbatim in CONTEXT.md):**
```java
private Schema sourceSchema;   // new field, alongside existing `valueSchema` (line 41)
private Schema valueSchema;

// in applyWithSchema(record):
final Struct oldValue = (Struct) record.value();
if (valueSchema == null || oldValue.schema() != sourceSchema) {
    sourceSchema = oldValue.schema();
    valueSchema  = buildSchema(sourceSchema);
}
```
Use **reference** comparison (`!=`), not `.equals()` — Connect reuses schema instances per converter, so reference equality is the cheap common case (see CONTEXT.md "Specific Ideas").

**`close()` update:** `close()` (lines 190-195) already nulls `valueSchema` and `headersSchema` — add `sourceSchema = null;` alongside them.

---

### `src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java` (D-02 — stale schema cache fix)

**Analog:** itself — `applyWithSchema()`, `HeaderToValue.java:137-176`, mirrors the `HeadersToValue.java` fix exactly

**Current (buggy) core pattern:**
```java
// HeaderToValue.java:137-143
private R applyWithSchema(R record) {
    final Struct oldValue = (Struct) record.value();

    // Lazily initialize the schema
    if (valueSchema == null) {
        valueSchema = buildSchema(oldValue.schema());
    }
    ...
```

**Fix:** identical shape to `HeadersToValue.java`'s fix — add `private Schema sourceSchema;` field (near `valueSchema` at line 43), guard with `oldValue.schema() != sourceSchema`, and null `sourceSchema` in `close()` (lines 224-228, alongside the existing `valueSchema = null; fieldToHeaderIdx = null;`).

---

### `src/main/java/com/clickhouse/kafka/connect/transforms/TableOperationFilter.java` (D-03 — configure() clear fix)

**Analog:** itself — `configure()`, `TableOperationFilter.java:54-86`

**Current pattern:**
```java
// TableOperationFilter.java:50-58
private Set<String> targetTables = new HashSet<>();
private Set<String> skippedOperations = new HashSet<>();

@Override
public void configure(Map<String, ?> configs) {
    final AbstractConfig config = new AbstractConfig(CONFIG_DEF, configs);

    // Extract configuration values
    String tablesStr = config.getString(TABLES_CONFIG);
    ...
```

**Fix to apply (D-03):** add `targetTables.clear(); skippedOperations.clear();` at the top of `configure()`, right after `AbstractConfig` construction (line 56) and before the `tablesStr`/`operationsStr` parsing. Zero production behavior change (Connect calls `configure()` once per instance) — this only matters for reconfigure-style tests.

---

### `src/test/java/transforms/TableOperationFilterTest.java` (unit test, grows — D-09/D-10/D-11/D-15/D-16)

**Analog:** itself (existing 9 tests) + shared conventions below

**Imports pattern (lines 1-14, extend as needed for new assertions/annotations):**
```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
// ADD for new tests: assertThrows, assertInstanceOf, assertTrue, assertDoesNotThrow

import com.clickhouse.kafka.connect.transforms.TableOperationFilter;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
// ADD: import org.junit.jupiter.api.DisplayName;
// ADD: import org.junit.jupiter.api.params.ParameterizedTest;
// ADD: import org.junit.jupiter.params.provider.ValueSource;  (note actual package: org.junit.jupiter.params.ParameterizedTest / org.junit.jupiter.params.provider.ValueSource)
```

**Core test pattern (build Struct-valued Debezium-shaped record, assert pass-through or filter), lines 80-100:**
```java
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
            .put("op", "c")
            .put("source", source);

    SinkRecord record = new SinkRecord("topic", 0, null, null, recordSchema, value, 0L);
    assertEquals(record, filter.apply(record), "Record with non-skipped operation should pass through unchanged");
}
```

**New matrix pattern to add (TOF-03, D-11):**
```java
@ParameterizedTest(name = "TOF-03: token ''{0}'' is ignored")
@ValueSource(strings = {"x", "r", "", "   ", "cu"})
@DisplayName("TOF-03: invalid tokens ignored, records pass through")
void testInvalidTokenIgnored(String token) {
    Map<String, String> configs = new HashMap<>();
    configs.put("tables", "mydb.mytable");
    configs.put("skipped.operations", token);
    TableOperationFilter<SinkRecord> f = new TableOperationFilter<>();
    assertDoesNotThrow(() -> f.configure(configs));
    // then build a record with op=token (if token is a valid single 'cudt' char it still must
    // NOT be in skippedOperations because configure() rejected it) and assert pass-through
}
```

**`@DisplayName` convention (D-10):**
```java
@Test
@DisplayName("TOF-03: invalid, empty and whitespace tokens are ignored")
public void testInvalidSkippedOperationsIgnored() { ... }
```

**Error handling for negative tests (D-16, TableOperationFilter's distinct message):**
```java
DataException ex = assertThrows(DataException.class, () -> filter.apply(record));
assertEquals("Failed to process record in TableOperationFilter", ex.getMessage());  // NOTE: different from the other 3 SMTs
assertInstanceOf(<CauseType>.class, ex.getCause());
assertTrue(ex.getCause().getMessage().contains("<expected substring>"));
```

**No log-appender assertion (D-15):** do not assert on `LOGGER.warn` text from `TableOperationFilter.java:78-79` — behavioral assertion only (config does not throw + record with that op passes through untouched).

---

### `src/test/java/transforms/HeadersToValueTest.java` (unit test, grows — D-09/D-10/D-12)

**Analog:** itself (existing 2 tests, `generateSampleRecord` at lines 72-83)

**Imports pattern (lines 1-16):**
```java
import com.clickhouse.kafka.connect.transforms.HeadersToValue;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
// ADD: import transforms.TransformTestHelpers; (D-12 shared helper, same package `transforms` so no import needed if TransformTestHelpers is package-private in the same package)
```

**Core try-with-resources pattern (lines 19-46) — every new test method should follow this shape so `close()` is exercised implicitly (Research Pattern 1):**
```java
@Test
public void applySchemalessTest() {
    try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
        Map<String, Object> configs = new HashMap<>();
        configs.put("field", "headers_map");
        headersToValue.configure(configs);
        ...
    }
}
```

**D-02's new test requirement — feed two different schemas through one configured instance:**
```java
// New test, not in the 30 numbered requirements but locked by D-02:
try (HeadersToValue<SinkRecord> x = new HeadersToValue<>()) {
    x.configure(Map.of("field", "headers_map"));
    SinkRecord r1 = /* schema A */;
    SinkRecord out1 = x.apply(r1);
    SinkRecord r2 = /* schema B, different fields */;
    SinkRecord out2 = x.apply(r2);  // must rebuild valueSchema, not reuse stale one from r1
    // assert out2's schema reflects schema B's fields, not schema A's
}
```

**Config-error split (D-14):**
```java
// absent "field" key -> ConfigException (thrown inside new AbstractConfig(...), HeadersToValue.java:46)
Map<String, Object> configs = new HashMap<>(); // no "field" key at all
assertThrows(ConfigException.class, () -> new HeadersToValue<SinkRecord>().configure(configs));

// blank "field" value -> DataException (HeadersToValue.java:49-51)
Map<String, Object> configsBlank = new HashMap<>();
configsBlank.put("field", "   ");
assertThrows(DataException.class, () -> new HeadersToValue<SinkRecord>().configure(configsBlank));
```

---

### `src/test/java/transforms/HeaderToValueTest.java` (unit test, grows — D-09/D-10/D-12)

**Analog:** itself (existing 2 tests, byte-identical `generateSampleRecord` duplicate of `HeadersToValueTest`'s — this duplication is exactly what D-12's `TransformTestHelpers` should remove)

Follow the same imports/try-with-resources/config-error-split shape as `HeadersToValueTest.java` above. Key difference: HDR-01's config-error split covers **two** `NO_DEFAULT_VALUE` keys (`fields`, `headers`, lines 49/55) — absent either → `ConfigException`; a genuine count mismatch (both present, different sizes) → `DataException` at line 67-68:
```java
// HeaderToValue.java:66-69
if (fields.size() != headers.size()) {
    throw new DataException("The number of fields (" + fields.size() +
                            ") must match the number of headers (" + headers.size() + ").");
}
```

---

### `src/test/java/transforms/ConvertToMapOrJsonTest.java` (unit test, grows — D-09/D-10/D-11/D-13/D-16)

**Analog:** itself (existing 13 tests — strongest existing coverage in the group)

**Imports pattern (lines 1-13):**
```java
import static org.junit.jupiter.api.Assertions.*;

import com.clickhouse.kafka.connect.transforms.ConvertToMapOrJson;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;
```

**Core construct→configure→apply→assert pattern (lines 66-82, nested-Struct case — extend for CONV-02's List/Map-nested case after the D-01 fix lands):**
```java
@Test
public void testStructToMapNested() {
    Schema nestedSchema = SchemaBuilder.struct().field("x", Schema.STRING_SCHEMA).build();
    Schema schema = SchemaBuilder.struct()
            .field("foo", Schema.STRING_SCHEMA)
            .field("nested", nestedSchema)
            .build();
    Struct nested = new Struct(nestedSchema).put("x", "val");
    Struct struct = new Struct(schema).put("foo", "abc").put("nested", nested);
    SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
    ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
    xform.configure(new HashMap<>());
    SinkRecord result = xform.apply(record);
    assertTrue(result.value() instanceof Map);
    Map<?,?> map = (Map<?,?>) result.value();
    assertTrue(map.get("nested") instanceof Map);
}
```

**CONV-05 config-error pattern (D-13, case-sensitivity quirk):**
```java
// unknown/wrong-case library -> ConfigException, not DataException
Map<String, Object> config = new HashMap<>();
config.put("output.json.library", "JACKSON"); // rejected even though configure() lowercases
assertThrows(ConfigException.class, () -> new ConvertToMapOrJson<SinkRecord>().configure(config));
```

**CONV-06 sliced/direct ByteBuffer matrix (extends `testStructToMapByteBufferToBase64`, lines 141-158):** add a `ByteBuffer.allocateDirect(...)` and a `.slice()` case exercising `encodeByteBuffer()`'s `duplicate()`+copy fallback branch (`ConvertToMapOrJson.java:266-272`).

**Preserve-null matrix (CONV-01) across Jackson and Gson — mirrors the existing dual-library pattern at lines 17-64** (`testDateTypesJsonJacksonVsGson`).

---

### `src/test/java/transforms/TransformTestHelpers.java` (NEW — D-12)

**Analog:** `HeadersToValueTest.generateSampleRecord()` (`HeadersToValueTest.java:72-83`) and its byte-identical duplicate `HeaderToValueTest.generateSampleRecord()` (`HeaderToValueTest.java:63-74`)

**Pattern to generalize (source, verbatim):**
```java
private SinkRecord generateSampleRecord(String topic, int partition, long offset, ConnectHeaders headers) {
    Schema schema = SchemaBuilder.struct()
            .field("off16", Schema.INT16_SCHEMA)
            .field("timestamp_int64", Schema.INT64_SCHEMA)
            .field("date_date", Schema.INT32_SCHEMA)
            .build();
    Struct valueStruct = new Struct(schema)
            .put("off16", (short) new Random().nextInt(Short.MAX_VALUE + 1))
            .put("timestamp_int64", new Date().getTime())
            .put("date_date", (int) (new Date().getTime() / 1000));
    return new SinkRecord(topic, partition, null, null, schema, valueStruct, offset, 0L, TimestampType.NO_TIMESTAMP_TYPE, headers);
}
```

**Target shape (package-private class in `transforms` package, no `public` needed since all 4 test classes are in the same package):**
```java
package transforms;

final class TransformTestHelpers {
    static SinkRecord schemaRecord(String topic, ConnectHeaders h) { /* generalizes generateSampleRecord above */ }
    static SinkRecord schemalessRecord(String topic, ConnectHeaders h) { /* Map-valued variant, same fields as HeadersToValueTest's inline originalValue Map, line 30-31 */ }
    static SinkRecord tombstone(String topic) { /* value == null, per HDRS-01 */ }

    static void assertMetadataPreserved(SinkRecord in, SinkRecord out) {
        // generalizes the implicit contract behind HDRS-06/HDR-05/CONV-04 (newRecord(...) preserving
        // topic/partition/key/timestamp/headers, e.g. ConvertToMapOrJson.java:187-196's 8-arg newRecord)
        assertEquals(in.topic(), out.topic());
        assertEquals(in.kafkaPartition(), out.kafkaPartition());
        assertEquals(in.key(), out.key());
        assertEquals(in.timestamp(), out.timestamp());
        assertEquals(in.headers(), out.headers());
    }
}
```
No imports needed beyond what `HeadersToValueTest.java`/`HeaderToValueTest.java` already use (`java.util.Date`, `java.util.Random`, `java.util.UUID`, `org.apache.kafka.common.record.TimestampType`, `org.apache.kafka.connect.data.{Schema,SchemaBuilder,Struct}`, `org.apache.kafka.connect.header.ConnectHeaders`, `org.apache.kafka.connect.sink.SinkRecord`).

---

### `src/test/java/com/clickhouse/kafka/connect/sink/<NewSinkSmtTest>.java` (NEW — D-05, D-06, D-07, D-08)

**Primary analog (lifecycle discipline):** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java` — specifically `leakedMBeanWasSweptAndUniqueNamesAreSafe()` (lines 140-163) for the try/finally + `uniqueConnectorName(TestInfo)` shape, and `helpersDecodeResponsesAsUtf8()` (lines 172-192) for the createTable→insert→query-and-assert shape.

**Secondary analog (record-building + apply-transform-before-insert shape):** `src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java` — `createTestRecords()` (lines 32-50) and `runJsonTest()` (lines 52-79) for building `SinkRecord`s and applying an SMT before `sinkTask.put()`. **Do NOT copy its missing try/finally around `start()/put()/stop()` (lines 70-73) — that is Pitfall 1/5, a documented anti-pattern, not a pattern to reuse.**

**Class declaration / imports (from `ClickHouseTestHarnessTest.java:1-24` and `JsonColumnTest.java:1-16`):**
```java
package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.transforms.TableOperationFilter; // or HeadersToValue / HeaderToValue / ConvertToMapOrJson
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.transforms.Transformation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

public class <NewSinkSmtTest> extends ClickHouseBase {
    // no @Tag("clickhouse") re-declaration — inherited from ClickHouseBase (Pattern 2)
```

**Auth/harness pattern (createProps → createClient → createTable), from `ClickHouseTestHarnessTest.java:175-179`:**
```java
ClickHouseHelperClient chc = createClient(createProps());
String table = createTopicName("tof06_test"); // or hdrs09_/hdr07_/conv08_ per SMT
ClickHouseTestHelpers.dropTable(chc, table);
ClickHouseTestHelpers.createTable(chc, table, "CREATE TABLE %s (...) ENGINE = MergeTree ORDER BY ...");
```

**D-05's local `transform()` helper (locked verbatim in CONTEXT.md):**
```java
private Collection<SinkRecord> transform(
        Transformation<SinkRecord> xform,
        Collection<SinkRecord> records) {
    List<SinkRecord> out = new ArrayList<>(records.size());
    for (SinkRecord r : records) {
        SinkRecord t = xform.apply(r);
        if (t != null) out.add(t);   // null == filtered by the SMT
    }
    return out;
}
```

**Correct lifecycle pattern — try/finally + `uniqueConnectorName` (from `ClickHouseTestHarnessTest.java:151-163`, the fix for `JsonColumnTest.java`'s missing finally):**
```java
Map<String, String> props = createProps();
createClient(props);
props.put("name", uniqueConnectorName(testInfo));

ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
try {
    sinkTask.start(props);
    sinkTask.put(transformed);
} finally {
    sinkTask.stop();
}
assertEquals(expectedRowCount, ClickHouseTestHelpers.countRows(chc, table));
```

**Table shapes (D-06/D-07/D-08, locked verbatim in CONTEXT.md, confirmed viable by RESEARCH.md's R-01/R-02):**
```sql
-- TOF-06
CREATE TABLE %s (`id` Int32, `op` String, `source` Tuple(db String, `table` String)) ENGINE = MergeTree ORDER BY id

-- HDRS-09
CREATE TABLE %s (`off16` Int16, `str` String, `headers_map` Map(String, Nullable(String))) ENGINE = MergeTree ORDER BY off16

-- HDR-07
CREATE TABLE %s (`off16` Int16, `str` String, `trace_id` String) ENGINE = MergeTree ORDER BY off16

-- CONV-08
CREATE TABLE %s (`off16` Int16, `str` String, `p_int32` Int32, `nested` JSON) ENGINE = MergeTree ORDER BY off16
```

**Record-building pattern for Debezium-shaped fixtures (TOF-06), generalized from `JsonColumnTest.createTestRecords()`:**
```java
private List<SinkRecord> buildDebeziumRecords(String topic) {
    Schema sourceSchema = SchemaBuilder.struct()
            .field("db", Schema.STRING_SCHEMA).field("table", Schema.STRING_SCHEMA).build();
    Schema recordSchema = SchemaBuilder.struct()
            .field("id", Schema.INT32_SCHEMA).field("op", Schema.STRING_SCHEMA)
            .field("source", sourceSchema).build();
    // op values must be within "cudt" per Pitfall 3 — op=r is structurally never filterable
    ...
}
```

**Error handling:** none needed beyond what `ClickHouseSinkTask` already provides — sink-level tests assert on row counts / row content, not exceptions (that is the unit tests' job per D-16).

---

### `.planning/REQUIREMENTS.md` (D-14 — HDRS-04 wording correction)

**Analog:** itself, line 38

**Current (incorrect) text:**
```
- [ ] **HDRS-04**: Missing or blank `field` configuration is rejected with `DataException` at `configure()`
```

**Correction to apply (per D-14/research):** split into the two distinct outcomes — absent `field` key throws `ConfigException` (from `AbstractConfig` construction, before `HeadersToValue.java`'s explicit check at line 46/50 runs); only a blank/whitespace value reaches the explicit `DataException` at `HeadersToValue.java:50`. Update the requirement text to describe both cases separately, matching the pattern already used elsewhere in REQUIREMENTS.md for split-outcome requirements (see HDR-01 at line ~which already correctly separates "absent" vs "mismatch" per CONTEXT.md's note that HDR-01 as written is correct).

---

## Shared Patterns

### Try-with-resources for `close()` coverage
**Source:** `HeadersToValueTest.java:21`, `HeaderToValueTest.java:21`
**Apply to:** Every new/modified test method in `TableOperationFilterTest.java`, `HeadersToValueTest.java`, `HeaderToValueTest.java`, `ConvertToMapOrJsonTest.java` that constructs a transform (research Pattern 1).
```java
try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
    ...
}
```

### `@DisplayName` requirement-ID prefix (D-10)
**Source:** New convention per CONTEXT.md D-10; no existing file in `transforms/` uses it yet, but 5 other test classes in the repo do (`LocalProviderTest`, `ProcessingTest`, `RangeContainerTest`, `UtilsTest`, `MaskTest`).
**Apply to:** Every new `@Test`/`@ParameterizedTest` method in all four growing unit-test files.
```java
@Test
@DisplayName("TOF-03: invalid, empty and whitespace tokens are ignored")
public void testInvalidSkippedOperationsIgnored() { ... }
```

### Config-error exception-type + cause-substring assertion (D-16)
**Source:** Derived from all 4 SMTs' `apply()`-level wrapping: `TableOperationFilter.java:130-133`, `HeadersToValue.java:70-74`, `HeaderToValue.java:93-97`, `ConvertToMapOrJson.java:197-200`.
**Apply to:** All negative-path unit tests across the four growing test files.
```java
DataException ex = assertThrows(DataException.class, () -> xform.apply(record));
assertEquals("Failed to transform record", ex.getMessage()); // "Failed to process record in TableOperationFilter" for TOF only
assertInstanceOf(DataException.class, ex.getCause());
assertTrue(ex.getCause().getMessage().contains("<expected substring>"));
```

### `ConfigException` vs `DataException` split at `configure()` (D-14)
**Source:** `HeadersToValue.java:46,50` and `HeaderToValue.java:49,55,66-69` (both use `ConfigDef.NO_DEFAULT_VALUE`, so `AbstractConfig` construction throws `ConfigException` before the explicit `DataException` check ever runs).
**Apply to:** `HeadersToValueTest.java` (HDRS-04), `HeaderToValueTest.java` (HDR-01), and `ConvertToMapOrJsonTest.java` (CONV-05, via `ConfigDef.ValidString.in(...)` at `ConvertToMapOrJson.java:67`).

### MBean-safe sink-task lifecycle: try/finally + `uniqueConnectorName(TestInfo)` (Phase 1 D-09/D-10, carried forward)
**Source:** `ClickHouseBase.java:85-97` (`cleanupLeakedMBeans` safety net) and `ClickHouseBase.java:103-106` (`uniqueConnectorName`); correct usage demonstrated in `ClickHouseTestHarnessTest.java:151-163`.
**Apply to:** The new sink-level test class only (D-05..D-08). **Explicitly do not copy** `JsonColumnTest.java:70-73`'s bare `start()/put()/stop()` without try/finally — that is the anti-pattern this shared pattern replaces.

### `ClickHouseTestHelpers` read/assert helpers (never hand-roll response parsing)
**Source:** `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` — `dropTable` (line 75), `createTable` (line 106), `countRows` (line 188), `validateRows` (line 228), `getAllRowsAsJson` (line 152), all UTF-8-safe since Phase 1 D-11.
**Apply to:** The new sink-level test class for all table setup and row-count/content assertions.

## No Analog Found

None. Every file in scope has at least a role-match analog in the existing codebase; the new sink-level test class is the only file needing a composite of two analogs (`ClickHouseTestHarnessTest.java` for lifecycle, `JsonColumnTest.java` for record-building), since no existing sink test both applies an SMT and follows the corrected try/finally discipline.

## Metadata

**Analog search scope:** `src/main/java/com/clickhouse/kafka/connect/transforms/`, `src/test/java/transforms/`, `src/test/java/com/clickhouse/kafka/connect/sink/`, `src/test/java/com/clickhouse/kafka/connect/sink/helper/`, `.planning/REQUIREMENTS.md`
**Files scanned:** 12 (4 main-code transforms, 4 unit test files, `ClickHouseBase.java`, `ClickHouseTestHarnessTest.java`, `JsonColumnTest.java`, `ClickHouseTestHelpers.java`) + `.planning/REQUIREMENTS.md`
**Pattern extraction date:** 2026-09-16
**Tracked-source gate:** all 13 analog/target paths confirmed via `git ls-files` — none are gitignored mirrors.
