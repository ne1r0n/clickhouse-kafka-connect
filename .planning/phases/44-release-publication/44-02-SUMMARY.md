---
phase: 44-release-publication
plan: "02"
subsystem: release
tags: [release, rollout-note, labels, rollback-rehearsal, evidence, v1.7.0_gg7]

requires:
  - phase: 44-release-publication
    provides: plan 44-01 (VERSION v1.7.0_gg7, the 1.7.0_gg7 changelog section, the final FORK-DELTA, the local tag pre-v1.7.0_gg7, the five helpers, evidence E-01 to E-04)
provides:
  - docs/ROLLOUT-v1.7.0_gg7.md, the operator note for the move from v1.7.0_gg6 (33 labelled rows in sections A to G, the verbatim WARN, the line `FUT-05 stays open.`, the supersession table, the known limits)
  - 44-note-check.py, the structure gate of the note (rows, labels, forbidden, fut, claims, all)
  - evidence E-05 to E-07 (re-measured archives and source facts, the rollback block run unchanged on a dry-run merge of the committed tip, the checker and its mutations)
affects: [44-03, 44-04, 44-05]

actuals:
  tokens: 15814
  tasks: 3
  commits: 7
plan_head_before: 27859a8d262c8dbc2cf0ba9499676c80b02dc517
plan_head_after: f7b807083b8c9c7f45fb29a21be827a9044215d2

tech-stack:
  added: []
  patterns:
    - "every row of the note carries one of four label forms and an Applies-when value; a structure gate enforces the forms, the T6 and F4 label, client V in every T row, one line for FUT-05 and no planning identifier"
    - "the rollback block is extracted from the committed note and run unchanged on a dry-run merge of the committed tip onto the published commit, then reverted"

key-files:
  created:
    - docs/ROLLOUT-v1.7.0_gg7.md
    - .planning/phases/44-release-publication/44-note-check.py
  modified:
    - .planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md

key-decisions:
  - "Row T7 states the duplicate with exactlyOnce=false but not the planning sentence that no deduplication token is sent without exactlyOnce: the code sends the same token on every attempt on the default path (only ignorePartitionsWhenBatching=true sends none)"
  - "Step 3 of the rollback tells operators to download the published v1.7.0_gg6 asset and not to rebuild it: a local rebuild of the tag is 59 bytes smaller and six connector classes differ from the asset"
  - "The note carries no gradlew.bat sentence: the block left git status empty on the dry run (git 2.50.1), and the forward path uses plain git switch and git revert, which the plain rehearsal restored"
  - "The checker's --only now ignores rows of the other sections, so the partial-note verify blocks of tasks 1 and 2 still pass on the finished note"

requirements-completed: [RLS-03]

coverage:
  - id: D1
    description: "The note skeleton exists end to end: preamble with the four label forms and the Applies-when vocabulary, section G rows D1 to D5, and the rollback block, which runs unchanged on a dry-run merge of the committed tip onto 72e8ccff (prints ok, VERSION v1.7.0_gg6, no copy of the note, tree equal to the tag, a revert restores the merged tree)"
    requirement: "RLS-03"
    verification:
      - kind: other
        ref: "bash verify block of task 1: P44-02-T1-OK (re-run on the final tip)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Sections A and B: rows F1 to F5 and T1 to T15 (the 13 claims in order, client V2 scope first, T6 as derived (mechanism, not measured)), and the table of what this release closes from the gg6 note"
    requirement: "RLS-03"
    verification:
      - kind: other
        ref: "bash verify block of task 2: P44-02-T2-OK (re-run on the final tip)"
        status: pass
    human_judgment: true
    rationale: "Whether each label is true for its evidence and each Applies-when matches the code path is the judgment of the independent review of plan 44-03; the structure gate cannot decide it"
  - id: D3
    description: "Sections C to F and the known limits: rows R1 to R8, the WARN quoted verbatim, the line FUT-05 stays open. in section F only, 33 rows in all, links resolving, six checker mutations failing"
    requirement: "RLS-03"
    verification:
      - kind: other
        ref: "bash verify block of task 3: P44-02-T3-OK (re-run on the final tip)"
        status: pass
    human_judgment: true
    rationale: "The independent review of plan 44-03 reads every row against its source; this plan cannot supply that review"

duration: 18min
completed: 2026-11-02
status: complete
---

# Phase 44 Plan 02: Rollout note for v1.7.0_gg7 Summary

