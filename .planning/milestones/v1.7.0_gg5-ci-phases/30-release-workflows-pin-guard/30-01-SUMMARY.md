---
phase: 30-release-workflows-pin-guard
plan: "01"
subsystem: ci
tags: [github-actions, sha-pinning, gh-release, setup-gradle, dependabot, junit5, fork-anchors]

requires:
  - phase: 17-ci-hardening
    provides: tests.yaml SHA pins, TestsWorkflowPinGuardUnitTest, the Dependabot github-actions entry
  - phase: 29-release-publication
    provides: verify-merge-invariants.sh and fork-anchors.txt (94 rows), the v1.7.0_gg4 archive naming
provides:
  - WorkflowActionPinGuardUnitTest: Docker-free guard over every fork-owned workflow (CIG-01) plus the D-10 Dependabot coverage test
  - release.yaml and pre-release.yaml: SHA-pinned, wrapper Gradle through setup-gradle, gh release create, dry_run input with a ref-derived dry mode
  - increment-version.yml: SHA-pinned, Node 24, exact semver without install scripts, job-level permissions
  - Dependabot exclude-paths reduced to integrationTests.yaml; 29 Phase 30 anchor rows (123 in total)
  - 30-COMMAND-EVIDENCE.md entries E-01 to E-05
affects: [30-02 FORK-DELTA, 30-03 operator dry run, 31 nightly workflow (written under this guard), 32 release flow]

plan_head_before: d6a574d09437f813c5967a118e81f2a07fb237f2
plan_head_after: d3cdaf149426b0f29f02d5380246b11b5a219382

actuals:
  tokens: 12800
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - "Directory-discovered, line-numbered pin guard: file:line: reason: 'text' per unpinned uses: line, fail-closed on ./ and docker:// values"
    - "Dry run as the exact complement of publish: the publish step and the dry steps carry complementary if: expressions, pinned by anchor rows (3 and 5)"
    - "Guard lands green in the same commit as the pins; its red state is recorded first in a docs commit"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/ci/WorkflowActionPinGuardUnitTest.java
    - .planning/phases/30-release-workflows-pin-guard/30-COMMAND-EVIDENCE.md
  modified:
    - .github/workflows/release.yaml
    - .github/workflows/pre-release.yaml
    - .github/workflows/increment-version.yml
    - .github/dependabot.yml
    - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
    - .planning/scripts/fork-anchors.txt

key-decisions:
  - "Dry run uses a throwaway tag dry-run-<run_id>-<run_attempt>, a draft and a plain gh release delete --yes; the tag-cleanup flag is never used and the real VERSION tag is never touched (UD-1)"
  - "Real publish only under !inputs.dry_run && github.ref == refs/heads/custom_build; the tests.yaml-green and tag-absent checks run on every dispatch and block only a real publish (UD-4, PT-05)"
  - "increment-version.yml keeps checkout's persisted credentials because it pushes a branch; GH_TOKEN is set only on the Commit and push step (UD-2)"
  - "The guard fails closed on local ./ and docker:// uses values instead of reading composites (UD-3, D-12)"
  - "Dependabot exclude-paths keeps only integrationTests.yaml; UPSTREAM_WORKFLOWS is that one path (D-10, user-approved)"
  - "The increment-version vnull defect is left unfixed and recorded in the workflow header (D-08); recording in FORK-DELTA is plan 30-02"

patterns-established:
  - "Pin guard reuses TestsWorkflowPinGuardUnitTest.PINNED_USES and USES_LINE (six members made package-private) instead of copying them"

requirements-completed: [CIG-01, REL-01, REL-02, REL-03, REL-04, REL-05]

