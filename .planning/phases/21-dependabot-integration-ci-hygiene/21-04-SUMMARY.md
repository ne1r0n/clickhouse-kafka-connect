---
phase: 21-dependabot-integration-ci-hygiene
plan: "04"
subsystem: testing
tags: [pin-guard, ci, junit5, github-actions, dependabot, gradle, fixtures]

requires:
  - phase: 21-dependabot-integration-ci-hygiene
    provides: "21-02/21-03: the DEPS-01 merge 82524e12 and a gate that passes over the full range; tests.yaml, build.gradle.kts and dependabot.yml unchanged since that merge"
provides:
  - "TestsWorkflowPinGuardUnitTest rules extracted as static Optional<String> line-list methods (D-06)"
  - "CI-18: the Report test fork count step must call ./gradlew reportTestForks in its own block, nproc is allowed only in the msg= context line, build.gradle.kts must register reportTestForks reading maxParallelForks from the test task"
  - "CI-19: runs-on accepts a trailing comment; write scope ignores step name and run text and now also rejects the quoted write forms"
  - "D-09: dependabot.yml github-actions exclude-paths must list the four upstream workflows and only existing files under .github/workflows/"
  - "18 committed fixture tests and 3 new real-file tests (26 in the class), E-10 and E-11 in 21-EVIDENCE.md"
affects: [21-05 phase close gate, 23 DEBT-12 (17-REVIEW closures), 24]

actuals:
  tokens: 14000
  tasks: 3
  commits: 2

plan_head_before: a81d3dc3d66f4a25746c10fe1a86eb2706e4ac2e
plan_head_after: fee898c635fe7a9fd33e5e00a0485743e14848d2

tech-stack:
  added: []
  patterns:
    - "Guard rules as static methods over List<String> returning Optional<String> (empty = accept), exercised on committed good and bad fixtures and on the real files"
    - "Indent-bounded step scan for workflow steps instead of a list-item boundary"
    - "Red run recorded against the old rules before the fix, with tests and fix committed together"

key-files:
  created:
    - .planning/phases/21-dependabot-integration-ci-hygiene/21-04-SUMMARY.md
  modified:
    - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
    - .planning/phases/21-dependabot-integration-ci-hygiene/21-EVIDENCE.md

key-decisions:
  - "The deliberate narrowing of RUNS_ON and WRITE_SCOPE is a user-decided (D-10) exception to AGENTS.md's 'never delete/weaken existing tests', recorded as the Accept-set changes (D-10) table in E-11"
  - "Fixtures are List.of(...) with exact leading spaces (research Pattern 4), not text blocks, because the indent-aware parsers depend on them"
  - "The fixture upstream-workflow list is kept as its own literal list instead of reusing UPSTREAM_WORKFLOWS, so a bad edit of the production constant cannot silently change what the fixtures expect"

patterns-established:
  - "Pin-guard rule additions get a must-accept and a must-reject fixture group, seen red before the rule body exists"

requirements-completed: [CI-18, CI-19]

coverage:
  - id: D1
    description: "Pin-guard rules extracted into static line-list methods with identical behaviour and messages on the real files"
    requirement: CI-18
    verification:
      - kind: unit
        ref: "TestsWorkflowPinGuardUnitTest (5 pre-existing real-file tests, green after the refactor commit 37ef6d40)"
        status: pass
    human_judgment: false
  - id: D2
    description: "CI-18: the fork-count step, the nproc formula ban and the reportTestForks task are pinned, with each failure seen red on a fixture"
    requirement: CI-18
    verification:
      - kind: unit
        ref: "TestsWorkflowPinGuardUnitTest#forkCountStepRuleRejectsShellFormulas, #forkTaskRuleRejectsMissingOrConstantTasks and the real-file tests forkCountStepCallsReportTestForks, reportTestForksReadsMaxParallelForksFromTheTestTask"
        status: pass
    human_judgment: false
  - id: D3
    description: "CI-19: runs-on accepts a trailing comment, step text is not a write scope, every real write grant (including quoted write) is still rejected"
    requirement: CI-19
    verification:
      - kind: unit
        ref: "TestsWorkflowPinGuardUnitTest#runsOnRuleAcceptsTrailingComment, #runsOnRuleRejectsOtherRunners, #writeScopeRuleIgnoresStepText, #writeScopeRuleRejectsWriteGrants"
        status: pass
    human_judgment: false
  - id: D4
    description: "D-09 guard half of 17-REVIEW WR-01: dependabot exclude-paths lists the four upstream workflows and only existing files"
    requirement: CI-18
    verification:
      - kind: unit
        ref: "TestsWorkflowPinGuardUnitTest#dependabotExcludeRuleRejectsMissingOrStalePaths and #dependabotExcludesTheUpstreamWorkflowsAndOnlyExistingFiles"
        status: pass
    human_judgment: false

duration: 7min
completed: 2026-10-17
status: complete
---

