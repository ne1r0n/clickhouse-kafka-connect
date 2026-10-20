---
phase: 17-ci-build-test-hygiene
verified: 2026-10-12T09:00:00Z
status: passed
score: 5/5 must-haves verified
covered_files:
  - ".github/dependabot.yml"
  - ".github/workflows/tests.yaml"
  - ".planning/phases/17-ci-build-test-hygiene/17-01-PLAN.md"
  - ".planning/phases/17-ci-build-test-hygiene/17-01-SUMMARY.md"
  - ".planning/phases/17-ci-build-test-hygiene/17-02-PLAN.md"
  - ".planning/phases/17-ci-build-test-hygiene/17-02-SUMMARY.md"
  - ".planning/phases/17-ci-build-test-hygiene/17-03-PLAN.md"
  - ".planning/phases/17-ci-build-test-hygiene/17-03-SUMMARY.md"
  - ".planning/phases/17-ci-build-test-hygiene/17-04-PLAN.md"
  - ".planning/phases/17-ci-build-test-hygiene/17-04-SUMMARY.md"
  - "build.gradle.kts"
  - "docs/FORK-DELTA.md"
  - "lombok.config"
  - "src/test/java/com/clickhouse/kafka/connect/ci/TestRuntimeClasspathLz4UnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java"
covered_digest: "v3:sha256:dda874cc5be44ae523423586f026f5eadcec127f5541cbc7089b4f334f39e036"
behavior_unverified: 0
overrides_applied: 0
re_verification: false
advisory:
  - finding: "WR-01 (17-REVIEW): Dependabot `exclude-paths` on the github-actions ecosystem is confirmed only from dependabot-core source, never observed in a real run; the guard test does not pin the four exclude entries; a rejected key could mark the whole dependabot.yml invalid and also stall the gradle entry"
    category: other
    reason: "Not part of CI-15's text (ecosystem present). Resolve by adding the 'no Dependabot config error, gradle entry still runs' check to Phase 20's operator list and, optionally, a guard assertion on the four exclude entries"
    evidence_status: "none provided (open in 17-REVIEW-DISPOSITION)"
deferred:
  - truth: "Dependabot version updates are enabled on the fork and the default branch (main) carries the config, so the pinned actions actually receive update PRs"
    addressed_in: "Phase 20"
    evidence: "ROADMAP Phase 20 note: 'Manual checks for the user ... Whether Dependabot version updates are enabled in the fork's settings. Which branch is the GitHub default'. Phase 17 CONTEXT D-05 scopes CI-15 to the config file alone"
---

# Phase 17: CI, Build & Test Hygiene Verification Report

