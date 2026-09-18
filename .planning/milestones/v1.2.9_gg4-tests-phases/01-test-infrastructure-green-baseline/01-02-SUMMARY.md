---
phase: 01-test-infrastructure-green-baseline
plan: 02
subsystem: testing
tags: [testcontainers, clickhouse, junit, jmx, encoding, testing-infrastructure]

# Dependency graph
requires:
  - phase: 01-01
    provides: "ClickHouse latest default test image, green baseline, ClickHouseServerVersionSmokeTest"
provides:
  - "ClickHouseBase: shared latest container started at most once per test JVM fork (PullPolicy.alwaysPull()), per-class database via newTestDatabaseName(), single inherited @Tag(\"clickhouse\")"
  - "ClickHouseBase: @AfterEach cleanupLeakedMBeans() safety net, opt-in uniqueConnectorName(TestInfo) helper"
  - "Proxy classes (WithSchema/Schemaless) isolated on their own proxyDb 23.8 container with their own @AfterAll teardown, never touching the shared latest singleton"
  - "ClickHouseTestHarnessTest: 5 tests proving the singleton, per-class database, MBean leak sweep, unique-name safety, and UTF-8 helper decoding"
  - "ClickHouseTestHelpers.getAllRowsAsJson / validateRows decode ClickHouse HTTP responses as UTF-8"
affects: [01-03, 02, 03, 04, 05]

# Actuals (#2632)
actuals:
  tokens: 5700
  tasks: 2
  commits: 4
  plan_head_before: d733586f3730d57742af1d7425ce8f026bad7cab

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Container-per-fork singleton: static db field guarded by if (db == null), started once per test JVM, cleaned up by Testcontainers Ryuk rather than an explicit @AfterAll stop"
    - "Per-class isolated database via newTestDatabaseName() (kafka_connect_test_<ts>_<counter>), created in @BeforeAll and dropped in @AfterAll, independent of container lifecycle"
    - "@AfterEach MBean sweep as a base-class safety net for new/touched tests that fail between start() and stop(), instead of retrofitting every existing test file"
    - "Deliberate-failure JUnit test (custom RuntimeException + TestExecutionExceptionHandler) used as the proof mechanism for lifecycle safety nets"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java

key-decisions:
  - "Ryuk reliably reaps the shared container and Ryuk's own container within ~20s of the Gradle JVM exiting; no JVM shutdown hook was added (the plan's escape hatch was not needed in either task, confirmed by a second docker ps check after a short wait, per Claude's Discretion in 01-CONTEXT.md)."
  - "No maxParallelForks change: the full suite showed 0 flakes at the default (cores/2 = 6) fork count on both the single-JVM multi-class run and the full-suite run, so D-04's evidence-gated change was not triggered."
  - "Order(3)/Order(4) needed a table created in the class database before start(): ClickHouseWriter.start() returns false (\"Did not find any tables in destination\") against an empty database, and the class database starts empty by design (Order(2) proves this). Added a minimal single-column table creation to both leak-probe tests; not called out explicitly in the plan's action steps but required for ClickHouseSinkTask.start() to succeed at all."
  - "gsd_run check tdd-red-evidence was not run for the D-11 RED phase: that verb classifies Node/TAP test output (parseNodeTestSummary/tapFailedTestNames) and does not parse Gradle/JUnit XML output. The plan's own explicit RED-capture instructions (run Order(5) before touching ClickHouseTestHelpers, save the failure output) were followed instead and are recorded below."

requirements-completed: [INFRA-03, INFRA-04, INFRA-05, INFRA-06]

