# Phase 3: Table Metadata & JSON/Dynamic Types - Research

**Researched:** 2026-09-17
**Domain:** ClickHouse table-metadata discovery (`describeTable`, `targetTableFilter`) and `JSON`/`Dynamic` column type mapping + RowBinary serialization in a Kafka Connect sink fork
**Confidence:** HIGH (both research flags resolved via live ClickHouse `latest` probing and one live code-execution probe, not source reasoning alone)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Main-code fix scope (D-01):** Five main-code changes are pre-approved, and only these five:
1. `ClickHouseWriter.doWriteColValue()` — throw in the `default:` branch (D-02, D-03, D-05)
2. `ClickHouseWriter.validateDataSchema()` — same message for `JSON`/`DYNAMIC`/`UNKNOWN` columns (D-04)
3. `ClickHouseHelperClient` — extract `shouldSkipDescribeRow(...)`, apply the structural skip rule, and change the unmappable-column log from `warn` to `error` (D-06, D-07, D-08)
4. `ClickHouseHelperClient` — extract the `match()` clause builder and escape backslashes (D-09, D-11)
5. `ClickHouseHelperClient` lines 370 and 417 — explicit `StandardCharsets.UTF_8` on the `describeTable` readers, **if** D-14's identifier test proves the bug

Anything beyond this list stops for a user decision. Never disable or `@Disabled` a test to get green.

**RowBinary fail-fast (TYPE-06):**
- D-02: Throw in `doWriteColValue()`'s `default:` branch, replacing the log-only line, with a `DataException`.
- D-03: The throw covers every type reaching the branch (including `Type.UNKNOWN`), not just `JSON`/`DYNAMIC`.
- D-04: `validateDataSchema()` speaks the same error (a `JSON`/`DYNAMIC`/`UNKNOWN` check emitting the D-05 message) — **research note: this project's own text already predicted validateDataSchema fires first under default config; R-02 below confirms that by live run, not just source reading, so D-04 is REQUIRED, not optional.**
- D-05: Message names the column, its type, and both escape hatches — `insertFormat=json` and `bypassRowBinary=true`. Tests assert a stable substring only.
- D-06: TYPE-06 pinned at two levels — Docker-free unit tests on the two `protected` gates directly, plus one ClickHouse-backed test through `ClickHouseSinkTask.put()` asserting insert failure **and** `countRows == 0`.

**`describeTable` sub-column handling (DESC-01..04):**
- D-07: Structural skip rule by parent type — skip any row whose parent column resolves to `Type.JSON` or `Type.DYNAMIC`. `.null`/`.size0` stay for `Nullable`/`Array`. Map/Tuple sub-columns must keep flowing through. Fix the un-guarded, trailing-dot-requiring `.object_shared_data.` clause while in there.
- D-08: One private helper (`shouldSkipDescribeRow(ClickHouseFieldDescriptor, Table)`), called from both `describeTableV1` and `describeTableV2`, directly unit-testable with synthesized descriptors.
- D-09: DESC-04 pinned behaviorally (drop to `null`); log level flip `warn`→`error` unasserted. `Column.extractColumn` returns `null` only for `Nested`/`Array(Nested(...))` — the DESC-04 fixture must use a `Nested` column.

**`targetTableFilter` (FILT-01..04):**
- D-10: Extract `buildTableFilterClause(String filter)` from the ternary at `ClickHouseHelperClient.java:297`.
- D-11: Escape backslashes as well as quotes — **research note: R-03 below falsifies this premise by live query; see Pitfall/Open Question section. Confirm with the user before applying.**
- D-12: FILT-02 splits in two — (a) filter matches nothing → `start()` throws regardless of `suppressTableExistenceException`; (b) filter matches some tables but not the target → `suppress` flag governs silent-drop vs. throw in `getTable()`. Update FILT-02's wording in REQUIREMENTS.md.

**`JSON`/`Dynamic` round-trips and non-ASCII (TYPE-01..05, TYPE-07):**
- D-13: Two routes pin TYPE-03/04/05 — schemaless `JSONEachRow` and `bypassRowBinary` Struct records. Not explicit `insertFormat=json` (Phase 4).
- D-14: Typed SQL read-back, not row counts — nested path access for `JSON` (`json_col.a.b`), `toTypeName()`/`dynamicType()` for `Dynamic`.
- D-15: TYPE-07 covers non-ASCII identifiers (column names, Enum members) as well as values, because the deferred UTF-8 reader fix (Phase 1 D-11) only ever touches DESCRIBE output.

**Test organization:**
- D-16: All Phase 3 tests land in new fork-only test classes. `ClickHouseHelperClientTest`, `ColumnTest`, `ClickHouseSinkTaskMappingTest` stay untouched (upstream files).
- D-17: Phase 2's unit-test conventions carry forward (`REQ-ID: ` prefix, `@ParameterizedTest` for genuine matrices, substring assertions on exception causes).
- D-18: Non-ASCII test data built from Unicode code points at runtime, never `\u` escape literals in source.

### Claude's Discretion

- Names/packages of new fork-only test classes and how they map onto plans 03-01/03-02/03-03.
- Exact signature/visibility of `shouldSkipDescribeRow(...)` and `buildTableFilterClause(...)` — only that they are reachable from a Docker-free test.
- Exact wording of the D-05 error message beyond naming column, type, `insertFormat=json`, `bypassRowBinary=true`; and the exception type at each gate.
- Live-test table shapes, record counts, and whether tests share a table.
- Which TYPE-01/TYPE-02 type strings get parameterized (`JSON`, `JSON(max_dynamic_paths=...)`, `Dynamic`, `Dynamic(max_types=N)`, and `Nullable`/`LowCardinality`/`Array`/`Map` wrappers).
- Exact `@DisplayName` phrasing beyond the `REQ-ID: ` prefix.

### Deferred Ideas (OUT OF SCOPE)

