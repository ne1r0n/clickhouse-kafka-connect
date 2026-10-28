---
phase: 31-fork-owned-nightly-integration-tests
plan: "04"
subsystem: ci
tags: [github-actions, nightly, integration-tests, workflow-guard, junit-xml, python-stdlib]

requires:
  - phase: 31-fork-owned-nightly-integration-tests
    provides: "31-06 E-18 nightly-timeouts line (step=75 job=85, unamended by E-06b); 31-01 measurement; 31-03 upstream byte guard"
provides:
  - ".github/workflows/nightly-integration-tests.yaml: gated, read-only, SHA-pinned nightly running the whole integrationTest task on ClickHouse latest x client V2"
  - ".github/scripts/failed_tests_summary.py: stdlib failing-test summary and one escaped, capped annotation"
  - "NightlyWorkflowGuardUnitTest (58 Docker-free cases) and FailedTestsSummaryScriptUnitTest (8 cases)"
  - "five NIT-01 anchor rows; E-12 to E-14"
affects: [31-05, phase-32-fork-delta, phase-32-nit-03-dispatch]

# Actuals (#2632): chars/4 over the diff of the six plan commits (7 files)
actuals:
  tokens: 19600
  tasks: 3
  commits: 6
plan_head_before: 534861ffc6229b9734bdd8f2712e939aff525e26
plan_head_after: 392d8d1b33e3b1a54f8bd490b8590e15daf25882

tech-stack:
  added: []
  patterns:
    - "Nightly guard fixtures are edits of the real workflow's non-comment lines (timeouts pinned at 80/70), built by helpers that throw on a missing anchor line"
    - "Python helper script under test through ProcessBuilder with python3 -I, output read as UTF-8 from redirected files"

key-files:
  created:
    - .github/workflows/nightly-integration-tests.yaml
    - .github/scripts/failed_tests_summary.py
    - src/test/java/com/clickhouse/kafka/connect/ci/NightlyWorkflowGuardUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/ci/FailedTestsSummaryScriptUnitTest.java
  modified:
    - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/31-fork-owned-nightly-integration-tests/31-COMMAND-EVIDENCE.md

key-decisions:
  - "Timeouts are the last nightly-timeouts line of the evidence (E-18): Gradle step 75 minutes, job 85; E-06b left them unamended"
  - "The capacity probe tolerates a failing docker version call (|| echo unavailable) so a probe failure cannot stop the run before the tests"
  - "Guard fixtures derive from the real file, so removing an anchor line from the real workflow turns the reject sources red at initialisation as well as the real-file tests"
  - "Names in the annotation are cut at 300 characters so the first name always fits the 1000-character cap after percent-escaping; the step summary keeps every full name"

patterns-established:
  - "One workflow-command line per run: percent, CR and LF escaped first, names joined and cut at the last whole name inside the cap with a (+k more) tail"

requirements-completed: [NIT-01]

coverage:
  - id: D1
    description: "The fork-owned nightly workflow runs the whole integrationTest task on ClickHouse latest x client V2 on ubuntu-24.04 with read-only permissions, non-persisted checkout credentials, a 75-minute Gradle-step timeout inside an 85-minute job timeout, no secret, no cloud reference and no test filter"
    requirement: NIT-01
    verification:
      - kind: unit
        ref: "NightlyWorkflowGuardUnitTest (58 cases, 0 failures; red on the missing file first, E-12)"
        status: pass
      - kind: unit
        ref: "WorkflowActionPinGuardUnitTest (21) and TestsWorkflowPinGuardUnitTest (39) green with no guard edit"
        status: pass
    human_judgment: false
  - id: D2
    description: "The job is gated so a manual dispatch always starts and a scheduled run starts only once NIGHTLY_ENABLED is true; triggers are exactly schedule and an input-free workflow_dispatch"
    requirement: NIT-01
    verification:
      - kind: unit
        ref: "NightlyWorkflowGuardUnitTest gate rule (7 rejects) and trigger rule (10 rejects)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Failed test names reach the step summary and one escaped, capped error annotation, and the summary and both uploads run under always()"
    requirement: NIT-01
    verification:
      - kind: unit
        ref: "FailedTestsSummaryScriptUnitTest (8 cases, 0 failures; mutation of the cap and the LF escape turned two cases red, E-14)"
        status: pass
      - kind: unit
        ref: "NightlyWorkflowGuardUnitTest always-run step rule (7 rejects)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The nightly actually runs green on a GitHub-hosted runner within the timeouts"
    requirement: NIT-01
    verification: []
    human_judgment: true
    rationale: "Nothing in this phase pushes or dispatches (the workflow reaches GitHub only with Phase 32's publication); the first real run is the NIT-03 dispatch"

