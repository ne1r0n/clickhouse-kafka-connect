---
phase: 30-release-workflows-pin-guard
reviewed: 2026-10-26T00:00:00Z
depth: standard
files_reviewed: 7
files_reviewed_list:
  - .github/dependabot.yml
  - .github/workflows/increment-version.yml
  - .github/workflows/pre-release.yaml
  - .github/workflows/release.yaml
  - docs/FORK-DELTA.md
  - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/WorkflowActionPinGuardUnitTest.java
findings:
  critical: 0
  warning: 3
  info: 4
  total: 7
status: issues_found
---

# Phase 30: Code Review Report

**Reviewed:** 2026-10-26
**Depth:** standard
**Files Reviewed:** 7
**Status:** issues_found

## Summary

The release path is well built. The publish gate runs from `custom_build` only. Failure of the `tests.yaml` and tag lookups fails closed on a publish. The `bash` shell gives `-e -o pipefail`, so the `| tee` build step cannot hide a Gradle failure. The dry-run cleanup is `always()` and has a fallback that deletes by release id. The archive path `build/confluent/clickhouse-kafka-connect-<VERSION>.zip` matches `createConfluentArchive` (`archiveBaseName` empty, appendix, `project.version` read from `VERSION`).

The `vnull` defect in `increment-version.yml` (E-06, D-08) is known and not re-reported here.

No blocker was found. The three warnings are about queueing semantics, safety gates that no test pins, and a wrong code comment. The four info items are smaller.

Not checked: whether the pinned commit SHAs and `semver@7.8.5` exist upstream. That needs network access.

## Warnings

### WR-01: Shared `release-publish` concurrency group can silently drop a queued publish

**File:** `.github/workflows/release.yaml:22-24` (identical in `.github/workflows/pre-release.yaml:22-24`)
**Issue:** `release.yaml` and `pre-release.yaml` share the group `release-publish` with `cancel-in-progress: false`. GitHub keeps one running and one pending run per group. A newer pending run cancels the older pending one. Dry runs share the group with real publishes. A real publish queued behind a running dry run, or behind the other release workflow, is cancelled without a failure if anyone dispatches a third run. For example, a dry run dispatched from another branch to check something would do this. The operator sees a cancelled run and may assume a publish happened or failed for another reason.
**Fix:** Keep one group for publishes only, and give dry runs their own group per run, for example `group: release-publish-${{ (inputs.dry_run || github.ref != 'refs/heads/custom_build') && github.run_id || 'main' }}`. Otherwise document in the header that a queued dispatch can be superseded and must be re-dispatched.

### WR-02: Nothing pins the release safety gates in the two release workflows

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/WorkflowActionPinGuardUnitTest.java:166-193`, `.github/workflows/release.yaml:116-128`
**Issue:** `WorkflowActionPinGuardUnitTest` pins only the SHA form of `uses:` lines. The hardening that matters for a workflow with `contents: write` is not under test:
- the `custom_build`-only publish condition and the dry-run default for every other ref
- the tag-absent and `tests.yaml` gates
- the top-level `permissions: contents: read`
- `persist-credentials: false`
- the `ubuntu-24.04` runner pin

`TestsWorkflowPinGuardUnitTest` checks the permissions, runner and checkout rules for `tests.yaml` only. A later edit can widen the token, switch back to `ubuntu-latest`, drop the `if:` on "Publish release", or remove a gate, and `testUnit` stays green. The `if:` text and the `PUBLISH:` env expression are also duplicated by hand in several places in each file, so they can drift apart. A drift would let a publish step run while the gate steps are in advisory mode.
**Fix:** Add a line-level rule over `release.yaml` and `pre-release.yaml` that:
- requires a single top-level `contents: read`
- requires the `if:` on "Publish release" to equal the `custom_build` expression
- requires every `PUBLISH:` env to equal the same expression

Reuse the existing `nonCommentLines` helpers. Reading `custom_build` and the `PUBLISH` expression from one `env:` at the job level would also remove the drift.

### WR-03: `dependabot.yml` comment overstates what the guard test checks

**File:** `.github/dependabot.yml:16-19`
**Issue:** The comment says "`WorkflowActionPinGuardUnitTest` fails if this list changes". This is true only for the `github-actions` entry's `exclude-paths`. The guard reads the first `github-actions` entry through `excludePaths`. It does not check that the entry exists, its schedule, or its grouping. `TestsWorkflowPinGuardUnitTest.dependabotEcosystemProblem` checks that the entry exists.
**Fix:** Reword to "fails if the exclude-paths of this entry hold anything but `integrationTests.yaml`".

## Info

### IN-01: `FORK-DELTA.md` and the workflow header call `increment-version.yml` "pinned only" or "hardening only, the behavior is upstream's"

**File:** `docs/FORK-DELTA.md` (the paragraph under the CI table), `.github/workflows/increment-version.yml:1-5`
**Issue:** The diff shows more than pinning:
- Node 16 changed to 24
- `npm install semver` plus `npm install fs` changed to `semver@7.8.5 --no-save --ignore-scripts`
- a 10-minute timeout was added
- `GH_TOKEN` was narrowed from workflow level to the single push step
- token scopes moved to the job

The Node major bump is a runtime change that a future upstream sync should see. The `fs` install is dropped silently.
**Fix:** Add one sentence naming the Node and npm changes to the FORK-DELTA paragraph.

### IN-02: Shell and JS interpolation in `increment-version.yml`

**File:** `.github/workflows/increment-version.yml:51-52, 60-64`
**Issue:** `$VERSION` is placed inside single quotes in `node -p "...inc('$VERSION', '${{ github.event.inputs.version }}')"`. `${{ env.NEW_VERSION }}` is expanded into the script text for `git checkout -b`, `git commit -m` and `gh pr create`. The dispatch input is a `choice`, and `VERSION` comes from the dispatched ref, so exploitability needs write access. The pattern is still fragile, and a malformed `VERSION` breaks the JS expression.
**Fix:** Pass them through `env:` and read `process.env`:
```yaml
env:
  BUMP: ${{ inputs.version }}
run: |
  NEW_VERSION=$(node -p "require('semver').inc(require('fs').readFileSync('VERSION','utf8').trim(), process.env.BUMP)")
```

### IN-03: `release.yaml` and `pre-release.yaml` are about 180 duplicated lines

**File:** `.github/workflows/release.yaml`, `.github/workflows/pre-release.yaml`
**Issue:** The two files differ only in the `name`, `run-name`, `--prerelease` on two `gh release create` lines, and the `isPrerelease` expectation. Any later fix to a gate, such as WR-01 or WR-02, must be made twice, and no test asserts the two stay equal.
**Fix:** Extract a reusable workflow with a `prerelease` input, or add a test that diffs the two files after normalising those lines.

### IN-04: The `tests.yaml` gate accepts a successful run from any branch or event

**File:** `.github/workflows/release.yaml:90`
**Issue:** The query filters by `head_sha` and `status=success` only. A successful run on the same SHA from another branch counts as green. That is arguably correct, because it is the same code. The warning text "successful tests.yaml runs on SHA" does not say this.
**Fix:** If strictness is wanted, add `&branch=custom_build`. Otherwise leave it as is.

---

_Reviewed: 2026-10-26_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
