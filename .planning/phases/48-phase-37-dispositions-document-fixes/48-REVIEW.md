---
phase: 48-phase-37-dispositions-document-fixes
reviewed: 2026-11-06T00:00:00Z
depth: standard
files_reviewed: 14
files_reviewed_list:
  - docs/FORK-DELTA.md
  - docs/ROLLOUT-v1.7.0_gg1.md
  - docs/ROLLOUT-v1.7.0_gg6.md
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
  - src/test/java/com/clickhouse/kafka/connect/ci/AllowListWildcardRuleUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumNullBindingUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/SimpleAggregateTupleRoundTripLiveTest.java
findings:
  critical: 0
  warning: 2
  info: 3
  total: 5
  necessity: 0
status: issues_found
---

# Phase 48: Code Review Report

**Reviewed:** 2026-11-06
**Depth:** standard
**Files Reviewed:** 14
**Status:** issues_found

## Summary

Scope was `git diff 92570854 HEAD -- . ':!.planning'`. I read every changed file and the code the changes talk about, and ran the new and touched unit tests (AllowListWildcardRuleUnitTest, both StopUnitTests, RetriableExceptionPassThroughUnitTest, UnvettedRetriableCodesWarnUnitTest, DescribeUnreadableEnumNullBindingUnitTest, DeclaredNullabilityTest, SourcePlanningIdGuardUnitTest, CompareConfluentArchivesScriptUnitTest): all green, Docker-free.

Production move: `ESCAPED_NEWLINE` is gone from `DeclaredNullability`. A grep over `src`, `.github`, `docs`, `build.gradle.kts` and `.planning/scripts` finds no reader left. The only users are the 7 test call sites, and they now point at `DeclaredNullabilityFixtures`. No `src/integrationTest` reader exists. The fork-anchor row `declaredType.replace(ESCAPED_NEWLINE, SPACE)` has expected count 0, so it is unaffected.

Tests: none of the new classes uses Mockito, `ClickHouseBase` or Testcontainers. No existing assertion was removed or weakened. The only removed assertion lines are the three that switch to the moved constant. The MBean-unregister, null-binding and plain-DataException tests assert real behaviour. Nothing logs record data.

Docs: the resolver paths in FORK-DELTA rows 25 and 35 resolve and are tracked. The `KeyToValueTest` path, items 17 and 24 (budget and built-in set), the `v1.7.0_gg7` claim for the tripwire test (commit `59c5975e` is an ancestor of tag `v1.7.0_gg7`) and the tests-gate/tag-gate order (`ReleaseWorkflowGuardUnitTest.CHAIN`) are all true. The ROLLOUT notes carry no planning identifier. Two findings are real: a test rule weaker than its stated purpose, and one doc overclaim.

## Warnings

