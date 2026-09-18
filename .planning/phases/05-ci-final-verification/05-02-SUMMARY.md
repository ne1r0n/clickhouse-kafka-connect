---
phase: 05-ci-final-verification
plan: 02
subsystem: testing
tags: [ci, github-actions, jacoco, gradle, clickhouse, testcontainers]

# Dependency graph
requires:
  - phase: 05-ci-final-verification (plan 01)
    provides: ".github/workflows/tests.yaml single-job rewrite and .github/scripts/jacoco_summary.py"
provides:
  - "Machine-verified proof that the pinned suite is green locally on ClickHouse latest x client V2 (371+ tests, 0 failures/errors)"
  - "Machine-verified proof the fork coverage report reflects a real full run (non-zero LINE coverage on the four previously-0% SMT/sink classes)"
  - "Machine-verified proof the workflow's coverage-summary command renders correctly against that report"
  - "The first public push of custom_build (106 commits, including tracked .planning/ files) to origin, after an explicit human go/no-go"
  - "Machine-verified proof of a green CI run on the pushed SHA: exactly one tests.yaml run, exactly one job, Run tests and Generate coverage summary steps both success, both artifacts present and unexpired"
affects: [ci, coverage-reporting, release-readiness]

# Actuals (#2632)
actuals:
  tokens: 9000
  tasks: 3
  commits: 1
  plan_head_before: a295af8aacef6c5c3ad0b49abe89a3a85ca4efa2

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "CI verification via unauthenticated GitHub REST API (run/jobs/artifacts endpoints only) instead of gh CLI or a token — read-only, budget-aware (60 req/hr, 60s poll floor)"
    - "Run located by head_sha + path match (DEC-F), never by 'most recent run on branch', to avoid attributing an unrelated push's result to this work"

key-files:
  created:
    - .planning/phases/05-ci-final-verification/05-02-SUMMARY.md
  modified: []

key-decisions:
  - "Human resolved the Task 2 blocking-human checkpoint with 'push' — approved publishing all 106 local commits (including tracked .planning/ files) to the public fork, per D-09/D-10"
  - "DEC-D: CI-03's job-summary sub-claim split into content-correctness (machine-verified locally in Task 1), CI-execution-success (machine-verified via per-step conclusion in Task 3), and GitHub-rendering (one scoped human-check on the run's Summary tab, harvested at end-of-phase per workflow.human_verify_mode)"
  - "DEC-E not invoked — the pushed run went green on the first attempt, so no workflow-configuration fix or Active-list routing was needed"
  - "DEC-F: run located by filtering actions/runs?head_sha=<pushed SHA> to path == .github/workflows/tests.yaml, asserting exactly one match, before evaluating conclusion"

patterns-established:
  - "Red-CI-run classification (DEC-E) as a documented decision path for future CI plans, even though not exercised here"

requirements-completed: [CI-03, CI-04]

