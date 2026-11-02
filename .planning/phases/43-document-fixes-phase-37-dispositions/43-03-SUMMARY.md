---
phase: 43-document-fixes-phase-37-dispositions
plan: "03"
subsystem: docs-ledger
tags: [disposition-ledger, fork-delta, phase-gate, phase-37-dispositions, doc-02, doc-01]

requires:
  - phase: 43-document-fixes-phase-37-dispositions
    provides: "plan 43-01 code commits C1..C4 (header comment, CHAIN tag gate, zero-jar hint, gg4/gg5 allow-list line 1) and plan 43-02 docs commits D1 and D2 with evidence E-01..E-08"
provides:
  - "43-DISPOSITION-LEDGER.md: 18 rows for the 17 findings Phase 37 left open (4 fixed with 40-hex pointers, 14 dispositioned with owner and re-open trigger), the user's three answers quoted verbatim, twelve carried lines, the Phase 44 handoff"
  - "43-ledger-check.py: read-only gate (check, pointers, counts, status, pointer, carried) derived from the Phase 37 checker"
  - "docs/FORK-DELTA.md items 19 (closed), 20 (ledger pointer) and 21 (gg6 condition held)"
  - "E-09 and E-10 in 43-COMMAND-EVIDENCE.md: every index row reads pass; the phase gate is recorded"
affects: [44-release-gg7, rls-03-review, rsk-03]

actuals:
  tokens: 8700
  tasks: 2
  commits: 3
plan_head_before: 5f32e312000dbaa058bc8331c46694c545cd7686
plan_head_after: 0b8f26d8a32039ce7be97633a775c697a72ea8fa

tech-stack:
  added: []
  patterns:
    - "A ledger checker that resolves each fixed row's pointer to the code commit of the evidence entry and to exactly that commit's file list"
    - "Carried rows copied verbatim and compared against the superseded ledger by a read-only subcommand"

key-files:
  created:
    - .planning/phases/43-document-fixes-phase-37-dispositions/43-DISPOSITION-LEDGER.md
    - .planning/phases/43-document-fixes-phase-37-dispositions/43-ledger-check.py
  modified:
    - docs/FORK-DELTA.md
    - .planning/phases/43-document-fixes-phase-37-dispositions/43-COMMAND-EVIDENCE.md

key-decisions:
  - "43-03: Phase 44 handoff: the gg7 rollout note states 'FUT-05 stays open' in its RSK-03 section, and the RLS-03 reviewer brief covers docs/ROLLOUT-v1.7.0_gg1.md, docs/ROLLOUT-v1.7.0_gg6.md and docs/FORK-DELTA.md as edited in Phase 43, with FUT-05 in the gg6 note deliberate (43-DISPOSITION-LEDGER.md, Handoff to Phase 44)"
  - "43-03: no src/main row was approved; 26-IN-02, 27-WR-01, 27-IN-01 and 28-IN-01 are dispositioned in the user's words 'Defer all four (Recommended)', and 28-IN-05(b) is dispositioned with 37-IN-04 fixed under 'Approve 37 IN-04, defer 28 IN-05(b) (Recommended)'"
  - "43-03: 37-IN-05 is split: (a) fixed for the gg4 and gg5 allow-lists, (b) the 25 and gg6 lists deferred so the gg6 list never diverges from its copy in tag v1.7.0_gg6"
  - "43-03: the full ClickHouse suite and the cumulative invariant script were not run at the phase gate (src/main unchanged, no anchored line moved); Phase 44 runs the two full suites and the anchors gate on the release tip"

patterns-established:
  - "Re-owning a row whose owner phase has finished: 30-IN-04 moves to the fork maintainer with the gg6 publication fact as its evidence"

requirements-completed: [DOC-02, DOC-01]

