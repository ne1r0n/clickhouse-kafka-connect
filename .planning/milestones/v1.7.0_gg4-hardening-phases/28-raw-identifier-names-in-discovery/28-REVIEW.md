---
phase: 28-raw-identifier-names-in-discovery
reviewed: 2026-10-23T17:00:00Z
depth: standard
files_reviewed: 21
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
  - src/test/java/com/clickhouse/kafka/connect/ci/ForkCoverageScopeUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/RawIdentifierNameLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCatalog.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumMockServerTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrix.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixDriftLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixFixturesUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/UnescapeJsonLayerTest.java
  - build.gradle.kts
  - .github/scripts/jacoco_summary.py
  - docs/FORK-DELTA.md
  - CHANGELOG.md
findings:
  critical: 0
  warning: 0
  info: 5
  total: 5
status: issues_found
---

# Phase 28: Code Review Report

**Reviewed:** 2026-10-23T17:00:00Z
**Depth:** standard
**Files Reviewed:** 21
**Status:** issues_found

## Summary

This is an incremental re-review of the review-fix commits since `a9f7087f` (fixes for WR-01 to WR-04 of the
first report). The four warnings are resolved and I found no new critical or warning issues in the fixes.

What I checked on the fixes:

- **WR-01 (`Column.unescapedLabelChar`).** The new escape set (`n t r b f 0 \\ \'`, anything else refused) matches
  what the server writes. I measured it rather than relying on reading: `DESCRIBE TABLE` on
  `clickhouse/clickhouse-server:latest` for an `Enum16` with labels backspace, form feed, NUL, BEL, VT, 0x01,
  0x7f, a non-ASCII letter, a double quote, an apostrophe and a backslash returns `'\\b'`, `'\\f'`, `'\\0'`,
  `'\\''` and `'\\\\'` for the five SQL-escaped ones (the JSON layer doubles the backslash), and writes BEL, VT and
  0x01 as `\u0007`, `\u000B`, `\u0001` (the JSON layer only, decoded by `unescapeJsonLayer`) and 0x7f, `"` and the
  non-ASCII letter raw. So the throw on an unknown escape cannot fire for any label the server writes, and the
  collision case (`'\b'` and `'b'`) is now two distinct keys. The new `ColumnTest` pin uses the doubled-backslash
  text as the server prints it, which is the correct layer.
- **WR-02 (`describeTableV1`).** Catching `RuntimeException` makes V1 match V2. `extractTablesMapping` already skips a
  `null` table and `ClickHouseWriter` (line 1266) falls back to the existing mapping, so a skipped table is handled.
  The new stub test fails without the fix and passes with it.
- **WR-03 (`UnescapeJsonLayerTest`, malformed-enum cases).** I traced each case through `unescapeJsonLayer` and
  `extractEnumValues`; every row lands on the branch its comment names. The Java source escapes are safe (a `\\u`
  is not a Java unicode escape, and the tests run under `file.encoding=windows-1252`, so only ASCII and `\u` escapes
  appear in source).
- **WR-04.** The two substrings are the real cause-chain texts, so the pin no longer passes on an unrelated failure.

The remaining items are the three info items from the first report (still open per the disposition file) and two
small new test-quality notes.

## Resolved

Fixed in this iteration and re-verified above; carried only so the history stays visible.

- WR-01: Enum label scan reads the SQL escapes `\0`, `\b` and `\f` as the letters 0, b and f (fixed, a8f50d87 and 54447ab4)
- WR-02: `describeTableV1` lets the new enum parse failure escape and abort the mapping of the whole database (fixed, ae97c496)
- WR-03: The new JSON-layer decoder's `\uXXXX` path and the malformed-enum branches have no test (fixed, 16950de4)
- WR-04: `legacyEnumLabelKeyFailsLoudly` passes on any failure, not on the enum lookup miss it names (fixed, 2575092a)

## Info

