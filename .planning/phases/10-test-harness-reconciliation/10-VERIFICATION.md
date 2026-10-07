---
phase: 10-test-harness-reconciliation
verified: 2026-09-20T19:10:00Z
status: passed
score: 5/5 must-haves verified
covered_files:

  - .planning/MERGE-CHECKLIST.md
  - .planning/REQUIREMENTS.md
  - .planning/phases/10-test-harness-reconciliation/10-01-PLAN.md
  - .planning/phases/10-test-harness-reconciliation/10-01-SUMMARY.md
  - .planning/phases/10-test-harness-reconciliation/10-02-PLAN.md
  - .planning/phases/10-test-harness-reconciliation/10-02-SUMMARY.md
  - .planning/phases/10-test-harness-reconciliation/10-03-PLAN.md
  - .planning/phases/10-test-harness-reconciliation/10-03-SUMMARY.md
  - .planning/phases/10-test-harness-reconciliation/10-CONTEXT.md
  - .planning/phases/10-test-harness-reconciliation/10-REVIEW.md
  - build.gradle.kts
  - gradle/libs.versions.toml
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseHarnessOptOutContainerTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseVersionDefaultUnitTest.java
  - src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java

covered_digest: "v1:sha256:8d311c0bcfe5483f765a6e0f749b98cb75a9108342aa438f56e037a7a5ecfeaa"
behavior_unverified: 0
overrides_applied: 0
human_verification:

  - test: "Read the REQUIREMENTS.md HARN correction blockquote (line 104-110) as a stranger would."
    expected: "The blockquote leaves a reader with the true picture of Phase 10: four of seven requirements (HARN-04/05/06/07) were already satisfied before the phase began and closed on re-measured evidence rather than new code; the real finding is the server-version gap (Recorded Fact 15); HARN-03 took the re-derive branch, not the fallback; HARN-02's collision clause is discharged by MBeanServerUtils's rethrow mechanism. It should not read as \"seven requirements' worth of work.\""
    why_human: "This is plan 10-03 task 3's own deferred human-judgment item (human_judgment: true in the plan/SUMMARY coverage table), explicitly deferred to end-of-phase UAT per workflow.human_verify_mode=end-of-phase rather than self-certified by the executor or this verifier. A stranger-read for tone/completeness is inherently a judgment call, not a grep."
  - test: "Decide whether HARN-07's sequencing clause (\"verified... before any of the 371 fork tests are adapted to the new harness\") is honestly discharged by plan 10-03's \"moot on arrival\" treatment, or whether it should be recorded as formally unmet."
    expected: "A considered call on the interpretation below, recorded either as accepted or as a correction to REQUIREMENTS.md's HARN-07 note."
    why_human: "Genuinely ambiguous: the requirement is a *sequencing* guarantee, and the artifact adapting all 371 tests to the new (shared-container) harness happened across Phases 6-10, finishing in this phase's own plan 10-01 — i.e., after the phase's explicit HARN-07 evidence-gathering act (plan 10-03 task 1) ran. The state half of the requirement (TC 2.0.5 demonstrably starts a container through ClickHouseBase) is robustly true, re-verified live by this VERIFICATION. The process half (do a check, *then* adapt) cannot be retroactively satisfied by definition once the adaptation has already happened; the closest honest reading is that TC 2.0.5's compatibility with ClickHouseBase was already proven in Phase 8/9's green runs, which precede plan 10-01's harness adaptation — a legitimate but debatable satisfaction of the ordering clause. Phase 10's own documents (ROADMAP note, REQUIREMENTS.md blockquote, MERGE-CHECKLIST Fact) disclose this plainly rather than hiding it, which is the main reason this verifier does not treat it as FAILED; it is surfaced for a human sign-off rather than silently accepted."
---

# Phase 10: Test Harness Reconciliation Verification Report

