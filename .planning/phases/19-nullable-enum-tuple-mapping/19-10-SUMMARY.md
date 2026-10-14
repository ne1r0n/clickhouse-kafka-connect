---
phase: 19-nullable-enum-tuple-mapping
plan: "10"
subsystem: testing
tags: [suite-gate, anchors, review-disposition, roadmap-carry-forward, gap-closure]
requires:
  - phase: 19-nullable-enum-tuple-mapping
    provides: "19-07 CR-01 fix, 19-08 total pass and test fixes, 19-09 wrapper-edge pins"
provides:
  - "Cache-busted testUnit and two full runs on latest x V2 green after gap closure"
  - "Anchors invariant PASS over every commit; scope set and ledger L-31..L-41 verified"
  - "19-REVIEW-DISPOSITION.md with all eight findings triaged, open 0"
  - "ROADMAP Phase 20 carry-forward block for WR-02 (L-38), PUB-07 VERSION/CHANGELOG, IN-04"
affects: [phase-20, 19-VERIFICATION]
actuals:
  tokens: 3500
  tasks: 3
  commits: 1
  plan_head_before: 27dd5e80601ee02ab7c360f76640b2943f49d995
  plan_head_after: ea20cf0a8dab6deb8082a884157a645836329b43
tech-stack:
  added: []
  patterns: ["evidence-before-record: gate logs older than the docs commit, newer than the last code commit"]
key-files:
  created: []
  modified:
    - .planning/phases/19-nullable-enum-tuple-mapping/19-REVIEW-DISPOSITION.md
    - .planning/ROADMAP.md
key-decisions:
  - "WR-02 and IN-04 are deferred into the ROADMAP Phase 20 entry (not Phase 19 files) so Phase 20 planning reads them"
  - "IN-02 skipped: the unused STORES_WRONG branch is part of the permanent D-05 record"
requirements-completed: [COLS-10, COLS-11]
coverage:
  - id: D1
    description: "Phase 19 suite green after gap closure: cache-busted testUnit plus two full runs on latest x V2, 18 named skips, matrix at 30 and CR-01 live class at 2"
    requirement: "COLS-10"
    verification:
      - kind: command
        ref: "Task 1 automated command -> PHASE-19-GAP-SUITE-GREEN"
        status: pass
    human_judgment: false
  - id: D2
    description: "Anchors invariant over every commit, exact scope set, ledger L-31..L-41, protected files unchanged"
    requirement: "COLS-11"
    verification:
      - kind: command
        ref: "Task 2 automated command -> PHASE-19-GAP-GATE-OK"
        status: pass
    human_judgment: false
  - id: D3
    description: "All eight 19-REVIEW findings dispositioned (open 0) and deferrals carried into the ROADMAP Phase 20 entry"
    requirement: "COLS-11"
    verification:
      - kind: command
        ref: "Task 3 automated command -> GAP-10-DISPOSITIONS-OK"
        status: pass
    human_judgment: false
duration: 10min
completed: 2026-10-13
status: complete
---

# Phase 19 Plan 10: Suite Gate, Anchors Gate and Review Dispositions Summary

**After gap closure the whole suite is green twice on ClickHouse 26.9.17.8 x client V2, the anchors invariant passes over every commit, and all eight 19-REVIEW findings are triaged (open 0) with WR-02/L-38, the VERSION/CHANGELOG obligation and IN-04 carried into the ROADMAP Phase 20 entry.**

## Performance
- Duration: 10 min (gate runs dominate)
- Tasks: 3 (no tracked-file changes in Tasks 1 and 2)
- Files modified: 2 (`19-REVIEW-DISPOSITION.md`, `ROADMAP.md`)

## Accomplishments

**Task 1 - suite gate (PHASE-19-GAP-SUITE-GREEN).** Server version `26.9.17.8` (from `NULLABLE_TUPLE_MATRIX server version=`).

