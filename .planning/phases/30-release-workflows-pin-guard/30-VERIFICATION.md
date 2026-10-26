---
phase: 30-release-workflows-pin-guard
verified: 2026-10-26T12:00:00Z
status: passed
score: 4/4 must-haves verified
covered_files:
  - .github/dependabot.yml
  - .github/workflows/increment-version.yml
  - .github/workflows/pre-release.yaml
  - .github/workflows/release.yaml
  - .github/workflows/tests.yaml
  - .planning/phases/30-release-workflows-pin-guard/30-01-PLAN.md
  - .planning/phases/30-release-workflows-pin-guard/30-01-SUMMARY.md
  - .planning/phases/30-release-workflows-pin-guard/30-02-PLAN.md
  - .planning/phases/30-release-workflows-pin-guard/30-02-SUMMARY.md
  - .planning/phases/30-release-workflows-pin-guard/30-03-PLAN.md
  - .planning/phases/30-release-workflows-pin-guard/30-03-SUMMARY.md
  - docs/FORK-DELTA.md
  - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/WorkflowActionPinGuardUnitTest.java
covered_digest: "v3:sha256:1219de07c25a9f0ca23e6b5bce5d84ce473108927c4de7f219d5c1680a4a372b"
behavior_unverified: 0
overrides_applied: 0
advisory:
  - finding: "WR-01 (open in 30-REVIEW-DISPOSITION.md): release.yaml and pre-release.yaml share concurrency group release-publish with cancel-in-progress false; a third dispatch can cancel a queued (pending) real publish without a failure"
    category: other
    reason: "Real, confirmed in both workflow files (lines 22-24). It does not falsify any Phase 30 success criterion (none asks for queue semantics) and the operator sees a cancelled run. Fix is a dry-run-specific group or a header note; carry to a follow-up or Phase 32 decision."
    evidence_status: "code read; not exercised"
  - finding: "WR-02 (open): WorkflowActionPinGuardUnitTest pins only uses: lines; the custom_build-only publish condition, tag-absent and tests.yaml gates, permissions and runner pin of the two release workflows are not under test"
    category: other
    reason: "Outside the stated CIG-01 contract; the gates are present and were exercised by the dry runs (gate steps ran, Publish release skipped). Regression protection only."
    evidence_status: "code read"
---

# Phase 30: Release Workflows & Pin Guard Verification Report

