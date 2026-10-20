---
phase: 20-release-publication
plan: "07"
subsystem: release
tags: [publication, merge, tags, github-actions, rollback]

requires:
  - phase: 20-release-publication
    provides: "20-06: X0 rehearsed green on CI; the measured Approval bundle"
provides:
  - "origin/custom_build = M a246ca01, the --no-ff merge of sync/v1.7.0 (X 3879b883) onto be6b8861, tree equal to X, one green tests.yaml run 38587062864"
  - "Tags on origin: pre-v1.7.0_gg2 (be6b8861, pushed before the merge) and v1.7.0_gg2 (M, pushed only after green)"
  - "Scratch branch sync/v1.7.0 deleted from origin; FORK-DELTA raw numbers reproduced on the tag"
affects: [20-08]

actuals:
  tokens: 8000
  tasks: 3
  commits: 2

tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - .planning/STATE.md
    - .planning/phases/20-release-publication/20-COMMAND-EVIDENCE.md

key-decisions:
  - "The user answered 'publish' directly to the orchestrator at the Task 1 blocking-human gate; Tasks 2 and 3 ran inline in the orchestrator"
  - "Order held: rollback tag -> merge -> checks on M -> custom_build push -> single green run -> 120s re-check -> release tag -> scratch-branch deletion"

patterns-established: []

requirements-completed: [PUB-08]

coverage:
  - id: D1
    description: "custom_build published as one --no-ff merge M whose tree equals the rehearsed X; rollback tag on origin before anything moved"
    requirement: "PUB-08"
    verification:
      - kind: e2e
        ref: "E-70/E-71: ROLLBACK-TAG-PUSHED-NO-RUN, MERGE-SHAPE-OK, MERGED-TREE-NO-TRUNCATE-INSTRUCTION, ROLLBACK-RECHECK-ON-M-OK, CUSTOM-BUILD-PUBLISHED-ONLY"
        status: pass
    human_judgment: false
  - id: D2
    description: "M has exactly one green tests.yaml run; v1.7.0_gg2 pushed only after green and started no run; final refs exact; raw numbers reproduce on the tag"
    requirement: "PUB-08"
    verification:
      - kind: e2e
        ref: "E-72: PUBLISHED-SHA-RUN-GREEN, FINAL-REFS-OK, RAW-NUMBERS-AND-TAIL-OK; run 38587062864"
        status: pass
    human_judgment: false
  - id: D3
    description: "Publication proceeded only on the user's own explicit answer, given directly to the orchestrator and recorded verbatim before any command"
    requirement: "PUB-08"
    verification: []
    human_judgment: true
    rationale: "The approval is a human act; E-70 records the verbatim answer and the receiver"

duration: 35min
completed: 2026-10-14
status: complete
---

# Phase 20 Plan 07: Release publication Summary

**v1.7.0_gg2 is published: origin/custom_build is the --no-ff merge a246ca01 (tree = the rehearsed X) with one green tests.yaml run, tagged v1.7.0_gg2 only after green, rollback tag pre-v1.7.0_gg2 on origin first, scratch branch deleted**

## Performance

- **Duration:** about 35 min (merge + checks ~5 min; CI run ~7 min; the rest waits and records)
- **Started:** 2026-10-14T15:03Z (ground truth) — **Completed:** 2026-10-14T15:38Z (approx)
- **Tasks:** 3 (Task 1 checkpoint presented by the orchestrator; Tasks 2-3 orchestrator inline)
- **Files modified:** 2 (.planning only; both commits on the local tail, not pushed)

## The publication answer

**Verbatim:** `publish` — the user's own answer, given directly to the orchestrator at the Task 1 `blocking-human` checkpoint after 20-06-SUMMARY.md's `## Approval bundle` (items 1-8) was presented verbatim. Received by: the orchestrator, directly. Recorded in E-70 before any Task 2 command ran. Task 1 was presented by the orchestrator itself (no automated work precedes the gate; the committed bundle was quoted from 20-06-SUMMARY.md) — a deviation from the dispatched-executor shape, recorded below.

## Accomplishments

