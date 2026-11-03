---
phase: 38-release-publication
plan: "05"
subsystem: release
tags: [publication-rehearsal, merge-message, approval-bundle, user-handoffs, runbook, local-close, evidence]

requires:
  - phase: 38-release-publication
    provides: "plans 38-01 to 38-04: VERSION v1.7.0_gg6, the frozen documents (E-10), the allow-list (E-11), the Docker-free gates and two full suites on one pinned image (E-12, E-13), the go decision"
provides:
  - "38-MERGE-MESSAGE.txt and 38-APPROVAL-BUNDLE.md (eight items): the message the orchestrator uses for M and the measured bundle it presents verbatim"
  - "four user handoffs (U1 and U2, U3, U4, U5) and 38-ORCHESTRATOR-RUNBOOK.md: pre-checks with expected values, abort rules, resume paths, eight contingencies, the evidence sequence from E-20"
  - "38-LOCAL-CLOSE-RECORD.md: requirement status, the wording changes still outstanding, the carried items, the not-proven items"
  - "38-COMMAND-EVIDENCE.md entries E-14 (pass), E-15 to E-18 (pending, user-gated) and E-19 (pass for the local gate only)"
affects: [milestone-close]

plan_head_before: b0ab8c7b1b47185b58a22561742c00ec95b24f73
plan_head_after: cbb9685c8f39bbed478194c5c6cceda3db6569e9

actuals:
  tokens: 30421
  tasks: 3
  commits: 6

tech-stack:
  added: []
  patterns:
    - "dry-run --no-ff merge in a throwaway detached worktree, with git attributes switched off for the merge and the revert (the gradlew.bat line-ending trap at the base commit)"
    - "pending evidence records for user-gated handoffs, generated from the handoff files so the numbered pre-check counts are read, not typed"

key-files:
  created:
    - .planning/phases/38-release-publication/38-MERGE-MESSAGE.txt
    - .planning/phases/38-release-publication/38-APPROVAL-BUNDLE.md
    - .planning/phases/38-release-publication/38-USER-DISPATCH-1-SCRATCH-DRYRUN.md
    - .planning/phases/38-release-publication/38-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md
    - .planning/phases/38-release-publication/38-USER-DISPATCH-3-PUBLISH.md
    - .planning/phases/38-release-publication/38-USER-DISPATCH-4-PRETAG-CLEANUP.md
    - .planning/phases/38-release-publication/38-ORCHESTRATOR-RUNBOOK.md
    - .planning/phases/38-release-publication/38-LOCAL-CLOSE-RECORD.md
  modified:
    - .planning/phases/38-release-publication/38-COMMAND-EVIDENCE.md

key-decisions:
  - "The merge in the dry run (and in the runbook for M) runs with git --attr-source set to the empty tree, because a fresh worktree at 3a3a15a9 lists ' M gradlew.bat' and a plain merge stops on it (the 38-02 finding)"
  - "RLS-05 is never marked satisfied here: E-15 to E-18 stay pending and the close record lists U1 to U5 as the remaining gate"
  - "The U3 handoff gained a seventh pre-check (the UTC date against the CHANGELOG heading) and a note that M exists only in the local repository that built it"

requirements-completed: []

