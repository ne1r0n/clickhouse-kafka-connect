---
phase: 39-dependabot-absorption-behaviour-pins
verified: 2026-11-01T21:30:00Z
status: passed
score: 5/5 must-haves verified
covered_files:
  - ".github/workflows/nightly-integration-tests.yaml"
  - ".github/workflows/pre-release.yaml"
  - ".github/workflows/release.yaml"
  - ".github/workflows/tests.yaml"
  - ".planning/phases/39-dependabot-absorption-behaviour-pins/39-01-PLAN.md"
  - ".planning/phases/39-dependabot-absorption-behaviour-pins/39-01-SUMMARY.md"
  - ".planning/phases/39-dependabot-absorption-behaviour-pins/39-02-PLAN.md"
  - ".planning/phases/39-dependabot-absorption-behaviour-pins/39-02-SUMMARY.md"
  - ".planning/phases/39-dependabot-absorption-behaviour-pins/39-03-PLAN.md"
  - ".planning/phases/39-dependabot-absorption-behaviour-pins/39-03-SUMMARY.md"
  - ".planning/phases/39-dependabot-absorption-behaviour-pins/39-04-PLAN.md"
  - ".planning/phases/39-dependabot-absorption-behaviour-pins/39-04-SUMMARY.md"
  - "docs/FORK-DELTA.md"
  - "docs/ROLLOUT-v1.7.0_gg6.md"
  - "gradle/libs.versions.toml"
  - "src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConnectorIntegrationTest.java"
  - "src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/WrappedDataExceptionClassificationPinUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java"
covered_digest: "v4:sha256:7367904b5815cd49f0835cbbac10417d5184e96a4b2e0142fd1388c887f0dfd0"
behavior_unverified: 0
overrides_applied: 0
---

# Phase 39: Dependabot Absorption & Behaviour Pins Verification Report

**Phase Goal:** The code phases start from a tree that already holds both open Dependabot updates, each in its own commit behind its own gate, so a later red is attributable to one change. The same tree pins the behaviour that later phases could disturb without noticing: how a wrapped `DataException` is classified through `ProxySinkTask.put`, and how client 0.11.0 forwards a `custom_*` key. The integration harness stops counting connectors that are not its own (F-3). `src/main` does not change.
**Verified:** 2026-11-01T21:30:00Z
**Status:** passed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths

