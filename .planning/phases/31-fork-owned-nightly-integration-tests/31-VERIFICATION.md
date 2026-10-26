---
phase: 31-fork-owned-nightly-integration-tests
verified: 2026-10-26T19:20:00Z
status: passed
score: 4/4 must-haves verified
covered_files:
  - .github/scripts/failed_tests_summary.py
  - .github/workflows/integrationTests.yaml
  - .github/workflows/nightly-integration-tests.yaml
  - .planning/phases/31-fork-owned-nightly-integration-tests/31-01-PLAN.md
  - .planning/phases/31-fork-owned-nightly-integration-tests/31-01-SUMMARY.md
  - .planning/phases/31-fork-owned-nightly-integration-tests/31-02-PLAN.md
  - .planning/phases/31-fork-owned-nightly-integration-tests/31-02-SUMMARY.md
  - .planning/phases/31-fork-owned-nightly-integration-tests/31-03-PLAN.md
  - .planning/phases/31-fork-owned-nightly-integration-tests/31-03-SUMMARY.md
  - .planning/phases/31-fork-owned-nightly-integration-tests/31-04-PLAN.md
  - .planning/phases/31-fork-owned-nightly-integration-tests/31-04-SUMMARY.md
  - .planning/phases/31-fork-owned-nightly-integration-tests/31-05-PLAN.md
  - .planning/phases/31-fork-owned-nightly-integration-tests/31-05-SUMMARY.md
  - .planning/phases/31-fork-owned-nightly-integration-tests/31-06-PLAN.md
  - .planning/phases/31-fork-owned-nightly-integration-tests/31-06-SUMMARY.md
  - .planning/scripts/fork-anchors.txt
  - src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java
  - src/test/java/com/clickhouse/kafka/connect/ci/FailedTestsSummaryScriptUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/NightlyWorkflowGuardUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/UpstreamIntegrationWorkflowBytesGuardUnitTest.java
covered_digest: "v3:sha256:836da0ae3dc6b3f54c9faeccdabd3e1ba337fc6d998136523158c92e8eaf8b9a"
behavior_unverified: 0
overrides_applied: 0
deferred:
  - truth: "NIT-03: the nightly passes, or each failure is classified in a named skip list, on a manual dispatch before the schedule is enabled"
    addressed_in: "Phase 32"
    evidence: "REQUIREMENTS.md maps NIT-03 to Phase 32; ROADMAP Phase 32 lists NIT-03 under Requirements and gains success criterion 5 (post-merge dispatch). Moved by the user's decision D-02 (31-COMMAND-EVIDENCE.md E-16, 2026-10-26)."
---

# Phase 31: Fork-Owned Nightly Integration Tests Verification Report

**Phase Goal:** The fork has a nightly integration-test signal it can trust. A fork-owned workflow runs `integrationTest` on ClickHouse `latest` x client V2 in the runner's own Docker, no cloud job, no cloud secrets. Its first manual run happens after publication, in Phase 32 (NIT-03, moved by D-02). Upstream's `integrationTests.yaml` stays byte-identical to upstream and disabled, and a guard pins its bytes. No connector code changes.
**Verified:** 2026-10-26
**Status:** passed
**Re-verification:** No, initial verification

NIT-03 (the dispatch run) is deferred to Phase 32 by the user's decision (E-16), so this phase is verified without a dispatch run. The amended ROADMAP criterion 4 is a `[record]` criterion.

## Goal Achievement

