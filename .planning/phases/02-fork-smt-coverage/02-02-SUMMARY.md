---
phase: 02-fork-smt-coverage
plan: 02
subsystem: testing
tags: [junit5, kafka-connect-smt, header-to-value, schema-cache, test-helpers]

# Dependency graph
requires:
  - phase: 02-fork-smt-coverage
    provides: "transforms.TransformTestHelpers shared, package-private unit-test record builders and metadata-preservation assertion (02-01)"
provides:
  - "HeaderToValue D-02 fix — sourceSchema field guards the cached valueSchema, rebuilding it when the incoming record's schema instance changes, closing a data-loss path for mid-stream Debezium schema evolution"
  - "HDR-01..HDR-06 pinned by 22 new named unit tests in HeaderToValueTest (24 total)"
affects: [02-05-sink-level-smt-tests]

# Actuals (#2632)
actuals:
  tokens: 6150
  tasks: 3
  commits: 3

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Stale-schema cache guard: a private `sourceSchema` field alongside the cached `valueSchema`, rebuilt via reference comparison (`!=`, never `.equals()`) whenever the incoming record's schema instance differs — the same pattern the fork's HeadersToValue carries the identical bug for."
    - "Non-ASCII test data built from `int[]` Unicode code points at runtime (`new String(new int[]{...}, 0, n)`), continuing the 02-01 convention, since the editing tool chain silently decodes literal backslash-u escape text into real non-ASCII bytes."
    - "@DisplayName requirement-ID prefix convention (`HDR-0N: ...`) applied to every new test method."

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java
    - src/test/java/transforms/HeaderToValueTest.java

key-decisions:
  - "Built the HDR-06 Cyrillic/CJK encoding-edge test value from int[] Unicode code points at runtime rather than \\u escape literals in source, following the 02-01 convention that avoids the tool chain's escape-decoding hazard, and verified with a zero-non-ASCII-byte scan of the final file."
  - "HDR-04's 'empty edge' assertion (added field present-and-null when its header is missing) is intentionally near-duplicate in body to the HDR-03 'field new to schema' test, since both requirements independently require the identical present-and-null outcome for different reasons (HDR-03: missing-header semantics; HDR-04: buildSchema() always adds the field) — kept both distinct tests per the plan's explicit instruction rather than merging them."

patterns-established:
  - "The HeaderToValue D-02 guard is a verbatim mirror of the fix HeadersToValue will need for the identical bug; a future plan touching HeadersToValue.applyWithSchema() should reuse this exact shape."

requirements-completed: [HDR-01, HDR-02, HDR-03, HDR-04, HDR-05, HDR-06]

