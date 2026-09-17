---
phase: 03-table-metadata-json-dynamic-types
plan: 03
subsystem: database
tags: [clickhouse, rowbinary, json, dynamic, error-handling, testing]

# Dependency graph
requires:
  - phase: 03-table-metadata-json-dynamic-types
    provides: "Plan 03-02's bypassRowBinary Struct-record route proof, reused here as the positive-control shape for the escape hatch this plan's message advertises"
provides:
  - "ClickHouseWriter.doWriteColValue's default branch and validateDataSchema's JSON/DYNAMIC/UNKNOWN case both throw an actionable DataException naming the column, its type, and the insertFormat=json / bypassRowBinary=true escape hatches, sharing one private message-builder helper"
  - "RowBinaryUnsupportedTypeGateTest — Docker-free pins on both gates, including the zero-bytes-written stream-desync proof and a privacy assertion that the message never contains the record's field value"
  - "RowBinaryFailFastLiveTest — ClickHouse-backed proof that a JSON-column Struct-schema insert fails with the actionable message and leaves zero rows under the real production default path, plus the bypassRowBinary=true positive control"
affects: []

# Actuals (#2632)
actuals:
  tokens: 4216
  tasks: 3
  commits: 3
  plan_head_before: be6df17912478c3763683ebb2feb452fd1aa8668

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Shared message-builder helper: doWriteColValue's default branch and validateDataSchema's new case both call one private buildRowBinaryUnsupportedTypeMessage(columnName, columnType) method, so the two gates cannot drift apart in wording (verified: exactly one occurrence of the insertFormat=json literal in the whole file, outside comments)."
    - "Live fail-fast test shape: create the JSON-column table before start(), drive a Struct-schema SinkRecord through ClickHouseSinkTask.put() under the real default config (no bypass flags), assertThrows + walk getCause() to the root for content, then assert countRows == 0. A second test with bypassRowBinary=true against a fresh table proves the advertised escape hatch actually works."
    - "ClickHouse's JSON type requires an object at the root via JSONEachRow (bypassRowBinary route) — a Struct field must be a nested Map(String, Map(String, Int32)) shape, not a raw String, or the insert fails with 'JSON object should start with {' unrelated to the behavior under test (same finding as plan 03-02, rediscovered and fixed live this session)."

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryFailFastLiveTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java

key-decisions:
  - "Task 1 shipped the throw inline (D-02, D-03) exactly as instructed, then Task 2 factored it into a shared private helper alongside the new validateDataSchema case (D-04) - matching the plan's own staged instructions rather than extracting the helper prematurely in Task 1."
  - "Fixed a self-introduced bug in the live test's own Struct fixture (not a production/SUT bug): the first draft used a raw String value for json_col in the bypassRowBinary positive-control test, which ClickHouse's JSON type rejects at the JSONEachRow boundary ('JSON object should start with {'). Replaced with the nested Map(String, Map(String, Int32)) shape already proven by plan 03-02's JsonDynamicRoundTripTest. Caught before any commit existed; git status --porcelain src/main stayed empty throughout, so no Rule 1-4 deviation classification applies."

requirements-completed: [TYPE-06]

