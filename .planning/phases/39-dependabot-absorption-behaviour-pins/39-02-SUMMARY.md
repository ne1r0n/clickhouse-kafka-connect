---
phase: 39-dependabot-absorption-behaviour-pins
plan: "02"
subsystem: infra
tags: [dependabot, setup-gradle, github-actions, action-pinning, fork-delta, merge-invariants]

# Dependency graph
requires:
  - phase: 39-dependabot-absorption-behaviour-pins
    provides: "plan 39-01: the gson tip (merge ac216401), E-01 execution-start facts and the evidence helper"
provides:
  - "gradle/actions/setup-gradle 6.4.1 pinned by its peeled commit 8da46b53 in the four fork workflows, absorbed as its own --no-ff merge commit 82202f7d (second parent = PR #21 head 14be67ce)"
  - "FORK-DELTA tests.yaml row naming setup-gradle v6.4.1 with DEP-02, changed in the same merge commit"
  - "39-COMMAND-EVIDENCE.md entries E-04 and E-05"
  - "a green setup-gradle tip: six workflow guard classes (184 tests) and the full testUnit (1672 tests) under --rerun with Docker unreachable, cumulative merge-invariant gate PASS across both Dependabot merges"
affects: [39-03, 39-04, 44-release-rehearsal]

requires_next:
  - "39-03 and 39-04 build on a tree that holds both Dependabot merges; the first GitHub run of the combined tree and of the dispatch-only workflows is the Phase 44 rehearsal push"

# Actuals (#2632): chars/4 over the realized diff (STATE.md excluded)
actuals:
  tokens: 9000
  tasks: 2
  commits: 4
plan_head_before: 1f810e2f0552e319714598cfa1f2a691e93f39a4
plan_head_after: 4a848dc2b1261cdae755bef19f57d54ddc5abe2e

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Dependabot action bump absorbed as a --no-ff --no-commit merge of the fetched PR head, with the FORK-DELTA row edited in the same commit"
    - "Action pin is the peeled commit of refs/tags/vX.Y.Z (the ^{} line of git ls-remote), never the annotated tag object"

key-files:
  created: []
  modified:
    - .github/workflows/tests.yaml
    - .github/workflows/nightly-integration-tests.yaml
    - .github/workflows/release.yaml
    - .github/workflows/pre-release.yaml
    - docs/FORK-DELTA.md
    - .planning/phases/39-dependabot-absorption-behaviour-pins/39-COMMAND-EVIDENCE.md

key-decisions:
  - "Pinned SHA is the peeled commit 8da46b53 of v6.4.1, not the tag object 7296c772; verified against the live gradle/actions remote"
  - "No vulnerability claim is written: the 6.4.1 security fix concerns dependency-submission, which no fork workflow uses"
  - "The cumulative gate ran on the explicit SHA of the E-04 evidence commit and no commit was made while it ran"

patterns-established:
  - "release.yaml and pre-release.yaml keep their mutual diff signature across a bump that changes one pin line in each (numstat 1/1 each)"

requirements-completed: [DEP-02]

coverage:
  - id: D1
    description: "setup-gradle 6.4.1 arrives from the fetched Dependabot PR #21 head as its own --no-ff merge commit whose first-parent diff is exactly the four fork workflows and docs/FORK-DELTA.md"
    requirement: DEP-02
    verification:
      - kind: other
        ref: "git diff --name-only 82202f7d^1 82202f7d, second parent equals 14be67ce (39-COMMAND-EVIDENCE.md E-04, P39-02-T1-OK)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Each of the four workflows holds exactly one setup-gradle line pinned to the peeled commit 8da46b53 # v6.4.1, equal to the live refs/tags/v6.4.1^{} and different from the tag object; integrationTests.yaml is byte-identical and release/pre-release keep their mutual diff"
    requirement: DEP-02
    verification:
      - kind: other
        ref: "grep -c pin lines, git ls-remote gradle/actions, shasum of integrationTests.yaml and the release/pre-release diff (E-04)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The pin-guard classes plus the nightly and upstream-bytes guards pass under testUnit --rerun with Docker unreachable, with no test file edited"
    requirement: DEP-02
    verification:
      - kind: unit
        ref: "gradlew testUnit --rerun --tests '*ReleaseWorkflowGuardUnitTest' ... : 6 files, 184 tests, 0 failures, 0 errors, 0 skipped (E-04)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Full testUnit passes with --rerun and Docker unreachable on the setup-gradle tip, and the cumulative merge-invariant gate ends result=PASS on an explicit SHA holding both Dependabot merges"
    requirement: DEP-02
    verification:
      - kind: other
        ref: "gradlew testUnit --rerun: 1672 tests, 0 failures, 0 errors, 2 skipped; verify-merge-invariants.sh 54e63648 686cc497 (E-05, P39-02-T2-OK)"
        status: pass
    human_judgment: false

# Metrics
duration: 45min
completed: 2026-11-01
status: complete
---

# Phase 39 Plan 02: setup-gradle 6.4.1 absorption Summary

**setup-gradle 6.4.1 absorbed from Dependabot PR #21 as its own `--no-ff` merge, pinned by its peeled commit `8da46b53` in all four fork workflows with the FORK-DELTA row in the same commit, proven by six green workflow-guard classes (184 tests), a full green testUnit (1672 tests) and a cumulative merge-invariant gate PASS across both Dependabot merges.**

## Performance

- **Duration:** 45 min (the cumulative gate took 38 min and ran in the background while testUnit ran)
- **Started:** 2026-11-01T18:33:29Z
- **Completed:** 2026-11-01T19:18Z
- **Tasks:** 2
- **Files modified:** 6 (four workflows and FORK-DELTA through the merge, the evidence file by two commits)

