---
phase: 03-table-metadata-json-dynamic-types
plan: 02
subsystem: database
tags: [clickhouse, json, dynamic, testing, rowbinary]

# Dependency graph
requires:
  - phase: 03-table-metadata-json-dynamic-types
    provides: "Plan 03-01's shouldSkipDescribeRow/buildTableFilterClause extraction and the ClickHouseBase.createClient() targetTableFilter wiring fix"
provides:
  - "JsonDynamicTypeMappingTest — Docker-free TYPE-01/TYPE-02 pins over Column.extractColumn, including the parameterized JSON(...)/Dynamic(...) forms and a non-ASCII column-name dispatch edge"
  - "JsonDynamicRoundTripTest — ClickHouse-backed TYPE-03/04/05 round-trips on both D-13 insert routes (schemaless JSONEachRow, bypassRowBinary Struct records) with typed SQL read-back, plus the TYPE-07 non-ASCII values and SMT header half"
  - "A reusable allRows(chc, sql, columns...) row-snapshot pattern that avoids the client-v2 GenericRecord mutable-row-reuse pitfall"
affects: [03-03]

# Actuals (#2632)
actuals:
  tokens: 8372
  tasks: 3
  commits: 3
  plan_head_before: b9f810a6b846c9566c15365e7eee98292e201e7e

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Row-snapshot pattern: com.clickhouse.client.api.query.GenericRecord returned by Records' iterator is a mutable, reused view over the binary reader buffer — collect Map<String,String> snapshots per row during iteration, never the GenericRecord references themselves."
    - "ClickHouse JSON-type columns require an object at the root (\"JSON object should start with '{'\" otherwise) — wrap scalar test values under a synthetic key rather than inserting a bare scalar."
    - "Docker-free vs ClickHouse-backed split by inheritance (extend ClickHouseBase vs extend nothing), matching Phase 1 Architecture Pattern 2 and Plan 03-01's convention."

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/JsonDynamicTypeMappingTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/JsonDynamicRoundTripTest.java
  modified: []

key-decisions:
  - "TYPE-07's SMT header case uses HeaderToValue (singular class, maps one header directly to a named String-typed record field) instead of the plan's read_first pointer to HeadersToValue (plural, always produces a Map<String,String> of all headers). HeaderToValue matches the plan's own requirement wording — \"insert the transformed record into a String column\" — exactly, whereas HeadersToValue would have required wiring a Map column instead. Within Claude's Discretion per 03-CONTEXT.md (live-test shapes)."
  - "TYPE-07's non-empty json_col fixtures wrap their scalar value under a synthetic \"value\" key (e.g. {\"value\": \"<cyrillic>\"}) rather than inserting a bare string. Live-verified this session: ClickHouse's JSON type rejects a root scalar via JSONEachRow with \"JSON object should start with '{'\". The empty-object edge (row 4, {}) is unaffected and stays a true root object with no keys."
  - "A private allRows(chc, sql, columns...) helper snapshots each row into a Map<String,String> during iteration, never returning List<GenericRecord>. Live-verified this session: the client-v2 GenericRecord returned by Records' iterator is a mutable, reused view — collecting the reference itself (not its values) silently aliases every collected \"row\" to whatever row the iterator last advanced to. This is a general client-v2 test-authoring pitfall, not specific to this plan's data shapes, and is documented inline via Javadoc on the helper for future test authors."

patterns-established:
  - "REQ-ID-prefixed @DisplayName carried forward from Phase 2 D-10/D-17 and Plan 03-01 into this plan's test methods."
  - "Non-ASCII test data built from int[] Unicode code points at runtime (D-18); neither a raw non-ASCII byte NOR a literal \\u escape sequence may appear anywhere in source — including inside comments, since javac treats \\u as a real Unicode escape even in comment text. Both forms found and fixed live this session (see Deviations)."

requirements-completed: [TYPE-01, TYPE-02, TYPE-03, TYPE-04, TYPE-05, TYPE-07]

