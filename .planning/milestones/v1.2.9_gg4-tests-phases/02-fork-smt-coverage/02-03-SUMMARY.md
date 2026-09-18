---
phase: 02-fork-smt-coverage
plan: 03
subsystem: testing
tags: [junit5, kafka-connect-smt, headers-to-value, schema-cache, test-helpers]

# Dependency graph
requires:
  - phase: 02-fork-smt-coverage
    provides: "transforms.TransformTestHelpers shared, package-private unit-test record builders and metadata-preservation assertion (02-01)"
provides:
  - "HeadersToValue D-02 fix — sourceSchema field guards the cached valueSchema, rebuilding it when the incoming record's schema instance changes, closing a data-loss path for mid-stream Debezium schema evolution"
  - "HDRS-01..HDRS-08 pinned by 25 named unit tests in HeadersToValueTest"
  - "HDRS-04 requirement wording in REQUIREMENTS.md corrected to describe the two-outcome ConfigException/DataException split"
affects: []

# Actuals (#2632)
actuals:
  tokens: 7610
  tasks: 3
  commits: 3

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Stale-schema cache guard: a private sourceSchema field alongside the cached valueSchema, rebuilt via reference comparison (!=, never .equals()) whenever the incoming record's schema instance differs — the same shape 02-02 applied to HeaderToValue for the identical bug."
    - "Non-ASCII test data built from int[] Unicode code points at runtime (new String(int[], 0, n)), continuing the 02-01/02-02 convention, since the editing tool chain silently decodes literal backslash-u escape text into real non-ASCII bytes."
    - "@DisplayName requirement-ID prefix convention (HDRS-0N: ...) applied to every new test method."

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/transforms/HeadersToValue.java
    - src/test/java/transforms/HeadersToValueTest.java
    - .planning/REQUIREMENTS.md

key-decisions:
  - "Built the HDRS-07 Cyrillic/CJK encoding-edge test value from int[] Unicode code points at runtime rather than \\u escape literals in source, following the established 02-01/02-02 convention that avoids the tool chain's escape-decoding hazard; verified with a zero-non-ASCII-byte scan of the final file."
  - "Corrected a stale per-plan commit ledger at .git/gsd-plan-head-before-02-03: the file predated this milestone's phase restructuring (dated 2026-09-13, belonging to a superseded '02-rebalance-integration-repro' phase's own plan 03), so its baseline pointed at the wrong commit and would have inflated this plan's actuals.commits to 48. Reset the ledger to the true pre-plan HEAD (ca138ff, the 02-02 completion commit) before computing actuals."

patterns-established:
  - "HeadersToValue.sourceSchema mirrors HeaderToValue.sourceSchema exactly (02-02) — both fork SMTs with a lazily-cached valueSchema now guard it identically."

requirements-completed: [HDRS-01, HDRS-02, HDRS-03, HDRS-04, HDRS-05, HDRS-06, HDRS-07, HDRS-08]

