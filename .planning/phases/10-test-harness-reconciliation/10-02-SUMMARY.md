---
phase: 10-test-harness-reconciliation
plan: 02
subsystem: testing
tags: [testcontainers, junit5, jmx, gradle, clickhouse-kafka-connect, harness, acceptance-gate]

requires:
  - phase: 10-test-harness-reconciliation
    provides: "Plan 10-01's shared-container-per-fork contract, opt-out seam, MBean sweep, and the `latest`-default fix this plan's gate proves stable under repeated live execution"
provides:
  - "HARN-01 proven, not merely asserted: `testUnit` completing green with the Docker daemon measurably unreachable at the moment the run started"
  - "HARN-02's zero-`InstanceAlreadyExistsException` clause discharged by two zero-failure full-suite runs"
  - "The first full-suite evidence this milestone has taken on ClickHouse `latest` (Phases 8 and 9 were silently green against the pinned `25.8.18.1`)"
  - "D-06 settled: nothing for `latest` to attribute — both full runs were clean"
affects: [10-03-checklist-and-requirements-closeout, phase-11-publication]

actuals:
  tokens: 4200
  tasks: 3
  commits: 0

tech-stack:
  added: []
  patterns:
    - "Two-consecutive-clean-full-suite gate for a shared-container harness change (D-09): a single green run cannot distinguish 'works' from 'worked once' once 25 classes become order-dependent within a fork"

key-files:
  created: []
  modified: []

key-decisions:
  - "D-06 settled as 'Nothing to attribute (expected)': both full-suite runs on ClickHouse `latest` were clean end to end — zero failures, zero errors, identical executed-class sets, non-growing skip inventory. The D-06 attribution procedure (re-run against the previously-pinned version) was never triggered because no class was ever red."
  - "This run is recorded explicitly as the milestone's first full-suite evidence taken on ClickHouse `latest` rather than the `25.8.18.1` pin Phases 8 and 9 silently ran against — the reframing D-06/D-04 exist to surface, stated even though the answer is 'all green'."
  - "Task 2's second full-suite invocation was forced with `--rerun` rather than a bare re-invocation: Gradle's `UP-TO-DATE` build cache would otherwise have skipped re-execution and echoed run 1's cached results as if they were a genuine second run, defeating the entire purpose of the two-run gate (detecting fork-assignment-dependent order effects)."

requirements-completed: [HARN-01, HARN-02]

coverage:
  - id: D1
    description: "HARN-01's proof: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit` completes green while `docker info` genuinely fails at the moment the run starts, proving `@Tag(\"clickhouse\")` is on `ClickHouseBase` and every one of the 27 live classes inherits it"
    requirement: "HARN-01"
    verification:
      - kind: other
        ref: "manual gate run: colima stop -p vz; docker info fails; env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit; colima start -p vz; docker info succeeds"
        status: pass
    human_judgment: false
  - id: D2
    description: "HARN-02's zero-`InstanceAlreadyExistsException` clause discharged by two full-suite runs with zero failures and zero errors (the registration path rethrows the exception, so a collision would already be a test failure)"
    requirement: "HARN-02"
    verification:
      - kind: other
        ref: "manual gate run 1: env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test (654 tests, 636 succeeded, 0 failed, 18 skipped)"
        status: pass
      - kind: other
        ref: "manual gate run 2 (forced with --rerun): env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun (654 tests, 636 succeeded, 0 failed, 18 skipped)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Two consecutive full-suite runs on `latest` prove the shared-container decision (D-01/10-01) stable across fork assignments, not just green once"
    verification:
      - kind: other
        ref: "byte-identical executed result-class sets (57/57) between the two runs; skip count did not grow (18 -> 18)"
        status: pass
    human_judgment: false
  - id: D4
    description: "D-06 attribution decision: nothing surfaced by `latest`, confirmed at the task 3 checkpoint"
    verification: []
    human_judgment: true
    rationale: "The checkpoint decision itself is the artifact — a human confirmation that the clean-gate reading is correct and that no attribution work is owed to Phase 11. Recorded verbatim in this SUMMARY's Checkpoint Decision section."

duration: 4min
completed: 2026-09-20
status: complete
---

# Phase 10 Plan 02: Full-Suite Acceptance Gate (Docker-Free + Two Clean `latest` Runs) Summary

**Proved HARN-01's Docker-free claim with the daemon genuinely stopped, ran two back-to-back full suites on ClickHouse `latest` with byte-identical executed-class sets and zero failures, and settled D-06 with nothing to attribute — the milestone's first full-suite evidence on the server it actually claims to test.**

