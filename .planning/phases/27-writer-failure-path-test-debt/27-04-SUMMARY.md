---
phase: 27-writer-failure-path-test-debt
plan: "04"
subsystem: writer
tags: [kafka-connect, clickhouse, refactor, comment-scrub, source-guard, wrt-19]

requires:
  - phase: 27-writer-failure-path-test-debt
    provides: plan 27-03 WRT-18 fix (E-07 pass, anchor_rows=82) and the value-free writer test scaffolding
provides:
  - ClassCastException failure text pinned exactly by ClickHouseWriterColumnContextTest.classCastFailureTextIsPinnedExactly, committed green before the writer changed
  - ClickHouseWriter derives failure metadata once through sourceTypeName(Data) and valueClassName(Data); the ClassCastException format is WRITE_TYPE_MISMATCH_FAILURE_FORMAT on the shared WRITE_FAILURE_COLUMN_PREFIX, text byte-identical
  - all 27 planning identifiers removed from src/main comments in two comment-only commits, each compiled
  - SourcePlanningIdGuardUnitTest, a Docker-free testUnit guard seen red on the unscrubbed tree and on a planted ID
  - fork-anchors row (83), two FORK-DELTA amendments, evidence E-08..E-10
affects: [27-05, 28, 29]

actuals:
  tokens: 6700
  tasks: 3
  commits: 6

plan_head_before: ba2dc2d4031a859e3993c24abb7e610a9a24267b
plan_head_after: 7548c06ea429a32c1620abe1c69649203167082e

tech-stack:
  added: []
  patterns:
    - "Pin first, refactor second: the exact text is committed green on the old writer, then the refactor commit runs the full suite on exactly its staged tree"
    - "Source guard as a Docker-free unit test with a pattern self-test (10 planning samples, 7 ordinary tokens) and a non-vacuity floor of 60 files"
    - "Comment-only scrub commits proven by a diff check that rejects any changed non-comment line, plus a compile on exactly the commit's tree"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/ci/SourcePlanningIdGuardUnitTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/main/java/com/clickhouse/kafka/connect/util/WithheldMessageException.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/ParentShape.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
    - src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java
    - src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterColumnContextTest.java
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - .planning/phases/27-writer-failure-path-test-debt/27-COMMAND-EVIDENCE.md

key-decisions:
  - "The guard is a unit test, not a verify-merge-invariants.sh addition: that script and its self-test stay read-only, and the rule has nothing to do with merge provenance."
  - "WRITE_TYPE_MISMATCH_FAILURE_FORMAT gets its own fork-anchors row (2), so an upstream sync that brings the inline format back fails the gate instead of silently reopening the drift risk."
  - "The two message builders stay separate and no failure text changed; merging them stays a deferred idea."

requirements-completed: [WRT-19]

coverage:
  - id: D1
    description: "The ClassCastException text is pinned exactly and the pin is green both before and after the refactor"
    requirement: "WRT-19"
    verification:
      - kind: unit
        ref: "ClickHouseWriterColumnContextTest#classCastFailureTextIsPinnedExactly"
        status: pass
    human_judgment: false
  - id: D2
    description: "ClickHouseWriter derives failure metadata in one place and the ClassCastException format is built on the shared prefix with every character unchanged"
    requirement: "WRT-19"
    verification:
      - kind: unit
        ref: "ClickHouseWriterColumnContextTest, WriterValueFreeFailureUnitTest (29), WriterFailureClassificationUnitTest (216); full suite 1662 tests, 0 failed on the refactor tree"
        status: pass
    human_judgment: false
  - id: D3
    description: "src/main carries no planning identifier on any line, and the guard fails on the unscrubbed tree and on a planted ID"
    requirement: "WRT-19"
    verification:
      - kind: unit
        ref: "SourcePlanningIdGuardUnitTest (18 tests); red run E-09 (27 hits), planted D-99 run E-10"
        status: pass
    human_judgment: false
  - id: D4
    description: "The two scrub commits change comment lines only and every anchored count is unchanged"
    requirement: "WRT-19"
    verification:
      - kind: other
        ref: "comment-only diff check on both commits (changed 25 and 29, non-comment []); verify-merge-invariants.sh anchor_rows=83 result=PASS"
        status: pass
    human_judgment: false
  - id: D5
    description: "The scrubbed comments still read correctly: each invariant sentence survives, and the wrapped prefix comment and the 'a future raw-name pairing' wording read well"
    verification: []
    human_judgment: true
    rationale: "Whether a comment still explains its invariant after losing its tag is a reading judgment no test asserts"

