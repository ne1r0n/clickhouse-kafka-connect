---
phase: 40-helper-client-closing
plan: "02"
subsystem: sink-db
tags: [helper-client-lifecycle, writer, red-first, gate-halted, clickhouse-kafka-connect]

requires:
  - phase: 40-helper-client-closing
    provides: "40-red-writer.patch (writer red tree), the evidence helper and E-01..E-04 (plan 40-01)"
provides:
  - "ClickHouseWriter closes its helper clients: stop() closes chc once after the table-sync executor, start() closes it on every non-success exit, getMutationRequest closes the per-batch helper in try-with-resources"
  - "The writer-half commit de3faf77 (five files) with the FORK-DELTA writer row, harness row, Known cost 13 and four fork-anchors rows"
  - "Evidence E-05 (pass), E-06 (pass) and E-07 (fail: gate A red on three existing ClickHouseWriterTest cases)"
affects: [40-03]

actuals:
  tokens: 36000
  tasks: 2
  commits: 4

tech-stack:
  added: []
  patterns:
    - "start() wrapper over startInternal whose finally releases the client when the start did not return true"
    - "Null-before-close closeHelperClient(): a second call is a no-op without relying on the client library's idempotence"
    - "Per-batch ClickHouseNode read inside try-with-resources; the node is a value object that outlives its helper client"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java
    - docs/FORK-DELTA.md
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/40-helper-client-closing/40-COMMAND-EVIDENCE.md

key-decisions:
  - "No test and no production line was changed after the red full-suite run: the plan forbids changing existing assertions and says never to adjust a test to pass, so the gate is recorded fail and the plan halts for a decision"
  - "Plan status is halted, not complete: Task 3 (gate A) failed, so plan 40-03 (which depends on this gate) must not start; STATE advance-plan, requirements mark-complete and roadmap progress were not run"

patterns-established:
  - "Gate-red diagnosis: match the count of a distinctive log line in the failing class XML against the number of failing cases to confirm a common cause"

requirements-completed: []

coverage:
  - id: D1
    description: "Tracer: stop() closes the writer's helper client once, null-safe; three stop-path cases turn green on V1 and V2 while the seven failed-start and per-batch methods stay red on close count"
    requirement: CLS-01
    verification:
      - kind: unit
        ref: "WriterHelperClientClosingUnitTest (22 tests, 13 failures, exact seven-method failing set, E-05)"
        status: pass
    human_judgment: false
  - id: D2
    description: "start() wrapper and per-batch try-with-resources: all 22 writer cases and the seven guard and CountingHelperClient classes green with Docker unreachable"
    requirement: CLS-02
    verification:
      - kind: unit
        ref: "WriterHelperClientClosingUnitTest (22 tests, 0 failures) plus the seven guard classes (E-06)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Gate A on the writer-half commit: testUnit with Docker unreachable passes (1709 tests, 0 failures, 2 skipped)"
    requirement: CLS-04
    verification:
      - kind: unit
        ref: "gradlew testUnit --rerun with DOCKER_HOST unreachable (E-07)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Gate A full suite on ClickHouse latest x client V2 is green on the writer-half commit"
    requirement: CLS-01
    verification:
      - kind: integration
        ref: "gradlew test --rerun, CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 (E-07): 2307 tests, 3 failed (ClickHouseWriterTest updateMapping, getTableReturnsNullWhenMissingAndSuppressionEnabled, getTableThrowsWhenMissingAndSuppressionDisabled)"
        status: fail
    human_judgment: true
    rationale: "Three existing live cases use a writer after start() returned false; whether to change how they obtain a started writer, or change the start() contract, is a decision the plan reserves (it forbids changing existing assertions)"

duration: 12min
completed: 2026-11-01
status: halted
plan_head_before: 1586c93142ad49aa569697cdc69ecac912dc51a6
plan_head_after: 41f61da0c8de642d46d928a68162df05720ee66a
commits: 4
---

# Phase 40 Plan 02: Writer half of helper-client closing (gate A red) Summary

**ClickHouseWriter now closes `chc` on stop and on every failed start and closes the per-batch client V1 helper in try-with-resources, all 22 new writer cases and `testUnit` are green, but the full suite fails three existing `ClickHouseWriterTest` cases that use a writer after `start()` returned false, so the plan halts at gate A**

## Performance

- **Duration:** about 12 min (execution, including the two Gradle gate runs)
- **Started:** 2026-11-01T22:07Z
- **Completed:** 2026-11-01T22:16Z
- **Tasks:** 2 of 3 complete; Task 3 ran and failed its gate
- **Files modified:** 6 (five in the writer-half commit, plus the evidence file)

## Accomplishments

- E-05: re-applied `40-red-writer.patch` (SHA-256 equal to E-03), reproduced E-03's ten-method red set, then added `closeHelperClient()` to `stop()`: the class went to 22 tests, 13 failures, with `successfulStartThenStopClosesTheHelperClientOnce`, `secondStopNeitherThrowsNorClosesAgain` and `stopClosesTheHelperClientOnlyAfterTheTableSyncExecutorTerminated` green and the other seven methods still red on `close count`.
- E-06 and the writer-half commit `de3faf77`: `start()` wrapper over `startInternal`, per-batch `try (ClickHouseHelperClient chcTmp = createHelperClient(`, 22 of 22 writer cases and the seven guard classes green, scoped Spotless as required (import-only for `ClickHouseWriter.java`, `IS CLEAN` for the two test files), anchors mirror `rows=176 bad=0`, commit holds exactly the five planned files and `src/main` differs from phase start only in `ClickHouseWriter.java` with no import line changed.
- E-07 (fail): `testUnit` with Docker unreachable passes (1709 tests = 1687 + 22, 0 failures, 2 skipped). The full suite on ClickHouse `latest` (`26.10.3.7`) x client V2 runs 2307 tests (2285 + 22), 18 skipped, and 3 fail.