| # | Truth (ROADMAP success criterion) | Status | Evidence |
|---|-----------------------------------|--------|----------|
| 1 | gson 2.15.0 is on `runtimeClasspath` from its own commit, which also updates the gson FORK-DELTA row; `testUnit` and the full suite on `latest` x V2 pass twice, the second with `--rerun`, 18 named skips (DEP-01) | VERIFIED | Merge `ac216401` (parents `4c463a87`, PR #22 head `b5cccbd3`) changes only `gradle/libs.versions.toml` (`gson = "2.14.0"` to `"2.15.0"`) and `docs/FORK-DELTA.md`. Re-run live: `dependencyInsight --configuration runtimeClasspath` prints `com.google.code.gson:gson:2.15.0`. E-03 records testUnit 1672/0/0/2 skipped twice and full suite 2270/0/0/18 skipped twice (skips helper: named=7, state_mismatch_cases=11, unexpected=0), the second of each with `--rerun`, on a tree equal to the merge. Cumulative invariants gate PASS (E-03). Not re-run here (Docker suites, per instructions). |
| 2 | Four workflows pin `setup-gradle@8da46b53... # v6.4.1` (commit, not tag object); release/pre-release mutual diff unchanged; `integrationTests.yaml` untouched; four pin-guard classes pass; FORK-DELTA row changes in the same commit (DEP-02) | VERIFIED | `grep` shows exactly one `setup-gradle@8da46b539cd8fe6ab27ca75128919a18d757604c # v6.4.1` line in each of `tests.yaml`, `nightly-integration-tests.yaml`, `release.yaml`, `pre-release.yaml`; no `3f5f9ada` or tag-object `7296c772` remains in `.github`. Merge `82202f7d` (second parent = PR #21 head `14be67ce`) changes those four workflows (1 line each) plus `docs/FORK-DELTA.md`. `git diff 72e8ccff HEAD -- .github` shows only the four setup-gradle pin lines. `diff release.yaml pre-release.yaml` hash on HEAD equals the hash on `72e8ccff` (`ac7dce11...`), so the mutual diff is unchanged. `integrationTests.yaml` has an empty diff against `72e8ccff`. Guard classes: 6 files, 184 tests, 0 failures under `testUnit --rerun` (E-04). Full `testUnit` re-run here passes. |
| 3 | With Docker unreachable, tests drive `ProxySinkTask.put` under `errors.tolerance=none` and `all` and assert today's outcome for the `RuntimeException(queryId, DataException)` wrapper, a retriable server code in the chain and a `SocketTimeoutException` root, on unchanged code (PIN-02) | VERIFIED | `WrappedDataExceptionClassificationPinUnitTest` read in full: one parameterized test, 14 cases (7 shapes x 2 tolerances) through the real `ProxySinkTask(config, dlq, writer).put`, using a `FailingDBWriter` subclass of `InMemoryDBWriter` and `InMemoryDLQ`, no mocking framework. Each case asserts a literal outcome (RETRIABLE, FAILS_TASK, DLQ, ESCAPES_RAW) and its details (DLQ size, wrapper class, `Number of records: N`, writer call count). Re-run here with `DOCKER_HOST=unix:///nonexistent/docker.sock`: 14 tests, 0 failures. `git diff --stat 8d9d1a17 HEAD -- src/main` is empty, so it passes on unchanged code. |
| 4 | A Docker-free test shows how client 0.11.0 forwards a `custom_*` key (doubled encoding); name and failure message call it a client-version tripwire (PIN-03) | VERIFIED | `customKeysAreForwardedWithDoubledEncodingClientVersionTripwire` in `ClientEndpointAndOptionsPinUnitTest` asserts `custom_settings=max_threads%253D3`, `custom_a=b%2520c`, `custom_myparam=7` against `StubClickHouseServer` on the 0.11.0 line. Both the `@DisplayName` and the `CLIENT_VERSION_TRIPWIRE` failure prefix say "client-version tripwire" and point at ROLLOUT row D7. Class re-run here: 5 tests, 0 failures. The change to the file is additive (no removed line). |
| 5 | One recorded `integrationTest` run shows the harness counting only the connector it started (F-3); the fix touches only `src/integrationTest` (PIN-04) | VERIFIED | `countFinishedDatagenTasks(connectorName)` reads only `/connectors/{name}/status`; `getConnectors()` (all connectors) is removed and has no remaining reference under `src/integrationTest`. New `datagenCompletionCountIgnoresLeftoverConnectorTest` keeps two finished connectors present and asserts each count equals its own task total. E-08 records it red against a scratch cross-connector count (`expected: <10> but was: <30>`) and green on the real code. E-09 records the `ClickHouseSinkConnectorIntegrationTest` class run on latest x V2: 55 tests, 0 failures, 0 skipped, pin passed; the JUnit XML is still in `build/test-results/integrationTest` (`tests="55"`). The diff outside `.planning`, docs and workflows is confined to `src/integrationTest` and the two `src/test` pin classes; `src/main` is empty. |

**Score:** 5/5 truths verified (0 present, behavior-unverified)

Note on F-3: the research (E-08 / orchestrator decision 1) found that `generateData` at the phase-start tree already counted by connector name. The phase deliverable is therefore the leftover-connector pin, removal of the all-connectors accessor, and a scoped completion helper, rather than a fix of a live defect. The goal ("stops counting connectors that are not its own") holds on the final tree and is pinned.

### Deferred Items

None.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `gradle/libs.versions.toml` | gson 2.15.0 only | VERIFIED | One-line catalog diff since phase start |
| `.github/workflows/{tests,nightly-integration-tests,release,pre-release}.yaml` | v6.4.1 peeled-commit pin | VERIFIED | One line each |
| `docs/FORK-DELTA.md` | gson row, tests.yaml row, Known cost 15 and 17 updated | VERIFIED | gson 2.15.0 and setup-gradle v6.4.1 changed inside the merge commits; Known costs 15 and 17 rewritten in the pin commits |
| `WrappedDataExceptionClassificationPinUnitTest.java` | PIN-02 matrix | VERIFIED | 222 lines, substantive, 14 passing cases |
| `ClientEndpointAndOptionsPinUnitTest.java` | PIN-03 tripwire | VERIFIED | additive, passing |
| `ConfluentPlatform.java`, `ClickHouseSinkConnectorIntegrationTest.java` | scoped Datagen counting and pin | VERIFIED | `compileIntegrationTestJava` re-run on HEAD: BUILD SUCCESSFUL |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| PR #22 head | `custom_build` lineage | `--no-ff` merge `ac216401` | WIRED | second parent is `b5cccbd3` |
| PR #21 head | `custom_build` lineage | `--no-ff` merge `82202f7d` | WIRED | second parent is `14be67ce` |
| PIN-02 test | `ProxySinkTask.put` | direct construction with injected writer and DLQ | WIRED | Real `Processing` and `Utils.handleException` run |
| `generateData` | `countFinishedDatagenTasks(name)` via `awaitDatagenCompletion` | per-connector status endpoint | WIRED | All 8 `generateData` calls in the recorded class run logged equal N of N |

### Data-Flow Trace (Level 4)

Not applicable: test, build and CI configuration only; no rendered dynamic data.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| gson resolved version | `./gradlew -q dependencyInsight --dependency com.google.code.gson:gson --configuration runtimeClasspath` | `gson:2.15.0` | PASS |
| Full Docker-free unit suite on HEAD | `DOCKER_HOST=unix:///nonexistent/docker.sock ./gradlew testUnit --rerun -PjavaVersion=21` | exit 0; 1687 tests, 0 failures, 0 errors, 2 skipped, 81 files | PASS |
| PIN-02 class | evidence-check `--class WrappedDataExceptionClassificationPinUnitTest` | 14 tests, 0 failures | PASS |
| PIN-03 class | evidence-check `--class ClientEndpointAndOptionsPinUnitTest` | 5 tests, 0 failures | PASS |
| Integration sources compile after review fixes WR-01 and WR-02 | `./gradlew compileIntegrationTestJava -PjavaVersion=21` | BUILD SUCCESSFUL | PASS |
| `src/main` untouched | `git diff --stat 8d9d1a17 HEAD -- src/main` | empty | PASS |

### Probe Execution

SKIPPED: the phase declares no probe scripts (`scripts/*/tests/probe-*.sh`). The merge-invariants gate (`.planning/scripts/verify-merge-invariants.sh`) was recorded PASS at three tips (E-03, E-05, E-11) and was not re-run (multi-minute, per instructions).

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| DEP-01 | 39-01 | gson 2.15.0 absorbed in own commit, gates, FORK-DELTA row | SATISFIED | Truth 1 |
| DEP-02 | 39-02 | setup-gradle 6.4.1 pinned by commit in four workflows; mutual diff unchanged; `integrationTests.yaml` untouched; guards green; FORK-DELTA rows | SATISFIED | Truth 2 (wording as user-decided) |
| PIN-02 | 39-03 | wrapped `DataException` pinned through `ProxySinkTask.put` under both tolerances, no code change | SATISFIED | Truth 3 |
| PIN-03 | 39-03 | `custom_*` forwarding pinned on 0.11.0 as a client-version tripwire | SATISFIED | Truth 4 |
| PIN-04 | 39-04 | F-3: harness counts only its own connector; one `integrationTest` run | SATISFIED | Truth 5 |

REQUIREMENTS.md maps exactly DEP-01, DEP-02, PIN-02, PIN-03, PIN-04 to Phase 39 and all five are marked complete; no orphaned IDs. PIN-01 is mapped to Phase 41, not this phase.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none) | - | No `TBD`, `FIXME`, `XXX`, `TODO` or `HACK` in added lines across `src`, `docs`, `.github`, `gradle` | - | - |

### Advisory Notes (non-blocking)

- `39-REVIEW-DISPOSITION.md` still records WR-01 and WR-02 as `open`, although `39-REVIEW-FIX.md` and commits `51200928` and `12379bf1` fixed them. The disposition record is stale and should be updated by the orchestrator. Review Infos IN-01 to IN-03 remain open by design.
- The E-09 integration run was made at `31b17214`, before WR-01 changed two log-only lines in `ClickHouseSinkConnectorIntegrationTest.java`. The edit adds `sinkTaskStatusForLog()`, which swallows `IOException`, and does not affect Datagen counting. Only compilation was re-checked here, as instructed.
- Existing `src/test/.../WorkflowActionPinGuardUnitTest` and `TestsWorkflowPinGuardUnitTest` keep `v6.4.0` strings as synthetic guard fixtures; they are not live pins.
- `.planning/config.json` and `.planning/state.json` are uncommitted orchestrator state and unrelated to the phase.

### Human Verification Required

None. The one live item (criterion 5) is backed by a recorded run whose XML is still present. The first GitHub run of setup-gradle 6.4.1 on the dispatch-only workflows is deliberately deferred to the Phase 44 rehearsal push, as the roadmap note allows.

### Gaps Summary

No gaps. All five roadmap success criteria hold in the codebase, `src/main` is byte-identical to the phase start, and the independently re-run Docker-free checks agree with the recorded evidence.

---

_Verified: 2026-11-01T21:30:00Z_
_Verifier: Claude (gsd-verifier)_
