---
phase: 30-release-workflows-pin-guard
verified: 2026-10-27T13:30:00Z
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
covered_digest: "v3:sha256:cdd598b35210d4156ecc7321850f35b3d64ac585c6c7027215bbce6d5fe1c679"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: passed
  previous_score: 4/4
  gaps_closed: []
  gaps_remaining: []
  regressions: []
advisory:
  - finding: "WR-01 (open in 30-REVIEW-DISPOSITION.md): release.yaml and pre-release.yaml share concurrency group release-publish with cancel-in-progress false; a third dispatch can cancel a queued (pending) real publish without a failure"
    category: other
    reason: "Real, confirmed in both workflow files. It does not falsify any Phase 30 success criterion (none asks for queue semantics) and the operator sees a cancelled run. Fix is a dry-run-specific group or a header note."
    evidence_status: "code read; not exercised"
  - finding: "WR-02 (open): WorkflowActionPinGuardUnitTest pins only uses: lines; the custom_build-only publish condition, tag-absent and tests.yaml gates, permissions and runner pin of the two release workflows are not under test"
    category: other
    reason: "Outside the stated CIG-01 contract; the gates are present and were exercised by the dry runs (gate steps ran, Publish release skipped). Regression protection only."
    evidence_status: "code read"
---

# Phase 30: Release Workflows & Pin Guard Verification Report

**Phase Goal:** The fork's three release-path workflows run current, SHA-pinned actions only; pre-release.yaml and release.yaml build the Confluent archive with the project Gradle wrapper through setup-gradle (verifyConfluentArchive runs), create the release with `gh release create` attaching the archive; a dry run proves both paths without a release or tag; a Docker-free guard fails on any unpinned action in a fork-owned workflow; no connector code changes.
**Verified:** 2026-10-27
**Status:** passed
**Re-verification:** Yes. The earlier report (2026-10-26, passed) went stale because Phase 32 commits touched `docs/FORK-DELTA.md` and `TestsWorkflowPinGuardUnitTest.java`. Every truth was re-checked against the current tree; the verdict is unchanged.

## What changed since the previous report

`git diff e4a74c89 HEAD` (the commit that wrote the previous report) over the covered inputs:

| Covered file | Change | Effect on Phase 30 |
|---|---|---|
| `.github/workflows/release.yaml`, `pre-release.yaml`, `increment-version.yml`, `.github/dependabot.yml` | none | none |
| `tests.yaml` | none | none |
| `WorkflowActionPinGuardUnitTest.java` | none | none |
| `TestsWorkflowPinGuardUnitTest.java` | two `private static final Pattern` fields (`CONTINUE_ON_ERROR`, `TRAILING_COMMENT`) widened to package-private so the Phase 31 `NightlyWorkflowGuardUnitTest` can reuse them | none; test still passes (39/39) |
| `docs/FORK-DELTA.md` | Phase 32 added gg5 identity, workflow grouping, nightly rows | The Phase 30 rows are still present: release/pre-release rewrite, `increment-version.yml` hardening, `WorkflowActionPinGuardUnitTest` row, and the Dependabot row saying `exclude-paths` leaves out only `integrationTests.yaml` |

