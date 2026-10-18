---
phase: 21-dependabot-integration-ci-hygiene
verified: 2026-10-18T17:40:00Z
status: human_needed
score: 4/5 must-haves verified
covered_files:
  - ".planning/phases/21-dependabot-integration-ci-hygiene/21-01-PLAN.md"
  - ".planning/phases/21-dependabot-integration-ci-hygiene/21-01-SUMMARY.md"
  - ".planning/phases/21-dependabot-integration-ci-hygiene/21-02-PLAN.md"
  - ".planning/phases/21-dependabot-integration-ci-hygiene/21-02-SUMMARY.md"
  - ".planning/phases/21-dependabot-integration-ci-hygiene/21-03-PLAN.md"
  - ".planning/phases/21-dependabot-integration-ci-hygiene/21-03-SUMMARY.md"
  - ".planning/phases/21-dependabot-integration-ci-hygiene/21-04-PLAN.md"
  - ".planning/phases/21-dependabot-integration-ci-hygiene/21-04-SUMMARY.md"
  - ".planning/phases/21-dependabot-integration-ci-hygiene/21-05-PLAN.md"
  - ".planning/phases/21-dependabot-integration-ci-hygiene/21-05-SUMMARY.md"
  - ".planning/scripts/fork-anchor-exemptions.txt"
  - ".planning/scripts/fork-anchors.txt"
  - ".planning/scripts/verify-merge-invariants-selftest.sh"
  - ".planning/scripts/verify-merge-invariants.sh"
  - "build.gradle.kts"
  - "docs/FORK-DELTA.md"
  - "gradle/libs.versions.toml"
  - "src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java"
covered_digest: "v3:sha256:8437ca2731f6950034cc47cffd1d540fe721b7daad5a362f1bca7b270fe5a6eb"
behavior_unverified: 0
overrides_applied: 0
re_verification: false
human_verification:
  - test: "Open the repository's Insights > Dependency graph > Dependabot tab, find the `gradle` entry, and state in your own words the 'Last checked' time shown and whether any configuration error is shown for that entry."
    expected: "A 'Last checked' time within the last few days (the `gradle in /` job #1627525145 ran 2026-10-14T21:05:35Z and succeeded) and no configuration error on the `gradle` entry. The open-pull-request-limit banner seen in the earlier screenshot is not a configuration error."
    why_human: "ROADMAP criterion 5 requires the user's direct confirmation of the 'Last checked' time. E-02 recorded 'dependabot works' plus two screenshots, none of which shows a 'Last checked' time for the gradle entry. The GitHub REST API does not expose that field, and a relayed or inferred answer is not accepted (D-11)."
---

# Phase 21: Dependabot Integration & CI Hygiene Verification Report

**Phase Goal:** Every later phase builds and tests on the dependency versions already published on `custom_build`. The CI pin guard catches the two drift cases 17-REVIEW found. The GitHub-side loose ends left by `v1.7.0_gg2` are closed by the user. No connector behaviour changes.
**Verified:** 2026-10-18T17:40:00Z
**Status:** human_needed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| #   | Truth | Status | Evidence |
| --- | ----- | ------ | -------- |
| 1 | `sync/v1.7.0` contains the three Dependabot commits through a merge, with the build resolving lombok 1.18.48, spotless 8.10.4, slf4j-reload4j 2.0.20; spotless baseline-equal; `testUnit` green; no Lombok Ambiguous warning; anchors and FORK-DELTA rows name the new versions | VERIFIED | See sub-checks below. |
| 2 | `TestsWorkflowPinGuardUnitTest` fails if the fork-count step stops reading `reportTestForks` or if the task disappears; each seen red on a fixture | VERIFIED | Fixtures present and green at the tip; I mutated both real files in a throwaway worktree and the real-file test went red each time (see Behavioral Spot-Checks). Fixture red run is E-10. Robustness caveat under Warnings (WR-03). |
| 3 | Pin guard accepts `runs-on: ubuntu-24.04 # comment` and no longer reports step `name:`/`run:` text containing `: write` as a write scope; each seen red then green on a fixture | VERIFIED | `RUNS_ON` strips a whitespace-preceded comment; `WRITE_SCALAR` excludes `name:` and `run:` keys. The 26-test class is green at the tip (re-run by me). Old-regex red run recorded in E-10 (2 and 6 failures on the named fixtures). |
| 4 | User disabled nightly `integrationTests.yaml`, date recorded; file byte-identical to upstream `v1.7.0`; no `schedule` run created after the disable once a cron time passed | VERIFIED | Re-queried the REST API myself (see below). |
| 5 | User confirmed directly, in their own words, that the Dependabot `gradle` entry shows a recent "Last checked" with no configuration error, and that the read-only token is revoked; Keychain lookup finds nothing | PARTIAL, needs human | Token and Keychain half verified. The "Last checked" half is `unmeasured` in E-02 and stays unmeasured. |

