# Phase 2: Fork SMT Coverage - Context

**Gathered:** 2026-09-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Every behavior of the four fork SMTs is pinned by an automated test:

- `TableOperationFilter` — TOF-01..06
- `HeadersToValue` — HDRS-01..09
- `HeaderToValue` — HDR-01..07
- `ConvertToMapOrJson` — CONV-01..08

That is 26 unit-level requirements (run by `./gradlew testUnit`, no Docker) plus 4 ClickHouse-backed
sink-level requirements (TOF-06, HDRS-09, HDR-07, CONV-08) proving transformed records land correctly
in ClickHouse `latest`. Bugs these tests expose are fixed in this phase with small, rebase-friendly
changes confined to `src/main/java/com/clickhouse/kafka/connect/transforms/`.

Out of this phase:

- `targetTableFilter`, `describeTable`, `JSON`/`Dynamic` type mapping — Phase 3
- Explicit `insertFormat=json`, `jsonInsertColumns`, MBean naming, `async_insert` — Phase 4
- CI workflow changes — Phase 5
- Upstream SMTs `KeyToValue`, `ExtractTopic` — not fork additions
- New SMT capabilities or new config semantics — out of scope for the milestone

</domain>

<decisions>
## Implementation Decisions

### Main-code fixes (bug-vs-pin policy)

Three fixes are pre-approved. All are small, confined to one method each, and inside `transforms/`.

- **D-01:** **CONV-02 — recurse into collections.** `ConvertToMapOrJson.convertFieldValue()`
  (`ConvertToMapOrJson.java:238`) only recurses when the value is itself a `Struct`. A `Struct` inside
  an Array field (`List<Struct>`) or a Map field (`Map<String,Struct>`) passes through raw into the
  output map, and then into Jackson/Gson, which reflect over `Struct`'s internals and emit garbage.
  Fix: walk `List`/`Collection` elements and `Map` values recursively, applying **both** the
  `Struct`→`Map` conversion and the `bytes`→base64 conversion at every depth.

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

  Rejected: converting nested Structs but leaving bytes alone (makes `bytes.to.base64` behave
  differently by nesting depth); pinning the broken output.

- **D-02:** **Stale `valueSchema` cache — add a source-schema guard.** Both `HeadersToValue`
  (`HeadersToValue.java:112`) and `HeaderToValue` (`HeaderToValue.java:141`) cache `valueSchema` from
  the first record and never invalidate it. When a Debezium table's schema evolves mid-stream, later
  records are rebuilt against the stale schema — new fields are silently dropped and
  `new Struct(staleSchema)` can fail validation. Fix: remember the schema the cache was built from and
  rebuild on change. `close()` nulls the new field too.

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

  This is not one of the 30 listed requirements, but it is a behavior of these SMTs and a real
  data-loss path for the CDC pipelines this fork serves. Needs a test feeding two different schemas
  through one configured transform instance.

- **D-03:** **`TableOperationFilter.configure()` clears its sets.** `targetTables` and
  `skippedOperations` (`TableOperationFilter.java:51-52`) are field initializers that `configure()`
  only ever adds to. Connect calls `configure()` once per instance so production never notices, but it
  traps any reconfigure-style test. Fix: `targetTables.clear()` and `skippedOperations.clear()` at the
  top of `configure()`. Zero production behavior change.

- **D-04:** **Escalation rule for anything else — Phase 1's D-02 rule applies unchanged.** Fix inline
  if it is small: a few lines, confined to one method, rebase-friendly. Anything larger — a new
  method, a signature change, or a fix reaching into main code outside `transforms/` — **stops for a
  user decision**. Never disable or `@Disabled` a test to get green.

### Sink-level test design (TOF-06, HDRS-09, HDR-07, CONV-08)

- **D-05:** **SMTs are applied by a shared private helper in the new sink test class**, mirroring what
  the Connect worker does: apply the configured transform per record, drop records the transform
  nulled out. Drop-on-null is the entire point of TOF-06, so it lives in one visible place.

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

  **Rejected: `org.apache.kafka.connect.runtime.TransformationChain`.** Only `connect-api` is on the
  classpath (`build.gradle.kts:86`); `TransformationChain` lives in `connect-runtime`. Adding it is a
  second new build dependency this milestone and pulls a large transitive tree into the test
  classpath — against the Tooling constraint (JaCoCo is the only expected new dependency).
  — **Reversibility:** reversible — a local test helper, no production surface.

