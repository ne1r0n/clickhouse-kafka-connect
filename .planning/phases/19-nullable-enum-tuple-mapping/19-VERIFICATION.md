---
phase: 19-nullable-enum-tuple-mapping
verified: 2026-10-13T00:00:00Z
status: gaps_found
score: 5/6 must-haves verified
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
  - ".planning/scripts/fork-anchors.txt"
  - "docs/FORK-DELTA.md"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/NullableEnumRoundTripLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/NullableEnumWriteTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java"
covered_digest: "v3:sha256:dadeddd793f5d735a5e3de0fee38605835b11e92c1875e64df4a7971f4e8b4e4"
behavior_unverified: 0
overrides_applied: 0
gaps:
  - truth: "Nullable(Tuple(...)) round-trips with element nullability taken from the declared type string, without breaking tuple schemas that wrote correctly before the phase (SC3 / COLS-10; the phase research flag names this exact regression class: a nullability change that silently corrupts tuples that work today)"
    status: failed
    reason: "DeclaredNullability's leaf rule records parsed.isNullable(). For a Tuple element declared SimpleAggregateFunction(f, Nullable(T)), ClickHouseColumn.of leaves the SimpleAggregateFunction node non-nullable and puts the flag on its nested column, so apply() flips the correct nullable=true that Column.extractColumn produced to false. The writer then omits the null marker for that element and the RowBinary stream misaligns for the rest of the row. This schema wrote correctly before Phase 19, so this is a silent-corruption regression in the high-risk write path. Reproduced independently by the verifier (production Column/Table/DeclaredNullability classes; real DESCRIBE on ClickHouse 26.9.17.8). Review finding CR-01 is still disposition: open."
    artifacts:
      - path: "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java"
        issue: "collect() default branch (lines ~111-116) uses parsed.isNullable() without unwrapping SimpleAggregateFunction the way Column.extractColumn does"
      - path: "src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java"
        issue: "No case for a SimpleAggregateFunction(..., Nullable(...)) element (grep for SimpleAggregate and LowCardinality in the file returns nothing), and the live matrix has none either"
    missing:
      - "Unwrap SimpleAggregateFunction (nested column 0, repeated) when reading the declared leaf nullability in DeclaredNullability.collect (review CR-01 gives the fix)"
      - "DeclaredNullabilityTest case: Tuple(a SimpleAggregateFunction(sum, Nullable(Int64)), b Nullable(Int32)) expects c.a=true and c.b=true; add LowCardinality(Nullable(String)) pins as Tuple element, Array element and Map value (they already behave, nothing pins them)"
      - "Disposition CR-01 in 19-REVIEW-DISPOSITION.md (all 8 findings are still open)"
deferred: []
human_verification: []
---

# Phase 19: Nullable Enum & Tuple Mapping Verification Report

**Phase Goal:** `Nullable(Enum8)`/`Nullable(Enum16)` columns store exactly what the record says. `Nullable(Tuple)` either round-trips or stays explicitly warned, with a recorded Known cost. A spike decides which, so a no-go cannot hold the release.
**Verified:** 2026-10-13
**Status:** gaps_found
**Re-verification:** No, initial verification

## Summary

The phase delivered what it set out to. The spike ran before any Tuple production code and recorded `go`. `Nullable(Enum8|Enum16)` round-trips. `Nullable(Tuple)` and `Array(Nullable(Tuple))` round-trip on the live matrix. The anchors invariant passes, DEBT-09 is closed, and `Table.java` is untouched.

One thing stands between this phase and a clean pass. The new `DeclaredNullability` pass introduces a data-corruption regression for one valid schema shape. The verifier reproduced it, so it is a BLOCKER under the project rule "No data loss is acceptable". The fix is small and the review already specifies it. The gap is not a goal failure in the main path. The rest of the phase is sound, so a narrow closure plan is enough.

## Goal Achievement

### Observable Truths

