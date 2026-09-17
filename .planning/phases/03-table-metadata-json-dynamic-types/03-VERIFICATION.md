---
phase: 03-table-metadata-json-dynamic-types
verified: 2026-09-17T00:00:00Z
status: passed
score: 5/5 must-haves verified
covered_files:
  - ".planning/REQUIREMENTS.md"
  - ".planning/phases/03-table-metadata-json-dynamic-types/03-01-PLAN.md"
  - ".planning/phases/03-table-metadata-json-dynamic-types/03-01-SUMMARY.md"
  - ".planning/phases/03-table-metadata-json-dynamic-types/03-02-PLAN.md"
  - ".planning/phases/03-table-metadata-json-dynamic-types/03-02-SUMMARY.md"
  - ".planning/phases/03-table-metadata-json-dynamic-types/03-03-PLAN.md"
  - ".planning/phases/03-table-metadata-json-dynamic-types/03-03-SUMMARY.md"
  - ".planning/phases/03-table-metadata-json-dynamic-types/03-CONTEXT.md"
  - ".planning/phases/03-table-metadata-json-dynamic-types/03-REVIEW.md"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/JsonDynamicRoundTripTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryFailFastLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/JsonDynamicTypeMappingTest.java"
covered_digest: "v1:sha256:4164abb511f1f0ca409fc09c757645a23ec3c52b054a18472bb6c69f87fd09c0"
behavior_unverified: 0
overrides_applied: 0
human_verification: []
---

# Phase 3: Table Metadata & JSON/Dynamic Types Verification Report

**Phase Goal:** Table discovery and column typing added by the fork work correctly on ClickHouse `latest`. `targetTableFilter` selects the intended tables, `describeTable` never drops tables because of sub-columns, `JSON`/`Dynamic` columns round-trip real data, and the RowBinary path fails clearly instead of silently for those types.
**Verified:** 2026-09-17T00:00:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth (Success Criterion) | Status | Evidence |
|---|------|--------|----------|
| 1 | `targetTableFilter` tests pass: empty filter → no clause / all tables mapped; filter matching nothing → empty mapping + suppress-flag behavior; single-quote filter escaped and runs live; RE2 `\d`/anchors select intended tables live | ✓ VERIFIED | `buildTableFilterClause` extracted (`ClickHouseHelperClient.java:305-309`); `ClickHouseHelperClientForkUnitTest` (FILT-01/03/04, 16 tests, 0 failures) + `ClickHouseHelperClientForkLiveTest` (FILT-01..04, 9 tests, 0 failures) pin all four sub-clauses including the FILT-02 two-halves split (D-12) and the FILT-04 anchored-vs-unanchored identity assertion (D-19) |
| 2 | `describeTable` on `latest` returns correct tables with `Nullable`/`Array`/`JSON` columns without leaking `.null`/`.size0`/JSON-internal sub-columns; unhandled suffix found by audit is fixed; unmappable column still drops the table (pinned) and logs at ERROR | ✓ VERIFIED (see Warning below) | `shouldSkipDescribeRow` extracted (`ClickHouseHelperClient.java:320-335`) implementing the D-07 structural rule + D-20's bare-`.size` addition; both unmappable-column logs raised WARN→ERROR (`ClickHouseHelperClient.java:432`, and V2 equivalent — confirmed 2 occurrences, 0 WARN); `ClickHouseHelperClientForkLiveTest#DESC-01` and `#DESC-02` assert exact root-column-name sets on a live table carrying Nullable/Array/String/JSON/parameterized-JSON/Dynamic columns; `#DESC-04` asserts `describeTable` returns null for `Array(Nested(...))`. **WR-01 (code review) is a real, narrower residual gap** — see Warning below |
| 3 | `JSON`, parameterized `JSON(...)`, and `Dynamic` type strings map to `Type.JSON`/`Type.DYNAMIC`; records round-trip correctly with nested values/types in JSON, Dynamic, and mixed tables | ✓ VERIFIED | `JsonDynamicTypeMappingTest` (5 tests, Docker-free) pins TYPE-01/02 dispatch including parameterized forms and a non-ASCII-name dispatch edge; `JsonDynamicRoundTripTest` (6 tests, live) pins TYPE-03 (both D-13 routes: schemaless JSONEachRow and `bypassRowBinary` Struct records) with typed SQL read-back (`toTypeName`, `dynamicType`) rather than row counts, TYPE-04 (dynamicType stability), and TYPE-05 (7-column mixed table, both routes, exact Float64/Decimal, large-integer JSON leaf confirmed non-float) |
| 4 | A schema record targeting `JSON`/`Dynamic` via RowBinary fails fast with an `insertFormat=json`-naming error, no silent data loss | ✓ VERIFIED for the phase's scoped types (JSON/DYNAMIC/UNKNOWN) — **residual gap flagged, see Warning below** | `doWriteColValue`'s `default:` throws `DataException` for every `Type` not otherwise handled (true catch-all, confirmed by reading `ClickHouseWriter.java:563-568`); `validateDataSchema` throws the same shared-helper message for JSON/DYNAMIC/UNKNOWN (`:267-281`) — confirmed this is the gate that fires FIRST under default config per R-02; `RowBinaryUnsupportedTypeGateTest` (7 tests) pins both gates + zero-bytes-written + no-payload-leak; `RowBinaryFailFastLiveTest` (2 tests) proves a live `ClickHouseSinkTask.put()` failure leaves `countRows==0` and the `bypassRowBinary=true` escape hatch actually works |
| 5 | Cyrillic/CJK values round-trip through String/JSON/Dynamic columns and SMT header values under `windows-1252` | ✓ VERIFIED | `JsonDynamicRoundTripTest` TYPE-07 methods (values: Cyrillic, CJK, non-BMP surrogate pair, empty string, quote+backslash, via `String.equals` + `codePointCount`; SMT header: Cyrillic header through `HeaderToValue`); `ClickHouseHelperClientForkLiveTest#TYPE-07` (identifier half: non-ASCII column name + CJK Enum members) — the deferred Phase 1 UTF-8 reader bug was reproduced live and fixed (`StandardCharsets.UTF_8` added to both `describeTable` readers) |

