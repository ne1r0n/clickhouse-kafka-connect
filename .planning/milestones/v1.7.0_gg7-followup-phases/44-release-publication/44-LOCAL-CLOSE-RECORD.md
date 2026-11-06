# Phase 44: local close record for the publication of v1.7.0_gg7

> Superseded on 2026-11-04: the publication happened (E-28 to E-36), the phase verification passed (`44-VERIFICATION.md`) and RLS-01 to RLS-04 are Complete. The text below is the record as written before the publication; read `44-RELEASE-RECORD.md` for what happened.

## Status

Local-complete. Plans 44-01 to 44-05 were executed and gated at the head named in E-12 and E-13, `b265f5a45a0752f314bb95315e3cdfbeaa2b78cd`, and the tip equals that head outside `.planning` and under `.planning/scripts` (E-14, E-19). The publication is pending and human-gated. Nothing was pushed, merged, tagged or dispatched: origin `custom_build` reads `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`, no `sync/v1.7.0` head and no `v1.7.0_gg7` tag exists on origin, and the local tag `pre-v1.7.0_gg7` is on `72e8ccff` and not on origin. Ready for the user's direct approval: the orchestrator presents `44-APPROVAL-BUNDLE.md`.

## Requirement status

| Requirement | Status | Evidence |
|-------------|--------|----------|
| RLS-01 | satisfied | E-01 to E-04: `VERSION` `v1.7.0_gg7` through the bump script with its anchors row, the CHANGELOG section, FORK-DELTA final with refreshed numbers |
| RLS-02 | satisfied | E-11: the gg7 allow-list was generated last from the final tree; 18 rows cover all 120 differences; the local compare dry run exits 0 with `RESULT: PASS`; three mutations turn it red; the runner repeats the compare in U2 |
| RLS-03 | satisfied | E-05 to E-10: the rollout note with labelled rows, four fresh-context review rounds (agent ids distinct), 20 findings all dispositioned (19 fixed, 1 deferred), the documents frozen |
| RLS-04 | pending, human-gated | the gates are satisfied (E-12, E-20, E-13: testUnit with Docker unreachable, two full suites, the invariant script, `gate_decision: go`) and the rehearsal is E-14; the merge, the single `tests.yaml` run on the merged SHA and the tag by the publish run are the deferred tail, recorded from E-21 on |

The five ROADMAP criteria:

| Criterion | Status | Evidence |
|-----------|--------|----------|
| 1. `VERSION`, CHANGELOG, FORK-DELTA | satisfied | E-02, E-03, E-04 |
| 2. the rollout note with its labels | satisfied | E-05, E-06, E-07 |
| 3. the independent review and the allow-list | satisfied, with the human-judgment items below | E-08 to E-11 |
| 4. two full suites and `testUnit` without Docker | satisfied | E-12, E-20, E-13 |
| 5. dry run, merge, one `tests.yaml` run, publish run, tag, scratch deletion | pending, human-gated | E-14 is the rehearsal and the bundle; E-15 to E-18 are the pending handoffs; the remaining gate is G3 to G5 and U1 to U5 |

Human-judgment items, not passed gates: the review loop ended after round 4 on the user's decision "Fix both, accept without round 5", so the last fixes were not read by a fresh reviewer; finding IN-03 is deferred; the F-2 progress-header claim is carried as `derived (mechanism, not measured)`; the integration suite was not re-run (E-12 `integration_test`); E-12 keeps verdict `fail` by design and E-20 supersedes it; the CHANGELOG heading date (bundle question 5).

## Deferred must-haves (satisfied only after the user's direct approval)

These must-haves of ROADMAP criterion 5 are satisfied only in the orchestrator-inline tail, after the user's direct approval, and recorded from E-21 on (E-20 is the plan 44-04 hygiene re-run):

1. the user's direct approval in their own words;
2. `custom_build` a non-force `--no-ff` merge whose tree equals the rehearsed tip;
3. exactly one green `tests.yaml` run on the published SHA before the publish run;
4. the tag `v1.7.0_gg7` and the GitHub release created by the user's `release.yaml` publish run;
5. `pre-v1.7.0_gg7` pushed by name after it;
6. the scratch branch deleted.

Freeze rule: these must-haves hold only for a pushed tip equal to the gated head `b265f5a45a0752f314bb95315e3cdfbeaa2b78cd` outside `.planning` and under `.planning/scripts`. Any later non-`.planning` change (a fix from a review round that the code-review gate appends after this plan, or a CHANGELOG date-fix) re-opens plan 44-03 and re-runs plan 44-04 Tasks 1 to 3 and plan 44-05 Task 1 on the new head first; the bundle is void and every approval for the older head is asked again.

