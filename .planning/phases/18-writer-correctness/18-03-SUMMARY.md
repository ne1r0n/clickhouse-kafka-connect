---
phase: 18-writer-correctness
plan: 03
subsystem: sink-writer
tags: [clickhouse-writer, debezium-cdc, empty-record-skip, rowbinary, json-each-row, live-tests, fork-anchors, JSONP-09]
requires:
  - phase: 18-writer-correctness
    provides: "18-01/18-02 ledger rows L-23..L-29 and the Phase 18 anchor block"
provides:
  - "One shared skipEmptyCdcRecord helper in ClickHouseWriter, called from writeRecordsJson and writeRecordsRowBinary (JSONP-09)"
  - "No phantom all-defaults rows from empty Debezium records on the bypass JSON and RowBinary routes; the metadata-free route keeps its skip with the identical DEBUG line"
  - "CdcTestFixtures: shared Debezium envelope fixtures, canonical batches and the snapshot read-back"
  - "EmptyCdcSkipRouteLiveTest: four live pins (RowBinary DEFAULT table, bypass, explicit, RowBinary plain table) on clients V1 and V2"
  - "Ledger row L-30, two fork-anchors rows, a FORK-DELTA Connector source row, metadata-free row sentence updated, Known costs item 2 closed"
affects: [18-04, 20-release]
plan_head_before: ceab6e22c91119df2dc22796cb2cc7f57d86f4fd
plan_head_after: ae08298821c554fbfddcf322cbb7aecc0c687f12
actuals:
  tokens: 5500
  tasks: 2
  commits: 2
tech-stack:
  added: []
  patterns:
    - "Skip decision lives in the writer serialization loops, never before them, so offsets, exactly-once state and the dedup token cover the whole batch"
    - "Route live test: three batches (mixed, all-empty, empty-first) through one task with strictly increasing offsets, snapshot read-back after each put"
key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/CdcTestFixtures.java
    - src/test/java/com/clickhouse/kafka/connect/sink/EmptyCdcSkipRouteLiveTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - .planning/phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md
key-decisions:
  - "No short-circuit for an all-skipped batch (D-01): the insert still opens with an empty body; both clients accepted it on 26.9.16.11"
  - "The skip log is the metadata-free route's existing DEBUG line moved into the helper, byte-identical, one line per skipped record, no batch count (D-02)"
  - "No JMX metric for skipped records (D-04)"
requirements-completed: [JSONP-09]
coverage:
  - id: D1
    description: "JSONP-09: RowBinary against an all-DEFAULT table lands exactly the 2 real rows of a mixed Debezium batch, typed read-back of all six columns including ts"
    requirement: JSONP-09
    verification:
      - kind: integration
        ref: "EmptyCdcSkipRouteLiveTest#JSONP-09: RowBinary route with DEFAULT columns ... (V1 and V2)"
        status: pass
    human_judgment: false
  - id: D2
    description: "JSONP-09: bypassRowBinary=true JSON route lands exactly the 2 real rows, read back without ts (L-03)"
    requirement: JSONP-09
    verification:
      - kind: integration
        ref: "EmptyCdcSkipRouteLiveTest#JSONP-09: bypassRowBinary JSON route ... (V1 and V2)"
        status: pass
    human_judgment: false
  - id: D3
    description: "JSONP-09: explicit insertFormat=json route lands exactly the 2 real rows with ts; the existing metadata-free pin stays green unchanged"
    requirement: JSONP-09
    verification:
      - kind: integration
        ref: "EmptyCdcSkipRouteLiveTest#JSONP-09: explicit insertFormat=json route ...; ExplicitJsonInsertPathLiveTest (7 cases)"
        status: pass
    human_judgment: false
  - id: D4
    description: "JSONP-09: RowBinary on a plain non-null table lands the real rows instead of failing the batch"
    requirement: JSONP-09
    verification:
      - kind: integration
        ref: "EmptyCdcSkipRouteLiveTest#JSONP-09: RowBinary route on a plain non-null table ... (V1 and V2)"
        status: pass
    human_judgment: false
  - id: D5
    description: "D-01 live: an all-empty batch completes put() with no exception and adds 0 rows on every route, both clients; an empty-first batch lands its real row"
    requirement: JSONP-09
    verification:
      - kind: integration
        ref: "EmptyCdcSkipRouteLiveTest (second and third batch of every route test)"
        status: pass
    human_judgment: false
  - id: D6
    description: "Anchors: skipEmptyCdcRecord( 3, the skip-log text 1, shouldSkipTableMetadata() still 8, buildRowBinaryUnsupportedTypeMessage still 5; helper allocation-free and ordered after the tombstone check"
    requirement: JSONP-09
    verification:
      - kind: other
        ref: "Task 1 second automated verify (structure, anchors at HEAD, commit membership)"
        status: pass
    human_judgment: false
