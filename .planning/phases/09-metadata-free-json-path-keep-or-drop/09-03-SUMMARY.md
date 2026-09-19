---
phase: 09-metadata-free-json-path-keep-or-drop
plan: 03
subsystem: testing
tags: [clickhouse, kafka-connect, insertFormat, json, planning-bookkeeping, merge-checklist, requirements-traceability]

# Dependency graph
requires:
  - phase: 09-01
    provides: The four re-applied guard sites, the synthetic Table, jsonInsertColumns re-attachment — the resolved diff this plan reads as JSONP-04's evidence
  - phase: 09-02
    provides: The live proof run and the TableTest.extractNullables() commit (b84b179e) this plan cites when resolving Recorded Fact 13
provides:
  - JSONP-04 discharged by reading the resolved ClickHouseWriter.java diff against v1.6.0, not by a green test
  - REQUIREMENTS.md JSONP-03 dated correction blockquote (annotate, not rewrite) and JSONP-01..04 closed in both the checkbox list and the traceability table
  - MERGE-CHECKLIST.md row 26 annotated with a Phase 9 note (status/owning phase untouched)
  - MERGE-CHECKLIST.md Recorded Fact 13 resolved in place, citing commit b84b179e
  - MERGE-CHECKLIST.md Recorded Fact 14 — the KEEP disposition, its rationale, the three operator facts, the accepted cost, and the second prefer-upstream conflict-policy departure
  - MERGE-CHECKLIST.md's closing sentence superseded by a one-line note rather than deleted
affects: [11 (SUITE-03's fork-delta record and the zero-pending publication gate both read this plan's MERGE-CHECKLIST.md entries)]

# Actuals (#2632)
actuals:
  tokens: 3378
  tasks: 3
  commits: 2
  plan_head_before: 59ca0d074c55f6444967c03b0852b148c7899ac7

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Diff-read evidence act: JSONP-04's requirement explicitly rules out a green test as sufficient — the discharge is a stated reading of `git diff v1.6.0 HEAD` with grep-verified counts, not a new test"
    - "Annotate-don't-flip bookkeeping applied to a row and a Recorded Fact this phase does not own (row 26 is Phase 7's) as well as to ones it does (Fact 13, the new Fact 14) — append only, never rewrite or flip status/owning-phase columns"

key-files:
  created: []
  modified:
    - .planning/REQUIREMENTS.md
    - .planning/MERGE-CHECKLIST.md

key-decisions:
  - "Task 1 produced no commit, exactly as the plan specifies — it is a reading, not a change. All three of its verify blocks (call-site census, diff-marker counts, git-status-clean) were run and passed before Task 2 began."
  - "Task 2 and Task 3 landed as two separate commits (601804e3, 3fc4535e) rather than merged into one — the plan's D-13 note explicitly allows either shape at Claude's discretion, and keeping REQUIREMENTS.md and MERGE-CHECKLIST.md changes in separate commits keeps each diff independently reviewable against its own verify block."
  - "Fact 14's KEEP disposition text was written to require exact-substring matches the plan's own verify block checks for ('conflict policy' as two space-separated words, not hyphenated) — phrased as '...the milestone's prefer-upstream conflict policy...' so both the prose reading and the mechanical check land on the same words."
  - "Recorded Fact 13's resolution sentence was appended to the end of its existing single-line paragraph (matching the file's established one-line-per-numbered-entry convention for Facts 9-13) rather than as a new paragraph, so the awk-range verify check and a human skimming the numbered list see one continuous resolved entry."

patterns-established: []

requirements-completed: [JSONP-01, JSONP-04]

