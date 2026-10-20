---
phase: 19-nullable-enum-tuple-mapping
plan: "01"
subsystem: testing
tags: [clickhouse, nullable-tuple, rowbinary, twin-tables, spike, junit5, testcontainers]

requires:
  - phase: 18-hardening-follow-through
    provides: CapturingClickHouseWriter seam, RowSnapshots typed read-back, LogCapture, the nested-null write contract
provides:
  - NullableTupleMatrixLiveTest, a committed live twin-table matrix for Nullable(Tuple) (COLS-09), two sides per cell
  - NullableRoundTrip, shared live round-trip helpers for the Phase 19 classes
  - 19-SPIKE.md, the D-04/D-08 go/no-go bundle with a GO recommendation
affects: [19-02, 19-03, 19-04, COLS-10, COLS-11]

actuals:
  tokens: 17300
  tasks: 3
  commits: 3

plan_head_before: 95b623f0e0bffde2e13ffeab310631c123d4fd3f
plan_head_after: b53afc0200fde6b81fb04a0a8a798168c6ecf486
commits: 3

tech-stack:
  added: []
  patterns:
    - "Twin-table matrix: every cell runs on a plain table (RowBinary) and a table with a DEFAULT column (RowBinaryWithDefaults), and the precondition is asserted via describeTable(...).hasDefaults()"
    - "Two sides per cell: side (a) pins the DESCRIBE-derived outcome through ClickHouseSinkTask.put; side (b) serializes with the unchanged writer over a hand-built Table with declared nullability and reads back with typed SQL"
    - "Observe-then-pin: a throwaway probe recorded each row kind's real failure text or wrong read-back before the expectations were encoded"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/NullableRoundTrip.java
    - src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java
    - .planning/phases/19-nullable-enum-tuple-mapping/19-SPIKE.md
  modified: []

key-decisions:
  - "Recommendation is GO: every deciding cell (C1, C2, C3) round-trips on side (b) on both twins and the approach test passed; the user's own answer is still taken at the 19-03 gate"
  - "Side (a) outcomes are pinned as observed, not as the research predicted (CANNOT_READ_ALL_DATA, not ATTEMPT_TO_READ_AFTER_EOF)"
  - "Expect.storesWrong carries a phantom-row count so the silent plain-twin C2 corruption is pinned including its extra row"

patterns-established:
  - "Spike as a committed live class (D-05): pre-fix assertions are the red record and flip on go"
  - "A refused-by-the-declared-writer expectation (RowCase.declaredWriterRejects) for rows that have no correct read-back"

requirements-completed: [COLS-09]

coverage:
  - id: D1
    description: "Live twin-table matrix NullableTupleMatrixLiveTest: C1-C3 deciding and X1-X3 informational cells, both twins, both sides, plus the double-flag control"
    requirement: COLS-09
    verification:
      - kind: integration
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests '*NullableTupleMatrixLiveTest' --rerun (28 tests, 0 failures)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Approach evidence: every matrix root plus the multi-line, unnamed and backtick-named tuples parse with ClickHouseColumn.of after normalisation and carry the DDL's nullability"
    requirement: COLS-09
    verification:
      - kind: integration
        ref: "NullableTupleMatrixLiveTest#declaredTupleTypesParseWithDdlNullability"
        status: pass
    human_judgment: false
  - id: D3
    description: "19-SPIKE.md go/no-go bundle with the recommendation derived from its own deciding rows"
    requirement: COLS-09
    verification:
      - kind: other
        ref: "Task 3 SPIKE-BUNDLE-OK structural check"
        status: pass
    human_judgment: true
    rationale: "The go/no-go decision itself belongs to the user at the 19-03 gate (D-01); the bundle only recommends"
  - id: D4
    description: "No production code lands before the gate"
    requirement: COLS-09
    verification:
      - kind: other
        ref: "git diff --quiet ede0a45d HEAD -- src/main"
        status: pass
    human_judgment: false

duration: 14min
completed: 2026-10-12
status: complete
---

# Phase 19 Plan 01: Tuple spike matrix and go/no-go bundle Summary

**A committed live twin-table matrix proves the unchanged writer round-trips every Nullable(Tuple) shape once element nullability is declared correctly, shows the DESCRIBE-derived mapping fails or silently corrupts the same rows today, and recommends GO.**

## Performance

