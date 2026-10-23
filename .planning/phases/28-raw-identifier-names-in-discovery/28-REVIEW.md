---
phase: 28-raw-identifier-names-in-discovery
reviewed: 2026-10-23T12:00:00Z
depth: standard
files_reviewed: 19
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
  - src/test/java/com/clickhouse/kafka/connect/ci/ForkCoverageScopeUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/RawIdentifierNameLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCatalog.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrix.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixDriftLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixFixturesUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
  - build.gradle.kts
  - .github/scripts/jacoco_summary.py
  - docs/FORK-DELTA.md
  - CHANGELOG.md
findings:
  critical: 0
  warning: 4
  info: 3
  total: 7
status: issues_found
---

# Phase 28: Code Review Report

**Reviewed:** 2026-10-23T12:00:00Z
**Depth:** standard
**Files Reviewed:** 19
**Status:** issues_found

## Summary

The phase moves discovery to raw `CREATE TABLE` identifiers on both ClickHouse clients: a names-only
JSON decode in `ClickHouseFieldDescriptor.fromJsonRow`, one UTF-8 raw-line reader for both describe
paths, a rewritten `DeclaredNullability.normalise`, and an escape-aware enum label scan in
`Column.extractEnumValues`. I traced the following and found them sound for the characters the phase
spiked (backslash, backslash-n, line feed, tab, carriage return, slash, double quote, backtick,
apostrophe, non-ASCII):

- The token-offset slicing in `fromJsonRow` is consistent for both the char-array and the `StringReader`
  parser variants, and non-string values are consumed whole by `readValueAsTree`, so they cannot
  desynchronise the loop.
- `unescapeJsonLayer` and `normalise` consume escape pairs together, so an escaped quote never closes a
  quote and a trailing backslash or an unterminated quote is copied unchanged. The JSON layer is correctly
  decoded before the SQL layer is read.
- No code that builds SQL from a discovered `Column` name was affected by the switch to raw names (INSERT
  paths use the table-level insert or config-supplied column lists).
- The 21-entry fork coverage list in `build.gradle.kts` and `jacoco_summary.py` agree, and every entry has
  a source file and an inner-class glob.

No critical issues. The highest-value findings are all at the edge the phase declares "not measured": the
SQL escapes that the server writes for NUL, backspace and form feed, which the enum label scan silently
mis-reads (WR-01); an exception-handling asymmetry that lets the new enum parse failure abort the whole
table mapping on the default client V1 (WR-02); and test gaps over the new decoders (WR-03, WR-04).

## Warnings

### WR-01: Enum label scan reads the SQL escapes `\0`, `\b` and `\f` as the letters 0, b and f

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java:457-468` (used at line 406)
**Issue:** `unescapedLabelChar` translates only `n`, `t` and `r`; every other escaped character is returned
as itself. ClickHouse writes a string literal through `writeAnyEscapedString`, which escapes backspace,
form feed and NUL as the pairs backslash-b, backslash-f and backslash-0 (this is my reading of the server's
quoting routine; it is not measured in this repo, and `28-RESEARCH.md` assumption A1 records that NUL,
backspace and form feed were not measured). For an enum label holding one of those characters the scan
therefore produces a different label than the DDL declares:

- `Enum8('\b' = 1, 'b' = 2)` yields the keys `b` -> 1 then `b` -> 2 in the `HashMap`; the second `put`
  silently overwrites the first, so the label that was declared as backspace is lost and a record value of
  `b` maps to whichever entry was inserted last. Reverse the declaration order and `b` silently maps to the
  wrong code. That is a silent type-mapping error on a connector whose rule is "no data loss is acceptable".
- With no collision the label is simply unreachable (a lookup miss).

`docs/FORK-DELTA.md` (Known costs item 11) states that for a control character outside the nine spiked, "a
label is a lookup miss". That is inaccurate for the collision case above, and the claim rests on a
behaviour nobody measured. The same gap exists for element names in `normalise` (only `\n`, `\t`, `\r` are
translated), but there the pair is left for `ClickHouseColumn.of`, the element fails to pair, and the
fallback WARN fires, so that side fails open.
**Fix:** Translate every escape the server writes, and refuse an escape that is not a known one so a
mis-read can never become a silent key:
```java
private static char unescapedLabelChar(char escaped) {
    switch (escaped) {
        case 'n': return '\n';
        case 't': return '\t';
        case 'r': return '\r';
        case 'b': return '\b';
        case 'f': return '\f';
        case '0': return '\0';
        case '\\':
        case '\'': return escaped;
        default: throw malformedEnumType(); // unknown escape: map the table as unreadable, not as a wrong key
    }
}
```
Add a `ColumnTest` case with `Enum8('\\b' = 1, 'b' = 2)` (the type text as the server prints it) asserting both
keys survive, and add `bs`-style spike entries for backspace, form feed and NUL to `RawNameMatrix` so the
assumption becomes a measurement. Correct the Known costs wording once measured.

### WR-02: `describeTableV1` lets the new enum parse failure escape and abort the mapping of the whole database

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:505`
**Issue:** `describeTableV2` catches `Exception` and returns `null`, so one undescribable table is skipped.
`describeTableV1` (the default client, per the CHANGELOG) catches only `ClickHouseException | IOException`.
This phase adds a new throw site in `Column.extractEnumValues` (`malformedEnumType()`, an
`IllegalArgumentException`, nine places) that is reached from `DescribeTableBuilder.addRow` inside
`readDescribeRows`. On V1 it propagates out of `describeTable`, out of `extractTablesMapping`'s per-table
loop (line ~613) and out of `ClickHouseWriter.updateMapping`, so one table with an enum type the scan
rejects stops every later table in that database from being described, and on startup it fails
`ClickHouseWriter.start()`. V2 returns `null` for the same input. The phase's own premise is that both
clients behave the same on the same lines; this keeps one divergence, and the phase makes it reachable by
a new exception. The same applies to the pre-existing `NumberFormatException`/`RuntimeException` paths in
`Column.extractColumn`.
**Fix:** Make the V1 path total the way V2 is:
```java
} catch (ClickHouseException | IOException | RuntimeException e) {
    LOGGER.error(String.format("Exception when running describeTable %s", describeQuery), e);
    return null;
}
```
Add a V1 unit or live test with a deliberately unreadable enum type asserting `describeTable` returns `null`
and a later table is still mapped.

