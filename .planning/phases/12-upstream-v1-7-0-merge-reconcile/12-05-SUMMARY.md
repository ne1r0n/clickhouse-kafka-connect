---
phase: 12-upstream-v1-7-0-merge-reconcile
plan: "05"
subsystem: testing
tags: [full-suite-gate, sync-09, sync-10, fork-delta, merge-checklist, recorded-facts, invariant-script, clickhouse-26.9]

requires:
  - phase: 12-04
    provides: "KeeperStateProviderRestartLiveTest and the EOS-01 mutation evidence"
  - phase: 12-03
    provides: "D-17 retirement, CMP-03 live results, D-12 probe table and drafted sentence"
  - phase: 12-02
    provides: "Scoped MAP guard, single LZ4 rule, checklist with Recorded Facts 17-21"
  - phase: 12-01
    provides: "Merge commit M, verify-merge-invariants.sh, fork-anchors.txt"
provides:
  - "Full-suite gate on ClickHouse latest x client V2: 61 classes, 691 tests, 18 skipped, 0 failures, 0 errors"
  - "SYNC-09 pin matrix (29 classes) and SYNC-10 byte identity proven from the XML and git"
  - "docs/FORK-DELTA.md updated for the v1.7.0 merge: D-17 and the fork LZ4 rule retired, CR-02 row, compile-only integrationTest skip sentence"
  - ".planning/MERGE-CHECKLIST-v1.7.0.md with every Phase 12 row done and Recorded Facts 22-30"
  - "End-of-phase invariant gate (D-09): script PASS, self-test ST1-ST4 PASS"
affects: [phase-13, phase-14, phase-15]

plan_head_before: ffcd6566d29bb23d429b96c694364671ecfa572e
plan_head_after: ea88d289d7d867bd28949cb6095c3d8b2fd0baf0

actuals:
  tokens: 7283
  tasks: 2
  commits: 1

tech-stack:
  added: []
  patterns:
    - "Recorded Facts transcribe run output scrubbed of credentials; each fact states its source (repo-derived or observed run)"
    - "Checklist rows closed only with the closing commit hash in the evidence column"

key-files:
  created: []
  modified:
    - docs/FORK-DELTA.md
    - .planning/MERGE-CHECKLIST-v1.7.0.md

key-decisions:
  - "The D-17 load-bearing-control sentence is derived only from Recorded Fact 26's probe rows, including the V1 'not observed' clause"
  - "The checklist rows owned by phases 13 (MAP #820 nested slots, cleanupExtraFields) and 14 (lz4Java) stay pending"
  - "FORK-DELTA's introduction and 'The raw numbers' are left for Phase 13/15 (PUB-02) as the plan directs"

requirements-completed: [SYNC-09, SYNC-10, SYNC-08, SYNC-07, BLD-09, CMP-03]

coverage:
  - id: D1
    description: "Full suite on latest (26.9.12.8) x V2 is green with exactly FORK-DELTA's 18 named skips"
    requirement: SYNC-09
    verification:
      - kind: integration
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun (BUILD SUCCESSFUL; 61 classes, 691 tests, 18 skipped, 0 failures, 0 errors; distinct_skipped=18)"
        status: pass
    human_judgment: false
  - id: D2
    description: "All 29 SYNC-09 pin classes report tests > 0, failures 0, errors 0 (no vacuous pass)"
    requirement: SYNC-09
    verification:
      - kind: integration
        ref: "build/test-results/test/TEST-*.xml for the 29 pin classes (SYNC09-OK)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The 11 unmodified upstream test-tree paths are byte-identical to refs/tags/v1.7.0, upstream's 9 new methods pass, and the integrationTest source set compiles"
    requirement: SYNC-10
    verification:
      - kind: other
        ref: "git diff --quiet refs/tags/v1.7.0 HEAD -- <11 paths> (SYNC10-IDENTICAL); ./gradlew compileIntegrationTestJava -q; connect-api:4.1.1 on compileClasspath"
        status: pass
    human_judgment: true
    rationale: "The #800 Avro-union and cluster buildFromClause tests are proven by byte identity and compilation only. Whether that satisfies SYNC-10's 'skip for a named reason' (12-RESEARCH Open Question 3) is the user's call."
  - id: D4
    description: "FORK-DELTA says what is true after the merge: D-17 and the fork LZ4 rule retired, CR-02 MAP-guard row, dependency-pin row, compile-only skip sentence, next-syncer pointers"
    requirement: CMP-03
    verification:
      - kind: other
        ref: "FORK-DELTA grep gate (FORKDELTA-OK): zero 1.10.2, setCompressionMethod in the Retired table, no D-17 of Phase 11 in Connector source, compileIntegrationTestJava, MERGE-CHECKLIST-v1.7.0.md pointer, Nullable columns"
        status: pass
    human_judgment: true
    rationale: "The load-bearing-control sentence must say only what Recorded Fact 26's D-12 rows observed (T-12-15); a human reads the new D-17 row and confirms."
  - id: D5
    description: "Checklist has no pending row owned by phase 12, Recorded Facts continue 17-30 with no gap, and the end-of-phase invariant gate passes"
    requirement: SYNC-08
    verification:
      - kind: other
        ref: "CHECKLIST-DONE gate; bash .planning/scripts/verify-merge-invariants.sh <M> HEAD (result=PASS); verify-merge-invariants-selftest.sh (PASS ST1..ST4)"
        status: pass
    human_judgment: false

