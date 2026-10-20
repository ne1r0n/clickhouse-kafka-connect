---
phase: 19-nullable-enum-tuple-mapping
plan: "08"
subsystem: declared-nullability-pass
tags: [cols-10, cols-11, wr-01, in-01, wr-03, in-03, gap-closure, review-fixes]
requires:
  - phase: 19-nullable-enum-tuple-mapping (19-07)
    provides: DeclaredNullability with SimpleAggregateFunction unwrap and the anchored apply call
provides:
  - DeclaredNullability.apply(Column, String, String, String) is total - any RuntimeException from parse, pairing or assignment falls back with one metadata-only WARN (WR-01)
  - the fallback WARN names database and table (IN-01)
  - the matrix control passes only on a read-past-end server code and logs the matched code per twin (WR-03)
  - tests use production normalisation and one shared Column-tree nullability flattener (IN-03)
affects: [19-09, 19-10, FORK-DELTA, ROLL-02]
actuals:
  tokens: 14000
  tasks: 2
  commits: 2
plan_head_before: 1ca568f33d086931eb1dfa5f47b2b7d94c3611f0
plan_head_after: 448a5ba5e650f0558f992293268803785356534a
tech-stack:
  added: []
  patterns:
    - "One try spanning parse, pairing and assignment, with assignments applied only after the whole root pairs, so a caught exception never leaves a half-updated root"
key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnNullabilityPaths.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/NullableRoundTrip.java
    - docs/FORK-DELTA.md
    - .planning/phases/19-nullable-enum-tuple-mapping/19-BEHAVIOUR-LEDGER.md
key-decisions:
  - "normalise was made public in the Task 1 commit (the new signature touched the same file), not in Task 2 as the plan listed; the Task 2 commit therefore does not contain DeclaredNullability.java"
requirements-completed: [COLS-10, COLS-11]
coverage:
  - id: D1
    description: "An unexpected exception in the declared-nullability pass (type-less Column) returns false, leaves the root untouched and logs one WARN with the exception class name and no throwable"
    requirement: "COLS-10"
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest#unexpectedExceptionInThePassFailsOpenWithOneMetadataWarning"
        status: pass
    human_judgment: false
  - id: D2
    description: "Every fallback WARN (NoSuchType, structure mismatch, unmatched element, unexpected exception, discovery path) names db.t"
    requirement: "COLS-10"
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest (four fail-open cases) and ClickHouseHelperClientForkUnitTest#fallbackWarningNamesDatabaseAndTable"
        status: pass
    human_judgment: false
  - id: D3
    description: "The doubled-flag control fails with a read-past-end server code on both twins, not on any exception"
    requirement: "COLS-10"
    verification:
      - kind: integration
        ref: "NullableTupleMatrixLiveTest#controlWithBothFieldsNullableFailsToInsert (PLAIN: CANNOT_READ_ALL_DATA, DEFAULT: ATTEMPT_TO_READ_AFTER_EOF) on ClickHouse 26.9.17.8, client V2"
        status: pass
    human_judgment: false
  - id: D4
    description: "Normalisation and the Column-tree flattener each exist once; the anchored apply line keeps count 1; FORK-DELTA and ledger L-40 land in the fix commit"
    requirement: "COLS-11"
    verification:
      - kind: other
        ref: "grep -c 'DeclaredNullability.apply(' ClickHouseHelperClient.java = 1; git diff 5fd09cb1 HEAD over the protected paths is empty"
        status: pass
    human_judgment: false
duration: 9min
completed: 2026-10-13
status: complete
---

# Phase 19 Plan 08: Total declared-nullability pass and review test fixes Summary

**DeclaredNullability.apply now catches any RuntimeException and names the database and table in its single metadata-only fallback WARN; the matrix control only passes on a server read-past-end code (CANNOT_READ_ALL_DATA / ATTEMPT_TO_READ_AFTER_EOF), and tests share production normalisation and one nullability flattener.**

## Performance
- **Duration:** 9 min
- **Started:** 2026-10-13T10:06:28Z
- **Completed:** 2026-10-13T10:10Z
- **Tasks:** 2
- **Files modified:** 10 (1 created, 9 modified)

## Accomplishments
- WR-01: one `try` spans `ClickHouseColumn.of`, `collect` and the `setNullable` loop with one `catch (RuntimeException)`. A Column with no type (NullPointerException in `collect`) now falls back instead of escaping `describeTableV1`.
- IN-01: `buildDescribedTable` passes `database` and `tableName`; the WARN reads `Could not take element nullability for column {} of table {}.{} from its declared type {} ({}); ...`.
- WR-03: the control asserts a read-past-end code in the cause chain and logs only the code name.
- IN-03: `NullableRoundTrip.normaliseDeclaredType` delegates to `DeclaredNullability.normalise`; `ColumnNullabilityPaths.byPath` replaces the two private flatteners.

## Task Commits
1. **Task 1: total pass and table context (WR-01, IN-01)** - `88e4e854` (fix)
2. **Task 2: control code, shared normalisation and flattener (WR-03, IN-03)** - `448a5ba5` (refactor)

**Plan metadata:** see the docs(19-08) commit that follows this file.

## Seen red (before Task 1's fix)
The new and updated Docker-free tests were written with the four-argument `apply` first. Against the unchanged production code the test source set failed to compile (`method apply in class DeclaredNullability cannot be applied to given types`, 15+ sites; log `/tmp/gsd19/exec/gap-08-t1-red.log`). The red state is a compile failure from the signature change, so the behavioural assertions (single WARN naming `db.t`, the type-less Column case) could not run red separately.

## Control observed per twin (server 26.9.17.8, client V2)
- PLAIN twin: `CANNOT_READ_ALL_DATA`
- DEFAULT twin: `ATTEMPT_TO_READ_AFTER_EOF`