**Score:** 5/5 truths verified (0 present-but-behavior-unverified)

### Warnings (from code review, independently confirmed by reading source)

**WR-01 — `shouldSkipDescribeRow` only walks the immediate parent, not the full ancestor chain.**
Confirmed by reading `ClickHouseHelperClient.java:328-334`: the lookup is `name.lastIndexOf('.')` → one `table.getAllColumnsMap()` lookup. For a subcolumn two-or-more levels below a JSON/Dynamic root (which arises only from an explicitly-typed path declaration like `JSON(a.b UInt32)`, a shape neither R-01's live audit nor any test fixture in this phase exercises — every fixture uses a bare `JSON`/`Dynamic` column or `JSON(max_dynamic_paths=...)`), the row is not skipped, is not dropped from `allColumnsList`, and produces persistent ERROR-level log noise on every `describeTable()` call (confirmed: this phase raised that log from WARN to ERROR, D-09, making the pre-existing noise louder for this specific shape). No data loss results — the JSON column is still written as one blob — and R-01's live audit established that **no such subcolumn rows are emitted at all** for any column shape this phase's tests actually declare, so criterion 2 holds for the tested surface. This is a real, narrower-than-advertised limitation of D-07's structural rule for a column shape outside this phase's own tested scope (and outside D-20's revised scope, which explicitly limited itself to what R-01's live audit found). Not a blocker; recommend a follow-up fix per WR-01's suggested ancestor-walk.

**WR-03 — `validateDataSchema`'s new fail-fast gate is narrower than `doWriteColValue`'s.**
Independently confirmed by reading both switches: `doWriteColValue`'s `default:` (`ClickHouseWriter.java:563-568`) is a true catch-all over the `Type` enum — any type without an explicit case throws the actionable message, including `INT128`/`INT256`/`UINT128`/`UINT256` (confirmed these map to real `Type` enum members via `Column.java:95,98,113,116`, not `Type.UNKNOWN`). `validateDataSchema`'s new case (`:267-281`) instead enumerates `"JSON"`/`"DYNAMIC"`/`"UNKNOWN"` explicitly ahead of its own unchanged `default:`, which still does the old comparison-based validation and, for `INT128`/`INT256`/`UINT128`/`UINT256` columns (`dataTypeName` can never equal `colTypeName` for these), sets `validSchema=false` rather than throwing the new actionable message — the caller then raises the pre-existing bare `"Data schema validation failed."` for those four types.

**My judgement on criterion 4:** this does **not** invalidate criterion 4. The requirement (TYPE-06) and every must-have truth in `03-03-PLAN.md` scope the fail-fast explicitly to `JSON`/`DYNAMIC`/`UNKNOWN` — the three types the fork's own `Column.extractColumn` produces for JSON/Dynamic type strings (and the `UNKNOWN` fallback for any unrecognized type string) — not to every `Type` enum member. For `INT128`/`INT256`/`UINT128`/`UINT256` specifically: behavior is **unchanged from before this phase** (both before and after, `validateDataSchema` already failed first with the same generic message; before this phase `doWriteColValue` would never even be reached for these types under default config) — no regression, no silent data loss, just an opportunity for message-clarity consistency not fully realized outside the phase's declared scope. Recorded here explicitly per the verification brief's instruction, not swept under the rug: a real, out-of-phase-scope residual gap that a maintainer should track (e.g. as a Phase 4+ follow-up or a fork-wide `EnumSet`-driven single-authority refactor, per WR-03's suggested fix).

