---
phase: 44-release-publication
plan: "05"
subsystem: release
tags: [release, publication, approval-bundle, handoffs, runbook, rehearsal, v1.7.0_gg7]

requires:
  - phase: 44-release-publication
    provides: plan 44-04 (the allow-list, the gated head b265f5a4, E-11 to E-13 and E-20) and plans 44-01 to 44-03 outputs
provides:
  - 44-MERGE-MESSAGE.txt (the message for the merge commit M)
  - 44-APPROVAL-BUNDLE.md (gates G0 to G5, eight questions, the evidence, valid only for the gated head)
  - four user handoffs 44-USER-DISPATCH-1 to 4 and 44-ORCHESTRATOR-RUNBOOK.md
  - 44-LOCAL-CLOSE-RECORD.md (requirement status, deferred must-haves, wording changes, carried items)
  - evidence E-14 (rehearsal, pass), E-15 to E-18 (pending, user-gated), E-19 (local close, pass)
affects: [phase-44-verification, milestone-close, publication-tail]

actuals:
  tokens: 26500
  tasks: 3
  commits: 6
plan_head_before: 45cbc9db5bba2e5f367eff4bb7043251587e44e2
plan_head_after: 5d6bd098c1c14ea66eee785a4660daba0da5de8e

tech-stack:
  added: []
  patterns:
    - "publication rehearsal: the real merge message and a --no-ff merge D of the tip in a throwaway detached worktree with git attributes switched off, the note's rollback block run unchanged on D, D removed and never pushed"
    - "an approval bundle that names the one head it is valid for and states the Freeze rule, so a later change outside .planning voids it"

key-files:
  created:
    - .planning/phases/44-release-publication/44-MERGE-MESSAGE.txt
    - .planning/phases/44-release-publication/44-APPROVAL-BUNDLE.md
    - .planning/phases/44-release-publication/44-USER-DISPATCH-1-SCRATCH-DRYRUN.md
    - .planning/phases/44-release-publication/44-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md
    - .planning/phases/44-release-publication/44-USER-DISPATCH-3-PUBLISH.md
    - .planning/phases/44-release-publication/44-USER-DISPATCH-4-PRETAG-CLEANUP.md
    - .planning/phases/44-release-publication/44-ORCHESTRATOR-RUNBOOK.md
    - .planning/phases/44-release-publication/44-LOCAL-CLOSE-RECORD.md
  modified:
    - .planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md

key-decisions:
  - "The CHANGELOG heading date was not edited: the bundle (question 5) and the runbook (O2, C-7) state the problem and each choice's exact consequence and recommend keeping the heading date as the freeze date; the decision is the user's"
  - "Upstream main moving (six commits, dependency bumps and cloud-test secrets) was treated as a bundle item per the plan, not a STOP: no tag past v1.7.0, pull requests 861, 855 and 857 unchanged, origin unchanged"
  - "The publication tail evidence numbers start at E-21 because E-20 is already the plan 44-04 hygiene re-run"

requirements-completed: []

coverage:
  - id: D1
    description: "The publication rehearsal on the final tip: origin unchanged, left count 0, upstream recheck 2, the merge message, a dry-run merge D with two parents and the tip's tree, the note's rollback block printing ok on D, the image equal to the suites, the date check, and the approval bundle with G0 to G5 and eight questions"
    requirement: "RLS-04"
    verification:
      - kind: other
        ref: "verify block of plan 44-05 Task 1: P44-05-T1-OK (E-14, 43 checks)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Four user handoffs (U1 and U2, U3, U4, U5) and the orchestrator runbook with the executor boundary, the Freeze rule and contingencies C-1 to C-8; E-15 to E-18 pending"
    requirement: "RLS-04"
    verification:
      - kind: other
        ref: "verify block of plan 44-05 Task 2: P44-05-T2-OK"
        status: pass
    human_judgment: false
  - id: D3
    description: "The local close record: RLS-01 to RLS-03 satisfied, RLS-04 pending and human-gated, six deferred must-haves, five wording changes, sixteen carried items; E-19"
    requirement: "RLS-04"
    verification:
      - kind: other
        ref: "verify block of plan 44-05 Task 3 (adapted for E-20, see deviations): P44-05-T3-OK and READY-FOR-USER-APPROVAL"
        status: pass
    human_judgment: false
  - id: D4
    description: "The publication itself: the user's direct approval, the merge, the single green tests.yaml run, the publish run that creates the tag and the release, the pre-tag push and the scratch deletion"
    requirement: "RLS-04"
    verification: []
    human_judgment: true
    rationale: "Every origin write and the publish dispatch happen only on the user's direct word, which no executor can receive; E-15 to E-18 stay pending by design"

