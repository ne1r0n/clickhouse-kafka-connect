---
phase: 36-client-0-11-0-audit-bump
plan: "01"
subsystem: kafka-connect-sink
tags: [clickhouse-client, client-0.11.0, error-classification, rowbinary-golden, test-pins, CLT-01, CLT-03, CLT-04, CLT-06]

requires:
  - phase: 35-buffered-offset-commit-under-topic-renaming-smts
    provides: the gated post-BUF tree, the read-only evidence helper and the hygiene helper this plan reuses
provides:
  - ServerExceptions.create, a reflection factory that lets the 10 three-argument ServerException sites compile on client 0.9.6 and 0.11.0
  - StubClickHouseServer and ClientErrorShapes, an in-JVM loopback stub and one shared table of error-shape rows per client line
  - 14 Docker-free pin executions (ClientErrorShapePinUnitTest 7, ClientTimeoutPrefixPinUnitTest 2, ClientEndpointAndOptionsPinUnitTest 3, ClientInsertWirePinUnitTest 2) green on 0.9.6
  - Client011BehaviourPinLiveTest, 6 live executions keyed by client line, green on 0.9.6 on ClickHouse latest with client V2
  - the exact bare-0.11.0 red set (3 Docker-free, 3 live) that plan 36-02's createClientV2 fixes turn green
  - RowBinary golden, 48 type cases in two variants (96 lines), blessed on client-v2 0.9.6 and proven able to fail
  - Phase 36 evidence E-01..E-04
affects: [36-02, 36-03, 36-04, phase-38-release]

actuals:
  tokens: 29236
  tasks: 3
  commits: 8

plan_head_before: 99b9d85c15ce1a9b76f5e843b9aec472d24f9708
plan_head_after: 585ccb402b54e69cdd57e7e31a7fa725bc4d76e3

tech-stack:
  added: []
  patterns:
    - "Client behaviour pinned by a real V2 client talking over a loopback socket to a scripted in-JVM stub, then classified by Utils.handleException; rows keyed by client-v2 version, failing with a request for a new row on an unknown line"
    - "Blessed-resource golden with a bless switch that rewrites the file and fails on purpose; a mutation run from a detached worktree proves a one-digit change is caught and names the case"
    - "Bare-bump worktree: only the catalog value changed, so the red set is attributable to the client and nothing else"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/util/ServerExceptions.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientErrorShapes.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientErrorShapePinUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientTimeoutPrefixPinUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientInsertWirePinUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Client011BehaviourPinLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryParityCases.java
    - src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryClientParityLiveTest.java
    - src/test/resources/rowbinary-golden-sha256.txt
    - .planning/phases/36-client-0-11-0-audit-bump/36-COMMAND-EVIDENCE.md
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java

key-decisions:
  - "No src/main file, catalog line, dependency, VERSION or CHANGELOG changed; the client bump and the createClientV2 fixes stay in plan 36-02"
  - "The 10 call sites go through one reflection factory in its own commit, so the later catalog commit stays a one-line change and the CLT-09 revert is one commit"
  - "Every pin file is a byte copy of its plan asset (cmp identical), committed unedited"

patterns-established:
  - "Spotless is checked per file in a detached worktree; for an existing file the figure is read against the staged copy of the edited file, so it counts what Spotless still wants to change and not the edit itself"

requirements-completed: [CLT-01, CLT-03, CLT-04, CLT-06]

coverage:
  - id: D1
    description: "The 10 three-argument ServerException sites compile on client 0.9.6 and 0.11.0 through one factory; a worktree without the factory fails compileTestJava with exactly 10 constructor errors, one with the factory and only the catalog value changed compiles and keeps UtilsTest, both AdditionalRetriableErrorCodes classes and the shape pin green"
    requirement: "CLT-01"
    verification:
      - kind: other
        ref: "36-COMMAND-EVIDENCE.md E-02; P36-01-T1-OK and P36-01-T1-TESTS-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "Error shapes (502, 503, unexpected status, 159, 252, 60, bare ClientException) and the two timeout-message prefixes are pinned through a real socket on 0.9.6 and classified by Utils.handleException; the same 7 shape rows also pass on 0.11.0"
    requirement: "CLT-01"
    verification:
      - kind: unit
        ref: "ClientErrorShapePinUnitTest (7), ClientTimeoutPrefixPinUnitTest (2); E-02, E-03"
        status: pass
    human_judgment: false
  - id: D3
    description: "The endpoint path, the unknown-key break and the V1-only jdbcConnectionProperties keys are green on 0.9.6 and red on a bare 0.11.0 worktree, exactly 3 Docker-free and 3 live tests and nothing else"
    requirement: "CLT-03"
    verification:
      - kind: unit
        ref: "ClientEndpointAndOptionsPinUnitTest (3), Client011BehaviourPinLiveTest, ClickHouseSinkJdbcPropertiesTest; E-03 bare_dockerfree_prediction_met and bare_live_prediction_met"
        status: pass
    human_judgment: false
  - id: D4
    description: "Live 159 as RetriableException, ssl and sslmode against a real server, the 502 and 503 shapes with and without retryOnSocketException, the non-retriable ClientException and the ZSTD decode keyed by client line are pinned on ClickHouse latest with client V2"
    requirement: "CLT-04"
    verification:
      - kind: integration
        ref: "Client011BehaviourPinLiveTest (6) on 26.10.3.7; E-03 live_xml"
        status: pass
    human_judgment: false
  - id: D5
    description: "The exact RowBinary body of 48 type cases, plain and RowBinaryWithDefaults, equals the golden blessed on client-v2 0.9.6, under the workstation zone and America/Los_Angeles; bless mode fails on purpose and a one-digit mutation turns the test red naming the case"
    requirement: "CLT-06"
    verification:
      - kind: integration
        ref: "RowBinaryClientParityLiveTest (1) on 26.10.3.7; E-04 verify_xml, tz_la_xml, mutation_prediction_met"
        status: pass
    human_judgment: false