coverage:
  - id: D1
    description: "D-02: stale valueSchema cache is rebuilt when the incoming record's schema instance changes, closing a data-loss path for mid-stream Debezium schema evolution"
    verification:
      - kind: unit
        ref: "transforms.HeaderToValueTest#testStaleSchemaCacheRebuildsOnSchemaChange"
        status: pass
    human_judgment: false
  - id: D2
    description: "HDR-01: absent fields/headers config keys throw ConfigException; a genuine size mismatch in either direction throws DataException with the 'must match the number of headers' message"
    requirement: "HDR-01"
    verification:
      - kind: unit
        ref: "transforms.HeaderToValueTest#testConfigureThrowsConfigExceptionWhenFieldsAbsent"
        status: pass
      - kind: unit
        ref: "transforms.HeaderToValueTest#testConfigureThrowsConfigExceptionWhenHeadersAbsent"
        status: pass
      - kind: unit
        ref: "transforms.HeaderToValueTest#testConfigureThrowsDataExceptionWhenFieldsOutnumberHeaders"
        status: pass
      - kind: unit
        ref: "transforms.HeaderToValueTest#testConfigureThrowsDataExceptionWhenHeadersOutnumberFields"
        status: pass
    human_judgment: false
  - id: D3
    description: "HDR-02: a header added three times under one name copies its last value into the target field, on the schema and schemaless paths"
    requirement: "HDR-02"
    verification:
      - kind: unit
        ref: "transforms.HeaderToValueTest#testMultiValuedHeaderLastWinsSchemaPath"
        status: pass
      - kind: unit
        ref: "transforms.HeaderToValueTest#testMultiValuedHeaderLastWinsSchemalessPath"
        status: pass
    human_judgment: false
  - id: D4
    description: "HDR-03: a missing or null-valued header leaves the target field untouched, on both the schema and schemaless paths"
    requirement: "HDR-03"
    verification:
      - kind: unit
        ref: "transforms.HeaderToValueTest#testMissingHeaderLeavesNewFieldPresentAndNull"
        status: pass
      - kind: unit
        ref: "transforms.HeaderToValueTest#testMissingHeaderLeavesExistingFieldUnchanged"
        status: pass
      - kind: unit
        ref: "transforms.HeaderToValueTest#testNullValuedHeaderLeavesFieldUntouched"
        status: pass
      - kind: unit
        ref: "transforms.HeaderToValueTest#testMissingOrNullHeaderLeavesKeyAbsentSchemalessPath"
        status: pass
    human_judgment: false
  - id: D5
    description: "HDR-04: a configured field absent from the original schema is added as Schema.OPTIONAL_STRING_SCHEMA, stays present-and-null when its header never arrives, and field-name matching is exact case-sensitive String equality"
    requirement: "HDR-04"
    verification:
      - kind: unit
        ref: "transforms.HeaderToValueTest#testAddedFieldIsOptionalString"
        status: pass
      - kind: unit
        ref: "transforms.HeaderToValueTest#testAddedFieldPresentAndNullWhenHeaderMissing"
        status: pass
      - kind: unit
        ref: "transforms.HeaderToValueTest#testFieldNameMatchingIsCaseSensitive"
        status: pass
    human_judgment: false
  - id: D6
    description: "HDR-05: record metadata (topic, partition, key, key schema, timestamp, headers) survives apply() on the schema and schemaless paths"
    requirement: "HDR-05"
    verification:
      - kind: unit
        ref: "transforms.HeaderToValueTest#testMetadataPreservedSchemaPath"
        status: pass
      - kind: unit
        ref: "transforms.HeaderToValueTest#testMetadataPreservedSchemalessPath"
        status: pass
    human_judgment: false
  - id: D7
    description: "HDR-06: non-string header values are coerced via toString(), the empty-string and zero-value edges are copied rather than treated as missing, an escaped-unicode value round-trips byte-identically, and config()/close() lifecycle contracts hold"
    requirement: "HDR-06"
    verification:
      - kind: unit
        ref: "transforms.HeaderToValueTest#testNonStringHeaderValuesCoercedSchemaPath"
        status: pass
      - kind: unit
        ref: "transforms.HeaderToValueTest#testNonStringHeaderValuesCoercedSchemalessPath"
        status: pass
      - kind: unit
        ref: "transforms.HeaderToValueTest#testEmptyStringAndZeroValuedHeadersAreCopied"
        status: pass
      - kind: unit
        ref: "transforms.HeaderToValueTest#testEscapedUnicodeHeaderValueCopiedUnchanged"
        status: pass
      - kind: unit
        ref: "transforms.HeaderToValueTest#testConfigReturnsSharedConfigDefInstance"
        status: pass
      - kind: unit
        ref: "transforms.HeaderToValueTest#testCloseCanBeCalledRepeatedly"
        status: pass
    human_judgment: false

# Metrics
duration: 20min
completed: 2026-09-16
status: complete
---

# Phase 2 Plan 2: HeaderToValue Unit Coverage + Stale-Schema Cache Fix Summary

**All six HeaderToValue unit-level requirements (HDR-01..06) pinned by 22 new named tests, plus a D-02 stale-schema-cache guard that closes a real mid-stream Debezium data-loss path.**

## Performance

- **Duration:** 20 min
- **Started:** 2026-09-16T17:53:49Z
- **Completed:** 2026-09-16T18:13:49Z
- **Tasks:** 3
- **Files modified:** 2

## Accomplishments
- Fixed the stale `valueSchema` cache in `HeaderToValue.applyWithSchema()` (D-02): a new `sourceSchema` field is compared by reference against the incoming record's schema, rebuilding the cached schema whenever it changes instead of silently reusing the first record's schema forever. Exactly one field, one widened condition, one nulling line in `close()` — no new method, no signature change.
- All six unit-level `HeaderToValue` requirements (HDR-01 through HDR-06) pinned with 22 new tests plus the pre-existing 2, for 24 total in `HeaderToValueTest` — config-error split, multi-valued last-wins headers, missing/null header handling, added-optional-field schema behavior, metadata preservation, and value coercion/lifecycle/encoding edges.
- Removed the duplicated private `generateSampleRecord` builder, replacing it with the shared `TransformTestHelpers` builders from plan 02-01.
- Full Docker-free `testUnit` suite green: 108 tests, 0 failed, 2 pre-existing skips unrelated to this plan.

