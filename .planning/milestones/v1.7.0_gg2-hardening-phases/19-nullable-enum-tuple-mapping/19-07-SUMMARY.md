---
phase: 19-nullable-enum-tuple-mapping
plan: "07"
subsystem: declared-nullability-pass
tags: [cols-10, cols-11, cr-01, simple-aggregate-function, tuple, row-binary, gap-closure]
requires:
  - phase: 19-nullable-enum-tuple-mapping (19-04)
    provides: DeclaredNullability pass wired into ClickHouseHelperClient.buildDescribedTable
provides:
  - DeclaredNullability.collect reads through SimpleAggregateFunction at its entry, so SimpleAggregateFunction(f, Nullable(T)) Tuple elements keep isNullable=true and their RowBinary null marker (CR-01 closed)
  - Docker-free CR-01 and root-wrapper pins plus a live twin round trip (RowBinary and RowBinaryWithDefaults)
  - fork-anchors row, FORK-DELTA clause and ledger row L-41 in the same commit as the fix
affects: [19-09, 19-10, 20-release, FORK-DELTA, ROLL-02]
actuals:
  tokens: 11000
  tasks: 1
  commits: 1
plan_head_before: f6b4e93321e9d430a7102e9535553b0ec052f2fd
plan_head_after: 4e133d4e7465008fb51ab84aada0de3815737226
tech-stack:
  added: []
  patterns:
    - "Unwrap type wrappers at the entry of a recursive declared-tree walk so every branch sees the node Column.extractColumn saw"
key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/SimpleAggregateTupleRoundTripLiveTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - .planning/phases/19-nullable-enum-tuple-mapping/19-BEHAVIOUR-LEDGER.md
key-decisions:
  - "Unwrap at the entry of collect (all four branches use the unwrapped node), not only in the leaf branch: mirrors Column.extractColumn at every depth, and a wrapped Array, Map or Tuple now pairs instead of logging the L-40 WARN. Reversible, one call site."
  - "Root SimpleAggregateFunction(f, Tuple(...)) pairs through the pass and flips no flag (pinned, chosen outcome)"
requirements-completed: [COLS-10, COLS-11]
coverage:
  - id: D1
    description: "A Tuple element declared SimpleAggregateFunction(f, Nullable(T)) keeps isNullable=true after DeclaredNullability.apply (Docker-free)"
    requirement: "COLS-10"
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest#simpleAggregateFunctionNullableElementKeepsItsNullability"
        status: pass
    human_judgment: false
  - id: D2
    description: "A root SimpleAggregateFunction over a Tuple pairs, logs no WARN and leaves the flags unchanged"
    requirement: "COLS-10"
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest#rootSimpleAggregateFunctionOverTuplePairsWithoutFlippingFlags"
        status: pass
    human_judgment: false
  - id: D3
    description: "Live round trip on both twins through the real sink task: NULL in the wrapped element, NULL in the sibling after it, Int64 2^53+1, empty Array and [NULL] in the wrapped Array element, no DeclaredNullability WARN, multi-line Tuple roots delivered by DESCRIBE"
    requirement: "COLS-10"
    verification:
      - kind: integration
        ref: "SimpleAggregateTupleRoundTripLiveTest#simpleAggregateFunctionTupleElementsRoundTrip (PLAIN, DEFAULT) on ClickHouse 26.9.17.8, client V2"
        status: pass
    human_judgment: false
  - id: D4
    description: "Same-commit bookkeeping: anchor row (count 2), FORK-DELTA clause and Pinned-by entry, ledger row L-41"
    requirement: "COLS-11"
    verification:
      - kind: other
        ref: "Task 1 commit verify: src True anchor True forkdelta True ledger True commits 1 extra [] missing []"
        status: pass
    human_judgment: false
duration: 4min
completed: 2026-10-13
status: complete
---

# Phase 19 Plan 07: SimpleAggregateFunction Tuple element nullability (CR-01) Summary

**DeclaredNullability now unwraps SimpleAggregateFunction at the entry of its tree walk, so `SimpleAggregateFunction(f, Nullable(T))` Tuple elements keep their null marker and round-trip on both RowBinary routes; fix, pins, anchor row, FORK-DELTA clause and ledger L-41 landed in one commit.**

## Performance
- **Duration:** 4 min
- **Started:** 2026-10-13T10:00:16Z
- **Completed:** 2026-10-13T10:04:00Z
- **Tasks:** 1
- **Files modified:** 6 (1 created, 5 modified)

## Accomplishments
- CR-01 fixed: the pass no longer flips a Tuple element declared `SimpleAggregateFunction(f, Nullable(T))` from nullable to non-nullable, which had made the writer drop its null marker and misalign the RowBinary row.
- Wrapped Array, Map or Tuple elements (and a root wrapper over a Tuple) now pair through the pass instead of falling back with the L-40 WARN.
- The matrix class still has exactly 30 passing cases; `DeclaredNullabilityTest` went from 15 to 17.

## Task Commits
1. **Task 1 (tracer): unwrap, pins, live class, anchors, FORK-DELTA, ledger** - `4e133d4e` (fix)

