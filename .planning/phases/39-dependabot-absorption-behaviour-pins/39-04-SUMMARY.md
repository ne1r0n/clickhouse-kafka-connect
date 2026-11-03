---
phase: 39-dependabot-absorption-behaviour-pins
plan: "04"
subsystem: testing
tags: [integration-harness, datagen, confluent-platform, leftover-connector-pin, red-first, fork-delta, phase-gate, merge-invariants]

# Dependency graph
requires:
  - phase: 39-dependabot-absorption-behaviour-pins
    provides: "plans 39-01 to 39-03: both Dependabot merges, the PIN-02 matrix and the PIN-03 tripwire, E-01..E-07, a testUnit baseline of 1687 tests"
provides:
  - "ConfluentPlatform counts finished Datagen tasks only from /connectors/{name}/status of the connector the harness started; the all-connectors accessor is gone"
  - "datagenCompletionCountIgnoresLeftoverConnectorTest: two finished Datagen connectors kept present, each scoped count asserted equal to its own task total; seen red against a cross-connector count, then green"
  - "One recorded integrationTest run of ClickHouseSinkConnectorIntegrationTest on ClickHouse latest x V2: 55 tests, 0 failures, 0 errors, 0 skipped, 8 generateData lines all N of N"
  - "FORK-DELTA Known cost 15 rewritten as closed"
  - "Phase 39 gate recorded: testUnit 1687 (Docker unreachable), full suite 2285 with 18 named skips, cumulative merge-invariant gate PASS, empty src/main diff, origin unmoved; E-08..E-11, index E-01..E-11 all pass"
affects: [40-ci-and-test-hygiene, 41-timeout-classification, 42-retry-budget, 44-release-rehearsal]

requires_next:
  - "Phase 39 is complete: verify the phase, then continue with the next roadmap phase. Known costs item 15 is closed; items 12 to 14 and 16 to 23 stay open"

# Actuals (#2632): chars/4 over the realized diff (STATE.md excluded, evidence file included)
actuals:
  tokens: 11000
  tasks: 3
  commits: 4
plan_head_before: 46459dc54f2cc93706ec7bc3753cf8547cf8f688
plan_head_after: e4d3f6b92eac26126c17a26abdde53b304bd453f

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Scoped completion counting: one status read per connector, one count per task, a precompiled marker Pattern, a less-than exit condition and named poll constants"
    - "Red-first against a scratch regression: the discriminating variant is applied uncommitted, its patch and SHA-256 are recorded, then it is reverted before the green run"
    - "Completion of the connectors a pin keeps is awaited on task state (awaitAllTasksFailed), independent of the count under test"

key-files:
  created: []
  modified:
    - src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java
    - src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConnectorIntegrationTest.java
    - docs/FORK-DELTA.md
    - .planning/phases/39-dependabot-absorption-behaviour-pins/39-COMMAND-EVIDENCE.md

key-decisions:
  - "The F-3 deliverable is the leftover-connector pin, the removal of getConnectors() and the IN-06 hygiene, not a change to the counting key: at HEAD generateData already counted by connector name (orchestrator decision 1)"
  - "The recorded run is the ClickHouseSinkConnectorIntegrationTest class, not the whole integrationTest task, because every executing caller of the changed helpers is in the class (ExactlyOnceTest is skipped on standalone, ClickHouseCloudTest never calls the helpers)"
  - "The import order of ClickHouseSinkConnectorIntegrationTest.java stays as it was at the phase start (pre-existing violation, waiver in E-08); ConfluentPlatform.java is formatter-clean with its import set unchanged"

patterns-established:
  - "A pin that must discriminate is run once against the regression it guards before it is relied on, and the evidence entry carries the patch"

requirements-completed: [PIN-04, DEP-01, DEP-02, PIN-02, PIN-03]