coverage:
  - id: D1
    description: "TYPE-06 (writer gate): doWriteColValue's default branch throws an actionable DataException for every Type reaching it (JSON, DYNAMIC, UNKNOWN), naming the column, its type, and both insertFormat=json/bypassRowBinary=true escape hatches, and writes zero bytes to the stream before throwing"
    requirement: "TYPE-06"
    verification:
      - kind: unit
        ref: "RowBinaryUnsupportedTypeGateTest#doWriteColValueThrowsActionableExceptionForJsonColumnAndWritesNothing"
        status: pass
      - kind: unit
        ref: "RowBinaryUnsupportedTypeGateTest#doWriteColValueThrowsForEveryUnsupportedType (parameterized: JSON, DYNAMIC, UNKNOWN)"
        status: pass
    human_judgment: false
  - id: D2
    description: "TYPE-06 (validation gate): validateDataSchema throws the same shared, actionable message for a non-nullable, non-defaulted JSON/DYNAMIC/UNKNOWN root column instead of silently flipping validSchema to false - the gate that fires FIRST under default config - and the message never contains the record's field value"
    requirement: "TYPE-06"
    verification:
      - kind: unit
        ref: "RowBinaryUnsupportedTypeGateTest#validateDataSchemaThrowsForEveryUnsupportedTypeAndNeverLeaksTheValue (parameterized: JSON, DYNAMIC, UNKNOWN)"
        status: pass
    human_judgment: false
  - id: D3
    description: "TYPE-06 (live, no silent data loss): a Struct-schema record inserted into a JSON-column table through ClickHouseSinkTask.put() under the real production default path (no bypassRowBinary, no bypassSchemaValidation) fails with a message containing insertFormat=json somewhere in the cause chain, and countRows is 0 afterwards"
    requirement: "TYPE-06"
    verification:
      - kind: integration
        ref: "RowBinaryFailFastLiveTest#putFailsWithActionableMessageAndLeavesZeroRows"
        status: pass
    human_judgment: false
  - id: D4
    description: "TYPE-06 (escape hatch proof): the identical insert succeeds and the row lands once bypassRowBinary=true is set, proving the error message's advertised escape hatch is real"
    requirement: "TYPE-06"
    verification:
      - kind: integration
        ref: "RowBinaryFailFastLiveTest#bypassRowBinaryEscapeHatchMakesTheSameInsertSucceed"
        status: pass
    human_judgment: false

# Metrics
duration: 22min
completed: 2026-09-17
status: complete
---

# Phase 3 Plan 3: RowBinary Fail-Fast for JSON/Dynamic/Unknown Types Summary

**RowBinary path now throws an actionable DataException (naming the column, its type, and the insertFormat=json/bypassRowBinary=true escape hatches) instead of silently corrupting the stream, pinned Docker-free at both internal gates and live through a full ClickHouseSinkTask.put() failure that leaves zero rows.**

## Performance

- **Duration:** 22 min
- **Started:** 2026-09-17T12:05:00Z (approximate)
- **Completed:** 2026-09-17T12:27:31Z
- **Tasks:** 3
- **Files modified:** 3 (1 modified, 2 created)

## Accomplishments
- `doWriteColValue`'s `default:` branch (`ClickHouseWriter.java`) now throws a `DataException` for every `Type` that reaches it — including `Type.UNKNOWN`, not just `JSON`/`DYNAMIC` — instead of logging and writing zero bytes into a RowBinary stream the caller keeps filling (D-02, D-03).
- `validateDataSchema` gained a `JSON`/`DYNAMIC`/`UNKNOWN` case that throws the same actionable message rather than silently returning `validSchema=false` — this is the gate that R-02's live probe confirmed fires FIRST under default config, so it's the message an operator actually hits today (D-04).
- Both gates share exactly one private message-builder helper (`buildRowBinaryUnsupportedTypeMessage`), so the wording — naming the column, its type, and both `insertFormat=json`/`bypassRowBinary=true` escape hatches — cannot drift apart (D-05). Verified: the `insertFormat=json` literal appears exactly once in the whole file, outside comments.
- `RowBinaryUnsupportedTypeGateTest` (new, Docker-free) pins both gates across `Type.JSON`, `Type.DYNAMIC`, `Type.UNKNOWN` with substring assertions on message content — never a type-only `assertThrows`, which would have passed identically before this fix — plus the zero-bytes-written stream-desync proof and a privacy assertion that the thrown message never contains the record's field value (T-03-21).
- `RowBinaryFailFastLiveTest` (new, ClickHouse-backed) proves the full stack: a Struct-schema record into a live `JSON`-column table through `ClickHouseSinkTask.put()` under the real default config fails with `insertFormat=json` somewhere in the cause chain and leaves `countRows == 0`; a second test proves the `bypassRowBinary=true` escape hatch the message advertises actually works.
- Confirmed no scope creep: the only modified file under `src/main` across all three tasks is `ClickHouseWriter.java`; the upstream `ClickHouseWriterTest.java` is byte-identical to its recorded blob (`1a7fd296533f5d929e1f2502901f9f4e6187ecb3`) and still green; the full `testUnit` suite (183 tests) and the full `db`-package + live-test slice (68 tests) both pass.

