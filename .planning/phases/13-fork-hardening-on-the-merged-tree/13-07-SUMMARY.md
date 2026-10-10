---
phase: 13-fork-hardening-on-the-merged-tree
plan: "07"
subsystem: testing
tags: [junit5, testcontainers, test-harness, javadoc, testfixtures]

requires:
  - phase: 11-full-suite-ci-publication
    provides: shared-container ClickHouseBase contract and the CI no-version change (IN-01)
  - phase: 12-upstream-sync-v1.7.0
    provides: CMP-01 setCompressionMethod and the retired D-17 pin
provides:
  - "ClickHouseBase.dropSharedDatabaseOrAttribute: a failed per-class database drop fails the leaking class"
  - "Docker-free ClickHouseBaseTearDownAttributionUnitTest"
  - "RowSnapshots.select: shared typed read-back helper in testFixtures"
  - "Four Javadocs corrected to describe the current code (DEBT-05)"
affects: [13-02, 13-03, 13-04, 13-06, 13-08, 15]

actuals:
  tokens: 5200
  tasks: 2
  commits: 2
plan_head_before: 4b8170eab7448195ec6df9201b6094e8724ce80a
plan_head_after: d239e699793288c5f48b51adcfe59082da301910

tech-stack:
  added: []
  patterns:
    - "Teardown failures are attributed to the leaking class with an IllegalStateException carrying class name, database name and the cause"
    - "Live tests read back through RowSnapshots.select (snapshot while the GenericRecord is current) and compare bytes via hex() in SQL"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBaseTearDownAttributionUnitTest.java
    - src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RowSnapshots.java
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseVersionDefaultUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java

key-decisions:
  - "dropSharedDatabaseOrAttribute is package-private static with a nested DatabaseDrop interface so the Docker-free test can drive it without a container"
  - "RowSnapshots lives in testFixtures so every src/test class can use it; existing private helpers in other classes are left untouched"

requirements-completed: [DEBT-04, DEBT-05]

coverage:
  - id: D1
    description: "A failed shared-container database drop in ClickHouseBase.tearDown() throws an IllegalStateException naming the test class and database with the cause preserved; a successful drop throws nothing"
    requirement: DEBT-04
    verification:
      - kind: unit
        ref: "ClickHouseBaseTearDownAttributionUnitTest#failingDropIsAttributedToTheLeakingClass"
        status: pass
      - kind: unit
        ref: "ClickHouseBaseTearDownAttributionUnitTest#succeedingDropThrowsNothing"
        status: pass
      - kind: integration
        ref: "ExplicitJsonInsertPathLiveTest (normal teardown through the shared container)"
        status: pass
    human_judgment: false
  - id: D2
    description: "RowSnapshots.select typed read-back helper available to all src/test classes through testFixtures"
    verification:
      - kind: other
        ref: "./gradlew compileTestFixturesJava compileTestJava"
        status: pass
    human_judgment: true
    rationale: "Helper has no consumer in this plan; its behaviour is exercised by the live tests of plans 13-02, 13-03, 13-04 and 13-06"
  - id: D3
    description: "Four stale Javadocs describe the current code with no assertion, method or DisplayName change"
    requirement: DEBT-05
    verification:
      - kind: other
        ref: "git diff of the DEBT-05 commit filtered for non-Javadoc lines prints nothing"
        status: pass
      - kind: unit
        ref: "ClickHouseVersionDefaultUnitTest, NetworkCompressionPinUnitTest, AsyncInsertLiveTest, NetworkCompressionPinLiveTest (20 tests, 0 failures)"
        status: pass
    human_judgment: true
    rationale: "Whether the prose is accurate is a documentation judgment; only the Javadoc-only diff and green tests are machine-checked"

duration: 12min
completed: 2026-10-08
status: complete
---

# Phase 13 Plan 07: Harness Debt (DEBT-04, DEBT-05) Summary

**A failed shared-container database drop in ClickHouseBase.tearDown() now fails the leaking class with an attributed IllegalStateException, plus a shared RowSnapshots read-back helper and four corrected Javadocs.**

## Performance

- **Duration:** 12 min
- **Started:** 2026-10-08T17:02:00Z (approximate)
- **Completed:** 2026-10-08T17:14:00Z
- **Tasks:** 2
- **Files modified:** 7 (2 created, 5 modified)

## Accomplishments