**Phase Goal:** The fork's three release-path workflows run current, SHA-pinned actions only; pre-release.yaml and release.yaml build the Confluent archive with the project Gradle wrapper through setup-gradle (verifyConfluentArchive runs), create the release with `gh release create` attaching the archive; a dry run proves both paths without a release or tag; a Docker-free guard fails on any unpinned action in a fork-owned workflow; no connector code changes.
**Verified:** 2026-10-26
**Status:** passed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Docker-free guard reads every `.github/workflows` file except `integrationTests.yaml`, fails naming file:line on any non-40-hex-SHA-with-version-comment `uses:`, discovers files from the directory, each unpinned form seen red on a fixture, and seen red on the unchanged release workflows (CIG-01) | VERIFIED | `WorkflowActionPinGuardUnitTest.java` uses `Files.list(dir)`, `WORKFLOW_SUFFIXES` .yml/.yaml, single skip constant `UPSTREAM_IDENTICAL = "integrationTests.yaml"`, discovery fixture, 16-form parameterized reject fixture, `@Tag`-free (not `ClickHouseBase`). I re-ran `testUnit --rerun`: both pin-guard classes green, 21/0/0/0 and 39/0/0/0 (60 tests). Red-first state is recorded in 30-COMMAND-EVIDENCE E-02 (12 file:line tokens on the unchanged tree, plus the D-10 failure); I could not replay it (tree is now pinned) but the fixtures reject every form. |
| 2 | The three workflows pin every action by full SHA with `# vX.Y.Z`, pass the guard; neither release workflow has `gradle-build-action`, `gradle-version:` or `7.4.2`; both use `gradle/actions/setup-gradle` and `./gradlew createConfluentArchive` (REL-01, REL-02, REL-03) | VERIFIED | `grep -n uses:` shows every line is `@<40-hex> # vX.Y.Z`. grep for `gradle-version\|7.4.2\|gradle-build-action\|create-release\|upload-release-asset\|git tag\|git push` over release.yaml, pre-release.yaml, tests.yaml: no match. Both files hold `gradle/actions/setup-gradle@3f5f9ada... # v6.4.0` and `./gradlew createConfluentArchive`, then fail unless the log holds the exact line `> Task :verifyConfluentArchive`. Pins re-resolved read-only with `git ls-remote`: checkout v7.0.1 = 3d3c42e5, setup-java v6.0.1 = de7274f0, setup-node v7.1.0 = 949feb24, upload-artifact v7.0.2 = cf430e03, gradle/actions v6.4.0 peeled = 3f5f9ada. All match the workflows. |
| 3 | No `create-release`/`upload-release-asset`; both create the release with `gh release create` attaching `build/confluent/clickhouse-kafka-connect-<VERSION>.zip`; pre-release marks prerelease, release does not (REL-04) | VERIFIED | release.yaml: `gh release create "$RELEASE_VERSION" "$ARCHIVE" ... --notes ""` (no `--prerelease`); pre-release.yaml: same plus `--prerelease`. `ARCHIVE=build/confluent/clickhouse-kafka-connect-${version}.zip` set from `VERSION`. Dry-run check step asserts `isPrerelease` true for pre-release and false for release. No step tags or pushes from the runner. |
| 4 | Operator dry runs of both workflows on a scratch branch are green, build with wrapper Gradle, pass `verifyConfluentArchive`, exercise the attach step, publish nothing; origin has no new release and no new tag (REL-05) | VERIFIED | Independently read via unauthenticated GitHub REST: run 40296330498 (`Pre-release sync/v1.7.0 (dry run)`, `.github/workflows/pre-release.yaml`) and 40296481765 (`Release sync/v1.7.0 (dry run)`, `release.yaml`), both `workflow_dispatch`, head_sha b847fbdb, conclusion success. Job steps for both: build, archive, gate checks, Create draft, Check draft holds archive, Check no tag, Delete draft all success; `Publish release` skipped. `/releases?per_page=100` returns `[]`; `matching-refs/tags/dry-run` returns `[]`; origin tag count is 10, equal to the before-snapshot in E-08. Workflow, `src` and `.github` unchanged between b847fbdb and HEAD (empty `git diff --stat`), so the proven files are the delivered files. The sync/v1.7.0 branch is gone from origin per E-09 and the heads set. |

**Score:** 4/4 truths verified (0 present, behavior-unverified)

