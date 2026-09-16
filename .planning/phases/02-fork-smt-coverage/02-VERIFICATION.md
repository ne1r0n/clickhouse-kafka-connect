---
phase: 02-fork-smt-coverage
verified: 2026-09-16T21:45:00Z
status: passed
score: 30/30 requirements verified (TOF-01..06, HDRS-01..09, HDR-01..07, CONV-01..08)
covered_files:
  - .planning/REQUIREMENTS.md
  - .planning/ROADMAP.md
  - .planning/phases/02-fork-smt-coverage/02-01-PLAN.md
  - .planning/phases/02-fork-smt-coverage/02-01-SUMMARY.md
  - .planning/phases/02-fork-smt-coverage/02-02-PLAN.md
  - .planning/phases/02-fork-smt-coverage/02-02-SUMMARY.md
  - .planning/phases/02-fork-smt-coverage/02-03-PLAN.md
  - .planning/phases/02-fork-smt-coverage/02-03-SUMMARY.md
  - .planning/phases/02-fork-smt-coverage/02-04-PLAN.md
  - .planning/phases/02-fork-smt-coverage/02-04-SUMMARY.md
  - .planning/phases/02-fork-smt-coverage/02-05-PLAN.md
  - .planning/phases/02-fork-smt-coverage/02-05-SUMMARY.md
  - .planning/phases/02-fork-smt-coverage/02-CONTEXT.md
  - .planning/phases/02-fork-smt-coverage/02-REVIEW.md
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson.java
  - src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java
  - src/main/java/com/clickhouse/kafka/connect/transforms/HeadersToValue.java
  - src/main/java/com/clickhouse/kafka/connect/transforms/TableOperationFilter.java
  - src/test/java/com/clickhouse/kafka/connect/sink/SmtSinkLandingTest.java
  - src/test/java/transforms/ConvertToMapOrJsonTest.java
  - src/test/java/transforms/HeaderToValueTest.java
  - src/test/java/transforms/HeadersToValueTest.java
  - src/test/java/transforms/TableOperationFilterTest.java
  - src/test/java/transforms/TransformTestHelpers.java
covered_digest: "v1:sha256:e23855273e0ca8ba19c4eac044b1f6c34b35c23b7a74aa0984119ab889aa0f9f"
overrides_applied: 0
behavior_unverified: 0
---

# Phase 2: Fork SMT Coverage Verification Report

**Phase Goal:** Every behavior of `TableOperationFilter`, `HeadersToValue`, `HeaderToValue`, and
`ConvertToMapOrJson` is pinned: edge cases, config validation, record metadata preservation, and
records reaching ClickHouse through the sink after transformation
**Verified:** 2026-09-16T21:45:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `testUnit` passes with `TableOperationFilter` coverage: truncate drops, multi-table filtering, invalid/empty/whitespace/`r` tokens ignored, `op=r`/schemaless pass-through, `config()`/repeated `close()` | ✓ VERIFIED | `TableOperationFilterTest.java` (397 lines, 26 `@Test`/`@ParameterizedTest` methods) — fresh run: `tests="26" failures="0" errors="0" skipped="0"`. `TableOperationFilter.configure()` clears `targetTables`/`skippedOperations` (D-03), confirmed at lines 57-58. |
| 2 | `HeadersToValue`/`HeaderToValue` tests pass on schema+schemaless for tombstones, missing/null/duplicate/multi-valued/non-string headers, `DataException` on bad config, field-collision, added optional field, preserved metadata | ✓ VERIFIED | `HeaderToValueTest.java` (24 tests, 0 fail), `HeadersToValueTest.java` (25 tests, 0 fail). All `HDR-01..06` and `HDRS-01..08` `@DisplayName` prefixes present in source. D-02 stale-schema guards (`sourceSchema` field, reference comparison) confirmed in both `HeaderToValue.java` and `HeadersToValue.java`. |
| 3 | `ConvertToMapOrJson` tests pass for null-field omission (Jackson+Gson), Structs nested in Array/Map, non-Struct passthrough, `ConfigException` for invalid library, direct/sliced `ByteBuffer` base64, preserved metadata | ✓ VERIFIED | `ConvertToMapOrJsonTest.java` (37 tests, 0 fail). All `CONV-01..07` prefixes present. D-01 recursion fix confirmed: `convertFieldValue()` has `instanceof List` and `instanceof Map` branches building `ArrayList`/`LinkedHashMap`, both recursing. |
| 4 | On ClickHouse `latest`, one sink-level test per SMT passes: filtered Debezium ops never land while unfiltered do; headers land in `Map(String, Nullable(String))`; header-derived field lands in its column; JSON-string output lands in String/JSON columns | ✓ VERIFIED | `SmtSinkLandingTest.java` (5 tests, 0 fail) — read in full; each test builds real Debezium/header/JSON fixtures, creates real ClickHouse tables, runs a real `ClickHouseSinkTask`, and asserts row content via `getAllRowsAsJson`/`queryAll` (not stubs). `TOF-06`, `HDRS-09`, `HDR-07`, `CONV-08` `@DisplayName` prefixes all present. |
| 5 | Any bug these tests expose is fixed with a small targeted change, and `./gradlew test` stays green | ✓ VERIFIED | Four fixes landed and confirmed in source: D-01 (ConvertToMapOrJson recursion), D-02×2 (HeaderToValue + HeadersToValue stale-schema guards), D-03 (TableOperationFilter configure() clear), and the approved out-of-scope `ClickHouseWriter` MAP-branch null-marker fix (exactly 3 lines, commit `3929ce6`). Full suite: 285 tests, 278 succeeded, 0 failed, 7 skipped (all pre-existing/unrelated to this phase — confirmed by inspecting the 3 skip-bearing XML files, none of which are phase-2 classes). |