coverage:
  - id: D1
    description: "Shared latest container started at most once per test JVM fork (PullPolicy.alwaysPull()), proven across a proxy class, an ordinary ClickHouse-backed class, the 01-01 smoke test, and the harness test in one JVM (INFRA-03, D-05)"
    requirement: "INFRA-03"
    verification:
      - kind: integration
        ref: "ClickHouseTestHarnessTest#sharedContainerIsStartedOncePerJvmAndIsTheRequestedImage; single-JVM run (--max-workers=1) shows exactly 1 'Starting shared ClickHouse container' log line across 26 tests"
        status: pass
    human_judgment: false
  - id: D2
    description: "Every ClickHouseBase subclass gets a fresh, isolated kafka_connect_test_ database per class, dropped in tearDown (D-08)"
    requirement: "INFRA-03"
    verification:
      - kind: integration
        ref: "ClickHouseTestHarnessTest#classGetsFreshIsolatedDatabase"
        status: pass
    human_judgment: false
  - id: D3
    description: "Proxy classes (WithSchema/Schemaless) run on their own 23.8 proxyDb container and never touch the shared latest singleton (D-07)"
    requirement: "INFRA-03"
    verification:
      - kind: integration
        ref: "ClickHouseSinkTaskWithSchemaProxyTest / ClickHouseSinkTaskSchemalessProxyTest full suites pass unchanged; grep -nE '^\\s+db = ' on both files returns nothing"
        status: pass
    human_judgment: false
  - id: D4
    description: "Single @Tag(\"clickhouse\") on ClickHouseBase, inherited by every subclass (INFRA-04, D-12)"
    requirement: "INFRA-04"
    verification:
      - kind: unit
        ref: "grep -c '@Tag(\"clickhouse\")' ClickHouseBase.java == 1; grep -rl '@Tag(' src/test/java returns only ClickHouseBase.java"
        status: pass
    human_judgment: false
  - id: D5
    description: "@AfterEach MBean sweep unregisters and logs leftover connector MBeans after a test that fails between start() and stop() (INFRA-05, D-09)"
    requirement: "INFRA-05"
    verification:
      - kind: integration
        ref: "ClickHouseTestHarnessTest#testFailingBetweenStartAndStopLeaksAnMBean + #leakedMBeanWasSweptAndUniqueNamesAreSafe; TEST-*.xml contains 'Unregistering leaked MBean left by a test' for connector=harness-mbean-leak-probe exactly once"
        status: pass
    human_judgment: false
  - id: D6
    description: "uniqueConnectorName(TestInfo) produces distinct, ObjectName-safe tokens for opt-in use by new tests (D-10)"
    requirement: "INFRA-05"
    verification:
      - kind: unit
        ref: "ClickHouseTestHarnessTest#leakedMBeanWasSweptAndUniqueNamesAreSafe (distinctness + ^[A-Za-z0-9_$-]+$ regex assertions)"
        status: pass
    human_judgment: false
  - id: D7
    description: "ClickHouseTestHelpers.getAllRowsAsJson and validateRows decode responses as UTF-8, proven RED (windows-1252 mojibake) then GREEN (INFRA-06, D-11)"
    requirement: "INFRA-06"
    verification:
      - kind: integration
        ref: "ClickHouseTestHarnessTest#helpersDecodeResponsesAsUtf8; RED commit 2387d53, GREEN commit 1adf5f6"
        status: pass
    human_judgment: false
  - id: D8
    description: "Full suite green on the new harness with no CLICKHOUSE_VERSION/CLIENT_VERSION overrides, shared-container start count at or below maxParallelForks, no leftover Testcontainers containers"
    requirement: "INFRA-03"
    verification:
      - kind: integration
        ref: "build/phase1-02-full-test.log (Test result: SUCCESS, Tests summary: 194 tests, 187 succeeded, 0 failed, 7 skipped; shared-container starts = 6 = maxParallelForks)"
        status: pass
    human_judgment: false

duration: 55min
completed: 2026-09-16
status: complete
---

# Phase 1 Plan 2: Container Singleton, Per-Class Databases, Proxy Isolation, MBean Safety Net, UTF-8 Helpers Summary

**One shared ClickHouse `latest` container per test JVM fork (down from one per class), a fresh isolated database per test class, isolated 23.8 proxy containers, an `@AfterEach` MBean leak sweep proven by a deliberate mid-test failure, and UTF-8 decoding in test helpers proven RED-then-GREEN — full suite green at 194 tests, 0 failed, 7 skipped.**

## Performance

- **Duration:** ~55 min
- **Started:** 2026-09-15T21:41:00Z (approx.)
- **Completed:** 2026-09-16T00:57:00Z (approx.)
- **Tasks:** 2
- **Files modified:** 5 (1 created, 4 modified)

## Accomplishments

