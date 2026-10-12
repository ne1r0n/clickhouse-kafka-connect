---
phase: 19-nullable-enum-tuple-mapping
reviewed: 2026-10-13T00:00:00Z
depth: standard
files_reviewed: 14
files_reviewed_list:
  - docs/FORK-DELTA.md
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
  - src/test/java/com/clickhouse/kafka/connect/sink/NestedNullRowBinaryLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/NullableEnumRoundTripLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/NullableRoundTrip.java
  - src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/NullableEnumWriteTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
findings:
  critical: 1
  warning: 3
  info: 4
  total: 8
status: issues_found
---

# Phase 19: Code Review Report

**Reviewed:** 2026-10-13
**Depth:** standard
**Files Reviewed:** 14
**Status:** issues_found

## Summary

Phase 19 carries `Nullable` through the Enum and Tuple branches of `Column.extractColumn` and adds `DeclaredNullability`, a post-`Table` pass that overwrites `isNullable` on Tuple nodes and their leaves from the declared type string.

The pairing walk is sound for the shapes the tests cover. I checked nested Tuples, Array and Map wrappers, unnamed elements, and backtick names with spaces, commas, dots and quotes. Parse failures and structure mismatches fail open, and the all-or-none assignment list works as designed. The pass runs once per root per DESCRIBE and mutates Columns only before the `Table` is published, so there is no thread-safety problem. Logging carries metadata only, and the DESCRIBE reader uses explicit UTF-8.

One regression is a blocker. The leaf rule `parsed.isNullable()` is wrong for a leaf wrapped in `SimpleAggregateFunction`. For such an element it silently flips a correct nullable flag to non-nullable on a schema that worked before this phase. The writer then omits the null marker and the RowBinary stream misaligns.

I confirmed this by running the production `Column.extractColumn`, `Table` and `DeclaredNullability` classes on DESCRIBE rows. The type string and row shape come from ClickHouse 26.9.17, where `Tuple(a SimpleAggregateFunction(sum, Nullable(Int64)), b Nullable(Int32))` is accepted. The other findings are a fail-open gap, stale operator documentation, and test-quality issues.

## Critical Issues

### CR-01: Tuple element wrapped in SimpleAggregateFunction(Nullable(...)) is flipped to non-nullable

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java:111-116`
**Issue:** The leaf branch records `parsed.isNullable()` for the paired node. `ClickHouseColumn.of` does not put the flag on a `SimpleAggregateFunction` node. It leaves the node non-nullable and puts it on the nested column. For `SimpleAggregateFunction(sum, Nullable(Int64))` the parsed node reports `nullable=false` and `getNestedColumns().get(0)` reports `nullable=true`.

`Column.extractColumn` unwraps `SimpleAggregateFunction` and correctly yields `nullable=true`, and DESCRIBE reports `c.a` as `SimpleAggregateFunction(sum, Nullable(Int64))`. For `Tuple(a SimpleAggregateFunction(sum, Nullable(Int64)), b Nullable(Int32))`:

- Before `apply`, `c.a` is `nullable=true`, which is correct.
- After `apply` returns `true`, `c.a` is `nullable=false`. The writer then drops the null marker for that element, and every later byte in the row shifts.

Before Phase 19 this schema wrote correctly, because the tuple element nullability came from DESCRIBE. So this is a silent data-corruption regression in the fork's high-risk binary path. It only triggers for tuples that contain a `SimpleAggregateFunction` over a `Nullable` type, which ClickHouse accepts. `DeclaredNullabilityTest` and the live matrix have no case for it.

**Fix:** Unwrap `SimpleAggregateFunction` in the leaf branch the way `extractColumn` does:
```java
default:
    if (parsed.isTuple() || parsed.isArray() || parsed.isMap()) {
        return false;
    }
    into.add(new Assignment(column, leafNullable(parsed)));
    return true;

private static boolean leafNullable(ClickHouseColumn parsed) {
    ClickHouseColumn current = parsed;
    while (current.getDataType() == ClickHouseDataType.SimpleAggregateFunction
            && !current.getNestedColumns().isEmpty()) {
        current = current.getNestedColumns().get(0);
    }
    return current.isNullable();
}
```
Add a `DeclaredNullabilityTest` case for `Tuple(a SimpleAggregateFunction(sum, Nullable(Int64)), b Nullable(Int32))`. It should expect `c.a=true` and `c.b=true`. Also add cases for `LowCardinality(Nullable(String))` as a Tuple element, an Array element and a Map value. I probed `LowCardinality` and it parses correctly, but nothing pins it.

## Warnings

### WR-01: "No exception may escape" holds only for the parse; `collect` is unguarded and describeTableV1 does not catch RuntimeException

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java:86-90`, `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:507-511` and `585`
**Issue:** Only `ClickHouseColumn.of` sits inside `try/catch (RuntimeException)`. `collect` and `collectTuple` can still throw unchecked exceptions on input I did not manage to construct today: a null `Column.getType()`, a `getNestedColumns().get(n)` on an unexpected parse shape, or a future parser change.

`describeTableV2` catches `Exception` and drops the table. `describeTableV1` catches only `ClickHouseException | JsonProcessingException`, so a `RuntimeException` from the pass escapes `extractTablesMapping`. That loop calls `describeTable` for every table, so one bad column aborts discovery for the whole database. That is the failure the fail-open policy exists to prevent. I found no current trigger, so this is a robustness gap rather than a live bug.

