---
phase: 37-ci-hardening-advisory-debt
plan: "01"
subsystem: ci
tags: [github-actions, workflow, python, junit, fork-anchors, version-bump]

requires:
  - phase: 36-client-0-11-0-audit-bump
    provides: "the evidence helpers (byte copies), the Phase 36 evidence format and a green sync/v1.7.0 baseline"
provides:
  - "bump_version.py: strict vX.Y.Z / vX.Y.Z_ggN grammar, the gg/patch/minor/major rules, the VERSION-row edit of fork-anchors.txt, write-nothing-on-failure"
  - "increment-version.yml calling the script (input via env:, no Node, no npm), gg as the fourth dispatch option"
  - "BumpVersionScriptUnitTest (48 executions) and IncrementVersionWorkflowGuardUnitTest (16 executions), both Docker-free"
  - "fork-anchors.txt at rows=157 with the VERSION row kept in step by the bump"
affects: [37-02, 37-03, 37-04, 37-05, 38-release]

plan_head_before: ff8730860ba8560171fcda44bc1cdcd6dba1ba2a
plan_head_after: 22db7c13755a2f72b3be873af2dc9870dbbca375

actuals:
  tokens: 19783
  tasks: 3
  commits: 8

tech-stack:
  added: []
  patterns:
    - "python3 -I script run from a Docker-free JUnit class, paths passed as arguments into a @TempDir"
    - "line-level workflow guard with valid-list fixtures built by anchor-checked edit helpers"

key-files:
  created:
    - .github/scripts/bump_version.py
    - src/test/java/com/clickhouse/kafka/connect/ci/BumpVersionScriptUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/ci/IncrementVersionWorkflowGuardUnitTest.java
    - .planning/phases/37-ci-hardening-advisory-debt/37-COMMAND-EVIDENCE.md
  modified:
    - .github/workflows/increment-version.yml
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md

key-decisions:
  - "Assets copied unchanged (cmp-proven); the plan's O-4 rule is pinned by the table: patch/minor/major on a suffix-less version give _gg1"
  - "The table commit rebuilt the anchors template from helper methods (header, example row, VERSION row, Gradle row); the two tracer tests keep their text"

patterns-established:
  - "A workflow that moves an anchored file commits its fork-anchors.txt rows in the same commit (rows=157 bad=0 at every commit)"

requirements-completed: [CIH-01, CIH-02, CIH-03]

coverage:
  - id: D1
    description: "bump_version.py computes the next version under a strict grammar and writes VERSION and the VERSION anchors row together, refusing and writing nothing on any malformed text, mismatch or missing row"
    requirement: "CIH-01"
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/ci/BumpVersionScriptUnitTest.java (48 executions green, mut1 and mut2 seen red)"
        status: pass
    human_judgment: false
  - id: D2
    description: "increment-version.yml calls the script with the input through env:, installs no npm package, keeps its SHA pin, scopes and timeout, and is pinned line by line"
    requirement: "CIH-02"
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/ci/IncrementVersionWorkflowGuardUnitTest.java (16 executions green, mutation seen red)"
        status: pass
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/ci/WorkflowActionPinGuardUnitTest.java (21, unchanged)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The workflow's own Increment version step turns v1.7.0_gg5 into v1.7.0_gg6 in VERSION and in the VERSION anchors row on a throwaway worktree, the anchors mirror reads bad=0, and a VERSION-only bump reads bad=1"
    requirement: "CIH-03"
    verification:
      - kind: other
        ref: "37-COMMAND-EVIDENCE.md E-02 (e2e_* keys)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The workflow has not been run on GitHub: nothing is pushed or dispatched in this phase, so the GitHub-side behaviour of the rewritten workflow is not exercised"
    requirement: "CIH-02"
    verification: []
    human_judgment: true
    rationale: "A workflow_dispatch run needs the workflow on origin; Phase 37 pushes nothing, so the first real run belongs to a later, user-gated step"

