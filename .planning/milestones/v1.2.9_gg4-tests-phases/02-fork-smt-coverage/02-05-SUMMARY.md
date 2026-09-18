---
phase: 02-fork-smt-coverage
plan: 05
subsystem: testing
tags: [junit5, testcontainers, clickhouse-latest, client-v2, kafka-connect-smt, debezium, rowbinary]

# Dependency graph
requires:
  - phase: 01-test-infrastructure-green-baseline
    provides: "ClickHouseBase shared per-JVM container, MBean-leak sweep, uniqueConnectorName lifecycle"
provides:
  - "SmtSinkLandingTest — sink-level (ClickHouse `latest`, client V2) proof that TableOperationFilter, HeadersToValue, HeaderToValue and ConvertToMapOrJson land transformed records correctly through a real ClickHouseSinkTask"
  - "ClickHouseWriter MAP-branch RowBinary null-marker fix for Nullable map values"
affects: []

# Actuals (#2632)
actuals:
  tokens: 4957
  tasks: 3
  commits: 4

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Local apply-and-drop `transform()` helper mirroring the Connect worker's null-means-filtered contract, avoiding a `connect-runtime`/`TransformationChain` dependency (D-05)."
    - "MBean-safe `runSink()` helper: every test's start()/put()/stop() goes through one method with `stop()` in a `finally`, keyed by `uniqueConnectorName(TestInfo)`."
    - "Content assertions read exclusively through `ClickHouseTestHelpers.getAllRowsAsJson` (explicit UTF-8 reader) so the `windows-1252` test-JVM default never masks non-ASCII regressions; non-ASCII test data is escaped-unicode literals only."

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/SmtSinkLandingTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java

key-decisions:
  - "Approved out-of-scope fix (user decision at the Task 2 checkpoint): ClickHouseWriter's MAP branch never wrote the RowBinary null-indicator byte for a Nullable map value, unlike the sibling ARRAY branch — every insert into a `Map(String, Nullable(*))` column failed with `CANNOT_READ_ALL_DATA`. Fixed by mirroring the ARRAY branch's `writeNonNull()` guard. Scope held to exactly that one branch in that one file; actual null map values remain deliberately unhandled and stay pinned at unit level by HDRS-03 (02-02-PLAN.md)."
  - "HDRS-09's fixture intentionally carries no null-valued header, per the plan's locked prohibition — the null-map-entry contract is out of this phase's scope even after the writer fix landed, so the fixture was not widened to cover it."
  - "CONV-08's nested-Struct empty edge is modeled by an all-null-fields Struct (not a null Struct reference), matching `preserve.null.values=false` dropping only individual null fields inside `structToMap` — this lands `{}` in the JSON column with the row present, per the plan's locked contract."

patterns-established:
  - "CONV-08's `jsonFixtureRecord`/`newConvertToJsonTransform` builders are reusable if a future plan needs more `ConvertToMapOrJson` sink-level coverage."

requirements-completed: [TOF-06, HDRS-09, HDR-07, CONV-08]

coverage:
  - id: D1
    description: "TOF-06: filtered Debezium d/t operations for a listed table never reach ClickHouse, unfiltered c/u/r and an unlisted table's d all land, and the nested Debezium `source` Struct round-trips into a `Tuple(db String, table String)` column"
    requirement: "TOF-06"
    verification:
      - kind: integration
        ref: "com.clickhouse.kafka.connect.sink.SmtSinkLandingTest#tof06FilteredOperationsNeverLandUnfilteredDo"
        status: pass
    human_judgment: false
  - id: D2
    description: "HDRS-09: HeadersToValue(field=headers_map) lands Kafka headers in a Map(String, Nullable(String)) column, including the zero-header empty-map edge and an empty-string plus escaped-unicode header value"
    requirement: "HDRS-09"
    verification:
      - kind: integration
        ref: "com.clickhouse.kafka.connect.sink.SmtSinkLandingTest#hdrs09HeadersLandInMapColumn"
        status: pass
    human_judgment: false
  - id: D3
    description: "HDR-07: HeaderToValue(fields=trace_id, headers=trace-id) lands the header-derived field in its non-nullable String column with the exact header value, leaving other fields unchanged"
    requirement: "HDR-07"
    verification:
      - kind: integration
        ref: "com.clickhouse.kafka.connect.sink.SmtSinkLandingTest#hdr07HeaderDerivedFieldLandsInColumn"
        status: pass
    human_judgment: false
  - id: D4
    description: "CONV-08: ConvertToMapOrJson(output.json.enabled=true) JSON-string output routes through StringRecordConvertor/JSONEachRow, landing top-level keys in typed columns and a nested Struct in a JSON column, with the all-null-nested and non-ASCII edges proven, plus a single-record-batch sanity check"
    requirement: "CONV-08"
    verification:
      - kind: integration
        ref: "com.clickhouse.kafka.connect.sink.SmtSinkLandingTest#conv08JsonStringOutputLandsAcrossColumns"
        status: pass
      - kind: integration
        ref: "com.clickhouse.kafka.connect.sink.SmtSinkLandingTest#conv08SingleRecordBatchLandsOneRow"
        status: pass
    human_judgment: false
  - id: D5
    description: "ClickHouseWriter MAP branch writes the RowBinary null marker for Nullable map values (production fix, mirrors the ARRAY branch), and the change does not regress any pre-existing Map-column test"
    verification:
      - kind: integration
        ref: "com.clickhouse.kafka.connect.sink.SmtSinkLandingTest#hdrs09HeadersLandInMapColumn"
        status: pass
      - kind: integration
        ref: "com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskWithSchemaTest#mapTypesTest (regression re-run, full suite)"
        status: pass
    human_judgment: false

