# Phase 13: Fork Hardening on the Merged Tree - Pattern Map

**Mapped:** 2026-10-08
**Files analyzed:** 22 (5 main modified, 1 main new, 1 main additive getter, 15 test files new/modified)
**Analogs found:** 21 / 22 (1 partial: the route-scoped JSON writer has no in-repo analog for schema-aware generator writing)

All paths below were verified with `git ls-files`. Note: `ClickHouseTestHelpers`, `SchemaTestData`, `SchemalessTestData` now live in `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/` (moved in v1.7.0), not `src/test`. `HeaderToValueTest` is in package `transforms` at `src/test/java/transforms/HeaderToValueTest.java`.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (`doInsertBatch` routing, `:238`) | service/router | request-response | itself (`:232-251`) | exact (in-place edit) |
| `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (`writeRecordsJson`, `:1279`) | service | streaming/transform | itself | exact |
| `src/main/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriter.java` (NEW, planner may rename) | utility/serializer | transform | `util/DataJson.java` + `sink/data/StructToJsonMap.java` | partial |
| `ClickHouseWriter.java` `doWriteColValue` ARRAY/MAP/TUPLE loops (`:613-699`) | service | streaming (RowBinary) | MAP WRT-06 guard at `:614-626` | exact |
| `ClickHouseWriter.java` `cleanupExtraFields` (`:1413-1416`) | service | transform | itself | exact |
| `ClickHouseWriter.java` `validateDataSchema` (INT256/UINT256 gate, `:285-340`) | service | validation | `case "DYNAMIC": case "UNKNOWN":` at `:332-337` | exact |
| `src/main/java/com/clickhouse/kafka/connect/sink/data/Data.java` (add `getSchema()`) | model | n/a | existing getters `:15-31` | exact |
| `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` (`shouldSkipDescribeRow`, `:471`) | utility | transform | itself | exact |
| `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` (`:213`) | model | transform | itself (signature `:193`) | exact |
| `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` (`:440-443`) | config | n/a | sibling `define` for DATABASE / constants `:83-84` | exact |
| `src/test/java/.../sink/db/MetadataFreeJsonInsertUnitTest.java` (add routing + operand-order tests) | test (unit, Docker-free) | request-response | itself (`RecordingAutoEvolveWriter` `:178-197`) | exact |
| `src/test/java/.../sink/ExplicitJsonInsertPathLiveTest.java` (flip R-01 pin `:278-343`) | test (live) | CRUD | itself (`:51-110` for typed read-back) | exact |
| NEW live parity-matrix test (e.g. `.../sink/MetadataFreeJsonTypeParityLiveTest.java`) | test (live) | CRUD | `ExplicitJsonInsertPathLiveTest` + `RowBinaryFailFastLiveTest` | role-match |
| `src/test/java/.../sink/db/NestedNullableCompositeWriteTest.java` (new TUPLE/ARRAY/MAP pins) | test (unit) | transform | itself `:31-84` | exact |
| NEW nested-null live test (cause-chain walk) | test (live) | CRUD | `src/test/java/.../sink/RowBinaryFailFastLiveTest.java` `:40-96` | exact |
| `src/test/java/.../sink/db/RowBinaryUnsupportedTypeGateTest.java` (INT256/UINT256) | test (unit) | validation | itself | exact |
| `src/test/java/.../sink/db/helper/ClickHouseHelperClientForkUnitTest.java` (DESC-05) | test (unit) | transform | itself `:98-123` | exact |
| `src/test/java/.../sink/db/mapping/ColumnTest.java` (COLS-07) | test (unit) | transform | itself | exact |
| NEW CFG-04 test (or add to `ClickHouseVersionDefaultUnitTest` style class) | test (unit) | n/a | `src/test/java/.../sink/ClickHouseVersionDefaultUnitTest.java` | role-match |
| `src/test/java/transforms/HeaderToValueTest.java` (DEBT-01) | test (unit) | transform | itself `:17-59` | exact |
| `src/test/java/.../sink/JsonColumnTest.java` (DEBT-02) | test (live) | CRUD | itself `:58-81`, `ExplicitJsonInsertPathLiveTest.allRows` | role-match |
| `src/test/java/.../sink/ClickHouseSinkTaskJsonInsertFormatTest.java` (DEBT-03) | test (live) | CRUD | `ExplicitJsonInsertPathLiveTest` try/finally | exact |
| `src/test/java/.../sink/ClickHouseBase.java` (`tearDown`, `:131-153`) | test base | n/a | itself | exact |

## Pattern Assignments

### `ClickHouseWriter.java` routing (JSONP-05, D-05/D-06)

**Analog:** itself, `:232-251`. The whole change is the condition on `:238`.

```java
case SCHEMA:
case DEBEZIUM_CDC:
    if (csc.isBypassRowBinary()) {            // becomes: csc.isBypassRowBinary() || shouldSkipTableMetadata()
        doInsertJson(records, table, queryId);
    } else {
        doInsertRawBinary(records, table, queryId, table.hasDefaults(), true);
    }
    break;