coverage:
  - id: D1
    description: "ConfluentPlatform counts finished Datagen tasks only from /connectors/{name}/status of its own connector (one count per task), waits through awaitDatagenCompletion with a less-than exit and named constants, deletes the connector in a finally that cannot mask the generation failure, and has no all-connectors accessor"
    requirement: PIN-04
    verification:
      - kind: other
        ref: "P39-04 Task 1 automated verify (grep checks, scoped Spotless IS CLEAN, anchors rows=172 bad=0, compileIntegrationTestJava, h expect E-08: 16 checks, 0 failed)"
        status: pass
    human_judgment: false
  - id: D2
    description: "datagenCompletionCountIgnoresLeftoverConnectorTest keeps two finished Datagen connectors present and asserts each count equals its own task total (10 and 20), then runs generateData while both are present; against a scratch cross-connector count it failed with expected: <10> but was: <30>"
    requirement: PIN-04
    verification:
      - kind: integration
        ref: "ClickHouseSinkConnectorIntegrationTest#datagenCompletionCountIgnoresLeftoverConnectorTest: red run exit 1 on the scratch variant, green run exit 0 after the revert (E-08); passed again in the recorded class run (E-09)"
        status: pass
    human_judgment: false
  - id: D3
    description: "One recorded integrationTest run of ClickHouseSinkConnectorIntegrationTest on ClickHouse latest x client V2 passes with 0 failures and 0 errors, and every generateData call logged N of N for its own connector (8 lines)"
    requirement: PIN-04
    verification:
      - kind: integration
        ref: "gradlew integrationTest --tests ClickHouseSinkConnectorIntegrationTest: 55 tests, 0 failures, 0 errors, 0 skipped (E-09, P39-04 Task 2 verify)"
        status: pass
    human_judgment: false
  - id: D4
    description: "FORK-DELTA Known cost 15 is titled as scoped and closed, names the pin, cites E-08 and E-09, and keeps an owner and a re-open trigger"
    requirement: PIN-04
    verification:
      - kind: other
        ref: "grep 'scoped to its own connector (closed)' docs/FORK-DELTA.md; grep datagenCompletionCountIgnoresLeftoverConnectorTest docs/FORK-DELTA.md; grep -c 'spans all connectors' = 0"
        status: pass
    human_judgment: false
  - id: D5
    description: "Phase 39 gate on the final tip: testUnit with Docker unreachable (1687 tests, 2 skipped), full suite on latest x V2 (2285 tests, the 18 named skips, E-03 plus 15), cumulative merge-invariant gate PASS, anchors mirror rows=172 bad=0, empty src/main diff against phase start and origin/custom_build, origin unmoved"
    requirement: DEP-01
    verification:
      - kind: other
        ref: "P39-04 Task 3 verify (h expect E-10, E-11; h junit testUnit and test; h skips; h index 11 --done); verify-merge-invariants.sh 54e63648 bcf42763 result=PASS"
        status: pass
    human_judgment: false

# Metrics
duration: 84min
completed: 2026-11-01
status: complete
---

# Phase 39 Plan 04: PIN-04 Datagen harness fix and the Phase 39 gate Summary

**The integration harness can count Datagen completion only on the connector it started, a live pin with two finished leftover connectors failed against a cross-connector count (`expected: <10> but was: <30>`) and passes on the fix, one recorded class run on ClickHouse `latest` x V2 shows 8 of 8 `generateData` calls counting `N of N`, and the Phase 39 gate holds on the final tip.**

## Performance

- **Duration:** 84 min
- **Started:** 2026-11-01T19:25:40Z
- **Completed:** 2026-11-01T20:49Z
- **Tasks:** 3
- **Files modified:** 4 (two `src/integrationTest` files, FORK-DELTA, the evidence file)

## Accomplishments

- Tracer (E-08): `ConfluentPlatform.countFinishedDatagenTasks(connectorName)` reads only `/connectors/{name}/status` and counts each task once whose `trace` holds the marker. `generateData` now runs on `startDatagenConnector` and `awaitDatagenCompletion` (less-than exit, `DATAGEN_POLL_INTERVAL_MS`, `DATAGEN_MAX_POLLS`, one precompiled `DATAGEN_FINISHED_MARKER`), and its `finally` logs a delete failure as `Could not delete Datagen connector {}` instead of letting it replace a generation failure. `getConnectors()` is deleted; it read every connector's config, and its two log call sites now print the sink connector's task status only.
- The pin `datagenCompletionCountIgnoresLeftoverConnectorTest` keeps two finished connectors (10 and 20 tasks, completion awaited on task state by `awaitAllTasksFailed`) and asserts each scoped count equals its own total, then runs `generateData` for a third topic with both still present. Red first: against a scratch, uncommitted whole-response count it failed with `expected: <10> but was: <30>`; after the revert it passed.
- Formatting: `ConfluentPlatform.java` prints `IS CLEAN` on the scoped Spotless check with its import set unchanged (only reordered); `ClickHouseSinkConnectorIntegrationTest.java` keeps its import lines byte-identical and its non-import lines equal the Spotless output (the pre-existing import-order waiver is in E-08). The three NIT-01 anchor rows of `ConfluentPlatform.java` hold (mirror `rows=172 bad=0`).
- Recorded run (E-09): `integrationTest --tests ClickHouseSinkConnectorIntegrationTest` on ClickHouse `latest` (26.10.3.7) x V2 ran 55 tests in 29m 49s with 0 failures, 0 errors, 0 skipped. The XML holds 8 distinct `Datagen connector ...: N of N` lines (10/10 for one partition, 30/30 for three). FORK-DELTA Known cost 15 is rewritten as closed from that record.
- Phase gate (E-10, E-11): `testUnit` with Docker unreachable and `--rerun` ran 1687 tests (E-07's total), 2 skipped; the full suite with `--rerun` ran 2285 tests (E-03's 2270 plus 15), the 18 named skips, `SELECT version()` 26.10.3.7 before and after with E-03's digest; the cumulative gate `verify-merge-invariants.sh 54e63648 bcf42763` ended `result=PASS`; `src/main` equals the phase start and `origin/custom_build`; origin did not move; nothing was pushed.

## Task Commits

1. **Task 1 (tracer): F-3 harness fix, IN-06 hygiene, accessor removed, leftover-connector pin** - `4bbc2bac` (test), evidence `31b17214` (docs)
2. **Task 2: recorded class run and FORK-DELTA Known cost 15** - `bcf42763` (docs; FORK-DELTA and E-09 in one commit)
3. **Task 3: Phase 39 gate** - `e4d3f6b9` (docs; E-10 and E-11)