## Accomplishments

- Tracer (E-04): the PR #21 head `14be67ce` (dependabot[bot], one parent `72e8ccff`, only the four fork workflows, one `uses:` line each) was merged with `--no-ff --no-commit`; the FORK-DELTA `tests.yaml` row was edited (`setup-gradle v6.4.0` to `v6.4.1`, `DEP-02` after `SWP-02`) and the merge `82202f7d` carries both. Its first-parent diff is exactly the four workflows plus `docs/FORK-DELTA.md`, one added and one removed line each.
- Peeled-commit pin: the live `git ls-remote` of `gradle/actions` gives `7296c772...` for the tag object and `8da46b53...` for `refs/tags/v6.4.1^{}`; the one SHA pinned in all four workflows is the peeled commit. No file under `.github` names the v6.4.0 commit any more.
- Untouched surfaces: `integrationTests.yaml` keeps SHA-256 `07dff3d0...5109`, and `diff release.yaml pre-release.yaml` keeps signature `ac7dce11...8d44`, so the release/pre-release pair keeps its mutual diff, which is the measured form of the reworded criterion 2.
- Guard classes: `ReleaseWorkflowGuardUnitTest`, `TestsWorkflowPinGuardUnitTest`, `WorkflowActionPinGuardUnitTest`, `DependabotProtobufIgnoreGuardUnitTest`, `NightlyWorkflowGuardUnitTest` and `UpstreamIntegrationWorkflowBytesGuardUnitTest` ran under `testUnit --rerun` with Docker unreachable: 6 XML files, 184 tests, no failure, error or skip; no test file was edited and the synthetic v6.4.0 fixtures stay.
- Gate (E-05): the full `testUnit` with `--rerun` passed (1672 tests, 2 skipped, equal to E-03's second run), the anchors mirror reads `rows=172 bad=0`, and `verify-merge-invariants.sh 54e63648 686cc497` ended `result=PASS`, exit 0, on a SHA that holds both Dependabot merges and the gg6 publication merge.

## Task Commits

1. **Task 1 (tracer): setup-gradle merged as its own commit with the FORK-DELTA row; pins, peeled commit, guard classes (E-04)** - merge `82202f7d` (second parent `14be67ce`), evidence `686cc497` (docs)
2. **Task 2: setup-gradle gate and the cumulative merge-invariant gate (E-05)** - `4a848dc2` (docs)

**Plan metadata:** the SUMMARY commit, followed by the STATE and ROADMAP commit (docs).

The measured `commits: 4` counts `plan_head_before..plan_head_after`: the merge, the two evidence commits, and the absorbed Dependabot head `14be67ce`.

## Files Created/Modified

- `.github/workflows/tests.yaml`, `.github/workflows/nightly-integration-tests.yaml`, `.github/workflows/release.yaml`, `.github/workflows/pre-release.yaml` - one `uses:` line each, now `gradle/actions/setup-gradle@8da46b539cd8fe6ab27ca75128919a18d757604c # v6.4.1` (arrived through the merge)
- `docs/FORK-DELTA.md` - `tests.yaml` row: setup-gradle `v6.4.1 by its peeled commit` and `DEP-02` in the requirement cell (one line, in the merge commit)
- `.planning/phases/39-dependabot-absorption-behaviour-pins/39-COMMAND-EVIDENCE.md` - entries E-04 and E-05, index rows flipped to pass

## Decisions Made

- The pin is the peeled commit, checked against the live remote rather than taken from the PR alone; the Dependabot head already carried it, and the check confirmed it differs from the tag object.
- The merge message and the FORK-DELTA edit state no closed vulnerability: GHSA-chpw-2mqm-95cc concerns dependency-submission, which no fork workflow uses.
- No scratch push was made; both PR heads already ran `tests.yaml` green on GitHub (run 41297596686 on the PR head), and the first GitHub run of the combined tree and of the dispatch-only workflows is the Phase 44 rehearsal push.

## Deviations from Plan

None - plan executed exactly as written.

One execution note that is not a deviation: `h` is an alias for `history` in this zsh, so the plan's `h() {...}` automated verify blocks ran under `/bin/bash -c` and the other helper calls as direct `python3 -I` invocations, as the operator notes for this plan said. The cumulative gate took 38 minutes against the research's 17 to 23 on a swap-pressured host; it changed no result.

## Issues Encountered

None. No JVM crash file appeared in the repository root and no run needed a retry. Origin `custom_build` still read `72e8ccff` at every re-read.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None.

## Threat Flags

None. The plan moves four existing GitHub Action pins to a verified peeled commit and writes local evidence; no new endpoint, auth path, file access pattern or schema surface appears. Nothing was pushed and no pull request or GitHub resource was modified (only `git ls-remote`, `git fetch` of a named ref, and unauthenticated REST GETs).

## Next Phase Readiness

- Ready for plan 39-03 (PIN-02 and PIN-03 behaviour pins) on a tree holding both Dependabot merges, each behind its own gate. `src/main`, `src/test` and `.planning/scripts` are unchanged against `phase_start_sha`.
- Nothing is pushed; the Phase 44 rehearsal push is the first GitHub run of this tree.

## Self-Check: PASSED

- Evidence file exists with E-04 and E-05; both index rows read `pass`.
- Commits exist and are ancestors of HEAD: `14be67ce`, `82202f7d`, `686cc497`, `4a848dc2`.
- Task automated verifications printed `P39-02-T1-OK` and `P39-02-T2-OK`; `git diff --quiet phase_start_sha HEAD -- src/main src/test .planning/scripts` exits 0.

---
*Phase: 39-dependabot-absorption-behaviour-pins*
*Completed: 2026-11-01*
