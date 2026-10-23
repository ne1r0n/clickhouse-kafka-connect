---
phase: 23-declared-nullability-hardening-review-closure
reviewed: 2026-10-20T00:00:00Z
depth: standard
files_reviewed: 8
files_reviewed_list:
  - docs/FORK-DELTA.md
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/NullableRoundTrip.java
  - src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/SimpleAggregateTupleRoundTripLiveTest.java
findings:
  critical: 0
  warning: 2
  info: 4
  total: 6
status: issues_found
---

# Phase 23: Code Review Report

**Reviewed:** 2026-10-20
**Depth:** standard
**Files Reviewed:** 8
**Status:** issues_found

## Summary

Reviewed the phase diff (`8385d9df..HEAD`) for `DeclaredNullability` and its tests in the context of the whole files, and cross-checked the single production call site (`ClickHouseHelperClient.buildDescribedTable`, `shouldSkipDescribeRow`, `describeTableV2`). `testUnit` for `DeclaredNullabilityTest` and `ClickHouseHelperClientForkUnitTest` passes locally. The Docker-backed live tests were not run.

I traced the new quote-aware `normalise` by hand against every captured fixture (backslash-n in backtick names, real newline in a name, Enum label, nested names, trailing backslash, Nullable root). The quote open/close arithmetic (`(run / 2) % 2 == 0` closes the quote, an odd half-run is an escaped quote) is correct under the stated premise that the JSON layer doubles every backslash. The null hardening in `apply`, `collect`, `collectTuple` and `containsTuple` is consistent (a null anywhere is a structure mismatch, a null root is a silent false). The unpaired-element pass applies assignments only after `collect` succeeds, so a root is never half-updated by a mismatch. The log calls carry metadata only (column, database, table, declared type, exception class name), which matches the project rule. The modified pre-existing tests are marked with the documented D-08 and D-19 exemptions, and their old inputs are kept as new tests.

No bugs or security defects found in production code. The findings below are one test-coverage gap on the new parsing branch, one test that copies a production loop, and smaller quality items.

## Warnings

### WR-01: The escaped-quote branch of `normalise` is not pinned by any test

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java:97-103` (tests: `DeclaredNullabilityTest.java:248-345`)
**Issue:** The new rule decides whether a backslash run before a quote character closes the quote. The branch where the quote stays open (`escaped == quote` with an odd half-run, that is an escaped backtick or an escaped single quote inside a name or label, raw run of 2, 6, ...) has no test. Every fixture with a backslash run before a quote character is the trailing-backslash case (run 4, closes). A mutation to `escaped == quote` (always close), or to `(run / 2) % 2 != 0`, passes the whole suite. Wrong behaviour here would either replace a `\n` inside a name with a space, or leave a real line break unreplaced after a name that contains a quote. Either one makes the type unparsable or mispaired, so the pass falls back to the DESCRIBE nullability. The fallback is the double-flag defect this class exists to prevent. The same applies to the outside-quote `run == 1` guard: no test has two or more backslashes followed by `n` outside quotes. The class is on the "high risk" path for byte layout, so the branch needs a pin.
**Fix:** Add Docker-free cases to `DeclaredNullabilityTest` using the escaping the server produces (SQL `\`` becomes server `\`` becomes JSON `\\``). For example, a Tuple element named ``a`b`` and a following line break, and an Enum label containing `\'`:
```java
@Test
void escapedBacktickInsideANameKeepsTheQuoteOpen() {
    // raw name: a, escaped backtick, then a literal backslash-n pair that must survive
    String raw = "Tuple(\\n    `a\\\\`\\\\\\\\nb` Int32,\\n    d String)";
    assertEquals("Tuple(     `a\\\\`\\\\\\\\nb` Int32,     d String)", DeclaredNullability.normalise(raw));
}
```
Capture one real server literal for it in `23-EVIDENCE.md` and the fixtures, like the other shapes.

### WR-02: `discoverAsDescribeTableV2` copies the production DESCRIBE loop into the test

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java:311-342`
**Issue:** The helper re-implements the control flow of `ClickHouseHelperClient.describeTableV2` (the `skippedCols` parent check, `shouldSkipDescribeRow`, `declaredTypes` capture, `numColumns` counting). It already differs from production (no alias, materialized or ephemeral branch). The 999.4 pin (`sizeNamedTupleElementDroppedByDiscoveryIsNamedInOneWarning`) therefore verifies the copy, not the shipped loop. If the production loop changes (for example the 999.4 fix that stops dropping `.size` and `.size0` rows), this test can stay green or red for reasons unrelated to production, and the test's "the 999.4 fix flips this pin" comment cannot be relied on. The same loop is already duplicated in `describeTableV1` and `describeTableV2`.
**Fix:** Extract the per-row decision in production into a package-private method (for example `describeRows(Stream<String> jsonLines)` returning the `Table`) and call it from `describeTableV1`, `describeTableV2` and the test. If the production refactor is out of scope for a fork that keeps changes small, state the duplication explicitly in the helper's Javadoc and add a live test that pins the 999.4 WARN against a real table with a `size` element.