**Score:** 5/5 ROADMAP success criteria verified; 30/30 individual requirement IDs (TOF-01..06,
HDRS-01..09, HDR-01..07, CONV-01..08) have named, passing tests confirmed in the current source tree.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/test/java/transforms/TransformTestHelpers.java` | Shared package-private builder class | ✓ VERIFIED | 179 lines, package `transforms`, `final class`, no `public`, no `extends ClickHouseBase`/`@Tag`. |
| `src/test/java/transforms/TableOperationFilterTest.java` | Grown to pin TOF-01..05 | ✓ VERIFIED | 397 lines, 26 tests, all 5 `TOF-0N:` DisplayName prefixes present, 0 failures. |
| `src/test/java/transforms/HeaderToValueTest.java` | Grown to pin HDR-01..06 | ✓ VERIFIED | 478 lines, 24 tests, all 6 `HDR-0N:` prefixes present, 0 failures. |
| `src/test/java/transforms/HeadersToValueTest.java` | Grown to pin HDRS-01..08 | ✓ VERIFIED | 593 lines, 25 tests, all 8 `HDRS-0N:` prefixes present, 0 failures. |
| `src/test/java/transforms/ConvertToMapOrJsonTest.java` | Grown to pin CONV-01..07 | ✓ VERIFIED | 792 lines, 37 tests, all 7 `CONV-0N:` prefixes present, 0 failures. |
| `src/test/java/com/clickhouse/kafka/connect/sink/SmtSinkLandingTest.java` | New ClickHouse-backed sink test class | ✓ VERIFIED | 411 lines, `extends ClickHouseBase`, 5 tests covering TOF-06/HDRS-09/HDR-07/CONV-08 (2 tests), 0 failures against `latest`/V2. |
| `TableOperationFilter.java` (D-03) | 2-line `configure()` clear fix | ✓ VERIFIED | `targetTables.clear(); skippedOperations.clear();` at lines 57-58, before config re-parse. |
| `HeaderToValue.java` (D-02) | `sourceSchema` guard | ✓ VERIFIED | Field at line 44, reference-compared rebuild at line 143, nulled in `close()` at line 231. |
| `HeadersToValue.java` (D-02) | `sourceSchema` guard | ✓ VERIFIED | Field at line 41, reference-compared rebuild at line 113, nulled in `close()` at line 195. |
| `ConvertToMapOrJson.java` (D-01) | List/Map recursion branches | ✓ VERIFIED | `instanceof List` (line 245) and `instanceof Map` (line 253) branches inside `convertFieldValue()`, both recursing and building `ArrayList`/`LinkedHashMap`. |
| `ClickHouseWriter.java` (approved deviation) | MAP branch null-marker fix | ✓ VERIFIED | Commit `3929ce6`: exactly 3 lines added (`if (col.getMapValueType().isNullable() && mapValue != null) { BinaryStreamUtils.writeNonNull(stream); }`), 0 removed. Matches the user-authorized scope note exactly. |
| `.planning/REQUIREMENTS.md` HDRS-04 correction | Two-outcome `ConfigException`/`DataException` wording | ✓ VERIFIED | Line 38 now reads "An absent `field` key throws `ConfigException` ...; a present but blank or whitespace-only `field` value throws `DataException` ..." — matches D-14's locked correction. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `TransformTestHelpers` | `TableOperationFilterTest`/`HeaderToValueTest`/`HeadersToValueTest`/`ConvertToMapOrJsonTest` | same-package, no import | ✓ WIRED | All four consumer classes are in package `transforms`; `TransformTestHelpers` builders (`schemaRecord`, `debeziumRecord`, `assertMetadataPreserved`, etc.) are referenced throughout each file. |
| `SmtSinkLandingTest` | `ClickHouseSinkTask` → `ClickHouseWriter` → ClickHouse | `runSink()` → `sinkTask.put(records)` | ✓ WIRED | `runSink()` calls real `start()`/`put()`/`stop()`; assertions read back through `ClickHouseTestHelpers.getAllRowsAsJson`/`chc.getClient().queryAll(...)`, i.e. real database round-trips, not mocked. |
| `ClickHouseWriter` MAP branch | RowBinary stream | `BinaryStreamUtils.writeNonNull(stream)` | ✓ WIRED | Fix sits directly in `doWriteColValue()`'s `case MAP:` branch (lines 439-451), exercised end-to-end by `hdrs09HeadersLandInMapColumn` which inserts into and reads back a real `Map(String, Nullable(String))` column. |

### Data-Flow Trace (Level 4)

Not applicable in the UI-rendering sense — this is a test-coverage phase, not a data-rendering
feature. The equivalent trace (assertion → real query → real table) is captured in the Key Link
table above: every sink-level assertion in `SmtSinkLandingTest` reads its value from a live
`getAllRowsAsJson`/`queryAll` call against the ClickHouse container that `runSink()` just wrote to,
not from a static fixture or fallback.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Full ClickHouse-backed suite green (pre-established, this run) | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun` | 285 tests, 278 succeeded, 0 failed, 7 skipped (all pre-existing, unrelated) | ✓ PASS |
| Docker-free `testUnit` green (pre-established, this run) | `./gradlew testUnit --rerun` | 155 tests, 153 succeeded, 0 failed, 2 skipped | ✓ PASS |
| Per-class test counts match SUMMARY claims | Read `build/test-results/test/TEST-*.xml` directly | TOF=26, HDR=24, HDRS=25, CONV=37, Sink=5, all `failures="0" errors="0" skipped="0"` | ✓ PASS |
| D-01/D-02×2/D-03 fixes present in source | `grep` against production files | All four confirmed present, matching plan-locked shape exactly | ✓ PASS |
| ClickHouseWriter diff scope | `git show 3929ce6` | Exactly 3 lines added, 0 removed, in the single MAP branch | ✓ PASS |
| No debt markers in phase-touched files | `grep -nE "TBD|FIXME|XXX|TODO|HACK|PLACEHOLDER"` | Zero hits in all 11 phase-touched files (5 pre-existing TODOs in `ClickHouseWriter.java` are untouched by this phase's diff) | ✓ PASS |

### Requirements Coverage

All 30 requirement IDs declared across the 5 plans (`02-01`: TOF-01..05; `02-02`: HDR-01..06;
`02-03`: HDRS-01..08; `02-04`: CONV-01..07; `02-05`: TOF-06, HDRS-09, HDR-07, CONV-08) match
REQUIREMENTS.md exactly — no orphans, no gaps.

| Requirement Group | Source Plan(s) | Status | Evidence |
|---|---|---|---|
| TOF-01..05 | 02-01 | ✓ SATISFIED | 26 tests in `TableOperationFilterTest.java`, all passing |
| TOF-06 | 02-05 | ✓ SATISFIED | `tof06FilteredOperationsNeverLandUnfilteredDo` — real insert path, real query assertions |
| HDR-01..06 | 02-02 | ✓ SATISFIED | 24 tests in `HeaderToValueTest.java`, all passing |
| HDR-07 | 02-05 | ✓ SATISFIED | `hdr07HeaderDerivedFieldLandsInColumn` |
| HDRS-01..08 | 02-03 | ✓ SATISFIED | 25 tests in `HeadersToValueTest.java`, all passing |
| HDRS-09 | 02-05 | ✓ SATISFIED | `hdrs09HeadersLandInMapColumn` (also the test that tripped and validated the `ClickHouseWriter` fix) |
| CONV-01..07 | 02-04 | ✓ SATISFIED | 37 tests in `ConvertToMapOrJsonTest.java`, all passing |
| CONV-08 | 02-05 | ✓ SATISFIED | `conv08JsonStringOutputLandsAcrossColumns` + `conv08SingleRecordBatchLandsOneRow` |

REQUIREMENTS.md traceability table (lines 176-205) marks all 30 as "Phase 2 | Complete", matching
the checkbox state (`- [x]`) for every one of TOF/HDRS/HDR/CONV. No ORPHANED requirements.

### Anti-Patterns Found

Zero blockers. Scan of all 11 phase-modified files (`TransformTestHelpers.java`,
`TableOperationFilterTest.java`, `HeaderToValueTest.java`, `HeadersToValueTest.java`,
`ConvertToMapOrJsonTest.java`, `SmtSinkLandingTest.java`, `TableOperationFilter.java`,
`HeaderToValue.java`, `HeadersToValue.java`, `ConvertToMapOrJson.java`, `ClickHouseWriter.java`) for
`TBD|FIXME|XXX|TODO|HACK|PLACEHOLDER|@Disabled|assumeTrue` found nothing in the ten
phase-test/transform files. `ClickHouseWriter.java` carries 5 pre-existing `TODO` comments at lines
171, 242, 284, 655, and 1229 — none inside or adjacent to the phase's 3-line diff (lines 446-449),
confirmed via `git show 3929ce6` showing only those 3 lines changed.

### Code Review Findings (context, not gating)

`02-REVIEW.md` (0 critical, 3 warning, 2 info) flags three items worth carrying forward as known,
accepted gaps rather than phase blockers, because none is required by any of the 30 requirement IDs
or the 5 ROADMAP success criteria:

- **WR-01**: no test exercises a null entry inside a `Nullable` map *value* through
  `ClickHouseWriter`'s RowBinary path end-to-end (only reasoned about, and pinned at the SMT-return
  level by HDRS-03). This was an explicit, documented exclusion in 02-05-PLAN.md's prohibitions
  ("No null-valued header in the HDRS-09 sink fixture ... outside this phase's inline-fix scope").