| # | Truth (ROADMAP SC) | Status | Evidence |
|---|--------------------|--------|----------|
| 1 | SC1: A live twin-table spike on `latest` ran before any Tuple production code, covering `Nullable(Tuple)` with nullable and non-nullable elements and `Array(Nullable(Tuple))`, and recorded go/no-go plus the type-string parsing approach | VERIFIED | `19-SPIKE.md` holds the C1-C3 deciding and X1-X3 extra cells on plain (RowBinary) and DEFAULT (RowBinaryWithDefaults) twins, both sides each, a double-flag control, and DESCRIBE rows. Server 26.9.17.8. `## Decision: go` with approach C and fail-open policy. Commit order: spike `fc44b1f6`/`5d4424c7` and gate `12f225cf` precede the Tuple fix `2a94e045`. `enable_nullable_tuple_type` is not set in any test (19-06 scope check). |
| 2 | SC2: `Nullable(Enum8)`/`Nullable(Enum16)` NULL and non-NULL round-trip through RowBinary, top level and in `Array(Nullable(Enum))` and `Map(K, Nullable(Enum))`; D-16/L-13 pins flipped with rationale, none deleted | VERIFIED | `Column.extractColumn` Enum branch carries `.isNullable(isNull)` (diff `598f2caf`). Verifier reran `NullableEnumRoundTripLiveTest` (6 passed) and read `NullableEnumWriteTest` and the adjacent unit tests in the earlier run (14 and 18 passed, Docker-free). Pin flips are renames with the input unchanged and a written rationale comment, not deletions: `nullElementInArrayOfNullableTupleFailsWithDataException` became `...ReadsBackAsSqlNull` and `...ThrowsBecauseColumnDropsTupleNullability` became `...WritesNullMarker`. |
| 3 | SC3 (go): `Nullable(Tuple(...))` round-trips with element nullability from the declared type string, `Table.java` untouched, `Array(Nullable(Tuple))` still round-trips | FAILED (BLOCKER) | Main path verified: `NullableTupleMatrixLiveTest` 30 passed on 26.9.17.8 (C1, C2, C3, X1-X3 on both twins), `DeclaredNullabilityTest` 15 passed, `git diff` of `Table.java`, `ColumnTest`, `TableTest` is empty. But the pass flips `SimpleAggregateFunction(f, Nullable(T))` Tuple elements from correct `nullable=true` to `false` (see Gaps). Previously-correct schemas now misalign. |
| 4 | SC4: Discovery WARN, `fork-anchors.txt` row for `hasNullableEnumOrTuple` and `FORK-DELTA.md` change in the same commit as the code; merge-invariant script passes at every commit | VERIFIED | `598f2caf` and `2a94e045` each touch code, `fork-anchors.txt` and `FORK-DELTA.md` together; the `hasNullableEnumOrTuple` row is removed and COLS-10 rows added. `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` rerun by the verifier: `result=PASS` (anchor_commits=279, anchor_rows=58). No remaining source references to `hasNullableEnumOrTuple`, `warnOnceIfNullable` or `NULLABLE_ENUM_OR_TUPLE`. |
| 5 | SC5: 15-REVIEW IN-01..IN-05 each fixed or closed with a written reason, consistent with the Tuple outcome (DEBT-09) | VERIFIED | `15-REVIEW-DISPOSITION.md` (archived under `.planning/milestones/v1.7.0_gg1-sync-phases/15-...`) shows IN-01..IN-05 `fixed`, each Source cell citing `2a94e045` and the word superseded; `open: 0`. Consistent with the go outcome, since the WARN block they concern is deleted. |
| 6 | The Decision path: a no-go cannot hold the release (19-05 retired) | VERIFIED | `19-05-SUMMARY.md` is `status: superseded`, matching `## Decision` ("Superseded: 19-05"). The user's `go` is recorded in `19-SPIKE.md`. This is correct behaviour, not a gap. |

