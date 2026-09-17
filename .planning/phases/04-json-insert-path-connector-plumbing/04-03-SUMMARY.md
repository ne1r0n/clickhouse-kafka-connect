---
phase: 04-json-insert-path-connector-plumbing
plan: 03
subsystem: testing
tags: [junit5, testcontainers, clickhouse-latest, client-v2, jmx, async-insert]

# Dependency graph
requires:
  - phase: 04-json-insert-path-connector-plumbing (plans 01, 02)
    provides: "Typed SQL read-back convention (allRows/GenericRecord aliasing helper), try/finally + table-before-start() lifecycle idiom, uniqueConnectorName(TestInfo) usage, blob-hash-pin pattern for untouched main-code files"
  - phase: 01-test-infrastructure-green-baseline
    provides: "ClickHouseBase's cleanupLeakedMBeans @AfterEach sweep and its queryNames idiom, which this plan's registeredFor helper reuses against the same platform MBean server"
provides:
  - "ProxySinkTask.buildMBeanName(String, int, String) - the phase's one pre-approved main-code extraction, byte-for-byte identical rendered output"
  - "MBeanNameUnitTest - Docker-free pins for JMX-01, JMX-02, JMX-03"
  - "MBeanRegistrationLiveTest - ClickHouse-backed pins for JMX-04 (single-task baseline and four-task, two-connector distinctness/selective-unregistration)"
  - "AsyncInsertSettingsUnitTest - Docker-free pins for ASYNC-01, ASYNC-02"
  - "AsyncInsertLiveTest - ClickHouse-backed pin for ASYNC-03"
affects: [05-ci-and-final-verification]

# Actuals (#2632)
actuals:
  tokens: 6166
  tasks: 3
  commits: 3
plan_head_before: 09a02d4ee98278c4e2123292ae5136f22cf6a3f2

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Package-private static extraction with byte-for-byte-preservation Javadoc (D-02), mirroring ClickHouseHelperClient.buildTableFilterClause - the phase's only pre-approved main-code change."
    - "MBean identity via before/after Set<ObjectName> difference, never an absolute SinkTask<id> literal, because the static NEXT_ID counter never resets inside a test JVM fork (D-04)."
    - "Docker-free ClickHouseSinkConfig unit test constructing the POJO directly from a minimal Map<String,String> (HOSTNAME/PORT/DATABASE/USERNAME/PASSWORD) and asserting on getClickhouseSettings() - extends nothing, stays in testUnit (D-16)."
    - "Per-task props map for a multi-task live test must be a copy of the ALREADY-createClient()-mutated base map, not a fresh createProps() call - createProps() alone always points at the 'default' database, and only createClient() swaps in the per-class test database."

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/MBeanRegistrationLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertSettingsUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertLiveTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java

key-decisions:
  - "buildMBeanName extracted exactly per D-02: package-private static, holding today's null/colon logic and format template verbatim; getMBeanName() reduced to a one-line delegate. The format template appears exactly once in ProxySinkTask.java, verified by a source gate. No other production file was touched anywhere in this plan or this phase - MBeanServerUtils.java, ClickHouseSinkConfig.java, ClickHouseWriter.java and build.gradle.kts blob hashes are unchanged at every checkpoint."
  - "Self-caught test-harness bug (Rule 1, own test code, not production): the first JMX-04 four-task test run failed on the second task's start() with 'Connection to ClickHouse is not active.' The cause was building each per-task props map from a fresh createProps() call, which always points at the 'default' database (no fixture table there) - only createClient()'s mutation of its argument map swaps in the per-class test database. Fixed by copying the already-mutated base map (new HashMap<>(baseProps)) per task instead. Caught and fixed before any commit; not present in the committed source."
  - "A Javadoc literal 'SinkTask0' (used descriptively, not as an assertion) tripped the plan's own D-04 source gate for absolute SinkTask<n> literals. Reworded to 'SinkTask<N>' before the Task 2 commit - the gate exists to prevent exactly this kind of accidental absolute-name creep, even in comments."

