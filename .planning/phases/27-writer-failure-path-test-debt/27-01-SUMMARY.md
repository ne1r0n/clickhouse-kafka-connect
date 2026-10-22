---
phase: 27-writer-failure-path-test-debt
plan: "01"
subsystem: testing
tags: [kafka-connect, clickhouse, junit5, jackson, retry-classification, wrt-16]

requires:
  - phase: 25-v1.7.0-sync
    provides: WriterFailureClassificationUnitTest (WRT-14 matrix, 184 cases), ScriptedWriteFailureWriter, CountingHelperClient
provides:
  - four Jackson-group FailureKind rows (32 cases) pinning retry classification green on the unchanged writer
  - 27-COMMAND-EVIDENCE.md with E-01 (phase base every later plan measures against) and E-02
affects: [27-02, 27-03, 27-04, 27-05]

actuals:
  tokens: 2000
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Additive-only edits to an existing parameterized matrix: new enum constants inserted mid-list, zero removed lines, testcase-name-set identity proof against the phase-base XML"

key-files:
  created:
    - .planning/phases/27-writer-failure-path-test-debt/27-COMMAND-EVIDENCE.md
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/WriterFailureClassificationUnitTest.java

key-decisions:
  - "Fourth WRT-16 shape is a plain IOException (PLAIN_IO_CONTROL), the literal roadmap shape; the Jackson-over-plain-IOException and IOException-over-Jackson shapes are not rows because a classification row cannot distinguish pass-through from conversion (both NON_RETRIABLE); plan 27-02 pins them as accepted residuals"
  - "JsonProcessingException is realised as JsonMappingException because every JsonProcessingException constructor is protected in jackson-core 2.22.4; the project's Jackson is 2.22.4, not 2.21"

requirements-completed: [WRT-16]

coverage:
  - id: D1
    description: "WriterFailureClassificationUnitTest pins retry classification of a Jackson exception alone (NON_RETRIABLE), over a SocketTimeoutException (RETRIABLE), over a timeout-text IOException (RETRIABLE) and a plain IOException (NON_RETRIABLE) on both entries, both clients and both tolerance settings, green on the unchanged writer"
    requirement: "WRT-16"
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/WriterFailureClassificationUnitTest.java#classificationOutcomeIsPinned (216 cases, 0 failed)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The existing 184 cases are byte-identical: all 184 phase-base testcase names present and passing, git diff -U0 against the phase base removes 0 lines, src/main identical to the phase base"
    requirement: "WRT-16"
    verification:
      - kind: other
        ref: "python3 -I testcase-name-set comparison of build/test-results/testUnit XML against /tmp/gsd27/exec/wfc-base.xml (184/184 present and passing); git diff -U0 removed-line count 0"
        status: pass
    human_judgment: false
  - id: D3
    description: "E-01 records the phase base (phase_base, origin_custom_build_tip, testUnit 1128, class counts, anchor_rows=79, 46-line Spotless numstat base) for plans 27-02..27-05"
    requirement: "WRT-16"
    verification:
      - kind: other
        ref: "27-COMMAND-EVIDENCE.md#E-01 (Task 1 TRACER-GREEN verify)"
        status: pass
    human_judgment: false

duration: 7min
completed: 2026-10-22
status: complete
plan_head_before: b8f67b3b0c81e19ed782932774e908423533629a
plan_head_after: f604f720b6ae09d7da2f18c6a1da30afd45ba3da
commits: 2
---

# Phase 27 Plan 01: WRT-16 Jackson-group classification rows Summary

**Four Jackson-group FailureKind rows (32 cases) pin the retry classification of a Jackson exception alone, over a socket timeout, over a timeout-text IOException, and a plain IOException, green on the unchanged writer with the existing 184 cases untouched.**

## Performance

- **Duration:** about 7 min
- **Started:** 2026-10-22T21:36Z (approx.)
- **Completed:** 2026-10-22T21:43Z
- **Tasks:** 2
- **Files modified:** 2 (1 test file, 1 new evidence file)

## Accomplishments

- Tracer: `JACKSON_ALONE` driven through the real `ProxySinkTask.put` / `ChunkFlusher.putDirect`, `Processing.doLogic`, `ClickHouseWriter.doWriteColValue`'s generic catch, `doInsertRawBinary` and `Utils.handleException` on the unchanged writer (192 cases green before any other row).
- `JACKSON_OVER_SOCKET_TIMEOUT` and `JACKSON_OVER_TIMEOUT_TEXT` pin RETRIABLE and `PLAIN_IO_CONTROL` pins NON_RETRIABLE, so a naive WRT-17 fix that excludes the whole chain once its head is a Jackson exception turns two rows red.
- Class now runs 216 cases, 0 failed; full testUnit 1160 tests, 0 failed, 2 skipped (base 1128); Spotless numstat byte-equal to the 46-line base; tail gate `anchor_rows=79 result=PASS`.
- Evidence file opened with E-01 (phase base, origin tip `69cd680b`) and E-02.

## Task Commits

1. **Task 1: Tracer, E-01 and JACKSON_ALONE** - no commit of its own by plan design (edits land in the Task 2 commits)
2. **Task 2: other three rows, 184-case identity proof, E-02** - `108f79a5` (test), `f604f720` (docs)

**Plan metadata:** committed separately after this SUMMARY (docs: complete plan).

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/WriterFailureClassificationUnitTest.java` - import `JsonMappingException` and constants `JACKSON_ALONE`, `JACKSON_OVER_SOCKET_TIMEOUT`, `JACKSON_OVER_TIMEOUT_TEXT`, `PLAIN_IO_CONTROL` between `SOCKET_TIMEOUT` and `UNKNOWN_HOST` (11 insertions, 0 removals)
- `.planning/phases/27-writer-failure-path-test-debt/27-COMMAND-EVIDENCE.md` - phase evidence file with index E-01..E-13, E-01 and E-02 filled

## Decisions Made

- Fourth shape is the plain `IOException` (`PLAIN_IO_CONTROL`), duplicating `IO_PLAIN` on purpose; the Jackson-over-plain-IOException and IOException-over-Jackson shapes are same-instance accepted residuals pinned in plan 27-02, since a classification row cannot tell pass-through from conversion.
- `JsonProcessingException` is realised as `JsonMappingException` (public constructors; jackson-core 2.22.4).

## Deviations from Plan

None - plan executed exactly as written.

Notes (not deviations): at plan start the tracked `.planning/STATE.md` carried the orchestrator's "Phase 27 execution started" edit, so the plan's clean-tree check printed ` M .planning/STATE.md`. It is orchestrator-owned planning state, not source; it was excluded from every task commit and is committed with the state updates of this plan. No OpenJ9 crash occurred in any run.

## Issues Encountered

None.

## Known Stubs

None.

## Threat Flags

None - test-only change; no new endpoints, auth paths or trust-boundary schema.

## Next Phase Readiness

Ready for 27-02 (WRT-17): the four Jackson-group classification rows and the E-01 base are in place, so the writer fix can be driven red-first and checked against the unchanged outcomes.

## Self-Check: PASSED

- `src/test/java/com/clickhouse/kafka/connect/sink/WriterFailureClassificationUnitTest.java`: FOUND
- `.planning/phases/27-writer-failure-path-test-debt/27-COMMAND-EVIDENCE.md`: FOUND
- commit `108f79a5`: FOUND on HEAD ancestry
- commit `f604f720`: FOUND on HEAD ancestry
- Task 1 verify TRACER-GREEN, Task 2 verifies WRT16-ROWS-COMMITTED (tracked equivalently) and WRT16-GATED printed
