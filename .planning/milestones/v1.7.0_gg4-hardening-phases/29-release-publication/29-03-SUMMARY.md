---
phase: 29-release-publication
plan: "03"
subsystem: release
tags: [release, rollout-note-inputs, backlog-999.5, raw-identifier-names, enum-labels, in-flight-retry, detection-queries, keepermap, roll-05, clickhouse-26.9]

requires:
  - phase: 29-release-publication
    provides: "plans 29-01 and 29-02: the bump commit, worktrees /tmp/gsd29/wt-gg3 and wt-tip, the probe class and its evidence format, E-01..E-15"
  - phase: 28-raw-identifier-names-in-discovery
    provides: "the fix whose name, label and in-flight consequences this plan measures (NAME-01..NAME-06, ledger L-58..L-64)"
provides:
  - "29-COMMAND-EVIDENCE.md E-20..E-25 with the lines D995-DIFFERING= (16 tables), LABEL-DIFFERING= (9 entries), NAMEQ-OVERFLAG=n5_bt_tuple and an empty LABELQ-OVERFLAG="
  - "the gg3-versus-gg4 name tree diff over 36 tables, the raw-label puts for twelve entries on both trees, and the six in-flight retry outcomes for the backslash entry"
  - "the gg3 name query unchanged, a companion query for the three tables it misses, and an enum-label query, each proven on a fixture and a look-alike database"
  - "the live DESCRIBE forms of backspace, form feed, NUL and BEL in an Enum8 label and in a Tuple element name"
  - "the KeeperMap state-table query and both gg1 DELETE examples run on a scratch state table created by the gg4 KeeperStateProvider, with a prefixed key"
  - "docs/ROLLOUT-v1.7.0_gg1.md, gg2.md and gg3.md corrected for the enableDbTopicSplit=true key rule (ROLL-05)"
affects: [29-04, 29-05, 29-07]

actuals:
  tokens: 10572
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Same scratch probe class run unchanged in the gg3 and gg4 worktrees, output compared as PROBE- lines"
    - "Doc-form SQL copied byte for byte from the note into a file, run with only placeholders substituted, quoted back into the evidence and the note"

key-files:
  created: []
  modified:
    - .planning/phases/29-release-publication/29-COMMAND-EVIDENCE.md
    - docs/ROLLOUT-v1.7.0_gg1.md
    - docs/ROLLOUT-v1.7.0_gg2.md
    - docs/ROLLOUT-v1.7.0_gg3.md

key-decisions:
  - "The gg4 note must say that the gg3 name query misses the root carriage-return table and both double-quote tables, and must carry the companion query next to it; the union over-flags exactly n5_bt_tuple (a backtick in an element name)"
  - "The gg4 note can quote an enum-label query: it lists exactly the nine differing enum tables and nothing on the look-alike database, but was run on Enum8 tables only"
  - "A raw label put on gg3 for a dq or ap entry fails with the text 'Connection to ClickHouse is not active.' because the table is unmapped there; the other seven fail as the withheld lookup miss"
  - "The gg1 and gg2 notes may call the state-table query and both DELETE examples run, and must keep the key rule labelled read from the connector code"

requirements-completed: [ROLL-04, ROLL-05]

plan_head_before: b042740414c1556a18a700a7c766c99598c9c81c
plan_head_after: c227e06489d39965048edc2a41cb2ecc7713b221
duration: 11min
completed: 2026-10-23
status: complete

coverage:
  - id: D1
    description: "The gg3 and gg4 Column trees differ for exactly the 16 tables of bs, bsn, lf, tab, cr, sl, dq and btbsn (root and Tuple element), a gg3 unmapped table counted as different; a raw label lands on gg4 for all twelve entries and the outcome differs from gg3 for exactly nine"
    requirement: ROLL-04
    verification:
      - kind: other
        ref: "python3 -I /tmp/gsd29-scripts/verify03-task1.py (printed D995-NAMES-OK)"
        status: pass
    human_judgment: false
  - id: D2
    description: "In-flight retries for the backslash entry in both directions: dropped at the root and failed for a Tuple element with the landed row kept, DEFAULT stored without a window"
    requirement: ROLL-04
    verification:
      - kind: other
        ref: "python3 -I /tmp/gsd29-scripts/verify03-task1.py (printed D995-NAMES-OK)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The gg3 name query plus a companion list the 16 differing tables and one recorded over-flag, the label query lists exactly the nine differing enum tables, all three list nothing on the look-alike database; eight control-character DESCRIBE rows recorded"
    requirement: ROLL-04
    verification:
      - kind: other
        ref: "python3 -I /tmp/gsd29-scripts/verify03-task2.py (printed D995-QUERIES-OK)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The gg1 and gg2 notes carry the corrected enableDbTopicSplit=true key rule, the database sentence and a revision paragraph; the gg3 note no longer calls their rule shorter; the state-table query and both DELETE examples were run on a KeeperMap scratch table"
    requirement: ROLL-05
    verification:
      - kind: other
        ref: "python3 -I /tmp/gsd29-scripts/verify03-task3.py plus git diff --quiet P29_BASE HEAD over CHANGELOG.md, docs/FORK-DELTA.md, src, build.gradle.kts, gradle, .github (printed ROLL05-OK)"
        status: pass
    human_judgment: false