### WR-01: AllowListWildcardRuleUnitTest accepts `outer/<dir>/*` and empty segments, so the rule is weaker than its javadoc

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/AllowListWildcardRuleUnitTest.java:62-68`
**Issue:** `followsTwoLiteralSegments` counts `/` characters before the first wildcard. The javadoc says the rule stops a row such as `com/*` or `META-INF/*` from silencing a whole namespace. `compare_confluent_archives.py` (lines 106-114) prefixes every outer-zip path with `outer/`. So `outer/com/*` or `outer/META-INF/*` has two slashes and passes, yet only one real path segment is literal, which is the same breadth as the refused `com/*`. A row like `a//*` or `//*` also passes, because empty segments count. The refused-case parameter list has `outer/*` but no `outer/<dir>/*`, so the gap is untested. The current shipped lists hold no `outer/` row (grep count 0 in all five), so nothing is wrong today. The guard would not catch the case it is documented to catch, for the next release's list.
**Fix:** Strip a leading `outer/` before counting, and count non-empty segments:
```java
private static final String OUTER_PREFIX = "outer/";

static boolean followsTwoLiteralSegments(String glob) {
    int first = firstWildcard(glob);
    if (first < 0) {
        return true;
    }
    String literal = glob.substring(0, first);
    if (literal.startsWith(OUTER_PREFIX)) {
        literal = literal.substring(OUTER_PREFIX.length());
    }
    int lastSlash = literal.lastIndexOf(SEPARATOR);
    if (lastSlash < 0) {
        return false;
    }
    return Arrays.stream(literal.substring(0, lastSlash).split("/", -1))
                    .filter(s -> !s.isEmpty()).count() >= MIN_LITERAL_SEGMENTS;
}
```
Add `outer/com/*`, `outer/META-INF/*` and `a//*` to the refused list, and `outer/com/google/*` to the accepted list.

### WR-02: FORK-DELTA item 14 says "the same two lines any failing RowBinary column logs"; the first line is not logged for every write site

**File:** `docs/FORK-DELTA.md:244` (the added sentence beginning "On the RowBinary route, a body write that the write-phase deadline cuts first logs two ERROR lines")
**Issue:** `Error writing value of ...` comes only from the generic `catch (Exception e)` of `doWriteColValue` (`ClickHouseWriter.java:1092-1094`). `Record serialization failed: ...` comes from `writeColumnWithRecordContext` (`ClickHouseWriter.java:1574`) for any `IOException | RuntimeException` that is not a `RetriableException`. `doWriteCol` also writes outside `doWriteColValue`: the null and default flag bytes at `ClickHouseWriter.java:1279-1309` (`BinaryStreamUtils.writeNonNull`, `writeNull`, `writeUnsignedInt8`). A deadline cut that lands in one of those writes produces only the second line. The note gives operators grep targets for a deadline cut, so "two lines" and "any failing column logs" are stated as certainties that hold only for cuts inside the value write. The note does say "(read from the code)", but it still overclaims.
**Fix:** Reword to "logs the `Record serialization failed: ...` line for the record and column being written, and, when the cut lands inside the value write, `Error writing value of ...` before it". Keep the remark that neither line means the record is bad.

## Info

### IN-01: DeclaredNullabilityFixtures javadoc says production "no longer reads" the constant; it never did

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java:5-12`
**Issue:** The constant was unused in `src/main` (that was finding 28 IN-01), so "production code no longer reads it" implies a past use that did not exist. The old class javadoc enforced "never hold a copy of the production constant; tests read the production constant". That rule is now gone. `ESCAPED_NEWLINE = "\\n"` is the test suite's own copy of the backslash-n knowledge that `DeclaredNullability.normalise` encodes separately, and the javadoc does not say that the live premise assertions (`NullableTupleMatrixLiveTest:763`, `SimpleAggregateTupleRoundTripLiveTest:118`, `DeclaredNullabilityTest:214`) are what tie it to what the server and `fromJsonRow` produce. The new constant also has no blank line before the `ROOT_SENTINEL` javadoc.
**Fix:** Reword to "{@link #ESCAPED_NEWLINE} is the test-side name for the two characters `fromJsonRow` leaves for a server-escaped newline; the live premise assertions check it against the server", and add a blank line before `ROOT_SENTINEL`.

### IN-02: `theTaskMBeanIsRegisteredBeforeStopAndGoneAfter` duplicates an existing test apart from its precondition

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java:249-256`
**Issue:** The body equals `stopWithoutADeliveryStrategyStillStopsTheProxyTask` (lines 241-247) plus `assertTrue(harness.mBeanRegistered(), ...)`. The "gone after" half is asserted twice, and the "gone" assertions in `assertProxyStoppedOnce` are only meaningful because of the precondition. This is valid but is the repeat the project's DRY rule warns against.
**Fix:** Put the precondition into the `Harness` constructor or `assertProxyStoppedOnce`, then drop the extra test. Alternatively, leave it and fold the no-strategy case into it.

### IN-03: FORK-DELTA still cites `.planning/phases/45..47` evidence paths, and item 20 still uses review-row identifiers

**File:** `docs/FORK-DELTA.md:47-49, 53, 111, 117, 228, 242, 244, 250` (paths); `docs/FORK-DELTA.md:256` (item 20)
**Issue:** These paths resolve today, but they carry the staleness this phase repaired for the Phase 25 and 35 paths: they move at the milestone archive. The phase records this as a deliberate hand-off (48-CONTEXT D-2, ledger hand-off item 6), so this is a reminder and not a defect of the phase. Item 20 reads "Phase 37", "28 IN-01", "26 IN-02", "27 WR-01", "27 IN-01" and "28 IN-05". Those identifiers mean nothing outside `.planning`. This matches the existing style of FORK-DELTA, and the ROLLOUT notes (the user-facing documents) are clean, so I rate it Info.
**Fix:** At the milestone close, repoint the `.planning/phases/45..47` paths and re-run `48-path-check.py` (as the ledger says). Optionally name item 20's findings by file and symbol (`DeclaredNullability.ESCAPED_NEWLINE`, `shouldSkipDescribeRow`) instead of review-row numbers.

---

_Reviewed: 2026-11-06_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