coverage:
  - id: D1
    description: "Disposition ledger with one row per Phase 37 item (37-IN-05 split), fixed rows pointing at the Phase 43 code commits, every other row owned by the fork maintainer with a re-open trigger"
    requirement: "DOC-02"
    verification:
      - kind: other
        ref: "python3 -I 43-ledger-check.py check 43-DISPOSITION-LEDGER.md (OK ledger: 18 rows, 0 problems; counts rows=18 bases=17 fixed=4 dispositioned=14)"
        status: pass
      - kind: other
        ref: "python3 -I 43-ledger-check.py pointer and git diff-tree for C1..C4 (E-09 pointers_match_evidence, pointer_files_ok)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The four src/main rows and 28-IN-05(b) are deferred and quote the user's answers verbatim; the ledger records no approval that is not in 43-RESEARCH.md at f8ff8fc3"
    requirement: "DOC-02"
    verification:
      - kind: other
        ref: "E-09 user_quotes_rows: yes; E-10 src_main_unchanged: yes"
        status: pass
    human_judgment: false
  - id: D3
    description: "FORK-DELTA item 19 closed and citing the header commit, item 20 pointing at the ledger, item 21 recording that the gg6 condition held; exactly three lines changed"
    requirement: "DOC-01"
    verification:
      - kind: other
        ref: "E-09 forkdelta_items_scope: removed=3 added=3; item19_cites_c1: yes; old_ledger_path_absent: yes"
        status: pass
    human_judgment: false
  - id: D4
    description: "Phase gate: cache-busted testUnit adds exactly the two planned testcases, the anchors mirror holds, scope and origin facts hold"
    requirement: "DOC-02"
    verification:
      - kind: unit
        ref: "./gradlew testUnit --rerun (1826 tests, 0 failures, 0 errors, 2 skipped); 43-testcase-status.py delta removed=0 added=2"
        status: pass
      - kind: other
        ref: "43-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190 (rows=190 bad=0)"
        status: pass
    human_judgment: false

duration: 13min
completed: 2026-11-02
status: complete
---

# Phase 43 Plan 03: The disposition ledger, FORK-DELTA closeout and the phase gate Summary

