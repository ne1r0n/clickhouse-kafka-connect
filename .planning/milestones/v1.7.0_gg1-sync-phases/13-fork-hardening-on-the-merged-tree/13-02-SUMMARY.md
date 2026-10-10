---
phase: 13-fork-hardening-on-the-merged-tree
plan: "02"
subsystem: testing
tags: [clickhouse, kafka-connect, rowbinary, jsoneachrow, parity, debezium, testcontainers]

requires:
  - phase: 13-fork-hardening-on-the-merged-tree
    provides: "13-01 MetadataFreeJsonWriter and the SCHEMA/DEBEZIUM_CDC routing; 13-07 RowSnapshots typed read-back helper"
provides:
  - "MetadataFreeJsonTypeParityLiveTest: live RowBinary-vs-JSON twin-table parity matrix (JSONP-06)"
  - "CDC envelope parity plus Debezium Date/MicroTimestamp literal pins"
  - "JSONP-07 null-semantics, nested-null, jsonInsertColumns and bypassFieldCleanup pins on Struct records"
affects: [13-08 behaviour ledger consolidation, docs/FORK-DELTA.md, Phase 14 BLD-11 Jackson bump, Phase 15 rollout notes]

actuals:
  tokens: 9600
  tasks: 2
  commits: 2
plan_head_before: 5dde9543dc4b5619d9e81ba288ec592cb9d33f62
plan_head_after: fd31cadcc413b374601478232145116679986140

tech-stack:
  added: []
  patterns:
    - "Twin-table parity: same Struct records through insertFormat=json and default RowBinary into identical DDL, read back with one typed SELECT, compared per id and column; literal anchors on the reference (RowBinary) side only, checked before the comparison"
    - "Projection builder gives every compared expression a distinct alias (col_s, col_hex) so ClickHouse alias shadowing cannot change what is compared"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/MetadataFreeJsonTypeParityLiveTest.java
  modified: []

key-decisions:
  - "No writer change: every D-04 cell, boundary, byte pattern and D-07 nested cell already stores the RowBinary value, so MetadataFreeJsonWriter and its unit test are untouched"
  - "Reference-side anchors include an epoch value (toUnixTimestamp64Milli) next to the toString literal so the anchor does not depend on the server time zone"

patterns-established:
  - "assertRoutesAgree(candidateTable, referenceTable, anchors): reuses the same helper for the route comparison and the bypassFieldCleanup true/false comparison"

requirements-completed: [JSONP-06, JSONP-07]

coverage:
  - id: D1
    description: "Timestamp, Date, Time, Decimal, bytes and nested cells (boundaries, empty, encoding, precision edges) store the RowBinary value through the metadata-free JSON route"
    requirement: JSONP-06
    verification:
      - kind: integration
        ref: "MetadataFreeJsonTypeParityLiveTest#logicalTypesReachRowBinaryParityOnTheMetadataFreeRoute"
        status: pass
    human_judgment: false
  - id: D2
    description: "Negative epochs (-1500 ms, -1 ms) reach parity in DateTime64(0/3/9) and Date32 (days -365, -1)"
    requirement: JSONP-06
    verification:
      - kind: integration
        ref: "MetadataFreeJsonTypeParityLiveTest#negativeEpochsReachRowBinaryParityInDateTime64AndDate32"
        status: pass
    human_judgment: false
  - id: D3
    description: "A DEBEZIUM_CDC envelope with a Connect Timestamp and a Decimal reaches parity; io.debezium.time.Date 19723 reads 2024-01-01 and MicroTimestamp 1704067200123456 reads 2024-01-01 00:00:00.123456"
    requirement: JSONP-06
    verification:
      - kind: integration
        ref: "MetadataFreeJsonTypeParityLiveTest#cdcEnvelopeReachesRowBinaryParityOnTheMetadataFreeRoute"
        status: pass
      - kind: integration
        ref: "MetadataFreeJsonTypeParityLiveTest#debeziumDateAndMicroTimestampReadBackAsLiterals"
        status: pass
    human_judgment: false
  - id: D4
    description: "Null semantics: Nullable columns store NULL (DEFAULT when the field is omitted), non-nullable columns take DEFAULT or the type default; nested nulls are coerced inside containers"
    requirement: JSONP-07
    verification:
      - kind: integration
        ref: "MetadataFreeJsonTypeParityLiveTest#nullFieldsStoreNullInNullableColumnsAndDefaultsElsewhere"
        status: pass
      - kind: integration
        ref: "MetadataFreeJsonTypeParityLiveTest#nestedNullsAreCoercedInsideContainers"
        status: pass
    human_judgment: false
  - id: D5
    description: "jsonInsertColumns=id,listed leaves unlisted columns at DEFAULT for Struct records; bypassFieldCleanup true/false land identical rows"
    requirement: JSONP-07
    verification:
      - kind: integration
        ref: "MetadataFreeJsonTypeParityLiveTest#jsonInsertColumnsLeavesUnlistedColumnsAtDefaultForStructRecords"
        status: pass
      - kind: integration
        ref: "MetadataFreeJsonTypeParityLiveTest#bypassFieldCleanupDoesNotChangeWhatLandsOnTheMetadataFreeRoute"
        status: pass
    human_judgment: false

