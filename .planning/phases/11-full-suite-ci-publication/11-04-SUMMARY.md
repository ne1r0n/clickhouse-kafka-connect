---
phase: 11-full-suite-ci-publication
plan: 04
subsystem: testing
tags: [gradle, junit5, testcontainers, clickhouse-latest, client-v2, acceptance-gate, skip-inventory, testUnit]

requires:
  - phase: 11-full-suite-ci-publication
    provides: "11-01 debt closure, 11-02 CI pin removal, 11-03 FORK-DELTA, and 11-08 LZ4 network-compression fix (D-17) - the tree this gate measures"
provides:
  - "SUITE-01 evidence: one cache-busted full run on ClickHouse latest (26.9.12.8) x client V2, 663 tests, 0 failed, 0 errors, 18 skipped"
  - "Skip inventory re-derived from this run's XML: set-identical to Phase 10's named 18"
  - "SUITE-02 evidence: four fork SMT classes plus SmtSinkLandingTest executed clean and unskipped, read from this run's XML"
  - "Docker-free testUnit contract re-proved with the daemon confirmed down: 331 tests, 0 failed"
  - "Measured fork topology of the local gate: 6 parallel forks, not 1 (finding for plan 11-05)"
affects: [11-05, 11-06, 11-07]

actuals:
  tokens: 0
  tasks: 3
  commits: 0

plan_head_before: 382c9e7162b80be5c89b1586172fd061fa50f088
plan_head_after: 382c9e7162b80be5c89b1586172fd061fa50f088
commits: 0

tech-stack:
  added: []
  patterns:
    - "Skip inventory compared as sorted sets of (class, test-method) pairs derived from result XML, never by count"
    - "testUnit proof taken only after docker info is observed to fail"

key-files:
  created: []
  modified: []

key-decisions:
  - "No file changed and no commit made by this plan; it is an evidence plan, so commits: 0 is the measured and legitimate count."
  - "Class-set comparison against Phase 10's 57 rests on git (only two test classes added since c48754be) because Phase 10 stored the count, not the list; stated as inference, not as a set comparison."
  - "The concurrency backstop truth is reported as refuted for the local half: the local gate ran 6 forks, so it does not share a topology with a 2-core CI runner."

requirements-completed: [SUITE-01, SUITE-02]

coverage:
  - id: D1
    description: "Published tree is green on ClickHouse latest (26.9.12.8) with client V2 on one cache-busted full run: 663 tests, 645 succeeded, 0 failed, 0 errors, 18 skipped"
    requirement: SUITE-01
    verification:
      - kind: integration
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun -> BUILD SUCCESSFUL in 3m 7s; GATE_TOTALS_OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "Every skip accounted for by identity: re-derived 18 (class, method) pairs are set-identical to Phase 10's named 18"
    requirement: SUITE-01
    verification:
      - kind: other
        ref: "awk over build/test-results/test/TEST-*.xml -> /tmp/p11-skips-now.txt, 18 pairs, diffed against 10-02-SUMMARY Named Skip Inventory"
        status: pass
    human_judgment: false
  - id: D3
    description: "SUITE-02: TableOperationFilterTest(26), HeadersToValueTest(25), HeaderToValueTest(24), ConvertToMapOrJsonTest(37), SmtSinkLandingTest(5) executed with 0 failures, 0 errors, 0 skips; ConvertToMapOrJsonPerformanceTestJson executed with exactly 2 known skips"
    requirement: SUITE-02
    verification:
      - kind: integration
        ref: "SMT_CORRECTNESS_COVERAGE_CLEAN and PERF_CLASS_SKIPS_AS_NAMED from this run's result XML"
        status: pass
    human_judgment: false
  - id: D4
    description: "Docker-free testUnit contract holds with Docker confirmed down: 331 tests, 0 failed, 0 errors, 2 skipped"
    verification:
      - kind: other
        ref: "docker info rc=1 (socket absent) then env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun -> TESTUNIT_CONTRACT_HOLDS"
        status: pass
    human_judgment: false
  - id: D5
    description: "CI executes at the same single-fork topology as the local gate"
    verification: []
    human_judgment: true
    rationale: "Refuted for the local half: this machine has 12 cores so maxParallelForks resolved to 6 and 6 forks ran. A 2-core runner resolves to 1. Whether a single-fork CI run is green is only answerable by plan 11-05's CI run."