Other files outside the covered set that Phase 31/32 added (`nightly-integration-tests.yaml`, `NightlyWorkflowGuardUnitTest`, etc.) are not Phase 30 deliverables. The pin guard discovers workflows from the directory, so it now also covers the nightly workflow and still passes.

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Docker-free guard reads every `.github/workflows` file except `integrationTests.yaml`, fails naming file:line on any unpinned `uses:`, discovers files from the directory, each unpinned form seen red on a fixture, seen red on the unchanged release workflows (CIG-01) | VERIFIED | `./gradlew test --tests "*WorkflowActionPinGuardUnitTest*"`: 21 tests, 21 succeeded, 0 failed. Guard is not a `ClickHouseBase` test (no Docker). Red-first state on the unchanged tree is recorded in 30-COMMAND-EVIDENCE E-02; it cannot be replayed now that the tree is pinned, but the 16-form reject fixture suite is green and the guard now also reads the Phase 31 nightly workflow without edits (directory discovery demonstrated by use). |
| 2 | `release.yaml`, `pre-release.yaml`, `increment-version.yml` pin every action by full SHA with `# vX.Y.Z`; neither release workflow has `gradle-build-action`, `gradle-version:` or `7.4.2`; both use `setup-gradle` and `./gradlew createConfluentArchive` (REL-01, REL-02, REL-03) | VERIFIED | `grep -n uses:` over the three files: every line is `@<40-hex> # vX.Y.Z` (checkout `3d3c42e5` v7.0.1, setup-java `de7274f0` v6.0.1, setup-gradle `3f5f9ada` v6.4.0, setup-node `949feb24` v7.1.0). `grep -E "gradle-version\|7\.4\.2\|gradle-build-action\|create-release\|upload-release-asset"` over release.yaml, pre-release.yaml, tests.yaml, increment-version.yml: no match. Both release files hold `./gradlew createConfluentArchive --console=plain | tee` then fail unless the log holds the exact line `> Task :verifyConfluentArchive` (lines 70-72). The only `uses:` lines that are not pinned live in `integrationTests.yaml`, the upstream-identical exclusion. |
| 3 | No `create-release`/`upload-release-asset`; both create the release with `gh release create` attaching `build/confluent/clickhouse-kafka-connect-<VERSION>.zip`; pre-release marks prerelease, release does not (REL-04) | VERIFIED | release.yaml:121 `gh release create "$RELEASE_VERSION" "$ARCHIVE" --target "$GITHUB_SHA" --title ... --notes ""` (no `--prerelease`); pre-release.yaml:121 same with `--prerelease`. `ARCHIVE=build/confluent/clickhouse-kafka-connect-${version}.zip` (line 64 in both). Dry-run step (line 128) uses `--draft` and the notice at line 145 asserts `prerelease=false` in release.yaml, `prerelease=true` in pre-release.yaml. |
| 4 | Operator dry runs of both workflows on a scratch branch are green, build with wrapper Gradle, pass `verifyConfluentArchive`, exercise the attach step, publish nothing; origin has no new release and no new tag (REL-05) | VERIFIED | Runs 40296330498 (pre-release) and 40296481765 (release) at head b847fbdb, recorded and independently read via unauthenticated REST in the previous report; the workflow files they ran are byte-unchanged since (empty `git diff` for the three files over `e4a74c89..HEAD`). 30-UAT.md tests 1 and 2 (8/8 passed, 0 issues) record the same outcome. Origin now lists 12 tags, the two extra relative to the dry-run snapshot (10) being `v1.7.0_gg5` and `pre-v1.7.0_gg5`, created by the later gg5 publication in Phase 32, not by the dry runs. No `dry-run` tag exists. The `sync/v1.7.0` branch is not on origin. |

**Score:** 4/4 truths verified (0 present, behavior-unverified)

