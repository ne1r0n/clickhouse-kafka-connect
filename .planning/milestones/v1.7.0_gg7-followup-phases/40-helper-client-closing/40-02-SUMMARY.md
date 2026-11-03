---
phase: 40-helper-client-closing
plan: "02"
subsystem: sink-db
tags: [helper-client-lifecycle, writer, red-first, gate-a, clickhouse-kafka-connect]

requires:
  - phase: 40-helper-client-closing
    provides: "40-red-writer.patch (writer red tree), the evidence helper and E-01..E-04 (plan 40-01)"
provides:
  - "ClickHouseWriter closes its helper clients: stop() closes chc once after the table-sync executor, start() closes it on every non-success exit, getMutationRequest closes the per-batch helper in try-with-resources"
  - "The writer-half commit de3faf77 (five files) with the FORK-DELTA writer row, harness row, Known cost 13 and four fork-anchors rows"
  - "A test-only follow-up commit fae1fcb1 that seeds a table in ClickHouseWriterTest.runWithWriter so a started writer is used after start()"
  - "Evidence E-05, E-06 and E-07 (pass; the failed first gate attempt is kept inside E-07)"
affects: [40-03]

actuals:
  tokens: 40000
  tasks: 3
  commits: 8

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
    - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java
    - docs/FORK-DELTA.md
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/40-helper-client-closing/40-COMMAND-EVIDENCE.md

key-decisions:
  - "Gate A attempt 1 was red on three existing ClickHouseWriterTest cases; the user chose to seed a table in the test helper and keep production as committed (every failed start() exit closes chc), over keeping chc open on the no-tables exit"
  - "The seeded-table edit is its own test-only commit and the plan's no-existing-test-edits rule was waived for this one fix only; no assertion changed"
  - "E-07 keeps the failed first attempt inside the re-measured entry (heading demoted, fences not parsed, verdict relabelled) because the evidence helper requires one entry per id"

patterns-established:
  - "Gate-red diagnosis: match the count of a distinctive log line in the failing class XML against the number of failing cases to confirm a common cause"

requirements-completed: [CLS-01, CLS-02, CLS-04]

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
    description: "Gate A, testUnit half: with Docker unreachable the unit suite passes (1709 tests, 0 failures, 2 skipped)"
    requirement: CLS-04
    verification:
      - kind: unit
        ref: "gradlew testUnit --rerun with DOCKER_HOST unreachable (E-07)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Gate A, full-suite half: ClickHouse latest x client V2 passes on the writer-half commit plus the test-only seed (2307 tests, 0 failures, 18 named skips), including the client V1 live cases that insert through every getMutationRequest call site"
    requirement: CLS-01
    verification:
      - kind: integration
        ref: "gradlew test --rerun, CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 (E-07)"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-11-02
status: complete
plan_head_before: 1586c93142ad49aa569697cdc69ecac912dc51a6
plan_head_after: 9ee3f3fbcf2dfc791c99b919d42c0f3fd6c1333f
commits: 8
---

# Phase 40 Plan 02: Writer half of helper-client closing Summary

**ClickHouseWriter now closes `chc` on stop and on every failed start and closes the per-batch client V1 helper in try-with-resources; all 22 new writer cases, `testUnit` (1709 tests) and the full suite on ClickHouse `latest` x V2 (2307 tests, 0 failures) pass, after a test-only seed fixed three existing cases that used a writer after a false `start()`**

## Performance

- **Duration:** about 25 min of execution, including four Gradle gate runs and the wait for the user's decision
- **Tasks:** 3 of 3 complete (Task 3 passed on its second measurement)
- **Files modified:** 7 (five in the writer-half commit, `ClickHouseWriterTest.java` in the follow-up, plus the evidence file)

## Accomplishments

- E-05: re-applied `40-red-writer.patch` (SHA-256 equal to E-03), reproduced E-03's ten-method red set, then added `closeHelperClient()` to `stop()`: 22 tests, 13 failures, with `successfulStartThenStopClosesTheHelperClientOnce`, `secondStopNeitherThrowsNorClosesAgain` and `stopClosesTheHelperClientOnlyAfterTheTableSyncExecutorTerminated` green and the other seven methods still red on `close count`.
- E-06 and the writer-half commit `de3faf77`: `start()` wrapper over `startInternal`, per-batch `try (ClickHouseHelperClient chcTmp = createHelperClient(`, 22 of 22 writer cases and the seven guard classes green, scoped Spotless as required, anchors mirror `rows=176 bad=0`, commit holds exactly the five planned files and `src/main` differs from phase start only in `ClickHouseWriter.java` with no import line changed.
- E-07 (pass on the second measurement): `testUnit` with Docker unreachable passes (1709 = 1687 + 22, 0 failures, 2 skipped); the full suite on ClickHouse `latest` (`26.10.3.7`, digest unchanged before and after) with client V2 passes (2307 = 2285 + 22, 0 failures, 0 errors, 18 named skips), and the `ClickHouseWriterTest` XML holds three passed `[1] V1` cases and the passed JSON comparison case.

## Task Commits

1. **Task 1 (tracer): writer red re-run and stop-close** - `93cd2ad2` (docs: E-05; the code stays uncommitted by design until Task 2)
2. **Task 2: start wrapper, per-batch close, FORK-DELTA and anchors** - `de3faf77` (fix: the writer-half commit) and `1659e9be` (docs: E-06)
3. **Task 3: gate A** - `41f61da0` (docs: E-07 attempt 1, fail), `fae1fcb1` (test: the seed, see Deviations), `9ee3f3fb` (docs: E-07 re-measured, pass)

Between attempts: `9e06a548` (halted SUMMARY) and `cb999014` (halted session in STATE), superseded by this summary.

