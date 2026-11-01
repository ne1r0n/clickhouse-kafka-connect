---
phase: 39-dependabot-absorption-behaviour-pins
plan: "01"
subsystem: infra
tags: [dependabot, gson, gradle, version-catalog, fork-delta, merge-invariants, testcontainers]

# Dependency graph
requires:
  - phase: 38-release-publication
    provides: "the published gg6 tip 72e8ccff (origin custom_build) that both Dependabot PR heads are based on, and the Phase 38 evidence helper"
provides:
  - "gson 2.15.0 on runtimeClasspath, absorbed as its own --no-ff merge commit ac216401 (second parent = PR #22 head b5cccbd3)"
  - "FORK-DELTA version-catalog row naming gson 2.15.0 and the PR #22 absorption, changed in the same merge commit"
  - "39-COMMAND-EVIDENCE.md with E-01..E-03 and the 39-evidence-check.py helper (byte copy of Phase 38's)"
  - "a green, attributable gson tip: testUnit and full suite twice each on latest x V2, cumulative merge-invariant gate PASS"
affects: [39-02, 39-03, 39-04, 44-release-rehearsal]

requires_next:
  - "39-02 reads E-01 phase_start_sha, origin_custom_build, absorption_mode and setup_gradle_source"

# Actuals (#2632): chars/4 over the realized diff (STATE.md excluded)
actuals:
  tokens: 11000
  tasks: 3
  commits: 6
plan_head_before: 8d9d1a17cb729efba591f7470776a68a6e9edf5a
plan_head_after: a58b18b60bdc19b0a855e9f838e7bbffd0e73419

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Dependabot bump absorbed as a --no-ff --no-commit merge of the fetched PR head, with the FORK-DELTA row edited in the same commit"
    - "Per-bump gate before any new test lands, so a red is attributable to one change"

key-files:
  created:
    - .planning/phases/39-dependabot-absorption-behaviour-pins/39-COMMAND-EVIDENCE.md
    - .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py
  modified:
    - gradle/libs.versions.toml
    - docs/FORK-DELTA.md

key-decisions:
  - "Absorption mode pr-heads: origin custom_build still 72e8ccff and PRs #21 and #22 open and unmerged at execution start, so the PR #22 head was merged, not an origin commit"
  - "The cumulative merge-invariant gate was started on the merge commit's explicit SHA, after the E-02 evidence commit and before the first test run, and no commit was made until it ended"

patterns-established:
  - "Evidence helper h() is run under /bin/bash: in zsh h is an alias for history and the function definition fails"

requirements-completed: [DEP-01]

coverage:
  - id: D1
    description: "gson 2.15.0 resolves on runtimeClasspath and clickhouseDependencies (dependencyInsight), from PR #22's head merged as its own commit"
    requirement: DEP-01
    verification:
      - kind: other
        ref: "gradlew -q dependencyInsight --dependency com.google.code.gson:gson --configuration runtimeClasspath (39-COMMAND-EVIDENCE.md E-02)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The merge commit's first-parent diff is exactly gradle/libs.versions.toml (gson line) and docs/FORK-DELTA.md (one removed, one added line)"
    requirement: DEP-01
    verification:
      - kind: other
        ref: "git diff --name-only ac216401^1 ac216401 and git diff --numstat (39-COMMAND-EVIDENCE.md E-02, P39-01-T2-OK)"
        status: pass
    human_judgment: false
  - id: D3
    description: "testUnit (Docker unreachable) and the full suite on ClickHouse latest x client V2 each pass twice on the gson tip, second run with --rerun, with the 18 named skips"
    requirement: DEP-01
    verification:
      - kind: integration
        ref: "gradlew testUnit and gradlew test, 1672 and 2270 tests, 0 failures, 0 errors (39-COMMAND-EVIDENCE.md E-03, P39-01-T3-OK)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Cumulative verify-merge-invariants.sh gate on the gson merge commit ends result=PASS with exit 0"
    requirement: DEP-01
    verification:
      - kind: other
        ref: "bash .planning/scripts/verify-merge-invariants.sh 54e63648 ac216401a1efef629dd543fdabe49f3144b9af39 (E-03 invariants_line)"
        status: pass
    human_judgment: false

# Metrics
duration: 40min
completed: 2026-11-01
status: complete
---

# Phase 39 Plan 01: Execution-start re-read and gson 2.15.0 absorption Summary

**gson 2.15.0 absorbed from Dependabot PR #22 as its own `--no-ff` merge with its FORK-DELTA row, proven on runtimeClasspath and green twice on testUnit (1672 tests) and the full suite on latest x V2 (2270 tests, 18 named skips), with the cumulative merge-invariant gate PASS.**

## Performance

- **Duration:** 40 min (the cumulative gate alone took 37 min and overlapped the four Gradle runs)
- **Started:** 2026-11-01T17:51:40Z
- **Completed:** 2026-11-01T18:32Z
- **Tasks:** 3
- **Files modified:** 4 (the evidence file and helper created; the catalog and FORK-DELTA changed through the merge)