**docs/ROLLOUT-v1.7.0_gg7.md with 33 rows each labelled measured, read from the code, derived or derived (mechanism, not measured) and scoped by Applies-when, a rollback block proven unchanged on a dry-run merge of the committed tip, and a structure gate (44-note-check.py) whose six mutations each fail; nothing pushed.**

## Performance

- **Duration:** 18 min
- **Started:** 2026-11-02T21:12:02Z
- **Completed:** 2026-11-02T21:30:00Z
- **Tasks:** 3
- **Files modified:** 3 (2 created, 1 modified; 620 insertions, 3 deletions before this summary)

## Accomplishments

- The tracer: the checker, both archives rebuilt and compared, the source facts read, the skeleton with section G, and the rollback block rehearsed on the committed tip (E-05).
- Sections A and B: five helper-closing rows and the fifteen bounded-insert rows, with the progress-header claim labelled `derived (mechanism, not measured)`, the duplicate, the `socket_timeout` override and the unbounded cases stated, and the supersession table (E-06).
- Sections C to F and the known limits: the original-topic rows, the limits under topic-changing SMTs, the `RetriableException` pass-through, the start-up WARN quoted verbatim from the source, and `FUT-05 stays open.` as the only mention of that identifier (E-07).

## Task Commits

1. **Task 1 (tracer): checker, skeleton with section G, rollback rehearsal** - `7ef33b4f` (note and checker), `057eea9a` (E-05)
2. **Task 2: sections A and B, the supersession table** - `38b26281` (note), `fcdfed14` (E-06)
3. **Task 3: sections C to F, known limits, full checks** - `7b6640fd` (note), `bf9e9409` (E-07)
4. **Checker follow-up (deviation 4):** `f7b80708`

**Plan metadata:** the commit holding this file (docs: complete plan).

## Files Created/Modified

- `docs/ROLLOUT-v1.7.0_gg7.md` - the operator note (212 lines)
- `.planning/phases/44-release-publication/44-note-check.py` - the structure gate
- `.planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md` - entries E-05 to E-07, index rows flipped to pass

## Facts

- Note commits: `note_commit_1` `7ef33b4feae0759c17a77e171e4d53e36b924b3a` (note and checker), `note_commit_2` `38b26281f0556350b777313231093c036870ddb4`, `note_commit_3` `7b6640fdfe30532eb03224bc6bd93a5f9915f4b0`; evidence commits `057eea9a`, `fcdfed14`, `bf9e9409`; checker follow-up `f7b807083b8c9c7f45fb29a21be827a9044215d2`.
- Archives (local rebuilds, clean exports, one Gradle run each, no crash): `base_entries` 7933, `tip_entries` 7932, `base_bytes` 13558633, `tip_bytes` 13564144; compare with a comment-only list `compare_exit` 1, `compare_totals` `totals files-a 7513 files-b 7512 only-a 1 only-b 0 changed 119 same 7393`, `compare_unallowed` 120 (101 gson classes changed, 1 only in the base, 3 gson metadata, 14 connector classes, 1 manifest, 0 other), `compare_version_table` the gson line only (18 version lines), `compare_manifest` `MANIFEST.MF: differs only in Implementation-Version`.
- Source facts: `client_version_default` V1, `insert_timeout_min` `atLeast(100)`, `originaltopic_line` line 137 of `Processing.java`, `vetted_codes` `15 + 216`, `writer_stop_wait` 5 seconds, WARN text assembled from the literals.
- Rehearsal on the committed note (tip `7ef33b4f`): `rehearsal_parents` `72e8ccff... 7ef33b4f...`, tree equal, `d0_status_before_block` clean, `rollback_output` ok, `gradlew_bat_trap` no, `rollback_version` v1.7.0_gg6, note absent after the block, tree equal to tag `pre-v1.7.0_gg7`, `revert_restores_tree` yes with git's attributes off and with plain `git revert` (git 2.50.1), worktree and branches removed.
- Rows: 33 (F1-F5, T1-T15, R1-R8, D1-D5); labels by row: measured 23, read from the code 18, derived 3, derived (mechanism, not measured) 2 (rows counted once per label they carry).
- `gg6_asset_vs_rebuild`: published asset 13558692 bytes, SHA-256 `e745e7d5363fca8067ddfcc80f3ebea034b93abb4f2912e514492201a729c2af` (re-read and re-hashed from the public release); local rebuild 59 bytes smaller; compare shows 6 changed connector classes (`ClickHouseWriter`, `Column`, `Column$ColumnBuilder`, `StateRecord`, `ConvertToMapOrJson`, `ExtractTopic`).

