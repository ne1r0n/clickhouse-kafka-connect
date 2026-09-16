# Phase 2: Fork SMT Coverage - Research

**Researched:** 2026-09-16
**Domain:** Kafka Connect SMT (Single Message Transform) unit testing + ClickHouse-backed sink-level testing for a Java 17 / Gradle 7.4.2 fork
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

Copied verbatim from `.planning/phases/02-fork-smt-coverage/02-CONTEXT.md`.

**Main-code fixes (bug-vs-pin policy)** — three fixes pre-approved, all small, one method each, inside `transforms/`:

- **D-01: CONV-02 — recurse into collections.** `ConvertToMapOrJson.convertFieldValue()` (`ConvertToMapOrJson.java:238`) only recurses when the value is itself a `Struct`. A `Struct` inside an Array field (`List<Struct>`) or a Map field (`Map<String,Struct>`) passes through raw. Fix: walk `List`/`Collection` elements and `Map` values recursively, applying **both** the `Struct`→`Map` conversion and the `bytes`→base64 conversion at every depth.
  ```java
  private Object convertFieldValue(Object value) {
      if (value instanceof Struct s) return structToMap(s);
      if (value instanceof List<?> list) {
          List<Object> out = new ArrayList<>(list.size());
          for (Object e : list) out.add(convertFieldValue(e));
          return out;
      }
      if (value instanceof Map<?,?> map) {
          Map<Object,Object> out = new LinkedHashMap<>();
          for (var e : map.entrySet())
              out.put(e.getKey(), convertFieldValue(e.getValue()));
          return out;
      }
      if (bytesToBase64) { /* unchanged */ }
      return value;
  }
  ```
  Rejected: converting nested Structs but leaving bytes alone; pinning the broken output.

- **D-02: Stale `valueSchema` cache — add a source-schema guard.** Both `HeadersToValue` (`HeadersToValue.java:112`) and `HeaderToValue` (`HeaderToValue.java:141`) cache `valueSchema` from the first record and never invalidate it. When a Debezium table's schema evolves mid-stream, later records rebuild against the stale schema. Fix: remember the schema the cache was built from and rebuild on change. `close()` nulls the new field too.
  ```java
  private Schema sourceSchema;   // new field
  private Schema valueSchema;

  // in applyWithSchema(record):
  final Struct oldValue = (Struct) record.value();
  if (valueSchema == null || oldValue.schema() != sourceSchema) {
      sourceSchema = oldValue.schema();
      valueSchema  = buildSchema(sourceSchema);
  }
  ```
  Not one of the 30 listed requirements, but a real data-loss path for CDC pipelines. Needs a test feeding two different schemas through one configured transform instance.

- **D-03: `TableOperationFilter.configure()` clears its sets.** `targetTables`/`skippedOperations` (`TableOperationFilter.java:51-52`) are field initializers that `configure()` only adds to. Connect calls `configure()` once per instance so production never notices, but it traps any reconfigure-style test. Fix: `targetTables.clear()` and `skippedOperations.clear()` at the top of `configure()`. Zero production behavior change.

- **D-04: Escalation rule for anything else — Phase 1's D-02 rule applies unchanged.** Fix inline if small (a few lines, one method, rebase-friendly). Anything larger (new method, signature change, or a fix reaching outside `transforms/`) **stops for a user decision**. Never disable or `@Disabled` a test to get green.

**Sink-level test design (TOF-06, HDRS-09, HDR-07, CONV-08):**

- **D-05: SMTs are applied by a shared private helper in the new sink test class**, mirroring the Connect worker: apply the configured transform per record, drop records the transform nulled out.
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
  **Rejected: `org.apache.kafka.connect.runtime.TransformationChain`.** Only `connect-api` is on the classpath (`build.gradle.kts:86`); `TransformationChain` lives in `connect-runtime`. Adding it is a second new build dependency this milestone — against the Tooling constraint (JaCoCo is the only expected new dependency). Reversibility: reversible — a local test helper, no production surface.

- **D-06: TOF-06 keeps the real Debezium envelope shape.** `TableOperationFilter.apply()` returns early unless `record.value() instanceof Struct`, and TOF-04 pins schemaless envelopes as pass-through — so TOF-06 **must** use the schema/Struct path. The table carries a matching `Tuple` column so the nested `source` Struct actually round-trips.
  ```sql
  CREATE TABLE %s (
    `id`     Int32,
    `op`     String,
    `source` Tuple(db String, `table` String)
  ) Engine = MergeTree ORDER BY id
  ```
  Assertion: records with a filtered `op` for a listed table are absent; unfiltered rows are present. Rejected: `bypassSchemaValidation=true` with a flat table; flattening `source` into plain String columns. **See research flag R-01 (resolved below).**

- **D-07: CONV-08 asserts the real end-to-end landing, not a single-column store.** With `output.json.enabled=true` the SMT emits a `String` value with a null schema, which the connector routes through `StringRecordConvertor` — that path parses the JSON and maps **keys to columns**. Top-level keys land in their matching typed columns, and a nested struct field lands in a `JSON` column with correct content.
  ```sql
  CREATE TABLE %s (
    `off16`   Int16,
    `str`     String,
    `p_int32` Int32,
    `nested`  JSON
  ) Engine = MergeTree ORDER BY off16
  ```
  Rejected: wrapping the payload so the whole document lands in one String column; running the SMT with `output.json.enabled=false`. **See research flag R-02 (resolved below).**

- **D-08: HDRS-09 and HDR-07 use the schema/Struct path only.** The map schema `HeadersToValue.configure()` builds — `SchemaBuilder.map(STRING, OPTIONAL_STRING).optional()` — is the fork addition worth pinning at sink level, driving the insert into `Map(String, Nullable(String))`. The schemaless path is already covered by unit tests (HDRS-02, HDR-03).
  ```sql
  -- HDRS-09
  CREATE TABLE %s (
    `off16`       Int16,
    `str`         String,
    `headers_map` Map(String, Nullable(String))
  ) Engine = MergeTree ORDER BY off16

  -- HDR-07
  CREATE TABLE %s (
    `off16`    Int16,
    `str`      String,
    `trace_id` String     -- HeaderToValue adds the field as OPTIONAL_STRING
  ) Engine = MergeTree ORDER BY off16
  ```
  Rejected: a fifth sink test covering HDRS-09 on the schemaless path too; schemaless-only.

**Unit test organization and style:**

- **D-09: Grow the four existing test files**; do not add per-SMT sibling classes. Expect ~350-600 lines each:
  - `src/test/java/transforms/TableOperationFilterTest.java` (177 lines / 9 tests today)
  - `src/test/java/transforms/HeadersToValueTest.java` (84 / 2)
  - `src/test/java/transforms/HeaderToValueTest.java` (74 / 2)
  - `src/test/java/transforms/ConvertToMapOrJsonTest.java` (322 / 13)

  Rejected: `@Nested` grouping — no test in the repo uses it today.