duration: 17min
completed: 2026-10-31
status: complete
---

# Phase 37 Plan 01: Bump script, workflow wiring and guard Summary

**Version bumping moved from an npm `semver.inc` call (which wrote the text `null` for any `_ggN` version) into `bump_version.py`: a strict-grammar script that keeps `VERSION` and the `VERSION` anchors row in step, a 48-execution Docker-free table test, a 16-execution line-level guard of `increment-version.yml`, and the end-to-end proof on a throwaway worktree.**

## Performance

- **Duration:** about 17 min
- **Started:** 2026-10-31T08:47Z (approximate, first command)
- **Completed:** 2026-10-31T09:04Z
- **Tasks:** 3 (one tracer, two auto)
- **Files modified:** 7 (3 created in source, the evidence file, 3 edited)

## Accomplishments

- E-01 re-measured the untouched tree: Phase 36 complete, `v1.7.0_gg4` and `v1.7.0_gg5` tags at their SHAs, origin `custom_build` still equal to gg5, the anchors mirror at `rows=152 bad=0`, both helper copies byte-identical to Phase 36's, and a green cache-busted `testUnit` of 1475 tests (2 skipped). ClickHouse `latest` is 26.10.3.7.
- Tracer: `BumpVersionScriptUnitTest` started red (python3 could not open the missing script, `tests=2 failures=2`) and went green with `bump_version.py` copied unchanged from the asset. The workflow's own `Increment version` run block, run as GitHub runs it on a worktree of the real files, turned `v1.7.0_gg5` into `v1.7.0_gg6` in `VERSION` and in the `VERSION` anchors row and nothing else; the anchors mirror then read `rows=157 bad=0`, while a bump of `VERSION` alone read `bad=1`.
- Table: 48 executions (14 accepted bumps, 17 refused version texts, 4 unknown-bump usage errors, the no-argument run, 10 anchors-row fixtures, the 2 tracer tests), every refusal leaving both files byte-identical. Mutation 1 (the row-equality check deleted) turned exactly the tracer's mismatch fixture red; mutation 2 (the row rewrite skipped) turned 17 executions red.
- Guard: `IncrementVersionWorkflowGuardUnitTest` pins the script call with the input from `env:`, no `${{ }}` in a run block, no Node or npm, the add of `VERSION` with the anchors file, the job-level write scopes, the four options, the header and the checkout pin. 16 executions, green with `DOCKER_HOST` pointing nowhere; an env expression put into the commit line turned two tests red.
- Plan gate: `testUnit` 1539 tests (the 1475 baseline plus 48 plus 16), 0 failures, 0 errors; the two pin guards keep 21 and 39; anchors mirror `rows=157 bad=0`; no `src/main`, `VERSION`, `CHANGELOG.md`, gradle file or existing assertion changed.

## Task Commits

1. **Task 1 (tracer): pre-flight, script, workflow, anchors, docs, end to end**
   - `87351f11` docs: open the evidence (E-01)
   - `1f2ba9f3` ci: the script and the tracer test (script_commit)
   - `501d0696` ci: the workflow, the anchors rows and the FORK-DELTA row (workflow_commit)
   - `f04c9416` docs: record the tracer (E-02)
2. **Task 2: rule table, refused texts, usage errors, anchors fixtures, two mutations**
   - `56cc488c` test: the full table (table_commit)
   - `7bcbae05` docs: E-03
3. **Task 3: workflow guard, plan gate**
   - `b40263ed` test: the guard class (guard_commit)
   - `22db7c13` docs: E-04

## Commits

- `script_commit`: `1f2ba9f359e9819f6cf1707a5b5c13327914a69d`
- `workflow_commit`: `501d069644b452db7405aee96a9c9e9df056bce0` (the pointer for the Phase 30 ledger rows 30-IN-01 and 30-IN-02)
- `table_commit`: `56cc488c1dfafbe3b678c3b19c0d9e16e6c074f3`
- `guard_commit`: `b40263edb679a8e6588ec1a898244d735c0f116f`

