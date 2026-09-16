# Phase 3: Table Metadata & JSON/Dynamic Types - Context

**Gathered:** 2026-09-17
**Status:** Ready for planning

<domain>
## Phase Boundary

Table discovery and column typing added by the fork work correctly on ClickHouse `latest` with client V2:

- `targetTableFilter` — FILT-01..04
- `describeTable` sub-column skipping — DESC-01..04
- `JSON` / `JSON(...)` / `Dynamic` type mapping and round-trips — TYPE-01..05, TYPE-07
- RowBinary fail-fast for those types — TYPE-06

That is 15 requirements. Unlike Phase 2, **this phase has no `transforms/`-style safe zone** — every target
is main code in `ClickHouseHelperClient`, `Column`/`Table`, and `ClickHouseWriter`. The fix budget is
therefore enumerated explicitly in D-01 rather than scoped by directory.

Out of this phase:

- Explicit `insertFormat=json` / metadata-free mode, `jsonInsertColumns`, MBean naming, `async_insert` — Phase 4
  (JSON-01..05, COLS-01..06, JMX-01..04, ASYNC-01..03)
- CI workflow changes — Phase 5
- RowBinary **serialization** for `JSON`/`Dynamic` — locked Out of Scope in REQUIREMENTS.md; this phase
  produces a clear error instead
- Auto-routing tables with `JSON`/`Dynamic` columns to the JSON path — locked Out of Scope
- `describeTableV1`/`V2` result parity (PAR-01) — v2 requirement

</domain>

<decisions>
## Implementation Decisions

### Main-code fix scope (the escalation rule for a phase with no safe directory)

- **D-01:** **Five main-code changes are pre-approved, and only these five.** Phase 1 D-02 / Phase 2 D-04
  carry forward unchanged, but with the declared scope being this enumerated list rather than a directory:

  1. `ClickHouseWriter.doWriteColValue()` — throw in the `default:` branch (D-02, D-03, D-05)
  2. `ClickHouseWriter.validateDataSchema()` — same message for `JSON`/`DYNAMIC`/`UNKNOWN` columns (D-04)
  3. `ClickHouseHelperClient` — extract `shouldSkipDescribeRow(...)`, apply the structural skip rule, and
     change the unmappable-column log from `warn` to `error` (D-06, D-07, D-08)
  4. `ClickHouseHelperClient` — extract the `match()` clause builder and escape backslashes (D-09, D-11)
  5. `ClickHouseHelperClient` lines 370 and 417 — explicit `StandardCharsets.UTF_8` on the `describeTable`
     readers, **if** D-14's identifier test proves the bug

  Anything beyond this list — a new method elsewhere, a signature change, a fix in `Processing`,
  `ClickHouseSinkConfig`, `ProxySinkTask`, or `Table` — **stops for a user decision**. Never disable or
  `@Disabled` a test to get green.

### RowBinary fail-fast (TYPE-06)

- **D-02:** **Throw in `doWriteColValue()`'s `default:` branch.** Today (`ClickHouseWriter.java:535-538`)
  that branch logs `"Cannot serialize unsupported type {}"` and **writes zero bytes**, so the caller keeps
  streaming the next column into a desynced RowBinary stream — silent corruption, not a failure. Replace the
  log-only line with a thrown `DataException`. Roughly two lines, confined to one method, rebase-friendly.

  Rejected: a pre-check scanning `table`'s columns before streaming (a second place holding the same
  knowledge, and a larger change under D-01); doing both (largest diff on a fork that must stay rebasable).

- **D-03:** **The throw covers every type reaching the branch**, not just `JSON`/`DYNAMIC`. `Type.UNKNOWN`
  lands there too — `Column.extractColumn` falls back to it for any type string `dispatchPrimitive` does not
  recognize — and a desynced stream is never the better outcome. One message, no type list to keep in sync.