# Phase 21 Plan 04: Pin-guard rules, fixtures and the CI-18/CI-19/D-09 closures Summary

**The tests.yaml pin guard now fails `testUnit` without Docker if the fork-count step stops calling `./gradlew reportTestForks`, if the `reportTestForks` task is deleted or hard-coded, or if Dependabot's exclude-paths drifts, while `runs-on: ubuntu-24.04 # comment` and step text containing `: write` no longer false-fail and the quoted `"write"` form is newly rejected**

## Performance

- **Duration:** 7 min
- **Started:** 2026-10-17T09:05:15Z
- **Completed:** 2026-10-17T09:12Z
- **Tasks:** 3
- **Files modified:** 3 (the test class, 21-EVIDENCE.md and this summary)

## Accomplishments

- Extracted the five existing rules into `static Optional<String> ...(List<String> lines)` methods with the old regexes and byte-identical messages; the five `@Test` methods, `@DisplayName`s and message texts are unchanged (commit `37ef6d40`, 5/5 green).
- Added 18 fixture tests and 3 new real-file tests (26 in the class). The red run against the old regexes and accept-all new rules failed exactly the six predicted fixture tests (E-10), with the fixture-level messages recorded.
- Swapped in the fixed `RUNS_ON`, `WRITE_SCALAR`/`WRITE_FLOW` and the real `forkCountStepProblem`, `nprocProblem`, `forkTaskProblem` and `dependabotExcludeProblem` bodies (commit `fee898c6`): class 26/26, full `testUnit` 577 tests, 0 failed, 2 skipped, per-file spotless clean (E-11).
- E-11's `#### Accept-set changes (D-10)` table records every verdict change: eight inputs newly accepted (two runs-on comment fixtures, six step-text fixtures), the real write grants and `runs-on: ubuntu-latest # x` rejected before and after, and the quoted `contents: "write"` / `'write'` forms accepted before and rejected after.

## Task Commits

1. **Task 1: Extract the five rules into line-list methods (D-06)** - `37ef6d40` (refactor)
2. **Task 2: Fixture tests and new real-file tests, red run in E-10** - no commit by design (D-07); its edits ship in Task 3
3. **Task 3: Fixed rules, real CI-18/D-09 bodies, green run, E-11** - `fee898c6` (test)

**Plan metadata:** the `docs(21-04): complete ...` commit that follows this file.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java` - rule methods, helpers (`ktsCode`, `excludePaths`, `indent`, `accepts`/`rejects`), new constants, 8 real-file tests, 18 fixture tests
- `.planning/phases/21-dependabot-integration-ci-hygiene/21-EVIDENCE.md` - E-10 and E-11 and their index verdicts

## Decisions Made

- Followed the plan as specified. The `accepts`/`rejects` helpers keep the `(String fixture, Optional<String> problem)` signature the plan names, plus thin overloads taking a rule function and the lines, so each fixture is one line at the call site.
- Used literal fixture path lists rather than the production `UPSTREAM_WORKFLOWS` constant, so the fixtures stay independent of the constant they check.

## Deviations from Plan

None - plan executed exactly as written.

## 17-REVIEW closures for Phase 23 (DEBT-12)

- **IN-01** is closed by CI-18: the fork-count step call, the nproc formula ban and the `reportTestForks` task are pinned, each with a red-seen fixture.
- **IN-02** is closed by CI-19: the `runs-on` trailing comment and the step-text false failures are gone, each seen red on the old regex.
- **WR-01, guard half** is closed by D-09: the exclude-paths rule is in place. Together with OPS-02 (plan 21-01), Phase 23 can record WR-01 as fixed.

## Issues Encountered

None.

## Known Stubs

None.

## Threat Flags

None. The change adds only test code and evidence; no network, auth or file-access surface in shipped code (T-21-14 to T-21-17 mitigations are covered by the committed fixtures, the E-10 red run and the unchanged-guarded-files check).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 21-05 can rely on a green `testUnit` (577 tests) and unchanged `.github`, `build.gradle.kts`, `gradle` and `src/main` since the DEPS-01 merge.
- Nothing was pushed; `custom_build` was never checked out or committed to. `.planning/state.json` and `.planning/milestone.lock` were left unstaged.

---
*Phase: 21-dependabot-integration-ci-hygiene*
*Completed: 2026-10-17*

## Self-Check: PASSED

- FOUND: `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java`, `.planning/phases/21-dependabot-integration-ci-hygiene/21-EVIDENCE.md` (E-10 and E-11 present)
- FOUND commits: `37ef6d40`, `fee898c6` (ancestors of HEAD)
- Automated verify markers printed: EXTRACTED-GREEN, PER-FILE-SPOTLESS-CLEAN, RED-RUN-RECORDED, PINGUARD-GREEN, PINGUARD-COMMITTED, ACCEPT-SET-RECORDED.
