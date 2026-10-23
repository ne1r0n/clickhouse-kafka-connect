---
phase: 26-size-named-tuple-elements-in-discovery
fixed_at: 2026-10-22T14:30:00+03:00
review_path: .planning/phases/26-size-named-tuple-elements-in-discovery/26-REVIEW.md
iteration: 1
fix_scope: all
findings_in_scope: 4
fixed: 3
skipped: 1
status: partial
---

# Phase 26: Code Review Fix Report

**Fixed at:** 2026-10-22
**Source review:** `.planning/phases/26-size-named-tuple-elements-in-discovery/26-REVIEW.md`
**Iteration:** 1

**Summary:**
- Findings in scope: 4 (IN-02, IN-06, IN-12, IN-13; no Critical or Warning findings)
- Fixed: 3 (IN-06, IN-12, IN-13)
- Skipped: 1 (IN-02)

**Previously fixed (earlier passes, not repeated here):** WR-01, WR-02 and IN-01, IN-03, IN-04, IN-05, IN-07 to IN-11
(commits `595d0dc7`, `eb4e6821`, `a58c66da` to `0a1c79f8`); the review above re-verified them as correct.

**Isolation:** the fixes were made in a dedicated worktree on the temp branch `gsd-reviewfix/26-85754`
(`workflow.use_worktrees` is unset in `.planning/config.json`, so the default applies) and fast-forwarded onto
`sync/v1.7.0` by the cleanup tail. The unrelated uncommitted `.planning/state.json` and `gradlew.bat` were never staged
(`gradlew.bat` shows as dirty in every checkout of this repo because of line endings; it was reverted after each Gradle run).

**Verification:** ran in the isolated worktree (reproducible from the main checkout once the commits are fast-forwarded).
`./gradlew testUnit` over the whole unit set: 1128 tests, 1126 succeeded, 0 failed, 2 skipped (skips come from unrelated test classes, for example
`ConvertToMapOrJsonPerformanceTestJson`, which I did not touch). `compileTestJava compileIntegrationTestJava` clean. The merge-invariant gate
(`PRE_TAG=pre-v1.7.0-tail ... verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD`) ended
`anchor_commits=68 anchor_rows=79 marker_commits=80 result=PASS` at the final commit, and also passed after each
anchor-touching commit (rows 77, then 79). `verify-merge-invariants-selftest.sh` (unchanged by these commits) was started
but stopped after ST1 to ST3 passed, because each of its 16 cases re-runs the roughly two minute gate; it was not run to completion. `spotlessApply` was run and every file it reformatted that I had not changed
(the 46 pre-existing violations) was reverted; in `ClickHouseHelperClient.java` the import reordering it produced was
discarded and only the intended lines are in the diff.

## Fixed Issues

### IN-06: `DescribeTableBuilder` and `ClickHouseHelperClient` depend on each other

