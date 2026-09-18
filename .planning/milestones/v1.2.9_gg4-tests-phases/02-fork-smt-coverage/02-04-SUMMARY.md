---
phase: 02-fork-smt-coverage
plan: 04
subsystem: testing
tags: [junit5, kafka-connect-smt, convert-to-map-or-json, jackson, gson, bytebuffer]

# Dependency graph
requires:
  - phase: 02-fork-smt-coverage
    provides: "transforms.TransformTestHelpers (plan 02-01) — schemaRecord/assertMetadataPreserved consumed directly by CONV-04"
provides:
  - "D-01 recursion fix: ConvertToMapOrJson.convertFieldValue() now recurses into List and Map values, converting nested Structs to plain maps and applying bytes-to-base64 at every depth — closes the reflection-over-Struct-internals bug"
  - "CONV-01..CONV-07 pinned by 24 new named unit tests in ConvertToMapOrJsonTest (13 pre-existing tests untouched)"
affects: []

# Actuals (#2632)
actuals:
  tokens: 6547
  tasks: 3
  commits: 3

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "@ParameterizedTest with @ValueSource(strings = {\"jackson\", \"gson\"}) for the one genuine CONV-01 matrix (D-11), plain @Test everywhere else in this file."
    - "Private encodeViaTransform(schema, config, ByteBuffer) test helper local to ConvertToMapOrJsonTest, factoring the construct-configure-apply-extract shape shared by all CONV-06 ByteBuffer cases."

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson.java
    - src/test/java/transforms/ConvertToMapOrJsonTest.java

key-decisions:
  - "Applied the D-01 fix exactly as locked in CONTEXT.md: List and Map recursion branches added above the existing Struct branch and above the bytesToBase64 block in convertFieldValue(), with only the two required imports (java.util.ArrayList, java.util.List) added — no other change to the file."
  - "CONV-05's upper-case-JACKSON test carries an inline comment recording that the case-sensitivity rejection is pinned as-is (D-13) and that widening ConfigDef.ValidString is out of scope, so a future reader does not mistake the assertion for an accidental regression."

patterns-established: []

requirements-completed: [CONV-01, CONV-02, CONV-03, CONV-04, CONV-05, CONV-06, CONV-07]

coverage:
  - id: D1
    description: "D-01 bug fix: Structs nested inside ARRAY and MAP fields now convert to plain Maps (not raw Structs) before reaching Jackson/Gson, at every nesting depth, with bytes.to.base64 applied consistently at every depth too"
    requirement: "CONV-02"
    verification:
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testArrayOfStructsConvertedToListOfMaps"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testMapOfStructsConvertedToMapOfMaps"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testNestedBytesInArrayAndMapBase64Encoded"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testEmptyArrayAndMapFieldsConvertToEmptyCollections"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testAdjacentEqualStructsInArrayStayDistinct"
        status: pass
    human_judgment: false
  - id: D2
    description: "CONV-04: apply() preserves record metadata (topic, partition, key, key schema, timestamp, headers) for both the map-output and JSON-output configurations"
    requirement: "CONV-04"
    verification:
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testMetadataPreservedMapOutput"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testMetadataPreservedJsonOutput"
        status: pass
    human_judgment: false
  - id: D3
    description: "CONV-01: preserve.null.values=false omits null fields from map output and from JSON for both jackson and gson; the default keeps the field present"
    requirement: "CONV-01"
    verification:
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testPreserveNullValuesFalseOmitsNullFieldFromMap"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testPreserveNullValuesFalseOmitsNullFieldFromJson (parameterized: jackson, gson)"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testDefaultPreserveNullValuesKeepsNullFieldInMap"
        status: pass
    human_judgment: false
  - id: D4
    description: "CONV-03: non-Struct record values (bare Long, String, Map, null) pass through apply() as the identical record object when output.json.enabled=false"
    requirement: "CONV-03"
    verification:
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testLongValuePassthroughWhenJsonDisabled"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testStringValuePassthroughWhenJsonDisabled"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testMapValuePassthroughWhenJsonDisabled"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testNullValuePassthrough"
        status: pass
    human_judgment: false
  - id: D5
    description: "CONV-07: config() returns the shared CONFIG_DEF instance and close() is idempotent and non-destructive to subsequent apply() calls"
    requirement: "CONV-07"
    verification:
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testConfigReturnsSharedConfigDefInstance"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testCloseIsRepeatableAndNonDestructive"
        status: pass
    human_judgment: false
  - id: D6
    description: "CONV-05: an unknown or wrong-case output.json.library value throws ConfigException (not the unreachable DataException branch), while lower-case jackson/gson configure without throwing"
    requirement: "CONV-05"
    verification:
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testUnknownJsonLibraryThrowsConfigException"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testUpperCaseJsonLibraryIsRejected"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testLowerCaseLibrariesConfigureWithoutThrowing"
        status: pass
    human_judgment: false
  - id: D7
    description: "CONV-06: array-backed, direct, and sliced ByteBuffers over the same bytes all base64-encode identically, including zero-length, mid-range, and start/end-boundary slice edges"
    requirement: "CONV-06"
    verification:
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testArrayDirectAndSlicedByteBuffersEncodeIdentically"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testEmptyByteBuffersEncodeToEmptyString"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testMiddleSliceEncodesExactSubRangeInOrder"
        status: pass
      - kind: unit
        ref: "transforms.ConvertToMapOrJsonTest#testBoundarySlicesMatchArrayBackedEncoding"
        status: pass
    human_judgment: false