duration: 5min
completed: 2026-10-12
status: complete
---

# Phase 18 Plan 03: JSONP-09 empty-CDC skip on every route Summary

**A Debezium record that carries no values (op=t, or a null before/after struct) now inserts no row on the explicit JSON, `bypassRowBinary=true` JSON and RowBinary routes, decided by one allocation-free helper inside `ClickHouseWriter`, and is pinned live on ClickHouse 26.9.16.11 with clients V1 and V2.**

## Performance
- **Duration:** 5 min
- **Started:** 2026-10-12T13:58:41Z
- **Completed:** 2026-10-12T14:03:30Z (code; SUMMARY and state follow)
- **Tasks:** 2
- **Files modified:** 7 (1 in `src/main`, 2 new test classes, 1 migrated test class, `fork-anchors.txt`, `FORK-DELTA.md`, the ledger)

## Accomplishments
- `skipEmptyCdcRecord(Record)` returns false unless the record is `DEBEZIUM_CDC` with a null or empty `getJsonMap()`; only then does it log the unchanged DEBUG line (through `SKIP_EMPTY_CDC_LOG_FORMAT`) and return true. It reads no config, time or retry state, and allocates nothing on the non-skip path.
- It is called right after the tombstone check in `writeRecordsJson` (above the three-way branch, replacing the inline metadata-free block) and as the first statement inside the non-null branch of `writeRecordsRowBinary`. No routing, insert method, `ProxySinkTask` or `Processing` change.
- `CdcTestFixtures` holds the envelope schemas, `cdcRow`, `cdcEnvelope`, `cdcSinkRecords`, the three canonical batches and `allRows`. `ExplicitJsonInsertPathLiveTest` now reaches them through four static imports; its tests, assertions, `allRows` and `cdcEnvelope` calls are unchanged (counts compared against the phase-start blob).

## Task Commits
1. **Task 1 (tracer): helper, both loops, DEFAULT-table live test, anchors, FORK-DELTA and ledger** - `f7e1440b` (fix)
2. **Task 2: bypass, explicit and plain-table RowBinary live tests; fixture migration** - `ae082988` (test)

Tracer gate: Task 1's `<verify>` holds only automated commands, so it was re-run after the commit (test XMLs, structure checks, anchors at HEAD, commit membership) and passed. Tracer verified end-to-end, then Task 2 expanded.

## Red and green evidence
- **Red (unfixed writer, V2, RowBinary against an all-DEFAULT table):** `rows after the mixed batch: [{id=0, ts=1970-01-01 00:00:00.000, amount=0, name=, v=0, del=0}, {id=0, ts=1970-01-01 00:00:00.000, amount=0, name=, v=0, del=0}, {id=1, ts=2024-01-01 00:00:00.123, amount=12.3456, name=created, v=100, del=0}, {id=2, ts=2024-01-01 00:00:00.123, amount=12.3456, name=deleted, v=101, del=1}] ==> expected: <2> but was: <4>`. Two phantom rows (id 0, `_version` 0), failing on the planned row-count assertion, not on a fixture error.
- **Green (tracer, V2):** `EmptyCdcSkipRouteLiveTest` 1/1, `ExplicitJsonInsertPathLiveTest` 7/7, `MetadataFreeJsonInsertUnitTest` 23/23, 0 failures, errors or skips.

## Route results (ClickHouse 26.9.16.11)
| Route | Table | assertTs | V1 | V2 |
|-------|-------|----------|----|----|
| RowBinary | all DEFAULT | true | pass | pass |
| `bypassRowBinary=true` | plain | false (L-03) | pass | pass |
| `insertFormat=json` | plain | true | pass | pass |
| RowBinary | plain non-null | true | pass | pass |

