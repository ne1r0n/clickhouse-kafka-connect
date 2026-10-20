---
phase: 19-nullable-enum-tuple-mapping
plan: "09"
subsystem: declared-nullability-pass
tags: [cols-10, cols-11, gap-closure, pins, mutation]
requires:
  - phase: 19-nullable-enum-tuple-mapping (19-07, 19-08)
    provides: SimpleAggregateFunction unwrap in DeclaredNullability.collect; four-argument total apply; ColumnNullabilityPaths
provides:
  - parser premise pinned (LowCardinality(Nullable(String)) nullable, LowCardinality(String) not, SimpleAggregateFunction argument nullable)
  - LowCardinality(Nullable(String)) pinned as Tuple element, Array element and Map value
  - SimpleAggregateFunction boundary and adjacency pins (Int64, over LowCardinality(Nullable), over Array)
  - proof by never-committed mutation that the 19-07 unwrap is necessary
affects: [19-10, FORK-DELTA, ROLL-02]
actuals:
  tokens: 6500
  tasks: 2
  commits: 1
plan_head_before: 4a532a6f4b2bc386a8bf7a16eda39c00bfba063e
plan_head_after: 01e932e37a41a22673237b27a20663c7b89207fc
tech-stack:
  added: []
  patterns:
    - "Pin-only plan: new cases ran green on first run because the fix landed in 19-07; necessity shown by an uncommitted mutation"
key-files:
  created: []
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
    - .planning/phases/19-nullable-enum-tuple-mapping/19-BEHAVIOUR-LEDGER.md
key-decisions:
  - "No production change: every new pin passed against the 19-07/19-08 code, so no fix commit was needed"
requirements-completed: [COLS-10, COLS-11]
coverage:
  - id: D1
    description: "Parser premise and LowCardinality(Nullable) pins in Tuple, Array and Map positions"
    requirement: "COLS-10"
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest (25 cases, 0 failures) - parserReadsLowCardinalityNullableAsNullable, lowCardinalityNullableTupleElementKeepsDeclaredFlags, lowCardinalityNullableArrayElementIsNullable, lowCardinalityNullableMapValueIsNullable"
        status: pass
    human_judgment: false
  - id: D2
    description: "SimpleAggregateFunction wrapper boundary and adjacency pins"
    requirement: "COLS-10"
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest - simpleAggregateFunctionNonNullableElementStaysNonNullable, simpleAggregateFunctionOverLowCardinalityNullableIsNullable, simpleAggregateFunctionOverArrayElementPairsWithoutFallback"
        status: pass
    human_judgment: false
  - id: D3
    description: "Mutation (unwrap removed) fails exactly the four wrapper cases and both live cases; restored, src/main clean"
    requirement: "COLS-10"
    verification:
      - kind: command
        ref: "gap-09 mutation command, GAP-09-MUTATION-RED"
        status: pass
    human_judgment: false
  - id: D4
    description: "Ledger L-41 Pinned-by names the seven new methods in the same commit; anchors invariant passes over all commits"
    requirement: "COLS-11"
    verification:
      - kind: command
        ref: "bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD -> result=PASS"
        status: pass
    human_judgment: false
duration: 6min
completed: 2026-10-13
status: complete
---

# Phase 19 Plan 09: Wrapper edge pins for the declared-nullability pass Summary

**Seven Docker-free pins (parser premise, LowCardinality(Nullable) in Tuple/Array/Map, SimpleAggregateFunction boundary and adjacency) added to DeclaredNullabilityTest, with a never-committed mutation proving the 19-07 unwrap is what makes four of them pass.**

## Performance
- Duration: about 6 min (2026-10-13T10:11:57Z to 10:18:06Z)
- Tasks: 2 (1 commit; Task 2 changes no tracked file by design)
- Files modified: 2