# Metrics
duration: 16min
completed: 2026-09-16
status: complete
---

# Phase 2 Plan 4: ConvertToMapOrJson Full Unit Coverage + Nested-Collection Recursion Fix Summary

**Fixed the D-01 nested-Struct-in-collection bug in `ConvertToMapOrJson.convertFieldValue()` and pinned all seven unit-level requirements (CONV-01..CONV-07) with 24 new named tests, taking the file from 13 to 37 tests.**

## Performance

- **Duration:** 16 min
- **Started:** 2026-09-16T18:15:00Z (approx.)
- **Completed:** 2026-09-16T18:31:24Z
- **Tasks:** 3
- **Files modified:** 2

## Accomplishments
- Fixed the one confirmed bug in this phase's scope: `convertFieldValue()` now recurses into `List` and `Map` values, converting any nested `Struct` to a plain map (via `ArrayList`/`LinkedHashMap` branches placed above the existing `Struct` branch) before it reaches Jackson/Gson, and applying `bytes.to.base64` at every nesting depth instead of only at the top level. Exactly the two branches and two imports specified by D-01 — no other production change.
- Pinned CONV-02 with all three named edges: empty ARRAY/MAP fields convert to empty (not null) collections, ARRAY element order and MAP entry-insertion order survive conversion, and two adjacent structurally-equal Structs stay two distinct, equal (not merged/deduplicated) output maps.
- Pinned CONV-01 (null omission across `preserve.null.values` for both map output and both JSON libraries), CONV-03 (four non-Struct passthrough shapes), CONV-04 (metadata preservation for both output modes), CONV-05 (the `ConfigException` contract including its case-sensitivity quirk, with the dead `DataException` branch deliberately left untouched), CONV-06 (array-backed/direct/sliced `ByteBuffer` matrix with empty, mid-range, and boundary-slice edges), and CONV-07 (`config()`/`close()` lifecycle).
- Full Docker-free `testUnit` suite green across the whole phase: 155 tests, 0 failed, 2 pre-existing skips (unrelated to this plan).

## Task Commits

Each task was committed atomically:

1. **Task 1: End-to-end slice — D-01 nested-collection recursion, CONV-02 with its three edges, CONV-04 metadata** - `6f9d866` (test)
2. **Task 2: CONV-01 null omission across jackson and gson, CONV-03 non-Struct passthrough, CONV-07 lifecycle** - `c1afb23` (test)
3. **Task 3: CONV-05 ConfigException contract including the case-sensitivity quirk, CONV-06 ByteBuffer matrix with its three edges** - `3d8e6bf` (test)

**Plan metadata:** (this commit)

## Files Created/Modified
- `src/main/java/com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson.java` - Added `List` and `Map` recursion branches to `convertFieldValue()` (D-01 fix); added `java.util.ArrayList` and `java.util.List` imports; no other change. Diff: 19 lines added, 1 line changed.
- `src/test/java/transforms/ConvertToMapOrJsonTest.java` - Grew from 13 to 37 tests, pinning CONV-01 through CONV-07. Diff: 470 lines added.

## Decisions Made
- Applied the D-01 fix exactly as locked in CONTEXT.md: `List`/`Map` recursion branches added above the existing `Struct` branch and above the `bytesToBase64` block, with only the two required imports added.
- CONV-05's upper-case-`JACKSON` rejection test carries an inline comment recording that the behaviour is pinned as-is per D-13 (the validator is case-sensitive by design, widening it is out of scope), so the assertion reads as an intentional pin rather than an oversight.

## Deviations from Plan

None - plan executed exactly as written. All acceptance criteria for all three tasks were met on the first attempt with no fix-attempt cycles required.

## Issues Encountered

- A stale per-plan commit ledger (`.git/gsd-plan-head-before-02-04`) was found at session start, dated 2026-09-13 — left over from a superseded milestone that reused this plan id (same pattern documented in the 02-03 SUMMARY). Verified the stale SHA's commit date predated this plan's real prior commit (`da9b187`, 2026-09-16) and replaced it with the correct baseline before the first commit, so `actuals.commits` (3) reflects only this plan's work.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- `ConvertToMapOrJson` unit-level coverage (CONV-01..CONV-07) is complete; only CONV-08 (sink-level, ClickHouse-backed) remains for this SMT, covered by plan 02-05 (already committed per STATE.md).
- All four fork SMT unit-level requirement sets (TOF, HDRS, HDR, CONV) are now pinned across plans 02-01 through 02-04.
- No blockers for the remaining Phase 2 work or for Phase 3.

---
*Phase: 02-fork-smt-coverage*
*Completed: 2026-09-16*

## Self-Check: PASSED

- `src/main/java/com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson.java` — FOUND
- `src/test/java/transforms/ConvertToMapOrJsonTest.java` — FOUND
- Commit `6f9d866` — FOUND
- Commit `c1afb23` — FOUND
- Commit `3d8e6bf` — FOUND
- Re-ran plan-level `<verification>`: `testUnit --tests 'transforms.ConvertToMapOrJsonTest'` → 37 tests, 0 failures (>=28 required); full `testUnit --rerun` → 155 tests, 0 failed, 2 pre-existing skips; all seven `CONV-0N:` `@DisplayName` prefixes present; `convertFieldValue()` contains `List` branch, `LinkedHashMap`-building `Map` branch, and recurses 3 times; `ValidString.in("jackson", "gson")` and `Invalid JSON library: ` both still present in production source.
