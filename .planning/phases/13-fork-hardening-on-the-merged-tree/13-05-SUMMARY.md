---
phase: 13-fork-hardening-on-the-merged-tree
plan: "05"
subsystem: sink-db-metadata-and-config
tags: [clickhouse, kafka-connect, describe-table, json, dynamic, configdef, tdd]

requires:
  - phase: 12-merge-line-decisions
    provides: merged v1.7.0 tree, fork-anchors.txt, verify-merge-invariants.sh
provides:
  - "shouldSkipDescribeRow walks every ancestor prefix and skips rows at any depth below a JSON or Dynamic column (DESC-05)"
  - "Column.extractColumn Array recursion passes hasDefaultValue and isSubColumn in signature order (COLS-07)"
  - "ClickHouseSinkConfig USERNAME ConfigDef default is usernameDefault (CFG-04)"
  - "Fork-only Docker-free pins: ColumnArrayRecursionTest, ClickHouseSinkConfigUsernameDefaultUnitTest"
affects: [13-08 behaviour ledger consolidation, docs/FORK-DELTA.md]

actuals:
  tokens: 3960
  tasks: 3
  commits: 3

plan_head_before: 08c3e2f5c789bfec61253fb0e5f6e08a28080cad
plan_head_after: 189e0bb2aa70352c8e86fbf4677af9229ddc55f1

tech-stack:
  added: []
  patterns:
    - "Ancestor-prefix walk (lastIndexOf('.', dot - 1)) for DESCRIBE sub-column rows"
    - "Fork-only test class beside an upstream one (ColumnArrayRecursionTest next to ColumnTest) to keep future syncs conflict-free"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnArrayRecursionTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfigUsernameDefaultUnitTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
    - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java

key-decisions:
  - "DESC-05 skips only when an ancestor is JSON or Dynamic; Tuple-in-Tuple, Array(JSON), Map and Tuple rows stay un-skipped"
  - "COLS-07 is a one-site argument-order fix; Enum and Tuple nullability handling untouched (D-16)"
  - "CFG-04 changes display metadata only; the runtime still reads props.getOrDefault(USERNAME, usernameDefault)"

requirements-completed: [DESC-05, COLS-07, CFG-04]

coverage:
  - id: D1
    description: "DESCRIBE rows at any depth below a JSON or Dynamic column are skipped; Tuple, Map and Array(JSON) sub-columns are kept"
    requirement: DESC-05
    verification:
      - kind: unit
        ref: "ClickHouseHelperClientForkUnitTest#rowsAtAnyDepthBelowJsonOrDynamicAreSkipped"
        status: pass
      - kind: unit
        ref: "ClickHouseHelperClientForkUnitTest#rowsWithoutJsonOrDynamicAncestorAreNotSkipped"
        status: pass
      - kind: integration
        ref: "ClickHouseHelperClientForkLiveTest#describeTableSkipsRowsAtAnyDepthBelowJson"
        status: pass
    human_judgment: false
  - id: D2
    description: "Array element columns carry the parent's hasDefault and isSubColumn for all four combinations, at depth 1 and 2"
    requirement: COLS-07
    verification:
      - kind: unit
        ref: "ColumnArrayRecursionTest"
        status: pass
    human_judgment: false
  - id: D3
    description: "ConfigDef reports \"default\" as the USERNAME default; runtime default and PASSWORD default unchanged"
    requirement: CFG-04
    verification:
      - kind: unit
        ref: "ClickHouseSinkConfigUsernameDefaultUnitTest"
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-10-08
status: complete
---

# Phase 13 Plan 05: Metadata and Config Hardening Summary

**DESCRIBE rows at any depth below a JSON/Dynamic column are skipped via an ancestor-prefix walk, Array recursion now passes hasDefault/isSubColumn in signature order, and the USERNAME ConfigDef default is "default"**

## Performance

- **Duration:** 8 min
- **Started:** 2026-10-08T17:01:30Z
- **Completed:** 2026-10-08T17:09:00Z
- **Tasks:** 3
- **Files modified:** 7 (2 created, 5 modified)

## Accomplishments
- DESC-05: `j.b.c` (typed JSON path with no `j.b` row) no longer becomes a bogus Column; `nt.a`, `nt.a.b`, `aj.k`, `m.values`, `t.a` are still kept.
- COLS-07: the swapped `isSubColumn, hasDefaultValue` arguments in the Array branch are back in signature order.
- CFG-04: `define(USERNAME, ...)` uses `usernameDefault` instead of `passwordDefault`.

## Task Commits

1. **Task 1: DESC-05 ancestor walk** - `31dc4939` (fix)
2. **Task 2: COLS-07 Array recursion argument order** - `4543507c` (fix)
3. **Task 3: CFG-04 USERNAME ConfigDef default** - `189e0bb2` (fix)

**Plan metadata:** committed separately as `docs(13-05)`.