## Accomplishments
- Premise run (Task 1 step 1), recorded on its own line: `parserReadsLowCardinalityNullableAsNullable` added alone, `DeclaredNullabilityTest` = 19 tests, 0 failures, 0 errors, 0 skipped. The premise holds, so the dependent fixtures were written.
- Fixture run: six further cases added, `DeclaredNullabilityTest` = 25 tests, 0 failures, errors, skips. All new pins passed first run (pin-only plan; the fix is 19-07's, so no case was seen red before GREEN, and none was expected to be).
- Mutation (`DeclaredNullability.collect`: `unwrapSimpleAggregate(declared)` replaced by `declared`, never committed, restored by `git checkout`, `git diff --quiet -- src/main` true afterwards):
  - Docker-free failures, exactly four: `simpleAggregateFunctionNullableElementKeepsItsNullability`, `rootSimpleAggregateFunctionOverTuplePairsWithoutFlippingFlags`, `simpleAggregateFunctionOverLowCardinalityNullableIsNullable`, `simpleAggregateFunctionOverArrayElementPairsWithoutFallback`.
  - Live failures, both: `COLS-10 CR-01 SimpleAggregateFunction Tuple elements round-trip PLAIN` and `... DEFAULT`.
- Regression on restored code (server ClickHouse `26.9.17.8`, client V2), all with 0 failures/errors/skips: DeclaredNullabilityTest 25, ClickHouseHelperClientForkUnitTest 45, NestedNullableCompositeWriteTest 18, NullableEnumWriteTest 14, ColumnArrayRecursionTest 9, SimpleAggregateTupleRoundTripLiveTest 2, NullableTupleMatrixLiveTest 30 (exactly), NestedNullRowBinaryLiveTest 10, ClickHouseHelperClientForkLiveTest 12, NullableEnumRoundTripLiveTest 6.
- Anchors gate line: `invariants: provenance_paths=21 anchor_commits=294 anchor_rows=59 marker_commits=341 result=PASS` (range `54e63648..HEAD`, includes fix(19-07), fix(19-08), refactor(19-08), test(19-09)).
- Spotless IDE hook on `DeclaredNullabilityTest.java`: exit 0, stderr `IS CLEAN`; stdout empty; before/after snapshot comparison identical (`SNAP-IDENTICAL`).

## Task Commits
1. Task 1: `01e932e3` `test(19-09): pin LowCardinality and SimpleAggregateFunction wrapper edges of the declared-nullability pass (COLS-10)` (test file + ledger L-41, exactly those two files)
2. Task 2: no commit (mutation and regression only)

## Files Created/Modified
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` - +7 cases, +`ClickHouseColumn` import
- `.planning/phases/19-nullable-enum-tuple-mapping/19-BEHAVIOUR-LEDGER.md` - L-41 Pinned-by cell names the seven methods (row stays 8 columns)

## Decisions Made
None beyond keeping production code untouched.

## Deviations from Plan

**1. [Rule 3 - Blocker] Plan verify scripts match method names against JUnit XML testcase names**
- **Found during:** Task 1 and Task 2 verification
- **Issue:** `DeclaredNullabilityTest` uses `@DisplayName`, so `<testcase name>` in the XML is the display text, not the method name. The plan's inline `startswith(method)` check (Task 1) and `name.split("(")[0] == method` check (Task 2) can never match.
- **Fix:** Ran the same checks with the method-name mapping derived from the `@DisplayName` / `void name(` pairs in the source (`/tmp/gsd19/exec/gap09_mut_check.py` for the mutation; source grep for presence of all nine names in Task 1). Counts (25), zero failures, the exact four-name failing set and the two live failures were all asserted unchanged. No expectation was altered.
- **Files modified:** none in repo
- **Commit:** n/a

**2. [Verify adaptation] Task 1 composite verify run in pieces**
- The composite one-liner was executed as its parts (unit run, hook with before/after snapshot, source greps, `git show --name-only`, L-41 column count of 10 `|`-fields = 8 columns, `git diff --quiet -- src/main`, clean status for `src docs .planning/scripts` and the ledger) because of the display-name issue above. All conditions held; the printed `GAP-09-T1-OK` token was therefore not emitted as one line.

**Total deviations:** 1 Rule 3 (verify tooling), 0 code changes. **Impact:** none on behaviour or coverage.

## Issues Encountered
- Anchors script exceeds the foreground 120 s limit; it completed in the background with result=PASS.

## Known Stubs
None.

## Next Phase Readiness
Gap 1 pins complete; ROADMAP Phase 19 criterion 4 holds over every commit so far.

## Self-Check: PASSED
- DeclaredNullabilityTest.java and 19-BEHAVIOUR-LEDGER.md exist; commit `01e932e3` is on `sync/v1.7.0`; `git diff --quiet -- src/main` true; all acceptance criteria evidence recorded above.

---
*Phase: 19-nullable-enum-tuple-mapping*
*Completed: 2026-10-13*