coverage:
  - id: D1
    description: "Fresh full-suite local run on ClickHouse latest x client V2 is green (371 tests, 0 failures, 0 errors, 7 skipped) with real coverage on the four fork classes that previously read 0.0%"
    requirement: "CI-04"
    verification:
      - kind: other
        ref: "Task 1 <verify>: env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test + jacocoForkReport.xml LINE-coverage assertion (executed by prior agent this session, not re-run)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The workflow's exact Generate coverage summary command, run verbatim against the freshly regenerated report, renders both headline rows and all 15 fork-class rows to both GITHUB_STEP_SUMMARY and stdout"
    requirement: "CI-03"
    verification:
      - kind: other
        ref: "Task 1 <verify>: python3 .github/scripts/jacoco_summary.py with GITHUB_STEP_SUMMARY set (executed by prior agent this session, not re-run)"
        status: pass
    human_judgment: false
  - id: D3
    description: "custom_build (106 commits, 79 tracked .planning/ files) pushed to git@github.com:ne1r0n/clickhouse-kafka-connect.git after an explicit blocking-human go/no-go naming the one-way public-visibility consequence"
    verification: []
    human_judgment: true
    rationale: "A one-way, permanently-public-visibility decision (T-05-06) requires an explicit human choice — this is the record of that approval ('push'), not something automation can self-certify."
  - id: D4
    description: "A push to custom_build triggered exactly one tests.yaml run for the pushed SHA (35317272594); it and its single job concluded success; the Run tests and Generate coverage summary steps both concluded success; both artifacts (jacoco-coverage-reports 618613 bytes, gradle-test-reports 1912272 bytes) are present and unexpired"
    requirement: "CI-04"
    verification:
      - kind: other
        ref: "Task 3 <verify>: unauthenticated GitHub REST API script (git@github.com:ne1r0n/clickhouse-kafka-connect actions/runs, /jobs, /artifacts endpoints) — see Task Commits below"
        status: pass
    human_judgment: false
  - id: D5
    description: "GitHub rendered the Coverage Summary as formatted markdown (heading, two headline rows, 15-row fork-class table) on the run's Summary tab"
    requirement: "CI-03"
    verification: []
    human_judgment: true
    rationale: "No unauthenticated GitHub REST API endpoint exists for a rendered job-summary page (verified in 05-RESEARCH.md); per DEC-D part 3 this is a single scoped human glance at https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/35317272594, harvested into UAT at end-of-phase (workflow.human_verify_mode=end-of-phase), not blocked on inline."

duration: ~20min (continuation session, resumed at Task 3 after human checkpoint)
completed: 2026-09-18
status: complete
---

# Phase 5 Plan 2: CI Push and Machine-Verified Green Run Summary

**Pushed `custom_build` to the public fork after human approval and machine-verified a fully green `tests.yaml` run (run 35317272594) on the pushed SHA via the unauthenticated GitHub REST API — exactly one job, both key steps and both artifacts confirmed.**

## Performance

- **Duration:** ~20 min (continuation session; Task 1 was completed and verified by a prior agent earlier this session and is not re-timed here)
- **Completed:** 2026-09-18T07:12:00Z (approx.)
- **Tasks:** 3 (Task 1 — carried forward complete; Task 2 — checkpoint resolved by human; Task 3 — executed this session)
- **Files modified:** 0 source files; 1 new file (this SUMMARY)

## Accomplishments

- **Task 1 (carried forward, verified by prior agent, not re-run):** A fresh `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` on ClickHouse `latest` with client V2 passed in full — 371 tests aggregated across 43 test classes, 0 failures, 0 errors, 7 pre-existing skips. `build/reports/jacoco/fork/jacocoForkReport.xml` showed non-zero LINE coverage for all four previously-0% fork classes (`TableOperationFilter`=57, `HeadersToValue`=74, `HeaderToValue`=77, `ConvertToMapOrJson`=108). `.github/scripts/jacoco_summary.py` run verbatim with `GITHUB_STEP_SUMMARY` set exited 0 and rendered the heading, both headline rows, and all 15 fork-class rows identically to both destinations. A pre-push scan of the 106-commit delta found 4 candidate credential-shaped strings, all confirmed false positives.
- **Task 2 (checkpoint, resolved this session):** Human replied "push" to the `blocking-human` go/no-go, explicitly approving the one-way publication of `custom_build` (all commits, tracked `.planning/` files included) to `git@github.com:ne1r0n/clickhouse-kafka-connect.git`.
- **Task 3 (executed this session):** Pushed `custom_build` to `origin` (`22f055c..a295af8`, fast-forward, no force, no history rewrite). Confirmed `git rev-parse origin/custom_build` equals `git rev-parse HEAD` (`a295af8aacef6c5c3ad0b49abe89a3a85ca4efa2`). Located the triggered run unauthenticated by filtering `actions/runs?head_sha=<pushed SHA>` to `path == .github/workflows/tests.yaml`, confirming exactly one match (id `35317272594`, DEC-F). Polled at 60-second intervals (7 polls, well under the 60-minute/60-request budget) until `status: completed`. Asserted, in order: run `conclusion: success`; exactly one job (`"ClickHouse latest Client version V2 tests"`) with `conclusion: success`; within that job's `steps`, `Run tests` and `Generate coverage summary` both `conclusion: success` (full step list: `Set up job`, `Run actions/checkout@v4`, `Set up JDK 17`, `Set up Gradle`, `Run tests`, `Generate coverage summary`, `Upload JaCoCo coverage reports`, `Upload test reports`, plus three `Post *`/`Complete job` steps — all `success`); artifacts `jacoco-coverage-reports` (618,613 bytes, `expired: false`) and `gradle-test-reports` (1,912,272 bytes, `expired: false`) both present. No `/logs` or artifact `/zip` endpoint was called; no token was minted; `gh` was not installed. Run was green on the first push — DEC-E's red-run classification path was not exercised.

