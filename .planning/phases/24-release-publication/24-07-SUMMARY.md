---
phase: 24-release-publication
plan: "07"
subsystem: release
tags: [publication, merge, tag, github-actions, rollback]

requires:
  - phase: 24-release-publication
    provides: "24-06: the gated tree, the green rehearsal run on X0=3ab55e1d and the measured approval bundle"
provides:
  - "v1.7.0_gg3 published: origin/custom_build = M = 3376a18fd057dba329ceae7d8c74d1ba4e3d36f6, a --no-ff merge of sync/v1.7.0 (X=8b1c318d) onto 6473e24f, with one green tests.yaml run 39472696952"
  - "Tags on origin: pre-v1.7.0_gg3 (6473e24f, pushed first) and v1.7.0_gg3 (M, pushed after green); scratch branch sync/v1.7.0 deleted from origin"
  - "Evidence E-70, E-71, E-72 and the STATE.md publication record on the local tail"
affects: [milestone v1.7.0_gg3-hardening close]

actuals:
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - .planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md
    - .planning/STATE.md

key-decisions:
  - "The user answered 'publish' directly to the orchestrator at the blocking-human gate; Tasks 2 and 3 ran inline in the orchestrator, never in a subagent"
  - "The merge message words the shipped changes as the CHANGELOG does (conversion errors on the RowBinary route, the Map-key exception, the backslash-n / line-break fix, the WARN and its known issue)"

patterns-established: []

requirements-completed: [PUB-10]

coverage:
  - id: D1
    description: "Rollback tag pre-v1.7.0_gg3 on origin at 6473e24f before anything else moved, starting no CI run"
    requirement: "PUB-10"
    verification:
      - kind: e2e
        ref: "E-70: ROLLBACK-TAG-PUSHED-NO-RUN"
        status: pass
    human_judgment: false
  - id: D2
    description: "The --no-ff merge M of X onto 6473e24f has the rehearsed shape, carries no TRUNCATE instruction, passes the rollback re-check, and was the only origin change at push time"
    requirement: "PUB-10"
    verification:
      - kind: e2e
        ref: "E-71: MERGE-SHAPE-OK, MERGED-TREE-NO-TRUNCATE-INSTRUCTION, ROLLBACK-RECHECK-ON-M-OK, CUSTOM-BUILD-PUBLISHED-ONLY"
        status: pass
    human_judgment: false
  - id: D3
    description: "M has exactly one green tests.yaml run; the v1.7.0_gg3 tag names M; origin's final heads and tags are exactly the expected set; FORK-DELTA's gg3 raw numbers reproduce on the tag"
    requirement: "PUB-10"
    verification:
      - kind: e2e
        ref: "E-71/E-72: PUBLISHED-SHA-RUN-GREEN, FINAL-REFS-OK, RAW-NUMBERS-AND-TAIL-OK"
        status: pass
    human_judgment: false
  - id: D4
    description: "The publication happened only on the user's own explicit answer, given directly to the orchestrator and recorded verbatim"
    requirement: "PUB-10"
    verification: []
    human_judgment: true
    rationale: "The approval itself is a human act; E-70 records the verbatim answer and the receiver"

duration: 25min
completed: 2026-10-20
status: complete
---

# Phase 24 Plan 07: Publish v1.7.0_gg3 Summary

**`v1.7.0_gg3` is published: origin `custom_build` = `3376a18f`, a `--no-ff` merge of the rehearsed tree with one green CI run, the rollback tag pushed first and the release tag pushed only after green**

## Performance

- **Duration:** about 25 min (the checkpoint return about 1 min; the publication itself, re-check to final refs, 18:10:42Z-18:22:23Z; the rest was records)
- **Started:** 2026-10-20T18:04:51Z
- **Completed:** 2026-10-20T18:30:00Z (approx)
- **Tasks:** 3 (Task 1 checkpoint returned by a subagent; Tasks 2 and 3 orchestrator inline)
- **Files modified:** 2 (24-COMMAND-EVIDENCE.md, STATE.md)

## The publication answer

**Verbatim:** `publish`. It is the user's own answer, given directly to the orchestrator at the Task 1 `blocking-human` checkpoint after the `## Approval bundle` of 24-06-SUMMARY.md (items 1-8) and every push command were presented. Tasks 2 and 3 ran inline in the orchestrator, and no subagent held push authority. The answer is recorded in E-70.

## Identities

- X0 = `3ab55e1da878c6619f711d1220793e9d2456fa9d` (rehearsed, run 39470352668 green)
- X = `8b1c318d52880de07571329d202f26cf322ed043` (local `sync/v1.7.0` at merge time; X0..X `.planning/`-only)
- M = `3376a18fd057dba329ceae7d8c74d1ba4e3d36f6`, parents `6473e24f372a8b37dfd8cc8cf3124e6cff52edca` and X, tree equal to X
- Merge subject: `Merge sync/v1.7.0: release v1.7.0_gg3`

## Every push, as run (UTC, 2026-10-20)

| Time | Command | Result |
|------|---------|--------|
| 18:10:45 | `git push origin refs/tags/pre-v1.7.0_gg3:refs/tags/pre-v1.7.0_gg3` | `[new tag]`, no run started (checked at 77 s) |
| 18:12:12 | `git -C /tmp/gsd24/cb push origin refs/heads/custom_build:refs/heads/custom_build` | `6473e24f..3376a18f` |
| 18:20:56 | `git push origin refs/tags/v1.7.0_gg3:refs/tags/v1.7.0_gg3` | `[new tag]`, no run started (checked at 73 s) |
| 18:22:20 | `git push origin --delete refs/heads/sync/v1.7.0` | `[deleted]` |

