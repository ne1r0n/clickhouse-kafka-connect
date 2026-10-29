---
phase: 36-client-0-11-0-audit-bump
plan: "02"
subsystem: kafka-connect-sink
tags: [clickhouse-client, client-0.11.0, createClientV2, catalog-bump, fork-anchors, non-utc, v1-mode, integration-test, CLT-02, CLT-03, CLT-04, CLT-07]

requires:
  - phase: 36-client-0-11-0-audit-bump
    provides: plan 36-01 pins on client 0.9.6, the bare-bump red set and the RowBinary golden blessed on 0.9.6
provides:
  - createClientV2 fix commit on client 0.9.6 (endpoint of protocol, host and port only; ignore_unknown_config_key=true before the property loop), proven a no-op there
  - the catalog commit, clickHouseDriver 0.11.0 as one marked fork override with a Docker-free pin and a mid-file anchor row
  - every pin that was red on the bare bump green on 0.11.0, BLD-16 green without a re-bless, the golden blessed on 0.9.6 still matching
  - CLT-07 local halves: date and datetime live pins green under four zones, the V1-mode run green, the Connect-worker integrationTest green against the shaded 0.11.0 archive
  - Phase 36 evidence E-05, E-06 and E-07
affects: [36-03, 36-04, phase-38-release]

actuals:
  tokens: 6374
  tasks: 3
  commits: 5

plan_head_before: 0a88f7b77708019053ab0edb2ef1c1c523a0b0ad
plan_head_after: beeee6d815debda90087735caf3a9e6834160cae

tech-stack:
  added:
    - "com.clickhouse client-v2 / clickhouse-client / clickhouse-data / clickhouse-http-client 0.11.0 (one catalog value, replaces 0.9.6); com.github.luben:zstd-jni 1.5.7-23 arrives only through client-v2's own POM"
  patterns:
    - "A fix that is a no-op on the shipped dependency lands first, so the dependency bump is one line and a revert of it keeps the fix"
    - "Catalog anchor row inserted mid-file next to the other catalog rows so a later appended block cannot make a git revert of the bump conflict"
    - "A zone probe with a negative control proves an environment variable reaches the test JVM before the zone-dependent pins are trusted"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/ci/ClientCatalogPinUnitTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
    - gradle/libs.versions.toml
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - .planning/phases/36-client-0-11-0-audit-bump/36-COMMAND-EVIDENCE.md

key-decisions:
  - "No third createClientV2 option (the custom_ forwarding of 0.11.0 stays a rollout item for plan 36-04, per the research's resolved default)"
  - "ignore_unknown_config_key is set through the connector's own string constant, not the client's constant that exists only in 0.11.0, so a revert of the bump still compiles"
  - "The CLT-02 anchor row sits directly after the kafka row, never at the end of fork-anchors.txt"

patterns-established:
  - "Per-file formatting check in a detached worktree; for an existing file the figure is the staged copy against a second spotlessApply run"

requirements-completed: [CLT-02, CLT-03, CLT-04, CLT-07]