duration: 18min
completed: 2026-10-23
status: complete
---

# Phase 27 Plan 04: Failure metadata derived once, src/main scrubbed of planning IDs Summary

**ClickHouseWriter builds both write-failure texts from one prefix constant and two metadata helpers, with the ClassCastException text pinned byte for byte before and after; all 27 planning identifiers left src/main comments, and a Docker-free unit test keeps them out**

## Performance

- **Duration:** 18 min
- **Started:** 2026-10-23T00:27:34Z
- **Completed:** 2026-10-23T00:45:37Z
- **Tasks:** 3
- **Files modified:** 13 (9 under src/main, 2 test files, fork-anchors, FORK-DELTA, evidence)

## Accomplishments

- Pin first: `classCastFailureTextIsPinnedExactly` asserts the full message ``Failed to write column `effective_at`: cannot convert Kafka value of type STRING (value class: java.lang.String) to ClickHouse type UINT64`` and the `ClassCastException` cause. It was green on the writer as it stood (9 tests, 0 failed), so assumption A2 was not refuted.
- Refactor: `sourceTypeName(Data)` and `valueClassName(Data)` replace the two local derivations, and `WRITE_TYPE_MISMATCH_FAILURE_FORMAT` is `WRITE_FAILURE_COLUMN_PREFIX` plus the rest of the old literal. `String sourceType =` and `String valueClass =` went from 2 to 0, the helpers are defined once and called twice each, and the literal `"Failed to write column ` stays only in the prefix declaration. testUnit ran 1173 tests, 0 failed; the full suite on exactly the commit's tree ran 1662 tests, 0 failures, 0 errors, the 18 named skips, ClickHouse 26.9.19.9, client V2.
- Scrub: the guard's pattern found exactly 27 hits on the unscrubbed tree (ClickHouseWriter 11, SinkTaskStatistics 4, DescribeTableBuilder 4, ClickHouseHelperClient 2, DeclaredNullability 2, ParentShape 2, HeaderToValue 1, WithheldMessageException 1). Two comment-only commits (2 and 6 files) removed them with each invariant sentence kept; the long prefix comment is wrapped onto two lines; `WithheldMessageException` Javadoc lines 22-24 are untouched.
- Guard: `SourcePlanningIdGuardUnitTest` (18 tests) failed only on its scan test on the unscrubbed tree, passes after the scrub, and fails again naming `HeaderToValue.java:237` when `// planted (D-99)` is appended; the plant was reverted.
- At the end of the plan: testUnit 1191 tests, 0 failed; full suite on HEAD 1680 tests (1662 plus the 18 guard tests), 0 failures, 0 errors, 18 skipped; tail gate `anchor_rows=83 result=PASS`.

## Task Commits

1. **Task 1: Exact pin, then the behaviour-preserving refactor** - `c6b2101e` (test), `b78d8691` (refactor)
2. **Task 2: Guard seen red, then two comment-only scrubs** - `5c3eb2d1` (chore, writer path), `e50f3e7b` (chore, six other files); the guard file was held out of the tree while they were made
3. **Task 3: Guard commit, mutation proof, gate, evidence** - `cafd512c` (test), `7548c06e` (docs, E-08..E-10)

**Plan metadata:** the SUMMARY commit and the state-and-roadmap commit that follow this file.

## Files Created/Modified

- `src/main/java/.../sink/db/ClickHouseWriter.java` - helpers, format constant, scrubbed comments
- `src/main/java/.../util/WithheldMessageException.java`, `sink/db/helper/ClickHouseHelperClient.java`, `sink/db/helper/DescribeTableBuilder.java`, `sink/db/mapping/ParentShape.java`, `sink/db/mapping/DeclaredNullability.java`, `util/jmx/SinkTaskStatistics.java`, `transforms/HeaderToValue.java` - comment-only scrubs
- `src/test/java/.../sink/db/ClickHouseWriterColumnContextTest.java` - `CLASS_CAST_FAILURE_TEXT` and the exact pin
- `src/test/java/.../ci/SourcePlanningIdGuardUnitTest.java` - the standing guard
- `.planning/scripts/fork-anchors.txt` - row `WRITE_TYPE_MISMATCH_FAILURE_FORMAT` 2 (83 rows)
- `docs/FORK-DELTA.md` - the doWriteColValue row's ClassCastException sentence and Pinned-by cell, and a Test-harness row for the guard
- `.planning/phases/27-writer-failure-path-test-debt/27-COMMAND-EVIDENCE.md` - E-08, E-09, E-10

