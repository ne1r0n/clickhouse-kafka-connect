---
phase: 13-fork-hardening-on-the-merged-tree
plan: "06"
subsystem: testing
tags: [junit5, testcontainers, kafka-connect-smt, json-column, test-lifecycle]

requires:
  - phase: 13-fork-hardening-on-the-merged-tree
    provides: "13-07 RowSnapshots.select typed read-back fixture"
provides:
  - "DEBT-01: HeaderToValue behaviour pinned for a pre-existing non-STRING target field on both paths"
  - "DEBT-02: JsonColumnTest reads all 10000 rows back and asserts nested_str, nested_int and nested_bytes per configuration"
  - "DEBT-03: both ClickHouseSinkTaskJsonInsertFormatTest tests use try/finally stop + dropTable"
affects: [13-08, 15]

actuals:
  tokens: 3150
  tasks: 3
  commits: 3
plan_head_before: efffcf5f3541677bd6579cd8a79e010c77b1fee9
plan_head_after: b3f78bc40ca514ec6f4553b4b90b776c700a8b04

tech-stack:
  added: []
  patterns:
    - "Configuration-dependent expected rendering expressed as a small private enum (BytesRendering) passed into a shared runner, so assertion code is written once"
    - "Behaviour pins on unsupported input assert the wrapping DataException plus the cause type and the field name, not the whole Kafka message"

key-files:
  created: []
  modified:
    - src/test/java/transforms/HeaderToValueTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java

key-decisions:
  - "HeaderToValue main code is untouched (D-15); the non-STRING target failure on the schema path is pinned as current behaviour, the fix stays a deferred idea"
  - "JsonColumnTest.compareAllConfigurations (still @Disabled) derives BytesRendering from output.json.library so it keeps compiling and stays correct if enabled"

patterns-established:
  - "Teeth check for GREEN-on-first-run pins: invert one expectation in the working tree, see RED, revert, never commit the inversion"

requirements-completed: [DEBT-01, DEBT-02, DEBT-03]

coverage:
  - id: D1
    description: "HeaderToValue schema path throws DataException(Failed to transform record) with a DataException cause naming date_date when fields names an existing INT32 field and the header is present (including a numeric-looking String header); absent header keeps the INT32 value and type"
    requirement: DEBT-01
    verification:
      - kind: unit
        ref: "src/test/java/transforms/HeaderToValueTest.java#DEBT-01 (3 schema-path tests)"
        status: pass
    human_judgment: false
  - id: D2
    description: "HeaderToValue schemaless path replaces the INT32 value with the header's String text, including an Integer header landing as \"42\""
    requirement: DEBT-01
    verification:
      - kind: unit
        ref: "src/test/java/transforms/HeaderToValueTest.java#DEBT-01 (2 schemaless-path tests)"
        status: pass
    human_judgment: false
  - id: D3
    description: "JsonColumnTest asserts per-row json_data.nested_str, nested_int, nested_bytes value and dynamic type for all six configurations against live ClickHouse"
    requirement: DEBT-02
    verification:
      - kind: integration
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun --tests '*JsonColumnTest' (6 pass, 1 @Disabled skipped)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Both ClickHouseSinkTaskJsonInsertFormatTest tests stop the task and drop the table in a finally block with unchanged assertions"
    requirement: DEBT-03
    verification:
      - kind: integration
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun --tests '*ClickHouseSinkTaskJsonInsertFormatTest' (2 pass); testUnit green"
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-10-08
status: complete
---

# Phase 13 Plan 06: Test-only debt closure Summary

**HeaderToValue's non-STRING target behaviour pinned on both paths, JsonColumnTest now proves every payload value by typed read-back for all six configurations, and both JSON insert-format tests release their MBean and table in try/finally.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-10-08T17:41:50Z (plan clock; reading began a little earlier)
- **Completed:** 2026-10-08T17:45:00Z
- **Tasks:** 3
- **Files modified:** 3 (all test sources; no main-code file touched)