requirements-completed: [JMX-01, JMX-02, JMX-03, JMX-04, ASYNC-01, ASYNC-02, ASYNC-03]

coverage:
  - id: D1
    description: "buildMBeanName extracted from getMBeanName byte-for-byte identical; JMX-01's connector-present case pinned by whole-string equality, which also pins the ObjectName key order"
    requirement: "JMX-01"
    verification:
      - kind: unit
        ref: "MBeanNameUnitTest#connectorNamePresentRendersFullMBeanName"
        status: pass
    human_judgment: false
  - id: D2
    description: "A null connector name renders the literal connector=null; an empty connector name renders connector= immediately followed by name= - absent and empty are pinned as producing distinct text"
    requirement: "JMX-02"
    verification:
      - kind: unit
        ref: "MBeanNameUnitTest#nullConnectorNameRendersLiteralConnectorNull, #emptyConnectorNameRendersEmptyConnectorValue"
        status: pass
    human_judgment: false
  - id: D3
    description: "A null version renders version=unknown; every colon in a version is replaced by an underscore (asserted via endsWith, since the ObjectName domain separator is itself a colon)"
    requirement: "JMX-03"
    verification:
      - kind: unit
        ref: "MBeanNameUnitTest#nullVersionRendersVersionUnknown, #colonInVersionIsReplacedByUnderscore"
        status: pass
    human_judgment: false
  - id: D4
    description: "One started task registers exactly one MBean and stop() unregisters it, with a second stop() proven idempotent; four tasks across two connector names register four distinct MBeans (identified via before/after set difference, never an absolute name) and stopping one unregisters exactly that one while the other three survive"
    requirement: "JMX-04"
    verification:
      - kind: integration
        ref: "MBeanRegistrationLiveTest#startedTaskRegistersAndStopUnregistersExactlyOne, #fourTasksAcrossTwoConnectorsRegisterDistinctMBeansAndSelectiveStopUnregistersOne"
        status: pass
    human_judgment: false
  - id: D5
    description: "A default ClickHouseSinkConfig sends no async_insert setting while still sending the three other forced settings (positive control, so the negative assertion certifies something); user-supplied async_insert=1/wait_for_async_insert=1 and async_insert=0 both round-trip verbatim"
    requirement: "ASYNC-01, ASYNC-02"
    verification:
      - kind: unit
        ref: "AsyncInsertSettingsUnitTest#defaultConfigHasNoAsyncInsertButHasTheOtherForcedSettings, #userSuppliedAsyncInsertAndWaitForAsyncInsertRoundTripToOne, #userSuppliedAsyncInsertZeroRoundTripsToZero"
        status: pass
    human_judgment: false
  - id: D6
    description: "A real sink insert with async_insert=1,wait_for_async_insert=1 completes and all five rows land with correct id/str values immediately after put(), asserted by typed read-back with no polling loop"
    requirement: "ASYNC-03"
    verification:
      - kind: integration
        ref: "AsyncInsertLiveTest#asyncInsertWithWaitForAsyncInsertCompletesAndDataIsPresent"
        status: pass
    human_judgment: false

duration: ~45 min (single session)
completed: 2026-09-17
status: complete
---

# Phase 4 Plan 3: JMX Naming, Registration, and async_insert Summary

