---
phase: 20-release-publication
plan: "02"
subsystem: release
tags: [version-bump, rollout-evidence, detection-queries, clickhouse, probe, keepermap, deduplication]

requires:
  - phase: 20-release-publication
    provides: "20-01 dedup-token pins and E-01..E-04 (the dedup outcome the table-classification query must agree with)"
  - phase: 19-nullable-enum-tuple-mapping
    provides: "the Nullable(Enum)/Nullable(Tuple) fixes whose gg1 symptoms the detection queries find"
  - phase: 18-writer-correctness
    provides: "the empty-CDC skip (ledger L-30) whose gg1 phantom rows the empty-CDC query finds"
provides:
  - "VERSION v1.7.0_gg2 with its fork-anchors row in one commit (PUB-07 version half)"
  - "20-COMMAND-EVIDENCE.md E-10..E-20: live-proven doc-form queries and observed outcomes for the gg2 rollout note"
affects: [20-03 rollout note, 20-05 review fixes, 20-04 changelog]

actuals:
  tokens: 8300
  tasks: 2
  commits: 3

plan_head_before: e1fab988402025bfb56fb9258b3649be3d0d5223
plan_head_after: fcdd39292313f96380feaae31617d7b8d6728eab

tech-stack:
  added: []
  patterns:
    - "Doc-form SQL kept in files with placeholders; the probe substitutes and runs it unchanged, so the quoted text is the proven text"
    - "Detection queries are proven against rows written by the old code (a worktree of the old SHA) and must flag nothing for rows the new code writes from the same records"

key-files:
  created:
    - .planning/phases/20-release-publication/20-02-SUMMARY.md
  modified:
    - VERSION
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/20-release-publication/20-COMMAND-EVIDENCE.md

key-decisions:
  - "The Nullable(Tuple) query lists wrong-row suspects (tuple not NULL, every element NULL or empty) plus NULL-tuple rows in the same part as a suspect, because the gg1 phantom rows carry a garbage key and cannot be recognised by content"
  - "Enum8 top-level, Enum8 array and Enum8 map each get their own query; one query text serves Enum8 and Enum16 top-level columns, since gg1 failed the Enum16 batch outright and stored nothing"
  - "The probe resolves one doc-form file per query key (enum-top, enum-array, enum-map) instead of one file per shape key; shapes enum8-top and enum16-top share enum-top.sql"

patterns-established:
  - "Evidence entries carry a sql block, the substitution map, the verbatim PROBE lines of both trees and the verdict pending (20-03)"

requirements-completed: [PUB-07, ROLL-02]

coverage:
  - id: D1
    description: "VERSION is v1.7.0_gg2 and its anchor row moved with it in one commit with exactly those two files; the clean build generates ARTIFACT_VERSION v1.7.0_gg2 and the anchors invariant ends result=PASS"
    requirement: PUB-07
    verification:
      - kind: other
        ref: "Task 1 automated verify VERSION-BUMP-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "Each 999.1 shape that gg1 stores silently wrong (tuple-c2-plain, enum8-top, enum8-array, enum8-map, tuple-x1-null-field) has a detection query returning rows on the gg1-written fixture and none on the gg2-written one"
    requirement: ROLL-02
    verification:
      - kind: integration
        ref: "20-COMMAND-EVIDENCE.md E-11..E-15 (probe XMLs /tmp/gsd20/exec/probe-gg1.xml, probe-gg2.xml) and Task 2 verify PROBE-EVIDENCE-OK"
        status: pass
    human_judgment: false
  - id: D3
    description: "The empty-CDC phantom-row query returns the two all-defaults rows gg1 writes on the bypassRowBinary route and none for gg2"
    requirement: ROLL-02
    verification:
      - kind: integration
        ref: "20-COMMAND-EVIDENCE.md E-16"
        status: pass
    human_judgment: false
  - id: D4
    description: "The topic-scoped KeeperMap self-check, run on a state table created by KeeperStateProvider, returns exactly orders-1 while in flight, excludes orders-v2-0, and returns nothing once orders-1 is AFTER_PROCESSING"
    requirement: ROLL-02
    verification:
      - kind: integration
        ref: "20-COMMAND-EVIDENCE.md E-17"
        status: pass
    human_judgment: false
  - id: D5
    description: "The table-classification query marks default ReplicatedMergeTree and MergeTree with a window as deduplicating and plain MergeTree and ReplicatedMergeTree with window 0 as not, agreeing with E-03"
    requirement: ROLL-02
    verification:
      - kind: integration
        ref: "20-COMMAND-EVIDENCE.md E-18"
        status: pass
    human_judgment: false
  - id: D6
    description: "MBean version segment and client name read v1.7.0_gg2 on the bumped code and v1.7.0_gg1 on the old code"
    requirement: PUB-07
    verification:
      - kind: integration
        ref: "20-COMMAND-EVIDENCE.md E-19"
        status: pass
    human_judgment: false
  - id: D7
    description: "No probe residue: the probe class, both worktrees and any src change are absent from the repository"
    requirement: ROLL-02
    verification:
      - kind: other
        ref: "Task 2 automated verify PROBE-EVIDENCE-OK (worktree list, ls-files, status, src diff against P20_BASE)"
        status: pass
    human_judgment: false

