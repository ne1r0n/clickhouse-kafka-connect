---
phase: 11-full-suite-ci-publication
plan: 02
subsystem: ci
tags: [github-actions, ci, clickhouse-version, javadoc, test-fixtures]

requires:
  - phase: 10-version-default-restoration
    provides: ClickHouseVersionDefaultUnitTest Docker-free canary and the restored `latest` default
provides:
  - tests.yaml Run tests step with no job-environment version pins
  - ClickHouseTestHelpers D-04 Javadoc describing the post-change world
  - Docker-free evidence that the code default resolves with both variables unset
affects: [11-04 local gate, 11-05 scratch-branch CI run, 11-07 CI-05 closure]

actuals:
  tokens: 900
  tasks: 2
  commits: 1
plan_head_before: 901416c5699861482ecdc1df3854293b6c06715c
plan_head_after: 51119b262b27de86016235e8b22fa32799d7b7a1

tech-stack:
  added: []
  patterns:
    - "CI resolves the same code defaults as a developer with nothing set; a Docker-free canary, not an env pin, guards the default"

key-files:
  created: []
  modified:
    - .github/workflows/tests.yaml
    - src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java

key-decisions:
  - "Dropped both CLICKHOUSE_VERSION and CLIENT_VERSION from the CI job environment (D-09): extractClientVersion() already returns V2 for anything but literal V1, so the client pin is redundant on the same argument as the server pin."
  - "Left the FORK MODIFICATION header in tests.yaml unedited: it makes a behavioural claim and names no environment variables."

patterns-established:
  - "A comment that asserts a configuration is corrected in the same commit that removes the configuration"

requirements-completed: [CI-05]

coverage:
  - id: D1
    description: "The Run tests step in tests.yaml carries no env block; both job-environment pins are gone and trigger, timeout, command and coverage steps are untouched"
    requirement: CI-05
    verification:
      - kind: other
        ref: "grep -v '^[[:space:]]*#' .github/workflows/tests.yaml | grep -cE 'CLICKHOUSE_VERSION|CLIENT_VERSION' returns 0; seven must-survive constructs present"
        status: pass
    human_judgment: false
  - id: D2
    description: "ClickHouseTestHelpers Javadoc no longer asserts a CI pin; constants and canary reference survive; commit touches exactly two files"
    requirement: CI-05
    verification:
      - kind: other
        ref: "negative grep for 'CI pins'; positive greps for the three constants and canary name; git diff --name-only on the commit equals the two planned paths"
        status: pass
    human_judgment: false
  - id: D3
    description: "ClickHouseVersionDefaultUnitTest and the full Docker-free testUnit task pass with both job-environment variables unset"
    requirement: CI-05
    verification:
      - kind: unit
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE -u CLICKHOUSE_VERSION -u CLIENT_VERSION ./gradlew testUnit --tests '*ClickHouseVersionDefaultUnitTest*' --rerun"
        status: pass
      - kind: unit
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE -u CLICKHOUSE_VERSION -u CLIENT_VERSION ./gradlew testUnit --rerun"
        status: pass
    human_judgment: false
  - id: D4
    description: "The CI runner resolves the same code default as the local run"
    requirement: CI-05
    verification: []
    human_judgment: true
    rationale: "Not provable locally; this is the first CI run that exercises the code default and is settled by plan 11-05's scratch-branch run"

duration: 12min
completed: 2026-10-07
status: complete
---

# Phase 11 Plan 02: Drop CI Version Pins Summary

**CI no longer pins the ClickHouse server or client version in its job environment, so it exercises the same code defaults as a local run, with the Docker-free `ClickHouseVersionDefaultUnitTest` as the detector, and the Javadoc that asserted the pin is corrected in the same commit.**

## Performance

- **Duration:** about 12 min
- **Completed:** 2026-10-07T10:42Z
- **Tasks:** 2 (1 committed, 1 evidence-only by design)
- **Files modified:** 2

## Accomplishments

- Removed the whole `env:` mapping (`CLICKHOUSE_VERSION: latest`, `CLIENT_VERSION: V2`) from the `Run tests` step; the step is now `name` plus `run: ./gradlew test`.
- Rewrote the D-04 Javadoc clause in `ClickHouseTestHelpers` to past tense ("CI used to set ...") and to state that CI and local runs now both resolve through `CLICKHOUSE_VERSION_DEFAULT`.
- Proved Docker-free, with both variables explicitly unset, that the canary and the whole `testUnit` task pass.

## Measured Evidence (Task 2)

