---
phase: 04-json-insert-path-connector-plumbing
plan: 01
subsystem: testing
tags: [junit5, testcontainers, clickhouse-latest, client-v2, kafka-connect-json-insert, rowbinary]

# Dependency graph
requires:
  - phase: 03-table-metadata-json-dynamic-types
    provides: "Typed SQL read-back convention (allRows/GenericRecord aliasing helper), RowBinaryUnsupportedTypeGateTest Docker-free precedent, jacocoForkReport coverage of ClickHouseWriter/ClickHouseSinkConfig/ProxySinkTask/ClickHouseSinkTask"
provides:
  - "ExplicitJsonInsertPathLiveTest — ClickHouse-backed pins for JSON-01 (live differential + R-01 pin), JSON-02 (end-to-end metadata-free table resolution), JSON-03 (live half of cleanupExtraFields bypass), and JSON-04 (mixed batch + all-tombstone batch)"
  - "MetadataFreeJsonInsertUnitTest — Docker-free pins for JSON-01's letter-case matrix/negative half and JSON-03's cleanupExtraFields differential"
  - "JSON-05 typed column-value assertions added to both pre-existing explicit-JSON tests in ClickHouseSinkTaskJsonInsertFormatTest"
  - "R-01 silent-data-loss finding resolved: escalated to a human at a blocking-human checkpoint, answered pin-current-behavior, pinned by one new live test, no production fix made"
affects: [04-02-cols-plan, 04-03-jmx-async-plan]

# Actuals (#2632)
actuals:
  tokens: 7520
  tasks: 4
  commits: 4
plan_head_before: 40c05206399e16769534fad855d65a26ef58a014

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Schemaless-first live testing (D-06): every SinkRecord built as a Map value, never a Struct, because the schemaless route is the only one where metadata-free mode actually changes behavior."
    - "Docker-free ClickHouseWriter unit test driving the existing protected setSinkConfig seam on a bare new ClickHouseWriter() — extends nothing, so it stays in testUnit (D-16, precedent: RowBinaryUnsupportedTypeGateTest)."
    - "R-01 pin pattern: a live test documenting confirmed-broken behavior carries a loud class-adjacent Javadoc stating it is NOT an endorsement, naming the checkpoint that authorized it and the decision locking out an autonomous fix."

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java

key-decisions:
  - "R-01 escalation resolved via blocking-human checkpoint (Task 4): the user chose pin-current-behavior. One new live test (JSON-01: [R-01 pin]) now asserts, without endorsing, that insertFormat=json + a Struct record + bypassRowBinary left at its default (false) makes put() return without throwing while 0 of 5 records land. No fail-fast fix was made — per 04-CONTEXT.md D-01/D-07, that would be a main-code change outside this plan's files_modified and needs its own plan."
  - "JSON-04's all-tombstone batch (a real Debezium-delete-burst shape, previously untested) is confirmed live: an empty JSONEachRow body is accepted by ClickHouse as a clean zero-row insert, put() does not throw, and 0 rows land — matching the research session's inference (04-RESEARCH.md Pitfall 3 / Assumption A1), now empirically verified rather than assumed."
  - "JSON-01's negative half additionally pins the upstream props.getOrDefault(INSERT_FORMAT, \"node\") typo: an unset insertFormat still resolves getInsertFormat() to InsertFormats.JSON via the default: branch, while isJsonInsertFormatExplicit() (the real metadata-free gate) stays false. Confirmed present on upstream main; deliberately not fixed (D-01)."

requirements-completed: [JSON-01, JSON-02, JSON-03, JSON-04, JSON-05]

