---
phase: 42-isolated-high-risk-edits
plan: "04"
subsystem: testing
tags: [git-revert, throwaway-worktree, merge-invariants, fork-anchors, phase-gate, evidence]

requires:
  - phase: 42-isolated-high-risk-edits
    provides: the three gated code commits (RSK-01 cba0909b, RSK-02 c8574890, RSK-03 4935e9c5), E-01..E-10, the evidence helpers and the record placement rule
provides:
  - "E-11: each RSK code commit reverts alone in a throwaway detached worktree - clean, exactly inverse, production file back to phase start, anchors rows=188 bad=0, cache-busted testUnit green at the E-10 total minus 6, 38 and 21"
  - "E-12: the cumulative merge-invariant script run once on an explicit SHA ended result=PASS with anchor_rows=190, plus the phase facts (three src/main files, three non-planning commits, untouched files, no existing test modified, four tests added, origin unmoved, nothing pushed)"
  - "every index row E-01..E-12 of 42-COMMAND-EVIDENCE.md reads pass"
affects: [43-document-fixes, 44-release-notes]

actuals:
  tokens: 3000
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Revert independence measured, not assumed: git revert --no-commit of each code commit in a detached throwaway worktree, with the saved diff compared against the live inverse of the commit"
    - "One invariant-script run on an explicit SHA covers every commit of the range, with the anchors mirror re-read at the final row count"

key-files:
  created:
    - .planning/phases/42-isolated-high-risk-edits/42-04-SUMMARY.md
  modified:
    - .planning/phases/42-isolated-high-risk-edits/42-COMMAND-EVIDENCE.md

key-decisions:
  - "No production or test file was touched in this plan: it only measures and records, so the three RSK edits stay exactly as gated"
  - "The 40-minute invariant script ran once, alone, on the E-11 commit (non-planning tree equal to the RSK-03 commit); a post-review fix needs it again only if it changes an anchored line or fork-anchors.txt"

patterns-established:
  - "Record placement rule confirmed by measurement: no two Phase 42 insertions are adjacent in FORK-DELTA or fork-anchors.txt, so each revert applies without conflict"

requirements-completed: [RSK-01, RSK-02, RSK-03, RSK-04]

plan_head_before: 7fdfdaff339b12e567ee8b1dadbc0d4acfb062ec
plan_head_after: cb4170d62f2bfbd531cf26d1c3f28efa4a698f06

coverage:
  - id: D1
    description: "Each of the three RSK code commits reverts alone in a throwaway worktree at the final head: no conflict, the reverted paths equal the commit's paths, the diff is the exact inverse, the production file equals the phase start, the new tests are gone, the anchors mirror reads rows=188 bad=0, and a cache-busted testUnit with Docker unreachable exits 0 with 0 failures and 2 skips at 1816, 1784 and 1801 tests (E-10's 1822 minus 6, 38 and 21)"
    requirement: RSK-01
    verification:
      - kind: other
        ref: "42-COMMAND-EVIDENCE.md E-11; P42-04-T1-OK (git revert --no-commit per commit in /tmp/gsd42/exec/revert-wt, testUnit XML copies xml-revert-c1..3)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Phase gate: verify-merge-invariants.sh 54e63648 cca74819 ends result=PASS with anchor_rows=190 and exit 0 over 1428 commits; src/main differs from phase start in exactly ClickHouseSinkTask.java, Processing.java and Utils.java; the only non-planning commits are the three code commits; ClickHouseSinkConfig, sink/state, sink/kafka, ProcessingTest, VERSION, CHANGELOG.md, the published rollout notes and the FORK-DELTA raw numbers are unchanged; no existing test was modified and exactly four test files were added; origin did not move and nothing was pushed"
    requirement: RSK-04
    verification:
      - kind: other
        ref: "42-COMMAND-EVIDENCE.md E-12; P42-04-T2-OK (live recomputation of every scope fact plus python3 -I 42-evidence-check.py index E 12 --done)"
        status: pass
    human_judgment: false

duration: 45min
completed: 2026-11-02
status: complete
---

# Phase 42 Plan 04: Phase gate - revert independence and the cumulative invariants Summary