### Observable Truths (ROADMAP success criteria, amended)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Local `integrationTest` run on `latest` x V2 with Docker before the workflow; `SELECT version()`, every class ran/skipped, each failure caused, and why upstream's standalone job fails | VERIFIED | E-05 (cold run: server 26.10.3.7, 3 classes with counts, all 54 failures traced to one cause, Java 11 worker vs Java 17 connector bytecode, category `harness`, hypotheses 1-4 for run 38881153720 weighed). E-18 re-measurement after the 31-06 harness fix: 55 tests, 54 passed, 0 failed, 1 skipped (63 testcases in XML; `ExactlyOnceTest` 8 and `ClickHouseCloudTest` 1 skip via assumption, not fail). The record is in `31-COMMAND-EVIDENCE.md`, written before the workflow commit `3e2d8eb6`. Harness fix is read in `ConfluentPlatform.java` (`connectWorkerImage()` builds on `confluentinc/cp-server-connect:7.9.6` plus the datagen plugin dir); anchors on it hold (1/1/0). |
| 2 | New workflow: `latest` x V2, `ubuntu-24.04`, timeout, top-level `permissions: contents: read`, `persist-credentials: false`, uploads reports on failure, passes CIG-01 unchanged, no cloud secret/variable/job, triggers `workflow_dispatch` + a schedule that cannot fire before the NIT-03 run | VERIFIED | Read `.github/workflows/nightly-integration-tests.yaml` directly: `permissions: contents: read` top level, `runs-on: ubuntu-24.04`, job `timeout-minutes: 85`, Gradle step 75 (matches E-18 `step=75 job=85`), `CLICKHOUSE_VERSION: latest`, `CLIENT_VERSION: V2`, `./gradlew integrationTest` (whole task, no `--tests`), `persist-credentials: false`, both uploads and the summary step `if: always()`, all four actions SHA-pinned, no `secrets.`, no cloud job. The only `vars.` is `NIGHTLY_ENABLED` in the job `if` (`workflow_dispatch` OR `NIGHTLY_ENABLED == 'true'`). `WorkflowActionPinGuardUnitTest` (21) passes with no edit; `NightlyWorkflowGuardUnitTest` (58, 44 reject fixtures) passes; I re-ran both. `TestsWorkflowPinGuardUnitTest` was edited only to widen two `private` constants to package-private (guard rules not changed). |
| 3 | Docker-free guard fails on a one-byte difference of `integrationTests.yaml` from upstream (identical at `v1.7.0` and `a6f1df83`), seen red on a one-byte edit; Actions API reports it disabled, no scheduled run since 2026-10-16 | VERIFIED | Independent: `git rev-parse` of the file at `v1.7.0`, `a6f1df83`, `HEAD` all `3e9b9be0...`; working file 4812 bytes, sha256 `07dff3d0...` equal to the guard's `EXPECTED_*`. `UpstreamIntegrationWorkflowBytesGuardUnitTest` 9/9 green on re-run (7 mutated-copy fixtures reject, including CRLF and empty). Red-on-real-edit recorded in E-10 (first byte `n`->`N`, 1 failure, restored, green). Live GitHub read by me today: `state: disabled_manually`, `updated_at 2026-10-16T09:38:23Z`, `total_count: 0` for runs created after 2026-10-16T10:00Z. |
| 4 | [record] Schedule gated by `NIGHTLY_ENABLED` (unset) so no scheduled run executes a job before the dispatch; dispatch moved to Phase 32 (D-02); every failure of the measured run is skip-listed with cause, or the run was green | VERIFIED | Gate expression is in the workflow and pinned by an anchor row (1) and by the nightly guard. Nightly returns 404 on `custom_build` (contents API, today), so no scheduled or dispatched run exists. E-18 run was green, user disposition E-06b `skip-entries: 0`; E-07 records that no skip list is needed. `build.gradle.kts` is unchanged in the phase (no skip-list reader, no `gradle/integration-test-skip-list.tsv`). Move recorded in REQUIREMENTS.md (NIT-03 -> Phase 32), ROADMAP Phase 31 text and Phase 32 criterion 5, E-16. |

**Score:** 4/4 truths verified (0 present-behavior-unverified)

Constraint truth (CONTEXT, ROADMAP "No connector code changes"): `git diff v1.7.0_gg4 HEAD -- src/main` is empty (exit 0, re-run). Non-planning diff since the phase base `e4a74c89` is limited to the workflow, the script, `ConfluentPlatform.java` (harness, `src/integrationTest`) and four test classes. `integrationTests.yaml` is not in that diff. No existing assertion is weakened (harness change only swaps the worker image).

### Deferred Items

