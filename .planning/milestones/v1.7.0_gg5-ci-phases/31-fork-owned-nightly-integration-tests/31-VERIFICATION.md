---
phase: 31-fork-owned-nightly-integration-tests
verified: 2026-10-27T14:55:00Z
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
covered_digest: "v3:sha256:13f95f7739a47334b84a0b7ee543957d19445201e88d45a5b228fe025c5eb64f"
behavior_unverified: 0
overrides_applied: 0
deferred:
  - truth: "NIT-03: the nightly passes, or each failure is classified in a named skip list, on a manual dispatch before the schedule is enabled"
    addressed_in: "Phase 32"
    evidence: "REQUIREMENTS.md traceability `NIT-03 | Phase 32 | Complete`; moved by D-02 (E-16). Now met: GitHub run 40474535605 (workflow_dispatch, custom_build 3a3a15a9) concluded success at 2026-10-27T08:35Z; the only scheduled run 40477788530 (09:02Z) came after it and concluded skipped."
---

# Phase 31: Fork-Owned Nightly Integration Tests Verification Report

**Phase Goal:** The fork has a nightly integration-test signal it can trust. A fork-owned workflow runs the `integrationTest` task on ClickHouse `latest` x client V2 in the runner's own Docker, with no cloud job and no cloud secrets. Its first manual run happens after publication, in Phase 32 (NIT-03, moved by D-02). Upstream's `integrationTests.yaml` stays byte-identical to upstream and disabled, and a guard pins its bytes. No connector code changes.
**Verified:** 2026-10-27T14:55:00Z
**Status:** passed
**Re-verification:** Yes. The report was refreshed because it went stale (covered input `.planning/scripts/fork-anchors.txt` changed). The previous report (2026-10-26) was `passed` with no gaps.

## Why the previous report went stale

Since the last verification (`b93e2f05`), `.planning/scripts/fork-anchors.txt` changed in exactly one commit, `a6bfd78f` (Phase 32-01, CIG-02). That commit changed one line: `VERSION	v1.7.0_gg4	1	BLD-07` became `VERSION	v1.7.0_gg5	1	BLD-07`. None of the eight Phase 31 rows (lines 148-156: three on the `ConfluentPlatform.java` harness, five on the nightly) changed. **The change affects no Phase 31 claim.**

Since `b93e2f05`, the only non-`.planning` changes are `CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md` and `docs/ROLLOUT-v1.7.0_gg5.md`. No file under `.github/`, `src/` or the build changed (`git diff --stat e3407b12 HEAD -- .github src/integrationTest src/test/.../ci src/main build.gradle.kts` is empty).

## Goal Achievement

### Observable Truths (ROADMAP success criteria, amended)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | *[live, record]* A local `integrationTest` run on `latest` x V2 with Docker came before the workflow. The record has `SELECT version()`, every class that ran or skipped, a cause for each failure, and why upstream's standalone job fails. | VERIFIED | This is a record criterion. The records are in `31-COMMAND-EVIDENCE.md`. E-05 (cold run): server 26.10.3.7, 54 failures traced to one `harness` cause, a Java 11 worker loading Java 17 connector bytecode. E-18 (re-measured after 31-06): 55 tests, 54 passed, 0 failed, 1 skipped. Both precede workflow commit `3e2d8eb6`. UAT tests 5 and 16 passed on this record. The harness fix is in the current tree: `ConfluentPlatform.java` has `connectWorkerImage()` returning `new ImageFromDockerfile()` built on `confluentinc/cp-server-connect:` with the datagen plugin dir, and the old `new GenericContainer<>(CP_DATA_GEN)` count is 0. The three harness anchor rows evaluate 1/1, 1/1 and 0/0. |
| 2 | *[Docker-free]* The new workflow runs `latest` x V2 on `ubuntu-24.04`, with a timeout, top-level `permissions: contents: read`, `persist-credentials: false` and report uploads on failure. It passes CIG-01 with no guard edit, references no cloud secret, variable or job, and has triggers `workflow_dispatch` plus a schedule that cannot fire before the NIT-03 run. | VERIFIED | I read `.github/workflows/nightly-integration-tests.yaml` (unchanged since the last verification; same blob `0e0bcd68` on HEAD and on the published dispatch commit `3a3a15a9`). It has `permissions: contents: read`, `runs-on: ubuntu-24.04`, a job `timeout-minutes: 85` with a Gradle step of 75, and env `CLICKHOUSE_VERSION: latest` / `CLIENT_VERSION: V2`. It runs `./gradlew integrationTest` with no `--tests`, sets `persist-credentials: false`, and runs the summary step and both uploads with `if: always()`. All actions are pinned to 40-hex SHAs. It has no `secrets.`, and its only `vars.` is `NIGHTLY_ENABLED` in the job `if`. Its triggers are `schedule` (`17 3 * * *`) and `workflow_dispatch`. I re-ran the guards today (`testUnit --rerun`): `WorkflowActionPinGuardUnitTest` 21/21 and `NightlyWorkflowGuardUnitTest` 58/58 passed. |
| 3 | *[Docker-free, CI]* A Docker-free guard fails on a one-byte difference of `integrationTests.yaml` from upstream (identical at `v1.7.0` and `a6f1df83`), and was seen red on a one-byte edit. The Actions API reports the workflow disabled, with no scheduled run since 2026-10-16. | VERIFIED | `git rev-parse` gives the same blob `3e9b9be0...` at `HEAD`, `v1.7.0` and `a6f1df83`. The working file is 4812 bytes with sha256 `07dff3d0...5109`, equal to the guard's `EXPECTED_LENGTH` / `EXPECTED_SHA256`. `UpstreamIntegrationWorkflowBytesGuardUnitTest` passed 9/9 when I re-ran it, including 7 mutated-copy fixtures. The red-on-real-edit observation is recorded in E-10 and UAT test 8. A live API read today returned workflow 384641581 `state: disabled_manually`, `updated_at 2026-10-16T09:38:23Z`, and `total_count: 0` for runs created after 2026-10-16T10:00Z. |
| 4 | *[record]* The schedule is gated by the `NIGHTLY_ENABLED` variable, unset, so no scheduled run executes a job before the dispatch run. The dispatch moved to Phase 32 (D-02). Every failure of the measured run is skip-listed with its cause, or the run was green. | VERIFIED | The gate expression `github.event_name == 'workflow_dispatch' \|\| vars.NIGHTLY_ENABLED == 'true'` is in the workflow, pinned by anchor row 152 (1/1) and by the nightly guard. A live read is now possible because the workflow was published (workflow 397634804, `active`). It shows exactly two runs: dispatch run 40474535605 (`success`, 2026-10-27T08:35Z) and scheduled run 40477788530 (2026-10-27T09:02Z, after the dispatch, `skipped`). No scheduled run executed a job before the dispatch. The measured run E-18 was green and E-06b records `skip-entries: 0`; E-07 records that no skip list is needed. `gradle/integration-test-skip-list.tsv` is absent. The move is recorded in REQUIREMENTS.md and ROADMAP (D-02, E-16). The operator set `NIGHTLY_ENABLED` after the dispatch, per 32-07-SUMMARY, which is the intended D-01 sequence. |