## Task Commits

Each task was committed atomically:

1. **Task 1: End-to-end slice — D-02 schema-cache guard, its stale-schema test, and HDR-05 on the shared helper** - `2b7d387` (test)
2. **Task 2: HDR-01 config-error split and HDR-06 value coercion, lifecycle and encoding edges** - `c405925` (test)
3. **Task 3: HDR-02 multi-valued headers, HDR-03 missing/null headers, HDR-04 added optional field** - `af57220` (test)

**Plan metadata:** (this commit)

## Files Created/Modified
- `src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java` - Added the `sourceSchema` field and the D-02 stale-schema-cache guard in `applyWithSchema()`; nulled in `close()`. Exactly the change the plan specified: no new method, no signature change.
- `src/test/java/transforms/HeaderToValueTest.java` - Grew from 2 to 24 tests, pinning HDR-01 through HDR-06 and the D-02 regression, using the shared `TransformTestHelpers` builders throughout.

## Decisions Made
- Built the HDR-06 Cyrillic/CJK encoding-edge test value from `int[]` Unicode code points at runtime, continuing the 02-01 convention that avoids the editing tool chain's escape-decoding hazard; verified with a zero-non-ASCII-byte scan of the final file.
- Kept HDR-04's "empty edge" test distinct from HDR-03's "field new to schema" test even though their bodies are near-identical — both requirements independently require the same present-and-null outcome for different reasons, and the plan explicitly calls for both.

## Deviations from Plan

None - plan executed exactly as written. The D-02 fix and the escaped-unicode test-data approach were both explicit, pre-locked plan requirements (CONTEXT.md D-02, and the established 02-01 non-ASCII authoring pattern), not ad hoc deviations.

## Issues Encountered
- A code comment describing the non-ASCII authoring convention initially spelled out the literal two-character sequence "backslash" + "u" inside a Java comment, which the `javac` unicode-escape preprocessor treats as the start of a unicode escape sequence even inside comments, producing an `illegal unicode escape` compile error. Reworded the comment to avoid that literal sequence; no functional change, re-verified the file compiles and all tests still pass.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- `HeaderToValue` now has full unit-level coverage (HDR-01..06) and the same D-02 stale-schema-cache fix pattern is ready to be mirrored onto `HeadersToValue` if a future plan in this phase targets that class (per 02-CONTEXT.md, the two classes carry the identical bug).
- No blockers for the remaining Phase 2 plans (02-03 `HeadersToValue`, 02-04 `ConvertToMapOrJson`).
- Sink-level coverage for `HeaderToValue` (HDR-07) was already delivered by plan 02-05, which ran ahead of this plan in wave scheduling and already proved the header-derived field lands correctly in a ClickHouse `String` column.

---
*Phase: 02-fork-smt-coverage*
*Completed: 2026-09-16*

## Self-Check: PASSED

- `[ -f src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java ]` -> FOUND
- `[ -f src/test/java/transforms/HeaderToValueTest.java ]` -> FOUND
- `git log --oneline --all | grep -q 2b7d387` -> FOUND
- `git log --oneline --all | grep -q c405925` -> FOUND
- `git log --oneline --all | grep -q af57220` -> FOUND
- Re-ran plan-level `<verification>`: `testUnit --tests 'transforms.HeaderToValueTest'` -> 24 tests, 0 failures; full `testUnit --rerun` -> 108 tests, 0 failed, 2 pre-existing skips; all six `HDR-0N:` `@DisplayName` prefixes present; `HeaderToValue.java` declares `sourceSchema`, compares by reference (`!=`), no `.equals()` usage; zero bytes above 0x7F in `HeaderToValueTest.java`; `HeaderToValue.java` still declares exactly `configure`, `apply`, `applySchemaless`, `applyWithSchema`, `buildSchema`, `config`, `close`.
