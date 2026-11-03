---
phase: 37-ci-hardening-advisory-debt
plan: "03"
subsystem: ci
tags: [github-actions, release-workflow, archive-compare, junit, fork-anchors, dependabot]

requires:
  - phase: 37-ci-hardening-advisory-debt
    provides: "plan 37-02: compare_confluent_archives.py, baseline_tag.py, the allow-lists named by VERSION, the scratch archives"
provides:
  - "release.yaml and pre-release.yaml run select-baseline, build-baseline-from-tag, compare and report-upload steps before the tag gate; dry runs get their own concurrency group"
  - "fork-anchors.txt at rows=171 (dry-run expression rows 5 to 6, 14 new rows)"
  - "ReleaseWorkflowGuardUnitTest: 34 Docker-free executions pinning the release safety gates, the compare chain and the twin equality"
  - "TestsWorkflowPinGuardUnitTest: stricter archive-step rules and a separate archive-before-tests rule (39 to 44 executions, additions only)"
  - "E-09, E-10, E-11 (pass) and E-12 (pending, user-gated); 37-USER-DISPATCH-DRYRUN.md"
affects: [37-04, 37-05, 38-release]

plan_head_before: 256fa2849ecbe8f512392a4126ea82274a17fd30
plan_head_after: 44b717a624e6fa35ba39a4524c0a55ce574d75ad

actuals:
  tokens: 28004
  tasks: 3
  commits: 5

tech-stack:
  added: []
  patterns:
    - "line-level workflow guard parameterised over the two release files, with anchor-checked fixture helpers"
    - "local replay of workflow run blocks extracted from the committed YAML under the runner's bash options"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java
    - .planning/phases/37-ci-hardening-advisory-debt/37-USER-DISPATCH-DRYRUN.md
  modified:
    - .github/workflows/release.yaml
    - .github/workflows/pre-release.yaml
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
    - .github/dependabot.yml
    - .planning/phases/37-ci-hardening-advisory-debt/37-COMMAND-EVIDENCE.md

key-decisions:
  - "The four workflow edits, the anchors rows and the FORK-DELTA rows come from the plan assets and the tested line editor, unchanged"
  - "The compare rule also requires the line exit \"$status\", so a step that captures the status and then ignores it is rejected (stricter than the plan's wording)"
  - "The increment-version.yml header comment (it names ReleaseWorkflowGuardUnitTest instead of IncrementVersionWorkflowGuardUnitTest) was left unfixed: the plan forbids editing that file and its T1 verify requires it unchanged"

patterns-established:
  - "A workflow change and its anchors rows land in one commit; the guard class lands separately with its three red mutations"

requirements-completed: [CIH-04, ADV-01]

coverage:
  - id: D1
    description: "Both release workflows select the baseline tag, build its archive in a separate worktree with the tag's own wrapper, compare it under the allow-list named by VERSION and upload the report, before the tag gate; a dry run has a per-run concurrency group and the checkout fetches the full history"
    requirement: "CIH-04"
    verification:
      - kind: unit
        ref: "TestsWorkflowPinGuardUnitTest, WorkflowActionPinGuardUnitTest, IncrementVersionWorkflowGuardUnitTest (76 then 81 executions green on the edited workflows); anchors mirror rows=171 bad=0; 37-COMMAND-EVIDENCE.md E-09"
        status: pass
    human_judgment: false
  - id: D2
    description: "ReleaseWorkflowGuardUnitTest pins permissions, runner, checkout, the publish and dry-run conditions, the compare chain order and strictness, the report upload, the concurrency group, no continue-on-error and the twin equality; three mutations (advisory compare, drifted PUBLISH value, one twin changed) each turned it red"
    requirement: "ADV-01"
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java (34 executions green with Docker unreachable; gmut1 3 failures, gmut2 2, gmut3 2); 37-COMMAND-EVIDENCE.md E-10"
        status: pass
    human_judgment: false
  - id: D3
    description: "The archive step of tests.yaml is pinned against a skipped task, a dry run, a swallowed exit status and a step condition, and before the tests; the dependabot comment names only what the guard checks"
    requirement: "ADV-01"
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java (44 executions, 5 added, existing fixtures unedited)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The run blocks of the three new compare steps, extracted from the committed release.yaml and run under bash -eo pipefail, choose v1.7.0_gg4, fail closed on a missing allow-list and a missing baseline, build the baseline from the tag with its Gradle 9.8.1 wrapper, fail red on the tip's archive (238 unallowed) and pass green on the gg5 rebuild; the three blocks are identical in both workflows"
    requirement: "CIH-04"
    verification:
      - kind: other
        ref: "37-COMMAND-EVIDENCE.md E-11"
        status: pass
    human_judgment: false
  - id: D5
    description: "The steps have not run on a GitHub runner: the checkout fetch, the tag worktree under setup-gradle and the artifact upload need one CI dry run, prepared as a handoff for the user and expected RED at the compare step"
    requirement: "CIH-04"
    verification: []
    human_judgment: true
    rationale: "A CI dry run needs a scratch branch pushed to origin and a workflow dispatch, both the user's; E-12 stays pending and the expected red run is never counted as a pass"