```

Predicate to reuse (`:1632-1633`): `private boolean shouldSkipTableMetadata() { return csc != null && csc.isJsonInsertFormatExplicit(); }`. Never key on `csc.getInsertFormat()`.

**Docker-free routing test seam:** `MetadataFreeJsonInsertUnitTest.java:178-197` subclasses the writer (`super(statistics)`) and overrides `protected` hooks. Copy the shape and override `doInsertJson` / `doInsertRawBinary` to record which one ran; inject config with the reflective `setSinkConfig(writer, csc)` helper (`:44-54`) and `minimalProps()` (`:56-64`). Records come from `schemaRecordWithOneField(topic, database)` (`:199`, read it for the exact builder).

### `ClickHouseWriter.writeRecordsJson` (JSONP-06/07, D-06/D-07/D-19)

**Analog:** itself, `:1279-1319`. Current per-record flow to preserve for non-metadata-free routes:

```java
switch (record.getSchemaType()) {
    case SCHEMA:
        data = new HashMap<>(16);
        Struct struct = (Struct) record.getSinkRecord().value();
        for (Field field : struct.schema().fields()) {
            data.put(field.name(), struct.get(field));//Doesn't handle multi-level object depth
        }
        break;
    case DEBEZIUM_CDC:
        data = new HashMap<>(record.getJsonMap().size());
        record.getJsonMap().forEach((k, v) -> data.put(k, v.getObject()));
        break;
    default:
        data = (Map<String, Object>) record.getSinkRecord().value();
        break;
}
long beforeSerialize = System.currentTimeMillis();
Map<String, Object> cleaned = cleanupExtraFields(data, table);
byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(cleaned);
...
statistics.bytesInserted(bytes.length);
BinaryStreamUtils.writeBytes(stream, bytes);
```

Copy: keep the `getSinkRecord().value() != null` tombstone guard and warn-skip else-branch (`:1282`, `:1316-1318`); keep `statistics.bytesInserted` and the trace-log shape (`:1307-1313`, no values at INFO/DEBUG). Gate the new path on `shouldSkipTableMetadata()` only. D-19: in the CDC branch, `continue` when `record.getJsonMap()` is empty on the metadata-free route (before writing bytes). Keep output byte-based (`writeValueAsBytes`-style UTF-8 generator) so `writeUTF8String` is exact (RESEARCH Pitfall 2).

### `MetadataFreeJsonWriter.java` (NEW)

**Analogs:** `src/main/java/com/clickhouse/kafka/connect/util/DataJson.java` (shared `OBJECT_MAPPER`: `NON_NULL`, `JacksonStructSerializer`; use `OBJECT_MAPPER.copy()`, never mutate it) and `src/main/java/com/clickhouse/kafka/connect/sink/data/StructToJsonMap.java` (detects logical types by `LOGICAL_NAME`; copy its detection pattern but it keeps raw objects). Read both before planning: I did not extract excerpts since neither contains generator-level writing. No in-repo analog exists for the schema-walking generator; use RESEARCH.md "Code Examples" (decimal seconds `BigDecimal.valueOf(ms, 3)`, integer days via `Math.floorDiv`, `writeUTF8String`, `java.sql.Date#toLocalDate().toEpochDay()`).

### `doWriteColValue` nested-null checks (WRT-07/08, D-09..D-12, D-16)

**Analog (template):** MAP branch WRT-06 guard, `ClickHouseWriter.java:613-633`:

