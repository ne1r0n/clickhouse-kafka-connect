---
status: complete
phase: 30-release-workflows-pin-guard
source: [30-01-SUMMARY.md, 30-02-SUMMARY.md, 30-03-SUMMARY.md]
started: 2026-10-27T00:00:00Z
updated: 2026-10-27T12:50:11.310Z
---

## Current Test

[testing complete]

## Tests

### 1. Release workflows build with wrapper Gradle and require verifyConfluentArchive (D3)
expected: release.yaml and pre-release.yaml build with the wrapper's Gradle through setup-gradle and require the '> Task :verifyConfluentArchive' line; the GitHub dry runs (pre-release run 40296330498, release run 40296481765) built the archive and passed verifyConfluentArchive.
result: pass

### 2. dry_run mechanism and publish gates on GitHub (D4)
expected: gh release create replaces the upstream create/upload actions and runner tag push. A dry run creates a throwaway draft, checks the asset, confirms no tag exists, deletes the draft (always()), and Publish release is skipped. Origin gained no release and no tag (10 tags before and after). Real publish only under !inputs.dry_run on refs/heads/custom_build.
result: pass

### 3. WorkflowActionPinGuardUnitTest pin guard (D1)
expected: Fails with file:line on any unpinned uses: line in fork-owned workflows (16 reject fixtures, 21 cases, 0 failures)
result: pass
source: automated
coverage_id: D1

### 4. Release workflows pin every action by full SHA (D2)
expected: release.yaml, pre-release.yaml and increment-version.yml pin every action by full SHA with a version comment
result: pass
source: automated
coverage_id: D2

### 5. Dependabot coverage and anchors (D5)
expected: Dependabot keeps every guarded workflow current; 29 anchor rows; cumulative anchors gate passes
result: pass
source: automated
coverage_id: D5

### 6. increment-version vnull defect measured (30-02 D1)
expected: semver 7.8.5 on Node 24 returns null for v1.7.0_gg4; recorded as E-06
result: pass
source: automated
coverage_id: 30-02-D1

### 7. FORK-DELTA workflow paragraph (30-02 D2)
expected: FORK-DELTA describes the three fork-modified release-path workflows and cites E-06
result: pass
source: automated
coverage_id: 30-02-D2

### 8. FORK-DELTA Dependabot row (30-02 D3)
expected: Row says only integrationTests.yaml is excluded and names WorkflowActionPinGuardUnitTest
result: pass
source: automated
coverage_id: 30-02-D3

## Summary

total: 8
passed: 8
issues: 0
pending: 0
skipped: 0

## Gaps

[none yet]
