---
phase: 19-nullable-enum-tuple-mapping
plan: "06"
subsystem: planning-disposition-and-phase-gate
tags: [debt-09, disposition, phase-gate, anchors, nullable-tuple]
requires:
  - phase: 19-nullable-enum-tuple-mapping (19-03)
    provides: recorded go for the Nullable(Tuple) approach (Decision, Runs next 19-04)
  - phase: 19-nullable-enum-tuple-mapping (19-04)
    provides: fix commit 2a94e045 that removed the discovery WARN machinery
provides:
  - 15-REVIEW IN-01..IN-05 closed as fixed (superseded) in the archived Phase 15 disposition file, each citing 2a94e045
  - Phase 19 gate evidence - cache-busted testUnit plus two full-suite runs green on latest x V2, anchors invariant PASS, scope and ledger checks for the go branch
affects: [20-release, FORK-DELTA, ROLL-02]
actuals:
  tokens: 1100
  tasks: 2
  commits: 1
plan_head_before: 7b6ec92797f1c0cf8f632f6acd7154138e026ecf
plan_head_after: a159f238ed3bceb8105c6f33957a14a4f9eb0a26
tech-stack:
  added: []
  patterns:
    - "Superseded findings close as fixed with the removing commit cited and the word superseded in the Source cell"
key-files:
  created: []
  modified:
    - .planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout/15-REVIEW-DISPOSITION.md
key-decisions:
  - "Branch read from 19-SPIKE.md Decision (go, Runs next 19-04), not chosen here; outcome commit resolved by subject as 2a94e045"
  - "IN-06 left at deferred: D-12 limits this plan to IN-01..IN-05, so the stale row is left for Phase 20's FORK-DELTA completeness pass"
requirements-completed: [DEBT-09]
coverage:
  - id: D1
    description: "IN-01..IN-05 are fixed (superseded) in frontmatter and table, each Source cell citing the 19-04 fix commit 2a94e045 with a reason; every other row, counter and the recorded value are byte-identical to the phase start"
    requirement: "DEBT-09"
    verification:
      - kind: other
        ref: "Task 1 automated verify (python3 -I disposition check) printed branch go sha 2a94e045 bad [] rest-identical True counters True; git cat-file -e 2a94e045^{commit} succeeds"
        status: pass
    human_judgment: false
  - id: D2
    description: "Phase gate: cache-busted testUnit and two full-suite runs on latest x V2 are green with the 18 named skips, and every Phase 19 class is present"
    requirement: "DEBT-09"
    verification:
      - kind: other
        ref: "bash /tmp/gsd19/exec/gate1.sh (plan Task 2 first automated command, verbatim) printed PHASE-19-SUITE-GREEN"
        status: pass
    human_judgment: false
  - id: D3
    description: "Anchors invariant over every phase commit, phase scope (expected non-.planning file set for the go branch, Table.java and release files untouched, no test names the nullable-tuple server setting) and ledger L-31..L-40"
    requirement: "DEBT-09"
    verification:
      - kind: other
        ref: "bash /tmp/gsd19/exec/gate2.sh (plan Task 2 second automated command, verbatim) printed PHASE-19-GATE-OK; anchors log ends result=PASS"
        status: pass
    human_judgment: false
duration: 10min
completed: 2026-10-12
status: complete
---

# Phase 19 Plan 06: Disposition IN-01..IN-05 and Phase 19 Gate Summary

**15-REVIEW IN-01..IN-05 closed as `fixed` (superseded) against the 19-04 Nullable(Tuple) fix commit `2a94e045`, and the phase gate passes: cache-busted testUnit plus two full-suite runs green on ClickHouse `latest` x client V2, merge-invariant anchors PASS.**

## Performance

- Duration: about 10 minutes wall clock. The three Gradle runs took 4 s (testUnit) and 2m37s / 2m39s (full suite); the plan predicted well over ten minutes for the whole gate, which was an overestimate.
- Tasks: 2 (1 commit; Task 2 changes no tracked file).

## Accomplishments

- Read the branch from `19-SPIKE.md` `## Decision`: `Answer: go`, `Runs next: 19-04`, `Superseded: 19-05`. Resolved the outcome commit by subject: `2a94e045` (`fix(19-04): take Tuple element nullability from the declared type and remove the discovery WARN (COLS-10, COLS-11)`).
- Set IN-01..IN-05 to `fixed` in the frontmatter and the table of `15-REVIEW-DISPOSITION.md`. Every Source cell cites `2a94e045`, starts its reason with `Phase 19, COLS-10` and says `superseded`; IN-02's cell also records that both describe loops now return through one shared Table-building helper.
- Left everything else untouched: rows IN-06, WR-01..WR-03, IN-07..IN-10, the frontmatter entries for them, `open: 0`, `total: 13`, `recorded: 2026-10-10T12:29:38.081Z`, the header, the notes under the table and all ordering.
- Source check for the go branch: `ClickHouseHelperClient.java` no longer contains `newKeySet`.
- Ran the phase gate (below) and the anchors, scope and ledger checks.