## Decisions Made

- Row T7 and the checklist state the duplicate as a possibility in a table that does not deduplicate, and say the token is the same on every attempt unless `ignorePartitionsWhenBatching=true` (read from `Processing.doLogic` and `QueryIdentifier`).
- Row T12 splits its label (`read from the code` for the three wait sites, `measured` for the 30.1 s bound); T13 limits its measured half to a stalled insert, which is what the probes bounded.
- The rollback step 3 points at the published asset and does not claim a rebuild is equal to it.
- Applies-when values may carry a parenthesis that names the claim they cover (rows F3 and T15), defined in the preamble.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] The plan's statement that a rebuild of tag v1.7.0_gg6 compares equal to the published gg6 asset is false**
- **Found during:** Task 1 (rollback step 3), while hashing and comparing the public asset
- **Issue:** the plan and the research say the rebuild compares equal entry by entry; measured, the asset is 59 bytes larger and the compare lists 6 connector class files as different. For two (`StateRecord`, `ExtractTopic`) `javap` shows a different compiled string concatenation; the compiler that built the asset was not established.
- **Fix:** the note tells operators to download the asset and quotes the measured difference; E-05 records the keys `gg6_asset_vs_rebuild_*`.
- **Files modified:** `docs/ROLLOUT-v1.7.0_gg7.md`, E-05
- **Verification:** the compare output `/tmp/gsd44/exec/cmp-gg6asset-e05.out` (scratch)
- **Committed in:** `7ef33b4f`, `057eea9a`

**2. [Rule 1 - Bug] Row T7 would have said no deduplication token is sent without exactlyOnce**
- **Found during:** Task 2, checking row T7 against `Processing.doLogic`
- **Issue:** `doLogic` runs for `exactlyOnce=false` too (in-memory state) and builds a `QueryIdentifier` with partition and offsets, so the V2 insert methods set the same `insert_deduplication_token` on each attempt; only `ignorePartitionsWhenBatching=true` sends none.
- **Fix:** T7 states the duplicate in a table that does not deduplicate, the same token per attempt, and the `ignorePartitionsWhenBatching=true` exception; T12 got the split label.
- **Files modified:** `docs/ROLLOUT-v1.7.0_gg7.md`, E-06
- **Committed in:** `38b26281`, `fcdfed14`

**3. [Rule 3 - Blocking] Plan verify clauses that cannot pass as written**
- **Found during:** Task 2 verify and the final-tip re-run
- **Issue:** (a) the Task 2 clause `git diff --quiet {L0} HEAD -- ... CHANGELOG.md docs/FORK-DELTA.md` fails by design, because plan 44-01 edited both files; (b) the Tasks 1 and 2 clauses `rows --only D` and `rows --only F T D` failed on the finished note, because the checker, as specified, rejected rows of other sections; (c) the `PYTHONPYCACHEPREFIX ... py_compile` form writes bytecode into the phase directory under `-I` (same finding as plan 44-01).
- **Fix:** (a) the run used the FORK-DELTA commit of E-04 as the anchor for those two files and `{L0}` for the rest (script `/tmp/gsd44/exec/verify-44-02-t2.sh`); (b) see deviation 4; (c) `-X pycache_prefix=/tmp/gsd44/w/pyc`.
- **Files modified:** none tracked for (a) and (c)
- **Verification:** the three blocks print `P44-02-T1-OK`, `P44-02-T2-OK` and `P44-02-T3-OK` on the final tip with those substitutions
- **Committed in:** not applicable

**4. [Rule 3 - Blocking] `--only` of the checker now ignores rows of the other sections**
- **Found during:** final-tip re-run of the Task 1 and Task 2 verify blocks
- **Issue:** the plan's time-scoping rule says every task's verify still passes on the plan's final commit; the gg6-derived `--only` rejected the rows of later sections.
- **Fix:** with `--only`, only the listed letters are checked; without it, all 33 IDs must occur once, as before. Self-test (4 of 4), six mutations (6 of 6) and an extra case (a `T16` row fails under `--only T`) re-run green.
- **Files modified:** `.planning/phases/44-release-publication/44-note-check.py`
- **Verification:** `P44-02-T1-OK`, `P44-02-T2-OK`, `P44-02-T3-OK` on `f7b80708`
- **Committed in:** `f7b80708`

---

**Total deviations:** 4 auto-fixed (2 bugs in planning claims, 2 blocking verify defects). **Impact on plan:** none on scope; the note states what the code does, and the verify blocks are repeatable on the final tip.

