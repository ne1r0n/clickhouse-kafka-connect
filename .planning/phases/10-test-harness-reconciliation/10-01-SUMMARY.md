---
phase: 10-test-harness-reconciliation
plan: 01
subsystem: testing
tags: [testcontainers, junit5, jmx, gradle, clickhouse-kafka-connect, harness]

requires:
  - phase: 06-08-orchestration-jmx-re-plumbing
    provides: getBaseProps()/ClickHouseTestHelpers.createClient() as the post-merge API 12 fork test files already migrated onto
  - phase: 09-metadata-free-json-path-keep-or-drop
    provides: the Docker-free-canary-for-a-silently-vanishing-behaviour pattern (MetadataFreeJsonInsertUnitTest) reused here for D-04
provides:
  - One shared ClickHouse container per test JVM fork behind an opt-out seam (D-01/D-02), replacing upstream's per-class @TestInstance(PER_CLASS) container lifecycle
  - tearDown() drops the class database instead of stopping the shared container; the opt-out branch still stops its own container
  - The @AfterEach MBean leak sweep restored verbatim, now proven to cover upstream's per-topic MBeans (D-08/D-09a, HARN-02)
  - CLICKHOUSE_VERSION_DEFAULT restored to the floating "latest" tag and pinned by a Docker-free canary reachable from testUnit (D-04, the seventh silently-vanishing fork behaviour)
  - ClickHouseTestHarnessTest un-excluded from compileTestJava, adapted to the post-merge API, and extended with two new ordered pins
affects: [10-02-full-suite-green-gate, 10-03-checklist-and-requirements-closeout, phase-11-publication]

actuals:
  tokens: 7884
  tasks: 3
  commits: 6

tech-stack:
  added: []
  patterns:
    - "Lazy static shared container guarded by synchronized(ClickHouseBase.class), layered on top of JUnit 5's @TestInstance(PER_CLASS) instance lifecycle (BeforeAll stays an instance method; the container field is static)"
    - "Opt-out seam via an overridable protected boolean usesSharedContainer() hook, not a JUnit @Tag"
    - "Docker-free canary for a silently-vanishing fork behaviour, extending nothing so @Tag(\"clickhouse\") never gets inherited"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseHarnessOptOutContainerTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseVersionDefaultUnitTest.java
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java
    - src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java
    - build.gradle.kts

key-decisions:
  - "Restored CLICKHOUSE_VERSION_DEFAULT to the floating \"latest\" tag (D-04): the v1.6.0 merge had silently reverted it to a pinned LTS version with no conflict marker and no compile error, so Phases 8 and 9's green closes ran against the wrong server. Verified live: the container this plan starts reports image clickhouse/clickhouse-server:latest."
  - "Shared-container strategy re-derived over upstream's per-class @TestInstance(PER_CLASS) rewrite rather than accepting HARN-03's fallback (D-01), with an explicit opt-out seam (D-02) so the two Toxiproxy classes never collide on the shared network alias."
  - "tearDown()'s database drop is load-bearing under sharing, not tidiness (D-01): it is now the only isolation mechanism between classes sharing a fork, pinned by classGetsFreshIsolatedDatabase's zero-other-databases assertion."
  - "MBean sweep restored WARN-unregister-continue, never fail-the-test (D-09a): failing the test would immediately red two pre-existing ClickHouseSinkTaskJsonInsertFormatTest methods (P4-WR-01, an accepted out-of-scope follow-up)."
  - "The sweep's existing ',*' wildcard pattern was left unwidened and proven sufficient by a leak-then-assert pair (D-08) rather than changed speculatively — confirmed live: the per-topic MBean was matched and swept without any pattern change."

requirements-completed: [HARN-02, HARN-03, HARN-07]