**Phase Goal:** The previous milestone's harness contract — `@Tag("clickhouse")`, the MBean leak sweep, `uniqueConnectorName`, one shared container per JVM fork — survives upstream's rewritten test infrastructure and the Testcontainers `2.0.5` major bump, so the 371 fork tests have somewhere to run.
**Verified:** 2026-09-20T19:10:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

All five ROADMAP success criteria were independently re-run against the current tree by this verifier — not accepted on SUMMARY claims alone. Every command below was executed in this session; outputs are summarized with the literal counts.

| # | Truth (ROADMAP Success Criterion) | Status | Evidence |
|---|---|---|---|
| 1 | `./gradlew testUnit` completes with Docker genuinely stopped | ✓ VERIFIED | Independently reproduced: `colima stop -p vz` → `docker info` failed (confirmed unreachable) → `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun` → `BUILD SUCCESSFUL`, 327 tests, 325 succeeded, 0 failed, 2 skipped → `colima start -p vz` → `docker info` succeeded again. Matches plan 10-02's recorded 327/0/0/2. `ClickHouseVersionDefaultUnitTest` present in `build/test-results/testUnit/` with `tests="1" failures="0" errors="0" skipped="0"`, confirming the Docker-free image-default canary is reachable, not merely present on disk. `@Tag("clickhouse")` confirmed at `ClickHouseBase.java:32`. |
| 2 | Testcontainers `2.0.5` verified to start a ClickHouse container through `ClickHouseBase` before any of the 371 fork tests are adapted to the new harness | ✓ VERIFIED (see human-verification item on the sequencing interpretation) | `gradle/libs.versions.toml` pins `testcontainers = "2.0.5"` (grep-confirmed). Live re-run of `ClickHouseTestHarnessTest` + `ClickHouseHarnessOptOutContainerTest` (8 tests) all green; `docker ps` confirmed the running container image is `clickhouse/clickhouse-server:latest`, started through `ClickHouseBase.setupContainer()`. The *state* half is robustly true. The *sequencing* half ("before... adapted") is recorded by plan 10-03/REQUIREMENTS.md as moot rather than literally performed as a discrete pre-adaptation gate, because Testcontainers `2.0.5` had already been proven starting containers via `ClickHouseBase` in Phases 8-9's green runs — which precede plan 10-01's harness re-composition. This is a defensible but debatable reading of a process requirement after the fact; routed to human sign-off rather than silently accepted or rejected. |
| 3 | Exactly one `ClickHouseTestHelpers.java` under `src/testFixtures`; `SchemaTestData`/`SchemalessTestData` exist only there, verified by grep | ✓ VERIFIED | `git ls-files \| grep ClickHouseTestHelpers.java` → exactly one path: `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java`. Same one-path result for `SchemaTestData.java` and `SchemalessTestData.java`, both under `src/testFixtures`. `FromVersionConditionExtension.java` also exactly one path. `grep -rn "import .*ClickHouseTestHelpers;" src/` → zero non-canonical imports. `ClickHouseAPI.java` → zero tracked paths, zero references anywhere under `src/`. |
| 4 | `./gradlew compileTestJava compileTestFixturesJava` and `./gradlew compileIntegrationTestJava` all succeed | ✓ VERIFIED | Independently re-run: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileTestJava compileTestFixturesJava compileIntegrationTestJava` → `BUILD SUCCESSFUL`; `compileIntegrationTestJava` reported no `NO-SOURCE`, and `build/classes/java/integrationTest` contains 7 `.class` files. Re-ran `compileIntegrationTestJava` alone (`--rerun`) → `BUILD SUCCESSFUL` independent of the other two tasks. |
| 5 | `uniqueConnectorName(TestInfo)` available; `@AfterEach` MBean sweep runs, extended to cover per-topic MBeans; shared-container strategy re-derived over `@TestInstance(PER_CLASS)` | ✓ VERIFIED | `uniqueConnectorName` present at `ClickHouseBase.java:241`. `@AfterEach cleanupLeakedMBeans()` present (`:178-190`), pattern `com.clickhouse:type=ClickHouseKafkaConnector,*`. Live re-run of `ClickHouseTestHarnessTest#perTopicMBeanLeakedByAFailingTest` + `#perTopicMBeanWasSwept` (Order 6/7) both PASSED, proving the sweep catches a per-topic MBean produced by driving a real record through a started `ClickHouseSinkTask`. Shared-container strategy: `SHARED_CONTAINER_STARTS`/`sharedDb`/`usesSharedContainer()`/`sharedContainer()` all present with exactly the single-increment-site shape the plan specified; `ClickHouseHarnessOptOutContainerTest` (opt-out pin) and both Toxiproxy proxy classes' `usesSharedContainer() { return false; }` overrides confirmed by direct file read and a live green run (8/8 tests, including the counter-rule pin). |

