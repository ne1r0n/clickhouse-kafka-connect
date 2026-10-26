---
phase: 27-writer-failure-path-test-debt
plan: "05"
subsystem: testing
tags: [junit5, kafka-connect, clickhouse, test-debt, phase-gate, mutation-proof]

requires:
  - phase: 27-writer-failure-path-test-debt
    provides: plan 27-04 refactor, planning-ID scrub and guard (E-10 pass, anchor_rows=83)
provides:
  - schemaWithFixedStringMismatchTest that fails with "nothing was thrown" when put succeeds, both FixedString assertions kept verbatim, stop() in a finally
  - CountingHelperClient without the unused useClientV2Calls counter; Javadoc and FORK-DELTA row updated
  - NullableTupleMatrixLiveTest premise row and Column lookups that fail with a message instead of passing vacuously or throwing a bare NullPointerException
  - the Phase 27 gate recorded in E-13 (testUnit, full suite, anchors invariant with self-test, Spotless, origin freeze) and the assertion diff check with its predicate-changes table
  - ledger row L-57, evidence E-11..E-13
affects: [28, 29]

actuals:
  tokens: 8161
  tasks: 3
  commits: 3

plan_head_before: 7a7247eea07d3a59092e0f1d1681a72512cc8190
plan_head_after: c77cbcf619e663e7f2ca77258a4d487de03ef924

tech-stack:
  added: []
  patterns:
    - "Mutation-first proof for vacuous tests: apply a mutated premise to the old code (green or bare NPE) and to the new code (fails with a message), revert, commit only the real edit"
    - "Phase gate with a count-based assertion diff check plus a separate table for predicate changes the counts cannot see"

key-files:
  created: []
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java
    - src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java
    - docs/FORK-DELTA.md
    - .planning/phases/27-writer-failure-path-test-debt/27-BEHAVIOUR-LEDGER.md
    - .planning/phases/27-writer-failure-path-test-debt/27-COMMAND-EVIDENCE.md

key-decisions:
  - "The Spotless expectation was recorded as measured, not forced: ClickHouseWriter.java 10/11 and ClickHouseSinkTaskWithSchemaTest.java 28/29, because the imports this phase adds are used and Spotless keeps and moves them; the plan's 9/12 and 27/29 hold only for unused imports."
  - "The default OpenJ9 JVM was tried first for the full suite and passed; no -PjavaVersion=21 run was needed in this plan."

requirements-completed: [DEBT-16, WRT-16, WRT-17, WRT-18, WRT-19]

coverage:
  - id: D1
    description: "schemaWithFixedStringMismatchTest fails when put does not throw and keeps both FixedString assertions"
    requirement: "DEBT-16"
    verification:
      - kind: unit
        ref: "ClickHouseSinkTaskWithSchemaTest#schemaWithFixedStringMismatchTest; widened FixedString: old form passes, new form fails with 'nothing was thrown' (E-11); unmutated passes"
        status: pass
    human_judgment: false
  - id: D2
    description: "CountingHelperClient.useClientV2Calls is removed without removing an assertion"
    requirement: "DEBT-16"
    verification:
      - kind: unit
        ref: "testUnit 1191 tests, 0 failed; grep -rn useClientV2Calls src finds nothing (E-11)"
        status: pass
    human_judgment: false
  - id: D3
    description: "NullableTupleMatrixLiveTest premises fail with a message under a mutated premise"
    requirement: "DEBT-16"
    verification:
      - kind: integration
        ref: "NullableTupleMatrixLiveTest, 33 tests; MUT-A old passes, new fails with 'DESCRIBE returned no row named'; MUT-B old NullPointerException, new 'expected: not <null>' (E-12)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The phase gate holds: testUnit, a full suite on latest x V2, the anchors invariant with its self-test, an unmoved origin/custom_build, and no existing assertion deleted or weakened"
    requirement: "WRT-16, WRT-17, WRT-18, WRT-19"
    verification:
      - kind: other
        ref: "E-13: testUnit 1191/0 failed; suite 1680 tests, 0 failures, 0 errors, 18 skipped; gate anchor_rows=83 result=PASS; selftest no failures; assertion diff check ASSERTION-DIFF-CLEAN"
        status: pass
    human_judgment: false
  - id: D5
    description: "The widened JsonAsStringKeyResidualLiveTest.assertJacksonRouteReached predicate still requires the Jackson class identity and rests on the user's sign-off in E-04"
    verification: []
    human_judgment: true
    rationale: "The end-of-phase human check in the plan: the user reads E-13's predicate-changes table, confirms E-04 holds their own answer, and that no assertion was loosened beyond the listed rows"

