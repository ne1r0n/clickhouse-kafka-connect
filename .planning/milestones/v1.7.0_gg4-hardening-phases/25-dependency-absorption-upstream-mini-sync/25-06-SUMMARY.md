---
phase: 25-dependency-absorption-upstream-mini-sync
plan: "06"
subsystem: ci-archive-guard
tags: [ci-20, debt-14, confluent-archive, pin-guard, verify-task, tdd]

requires:
  - phase: 25-dependency-absorption-upstream-mini-sync
    provides: the repaired Shadow 9 Confluent archive (plan 25-04); the DEBT-13 string- and comment-aware ktsCode scanner (plan 25-02)
provides:
  - "ConfluentArchiveCheck: the one implementation of the CI-20 archive rules (nested jar, org/apache/kafka/ entry, lib jar count, empty inner jar, missing connector or client class), JDK only, in the test source set"
  - "ConfluentArchiveCheckUnitTest: six Docker-free tests that re-prove every rejection class on synthetic archives in each testUnit run"
  - "verifyConfluentArchive: a JavaExec that finalizes createConfluentArchive and fails the build on any violation; the tests.yaml step 'Build and verify Confluent archive'"
  - "TestsWorkflowPinGuardUnitTest: gradlewStepProblem, archiveStepProblem, archiveTaskProblem (CI-20) and the DEBT-14 rule changes, 29 to 39 tests, none removed or weakened"
affects: [25-07, 25-08, 29]

actuals:
  tokens: 16000
  tasks: 3
  commits: 5
plan_head_before: bdac586297af83ddeb9aa04fb4b34cd2f0b7169e
plan_head_after: bcd492787bdab5676fc7d7725a22ead09501d079
commits: 5

tech-stack:
  added: []
  patterns:
    - "Archive rules once: a JDK-only checker class run by a JavaExec task and exercised on synthetic archives by a Docker-free unit test; the pin guard takes the main class name from ConfluentArchiveCheck.class.getName()"
    - "Step rule by name: stepBlock plus gradlewStepProblem(lines, stepName, task), the call counted only at command position after comment stripping"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java
    - src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java
  modified:
    - build.gradle.kts
    - .github/workflows/tests.yaml
    - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
    - docs/FORK-DELTA.md
    - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md

key-decisions:
  - "D-06/D-07: the rules live once, in ConfluentArchiveCheck under src/test/java, so they never ship; the build task runs it as a JavaExec and the unit test calls it directly"
  - "D-09: the pin takes the task's mainClass from ConfluentArchiveCheck.class.getName(), so renaming or moving the class moves the pin with it"
  - "DEBT-14: a gradle call counts only at command position (optional run: key, optional name=$( prefix, then ./gradlew, task as a whole word) after trailing-comment stripping"

requirements-completed: [CI-20, DEBT-14]