None used a force flag, `--tags`, `--follow-tags`, `--all` or `--mirror`.

## CI

| Run | SHA | Branch | Result | Duration | Runner |
|-----|-----|--------|--------|----------|--------|
| [39470352668](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/39470352668) (rehearsal) | X0 | `sync/v1.7.0` | success | about 6 min (17:56:03Z-18:02:09Z) | `ubuntu-24.04` |
| [39472696952](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/39472696952) (release) | M | `custom_build` | success | about 5 min (18:12:17Z-18:17:32Z) | `ubuntu-24.04` |

Each SHA has exactly one `tests.yaml` run, and none was cancelled. The late re-check came 135 s after green.

## Checks on M

- MERGE-SHAPE-OK, MERGED-TREE-NO-TRUNCATE-INSTRUCTION (7 operator docs scanned, none flagged) and ROLLBACK-RECHECK-ON-M-OK all passed before the push.
- The rollback re-check ran `/tmp/gsd24/rollback-demo.sh` against M itself: `ok`, the rollout doc gone from the branch, item-7 headings 1, the rolled-back `tests.yaml` keeps `branches: ['**']`, and `git revert` restores the target tree.
- After the publication: CUSTOM-BUILD-PUBLISHED-ONLY, PUBLISHED-SHA-RUN-GREEN, FINAL-REFS-OK and RAW-NUMBERS-AND-TAIL-OK.

## Final origin refs

- **Heads** (excluding `dependabot/*`): `custom_build` at `3376a18f`, and `main` at `bec167d8` (unchanged). The five `dependabot/*` heads did not change.
- **Tags:** `pre-v1.6.0-sync`, `pre-v1.7.0-sync`, `v1.6.0_gg1`, `v1.7.0_gg1`, `pre-v1.7.0_gg2`, `v1.7.0_gg2`, `pre-v1.7.0_gg3` (6473e24f) and `v1.7.0_gg3` (3376a18f).

## Task Commits

1. **Task 1: publication checkpoint.** No files.
2. **Task 2: merge and CI record (E-70, E-71).** Commit `c28b7e6f` (docs).
3. **Task 3: publication record (E-72, STATE.md).** Commit `938d14af` (docs).

**Plan metadata:** see close-out commit

## Files Created/Modified

- `.planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md`: E-70 (rollback tag), E-71 (merge, checks on M, publish push, CI), E-72 (late re-check, release tag, deletion, final refs, raw numbers)
- `.planning/STATE.md`: a dated [Publish] line for gg3, the old [Publish] line updated, and the [Phase 23] 999.4 line updated to name where the release discloses it

## Decisions Made

- The merge message words the shipped changes the way the CHANGELOG does. A first draft said writer errors and DLQ headers "carry no record values"; the CHANGELOG limits that to conversion errors on the RowBinary route and keeps the DLQ class-name and message headers unchanged. The draft was rewritten before the merge, and nothing of it was committed.

## Deviations from Plan

- **Rollback-tag check timing.** The plan asks for a wait of at least 60 s before verify command 1. The first two runs came at 26 s and 54 s. A third run at 77 s, before any further push, also printed ROLLBACK-TAG-PUSHED-NO-RUN, so the plan's condition held before `custom_build` was pushed. Steps 3-6 (worktree, merge message, local merge, checks on M) ran during the wait; they are local only.
- Otherwise the plan was executed as written. The D-10 contingency did not fire (no E-73), and no CI-log token was needed.

## Issues Encountered

None.

## User Setup Required

These are the three open user items from `24-MILESTONE-CLOSE-RECORD.md` (D-11). They are reminders only, in the GitHub web UI, and nothing waits on them:

1. **The `dependencies` label.** Go to Issues → Labels → New label and create `dependencies`. Alternatively, drop the two `labels:` requests from `.github/dependabot.yml` in a later milestone.
2. **PRs #4 and #9, the fork-only pins.** #4 is client 0.9.6 → 0.10.0 and #9 is `connect-api` 4.1.1 → 4.4.0. Close them, or leave them open, and consider `ignore` entries in `dependabot.yml` later.
3. **PRs #6, #7 and #8** (commons-lang3 3.21.0, ben-manes versions 0.66.0, Gradle wrapper 9.8.1). Merge or close each when ready now that `v1.7.0_gg3` is published. Any merge diverges the next milestone's base, which then opens with a DEPS-01-style merge.

## Next Phase Readiness

- Phase 24's plans are complete. Next come phase verification, then `/gsd-audit-milestone`, then `/gsd-complete-milestone`.
- Commits after X (`c28b7e6f`, `938d14af` and this close-out) touch only `.planning/` and stay a local tail on `sync/v1.7.0`.

## Self-Check: PASSED

- E-70, E-71 and E-72 are in `24-COMMAND-EVIDENCE.md`, and commits `c28b7e6f` and `938d14af` exist on `sync/v1.7.0`.
- `git ls-remote origin` shows `custom_build` = `3376a18f`, the tags `v1.7.0_gg3` and `pre-v1.7.0_gg3`, and no `sync/v1.7.0`.

---
*Phase: 24-release-publication*
*Completed: 2026-10-20*
