---
phase: 17-ci-build-test-hygiene
plan: "01"
subsystem: infra
tags: [github-actions, ci, sha-pinning, dependabot, gradle, permissions]

requires:
  - phase: 14-dependency-bumps-ci
    provides: the single-job tests.yaml and the 14-SECURITY T-14-16 / 14-REVIEW WR-02 findings this plan closes
provides:
  - reportTestForks Gradle task and a fork-count step that reads it (CI-14)
  - tests.yaml pinned by verified commit SHA, on ubuntu-24.04, with a read-only token and no persisted checkout credentials (CI-11, CI-12, CI-13)
  - Dependabot github-actions entry scoped to tests.yaml (CI-15)
  - test-head.yaml deleted, with Recorded Fact 41 and FORK-DELTA rows (CI-16)
  - TestsWorkflowPinGuardUnitTest, a Docker-free guard in testUnit (D-06)
affects: [17-02, 17-03, 17-04, phase-20-release]

actuals:
  tokens: 10600
  tasks: 3
  commits: 9

tech-stack:
  added: []
  patterns:
    - "Workflow reads Gradle for the fork count; no formula copy in shell"
    - "Line-level Docker-free guard over .github files, UTF-8 explicit, no YAML library"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
    - .planning/phases/17-ci-build-test-hygiene/17-EVIDENCE.md
  modified:
    - build.gradle.kts
    - .github/workflows/tests.yaml
    - .github/dependabot.yml
    - docs/FORK-DELTA.md
    - .planning/MERGE-CHECKLIST-v1.7.0.md
  deleted:
    - .github/workflows/test-head.yaml

key-decisions:
  - "gradle/actions is pinned to the peeled commit 3f5f9adaf7d9fecd50b5935e54106014257a94e6, never the tag object b9bee63ef481cc545e4517ec730544a4d8b1dfa0"
  - "The fork count is read from Gradle's own test task; the integer guard turns a failed or empty Gradle call into a red step"
  - "test-head.yaml is deleted rather than adapted; future syncs keep it deleted on modify/delete (Recorded Fact 41)"

patterns-established:
  - "Red-first Docker-free pin guard for CI configuration, extends nothing so it stays in testUnit"

requirements-completed: [CI-11, CI-12, CI-13, CI-14, CI-15, CI-16]

coverage:
  - id: D1
    description: "CI-14: the Report test fork count step prints the maxParallelForks that reportTestForks reads from the test task, fails on a non-integer, and carries no formula copy"
    requirement: "CI-14"
    verification:
      - kind: other
        ref: "17-EVIDENCE.md#E-01 (step extracted from tests.yaml and run with bash -e; TRACER-FORK-COUNT-OK, TRACER-EDGES-OK)"
        status: pass
    human_judgment: false
  - id: D2
    description: "CI-11: every uses line in tests.yaml is a 40-hex commit with a version comment; each SHA matched its tag via git ls-remote at execution time; each pinned action.yml declares node24"
    requirement: "CI-11"
    verification:
      - kind: unit
        ref: "TestsWorkflowPinGuardUnitTest#everyActionIsPinnedToAFullCommitShaWithAVersionComment"
        status: pass
      - kind: other
        ref: "17-EVIDENCE.md#E-02 (PINS-MATCH-TAGS-NODE24)"
        status: pass
    human_judgment: false
  - id: D3
    description: "CI-12 and CI-13: ubuntu-24.04 runner, one top-level permissions block holding only contents: read, no write scope, checkout persist-credentials: false"
    requirement: "CI-13"
    verification:
      - kind: unit
        ref: "TestsWorkflowPinGuardUnitTest#everyJobRunsOnUbuntu2404, #workflowTokenIsReadOnly, #checkoutDoesNotPersistCredentials"
        status: pass
    human_judgment: false
  - id: D4
    description: "CI-15: dependabot.yml has a grouped monthly github-actions entry with exclude-paths for the four upstream workflows, and the gradle entry is byte-identical"
    requirement: "CI-15"
    verification:
      - kind: unit
        ref: "TestsWorkflowPinGuardUnitTest#dependabotUpdatesTheGithubActionsEcosystem"
        status: pass
      - kind: other
        ref: "17-EVIDENCE.md#E-03 (WORKFLOW-AND-DEPENDABOT-OK)"
        status: pass
    human_judgment: true
    rationale: "Dependabot only reads the default branch (main here) and fork version updates are off, so the entry cannot be observed working from custom_build; exclude-paths support is source-read, not an observed run"
  - id: D5
    description: "CI-16: test-head.yaml deleted and untracked with no remaining reference; Recorded Fact 41 and the FORK-DELTA rows written"
    requirement: "CI-16"
    verification:
      - kind: other
        ref: "17-EVIDENCE.md#E-04 (CI-16-STATIC-OK, FORK-DELTA-17-01-OK, CHECKLIST-LINE-OK)"
        status: pass
    human_judgment: false