- The misleading `"Connection to ClickHouse is not active."` message (`ProxySinkTask.java:65`) when the real cause is an empty/filtered-out mapping — declined under D-12, no requirement owns it.
- A pre-check scanning a table's columns for unsupported types before streaming any RowBinary bytes — declined under D-02.
- The log-capture test helper — declined again under D-09.
- `describeTableV2` ignoring its `database` parameter (`ClickHouseHelperClient.java:408`) — harmless today, out of D-01's fix list.
- `describeTableV1`/`V2` result parity — tracked as v2 requirement PAR-01.
- Explicit `insertFormat=json`, `jsonInsertColumns`, MBean naming, `async_insert` — Phase 4.
- CI workflow changes — Phase 5.
- RowBinary **serialization** for `JSON`/`Dynamic` — locked Out of Scope; this phase produces a clear error instead.
- Auto-routing tables with `JSON`/`Dynamic` columns to the JSON path — locked Out of Scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| FILT-01 | Empty filter → no `match()` clause, all tables mapped | `buildTableFilterClause("")` returns `""` per current ternary at `ClickHouseHelperClient.java:297`; verified live in a fresh per-class database (Phase 1 D-08) |
| FILT-02 | Filter matching nothing → empty mapping + `suppressTableExistenceException` behavior (wording corrected per D-12) | Live-traced: `start()` throws `RuntimeException("Connection to ClickHouse is not active.")` at `ProxySinkTask.java:65` regardless of the suppress flag when mapping is empty; the flag only matters in `getTable()` (`ClickHouseWriter.java:1226-1231`) when mapping is non-empty |
| FILT-03 | Single-quote escaping produces a valid query (unit + live) | Current escaping confirmed at `ClickHouseHelperClient.java:297` (`replace("'", "''")`); extraction target for `buildTableFilterClause` |
| FILT-04 | `\d`/anchors select intended tables live (fixed if broken) | **R-03 live probe REFUTES the "broken" premise** — see Common Pitfalls; `\d` and `^`/`$` anchors survive ClickHouse's single-quoted literal intact today |
| DESC-01 | `.null` subcolumns of `Nullable` skipped | Confirmed live: `nullable_col.null` (`is_subcolumn:1`) emitted and correctly matched by current `endsWith(".null")` |
| DESC-02 | `.size0` subcolumns of `Array` skipped | Confirmed live: `array_col.size0` (`is_subcolumn:1`) emitted and correctly matched by current `endsWith(".size0")` |
| DESC-03 | JSON internal subcolumns skipped; raw output compared to skip list | **R-01 live probe: no JSON/Dynamic-internal subcolumn rows appear in `DESCRIBE ... describe_include_subcolumns=1` on `latest` (26.8.6.5) at all** — but a previously-unknown bare `.size` subcolumn (distinct from `.size0`) appears broadly for `String`/`Nullable`/`Array`/`Tuple`/`Map` columns and is unhandled by the current skip list. See Common Pitfalls. |
| DESC-04 | Unmappable column drops table (pinned), logged at ERROR | Confirmed via source: `Column.extractColumn` returns `null` only for `Nested`/`Array(Nested(...))` (`Column.java:215-218`); existing `ignoreArrayWithNestedTable` test in `ClickHouseHelperClientTest.java:66-79` is the fixture template |
| TYPE-01 | `JSON`/`JSON(...)` → `Type.JSON` | Confirmed live: DESCRIBE returns `type:"JSON"` and `type:"JSON(max_dynamic_paths=10)"`; `Column.dispatchPrimitive` (`Column.java:152-153`) matches both via `startsWith("JSON")` |
| TYPE-02 | `Dynamic` → `Type.DYNAMIC` | Confirmed live: DESCRIBE returns `type:"Dynamic"`; `Column.dispatchPrimitive` (`Column.java:154-155`) matches via `startsWith("Dynamic")` |
| TYPE-03 | `JSON` column round-trips nested values | Confirmed live: `json_col.a.b` queryable and typed after insert (`toTypeName` = `Dynamic`, ClickHouse's own internal JSON leaf representation) — D-13's two insert routes (schemaless JSONEachRow, `bypassRowBinary` Struct) are untested today |
| TYPE-04 | `Dynamic` column round-trips value + type | Confirmed live: `dynamicType(dynamic_col)` correctly reports `Int64`/`String` per row after insert |
| TYPE-05 | Mixed `JSON`/`Dynamic` + ordinary columns round-trip | No blocker found in probing; same two insert routes apply |
| TYPE-06 | RowBinary path fails fast, no silent loss | **R-02 live-executed probe** (see below) — under default config, `validateDataSchema()` fires first and already fails (not silent); under `bypassSchemaValidation=true`, the writer gate's silent zero-byte write desyncs the stream and ClickHouse's server rejects it with a cryptic `Code: 32 ATTEMPT_TO_READ_AFTER_EOF`, not a clear `insertFormat=json` message |
| TYPE-07 | Non-ASCII round-trips (String/JSON/Dynamic/headers) + identifiers | Deferred UTF-8 reader fix location confirmed at `ClickHouseHelperClient.java:370` and `:417` (both unguarded `InputStreamReader`) |
</phase_requirements>

## Summary

This phase touches exactly three files (`ClickHouseHelperClient`, `ClickHouseWriter`, and transitively `Column`/`Table`/`Type`, which need no changes) and is scoped by an explicit five-item fix list rather than a directory, because there is no `transforms/`-style safe zone. Both roadmap research flags were resolved by direct execution against a live ClickHouse `latest` container (image `clickhouse/clickhouse-server:latest`, resolved version **26.8.6.5**) rather than by reasoning from source, per this phase's explicit instruction.

**R-01 (DESC-03 audit) delivered a bigger finding than the roadmap anticipated.** On ClickHouse `latest`, `DESCRIBE TABLE ... SETTINGS describe_include_subcolumns=1` never emits a JSON/Dynamic-internal path as a subcolumn row — the `.object_shared_data.` clause in the current skip list appears to target an older/different ClickHouse internal representation and is effectively dead code against `latest`. Meanwhile, a previously-undocumented, previously-unaudited **bare `.size` subcolumn** (distinct from the already-handled `.size0`) is emitted broadly for `String`, `Nullable(String)`, `Array`, `Tuple` string fields, and `Map` string keys — i.e., on almost every real-world table — and is **not** covered by the current skip list, D-07's JSON/Dynamic-only structural rule, or the deferred literal-suffix-list approach. It does not drop tables (unlike DESC-04's unmappable-column case) but produces WARN or ERROR log noise on effectively every `describeTable()` call. This must be surfaced to the plan explicitly since it sits outside D-07's stated scope.