## Why this is the honest shape

Executors cannot receive the user's direct word (they refuse relayed approvals), and `custom_build` is the protected base branch under GSD's rules, so the part of the ROADMAP that publishes can only be prepared and rehearsed by an executor. The ROADMAP says the phase publishes, so the phase checkbox stays unchecked and RLS-04 stays Pending until E-21 to E-29 show the publication. Verification of this phase should read `human_needed` for criterion 5 and RLS-04 and pass for the rest.

## Wording changes the orchestrator applies

This phase edits none of these files; the orchestrator applies them.

1. ROADMAP Phase 44 Note, step 1: "A left count of 1 is the gg6 merge; more means `origin` moved." becomes "The left count is 0, because `sync/v1.7.0` already contains the gg6 merge `72e8ccff`; any count above 0 means `origin` moved." (measured `0 198` at planning time and `0 261` at the rehearsal; every gate of this phase expects 0).
2. ROADMAP Phase 44 Note, last sentence: "`custom_build` is the protected base branch" is a GSD rule; GitHub reports `protected: false`. Add "(a GSD rule)" or leave as is (the orchestrator's choice).
3. Closure shape: Phase 44's checkbox stays unchecked and RLS-04 stays `Pending` in the traceability table until E-21 to E-29 show the publication; RLS-01, RLS-02 and RLS-03 may flip to Complete when the phase verification passes (RLS-02's "dry run of the compare" is the local dry run of E-11; the runner repeats it in U2).
4. STATE.md: the line "Measured 2026-11-01: `origin/custom_build...sync/v1.7.0` counts 1 and 13" and the "Operator Next Steps" item "Plan Phase 39" are stale.
5. Optional: ROADMAP Phase 39 Note's "leaves the first run to the Phase 44 rehearsal push" can name U1 and U2 as those first runs once they exist.

## What the publication still needs from the user

Each step is from its own file in `.planning/phases/44-release-publication/`, in this order; each starts with the check that `custom_build` still reads `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`, and a move voids the approval.

1. G3, the user's direct "go" on the eight questions of `44-APPROVAL-BUNDLE.md` (the orchestrator presents it).
2. U1, `44-USER-DISPATCH-1-SCRATCH-DRYRUN.md`: push the tip to the scratch branch `sync/v1.7.0` and wait for its one green `tests.yaml` run.
3. U2, the same file: dispatch `release.yaml` from that branch as a dry run.
4. G4, the user's own direct "publish" message; the orchestrator then builds the merge commit M (runbook step O3).
5. U3, `44-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md`: push M to `custom_build` by SHA and wait for its single green `tests.yaml` run.
6. G5, after M's single green run.
7. U4, `44-USER-DISPATCH-3-PUBLISH.md`: dispatch `release.yaml` from `custom_build` with `dry_run` not ticked; the run creates the release and the tag `v1.7.0_gg7`. Never push that tag.
8. U5, `44-USER-DISPATCH-4-PRETAG-CLEANUP.md`: push `pre-v1.7.0_gg7` by name, delete the scratch branch, read back the two pull requests.

## Carried to the milestone close