**Fix:** Make `apply` total by wrapping the whole body:
```java
try {
    parsed = ClickHouseColumn.of(root.getName(), normalise(declaredType));
    List<Assignment> assignments = new ArrayList<>();
    if (!collect(root, parsed, assignments)) {
        LOGGER.warn(FALLBACK_WARNING, root.getName(), declaredType, STRUCTURE_MISMATCH);
        return false;
    }
    assignments.forEach(a -> a.column().setNullable(a.nullable()));
    return true;
} catch (RuntimeException e) {
    LOGGER.warn(FALLBACK_WARNING, root.getName(), declaredType, e.getClass().getSimpleName());
    return false;
}
```
Add a test that feeds a Column whose `getType()` is null and asserts `false`.

### WR-02: Operator-facing release documents still describe the removed WARN and the unfixed limitation

**File:** `docs/ROLLOUT-v1.7.0_gg1.md:159-190` (Item 4), `CHANGELOG.md:22-23`
**Issue:** Phase 19 removed the discovery WARN and fixed `Nullable(Enum)` and `Nullable(Tuple)`. `docs/FORK-DELTA.md` was updated, but the two documents operators read were not.

- Item 4 of the rollout checklist still says "Known limitation, not fixed in this release". It tells operators to expect the removed WARN, with its full text, and to change the column type or move to the JSON route for every column the query lists.
- `CHANGELOG.md` still announces "New startup WARN ... The root cause is not fixed in this release."

Operators will look for a log line that no longer exists and may rework schemas that now write correctly.

A new behaviour change is also missing from both documents, and it matters for a CDC fork run with `errors.tolerance=none`. `FORK-DELTA.md` item 1 notes that a single-field `Nullable(Tuple(a T))` whose `a` is null now throws a `DataException` naming the tuple field, where earlier releases stored a NULL tuple. Under `errors.tolerance=none` that stops the task, and it stops again on every retry because the batch is rebuilt identically. This belongs in the upgrade notes.

**Fix:** Rewrite Item 4's "Known limitation" and "Am I affected?" sections and the CHANGELOG bullet. They should say the limitation is closed and the WARN is gone. They should also note that rows written by earlier releases are not repaired, and add the new single-field-tuple `DataException`. The rollout document must not be published with the old text.

### WR-03: Control test passes on any exception

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java:673-676`
**Issue:** `controlWithBothFieldsNullableFailsToInsert` is the byte-level evidence that doubled null flags fail on the server. It uses `assertThrows(Exception.class, ...)` and only asserts `assertNotNull(failure)`. A connection problem, a missing table or a typo in the insert would also pass it. The claim recorded in `FORK-DELTA.md` ("the Tuple flag alone double-flags the elements (`ATTEMPT_TO_READ_AFTER_EOF`)") is never checked.

**Fix:** Assert the server code through the cause chain, for example `NullableRoundTrip.causeChainMessages(failure).contains("ATTEMPT_TO_READ_AFTER_EOF")` or the numeric code. Match the code the server returns for this input on the CI ClickHouse version. If the code can vary, assert that the message names a read or parse failure rather than accepting any exception.

## Info

### IN-01: Fallback WARN has no database or table context

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java:33-34`, called from `ClickHouseHelperClient.java:509`
**Issue:** The WARN names the column and raw type but not the database or table. The same column name (`payload`, `meta`) is common across tables, so the line is not actionable. AGENTS.md asks for context in error messages. The deleted WARN carried `database.table.column`, and `buildDescribedTable` already has both values. The old once-per-process dedup is also gone, so the line repeats on every re-describe of an affected table.
**Fix:** Pass a context string into `apply(root, declaredType, context)` and log it. Alternatively log from `buildDescribedTable` on a `false` return.

### IN-02: Dead STORES_WRONG machinery in the matrix test

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java:106-129`, `489-502`, `599-607`
**Issue:** No cell pins `STORES_WRONG` any more, because every `currentOutcome` is `roundTrips` or `fails`. `Expect.Kind.STORES_WRONG`, `storesWrong(...)`, the `observed` and `phantomRows` fields, `phantomRowCount` and the switch arm are all unreachable. `phantomRowCount` is also the `count()` style that the phase rules out for assertions.
**Fix:** Remove the unused kind, factories, fields and helper, or keep one cell that exercises them.

### IN-03: Test re-implements production normalisation and duplicates helpers

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/NullableRoundTrip.java:46-47` and `200-202`, `NullableTupleMatrixLiveTest.java:83-84` and `523-550`, `DeclaredNullabilityTest.java:57-84`
**Issue:** `NullableRoundTrip.normaliseDeclaredType` is a private copy of `DeclaredNullability.normalise`. The "approach" test therefore proves the copy parses, not the production code. If one changes, nothing fails. `nullabilityByPath` and `collectNullability` are duplicated almost verbatim between `DeclaredNullabilityTest` and the matrix test.
**Fix:** Make `DeclaredNullability.normalise` public, or reach it from the test, and call it from `NullableRoundTrip`. Move the Column-tree flattening into one shared test helper.

### IN-04: A Tuple Column that lacks a declared element still "applies"

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java:133-143`, pinned by `DeclaredNullabilityTest.java:215-223`
**Issue:** `collectTuple` iterates only the Column's own `tupleFields`. A declared element with no Column (for example one whose dotted name `Table` cannot route to its parent) is silently ignored and `apply` returns `true`. The test pins that as correct. The writer also emits only the fields in `tupleFields`, so the stream is short for that element, and that is a Table-pairing bug the pass cannot see. The pass already has both element lists, so it could at least log it.
**Fix:** After the loop, compare the count of paired declared elements with `elements.size()`. Log a metadata-only WARN on a shortfall. Optionally fail open, which needs a decision on whether legitimately skipped elements exist.

---

_Reviewed: 2026-10-13_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