duration: 5min
completed: 2026-10-11
status: complete
plan_head_before: 22dff3fe99b85bd8e6106568af8bfd3ade7aee13
plan_head_after: 34138e1ed55ec32b43b435929fb81ad63f75472e
---

# Phase 17 Plan 01: tests.yaml hardening Summary

**tests.yaml now runs on ubuntu-24.04 with a read-only token, no persisted checkout credentials and every action pinned to a tag-verified commit SHA; its fork-count step reads a new `reportTestForks` Gradle task, and a Docker-free `testUnit` guard that was seen red pins all of it.**

## Performance

- **Duration:** 5 min
- **Started:** 2026-10-11T20:14:40Z
- **Completed:** 2026-10-11T20:19Z
- **Tasks:** 3
- **Files modified:** 8 (one deleted, two created)

## Accomplishments

- Tracer (CI-14): `reportTestForks` prints the test task's `maxParallelForks` (6 on this 12-core machine, 1 with `CLICKHOUSE_CLUSTER_NAME` set). The step extracted from `tests.yaml` and run with `bash -e` printed exactly one notice whose figure equals Gradle's, wrote the summary, and went red with an `::error::` line when a fake `gradlew` exited 1 with no output. Closes 14-REVIEW WR-02.
- CI-11, CI-12, CI-13: five `uses:` lines pinned by SHA with version comments; ubuntu-24.04; top-level `permissions: contents: read`; `persist-credentials: false`. Closes 14-SECURITY T-14-16.
- CI-15: Dependabot `github-actions` entry, monthly, one group, four upstream workflows excluded; the gradle entry's bytes are untouched.
- CI-16: `test-head.yaml` deleted; Recorded Fact 41 and FORK-DELTA rows tell future syncs to keep it deleted.
- D-06: `TestsWorkflowPinGuardUnitTest` (5 tests) failed 5 of 5 against the old files, passes after the edits. Full `testUnit` is green: 459 tests, 457 succeeded, 0 failed, 2 skipped.

## Pinned actions (E-02)

All four commits equal the planning-time values; no impostor-commit stop was raised. `runs.using` is `node24` for every pin. No newer same-major release exists, so nothing is waiting for Dependabot.

| Action | Tag | Pinned commit | Newest same-major tag |
|--------|-----|---------------|-----------------------|
| actions/checkout | v7.0.1 | 3d3c42e5aac5ba805825da76410c181273ba90b1 | v7.0.1 |
| actions/upload-artifact (both steps) | v7.0.2 | cf430e030ddbb5b0abf93d22962f4752f3646cd9 | v7.0.2 |
| actions/setup-java | v6.0.1 | de7274f081f381c8f8158605e0321c36c376e2e6 | v6.0.1 |
| gradle/actions/setup-gradle | v6.4.0 (annotated) | 3f5f9adaf7d9fecd50b5935e54106014257a94e6 (peeled; tag object b9bee63e... not used) | v6.4.0 (a `v6.4.0-rc.1` pre-release exists) |

## Guard red messages (E-03)