- **D-06:** **TOF-06 keeps the real Debezium envelope shape.** `TableOperationFilter.apply()` returns
  early unless `record.value() instanceof Struct`, and TOF-04 pins schemaless envelopes as
  pass-through — so TOF-06 **must** use the schema/Struct path. The table carries a matching `Tuple`
  column so the nested `source` Struct actually round-trips.

  ```sql
  CREATE TABLE %s (
    `id`     Int32,
    `op`     String,
    `source` Tuple(db String, `table` String)
  ) Engine = MergeTree ORDER BY id
  ```

  Assertion: records with a filtered `op` for a listed table are absent; unfiltered rows are present.
  Rejected: `bypassSchemaValidation=true` with a flat table (disables the validation a regression
  would trip); flattening `source` into plain String columns (no longer the Debezium shape the filter
  is written against). **See research flag R-01.**

- **D-07:** **CONV-08 asserts the real end-to-end landing, not a single-column store.** With
  `output.json.enabled=true` the SMT emits a `String` value with a null schema, which the connector
  routes through `StringRecordConvertor` — that path parses the JSON and maps **keys to columns**.
  So: top-level keys land in their matching typed columns, and a nested struct field lands in a `JSON`
  column with correct content (this is what satisfies the "String/JSON column" criterion).

  ```sql
  CREATE TABLE %s (
    `off16`   Int16,
    `str`     String,
    `p_int32` Int32,
    `nested`  JSON
  ) Engine = MergeTree ORDER BY off16
  ```

  Rejected: wrapping the payload so the whole document lands in one String column (artificial — the
  SMT only ever transforms the whole record value, so nothing in production produces that shape);
  running the SMT with `output.json.enabled=false` (skips the JSON encoder, the part most worth
  pinning at sink level). **See research flag R-02.**

- **D-08:** **HDRS-09 and HDR-07 use the schema/Struct path only.** The map schema
  `HeadersToValue.configure()` builds — `SchemaBuilder.map(STRING, OPTIONAL_STRING).optional()` — is
  the fork addition worth pinning at sink level, and it is what drives the insert into
  `Map(String, Nullable(String))`. The schemaless path is already covered by the unit tests
  (HDRS-02, HDR-03).

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

  Rejected: a fifth sink test covering HDRS-09 on the schemaless path too (extra container time beyond
  the "one per SMT" criteria); schemaless-only (then nothing at sink level exercises the generated
  map schema).

### Unit test organization and style

- **D-09:** **Grow the four existing test files**; do not add per-SMT sibling classes. One file per SMT
  stays the rule so everything about a transform is in one place. Upstream has none of these SMTs, so
  these are fork-only files with no rebase conflict from an upstream sync. Expect them to land around
  350-600 lines:
  - `src/test/java/transforms/TableOperationFilterTest.java` (177 lines / 9 tests today)
  - `src/test/java/transforms/HeadersToValueTest.java` (84 / 2)
  - `src/test/java/transforms/HeaderToValueTest.java` (74 / 2)
  - `src/test/java/transforms/ConvertToMapOrJsonTest.java` (322 / 13)

  Rejected: `@Nested` grouping — no test in the repo uses it today.

- **D-10:** **Every new test carries its requirement ID in `@DisplayName`.** Method names keep the
  repo's `test*` convention. This gives Phase 5 a greppable REQUIREMENTS.md → test mapping that
  cannot drift, and `@DisplayName` is already used in 5 test classes
  (`LocalProviderTest`, `ProcessingTest`, `RangeContainerTest`, `UtilsTest`, `MaskTest`).

  ```java
  @Test
  @DisplayName("TOF-03: invalid, empty and whitespace tokens are ignored")
  public void testInvalidSkippedOperationsIgnored() { ... }
  ```