duration: 19min
completed: 2026-10-31
status: complete
---

# Phase 37 Plan 03: Release workflow compare, guard and dry-run handoff Summary

**`release.yaml` and `pre-release.yaml` now compare the built archive with one rebuilt from the previous release tag before anything is published (select baseline, build it in a tag worktree, compare under the allow-list named by `VERSION`, upload the report), `ReleaseWorkflowGuardUnitTest` pins the release safety gates and that chain in 34 Docker-free executions with three red mutations, the replay of the new steps under the runner's shell options passes in both polarities, and the one CI dry run is a handoff for the user that expects RED.**

## Performance

- **Duration:** about 19 min
- **Started:** 2026-10-31T09:10Z (approximate, first command)
- **Completed:** 2026-10-31T09:30Z
- **Tasks:** 3 (all auto)
- **Files modified:** 9 in the diff (2 created, 7 modified)

## Accomplishments

- E-09: the line editor applied the four edits to both workflows (header paragraph, per-run dry-run concurrency group, `fetch-depth: 0`, the four steps between `Check archive` and the tests.yaml gate), the 14 anchors rows and the FORK-DELTA row, all exit 0. The dry-run expression rose from 5 to 6 in each file, every other gate count held (publish expression 3, archive call 1, release create 2, continue-on-error 0), both files parse as YAML, the twin diff is 16 lines before and after, and the anchors mirror reads `rows=171 bad=0`. The three existing pin guard classes pass on the edited text.
- E-10: `ReleaseWorkflowGuardUnitTest` has 34 executions (20 real-file executions over the two workflows, the twin equality test, 13 fixture tests), green with `DOCKER_HOST` pointing nowhere. Mutations of a copy at the guard commit: the compare made advisory failed 3 executions, one drifted `PUBLISH:` value failed 2, the `fetch-depth: 0` line removed from one twin failed 2. `TestsWorkflowPinGuardUnitTest` went from 39 to 44 executions: `archiveStepProblem` also rejects `-x`, `--exclude-task`, `--dry-run`, ` -m `, `|| true`, `|| :`, `|| exit 0` and a step `if:`, and `archiveBeforeTestsProblem` pins the order; no existing assertion or fixture changed. Spotless changes nothing in either test file. The dependabot comment now names only the exclude-paths of its entry.
- E-11: the three run blocks were extracted from the committed `release.yaml` and run from the repository root with `bash --noprofile --norc -eo pipefail`. Select chose `v1.7.0_gg4` and `confluent-archive-allowlist-v1.7.0_gg5.txt`; `v1.7.0_gg6` failed closed on the missing list, `v0.0.1` on the missing baseline. The baseline build made a worktree of the tag, used the tag's Gradle 9.8.1 wrapper (`BUILD SUCCESSFUL in 11s`, an archive of 12453558 bytes), and was removed afterwards. Compare against the tip's archive exited 1 with an error annotation of 238 unallowed lines (the E-07 number) and `RESULT: FAIL`; against the gg5 rebuild it exited 0 with a notice of 0 unallowed and 0 stale rows and `RESULT: PASS`. The blocks are byte-identical in both workflows.
- E-12: `37-USER-DISPATCH-DRYRUN.md` carries the checks before the push, the push command, the dispatch, the fields to report, the clean-up and the E-19 resume instruction, and states under `Expected result: RED` that the run is expected to fail at the compare step and is never counted as a pass. E-12 reads pending (user-gated); nothing was pushed or dispatched.
- Plan gate: cache-busted `testUnit` 1624 tests (the 1585 of plan 37-02 plus 34 plus 5), 0 failures, 0 errors, 2 skipped; anchors mirror `rows=171 bad=0`; no `src/main`, `VERSION`, `CHANGELOG.md`, gradle file, `tests.yaml`, nightly, upstream workflow or `increment-version.yml` change; `git ls-remote origin` shows no ref created by this plan.

## Task Commits

1. **Task 1: the four workflow edits, anchors rows, FORK-DELTA row**
   - `44b8fb62` ci: both release workflows (workflows_commit)
   - `bd512e9b` docs: E-09
2. **Task 2: the guard class, the stricter archive rules, the dependabot comment, three mutations**
   - `fb63b0e9` test: the guard commit (guard_commit)
   - `4bc3454f` docs: E-10
3. **Task 3: the local replay and the handoff**
   - `44b717a6` docs: E-11, E-12 and `37-USER-DISPATCH-DRYRUN.md`