duration: 5min
completed: 2026-10-07
status: complete
---

# Phase 12 Plan 05: Full-suite gate, FORK-DELTA and checklist close-out Summary

**Full suite green on ClickHouse 26.9.12.8 x client V2 (691 tests, exactly the 18 named skips), every SYNC-09 pin class non-empty and green, upstream's tests byte-identical, FORK-DELTA rewritten for the D-17 retirement, and Phase 12's checklist rows closed with Recorded Facts 22-30 behind a passing end-of-phase invariant gate.**

## Performance

- **Duration:** 5 min (the Gradle full suite itself took 2m 40s)
- **Started:** 2026-10-07T19:57:11Z
- **Completed:** 2026-10-07T20:02:33Z (docs commit; this file follows)
- **Tasks:** 2 (Task 1 commits nothing by design; Task 2 is one docs commit)
- **Files modified:** 2

## Accomplishments

- Task 1: the full gate ran once at HEAD `ffcd6566` with `CLIENT_VERSION` and `CLICKHOUSE_VERSION` unset, so the code defaults (V2, `latest`) applied.
- Task 2: one docs commit, `ea88d289`, touching exactly `docs/FORK-DELTA.md` and `.planning/MERGE-CHECKLIST-v1.7.0.md`.
- End-of-phase gate (D-09): invariant script PASS, self-test PASS ST1-ST4, `custom_build` and `origin/custom_build` unmoved.

## Full-suite gate (Task 1)

