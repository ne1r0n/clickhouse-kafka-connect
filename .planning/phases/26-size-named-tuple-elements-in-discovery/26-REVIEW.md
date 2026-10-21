---
phase: 26-size-named-tuple-elements-in-discovery
reviewed: 2026-10-22T00:00:00Z
depth: standard
files_reviewed: 19
files_reviewed_list:
  - build.gradle.kts
  - CHANGELOG.md
  - docs/FORK-DELTA.md
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/ParentShape.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCapture.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCatalog.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureDriftLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureSnapshotUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixturesUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilderUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/NullableRoundTrip.java
  - src/test/java/com/clickhouse/kafka/connect/sink/TupleSizeNamedElementRoundTripLiveTest.java
findings:
  critical: 0
  warning: 2
  info: 8
  total: 10
status: issues_found
---

# Phase 26: Code Review Report

**Reviewed:** 2026-10-22
**Depth:** standard
**Files Reviewed:** 19
**Status:** issues_found

## Summary

Reviewed the six `src/main` files, the build task, the docs and the twelve test-side files of Phase 26 against
`git diff 4ae4ab99..HEAD`. The 95 fixture files under `src/test/resources/describe-fixtures/` were used only as
evidence (server DESCRIBE rows for `s4_a04`, `s4_a05`, `s4_o01`, `n01`, `n02`, `n03`, `x02`, `x03`, `x05`,
`x07`, `x08`).

The classifier (`DescribeTableBuilder.decide` plus `ParentShape`) is correct for every shape I traced by hand against the
committed server output. A declared element's row type always has the parent's full Array depth in wrappers, while
Array-offsets row k has only k wrappers, so `isOffsetsRow` cannot confuse the two. The cheap
`parentType.contains(segment)` shortcut only ever routes to "structural" for a parent whose type text lacks the name.
Structural twins such as `c.size0 UInt64` ahead of a declared `c.size0`, and `c.size0.size UInt64` below a declared
String element, are classified correctly. Not entering `SKIP_STRUCTURAL_SIZE` rows into `skippedCols` is what lets a
declared element's children survive. The V1 and V2 extraction preserves the old per-row order of operations.

No security problem was found. Nothing logs record values, and the new WARN carries only the parent type, the
database and table, and an exception class name. No blocker was found. The remaining weaknesses are one fail-open
residue in the classifier that tests and comments describe as "fails closed", one test-reliability coupling, and
several dead-code, stale-comment and test-hygiene items.

## Warnings

### WR-01: Unreadable-parent path is fail-open (silently re-creates the short tuple) but is documented and tested as "fails closed"

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java:200-219`, `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilderUnitTest.java:254-285`, `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixturesUnitTest.java:33`
**Issue:** When the parent's raw type is missing from `rawTypes`, or `ParentShape.parse` throws (for example a type the
client's `ClickHouseColumn.of` does not know), a bare `size` or `size0` row is `SKIP_UNDECIDABLE` and a declared Tuple
element is dropped. That is exactly the 999.4 defect: the Table registers with a short Tuple and the writer emits
wrong RowBinary bytes for every row, with only a WARN as signal. `anUnreadableParentFailsClosedWithOneMetadataOnlyWarning`
pins this outcome (`assertEquals(List.of("c.x.v"), fieldNamesOf(unreadable))`), so its name and the
"fail-closed path of the classifier" comment in `DescribeFixturesUnitTest` state the opposite of what happens. Fail-closed
in this codebase is `addRow` returning false, which makes `describeTable` return null and the table is not served. The
FORK-DELTA row is honest ("keeps the earlier behaviour"), but the CHANGELOG states "at any depth ... no longer dropped"
without this caveat. The reachable trigger is any server-printed parent type that the pinned client cannot parse and that
contains a size-named element. That includes any newer ClickHouse type the client library lacks, or a dotted element name
whose parent path is not a described row.
**Fix:** Either make the behaviour match the claim, or correct the claim.
```java
// DescribeTableBuilder.addRow, after decide(...)
if (decision == RowDecision.SKIP_UNDECIDABLE) {
    warnUnreadableParent(fieldDescriptor.getName());
    LOGGER.error("Cannot tell a declared Tuple element from a structural row for {}; ignoring table {}.{}",
            fieldDescriptor.getName(), database, tableName);
    return false; // table not served, inserts fail loudly instead of writing short tuples
}
```
Only the bare `size` and `size0` case needs this, because `size<digits>` rows are kept either way and `Table` decides. If
changing production behaviour is out of scope for the fork, rename the test to say "keeps the earlier behaviour", change
the `DescribeFixturesUnitTest` comment, and add the undecidable caveat to the CHANGELOG entry.

### WR-02: Byte-for-byte drift tests against a floating `latest` image can turn the whole suite red for reasons unrelated to the connector

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureDriftLiveTest.java:56-73`
**Issue:** 94 parameterised cases assert that the raw JSONEachRow DESCRIBE lines and the full Column snapshot of
`clickhouse/clickhouse-server:latest` equal files committed at version 26.9.19.9. Any upstream change to DESCRIBE
formatting, sub-column ordering or type printing (including cosmetic changes) fails 47 to 94 cases at once, and the fix is
a manual re-capture and diff review. The project already carries a "latest drift" memory note, and CI is the place where
this will bite first. The premise is useful, but its failure mode (hard red on the main suite) is out of proportion to
its purpose.
**Fix:** Keep the premise but make it informative rather than blocking. Options are to tag it so it runs in a separate
non-gating job or nightly, or to compare a canonicalised form (name, normalised type, `is_subcolumn`) instead of raw
bytes, and to report the server version when it fails. The unit tests over the committed fixtures already provide the
regression protection.

## Info