coverage:
  - id: D1
    description: "One shared ClickHouse container per test JVM fork with an opt-out seam that leaves the shared-start counter untouched under every fork assignment (HARN-03)"
    requirement: "HARN-03"
    verification:
      - kind: integration
        ref: "ClickHouseTestHarnessTest#sharedContainerIsStartedOncePerJvmAndIsTheRequestedImage"
        status: pass
      - kind: integration
        ref: "ClickHouseHarnessOptOutContainerTest#optOutClassGetsItsOwnContainerAndDoesNotMoveTheSharedCounter"
        status: pass
      - kind: integration
        ref: "ClickHouseSinkTaskSchemalessProxyTest (10 tests) + ClickHouseSinkTaskWithSchemaProxyTest (15 tests), opted out"
        status: pass
    human_judgment: false
  - id: D2
    description: "tearDown() drops the class database instead of stopping the shared container; the opt-out branch still stops its own container"
    verification:
      - kind: integration
        ref: "ClickHouseTestHarnessTest#classGetsFreshIsolatedDatabase"
        status: pass
    human_judgment: false
  - id: D3
    description: "@AfterEach MBean leak sweep restored verbatim and proven to cover upstream's per-topic MBeans (HARN-02)"
    requirement: "HARN-02"
    verification:
      - kind: integration
        ref: "ClickHouseTestHarnessTest#testFailingBetweenStartAndStopLeaksAnMBean + #leakedMBeanWasSweptAndUniqueNamesAreSafe"
        status: pass
      - kind: integration
        ref: "ClickHouseTestHarnessTest#perTopicMBeanLeakedByAFailingTest + #perTopicMBeanWasSwept"
        status: pass
    human_judgment: false
  - id: D4
    description: "CLICKHOUSE_VERSION_DEFAULT resolves to the floating latest tag, pinned by a test inside the Docker-free testUnit gate (D-04, HARN-07)"
    requirement: "HARN-07"
    verification:
      - kind: unit
        ref: "ClickHouseVersionDefaultUnitTest#clickhouseVersionDefaultIsTheFloatingLatestTag (testUnit, Docker stopped)"
        status: pass
    human_judgment: false
  - id: D5
    description: "ClickHouseTestHarnessTest compiles, is no longer excluded from build.gradle.kts's test source set, and runs green live end to end"
    verification:
      - kind: integration
        ref: "compileTestJava + compileTestFixturesJava producing ClickHouseTestHarnessTest.class; live run 7/7 tests, 0 failures, 0 errors, 0 skipped"
        status: pass
    human_judgment: false

duration: 16min
completed: 2026-09-20
status: complete
---

# Phase 10 Plan 01: Test Harness Reconciliation Summary

**Re-derived `ClickHouseBase`'s shared-container-per-fork contract over upstream's per-class rewrite, restored the `@AfterEach` MBean sweep with a proven per-topic extension, and fixed a silent `latest`→pinned-LTS version regression that had every local gate since the v1.6.0 merge testing the wrong ClickHouse server.**

## Performance

- **Duration:** 16 min
- **Started:** 2026-09-20T14:12:28Z
- **Completed:** 2026-09-20T14:28:22Z
- **Tasks:** 3
- **Files modified:** 8 (2 created, 6 modified)

## Accomplishments

- `ClickHouseBase` now starts one shared ClickHouse container per test JVM fork (a `synchronized` lazy-start guard, `SHARED_CONTAINER_STARTS` incremented at exactly one site), behind an overridable `usesSharedContainer()` opt-out hook that the two Toxiproxy classes use to avoid a network-alias collision.
- `tearDown()` drops the class's own database instead of stopping the shared container when sharing; the opt-out branch still stops its own container, keeping both Toxiproxy classes' `super.tearDown()` call working.
- `@AfterEach cleanupLeakedMBeans()` is restored verbatim (WARN, unregister, continue — never fails the test) and is now proven, by a leak-then-assert pair driven through a real insert, to already cover upstream's per-topic MBean names without any pattern change.
- `CLICKHOUSE_VERSION_DEFAULT` is restored to the floating `latest` tag via a new `CLICKHOUSE_VERSION_LATEST` constant, and pinned by a Docker-free `testUnit`-reachable canary — the only gate that could have caught this, since CI pins the version explicitly.
- `PullPolicy.alwaysPull()` re-applies, but only when the resolved version is the floating tag, so a deliberately pinned `CLICKHOUSE_VERSION` is not re-pulled on every run.
- `ClickHouseTestHarnessTest` is un-excluded from `compileTestJava`, redirected onto the post-merge API (`getBaseProps()`, `ClickHouseTestHelpers.createClient`, a local `createTable` helper, `executeQueryIgnoreResult`), and extended with two new `@Order(6)`/`@Order(7)` methods proving the per-topic MBean sweep — 7 tests, all green live.

## Task Commits

Each task was committed atomically (task 1 split into three behaviour-scoped commits per D-11):

1. **Task 1, commit A: `latest` image default + floating-tag pull policy** - `872d1197` (feat)
2. **Task 1, commit B: shared container, opt-out seam, database drop, MBean sweep** - `fb09f823` (feat)
3. **Task 1, commit C: canary API redirection + build exclusion removal** - `b39ed8fd` (feat)
4. **Task 2: opt-out variant for both Toxiproxy classes + counter pin** - `28b14b0a` (feat)
5. **Task 3, commit A: Docker-free `CLICKHOUSE_VERSION_DEFAULT` canary** - `afcd24b5` (test)
6. **Task 3, commit B: per-topic MBean sweep pin** - `07153629` (test)

_Note: TDD is not applicable to this plan._

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` - shared container + opt-out seam + counter, drop-not-stop `tearDown`, class-name-keyed server log copy, `@AfterEach` MBean sweep, conditional `alwaysPull()`
- `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` - `CLICKHOUSE_VERSION_LATEST` constant; `CLICKHOUSE_VERSION_DEFAULT` now defined in terms of it
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java` - API redirection off the removed `createProps()`/unqualified `createClient()`, local `createTable` helper, two new `@Order(6)`/`@Order(7)` per-topic MBean pins, `mbeanNamesForConnector` extraction
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java` - `usesSharedContainer()` override only
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java` - `usesSharedContainer()` override only
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseHarnessOptOutContainerTest.java` - new fork-only class pinning the counter rule under every fork assignment
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseVersionDefaultUnitTest.java` - new Docker-free canary pinning the `latest` default
- `build.gradle.kts` - removed the `exclude("**/ClickHouseTestHarnessTest.java")` line and its surrounding TEMPORARY comment block