**Score:** 4/5 truths verified (0 present, behavior-unverified). Truth 5 is routed to human verification, not upgraded.

#### Truth 1 sub-checks (independently re-run)

| Check | Result |
| ----- | ------ |
| `git merge-base --is-ancestor 6473e24f HEAD` | succeeds |
| Merge shape | `82524e12` has parents `54d19dc5` and `6473e24f`; first-parent merge, no rebase. `git status` shows no push (the orchestrator confirms nothing is pushed by design). |
| Catalog and build | `gradle/libs.versions.toml`: `lombok = "1.18.48"`, `spotless = "8.10.4"`. `build.gradle.kts:106`: `slf4j-reload4j:2.0.20`. `dependencyInsight` resolves `org.slf4j:slf4j-reload4j:2.0.20` (runtimeClasspath) and `org.projectlombok:lombok:1.18.48` (annotationProcessor). |
| Lombok warning | Clean `compileJava`: exit 0, 0 lines matching "ambiguous". The `lombok.config` is unchanged. |
| Spotless baseline-equal | `spotlessApply` in a throwaway worktree at the tip: diff md5 `3393873370f9b48d2239131eedba8ff8`, 46 files, 260 insertions, 337 deletions. These are the same md5 and counts as the 8.10.1 baseline in E-07. None of the files Phase 21 edited is in the diff, so they are spotless-clean. No tree-wide reformat commit exists. |
| `testUnit` green | Orchestrator's post-merge `./gradlew test`: run 2 BUILD SUCCESSFUL, 1009 tests, 0 failures, 0 errors, 18 skipped. Run 1 hit the known intermittent OpenJ9 `retransformClasses` crash (Phase 18 deferred item). E-12 `testUnit`: 577 tests, 0 failed, 2 skipped. I re-ran the 26-test pin-guard class (green). I did not re-run the full suite. |
| Anchors and FORK-DELTA rows | `fork-anchors.txt:43` reads `build.gradle.kts slf4j-reload4j:2.0.20 1 BLD-08`. FORK-DELTA lines 63 and 65 name `slf4j-reload4j:2.0.20` and `Lombok 1.18.48`. |
| Merge-invariants gate | `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` exits 0: `anchor_commits=396 ... anchor_exempt=1 result=PASS`. The single exemption is `.planning/scripts/fork-anchor-exemptions.txt`, a full-SHA-, path- and pattern-pinned row for `6473e24f`. |
| Self-test | `verify-merge-invariants-selftest.sh` PASS per E-12 (16 PASS, 0 FAIL). E-09 records the fail-first runs: the old gate fails 7 cases and a SHA-only mutant fails 2. I did not re-run the self-test myself. |

#### Truth 4 sub-checks (independently re-run, GitHub `date:` Sun, 18 Oct 2026 17:25:50 GMT)

- Workflow state `disabled_manually`, `updated_at` 2026-10-16T09:38:23Z. The user confirmed that instant ("OPS-01: time is right"), so the disable date is recorded.
- `schedule` runs of `integrationTests.yaml` created after 2026-10-16T09:38:23Z: **0**.
- Control: the same query since 2026-10-01 returns 2 runs, created 2026-10-15T09:39:18Z and 2026-10-16T09:32:28Z. The cron fires around 09:3x UTC, and the 2026-10-17 and 2026-10-18 fires have passed with no run, so the zero is a real observation.
- `git diff --quiet v1.7.0 HEAD -- .github/workflows/integrationTests.yaml` succeeds (byte-identical).

#### Truth 5 sub-checks