- **WR-02**: the MAP branch (mirroring a pre-existing ARRAY branch pattern) has no guard against a
  null value landing in a *non-nullable* map value slot. Pre-existing pattern, not introduced by
  this phase's 3-line diff, not required by any requirement ID.
- **WR-03**: `HeaderToValue` crashes if `fields` names a pre-existing non-STRING column (a real
  production robustness gap). Not covered by HDR-01..07 as written — those requirements test the
  documented "added optional string field" case, not overwriting an existing typed column.

These are legitimate follow-up candidates but do not block this phase's goal: the goal is that the
fork's *existing, documented* behavior is pinned by tests, and all 30 enumerated requirements are.

### Human Verification Required

None. All must-haves are verifiable programmatically (compiled Java tests with concrete assertions
against a real ClickHouse `latest` container), and all were confirmed via fresh XML test results.

### Gaps Summary

None. All 5 ROADMAP success criteria and all 30 requirement IDs (TOF-01..06, HDRS-01..09,
HDR-01..07, CONV-01..08) have named, passing tests confirmed directly in the current source tree
(not merely claimed in SUMMARY.md). The one approved out-of-scope production change
(`ClickHouseWriter` MAP-branch null-marker fix, commit `3929ce6`) is exactly as scoped and recorded
in 02-05-SUMMARY.md, matches the user's checkpoint authorization, and is covered by a passing test.
No debt markers, no stubs, no orphaned requirements, no regressions in the full suite (285 tests,
278 succeeded, 0 failed, 7 pre-existing unrelated skips).

---

_Verified: 2026-09-16T21:45:00Z_
_Verifier: Claude (gsd-verifier)_
