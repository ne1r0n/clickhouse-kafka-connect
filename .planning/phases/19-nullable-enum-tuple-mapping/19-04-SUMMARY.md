---
phase: 19-nullable-enum-tuple-mapping
plan: "04"
subsystem: sink-db-mapping
tags: [tuple, nullable, describe, declared-nullability, rowbinary, fork-delta]

requires:
  - phase: 19-nullable-enum-tuple-mapping (19-01)
    provides: Tuple spike matrix (NullableTupleMatrixLiveTest) and NullableRoundTrip harness
  - phase: 19-nullable-enum-tuple-mapping (19-02)
    provides: Nullable(Enum) fix and the discovery WARN this plan removes
  - phase: 19-nullable-enum-tuple-mapping (19-03)
    provides: recorded go for approach C with fail-open per root
provides:
  - Nullable(Tuple(...)) and Array(Nullable(Tuple(...))) round-trip through the real sink task on both twins, with element nullability from the declared type
  - DeclaredNullability pass run once per root column that contains a Tuple, once per DESCRIBE, after upstream's Table is built (Table.java untouched)
  - The Nullable(Tuple) discovery WARN, its anchors row and its FORK-DELTA row removed
  - Docker-free and live pins for the pass, including fail open and the omitted-field effect
affects: [19-06, 20-release, FORK-DELTA, CHANGELOG, ROLL-02]

actuals:
  tokens: 21000
  tasks: 2
  commits: 2
plan_head_before: 5155475b4dada721991f572ce928c0d32e0209e9
plan_head_after: ea5b856d58a13ba7020146942187e6aca5a6fba2

tech-stack:
  added: []
  patterns:
    - "Post-Table pass: fork logic runs on the Column tree after upstream's Table, so Table.java stays upstream-identical"
    - "Fail open per root with an all-or-nothing assignment list and a metadata-only WARN"
    - "Flip, never delete: old-behaviour pins are renamed with a COLS-10 rationale and keep their class case count"

key-files:
  created:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/NestedNullRowBinaryLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - .planning/phases/19-nullable-enum-tuple-mapping/19-BEHAVIOUR-LEDGER.md

key-decisions:
  - "Approach C with fail open, as approved at the 19-03 gate: the declared type string is parsed with ClickHouseColumn.of and paired onto the Column tree by element name (1-based position for unnamed elements)"
  - "D-10: the whole discovery WARN machinery is deleted in the same commit as the fix; D-11: every WARN pin is flipped to a negative pin, never deleted"
  - "nullTupleFollowsColumnNullability stays unchanged: it hand-builds its Tuple Column and never reaches Column.extractColumn or the pass"
  - "Extras X1, X2, X3 are pinned as observed after the fix (see Accomplishments); they could not veto (D-07)"

patterns-established:
  - "A mutation that removes only one of two redundant sources (the pass call, or the Tuple builder flag) leaves the omitted-field case green; both must be removed to see it red"

requirements-completed: [COLS-10, COLS-11]