duration: 12min
completed: 2026-10-08
status: complete
---

# Phase 13 Plan 02: Metadata-free JSON route type parity and null semantics Summary

**A live twin-table matrix proves the metadata-free JSON route stores the same values RowBinary stores for Timestamp, Date, Time, Decimal, bytes, nested Array/Map/Tuple cells and CDC envelopes, and pins JSONP-07 null semantics plus the jsonInsertColumns and bypassFieldCleanup interactions, with no writer change needed.**

## Performance

- **Duration:** about 12 min (start approximated at 17:14Z; the exact start was not captured at plan entry)
- **Started:** 2026-10-08T17:14:00Z
- **Completed:** 2026-10-08T17:26:00Z
- **Tasks:** 2
- **Files modified:** 1 (1 created)

## Accomplishments

- `logicalTypesReachRowBinaryParityOnTheMetadataFreeRoute`: 7 records (ids 1..7) through both routes into 20-column twin tables. Covers DateTime and DateTime64(0/3/6/9) at epochs 0, 1 ms, 86400123, 645453296789, 1704067200123 and 4102444800123 (year 2100); Date and Date32 days 0, 1, 19723, 40000; Time; five Decimal shapes (scale 0, scale 7 with 1E-7, Decimal(38,10) 12345678901234567890.0123456789, negative, zero, Kafka scale 6 into Decimal(18,4)); bytes (empty, "abc", all 256 values, `C3 28 ED A0 80 C0 80`, `22 5C 2F`, non-ASCII UTF-8, 300000 seeded random bytes, and a full-array `ByteBuffer` in a second column); `Array(DateTime64(3))`, `Map(String, Date)`, a Tuple of DateTime64(3) and Decimal(18,4), and `Map(String, Nullable(Int32))` with a null value. Every compared column agreed on the first run.
- `negativeEpochsReachRowBinaryParityInDateTime64AndDate32`: -1500 ms and -1 ms in DateTime64(0/3/9), days -365 and -1 in Date32.
- The comparison cannot pass vacuously: RowBinary-side anchors (row count 7, `2024-01-01 00:00:00.123` and epoch 1704067200123 for id 5, epoch 4102444800123 for id 6, `2024-01-01` for id 3, `12.3456` for id 1, hex `616263` for id 2, hex length 600000 for the random cell) run before the cross-route comparison.
- Mutation check: temporarily reverting the writer's Timestamp encoding to epoch milliseconds made both matrix tests fail (`ts0_s` of id 1: expected `1969-12-31 23:59:59`, was `1969-12-31 23:35:00`). The writer file was restored with `git checkout` and is unchanged in the plan diff.
- CDC parity, Debezium literals, null semantics, nested nulls, jsonInsertColumns and bypassFieldCleanup tests (6 more tests, 8 in the class).

## Task Commits

1. **Task 1: RowBinary-vs-JSON parity matrix and negative epochs** - `d2c5e7ad` (test)
2. **Task 2: CDC parity, Debezium literals, JSONP-07 null semantics, option interactions** - `fd31cadc` (test)

**Plan metadata:** recorded in the docs commit that follows this file.

## TDD Gate Compliance