**Status:** fixed
**Files modified:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java`, `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java`, `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java`, `.planning/scripts/fork-anchors.txt`, `docs/FORK-DELTA.md`
**Commit:** 1427ce83
**Applied fix:** `buildDescribedTable` (its only production caller is `DescribeTableBuilder.build`) moved verbatim into
`DescribeTableBuilder`, and the builder's logger is now `LoggerFactory.getLogger(LOGGER_NAME)` with the constant
`com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient`, so the logger identity (and every
`LogCapture.attach(ClickHouseHelperClient.class)` in the tests) is unchanged while the class reference is gone. The builder
no longer references `ClickHouseHelperClient` at all; the client only constructs the builder. The now-unused
`DeclaredNullability` import was dropped from the client. The one test caller (`discover(...)` in
`ClickHouseHelperClientForkUnitTest`) points at `DescribeTableBuilder.buildDescribedTable`. In the same commit the two
COLS-10 anchor rows moved to `DescribeTableBuilder.java` (`DeclaredNullability.apply(` count 1; `buildDescribedTable(`
count 2, the definition plus the call in `build()`), the Phase 19 comment above them says where they went, and the
FORK-DELTA nullability row lists `DescribeTableBuilder.java` in place of `ClickHouseHelperClient.java`. Behaviour is
unchanged: the method body, its call order and the log source are identical.

### IN-12: The IN-01 fix leaves the builder's own name map order dependent, and its stated reason overlooks a declared JSON or Dynamic element

**Status:** fixed (the change is a consistency guard; its reviewer-described failure is not reachable, see below)
**Files modified:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java`
**Commit:** f71899ae
**Applied fix:** `columnsByName.put(...)` became the same `merge` rule `Table.registerValidColumn` uses
(`kept.isDeclaredElement() && !added.isDeclaredElement() ? kept : added`), so the builder's map and `Table`'s map cannot
disagree on which row owns a shared name.
**Note on the finding's premise and on tests:** I tried to write the suggested test (a declared JSON element with a
structural twin printed after it) and it could not be made to differ from the old behaviour. A structural `size<digits>`
twin only exists below an Array parent of depth two or more (`ParentShape.isOffsetsRow`); the declared element there is
typed `Array(Array(JSON))`, whose `Column.getType()` is `ARRAY`, and `hasJsonOrDynamicAncestor` only fires on a Column
whose own type is `JSON` or `DYNAMIC`. A declared `JSON` element at depth 0 has no structural twin, so both rows would
be classified as declared and last-wins already gives the same result. So the merge changes nothing observable through
`addRow`, and no new test was added (an assertion that passes with and without the change would pin nothing). The
existing `theDeclaredElementWinsTheSharedNameInEitherRowOrder` still covers the `Table` side. Human verification is not
needed for behaviour (none changes) but a reviewer may prefer to drop this commit as redundant.

### IN-13: The new `allColumnsMap.merge` in upstream-owned `Table.registerValidColumn` has no fork-anchors row

**Status:** fixed
**Files modified:** `.planning/scripts/fork-anchors.txt`, `docs/FORK-DELTA.md`
**Commit:** 9bc34174
**Applied fix:** two rows for `Table.java` under DESC-08: `allColumnsMap.merge(` at count 1, and the count-0 row for
upstream's replaced line `allColumnsMap.put(column.getName(), column)` (the file has no other `allColumnsMap.put`). The
FORK-DELTA `Table.updateParent` row now names both new rows. The gate was re-run on the commit: `anchor_rows=79 result=PASS`.

## Skipped Issues

### IN-02: Production dead code kept only for tests

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:475-487`, `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/ParentShape.java:49`
**Reason:** skipped: the anchor rows could be edited, but the removal is not a narrow fix and would weaken the fork's
sync tooling. `shouldSkipDescribeRow` is the named landmark in `.planning/MERGE-CHECKLIST.md` (item 13), in three
FORK-DELTA rows and the Known-costs sync notes (FORK-DELTA lines 47 and 217), and in the HLP-02 anchor (count 3); the
anchor is the only guard that a sync does not bring back an upstream copy of the skip rule, and the review's own fix says
to delete the anchor row with the overloads. The two-argument overload is also pinned by 14 call sites in
`ClickHouseHelperClientForkUnitTest` that assert the unreadable-parent default of the old rule, which would each have to be
rewritten to `DescribeTableBuilder.decide(...).skipsRow()` with an explicit raw-type map, plus `arrayDepth()` is a public
method of a public class. That is a deliberate API and documentation change across the fork's merge tooling, not a
behaviour-preserving cleanup, and the project constraint is to keep changes additive and small for rebasability. It should
be done as its own change that also rewrites the MERGE-CHECKLIST landmark.
**Original issue:** Both `shouldSkipDescribeRow` overloads have no caller under `src/main`; the two-argument form passes
`Map.of()` as `rawTypes`, so it is a weaker rule than the one production runs, and tests that call it pin behaviour
production never executes. `ParentShape.arrayDepth()` is public but referenced only in a Javadoc `{@link}`.

---

_Fixed: 2026-10-22_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