coverage:
  - id: D1
    description: "WorkflowActionPinGuardUnitTest fails with file:line on any unpinned uses: line in any fork-owned workflow, with 16 fail-closed reject fixtures, and was seen red on the unchanged tree with exactly the 12 predicted lines"
    requirement: "CIG-01"
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/ci/WorkflowActionPinGuardUnitTest.java (21 cases, 0 failures)"
        status: pass
    human_judgment: false
  - id: D2
    description: "release.yaml, pre-release.yaml and increment-version.yml pin every action by full SHA with a version comment"
    requirement: "REL-02"
    verification:
      - kind: unit
        ref: "WorkflowActionPinGuardUnitTest#every fork-owned workflow under .github/workflows pins every action to a full commit SHA with a version comment (CIG-01)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Both release workflows build with the wrapper's Gradle through setup-gradle and require the verifyConfluentArchive task line; the build step was checked locally against the real project"
    requirement: "REL-01, REL-03"
    verification:
      - kind: other
        ref: "plan verify command WORKFLOW-SHAPE-OK; local ./gradlew createConfluentArchive --console=plain printed the exact line '> Task :verifyConfluentArchive'"
        status: pass
    human_judgment: true
    rationale: "Whether the runner's Gradle, gh and API calls behave as written is only provable by the operator dry run of plan 30-03"
  - id: D4
    description: "gh release create replaces the upstream create/upload actions and the runner tag push; the dry_run mechanism (throwaway draft, asset check, no-tag check, always() cleanup) and the publish gates are in place"
    requirement: "REL-04, REL-05"
    verification:
      - kind: other
        ref: "plan verify command WORKFLOW-SHAPE-OK; anchors rows for gh release create 2, --draft 1, --cleanup-tag 0, the publish expression 3 and its complement 5"
        status: pass
    human_judgment: true
    rationale: "The mechanism is statically pinned; its behavior on GitHub (draft creates no tag, gh release view finds a draft, matching-refs returns an empty array) is measured by plan 30-03"
  - id: D5
    description: "Dependabot keeps every guarded workflow current; D-10 fixtures replaced without losing a test; 29 anchor rows; cumulative anchors gate passes"
    requirement: "CIG-01"
    verification:
      - kind: unit
        ref: "TestsWorkflowPinGuardUnitTest 39/0/0/0; WorkflowActionPinGuardUnitTest D-10 test; verify-merge-invariants.sh 54e63648 HEAD anchor_rows=123 result=PASS; selftest: no failures"
        status: pass
    human_judgment: false

duration: 45min
completed: 2026-10-24
status: complete
---

# Phase 30 Plan 01: Release workflows and pin guard Summary

**A Docker-free guard that fails on any unpinned, local or docker:// action in every fork workflow, plus the three release-path workflows rewritten under it: SHA pins, the wrapper's Gradle via setup-gradle, `gh release create`, and a ref-derived dry run with a throwaway draft.**

## Performance

- **Duration:** about 45 min (most of it the 30-minute cumulative anchors gate)
- **Started:** 2026-10-24T13:33Z (approximate)
- **Completed:** 2026-10-24T14:20Z
- **Tasks:** 3 of 3 (task 1 is a tracer)
- **Files modified:** 8 (7 in the green commit, plus the evidence file)

## Accomplishments

- `WorkflowActionPinGuardUnitTest` (21 cases) discovers `.yml` and `.yaml` files under `.github/workflows` except `integrationTests.yaml`, and fails with one `<path>:<line>: <reason>: '<text>'` line per unpinned `uses:` line. 16 fixtures prove each unpinned form is rejected (major tag, version tag, branch, short, 39-hex, 41-hex and upper-case SHA, missing and wrong comments, local and docker forms, quoted value, flow mapping, no owner).
- The guard was seen red on the unchanged tree before any workflow edit: exactly the pin test (12 `file:line` tokens: `increment-version.yml:27` and `:36`, and lines 15, 23, 29, 34, 44 of both release files) and the D-10 Dependabot test failed; every fixture passed (E-02).
- `release.yaml` and `pre-release.yaml` now pin checkout, setup-java, setup-gradle (peeled commit) by SHA, build with the wrapper through `setup-gradle` (cache disabled), require the `> Task :verifyConfluentArchive` log line, and publish with `gh release create` attaching the archive (`--prerelease` only in `pre-release.yaml`). A real publish needs `custom_build`, `dry_run` false, a green `tests.yaml` run on the SHA and an absent tag; every other dispatch creates, checks and deletes a throwaway draft under `dry-run-<run_id>-<run_attempt>`.
- `increment-version.yml` is pinned (checkout, setup-node on Node 24), installs `semver@7.8.5` without scripts, drops `fs`, and holds its token scopes at job level; its run bodies are byte-identical (D-08).
- Dependabot's `github-actions` entry now excludes only `integrationTests.yaml` (D-10, user-approved), with the reason in the comment and the commit body; the fixtures that indexed a four-element list were replaced by equally strong ones and no test method was added or removed.
- 29 anchor rows (123 in total) pin the new workflow text, including the publish expression (3) and its dry-run complement (5); the cumulative gate ends `anchor_rows=123 ... result=PASS` and the self-test `selftest: no failures`.

## Task Commits