duration: 12min
completed: 2026-11-02
status: complete
---

# Phase 44 Plan 05: The approval bundle, the handoffs and the local close Summary

**The publication path was rehearsed on the final tip without a push (a dry-run merge D with the real message, the note's rollback block printing `ok` on it), the approval bundle with gates G0 to G5 and eight questions, four user handoffs, the orchestrator runbook and the local close record exist, and the plan stops at ready for the user's direct approval with origin untouched.**

## Performance

- **Duration:** about 12 min
- **Started:** 2026-11-02T23:38Z
- **Completed:** 2026-11-02T23:51Z
- **Tasks:** 3
- **Files modified:** 9 (8 created, the evidence file extended) plus this SUMMARY

## Facts

- `tip_head`: `45cbc9db5bba2e5f367eff4bb7043251587e44e2` (rehearsed); the gated head `b265f5a45a0752f314bb95315e3cdfbeaa2b78cd`, the gated SHA (last commit outside `.planning`) `a43ff32f39647dfc3afb9849f6729b251f2619d2`; the tip equals the gated head outside `.planning` and under `.planning/scripts`, and still did at the close (E-19).
- `unpublished_commits`: 259 (`ahead_of_gg6` 261); `non_planning_vs_gg6`: 40 files changed, 3167 insertions, 146 deletions; `planning_vs_gg6`: 301 files changed, 30107 insertions, 1427 deletions.
- Origin `custom_build` `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`, left count 0, no `sync/*` head, no `v1.7.0_gg7` tag; open pull requests #21 (`14be67ce`) and #22 (`b5cccbd3`) unchanged.
- Upstream recheck 2: newest tag `v1.7.0` (no tag past it), pull requests #861, #855, #857 open and unchanged. **Upstream `main` moved** from `9bed6f79` to `d92a1830`: six commits merged between 2026-11-01 and 2026-11-02T23:29Z touching only `.github/workflows/tests.yaml` (cloud-test secrets) and `gradle/libs.versions.toml` (spotless 8.10.4, commons-lang3 3.21.0). Treated as a bundle item and information for the next sync (the plan says a change is not a STOP); it is not in the operator's list of STOP triggers either. The orchestrator may overrule this reading.
- `changelog_date_vs_today`: heading `2026-11-02`; equal at the measurement (2026-11-02T23:41Z) and flipping at 2026-11-03T00:00Z (see below). `d_commit`: `0209fb6a218dc5e669e2f108fd73e981ef012457` (never pushed, its worktree removed).
- D: parents `72e8ccff` and the tip, the tip's tree, subject `Merge sync/v1.7.0: release v1.7.0_gg7`, `VERSION` `v1.7.0_gg7`, baseline `v1.7.0_gg6`, rollback block exit 0 printing `ok`, `d_gradlew_bat_trap: no` (equal to E-05), revert restores D's tree. `SELECT version()` 26.10.3.7 on the digest of the suites.

## Ready for the user's direct approval

- Bundle: `.planning/phases/44-release-publication/44-APPROVAL-BUNDLE.md` (valid only for gated head `b265f5a45a0752f314bb95315e3cdfbeaa2b78cd`). Questions in one line each: (1) who runs the origin writes (default: the user); (2) go for U1 and U2, with what U1 publishes and the 40 changed files; (3) the JVM record (default JDK 17, no crash); (4) the review's end state (4 rounds, 20 findings, 19 fixed, 1 deferred, accepted without round 5); (5) the CHANGELOG heading date against UTC (recommendation: keep as the freeze date); (6) do not merge PRs #21 and #22 before M lands; (7) `integrationTest` not re-run: overrule?; (8) the F-2 progress-header claim is `derived (mechanism, not measured)`: measure first?
- **The date issue, stated for the orchestrator.** The heading must carry the merge date in UTC, and the UTC date flips at 00:00Z minutes after this plan ran; no merge can happen on 2026-11-02 UTC any more. `CHANGELOG.md` was not edited. A date edit after the gates re-opens plan 44-03's freeze: the allow-list, the Docker-free gates, both suites and the rehearsal re-run on the new head (about 2.5 hours), every approval for the older head is void, and the date can go stale again if the merge slips. The alternative, recommended in the bundle, is to keep the heading as the date the documents were frozen (E-10), recorded as a deviation in the release record; nothing in `.github` or `src/test` reads the heading. The decision is the user's, asked as bundle question (5).
- No executor runs any step of the tail: the runbook states the boundary (`## CHECKPOINT REACHED` for an executor), the Freeze rule and the relayed-approval refusal.

## Task Commits

1. **Task 1: rehearsal, merge message, approval bundle** - `adf06e15` (the message and the bundle), `827fc246` (E-14, pass)
2. **Task 2: handoffs, runbook, pending records** - `44464b7c` (five documents), `d2f56ad4` (E-15 to E-18 pending)
3. **Task 3: local close record** - `9d5c8d0d` (the record), `5d6bd098` (E-19, pass)

**Plan metadata:** the commit that carries this SUMMARY.

## Files Created/Modified

See the frontmatter; nothing outside `.planning/phases/44-release-publication/` changed (`src`, `.github`, `docs`, `CHANGELOG.md`, `VERSION` and `.planning/scripts` are byte-equal to the gated head).

## Decisions Made

See `key-decisions`. In addition, the four handoffs state that the publish dispatch is the user's under either answer to question (1), and the bundle and the runbook say the orchestrator never decides the date question.

## Deviations from Plan

**1. [Process] Evidence ids shift by one.** E-20 is already the plan 44-04 hygiene re-run, so the documents number the publication tail from E-21 (answers E-21, U1 E-22, U2 E-23, publish message and M E-24, U3 E-25, U4 E-26, publish read-back E-27, U5 E-28, final reads E-29) instead of the plan's E-20 to E-28. The handoffs, the runbook and the close record carry the shifted numbers; nobody edits an entry.

**2. [Process] Verify clauses adapted, as plan 44-04 did.** The evidence helper's `index` check demands contiguous rows up to the given number and E-20 exists, so Task 3's `index ... 19` clause fails by design (it reports 20 rows); Task 3's `rows` clause reads `E-20` in place of E-12 (E-12 stays verdict `fail`, superseded by E-20). Run with those two clauses adapted, Task 3 prints `P44-05-T3-OK` and `READY-FOR-USER-APPROVAL`. Tasks 1 and 2 print `P44-05-T1-OK` and `P44-05-T2-OK` as written. E-19's `index_rows` key carries the plan's exact string plus an `index_note` key naming E-20. The helper's `rows` check also reports the E-15 to E-18 index cell `pending (user-gated)` (the cell the plan prescribes) as differing from the entry verdict word `pending`; no verify clause runs `rows` on those entries.

**3. [Process] The plan ledger file was created after the first commit.** The `gsd-plan-head-before-44-05` file was written from the parent of the first commit (`45cbc9db`, equal to HEAD at the start), so the measured commit count is correct (6).

**4. [Judgment] `requirements-completed` is empty.** The plan declares RLS-04, but its publication half is pending and human-gated and the plan forbids marking it satisfied; copying the ID would let downstream tooling flip it. RLS-04 is deliberately not marked complete.

**Total deviations:** 4 process or judgment deviations, 0 auto-fixed. **Impact on plan:** none on the artifacts; no code or document outside `.planning` changed.

## Issues Encountered

- The date flip described above (a decision for the user, not a defect).
- Upstream `main` moved after the first recheck (information, see Facts).

## Known Stubs

None. (Angle-bracket values such as `<M>` and `<X0>` in the handoffs are the deliberate placeholders the orchestrator fills in after it builds M.)

## Threat Flags

None. No file added introduces a network endpoint, auth path or schema; the documents describe origin writes and perform none.

## Handoffs

- **Orchestrator:** present `44-APPROVAL-BUNDLE.md` verbatim and follow `44-ORCHESTRATOR-RUNBOOK.md` (O1 to O7, contingencies C-1 to C-8). Record the tail as E-21 to E-29 in order. Apply the five wording changes of `44-LOCAL-CLOSE-RECORD.md`. RLS-04 flips only when E-21 to E-29 show the publication. The 16 carried items are in the close record's table.
- **STATE.md and ROADMAP.md:** updated by the state verbs once; `.planning/config.json` and `.planning/state.json` remain modified and uncommitted (orchestrator-managed).
- **Evidence entries E-15 to E-18** stay pending forever.

## Next Phase Readiness

Phase 44 is local-complete; the next step is the user's direct approval of the bundle.

## Self-Check: PASSED

The nine files exist; commits `adf06e15`, `827fc246`, `44464b7c`, `d2f56ad4`, `9d5c8d0d` and `5d6bd098` are ancestors of HEAD; the verifies print `P44-05-T1-OK`, `P44-05-T2-OK` and (adapted) `P44-05-T3-OK` with `READY-FOR-USER-APPROVAL`; origin `custom_build` still reads `72e8ccff`, no `sync/*` head, no `v1.7.0_gg7` tag, no `refs/gsd/release-m-v1.7.0_gg7`, one worktree; nothing was pushed, merged, tagged or dispatched.

---
*Phase: 44-release-publication*
*Completed: 2026-11-02*
