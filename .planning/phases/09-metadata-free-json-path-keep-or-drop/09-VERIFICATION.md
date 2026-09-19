---
phase: 09-metadata-free-json-path-keep-or-drop
verified: 2026-09-20T00:00:00Z
status: human_needed
score: 4/4 must-haves verified
covered_files:
  - .planning/MERGE-CHECKLIST.md
  - .planning/REQUIREMENTS.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-01-PLAN.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-01-SUMMARY.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-02-PLAN.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-02-SUMMARY.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-03-PLAN.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-03-SUMMARY.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-CONTEXT.md
  - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-REVIEW.md
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/TableTest.java
covered_digest: "v1:sha256:7c5f9e8c1fd734e1eb81297c1c64de4b41ba0d7e6a2bc19ef92653c5609418a4"
behavior_unverified: 0
overrides_applied: 0
human_verification:
  - test: "Decide the disposition of CR-01 (09-REVIEW.md Critical finding): ClickHouseWriter.doInsert's `csc.isAutoEvolve()` branch calls `evolveTableSchema(table, lastFields)` with no `shouldSkipTableMetadata()` guard. Confirmed still present and unguarded at ClickHouseWriter.java:210-222 on this tree. Reproduce by configuring `auto.evolve=true` together with an explicit `insertFormat=json` and sending a record with a non-null schema (SCHEMA/DEBEZIUM_CDC): `evolveTableSchema` reads the synthetic Table's empty `getRootColumnsList()`, treats every field as missing, issues `ALTER TABLE ... ADD COLUMN`, then `urgentTableUpdate` falls through to a direct `chc.describeTable(...)` call — the exact metadata query D-01's bypass exists to avoid. If the connector user lacks DESCRIBE (operator fact 3), every batch for that topic throws `IllegalStateException` and fails hard; if they have it, the connector silently reintroduces unbounded per-batch DESCRIBE/ALTER traffic that D-03's `updateMapping` guard was built specifically to eliminate."
    expected: "A human decision: either (a) accept CR-01 as a recorded, scoped-out gap and schedule a fix (e.g. guard the `auto.evolve` branch with `!shouldSkipTableMetadata()`, per the reviewer's suggested fix) as a Future Requirement/backlog item before this milestone claims the bypass is production-safe under all supported config combinations, or (b) fix it now before the phase is considered closed. Either way this needs to be a recorded decision, not silence — the same evidentiary bar D-01 itself was held to."
    why_human: "This is a design/priority trade-off (fix now vs. schedule later) about a correctness gap discovered by code review, not something a grep/test check can resolve on its own. CONTEXT.md's own Integration Points section enumerated three known downstream consumers of the synthetic Table's empty column list; CR-01 identifies a fourth (fifth counting cleanupExtraFields), undocumented one that no must-have in any of the three plans covers, so no automated verify block in this phase asserts on it either way."
gaps: []
---

# Phase 9: Metadata-free JSON Path — Keep or Drop Verification Report