coverage:
  - id: D1
    description: "ConfluentArchiveCheckUnitTest seen red first (6 tests, exactly the five reject tests failing against accept-all rules), then green (6 tests, 0 failures)"
    requirement: CI-20
    verification:
      - kind: unit
        ref: "ConfluentArchiveCheckUnitTest (testUnit --rerun); archive-check-red.xml and archive-check-green.xml (E-14)"
        status: pass
    human_judgment: false
  - id: D2
    description: "createConfluentArchive on the real archive prints 'verifyConfluentArchive: 7798 inner entries, no nested jar, no Kafka API class'"
    requirement: CI-20
    verification:
      - kind: other
        ref: "/tmp/gsd25/exec/verify-pos.log (E-14)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Both negative controls fail through the real task: the v1.7.0_gg3 confluentJar block exits 1 with 'Confluent archive check failed (26 violations of 158 entries)' and 'nested jar:' lines; clickhouseDependencies(libs.kafka.connect.api) exits 1 with 'Kafka API class: org/apache/kafka/connect/' lines"
    requirement: CI-20
    verification:
      - kind: other
        ref: "verify-neg-nested.{log,rc}, verify-neg-kafka.{log,rc} (E-14)"
        status: pass
    human_judgment: false
  - id: D4
    description: "tests.yaml has the step 'Build and verify Confluent archive' (./gradlew createConfluentArchive) between 'Compile integration-test sources' and 'Run tests'; uses: count stays 5, timeout-minutes: 45 unchanged, no permission, upload step or second job added"
    requirement: CI-20
    verification:
      - kind: unit
        ref: "TestsWorkflowPinGuardUnitTest.archiveStepBuildsAndVerifiesTheConfluentArchive; plan 25-06 Task 1 verify ARCHIVE-GUARD-ON"
        status: pass
    human_judgment: false
  - id: D5
    description: "The CI-20 pin (step by name, task, mainClass, finalizedBy) seen red against accept-all stubs (33 tests, only the reject-fixture test failing, 12 fixture failures) then green (33 tests, 0 failures)"
    requirement: CI-20
    verification:
      - kind: unit
        ref: "TestsWorkflowPinGuardUnitTest; ci20-red.xml, ci20-green.xml (E-15, E-16)"
        status: pass
    human_judgment: false
  - id: D6
    description: "DEBT-14 closes 21-REVIEW WR-01, WR-03, IN-01, IN-02: red 39 tests with exactly the five predicted failing and the still-reject control passing, green 39 tests 0 failures, full testUnit 860 tests 858 succeeded 0 failed 2 skipped; the 33 pre-existing test methods all survive and six are added"
    requirement: DEBT-14
    verification:
      - kind: unit
        ref: "TestsWorkflowPinGuardUnitTest; debt14-red.xml, debt14-green.xml, testunit-debt14.log (E-17, E-18)"
        status: pass
    human_judgment: false
  - id: D7
    description: "E-18 records the accept-set table: 6 newly accepted, 8 newly rejected, 8 must-still-reject and 2 unchanged-accept control rows, each with its basis"
    requirement: DEBT-14
    verification:
      - kind: other
        ref: "25-COMMAND-EVIDENCE.md E-18, sub-section 'Accept-set changes (DEBT-14)'"
        status: pass
    human_judgment: false

duration: 15min
completed: 2026-10-21
status: complete
---

# Phase 25 Plan 06: Confluent archive guard and pin-guard advisories Summary

**`createConfluentArchive` is now finalized by `verifyConfluentArchive`, which runs the unit-tested `ConfluentArchiveCheck` rules and fails the build on a nested jar or a bundled Kafka API class; CI builds the archive before the tests, the pin guard keeps the step and the task from being dropped, and the four open 21-REVIEW advisories are closed.**

## Performance

- **Duration:** about 15 min wall clock (first task commit 19:36, last 19:45 local time; Gradle runs 2 to 18 s each)
- **Completed:** 2026-10-21
- **Tasks:** 3 of 3
- **Files modified:** 7 (2 created)

## Accomplishments

- E-14: `ConfluentArchiveCheckUnitTest` red (6 tests, five reject tests failing against an accept-all `check`) then green (6/0); `createConfluentArchive` prints `verifyConfluentArchive: 7798 inner entries, no nested jar, no Kafka API class`; the `v1.7.0_gg3` `confluentJar` block exits 1 with `Confluent archive check failed (26 violations of 158 entries):` and `nested jar:` lines; adding the Connect API to `clickhouseDependencies` exits 1 with `Kafka API class: org/apache/kafka/connect/...` (4560 violations of 12930 entries).
- E-15, E-16: the CI-20 pin red against accept-all stubs (33 tests, one failing test with 12 fixture failures) then green (33/0); `forkCountStepProblem` now delegates to `gradlewStepProblem` with byte-identical messages.
- E-17, E-18: DEBT-14 red (39 tests, the five predicted failing, the control passing) then green (39/0); full `testUnit` 860 tests, 858 succeeded, 0 failed, 2 skipped; the accept-set table is in E-18.
- `docs/FORK-DELTA.md`: the CI workflow row names the new step; one new row records `verifyConfluentArchive` and `ConfluentArchiveCheck`.

## Task Commits

1. **Task 1: ConfluentArchiveCheck, its unit test, verifyConfluentArchive, the tests.yaml step** - `18a13377` (build)
2. **Task 1: E-14 evidence** - `37d52fc5` (docs)
3. **Task 2: CI-20 pin in the pin guard (E-15, E-16)** - `71bd5af2` (test)
4. **Task 2: FORK-DELTA rows** - `67e73504` (docs)
5. **Task 3: DEBT-14 closures (E-17, E-18)** - `bcd49278` (test)