- **D-10: Every new test carries its requirement ID in `@DisplayName`.** Method names keep `test*`.
  ```java
  @Test
  @DisplayName("TOF-03: invalid, empty and whitespace tokens are ignored")
  public void testInvalidSkippedOperationsIgnored() { ... }
  ```

- **D-11: `@ParameterizedTest` for genuine matrices only**, plain `@Test` everywhere else. Matrices: TOF-03's invalid/empty/whitespace/`r` tokens, CONV-01's jackson-vs-gson, schema-vs-schemaless duplicates in HDRS/HDR.
  ```java
  @ParameterizedTest(name = "TOF-03: token ''{0}'' is ignored")
  @ValueSource(strings = {"x", "r", "", "   ", "cu"})
  @DisplayName("TOF-03: invalid tokens ignored, records pass through")
  void testInvalidTokenIgnored(String token) { ... }
  ```

- **D-12: One shared, fork-only helper class in `src/test/java/transforms/`** holding record builders and the metadata-preservation assertion.
  ```java
  final class TransformTestHelpers {
      static SinkRecord schemaRecord(String topic, ConnectHeaders h) {...}
      static SinkRecord schemalessRecord(String topic, ConnectHeaders h) {...}
      static SinkRecord tombstone(String topic) {...}

      static void assertMetadataPreserved(SinkRecord in, SinkRecord out) {
          assertEquals(in.topic(), out.topic());
          assertEquals(in.kafkaPartition(), out.kafkaPartition());
          assertEquals(in.key(), out.key());
          assertEquals(in.timestamp(), out.timestamp());
          assertEquals(in.headers(), out.headers());
      }
  }
  ```

**Config-error contracts:**

- **D-13: CONV-05 — pin `ConfigException`, including the case-sensitivity quirk; change no code.** `CONFIG_DEF` uses `ConfigDef.ValidString.in("jackson", "gson")`, case-sensitive. Pin: unknown library → `ConfigException` (not `DataException`); `output.json.library=JACKSON` is rejected even though `configure()` `.toLowerCase()`s; the `try { JsonLibrary.valueOf } catch → DataException` is unreachable dead code, stays.

- **D-14: HDRS-04 — pin the real error split and correct the requirement text.** Absent `field` key → `ConfigException` from `new AbstractConfig(...)`. Only blank/whitespace value reaches the explicit `DataException`. Test both cases separately. **Update HDRS-04 wording in `.planning/REQUIREMENTS.md`.** Also check HDR-01: `fields`/`headers` are also `NO_DEFAULT_VALUE`, so absent → `ConfigException`, genuine count mismatch → `DataException` (HDR-01 as written already covers only the mismatch, correctly).

- **D-15: TOF-03 — behavioral assertion only, no log appender.** Assert `configure()` does not throw for a bad token and a record whose `op` matches passes through untouched. Do not assert on `LOGGER.warn` text.

- **D-16: Negative tests assert exception type, plus the cause message where that message is the contract.** All three wrap `apply()` failures as `DataException("Failed to transform record", e)` (`TableOperationFilter` uses `"Failed to process record in TableOperationFilter"`), so the diagnostic lives on the cause. Substring match on the inner message, never full-string equality at every level.
  ```java
  DataException ex = assertThrows(DataException.class,
          () -> xform.apply(record));
  assertEquals("Failed to transform record", ex.getMessage());
  assertInstanceOf(DataException.class, ex.getCause());
  assertTrue(ex.getCause().getMessage().contains("must be a Map"));
  ```

**Research flags (resolved below in this document — see `## Research Flags Resolved`):**

- **R-01 (D-06):** Confirm the writer maps a nested `Struct` to a ClickHouse `Tuple` column on `latest` via the RowBinary path. If not, TOF-06 needs a flattened fallback.
- **R-02 (D-07):** Confirm a `String`-valued record (null schema) inserts via `JSONEachRow`, not RowBinary. If RowBinary, CONV-08's `JSON` column collides with the Phase 3 TYPE-06 gap.

### Claude's Discretion

- Name and package of the new ClickHouse-backed sink test class (it must extend `ClickHouseBase` to inherit `@Tag("clickhouse")`, the per-class database and the MBean sweep).
- Name of the shared unit-test helper class (`TransformTestHelpers` is a placeholder) and the exact set of builders it exposes.
- Whether each sink-level test gets its own table or some share one — the four column shapes in D-06 / D-07 / D-08 differ, so separate tables is the likely answer.
- Exact `@DisplayName` phrasing beyond the `REQ-ID: ` prefix.
- Which specific cases become `@ParameterizedTest` within the matrices named in D-11.
- How many records each sink test inserts, and the assertion helper used to read them back (`ClickHouseTestHelpers.countRows` / `validateRows` or a direct query).

### Deferred Ideas (OUT OF SCOPE)

