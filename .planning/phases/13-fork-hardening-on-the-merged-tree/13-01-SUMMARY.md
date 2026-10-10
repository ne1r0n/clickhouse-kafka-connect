---
phase: 13-fork-hardening-on-the-merged-tree
plan: "01"
subsystem: sink-db-json-route
tags: [clickhouse, kafka-connect, jackson, jsoneachrow, debezium, exactly-once]

requires:
  - phase: 12-merge-line-decisions
    provides: v1.7.0 merge commit M (54e63648), fork-anchors.txt, verify-merge-invariants.sh
provides:
  - "Routing: SCHEMA and DEBEZIUM_CDC records reach doInsertJson when bypassRowBinary or an explicit insertFormat=json asks for it (R-01 fixed)"
  - "MetadataFreeJsonWriter: route-scoped, byte-based, schema-aware row writer (structRow, cdcRow)"
  - "Data.getSchema() additive accessor"
  - "Docker-free routing matrix and byte-level encoding pins; flipped R-01 live test; live CDC route test"
affects: [13-02 type parity matrix, 13-08 behaviour ledger consolidation, docs/FORK-DELTA.md, Phase 14 BLD-11 Jackson bump, Phase 15 ROLL-01]

actuals:
  tokens: 13700
  tasks: 2
  commits: 2

plan_head_before: 75cfb5cf390fa2d525c5d6f47eef98115caf36b1
plan_head_after: d3f6d6fb2dab3f5287611deafcc634359295b116

tech-stack:
  added: []
  patterns:
    - "Route-scoped Jackson writer: DataJson.OBJECT_MAPPER.copy() plus a named SimpleModule; the shared mapper is never mutated"
    - "Logical types dispatched on schema.name() and guarded by runtime class, at every nesting depth"

key-files:
  created:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriter.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/main/java/com/clickhouse/kafka/connect/sink/data/Data.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java
    - .planning/scripts/fork-anchors.txt

key-decisions:
  - "Routing keyed on shouldSkipTableMetadata(), unconditional, no opt-out flag (D-05, D-06, D-08)"
  - "Row writer is a private copy of the shared mapper with a named SimpleModule, so the upstream bypassRowBinary=true bytes stay identical (D-05)"
  - "Live CDC test creates ts as DateTime64(3, 'UTC') so toString(ts) does not depend on the server time zone"

patterns-established:
  - "Empty CDC value maps are skipped on the metadata-free route before any bytes are written (D-19)"
  - "Shared row tail (time accounting, TRACE payload log, bytesInserted, write) lives in ClickHouseWriter.writeJsonRow"

requirements-completed: [JSONP-05, JSONP-06]

coverage:
  - id: D1
    description: "insertFormat=json with a Struct record and bypassRowBinary unset lands every record, read back by typed SELECT (R-01 fixed)"
    requirement: JSONP-05
    verification:
      - kind: integration
        ref: "ExplicitJsonInsertPathLiveTest#explicitJsonStructRecordWithDefaultBypassRowBinaryLandsEveryRecord"
        status: pass
    human_judgment: false
  - id: D2
    description: "doInsertBatch routing matrix for SCHEMA and DEBEZIUM_CDC across json, unset, none and bypassRowBinary"
    requirement: JSONP-05
    verification:
      - kind: unit
        ref: "MetadataFreeJsonInsertUnitTest#schemaAndCdcRecordsRouteToJsonOnlyWhenMetadataFreeOrBypassed (7 invocations)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Timestamp, Date, Time, Decimal and bytes encodings at every depth, null placement, determinism and shared-mapper guard"
    requirement: JSONP-06
    verification:
      - kind: unit
        ref: "MetadataFreeJsonWriterTest (33 invocations)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Debezium CDC conversion scope and the empty-CDC skip, live: c, t, null-after and d records land exactly 2 typed rows"
    requirement: JSONP-05
    verification:
      - kind: integration
        ref: "ExplicitJsonInsertPathLiveTest#metadataFreeRouteLandsCdcRecordsAndSkipsEmptyOnes"
        status: pass
    human_judgment: false
  - id: D5
    description: "Encodings produce the value RowBinary would store, for each D-04 type, against a live server"
    requirement: JSONP-06
    verification: []
    human_judgment: true
    rationale: "The RowBinary-vs-JSON parity matrix is plan 13-02; this plan byte-pins the encodings only"

duration: 10 min
completed: 2026-10-08
status: complete
---

# Phase 13 Plan 01: Metadata-free JSON route routing and logical-type encoding Summary

