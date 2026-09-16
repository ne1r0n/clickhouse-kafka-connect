# Phase 3: Table Metadata & JSON/Dynamic Types - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-17
**Phase:** 3-table-metadata-json-dynamic-types
**Areas discussed:** RowBinary fail-fast (TYPE-06), describeTable audit + DESC-04, targetTableFilter test surface, JSON/Dynamic round-trips + non-ASCII

---

## Area selection

| Option | Description | Selected |
|--------|-------------|----------|
| RowBinary fail-fast (TYPE-06) | Where the clear error is raised and how broad it is | ✓ |
| describeTable audit + DESC-04 (DESC-01..04) | Fix policy for missed sub-column shapes; whether DESC-04 asserts the log level | ✓ |
| targetTableFilter test surface (FILT-01..04) | Clause extraction for unit tests; what FILT-02 pins | ✓ |
| JSON/Dynamic round-trips + non-ASCII (TYPE-03..05, 07) | Insert route, read-back depth, non-ASCII scope | ✓ |

**User's choice:** all four areas.

---

## RowBinary fail-fast (TYPE-06)

### Q1 — Where should the fail-fast live?

| Option | Description | Selected |
|--------|-------------|----------|
| Throw in the default branch | ~2 lines in `doWriteColValue`'s `default:`, one method, rebase-friendly; fires exactly where the stream would desync | ✓ |
| Pre-check before streaming | Scan the Table's columns in `doInsertRawBinary` and throw before any byte is written | |
| Both | Pre-check plus default-branch backstop | |

**Notes:** Ground truth shown: the `default:` branch logs `"Cannot serialize unsupported type {}"` and writes zero bytes, so the caller keeps streaming into a desynced RowBinary stream.

### Q2 — How broad should the throw be?

| Option | Description | Selected |
|--------|-------------|----------|
| Throw for everything, one message | Covers JSON, DYNAMIC and UNKNOWN; no type list to keep in sync | ✓ |
| Throw only for JSON/DYNAMIC | Narrowest behavior change; leaves silent corruption alive for UNKNOWN | |
| Throw for all, branch the message | Best diagnostics, two message shapes to assert | |

### Q3 — Does the validation gate get the same error?

| Option | Description | Selected |
|--------|-------------|----------|
| Both gates speak it | Add a JSON/DYNAMIC/UNKNOWN check in `validateDataSchema` so the default-config path stops saying "Data schema validation failed." | ✓ |
| Writer gate only | Keep one touch point; arrange the test to reach it via `bypassSchemaValidation` or a DEFAULT | |
| Writer gate now, decide validation after ground truth | Defer the second decision into planning | |

**Notes:** Established during discussion that `validateDataSchema` runs first under default config and a plain `JSON` column always fails it, so the change is a message improvement on an already-failing insert rather than a behavior change. Recorded as research flag R-02 to confirm by running.

### Q4 — At what level is TYPE-06 pinned?

| Option | Description | Selected |
|--------|-------------|----------|
| Unit + one sink-level test | Unit tests pin the message contract on both protected gates; one live test proves `countRows == 0` | ✓ |
| Sink-level only | Closest to production; costs container time and couples to exception wrapping | |
| Unit only | Fastest; nothing proves the table stays empty | |

### Q5 (asked later, in the TYPE area) — What does the message say?

| Option | Description | Selected |
|--------|-------------|----------|
| Name both escape hatches | `insertFormat=json` **and** `bypassRowBinary=true`, both pinned as working by D-13 | ✓ |
| `insertFormat=json` only | Matches TYPE-06's literal wording; hides the one-flag alternative | |

---

## describeTable audit + DESC-04 (DESC-01..04)

### Q1 — Fix policy for sub-column shapes the skip list misses

| Option | Description | Selected |
|--------|-------------|----------|
| Structural rule by parent type | Skip rows whose parent column is JSON/DYNAMIC; keep `.null`/`.size0`; leave Map/Tuple sub-columns alone | ✓ |
| Extend the literal suffix list | Smallest diff, matches upstream style, goes stale silently | |
| Decide in planning after the audit | Capture raw DESCRIBE output first, then choose | |

**Notes:** Two defects shown in the current condition — the `.object_shared_data.` clause is not guarded by `isSubcolumn()` and requires a trailing dot. Also established that `Table.updateParent` genuinely consumes Map/Tuple sub-columns, so a blanket skip would break them.

### Q2 — Where the rule lands, given V1/V2 duplication

| Option | Description | Selected |
|--------|-------------|----------|
| One private helper, called from both | Also makes DESC-01..03 unit-testable with synthesized field descriptors | ✓ |
| Duplicate it in both methods | Smallest conflict surface; container-only testing; copies drift | |
| V2 only | Milestone targets V2; leaves V1 divergent | |

### Q3 — Does DESC-04 assert the log level?

| Option | Description | Selected |
|--------|-------------|----------|
| Behavior-only, change the level unasserted | Assert the table drop; flip warn→error without an appender — same call as P2 D-15 for TOF-03 | ✓ |
| Build the log-capture helper | Proves the requirement literally; new global-state test infrastructure | |
| Behavior-only, and drop the level change | Pin the drop, leave `warn`, correct REQUIREMENTS.md | |

