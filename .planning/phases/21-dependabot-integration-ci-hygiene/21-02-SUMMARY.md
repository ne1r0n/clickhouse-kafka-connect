---
phase: 21-dependabot-integration-ci-hygiene
plan: "02"
subsystem: build
tags: [dependabot, git-merge, lombok, spotless, slf4j-reload4j, anchors-gate, gradle]

requires:
  - phase: 21-dependabot-integration-ci-hygiene
    provides: "21-01: 21-EVIDENCE.md with the index and E-01, E-02"
provides:
  - "Merge commit 82524e12 on sync/v1.7.0: parents 54d19dc5 and 6473e24f (origin/custom_build), carrying lombok 1.18.48, spotless 8.10.4 and slf4j-reload4j 2.0.20"
  - "D-03 bookkeeping inside the merge: fork-anchors.txt BLD-08 slf4j-reload4j row at 2.0.20, FORK-DELTA BLD-08 and BLD-14 rows naming 2.0.20 and Lombok 1.18.48"
  - "E-03..E-07 in 21-EVIDENCE.md: merge facts, the gate with exactly one violation, slf4j resolution before and after, testUnit and the Lombok warning check, spotless baseline-equal"
  - "pre_merge_head 54d19dc506a8b43f561b69d6b48cc4d3a2218910, the local rollback point for D-03"
affects: [21-03 exemption of 6473e24f, 21-05 phase close gate, 22, 23, 24 rollout note and push]

actuals:
  tokens: 9000
  tasks: 2
  commits: 10

plan_head_before: 54d19dc506a8b43f561b69d6b48cc4d3a2218910
plan_head_after: 6147fa223d9f2363c35e19ca4600177ab33f7e2e

tech-stack:
  added: []
  patterns:
    - "Merge that carries its own bookkeeping edits so the merge commit satisfies its own anchors file (D-03)"
    - "Baseline-equal spotless check: identical git-diff md5 in throwaway worktrees before and after, no tree-wide reformat commit (D-14)"

key-files:
  created:
    - .planning/phases/21-dependabot-integration-ci-hygiene/21-02-SUMMARY.md
  modified:
    - build.gradle.kts
    - gradle/libs.versions.toml
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - .planning/phases/21-dependabot-integration-ci-hygiene/21-EVIDENCE.md

key-decisions:
  - "Merged origin/custom_build only because it was exactly 6473e24f at start (ls-remote and fetch agreed); a rehearsal in a throwaway worktree staged exactly build.gradle.kts and gradle/libs.versions.toml with no unmerged paths"
  - "The three D-03 bookkeeping edits were made between merge --no-commit and commit in one bash -c, so the merge differs from its first parent in exactly four files (1/1, 1/1, 2/2, 2/2)"
  - "The unmodified anchors gate reports exactly one violation, at 6473e24f, so 6473e24f is the only commit plan 21-03 must exempt"
  - "No reformat commit: spotlessApply gives the identical 46-file diff (md5 3393873370f9b48d2239131eedba8ff8) before and after the merge"

patterns-established:
  - "Evidence timestamps come from file mtimes and commit times of the artifacts, not estimates"

requirements-completed: [DEPS-01]

coverage:
  - id: D1
    description: "The three Dependabot commits are in sync/v1.7.0 by a two-parent merge whose second parent is 6473e24f, with no rebase and no force; origin/custom_build has 0 commits HEAD lacks"
    requirement: DEPS-01
    verification:
      - kind: other
        ref: "21-02 Task 1 automated verify (MERGE-SHAPE-OK)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The unmodified anchors gate on the merged tree reports exactly one ANCHOR VIOLATION line, at 6473e24f for the slf4j-reload4j BLD-08 row, and result=FAIL"
    requirement: DEPS-01
    verification:
      - kind: other
        ref: "21-02 Task 1 automated verify (GATE-ONE-VIOLATION)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The build resolves slf4j-api 2.0.20 on runtimeClasspath, testRuntimeClasspath and integrationTestRuntimeClasspath, clean-compiles four source sets with no Lombok ambiguity warning, and testUnit passes with 0 failed"
    requirement: DEPS-01
    verification:
      - kind: other
        ref: "21-02 Task 2 automated verify (BUILD-EVIDENCE-OK)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Spotless 8.10.4 adds no violation beyond the 8.10.1 baseline: identical spotlessApply diff before and after the merge, no reformat commit"
    requirement: DEPS-01
    verification:
      - kind: other
        ref: "21-02 Task 2 automated verify (SPOTLESS-BASELINE-EQUAL)"
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-10-17
status: complete
---

# Phase 21 Plan 02: Dependabot merge (DEPS-01) Summary