Goal clause "No connector code changes": `git diff --stat v1.7.0_gg4 HEAD -- src/main` is empty. Non-`.planning` paths changed since `v1.7.0_gg4` are workflows, `dependabot.yml`, `CHANGELOG.md`, `VERSION`, docs, `.github/scripts/failed_tests_summary.py`, a `src/integrationTest` helper and CI test classes: Phase 30's own subset (three workflows, `dependabot.yml`, `FORK-DELTA.md`, the two pin-guard test classes) is as before, and the rest belongs to Phases 31 and 32.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/test/java/.../ci/WorkflowActionPinGuardUnitTest.java` | CIG-01 guard | VERIFIED | Substantive; passes 21/21 |
| `src/test/java/.../ci/TestsWorkflowPinGuardUnitTest.java` | sibling guard for `tests.yaml` and Dependabot `exclude-paths` | VERIFIED | Passes 39/39 after the visibility change |
| `.github/workflows/release.yaml` | pinned, wrapper, gh release | VERIFIED | Greps above; unchanged since previous full read |
| `.github/workflows/pre-release.yaml` | same plus `--prerelease` | VERIFIED | Greps above |
| `.github/workflows/increment-version.yml` | pinned, Node 24, semver `--ignore-scripts` | VERIFIED | Pins confirmed; `vnull` defect on `vX.Y.Z_ggN` recorded, not fixed (D-08, E-06), and described in FORK-DELTA |
| `.github/dependabot.yml` | `exclude-paths` only `integrationTests.yaml` | VERIFIED | Lines 22-23 |
| `docs/FORK-DELTA.md` | Phase 30 workflow paragraph and Dependabot row | VERIFIED | Rows present in the current file (per diff); matches the workflows |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| release/pre-release `Build and verify` | `verifyConfluentArchive` finalizer | `build.gradle.kts` `finalizedBy("verifyConfluentArchive")`, exact log-line assertion in workflow | WIRED |
| `ARCHIVE` env | `gh release create` | `$GITHUB_ENV` then positional asset argument | WIRED |
| guard | real workflows | `Files.list(.github/workflows)`; real-tree test green, nightly workflow covered without guard edits | WIRED |
| guard | `dependabot.yml` | D-10 test in `WorkflowActionPinGuardUnitTest` | WIRED |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Requested: sibling pin guard passes | `./gradlew test --tests "*TestsWorkflowPinGuardUnitTest*" --rerun-tasks` | 39 tests, 39 succeeded, 0 failed, 0 skipped; BUILD SUCCESSFUL (no Docker needed) | PASS |
| Pin guard passes | `./gradlew test --tests "*WorkflowActionPinGuardUnitTest*"` | 21 tests, 21 succeeded, 0 failed, 0 skipped; BUILD SUCCESSFUL | PASS |
| No unpinned, tag-pinned or forbidden actions in fork-owned workflows | grep as in truth 2 | no hits outside `integrationTests.yaml` | PASS |
| `src/main` untouched | `git diff --stat v1.7.0_gg4 HEAD -- src/main` | empty | PASS |

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

All six IDs appear in the plan frontmatter and in REQUIREMENTS.md, now ticked `[x]` and marked Complete in the traceability table (lines 11-15, 25, 50-58); none orphaned.

### Anti-Patterns Found

None blocking. No `TBD`/`FIXME`/`XXX` introduced in the Phase 30 files. The `git push` hit in `increment-version.yml` is upstream's own "Commit and push" step (it pushes an `update-version-*` branch for a PR, not a tag or release) and is outside the release workflows' forbidden-term scope.

### Deferred Items

None. `CIG-02` FORK-DELTA staleness items were carried to Phase 32 per E-07 and have since been worked there.

### Advisory (open review findings, not blocking)

| # | Finding | Why Advisory |
|---|---------|--------------|
| 1 | WR-01: shared `release-publish` concurrency group can drop a queued real publish when a third run is queued | Confirmed in both files; no roadmap criterion covers queue semantics |
| 2 | WR-02: publish gates, permissions and runner pin of release workflows not pinned by a test | Outside the CIG-01 contract; gates were exercised by the dry runs |
| 3 | WR-03, IN-01..IN-04 | Comment accuracy, duplication, gate looseness; do not affect the goal |

### Human Verification Required

None. This is consistent with 30-UAT.md: status complete, 8 passed, 0 issues, 0 pending. The real (non-draft) `Publish release` path was not exercised by this phase by design; the roadmap asks only for the dry run.

### Gaps Summary

No gaps. All four ROADMAP success criteria still hold on the current tree, all six requirement IDs are accounted for, the later Phase 32 edits to `FORK-DELTA.md` and `TestsWorkflowPinGuardUnitTest.java` do not regress any Phase 30 deliverable, and both pin-guard test classes pass.

---

_Verified: 2026-10-27_
_Verifier: Claude (gsd-verifier)_