### Q4 — Where do new tests live? (phase-wide rule)

| Option | Description | Selected |
|--------|-------------|----------|
| New fork-only test classes | Upstream test files untouched; protects the Rebasability constraint | ✓ |
| Grow the existing upstream files | One place per subject; conflict surface in three upstream files | |
| Mixed by test kind | Unit tests new, live tests appended to upstream classes | |

**Notes:** Flagged that Phase 2's D-09 ("grow the existing file") does not transfer, because those four SMT test files are fork-only while `ClickHouseHelperClientTest`, `ColumnTest` and `ClickHouseSinkTaskMappingTest` are upstream.

---

## targetTableFilter test surface (FILT-01..04)

### Q1 — How FILT-03 gets a unit-testable seam

| Option | Description | Selected |
|--------|-------------|----------|
| Extract a clause builder | Docker-free assertions on the empty clause, the escaped quote, and RE2 round-tripping | ✓ |
| Extract the whole query builder | More coverage per test; pins the `system.columns` query shape | |
| Live-only, no main-code change | Zero production diff; escaping bugs appear as generic query failures | |

### Q2 — How FILT-02 is pinned

| Option | Description | Selected |
|--------|-------------|----------|
| Split it in two, correct the wording | Empty mapping fails `start()` regardless of the flag; suppression only applies with a non-empty mapping | ✓ |
| Split it, and fix the misleading message | Same tests plus a message change in an upstream-shared path | |
| Pin the empty-mapping case only | Narrowest; leaves the suppress path to the upstream test | |

**Notes:** Traced that `mapping.isEmpty()` → `start()` returns false → `ProxySinkTask` throws "Connection to ClickHouse is not active.", so the requirement's two clauses describe states that cannot co-occur. Applied the P2 D-14 precedent of correcting REQUIREMENTS.md.

### Q3 — If `\d` is mangled by the SQL literal

| Option | Description | Selected |
|--------|-------------|----------|
| Escape backslashes too | Plain RE2 patterns work as written; only a pre-doubled workaround config needs an edit | ✓ |
| Pin the current behavior | Zero risk to existing connectors; leaves a footgun documented | |
| Decide after the live ground truth | Run the check first, then choose | |

**Notes:** Reasoned that a config using `\d` today already matches nothing, so nobody can be successfully relying on the current behavior. Recorded as research flag R-03 to confirm before applying.

---

## JSON/Dynamic round-trips + non-ASCII (TYPE-01..05, 07)

### Q1 — Which route pins TYPE-03/04/05?

| Option | Description | Selected |
|--------|-------------|----------|
| Schemaless JSONEachRow + bypassRowBinary | Both non-RowBinary production routes; no Phase 4 overlap | ✓ |
| Schemaless JSONEachRow only | Smallest container cost; leaves the Struct route uncovered | |
| Explicit insertFormat=json | Matches the wording literally; collides with Phase 4's JSON-01..05 | |

### Q2 — Read-back assertion depth

| Option | Description | Selected |
|--------|-------------|----------|
| Typed SQL read-back | Nested path access for JSON plus `toTypeName()`/`dynamicType()` for Dynamic | ✓ |
| Reuse getAllRowsAsJson and compare structures | Less SQL; Dynamic's runtime type is invisible in a JSON dump | |
| Values only, no type assertions | Least brittle; drops TYPE-04's "and types" clause | |

### Q3 — Does TYPE-07 cover identifiers?

| Option | Description | Selected |
|--------|-------------|----------|
| Values + non-ASCII identifiers | The only thing that exercises the unguarded describeTable readers; closes Phase 1 D-11 with proof | ✓ |
| Values only, fix the readers anyway | Two-line safety change with no regression test | |
| Values only, re-defer the readers | Carries the Phase 1 item across a second phase | |

**Notes:** Established that the unguarded readers at `ClickHouseHelperClient.java:370` and `:417` only ever see DESCRIBE output, so record values never reach them — column names, Enum members and DEFAULT expressions do.

---

## Claude's Discretion

- Names and packages of the new fork-only test classes, and their mapping onto plans 03-01 / 03-02 / 03-03
- Exact signature and visibility of `shouldSkipDescribeRow(...)` and `buildTableFilterClause(...)`
- Exact wording of the TYPE-06 error message beyond column, type, `insertFormat=json`, `bypassRowBinary=true`; and the exception type at each gate
- Live-test table shapes, record counts, and whether tests share a table
- Which TYPE-01/TYPE-02 type strings get parameterized
- `@DisplayName` phrasing beyond the `REQ-ID: ` prefix

## Deferred Ideas

- The misleading `"Connection to ClickHouse is not active."` message for an empty/filtered-out mapping
- A pre-check scanning table columns for unsupported types before streaming RowBinary bytes
- The log-capture test helper (declined for the second time)
- `describeTableV2` ignoring its `database` parameter while V1 honours it
- `describeTableV1`/`V2` result parity — already tracked as v2 requirement PAR-01
