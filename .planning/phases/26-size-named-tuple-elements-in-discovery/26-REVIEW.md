---
phase: 26-size-named-tuple-elements-in-discovery
reviewed: 2026-10-22T13:00:00Z
depth: standard
files_reviewed: 12
files_reviewed_list:
  - build.gradle.kts
  - CHANGELOG.md
  - docs/FORK-DELTA.md
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCapture.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureDriftLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureSnapshotUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixturesUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilderUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/TupleSizeNamedElementRoundTripLiveTest.java
findings:
  critical: 0
  warning: 0
  info: 4
  total: 4
status: issues_found
---

# Phase 26: Code Review Report

**Reviewed:** 2026-10-22
**Depth:** standard
**Files Reviewed:** 12
**Status:** issues_found

## Summary

This is an incremental re-review of the fix pass `a58c66da..0a1c79f8` (nine fix commits, one per finding) against the
prior review at `80275a4a`. The scope is the 12 files those commits touched. I read the full diff
`80275a4a..HEAD` outside `.planning`, then re-read `Table`, `DescribeTableBuilder`, `DeclaredNullability`,
`ParentShape` and `DescribeFixtureCapture` in full to check each fix in context. I did not run Gradle or any tests,
as instructed.

**All nine applied fixes are correct.** None is re-opened. Two small residues are new findings (IN-12, IN-13). The two
skipped findings stand by decision (IN-02, IN-06) and are carried forward unchanged.

**Resolved since the prior review (not re-opened):**

| ID | Fix | Verdict |
|----|-----|---------|
| IN-01 | `Table.registerValidColumn` uses `allColumnsMap.merge`, keeping a registered declared element against a later structural twin | Correct for the `Table` map. Both row orders now give the declared Column, and `handleNonRoot` still ignores the structural twin through `!child.isDeclaredElement()`. New test covers both orders. A residual in the builder's own map is IN-12. |
| IN-03 | `DeclaredNullability` class Javadoc reworded | Correct. The example now matches the only path that reaches the unpaired WARN. The wrap is ragged ("That is" ends a short line) but it is a comment. |
| IN-04 | Snapshot test renamed, Javadoc says the snapshots are regression-only | Correct. `tupleFieldsEqualTheDeclaredElements` exists. No stale reference to the old method name or "pre-extraction" remains in `src`, `docs`, `.planning/scripts`, `CHANGELOG.md` or `build.gradle.kts`. |
| IN-05 | Comment on `NULL_SUFFIX` records that the server rejects a Tuple element named `null` | Correct for the project's stated test target (ClickHouse `latest`). The evidence (code 36 on 26.9.19.9) is in the fix report. No code change. |
| IN-07 | `DescribeFixtureCapture` collects every file, then deletes stale files and writes, `SERVER_VERSION.txt` last. Failure line prints the root cause | Correct. A failure while capturing now leaves the committed directory and version stamp untouched. A failure inside the final write loop can still leave a mixed directory, but that window is now only disk I/O. The message carries only a class name and a Docker, pull or server error text, no record values. |
| IN-08 | Round-trip test: `chc` in try-with-resources, setup inside the inner `try` | Correct. `close()` now runs on every path. Assertions are unchanged. |
| IN-09 | FORK-DELTA "Known costs" item 10 says the drift premise is on demand only | Correct and accurate against `build.gradle.kts` (`test` excludes the tag, `check` does not depend on `describeFixtureDrift`). |
| IN-10 | CHANGELOG names both WARNs and the absent-parent case | Correct against `UNREADABLE_PARENT_WARNING` and `DeclaredNullability.UNPAIRED_ELEMENT_WARNING`. |
| IN-11 | `theDriftTagInTheBuildEqualsTheTagOfTheDriftTest` plus `inputs.file(build.gradle.kts)` on every `Test` task | Correct. The regex matches the single declaration at `build.gradle.kts:213`, the quoted literal appears once, `getAnnotationsByType(Tag.class)` handles the repeatable annotation, and `Files.readString` is UTF-8 regardless of the `windows-1252` `file.encoding` the suite sets. Referencing `DescribeFixtureDriftLiveTest.DRIFT_TAG` (a compile-time constant) does not initialise the Docker-dependent class, so the test stays Docker-free. |

**Standing by decision:** IN-02 and IN-06 (see below). The fix report's reasons are valid: both are pinned by
`fork-anchors.txt` rows that a pure move would break.

## Info