| # | Item | Addressed In | Evidence |
|---|------|-------------|----------|
| 1 | NIT-03: first manual dispatch of the nightly, green or failures skip-listed | Phase 32 | REQUIREMENTS.md traceability `NIT-03 | Phase 32`; ROADMAP Phase 32 `Requirements: CIG-02, CIG-03, NIT-03` and criterion 5; E-16 (user answer `move`, 2026-10-26). GitHub dispatches only from the default branch, and the nightly is absent from `custom_build` (404). |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `.github/workflows/nightly-integration-tests.yaml` | gated, pinned nightly | VERIFIED | 79 lines, read in full; wired to the summary script and the two guards |
| `.github/scripts/failed_tests_summary.py` | failed-test summary for `GITHUB_STEP_SUMMARY` + annotation (D-10) | VERIFIED | 181 lines; invoked by the workflow; ran it on the local E-18 results: `tests=63 failures=0 errors=0 skipped=9`, "No failed tests."; `FailedTestsSummaryScriptUnitTest` 8/8 |
| `src/test/.../ci/UpstreamIntegrationWorkflowBytesGuardUnitTest.java` | NIT-02 byte guard | VERIFIED | Substantive (length + SHA-256 over raw bytes), runs in `testUnit` (extends nothing) |
| `src/test/.../ci/NightlyWorkflowGuardUnitTest.java` | NIT-01 shape guard | VERIFIED | reads the real file (`NIGHTLY` path), 58 cases |
| `src/test/.../ci/FailedTestsSummaryScriptUnitTest.java` | script tests | VERIFIED | 8 cases |
| `ConfluentPlatform.java` (harness) | Java 17 Connect worker | VERIFIED | `ImageFromDockerfile` worker in use; old `new GenericContainer<>(CP_DATA_GEN)` absent |
| `.planning/scripts/fork-anchors.txt` | five NIT-01 nightly rows + three harness rows | VERIFIED | I re-evaluated all eight rows against the files: counts equal expected |
| Skip list (`gradle/integration-test-skip-list.tsv`, build reader) | only if a failure remained | N/A | Correctly absent: E-18 green, E-06b `skip-entries: 0`, E-07 |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| nightly workflow | `failed_tests_summary.py` | `python3 -I .github/scripts/failed_tests_summary.py build/test-results/integrationTest`, `if: always()` | WIRED |
| nightly workflow | `integrationTest` Gradle task | `./gradlew integrationTest` with `CLICKHOUSE_VERSION=latest`, `CLIENT_VERSION=V2` | WIRED |
| `NightlyWorkflowGuardUnitTest` | the real nightly file | `Path.of(".github","workflows","nightly-integration-tests.yaml")`, reuses `TestsWorkflowPinGuardUnitTest.nonCommentLines` | WIRED |
| byte guard | the real `integrationTests.yaml` | `Files.readAllBytes(UPSTREAM_WORKFLOW)` | WIRED |
| CIG-01 pin guard | nightly | auto-discovers `.github/workflows/*`; 21/21 green unchanged | WIRED |

### Data-Flow Trace (Level 4)

| Artifact | Data | Source | Real data | Status |
|----------|------|--------|-----------|--------|
| step summary script | failed-test list | `TEST-*.xml` under `build/test-results/integrationTest` | Yes (ran on the real E-18 XMLs, 63 testcases) | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Nightly guard, summary-script tests, pin guard, byte guard | `./gradlew testUnit --rerun --tests` on the four classes | 96 tests, 96 succeeded, 0 failed, 0 skipped | PASS |
| `src/main` untouched | `git diff --quiet v1.7.0_gg4 HEAD -- src/main` | exit 0 | PASS |
| Upstream workflow bytes | `git rev-parse {HEAD,v1.7.0,a6f1df83}:...integrationTests.yaml`; `shasum -a 256`; `wc -c` | all `3e9b9be0...`; `07dff3d0...`; 4812 | PASS |
| Anchor rows of the phase | `grep -cF` for the 8 Phase 31 rows | counts equal expected | PASS |
| Disabled state on GitHub | unauthenticated Actions API read | `disabled_manually`, 0 runs after cut-off | PASS |
| Post-merge gates reported by the orchestrator at HEAD `e3407b12` | `./gradlew assemble` exit 0; `./gradlew test` 1961 tests, 0 failures, 0 errors, 18 skipped | taken as reported, not re-run (full suite, per the single-run rule); only docs/planning files changed since the gated tree `f0f213bf` (empty non-`.planning` diff) | ACCEPTED |
| Cumulative anchors invariant `verify-merge-invariants.sh 54e63648 HEAD` | not completed by me (about 35 minutes; I stopped it) | E-15 records `result=PASS` (131 rows) and selftest `no failures` at `f0f213bf`; no non-`.planning` change since | ACCEPTED from E-15 |

