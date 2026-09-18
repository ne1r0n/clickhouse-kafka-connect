# Phase 4: JSON Insert Path & Connector Plumbing - Context

**Gathered:** 2026-09-17
**Status:** Ready for planning

<domain>
## Phase Boundary

The work-in-progress metadata-free JSON insert path and `jsonInsertColumns` are pinned **as they are
today**, and the connector-level fork changes are covered:

- Explicit `insertFormat=json` / metadata-free mode — JSON-01..05
- `jsonInsertColumns` parsing and insert behavior — COLS-01..06
- MBean naming and per-task registration — JMX-01..04
- `async_insert` left to the user (upstream's forced `async_insert=0` is commented out) — ASYNC-01..03

That is 18 requirements across three plans, all wave 1 (04-01 JSON, 04-02 COLS, 04-03 JMX + ASYNC).

Like Phase 3, this phase has **no `transforms/`-style safe zone** — every target is main code in
`ClickHouseSinkConfig`, `ClickHouseWriter`, `ProxySinkTask`, and `ClickHouseSinkTask`. The fix budget is
therefore enumerated explicitly in D-01.

Out of this phase:

- `targetTableFilter`, `describeTable` sub-column skipping, `JSON`/`Dynamic` type mapping and RowBinary
  fail-fast — Phase 3, complete
- CI workflow changes and the final green run — Phase 5
- Validating `jsonInsertColumns` against the table schema, or any other completion of the WIP JSON
  feature — locked Out of Scope in PROJECT.md and REQUIREMENTS.md
- Refactoring `ClickHouseWriter` / `ClickHouseSinkConfig` — locked Out of Scope (rebasability)

</domain>

<decisions>
## Implementation Decisions

### Main-code fix scope (the escalation rule for a phase with no safe directory)

- **D-01:** **Exactly one main-code change is pre-approved: the `ProxySinkTask.buildMBeanName`
  extraction (D-02). Nothing else.** Phase 1 D-02 / Phase 2 D-04 / Phase 3 D-01 carry forward unchanged.
  Every other main-code edit stops for a user decision at a blocking checkpoint — explicitly including:

  - a fail-fast throw in the `insertFormat=json` + Struct + RowBinary routing path, even if R-01 proves
    silent data loss (D-07)
  - any `jsonInsertColumns` validation, normalization, or quoting (`ClickHouseSinkConfig`,
    `ClickHouseWriter`)
  - any change to `Utils.handleException`'s retriable/non-retriable classification (D-10)
  - making the String-record route honour `jsonInsertColumns` (see Deferred Ideas)

  Never disable or `@Disabled` a test to get green.

### JMX naming and registration (JMX-01..04)

- **D-02:** **Extract `buildMBeanName` from `ProxySinkTask`, extraction only.** Today
  `getMBeanName()` (`ProxySinkTask.java:71-79`) is `private`, reads
  `ClickHouseClientOption.class.getPackage().getImplementationVersion()` inline, and is only reachable
  through a constructor that requires a live ClickHouse (`dbWriter.start()` at `:63`). JMX-03's two
  branches (`null` → `unknown`, `:` → `_`) can therefore never execute in a test: the real client jar
  reports a plain version like `0.9.6`.

  Add `static String buildMBeanName(String connectorName, int id, String version)` (package-private)
  holding today's null/colon logic and the exact format string; `getMBeanName()` becomes a one-line
  delegate that passes the package version. **Output identical byte-for-byte.** Nothing else in
  `ProxySinkTask` or `MBeanServerUtils` changes — no version-injection overload, no test accessors for
  `id`/`statistics`.

  Rejected: reflection on the private method (still cannot inject a version — it is read *inside* the
  method, so JMX-03 stays uncoverable); live-only coverage with JMX-03 marked partial.

- **D-03:** **JMX-01, JMX-02 and JMX-03 are Docker-free unit tests on the helper.** Connector name
  present → `connector=<name>`; connector name `null` → the literal `connector=null` (pinned — it is
  `String.format` interpolating a null, and `ClickHouseSinkTask.start()` does a plain
  `props.get("name")` at `:36`); version `null` → `version=unknown`; version containing `:` → `_`.

- **D-04:** **JMX-04 is a live registration test only — no overlapping unit test.** `ProxySinkTask.id`
  comes from a static `NEXT_ID` counter (`:31`, `:40`) that never resets inside a test JVM, so exact
  names like `SinkTask0` are unassertable. Start two `ClickHouseSinkTask`s under different `name` props
  plus two tasks under the same name, query the platform MBean server for
  `com.clickhouse:type=ClickHouseKafkaConnector,*`, **filter to this test's own connector names**, assert
  distinctness, then `stop()` one and assert exactly that one disappeared while the others survive.
  This is the only way to exercise `MBeanServerUtils.registerMBean`/`unregisterMBean` themselves.

- **D-05:** **Plain connector names only.** No coverage of ObjectName-hostile names. See Deferred Ideas
  for the silent-degradation behavior that was surfaced and consciously left untested.

### Explicit JSON insert path (JSON-01..05)

- **D-06:** **Schemaless-first.** JSON-01..04 are pinned on **schemaless `Map` records**, because that is
  the only route where metadata-free mode actually changes behavior: `getTable` returns a synthetic
  column-less `Table` (`ClickHouseWriter.java:1241-1245`), `cleanupExtraFields` returns the map untouched
  (`:1039-1042`), and `jsonInsertColumns` is applied at `:1031-1032`. The two existing tests in
  `ClickHouseSinkTaskJsonInsertFormatTest` use **String** records, which go to `doInsertStringV2`
  (`:1127-1200`) — a raw passthrough that never touches either mechanism. JSON-05 upgrades those two
  existing tests from `countRows` to real typed column-value assertions (Phase 3 D-14 carried forward);
  that file is fork-only (verified: it does not exist on `main`), so editing it does not violate Phase 3
  D-16.

  Rejected: both routes (roughly doubles plan 04-01's live tests for coverage that is mostly
  `doInsertStringV2`'s format switch); String-record-first (leaves JSON-02/03's actual subject untested).

- **D-07:** **The `insertFormat=json` + Struct + no-`bypassRowBinary` combination is probed before it is
  decided (R-01).** `doInsert` (`:208-222`) routes SCHEMA records to `doInsertRawBinary` unless
  `bypassRowBinary=true` — against the column-less `Table` metadata-free mode produced. Nobody has run
  that combination. Research establishes ground truth (silent 0 rows? empty rows? server error?). If it
  already fails clearly, it becomes a pin. If it silently loses data, the fail-fast fix (Phase 3 TYPE-06's
  shape) **escalates under D-01 — it is not pre-approved**, because it changes behavior for a config
  combination that may run in production today.
  — **Reversibility:** costly — a fail-fast throw would break any pipeline currently running
  `insertFormat=json` with a Struct converter, whatever it does today.

- **D-08:** **JSON-03's `cleanupExtraFields` bypass is pinned Docker-free, not live.** The connector
  always adds `input_format_skip_unknown_fields=1` (`ClickHouseSinkConfig.java:207`), so a live insert
  cannot distinguish "the connector stripped the key" from "the server ignored it". Use the **existing**
  `protected void setSinkConfig(ClickHouseSinkConfig)` seam (`ClickHouseWriter.java:81-82`) — no new seam
  needed — and assert: with `insertFormat=json` the same map comes back untouched; with default config
  unknown keys are stripped. The live half then asserts only that known fields land with correct values.

  Rejected: the `input_format_skip_unknown_fields=0` live differential (pins a server error path and
  couples the test to a ClickHouse setting for evidence the unit test gives directly).

- **D-09:** **JSON-04 covers a mixed batch and an all-tombstone batch.** A tombstone converts via
  `EmptyRecordConvertor` to a `SCHEMA_LESS` record with a null value, which `doInsertJsonV2` skips with a
  WARN (`:1023-1025`). Mixed batch: tombstones skipped, normal rows land with correct values, no error.
  All-tombstone batch: `client.insert(...)` still runs with an **empty** JSONEachRow stream — the shape a
  real Debezium delete burst produces, untested today. Pin whatever it does; if it throws, that is
  ground truth worth knowing, and any fix escalates under D-01.

### `jsonInsertColumns` (COLS-01..06)

- **D-10:** **COLS-05 pins the server-side error, including its classification.** The list is passed
  straight to `client.insert(table, columns, ...)`, so a nonexistent column fails inside ClickHouse. The
  live test asserts: `put()` throws, the offending column name appears somewhere in the message/cause
  chain, `countRows == 0` (no partial insert), no NPE — **and records which exception type the connector
  surfaces** (retriable vs task-failing), so the classification itself is pinned. If it turns out to be
  retriable — a typo'd column name retrying forever in production — that is an escalation under D-01,
  not an autonomous fix, because it lives in upstream-shared `Utils.handleException`.

  Rejected: pre-validating the list against the table schema (explicitly Out of Scope in PROJECT.md, and
  metadata-free mode has no schema to validate against).

- **D-11:** **COLS-02's research flag is downgraded to a note.** The roadmap flagged "check how
  client-v2 `0.9.6` handles an empty column list" before writing COLS-02. Phase 3's TYPE-03..05
  round-trips already drove `doInsertJsonV2` — which passes `csc.getJsonInsertColumns()` unconditionally
  — with the default empty list, and they are green. COLS-02 is therefore a plain live test: default
  empty list → every column gets correct values, asserted by typed read-back. If it fails, that is a
  genuine bug and escalates under D-01.

- **D-12:** **COLS-03's fixture uses explicit `DEFAULT` expressions.** With plain columns, "unlisted
  columns receive their `DEFAULT`" is indistinguishable from "the column was never written" — both read
  `0`/`''`. Give the unlisted columns non-trivial defaults (e.g. `extra Int32 DEFAULT 42`,
  `tag String DEFAULT 'unset'`) so reading back `42`/`unset` proves ClickHouse applied the DEFAULT.

- **D-13:** **COLS-06 also pins the column-clause-vs-cleanup interaction.** In the non-explicit path
  `cleanupExtraFields` runs and keeps every key that exists as a table column, so a payload key that is a
  real column but sits *outside* the configured list is still serialized into the JSONEachRow body — only
  the INSERT column clause excludes it. Assert, with default config and schemaless records: the
  configured subset is filled, unlisted columns keep their `DEFAULT`, **and** that valid-column-outside-
  the-list key does not land. That is the one genuinely surprising interaction in this requirement.

### `async_insert` (ASYNC-01..03)

- **D-14:** **ASYNC-01 and ASYNC-02 are Docker-free config unit tests.** The fork's entire change is one
  commented-out line (`ClickHouseSinkConfig.java:209`; upstream sets `async_insert=0`). Assert
  `new ClickHouseSinkConfig(props).getClickhouseSettings()` has no `async_insert` key by default, and
  that `clickhouseSettings=async_insert=1` round-trips to exactly `1`. Both insert paths copy every entry
  of that map into their settings (`:1147-1149` for V2, `:1233-1235` for V1), so the config-level
  assertion pins the fork's actual change.

  Rejected: a `system.query_log` wire-level check (depends on query_log flush timing and query-id
  plumbing — more moving parts, slower, flakier, for a map-copy that is already straight-line code).

- **D-15:** **ASYNC-03 runs with `async_insert=1,wait_for_async_insert=1`.** With `async_insert=1` alone
  the server acknowledges before the data is queryable, so a `countRows` immediately after `put()` can
  legitimately read 0. `wait_for_async_insert=1` makes the insert return only once the buffer is flushed,
  which keeps the assertion deterministic with no polling loop — and doubles as a second ASYNC-02
  pass-through case (two settings, both forwarded unchanged).

### Test organization

- **D-16:** **One new fork-only test class per concern, each owned by exactly one plan.** The three plans
  run in one wave, so no file may be written by two of them. Docker-free classes: MBean-name helper
  (04-03), `ClickHouseSinkConfig` parsing for COLS-01 (04-02), writer `cleanupExtraFields` bypass
  (04-01); plus one ClickHouse-backed class per plan. The single exception is
  `ClickHouseSinkTaskJsonInsertFormatTest`, which 04-01 edits for JSON-05 (fork-only file, D-06).

  Docker-free classes must **not** extend `ClickHouseBase` — the `@Tag("clickhouse")` is inherited and
  would exclude them from `testUnit` (Phase 1 D-12). `RowBinaryUnsupportedTypeGateTest` is the working
  precedent from Phase 3.

- **D-17:** **Prior test conventions carry forward unchanged:** `REQ-ID: ` prefix in every
  `@DisplayName` (Phase 2 D-10), `@ParameterizedTest` for genuine matrices only (Phase 2 D-11), negative
  tests assert exception type plus a **substring** of the message (Phase 2 D-16), typed SQL read-back
  rather than `countRows` alone (Phase 3 D-14), tables created **before** `start()`, try/finally around
  `start()`/`stop()`, `uniqueConnectorName(TestInfo)` (Phase 1 D-08/D-10). No `build.gradle.kts` change:
  `ProxySinkTask`, `ClickHouseSinkConfig`, `ClickHouseSinkTask`, `ClickHouseWriter` are all already in the
  Phase 1 D-13 `jacocoForkReport` include list.

### Research flags

| Flag | Question | Blocks | Decided in |
|------|----------|--------|------------|
| **R-01** | What actually happens with `insertFormat=json` + a Struct record + `bypassRowBinary` unset (RowBinary against a column-less `Table`)? Silent 0 rows, empty rows inserted, or a server error? | JSON-01..05 scope; whether a fail-fast fix gets escalated | D-07 |
| ~~COLS-02 empty-list probe~~ | ~~How client-v2 `0.9.6` handles an empty column list~~ | — | **Withdrawn** by D-11 (Phase 3's green round-trips already exercise it) |

### Claude's Discretion

- Names and packages of the new fork-only test classes, and exactly how they map onto 04-01 / 04-02 / 04-03.
- Signature details of `buildMBeanName` beyond "package-private static, takes connector name + id +
  version, output byte-for-byte identical to today".
- Live fixture table shapes, column types, record counts, and whether tests within a class share a table.
- Which COLS-01 parsing inputs get parameterized (blank, whitespace-only entries, empty entries between
  commas, leading/trailing spaces, order preservation).
- Which JSON-01 letter-case variants get parameterized (`json`, `JSON`, `Json`, `  json  `) and how the
  negative half is expressed (unset, `none`, `csv`).
- Exact `@DisplayName` phrasing beyond the `REQ-ID: ` prefix.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and requirements

- `.planning/ROADMAP.md` § "Phase 4: JSON Insert Path & Connector Plumbing" — goal, the 5 success
  criteria, the (now withdrawn) COLS-02 research flag, and the plan split 04-01 / 04-02 / 04-03, all wave 1
- `.planning/REQUIREMENTS.md` § "Explicit JSON Insert Path" (JSON-01..05), § "jsonInsertColumns"
  (COLS-01..06), § "Connector Plumbing — JMX" (JMX-01..04), § "Connector Plumbing — async_insert"
  (ASYNC-01..03), plus § Out of Scope
- `.planning/PROJECT.md` § Constraints, § Key Decisions, § Context — behavior preservation, rebasability,
  "test WIP JSON-path features as-is", "`async_insert=0` intentionally not forced"

### Prior phase decisions that bind this one

- `.planning/phases/03-table-metadata-json-dynamic-types/03-CONTEXT.md` — D-01 (the enumerated fix-budget
  pattern this phase's D-01 mirrors), D-14 (typed read-back, never `countRows`), D-16 (new fork-only test
  classes), D-17 (test conventions), D-13 (the two JSON insert routes Phase 3 pinned, which D-11 relies on)
- `.planning/phases/02-fork-smt-coverage/02-CONTEXT.md` — D-04 (escalation), D-10/D-11/D-16 (test
  conventions carried forward in D-17)
- `.planning/phases/01-test-infrastructure-green-baseline/01-CONTEXT.md` — D-08 (fresh database per test
  class), D-10 (`uniqueConnectorName`), D-12 (`@Tag("clickhouse")` inherited from `ClickHouseBase` —
  Docker-free classes must not extend it), D-13 (the `jacocoForkReport` include list already covers every
  class this phase touches)
- `.planning/STATE.md` § Blockers/Concerns — run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`;
  colima profile `vz`; worktree isolation degrades to sequential while `custom_build` has unpushed commits

### Research (per-feature test-case inventories and pitfalls)

- `.planning/research/FEATURES.md` — per-feature test-case inventory with source line references for the
  explicit JSON insert path, `jsonInsertColumns`, MBean naming, and `async_insert`
- `.planning/research/PITFALLS.md` — Pitfall 5 (MBean leak / no try/finally) is directly load-bearing for
  the JMX-04 live test; Pitfall 7 (`windows-1252` test JVM)

### Codebase maps

- `.planning/codebase/TESTING.md` — test framework and setup/teardown patterns (its "no coverage plugin"
  section is stale as of Phase 1)
- `.planning/codebase/ARCHITECTURE.md` — the `ClickHouseSinkTask` → `ProxySinkTask` → `Processing` →
  `ClickHouseWriter` chain the JMX and JSON tests drive end to end

### Source under test

- `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` — `registerMBean` call `:68`,
  `getMBeanName()` `:71-79`, `stop()` `:81-83`, the static `NEXT_ID` counter `:31`/`:40`
- `src/main/java/com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.java` — `registerMBean` `:17-28`
  (swallows `MalformedObjectNameException` with a WARN), `unregisterMBean` `:30-41`
- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java` — `connectorName = props.get("name")` `:36`
- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` — `clickhouseSettings`
  parsing `:195-206`, the forced-setting block with the commented-out `async_insert` `:207-211`,
  `insertFormat` + `jsonInsertFormatExplicit` + `jsonInsertColumns` parsing `:225-252`
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` — `setSinkConfig` seam
  `:81-82`, metadata-free `start()` short-circuit `:136-139`, `updateMapping` short-circuit `:150-158`,
  `doInsert` routing `:196-223`, `doInsertJsonV2` `:963-1037` (the `jsonInsertColumns` insert call
  `:1031-1032`), `cleanupExtraFields` `:1039-1051`, `doInsertStringV2` `:1127-1200` (insert call `:1195`,
  no column list), `getTable` `:1241-1266`, `shouldSkipTableMetadata` `:1268-1270`
- `src/main/java/com/clickhouse/kafka/connect/sink/data/convert/EmptyRecordConvertor.java` — tombstones
  become `SCHEMA_LESS` records with a null value (JSON-04)

### Existing tests to build on or edit

- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java` — the two
  existing explicit-JSON tests (String records, `countRows` only); edited by 04-01 for JSON-05
- `src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java` — Phase 3's
  Docker-free precedent: a bare `new ClickHouseWriter()` calling `protected` methods, no `ClickHouseBase`
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonDynamicRoundTripTest.java` — Phase 3's typed
  read-back helpers and the green empty-`jsonInsertColumns` evidence D-11 relies on
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` — `createProps()`, `createClient()`,
  `createTopicName()`, `uniqueConnectorName(TestInfo)`, the `@AfterEach` MBean leak sweep `:84-97`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- `ClickHouseBase` — fresh database per test class, shared container per JVM fork, `createClient(props)`,
  `uniqueConnectorName(TestInfo)`, and the `@AfterEach` sweep that unregisters any leaked
  `com.clickhouse:type=ClickHouseKafkaConnector,*` MBean. **The JMX-04 test queries the same MBean server
  that sweep uses**, so it must filter by its own connector names rather than counting globally.
- `ClickHouseTestHelpers` — `query`, `createTable`, `dropTable`, `countRows`, UTF-8-safe readers, and the
  `newDescriptor(...)` builders.
- `ClickHouseWriter.setSinkConfig(ClickHouseSinkConfig)` (`:81-82`) — an **already existing** protected
  seam that lets a Docker-free test drive `cleanupExtraFields` with an arbitrary config (D-08). No new
  main-code seam is needed for JSON-03.
- `RowBinaryUnsupportedTypeGateTest` — Phase 3's template for a Docker-free test that pokes `protected`
  `ClickHouseWriter` methods on a bare instance.

### Established Patterns

- Unit vs ClickHouse-backed is split by **inheritance**, not directory: extending `ClickHouseBase`
  inherits `@Tag("clickhouse")` and drops out of `testUnit`.
- ClickHouse-backed flow: build props → `createClient` → create table → build records → `new
  ClickHouseSinkTask()` → `start(props)` → `put(records)` → `stop()` in `finally` → assert by typed query.
  The table must exist before `start()` unless `insertFormat=json` is set — metadata-free mode is exactly
  what makes `explicitJsonInsertSkipsTableCheckOnStart` legal today.
- `doInsert` picks its route from `records.get(0).getSchemaType()`, so the **first** record decides how
  the whole batch is written.

### Integration Points

- `ProxySinkTask` — one extraction (D-02). The only pre-approved main-code edit in this phase.
- `ClickHouseWriter` — read-only for tests: `cleanupExtraFields` via the existing `setSinkConfig` seam,
  plus the live routes. Any edit escalates (D-01).
- `ClickHouseSinkConfig` — read-only: COLS-01 and ASYNC-01/02 assert on constructor output only.
- `ClickHouseSinkTaskJsonInsertFormatTest` — the one existing test file this phase edits (JSON-05).
- No `build.gradle.kts` change.

</code_context>

<specifics>
## Specific Ideas

- **The String route never sees `jsonInsertColumns`.** `doInsertStringV2` calls
  `client.insert(table, data, format, settings)` (`:1195`) with no column list, while `doInsertJsonV2`
  calls the 5-arg overload with `csc.getJsonInsertColumns()` (`:1031-1032`). The existing explicit-JSON
  tests are String-record tests, which is precisely why they could pass while `jsonInsertColumns` stayed
  entirely uncovered. This is the single most load-bearing fact behind D-06.
- **`input_format_skip_unknown_fields=1` is what makes JSON-03 untestable live.** The connector adds it
  unconditionally (`ClickHouseSinkConfig.java:207`), so the server tolerates unknown keys whether or not
  the connector stripped them. D-08 exists because of that line.
- **COLS-03 must use non-trivial `DEFAULT`s or it asserts nothing** — an unwritten `Int32` and a
  `DEFAULT 0` `Int32` read back identically.
- **JMX-04 cannot assert absolute MBean names.** The static `NEXT_ID` counter keeps incrementing across
  every test in the JVM fork, so assertions must be about distinctness and about which name disappeared
  after `stop()`, never about `SinkTask0`.
- **ASYNC-03 without `wait_for_async_insert=1` is a coin flip**, not a test.
- The upstream `props.getOrDefault(INSERT_FORMAT, "node")` typo (`:225`) means an *unset* `insertFormat`
  already falls through to `InsertFormats.JSON` — but `jsonInsertFormatExplicit` stays `false`, which is
  exactly what JSON-01's negative half must assert. The typo is upstream (confirmed against `main`), not
  a fork addition, so it is pinned by implication and not fixed here.

</specifics>

<deferred>
## Deferred Ideas

Surfaced during discussion, not decided — kept so they are not lost:

- **`MBeanServerUtils.registerMBean` swallows `MalformedObjectNameException`** (`:17-28`), so a connector
  named `cdc,prod` or `a=b` runs perfectly while silently publishing no JMX metrics. Declined under D-05
  (plain names only). A future fix would quote the connector name via `ObjectName.quote`, which changes
  MBean names operators may already scrape — needs its own decision.
- **The String-record route silently ignores `jsonInsertColumns`** — an operator combining
  `insertFormat=json` with a `StringConverter` and a column list gets no error and no column clause.
  Surfaced by D-06's analysis; not pinned in Phase 4 and explicitly not fixed (D-01).
- **Metadata-free `getTable` bypasses `Utils.getTableName`** (`:1243`), so the db-topic-split path
  (`enableDbTopicSplit`) is skipped entirely in explicit JSON mode. Noted, untested.
- **Wire-level verification that no `async_insert` reaches the server** via `system.query_log` —
  declined under D-14 in favour of the config-level assertion. Revisit if the settings-copy code ever
  becomes conditional.
- **COLS-01 parsing edges beyond the requirement** — duplicate entries, backticked or quoted column
  names, names with spaces. Left to Claude's discretion whether any are worth a parameterized case;
  none are required.

</deferred>

---

*Phase: 04-json-insert-path-connector-plumbing*
*Context gathered: 2026-09-17*