### IN-01: `DeclaredNullability.ESCAPED_NEWLINE` is a public production constant that only tests read

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java:43-44`
**Issue:** After the `normalise` rewrite nothing in `src/main` uses `ESCAPED_NEWLINE`; five test files do. It
is public API surface in a shipped class, existing so tests can build multi-line type text. The class
Javadoc paragraph at lines 32-37 is also wrapped mid-sentence ("That is\n not a mismatch").
**Fix:** Move the constant to `DeclaredNullabilityFixtures` (test sources) and reflow the paragraph.

### IN-02: `ForkCoverageScopeUnitTest` cannot detect a fork class missing from the list, and `SinkTaskStatistics` is one

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/ForkCoverageScopeUnitTest.java:44-51,98-106`; `build.gradle.kts:308-353`
**Issue:** The class Javadoc and the 28-06 summary say the guard stops a class "vanishing from the coverage
view" and keeps the lists "complete". It only (a) compares the two copies with each other and (b) requires
six hard-coded names. A fork class that nobody adds to `CLASSES_ADDED_BY_THIS_MILESTONE` is not detected.
Concretely, `util/jmx/SinkTaskStatistics.java` carries 51 changed lines against `v1.7.0` (the connector-name
JMX naming that `PROJECT.md` lists as a fork feature) and appears in neither list, while the neighbouring
`MBeanServerUtils` does.
**Fix:** Either add `SinkTaskStatistics` (and its `$*` glob, plus the Python entry) or record in FORK-DELTA why
it is out of scope, and reword the Javadoc to say the guard pins list equality and a named minimum.

### IN-03: A record still using the escaped name form is stored with the column DEFAULT and no log line

**File:** `docs/FORK-DELTA.md` (Known costs item 11); `CHANGELOG.md:26-35`
**Issue:** This is the accepted D-06 decision, recorded here only so it is not lost: after the upgrade a
root field named in the old doubled form is ignored and the column DEFAULT is stored without any WARN
(the Tuple element case fails loudly). The CHANGELOG sits under "Bug Fixes" and tells operators to rename
fields first, but nothing at runtime reveals a missed rename, and rows already written are not repaired.
**Fix:** If a runtime signal is ever acceptable, log one WARN per task when a record field matches a column
only after applying the old doubling; otherwise promote the CHANGELOG entry to a breaking-change notice
at the top of the release.

### IN-04: The server's escape set for enum labels is now measurable, but the pin and the wording still say "not measured"

**File:** `docs/FORK-DELTA.md` (Known costs item 11); `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java:195-201`
**Issue:** The fix pins backspace, form feed and NUL only on hand-written type text, and the FORK-DELTA wording
says the server is "expected to write" those escapes. A one-line `DESCRIBE` against `latest` (see the Summary)
confirms it, including that other control characters (BEL, VT, 0x01) are written raw and only JSON-escaped as
`\u00XX`. Nothing in `RawNameMatrix` or its live and drift tests will notice if a later server changes that set,
and the new fail-closed throw would then turn such a label into an unmapped table instead of a mis-key. The
same escape set is still translated for element names only for `\n`, `\t`, `\r` in `DeclaredNullability.normalise`
(a name with a backspace, form feed or NUL falls back with the existing WARN, also unmeasured).
**Fix:** Add backspace, form feed, NUL and BEL spike entries to `RawNameMatrix` (enum label and Tuple element
name), run the live matrix once, and then drop the "expected to write" and "not measured" hedges for those
characters in FORK-DELTA item 11.

### IN-05: `DescribeUnreadableEnumMockServerTest` never closes the helper clients and does not tie the null to the enum

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumMockServerTest.java:62-75`
**Issue:** `client(boolean)` builds a `ClickHouseHelperClient` (which is `AutoCloseable`) per test and neither
test closes it, against the repository rule to use try-with-resources for `AutoCloseable`. Separately, the
assertion `assertNull(describeTable(unreadable))` is satisfied by any exception inside the describe, not only the
enum scan; the stub serving a clean `readable` table afterwards guards against a dead server but not against the
unreadable table failing for a different reason. `RawIdentifierNameLiveTest.ENUM_LOOKUP_MISS_TEXT` also hard-codes
`Enum8` in a message prefix, so adding an `Enum16` matrix entry would fail the pin for a reason unrelated to the
behaviour.
**Fix:** Create the client in a try-with-resources block inside `assertUnreadableTableIsSkippedAndTheNextOneIsMapped`
(pass the boolean instead of the client). To bind the null to the enum cause, assert on the logged error (a
Logback or Log4j test appender capturing `Enum type could not be read`) or call `Column.extractColumn` on the
stub row in a separate assertion. Build the live pin's type text from `RawNameMatrix` rather than a literal.

---

_Reviewed: 2026-10-23T17:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