coverage:
  - id: D1
    description: "D-02: stale valueSchema cache is rebuilt when the incoming record's schema instance changes, closing a data-loss path for mid-stream Debezium schema evolution"
    verification:
      - kind: unit
        ref: "transforms.HeadersToValueTest#testStaleSchemaCacheRebuildsOnSchemaChange"
        status: pass
    human_judgment: false
  - id: D2
    description: "HDRS-01: a tombstone (null value, null value schema) record is returned as the identical record object, with and without headers attached"
    requirement: "HDRS-01"
    verification:
      - kind: unit
        ref: "transforms.HeadersToValueTest#testTombstonePassesThroughUnchanged"
        status: pass
      - kind: unit
        ref: "transforms.HeadersToValueTest#testTombstoneWithHeadersPassesThroughUnchanged"
        status: pass
    human_judgment: false
  - id: D3
    description: "HDRS-02: a record with no headers gets a size-0 map in the configured field, on both the schema and schemaless paths"
    requirement: "HDRS-02"
    verification:
      - kind: unit
        ref: "transforms.HeadersToValueTest#testNoHeadersYieldsEmptyMapSchemaPath"
        status: pass
      - kind: unit
        ref: "transforms.HeadersToValueTest#testNoHeadersYieldsEmptyMapSchemalessPath"
        status: pass
    human_judgment: false
  - id: D4
    description: "HDRS-03: a null-valued header is stored as a present, null map entry on both paths, alongside an unaffected normal header"
    requirement: "HDRS-03"
    verification:
      - kind: unit
        ref: "transforms.HeadersToValueTest#testNullValuedHeaderStoredAsNullEntrySchemaPath"
        status: pass
      - kind: unit
        ref: "transforms.HeadersToValueTest#testNullValuedHeaderStoredAsNullEntrySchemalessPath"
        status: pass
    human_judgment: false
  - id: D5
    description: "HDRS-04: an absent field config key throws ConfigException from AbstractConfig construction; a blank/whitespace field value throws DataException with the 'must be specified and non-empty' message"
    requirement: "HDRS-04"
    verification:
      - kind: unit
        ref: "transforms.HeadersToValueTest#testAbsentFieldThrowsConfigException"
        status: pass
      - kind: unit
        ref: "transforms.HeadersToValueTest#testBlankFieldThrowsDataException"
        status: pass
    human_judgment: false
  - id: D6
    description: "HDRS-05: field-name collision is pinned as-is — schema path throws DataException naming the colliding field, schemaless path overwrites with no exception, including the empty-map and null-value edges, and field matching is exact-case String equality"
    requirement: "HDRS-05"
    verification:
      - kind: unit
        ref: "transforms.HeadersToValueTest#testSchemaPathFieldCollisionThrows"
        status: pass
      - kind: unit
        ref: "transforms.HeadersToValueTest#testSchemalessPathFieldCollisionOverwritten"
        status: pass
      - kind: unit
        ref: "transforms.HeadersToValueTest#testSchemalessEmptyValueMapGetsOneKey"
        status: pass
      - kind: unit
        ref: "transforms.HeadersToValueTest#testSchemalessNullValueOverwritten"
        status: pass
      - kind: unit
        ref: "transforms.HeadersToValueTest#testFieldCollisionIsCaseSensitive"
        status: pass
    human_judgment: false
  - id: D7
    description: "HDRS-06: the output record preserves topic, partition, key, key schema, timestamp and headers, on both the schema and schemaless paths"
    requirement: "HDRS-06"
    verification:
      - kind: unit
        ref: "transforms.HeadersToValueTest#testMetadataPreservedSchemaPath"
        status: pass
      - kind: unit
        ref: "transforms.HeadersToValueTest#testMetadataPreservedSchemalessPath"
        status: pass
    human_judgment: false
  - id: D8
    description: "HDRS-07: a header name added three times resolves to its last value on both paths; Integer/Boolean values coerce via toString(); an empty-string header value is distinguishable from a null entry; an escaped-unicode header value round-trips unchanged"
    requirement: "HDRS-07"
    verification:
      - kind: unit
        ref: "transforms.HeadersToValueTest#testDuplicateHeaderLastWinsSchemaPath"
        status: pass
      - kind: unit
        ref: "transforms.HeadersToValueTest#testDuplicateHeaderLastWinsSchemalessPath"
        status: pass
      - kind: unit
        ref: "transforms.HeadersToValueTest#testNonStringHeaderValuesCoerced"
        status: pass
      - kind: unit
        ref: "transforms.HeadersToValueTest#testEmptyStringHeaderValueStoredDistinctFromNull"
        status: pass
      - kind: unit
        ref: "transforms.HeadersToValueTest#testEscapedUnicodeHeaderValueUnchanged"
        status: pass
    human_judgment: false
  - id: D9
    description: "HDRS-08: close() followed by a fresh configure() rebuilds the cached schema for the next record; config() returns the shared CONFIG_DEF instance"
    requirement: "HDRS-08"
    verification:
      - kind: unit
        ref: "transforms.HeadersToValueTest#testCloseThenReconfigureRebuildsSchema"
        status: pass
      - kind: unit
        ref: "transforms.HeadersToValueTest#testConfigReturnsSharedConfigDefInstance"
        status: pass
    human_judgment: false

# Metrics
duration: 8min
completed: 2026-09-16
status: complete
---

# Phase 2 Plan 3: HeadersToValue Unit Coverage + Stale-Schema Cache Fix Summary

**All eight HeadersToValue unit-level requirements (HDRS-01..08) pinned by 23 new named tests (25 total), plus the D-02 stale-schema-cache guard and a corrected HDRS-04 requirement wording.**

## Performance

- **Duration:** 8 min
- **Started:** 2026-09-16T18:15:00Z
- **Completed:** 2026-09-16T18:23:02Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments
- Fixed the stale `valueSchema` cache in `HeadersToValue.applyWithSchema()` (D-02): a new `sourceSchema` field is compared by reference against the incoming record's schema, rebuilding the cached schema whenever it changes instead of silently reusing the first record's schema forever. Exactly one field, one widened condition, one nulling line in `close()` — no new method, no signature change.
- All eight unit-level `HeadersToValue` requirements (HDRS-01 through HDRS-08) pinned with 23 new tests plus the pre-existing 2, for 25 total in `HeadersToValueTest` — tombstone pass-through, empty-map/null-entry handling, config-error split, field-name collision (schema throw / schemaless overwrite), metadata preservation, duplicate-key/coercion/encoding edges, and the close()-then-reconfigure lifecycle.
- Removed the duplicated private `generateSampleRecord` builder, replacing it with the shared `TransformTestHelpers` builders from plan 02-01.
- Corrected the HDRS-04 wording in `.planning/REQUIREMENTS.md` to describe the real two-outcome error contract (absent key → `ConfigException`, blank value → `DataException`) instead of claiming both throw `DataException`.
- Full Docker-free `testUnit` suite green: 131 tests, 0 failed, 2 pre-existing skips unrelated to this plan.

