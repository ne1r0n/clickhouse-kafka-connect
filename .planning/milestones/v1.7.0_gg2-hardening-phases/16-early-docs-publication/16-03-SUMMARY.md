---
phase: 16-early-docs-publication
plan: "03"
subsystem: docs
tags: [publication, approval-gate, no-ff-merge, ci-evidence, rollout-checklist]

requires:
  - phase: 16-early-docs-publication
    provides: "Plan 16-02 approval bundle measured on X0=427a2f9d, 16-REVIEW-DISPOSITION.md open 0"
provides:
  - "origin/custom_build = 537beb1f, a two-parent --no-ff merge of sync/v1.7.0 tip 1e0eefde onto f328fd21 (docs-only outside .planning/)"
  - "Exactly one green tests.yaml run on 537beb1f (run 38074050935)"
  - "STATE.md Blockers/Concerns records the publication on the local tail"
affects: [17, 20]

actuals:
  tasks: 3
  commits: 1

tech-stack:
  added: []
  patterns:
    - "Executor runs only the blocking-human gate; the orchestrator runs the post-approval tasks inline on the user's direct answer"
    - "Merge by hash in a throwaway worktree; shape, allow-list and no-TRUNCATE checks on M before the push"

key-files:
  created:
    - .planning/phases/16-early-docs-publication/16-03-SUMMARY.md
  modified:
    - .planning/STATE.md

key-decisions:
  - "User answered \"publish\" directly to the orchestrator; Tasks 2-3 ran inline in the orchestrator."
  - "The merge is a true three-way merge (custom_build had 2 commits X lacked: release merges f328fd21 and c61d90ce), not a would-be fast-forward as the plan assumed; --no-ff was used as planned and the merged tree equals X."

requirements-completed: [PUB-06]

coverage:
  - id: D1
    description: "custom_build on origin is a non-force two-parent --no-ff merge with M^1 = f328fd21 and tree(M) = tree(X), within the allow-list, no .gsd-ws-arg"
    requirement: PUB-06
    verification:
      - kind: other
        ref: "MERGE-SHAPE-OK (Task 2 verify block 1), run before the push"
        status: pass
    human_judgment: false
  - id: D2
    description: "The merged tree carries no TRUNCATE instruction for the shared KeeperMap state table"
    requirement: PUB-06
    verification:
      - kind: other
        ref: "MERGED-TREE-NO-TRUNCATE-INSTRUCTION on M, run before the push"
        status: pass
    human_judgment: false
  - id: D3
    description: "Only custom_build was pushed; origin tags and other heads are unchanged"
    requirement: PUB-06
    verification:
      - kind: other
        ref: "CUSTOM-BUILD-PUBLISHED-ONLY (Task 2) and FINAL-REFS-OK (Task 3)"
        status: pass
    human_judgment: false
  - id: D4
    description: "M has exactly one green tests.yaml push run and no cancelled sibling, re-checked after at least 120 s"
    requirement: PUB-06
    verification:
      - kind: other
        ref: "PUBLISHED-SHA-RUN-GREEN (Task 2) and CI-STILL-ONE-RUN (Task 3)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Publication happened only after the user's own explicit approval"
    requirement: PUB-06
    verification: []
    human_judgment: true
    rationale: "The user replied \"publish\" directly in the orchestrator session; recorded verbatim below"

duration: 12min
completed: 2026-10-10
status: complete
---

# Phase 16 Plan 03: Approval gate and docs-only publication Summary

**The corrected v1.7.0_gg1 docs are on `origin/custom_build` at `537beb1f`, a single revertible `--no-ff` merge with one green CI run.**

## Approval

- Task 1 (a dispatched `gsd-executor`) re-measured the bundle on X = `1e0eefde` and returned the `checkpoint:decision` (gate `blocking-human`). It made no commits and pushed nothing.
- User's verbatim answer: `publish`. It was given directly to the orchestrator in the execute-phase session on 2026-10-10, not relayed by an agent.
- Tasks 2 and 3 ran inline in the orchestrator. No executor touched the merge or the push.

## Ground truth before the merge (Task 2 step 1)

- X = `1e0eefdefbe253e1c028cf0ff369ed8f85b33a59` (full SHA of local `sync/v1.7.0`). X0 = `427a2f9d06c3d67b13eef6697f5ba898526ad790`. X0 is an ancestor of X, and X0..X touches only `.planning/` (`e45c6387`, `1e0eefde`).
- `custom_build` local and on origin: `f328fd218dce5b9c652f8872042e922f9686b87f`.
- These checks printed on X, using plan 16-02's verify commands verbatim: ALLOW-LIST-OK, "WS-ARG-ABSENT PUSH-CONFIG-SAFE", SECRET-SCAN-CLEAN, MERGED-TREE-NO-TRUNCATE-INSTRUCTION (`md-files 17 truncate-lines 2 bad []`).
- UTC date 20261010 < 20261019. GitHub API core: 54 of 60 remaining.
- Origin snapshots: `/tmp/gsd16/origin-tags-before.txt`, `/tmp/gsd16/origin-heads-before.txt`.

## Merge

