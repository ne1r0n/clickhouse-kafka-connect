---
phase: 30-release-workflows-pin-guard
plan: "02"
subsystem: docs
tags: [fork-delta, semver, increment-version, dependabot, release-workflows]

requires:
  - phase: 30-release-workflows-pin-guard
    provides: plan 30-01 workflows, pin guard and Dependabot edit that made two FORK-DELTA statements false
provides:
  - E-06, the measured semver.inc defect of increment-version.yml on the real VERSION
  - FORK-DELTA workflow paragraph describing the three fork-modified release-path workflows and the known vnull defect
  - FORK-DELTA Dependabot row corrected to the one-path exclude-paths list (D-10)
  - E-07, the corrections before and after and the two clauses carried to Phase 32
affects: [30-03 operator dry run, 32 FORK-DELTA refresh (CIG-02)]

plan_head_before: 80d8c55b56638810738aa71a1e5b57f1bd98605d
plan_head_after: 2f66977a8b422dde2811b3c1082afd77ebaa37a9

actuals:
  tokens: 2750
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Measure before documenting: the defect was run on the real VERSION with the pinned package, not read from the grammar"

key-files:
  created: []
  modified:
    - docs/FORK-DELTA.md
    - .planning/phases/30-release-workflows-pin-guard/30-COMMAND-EVIDENCE.md

key-decisions:
  - "The vnull defect of increment-version.yml stays unfixed and is stated in FORK-DELTA with a citation of E-06 (D-08)"
  - "The row's stale default-branch and inert clauses, and the same default-branch claim in the test-head row, are carried to Phase 32 (CIG-02), not edited (UD-5)"

requirements-completed: [REL-02, REL-04]

coverage:
  - id: D1
    description: "semver 7.8.5 on Node v24.14.1 returns null for inc patch, minor, major and for valid on v1.7.0_gg4, so increment-version.yml would write vnull to VERSION; recorded as E-06"
    requirement: "REL-02"
    verification:
      - kind: other
        ref: "plan verify command D08-MEASURED-OK; transcript line semver-d08 v1.7.0_gg4 [null,null,null,null,\"7.8.5\"]"
        status: pass
    human_judgment: false
  - id: D2
    description: "FORK-DELTA's workflow paragraph describes the three release-path workflows as fork-modified and cites E-06; the old upstream-as-shipped and Gradle 7.4.2 sentences are gone"
    requirement: "REL-04"
    verification:
      - kind: other
        ref: "plan verify command PARAGRAPH-OK (13 required phrases present, old and stale text count 0, commit holds exactly FORK-DELTA and the evidence file)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The Dependabot row says only integrationTests.yaml is excluded and names WorkflowActionPinGuardUnitTest; every claim matches dependabot.yml and the workflow files"
    requirement: "REL-02"
    verification:
      - kind: other
        ref: "plan verify commands ROW-AND-TREE-OK and SCOPE-30-02-OK (exclude list, composite use, gh release create counts, --prerelease placement, Node 24 and semver pin)"
        status: pass
    human_judgment: false

duration: 2min
completed: 2026-10-24
status: complete
---

# Phase 30 Plan 02: FORK-DELTA corrections and the vnull record Summary

**The `semver.inc` defect of `increment-version.yml` measured on the real `v1.7.0_gg4` (semver 7.8.5 returns `null`, so the workflow would write `vnull`), and FORK-DELTA corrected so it describes the three fork-modified release workflows and a Dependabot list that excludes only `integrationTests.yaml`.**

## Performance

- **Duration:** about 2 min (clock readings 14:18Z to 14:20Z)
- **Tasks:** 2 of 2 (task 1 is a tracer)
- **Files modified:** 2 (`docs/FORK-DELTA.md`, `30-COMMAND-EVIDENCE.md`)

## Accomplishments