**WR-02** (pre-existing `describeTableV2` ignoring its `database` parameter) is explicitly out of this phase's diff (unchanged by any Phase 3 commit) and tracked separately as `PAR-01`/deferred in `03-CONTEXT.md`; not evaluated further here as it is not part of this phase's goal.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `ClickHouseHelperClient.java` | `shouldSkipDescribeRow` + `buildTableFilterClause`, wired into `showTables`/`describeTableV1`/`describeTableV2`, WARN→ERROR | ✓ VERIFIED | Both helpers present with full javadoc; `describeTableV1`/`V2` both call `shouldSkipDescribeRow`; both unmappable-column logs at `LOGGER.error`, 0 remaining `LOGGER.warn` for that message |
| `ClickHouseWriter.java` | `doWriteColValue` default-branch throw, `validateDataSchema` JSON/DYNAMIC/UNKNOWN case, shared message helper | ✓ VERIFIED | `buildRowBinaryUnsupportedTypeMessage` helper called from both gates (`:279`, `:566`); message contains `insertFormat=json` and `bypassRowBinary=true` literals exactly once each outside comments |
| `ClickHouseHelperClientForkUnitTest.java` | Docker-free FILT/DESC pins | ✓ VERIFIED | 16 tests, 0 failures/errors, all REQ-ID-prefixed `@DisplayName`s present |
| `ClickHouseHelperClientForkLiveTest.java` | Live FILT/DESC/TYPE-07-identifier pins | ✓ VERIFIED | 9 tests, 0 failures/errors |
| `JsonDynamicTypeMappingTest.java` | Docker-free TYPE-01/02 pins | ✓ VERIFIED | 5 tests, 0 failures/errors |
| `JsonDynamicRoundTripTest.java` | Live TYPE-03/04/05/07 pins, both D-13 routes | ✓ VERIFIED | 6 tests, 0 failures/errors; typed read-back (`toTypeName`, `dynamicType`) confirmed present, not row-count-only |
| `RowBinaryUnsupportedTypeGateTest.java` | Docker-free TYPE-06 pins, both gates | ✓ VERIFIED | 7 tests, 0 failures/errors; zero-bytes-written and no-payload-leak assertions present |
| `RowBinaryFailFastLiveTest.java` | Live TYPE-06 fail-fast + escape-hatch proof | ✓ VERIFIED | 2 tests, 0 failures/errors; `getCause()` chain-walk content assertion present, not a bare `assertThrows` |
| `.planning/REQUIREMENTS.md` | Corrected FILT-02 wording, all 15 IDs marked complete | ✓ VERIFIED | FILT-02 bullet matches D-12 wording exactly; bullet count still 75; all 15 phase requirement IDs (`FILT-01..04`, `DESC-01..04`, `TYPE-01..07`) marked `[x] Complete` in both the requirement list and the Phase 3 traceability table |

### Key Link Verification

| From | To | Via | Status |
|------|-----|-----|--------|
| `ClickHouseHelperClient.shouldSkipDescribeRow` | `Table.getAllColumnsMap()` | parent-type lookup | ✓ WIRED (confirmed at `:332-334`; single-level only, see WR-01) |
| `ClickHouseHelperClientForkUnitTest`/`ForkLiveTest` | `ClickHouseHelperClient` (package-private helpers) | same-package direct call | ✓ WIRED |
| `JsonDynamicRoundTripTest` | `ClickHouseWriter` (`doInsertJson` via `bypassRowBinary`) | `ClickHouseSinkTask.put` routing | ✓ WIRED — confirmed both schemaless and Struct/`bypassRowBinary` routes exercised |
| `ClickHouseWriter.doWriteColValue` default / `validateDataSchema` new case | `buildRowBinaryUnsupportedTypeMessage` | shared private helper | ✓ WIRED — single source of message wording, confirmed by direct read |
| `RowBinaryFailFastLiveTest` | `ClickHouseSinkTask.put` → `ClickHouseWriter` | live default-config insert | ✓ WIRED — `getCause()` chain walked to content, `countRows==0` asserted |