coverage:
  - id: D1
    description: "TYPE-01/TYPE-02: JSON, parameterized JSON(...), Dynamic, and parameterized Dynamic(...) type strings map to Type.JSON/Type.DYNAMIC via Column.extractColumn, Docker-free; a Dynamic column with a non-ASCII name still dispatches correctly, proving dispatch is over the type string only"
    requirement: "TYPE-01"
    verification:
      - kind: unit
        ref: "JsonDynamicTypeMappingTest#jsonTypeStringsMapToJson (parameterized: JSON, JSON(max_dynamic_paths=10))"
        status: pass
      - kind: unit
        ref: "JsonDynamicTypeMappingTest#dynamicTypeStringsMapToDynamic (parameterized: Dynamic, Dynamic(max_types=8))"
        status: pass
      - kind: unit
        ref: "JsonDynamicTypeMappingTest#dynamicTypeDispatchIgnoresNonAsciiColumnName"
        status: pass
    human_judgment: false
  - id: D2
    description: "TYPE-03: a JSON column's nested leaf and a Dynamic column's value+dynamicType round-trip correctly through BOTH D-13 insert routes (schemaless JSONEachRow and bypassRowBinary=true Struct records), asserted with typed SQL read-back rather than a row count"
    requirement: "TYPE-03"
    verification:
      - kind: integration
        ref: "JsonDynamicRoundTripTest#jsonAndDynamicRoundTripThroughSchemalessRoute"
        status: pass
      - kind: integration
        ref: "JsonDynamicRoundTripTest#jsonAndDynamicRoundTripThroughStructRoute"
        status: pass
    human_judgment: false
  - id: D3
    description: "TYPE-04: dynamicType(dynamic_col) is stable across repeated reads and independent of read order, on integer/string/float values on the Dynamic column's own table"
    requirement: "TYPE-04"
    verification:
      - kind: integration
        ref: "JsonDynamicRoundTripTest#dynamicColumnTypeIsStableAcrossRepeatedReads"
        status: pass
    human_judgment: false
  - id: D4
    description: "TYPE-05: a mixed table (Dynamic, String, UInt32, JSON(max_dynamic_paths=10), Float64, Decimal(18,4), Dynamic) round-trips every column correctly through both insert routes on disjoint id ranges; Float64/Decimal values are exact and a large-integer JSON leaf is confirmed not a floating-point type"
    requirement: "TYPE-05"
    verification:
      - kind: integration
        ref: "JsonDynamicRoundTripTest#mixedTableRoundTripsThroughBothInsertRoutes"
        status: pass
    human_judgment: false
  - id: D5
    description: "TYPE-07 (values half): Cyrillic, CJK, a single non-BMP code point, an empty string, and a value carrying a double quote and backslash round-trip byte-identically through String, JSON and Dynamic columns under the windows-1252 test JVM, verified with String.equals + codePointCount and cross-checked via both the UTF-8-safe getAllRowsAsJson helper and an independent typed SELECT"
    requirement: "TYPE-07"
    verification:
      - kind: integration
        ref: "JsonDynamicRoundTripTest#nonAsciiAndBoundaryValuesRoundTripThroughAllColumnKinds"
        status: pass
    human_judgment: false
  - id: D6
    description: "TYPE-07 (SMT header half): a Cyrillic SMT header value round-trips byte-identically through HeaderToValue into a String column via the schemaless insert route"
    requirement: "TYPE-07"
    verification:
      - kind: integration
        ref: "JsonDynamicRoundTripTest#nonAsciiHeaderValueRoundTripsThroughHeaderToValueSmt"
        status: pass
    human_judgment: false

# Metrics
duration: 26min
completed: 2026-09-17
status: complete
---

# Phase 3 Plan 2: JSON/Dynamic Type Round-Trips Summary

**Pinned TYPE-01/02 type-string dispatch and TYPE-03/04/05/07 JSON+Dynamic round-trips across both fork insert routes (schemaless JSONEachRow, bypassRowBinary Struct records) with typed SQL read-back instead of row counts, plus non-ASCII values under the windows-1252 test JVM.**

## Performance

- **Duration:** 26 min
- **Started:** 2026-09-17T08:50:25Z
- **Completed:** 2026-09-17T09:16:32Z
- **Tasks:** 3
- **Files modified:** 2 (both new)

## Accomplishments
- `JsonDynamicTypeMappingTest` pins TYPE-01/TYPE-02 Docker-free: `JSON`, parameterized `JSON(...)`, `Dynamic`, and parameterized `Dynamic(...)` all map correctly through `Column.extractColumn`, plus a non-ASCII column-name edge proving dispatch is over the type string only — no main-code change, confirming RESEARCH's finding that `Column.dispatchPrimitive`'s `startsWith` checks are already correct.
- `JsonDynamicRoundTripTest` proves TYPE-03 (JSON nested-leaf + Dynamic value/type) round-trips correctly through both D-13 insert routes — schemaless `JSONEachRow` and `bypassRowBinary=true` Struct records — the latter being the untested-until-now, more-common CDC shape and the honest proof that plan 03-03's new RowBinary error message's escape hatch actually works.
- TYPE-04 pins `dynamicType()` stability across repeated reads, independent of read order.
- TYPE-05 pins a 7-column mixed table (`Dynamic, String, UInt32, JSON(max_dynamic_paths=10), Float64, Decimal(18,4), Dynamic`) round-tripping through both routes on disjoint id ranges, proving JSON/Dynamic column position doesn't matter and that `Float64`/`Decimal` values survive the JSON insert route exactly.
- TYPE-07 pins Cyrillic, CJK, a non-BMP code point, an empty string, and a quote-and-backslash value round-tripping byte-identically through `String`, `JSON`, and `Dynamic` columns, plus a Cyrillic SMT header value through `HeaderToValue`.
- No main-code file modified; both upstream reference files (`ColumnTest.java`, `JsonColumnTest.java`) verified unchanged via `git hash-object` before every commit.

