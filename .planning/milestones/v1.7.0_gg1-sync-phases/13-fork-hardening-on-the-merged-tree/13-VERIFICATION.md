---
phase: 13-fork-hardening-on-the-merged-tree
verified: 2026-10-08T18:20:00Z
status: passed
score: 5/5 must-haves verified
covered_files:
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-01-PLAN.md
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-01-SUMMARY.md
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-02-PLAN.md
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-02-SUMMARY.md
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-03-PLAN.md
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-03-SUMMARY.md
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-04-PLAN.md
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-04-SUMMARY.md
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-05-PLAN.md
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-05-SUMMARY.md
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-06-PLAN.md
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-06-SUMMARY.md
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-07-PLAN.md
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-07-SUMMARY.md
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-08-PLAN.md
  - .planning/phases/13-fork-hardening-on-the-merged-tree/13-08-SUMMARY.md
  - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
  - src/main/java/com/clickhouse/kafka/connect/sink/data/Data.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
covered_digest: "v3:sha256:e3d5761a9031477fb37984c7f5aa4151d448407d76158b941c4cc4d5e6103863"
behavior_unverified: 0
overrides_applied: 0
advisory:
  - finding: "WR-01: empty-CDC skip exists only on the insertFormat=json route; the bypassRowBinary=true JSON route still emits an all-defaults row for op=t / null-struct CDC records"
    category: other
    reason: "Upstream route, deliberately left byte-identical (D-05, ledger L-03). Outside every Phase 13 success criterion, which is scoped to insertFormat=json."
    evidence_status: "code read at ClickHouseWriter.java:1360-1400; no test demonstrates the phantom row"
  - finding: "WR-02: io.debezium.time.Timestamp millisecond longs are not converted on the metadata-free route and ClickHouse reads them as seconds"
    category: other
    reason: "Recorded as a known gap (ledger L-05, D-18). SC2 enumerates Kafka Timestamp/Date/Time/Decimal/bytes only. Silent wrong value on a common Debezium MySQL mapping, so it should reach FORK-DELTA and the rollout checklist, which the ledger already routes."
    evidence_status: "MetadataFreeJsonWriter.writeLogicalValue has no case for it; pass-through is pinned by MetadataFreeJsonWriterTest"
  - finding: "WR-05: Column.extractColumn drops Enum/Tuple nullability, so nested-null guards throw on legal Nullable(Enum)/Nullable(Tuple) slots"
    category: other
    reason: "Pre-existing defect, ledger L-11/L-13 and backlog 999.1. Before the phase these slots were mis-encoded (shifted or NULLed), so the change is silent corruption becoming a loud DataException, which matches the goal text 'round-trips as NULL or fails loudly'."
    evidence_status: "ledger L-13; ROADMAP.md backlog 999.1 present"
---

# Phase 13: Fork Hardening on the Merged Tree Verification Report