- **D-04:** **`validateDataSchema()` speaks the same error.** With default config the writer gate is never
  reached: `doInsertRawBinaryV2` calls `validateDataSchema` first (`ClickHouseWriter.java:747`), which
  inspects only `!isNullable && !hasDefault` columns — exactly what a plain `JSON`/`Dynamic` column is — and
  its `colTypeName` (`"JSON"`) can never equal a Connect `dataTypeName` (`STRUCT`, `STRING`, …). So today an
  operator gets `RuntimeException("Data schema validation failed.")`, which says nothing actionable.

  Add a `JSON`/`DYNAMIC`/`UNKNOWN` check in that loop emitting the D-05 message. **That path already fails
  today**, so this is a message change on an already-failing insert, not a behavior change.

- **D-05:** **The message names the column, its type, and both escape hatches** — `insertFormat=json` *and*
  `bypassRowBinary=true`. Both are routes D-12 pins as working, so the error should not point only at the
  heavier config change. Tests assert the stable `insertFormat=json` substring, never full-string equality,
  so wording can evolve (Phase 2 D-16).

- **D-06:** **TYPE-06 is pinned at two levels.** Docker-free unit tests call the two `protected` gates
  directly to pin the message contract; one ClickHouse-backed test drives a real `JSON` table through
  `ClickHouseSinkTask.put()` and asserts the insert fails **and** `countRows == 0`. The requirement says
  "with no silent data loss" — only the live test proves that half.

### `describeTable` sub-column handling (DESC-01..04)

- **D-07:** **Structural skip rule by parent type.** When the DESC-03 audit finds sub-column shapes the
  current list misses, do not chase suffixes: skip any row whose parent column resolves to `Type.JSON` or
  `Type.DYNAMIC` (the `Table` under construction already exposes `getAllColumnsMap()` for the lookup).
  `.null` and `.size0` stay for `Nullable`/`Array`.

  **Map and Tuple sub-columns must keep flowing through** — `Table.updateParent` (`Table.java:93-182`)
  genuinely consumes them to fill `mapValueType`, `mapDepth`, and `tupleFields`. A blanket "skip all
  sub-columns" rule would break Map and Tuple mapping.

  Known defect to fix while in there: the current condition is
  `fd.isSubcolumn() && (name.endsWith(".null") || name.endsWith(".size0")) || name.contains(".object_shared_data.")`
  — the last clause is **not** guarded by `isSubcolumn()` and requires a **trailing dot**, so a row named
  exactly `json_col.object_shared_data` slips through.

  Rejected: extending the literal suffix list (goes stale the next time ClickHouse changes JSON's internal
  layout, and the failure is silent).

- **D-08:** **One private helper, called from both `describeTableV1` and `describeTableV2`.** The block is
  duplicated verbatim today (`ClickHouseHelperClient.java:379` and `:426`) and the structural rule is several
  lines, so two copies would drift. A static/package-private
  `shouldSkipDescribeRow(ClickHouseFieldDescriptor, Table)` is also directly unit-testable with synthesized
  `ClickHouseFieldDescriptor`s, which turns DESC-01..03 into Docker-free unit tests plus one live
  end-to-end check instead of container-only tests.

  Rejected: V2-only (leaves V1 silently divergent); duplicating the rule in both methods.

- **D-09:** **DESC-04 is pinned behaviourally; the log level changes unasserted.** Assert `describeTable`
  returns `null` so the table is dropped, flip `LOGGER.warn` → `LOGGER.error` in both methods, and build no
  appender. This is the call Phase 2 D-15 made for TOF-03: no shared logger state, no coupling to wording.

  Note for whoever writes this test: `Column.extractColumn` returns `null` **only** for `Nested` (and
  `Array(Nested(...))`) — every unrecognized primitive becomes `Type.UNKNOWN` instead. So DESC-04's fixture
  must use a `Nested` column, the same shape as the existing upstream `ignoreArrayWithNestedTable` test.

### `targetTableFilter` (FILT-01..04)

- **D-10:** **Extract a clause builder.** Pull the ternary at `ClickHouseHelperClient.java:297` into a small
  static helper (e.g. `buildTableFilterClause(String filter)`) called from `showTables()`. FILT-01 becomes
  "the clause is the empty string", FILT-03 becomes an exact-string assertion on the escaped quote, and RE2
  patterns can be checked for round-tripping — all Docker-free, plus the live queries the requirements ask for.

  Rejected: extracting the whole query builder (pins the `system.columns` query shape, brittle against
  upstream); live-only with no main-code change (an escaping bug surfaces as a generic query failure).