## Task Commits

Each task was committed atomically:

1. **Task 1: End-to-end slice — the doWriteColValue default-branch throw, proven Docker-free with an actionable message and zero bytes written** - `efec319` (test)
2. **Task 2: The gate that actually fires first — validateDataSchema speaks the same message for JSON, DYNAMIC and UNKNOWN** - `0d51354` (test)
3. **Task 3: Live fail-fast — a JSON table, a Struct record, zero rows after the failure, and the advertised escape hatch actually working** - `c27307d` (test)

_Note: Task 1 is `type="tracer"` — verified end-to-end before Task 2 began, per the tracer feedback gate (row 3: interactive, `end-of-phase`, automated-only verify → re-run and continue, no checkpoint)._

## Files Created/Modified
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` - Added the `doWriteColValue` default-branch throw, the `validateDataSchema` JSON/DYNAMIC/UNKNOWN case, and the shared private message-builder helper
- `src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java` - New Docker-free unit class, 7 tests pinning both gates across the three unsupported types plus the zero-bytes and no-payload-leak proofs
- `src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryFailFastLiveTest.java` - New ClickHouse-backed live class, 2 tests proving the live failure + zero-rows and the bypassRowBinary escape hatch

## Decisions Made
- Task 1 shipped the throw inline exactly as the plan instructed, deferring the shared-helper extraction to Task 2 alongside the new `validateDataSchema` case — matches the plan's staged design rather than front-loading the refactor.
- No other implementation decisions beyond the plan's explicit instructions; D-01's five-item fix list already fully specified this plan's two touch points.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug in this plan's own new test code] Positive-control test used a raw String for a JSON column value, which ClickHouse's JSON type rejects**
- **Found during:** Task 3 (live fail-fast test, `bypassRowBinaryEscapeHatchMakesTheSameInsertSucceed`)
- **Issue:** The first draft's Struct fixture set `json_col` to the plain string `"{\"a\":1}"`. ClickHouse's `JSON` column type requires an object at the JSONEachRow root, so the insert failed with `Code: 117. DB::Exception: JSON object should start with '{'` — an unrelated failure that would have made the positive control assert the wrong thing.
- **Fix:** Replaced the `String`-typed `json_col` field with the nested `Map(String, Map(String, Int32))` schema shape already proven live by plan 03-02's `JsonDynamicRoundTripTest`, applied to both the negative and positive test fixtures for consistency.
- **Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryFailFastLiveTest.java`
- **Verification:** Both live tests pass; `bypassRowBinaryEscapeHatchMakesTheSameInsertSucceed` inserts and reads back 1 row.
- **Committed in:** `c27307d` (part of Task 3's single commit — caught before any commit existed for this file)

---

**Total deviations:** 1 auto-fixed (1 bug, in this plan's own new test code — not a production/SUT bug; `git status --porcelain src/main` stayed empty throughout, so no Rule 1-4 production-code deviation classification applies)
**Impact on plan:** No scope creep. The fix was entirely within the new test file this plan was already creating.

## Issues Encountered
None beyond the deviation above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 3 (Table Metadata & JSON/Dynamic Types) is now complete: all three plans (03-01 table discovery helpers, 03-02 JSON/Dynamic round-trips, 03-03 this plan's RowBinary fail-fast) are summarized.
- TYPE-06 is the last requirement this phase owned; the phase's declared success criteria (all 15 requirements: FILT-01..04, DESC-01..04, TYPE-01..07) are now pinned.
- No blockers for Phase 4 (explicit JSON insert path, `jsonInsertColumns`, JMX naming, `async_insert`).

## Self-Check: PASSED

---
*Phase: 03-table-metadata-json-dynamic-types*
*Completed: 2026-09-17*
