---
phase: 03-table-metadata-json-dynamic-types
plan: 01
subsystem: database
tags: [clickhouse, describeTable, targetTableFilter, json, dynamic, testing]

# Dependency graph
requires:
  - phase: 01-test-infrastructure-green-baseline
    provides: ClickHouseBase shared-container harness, testUnit Docker-free task, fresh per-class database
provides:
  - "ClickHouseHelperClient.shouldSkipDescribeRow(ClickHouseFieldDescriptor, Table) — the single structural DESCRIBE-row skip rule, called from both describeTableV1 and describeTableV2"
  - "ClickHouseHelperClient.buildTableFilterClause(String) — the extracted showTables() match() clause builder"
  - "Both unmappable-column logs raised from WARN to ERROR"
  - "Conditional StandardCharsets.UTF_8 fix on both describeTable readers (D-15), proven necessary by the TYPE-07 test"
  - "Two new fork-only test classes: ClickHouseHelperClientForkUnitTest (Docker-free, 16 tests) and ClickHouseHelperClientForkLiveTest (ClickHouse-backed, 9 tests)"
  - "Corrected FILT-02 wording in REQUIREMENTS.md (D-12 two-halves split)"
affects: [03-02, 03-03]

# Actuals (#2632)
actuals:
  tokens: 8650
  tasks: 3
  commits: 3

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "shouldSkipDescribeRow/buildTableFilterClause extraction pattern: package-private static helpers directly unit-testable via synthesized ClickHouseFieldDescriptor/Table objects, no Docker required"
    - "Docker-free vs ClickHouse-backed split by inheritance (extend ClickHouseBase vs extend nothing), matching Phase 1 Architecture Pattern 2"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java
    - .planning/REQUIREMENTS.md

key-decisions:
  - "TYPE-07's identifier round-trip test reproduced the deferred Phase 1 D-11 reader bug (Cyrillic column name mangled under the windows-1252 test JVM), so the conditional StandardCharsets.UTF_8 fix (D-01 item 5) shipped on both describeTable readers."
  - "Fixed a test-harness gap in ClickHouseBase.createClient(): it never wired targetTableFilter into the ClickHouseHelperClient it builds, unlike production ClickHouseWriter.start(). Without this fix every live filter test silently ran with no filter applied at all."

patterns-established:
  - "REQ-ID-prefixed @DisplayName carried forward from Phase 2 D-10/D-17 into Phase 3's fork-only test classes."
  - "Non-ASCII test data built from int[] Unicode code points at runtime (D-18); raw non-ASCII bytes and \\u escape literals are both forbidden in source (the latter because javac treats \\u as a real unicode escape even inside comments)."

requirements-completed: [FILT-01, FILT-02, FILT-03, FILT-04, DESC-01, DESC-02, DESC-03, DESC-04, TYPE-07]

coverage:
  - id: D1
    description: "buildTableFilterClause and shouldSkipDescribeRow extracted from ClickHouseHelperClient, wired through showTables/describeTableV1/describeTableV2; both unmappable-column logs raised to ERROR"
    requirement: "DESC-01"
    verification:
      - kind: unit
        ref: "ClickHouseHelperClientForkUnitTest#emptyFilterProducesEmptyClause"
        status: pass
      - kind: integration
        ref: "ClickHouseHelperClientForkLiveTest#describeTableReturnsDeclaredColumnsForJsonAndDynamic"
        status: pass
    human_judgment: false
  - id: D2
    description: "FILT-01..04 pinned at unit and live levels; FILT-02 split into its two D-12 behaviours (empty mapping vs suppress-governed silent-drop/throw); REQUIREMENTS.md wording corrected"
    requirement: "FILT-02"
    verification:
      - kind: unit
        ref: "ClickHouseHelperClientForkUnitTest (FILT-01/03/04 cases)"
        status: pass
      - kind: integration
        ref: "ClickHouseHelperClientForkLiveTest (FILT-01/02/03/04 cases)"
        status: pass
    human_judgment: false
  - id: D3
    description: "DESC-01..04 full skip-rule matrix pinned by unit assertions over synthesized descriptors; DESC-04 pinned live by a Nested fixture returning null; the trailing-dot and operator-precedence defects proven fixed"
    requirement: "DESC-04"
    verification:
      - kind: unit
        ref: "ClickHouseHelperClientForkUnitTest (16 DESC/FILT cases)"
        status: pass
      - kind: integration
        ref: "ClickHouseHelperClientForkLiveTest#arrayOfNestedTableIsUnmappable"
        status: pass
    human_judgment: false
  - id: D4
    description: "TYPE-07 identifier round-trip (non-ASCII column name, CJK Enum members) passes under the windows-1252 test JVM after the conditional UTF-8 reader fix"
    requirement: "TYPE-07"
    verification:
      - kind: integration
        ref: "ClickHouseHelperClientForkLiveTest#nonAsciiIdentifiersRoundTripThroughDescribeTable"
        status: pass
    human_judgment: false

# Metrics
duration: 24min
completed: 2026-09-17
status: complete
---

# Phase 3 Plan 1: Table Discovery Helpers Summary

**Extracted `shouldSkipDescribeRow`/`buildTableFilterClause` from `ClickHouseHelperClient`, pinned FILT-01..04 and DESC-01..04 with 25 new Docker-free/ClickHouse-backed tests, and fixed the deferred Phase 1 non-ASCII DESCRIBE-reader bug.**

## Performance

- **Duration:** 24 min
- **Started:** 2026-09-17T08:25:00Z
- **Completed:** 2026-09-17T08:49:02Z
- **Tasks:** 3
- **Files modified:** 5 (1 main-code file, 2 new test files, 1 test-harness file, 1 requirements doc)

