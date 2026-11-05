---
phase: 46-insert-timeout-limits
plan: "02"
subsystem: sink-db
tags: [client-v2, write-stall, watchdog, cancelTransportRequest, operator-gate, retriable, pins, anchors, evidence]
status: complete

requires:
  - phase: 46-insert-timeout-limits
    provides: plan 46-01's raw-socket StubClickHouseServer responders, B0 3d4ce2e8, R0 9bd0944e, the testUnit 2086 baseline and the 53.9x write-stall margin
provides:
  - ClickHouseWriter.InsertWriteDeadline, a private static nested class armed before client.insert at the three V2 sites and disarmed right after the body stream closes
  - ClickHouseHelperClient.insertBoundMs, the single operator gate both connector bounds read (plan 46-03 reuses it for client V1)
  - InsertBoundPinFixtures, shared routes, records, writer, config and chain checks for the V2 and V1 pin classes
  - InsertWriteStallBoundPinUnitTest (19 V2 cases) and InsertBoundGateUnitTest (20 gate cases)
  - eight TMO-03 anchor rows and the operator row 3 to 4 (anchors mirror rows=208)
  - evidence E-04 (red on R0), E-05 (green in one commit), E-06 (four mutations, queue probe, testUnit, tail gate)
affects: [46-03, 46-04, 46-05]

actuals:
  tokens: 14001
  tasks: 3
  commits: 4
plan_head_before: bc0daf945e8f8144db545941eb551982b834747f
plan_head_after: 9ff3ce7e80fd4958aac35989b21487cd92b1e8e1