coverage:
  - id: D1
    description: "JSONP-04 discharged: src/main holds exactly three new Table( call sites, all five-argument; the ClickHouseWriter one is the only hand-built one and carries List.of(), 0; no JSON-path method (getTable, doInsertJson, doInsertJsonV2, writeRecordsJson, cleanupExtraFields) calls a mutator on Table; the guards plus the overload branch are the only Phase 9 additions on that path"
    requirement: JSONP-04
    verification:
      - kind: other
        ref: "PLAN.md Task 1 verify block 1 (new Table( census + arity + List.of(),0 shape) — run this session, JSONP04_TABLE_SHAPE_EVIDENCE_OK"
        status: pass
      - kind: other
        ref: "PLAN.md Task 1 verify block 2 (git diff v1.6.0 HEAD marker counts) — run this session, JSONP04_DIFF_READ_EVIDENCE_OK"
        status: pass
      - kind: other
        ref: "PLAN.md Task 1 verify block 3 (git status --porcelain -- src/ build.gradle.kts clean) — run this session, READ_ONLY_RESPECTED"
        status: pass
    human_judgment: false
  - id: D2
    description: "REQUIREMENTS.md: JSONP-03's rationale corrected by a dated, cited blockquote without touching the original requirement text; JSONP-01..04 closed as checkboxes and traceability rows, with later-phase rows left Pending"
    requirement: JSONP-01
    verification:
      - kind: other
        ref: "PLAN.md Task 2 verify block 1 (correction blockquote presence + 5 required citations) — run this session, JSONP03_CORRECTION_APPENDED"
        status: pass
      - kind: other
        ref: "PLAN.md Task 2 verify block 2 (checkbox + traceability closure, scoped to the 4 IDs) — run this session, JSONP_REQUIREMENTS_CLOSED"
        status: pass
    human_judgment: false
  - id: D3
    description: "MERGE-CHECKLIST.md: row 26 annotated (status/owner untouched), Recorded Fact 13 resolved in place citing b84b179e, a new operator-confirmed Recorded Fact 14 states the KEEP disposition with its accepted cost and conflict-policy departure, and the closing sentence is superseded rather than deleted"
    requirement: JSONP-01
    verification:
      - kind: other
        ref: "PLAN.md Task 3 verify block 1 (row 26 + Fact 13 annotation) — run this session, ROW26_AND_FACT13_ANNOTATED"
        status: pass
      - kind: other
        ref: "PLAN.md Task 3 verify block 2 (Fact 14 presence, required citations, contiguous numbering, superseding note) — run this session, KEEP_DISPOSITION_RECORDED"
        status: pass
      - kind: other
        ref: "PLAN.md Task 3 verify block 3 (commit scope: no src/ files, more added than removed) — run this session, ANNOTATE_DONT_FLIP_RESPECTED"
        status: pass
    human_judgment: false

duration: ~15min
completed: 2026-09-20
status: complete
---

# Phase 9 Plan 3: Discharge JSONP-04 and Record the KEEP Disposition Summary

**Read the resolved `ClickHouseWriter.java` diff against `v1.6.0` to discharge JSONP-04 (no code, no test), then put Phase 9's decisions on the permanent record: JSONP-03's corrected rationale, all four JSONP requirements closed, `MERGE-CHECKLIST.md` row 26 annotated, Recorded Fact 13 resolved, and a new operator-confirmed Recorded Fact 14 stating the KEEP disposition with its accepted cost.**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-09-20 (this session)
- **Completed:** 2026-09-20 (this session)
- **Tasks:** 3
- **Files modified:** 2 (`.planning/REQUIREMENTS.md`, `.planning/MERGE-CHECKLIST.md`)

## Accomplishments

- Discharged JSONP-04 by reading `git diff v1.6.0 HEAD -- ClickHouseWriter.java` and stating three findings with their evidence (below) — no source file was touched, matching the requirement's own instruction that a green test is not sufficient
- Appended a dated, `javap`-cited correction blockquote after JSONP-03 in `REQUIREMENTS.md`, leaving the original (factually-wrong-rationale) requirement text byte-identical below it
- Closed JSONP-01 through JSONP-04 as both checkboxes and traceability rows, while every later-phase (`HARN`/`SUITE`/`CI`) row stayed `Pending`
- Annotated `MERGE-CHECKLIST.md` row 26 with a Phase 9 note (status `done` and owning phase `7` both left untouched) clarifying that the JSON-path portion of its description closed in Phase 9, not Phase 7
- Resolved Recorded Fact 13 in place — cites `TableTest` commit `b84b179e`, states the assertion moved 3 → 2, and confirms `shouldSkipDescribeRow`/`Table.java`/`ClickHouseHelperClient.java` are unchanged
- Added Recorded Fact 14 (`operator-confirmed`) stating D-01's KEEP call, its rationale, the three operator facts, the accepted cost (R-01 stays pinned), the second deliberate departure from the milestone's prefer-upstream conflict policy, and the costly reversibility
- Superseded `MERGE-CHECKLIST.md`'s closing sentence ("no keep-or-drop disposition is recorded anywhere") with a one-line note pointing at the new Recorded Fact 14, leaving the original sentence intact

## JSONP-04's Three Findings (Task 1 — read-only, no commit)

Read `git diff v1.6.0 HEAD -- src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` in full. The command succeeded (exit 0) and the diff is non-empty: **+109 / −10** lines against `v1.6.0`.

