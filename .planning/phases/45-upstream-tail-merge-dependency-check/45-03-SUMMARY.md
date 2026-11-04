---
phase: 45-upstream-tail-merge-dependency-check
plan: "03"
subsystem: writer-value-free-failures
tags: [ups-02, value-free, jackson-rebuild, issue-857, option-a, retry-classification, mutation-testing, evidence]

requires:
  - phase: 45-upstream-tail-merge-dependency-check
    provides: plan 45-01's merge commit 12e2cf17, plan 45-02's U-2 repairs and the E-06 #857 measurement (15 shapes)
provides:
  - commit f13d8378, doWriteColValue rebuilds every value-carrying Jackson chain value-free (withheldJacksonChain), Utils' two client-timeout prefix constants made public, JacksonOverIoValueFreeUnitTest (19 chains x 5 checks), the three retired WriterValueFreeFailureUnitTest assertions rewritten
  - commit f4b85584, writeColumnWithRecordContext rethrows the fork's own exception and logs one coordinate line value-free; #857's serializationFailure helper removed; WriterRecordContextUnitTest and WriterRecordContextDlqUnitTest; three coordinate assertions of upstream's ClickHouseWriterRecordContextTest moved to the log line
  - E-07, E-08, E-09 (red-first, green, six mutations, cache-busted testUnit, live WRT-10 on latest x V2, tail gate with anchor_rows=195)
affects: [45-04, 45-05, 45-06]

actuals:
  tokens: 18800
  tasks: 3
  commits: 5
plan_head_before: 0b9ac5ba74f4049cad52ebe6beca304efdd6cdd5
plan_head_after: 3a9ff68ccd5f054a03758ca6d9ed62aaf9d6d3e0

tech-stack:
  added: []
  patterns: [value-free stand-in chain that keeps what retry classification reads, same-kind exception stand-ins, Docker-free pins through writeRecordsRowBinary and ProxySinkTask.put, detached-worktree mutation runs]

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/JacksonOverIoValueFreeUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterRecordContextUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/WriterRecordContextDlqUnitTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/main/java/com/clickhouse/kafka/connect/util/Utils.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterRecordContextTest.java
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/45-upstream-tail-merge-dependency-check/45-COMMAND-EVIDENCE.md

key-decisions:
  - "Both user decisions executed as locked: every Jackson chain is rebuilt (plan-check answer 1 and the iteration-2 answer) and the record loop keeps the fork's own exception and logs topic, partition, offset, column and exception class only (U-1 Option A, answer 3)"
  - "The stand-in text of a rebuilt link is the class name plus the withheld note, held as one private constant in the writer, so the existing WRT-13 anchor row (WithheldMessageException.of( = 1) did not move"

requirements-completed: [UPS-02]

coverage:
  - id: D1
    description: "doWriteColValue rebuilds every pass-through chain holding a Jackson link value-free: the trusted non-Jackson IOException suffix stays the same objects, a Jackson root becomes an IOException stand-in keeping the timeout-prefix fact, every link above is a same-kind stand-in (server errors with the same code), the top keeps its IOException or RuntimeException category; Utils changes only in two constants' visibility"
    requirement: UPS-02
    verification:
      - kind: other
        ref: "P45-03-T1-OK; JacksonOverIoValueFreeUnitTest 95 cases (19 chains x 5), red on the unchanged writer for the 38 value cases, green after; E-07"
        status: pass
    human_judgment: false
  - id: D2
    description: "writeColumnWithRecordContext rethrows the exception doWriteCol produced and logs one ERROR line with topic, partition, offset, column and exception class, no message, throwable or value; a RetriableException is rethrown without the line; upstream's wrapping helper and message text are gone"
    requirement: UPS-02
    verification:
      - kind: other
        ref: "P45-03-T2-OK; WriterRecordContextUnitTest 59 cases and WriterRecordContextDlqUnitTest 4 cases, 32 red on the #857 tree, all green after; E-08"
        status: pass
    human_judgment: false
  - id: D3
    description: "The new non-change checks bite under six mutations, testUnit is green at 1995 tests with 2 skips, WRT-10 and the 216-case matrix pass live on latest x client V2, the gg3 pins are unchanged since pre-v1.7.0_gg8 apart from the three retired methods, and the unedited tail gate ends anchor_rows=195 result=PASS"
    requirement: UPS-02
    verification:
      - kind: other
        ref: "P45-03-T3-OK; E-09"
        status: pass
    human_judgment: false