**Extracted `ProxySinkTask.buildMBeanName` byte-for-byte (the phase's one pre-approved main-code change) and pinned every JMX MBean naming/registration branch (JMX-01..04) plus the fork's `async_insert` pass-through (ASYNC-01..03) in four new fork-only test classes, closing out Phase 4's third and final plan.**

## Performance

- **Duration:** ~45 min (single session)
- **Started:** 2026-09-17
- **Completed:** 2026-09-17
- **Tasks:** 3 / 3 complete
- **Files modified:** 5 (4 created, 1 edited)

## Accomplishments

- `ProxySinkTask.buildMBeanName(String connectorName, int id, String version)` (new, package-private static): the phase's only pre-approved production change (D-02). `getMBeanName()` is now a one-line delegate. The format template appears exactly once in the file; a source gate and a whole-string unit assertion both pin the rendered name as byte-for-byte identical to the pre-extraction inline logic.
- `MBeanNameUnitTest` (new, `com.clickhouse.kafka.connect.sink`, Docker-free, extends nothing): 6 tests pinning JMX-01 (connector present), JMX-02 (absent vs. empty connector name), and JMX-03 (null version, colon-in-version substitution via `endsWith`).
- `MBeanRegistrationLiveTest` (new, `com.clickhouse.kafka.connect.sink`, extends `ClickHouseBase`): 2 tests pinning JMX-04 - a single-task registration/unregistration/idempotent-second-stop baseline, and a four-task, two-connector-name test that identifies each task's MBean via before/after `Set<ObjectName>` difference (never an absolute `SinkTask<id>` name, per D-04) and proves selective unregistration.
- `AsyncInsertSettingsUnitTest` (new, `com.clickhouse.kafka.connect.sink`, Docker-free, extends nothing): 3 tests pinning ASYNC-01 (no `async_insert` key by default, with a positive control on the three other forced settings) and ASYNC-02 (`async_insert=1`/`wait_for_async_insert=1` and `async_insert=0` both round-trip verbatim).
- `AsyncInsertLiveTest` (new, `com.clickhouse.kafka.connect.sink`, extends `ClickHouseBase`): 1 test pinning ASYNC-03 - a real 5-row insert with `async_insert=1,wait_for_async_insert=1` lands every row with correct values immediately after `put()`, no polling loop.
- `git status --porcelain -- src/main/` is empty for the whole plan (all changes already committed); `MBeanServerUtils.java`, `ClickHouseSinkConfig.java`, `ClickHouseWriter.java`, and `build.gradle.kts` blob hashes are unchanged at every task's verify gate.
- Full `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun` is green: 210 tests, 208 passed, 2 skipped (pre-existing).
- Full `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun` is green: 371 tests, 364 passed, 7 skipped (pre-existing).

## Task Commits

Each task was committed atomically:

1. **Task 1: End-to-end MBean slice — extract buildMBeanName, JMX-01, JMX-04 baseline** - `7c60603` (test)
2. **Task 2: Expand JMX coverage — JMX-02, JMX-03, JMX-04 four-task distinctness** - `2513996` (test)
3. **Task 3: async_insert left to the user — ASYNC-01, ASYNC-02, ASYNC-03** - `721682c` (test)

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` - `buildMBeanName` extracted; `getMBeanName()` reduced to a delegate. Only production-code change in the entire phase.
- `src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java` - New Docker-free class; 6 tests for JMX-01/02/03
- `src/test/java/com/clickhouse/kafka/connect/sink/MBeanRegistrationLiveTest.java` - New ClickHouse-backed class; 2 tests for JMX-04
- `src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertSettingsUnitTest.java` - New Docker-free class; 3 tests for ASYNC-01/02
- `src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertLiveTest.java` - New ClickHouse-backed class; 1 test for ASYNC-03

## Decisions Made

- **The one pre-approved main-code change made exactly as scoped (D-02).** `buildMBeanName` holds precisely the previous null/colon logic and format string; nothing else in `ProxySinkTask` changed (no overload, no test accessor for `id`/`statistics`, no `stop()` change, no import change). Verified by a source gate (format template count == 1) plus blob-hash pins on the four other main-code files this plan's `must_haves` named as untouchable.
- **JMX-04's four-task test identifies MBeans by set difference, never by absolute name**, per the plan's `assumption_delta_decision`: the full `ObjectName` (connector + name keys together) is the identity, and the connector name alone is only a filter predicate. This was necessary because `ProxySinkTask.id` comes from a process-global `AtomicInteger` that never resets inside a test JVM fork.
- **D-05 (ObjectName-hostile connector names) deliberately left uncovered**, recorded as an inline comment in `MBeanNameUnitTest` naming `MalformedObjectNameException` and the silent-degradation risk it causes - consistent with `04-CONTEXT.md`'s Deferred Ideas.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug in own new test code] JMX-04 four-task test's per-task props pointed at the wrong database**
- **Found during:** Task 2 (first run of the four-task `MBeanRegistrationLiveTest` test)
- **Issue:** Each per-task props map was built from a fresh `createProps()` call, which always sets `DATABASE` to `"default"`. Only `createClient()`'s in-place mutation of its argument map swaps in the per-class test database that actually holds the fixture table. The second task to `start()` (and every task after it) found no tables in `"default"` and `ClickHouseWriter.start()` returned `false`, throwing `RuntimeException("Connection to ClickHouse is not active.")`.
- **Fix:** Build one `baseProps` map, pass it through `createClient()` once (which mutates it in place), then derive each per-task props map as `new HashMap<>(baseProps)` with only `"name"` overridden.
- **Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/MBeanRegistrationLiveTest.java`
- **Verification:** Re-ran `./gradlew test --tests MBeanRegistrationLiveTest` after the fix; both tests passed.
- **Committed in:** `2513996` (Task 2 commit) - the bug was caught and fixed before any commit existed for this test method, so no separate fix commit was needed.

**2. [Rule 1 - Bug in own new test code] Javadoc literal tripped the plan's own D-04 source gate**
- **Found during:** Task 2 (running the plan's `absolute_sinktask_literals` verify gate)
- **Issue:** A Javadoc sentence describing why absolute names are unassertable used the literal text `SinkTask0` as an illustrative example, which the gate's `SinkTask[0-9]` regex flagged - correctly, since the gate exists precisely to prevent any accidental absolute-name literal, including in prose.
- **Fix:** Reworded to `SinkTask<N>`.
- **Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/MBeanRegistrationLiveTest.java`
- **Verification:** Re-ran the plan's `absolute_sinktask_literals` Python assertion; empty list, passed.
- **Committed in:** `2513996` (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (both Rule 1, both self-introduced bugs in this plan's own new test code, caught before any commit - neither is a production bug and neither required a separate fix commit).
**Impact on plan:** No scope creep. Both fixes were necessary for the tests to correctly exercise the behavior they claim to pin; neither touched production code.

## Issues Encountered

None beyond the two self-caught test-authoring issues documented above under Deviations. Docker (colima `vz` profile) was reachable throughout; all live and Docker-free verification commands passed after the fixes.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- JMX-01 through JMX-04 and ASYNC-01 through ASYNC-03 are fully pinned; MBean naming/registration and the fork's `async_insert` pass-through now have automated regression coverage against ClickHouse `latest` / client V2.
- Phase 4 is now complete: all three plans (04-01 explicit JSON insert path, 04-02 `jsonInsertColumns`, 04-03 JMX + `async_insert`) are done, and the full `./gradlew test` suite is green with `src/main/` touched only by this plan's one pre-approved `ProxySinkTask.java` extraction.
- No blockers for Phase 5 (CI and final verification) - this plan's tests, like 04-01 and 04-02's, run entirely within the existing `test`/`testUnit` tasks with no new Gradle configuration.

## Self-Check: PASSED

All created files (`MBeanNameUnitTest.java`, `MBeanRegistrationLiveTest.java`, `AsyncInsertSettingsUnitTest.java`, `AsyncInsertLiveTest.java`, this SUMMARY.md) confirmed present on disk, and all three task commits (`7c60603`, `2513996`, `721682c`) confirmed present in `git log --oneline --all`. All plan-level `<verification>` commands re-run successfully: `testUnit --rerun` (210/208/0/2), `test --rerun` (371/364/0/7), `git status --porcelain -- src/main/` empty, and the four untouched main-code files' blob hashes match their pre-plan values.

---
*Phase: 04-json-insert-path-connector-plumbing*
*Completed: 2026-09-17*