`EmptyCdcSkipRouteLiveTest` 4/4 on V1 and on V2; `ExplicitJsonInsertPathLiveTest` 7/7 on V2 after the migration. No body rejection occurred: every all-empty batch (offsets 5-7) completed `put()` with the table unchanged on both clients. `ts` read back as `2024-01-01 00:00:00.123` on the RowBinary and explicit routes. Neither the D-01 stop rule nor the `ts` stop rule was raised.

## Files Created/Modified
- `src/main/java/.../sink/db/ClickHouseWriter.java` - helper, `SKIP_EMPTY_CDC_LOG_FORMAT`, two call sites, inline block removed
- `src/test/java/.../sink/CdcTestFixtures.java` - shared CDC fixtures and snapshot read-back
- `src/test/java/.../sink/EmptyCdcSkipRouteLiveTest.java` - four live route pins
- `src/test/java/.../sink/ExplicitJsonInsertPathLiveTest.java` - moved onto the shared fixtures
- `.planning/scripts/fork-anchors.txt` - two JSONP-09 rows
- `docs/FORK-DELTA.md` - empty-CDC row after WRT-11, metadata-free row sentence, item 2 closed
- `.planning/phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md` - L-30 and the Source plans line

## Decisions Made
- Followed the plan's D-01..D-04 as written (no short-circuit, one DEBUG line per skipped record moved byte-identically, no metric).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Spotless IDE-hook gate cannot be IS CLEAN for `ClickHouseWriter.java`**
- **Found during:** Task 1 commit step
- **Issue:** Same as 18-01 and 18-02. The hook reports `IS DIRTY` for the phase-start file because upstream keeps its own import order. I diffed the hook output against the file and filtered import and blank lines: nothing else differs, so this plan adds no formatting debt. `CdcTestFixtures.java`, `EmptyCdcSkipRouteLiveTest.java` and `ExplicitJsonInsertPathLiveTest.java` are `IS CLEAN`.
- **Fix:** Did not reformat `ClickHouseWriter.java`, to keep the fork rebasable. The `IS CLEAN` clause was run for the three test files; every other clause of the verify ran and passed.
- **Files modified:** none
- **Commit:** n/a

**Total deviations:** 1 (a blocking-check adjustment, carried from earlier plans). **Impact:** none on behaviour or scope.

## Issues Encountered
None. The green run passed on the first attempt after the writer edit.

## Authentication Gates
None.

## Known Stubs
None.

## Threat Flags
None. T-18-09 (the call sites follow the tombstone check and precede cleanup and the column loop; no routing or insert method changed) and T-18-10 (every real row read back with `assertEquals` on each route) are mitigated here, with the Docker-free state and byte-identity pins following in 18-04. T-18-11 is mitigated by the live all-empty batches on both clients. T-18-12 is accepted and recorded in L-30. T-18-13 holds: the DEBUG line takes topic, partition and offset only.

## Next Phase Readiness
18-04 can add the Docker-free byte-identity, edge, log-line and exactly-once state pins and run the phase gate. `CdcTestFixtures` is ready for reuse; the ledger continues at L-31. Phase 20 should carry L-30 (phantom-row query and the token-based upgrade note) into the CHANGELOG, FORK-DELTA and ROLL-02.

---
*Phase: 18-writer-correctness*
*Completed: 2026-10-12*

## Self-Check: PASSED

- Files: `CdcTestFixtures.java`, `EmptyCdcSkipRouteLiveTest.java` and the other artifacts exist on disk.
- Commits: `f7e1440b` and `ae082988` are ancestors of HEAD; `gsd-tools check evaluation-scope --plan 18-03 --commits-only` resolved both.
- Acceptance criteria re-run after the last commit: JSONP09-TRACER-GREEN checks, anchors at HEAD (`skipEmptyCdcRecord(` 3, skip-log text 1, `shouldSkipTableMetadata()` 8, `buildRowBinaryUnsupportedTypeMessage` 5), JSONP09-ROUTES-GREEN and FIXTURE-MIGRATION-OK all passed, apart from the documented Spotless adjustment for `ClickHouseWriter.java`.