**Plan metadata:** see the docs(19-07) commit that follows this file.

## Seen red (before the fix)
Run against the unfixed code, logs in `/tmp/gsd19/exec/gap-07-red-unit.log` and `gap-07-red-live.log`:
- `DeclaredNullabilityTest` CR-01 case failed: expected `{c=false, c.a=true, c.b=true}` but was `{c=false, c.a=false, c.b=true}` (premise `c.a=true` before apply held). This is the wrapper node's own non-nullable flag overwriting the wrapped element's.
- `DeclaredNullabilityTest` root-wrapper case failed: `apply` returned false (one structure-mismatch WARN) where the pinned outcome is true. All three premises (root is TUPLE, `containsTuple` true, flags `{c=false, c.a=true, c.b=false}`) held, so the pass does see that shape.
- `SimpleAggregateTupleRoundTripLiveTest` failed on both twins at "the declared-nullability pass must not fall back": the pass logged one structure-mismatch WARN for column `g` (wrapped Array element, the L-40 fallback). The `c` root paired, so `c.a` was only caught by the Docker-free case before the live class reached its flag assertions.
After the fix: 17/17 Docker-free, 2/2 live, matrix 30/30, all green.

## Observed `SAF_TUPLE describe` rows (server 26.9.17.8, identical on PLAIN and DEFAULT)
```
id | UInt32 | subcolumn=false
c | Tuple(\n    a SimpleAggregateFunction(sum, Nullable(Int64)),\n    b Nullable(Int32)) | subcolumn=false
g | Tuple(\n    a SimpleAggregateFunction(anyLast, Array(Nullable(Int32))),\n    b Int32) | subcolumn=false
(DEFAULT twin only: d | UInt32 | subcolumn=false)
c.a | SimpleAggregateFunction(sum, Nullable(Int64)) | subcolumn=true
c.a.null | UInt8 | subcolumn=true
c.b | Nullable(Int32) | subcolumn=true
c.b.null | UInt8 | subcolumn=true
g.a | SimpleAggregateFunction(anyLast, Array(Nullable(Int32))) | subcolumn=true
g.a.size0 | UInt64 | subcolumn=true
g.a.null | Array(UInt8) | subcolumn=true
g.b | Int32 | subcolumn=true
```
The sub-column rows for `c.a`, `c.b`, `g.a`, `g.b` match the Docker-free fixture rows, so no fixture change was needed.

## Test counts and server
- `DeclaredNullabilityTest` 17 (testUnit), `SimpleAggregateTupleRoundTripLiveTest` 2, `NullableTupleMatrixLiveTest` 30 (test), 0 failures, errors and skips.
- ClickHouse server `26.9.17.8` (`latest`), client V2.

## Decisions Made
- Unwrap at the entry of `collect` rather than only in the leaf branch (plan's reversible design choice, kept). Anchor count is 2 (definition plus one call).

## Deviations from Plan

**1. [Verify-script discrepancy, not a code change] The Task 1 commit-verify diff against the phase-start SHA is not empty for ClickHouseWriter.java**
- **Found during:** Task 1 verification
- **Issue:** `git diff --quiet ede0a45d HEAD -- ... ClickHouseWriter.java ...` fails because plan 19-04's commit `2a94e045` already changed a two-line comment in the TUPLE case (no code). This plan did not touch the file.
- **Resolution:** Ran the same diff against the gap-closure base `5fd09cb1` (the verification commit): it is empty for Table.java, ClickHouseWriter.java, the processing, state and kafka packages, ProxySinkTask.java, ColumnTest and TableTest (`UNCHANGED-SINCE-GAP-BASE`). Every other check in that command passed. The remaining part of the command (`git status --porcelain` clean for src, docs, anchors, ledger) was verified separately and is clean.
- **Files modified:** none

**Total deviations:** 1 (verify-script baseline, documentation only). **Impact:** none on the code or the prohibitions; this plan changed no protected file. 19-09 and 19-10 should diff against the gap base, or accept the comment-only 19-04 edit.

## Issues Encountered
None.

## Known Stubs
None.

## Threat Flags
None. The change sits inside the once-per-DESCRIBE pass; no new endpoint, auth path or schema surface.

## Next Phase Readiness
- 19-09 can add the wrapper edge pins, the mutation (removing the unwrap must turn the pins red) and the adjacent-suite regression run; the anchor row is in place for `verify-merge-invariants.sh 54e63648 HEAD`.

## Self-Check: PASSED
- `[ -f ]` on `SimpleAggregateTupleRoundTripLiveTest.java`: found. Commit `4e133d4e` is an ancestor of HEAD. `gsd-tools check evaluation-scope --plan 19-07 --commits-only` returns the commit.
- GAP-07-T1-TESTS-GREEN conditions met (17 / 2 / 30 cases, no failures); commit-set check: `src True anchor True forkdelta True ledger True commits 1 extra [] missing []`; the live class has no `Thread.sleep` or `count()` and one `COLS-10 CR-01` name; all three Java files report `IS CLEAN`.