coverage:
  - id: D1
    description: "createClientV2 passes protocol://host:port only to addEndpoint and sets ignore_unknown_config_key=true before the jdbcConnectionProperties loop; on client 0.9.6 every plan 36-01 pin keeps its result and the cache-busted testUnit is exactly 14 larger than the baseline"
    requirement: "CLT-03"
    verification:
      - kind: unit
        ref: "ClientEndpointAndOptionsPinUnitTest (3), ClientErrorShapePinUnitTest (7), ClientTimeoutPrefixPinUnitTest (2), ClientInsertWirePinUnitTest (2); 36-COMMAND-EVIDENCE.md E-05; P36-02-T1-OK, P36-02-T1-TESTS-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "clickHouseDriver is 0.11.0 as one marked fork override in one commit of three paths, seen red on the unbumped catalog first, with all four client modules resolving 0.11.0 on four configurations and the deprecation-warning count equal to 0.9.6"
    requirement: "CLT-02"
    verification:
      - kind: unit
        ref: "ClientCatalogPinUnitTest (2); E-06; P36-02-T2-OK, P36-02-T2-TESTS-OK"
        status: pass
    human_judgment: false
  - id: D3
    description: "On 0.11.0 with the fixes the 16 Docker-free pins, BLD-16, UtilsTest and the AdditionalRetriableErrorCodes classes, Client011BehaviourPinLiveTest (6), the RowBinary golden blessed on 0.9.6, ClickHouseSinkJdbcPropertiesTest, NetworkCompressionPinLiveTest and Lz4RoundTripLiveTest are green"
    requirement: "CLT-04"
    verification:
      - kind: integration
        ref: "E-06 live_xml, golden_xml, jdbc_props_xml, netcomp_live_xml, lz4_live_xml on ClickHouse 26.10.3.7 with client V2"
        status: pass
    human_judgment: false
  - id: D4
    description: "The 15 date and datetime live executions are green on 0.11.0 under UTC, America/Los_Angeles, Asia/Kolkata and Pacific/Kiritimati with a zone probe and a red negative control, the V1-mode run of 35 executions is green, and the Connect-worker integrationTest exits 0 on HotSpot Temurin 17 against an archive carrying client 0.11.0"
    requirement: "CLT-07"
    verification:
      - kind: integration
        ref: "E-07; P36-02-T3-OK; integrationTest 55 tests, 54 succeeded, 0 failed, 1 skipped, 31m 30s"
        status: pass
    human_judgment: false
  - id: D5
    description: "CLT-07's manual nightly dispatch needs a scratch branch pushed to origin and is the user's step; it was neither run nor prepared here"
    requirement: "CLT-07"
    verification: []
    human_judgment: true
    rationale: "A push to origin and a workflow dispatch are one-way actions only the user may authorize; plan 36-04 writes the handoff"

duration: 56min
completed: 2026-10-29
status: complete
---

# Phase 36 Plan 02: createClientV2 Fixes and the Client 0.11.0 Bump Summary

**`createClientV2` now hands the V2 client `protocol://host:port` only and sets `ignore_unknown_config_key=true` before the property loop (no-ops on 0.9.6), then `clickHouseDriver` moves to 0.11.0 in one revertable catalog commit, and every 36-01 pin, BLD-16, the 0.9.6 golden, the non-UTC date pins, the V1-mode run and the Connect-worker `integrationTest` are green on 0.11.0.**

## Performance

- **Duration:** about 56 min (start 2026-10-29T12:38Z, end 13:34Z; the `integrationTest` alone took 31m 30s)
- **Tasks:** 3
- **Files:** 6 (1 new test, 4 edited repository files, 1 evidence file)

## Accomplishments

