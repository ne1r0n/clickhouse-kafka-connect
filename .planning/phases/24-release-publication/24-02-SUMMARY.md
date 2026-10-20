---
phase: 24-release-publication
plan: "02"
subsystem: release
tags: [release, rollout-note, evidence, in-flight-bytes, rollback, detection-queries, nullable-tuple, escaped-names, size-element]

requires:
  - phase: 24-release-publication
    provides: plan 24-01 release identity (VERSION v1.7.0_gg3), scratch probe and worktrees, evidence entries E-01..E-05
provides:
  - "E-10 and E-11: gg2-versus-gg3 Column-tree diff (14 differing tables) and one live insert per shape, D-09"
  - "E-12: gg2 retry of a range the differing shape landed fails with code 33 and leaves one row; plain-name control is dropped"
  - "E-13: shape query (narrowed to backslash followed by n) that lists exactly the 14 differing tables and nothing on the look-alike database"
  - "E-14: the gg2 item-1 KeeperMap query proven on a state table created by the gg3 connector's own KeeperStateProvider"
  - "E-15: size-element WARN oracle (13 tables), suspects query (13 plus one known over-flag), insert codes 32/33, null code 36"
  - "E-16: per-character Column names, natural versus escaped field outcomes on both trees, escaped-name query"
affects: [24-03, 24-04, 24-05, 24-06, 24-07]

actuals:
  tokens: 8656
  tasks: 3
  commits: 3
plan_head_before: 719c3ef114531a2fc6937c932e8a08ab34ee29ae
plan_head_after: b03a6f26a1597ef2d3bfe4cb00c80c72363d7488
commits: 3

tech-stack:
  added: []
  patterns:
    - "Doc-form detection queries are proven against an oracle and against a look-alike-only database before the note may quote them"
    - "A supplementary probe outside the plan's fixture set decides a predicate when the plan's wording would over-flag"

key-files:
  created: []
  modified:
    - .planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md

key-decisions:
  - "D-09 verdict: the shape that encodes differently is a Nullable(Tuple)-bearing root with a backslash-n or line-break element name (14 fixture tables); every other shape is byte-identical on both releases"
  - "D-15: the clean stop before a rollback is shape-limited and measured: a gg2 retry of such a range fails with code 33 instead of being dropped, so the KeeperMap in-flight query (E-14) plus the shape query (E-13) are the operator checks"
  - "The shape query tests for a backslash directly followed by n, not for any backslash, because the broad form over-flags single-backslash, trailing-backslash and tab names whose trees are identical"

requirements-completed: [ROLL-03]

coverage:
  - id: D1
    description: "Column trees of the gg2 and gg3 classes over 44 fixture tables on one server: 14 differ, all Nullable(Tuple)-bearing roots with a backslash-n or line-break element name; D09-DIFFERING recorded"
    requirement: ROLL-03
    verification:
      - kind: other
        ref: "plan 24-02 Task 1 verify command D09-OK (E-10)"
        status: pass
    human_judgment: false
  - id: D2
    description: "One live insert per shape on V2: gg2 fails Nullable(Tuple), Array(Nullable(Tuple)) and Map(String, Nullable(Tuple)) with a backslash-n name and Nullable(Tuple) with a line-break name (code 33, 0 rows); gg3 stores all seven tables"
    requirement: ROLL-03
    verification:
      - kind: other
        ref: "plan 24-02 Task 1 verify command D09-OK (E-11)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Rollback retry, shape query and KeeperMap self-check measured end to end (E-12, E-13, E-14)"
    requirement: ROLL-03
    verification:
      - kind: other
        ref: "plan 24-02 Task 2 verify command D15-OK"
        status: pass
    human_judgment: true
    rationale: "The shape query's third predicate was narrowed from 'a backslash' to 'a backslash followed by n' after a supplementary run showed the broad form over-flags; a reviewer should accept that wording before plan 24-03 quotes it"
  - id: D4
    description: "Size-element and escaped-name detection queries proven against their oracles and look-alikes on both releases, with the per-character Column-name table (E-15, E-16)"
    requirement: ROLL-03
    verification:
      - kind: other
        ref: "plan 24-02 Task 3 verify command QUERIES-OK (E-15, E-16)"
        status: pass
    human_judgment: false

duration: 16min
completed: 2026-10-20
status: complete
---

# Phase 24 Plan 02: In-flight bytes, rollback retry and detection queries Summary

**Measured on ClickHouse 26.9.19.9, client V2, on the real v1.7.0_gg2 and gg3 class trees: 14 Nullable(Tuple)-bearing fixture tables encode differently, a gg2 retry of such a range fails with code 33, and four read-only detection queries each list exactly their oracle and nothing on a look-alike database**

