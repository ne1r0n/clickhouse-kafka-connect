---
phase: 15-suite-gate-publication-rollout
plan: "08"
subsystem: publication
tags: [publication, release, custom_build, tags, ci-08, github-rest-api]

requires:
  - phase: 15-suite-gate-publication-rollout
    provides: "plan 15-07: the rehearsed SHA X = ae8520ed on origin's sync/v1.7.0, its green CI run, and the D-12 approval bundle"
provides:
  - "origin custom_build = M = f328fd21, a forced --no-ff merge of X onto c61d90ce, tree byte-identical to X"
  - "origin tags v1.7.0_gg1 (M) and pre-v1.7.0-sync (c61d90ce)"
  - "One green tests.yaml run on M (38050860929), CI-08 proven after the tag push"
  - "Recorded Fact 40 and the D-14 STATE note on the local tail"
affects: [milestone-completion]

plan_head_before: caa9a70b0517ebfc019b27c744c78f0d87f09ad1
plan_head_after: 6a320cd7

actuals:
  tasks: 3
  commits: 1

tech-stack:
  added: []
  patterns:
    - "Merge and push the protected branch from a throwaway worktree so the main tree stays on the planning branch"
    - "Push tags by name only after the published SHA's own CI run is green, then re-check that SHA for tag-triggered or cancelled runs"

key-files:
  created:
    - .planning/phases/15-suite-gate-publication-rollout/15-08-SUMMARY.md
  modified:
    - .planning/MERGE-CHECKLIST-v1.7.0.md
    - .planning/STATE.md

key-decisions:
  - "Task 1 (the approval gate) was presented by the orchestrator directly from 15-07-SUMMARY's approval bundle instead of through a dispatched executor; the user answered the orchestrator directly with \"publish\""
  - "Local sync/v1.6.0 deleted after confirming 3b6736e3 is contained in origin/custom_build; local sync/v1.7.0 kept and checked out (D-14)"
  - "release.yaml and pre-release.yaml not dispatched: publication is branch plus tags"

requirements-completed: [PUB-03, PUB-04]

duration: ~15min
completed: 2026-10-10
---

# Phase 15 Plan 08: Publish v1.7.0_gg1 Summary

**`v1.7.0_gg1` is published: origin's `custom_build` is the forced `--no-ff` merge `f328fd21` of the rehearsed tree `ae8520ed`, CI ran it once green, and the release and rollback tags are on origin.**

## Performance

- **Duration:** about 15 min (approval to records)
- **Completed:** 2026-10-10
- **Tasks:** 3 (Task 1 checkpoint, Tasks 2 and 3 run inline by the orchestrator)
- **Files modified:** 2 plus this SUMMARY

## Approval

Given by the user directly to the orchestrator at the Task 1 gate, verbatim: `publish`.

## Accomplishments

- **Ground truth (Task 2 step 1):** origin's `sync/v1.7.0` = X = `ae8520ed2ae848cb131e22a729ca670571782d55`. Origin's `custom_build`, local `custom_build` and `pre-v1.7.0-sync` are all `c61d90ce5dfb32ea5876dffacc8bb54d8846739a`. `git show X:VERSION` reads `v1.7.0_gg1`. `.planning/.gsd-ws-arg` is absent from X's tree and from `c61d90ce..X`. `3b6736e3` is an ancestor of X. `X..sync/v1.7.0` touches only `.planning/`. `push.followTags` is unset and the REST rate limit was 60.
- **Merge:** run in the worktree `/tmp/gsd15/cb` as `git merge --no-ff -F /tmp/gsd15/merge-msg.txt ae8520ed2ae848cb131e22a729ca670571782d55`. M = `f328fd218dce5b9c652f8872042e922f9686b87f`, with parents `c61d90ce` and X. `git diff --quiet M X` held, and the check printed MERGE-SHAPE-OK. The message names `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg1.md`, `.planning/MERGE-CHECKLIST-v1.7.0.md`, X, the rehearsal run URL and `git revert -m 1`.
- **Branch push:** `git -C /tmp/gsd15/cb push origin refs/heads/custom_build:refs/heads/custom_build`, with no force flag. It moved `c61d90ce..f328fd21`, and origin's tags were unchanged afterwards (CUSTOM-BUILD-PUBLISHED-TAGS-UNCHANGED).
- **CI on M:** exactly one `tests.yaml` run, [38050860929](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/38050860929), on a push to `custom_build`. It succeeded in 389 s with a 385 s job, against the rehearsal's 391 s and 386 s (PUBLISHED-SHA-RUN-GREEN).
- **Tags:** `git tag v1.7.0_gg1 f328fd21` (lightweight), then `git push origin refs/tags/v1.7.0_gg1 refs/tags/pre-v1.7.0-sync`.
- **Post-tag re-check, more than 150 s later:**
  - M still has exactly one run, none cancelled and none with head_branch `v1.7.0_gg1` (CI08-END-TO-END-OK).
  - `c61d90ce` gained run 38051333950 for head_branch `pre-v1.7.0-sync`, which was `in_progress` when recorded. This is the expected run under the old pre-CI-08 workflow and was not gated on.