- **Duration:** about 14 min (Task 1 about 6 min in the prior context, Tasks 2-3 and close-out about 8 min)
- **Started:** 2026-10-12T20:20Z (approximate, Task 1)
- **Completed:** 2026-10-12T20:36Z
- **Tasks:** 3
- **Files created:** 3 (two test classes, one planning bundle); `src/main` untouched

## Accomplishments

- `NullableTupleMatrixLiveTest` covers C1 `Nullable(Tuple(a Int32, b String))`, C2 `Nullable(Tuple(a Nullable(Int32), b String))`, C3 `Array(Nullable(Tuple(a Int32, b Nullable(String))))` (deciding) and X1 single-field, X2 `Map(String, Nullable(Tuple(...)))`, X3 tuple-in-tuple (informational). Each runs on both twins and both sides: 28 cases, 0 failures.
- Side (b) round-trips for every cell on both twins, with `d` = 42 on the DEFAULT twin, a NULL beside a non-null row in one batch, a NULL array element between two intact neighbours, an empty array and a lone-NULL array. X1 id 3 is refused by the declared writer before any insert.
- The control (C1 with both fields marked nullable, the double flag DESCRIBE produces today) fails on both twins and leaves no row, so side (b) can fail.
- Approach evidence passed for all roots: C1-C3, X1-X3, the plain and nested multi-line tuples (`p1`, `p2`, raw form carries the literal backslash-n), the unnamed `p6` (matched by position) and the backtick-named `p8` (names holding a space and a comma).
- `19-SPIKE.md` holds the bundle and `**Recommendation:** GO`; `git diff ede0a45d HEAD -- src/main` is empty.

## Server

`NULLABLE_TUPLE_MATRIX server version=26.9.17.8`. Planning saw 26.9.16.11, so the floating `latest` image moved. No server setting was set in any DDL or query.

## Side (a) observation table (DESCRIBE-derived mapping today)

| Cell | Row | PLAIN twin | DEFAULT twin |
|------|-----|-----------|--------------|
| C1 | id 1 {a, b} | fails: server CANNOT_READ_ALL_DATA (Bytes read 2, expected 4) | fails: CANNOT_READ_ALL_DATA (Bytes read 0, expected 1) |
| C1 | id 2 NULL tuple | fails in writer: An attempt to write null into not nullable column 'c' | same |
| C2 | id 1 {a, b} | stores wrong: tuple not NULL, a NULL, b empty, plus 1 phantom row; no failure | fails: CANNOT_READ_ALL_DATA (Bytes read 0, expected 1) |
| C2 | id 2 NULL tuple | fails in writer (not nullable column 'c') | same |
| C2 | id 3 {a NULL, b} | fails: CANNOT_READ_ALL_DATA (Bytes read 3, expected 4) | same |
| C3 | id 1 [tuple, NULL, tuple] | fails in writer: column 'c' (Array(Tuple(...))) element: null in non-nullable slot | same |
| C3 | id 2 [] | round-trips | round-trips |
| C3 | id 3 [NULL] | fails in writer (array element, same text) | same |
| X1 | id 1 {a} | round-trips (single-field coincidence) | round-trips |
| X1 | id 2 NULL tuple | fails in writer (not nullable column 'c') | same |
| X1 | id 3 {a NULL} | stores wrong: a NULL tuple, no failure | same |
| X2 | id 1 {k1: tuple, k2: NULL} | fails in writer: column 'c' (Map(STRING, Tuple(...))) map value: null in non-nullable slot | same |
| X2 | id 2 {} | round-trips | round-trips |
| X3 | id 1 {a, n: {d, e NULL}} | fails: CANNOT_READ_ALL_DATA (Bytes read 2, expected 4) | fails: CANNOT_READ_ALL_DATA (Bytes read 0, expected 1) |
| X3 | id 2 NULL tuple | fails in writer (not nullable column 'c') | same |

Every failing put left no row behind. Side (b) round-tripped on both twins for C1, C2 and C3 (the deciding cells) and for X1, X2 and X3.

## Task Commits

1. **Task 1 (tracer): C1 on both twins and both sides, control, approach evidence, version log** - `fc44b1f6` (test)
2. **Task 2: C2, C3, X1-X3 and the extended approach evidence** - `5d4424c7` (test)
3. **Task 3: 19-SPIKE.md bundle and recommendation** - `b53afc02` (docs)