**Score:** 5/6 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `19-SPIKE.md` | Go/no-go bundle with matrix, approach test and Decision | VERIFIED | Plain and DEFAULT twin matrices (C1-C3 deciding, X1-X3 extra), control, DESCRIBE rows, gate refresh, `## Decision` go |
| `Column.java` | Enum and Tuple branches carry `Nullable`; package setter | VERIFIED | `.isNullable(isNull)` count 3; `@Setter(AccessLevel.PACKAGE)` present |
| `DeclaredNullability.java` | Pairing pass after `Table`, fail open | PARTIAL | Exists, substantive, wired. Leaf nullability rule wrong for `SimpleAggregateFunction` |
| `ClickHouseHelperClient.java` | WARN deleted; shared `buildDescribedTable` used by both describe loops | VERIFIED | V1 and V2 both return through `buildDescribedTable`; `DeclaredNullability.apply(` appears once |
| `ClickHouseWriter.java` | Stale comment only | VERIFIED | Comment-only change |
| `fork-anchors.txt`, `FORK-DELTA.md` | Rows updated in step with code | VERIFIED | Four COLS-10 anchor rows; Known cost item 1 rewritten as "999.1 is closed" |
| `19-BEHAVIOUR-LEDGER.md` | L-31..L-40 | VERIFIED | Ten rows, including L-38 (single-field tuple now `DataException`) |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `describeTableV1` / `describeTableV2` | `DeclaredNullability.apply` | `buildDescribedTable` | WIRED | Both loops collect root raw types and return via the shared helper |
| `buildDescribedTable` | upstream `Table` | `new Table(...)` before the pass | WIRED | Pass runs after the Table has paired sub-columns; Table unchanged |
| `DeclaredNullability` | `ClickHouseWriter` null-marker decisions | `Column.isNullable()` | WIRED, WRONG for one shape | Writer emits marker per `isNullable()`; the flipped flag drops the marker |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `DeclaredNullability.apply` | declared type per root | raw DESCRIBE type of non-subcolumn rows | Yes (live DESCRIBE, re-observed) | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Live Nullable Enum/Tuple round-trips on `latest` x V2 | `gradlew test --rerun` over `NullableEnumRoundTripLiveTest`, `NullableTupleMatrixLiveTest`, `NestedNullRowBinaryLiveTest`, `ClickHouseHelperClientForkLiveTest` (server 26.9.17.8) | 6 + 30 + 10 + 12 passed, 0 failed, 0 skipped | PASS |
| Docker-free Phase 19 classes | same command over `DeclaredNullabilityTest`, `NullableEnumWriteTest`, `NestedNullableCompositeWriteTest`, `ClickHouseHelperClientForkUnitTest` | all passed (15, 14, 18, 44) | PASS |
| SimpleAggregateFunction Tuple element keeps its nullability | verifier probe: production `Column.extractColumn`, `Table`, `DeclaredNullability.apply` on `Tuple(a SimpleAggregateFunction(sum, Nullable(Int64)), b Nullable(Int32))` | `before: c.a=true`, `after apply: c.a=false` | FAIL |
| Same probe, other leaf wrappers | `LowCardinality(Nullable(String))`, `Nullable(Decimal)`, `Nullable(DateTime64)`, `Nullable(Enum8)`, `Nullable(FixedString)`, `Nullable(UUID)`, `Array(Nullable(Int32))`, `Map(String, Nullable(Int32))`, `SimpleAggregateFunction(sum, Int64)` | unchanged before/after | PASS |
| Wire format requires the marker | ClickHouse 26.9.17.8: `Tuple(a SimpleAggregateFunction(sum, Nullable(Int64)), b Nullable(Int32))`, `SELECT ... FORMAT RowBinary` of `(5,7)` | `01000000 00 0500000000000000 00 07000000`, i.e. a marker byte precedes `a` | confirms the corruption mechanism |

The live tests needed `TESTCONTAINERS_HOST_OVERRIDE=localhost` in the verifier's shell (the inherited value was the string `null`). That is a local environment quirk, not a phase defect.

### Probe Execution

Step 7c: SKIPPED. The phase declares no `probe-*.sh` scripts.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| COLS-08 | 19-02 | `Nullable(Enum8|Enum16)` round-trip, top level and nested, D-16/L-13 pins flipped | SATISFIED | Truth 2 |
| COLS-09 | 19-01 (19-03 gate) | Live Tuple spike, go/no-go with parsing approach, before Tuple production code | SATISFIED | Truth 1 |
| COLS-10 | 19-04 (19-05 superseded) | On go, `Nullable(Tuple)` round-trips with declared nullability, `Array(Nullable(Tuple))` still round-trips | BLOCKED (partial) | Main matrix round-trips; regression for `SimpleAggregateFunction(..., Nullable(...))` elements (Truth 3). REQUIREMENTS.md already marks it Complete; that mark should be held until CR-01 is closed |
| COLS-11 | 19-02, 19-04 | WARN, anchors row and FORK-DELTA change in the same commit; invariant script green | SATISFIED | Truth 4 |
| DEBT-09 | 19-06 | 15-REVIEW IN-01..IN-05 fixed or closed with reason | SATISFIED | Truth 5 |