- Token revoked: the user's verbatim answer "token've been deleted" (E-02). `security find-generic-password -s gsd17-gh-actions-read` exits 44 (not found), which I re-ran.
- "Last checked": the user answered "dependabot works" and sent two screenshots. By the orchestrator's own annotation in E-02, neither shows a per-manifest "Last checked" time. The Dependabot tab shows an open-PR-limit banner. The only objective trace is the `gradle in /` Dependabot job #1627525145, which succeeded on 2026-10-14T21:05:35Z. That is circumstantial evidence, not the user's direct confirmation, and no Dependabot run has been created since then. The criterion asks for a statement from the user, so I did not infer one.

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | -------- | ------ | ------- |
| `gradle/libs.versions.toml` | lombok 1.18.48, spotless 8.10.4 | VERIFIED | Lines 9 and 31 |
| `build.gradle.kts` | `slf4j-reload4j:2.0.20`, `reportTestForks` task | VERIFIED | Lines 106 and 255-262; only this one-line change vs. the merge parent plus the Dependabot lines |
| `.planning/scripts/fork-anchors.txt` | BLD-08 row on the new version | VERIFIED | Line 43 |
| `.planning/scripts/fork-anchor-exemptions.txt` | One SHA- and row-pinned row for `6473e24f` | VERIFIED | Full 40-hex SHA, tab-separated, with reason |
| `.planning/scripts/verify-merge-invariants.sh` and `-selftest.sh` | Exemption lookup plus fail-first cases ST5-ST9 | VERIFIED | Gate PASS with the exemption (re-run), FAIL without it per E-08 |
| `TestsWorkflowPinGuardUnitTest.java` | Rules extracted, fixtures, CI-18 and CI-19 rules, D-09 guard | VERIFIED | 848 lines, 26 tests, all green (re-run) |
| `docs/FORK-DELTA.md` BLD-08 and BLD-14 rows | New versions | VERIFIED | Lines 63 and 65 |
| `21-EVIDENCE.md` | E-01..E-13 | VERIFIED | Present. Cited facts matched my independent re-runs. |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | -- | --- | ------ | ------- |
| `TestsWorkflowPinGuardUnitTest` | `.github/workflows/tests.yaml` "Report test fork count" step | `forkCountStepProblem(nonCommentLines(WORKFLOW))` | WIRED | Renaming the called task in `tests.yaml` turned the real-file test red |
| `TestsWorkflowPinGuardUnitTest` | `build.gradle.kts` `reportTestForks` | `forkTaskProblem(ktsCode(...))` | WIRED | Renaming the task registration turned the real-file test red |
| `TestsWorkflowPinGuardUnitTest` | `.github/dependabot.yml` exclude-paths | `dependabotExcludeProblem` against the real file | WIRED | Real-file test green; the four paths are listed under the github-actions entry |
| `verify-merge-invariants.sh` | `fork-anchor-exemptions.txt` | `grep -qxF` on `sha<TAB>path<TAB>pattern` | WIRED | `anchor_exempt=1` with the file, `anchor_exempt=0` and FAIL when disabled (E-08) |

### Data-Flow Trace (Level 4)

Not applicable. The phase changes build config, CI test code, a shell gate and docs. It renders no dynamic data.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| Pin guard green at tip | `./gradlew --offline testUnit --tests '*TestsWorkflowPinGuardUnitTest' --rerun` | 26 tests, 26 succeeded, 0 failed | PASS |
| CI-18: fork-count step stops reading `reportTestForks` | Throwaway worktree: `sed` renames the call in `tests.yaml`, then the same test run | 1 failure: "Step 'Report test fork count' ... must call './gradlew ... reportTestForks'" | PASS (fails as required) |
| CI-18: task disappears from `build.gradle.kts` | Throwaway worktree: `sed` renames the registration, then the same test run | 1 failure: "tasks.register(\"reportTestForks\") not found in build.gradle.kts outside comments" | PASS (fails as required) |
| Merge-invariants gate | `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` | exit 0, `result=PASS`, `anchor_exempt=1` | PASS |
| Dependency resolution | `dependencyInsight` slf4j-reload4j and lombok | 2.0.20 and 1.18.48 | PASS |
| Clean compile Lombok warning | `clean compileJava`, grep "ambiguous" | exit 0, 0 matches | PASS |
| Spotless baseline-equal | `spotlessApply` in a worktree at the tip | Same md5 and 46 files as the 8.10.1 baseline | PASS |

All throwaway worktrees were removed (`git worktree list` shows only the main tree).

### Probe Execution

No probes are declared by the Phase 21 plans. SKIPPED. The merge-invariants gate and its self-test play that role and are covered above.

### Requirements Coverage

Every ID from the phase and the PLAN frontmatter is accounted for in REQUIREMENTS.md. There are no orphaned requirements.

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ----------- | ----------- | ------ | -------- |
| DEPS-01 | 21-02, 21-03, 21-05 | Merge of the three Dependabot commits, spotless baseline-equal, `testUnit` green | SATISFIED | Truth 1 |
| CI-18 | 21-04 | Pin guard fails on fork-step or task loss | SATISFIED | Truth 2, with the WR-03 robustness caveat |
| CI-19 | 21-04 | runs-on trailing comment accepted, step text not a write scope | SATISFIED | Truth 3 |
| OPS-01 | 21-01, 21-05 | Nightly `integrationTests.yaml` disabled, no scheduled run since | SATISFIED | Truth 4 |
| OPS-02 | 21-01 | Dependabot `gradle` entry shows a recent "Last checked", no config error | NEEDS HUMAN | Truth 5. REQUIREMENTS.md correctly leaves the box unchecked and the traceability row Pending. |
| OPS-03 | 21-01 | Token revoked, Keychain item deleted | SATISFIED | User's verbatim answer plus Keychain exit 44 (re-run) |

