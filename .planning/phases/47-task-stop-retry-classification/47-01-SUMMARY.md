---
phase: 47-task-stop-retry-classification
plan: "01"
subsystem: testing
tags: [sink-task-lifecycle, stop, try-finally, delivery-strategy, reflection-seam, red-first, anchors, fork-delta]
status: complete

requires:
  - phase: 46-insert-timeout-limits
    provides: the sync/v1.7.0 tip, fork-anchors.txt (216 rows), the tail gate scripts, the testUnit 2181 and full-suite 2784 expectations
provides:
  - 47-COMMAND-EVIDENCE.md with the header, the E-01..E-11 index and entries E-01..E-04
  - a green, attributable baseline on the untouched base B0 (anchors, gate self-test, tail gate, testUnit with Docker unreachable, one full suite on latest x V2)
  - ClickHouseSinkTaskStopUnitTest, six Docker-free cases, no Mockito, red on the unchanged tree before the fix
  - ClickHouseSinkTask.stop() with the delivery strategy's stop() in a try and the proxy task's stop() in its finally (the only src/main change, 8 insertions and 5 deletions)
  - two LIF-01 anchor rows (216 to 218), 47-lif01-expected-added.tsv, FORK-DELTA item 13 corrected
affects: [47-02, 47-03, 47-04, 49]

actuals:
  tokens: 13346
  tasks: 2
  commits: 5
plan_head_before: 427f30b4c4dffd56841f5e5eafe13a55ac4caca8
plan_head_after: a3f1f29882c1f2fb9ffa446a39cbd8717e3cfe8c

tech-stack:
  added: []
  patterns: [reflection on the private proxySinkTask and deliveryStrategy fields with a stub of the package-private DeliveryStrategy, per-case MBean cleanup in a try-with-resources harness, expected-added list committed before the fix]

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java
    - .planning/phases/47-task-stop-retry-classification/47-COMMAND-EVIDENCE.md
    - .planning/phases/47-task-stop-retry-classification/47-lif01-expected-added.tsv
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md

key-decisions:
  - "D-1 applied literally: a plain try/finally with no catch, so the strategy's own exception still reaches the caller; when both stops throw, the proxy's exception is thrown and the strategy's is dropped with 0 suppressed (measured, not pinned)"
  - "The pin asserts that both stops ran and that a RuntimeException surfaced, not which one, so a later move to the keepFirst idiom needs no assertion edit"
  - "close(partitions) has the same no-finally shape; read from the code, recorded in FORK-DELTA item 13, not changed (D-1)"

requirements-completed: [LIF-01]

coverage:
  - id: D1
    description: "Attribution baseline on the untouched base B0: anchors rows=216 bad=0, gate self-test clean, unedited tail gate PASS with anchor_rows=216, cache-busted testUnit with Docker unreachable 2181 tests green (2 skips, eight classification and stop pin classes counted), one full suite 2784 tests green on ClickHouse 26.10.3.7 x client V2 with the 18 named skips, digest recorded, per-testcase dumps kept"
    requirement: LIF-01
    verification:
      - kind: other
        ref: "P47-01-T1-OK (Task 1 automated verify; E-01)"
        status: pass
    human_judgment: false
  - id: D2
    description: "ClickHouseSinkTaskStopUnitTest was red on the unchanged tree for the stated reason (exactly the two named cases, AssertionFailedError, writer stop expected 1 but was 0, four characterization cases green) and recorded, with the 6-row expected-added list, before any src/main edit"
    requirement: LIF-01
    verification:
      - kind: unit
        ref: "ClickHouseSinkTaskStopUnitTest#strategyStopFailureStillStopsTheProxyTask and #bothStopsRunWhenTheStrategyAndTheProxyTaskThrow (E-02, red run on B0)"
        status: pass
    human_judgment: false
  - id: D3
    description: "ClickHouseSinkTask.stop() stops the proxy task (writer, task MBean, state provider) even when the delivery strategy's stop() throws, and with each of the three shipped strategies stop() discards buffered records without inserting them and reaches the proxy stop; the fix is its own commit holding only ClickHouseSinkTask.java (8/5), the pin and two LIF-01 anchor rows (mirror rows=218 bad=0)"
    requirement: LIF-01
    verification:
      - kind: unit
        ref: "ClickHouseSinkTaskStopUnitTest (6 cases green, test file byte-identical to the red run; E-03)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The fix's own gate (testUnit 2187 with Docker unreachable, per-testcase delta removed=0 added=6 against the base, eight pin classes at baseline counts), the both-throw probe at the fix commit, and FORK-DELTA item 13 byte-equal to the planned text"
    requirement: LIF-01
    verification:
      - kind: other
        ref: "P47-01-T2-OK (Task 2 automated verify; E-04)"
        status: pass
    human_judgment: false
---

# Phase 47 Plan 01: Task stop LIF-01 Summary

**ClickHouseSinkTask.stop() now reaches the proxy task's stop in a `finally`, so a delivery strategy whose `stop()` throws no longer leaves the writer, the task MBean and the state provider allocated; pinned Docker-free and red-first.**

## Performance

- **Duration:** about 7 hours of wall clock, most of it an interruption by an API login expiry between the red run and the first commit; the self-test (about 63 min) and the base tail gate ran in the background during the baselines. No plan start time was captured at launch, so this figure is read from scratch-file timestamps.
- **Tasks:** 2 (tracer plus auto), 6 files changed (3 created, 3 modified), 5 commits before this SUMMARY.

## Accomplishments