coverage:
  - id: D1
    description: "The publication path is rehearsed without a push: a --no-ff merge D of the tip onto 3a3a15a9 in a throwaway detached worktree has two parents, the tip's tree, the agreed subject and no .gsd-ws-arg; the note's rollback block ran unchanged on it and printed ok; the baseline on D is v1.7.0_gg5; SELECT version() and the image digest are recorded; D was never pushed and its worktree is gone"
    requirement: "RLS-05"
    verification:
      - kind: other
        ref: "38-COMMAND-EVIDENCE.md E-14 (34 checks); plan verify P38-05-T1-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "The merge message is a committed file whose subject and last line are as agreed and the eight-item approval bundle exists with measured figures"
    requirement: "RLS-05"
    verification:
      - kind: other
        ref: "E-14 keys merge_subject, merge_trailer_last, message_check, bundle_items; plan verify P38-05-T1-OK"
        status: pass
    human_judgment: false
  - id: D3
    description: "Four user handoffs and the orchestrator runbook exist with the required sections, numbered pre-checks and expected values, abort rules and resume paths; none carries a push of the v1.7.0_gg6 tag, a forbidden push option or a mutating gh line; E-15 to E-18 are pending by design"
    requirement: "RLS-05"
    verification:
      - kind: other
        ref: "E-15 to E-18; plan verify P38-05-T2-OK"
        status: pass
    human_judgment: false
  - id: D4
    description: "Whether the handoffs are clear and safe enough for the user to run every origin step from them, and whether the facts not proven by the local gate (the unreviewed last two review fixes, the deferred gg1 half of WR-08, the IN-04 pin gap, the JDK 21 record of the two full suites) are acceptable"
    verification: []
    human_judgment: true
    rationale: "No test reads a handoff as a user would; and the acceptance of unproven items is the user's call, asked in the approval bundle"
  - id: D5
    description: "The phase closes locally: RLS-01 to RLS-04 and RLS-06 satisfied with evidence, RLS-05 pending and human-gated, origin unchanged, no tag, only the five expected paths changed outside .planning"
    requirement: "RLS-05"
    verification:
      - kind: other
        ref: "E-19 (19 checks); plan verify P38-05-T3-OK"
        status: pass
    human_judgment: false
  - id: D6
    description: "The publication itself: the scratch push, the release dry run on a runner, the push of M, the single tests.yaml run, the publish run that creates the release and the tag, the pre-tag push and the scratch deletion"
    requirement: "RLS-05"
    verification: []
    human_judgment: true
    rationale: "User-run origin steps that no agent may take (O-1, O-2); pending until E-20 to E-27 exist"

duration: 12min
completed: 2026-10-31
status: complete
---

# Phase 38 Plan 05: The publication rehearsal, the user handoffs and the local close Summary

**A `--no-ff` merge of the tip onto `3a3a15a9` was rehearsed in a throwaway worktree with every check the real merge will face (rollback block printed `ok`, baseline `v1.7.0_gg5`, `SELECT version()` 26.10.3.7), the merge message, an eight-item approval bundle, four user handoffs and the orchestrator runbook were written, and the phase closes locally with RLS-05 pending and human-gated: nothing was pushed, tagged or dispatched.**

## Gate

