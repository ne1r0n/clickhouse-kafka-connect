---
phase: 48-phase-37-dispositions-document-fixes
plan: "04"
subsystem: docs
tags: [doc-01, doc-02, rollout-notes, fork-delta, evidence-paths, planning-identifiers, doc-edits-json]
status: complete

requires:
  - phase: 48-phase-37-dispositions-document-fixes
    provides: plan 48-01 frozen check scripts (48-path-check.py, 48-id-scan.py, E-02 calibration) and 48-doc-edits.json, plan 48-03 gate head e0c0c9d8 (src frozen since)
provides:
  - the published gg6 note with no planning identifier (row D7 says since v1.7.0_gg7; row C2 and the notice point to FORK-DELTA's decided limits)
  - the gg1 note saying item 6 was measured on the v1.7.0_gg6 tree
  - FORK-DELTA with every evidence path resolving on the tip, item 8 and item 20 rewritten, row 94 and item 14 sentence
  - E-12 and E-13
affects: [48-05, 49]

actuals:
  tokens: 5600
  tasks: 2
  commits: 6
plan_head_before: 401ad168b78164c6616e67c1a9760815f6b1c274
plan_head_after: e6e74b9c02d076f8953da818afe6c64342b74ca8

tech-stack:
  added: []
  patterns: [plan-time JSON text applied by a script that asserts each old string occurs once, replayed against the commit's parent for byte equality, frozen resolver and identifier scan read before and after]

key-files:
  created: []
  modified:
    - docs/ROLLOUT-v1.7.0_gg6.md
    - docs/ROLLOUT-v1.7.0_gg1.md
    - docs/FORK-DELTA.md
    - .planning/phases/48-phase-37-dispositions-document-fixes/48-COMMAND-EVIDENCE.md

key-decisions:
  - "Each key of 48-doc-edits.json was applied unchanged by a script that asserts every old string occurs exactly once; no JSON text was adapted"
  - "The tail gate and the anchors mirror were run once on the final tip as an extra read-only check, although the plan itself names neither; they are recorded here and not in E-12 or E-13"

requirements-completed: [DOC-01, DOC-02, P37-04]

coverage:
  - id: D1
    description: "DOC-01 (U-6, U-7, 44 IN-03): the gg6 note reads ids: hits=0 (was 3: FUT-05 twice, one planning-phase reference); row D7 says since v1.7.0_gg7 and the tripwire test is in the gg7 tag only; row C2 and the notice point to FORK-DELTA items 17 and 24; the note still differs from its v1.7.0_gg6 tag copy in exactly 2a3,4 112c114 146c148; row C2's instruction and the notice's rule are kept; commit equals key rollout_gg6, numstat 3 3"
    requirement: DOC-01
    verification:
      - kind: other
        ref: "P48-04-T1-OK (E-12)"
        status: pass
    human_judgment: false
  - id: D2
    description: "43 IN-02 (U-6): the gg1 notice and item 6 label say measured on the v1.7.0_gg6 tree; commit equals key rollout_gg1, numstat 2 2; identifier count stays 26 and the paths line is unchanged"
    requirement: P37-04
    verification:
      - kind: other
        ref: "P48-04-T1-OK (E-12)"
        status: pass
    human_judgment: false
  - id: D3
    description: "DOC-02 (U-10, U-11, D-2): FORK-DELTA resolves 98 candidates, 70 resolved, 20 abbreviated, 20 unique, 2 placeholders, 6 by design, 0 unresolved (was 99/69/20/20/2/6/2); the 25 and 35 evidence paths sit under .planning/milestones; the removed KeyToValueTest path stays named beside the package path; item 8 points to the gg6 milestone requirements file; item 20 names 48-DISPOSITION-LEDGER.md and git ls-files '*48-DISPOSITION-LEDGER.md'"
    requirement: DOC-02
    verification:
      - kind: other
        ref: "P48-04-T2-OK (E-13)"
        status: pass
    human_judgment: false
  - id: D4
    description: "43 IN-01, 45 IN-04, 46 WR-05 (U-8): row 94 names the compare chain, the tests gate, the tag gate and the publish step in that order; item 14 carries the sentence that a deadline cut on the RowBinary route logs two ERROR lines while the task receives the retriable timeout; the raw-numbers section, both allow-lists, .planning/scripts, CHANGELOG and the gg7 note are unchanged"
    requirement: P37-04
    verification:
      - kind: other
        ref: "P48-04-T2-OK (E-13)"
        status: pass
    human_judgment: false

duration: 16min
completed: 2026-11-06
---

# Phase 48 Plan 04: Published notes and FORK-DELTA edits Summary

**The gg6 note now names no planning identifier (`ids: hits=0`, was 3), the gg1 note says its item 6 was measured on the `v1.7.0_gg6` tree, and FORK-DELTA's resolver reads `unresolved=0` (was 2), each commit byte-equal to its key of `48-doc-edits.json`.**

## Performance

- **Duration:** about 16 min wall clock, plus the extra tail gate (about 8 min, run on the final tip while the SUMMARY was prepared)
- **Started:** 2026-11-06T09:17Z
- **Completed:** 2026-11-06T09:35Z
- **Tasks:** 2 (a tracer and an auto task)
- **Files:** three documents and the evidence file; no `src`, `.github`, `.planning/scripts` or CHANGELOG change

## Accomplishments

- **Inputs:** `/tmp/gsd48/exec` was intact. The md5 values of both per-file XML manifests (`4c17ef56...`, `a3394159...`), both status dumps, both frozen check scripts and `48-doc-edits.json` (`864361fc...`) matched E-01 and E-02, and the XML directories re-hashed to the manifests, so no re-base was needed. E-01..E-11 all read `Verdict: pass` and the working tree held only `.planning/config.json` and `.planning/state.json` as dirty.
- **The four document commits** (one file each, in the plan's order):
  - `5453a69c` the gg6 note (`rollout_gg6`, numstat 3 3). Identifier scan 3 hits before, 0 after, at the commit and at HEAD. Tag hunks `2a3,4 112c114 146c148`. Paths line unchanged (6 candidates, 0 unresolved).
  - `eadacf83` the gg1 note (`rollout_gg1`, numstat 2 2). Identifier count stays 26, paths line unchanged.
  - `8f187c9d` FORK-DELTA DOC-02 (`fork_delta_doc02`, numstat 5 5).
  - `cc2ca206` FORK-DELTA review rows (`fork_delta_reviews`, numstat 2 2).
- **Evidence commits:** `3f5cffc1` (E-12) and `e6e74b9c` (E-13), the index rows flipped from pending to pass.
- **Read checks before editing (all held):** tripwire test in the gg7 tag (1) and not in the gg6 tag (0) and on the tip (1); FORK-DELTA item 17 `(recorded limit, by decision)` and item 24 `FUT-05 is closed by decision`; gg6 row D13 labelled `measured` and naming `Client011BehaviourPinLiveTest`; both milestone evidence files and the gg6 requirements file tracked (5 `FUT-0` definitions); the `KeyToValueTest` package path tracked and the removed path untracked; `CHAIN` order `COMPARE_STEP`, `TESTS_GATE_STEP`, `TAG_GATE_STEP`, `PUBLISH_STEP`; the two RowBinary log formats once each and the retriable throw in `throwIfFired`; the 28 IN-01 move commit touching `DeclaredNullability.java` and the null-binding commit adding `DescribeUnreadableEnumNullBindingUnitTest.java`.
- **Resolver and scan lines, before and after:**

  | Check | Before (E-02) | After |
  |---|---|---|
  | FORK-DELTA resolver | `candidates=99 resolved=69 abbreviated=20 abbreviated_unique=20 placeholders=2 by_design=6 unresolved=2` | `candidates=98 resolved=70 abbreviated=20 abbreviated_unique=20 placeholders=2 by_design=6 unresolved=0`, exit 0, 6 `BY-DESIGN` lines |
  | gg6 note identifiers | `ids: hits=3` | `ids: hits=0` |
  | gg1 note identifiers | `ids: hits=26` | `ids: hits=26` |
  | CHANGELOG, gg6, gg1, gg7 paths lines | E-02 values | equal to E-02 |

- **Nothing else moved:** FORK-DELTA's `## The raw numbers` section equals B0's; the gg6 and gg7 allow-lists are byte-identical to their tags; `.planning/scripts`, `.github`, CHANGELOG and the gg7 note equal B0; `src` equals the 48-03 gate head `e0c0c9d8`; the anchors mirror reads `rows=218 bad=0`.
- **Extra tail gate (not required by the plan):** on the final tip `e6e74b9c`, `invariants: provenance_paths=11 anchor_commits=157 anchor_rows=218 marker_commits=175 anchor_exempt=0 result=PASS`, exit 0 (log `/tmp/gsd48/exec/tail-gate-04.log`). It read git objects only and no Gradle or Testcontainers job ran at the same time.
- **Handed to the milestone close (D-2):** 12 `.planning/phases/45..47` path tokens in FORK-DELTA; they resolve today and move with the archive.

## Task Commits

1. **Task 1 (tracer): the gg6 and gg1 notes** - `5453a69c` (gg6, docs), `eadacf83` (gg1, docs), evidence E-12 `3f5cffc1` (docs)
2. **Task 2: FORK-DELTA** - `8f187c9d` (DOC-02, docs), `cc2ca206` (review rows, docs), evidence E-13 `e6e74b9c` (docs)

## Files Created/Modified

- `docs/ROLLOUT-v1.7.0_gg6.md` - notice, row C2, row D7 (3 lines)
- `docs/ROLLOUT-v1.7.0_gg1.md` - notice and item 6 label (2 lines)
- `docs/FORK-DELTA.md` - evidence paths, the `KeyToValueTest` row, item 8, item 20 (5 lines); row 94 and item 14 (2 lines)
- `.planning/phases/48-phase-37-dispositions-document-fixes/48-COMMAND-EVIDENCE.md` - E-12, E-13 and the flipped index rows

## Decisions Made

- No JSON text was adapted; every `old` string occurred exactly once and every commit replays byte-equal from its parent.
- The tail gate was run although the plan names none, because the anchors file and the merge gate read documents' neighbours; it passed and changes nothing recorded in E-12 or E-13.

## Deviations from Plan

None - plan executed exactly as written. (The additional tail gate run is an extra check, not a change of any file or text.)

One slip of my own, not a deviation: the first call of the evidence helper failed on a stale `pending (plan 48-03)` pattern copied from the previous plan's helper; it asserted before writing anything, so no file changed, and the pattern was corrected for 48-04 (the helper lives under `/tmp/gsd48/exec`, outside the repository).

## Authentication Gates

None.

## Issues Encountered

None. The measured values matched the plan's: resolver 98/70/20/20/2/6/0, gg6 note identifiers 0, gg1 count 26, 12 phase 45..47 path tokens.

## Known Stubs

None.

## Threat Flags

None. Documentation only; no endpoint, auth path, file access or schema changed.

## Notes for the next plans

- **48-05 ledger:** the pointers `gg6_commit` `5453a69c2aa276ef6a4dd6bc3e4389407bb9e9c4` (44-IN-03), `gg1_commit` `eadacf83dbc07a908eb3976bf5cf3decd4c79a95` (43-IN-02) and `fd_reviews_commit` `cc2ca206f79b3347af74027d5d4b490bd02cb860` (43-IN-01 fixed; 45-IN-04 and 46-WR-05 closed by the one sentence); `fd_doc02_commit` is `8f187c9d4acbe891b2338b92273536a9b07e7a81`. FORK-DELTA item 20 names `48-DISPOSITION-LEDGER.md` and `git ls-files '*48-DISPOSITION-LEDGER.md'`, which 48-05 must make print exactly one path.
- **Carried from 48-03 (for the 48-05 ledger, D-3):** the `DeclaredNullability` class Javadoc paragraph that finding 28 IN-01 names was left unreflowed; reflowing it would be a production diff beyond the three-line move.
- **Phase 49 reviewer brief:** the gg6 note, the gg1 note, FORK-DELTA and CHANGELOG. The gg6 and gg1 notes are published operator documents edited here; FORK-DELTA item 20 and row 94 are new prose.
- `48-ledger-check.py` was not run: it checks the ledger that plan 48-05 writes.
- STATE.md, ROADMAP.md, REQUIREMENTS.md, `.planning/config.json` and `.planning/state.json` were not staged by any commit of this plan. Nothing was pushed and no remote ref was touched.

## Self-Check: PASSED

- Files exist: the three documents and `48-COMMAND-EVIDENCE.md` (tracked, committed).
- Commits exist on HEAD: `5453a69c`, `eadacf83`, `3f5cffc1`, `8f187c9d`, `cc2ca206`, `e6e74b9c`; `git rev-list --count 401ad168..HEAD` read 6 before this SUMMARY commit.
- `P48-04-T1-OK` and `P48-04-T2-OK` printed on the committed tip; `git status --short` lists only `.planning/config.json` and `.planning/state.json`.