| Measurement | Value |
|---|---|
| Filtered canary run (`*ClickHouseVersionDefaultUnitTest*`, `--rerun`) | 1 test executed, 1 succeeded, 0 failed, 0 skipped |
| Full `testUnit` (`--rerun`) | 327 tests executed, 325 succeeded, 0 failed, 2 skipped (total meets the 327 floor; 2 skips are pre-existing `Assumptions`-style skips) |
| Resolved default asserted by the canary, verbatim | `"latest"` (both `CLICKHOUSE_VERSION_DEFAULT` and `CLICKHOUSE_VERSION_LATEST`) |
| Environment | `env -u TESTCONTAINERS_HOST_OVERRIDE -u CLICKHOUSE_VERSION -u CLIENT_VERSION` |

**Stated inference, not a measurement:** this task does NOT prove that the CI runner resolves the same default. That is unproven until plan 11-05's scratch-branch run executes, and that run is the first CI run that will ever exercise the code default rather than a pinned environment. Plan 11-05's evidence bundle is what confirms it; this local run does not settle it.

## Task Commits

1. **Task 1: Drop the job-environment pins and correct the Javadoc** - `51119b26` (ci)
2. **Task 2: Prove the code default, Docker-free** - no commit by design (evidence only)

**Plan metadata:** recorded in the docs commit following this summary.

## Files Created/Modified

- `.github/workflows/tests.yaml` - `Run tests` step no longer sets a server or client version
- `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` - D-04 Javadoc corrected; no code change

## Decisions Made

- Dropped both variables, not only the server pin, per CONTEXT.md D-09's stated default.
- Did not edit the `FORK MODIFICATION` header; it names no environment variables, so D-09's conditional clause is moot (re-verified against the literal file).
- Spotless: `spotlessApply` leaves the edited helper byte-identical, so it satisfies Spotless as-is.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Process slip] Tree-wide `spotlessApply` ran by accident and was reverted**
- **Found during:** Task 1 (Spotless step)
- **Issue:** `-PspotlessFiles=<regex>` was ignored by this Spotless version, so `spotlessApply` reformatted 45 unrelated files under `src/` (pre-existing import-order violations, tree-wide). This violated the plan prohibition on a tree-wide apply.
- **Fix:** Every one of those files had been clean in `git status` before the run, so each was restored with a path-specific `git checkout -- <file>` (no blanket reset). The helper file was verified byte-identical before and after the apply, proving the edit itself is Spotless-compliant. Nothing from the reformat reached a commit.
- **Files modified:** none net
- **Verification:** `git diff --stat` afterwards showed only the two planned files plus pre-existing orchestrator `.planning/` changes; the commit file list assertion `SINGLE_BEHAVIOUR_COMMIT_OK` passed.
- **Committed in:** n/a (reverted before commit)

---

**Total deviations:** 1 auto-fixed (1 process slip, fully reverted)
**Impact on plan:** None on the result. Lesson recorded: this repo's `spotlessApply` cannot be file-scoped via `-PspotlessFiles`; use `spotlessCheck` plus a before/after diff of the single file instead.

## Issues Encountered

- `spotlessCheck` fails tree-wide on 44 files for pre-existing import-order violations (not introduced here, not in the edited file). Left alone per the single-behaviour-diff property.

## Deferred Issues

- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseVersionDefaultUnitTest.java` line 12 still says "CI pins `CLICKHOUSE_VERSION: latest` in its job environment". That is now stale in the same way the Javadoc was. The plan scopes this commit to exactly two files and prohibits touching tests beyond that, so it was not edited. A one-line comment correction belongs in a later plan of this phase (11-04 or 11-07 are the natural homes) or the 11-07 closure sweep.

## Threat Flags

None. No new network endpoint, auth path, file access pattern or schema surface; the edit removes configuration.

## Next Phase Readiness

- Ready for 11-03. Plan 11-04's local gate will run on a tree where CI and local defaults agree.
- Plan 11-05 must confirm CI's own resolution of the `latest` default (see inference above).

## Self-Check: PASSED

- `.github/workflows/tests.yaml` and `ClickHouseTestHelpers.java` exist and carry the edits.
- Commit `51119b26` is an ancestor of HEAD; `git rev-list --count 901416c5..HEAD` measured 1 at the time of the task commit.
- All three Task 1 automated checks passed; both Task 2 runs passed; no `src/main` file modified.

---
*Phase: 11-full-suite-ci-publication*
*Completed: 2026-10-07*