**Struct and Debezium CDC records now reach the JSON writer under an explicit insertFormat=json (R-01 fixed by routing), through a new byte-based, schema-aware MetadataFreeJsonWriter that encodes Timestamp as decimal seconds, Date as epoch days, and bytes raw instead of Jackson's wrong defaults.**

## Performance

- **Duration:** about 10 min (start approximated at 16:50Z; the start timestamp was not captured at plan entry)
- **Started:** 2026-10-08T16:50:00Z
- **Completed:** 2026-10-08T17:02:00Z
- **Tasks:** 2
- **Files modified:** 7 (2 created, 5 modified)

## Accomplishments

- R-01 closed: `doInsertBatch` sends SCHEMA and DEBEZIUM_CDC records to `doInsertJson` when `csc.isBypassRowBinary() || shouldSkipTableMetadata()`. The former silent-loss pin now reads back all 5 rows by `SELECT id, hex(str)`, including `Grüße ✓` byte-exact.
- `MetadataFreeJsonWriter` writes one JSONEachRow object per record from a private copy of the shared mapper. Logical types convert at every depth, top-level nulls are omitted, nested nulls are explicit.
- Empty CDC records (op=t, null before/after) are skipped on this route; a live batch of c, t, null-after and d lands exactly 2 typed rows and no id-0 all-defaults row. A mutation run (skip disabled) failed that test with `expected: <2> but was: <4>`, so the test does exercise D-19.
- `DataJson.OBJECT_MAPPER` is untouched; a guard test proves it still writes epoch milliseconds for `java.util.Date` and base64 for `byte[]`.

## Task Commits

1. **Task 1: routing, writer skeleton, flipped R-01 test (tracer)** - `3e18e8e8` (fix)
2. **Task 2: logical-type encodings, CDC row, empty-CDC skip, live CDC test** - `d3f6d6fb` (feat)

**Plan metadata:** recorded in the docs commit that follows this file.

Tracer gate: interactive/auto both resolve to re-running the tracer `<verify>` (automated-only, `end-of-phase` mode). It passed (26/26 on the two classes, `testUnit` green, invariants PASS), so the log line was "Tracer verified end-to-end - expanding".

## RED evidence

**Task 1 (routing), run before the routing line changed** (`MetadataFreeJsonInsertUnitTest`, 20 tests, 2 failures, 0 errors):

```
JSONP-05: SCHEMA + insertFormat=json -> JSON | AssertionFailedError: SCHEMA + insertFormat=json -> JSON: doInsertJson calls ==> expected: <1> but was: <0>
JSONP-05: DEBEZIUM_CDC + insertFormat=json -> JSON | AssertionFailedError: DEBEZIUM_CDC + insertFormat=json -> JSON: doInsertJson calls ==> expected: <1> but was: <0>
```

The other five JSONP-05 cases passed, as they should (they describe behaviour that must not change). Semantic assessment: the target test executed, and the two failing cases reach `doInsertRawBinary` instead of `doInsertJson`, which is exactly R-01's routing fault.

**Task 2 (encodings), run with a temporary Jackson-defaults `cdcRow` stub so the class compiled** (`MetadataFreeJsonWriterTest`, 33 tests, 18 failures, 0 errors). Representative messages:

```
Timestamp 1704067200123 ms  expected: <{"ts":1704067200.123}> but was: <{"ts":1704067200123}>
Date 1704067200000 ms       expected: <{"d":19723}> but was: <{"d":1704067200000}>
byte[] raw bytes            expected: <{"b":"abc"}> but was: <{"b":"YWJj"}>
quote/backslash escaped     expected: <{"b":"a\"b\\c"}> but was: <{"b":"YSJiXGM="}>
CDC io.debezium.time.Date   expected: <{"d":19723}> but was: <{"d":1704060000000}>   (Europe/Riga, local-midnight millis)
ByteBuffer                  array lengths differ, expected: <11> but was: <12>
```

Failing groups: Timestamp (4), Date (3), bytes (5), ByteBuffer, nesting, CDC Timestamp/Date (3)/MicroTimestamp, and the `_version`/null-Data omission. Passing in RED, as they pin existing behaviour: Time, Decimal (5), empty bytes, nested nulls, top-level null and schema order, determinism, D-05 guard, static-final check. Semantic assessment: every failure is an assertion on the planned encoding, none is a load or setup fault.

## Files Created/Modified