**Phase Goal:** CI runs on a SHA-pinned, least-privilege, Node 24 workflow on `ubuntu-24.04` that reports Gradle's own fork count, and the Phase 14 build/test debt is closed (Lombok warning, duplicate lz4 on the test classpath, unasserted LZ4 response leg, NanoTimestamp edge row). A green run on a pushed SHA proves it.
**Verified:** 2026-10-12
**Status:** passed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `tests.yaml` on `ubuntu-24.04`, `permissions: contents: read`, `persist-credentials: false`, every action SHA-pinned with `# vX.Y.Z` (Node 24 majors, peeled commit for gradle/actions); dependabot has `github-actions`; `test-head.yaml` no longer calls `tests.yaml` | VERIFIED | Read `tests.yaml`: top-level `permissions: contents: read`, `runs-on: ubuntu-24.04`, checkout `persist-credentials: false`, five `uses:` lines all `@<40hex> # vX.Y.Z` (checkout v7.0.1, setup-java v6.0.1, setup-gradle v6.4.0, upload-artifact v7.0.2 x2). I re-ran `git ls-remote` on all four tags: checkout/setup-java/upload-artifact SHAs equal the tag commits; gradle/actions pinned `3f5f9ada...` equals the peeled `v6.4.0^{}` (tag object is `b9bee63e...`, correctly not used). `dependabot.yml` has `gradle` plus `github-actions` (monthly, label, limit 5, grouped). `.github/workflows/test-head.yaml` absent, `git grep test-head` outside `.planning` finds only the FORK-DELTA row, no `workflow_call` in `tests.yaml`. `TestsWorkflowPinGuardUnitTest` 5/5 green on my re-run. |
| 2 | Fork-count step reports `maxParallelForks` read from a Gradle task; no formula copy in shell | VERIFIED | `build.gradle.kts` registers `reportTestForks` (lazy `tasks.named<Test>("test").map { it.maxParallelForks }`); `./gradlew -q reportTestForks` prints `6` locally. The step runs `./gradlew -q --console=plain reportTestForks \| tail -n 1`, rejects non-integers, and `nproc` appears only in the message text. No `/ 2` or cluster-name branch remains in the workflow. CI notice observed on the run: `runner nproc=4, Gradle maxParallelForks=2 (...)`. |
| 3 | One user-approved SHA pushed on one branch, no tag, exactly one green `tests.yaml` run showing `ubuntu-24.04`, only Contents/Metadata read, job summary + fork-count notice, both artifacts | VERIFIED | Re-queried the public API myself. `actions/runs?head_sha=0c90d331...` returns total_count 1: run 38172811430, path `.github/workflows/tests.yaml`, event push, branch `sync/v1.7.0`, attempt 1, completed/success. Job labels `["ubuntu-24.04"]`, all 14 steps success (Set up Gradle succeeds under read-only token). Annotations: 21 (1 notice `Test fork count`, 20 pre-existing javac warnings); no Node 20, no `Ambiguous: Jackson2`, no ubuntu-latest migrate annotation. Artifacts `gradle-test-reports` and `jacoco-coverage-reports` present. `git ls-remote --heads origin` now shows only `custom_build` (be6b8861, unchanged) and `main`: scratch branch deleted. Image `ubuntu-24.04` / `Contents: read`, `Metadata: read` / four download-by-SHA lines are in E-11 from the job log. The user's direct `push` approval is recorded in E-10 (2026-10-11T20:34:36Z). |
| 4 | No Lombok "Ambiguous: Jackson2 and Jackson3" warning; `javap` shows `ClickHouseFieldDescriptor` unchanged; `testRuntimeClasspath` resolves only `at.yawk.lz4`, pinned by a Docker-free test; `runtimeClasspath`/`integrationTestRuntimeClasspath` unchanged | VERIFIED | `lombok.config` = 3 lines incl. `lombok.jacksonized.jacksonVersion += 2` (only diff vs phase start; `addLombokGeneratedAnnotation` intact). My `compileJava --rerun` printed 0 "Ambiguous: Jackson" lines. `dependencies --configuration` re-run: `testRuntimeClasspath` shows only `at.yawk.lz4` entries; `runtimeClasspath` and `integrationTestRuntimeClasspath` still show `org.lz4:lz4-java:1.8.0 -> at.yawk.lz4:lz4-java:1.12.0`. `build.gradle.kts` uses a scoped `configurations.named("testRuntimeClasspath") { exclude(...) }`, no `configurations.all { exclude }`, and `withCapability("org.lz4:lz4-java")` count stays 1. `TestRuntimeClasspathLz4UnitTest` (counts `LZ4Factory.class` URLs == 1, from `at.yawk.lz4`) passes under `testUnit`. The javap no-change diff rests on E-07 (1193/420 lines cmp-identical); I did not rebuild the old tree to repeat it, and the CI run carries no Lombok annotation. |
| 5 | `Lz4RoundTripLiveTest` asserts LZ4 on the response leg, seen red with server compression off; `MetadataFreeJsonWriterTest` pins NanoTimestamp mantissa-with-fraction | VERIFIED | Assertion reads the read query's own `system.query_log` row (`NetworkSendBytes < result_bytes/10`, method == lz4). I re-ran the single test against live ClickHouse: 2 tests, 0 failures, `Response leg: NetworkSendBytes=61864 result_bytes=4790219 ratio=0.0129 method=lz4`. The red leg is in `/tmp/gsd17/exec/debt06-red.log` (`response not compressed: NetworkSendBytes=4778633 result_bytes=4790219`, BUILD FAILED), and the committed test file contains `.compressServerResponse(true)` (flip reverted, not committed). Four rows (`123`, `-123`, `999`, `1000`) added; `MetadataFreeJsonWriterTest` is green in my re-run (51 tests across the three Docker-free classes, 0 failed). |