# Metrics
duration: 24min
completed: 2026-09-16
status: complete
---

# Phase 2 Plan 5: Sink-Level SMT Landing Tests Summary

**A new `SmtSinkLandingTest` class proves all four fork SMTs against ClickHouse `latest`/client V2 through the real insert path, and fixes a production RowBinary null-marker bug in `ClickHouseWriter`'s MAP branch that the HDRS-09 test tripped.**

## Performance

- **Duration:** 24 min (spanning two executor sessions across a checkpoint)
- **Started:** 2026-09-16T17:39:23Z (Task 1, prior session)
- **Completed:** 2026-09-16T18:02:57Z (Task 3, this session)
- **Tasks:** 3
- **Files modified:** 2 (`SmtSinkLandingTest.java` created, `ClickHouseWriter.java` modified)

## Accomplishments
- TOF-06: filtered Debezium `d`/`t` operations for `mydb.mytable` proven absent from ClickHouse while `c`/`u`/`r` and an unlisted table's `d` land; the nested Debezium `source` Struct round-trips into a `Tuple(db String, table String)` column.
- HDRS-09: Kafka headers land in a `Map(String, Nullable(String))` column, with the zero-header empty-map edge and an empty-string-plus-escaped-unicode header value both proven.
- HDR-07: a header-derived field lands in its non-nullable `String` column with the exact header value.
- CONV-08: `ConvertToMapOrJson` JSON-string output lands across typed columns plus a nested `JSON` column, with the all-null-nested empty edge, a non-ASCII round-trip, and a single-record-batch sanity check all proven.
- Fixed a production bug in `ClickHouseWriter`'s MAP branch: it never wrote the RowBinary null-indicator byte for `Nullable`-typed map values, so every insert into a `Map(String, Nullable(*))` column failed with `CANNOT_READ_ALL_DATA` — this is exactly the write path HDRS-09 exercises.
- Full `./gradlew test --rerun` suite is green: 216 tests, 0 failures, 7 pre-existing version-gated skips unrelated to this class.

## Task Commits

Each task was committed atomically:

1. **Task 1: End-to-end slice — sink test class, D-05 helpers, TOF-06 proven against ClickHouse `latest`** - `769f354` (test) — completed by the prior executor session
2. **Task 2: HDRS-09 headers → `Map(String, Nullable(String))`, HDR-07 header-derived column** - two commits, produced by this continuation session after the checkpoint resolved:
   - `3929ce6` (fix) — the approved out-of-scope `ClickHouseWriter` MAP-branch RowBinary null-marker fix, committed separately per the user's checkpoint decision
   - `0e250ad` (test) — the HDRS-09 and HDR-07 test methods
3. **Task 3: CONV-08 JSON-string landing, full suite green** - `11a644d` (test)

_Note: Task 2 produced two commits by design — the checkpoint resolution required the production fix to be reviewable on its own, separate from the test commit that depends on it._