- semver `7.8.5` was installed with `--no-save --ignore-scripts` into the new empty `/tmp/gsd30/semver`; `npm view` showed no postinstall script and no deprecation. Run from the repository root with `NODE_PATH`, it printed `null` for `inc` with `patch`, `minor` and `major` and for `valid` on `v1.7.0_gg4`. The verify transcript line is `semver-d08 v1.7.0_gg4 [null,null,null,null,"7.8.5"]` (Node `v24.14.1`). The premise of D-08 holds, so no checkpoint was needed.
- E-06 records the commands, the transcript and the consequence. In `increment-version.yml`, `NEW_VERSION` becomes the string `null`, `VERSION` gets `vnull`, and `Commit and push` opens a pull request named `update-version-null`.
- The workflow paragraph after the `### Build, dependencies and CI` table no longer says the three workflows are upstream's or that `pre-release.yaml` builds with Gradle 7.4.2. It states that `integrationTests.yaml` and the composite stay as upstream, that the three files are fork-modified, and it describes the pins, the wrapper through `setup-gradle`, the GitHub CLI release, the publish conditions and the dry run. It ends with the `vnull` defect, known and unfixed, citing `30-COMMAND-EVIDENCE.md` E-06.
- The Dependabot row has the three planned replacements: the `exclude-paths` description, the closed-not-merged sentence with the D-10 note, and the Pinned-by cell `CI-15, REL-02; TestsWorkflowPinGuardUnitTest, WorkflowActionPinGuardUnitTest`.
- Every claim was cross-checked against the tree: `dependabot.yml` excludes exactly `.github/workflows/integrationTests.yaml`; no workflow names `use-clickhouse-java`; `release.yaml` and `pre-release.yaml` hold two `gh release create` lines each; `--prerelease` is in `pre-release.yaml` only; `increment-version.yml` holds `node-version: '24'` and `semver@7.8.5`.

## Task Commits

1. **Task 1: measure semver.inc, record E-06, rewrite the workflow paragraph (tracer)** - `082c127b`
2. **Task 2: Dependabot row and tree cross-check, E-07** - `2f66977a`

**Plan metadata:** the SUMMARY, STATE and ROADMAP commit follows this file.

## Clauses carried to Phase 32 (CIG-02)

Recorded in E-07 under `#### Carried to Phase 32 (CIG-02)`, not edited here:

1. The row's `Dependabot reads its configuration from the default branch (main here)` parenthesis. Stale because `origin`'s default branch is `custom_build` (`git ls-remote --symref origin HEAD`). The `test-head.yaml` row above it makes the same claim and is outside this plan's one-row scope.
2. The row's `so the entry is inert until the user changes both` clause. Stale because Dependabot commits landed on `custom_build` on 2026-10-20 and 2026-10-21 (`83de3b78`, `de27d2e9`, `2555fd2b`).

## Deviations from Plan

None - plan executed exactly as written. The plan names two stale clauses; while checking facts for them I noticed the `test-head.yaml` row repeats the default-branch claim and noted that in E-07 without editing it, as the plan's scope prohibition requires.

## Authentication Gates

None. Network access was read-only (`npm view`, `npm install` of the one pinned package, `git ls-remote`).

## Known Stubs

None.

## Issues Encountered

None. The scratch directory `/tmp/gsd30/semver` was removed after the verify passed; `/tmp/gsd30/exec/semver-d08.txt` holds the transcript.

## Threat Flags

None. T-30-11, T-30-12 and T-30-SC are mitigated as listed: claims tied to the tree by the verify, two commits touching only FORK-DELTA and the evidence file, `CHANGELOG.md` and `VERSION` equal to the planning base, exact package version installed without scripts and run only through `NODE_PATH`.

## Next Phase Readiness

- Plan 30-03 (operator dry runs, E-08 and E-09) can proceed.
- Phase 32 owns the FORK-DELTA rows for the new workflows and guards, CHANGELOG and VERSION, and the two carried clauses.

## Self-Check: PASSED

- `docs/FORK-DELTA.md` and `30-COMMAND-EVIDENCE.md` exist; E-06 and E-07 are present, E-06 reads `Matches text: yes`.
- Commits `082c127b` and `2f66977a` are ancestors of HEAD.
- Plan verification: `D08-MEASURED-OK`, `PARAGRAPH-OK`, `ROW-AND-TREE-OK` (row checks run before the E-07 commit, E-07 checks after), `SCOPE-30-02-OK`; `git diff --quiet d6a574d0 HEAD -- CHANGELOG.md VERSION` exits 0.
