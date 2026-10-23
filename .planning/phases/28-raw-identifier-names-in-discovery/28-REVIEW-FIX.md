---
phase: 28-raw-identifier-names-in-discovery
fixed_at: 2026-10-23T16:10:00Z
review_path: .planning/phases/28-raw-identifier-names-in-discovery/28-REVIEW.md
iteration: 1
findings_in_scope: 4
fixed: 4
skipped: 0
status: all_fixed
---

# Phase 28: Code Review Fix Report

**Fixed at:** 2026-10-23T16:10:00Z
**Source review:** .planning/phases/28-raw-identifier-names-in-discovery/28-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 4 (fix_scope critical_warning; IN-01 to IN-03 out of scope)
- Fixed: 4
- Skipped: 0

**Verification ran in:** an isolated git worktree (`.claude/worktrees/rf-28-...`, now removed), then the commits were fast-forwarded onto `sync/v1.7.0`. The live tests ran against Docker (colima profile `vz`, ClickHouse `latest`, default client V2 for the matrix tests). The worktree run is not reproducible after teardown, but the same commits are on the branch tip and the commands below can be re-run from the main checkout.

## Fixed Issues

### WR-01: Enum label scan reads the SQL escapes `\0`, `\b` and `\f` as the letters 0, b and f

**Files modified:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java`, `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java`, `docs/FORK-DELTA.md`
**Commits:** a8f50d87 (code and tests), 54447ab4 (FORK-DELTA wording)
**Status:** fixed: requires human verification (logic change in the enum label scan; the server's escape set is still not measured against a live server)
**Applied fix:** `unescapedLabelChar` now translates `b`, `f` and `0` as well, keeps `\\` and `\'`, and throws the existing `Enum type could not be read` error for any other escape instead of returning the bare letter. New `ColumnTest` cases pin that `'\b'`/`'b'`, `'\f'`/`'f'` and `'\0'`/`'0'` stay six distinct keys and that an unknown escape is refused. The FORK-DELTA Known costs item 11 wording was corrected: it no longer says such a label is a lookup miss. The reviewer's suggestion to add backspace/form feed/NUL spike entries to `RawNameMatrix` was not done (it needs a live measurement of the server's output, outside a targeted fix); the "not measured" wording stays.

### WR-02: `describeTableV1` lets the new enum parse failure escape and abort the mapping of the whole database

**Files modified:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java`, `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumMockServerTest.java` (new)
**Commit:** ae97c496
**Applied fix:** `describeTableV1` now catches `ClickHouseException | IOException | RuntimeException`, logs, and returns `null`, as V2 already does. The new Docker-free test serves DESCRIBE rows from an in-process HTTP stub and asserts, for client V1 and client V2, that a table with an unreadable enum type returns `null` and the next table is still described. Confirmed the V1 case fails without the fix.

### WR-03: The new JSON-layer decoder's `\uXXXX` path and the malformed-enum branches have no test

**Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/UnescapeJsonLayerTest.java` (new), `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java`
**Commit:** 16950de4
**Applied fix:** Tests only. `UnescapeJsonLayerTest` covers `A`, lower and upper case hex, a `é`/`é` equivalence, a surrogate pair, `\\`, `\/`, `\"`, a tab, and copy-unchanged cases (non-hex digits, truncated digits, bare `\u`, Arabic-Indic digits, `\q`, a lone trailing backslash). `ColumnTest` gains a parameterized case over ten malformed enum shapes (no list, no quote, unterminated label, no separator, no digits, sign only, no closing parenthesis, junk after the code, trailing backslash, overflowing code), each asserting the message equals `Enum type could not be read`.

### WR-04: `legacyEnumLabelKeyFailsLoudly` passes on any failure, not on the enum lookup miss it names

**Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/RawIdentifierNameLiveTest.java`
**Commit:** 2575092a
**Applied fix:** The pin now passes cause substrings to `assertPutFails`. The reviewer's suggested text (`Enum label not found for column e`) was wrong: the writer withholds that message, and the live run showed the cause chain holds ``Failed to write column `e` `` and `could not be written as ClickHouse type Enum8: java.lang.IllegalArgumentException`. The test asserts those two texts (with a comment saying why). All nine parameterized entries pass live.

## Verification

- `./gradlew test --tests "*ColumnTest" --tests "*UnescapeJsonLayerTest" --tests "*DeclaredNullabilityTest"`: passed (ColumnTest 50, UnescapeJsonLayerTest 16).
- `./gradlew test --tests "*DescribeUnreadableEnumMockServerTest" --tests "*ClickHouseReplicaPinningMockServerTest"`: passed.
- Live, Docker: `RawIdentifierNameLiveTest` (73), `RawNameMatrixLiveTest` (25), `NullableEnumRoundTripLiveTest` (6), plus `RawNameMatrixFixturesUnitTest` (50) and `ForkCoverageScopeUnitTest` (3): all passed.
- The full `./gradlew test` suite was not run. `spotlessCheck` flagged none of the edited files.
- `gradlew.bat` shows as modified in the main checkout; that was already the case before this run and was not committed.

---

_Fixed: 2026-10-23T16:10:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