### IN-01: Structural `size<digits>` rows share a name with the declared element and the last registration wins

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java:169-170`, `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java:77-80`
**Issue:** For `Array(Array(Tuple(size1 ...)))` the server prints `c.size1 Array(UInt64)` (structural, kept as a Column)
and `c.size1 Array(Array(UInt8))` (declared) under one name. `columnsByName.put` and `Table.registerValidColumn` both
overwrite, so which Column serves as the parent of `c.size1.*` children depends on server row order. Today the structural
row comes first (`n01`, `s4_a10`, `x05`), so the declared one wins. If an older or newer server printed the declared row
first, Tuple-valued size-named elements at Array depth 2 or more would bind their children to the `UInt64` structural
Column and silently lose fields. I could not verify other server versions. The drift test is the only guard.
**Fix:** Make the winner order-independent in the builder, for example
`if (decision == RowDecision.KEEP_DECLARED || !columnsByName.containsKey(column.getName())) columnsByName.put(...)`,
or stop adding `KEEP_STRUCTURAL_SIZE` rows to `columns` (they are ignored by `Table` anyway). The second option changes the
committed `all` snapshot lines.

### IN-02: Production dead code kept only for tests

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:475-487`, `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/ParentShape.java:49`
**Issue:** Both `shouldSkipDescribeRow` overloads have no caller under `src/main`. `describeTableV1` and `describeTableV2`
go through `DescribeTableBuilder.addRow`. The two-argument form passes `Map.of()` as `rawTypes`, so it is a
different, weaker rule than the one production runs, and tests that call it pin behaviour production never executes.
`ParentShape.arrayDepth()` is public but referenced only in a Javadoc `{@link}`.
**Fix:** Point the tests at `DescribeTableBuilder.decide(...).skipsRow()` and delete both overloads. Make `arrayDepth()`
package-private or remove it.

### IN-03: Stale class Javadoc in `DeclaredNullability` describes the fixed defect as current

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java:35-41`
**Issue:** The class comment says "when one is missing, discovery dropped it (for example backlog 999.4, an element named
`size` or `size0`)". After this phase that example no longer occurs for readable parents. The unpaired WARN is now reachable
only through the undecidable path (WR-01) or an unrelated discovery drop.
**Fix:** Reword to "for example a parent whose declared type could not be read (see `DescribeTableBuilder`)" and drop the
999.4 reference.

### IN-04: The snapshot characterization test is described as "pre-extraction" but its snapshots are regenerated by the code under test

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureSnapshotUnitTest.java:27-35`
**Issue:** The DisplayName and assertion message say the builder must reproduce "the snapshot the pre-extraction
production describeTable wrote". `git log` shows the `.columns.txt` files were re-captured in the fix commit `7c00953e`
(for example `s4_a04` now contains `c.size UINT16`), so they are post-fix output of the production path, not
pre-extraction. The snapshots are also written by the same code they verify, so a classifier regression that is
re-captured passes unnoticed unless a human reads the diff. The independent oracle is `tupleFieldsEqualTheDeclaredElements`
and the live round trip.
**Fix:** Rename the test and message to "reproduces the committed Column snapshot", and note in the Javadoc that the
snapshot is regression-only and `tupleFieldsEqualTheDeclaredElements` is the correctness oracle.

### IN-05: The same name ambiguity remains for a Tuple element named `null`

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java:186`, `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java:120`
**Issue:** `decide` drops every sub-column whose name ends with `.null` before any parent-type check, and `Table` ignores
`.null` children of Array parents. A declared element called `null` (`Tuple(\`null\` UInt8, v String)`) is therefore
dropped by the same mechanism 999.4 fixed for `size`, with only the unpaired WARN as signal. I did not reproduce this
against a server, so it is a risk rather than a confirmed defect, and it is outside the stated DESC-08 scope.
**Fix:** Record it as a backlog item next to 999.4, or reuse `ParentShape.declaresElement("null")` for the `.null` case
(the null map exists only when the parent is `Nullable`).

### IN-06: `DescribeTableBuilder` and `ClickHouseHelperClient` depend on each other

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java:30,287`
**Issue:** The builder borrows `ClickHouseHelperClient`'s logger identity and calls its package-private static
`buildDescribedTable`, while the client constructs the builder and calls `decide`. This is a two-class cycle created by an
extraction meant to separate the concerns. It works, but any later move of `buildDescribedTable` has to change both
files.
**Fix:** Move `buildDescribedTable` into `DescribeTableBuilder` (its only production caller) and keep the logger name as a
string constant.

### IN-07: `DescribeFixtureCapture` rewrites fixtures non-atomically and reports failures by class name only

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCapture.java:67-69,110-146`
**Issue:** Files are overwritten fixture by fixture and `SERVER_VERSION.txt` is written last. A failure partway through
leaves a mixed-version directory under the old version stamp, and the only output is
`DescribeFixtureCapture failed: <exception class>` with no cause, so a Docker or pull failure is undiagnosable. The "metadata
only" rationale does not apply to Docker or network exceptions, which carry no record values.
**Fix:** Capture into a temp directory and move into place after the last fixture succeeds, and print
`e.getMessage()` of the root cause.

### IN-08: Client leak if table setup fails in the round-trip test

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/TupleSizeNamedElementRoundTripLiveTest.java:476-497`
**Issue:** `chc` is created before the `try`, and `dropTable` and `NullableRoundTrip.createTable` run before it. If either
throws, `chc.close()` in the `finally` is never reached. In the `finally`, a throwing `dropTable` also skips
`chc.close()`.
**Fix:** Use try-with-resources for `chc` (it implements `AutoCloseable`) and move the setup calls inside the `try`. Wrap the
`dropTable` in its own try/finally so `close()` always runs.

---

_Reviewed: 2026-10-22_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