coverage:
  - id: D1
    description: "Nullable(Tuple(...)) (C1, C2) and Array(Nullable(Tuple(...))) (C3) round-trip through the real ClickHouseSinkTask on PLAIN and DEFAULT twins, and the DESCRIBE-derived Column tree has the same per-node nullability as the declared tree"
    requirement: COLS-10
    verification:
      - kind: integration
        ref: "NullableTupleMatrixLiveTest side (a), 30/30 on ClickHouse latest, client V2"
        status: pass
    human_judgment: false
  - id: D2
    description: "A NULL element of Array(Nullable(Tuple(a Int32))) writes the null marker (02 00 05 00 00 00 01) and reads back as SQL NULL with its neighbour intact (ROADMAP criterion 3 flip)"
    requirement: COLS-10
    verification:
      - kind: unit
        ref: "NestedNullableCompositeWriteTest#nullElementInArrayOfNullableTupleWritesNullMarker (18/18)"
        status: pass
      - kind: integration
        ref: "NestedNullRowBinaryLiveTest#nullElementInArrayOfNullableTupleReadsBackAsSqlNull (10/10)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Pairing edges: nested Tuple under a Nullable parent stays non-nullable, mixed element flags, unnamed positional elements, backtick names, a missing declared element, the multi-line escaped form"
    requirement: COLS-10
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest, 15/15 (testUnit)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Fail open: an unparsable declared type, a structure mismatch and an unmatched element each leave the root as DESCRIBE reported it and log one WARN with column name, raw type and no exception text"
    requirement: COLS-10
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest (NoSuchType, structure-mismatch, unmatched-element cases)"
        status: pass
    human_judgment: false
  - id: D5
    description: "A record that omits a Nullable(Tuple) column without a DEFAULT stores NULL on both twins (ledger L-37)"
    requirement: COLS-10
    verification:
      - kind: integration
        ref: "NullableTupleMatrixLiveTest#omittedNullableTupleFieldStoresNull (2 cases)"
        status: pass
    human_judgment: false
  - id: D6
    description: "The WARN machinery, its anchors row and its FORK-DELTA row are gone in the fix commit and every WARN pin is flipped with its class count kept (ForkUnit 44, ForkLive 12)"
    requirement: COLS-11
    verification:
      - kind: other
        ref: "GO-T1-SOURCE-OK, GO-T1-COMMIT-OK, GO-T1-TESTS-GREEN; verify-merge-invariants.sh 54e63648 HEAD result=PASS"
        status: pass
    human_judgment: false
  - id: D7
    description: "Retried batches stay byte-identical: the pass runs once per DESCRIBE before the Table is published, never per record; Processing, ProxySinkTask, state and kafka packages are unchanged"
    requirement: COLS-10
    verification:
      - kind: other
        ref: "git diff --quiet ede0a45d..HEAD over Table.java, processing, ProxySinkTask, state, kafka, ColumnTest, TableTest (GO-T1-SOURCE-OK)"
        status: pass
    human_judgment: true
    rationale: "Byte identity on retry is an exactly-once design property; the diff proves the code paths were not touched, and a reviewer should confirm the pass cannot depend on anything but the published Table"
  - id: D8
    description: "Rows already stored wrongly in Nullable(Tuple) slots are not repaired"
    requirement: COLS-10
    verification: []
    human_judgment: true
    rationale: "A prohibition (no repair of stored rows) satisfied by absence: no code touches stored data; judged by reading the diff"

duration: 17min
completed: 2026-10-13
status: complete
---

# Phase 19 Plan 04: Declared-Nullability Tuple Fix Summary

