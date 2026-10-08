---
phase: 13-fork-hardening-on-the-merged-tree
plan: "03"
subsystem: testing
tags: [clickhouse, kafka-connect, rowbinary, nested-null, dataexception, dlq, testcontainers]

requires:
  - phase: 13-fork-hardening-on-the-merged-tree
    provides: "13-01 ClickHouseWriter routing (anchors recorded); 13-07 RowSnapshots typed read-back helper"
provides:
  - "WRT-07: live typed read-back pin that null entries in Map(String, Nullable(V)) (12 types) and Array(Nullable(V)) (4 types) are SQL NULL, with a seen-RED mutation"
  - "WRT-08: nested non-nullable scalar and Tuple slots throw a data-free DataException from doWriteColValue's container loops"
  - "D-09: nested null Array/Map in a non-nullable slot is written as an empty container whether or not the table has DEFAULT columns"
  - "D-10/D-16: TUPLE slots follow Column's reported nullability"
  - "REQUIREMENTS.md dated narrowing note for WRT-08 and ledger entries L-09..L-13"
affects: [13-08 behaviour ledger consolidation, docs/FORK-DELTA.md, Phase 15 rollout checklist]

actuals:
  tokens: 11800
  tasks: 3
  commits: 5
plan_head_before: e1cccd195e52f9d8ed4780b95dd777aeab9c2f7f
plan_head_after: a313adc1957d75e8dd45e56659fd17acdd18710c

tech-stack:
  added: []
  patterns:
    - "Nested-null checks live in the container's own loop (ARRAY forEach, MAP forEach, TUPLE field loop), so the nested context is known without a new parameter on doWriteCol / doWriteColValue"
    - "DataException message built only from Column metadata through one format constant: column name, rendered type, slot kind"
    - "Live failure assertion walks the cause chain for an instanceof DataException, because Processing.doInsert wraps every failure in RuntimeException"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/NestedNullRowBinaryLiveTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java
    - .planning/REQUIREMENTS.md

key-decisions:
  - "WRT-07 needed no writer change: it already held on this tree, so Task 1 is a pin with no RED; its teeth are shown by a throwaway mutation of the MAP NonNull guard"
  - "The ARRAY and MAP loops share isNullCoercedToEmpty(slot) and each keeps its own throw site, so the nested context (element versus map value) stays in the loop that knows it"
  - "Variant slots are excluded from the new check: a nested Variant null keeps falling through to the existing discriminator 255"

patterns-established:
  - "assertNestedNullFailure(ex, column, slotKind, sentinels...): cause-chain walk asserting DataException, column and slot text, and the absence of sentinel record values"

requirements-completed: [WRT-07, WRT-08]