1. **Task 1: guard end to end, seen red (tracer)** - `4c9be465` (docs; the guard itself stays uncommitted until Task 3)
2. **Task 2: rewrite the three workflows** - `9b5f0c6f` (docs, records E-03; the workflow files stay uncommitted until Task 3)
3. **Task 3: Dependabot, fixtures, anchors, one green commit** - `496de32a` (ci, the single commit with all seven files) and `d3cdaf14` (docs, records E-04 and E-05)

**Plan metadata:** the SUMMARY, STATE and ROADMAP commit follows this file.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/ci/WorkflowActionPinGuardUnitTest.java` - the CIG-01 guard, fixtures and the D-10 test
- `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java` - six members package-private; `UPSTREAM_WORKFLOWS` is one path; fixtures and two display names replaced
- `.github/workflows/release.yaml`, `.github/workflows/pre-release.yaml` - rewritten release path
- `.github/workflows/increment-version.yml` - pinned and hardened
- `.github/dependabot.yml` - `exclude-paths` lists only `integrationTests.yaml`
- `.planning/scripts/fork-anchors.txt` - 29 Phase 30 rows
- `.planning/phases/30-release-workflows-pin-guard/30-COMMAND-EVIDENCE.md` - E-01 to E-05

## Decisions Made

See `key-decisions`. In addition, within the plan's latitude: the parameterized reject test takes the expected reason as a third argument (`NOT_PINNED` or `LOCAL_OR_DOCKER`) instead of deriving it from the label, so each form is also pinned to its reason.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] The plan's GREEN-COMMIT-OK check is over-broad**
- **Found during:** Task 3 (verify command 3)
- **Issue:** It rejects any `subList(` in `TestsWorkflowPinGuardUnitTest.java`, but four unrelated, pre-existing `subList(` calls remain (helpers at lines 191, 358, 380 and `FIVE_PINNED.subList(0, 4)` at 820). The same command's clean-tree clause also flagged `.planning/ROADMAP.md` and `.planning/STATE.md`, which were dirty before the plan started.
- **Fix:** Ran the remaining parts of the command by hand: the targeted check on `UPSTREAM.subList`, `UPSTREAM.get(1..3)` and "four upstream workflows" finds 0 matches; files, method set (41 before and after) and the `UPSTREAM_WORKFLOWS` definition pass; `spotlessApply` in a worktree at the commit leaves both Java files byte-identical.
- **Files modified:** none (check only)
- **Verification:** recorded in E-04
- **Commit:** `d3cdaf14`

### Minor shape choices (no behavior change)

- The reject test's third argument (see Decisions Made).
- `pre-release.yaml` is generated from `release.yaml` with six exact substitutions (name, run-name, header sentence, two `--prerelease` suffixes, expected `isPrerelease`), so the two files stay structurally identical.

**Total deviations:** 1 auto-fixed (1 blocking verify-command defect). **Impact:** none on the delivered files.

## Authentication Gates

None. All network access was read-only (`git ls-remote`); no push, dispatch or `gh` command was run.

## Known Stubs

None.

## Issues Encountered

None. The OpenJ9 crash workaround was not needed (all Gradle runs exited cleanly).

## Threat Flags

None beyond the plan's threat register (T-30-01 to T-30-10 are mitigated as listed; T-30-07 accepted).

## Next Phase Readiness

- Plan 30-02 (FORK-DELTA corrections and the D-08 `vnull` record) and plan 30-03 (operator dry run, push approval, `gh` reads) can proceed.
- Open runner-side facts for plan 30-03 to measure: `gh release view` finds a draft by tag, the matching-refs endpoint returns an empty array for no match, the dispatch form shows `dry_run` from a non-default branch, and a draft creates no tag.
- Phase 31's nightly workflow must pass `WorkflowActionPinGuardUnitTest` unchanged.

## Self-Check: PASSED

- Created files exist: the guard class, `30-COMMAND-EVIDENCE.md`, this SUMMARY.
- Commits `4c9be465`, `9b5f0c6f`, `496de32a`, `d3cdaf14` are ancestors of HEAD.
- Plan verification: `GUARD-RED-OK`, `T1-RECORDED-OK`, `WORKFLOW-SHAPE-OK`, `T2-GUARD-OK`, `ANCHORS-MIRROR-OK` (rows=123 bad=0), `GREEN-SUITE-OK` (testUnit SUCCESS, guard 21/0/0/0, TestsWorkflowPinGuardUnitTest 39/0/0/0), `ANCHORS-GATE-OK`; `GREEN-COMMIT-OK` run in parts (see Deviations).
- `git diff --quiet v1.7.0_gg4 HEAD -- src/main` exits 0; nothing was pushed or dispatched.