**Phase Goal:** The fork's open correctness follow-ups are closed on top of the merged writer. R-01's silent data loss is gone (`Struct` records in metadata-free `insertFormat=json` mode land through the JSON writer with correct typed values). A null in a Map/Array/Tuple slot either round-trips as `NULL` or fails loudly instead of desyncing RowBinary. The metadata, config and test-debt findings from earlier review are fixed and pinned.
**Verified:** 2026-10-08T18:20:00Z
**Status:** passed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `insertFormat=json` + `Struct` + default `bypassRowBinary`: every record lands; routing keys on `shouldSkipTableMetadata()`; R-01 pin asserts typed read-back | VERIFIED | `ClickHouseWriter.doInsertBatch` (line 238) routes `SCHEMA`/`DEBEZIUM_CDC` to `doInsertJson` on `csc.isBypassRowBinary() \|\| shouldSkipTableMetadata()`. `shouldSkipTableMetadata()` is `csc != null && csc.isJsonInsertFormatExplicit()`, not the raw format string. `writeRecordsJson` sends `SCHEMA` records to `MetadataFreeJsonWriter.structRow`. `ExplicitJsonInsertPathLiveTest#explicitJsonStructRecordWithDefaultBypassRowBinaryLandsEveryRecord` leaves `bypassRowBinary` unset, puts 5 Struct records (incl. non-ASCII), and asserts 5 rows with `id` and `hex(str)` per row. Re-run today: 7/7 pass. |
| 2 | Typed read-back matrix for Timestamp/Date/Time/Decimal/bytes into DateTime/DateTime64/Date/Date32/Decimal/String; Jackson defaults fixed; null -> NULL/DEFAULT; `jsonInsertColumns` and `bypassFieldCleanup` pinned | VERIFIED | `MetadataFreeJsonWriter.writeLogicalValue` encodes Timestamp as decimal seconds, Date as epoch days, Time as ms, raw bytes via `writeUTF8String` (not base64). A private `ObjectMapper.copy()` keeps the shared mapper untouched. `MetadataFreeJsonTypeParityLiveTest` (8 tests) compares each cell against a RowBinary twin table with literal anchors, and covers negative epochs, CDC parity, `nullFieldsStoreNullInNullableColumnsAndDefaultsElsewhere`, `jsonInsertColumnsLeavesUnlistedColumnsAtDefault...`, `bypassFieldCleanupDoesNotChange...`. Byte-level pins in `MetadataFreeJsonWriterTest` (33). 8/8 and 33/33 pass in the full run; writer-level classes re-run today. |
| 3 | `null` in `Map(K, Nullable(V))` reads back NULL; null in nested non-nullable slot throws `DataException` naming the column with no row; top-level null Map/Array keeps coercion | VERIFIED | `doWriteColValue` MAP/ARRAY/TUPLE loops (lines ~683-767) throw `DataException(buildNestedNullMessage(col, slot))`, built only from `Column` metadata. `NestedNullRowBinaryLiveTest` (10): 12 Map value types and 4 Array types read back SQL NULL with neighbour assertions; Array element, Map value and Tuple field null each assert a `DataException` in the cause chain, the column name, no record data in the message, and no row; DLQ whole-batch pin; `topLevelNullArrayAndMapStillReadBackEmpty` asserts `[]` and `{}`. 10/10 re-run today. The nested null Array/Map coerce-to-empty narrowing (D-09) is recorded in REQUIREMENTS.md. |
| 4 | Pins: DESCRIBE row 2+ levels below JSON/Dynamic skipped; Int256/UInt256 fail fast (Int128/UInt128 still write); `extractColumn` Array arg order; ConfigDef USERNAME default; `cleanupExtraFields` evaluates predicate before `csc` | VERIFIED | DESC-05: `shouldSkipDescribeRow` now walks every ancestor prefix (`lastIndexOf('.', dot - 1)`); `ClickHouseHelperClientForkUnitTest` 29/29 and `ForkLiveTest` 11/11. TYPE-08: `case "INT256": case "UINT256":` joins the DYNAMIC/UNKNOWN fail-fast block in `validateDataSchema` (line 332); `RowBinaryUnsupportedTypeGateTest` 11/11 and `WideIntegerTypeGateLiveTest` 3/3 (re-run today) incl. Int128/UInt128 INT64-extreme round trip. COLS-07: diff shows `false, hasDefaultValue, isSubColumn, arrayDepth + 1`; `ColumnArrayRecursionTest` 9/9. CFG-04: diff `passwordDefault` -> `usernameDefault`; `ClickHouseSinkConfigUsernameDefaultUnitTest` 3/3. JSONP-08: `shouldSkipTableMetadata() \|\| csc.isBypassFieldCleanup()` (line 1521); `MetadataFreeJsonInsertUnitTest` Mockito `never()`/`times(1)` order pin, 23/23 re-run today. |
| 5 | Test debt closed: HeaderToValue non-STRING pin; JsonColumnTest value assertions; try/finally in both JsonInsertFormat tests; tearDown drop failure attributed; stale Javadocs corrected | VERIFIED | `HeaderToValueTest` 29/29 (DEBT-01 schema and schemaless pins). `JsonColumnTest.assertPayloadValues` reads back 10000 rows and asserts `nested_str`, `nested_int`, `nested_bytes` (7 run, 1 pre-existing `@Disabled`). `ClickHouseSinkTaskJsonInsertFormatTest` diff wraps both tests in `try { } finally { stop(); dropTable(); }`. `ClickHouseBase.dropSharedDatabaseOrAttribute` throws `IllegalStateException` naming class and database; `ClickHouseBaseTearDownAttributionUnitTest` 2/2. Javadoc diffs read for `ClickHouseVersionDefaultUnitTest`, `AsyncInsertLiveTest`, `NetworkCompressionPinUnit/LiveTest`; the version Javadoc claim ("workflow now sets no version") is consistent with `.github/workflows` (no `CLICKHOUSE_VERSION: latest` in the test job). |

**Score:** 5/5 truths verified (0 present-but-behavior-unverified). Every behavior-dependent truth (routing, nested-null failure with no row, null semantics, teardown attribution) is backed by a passing behavioral test.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `sink/db/MetadataFreeJsonWriter.java` | Schema-aware row writer | VERIFIED | 255 lines, substantive, stateless, used from `writeRecordsJson` (structRow, cdcRow) |
| `sink/data/Data.java` | `getSchema()` accessor | VERIFIED | additive accessor used by `CdcRowSerializer` |
| `sink/db/ClickHouseWriter.java` | Routing, nested-null checks, Int256 gate, predicate order | VERIFIED | all four changes present and wired |
| `sink/db/helper/ClickHouseHelperClient.java` | Ancestor-walk skip | VERIFIED | diff read |
| `sink/db/mapping/Column.java` | Arg-order fix | VERIFIED | one-site diff |
| `sink/ClickHouseSinkConfig.java` | USERNAME default | VERIFIED | one-line diff |
| `testFixtures/.../RowSnapshots.java` and the 7 new Phase 13 test classes | Typed read-back helper and pins | VERIFIED | present; each reported tests > 0 with 0 failures in the XML results |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `doInsertBatch` | `doInsertJson` | `isBypassRowBinary() \|\| shouldSkipTableMetadata()` | WIRED |
| `writeRecordsJson` | `MetadataFreeJsonWriter.structRow` / `cdcRow` | `metadataFree` branch | WIRED |
| `doWriteColValue` ARRAY/MAP/TUPLE | `buildNestedNullMessage` -> `DataException` | in-loop checks | WIRED |
| `validateDataSchema` | `buildRowBinaryUnsupportedTypeMessage` | INT256/UINT256 case labels | WIRED |
| `ClickHouseBase.tearDown` | `dropSharedDatabaseOrAttribute` | line 146 | WIRED |