## Task Commits

1. **Task 1 (tracer): writer red re-run and stop-close** - `93cd2ad2` (docs: E-05; the code stays uncommitted by design until Task 2)
2. **Task 2: start wrapper, per-batch close, FORK-DELTA and anchors** - `de3faf77` (fix: the writer-half commit) and `1659e9be` (docs: E-06)
3. **Task 3: gate A** - `41f61da0` (docs: E-07, Verdict: fail)

**Plan metadata:** committed with this summary (docs: halted plan)

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` - `createHelperClient` factory, `closeHelperClient`, `start` wrapper over `startInternal`, `stop` ending in `closeHelperClient();`, per-batch helper in try-with-resources
- `src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java` - extended harness (builder-built, counts `close`, scripted `ping`/`version`/mapping/query answers)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java` - the 22-case writer counting pin
- `docs/FORK-DELTA.md` - writer row, harness row update, Known cost 13 rewritten
- `.planning/scripts/fork-anchors.txt` - `# Phase 40` comment and four `ClickHouseWriter.java` rows
- `.planning/phases/40-helper-client-closing/40-COMMAND-EVIDENCE.md` - E-05, E-06, E-07

## Decisions Made

- Halted at gate A instead of editing the three failing tests or relaxing the production change. The plan forbids changing existing assertions ("MUST NOT change any existing assertion", "never adjust a test to pass") and requires STOP on a red gate; the choice of remedy belongs to the planner or the user.
- The summary is `status: halted` so that plan 40-03 (`depends_on: 40-02`) is reported blocked until the gate is re-measured green.

## Deviations from Plan

None in the executed steps - Tasks 1 and 2 ran as written. Task 3's step 4 (on a red: record, set `Verdict: fail`, commit the evidence, STOP) was followed exactly.

**Total deviations:** 0 auto-fixed.

## Issues Encountered

**Gate A full suite red: three existing `ClickHouseWriterTest` cases use a writer after `start()` returned false.**

- Failing cases: `updateMapping` and `getTableReturnsNullWhenMissingAndSuppressionEnabled` (`NullPointerException`: `this.chc` is null at `ClickHouseWriter.updateMapping`), and `getTableThrowsWhenMissingAndSuppressionDisabled` (its `contains("does not exist")` assertion sees the `NullPointerException` text).
- Cause: each calls the private `runWithWriter` helper, which does `writer.start(config)`, ignores the boolean result and then calls `updateMapping` or `getTable`. Each starts against an empty destination database (the case drops its own table first), so `start()` takes the "no tables without db-topic split" exit and returns false. Before this plan that exit left `chc` open and the case carried on; the plan's CLS-01 requirement closes and nulls `chc` on that exit, so the writer is unusable afterwards. The class log has exactly three `Did not find any tables in destination` lines, matching the three failures.
- Production is unaffected by the cause: `ProxySinkTask.start` throws `Connection to ClickHouse is not active.` when `start()` returns false and never uses that writer again. The research did not list `ClickHouseWriterTest` as a user of a writer after a failed `start()`.
- Decision needed (for the planner or the user; I did not choose): (a) change `runWithWriter`, or those three cases, so the destination database holds a table before `start()` (for example create the case's own table first), which touches existing tests the plan says not to touch; (b) change the contract so the "no tables" exit keeps `chc` open for `getTable`/`updateMapping` use, which contradicts the plan's "every exit closes" must-have and Known cost 13 wording; (c) another remedy. Whichever is chosen, E-07 needs a fresh measurement (the rule is to re-measure in a new run, never edit the evidence), and 40-03 must not start before it is green.
- Nothing about the failure involves the V1 per-batch close: the three `[1] V1` cases and the JSON wire-path comparison of that class passed.

## Known Stubs

None. No stub, placeholder or empty data flow was added.

## Threat Flags

None. No new network endpoint, auth path, file access or schema surface; the close-failure WARN logs only the constant `HELPER_CLIENT_CLOSE_FAILURE_LOG` and the exception (T-40-07).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Not ready: gate A is red. Plan 40-03 (Keeper half) is blocked until the three-case question above is decided and E-07 is re-measured green on the writer-half tip.
- Origin `custom_build` still equals `72e8ccff`; nothing was pushed. The `de3faf77` writer-half commit and its evidence sit on `sync/v1.7.0` and are revertable as one commit.

## Self-Check: PASSED

- Created and modified files exist: `ClickHouseWriter.java`, `CountingHelperClient.java`, `WriterHelperClientClosingUnitTest.java`, `FORK-DELTA.md`, `fork-anchors.txt`, `40-COMMAND-EVIDENCE.md` (FOUND).
- Commits `93cd2ad2`, `de3faf77`, `1659e9be`, `41f61da0` are ancestors of HEAD (FOUND); measured `commits: 4` from `plan_head_before` to `plan_head_after`.
- Task 1 and Task 2 verify blocks printed their OK lines (`P40-02-T1-OK` equivalent after the E-05 commit, `P40-02-T2-REST-OK` plus the earlier scoped-Spotless and anchors checks). Task 3's verify cannot pass: E-07 is `fail` by design of the halt, and this is the reported blocker, not a self-check defect.

---
*Phase: 40-helper-client-closing*
*Completed: 2026-11-01 (halted at gate A)*