| Run | Classes | Tests | Failures | Errors | Skipped |
|-----|---------|-------|----------|--------|---------|
| testUnit (cache-busted) | 45 | 556 | 0 | 0 | 2 |
| full run 1 (`test --rerun`) | 84 | 980 | 0 | 0 | 18 |
| full run 2 (`test --rerun`) | 84 | 980 | 0 | 0 | 18 |

Both full runs carry exactly the 18 named skips (11 parameterised `ClickHouseSinkTaskWithSchemaTest` cases plus 7 named). `NullableTupleMatrixLiveTest` is exactly 30 cases and `SimpleAggregateTupleRoundTripLiveTest` exactly 2. No stale or missing Phase 19 class.

**Task 2 - anchors, scope, ledger (PHASE-19-GAP-GATE-OK).** Gate line: `invariants: provenance_paths=21 anchor_commits=296 anchor_rows=59 marker_commits=343 result=PASS`. Non-.planning paths changed since the phase start match the expected set exactly (extra `[]`, missing `[]`); ledger is L-31..L-41 with 8 columns each; `Table.java`, exactly-once code, `VERSION`, `CHANGELOG.md` and the gg1 rollout doc are unchanged; `ClickHouseWriter.java` and `Column.java` are unchanged since `5fd09cb1`; no test names the nullable-tuple server setting.

**Task 3 - dispositions (GAP-10-DISPOSITIONS-OK).** Docs commit `ea20cf0a` touches only the two .planning files and deletes no ROADMAP line.

| Finding | Disposition | Source |
|---------|-------------|--------|
| CR-01 | fixed | 4e133d4e (19-07) |
| WR-01 | fixed | 88e4e854 (19-08) |
| WR-02 | deferred | Phase 20 (ROLL-02, PUB-07), ledger L-38, ROADMAP carry-forward |
| WR-03 | fixed | 448a5ba5 (19-08) |
| IN-01 | fixed | 88e4e854 (19-08) |
| IN-02 | skipped | permanent D-05 record; removal changes no behaviour |
| IN-03 | fixed | 448a5ba5 (19-08) |
| IN-04 | deferred | Phase 20 owner; trigger = clickhouse-data change or observed DESCRIBE shape |

Inserted ROADMAP block (in `### Phase 20`, before `**Plans**: TBD`): `**Carried from Phase 19**` with three bullets - WR-02 (gg2 rollout/CHANGELOG must state the limitation is closed, the WARN is gone, earlier rows are not repaired, and carry L-38 `DataException` under `errors.tolerance=none`), PUB-07 owning the `VERSION` bump and `CHANGELOG.md` entry, and IN-04 with owner and trigger.

## Task Commits
1. Tasks 1-2 - gates only, no commit (no tracked-file changes).
2. Task 3 - `ea20cf0a` `docs(19-10): disposition the eight 19-REVIEW findings and carry WR-02 and IN-04 to Phase 20 (CR-01 fixed, open 0)`

## Files Created/Modified
- `.planning/phases/19-nullable-enum-tuple-mapping/19-REVIEW-DISPOSITION.md` - eight findings triaged, `open: 0`
- `.planning/ROADMAP.md` - additive Phase 20 carry-forward block

## Decisions Made
- Deferred findings land in the ROADMAP Phase 20 entry so Phase 20 planning reads them; the published gg1 operator documents stay as released.
- IN-02 skipped rather than fixed, to keep the D-05 spike class as the permanent record.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## Known Stubs
None.

## Next Phase Readiness
19-VERIFICATION.md gap 1 (SC3 / COLS-10) and missing item 3 are ready for re-verification. Phase 20 must read its `**Carried from Phase 19**` block.

---
*Phase: 19-nullable-enum-tuple-mapping*
*Completed: 2026-10-13*

## Self-Check: PASSED

- Disposition file and ROADMAP exist; commit `ea20cf0a` is an ancestor of HEAD; commits 4e133d4e, 88e4e854, 448a5ba5 resolve.
- PHASE-19-GAP-SUITE-GREEN, PHASE-19-GAP-GATE-OK and GAP-10-DISPOSITIONS-OK were printed in this run, in that order.