**Phase Goal:** Settle the one genuinely open design question in this milestone — whether the fork's metadata-free `insertFormat=json` bypass (and with it R-01's silent data loss) is kept or retired in favour of upstream's `writeRecordsJson` — at a blocking-human checkpoint on evidence read from the merged code, then make the code and the tests say the same thing.
**Verified:** 2026-09-20
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth (ROADMAP success criterion) | Status | Evidence |
|---|---|---|---|
| 1 | A recorded keep-or-drop decision with rationale exists, made at a blocking-human checkpoint on evidence from the merged code, before the dependent implementation work | ✓ VERIFIED | `09-01-PLAN.md` Task 1 is `type="checkpoint:decision"`, gated on `git status --porcelain -- src/` printing nothing before the question is asked. `09-01-SUMMARY.md` records the developer's verbatim answer, "Confirm KEEP (expected)." `09-CONTEXT.md` D-01/D-02 records the checkpoint occurred during `/gsd-discuss-phase`, on the measured-starting-state evidence table plus three operator facts, before any implementation existed. `MERGE-CHECKLIST.md` Recorded Fact 14 (read directly on this tree) restates the decision, its rationale, the three operator facts, the accepted cost (R-01 re-introduced), and the conflict-policy departure. |
| 2 | The JSON-path tests reflect the KEEP decision: the bypass works and R-01's labelled loss test still passes | ✓ VERIFIED | Independently re-aggregated `build/test-results/test/TEST-*.xml` on this tree: 54 classes, 643 tests, 0 failures, 0 errors, 18 skipped — matches 09-02-SUMMARY's claimed totals exactly. `ExplicitJsonInsertPathLiveTest` (6 tests), `JsonInsertColumnsLiveTest` (5), `ClickHouseSinkTaskJsonInsertFormatTest` (2), `MetadataFreeJsonInsertUnitTest` (11), `TableTest` (7) each independently confirmed 0 failures/0 errors/0 skipped. The R-01 pin `explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch` and the guard-scope negative `unsetInsertFormatStillThrowsOnAbsentTable` both confirmed present in `ExplicitJsonInsertPathLiveTest`'s result XML (2 hits via grep). |
| 3 | `jsonInsertColumns` works again as a production consumer, redesigned per the corrected premise (D-06/D-07/D-08: server-side branch on the surviving 5-arg `client.insert` overload, not a client-side field filter) | ✓ VERIFIED | Read `ClickHouseWriter.java:1387-1390` directly: `List<String> insertColumns = csc.getJsonInsertColumns();` followed by a ternary selecting the 5-arg `client.insert(table.getName(), insertColumns, dataWriter, ClickHouseFormat.JSONEachRow, insertSettings)` when non-empty and the unchanged 4-arg overload when empty — matches D-06/D-08 exactly. `REQUIREMENTS.md` carries the dated correction blockquote after JSONP-03 citing the `javap` evidence, with the original (now-known-wrong) rationale text left byte-identical below it. `JsonInsertColumnsUnitTest` (Docker-free, COLS-01) and `JsonInsertColumnsLiveTest` (COLS-02/06, live) both independently confirmed green. |
| 4 | Reading the resolved diff (not merely a green test) shows no fork-only JSON-path code left standing against a `Table` shape upstream no longer produces | ✓ VERIFIED | `grep -rn "new Table(" src/main/java` on this tree returns exactly 3 call sites (`ClickHouseHelperClient.java:533`, `:602`, `ClickHouseWriter.java:1618`), all passing 5 arguments to `Table.java:48`'s sole constructor; the `ClickHouseWriter.java` site is the only hand-built one and carries the `false, List.of(), 0` tail — matches `09-03-SUMMARY.md`'s Finding (a) verbatim. `Table.addColumn` confirmed `private`, called only once from inside the constructor — no JSON-path method (`getTable`, `doInsertJsonV2`, `writeRecordsJson`, `cleanupExtraFields`) calls a mutator on a `Table` post-construction. |

