---
phase: 23-declared-nullability-hardening-review-closure
plan: "01"
subsystem: testing
tags: [java, junit5, declared-nullability, test-cleanup, review-closure]

requires:
  - phase: 19-nullable-enum-tuple-mapping
    provides: DeclaredNullability.normalise and the 19-REVIEW IN-01 / IN-02 findings this plan closes
provides:
  - DeclaredNullability.ESCAPED_NEWLINE is the single public definition of the backslash-n literal
  - NullableTupleMatrixLiveTest normalises with the production method directly
  - ClickHouseHelperClientForkUnitTest constants block (DATABASE, TABLE, DISCOVERED_TABLE, WARN_LEVEL)
  - 23-EVIDENCE.md execution index E-01..E-07 and entry E-01
affects: [23-04 COLS-13 (adds no fourth copy of the literal), 23-03 DEBT-10, 23-06 phase gate]

actuals:
  tokens: 4000
  tasks: 2
  commits: 3
plan_head_before: 360644e595d28f1aafde1b01d897c829051c76f8
plan_head_after: 4d59d001e70d34a8aa2be343ded381f199ac2bd3

tech-stack:
  added: []
  patterns:
    - "A test needing a production literal reads the production constant instead of keeping an equal copy"

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/SimpleAggregateTupleRoundTripLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/NullableRoundTrip.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
    - .planning/phases/23-declared-nullability-hardening-review-closure/23-EVIDENCE.md

key-decisions:
  - "ESCAPED_NEWLINE is made public rather than duplicated, so exactly one definition exists and no test can drift from production; independence from production rests on the D-12 live premise, where the server is the oracle"
  - "The only src/main change in the plan is the one-line visibility change (numstat 1 1)"

patterns-established:
  - "Constants block above the first @Test in unit-test classes instead of mid-class declarations"

requirements-completed: [DEBT-11]

coverage:
  - id: D1
    description: "The backslash-n literal is defined once, in production, and every test that needs it reads DeclaredNullability.ESCAPED_NEWLINE"
    requirement: DEBT-11
    verification:
      - kind: unit
        ref: "./gradlew testUnit (795 tests, 793 succeeded, 0 failed, 2 skipped)"
        status: pass
      - kind: integration
        ref: "SimpleAggregateTupleRoundTripLiveTest (2 testcases) and NullableTupleMatrixLiveTest.declaredTupleTypesParseWithDdlNullability on ClickHouse latest x client V2"
        status: pass
    human_judgment: false
  - id: D2
    description: "The normalise pass-through in NullableRoundTrip is gone and the mid-class test constants sit in a constants block above the first @Test"
    requirement: DEBT-11
    verification:
      - kind: unit
        ref: "ClickHouseHelperClientForkUnitTest XML tests=45 failures=0; grep checks D15-STATIC-OK"
        status: pass
      - kind: integration
        ref: "NullableTupleMatrixLiveTest.declaredTupleTypesParseWithDdlNullability on latest x V2"
        status: pass
    human_judgment: false

duration: 15min
completed: 2026-10-20
status: complete
---

# Phase 23 Plan 01: Shared Backslash-n Literal and Test Cleanups Summary

**DeclaredNullability.ESCAPED_NEWLINE is now the only definition of the backslash-n literal (public, replacing three test copies), the one-line normalise pass-through is gone, and ClickHouseHelperClientForkUnitTest gets a constants block (DEBT-11, 19-REVIEW IN-01 and IN-02)**

## Performance

- **Duration:** 15 min
- **Started:** 2026-10-20T11:26:00Z
- **Completed:** 2026-10-20T11:41:30Z
- **Tasks:** 2
- **Files modified:** 7 (6 source, 1 evidence)

