---
phase: 08-orchestration-jmx-re-plumbing
plan: 02
subsystem: testing
tags: [gate, jmx, testcontainers, clickhouse, triage, jacoco]

requires:
  - phase: 08-orchestration-jmx-re-plumbing
    provides: "plan 08-01's connector=<name> JMX re-plumb (SinkTaskStatistics 2-arg ctor, ProxySinkTask 3-arg ctor, getTopicMBeanName composition) and the three UTF-8 InputStreamReader fixes in ClickHouseTestHelpers.java"
provides:
  - "Machine-checked evidence that testUnit is green with the new layering (28 classes, 317 tests, 0 failures/errors) — ROADMAP criterion 3's Docker-free half"
  - "Machine-checked evidence that the live suite proves ORCH-02/ORCH-03 for real: MBeanRegistrationLiveTest, JsonDynamicRoundTripTest, SmtSinkLandingTest and ClickHouseSinkTaskTest all report 0 failures/0 errors against a live ClickHouse container"
  - "The re-baselined D-12 pass condition satisfied: 1 failure (the pre-known TableTest.extractNullables() carry-in), 0 errors, 0 new regressions against the 7-failure Phase-7 baseline"
  - "A named cause for TableTest.extractNullables(), distinguishing it from both of the plan's candidate explanations, plus the full failure/skip inventory for Phase 9-11 to inherit"
affects: [09-metadata-free-json-path, 10-harness-and-live-suite-restoration, 11-full-suite-ci-publication]

actuals:
  tokens: 0
  tasks: 3
  commits: 0

tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified: []

key-decisions:
  - "Forced a fresh (--rerun) testUnit execution rather than trusting Gradle's UP-TO-DATE cache from plan 08-01's session, so this plan's own evidence is a genuine run, not a stale result reused across plans."
  - "Ran the full live suite exactly once, detached, per the plan's instruction not to narrow with --tests or stop at the first failure — the whole point is a complete inventory."
  - "Triaged TableTest.extractNullables() to its real mechanism (ClickHouseHelperClient.shouldSkipDescribeRow, a fork-only file, unconditionally strips ALL Nullable .null subcolumns) rather than accepting the plan's own two candidate explanations as exhaustive — Table.java itself is confirmed irrelevant (0-line diff, and its addColumn/registerValidColumn logic just counts whatever list it is handed)."

requirements-completed: [ORCH-01, ORCH-02, ORCH-03]

coverage:
  - id: D1
    description: "testUnit is green with the new layering in place — upstream's DeliveryStrategy/ChunkFlusher above ProxySinkTask and upstream's 5-arg Processing construction below it both survive plan 08-01's constructor change"
    requirement: ORCH-01
    verification:
      - kind: unit
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun (BUILD SUCCESSFUL, 28 result files, 317 tests, 0 failures, 0 errors)"
        status: pass
    human_judgment: false
  - id: D2
    description: "A real registered MBean carries connector=<name> at both the task and topic level — proven live, not only in MBeanNameUnitTest's formatter unit test"
    requirement: ORCH-02
    verification:
      - kind: integration
        ref: "build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.MBeanRegistrationLiveTest.xml (2 tests, 0 failures, 0 errors)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Per-topic MBean registration and the four-tasks-two-connectors selective-unregistration path both exercise the connector=<name> segment live"
    requirement: ORCH-03
    verification:
      - kind: integration
        ref: "build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.MBeanRegistrationLiveTest.xml#fourTasksAcrossTwoConnectorsRegisterDistinctMBeansAndSelectiveStopUnregistersOne"
        status: pass
    human_judgment: false
  - id: D4
    description: "The re-baselined D-12 pass condition holds: at most 1 failure, 0 errors, and every remaining failure/skip named with its owning phase"
    verification:
      - kind: other
        ref: "build/test-results/test aggregate: 54 classes, 636 tests, 1 failure (TableTest.extractNullables), 0 errors, 35 skipped — see Failure Inventory and Skip Inventory below"
        status: pass
    human_judgment: true
    rationale: "Whether a re-baselined pass condition (rather than zero failures) counts as the phase's gate being satisfied is itself a judgment the plan asks a human/reviewer to weigh, even though every individual number is machine-verified."
  - id: D5
    description: "TableTest.extractNullables()'s failure is triaged with a named cause and owning phase, not fixed"
    verification:
      - kind: other
        ref: "git diff v1.6.0 HEAD -- Table.java and TableTest.java both empty (read-only triage honored); see TableTest.extractNullables() Triage section below"
        status: pass
    human_judgment: true
    rationale: "The triage's adequacy (does it actually name a cause rather than restate the symptom) is exactly the human-check the plan's own <verify> block specifies."