- `HeadersToValue.close()` nulls `headersSchema` (line 194) as well as `valueSchema`, so a `close()`-then-`apply()` sequence would NPE or build a field with a null schema. Not a legal Connect lifecycle, so no test and no fix this phase.
- Removing the unreachable `DataException` branch in `ConvertToMapOrJson.configure()` (lines 100-104) — behavior-neutral cleanup, declined under D-13.
- A fifth sink test covering HDRS-09 on the schemaless path — declined under D-08.
- Case-insensitive `output.json.library` — declined under D-13 as a config-semantics change.
- `HeaderToValueConfig` dead-code question — tracked as v2 requirement CLEAN-01, untouched here.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| TOF-01 | Truncate (`t`) events for a listed table are dropped while other operations for that table pass | Source read: `TableOperationFilter.java:75` accepts `t` at config-parse; `apply()` filters purely on `op`+`source.db`/`source.table`, no operation-specific branching after config parse |
| TOF-02 | Multiple tables in `tables`, only listed tables filtered | `configure()` splits on `,` per-token (`TableOperationFilter.java:63-68`); `apply()` checks `targetTables.contains(fullTableName)` (line 123) — no per-table special-casing |
| TOF-03 | Invalid/empty/whitespace `skipped.operations` tokens (incl. `r`) ignored without failing config; `op=r` always passes | `configure()` line 75: `if (op.length() == 1 && "cudt".contains(op))` else `LOGGER.warn` — `r` structurally cannot enter `skippedOperations` |
| TOF-04 | Schemaless (Map-valued) envelopes pass through unfiltered | `apply()` line 97-99: `if (!(record.value() instanceof Struct)) return record;` |
| TOF-05 | `config()` returns the definition; `close()` safe to call repeatedly | `config()` line 164-166 returns `CONFIG_DEF`; `close()` line 169-171 is stateless (no fields nulled) |
| TOF-06 | Sink-level: filtered Debezium `op` never reaches ClickHouse, unfiltered does | D-06 table shape verified against R-01 (Tuple/RowBinary — resolved below) |
| HDRS-01 | Tombstone passes through unchanged | `apply()` line 63-65: `if (record.value() == null) return record;` |
| HDRS-02 | No headers → empty map (schema + schemaless) | `extractHeaders()` line 144-151 always returns a `Map`, even empty |
| HDRS-03 | Null header value → null map entry | Line 149: `value == null ? null : value.toString()` |
| HDRS-04 | Missing/blank `field` rejected with `DataException` at `configure()` | **Corrected per D-14**: absent → `ConfigException` (line 46, `NO_DEFAULT_VALUE`); blank → `DataException` (line 50) — REQUIREMENTS.md wording needs updating |
| HDRS-05 | Field-name collision pinned: schema path throws `DataException`, schemaless overwrites | `buildSchema()` line 178 only adds field `if (oldSchema.field(fieldName) == null)`; value loop still `newValue.put(name, extractHeaders(record))` unconditionally — type mismatch throws via `Struct.put()`'s own validation |
| HDRS-06 | Output preserves topic/partition/key/timestamp/headers | Both `applySchemaless`/`applyWithSchema` use the 7-arg `newRecord(...)` (no headers arg) — Connect's default overload retains original headers |
| HDRS-07 | Duplicate header keys last-wins; non-string values via `toString()` (pinned) | `extractHeaders()` iterates `record.headers()` in insertion order, `Map.put` per key — last insertion wins; line 149 calls `.toString()` unconditionally |
| HDRS-08 | `close()` resets cached schema; `config()` returns definition | `close()` line 191-195 nulls `valueSchema`/`headersSchema`; `configure()` line 112 lazily rebuilds |
| HDRS-09 | Sink-level: headers land in `Map(String, Nullable(String))` | `configure()` line 53-55 builds `SchemaBuilder.map(STRING, OPTIONAL_STRING).optional()` — matches ClickHouse `Map(String, Nullable(String))` column type exactly |
| HDR-01 | Mismatched `fields`/`headers` counts rejected with `DataException` (both directions) | `configure()` line 66-69 explicit check; `fields`/`headers` are `NO_DEFAULT_VALUE` (lines 49, 55) so absent → `ConfigException` first |
| HDR-02 | Multi-valued header copies last value | Uses `record.headers().lastWithName(...)` (lines 114, 154) — Kafka Connect API's own last-wins semantics |
| HDR-03 | Missing/null header leaves target field untouched | Guard `header != null && header.value() != null` (lines 115, 155) — field keeps prior value or stays absent if new |
| HDR-04 | Configured field absent from schema added as optional string | `buildSchema()` lines 208-214: unmatched `fields` added as `Schema.OPTIONAL_STRING_SCHEMA` |
| HDR-05 | Output preserves topic/partition/key/timestamp/headers | Same 7-arg `newRecord` pattern as HeadersToValue |
| HDR-06 | Non-string values via `toString()` (pinned); `config()`/`close()` contract | Line 116/156 `.toString()`; `close()` line 226-228 nulls `valueSchema`/`fieldToHeaderIdx` |
| HDR-07 | Sink-level: header-derived field lands in its column | `buildSchema()` produces `Schema.OPTIONAL_STRING_SCHEMA` field → maps to a ClickHouse `String`/`Nullable(String)` column via the RowBinary schema path |
| CONV-01 | `preserve.null.values=false` omits nulls (map + Jackson + Gson) | `structToMap()` line 218-223 skips `result.put` when `!preserveNullValues`; Jackson via `NON_NULL` inclusion (line 145), Gson via *not* calling `serializeNulls()` (line 152-153) — two different mechanisms, same contract |
| CONV-02 | Nested Structs in Array/Map fields converted (fixed if broken) | **Confirmed gap**: `convertFieldValue()` line 238-254 only recurses on `Struct`, not `List`/`Map` — D-01 fix required |
| CONV-03 | Non-Struct values pass through unchanged | `apply()` line 175-186: `else { return record; }` fallthrough for e.g. bare `Long` or `String` w/ JSON disabled |
| CONV-04 | Output preserves key, key schema, timestamp, headers | `apply()` line 187-196 uses the 8-arg `newRecord(..., timestamp, headers)` overload explicitly — the only one of the 4 SMTs to pass headers explicitly |
| CONV-05 | Invalid `output.json.library` rejected with `ConfigException` | `ConfigDef.ValidString.in("jackson","gson")` (line 67) validates inside `new AbstractConfig(...)` (line 92) — see D-13 |
| CONV-06 | Direct + sliced `ByteBuffer` match array-backed base64 | `encodeByteBuffer()` line 262-275 branches on `hasArray() && arrayOffset()==0 && limit()==array().length`; a `ByteBuffer.allocateDirect(...)` or `.slice()` exercises the `duplicate()`+copy fallback |
| CONV-07 | `config()`/`close()` contract | `config()` line 289-291; `close()` line 294-297 clears the `ThreadLocal` encoder (no observable behavior diff) |
| CONV-08 | Sink-level: JSON-string output lands in String/JSON column with correct content | `Record.getConvertor()` routes `String`-valued data to `StringRecordConvertor` → `SchemaType.STRING_SCHEMA` → `doInsertString`/`doInsertStringV2` → `ClickHouseFormat.JSONEachRow` — confirms D-07/R-02 (resolved below) |
</phase_requirements>

## Summary

This phase adds unit tests (no Docker) and 4 ClickHouse-backed sink-level tests (Docker required) for four already-implemented SMTs. There is no new production capability to design — CONTEXT.md's 16 decisions (D-01 through D-16) already specify the exact fixes, table shapes, test organization, and error-contract semantics. All of those decisions were re-verified against the actual source in this research pass; every line-number citation in CONTEXT.md checked out exactly against the current `transforms/` source.

The two open research flags from CONTEXT.md (R-01: does a nested `Struct` map to ClickHouse `Tuple` via RowBinary; R-02: does a `String`-valued record insert via `JSONEachRow` not RowBinary) are **both resolved positively** by reading `ClickHouseWriter.java` and `Record.java` directly (see `## Research Flags Resolved`). Neither needs a fallback — the table shapes in D-06 and D-07 can be planned as written.

The remaining research value here is: (1) confirming the exact existing-test baseline so plans can state "grow this file, don't replace it" with line-accurate before/after counts, (2) the shared record-conversion/insert-dispatch pipeline a sink-level test must go through, and (3) the sink-level test scaffolding pattern (`ClickHouseBase`, `ClickHouseTestHelpers`, and the closest existing precedent, `JsonColumnTest.java`) so 02-03 doesn't reinvent it.

