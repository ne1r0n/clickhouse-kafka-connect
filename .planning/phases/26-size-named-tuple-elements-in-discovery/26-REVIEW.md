---
phase: 26-size-named-tuple-elements-in-discovery
reviewed: 2026-10-22T12:00:00Z
depth: standard
files_reviewed: 18
files_reviewed_list:
  - CHANGELOG.md
  - build.gradle.kts
  - docs/FORK-DELTA.md
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureDriftLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixturesUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilderUnitTest.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/ParentShape.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCapture.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCatalog.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureSnapshotUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/TupleSizeNamedElementRoundTripLiveTest.java
  - src/test/resources/describe-fixtures/SERVER_VERSION.txt
  - .github/workflows/tests.yaml
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java
findings:
  critical: 0
  warning: 0
  info: 11
  total: 11
status: issues_found
---

# Phase 26: Code Review Report

**Reviewed:** 2026-10-22
**Depth:** standard
**Files Reviewed:** 18
**Status:** issues_found

## Summary

This is an incremental re-review since `5704c8aa`. It covers the two fix commits `595d0dc7` (WR-01) and `eb4e6821`
(WR-02), which changed the six files listed first in `files_reviewed_list`. The other files in the list were re-read
only to confirm that the carried-forward Info findings still hold.

**WR-01 and WR-02 fixes: both are correct and complete as far as I can prove.** No new Critical or Warning finding.

- `build.gradle.kts`. The second `useJUnitPlatform { excludeTags(...) }` on `test` does not drop any earlier option:
  the only other JUnit Platform configuration in the file is the `testUnit` and `describeFixtureDrift` tasks, and the
  `testing.suites` `useJUnitJupiter()` only selects the framework and dependencies. `withType<Test>` sets no engine or
  tag options. `./gradlew check --dry-run` on the current tree lists `:test` and no `describeFixtureDrift`, as claimed.
  The new task shares what the claim says: it sits under the same `withType<Test>` block (forks, `file.encoding`,
  logging, Java launcher, listener) and uses the same `testClassesDirs` and `classpath` as `test`. The drift class
  keeps `@Tag("clickhouse")` from `ClickHouseBase`, so it stays out of `testUnit` as well.
- `DescribeFixtureDriftLiveTest`. `ClickHouseHelperClient.version()` exists and returns `String` (it can return null,
  which `%s` renders as `null`). The `Supplier` is only evaluated on failure. The `@Tag` on the subclass adds to the
  inherited `@Tag("clickhouse")` instead of replacing it. `SERVER_VERSION.txt` really is `version=26.9.19.9`,
  `image=...`, `image_id=...`, so the `version=` prefix parse is right.
- `DescribeTableBuilderUnitTest` and `DescribeFixturesUnitTest`. The renamed test, its `@DisplayName`, the new comment
  and the reworded Javadoc now match the asserted behaviour (`List.of("c.x.v")` with the table served and one metadata-only
  WARN). No `fail-closed` wording is left in `src` or `docs` apart from the explicit "not fail-closed" comment.
- `CHANGELOG.md` and `docs/FORK-DELTA.md`. The caveats match `DescribeTableBuilder.decide`: only a bare `size` or
  `size0` becomes `SKIP_UNDECIDABLE`, and `size<digits>` stays `KEEP_UNDECIDABLE`. The WARN text quoted in the CHANGELOG
  matches `UNREADABLE_PARENT_WARNING`. The residual fail-open behaviour of the undecidable path is now documented and
  accepted by decision D-03, so it is not re-raised. Two small accuracy gaps are recorded as IN-09 and IN-10.

**Carried forward unchanged in id and title:** IN-01 to IN-08. I re-opened each cited file and line range in the
current source and all eight are still valid. Line numbers were refreshed where they moved. IN-02 and IN-03 stay
accurate for the undecidable path that WR-01 deliberately kept. **Dropped:** none.

**New:** IN-09 (the drift premise now has no automated runner and one doc line is stale), IN-10 (the CHANGELOG
undercounts the WARNs on the undecidable path), IN-11 (the tag string is duplicated across Java and Gradle with no
guard).

## Info

### IN-01: Structural `size<digits>` rows share a name with the declared element and the last registration wins

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java:166-170`, `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java:77-80`
**Issue:** For `Array(Array(Tuple(size1 ...)))` the server prints `c.size1 Array(UInt64)` (structural, kept as a Column)
and `c.size1 Array(Array(UInt8))` (declared) under one name. `columnsByName.put` and `Table.registerValidColumn` both
overwrite, so which Column serves as the parent of `c.size1.*` children depends on server row order. Today the structural
row comes first (`n01`, `s4_a10`, `x05`), so the declared one wins. If an older or newer server printed the declared row
first, Tuple-valued size-named elements at Array depth 2 or more would bind their children to the `UInt64` structural
Column and silently lose fields. I could not verify other server versions. The drift test is the only guard, and after
the WR-02 fix it no longer runs by default (see IN-09).
**Fix:** Make the winner order-independent in the builder, for example
`if (decision == RowDecision.KEEP_DECLARED || !columnsByName.containsKey(column.getName())) columnsByName.put(...)`,
or stop adding `KEEP_STRUCTURAL_SIZE` rows to `columns` (they are ignored by `Table` anyway). The second option changes the
committed `all` snapshot lines.

### IN-02: Production dead code kept only for tests

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:475-487`, `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/ParentShape.java:49`
**Issue:** Both `shouldSkipDescribeRow` overloads have no caller under `src/main`. `describeTableV1` and `describeTableV2`
go through `DescribeTableBuilder.addRow`. The two-argument form passes `Map.of()` as `rawTypes`, so it is a
different, weaker rule than the one production runs, and tests that call it (`ClickHouseHelperClientForkUnitTest`,
`DescribeTableBuilderUnitTest:367`) pin behaviour production never executes. `ParentShape.arrayDepth()` is public but
referenced only in a Javadoc `{@link}`.
**Fix:** Point the tests at `DescribeTableBuilder.decide(...).skipsRow()` and delete both overloads. Make `arrayDepth()`
package-private or remove it.