- **Rollback point first (D-15):** `git push origin refs/tags/pre-v1.7.0_gg2:refs/tags/pre-v1.7.0_gg2` (be6b8861, branch-only trigger); run count on be6b8861 unchanged after 62 s (ROLLBACK-TAG-PUSHED-NO-RUN).
- **The merge:** in `/tmp/gsd20/cb`, `git merge --no-ff -F /tmp/gsd20/merge-msg.txt 3879b883…` → M = `a246ca01b073faae86e2e92bb0288c07dd7eb0d7`, subject `Merge sync/v1.7.0: release v1.7.0_gg2`; MERGE-SHAPE-OK (two parents, M^1 = be6b8861, M^2 = X, tree(M) = tree(X), X0..X planning-only); MERGED-TREE-NO-TRUNCATE-INSTRUCTION (6 operator docs scanned, none bad); ROLLBACK-RECHECK-ON-M-OK (E-31 script against M itself).
- **The publish push:** `git -C /tmp/gsd20/cb push origin refs/heads/custom_build:refs/heads/custom_build` (15:10Z); CUSTOM-BUILD-PUBLISHED-ONLY (only custom_build moved; only pre-v1.7.0_gg2 added).
- **CI on M:** run 38587062864, push/custom_build, completed/success, 15:10:54Z→15:17:42Z (~7 min, same as X0's run 38578966373), runner ubuntu-24.04, the only run on M, zero cancelled (PUBLISHED-SHA-RUN-GREEN).
- **The tag, after green + 148 s re-check:** `git push origin refs/tags/v1.7.0_gg2:refs/tags/v1.7.0_gg2` (15:21Z); after 62 s M still had exactly one run. **Then** `git push origin --delete refs/heads/sync/v1.7.0` (15:23Z).
- **Final refs (FINAL-REFS-OK):** heads exactly custom_build at M and main (unchanged); tags exactly the six expected at their targets.
- **Raw numbers on the tag (RAW-NUMBERS-AND-TAIL-OK):** `git diff --shortstat v1.7.0 v1.7.0_gg2` = 119/18802/529 (wide) and 32/2042/143 (src/main), equal to FORK-DELTA's published gg2 rows.

## Task Commits

1. **Task 2: merge + run records (E-70, E-71)** - `904ef857` (docs)
2. **Task 3: publication record (E-72, STATE)** - `37673166` (docs)

Both on the local `sync/v1.7.0` tail; not pushed.

## Push commands as run (none with a force flag or a tag pattern beyond the named refspec)

```sh
git push origin refs/tags/pre-v1.7.0_gg2:refs/tags/pre-v1.7.0_gg2   # 15:04Z
git -C /tmp/gsd20/cb push origin refs/heads/custom_build:refs/heads/custom_build   # 15:10Z
git push origin refs/tags/v1.7.0_gg2:refs/tags/v1.7.0_gg2           # 15:21Z, after green + re-check
git push origin --delete refs/heads/sync/v1.7.0                     # 15:23Z, after the tag
```

## Decisions Made

- Task 1 presented inline by the orchestrator (bundle quoted from the committed 20-06-SUMMARY.md) instead of dispatching an executor whose only job was to echo that file back; the gate semantics (blocking-human, direct answer, verbatim record) were preserved exactly.

## Deviations from Plan

**1. [Process] Task 1 presented by the orchestrator, not a dispatched executor**
- **Found during:** Task 1
- **Issue:** Task 1 contains no automated work — only presenting a committed file verbatim; a dispatch would add a lossy round-trip
- **Fix:** the orchestrator presented the committed `## Approval bundle` verbatim and received the answer directly (strictly stronger for T-20-28: one fewer hop)
- **Files modified:** none
- **Verification:** acceptance criteria hold — bundle presented verbatim, answer recorded verbatim with receiver before any Task 2 command
- **Committed in:** E-70 records the answer and receiver

**Total deviations:** 1 (process shape). **Impact:** none on the published artifacts; the trust boundary (user → orchestrator → commands) was tightened, not loosened.

## Issues Encountered

None. The merge was clean (as rehearsed), the run was green first try, and no re-run or extra push happened.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 20-08 presents the three D-11 manual checks (Dependabot enablement, default-branch intent, exclude-paths key), records their outcomes, and closes the milestone record. The worktree is removed; the main tree is on `sync/v1.7.0` with a planning-only local tail.

---
*Phase: 20-release-publication*
*Completed: 2026-10-14*