## Files Created/Modified

- `.github/scripts/bump_version.py` - the bump script (asset, unchanged), mode 755
- `.github/workflows/increment-version.yml` - calls the script, no Node, `gg` option (asset, unchanged)
- `.planning/scripts/fork-anchors.txt` - the Node row replaced by six rows (152 to 157)
- `docs/FORK-DELTA.md` - the increment-version.yml row no longer calls the defect open
- `src/test/java/com/clickhouse/kafka/connect/ci/BumpVersionScriptUnitTest.java` - 48 executions
- `src/test/java/com/clickhouse/kafka/connect/ci/IncrementVersionWorkflowGuardUnitTest.java` - 16 executions
- `.planning/phases/37-ci-hardening-advisory-debt/37-COMMAND-EVIDENCE.md` - index E-01..E-18, entries E-01..E-04

## Decisions Made

- The assets were copied unchanged and proven with `cmp`; no asset behaved differently from its plan.
- Refused-text rows carry an escaped display label (a backslash followed by n, r, or u and four hex digits for a non-ASCII character or a space) so the test names stay readable; the content is passed separately.
- STATE.md, ROADMAP.md, REQUIREMENTS.md, `.planning/state.json` and `.planning/config.json` were not touched by this run, per the orchestrator's staging rules; requirements `CIH-01`, `CIH-02`, `CIH-03` are listed in `requirements-completed` for the orchestrator to mark after verification.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] The two Gradle verify commands carry an unquoted `tests>=N` expression**
- **Found during:** Task 1 verify (second `<automated>`), repeated in Task 2's.
- **Issue:** `junit ... tests>=2 ...` is a shell redirect in bash and zsh: the helper received no expression (exit 2) and the shell created a file named `=2` in the repository root.
- **Fix:** deleted the stray file (it was untracked and held only the helper's usage error), and ran both verify commands with the expression quoted (`'tests>=2'`, `'tests>=45'`). Nothing else in them changed.
- **Files modified:** none committed.
- **Verification:** `P37-01-T1-TESTS-OK` and `P37-01-T2-TESTS-OK` print; `git status` shows only the two orchestrator-owned files.
- **Committed in:** not applicable.

**2. [Rule 3 - Blocking] The guard class's Javadoc could not say "extends nothing"**
- **Found during:** Task 3 verify (first `<automated>`).
- **Issue:** the verify greps the committed class for `extends ` as a superclass check, and that pattern also matches the sentence the plan asks the Javadoc to contain.
- **Fix:** the Javadoc says "Deliberately has no superclass - deriving from ClickHouseBase would inherit its clickhouse tag". The test file (`BumpVersionScriptUnitTest`) keeps the house wording because its verify greps for `extends ClickHouseBase`. For `extends_nothing` in E-04 I used a class-declaration grep that matches only a real superclass clause.
- **Files modified:** `IncrementVersionWorkflowGuardUnitTest.java` (before its commit).
- **Verification:** `P37-01-T3-OK`.
- **Committed in:** `b40263ed`.

**3. [Plan wording] Origin now has two `scratch/*` refs**
- **Found during:** Task 1 (E-01).
- **Issue:** the planning-time research found none; `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6` exist (the Phase 36 CLT-07 refs). The truth says "the absent scratch refs".
- **Fix:** recorded as `origin_scratch_refs` in E-01, not a STOP (this phase merges and pushes nothing). `origin_custom_build` still equals the gg5 tag.
- **Committed in:** `87351f11`.

**4. [Refactor, no behaviour change] Fixture helpers replaced the tracer's anchors template constant**
- **Found during:** Task 2.
- **Issue:** the 10 anchors-row fixtures need the template's rows individually (a decoy row, a missing row, a doubled row); one concatenated constant would have been repeated.
- **Fix:** the template is built from `ANCHORS_HEADER`, `EXAMPLE_ROW`, `versionRow(...)` and `GRADLE_ROW`; the two tracer tests keep their text and assertions, and `anchorsFor(...)` yields the same bytes.
- **Committed in:** `56cc488c`.

---

**Total deviations:** 4 (2 blocking verify-command issues, 1 premise change recorded, 1 DRY refactor). **Impact on plan:** none on scope or results; no src/main, dependency, assertion or origin change.

## Issues Encountered

- The workflow asset's header comment says `ReleaseWorkflowGuardUnitTest` pins the file, but the class that pins `increment-version.yml` is `IncrementVersionWorkflowGuardUnitTest` (plan 37-03 refers to the latter as the house style). The asset is copied unchanged by rule, so the comment is inexact; fixing it means editing the workflow, its asset and re-checking the anchors, which no anchored text depends on. Flagged for the user or plan 37-03.
- The first run of `testUnit` after each edit takes about 6 seconds; no OpenJ9 crash, no core dump or javacore appeared in any run.

## Known Stubs

None.

## Threat Flags

None. The workflow keeps its scopes, pin and timeout; no new endpoint, auth path or file access appears.

## User Setup Required

None - no external service configuration required. The rewritten workflow has not run on GitHub (nothing is pushed in this phase).

## Handoffs

- **Plan 37-02:** the E-01 baselines are `testUnit` 1475 tests (2 skipped) before this plan and 1539 after it; class tallies at E-01: `TestsWorkflowPinGuardUnitTest` 39, `WorkflowActionPinGuardUnitTest` 21, `FailedTestsSummaryScriptUnitTest` 8, `UpstreamIntegrationWorkflowBytesGuardUnitTest` 9, `ConfluentArchiveCheckUnitTest` 6, `ConnectApiReferencesUnitTest` 3, `ColumnTest` 50, `DescribeUnreadableEnumMockServerTest` 2. The new classes add `BumpVersionScriptUnitTest` 48 and `IncrementVersionWorkflowGuardUnitTest` 16.
- The anchors count is now `rows=157` (171 expected after plan 37-03, 172 after plan 37-04). `HEAD` after this plan is the commit that carries this SUMMARY; `local_head` of plan 37-02's start is that commit.
- FORK-DELTA rows 2 to 5 of the assets land in plans 37-02 (compare), 37-03 (release compare, release guard) and 37-04 (byte guard / summary script); row 1 landed here.
- Scratch rules: logs under `/tmp/gsd37/exec/`, worktrees under `/tmp/gsd37/w/<name>` with names that do not exist yet, all removed after use (`git worktree list` shows one entry); the `=2` redirect trap applies to every `junit ... tests>=N` call, so quote it.
- Never stage `.planning/config.json`, `.planning/state.json` or `.planning/STATE.md`; origin and GitHub stay read-only.

## Next Phase Readiness

Plan 37-02 can start: the tree is clean apart from the orchestrator-owned state files, the evidence index E-01..E-04 reads pass and E-05..E-18 are pending, and the anchors mirror reads `rows=157 bad=0`.

## Self-Check: PASSED

- Files: `.github/scripts/bump_version.py`, `.github/workflows/increment-version.yml`, both test classes and the evidence file exist; `bump_version.py` and the workflow equal their assets (`cmp`).
- Commits `87351f11`, `1f2ba9f3`, `501d0696`, `f04c9416`, `56cc488c`, `7bcbae05`, `b40263ed`, `22db7c13` are ancestors of `HEAD`.
- All six plan `<automated>` checks print their OK marker (`P37-01-T1-OK`, `P37-01-T1-TESTS-OK`, `P37-01-T2-OK`, `P37-01-T2-TESTS-OK`, `P37-01-T3-OK`, `P37-01-T3-TESTS-OK`; with the quoting fix for the two `tests>=N` calls).

---
*Phase: 37-ci-hardening-advisory-debt*
*Completed: 2026-10-31*
