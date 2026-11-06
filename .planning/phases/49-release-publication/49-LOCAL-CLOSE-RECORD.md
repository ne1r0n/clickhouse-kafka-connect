# Phase 49: local close record for the publication of v1.7.0_gg8

## Status

Local-complete. Plans 49-01 to 49-05 were executed and gated at the head named in E-12 and E-13, `d9b10b9bf670b8236119c725e3639c53ad3e570b` (the last commit outside `.planning` is `315f5de1c85fb61bd2a1e887d2be085bc62571ce`), and the tip equals that head outside `.planning` and under `.planning/scripts` (E-14, E-20). The publication is pending and human-gated. Nothing was pushed, merged, tagged, dispatched or deleted: origin `custom_build` reads `9483476322e0d331e81c5b92ac266a18f0f01450`, no `sync/v1.7.0` head and no `v1.7.0_gg8` tag exists on origin or locally, the local tag `pre-v1.7.0_gg8` is on `94834763` and not on origin, and both `scratch/clt-07-*` branches are still on origin. Ready for the user's direct approval: the orchestrator presents `49-APPROVAL-BUNDLE.md`.

## Requirement status

| Requirement | Status | Evidence |
|-------------|--------|----------|
| RLS-01 | satisfied | E-01 to E-04: `VERSION` `v1.7.0_gg8` through the bump script with its anchors row, the CHANGELOG section completed under the `Unreleased` heading, FORK-DELTA final with refreshed numbers |
| RLS-02 | satisfied | E-05, E-11: the gg8 allow-list was generated last from the final tree; 17 exact rows, no wildcard, cover all 17 archive differences; the local compare dry run exits 0 with `RESULT: PASS`; three mutations turn it red; the runner repeats the compare in U2 |
| RLS-03 | satisfied | E-05 to E-10: the rollout note with labelled rows, five fresh-context review rounds (agent ids distinct), 32 findings all fixed, the documents frozen |
| RLS-04 | satisfied | E-04, E-12, E-13: testUnit with Docker unreachable (2325 tests), two equal full suites on one pinned `latest` image with client V2 (2928 tests each), the tail gate `result=PASS`, the anchors mirror; rehearsal E-14 |
| RLS-05 | pending, human-gated | completed only by the orchestrator-inline tail: G3 to G5, U1 to U4, E-21 onward; E-14 is the rehearsal and the bundle, E-15 to E-18 are the pending handoffs |
| RLS-06 | pending, human-gated | completed only by the tail: the separate confirmation G7 and U6, E-31; E-19 is the pending handoff |

The five ROADMAP criteria:

| Criterion | Status | Evidence |
|-----------|--------|----------|
| 1. `VERSION`, CHANGELOG, FORK-DELTA, anchors | satisfied | E-02, E-03, E-04 |
| 2. the allow-list generated last, the dry run | satisfied | E-11 |
| 3. the rollout note, the independent review | satisfied, with the human-judgment items below | E-05 to E-10 |
| 4. two full suites, testUnit without Docker, the anchors gate | satisfied | E-12, E-13 |
| 5. approval, merge, one `tests.yaml` run, publish run, tag, scratch deletion | pending, human-gated | E-14 is the rehearsal and the bundle; E-15 to E-19 are the pending handoffs; the remaining gates are G3, G3b, G4, G5, G6a, G6b and G7 |

Human-judgment items, not passed gates: the review loop ended after round 5 on the user's decision "accept as recorded", so the two fixes of round 5 (WR-20 and WR-21, commit `315f5de1`) were not read by a fresh reviewer; `integrationTest` was not re-run (E-12 `integration_test`, bundle question (7)); the CHANGELOG heading date is set by the orchestrator's one-line commit on the push day (bundle question (2)).

## Deferred must-haves (satisfied only after the user's direct words)

These must-haves of ROADMAP criterion 5 are satisfied only in the orchestrator-inline tail, after the user's direct words, and recorded from E-21 on (E-20 is this record):