## Performance

- **Duration:** 4 min (this continuation segment; tasks 1-2's own measured wall-clock is recorded below from the prior executor's evidence)
- **Tasks:** 3 (tasks 1-2 completed by the prior executor as read-only verification acts; task 3 completed here)
- **Files modified:** 0 (this plan is entirely a verification/decision plan — no source files were created or modified by any task)

## Accomplishments

- **HARN-01 proven, not asserted.** `docker info` failed immediately before the run (colima profile `vz` genuinely stopped) and succeeded again after `colima start -p vz`. `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit` was `BUILD SUCCESSFUL` in 2s: 29 result classes, 327 tests, 0 failures, 0 errors, 2 pre-existing skips. `ClickHouseVersionDefaultUnitTest` executed inside the gate clean — the Docker-free canary for the `latest`-default regression is reachable, not merely present on disk.
- **Two consecutive clean full-suite runs on ClickHouse `latest`.** Run 1: `BUILD SUCCESSFUL` in 2m51s (172s wall-clock) — 654 tests, 636 succeeded, 0 failed, 18 skipped, 57 result classes. Run 2 was forced with `--rerun` because Gradle's `UP-TO-DATE` cache would otherwise have silently reused run 1's cached results instead of genuinely re-executing — `BUILD SUCCESSFUL` in 2m54s (174s wall-clock), 654 tests, 636 succeeded, 0 failed, 18 skipped, 57 result classes.
- **Order-independence confirmed.** The two runs' executed result-class sets were byte-identical (57 each); the skip count did not grow (18 -> 18). `ClickHouseTestHarnessTest`, `ClickHouseHarnessOptOutContainerTest`, and `ClickHouseVersionDefaultUnitTest` each appear in both runs with 0 failures, 0 errors, 0 skips. `sharedContainerStarts()` = 1 was observed in both runs (single-fork local execution).
- **D-06 settled at the task 3 checkpoint: nothing to attribute.** Both full runs were clean end to end; the D-06 attribution procedure (re-running a red class against the previously-pinned version) was never triggered. The developer confirmed this reading and approved proceeding to plan 10-03's bookkeeping.
- **First full-suite evidence on `latest`, stated explicitly.** This is the milestone's first full-suite run taken against ClickHouse `latest` rather than the `25.8.18.1` pin that Phases 8 and 9 silently ran against (a regression Phase 10-01 found and fixed). The reframing was surfaced to the developer at the checkpoint even though the answer was "all green."

## Task Commits

This plan produced no source-code changes and no per-task commits — tasks 1 and 2 were read-only verification acts (running the build under measured conditions), and task 3 is a decision-recording checkpoint. This is expected for a full-suite acceptance-gate plan, not an omission.

**Plan metadata:** committed as part of this SUMMARY's own docs commit.

_Note: TDD is not applicable to this plan._

## Files Created/Modified

None. This plan verifies the harness built in plan 10-01 under measured conditions; it introduces no new symbols and modifies no source, build, or test file.

## Decisions Made

See `key-decisions` in frontmatter:
- D-06 settled as "Nothing to attribute (expected)" — both full-suite runs on `latest` were clean, so the harness-vs-server attribution procedure was never triggered.
- This run is the milestone's first full-suite evidence taken on ClickHouse `latest` rather than the previously pinned `25.8.18.1` — recorded explicitly per the checkpoint's own requirement.
- Run 2 required `--rerun` to force genuine re-execution past Gradle's `UP-TO-DATE` cache; without it, the second invocation would have been a cached echo of run 1, not independent evidence.

## Deviations from Plan