duration: 7min
completed: 2026-10-26
status: complete
---

# Phase 31 Plan 04: Fork-owned nightly workflow, failed-test summary and guards Summary

**A variable-gated, read-only, SHA-pinned nightly that runs the whole `integrationTest` task on ClickHouse latest x client V2 in the runner's Docker (75-minute step, 85-minute job), reports failures through a stdlib JUnit-XML summary script, and is pinned Docker-free by a 58-case workflow guard and an 8-case script test.**

## Performance

- **Duration:** 7 min (from the recorded start; the measurements it relies on were taken in plans 31-01 and 31-06)
- **Started:** 2026-10-26T17:28:07Z
- **Completed:** 2026-10-26T17:36Z
- **Tasks:** 3
- **Files modified:** 7 (4 created, 3 modified)

## Accomplishments

- **Tracer (Task 1):** `NightlyWorkflowGuardUnitTest` core rules red on the missing file (3 of 3 failed), then the workflow and the script written and green in one commit: nightly guard 3, `WorkflowActionPinGuardUnitTest` 21 and `TestsWorkflowPinGuardUnitTest` 39 passing with no edit to the CIG-01 guard; YAML parse, shape check and the script smoke on a fixture report all passed; five anchor rows take the mirror from 126 to 131 rows, bad=0. The tracer's automated `<verify>` was re-run end to end and passed before expansion.
- **Workflow:** schedule `17 3 * * *` plus input-free `workflow_dispatch`; one job gated on `github.event_name == 'workflow_dispatch' || vars.NIGHTLY_ENABLED == 'true'`; `ubuntu-24.04`, `permissions: contents: read`, checkout without persisted credentials, the four tests.yaml pins, a capacity probe (nproc, memory, disk, Docker server version), the Gradle step with `CLICKHOUSE_VERSION: latest`, `CLIENT_VERSION: V2` and a bare `./gradlew integrationTest`, then the summary step and two 14-day uploads, all under `always()`.
- **Script:** `failed_tests_summary.py` parses `TEST-*.xml`, prints and appends a Markdown summary (totals, table of failed `class#method` with kind and first message line), and emits exactly one annotation (`::error` capped at 1000 characters with a `(+k more)` tail, or `::notice`), plus warnings for a missing directory or unreadable files. The tests found no defect in it; a mutation of the cap and the LF escape turned the escaping and cap cases red.
- **Guard rules (Task 2):** 58 cases = 8 real-file tests, 6 accept fixtures and 44 reject fixtures (gate 7, trigger 10, timeout 6, Gradle step 9, cloud reference 5, always-run step 7), each reject exactly one edit of the valid nightly. `TestsWorkflowPinGuardUnitTest` changed by exactly two removed `private` words. A five-edit mutation of the real file turned six real-file tests red, then the file was restored byte for byte.
- **Gates (Task 3):** full `testUnit` 1374 tests, 1372 succeeded, 0 failed, 2 skipped; the three Java files are unchanged by `spotlessApply` in a detached worktree at HEAD; `src/main` identical to `v1.7.0_gg4`; `integrationTests.yaml`, `dependabot.yml` and every other workflow untouched.

## Task Commits

1. **Task 1: tracer (workflow, script, core guard, anchors)** - `3e2d8eb6` (ci), evidence `18a1cb24` (docs, E-12)
2. **Task 2: remaining guard rules and fixtures** - `feb97802` (test), evidence `93bf0b88` (docs, E-13)
3. **Task 3: summary script test and suite gates** - `2f55c033` (test), evidence `392d8d1b` (docs, E-14)

