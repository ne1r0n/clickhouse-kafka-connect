---
phase: 11-full-suite-ci-publication
plan: 06
subsystem: publication
tags: [publication-gate, no-ff-merge, tags, ci-06, scratch-cleanup]

requires:
  - phase: 11-full-suite-ci-publication
    provides: "11-05 scratch CI rehearsal (run 37617508949, success) and D-03 evidence bundle; 11-04 local gate; 11-08 D-17 fix"
provides:
  - "custom_build on origin at merge commit c61d90ce5dfb32ea5876dffacc8bb54d8846739a (--no-ff, 2 parents: 6ac2d20c, 586af775), pushed without force"
  - "origin's first two tags: pre-v1.6.0-sync -> 6ac2d20c (rollback point, unmoved) and v1.6.0_gg1 -> c61d90ce (matches VERSION)"
  - "CI-06 evidence on the published SHA: run 37619434848, conclusion success, 386 s run-level / 380 s job-level"
  - "Scratch branch sync/v1.6.0 deleted from origin after green; local ref kept"
  - "F-1 fixed before publish: docs/FORK-DELTA.md raw-number after rows refreshed to the measured 84/9476/451 and 30/1368/98"
affects: [11-07]

plan_head_before: 8cea7937
plan_head_after: 586af775
commits: 2

key-files:
  created: []
  modified:
    - docs/FORK-DELTA.md

key-decisions:
  - "Human approval recorded at the Task 1 blocking-human gate: the user answered 'Fix F-1, then publish (Recommended)' directly to the orchestrator (AskUserQuestion, 2026-10-07). Option text: docs-only refresh of the FORK-DELTA raw-number table, then merge/push via a clean worktree with second parent = local sync/v1.6.0; published-SHA CI run covers the final tree."
  - "Tasks 2-3 were executed inline by the orchestrator, not by the executor: the dispatched executor (correctly, by its own rules) refused to act on an approval relayed by another agent, and the approval had been given to the orchestrator directly."
  - "Second parent = local sync/v1.6.0 (586af775), not the scratch SHA 64289171, per the approved option: the merge tree equals sync/v1.6.0 byte-for-byte and equals the CI-proven 64289171 outside .planning/ and docs/FORK-DELTA.md."
  - "Merge performed in a separate clean git worktree of custom_build (/tmp/gsd11/cb, removed afterwards) so the main tree's uncommitted orchestrator files were never touched."

requirements-completed: []
---

# Plan 11-06 Summary: approve, then publish

The public fork's `custom_build` now carries the merged v1.6.0-based connector behind a single revertible merge commit, `origin` has its first two tags, CI ran the published SHA green, and the scratch branch is gone.

## Task 1: Blocking-human gate

The executor assembled D-03's evidence bundle (local gate 663/0/18 set-identical to Phase 10; scratch run 37617508949 success at 350 s against a 1350 s threshold; MERGE-CHECKLIST 0 open; invariant script exit 0; fork-delta figures re-measured; knowingly-open items R-01, WR-01, four review findings; SECURITY 07 and 09 at 0 open; D-17 fix and its residual risk T-11-38; 752 commits to publish) and stopped at the `blocking-human` checkpoint with findings F-1 (stale FORK-DELTA after rows) and F-2 (open-item lists not fully aligned; comment-only 07-REVIEW IN-01/IN-02 absent).

**Recorded approval:** the user chose **"Fix F-1, then publish (Recommended)"**, given directly to the orchestrator on 2026-10-07.

## F-1 fix (pre-publication, docs-only)

- Re-measured immediately before editing: `git diff --shortstat v1.6.0 sync/v1.6.0 -- src build.gradle.kts gradle .github` → 84 files, 9476 ins, 451 del; `-- src/main` → 30 files, 1368 ins, 98 del.
- `docs/FORK-DELTA.md`: both "after" rows, the growth sentence (9255→9476, 1358→1368, 450→451) and the src/main deletions sentence (97→98) updated; the sentence saying the figures predate plan 11-08 replaced with "measured on the tree as published"; the ledger-check sentence notes the `network_compression_method` row was added at `87fa1e78`.
- Commit `586af775` `docs(11-06): refresh FORK-DELTA raw numbers after D-17 fix (F-1)` — `docs/FORK-DELTA.md` only.
- Re-measured after the commit: unchanged (docs/ is outside both scopes).
- `git diff --quiet 64289171 HEAD -- . ':!.planning' ':!docs/FORK-DELTA.md'` → equal (TREE_EQUALS_CI_PROVEN_OUTSIDE_PLANNING_AND_FORKDELTA).

## Task 2: Forced non-fast-forward merge, tags, non-force push

Commands, run from a clean worktree of `custom_build` (`git worktree add /tmp/gsd11/cb custom_build`):