coverage:
  - id: D1
    description: "A null entry in Map(String, Nullable(V)) for 12 value types and in Array(Nullable(V)) for 4 types reads back as SQL NULL, including the only-entry and last-of-three boundaries and exact neighbouring values"
    requirement: WRT-07
    verification:
      - kind: integration
        ref: "NestedNullRowBinaryLiveTest#nullEntriesInNullableMapAndArrayValuesReadBackAsSqlNull"
        status: pass
    human_judgment: false
  - id: D2
    description: "A null in a nested non-nullable Array element, Map value, Tuple field or Tuple slot throws a DataException naming column, type and slot with no record data, and writes no row"
    requirement: WRT-08
    verification:
      - kind: unit
        ref: "NestedNullableCompositeWriteTest#nullElementInNonNullableArrayThrowsDataExceptionWithoutData"
        status: pass
      - kind: unit
        ref: "NestedNullableCompositeWriteTest#nullValueInNonNullableMapThrowsDataExceptionWithoutKey"
        status: pass
      - kind: unit
        ref: "NestedNullableCompositeWriteTest#nullNonNullableTupleFieldThrowsDataException"
        status: pass
      - kind: integration
        ref: "NestedNullRowBinaryLiveTest#nullArrayElementFailsWithDataExceptionAndNoRow"
        status: pass
      - kind: integration
        ref: "NestedNullRowBinaryLiveTest#nullMapValueFailsWithDataExceptionAndNoRow"
        status: pass
      - kind: integration
        ref: "NestedNullRowBinaryLiveTest#nullTupleFieldFailsWithDataExceptionAndNoRow"
        status: pass
    human_judgment: false
  - id: D3
    description: "Nested null Array/Map containers are coerced to empty in tables with and without DEFAULT columns; top-level null Array/Map still read back [] and {}"
    requirement: WRT-08
    verification:
      - kind: unit
        ref: "NestedNullableCompositeWriteTest#nullInnerArrayIsCoercedToEmptyArray"
        status: pass
      - kind: integration
        ref: "NestedNullRowBinaryLiveTest#nullContainersAreCoercedToEmptyWithoutDefaults"
        status: pass
      - kind: integration
        ref: "NestedNullRowBinaryLiveTest#nullContainersAreCoercedToEmptyWithDefaults"
        status: pass
      - kind: integration
        ref: "NestedNullRowBinaryLiveTest#topLevelNullArrayAndMapStillReadBackEmpty"
        status: pass
    human_judgment: false
  - id: D4
    description: "A batch with a nested null fails whole: a 3-record batch with the null in the last record writes zero rows, and with errors.tolerance=all all 3 records reach the DLQ"
    requirement: WRT-08
    verification:
      - kind: integration
        ref: "NestedNullRowBinaryLiveTest#nestedNullInLastRecordFailsTheWholeBatch"
        status: pass
      - kind: integration
        ref: "NestedNullRowBinaryLiveTest#nestedNullUnderToleranceAllSendsWholeBatchToDlq"
        status: pass
    human_judgment: false
  - id: D5
    description: "Operator-visible semantic changes (D-09 coercion, D-16 single-field Array(Nullable(Tuple)) null now throws, Array/Map(Nullable(Enum)) null entries now throw) are recorded in the behaviour ledger for FORK-DELTA"
    requirement: WRT-08
    verification: []
    human_judgment: true
    rationale: "Whether the ledger wording is what the operator rollout checklist needs is an editorial judgement; Phase 15 writes the FORK-DELTA prose"

duration: 13min
completed: 2026-10-08
status: complete
---

# Phase 13 Plan 03: Nested Nulls in RowBinary Summary

**Nested nulls in RowBinary now either round-trip (Nullable slots, Variant 255), coerce to an empty container (null Array/Map in a non-nullable slot), or fail the whole batch with a data-free DataException naming column, type and slot; Map(K, Nullable(V)) null entries are pinned live with a mutation that makes the pin fail.**

## Performance

- **Duration:** 13 min
- **Started:** 2026-10-08T17:28:32Z
- **Completed:** 2026-10-08T17:41:30Z
- **Tasks:** 3
- **Files modified:** 4 (1 created)

## Accomplishments

- WRT-07 pinned by `NestedNullRowBinaryLiveTest#nullEntriesInNullableMapAndArrayValuesReadBackAsSqlNull`: 12 `Map(String, Nullable(V))` types plus 4 `Array(Nullable(V))` types, three records (null last of two, only entry null, null last of three), read back as `isNull`, `mapContains`, `length` and the exact neighbouring value (`Decimal(18,4)` 12.3456, `DateTime64(3)` 2024-01-01 00:00:00.123).
- WRT-08: `ClickHouseWriter.doWriteColValue`'s ARRAY and MAP loops and the TUPLE field loop and early-null branch throw `DataException("column '<name>' (<type>) <slot>: null in non-nullable slot")`. No signature changed; `doWriteCol` and the top level are untouched (D-11).
- D-09: a null Array or Map in a non-nullable nested slot writes varint 0 in every table kind (the no-defaults desync is fixed; with defaults it was accidentally correct).
- Live proof that the DataException is the root cause of the `RuntimeException` from `Processing.doInsert`, that no row lands, that `errors.tolerance=all` DLQs all 3 records of the batch, and that `[[1],[]]`, `{'k':[]}`, `[{'a':1},{}]` read back in both table kinds.

## TDD Gate Compliance