Additional goal clause "No connector code changes": `git diff --stat v1.7.0_gg4 HEAD -- src/main` is empty; non-`.planning` paths changed are exactly the three workflows, `dependabot.yml`, `docs/FORK-DELTA.md` and the two test classes.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/test/java/.../ci/WorkflowActionPinGuardUnitTest.java` | CIG-01 guard | VERIFIED | Substantive (fixtures + real-tree test + Dependabot D-10 test), picked up by `testUnit`, passes |
| `.github/workflows/release.yaml` | pinned, wrapper, gh release | VERIFIED | Read in full |
| `.github/workflows/pre-release.yaml` | same plus `--prerelease` | VERIFIED | Read in full |
| `.github/workflows/increment-version.yml` | pinned, Node 24, semver 7.8.5 `--ignore-scripts` | VERIFIED | Read in full; `vnull` defect on `vX.Y.Z_ggN` is recorded, not fixed (D-08, E-06) |
| `.github/dependabot.yml` | `exclude-paths` only `integrationTests.yaml` | VERIFIED | Read; D-10 guard test passes |
| `docs/FORK-DELTA.md` | corrected paragraph and Dependabot row | VERIFIED | In changed-path list; E-07 cross-check recorded |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| release/pre-release `Build and verify` | `verifyConfluentArchive` finalizer | `build.gradle.kts` line 452 `finalizedBy("verifyConfluentArchive")`, exact log-line assertion | WIRED |
| `ARCHIVE` env | `gh release create` | `$GITHUB_ENV` then positional asset argument | WIRED |
| guard | real workflows | `Files.list(.github/workflows)`; real-tree test green | WIRED |
| guard | `dependabot.yml` | D-10 test | WIRED |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Pin guard and sibling test pass | `./gradlew testUnit --tests '*WorkflowActionPinGuardUnitTest' --tests '*TestsWorkflowPinGuardUnitTest' --rerun` | 60 tests, 60 succeeded, 0 failed | PASS |
| Pins resolve upstream | `git ls-remote` on five actions | all equal workflow SHAs | PASS |
| Dry runs green, nothing published | unauthenticated REST (runs, jobs, releases, matching-refs) | as in truth 4 | PASS |

### Probe Execution

SKIPPED: no `probe-*.sh` declared by this phase.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| CIG-01 | 30-01 | guard fails on unpinned action in fork-owned workflow | SATISFIED | truth 1 |
| REL-01 | 30-01, 30-03 | pre-release builds with project wrapper, no 7.4.2 | SATISFIED | truth 2, dry run |
| REL-02 | 30-01, 30-02 | all three workflows SHA-pinned with version comment | SATISFIED | truth 2 |
| REL-03 | 30-01, 30-03 | gradle-build-action replaced by setup-gradle | SATISFIED | truth 2 |
| REL-04 | 30-01, 30-02, 30-03 | `gh release create` with archive attached | SATISFIED | truth 3 |
| REL-05 | 30-01, 30-03 | dry run proves both paths without publishing | SATISFIED | truth 4 |

All six IDs appear in the 30-01 PLAN frontmatter and in REQUIREMENTS.md; none orphaned. Bookkeeping note: REQUIREMENTS.md still shows REL-01, REL-03, REL-04, REL-05 as `[ ]`/`Pending` (CIG-01, REL-02 are ticked). Code satisfies them; the orchestrator's phase-complete step should tick them.

### Anti-Patterns Found

No `TBD`/`FIXME`/`XXX`/`TODO` introduced in the changed files (the `increment-version.yml` header documents the known `vnull` defect without a marker). No stubs.

### Deferred Items

None. `CIG-02` FORK-DELTA staleness items (default-branch parenthesis, "inert until the user changes both") are explicitly carried to Phase 32 per E-07.

### Advisory (open review findings, not blocking)

| # | Finding | Why Advisory |
|---|---------|--------------|
| 1 | WR-01: shared `release-publish` concurrency group can drop a queued real publish when a third run is queued | Confirmed in both files; no roadmap criterion covers queue semantics; user-visible as a cancelled run |
| 2 | WR-02: publish gates, permissions and runner pin of release workflows not pinned by a test | Outside the CIG-01 contract; gates were exercised by the dry runs |
| 3 | WR-03, IN-01..IN-04 | Comment accuracy, duplication, and gate-looseness; do not affect the goal |

### Human Verification Required

None. The operator step (dispatch) was already performed by the user, and I corroborated its outcome from the public REST API rather than relying on the relayed report. The real publish path (`Publish release`, non-draft) was not exercised, by design; the roadmap asks only for the dry run, and Phase 32 owns the publication decision.

### Gaps Summary

No gaps. All four ROADMAP success criteria hold in the codebase and on GitHub, all six requirement IDs are accounted for, and the diff against `v1.7.0_gg4` leaves `src/main` untouched.

---

_Verified: 2026-10-26_
_Verifier: Claude (gsd-verifier)_