- The handoffs quote `gated_head` `ea613ec35ebade7a2182cac4e6ecd5d86d099def`, `last_non_planning_commit` `927ac07c30274c46b2330d597d417f9f1eda8f8b` (the "gated SHA" of the pre-checks) and `last_scripts_commit` `cc928221b7f2a19ccad3fa2f160f970b35c671e9`. `gate_decision` is `go` (E-12, E-13). `final_tip_bytes` is 13558634.
- HEAD differs from the gated head only under `.planning/phases/38-release-publication` (`git diff --quiet ea613ec3 HEAD -- . ':(exclude).planning'` and `-- .planning/scripts` both exit 0; E-14 and E-19 `tip_equals_gate_outside_planning: yes`).
- Dry-run merge D (E-14), built in `/tmp/gsd38/w/pubdry` detached at `3a3a15a9ae064ffe09c696b8bb87bce23419d463` with the committed merge message and the tip `b0ab8c7b1b47185b58a22561742c00ec95b24f73`: commit `9e31f79c7d9f4ef8c946cf5ff8de56daba5588f8` (unreachable, never pushed); `d_parents` the two SHAs; `d_tree_equal` yes; subject `Merge sync/v1.7.0: release v1.7.0_gg6`; no `.gsd-ws-arg`; `VERSION` `v1.7.0_gg6`; allow-list present; baseline selection `v1.7.0_gg5`; no gg6 tag merged; operator-document scan `none`; the note's rollback block, extracted from D's own copy of the note and byte-identical to the E-07 block, exit 0 and `ok`, `VERSION` `v1.7.0_gg5`, note absent, tree equal to `pre-v1.7.0_gg6`; a revert restored D's tree; worktree and rollback branch removed; the local `custom_build` ref unmoved (it reads `3a3a15a9`).
- `SELECT version()` 26.10.3.7 and image `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`, equal to E-13 `digest_after_run2`.
- Date check: the CHANGELOG heading date `2026-10-31` equals today's UTC date `2026-10-31` (measured 18:28Z); the runbook (step O2) and the U3 pre-check 7 repeat the comparison before the push of M.
- Figures at tip `b0ab8c7b` (the bundle's item 1): 331 commits since `3a3a15a9`, 168 on no origin ref, outside `.planning` 88 files (8499 insertions, 619 deletions), inside `.planning` 237 files (49912 insertions, 507 deletions); left count 0.

## Task Commits

1. **Task 1: rehearsal and bundle** - `36dd3f8a` (merge message and approval bundle), `d4749271` (E-14)
2. **Task 2: handoffs and runbook** - `de51a8c8` (four user documents and the runbook), `e65fb537` (E-15 to E-18, pending)
3. **Task 3: local close** - `5c7f51a7` (close record), `cbb9685c` (E-19)

**Plan metadata:** the commit that carries this SUMMARY.

## Deviations from Plan

**1. [Plan wording] The dry-run merge uses `git --attr-source` (the gradlew.bat trap)**
- **Found during:** Task 1, step 3.
- **Issue:** the plan says a plain `git merge --no-ff -F`; a fresh worktree at `3a3a15a9` lists ` M gradlew.bat` and a plain merge of the tip stops on it (the finding recorded in E-07 for plan 38-02).
- **Fix:** the merge in D, the revert, and the merge in the runbook for M run with `git --attr-source="$(git hash-object -t tree /dev/null)"`, as the note's rollback section does; the rollback block itself ran plain. E-14 `gradlew_bat_at_base` states it.
- **Commit:** `d4749271`, `de51a8c8`.

**2. [Plan wording] The bundle's file list is `--name-status`, in C order, complete (88 paths)**
- The plan asks for the changed-file list of `git diff --stat`; the full name-status list (generated by script, 88 paths) gives the same set with the change type, and the 43 review findings are listed with the exact titles of `38-REVIEW.md`, generated by script as well. **Commit:** `36dd3f8a`.

**3. [Rule 2 - Missing critical] Two additions to the handoffs beyond the plan's text**
- U3 got a seventh pre-check (`date -u +%F` and the CHANGELOG heading of M both `2026-10-31`; a different date is a stop, the plan's PT-14) because the date rule would otherwise rest on the orchestrator alone, and a line saying that M exists only in the local repository that built it, so the user must push from that repository. U1, U3, U4 and U5 each state the facts not proven by the local gate. **Commit:** `de51a8c8`.

**4. [Plan wording] E-19 index cell reads `pass`**
- The evidence helper requires the index cell to equal the entry verdict, so the cell reads `pass`; the entry title, the close record and this SUMMARY state that the verdict is for the local gate only. **Commit:** `cbb9685c`.

**5. [Plan wording] The wording-change list has eleven items, six of them already applied**
- Items 1 to 6 (the Goal, criterion 5, the Note, the Research flag and Plans, REQUIREMENTS RLS-05, and RLS-02 with criterion 2) were applied by the orchestrator in `0d4763ee` and are listed as applied after checking each against the current ROADMAP.md and REQUIREMENTS.md. Three outstanding items were found beyond the plan's eight: the Phase 38 line of the ROADMAP phase list still says "before the tags", the Phase 33 note still says "Phase 38 pushes it only after the published SHA's run is green", and the progress row and plan checklist of Phase 38. **Commit:** `5c7f51a7`.

**6. [Plan wording] The per-plan commit ledger was written after the fact**
- The protocol file `gsd-plan-head-before-38-05` was not created before the first commit; it was written at SUMMARY time with the plan-start HEAD (`b0ab8c7b`, E-14 `tip_head`), so `commits: 6` is measured with `git rev-list --count b0ab8c7b..HEAD` before the SUMMARY commit.

**Total deviations:** 6 (1 blocking-avoidance, 1 completeness, 1 missing-critical, 3 plan wording). **Impact:** none on scope; all three plan verifies print `P38-05-T1-OK`, `P38-05-T2-OK` and `P38-05-T3-OK` as written on the final tip.

## Issues Encountered

- The verify blocks needed no adjustment for the fixed-tip clauses that 38-03 and 38-04 hit: every clause compares against `L0` (the phase start), the gated head or a literal, and all hold at HEAD.
- `.planning/config.json` and `.planning/state.json` stay modified in the working tree and were never staged. STATE.md, ROADMAP.md and REQUIREMENTS.md were not touched, and no STATE update was run: the orchestrator owns them.

## Known Stubs

None.

## Threat Flags

None. The plan adds documents and evidence only; no endpoint, auth path or file access.

## User Setup Required

None for this plan. The publication needs the user's five steps (below).

## Open for the user and the orchestrator

- **The five user steps and their files** (all in `.planning/phases/38-release-publication/`): U1 and U2 `38-USER-DISPATCH-1-SCRATCH-DRYRUN.md` (scratch push to `sync/v1.7.0`, the one `tests.yaml` run, the release dry run from that branch); after the user's own direct "publish" message the orchestrator builds M (runbook O3); U3 `38-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md` (push M to `custom_build` by SHA, single green run); U4 `38-USER-DISPATCH-3-PUBLISH.md` (the real `release.yaml` dispatch from `custom_build`, `dry_run` not ticked; the run creates the release and the tag); U5 `38-USER-DISPATCH-4-PRETAG-CLEANUP.md` (`pre-v1.7.0_gg6` by name, scratch deletion, pull requests). E-15 to E-18 stay pending forever; the user's reports become E-20 to E-27 as the runbook lists.
- **Wording changes** (full text in `38-LOCAL-CLOSE-RECORD.md`): applied already in `0d4763ee`: the Goal sentence, criterion 5, the Note, the Research flag and Plans, REQUIREMENTS RLS-05, RLS-02 and criterion 2. Still outstanding for the orchestrator: (7) closure shape (phase checkbox and RLS-05 stay open until E-27; RLS-01 to RLS-04 and RLS-06 flip when the verification passes); (8) the SWP-03 wording flag, not edited; (9) the Phase 38 phase-list line "before the tags"; (10) the Phase 33 note on when `pre-v1.7.0_gg6` is pushed; (11) the Phase 38 progress row and plan checklist.
- **CHANGELOG date:** heading `2026-10-31` equals the UTC date at measurement; the orchestrator compares again before the push of M and asks the user directly if it differs (PT-14).
- **Not proven, to be decided by the user as human-judgment items:** the 12-round review loop and its two unreviewed Info fixes (IN-14, IN-15), the deferred `docs/ROLLOUT-v1.7.0_gg1.md` half of WR-08, the IN-04 pin gap, JDK 21 for the two full suites (criterion 4 met on JDK 21; the Docker-free `testUnit` ran on the default JDK 17), and the integration suite not re-run (the accepted nightly is the record).
- **CI wiring never run on a runner:** the Phase 37 compare chain; a CI-only defect goes back to Phase 37 as a gap (Phase 38 cannot edit `.github/`), and the user decides on a `.github/` exception.
- **Assumptions needing a read after publication:** A1 (pull requests #16, #18, #19, #20 marked merged), A2 (Dependabot closes #10 and #17), A3 (the workflow's token creates the tag and a release authored by `github-actions[bot]`), A7 (the runner's archive size near 13558634 bytes). `NIGHTLY_ENABLED` is not set.
- **Hygiene of what is committed:** the user-owned `scratch/clt-07-*` branches were never touched; the approval bundle, the handoffs and the runbook name no deployment, and the strict hygiene helper is clean over every file this plan adds.

## Closure shape

Local-complete: plans 38-01 to 38-05 are executed and gated, E-01 to E-14 and E-19 are `pass`, E-15 to E-18 are `pending (user-gated)`. RLS-01 to RLS-04 and RLS-06 are satisfied; RLS-05 is pending and human-gated with U1 to U5 as the remaining gate; the Phase 38 checkbox stays unchecked and RLS-05 stays Pending until E-20 to E-27 are recorded. Phase verification should read `human_needed` for criterion 5 and RLS-05 and pass for the rest. Origin `custom_build` still reads `3a3a15a9ae064ffe09c696b8bb87bce23419d463`; no `sync/v1.7.0` ref and no `v1.7.0_gg6` tag exists on origin or locally; `pre-v1.7.0_gg6` is local only; the non-`.planning` change set is exactly `CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg5.md` and `docs/ROLLOUT-v1.7.0_gg6.md`.

## Handoffs

The milestone close takes the carried items table of `38-LOCAL-CLOSE-RECORD.md` (18 rows, each with disposition, owner and re-open trigger):

| Item | Owner | Re-open trigger |
|------|-------|-----------------|
| The writer's `stop()` never closes its client (FORK-DELTA item 13) | fork maintainer (the user) | a task-restart leak report, or the next edit of `ClickHouseWriter.stop()` |
| The four-minute insert timeout without effect while `async=false` (item 14) | fork maintainer (the user) | an operator report of a hang on a stalled server, or the next edit of the client V2 insert methods |
| The harness Datagen completion counting (item 15) | fork maintainer (the user) | a red nightly traced to it, or the next edit of the `ConfluentPlatform` counting helper |
| Two `scratch/clt-07-*` branches on a public origin | fork maintainer (the user) | the user's answer in U5, or the milestone close |
| No dependency checksum file, no `zstd-jni` row in the compare's version table (item 18) | fork maintainer (the user) | a substitution incident, a new native-bundling dependency, or the next edit of the version table |
| Phase 36 review notes IN-02 to IN-06 and UF-36-5 | fork maintainer (the user) | the next edit of the Phase 36 pin test classes, or the milestone close |
| The documented limits of the buffered fix (item 16, row B8) | fork maintainer (the user) | a Direct-path rename with `reportInsertedOffsets=true`, a many-to-one or input-topic rename, or upstream resolving issue 777 |
| `DataException` retriable cases and the missing retry budget (item 17) | fork maintainer (the user) | a decision on FUT-05, or a redelivery loop report |
| `increment-version.yml` header names the wrong guard class (item 19) | fork maintainer (the user) | the next edit of that workflow |
| Phase 37 ledger rows needing approval (item 20) | fork maintainer (the user) | the user's approval to edit `src/main`, or the next edit of the files in the ledger |
| The `tests.yaml` gate accepts a green run of any branch (item 21) | fork maintainer (the user) | a publish whose only green run came from another branch |
| The earlier caveat that no run exercised the publish path (item 22) | Phase 38 publication | E-24 and E-25, or a failed or cancelled publish run |
| SWP-03: states of pull requests #10 and #17 (item 23) | Phase 38 publication | E-27, or Dependabot's next monthly run |
| CLT-07 nightly: one green run, accepted by the user | fork maintainer (the user) | any change of the `src/main`, build or `gradle` files before publication, or the first nightly after `NIGHTLY_ENABLED` is set |
| Review finding WR-08, the gg1 note half (deferred) | fork maintainer (the user) | the next edit of `docs/ROLLOUT-v1.7.0_gg1.md`, or the milestone close |
| Review finding IN-04, the Docker-free pin of the `custom_` forwarding (not applied) | fork maintainer (the user) | the next client bump, or the next edit of `createClientV2` |
| Review findings IN-14 and IN-15, fixed and not reviewed again | fork maintainer (the user) | the next edit of the first-start subsection of the note, or an operator question about it |
| The JDK 21 record of the two full suites | fork maintainer (the user) | a full suite that must run on the default JDK 17, or a change of the host's JDK |

## Self-Check: PASSED

- Files exist: the merge message, the approval bundle, the four user handoffs, the runbook and the close record under `.planning/phases/38-release-publication/`, and `38-COMMAND-EVIDENCE.md` with entries E-14 to E-19.
- Commits `36dd3f8a`, `d4749271`, `de51a8c8`, `e65fb537`, `5c7f51a7` and `cbb9685c` are ancestors of `HEAD`.
- `P38-05-T1-OK`, `P38-05-T2-OK` and `P38-05-T3-OK` print on the tip before this SUMMARY; the `custom_build` ref, the scratch ref and the tag checks read as in E-19.

---
*Phase: 38-release-publication*
*Completed: 2026-10-31*