duration: 16min
completed: 2026-10-29
status: complete
---

# Phase 36 Plan 01: Client 0.11.0 Audit - Pins on 0.9.6 Summary

**The client behaviour the 0.11.0 audit must compare is pinned on 0.9.6 before any bump: a reflection factory makes the test sources compile on both client lines, an in-JVM stub drives the real V2 client into `Utils.handleException` (14 Docker-free and 7 live new tests, all green), a bare 0.11.0 worktree turns exactly the endpoint and unknown-key pins red, and a 96-line RowBinary golden is blessed on 0.9.6.**

## Performance

- **Duration:** about 16 min (start not captured; the first command ran at about 2026-10-29T12:22Z)
- **Completed:** 2026-10-29T12:37Z
- **Tasks:** 3
- **Files:** 15 (11 new test-side files, 3 edited test files, 1 evidence file)

## Accomplishments

- **Pre-flight and baselines (E-01).** Origin `custom_build` is still `3a3a15a9`; no `pre-v1.7.0-tail-2`, `sync/v1.7.0` or scratch ref on origin; PR #17 (OPEN, MERGEABLE, head `6a53525b`) and the Dependabot branch were only read; the gate scripts are unchanged from Phase 33; anchors mirror `rows=148 bad=0`; the catalog line is `0.9.6`. Baselines re-measured on the untouched tree: testUnit 1458/0/0/2, full suite 2049/0/0/18 skips (skips helper `OK`), ClickHouse 26.10.3.7, digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (same as Phase 35), no JVM crash.
- **Factory (E-02).** `ServerExceptions.create` replaces the 10 three-argument `new ServerException(` sites (UtilsTest 8, AdditionalRetriableErrorCodesTest 1, AdditionalRetriableErrorCodesPinUnitTest 1) in one four-file commit. Class tallies unchanged (34, 3, 19) and Spotless hunk sets unchanged (8/9 and 10/11, the pin class and the factory clean). A worktree without the factory fails `compileTestJava` with exactly 10 constructor errors (`required: int,String,int,String`, `found: int,String,int`); one with the factory and only the catalog value changed compiles and keeps the 7 shape rows, `UtilsTest` and both `AdditionalRetriableErrorCodes` classes green on 0.11.0.
- **Pins on 0.9.6 (E-02, E-03).** Docker-free: 7, 2, 3 and 2 executions green; live: `Client011BehaviourPinLiveTest` 6 green. All files are byte copies of the plan assets with identical Spotless output and no `@Disabled`, tag or assumption.
- **Bare-bump red set (E-03).** In a throwaway worktree whose only change is the catalog value, the Docker-free red set is exactly the three `ClientEndpointAndOptionsPinUnitTest` tests and the live red set exactly `ClickHouseSinkJdbcPropertiesTest` `primitiveTypesTest` and `withEmptyDataRecordsTest` plus the `Client011BehaviourPinLiveTest` ssl-and-sslmode pin; the shape, timeout and wire pins and the other five live pins are green on 0.11.0. Root message: `Unknown and unmapped config properties: [ssl, sslmode]`.
- **RowBinary golden (E-04).** 48 cases, 96 data lines (48 plain, 48 with defaults), data-lines SHA-256 `716b0f7e5de97198e031983d4602f135c0bd14753e6603d7747338e46c1acb6e` (equal to the planning prototype), header `# blessed on client-v2 0.9.6 (revision: 5777f5a)`. Green under the workstation zone and `America/Los_Angeles`; the bless run exited non-zero on purpose; flipping one hex digit of the `plain|string|rowbinary|false|` line turned the test red naming that case.

## Bare-bump red set