| Item | Disposition | Owner | Re-open trigger |
|------|-------------|-------|-----------------|
| Review finding IN-03: the planning identifier "Phase 39" that Phase 43 added to the published gg6 note | deferred (the published gg6 note is not edited without the user's direct authorization, decision O-1) | fork maintainer (the user) | the next edit of the published gg6 note |
| The review ended after round 4 on the user's decision, so the last fixes had no fresh reviewer | carried as a human-judgment item | fork maintainer (the user) | the next edit of `docs/ROLLOUT-v1.7.0_gg7.md`, or an operator question about its rewritten rows |
| The closing has residuals: a Keeper state-provider constructor that fails on client V1, a helper-client constructor that fails after it built its client V2, a second `start()` on one writer, a task-level `stop()` without `try`/`finally` around the delivery strategy's `stop()`, and the writer's five-second wait for the table refresh (FORK-DELTA item 13) | carried, published knowingly open in FORK-DELTA, the CHANGELOG Known Issues and the note | fork maintainer (the user) | a task-restart leak report from an operator, or the next edit of `ClickHouseWriter.stop()` or `ClickHouseHelperClient` |
| The bounded insert has limits: a write-side stall, client V1 inserts and non-insert requests stay unbounded, and the help text of `clickhouseClientInsertTimeoutMs` still describes a hard limit (FORK-DELTA item 14) | carried, as above | fork maintainer (the user) | an operator report of an insert that hangs on a stalled server, or the next edit of the client V2 insert methods |
| The F-2 progress-header claim (row T6 of the note) is `derived (mechanism, not measured)` | carried; the user was asked in bundle question (8) | fork maintainer (the user) | an operator report of an insert that escapes the bound, or the user's request to measure it |
| The limits of the original-topic fixes: many-to-one rename and a partition-changing SMT are not supported for exactly-once, the in-memory state machine or `reportInsertedOffsets` (FORK-DELTA item 16) | carried, stated in FORK-DELTA, the CHANGELOG Known Issues and the note | fork maintainer (the user) | an operator who enables `reportInsertedOffsets` under such an SMT, or upstream refactoring `Processing.doLogic` |
| Retriable `DataException` cases and the missing retry budget (FORK-DELTA item 17) | carried, with its rollout warning in the note | fork maintainer (the user) | an operator report of a redelivery loop with a wrapped `DataException`, or a decision on the retry budget |
| FUT-05: codes in `additionalRetriableErrorCodes` outside the vetted set get a start-up WARN and no retry budget (FORK-DELTA item 24) | carried; the note says FUT-05 stays open | fork maintainer (the user) | a decision on FUT-05, or an operator report of a redelivery loop with `additionalRetriableErrorCodes` |
| Phase 37 advisory rows that need the user's approval: four `src/main` rows and one null assertion (FORK-DELTA item 20) | carried, pointing at the Phase 37 advisory ledger | fork maintainer (the user) | the user's approval to edit `src/main`, or the next edit of the files named in the ledger |
| Upstream pull requests #861, #855 and #857 touch the same files and are open; upstream `main` moved from `9bed6f79` to `d92a1830` after the first recheck (dependency bumps and cloud-test secrets only) | information for the next upstream sync; this release stays on `9bed6f79` | fork maintainer (the user) | the next upstream sync, or an upstream tag past `v1.7.0` |
| `integrationTest` was not re-run on the gated head (E-12); `src/integrationTest` changed after the recorded run only by a diagnostic log-line fix (`51200928`) | carried as a human-judgment item (bundle question 7) | fork maintainer (the user) | any change of the integration sources or harness before publication, or the first nightly after `NIGHTLY_ENABLED` is set |
| Phase 43 handoff item 6: the SAME first-chunk case is unstated, and section B of the gg6 note lists configurations it does not characterise | carried as information | fork maintainer (the user) | an operator question about either case, or the next edit of the gg6 note |
| Phase 43 handoff item 7: FORK-DELTA item 20 names the Phase 43 ledger by path; the Phase 43 directory moves under `.planning/milestones/` at the milestone archive | carried | fork maintainer (the user) | the milestone archive (update the path in FORK-DELTA item 20 in the same step) |
| Row D4 of the note quotes the archive sizes of the 2026-11-02 builds; a rebuild on a later date is one byte larger because the archive manifest holds the build date (E-12 `archive_size_delta_cause`) | carried; the entry counts match | fork maintainer (the user) | a decision to cite the sizes as exact (a plan 44-03 re-open) |
| The CHANGELOG heading date `2026-11-02` against the UTC date of the merge (bundle question 5) | pending the user's answer; if kept, a recorded deviation in the release record | fork maintainer (the user) | the user's answer, or any date-fix commit (the Freeze rule re-opens the gates) |
| Two `scratch/clt-07-*` branches on a public origin | stay with the user; no handoff asks about them; deleting a branch does not unpublish its commits | fork maintainer (the user) | the user's decision, or the milestone close |

## Assumptions the publication confirms or refutes

- Pull requests #21 and #22 are marked merged by GitHub once M lands (their heads are ancestors of the tip). Read their states after U5 (E-29).
- The publish run's tag and release will be authored by `github-actions[bot]`, as for the previous release. Read the tag ref, the release author and the asset uploader after U4 (E-27).
- The runner's archive size is close to E-11 `final_tip_bytes` (13564145). Read the `Check archive` notice of the dry run and of the publish run; the compare, not a size, decides.
- Testcontainers did not re-pull `latest`: the image digest read the same three times in E-13 and again in E-14. Read `docker image inspect` again if the suites are re-run.

## Not done by this phase

No push, no merge, no release tag, no dispatch, no pull-request action. No change to `src/`, a test, the build, `gradle/` or `.github/`. No edit of ROADMAP.md, REQUIREMENTS.md or STATE.md. The tag `v1.7.0_gg7` and the GitHub release will be created by the user's publish run after the user's direct approval; this record says nothing about that run having happened.
