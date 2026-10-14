---
phase: 19-nullable-enum-tuple-mapping
verified: 2026-10-13T11:00:00Z
status: passed
score: 6/6 must-haves verified
covered_files:
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-01-PLAN.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-01-SUMMARY.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-02-PLAN.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-02-SUMMARY.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-03-PLAN.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-03-SUMMARY.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-04-PLAN.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-04-SUMMARY.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-05-PLAN.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-05-SUMMARY.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-06-PLAN.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-06-SUMMARY.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-07-PLAN.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-07-SUMMARY.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-08-PLAN.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-08-SUMMARY.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-09-PLAN.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-09-SUMMARY.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-10-PLAN.md"
  - ".planning/phases/19-nullable-enum-tuple-mapping/19-10-SUMMARY.md"
  - ".planning/scripts/fork-anchors.txt"
  - "docs/FORK-DELTA.md"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/NestedNullRowBinaryLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/NullableEnumRoundTripLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/NullableRoundTrip.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/SimpleAggregateTupleRoundTripLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/NullableEnumWriteTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java"
covered_digest: "v3:sha256:aac3bfc0dabdca8fc1870b3c35532cd9e5acb6f66e48e842610d9963a6ebd86d"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: gaps_found
  previous_score: 5/6
  gaps_closed:
    - "SC3 / COLS-10: Tuple element declared SimpleAggregateFunction(f, Nullable(T)) was flipped to non-nullable by DeclaredNullability (review CR-01)"
  gaps_remaining: []
  regressions: []
gaps: []
deferred: []
advisory:
  - finding: "19-REVIEW WR-01: apply's catch handler dereferences root.getName(), and buildDescribedTable calls containsTuple outside any guard; both paths need a Column with a null name or null type, which Column.extractColumn never produces"
    category: other
    reason: "Unreachable from production callers; no trigger found. Totality of the pass is otherwise pinned by the catch-all and its WARN test."
    evidence_status: "none provided (no reproduction)"
  - finding: "19-REVIEW WR-02: the WARN column-name assertions use the one-letter name c, which also appears in the fixed text (Could), so they cannot fail on a dropped column argument"
    category: other
    reason: "Test-quality weakness, not a behaviour defect; the table-name half of the assertion is real."
    evidence_status: "none provided"
  - finding: "19-REVIEW WR-03: FORK-DELTA names a per-twin server error code (CANNOT_READ_ALL_DATA plain, ATTEMPT_TO_READ_AFTER_EOF defaults) that the control test accepts as either code"
    category: other
    reason: "Documentation precision; the control still fails closed on read-past-end."
    evidence_status: "none provided"
---

# Phase 19: Nullable Enum & Tuple Mapping Verification Report

**Phase Goal:** `Nullable(Enum8)`/`Nullable(Enum16)` columns store exactly what the record says. `Nullable(Tuple)` either round-trips or stays explicitly warned, with a recorded Known cost. A spike decides which, so a no-go cannot hold the release.
**Verified:** 2026-10-13
**Status:** passed
**Re-verification:** Yes, after gap closure (plans 19-07..19-10). The previous report was `gaps_found` on SC3 / COLS-10 (review CR-01).

## Summary

The one blocker from the previous report is closed. `DeclaredNullability` now reads through `SimpleAggregateFunction` the same way `Column.extractColumn` does, so a Tuple element declared `SimpleAggregateFunction(f, Nullable(T))` keeps its null marker. I confirmed this three ways rather than from the summaries. I read the code, I reran the Phase 19 test classes against a live ClickHouse, and I broke the fix on purpose to check that the pins fail. The other five roadmap success criteria were re-checked for regression and still hold.

The new 19-REVIEW (0 critical, 3 warning, 3 info) was weighed. Its findings concern unreachable paths and test or documentation precision. None changes stored data, so they are recorded as advisory and do not hold the phase.

## Goal Achievement

### Observable Truths