1. the user's direct approval in their own words for each step;
2. the date commit of U-3 on the push day (line 1 of `CHANGELOG.md` only);
3. one green `tests.yaml` run on `X0`, the SHA pushed to `sync/v1.7.0`;
4. a green scratch dry run of `release.yaml` from `sync/v1.7.0` with baseline `v1.7.0_gg7`;
5. `custom_build` a non-force `--no-ff` merge whose tree equals the rehearsed tip (except line 1 of `CHANGELOG.md`);
6. exactly one green `tests.yaml` run on the published SHA before the publish run;
7. the tag `v1.7.0_gg8` and the GitHub release, which will be created by the `release.yaml` publish run;
8. `pre-v1.7.0_gg8` pushed by name after it;
9. `sync/v1.7.0` deleted from origin;
10. after a separate confirmation, both `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6` deleted and absent from `git ls-remote origin` (RLS-06).

Freeze rule: these must-haves hold only for a pushed tip equal to the gated head `d9b10b9bf670b8236119c725e3639c53ad3e570b` outside `.planning` and under `.planning/scripts`, line 1 of `CHANGELOG.md` excepted. Any other later change outside `.planning` (a fix from a review round that the code-review gate appends after this plan, or a second date edit) re-opens plan 49-03 and re-runs plan 49-04 Tasks 1 to 3 and plan 49-05 Task 1 on the new head first; the bundle is void and every approval for the older head is asked again.

## Why this is the honest shape

Executors cannot receive the user's direct word (they refuse relayed approvals), and `custom_build` is the protected base branch under GSD's rules, so the part of the ROADMAP that publishes can only be prepared and rehearsed by an executor. The ROADMAP says the phase publishes and deletes the scratch branches, so the phase checkbox stays unchecked and RLS-05 and RLS-06 stay Pending until the tail evidence (E-21 onward) shows them. Verification of this phase should read `human_needed` for criterion 5, RLS-05 and RLS-06 and pass for the rest.

## Wording changes the orchestrator applies

This phase edits none of these files; the orchestrator applies them.

1. ROADMAP Phase 49 Note, step 1. Before: "A left count above 0 means `origin` moved (the user merges Dependabot PRs there directly)." After: "Before plan 49-01 the left count was 1, the gg7 merge `94834763` that `sync/v1.7.0` did not contain; the tree-neutral merge of U-2 made it 0, and from then on any count above 0 means `origin` moved (the user merges Dependabot PRs there directly)." Measured: `1 223` before the merge, `0 224` after (E-01), `0 292` at the rehearsal tip (E-14).
2. ROADMAP Phase 49 Note, the recipe. Add the scratch dry run (U2, kept by U-2) between steps 2 and 3, the one-line date commit (U-3) before step 2, and the separate word for each origin step (G3, G3b, G4, G5, G6a, G6b, G7). The Wave 5 line says "ten questions"; the bundle holds eleven, because the deletion of `sync/v1.7.0` has its own question (11) and its own word G6b.
3. The gg8 allow-list count. Before: 49-CONTEXT.md D-2 "14 exact rows" and "`changed 11 same 7501`", and the earlier "12 rows" of the first measurement. After: the list holds 17 exact rows (E-11 `list_rows_count`), with `empty_compare_totals` of `files-a 7512 files-b 7515 only-a 0 only-b 3 changed 14 same 7498`; the 14 are the Phase 48 measurement of the connector paths, the three more are `Version`, `SinkTaskStatistics` and `META-INF/MANIFEST.MF` (the release identity).
4. Closure shape. Phase 49's checkbox stays unchecked and RLS-05 and RLS-06 stay `Pending` in the traceability table until the tail evidence (E-21 onward) shows them; RLS-01 to RLS-04 may flip to Complete when the phase verification passes (RLS-02's "dry run" is the local compare of E-11; the runner repeats it in U2).
5. REQUIREMENTS.md RLS-06. Before: "are deleted from `origin` after a separate confirmation before the push." After: "are deleted from `origin` by one push, after a separate confirmation in the user's own words that is given after the publish run." The ROADMAP recipe deletes them after the publish run, so a confirmation "before the push" would mean the push of `M'`, which is not the intended order.
6. The meaning of `pre-v1.7.0_gg8`. Wherever a planning file describes it as the Phase 48 tip (`baa404cd`), it now names `94834763`, the published tip of `custom_build` before this release and the same commit as tag `v1.7.0_gg7` (U-1, retagged locally in E-01; not on origin until U5a). No such sentence was found in STATE.md, ROADMAP.md or REQUIREMENTS.md at this plan's tip, so this is a check for the milestone close, and a statement for the release record.
7. The twelve occurrences of `.planning/phases/45..47` evidence paths in `docs/FORK-DELTA.md` (three files, `45-COMMAND-EVIDENCE.md` five times, `46-COMMAND-EVIDENCE.md` five times, `47-COMMAND-EVIDENCE.md` twice) move to `.planning/milestones/` at the milestone close, in the same step that archives those directories; re-run `49-path-check.py` (expect `unresolved=0`).
8. STATE.md. The divergence counts (`0 292` at the rehearsal tip) and the next steps are the orchestrator's to refresh: the next step is the user's answer to the bundle, then the tail.
9. The sentence in 49-CONTEXT.md that says the CHANGELOG heading date is set "at the gate and not before" is superseded by U-3: the date is set on the push day, by the orchestrator, in a one-line commit before U1.