**Plan metadata:** the commit holding this SUMMARY, STATE.md, ROADMAP.md and REQUIREMENTS.md (docs: complete plan)

## Files Created/Modified

- `.github/workflows/nightly-integration-tests.yaml` - the fork-owned nightly
- `.github/scripts/failed_tests_summary.py` - JUnit XML to Markdown summary and one safe annotation
- `src/test/java/com/clickhouse/kafka/connect/ci/NightlyWorkflowGuardUnitTest.java` - 58-case Docker-free guard
- `src/test/java/com/clickhouse/kafka/connect/ci/FailedTestsSummaryScriptUnitTest.java` - 8-case script test through `python3 -I`
- `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java` - two `private` words removed (`CONTINUE_ON_ERROR`, `TRAILING_COMMENT`)
- `.planning/scripts/fork-anchors.txt` - comment line and five NIT-01 rows
- `.planning/phases/31-fork-owned-nightly-integration-tests/31-COMMAND-EVIDENCE.md` - E-12 to E-14

## Decisions Made

- Timeouts step 75 / job 85 from E-18 (cold-equivalent wall 36 min x 2, rounded up), unamended by E-06b.
- The capacity probe cannot fail the job: `docker version` falls back to `unavailable`.
- Fixtures follow the real file (only the two timeouts are pinned to 80 and 70), so the guard cannot drift from the file it protects.
- Annotation names are cut at 300 characters before escaping; the step summary lists full names.

## Deviations from Plan

None - plan executed exactly as written.

Notes (not deviations): the plan's verify commands 1 and 3 of Task 1 were run from scratch script files under `/tmp/gsd31-scripts` with identical text; the Spotless comparison of `NIGHTLY-GATES-OK` was run in a throwaway worktree and its result printed separately, then the remaining conditions of that command were run in one line (all printed `NIGHTLY-GATES-OK`). Two extra mutation checks (real workflow, script) were added beyond the plan and recorded in E-13 and E-14.

**Total deviations:** 0
**Impact on plan:** none.

## Issues Encountered

None. No OpenJ9 crash occurred (default Gradle JVM, no core or javacore files).

## Known Stubs

None.

## Threat Flags

None. The plan's threat register (T-31-13 to T-31-21) is covered by the workflow shape and the guard rules: full-SHA pins, read-only token, `persist-credentials: false`, escaped capped annotation, no secret or cloud reference, the pinned gate, always-run reporting. The workflow only reaches GitHub with Phase 32's publication.

## User Setup Required

None - no external service configuration required in this phase. The repository variable `NIGHTLY_ENABLED` is only read; the operator sets it after the Phase 32 dispatch run (D-01).

## Next Phase Readiness

- 31-05 can record the criterion-2 wording and the NIT-03 move; the nightly's file path, name and job key are now fixed (`Nightly Integration Tests (ClickHouse latest, client V2)`, job `integration`).
- Phase 32 carries FORK-DELTA rows for the nightly, the CIG-01 coverage of it, the summary script and the guards; the first real run is the NIT-03 dispatch, where the capacity probe and the failed-test summary diagnose a CI-only failure (image footprint 13.83 GB against a 14 GB runner SSD is the largest known risk, E-18).

## Self-Check: PASSED

Created and modified files exist on the branch and the six task commits (`3e2d8eb6`, `18a1cb24`, `feb97802`, `93bf0b88`, `2f55c033`, `392d8d1b`) are ancestors of HEAD. NIGHTLY-SHAPE-OK, NIGHTLY-TRACER-GREEN-OK, SUMMARY-SMOKE-OK, NIGHTLY-COMMIT-OK, NIGHTLY-GUARD-OK, NIGHTLY-RULES-COMMIT-OK, SUMMARY-TEST-OK and NIGHTLY-GATES-OK were printed.

---
*Phase: 31-fork-owned-nightly-integration-tests*
*Completed: 2026-10-26*