### Behavioral Spot-Checks

Full-suite result already gathered by the orchestrator (330 tests, 323 succeeded, 0 failed, 7 skipped; build successful) is accepted as the authoritative test-execution evidence per the verification brief; not re-run here (would take ~4.5 min for no new evidence). Per-class result XMLs were read directly instead (see table below) as the required independent confirmation that each phase-specific test class is present in that run and green:

| Test class | tests | failures | errors | Status |
|------------|-------|----------|--------|--------|
| `ClickHouseHelperClientForkUnitTest` (testUnit) | 16 | 0 | 0 | ✓ PASS |
| `ClickHouseHelperClientForkLiveTest` (test) | 9 | 0 | 0 | ✓ PASS |
| `JsonDynamicTypeMappingTest` (testUnit) | 5 | 0 | 0 | ✓ PASS |
| `JsonDynamicRoundTripTest` (test) | 6 | 0 | 0 | ✓ PASS |
| `RowBinaryUnsupportedTypeGateTest` (testUnit) | 7 | 0 | 0 | ✓ PASS |
| `RowBinaryFailFastLiveTest` (test) | 2 | 0 | 0 | ✓ PASS |

All six phase-specific test classes are green, individually and as part of the already-gathered full-suite run.

### Requirements Coverage

| Requirement | Source Plan | Status | Evidence |
|-------------|-------------|--------|----------|
| FILT-01..04 | 03-01 | ✓ SATISFIED | Unit + live tests, both levels, FILT-02 two-halves split |
| DESC-01..04 | 03-01 | ✓ SATISFIED (WR-01 noted) | Unit + live tests; structural skip rule implemented and tested for the shapes R-01's live audit found |
| TYPE-01..05, TYPE-07 | 03-02 (+ TYPE-07 identifier half in 03-01) | ✓ SATISFIED | Docker-free dispatch pins + live typed round-trips on both D-13 routes |
| TYPE-06 | 03-03 | ✓ SATISFIED (WR-03 noted, out-of-scope-type gap) | Both gates pinned Docker-free + live fail-fast/escape-hatch proof |

No orphaned requirements: `.planning/REQUIREMENTS.md`'s Phase 3 traceability table lists exactly the 15 IDs declared across the three plans' `requirements` frontmatter, and all 15 are marked Complete.

### Anti-Patterns Found

No `TBD`/`FIXME`/`XXX`/`TODO`/`HACK`/`PLACEHOLDER` markers, empty stub implementations, or hardcoded-empty-data patterns found in any of the 8 files this phase modified or created. Five pre-existing `// TODO` comments in `ClickHouseWriter.java` (lines 171, 255, 312, 685, 1259) predate this phase's diff (confirmed via `git diff` against the commit range) and are unrelated to the changes under review.

### Human Verification Required

None. All five ROADMAP success criteria are verifiable programmatically via existing test evidence and direct source reading; no visual, real-time, or external-service behavior is in scope for this phase.

### Gaps Summary

No blocking gaps. Two warnings carried forward from the phase's own code review (WR-01, WR-03), both independently re-confirmed by reading the relevant source directly rather than trusting the review's narrative:

- **WR-01**: `shouldSkipDescribeRow`'s structural JSON/Dynamic skip rule only resolves the immediate parent, not the full ancestor chain, so a hypothetical two-or-more-level-deep typed JSON subcolumn path (not emitted by any column shape this phase's tests declare, and not found by R-01's live audit) would leak ERROR-level log noise without dropping the table or losing data. Recommend as a follow-up fix; does not block this phase's goal.
- **WR-03**: `validateDataSchema`'s new fail-fast case protects only `JSON`/`DYNAMIC`/`UNKNOWN` (the phase's declared scope), not the full catch-all `doWriteColValue`'s `default:` branch covers (e.g. `INT128`/`INT256`/`UINT128`/`UINT256`). Behavior for those four types is unchanged from before this phase — no regression, no silent data loss — but the "one shared actionable message for every RowBinary-unsupported type" aspiration in the phase's own commit messages is achieved only for the types TYPE-06 actually scopes. Recommend tracking as a maintainer follow-up (e.g. a shared `EnumSet` single-authority refactor).

Both are documented here explicitly, as instructed, rather than silently accepted or silently dropped.

---

_Verified: 2026-09-17T00:00:00Z_
_Verifier: Claude (gsd-verifier)_