| # | Truth (ROADMAP SC) | Status | Evidence |
|---|--------------------|--------|----------|
| 1 | SC1: A live twin-table spike on `latest` ran before any Tuple production code, covering `Nullable(Tuple)` with nullable and non-nullable elements and `Array(Nullable(Tuple))`, and recorded go/no-go plus the parsing approach | VERIFIED | `19-SPIKE.md` holds the C1-C3 deciding and X1-X3 extra cells on a plain (RowBinary) and a DEFAULT (RowBinaryWithDefaults) twin, both sides each, a double-flag control, DESCRIBE rows, and a gate refresh on server 26.9.17.8. `## Decision` is `go` with approach C and the fail-open policy. Commit order is spike `fc44b1f6` before the Tuple fix. No test sets `enable_nullable_tuple_type` (grep over `src/test` is empty). |
| 2 | SC2: `Nullable(Enum8)`/`Nullable(Enum16)` NULL and non-NULL round-trip through RowBinary by typed read-back, top level and in `Array(Nullable(Enum))` and `Map(K, Nullable(Enum))`; D-16/L-13 pins flipped with rationale, none deleted | VERIFIED | `Column.extractColumn` Enum branch carries `.isNullable(isNull)` (diff against `ede0a45d`). I reran `NullableEnumRoundTripLiveTest` (6 passed, live) and `NullableEnumWriteTest` (14 passed, Docker-free). The flipped pins exist under their new names (`nullElementInArrayOfNullableTupleReadsBackAsSqlNull`, `nullElementInArrayOfNullableTupleWritesNullMarker`). The only removed test methods are the eight that pinned the deleted WARN machinery, which SC4 requires gone; the WARN is covered by negative-form pins (`formerlyFlaggedTupleTypesDiscoverWithDeclaredNullabilityAndNoWarning`, `otherTypesDiscoverWithoutWarningAndUnchangedRootNullability`, `repeatedDiscoveryNeverWarns`). |
| 3 | SC3 (go): `Nullable(Tuple(...))` round-trips with element nullability from the declared type string, `Table.java` untouched, `Array(Nullable(Tuple))` still round-trips | VERIFIED | `NullableTupleMatrixLiveTest` 30 passed live (C1-C3, X1-X3, both twins, control). `SimpleAggregateTupleRoundTripLiveTest` 2 passed live (the CR-01 shape, both twins). `DeclaredNullabilityTest` 25 passed. `git diff ede0a45d HEAD` over `Table.java`, `TableTest.java` and `ColumnTest.java` is empty. The CR-01 defect is gone in the code: `collect` now starts with `unwrapSimpleAggregate(declared)` (`DeclaredNullability.java` lines 109-120), and `collectTuple` recurses through `collect`, so tuple elements, array elements and map values all unwrap. |
| 4 | SC4: Discovery WARN, `fork-anchors.txt` row for `hasNullableEnumOrTuple` and `FORK-DELTA.md` change in the same commit as the code; merge-invariant script passes at every commit | VERIFIED | `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD`: `invariants: provenance_paths=21 anchor_commits=301 anchor_rows=59 marker_commits=348 result=PASS`. No remaining reference to `hasNullableEnumOrTuple`, `warnOnceIfNullable` or `NULLABLE_ENUM_OR_TUPLE` in `src`, `docs` or `.planning/scripts`. `fork-anchors.txt` carries the COLS-10 rows including the new `unwrapSimpleAggregate(` anchor (count 2), added with the 19-07 fix. FORK-DELTA line 50 describes the declared-type pass. |
| 5 | SC5: 15-REVIEW IN-01..IN-05 each fixed or closed with a written reason, consistent with the Tuple outcome (DEBT-09) | VERIFIED | The archived `15-REVIEW-DISPOSITION.md` lists IN-01..IN-05 as `fixed`. This is consistent with the go outcome, because the WARN block those findings concern is deleted. The Phase 19 review dispositions are also recorded in `19-REVIEW-DISPOSITION.md`. |
| 6 | The decision path: a no-go cannot hold the release | VERIFIED | The user's `go` is recorded in `19-SPIKE.md`; `19-05` (no-go alternative) is superseded. The spike gated the work and the release is not held. |