duration: 6min
completed: 2026-10-07
status: complete
---

# Phase 11 Plan 04: Local Acceptance Gate (Cache-Busted Full Run + Docker-Free testUnit) Summary

**One `--rerun` full suite on ClickHouse `latest` (26.9.12.8) x client V2 passes at 663 tests, 0 failed, 0 errors, 18 skipped with the skip set identical to Phase 10's named 18, all five SMT classes clean and unskipped, and `testUnit` green at 331 tests with the Docker daemon confirmed down; the run was a 6-fork run, not the single-fork shape the plan assumed.**

## Performance

- **Duration:** ~6 min (full run 187 s wall-clock, `testUnit` 4 s)
- **Started:** 2026-10-07T11:48:13Z
- **Completed:** 2026-10-07T11:54Z
- **Tasks:** 3
- **Files modified:** 0

## Preconditions confirmed before the run

- Commits from 11-01 (`b62293aa`), 11-02 (`09cae191`), 11-03 (`29c93369`) and 11-08 (`c88416fa`) are all ancestors of HEAD `382c9e71`.
- `git status --porcelain` held only the four orchestrator-owned paths (`.planning/STATE.md`, `.planning/state.json`, staged `.planning/milestone.lock`, untracked `.planning/.STATE.md.swp`); no source, test or build file was dirty. This is not the strictly empty output the plan wrote, and the difference is the orchestrator's own pre-existing state.
- `git log c48754be..HEAD -- src/main` returns exactly one commit, `87fa1e78 fix(11-08)`: the D-17 carve-out. No other `src/main` change exists in Phase 11.
- `docker info` succeeded; colima profile `vz` running.

## Task 1: Full run (SUITE-01)

**Exact command:** `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun`

Gradle output confirmed `> Task :test` executed (not `UP-TO-DATE`); all 59 result XML files were written during this run's window.

| Measure | Value |
|---|---|
| Tests executed | 663 |
| Succeeded | 645 |
| Failed | 0 |
| Errored | 0 |
| Skipped | 18 |
| Wall-clock | 187 s (Gradle `BUILD SUCCESSFUL in 3m 7s`) |
| Executed result classes | 59 |
| Summary-listener line | `Tests summary: 663 tests, 645 succeeded, 0 failed, 18 skipped` |

The XML totals (663 / 0 / 0 / 18) and the listener line agree. Phase 10 measured 172 s and 174 s; 187 s is inside the band's neighbourhood and not a finding.

**Why 663, not 654.** Phase 10 ran 654. The +9 is the nine new `NetworkCompressionPin*` methods added by plan 11-08 (`NetworkCompressionPinUnitTest` 4, `NetworkCompressionPinLiveTest` 5). It is new tests, not a skip-set change, and it accounts for the full difference: 636 succeeded in Phase 10, 645 now.

**Executed-result-class set: 59 against Phase 10's 57.** The two additional classes are `com.clickhouse.kafka.connect.sink.db.helper.NetworkCompressionPinLiveTest` and `...NetworkCompressionPinUnitTest`. `git diff --name-status c48754be..HEAD -- src/test src/testFixtures src/integrationTest` shows exactly those two files added, one `src/testFixtures` file modified (`ClickHouseTestHelpers.java`, from 11-02), and nothing removed or renamed. Phase 10 recorded the class count (57) but did not store the list, so this is an inference from git rather than a sorted-set comparison against a stored list. No class that ran in Phase 10 can have dropped out unless a test file was deleted, and none was.

### Skip diff: identical set

Re-derived from this run's XML as (class, method) pairs, 18 pairs, sorted:

| Class | Method(s) | Phase 10's named reason |
|---|---|---|
| `ClickHouseSinkTaskTest` | `clientNameTest()` | `@Disabled`, pre-existing flaky-test disable |
| `ClickHouseSinkTaskWithSchemaTest` | `schemaWithNestedTupleMapArrayAndVariant()`, `schemaWithTupleOfMapsWithVariantTest()` | `@Disabled`, requires a flag on the instance |
| `ClickHouseSinkTaskWithSchemaTest` | `exactlyOnceStateMismatchTest` cases `[1]` to `[11]` | `Assumptions.assumeTrue(isCluster \|\| isCloud)`, standalone run |
| `InsertFormatPerformanceTest` | `compareInsertPerformance()` | `@Disabled`, not suitable for unit tests |
| `JsonColumnTest` | `compareAllConfigurations()` | `@Disabled`, not suitable for unit tests |
| `transforms.ConvertToMapOrJsonPerformanceTestJson` | `testWideStructPerformance()`, `testDeepNestedStructPerformance()` | `@Disabled`, not suitable for unit tests |