**Tuple element nullability now comes from the declared type string (`ClickHouseColumn.of` paired onto the Column tree after upstream's `Table`, fail open per root), so `Nullable(Tuple(...))` and `Array(Nullable(Tuple(...)))` round-trip on both twins, with the discovery WARN and its anchors and FORK-DELTA rows deleted in the same commit.**

## Performance

- **Duration:** about 17 min (Task 1 about 9 min in a prior context, Task 2 and close-out about 8 min in this one)
- **Tasks:** 2 (Task 1 tracer, Task 2 auto/TDD)
- **Files:** 13 in the fix commit; 3 in the expansion commit (a subset)

## Accomplishments

- **The pass (Task 1).** New `DeclaredNullability` (`containsTuple`, `apply`, `normalise`) is called from `ClickHouseHelperClient.buildDescribedTable`, which both `describeTableV1` and `describeTableV2` return through. `Column` gained a package-level `isNullable` setter and carries `Nullable` on the Tuple branch. `Table.java` is unchanged.
- **WARN removed (D-10).** Pattern, text, dedup set, predicate, logging method and both describe-loop hooks are gone, with `fork-anchors.txt` row 57 and the FORK-DELTA WARN row. Known costs item 1 now reads "Backlog 999.1 is closed" and item 5 is rewritten.
- **Pins flipped (D-11), not deleted.** `ClickHouseHelperClientForkUnitTest` keeps 44 cases, `ClickHouseHelperClientForkLiveTest` 12, `NestedNullableCompositeWriteTest` 18, `NestedNullRowBinaryLiveTest` 10.
- **Extras, pinned as observed after the fix:**
  - X1 `Nullable(Tuple(a Int32))`: id 1 and id 2 (NULL tuple) round-trip; id 3 (null `a`) fails with a `DataException` naming tuple field `a` (ledger L-38; before, a NULL tuple was stored silently).
  - X2 `Map(String, Nullable(Tuple(a Int32, b String)))`: round-trips; a null map value stores NULL.
  - X3 `Nullable(Tuple(a Int32, n Tuple(d String, e Nullable(Int32))))`: round-trips; the nested tuple `n` stays non-nullable.
- **Edge pins (Task 2).** `DeclaredNullabilityTest` 7 -> 15: unnamed positional pairing, backtick names (`a b`, `c,d`), a missing declared element, `NoSuchType` fail open, structure-mismatch fail open, an unmatched element applying nothing, `containsTuple` false cases (including `null`), a null declared type and `normalise`. The fail-open WARN is asserted to be exactly one event with the column name and raw type and with no throwable text. `NullableTupleMatrixLiveTest` 28 -> 30: the omitted-field case on both twins.

### Seen red (TDD evidence)

- **Before the fix (Task 1, flipped expectations on unchanged code):** `NullableTupleMatrixLiveTest` side (a) c1, c2, c3, x1, x2, x3 on PLAIN and DEFAULT (12 cases); `NestedNullRowBinaryLiveTest#nullElementInArrayOfNullableTupleReadsBackAsSqlNull` (DataException: element null in non-nullable slot); `NestedNullableCompositeWriteTest#nullElementInArrayOfNullableTupleWritesNullMarker` (DataException).
- **Mutation, pass call commented out (Task 2, never committed):** 13 failures: side (a) c1, c2, c3, x1, x2, x3 on both twins (12) and `NestedNullRowBinaryLiveTest#nullElementInArrayOfNullableTupleReadsBackAsSqlNull`. Restored with `git checkout`; `git diff --quiet -- src/main` succeeded.
- **Omitted-field case:** green under the pass-call mutation alone and green under a Tuple-builder-flag mutation alone (either source makes the tuple nullable); red on both twins (`COLS-10 omitted Nullable(Tuple) field stores NULL PLAIN` and `... DEFAULT`) when both were removed together (14 failures in that run). Restored.
- **Docker-free edge pins** characterise behaviour the fix commit already delivers; they were green on first run against the real code and have no separate red state.

### Renamed test methods (old -> new)

- `nullableTupleTypesAreFlagged` -> `formerlyFlaggedTupleTypesDiscoverWithDeclaredNullabilityAndNoWarning`
- `otherTypesAreNotFlagged` -> `otherTypesDiscoverWithoutWarningAndUnchangedRootNullability`
- `nullRawTypeIsNotFlagged` -> `missingDeclaredTypeKeepsDescribeNullabilityWithoutWarning`
- `warnOnceDedupesPerColumnAndRawType` -> `repeatedDiscoveryNeverWarns`
- `subColumnsAndUnflaggedTypesNeverWarn` -> `subColumnRowsNeverWarn`
- `describeTableWarnsOnceForNullableTupleColumnsOnly` -> `describeTableNoLongerWarnsAndCarriesDeclaredNullability`
- `nullElementInArrayOfNullableTupleThrowsBecauseColumnDropsTupleNullability` -> `nullElementInArrayOfNullableTupleWritesNullMarker`
- `nullElementInArrayOfNullableTupleFailsWithDataException` -> `nullElementInArrayOfNullableTupleReadsBackAsSqlNull`

`NestedNullableCompositeWriteTest#nullTupleFollowsColumnNullability` is **unchanged** on purpose: it hand-builds its Tuple Column and never reaches `Column.extractColumn` or the declared-nullability pass.

### Gates

- Task 1 markers: GO-T1-TESTS-GREEN, GO-T1-SOURCE-OK (see deviation 1), GO-T1-COMMIT-OK, GO-T1-FORMAT-OK.
- Task 2: GO-T2-TESTS-GREEN (DeclaredNullabilityTest 15, NullableTupleMatrixLiveTest 30, NestedNullRowBinaryLiveTest 10, ClickHouseHelperClientForkLiveTest 12, client V2), GO-T2-COMMIT-OK.
- Anchors gate: `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` -> `invariants: provenance_paths=21 anchor_commits=272 anchor_rows=58 marker_commits=319 result=PASS` (ANCHORS-19-04-PASS).
- Client V1: structural only (both describe loops return through `buildDescribedTable`); no V1 run, as the plan states.

## Task Commits

1. **Task 1 (tracer): the go commit** - `2a94e045` (fix)
2. **Task 2: edge pins, omitted-field case, ledger Pinned-by rows** - `ea5b856d` (test)

**Plan metadata:** the `docs(19-04)` close-out commit follows this file.

## Files Created/Modified

See the frontmatter `key-files`. `ClickHouseWriter.java` changed in comment lines only; `Table.java`, Processing, ProxySinkTask, state, kafka, ColumnTest and TableTest are unchanged since the phase start.

## Decisions Made

- Approach C with fail open as approved at the gate; a Tuple that cannot be paired keeps what DESCRIBE reported and logs one metadata-only WARN (column name, declared type, exception class name or `structure mismatch`).
- Extras X1 to X3 pinned as observed; X1's id 3 now fails loudly instead of storing a NULL tuple (ledger L-38).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - verify defect] Task 1 second `<automated>` source check cannot pass as written**
- **Found during:** Task 1
- **Issue:** the writer-comment-only test lets the diff's trailing empty string through (`"" in "+-"` is true), so `startswith("//")` fails although all four changed ClickHouseWriter.java lines are `//` comments.
- **Fix:** an otherwise identical copy with an `x and ...` guard printed GO-T1-SOURCE-OK (`/tmp/gsd19/exec/t1-verify-2-fixed.sh`). Any reuse of that filter needs the same guard.
- **Files modified:** none (verification only)

**2. [Rule 1/2 - stale comment] fork-anchors.txt phase comment**
- **Issue:** the `# Phase 19 (v1.7.0_gg2)` comment line became stale once the WARN row was deleted.
- **Fix:** rewritten; comment lines are ignored by the invariant script.
- **Commit:** `2a94e045`

**3. [Cleanup] Unused constants in NullableTupleMatrixLiveTest**
- Removed `SERVER_CANNOT_READ_ALL_DATA`, `WRITER_NULL_INTO_NOT_NULLABLE`, `WRITER_NULL_MAP_VALUE`, `WRITER_NULL_ARRAY_ELEMENT`; the `Expect.STORES_WRONG` and `FAILS` harness stays.
- **Commit:** `2a94e045`

**4. [Scope note] Structural nullability check covers all six cells**
- The check compares the DESCRIBE-derived and declared trees for C1-C3 and the X cells, not only C1-C3; it passes for the X cells.

**5. [Cleanup] Unused import**
- Dropped the unused `RowSnapshots` import from `ClickHouseHelperClientForkLiveTest`.
- **Commit:** `2a94e045`

**6. [Plan premise] The omitted-field case is not red under the plan's mutation**
- **Found during:** Task 2 step 3
- **Issue:** the plan's mutation (skip the pass call) was expected to turn the deciding cells and the D-16 live case red, and it did (13 failures). The new omitted-field case stayed green, because the Tuple builder flag in `Column.extractColumn` also makes the root tuple nullable; the same holds the other way round.
- **Fix:** none needed in code. To see that case red, I removed both sources together (pass call and the Tuple-branch `.isNullable(isNull)`): both twins failed. Ledger L-37 says so. Both mutations were restored with `git checkout`; `git diff --quiet -- src/main` succeeded.
- **Files modified:** none (mutation never committed)

**7. [Process] Ledger edit rides in the test commit**
- L-37 and L-40 `Pinned by` were rewritten from "the following `test(19-04)` commit" to the concrete case names and included in `ea5b856d`, which the plan's file list for Task 2 allows (the ledger is listed).

## Issues Encountered

None beyond the deviations above. No auth gates.

## User Setup Required

None.

## Next Phase Readiness

- 19-06 can run the full-suite gate; the integration fixture `src/testFixtures/proto/schemas/incompatible/union_two_records.json` (single-field `Nullable(Tuple(...))`, integration source set only) now stores NULL for an absent oneof branch, which Phase 20 should note if integration tests run before release.
- CHANGELOG and ROLL-02 should carry ledger rows L-35..L-40; FORK-DELTA already has the COLS-10 row and rewritten Known costs items 1 and 5.
- Rows already stored wrongly in `Nullable(Tuple)` slots by earlier releases are not repaired.

---
*Phase: 19-nullable-enum-tuple-mapping*
*Completed: 2026-10-13*

## Self-Check: PASSED

- Key files exist: DeclaredNullability.java, DeclaredNullabilityTest.java, this SUMMARY.
- Commits `2a94e045` and `ea5b856d` are ancestors of HEAD; `evaluation-scope --commits-only` lists both.
- Task 2 gates re-run in this context: GO-T2-TESTS-GREEN, GO-T2-COMMIT-OK, ANCHORS-19-04-PASS. Task 1 markers were printed in the prior context at its commit.
- Plan verification: one fix commit and one expansion commit; seen red by mutation; both describe loops route through `buildDescribedTable`; anchors invariant PASS; Table.java and the exactly-once code untouched.