duration: 18min
completed: 2026-11-04
status: complete
---

# Phase 45 Plan 03: Value-free Jackson chains and Option A record context Summary

**Every serialization failure the writer lets through is now value-free on every surface (message, cause chain, rendered trace, DLQ headers, logs): a value-carrying Jackson chain is rebuilt as same-kind stand-ins that keep what retry classification reads, and the RowBinary record loop rethrows the fork's own exception and logs one coordinates-only line instead of upstream #857's wrapper.**

## Performance

- **Duration:** about 18 min wall clock (the six mutation builds and the live run are most of it)
- **Started:** 2026-11-04T20:24:00Z
- **Completed:** 2026-11-04T20:42:00Z
- **Tasks:** 3
- **Files modified:** 3 created and 6 modified (two main files, four test files with the anchors file and the evidence)

## Accomplishments

- **Task 1 (commit `f13d8378`, E-07).** `withheldJacksonChain` and one `sameKindStandIn` mapping rebuild a chain that holds a Jackson link: the non-Jackson IOException run at the bottom is kept as the same objects; a Jackson root becomes a plain `IOException` carrying the withheld text and, only when the root's message started with it, the read- or write-timeout prefix `Utils.handleException` reads; every link above is a stand-in of the same kind (`RetriableException`, `DataException`, `ConnectException`, client V1 `ClickHouseException` and client V2 `ServerException` with the same error code, otherwise `IOException` or `RuntimeException`); Jackson links are dropped; the top keeps its IOException or RuntimeException category so client V2 wraps exactly as before. `JacksonOverIoValueFreeUnitTest` pins 19 chains (the five Jackson-over-IO shapes, the iteration-2 shapes (a), (b) and (c), a Jackson root under an IOException with and without a timeout prefix, an exact RuntimeException and a RetriableException over a Jackson root, nested Jackson links, a V1 and a V2 server error above a Jackson root, the four between-link chains and a value-carrying non-IO link below a Jackson link) x 5 checks.
- **Red-first on the unchanged writer:** 340 tests, 41 red: the structure and value checks (`rebuildIsValueFreeAndKeepsTheSuffix`, `carriesNoRecordValue`) on all 19 chains and exactly the three edited `WriterValueFreeFailureUnitTest` methods; the three non-change checks and the 216-case matrix were green. After the change all green; the matrix file is unchanged since the tag.
- **Task 2 (commit `f4b85584`, E-08).** `writeColumnWithRecordContext` now catches `RetriableException` (rethrown, no line) and `IOException | RuntimeException` (logs `RECORD_CONTEXT_FAILURE_LOG_FORMAT` with topic, partition, offset, column and exception class, then `throw e`); `serializationFailure` and its `SinkRecord` import are gone. `WriterRecordContextUnitTest` (15 shapes x 4 checks, no exemption, classification over the Processing-shaped wrap) and `WriterRecordContextDlqUnitTest` (`ProxySinkTask.put` over a `CapturingClickHouseWriter`, errors.tolerance all and none, DLQ headers, thrown chain and four captured loggers) pin it. **Red-first on the #857 tree:** 65 tests, 32 red: `rethrowsWhatDoWriteColThrew` and `logsOneValueFreeCoordinateLine` for exactly the 14 wrapped shapes (E-06 `m857_new_layer_shapes`), the four DLQ cases on the missing coordinate line; no value-free check red, the retriable shape green, upstream's own test green. Upstream's `ClickHouseWriterRecordContextTest` differs from `83808df9` in 13 removed lines, five approved (the two constructor lines and the three coordinate assertions) and eight that reappear re-indented.
- **Task 3 (E-09).** Six mutations in detached worktrees, each turning the expected checks red: M1 a value in the log line (18 red, `logsOneValueFreeCoordinateLine` and `carriesNoRecordValue` of the four value shapes), M2 rethrow as `RetriableException` (38 red, `keepsTheRetryOutcome` for the nine NON_RETRIABLE shapes and SOCKET_EXCEPTION), M3 the rebuild switched off (42 red: all 19 value checks, the three WVF methods, the record-loop JACKSON_OVER_SOCKET_TIMEOUT check), M4 the kept suffix replaced by a withheld copy (40 red, including 8 matrix cases named JACKSON_OVER_SOCKET_TIMEOUT), M5 the RetriableException stand-in made a RuntimeException (4 red, RETRIABLE_OVER_JACKSON_ROOT and OVER_RETRIABLE_OVER_PLAIN_IO), M6 the root stand-in dropping the timeout prefix (3 red, PLAIN_IO_OVER_TIMEOUT_PREFIXED_JACKSON_ROOT). Cache-busted testUnit: **1995 tests** (1837 + 95 + 59 + 4), 0 failures, 2 skipped. Live on ClickHouse `26.10.3.7` x client V2: `WriterErrorPathLiveTest` 10 tests with **WRT-10 passing**, `JsonAsStringKeyResidualLiveTest` 4, matrix 216, all green. Tail gate (unedited script): `provenance_paths=11 anchor_commits=15 anchor_rows=195 marker_commits=33 anchor_exempt=0 result=PASS`.

