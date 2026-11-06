---
phase: 46-insert-timeout-limits
plan: "03"
subsystem: sink-db
tags: [client-v1, future-get, bounded-wait, operator-gate, retriable, pins, anchors, evidence]
status: complete

requires:
  - phase: 46-insert-timeout-limits
    provides: plan 46-02's ClickHouseHelperClient.insertBoundMs operator gate, InsertBoundPinFixtures, the raw-socket stub responders, B0 3d4ce2e8, R0 9bd0944e, testUnit 2131 and anchors rows=208
provides:
  - ClickHouseWriter.awaitInsertResponseV1, the single bounded wait the three client V1 insert sites call
  - InsertV1WaitBoundPinUnitTest (21 Docker-free V1 cases)
  - four TMO-03 anchor rows (anchors mirror rows=212)
  - evidence E-07 (V1 pipe measured, pins red on R0), E-08 (green in one commit), E-09 (fixed-tree body-write measurement, three mutations, testUnit, tail gate)
affects: [46-04, 46-05]

actuals:
  tokens: 10101
  tasks: 3
  commits: 4
plan_head_before: 4d5a9dd1ba2b7ee4c356e30a7ac244b394832aba
plan_head_after: db61a7971bfc4bf76f98dad39131f372d1fa3063

tech-stack:
  added: []
  patterns: [bounded Future.get with cancel and RetriableException over the TimeoutException, one operator gate shared by the V2 deadline and the V1 wait, red-first pin on a worktree at R0, detector verified against a live stall]

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/InsertV1WaitBoundPinUnitTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/46-insert-timeout-limits/46-COMMAND-EVIDENCE.md

key-decisions:
  - "The V1 pin body is 64 MiB: the pipe measurement (E-07) showed the SinkTask thread in the future wait, not in the pipe write, at 16, 64 and 256 MiB, so the plan's rule picks 64"
  - "No code is added for a stall inside the V1 body write: none was seen at any size up to 256 MiB, so the measure-first branch resolves to not applicable rather than bounded or not bounded by decision"
  - "awaitInsertResponseV1 reuses insertBoundMs, so an operator-named socket_timeout (0 included) leaves the V1 wait unbounded and the only bare future.get() left in the class sits in that gated branch"
  - "A timed-out V1 wait cancels the future and throws a RetriableException whose cause is the TimeoutException; message text is fixed to client V1, the setting name, the bound and the query id"

requirements-completed: [TMO-03]

coverage:
  - id: D1
    description: "A client V1 insert whose response is not received within clickhouseClientInsertTimeoutMs (a 64 MiB body the server stops reading, a server that reads the body and never answers, a server streaming progress headers) ends at the bound as a RetriableException over a TimeoutException on all three routes, direct and through ProxySinkTask.put under errors.tolerance none and all, with no DLQ record and one POST; red by the preemptive guard on the unchanged tree"
    requirement: TMO-03
    verification:
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertV1WaitBoundPinUnitTest.java#writeStalledV1InsertEndsWithinTheBoundAsRetriable"
        status: pass
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertV1WaitBoundPinUnitTest.java#quietV1InsertEndsWithinTheBoundAsRetriable"
        status: pass
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertV1WaitBoundPinUnitTest.java#progressStreamingV1InsertEndsWithinTheBoundAsRetriable"
        status: pass
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertV1WaitBoundPinUnitTest.java#boundedV1InsertThroughPutIsRetriableWithNoDlqRecord"
        status: pass
      - kind: other
        ref: "P46-03-T1-OK (E-07 red on R0, 18 of 21 by the guard), P46-03-T2-OK (E-08 green)"
        status: pass
    human_judgment: false
  - id: D2
    description: "After a timed-out V1 write-stall insert, with the stub still holding the connection, no client thread stays blocked in a socket write; the detector was shown to see a live blocked writer (one ClickHouseWorker thread) on JDK 21 and on the default toolchain"
    requirement: TMO-03
    verification:
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertV1WaitBoundPinUnitTest.java#timedOutV1InsertLeavesNoClientThreadBlockedInASocketWrite"
        status: pass
      - kind: other
        ref: "E-09 throwaway detector probe (never committed): blocked_writer_threads=1"
        status: pass
    human_judgment: false
  - id: D3
    description: "The operator's socket_timeout keeps ownership of the V1 limits: a longer value leaves a write stall running past 3 s with no connector bound in the chain, a shorter value ends a quiet stall through put as retriable under both tolerances; insertBoundMs is the single gate"
    requirement: TMO-03
    verification:
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertV1WaitBoundPinUnitTest.java#operatorSocketTimeoutLeavesTheV1WaitToTheOperator"
        status: pass
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertV1WaitBoundPinUnitTest.java#operatorShorterSocketTimeoutEndsAV1QuietStallThroughPut"
        status: pass
      - kind: other
        ref: "E-09 mutation V1b (gate ignored) turns only the longer-operator case red"
        status: pass
    human_judgment: false
  - id: D4
    description: "Whether the V1 bound ends a stall inside the body write was measured: no size up to 256 MiB blocked the SinkTask thread in the pipe write (it reached the future wait at 16, 64 and 256 MiB on the unchanged tree and the bound ended the 256 MiB insert at 1530 ms on the fixed tree), so the question is not applicable and no code was added"
    requirement: TMO-03
    verification:
      - kind: other
        ref: "E-07 PROBEV1PIPE tree=base x3, E-09 PROBEV1PIPE tree=fixed (P46-03-T1-OK, P46-03-T3-OK)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Phase gate for this plan: cache-busted testUnit 2152 tests green with 2 skips, src/main changed only in the writer and the helper client, anchors rows=212 bad=0, unedited tail gate PASS with anchor_rows=212, three mutations each turn the expected cases red"
    requirement: TMO-03
    verification:
      - kind: other
        ref: "P46-03-T3-OK (E-09)"
        status: pass
    human_judgment: false