- Worktree: `git worktree add /tmp/gsd16/cb custom_build`. Status was clean and HEAD was `f328fd21`.
- Command: `git -C /tmp/gsd16/cb merge --no-ff -F /tmp/gsd16/merge-msg.txt 1e0eefdefbe253e1c028cf0ff369ed8f85b33a59`.
- M = `537beb1fcf23b987c8b340006f4f74f7c5229d00`. Parents: M^1 = `f328fd21`, M^2 = `1e0eefde`. tree(M) = tree(X) = `3f11a84d`.
- Subject: `Merge sync/v1.7.0: publish corrected v1.7.0_gg1 release docs`. The body names `docs/ROLLOUT-v1.7.0_gg1.md`, `docs/FORK-DELTA.md`, `CHANGELOG.md` and `git revert -m 1 <this commit>`.
- Pre-push checks on M: MERGE-SHAPE-OK, clean worktree status, MERGED-TREE-NO-TRUNCATE-INSTRUCTION.

## Push

Exactly as run, with no force flag and no tag refspec:

```
git -C /tmp/gsd16/cb push origin refs/heads/custom_build:refs/heads/custom_build
   f328fd21..537beb1f  custom_build -> custom_build
```

Pushed at 2026-10-10T18:00:40Z. CUSTOM-BUILD-PUBLISHED-ONLY printed.

## CI

| Run | SHA | Workflow | Event / branch | Conclusion | Started | Updated | Duration | Runner |
|---|---|---|---|---|---|---|---|---|
| [38074050935](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/38074050935) | `537beb1f` | `.github/workflows/tests.yaml` | push / custom_build | success | 18:00:42Z | 18:07:11Z | 6 m 29 s | `GitHub Actions 1000000185`, labels `["ubuntu-latest"]` |
| [38050860929](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/38050860929) (baseline) | `f328fd21` | `.github/workflows/tests.yaml` | push / custom_build | success | 12:08:09Z | 12:14:38Z | 6 m 29 s | `ubuntu-latest` |

- Job: "ClickHouse latest Client version V2 tests", success.
- PUBLISHED-SHA-RUN-GREEN printed at 18:08Z. CI-STILL-ONE-RUN printed at 18:10:44Z, more than 120 s after the green result.
- There was no D-11 deviation (no red run, no re-run) and no D-12 deviation (published before 2026-10-19).

## Final refs (FINAL-REFS-OK)

```
537beb1fcf23b987c8b340006f4f74f7c5229d00	refs/heads/custom_build
bec167d85918e77c11621063000acc6e031ae2fa	refs/heads/main
6ac2d20cc0c7feacc315b21b322325b4fee25784	refs/tags/pre-v1.6.0-sync
c61d90ce5dfb32ea5876dffacc8bb54d8846739a	refs/tags/pre-v1.7.0-sync
c61d90ce5dfb32ea5876dffacc8bb54d8846739a	refs/tags/v1.6.0_gg1
f328fd218dce5b9c652f8872042e922f9686b87f	refs/tags/v1.7.0_gg1
```

Origin's tag list is byte-identical to the before-snapshot. No tag was created, and `sync/v1.7.0` was not pushed. `release.yaml` and `pre-release.yaml` were not dispatched.

## Cleanup and local tail

- `git worktree remove /tmp/gsd16/cb` and `git worktree prune`; `git worktree list` shows only the main tree, which is on `sync/v1.7.0`.
- `git fetch origin --no-tags`: `origin/custom_build` = `537beb1f`.
- STATE.md Blockers/Concerns: the [Publish] TRUNCATE warning line, the [Publish] local-tail line and the Phase 16 sentence of the [CI] line were replaced. Commit `6c17f738` (local only). TAIL-OK printed.

## Task Commits

1. **Task 1: approval gate.** No commit, by design.
2. **Task 2: merge and push.** Merge commit `537beb1f` on `custom_build`, published.
3. **Task 3: record.** `6c17f738` docs(16-03): record the docs-only publication of the corrected v1.7.0_gg1 docs (PUB-06).

## Deviations from Plan

- **Task 2 step 2's left-count expectation was wrong; no effect on the outcome.**
  - The plan expected `git rev-list --left-right --count custom_build...X` to show 0 on the left (a would-be fast-forward). It printed `2 43`.
  - `custom_build` holds the two earlier release merges (`f328fd21`, `c61d90ce`), which X lacks, because `sync/v1.7.0` continued from `ae8520ed` (M^2 of `f328fd21`).
  - `tree(f328fd21) = tree(ae8520ed)` = `9d9a7fbd`, so the three-way merge resolves cleanly to X's tree. MERGE-SHAPE-OK verified this before the push.
  - `--no-ff` was used as planned. The user was told about this at the gate.

## Issues Encountered

None.

## Next Phase Readiness

- Phase 17 (CI, Build & Test Hygiene) can proceed.
- The local tail after `1e0eefde` stays unpublished until Phase 20.

## Self-Check: PASSED

- MERGE-SHAPE-OK, CUSTOM-BUILD-PUBLISHED-ONLY, PUBLISHED-SHA-RUN-GREEN, CI-STILL-ONE-RUN, FINAL-REFS-OK and TAIL-OK all printed.
- `git log -1 --format=%B custom_build` names all three docs and `git revert -m 1`.