### WR-03: The new JSON-layer decoder's `\uXXXX` path and the malformed-enum branches have no test

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java:163-165,174-185`; `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java:383-447`
**Issue:** `unescapeJsonLayer` is a hand-written decoder on the type-text path of every Tuple and every enum,
and the server emits `\u00XX` for control characters below U+0020 other than `\b \f \n \r \t`. No test
exercises it: the matrix `ctl` entry deliberately uses `_` ("control with no escaped character"), the `na`
entry is written raw by the server, and a search of `src/test` finds no `\u00` row text fed to `normalise`,
`fromJsonRow` or `extractColumn`. So `isUnicodeEscape` (uppercase hex, a non-hex digit, fewer than four
digits at the end of the string, a non-ASCII digit rejected by `MAX_ASCII`) and the copy-unchanged branch
for an unknown escape are unpinned. Likewise none of the nine `throw malformedEnumType()` sites in
`extractEnumValues` is asserted by any test (`ColumnTest` only covers the success path and lookup misses),
yet the message `Enum type could not be read` is a deliberate contract (the label must stay out of it).
**Fix:** Add Docker-free cases to `DeclaredNullabilityTest` and `ColumnTest`: `unescapeJsonLayer` on `A`,
`é` and `é` (equal), `\u00zz`, `\u004` (truncated), `\q`, a lone trailing backslash and a surrogate
pair; and one `assertThrows(IllegalArgumentException.class, ...)` per malformed enum shape (no `(`, missing
quote, missing `=`, no digits, missing `)`, trailing backslash, an overflowing code) that also asserts the
message equals `Enum type could not be read`.

### WR-04: `legacyEnumLabelKeyFailsLoudly` passes on any failure, not on the enum lookup miss it names

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/RawIdentifierNameLiveTest.java:341`
**Issue:** The sibling element test passes a cause substring (`missingColumnText`) so that it cannot pass
for an unrelated reason, and its Javadoc says exactly that. The enum pin calls
`NullableRoundTrip.assertPutFails(props, List.of(enumRecord(table, legacyKey)), chc, table)` with no
substring, so it is green for a connection error, a timeout or a table-creation fault, which is also what a
flaky Docker run produces. The pin is meant to prove "an enum value in the earlier key form is a lookup
miss" (CHANGELOG, FORK-DELTA item 11).
**Fix:** Pass the expected text, which `Column.convertEnumValues` produces:
```java
NullableRoundTrip.assertPutFails(
        props, List.of(enumRecord(table, legacyKey)), chc, table, "Enum label not found for column e");
```

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

---

_Reviewed: 2026-10-23T12:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