---

# Phase 29 Plan 03: Raw identifier names, enum labels and the gg1/gg2 key rule Summary

**Measured on ClickHouse 26.9.19.9 with client V2, the raw-identifier fix changes the Column tree of exactly 16 of 24 name tables and the label outcome of exactly 9 of 12 enum entries; the gg3 name query plus a companion find those tables, an enum-label query finds the nine, and the gg1 and gg2 notes now carry the key rule for `enableDbTopicSplit=true` that the gg3 note proved.**

## Performance

- **Duration:** 11 min (18:08Z to 18:19Z)
- **Tasks:** 3 (1 tracer, 2 auto)
- **Files modified:** 4 (`29-COMMAND-EVIDENCE.md`, `docs/ROLLOUT-v1.7.0_gg1.md`, `gg2.md`, `gg3.md`); no file outside those changed

## Accomplishments

- **Names (E-20).** `D995-DIFFERING=` lists 16 tables: root and Tuple element of `bs`, `bsn`, `lf`, `tab`, `cr`, `sl`, `dq` and `btbsn`. The two `dq` tables are unmapped on gg3 and mapped on gg4; the other 14 map on both with the earlier doubled or escaped name on gg3 and the raw name on gg4. `ctl`, `bt`, `na` and `ap` have the same names on both trees, and the 28 handoff table's old V2 and new columns match the measurement.
- **Labels (E-20).** A record carrying the raw label lands with code 1 on gg4 for all twelve entries. On gg3 it lands for `ctl`, `bt` and `na` and fails for the other nine, `LABEL-DIFFERING=ap,bs,bsn,btbsn,cr,dq,lf,sl,tab`. Seven fail as the withheld lookup miss; for `dq` and `ap` the table is unmapped on gg3 and the put fails with `RuntimeException` `Connection to ClickHouse is not active.`
- **In-flight retries (E-21, backslash entry, V2).** Upgrade (gg4 retries what gg3 landed, old doubled field name): a root field is dropped by the token and leaves the landed row on a table with a window; a Tuple element fails with `DataException` (`column 't.x\y' ... no value provided ...`) and leaves the landed row; a root field on a table without a window and nothing landed stores one row holding `DEFAULTED`. Rollback (gg3 retries what gg4 landed, raw field name): the same three outcomes.
- **Detection queries (E-22, E-23).** The gg3 note's name query, byte for byte, lists 14 fixture tables: 13 of the 16 differing and the over-flag `n5_bt_tuple`; it misses `n5_cr_root`, `n5_dq_root` and `n5_dq_tuple`, which the companion query lists (with `n5_cr_tuple` that both list). Both return nothing on the look-alike database. The enum-label query lists exactly the nine differing enum tables, no over-flag (`LABELQ-OVERFLAG=` empty), nothing on the look-alike database.
- **Control characters (E-24).** All eight tables (label and element, backspace, form feed, NUL, BEL) were created and are mapped by gg4. `system.columns.type` writes `\b`, `\f` and `\0` as backslash escapes and the BEL as a raw U+0007; the DESCRIBE JSON carries `\\b`, `\\f`, `\\0` in type strings, `\u0007` for the BEL, and `\b`, `\f`, `\u0000`, `\u0007` in the sub-column row names. The discovered enum keys and element names are the raw characters. No insert was run.
- **KeeperMap and DELETE (E-25).** On a state table created by the gg4 `KeeperStateProvider` the gg3 note's query with the entries `'orders'` and `'db1.orders'` returns exactly `db1.orders-2` and `orders-1`, left out `orders-0`, `orders-v2-0` and `other-0`, and returns nothing once both ranges are `AFTER_PROCESSING`. Both gg1 DELETE examples succeeded: the key example removed `orders-1` and `db1.orders-2`, the topic example (with the prefixed topic added) removed every partition of both topics, each leaving the others.
- **ROLL-05.** gg1 items 1 and 2 and gg2 item 1 carry the gg3 key rule (a topic that `dbTopicSplitChar` divides into exactly two parts is keyed under the topic name itself, any other under the prefixed form, the trailing-character case, labelled read from the connector code) and the database sentence; each note opens with a revision paragraph naming the corrected items. gg1 line 66 and the "not run" label of item 2 now state what E-25 shows. gg3 line 3 gets one sentence and line 263 no longer calls the gg1 and gg2 rule shorter. The gg1 Do-not-`TRUNCATE` paragraph and every query and DELETE statement are unchanged.

## Task Commits

1. **Task 1: names, labels and in-flight retries** - `46f829da` (docs, E-20 and E-21)
2. **Task 2: detection queries and control characters** - `e99234cd` (docs, E-22, E-23 and E-24)
3. **Task 3: KeeperMap self-check and the gg1, gg2, gg3 corrections** - `c227e064` (docs, E-25 and three notes)

**Plan metadata:** the SUMMARY commit that follows this file.