**Dependabot's lombok 1.18.48, spotless 8.10.4 and slf4j-reload4j 2.0.20 merged into sync/v1.7.0 by a two-parent merge (82524e12) that carries its own anchors and FORK-DELTA edits, leaving the anchors gate with exactly one predicted violation at 6473e24f**

## Performance

- **Duration:** 8 min
- **Started:** 2026-10-17T08:40:12Z
- **Completed:** 2026-10-17T08:48Z
- **Tasks:** 2
- **Files modified:** 5 (four by the merge, plus 21-EVIDENCE.md)

## Accomplishments

- Merge commit `82524e12969cdcd8345835c1082b30b04da31c5a` with parents `54d19dc5` (the previous sync tip) and `6473e24f372a8b37dfd8cc8cf3124e6cff52edca`. `git rev-list --left-right --count origin/custom_build...HEAD` prints `0 27`. The first-parent merge list is the new merge, then `54e63648`.
- D-03: `fork-anchors.txt` BLD-08 row now `build.gradle.kts<TAB>slf4j-reload4j:2.0.20<TAB>1<TAB>BLD-08`; `docs/FORK-DELTA.md` BLD-08 and BLD-14 rows name `org.slf4j:slf4j-reload4j:2.0.20` and Lombok 1.18.48. The merge message says these edits are in neither parent.
- The unmodified gate `verify-merge-invariants.sh 54e63648 HEAD` exits 1 with one stderr line, `ANCHOR VIOLATION: 6473e24f build.gradle.kts 'slf4j-reload4j:2.0.17' expected 1 got 0 (BLD-08)`. That is plan 21-03's only exemption.
- slf4j-api resolves to 2.0.20 on all three classpaths (before: 2.0.17, 2.0.17, 2.0.18); the catalog line `slf4j = "2.0.18"` is unchanged. This is the Phase 24 rollout note.
- Clean compile of main, test, integrationTest and testFixtures is quiet (0 `ambiguous` lines), while the control without `lombok.config` prints `Ambiguous: Jackson2 and Jackson3`. testUnit: SUCCESS, 556 tests, 554 succeeded, 0 failed, 2 skipped.
- Spotless baseline-equal (D-14): md5 `3393873370f9b48d2239131eedba8ff8`, 46 files, `260 insertions(+), 337 deletions(-)` both before and after.

## Task Commits

1. **Task 1: DEPS-01 merge end to end (tracer)** - `82524e12` (merge commit, the task's commit)
2. **Task 2: Build evidence E-03..E-07** - `6147fa22` (docs)

**Plan metadata:** the `docs(21-02): complete ...` commit that follows this file.

## Files Created/Modified

- `build.gradle.kts` - slf4j-reload4j 2.0.20 (by merge)
- `gradle/libs.versions.toml` - lombok 1.18.48 and spotless 8.10.4 (by merge)
- `.planning/scripts/fork-anchors.txt` - BLD-08 slf4j-reload4j row at 2.0.20 (D-03, in the merge commit)
- `docs/FORK-DELTA.md` - BLD-08 and BLD-14 rows (D-03, in the merge commit)
- `.planning/phases/21-dependabot-integration-ci-hygiene/21-EVIDENCE.md` - E-03..E-07 and their index verdicts

## Decisions Made

- Followed the plan as specified. `pre_merge_head` is `54d19dc506a8b43f561b69d6b48cc4d3a2218910`; a local `git reset --hard` to it redoes D-03 until Phase 24 publishes the merge.
- The `actuals.commits: 10` figure is measured as `git rev-list --count 54d19dc5..HEAD` (plan_head_before to the evidence commit). It includes the merged-in `origin/custom_build` commits, not only the two commits this plan authored (the merge and the evidence commit).

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- `testUnit --rerun` finished in 4 seconds, reporting 556 tests; the count equals the research measurement, so no further check was made.
- Evidence timestamps were first written as estimates and were then corrected to the artifacts' real mtimes and commit time before the evidence commit.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None.

## Next Phase Readiness

- Plan 21-03 can exempt `6473e24f` for the BLD-08 slf4j-reload4j row (E-04 is its input).
- Nothing was pushed; `custom_build` was never checked out or committed to.
- `.planning/state.json` and `.planning/milestone.lock` were left unstaged throughout.

---
*Phase: 21-dependabot-integration-ci-hygiene*
*Completed: 2026-10-17*

## Self-Check: PASSED

- FOUND: `.planning/phases/21-dependabot-integration-ci-hygiene/21-EVIDENCE.md`
- FOUND commits: `82524e12`, `6147fa22` (ancestors of HEAD)
- Both Task 1 and both Task 2 automated verify commands printed their markers: MERGE-SHAPE-OK, GATE-ONE-VIOLATION, BUILD-EVIDENCE-OK, SPOTLESS-BASELINE-EQUAL.