**Plan metadata:** the SUMMARY commit, followed by the STATE, ROADMAP and REQUIREMENTS commit (docs).

## Files Created/Modified

- `src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java` - eight constants, six new public helpers, `generateData` rewritten on them, `getConnectors()` deleted, imports in Spotless order
- `src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConnectorIntegrationTest.java` - `STOCK_GEN_TEMPLATE`, the leftover-connector pin with its `assertOwnDatagenTaskCount` helper, two `Sink connector task status: {}` log lines
- `docs/FORK-DELTA.md` - Known cost 15 rewritten as scoped and closed
- `.planning/phases/39-dependabot-absorption-behaviour-pins/39-COMMAND-EVIDENCE.md` - entries E-08 to E-11, index rows flipped to pass

## Decisions Made

- The deliverable is the pin plus the accessor removal plus the IN-06 hygiene (orchestrator decision 1); no key of the counting changed because `generateData` already counted by connector name.
- The recorded run is the class run, not the whole `integrationTest` task; the reading that every executing caller of the changed helpers is in the class was re-checked by `grep` and held.
- The pin's completion wait is `awaitAllTasksFailed` (task state only), so it does not depend on the count under test.

## Deviations from Plan

### Auto-fixed Issues

None under the deviation rules. Four execution notes:

**1. [Gate form] Fix commit amended before it was recorded**
- **Found during:** Task 1, first run of the automated verify
- **Issue:** the plan's grep `long finished = 0` (meant for the dead initialiser of the old loop) also matched the legitimate accumulator in `countFinishedDatagenTasks`.
- **Fix:** the local variable was renamed `tasksWithMarker` (no behaviour change), the unpushed commit `16e69d54` was amended to `4bbc2bac`, the scoped Spotless check was re-run (`IS CLEAN`), and the red run and the green run were repeated on the amended commit. E-08 records the amend and carries the patch and SHA-256 of the repeat.
- **Files modified:** `ConfluentPlatform.java`
- **Commit:** `4bbc2bac`

**2. [Gate form] src/test check adapted**
- **Found during:** Task 1, first run of the automated verify
- **Issue:** the plan's `git diff --quiet "$S" HEAD -- src/main src/test` cannot hold, because plan 39-03 added and edited test classes under `src/test`.
- **Fix:** `src/main` was checked against the phase start and `src/main src/test` against this plan's start (`46459dc5`); both exit 0. E-08 states this.
- **Commit:** `31b17214`

**3. [Evidence accuracy] closing timestamps**
- **Found during:** writing E-08 and E-09
- **Issue:** I typed two closing timestamps by hand, each later than the clock.
- **Fix:** both were replaced with `date -u` readings before the entries were committed, so no committed entry carries an estimated time (the 39-03 correction does not recur).

**4. [Shell] `h` is a zsh alias for `history`**
- The plan's `h() {...}` blocks ran under `/bin/bash` with a sourced helper (`/tmp/gsd39/exec/h04.sh`, scratch, not committed), as in 39-02 and 39-03. `-PjavaVersion=21` was added to the compile command, which does not change the result.

**Total deviations:** 0 under the deviation rules; 4 execution notes. **Impact:** none on behaviour or results.

## Issues Encountered

None. No JVM crash file appeared in the repository root and no run needed a retry apart from the red and green repeats of note 1. The invariant gate took about 39 minutes (the plan expected 17 to 23) because the range now holds 1321 anchor commits. Nothing was pushed.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None.

## Threat Flags

None. The plan removes an accessor that logged every connector's config (T-39-13 mitigated), adds only test code and one documentation paragraph, and introduces no new endpoint, auth path or schema surface; the new log lines carry a connector name and two counts, and the evidence copies counts and topic names only.

## Next Phase Readiness

- Phase 39 is complete: all five requirements (DEP-01, DEP-02, PIN-02, PIN-03, PIN-04) have their evidence rows at `pass` (E-01..E-11), `src/main` is unchanged since the phase start, and nothing left the machine.
- Known costs item 15 is closed; the next edit of a `ConfluentPlatform` counting helper is guarded by the pin.

## Self-Check: PASSED

- Modified files exist: both `src/integrationTest` files, `docs/FORK-DELTA.md`, the evidence file.
- Commits exist and are ancestors of HEAD: `4bbc2bac`, `31b17214`, `bcf42763`, `e4d3f6b9`.
- Task automated verifications passed: Task 1 (pre-commit form, after the amend) 16 E-08 checks, anchors `rows=172 bad=0`, scoped Spotless `IS CLEAN`; Task 2 `checks [True, True, True, True] info_lines 8` and 10 E-09 checks; Task 3 16 E-10 checks and 11 E-11 checks, `h index $E 11 --done` OK; `git diff --quiet phase_start_sha HEAD -- src/main` exits 0.

---
*Phase: 39-dependabot-absorption-behaviour-pins*
*Completed: 2026-11-01*