## Accomplishments
- The literal `"\\n"` is defined once under src (`grep -rnF '= "\\n";' src` prints one line; baseline 4). The copies in NullableTupleMatrixLiveTest, SimpleAggregateTupleRoundTripLiveTest and DeclaredNullabilityTest are gone; every former use reads `DeclaredNullability.ESCAPED_NEWLINE` with its assertion message and position unchanged.
- `NullableRoundTrip.normaliseDeclaredType` and its now-unused import are removed; the approach test calls `DeclaredNullability.normalise(row.getType())` directly (0 references to the pass-through, baseline 2).
- `ClickHouseHelperClientForkUnitTest` opens with `DATABASE`, `TABLE`, `DISCOVERED_TABLE = DATABASE + "." + TABLE` and `WARN_LEVEL`; `discover(...)` passes `DATABASE, TABLE`; the class still reports 45 testcases with 0 failures.
- Scope held: `git diff --numstat 16e0b2d6 -- src/main` is exactly `1 1 .../DeclaredNullability.java`. Whole-tree spotless numstat equals the 46-file baseline (md5 f2e32c7b...) and the anchors mirror reads rows=65 bad=0.
- Live premises on ClickHouse latest x client V2: SimpleAggregateTupleRoundTripLiveTest 2/2 and NullableTupleMatrixLiveTest.declaredTupleTypesParseWithDdlNullability 1/1, both after the D-14 and again (matrix test) after the D-15 commit.

## Task Commits

1. **Task 1: shared backslash-n literal (D-14, tracer)** - `4478016a` (refactor)
2. **Task 2: drop the pass-through and move the mid-class constants (D-15)** - `252cf8f3` (refactor)
3. **Task 2: open E-01 in 23-EVIDENCE.md** - `4d59d001` (docs)

**Plan metadata:** the docs(23-01) completion commit that carries this file, STATE.md and ROADMAP.md.

## Files Created/Modified
- `src/main/java/.../sink/db/mapping/DeclaredNullability.java` - ESCAPED_NEWLINE made public (one-line change)
- `src/test/java/.../sink/db/mapping/DeclaredNullabilityTest.java` - private copy removed, uses the production constant
- `src/test/java/.../sink/NullableTupleMatrixLiveTest.java` - copy removed, import added, direct `DeclaredNullability.normalise` call
- `src/test/java/.../sink/SimpleAggregateTupleRoundTripLiveTest.java` - copy removed, root premise reads the production constant
- `src/test/java/.../sink/NullableRoundTrip.java` - pass-through and import removed
- `src/test/java/.../sink/db/helper/ClickHouseHelperClientForkUnitTest.java` - constants block, `discover(...)` shares DATABASE/TABLE
- `.planning/phases/23-declared-nullability-hardening-review-closure/23-EVIDENCE.md` - Index rows E-01..E-07 and entry E-01

## Decisions Made
- Followed the plan as specified (D-14, D-15); no new decisions.

## Deviations from Plan

None - plan executed exactly as written.

The tracer feedback gate was met by the Task 1 `<verify>` chain (D14-STATIC-OK, TESTUNIT-GREEN, D14-LIVE-GREEN, D14-COMMITTED) before Task 2 started, so expansion proceeded.

## Issues Encountered
- None blocking. The throwaway-copy anchors loop uses a variable named `path`, which zsh ties to `PATH`; it was re-run under `bash` and printed `rows=65 bad=0` (an execution-shell quirk, not a repository issue).
- The OpenJ9 exit-255 GPF did not occur; each testUnit run was single and green.

## Known Stubs

None.

## Threat Flags

None. Making `ESCAPED_NEWLINE` public exposes an immutable String constant only (T-23-02, accepted).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Plan 23-02 (COLS-12) can proceed; the shared literal exists, so COLS-13 adds no fourth copy.
- Evidence entries E-02..E-07 are pending rows in the index.

## Self-Check: PASSED

- Modified files exist; commits `4478016a`, `252cf8f3`, `4d59d001` are ancestors of HEAD.
- Acceptance criteria re-run: D14-STATIC-OK, TESTUNIT-GREEN, D14-LIVE-GREEN, D14-COMMITTED, D15-STATIC-OK, D15-GREEN, SPOTLESS-BASELINE-EQUAL, E01-RECORDED.

---
*Phase: 23-declared-nullability-hardening-review-closure*
*Completed: 2026-10-20*