duration: 22min
completed: 2026-10-23
status: complete
---

# Phase 27 Plan 05: Test debt closed and the Phase 27 gate Summary

**schemaWithFixedStringMismatchTest now fails when put does not throw, the dead CountingHelperClient counter is gone, the NullableTupleMatrixLiveTest premises fail with a message under a mutated name, and the whole phase passed testUnit, a 1680-test full suite, the anchors invariant with its self-test and the assertion diff check**

## Performance

- **Duration:** 22 min
- **Started:** 2026-10-23T00:47:37Z
- **Completed:** 2026-10-23T01:10:01Z
- **Tasks:** 3
- **Files modified:** 6 (3 test files, FORK-DELTA, ledger, evidence)

## Accomplishments

- DEBT-16 / 22-REVIEW IN-04: with the table's FixedString widened to the data size, the old catch-only form passed (vacuous) and the new `assertThrows` form failed with `Expected java.lang.RuntimeException to be thrown, but nothing was thrown.`; unmutated it passes on ClickHouse 26.9.19.9, client V2. The two FixedString assertions are word for word as before; `chst.stop()` moved into a `finally`.
- DEBT-16 / 22-REVIEW IN-05 (test half): `useClientV2Calls` (field, increment, getter) removed; `isUseClientV2` still returns the constructor flag; the class Javadoc (re-wrapped to 100 columns) and the FORK-DELTA test-doubles row no longer say it counts `isUseClientV2`. `grep -rn useClientV2Calls src` went from 4 lines to none.
- DEBT-16 / 23-REVIEW IN-03: the DESCRIBE premise row is collected and `assertNotNull(cellRow, PREMISE_MESSAGE + ": DESCRIBE returned no row named " + CELL_COLUMN)` runs before the type comparison; the two `columns.get(...)` lookups and the `getAllColumnsMap()` lookup are each wrapped in `assertNotNull` naming the key. Four mutated runs: MUT-A old 3 pass (vacuous), MUT-A new 2 Twin rows fail with the premise message; MUT-B old 3 NullPointerException, MUT-B new 3 `expected: not <null>`. Whole class 33 tests, 0 failures.
- Phase gate (E-13): testUnit 1191 tests, 0 failed; full suite 1680 tests, 0 failures, 0 errors, the 18 named skips, 97 XML files, one server version, exact class counts (216, 29, 4, 33, 37, 9, 18, 39); tail gate `anchor_rows=83 result=PASS` across 96 anchor commits; self-test exit 0, 16 PASS, 0 FAIL, `selftest: no failures`; both gate scripts equal `baseline-v1.7.0_gg4`; `origin/custom_build` still `69cd680b...` (E-01), no `sync/v1.7.0` on origin.
- Assertion diff check: removals only in the five allowed files, no `@Test`/`@ParameterizedTest` count lowered, no assert count lowered outside the sanctioned flip (JsonAsStringKeyResidualLiveTest 32 -> 31, the removed key-present helper), `WriterValueFreeFailureUnitTest` loses exactly the old `NO_ENUM_CONTENT_MESSAGE` line, and the predicate-changes table lists rows (a) to (e) with the widened `assertJacksonRouteReached` as the only wider predicate.

## Task Commits

1. **Task 1: FixedString mismatch red proof and counter removal (E-11)** - `71b5d102` (test)
2. **Task 2: premises fail with a message (E-12)** - `05ab0e3e` (test)
3. **Task 3: phase gate and assertion diff check (E-13), ledger L-57** - `c77cbcf6` (docs)

**Plan metadata:** the SUMMARY commit and the state-and-roadmap commit that follow this file.

## Files Created/Modified