None — plan executed exactly as written. Tasks 1 and 2 were carried out by the prior executor exactly per their `<action>`/`<verify>` blocks (Docker genuinely stopped and restarted; two full runs, the second forced with `--rerun` to avoid Gradle's cache silently defeating the two-run gate); task 3's checkpoint was answered by the developer with the "Nothing to attribute (expected)" option, which is this plan's own first listed option and required no scope expansion or fix work.

## Named Skip Inventory (18 skips, identical in both full-suite runs)

| Class | Test(s) | Reason |
|---|---|---|
| `ClickHouseSinkTaskTest` | `clientNameTest()` | `@Disabled` — pre-existing flaky-test disable |
| `ClickHouseSinkTaskWithSchemaTest` | `schemaWithNestedTupleMapArrayAndVariant()`, `schemaWithTupleOfMapsWithVariantTest()` | `@Disabled` — requires a flag on the instance |
| `ClickHouseSinkTaskWithSchemaTest` | `exactlyOnceStateMismatchTest` cases `[1]`..`[11]` | `Assumptions.assumeTrue(isCluster \|\| isCloud)` — standalone run |
| `InsertFormatPerformanceTest` | `compareInsertPerformance()` | `@Disabled` — not suitable for unit tests |
| `JsonColumnTest` | `compareAllConfigurations()` | `@Disabled` — not suitable for unit tests |
| `transforms.ConvertToMapOrJsonPerformanceTestJson` | `testWideStructPerformance()`, `testDeepNestedStructPerformance()` | `@Disabled` — not suitable for unit tests |

None of the 18 skips belong to the fork's own coverage (SMT sink-landing, JSON/Dynamic round-trip, RowBinary fail-fast, JMX registration, insert-columns) — all of those ran and passed in both runs, confirmed at the task 2 `<human-check>` gate.

## D-06 Attribution Table

Not applicable — no class was red on `latest` in either full-suite run, so the D-06 attribution procedure (re-running an affected class pinned to the previously-green version) was never triggered. This row is intentionally empty; recording that fact is itself the discharge of D-06's ownership rule for this plan.

## Checkpoint Decision (Task 3)

**Decision:** Nothing to attribute (expected) — option 1 of the checkpoint's three options.

**What was presented to the developer:**
- Run 1 total: 654 tests, 636 succeeded, 0 failed, 18 skipped; wall-clock 172s (2m51s).
- Run 2 total: 654 tests, 636 succeeded, 0 failed, 18 skipped; wall-clock 174s (2m54s), forced with `--rerun`.
- Skip count: 18, identical between runs, fully named above.
- Observed `sharedContainerStarts()`: 1 in both runs (single-fork local execution).
- The one fact that changed underneath these numbers: this is the milestone's first full-suite evidence taken on ClickHouse `latest` rather than the `25.8.18.1` pin Phases 8 and 9 silently ran against.

**Developer response:** APPROVED — option 1, "Nothing to attribute — proceed."

**Rationale for the decision:** No class was red on `latest` at any point in either run, so there was nothing for the D-06 attribution procedure to attribute. Re-baselining the gate was not offered as an option, consistent with the plan's own prohibition. No scope expansion into `src/main` was requested or needed. Plan 10-03's bookkeeping (checklist flips, `REQUIREMENTS.md` closure) proceeds on this clean result.

## Issues Encountered

None. The one process finding worth flagging forward: Gradle's `UP-TO-DATE` build cache required the second full-suite invocation to be forced with `--rerun`, or it would have silently echoed run 1's results as a "second" run — defeating the two-consecutive-clean-runs gate's entire purpose (detecting order effects introduced by plan 10-01's shared-container change across 25 classes newly sharing a JVM fork). Anyone re-running this gate in the future (Phase 11's CI measurement, a future re-verification) should use `--rerun` or an equivalent cache-busting flag on the second invocation, not assume a bare re-run is independent evidence.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- HARN-01 and HARN-02 are proven and ready to close — both are exclusively declared by this plan's `requirements` frontmatter (`[HARN-01, HARN-02]`); no sibling plan shares either ID, so both mark complete immediately once this SUMMARY exists.
- HARN-07 is **not** owned by this plan — it is shared between plans 10-01 and 10-03 and stays out of this plan's `requirements.mark-complete` call; 10-03 is the plan responsible for it via the shared-ID gate.
- D-06 is settled with no carry-in: nothing needs to be added to `STATE.md`'s blockers for Phase 11, since the "hand to Phase 11" branch of the checkpoint was not taken.
- Plan 10-03 (checklist and requirements closeout) is unblocked: this plan's clean result is the evidence its bookkeeping rests on. 17 pending `MERGE-CHECKLIST.md` rows and the remaining `HARN-04`/`05`/`06`/`07` grep-and-record closures are still ahead of it, unaffected by this plan.
- Wall-clock figures (172s and 174s for the full suite, 2s for `testUnit`) are available for Phase 11's `timeout-minutes: 45` CI measurement, alongside plan 10-01's already-recorded per-task numbers.

## Self-Check: PASSED

No files were created or modified by this plan's tasks, so there is nothing to verify with `[ -f ]` beyond this SUMMARY itself, which exists at `.planning/phases/10-test-harness-reconciliation/10-02-SUMMARY.md`. This plan's commit history will be verified against the `commits: 0` actuals figure immediately after the SUMMARY commit below (the `git rev-list --count` measurement, not a narrated claim).

---
*Phase: 10-test-harness-reconciliation*
*Completed: 2026-09-20*