---

# Phase 46 Plan 03: Client V1 Bounded Insert Wait Summary

**The three client V1 insert sites now wait for the response through `awaitInsertResponseV1`, a bounded `get` from the same `insertBoundMs` gate as the V2 deadline, so a V1 write stall, quiet stall or progress-streaming insert fails retriably at `clickhouseClientInsertTimeoutMs` (skipped when the operator named `socket_timeout`); pinned red on the unchanged tree, green in one commit, and bitten by three mutations.**

## Performance

- **Duration:** about 25 min by the session clock (10:29 to 10:54 UTC, most of it Gradle worktree builds, the mutation runs and the tail gate)
- **Started:** 2026-11-05T10:29:29Z
- **Completed:** 2026-11-05T10:54Z
- **Tasks:** 3
- **Files:** 4 touched (`ClickHouseWriter.java`, one new test class, the anchors file and the evidence file); `ClickHouseHelperClient.java` untouched in this plan; no new `src/main` file

## Names later plans use

- **F** (fix commit) = `05b46d18779d0981cf77e234c74df87062829450` (E-08 `v1_fix_commit`); it holds exactly `ClickHouseWriter.java`, `InsertV1WaitBoundPinUnitTest.java` and `fork-anchors.txt`.
- Evidence commits: `35b640ed` (E-07), `462611cb` (E-08), `db61a797` (E-09). The tail gate was run on `462611cb` (E-09 `tail_gate_head`).
- Counts: testUnit **2152** (2086 + 6 raw-stub premise + 39 V2 + 21 V1), anchors **212** rows, `awaitInsertResponseV1(` **4**, the old bare try line **0**, `future.get()` **1** (inside the gated branch), `chc.insertBoundMs(` **2**.

## Accomplishments

- **The V1 pipe, read and measured (E-07).** `createPipedOutputStream` returns the deprecated `NonBlockingPipedOutputStream` (the blocking one only with `use_blocking_queue`, default false). By the code it holds at most 512 buffers of 8192 bytes (4 MiB) and a blocked write ends with "Write timed out after <ms> ms" once the wait reaches the socket timeout (30000 ms by default). Measured on the unchanged tree with the real V1 client against `notReadingBody()`: at 16, 64 and 256 MiB the SinkTask thread was in a `CompletableFuture` wait at the 3 s sample and the call had not ended at 60 s, so the worker drains the pipe as fast as the connector fills it and no size up to 256 MiB reached the pipe write. Where those bytes go was not determined.
- **Red first (E-07).** The 21 V1 cases ran in a worktree at R0 (`src/main` byte-identical to B0): the 18 bounded and release cases were red by the `assertTimeoutPreemptively` guard (10000 ms for the write-stall and quiet shapes, 5000 ms for progress), the three operator cases green.
- **The bound (E-08).** `ClickHouseWriter.awaitInsertResponseV1(future, queryId)` reads `chc.insertBoundMs(csc.getClickhouseClientInsertTimeoutMs())`; at 0 (operator named `socket_timeout`, or a non-positive timeout) it returns `future.get()`, otherwise `future.get(bound, MILLISECONDS)`, and on `TimeoutException` it cancels the future and throws `RetriableException("ClickHouse client V1 insert response not received within clickhouseClientInsertTimeoutMs=<bound> ms (QueryId: [<id>])", e)`. The three sites call it; the piped stream, `stream.close()`, the client try-with-resources, the log lines and the statistics call are unchanged, and no thread or executor was added. The existing `Processing` wrap and `Utils.handleException` make it retriable under both tolerances with no DLQ record.
- **Bite and gate (E-09).** Three mutations each turned the expected cases red; the 256 MiB stall on the fixed tree ended in 1530 ms with the V1 message; the cache-busted `testUnit` is green and re-checked against its XML snapshot; the unedited tail gate prints `anchor_rows=212 ... result=PASS`.