All five IDs in the phase's PLAN frontmatter appear in REQUIREMENTS.md (traceability table maps each to Phase 19). No orphaned requirements: the Phase 19 row of the coverage table lists exactly COLS-08..COLS-11 and DEBT-09.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `DeclaredNullability.java` | ~111-116 | Leaf rule `parsed.isNullable()` without `SimpleAggregateFunction` unwrap | BLOCKER | Silent byte misalignment for Tuple elements wrapped in `SimpleAggregateFunction(..., Nullable(...))` (CR-01) |
| `DeclaredNullability.java` | 86-90 | Only the parse sits in `try/catch`; `collect` unguarded, and `describeTableV1` catches only `ClickHouseException | JsonProcessingException` | WARNING | No current trigger; contradicts the fail-open intent (WR-01) |
| `NullableTupleMatrixLiveTest.java` | 673-676 | `assertThrows(Exception.class)` control test | WARNING | Control passes on any failure (WR-03) |
| `NullableTupleMatrixLiveTest.java` | 106-129 and others | Dead `STORES_WRONG` machinery | INFO | IN-02 |
| `DeclaredNullability.java` | 33-34 | Fallback WARN lacks database/table | INFO | IN-01 |
| `NullableRoundTrip.java` and tests | 46-47, 200-202 | Test copy of `normalise`; duplicated helpers | INFO | IN-03 |
| `DeclaredNullability.java` | 133-143 | Declared element with no Column still "applies" | INFO | IN-04 |

No `TBD`, `FIXME` or `XXX` markers in the files this phase changed.

### Review Findings Weighed

- **CR-01 (critical): confirmed by the verifier and treated as the gap above.** Reasons it blocks despite the narrow trigger: it is a regression (the schema worked before), it fails silently with misaligned bytes and no exception, it sits in a file the project lists as high risk, the project rule is "No data loss is acceptable", and the phase's own research flag names this class of defect. The exposure is limited to Tuples with a `SimpleAggregateFunction` over a `Nullable` element. That is rare but valid, and a CDC fork cannot rule it out from outside.
- **WR-01, WR-03, IN-01..IN-04:** real but not goal-blocking. No trigger for WR-01 was found. WR-03 weakens one control test. They can ride along in the closure plan or be dispositioned.
- **WR-02 (rollout and CHANGELOG still describe the removed WARN):** the cited text is in the already-published `1.7.0_gg1` documents, where it is historically accurate. Phase 20 (ROLL-02, PUB-07) owns the `1.7.0_gg2` rollout note and CHANGELOG section, and Phase 19 recorded the needed behaviour changes (ledger L-31..L-40, FORK-DELTA item 1). So this is a Phase 20 input, not a Phase 19 gap. Phase 20 must carry the L-38 single-field-tuple `DataException`, which matters under `errors.tolerance=none`.
- `19-REVIEW-DISPOSITION.md` still lists all 8 findings as `open`. Triage is outstanding.

### Human Verification Required

None. Every truth resolved to VERIFIED or FAILED from code and live evidence.

### Gaps Summary

One gap, one root cause: `DeclaredNullability.collect` reads the declared leaf nullability from the parsed node without unwrapping `SimpleAggregateFunction`, so a Tuple element declared `SimpleAggregateFunction(f, Nullable(T))` loses its null marker. `Column.extractColumn` already unwraps this wrapper, so the pass disagrees with the mapper it is meant to correct. The fix is a loop that follows nested column 0 while the node is a `SimpleAggregateFunction`, plus the missing unit pins. Closing it should not change any matrix result, since none of the matrix cells contain the wrapper. After the fix, re-run `DeclaredNullabilityTest` and the live matrix, add the live or Docker-free pin, and set CR-01 to `fixed` in the disposition file. The other seven review findings can be dispositioned in the same pass.

Phase 20 must not publish `v1.7.0_gg2` with CR-01 open.

---

_Verified: 2026-10-13_
_Verifier: Claude (gsd-verifier)_