**Score:** 4/4 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `src/main/java/.../sink/db/ClickHouseWriter.java` | `shouldSkipTableMetadata()` predicate + 4 guard sites + synthetic `Table` + `doInsertJsonV2` overload branch | ✓ VERIFIED | `shouldSkipTableMetadata()` declared once (`:1612`), called at exactly 4 sites (`:125` `start`, `:147` `updateMapping`, `:1400` `cleanupExtraFields`, `:1617` `getTable`); `csc.isJsonInsertFormatExplicit()` appears exactly once, inside the predicate. Guard ordering matches D-03: `start`'s guard sits between the ping-success log and `updateMapping(...)`; `updateMapping`'s guard sits above `compareAndSet(false, true)`, with the `finally` reset intact. |
| `src/test/java/.../db/MetadataFreeJsonInsertUnitTest.java` | Docker-free canary, un-parked, 2 new pins added | ✓ VERIFIED | 4 `@Test` + 2 `@ParameterizedTest`, 0 `@Disabled`. Both new pin method names present verbatim: `getTableUnderExplicitJsonReturnsSyntheticRemappedTableWithoutHelperClient`, `updateMappingUnderExplicitJsonShortCircuitsAndLeavesTheInProgressFlagClear`. Result XML: 11 tests, 0 failures/errors/skipped. |
| `src/test/java/.../ExplicitJsonInsertPathLiveTest.java`, `JsonInsertColumnsLiveTest.java`, `ClickHouseSinkTaskJsonInsertFormatTest.java` | `@Disabled` removed, contract unchanged | ✓ VERIFIED | No `@Disabled` annotation or import remains in any of the three. R-01's `PIN - NOT AN ENDORSEMENT` Javadoc and pin method name both still present in `ExplicitJsonInsertPathLiveTest`. All 13 previously-parked methods run and pass. |
| `src/test/java/.../db/mapping/TableTest.java` | `extractNullables()`'s all-columns assertion `3` → `2` (D-14) | ✓ VERIFIED | `assertEquals(2, table.getAllColumnsList().size())` present with a comment citing `DESC-01`/`HLP-02`; the two `getRootColumnsList().size()==2` assertions untouched. `TableTest` result XML: 7 tests, 0 failures. |
| `.planning/REQUIREMENTS.md` | JSONP-03 dated correction blockquote; JSONP-01..04 closed | ✓ VERIFIED | Blockquote present immediately after the JSONP-03 line, citing `javap`, `client-v2-0.9.6`, `DataStreamWriter`, dated 2026-09-20, attributed Phase 9; original JSONP-03 wording (including "no longer exists at that call site") left intact below it. JSONP-01..04 all `[x]` and `Complete` in the traceability table; later-phase (`HARN`) rows confirmed still `Pending`. |
| `.planning/MERGE-CHECKLIST.md` | Row 26 Phase 9 note; Recorded Fact 13 resolved; new Recorded Fact 14 (KEEP disposition); closing sentence superseded | ✓ VERIFIED | Row 26 carries an inline "Phase 9 note" citing commits `9d8f3d95`/`3f9ecd82`, `status` still `done`, owning phase still `7`. Recorded Fact 13's original "Owning phase: none" text intact, with an appended "Resolved in Phase 9 (commit `b84b179e`)" sentence. Recorded Fact 14 present exactly once, `operator-confirmed`, naming the rationale, three operator facts, accepted cost, conflict-policy departure, and costly reversibility. Closing sentence ("No cell or entry... states a keep-or-drop disposition...") left verbatim, followed by a "Superseded (Phase 9, 2026-09-20)" note. |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| `ClickHouseSinkConfig.isJsonInsertFormatExplicit()` | `ClickHouseWriter.shouldSkipTableMetadata()` | single predicate, no inlined duplicates | ✓ WIRED | Confirmed one call site of `isJsonInsertFormatExplicit()` in `ClickHouseWriter.java`, inside the predicate; predicate called at exactly 4 guard sites. |
| Plan 09-01's guards | `ExplicitJsonInsertPathLiveTest` JSON-02 (creates table after `sinkTask.start(props)`) | only live coverage of the `start()` guard | ✓ WIRED | `ExplicitJsonInsertPathLiveTest` confirmed 6/6 passing, 0 skipped, in the independently re-aggregated result XML. |
| Plan 09-01's overload branch | `JsonInsertColumnsLiveTest` COLS-02 (empty list) / COLS-06 (non-explicit path) | proves branch keyed on emptiness, not `insertFormat` | ✓ WIRED | `JsonInsertColumnsLiveTest` confirmed 5/5 passing, 0 skipped. |
| Plan 09-01's `getTable` guard | JSONP-04's diff-read evidence | third `new Table(` call site in `src/main` | ✓ WIRED | Confirmed directly: 3 call sites, all 5-arg. |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| Full-suite aggregate matches SUMMARY claims | Re-aggregated `build/test-results/test/TEST-*.xml` (not re-run; tree unchanged since the recorded run — confirmed via `find src -name '*.java' -newer build/test-results/test` returning empty) | 54 classes, 643 tests, 0 failures, 0 errors, 18 skipped | ✓ PASS |
| Five phase-affected classes each individually green | Per-class XML re-aggregation | `ExplicitJsonInsertPathLiveTest` 6/0/0/0, `JsonInsertColumnsLiveTest` 5/0/0/0, `ClickHouseSinkTaskJsonInsertFormatTest` 2/0/0/0, `MetadataFreeJsonInsertUnitTest` 11/0/0/0, `TableTest` 7/0/0/0 | ✓ PASS |
| `new Table(` call-site census (JSONP-04 evidence) | `grep -rn "new Table(" src/main/java` | 3 sites, all 5-arg | ✓ PASS |
| No debt markers in phase-touched files | `grep -n "TBD\|FIXME\|XXX"` across the 6 modified source files | no matches | ✓ PASS |
| `git status --porcelain -- src/` clean | direct check | clean (only unrelated `.planning/state.json`, `.planning/milestone.lock`) | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| JSONP-01 | 09-01, 09-03 | Keep-or-drop decision at blocking-human checkpoint, on evidence, recorded with rationale | ✓ SATISFIED | Checkpoint task + developer answer + `MERGE-CHECKLIST.md` Fact 14, all confirmed above |
| JSONP-02 | 09-01, 09-02 | Tests reflect the decision; bypass works; R-01 pin still passes | ✓ SATISFIED | Live suite green, R-01 pin executed and passed |
| JSONP-03 | 09-01, 09-02, 09-03 | `jsonInsertColumns` works again; premise correction recorded | ✓ SATISFIED | Overload branch confirmed in code; correction blockquote confirmed in `REQUIREMENTS.md` |
| JSONP-04 | 09-03 | No fork-only JSON-path code against old `Table` shape; verified by diff read | ✓ SATISFIED | Direct 3-site/5-arg census confirms the SUMMARY's diff-read findings |

