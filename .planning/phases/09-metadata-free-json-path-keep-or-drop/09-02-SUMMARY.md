---
phase: 09-metadata-free-json-path-keep-or-drop
plan: 02
subsystem: testing
tags: [clickhouse, kafka-connect, insertFormat, json, jsonInsertColumns, table-metadata, testcontainers]

# Dependency graph
requires:
  - phase: 09-01
    provides: Re-applied metadata-free insertFormat=json bypass (four guards + shared predicate), jsonInsertColumns re-attached server-side, MetadataFreeJsonInsertUnitTest un-parked
provides:
  - All 13 live JSON-path test methods (ExplicitJsonInsertPathLiveTest, JsonInsertColumnsLiveTest, ClickHouseSinkTaskJsonInsertFormatTest) proven live against real ClickHouse, first execution since the 581-commit merge
  - TableTest.extractNullables() carry-in resolved (D-14) — the phase's one inherited red is gone
  - A clean zero-failure, zero-error full-suite run recorded as the phase's acceptance evidence
affects: [09-03 (MERGE-CHECKLIST.md/REQUIREMENTS.md bookkeeping for the KEEP disposition), 10 (harness/MBean-sweep work now meets 13 more live tests than Phase 8's baseline)]

# Actuals (#2632)
actuals:
  tokens: 1141
  tasks: 3
  commits: 2

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Delete-only re-enablement: removing a class-level @Disabled annotation plus its now-unused import is itself the proof that a test class's pinned contract did not move across a large merge"

key-files:
  created: []
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/TableTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java

key-decisions:
  - "TableTest.extractNullables()'s getAllColumnsList().size() assertion changed from 3 to 2, citing DESC-01/HLP-02, per D-14 — the sibling getRootColumnsList() assertion was already correct at 2 and stayed untouched."
  - "All three live test classes re-enabled by deleting only the @Disabled annotation and its now-unused import (D-10) — no other line touched, including ExplicitJsonInsertPathLiveTest's R-01 'PIN - NOT AN ENDORSEMENT' Javadoc, which stays byte-identical."
  - "No newly-enabled method required investigation: all 13 previously-parked methods passed on first execution against ClickHouse latest x client V2, with zero fix attempts needed."

patterns-established: []

requirements-completed: [JSONP-02, JSONP-03]

coverage:
  - id: D1
    description: "All 13 live test methods parked since the merge run and pass: ExplicitJsonInsertPathLiveTest (6), JsonInsertColumnsLiveTest (5), ClickHouseSinkTaskJsonInsertFormatTest (2)"
    requirement: JSONP-02
    verification:
      - kind: integration
        ref: "build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.ExplicitJsonInsertPathLiveTest.xml (6 tests, 0 failures, 0 errors, 0 skipped)"
        status: pass
      - kind: integration
        ref: "build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.JsonInsertColumnsLiveTest.xml (5 tests, 0 failures, 0 errors, 0 skipped)"
        status: pass
      - kind: integration
        ref: "build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskJsonInsertFormatTest.xml (2 tests, 0 failures, 0 errors, 0 skipped)"
        status: pass
    human_judgment: false
  - id: D2
    description: "R-01's labelled pin explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch passes, meaning the silent loss still reproduces exactly as documented under KEEP"
    requirement: JSONP-02
    verification:
      - kind: integration
        ref: "build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.ExplicitJsonInsertPathLiveTest.xml#explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch"
        status: pass
    human_judgment: false
  - id: D3
    description: "jsonInsertColumns re-attachment (from plan 09-01) proven live: COLS-02 (empty list still routes through the unchanged four-argument overload) and COLS-06 (column list applies on the non-explicit path too) both pass"
    requirement: JSONP-03
    verification:
      - kind: integration
        ref: "build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.JsonInsertColumnsLiveTest.xml"
        status: pass
    human_judgment: false
  - id: D4
    description: "TableTest.extractNullables() carry-in adopted so the full-suite gate is a clean binary, not a re-baselined allowance"
    verification:
      - kind: integration
        ref: "build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.db.mapping.TableTest.xml (7 tests, 0 failures)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Full env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test run completes with zero failures and zero errors across the whole suite, strictly fewer skips than Phase 8's 35"
    verification:
      - kind: other
        ref: "/tmp/gsd-09-02-fullsuite.log; 643 tests, 0 failures, 0 errors, 18 skipped (Phase 8 baseline was 35 skipped)"
        status: pass
    human_judgment: false

duration: ~20min
completed: 2026-09-20
status: complete
---

# Phase 9 Plan 2: Live Proof of the Metadata-free JSON Path Summary

**Re-enabled all 13 parked live JSON-path test methods (unchanged), adopted the TableTest.extractNullables() carry-in fix, and ran the full suite against real ClickHouse latex x client V2 clean: 643 tests, 0 failures, 0 errors, 18 skips (down from Phase 8's 35).**

## Performance

- **Duration:** ~20 min (full-suite run itself took 3m21s)
- **Started:** 2026-09-20 (this session)
- **Completed:** 2026-09-20T22:46:33Z (approx.)
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments

- Changed `TableTest.extractNullables()`'s stale `getAllColumnsList().size()` assertion from 3 to 2, citing `DESC-01`/`HLP-02`, closing the one inherited red carried since Phase 8
- Deleted the `@Disabled` annotation and its now-unused import from `ExplicitJsonInsertPathLiveTest`, `JsonInsertColumnsLiveTest`, and `ClickHouseSinkTaskJsonInsertFormatTest` — no other line touched
- Ran the full suite against a real ClickHouse container: 643 tests, 0 failures, 0 errors, 18 skips — a clean binary gate, not a re-baselined allowance
- All 13 newly-enabled live methods passed on first execution, including R-01's labelled pin and the guard-scope negative test `unsetInsertFormatStillThrowsOnAbsentTable`
- No collateral damage found in the non-JSON portions of the suite from plan 09-01's guards on `start()`/`updateMapping()`/`getTable()`/`cleanupExtraFields()`

## Task Commits

Each task was committed atomically:

1. **Task 1: Adopt the TableTest.extractNullables() carry-in** — `b84b179e` (test)
2. **Task 2: Un-park the three live JSON-path classes** — `ce7c40ce` (test)
3. **Task 3: The live gate — full suite, zero failures** — no commit (edits no source; result recorded below)

## Full-Suite Results

**Aggregate:** `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` — 54 live test classes, **643 tests, 0 failures, 0 errors, 18 skipped**. Log: `/tmp/gsd-09-02-fullsuite.log`. Runtime: 3m21s.

**Per-class results for the five classes this phase and 09-01 affected:**

| Class | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| `ExplicitJsonInsertPathLiveTest` | 6 | 0 | 0 | 0 |
| `JsonInsertColumnsLiveTest` | 5 | 0 | 0 | 0 |
| `ClickHouseSinkTaskJsonInsertFormatTest` | 2 | 0 | 0 | 0 |
| `MetadataFreeJsonInsertUnitTest` | 11 | 0 | 0 | 0 |
| `TableTest` | 7 | 0 | 0 | 0 |

The R-01 pin `explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch` and the guard-scope negative test `unsetInsertFormatStillThrowsOnAbsentTable` both executed and passed, confirmed by name in `ExplicitJsonInsertPathLiveTest`'s result XML.

**Arithmetic cross-check against the wave-1 post-merge baseline** (643 tests, 1 failure, 31 skipped, per this dispatch's `<prior_wave_state>`): Task 1's fix removed the one failure (1 → 0); Task 2's un-parking removed exactly the 13 newly-live methods from the skip count (31 → 18). Both deltas match this plan's two commits precisely, with no unexplained movement elsewhere in the suite.

### All 18 remaining skips, explained

None of the 18 are new; none belong to the four JSON-path classes (all run with zero skips, confirmed in the per-class table above).

| Class | Method(s) | Count | Reason |
|---|---|---|---|
| `ClickHouseSinkTaskTest` | `clientNameTest()` | 1 | Bare `@Disabled` (pre-existing, no message) |
| `ClickHouseSinkTaskWithSchemaTest` | `schemaWithTupleOfMapsWithVariantTest()`, `schemaWithNestedTupleMapArrayAndVariant()` | 2 | `@Disabled("Disabled because it requires a flag on the instance.")`, additionally gated by `@SinceClickHouseVersion("24.1")` |
| `ClickHouseSinkTaskWithSchemaTest` | `exactlyOnceStateMismatchTest` parameterized cases `[1]`..`[11]` | 11 | `Assumptions.assumeTrue(isCluster \|\| isCloud)` — this run is standalone (single-node) ClickHouse, so these cluster/cloud-only cases legitimately abort |
| `InsertFormatPerformanceTest` | `compareInsertPerformance()` | 1 | `@Disabled("Disabled because it not suitable for unit tests")` — deliberately non-unit performance test |
| `JsonColumnTest` | `compareAllConfigurations()` | 1 | `@Disabled("Disabled because it not suitable for unit tests")`, additionally gated by `@SinceClickHouseVersion("25.3")` |
| `ConvertToMapOrJsonPerformanceTestJson` | `testWideStructPerformance()`, `testDeepNestedStructPerformance()` | 2 | `@Disabled("Disabled because it not suitable for unit tests")` — deliberately non-unit performance tests |

Total: 1 + 2 + 11 + 1 + 1 + 2 = 18, matching the aggregate. All are environment gating (cluster/cloud-only), deliberately-disabled performance tests, or version/flag-gated features — none are silently-stopped-running regressions.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/TableTest.java` — `extractNullables()`'s all-columns assertion `3` → `2`, comment citing `DESC-01`/`HLP-02`
- `src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java` — deleted `@Disabled` line and its now-unused import
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java` — deleted `@Disabled` line and its now-unused import
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java` — deleted `@Disabled` line and its now-unused import

## Decisions Made

- **TableTest fix landed first, per RESEARCH Pitfall 4.** `TableTest` extends `ClickHouseBase` (`@Tag("clickhouse")`), so this assertion never runs under `testUnit` — it only surfaces in a full `./gradlew test` run. Landing it before the un-parking meant it was caught and fixed independently, not discovered inside the same run meant to prove Task 2's work.
- **No investigation needed for any of the 13 newly-enabled methods.** All passed clean on the first live run since the 581-commit merge — no fix attempts, no deviations, no Rule 1-3 action taken on any JSON-path test.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 09-03 can now record: the KEEP disposition's live proof is complete, `MERGE-CHECKLIST.md` row 26 and Recorded Fact 13 are ready for their Phase 9 annotations, and `REQUIREMENTS.md`'s JSONP-03 correction blockquote can cite this plan's live COLS-02/COLS-06 evidence.
- Phase 10's MBean leak sweep and shared-container work now meets 13 more live tests than Phase 8's baseline did (643 tests total, same as Phase 8, but 13 fewer skips and 1 fewer failure).
- No blockers for 09-03 from this plan's work.

## Self-Check: PASSED

- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/TableTest.java` — FOUND
- `src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java` — FOUND
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java` — FOUND
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java` — FOUND
- Commit `b84b179e` — FOUND in `git log --oneline --all`
- Commit `ce7c40ce` — FOUND in `git log --oneline --all`
- `/tmp/gsd-09-02-fullsuite.log` — FOUND, non-empty, 643 tests / 0 failures / 0 errors / 18 skipped
- Per-class XML results for `ExplicitJsonInsertPathLiveTest`, `JsonInsertColumnsLiveTest`, `ClickHouseSinkTaskJsonInsertFormatTest`, `MetadataFreeJsonInsertUnitTest`, `TableTest` — all re-confirmed zero failures, zero errors, zero skipped

---
*Phase: 09-metadata-free-json-path-keep-or-drop*
*Completed: 2026-09-20*