**Score:** 6/6 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `19-SPIKE.md` | Go/no-go bundle with matrix, approach test, Decision | VERIFIED | Both twin matrices, control, DESCRIBE rows, `## Decision` go |
| `Column.java` | Enum and Tuple branches carry `Nullable`; package setter | VERIFIED | `.isNullable(isNull)` on Enum and Tuple builders; `@Setter(AccessLevel.PACKAGE) private boolean isNullable` |
| `DeclaredNullability.java` | Pairing pass after `Table`, fail open, SimpleAggregateFunction unwrap | VERIFIED | 168 lines. All-or-none via the `Assignment` list. Catch-all `RuntimeException` fallback with one metadata-only WARN naming column, database, table, raw type and exception class |
| `ClickHouseHelperClient.java` | WARN deleted; shared `buildDescribedTable` used by both describe loops | VERIFIED | V1 and V2 both collect `declaredTypes` for non-subcolumn rows and return through `buildDescribedTable` |
| `ClickHouseWriter.java` | Stale comment only | VERIFIED | Comment-only change (diff 2 lines) |
| `fork-anchors.txt`, `FORK-DELTA.md` | Rows updated in step with code | VERIFIED | Anchor rows present; invariant script PASS |
| `19-BEHAVIOUR-LEDGER.md` | L-31..L-41 | VERIFIED | Eleven rows; L-41 is the SimpleAggregateFunction fix |
| `19-REVIEW-DISPOSITION.md` | Findings triaged | VERIFIED | CR-01 `fixed` (4e133d4e); IN-04 `deferred` with owner and trigger; six advisory items from the newest review recorded `open` |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `describeTableV1` / `describeTableV2` | `DeclaredNullability.apply` | `buildDescribedTable` | WIRED | Both loops return through the shared helper; `DeclaredNullability.apply(` anchor count 1 |
| `buildDescribedTable` | upstream `Table` | `new Table(...)` before the pass | WIRED | Table is built first, then the pass runs per root that `containsTuple` |
| `DeclaredNullability.collect` | `Column.extractColumn` leaf rule | `unwrapSimpleAggregate` | WIRED | Declared tree is read through the wrapper at every depth, matching the mapper |
| `Column.isNullable()` | writer null-marker decisions | `ClickHouseWriter` slots | WIRED | Marker follows the flag; the live round trips confirm the bytes align |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `DeclaredNullability.apply` | declared type per root | raw DESCRIBE type of non-subcolumn rows (live DESCRIBE, re-observed in the matrix logs) | Yes | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Phase 19 classes, live and Docker-free, ClickHouse 26.9.17.8 x client V2 | `TESTCONTAINERS_HOST_OVERRIDE=localhost ./gradlew test --rerun --offline --tests` over `NullableEnumRoundTripLiveTest`, `NullableTupleMatrixLiveTest`, `SimpleAggregateTupleRoundTripLiveTest`, `DeclaredNullabilityTest`, `NullableEnumWriteTest`, `NestedNullableCompositeWriteTest`, `ClickHouseHelperClientForkUnitTest`, `ClickHouseHelperClientForkLiveTest`, `NestedNullRowBinaryLiveTest` | 6 + 30 + 2 + 25 + 14 + 18 + 45 + 12 + 10 = 162 tests, 0 failures, 0 errors, 0 skipped. Server version logged as `26.9.17.8` | PASS |
| CR-01 fix has teeth (verifier mutation) | Temporarily replaced `unwrapSimpleAggregate(declared)` with `declared` in `collect`, reran `DeclaredNullabilityTest`, restored the file | 4 of 25 failed: the CR-01 element case, the CR-01 root-wrapper case, `SimpleAggregateFunction(anyLast, Array(Nullable(Int32)))`, and `SimpleAggregateFunction(anyLast, LowCardinality(Nullable(String)))`. Source restored; `git status --short src` is clean | PASS |
| Anchors invariant | `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` | `result=PASS` | PASS |
| Protected files untouched | `git diff ede0a45d HEAD` over `Table.java`, `TableTest.java`, `ColumnTest.java` | empty | PASS |