- **D-11:** **`@ParameterizedTest` for genuine matrices only**, plain `@Test` everywhere else.
  Matrices: TOF-03's invalid/empty/whitespace/`r` tokens, CONV-01's jackson-vs-gson, and the
  schema-vs-schemaless duplicates in HDRS/HDR. No build change needed — the `junit-jupiter` aggregate
  (BOM `6.0.2`, `build.gradle.kts:124-125`) already brings `junit-jupiter-params`, though no test in
  the repo uses it today.

  ```java
  @ParameterizedTest(name = "TOF-03: token ''{0}'' is ignored")
  @ValueSource(strings = {"x", "r", "", "   ", "cu"})
  @DisplayName("TOF-03: invalid tokens ignored, records pass through")
  void testInvalidTokenIgnored(String token) { ... }
  ```

  Rejected: parameterizing aggressively (pushes case data into annotations where a failure is harder
  to read back to a requirement).

- **D-12:** **One shared, fork-only helper class in `src/test/java/transforms/`** holding the record
  builders and the metadata-preservation assertion. HDRS-06, HDR-05 and CONV-04 are literally the same
  assertion across three SMTs — it gets written once. The sink-level test class can reuse the
  builders. No Docker, so it stays inside `testUnit`.

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

### Config-error contracts

- **D-13:** **CONV-05 — pin `ConfigException`, including the case-sensitivity quirk; change no code.**
  `CONFIG_DEF` uses `ConfigDef.ValidString.in("jackson", "gson")` (`ConvertToMapOrJson.java:67`), which
  validates inside `new AbstractConfig(...)` at line 92 and is **case-sensitive**. Consequences to pin:
  - an unknown library value throws `ConfigException`, not `DataException`
  - `output.json.library=JACKSON` is **rejected**, even though `configure()` calls `.toLowerCase()`
  - the `try { JsonLibrary.valueOf(...) } catch → DataException` at lines 100-104 is **unreachable**

  The dead branch stays. Removing it is behavior-neutral cleanup with no payoff on a fork that must
  stay rebasable, and widening `ValidString` to accept mixed case is a config-semantics change the
  milestone rules out.

- **D-14:** **HDRS-04 — pin the real error split and correct the requirement text.**
  `FIELD_CONFIG` is declared `ConfigDef.NO_DEFAULT_VALUE` (`HeadersToValue.java:35`), so an **absent**
  `field` key throws `ConfigException` out of `new AbstractConfig(...)` at line 46, before the explicit
  check ever runs. Only a **blank/whitespace** value reaches the `DataException` at line 50.
  REQUIREMENTS.md currently claims both are `DataException`.
  - Test both cases separately: absent → `ConfigException`; blank → `DataException` with its message.
  - **Update the HDRS-04 wording in `.planning/REQUIREMENTS.md`** to describe the split.
  - **Check HDR-01 for the same issue** — `fields` and `headers` are also `NO_DEFAULT_VALUE`
    (`HeaderToValue.java:49, 55`), so absent → `ConfigException` while a genuine count mismatch →
    `DataException` (HDR-01 as written covers only the mismatch, which is correct).

  Rejected: asserting the common `KafkaException` supertype (stops pinning what an operator actually
  sees); giving the config a `""` default so absent falls through to `DataException` (changing main
  code to make the docs right).

- **D-15:** **TOF-03 — behavioral assertion only, no log appender.** Assert that `configure()` does not
  throw for any bad token and that a record whose `op` matches that token passes through untouched —
  which is what "ignored" means and what a production config depends on. The `LOGGER.warn` at
  `TableOperationFilter.java:78` is not asserted: an appender would couple the test to log wording,
  need setup/teardown, and touch shared logger state under `maxParallelForks`.
  If Phase 3's DESC-04 ("logged at ERROR instead of WARN") turns out to need log capture, build the
  helper there.

- **D-16:** **Negative tests assert the exception type, plus the cause message where that message is
  the contract.** All three of `TableOperationFilter`, `HeadersToValue` and `HeaderToValue` wrap
  anything thrown inside `apply()` as `new DataException("Failed to transform record", e)`, so the
  real diagnostic lives on the cause. Use a substring match on the inner message, never full-string
  equality at every level.

  ```java
  DataException ex = assertThrows(DataException.class,
          () -> xform.apply(record));
  assertEquals("Failed to transform record", ex.getMessage());
  assertInstanceOf(DataException.class, ex.getCause());
  assertTrue(ex.getCause().getMessage().contains("must be a Map"));
  ```