- **RED (Task 1 mutation):** WRT-07 is already satisfied (RESEARCH Spike b2), so Task 1 has no RED. The pin's teeth: in the MAP forEach the NonNull guard `... isNullable() && mapValue != null` was changed to `... isNullable()`, writing a stray NonNull byte before the null marker. `./gradlew test --rerun --tests '*NestedNullRowBinaryLiveTest'` then failed: `ServerException: Code: 33. DB::Exception: Cannot read all data. Bytes read: 157. Bytes expected: 226: (at row 1) : While executing WaitForAsyncInsert. (CANNOT_READ_ALL_DATA)` (the put threw `RuntimeException: Number of records: 3`). Restored with `git checkout -- ClickHouseWriter.java`, `git diff --quiet` exit 0, GREEN re-run `tests="1" failures="0" errors="0"`. The mutation was never committed (commit 45db0bec lists only the test file).
- **RED (Task 2), commit `0f5ebe6f`:** 18 tests in `NestedNullableCompositeWriteTest`, 9 failed, 9 passed (the 7 pre-existing methods plus the empty-container and D-11 and nullable-slot pins, which are meant to pass before and after). Failures, quoted:
  - `Expected org.apache.kafka.connect.errors.DataException to be thrown, but nothing was thrown.` for the Array element (both orders and the lone null), Map value, D-10 non-nullable Tuple and D-16 cases
  - `Unexpected exception type thrown, expected: <DataException> but was: <java.lang.RuntimeException>` with `An attempt to write null into not nullable column 'c.b'` for the Tuple field case
  - `array lengths differ, expected: <7> but was: <6>`, `<4> but was: <3>`, `<9> but was: <8>` for the three D-09 byte pins under `defaultsSupport=false`
- **Semantic assessment of the RED evidence:** each failing target test executed and failed on the planned assertion for the intended reason (a missing DataException, or a missing empty-container byte), not on setup, import, or fixture faults. The `defaultsSupport=true` D-09 iterations passed, matching RESEARCH Spike (b) ("accidentally empty with defaults"). No `gsd_run check tdd-red-evidence` record was produced: `workflow.tdd_mode` is not enabled for this project.
- **GREEN, commit `1ffc147a`:** `NestedNullableCompositeWriteTest` 18/18, `RowBinaryUnsupportedTypeGateTest` 6/6, `ClickHouseWriterTest` 16/16, all `failures="0" errors="0"`.
- **REFACTOR:** none needed. Task 3's live tests passed on first run because the fix was already in; the unit RED above is their failing-first evidence.
- Gate commits: `git log --grep='^test(13-03)'` (RED), `git log --grep='^fix(13-03)'` (GREEN).

## Task Commits

1. **Task 1: WRT-07 live pin with mutation** - `45db0bec` (test)
2. **Task 2: nested-null rules** - `0f5ebe6f` (test, RED), `1ffc147a` (fix, GREEN)
3. **Task 3: live WRT-08 / D-09 / D-16 / DLQ pins** - `1df97132` (test); REQUIREMENTS note `a313adc1` (docs)

**Plan metadata:** committed after this file (docs: complete plan).

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` - `NESTED_NULL_*` constants, `buildNestedNullMessage`, `renderColumnType`, `isNullCoercedToEmpty`, and the nested-null branches in the ARRAY/MAP loops, the TUPLE field loop and the TUPLE early-null branch (72 added lines, 0 removed)
- `src/test/java/com/clickhouse/kafka/connect/sink/NestedNullRowBinaryLiveTest.java` - new; WRT-07 matrix, WRT-08 slot failures, batch/DLQ, D-09, D-16, #820
- `src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java` - 11 new Docker-free methods; no existing method touched (0 removed lines)
- `.planning/REQUIREMENTS.md` - dated narrowing note under the WRT section; the WRT-08 text is unchanged

## Behaviour ledger

**L-09: D-09 coercion.** A null Array or Map in a nested non-nullable slot (`Array(Array(T))` inner, `Map(K, Array(T))` value, `Array(Map(K, V))` element) is written as an empty container whether or not the table has DEFAULT columns. Before this, the empty value appeared only by accident when the table had DEFAULT columns; without them the bytes were missing and the server failed with `ATTEMPT_TO_READ_AFTER_EOF`. Mirrors #820's top-level rule. Decision: D-09. Pinned by `NestedNullableCompositeWriteTest#nullInnerArrayIsCoercedToEmptyArray`, `#nullArrayValueInMapIsCoercedToEmptyArray`, `#nullMapElementInArrayIsCoercedToEmptyMap` and `NestedNullRowBinaryLiveTest#nullContainersAreCoercedToEmptyWithoutDefaults`, `#nullContainersAreCoercedToEmptyWithDefaults`.