Before the edits, all five tests failed, for example `Action is not pinned to a 40-hex commit with a '# vX.Y.Z' comment: '- uses: actions/checkout@v4'`, `runs-on must be the pinned runner, not 'ubuntu-latest'`, `Exactly one 'permissions:' line is allowed ... expected: <1> but was: <0>`, `Checkout step must set 'persist-credentials: false'` and `Missing '- package-ecosystem: "github-actions"' in .github/dependabot.yml`.

## Evidence verdicts

| Entry | Requirement | Verdict |
|-------|-------------|---------|
| E-01 | CI-14 | pass |
| E-02 | CI-11 | pass |
| E-03 | CI-11, CI-12, CI-13, CI-15, D-06 | pass |
| E-04 | CI-16, D-02 | pass |

## Task Commits

1. **Task 1 (tracer): reportTestForks and the step that reads it** - `d00ff328` (ci), evidence `5c9f274e` (docs)
2. **Task 2: pins, runner, permissions, Dependabot, behind the guard** - RED `af198009` (test), GREEN `475ce7b9` (ci), evidence `6f82ea99` (docs)
3. **Task 3: delete test-head.yaml and write the docs rows** - `c3171c97` (ci), `43a26085` (docs), evidence `17abae44` (docs)

Also `34138e1e` (docs): correction of the E-04 timestamp.

## Files Created/Modified

- `build.gradle.kts` - `reportTestForks` task (group `verification`)
- `.github/workflows/tests.yaml` - pins, runner, permissions, `persist-credentials: false`, rewritten fork-count step and header
- `.github/dependabot.yml` - second `updates` entry for `github-actions`
- `.github/workflows/test-head.yaml` - deleted
- `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java` - Docker-free guard
- `docs/FORK-DELTA.md` - CI row rewritten, test-head and Dependabot rows, untouched-workflows line, Known costs items 5 and 6
- `.planning/MERGE-CHECKLIST-v1.7.0.md` - Recorded Fact 41
- `.planning/phases/17-ci-build-test-hygiene/17-EVIDENCE.md` - E-01..E-04

## Decisions Made

- Pinned gradle/actions to the peeled commit; the annotated tag object would not be a commit on the action's history.
- Left `src/main`, `fork-anchors.txt` and the four upstream workflows untouched; every anchor row re-counted equal on the tip.

## Deviations from Plan

None - plan executed exactly as written. (Two execution slips were corrected in-plan: a zsh loop that did not word-split was re-run under `bash -c`, and an E-01/E-04 evidence timestamp that did not match the system clock was corrected in a follow-up commit.)

## Issues Encountered

None. No SHA mismatch occurred, so no `checkpoint:decision` was raised.

## Known Stubs

None.

## Threat Flags

None. The Dependabot entry and the permissions block reduce surface; no new endpoint, auth path or schema change was introduced.

## User Setup Required

None for this plan. Manual check for the next release (recorded in FORK-DELTA): Dependabot reads the default branch (`main`) and fork version updates are off, so the new entry is inert until the user changes both.

## Next Phase Readiness

- Ready for 17-02. CI-17 (a branch-only scratch push proving the new workflow green) belongs to plan 17-03 and needs the user's own approval; nothing was pushed here.
- Open: assumption A6 (setup-gradle v6.4.0 needs nothing beyond `contents: read`) is proven only by that CI run.

## Self-Check: PASSED

- Files exist: the guard test, `17-EVIDENCE.md`, FORK-DELTA, the checklist; `test-head.yaml` is absent.
- Commits `d00ff328`, `5c9f274e`, `af198009`, `475ce7b9`, `6f82ea99`, `c3171c97`, `43a26085`, `17abae44`, `34138e1e` are ancestors of HEAD.
- Acceptance greps: `tasks.register("reportTestForks")` count 1; `^41\. \*\*CI-16` count 1; `src/main` diff against the phase start empty.

---
*Phase: 17-ci-build-test-hygiene*
*Completed: 2026-10-11*
