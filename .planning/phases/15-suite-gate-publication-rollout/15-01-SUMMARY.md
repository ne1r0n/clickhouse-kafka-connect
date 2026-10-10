---
phase: 15-suite-gate-publication-rollout
plan: 01
subsystem: sink-json-writer-and-table-discovery
tags: [debezium, datetime64, metadata-free-json, describe-table, nullable-enum, nullable-tuple, slf4j-warn, fork-anchors]

requires:
  - phase: 13-fork-hardening-on-the-merged-tree
    provides: MetadataFreeJsonWriter and the twin-table parity harness; 13-REVIEW WR-02 and WR-05 findings
  - phase: 14-dependency-bumps-ci
    provides: green dependency set the new pins run against
provides:
  - Debezium Timestamp and NanoTimestamp converted to decimal seconds on the metadata-free JSON route (13-REVIEW WR-02 closed)
  - One WARN per column per process for Nullable(Enum8|Enum16|Tuple) raw types at table discovery (D-02, D-15)
  - fork-anchors.txt rows for the two Debezium constants and the D-02 predicate
affects: [15-02, 15-03, 15-05, 15-06, 15-07]

plan_head_before: 672663c258c1cd618041e5a3f6ec3613a24e9798
plan_head_after: 4a91b92ce8bb68ee030a1dadf3ff46b8495a81ad

actuals:
  tokens: 6100
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Pure package-private static predicate plus a process-wide ConcurrentHashMap.newKeySet() dedup set for once-per-key logging"
    - "SLF4J {} parameters carrying catalog metadata only, never record data"

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriter.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/MetadataFreeJsonTypeParityLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java
    - .planning/scripts/fork-anchors.txt

key-decisions:
  - "D-01: Debezium Timestamp is written as BigDecimal.valueOf(long, 3) and NanoTimestamp as BigDecimal.valueOf(long, 9); NanoTimestamp small values render in Jackson scientific notation (0E-9, 1E-9, -1E-9) and the server accepts them (RESEARCH A7 closed)"
  - "D-02: the Nullable(Enum|Tuple) WARN is deduplicated process-wide on database.table.column:rawType, with no production reset hook; tests use unique table names"

patterns-established:
  - "Seen-RED for a fix-before-test slice: write the tests against the unfixed writer, record the failures, then add the cases"

requirements-completed: [PUB-02, ROLL-01]

coverage:
  - id: D1
    description: "A Debezium Timestamp (epoch ms) and NanoTimestamp (epoch ns) record written through a sink task on the metadata-free JSON route reads back from DateTime64(3) and DateTime64(9) as the same value the RowBinary route stores, including 0, 1 and -1"
    requirement: PUB-02
    verification:
      - kind: integration
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/MetadataFreeJsonTypeParityLiveTest.java#debeziumTimestampAndNanoTimestampReachRowBinaryParityOnTheMetadataFreeRoute"
        status: pass
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java#cdcDebeziumTimestampsAreDecimalSecondsAndZonedTimestampPassesThrough"
        status: pass
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java#debeziumTimestampEdgeValuesAreExactDecimalSeconds"
        status: pass
    human_judgment: false
  - id: D2
    description: "A table with a Nullable(Enum8), Nullable(Enum16) or Nullable(Tuple) slot gets exactly one WARN per column per process at table discovery, from both describe loops, naming database, table, column and raw type and no record data"
    requirement: ROLL-01
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java#nullableEnumOrTupleTypesAreFlagged"
        status: pass
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java#warnOnceDedupesPerColumnAndRawType"
        status: pass
      - kind: integration
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java#describeTableWarnsOnceForNullableEnumOrTupleColumns"
        status: pass
    human_judgment: false
  - id: D3
    description: "The WARN text wording (stored as NULL or shifted, DataException, pointer to FORK-DELTA Known costs and backlog 999.1) is accurate and actionable for an operator"
    verification: []
    human_judgment: true
    rationale: "Wording adequacy for operators is a judgment call; tests pin the mechanism, not the prose"

duration: 10min
completed: 2026-10-10
status: complete
---

# Phase 15 Plan 01: Debezium timestamp fix and Nullable(Enum|Tuple) discovery warning Summary

**The metadata-free JSON route now converts Debezium `Timestamp` and `NanoTimestamp` to decimal seconds (closing 13-REVIEW WR-02, proven value for value against RowBinary on a live ClickHouse), and table discovery warns once per column for `Nullable(Enum8|Enum16|Tuple)` slots.**

## Performance

- **Duration:** 10 min
- **Started:** 2026-10-10T10:26:39Z
- **Completed:** 2026-10-10T10:37Z
- **Tasks:** 2
- **Files modified:** 7 (2 in `src/main`, 4 test files, 1 gate-data file)

## Accomplishments