```
git status --porcelain                                   # 0 lines
git rev-list --left-right --count custom_build...sync/v1.6.0   # 0  725
git show sync/v1.6.0:VERSION                             # v1.6.0_gg1
git merge --no-ff sync/v1.6.0 -F /tmp/gsd11/merge-msg.txt
git tag v1.6.0_gg1 c61d90ce5dfb32ea5876dffacc8bb54d8846739a
git push origin custom_build                             # a295af8a..c61d90ce  (no force flag)
git push origin pre-v1.6.0-sync v1.6.0_gg1               # [new tag] x2       (no force flag)
```

Verification:
- `git rev-list --parents -n 1 c61d90ce` → 3 tokens: `c61d90ce 6ac2d20c 586af775` — a two-parent merge, no fast-forward.
- `c61d90ce^2 == sync/v1.6.0 (586af775)`; `git diff --quiet c61d90ce sync/v1.6.0` → identical; equal to `64289171` outside `.planning/` and `docs/FORK-DELTA.md`.
- `git ls-remote --heads origin custom_build` → `c61d90ce…` (= local).
- `git ls-remote --tags origin` → `pre-v1.6.0-sync 6ac2d20cc0c7…` (ROLLBACK_TAG_INTACT) and `v1.6.0_gg1 c61d90ce…`; `custom_build:VERSION` = `v1.6.0_gg1`.
- Merge message names `docs/FORK-DELTA.md` and `.planning/MERGE-CHECKLIST.md` and states `git revert -m 1 <this commit>` backs out the whole bring-forward.
- Scratch branch still on origin at end of Task 2 (64289171).

## Task 3: CI-06 evidence on the published SHA, then scratch cleanup

- Locator `head_sha=c61d90ce…` + `path=.github/workflows/tests.yaml` returned **2** runs:
  - `37619430314` — event push, head_branch `custom_build`, **cancelled**
  - `37619434848` — event push, head_branch `v1.6.0_gg1`, success
  Cause: the tag push created a second push event on the same SHA; the workflow's concurrency group is keyed on `github.sha` with `cancel-in-progress: true`, so the tag run cancelled the branch run. See Deviations.
- Narrowed locator `head_sha` + `path` + `conclusion != cancelled` → **exactly 1** match.
- **CI-06 evidence:** run **37619434848**, https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/37619434848, conclusion **success**. Duration 386 s run-level, 380 s job-level (`Run tests` step 363 s); scratch run was 350 s / 345 s (`Run tests` 326 s) — ~10% slower on a byte-identical tree, not material. Artifacts: `jacoco-coverage-reports` 816157 bytes, `gradle-test-reports` 23711100 bytes.
- `git push origin --delete sync/v1.6.0` → deleted, only after green.
- Final state: `sync/v1.6.0` absent from origin heads; origin `custom_build` = local `c61d90ce`; both tags on origin (FINAL_PUBLISHED_STATE_CONFIRMED). Local `sync/v1.6.0` kept.

## Task Commits

- `586af775` docs(11-06): refresh FORK-DELTA raw numbers after D-17 fix (F-1)
- `c61d90ce` Merge sync/v1.6.0: bring custom_build forward onto upstream v1.6.0 (on `custom_build`)

## Deviations from Plan

1. **Executor/orchestrator split.** The executor stopped at the gate and refused a relayed approval; the orchestrator, holding the user's direct answer, ran Tasks 2-3 inline.
2. **F-1 docs commit before the merge** (approved option). The merge tree therefore differs from the scratch-proven `64289171` in `docs/FORK-DELTA.md` and `.planning/` only; the published-SHA run covers the final tree.
3. **Second parent is local `sync/v1.6.0` (586af775), not `64289171`** (approved option; the plan's own automated verify asserts `M^2 == sync/v1.6.0`).
4. **Two runs on the published SHA.** The plan assumed one run per SHA, but pushing the `v1.6.0_gg1` tag onto the merge commit triggers a second push run in the same concurrency group, which cancelled the branch run. CI-06's evidence is the surviving non-cancelled run on the exact published SHA; the locator was narrowed by excluding `cancelled`, never by taking the newest. Future publications: push the tag after the branch run completes, or key the concurrency group on `github.ref`.

## Issues Encountered

- The `docker` CLI was unlinked from `/opt/homebrew/bin` after a brew upgrade (29.8.2); not needed for this plan, worked around elsewhere with `PATH=/opt/homebrew/opt/docker/bin:$PATH`.

## Next Phase Readiness

Plan 11-07 can close the milestone's last requirements on re-run evidence. Its no-`src/main` assertion carves out `fix(11-08)` (`87fa1e78`) only. Note for 11-07: `custom_build` (c61d90ce) is a merge commit; its `git log --no-merges` range check is correct for this shape.

## Self-Check: PASSED

- 2-parent merge, second parent = local sync/v1.6.0, tree identical: verified by command.
- origin custom_build = c61d90ce; both tags on origin; rollback tag at 6ac2d20c: verified by `git ls-remote`.
- Published-SHA run success, exactly one non-cancelled match: verified via REST API.
- Scratch branch absent from origin: verified.
- No force flag used on any push: commands recorded above.