### Research flags (resolve before the plan locks the affected test)

- **R-01 (D-06):** Confirm the writer maps a nested `Struct` to a ClickHouse `Tuple` column on
  `latest` via the RowBinary path. If it does not, TOF-06 needs a different table shape — fall back to
  flattening `source` into String columns rather than turning off schema validation.
- **R-02 (D-07):** Confirm a `String`-valued record (null schema) inserts via `JSONEachRow`, not
  RowBinary. If it goes through RowBinary, CONV-08's `JSON` column collides with the Phase 3 TYPE-06
  gap (`Type.DYNAMIC`/`JSON` have no RowBinary serialization) — in that case use a `String` column for
  the nested field in Phase 2 and leave the `JSON` column to Phase 3.

### Claude's Discretion

- Name and package of the new ClickHouse-backed sink test class (it must extend `ClickHouseBase` to
  inherit `@Tag("clickhouse")`, the per-class database and the MBean sweep).
- Name of the shared unit-test helper class (`TransformTestHelpers` is a placeholder) and the exact
  set of builders it exposes.
- Whether each sink-level test gets its own table or some share one — the four column shapes in D-06 /
  D-07 / D-08 differ, so separate tables is the likely answer.
- Exact `@DisplayName` phrasing beyond the `REQ-ID: ` prefix.
- Which specific cases become `@ParameterizedTest` within the matrices named in D-11.
- How many records each sink test inserts, and the assertion helper used to read them back
  (`ClickHouseTestHelpers.countRows` / `validateRows` or a direct query).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and requirements
- `.planning/ROADMAP.md` § "Phase 2: Fork SMT Coverage" — goal, the 5 success criteria, and the plan
  split 02-01 / 02-02 / 02-03 (all wave 1)
- `.planning/REQUIREMENTS.md` § Transforms — TOF-01..06, HDRS-01..09, HDR-01..07, CONV-01..08, plus
  § Out of Scope. **Note D-14: the HDRS-04 wording is wrong and must be corrected.**
- `.planning/PROJECT.md` § Constraints, § Key Decisions, § Context — behavior preservation, warn-and-ignore
  pinning for invalid `skipped.operations`, rebasability

### Research (per-feature test-case inventories and pitfalls)
- `.planning/research/FEATURES.md` — per-feature test-case inventory for all four SMTs with source
  line references
- `.planning/research/PITFALLS.md` — the 10 pitfalls grounded in fork source; Pitfall 5 (MBean leak /
  no try/finally) and Pitfall 7 (`windows-1252` test encoding) apply to the sink-level tests here
- `.planning/research/ARCHITECTURE.md` — Pattern 2 (JUnit tags + `testUnit`) governs where the new
  unit tests must land

### Prior phase decisions that bind this one
- `.planning/phases/01-test-infrastructure-green-baseline/01-CONTEXT.md` — D-09/D-10 (MBean discipline
  and `uniqueConnectorName`), D-12 (`@Tag("clickhouse")` inherited from `ClickHouseBase`), D-02
  (the escalation rule carried forward here as D-04)
- `.planning/STATE.md` § Blockers/Concerns — run Gradle as
  `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; colima profile `vz`

### Codebase maps
- `.planning/codebase/TESTING.md` — test framework, setup/teardown patterns, `build.gradle.kts` test
  config. Note: its "no coverage plugin configured" section is stale as of Phase 1.

### Source under test
- `src/main/java/com/clickhouse/kafka/connect/transforms/TableOperationFilter.java` (173 lines)
- `src/main/java/com/clickhouse/kafka/connect/transforms/HeadersToValue.java` (196 lines)
- `src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java` (232 lines)
- `src/main/java/com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson.java` (299 lines)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` — the sink-level test class
  extends this. Relevant members: `createProps()`, `createClient(props)`, `createTopicName(name)`,
  `createTable(chc, topic, query)`, `countRows(chc, database, topic)`,
  `uniqueConnectorName(TestInfo)` (opt-in, Phase 1 D-10), `cleanupLeakedMBeans()` `@AfterEach`.
