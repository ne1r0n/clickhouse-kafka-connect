---
phase: 29-release-publication
plan: "02"
subsystem: release
tags: [release, rollout-note-inputs, backlog-999.4, describe-table, dedup-token, in-flight-retry, suspects-query, clickhouse-26.9]

requires:
  - phase: 29-release-publication
    provides: "plan 29-01: the bump commit, worktrees /tmp/gsd29/wt-gg3 and wt-tip, the probe class, the evidence file with E-01..E-09"
  - phase: 26-size-named-tuple-elements-in-discovery
    provides: "the fix whose bytes, WARNs and in-flight consequences this plan measures (DESC-01..DESC-09)"
provides:
  - "29-COMMAND-EVIDENCE.md E-10..E-15 with the lines D994-DIFFERING= (25 keys) and SIZEQ-OVERFLAG=s4_o01,x08"
  - "the gg3-versus-gg4 Column-tree diff over the 47 catalog tables, the WARN counts on both trees and one captured gg3 WARN line"
  - "one live insert per shape on both trees, with the r01 shifted row (size 1667391751, v hex 78797A)"
  - "the four in-flight retry outcomes (rollback and upgrade, with and without a deduplication window)"
  - "the suspects query proven on the catalog and on look-alikes, and the doc-form WARN, alert-rule and config greps proven on scratch directories"
affects: [29-03, 29-04, 29-05, 29-07]

actuals:
  tokens: 6757
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Same scratch probe class run unchanged in the gg3 and gg4 worktrees, output compared as PROBE- lines"
    - "A row landed by SQL under the connector's insert_deduplication_token to measure in-flight retries per direction"

key-files:
  created: []
  modified:
    - .planning/phases/29-release-publication/29-COMMAND-EVIDENCE.md

key-decisions:
  - "The note must state the over-flags of the gg3 suspects query as s4_o01 and x08 (a JSON column whose typed path holds a size-named Tuple element), not only the Array(Map(...)) one the gg3 note names"
  - "The note must say a failed gg3 insert of an affected shape fails with server code 32 or 33, not a per-shape code: this server printed 33 for five of the six shapes and 32 for n03"

requirements-completed: [ROLL-04]

plan_head_before: 46b2cf15625462e59e29a2d4e5f6360eb9b1ecdb
plan_head_after: c82c64c7293c6b4c4bcfccaeddc18159c746f6a8
duration: 10min
completed: 2026-10-23
status: complete

coverage:
  - id: D1
    description: "The Column trees of the 47 catalog tables differ between gg3 and gg4 for exactly the 25 affected keys and for no look-alike, over-flag or JSON shape; gg3 logs the unpaired WARN for each, gg4 logs none"
    requirement: ROLL-04
    verification:
      - kind: other
        ref: "python3 -I /tmp/gsd29-scripts/verify-task1.py (printed D994-TREES-OK)"
        status: pass
    human_judgment: false
  - id: D2
    description: "One insert per shape: six affected shapes fail with code 32 or 33 and store 0 rows on gg3, r01 stores a shifted row, controls store; all ten store the record on gg4"
    requirement: ROLL-04
    verification:
      - kind: other
        ref: "python3 -I /tmp/gsd29-scripts/verify-task1.py (printed D994-TREES-OK)"
        status: pass
    human_judgment: false
  - id: D3
    description: "In-flight retries per direction: gg3 retry fails with code 33 on the affected shape and is dropped on the look-alike; gg4 retry of the shifted r01 row is dropped with a window and stored twice without one"
    requirement: ROLL-04
    verification:
      - kind: other
        ref: "python3 -I /tmp/gsd29-scripts/verify-task2.py (printed D994-INFLIGHT-OK)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The gg3 suspects query lists the 25 differing tables plus the over-flags s4_o01 and x08 and nothing on the look-alike database; the four doc-form greps print their target on a scratch directory that holds it and nothing on one that does not"
    requirement: ROLL-04
    verification:
      - kind: other
        ref: "python3 -I /tmp/gsd29-scripts/verify-task3a.py and verify-task3b.py, plus git diff --quiet P29_BASE HEAD over src docs CHANGELOG.md build.gradle.kts gradle .github (printed D994-QUERIES-AND-GREPS-OK)"
        status: pass
    human_judgment: false
---