duration: ~9min
completed: 2026-09-19
status: complete
---

# Phase 8 Plan 2: Acceptance Gate — testUnit, Live Suite, and the extractNullables Triage Summary

**Closed Phase 8 on live evidence: testUnit green (317 tests, 0 failures), the full live suite green modulo exactly the one pre-known carry-in (636 tests, 1 failure, 0 errors, 35 explained skips), and `TableTest.extractNullables()` traced to `ClickHouseHelperClient.shouldSkipDescribeRow` unconditionally stripping Nullable `.null` subcolumns — a fork behavior already pinned by `DESC-01`, not a Table.java defect.**

## Performance

- **Duration:** ~9 min
- **Started:** 2026-09-19T12:53:00Z (approximate)
- **Completed:** 2026-09-19T13:02:00Z
- **Tasks:** 3
- **Files modified:** 0 (verification/investigation only, per this plan's own `files_modified: []`)

## Accomplishments

- **Task 1 — Docker-free gate:** `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun` (forced fresh, not reused from plan 08-01's cache) is `BUILD SUCCESSFUL`. `build/test-results/testUnit/` holds 28 `TEST-*.xml` files summing to 317 tests, 0 failures, 0 errors (Gradle's own summary listener reported 315/311/0/4 — the 2-test difference is the listener's container-vs-leaf counting on a parameterized class, not a discrepancy in pass/fail; both counts agree on 0 failures and 0 errors). The formatter canary `MBeanNameUnitTest` reports 11/11 passing, 0 failures, 0 errors — the class count did not silently shrink from Phase 7's 306-test baseline; it grew, as expected with the canary restored.
- **Task 2 — Live gate:** Docker/colima confirmed up (`docker info` exit 0) before launch. The full live suite ran once, detached, via `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test`, completing in 3m 15s. Aggregate: 54 result-XML classes, 636 tests, **1 failure**, **0 errors**, 35 skipped. All four of this phase's named canaries are individually green: `MBeanRegistrationLiveTest` (2/2, the JMX-04 live proof), `JsonDynamicRoundTripTest` (6/6), `SmtSinkLandingTest` (5/5), and `ClickHouseSinkTaskTest` (16/16, 1 pre-existing unrelated skip). Zero occurrences of `InstanceAlreadyExistsException` in the suite log — no MBean leakage observed this run. The D-12 re-baselined pass condition is satisfied on all three parts: Phase 8's own two failures are gone, zero new failures appeared beyond the 1 expected carry-in, and every remaining failure/skip is named below with its owning phase.
- **Task 3 — extractNullables() triage:** Investigated past the plan's own two candidate explanations to the actual mechanism: `ClickHouseHelperClient.shouldSkipDescribeRow` (a fork-only addition, unrelated to `Table.java`) unconditionally treats any `.null`-suffixed DESCRIBE row as structural noise and drops it before it ever reaches `Table`'s constructor — for *any* Nullable column, not only JSON/Dynamic children. This exact behavior is itself a pinned, shipped fork requirement (`DESC-01: ".null subcolumn of a registered Nullable parent is skipped"`, `HLP-02`, closed `[x]` in REQUIREMENTS.md). `TableTest.extractNullables()` is upstream's own test, carried forward byte-identical by the merge, and it still encodes upstream's *June-2024* assumption that a `Nullable(Date)` column's `.null` subcolumn survives as a real `Column` (`allColumnsList.size()==3`) — an assumption upstream itself changed away from at some later commit before `v1.6.0` (confirmed: `git show pre-v1.6.0-sync:...TableTest.java` already expected `2`, matching the fork's pre-merge test; `git show v1.6.0:...TableTest.java` now expects `3`, an upstream-side change with no fork involvement). `Table.java` itself is byte-identical to upstream and structurally cannot cause this — its `addColumn`/`registerValidColumn` methods just count whatever list of `Column`s they are handed. No source was edited (`git diff v1.6.0 HEAD` on both `Table.java` and `TableTest.java` remains 0 lines).

## Task Commits

No task produced a code change (`files_modified: []` by design — this plan is verification and investigation only). No task-level commits were made; `git diff v1.6.0 HEAD` for every file this plan read remains 0 lines. The plan metadata commit (this SUMMARY.md plus the REQUIREMENTS.md flips) follows this SUMMARY.

## Files Created/Modified

None. No tracked source file, build file, or `.planning/MERGE-CHECKLIST.md` was touched by this plan (the checklist is explicitly plan 08-03's per this plan's own prohibitions).

## Decisions Made

- Forced `--rerun` on `testUnit` rather than accepting Gradle's `UP-TO-DATE` result from plan 08-01's session — this plan's own gate needed to be a genuine execution, not a cached result carried across plans.
- Ran the live suite exactly once, unfiltered, detached to a background job polled to completion — per the plan's explicit instruction not to narrow with `--tests` or stop at the first failure.
- Did not stop at the plan's two pre-supplied candidate causes for `extractNullables()` ("stale fork expectation" vs "genuine upstream behaviour change") once neither matched cleanly against `Table.java` alone; traced the mechanism one file further to `ClickHouseHelperClient.shouldSkipDescribeRow` and cross-referenced it against `DESC-01`'s own pinned assertion before writing the triage, per this task's own instruction to "determine which, by comparing the two revisions... and by reading what the test's fixture actually contains."

## Deviations from Plan

None - plan executed exactly as written. No auto-fixes were needed or applied; both Task 2's `<precondition>` (Docker reachable) and every acceptance criterion across all three tasks were satisfied without requiring a Rule 1-4 deviation.

## Full Failure Inventory (live suite, D-12 condition 3)

| Class | Test | Assertion / Exception | Owning phase |
|---|---|---|---|
| `com.clickhouse.kafka.connect.sink.db.mapping.TableTest` | `extractNullables()` | `org.opentest4j.AssertionFailedError: expected: <3> but was: <2>` | Untriaged pre-Phase-8 carry-in — see triage below. No existing phase (9, 10, 11) owns this fix; tracked as a milestone-level follow-up (see Next Phase Readiness). |

This is the sole failure in the entire 636-test live run — exactly the 1 the D-12 re-baseline permits, and exactly the failure named in `STATE.md`'s `[Phase 07 → untriaged]` row before this plan started.

## Skip Inventory (D-12 condition 3 / Phase 11 criterion 1 prerequisite)

35 skips across 9 classes, all pre-existing and explained by source-level `@Disabled`/`Assumptions.assumeTrue` annotations already in the tree — none newly introduced by this plan:

| Class | Skipped | Reason | Owning phase |
|---|---|---|---|
| `ClickHouseSinkTaskJsonInsertFormatTest` | 2 | `@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")` | Phase 9 |
| `JsonInsertColumnsLiveTest` | 5 | `@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")` | Phase 9 |
| `ExplicitJsonInsertPathLiveTest` | 6 | `@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")` | Phase 9 |
| `MetadataFreeJsonInsertUnitTest` | 4 | `@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")` | Phase 9 |
| `ClickHouseSinkTaskTest` | 1 | `clientNameTest()`: bare `@Disabled` — pre-existing upstream flaky-test disable ("TODO: fix instability of the test"), unrelated to JMX | Pre-existing upstream, not this milestone's scope |
| `ClickHouseSinkTaskWithSchemaTest` | 2 | `schemaWithTupleOfMapsWithVariantTest()` / `schemaWithNestedTupleMapArrayAndVariant()`: `@Disabled("Disabled because it requires a flag on the instance.")` | Pre-existing, not this milestone's scope |
| `ClickHouseSinkTaskWithSchemaTest` | 11 | Parameterized `exactlyOnceStateMismatchTestArgs` cases: `Assumptions.assumeTrue(isCluster \|\| isCloud)` — this run is standalone `latest`, not the cluster/cloud matrix this milestone deliberately excludes | Out of milestone scope (PROJECT.md: cluster/cloud matrix excluded) |
| `InsertFormatPerformanceTest` | 1 | `compareInsertPerformance()`: `@Disabled("Disabled because it not suitable for unit tests")` | Pre-existing, benchmark-only |
| `JsonColumnTest` | 1 | `compareAllConfigurations()`: `@Disabled("Disabled because it not suitable for unit tests")` | Pre-existing, benchmark-only |
| `ConvertToMapOrJsonPerformanceTestJson` (transforms) | 2 | Both cases: `@Disabled("Disabled because it not suitable for unit tests")` | Pre-existing, benchmark-only |

**Total: 2+5+6+4+1+2+11+1+1+2 = 35**, matching the aggregate. In addition, `ClickHouseTestHarnessTest.java` remains excluded from the test *compile* source set entirely (`build.gradle.kts`'s one remaining exclude, `# Phase 10 (HARN-04)`) — its absence is expected, is not part of the skip count above (it never compiles, so it produces no result XML at all), and is Phase 10's to remove.

## `TableTest.extractNullables()` Triage

- **Named cause:** `ClickHouseHelperClient.shouldSkipDescribeRow` (in `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java`, a fork-only file, re-applied by Phase 3 and re-expressed by Phase 7 plan 07-02) unconditionally treats any DESCRIBE row whose name ends in `.null`/`.size0`/bare `.size` as a structural sub-column and drops it before it ever reaches `Table`'s constructor — its own Javadoc states this applies "on every `Nullable`/`Array` column, regardless of parent type." This is exactly the mechanism `DESC-01` ("`.null` subcolumn of a registered Nullable parent is skipped", `ClickHouseHelperClientForkUnitTest.java`) pins, and `HLP-02` (`REQUIREMENTS.md`) is already closed `[x]` citing it.
- **Exact column and declared type:** `date_number Nullable(Date)` — specifically its structural `date_number.null` subcolumn (ClickHouse's internal Nullable null-bitmask, surfaced only because `describeTableV1`/`V2` set `describe_include_subcolumns=1`). It carries no user data of its own.
- **Supported explanation:** Neither of the plan's two candidates fits cleanly on its own, because `Table.java` — the file both candidates were framed around — is confirmed irrelevant: `git diff v1.6.0 HEAD -- Table.java` is 0 lines, and `Table.addColumn`/`registerValidColumn` unconditionally count whatever `List<Column>` they are handed; they perform no filtering themselves. The evidence instead supports a **stale test expectation, one level removed from where the plan looked**: `git show pre-v1.6.0-sync:.../TableTest.java` shows the fork's own pre-merge test already expected `allColumnsList.size()==2` (matching today's actual result); `git show v1.6.0:.../TableTest.java` shows upstream's *current* test expects `3` — upstream itself changed this expectation at some commit after `5a59e06a` ("Adding test and removing sub-column of Nullable", June 2024, which introduced the *same* 2-expecting test upstream and fork briefly shared) and before the `v1.6.0` tag, evidently because upstream stopped filtering `.null` subcolumns generically in its own `describeTableV1`/`V2` (confirmed: `git show v1.6.0:...ClickHouseHelperClient.java` has no `shouldSkipDescribeRow`-equivalent filter at all — every non-alias/materialized/ephemeral row, including `.null`, becomes a `Column`). The merge correctly kept upstream's `TableTest.java` byte-identical (it is not a conflicted path), but never reconciled its assertion against the fork's own deliberate, already-shipped, already-pinned `shouldSkipDescribeRow` design. The test's expectation of `3` is therefore the thing that is stale — not against "the fork's pre-merge behaviour" in the abstract, but against the fork's own current, intentional, and independently-tested (`DESC-01`) classification rule for exactly this column shape.
- **Owning phase:** None of Phase 9 (JSON path), Phase 10 (harness reconciliation), or Phase 11 (suite/CI/publication) has this in its declared requirements (`HARN-01..07`, `JSONP-01..04`, `SUITE-01..03`/`CI-05..07` — none mention `Table`/`describeTable` subcolumn semantics). This is a **milestone-level follow-up, not assignable to an existing phase without inventing scope** — most similar in shape to `WR-01`/`WR-03`/`P3-WR-01..03` already tracked in `PROJECT.md § Requirements → Active` as unscheduled review findings. It should be resolved before Phase 11 closes, since Phase 11's own acceptance criterion is a fully green `./gradlew test` (`SUITE-01`/`SUITE-02`) — but the *resolving action* (very likely: update `TableTest.extractNullables()`'s second assertion from `3` to `2`, since `DESC-01` is the already-decided, already-shipped source of truth) is not itself Phase 11's to invent; Phase 11 should either inherit a completed fix or make an explicit, recorded decision to keep it as a known accepted failure.
- **Consequence if left unfixed:** No data-loss or correctness risk — the filtered `.null` subcolumn carries no user-visible value, and no RowBinary/JSON insert path reads or writes it. The consequence is purely bookkeeping: `./gradlew test` will keep reporting exactly this 1 known, fully-explained failure indefinitely (it is deterministic, not flaky — it will reproduce on every run against `latest` with the current `shouldSkipDescribeRow`), consuming the milestone's "at most 1 failure" allowance until it is either fixed (update the stale assertion) or Phase 11 makes an explicit accepted-known-failure decision instead of a fully green suite.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- **Phase 8 is closed on live evidence**, not merely a green `testUnit`: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` ran once against a real ClickHouse container (colima profile `vz`), and all four ROADMAP-criterion canaries (`MBeanRegistrationLiveTest`, `JsonDynamicRoundTripTest`, `SmtSinkLandingTest`, `ClickHouseSinkTaskTest`) are individually green.
- **For plan 08-03:** this plan intentionally did not touch `.planning/MERGE-CHECKLIST.md` (prohibited by this plan's own frontmatter, D-14). Plan 08-03 should use this SUMMARY's failure/skip inventory and the `extractNullables()` triage block as its evidence source for any checklist rows it flips or annotates.
- **For Phase 9:** the 4 JSON-path classes (`ClickHouseSinkTaskJsonInsertFormatTest`, `JsonInsertColumnsLiveTest`, `ExplicitJsonInsertPathLiveTest`, `MetadataFreeJsonInsertUnitTest`, totaling 17 skips) are exactly the tests Phase 9's `JSONP-01` keep/drop decision will re-enable or retire.
- **For Phase 10:** zero `InstanceAlreadyExistsException` occurrences observed this run — the `@AfterEach` MBean leak sweep's absence (HARN-02) did not manifest as a visible collision in this single sequential run, but that is not proof of its absence under different test ordering/parallelism; HARN-02 remains Phase 10's to restore regardless. `ClickHouseTestHarnessTest.java`'s compile-exclusion is confirmed still the sole remaining `build.gradle.kts` exclude line, exactly as Phase 8 left it.
- **For Phase 11:** inherits exactly one open item from this milestone's live-suite gate — `TableTest.extractNullables()`, with the named cause above, needing either a one-line test-expectation fix or an explicit accepted-failure decision before `SUITE-01`/`SUITE-02`'s fully-green-suite criterion can be satisfied honestly.
- No blockers for plan 08-03.

## Self-Check: PASSED

- `/tmp/gsd-08-02-testunit.log` and `/tmp/gsd-08-02-fullsuite.log` both confirmed present and non-empty on disk.
- `build/test-results/testUnit/` confirmed to hold 28 `TEST-*.xml` files (≥27 required), summing to 317 tests (≥306 required), 0 failures, 0 errors.
- `build/test-results/test/` confirmed to hold 54 `TEST-*.xml` files, summing to 636 tests (≥306 required), 1 failure (≤1 required), 0 errors (0 required), 35 skipped.
- `MBeanRegistrationLiveTest`, `JsonDynamicRoundTripTest`, `SmtSinkLandingTest`, and `ClickHouseSinkTaskTest` each independently re-confirmed at 0 failures / 0 errors from their own result XML.
- Zero occurrences of `InstanceAlreadyExistsException` re-confirmed via `grep -c` against `/tmp/gsd-08-02-fullsuite.log`.
- `git diff v1.6.0 HEAD` re-confirmed as 0 lines for both `Table.java` and `TableTest.java` after the triage task, proving the read-only constraint held.
- `git status --porcelain` re-confirmed to show no change under `src/`, `build.gradle.kts`, or `.planning/MERGE-CHECKLIST.md` attributable to this plan — only the pre-existing, explicitly-untouched `.planning/` files (`ROADMAP.md`, `STATE.md`, `state.json`, `milestone.lock`, the two `*-DISCUSS-CHECKPOINT.json` deletions) that predate this plan's dispatch remain in the working tree.

---
*Phase: 08-orchestration-jmx-re-plumbing*
*Completed: 2026-09-19*