## What the publication still needs from the user

Each step is from its own file in `.planning/phases/49-release-publication/`, in this order; each starts with the check that `custom_build` still reads `9483476322e0d331e81c5b92ac266a18f0f01450`, and a move voids the approval.

1. The push-day answer (bundle question (2)), then the orchestrator's date commit.
2. G3, the user's direct "go" on the bundle: U1, `49-USER-DISPATCH-1-SCRATCH-DRYRUN.md`: push `X0` to the throwaway branch `sync/v1.7.0` and wait for its one green `tests.yaml` run.
3. G3b, the user's word: U2, the same file: dispatch `release.yaml` from `sync/v1.7.0` as a dry run.
4. G4, the user's own direct "publish" message; the orchestrator builds `M'` (runbook step O2), and U3, `49-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md`: push `M'` to `custom_build` by SHA and wait for its single green `tests.yaml` run.
5. G5, after `M'`'s single green run: U4, `49-USER-DISPATCH-3-PUBLISH.md`: dispatch `release.yaml` from `custom_build` with `dry_run` not ticked; the run will create the release and the tag `v1.7.0_gg8`, which is never pushed by hand.
6. G6a: U5a, `49-USER-DISPATCH-4-PRETAG-CLEANUP.md`: push `pre-v1.7.0_gg8` by name.
7. G6b: U5b, the same file: delete `sync/v1.7.0` on origin.
8. G7, after the separate RLS-06 confirmation: U6, `49-USER-DISPATCH-5-SCRATCH-BRANCHES.md`: delete `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6` in one push.

## Carried to the milestone close