- **D-11:** **Escape backslashes as well as quotes.** Only `'` → `''` is handled today, so inside a ClickHouse
  single-quoted literal an unrecognized escape like `\d` loses its backslash and `'^tbl_\d+$'` reaches
  `match()` as `^tbl_d+$` — matching a literal `d`. Escape `\` before `'` in the extracted builder so the
  connector config takes a plain RE2 pattern.

  A config using `\d` today already matches nothing (visibly broken), so nothing can be *successfully*
  relying on the current behavior except a config that pre-doubles the backslash as a workaround; such a
  config needs one edit after this ships. **Confirm with R-03 before applying.**
  — **Reversibility:** costly — reverting after release would re-break any connector config edited to the
  plain-pattern form, in a fork that runs production CDC pipelines.

- **D-12:** **FILT-02 is split in two, and its wording is corrected.** The requirement's premise cannot
  happen as written: a filter matching nothing leaves `mapping` empty, so `ClickHouseWriter.start()` returns
  `false` and `ProxySinkTask` throws `RuntimeException("Connection to ClickHouse is not active.")` at task
  start — **regardless of `suppressTableExistenceException`**, which only takes effect in `getTable()`
  (`ClickHouseWriter.java:1226`) when the mapping is non-empty but the target table is missing.

  - (a) filter matches nothing → `start()` throws, asserted with the suppress flag both `true` and `false`,
    proving it makes no difference
  - (b) filter matches some tables but not the target → `suppress=true` drops records silently,
    `suppress=false` throws `"Table X does not exist"`
  - **Update FILT-02's wording in `.planning/REQUIREMENTS.md`** to describe the split — the same move Phase 2
    D-14 made for HDRS-04.

  Rejected: also changing the misleading `"Connection to ClickHouse is not active."` message (touches an
  upstream-shared path no requirement in this phase owns — escalate under D-01 if it is wanted).

### `JSON` / `Dynamic` round-trips and non-ASCII (TYPE-01..05, TYPE-07)

- **D-13:** **Two routes pin TYPE-03/04/05: schemaless `JSONEachRow` and `bypassRowBinary` Struct records.**
  "The JSON insert path" is ambiguous across three routes (`ClickHouseWriter.java:208-222`); this is the
  reading that stays inside Phase 3. Since D-02..D-05 make RowBinary throw for these columns, these two are
  the real production escape hatches and both deserve pinning — the `bypassRowBinary` Struct route is the
  more common CDC shape and is untested today.

  Rejected: explicit `insertFormat=json` (that is JSON-01..05 in Phase 4 — two phases owning one code path);
  schemaless only (leaves the Struct route uncovered until an operator hits the new TYPE-06 error).

- **D-14:** **Typed SQL read-back, not row counts.** Today `JsonColumnTest` asserts only `countRows`, which is
  why a `JSON` column could be silently wrong. Read the columns back with explicit queries: nested path access
  for `JSON` (`json_col.a.b`), plus `toTypeName()` / `dynamicType()` assertions for `Dynamic`, so TYPE-04's
  "correct values **and types**" is literally checked. A `Dynamic` column storing the right value under the
  wrong inferred type is exactly the bug a JSON dump would hide.

- **D-15:** **TYPE-07 covers non-ASCII identifiers as well as values.** Values (Cyrillic, CJK) through
  `String`, `JSON`, and `Dynamic` columns and through SMT header values as written, **plus** a table with a
  non-ASCII column name (and/or `Enum` member) driven through `describeTable`.

  Rationale: the readers Phase 1 D-11 deferred to this phase — `new InputStreamReader(queryResponse.getInputStream())`
  with no charset at `ClickHouseHelperClient.java:370` and `:417`, under the `windows-1252` test JVM — only
  ever see **DESCRIBE output**. Record values never touch them. Column names, `Enum` member names, and DEFAULT
  expressions do. If the identifier test mangles, apply the explicit `StandardCharsets.UTF_8` fix to both
  readers (D-01 item 5), with a test that proves it.

  Rejected: fixing the readers unproven (nothing would catch a regression); re-deferring them a second time.