No orphaned requirements: `REQUIREMENTS.md`'s Phase 9 traceability rows (JSONP-01..04) match exactly the requirement IDs declared across the three plans' frontmatter.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---|---|---|---|
| `ClickHouseWriter.java` | 210-222 | `csc.isAutoEvolve()` branch calls `evolveTableSchema(table, lastFields)` with no `shouldSkipTableMetadata()` guard | 🛑 Critical (carried from `09-REVIEW.md` CR-01, confirmed still present on this tree, unaddressed by any of the three plans' must-haves) | Under `auto.evolve=true` + explicit `insertFormat=json` + a schema'd (`SCHEMA`/`DEBEZIUM_CDC`) record, `evolveTableSchema` reads the synthetic `Table`'s empty column list, treats every field as missing, issues `ALTER TABLE ... ADD COLUMN` for each, then `urgentTableUpdate` falls through to a direct `chc.describeTable(...)` call — the exact query D-01's bypass exists to avoid. Either throws `IllegalStateException` on every batch (if the connector user lacks DESCRIBE, per operator fact 3) or silently reintroduces unbounded per-batch DESCRIBE traffic (if they have it) — the traffic D-03's `updateMapping` guard specifically exists to eliminate. See Human Verification below. |
| `ExplicitJsonInsertPathLiveTest.java:37`, `JsonInsertColumnsLiveTest.java:35`, `ClickHouseSinkTaskJsonInsertFormatTest.java:25` | class Javadoc | Stale "Disabled pending the JSONP-01 keep/drop decision..." paragraph left in all three re-enabled classes (carried from `09-REVIEW.md` WR-01, confirmed still present) | ⚠️ Warning | Doc-only, zero functional risk — the next reader has to cross-reference `09-CONTEXT.md` to learn these classes are in fact live; `MetadataFreeJsonInsertUnitTest`'s own Javadoc was correctly rewritten in this phase (Task 2 Step F of 09-01-PLAN), establishing the pattern that was not applied to the other three files. Not a gate-blocking finding; does not affect any of the phase's four ROADMAP success criteria, which are about decision/test/code state, not Javadoc accuracy. |

Both findings were already surfaced in the phase's own committed code review (`09-REVIEW.md`, `status: issues_found`) and independently re-confirmed against the current tree by this verification rather than taken on the review's word.

## Deferred Items

None — no gap identified here is explicitly addressed by a later phase's stated goal or success criteria. CR-01 is not named in Phase 10's (`HARN-01..07`, test harness) or Phase 11's (`SUITE`/`CI`, publication) declared scope in `ROADMAP.md`; `09-CONTEXT.md`'s own "Out of this phase" list names the harness contract, publication/CI, and CR-01 *the security script* (`.planning/scripts/verify-merge-invariants.sh`, a same-ID-but-different-finding unrelated to `ClickHouseWriter.doInsert`) — none of that list covers the `auto.evolve` gap. It is an unrecorded, unscheduled finding, not a deferred one.

## Human Verification Required

### 1. CR-01 disposition: `auto.evolve` + metadata-free bypass interaction

**Test:** Configure `auto.evolve=true` together with an explicit `insertFormat=json`, and send a batch whose records carry a non-null schema (`SCHEMA`/`DEBEZIUM_CDC` route) to a table whose actual columns differ from the record's fields.

**Expected:** Either (a) the connector fails hard with `IllegalStateException` on every batch for that topic (if the connector user lacks `DESCRIBE`, per operator fact 3 — defeating the entire reason this phase kept the bypass), or (b) the connector silently issues `ALTER TABLE`/`DESCRIBE` traffic on every batch (if the user does have `DESCRIBE`) — the exact per-batch metadata traffic D-03's `updateMapping` guard was built to eliminate. Neither outcome matches "the bypass works" as ROADMAP criterion 2 and `MERGE-CHECKLIST.md` Recorded Fact 14 assert.

**Why human:** This is a scope/priority call — fix the `auto.evolve` branch now (the reviewer's suggested one-line guard) or record it as an accepted, scheduled gap — not something a grep or automated test check in this phase's own must-haves can resolve, because no plan's must-haves named this fifth consumer of the synthetic `Table`'s empty column list. `09-CONTEXT.md`'s own Integration Points section enumerated exactly three known consumers (`validateDataSchema`, `doInsertRawBinaryV2`'s write loop/R-01, `cleanupExtraFields`); this is a fourth, previously unidentified one, raised by code review and independently reconfirmed present on this tree during verification.

## Gaps Summary

No must-have from the ROADMAP's four success criteria, nor from any of the three plans' frontmatter `must_haves`, failed verification — all four criteria are independently confirmed true against the current tree, and the previously-recorded code review's Critical/Warning findings were independently re-confirmed still present (not resolved, not newly introduced by this verification). The phase's own declared scope (four guard sites: `start`, `updateMapping`, `getTable`, `cleanupExtraFields`) is fully and correctly implemented and tested. CR-01 sits outside that declared scope but bears directly on the phase's headline claim that "the bypass works" (ROADMAP criterion 2) under a real, supported configuration combination (`auto.evolve=true`), so it is escalated as a human-verification item rather than silently absorbed into a `passed` verdict or invented as a gap the phase's own must-haves never claimed to cover.

---

_Verified: 2026-09-20_
_Verifier: Claude (gsd-verifier)_