Exact names plan 36-02 turns green (from E-03):

Docker-free (3), `ClientEndpointAndOptionsPinUnitTest`:
- `CLT-03: the request path is slash and the database travels in the X-ClickHouse-Database header`
- `CLT-03: ssl=true and sslmode=strict build a working V2 client and never reach the server`
- `CLT-03: the V1-only keys of the live jdbcConnectionProperties pins are accepted too`

Live (3):
- `ClickHouseSinkJdbcPropertiesTest#primitiveTypesTest`
- `ClickHouseSinkJdbcPropertiesTest#withEmptyDataRecordsTest`
- `Client011BehaviourPinLiveTest#CLT-04: ssl=true and sslmode=strict still ping and query a real server on V2`

## Task Commits

1. **Task 1 (tracer):** `3db861ff` docs (evidence opened, E-01); `c6c7edb4` test (factory); `9dd36588` test (stub, shapes, 7-row pin); `681de964` docs (E-02)
2. **Task 2:** `765476af` test (timeout prefixes, endpoint and options, insert wire, live behaviours); `5ccd15ea` docs (E-03)
3. **Task 3:** `83d0b42a` test (RowBinary golden); `585ccb40` docs (E-04)

**Plan metadata:** the SUMMARY commit that follows this file.

## Handoffs

For plan 36-02:
- Commit SHAs: factory `c6c7edb4ab5c821ac1c5c53a0dae4872315a2059`, shape pin `9dd365882f2188ec27a8fb762aba300039a59523`, pins `765476af5d09c008e6b259299b6ff47deb83c06c`, golden `83d0b42a4e5e92a620577df74e83b684e8327439`. The tree at the start of the phase (`local_head`) is `99b9d85c15ce1a9b76f5e843b9aec472d24f9708`.
- E-01 baselines: testUnit 1458/0/0/2; full suite 2049/0/0/18, 591 Docker-only tests; class tallies `UtilsTest` 34, `AdditionalRetriableErrorCodesTest` 3, `AdditionalRetriableErrorCodesPinUnitTest` 19, `ClickHouseSinkJdbcPropertiesTest` 2, `NetworkCompressionPinLiveTest` 9; ClickHouse 26.10.3.7, digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`.
- The golden file `src/test/resources/rowbinary-golden-sha256.txt` is untouched from here on; only a deliberate bless run writes it.
- New tests every later count is measured against: 14 Docker-free (testUnit 1458 + 14 = 1472) and 7 live (full suite 2049 + 21 = 2070, before any plan 36-02 addition). The 10 factory call sites add no test. The 18 named skips stay exactly 18.
- Zone mechanism: `TZ` reached the test JVM and the golden stayed green under `America/Los_Angeles`; plan 36-02 (E-07) proves the mechanism.

## Deviations from Plan

None - plan executed exactly as written.

Two process notes, neither a deviation. First, the plan's Spotless recipe says to record the numstat of an existing file "again" after copying it over a second `spotlessApply`; read literally against `HEAD` that figure includes the call-site edit itself (a first attempt printed 2/2, 10/11 and 18/20). The numstat is therefore read against the staged copy of the edited file (the copies were `git add`ed in the throwaway worktree before the second run), which is the quantity the plan's baseline compares (8/9 and 10/11, the pin class clean). Second, one of my own check commands in the Task 2 red-set step called `failed` without `--expect` and printed a `FAIL` line, followed immediately by the plan's `--expect` call, which printed `OK`; only the second is the check of the plan.

**Total deviations:** 0.
**Impact:** none.

## Authentication Gates

None.

## Issues Encountered

None. No JVM crash occurred in any run (no `javacore`, core dump, `Snap` or `jitdump` file appeared).

## Known Stubs

None. The `StubClickHouseServer` is a test double by design (a scripted loopback HTTP server), not a stub standing in for unfinished code; no hardcoded empty value, placeholder or unwired data was added, and no skipped or disabled test was added (the skip inventory stays 18).

## Threat Flags

None. No `src/main` file changed; the stub binds `127.0.0.1` on an ephemeral port and holds no credential.

## Items for the user

- Nothing was pushed, no pull request or Dependabot branch was touched, and origin `custom_build` still reads `3a3a15a9ae064ffe09c696b8bb87bce23419d463`. No dependency, catalog line, `VERSION` or `CHANGELOG.md` changed.
- `.planning/config.json` and `.planning/state.json` were left uncommitted; `.planning/STATE.md` is orchestrator-managed.

## Self-Check: PASSED

All 12 created files exist on disk, the eight plan commits are ancestors of HEAD, P36-01-T1-OK, P36-01-T1-TESTS-OK, P36-01-T2-OK, P36-01-T2-TESTS-OK, P36-01-T3-OK and P36-01-T3-TESTS-OK were printed on the committed tip, and no worktree other than the main tree remains.
