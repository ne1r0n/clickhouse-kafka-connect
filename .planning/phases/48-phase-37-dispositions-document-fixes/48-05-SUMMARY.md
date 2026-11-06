---
phase: 48-phase-37-dispositions-document-fixes
plan: "05"
subsystem: release-evidence
tags: [disposition-ledger, allow-list-dry-run, phase-gate, hand-off, p37-01, p37-02, p37-03, p37-04, doc-01, doc-02]
status: complete

requires:
  - phase: 48-phase-37-dispositions-document-fixes
    provides: plans 48-01..48-04 (baselines E-01, frozen scripts, six test pins, the 28 IN-01 move, the document edits; E-01..E-13)
provides:
  - E-14 (the first dry-run measurement, expected 12 rows, reads fail and is kept) and E-15 (the re-measure with 14 exact rows, PASS, supersedes it)
  - 48-DISPOSITION-LEDGER.md, 57 rows (10 fixed, 47 closed) that FORK-DELTA item 20 names
  - E-16 (ledger checks), E-17 (phase gate), E-18 (Phase 49 hand-off, 10 rows)
affects: [49]

actuals:
  tokens: 24200
  tasks: 3
  commits: 8
plan_head_before: f198c812dc99cf4c33340a98102b8b83df3999d0
plan_head_after: 3859525d18ef6f129f55997f7be3ccf1a7603e8a

tech-stack:
  added: []
  patterns: [a fail evidence entry kept and superseded by a re-measure entry, checked by an inline index-cell script because the frozen index --done rejects any fail row]

key-files:
  created:
    - .planning/phases/48-phase-37-dispositions-document-fixes/48-DISPOSITION-LEDGER.md
  modified:
    - .planning/phases/48-phase-37-dispositions-document-fixes/48-COMMAND-EVIDENCE.md
    - .planning/phases/48-phase-37-dispositions-document-fixes/48-05-PLAN.md

key-decisions:
  - "The gg8 allow-list needs 14 exact rows, not 12: DeclaredNullability$1.class and DeclaredNullability$Assignment.class differ from the gg7 archive only in the line number table (the 28 IN-01 move shifted three lines)"
  - "E-14 stays as committed (Verdict fail); E-15 re-measures and supersedes it; the pending entries were renumbered E-16..E-18"
  - "The ledger hand-off is a numbered list, not a table, because 48-ledger-check.py reads every line that starts with a pipe as a six-cell ledger row"

requirements-completed: [P37-01, P37-02, P37-03, P37-04, DOC-01, DOC-02]

coverage:
  - id: D1
    description: "P37-03: the published gg6 to gg7 pair reads RESULT: PASS with 0 unallowed and 0 stale (totals 7513/7512/1/0/119/7393), and the Phase 48 tip against the gg7 archive reads 14 exact UNALLOWED class paths with a comment-only list and RESULT: PASS with 0 unallowed and 0 stale with a throwaway 14-row exact draft kept in /tmp (E-15; E-14 is the superseded first measurement)"
    requirement: P37-03
    verification:
      - kind: other
        ref: "P48-05-T1-OK (E-14, E-15)"
        status: pass
    human_judgment: false
  - id: D2
    description: "P37-01, P37-02, P37-04: 48-DISPOSITION-LEDGER.md holds the 57 rows (10 fixed with Phase 48 commits equal to their evidence keys, 47 closed with reasons, triggers and the owner fork maintainer); 26 IN-02 carries 15 compile errors, 27 WR-01 5 Docker-free tests, 27 IN-01 points at f13d8378; check rows=57 required=57 problems=0, counts rows=57 fixed=10 closed=47 pointers=14"
    requirement: P37-04
    verification:
      - kind: other
        ref: "P48-05-T2-OK (E-16)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Phase gate: cache-busted testUnit with Docker unreachable 2320 tests (2 skips) and one full suite on latest 26.10.3.7 x V2 2923 tests (18 named skips), deltas against E-01 add exactly the 25 expected rows; src/main differs only by three deleted DeclaredNullability lines; anchors 218; tail gate PASS; nothing pushed"
    requirement: P37-01
    verification:
      - kind: other
        ref: "P48-05-T3-OK (E-17, E-18)"
        status: pass
    human_judgment: false

duration: about 1 h 10 min wall clock, three stops included
completed: 2026-11-06
---

# Phase 48 Plan 05: Dry runs, disposition ledger, phase gate and Phase 49 hand-off Summary

**The ledger holds all 57 carried review rows (10 fixed with their Phase 48 commits, 47 closed with their own reasons), the phase gate is green (testUnit 2320, suite 2923 on ClickHouse 26.10.3.7 x V2, tail gate PASS), and the gg8 allow-list was measured at 14 exact rows, not the 12 the plan assumed.**

## Accomplishments