Each task was written test-first, but the RED test and the fix share one commit per task, as the plan specifies ("One commit per task"). RED evidence below comes from the actual Gradle runs before the fix.

## TDD RED evidence

Runner: Gradle JUnit XML (`build/test-results/test/TEST-*.xml`), run before each fix.

- **Task 1 (DESC-05):** 4 unit failures in `ClickHouseHelperClientForkUnitTest`: `DESC-05: j.b.c is skipped because a JSON or Dynamic ancestor exists`, `j.d.x`, `j.d.y`, `d.a.b`. Live `describeTableSkipsRowsAtAnyDepthBelowJson` failed with `no sub-column of the JSON column may become a Column, found: [nt.a.b, nt, j, nt.a, id, j.b.c]`. Semantic assessment: the failing assertion is the planned one (skip below a JSON ancestor) and the live failure names exactly the bogus `j.b.c` entry. `tj.jj.k` passed before the fix because `tj.jj` is the immediate parent in `columnsSoFar`; it stays as a regression pin.
- **Task 2 (COLS-07):** `ColumnArrayRecursionTest`: 9 tests, 3 failed: `hasDefault=true isSubColumn=false`, `hasDefault=false isSubColumn=true` and `Array(Array(Int32)) propagates the flags in the right order at depth 1 and depth 2`. The (true,true) and (false,false) combinations passed on the swapped arguments, as expected. Semantic assessment: failures are on the planned flag assertions.
- **Task 3 (CFG-04):** `ClickHouseSinkConfigUsernameDefaultUnitTest`: 3 tests, 1 failed, `expected: <default> but was: <>` on the USERNAME ConfigDef default. The runtime and PASSWORD tests passed before and after.

## Behaviour ledger

- **L-16: DESC-05.** DESCRIBE rows below JSON/Dynamic at any depth no longer create bogus Column entries. Root columns were already unaffected. Verified live on ClickHouse `latest` with client V2: root columns are exactly `{id, j, nt}` and `getAllColumnsMap()` holds no `j.`-prefixed key while keeping `nt.a` and `nt.a.b`.
- **L-17: COLS-07.** Array element columns now carry the parent's `hasDefault` and `isSubColumn`. For `Array(Tuple)`, a missing tuple field without a default now fails loudly in `doWriteCol`'s "not nullable and no value" path instead of writing a stray `0x01` marker. The regression run (`ColumnTest`, `JsonColumnTest`, `TableTest`, `ClickHouseSinkTaskWithSchemaTest`, `ClickHouseHelperClientTest`: 171 tests, 157 passed, 14 skipped, 0 failed) observed no affected test; the skips are the pre-existing named skips. Plan 13-08's full suite is the remaining net for this change.
- **L-18: CFG-04.** The ConfigDef/REST-displayed default for `username` is now `"default"`. Runtime behaviour is unchanged; `PASSWORD`'s ConfigDef default is still `""`.

## Decisions Made

None beyond the plan; the three fixes are the minimal ones specified.

## Deviations from Plan

None - plan executed exactly as written.

Notes on plan mechanics: the spotless step was not run because the project's `spotless` config only orders and removes unused imports (no new imports were added) and the plan forbids a tree-wide `spotlessApply`. The Java 17 `testUnit` task ran 404 tests, 402 passed, 2 skipped, 0 failed.

## Issues Encountered

A BSD `sed -i` call failed on the first COLS-07 edit attempt (unterminated pattern); no file was changed, and the edit was redone with the Edit tool.

## Verification

- Task 1: `ClickHouseHelperClientForkUnitTest` (29), `ClickHouseHelperClientForkLiveTest` (11), `ClickHouseHelperClientTest` (18): 58 passed, 0 failed.
- Task 2 acceptance greps: `false, hasDefaultValue, isSubColumn, arrayDepth + 1` count 1, swapped form count 0, `Dynamic` in Column.java count 1.
- Task 3 acceptance greps: `usernameDefault` follows `define(USERNAME,`; `TARGET_TABLE_FILTER` 3; `jsonInsertFormatExplicit` 2; `shouldSkipDescribeRow` 3 in ClickHouseHelperClient.java.
- `verify-merge-invariants.sh` after each commit: `result=PASS` (last: anchor_commits=33 anchor_rows=40 marker_commits=80).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Ready for the remaining Phase 13 plans. 13-08 should consolidate L-16..L-18 into the behaviour ledger and `docs/FORK-DELTA.md`.

## Self-Check: PASSED

- Created files exist: `ColumnArrayRecursionTest.java`, `ClickHouseSinkConfigUsernameDefaultUnitTest.java`.
- Commits `31dc4939`, `4543507c`, `189e0bb2` are ancestors of HEAD.

---
*Phase: 13-fork-hardening-on-the-merged-tree*
*Completed: 2026-10-08*