## Task Commits

1. **Task 1: Jackson-chain tracer** - `f13d8378` (rebuild, tests, one anchor row), `4067fa78` (E-07)
2. **Task 2: Option A re-expression** - `f4b85584` (fix, pins, upstream-test edit, four anchor rows), `d052b6b8` (E-08)
3. **Task 3: bite and gate** - `3a9ff68c` (E-09)

**Plan metadata:** the docs(45-03) completion commit follows this file (SUMMARY only; STATE.md, ROADMAP.md and requirements are the orchestrator's). `actuals.commits: 5` is the measured `git rev-list --count plan_head_before..plan_head_after`.

## Facts for plan 45-05's FORK-DELTA rows

- **Three retired `WriterValueFreeFailureUnitTest` assertions** (user-approved, each renamed, sentinel added, now asserting the value-free rebuild): `jacksonOverSocketTimeoutPassesThrough` becomes `jacksonOverSocketTimeoutBecomesValueFreeIoOverTheSameCause`; `jacksonOverPlainIoExceptionPassesThroughAsAcceptedResidual` becomes `jacksonOverPlainIoExceptionBecomesValueFreeIoOverTheSameCause`; `plainIoExceptionOverJacksonPassesThroughAsAcceptedResidual` becomes `plainIoExceptionOverJacksonBecomesValueFreeIoOverAWithheldRoot`. The class is otherwise byte-unchanged apart from one added import (`assertNull`).
- **`Utils`:** only `CLICKHOUSE_CLIENT_ERROR_READ_TIMEOUT_MSG` and `CLICKHOUSE_CLIENT_ERROR_WRITE_TIMEOUT_MSG` changed from `private` to `public`.
- **Upstream test edit (U-2 extended):** `ClickHouseWriterRecordContextTest.serializationFailureNamesTopicPartitionOffsetAndColumn` reads topic, partition and offset from the captured coordinate line instead of the exception message (plus the plan-45-02 constructor fix).
- **Behaviour change of the fork's exception text:** a chain that holds a Jackson link no longer reaches the caller as the original; the top message is the value-free conversion text and every link above the kept suffix is a withheld stand-in with the original's stack trace. The record loop adds no exception layer and logs `Record serialization failed: topic [..] partition [..] offset [..] column `..`: exception <class>`.
- **The two shapes `WriterRecordContextDlqUnitTest` uses:** INVALID_UUID and UNPARSEABLE_INT128. Schema validation did not reject either (the capturing writer replaces `doInsertRawBinary`, so the record loop runs), so TYPE_MISMATCH was not needed.
- **Client V1 constructor:** `ClickHouseException(int, Throwable, ClickHouseNode)` accepts a `null` node; no `ClickHouseNode` was needed for the pinned shape or the stand-in.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Stand-in text without a second `WithheldMessageException.of(` call**
- **Found during:** Task 1, anchors mirror after the first green run (`ANCHOR MOVED ... [WithheldMessageException.of(] expected 1 got 2`)
- **Issue:** the plan's helper text is `WithheldMessageException.of(link).toString()`, which added a second occurrence of a literal an existing WRT-13 anchor row pins at exactly 1; the plan requires the mirror to read `rows=191 bad=0` with no moved row, and a third `src/main` file (adding a helper to `WithheldMessageException`) would break the `main_diff_since_merge` gate.
- **Fix:** `withheldText(link)` is the class name plus one private constant `WITHHELD_LINK_NOTE` (the note text `WithheldMessageException.toString()` appends), with a comment saying why; the pins assert the same literal, so a drift of either note fails them.
- **Files modified:** `ClickHouseWriter.java` (inside the Task 1 commit)
- **Verification:** anchors `rows=191 bad=0`, then `rows=195 bad=0`; tail gate PASS
- **Commit:** `f13d8378`

### Other observations (not rule triggers)

- The m1 and m2 mutation targets differ from the plan's one-line text only in the line breaks of the formatted call; each was replaced exactly once and recorded in E-09.
- `WriterRecordContextDlqUnitTest` captures four loggers (`ClickHouseWriter`, `Processing`, `ProxySinkTask`, `Utils`) for the sentinel check, one more than the plan's single ClickHouseWriter capture; the coordinate-line check still reads the writer's events only.
- Spotless: `ClickHouseWriter.java` and upstream's `ClickHouseWriterRecordContextTest.java` carry pre-existing import-order and unused-import violations that this plan did not touch (`fmt_writer`, `fmt_rct`: `preexisting`); the new and edited files are `identical`.
- E-05's timestamp note (45-02) was a minute ahead of the clock; E-07, E-08 and E-09 timestamps were corrected to the clock before their commits.

**Total deviations:** 1 auto-fixed (1 blocking). **Impact:** none on behaviour or the verify gates.

## Issues Encountered

None. No JVM crash and no core dump occurred in any run; every worktree was removed (`git worktree list` has one line). Nothing was pushed and no remote ref was touched.

## Known Stubs

None.

## Threat Flags

None. The new classes in `src/test` and the new stand-in links add no network endpoint, auth path or file access; T-45-07, T-45-10, T-45-11 and T-45-22 are mitigated as the register states (mutations M1-M6 prove the checks bite).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for plan 45-04: `src/main` differs from the merge commit only in `ClickHouseWriter.java` and `Utils.java`; the anchors mirror reads `rows=195 bad=0`; testUnit is 1995 tests with 2 skips and WRT-10 passes live. The full-suite expectation for later plans is 2440 + 95 + 59 + 4 = 2598 tests (the three new classes are untagged and run in the full suite as well), with the 18 named skips.

## Self-Check: PASSED

- Files: the three new test classes, `45-03-SUMMARY.md` and the E-07..E-09 entries are present.
- Commits `f13d8378`, `4067fa78`, `f4b85584`, `d052b6b8`, `3a9ff68c` are ancestors of HEAD.
- Verify blocks `P45-03-T1-OK`, `P45-03-T2-OK` and `P45-03-T3-OK` printed on the committed tip; `git worktree list` has one line.