## Files Created/Modified
- `src/test/java/com/clickhouse/kafka/connect/sink/SmtSinkLandingTest.java` - New sink-level test class covering TOF-06, HDRS-09, HDR-07, CONV-08 (5 test methods total)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` - MAP branch now writes the RowBinary null marker for Nullable map values before the value itself, mirroring the sibling ARRAY branch

## Decisions Made
- Approved the out-of-scope `ClickHouseWriter.java` fix (user decision at the Task 2 blocking-human checkpoint) after the executor's diagnosis — `CANNOT_READ_ALL_DATA` on every `Map(String, Nullable(String))` insert, traced to the missing null-marker write in the MAP branch — was independently verified against the source. Scope held to exactly that branch; actual null map values remain unhandled by design (pinned at unit level by HDRS-03 instead).
- Rejected weakening the HDRS-09 column to `Map(String, String)` (checkpoint Option C) — kept the literal `Map(String, Nullable(String))` DDL as locked by D-08.
- Kept the HDRS-09 fixture exactly as planned: two headers, zero headers, empty-string-plus-non-ASCII — no null-valued header, so the fixture doesn't trip the still-unhandled null-map-entry path.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 4 - Architectural, user-approved] Fixed `ClickHouseWriter`'s MAP branch to write the RowBinary null marker for Nullable map values**
- **Found during:** Task 2 (HDRS-09 test, prior session — surfaced as a `blocking-human` checkpoint since the fix touches `src/main/java/`, outside this plan's declared `files_modified` scope)
- **Issue:** `doWriteColValue()`'s `MAP` branch called `doWriteColValue(...)` for each map value with no explicit null-indicator write, unlike the sibling `ARRAY` branch (`col.getArrayType().isNullable() && v != null -> writeNonNull()`). Every insert into a `Map(String, Nullable(*))` column server-side failed with `Code: 33 ... CANNOT_READ_ALL_DATA (Bytes read: 66. Bytes expected: 98)`.
- **Fix:** Added the same `col.getMapValueType().isNullable() && mapValue != null -> BinaryStreamUtils.writeNonNull(stream)` guard before writing the map value, mirroring the ARRAY branch exactly.
- **Files modified:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`
- **Verification:** HDRS-09 sink test passes; full-suite regression re-run of the pre-existing `Map(String, ...)`-column tests (`ClickHouseSinkTaskSchemalessTest`, `ClickHouseSinkTaskSchemalessProxyTest`, `ClickHouseSinkTaskWithSchemaProxyTest`, `ClickHouseSinkTaskStringTest`, `ClickHouseSinkTaskWithSchemaTest` incl. `mapTypesTest()`, `ColumnTest`, `TableTest` — 102 tests, 99 succeeded, 0 failed, 3 pre-existing skips) shows no regression; final full `./gradlew test --rerun` (216 tests) is green.
- **Committed in:** `3929ce6` (separate commit from the test change, per the checkpoint decision, so the production fix is independently reviewable)

---

**Total deviations:** 1 auto-fixed via explicit user approval at a `blocking-human` checkpoint (architectural, Rule 4). No scope creep beyond the single approved MAP branch — actual null map values remain unhandled by design.
**Impact on plan:** Necessary for HDRS-09's `<verify>` to pass at all; the fix is a strict, minimal mirror of an existing correct pattern (the ARRAY branch) with no behavior change to any non-Nullable or non-null-value map insert.

## Issues Encountered
None beyond the documented deviation above, which was the reason for the mid-plan checkpoint pause.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All four fork SMTs (`TableOperationFilter`, `HeadersToValue`, `HeaderToValue`, `ConvertToMapOrJson`) now have both unit-level (plans 02-01..02-04) and sink-level (this plan) coverage against ClickHouse `latest` with client V2.
- The `ClickHouseWriter` MAP-branch fix is a genuine production bug fix (not test-only) — it should be called out in any release notes for this fork build, since it affects every `Map(String, Nullable(*))` column insert, not just the SMT-generated ones this plan tests directly.
- No blockers for phase completion; `./gradlew test --rerun` is green end-to-end.

## Self-Check: PASSED

- `[ -f src/test/java/com/clickhouse/kafka/connect/sink/SmtSinkLandingTest.java ]` -> FOUND
- `[ -f src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java ]` -> FOUND (modified)
- `git log --oneline --all | grep -q 769f354` -> FOUND
- `git log --oneline --all | grep -q 3929ce6` -> FOUND
- `git log --oneline --all | grep -q 0e250ad` -> FOUND
- `git log --oneline --all | grep -q 11a644d` -> FOUND
- All `<acceptance_criteria>` for Tasks 2 and 3 re-verified via the plan's own `<automated>` verify commands: pass (5/5 tests green, zero non-ASCII bytes, all four `@DisplayName` requirement-ID prefixes present, no `TransformationChain`/`bypassSchemaValidation`/`SinceClickHouseVersion`).
- Plan-level `<verification>` re-run: `docker info` exits 0; `./gradlew test --rerun --tests SmtSinkLandingTest` exits 0 with 5 tests, 0 failures/errors/skips; full `./gradlew test --rerun` exits 0 with 216 tests, 0 failed.

---
*Phase: 02-fork-smt-coverage*
*Completed: 2026-09-16*