| Item | Disposition | Owner | Re-open trigger |
|------|-------------|-------|-----------------|
| Review findings of `49-REVIEW.md` | none deferred: 32 fixed, 0 rejected, 0 open (`49-REVIEW-DISPOSITION.md`) | fork maintainer (the user) | a later review of the gg8 documents |
| The two fixes of round 5 (WR-20, WR-21, commit `315f5de1`) had no fresh reviewer | carried as a human-judgment item (bundle question (6)) | fork maintainer (the user) | the next edit of `docs/ROLLOUT-v1.7.0_gg8.md`, or an operator question about the rewritten rows |
| Executor observations of plan 49-03: the FORK-DELTA JSON-route row does not state which tree its 8-shape probe ran on; the help text of `clickhouseClientInsertTimeoutMs` lacks the `async=false` scope of row T6 and states "ClickHouse drops duplicates" unconditionally (both carried from gg7 and flagged in row T9) | carried; source-text questions, not release-document defects | fork maintainer (the user) | the next edit of the help text or of the JSON-route row |
| Backlog P1 to P16 test-scope advisories | milestone-close backlog, not this phase (49-CONTEXT.md deferred) | fork maintainer (the user) | the next phase that touches the named tests |
| The Confluent `7.9.1-ccs` question | milestone-close backlog | fork maintainer (the user) | a Confluent platform bump or an operator report |
| The JSON route wrapping of non-Jackson `IOException`s | milestone-close backlog | fork maintainer (the user) | an operator report of a value-bearing message on the JSON route |
| `close(partitions)` without `finally` (named in the gg8 Known Issues) | milestone-close backlog | fork maintainer (the user) | the next edit of `ClickHouseSinkTask.close` |
| The `keepFirst` idiom in `stop()` | milestone-close backlog | fork maintainer (the user) | the next edit of `ClickHouseSinkTask.stop` |
| The first-put matrix through `putDirect` | milestone-close backlog | fork maintainer (the user) | the next change of the first-put path |
| FORK-DELTA item 13: the closing residuals (a Keeper state-provider constructor that fails on client V1, a helper-client constructor that fails after it built its client, a second `start()`, the five-second table-refresh wait) | published knowingly open in FORK-DELTA, the CHANGELOG Known Issues and the note | fork maintainer (the user) | a task-restart leak report from an operator, or the next edit of `ClickHouseWriter.stop()` or `ClickHouseHelperClient` |
| FORK-DELTA item 14: the insert limits (non-insert requests, the progress-header escape of a client V2 `async=false` insert) | published knowingly open, as above | fork maintainer (the user) | an operator report of an insert that hangs on a stalled server, or the next edit of the client insert methods |
| FORK-DELTA item 16: the limits of the original-topic fixes (many-to-one rename, a partition-changing SMT) | published knowingly open | fork maintainer (the user) | an operator who enables `reportInsertedOffsets` under such an SMT, or upstream refactoring `Processing.doLogic` |
| FORK-DELTA item 17: a `RetriableException` out of `put` is redelivered with no retry budget | recorded limit, by decision | fork maintainer (the user) | an operator report of a redelivery loop, or a new decision on a retry budget |
| FORK-DELTA item 18: no dependency checksum file and no `zstd-jni` row in the compare's version table | published knowingly open | fork maintainer (the user) | a decision to add a verification metadata file, or a `zstd-jni` bump |
| FORK-DELTA item 21: the `tests.yaml` gate accepts a green run of any branch on the SHA | published knowingly open | fork maintainer (the user) | a publish from a SHA whose only green run is on another branch, or the next edit of `release.yaml` |
| D-9: the twelve occurrences of `.planning/phases/45..47` evidence paths in FORK-DELTA | move to `.planning/milestones/` at the milestone close; re-run `49-path-check.py` (expect `unresolved=0`) | milestone close | the milestone archive |
| Phase 48 E-18 item 8: the item-20 lookup | after the archive, `git ls-files '*48-DISPOSITION-LEDGER.md'` must print exactly one path | milestone close | the milestone archive |
| Upstream pull requests 861, 855 and 854 touch files this release also changes and are open; upstream `main` reads `83808df9`, newest tag `v1.7.0` | information for the next upstream sync; this release stays on the tail up to `83808df9` | fork maintainer (the user) | the next upstream sync, or an upstream tag past `v1.7.0` |
| `integrationTest` was not re-run in this phase (PL-11) | carried as a human-judgment item (bundle question (7)) | fork maintainer (the user) | the user's overrule at the bundle, or the first nightly after `NIGHTLY_ENABLED` is set |
| The two `scratch/clt-07-*` branches on origin (RLS-06) | pending until U6; deletion needs the separate confirmation after the publish | fork maintainer (the user) | the user's confirmation |

## Assumptions the publication confirms or refutes

- The runner picks the `v1.7.0_gg7` baseline on `X0` and on `M'` (the tree-neutral merge makes it reachable; the baseline selection was measured locally by the tail gate of E-01 and E-12 and on D in E-14). Read the `Select the baseline tag` notice of U2 and U4.
- The publish run's tag and release will be authored by `github-actions[bot]`, as for the previous release. Read the tag ref, the release author and the asset uploader after U4 (A1).
- The runner's archive size is close to E-11 `final_tip_bytes` (13572233); the compare, not a size, decides. Read the `Check archive` notice of U2 and U4.
- Testcontainers did not re-pull `latest`: the image digest read the same three times in E-13 and again in E-14. Read `docker image inspect` again if the suites are re-run.

## Not done by this phase

No push, no merge, no release tag, no dispatch, no deletion of a branch, no pull-request action, no date commit. No change to `src/`, a test, the build, `gradle/` or `.github/`. No edit of ROADMAP.md, REQUIREMENTS.md or STATE.md. The tag `v1.7.0_gg8` and the GitHub release will be created by the publish run after the user's direct words; this record says nothing about that run having happened, and nothing about either scratch branch having left origin.