Result: **identical set**. 1 + 2 + 11 + 1 + 1 + 2 = 18 pairs on both sides, membership compared pair by pair, not by count. No new skip, no disappearance, no substitution. The parameterised cases appear in the XML under their display names (`[1] 11, 7` through `[11] 229, 221`), which are the same eleven cases Phase 10 named as `[1]..[11]`.

## Task 2: SUITE-02 from this run's own result XML

All counts below are from `build/test-results/test/TEST-*.xml` written by Task 1's run, not from Phase 10's summary.

| Class | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| `transforms.TableOperationFilterTest` | 26 | 0 | 0 | 0 |
| `transforms.HeadersToValueTest` | 25 | 0 | 0 | 0 |
| `transforms.HeaderToValueTest` | 24 | 0 | 0 | 0 |
| `transforms.ConvertToMapOrJsonTest` | 37 | 0 | 0 | 0 |
| `com.clickhouse.kafka.connect.sink.SmtSinkLandingTest` | 5 | 0 | 0 | 0 |
| `transforms.ConvertToMapOrJsonPerformanceTestJson` | 2 | 0 | 0 | 2 (the two named `@Disabled` performance methods) |

Exactly five correctness classes were seen; `SMT_CORRECTNESS_COVERAGE_CLEAN` and `PERF_CLASS_SKIPS_AS_NAMED` both printed.

Other own-coverage classes confirmed present in the executed set, all with 0 failures and 0 errors:

| Area | Class (tests / skipped) |
|---|---|
| JSON/Dynamic round-trip | `JsonDynamicRoundTripTest` (6/0), `JsonDynamicTypeMappingTest` (5/0), `JsonSerializationEquivalenceTest` (16/0) |
| RowBinary fail-fast gate | `RowBinaryFailFastLiveTest` (2/0), `RowBinaryUnsupportedTypeGateTest` (6/0) |
| JMX registration | `MBeanRegistrationLiveTest` (2/0), `MBeanNameUnitTest` (11/0) |
| Insert-columns | `JsonInsertColumnsLiveTest` (5/0), `JsonInsertColumnsUnitTest` (9/0) |
| Metadata-free JSON bypass canary | `MetadataFreeJsonInsertUnitTest` (13/0), `ExplicitJsonInsertPathLiveTest` (6/0) |
| Harness and version-default canary | `ClickHouseTestHarnessTest` (7/0), `ClickHouseHarnessOptOutContainerTest` (1/0), `ClickHouseVersionDefaultUnitTest` (1/0) |
| 11-08 compression pin | `NetworkCompressionPinUnitTest` (4/0), `NetworkCompressionPinLiveTest` (5/0) |

The only skip in this list is `JsonColumnTest` (1 skip, `compareAllConfigurations()`), which is one of the named 18 and is a `@Disabled` comparison benchmark, not the fork's round-trip coverage (`JsonDynamicRoundTripTest` ran 6 of 6).

## Task 3: Docker-free testUnit

