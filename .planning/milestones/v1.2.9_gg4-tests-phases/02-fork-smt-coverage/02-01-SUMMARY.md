---
phase: 02-fork-smt-coverage
plan: 01
subsystem: testing
tags: [junit5, kafka-connect-smt, debezium, table-operation-filter, test-helpers]

# Dependency graph
requires:
  - phase: 01-test-infrastructure-green-baseline
    provides: Docker-free `testUnit` Gradle task (excludeTags("clickhouse")), green baseline suite
provides:
  - "transforms.TransformTestHelpers — shared, fork-only unit-test record builders and metadata-preservation assertion, package-private in `transforms`"
  - "TableOperationFilter.configure() D-03 fix — clears targetTables/skippedOperations before re-parsing config, so a second configure() call fully replaces the first"
  - "TOF-01..TOF-05 pinned by named, passing unit tests in TableOperationFilterTest"
affects: [02-02-headers-to-value, 02-03-header-to-value, 02-04-convert-to-map-or-json, 02-05-sink-level-smt-tests]

# Actuals (#2632)
actuals:
  tokens: 4500
  tasks: 3
  commits: 3

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Shared package-private test-helper class (TransformTestHelpers) holding SinkRecord builders (schema/schemaless/tombstone/Debezium) and a metadata-preservation assertion, consumed across the four SMT unit-test files without an import (same package)."
    - "Non-ASCII test data built from Unicode code points (`new String(new int[]{...}, 0, n)`) instead of literal characters or `\\uXXXX` escapes in source, since compileTestJava pins no source encoding."
    - "@DisplayName requirement-ID prefix convention (`TOF-0N: ...`) for greppable REQUIREMENTS.md-to-test traceability."

key-files:
  created:
    - src/test/java/transforms/TransformTestHelpers.java
  modified:
    - src/test/java/transforms/TableOperationFilterTest.java
    - src/main/java/com/clickhouse/kafka/connect/transforms/TableOperationFilter.java

key-decisions:
  - "Non-ASCII test data for the TOF-01 encoding edge is built at runtime from `int[]` Unicode code points rather than `\\u` escape literals in source — the tool chain used to author this file could not reliably reproduce literal backslash-u escape sequences without silently decoding them into real Unicode bytes, and this codebase's build does not pin `compileTestJava`'s source encoding, so the code-point construction is the only fully verifiable way to keep the file's raw bytes ASCII-only while still exercising exact-match table filtering against a non-ASCII table name."
  - "Applied the D-03 fix exactly as specified: two `clear()` calls, no other change to TableOperationFilter.java — kept the diff to precisely 2 added lines, 0 removed, matching the plan's rebasability constraint."

patterns-established:
  - "Debezium envelope builders (`debeziumRecord`/`schemalessDebeziumRecord`) in TransformTestHelpers are the canonical fixtures plan 02-05's sink-level tests will also use."

requirements-completed: [TOF-01, TOF-02, TOF-03, TOF-04, TOF-05]