## Accomplishments

- Execution-start re-read (E-01): `origin/custom_build` still `72e8ccff`, PRs #21 and #22 open with `merged_at` null, heads unchanged since the research, so the absorption mode is `pr-heads`. The evidence file, its 11-row index and the byte-copied helper were created.
- Tracer (E-02): the PR #22 head (`b5cccbd3`, dependabot[bot], one parent `72e8ccff`, one changed line `gson 2.14.0 -> 2.15.0`) was merged with `--no-ff --no-commit`, the gson value, the Phase 39 absorption sentence and `DEP-01` were edited in the FORK-DELTA version-catalog row, and the merge `ac216401` carries both. Its first-parent diff is exactly `docs/FORK-DELTA.md` and `gradle/libs.versions.toml`; `dependencyInsight` resolves `com.google.code.gson:gson:2.15.0` on `runtimeClasspath` and `clickhouseDependencies`.
- Gate (E-03): `testUnit` with Docker unreachable twice (1672 tests, 2 skipped) and the full suite on ClickHouse `latest` (26.10.3.7, digest equal to the research's) x V2 twice (2270 tests, 18 skipped, `OK skips: named=7 state_mismatch_cases=11 unexpected=0`), second runs with `--rerun`, all with 0 failures and 0 errors; the cumulative merge-invariant gate on `ac216401` ended `result=PASS`, exit 0. Totals equal the research's measurement on a throwaway copy.

## Task Commits

1. **Task 1: Execution-start re-read, absorption mode, environment, evidence file (E-01)** - `4c463a87` (docs)
2. **Task 2 (tracer): gson merged as its own commit, runtimeClasspath proof (E-02)** - merge `ac216401` (the gson merge, second parent `b5cccbd3`), evidence `f2d54175` (docs)
3. **Task 3: gson gate (E-03)** - `a58b18b6` (docs)

**Plan metadata:** the SUMMARY commit, followed by the STATE and ROADMAP commit (docs).

The measured `commits: 6` counts `plan_head_before..plan_head_after`: the four commits above plus the two absorbed ones, the Dependabot head `b5cccbd3` and the gg6 publication merge `72e8ccff`, which the merge brought into this branch's history.

## Files Created/Modified

- `.planning/phases/39-dependabot-absorption-behaviour-pins/39-COMMAND-EVIDENCE.md` - Phase 39 evidence: header, 11-row index, E-01 to E-03
- `.planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py` - read-only gate helper, byte copy of Phase 38's, never edited
- `gradle/libs.versions.toml` - `gson = "2.15.0"` (arrived through the merge)
- `docs/FORK-DELTA.md` - version-catalog row: gson `2.15.0`, Phase 39 absorption sentence citing E-02, `DEP-01` in the requirement cell (one line changed, in the merge commit)

## Decisions Made

- Mode `pr-heads` was chosen from a fresh read, not from the research: origin had not moved and neither PR was merged.
- The cumulative gate ran on the merge commit's explicit SHA rather than HEAD, and no commit was made while it ran, as the plan requires; it started after the E-02 evidence commit because that commit is the gated head.

## Deviations from Plan

None - plan executed exactly as written.

Two execution notes that are not deviations: `h` is an alias for `history` in this zsh, so the plan's `h() {...}` function definition fails there and every helper call ran under `/bin/bash -c` or as a direct `python3 -I` call; and the cumulative gate took about 37 minutes against the research's 17 to 23, because it overlapped four Gradle runs on a swap-pressured host. Neither changed a result.

## Issues Encountered

None. No JVM crash file appeared in the repository root, and no run needed a retry.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None.

## Threat Flags

None. The plan changes one existing Maven coordinate's version and writes local evidence; no new endpoint, auth path, file access pattern or schema surface appears. Nothing was pushed and no pull request or GitHub resource was modified (only unauthenticated REST GETs and `git fetch` of named refs).

## Next Phase Readiness

- Ready for plan 39-02 (setup-gradle 6.4.1, PR #21): the tree holds gson 2.15.0 behind its own commit and gate; E-01 holds `phase_start_sha`, `origin_custom_build`, `absorption_mode: pr-heads` and `setup_gradle_source: refs/pull/21/head`.
- Origin must still read `72e8ccff` when 39-02 starts (origin re-read rule).

## Self-Check: PASSED

- Created files exist: the evidence file and the helper.
- Commits exist and are ancestors of HEAD: `4c463a87`, `ac216401`, `f2d54175`, `a58b18b6`, `b5cccbd3`.
- Task automated verifications printed `P39-01-T1-OK`, `P39-01-T2-OK` and `P39-01-T3-OK`; `src/main` is unchanged against `phase_start_sha`; the catalog diff against `phase_start_sha` is the one gson line.

---
*Phase: 39-dependabot-absorption-behaviour-pins*
*Completed: 2026-11-01*
