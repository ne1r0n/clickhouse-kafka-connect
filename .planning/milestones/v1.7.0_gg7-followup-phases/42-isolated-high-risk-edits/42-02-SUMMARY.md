---
phase: 42-isolated-high-risk-edits
plan: "02"
subsystem: testing
tags: [kafka-connect, retriable-exception, error-classification, fork-delta, junit5, handleException]

requires:
  - phase: 42-isolated-high-risk-edits
    provides: plan 42-01 gate E-04 (testUnit 1763, full 2361, the status dumps), the evidence helpers and the record placement rule
provides:
  - "Utils.handleException throws a RetriableException found as the exception or anywhere in its cause chain as retriable, after the server-error rule and before the DataException rule"
  - "RetriableExceptionPassThroughUnitTest: 38 Docker-free cases (22 direct, 16 through ProxySinkTask.put and ChunkFlusher.putDirect)"
  - "42-rsk02-expected-added.tsv, committed before the change: the 38 added rows and no changed existing row"
  - "FORK-DELTA Connector source row and Known cost 17 sentences stating both consequences of the chain form"
  - "42-COMMAND-EVIDENCE.md E-05..E-07"
affects: [42-03, 42-04, 44-release-notes]

actuals:
  tokens: 11200
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - "expected changed-row list committed before the production edit, then compared mechanically with the actual per-testcase status delta"
    - "red-then-green by assertion with a SHA-256 freeze of the test file between the red run and the code commit"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java
    - .planning/phases/42-isolated-high-risk-edits/42-rsk02-expected-added.tsv
  modified:
    - src/main/java/com/clickhouse/kafka/connect/util/Utils.java
    - docs/FORK-DELTA.md
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/42-isolated-high-risk-edits/42-COMMAND-EVIDENCE.md

key-decisions:
  - "Chain form (the user's choice): a RetriableException anywhere in the cause chain stays retriable, placed after the server-error block so server codes keep precedence for everything else, and before the DataException check so the outcome does not depend on errors.tolerance"
  - "The exception itself is rethrown when it is the RetriableException; otherwise it is wrapped once, so ChunkFlusher.putDirect no longer double-wraps the first handling's RetriableException"
  - "The expected changed-row list (38 added, none existing) was committed in E-05 before the Utils.java edit existed in any commit, so the gate could not be fitted to the diff"

patterns-established:
  - "Each Phase 42 code commit carries its test, its code lines and its FORK-DELTA and anchors rows, so it reverts alone"

requirements-completed: [RSK-02]

plan_head_before: 5ff19b9cfa231fb5a936fc08b47ad7f6744de7b8
plan_head_after: 9d55782438dcc2bfb82f1d9681e86ce6d872b89e

coverage:
  - id: D1
    description: "A RetriableException passed to Utils.handleException, as the exception itself or anywhere in its cause chain, comes out retriable under both errors.tolerance values (the same instance, or wrapped once)"
    requirement: RSK-02
    verification:
      - kind: unit
        ref: "RetriableExceptionPassThroughUnitTest#handleExceptionOutcome (18 of 22 rows red on the unchanged tree, all 22 green after the edit; E-05, E-06, E-07)"
        status: pass
    human_judgment: false
  - id: D2
    description: "A writer-thrown RetriableException that Processing.doInsert wraps (async insert timeout, DDL-propagation timeout, a DataException holding one) leaves ProxySinkTask.put and ChunkFlusher.putDirect as retriable with no DLQ record under both tolerances; plain failures keep their DLQ path"
    requirement: RSK-02
    verification:
      - kind: unit
        ref: "RetriableExceptionPassThroughUnitTest#writerFailureOutcomeThroughPut (12 of 16 rows red, all 16 green; the 4 socket-timeout control rows pass both ways)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Success criterion 2: the changed-row list was written down before the change and the actual diff equals it: no existing testcase changed status, 38 rows added, WriterFailureClassificationUnitTest stays 216/0, PIN-02 14/0, TMO-02 15/0"
    requirement: RSK-02
    verification:
      - kind: other
        ref: "42-COMMAND-EVIDENCE.md E-07: testUnit 1801/0/0/2 and full 2399/0/0/18, both deltas removed=0 added=38 expected=38 match=yes"
        status: pass
    human_judgment: false
  - id: D4
    description: "The two behaviour changes the chain form makes (a RetriableException overrides a non-retriable server code; a DataException holding one no longer fails the task under none) and the redelivery-without-retry-budget consequence are written in FORK-DELTA"
    requirement: RSK-02
    verification:
      - kind: unit
        ref: "RetriableExceptionPassThroughUnitTest rows BARE_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE, WRAPPED_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE, DATA_EXCEPTION_OVER_RETRIABLE"
        status: pass
    human_judgment: true
    rationale: "The wording of the published behaviour change and its rollout consequence is a maintainer judgment; the tests pin the outcomes, not the prose"

duration: 8min
completed: 2026-11-02
status: complete
---

# Phase 42 Plan 02: RSK-02 retriable pass-through Summary

**Seven added lines in `Utils.handleException` keep a `RetriableException` anywhere in the cause chain retriable, so the `async=true` insert timeout and the DDL-propagation timeout that `Processing.doInsert` wraps no longer fail the task or go to the DLQ; proven by 38 Docker-free pins red-then-green against a changed-row list committed before the edit, with no existing testcase changing status**

## Performance

- **Duration:** 8 min
- **Started:** 2026-11-02T12:15:00Z
- **Completed:** 2026-11-02T12:23:00Z
- **Tasks:** 3
- **Files modified:** 6 (2 created, 4 modified)

## Accomplishments

