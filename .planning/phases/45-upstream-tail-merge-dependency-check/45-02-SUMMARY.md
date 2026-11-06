---
phase: 45-upstream-tail-merge-dependency-check
plan: "02"
subsystem: upstream-sync
tags: [u-2, upstream-tests, test-only-edits, measurement, issue-857, evidence]

requires:
  - phase: 45-upstream-tail-merge-dependency-check
    provides: plan 45-01's merge commit 12e2cf17, E-01 baseline totals, E-04 bare-merge break inventory
provides:
  - commit c3946803, ClickHouseWriterRecordContextTest compiles on the fork (two constructor sites, test-only)
  - commit 0f4e45bd, FieldDescriptorEscapeTest#preservesEscapedCommentsAndDefaultExpressions asserts the fork's literal-JSON comment and default_expression
  - E-05, testUnit red on exactly one method between the edits and green after, and a full suite red on exactly WRT-10 (baseline + 15 tests, 18 named skips)
  - E-06, the #857 measurement over 15 writer failure shapes, taken before any code change
affects: [45-03, 45-05, 45-06]

actuals:
  tokens: 6500
  tasks: 2
  commits: 4
plan_head_before: bb49e04bc4c1dd733a7259c4c2edf98524c488a7
plan_head_after: f6ecadc85ef2faa15220260fcf970a56f9870e0a

tech-stack:
  added: []
  patterns: [two-edit U-2 repair each in its own commit, detached-worktree throwaway probe with a masked sentinel]

key-files:
  created: []
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterRecordContextTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/FieldDescriptorEscapeTest.java
    - .planning/phases/45-upstream-tail-merge-dependency-check/45-COMMAND-EVIDENCE.md

key-decisions:
  - "U-2 executed as locked: two named test-only edits, no @Disabled, no SinkTaskStatistics(int) constructor and no src/main change since the merge commit"
  - "The unused java.io.IOException import in upstream's RecordContextTest blob is left as merged: Spotless would drop it, but the plan bounds the diff against upstream to the two constructor lines"

requirements-completed: [UPS-02, UPS-04]

coverage:
  - id: D1
    description: "ClickHouseWriterRecordContextTest compiles against the fork's SinkTaskStatistics(String, int): exactly its two new SinkTaskStatistics(0) sites become (null, 0), in its own commit; numstat against upstream's blob is 2 2; all four compile tasks pass"
    requirement: UPS-04
    verification:
      - kind: other
        ref: "P45-02-T1-OK (Task 1 verify block): numstat 2 2, only the one file in the commit, src/main unchanged since M"
        status: pass
    human_judgment: false
  - id: D2
    description: "FieldDescriptorEscapeTest's one method asserts the fork's literal JSON for default_expression and comment, in its own commit; testUnit was red on exactly that method before and is green after (1837 tests = 1826 + 11, 2 skipped); the diff against upstream removes exactly the two assertion lines"
    requirement: UPS-04
    verification:
      - kind: other
        ref: "P45-02-T1-OK (Task 1 verify block) and E-05"
        status: pass
    human_judgment: false
  - id: D3
    description: "On the repaired merge the full suite on ClickHouse latest x client V2 has 2440 tests (2425 + 15), the 18 named skips and exactly one red testcase, WriterErrorPathLiveTest WRT-10, so plan 45-03's repair is attributable"
    requirement: UPS-02
    verification:
      - kind: other
        ref: "E-05 suite_* facts, helper `failed --expect` and `skips` checks"
        status: pass
    human_judgment: false
  - id: D4
    description: "The #857 measurement (UPS-02, measured first): 15 shapes through writeRecordsRowBinary and a direct doWriteCol call recording exception class, message, ERROR log, DLQ headers and the four retry outcomes; the probe lived only in a detached worktree, was never committed, and the evidence carries no sentinel"
    requirement: UPS-02
    verification:
      - kind: other
        ref: "P45-02-T2-OK (Task 2 verify block) and E-06"
        status: pass
    human_judgment: false

duration: 12min
completed: 2026-11-04
status: complete
---

# Phase 45 Plan 02: U-2 Upstream-Test Repair and the #857 Measurement Summary

**Upstream's two tests are kept green under the fork's design by two minimal test-only edits (a two-line constructor fix and the literal-JSON expectation for `comment` and `default_expression`), the merged tree's only remaining red is WRT-10, and a throwaway worktree probe measured that #857 leaks a record value on exactly one surface-set (the Jackson-over-socket-timeout shape) while moving no retry outcome.**

## Performance

- **Duration:** 12 min (the two full-suite runs, 164 s each, are most of it)
- **Started:** 2026-11-04T20:12:41Z
- **Completed:** 2026-11-04T20:23:00Z
- **Tasks:** 2
- **Files modified:** 2 test files, 1 evidence file

## Accomplishments

- **Commit A (`c3946803`):** `new SinkTaskStatistics(0)` becomes `new SinkTaskStatistics(null, 0)` at the two sites of `ClickHouseWriterRecordContextTest`; `compileJava`, `compileTestJava`, `compileTestFixturesJava` and `compileIntegrationTestJava` all pass; numstat against upstream's `83808df9` blob is `2 2`. No constructor was added to main.
- **testUnit between the commits:** 1837 tests (baseline 1826 + 11 upstream), 1 failure, exactly `FieldDescriptorEscapeTest#preservesEscapedCommentsAndDefaultExpressions` with the predicted `expected: <'a\\b'> but was: <'a\\\\b'>`.
- **Commit B (`0f4e45bd`):** that method's `getDefaultExpression()` and `getComment()` expectations are now the literal JSON text (four backslashes in the default expression; `\\`, `\"`, `\n`, `\t` kept as backslash sequences in the comment), with one comment line pointing at `docs/FORK-DELTA.md`. The diff against upstream removes exactly two lines, the file is Spotless-`identical`, and testUnit is green afterwards (1837 tests, 0 failures, 2 skipped).
- **E-05 full suite on `latest` x V2** (ClickHouse `26.10.3.7`, digest `sha256:739e099a...30b1ce`): 2440 tests (2425 + 15), 18 named skips, exactly one red, `WriterErrorPathLiveTest#WRT-10: a record whose Array`.
- **E-06 #857 measurement** (15 shapes, `probe_tree` `05183afc`): 14 shapes get one new `DataException` layer from #857 and the retriable shape is rethrown as the same instance; the prefix repeats the column name in 8 shapes; one ERROR log line with the full cause chain appears per failure (14 shapes); **none of the 56 outcome cells changes** through the Processing-shaped wrap.