coverage:
  - id: D1
    description: "Shared TransformTestHelpers unit-test helper (record builders + metadata-preservation assertion), package-private and Docker-free"
    verification:
      - kind: unit
        ref: "src/test/java/transforms/TableOperationFilterTest.java (compiles against and calls TransformTestHelpers.debeziumRecord/schemalessDebeziumRecord)"
        status: pass
    human_judgment: false
  - id: D2
    description: "TOF-05: config() returns the shared CONFIG_DEF instance, close() is idempotent, and reconfiguring an instance (D-03 fix) fully replaces the prior filter set"
    requirement: "TOF-05"
    verification:
      - kind: unit
        ref: "transforms.TableOperationFilterTest#testConfigReturnsSharedConfigDefInstance"
        status: pass
      - kind: unit
        ref: "transforms.TableOperationFilterTest#testCloseCanBeCalledRepeatedly"
        status: pass
      - kind: unit
        ref: "transforms.TableOperationFilterTest#testReconfigureReplacesPriorConfiguration"
        status: pass
    human_judgment: false
  - id: D3
    description: "TOF-01: truncate (t) operations are dropped for listed tables while c/u/d/r pass through; empty tables list disables filtering; non-ASCII and case-sensitive table-name matching edges"
    requirement: "TOF-01"
    verification:
      - kind: unit
        ref: "transforms.TableOperationFilterTest#testTruncateDroppedOtherOperationsPassThrough"
        status: pass
      - kind: unit
        ref: "transforms.TableOperationFilterTest#testEmptyTablesListMeansNoFiltering"
        status: pass
      - kind: unit
        ref: "transforms.TableOperationFilterTest#testNonAsciiTableNameMatchesByExactEquality"
        status: pass
      - kind: unit
        ref: "transforms.TableOperationFilterTest#testTableMatchingIsCaseSensitive"
        status: pass
    human_judgment: false
  - id: D4
    description: "TOF-02: multiple configured tables filter independently, and surrounding whitespace in the tables config is trimmed"
    requirement: "TOF-02"
    verification:
      - kind: unit
        ref: "transforms.TableOperationFilterTest#testMultipleTablesFilterIndependently"
        status: pass
      - kind: unit
        ref: "transforms.TableOperationFilterTest#testTablesConfigWhitespaceIsTrimmed"
        status: pass
    human_judgment: false
  - id: D5
    description: "TOF-03: configure() never throws for invalid/empty/whitespace/multi-char skipped.operations tokens, and records with those ops pass through untouched"
    requirement: "TOF-03"
    verification:
      - kind: unit
        ref: "transforms.TableOperationFilterTest#testInvalidSkippedOperationsTokenIsIgnored (parameterized: x, r, \"\", \"   \", cu)"
        status: pass
      - kind: unit
        ref: "transforms.TableOperationFilterTest#testMixedSkippedOperationsListKeepsOnlyValidToken"
        status: pass
    human_judgment: false
  - id: D6
    description: "TOF-04: schemaless (Map-valued) Debezium envelopes, including nested source maps, always pass through apply() unfiltered"
    requirement: "TOF-04"
    verification:
      - kind: unit
        ref: "transforms.TableOperationFilterTest#testSchemalessDebeziumEnvelopePassesThroughUnfiltered"
        status: pass
      - kind: unit
        ref: "transforms.TableOperationFilterTest#testSchemalessDebeziumEnvelopeWithNestedSourcePassesThroughUnfiltered"
        status: pass
    human_judgment: false

# Metrics
duration: 10min
completed: 2026-09-16
status: complete
---

# Phase 2 Plan 1: TableOperationFilter Unit Coverage + Shared Test Helper Summary

**All five TableOperationFilter unit-level requirements (TOF-01..05) pinned by 17 new named tests, plus the shared `TransformTestHelpers` builder class and a two-line `configure()` reconfigure fix (D-03).**

## Performance

- **Duration:** 10 min
- **Started:** 2026-09-16T17:19:24Z
- **Completed:** 2026-09-16T17:29:08Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments
- New `transforms.TransformTestHelpers` shared, package-private, Docker-free unit-test helper with Debezium/schema/schemaless record builders and a metadata-preservation assertion, consumed by this and the three other fork SMT unit-test plans (02-02, 02-03, 02-04).
- Fixed `TableOperationFilter.configure()` (D-03): `targetTables`/`skippedOperations` are now cleared before re-parsing config, so a second `configure()` call on one instance fully replaces the first configuration instead of unioning with it. Exactly 2 lines added, 0 removed.
- All five unit-level `TableOperationFilter` requirements (TOF-01 through TOF-05) pinned with 17 new tests plus the pre-existing 9, for 26 total in `TableOperationFilterTest` — truncate filtering, multi-table filtering, invalid/empty/whitespace `skipped.operations` tokens, schemaless pass-through, and the `config()`/`close()`/reconfigure lifecycle.
- Full Docker-free `testUnit` suite green: 86 tests, 0 failed, 2 pre-existing skips unrelated to this plan.

## Task Commits

Each task was committed atomically:

1. **Task 1: End-to-end slice — shared helper, D-03 fix, and TOF-05 proven through testUnit** - `31ee572` (test)
2. **Task 2: TOF-01 truncate filtering and TOF-02 multi-table filtering, with the empty and encoding edges** - `c7f7118` (test)
3. **Task 3: TOF-03 invalid-token matrix and TOF-04 schemaless pass-through** - `018fe97` (test)

**Plan metadata:** (this commit)

## Files Created/Modified
- `src/test/java/transforms/TransformTestHelpers.java` - New shared, package-private test-helper class with `schemaRecord`, `schemaRecordAlternateSchema`, `schemalessRecord`, `tombstone`, `debeziumRecord`, `schemalessDebeziumRecord`, and `assertMetadataPreserved`.
- `src/test/java/transforms/TableOperationFilterTest.java` - Grew from 9 to 26 tests, pinning TOF-01 through TOF-05.
- `src/main/java/com/clickhouse/kafka/connect/transforms/TableOperationFilter.java` - `configure()` now clears `targetTables`/`skippedOperations` before re-parsing config (D-03), 2 lines added, 0 removed.

