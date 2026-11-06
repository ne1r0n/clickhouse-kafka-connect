---
phase: 49-release-publication
plan: "05"
subsystem: release
tags: [release, publication, rehearsal, approval-bundle, handoffs, runbook, local-close, user-gated]
status: complete

requires:
  - phase: 49-release-publication
    provides: plans 49-01 to 49-04 (VERSION v1.7.0_gg8, the frozen release documents, the 17-row allow-list, the gates E-12 and E-13)
provides:
  - 49-MERGE-MESSAGE.txt, the message for the merge commit M'
  - 49-APPROVAL-BUNDLE.md, gates G0 to G7 with one word per origin step and eleven questions, valid for one gated head
  - five user handoffs (U1 and U2, U3, U4, U5a and U5b, U6) and the orchestrator runbook with contingencies C-1 to C-9
  - 49-LOCAL-CLOSE-RECORD.md, requirement status, deferred must-haves, nine wording changes, twenty carried items
  - evidence entries E-14 (pass), E-15 to E-19 (pending, user-gated), E-20 (pass, local gate)
affects: [phase-49-tail, milestone-close]

requirements: [RLS-04, RLS-05, RLS-06]
requirements-completed: [RLS-04]

coverage:
  - id: D1
    description: "The publication path rehearsed on the final tip without a push: dry-run merge in a throwaway worktree, frozen script in both modes, rollback block prints ok, upstream recheck 3"
    requirement: "RLS-04"
    verification:
      - kind: other
        ref: "bash /tmp/gsd49/escripts/t1verify.sh (the plan's Task 1 verify, prints P49-05-T1-OK); E-14 61 checks"
        status: pass
    human_judgment: false
  - id: D2
    description: "The approval bundle and the five handoffs plus the orchestrator runbook, one word per origin step"
    requirement: "RLS-05"
    verification: []
    human_judgment: true
    rationale: "The words, the questions and the commands are the user's to read and answer; the structure check passes (P49-05-T2-OK) but only the user's direct approval completes RLS-05"
  - id: D3
    description: "The local close record: RLS-05 and RLS-06 recorded as pending and human-gated, deferred must-haves, wording changes, carried items"
    requirement: "RLS-06"
    verification: []
    human_judgment: true
    rationale: "RLS-06 is completed only by the separate confirmation and the deletion (U6); nothing here can show it"

actuals:
  tokens: 31400
  tasks: 3
  commits: 6
plan_head_before: 3699a9cd617cdb01950bff86603e60eb8ae75725
plan_head_after: f1c37c1bd6dacc12f419c536d1b83222701f59b4

key-decisions:
  - "Eleven questions in the bundle, not the plan's ten: the release dry run U2 keeps its own word G3b (question 4) and the deletion of sync/v1.7.0 on origin gets its own question (11) and word G6b, as the orchestrator asked"
  - "requirements-completed lists RLS-04 only: RLS-05 and RLS-06 are pending and human-gated, so they are not copied from the plan's requirements array"

duration: 14min
completed: 2026-11-06
---

# Phase 49 Plan 05: Publication rehearsal, approval bundle, handoffs and local close Summary