## Issues Encountered

- E-05 to E-07 were written before deviation 4, so their text describes the checker as it was; the evidence is not edited. The final checker passes every check those entries record.

## Known Stubs

None.

## User Setup Required

None - no external service configuration required.

## Open for the orchestrator

- **Rebuild versus asset.** The research claim that a rebuild of `v1.7.0_gg6` equals the published asset entry by entry does not hold for a local rebuild (deviation 1). The note says so; the older gg6 note, which says the same for `v1.7.0_gg5`, is not edited. Plan 44-04 compares local archives with each other, so the allow-list is unaffected; the runner dry run (plan 44-05, U2) is the check on the runner's compiler.
- **Token claim.** The evidence of the bounded-insert phase says no token is sent without `exactlyOnce` (deviation 2). `docs/FORK-DELTA.md` item 14 says "a duplicate" and the changelog says "can be inserted twice"; neither contradicts the code, but neither mentions the token; the independent review may want both aligned with row T7.
- **Rows with a weaker source than the inventory expected.** T7 (token wording), T12 (split label) and T13 (the measured operator bound covers a stalled insert, not the other requests). T6 and F4 are `derived (mechanism, not measured)` as planned.
- **Gate verify blocks.** Re-run the three blocks from the plan with the substitutions of deviation 3, or fix the clause in plan 44-02 Task 2.
- **Left for the orchestrator.** `.planning/STATE.md` was updated by the state verbs and is left modified (plan rule: never staged here). `REQUIREMENTS.md` was not edited and RLS-03 is not marked complete: its review is plan 44-03. `.planning/config.json` and `.planning/state.json` are untouched.
- **Scratch.** `/tmp/gsd44/w/arch-*`, `arch`, `gg6asset`, `gg6diff` (archives, an untrusted downloaded asset, class files) and `/tmp/gsd44/exec` hold no tracked content and may be deleted; nothing was pushed and no workflow was dispatched; `origin/custom_build` still reads `72e8ccff`; no `v1.7.0_gg7` tag exists.

## Handoffs

- **Plan 44-03:** files under review: `docs/ROLLOUT-v1.7.0_gg7.md` (and `44-note-check.py`). Checker: `python3 -I .planning/phases/44-release-publication/44-note-check.py all docs/ROLLOUT-v1.7.0_gg7.md --allow 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 e745e7d5363fca8067ddfcc80f3ebea034b93abb4f2912e514492201a729c2af` (expects `OK rows=33`, labels 4 of 4, forbidden 0, fut OK, claims 0). Rehearsal to re-run if a fix touches the rollback block: extract the fenced `sh` block that holds `git read-tree -u --reset pre-v1.7.0_gg7` (`python3 -I` script), `git worktree add --detach <new dir> 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`, merge the committed tip there with `git --attr-source="$(git hash-object -t tree /dev/null)" merge --no-ff` and a demo identity, run `bash -e <block>`, expect the last line `ok`, `VERSION` `v1.7.0_gg6`, no `docs/ROLLOUT-v1.7.0_gg7.md`, `git diff --quiet HEAD pre-v1.7.0_gg7`, and `git revert --no-edit` restoring the merged tree; then `git worktree remove --force`, `git worktree prune`, `git branch -D rollback-v1.7.0_gg7`. The scratch script `/tmp/gsd44/exec/rehearse.sh <tip> <block> <new dir>` (and `MODE=plain`) does this and may be gone. A reviewer's Fix text is checked against the code before it is applied; rows T7, T12, T13 and the rollback step 3 are the ones whose wording differs from the plan.
- **Plan 44-04:** `base_entries` 7933, `tip_entries` 7932, `compare_unallowed` 120, `compare_totals` `totals files-a 7513 files-b 7512 only-a 1 only-b 0 changed 119 same 7393`; the 120 split as 102 gson classes (101 changed, 1 only in the base), 3 gson metadata, 14 connector classes, 1 manifest.

## Next Phase Readiness

- The note is complete and ready for the orchestrator's independent review (plan 44-03). No blockers.

## Self-Check: PASSED

The three created or modified files exist; the seven task commits are ancestors of HEAD; `P44-02-T1-OK`, `P44-02-T2-OK` and `P44-02-T3-OK` print on `f7b80708` (with the substitutions of deviation 3); no worktree, no rollback branch and no `v1.7.0_gg7` tag remain; `origin/custom_build` is unchanged.

---
*Phase: 44-release-publication*
*Completed: 2026-11-02*