```java
case MAP:
    if (value.getObject() == null && col.isNullable()) {
        BinaryStreamUtils.writeNull(stream);
        break;
    }
    Map<?, ?> mapTmp = (Map<?, ?>) value.getObject();
    if (mapTmp == null) {
        if (defaultsSupport) {
            BinaryStreamUtils.writeVarInt(stream, 0);
        }
        break;
    }
```

Container loops where the nested-null check goes (Pattern 1 in RESEARCH; signature of `doWriteCol`/4-arg `doWriteColValue` must not change):
- MAP forEach, `:636-646` (value written with `doWriteColValue(col.getMapValueType(), stream, new Data(value.getNestedValueSchema(), mapValue), defaultsSupport)`)
- ARRAY forEach, `:658-667` (`if (col.getArrayType().isNullable() && v != null) writeNonNull`)
- TUPLE early-null branch, `:671-677` (currently unconditional `writeNull`; D-10/D-16: `col.isNullable()` -> `writeNull`, else `DataException`)
- TUPLE field loop, `:688-698`, which routes through `doWriteCol(innerData, containsKey, column, stream, false)` and today throws bare `RuntimeException` (`:982`/`:997`, untouched per D-11).

D-09: nested null ARRAY/MAP in a non-nullable slot writes `BinaryStreamUtils.writeVarInt(stream, 0)` regardless of `defaultsSupport` (note the existing ARRAY null branch at `:651-654` writes `writeNonNull` only when `defaultsSupport`, which is the accidental behaviour). Exception style: `throw new DataException(msg)` as at `:336`, `:524`, `:766`. D-12 message: column name, type, slot kind, no values, e.g. `column 'tags' (Array(String)) element: null in non-nullable slot`. Use a static message-format constant (AGENTS.md) and the existing `buildRowBinaryUnsupportedTypeMessage(colName, type)` helper (`:331`) as the format-method precedent.

### `cleanupExtraFields` (JSONP-08, D-13)

**Analog:** itself, `:1413-1416`:

```java
protected Map<String, Object> cleanupExtraFields(Map<String, Object> m, Table t) {
    if (csc.isBypassFieldCleanup() || shouldSkipTableMetadata()) {   // swap operands
        return m;
    }
```

Test pattern: `MetadataFreeJsonInsertUnitTest.cleanupExtraFieldsBypassDiffersByInsertFormat` (`:98-123`) builds `new ClickHouseWriter(new SinkTaskStatistics(null, 0))`, a `Table("test_db","test_table",false,List.of(knownCol),1)` and injects config reflectively. For the Mockito pin: `Mockito.mock(ClickHouseSinkConfig.class)` with `isJsonInsertFormatExplicit()` returning true, inject via `setSinkConfig`, then `verify(csc, never()).isBypassFieldCleanup()`. Update the Javadoc/comment near `:1624-1631` so it stops claiming null-safety.

### `validateDataSchema` INT256/UINT256 gate (TYPE-08)

**Analog:** `ClickHouseWriter.java:332-337`:

```java
case "DYNAMIC":
case "UNKNOWN": {
    String unsupportedTypeMsg = buildRowBinaryUnsupportedTypeMessage(colName, type);
    LOGGER.error(unsupportedTypeMsg);
    throw new DataException(unsupportedTypeMsg);
}
```

Add `case "INT256": case "UINT256":` here (the switch keys on `type.name()`, `:312`). The switch is only reached for `!isNullable && !hasDefault && type != VARIANT` (`:292`), so tests must use a non-nullable, no-default column. Test analog: `RowBinaryUnsupportedTypeGateTest` (imports at `:1-25` show `DataException`, `Table`, `Record`, `SchemaType`, `ByteArrayOutputStream`; it asserts no leaked secret). Int128/UInt128 round-trip: the existing pin `ClickHouseSinkTaskWithSchemaTest.testInt32ToInt64ConversionWithConnectSchema` uses INT64 Kafka fields; do not use STRING fields.

### `Data.getSchema()` (D-18)

**Analog:** `Data.java:15-31`, plain getters over `private Schema schema`:

```java
public Schema.Type getFieldType() { return schema.type(); }
public Schema getMapKeySchema() { return schema.keySchema(); }
```