### Test organization

- **D-16:** **All Phase 3 tests land in new fork-only test classes.** Phase 2 D-09 ("grow the existing file")
  does **not** transfer: `ClickHouseHelperClientTest`, `ColumnTest`, and `ClickHouseSinkTaskMappingTest` are
  upstream files, so growing them adds conflict surface on every future upstream sync. Leave all three
  untouched. This also maps cleanly onto the roadmap's three-plan split.

- **D-17:** **Phase 2's unit-test conventions carry forward unchanged:** `REQ-ID: ` prefix in every
  `@DisplayName` (D-10), `@ParameterizedTest` for genuine matrices only (D-11), negative tests assert the
  exception type plus a **substring** of the cause message where that message is the contract (D-16).

- **D-18:** **Non-ASCII test data is built from Unicode code points at runtime, never from `\u` escape
  literals in source.** Phase 2 hit this directly: the editing tool chain silently decoded literal
  backslash-u escape text into real non-ASCII bytes. Applies to every TYPE-07 fixture.

### Research flags (resolve before the plan locks the affected test)

- **R-01 (D-07):** Capture raw `DESCRIBE TABLE ... SETTINGS describe_include_subcolumns=1` output on `latest`
  for `Nullable`, `Array`, `JSON`, `JSON(...)`, and `Dynamic` columns, and record it in the plan. It
  determines which rows the structural rule must catch and whether `isSubcolumn()` is even `true` for JSON
  internal paths — if it is `false`, those rows become **root** columns and `validateDataSchema` would start
  demanding them from every record, which is a louder failure than the current WARN.
- **R-02 (D-04):** Establish which gate actually fires first for a schema record targeting a `JSON` column
  under default config, and with what message, before the fix lands. The D-04 reading is from source, not
  from a run.
- **R-03 (D-11):** Confirm live what ClickHouse does with `\d` and with anchors inside the `match()` literal
  before changing the escaping. If backslashes survive intact, D-11 collapses to a pin rather than a fix.

### Claude's Discretion

- Names and packages of the new fork-only test classes, and how they map onto plans 03-01 / 03-02 / 03-03.
- Exact signature and visibility of `shouldSkipDescribeRow(...)` and `buildTableFilterClause(...)` — only that
  they are reachable from a Docker-free test.
- Exact wording of the D-05 error message beyond naming column, type, `insertFormat=json`, and
  `bypassRowBinary=true`; and the exception type at each gate.
- Live-test table shapes, how many records each inserts, and whether tests share a table.
- Which TYPE-01/TYPE-02 type strings get parameterized (`JSON`, `JSON(max_dynamic_paths=...)`, `Dynamic`,
  `Dynamic(max_types=N)`, and the `Nullable`/`LowCardinality`/`Array`/`Map` wrappers around them).
- Exact `@DisplayName` phrasing beyond the `REQ-ID: ` prefix.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and requirements

- `.planning/ROADMAP.md` § "Phase 3: Table Metadata & JSON/Dynamic Types" — goal, the 5 success criteria, the
  2 research flags, and the plan split 03-01 / 03-02 / 03-03 (all wave 1)
- `.planning/REQUIREMENTS.md` § "Table Metadata — targetTableFilter" (FILT-01..04), § "Table Metadata —
  describeTable" (DESC-01..04), § "Column Types — JSON and Dynamic" (TYPE-01..07), plus § Out of Scope.
  **Note D-12: the FILT-02 wording is wrong and must be corrected.**
- `.planning/PROJECT.md` § Constraints, § Key Decisions, § Context — behavior preservation, rebasability, the
  locked "`JSON`/`Dynamic` via RowBinary, if broken → explicit clear error" decision, and the known
  `Type.DYNAMIC` RowBinary risk

### Prior phase decisions that bind this one