## Task Commits

Each task was committed atomically:

1. **Task 1: End-to-end slice — TYPE-01/TYPE-02 type-string pins plus one full schemaless JSON and Dynamic round-trip with typed read-back** - `a79efcd` (test)
2. **Task 2: The second insert route and the mixed table — bypassRowBinary Struct records, TYPE-05 column position, and the precision edges** - `e0425cd` (test)
3. **Task 3: TYPE-07 values half — Cyrillic and CJK through String, JSON and Dynamic columns and through an SMT header value** - `82777e2` (test)

_Note: Task 1 is `type="tracer"` — verified end-to-end before Task 2 began, per the tracer feedback gate (row 3: interactive, `end-of-phase`, automated-only verify → re-run and continue, no checkpoint)._

## Files Created/Modified
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/JsonDynamicTypeMappingTest.java` - New Docker-free unit class, 5 tests (TYPE-01, TYPE-02)
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonDynamicRoundTripTest.java` - New ClickHouse-backed live class, 6 tests (TYPE-03 x2, TYPE-04, TYPE-05, TYPE-07 x2)

## Decisions Made
- Used `HeaderToValue` (singular class) rather than the plan's `read_first`-pointed `HeadersToValue` (plural) for the TYPE-07 SMT header case, because `HeaderToValue` maps one header directly to a named `String`-typed field, matching the requirement's own wording ("insert the transformed record into a String column") exactly. `HeadersToValue` always produces a `Map<String,String>` of every header, which would have required a `Map` column instead. Within Claude's Discretion (03-CONTEXT.md: live-test shapes).
- Non-empty `json_col` fixtures in the TYPE-07 values test wrap their scalar under a synthetic `"value"` key rather than inserting a bare string — live-verified this session that ClickHouse's `JSON` type rejects a root scalar via `JSONEachRow` with `Code: 117. DB::Exception: JSON object should start with '{'`. The empty-object edge (row 4) is unaffected and stays a true root `{}`.
- Introduced a private `allRows(chc, sql, columns...)` helper that snapshots each row into a `Map<String,String>` during iteration rather than collecting `GenericRecord` references — live-verified this session that client-v2's `GenericRecord` (returned by `Records`' iterator) is a mutable, reused view over the binary reader buffer; collecting the references themselves silently aliased every "row" to the last one read.

## Deviations from Plan

None - plan executed exactly as written. The three findings below were bugs in this plan's own newly-authored test code, caught and fixed during implementation before any commit existed (not pre-existing SUT/production bugs, so no Rule 1-4 deviation classification applies — `git status --porcelain src/main` stayed empty throughout):

- A literal `\u` sequence in two source comments (documenting the Unicode code points used) triggered `error: illegal unicode escape` from javac, which treats `\u` as a real escape sequence even inside comments. Fixed by rewording the comments to avoid the literal two-character sequence.
- Two comments contained literal Cyrillic/CJK characters as illustrative examples, tripping the plan's own "no byte above 0x7F" acceptance criterion. Fixed by replacing them with ASCII transliterations.
- The `allRows` helper's first version collected `GenericRecord` object references into a `List` and indexed them after the fact; because client-v2's `GenericRecord` is a mutable, reused row view, every "row" in the resulting list silently aliased the last row actually read. Live-run test failures (`expected id=1 leaf=1, got id=2 leaf=2` on every assertion) surfaced this immediately; fixed by snapshotting each row's needed columns into an immutable `Map<String,String>` during iteration.

## Issues Encountered
None beyond the deviations above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Both new test classes are self-contained and reusable as-is by plan 03-03 (RowBinary fail-fast), which shares the same live-test conventions (`ClickHouseBase`, `uniqueConnectorName`, table-before-`start()` ordering).
- The `allRows` row-snapshot pattern and the JSON-root-scalar-requires-wrapping finding are directly relevant if 03-03's live fail-fast test needs any typed read-back of its own.
- No blockers for 03-03.

## Self-Check: PASSED

---
*Phase: 03-table-metadata-json-dynamic-types*
*Completed: 2026-09-17*
