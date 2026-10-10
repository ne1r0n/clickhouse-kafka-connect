---
phase: 13-fork-hardening-on-the-merged-tree
plan: "04"
subsystem: testing
tags: [clickhouse, kafka-connect, rowbinary, validateDataSchema, int256, cleanupExtraFields, mockito, testcontainers]

requires:
  - phase: 13-fork-hardening-on-the-merged-tree
    provides: "13-01 and 13-03 settled ClickHouseWriter (anchor counts recorded); 13-07 RowSnapshots typed read-back helper"
provides:
  - "JSONP-08: cleanupExtraFields evaluates the metadata-free predicate before csc.isBypassFieldCleanup(); order pinned with Mockito never()/times(1); a null csc still throws NullPointerException"
  - "TYPE-08: non-nullable, no-default Int256/UInt256 root columns fail fast in validateDataSchema with the actionable DataException; Int128/UInt128 keep writing and round-trip INT64 extremes"
  - "Dated D-13 correction under JSONP-08 in REQUIREMENTS.md; behaviour ledger L-14 and L-15"
affects: [13-08 behaviour ledger consolidation, docs/FORK-DELTA.md P9-IN-01 row (Phase 15), Phase 15 rollout checklist]

actuals:
  tokens: 5600
  tasks: 2
  commits: 3
plan_head_before: eee831d00b2c16d617a9103f1fb9499cbf6c91bd
plan_head_after: dcd2c3840f50bf3c41f152a28e941920b15c4bdd

tech-stack:
  added: []
  patterns:
    - "Guard order pinned by Mockito verify(csc, never()) on a mocked ClickHouseSinkConfig injected through the existing reflective setSinkConfig"
    - "New enum-name case labels share the existing fail-fast block rather than adding a call site, so fork anchor counts stay unchanged"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/WideIntegerTypeGateLiveTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java
    - .planning/REQUIREMENTS.md

key-decisions:
  - "JSONP-08 delivers evaluation order only: no csc == null short-circuit was added and the Javadoc says the predicate's own null check does not make cleanupExtraFields null-safe (D-13)"
  - "TYPE-08 gate covers INT256/UINT256 only: the two labels join the DYNAMIC/UNKNOWN case group, so no new call of buildRowBinaryUnsupportedTypeMessage and no change to the outer nullable/default condition"

patterns-established:
  - "Live fail-fast test walks the cause chain for an instanceof DataException and asserts the generic 'Data schema validation failed.' text is absent"

requirements-completed: [JSONP-08, TYPE-08]

coverage:
  - id: D1
    description: "cleanupExtraFields in metadata-free mode never reads isBypassFieldCleanup(); default mode reads it once; a null config still throws NullPointerException"
    requirement: JSONP-08
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java#JSONP-08 (3 tests)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Non-nullable Int256/UInt256 columns make validateDataSchema throw the actionable DataException without leaking the field value; Nullable(Int256) keeps the doWriteColValue backstop; Int128/UInt128 still accepted"
    requirement: TYPE-08
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java#TYPE-08 (5 cases)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Live put() into Int256/UInt256 fails with the DataException in the cause chain and writes no row; Int128/UInt128 round-trip Long.MAX_VALUE, Long.MIN_VALUE and 0 by typed read-back"
    requirement: TYPE-08
    verification:
      - kind: integration
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/WideIntegerTypeGateLiveTest.java (3 tests)"
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-10-08
status: complete
---

# Phase 13 Plan 04: JSONP-08 guard order and TYPE-08 Int256/UInt256 fail-fast Summary

**cleanupExtraFields now reads the metadata-free predicate before the bypass flag (pinned with Mockito), and non-nullable Int256/UInt256 columns fail fast in validateDataSchema with the column-and-escape-hatch DataException while Int128/UInt128 still round-trip their INT64 extremes.**

## Performance

- **Duration:** 8 min
- **Started:** 2026-10-08T17:47:00Z
- **Completed:** 2026-10-08T17:55:10Z
- **Tasks:** 2
- **Files modified:** 5 (1 created)

## Accomplishments

- JSONP-08: one-line operand swap in `cleanupExtraFields`, a two-line comment, one Javadoc sentence on the predicate, and three Mockito/NPE pins. Behaviour for every non-null config is unchanged.
- TYPE-08: `case "INT256":` and `case "UINT256":` added to the existing fail-fast block in `validateDataSchema`; five Docker-free pins and three live pins.
- REQUIREMENTS.md carries the dated D-13 correction under JSONP-08; the requirement text itself is untouched.
- Invariant script passes after each commit (`result=PASS`); fork anchor counts unchanged (`shouldSkipTableMetadata()` 8, `case "DYNAMIC":` 1, `buildRowBinaryUnsupportedTypeMessage` 5).

## RED evidence

Task 1, before the operand swap (Mockito `never()` test against the old order):

```
org.mockito.exceptions.verification.NeverWantedButInvoked:
clickHouseSinkConfig.isBypassFieldCleanup();
Never wanted here: -> at ClickHouseSinkConfig.isBypassFieldCleanup(ClickHouseSinkConfig.java:127)
```

The other two JSONP-08 tests (default-mode `times(1)`, null-config NPE) already passed, as expected: they pin behaviour the swap must preserve. 23 tests, 22 passed, 1 failed.

Task 2, before the new case labels:

- Docker-free: `validateDataSchema throws the actionable message for INT256` and `... for UINT256` both failed with `Expected org.apache.kafka.connect.errors.DataException to be thrown, but nothing was thrown.` (the method returned false). 11 tests, 9 passed, 2 failed.
- Live: `put() into a non-nullable Int256 column ...` and the UInt256 twin failed with the cause chain ending in the generic text:
  `Number of records: 1 | Number of records: 1 | Topic: [wide_integer_gate_int256_...], Partition: [0], MinOffset: [1], MaxOffset: [1], (QueryId: [...]) | Data schema validation failed. |  ==> expected: <true> but was: <false>`
  The Int128/UInt128 round-trip test passed at that point. 3 tests, 1 passed, 2 failed.

## Task Commits

1. **Task 1: JSONP-08 guard order, honest docs, Mockito order pin** - `f51f3018` (fix); dated requirement note `44d3c34a` (docs)
2. **Task 2: TYPE-08 Int256/UInt256 fail fast** - `dcd2c384` (fix)

**Plan metadata:** recorded in the following `docs(13-04)` commit.

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` - swapped guard in `cleanupExtraFields`, predicate Javadoc sentence, `INT256`/`UINT256` case labels
- `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java` - three JSONP-08 tests (metadata-free never-read incl. empty payload and bypass-also-set; default-mode once; null-config NPE)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java` - parameterised INT256/UINT256 gate test with secret-value assertion, Nullable(Int256) backstop test, parameterised INT128/UINT128 acceptance test (new methods only)
- `src/test/java/com/clickhouse/kafka/connect/sink/WideIntegerTypeGateLiveTest.java` - new live class, three tests
- `.planning/REQUIREMENTS.md` - dated D-13 correction under JSONP-08

## Behaviour ledger

- **L-14 (JSONP-08):** Changes nothing observable for a non-null config. In metadata-free mode `isBypassFieldCleanup()` is no longer read; both operands still return the payload untouched, so the result is identical. A null config still throws `NullPointerException` from `cleanupExtraFields`. FORK-DELTA's P9-IN-01 row (docs/FORK-DELTA.md line 133) must say "evaluation order", not "null-safety"; the row still shows the old operand order and is corrected in Phase 15.
- **L-15 (TYPE-08):** For a non-nullable, no-default Int256/UInt256 root column the operator-visible error changes from "Data schema validation failed." to the actionable DataException (column name, type, `insertFormat=json`, `bypassRowBinary=true`, never the field value). Nullable or defaulted Int256/UInt256 columns, and every run with `bypassSchemaValidation=true`, skip the gate and fail mid-stream with the same message from `doWriteColValue`'s default branch. Through `put()` both surface as the root cause of Processing's RuntimeException. Int128/UInt128 behaviour is unchanged.

## Decisions Made

- No `csc == null` short-circuit and no null-safety claim (D-13); the Javadoc states the limit explicitly.
- Gate narrowed to INT256/UINT256 by sharing the existing case block, so anchor counts stay put and the wording cannot drift from the DYNAMIC/UNKNOWN message.

## Deviations from Plan

None - plan executed exactly as written.

Two small notes, neither a deviation: the live helper asserts the generic validation text is absent from the cause chain, and the empty-payload JSONP-08 case is folded into the metadata-free test (the plan allows three new tests, which this yields).

**Total deviations:** 0
**Impact on plan:** none.

## Issues Encountered

None. `ClickHouseSinkTaskWithSchemaTest` reports 13 skipped, which are the existing named skips, unchanged by this plan.

## Verification

- `MetadataFreeJsonInsertUnitTest`: 23 tests, 0 failures, 0 errors.
- `RowBinaryUnsupportedTypeGateTest`: 11 tests, 0 failures; `WideIntegerTypeGateLiveTest`: 3 tests, failures="0" errors="0" skipped="0"; `ClickHouseSinkTaskWithSchemaTest`: 97 tests, 0 failures, 13 skipped (pre-existing).
- `testUnit`: 431 tests, 429 passed, 0 failed, 2 skipped.
- Acceptance greps: guard line count 1, old order 0, `csc == null` 0, `case "INT256":` 1, `case "UINT256":` 1, `case "DYNAMIC":` 1, `buildRowBinaryUnsupportedTypeMessage` 5, `shouldSkipTableMetadata()` 8 (matches the anchors row); correction note count 1.
- `.planning/scripts/verify-merge-invariants.sh`: `result=PASS` after both fix commits.

## Known Stubs

None.

## Threat Flags

None. The Int256/UInt256 message reuses `buildRowBinaryUnsupportedTypeMessage` (column and type only); the unit test asserts a secret field value is absent (T-13-10).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for 13-08 (behaviour ledger consolidation): L-14 and L-15 are above.
- Phase 15 must correct the FORK-DELTA P9-IN-01 row wording from this plan's ledger.

## Self-Check: PASSED

- Files present: `WideIntegerTypeGateLiveTest.java`, `MetadataFreeJsonInsertUnitTest.java`, `RowBinaryUnsupportedTypeGateTest.java`, `ClickHouseWriter.java`, `REQUIREMENTS.md`.
- Commits reachable from HEAD: `f51f3018`, `44d3c34a`, `dcd2c384`.

---
*Phase: 13-fork-hardening-on-the-merged-tree*
*Completed: 2026-10-08*