## Performance

- **Duration:** 16 min
- **Started:** 2026-10-20T16:25:00Z (approximate; first tool call)
- **Completed:** 2026-10-20T16:42:00Z
- **Tasks:** 3
- **Files modified:** 1 (`24-COMMAND-EVIDENCE.md`, +321 lines); nothing outside `.planning/` changed

## Accomplishments

- **D-09 (E-10, E-11).** The real `describeTable` of tag `v1.7.0_gg2` and of the gg3 tip gave identical trees for 30 of 44 fixture tables and differing trees for exactly 14: the `bsn` and `lf` styles of r02, r03, r05, r07, r09, r10 and r12. In each, gg2 has `isNullable = true` where gg3 has `false`. gg2 logged 24 fallback WARNs (14 on the differing tables, 10 on identical ones), gg3 none. On a live insert gg2 fails the four inserted `Nullable(Tuple)`-bearing shapes with code 33 and 0 rows, and gg3 stores all seven tables.
- **D-15 (E-12, E-13, E-14).** With the correct row landed under token `<table>-0-0-0`, a gg2 retry of that range for the differing shape fails with code 33 and leaves one row; the plain-name control is dropped by the token with one row. The shape query returns exactly the 14 differing tables on the fixture database and nothing on the look-alike database. The gg2 item-1 KeeperMap query, on a state table created by the gg3 `KeeperStateProvider`, returns `orders-1` and then nothing.
- **D-01 / D-12 (E-15).** The unpaired-element WARN fires on exactly the 13 affected `size`, `size0` and Array-enclosed `size<digits>` tables. The suspects query returns those 13 plus the one known over-flag `s4_o01` and none of the 19 look-alikes. Affected inserts fail with code 32 (a01, a04, a08) or 33 (a13), 0 rows, identically on both releases; a Tuple element named `null` is rejected with code 36.
- **D-05 / D-13 (E-16).** For root names with a backslash, a backslash-n, a line break, a slash or a tab, a record using the natural name stores the column DEFAULT with no error and a record using the connector's Column name stores the value, identically on both trees; a name with a double quote leaves the table unmapped. The escaped-name query lists exactly the ten fixtures and none of the look-alikes.

### Results to carry into plan 24-03

| Measurement | Value |
|-------------|-------|
| `D09-DIFFERING` | `d09_r02_bsn, d09_r02_lf, d09_r03_*, d09_r05_*, d09_r07_*, d09_r09_*, d09_r10_*, d09_r12_*` (14 tables, `_bsn` and `_lf` each) |
| gg2 insert, differing shapes | code 33, 0 rows (r02 bsn and lf, r05 bsn, r07 bsn) |
| gg2 retry of a landed range | differing shape: code 33, 1 row; plain control: dropped, 1 row |
| `SHAPEQ-OVERFLAG` | empty |
| KeeperMap self-check | `orders-1`, then empty |
| Size WARN oracle / query | 13 tables / the same 13 plus `s4_o01` |
| Escaped-name query | `e5_e01` .. `e5_e10` |
| Server | `26.9.19.9`, client V2 |

## Task Commits

1. **Task 1: tree diff and insert matrix (tracer)** - `cb6cef77` (docs, E-10 and E-11). The tracer verify was re-run end to end after the commit (`D09-OK`); expansion followed.
2. **Task 2: rollback retry, shape query, KeeperMap self-check** - `aba3cbd0` (docs, E-12, E-13, E-14; `D15-OK`)
3. **Task 3: known-defect detection queries** - `b03a6f26` (docs, E-15 and E-16; the Python part of the verify and the tail `QUERIES-TAIL-OK` printed after the worktrees were removed)

**Plan metadata:** the commit of this SUMMARY (docs: complete plan)

## Files Created/Modified

- `.planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md` - E-10..E-16 (doc-form SQL, substitution maps, measured outputs, `D09-DIFFERING=`, `SHAPEQ-OVERFLAG=`)
- Scratch, not committed: `/tmp/gsd24/probe/Phase24RolloutProbe.java` (methods `describeDump`, `insertMatrix`, `rollbackRetry`, `shapeQuery`, `keeperSelfCheck`, `sizeQuery`, `escapeNames`, plus the supplementary `shapeExtra`), the four doc-form SQL files and the broad variant under `/tmp/gsd24/probe/sql/`, XML copies and `.lines` extracts under `/tmp/gsd24/exec/`

## Decisions Made