**Score:** 5/5 truths verified (0 present-but-behavior-unverified)

### Deferred Items

| # | Item | Addressed In | Evidence |
|---|------|--------------|----------|
| 1 | Dependabot version updates enabled on the fork and `dependabot.yml` on the default branch (`main`) | Phase 20 | ROADMAP Phase 20 manual checks list both; CONTEXT D-05 scopes CI-15 to the config file |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `.github/workflows/tests.yaml` | Hardened workflow | VERIFIED | Substantive, exercised by green run 38172811430; `timeout-minutes: 45` anchor count 1 |
| `.github/workflows/test-head.yaml` | Deleted | VERIFIED | Absent from tree and index |
| `.github/dependabot.yml` | github-actions entry | VERIFIED | Present with `exclude-paths` for the four upstream workflows (unobserved, see WR-01) |
| `build.gradle.kts` | `reportTestForks`, test-scoped lz4 exclude | VERIFIED | Both present and exercised |
| `lombok.config` | `jacksonVersion += 2` | VERIFIED | Present, warning gone |
| `TestsWorkflowPinGuardUnitTest`, `TestRuntimeClasspathLz4UnitTest` | Docker-free guards in `testUnit` | VERIFIED | Not tagged `clickhouse`; pass |
| `Lz4RoundTripLiveTest`, `MetadataFreeJsonWriterTest` | Response-leg assertion, NanoTimestamp rows | VERIFIED | Present and green |
| `docs/FORK-DELTA.md`, `MERGE-CHECKLIST` Fact 41 | Rows and closure notes | VERIFIED | CI row rewritten, test-head, Dependabot, lombok and lz4 rows added, Known costs 5 to 7 updated |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `tests.yaml` fork-count step | `build.gradle.kts` `reportTestForks` | `./gradlew reportTestForks` | WIRED | Task exists; CI notice shows the value |
| `reportTestForks` | `test` task `maxParallelForks` | lazy provider | WIRED | Prints 6 locally, 1 with `CLICKHOUSE_CLUSTER_NAME` (E-01) |
| Pin guard test | `tests.yaml` / `dependabot.yml` | line-level read | WIRED | Fails red on pre-change tree (E-03) |
| `testRuntimeClasspath` exclude | `LZ4Factory` single-URL test | classpath | WIRED | Red before (2 URLs), green after |

### Data-Flow Trace (Level 4)

Not applicable: CI, build and test changes; no rendered dynamic data.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Fork count task | `./gradlew -q --console=plain reportTestForks` | `6` | PASS |
| Docker-free guards + NanoTimestamp | `./gradlew testUnit --rerun --tests` on the three classes | 51 tests, 0 failed | PASS |
| LZ4 response leg live | `./gradlew test --tests '*Lz4RoundTripLiveTest' --rerun` | 2 tests, 0 failed, ratio 0.0129 | PASS |
| Lombok warning | `./gradlew compileJava --rerun` grep "Ambiguous: Jackson" | 0 | PASS |
| Pins vs tags | `git ls-remote` x4 | all match, gradle peeled | PASS |
| CI run | public REST API (runs, jobs, annotations, artifacts) | 1 run, success, ubuntu-24.04 | PASS |

The orchestrator's `./gradlew test` full-suite result (840 tests, 0 failures, 18 named skips, E-09) is accepted as given; I re-ran only the named tests above.