coverage:
  - id: D1
    description: "insertFormat=json in any letter case (with whitespace trimmed) enables the metadata-free gate; unset/none/csv/tsv leave it false; the upstream unset-defaults-to-JSON-enum typo is pinned by implication"
    requirement: "JSON-01"
    verification:
      - kind: unit
        ref: "MetadataFreeJsonInsertUnitTest#explicitJsonInsertFormatIsCaseInsensitive, #nonJsonInsertFormatsAreNotExplicit, #unsetInsertFormatIsNotExplicitDespiteResolvingToJsonEnum"
        status: pass
      - kind: integration
        ref: "ExplicitJsonInsertPathLiveTest#unsetInsertFormatStillThrowsOnAbsentTable (JSON-01 live differential)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Metadata-free mode resolves a remapped table (topic != table name) with no fetched table metadata, and every inserted column value lands correctly (typed read-back, not row count alone)"
    requirement: "JSON-02"
    verification:
      - kind: integration
        ref: "ExplicitJsonInsertPathLiveTest#metadataFreeModeResolvesRemappedTableAndLandsEveryColumnValue"
        status: pass
    human_judgment: false
  - id: D3
    description: "cleanupExtraFields passes a payload through unmodified (including an unknown key) under insertFormat=json, and strips unknown keys under default config, without mutating its input — Docker-free half via the setSinkConfig seam plus the live half proving known fields land correctly"
    requirement: "JSON-03"
    verification:
      - kind: unit
        ref: "MetadataFreeJsonInsertUnitTest#cleanupExtraFieldsBypassDiffersByInsertFormat"
        status: pass
      - kind: integration
        ref: "ExplicitJsonInsertPathLiveTest#metadataFreeModeInsertsUnknownFieldWithoutThrowing"
        status: pass
    human_judgment: false
  - id: D4
    description: "A mixed batch of schemaless records and a tombstone inserts exactly the non-null records with correct values; an all-tombstone batch is pinned by its ACTUAL observed outcome (confirmed: no throw, 0 rows, clean empty-JSONEachRow-body insert)"
    requirement: "JSON-04"
    verification:
      - kind: integration
        ref: "ExplicitJsonInsertPathLiveTest#mixedBatchWithTombstoneInsertsOnlyNonNullRecords, #allTombstoneBatchPinsObservedOutcome"
        status: pass
    human_judgment: false
  - id: D5
    description: "Both pre-existing explicit-JSON tests (String-record route) assert typed column values via read-back, in addition to their existing row-count assertion"
    requirement: "JSON-05"
    verification:
      - kind: integration
        ref: "ClickHouseSinkTaskJsonInsertFormatTest#explicitJsonInsertSkipsTableCheckOnStart, #explicitJsonInsertIgnoresTargetTableFilter"
        status: pass
    human_judgment: false
  - id: D6
    description: "R-01 silent data loss (insertFormat=json + Struct record + bypassRowBinary unset) is escalated to a human at a blocking-human checkpoint and the answer is pinned by a live test that is explicitly NOT an endorsement — no production fix made"
    verification:
      - kind: integration
        ref: "ExplicitJsonInsertPathLiveTest#explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch"
        status: pass
    human_judgment: false

duration: 2 agent sessions (Tasks 1-3 in the initial session; Task 4 + plan close in this continuation session, ~25 min)
completed: 2026-09-17
status: complete
---

# Phase 4 Plan 1: Explicit JSON Insert Path Summary

**Pinned the metadata-free `insertFormat=json` insert path (gate, table resolution, field cleanup, tombstone handling) with two new fork-only test classes, upgraded both pre-existing explicit-JSON tests to typed assertions, and resolved the R-01 silent-data-loss finding via a blocking-human checkpoint answered `pin-current-behavior`.**

## Performance

- **Duration:** Tasks 1-3 executed in an initial agent session; this continuation session executed Task 4 (the R-01 checkpoint resolution) and closed the plan, ~25 min.
- **Tasks:** 4 / 4 complete
- **Files modified:** 3 (2 created, 1 edited)

## Accomplishments

- `ExplicitJsonInsertPathLiveTest` (new, `com.clickhouse.kafka.connect.sink`, extends `ClickHouseBase`): 7 live tests covering JSON-01's live differential (the connector-side table-existence check metadata-free mode skips) and its R-01 pin, JSON-02's end-to-end metadata-free table resolution through a remapped table, JSON-03's live half (unknown-field payload insert without throwing), and JSON-04's mixed and all-tombstone batches.
- `MetadataFreeJsonInsertUnitTest` (new, `com.clickhouse.kafka.connect.sink.db`, Docker-free, extends nothing): pins JSON-01's letter-case matrix (`json`/`JSON`/`Json`/`  json  ` all explicit, `none`/`csv`/`tsv`/unset all not) including the upstream unset-defaults-to-JSON-enum typo, and JSON-03's `cleanupExtraFields` differential via the existing protected `setSinkConfig` seam.
- `ClickHouseSinkTaskJsonInsertFormatTest` (edited, the one D-16-named exception): both pre-existing explicit-JSON tests now assert typed column values via a copied `allRows` helper, in addition to their existing row-count assertion (JSON-05).
- R-01 (`insertFormat=json` + Struct record + `bypassRowBinary` unset silently drops all rows) escalated at the Task 4 blocking-human checkpoint; the user's answer was `pin-current-behavior`. One new live test now pins the confirmed outcome — `put()` returns without throwing, 0 of 5 records land — carrying a prominent Javadoc stating it is not an endorsement and naming the checkpoint and the D-01/D-07 decision that keeps a fail-fast fix out of this plan's scope.
- `git status --porcelain -- src/main/ build.gradle.kts` is empty for the whole plan; `ClickHouseWriter.java` blob hash is unchanged at `37c86dac435631117899d9e80cbb414d441ebc8a`.

