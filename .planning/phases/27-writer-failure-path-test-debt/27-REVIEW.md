---
phase: 27-writer-failure-path-test-debt
reviewed: 2026-10-23T00:00:00Z
depth: standard
files_reviewed: 20
files_reviewed_list:
  - CHANGELOG.md
  - docs/FORK-DELTA.md
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/ParentShape.java
  - src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java
  - src/main/java/com/clickhouse/kafka/connect/util/WithheldMessageException.java
  - src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java
  - src/test/java/com/clickhouse/kafka/connect/ci/SourcePlanningIdGuardUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonAsStringKeyResidualLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/WriterFailureClassificationUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterColumnContextTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java
findings:
  critical: 0
  warning: 1
  info: 3
  total: 4
status: issues_found
---

# Phase 27: Code Review Report

**Reviewed:** 2026-10-23
**Depth:** standard
**Files Reviewed:** 20
**Status:** issues_found

## Summary

Reviewed `git diff 4ea07e9d..HEAD` for every listed file. Verified that the changes in `HeaderToValue`, `ParentShape`, `DeclaredNullability`, `DescribeTableBuilder`, `ClickHouseHelperClient`, `WithheldMessageException` and `SinkTaskStatistics` are comment and Javadoc only (no code token changes in the diffs). In `ClickHouseWriter` the behavioural changes are the Jackson exclusion in `passesThroughUnchanged`, and the extraction of `sourceTypeName`, `valueClassName` and `WRITE_TYPE_MISMATCH_FAILURE_FORMAT`. The extraction is text-identical to the replaced inline code, which the new `classCastFailureTextIsPinnedExactly` test pins. In `Column`, an enum lookup miss now throws `IllegalArgumentException` where it used to return null and fail later as a `NullPointerException`; the only production caller is the `Enum8`/`Enum16` case in `doWritePrimitive`, and its generic catch converts the exception to a value-free `DataException`.

I traced the Jackson exclusion through the Map, Array and Tuple lambdas. A Jackson exception thrown by the JSON case is converted to a `DataException` at the innermost `doWriteColValue`. The outer `RuntimeException(e)` wrappers only ever see non-Jackson `IOException`s, so the exclusion works on every currently reachable path. A grep of `src/main` for planning identifiers (the `SourcePlanningIdGuardUnitTest` pattern plus extra prose forms) returns no hits. `useClientV2Calls` has no remaining callers. No blockers found.

## Warnings

### WR-01: Unknown-enum-label failure is not actionable for the operator, and the new column-named messages never reach any output

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java:531-534`
**Issue:** `convertEnumValues` now throws `IllegalArgumentException("Enum label not found for column <name>")`. That exception is not a `ConnectException` and not an exact `RuntimeException`, so `passesThroughUnchanged` returns false. The generic catch then wraps it in a `DataException` whose cause is `WithheldMessageException.of(e)`, and that copy withholds the message. What an operator sees in Connect status, the DLQ stack trace and the logs is `Failed to write column \`c\`: ... ClickHouse type Enum8: java.lang.IllegalArgumentException`. Nothing says the label is not one of the column's labels. This is a value-free message that is not actionable, which AGENTS.md's "Error messages must be clear, actionable" rule disallows. The new `ENUM_LABEL_NOT_FOUND_FORMAT` text is effectively dead on the writer path, because the cause message is withheld by design. `WriterValueFreeFailureUnitTest.unknownEnumLabelIsALookupMissWithoutTheLabel` pins this opaque output, so it is now a contract.
**Fix:** Throw a `ConnectException` subtype (for example `DataException`) from `convertEnumValues`, so the value-free column-named text passes through `passesThroughUnchanged` unchanged and is visible:
```java
if (code == null) {
    throw new DataException(String.format(ENUM_LABEL_NOT_FOUND_FORMAT, name)); // no label in the text
}
```
Then update `ColumnTest`, `WriterValueFreeFailureUnitTest` and the CHANGELOG and FORK-DELTA wording to match. If the `IllegalArgumentException` contract is kept deliberately (WRT-18), at least add the words "label not in the column's enum values" to `WRITE_VALUE_CONVERSION_FAILURE_FORMAT`'s enum case, or drop the unused message text.

## Info

### IN-01: `passesThroughUnchanged` short-circuits before the Jackson check, so the exclusion is a property of the call sites and not of the predicate

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:654-664`
**Issue:** The first `if` returns true for any `ConnectException` or exact `RuntimeException` before the chain walk. The Map, Array and Tuple lambdas wrap with `new RuntimeException(e)` (lines 763, 791, 835), and the message of that wrapper is `e.toString()`. If a Jackson exception ever reached one of those lambdas, which is not possible today because the innermost `doWriteColValue` converts it first, the wrapper would pass through with the Jackson message, which can quote a Map key. The Javadoc says "a Jackson link never counts", which holds only for the chain walk. Nothing tests a `RuntimeException(JsonMappingException)` or `DataException(msg, JsonMappingException)`.
**Fix:** Either note in the Javadoc that the exclusion relies on Jackson being converted at the innermost write, or add a pinning test for the wrapped-Jackson shape so a later refactor of the lambdas cannot silently reopen the leak.

### IN-02: `JsonAsStringKeyResidualLiveTest` name and Javadoc intro no longer describe what it pins

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/JsonAsStringKeyResidualLiveTest.java:34-43`
**Issue:** The test now pins that the residual is closed (the Map key reaches nothing), yet the class is still called `...Residual...`. FORK-DELTA still names the test under the "residual" heading.
**Fix:** Rename the class (and update the FORK-DELTA and CHANGELOG references) to something like `JsonAsStringKeyWithheldLiveTest`, or leave it and accept the stale name.

### IN-03: `SourcePlanningIdGuardUnitTest` has a narrow scope and a broad single-letter prefix list

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/SourcePlanningIdGuardUnitTest.java:34-45`
**Issue:** The scan covers only `src/main/java`, not `src/main/resources` or `build.gradle.kts`. It resolves `Path.of("src","main","java")` against the JVM working directory, so it fails confusingly (the `MIN_SOURCE_FILES` assertion) if run from an IDE with the repo root as a different module root. The pattern uses single-letter prefixes (`D|L|R|F|A|E|T`) with `\b…-\d{1,3}\b`. A legitimate token in a future upstream sync (for example `R-1` or `A-12` in a comment) would fail `testUnit` with a message that suggests a planning leak.
**Fix:** Optional: resolve the root from a system property or `user.dir` with a clearer failure message, and document in the failure message that an upstream-sync token can be a false positive and which lines of the pattern to adjust.

---

_Reviewed: 2026-10-23_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