**An 18-row disposition ledger with a pointer-resolving checker gives every Phase 37 leftover a fixed commit or an owned deferral (the four src/main rows deferred in the user's own words), FORK-DELTA items 19 to 21 agree with it, and a cache-busted testUnit of 1826 cases proves only the two planned tests were added.**

## Performance

- **Duration:** 13 min
- **Started:** 2026-11-02T19:39:00Z (approximate; the start time was not captured by a command)
- **Completed:** 2026-11-02T19:52:00Z
- **Tasks:** 2
- **Files modified:** 4 (2 created, 2 modified)

## Accomplishments

- `43-DISPOSITION-LEDGER.md` holds 18 rows for the 17 findings. Four are `fixed` (37-HDR at `566f92e4`, 37-IN-04 at `33d079a9`, 37-IN-05(a) at `103221a4`, 37-IN-06 at `7796ad0a`) and each pointer resolves to a commit that touches exactly the files of its row. Fourteen are `dispositioned` with the owner `fork maintainer (the user)` and a re-open trigger, among them all four `src/main` rows and 28-IN-05(b). The twelve other dispositioned rows of the Phase 37 ledger are carried verbatim, one line each.
- `43-ledger-check.py` is a copy of the Phase 37 checker with the eight planned changes. It rejects the Phase 37 ledger, a blank owner, a 7-hex pointer and an altered carried trigger (all four negative checks recorded in E-09), and prints the pointer and carried results the verify compares with the evidence.
- `docs/FORK-DELTA.md`: item 19 is closed and cites `566f92e4`, item 20 names `43-DISPOSITION-LEDGER.md` instead of the moved Phase 37 path, item 21 records that for `v1.7.0_gg6` the merged SHA `72e8ccff` has exactly one `tests.yaml` run, `41297502026`. Exactly three physical lines changed.
- Phase gate (E-10): cache-busted `testUnit` with Docker unreachable ran 1826 testcases (E-01 baseline 1824 plus 2), 0 failures, 0 errors, 2 skipped; the status delta is removed=0 added=2 and equals the expected file. The anchors mirror reads `rows=190 bad=0`, the only anchored path changed since the phase start is `increment-version.yml` and none of its anchored strings is on a changed line. `src/main`, `src/testFixtures`, `src/integrationTest`, build files, `VERSION`, `CHANGELOG.md` and `fork-anchors.txt` are byte-identical to the phase start; the only removed test-source line is the old size condition of `ConfluentArchiveCheck.run`; origin `custom_build` is unchanged at `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` and nothing was pushed.

## Task Commits

1. **Task 1 (tracer): the disposition ledger, its checker and FORK-DELTA items 19 to 21** - `e72447b5` (docs), evidence `c55ff51d` (E-09)
2. **Task 2: the phase gate** - evidence only, `0b8f26d8` (E-10)

**Plan metadata:** the commit that adds this SUMMARY (docs: complete plan). The measured count of `5f32e312..HEAD` before it is 3 (`e72447b5`, `c55ff51d`, `0b8f26d8`).

## Files Created/Modified

- `.planning/phases/43-document-fixes-phase-37-dispositions/43-DISPOSITION-LEDGER.md` - the ledger, the user's answers, the carried lines and the Phase 44 handoff
- `.planning/phases/43-document-fixes-phase-37-dispositions/43-ledger-check.py` - read-only ledger gate
- `docs/FORK-DELTA.md` - items 19, 20 and 21
- `.planning/phases/43-document-fixes-phase-37-dispositions/43-COMMAND-EVIDENCE.md` - entries E-09 and E-10; every index row reads `pass`

## Decisions Made

- No `src/main` row was approved, so none was fixed; all four are deferred in the user's words (`Defer all four (Recommended)`), as is 28-IN-05(b) (`Approve 37 IN-04, defer 28 IN-05(b) (Recommended)`). Approvals are quoted from `43-RESEARCH.md` at `f8ff8fc3` only.
- 37-IN-05 is split so the gg6 allow-list (and its twin `-25`) stays identical to its copy in tag `v1.7.0_gg6`.
- The full ClickHouse-backed suite and the cumulative invariant script were deliberately not run at the gate; the reasons are recorded in E-10 (`full_suite_reason`, `invariants_reason`).

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. The first helper calls from zsh failed on word splitting (an unquoted multi-word variable) and were re-run under `/bin/bash`; no file was affected.

## Known Stubs

None.

## Threat Flags

None. The plan adds no network endpoint, auth path or file-access surface; it changes a read-only checker, a planning ledger and three documentation lines.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Handoff to Phase 44 (full list, from `43-DISPOSITION-LEDGER.md`, section "Handoff to Phase 44"):

1. The gg7 rollout note states "FUT-05 stays open" in its RSK-03 section (the user's answer "gg6 note C2 + gg7 handoff (Recommended)"); RLS-03's review checks it there.
2. The reviewer brief of RLS-03 covers the published documents edited in Phase 43: `docs/ROLLOUT-v1.7.0_gg1.md` and `docs/ROLLOUT-v1.7.0_gg6.md` (commit `965e646f`) and `docs/FORK-DELTA.md` (commit `870fe9d8` and the commit that added the ledger, `e72447b5`), and says that FUT-05 in row C2 and in the correction notice of the gg6 note is deliberate, and that the section "The bypass of errors.tolerance" of the gg6 note (about line 121) describes the same redelivery without the ID by the user's choice (FUT-05 is named only in row C2 and the notice).
3. If Phase 44 corrects the gg6 note's sentence on the insert timeout with `async=false` (assigned to Phase 44 by `41-03-SUMMARY.md`), it extends the gg6 correction notice to name that row too.
4. Raw numbers: Phase 43 repointed only the gg6 reproduction commands and the gg5-to-gg6 paragraph to tags; the `HEAD`-relative commands of the gg4-to-gg5 paragraph (`git diff --shortstat v1.7.0_gg4 HEAD`) and of the import-order recount (`git diff --quiet v1.7.0 HEAD`) are left for Phase 44's one raw-numbers refresh, with the new gg7 row.
5. UF-37-3 and 37-IN-05(b): the verify of the generated gg7 allow-list checks that every wildcard row follows at least two literal path segments (true for all four shipped lists, measured in Phase 43 research), and its first line names `.github/scripts/compare_confluent_archives.py`.
6. Carried from Phase 38 as information: the SAME first-chunk case is unstated (`38-03-SUMMARY.md`, "Issues Encountered"), and section B of the gg6 note lists configurations it does not characterise.
7. FORK-DELTA item 20 names the ledger by file name; when the milestone is archived, the Phase 43 directory moves under `.planning/milestones/`.

Also for the orchestrator: `post_review_recheck` in E-10 is pending the code review of this phase. After the review and any review fix, re-run the scoped Spotless hook and the filtered `testUnit` of each class a fix touches, the anchors mirror at rows=190, and the ledger check; a review fix is its own commit.

## Self-Check: PASSED

- `43-DISPOSITION-LEDGER.md`, `43-ledger-check.py`: present; `docs/FORK-DELTA.md` modified in `e72447b5`.
- Commits `e72447b5`, `c55ff51d` and `0b8f26d8` are ancestors of HEAD.
- Both task verifies print `P43-03-T1-OK` and `P43-03-T2-OK`.

---
*Phase: 43-document-fixes-phase-37-dispositions*
*Completed: 2026-11-02*