FORK-DELTA's COLS-10 parenthetical named only `ATTEMPT_TO_READ_AFTER_EOF`, so it changed to name both codes by twin (docs/FORK-DELTA.md, in the Task 2 commit). Nothing else in that row changed in Task 2.

## Review-driven edits to verified 19-04 work
These were made because of 19-REVIEW, not because of new scope. They edit work 19-04 delivered and the verifier passed; none re-claims 19-04's verified truths.

- **WR-03 (control).** Before: `assertThrows(Exception.class, ...)` followed by `assertNotNull(failure)`, so any exception (connection error, unknown table, typo) passed. After: only a cause chain holding `ATTEMPT_TO_READ_AFTER_EOF` or `CANNOT_READ_ALL_DATA` passes; the chain is printed on mismatch.
- **IN-03 (normalisation and flattener).** Before: `NullableRoundTrip` re-implemented the backslash-n replacement with its own constant, and the Column-tree flattener existed twice (`DeclaredNullabilityTest`, `NullableTupleMatrixLiveTest`). After: one delegation to the now-public `DeclaredNullability.normalise`, one `ColumnNullabilityPaths.byPath`. Every former call site keeps its expected map.
- **IN-01 / WR-01 (ledger L-40).** Before: `Could not take element nullability for column {} from its declared type {} ({}); keeping the nullability DESCRIBE reported for it`. After: `Could not take element nullability for column {} of table {}.{} from its declared type {} ({}); keeping the nullability DESCRIBE reported for it`, with the note that any RuntimeException in the pass falls back the same way.

Counts:

| Class | Before | After Task 1 | After Task 2 |
|-------|--------|--------------|--------------|
| NullableTupleMatrixLiveTest | 30 | 30 | 30 |
| DeclaredNullabilityTest | 17 (before Task 1) | 18 | 18 |
| ClickHouseHelperClientForkUnitTest | 44 | 45 | - |
| NestedNullableCompositeWriteTest | 18 | 18 | - |
| ClickHouseHelperClientForkLiveTest / SimpleAggregateTupleRoundTripLiveTest / NestedNullRowBinaryLiveTest | 12 / 2 / 10 | 12 / 2 / 10 | - |

No test was deleted or weakened; all failures, errors and skips are 0.

## Decisions Made
- `normalise` went public in the Task 1 commit because that commit already rewrote the file's `apply` and Javadoc; the behaviour is identical. The plan's Task 2 commit check expects `DeclaredNullability.java` in the Task 2 file set, which cannot hold now (see Deviations).

## Deviations from Plan

**1. [Verify-script discrepancy] ClickHouseHelperClient.java is IS DIRTY under the Spotless IDE hook**
- **Found during:** Task 1 spotless gate
- **Issue:** The hook reports the file dirty, but the HEAD version before this plan is also dirty. The diff is only upstream's import order (`lombok.Getter` and `org.slf4j` imports), the same upstream-identical condition recorded for `ClickHouseWriter.java` in 18-01.
- **Resolution:** Not reformatted, to keep the file upstream-identical. The one-line change adds no formatting debt. The other four Java files in Task 1 are IS CLEAN.
- **Files modified:** none beyond the intended one-line edit

**2. [Verify-script discrepancy] The by-name check in Task 1's automated script cannot match JUnit XML testcase names**
- **Found during:** Task 1 verification
- **Issue:** The XML `testcase name` attribute holds the `@DisplayName`, not the method name, so the name probes (`unexpectedExceptionInThePassFailsOpenWithOneMetadataWarning`, `fallbackWarningNamesDatabaseAndTable`, the three existing fail-open cases) never match.
- **Resolution:** Every count and zero-failure condition of the script passed (18 / 45 / 18 / 12 / 2 / 30 / 10, no failures, errors or skips). The cases were confirmed by display name in the XML (the new DeclaredNullabilityTest case and the IN-01 fork-unit case) and by method name in source.
- **Files modified:** none

**3. [Plan layout] `normalise` made public in Task 1, not Task 2**
- **Resolution:** See Decisions Made. The Task 2 commit contains the four Java files and `docs/FORK-DELTA.md`; the plan's file-set check for Task 2 listed `DeclaredNullability.java`, which already carried the change.

**Total deviations:** 3 (all verification or layout discrepancies, no code behaviour change). **Impact:** none on the prohibitions. No protected file changed (`git diff 5fd09cb1 HEAD` over Table, ClickHouseWriter, processing, state, kafka, ProxySinkTask, VERSION, CHANGELOG and the rollout doc is empty).

## Issues Encountered
None.

## Known Stubs
None.

## Threat Flags
None. The only logging change adds catalog metadata (database and table names); no exception message or record value is read.

## Next Phase Readiness
- 19-09 adds the edge pins and mutation checks; 19-10 records WR-01, IN-01, WR-03 and IN-03 as `fixed` with `88e4e854` and `448a5ba5`, runs the anchors invariant and the two full runs. The anchored `DeclaredNullability.apply(` line in ClickHouseHelperClient.java stays at count 1.

## Self-Check: PASSED
- `ColumnNullabilityPaths.java` exists; commits `88e4e854` and `448a5ba5` are ancestors of HEAD; `gsd-tools check evaluation-scope --plan 19-08 --commits-only` returns the plan unit.
- Task 1 live and unit suites: 18 / 45 / 18 / 12 / 2 / 30 / 10, 0 failures, errors, skips. Task 2: DeclaredNullabilityTest 18, matrix 30, both control codes logged, FORK-DELTA names both.
- Spotless hook: IS CLEAN on every Java file touched except ClickHouseHelperClient.java (pre-existing, see Deviations).