- **Scratch branch:** `git push origin --delete sync/v1.7.0`.
- **Final origin state (FINAL-REFS-OK):**
  - Heads: `custom_build` = `f328fd21` and `main` = `bec167d8`.
  - Tags: exactly `pre-v1.6.0-sync`, `pre-v1.7.0-sync`, `v1.6.0_gg1` and `v1.7.0_gg1`, each at its expected target.
- **Cleanup:**
  - The worktree is removed.
  - `git fetch --no-tags origin` ran, and `3b6736e3` is contained in `origin/custom_build`.
  - Local `sync/v1.6.0` is deleted. Local `sync/v1.7.0` is kept and checked out.
- **Records:** Recorded Fact 40 and the dated D-14 STATE note are committed on the local tail, not pushed.

## Task Commits

1. **Task 1: approval gate.** No commit. The approval is recorded verbatim in Fact 40.
2. **Task 2: merge and `custom_build` push.** No commit on `sync/v1.7.0`. The merge commit is `f328fd21` on `custom_build`.
3. **Task 3: tags, CI-08 re-check, cleanup and records.** Commit `6a320cd7`, `docs(15-08): record the v1.7.0_gg1 publication (PUB-03, PUB-04, D-14)`.

## Files Created/Modified

- `.planning/MERGE-CHECKLIST-v1.7.0.md`: Recorded Fact 40.
- `.planning/STATE.md`: the [Publish] item now holds the dated D-14 publication note, and the [CI] item records CI-08 as proven end to end.

## Decisions Made

See `key-decisions` in the frontmatter.

## Deviations from Plan

- **Task 1 ran in the orchestrator.** The checkpoint was presented by the orchestrator, from the 15-07-SUMMARY approval bundle with the key items condensed and the full bundle referenced, not by a dispatched executor. The plan's security property holds: only the user's direct answer authorised Tasks 2 and 3.
- **Left/right count was `1 172`, not `0 172`.** Task 2 step 2 expected `git rev-list --left-right --count custom_build...X` to print 0 on the left.
  - The cause: `c61d90ce` is the v1.6.0 merge commit, and X descends from its second parent `586af775`, whose tree equals `c61d90ce`'s. X does not descend from the merge commit itself.
  - The forced `--no-ff` merge was used regardless. The parent checks, the tree-equality check and the ancestor check all passed, so the published tree is the rehearsed tree.

## Issues Encountered

None. The rollback-tag run 38051333950 on `c61d90ce` was still running when recorded. The plan treats it as noise on a different SHA and concurrency group, and it is attributed only if the user asks.

## Threat Flags

- T-15-23: publication happened only after the verbatim direct approval, and Tasks 2 and 3 ran in the orchestrator.
- No force flag was used anywhere.
- No tag other than the two named was pushed. The 67 local-only tags stay local, and `--tags`/`--follow-tags` were never used.

## Next Phase Readiness

- Phase 15's execute-phase gates (code review, verifier) and `/gsd-complete-milestone` commit to the local tail only (D-14).
- A finding that needs a `src/main` change is now a new user decision, not a post-publication edit.

## Self-Check: PASSED

- `git rev-parse custom_build` = `f328fd21`, which equals origin's `custom_build`. `M^1` = `c61d90ce` and `M^2` = `ae8520ed`.
- `git ls-remote --tags origin` lists 4 tags. `v1.7.0_gg1` = `f328fd21` and `pre-v1.7.0-sync` = `c61d90ce`.
- `git ls-remote --heads origin` has no `sync/v1.7.0`.
- `git rev-parse --verify --quiet sync/v1.6.0` prints nothing. `sync/v1.7.0` resolves, and HEAD is on it.
- `git worktree list` shows only the main tree.
- Commit `6a320cd7` exists and is not on origin.