- **Fix commit (E-05).** `ClickHouseHelperClient.java` changes by 9 added and 1 removed lines (no import, no new object): a public `IGNORE_UNKNOWN_CONFIG_KEY` constant, a local `endpoint` of protocol, host and port passed to `addEndpoint`, and one `setOption` before the `jdbcConnectionProperties` loop so an operator's own value still wins. The zero anchor row read 1 before the fix. On 0.9.6 every pin keeps its result (7, 2, 3 and 2 Docker-free; 6 behaviour pins, the golden and the JDBC-properties class live), the planning-id guard is green, testUnit is 1472 (exactly 14 above the 1458 baseline) and the Spotless hunk set of the file is unchanged (3/4 before and after). Anchors mirror `rows=151 bad=0`; tail gate `anchor_rows=151 anchor_exempt=0 result=PASS`.
- **Catalog commit (E-06).** `clickHouseDriver = "0.11.0"  # FORK OVERRIDE (upstream 0.9.6)` is the one catalog change, with `ClientCatalogPinUnitTest` (seen red on the unbumped catalog, exactly its version-line test) and one anchor row directly after the `kafka` row. All four client modules resolve 0.11.0 on `runtimeClasspath`, `clickhouseDependencies`, `testRuntimeClasspath` and `integrationTestRuntimeClasspath`; `zstd-jni` 1.5.7-23 comes only through the client's own POM. The four compile tasks pass and the deprecation-warning count is 100 on both client lines. On 0.11.0: the 7, 2, 3, 2 and 2 Docker-free pins, BLD-16 (3 tests, no re-bless), `UtilsTest` 34, `AdditionalRetriableErrorCodesTest` 3, `Client011BehaviourPinLiveTest` 6, the golden blessed on 0.9.6 (1), `ClickHouseSinkJdbcPropertiesTest` 2, `NetworkCompressionPinLiveTest` 9 and `Lz4RoundTripLiveTest` 2 are green; the bare-bump red set of E-03 is green with the fixes. testUnit is 1474 (exactly 16 above baseline). Anchors mirror `rows=152 bad=0`; tail gate `anchor_rows=152 anchor_exempt=0 result=PASS`.
- **CLT-07 local halves (E-07).** The zone reaches the test JVM (probes pass under `America/Los_Angeles`, `Asia/Kolkata`, `Pacific/Kiritimati`; the mismatched control goes red naming the probe). The 15 date and datetime live executions (5 + 9 + 1) are green under UTC and the three non-UTC zones. The V1-mode run (35 executions, 4 files) is green. The Connect-worker `integrationTest` (HotSpot Temurin 17, archive built from the catalog commit tree, `version=0.11.0` in its `clickhouse-client-version.properties`) exits 0: 55 tests, 54 succeeded, 0 failed, 1 skipped, 31m 30s, the connector class 54 executions with none skipped, the same shape as Phase 31 E-18.
- Every evidence entry records ClickHouse 26.10.3.7, digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`. No JVM crash occurred.

## Commits

| Commit | SHA | Paths |
|--------|-----|-------|
| fix (CLT-03) | `a1cc33825a07d960bc336e0e6c7474c6698ab40b` | `.planning/scripts/fork-anchors.txt`; `docs/FORK-DELTA.md`; `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` |
| evidence E-05 | `939e8992` | `36-COMMAND-EVIDENCE.md` |
| catalog (CLT-02) | `5ab3410e94fd64e8fa6a8bca6ffd3510b13d68f8` | `.planning/scripts/fork-anchors.txt`; `gradle/libs.versions.toml`; `src/test/java/com/clickhouse/kafka/connect/ci/ClientCatalogPinUnitTest.java` |
| evidence E-06 | `3d0b0461` | `36-COMMAND-EVIDENCE.md` |
| evidence E-07 | `beeee6d8` | `36-COMMAND-EVIDENCE.md` |

**Plan metadata:** the SUMMARY commit that follows this file.

## Handoffs

For plan 36-03:
- `fix_commit` = `a1cc33825a07d960bc336e0e6c7474c6698ab40b` (the "before" archive is built on the tree where the catalog is still 0.9.6 and the fixes are in: this commit), `catalog_commit` = `5ab3410e94fd64e8fa6a8bca6ffd3510b13d68f8` (the "after" archive).
- 0.11.0 tallies: testUnit 1474 (1458 baseline + 16); Docker-free pins 7, 2, 3, 2, catalog pin 2; BLD-16 3; `UtilsTest` 34; `AdditionalRetriableErrorCodesTest` 3, `AdditionalRetriableErrorCodesPinUnitTest` 19; `ClickHouseHelperClientForkUnitTest` 51; `NetworkCompressionPinUnitTest` 9; live `Client011BehaviourPinLiveTest` 6, golden 1, `ClickHouseSinkJdbcPropertiesTest` 2, `NetworkCompressionPinLiveTest` 9, `Lz4RoundTripLiveTest` 2.
- Resolved modules: `client-v2`, `clickhouse-client`, `clickhouse-data`, `clickhouse-http-client` all 0.11.0; `zstd-jni` 1.5.7-23 (client POM). The shaded archive from the catalog tree is at `build/confluentArchive/clickhouse-kafka-connect-v1.7.0_gg5/lib/clickhouse-kafka-connect-v1.7.0_gg5-confluent.jar` (the working copy of `VERSION` still reads `v1.7.0_gg5`); its client properties read `version=0.11.0`.
- The golden file `src/test/resources/rowbinary-golden-sha256.txt` is untouched; the byte comparison on 0.11.0 is plan 36-03's.

For plan 36-04:
- `integration_head` `3d0b046118b4f2514655a76b66be72ee66ce8764`, started 2026-10-29T13:02:19Z, ended 13:34:04Z, `BUILD SUCCESSFUL in 31m 30s`, summary `55 tests, 54 succeeded, 0 failed, 1 skipped`, connector class 54 executions, `ExactlyOnceTest` 8 and `ClickHouseCloudTest` 1 skipped by assumption, HotSpot Temurin 17 (`adoptium-17-aarch64-hotspot-mac`), server 26.10.3.7.
- Anchors count is now 152 (the tail gate of plan 36-04 must read `anchor_rows=152` before its own additions).
- The rollout items are unchanged from the plan: the `custom_` forwarding of 0.11.0 (no option added), `ignore_unknown_config_key=true` turning a mistyped client option into a WARN, `zstd-jni` native loading under a noexec temp folder.
- **CLT-07's manual nightly dispatch is NOT done**: it needs a scratch branch on origin and is the user's step; plan 36-04 writes the handoff and the orchestrator asks the user.

## Deviations from Plan

None - plan executed exactly as written.

Two process notes, neither a deviation. First, a `paste -sd '; '` call used to preview the fix commit's file list cycles its delimiters (`;` then a space), so the evidence value was written by hand with `; ` separators and then checked by the plan's own gate. Second, the red catalog-pin run was followed by a `failed` call without `--expect` that printed `FAIL` lines for the one expected red test; only the `--expect` call (`OK`) is the plan's check.

**Total deviations:** 0.
**Impact:** none.

## Authentication Gates

None.

## Issues Encountered

None. No JVM crash occurred (no `javacore`, core dump, `Snap` or `jitdump` file), including in the 31-minute `integrationTest` on the HotSpot toolchain. The local `.planning/config.json` and `.planning/state.json` modifications predate the phase and were never staged.

## Known Stubs

None. No hardcoded empty value, placeholder or unwired data was added; no skipped or disabled test was added (the `ExactlyOnceTest` and `ClickHouseCloudTest` skips are existing assumption aborts without cluster or cloud properties, and the 18 named skips of the unit suite are untouched).

## Threat Flags

None beyond the plan's register. The bump is the disclosed T-36-06 (one catalog line, `zstd-jni` through the client POM, nothing else moved); no new endpoint, auth path or file access was added, and no log line was added.

## Items for the user

- The authorization for the `clickHouseDriver` 0.9.6 to 0.11.0 bump is the milestone ROADMAP and the `/gsd-autonomous` request, disclosed in the plan and in the catalog commit message. PR #17 (the Dependabot bump of the same line) was not touched.
- CLT-07's manual nightly dispatch is waiting for the user's decision (it needs a scratch branch pushed to origin).
- Nothing was pushed; origin `custom_build` still reads `3a3a15a9ae064ffe09c696b8bb87bce23419d463`.

## Self-Check: PASSED

The created pin class and all edited files exist on disk, the five plan commits (`a1cc3382`, `939e8992`, `5ab3410e`, `3d0b0461`, `beeee6d8`) are ancestors of HEAD, P36-02-T1-OK, P36-02-T1-TESTS-OK, P36-02-T2-OK, P36-02-T2-TESTS-OK and P36-02-T3-OK were printed on the committed tip, the catalog commit holds exactly three paths, and no worktree other than the main tree remains.