## Commits

- `workflows_commit`: `44b8fb62c2177558ff38d075eef9401a4426286a` (the pointer for Phase 30 WR-01)
- `guard_commit`: `fb63b0e9cf477d34f0830cd30e8c17132c660f22` (the pointers for Phase 30 WR-02, WR-03 and IN-03 and for the Phase 29 close record row 25 WR-01)

## Files Created/Modified

- `.github/workflows/release.yaml`, `.github/workflows/pre-release.yaml` - the compare chain, the per-run dry-run group, the full-history checkout (identical in both, 62 lines each)
- `.planning/scripts/fork-anchors.txt` - 157 to 171 rows
- `docs/FORK-DELTA.md` - the release-compare row and the release-guard row
- `src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java` - new, 34 executions
- `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java` - additions only (148 lines)
- `.github/dependabot.yml` - one comment sentence
- `.planning/phases/37-ci-hardening-advisory-debt/37-COMMAND-EVIDENCE.md` - entries E-09 to E-12, index rows flipped
- `.planning/phases/37-ci-hardening-advisory-debt/37-USER-DISPATCH-DRYRUN.md` - the user-gated handoff

## Decisions Made

- The workflow edits, anchors rows and FORK-DELTA rows are the plan assets applied by the tested line editor; the assets are unchanged (`git diff --quiet` against the phase start).
- The compare rule requires the exact call with only the report redirection and status capture after it, and also the line `exit "$status"`. This is stricter than the plan's wording and each side has a fixture.
- STATE.md, ROADMAP.md, REQUIREMENTS.md, `.planning/state.json` and `.planning/config.json` were not touched by this run, per the orchestrator's staging rules; `CIH-04` and `ADV-01` are listed in `requirements-completed` for the orchestrator to mark after verification. CIH-04's optional CI dry run is the user's step and is not counted as a pass.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] The FORK-DELTA insertion prefix of 40 characters is ambiguous**
- **Found during:** Task 2 (the `insert-after` of the release-guard row).
- **Issue:** the first 40 characters of the compare row also start the existing release-workflow row, so the editor refused (`found 2`) and wrote nothing.
- **Fix:** the first 90 characters of the compare row, which are unique; the editor then wrote the row directly after the compare row. Recorded in E-10 (`forkdelta_prefix_chars: 90`).
- **Files modified:** `docs/FORK-DELTA.md` (as planned).
- **Committed in:** `fb63b0e9`.

**2. [Rule 3 - Blocking] The Task 3 verify expects no `scratch/*` ref on origin**
- **Found during:** Task 3 verify.
- **Issue:** `test -z "$(git ls-remote --heads origin 'scratch/*')"` fails because Phase 36 left `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6` on origin (plan 37-01 recorded them in E-01 `origin_scratch_refs`).
- **Fix:** the clause checks `scratch/cih-04-*` instead; E-12 records `origin_scratch_refs_now`, equal to the E-01 set, and the handoff's before-push check names `scratch/cih-04-dry-run` only.
- **Verification:** `P37-03-T3-OK`.
- **Committed in:** not applicable (verify command and handoff wording).

**3. [Rule 3 - Blocking] Spotless numstat method**
- **Found during:** Task 2 (formatting check).
- **Issue:** a numstat of the edited worktree against `HEAD` would include the additions themselves, so it could never equal the pristine baseline.
- **Fix:** the edited files were staged in the throwaway worktree before `spotlessApply`, so the working-tree numstat shows only Spotless's own changes. Both numstats are empty (`none`) and the new file is `identical`.
- **Committed in:** not applicable (measurement method, recorded in E-10).

**4. [Plan wording] The compare rule is stricter than specified**
- **Found during:** Task 2 design.
- **Issue:** the plan's rule (the call line after trimming and removing a trailing redirection, no `if:`, no `continue-on-error`) would accept a step that captures the status and never fails the job.
- **Fix:** an added requirement for the line `exit "$status"`, with fixtures; the real files satisfy it.
- **Committed in:** `fb63b0e9`.

**5. [Carry-over, left as is] The `increment-version.yml` header comment**
- **Found during:** the orchestrator's carry-over instruction.
- **Issue:** the header says `ReleaseWorkflowGuardUnitTest` pins the file; the pinning class is `IncrementVersionWorkflowGuardUnitTest`.
- **Decision:** not fixed. The replacement text would move no anchor count (none of `semver`, `node-version`, `setup-node` or `npm install` appears in it, and no row counts either class name) and the guard does not read that sentence, but this plan prohibits editing `increment-version.yml` and its Task 1 verify, which the plan re-runs on the committed tip, requires the file to equal its state at `workflow_commit` of plan 37-01. Plan 37-05's allowed-paths list already admits the file, so a one-line follow-up (change `ReleaseWorkflowGuardUnitTest pins this file` to `IncrementVersionWorkflowGuardUnitTest pins this file` and remove the clause or move its reference commit) is a decision for the user or plan 37-05.