## Task Commits

Each task was committed atomically:

1. **Task 1: End-to-end metadata-free slice (JSON-02 tracer)** - `c3a332c` (test)
2. **Task 2: JSON-01 differential, JSON-03 live half, JSON-04 tombstone batches** - `64f2253` (test)
3. **Task 3: Docker-free unit pins (JSON-01 case matrix, JSON-03 cleanup) + JSON-05 typed assertions** - `08c849a` (test)
4. **Task 4: R-01 escalation resolved — pin-current-behavior** - `9a41541` (test)

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java` - New ClickHouse-backed class; 7 live tests for JSON-01/02/03/04 plus the R-01 pin
- `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java` - New Docker-free class; JSON-01's case matrix and JSON-03's cleanup differential
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java` - Edited (the one D-16 exception); JSON-05 typed read-back added to both pre-existing tests

## Decisions Made

- **R-01 escalation resolved as `pin-current-behavior`** (Task 4 checkpoint). The finding: `insertFormat=json` (metadata-free mode) combined with a Struct/SCHEMA record and `bypassRowBinary` left at its default `false` routes to `doInsertRawBinaryV2` against a column-less synthetic `Table`; the per-column write loop iterates zero columns, so every record silently serializes to zero bytes. `put()` returns normally with no exception anywhere in the call chain; the client's own `InsertResponse` reports `Written Rows: [0]`; a follow-up `COUNT(*)` confirms 0 of 5 records landed. This was first observed in 04-RESEARCH.md's session (server `26.8.6.5`) and independently re-confirmed live in this session's test run. Per 04-CONTEXT.md D-01/D-07, a fail-fast throw for this combination is **not pre-approved** in this phase — it would break, loudly, any production pipeline currently running `insertFormat=json` with a Struct converter, which is losing data today but not failing today. The user's explicit direction was to pin the current behavior with a loudly-commented test rather than fix it or leave it unpinned; that is exactly what Task 4 did. No main-code change was made anywhere in this plan.
- **All-tombstone batch outcome recorded verbatim** (JSON-04, D-09): a batch of three tombstone `SinkRecord`s (all-null values) through `doInsertJsonV2` under `insertFormat=json` produces an empty JSONEachRow body, which ClickHouse `latest` (client V2) accepts as a clean, non-throwing, zero-row insert — `put()` does not throw, `countRows` is 0, and a typed read-back confirms zero rows. This matches the research session's source-level inference (empty JSONEachRow body is well-formed "zero rows") and is now empirically confirmed rather than assumed.
- JSON-01's negative half additionally pins the upstream `props.getOrDefault(INSERT_FORMAT, "node")` typo as a side effect of asserting the unset case: `getInsertFormat()` still resolves to `InsertFormats.JSON` via the `default:` switch branch even though `isJsonInsertFormatExplicit()` (the real metadata-free gate) stays `false`. Confirmed present on upstream `main`; pinned here by implication, deliberately not fixed (D-01).

## Deviations from Plan

None - plan executed exactly as written, including its one designed decision point (Task 4's blocking-human checkpoint), which resolved to the plan's own `pin-current-behavior` option.

## Issues Encountered

None. Docker (colima `vz` profile) was reachable throughout; all live and Docker-free verification commands passed on the first run in this continuation session.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- JSON-01..05 are fully pinned; the explicit JSON insert path's metadata-free behavior (gate, table resolution, field cleanup, tombstone handling) now has automated regression coverage against ClickHouse `latest` / client V2.
- R-01's silent-data-loss finding is recorded, escalated, and answered — it stays an accepted, documented risk (04-CONTEXT.md threat T-04-04) rather than a silent gap. Any future fail-fast fix for the Struct + RowBinary + metadata-free combination needs its own plan and its own user decision; nothing in this plan or its SUMMARY pre-approves one.
- No blockers for 04-02 (`jsonInsertColumns`) or 04-03 (JMX + `async_insert`) — both are independent test tracks per ROADMAP.md and share no files with this plan.

## Self-Check: PASSED

All created files (`ExplicitJsonInsertPathLiveTest.java`, `MetadataFreeJsonInsertUnitTest.java`, `ClickHouseSinkTaskJsonInsertFormatTest.java`, this SUMMARY.md) confirmed present on disk, and all four task commits (`c3a332c`, `64f2253`, `08c849a`, `9a41541`) confirmed present in `git log --oneline --all`.

---
*Phase: 04-json-insert-path-connector-plumbing*
*Completed: 2026-09-17*