**Each of the three RSK code commits reverts alone in a throwaway worktree (clean, exactly inverse, green Docker-free testUnit), and the merge-invariant script ended `result=PASS` with `anchor_rows=190` on one explicit SHA, closing Phase 42 with `src/main` changed only in `ClickHouseSinkTask.java`, `Processing.java` and `Utils.java`.**

## Performance

- **Duration:** 45 min
- **Started:** 2026-11-02T13:09:15Z
- **Completed:** 2026-11-02T13:55Z
- **Tasks:** 2
- **Files modified:** 1 (the evidence file; plus this summary)

## Accomplishments

- E-11 measured the phase goal "any one can be reverted alone". In a detached worktree at the final head, `git revert --no-commit` of the RSK-01, RSK-02 and RSK-03 code commits each applied without conflict, touched exactly the commit's own paths, and produced the exact inverse of the commit's `+`/`-` lines. Each reverted tree has its production file equal to the phase start and its new tests gone, reads `rows=188 bad=0`, and runs a cache-busted `testUnit` (Docker unreachable) at 1816, 1784 and 1801 tests with 0 failures, 0 errors and 2 skips. The worktree was removed and the main tree outside `.planning/` was empty before and after.
- E-12 ran `verify-merge-invariants.sh 54e63648 cca74819...` once and alone (13:11:36Z to 13:53:45Z): `anchor_commits=1428 anchor_rows=190 marker_commits=1494 anchor_exempt=19 result=PASS`, exit 0. The head's non-planning tree equals the RSK-03 commit.
- Phase scope was recomputed live: `src/main` differs from the phase start in exactly the three declared files; the non-planning commits are exactly the three code commits, each holding its production file, its new tests, `docs/FORK-DELTA.md` and `fork-anchors.txt`; the sink configuration, the state paths, `ProcessingTest`, `VERSION`, `CHANGELOG.md`, the published rollout notes and the FORK-DELTA raw numbers are unchanged; no existing test was modified, deleted or renamed, and exactly the four planned test files were added. `origin/custom_build` still equals the E-01 value.
- All twelve index rows E-01..E-12 read `pass`.

## Task Commits

1. **Task 1 (tracer): each code commit reverts alone** - `cca74819` (docs: E-11)
2. **Task 2: the phase gate** - `cb4170d6` (docs: E-12)

**Plan metadata:** the SUMMARY commit that follows this file (docs: complete plan), then the roadmap commit.

## Files Created/Modified

- `.planning/phases/42-isolated-high-risk-edits/42-COMMAND-EVIDENCE.md` - entries E-11 and E-12 and their index rows (the only file this plan changes besides this summary)

## Decisions Made

- None beyond the plan: it measures and records. The matrix label difference (ROADMAP and RSK-02 say 184 executions, the matrix runs 216 since Phase 27) is recorded in E-12 as `matrix_label_note` and not edited in this phase.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. The reverted-tree builds were fast because Gradle's build cache served the compile steps; `--rerun` still re-executed `testUnit` on each tree (88 result files, the expected totals). No crash file appeared and no Gradle run overlapped the invariant script (the idle daemon was stopped first).

## Known Stubs

None.

## Threat Flags

None. This plan adds evidence only; no endpoint, auth path, file access or schema surface.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

All four plans of Phase 42 have run and every evidence row passes. The orchestrator runs code review and any review fixes next. For a post-review fix, re-read the anchors mirror at rows=190 and run the classes the fix touches; a fix to `src/main` is a new commit and needs the E-11 revert check re-run for the commit it touches; the 40-minute invariant script needs a re-run only when a fix changes an anchored line or `fork-anchors.txt`. `.planning/config.json` and `.planning/state.json` are still modified for the orchestrator, and `.planning/STATE.md` is updated by the state verbs and left for the orchestrator to commit.

## Self-Check: PASSED

- The evidence file exists on disk and carries E-11 and E-12 with `Verdict: pass`.
- Commits `cca74819` and `cb4170d6` are on `sync/v1.7.0`.
- `P42-04-T1-OK` and `P42-04-T2-OK` were printed after the evidence commits.

---
*Phase: 42-isolated-high-risk-edits*
*Completed: 2026-11-02*