Add `public Schema getSchema() { return schema; }` alongside. Additive only.

### `ClickHouseHelperClient.shouldSkipDescribeRow` (DESC-05)

**Analog:** itself, `:471-486`. Current parent lookup only checks the immediate parent:

```java
String parentName = name.substring(0, lastDot);
Column parent = columnsSoFar.get(parentName);
return parent != null && (parent.getType() == Type.JSON || parent.getType() == Type.DYNAMIC);
```

Replace with a walk over every ancestor prefix; keep the early `!isSubcolumn()` return and the `.null/.size0/.size` suffix rule. Test analog: `ClickHouseHelperClientForkUnitTest.java:98-123` using `Column.extractColumn(newDescriptor("name","Type"))`, a `HashMap<String, Column>`, and `ClickHouseHelperClient.shouldSkipDescribeRow(newDescriptor("x.y.z","Int32"), columns)`; `newDescriptor` comes from `ClickHouseTestHelpers` (testFixtures). Use `@ParameterizedTest` + `@ValueSource` as at `:111-123`. Must-not-widen cases: `nt.a.b` (Tuple in Tuple), `aj.k` (Array(JSON)).

### `Column.extractColumn` (COLS-07)

**Analog:** `Column.java:193` signature and `:213` call:

```java
public static Column extractColumn(String name, String valueType, boolean isNull, boolean hasDefaultValue, boolean isSubColumn, int arrayDepth)
...
Column arrayType = extractColumn(name, valueType.substring("Array".length() + 1, valueType.length() - 1), false, isSubColumn, hasDefaultValue, arrayDepth + 1);
```

Fix: swap to `false, hasDefaultValue, isSubColumn, arrayDepth + 1`. Test analog: `ColumnTest` (`src/test/java/.../sink/db/mapping/ColumnTest.java`, uses static imports `col` and `newDescriptor`, plain `@Test` methods, no Docker). Assert `getArrayType().hasDefault()` / `isSubColumn()` on a descriptor built with default and subcolumn flags set. Run the full suite after (RESEARCH Pitfall 5).

### `ClickHouseSinkConfig` USERNAME default (CFG-04)

**Analog:** `ClickHouseSinkConfig.java:440-443` (bug) with constants at `:83-84` and runtime default at `:224`:

```java
configDef.define(USERNAME,
        ConfigDef.Type.STRING,
        passwordDefault,        // -> usernameDefault
```

Test shape: Docker-free class extending nothing, in the style of `ClickHouseVersionDefaultUnitTest` (`src/test/java/.../sink/ClickHouseVersionDefaultUnitTest.java`, plain `assertEquals` with an explanatory message). Assert `ClickHouseSinkConfig.CONFIG.configKeys().get(ClickHouseSinkConfig.USERNAME).defaultValue` equals `"default"`; grep the config class for the actual static ConfigDef accessor name before writing (not verified).

### `ExplicitJsonInsertPathLiveTest` R-01 flip

**Analog:** the same file. Replace the `assertEquals(0, countRows(...))` flow of `explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch` (`:278-343`) with the typed read-back style of `metadataFreeModeResolvesRemappedTableAndLandsEveryColumnValue` (`:51-110`):

```java
ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
ClickHouseTestHelpers.dropTable(chc, tableName);
ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
try {
    assertDoesNotThrow(() -> sinkTask.start(props));
    createTable(chc, tableName, "CREATE TABLE %s (...) ENGINE = MergeTree ORDER BY id");   // before put, per STATE rule
    ...
    List<Map<String, String>> rows = allRows(chc, String.format("SELECT ... ORDER BY id", tableName), "id", "str");
} finally {
    sinkTask.stop();
    ClickHouseTestHelpers.dropTable(chc, tableName);
}
```

Reuse the private helpers `allRows` (`:365-378`, snapshots each `GenericRecord` into a `Map` during iteration, required) and `messageChainContains` (`:346-355`). Keep an origin note in the Javadoc; remove "R-01 PIN - NOT AN ENDORSEMENT" and the `countRows`-zero assertion deliberately (commit message cites the milestone approval). Do not use `countRows` for new assertions. Builder for the Struct: `SchemaBuilder.struct().field(...)`, `new Struct(schema).put(...)`, `new SinkRecord(topic, 0, null, null, schema, value, i)` (`:322-330`).