duration: 15min
completed: 2026-10-14
status: complete
---

# Phase 20 Plan 02: VERSION bump and live rollout-query evidence Summary

**`VERSION` is `v1.7.0_gg2` (with its anchor row, one commit), and every query the gg2 rollout note will quote was run live on ClickHouse 26.9.18.11 against rows written by the gg1 code (`be6b8861`) and by the bumped code, with the doc-form text and both outputs recorded as E-10..E-20.**

## Performance
- **Duration:** 15 min
- **Started:** 2026-10-14T12:59:17Z
- **Completed:** 2026-10-14T13:15:16Z
- **Tasks:** 2 (one tracer, one auto)
- **Files modified:** `VERSION`, `.planning/scripts/fork-anchors.txt`, `20-COMMAND-EVIDENCE.md`; nothing under `src/`

## Accomplishments
- Bump commit `43c82975` changes exactly `VERSION` and `.planning/scripts/fork-anchors.txt`. After `./gradlew clean compileJava` the generated `Version.java` holds `ARTIFACT_VERSION = "v1.7.0_gg2"`, and `verify-merge-invariants.sh 54e63648 HEAD` ends `result=PASS` (316 anchor commits, 59 anchor rows).
- One throwaway probe (`/tmp/gsd20/probe/Phase20RolloutProbe.java`) ran in two detached worktrees, gg1 at `be6b8861` and gg2 at the bump commit, each in its own Testcontainers container; both reported server `26.9.18.11` (E-10, E-12).
- What the old and new code stored from the same records (all observed, none assumed):

| Shape | gg1 (`be6b8861`) | gg2 (bumped) | Query flags gg1 / gg2 |
|---|---|---|---|
| `tuple-c2-plain` | stored, wrong: 3 rows with tuple not NULL, `a` NULL, `b` empty, plus 3 phantom rows under id 16777216 with a NULL tuple; no error | round-trips | 6 / 0 |
| `enum8-top` | stored, every label became NULL; no error | round-trips | 3 / 0 |
| `enum16-top` | failed, `ServerException` `CANNOT_READ_ALL_DATA`, no row | round-trips | 0 / 0 (nothing stored by gg1) |
| `enum8-array` | stored `[NULL,NULL]`; no error | round-trips | 2 / 0 |
| `enum8-map` | stored `{'k1':NULL,'k2':NULL}`; no error | round-trips | 2 / 0 |
| `tuple-x1-null-field` | stored a NULL tuple silently | failed, `DataException` naming `tuple field 'a'` (L-38), no row | 1 / 0 |
| `empty-cdc-bypass` | stored the 2 real rows plus 2 all-defaults phantom rows | stored the 2 real rows | 2 / 0 |

- KeeperMap self-check (E-17): on a state table created by `KeeperStateProvider`, the query for topic `orders` returned exactly `orders-1` while it was `BEFORE_PROCESSING`, excluded `orders-v2-0`, and returned nothing after `orders-1` became `AFTER_PROCESSING`.
- Table classification (E-18): replicated default 1, non-replicated window 100 gives 1, plain `MergeTree` 0, replicated window 0 gives 0; this agrees with the insert-measured E-03.
- MBean and client name (E-19): both MBeans of the gg2 run carry `version=v1.7.0_gg2` and the user agent starts `clickhouse-kafka-connect/v1.7.0_gg2`; the gg1 run shows `v1.7.0_gg1` for both.
- Both worktrees were removed and pruned; no probe file is tracked or untracked in the main tree; `git diff P20_BASE HEAD -- src` lists only `DedupTokenRetryLiveTest.java`.

## Task Commits
1. **Task 1 (tracer): bump and the Nullable(Tuple) probe** - `43c82975` (chore: bump), `b43f9878` (docs: E-10, E-11)
2. **Task 2: remaining queries on gg1 and gg2 rows, worktrees removed** - `fcdd3929` (docs: E-12..E-20)

**Plan metadata:** recorded in the docs commit that carries this summary.