### Probe Execution

SKIPPED: no probes declared by the phase.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| CI-11 | 17-01 | Node 24 majors, SHA pins with version comment | SATISFIED | Truth 1 |
| CI-12 | 17-01 | `ubuntu-24.04` | SATISFIED | Truth 1, job labels |
| CI-13 | 17-01 | `permissions: contents: read`, `persist-credentials: false` | SATISFIED | Truth 1, E-11 token block |
| CI-14 | 17-01 | Fork count from Gradle | SATISFIED | Truth 2 |
| CI-15 | 17-01 | Dependabot `github-actions` ecosystem | SATISFIED (config) | Truth 1; runtime effect deferred to Phase 20, see WR-01 advisory |
| CI-16 | 17-01 | `test-head.yaml` fixed or disabled | SATISFIED | Deleted, static proof (E-04) |
| CI-17 | 17-03, 17-04 | One green run proves CI-11..16 | SATISFIED | Truth 3 |
| BLD-14 | 17-02 | Lombok warning gone, javap unchanged | SATISFIED | Truth 4 |
| DEBT-06 | 17-02 | LZ4 response leg asserted, seen red | SATISFIED | Truth 5 |
| DEBT-07 | 17-02 | Single lz4 on test classpath | SATISFIED | Truth 4 |
| DEBT-08 | 17-02 | NanoTimestamp scientific row | SATISFIED | Truth 5 |

All 11 IDs appear in plan frontmatter (17-01: CI-11..16; 17-02: DEBT-06/07/08, BLD-14; 17-03 and 17-04: CI-17), match REQUIREMENTS.md rows mapped to Phase 17, and no Phase 17 ID is orphaned.

### Anti-Patterns Found

None blocking. No TBD/FIXME/XXX markers in the changed files; the 20 javac warnings on the CI run are pre-existing source warnings unrelated to this phase.

### D4 decision (log-sourced evidence instead of a web-UI paste)

Accepted as satisfying D-13. The run's runner image, token permissions and action downloads come from the runner's own job log (stronger than a UI paste), and the key facts are independently corroborated by what I read myself through the public API: job label `ubuntu-24.04`, all steps including `Set up Gradle` succeeding under read-only permissions, the notice annotation, and both artifacts. The `##[group]`/`##[endgroup]` strip is recorded with a diff and file hashes, and the user's explicit sign-off is recorded in E-11. The one reconstructed item is the rendered job summary: it is rebuilt from the log's step script and `jacoco_summary.py` output, and `jacoco_summary.py` writes to `$GITHUB_STEP_SUMMARY` (line 121), so the content is deterministic from the log. I could not re-read the log (403 without a token), so E-11's quoted lines rest on the hashes and the user's approval plus my API corroboration.

### Code review findings (17-REVIEW, all open, advisory)

- **WR-01 (warning):** does not fail CI-15's text (the ecosystem entry exists and the guard asserts it). It is a real unobserved risk: if Dependabot rejects `exclude-paths`, the whole file could be flagged invalid and stall the existing gradle entry. Recommended: add "no Dependabot config error, gradle entry still runs" to Phase 20's manual checks and pin the four exclude entries in the guard. Note F1: the default branch is `main`, so Dependabot reads `main` and the config is inert until the default-branch decision and settings check in Phase 20; this is documented in FORK-DELTA and the ROADMAP, and is not a Phase 17 gap.
- **IN-01..IN-03 (info):** the guard does not pin `reportTestForks` usage; two regexes are brittle (fail closed); a duplicated jar-origin assertion. None affects a truth.

### Gaps Summary

No gaps. The phase goal is achieved: the hardened workflow, Gradle-sourced fork count, build/test debt closures and a single green pushed-SHA run are all present in the codebase and, where I could re-run them, behave as claimed.

---

_Verified: 2026-10-12_
_Verifier: Claude (gsd-verifier)_
