---
phase: 05-ci-final-verification
plan: 01
subsystem: infra
tags: [github-actions, ci, jacoco, coverage, python]

# Dependency graph
requires:
  - phase: 01-test-infrastructure-green-baseline
    provides: jacocoTestReport and jacocoForkReport Gradle tasks, both finalizedBy(test), with jacocoForkReport's XML output pinned to build/reports/jacoco/fork/jacocoForkReport.xml specifically for this phase to consume
provides:
  - .github/scripts/jacoco_summary.py - JaCoCo XML to job-summary markdown renderer (stdlib only)
  - .github/workflows/tests.yaml rewritten as a single ClickHouse latest x client V2 job on current actions
affects: [05-02-final-verification]

# Actuals (#2632)
actuals:
  tokens: 2169
  tasks: 2
  commits: 2

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Repo-local python3 stdlib script (xml.etree.ElementTree) renders CI coverage summaries, no marketplace action"
    - "CI coverage summary step opens $GITHUB_STEP_SUMMARY itself instead of a shell tee, so the step's own exit status reflects the script (bash -e has no pipefail by default)"

key-files:
  created:
    - .github/scripts/jacoco_summary.py
  modified:
    - .github/workflows/tests.yaml

key-decisions:
  - "Coverage parser is a separate checked-in file (.github/scripts/jacoco_summary.py), not an inline heredoc in the workflow - testable verbatim, avoids YAML/shell quoting hazards, no build.gradle.kts change, and .github/scripts/ can never conflict with an upstream sync"
  - "The script opens and appends to GITHUB_STEP_SUMMARY itself rather than the workflow piping stdout through tee -a, so a crashed parser fails the step instead of reporting false success under bash -e's default no-pipefail behavior"
  - "actions/upload-artifact@v4 selected over newer v5/v6/v7 (all confirmed to resolve) because v4 is not deprecated and matches the tag this phase's research/pattern evidence was verified against; a version bump is a low-risk follow-up"

patterns-established:
  - "Repo-local python3 stdlib coverage summary script pattern for CI, avoiding third-party marketplace actions"

requirements-completed: [CI-01, CI-02, CI-03]

coverage:
  - id: D1
    description: "jacoco_summary.py parses the two JaCoCo XML reports and renders headline + 15-row fork-changed-class markdown to both stdout and GITHUB_STEP_SUMMARY"
    requirement: "CI-03"
    verification:
      - kind: unit
        ref: "manual run: GITHUB_STEP_SUMMARY=<tmp> python3 .github/scripts/jacoco_summary.py, byte-identical stdout/file content, 15/15 fork-class rows present"
        status: pass
      - kind: unit
        ref: "manual run: ASCII-only byte scan + line-count + required-token scan over .github/scripts/jacoco_summary.py"
        status: pass
    human_judgment: false
  - id: D2
    description: "tests.yaml rewritten to a single ClickHouse latest x client V2 job on actions/checkout@v4, actions/setup-java@v6 (temurin 17), gradle/actions/setup-gradle@v6, with no matrix/cloud/guard remnants"
    requirement: "CI-01, CI-02"
    verification:
      - kind: unit
        ref: "manual run: ruby -ryaml structural assertion (single job, no strategy key, required uses/env/step-name/if values)"
        status: pass
      - kind: unit
        ref: "manual run: python3 banned-token scan (secrets./matrix./SKIP_STEP/TESTCONTAINERS/gradle-build-action/@v3/adopt/jacocoTestCoverageVerification/integrationTest all zero) plus header-comment and on: [push] checks"
        status: pass
      - kind: other
        ref: "manual run: live GitHub API tag resolution for all 4 action tags (checkout@v4, setup-java@v6, gradle/actions@v6, upload-artifact@v4)"
        status: pass
    human_judgment: false
  - id: D3
    description: "tests.yaml uploads build/reports/jacoco/ and build/reports/tests/ as artifacts, both if: always(), with no coverage threshold anywhere"
    requirement: "CI-03"
    verification:
      - kind: unit
        ref: "manual run: ruby -ryaml artifact-name and if:always() assertion for jacoco-coverage-reports and gradle-test-reports"
        status: pass
    human_judgment: false

duration: ~10min
completed: 2026-09-18
status: complete
---

# Phase 5 Plan 1: CI Workflow Modernization and Coverage Publishing Summary

**Rewrote the fork's push-triggered `tests.yaml` into a single ClickHouse `latest` x client V2 job on current GitHub Actions tags, backed by a new stdlib-only `jacoco_summary.py` that renders JaCoCo coverage into both the job summary and stdout with no coverage threshold.**

## Performance

- **Duration:** ~10 min
- **Started:** 2026-09-18T06:20:42Z (approx, per STATE.md phase-start marker)
- **Completed:** 2026-09-18T06:27:37Z
- **Tasks:** 2
- **Files modified:** 2 (1 created, 1 modified)