## Accomplishments
- Extracted `shouldSkipDescribeRow(ClickHouseFieldDescriptor, Table)` and `buildTableFilterClause(String)` from duplicated inline logic in `ClickHouseHelperClient`, fixing the pre-existing `.object_shared_data.` operator-precedence and trailing-dot defects, and adding the D-20 bare-`.size` skip.
- Raised both unmappable-column logs from WARN to ERROR in `describeTableV1`/`describeTableV2`.
- Pinned FILT-01..04 (empty-filter clause, quote escaping, backslash-class passthrough per D-19, and the two-halves FILT-02 split per D-12) with 25 tests total across a new Docker-free unit class and a new ClickHouse-backed live class.
- Pinned DESC-01..04's full skip-rule matrix, including the trailing-dot and operator-precedence defect fixes and the Map/Tuple non-skip guarantee.
- Reproduced and fixed the deferred Phase 1 D-11 non-ASCII DESCRIBE-reader bug (TYPE-07): added explicit `StandardCharsets.UTF_8` to both `describeTable` readers after the identifier round-trip test proved the mangling live.
- Corrected FILT-02's wording in `REQUIREMENTS.md` per D-12, keeping the total requirement-bullet count at 75.

## Task Commits

Each task was committed atomically:

1. **Task 1: End-to-end slice — extract both helpers, wire every call site, and prove the path from a synthesized descriptor through a live describeTable** - `ebca05a` (feat)
2. **Task 2: FILT-01 through FILT-04 — unit clause assertions, live filter behaviour, and the FILT-02 wording correction** - `27283c7` (test)
3. **Task 3: DESC-01 through DESC-04 — the full skip-rule matrix, the Nested table drop, and the TYPE-07 identifier half with its conditional UTF-8 fix** - `aa6e08b` (test)

_Note: Task 1 is `type="tracer"` — verified end-to-end before Task 2 began, per the tracer feedback gate (row 3: interactive, `end-of-phase`, automated-only verify → re-run and continue, no checkpoint)._

## Files Created/Modified
- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` - Extracted helpers, warn→error, conditional UTF-8 fix
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java` - New Docker-free unit class, 16 tests (FILT-01/03/04, DESC-01/02/03)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java` - New ClickHouse-backed live class, 9 tests (FILT-01/02/03/04, DESC-01/02/04, TYPE-07)
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` - Fixed missing `targetTableFilter` wiring in `createClient()`
- `.planning/REQUIREMENTS.md` - Corrected FILT-02 bullet wording (D-12)

## Decisions Made
- TYPE-07's identifier test reproduced the deferred Phase 1 D-11 reader bug, so the conditional UTF-8 fix shipped (D-01 item 5, D-15).
- Fixed the `ClickHouseBase.createClient()` test-harness gap (missing `targetTableFilter` wiring) as a Rule 3 blocking-issue deviation — without it, no live filter test could exercise the actual filter behavior.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `ClickHouseBase.createClient()` never wired `targetTableFilter` into the built `ClickHouseHelperClient`**
- **Found during:** Task 2, writing the FILT-03/FILT-04 live tests
- **Issue:** The shared test harness's `createClient()` helper builds a `ClickHouseHelperClient.ClickHouseClientBuilder` without calling `.setTargetTableFilter(csc.getTargetTableFilter())`, unlike production `ClickHouseWriter.start()` which already does. Every live test built through `createClient()` with a `targetTableFilter` config value silently ran with no filter applied at all — `showTables()` always returned every table.
- **Fix:** Added `.setTargetTableFilter(csc.getTargetTableFilter())` to both `ClickHouseClientBuilder` construction sites in `ClickHouseBase.createClient()`, mirroring the existing production idiom exactly.
- **Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java`
- **Verification:** FILT-03 and FILT-04 live tests failed before the fix (filter not applied, assertions mismatched) and passed after.
- **Committed in:** `27283c7` (Task 2 commit)

**2. [Rule 1 - Bug] FILT-02 live test used unwired props for `ClickHouseSinkTask.start()`**
- **Found during:** Task 2, writing the FILT-02(b) live test
- **Issue:** Calling `new ClickHouseSinkTask().start(props)` with `props` from `createProps()` alone (never passed through `createClient()`) left `DATABASE` pointed at `default` instead of this test class's per-class database, so the target/matched tables the test created were invisible to the sink task.
- **Fix:** Call `createClient(props)` before starting each `ClickHouseSinkTask`, which mutates `props` in place to the per-class test database — the same pattern the upstream `ClickHouseSinkTaskMappingTest.tableFilterTest` already uses.
- **Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java`
- **Verification:** FILT-02(b) live test passed after the fix.
- **Committed in:** `27283c7` (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (1 Rule 3 blocking, 1 Rule 1 bug)
**Impact on plan:** Both fixes were necessary to make the plan's own required live tests actually exercise the behavior they claim to pin. No scope creep — both are test-infrastructure-only changes; no upstream test file was touched (D-16 honored, hash-object check confirmed unchanged).

## Issues Encountered
None beyond the deviations above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- `shouldSkipDescribeRow` and `buildTableFilterClause` are now directly testable and reused as-is by plans 03-02 and 03-03.
- The conditional UTF-8 reader fix is in place; any future non-ASCII DESCRIBE-output work in 03-02/03-03 inherits a correctly-decoded stream.
- No blockers for 03-02 (JSON/Dynamic round-trip and non-ASCII value tests) or 03-03 (RowBinary fail-fast tests).

## Self-Check: PASSED

---
*Phase: 03-table-metadata-json-dynamic-types*
*Completed: 2026-09-17*