## Edited lines of the three notes

- `docs/ROLLOUT-v1.7.0_gg1.md`: new revision paragraph as line 3; the rule sentence (old line 62, now 64); the query-was-not-run sentence (old line 66, now 68); the item 2 "not run" label (old line 100, now 102); the key comment (old line 104, now 106-107) and one added comment line before the topic example.
- `docs/ROLLOUT-v1.7.0_gg2.md`: new revision paragraph as line 3; item 1 step 2 (old line 78) and its closing pointer sentence (old line 88).
- `docs/ROLLOUT-v1.7.0_gg3.md`: line 3 (one sentence appended) and line 263 (the "shorter" sentence replaced).

## Decisions Made

- The gg4 note's name detection must list both queries (the gg3 query and the companion) and name the over-flag `n5_bt_tuple`; `n5_ap_tuple` is listed by neither.
- The label query was kept as written (no refinement) and is described as run on `Enum8` tables only.
- The gg1 and gg2 notes may say their query and DELETE examples were run, but keep the key form labelled "read from the connector code, not measured", because the probe wrote the prefixed key as a state record itself.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocker] Probe compile error in the first Gradle run**
- **Found during:** Task 1, first run of the extended probe
- **Issue:** `retryCase` was declared `static` but calls the instance methods `getBaseProps()` and `uniqueConnectorName`; `compileTestJava` failed in the gg4 worktree and no test ran.
- **Fix:** removed `static` from the scratch probe and re-ran both trees; the aborted first run left no XML that was used (the XML copies were deleted before the real run).
- **Files modified:** `/tmp/gsd29/probe/Phase29RolloutProbe.java` (scratch, not committed)
- **Verification:** `compileTestJava` passes in both worktrees; D995-NAMES-OK printed.
- **Commit:** none (scratch file)

### Planned-versus-done details

- **Control-character tables.** The plan names one table `n5ctl_label` with four labels; the probe creates one label table per character (`n5ctl_label_<bsp|ff|nul|bel>`) so that a creation failure of one character cannot hide the others. No creation failed, so the outcome would have been the same. Recorded in E-24.
- **Eight PROBE-CTL lines** as required; the label lines report the per-character table's type and DESCRIBE rows.
- **The shared-ID closure of ROLL-04** is decided by the requirements verb below; this plan does not claim the plan-04 and plan-05 text that quotes the evidence.

**Total deviations:** 1 auto-fixed (scratch probe compile error), 1 planned-detail change (per-character label tables).
**Impact on plan:** none on the measured results or on the committed files.

## Issues Encountered

The first backgrounded Gradle command was started before the compile error surfaced; it was stopped (its Gradle processes ended with it) and the runs were repeated in the foreground. The OpenJ9 crash did not occur on any of the four Gradle runs; no javacore or core files were produced. Each run started its own container (server `26.9.19.9`, image `sha256:bdb5c911a23d...`).

## Known Stubs

None. No source file was created or modified.

## Threat Flags

None. Fixture DDL, inserts, landed rows and DELETEs ran only inside the Testcontainers ClickHouse of each Gradle run (T-29-12); the two DELETE examples keep a key or topic scope and were run on a scratch table that held another topic and a prefix look-alike, whose survival was checked (T-29-10); the key rule keeps "read from the connector code, not measured" (T-29-11); records are constants and the twelve fixed name entries (T-29-13). No TRUNCATE instruction was added. Nothing was pushed, tagged, merged or commented on.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 29-04 quotes E-20 (`D995-DIFFERING=`, `LABEL-DIFFERING=`), E-21 (the six retry lines and verdicts), E-22 (both queries, `NAMEQ-OVERFLAG=`), E-23 (the label query, empty `LABELQ-OVERFLAG=`) and E-25 (the state-table query and DELETE block, for gg4 item 4). Every entry reads `Matches text: pending (29-04)` except E-24, pending (29-05).
- Plan 29-05 uses E-24 for FORK-DELTA Known costs item 11: the server-side escapes are now measured, the test-side pin stays carried.
- Worktrees `/tmp/gsd29/wt-gg3` and `/tmp/gsd29/wt-tip` (detached at `46b2cf15`, `src/` equal to the current HEAD) and the probe are kept; their Gradle daemons were stopped. Local state outside this plan's scope remains dirty and untouched: `gradlew.bat`, `.planning/milestone.lock`.

## Self-Check: PASSED

Files verified on disk: `.planning/phases/29-release-publication/29-COMMAND-EVIDENCE.md` (E-20..E-25 present), `docs/ROLLOUT-v1.7.0_gg1.md`, `gg2.md`, `gg3.md`, the XML copies `/tmp/gsd29/exec/n5-gg3.xml`, `n5-gg4.xml`, `n5-query.xml`, `keeper.xml`. Commits verified as ancestors of HEAD: `46f829da`, `e99234cd`, `c227e064`. Verify commands printed D995-NAMES-OK, D995-QUERIES-OK and ROLL05-OK.

---
*Phase: 29-release-publication*
*Completed: 2026-10-23*