- D-01: `MetadataFreeJsonWriter` writes `io.debezium.time.Timestamp` as `BigDecimal.valueOf(v, 3)` and `io.debezium.time.NanoTimestamp` as `BigDecimal.valueOf(v, 9)`. A `ZonedTimestamp` string still passes through. The live twin-table cell (ids 1..4: a 2024 value, 0, 1, -1) matches RowBinary value for value, with anchors on the reference side (4 rows, `2024-01-01 00:00:00.123`, `1704067200123456789` ns, `-1` ms).
- D-02/D-15: `ClickHouseHelperClient.hasNullableEnumOrTuple(String)` and `warnOnceIfNullableEnumOrTuple(...)`, hooked once into each of `describeTableV1` and `describeTableV2` between the structural skip and `Column.extractColumn`. Deduplicated process-wide on `database.table.column:rawType`.
- `fork-anchors.txt` gained three rows (two Debezium constants, one D-02 predicate), each in the commit that introduced it. The invariant script prints `result=PASS` after each commit.
- `Column.java` is byte-unchanged; the only `src/main` files touched are the two named in D-04.

## Task Commits

1. **Task 1: Debezium Timestamp/NanoTimestamp on the metadata-free route (D-01)** - `981a89d6` (fix)
2. **Task 2: once-per-column Nullable(Enum8|Enum16|Tuple) WARN (D-02, D-15)** - `4a91b92c` (feat)

**Plan metadata:** recorded in the docs commit that follows this file.

## The exact WARN text

SLF4J format string, four `{}` parameters (database, table, column, raw type):

```
Column {}.{}.{} has type {}, which contains Nullable(Enum8), Nullable(Enum16) or Nullable(Tuple(...)). The connector writes such slots without their null marker, so values in that slot may be stored as NULL or shifted with no error, and a null entry fails the batch with DataException. See Known costs in docs/FORK-DELTA.md (backlog 999.1).
```

Sample rendered line from the live run: `Column kafka_connect_test_..._.d02_nullable_enum_tuple_....t has type Array(Nullable(Tuple(a Int32))), which contains ...`.

## Seen-RED evidence

**Task 1 (D-01).** The plan prescribes deleting the new `case` blocks after the fact. The tests were instead written first against the unfixed writer (equivalent evidence, and the order TDD requires), then the cases were added.

Run against the unfixed writer (`./gradlew test --rerun --tests '*MetadataFreeJsonWriterTest' --tests '*MetadataFreeJsonTypeParityLiveTest'`, 50 tests, 8 failed):

- Unit `cdcDebeziumTimestampsAreDecimalSecondsAndZonedTimestampPassesThrough` failed on the assertion (raw `1704067200123` where `1704067200.123` was expected).
- Unit `debeziumTimestampEdgeValuesAreExactDecimalSeconds`, all six invocations failed (for example `expected: {"t":0.000} but was: {"t":0}`).
- Live `debeziumTimestampAndNanoTimestampReachRowBinaryParityOnTheMetadataFreeRoute` failed because the server rejected the JSON batch: `Code: 407 ... Numeric value is out of range for DateTime64: (while reading the value of key nts) (DECIMAL_OVERFLOW)`. The raw nanosecond long is read as seconds, so the bug is a hard batch failure for `NanoTimestamp` and a silent clamp for `Timestamp`.
- The two non-Number fall-through cases passed before the fix, as expected: they pin that the new cases do not change fall-through behaviour.

After adding the cases: same command, `BUILD SUCCESSFUL`. Semantic assessment: the target tests executed and failed on the planned assertions (or the planned server rejection), not on a setup, compile or fixture fault. No mutation was committed.

**Task 2 (D-02).** Two mutations applied to the working tree after the implementation, each reverted (file compared byte-identical to a saved copy afterwards):

1. Hook statement removed from the `describeTableV2` loop: `ClickHouseHelperClientForkLiveTest > D-02: describeTable registers the Nullable(Enum8|Enum16|Tuple) warning ...` FAILED (1 of 12).
2. `hasNullableEnumOrTuple` forced to `return false`: 7 of 44 `ClickHouseHelperClientForkUnitTest` tests FAILED, namely the six `D-02: ... is flagged` cases and `D-02: the warning fires once per database, table, column and raw type, and again when the raw type changes`.

## Pinned edge-value strings (A7 closed)

`MetadataFreeJsonWriterTest#debeziumTimestampEdgeValuesAreExactDecimalSeconds` pins the exact bytes:

| Logical name | Value | Bytes written |
|---|---|---|
| `io.debezium.time.Timestamp` | 0 | `0.000` |
| `io.debezium.time.Timestamp` | 1 | `0.001` |
| `io.debezium.time.Timestamp` | -1 | `-0.001` |
| `io.debezium.time.NanoTimestamp` | 0 | `0E-9` |
| `io.debezium.time.NanoTimestamp` | 1 | `1E-9` |
| `io.debezium.time.NanoTimestamp` | -1 | `-1E-9` |

`NanoTimestamp` at scale 9 renders in scientific notation because Jackson emits `BigDecimal.toString()`. The live cell stores all three at RowBinary parity on ClickHouse 26.9.14.10, so the server accepts the scientific form. Pre-1970 (`-1`) is accepted too (RESEARCH A7 closed). No format or scale change was needed.

## Live D-02 table kept the Tuple column

