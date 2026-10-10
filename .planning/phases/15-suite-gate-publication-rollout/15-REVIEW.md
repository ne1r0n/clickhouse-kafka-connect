---
phase: 15-suite-gate-publication-rollout
reviewed: 2026-10-10T10:40:00Z
depth: standard
files_reviewed: 6
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/MetadataFreeJsonTypeParityLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java
findings:
  critical: 0
  warning: 0
  info: 6
  total: 6
status: issues_found
---

# Phase 15: Code Review Report

**Reviewed:** 2026-10-10
**Depth:** standard
**Files Reviewed:** 6
**Status:** issues_found

## Summary

I reviewed the diff of the two 15-01 code commits (`981a89d6` fix D-01, `4a91b92c` feat D-02/D-15), range `672663c2..HEAD`, restricted to the six source and test files above. `.planning/scripts/fork-anchors.txt` is outside the review scope by design. I read each changed file in context (the full `MetadataFreeJsonWriter` serializer chain and both `describeTable` loops) and checked the points the plan named explicitly. I found no Critical and no Warning findings. I did not run the Gradle build; the 15-01 SUMMARY records the targeted runs, and plan 15-02 Task 2 re-runs them.

Explicit checks from the plan:

- **Non-Number values and overflow-free scaling.** Both new cases guard with `value instanceof Number` and fall through to the default rules otherwise (pinned by `nonNumberDebeziumTimestampFallsThrough`). `BigDecimal.valueOf(long, scale)` carries the unscaled long exactly, so no value can overflow or lose digits. I confirmed the scientific rendering of small `NanoTimestamp` values with a mantissa fraction against the server (`clickhouse-local`, `latest`): `1.23E-7`, `9.99E-7`, `-9.99E-7`, `-1.5E-8` read back as 123, 999, -999 and -15 ns. The committed tests pin `0E-9`, `1E-9` and `-1E-9` only; the mantissa-with-fraction form is covered by this check, not by a test (see IN-06 on test coverage of the form).
- **Nested values.** `writeValue` recurses through `ARRAY`, `MAP` and nested `STRUCT` and calls `writeLogicalValue` per element, so a Debezium `Timestamp` inside an array or struct is converted too.
- **Static warned-key set.** `ConcurrentHashMap.newKeySet().add(...)` is atomic, so two tasks racing on the same column log once. See IN-03 for growth and key shape.
- **WARN never runs per record and carries no data.** The call sits in the DESCRIBE loops only, with `database`, `table`, `column` and raw type as the four `{}` arguments. The `isSubcolumn()` check runs before the regex and the key build, so unflagged and sub-column rows pay for neither.
- **Hook placement.** In both `describeTableV1` and `describeTableV2` the hook is after the alias/materialized/ephemeral skip, after the `shouldSkipDescribeRow` structural skip, and before `fieldDescriptor.hasDefault()` and `Column.extractColumn`.
- **WARN wording against D-15.** It states that values may be stored as NULL or shifted with no error, that a null entry fails the batch with `DataException`, and points at FORK-DELTA Known costs and backlog 999.1. Plan 15-06 owes the Known costs entry it points at.
- **Fork-anchor strings.** `"io.debezium.time.Timestamp"`, `"io.debezium.time.NanoTimestamp"` and `static boolean hasNullableEnumOrTuple(` each occur exactly once in their file; no new comment repeats an anchored string.
- **Renamed unit test.** `cdcDebeziumTimestampsAreDecimalSecondsAndZonedTimestampPassesThrough` still asserts MicroTimestamp (`micro`) and ZonedTimestamp (`zoned`) and gains `millis` and `nanos` with corrected bytes. The old assertion was tightened, not deleted or weakened, and the comment says why.

## Info