## Task Commits

Each task was committed atomically:

1. **Task 1: End-to-end slice — D-02 schema-cache guard, its stale-schema test, and HDRS-06 on the shared helper** - `b893c2a` (test)
2. **Task 2: HDRS-01 tombstone, HDRS-02 empty map, HDRS-03 null entry, HDRS-07 duplicates and coercion** - `4d4b2e8` (test)
3. **Task 3: HDRS-04 config-error split (plus the REQUIREMENTS.md correction), HDRS-05 collision, HDRS-08 lifecycle** - `3fa3ac6` (test)

**Plan metadata:** (this commit)

## Files Created/Modified
- `src/main/java/com/clickhouse/kafka/connect/transforms/HeadersToValue.java` - Added the `sourceSchema` field and the D-02 stale-schema-cache guard in `applyWithSchema()`; nulled in `close()`. Exactly the change the plan specified: no new method, no signature change.
- `src/test/java/transforms/HeadersToValueTest.java` - Grew from 2 to 25 tests, pinning HDRS-01 through HDRS-08 and the D-02 regression, using the shared `TransformTestHelpers` builders throughout.
- `.planning/REQUIREMENTS.md` - Corrected the single HDRS-04 bullet to describe the `ConfigException`/`DataException` split; no other requirement touched, still 75 total bullets.

## Decisions Made
- Built the HDRS-07 Cyrillic/CJK encoding-edge test value from `int[]` Unicode code points at runtime, continuing the 02-01/02-02 convention that avoids the editing tool chain's escape-decoding hazard; verified with a zero-non-ASCII-byte scan of the final file.
- Corrected a stale per-plan commit ledger discovered at `.git/gsd-plan-head-before-02-03` (dated 2026-09-13, from a now-superseded milestone phase that also happened to be numbered `02-03`). Reset it to the true pre-plan `HEAD` (`ca138ff`, the 02-02 completion commit) so `actuals.commits` reflects this plan's own 3 commits rather than 48 unrelated ones from the old milestone's restructuring.

## Deviations from Plan

None - plan executed exactly as written. The D-02 fix and the escaped-unicode test-data approach were both explicit, pre-locked plan requirements (CONTEXT.md D-02, and the established 02-01/02-02 non-ASCII authoring pattern), not ad hoc deviations. The stale-ledger correction (above) was bookkeeping housekeeping, not a change to plan scope or committed code.

## Issues Encountered
- The `.git/gsd-plan-head-before-02-03` ledger file (used to measure `actuals.commits`) pre-existed from a prior, now-superseded milestone's differently-scoped phase that also used plan ID `02-03`. Detected because the measured baseline commit (`946fd84c`, dated 2026-09-13) predated this milestone's phase restructuring by three days and the resulting diff spanned 128 unrelated files. Corrected by resetting the ledger to the actual pre-plan `HEAD` before computing actuals; no code or test files were affected.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- `HeadersToValue` now has full unit-level coverage (HDRS-01..08), mirroring the D-02 stale-schema-cache fix already applied to `HeaderToValue` in 02-02.
- Sink-level coverage for `HeadersToValue` (HDRS-09) was already delivered by plan 02-05, which ran ahead of this plan in wave scheduling and already proved headers land correctly in a ClickHouse `Map(String, Nullable(String))` column.
- No blockers for the remaining Phase 2 plan (02-04 `ConvertToMapOrJson`).

---
*Phase: 02-fork-smt-coverage*
*Completed: 2026-09-16*

## Self-Check: PASSED

- `[ -f src/main/java/com/clickhouse/kafka/connect/transforms/HeadersToValue.java ]` -> FOUND
- `[ -f src/test/java/transforms/HeadersToValueTest.java ]` -> FOUND
- `[ -f .planning/REQUIREMENTS.md ]` -> FOUND (modified)
- `git log --oneline --all | grep -q b893c2a` -> FOUND
- `git log --oneline --all | grep -q 4d4b2e8` -> FOUND
- `git log --oneline --all | grep -q 3fa3ac6` -> FOUND
- Re-ran plan-level `<verification>`: `testUnit --tests 'transforms.HeadersToValueTest'` -> 25 tests, 0 failures; full `testUnit --rerun` -> 131 tests, 0 failed, 2 pre-existing skips; all eight `HDRS-0N:` `@DisplayName` prefixes present; `HeadersToValue.java` declares `sourceSchema` (5 occurrences), compares by reference (`!=`), no `.equals()` usage; zero bytes above 0x7F in `HeadersToValueTest.java`; `HeadersToValue.java` still declares exactly `configure`, `apply`, `applySchemaless`, `applyWithSchema`, `extractHeaders`, `buildSchema`, `config`, `close`; `.planning/REQUIREMENTS.md` has exactly one `HDRS-04` bullet naming both `ConfigException` and `DataException`, and 75 total requirement bullets.