**Primary recommendation:** Follow CONTEXT.md's decisions as written — this research found no contradictions, no missing information, and both research flags resolve in favor of the design already specified. The planner should treat D-01 through D-16 as locked implementation instructions, not as options to re-derive.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| SMT unit behavior (config validation, field mutation, error contracts) | Transform (SMT) | — | Pure `Transformation<R>` implementations, no I/O; tested in-process via `apply()`/`configure()` calls, no Connect runtime needed |
| Record → insert-format dispatch (`STRING_SCHEMA`/`SCHEMA`/`SCHEMA_LESS`) | Sink / Connector internals | — | `Record.getConvertor()` (data tier) picks the convertor based on `(schema, value)` shape; downstream `ClickHouseWriter.doInsert()` dispatches by the resulting `SchemaType` — this is what makes CONV-08's `output.json.enabled=true` land via JSONEachRow, not RowBinary |
| Sink-level SMT verification (records actually landing in ClickHouse) | Database / Storage (via Sink tier) | Transform (SMT) | New test class extends `ClickHouseBase`, drives a real `ClickHouseSinkTask` against a Testcontainers ClickHouse `latest`; the SMT itself stays a pure transform, only the *test* crosses into the sink tier |
| Struct → Tuple / Map / JSON column serialization | Database / Storage (`ClickHouseWriter`) | — | Pre-existing serialization logic (`doWriteColValue` in `ClickHouseWriter.java`); this phase only writes tests against it, does not modify it (no requirement here touches `ClickHouseWriter`) |

## Existing Test Baseline (verified this session)