- Base B0 = `427f30b4c4dffd56841f5e5eafe13a55ac4caca8` measured green and recorded as E-01, so every later red and every added row is attributable to this phase.
- The pin `ClickHouseSinkTaskStopUnitTest` (3 plain cases plus one 3-row enum-parameterized case) failed on the unchanged tree for the stated reason and only that reason, and its red run and the 6-row expected-added list were committed before the fix (E-02).
- The fix F1 = `796c75fa3a0a8a7bb8d740bcffd3b2a3858b9cbe`: the `try`/`finally` in `stop()`, numstat 8/5, the file byte-identical to B0 outside `stop()`, the four comment lines kept, no catch.
- FORK-DELTA item 13 (commit `e1b03148940c85683c9b0376aa07b1b38d58eab6`) states the fix, the measured both-throw limit and the `close(partitions)` residual, byte-equal to the planned text in `47-doc-edits.json`.

## Baseline numbers later plans compute from

| Measure | Value |
|---|---|
| B0 (phase base) | `427f30b4c4dffd56841f5e5eafe13a55ac4caca8` |
| F1 (fix commit) | `796c75fa3a0a8a7bb8d740bcffd3b2a3858b9cbe` |
| Baseline testUnit, Docker unreachable | 2181 tests, 0 failures, 0 errors, 2 skipped, 103 files |
| testUnit on the fix tree | 2187 tests (2181 + 6), 0 failures, 0 errors, 2 skipped, delta removed=0 added=6 match=yes |
| Baseline full suite, ClickHouse latest x V2 | 2784 tests, 0 failures, 0 errors, 18 skipped (`skips`: named=7 state_mismatch_cases=11 unexpected=0), 152 files, 3 min 17 s |
| ClickHouse | `26.10.3.7`, `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` |
| Anchors mirror | 216 rows at B0, 218 from F1 on |
| Base tail gate | `anchor_commits=91 anchor_rows=216 marker_commits=109 anchor_exempt=0 result=PASS` |
| Gate self-test | `selftest: no failures` (about 63 min) |
| Per-class counts (testUnit) | WrappedDataExceptionClassificationPinUnitTest 14, RetriableExceptionPassThroughUnitTest 38, WriterFailureClassificationUnitTest 216, ProxySinkTaskStopUnitTest 4, UnvettedRetriableCodesWarnUnitTest 21, AdditionalRetriableErrorCodesPinUnitTest 19, HandleExceptionCauseCycleUnitTest 2, InsertStallBoundPinUnitTest 15 |
| Red tally (unchanged tree) | 6 tests, 2 failures (the two named cases, `AssertionFailedError`, `expected: <1> but was: <0>`), 0 errors, 4 passed |
| Green tally (fix tree) | 6 tests, 0 failures, 0 errors; neighbours (ProxySinkTaskStopUnitTest, UnvettedRetriableCodesWarnUnitTest, SourcePlanningIdGuardUnitTest) 43 tests, 0 failures |
| Both-throw probe at F1 | `thrown=IllegalArgumentException:proxy side failure suppressed=0` (the proxy's exception is thrown, the strategy's is dropped) |
| JVM crash / `-PjavaVersion=21` | none; every run used the default toolchain, no crash file, no `-PjavaVersion=21` |

## Backlog candidates (recorded, not changed)

1. A `finally` in `ClickHouseSinkTask.close(partitions)`: it calls `proxySinkTask.onPartitionRemoved` and then `deliveryStrategy.close` with the same no-`finally` shape. Out of scope under D-1; named in FORK-DELTA item 13.
2. The in-repo `keepFirst` idiom of `ProxySinkTask.stop()` applied in `ClickHouseSinkTask.stop()`: it would attach the strategy's exception as suppressed when both stops throw. The pin does not assert which exception surfaces, so the move needs no assertion edit.

## Deviations from Plan

None - plan executed exactly as written.

Process notes, not deviations: the run was interrupted once by an API login expiry after the red run and before the first commit. The coordinator's message said the self-test had been killed; it had in fact finished (exit 0, last line `selftest: no failures`) with `HEAD` unchanged, so its result was recorded without a re-run. The pin file on disk was re-verified against the red run's SHA-256 before use. Per the run's rules, `.planning/STATE.md`, `.planning/ROADMAP.md` and `.planning/REQUIREMENTS.md` were not touched or staged by this executor (the orchestrator owns them); `.planning/config.json` and `.planning/state.json` stay dirty and unstaged as before.

## Authentication Gates

None.

## Issues Encountered

None.

## Known Stubs

None. (`ThrowingStrategy` in the pin is a deliberate test stub of the package-private `DeliveryStrategy`, not a placeholder.)

## Threat Flags

None. No new endpoint, auth path or trust-boundary surface; the only production change is the `stop()` body (T-47-01 and T-47-02 mitigated as planned: scope check by token list, numstat 8/5, anchors).

## Ready for

Plan 47-02 (RTY-01 limit pin and FORK-DELTA item 17). Planned counts after 47-02: testUnit 2181 + 114 = 2295 (6 from this plan, 108 from 47-02) and full suite 2784 + 114 = 2898; measured values win.

## Self-Check: PASSED

Created files exist (the pin, the evidence file, the expected-added list); commits `a7258507`, `796c75fa3a0a8a7bb8d740bcffd3b2a3858b9cbe`, `43a21668`, `e1b03148` and `a3f1f298` are ancestors of HEAD; P47-01-T1-OK and P47-01-T2-OK both printed on the committed tip.