**L-10: WRT-08's DataException.** A null in a nested non-nullable scalar or Tuple slot (Array element, Map value, Tuple field, Tuple slot) throws `DataException` with the message `column '<name>' (<rendered type>) <slot>: null in non-nullable slot`, slot being `element`, `map value`, `tuple field '<field>'` or `tuple`. The message is built from `Column` metadata only: no field value, array element, map key or index (D-12). Through `ClickHouseSinkTask.put` the DataException is the root cause of the `RuntimeException(queryId, e)` that `Processing.doInsert` wraps around every failure, then of `Utils.handleException`'s `RuntimeException("Number of records: N", e)`; it is never a bare `DataException` from `put()`. The whole batch fails: no row is written, with `errors.tolerance=none` the task fails on the first offending batch and on every retry of it (the batch is rebuilt identically), and with `errors.tolerance=all` the whole batch (3 of 3 records in the pin) goes to the DLQ through `Utils.handleException`'s tolerance branch. Rollout note: a producer that starts emitting a null into a non-nullable nested slot now stops the task instead of corrupting the stream. Decisions: D-10, D-12. Pinned by `NestedNullRowBinaryLiveTest#nullArrayElementFailsWithDataExceptionAndNoRow`, `#nullMapValueFailsWithDataExceptionAndNoRow`, `#nullTupleFieldFailsWithDataExceptionAndNoRow`, `#nestedNullInLastRecordFailsTheWholeBatch`, `#nestedNullUnderToleranceAllSendsWholeBatchToDlq`.

**L-11: D-10/D-16 Tuple scoping.** `Nullable(Tuple(...))` is legal on 26.9, but `Column.extractColumn` drops Tuple nullability (unchanged), so a DESCRIBE-derived Tuple slot is non-nullable and a null in it throws the nested DataException; a Tuple column built with `isNullable(true)` still writes the null marker `0x01`. A single-field `Array(Nullable(Tuple(a Int32)))` null element moved from `NULL` (a coincidence: the lone `Nullable(Int32)` sub-field marker doubled as the tuple null flag) to DataException. A multi-field `Nullable(Tuple)` was already broken before this plan (`ATTEMPT_TO_READ_AFTER_EOF`). Decisions: D-10, D-16. Pinned by `NestedNullableCompositeWriteTest#nullTupleFollowsColumnNullability`, `#nullElementInArrayOfNullableTupleThrowsBecauseColumnDropsTupleNullability` and `NestedNullRowBinaryLiveTest#nullElementInArrayOfNullableTupleFailsWithDataException`.

**L-12: D-11 top level untouched.** A top-level null in a non-nullable scalar or Tuple column keeps upstream's `RuntimeException("An attempt to write null into not nullable column '<name>'")`, which is not a DataException, so it differs from the nested failure in type and message. A top-level null in a non-nullable Array or Map column still reads back `[]` or `{}` (#820). `doWriteCol` and the 4-arg `doWriteColValue` signature are unchanged. Decision: D-11. Pinned by `NestedNullableCompositeWriteTest#topLevelNullInNonNullableScalarKeepsUpstreamRuntimeException`, the existing WRT-06/WRT-09 methods, and `NestedNullRowBinaryLiveTest#topLevelNullArrayAndMapStillReadBackEmpty`.