### IN-03: Stale class Javadoc in `DeclaredNullability` describes the fixed defect as current

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java:32-38`
**Issue:** The class comment says "when one is missing, discovery dropped it (for example backlog 999.4, an element named
`size` or `size0`)". After this phase that example no longer occurs for readable parents. The unpaired WARN is now reachable
only through the undecidable path (parent type unreadable or absent from the described rows) or an unrelated discovery
drop.
**Fix:** Reword to "for example a bare `size` or `size0` element whose parent's declared type could not be read (see
`DescribeTableBuilder`)" and drop the bare 999.4 reference.

### IN-04: The snapshot characterization test is described as "pre-extraction" but its snapshots are regenerated by the code under test

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureSnapshotUnitTest.java:32-45`
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
only" rationale does not apply to Docker or network exceptions, which carry no record values. After the WR-02 fix the
drift message tells the operator to run this task, so a broken capture now matters more.
**Fix:** Capture into a temp directory and move into place after the last fixture succeeds, and print
`e.getMessage()` of the root cause.

### IN-08: Client leak if table setup fails in the round-trip test

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/TupleSizeNamedElementRoundTripLiveTest.java:476-497`
**Issue:** `chc` is created before the `try`, and `dropTable` and `NullableRoundTrip.createTable` run before it. If either
throws, `chc.close()` in the `finally` is never reached. In the `finally`, a throwing `dropTable` also skips
`chc.close()`.
**Fix:** Use try-with-resources for `chc` (it implements `AutoCloseable`) and move the setup calls inside the `try`. Wrap the
`dropTable` in its own try/finally so `close()` always runs.

### IN-09: The drift premise has no automated runner after WR-02, and one FORK-DELTA sentence still reads as if it does

**File:** `.github/workflows/tests.yaml:76`, `build.gradle.kts:286-297`, `docs/FORK-DELTA.md:179`
**Issue:** CI runs only `./gradlew test`, which now excludes `describe-drift`. Nothing runs `describeFixtureDrift`
(no schedule, no non-gating job), so the committed fixtures can go stale against `latest` without anyone noticing. They
are the only guard for IN-01's row-order assumption. The fix report records this as a known trade-off. FORK-DELTA
"Known costs" item 10 still says the rule is pinned "live by the drift premise" without saying that the premise is on
demand only.
**Fix:** Add a separate non-gating job (`continue-on-error: true`, or a nightly `schedule`) that runs
`./gradlew describeFixtureDrift`, or at minimum change item 10 to "and, on demand, live by the drift premise
(`describeFixtureDrift`, not part of `test` or CI)".

### IN-10: The CHANGELOG caveat says "one WARN per such parent" but a second, different WARN is also emitted

**File:** `CHANGELOG.md:8-11`
**Issue:** On the undecidable path `DescribeTableBuilder` logs one `Could not read the declared type ...` WARN per
parent, and `DeclaredNullability` then also logs `Declared Tuple element(s) [...] ... have no matching DESCRIBE
sub-column` for the root column. The unit test asserts both (`DescribeTableBuilderUnitTest:278-286`). An operator who
greps for the quoted text finds the first only, and the second is the one that names the dropped elements. The
parent can also be "unreadable" because it is absent from the described rows, not only because the parser rejects it.
**Fix:** Reword to "The connector logs a `Could not read the declared type ...` WARN per such parent and a `Declared
Tuple element(s) ... have no matching DESCRIBE sub-column` WARN naming the dropped elements, and the table is still
served."

### IN-11: The `describe-drift` tag string is duplicated across Java and Gradle with no guard

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureDriftLiveTest.java:42-43`, `build.gradle.kts:213`
**Issue:** `DRIFT_TAG` in the test and `describeFixtureDriftTag` in the build must match; only a comment says so. If
the Java constant is renamed alone, `describeFixtureDrift` finds no tests and fails (Gradle 9 default), which is loud.
If the Gradle constant is renamed alone, `test` silently includes the 94 live cases again and the WR-02 problem
returns without any signal.
**Fix:** Pass the tag from Gradle into the test JVM (for example `systemProperty("describe.drift.tag", ...)`) and add a
Docker-free assertion that `DescribeFixtureDriftLiveTest.class.getAnnotation(Tag.class).value()` equals that property,
or accept the duplication and leave the comment as the only guard.

---

_Reviewed: 2026-10-22_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