- `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` — `dropTable`,
  `createTable`, `countRows`, `validateRows`, and the UTF-8 readers fixed in Phase 1 (D-11).
- `src/test/java/transforms/HeadersToValueTest.java` `generateSampleRecord(topic, partition, offset,
  headers)` — the shape the D-12 shared builders should generalize.
- The `junit-jupiter` aggregate is already on the test classpath, so `@ParameterizedTest` /
  `@ValueSource` need no build change.

### Established Patterns
- Existing sink tests follow: build props → `createClient` → drop/create table → build records →
  `new ClickHouseSinkTask()` → `start(props)` → `put(records)` → `stop()` → assert
  `countRows` / `validateRows`. Phase 1's rule adds: create the table **before** `start()` (an empty
  database makes `ClickHouseWriter.start()` fail), wrap in try/finally, use `uniqueConnectorName`.
- Unit vs ClickHouse-backed is split by **inheritance**, not directory: anything extending
  `ClickHouseBase` inherits `@Tag("clickhouse")` and is excluded from `testUnit`. The new unit tests
  must stay outside that hierarchy so `testUnit` keeps running them Docker-free.
- Test method naming is `test*`; assertions are plain JUnit Jupiter (`assertEquals`, `assertTrue`,
  `assertThrows`, `assertInstanceOf`). Hamcrest is available but barely used.
- All four SMTs wrap `apply()` failures in `DataException("Failed to transform record", e)` — except
  `TableOperationFilter`, which uses `DataException("Failed to process record in TableOperationFilter", e)`.
- Only `org.apache.kafka:connect-api` is a dependency (`build.gradle.kts:86`) — no `connect-runtime`.

### Integration Points
- `src/test/java/transforms/` — four existing test files grow (D-09), one new shared helper (D-12).
- A new ClickHouse-backed sink test class extending `ClickHouseBase` (D-05..D-08).
- `src/main/java/com/clickhouse/kafka/connect/transforms/` — three targeted fixes (D-01, D-02, D-03).
  All three classes are already in the `jacocoForkReport` include list from Phase 1 D-13, so no build
  change is needed for coverage.
- `.planning/REQUIREMENTS.md` — the HDRS-04 wording correction (D-14).

</code_context>

<specifics>
## Specific Ideas

- The `JSON` column in D-07 and the `Tuple` column in D-06 are the two places this phase touches
  ClickHouse types that Phase 3 owns. Both carry a research flag (R-01, R-02) and a named fallback, so
  Phase 2 never blocks on Phase 3.
- The D-02 fix uses **reference** comparison (`oldValue.schema() != sourceSchema`), not `equals()`.
  Connect reuses schema instances per converter, so reference equality is the cheap common case; a
  deep `equals()` on every record would be a per-record cost in the hot path.
- D-11's `@ValueSource` for TOF-03 should include `"r"` explicitly — PROJECT.md pins `r` as
  warn-and-ignore specifically to avoid breaking existing production connector configs.

</specifics>

<deferred>
## Deferred Ideas

Surfaced during discussion, not decided — kept so they are not lost:

- **`HeadersToValue.close()` nulls `headersSchema`** (line 194) as well as `valueSchema`, so a
  `close()`-then-`apply()` sequence would NPE or build a field with a null schema. Not a legal Connect
  lifecycle, so no test and no fix this phase. Revisit if HDRS-08 work makes it observable.
- **Removing the unreachable `DataException` branch** in `ConvertToMapOrJson.configure()`
  (lines 100-104) — behavior-neutral cleanup, deliberately declined under D-13. A candidate for a
  future upstream-sync or cleanup pass.
- **A fifth sink test covering HDRS-09 on the schemaless path** — declined under D-08 to stay within
  "one sink-level test per SMT". Worth revisiting if the JSONEachRow route ever regresses.
- **Case-insensitive `output.json.library`** — declined under D-13 as a config-semantics change.
  Belongs in a future milestone alongside CLEAN-01 if the fork ever revisits SMT config.
- **`HeaderToValueConfig` dead-code question** — already tracked as v2 requirement CLEAN-01 in
  `.planning/REQUIREMENTS.md`; untouched here.

</deferred>

---

*Phase: 02-fork-smt-coverage*
*Context gathered: 2026-09-16*