### New live parity-matrix test (D-01, D-04, D-06, D-07)

**Analogs:** `ExplicitJsonInsertPathLiveTest` for structure; `RowBinaryFailFastLiveTest.java:40-96` for `extends ClickHouseBase`, `getBaseProps()`, `createTopicName(...)`, `uniqueConnectorName(testInfo)`, `props.put("name", ...)`. Run the same `Struct` through two `ClickHouseSinkTask`s (one `insertFormat=json`, one default RowBinary, separate tables or a route column) and compare per-column `SELECT` text; no hand-authored expected values. Keep Debezium `Date` out of the equality (RESEARCH Pitfall 3). Use UTF-8 explicitly in any new reader (STATE rule).

### Nested-null live test (WRT-08)

**Analog:** `RowBinaryFailFastLiveTest.java:67-96`:

```java
RuntimeException ex = assertThrows(RuntimeException.class, () -> sinkTask.put(List.of(record)));
StringBuilder causeChain = new StringBuilder();
Throwable current = ex;
while (current != null) {
    if (current.getMessage() != null) { causeChain.append(current.getMessage()).append(" | "); }
    current = current.getCause();
}
assertTrue(causeChain.toString().contains("..."), causeChain.toString());
...
assertEquals(0, ClickHouseTestHelpers.countRows(chc, tableName));   // zero-rows half; prefer a typed SELECT for new tests
```

Assert `instanceof DataException` somewhere in the chain (Processing wraps in `RuntimeException`, RESEARCH Pitfall 1), the column name substring, and `assertFalse(msg.contains(secret))` for no leaked values.

### `NestedNullableCompositeWriteTest` additions

**Analog:** itself, `:31-84`. Direct-call pattern (4-arg signature stays):

```java
ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
Column column = Column.builder().name("nested_tuple").type(Type.TUPLE).isNullable(true).build();
Data nullData = new Data(Schema.OPTIONAL_STRING_SCHEMA, null);
ByteArrayOutputStream out = new ByteArrayOutputStream();
assertDoesNotThrow(() -> writer.doWriteColValue(column, out, nullData, true));
assertArrayEquals(new byte[] {NULL_MARKER}, out.toByteArray());
```

For the non-nullable Tuple/scalar slots use `assertThrows(DataException.class, ...)`. Re-pinning comment convention at `:80-83` ("2026-10-07 user-approved tightening ...") must be copied next to any re-pinned assertion. Existing pins build only `isNullable(true)` tuples, so D-10 has nothing to re-pin; add new tests, never edit existing ones.

### `HeaderToValueTest` (DEBT-01, D-15)

**Analog:** itself, `:17-59`:

```java
try (HeaderToValue<SinkRecord> headerToValue = new HeaderToValue<>()) {
    Map<String, Object> configs = new HashMap<>();
    configs.put("fields", "field1,field2");
    configs.put("headers", "headerKey1,headerKey2");
    headerToValue.configure(configs);
    ConnectHeaders headers = new ConnectHeaders();
    headers.add("headerKey1", "headerValue1", null);
    SinkRecord record = TransformTestHelpers.schemaRecord("topic1", 0, headers);   // schemalessRecord(...) for the Map path
```

Schema path: assert `DataException` ("Failed to transform record", thrown at `HeaderToValue.java:97`) with a non-null cause (`Assertions.assertThrows` + `getCause()`); use a field name in `fields` that exists with an INT32 schema in a `TransformTestHelpers` record (check `TransformTestHelpers` for a suitable fixture or build a Struct inline). Schemaless path (`HeaderToValue.java:107-130`): value replaced by `header.value().toString()`. `DataException` and `DisplayName` are already imported.

### `JsonColumnTest` (DEBT-02) and `ClickHouseSinkTaskJsonInsertFormatTest` (DEBT-03)