- DEBT-04: `ClickHouseBase.tearDown()` delegates the shared-container drop to `dropSharedDatabaseOrAttribute(getClass().getSimpleName(), database, ...)`. On any failure it logs at ERROR and throws `IllegalStateException` carrying the class simple name, the database name and the original exception as its cause. `copyServerLogs(ch)` still runs first, and a successful drop throws nothing.
- Docker-free `ClickHouseBaseTearDownAttributionUnitTest` (2 tests, in `testUnit`) pins both paths.
- `RowSnapshots.select(chc, sql, columns...)` added to testFixtures for the phase's new live tests.
- DEBT-05: four class Javadocs rewritten (CI sets no version; the async race is cited, not tested; the NetworkCompressionPin pair describe post-CMP-01 code and Recorded Facts 25 and 26). Diff is Javadoc-only.

## Task Commits

1. **Task 1: DEBT-04 attributed tearDown drop failure plus RowSnapshots** - `281a6c9e` (test)
2. **Task 2: DEBT-05 four stale Javadocs** - `d239e699` (docs)

**Plan metadata:** recorded in the following docs commit (docs: complete plan).

## TDD Gate Compliance

Task 1 carried `tdd="true"` inside an `execute`-type plan, and the plan specifies a single commit for it.

- **RED:** `ClickHouseBaseTearDownAttributionUnitTest` was written first. `./gradlew compileTestJava` failed with two `error: cannot find symbol ... method dropSharedDatabaseOrAttribute(String,String,...)` errors at the two call sites in the new test; this is the plan-sanctioned compile-failure RED. Semantic assessment: the failure is exactly the missing production symbol under test, not a syntax or fixture fault.
- **GREEN:** adding the method to `ClickHouseBase` made both tests pass (2 tests, 0 failures, 0 errors).
- **REFACTOR:** none.
- The RED state was not committed separately because a non-compiling test commit would break the tree and the plan prescribes one `test(13-07)` commit. No `feat(13-07)` commit exists for the same reason.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` - `DatabaseDrop`, `DROP_FAILURE_MESSAGE_FORMAT`, `dropSharedDatabaseOrAttribute`, and the tearDown call site
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBaseTearDownAttributionUnitTest.java` - new Docker-free DEBT-04 pin
- `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RowSnapshots.java` - new shared read-back helper
- `ClickHouseVersionDefaultUnitTest.java`, `AsyncInsertLiveTest.java`, `NetworkCompressionPinUnitTest.java`, `NetworkCompressionPinLiveTest.java` - Javadoc text only

## Behaviour ledger

- **L-19: DEBT-04, DEBT-05.** DEBT-04 makes a shared-container database drop failure fail its own class (test harness only; no production code touched), so FORK-DELTA "Known costs and open items" item 2 can be closed in Phase 15. A flaky drop now fails an otherwise green class by design (D-14); Phase 15's two clean full-suite runs will show whether any drop is flaky in practice. DEBT-05 has no behaviour change: the diff of the DEBT-05 commit contains only Javadoc lines.

## Decisions Made

- The attribution seam is a package-private static method taking a `DatabaseDrop` action, so the unit test needs no container and no subclass of `ClickHouseBase`.
- Existing private `allRows` helpers in other live tests are left in place (additive change; rebasability).

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. `spotless` in this repo only does import order and unused-import removal, so no tree-wide `spotlessApply` was run; imports in the new files were written in sorted order.

## Verification Results

- `compileTestFixturesJava compileTestJava test --rerun --tests '*ClickHouseBaseTearDownAttributionUnitTest' --tests '*ExplicitJsonInsertPathLiveTest'`: 9 tests, 0 failed (attribution test 2/0/0, live class 7/0/0).
- `testUnit`: 406 tests, 404 succeeded, 0 failed, 2 skipped (pre-existing named skips), run after each commit.
- Four DEBT-05 classes: 20 tests, 0 failed.
- Invariant script after each commit: `result=PASS`.
- Anchors in `ClickHouseBase.java`: `@Tag("clickhouse")` 1, `sharedContainerStarts()` 2; old `LOGGER.error("Error dropping database", e);` count 0; `copyServerLogs(ch)` (line 141) precedes the `dropSharedDatabaseOrAttribute(getClass()` call (line 146).

## Known Stubs

None.

## Threat Flags

None. T-13-16 (repudiation of a swallowed drop failure) is mitigated and pinned Docker-free; T-13-17 accepted (message carries only a test class name and a generated database name).

## Next Phase Readiness

`RowSnapshots` is ready for plans 13-02, 13-03, 13-04 and 13-06. No blockers.

## Self-Check: PASSED

- FOUND: ClickHouseBaseTearDownAttributionUnitTest.java, RowSnapshots.java, ClickHouseBase.java
- FOUND commits: `281a6c9e`, `d239e699` (both ancestors of HEAD)

---
*Phase: 13-fork-hardening-on-the-merged-tree*
*Completed: 2026-10-08*