## Task Commits

Task 1 and Task 3 were read-only / a remote `git push` (no local tracked-file changes on the happy path) — neither produced a task-level commit. The plan's only commit is the metadata commit below.

1. **Task 1: Fresh full-suite local run and a coverage report** — no commit (read-only verification; only gitignored `build/` outputs regenerated by the prior agent this session)
2. **Task 2: checkpoint:decision — public push go/no-go** — no commit (human decision recorded above)
3. **Task 3: Push, locate the run by its commit SHA, and machine-verify the green CI result** — no local commit (a `git push` of pre-existing commits `22f055c..a295af8`; no tracked file was modified on the happy path)

**Plan metadata:** committed immediately following this SUMMARY (see final commit in the executor's completion report).

## Files Created/Modified

- `.planning/phases/05-ci-final-verification/05-02-SUMMARY.md` - this summary, recording Task 1's carried-forward local evidence and Task 3's pushed-SHA / CI-run evidence

## Decisions Made

- Human approved "push" at the Task 2 `blocking-human` checkpoint — see `key-decisions` above.
- DEC-D's three-way split of CI-03's job-summary sub-claim (content locally machine-proven, execution-success machine-proven via per-step conclusion, rendering left to one scoped human-check harvested at end-of-phase) was followed exactly as authored in the plan.
- DEC-E (red-run policy) was not triggered — the run was green on the first push.
- DEC-F (locate by `head_sha` + `path`, assert exactly one match, 60s/60min poll budget) was followed exactly; exactly one matching run was found.

## Deviations from Plan

None - plan executed exactly as written. Task 1's evidence was carried forward from the prior agent's verified run per the resume instructions (re-running it was explicitly not required or wanted). Task 3 executed on the first attempt with a green result, so DEC-E's remediation path was never entered.

## Issues Encountered

None. The pushed commit triggered exactly one `tests.yaml` run, which completed green on the first attempt (~7 polling cycles, well inside the 60-minute budget).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- This is the last plan of the milestone (per the plan's own objective). `custom_build` is now public at `origin`, with a machine-verified green CI run on the pushed SHA.
- **Outstanding for end-of-phase UAT (workflow.human_verify_mode=end-of-phase):** one human-check remains — open `https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/35317272594` and visually confirm the rendered "Coverage Summary" markdown (heading, two headline rows, 15-row fork-class table) appears on the run's Summary tab. This is the one CI-03 sub-claim no unauthenticated API call can return (see D5 in the coverage block above).
- No new bugs surfaced; no follow-up plan required. The pre-existing `PROJECT.md` "Active" follow-up items (R-01 silent data loss, retriable-error classification dead code, and the three earlier-phase code-review items) remain open and unaffected by this plan's scope.

---
*Phase: 05-ci-final-verification*
*Completed: 2026-09-18*