### IN-02: Production dead code kept only for tests

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:475-487`, `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/ParentShape.java:49`
**Issue:** Both `shouldSkipDescribeRow` overloads have no caller under `src/main`. `describeTableV1` and `describeTableV2`
go through `DescribeTableBuilder.addRow`. The two-argument form passes `Map.of()` as `rawTypes`, so it is a
different, weaker rule than the one production runs, and tests that call it (`ClickHouseHelperClientForkUnitTest`,
`DescribeTableBuilderUnitTest`) pin behaviour production never executes. `ParentShape.arrayDepth()` is public but
referenced only in a Javadoc `{@link}`.
**Status:** Stands by decision (skipped). `fork-anchors.txt` pins `shouldSkipDescribeRow` at count 3 (HLP-02) and
FORK-DELTA and MERGE-CHECKLIST name it as the sync landmark. Remove it in a deliberate change that edits the anchor row
in the same commit.
**Fix:** Point the tests at `DescribeTableBuilder.decide(...).skipsRow()` and delete both overloads together with the
anchor row. Make `arrayDepth()` package-private or remove it.

### IN-06: `DescribeTableBuilder` and `ClickHouseHelperClient` depend on each other

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java:30,289`
**Issue:** The builder borrows `ClickHouseHelperClient`'s logger identity and calls its package-private static
`buildDescribedTable`, while the client constructs the builder and calls `decide`. This is a two-class cycle created by an
extraction meant to separate the concerns. It works, but any later move of `buildDescribedTable` has to change both
files.
**Status:** Stands by decision (skipped). `buildDescribedTable` carries the only `DeclaredNullability.apply(` call in
`ClickHouseHelperClient.java`, pinned at count 1 (COLS-10); moving it fails the merge-invariant gate unless the anchor row
and the docs move in the same change.
**Fix:** Move `buildDescribedTable` into `DescribeTableBuilder` (its only production caller) and keep the logger name as a
string constant, editing the COLS-10 anchor row in the same commit.

### IN-12: The IN-01 fix leaves the builder's own name map order dependent, and its stated reason overlooks a declared JSON or Dynamic element

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java:172,251-258`
**Issue:** `Table` is now order independent, but `columnsByName.put(column.getName(), column)` is still last-wins. The fix
report leaves it alone because "a structural `size<digits>` twin is never JSON or Dynamic". That is true of the twin and
beside the point. The map is read by `hasJsonOrDynamicAncestor`, which needs the declared element, and the declared
element can be JSON or Dynamic. If a structural `c.size1 Array(UInt64)` row were printed after a declared
`c.size1 Array(Array(JSON))` row, the structural one would overwrite it, `c.size1.<path>` rows would no longer find a
JSON ancestor, and they would become Columns instead of being skipped (DESC-05 regression). The server currently prints the
structural row first (the fixtures), so the declared row wins and nothing breaks today. This is the same order assumption
IN-01 was about, now guarded in `Table` and unguarded in the builder. I did not verify that any server accepts a
size-named JSON or Dynamic element at Array depth 2 or more, so this is a risk and not a reproduced defect.
**Fix:** Make the builder's map follow the same rule as `Table`, so the two cannot disagree:
```java
columnsByName.merge(
        column.getName(),
        column,
        (kept, added) -> kept.isDeclaredElement() && !added.isDeclaredElement() ? kept : added);
```
and add a case to `theDeclaredElementWinsTheSharedNameInEitherRowOrder`, or a sibling test, with a JSON element and its
children in the declared-first order.

### IN-13: The new `allColumnsMap.merge` in upstream-owned `Table.registerValidColumn` has no fork-anchors row

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java:77-85`, `.planning/scripts/fork-anchors.txt:95-96`
**Issue:** IN-01 replaced upstream's `allColumnsMap.put(column.getName(), column)` with a fork-owned `merge`. The FORK-DELTA
row for `Table.updateParent` was updated to describe it, but `fork-anchors.txt` still has only the
`!child.isDeclaredElement()` row (count 1) and the count-0 row for upstream's condition for `Table.java`. A future upstream
sync that takes upstream's `registerValidColumn` leaves both anchor rows green and silently reverts the rule. In the
structural-first order that is still harmless, because last-wins gives the declared element. Only the declared-first order
regresses, and then only `theDeclaredElementWinsTheSharedNameInEitherRowOrder` notices. That is a test failure, not a
merge-gate failure, and the repo uses the anchors precisely to catch a silent upstream overwrite before tests run.
**Fix:** Add a row to `fork-anchors.txt`, for example
`src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java<TAB>allColumnsMap.merge(<TAB>1<TAB>DESC-08`,
and a count-0 row for `allColumnsMap.put(column.getName(), column)` if the gate's convention for replaced upstream lines
(as for row 96) applies here.

---

_Reviewed: 2026-10-22_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