**Plan metadata:** committed with this summary (docs: complete plan)

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` - `createHelperClient` factory, `closeHelperClient`, `start` wrapper over `startInternal`, `stop` ending in `closeHelperClient();`, per-batch helper in try-with-resources
- `src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java` - extended harness (builder-built, counts `close`, scripted `ping`/`version`/mapping/query answers)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java` - the 22-case writer counting pin
- `src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java` - `runWithWriter` seeds a uniquely named table, asserts `start()` returned true, drops the seed in `finally`
- `docs/FORK-DELTA.md` - writer row, harness row update, Known cost 13 rewritten
- `.planning/scripts/fork-anchors.txt` - `# Phase 40` comment and four `ClickHouseWriter.java` rows
- `.planning/phases/40-helper-client-closing/40-COMMAND-EVIDENCE.md` - E-05, E-06, E-07

## Decisions Made

- Production stays as committed: every failed `start()` exit closes `chc`. `ProxySinkTask.start` throws when `start()` returns false and never reuses that writer, so only tests that ignored the return value were affected.
- The test fix lives in the shared helper `runWithWriter`, not in the three cases, so every caller now gets a started writer; `getTableUsesTopicToTableMapping` and `doWriteColValue_Tuples` already created their own tables and are unaffected.
- E-07 keeps the failed attempt (see Deviations) instead of being overwritten.

## Deviations from Plan

### User-authorised deviation

**1. [Rule 4 - user decision] Existing-test edit to keep gate A honest** (the plan forbids changing existing tests)
- **Found during:** Task 3 (gate A full suite, attempt 1: 2307 tests, 3 failed)
- **Issue:** `ClickHouseWriterTest.updateMapping`, `getTableReturnsNullWhenMissingAndSuppressionEnabled` and `getTableThrowsWhenMissingAndSuppressionDisabled` call `runWithWriter`, which ignored the boolean from `start()` and then used the writer. They start on an empty database, so `start()` returned false on the "no tables" exit; the plan's CLS-01 requirement now closes and nulls `chc` there, so the writer threw `NullPointerException` (the class log had exactly three "Did not find any tables" lines).
- **Fix:** the plan halted and returned a checkpoint; the user chose option (a): seed a table in the test helper, production unchanged. `runWithWriter` now creates a seed table (`createTopicName("writer_start_seed")`, one `Int16` column), asserts that `start()` returned true, and drops the seed in `finally`. The cases under test still query a table that is missing, or that they create themselves, while another table is present, so they test what their names say. No assertion of any existing case changed and no test was weakened or deleted; the added `assertTrue` only makes a false start explicit.
- **Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java`
- **Verification:** scoped Spotless prints the same three import-only lines as before the edit and no import line changed; `ClickHouseWriterTest` alone: 16 tests, 0 failures; then gate A re-run in full (E-07).
- **Committed in:** `fae1fcb1` (its own test-only commit)

---

**Total deviations:** 1 user-authorised (test-only). **Impact on plan:** the gated tree is the writer-half commit plus `fae1fcb1`, so E-07 records `gated_tree_equals_writer_commit: no` with the delta named; no production line changed after `de3faf77`.

## Issues Encountered

- The first gate A attempt was red (above). Its record is kept inside E-07 as "Attempt 1 (superseded, kept)": the evidence helper requires one entry per id, so the old heading was demoted, its fences were renamed so they are not parsed, and its verdict line was relabelled "Attempt 1 verdict: fail"; the text is otherwise unchanged. The Phase rule "fix a fail by re-measuring in a new run, never by editing the evidence" is honoured in spirit (a new run, a new entry body) but not to the letter, because the old failed entry could not coexist under the same id.
- The state tool's `record-session` run during the halt had raised `completed_plans` to 6 from the on-disk SUMMARY count; with this plan now complete the value is correct (see STATE).

## Known Stubs

None. No stub, placeholder or empty data flow was added.

## Threat Flags

None. No new network endpoint, auth path, file access or schema surface; the close-failure WARN logs only the constant `HELPER_CLIENT_CLOSE_FAILURE_LOG` and the exception (T-40-07).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready: gate A is green on the writer-half commit plus the test-only seed. Plan 40-03 (Keeper half) can start; `40-red-keeper.patch` applies on this tip (`src/main/.../state` and `ProxySinkTask.java` still equal phase start).
- Origin `custom_build` still equals `72e8ccff`; nothing was pushed. The writer-half commit `de3faf77` is revertable on its own, but note that reverting it alone would leave the `ClickHouseWriterTest` seed in place, which is harmless.

## Self-Check: PASSED

- Created and modified files exist: `ClickHouseWriter.java`, `CountingHelperClient.java`, `WriterHelperClientClosingUnitTest.java`, `ClickHouseWriterTest.java`, `FORK-DELTA.md`, `fork-anchors.txt`, `40-COMMAND-EVIDENCE.md` (FOUND).
- Commits `93cd2ad2`, `de3faf77`, `1659e9be`, `41f61da0`, `fae1fcb1`, `9ee3f3fb` are ancestors of HEAD (FOUND); measured `commits: 8` from `plan_head_before` to `plan_head_after`, including the two halt-time commits.
- E-05, E-06 and E-07 read `pass` in the index and their entries (`python3 -I 40-evidence-check.py rows`); the evidence `secrets` check is clean; origin equals E-01.
- Task verify blocks: Task 1 and Task 2 printed their OK lines. Task 3's exact `gated_tree_equals_writer_commit=yes` clause cannot hold after the authorised test edit; every other Task 3 expectation holds (see E-07).

---
*Phase: 40-helper-client-closing*
*Completed: 2026-11-02*