## Files Created/Modified
- `VERSION` - `v1.7.0_gg2`.
- `.planning/scripts/fork-anchors.txt` - the `VERSION` row now `VERSION<TAB>v1.7.0_gg2<TAB>1<TAB>BLD-07`.
- `.planning/phases/20-release-publication/20-COMMAND-EVIDENCE.md` - entries E-10 (versions), E-11 (Tuple C2), E-12 (Enum8/Enum16 top level), E-13 (Enum array), E-14 (Enum map), E-15 (single-field tuple), E-16 (empty CDC), E-17 (KeeperMap self-check), E-18 (table classification), E-19 (MBean and client name), E-20 (shapes that failed instead of storing). Each query entry has a `sql` block for plan 20-03 to quote byte for byte, the substitution map, the verbatim `PROBE-` lines of both trees and the verdict `pending (20-03)`.
- Throwaway, kept for plan 20-05: `/tmp/gsd20/probe/` (probe, `sql/*.sql`, `xml-probe-lines.py`, `make-evidence.py`) and the XMLs under `/tmp/gsd20/exec/`.

## Decisions Made
- The Tuple C2 query lists two kinds of suspect (a non-NULL tuple whose elements are all NULL or empty, and NULL-tuple rows in a part that also holds such a suspect) because gg1's phantom rows have a garbage key (16777216 here) and no content that identifies them. Both are called suspects in the evidence and the query reasons.
- Enum8 and Enum16 top-level columns share one query text; array and map get their own. The probe resolves doc-form files by query key, not by shape key, so `enum8-top` and `enum16-top` both read `enum-top.sql`.
- `empty-cdc-bypass` is detected by key and `_version` both 0, adapted from the ledger's L-30 example to list rows; the entry states that key 0 can occur legitimately.

## Deviations from Plan

**1. [Rule 3 - Plan wording] One doc-form file per query key instead of per shape key**
- **Found during:** Task 2, writing the Enum queries
- **Issue:** the plan names `/tmp/gsd20/probe/sql/<shape>.sql` for every shape; `enum8-top` and `enum16-top` would have needed two identical files, against the plan's own "written once" rule.
- **Fix:** the probe keeps a query key per shape (`enum-top`, `enum-array`, `enum-map`, plus the shape names for the rest); `PROBE-RESULT` lines still use the shape key, so the verify is unaffected.
- **Files modified:** `/tmp/gsd20/probe/Phase20RolloutProbe.java` (throwaway)
- **Commit:** none (probe is never committed)

**2. [Rule 3 - Tooling] Evidence for E-12..E-20 generated by a script**
- **Found during:** Task 2, evidence step
- **Issue:** hand-copying about 30 verbatim `PROBE-` lines and nine `sql` blocks risks transcription errors in text plan 20-03 must quote byte for byte.
- **Fix:** `/tmp/gsd20/probe/make-evidence.py` reads the `sql/*.sql` files and the probe XML lines and emits the entries; its output was appended to the evidence file. The Task 2 verify confirms every `PROBE-RESULT`, `PROBE-KEEPER` and `PROBE-DEDUP-TABLE` line of both XMLs is present in the evidence.
- **Files modified:** `20-COMMAND-EVIDENCE.md`
- **Commit:** `fcdd3929`

**Total deviations:** 2 (both Rule 3, tooling and wording). **Impact:** none on scope or outcome.

No checkpoint stop was needed: gg1 on today's `latest` showed the silent wrong rows the plan's premise (A6) expects for `tuple-c2-plain` and `enum8-top`.

## Issues Encountered
- The Task 1 first draft of the Tuple query had no phantom-row coverage; the observed gg1 output showed why (garbage key, NULL tuple), and the part-co-location clause was added before the query was recorded.
- `latest` pulled `26.9.18.11` for both worktree runs, so no server-version difference between gg1 and gg2 needs a caveat (assumption A5).

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Plan 20-03 can quote the `sql` blocks of E-11..E-18 byte for byte. Wording facts for it: every detection result is a suspect list; `enum16-top` and the `tuple-x1` NULL-field shape are refused by one release or the other (E-20), not silently wrong in both; the dedup-tables query ignores `replicated_deduplication_window_seconds` and was not run on `SharedMergeTree`.
- The probe stays in `/tmp/gsd20/probe` so plan 20-05 can re-run a query if a review fix changes it (the worktrees must be recreated).
- No blockers.

## Known Stubs
None.

## Threat Flags
None - only `VERSION`, an anchors row and a planning document were changed; the probe and both worktrees were removed.

---
*Phase: 20-release-publication*
*Completed: 2026-10-14*

## Self-Check: PASSED
- `VERSION`, `fork-anchors.txt`, `20-COMMAND-EVIDENCE.md` exist; E-10..E-20 present.
- Commits `43c82975`, `b43f9878`, `fcdd3929` are ancestors of HEAD; `gsd check evaluation-scope --plan 20-02 --commits-only` resolves them.
- Task verifies printed `VERSION-BUMP-OK`, `PROBE-TRACER-OK` and `PROBE-EVIDENCE-OK`; `git worktree list` shows only the main tree.