## Decisions Made
- Built the TOF-01 non-ASCII table-name test data from `int[]` Unicode code points at runtime (`new String(new int[]{0x0431, ...}, 0, n)`) instead of `\u` escape literals in the source text. During authoring, literal backslash-`u` escape sequences typed into the Edit/Write tool parameters were silently decoded into real non-ASCII bytes before landing in the file (confirmed via a byte-level scan), which would have violated the plan's "no raw non-ASCII byte" prohibition. The code-point construction sidesteps that tool behavior entirely while still exercising exact-match table filtering against a genuinely non-ASCII table name at runtime, and keeps the file's raw bytes 100% ASCII (verified: zero bytes above 0x7F in the final file).
- Applied the D-03 fix exactly as the plan and CONTEXT.md specified — two `clear()` calls immediately after `AbstractConfig` construction, nothing else — to keep the diff to the plan-mandated "exactly 2 added lines, 0 removed."

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Non-ASCII bytes leaked into source during test-data authoring, fixed before commit**
- **Found during:** Task 2 (TOF-01 encoding edge)
- **Issue:** The intended `б`-style escape literals for the Cyrillic test data were converted to actual UTF-8 non-ASCII bytes by the editing tool chain when first written, both in a string literal and in a comment (also in an unrelated Javadoc em-dash in `TransformTestHelpers.java`), which would have failed the plan's own automated non-ASCII byte-scan verification and violated the "no raw non-ASCII character" prohibition.
- **Fix:** Rewrote the non-ASCII test data using `new String(new int[]{...}, 0, n)` Unicode code-point construction (no escape-sequence typing involved) and replaced the stray em-dash with a plain hyphen. Re-ran the byte-level scan (`b>127` check) on both files after the fix — zero non-ASCII bytes remain.
- **Files modified:** `src/test/java/transforms/TableOperationFilterTest.java`, `src/test/java/transforms/TransformTestHelpers.java`
- **Verification:** `python3 -c '... bad=[i for i,b in enumerate(s) if b>127] ...'` returns `count 0` for both files; the affected test (`testNonAsciiTableNameMatchesByExactEquality`) still passes, proving the code-point string still round-trips correctly through `TableOperationFilter`'s exact-string-equality matching.
- **Committed in:** `c7f7118` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 bug, caught before commit — no impact on the final committed state, which is fully compliant with the plan's ASCII-only prohibition).
**Impact on plan:** None on scope; this was a self-correction during authoring, not a change to what the plan required. No non-ASCII bytes exist in any committed file for this plan.

## Issues Encountered
- `.planning/milestone.lock` was already staged (by a process prior to this executor's dispatch) when Task 1's commit was made; it rode along in that commit alongside the three intended Task 1 files. This was not introduced by this plan's work and required no action — it is a legitimate `.planning` artifact, not an accidental inclusion of unrelated source changes.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- `TransformTestHelpers` is ready for plans 02-02 (`HeadersToValue`), 02-03 (`HeaderToValue`), and 02-04 (`ConvertToMapOrJson`) to consume directly (same package, no import needed).
- `TableOperationFilter`'s D-03 reconfigure fix is landed and pinned; no further main-code changes to this class are anticipated in this phase.
- TOF-06 (sink-level, ClickHouse-backed) remains for plan 02-05, which can reuse `TransformTestHelpers.debeziumRecord` as its fixture builder per the phase CONTEXT.md D-06 decision.
- No blockers for the remaining Phase 2 plans.

---
*Phase: 02-fork-smt-coverage*
*Completed: 2026-09-16*

## Self-Check: PASSED

- `src/test/java/transforms/TransformTestHelpers.java` — FOUND
- `src/test/java/transforms/TableOperationFilterTest.java` — FOUND
- `src/main/java/com/clickhouse/kafka/connect/transforms/TableOperationFilter.java` — FOUND
- Commit `31ee572` — FOUND
- Commit `c7f7118` — FOUND
- Commit `018fe97` — FOUND
- Re-ran plan-level `<verification>`: `testUnit --tests 'transforms.TableOperationFilterTest'` → 26 tests, 0 failures; full `testUnit` → 86 tests, 0 failed, 2 pre-existing skips; all five `TOF-0N:` `@DisplayName` prefixes present; `TransformTestHelpers` package-private with no `extends ClickHouseBase`/`@Tag`; `TableOperationFilter.java` diff is exactly 2 added, 0 removed lines.