## Case times (wall clock through the real writer and a raw loopback stub)

| Case | Unchanged tree (R0) | After the fix |
|---|---|---|
| write stall, 64 MiB, 3 routes direct | blocked, guard at 10 s (research: > 80 s) | 1.137, 1.145, 1.200 s (bound 1000 ms) |
| write stall through put, 3 routes x 2 tolerances | blocked, guard at 10 s | 1.121 to 1.218 s |
| quiet stall (reads the body, never answers), 3 routes | guard at 10 s (the library ends it at about 30 s) | 0.313 to 0.325 s (bound 300 ms) |
| progress-streaming insert (bound 1500 ms) | guard at 5 s (research: about 6.2 s, returns normally) | 1.510 s |
| 256 MiB write stall (probe, bound 1000 ms) | still in the wait at 60 s | 1.530 s including about 0.5 s of writing the body |
| operator `socket_timeout=60000` write stall | still running after 3 s | still running after 3.024 s |
| operator `socket_timeout=200` quiet stall through put | ended retriably | 0.22 to 0.73 s |

## Mutation red sets (E-09)

- **V1a** (`future.get(Long.MAX_VALUE, ...)`, still compiles): 18 red, every bounded and release case, all by the preemptive guard; the three operator cases green.
- **V1b** (`insertBoundMs` ignores the operator flag): 1 red, `operatorSocketTimeoutLeavesTheV1WaitToTheOperator`. The shorter-operator put cases stay green under it because the library's own read timeout (200 ms) still ends them first.
- **V1c** (`RuntimeException` instead of `RetriableException`): 18 red, the 10 put cases and the 8 direct and release cases that look for a `RetriableException` in the chain.

## Facts plan 46-04 writes into FORK-DELTA

- **Shape.** On client V1 (the default `client_version`) the wait for the insert response is bounded by `clickhouseClientInsertTimeoutMs` through a bounded `Future.get`; on timeout the future is cancelled, the caller's try-with-resources closes the piped stream and the client (no client thread stays in a socket write within the 1.34 s the release case takes), and a `RetriableException` is thrown. One insert makes one POST.
- **What it covers (measured).** A 16, 64 or 256 MiB body whose receiver stops reading ends at the bound: the SinkTask thread reaches the response wait, so the V1 write stall is bounded for every size measured. A quiet stall ends at the bound instead of the library's 30 s inactivity limit; a progress-streaming V1 insert is cut at the bound (1.51 s against 1500 ms), where the unchanged tree let it run to its end.
- **Residual, not measured (plan-check B2a).** No stall inside the V1 body write was seen at any size up to 256 MiB, so `v1_bound_ends_pipe_write_stall` reads `not applicable` and nothing was added. By the code reading, a body larger than the pipe with a consumer that stops draining would block the SinkTask thread in the pipe write until the stream's write timeout (the socket timeout, 30000 ms by default) raises "Write timed out", which the connector already classifies as retriable; this was not reached. FORK-DELTA should say "not reached up to 256 MiB" and must not say "bounded" or "cannot be bounded" for it.
- **Operator gate.** When `jdbcConnectionProperties` names `socket_timeout` (0 included) the V1 wait is left unbounded, as on V2; the only bare `future.get()` in the class is in that branch.
- **Rollout consequence.** Any V1 insert still waiting for its response when the bound elapses (quiet, slow, or progress-streaming) now fails and is retried. Before, a quiet V1 insert already failed at the library's 30 s inactivity limit, but a progress-streaming or slowly answered one ran to its end. The server may still commit a timed-out insert, so with `exactlyOnce=false` the retry can duplicate rows; with `exactlyOnce=true` the retry reuses the dedup token.
- **Cost.** No thread, executor or config key was added.