### Probe Execution

SKIPPED: no `probe-*.sh` is declared by the phase plans.

### Requirements Coverage

| Requirement | Source plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| NIT-01 | 31-01, 31-04, 31-05, 31-06 | fork-owned nightly runs `integrationTest` on `latest` x V2 in local Docker, no cloud job, SHA-pinned | SATISFIED | Truths 1, 2, 4; workflow, guard, harness fix, E-18 |
| NIT-02 | 31-03, 31-05 | upstream `integrationTests.yaml` byte-identical and disabled, guarded | SATISFIED | Truth 3; byte guard, E-10, E-11, live API read |
| NIT-03 | 31-01, 31-02, 31-05, 31-06 (frontmatter) | nightly passes or failures classified, on manual dispatch before schedule enabled | DEFERRED to Phase 32 by user decision (E-16) | REQUIREMENTS.md maps it to Phase 32; ROADMAP Phase 32 carries it with criterion 5. Not a gap and not orphaned |

All three IDs from the plan frontmatter are accounted for. REQUIREMENTS.md maps no other ID to Phase 31, so there are no orphans. Four plan frontmatters still list NIT-03, which is the earlier mapping; the summary of 31-02 claims `requirements-completed: [NIT-03]`, which overstates (see Anti-Patterns/Notes).

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (modified files) | - | `TBD`/`FIXME`/`XXX` debt markers | none found in the seven touched implementation files | none |
| `.github/scripts/failed_tests_summary.py` | 128-161 | WR-01: unreadable-only reports render "No failed tests." with a success notice | WARNING (open, from 31-REVIEW.md) | Misleading summary only when the JVM is killed and every XML is truncated; the Gradle step itself still fails the job. Not a must-have |
| `.github/scripts/failed_tests_summary.py` | 39-46 | WR-02: Markdown escaping misses `<`, `&`, `[`, `*`, `\` | WARNING (open) | Failure messages with `<..>` may lose content in the step summary; the uploaded XML is intact |
| `UpstreamIntegrationWorkflowBytesGuardUnitTest.java` | - | WR-03: raw-byte check can fail on autocrlf Windows clones | WARNING (open) | Developer-machine false red only; CI is Linux. Three INFO items also open |
| `31-02-SUMMARY.md` | frontmatter | `requirements-completed: [NIT-03]` while NIT-03 is deferred | INFO | Planning-metadata inconsistency; the REQUIREMENTS.md traceability (Phase 32, Pending) is the authority |

None of the open review findings contradicts a must-have. Per the advisory classification they are recorded as warnings and do not change the status.

### Human Verification Required

None for this phase. The first dispatch on a GitHub runner (disk about 13.8 GB of images on a 14 GB SSD, x86, pull limits, cold wall time against the 75/85 minute timeouts) is NIT-03 and sits in Phase 32 by the user's decision; E-15 lists these CI-only risks for carrying forward.

### Gaps Summary

No gaps. The four amended success criteria hold in the codebase and on GitHub. The nightly is correct, gated, pinned and guarded; the NIT-02 pin matches upstream's bytes; the harness defect the measurement uncovered (a Java 11 Connect worker unable to load the Java 17 connector) was fixed in `src/integrationTest` only, with the full task green locally (54/54 connector tests); `src/main` is untouched. The only unproven item is behaviour on a hosted runner, which is explicitly outside this phase's scope.

Residual risk worth carrying to Phase 32 planning: the phase was never exercised on a GitHub runner, and E-15 notes that a red first dispatch would need a build or `.github/` change that Phase 32 is not allowed to make, so it would return to Phase 31 as a gap and re-gate the release tip.

---

_Verified: 2026-10-26_
_Verifier: Claude (gsd-verifier)_