**6. [Note] Evidence timestamps**
- E-09's timestamp (`09:27:00Z`) is about a minute and a half ahead of the real clock at its commit; E-10 to E-12 carry the real clock. E-09 was not edited after its commit.

---

**Total deviations:** 6 (3 blocking verify or method issues, 1 stricter-than-plan rule, 1 carry-over left open, 1 timestamp note). **Impact on plan:** none on scope or results; no src/main, dependency, existing assertion, `increment-version.yml` or origin change. Carry-overs (a) to (d) were applied: `tests>=N` was quoted and no stray `=N` file exists, no evidence key needed renaming (all plan keys were lowercase), the new test class has no `extends ` text, and every path list in a verify command is in C order.

## Issues Encountered

- No OpenJ9 crash, core dump or javacore appeared in any run.
- A built-in safety check blocked one `rm -f` of a variable-built path during the replay; it was not worked around, the report file is overwritten by the compare block itself so no removal was needed.

## Known Stubs

None.

## Threat Flags

None. The only new `uses:` line is the SHA-pinned upload-artifact action already used by `tests.yaml`; permissions stay one top-level `contents: read` and the job's `contents: write` and `actions: read`; the compare step has no `if:` and no `continue-on-error`.

## User Setup Required

None for the plan. The optional CI dry run needs a push and a dispatch by the user; see `37-USER-DISPATCH-DRYRUN.md`.

## Handoffs

- **Plan 37-04:**
  - The anchors count is `rows=171` (172 expected after plan 37-04).
  - The pin-guard tally after this plan is `pinguard_after` 44 (`TestsWorkflowPinGuardUnitTest`, 39 before); `ReleaseWorkflowGuardUnitTest` has 34; the cache-busted `testUnit` total is 1624 tests (2 skipped).
  - The Spotless numstat baseline of `TestsWorkflowPinGuardUnitTest` is empty (`none`): the class has no pre-existing Spotless hunk, so any later hunk is new.
  - The edited class kept its existing 39 executions unedited; later plans add by addition only.
- **Plan 37-05:** `37-USER-DISPATCH-DRYRUN.md` names `<gated sha>`, "the last commit outside `.planning` named in the SUMMARY of plan 37-05", so that SUMMARY must state it. E-12 stays `pending (user-gated)`. Ledger pointers: `workflows_commit` `44b8fb62` for Phase 30 WR-01, `guard_commit` `fb63b0e9` for Phase 30 WR-02, WR-03, IN-03 and Phase 29 close record row 25 WR-01; Phase 30 IN-04 stays a disposition (owner Phase 38 RLS-05). The `increment-version.yml` header comment (deviation 5) is open for a decision.
- **Orchestrator, if the user runs the dry run:** record the user's report verbatim as `### E-19: CIH-04 dry-run dispatch, user-reported` with `key: value` lines and the words `user-reported`, add index row E-19, leave E-12 `pending` and never edit it, mark nothing passed through it, commit documents only, do not touch `.github/`, and act only on the user's own message, never on a relayed approval. The expected conclusion is `failure` at the step `Compare the archive with the previous release` with about 238 unallowed lines; a failure earlier is the finding that justifies the run and needs a gap for this phase.
- Scratch rules stay as in plan 37-01: logs under `/tmp/gsd37/exec/`, worktrees under `/tmp/gsd37/w/<name>` removed after use (`git worktree list` shows one entry), never stage `.planning/config.json`, `.planning/state.json` or `.planning/STATE.md`, origin and GitHub stay read-only.

## Next Phase Readiness

Plan 37-04 can start: the tree is clean apart from the orchestrator-owned state files, the evidence index reads pass for E-01 to E-11 and pending for E-12 (user-gated) and E-13 to E-18, `testUnit` is green at 1624 tests, and the anchors mirror reads `rows=171 bad=0`.

## Self-Check: PASSED

- Files: both release workflows, `ReleaseWorkflowGuardUnitTest.java`, `37-USER-DISPATCH-DRYRUN.md` and the evidence file exist; the plan assets are unchanged against the phase start.
- Commits `44b8fb62`, `bd512e9b`, `fb63b0e9`, `4bc3454f`, `44b717a6` are ancestors of `HEAD`.
- All five plan `<automated>` checks print their OK marker (`P37-03-T1-OK`, `P37-03-T1-TESTS-OK`, `P37-03-T2-OK`, `P37-03-T2-TESTS-OK`, `P37-03-T3-OK`; the Task 3 check with the scratch-ref clause adjusted as in deviation 2).

---
*Phase: 37-ci-hardening-advisory-debt*
*Completed: 2026-10-31*