Command: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun` (background, exit 0, `BUILD SUCCESSFUL in 2m 40s`). Tests ran under `file.encoding=windows-1252` (`tasks.withType<Test>`), with the build's own `maxParallelForks` (`availableProcessors() / 2`; 12 CPUs here) and one shared container per fork.

| classes | tests | skipped | failures | errors |
|---------|-------|---------|----------|--------|
| 61 | 691 | 18 | 0 | 0 |

Server `26.9.12.8` (`com.clickhouse.build.version` label of `clickhouse/clickhouse-server:latest`), image Created `2026-10-06T18:12:57Z`, id `sha256:5d6b622bbb10...`. The image date was checked before the run and is the same as at 12-03, so no `latest` drift entered this gate. Client `0.9.6`. `spotlessCheck` is red repo-wide and was not used as a gate.

### The 18 skipped testcases (distinct identities from the XML)

Equal to FORK-DELTA's Named skip inventory, none missing, none extra:

| Class | Testcase |
|-------|----------|
| `ClickHouseSinkTaskTest` | `clientNameTest()` |
| `ClickHouseSinkTaskWithSchemaTest` | `schemaWithNestedTupleMapArrayAndVariant()` |
| `ClickHouseSinkTaskWithSchemaTest` | `schemaWithTupleOfMapsWithVariantTest()` |
| `ClickHouseSinkTaskWithSchemaTest` | `exactlyOnceStateMismatchTest` cases `[1]` to `[11]` (11 testcases; XML display names `[1] 11, 7` through `[11] 229, 221`) |
| `InsertFormatPerformanceTest` | `compareInsertPerformance()` |
| `JsonColumnTest` | `compareAllConfigurations()` |
| `transforms.ConvertToMapOrJsonPerformanceTestJson` | `testWideStructPerformance()` |
| `transforms.ConvertToMapOrJsonPerformanceTestJson` | `testDeepNestedStructPerformance()` |

### SYNC-09 pin matrix (tests / skipped / failures / errors, from each XML)

Docker-free: `RowBinaryUnsupportedTypeGateTest` 6/0/0/0, `JsonDynamicTypeMappingTest` 5/0/0/0, `MBeanNameUnitTest` 11/0/0/0, `ClickHouseHelperClientForkUnitTest` 19/0/0/0, `MetadataFreeJsonInsertUnitTest` 13/0/0/0, `JsonInsertColumnsUnitTest` 9/0/0/0, `AsyncInsertSettingsUnitTest` 3/0/0/0, `ClickHouseVersionDefaultUnitTest` 1/0/0/0, `NewUpstreamConfigDefaultsUnitTest` 4/0/0/0, `NestedNullableCompositeWriteTest` 6/0/0/0, `NetworkCompressionPinUnitTest` 9/0/0/0, `ConvertToMapOrJsonTest` 37/0/0/0, `HeaderToValueTest` 24/0/0/0, `HeadersToValueTest` 25/0/0/0, `TableOperationFilterTest` 26/0/0/0.

Live: `RowBinaryFailFastLiveTest` 2/0/0/0, `JsonDynamicRoundTripTest` 6/0/0/0, `MBeanRegistrationLiveTest` 2/0/0/0, `ClickHouseHelperClientForkLiveTest` 10/0/0/0, `ClickHouseSinkTaskMappingTest` 9/0/0/0, `TableTest` 7/0/0/0, `ExplicitJsonInsertPathLiveTest` 6/0/0/0, `JsonInsertColumnsLiveTest` 5/0/0/0, `ClickHouseSinkTaskJsonInsertFormatTest` 2/0/0/0, `AsyncInsertLiveTest` 1/0/0/0, `ClickHouseTestHarnessTest` 7/0/0/0, `NetworkCompressionPinLiveTest` 9/0/0/0, `SmtSinkLandingTest` 5/0/0/0, `KeeperStateProviderRestartLiveTest` 6/0/0/0.

`SYNC09-OK`: no `NOXML`, `RED` or `EMPTY` line.

### SYNC-10

- `SYNC10-IDENTICAL`: `git diff --quiet refs/tags/v1.7.0 HEAD -- <path>` passed for all 11 unmodified upstream paths.
- Upstream's 9 new or changed methods, each `passed` (not skipped) in the XML:

| Method | Class | Status |
|--------|-------|--------|
| `nullMapTypeTest()` | `ClickHouseSinkTaskWithSchemaTest` | passed |
| `nullMapTypeJsonInsertTest()` | `ClickHouseSinkTaskWithSchemaTest` | passed |
| `nullMapTypesTest()` | `ClickHouseSinkTaskSchemalessTest` | passed |
| `optionalMapFieldNullDoesNotThrow()` | `StructToJsonMapTest` | passed |
| `optionalMapFieldPresent()` | `StructToJsonMapTest` | passed |
| `testSetCompressionMethod_DefaultIsTrue()` | `ClickHouseHelperClientTest` | passed |
| `testSetCompressionMethod_Disabled()` | `ClickHouseHelperClientTest` | passed |
| `testSinkConfig_SetCompressionMethod()` | `ClickHouseHelperClientTest` | passed |
| `retriableFlushFailureDoesNotDuplicateBufferedRecords()` | `ClickHouseSinkTaskBufferTest` | passed |

- `./gradlew compileIntegrationTestJava -q` is green (`COMPILE-IT-OK`), which is the only proof of the #800 Avro-union and cluster `buildFromClause` tests.
- `dependencyInsight --dependency connect-api --configuration compileClasspath` printed `org.apache.kafka:connect-api:4.1.1`; `onPartitionsRevoked` compiles without `@Override` (the full compile is part of the run).

### Anchor-to-row map

Every row of FORK-DELTA's "What this fork still patches" (as edited by this plan) has at least one anchor row in `.planning/scripts/fork-anchors.txt`:

| FORK-DELTA row | Anchor ID(s) |
|----------------|--------------|
| `targetTableFilter` / `jsonInsertColumns` config | CFG-01 (`TARGET_TABLE_FILTER`, `jsonInsertFormatExplicit`) |
| No default `async_insert` | CFG-02 |
| JMX `connector=<name>` | ORCH-02 |
| `onPartitionsRevoked` without `@Override` | RF-8 |
| Metadata-free JSON path | JSONP-01 |
| `jsonInsertColumns` reaches the insert | JSONP-03 |
| RowBinary fail-fast | WRT-01 (`case "DYNAMIC":`, `buildRowBinaryUnsupportedTypeMessage`) |
| CR-02 null guards, MAP scoped to Nullable (new row) | WRT-06 |
| Server-side table filter | HLP-01 |
| `shouldSkipDescribeRow` | HLP-02 |
| Describe reader explicit UTF-8 | HLP-04 |
| `Type.DYNAMIC` | MAP-01 |
| Four SMTs | SUITE-02 |
| CI workflow (`timeout-minutes: 45`) | CI-05 |
| `VERSION` | BLD-07 |
| Five build additions | BLD-03, BLD-04, BLD-05 |
| `slf4j-reload4j` literal | BLD-08 |
| Dependency pin (Connect API `4.1.1`) | BLD-08 (`kafka = "4.1.1"`) |
| `ClickHouseBase` contract | HARN-01, HARN-03 |
| `CLICKHOUSE_VERSION_DEFAULT` | RF-15 |
| Test helpers explicit UTF-8 | HLP-04 |

Gap, reported rather than papered over: inside the "Five fork build additions" row, the `ben-manes` plugin and the Spotless `importOrder()` block have no anchor of their own; the row is covered by the BLD-03, BLD-04 and BLD-05 anchors. No row is without an anchor.

## Docs commit (Task 2)

`ea88d289` touches exactly `docs/FORK-DELTA.md` and `.planning/MERGE-CHECKLIST-v1.7.0.md` (no deletions).

FORK-DELTA changes: (1) the Connector-source D-17 row is removed and a D-17 row added to "Retired in favour of an upstream implementation", ending in the CMP-03 sentence (derived only from Recorded Fact 26) plus the operator `compress=true` caveat; (2) "Six" became "Five fork build additions" without the LZ4 clause, and a Retired row for the fork LZ4 rule was added; (3) the dependency-pin row covers only Connect API `4.1.1` and says client `0.9.6` is upstream's own value; (4) a CR-02 / WRT-06 Connector-source row states why upstream lacks the TUPLE and VARIANT guards (its TUPLE case calls `getClass()` on the value and its VARIANT case calls `values()` on it, each without a null check); (5) the Named skip inventory gains the compile-only integrationTest sentence; (6) "For the next syncer" points to the v1.7.0 checklist first and replaces the "eighth behaviour" paragraph. `grep -c '1\.10\.2' docs/FORK-DELTA.md` prints 0.

Checklist changes: all 11 rows owned by phase 12 that were pending (2 in Section 1, 3 in Section 2, 6 in Section 2a) are `done`, each naming `79664ce3`, `a75bd464` or `9a4f7978`. The three rows left `pending` are owned by 13 (the #820 MAP nested slots, `cleanupExtraFields`) and 14 (`lz4Java`). Recorded Facts 22-30 were appended: WRT-09, nested null Map, BLD-09 after-state, CMP-03 live results, D-12 probe, D-13 outcome, EOS-01, full-suite gate, SYNC-10. `.planning/.gsd-ws-arg` is not tracked.

## End-of-phase gate (D-09)

```
invariants: provenance_paths=21 anchor_commits=14 anchor_rows=40 marker_commits=61 result=PASS

