---
phase: 47-task-stop-retry-classification
plan: "02"
subsystem: testing
tags: [retry-classification, redelivery, characterization-pin, mutation-bite, connect-worker-measurement, fork-delta]
status: complete

requires:
  - phase: 47-task-stop-retry-classification
    provides: plan 47-01's fix commit F1, the E-01 baselines (testUnit 2181, eight pin classes), anchors at 218 rows
provides:
  - RetriableRedeliveryLimitPinUnitTest, 108 Docker-free cases, no Mockito, green on the unchanged production code
  - 47-rty01-expected-added.tsv (the 108 added testcase rows)
  - E-05 (pin, budget-of-5 mutation, testUnit gate), E-06 (live Connect worker measurement and source read), E-07 (FORK-DELTA item 17)
  - FORK-DELTA item 17 rewritten as a recorded limit, by decision (U-1), with measured numbers only
affects: [47-03, 47-04, 49]

actuals:
  tokens: 16136
  tasks: 2
  commits: 5
plan_head_before: a6baa8b7f9613241cff65ee46530cbff4e4de637
plan_head_after: f5f02f7595e431f032b41f7296b7c4d54af05b78

tech-stack:
  added: []
  patterns: [a counting InMemoryDBWriter that throws a shape's exception on every doInsert, 500 consecutive redeliveries per row, the pin's teeth shown by a budget mutation in a removed worktree, a throwaway Connect worker measurement with docker cp instead of bind mounts]

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/RetriableRedeliveryLimitPinUnitTest.java
    - .planning/phases/47-task-stop-retry-classification/47-rty01-expected-added.tsv
  modified:
    - docs/FORK-DELTA.md
    - .planning/phases/47-task-stop-retry-classification/47-COMMAND-EVIDENCE.md

key-decisions:
  - "U-1 applied: today's unbounded redelivery is recorded and pinned, with no config key, counter, budget or sleep anywhere in the connector; no src/main file changed in this plan"
  - "The pin is a characterization pin, so it is not red first; a budget-of-5 mutation in a removed worktree is its evidence that it can fail (exactly the 64 redelivery rows, each at redelivery 6)"
  - "FORK-DELTA item 17 states the redelivery interval as the worker's offset.flush.interval.ms, not as a connector fact, and states exactlyOnce=true as not pinned"

requirements-completed: [RTY-01]

coverage:
  - id: D1
    description: "RetriableRedeliveryLimitPinUnitTest pins, through ProxySinkTask.put under errors.tolerance=none and all, the first-put outcome of 22 writer-failure shapes (44 literal rows) and, for each of the 16 retriable shapes through ProxySinkTask.put and ChunkFlusher.putDirect, a RetriableException on each of 500 consecutive redeliveries with errors.retry.timeout=60 and retryCount=3 set, the writer called 500 times and no DLQ record (64 rows); 108 green cases with Docker unreachable"
    requirement: RTY-01
    verification:
      - kind: unit
        ref: "RetriableRedeliveryLimitPinUnitTest (108 tests, 0 failures; E-05; P47-02-T1-OK)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The pin has teeth against any budget under 500: a consecutive-failure counter of 5 added to ProxySinkTask.put in a throwaway worktree turned exactly the 64 redelivery rows red, every failure at redelivery 6, and left the 44 first-put rows green; the main tree's ProxySinkTask.java equals the phase base and the worktree is removed"
    requirement: RTY-01
    verification:
      - kind: unit
        ref: "RetriableRedeliveryLimitPinUnitTest#retriableShapeIsRedeliveredWithoutBound on the mutated worktree (E-05, xml-rty01-mut)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The testUnit gate with Docker unreachable is green at 2295 tests (E-01's 2181 plus 114) with 2 skips; the per-testcase delta against E-01 is removed=0 added=114 (6 LIF-01 rows and 108 RTY-01 rows); the eight classification and stop pin classes keep E-01's counts; no src/main change since the 47-01 fix commit; anchors mirror rows=218"
    requirement: RTY-01
    verification:
      - kind: other
        ref: "P47-02-T1-OK (Task 1 automated verify; E-05)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Measured on a real Connect worker of the Kafka 3.9 line (7.9.7-ccs) and read in the WorkerSinkTask sources of 3.9.1, 4.1.1 and 4.4.0: a task that always throws RetriableException from put is redelivered at offset.flush.interval.ms with no count and no effect of errors.retry.timeout, errors.tolerance or a DLQ topic (DLQ stays empty, task stays RUNNING, one ERROR line per redelivery); a DataException from put fails the task once under errors.tolerance=all with an empty DLQ"
    requirement: RTY-01
    verification:
      - kind: other
        ref: "E-06 (live worker instants and source facts; P47-02-T2-OK range check)"
        status: pass
    human_judgment: false
  - id: D5
    description: "FORK-DELTA item 17 is a recorded limit by decision: new title, classification sentences verbatim, Connect behaviour with E-06's numbers, the pin with E-05's counts, exactlyOnce=true stated as not pinned, the Phase 46 interaction labelled derived, no open FUT-05 reference; one docs commit changing only that line"
    requirement: RTY-01
    verification:
      - kind: other
        ref: "P47-02-T2-OK (Task 2 automated verify; E-07)"
        status: pass
    human_judgment: false