- `src/main/java/.../sink/db/MetadataFreeJsonWriter.java` - new route-scoped row writer (`structRow`, `cdcRow`)
- `src/main/java/.../sink/db/ClickHouseWriter.java` - routing condition, `metadataFree` branches in `writeRecordsJson`, shared `writeJsonRow` tail
- `src/main/java/.../sink/data/Data.java` - additive `getSchema()`
- `src/test/java/.../sink/db/MetadataFreeJsonWriterTest.java` - 33 byte-level pins
- `src/test/java/.../sink/db/MetadataFreeJsonInsertUnitTest.java` - `RecordingRouteWriter` and the 7-case routing matrix
- `src/test/java/.../sink/ExplicitJsonInsertPathLiveTest.java` - flipped R-01 test, live CDC test
- `.planning/scripts/fork-anchors.txt` - `shouldSkipTableMetadata()` 6 -> 8

## Decisions Made

- The writer registers its serializers in a `SimpleModule` with an explicit name. An unnamed `SimpleModule` has no type id, so duplicate-registration handling on the copied mapper cannot be relied on.
- The live CDC table declares `ts DateTime64(3, 'UTC')` (the plan said `DateTime64(3)`), so `toString(ts)` is `2024-01-01 00:00:00.123` whatever the server's default time zone.
- Unicode in test sources is written as `\u` escapes, so the assertion does not depend on the compiler's source encoding.

## Behaviour ledger

Consolidated by plan 13-08.

**L-01: R-01 routing.** With `insertFormat=json` (any letter case), SCHEMA and DEBEZIUM_CDC records are routed to the JSON writer; before, they went to RowBinary against a column-less synthetic table and were silently discarded. The change is unconditional with no opt-out flag, and it is one-way (D-08, D-05, D-06). KeeperMap backfill: offset ranges already marked AFTER_PROCESSING while R-01 was losing data are not replayed. Recovering them needs a consumer offset reset AND clearing the KeeperMap state for the affected partitions. Decisions: D-05, D-06, D-08. Pinned by `MetadataFreeJsonInsertUnitTest#schemaAndCdcRecordsRouteToJsonOnlyWhenMetadataFreeOrBypassed` and `ExplicitJsonInsertPathLiveTest#explicitJsonStructRecordWithDefaultBypassRowBinaryLandsEveryRecord`.

**L-02: encodings on the metadata-free route.** Timestamp is an unquoted decimal-seconds number with exactly three fractional digits; Date is an unquoted integer count of epoch days (`Math.floorDiv` on milliseconds); Time is its integer milliseconds; Decimal is Jackson's BigDecimal number; bytes are raw bytes inside a JSON string with only quote, backslash and control bytes escaped, never base64. A `ByteBuffer` emits its remaining bytes (position and limit untouched), whereas RowBinary's `Struct.getBytes` writes the whole backing array; this asymmetry is not reconciled because RowBinary is upstream code. Decisions: D-02, D-03, D-07. Pinned by `MetadataFreeJsonWriterTest` (timestamp, date, time, decimal, bytes, ByteBuffer and nesting methods).

**L-03: upstream bypass route unchanged.** The `bypassRowBinary=true` route without an explicit `insertFormat=json` keeps Jackson's defaults (epoch milliseconds read as seconds, base64 bytes). This is a known difference from the metadata-free route, kept so upstream bytes do not change. Decision: D-05. Pinned by `MetadataFreeJsonWriterTest#sharedMapperIsNeverMutated` and the routing case "SCHEMA + bypassRowBinary=true, insertFormat unset -> JSON".

**L-04: Date into a DateTime/DateTime64 column.** A Kafka Date written to a DateTime or DateTime64 column on this route lands its integer days as seconds (silent 1970 timestamps), because the same literal must also serve Date and Date32 columns and no column metadata exists here. RowBinary writes midnight UTC seconds. Known gap, outside the D-04 parity matrix. Decision: D-17. Pinned by `MetadataFreeJsonWriterTest#dateIsUnquotedIntegerEpochDays` (the encoding that causes it).

**L-05: CDC conversion scope.** The route converts Connect Timestamp, `io.debezium.time.Date` (via `toLocalDate().toEpochDay()`, time-zone independent) and `io.debezium.time.MicroTimestamp` (decimal seconds, six fractional digits). `io.debezium.time.Timestamp` millisecond longs and ZonedTimestamp strings pass through unchanged, so a raw millisecond long is read by ClickHouse as seconds. Decision: D-18. Pinned by `MetadataFreeJsonWriterTest#cdcDebeziumDateIsEpochDayUnderAnyTimeZone`, `cdcMicroTimestampConvertsAndOtherDebeziumTimesPassThrough`.