## Task Commits

1. **Task 1 (tracer): the pipe measured and the pins red on R0** - `35b640ed` (E-07); the pin class stayed uncommitted until Task 2
2. **Task 2: the bounded V1 wait, the pins and the anchors** - `05b46d18` (code and tests), `462611cb` (E-08)
3. **Task 3: bite and gate** - `db61a797` (E-09)

**Plan metadata:** this SUMMARY is committed alone; STATE.md, ROADMAP.md and REQUIREMENTS.md are left to the orchestrator, per the dispatch instructions.

`actuals.commits: 4` is the measured `git rev-list --count plan_head_before..plan_head_after`; `actuals.tokens` is chars/4 over the added lines under `src`, the anchors file and the evidence file.

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` - `awaitInsertResponseV1`, `V1_INSERT_TIMEOUT_FORMAT`, the three V1 sites routed through it
- `src/test/java/com/clickhouse/kafka/connect/sink/InsertV1WaitBoundPinUnitTest.java` - 21 V1 cases
- `.planning/scripts/fork-anchors.txt` - one comment line and four TMO-03 rows
- `.planning/phases/46-insert-timeout-limits/46-COMMAND-EVIDENCE.md` - E-07..E-09

## Decisions Made

See `key-decisions` above. Nothing belonging to the user came up: U-2 and U-3 were applied as recorded in CONTEXT.md, and the measure-first branch for the body write resolved to `not applicable` on the measurement, so no decision was needed.

## Deviations from Plan

None - plan executed exactly as written.

### Observations (not rule triggers)

- The release pin's detector ignores threads that have a `StubClickHouseServer` frame, so the stub's own reader and writer threads cannot trip it; the plan names only a socket-write frame together with a `com.clickhouse.` frame. A throwaway probe confirmed the detector finds the live blocked writer (`ClickHouseWorker-1`) during a stall, on JDK 21 and on the default OpenJ9 toolchain, so the release case is not vacuous.
- The first fixed-tree probe run printed `elapsed_ms=3007`; that included the probe's own 3 s sampling sleep (the insert had already ended). The probe was changed to read the thread's own end time and re-run (1530 ms); both figures are explained in E-09. The E-07 base lines were unaffected (no base run ended).
- The probes were run on JDK 21 with a 3 GB heap in scratch worktrees, as the plan says; the pin class and the neighbour, mutation and full runs used the default toolchain (OpenJ9 17) except where stated, and no JVM crash occurred.
- `ClickHouseWriter.java` reports `preexisting` for the per-file formatting check: `spotlessApply` changes only its import ordering and does so on the unchanged HEAD copy as well (the text from the class declaration on is equal); the pin class was identical after `spotlessApply`.
- The tail gate was run on `462611cb` (the E-08 commit), as plan 46-02 did on its E-05 commit, and the T3 verify block re-ran it unedited over that head.

## Issues Encountered

None. The red run, the green run and every neighbour run passed on the first attempt; no crash file was left in the repository root.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None. No stub pattern was added under `src/main`.

## Threat Flags

None beyond the plan's register. The new surface is a bounded wait on an existing future (T-46-09, T-46-11); the new exception text carries the client version, the setting name, the bound and the query id only, and the pins assert the record sentinel is in no message or stack-trace line (T-46-13).

## Next Phase Readiness

- Ready for plan 46-04 (live TMO-02 re-measurement, non-insert inventory, FORK-DELTA rows): the facts above are what it writes for client V1, including the "not reached up to 256 MiB" residual wording and the rollout consequence.
- Plan 46-05 help text: on V1 the setting now bounds the wait for the insert response (write stall, quiet stall and progress-streaming included, for the sizes measured); with an operator `socket_timeout` the connector adds no bound on either client.
- Expected counts for later plans: testUnit starts at 2152; the anchors mirror reads `rows=212 bad=0` until plan 46-05 adds its two rows (214).

## Self-Check: PASSED

- Files: `InsertV1WaitBoundPinUnitTest.java`, `ClickHouseWriter.java` with `awaitInsertResponseV1` and `46-COMMAND-EVIDENCE.md` present.
- Commits `35b640ed`, `05b46d18`, `462611cb` and `db61a797` are ancestors of HEAD.
- Verify blocks P46-03-T1-OK, P46-03-T2-OK and P46-03-T3-OK printed; anchors mirror `rows=212 bad=0`; no worktree is registered.