---

# Phase 47 Plan 02: RTY-01 limit pin and FORK-DELTA item 17 Summary

**The unbounded redelivery of retriable writer failures is now pinned through `ProxySinkTask.put` under both `errors.tolerance` values (108 Docker-free cases, shown to fail against a retry budget of 5), measured on a real Connect worker, and recorded in FORK-DELTA item 17 as a limit by decision, with no `src/main` change.**

## Performance

- **Duration:** 43 min (start 2026-11-05T20:27:05Z, end about 21:11Z).
- **Tasks:** 2 (tracer plus auto), 4 files changed (2 created, 2 modified), 5 commits before this SUMMARY.

## Accomplishments

- `RetriableRedeliveryLimitPinUnitTest` (PC = `097243fd9b24a4ebf27ff548b3ed0ae450f00c3a`): 44 first-put rows (22 shapes x 2 tolerance values, literal outcomes) and 64 redelivery rows (16 retriable shapes x `ProxySinkTask.put` and `ChunkFlusher.putDirect` x 2 values), each of 500 consecutive redeliveries throwing `RetriableException`, writer called 500 times, DLQ empty. 108 tests, 0 failures with `DOCKER_HOST=unix:///nonexistent.sock`; the pin class took 22.037 s. The expected-added list (`47-rty01-expected-added.tsv`, SHA-256 `c4f61823...6c94`) holds the 108 PASS rows.
- Mutation (E-05): a consecutive-failure counter of 5 in `ProxySinkTask.put`, in the removed worktree `/tmp/gsd47/wt-mut` only, turned exactly the 64 redelivery rows red, all 64 messages reading `redelivery 6 ==> Unexpected exception type thrown, expected: RetriableException`, and left the 44 first-put rows green; the main tree's `ProxySinkTask.java` equals the phase base.
- Gate (E-05): testUnit with Docker unreachable 2295 tests (2181 + 6 + 108), 0 failures, 0 errors, 2 skips, `delta: removed=0 added=114 expected=114 match=yes`, the eight pin classes at E-01's counts (14, 38, 216, 4, 21, 19, 2, 15), anchors `rows=218 bad=0`, no `src/main` change since the 47-01 fix commit. The full suite is Phase 47's phase gate (plan 47-04), not this plan's.
- Live measurement (E-06): throwaway single-node KRaft `confluentinc/cp-kafka:7.9.10` and `confluentinc/cp-kafka-connect-base:7.9.7` (runtime `7.9.7-ccs`, the Kafka 3.9 line) running the research's `ProbeSink` with the connector name added to its two print lines. Results: c1 (always `RetriableException`, `errors.retry.timeout=5000`, `errors.tolerance=all`, DLQ `dlq1`) called at 506, 60042 and 120055 ms; c3 (Connect defaults) at 449, 60012 and 120013 ms (gap between attempts 2 and 3: 60013 and 60001 ms; third attempts 42 ms apart); both tasks `RUNNING`, DLQ end offsets 0, three ERROR lines each (one per attempt); with `offset.flush.interval.ms=7000` c3 was called at 19856, 21065, 28069, 35073 and 42079 ms (gaps from attempt 2: 7004, 7004 and 7006 ms); c2 (always `DataException`, `errors.tolerance=all`, DLQ `dlq2`) was called once and ended `FAILED` with `Exiting WorkerSinkTask due to unrecoverable exception`, DLQ empty. Containers and network removed.
- Source read (E-06): the body of `private void deliverMessages()` in `WorkerSinkTask.java` is byte-identical in connect-runtime 3.9.1, 4.1.1 and 4.4.0, calls `task.put(` with no `retryWithToleranceOperator`, catches `RetriableException` and calls `pauseAll()`; `ERRORS_RETRY_TIMEOUT_DEFAULT = 0` (4.1.1); the connector never calls `SinkTaskContext.timeout` (`grep -rn 'context.timeout(' src/main/java` finds 0 lines).
- FORK-DELTA item 17 (commit `e1f3947454f15ae51e245e6522ff27c7399e50fe`, E-07): the new title, the existing classification sentences kept verbatim, and the planned tail with E-05's and E-06's numbers; one line changed (1 insertion, 1 deletion). `(FUT-05)` stays on exactly one FORK-DELTA line (item 8, closed in plan 47-03).