PASS ST1
PASS ST2
PASS ST3
PASS ST4
selftest: no failures
```

`custom_build` and `origin/custom_build` both resolve to `c61d90ce5dfb32ea5876dffacc8bb54d8846739a`. Nothing was pushed.

## Task Commits

1. **Task 1: full-suite gate and SYNC-09 / SYNC-10 evidence** - no commit by design
2. **Task 2: Docs commit** - `ea88d289` (docs)

**Plan metadata:** committed separately (docs: complete plan).

## Files Created/Modified

- `docs/FORK-DELTA.md` - ledger updated for the v1.7.0 merge (D-17 and fork LZ4 rule retired, CR-02 row, dependency-pin row, skip sentence, syncer notes)
- `.planning/MERGE-CHECKLIST-v1.7.0.md` - Phase 12 rows closed with commit evidence; Recorded Facts 22-30

## Decisions Made

- The load-bearing-control sentence transcribes 12-03's drafted sentence, which maps clause by clause to the D-12 rows, and keeps the V1 "not observed" clause instead of generalising.
- FORK-DELTA's introduction still names the `network_compression_method` row as added at `87fa1e78`; it describes the v1.6.0 sync and is left for the Phase 13/15 rewrite, as the plan restricts which rows change.

## Deviations from Plan

### Auto-fixed Issues

None.

### Precondition and verify wording not literally met

**1. Final verify clause "no tracked changes left uncommitted"**
- **Found during:** Task 2 end-of-phase gate
- **Issue:** `git status --porcelain --untracked-files=no` lists `.planning/config.json` and `.planning/state.json` as modified. They are the orchestrator's own bookkeeping, dirty before this plan started, and the orchestrator notes say not to commit or revert them.
- **Resolution:** Not committed and not reverted. Every other clause of the gate passed (script `result=PASS`, self-test ST1-ST4, `custom_build` and `origin/custom_build` equal to `c61d90ce`). The same reading was recorded in 12-01.
- **Files modified:** none

---

**Total deviations:** 0 auto-fixed, 1 verify-wording note
**Impact on plan:** none; no source, test or build file was touched by this plan.

## Issues Encountered

- A first attempt to list upstream's 9 methods used a `sed` expression with `#` as delimiter and a `\1#\2`-style replacement that BSD sed rejected; it was re-run in Python over the XML. No repository state was affected.
- The XML carries no `file.encoding` property, so the `windows-1252` run condition is evidenced by the `tasks.withType<Test>` configuration, not by the report.