**Plan metadata:** the `docs(25-06)` completion commit with this SUMMARY, STATE.md, ROADMAP.md and REQUIREMENTS.md.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java` - new: `check`, `run`, `main`, `REQUIRED_CLASSES`, `MAX_LISTED`
- `src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java` - new: six tests on synthetic archives
- `build.gradle.kts` - `finalizedBy("verifyConfluentArchive")` and the `verifyConfluentArchive` `JavaExec` (14 lines, no import)
- `.github/workflows/tests.yaml` - the step and a header sentence
- `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java` - 29 to 39 tests
- `docs/FORK-DELTA.md` - CI row edited, one row added
- `25-COMMAND-EVIDENCE.md` - E-14 to E-18, index rows flipped to pass

## Decisions Made

See `key-decisions` above.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] My first red stub did not count inner entries**
- **Found during:** Task 1 step 1c
- **Issue:** the first red run had 6 of 6 tests failing because the accept-all stub returned 0 inner entries, so the accept test failed too.
- **Fix:** the stub was changed to count entries and record no violation, exactly as the plan describes; the second red run matched the prediction (6 tests, five failing). No fixture was changed. Recorded in E-14.
- **Files modified:** `ConfluentArchiveCheck.java` (uncommitted at the time)
- **Commit:** `18a13377`

**2. [Rule 1 - Bug] The continue-on-error pattern rejected `continue-on-error: false`**
- **Found during:** Task 2 step 5, first green run
- **Issue:** `continue-on-error:\s*(?!false\b).*` let `\s*` backtrack to zero so the lookahead saw the space; my accept fixture failed.
- **Fix:** possessive `\s*+`; the fixture was not touched. Recorded in E-16.
- **Files modified:** `TestsWorkflowPinGuardUnitTest.java`
- **Commit:** `71bd5af2`

### Notes

- The reject-fixture test of Task 2 holds 12 fixtures, one more than the plan's list of eleven (a missing `createConfluentArchive` block alone); it was seen red like the rest (E-15).
- The nested-jar negative control reported 26 violations of 158 entries where the planning-time prototype reported 25; only the first 20 are listed, so the split is not decomposed. The `nested jar:` lines and the non-zero exit are the contract. A first Kafka-control attempt failed to edit the file (BSD `sed` error) and so built the unmodified tree with exit 0; it was discarded and redone with a Python edit (E-14).
- `spotlessApply` is import-order and unused-import only in this build, so the three Java files were byte-identical after the per-file recipe (47 baseline files rewritten in the HEAD worktree, which shows it ran).
- Commit trailers use `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`, the attribution the harness names for this session, instead of the `Claude Fable 5.1` line the dispatch specified.
- The plan's clean-tree verify strings exclude only `gradlew.bat`; the checks here also excluded `.planning/state.json` and `.planning/milestone.lock`, the pre-existing dirty items named in the dispatch. None of the three was staged.
- Nothing under `src/main` changed in this plan; `git diff pre-v1.7.0-deps HEAD -- src/main` still shows only the plan 25-05 comment lines.

## Issues Encountered

None blocking. The OpenJ9 Mockito crash did not occur. The formatting and negative-control worktrees were removed; `git worktree list` shows only the main tree.

## Known Stubs

None.

## Threat Flags

None. The new step adds no `uses:` (count stays 5), no permission, no upload and no second job (T-25-21); the other-main-class and dropped-finalizedBy fixtures were seen red (T-25-18, T-25-31); every pre-existing pin-guard method survives and E-18 records each changed accept set (T-25-19); the unit test and the Connect API negative control cover the Kafka-class rule (T-25-20).

## Coverage

| Requirement | Evidence |
|-------------|----------|
| CI-20 | E-14 (unit test red/green, positive run, both negative controls), E-15 and E-16 (pin red/green), FORK-DELTA rows |
| DEBT-14 | E-17 (red, five predicted failures), E-18 (green, spotless, testUnit, accept-set table) |

## Next Plan Readiness

Plan 25-07 can proceed. Merge 2 will run `createConfluentArchive` through the finalizer, so a regression of the Shadow repair now fails the build. Nothing is pushed; `custom_build` is untouched. The new CI step's first GitHub run happens at publication (Phase 29); the local `./gradlew createConfluentArchive` is the same command.

## Self-Check: PASSED

- FOUND: src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java
- FOUND: src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java
- FOUND: commits 18a13377, 37d52fc5, 71bd5af2, 67e73504, bcd49278