- `colima stop --profile vz` completed; `docker info` then failed: `failed to connect to the docker API at unix:///Users/alexandr.chashnikov/.colima/vz/docker.sock ... connect: no such file or directory`, return code 1. `DOCKER_CONFIRMED_DOWN` printed. This is the evidence the daemon was genuinely stopped, observed rather than assumed.
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun` -> `BUILD SUCCESSFUL in 3s`: **331 tests, 329 succeeded, 0 failed, 0 errors, 2 skipped**, 30 result classes, 4 s wall-clock. `TESTUNIT_CONTRACT_HOLDS` printed.
- 331 against Phase 10's 327: the +4 is `NetworkCompressionPinUnitTest`'s four methods (11-08 reported the same figure). Two skips are the pre-existing ones, unchanged.
- `colima start --profile vz` run afterwards; `docker info` succeeds again. The environment is as it was found.

## Finding: the local gate ran 6 forks, not 1 (concurrency backstop)

The plan's backstop truth says CI "executes at the same single-fork topology as this local gate - `maxParallelForks` resolves to 1 on a 2-core runner exactly as it did here". The local half did not measure that way.

- This machine reports `hw.ncpu = 12`. `build.gradle.kts:194` sets `maxParallelForks = availableProcessors() / 2`, which is **6** here.
- The run's own output corroborates it: 6 `testcontainers/ryuk` starts (one per fork JVM), 9 `clickhouse/clickhouse-server:latest` container creations plus 2 toxiproxy, and a measured maximum of 6 test classes executing concurrently (derived from the XML `timestamp` and `time` attributes).
- A 2-core CI runner resolves `maxParallelForks` to 1.

So the local green is a 6-fork result and CI will be the first execution of this tree at a single fork, not a repeat of a measured shape. This does not make this plan fail: the gate's pass condition is zero failures, zero errors and a non-growing skip set, all of which hold. But the topology-equivalence claim is refuted on its local side, and Phase 10's "single-fork local execution" (`sharedContainerStarts() = 1`) was a per-fork figure that does not establish a single fork on a machine with this core count. Plan 11-05's CI run is the only evidence for the single-fork shape. If a single-fork run is wanted before pushing, `./gradlew test --rerun -Dorg.gradle.workers.max=1` would not change `maxParallelForks`; it would need `CLICKHOUSE_CLUSTER_NAME` set (which switches to cluster-mode tests) or a local override of the build file, neither of which this plan was permitted to do.

## Task Commits

None. This plan produced evidence only; no file was modified and no commit was made by Tasks 1 to 3. `git rev-list --count 382c9e71..HEAD` returned 0 at SUMMARY-write time, and there are no code changes, so 0 is the legitimate count. The plan's own SUMMARY commit follows this file.

## Files Created/Modified

None. Gradle result XML under `build/` is gitignored; two scratch files under `/tmp/p11-04/` and `/tmp/p11-skips-now.txt` hold the run logs and the re-derived skip pairs.

## Decisions Made

- Evidence plan: nothing committed, nothing re-baselined, no test touched (all prohibitions held).
- Class-set comparison reported as an inference from git, because Phase 10 stored only the count.
- Concurrency backstop reported as refuted locally rather than absorbed into "same shape as Phase 10".

## Deviations from Plan

None - plan executed exactly as written. The `git status --porcelain` clean-tree precondition was read as "no uncommitted source, test or build change" because the orchestrator's pre-existing planning-state files were explicitly carved out in the dispatch notes.

## Issues Encountered

- The plan's concurrency premise did not hold locally (see Finding above); recorded, not worked around.
- The `testUnit` precondition required stopping colima, which also stops the user's Docker for the 4-second run; it was restarted immediately and confirmed up.
- The Gradle in use is 9.6.1 (the build log's configuration-cache hint), not the 7.4.2 `.claude/CLAUDE.md` lists; irrelevant to the result.

## Known Stubs

None.

## Threat Flags

None - no source, dependency or workflow change.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 11-05 can take D-03's first two evidence items from this file: run totals and skip count (663 / 0 / 0 / 18; identical set), and the Docker-free `testUnit` result (331 / 0, daemon confirmed down).
- For plan 11-05's D-10 reading: the local full run took 187 s at 6 forks. A single-fork 2-core runner will be slower; the 22.5-minute threshold is the check.
- For plan 11-05's concurrency backstop: the CI run is the first execution at the single-fork shape, and a CI-only failure should be attributed with D-11 and the 6-fork finding in hand.
- Latest image was 26.9.12.8; the 11-08 fix is exercised by 9 passing `NetworkCompressionPin*` tests inside this run.

## Self-Check: PASSED

- `/tmp/p11-skips-now.txt` exists with 18 lines; `build/test-results/test` holds 59 XML files from this run; `build/test-results/testUnit` holds 30.
- `git rev-list --count 382c9e71..HEAD` = 0, consistent with `commits: 0` and no code changes.
- `git merge-base --is-ancestor` for 87fa1e78 and the 11-01/02/03/08 commits against HEAD: confirmed by the `git log` listing at start.
- Task 1, 2 and 3 verify commands each printed their pass marker (`GATE_TOTALS_OK`, `SMT_CORRECTNESS_COVERAGE_CLEAN`, `PERF_CLASS_SKIPS_AS_NAMED`, `DOCKER_CONFIRMED_DOWN`, `TESTUNIT_CONTRACT_HOLDS`).

---
*Phase: 11-full-suite-ci-publication*
*Completed: 2026-10-07*