**Analogs:** `JsonColumnTest.java:58-81` (table `json_data JSON`, `countRows` only today; add typed read-back via the `allRows`-style snapshot helper from `ExplicitJsonInsertPathLiveTest:365-378`). Expected observed values (RESEARCH): `nested_str`=`"test"+i`, `nested_int`=`i`, `nested_bytes` base64 text for Jackson configs and `Array(Int64)` for Gson configs. `ClickHouseSinkTaskJsonInsertFormatTest`: wrap start/put in `try { ... } finally { sinkTask.stop(); ClickHouseTestHelpers.dropTable(chc, topic); }` exactly as `ExplicitJsonInsertPathLiveTest:63-109`.

### `ClickHouseBase.tearDown` (DEBT-04, D-14)

**Analog:** itself, `:131-153`. Current catch to change (`:142-146`):

```java
try (var tmpClient = ClickHouseTestHelpers.createClient(getBaseProps())) {
    ClickHouseTestHelpers.dropDatabase(tmpClient, database);
} catch (Exception e) {
    LOGGER.error("Error dropping database", e);       // -> wrap with getClass().getSimpleName() + database, rethrow
}
```

`copyServerLogs(ch)` already runs first (`:138`); keep that order. Check subclasses calling `super.tearDown()` (grep before editing; not enumerated here).

## Shared Patterns

### Exception type for connector-detected data problems
**Source:** `ClickHouseWriter.java:336`, `:524`, `:766`, `:803` (`throw new DataException(msg)`), `import org.apache.kafka.connect.errors.DataException;` at `:30`.
**Apply to:** nested-null checks (D-10/D-12), INT256/UINT256 gate. Messages carry column, type, slot kind; never record values or map keys (AGENTS.md).

### Docker-free test class convention
**Source:** `MetadataFreeJsonInsertUnitTest.java:27-42`, `NestedNullableCompositeWriteTest.java:15-26`, `ClickHouseVersionDefaultUnitTest.java`.
**Apply to:** every new unit test. Extend nothing (not `ClickHouseBase`, whose `@Tag("clickhouse")` drops the class out of `testUnit`), construct `new ClickHouseWriter(new SinkTaskStatistics(null, 0))`, `@DisplayName` with the requirement ID prefix (e.g. `"WRT-08: ..."`), JUnit 5 static assertion imports.

### Live test convention
**Source:** `ExplicitJsonInsertPathLiveTest.java:41-110`, `RowBinaryFailFastLiveTest.java:28-96`.
**Apply to:** all live tests. `extends ClickHouseBase`; `getBaseProps()`; `createTopicName(...)`; `props.put("name", uniqueConnectorName(testInfo))`; `ClickHouseTestHelpers.createClient(props)`; create the table before `start()`; `try { ... } finally { sinkTask.stop(); dropTable }`; typed `SELECT` read-back with a per-row `Map` snapshot; `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests "*Class" --offline`.

### Cause-chain assertion for errors through `put()`
**Source:** `RowBinaryFailFastLiveTest.java:71-84`; `ExplicitJsonInsertPathLiveTest.messageChainContains` (`:346-355`).
**Apply to:** WRT-08 live tests (Processing wraps in `RuntimeException`; D-12 substring assertions).

### Re-pin annotation
**Source:** `NestedNullableCompositeWriteTest.java:80-83`.
**Apply to:** any CR-02 pin re-pinned under the 2026-10-07 approval (dated rationale comment next to the assertion).

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `MetadataFreeJsonWriter.java` generator-level schema-aware writing | serializer | transform | No existing code writes a `Schema`-walked value straight to a Jackson `JsonGenerator`; use `DataJson.OBJECT_MAPPER.copy()` plus RESEARCH.md "Code Examples" |

## Metadata

**Analog search scope:** `src/main/java/com/clickhouse/kafka/connect/{sink,sink/db,sink/db/helper,sink/db/mapping,sink/data,util,transforms}`, `src/test/java/**` (sink, db, transforms), `src/testFixtures/java/**`
**Files scanned/read:** ~20 (git-tracked verified via `git ls-files`; no gitignored mirrors involved)
**Pattern extraction date:** 2026-10-08
**Not verified (planner to check):** `ClickHouseSinkConfig` static `ConfigDef` accessor name for the CFG-04 test; `TransformTestHelpers` fixtures for an INT32-field Struct; subclasses calling `super.tearDown()`; the body of `schemaRecordWithOneField` (`MetadataFreeJsonInsertUnitTest.java:199`).