# Phase 29 Plan 02: Rollout inputs for backlog 999.4 Summary

**Measured on one ClickHouse 26.9.19.9 server with client V2, the size-named-element fix changes the Column tree of exactly 25 of 47 catalog tables; gg3 fails inserts into six affected shapes with code 32 or 33 and stores a shifted row for r01, gg3 retries of landed gg4 rows fail on the bytes before the token decides, and the gg4 retry of the shifted row follows the table's deduplication window.**

## Performance

- **Duration:** 10 min (17:53Z to 18:03Z)
- **Tasks:** 3 (1 tracer, 2 auto)
- **Files modified:** 1 (`29-COMMAND-EVIDENCE.md`); no file outside `.planning/` changed

## Accomplishments

- `D994-DIFFERING=n01,n02,n03,n04,r01,s4_a01..s4_a13,x01..x07` (25 keys): the Column trees of the gg3 tag and the gg4 tip differ for exactly these and are identical for the 19 `s4_u` look-alikes, `s4_o01`, `n05` and `x08`. In every differing table gg4 holds the size-named element nodes that gg3 dropped and gg3 holds no node that gg4 lacks (E-10).
- WARN counts: gg3 logs one unpaired-element WARN for each of the 25 differing tables and none for the other 22; gg4 logs neither the unpaired nor the unreadable-parent WARN for any of the 47 tables, x08 included (0 and 0 on both trees). One real gg3 WARN line is recorded (E-10).
- Inserts (E-11): on gg3 `s4_a01`, `s4_a04`, `s4_a08`, `s4_a13` and `x01` fail with code 33 and `n03` with code 32, all with 0 rows; `r01` stores a row that is not the record (`size` 1667391751, `v` hex `78797A`, because gg3 writes only the string and the server reads the size from its first four bytes); controls `s4_u01`, `s4_u03`, `s4_u05` store their row. On gg4 all ten store a row equal to the record.
- In-flight retries (E-12): rollback, the gg3 retry of a range whose full row had landed under token `rb_a01-0-0-0` fails with code 33 and leaves the one row; the look-alike `rb_u01` retry is dropped with one row. Upgrade, with the gg3 `r01` row landed under `up_r01_w-0-0-0`, the gg4 retry is dropped (shifted row stays, one row) on a table with `non_replicated_deduplication_window = 100` and stored as a second row (two rows) on `up_r01_nw`, which has none.
- Suspects query (E-13): the gg3 note's query, byte for byte, lists 27 of the 47 catalog tables: the 25 differing and two over-flags, `SIZEQ-OVERFLAG=s4_o01,x08`; it lists nothing on a database holding only `s4_u01..s4_u19` and `n05`.
- Greps (E-14, E-15): `have no matching DESCRIBE sub-column` and `Could not read the declared type` print their line on the scratch log that holds them and nothing (exit 1) on the one that does not; the alert-rule grep lists `through reference chain`, `No serializer found` and `NullPointerException` once each, nothing on the unrelated directory and does not list the `\s`-escaped rule; the config grep lists the `=1` and `=true` files and nothing on the unrelated directory.

## Task Commits

1. **Task 1: Column trees and inserts, gg3 versus gg4** - `e2687c2f` (docs, E-10 and E-11)
2. **Task 2: in-flight retries in both directions** - `c2c65886` (docs, E-12)
3. **Task 3: suspects query and greps** - `c82c64c7` (docs, E-13..E-15)

**Plan metadata:** the SUMMARY commit that follows this file.

## Files Created/Modified

- `.planning/phases/29-release-publication/29-COMMAND-EVIDENCE.md` - E-10..E-15 appended (E-01..E-09 untouched)
- Scratch only, not committed: `/tmp/gsd29/probe/Phase29RolloutProbe.java` (methods `sizeDescribe`, `sizeInsert`, `sizeRetryRollback`, `sizeRetryUpgrade`, `sizeQuery`), `/tmp/gsd29/probe/catalog.tsv`, `/tmp/gsd29/probe/r01-gg3-row.txt`, `/tmp/gsd29/probe/sql/size-suspects.sql`, `/tmp/gsd29-scripts/catalog-tsv.py`, `run-probe.sh`, `verify-task1.py`, `verify-task2.py`, `verify-task3a.py`, `verify-task3b.py`, the XML copies `d994-gg3.xml`, `d994-gg4.xml`, `d994-retry-gg3.xml`, `d994-retry-gg4.xml`, `d994-query.xml` under `/tmp/gsd29/exec`, and the scratch directories `/tmp/gsd29/logs`, `logs-empty`, `alerts`, `alerts-empty`, `conncfg`, `conncfg-empty`. The worktrees `wt-gg3` and `wt-tip` (now detached at `46b2cf15`) and the probe are kept for plan 29-03.