**Plan metadata:** recorded by the closing docs commit (SUMMARY, STATE, ROADMAP, REQUIREMENTS).

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/NullableRoundTrip.java` - shared live helpers: Twin, Cell, createTable, putAndStop, selectAndAssert, assertPutFails, insertCaptured, describeRows, normaliseDeclaredType, serverVersion
- `src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java` - the matrix, control, approach and version tests
- `.planning/phases/19-nullable-enum-tuple-mapping/19-SPIKE.md` - the go/no-go bundle

## Decisions Made

- Recommend GO: all deciding side (b) cells round-trip on both twins and the approach test passed. The answer is the user's at the 19-03 gate.
- Pin side (a) as observed. Two research predictions were off: the server error is `CANNOT_READ_ALL_DATA` rather than `ATTEMPT_TO_READ_AFTER_EOF`, and C2 id 1 on the plain twin is a silent wrong write with a phantom row rather than a failure.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] Per-twin side (a) outcomes and a phantom-row check**
- **Found during:** Task 2 (observation probe)
- **Issue:** the plan keyed side (a) outcomes by row id only, but C2 id 1 differs by twin (plain stores wrong, DEFAULT fails), and the plain-twin write also leaves an extra row under an id no record carries, which an id-filtered read-back would hide.
- **Fix:** `MatrixCell.currentOutcome` is keyed by twin then row id; `Expect.storesWrong` carries a phantom-row count; the STORES_WRONG branch also asserts the DEFAULT twin's `d` = 42 and the count of rows under foreign ids.
- **Files modified:** `NullableTupleMatrixLiveTest.java`
- **Verification:** 28 of 28 cases pass; the phantom row was seen in the probe (a second row under an id outside the cell's ids).
- **Commit:** `5d4424c7`

**2. [Rule 3 - Blocking] Map width check uses `length(mapKeys(c))`**
- **Found during:** Task 2
- **Issue:** the plan wrote `length(c)` for Map cells; the portable form that cannot depend on `length` supporting Map is the key count.
- **Fix:** X2 read-backs use `length(mapKeys(c))` (same expected values 2 and 0).
- **Commit:** `5d4424c7`

**3. [Plan extension] Extra approach root `p2`**
- **Found during:** Task 2
- **Issue:** the plan's multi-line encoding edge named only `p1`; a nested non-nullable tuple is the harder multi-line shape.
- **Fix:** added `p2 Tuple(a Int32, n Tuple(d String, e Nullable(Int32)))` to the approach test and asserted both `p1` and `p2` reach the parser in the escaped multi-line form. It passed.
- **Commit:** `5d4424c7`

**Total deviations:** 3 (1 missing-critical, 1 blocking-portability, 1 additive coverage). **Impact:** none on scope; all additions are inside the planned test files, and the phantom-row finding strengthens the GO case evidence.

## Execution Notes

- The plan's execution note asked for a context hand-off after Task 2. The orchestrator's dispatch instead asked this continuation to run Tasks 2 and 3 and the close-out in one context, which it did; the commit-body hand-off for Task 2 was still written.
- A throwaway probe method (never committed) recorded each side (a) row outcome before the expectations were encoded; it was removed before the Task 2 commit.

## Issues Encountered

None blocking. The `latest` image drifted from 26.9.16.11 to 26.9.17.8 between planning and execution; all pins were observed on 26.9.17.8.

## Known Stubs

None.

## Threat Flags

None. No new network, auth or trust-boundary surface; the matrix uses synthetic ASCII literals and logs catalog metadata only (T-19-02), every test drops its tables in `finally` (T-19-03), and Task 3's structural check recomputes the recommendation from the bundle's own deciding rows (T-19-01).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- 19-02 (Enum fix) is independent of this spike and can proceed.
- The 19-03 gate has what it needs: `19-SPIKE.md` recommends GO with approach C (fail open), and lists the visible behaviour edges FORK-DELTA must state on go (X1 with a NULL `a` becomes a `DataException`; an omitted `Nullable(Tuple)` field now passes validation and writes NULL).
- COLS-09 stays open in spirit until the user's go/no-go answer is recorded at the 19-03 gate.

---
*Phase: 19-nullable-enum-tuple-mapping*
*Completed: 2026-10-12*

## Self-Check: PASSED

Created files exist (NullableRoundTrip.java, NullableTupleMatrixLiveTest.java, 19-SPIKE.md); commits fc44b1f6, 5d4424c7 and b53afc02 are ancestors of HEAD; `check evaluation-scope` resolves 3 commits; the matrix class ran 28 of 28 green; both test files are `IS CLEAN`; `git diff ede0a45d HEAD -- src/main` is empty.