### Phase gate evidence

Branch: **go** (19-04 ran, 19-05 superseded). Server: `NULLABLE_TUPLE_MATRIX server version=26.9.17.8`; local `clickhouse/clickhouse-server:latest` image created 2026-10-12 20:53:31 +0300.

| Run | Classes | Tests | Failures | Errors | Skipped | Missing Phase 19 classes | Stale results |
|-----|---------|-------|----------|--------|---------|--------------------------|---------------|
| testUnit | 45 | 545 | 0 | 0 | 2 | none | 0 |
| full run 1 (`test --rerun`) | 83 | 967 | 0 | 0 | 18 (the 18 named skips of Phase 18) | none | 0 |
| full run 2 (`test --rerun`) | 83 | 967 | 0 | 0 | 18 (the 18 named skips of Phase 18) | none | 0 |

- `gate-test-1.log` and `gate-test-2.log` each show `> Task :test` executing (not `UP-TO-DATE`) and `BUILD SUCCESSFUL` (2m 37s and 2m 39s). Result XML timestamps span 2026-10-12T21:34:12Z to 21:36:45Z, after the last code commit.
- Command 1 printed `PHASE-19-SUITE-GREEN`; command 2 printed `PHASE-19-GATE-OK`.
- Anchors gate line: `invariants: provenance_paths=21 anchor_commits=275 anchor_rows=58 marker_commits=322 result=PASS` (`verify-merge-invariants.sh 54e63648 HEAD`).
- Scope: non-.planning paths changed since `ede0a45d` are exactly the expected go-branch set (FORK-DELTA, Column, ClickHouseHelperClient, DeclaredNullability, ClickHouseWriter and the nine test files); `Table.java`, `ColumnTest`, `TableTest`, `VERSION`, `CHANGELOG.md` and `docs/ROLLOUT-v1.7.0_gg1.md` unchanged; no file under `src/test` names `enable_nullable_tuple_type`; ledger holds L-31..L-40 with 8 columns each.
- Logs: `/tmp/gsd19/exec/gate-testUnit.log`, `gate-test-1.log`, `gate-test-2.log`, `anchors-19-06.log` (scratch, not committed).

## Task Commits

1. **Task 1: disposition IN-01..IN-05** - `a159f238` (docs)
2. **Task 2: phase gate** - no tracked-file change, so no commit; evidence recorded above.

The SUMMARY and planning-state update follow in the closing `docs(19-06)` metadata commit.

## Files Created/Modified

- `.planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout/15-REVIEW-DISPOSITION.md` - IN-01..IN-05 frontmatter dispositions and table rows only.

## Decisions Made

- The branch was read from the recorded Decision, not chosen. On go, `fixed` with `superseded` in the Source cell is the accurate word in this file's vocabulary because 2a94e045 removes the code each finding concerns (plan assumption A6).
- IN-06 still reads `deferred` although DEBT-08 closed it in Phase 17. D-12 limits this plan to IN-01..IN-05, so it is deliberately not touched here and is left for Phase 20's FORK-DELTA completeness pass.

## Deviations from Plan

None - plan executed exactly as written. The only variance is timing: the gate finished in about 6 minutes rather than "well over ten". The two verify commands were run verbatim from scratch scripts (`/tmp/gsd19/exec/gate1.sh`, `gate2.sh`) because the first runs in the background.

## Issues Encountered

None. No full-suite failure, so no `latest` image drift investigation was needed.

## Known Stubs

None.

## Threat Flags

None. The plan changed only an archived planning document.

## User Setup Required

None.

## Next Phase Readiness

Phase 19 plans are all complete (19-05 superseded by the recorded go). The tree is green and scoped for phase verification. Carried to Phase 20: the stale IN-06 `deferred` row, and the release docs (VERSION, CHANGELOG, rollout) which this phase intentionally left unchanged.

## Self-Check: PASSED

- `.planning/milestones/.../15-REVIEW-DISPOSITION.md` exists and is modified by commit a159f238 (alone in that commit).
- `git cat-file -e 2a94e045^{commit}` succeeds; a159f238 is an ancestor of HEAD.
- `gsd-tools check evaluation-scope --plan 19-06 --commits-only` returns status resolved with 1 commit (a159f238).
- Task 1 verify re-run: `branch go sha 2a94e045 bad [] rest-identical True counters True`.
- Task 2 verify: `PHASE-19-SUITE-GREEN` and `PHASE-19-GATE-OK` printed.

---
*Phase: 19-nullable-enum-tuple-mapping*
*Completed: 2026-10-12*