The live tests needed `TESTCONTAINERS_HOST_OVERRIDE=localhost` in the verifier's shell because the inherited value was the string `null`. That is a local environment quirk, not a phase defect. I did not repeat the two full-suite runs (84 classes, 980 tests) that 19-10 reports; the Phase 19 classes above are the ones that exercise the changed code.

### Probe Execution

Step 7c: SKIPPED. The phase declares no `probe-*.sh` scripts.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| COLS-08 | 19-02 | `Nullable(Enum8|Enum16)` round-trip, top level and nested; D-16/L-13 pins flipped | SATISFIED | Truth 2 |
| COLS-09 | 19-01 (19-03 gate) | Live Tuple spike with go/no-go and parsing approach before Tuple production code | SATISFIED | Truth 1 |
| COLS-10 | 19-04, 19-07..19-10 (19-05 superseded) | On go, `Nullable(Tuple)` round-trips with declared nullability; `Array(Nullable(Tuple))` still round-trips | SATISFIED | Truth 3, including the closed CR-01 shape |
| COLS-11 | 19-02, 19-04, 19-07..19-10 | WARN, anchors row and FORK-DELTA change in the same commit; invariant green | SATISFIED | Truth 4 |
| DEBT-09 | 19-06 | 15-REVIEW IN-01..IN-05 fixed or closed with reason | SATISFIED | Truth 5 |

Every ID in the PLAN frontmatter (COLS-08, COLS-09, COLS-10, COLS-11, DEBT-09) appears in REQUIREMENTS.md and maps to Phase 19. No orphaned requirements: the Phase 19 row lists exactly these five.

Bookkeeping for the orchestrator: REQUIREMENTS.md still shows COLS-08, COLS-09 and DEBT-09 as unchecked and `Gaps Found` in the traceability table (COLS-10 and COLS-11 are already `Complete`). That reflects the earlier `gaps_found` result and should be updated now that the gap is closed.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `DeclaredNullability.java` | 95-100 | Handler dereferences `root.getName()` | Advisory (WR-01) | Only a null `root` reaches it, and the sole caller never passes one |
| `ClickHouseHelperClient.java` | `buildDescribedTable` | `containsTuple` called outside a guard | Advisory (WR-01) | Needs a Column with a null type, which `extractColumn` never builds |
| `ClickHouseHelperClientForkUnitTest.java`, `DeclaredNullabilityTest.java` | 418; 216, 238, 278 | Column-name WARN assertions use `c` | Advisory (WR-02) | Assertions cannot fail on a dropped column argument |
| `FORK-DELTA.md` | 50 | Per-twin error code not pinned by the control test | Advisory (WR-03) | Documentation precision only |
| `Column.java` | 169 | `FIXME: Map keys doesn't support Decimal` | Info | Upstream comment; the phase diff against `ede0a45d` does not touch the line. Not a phase-introduced debt marker |

No `TBD`, `FIXME` or `XXX` marker was added by this phase.

### Human Verification Required

None. Every truth resolved to VERIFIED from code, live test runs and a mutation check.

### Gaps Summary

No gaps. The previous blocker (CR-01) is fixed in `4e133d4e`, hardened by 19-08 and 19-09, and pinned by Docker-free unit cases, a live round trip on both twins, and a mutation that I reproduced. The fix does not change any matrix result: the matrix still passes at exactly 30 cases.

Carry-forward items for Phase 20 (already recorded in the ROADMAP Phase 20 entry by 19-10): the `1.7.0_gg2` rollout note and CHANGELOG must state that the `Nullable(Tuple)` limitation is closed and the WARN is gone, and must carry the L-38 single-field-tuple `DataException`, which matters under `errors.tolerance=none`. PUB-07 owns the `VERSION` bump and CHANGELOG entry, which Phase 19 left untouched on purpose. IN-04 is deferred with an owner and a re-open trigger.

The three advisory review warnings could be tidied in a later pass (use a column name that cannot appear in the WARN text, widen the guard around `apply`, soften or pin the per-twin code claim), but none blocks the phase goal.

---

_Verified: 2026-10-13_
_Verifier: Claude (gsd-verifier)_