- The shape query keeps a backslash-followed-by-`n` test (see Deviation 1). The broad form is kept in `/tmp/gsd24/probe/sql/escaped-nullable-tuple-broad.sql` and its extra hits are recorded in E-13.
- E-12 simulates the row gg3 landed by plain SQL under the connector's token, as the plan prescribes; the gg3 connector did not write it in that measurement, and E-12 says so.
- Each probe method uses its own database suffix (`_dd`, `_im`, `_rr`, `_sq`, `_sf`, `_e5` and so on) so two methods in one Gradle run never see each other's tables.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug in the specified query] The shape query's backslash predicate over-flags**
- **Found during:** Task 2, a supplementary run (`shapeExtra`) added to check other special characters
- **Issue:** The plan describes the query as listing a column whose type text holds `Nullable(Tuple(` and a backtick-quoted name holding a backslash. On `Nullable(Tuple(<name> Int32, d String))` with a single backslash, a trailing backslash, a tab or a literal backslash-t, gg2 and gg3 give identical trees and both store the record, yet that form lists them. The trees differ only where the declared type text holds a backslash directly followed by `n`: gg2's `normalise` replaces every such pair by a space.
- **Fix:** The third predicate is `` `[^`]*\\\\n[^`]*` `` (backslash then `n`). On the plan's fixture set the result is unchanged (exactly the 14 tables, `SHAPEQ-OVERFLAG=` empty), the look-alike database stays empty, and on the extra names only `x_bbn` (two backslashes and `n`, which does differ) is listed. E-13 records both forms.
- **Files modified:** `.planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md` (evidence only)
- **Verification:** `D15-OK`
- **Committed in:** `aba3cbd0`

---

**Total deviations:** 1 auto-fixed (Rule 1 query accuracy). **Impact:** plan 24-03 must quote the narrowed query from E-13 and word it as "a backslash followed by `n`", not "a backslash".

### Findings that differ from, or add to, research and the plan text

- r12 `Tuple(n Int32, y Nullable(Tuple(c Int32)))` differs too (only the nested `y.c` node flips). The plan allowed it; research R3.2 did not list it. gg2 logged 24 fallback WARNs here against 23 in research (research used a different shape set). Neither is a contradiction.
- The four differing roots r03, r09, r10 and r12 were not inserted live (the plan inserts r02, r05, r07 only); their insert outcome is by analogy and E-11 says so.
- A `Nullable(Tuple)` with a slash in an element name fails with code 33 on both releases (fallback WARN on both, supplementary `x_sl`). That is not a rollback difference, but plan 24-03 should keep it with the 999.5 paragraph.
- For an element name (not a root name) the natural field name makes the put fail rather than store a DEFAULT (`e5_e09`, both trees); the error text was not captured.
- The gg2 oracle for the size WARN is empty (the WARN is new in gg3) while gg2 inserts fail the same way, so the size defect predates the WARN.

No measurement contradicted a locked decision (D-12, D-13, D-15), so no `checkpoint:decision` was raised.

## Issues Encountered

- The shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal `null`; every Gradle call used `env -u TESTCONTAINERS_HOST_OVERRIDE`. No OpenJ9 crash occurred in any run.
- The background-task completion notice fired for the wrapper shell before the inner Gradle runs finished; the executor waited on flag files.

## Known Stubs

None.

## Threat Flags

None. Only an evidence file changed. T-24-05: every fixture, DDL and insert ran inside the Testcontainers ClickHouse of that Gradle run. T-24-06: each verify encodes the research answer and every measurement matched it. T-24-07: the four doc-form queries are single read-only `SELECT` statements over `system.columns` or the state table.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 24-03 can quote E-10 to E-16 byte for byte, including the narrowed shape query (E-13) and the five `sql` blocks (E-13 to E-16 plus the dedup-token INSERT in E-12, which is a measurement step and not an operator query).
- `/tmp/gsd24/probe` is kept for any re-proof in plan 24-05; the two worktrees `wt-gg2` and `wt-tip` were removed as the plan requires.
- No blocker. Plan 24-03 must word the jar delta from E-04 (eleven changed entries), as plan 24-01 noted.

## Self-Check: PASSED

- Files: `24-COMMAND-EVIDENCE.md` exists and holds `### E-10:` to `### E-16:`, one `D09-DIFFERING=` line and one `SHAPEQ-OVERFLAG=` line.
- Commits `cb6cef77`, `aba3cbd0`, `b03a6f26` are ancestors of HEAD; `git rev-list --count 719c3ef1..HEAD` = 3 before this SUMMARY; `git diff --name-only 719c3ef1 HEAD` lists only the evidence file.
- Acceptance: `D09-OK`, `D15-OK`, the Python part of the Task 3 verify and `QUERIES-TAIL-OK` (no worktree left, `src/main`, `src/testFixtures`, `docs`, `CHANGELOG.md` unchanged since `P24_BASE`) printed.

---
*Phase: 24-release-publication*
*Completed: 2026-10-20*