## Decisions Made

- Guard form, anchor row and builder separation as listed in `key-decisions`; all three follow the plan.
- The pin text was taken from the plan and confirmed against the writer's format applied to the same inputs (A2 not refuted), so no measured-text pin was needed.

## Deviations from Plan

### Auto-fixed Issues

None - the code and documents were executed as the plan wrote them.

### Environment and expectation deviations (no code change)

**1. [Test JVM] Two full-suite runs on the refactor tree died with the OpenJ9 GPF**
- **Found during:** Task 1 (full suite on the staged tree)
- **Issue:** Both default-JVM runs ended rc=1 after 1540 tests with 0 failed (`Gradle Test Executor 164 finished with non-zero exit value 255`). The javacore shows the known GPF in Mockito inline mocking (`Test worker` in `InstrumentationImpl.retransformClasses0`), at 28.7 of 29.7 GB swap used.
- **Fix:** core, javacore, Snap and jitdump files deleted one by one after each run (never `git clean`); the third run used the documented escape `-PjavaVersion=21` on the same staged tree (tree equality checked) and ended BUILD SUCCESSFUL. The end-of-plan full suite also used `-PjavaVersion=21`. Logs: `suite-27-04-run1-gpf.log`, `suite-27-04-run2-gpf.log`.
- **Impact:** none on the code; the suite evidence for the refactor tree and for HEAD is from JDK 21.

**2. [Expectation] Spotless numstat for ClickHouseWriter.java is 10/11, not 9/12**
- **Found during:** Task 3
- **Issue:** the plan expects `9<TAB>12`; the measured figure is `10<TAB>11`, as already recorded in E-05 and E-07.
- **Cause:** plan 27-02's used imports, not this plan, which adds no import. All other 45 lines equal the base and the new guard file does not appear. The expectation was not forced to match; the automated Spotless comparison in the plan's Task 3 verify therefore reads `10<TAB>11` and is recorded as such in E-10.

**3. [Count] Full-suite total is 1662, not 1661**
- The plan's 1661 baseline grows by the one new exact pin test; the plan's own floor (at least 1660) holds.

---

**Total deviations:** 0 auto-fixed; 3 environment or expectation notes
**Impact on plan:** none; behaviour, texts and anchors are as specified.

## Issues Encountered

- The OpenJ9 Mockito crash described above, handled with the project's documented escape.
- The refactor commit was staged before the full suite ran, so its tree is the tree the suite ran on; `git write-tree` equals the commit's tree.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None.

## Threat Flags

None - no new endpoint, auth path or trust-boundary surface; T-27-12 to T-27-15 are mitigated as planned (exact pins before and after the refactor, comment-only checks and compiles, the guard, and the new anchor row).

## Next Phase Readiness

- Plan 27-05 (DEBT-16 and the phase gate) can start: anchors are at 83 rows, the tail gate passes, and E-11..E-13 are still pending.
- Open for the user at the end of the phase: the planning IDs that remain in the test tree (D-11 limits that scrub to plan 27-02's file and defers the rest).

## Self-Check: PASSED

- Created files exist: `SourcePlanningIdGuardUnitTest.java` FOUND; modified `ClickHouseWriter.java` and `ClickHouseWriterColumnContextTest.java` FOUND.
- Commits in HEAD's history: `c6b2101e`, `b78d8691`, `5c3eb2d1`, `e50f3e7b`, `cafd512c`, `7548c06e` FOUND.
- `valueClassName(Data value)` is defined once in `ClickHouseWriter.java`; the PLANNING-ID mirror reports 0 hits in src/main; WT-ANCHORS 83 rows, none bad.

---
*Phase: 27-writer-failure-path-test-debt*
*Completed: 2026-10-23*