All four counts confirmed by direct `Read` of the current files (not from CONTEXT.md's stated counts, though they match):

| File | Current tests | Current lines | Notes |
|------|---------------|----------------|-------|
| `src/test/java/transforms/TableOperationFilterTest.java` | 9 `@Test` methods | 178 (177 content + trailing) | No `t`/truncate case, no invalid-config case, no `close()`/`config()` test — matches CONTEXT.md D-09 |
| `src/test/java/transforms/HeadersToValueTest.java` | 2 `@Test` methods | 85 | Only happy-path schema/schemaless; has a `generateSampleRecord` helper (D-12's `TransformTestHelpers` should generalize this shape) |
| `src/test/java/transforms/HeaderToValueTest.java` | 2 `@Test` methods | 75 | Same shape as `HeadersToValueTest`, independent `generateSampleRecord` copy — a second confirmation that a shared D-12 helper removes real duplication |
| `src/test/java/transforms/ConvertToMapOrJsonTest.java` | 13 `@Test` methods | 323 | Strongest existing coverage — base64/JSON-library/date-serialization paths already tested; zero tests for CONV-02 (nested Array/Map), CONV-05 (`ConfigException`), CONV-06 (sliced `ByteBuffer`) |

None of the four files currently import `@DisplayName`, `@ParameterizedTest`, or `assertThrows` — D-10/D-11/D-16 are genuinely new conventions being introduced to this file group, not extensions of an existing pattern. Confirm `@ParameterizedTest`/`@ValueSource` need no new dependency: `build.gradle.kts:125` already declares `testImplementation("org.junit.jupiter:junit-jupiter")`, the aggregate artifact that transitively includes `junit-jupiter-params` `[VERIFIED: build.gradle.kts:125]`.

## Research Flags Resolved

### R-01: Struct → ClickHouse `Tuple` column via RowBinary — CONFIRMED SUPPORTED

`ClickHouseWriter.doWriteColValue()` has an explicit `case TUPLE:` branch (`ClickHouseWriter.java:474-496`) that handles a `Struct`-typed field value directly:

```java
// ClickHouseWriter.java:474-496 [VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:474-496]
case TUPLE:
    Map<?, ?> jsonMapValues;
    Object underlyingObject = value.getObject();
    if (underlyingObject.getClass() != Struct.class) {
        // Tuples in the root structure are parsed using StructToJsonMap
        jsonMapValues = (Map<?, ?>) underlyingObject;
    } else {
        jsonMapValues = StructToJsonMap.toJsonMap((Struct) underlyingObject);
    }
    col.getTupleFields().forEach(column -> {
        String[] colNameSplit = column.getName().split("\\.");
        String fieldName = colNameSplit.length > 0 ? colNameSplit[colNameSplit.length - 1] : column.getName();
        Data innerData = (Data) jsonMapValues.get(fieldName);
        try {
            doWriteCol(innerData, jsonMapValues.containsKey(fieldName), column, stream, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });
    break;
```

And `Column.java:210-212` maps a `Tuple(...)` type string from `DESCRIBE` output to `Type.TUPLE`: `[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java:210-212]` (`valueType.startsWith("Tuple")` → `builder.type(Type.TUPLE)`).

**Conclusion: D-06's table shape (`source Tuple(db String, table String)`) will work as written against the RowBinary path — no fallback needed.** The nested Debezium `source` Struct converts via `StructToJsonMap.toJsonMap()` and each named tuple field (`db`, `table`) is written individually. Plan TOF-06 exactly as CONTEXT.md D-06 specifies.

### R-02: `String`-valued record (null schema) → `JSONEachRow`, not RowBinary — CONFIRMED

Two-step trace, both read directly this session:

1. **Convertor selection** (`Record.java:54-67`) `[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/data/Record.java:54-67]`:
   ```java
   private static RecordConvertor getConvertor(Schema schema, Object data) {
       if (data == null ) { return emptyRecordConvertor; }
       if (schema != null && data instanceof Struct) { return schemaRecordConvertor; }
       if (data instanceof Map) { return schemalessRecordConvertor; }
       if (data instanceof String) { return stringRecordConvertor; }   // <-- CONV-08's output
       throw new DataException(...);
   }
   ```
   `ConvertToMapOrJson.apply()` with `output.json.enabled=true` produces a `String` value with a `null` schema (`ConvertToMapOrJson.java:187-196`, `newRecord(..., null /* schema */, transformedValue, ...)`) — this matches the `data instanceof String` branch regardless of the null schema, so it always routes to `StringRecordConvertor`.

2. **`StringRecordConvertor.doConvert()`** (`StringRecordConvertor.java:11-19`) tags the record `SchemaType.STRING_SCHEMA` `[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/data/convert/StringRecordConvertor.java:18]`.

3. **`ClickHouseWriter.doInsert()` dispatch** (`ClickHouseWriter.java:216-220`) `[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:216-220]`:
   ```java
   case SCHEMA_LESS:
       doInsertJson(records, table, queryId);
       ...
   case STRING_SCHEMA:
       doInsertString(records, table, queryId);
   ```
   `doInsertString` → `doInsertStringV2` (client V2 path) sets `clickHouseFormat = ClickHouseFormat.JSONEachRow` (`ClickHouseWriter.java:1134`) `[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1134]`.

**Conclusion: CONV-08's `String`-valued output always goes through `doInsertStringV2` → `JSONEachRow`, never RowBinary.** D-07's table shape (a `JSON` column for the nested field) is safe — it does not collide with the Phase 3 TYPE-06 RowBinary/`JSON` gap, because that gap only applies to the *schema/Struct* RowBinary path, and CONV-08's SMT output is never schema/Struct. Plan CONV-08 exactly as CONTEXT.md D-07 specifies.

## Standard Stack

No new libraries are needed for this phase — it exercises the existing test stack.

### Core (already present, confirmed this session)
| Library | Version | Purpose | Confirmed |
|---------|---------|---------|-----------|
| JUnit Jupiter (aggregate) | via `org.junit.jupiter:junit-jupiter` | Test framework, brings `junit-jupiter-params` transitively | `[VERIFIED: build.gradle.kts:125]` |
| JUnit Jupiter API | 5.11.3 | Explicit API dependency (separate from the aggregate) | `[VERIFIED: build.gradle.kts:107]` |
| Testcontainers ClickHouse module | via `ClickHouseBase` | Sink-level container lifecycle, shared per test JVM fork | `[VERIFIED: src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java:29]` |
| `org.apache.kafka:connect-api` | 4.1.1 (project-wide) | `Transformation<R>`, `SinkRecord`, `ConnectHeaders` — no `connect-runtime` on classpath | `[VERIFIED: CONTEXT.md D-05, cross-checked against source imports]` |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Local `transform()` helper in the sink test class (D-05) | `org.apache.kafka.connect.runtime.TransformationChain` | Rejected — lives in `connect-runtime`, a second new build dependency not on the classpath today; against the Tooling constraint |

**Installation:** None required. This phase adds zero new Gradle dependencies.

## Package Legitimacy Audit

**Not applicable.** This phase installs no new external packages. All test tooling (JUnit Jupiter aggregate, Testcontainers ClickHouse module) is already declared in `build.gradle.kts` and was verified present in the previous phase (Phase 1). No `npm view`/`pip index`/`cargo search` style check is relevant to a Java/Gradle-only, zero-new-dependency phase.

## Architecture Patterns

### System Architecture Diagram

```
                    ┌─────────────────────────────────────────┐
                    │   Unit test (no Docker) — testUnit task  │
                    │                                            │
  SinkRecord  ──►   │  TableOperationFilter.apply()              │
  (built in-test)   │  HeadersToValue.apply()      ──► asserts   │
                    │  HeaderToValue.apply()            on the   │
                    │  ConvertToMapOrJson.apply()       returned │
                    │                                    record  │
                    └─────────────────────────────────────────┘

                    ┌───────────────────────────────────────────────────────────┐
                    │   Sink-level test (Docker, @Tag("clickhouse")) — 02-03     │
                    │                                                             │
  SinkRecord(s) ──► │  transform(xform, records)   [D-05 helper, drops nulls]    │
  (Struct-valued,   │        │                                                    │
   Debezium/plain)  │        ▼                                                    │
                    │  ClickHouseSinkTask.start(props) / .put(records) / .stop() │
                    │        │                                                    │
                    │        ▼                                                    │
                    │  Record.getConvertor()  ──►  SchemaType                     │
                    │   (Struct+schema → SCHEMA; Map → SCHEMA_LESS;               │
                    │    String → STRING_SCHEMA)                                  │
                    │        │                                                    │
                    │        ▼                                                    │
                    │  ClickHouseWriter.doInsert()  ──►  dispatch by SchemaType   │
                    │   ├─ SCHEMA        → doInsertRawBinary (RowBinary)          │
                    │   ├─ SCHEMA_LESS   → doInsertJson (JSONEachRow)             │
                    │   └─ STRING_SCHEMA → doInsertString (JSONEachRow)           │
                    │        │                                                    │
                    │        ▼                                                    │
                    │  ClickHouse `latest` container (Testcontainers)             │
                    │        │                                                    │
                    │        ▼                                                    │
                    │  Assertion: ClickHouseTestHelpers.countRows/validateRows /  │
                    │             direct SELECT via chc.getClient()               │
                    └───────────────────────────────────────────────────────────┘
```

A reader can trace TOF-06 end to end: build Debezium-shaped `SinkRecord`s → `transform(filter, records)` drops filtered ones → `ClickHouseSinkTask.put()` → `Record.getConvertor()` picks `SchemaRecordConvertor` (Struct+schema) → `ClickHouseWriter.doInsertRawBinary` → `Tuple` column write (R-01) → `countRows` proves filtered rows never landed.

### Recommended Project Structure
```
src/test/java/transforms/
├── TableOperationFilterTest.java     # grows: +truncate, +multi-table, +invalid-token matrix, +config()/close()
├── HeadersToValueTest.java            # grows: +tombstone, +empty-headers, +null-header, +config errors, +collision, +metadata, +dup-headers, +non-string, +close()
├── HeaderToValueTest.java             # grows: +count-mismatch both directions, +multi-value, +missing/null header, +new-field, +metadata, +non-string, +config()/close()
├── ConvertToMapOrJsonTest.java        # grows: +preserve-null(jackson+gson), +nested Array/Map (CONV-02 fix), +non-Struct passthrough, +ConfigException, +sliced ByteBuffer, +metadata (key/keySchema/timestamp/headers), +config()/close()
└── TransformTestHelpers.java          # NEW (D-12): shared record builders + assertMetadataPreserved()

src/test/java/com/clickhouse/kafka/connect/sink/
└── <NewSinkSmtTest>.java              # NEW (02-03, D-05..D-08): extends ClickHouseBase, @Tag("clickhouse") inherited,
                                        # covers TOF-06 / HDRS-09 / HDR-07 / CONV-08, one table per SMT shape
```

### Pattern 1: `try (Transform<SinkRecord> x = new Transform<>())` for `close()` coverage
**What:** All four SMTs implement `Transformation<R>` which extends `Closeable` — the existing tests already use try-with-resources for `HeadersToValue`/`HeaderToValue` but plain construction for `TableOperationFilter`/`ConvertToMapOrJson`.
**When to use:** Every new test method that constructs a transform, so `close()` is exercised implicitly in addition to any dedicated `TOF-05`/`HDRS-08`/`HDR-06`/`CONV-07` test.
**Example:**
```java
// Existing pattern, confirmed in HeadersToValueTest.java:21 and HeaderToValueTest.java:21
try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
    Map<String, Object> configs = new HashMap<>();
    configs.put("field", "headers_map");
    headersToValue.configure(configs);
    // ... apply + assert
}
```

### Pattern 2: `@Tag("clickhouse")` inheritance gates unit-vs-sink test placement
**What:** `ClickHouseBase` is annotated `@Tag("clickhouse")` at the class level (`ClickHouseBase.java:32`); any subclass inherits the tag automatically — the `testUnit` Gradle task excludes anything carrying it.
**When to use:** The new 02-03 sink test class MUST extend `ClickHouseBase` and must NOT be placed under `src/test/java/transforms/` (that package's tests must stay Docker-free per D-09/D-12).
**Example:**
```java
// build.gradle.kts:226 registers testUnit to run everything NOT tagged "clickhouse"
@Tag("clickhouse")  // inherited automatically from ClickHouseBase — do not re-declare
public class ClickHouseBase { ... }
```

### Pattern 3: sink-level test lifecycle (`JsonColumnTest.java` precedent)
**What:** `createProps()` → `createClient(props)` → `dropTable`/`createTable` → build `SinkRecord`s → apply SMT(s) → `new ClickHouseSinkTask()` → `start(props)` → `put(records)` → `stop()` → assert.
**When to use:** The 02-03 sink test class should follow this shape per-test-method, with D-05's `transform()` helper inserted between "build records" and `start()`.
**Example:**
```java
// Source: src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java:52-78 (adapted)
private void runSinkTest(Map<String, String> props, Transformation<SinkRecord> xform, String tableName) {
    ClickHouseHelperClient chc = createClient(props);
    ClickHouseTestHelpers.dropTable(chc, tableName);
    ClickHouseTestHelpers.createTable(chc, tableName, "CREATE TABLE %s (...) ENGINE = MergeTree ORDER BY ...");

    List<SinkRecord> sourceRecords = buildDebeziumRecords(tableName);
    Collection<SinkRecord> transformed = transform(xform, sourceRecords); // D-05 helper — drops nulls

    ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
    try {
        sinkTask.start(props);
        sinkTask.put(transformed);
    } finally {
        sinkTask.stop(); // Pitfall 5: MBean leak if not in finally — see below
    }

    assertEquals(expectedRowCount, ClickHouseTestHelpers.countRows(chc, tableName));
}
```
**Critical deviation from `JsonColumnTest.java`'s existing pattern:** that file calls `start()/put()/stop()` **without** try/finally (lines 70-73) — this is a known, documented pitfall (Pitfall 5 below), not a pattern to copy. New tests must wrap in try/finally.

### Anti-Patterns to Avoid
- **Copying `JsonColumnTest`'s bare `start()/put()/stop()` without try/finally:** leaks an MBean into the JVM's platform MBeanServer on any assertion failure between `start()` and `stop()`, causing `InstanceAlreadyExistsException` in a later, unrelated test (Pitfall 5, `.planning/research/PITFALLS.md`). `ClickHouseBase.cleanupLeakedMBeans()` (`@AfterEach`, `ClickHouseBase.java:85-97`) is a safety net, not a substitute for correct test code — Phase 1 D-09 already established try/finally + `uniqueConnectorName()` as the required pattern; this phase's new sink test must follow it, not the older file's shape.
- **Asserting on `LOGGER.warn` text for TOF-03:** couples the test to log wording rather than behavior (D-15, explicit rejection).
- **Testing `HeaderToValueConfig` in isolation:** confirmed likely-dead-code by `.planning/research/FEATURES.md` section 3 — out of this phase's scope (CLEAN-01, v2).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|--------------|-----|
| Applying a chain of Kafka Connect transforms to records before insert | A custom SMT-chain runtime, or pulling in `connect-runtime`'s `TransformationChain` | D-05's local `transform()` helper (apply-and-drop-nulls loop) | `TransformationChain` requires `connect-runtime` on the classpath — a second new build dependency this milestone explicitly avoids; the helper is ~10 lines and reversible |
| Reading back ClickHouse row content in an assertion | Hand-rolled JDBC/HTTP response parsing | `ClickHouseTestHelpers.countRows`/`validateRows`/`getAllRowsAsJson`, or `chc.getClient().query(...)` directly (as `ClickHouseBase.countRows` does) | Existing helpers already handle UTF-8 decoding (Phase 1 D-11 fix), sequential-consistency settings, and JSON parsing — re-implementing risks reintroducing the `windows-1252` charset bug (Pitfall 7) |
| Building Debezium-envelope `SinkRecord`s for each test | Ad hoc `Struct`/`Schema` construction per test method | `TransformTestHelpers` (D-12) shared builders | HDRS-06/HDR-05/CONV-04 all assert the identical metadata-preservation contract — one helper avoids three near-duplicate assertion blocks and three near-duplicate record builders |

**Key insight:** This phase's "don't hand-roll" list is short because almost everything needed already exists in the repo (`ClickHouseBase`, `ClickHouseTestHelpers`, the try-with-resources SMT pattern). The one real avoidance is `TransformationChain` — resist the urge to add `connect-runtime` just to get a "more realistic" transform-chaining mechanism; the constraint here is explicit and the manual loop is behaviorally identical for this phase's purposes (single-transform-per-test, not multi-SMT chains).

## Common Pitfalls

### Pitfall 1: MBean leak on `start()`/`put()`/`stop()` without try/finally
**What goes wrong:** `MBeanServerUtils.registerMBean` throws `RuntimeException` on `InstanceAlreadyExistsException`; if a prior test in the same JVM fork leaked its MBean (by not calling `stop()` after a failure), the next test with a colliding connector name crashes on `start()` — a suite-order-dependent flake, not visible when running a single test in isolation.
**Why it happens:** The existing precedent (`JsonColumnTest.runJsonTest`, lines 70-73) calls `start(); put(); stop();` sequentially with no try/finally.
**How to avoid:** Wrap every `start()/put()/stop()` sequence in the new 02-03 sink test class in try/finally, and use `uniqueConnectorName(TestInfo)` (already available on `ClickHouseBase`, opt-in per Phase 1 D-10) for each test method's connector name.
**Warning signs:** A new sink test passes alone (`--tests "...NewSinkSmtTest"`) but fails only when the full suite runs, with `InstanceAlreadyExistsException`.

### Pitfall 2: `windows-1252` test-JVM charset affects non-ASCII header/JSON content
**What goes wrong:** `build.gradle.kts` sets `file.encoding=windows-1252` for all tests (`.planning/research/PITFALLS.md` Pitfall 7); stream readers without an explicit `StandardCharsets.UTF_8` risk mangled strings. `ClickHouseTestHelpers.getAllRowsAsJson` already constructs its `InputStreamReader` with explicit UTF-8 (`ClickHouseTestHelpers.java:159-160` — confirmed), so using that helper (rather than a new ad hoc reader) sidesteps this.
**Why it happens:** JVM-wide `file.encoding` silently becomes the default for any stream API that doesn't specify a charset.
**How to avoid:** For sink-level tests asserting header values or JSON content, use the existing `ClickHouseTestHelpers` helpers (already UTF-8-safe) rather than writing a new response reader. If any test data includes non-ASCII (not required by this phase's requirement IDs, but a good practice given the header/JSON content being tested), verify it round-trips under `./gradlew test`'s actual settings, not an IDE run.
**Warning signs:** A string-content assertion passes when run from an IDE but fails under `./gradlew test`.

### Pitfall 3: TOF-06's Debezium `op=r` and schemaless envelopes are pinned pass-through, not filterable — don't accidentally test them as filterable
**What goes wrong:** Because `skipped.operations` can structurally never contain `r` (config parser only accepts `c`/`u`/`d`/`t`, `TableOperationFilter.java:75`), and `apply()` returns early for non-`Struct` values (line 97-99), a sink-level test that expects an `op=r` row or a schemaless envelope to be filtered will fail — but that failure would be *correct current behavior*, not a bug. TOF-06's assertion must only exercise `op` values inside `"cudt"`.
**Why it happens:** TOF-03/TOF-04 already pin this at the unit level; a sink-level test author could forget the same constraint applies when picking Debezium-shaped test data.
**How to avoid:** Reuse the same `op` values already proven filterable by the TOF-03/TOF-01 unit tests (`c`, `u`, `d`, `t`) when building TOF-06's sink-level fixture.

### Pitfall 4: `ConvertToMapOrJson`'s nested-Struct bug (CONV-02) must be fixed, not merely observed, before CONV-08 can rely on correct nested output
**What goes wrong:** If 02-02 (which owns CONV-02) is implemented after or independently of 02-03 (which owns CONV-08), a sink-level test asserting a nested struct field lands correctly in a `JSON` column could pass against *buggy* pre-fix output if the nested field isn't inside an Array/Map (CONV-08's D-07 example nests a struct directly under a top-level key, not inside a List/Map, so this specific interaction risk is LOW for CONV-08 as scoped — verify the CONTEXT.md D-07 table only requires a single nested struct field, not nested-in-array).
**Why it happens:** 02-01/02-02/02-03 are all wave-1 (parallel) per the roadmap; CONV-02's fix lands in 02-02 while CONV-08 lands in 02-03.
**How to avoid:** No fix is needed for CONV-08 to be correct as scoped (D-07's example table has `nested JSON` for a directly-nested struct, which already converts correctly before the D-01 fix — `convertFieldValue()`'s existing `if (value instanceof Struct nestedStruct) return structToMap(nestedStruct);` branch, line 239-241, already handles the *directly* nested case). The bug only affects Structs nested inside `List`/`Map` fields, which CONV-08's D-07 table does not exercise. No cross-plan ordering dependency exists for this specific requirement.

## Code Examples

### Verified metadata-preservation assertion pattern (D-12, generalized from existing tests)
```java
// Adapted from HeadersToValueTest.java:72-83 / HeaderToValueTest.java:63-74 (both read this session)
// Source: src/test/java/transforms/{HeadersToValueTest,HeaderToValueTest}.java
private static SinkRecord generateSampleRecord(String topic, int partition, long offset, ConnectHeaders headers) {
    Schema schema = SchemaBuilder.struct()
            .field("off16", Schema.INT16_SCHEMA)
            .field("timestamp_int64", Schema.INT64_SCHEMA)
            .field("date_date", Schema.INT32_SCHEMA)
            .build();
    Struct valueStruct = new Struct(schema)
            .put("off16", (short) new Random().nextInt(Short.MAX_VALUE + 1))
            .put("timestamp_int64", new Date().getTime())
            .put("date_date", (int) (new Date().getTime() / 1000));
    return new SinkRecord(topic, partition, null, null, schema, valueStruct, offset, 0L,
            TimestampType.NO_TIMESTAMP_TYPE, headers);
}
```

### Verified config-error assertion pattern (D-16)
```java
// Pattern derived from all 4 SMTs' apply()-level exception wrapping, confirmed this session:
// TableOperationFilter.java:130-133, HeadersToValue.java:70-74, HeaderToValue.java:93-97, ConvertToMapOrJson.java:197-200
DataException ex = assertThrows(DataException.class, () -> xform.apply(record));
assertEquals("Failed to transform record", ex.getMessage()); // or "Failed to process record in TableOperationFilter" for TOF
assertInstanceOf(DataException.class, ex.getCause());
assertTrue(ex.getCause().getMessage().contains("<expected substring>"));
```

### Verified ConfigException-vs-DataException split (D-14, confirmed via `AbstractConfig` construction order)
```java
// HeadersToValue: absent "field" key -> ConfigException (thrown inside new AbstractConfig(CONFIG_DEF, configs), before line 46 is reached)
Map<String, Object> configs = new HashMap<>(); // no "field" key at all
HeadersToValue<SinkRecord> x = new HeadersToValue<>();
assertThrows(ConfigException.class, () -> x.configure(configs));

// HeadersToValue: blank "field" value -> DataException (HeadersToValue.java:49-51, reached only after AbstractConfig succeeds)
Map<String, Object> configsBlank = new HashMap<>();
configsBlank.put("field", "   ");
HeadersToValue<SinkRecord> x2 = new HeadersToValue<>();
assertThrows(DataException.class, () -> x2.configure(configsBlank));
```

## State of the Art

Not applicable in the conventional sense — no external ecosystem shifted here; this is a same-repo test-writing task against stable, already-implemented SMTs. The relevant "state of the art" comparison is upstream Apache Kafka Connect's own SMT test conventions, already captured in `.planning/research/FEATURES.md`'s "Reference Test Suites Consulted" table:

| Old Approach (fork today) | Target (this phase) | Convention Source |
|---|---|---|
| Only happy-path schema/schemaless tests per SMT | Every table-stakes edge case gets both variants where the code path differs | Apache Kafka's `HeaderFromTest` schemaless()/withSchema() pattern |
| Zero `ConfigDef`/`ConfigException`/`DataException` assertions across all 4 SMTs | One dedicated, clearly-named test method per existing `configure()`-time guard | Apache Kafka's `InsertHeaderTest.configRejects*`/`HeaderFromTest.invalidConfig*` naming |
| No `newRecord`-preservation assertion on any SMT | One shared assertion (D-12) reused across HDRS-06/HDR-05/CONV-04 | Implicit in upstream's before/after struct assertions |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| — | *(none)* | — | — |

No claims in this research are tagged `[ASSUMED]`. Every factual claim about source behavior, line numbers, and the two research flags was verified this session by `Read`-ing the actual source files (`TableOperationFilter.java`, `HeadersToValue.java`, `HeaderToValue.java`, `ConvertToMapOrJson.java`, `ClickHouseWriter.java`, `Record.java`, `StringRecordConvertor.java`, `Type.java`, `Column.java`, `ClickHouseBase.java`, `ClickHouseTestHelpers.java`, `JsonColumnTest.java`, `build.gradle.kts`, and the four existing test files). No new external packages or unverifiable third-party claims are involved in this phase.

## Open Questions

None blocking. Both research flags from CONTEXT.md (R-01, R-02) are resolved positively in `## Research Flags Resolved` above — the planner does not need to choose a fallback table shape for TOF-06 or CONV-08.

One minor item for the planner's awareness, not a gap: `.planning/REQUIREMENTS.md`'s HDRS-04 wording currently says both absent and blank `field` throw `DataException` — CONTEXT.md D-14 already flags this as incorrect and instructs correcting it during this phase. The plan for 02-01/02-02 (whichever plan owns HDRS-04) should include a small documentation-fix task alongside the test, per D-14.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 17 (Gradle toolchain) | Compiling/running all tests | Established in Phase 1 | 17 | — |
| Gradle 7.4.2 | Build orchestration | Established in Phase 1 | 7.4.2 | — |
| Docker (via colima, profile `vz`) | 02-03 sink-level tests only (`@Tag("clickhouse")`) | Established in Phase 1 — `TESTCONTAINERS_HOST_OVERRIDE` must be unset (`env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`), per `.planning/STATE.md` Blockers/Concerns | colima `vz` profile | None — 02-03 cannot run without it; 02-01/02-02 (`testUnit`) run without Docker |
| JaCoCo Gradle plugin | Coverage reporting (already attached to `test` task, not new to this phase) | Established in Phase 1 | 0.8.15 | — |

**Missing dependencies with no fallback:** None currently missing — Docker/colima availability was already established as a precondition in Phase 1; this phase inherits that state, it does not newly introduce the dependency.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter (5.11.3 API + aggregate) `[VERIFIED: build.gradle.kts:107,125]` |
| Config file | `build.gradle.kts` (`test`/`testUnit` task definitions, lines ~176-260) |
| Quick run command | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --tests "transforms.*"` (no Docker) |
| Full suite command | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` (includes 02-03's `@Tag("clickhouse")` sink test, needs Docker) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| TOF-01..05 | `TableOperationFilter` edge cases | unit | `./gradlew testUnit --tests "transforms.TableOperationFilterTest"` | ✅ file exists, grows |
| TOF-06 | Sink-level filter | clickhouse-backed | `./gradlew test --tests "...NewSinkSmtTest"` | ❌ new file, 02-03 |
| HDRS-01..08 | `HeadersToValue` edge cases | unit | `./gradlew testUnit --tests "transforms.HeadersToValueTest"` | ✅ file exists, grows |
| HDRS-09 | Sink-level headers map | clickhouse-backed | `./gradlew test --tests "...NewSinkSmtTest"` | ❌ new file, 02-03 |
| HDR-01..06 | `HeaderToValue` edge cases | unit | `./gradlew testUnit --tests "transforms.HeaderToValueTest"` | ✅ file exists, grows |
| HDR-07 | Sink-level header field | clickhouse-backed | `./gradlew test --tests "...NewSinkSmtTest"` | ❌ new file, 02-03 |
| CONV-01..07 | `ConvertToMapOrJson` edge cases | unit | `./gradlew testUnit --tests "transforms.ConvertToMapOrJsonTest"` | ✅ file exists, grows |
| CONV-08 | Sink-level JSON output | clickhouse-backed | `./gradlew test --tests "...NewSinkSmtTest"` | ❌ new file, 02-03 |

### Sampling Rate
- **Per task commit:** `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --tests "transforms.*"` (fast, no Docker) for 02-01/02-02; `./gradlew test --tests "...<NewSinkSmtTest>"` for 02-03
- **Per wave merge:** `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` (full suite, Docker required — all three plans are wave 1)
- **Phase gate:** Full suite green before `/gsd-verify-work`, per success criterion 5 (`./gradlew test` stays green)

### Wave 0 Gaps
None — existing test infrastructure (`ClickHouseBase`, `ClickHouseTestHelpers`, `testUnit`/`test` Gradle tasks, JUnit Jupiter aggregate with params support) covers all phase requirements. No new framework install or shared fixture is needed before 02-01/02-02/02-03 can start.

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-------------------|
| V2 Authentication | No | This phase touches no auth code |
| V3 Session Management | No | N/A |
| V4 Access Control | No | N/A |
| V5 Input Validation | Yes | SMT `ConfigDef` validation (`ConfigDef.ValidString.in(...)`, `NO_DEFAULT_VALUE` guards) — this phase's tests pin existing validation, introduce no new user-facing input surface |
| V6 Cryptography | No | N/A |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|----------------------|
| Malformed/adversarial header values reaching `toString()` in `HeadersToValue`/`HeaderToValue` | Tampering (low severity — internal CDC pipeline data, not external user input) | Existing `.toString()` behavior is pinned as-is (HDRS-07/HDR-06); no new mitigation introduced or required this phase — this is CDC pipeline data from Debezium, not untrusted external input |
| Test credentials/config in sink-level test failure output | Information Disclosure (low severity, test-only) | `ClickHouseBase.createProps()` already uses a fixed non-sensitive `test_password` for local Testcontainers runs (Pitfall from `.planning/research/PITFALLS.md` Security Mistakes table) — new 02-03 tests should not log full `props` maps on failure |

This phase does not introduce new attack surface — it is entirely test code exercising existing, already-shipped transform logic. No new security controls are required.

## Sources

### Primary (HIGH confidence — read directly this session)
- `src/main/java/com/clickhouse/kafka/connect/transforms/TableOperationFilter.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/transforms/HeadersToValue.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (dispatch logic, lines 192-220, 440-537, 700-1140)
- `src/main/java/com/clickhouse/kafka/connect/sink/data/Record.java` (convertor selection, lines 1-75)
- `src/main/java/com/clickhouse/kafka/connect/sink/data/convert/StringRecordConvertor.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/sink/data/convert/RecordConvertor.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Type.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` (grep for Tuple handling, lines 210-212)
- `src/test/java/transforms/{TableOperationFilterTest,HeadersToValueTest,HeaderToValueTest,ConvertToMapOrJsonTest}.java` (all four, full files)
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` (full file)
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java` (full file)
- `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` (lines 140-240)
- `build.gradle.kts` (grep for `junit-jupiter`, `testUnit`, `jacocoForkReport`)

### Secondary (MEDIUM confidence — prior-session research, cross-checked)
- `.planning/research/FEATURES.md` — per-feature test-case inventory (sections 1-4 cover this phase's four SMTs directly)
- `.planning/research/PITFALLS.md` — Pitfall 5 (MBean leak) and Pitfall 7 (`windows-1252` encoding) apply directly to this phase's sink-level tests
- `.planning/research/ARCHITECTURE.md` — referenced in CONTEXT.md for the JUnit-tags/`testUnit` pattern (not re-read this session; CONTEXT.md's summary was consistent with what `build.gradle.kts` and `ClickHouseBase.java` show directly)

### Tertiary
None — no WebSearch or external documentation lookups were needed for this phase; it is entirely grounded in the fork's own source, already-completed prior-phase research, and CONTEXT.md's locked decisions.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — zero new dependencies, all existing tooling verified present in `build.gradle.kts`
- Architecture: HIGH — every dispatch path (convertor selection, SchemaType routing, RowBinary/JSONEachRow split) read directly from source this session
- Pitfalls: HIGH — grounded in prior-session pitfalls research plus this session's direct confirmation of the `JsonColumnTest` try/finally gap

**Research date:** 2026-09-16
**Valid until:** Stable until the fork's `transforms/` or `sink/db` source changes — no external ecosystem dependency to go stale. Re-verify line numbers if any prior-wave plan modifies these files before 02-01/02-02/02-03 execute.