## Open items for the user (human judgment, recorded not resolved)

1. **WRT-09 reading (from 12-02, Recorded Fact 22):** zero existing assertions were changed and three new pins were added. Confirm that is the intended reading, or name the assertion expected to change.
2. **D-17 sentence (T-12-15 human-check):** read the new D-17 row in FORK-DELTA's Retired table against Recorded Fact 26 and confirm it says only what the probe rows observed, including the V1 "not observed" clause.
3. **SYNC-10 compile-only reading (RESEARCH Open Question 3):** the #800 Avro-union and cluster `buildFromClause` tests are proven by byte identity and `compileIntegrationTestJava` only. Confirm that satisfies "skip for a named reason", or arrange a Confluent stack run.
4. **ROLL-01 (Phase 15):** operators must not combine `compress=true` in `jdbcConnectionProperties` with `setCompressionMethod=false` on a 26.9+ server (Recorded Fact 26).

## Known Stubs

None.

## Threat Flags

None. No source, test, build or dependency file changed; the docs record resolved versions only. T-12-16: `grep -ciE 'password=|test_password'` prints 0 for both changed files. T-12-18: `custom_build` and `origin/custom_build` are unmoved.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 12 is complete: the checklist has no pending row owned by 12, and the invariant script passes at the end of the phase.
- Phase 13 inherits Recorded Fact 23 (nested null Map) and the two pending rows it owns; Phase 14 owns the `lz4Java` row (BLD-10); Phase 15 owns publication, the FORK-DELTA introduction and numbers (PUB-02), and ROLL-01.

## Self-Check: PASSED

- FOUND: `docs/FORK-DELTA.md`, `.planning/MERGE-CHECKLIST-v1.7.0.md`
- FOUND commit `ea88d289` (ancestor of HEAD); subject starts `docs(12-05):`; touches exactly the two planned files
- Recorded Facts 17-30 are consecutive in the checklist; every fact cited in the changed FORK-DELTA lines (1, 2, 7, 19, 20, 22, 23, 24, 25, 26, 30) exists
- `bash .planning/scripts/verify-merge-invariants.sh <M> HEAD`: `result=PASS`

---
*Phase: 12-upstream-v1-7-0-merge-reconcile*
*Completed: 2026-10-07*