### IN-01: The WARN fires on routes where the defect cannot occur

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:490-527`
**Issue:** Table discovery has no knowledge of the insert route. `ClickHouseWriter.doInsertBatch` writes `SCHEMA_LESS` and `STRING_SCHEMA` records through JSON, and `SCHEMA`/`DEBEZIUM_CDC` records through JSON when `bypassRowBinary=true`. Only the RowBinary route omits the null marker. A deployment on those routes with a `Nullable(Enum8)` column still gets the unqualified sentence "The connector writes such slots without their null marker". This follows D-15 as locked (the WARN fires for any such column and cannot see the route), so it is a precision gap in the text, not a deviation.
**Fix:** Deferred. Qualifying the text ("on the RowBinary insert route") is a wording decision against D-15; plumbing the route into `ClickHouseHelperClient` needs edits in `ClickHouseWriter`, outside the D-04 carve-out. FORK-DELTA Known costs (plan 15-06) can state the route restriction.

### IN-02: The V1 describe hook is not exercised by the V2-only target run

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java:395-434`
**Issue:** The live wiring pin goes through `chc.describeTable`, which dispatches on `useClientV2`. With the project's V2 target only `describeTableV2` is run; the 15-01 mutation check also removed the hook from the V2 loop only. A deletion of the hook from `describeTableV1` would pass the default run and fail only under `CLIENT_VERSION=V1`.
**Fix:** Deferred. The suite target is client V2 only (project constraint), and the V1 test run is outside this milestone.

### IN-03: The warned-key set never evicts and its key is delimiter-ambiguous

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:497-518`
**Issue:** `WARNED_NULLABLE_ENUM_OR_TUPLE` lives for the process and only grows. Growth is bounded by distinct flagged (database, table, column, raw type) tuples, so it is a few hundred bytes per flagged column in practice. The key joins its parts with `.` and `:`, so `("a.b","c")` and `("a","b.c")` collide; the consequence is one suppressed WARN, in a case that needs both names to contain dots.
**Fix:** Deferred. No behavioural risk worth a change on a fork that rebases on upstream; a key record or `\u0000` separator would remove the collision if it ever matters.

### IN-04: The WARN wording is not pinned by a test

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java:264-324`
**Issue:** The tests pin the predicate, the once-per-key return value and the live registration. None asserts any text of `NULLABLE_ENUM_OR_TUPLE_WARNING`, so the D-15 wording (NULL or shifted, `DataException`, 999.1) can change silently. The 15-01 SUMMARY already marks the wording as a human-judgement deliverable (D3).
**Fix:** Deferred. A text pin would need either a log appender or widening the constant's visibility; the wording is a reviewed-by-reading deliverable.

### IN-05: New constants sit in the middle of the class

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:490-499`
**Issue:** The pattern, the message and the dedup set are declared between `shouldSkipDescribeRow` and `describeTableV1`, while the class keeps its other constants (for example `NETWORK_COMPRESSION_METHOD`, `LZ4`) near the top. Style only.
**Fix:** Deferred. Moving them adds diff churn against the fork's insertion point for no behaviour change, against the rebasability constraint.

### IN-06: Edge-value pins cover the mantissa-less scientific form only

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java:325-336`
**Issue:** `debeziumTimestampEdgeValuesAreExactDecimalSeconds` pins `0E-9`, `1E-9` and `-1E-9`. Values with magnitude 2 to 999 ns render as `1.23E-7`-style numbers, which the unit tests do not pin and no live cell reads back. A server-side change in how `DateTime64` parses exponent numbers would be caught only for the mantissa-less form. The server accepts the forms today (verified in the Summary above). `value.longValue()` on a non-`Long` `Number` would also truncate, as the existing `Timestamp` and `MicroTimestamp` cases do; the Connect `INT64` contract makes a `Long` the only reachable type.
**Fix:** Deferred. An additional `@CsvSource` row (for example `io.debezium.time.NanoTimestamp, 123, 1.23E-7`) is possible later and would only add coverage.

---

_Reviewed: 2026-10-10_
_Reviewer: Claude (gsd-code-reviewer, run inline by the plan executor)_
_Depth: standard_