**L-06: empty CDC records are skipped.** A CDC record whose converted map is empty (op=t, or a null after/before struct) writes nothing on this route, so no all-defaults row is stored; a batch made only of such records sends an empty body, which the server accepts as a zero-row insert. A DEBUG line names only topic, partition and offset. Decisions: D-06, D-19. Pinned by `ExplicitJsonInsertPathLiveTest#metadataFreeRouteLandsCdcRecordsAndSkipsEmptyOnes` (mutation-checked).

**L-07: nested null semantics.** On this route, Map null values and Array null elements are sent as explicit JSON null, which ClickHouse coerces to the element default in non-nullable slots; nested Struct null fields are explicit null; top-level null fields are omitted. RowBinary rejects the same nested nulls after plan 13-03, so this is a cross-route asymmetry. Decision: D-01 (Map(K, Nullable(V)) parity). Pinned by `MetadataFreeJsonWriterTest#nestedNullsAreExplicit` and `topLevelNullFieldIsOmittedAndSchemaOrderIsKept`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Temporary `cdcRow` stub during the Task 2 RED run**
- **Found during:** Task 2 step 1
- **Issue:** The test class calls `MetadataFreeJsonWriter.cdcRow`, which the plan adds only in step 4, so the class would not compile and a compile error is an invalid RED.
- **Fix:** Added a Jackson-defaults stub for the RED run, then replaced it with the real implementation before the commit. The stub never reached a commit.
- **Files modified:** `MetadataFreeJsonWriter.java`
- **Verification:** the RED run failed 18 tests on planned assertions; final run 33/33.

**2. [Rule 3 - Tightening] `ts DateTime64(3, 'UTC')` in the live CDC table**
- **Found during:** Task 2 step 6
- **Issue:** `toString(ts)` on a column without a zone follows the server's default zone, which the test cannot control.
- **Fix:** declared the zone on the column; the read-back SQL is as the plan states.
- **Files modified:** `ExplicitJsonInsertPathLiveTest.java`

---

**Total deviations:** 2 (both test-scaffolding; no production-code deviation)
**Impact on plan:** None on behaviour or scope. Task 2 is one commit rather than a separate `test(...)` RED commit because the plan specifies a single `feat(13-01)` commit per task.

## Issues Encountered

- `spotlessCheck` reports violations only in two pre-existing integration test files (`ClickHouseCloudTest`, `ClickHouseSinkConnectorIntegrationTest`), neither touched by this plan; no tree-wide `spotlessApply` was run (Phase 9 D-13).
- The anchor row for `shouldSkipTableMetadata()` moved 6 -> 8 in the Task 1 commit (routing line and `metadataFree` line); Task 2 added no further such line.

## Known Stubs

None. `MetadataFreeJsonWriter` contains no placeholder; the temporary RED stub was removed before commit.

## Threat Flags

None. No new endpoint, auth path or trust boundary beyond the plan's threat model. T-13-01 to T-13-04 are mitigated as planned: `writeUTF8String` only (no `writeRawUTF8String`/`writeBinary`), payload logging at TRACE only in `writeJsonRow`, DEBUG skip log names topic, partition and offset only, and `MetadataFreeJsonWriter` has only `static final` fields (asserted by test).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 13-02 can build the RowBinary-vs-JSON parity matrix on `MetadataFreeJsonWriter` and the flipped route.
- Phase 14 BLD-11 must re-run `MetadataFreeJsonWriterTest` after the Jackson bump (RESEARCH A4: `writeUTF8String` behaviour).
- ROLL-01 (Phase 15) should carry L-01's KeeperMap backfill caveat.

## Self-Check: PASSED

- Created files exist: `MetadataFreeJsonWriter.java`, `MetadataFreeJsonWriterTest.java`.
- Commits `3e18e8e8` and `d3f6d6fb` are ancestors of HEAD; `check evaluation-scope --plan 13-01 --commits-only` returns both.
- Acceptance criteria re-run: routing grep 1, `OBJECT_MAPPER.copy()` 1, no `OBJECT_MAPPER.(register|configure|set)`, old test name and banner absent, anchor row equals measured count (8), `getSchema()` 1, no mutable static field, no `date_time_input_format`, `serverSetting` still 3.
- Plan verification: four test classes 76/76, `testUnit` 382 tests with 0 failures (2 pre-existing skips), `verify-merge-invariants.sh` result=PASS after each commit.

---
*Phase: 13-fork-hardening-on-the-merged-tree*
*Completed: 2026-10-08*