**Finding (a) — the only hand-built `Table` uses upstream's five-argument constructor.** `src/main` holds exactly three `new Table(` call sites:

```
src/main/.../db/ClickHouseWriter.java:1618:            return new Table(database, Utils.getMappedOrTopicTableName(topic, csc.getTopicToTableMap()), false, List.of(), 0);
src/main/.../db/helper/ClickHouseHelperClient.java:533: return new Table(database, tableName, hasDefaults, columns, numColumns);
src/main/.../db/helper/ClickHouseHelperClient.java:602: return new Table(database, tableName, hasDefaults, columns, numColumns);
```

All three pass five arguments; no two-argument form survives anywhere in `src/main`. The `ClickHouseWriter.java` site is the only one not backed by a real `DESCRIBE` response and is the only one carrying the explicit `List.of(), 0` empty-column tail. The mechanism that makes the three-site count trustworthy evidence rather than a coincidence: `Table.java`'s pre-merge two-argument constructor no longer exists — only the five-argument form is declared (`Table.java:48`) — so a surviving `new Table(database, tableName)` call would not have compiled. A count of 3, all five-arg, is therefore evidence the fork's hand-built `Table` construction was re-expressed against the new shape everywhere, not merely evidence that today's tree happens to look that way.

**Finding (b) — no JSON-path code assumes the old mutable `Table` shape.** `Table`'s `addColumn(Column)` is `private` (`Table.java:82`) and is called only once, from inside the constructor itself (`Table.java:59`, `columns.forEach(this::addColumn)`). Grepping `ClickHouseWriter.java` for `addColumn` finds zero call sites — confirming nothing on the JSON write path (`getTable`, `doInsertJson`, `doInsertJsonV2`, `writeRecordsJson`, `cleanupExtraFields`) calls a mutator on a `Table` or expects to populate one after construction; every `Table` this file ever touches is fully formed at the moment of construction. The three downstream consumers of the synthetic table's empty `getRootColumnsList()`, confirmed by line: `validateDataSchema` (`:282`, its loop becomes vacuous so the fail-fast gate passes without checking anything), `writeRecordsRowBinary` (`:1190`, its per-column loop emits zero bytes per record — this is R-01's mechanism, reproduced knowingly per D-04), and `cleanupExtraFields` (`:1405`, guarded by `shouldSkipTableMetadata()`, so never reached under the bypass). A fourth, incidental consumer exists — `buildSchemaValidationFailureMessage` (`:556`, a Phase 7 addition, unrelated to the JSON path) streams `getRootColumnsList()` looking for a `JSON`-typed column to name in an error message; against an empty list it finds none and falls back to the generic message, which is harmless but worth naming for a future consumer of `getRootColumnsList()` who must handle the empty case, exactly as CONTEXT.md's Integration Points section warns.

**Finding (c) — the guards are the only Phase 9 additions on that path.** Reading the diff hunk by hunk and separating Phase 9's additions from Phase 7's (both land in the same file, against the same `v1.6.0` base): Phase 9 added the `shouldSkipTableMetadata()` private predicate and its javadoc, the early-return guard in `start()` (between the ping-success log and the `updateMapping` call), the early-return guard in `updateMapping()` (before its `compareAndSet`), the early-return guard in `getTable()` (before the `mapping.get` lookup, returning the synthetic `Table`), the `||` disjunct added to `cleanupExtraFields`'s existing `csc.isBypassFieldCleanup()` guard, and the overload-branch in `doInsertJsonV2` (`insertColumns != null && !insertColumns.isEmpty()` selecting between the four- and five-argument `client.insert` overloads). Everything else in the +109/−10 diff — the `DYNAMIC`/`UNKNOWN` RowBinary fail-fast case, the `MAP`/`TUPLE`/`VARIANT` null-entry guards (WR-02/WR-03), `targetTableFilter`'s builder setter, the schema-validation message helpers, and two unrelated log-level/message tweaks — is confirmed Phase 7's prior work on this same file, not Phase 9's. No upstream line below any of Phase 9's four guards was modified: `getTable`'s `describeTable` fallback, `updateMapping`'s `compareAndSet` and its surrounding logic, the `suppressTableExistenceException` branch, and `startBackgroundTableSync`'s `tableRefreshInterval > 0` gate are all intact below the guards, matching plan 09-01's preserved-body invariant.

**Diff totals for Phase 11's SUITE-03 fork-delta record:** `ClickHouseWriter.java` vs `v1.6.0`: **+109 / −10** lines total (Phase 7 + Phase 9 combined, this file has no phase-scoped diff boundary in git history). Phase 9 marker counts inside that diff (from the verify block): `shouldSkipTableMetadata` appears in 5 added lines, exactly 1 added `new Table(` line, and 3 added lines mentioning `insertColumns`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Discharge JSONP-04 by reading the resolved diff — no code, no test** — no commit (read-only task, findings recorded above)
2. **Task 2: Append the JSONP-03 rationale correction and close the four requirements** — `601804e3` (docs)
3. **Task 3: Put the KEEP disposition, its operator evidence and its accepted cost on the merge record** — `3fc4535e` (docs)

## Files Created/Modified

- `.planning/REQUIREMENTS.md` — JSONP-03 dated correction blockquote appended; JSONP-01..04 checkboxes and traceability rows flipped to Complete
- `.planning/MERGE-CHECKLIST.md` — row 26 annotated with a Phase 9 note; Recorded Fact 13 resolved in place; new Recorded Fact 14 (operator-confirmed KEEP disposition); closing sentence superseded

## Decisions Made

- **Task 2 and Task 3 landed as separate commits**, not merged into D-13's single "commit 6" — the plan explicitly leaves this to discretion ("either is within D-13's discretion, but do not interleave with any source change"). Keeping `REQUIREMENTS.md` and `MERGE-CHECKLIST.md` changes in separate commits let each one be independently verified against its own task's verify block before the other began.
- **Recorded Fact 14's prose was phrased to satisfy the plan's own mechanical verify check literally** — "conflict policy" as two space-separated words (not "conflict-policy") — so the human-readable rationale and the grep-based acceptance criterion read the identical phrase.
- **No source file was touched by any task in this plan.** Task 1 is read-only by the requirement's own wording; Tasks 2 and 3 are planning-document bookkeeping only. `git status --porcelain -- src/ build.gradle.kts` was verified clean after Task 1 and both commits' diffs were confirmed to touch zero files under `src/`, `gradle/`, or `build.gradle.kts`.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 9 is complete: all four JSONP requirements (`JSONP-01..04`) are closed in `REQUIREMENTS.md`, and `MERGE-CHECKLIST.md` now carries a full record of the KEEP disposition, its rationale, its accepted cost, and the second prefer-upstream conflict-policy departure.
- Phase 11's SUITE-03 fork-delta record can cite this plan's `ClickHouseWriter.java` diff totals (+109/−10 against `v1.6.0`) and Recorded Fact 14 directly; Phase 11's zero-pending publication gate no longer needs to resolve Recorded Fact 13, which this plan closed.
- R-01's fail-fast repair is worth rescheduling at milestone close on operator fact 2 (no live pipeline takes the `Struct` route today) — Phase 4 rated the fix prohibitive on the opposite assumption. This is recorded in `PROJECT.md` → Active and `REQUIREMENTS.md` → Future Requirements as an unscheduled follow-up; this plan changes nothing about its scheduling, only supplies the fact that should inform a future re-estimate.
- No blockers for Phase 10 from this plan's work — Phase 10's own scope (`HARN-01..07`, the `ClickHouseBase` harness contract) is untouched by anything landed here.

## Self-Check: PASSED

- `.planning/REQUIREMENTS.md` — FOUND, JSONP-01..04 all `[x]` and `Complete` in traceability, correction blockquote present
- `.planning/MERGE-CHECKLIST.md` — FOUND, row 26 Phase 9 note present, Recorded Fact 13 resolved, Recorded Fact 14 present exactly once, closing sentence superseded
- Commit `601804e3` — FOUND in `git log --oneline --all`
- Commit `3fc4535e` — FOUND in `git log --oneline --all`
- All three of Task 1's verify commands re-confirmed passing this session: `JSONP04_TABLE_SHAPE_EVIDENCE_OK`, `JSONP04_DIFF_READ_EVIDENCE_OK`, `READ_ONLY_RESPECTED`
- All three of Task 2's and Task 3's verify commands re-confirmed passing this session: `JSONP03_CORRECTION_APPENDED`, `JSONP_REQUIREMENTS_CLOSED`, `ROW26_AND_FACT13_ANNOTATED`, `KEEP_DISPOSITION_RECORDED`, `ANNOTATE_DONT_FLIP_RESPECTED`
- `git rev-list --count 59ca0d07..HEAD` = 2, matching the two commits made in this plan

---
*Phase: 09-metadata-free-json-path-keep-or-drop*
*Completed: 2026-09-20*