**L-13: Nullable(Enum) nested null entries now throw.** `Array(Nullable(Enum8/16))` and `Map(K, Nullable(Enum8/16))` null entries now throw the nested DataException, because `Column.extractColumn`'s Enum branch drops `isNull` and reports the slot non-nullable. Before, such an entry was written as a null marker into a slot the column treated as non-nullable (and `{a:'a', b:null}` read back `{'a':NULL,'b':NULL}`, RESEARCH Spike b2). This rides on the pre-existing Enum nullability defect, which is not fixed here (Pitfall 8) and which plan 13-08 logs to the backlog. Enum is excluded from the WRT-07 matrix for the same reason. Not pinned by a dedicated test; the behaviour follows from the same TUPLE/ARRAY/MAP loop checks pinned in L-10.

## Decisions Made

- Container-loop checks instead of a `nested` parameter (RESEARCH Pattern 1): signatures stay, direct unit calls keep working, and each loop already knows whether it holds an element or a map value.
- Variant slots excluded from the new check and left on today's path (discriminator 255), per D-11/CONTEXT; Nullable slots excluded because their null marker is correct.
- Separate RED (`test`) and GREEN (`fix`) commits for Task 2, following the TDD reference; the plan's "added lines only" acceptance check on the fix commit holds trivially (the fix commit does not touch the test file).

## Deviations from Plan

None - plan executed exactly as written.

The unit and live failures were all the expected ones; the Stop rule (a message without a DataException in the cause chain) never triggered, and no upstream test relied on a nested null being written (`ClickHouseSinkTaskWithSchemaTest` 97 run, 13 skipped as before, 0 failed; `ClickHouseSinkTaskSchemalessTest` 13/13).

## Issues Encountered

- The WRT-07 mutation attempt printed compiler deprecation warnings (`BinaryStreamUtils`, `ClickHouseNode`) that are pre-existing and unrelated.
- The REQUIREMENTS acceptance check (`git log -1 --grep='(13-03)' -- .planning/REQUIREMENTS.md`, then count removed lines) prints 0 for the note commit `a313adc1`. The closing metadata commit also carries `(13-03)` and flips the WRT-07/WRT-08 checkboxes (`- [ ]` to `- [x]`, via `requirements.mark-complete`), so re-running that check after the closing commit reports removed lines for those two checkbox lines. The WRT-08 requirement text itself is unchanged.

## Verification

- `NestedNullRowBinaryLiveTest` 10 tests, `ClickHouseSinkTaskWithSchemaTest` 97 (13 skipped, as before), `ClickHouseSinkTaskSchemalessTest` 13: all `failures="0" errors="0"` (one `--rerun` run, ClickHouse `latest` 26.9.12.8, client V2).
- `./gradlew testUnit --rerun`: 418 tests, 416 succeeded, 0 failed, 2 skipped.
- Invariant script after the fix and docs commits: `invariants: provenance_paths=21 anchor_commits=48 anchor_rows=40 marker_commits=95 result=PASS`.
- Anchors: `if (value.getObject() == null && col.isNullable()) {` count 1; `null in non-nullable slot` count 1; `throw new DataException(buildNestedNullMessage` count 4; both signatures count 1.

## Known Stubs

None.

## Threat Flags

None. T-13-07 (message leaks data) is mitigated and asserted by sentinel values in unit and live tests; T-13-09 (stray marker byte) by byte-level pins and live read-back in both table kinds; T-13-08 is accepted and recorded in L-10.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ledger entries L-09..L-13 are ready for plan 13-08's consolidation and Phase 15's FORK-DELTA prose.
- 13-06 and the remaining Phase 13 plans are unaffected: this plan changed `ClickHouseWriter.java` only inside `doWriteColValue` and added private helpers.

## Self-Check: PASSED

- Created and modified files exist on disk (`NestedNullRowBinaryLiveTest.java`, `NestedNullableCompositeWriteTest.java`, `ClickHouseWriter.java`, `REQUIREMENTS.md`).
- Commits `45db0bec`, `0f5ebe6f`, `1ffc147a`, `1df97132`, `a313adc1` are ancestors of HEAD; `gsd_run check evaluation-scope --plan 13-03 --commits-only` resolved the plan's commits.
- All task `<acceptance_criteria>` re-run and passing (test-file keyword counts, `countRows` absent, anchors, signature counts, 0 removed lines in the WRT-08 requirement text and in the existing test methods).

---
*Phase: 13-fork-hardening-on-the-merged-tree*
*Completed: 2026-10-08*