### Scope: "No connector behaviour changes"

VERIFIED. `git diff --name-only 82524e12^1 HEAD` outside `.planning/` is exactly four paths: `build.gradle.kts`, `docs/FORK-DELTA.md`, `gradle/libs.versions.toml` and `TestsWorkflowPinGuardUnitTest.java`. `src/main`, `src/integrationTest`, `src/testFixtures`, `.github`, `lombok.config` and `VERSION` are unchanged since the merge. No file under `src/main` changed relative to the merge parent. The only runtime-relevant delta is the slf4j-reload4j patch bump and the lombok and spotless bumps that Dependabot already published.

### Anti-Patterns Found

Files modified by Phase 21 were scanned for `TBD|FIXME|XXX`.

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| (none) | - | Debt markers in phase-modified files | - | No blocker |

### Code Review Findings (21-REVIEW.md: 0 critical, 3 warning, 4 info, all `open`)

None of these makes a ROADMAP criterion false, so none is a gap. They are carried as warnings. They are recorded `open` in `21-REVIEW-DISPOSITION.md`, which is the intended state until triage. I spot-read the code behind WR-01 through WR-03 and confirmed each hole exists.

| ID | Warning | Impact on the goal |
| -- | ------- | ------------------ |
| WR-01 | `runsOnProblem` rejects `runs-on: ubuntu-24.04 # was ubuntu-latest`, because the floating-label scan runs on lines that still carry the trailing comment | False failure on a legitimate comment, which the criterion-3 fix does not cover. The `# comment` case in the criterion passes. |
| WR-02 | `ktsCode` strips block comments over the whole text before line comments, so a `/*` inside a string or `//` comment can hide the live `reportTestForks` registration | Fragility. The current `build.gradle.kts` passes. |
| WR-03 | `forkTaskProblem` and the fork-step rule are substring-based (`println(6) // tasks.named<Test>("test").maxParallelForks`, or `x=1 # ./gradlew reportTestForks`, would satisfy them) | The guard can be defeated by an unusual edit, though the two cases the criterion names (step stops calling the task, task disappears) are caught on the real files. |
| IN-03 | FORK-DELTA BLD-14 cites Phase 17 E-07 (gathered on Lombok 1.18.46) for 1.18.48. The "javap diff" claim is not re-established. | Doc accuracy. This phase's clean compile and 0-warning check support the warning half only. |

### Notes for the Orchestrator

- The `./gradlew test` run 1 crash (OpenJ9 GPF in `retransformClasses` under `Mockito.spy` in `ClickHouseWriterTest`) is the known intermittent Phase 18 deferred item (closed by HARN-08 in Phase 22). Run 2 was green. I counted it as environmental, not as a Phase 21 regression, and did not re-run the full suite.
- Nothing is pushed by design. The merged tree's first CI run belongs to Phase 24.
- Hand-off to Phase 24 (from E-08): the "mechanical checks" paragraph of `docs/FORK-DELTA.md` should name `fork-anchor-exemptions.txt` beside `fork-anchors.txt`.

### Human Verification Required

#### 1. Dependabot `gradle` entry: "Last checked" and configuration error (OPS-02, ROADMAP criterion 5)

**Test:** In the GitHub web UI, open Insights > Dependency graph > Dependabot, find the `gradle` entry, and tell the orchestrator, in your own words, the "Last checked" time shown and whether any configuration error shows for that entry.
**Expected:** A recent "Last checked" time and no configuration error on the `gradle` entry. The open-PR-limit banner is not a configuration error.
**Why human:** The criterion needs your direct confirmation. The earlier answer ("dependabot works") and the two screenshots carried no "Last checked" time. The REST API does not expose it, and the only objective trace is a successful `gradle in /` job on 2026-10-14. After you answer, record it in E-02 and tick OPS-02 in REQUIREMENTS.md.

### Gaps Summary

There are no failed truths and no blockers. Four of five ROADMAP criteria are verified against the repository and live GitHub state, with several checks re-run independently rather than read from the SUMMARY or EVIDENCE files. The one open item is the first half of criterion 5, the user's direct statement about the Dependabot "Last checked" time. It stays human-needed and is not upgraded to passed. Three review warnings on pin-guard robustness remain open for later triage and do not block the goal.

---

_Verified: 2026-10-18T17:40:00Z_
_Verifier: Claude (gsd-verifier)_