Both tasks carried `tdd="true"` inside an `execute`-type plan and the plan prescribes one `test(13-02)` commit per task. These tests characterise behaviour delivered by plan 13-01, so there is no RED state to commit: the tests passed on first run. The validity of the tests (not vacuous, able to fail) was established by the reference-side anchors and by the Timestamp mutation run described above, in place of a failing-test commit. No `fix(13-02)` or `feat(13-02)` commit exists because no cell exposed a writer defect.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/MetadataFreeJsonTypeParityLiveTest.java` - 8 live tests; helpers `createTable`, `writeThroughTask`, `writeThroughJsonRoute`, `writeThroughBothRoutes`, `Projection`, `selectRows`, `assertRoutesAgree`, and the CDC envelope builders

## Decisions Made

- `MetadataFreeJsonWriter` is not changed. No matrix cell differed between routes, so the Stop rule never fired and no forced insert setting was needed (D-02).
- Compared SQL expressions get distinct aliases (`ts_dt_s`, `b_str_hex`) because ClickHouse lets a SELECT alias shadow the column of the same name inside the same SELECT, which would silently change `toUnixTimestamp(ts_dt)`.

## Behaviour ledger

**L-08: metadata-free JSON route parity and null semantics (JSONP-06, JSONP-07).** Observed against the ClickHouse `latest` Testcontainers image with client V2 and no forced insert setting.

- *Parity.* For every D-04 pair the JSON route stores the RowBinary value, across the boundary, empty, encoding and precision cells listed above. No writer fix was made in this plan.
- *Null semantics (JSONP-07), top-level fields all null:*

  | Column | Stored |
  |--------|--------|
  | `n Nullable(Int32)` | NULL |
  | `nd Nullable(Int32) DEFAULT 5` | 5 (field omitted, so the DEFAULT applies, not NULL) |
  | `nn Int32` | 0 |
  | `ndf Int32 DEFAULT 7` | 7 |
  | `s String` | '' |
  | `a Array(Int32)` | [] |
  | `m Map(String,String)` | {} |
  | `dt Nullable(DateTime64(3))` | NULL |

- *Nested nulls (L-07 confirmed live):* `Array(Int32)` `[1, null]` stores `[1,0]`; `Map(String,String)` `{k: null}` stores `{'k':''}`; `Map(String, Nullable(Int32))` `{k: null}` stores `{'k':NULL}`; a nested Struct with null `x`, `y` into `Tuple(x Nullable(Int32), y Int32)` stores `(NULL,0)`.
- *jsonInsertColumns:* with `jsonInsertColumns=id,listed`, a Struct carrying `listed` 9, `extra` 777, `tag` "should_not_land" and a non-column field lands `listed` 9 while `extra` and `tag` keep their DEFAULTs 42 and `unset`. The column list is applied server-side on the Struct route exactly as on the schemaless route (Phase 9 D-06).
- *bypassFieldCleanup:* `true` and `false` land the identical row (id 1, identical `hex(str)`) for a Struct with a non-column field; the option has no effect on the metadata-free route.
- *Operator caveat.* These non-nullable outcomes rely on the server's default `input_format_null_as_default=1`. An operator who sets it to 0 changes them. The connector deliberately does not force it (D-02); carry this to the Phase 15 rollout notes.
- Pinned by the eight `MetadataFreeJsonTypeParityLiveTest` methods.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Tightening] `'UTC'` zone on the CDC and Debezium-literal timestamp columns**
- **Found during:** Task 2
- **Issue:** The plan's CDC table declares `ts DateTime64(3)` and the literal table `DateTime64(6)`; `toString` of a column without a zone follows the server default zone, which a test cannot control.
- **Fix:** declared `DateTime64(3, 'UTC')` and `DateTime64(6, 'UTC')`, the same adjustment plan 13-01 made. The main matrix keeps plain `DateTime64(p)` as specified and adds an epoch-based anchor (`toUnixTimestamp64Milli`) next to the `toString` literal.
- **Files modified:** `MetadataFreeJsonTypeParityLiveTest.java`
- **Committed in:** `fd31cadc`

**2. [Rule 3 - Tightening] Projection aliases differ from column names**
- **Found during:** Task 1 design
- **Issue:** `toString(ts_dt) AS ts_dt` next to `toUnixTimestamp(ts_dt)` would let the alias shadow the column.
- **Fix:** every projected expression has a distinct alias.
- **Files modified:** `MetadataFreeJsonTypeParityLiveTest.java`
- **Committed in:** `d2c5e7ad`

---

**Total deviations:** 2 (both test-scaffolding, no production-code deviation)
**Impact on plan:** None on behaviour or scope.

## Issues Encountered

- `verify-merge-invariants.sh` needs the merge commit as an argument; run with `54e63648` it reports `result=PASS`.
- The JSON-only tests (Debezium literals, null semantics, nested nulls, jsonInsertColumns, bypassFieldCleanup) pin current behaviour, so none could fail on first run against unchanged production code; their assertions are the literal values listed in the ledger.

## Known Stubs

None.

## Threat Flags

None. T-13-05 is mitigated as planned (reference-side literal anchors and row-count check run before the comparison; bytes compared through `hex()`); T-13-06 accepted (table names come from `createTopicName`, values never enter SQL text).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 13-08 can consolidate L-08 into the behaviour ledger. FORK-DELTA should state the non-nullable null coercion and its `input_format_null_as_default` dependency.
- Phase 14 BLD-11 (Jackson bump) should re-run `MetadataFreeJsonTypeParityLiveTest` and `MetadataFreeJsonWriterTest` together.

## Verification Results

- `MetadataFreeJsonTypeParityLiveTest` 8 tests, `ExplicitJsonInsertPathLiveTest` 7, `JsonInsertColumnsLiveTest` 5: all `failures="0" errors="0" skipped="0"`.
- `MetadataFreeJsonWriterTest` 33/33. `testUnit` 406 tests, 404 succeeded, 0 failed, 2 skipped (pre-existing named skips).
- Acceptance greps re-run: `extends ClickHouseBase`, `RowSnapshots.select`, `hex(`, `toString(` present; `countRows` and `date_time_input_format` absent; all required DDL types and literals present; `git log --grep='(13-02)' -- ClickHouseWriter.java` prints nothing.

## Self-Check: PASSED

- FOUND: `src/test/java/com/clickhouse/kafka/connect/sink/MetadataFreeJsonTypeParityLiveTest.java`
- FOUND commits: `d2c5e7ad`, `fd31cadc` (both ancestors of HEAD; `rev-list --count` from the plan base is 2)

---
*Phase: 13-fork-hardening-on-the-merged-tree*
*Completed: 2026-10-08*