## Info

### IN-01: `apply` Javadoc still says "all or none"

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java:135-137`
**Issue:** The method comment says nullability is overwritten "all or none". Since D-01 a Tuple with unpaired declared elements returns `true` after applying the nullability to the paired nodes only. The class comment (lines 32-37) describes the partial case correctly, so the two comments now contradict each other.
**Fix:** Reword: "Overwrites `isNullable` on the Tuple nodes and leaves that pair with `declaredType`; a structure mismatch applies nothing, while declared Tuple elements missing from the Column tree are logged and skipped."

### IN-02: Null-name check sits inside the `try` and logs a null column slot

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java:147-152`
**Issue:** `root.getName()` is a plain getter that cannot throw, so the `columnName == null` early return does not need the `try`. It logs the fallback WARN with the literal text `null` in the column slot, which a reader may take for a column named "null". The test even asserts the `column null of table` segment. This is cosmetic, but it is the one place where the WARN is not actionable.
**Fix:** Move the check above the `try`, and log a fixed placeholder such as `<unnamed>`, then update `rootWithANullNameIsAStructureMismatch` to match. Optional.

### IN-03: Live-test premise loops can pass vacuously, and lookups can NPE instead of failing with a message

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java:~830-835` (the `describeRows` premise loop), `~845` (`columns.get(...).isNullable()`), `~895` (`getAllColumnsMap().get("n.p\\\\nq.t").isNullable()`)
**Issue:** In `backslashNInTupleElementNameRoundTripsWithoutFallback` the premise `assertEquals(NULLABLE_BACKSLASH_N_NAME_RAW, row.getType(), PREMISE_MESSAGE)` runs only if a row named `CELL_COLUMN` is found; if none is, the loop does nothing and the premise is never checked. The `columns.get(...)` and `getAllColumnsMap().get(...)` lookups throw a bare `NullPointerException` when discovery drops or renames the column, which hides the real cause (a pairing regression) behind a stack trace.
**Fix:** Collect the matching row into a variable and `assertNotNull` it with a message before comparing, and wrap the map lookups in `assertNotNull(columns.get(key), key)`.

### IN-04: The unpaired-element WARN repeats at every refresh for a known, unfixed defect

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java:163-165`
**Issue:** By decision D-02 the WARN is deliberately not deduplicated. For a table that holds a `size` or `size0` Tuple element (backlog 999.4) it fires on every `DESCRIBE` for the life of the task, which includes each `TableMappingRefresher` cycle. This is intended behaviour, not a defect, but the WARN text does not say that the connector will write a short tuple, which is the actionable consequence (R-01: code 33 `CANNOT_READ_ALL_DATA`).
**Fix:** Consider appending a short consequence hint to `UNPAIRED_ELEMENT_WARNING`, for example "inserts into this column will fail until the element is renamed (backlog 999.4)". The `sentinelRootOccursInNoFixedWarnText` test already guards the constant text. Optional.

---

_Reviewed: 2026-10-20_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