Yes. `t Array(Nullable(Tuple(a Int32)))` was created without any server setting on ClickHouse 26.9.14.10, so no fallback was needed and the live table has all three flagged columns (`e Nullable(Enum8)`, `m Map(String, Nullable(Enum16))`, `t Array(Nullable(Tuple))`) plus the unflagged `n Array(Nullable(Int32))`.

## Invariant summary line

After commit 1: `invariants: provenance_paths=21 anchor_commits=101 anchor_rows=42 marker_commits=148 result=PASS`
After commit 2: `invariants: provenance_paths=21 anchor_commits=102 anchor_rows=43 marker_commits=149 result=PASS`

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriter.java` - `DEBEZIUM_TIMESTAMP`, `DEBEZIUM_NANO_TIMESTAMP`, `NANOS_SCALE`, two `writeLogicalValue` cases, corrected Javadoc
- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` - predicate, warn-once method, pattern, message constant, dedup set, one hook per describe loop
- `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java` - renamed pin flipped to the correct bytes with a tightening comment, six edge invocations, two non-Number invocations
- `src/test/java/com/clickhouse/kafka/connect/sink/MetadataFreeJsonTypeParityLiveTest.java` - new RowBinary-parity cell
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java` - 15 Docker-free `D-02:` invocations
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java` - live wiring pin
- `.planning/scripts/fork-anchors.txt` - three new rows, all backing PUB-02

## Decisions Made

- Matching precision only in the live cell (Timestamp into `DateTime64(3)`, NanoTimestamp into `DateTime64(9)`), because RowBinary writes the raw long as ticks and is itself correct only there.
- The WARN dedup set is static and has no reset hook in production code, per the plan; tests use a unique table name per case.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] The plan's XML result-count script cannot match display names**
- **Found during:** Task 1 verification
- **Issue:** the `<automated>` python check filters `testcase` names with `startswith('debeziumTimestampAndNanoTimestamp')` and `startswith('cdcDebeziumTimestampsAreDecimalSeconds')`, but Gradle's XML records the `@DisplayName` text for these tests, so the live count printed `live 0` although the test passed. The Task 2 script already tolerates this (it filters on `D-02:`).
- **Fix:** ran the same check with the filter on the display-name prefix (`D-01 (13-REVIEW WR-02): io.debezium.time.Timestamp into` for the live cell, `D-01` for the unit cases). Result `live 1 unit 9`, exit 0. No test or production change.
- **Files modified:** none
- **Verification:** exit 0 on the corrected filter
- **Committed in:** n/a

**2. [Rule 3 - Blocking] Seen-RED taken before the fix instead of by deleting the cases afterwards**
- **Found during:** Task 1
- **Issue:** the plan orders implementation, then temporary case removal. Writing the tests first yields the same failing-test evidence without a mutation step.
- **Fix:** see "Seen-RED evidence" above. Same tests, same failures, no mutation to revert.
- **Files modified:** none beyond the plan's list
- **Verification:** failing test names recorded above
- **Committed in:** `981a89d6`

**3. Task 1 commit amended once.** The first commit (`05169c40`) wrote the comment as "Tightening per D-01"; the acceptance criterion asks for the lowercase word `tightening`, so the comment was reworded and the commit amended (local, unpushed) to `981a89d6`.

---

**Total deviations:** 2 auto-fixed (both Rule 3, both about verification procedure, neither changes production behaviour), 1 housekeeping amend.
**Impact on plan:** none on scope. `src/main` changes remain exactly the two files in D-04.

## Issues Encountered

None beyond the deviations above.

## Known Stubs

None.

## Threat Flags

None. The WARN carries database, table, column and raw type only; it runs in the describe loop, never in the write path (T-15-02, T-15-04). The encoding goes through `BigDecimal.valueOf(long, scale)` and `gen.writeNumber`, never string formatting (T-15-01). T-15-05 (retry of a v1.6.0_gg1 `BEFORE_PROCESSING` batch re-encoded with the new bytes) is carried by the D-07 pre-upgrade check that plan 15-05 writes.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- The last `src/main` changes of the phase are in. 15-02 (review) and later plans describe or measure this exact code.
- 15-03 owes the ledger entries L-20 (Timestamp/NanoTimestamp conversion) and L-21 (the WARN), and closing the 13-REVIEW WR-02 disposition. 15-05 owes the exactly-once pre-upgrade check; 15-06 owes the FORK-DELTA Known costs entry the WARN points at.

## Self-Check: PASSED

- All seven files in `key-files.modified` exist on disk.
- Commits `981a89d6cbe8a624d44f7b7ec56562a658ea334c` and `4a91b92ce8bb68ee030a1dadf3ff46b8495a81ad` are ancestors of HEAD; `git rev-list --count 672663c2..HEAD` was 2 before this file.
- `D01-SOURCE-OK` and `D02-SOURCE-OK` printed; invariant script `result=PASS` after each commit; `Column.java` unchanged; `testUnit` 454 tests, 0 failures, 0 errors.

---
*Phase: 15-suite-gate-publication-rollout*
*Completed: 2026-10-10*