## Baseline numbers later plans compute from

| Measure | Value |
|---|---|
| Pin commit PC | `097243fd9b24a4ebf27ff548b3ed0ae450f00c3a` |
| testUnit after this plan, Docker unreachable | 2295 tests, 0 failures, 0 errors, 2 skipped, 105 files; delta vs E-01 removed=0 added=114 |
| Planned full suite at the phase gate | 2784 + 114 = 2898 (not run here; plan 47-04 measures it) |
| Anchors mirror | 218 rows (unchanged by this plan) |
| Mutation | 108 tests, 64 failures (all `redelivery 6`), 44 first-put green |
| FORK-DELTA item 17 commit | `e1f3947454f15ae51e245e6522ff27c7399e50fe` |
| Connect runtime measured | `7.9.7-ccs`; images `confluentinc/cp-kafka-connect-base@sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055`, `confluentinc/cp-kafka@sha256:07c3d60362e67c296f7f34b690b1240e5ecdcde4a84f5e73cab320fb4041f58c` |
| JVM crash / `-PjavaVersion=21` | none; every run used the default toolchain |

## Deviations from Plan

None - plan executed exactly as written.

Process notes, not deviations. (1) The first run of the flush-7000 worker reached only 4 attempts inside the plan's 60 s window, because the consumer group of c3 still held the member of the force-removed first worker and the rebalance waited for its session to expire (first `put` at 40639 ms). Its gaps from attempt 2 (7006 and 7008 ms) already agreed with the recorded run. I re-ran the same worker once with the same files and the observation window capped at 150 s instead of 60 s; that run (5 attempts, gaps 7004, 7004 and 7006 ms) is the recorded one, and the first run's log is kept at `/tmp/gsd47/exec/live/connect-flush7000-run1.log`. No value fell outside its planned range in the recorded runs, and nothing was adapted to a measurement. (2) The first `put` instants are not a cadence fact (they include consumer start-up and, in the flush-7000 runs, the rebalance wait); the gaps are. They differ from the research run only in the first call (about 0.5 s here against 3.6 s there); the research's gaps (60001 and 60008 ms) and these (60013 and 60001 ms) agree. (3) `.planning/STATE.md`, `.planning/ROADMAP.md` and `.planning/REQUIREMENTS.md` were not touched or staged (the orchestrator owns them), and `.planning/config.json` and `.planning/state.json` stay dirty and unstaged.

## Authentication Gates

None.

## Issues Encountered

None.

## Known Stubs

None.

## Threat Flags

None. No `src/main` change, no new endpoint or trust-boundary surface; T-47-08 (the pin's teeth), T-47-09 (item 17 numbers all from E-05/E-06, verified by the verify), T-47-10 (no removed row), T-47-12 (mutation only in a removed worktree) and T-47-13 (sources read only with `python3 -I`, containers and network removed and checked) are mitigated as planned.

## Ready for

Plan 47-03 (FUT-05 closed in FORK-DELTA items 8 and 24, the new `# 1.7.0_gg8, Unreleased` CHANGELOG section). Hand-offs unchanged from the plan: the gg7 rollout rows F5/R8 and its "FUT-05 stays open." line are Phase 49's.

## Self-Check: PASSED

Created files exist (the pin, `47-rty01-expected-added.tsv`); commits `097243fd`, `a182b956`, `503b7561`, `e1f39474` and `f5f02f75` are ancestors of HEAD; P47-02-T1-OK and P47-02-T2-OK both printed on the committed tip; `git diff --name-only a6baa8b7 HEAD -- src/main` prints nothing.