- `.planning/phases/01-test-infrastructure-green-baseline/01-CONTEXT.md` — D-02 (the escalation rule carried
  forward here as D-01), **D-11 (the deferred UTF-8 fix this phase is expected to resolve — see D-15)**,
  D-08 (fresh database per test class, which FILT's table-listing assertions depend on), D-12
  (`@Tag("clickhouse")` inherited from `ClickHouseBase`), D-13 (the fork coverage include list already
  contains `ClickHouseHelperClient`, `ClickHouseWriter`, `Column`, `Table`, `Type` — no build change needed)
- `.planning/phases/02-fork-smt-coverage/02-CONTEXT.md` — D-04 (escalation), D-10/D-11/D-16 (test conventions,
  carried forward as D-17), D-14 (the precedent for correcting a wrong requirement, applied here in D-12),
  D-15 (the deferred log-capture helper, declined again in D-09)
- `.planning/STATE.md` § Blockers/Concerns — run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`;
  colima profile `vz`; the two Phase 3 research flags

### Research (per-feature test-case inventories and pitfalls)

- `.planning/research/FEATURES.md` — per-feature test-case inventory with source line references for
  `targetTableFilter`, `describeTable`, and the `JSON`/`Dynamic` type mapping
- `.planning/research/PITFALLS.md` — Pitfall 5 (MBean leak / no try/finally) and Pitfall 7 (`windows-1252`
  test encoding, directly relevant to TYPE-07) apply to every ClickHouse-backed test here
- `.planning/research/ARCHITECTURE.md` — Pattern 2 (JUnit tags + `testUnit`) governs where the Docker-free
  unit tests must land

### Codebase maps

- `.planning/codebase/TESTING.md` — test framework and setup/teardown patterns. Its "no coverage plugin
  configured" section is stale as of Phase 1.
- `.planning/codebase/CONCERNS.md` — the pre-existing note that `Type.DYNAMIC` has no RowBinary serialization,
  which TYPE-06 converts from silent corruption into a clear error

### Source under test

- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` (549 lines) —
  `showTables()` :296-307, `describeTableV1` :352-403, `describeTableV2` :405-448, the unguarded readers
  :370 and :417
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (1247 lines) —
  `validateDataSchema` :225-281, `doWriteColValue` default branch :535-539, `doInsert` routing :208-222,
  validation call sites :747 and :808, `getTable` :1211-1236
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` (385 lines) —
  `dispatchPrimitive` JSON/Dynamic handling :152-156, `extractColumn` :167-265
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java` (194 lines) — `addColumn` /
  `handleNonRoot` / `updateParent` :70-182, including the `default:` WARN a surviving JSON sub-column hits
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Type.java` — `JSON` and `DYNAMIC` enum members

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` — every ClickHouse-backed test class
  extends this: `createProps()`, `createClient(props)`, `createTopicName(name)`, `createTable(...)`,
  `countRows(...)`, `uniqueConnectorName(TestInfo)` (Phase 1 D-10), the `@AfterEach` MBean sweep, the
  inherited `@Tag("clickhouse")`, and a fresh database per test class (Phase 1 D-08).
- `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` — `query`, `dropTable`,
  `createTable`, `countRows`, `getAllRowsAsJson`, `validateRows` (UTF-8-fixed in Phase 1 D-11), and the
  `newDescriptor(name, valueType)` / `newDescriptor(valueType)` / `col(...)` builders that `ColumnTest` uses.
  **`newDescriptor` is the seam for both TYPE-01/02 and the D-08 skip-helper unit tests.**
- `src/test/java/com/clickhouse/kafka/connect/sink/junit/extension/FromVersionConditionExtension.java` +
  `@SinceClickHouseVersion` — already used by `JsonColumnTest` and `ClickHouseHelperClientTest`; available if
  any JSON/Dynamic case needs version gating (the milestone targets `latest` only, so it should rarely apply).
- `src/test/java/transforms/TransformTestHelpers.java` (Phase 2 D-12) — record builders, if a TYPE-07 case
  needs to push non-ASCII header values through an SMT.

### Established Patterns

- Unit vs ClickHouse-backed is split by **inheritance**, not directory: extending `ClickHouseBase` inherits
  `@Tag("clickhouse")` and is excluded from `testUnit`. The new Docker-free classes must stay outside that
  hierarchy.
- ClickHouse-backed tests follow: build props → `createClient` → drop/create table → build records →
  `new ClickHouseSinkTask()` → `start(props)` → `put(records)` → `stop()` → assert. Phase 1's rules add:
  create the table **before** `start()` (an empty database makes `ClickHouseWriter.start()` fail), wrap in
  try/finally, use `uniqueConnectorName`.
- `Column.extractColumn` returns `null` only for `Nested` / `Array(Nested(...))`; every other unrecognized
  type string becomes `Type.UNKNOWN` and flows onward. This is why DESC-04 needs a `Nested` fixture, and why
  D-03's blanket throw matters — `UNKNOWN` columns currently reach the writer and corrupt the stream.
- `Table.addColumn` routes `isSubColumn()` rows to `handleNonRoot` → `updateParent`, whose `default:` branch
  WARNs for an unsupported parent type. A JSON sub-column that survives the skip list lands there — noisy, but
  it does **not** drop the table.
- Test method naming is `test*`; assertions are plain JUnit Jupiter. Hamcrest is available but barely used.

### Integration Points

- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` — two extractions
  (D-08, D-10), one structural rule, one log level, one escaping fix, and conditionally two charset arguments.
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` — two touch points (D-02, D-04).
- New fork-only test classes only (D-16). No upstream test file is edited.
- `.planning/REQUIREMENTS.md` — the FILT-02 wording correction (D-12).
- No `build.gradle.kts` change: all five affected main classes are already in the Phase 1 D-13
  `jacocoForkReport` include list.

</code_context>

<specifics>
## Specific Ideas

- The `.object_shared_data.` clause carries **two** defects at once — missing `isSubcolumn()` guard and a
  required trailing dot. Fix both when the structural rule replaces it, and say so in the commit, because a
  reviewer diffing against upstream will otherwise read it as an unrelated change.
- D-04's value is that the error an operator *actually* hits becomes the actionable one. If R-02 shows the
  writer gate fires first under default config after all, D-04 becomes optional rather than required — flag
  it rather than implementing it anyway.
- TYPE-04's `dynamicType()` assertion is the one place where "the value round-tripped" and "the type
  round-tripped" genuinely differ; a `Dynamic` column holding `42` as a `String` passes a value-only test.
- FILT-01 ("all tables in the database are mapped") is only meaningful because Phase 1 D-08 gives each test
  class a fresh empty database — assert against tables this class created, never against a global list.
- The new `bypassRowBinary` Struct-record test in D-13 is also the honest proof that D-05's message is not
  lying when it offers that flag as an escape hatch.

</specifics>

<deferred>
## Deferred Ideas

Surfaced during discussion, not decided — kept so they are not lost:

- **The misleading `"Connection to ClickHouse is not active."` message** when the real cause is an empty or
  fully-filtered-out table mapping (`ProxySinkTask.java:63-65`). Declined under D-12 as a path no Phase 3
  requirement owns; a good candidate for a later cleanup or an upstream contribution.
- **A pre-check that scans a table's columns for unsupported types before streaming any RowBinary bytes** —
  declined under D-02 in favour of the single default-branch throw. Revisit if operators report that the
  per-column failure point is hard to read.
- **The log-capture test helper** — declined again under D-09, after Phase 2 D-15 deferred it here. If a
  future requirement genuinely needs log-level assertions, this is the third time it will have come up.
- **`describeTableV2` ignores its `database` parameter** — it builds `DESCRIBE TABLE \`this.database\`.\`table\``
  (`ClickHouseHelperClient.java:408`) while V1 honours the argument. Harmless today because
  `extractTablesMapping` always passes `this.database`, and `settings.setDatabase(database)` is set anyway.
  Out of D-01's fix list; note it for a future upstream sync or PAR-01.
- **`describeTableV1`/`V2` result parity** — already tracked as v2 requirement PAR-01; untouched here.

</deferred>

---

*Phase: 03-table-metadata-json-dynamic-types*
*Context gathered: 2026-09-17*