## Decisions Made

- The plan's verify commands were saved as script files and run unchanged in logic (`verify-task1.py`, `verify-task2.py`, `verify-task3a.py`, `verify-task3b.py`) instead of being pasted inline; the plan-level `git diff --quiet` check was run inline.
- The note should list both over-flags, because the catalog holds a second one (`x08`) that the gg3 note never named; the explanation is measured: a JSON column's typed paths are dropped by design, so the Column tree is identical on both trees and neither logs the WARN.

## Deviations from Plan

None - plan executed exactly as written. Two measured details differ from the wording of earlier documents and are recorded in the evidence rather than worked around, both within what the verify commands allow:

- **Server code per shape.** The gg3 note names code 32 for `Tuple(size UInt16, v String)`, `Array(Tuple(size UInt16, v String))` and `Array(Tuple(size1 UInt16, v String))` and code 33 for `Array(Tuple(v String, size1 UInt16))`. With this plan's records the same server printed 33 for all four and for `x01`, and 32 for `n03`. The plan and its verify command accept 32 or 33; the note for gg4 must not assign a code per shape (E-11).
- **Over-flags.** The gg3 note names one over-flag; on the 47-table catalog the query has two (`s4_o01`, `x08`). The plan allows over-flags when written on the `SIZEQ-OVERFLAG=` line.

No measurement contradicted research R4 (the rollback failure is a parse failure before the token decides; the upgrade qualifier for the shifted-row shape holds), so no stop or checkpoint was needed.

**Total deviations:** 0 auto-fixed.

## Issues Encountered

None. The OpenJ9 crash did not occur on any of the six Gradle runs; no javacore or core files were produced. Each run started its own container (server `26.9.19.9`, image `sha256:bdb5c911a23d...` created 2026-10-14T18:05:21Z). The captured WARN message prints the declared type with line breaks as the two characters `\n`; the probe's doubled backslash was turned back before the line went into the scratch log.

## Known Stubs

None. No source file was created or modified.

## Threat Flags

None. Fixture DDL, inserts and landed rows ran only inside the Testcontainers ClickHouse of each Gradle run (T-29-06); the in-flight outcomes are measured, not taken from Phase 26 E-11 (T-29-07); the suspects query is one read-only SELECT and the greps only read (T-29-08); records are constants and the captured WARN carries element paths, column, table and type only (T-29-09). Nothing was pushed, tagged, merged or commented on.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 29-03 can use `/tmp/gsd29/wt-gg3`, `/tmp/gsd29/wt-tip` (detached at `46b2cf15`, `src/` as at the bump commit) and the probe class with its new helper methods. Gradle daemons of the two worktrees were stopped.
- Plan 29-04 quotes E-10 (`D994-DIFFERING=`), E-13 (`SIZEQ-OVERFLAG=`, the one `sql` block), E-14 and E-15 (the four doc-form `sh` blocks) and the E-12 retry wording per direction; every entry still reads `Matches text: pending (29-04)`.
- Local state outside this plan's scope remains dirty and untouched: `gradlew.bat`, `.planning/state.json`, `.planning/milestone.lock`.

## Self-Check: PASSED

Files verified on disk: `.planning/phases/29-release-publication/29-COMMAND-EVIDENCE.md` (E-10..E-15 present), `/tmp/gsd29/probe/catalog.tsv` (47 rows), `/tmp/gsd29/probe/r01-gg3-row.txt`, the five XML copies. Commits verified as ancestors of HEAD: `e2687c2f`, `c2c65886`, `c82c64c7`. Verify commands printed D994-TREES-OK, D994-INFLIGHT-OK and D994-QUERIES-AND-GREPS-OK; `git diff --quiet P29_BASE HEAD -- src docs CHANGELOG.md build.gradle.kts gradle .github` exits 0.

---
*Phase: 29-release-publication*
*Completed: 2026-10-23*