## Measured E-06 summary

- **Value surfaces:** the record-value stand-in reaches the exception message, the DLQ headers (message or printed trace) and the ERROR log for exactly one shape, `JACKSON_OVER_SOCKET_TIMEOUT`. The message and DLQ-header exposure already exists before #857 (the pass-through Jackson exception, FORK-DELTA Known costs item 6, which plan 45-03 Task 1 closes per plan-check answer 1); the ERROR log is the one surface #857 adds for that shape (`sentinel_in_log_before=no`).
- **The other 13 shapes** (including `JACKSON_ALONE` and `SCRIPTED_ARITHMETIC`) carry no value on any surface even after #857, because the fork's own `doWriteColValue` messages are value-free; what #857 adds there is the extra coordinates, the repeated column and the extra layer.
- **Research predictions:** every prediction held (14 new-layer shapes, same-instance retriable rethrow, zero outcome changes). Nothing to carry forward as a surprise.
- Plan 45-03's pin must therefore treat `JACKSON_OVER_SOCKET_TIMEOUT` as the one shape whose pre-existing message exposure is closed by the approved wrapper, and every other shape as value-free today.

## Task Commits

1. **Task 1: U-2 tracer** - `c3946803` (compile fix, A), `0f4e45bd` (escape-test edit, B), `05183afc` (E-05)
2. **Task 2: #857 measurement** - `f6ecadc8` (E-06)

**Plan metadata:** the docs(45-02) completion commit follows this file (SUMMARY only; STATE.md, ROADMAP.md and requirements are the orchestrator's).

`actuals.commits: 4` is the measured `git rev-list --count plan_head_before..plan_head_after`.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterRecordContextTest.java` - two constructor sites, test-only
- `src/test/java/com/clickhouse/kafka/connect/sink/db/FieldDescriptorEscapeTest.java` - one method's two expected values and one comment line
- `.planning/phases/45-upstream-tail-merge-dependency-check/45-COMMAND-EVIDENCE.md` - entries E-05 and E-06, index rows flipped to pass

## Decisions Made

None beyond the plan. One local judgement: `ClickHouseWriterRecordContextTest` keeps upstream's unused `java.io.IOException` import (Spotless `removeUnusedImports` would remove that one line; recorded as `fmt_record_context_test: preexisting`), so the diff to upstream stays at exactly the two named lines.

## Deviations from Plan

### Auto-fixed Issues

None. Observations, not rule triggers:

- **JVM crash on the first full-suite attempt.** The test JVM crashed (OpenJ9, `Gradle Test Executor 33`, exit 255, partial XML of 2356 tests / 133 files, an 8 GB core dump and three sibling files). Per the standing rule the dump files were deleted and the suite re-run once; the re-run was clean (2440 tests) and is the one recorded in E-05, with the crash noted in `jvm_crash`.
- **E-06 sh fence.** The first draft of the fence carried the literal probe sentinel in a `grep` command, which the verify rejects (no sentinel text in the evidence). It was replaced by a placeholder before E-06 was committed.
- **E-05 timestamp.** The entry's UTC timestamp line (`20:21:30Z`) was written about a minute ahead of the wall clock; it is within the same minute of the work and was not altered after commit.
- The probe source was drafted at `/tmp/gsd45/probe-src/` and copied into the detached worktree instead of being written there directly; the probe also records two extra facts per shape (`same_instance`, `sentinel_in_log_before`). Neither changes any required fact.

**Total deviations:** 0 auto-fixed. **Impact:** none.

## Issues Encountered

None beyond the crash above. No Docker, network or Gradle problem; the probe worktree and the formatting worktree were removed after use and `git worktree list` has one line.

## User Setup Required

None - no external service configuration required.

## Threat Flags

None. The probe ran only in a detached worktree and never printed or stored its sentinel; T-45-06, T-45-08 and T-45-09 are handled as the plan's register states (measured, probe removed, upstream edits bounded against `83808df9`).

## Next Phase Readiness

- Ready for plan 45-03: the red set on the tree is exactly WRT-10 and the E-06 shape table (same 15 shapes, columns, values and scripted failures) is the measurement 45-03's `WriterRecordContextUnitTest` pin must mirror. Expected counts for later plans: testUnit 1837 (2 skipped) and full suite 2440 (18 skipped) before 45-03 adds its own tests.
- `src/main` is unchanged since the merge commit `12e2cf17`.

## Self-Check: PASSED

- Files: both edited tests, `45-COMMAND-EVIDENCE.md` and this summary are present.
- Commits `c3946803`, `0f4e45bd`, `05183afc`, `f6ecadc8` are ancestors of HEAD.
- Verify blocks `P45-02-T1-OK` and `P45-02-T2-OK` printed on the committed tip; `git worktree list` has one line; no `Probe857MeasurementTest` file is tracked, in `src`, or in any ref's history.