- Wrote `RetriableExceptionPassThroughUnitTest` (38 cases: 22 direct `Utils.handleException` rows and 16 rows through `ProxySinkTask.put` and `ChunkFlusher.putDirect`) and saw it red on the unchanged tree for the stated reason: exactly the 30 non-control cases fail by `AssertionFailedError`, the 8 `_CONTROL` cases pass, and all seven existing classification classes pass at their measured counts (216, 14, 15, 19, 34, 6, 14) (E-05).
- Committed the expected changed-row list (`42-rsk02-expected-added.tsv`: the 38 added rows, no existing row) with E-05 before `Utils.java` was touched, then made the edit: one hunk of 7 added lines, 0 removed, no import and no log statement. The test file is byte-identical to the red run (SHA-256 frozen) (E-06).
- Recorded the change in FORK-DELTA: a new Connector source row after the Four Single Message Transforms row, two sentences at the front of Known cost 17, and two anchor rows (mirror 186 to 188, `bad=0`). Both state the two chain-form consequences in the plan's exact words: a `RetriableException` overrides the server-code classification (over a non-retriable code such as 60 it is retriable), and a `DataException` holding one no longer fails the task under `errors.tolerance=none` (E-06).
- Gated the commit: a cache-busted `testUnit` (Docker unreachable) of 1801 tests and a full suite on ClickHouse 26.10.3.7 (`latest`) x client V2 of 2399 tests, exactly the RSK-01 gate totals plus 38, with both status deltas `removed=0 added=38 expected=38 match=yes` (E-07).

## Task Commits

1. **Task 1 (tracer: red run, expected list, then the edit)** - `b34d0280` (docs): the expected-added list and E-05, committed before the edit. The test class and the `Utils.java` edit stayed uncommitted until Task 2 by design.
2. **Task 2 (RSK-02 code commit)** - `c8574890` (fix): `Utils.java`, `RetriableExceptionPassThroughUnitTest.java`, `docs/FORK-DELTA.md`, `fork-anchors.txt`. This is the revert unit. Its E-06 record: `39174246` (docs).
3. **Task 3 (gate E-07)** - `9d557824` (docs).

**Plan metadata:** the SUMMARY and ROADMAP commit that follows this file (docs: complete plan).

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/util/Utils.java` - the `for (Throwable t = e; ...)` pass-through block between the server-error block and `//High-Level Explicit Exception Checking`.
- `src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java` - two parameterized methods, 38 cases; `FailingDBWriter` is an `InMemoryDBWriter` subclass, no Mockito.
- `.planning/phases/42-isolated-high-risk-edits/42-rsk02-expected-added.tsv` - the 38 PASS rows later gates compare against.
- `docs/FORK-DELTA.md` - Connector source row and Known cost 17 sentences.
- `.planning/scripts/fork-anchors.txt` - Phase 42 comment and two RSK-02 rows after the TAL-03 row.
- `.planning/phases/42-isolated-high-risk-edits/42-COMMAND-EVIDENCE.md` - E-05, E-06, E-07 and their index rows.

## Decisions Made

- The chain form is used as the user chose: the loop runs after the server-error block (server codes keep precedence for non-`RetriableException` chains) and before the `DataException` check (the outcome is independent of `errors.tolerance`, as the root-cause rules are).
- The input instance is rethrown when it is itself the `RetriableException`, mirroring `throw (DataException) e;`; a nested match is wrapped once like the four other `new RetriableException(e)` sites. The visible effect is that `ChunkFlusher.putDirect`'s second handling no longer double-wraps the first handling's `RetriableException`.
- No guard against a cyclic cause chain was added: `Utils.getRootCause` runs first with the same property, so a cyclic chain never reaches the loop (T-42-08 accepted).

## Deviations from Plan

None - plan executed exactly as written. The test class compiled against the unchanged tree and failed by assertion on the first run, so no guard literal needed correcting before the red freeze. One small readability choice inside the test (a `classify(thrown, input)` helper returning the `Outcome` directly) was made before the red run and is part of the frozen file.

## Issues Encountered

None. No JVM crash file appeared, the ClickHouse version and image digest were identical before and after every run, and `origin/custom_build` equaled the E-01 value at the gate.

## Known Stubs

None.

## Threat Flags

None - the edit adds no endpoint, auth path, file access or schema change. T-42-06 (a batch whose writer failure carries a `RetriableException` is now redelivered with no retry budget instead of going to the DLQ under `all`) is mitigated by the FORK-DELTA row and Known cost 17, which also state the server-code override and the `DataException` consequence; the `_CONTROL` shapes pin that plain failures keep their DLQ path. T-42-07 is mitigated by the pre-committed list and the equal status delta.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for 42-03 (RSK-03). Its FORK-DELTA row and anchors rows sit at positions separated from this commit's insertions, so each reverts alone (proven in 42-04).
- The anchors mirror stands at 188 rows; 42-03 should expect 190 after its commit.
- The gate dumps `status-gate2-*.tsv` in `/tmp/gsd42/exec` (testUnit 1801, full 2399) are the inputs of the 42-03 deltas; if they are gone, re-measure on the current tree.
- The behaviour change is user-visible (redelivery with no retry budget; a `RetriableException` overrides a non-retriable server code): Phase 44's rollout note should carry it.
- `STATE.md` is updated by the GSD verbs; `.planning/config.json` and `.planning/state.json` stay orchestrator-managed and uncommitted.

## Self-Check: PASSED

- Created files exist: the test class and `42-rsk02-expected-added.tsv`.
- Commits exist on HEAD: `b34d0280`, `c8574890`, `39174246`, `9d557824`.

---
*Phase: 42-isolated-high-risk-edits*
*Completed: 2026-11-02*
