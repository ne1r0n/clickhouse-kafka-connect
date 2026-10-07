---
phase: 11-full-suite-ci-publication
plan: 07
subsystem: milestone-closure
tags: [requirements-closure, evidence-acts, final-state-assertion, probe-accounting, no-push]

requires:
  - phase: 11-full-suite-ci-publication
    provides: "11-04 local gate, 11-05 scratch CI run 37617508949, 11-06 publication (c61d90ce, tags, CI-06 run 37619434848), 11-08 D-17 fix"
provides:
  - "SUITE-01..03 and CI-05..07 closed in REQUIREMENTS.md (checkboxes and Traceability rows), each on a command re-run at execution time; zero open checkbox rows in the whole milestone"
  - "A Phase 11 closure blockquote under the SUITE block stating that SUITE-01/02 were evidence acts, not fresh test work"
  - "A final published-state assertion, run as one pass, with full output below"
  - "The spec-less probe accounting closed at 11 of 11 with each backstop's confirmation status stated"
affects: []

actuals:
  tokens: 4500
  tasks: 2
  commits: 1

plan_head_before: 93a9b4e251b32e359a3746f51d38d87af75e89c2
plan_head_after: fd8fbdef
commits: 1

tech-stack:
  added: []
  patterns:
    - "Requirement closures cite the command re-run at execution time, never a planning document"
    - "Evidence plans that touch no source commit nothing; the only commit here is the one planning-document edit"

key-files:
  created: []
  modified:
    - .planning/REQUIREMENTS.md

key-decisions:
  - "Re-ran the full suite (--rerun) rather than re-reading plan 11-04's XML: the results directory still existed but a fresh run on tree 4a9fc072 (equal to the published c61d90ce outside .planning/) is a measurement, not a citation."
  - "Skip identity compared pair by pair against the list plan 11-04 derived and diffed against Phase 10's named 18; Phase 10's own list is not re-read here, so this is a comparison against 11-04's stored list."
  - "SmtSinkLandingTest named as the one exception to 'SMT tests pass untouched': Phase 7 adapted its helper calls, with 0 assertion lines changed."
  - "Bookkeeping lives on local sync/v1.6.0 (unprotected). Nothing pushed; local custom_build equals origin/custom_build."

requirements-completed: [SUITE-01, SUITE-02, SUITE-03, CI-05, CI-06, CI-07]

coverage:
  - id: D1
    description: "SUITE-01 closed on a cache-busted full run: 663 tests, 645 succeeded, 0 failed, 0 errors, 18 skipped, skip set identical to the set plan 11-04 diffed against Phase 10's named 18"
    requirement: SUITE-01
    verification:
      - kind: integration
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun -> BUILD SUCCESSFUL in 2m 46s; XML totals 663/0/0/18 over 59 classes; skip diff SKIP_SETS_IDENTICAL_18_PAIRS"
        status: pass
    human_judgment: false
  - id: D2
    description: "SUITE-02 closed: five SMT correctness classes 0 failures/0 skips, performance class exactly 2 @Disabled skips, four SMT main files and five SMT test classes byte-identical to pre-v1.6.0-sync"
    requirement: SUITE-02
    verification:
      - kind: integration
        ref: "this run's TEST-*.xml; git diff --quiet pre-v1.6.0-sync c61d90ce over the ten SMT files (FOUR_SMT_MAIN_AND_FIVE_TEST_CLASSES_BYTE_IDENTICAL)"
        status: pass
    human_judgment: false
  - id: D3
    description: "SUITE-03 closed: docs/FORK-DELTA.md in the published tree, linked from README.md and AGENTS.md, Known costs section present, four shortstat commands re-run"
    requirement: SUITE-03
    verification:
      - kind: other
        ref: "git ls-tree c61d90ce; grep -c FORK-DELTA.md README.md AGENTS.md = 1 each; four git diff --shortstat commands"
        status: pass
    human_judgment: false
  - id: D4
    description: "CI-05, CI-06 closed from the REST API: run 37619434848 success in 386 s run-level / 380 s job-level against a 1350 s threshold; jacoco artifact 816157 bytes; locator head_sha + path + conclusion != cancelled returns exactly one"
    requirement: CI-06
    verification:
      - kind: other
        ref: "api.github.com actions/runs?head_sha=c61d90ce..., /runs/37619434848/artifacts, /runs/37619434848/jobs"
        status: pass
    human_judgment: false
  - id: D5
    description: "CI-07 closed: origin custom_build is the two-parent merge commit c61d90ce, both tags on origin, scratch branch absent; approval recorded in 11-06-SUMMARY"
    requirement: CI-07
    verification:
      - kind: other
        ref: "git ls-remote --heads origin; git ls-remote --tags origin"
        status: pass
    human_judgment: false
  - id: D6
    description: "The closure blockquote reads honestly as three requirements closed on re-measured evidence and three on new evidence"
    verification: []
    human_judgment: true
    rationale: "Whether the framing survives a stranger-read is an interpretive call no command settles; Phase 10's equivalent blockquote was routed to UAT for the same reason"
  - id: D7
    description: "Both backstop truths (SUITE-01 concurrency, SUITE-03 ordering) are confirmed"
    verification: []
    human_judgment: true
    rationale: "Neither has explicit confirming evidence; see the probe accounting below. They abstain to human_needed by design."