## Accomplishments
- `.github/scripts/jacoco_summary.py` created: parses `jacocoTestReport.xml` and `jacocoForkReport.xml` with `xml.etree.ElementTree`, renders a `## Coverage Summary` heading, a two-row headline table (full suite / fork-changed classes), and a 15-row per-class table for exactly the fork-changed classes in `build.gradle.kts`'s `forkChangedClasses` list — writing identical markdown to stdout and `$GITHUB_STEP_SUMMARY` (when set), guarding classes with no `LINE`/`BRANCH` counter as `n/a`, and exiting non-zero naming the missing path if either XML is absent
- `.github/workflows/tests.yaml` rewritten: the 8-cell client x ClickHouse matrix, the cloud-credentials guard step, every `if:` condition, and both cloud secret env vars are gone; the single `build` job runs `actions/checkout@v4` -> `actions/setup-java@v6` (temurin 17) -> `gradle/actions/setup-gradle@v6` -> `./gradlew test` (env `CLICKHOUSE_VERSION=latest`, `CLIENT_VERSION=V2`) -> the new coverage-summary step -> two `if: always()` `actions/upload-artifact@v4` uploads (`jacoco-coverage-reports`, `gradle-test-reports`)
- Added a `concurrency` block (copied verbatim from `test-head.yaml`) with `cancel-in-progress: true`, `timeout-minutes: 45`, and a fork-modification header comment naming the removed matrix legs and pointing at `.planning/REQUIREMENTS.md` CI-01..04
- All four action tags (`actions/checkout@v4`, `actions/setup-java@v6`, `gradle/actions/setup-gradle@v6`, `actions/upload-artifact@v4`) re-confirmed live against `api.github.com` at execution time, resolving to real commits

## Task Commits

Each task was committed atomically:

1. **Task 1: Coverage summary end-to-end - real JaCoCo XML in, rendered markdown out to both destinations** - `666f585` (feat)
2. **Task 2: Rewrite tests.yaml as the single latest x V2 job on current actions, publishing coverage** - `73b2c71` (feat)

**Plan metadata:** committed together with STATE.md/ROADMAP.md updates below.

## Files Created/Modified
- `.github/scripts/jacoco_summary.py` - new: parses both JaCoCo XML reports, renders the coverage summary markdown to stdout + `$GITHUB_STEP_SUMMARY`
- `.github/workflows/tests.yaml` - rewritten: single ClickHouse `latest` x client V2 job on current actions, publishing coverage and test-report artifacts, no coverage gate

## Decisions Made
- Coverage parser lives in `.github/scripts/jacoco_summary.py` as a real checked-in file rather than an inline `run:` heredoc (DEC-A in the plan) - testable verbatim against real XML before it ships, avoids a quoting-hazard class of bug (dollar signs, braces, f-strings inside a YAML block scalar), introduces no third-party action, and adds no rebase-conflict surface since upstream has no file at that path
- The script opens and appends to `GITHUB_STEP_SUMMARY` itself; the workflow step carries no pipe (DEC-B) - `bash -e {0}` (GitHub Actions' default `run:` shell) does not set `pipefail`, so piping the script's stdout through `tee -a` would let a crashed parser report step success. Removing the pipe makes the step's exit status the script's own, exercising the same code path locally and in CI
- `actions/upload-artifact@v4` selected (DEC-C) over the newer `v5`/`v6`/`v7` (all confirmed live) because `v4` is not deprecated and is the tag this phase's research and pattern evidence was verified against; a later bump is a low-risk, one-line follow-up

## Deviations from Plan

None - plan executed exactly as written. Both tasks' acceptance criteria and `<verify>` blocks passed on the first implementation without any fix cycle.

## Issues Encountered

The commit for Task 1 initially swept in two unrelated files (`.planning/phases/05-ci-final-verification/05-DISCUSS-CHECKPOINT.json`, `.planning/phases/05-ci-final-verification/05-PATTERNS.md`) that were already staged in the git index before this executor session started (leftover from a prior planning step). Caught immediately via `git show --stat HEAD` after the first commit; fixed with `git reset --soft HEAD~1` followed by a path-scoped `git commit -m "..." -- .github/scripts/jacoco_summary.py`, which restored the pre-existing staged state exactly and produced a clean, atomic commit containing only the task's own file. Verified via `git status --short` showing the same `AD`/`A` staged files as before, untouched. No code or task content was affected.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- `.github/scripts/jacoco_summary.py` and the rewritten `.github/workflows/tests.yaml` are ready for plan 05-02, which pushes `custom_build` (behind its own blocking-human checkpoint, D-10) and verifies the resulting CI run via the unauthenticated GitHub REST API
- Nothing in `src/`, `build.gradle.kts`, or any test file was touched by this plan
- Local verification only; the workflow has not yet run on a GitHub Actions runner - that is 05-02's job

## Self-Check: PASSED

- FOUND: `.github/scripts/jacoco_summary.py`
- FOUND: `.github/workflows/tests.yaml`
- FOUND: commit `666f585` (Task 1)
- FOUND: commit `73b2c71` (Task 2)
- Re-ran Task 1's `<verify>` (structure + ASCII/line-count/token scan) and Task 2's `<verify>` (ruby YAML structural assertion, banned-token scan, live action-tag resolution) after both commits: all passed
- Re-ran plan-level `<verification>`: no `build.gradle.kts` or `src/` changes present in either commit

---
*Phase: 05-ci-final-verification*
*Completed: 2026-09-18*