- `src/test/java/.../sink/ClickHouseSinkTaskWithSchemaTest.java` - `assertThrows` form, `finally`, new static import
- `src/test/java/.../sink/db/CountingHelperClient.java` - counter removed, Javadoc updated
- `src/test/java/.../sink/NullableTupleMatrixLiveTest.java` - `cellRow`, `cell`, `element`, `nested` checked variables
- `docs/FORK-DELTA.md` - test-doubles row clause
- `.planning/phases/27-writer-failure-path-test-debt/27-BEHAVIOUR-LEDGER.md` - L-57
- `.planning/phases/27-writer-failure-path-test-debt/27-COMMAND-EVIDENCE.md` - E-11, E-12, E-13

## Decisions Made

- The mutation-first proof shape followed the plan: MUT-A and MUT-B were applied to the old code and to the new code, reverted with `git checkout --` or a restore from a saved copy, and `grep -cF '_x'` on the file is 0.
- No mutation was committed; no existing assertion was deleted or weakened; no `src/main` file changed in this plan.

## Deviations from Plan

### Auto-fixed Issues

None - the code and documents were executed as the plan wrote them.

### Expectation deviations (no code change)

**1. [Expectation] Spotless numstat reads ClickHouseWriter.java 10/11 and ClickHouseSinkTaskWithSchemaTest.java 28/29, not 9/12 and 27/29**
- **Found during:** Task 3
- **Cause:** the plan's figures hold only when the added imports are unused, because Spotless then deletes them. Both are used, so Spotless keeps them and moves the import block as one hunk. ClickHouseWriter.java carries plan 27-02's used imports (already in E-05, E-07, E-10); the schema test's `assertThrows` import is this plan's. Re-measured on the HEAD archive to confirm the hunk.
- **Impact:** the plan's literal Spotless check in Task 3 cannot pass; the equivalent check with the measured two lines passes (46 lines, no other difference). The 46 pre-existing violations are the same set and no new violation appears.

**2. [Count] Full suite is 1680 tests, above the plan's floor of 1678**
- The plan's floor is a lower bound; the 1680 is 1662 plus the 18 guard tests from plan 27-04.

**3. [Process] Default JVM passed the full suite**
- The OpenJ9 GPF seen in 27-04 did not recur; the default JVM ran the gate suite once, BUILD SUCCESSFUL in 2m 38s. No core or javacore files were left to delete.

---

**Total deviations:** 0 auto-fixed; 3 expectation or environment notes
**Impact on plan:** none; behaviour, assertions and anchors are as specified.

## Issues Encountered

- The first draft of the CountingHelperClient Javadoc exceeded 100 columns; it was re-wrapped before the commit and testUnit was re-run on the final text.
- The first timestamps typed into E-11 and E-12 were ahead of the clock; they were replaced with the real time before the evidence commit.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None.

## Threat Flags

None - test-only edits plus planning documents; no new endpoint, auth path or trust-boundary surface. T-27-16 to T-27-19 are mitigated as planned (mutation proofs, the diff check and its human check, `--rerun` with a cleared result directory and the start-time check, and a read-only origin freeze check).

## Next Phase Readiness

- Phase 27 is complete: all five plans have summaries, E-01..E-13 read pass, and nothing was pushed.
- Phase 28 gets an oracle whose premises fail loudly: a premise lookup by the old name in NullableTupleMatrixLiveTest fails with a message after a name decode; the COLS-13 twins are otherwise unchanged (33 tests).
- Open for the user at the end of the phase: the end-of-phase human check of E-13's predicate-changes table (especially row (a), the widened `assertJacksonRouteReached` resting on the E-04 sign-off), and the planning IDs that remain in the test tree (D-11 deferred that scrub).

## Self-Check: PASSED

- Modified files exist: the three test files, `docs/FORK-DELTA.md`, the ledger and the evidence file FOUND.
- Commits in HEAD's history: `71b5d102`, `05ab0e3e`, `c77cbcf6` FOUND.
- PHASE27-GATE-PASS and ASSERTION-DIFF-CLEAN printed; `grep -rn useClientV2Calls src` empty.

---
*Phase: 27-writer-failure-path-test-debt*
*Completed: 2026-10-23*