- **Dry runs (E-14, E-15).** The published pair (`v1.7.0_gg6` against `v1.7.0_gg7` archive with the committed gg7 list) reads exit 0, `RESULT: PASS`, 0 `UNALLOWED`, 0 `STALE-ROW`, totals `7513/7512/1/0/119/7393`. The Phase 48 tip against the gg7 archive reads 14 exact `UNALLOWED` class paths with a comment-only list (11 `CHANGED`, 3 `ONLY-B`: `ClickHouseWriter$3`, `$InsertBody`, `$InsertWriteDeadline`) and `RESULT: PASS` with 0/0 with a 14-row exact draft (0 wildcards, kept in `/tmp/gsd48/exec`, totals `7512/7515/0/3/11/7501`). The three archives were built twice (E-14, E-15) and have identical SHA-256 values.
- **Ledger.** `48-DISPOSITION-LEDGER.md` (commit `5ae549b9`): 57 rows, `check: rows=57 required=57 problems=0`, `rows=57 fixed=10 closed=47 pointers=14`. All 14 pointers resolve, touch their required file, and the Phase 48 ones equal their evidence keys and follow B0. 26 IN-02 carries `15 compile errors`, 27 WR-01 `5 Docker-free tests`, 27 IN-01 points at `f13d8378`. Each closed reason cites a line, test or commit that was re-read on the tree before it was written. The `DeclaredNullability` class Javadoc paragraph that finding 28 IN-01 asked to reflow is left unreflowed under D-3 and is noted in that row. FORK-DELTA item 20's lookup `git ls-files '*48-DISPOSITION-LEDGER.md'` prints exactly one path.
- **Phase gate (E-17), one Gradle or Testcontainers job at a time.** testUnit with `DOCKER_HOST=unix:///nonexistent.sock`: 2320 tests, 0 failures, 0 errors, 2 skips, delta against E-01 `removed=0 added=25 expected=25 match=yes`. Full suite on `latest` x client V2: 2923 tests, 0 failures, 0 errors, the 18 named skips, 3 min 11 s, server `26.10.3.7`, digest `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (equal to E-01 and E-11), delta by class `removed=0 added=25 expected=25 match=yes`. `ProcessingTest` 14 of 14 green in both. Tail gate on the explicit SHA `97b60d3a`: `invariants: provenance_paths=11 anchor_commits=164 anchor_rows=218 marker_commits=182 anchor_exempt=0 result=PASS`. Scope: `src/main` differs from B0 only by the three deleted lines of `DeclaredNullability.java`; the eleven high-risk and neighbouring production files are unchanged, so the `BEFORE`/`AFTER` flag sequence is unchanged; two test files added, eight changed with the four extended files losing no line; build files, `.github`, `VERSION`, `CHANGELOG.md` and `.planning/scripts` unchanged; docs changed only in FORK-DELTA and the gg1 and gg6 notes; resolver `unresolved=0`, gg6 scan `ids: hits=0`.
- **Hand-off (E-18, recorded and not acted on).** Ten rows; see below.

## Task Commits

1. **Task 1 (tracer): dry runs** - `76d6b563` E-14 (fail, the 12-row expectation missed), plan amendments `50ff0c23` and `62aa7231`, `889fc790` E-15 (14 rows, pass)
2. **Task 2: ledger** - `5ae549b9` (ledger), `97b60d3a` E-16
3. **Task 3: gate and hand-off** - `11daf819` E-17, `3859525d` E-18

## Files Created/Modified

- `.planning/phases/48-phase-37-dispositions-document-fixes/48-DISPOSITION-LEDGER.md` - created, 57 rows
- `.planning/phases/48-phase-37-dispositions-document-fixes/48-COMMAND-EVIDENCE.md` - E-14..E-18 and the index (E-01..E-18)
- `.planning/phases/48-phase-37-dispositions-document-fixes/48-05-PLAN.md` - amended for 14 rows and the renumbering

## Decisions Made

- E-14 is never edited: it stays `Verdict: fail` with its index cell `fail`; E-15 cites it as superseded (`supersedes_entry: E-14`). The frozen `index --done` and `rows` helpers reject a fail row, so the amended plan checks the index with `index "${E}" 18` and an inline script (E-14 `fail`, every other cell `pass`), and calls `rows` with every ID except E-14. No helper was edited.
- The ledger's hand-off section is a numbered list, because `48-ledger-check.py` parses every line starting with `| ` as a six-cell row.
- The gg6 and gg7 allow-lists and `.planning/scripts` stayed untouched; no draft list was written inside the repository.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Plan assumption] The tip changes 14 archive classes against the gg7 archive, not 12**
- **Found during:** Task 1 (E-14)
- **Issue:** the plan assumed only `DeclaredNullability.class` changes beyond the research's 11. `DeclaredNullability$1.class` and `DeclaredNullability$Assignment.class` also differ, with bytecode equal to the gg7 archive's and only the `LineNumberTable` different (the 28 IN-01 move deleted three lines above their code). The 12-row draft read FAIL with 2 UNALLOWED.
- **Fix:** stopped as the plan instructs, committed E-14 as `fail`, got a decision (a plan-assumption miss, not a regression), amended the plan to 14 rows, re-measured in E-15.
- **Files modified:** `48-05-PLAN.md`, `48-COMMAND-EVIDENCE.md` (index and entries only; E-14's entry is unedited)
- **Commits:** `76d6b563`, `50ff0c23`, `62aa7231`, `889fc790`

**2. [Rule 3 - Blocking] The frozen helpers reject a fail row**
- **Found during:** after E-14 (before E-15)
- **Issue:** `index --done` and `rows` require every named row to read `pass`.
- **Fix:** with approval, changed only the plan's verifies (see Decisions Made); helpers untouched.

**3. [Rule 1 - Format] Ledger hand-off as a list**
- **Issue:** a four-column table in the ledger would be read as malformed ledger rows.
- **Fix:** numbered list; the E-18 evidence entry carries the table.

**Total deviations:** 3 (1 plan-assumption correction, 1 verification-design change, 1 format). **Impact:** none on code; the gg8 allow-list count Phase 49 inherits is corrected from 12 to 14.

## Authentication Gates

None.

## Issues Encountered

- The first dry-run measurement missed the plan's table (E-14, see Deviations). No JVM crash, no core dump, no Ryuk failure; `-PjavaVersion=21` was used only to build the dry-run archives.
- Upstream `main` is still `83808df9`; the origin sync branch is absent and `pre-v1.7.0_gg8` is absent on origin (read through `git ls-remote` and `gh api`); nothing was pushed and no remote ref was touched.
- Two research details were not copied because they did not read the same on the tree: no row of `fork-anchors.txt` names `JsonAsStringKeyResidualLiveTest`, and the number of `Shape` rows was not re-counted. The coordinator's note that the Phase 47 E-11 hand-off says 12 does not match the file: E-11 carries no row count; the figure 12 stands in the ROADMAP line of plan 48-05 and in E-14.

## Known Stubs

None.

## Threat Flags

None. Documents and measurements only; no endpoint, auth path, file access or schema changed.

## Hand-off to Phase 49

| # | Item | Owner | What to do |
|---|------|-------|------------|
| 1 | gg8 allow-list: **14 exact rows**, not 12 | Phase 49 (RLS-02) | Generate last; the 12 paths of E-14 plus `com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability$1.class` and `.../DeclaredNullability$Assignment.class`; totals `changed 11 same 7501`; first line names `.github/scripts/compare_confluent_archives.py`; `AllowListWildcardRuleUnitTest` gates any wildcard row; regenerate rather than copy |
| 2 | Stale 12 | orchestrator | ROADMAP line of plan 48-05 says a 12-row draft; correct to 14 |
| 3 | Reviewer brief | Phase 49 (RLS-03) | gg6 note, gg1 note, FORK-DELTA, CHANGELOG |
| 4 | gg8 CHANGELOG | Phase 49 (RLS-01) | Known Issues line for the two ERROR lines of a deadline cut; may name the removed `DeclaredNullability.ESCAPED_NEWLINE` |
| 5 | gg8 rollout note | Phase 49 (RLS-03) | the same double-ERROR line for operators |
| 6 | Raw numbers | Phase 49 (RLS-01) | refresh once with E-17's totals (2320 / 2923 / 18 skips / anchors 218) |
| 7 | Evidence paths | milestone close (D-2) | repoint the 12 `.planning/phases/45..47` tokens of FORK-DELTA and re-run `48-path-check.py` |
| 8 | Item 20 lookup | milestone close | re-run `git ls-files '*48-DISPOSITION-LEDGER.md'` after the archive |
| 9 | REQUIREMENTS | orchestrator | see below |
| 10 | Backlog | fork maintainer | see below |

## Orchestrator

Set the REQUIREMENTS.md checkboxes and traceability rows of P37-01, P37-02, P37-03, P37-04, DOC-01 and DOC-02 complete; correct the ROADMAP wording of plan 48-05 (14 rows). STATE.md, ROADMAP.md, REQUIREMENTS.md, `.planning/config.json` and `.planning/state.json` were not staged by this plan; the plan-completion STATE and ROADMAP updates are left to the orchestrator.

## Backlog candidates

Closed in the ledger and not changed in this phase: the `DeclaredNullability` class Javadoc reflow (D-3), the removal of `shouldSkipDescribeRow` (U-1), the enum label text of 27 WR-01 (U-2).

## Self-Check: PASSED

- Files exist: the ledger, the evidence file and the amended plan (tracked, committed).
- Commits on HEAD: `76d6b563`, `50ff0c23`, `62aa7231`, `889fc790`, `5ae549b9`, `97b60d3a`, `11daf819`, `3859525d` (`git rev-list --count f198c812..HEAD` read 8 before this SUMMARY commit).
- `P48-05-T1-OK` printed on the committed tip, `P48-05-T2-OK` printed on the committed tip, `P48-05-T3-OK` printed with the live tail gate on `97b60d3a` (the only difference to the committed run is the evidence-file cleanliness check, true after the E-18 commit); `git status --short` lists only `.planning/config.json` and `.planning/state.json`.