## Accomplishments

- DEBT-01: five new `DEBT-01:` tests in `HeaderToValueTest` (29 tests, was 24, all green). Observed Kafka message pinned only as a substring: `Invalid Java object for schema with type INT32: class java.lang.String for field: "date_date"`; the assertions check the `DataException` cause type plus `contains("date_date")`.
- DEBT-02: `JsonColumnTest` keeps its row-count assertion and adds a read-back of all 10000 rows per configuration. Observed and pinned: `nested_bytes` is a `String` holding the Base64 text for the four non-Gson configurations, and an `Array(Nullable(Int64))` rendered as `[` signed decimals `]` (128 bytes from `new Random(42)`) for both Gson configurations. This matches RESEARCH's observation, with the element type `Nullable(Int64)` tolerated by the `startsWith("Array(")` check.
- DEBT-03: `explicitJsonInsertSkipsTableCheckOnStart` (start still before `createTable`) and `explicitJsonInsertIgnoresTargetTableFilter` now run start/put/assertions inside `try`, with `chst.stop()` then `dropTable` in `finally`. `assertEquals(` count is unchanged (8 before, 8 after) and the class Javadoc records the lifecycle.

## Task Commits

1. **Task 1: DEBT-01 HeaderToValue non-STRING pins** - `ec1ad3f4` (test)
2. **Task 2: DEBT-02 JsonColumnTest typed read-back** - `b6d5fc9d` (test)
3. **Task 3: DEBT-03 try/finally lifecycle** - `b3f78bc4` (test)

**Plan metadata:** committed separately (docs: complete plan)

## Behaviour ledger

13-06: test-only, no behaviour change.

## Teeth checks (not committed)

- Task 1: changed `contains("date_date")` to `contains("NOT_THERE")` in the working tree; the two schema-path throw tests went RED (27 of 29 passing) and printed the real cause message above; reverted before commit.
- Task 2: flipped `testBytesNoBase64JsonWithGson` to `BASE64_STRING`; it went RED with `nested_bytes dynamic type of row 0 ==> expected: <String> but was: <Array(Nullable(Int64))>`; reverted before commit.

## Files Created/Modified

- `src/test/java/transforms/HeaderToValueTest.java` - five DEBT-01 tests plus two private helpers (`configuredForDateDate`, `assertSchemaPathRejectsStringForInt32`)
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java` - private enum `BytesRendering`, `runJsonTest` parameter, `assertPayloadValues` read-back, try/finally around the sink task
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java` - try/finally lifecycle in both tests and a Javadoc sentence

## Decisions Made

- Followed D-15: no change to `HeaderToValue`; the schema path throwing for a non-STRING target is documented current behaviour.
- The `@Disabled` benchmark `compareAllConfigurations` calls `runJsonTest` with a rendering derived from `output.json.library`, because the signature gained a parameter and existing test methods are never deleted.

## Deviations from Plan

None - plan executed exactly as written.

Minor notes, not deviations: the DEBT-01 pins were written as two shared private helpers rather than five fully inline bodies (DRY, per AGENTS.md); the numeric-looking-String test reuses the same helper as the first schema-path test.

## Issues Encountered

None. `testUnit` is green (423 tests, 421 passed, 2 skipped, 0 failed).

## Known Stubs

None.

## Threat Flags

None. T-13-15 (test isolation) is mitigated by the DEBT-03 try/finally.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Plan 13-06 is complete. No main-code changes, so no coupling to the remaining Phase 13 plans.

## Self-Check: PASSED

- FOUND: src/test/java/transforms/HeaderToValueTest.java
- FOUND: src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java
- FOUND: src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java
- FOUND commits: ec1ad3f4, b6d5fc9d, b3f78bc4 (3 measured via rev-list from plan_head_before)
- Commit ec1ad3f4 touches only HeaderToValueTest.java (no main-code change, D-15)

---
*Phase: 13-fork-hardening-on-the-merged-tree*
*Completed: 2026-10-08*