- `ClickHouseBase`: single `@Tag("clickhouse")` on the class (inherited by all 14+ subclasses); shared `latest` container built with `PullPolicy.alwaysPull()` and started at most once per test JVM fork via `if (db == null)`; every class gets its own `kafka_connect_test_<ts>_<counter>` database, dropped (not the container) in `tearDown()`.
- `ClickHouseSinkTaskWithSchemaProxyTest` / `ClickHouseSinkTaskSchemalessProxyTest`: each now owns a `proxyDb` field and its own `@AfterAll tearDown()` stopping `proxyDb`/`toxiproxy`, so the 23.8 proxy container never assigns, overwrites, or stops the shared `latest` singleton.
- `ClickHouseBase.cleanupLeakedMBeans()` (`@AfterEach`): unregisters and logs any leftover `com.clickhouse:type=ClickHouseKafkaConnector` MBean after every test. `ClickHouseBase.uniqueConnectorName(TestInfo)`: opt-in helper producing distinct, `ObjectName`-safe connector names for new/touched tests; `createProps()` is unchanged so existing tests keep their `connector=null` MBean names.
- New `ClickHouseTestHarnessTest` (5 tests, `@TestMethodOrder`): proves the container singleton, the per-class database, the MBean sweep after a deliberate mid-test failure, unique-name safety, and UTF-8 helper decoding.
- `ClickHouseTestHelpers.getAllRowsAsJson` and `validateRows` now pass `StandardCharsets.UTF_8` to their `InputStreamReader`s, proven RED (mojibake under `file.encoding=windows-1252`) then GREEN.
- Full suite green with no `CLICKHOUSE_VERSION`/`CLIENT_VERSION` overrides: 194 tests, 187 succeeded, 0 failed, 7 skipped (`@Disabled` count unchanged from 01-01's baseline). Shared-container starts per full run: 6 (== `maxParallelForks`), down from 14 individual containers in the 01-01 baseline.

## Task Commits

1. **Task 1: Tracer — shared container singleton + per-class database + proxy isolation + @Tag, proven in one test JVM** - `5a903dd` (feat)
2. **Task 2: MBean safety net, unique connector names, and UTF-8 helpers, each proven, then the full suite green on the new harness** - three commits (TDD cycle for the D-11 slice):
   - `0fb6603` (feat) — MBean sweep + `uniqueConnectorName` + harness Order(3)/Order(4)
   - `2387d53` (test) — Order(5) UTF-8 test added, confirmed RED
   - `1adf5f6` (feat) — `StandardCharsets.UTF_8` added to helpers, confirmed GREEN

**Plan metadata:** committed separately (docs commit, see below)

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java` - new 5-test harness proof (container singleton, per-class DB, MBean sweep, unique names, UTF-8)
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` - `@Tag`, `newTestDatabaseName()`, `sharedContainerStarts()`, singleton `setup()`/`tearDown()`, `cleanupLeakedMBeans()`, `uniqueConnectorName()`
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java` - `proxyDb` field + own `@AfterAll tearDown()`
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java` - `proxyDb` field + own `@AfterAll tearDown()`
- `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` - `StandardCharsets.UTF_8` in `getAllRowsAsJson` and `validateRows`

## TDD RED/GREEN Evidence (D-11)

**RED** (commit `2387d53`, before touching `ClickHouseTestHelpers`):

```
ClickHouseTestHarnessTest > helpersDecodeResponsesAsUtf8() FAILED
    org.opentest4j.AssertionFailedError: expected: <Привет 中文> but was: <ÐŸÑ€Ð¸Ð²ÐµÑ‚ ä¸­æ–‡>
```

This is genuine windows-1252-over-UTF-8 mojibake (each UTF-8 byte reinterpreted as one windows-1252 code point), confirming the D-11/INFRA-06 assumption is real, not vacuous, under this project's `file.encoding=windows-1252` test JVM setting. All other 4 harness tests passed in this same run.

**GREEN** (commit `1adf5f6`, after adding `StandardCharsets.UTF_8` to both `InputStreamReader` call sites): `ClickHouseTestHarnessTest` reports `tests="5" failures="0"`.

`gsd_run check tdd-red-evidence` was not invoked for this RED evidence: that verb's classifier (`parseNodeTestSummary`/`tapFailedTestNames`) parses Node.js TAP output and does not understand Gradle/JUnit XML, so it is not applicable to this Java project. The plan's own explicit RED-capture instructions (run Order(5) before the fix, save the failure text) were followed instead and are reproduced above.

## Shared-Container Count vs. 01-01 Baseline

| Metric | 01-01 baseline (per-class container) | 01-02 (this plan, singleton) |
|---|---|---|
| `clickhouse/clickhouse-server:latest` containers per full run | 14 (one per `ClickHouseBase` subclass) | 6 (== `maxParallelForks`, one per test JVM fork) |
| `clickhouse/clickhouse-server:23.8` (proxy) containers | 2 | 2 (unchanged — each proxy class still owns its own `proxyDb`) |
| Full-suite wall-clock | `BUILD SUCCESSFUL in 4m 21s` (189 tests) | `BUILD SUCCESSFUL in 4m 6s` (194 tests) |
| Test count | 189 tests, 182 succeeded, 0 failed, 7 skipped | 194 tests, 187 succeeded, 0 failed, 7 skipped |

The 5 additional tests are `ClickHouseTestHarnessTest`'s new methods. Wall-clock time is essentially flat (slightly faster) despite 5 more tests and the same fork count, consistent with fewer container starts per fork.

## Ryuk Cleanup Result

Both the single-JVM multi-class run and the full-suite run left 1–2 Testcontainers containers (the shared `latest` container and/or the `testcontainers/ryuk` container) visible in `docker ps` in the instant after the Gradle process exited. A second `docker ps --filter label=org.testcontainers=true` check roughly 15–20 seconds later showed 0 containers in every case. Ryuk reliably reaps the shared container within its own client-disconnect timeout; **no JVM shutdown hook was added** (the plan's Claude's-Discretion escape hatch for this — 01-CONTEXT.md — was not needed).

## Decisions Made

- No `maxParallelForks` change (D-04): zero flakes observed across three separate full/partial suite runs at the existing `cores/2 = 6` fork count.
- Table creation added to the two new MBean leak-probe tests (Order 3/4): `ClickHouseWriter.start()` requires at least one table in the target database to build its table mapping, and the class database is empty by design at that point in the harness test (proven by Order(2)). This is a minimal, test-local addition — no main-code or upstream test file was touched.
- Ryuk (not a JVM shutdown hook) is the sole cleanup mechanism for the shared container, per Claude's Discretion in 01-CONTEXT.md, based on the observed ~15-20s reap delay across three separate runs in this environment.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added minimal table creation before starting a `ClickHouseSinkTask` in the two new MBean leak-probe tests**
- **Found during:** Task 2, first run of `ClickHouseTestHarnessTest` Order(3)/Order(4)
- **Issue:** Both tests failed with `RuntimeException: Connection to ClickHouse is not active.` — `ProxySinkTask`'s constructor throws when `ClickHouseWriter.start()` returns `false`, which it does when `updateMapping()` finds zero tables in the target database. The class database is intentionally empty at this point (Order(2) proves zero tables at class start), and the plan's action steps for Order(3)/Order(4) did not mention creating a table first.
- **Fix:** Added `ClickHouseTestHelpers.createTable(chc, "harness_mbean_probe_table", "CREATE TABLE IF NOT EXISTS %s (id UInt8) ENGINE = MergeTree ORDER BY id")` in Order(3), before `chst.start(props)`. The table persists in the class database (not dropped) so Order(4)'s independent `ClickHouseSinkTask` also finds it.
- **Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java`
- **Verification:** Both tests pass; `TEST-...ClickHouseTestHarnessTest.xml` reports `tests="5" failures="0"` after this fix (combined with the later D-11 GREEN fix).
- **Committed in:** `0fb6603` (Task 2, first commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Necessary for the new tests to exercise the code path the plan intended (start a real `ClickHouseSinkTask`); no main-code or upstream test file was touched. No scope creep.

## Issues Encountered

- `gsd_run check tdd-red-evidence` does not apply to this Java/Gradle project (it classifies Node.js TAP output). Followed the plan's own explicit RED-capture instructions instead — see "TDD RED/GREEN Evidence" above. Not a project defect; noted here for future TDD-task executors on this repo.
- Ryuk's cleanup lag (~15-20s after Gradle exits) means an immediate `docker ps` check after the build can show 1-2 leftover containers; a second check after a short wait is required to confirm no leak (as the plan's own verify commands anticipated with "on a second check after the Gradle build has exited").

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- The harness that Phases 2-4 will build ~10 new ClickHouse-backed test classes on is now in place: container singleton (fast startup), per-class database isolation (correct table-mapping assertions), `@Tag("clickhouse")` (already inherited, no per-class work needed), and the MBean safety net (protects Phase 4's JMX assertions from cross-test leaks).
- `uniqueConnectorName(TestInfo)` is available and ready for any new test that wants a distinct MBean connector name (e.g., Phase 4 JMX tests that need to distinguish multiple sink tasks); it is opt-in and does not affect any existing test's MBean name.
- `ClickHouseTestHelpers` decodes ClickHouse responses as UTF-8; Phase 3's `describeTable`/type tests can rely on this for non-ASCII assertions without additional charset handling. Main-code UTF-8 fixes (`ClickHouseHelperClient.java:370,417`) remain deferred to Phase 3 per D-11.
- No blockers. Plan 01-03 (JaCoCo, `testUnit`, fork coverage report) can proceed on this green baseline.

## Self-Check: PASSED

- FOUND: `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java`
- FOUND: `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java`
- FOUND: `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java`
- FOUND: `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java`
- FOUND: `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java`
- FOUND: `build/phase1-02-full-test.log`
- FOUND commit `5a903dd`, `0fb6603`, `2387d53`, `1adf5f6` in `git log --oneline --all`
- Re-ran acceptance criteria: `@Tag("clickhouse")` count = 1 (only in `ClickHouseBase.java`); `PullPolicy.alwaysPull()` count = 1; `StandardCharsets.UTF_8` count = 2; `TEST-...ClickHouseTestHarnessTest.xml` reports `tests="5" failures="0"`; `git log --grep='(01-02)' -- src/main` is empty (no main-code changes); `build/phase1-02-full-test.log` contains `Test result: SUCCESS`

---
*Phase: 01-test-infrastructure-green-baseline*
*Completed: 2026-09-16*