The publication path was rehearsed on the final tip without a push (a `--no-ff` merge of the tip onto `94834763` in a throwaway detached worktree, the frozen script passing in both modes, the note's rollback block printing `ok`), the approval bundle, five handoffs and the orchestrator runbook were written, and the phase was closed locally with RLS-05 and RLS-06 pending. Nothing was pushed, merged, tagged, dispatched or deleted: this plan is ready for the user's direct approval.

## Facts

- **tip_head:** `3699a9cd617cdb01950bff86603e60eb8ae75725` (the rehearsed tip, E-14). **Gated head:** `d9b10b9bf670b8236119c725e3639c53ad3e570b`; **gated SHA** (last commit outside `.planning`): `315f5de1c85fb61bd2a1e887d2be085bc62571ce`. `tip_equals_gate_outside_planning: yes`, `frozen_docs_equal_gate: yes`; the same holds at the final head `f1c37c1b`.
- **Origin:** `custom_build` `9483476322e0d331e81c5b92ac266a18f0f01450`, left count 0 (`0 292` at the tip), `unpublished_commits` 292, no `sync/v1.7.0`, no `v1.7.0_gg8` tag, `pre-v1.7.0_gg8` local on `94834763` (type `commit`), no open pull request.
- **What U1 publishes:** `non_planning_vs_gg7` 42 files changed, 5948 insertions, 130 deletions (6 `src/main` files, 428 and 72; 28 `src/test`; 4 documents; `VERSION`, `CHANGELOG.md`, `build.gradle.kts`, `gradle/libs.versions.toml`); `planning_vs_gg7` 253 files, 28568 insertions, 311 deletions.
- **Upstream recheck 3:** main `83808df9`, newest tag `v1.7.0`, no tag past it; PRs 861 (`b62e0ae4`), 855 (`ddabf28b`), 854 (`69d1efc5`) open, 788 (`bb4b4140`) closed unmerged; `upstream_changed_since_e12: no`.
- **Scratch tips now:** `939e8992 scratch/clt-07-prebump-0.9.6; de8c685b scratch/clt-07-client-0.11.0`, unchanged, both ancestors of `94834763` and of the tip.
- **utc_today:** `2026-11-06`; CHANGELOG heading `# 1.7.0_gg8, Unreleased` (the date commit is the orchestrator's step DATE).
- **Server:** `SELECT version()` `26.10.3.7`, image `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`, equal to E-13.
- **D's rehearsal lines** (frozen script md5 `67f30f31a1353a607aaed8d8f09767a7`, equal to E-05; both modes ended `rehearsal: PASS`): parents `9483476322e0d331e81c5b92ac266a18f0f01450` and the tip, `d_tree_equal=yes`, subject `Merge sync/v1.7.0: release v1.7.0_gg8`, `d_version=v1.7.0_gg8`, `d_allowlist_present=yes`, `d_baseline_tag=v1.7.0_gg7`, `d_rollback_output=ok`, block sha256 `2f30bd7d6b0109b22caaba63ff968a02f3f3eedfc11ec8ec4b46474012f67991` (equal to the note's block), `d_gradlew_bat_trap=no`, `d_revert_restores_tree=yes`, `d_rehearsal_cleaned=yes`. The local `custom_build` ref (`3a3a15a9`) did not move, one worktree remains, the rollback branch is absent, no operator document holds destructive SQL.
- **Commits (6, measured from the ledger):** `5154655d` merge message and bundle; `b95553bd` E-14; `cd6d7ac5` handoffs and runbook; `f55f3933` E-15 to E-19; `e16b90a1` close record; `f1c37c1b` E-20.
- **Verifies:** `P49-05-T1-OK`, `P49-05-T2-OK`, `P49-05-T3-OK` and `READY-FOR-USER-APPROVAL` print on the committed tip, each re-run as written after its evidence commit. Anchors mirror `rows=218 bad=0`.

## Ready for the user's direct approval

The bundle is `/Users/alexandr.chashnikov/repo/other/clickhouse-kafka-connect_my/.planning/phases/49-release-publication/49-APPROVAL-BUNDLE.md`; it is valid only for gated head `d9b10b9bf670b8236119c725e3639c53ad3e570b`, states the Freeze rule with its single date-commit exception, and says that `integrationTest` was not re-run (PL-11) so the user can overrule. The questions, one line each:

1. (1) who runs the origin writes (U-4: each after the user's direct word for that exact step);
2. (2) the push day (UTC date for the orchestrator's one-line date commit);
3. (3) go for U1, with the live counts of what it publishes;
4. (4) the release dry run U2 from `sync/v1.7.0`, its own word;
5. (5) the JVM record of the two suites;
6. (6) the review's end state (five rounds, 32 fixed, the last two fixes unreviewed);
7. (7) `integrationTest` not re-run: overrule?
8. (8) merge no pull request on origin before `M'` lands;
9. (9) the upstream state (publish first or sync first if a tag appears);
10. (10) the two scratch branches, information now, deletion waits for the separate confirmation;
11. (11) the deletion of `refs/heads/sync/v1.7.0` on origin, its own word.

The words: G3 authorises U1 only (push `X0` to `sync/v1.7.0`); G3b U2 only (dispatch `release.yaml` from `sync/v1.7.0`, dry run); G4 the build of `M'` and U3 only (push `M'` to `custom_build`); G5 U4 only (the publish run); G6a U5a only (push the tag `pre-v1.7.0_gg8`); G6b U5b only (delete `sync/v1.7.0`); G7 U6 only (delete both `scratch/clt-07-*` branches, after the separate RLS-06 confirmation). No executor runs any step of the tail.

## Handoffs

- **Orchestrator documents:** `49-ORCHESTRATOR-RUNBOOK.md` (executor boundary, `## Freeze rule`, `## The recheck block`, steps O1, DATE, U1 and U2, O2, U3, U4, A1, U5a and U5b, U6, A2, contingencies C-1 to C-9) and the five `49-USER-DISPATCH-*.md` files.
- **Evidence sequence:** E-21 the user's answers, E-22 the DATE commit, E-23 U1, E-24 U2, E-25 the publish word and `M'` with its rollback rehearsal, E-26 U3, E-27 U4, E-28 the read-back and `49-RELEASE-RECORD.md`, E-29 U5a, E-30 U5b, E-31 the RLS-06 confirmation and U6, E-32 the final reads. E-15 to E-19 stay pending forever.
- **Wording changes:** nine are listed in `49-LOCAL-CLOSE-RECORD.md` (the ROADMAP left-count wording first, the scratch dry run and the date commit in the recipe, the 17-row allow-list, closure shape, REQUIREMENTS RLS-06 wording, the meaning of `pre-v1.7.0_gg8`, the twelve FORK-DELTA evidence-path occurrences that move at the milestone close, STATE refresh, the superseded date sentence). Twenty carried items are in the same file.
- RLS-05 and RLS-06 are completed only through the tail (E-21 onward); RLS-01 to RLS-04 are satisfied locally.

## Deviations from Plan

### Orchestrator-directed additions

**1. [Orchestrator addition] Eleven questions and two more named words**
- **Found during:** Task 1 (bundle)
- **Issue:** the plan-checker warned that the `release.yaml` dry run (U2) and the deletion of `refs/heads/sync/v1.7.0` (U5b) were missing from the ROADMAP gate table and the plan's ten questions.
- **Fix:** the bundle names the dry run as question (4) with word G3b, and adds question (11) with word G6b for the branch deletion; both are named in the handoffs, the runbook, the close record and E-14. The plan's check (questions (1) to (10) once each) still passes.
- **Files modified:** the bundle, the handoffs, the runbook, the close record, E-14 and E-20 text.
- **Commit:** `5154655d`, `cd6d7ac5`, `e16b90a1`.

**2. [Orchestrator addition] Extra wording corrections in the close record**
- **Issue:** the orchestrator asked the close record to carry the left-count wording, the RLS-06 wording, the 17-row allow-list, the meaning of `pre-v1.7.0_gg8` and the 12 FORK-DELTA evidence paths.
- **Fix:** nine wording changes instead of five. The twelve paths measured as twelve occurrences of three distinct evidence files (five, five and two). No sentence naming `baa404cd` as the meaning of the tag exists in STATE.md, ROADMAP.md or REQUIREMENTS.md at this tip, so that item is a check, not an edit.
- **Commit:** `e16b90a1`.

### Process notes

- The first run of the Task 2 verify failed on `grep -qF '49-rehearse.sh at'` because the runbook quoted the script path (`49-rehearse.sh" at`). I rewrote the command without the quotes and amended the unpushed documents commit (`f4b5243e` became `cd6d7ac5`) before any evidence entry depended on it; the final verify prints `P49-05-T2-OK`.
- The plan's verify blocks end with a dirty-path test on the evidence file, which cannot pass before its own commit, so each was run once before the evidence commit without that clause and once after it exactly as written; all three print their OK lines.
- `requirements-completed` lists RLS-04 only, not the plan's whole array: copying RLS-05 and RLS-06 would mark them complete (T-49-18).

**Total deviations:** 2 orchestrator-directed additions, 0 auto-fixed bugs. **Impact:** none on the gated tree; only `.planning` files changed, the Freeze rule holds.

## Known Stubs

None. No code was written; no placeholder text remains in the documents (angle-bracket values in the handoffs are environment-specific by design).

## Authentication Gates

None.

## Threat Flags

None. The documents describe origin writes and perform none; no new endpoint or access path was added.

## Self-Check: PASSED

All ten files of the plan exist (`49-MERGE-MESSAGE.txt`, the bundle, the five handoffs, the runbook, the close record, the evidence file); the six commits `5154655d`, `b95553bd`, `cd6d7ac5`, `f55f3933`, `e16b90a1` and `f1c37c1b` are ancestors of HEAD; the three task verifies print their OK lines on the committed tip; origin `custom_build` reads `9483476322e0d331e81c5b92ac266a18f0f01450`, origin has four heads (`custom_build`, `main`, the two scratch branches), no `sync/v1.7.0` and no `v1.7.0_gg8` tag; one worktree remains and `refs/gsd/release-m-v1.7.0_gg8` does not exist.