## Decisions Made

See `key-decisions` in frontmatter. All decisions were locked in `10-CONTEXT.md` (D-01 through D-11) prior to execution; this plan applied them without deviation.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Discovered `-PspotlessFiles` does not scope `spotlessApply`/`spotlessJavaCheck` in this build**
- **Found during:** Task 1, first commit attempt
- **Issue:** `./gradlew spotlessApply -PspotlessFiles=<paths>` silently reformatted the entire tree (52 files) instead of only the touched files, reproducing the exact "unrelated 52-file reformat" hazard Phase 7's D-19/D-15 precedent warns against. `spotlessJavaCheck` with the same property also evaluated the whole tree and failed on 45 pre-existing violations unrelated to this plan.
- **Fix:** Reverted every file the mis-scoped `spotlessApply` touched except the two files this plan intentionally edits (`git checkout --` on each unrelated path), keeping only the intended diff. For all subsequent tasks, formatted new/edited code by hand to match the established single-group-alphabetical import-order convention (verified by reading the post-`spotlessApply` diff from the first attempt) instead of invoking `spotlessApply` again.
- **Files modified:** none beyond the plan's own scope (the revert restored 45 files to their pre-existing committed state; nothing from that mis-scoped run was kept)
- **Verification:** `git diff --name-only 3f384b93..HEAD` shows only the 8 files this plan's `files_modified` list and task descriptions name, plus `.planning/milestone.lock` (see deviation 2)
- **Committed in:** not committed — reverted before any commit

**2. [Rule 1 - Bug] A pre-existing staged `.planning/milestone.lock` was swept into the first task commit**
- **Found during:** Task 1, first commit, post-commit review
- **Issue:** `.planning/milestone.lock` was already staged (`git status` showed it as `A` before this plan began, from an orchestrator step prior to dispatch) — staging only the two files this plan intended to commit still carried that pre-staged file into the commit, since `git commit` commits the full index, not just newly-`git add`ed paths.
- **Fix:** Left it in place — it is a harmless GSD-internal session lock (phase/session id, pid, timestamp), not a code change, and re-splitting the commit was judged lower-value than a documented note. All subsequent commits were checked with `git status --short` before committing to confirm no other pre-staged files were present.
- **Files modified:** `.planning/milestone.lock` (bundled into commit `872d1197`, unintentionally)
- **Verification:** `git show 872d1197 --stat` confirms it is the only non-plan file in that commit; no further occurrences in later commits
- **Committed in:** `872d1197` (part of task 1 commit A)

---

**Total deviations:** 2 auto-fixed (1 blocking/tooling, 1 bug/process). **Impact on plan:** Neither touched production code or plan-scoped test files. The spotless discovery is a repo-wide tooling caveat worth carrying forward to plans 10-02/10-03 — do not run tree-wide `spotlessApply` or `spotlessJavaCheck` in this repo; hand-format new/edited Java against the existing import-order convention instead, exactly as Phase 7's own precedent already established.

## Issues Encountered

None beyond the deviations above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- All three of this plan's tasks are green: task 1's canary (5/5 live), task 2's opt-out trio (26/26 live), task 3's `testUnit` (327 tests, 0 failures, 0 errors, 2 pre-existing skips, 29 classes ≥ the 29-class floor) and live canary (7/7).
- `ClickHouseBase` now has exactly one shared-start increment site, one `ch.stop()` site, and one `dropDatabase` site — confirmed by the plan's own shape-check script.
- No `src/main` file was touched — confirmed via `git diff --name-only` against the pre-plan tip.
- Live container reported image `clickhouse/clickhouse-server:latest`, confirming D-04's fix took effect end to end, not just in the constant.
- `sharedContainerStarts()` observed as `1` in every live run in this plan (single-fork local execution); the two-consecutive-clean-full-suite gate (D-09) and the 17 `MERGE-CHECKLIST.md` row flips are explicitly out of this plan's scope per `10-CONTEXT.md` and belong to plans 10-02/10-03.
- Wall-clock note for Phase 11's `timeout-minutes: 45` CI measurement: this plan's three live Gradle invocations (5-test canary, 26-test opt-out trio, 7-test per-topic pin) measured 30s, 50s, and 16s respectively on this machine — not a full-suite number, but consistent with the shared-container decision (D-01) keeping per-class container-start overhead out of the hot path.

---
*Phase: 10-test-harness-reconciliation*
*Completed: 2026-09-20*