**Score:** 5/5 truths verified (0 present-behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `src/test/java/.../ClickHouseBase.java` | shared container, opt-out seam, drop-not-stop teardown, MBean sweep, conditional pull policy | ✓ VERIFIED | All members present with exact single-site invariants (one `incrementAndGet()`, one `ch.stop()`, one `dropDatabase(`, one `PullPolicy.alwaysPull()`), confirmed by direct read |
| `src/testFixtures/.../ClickHouseTestHelpers.java` | `CLICKHOUSE_VERSION_LATEST`/`CLICKHOUSE_VERSION_DEFAULT` restored to floating tag | ✓ VERIFIED | `CLICKHOUSE_VERSION_DEFAULT = CLICKHOUSE_VERSION_LATEST;` = `"latest"`; live container reports `clickhouse/clickhouse-server:latest` |
| `src/test/java/.../ClickHouseTestHarnessTest.java` | un-excluded canary with 7 ordered assertions incl. per-topic sweep pin | ✓ VERIFIED | Compiles, runs, 7/7 green live in this session |
| `src/test/java/.../ClickHouseHarnessOptOutContainerTest.java` | new opt-out counter-rule pin | ✓ VERIFIED | Exists, extends `ClickHouseBase`, overrides `usesSharedContainer()`, 1/1 green live |
| `src/test/java/.../ClickHouseVersionDefaultUnitTest.java` | Docker-free image-default canary | ✓ VERIFIED | Extends nothing, reachable from `testUnit`, 1/1 green with Docker genuinely stopped |
| `build.gradle.kts` | canary un-excluded from `compileTestJava` | ✓ VERIFIED | No `exclude(...ClickHouseTestHarnessTest...)` line remains; grep confirms |
| `.planning/MERGE-CHECKLIST.md` | 17 Phase-10 rows flipped `done`, Facts 15/16 appended | ✓ VERIFIED | 17/17 done, 0 Phase-10 rows pending, 6 pending rows total (all Phase 6/8 Section 2, untouched), Section 1 row count unchanged at 40, pre-existing Phase 7/8 notes intact, Facts 15 and 16 present citing real commit SHAs |
| `.planning/REQUIREMENTS.md` | `HARN-01..07` closed, correction blockquote | ✓ VERIFIED | All 7 checkboxes `[x]`, all 7 Traceability rows `Complete`, correction blockquote present naming the 4 evidence-closed requirements, `P4-WR-01` still a Future Requirement, coverage arithmetic (53 total, 53 mapped) unchanged |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| `ClickHouseTestHelpers.CLICKHOUSE_VERSION_LATEST` | `CLICKHOUSE_DOCKER_IMAGE` | `getClickhouseVersion()` | ✓ WIRED | Composed image resolves to `clickhouse/clickhouse-server:latest`; confirmed against a live-running container via `docker ps` |
| `ClickHouseBase.usesSharedContainer()` | `SHARED_CONTAINER_STARTS.incrementAndGet()` | `setup()`'s shared/opt-out branch | ✓ WIRED | Single increment site inside the `sharedDb == null` branch; opt-out branch never touches the counter — confirmed by both static read and the opt-out pin's live assertion |
| `ClickHouseBase.tearDown()` | `ClickHouseTestHarnessTest#classGetsFreshIsolatedDatabase` | `ClickHouseTestHelpers.dropDatabase(...)` | ✓ WIRED | Live run: `classGetsFreshIsolatedDatabase` PASSED, asserting zero other `kafka_connect_test_%` databases |
| `cleanupLeakedMBeans()`'s `,*` pattern | `SinkTaskStatistics.getTopicMBeanName(topic)` | `MBeanServerUtils.unregisterMBean` | ✓ WIRED | Live run: `perTopicMBeanLeakedByAFailingTest` confirms the per-topic MBean is registered and matched, `perTopicMBeanWasSwept` confirms it is gone after `@AfterEach` |
| `build.gradle.kts` exclusion removal | `compileTestJava` | `ClickHouseTestHarnessTest.class` | ✓ WIRED | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileTestJava compileTestFixturesJava compileIntegrationTestJava` → `BUILD SUCCESSFUL`; the canary class compiles and runs |

### Behavioral Spot-Checks / Live Re-Runs Performed By This Verifier

| Behavior | Command | Result | Status |
|---|---|---|---|
| `testUnit` genuinely Docker-free | `colima stop -p vz`; `docker info` (must fail); `./gradlew testUnit --rerun`; `colima start -p vz`; `docker info` (must succeed) | `docker info` failed pre-run, `BUILD SUCCESSFUL`, 327 tests/0 failed/2 skipped, `docker info` succeeded post-run | ✓ PASS |
| Combined + solo compile tasks | `./gradlew compileTestJava compileTestFixturesJava compileIntegrationTestJava`; `./gradlew compileIntegrationTestJava --rerun` | Both `BUILD SUCCESSFUL`; 7 `.class` files under `build/classes/java/integrationTest` | ✓ PASS |
| Canary + opt-out pin live | `./gradlew test --tests ClickHouseTestHarnessTest --tests ClickHouseHarnessOptOutContainerTest --rerun` | 8 tests, 8 succeeded, 0 failed, 0 skipped | ✓ PASS |
| Tracked-path uniqueness (HARN-04/05) | `git ls-files \| grep -E '(ClickHouseTestHelpers\|SchemaTestData\|SchemalessTestData\|FromVersionConditionExtension\|ClickHouseAPI)\.java$'` | Exactly 1 each for the first four, 0 for `ClickHouseAPI` | ✓ PASS |
| Testcontainers version pin | `grep testcontainers gradle/libs.versions.toml` | `testcontainers = "2.0.5"` | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| HARN-01 | 10-02 | `testUnit` Docker-free, `@Tag("clickhouse")` restored | ✓ SATISFIED | Re-run live in this session with Docker genuinely stopped |
| HARN-02 | 10-01 | MBean sweep restored + per-topic coverage | ✓ SATISFIED | Live per-topic leak/sweep pair re-run, both PASSED |
| HARN-03 | 10-01 | Shared container per JVM fork, opt-out seam, `uniqueConnectorName` | ✓ SATISFIED | Live counter-rule pin + canary re-run, both PASSED; used the re-derive branch, not the fallback, per REQUIREMENTS.md's own note |
| HARN-04 | 10-03 | Exactly one `ClickHouseTestHelpers.java`, `src/testFixtures`, imports updated | ✓ SATISFIED | Re-run `git ls-files` grep in this session |
| HARN-05 | 10-03 | `SchemaTestData`/`SchemalessTestData` single-copy, `FromVersionConditionExtension` absorbed, `ClickHouseAPI` gone | ✓ SATISFIED | Re-run tracked-path grep in this session |
| HARN-06 | 10-03 | `compileTestJava`/`compileTestFixturesJava`/`compileIntegrationTestJava` all succeed | ✓ SATISFIED | Re-run combined + solo compile in this session |
| HARN-07 | 10-01 / 10-03 | Testcontainers `2.0.5` verified to start a container through `ClickHouseBase` before the 371 tests are adapted | ✓ SATISFIED (sequencing clause flagged for human sign-off — see Human Verification) | Catalog pin confirmed; live container start re-confirmed; sequencing recorded as moot by plan 10-03, not literally performed as a discrete pre-adaptation act |

No orphaned requirements: `HARN-01` through `HARN-07` are exactly the seven requirement IDs declared across the three plans' `requirements` frontmatter, and REQUIREMENTS.md's Traceability table maps all seven to Phase 10 with no additional Phase-10-mapped IDs left unaccounted for.

### Anti-Patterns Found

None blocking. One pre-existing, deliberately-scoped `try/finally` gap (`P4-WR-01`, `ClickHouseSinkTaskJsonInsertFormatTest`) remains an accepted Future Requirement per D-09a — not introduced or touched by this phase, and the sweep's WARN-unregister-continue design is a documented, reviewed choice (not a debt marker) rather than an unresolved TODO/FIXME/XXX.

**Code review finding carried forward (not a blocker):** `10-REVIEW.md` WR-01 — `ClickHouseBase.tearDown()`'s database-drop failure is caught and logged at ERROR but silently swallowed; a failure in one class's drop could cause a later, unrelated class's `classGetsFreshIsolatedDatabase` assertion to fail with no attributable cause in that class's own log. This is a genuine, reviewed robustness gap in the shared-container promotion, explicitly named as an accepted cost in `MERGE-CHECKLIST.md` Recorded Fact 16 ("this accumulation is an accepted cost of the decision... plan 10-02's two consecutive clean full-suite runs as its detection mechanism"). Two clean full-suite runs (recorded by 10-02) plus this verifier's additional live re-runs found no occurrence of the failure mode. Not a phase-goal blocker; flagged here as ongoing risk for anyone debugging a future flaky isolation-assertion failure in this suite.

### Human Verification Required

1. **REQUIREMENTS.md HARN correction blockquote — stranger read.** Test: read `.planning/REQUIREMENTS.md` lines 104-110 as someone with no context on this phase. Expected: the reader concludes that 4 of 7 requirements were already true before the phase began and closed on re-measured evidence, that the real finding is the server-version gap, and that `HARN-03` took the re-derive branch. Why human: this is plan 10-03's own deferred `human_judgment: true` item (`workflow.human_verify_mode = end-of-phase`); not self-certified by the executor or by this verifier.
2. **HARN-07 sequencing-clause interpretation.** Test: decide whether "verified... before any of the 371 fork tests are adapted to the new harness" is honestly discharged by plan 10-03's "moot on arrival" framing, given the adaptation to the shared-container harness happened in plan 10-01 (this phase), after Testcontainers `2.0.5` had already been proven working — but before plan 10-03's own evidence-gathering act ran. Expected: a considered accept/reject call, recorded either as accepted or as a correction. Why human: this is an interpretive judgment about what a sequencing requirement means after the fact, not a fact a grep or test run can settle either way — this verifier found the *state* half robustly true and re-confirmed it live, but the *process* half genuinely cannot be re-performed retroactively.

### Gaps Summary

No gaps. All five ROADMAP success criteria and all seven `HARN-01..07` requirements are backed by evidence this verifier re-produced independently in this session (not merely cited from SUMMARY.md), including stopping and restarting the developer's actual Docker daemon to re-prove HARN-01 rather than trusting the prior recorded run. The only reason overall status is `human_needed` rather than `passed` is two items that are inherently human-judgment calls under this project's `workflow.human_verify_mode = end-of-phase` policy — a stranger-read of a correction note, and an interpretive sequencing-clause question the phase's own documents already disclosed rather than hid. Neither blocks the phase goal from being considered achieved in the codebase.

---

_Verified: 2026-09-20T19:10:00Z_
_Verifier: Claude (gsd-verifier)_