### Behavioral Spot-Checks and Test Evidence

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Routing, writer encodings, R-01 live pin, nested-null live, wide-int gate | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun --tests` on 5 classes (verifier re-run, ClickHouse latest, client V2) | 33 + 23 + 7 + 10 + 3 tests, 0 failures, 0 errors, 0 skipped | PASS |
| Full suite | JUnit XML in `build/test-results/test` summed by verifier | 803 tests, 18 skipped, 0 failures, 0 errors; XML timestamps (18:00Z) post-date the last `src` commit (`dcd2c384`, 17:54Z) | PASS |

### Data-Flow Trace (Level 4)

The JSON writer's bytes flow to a real ClickHouse insert (`writeJsonRow` -> HTTP stream), and the live tests read the rows back through typed `SELECT`s from a real server. No static/hollow data sources found.

### Probe Execution

No probes declared by the phase. SKIPPED.

### Requirements Coverage

All 15 phase IDs appear in the PLAN `requirements:` frontmatter (13-08 lists all 15) and in REQUIREMENTS.md; every one is marked `[x]` and `Complete` in the traceability table. No ORPHANED IDs.

| Requirement | Source Plan | Status | Evidence |
|-------------|-------------|--------|----------|
| JSONP-05 | 13-01 | SATISFIED | Truth 1 |
| JSONP-06 | 13-01, 13-02 | SATISFIED | Truth 2 |
| JSONP-07 | 13-02 | SATISFIED | Truth 2 (null semantics, jsonInsertColumns, bypassFieldCleanup tests) |
| JSONP-08 | 13-04 | SATISFIED | Truth 4 (operand swap with Mockito order pin; null csc still NPEs, documented, per D-13 correction) |
| WRT-07 | 13-03 | SATISFIED | Truth 3 |
| WRT-08 | 13-03 | SATISFIED (narrowed) | Truth 3; D-09 narrowing recorded in REQUIREMENTS.md |
| DESC-05 | 13-05 | SATISFIED | Truth 4 |
| TYPE-08 | 13-04 | SATISFIED (narrowed) | Truth 4; gate fires for non-nullable, no-default root columns, Nullable/defaulted reach the same message from `doWriteColValue` (ledger L-15) |
| COLS-07 | 13-05 | SATISFIED | Truth 4 |
| CFG-04 | 13-05 | SATISFIED | Truth 4 |
| DEBT-01 | 13-06 | SATISFIED | Truth 5 |
| DEBT-02 | 13-06 | SATISFIED | Truth 5 |
| DEBT-03 | 13-06 | SATISFIED | Truth 5 |
| DEBT-04 | 13-07 | SATISFIED | Truth 5 |
| DEBT-05 | 13-07 | SATISFIED | Truth 5 |

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| Phase 13 changed files | `TBD`/`FIXME`/`XXX` debt markers | none found in the files read | no debt-marker gate hit |
| `ClickHouseWriter.java:1094` | message-less `RuntimeException` (review WR-03) | Warning | Pre-existing; not a Phase 13 must-have |
| `ClickHouseWriter.java:1177` | `e.getMessage().contains` before null guard (review WR-04) | Warning | Pre-existing, outside the diff |

### Judgement on the Code-Review Findings (all `open` in the disposition)

- **WR-01 (bypass route lacks empty-CDC skip):** does not undermine any must-have. The criteria are scoped to `insertFormat=json`, and the upstream route is intentionally left byte-identical (D-05, L-03). Advisory.
- **WR-02 (`io.debezium.time.Timestamp` unconverted):** not a must-have. SC2 lists the Kafka logical types, and the Debezium conversion scope was set by D-18. It is a silent wrong value on a common CDC mapping, so it is a real production hazard. It is documented in the ledger and routed to FORK-DELTA and ROLL-01. Advisory, not a gap.
- **WR-05 (Enum/Tuple nullability):** the ledger shows these slots were already mis-encoded before (shifted or NULLed values); the phase turns that into a loud `DataException`, which satisfies the goal wording "round-trips as NULL or fails loudly". Logged as backlog 999.1. Advisory.
- WR-03, WR-04 and IN-01..04 are code-quality items outside the stated requirements.

### Human Verification Required

None. No truth is left PRESENT_BEHAVIOR_UNVERIFIED.

### Gaps Summary

No gaps. Every success criterion has code evidence in the tree and a passing behavioral test run against a real ClickHouse (latest, client V2), including a fresh re-run of the five central classes during this verification. The three advisories above are known, recorded and routed to Phase 15 documentation. The fork operator should read them (WR-02 especially) before rollout.

---

_Verified: 2026-10-08T18:20:00Z_
_Verifier: Claude (gsd-verifier)_