duration: 10min
completed: 2026-10-07
status: complete
---

# Phase 11 Plan 07: Close the Milestone's Last Six Requirements Summary

**SUITE-01..03 and CI-05..07 are closed in `REQUIREMENTS.md` on commands re-run now (a fresh `--rerun` of 663 tests with 0 failures and a skip set identical to Phase 10's, plus the REST API, `git ls-remote` and four shortstat commands), with a blockquote that says plainly that this phase's real risk was CI and the one-way push, not the suite; nothing was pushed.**

## Performance

- **Duration:** about 10 min (the full suite took 2m 46s of it)
- **Started:** 2026-10-07T12:22:47Z
- **Completed:** 2026-10-07T12:33Z
- **Tasks:** 2 (Task 1 committed, Task 2 assertions only, no commit)
- **Files modified:** 1 tracked (`.planning/REQUIREMENTS.md`)

## The six closure commands, re-run at execution time

**SUITE-01.** `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun` on tree `4a9fc072` (since rebased to `93a9b4e2` with an identical tree; `git diff --stat c61d90ce HEAD -- . ':!.planning'` is empty, so the run covers the published tree):

```
> Task :test                       (executed, not UP-TO-DATE)
Tests summary: 663 tests, 645 succeeded, 0 failed, 18 skipped
BUILD SUCCESSFUL in 2m 46s
TOTALS tests=663 failures=0 errors=0 skipped=18 succeeded=645 classes=59
SKIPPED_PAIRS 18   (ClickHouseSinkTaskWithSchemaTest 13, ConvertToMapOrJsonPerformanceTestJson 2,
                    ClickHouseSinkTaskTest 1, InsertFormatPerformanceTest 1, JsonColumnTest 1)
SKIP_SETS_IDENTICAL_18_PAIRS
```

All 59 XML files were written inside this run's window. The skip comparison is against `/tmp/p11-skips-now.txt`, the 18-pair list plan 11-04 derived and diffed against Phase 10's named inventory. Phase 10's own list is not re-read here. The run was a 6-fork run on a 12-core machine, so it is not a single-fork result.

**SUITE-02.** From the same XML: `TableOperationFilterTest` 26/0/0/0, `HeadersToValueTest` 25/0/0/0, `HeaderToValueTest` 24/0/0/0, `ConvertToMapOrJsonTest` 37/0/0/0, `SmtSinkLandingTest` 5/0/0/0 (tests/failures/errors/skipped); `ConvertToMapOrJsonPerformanceTestJson` 2/0/0/2. `git diff --quiet pre-v1.6.0-sync c61d90ce` over the five fork SMT main files and five SMT test classes exits 0 (`FOUR_SMT_MAIN_AND_FIVE_TEST_CLASSES_BYTE_IDENTICAL`). **Exception stated, not buried:** `SmtSinkLandingTest.java` is not byte-identical to its pre-sync form. Phase 7 (`e1a90309`) adapted its table-creation helper calls to upstream's rewritten APIs, and the diff contains 0 added or removed lines matching `assert`.

**SUITE-03.** `docs/FORK-DELTA.md` is in the published tree (`git ls-tree -r c61d90ce`, 167 lines) with these `##` sections: Contents, What this fork still patches, What the fork retired in favour of upstream, The raw numbers, Known costs and open items, Named skip inventory, For the next syncer. `FORK-DELTA.md` appears once in `README.md` and once in `AGENTS.md` at `c61d90ce`.

```
src build.gradle.kts gradle .github  v1.2.9..pre-v1.6.0-sync   93 files, 8401 insertions, 423 deletions
src build.gradle.kts gradle .github  v1.6.0..c61d90ce          84 files, 9476 insertions, 451 deletions
src/main                             v1.2.9..pre-v1.6.0-sync   32 files, 1308 insertions, 139 deletions
src/main                             v1.6.0..c61d90ce          30 files, 1368 insertions,  98 deletions
```

The figures match the published document after the F-1 refresh.

**CI-05.** REST API, run `37619434848`:

```
ClickHouse latest Client version V2 tests  success  job 380 s   (Run tests step 363 s)
run-level run_started_at -> updated_at     386 s
timeout-minutes 45 (read from tests.yaml at c61d90ce) -> threshold 1350 s -> UNDER
jacoco-coverage-reports 816157 bytes, expired False; gradle-test-reports 23711100 bytes
```

The scratch rehearsal run `37617508949` took 350 s on a byte-identical tree outside `.planning/` and `docs/FORK-DELTA.md`.

**CI-06.** `actions/runs?head_sha=c61d90ce5dfb32ea5876dffacc8bb54d8846739a`, filtered to path `.github/workflows/tests.yaml`:

```
total_count 2   path_matches 2
37619434848 push v1.6.0_gg1  completed success    386.0 s
37619430314 push custom_build completed cancelled   4.0 s
non_cancelled 1
EVIDENCE 37619434848 success https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/37619434848
```

The plan assumed exactly one run per SHA. The narrowed locator (`head_sha` + path + `conclusion != cancelled`) returns exactly one, with conclusion `success`, the same locator plan 11-06 used. The cancelled run is the branch push, cancelled by the tag push in the same `github.sha` concurrency group.

**CI-07.**

```
c61d90ce5dfb32ea5876dffacc8bb54d8846739a  refs/heads/custom_build
bec167d85918e77c11621063000acc6e031ae2fa  refs/heads/main
6ac2d20cc0c7feacc315b21b322325b4fee25784  refs/tags/pre-v1.6.0-sync
c61d90ce5dfb32ea5876dffacc8bb54d8846739a  refs/tags/v1.6.0_gg1
```

The approval is recorded in `11-06-SUMMARY.md` key-decisions: the user chose "Fix F-1, then publish (Recommended)" directly to the orchestrator on 2026-10-07. That record is the CI-07 approval evidence.

## Task 1: the edit

Checkboxes flipped without rewording any requirement; six Traceability rows flipped to `Complete` with that capitalisation; a three-paragraph `>` blockquote placed directly under the SUITE block (before `### Free Wins`). Committed alone as `fd8fbdef`, touching only `.planning/REQUIREMENTS.md` (18 insertions, 12 deletions; no file deletions). Verify output: `ALL_SIX_REQUIREMENTS_CLOSED`, `CLOSURE_BLOCKQUOTE_LINES=3`, `CLOSURE_NOTE_PRESENT`, open checkbox rows 0.

## Task 2: final-state assertion, full output

Run as one pass after the Task 1 commit (`/tmp/p11-07/final-all.txt`):

```
OPEN_CHECKLIST_ROWS=0
INVARIANT_EXIT=0                       (verify-merge-invariants.sh 673d29c3d5dc24b97de3b022acdfe473e1a5493c)
SECURITY_DOCS=5                        06-SECURITY.md 07-SECURITY.md 08-SECURITY.md 09-SECURITY.md 10-SECURITY.md
LOCAL_FINAL_STATE_OK                   (FORK-DELTA.md present, linked from README.md and AGENTS.md)
----
LOCAL_CUSTOM_BUILD=c61d90ce5dfb32ea5876dffacc8bb54d8846739a ORIGIN=c61d90ce5dfb32ea5876dffacc8bb54d8846739a
DIVERGENCE_ZERO
c61d90ce...  refs/heads/custom_build
bec167d8...  refs/heads/main
6ac2d20c...  refs/tags/pre-v1.6.0-sync
c61d90ce...  refs/tags/v1.6.0_gg1
TAGS_ON_ORIGIN=2 SCRATCH_ON_ORIGIN=0
PUBLISHED_REFS_FINAL_OK
----
git status --porcelain -- src/   -> 0 lines
87fa1e78 fix(11-08): force LZ4 network compression for ClickHouse 26.9+ servers
SRC_MAIN_COMMITS_OUTSIDE_11_08=0       (git log --no-merges 4f8aea0f..custom_build -- src/main)
NO_SRC_MAIN_EDITS_IN_PHASE_11
----
unpushed on sync/v1.6.0 beyond custom_build:
fd8fbdef docs(11-07): close SUITE-01..03 and CI-05..07 on re-run evidence
93a9b4e2 docs(11-06): record publication of custom_build and CI-06 evidence
non-.planning files in that range: 0
```

**Divergence differs from the plan's expectation, for a stated reason.** The plan expected local `custom_build` to be ahead of `origin` by this plan's bookkeeping commit. The orchestrator instead moved the post-publication bookkeeping off the protected `custom_build` onto local `sync/v1.6.0`, which is unprotected, and reset local `custom_build` to `c61d90ce`. The local-versus-origin divergence on `custom_build` is therefore 0. The bookkeeping is on a branch that has no remote, because 11-06 deleted `origin/sync/v1.6.0`. I verified the guard myself on the new HEAD: `git.base-branch --is-protected sync/v1.6.0` returns `false`.

### The four items `STATE.md` expected this phase to clear

| Item | Cleared by |
|---|---|
| Two missing `SECURITY.md` files (07, 09) | Plan 11-01 (`ebd6c9e7`); `ls .planning/phases/*/[0-9]*-SECURITY.md` returns 5 files now |
| Six open `MERGE-CHECKLIST.md` rows | Plan 11-01; `OPEN_CHECKLIST_ROWS=0` now |
| `verify-merge-invariants.sh` failing open on a `git grep` error | Plan 11-01 (`4b03e6f3`); exit 0 unattended now |
| Six `Pending` requirement rows | This plan; zero `[ ]` rows and zero Phase 11 `Pending` rows now |

## Probe accounting: 11 of 11

The deterministic edge probe surfaced 11 applicable rows. **11 surfaced = 9 explicit + 2 backstop + 0 flagged-unresolved, with 0 dismissed and 0 dropped.**

| Row | Disposition | Where | Evidence in execution |
|---|---|---|---|
| SUITE-01 adjacency, empty, ordering | explicit (3) | 11-04 | Run totals, empty-skip-set handling and skip-diff ordering all asserted by command in 11-04 and again here |
| SUITE-01 concurrency | **backstop** | 11-04 | see below |
| SUITE-02 (probe-unclassified) | explicit | 11-04 | Five SMT correctness classes, this run's XML |
| SUITE-03 adjacency, empty | explicit (2) | 11-03 | FORK-DELTA structure gate and re-run shortstat |
| SUITE-03 ordering | **backstop** | 11-03 | see below |
| CI-05, CI-06 (probe-unclassified) | explicit (2) | 11-05 | Threshold comparison, artifact, exactly-one locator |
| CI-07 (probe-unclassified) | explicit | 11-06 | Non-force push after recorded approval |

Explicit count: 3 + 1 + 2 + 2 + 1 = 9. Backstop count: 2.

**Backstop 1, SUITE-01 concurrency: not confirmed. It abstains to `human_needed`.** The truth says CI runs at the same single-fork topology as the local gate. Plan 11-04 refuted the local half: the local gate ran 6 forks (`hw.ncpu` 12, `maxParallelForks = availableProcessors() / 2`), and this run's 6-fork shape matches that. The CI half is only partly evidenced. CI is green twice (`37617508949`, `37619434848`) on a 2-core `ubuntu-latest` runner, which resolves `maxParallelForks` to 1, but job logs return HTTP 403 unauthenticated, so the resolved topology is inferred from the core count and not read from a log. A CI result is green at an unmeasured topology, and the equivalence the truth states is not established.

**Backstop 2, SUITE-03 ordering: not confirmed. It abstains to `human_needed`.** The truth says a future syncer diffing `FORK-DELTA.md` sees content changes, not reordering noise. The current revision does group its rows by disposition under stable headings, but stability across future revisions cannot be confirmed from one revision. No execution step here produced evidence that could.

## What this phase deliberately did not do

- The four open code-review findings remain open and published (D-08): `Column.extractColumn`, the `USERNAME` default, `JsonColumnTest`'s row-count-only assertions, and P9-IN-01.
- R-01's fail-fast fix remains an unscheduled Future Requirement. D-16 publishes the cost; it does not fix it.
- `maxParallelForks` remains untuned; the CI run at 386 s against a 1350 s threshold gave no evidence to justify tuning it.
- `CHANGELOG.md` has no `v1.6.0_gg1` entry. The top entry is still upstream's `1.6.0, 2026-09-11`, and the gate did not choose otherwise.
- `origin/main` is untouched, still at `bec167d8`.
- Plan 11-05's F-2 comment-only findings (IN-01 and IN-02 of `07-REVIEW.md`) are still absent from the published open-item lists.
- CI's own test totals and server version are still unread (log access needs authentication).

## Operator next steps

1. **Post-publication bookkeeping is unpushed, by design.** It sits on local `sync/v1.6.0`, all `.planning/`-only: `93a9b4e2` (11-06 SUMMARY and ROADMAP), `fd8fbdef` (this plan's `REQUIREMENTS.md` closure), plus this plan's SUMMARY commit. Pushing it would fire an additional `tests.yaml` run that is not CI-06's evidence, and the approval at plan 11-06's gate covered the merge commit, the branch push and the tag pushes only. If you want these on the public fork, that is a separate decision.
2. If those commits are pushed, the workflow's concurrency group is keyed on `github.sha` with `cancel-in-progress: true`. Pushing a tag at the same SHA as a branch push cancels the branch run, as it did here; push the tag after the branch run finishes, or key the group on `github.ref`.
3. Decide on a `CHANGELOG.md` entry for `v1.6.0_gg1` (`AGENTS.md` asks for one before a PR).

## Task Commits

1. **Task 1: Close the six SUITE and CI requirements on re-run evidence** - `fd8fbdef` (docs)
2. **Task 2: Final-state assertion** - no commit by design

**Plan metadata:** the SUMMARY commit that follows this file (`docs(11-07): complete plan`).

## Files Created/Modified

- `.planning/REQUIREMENTS.md` - six checkboxes closed, six Traceability rows `Complete`, Phase 11 closure blockquote

## Decisions Made

See key-decisions. No product decision was taken; all closures follow the plan.

## Deviations from Plan

**1. [Environment] Bookkeeping branch changed.** The first commit attempt was refused by the executor's protected-branch guard while HEAD was on `custom_build`. I halted and reported. The orchestrator re-homed the bookkeeping onto unprotected local `sync/v1.6.0` and reset local `custom_build` to the published `c61d90ce`; I re-verified the guard on the new HEAD before committing. Effect on the plan: the expected unpushed-divergence on `custom_build` is 0 and not "this plan's one commit". The plan's `ORIGIN_CUSTOM_BUILD_MISMATCH` line does not appear.

**2. [Plan premise] CI-06's "exactly one run per SHA" did not hold.** The published SHA has two runs, one cancelled; the evidence is the single non-cancelled run (see CI-06 above). Same narrowing as plan 11-06.

**Total deviations:** 2 (0 auto-fixed code changes). **Impact:** none on the closure evidence.

## Issues Encountered

- `git diff --stat` on `REQUIREMENTS.md` printed a note that the file had changed on disk since my first read; this was my own `sed` flip of the checkboxes and rows, and the final diff is the intended 18/12 lines.
- The `/tmp/p11-skips-now.txt` list from plan 11-04 stores fully qualified class names; the first comparison against this run's simple-name pairs reported a false difference until both sides were normalised to simple class names.

## Known Stubs

None.

## Threat Flags

None - one planning document edited, no source, dependency or workflow change, and nothing pushed.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Phase 11 is the last phase of milestone `v1.6.0_gg1-sync`; all 53 v1 requirements now read `Complete`. Ready for phase verification, then `/gsd-complete-milestone`. The two backstop truths will route to `human_needed`.

## Self-Check: PASSED

- `.planning/REQUIREMENTS.md`: FOUND, with 0 open checkbox rows and 0 Phase 11 `Pending` rows.
- Commit `fd8fbdef`: `git merge-base --is-ancestor fd8fbdef HEAD` holds; it touches only `.planning/REQUIREMENTS.md`.
- Task 1's three verify checks and Task 2's three verify checks each printed their pass marker.
- `git rev-list --count 93a9b4e2..HEAD` was 1 at SUMMARY-write time, consistent with `commits: 1` before the SUMMARY commit.
- Nothing pushed: `git ls-remote --heads origin custom_build` equals local `custom_build` at `c61d90ce`.

---
*Phase: 11-full-suite-ci-publication*
*Completed: 2026-10-07*