**Score:** 4/4 truths verified (0 present, behavior-unverified)

**Constraint "No connector code changes":** `git diff --quiet v1.7.0_gg4 HEAD -- src/main` exits 0, and the working tree has no `src/main` change.

### Deferred Items

| # | Item | Addressed In | Evidence |
|---|------|-------------|----------|
| 1 | NIT-03: first manual dispatch of the nightly, green or failures skip-listed | Phase 32 (now complete) | REQUIREMENTS.md `NIT-03 \| Phase 32 \| Complete`. Run 40474535605 succeeded on `custom_build` 3a3a15a9 (63 tests, 0 failures, 9 skipped per 32-07-SUMMARY). The Phase 31 files in that commit are byte-identical to HEAD: blobs of the nightly, the summary script, `integrationTests.yaml` and `ConfluentPlatform.java` match. |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `.github/workflows/nightly-integration-tests.yaml` | gated, pinned nightly | VERIFIED | Read in full. Wired to the summary script; green on a hosted runner. |
| `.github/scripts/failed_tests_summary.py` | failed-test summary + annotation (D-10) | VERIFIED | Invoked by the workflow under `always()`. `FailedTestsSummaryScriptUnitTest` 8/8. |
| `src/test/.../ci/UpstreamIntegrationWorkflowBytesGuardUnitTest.java` | NIT-02 byte guard | VERIFIED | Length + SHA-256 over raw bytes of the real file; 9/9. |
| `src/test/.../ci/NightlyWorkflowGuardUnitTest.java` | NIT-01 shape guard | VERIFIED | Reads the real nightly; 58/58. |
| `src/test/.../ci/TestsWorkflowPinGuardUnitTest.java` | shared helper (constants widened to package-private only) | VERIFIED | 39/39. |
| `src/integrationTest/.../helper/ConfluentPlatform.java` | Java 17 Connect worker | VERIFIED | `ImageFromDockerfile` on `cp-server-connect`; anchors hold. |
| `.planning/scripts/fork-anchors.txt` | 5 nightly rows + 3 harness rows | VERIFIED | All 8 rows evaluated against current files: counts equal expected. The only post-phase edit is the unrelated BLD-07 VERSION row. |
| Skip list | only if a failure remained | N/A | Correctly absent (E-18 green, E-06b, E-07). |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| nightly workflow | `failed_tests_summary.py` | `python3 -I .github/scripts/failed_tests_summary.py build/test-results/integrationTest`, `if: always()` | WIRED |
| nightly workflow | `integrationTest` task | `./gradlew integrationTest` with `CLICKHOUSE_VERSION=latest`, `CLIENT_VERSION=V2` | WIRED (exercised by run 40474535605) |
| `NightlyWorkflowGuardUnitTest` | real nightly file | `Path.of(".github","workflows","nightly-integration-tests.yaml")` | WIRED |
| byte guard | real `integrationTests.yaml` | `Path.of(".github","workflows","integrationTests.yaml")`, raw bytes | WIRED |
| CIG-01 pin guard | nightly | auto-discovers `.github/workflows/*`; 21/21 unchanged | WIRED |