**R-02 (TYPE-06 ground truth) was captured by actually running `ClickHouseSinkTask.put()`** against a live `JSON` column with a Struct-schema record, both under default config and under `bypassSchemaValidation=true`. Under default config, `validateDataSchema()` fires first (confirming D-04's own source-level prediction) and throws `RuntimeException("Data schema validation failed.")` wrapped in two more `RuntimeException`s — no silent data loss occurs today, but the message is useless. Forcing past that gate with `bypassSchemaValidation=true` reaches `doWriteColValue()`'s `default:` branch, which today writes zero bytes and desyncs the RowBinary stream; ClickHouse's server then rejects the corrupted stream with `Code: 32. DB::Exception: Attempt to read after eof: While executing WaitForAsyncInsert. (ATTEMPT_TO_READ_AFTER_EOF)` — a crash, but a cryptic one bearing no relation to the real cause. **This confirms D-02 through D-05 are the right fix** and gives an exact "before" message to contrast against in the TYPE-06 test's `@DisplayName`/comments.

**R-03 (FILT-04/D-11 backslash escaping) directly falsifies the "backslash gets eaten" premise.** A live `match(table, 'tbl_\d+')` query (single backslash, single-quoted SQL literal, exactly the shape `buildTableFilterClause` would produce) correctly matched `tbl_1`/`tbl_22` and excluded `tblx`; anchoring with `'^tbl_\d+$'` correctly excluded a table named `xtbl_1y` that contains `tbl_1` as a substring. ClickHouse's SQL string-literal parser does not treat `\d` as a recognized escape and passes it through intact to the RE2 `match()` engine. **D-11 should collapse to a pin, not a fix — flag this for the user per its own re-confirmation instruction before touching escaping logic.**

**Primary recommendation:** Implement D-01's five items in `ClickHouseHelperClient` and `ClickHouseWriter` exactly as scoped, but escalate two findings that fall outside the pre-approved fix list before locking the plan: (1) the newly discovered bare `.size` subcolumn noise across nearly all tables (DESC-03's audit obligation surfaced it; D-07's JSON/Dynamic-only rule does not cover it), and (2) D-11's backslash-escaping premise being empirically false (R-03 falsification). Both are exactly the kind of "research changes a locked decision" situations D-01/D-11 anticipated and pre-authorized a user check-in for.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| `targetTableFilter` clause construction (SQL generation) | Database / Storage (query-building code, but running inside the connector process) | API / Backend (`ClickHouseHelperClient` is part of the sink's backend layer) | Builds a `system.columns` filter clause that is sent to ClickHouse; correctness/injection-safety is enforced entirely in `ClickHouseHelperClient`, not delegated to a query builder or ORM |
| `describeTable` schema discovery | Database / Storage | API / Backend | Reads ClickHouse's live schema (`DESCRIBE TABLE`) and converts it into the connector's internal `Table`/`Column` model; this is metadata plumbing between ClickHouse and the connector, no client/browser tier involved |
| `JSON`/`Dynamic` type mapping (`Column.dispatchPrimitive`) | API / Backend | — | Pure in-process type-string-to-enum mapping; no I/O |
| `JSON`/`Dynamic` insert round-trip (JSONEachRow / `bypassRowBinary`) | API / Backend | Database / Storage | The connector serializes Kafka Connect records to JSON text and ships them via ClickHouse's JSON insert path; ClickHouse itself does the JSON→typed-column materialization |
| RowBinary fail-fast for `JSON`/`Dynamic` | API / Backend | — | `ClickHouseWriter` is the sole owner of RowBinary serialization; the fix is entirely local to two methods in this class |

This phase has no Browser/Client or CDN/Static tier involvement — it is exclusively backend connector code talking to ClickHouse's Database/Storage tier. There is no risk of tier misassignment here (unlike a full-stack phase); the map is included primarily to confirm that observation for the plan-checker.

## Standard Stack

No new libraries are introduced by this phase. All work is against the existing stack already locked in `.planning/phases/01-test-infrastructure-green-baseline/`:

### Core (unchanged, reused)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| ClickHouse Client (client-v2) | 0.9.6 | HTTP-based ClickHouse client used for both `DESCRIBE`/`SELECT` metadata queries and RowBinary/JSON inserts | Already the fork's only supported client per project constraints (`client V2` only) |
| TestContainers `clickhouse` module | 1.21.4 | Live ClickHouse `latest` container for sink-level tests | Already wired via `ClickHouseBase` |
| JUnit 5 (Jupiter) | 5.11.3 | Unit and sink-level test framework | Existing convention (D-17) |

No `build.gradle.kts` change is required: all five affected classes (`ClickHouseHelperClient`, `ClickHouseWriter`, `Column`, `Table`, `Type`) are already in the Phase 1 D-13 `jacocoForkReport` include list — verified live by grep: `"com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.class"` and `"com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.class"` are present `[VERIFIED: build.gradle.kts:256-265]`.

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Hand-rolled `DESCRIBE ... describe_include_subcolumns` parsing (current fork approach) | `com.clickhouse.client.api.internal.TableSchemaParser#readTSKV` from the ClickHouse Java client, reusing `ClickHouseColumn`'s battle-tested type parsing (proposed in upstream issue [ClickHouse/clickhouse-kafka-connect#622](https://github.com/ClickHouse/clickhouse-kafka-connect/issues/622)) `[CITED: github.com/ClickHouse/clickhouse-kafka-connect/issues/622]` | A real simplification, but it is a wholesale refactor of the parsing approach — explicitly Out of Scope per PROJECT.md ("Refactoring `ClickHouseWriter`, `Processing`, `ClickHouseSinkConfig`... hurts rebasability"). Noted here as forward context only; do not attempt in this phase. |

## Package Legitimacy Audit

Not applicable — this phase adds no new external dependencies (no `npm install`/`pip install`/`cargo add` equivalents). All work uses classes already present in `src/main` and `src/test`.

## Architecture Patterns

### System Architecture Diagram

```
Kafka Connect record (SinkRecord)
        │
        ▼
ClickHouseSinkTask.put()
        │
        ▼
ProxySinkTask ── batches by topic-partition ──▶ Processing.doLogic()
        │                                              │
        │ (needs table metadata)                       ▼
        ▼                                     ClickHouseWriter.doInsert()
ClickHouseWriter.getTable(db, topic)                    │
        │                                    ┌──────────┼───────────────┐
        │ mapping miss?                      │          │               │
        ▼                                    ▼          ▼               ▼
ClickHouseWriter.updateMapping()      SCHEMA type  SCHEMA_LESS type  STRING_SCHEMA
        │                             (bypassRowBinary?)  (JSON path)   (CSV/TSV/JSON)
        ▼                                 │    │
ClickHouseHelperClient.showTables()       │    └──▶ doInsertJson() ──▶ JSONEachRow ──▶ ClickHouse
   (targetTableFilter → match() clause)   ▼              (handles JSON/Dynamic columns fine)
        │                          doInsertRawBinary()
        ▼                                 │
ClickHouseHelperClient.extractTablesMapping()   ├─▶ validateDataSchema() ──▶ [today: generic RuntimeException
        │                                 │         (fires FIRST; R-02 confirmed)   for JSON/Dynamic — D-04 fix]
        ▼                                 ▼
ClickHouseHelperClient.describeTable()   doWriteColValue() per column
   (V1/V2, DESCRIBE ... describe_        │
    include_subcolumns=1)                └─▶ [today: default: branch silently
        │                                     writes 0 bytes for JSON/Dynamic/
        ▼                                     UNKNOWN — D-02/D-03 fix: throw
shouldSkipDescribeRow() (NEW, D-08)           DataException instead]
   skip .null/.size0/JSON+Dynamic-parent
   subcolumns (D-07) → Column.extractColumn()
        │
        ▼
Table.addColumn() ──▶ rootColumnsList / allColumnsMap
   (Nested → null drops table, DESC-04)
```

### Recommended Project Structure

No new production directories. New test files land alongside existing fork-only test conventions:

```
src/test/java/com/clickhouse/kafka/connect/sink/
├── db/helper/
│   ├── ClickHouseHelperClientTest.java          # upstream — DO NOT EDIT (D-16)
│   └── <new fork-only test class(es)>.java      # FILT-01..04, DESC-01..04 (Docker-free + live)
├── db/
│   ├── ClickHouseWriterTest.java                # upstream — DO NOT EDIT
│   └── <new fork-only test class(es)>.java      # TYPE-06 unit-level gate tests
└── <new fork-only test class(es)>.java          # TYPE-01..05, TYPE-07 sink-level round-trips
```

### Pattern 1: Docker-free unit test via synthesized `ClickHouseFieldDescriptor`

**What:** Build a `ClickHouseFieldDescriptor` directly with its `@Builder` (no live ClickHouse call) and pass it to the extracted `shouldSkipDescribeRow(...)` helper, or call `Column.extractColumn(fieldDescriptor)` directly.
**When to use:** FILT-01/03, DESC-01/02/03 (skip-rule logic), TYPE-01/02 (type-string mapping).
**Example (field shapes actually observed live, not invented):**
```java
// Source: this session's live probe against clickhouse/clickhouse-server:latest (26.8.6.5),
// via `DESCRIBE TABLE probe.t1 SETTINGS describe_include_subcolumns=1 FORMAT JSONEachRow`
ClickHouseFieldDescriptor nullSubcolumn = ClickHouseFieldDescriptor.builder()
        .name("nullable_col.null")
        .type("UInt8")
        .isSubcolumn(true)
        .build();
ClickHouseFieldDescriptor size0Subcolumn = ClickHouseFieldDescriptor.builder()
        .name("array_col.size0")
        .type("UInt64")
        .isSubcolumn(true)
        .build();
// NEW finding (not in current skip list) — see Common Pitfalls:
ClickHouseFieldDescriptor bareSizeSubcolumn = ClickHouseFieldDescriptor.builder()
        .name("plain_str.size")
        .type("UInt64")
        .isSubcolumn(true)
        .build();
```
`[VERIFIED: live probe, this session — raw JSON rows quoted in Common Pitfalls below]`

### Pattern 2: Sink-level typed read-back (D-14)

**What:** After `sinkTask.put(records)`, query ClickHouse directly for typed values rather than asserting only `countRows`.
**When to use:** TYPE-03/04/05.
**Example (queries actually run and verified live this session):**
```sql
-- Source: this session's live probe
SELECT id, json_col.a.b, toTypeName(json_col.a.b) FROM probe.t1 FORMAT JSONEachRow;
-- returns e.g. {"id":1,"json_col.a.b":1,"toTypeName(json_col.a.b)":"Dynamic"}

SELECT id, dynamicType(dynamic_col), dynamic_col FROM probe.t1 FORMAT JSONEachRow;
-- returns e.g. {"id":1,"dynamicType(dynamic_col)":"Int64","dynamic_col":42}
--              {"id":2,"dynamicType(dynamic_col)":"String","dynamic_col":"text_value"}
```
`[VERIFIED: live ClickHouse 26.8.6.5 probe, this session]`. Note: a `JSON` column's leaf path (`json_col.a.b`) types as ClickHouse's own `Dynamic` — this is expected JSON-type internal behavior, not a bug, and the test's `toTypeName` assertion should expect `Dynamic` for typed leaf paths of a plain (non-parameterized) `JSON` column.

### Anti-Patterns to Avoid

- **Extending the literal skip-suffix list for DESC-03:** The roadmap flag itself anticipates this ("Rejected: extending the literal suffix list — goes stale the next time ClickHouse changes JSON's internal layout"). R-01 proves this concern is broader than JSON: a brand-new bare `.size` suffix already exists on `latest` that isn't `.size0`, so a suffix list is provably incomplete today, not just fragile against future versions.
- **Assuming D-11's "backslash gets eaten" claim without re-verifying:** R-03 falsifies it on `latest`. Applying the escaping fix anyway, without the CONTEXT's own required user re-confirmation, risks changing working regex configs' semantics for no reason (reversibility for this item is explicitly flagged "costly" in D-11).
- **Reasoning about TYPE-06 from source only:** The actual failure mode (a cryptic ClickHouse-side `ATTEMPT_TO_READ_AFTER_EOF`, not the `doWriteColValue` log line itself) is only visible by running the insert; a test/plan built purely from reading `doWriteColValue` would miss that the *current* user-visible symptom is a ClickHouse protocol error, which is the real "unclear" behavior TYPE-06 must fix.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|--------------|-----|
| SQL-safe literal embedding for `targetTableFilter` | A bespoke escaping function beyond what's needed | The existing `.replace("'", "''")` pattern, extended only if a live falsification (not source reasoning) proves a real gap | R-03 shows the current single-quote-only escaping already handles regex metacharacters like `\d` correctly for ClickHouse's literal grammar; adding unneeded escaping is itself a source of new bugs |
| JSON/Dynamic type detection | A dedicated regex/parser for ClickHouse type strings | `Column.dispatchPrimitive`'s existing `startsWith("JSON")` / `startsWith("Dynamic")` checks (`Column.java:152-155`), already correct per live DESCRIBE output including `JSON(max_dynamic_paths=10)` | Already implemented and confirmed working; TYPE-01/02 are pin-only, no code change indicated |
| Detecting "this column can't be safely written to RowBinary" | A separate allow-list of "RowBinary-safe" types maintained alongside `Type` enum | D-03's single default-branch throw, which is exhaustive by construction (any `Type` not explicitly handled in the `switch` falls through) | One list to maintain (the `switch` cases themselves) instead of two lists that can drift |

**Key insight:** Both DESC-03 and TYPE-06 in this phase are exactly the domains where a second copy of ClickHouse's type/subcolumn knowledge (suffix lists, type-safety allow-lists) inevitably goes stale against ClickHouse version changes. The fork already has a working exhaustive mechanism in both cases (structural type check for DESC-03 per D-07; `switch` fallthrough for TYPE-06 per D-03) — the job is to route unhandled cases into a `default:`/fallback that fails safely, not to enumerate more literals.

## Common Pitfalls

### Pitfall 1: The `.object_shared_data.` skip clause targets behavior that does not exist on `latest`

**What goes wrong:** A plan built purely from reading `describeTableV1`/`V2`'s current skip condition might assume JSON internal sub-paths need a suffix-based skip rule that mirrors `.object_shared_data.`.
**Why it happens:** That clause was presumably written against an older ClickHouse version's JSON/Object-type internal representation. On `latest` (26.8.6.5), verified live:
```
$ DESCRIBE TABLE probe.t1 SETTINGS describe_include_subcolumns=1 FORMAT JSONEachRow
{"name":"json_col","type":"JSON", ..., "is_subcolumn":0}
{"name":"json_param_col","type":"JSON(max_dynamic_paths=10)", ..., "is_subcolumn":0}
{"name":"dynamic_col","type":"Dynamic", ..., "is_subcolumn":0}
```
— even after inserting rows with nested paths (`{"a": {"b": 1}, "c": "hello"}`) and running `OPTIMIZE TABLE ... FINAL`, **no** `json_col.*` or `dynamic_col.*` subcolumn rows are ever emitted by `DESCRIBE ... describe_include_subcolumns=1`. `system.parts_columns` also shows only the top-level `JSON`/`Dynamic` type for the on-disk part, not decomposed paths.
`[VERIFIED: live ClickHouse 26.8.6.5 probe, this session — raw JSON rows quoted above]`
**How to avoid:** D-07's structural skip rule (skip by resolved parent `Type.JSON`/`Type.DYNAMIC`) is still the right forward-compatible design, but the live-test half of DESC-03 cannot exercise it against real DESCRIBE output on `latest` — there is nothing to skip. The **unit test** with a synthesized `ClickHouseFieldDescriptor` (Pattern 1 above) is the only way to pin this rule's behavior; the live test should instead assert that `describeTable` on a `JSON`/`Dynamic` table returns a correct, non-null `Table` with the JSON/Dynamic columns as **root** columns (proving the current, correct behavior continues).
**Warning signs:** A live test asserting a specific skipped-subcolumn *count* for a JSON column will pass trivially (0 subcolumns) regardless of whether the skip logic is even reachable — this doesn't prove D-07's fix works. Prefer the unit test for the actual skip-rule assertion.

### Pitfall 2 (new finding, outside D-07's stated scope): a bare `.size` subcolumn is unhandled and appears on almost every real table

**What goes wrong:** `describeTable()` on `latest` emits a bare `.size` subcolumn (distinct from the already-handled `.size0`) for **plain `String`**, `Nullable(String)`, `Array` (at every depth, alongside `.size0`/`.size1`/etc.), `Tuple` fields that are `String`, and `Map` keys that are `Array(String)`. None of these are currently matched by the skip condition (`endsWith(".null")`, `endsWith(".size0")`, `contains(".object_shared_data.")`), so every one of them falls through into `Column.extractColumn` and then `Table.addColumn` → `handleNonRoot` → `updateParent`.
**Why it happens:** Verified live, minimal repro:
```
CREATE TABLE probe.t3 (id UInt32, plain_str String, ...) ...;
INSERT INTO probe.t3 VALUES (1, 'hi', ...); OPTIMIZE TABLE probe.t3 FINAL;
DESCRIBE TABLE probe.t3 SETTINGS describe_include_subcolumns=1 FORMAT JSONEachRow
```
returned (among others):
```
{"name":"plain_str.size","type":"UInt64", ..., "is_subcolumn":1}
{"name":"tuple_col.y.size","type":"UInt64", ..., "is_subcolumn":1}
{"name":"map_col.keys.size","type":"Array(UInt64)", ..., "is_subcolumn":1}
{"name":"nested_arr.size","type":"Array(Array(UInt64))", ..., "is_subcolumn":1}
```
`[VERIFIED: live ClickHouse 26.8.6.5 probe, this session — raw JSON rows quoted above]`. Tracing `Table.updateParent` for each: a `.size` child of a `STRING`-typed parent (e.g. `plain_str`, or `tuple_col.y`) hits the `default:` case (`Table.java:175-181`) and its `else` branch — `LOGGER.warn("Unsupported complex parent type: {}", parent.getType())` — because the name does not end in `.null`. A `.size` child of an `ARRAY`-typed parent whose element type is itself `STRING` (e.g. `map_col.keys.size`, `nested_arr.size`) hits the `ARRAY` case's inner `default:` (`Table.java:117-119`) — `LOGGER.error("Unhandled complex type '{}' as a child of an array", parentArrayType.getType())`. **Neither path drops the table** (unlike DESC-04's `Nested` case) — the column is registered in `allColumnsMap` only and silently excluded from `rootColumnsList`/tuple fields/map value type, which is actually the *correct* end result (the `.size` byte-length subcolumn should never be a real insertable column) — but it happens via noisy WARN/ERROR logging on essentially every `describeTable()` call for essentially every production table with any `String`-bearing column.
**How to avoid:** This is not covered by D-07's "skip by `Type.JSON`/`Type.DYNAMIC` parent" rule (the parent here is `STRING`/`ARRAY`, not `JSON`/`DYNAMIC`) and is not in D-01's five pre-approved fix items as written. **Flag this to the user before the plan locks** — it's real, live-verified, silently-correct-but-noisy behavior discovered exactly where DESC-03's audit obligation pointed, but it needs an explicit decision: (a) extend the structural skip rule to also match a bare `.size` suffix (`name.endsWith(".size")`, distinct from the digit-suffixed `.size0`/`.size1`/... already handled elsewhere), accepting this as within item 3's "apply the structural skip rule" wording, or (b) leave it as an acknowledged pre-existing WARN/ERROR-noise issue outside this phase's fix budget, and only add a test that pins the *current* silently-correct-but-noisy behavior (table still describes fine; no test currently asserts log level here per D-09's declined log-capture helper).
**Warning signs:** Any new DESC-03 test that runs a real DESCRIBE against a table with an ordinary `String` or `Array(String)` column and does not expect a WARN/ERROR log line will still pass (since the table isn't dropped) — this pitfall is invisible to result-only assertions and only shows up by reading logs or explicitly testing `shouldSkipDescribeRow` against this exact synthesized shape.

### Pitfall 3: D-11's backslash-escaping fix is based on a premise that live testing disproves

**What goes wrong:** Implementing D-11 (escape `\` before `'` in `buildTableFilterClause`) changes the semantics of any operator's existing `targetTableFilter` config that uses `\d`, `\w`, `\s`, etc. — but the premise that motivated the fix (ClickHouse "eating" the backslash) does not hold on `latest`.
**Why it happens:** Live falsification, three queries against tables `tbl_1`, `tbl_22`, `tblx`, `xtbl_1y`:
```sql
-- unanchored, single backslash, exact shape the current code already produces:
SELECT table FROM system.columns WHERE database='probe' AND match(table, 'tbl_\d+') GROUP BY table FORMAT JSONEachRow;
-- => {"table":"xtbl_1y"} {"table":"tbl_22"} {"table":"tbl_1"}   (matches, INCLUDING as a substring of xtbl_1y)

-- anchored:
SELECT table FROM system.columns WHERE database='probe' AND match(table, '^tbl_\d+$') GROUP BY table FORMAT JSONEachRow;
-- => {"table":"tbl_22"} {"table":"tbl_1"}   (correctly EXCLUDES xtbl_1y once anchored)
```
`[VERIFIED: live ClickHouse 26.8.6.5 probe, this session — raw JSON rows quoted above]`. If ClickHouse's SQL string-literal parser stripped the backslash from `\d` (turning it into literal `d`), the pattern `tbl_d+` would require a literal `d` character and would match **none** of these four table names — but it matched exactly the tables containing digits, both anchored and unanchored. This directly contradicts the D-11 claim that "`'^tbl_\d+$'` reaches `match()` as `^tbl_d+$`".
**How to avoid:** Per D-11's own text ("Confirm with R-03 before applying"), do not implement the backslash-escaping change without first surfacing this contradiction to the user. R-03 collapses FILT-04 to a **pin** (assert current, already-correct behavior with `\d` and anchors) rather than a **fix**.
**Warning signs:** If a plan writes a unit test asserting `buildTableFilterClause("tbl_\\d+")` produces a *doubled* backslash and then a live test that only checks "some tables matched" (not exact table identity, not an anchored-vs-unanchored contrast), it will not catch that the fix was unnecessary or that doubling the backslash could itself break the pattern (untested here — flagged as an open question below, not verified either way).

### Pitfall 4: TYPE-06's "silent" framing undersells the current failure — it's a crash, but a misleading one

**What goes wrong:** A test or plan built from `doWriteColValue`'s log line alone (`"Cannot serialize unsupported type {}"`) might assume the current behavior manifests as literal silent data loss (insert succeeds, wrong/missing data lands). Live execution shows the actual failure mode is more specific.
**Why it happens:** Live-executed probe (temporary throwaway JUnit test, deleted after use, no repo diff): a `JSON`-column table, a Struct-schema `SinkRecord` with a `json_col` field, inserted via `ClickHouseSinkTask.put()`:
- **Default config** (`bypassSchemaValidation=false`, `bypassRowBinary=false`): threw
  ```
  RuntimeException: Number of records: 1
    caused by RuntimeException: Topic: [...], Partition: [0], MinOffset: [0], MaxOffset: [0], (QueryId: [...])
    caused by RuntimeException: Data schema validation failed.
  ```
  `countRows` after the attempt: **0**. `[VERIFIED: live-executed probe, this session — see Sources]`. This confirms `validateDataSchema()` (`ClickHouseWriter.java:747`, quoted below) fires before `doWriteColValue` is ever reached, exactly as D-04's source-level reasoning predicted — and the insert already fails today, it just fails with an unhelpful generic message.
- **`bypassSchemaValidation=true`** (forcing past the writer gate to reach `doWriteColValue`): threw
  ```
  RuntimeException: Number of records: 1
    caused by RuntimeException: Topic: [...], Partition: [0], ...
    caused by com.clickhouse.client.api.ServerException: Code: 32. DB::Exception: Attempt to read after eof:
      While executing WaitForAsyncInsert. (ATTEMPT_TO_READ_AFTER_EOF) (version 26.8.6.5 (official build))
  ```
  `countRows` after the attempt: **0**. `[VERIFIED: live-executed probe, this session]`.
**How to avoid:** The test/plan should not assert "no exception is thrown today" (false) or "data silently lands with wrong values" (not what was observed in this single-JSON-column scenario). It should assert: today's failure is a **ClickHouse-side protocol error with no actionable message**; after D-02/D-03/D-04/D-05, the failure should instead be a **`DataException` naming the column, its type, and both `insertFormat=json`/`bypassRowBinary=true` escape hatches**, thrown from connector code before any bytes reach ClickHouse. Both "before" and "after" states are crashes, not silent — the improvement is message clarity and where in the stack the failure originates, not converting a silent success into a failure.
**Warning signs:** A test that only asserts `assertThrows(RuntimeException.class, ...)` without checking the message content would pass both before and after this phase's fix and would not actually pin the improvement.

## Code Examples

Verified patterns from source read this session (all line numbers and quoted text confirmed via `Read`):

### `showTables()`'s current filter-clause construction (extraction target for D-10/FILT-03)
```java
// Source: src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:297
String filterClause = targetTableFilter.isEmpty() ? "" : String.format(" AND match(table, '%s')", targetTableFilter.replace("'", "''"));
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:297]`

### `describeTableV1`/`V2`'s current skip condition (has two bugs — see D-07)
```java
// Source: src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:379 (V1), :426 (V2)
if (fieldDescriptor.isSubcolumn() && (fieldDescriptor.getName().endsWith(".null") || fieldDescriptor.getName().endsWith(".size0")) || fieldDescriptor.getName().contains(".object_shared_data.")) {
    LOGGER.debug("Skipping column {} as it is a system sub-column", fieldDescriptor.getName());
    continue;
}
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:379,426]` — operator precedence means the `.object_shared_data.` clause applies **regardless** of `isSubcolumn()`, and `endsWith`/`contains` mean a bare `plain_str.size` (Pitfall 2) is not caught by either branch.

### `doWriteColValue()`'s current unsupported-type branch (D-02/D-03 target)
```java
// Source: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:535-539
default:
    // If you wonder, how NESTED works in JDBC:
    // https://github.com/ClickHouse/clickhouse-java/blob/6cbbd8fe3f86ac26d12a95e0c2b964f3a3755fc9/clickhouse-data/src/main/java/com/clickhouse/data/format/ClickHouseRowBinaryProcessor.java#L159
    LOGGER.error("Cannot serialize unsupported type {}", columnType);
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:535-539]`

### `validateDataSchema()`'s call site that fires first today (D-04 target)
```java
// Source: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:747-748
if (!csc.isBypassSchemaValidation() && !validateDataSchema(table, first, false))
    throw new RuntimeException("Data schema validation failed.");
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:747-748]` — confirmed by live execution (Pitfall 4) to be the actual first failure point under default config.

### `Column.dispatchPrimitive`'s existing JSON/Dynamic mapping (TYPE-01/02 — already correct, pin only)
```java
// Source: src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java:152-156
} else if (valueType.startsWith("JSON")) {
    type = Type.JSON;
} else if (valueType.startsWith("Dynamic")) {
    type = Type.DYNAMIC;
}
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java:152-156]`

### `Type` enum (confirms `Type.JSON`/`Type.DYNAMIC` exist, no changes needed)
```java
// Source: src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Type.java:3-36
public enum Type {
    UNKNOWN, INT8, INT16, INT32, INT64, INT128, INT256, STRING, FLOAT32, FLOAT64, BOOLEAN,
    ARRAY, MAP, TUPLE, VARIANT, Date, Date32, DateTime, DateTime64, UUID,
    UINT8, UINT16, UINT32, UINT64, UINT128, UINT256, Decimal, FIXED_STRING, Enum8, Enum16,
    JSON, DYNAMIC
}
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Type.java:3-36]`

### FILT-02's actual failure path (empty mapping) — for the D-12 wording correction
```java
// Source: src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:65
throw new RuntimeException("Connection to ClickHouse is not active.");
```
```java
// Source: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:142-144
if (mapping.isEmpty()) {
    LOGGER.error("Did not find any tables in destination Please create before running.");
    return false;
}
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:65; src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:142-144]` — `start()` returning `false` here is what triggers `ProxySinkTask`'s `RuntimeException`, and it happens regardless of `suppressTableExistenceException`, which is only consulted in `getTable()`:
```java
// Source: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1226-1231
if (csc.isSuppressTableExistenceException()) {
    LOGGER.warn("Table [{}] does not exist, but error was suppressed.", tableName);
} else {
    LOGGER.error("Table [{}] does not exist - see docs for more details about table names and topic names.", tableName);
    throw new RuntimeException(String.format("Table %s does not exist", tableName));
}
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1226-1231]`

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|---------------|--------|
| Hand-parsed `DESCRIBE ... describe_include_subcolumns` output (this fork's current approach in `ClickHouseHelperClient`) | Upstream's client library already has `com.clickhouse.client.api.internal.TableSchemaParser#readTSKV` / `ClickHouseColumn`-based parsing that could replace this hand-rolled logic | Proposed, not yet done upstream (tracked as [ClickHouse/clickhouse-kafka-connect#622](https://github.com/ClickHouse/clickhouse-kafka-connect/issues/622)) `[CITED: github.com/ClickHouse/clickhouse-kafka-connect/issues/622]` | Out of scope for this phase (would be a wholesale refactor); worth flagging to the user as a future upstream-sync candidate, consistent with the "Deferred Ideas" pattern already used for `describeTableV1`/`V2` parity |

**Deprecated/outdated:** None specific to this phase's requirements — the `Type.JSON`/`Type.DYNAMIC` mapping and the general skip-list approach are both current-generation (not legacy) fork code; the finding is that the skip list is *incomplete against `latest`*, not that the approach itself is outdated.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Doubling the backslash (`\\d`) inside the single-quoted filter literal would *also* work correctly with ClickHouse's `match()` (only tested a single, un-doubled backslash live) | Pitfall 3 / Open Questions | If wrong, a plan that "fixes" D-11 by doubling backslashes could silently break `\d`-style patterns for the subset of operators who never worked around the (non-existent) bug — low likelihood given R-03's finding, but not verified this session |
| A2 | The bare `.size` subcolumn behavior (Pitfall 2) is consistent across `String`, `Nullable(String)`, `Array`, `Tuple`, and `Map`-key contexts on every ClickHouse `latest` patch release, not just the specific 26.8.6.5 build probed this session | DESC-03 / Pitfall 2 | If a later `latest` patch changes this, the plan's fixture for the new skip case could go stale; low risk since `latest` is re-pulled per test run (`PullPolicy.alwaysPull()` in `ClickHouseBase.java:60`) and CI already targets `latest` exclusively |
| A3 | A `JSON` column's typed leaf path (`json_col.a.b`) always types as ClickHouse's own `Dynamic` when queried directly, for any nesting depth or value type, not just the two shapes probed (`Int64`, `String`, and a nested object) | Pattern 2 / TYPE-04 | If a deeply nested or array-valued JSON leaf types differently, a TYPE-04 test asserting `toTypeName(...) == "Dynamic"` for every leaf shape could be too strict; verify against the specific shapes the plan actually uses |

## Open Questions

1. **Does doubling the backslash break `\d`-style `targetTableFilter` patterns?**
   - What we know: A single backslash survives ClickHouse's single-quoted string literal intact and reaches `match()`'s RE2 engine correctly (R-03, live-verified).
   - What's unclear: Whether applying D-11's proposed fix (escaping `\` to `\\` before the SQL literal) would then produce a literal `\d` reaching RE2 correctly (since RE2 also uses `\` for its own escapes, `\\d` inside a doubled-backslash SQL literal could resolve differently) — not tested this session.
   - Recommendation: Do not implement D-11 without first re-confirming with the user per D-11's own "Confirm with R-03 before applying" instruction — R-03's finding is that the premise motivating the fix is false, so the fix itself should most likely be dropped (FILT-04 becomes a pin of current, already-correct behavior) rather than modified.

2. **Should the bare `.size` subcolumn (Pitfall 2) be added to D-07's structural skip rule, treated as a separate fix, or left as acknowledged noise?**
   - What we know: It's live-verified, unhandled by the current code, does not drop tables, but produces WARN/ERROR log noise on nearly every `describeTable()` call for nearly every production table with a `String`-bearing column.
   - What's unclear: Whether this qualifies under D-01 item 3's "apply the structural skip rule" wording (same method, same `shouldSkipDescribeRow` helper) or is a "new" fix requiring separate escalation, since it wasn't anticipated when D-07/D-01 were locked.
   - Recommendation: Surface this explicitly to the user before the plan locks the DESC-03 test shape and fix scope — this is exactly the kind of research finding D-01's escalation rule exists to catch.

3. **Does a multi-column table (with a valid column after the JSON/Dynamic column) exhibit different RowBinary desync behavior than the single-JSON-column case probed?**
   - What we know: With a single problematic column, the desynced stream causes ClickHouse to reject the whole batch with `ATTEMPT_TO_READ_AFTER_EOF` (Pitfall 4) — not silent, not partial.
   - What's unclear: Whether a table with additional columns after the `JSON`/`Dynamic` column could shift values into the wrong columns (wrong-but-non-null data) rather than crashing outright, since the CONTEXT's original framing was "crash, silent drop, or wrong data" as three distinct possibilities.
   - Recommendation: The D-02/D-03 fix (throw before any byte for the unsupported column is written) makes this scenario moot regardless of the answer, so this does not block planning — but if the TYPE-06 test wants a "before" fixture demonstrating the exact production risk, a multi-column table would be a stronger demonstration than the single-column probe used here.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker (via colima profile `vz`) | All ClickHouse-backed tests (`@Tag("clickhouse")`), both research probes in this document | ✓ | Docker 27.4.0 engine / colima `vz` running, `default` profile stopped (matches STATE.md's documented setup) | — |
| `clickhouse/clickhouse-server:latest` image | Live probes, ClickHouse-backed tests | ✓ | Resolved to **26.8.6.5** at probe time (`SELECT version()`) | — |
| Gradle / JDK 17 toolchain | Compiling and running the R-02 throwaway probe test | ✓ | Existing project toolchain, unchanged | — |
| `TESTCONTAINERS_HOST_OVERRIDE` env var | ClickHouse-backed test execution via Testcontainers | ✗ (present but set to literal `"null"`, breaks Ryuk) | — | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` (documented in STATE.md Blockers, reused successfully this session for the R-02 probe) |

**Missing dependencies with no fallback:** None.
**Missing dependencies with fallback:** `TESTCONTAINERS_HOST_OVERRIDE` — always unset it before running Gradle, as already documented in STATE.md and reused successfully for this session's live probe.

## Security Domain

`security_enforcement` is `true` in `.planning/config.json` (ASVS level 1, block on `high`). This phase's main relevant surface is V5 (Input Validation), specifically SQL-literal construction for `targetTableFilter`.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | Out of scope — no auth logic touched |
| V3 Session Management | No | Out of scope |
| V4 Access Control | No | Out of scope |
| V5 Input Validation | Yes | `targetTableFilter` is operator-supplied (connector config, not end-user input) and is embedded into a SQL string via `String.format(" AND match(table, '%s')", filter.replace("'", "''"))`. This is a config-time, not request-time, injection surface — the filter comes from trusted connector configuration, not from Kafka message payloads. `[VERIFIED: ClickHouseHelperClient.java:297]` |
| V6 Cryptography | No | Not touched by this phase |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| SQL injection via `targetTableFilter` single quotes | Tampering | Existing `.replace("'", "''")` escaping, confirmed still necessary and sufficient for the single-quote case; FILT-03 already tests this. R-03 shows no *additional* escaping (e.g. for backslash) is needed for correctness — adding unneeded escaping does not close an injection gap (backslash is not a SQL metacharacter for ClickHouse string literals) and should not be framed as a security fix. |
| Malformed/adversarial `DESCRIBE TABLE` output causing a parsing crash | Denial of Service (of the connector task, not the cluster) | `describeTable`'s existing `try`/`catch (Exception e)` around the whole method (returns `null` on any exception) already contains this; no new gap introduced by DESC-01..04's changes since the structural skip rule and log-level change are additive to the existing skip logic, not replacements of the outer exception handling |

This phase does not introduce any new externally-reachable input surface — `targetTableFilter` is connector configuration (operator-trusted), and `JSON`/`Dynamic` column values flow through the same Kafka-record-to-ClickHouse-insert path as every other column type, with no additional deserialization surface added.

## Sources

### Primary (HIGH confidence)
- Live ClickHouse `latest` (resolved: 26.8.6.5) container, this session — `DESCRIBE TABLE ... SETTINGS describe_include_subcolumns=1` probes for `Nullable`, `Array`, `JSON`, `JSON(max_dynamic_paths=N)`, `Dynamic`, `String`, `LowCardinality(String)`, `Map`, `Tuple`, and nested `Array(Array(String))` columns; `match()` regex escaping probes with `\d` and anchors; `distinctJSONPaths`/`toTypeName`/`dynamicType` read-back queries `[VERIFIED: live probe, this session]`
- Live-executed throwaway JUnit test (`GsdResearchProbeR02Test`, created and deleted within this session, no repo diff remains) run via `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests "*GsdResearchProbeR02Test*"` against the same Testcontainers-managed ClickHouse `latest` — captured the exact exception chain for default-config and `bypassSchemaValidation=true` RowBinary inserts into a `JSON` column `[VERIFIED: live-executed probe, this session]`
- Direct `Read` of `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java`, `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`, `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/{Column,Table,Type}.java`, `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java`, `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java`, `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java`, `src/test/java/com/clickhouse/kafka/connect/sink/{ClickHouseBase,JsonColumnTest}.java`, `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientTest.java`, `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` — all line numbers cited above confirmed via this session's `Read` calls `[VERIFIED: file paths and line ranges cited inline]`

### Secondary (MEDIUM confidence)
- [ClickHouse/clickhouse-kafka-connect#622](https://github.com/ClickHouse/clickhouse-kafka-connect/issues/622) — upstream issue proposing to replace hand-rolled `DESCRIBE ... describe_include_subcolumns` parsing with the ClickHouse Java client's own `TableSchemaParser`/`ClickHouseColumn` `[CITED]`
- [ClickHouse DESCRIBE TABLE documentation](https://clickhouse.com/docs/reference/statements/describe-table) — confirms `describe_include_subcolumns` setting exists and documents Tuple-style `.name`/`.age` subcolumns, but does **not** document the `.size` subcolumn found live this session (supports treating that finding as undocumented/version-specific behavior rather than a known, documented feature) `[CITED]`

### Tertiary (LOW confidence)
- None used for factual claims in this document — all claims above are either `[VERIFIED]` via direct source read or live execution, or `[CITED]` from an official/upstream source.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new dependencies; existing stack unchanged and already verified in Phase 1
- Architecture: HIGH — all cited code paths read directly this session with line numbers
- Pitfalls: HIGH — both research flags resolved via live probing/execution, not source-only reasoning, per this phase's explicit instruction; two findings (bare `.size` subcolumn, D-11 falsification) go beyond what CONTEXT.md anticipated and are flagged for user re-confirmation rather than silently folded into "fixed if broken"

**Research date:** 2026-09-17
**Valid until:** 2026-10-17 (30 days) — shorter validity recommended for the `.size` subcolumn finding specifically, since it depends on the exact `clickhouse/clickhouse-server:latest` build (26.8.6.5) resolved at probe time; re-verify if `latest` has moved significantly by execution time