tech-stack:
  added: []
  patterns: [write-phase deadline armed before the request and disarmed when the body stream closes, cancel by query id, one lazily created daemon watchdog per writer, one gate method for the operator's socket_timeout, red-first pin on a worktree at R0]

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/InsertBoundPinFixtures.java
    - src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/InsertBoundGateUnitTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/46-insert-timeout-limits/46-COMMAND-EVIDENCE.md

key-decisions:
  - "Serialization time counts against the write deadline: the writer's DataStreamWriter serializes records and writes them to the socket in one pass, so separating the two would need per-write work on the insert hot path; the time already counts against max.poll.interval.ms, which the setting protects"
  - "The watchdog is one lazily created daemon thread named clickhouse-insert-watchdog per writer, created and ended under one lock and shut down in stop(), so a slow cancel on one task never delays another task's deadline and no static state is added"
  - "insertBoundMs returns 0 whenever jdbcConnectionProperties names socket_timeout (0 included) or the timeout is not positive, so the operator keeps ownership of the transport limits (U-3)"
  - "A fired deadline becomes a RetriableException whose message names client V2, clickhouseClientInsertTimeoutMs, the bound and the query id only; the existing Processing wrap and Utils.handleException make it retriable under both tolerances with no DLQ record, so no Utils, Processing or ProxySinkTask change"

requirements-completed: [TMO-03, TMO-02]

coverage:
  - id: D1
    description: "A client V2 write-side stall (the server stops reading the request body, 64 x 1 MiB) ends at the 1000 ms bound as a RetriableException on all three insert routes, direct and through ProxySinkTask.put under errors.tolerance none and all with no DLQ record and one POST; red by the preemptive guard on the unchanged tree"
    requirement: TMO-03
    verification:
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java#writeStalledV2InsertEndsWithinTheBoundAsRetriable"
        status: pass
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java#writeStalledV2InsertThroughPutIsRetriableWithNoDlqRecord"
        status: pass
      - kind: other
        ref: "P46-02-T1-OK (E-04 red on R0), P46-02-T2-OK (E-05 green)"
        status: pass
    human_judgment: false
  - id: D2
    description: "A slow V2 upload still sending at the bound, and a server already streaming progress headers while the body is still sent, are cut at the bound; a response that streams progress after the body is sent keeps running past the bound (U-1 recorded limit, 6.085 s against 1500 ms)"
    requirement: TMO-02
    verification:
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java#progressStreamedWhileTheBodyIsStillSentIsCutAtTheBound"
        status: pass
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java#progressStreamingV2InsertRunsPastTheBoundOnceTheBodyIsSent"
        status: pass
      - kind: other
        ref: "E-06 mutation D1 (disarm removed) turns only the progress-after-sent case red"
        status: pass
    human_judgment: false
  - id: D3
    description: "The operator's socket_timeout keeps ownership: no connector bound when jdbcConnectionProperties names it (a longer value leaves a write stall running past 3 s, a shorter value ends a quiet stall through put as retriable under both tolerances); insertBoundMs is the single gate"
    requirement: TMO-03
    verification:
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/InsertBoundGateUnitTest.java"
        status: pass
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java#operatorSocketTimeoutLeavesAWriteStallToTheOperator"
        status: pass
      - kind: other
        ref: "E-06 mutation D3 (gate ignored) turns 9 cases red"
        status: pass
    human_judgment: false
  - id: D4
    description: "The watchdog is one daemon thread ended by stop(), a late fire after a successful insert does not break the next insert, and its queue is empty after 100 armed and disarmed inserts (probe: queue_size=0, pool_size=1, terminated after stop)"
    requirement: TMO-03
    verification:
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java#insertWatchdogIsADaemonThreadEndedByStop"
        status: pass
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java#lateFireAfterASuccessfulV2InsertIsHarmless"
        status: pass
      - kind: other
        ref: "E-06 throwaway probe (never committed): PROBEQ queue_size=0 pool_size=1 after_stop_terminated=true"
        status: pass
    human_judgment: false
  - id: D5
    description: "Phase gate for this plan: cache-busted testUnit 2131 tests green with 2 skips, src/main changed only in the writer and the helper client, anchors rows=208 bad=0, unedited tail gate PASS with anchor_rows=208"
    requirement: TMO-03
    verification:
      - kind: other
        ref: "P46-02-T3-OK (E-06)"
        status: pass
    human_judgment: false
---

# Phase 46 Plan 02: Client V2 Write-Phase Deadline Summary

**A client V2 insert whose body cannot be sent within `clickhouseClientInsertTimeoutMs` now fails retriably at the bound (cancelled by query id from one daemon watchdog per writer, skipped when the operator named `socket_timeout`), while a progress-streaming response after the body is sent keeps its recorded behaviour; pinned red on the unchanged tree, green in one commit, and bitten by four mutations.**

## Performance

- **Duration:** about 23 min by the session clock (10:00 to 10:24 UTC, most of it Gradle worktree builds and the tail gate)
- **Started:** 2026-11-05T10:00:33Z
- **Completed:** 2026-11-05T10:24Z
- **Tasks:** 3
- **Files:** 7 touched (2 `src/main`, 3 new tests, the anchors file and the evidence file); no new `src/main` file

## Names later plans use

- **F** (fix commit) = `b53cd23c7114e9130ceeb4a552f2d87b1c50ca39` (E-05 `v2_fix_commit`); it holds exactly `ClickHouseWriter.java`, `ClickHouseHelperClient.java`, the three test files and `fork-anchors.txt`.
- Evidence commits: `f9b61959` (E-04), `2776ae08` (E-05), `9ff3ce7e` (E-06). The tail gate was run on `2776ae08` (E-06 `tail_gate_head`).
- Counts: testUnit **2131** (2086 + 6 raw-stub premise + 39 new), anchors **208** rows, `awaitInsertResponse(` **2** (was 4), `operatorSetSocketTimeout` lines **4**.

## Accomplishments

- **Red first (E-04).** 19 V2 cases run in a worktree at R0 (`src/main` byte-identical to B0): the 13 write-stall, slow-upload and progress-while-sending cases were red by the `assertTimeoutPreemptively` guard (still blocked after 10 s against a 1000 ms bound and a 64 MiB body), the watchdog case red by its thread-count assertion, and the five recorded or operator-owned cases green.
- **The deadline (E-05).** `ClickHouseWriter.InsertWriteDeadline` (private static nested class) is built by `insertWriteDeadline(client, queryId)` at the three V2 sites, armed by `insertWithWriteDeadline` immediately before `client.insert`, and disarmed by `deadline.disarm()` right after `out.close()` in each `DataStreamWriter`, so it covers sending only and never the response phase. On fire it sets a flag and calls `client.cancelTransportRequest(queryId)`; the insert then fails and `throwIfFired` converts it to a `RetriableException` ("ClickHouse client V2 insert body not sent within clickhouseClientInsertTimeoutMs=<bound> ms (QueryId: [<id>])", no record value). The watchdog is one core thread, daemon, named `clickhouse-insert-watchdog`, `setRemoveOnCancelPolicy(true)`, created lazily and shut down in `stop()`.
- **The gate (U-3).** `ClickHouseHelperClient.insertBoundMs` returns 0 when `jdbcConnectionProperties` names `socket_timeout` (any value, 0 included) or the timeout is not positive. `setInsertSocketTimeoutV2`, its three call lines and `awaitInsertResponse` (text compared against B0 with a `python3 -I` extract) are unchanged.
- **Bite and gate (E-06).** Four mutations each turned the expected cases red; a throwaway queue probe read `queue_size=0 pool_size=1` after 100 inserts and termination after `stop()`; the cache-busted `testUnit` is green and re-checked against its XML snapshot; the unedited tail gate prints `anchor_rows=208 ... result=PASS`.

## Case times (wall clock through the real writer and a raw loopback stub)

| Case | Unchanged tree (R0) | After the fix |
|---|---|---|
| write stall, 3 routes direct | blocked, guard at 10 s | 1.010, 1.065, 1.042 s (bound 1000 ms) |
| write stall through put, 3 routes x 2 tolerances | blocked, guard at 10 s | 1.018 to 1.082 s |
| slow upload (3.2 MB/s, 64 MiB) | blocked, guard at 10 s | 1.023 s, body bytes already read |
| progress while the body is still sent | blocked, guard at 10 s | 1.013 s |
| progress after the body is sent (bound 1500 ms) | 6.088 s, returns normally | 6.085 s, returns normally (recorded limit U-1) |
| operator `socket_timeout=60000` write stall | still running after 3 s | still running after 3.026 s |

## Mutation red sets (E-06)

- **D1** (disarm emptied, whole-insert deadline): 1 red, `progressStreamingV2InsertRunsPastTheBoundOnceTheBodyIsSent`.
- **D2** (arm emptied, never armed): 14 red, the 13 guard cases plus the watchdog case.
- **D3** (operator gate ignored): 9 red, 8 gate-unit cases plus `operatorSocketTimeoutLeavesAWriteStallToTheOperator`.
- **D4** (`throwIfFired` removed): 13 red; through put the batch fails with a `RuntimeException` under `none` and reaches the DLQ with nothing thrown under `all`.

## Facts plan 46-04 writes into FORK-DELTA

- **Shape.** On client V2 `async=false` inserts, a watchdog armed before `client.insert` cancels the request by query id when the body is not fully sent within `clickhouseClientInsertTimeoutMs`; it is disarmed when the body stream closes, so it does not bound the response phase. A cancelled request is not retried by the client, so one insert makes one POST.
- **Serialization counts.** The writer lambda serializes and sends in one pass, so serialization time is inside the bound.
- **Slow upload and progress while sending.** A V2 insert whose body (serialization included) is still being sent when the bound elapses now fails and is retried even when the upload is progressing and even when the server already streams progress headers (cut at 1.013 s with a 1000 ms bound); the server may keep what it received, so with `exactlyOnce=false` the retry can duplicate.
- **Recorded limit (U-1).** Once the body has been sent, a server that streams progress headers keeps the insert running past the bound (6.085 s against 1500 ms, the raw-stub shape); not bounded by decision.
- **Operator gate.** When `jdbcConnectionProperties` names `socket_timeout` the connector adds no wall-clock bound on V2 (and, with plan 46-03, none on V1).
- **Cost.** One daemon thread per writer that has run a bounded insert, ended by `stop()`; the queue stays empty (one task per insert, removed on cancel).

## Task Commits

1. **Task 1 (tracer): pins red on R0** - `f9b61959` (E-04); the two new test files stayed uncommitted until Task 2
2. **Task 2: the deadline, the gate, the pins and the anchors** - `b53cd23c` (code and tests), `2776ae08` (E-05)
3. **Task 3: bite and gate** - `9ff3ce7e` (E-06)

**Plan metadata:** this SUMMARY is committed alone; STATE.md, ROADMAP.md and REQUIREMENTS.md are left to the orchestrator, per the dispatch instructions.

`actuals.commits: 4` is the measured `git rev-list --count plan_head_before..plan_head_after`; `actuals.tokens` is chars/4 over the added lines under `src`, the anchors file and the evidence file.

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` - nested `InsertWriteDeadline`, `insertWriteDeadline`, `insertWithWriteDeadline`, the lazily created watchdog and its shutdown in `stop()`
- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` - `insertBoundMs`
- `src/test/java/com/clickhouse/kafka/connect/sink/InsertBoundPinFixtures.java` - shared fixtures (routes, small and 64 x 1 MiB records, table, config, `PinWriter`, chain and message checks)
- `src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java` - 19 V2 cases
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/InsertBoundGateUnitTest.java` - 20 gate cases
- `.planning/scripts/fork-anchors.txt` - eight TMO-03 rows, operator row 3 to 4
- `.planning/phases/46-insert-timeout-limits/46-COMMAND-EVIDENCE.md` - E-04..E-06

## Decisions Made

See `key-decisions` above. The decisions that belong to the user (U-1 record the progress case, U-3 the operator gate, U-7 the write-phase deadline with serialization counted) were applied as recorded in CONTEXT.md; nothing new came up.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] Thread-safe lazy creation of the watchdog**
- **Found during:** Task 2
- **Issue:** the plan names a plain field created lazily on the task thread and shut down in `stop()`, which the Connect worker thread calls; a `stop()` racing the first bounded insert could leave a created executor un-shut-down (AGENTS.md: all concurrent operations must be safe).
- **Fix:** the field is guarded by a private lock object in the creation method and in `stop()`. The anchor row on `insertWatchdog.shutdownNow()` still counts 1.
- **Files modified:** `ClickHouseWriter.java`
- **Commit:** `b53cd23c`

**Total deviations:** 1 auto-fixed (1 missing critical). **Impact:** none on behaviour or on any anchor count.

### Observations (not rule triggers)

- In `doInsertRawBinaryV2` the `format` local became a ternary (`supportDefaults ? RowBinaryWithDefaults : RowBinary`), because the insert call now sits inside a supplier lambda and needs an effectively final variable; behaviour is identical. In `doInsertJsonV2` the existing conditional between the column-list and plain insert moved into a local `Supplier` unchanged; `csc.getJsonInsertColumns()` is still one line.
- `arm()` logs a WARN carrying only the query id if the canceller throws, instead of letting the executor swallow it silently.
- The watchdog pin compares the set of live `clickhouse-insert-watchdog` threads before and after the insert (exactly one new daemon thread, dead after `stop()`), which is the plan's count check made robust against threads another class left ending.
- The put cases also assert that the bound's own message is in the failure chain, so a put test cannot pass on a read timeout.
- The two touched main files report `preexisting` for the per-file formatting check: `spotlessApply` changes only their import ordering, and does so on the unchanged HEAD copies as well; the three test files were identical after `spotlessApply`.
- The first run of the Task 3 verify block failed with `INVARIANT 0 VIOLATION ... does not resolve to a commit` because the merge SHA `M` was mistyped in that command (not in the evidence); the corrected command printed `P46-02-T3-OK`, with the same recorded facts.
- A first background launch of the mutation runs did nothing (zsh aborted on an unmatched `rm -f` glob before it); it was relaunched with no side effects and every mutation ran once.

## Issues Encountered

None. No JVM crash occurred; the green runs passed on the first attempt after the code was written, and no crash file was left in the repository root.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None. No stub pattern was added under `src/main`.

## Threat Flags

None beyond the plan's register. The new surface is the in-JVM watchdog thread and the cancel-by-query-id call already modelled as T-46-04, T-46-05 and T-46-06; the new exception text carries the client version, the setting name, the bound and the query id only, and the pins assert the record sentinel is in no message or stack-trace line (T-46-08).

## Next Phase Readiness

- Ready for plan 46-03 (client V1 bounded wait): `insertBoundMs` is the gate to reuse, `InsertBoundPinFixtures` carries `clientVersion` parameters for a V1 pin class, and the anchors count stays 208 until 46-03 adds its rows (212 expected).
- Expected counts for later plans: testUnit starts at 2131; the anchors mirror reads `rows=208 bad=0`.
- Rollout consequence this plan creates (for plans 46-04 and 46-05): a V2 insert still sending its body, serialization included, at `clickhouseClientInsertTimeoutMs` fails and is retried even when the upload progresses or the server already streams progress; the server may keep what it received.

## Self-Check: PASSED

- Files: the three new test files, `ClickHouseWriter.java` with `InsertWriteDeadline`, `ClickHouseHelperClient.java` with `insertBoundMs`, and `46-COMMAND-EVIDENCE.md` present.
- Commits `f9b61959`, `b53cd23c`, `2776ae08` and `9ff3ce7e` are ancestors of HEAD.
- Verify blocks P46-02-T1-OK, P46-02-T2-OK and P46-02-T3-OK printed on the committed tip; anchors mirror `rows=208 bad=0`; no worktree is registered.