### Data-Flow Trace (Level 4)

| Artifact | Data | Source | Real data | Status |
|----------|------|--------|-----------|--------|
| step summary script | failed-test list | `TEST-*.xml` under `build/test-results/integrationTest` | Yes. It ran on the E-18 XMLs locally and in hosted run 40474535605. | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Five Docker-free guard classes | `./gradlew testUnit --rerun --tests` (Nightly, FailedTestsSummary, TestsWorkflowPin, UpstreamBytes, WorkflowActionPin) | exit 0; 135 tests, 0 failures, 0 errors, 0 skipped (XML timestamps 2026-10-27T14:53Z) | PASS |
| Phase 31 anchor rows | `grep -cF` for rows 148-156 | 1/1, 1/1, 0/0, 1/1, 1/1, 1/1, 0/0, 0/0 | PASS |
| Upstream workflow bytes | `git rev-parse {HEAD,v1.7.0,a6f1df83}:...`; `wc -c`; `shasum -a 256` | all `3e9b9be0...`; 4812; `07dff3d0...` | PASS |
| `src/main` untouched | `git diff --quiet v1.7.0_gg4 HEAD -- src/main` | exit 0 | PASS |
| Upstream workflow disabled | unauthenticated Actions API | `disabled_manually`, 0 runs after cut-off | PASS |
| Nightly gate on GitHub | Actions API runs list for the nightly | dispatch `success` 08:35Z; schedule `skipped` 09:02Z | PASS |

The full `integrationTest` task was not re-run locally. The hosted dispatch run on byte-identical Phase 31 files is stronger evidence for the same claim.

### Probe Execution

SKIPPED: the phase plans declare no `probe-*.sh`.

### Requirements Coverage

| Requirement | Source plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| NIT-01 | 31-01, 31-04, 31-05, 31-06 | fork-owned nightly runs `integrationTest` on `latest` x V2 in local Docker, no cloud job, SHA-pinned | SATISFIED | Truths 1, 2, 4 |
| NIT-02 | 31-03, 31-05 | upstream `integrationTests.yaml` byte-identical and disabled, guarded | SATISFIED | Truth 3 |
| NIT-03 | 31-01, 31-02, 31-05, 31-06 (frontmatter) | nightly passes or failures classified, on manual dispatch before schedule enabled | DEFERRED to Phase 32 (D-02) and now Complete there | Run 40474535605 success; REQUIREMENTS.md traceability |

REQUIREMENTS.md maps no other ID to Phase 31, so no requirement is orphaned.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| seven phase implementation files | - | `TBD`/`FIXME`/`XXX` | none found (grep exit 1) | none |
| `.github/scripts/failed_tests_summary.py` | - | WR-01: all-unreadable reports render "No failed tests." | WARNING (open, 31-REVIEW-DISPOSITION) | The Gradle step still fails the job. Not a must-have. |
| `.github/scripts/failed_tests_summary.py` | - | WR-02: Markdown escaping gaps | WARNING (open) | Cosmetic in the step summary. Workflow-command injection is closed (31-SECURITY). |
| `UpstreamIntegrationWorkflowBytesGuardUnitTest.java` | - | WR-03: false red possible on autocrlf Windows clones | WARNING (open) | Developer machines only. |
| `31-02-SUMMARY.md` | frontmatter | `requirements-completed: [NIT-03]` | INFO | Planning metadata only. NIT-03 is now in fact complete (Phase 32). |
| `gradlew.bat` (working tree, uncommitted) | all | CRLF/LF-only diff (`--ignore-cr-at-eol` diff is empty) | INFO | Not a Phase 31 file. Outside this phase; no bearing on any truth. |

31-SECURITY.md: `threats_open: 0` (SECURED). 31-UAT.md: 16 passed, 0 issues.

### Human Verification Required

None. UAT is complete (16/16), and the earlier residual risk, behavior on a hosted runner, is now settled by the green dispatch run.

### Gaps Summary

No gaps. The staleness came from one unrelated edit to `fork-anchors.txt` (the BLD-07 VERSION row, gg4 to gg5, Phase 32-01). It changed none of the eight Phase 31 rows, and all eight still evaluate to their expected counts. On the current tree all four amended success criteria hold. The nightly is gated, pinned and guarded. The upstream workflow's bytes still match the pin, and GitHub still reports it disabled. `src/main` is unchanged from `v1.7.0_gg4`. Since the last report, the live GitHub record also confirms that the gate behaved as designed: the dispatch ran first and succeeded, and the later scheduled run was skipped.

---

_Verified: 2026-10-27T14:55:00Z_
_Verifier: Claude (gsd-verifier)_
