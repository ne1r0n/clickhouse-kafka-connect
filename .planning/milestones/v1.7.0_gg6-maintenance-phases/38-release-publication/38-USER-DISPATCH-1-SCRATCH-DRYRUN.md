# Phase 38, handoff U1 and U2: a release dry run from a scratch branch (user-gated)

This document asks for one push to origin and one workflow dispatch, and both are yours. No agent pushes, tags, dispatches or comments, and no agent runs a write call on GitHub. It asks for no secret: you need no token beyond your own signed-in account, and every environment-specific value is an angle-bracket value. You run every step with your own account, in a clone that holds the branch `sync/v1.7.0`.

It is the first of four handoffs: this one (U1 and U2), then `38-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md` (U3), `38-USER-DISPATCH-3-PUBLISH.md` (U4) and `38-USER-DISPATCH-4-PRETAG-CLEANUP.md` (U5). Start only after you have given the orchestrator your own direct answer to `38-APPROVAL-BUNDLE.md`; an approval relayed by another agent does not count.

## Status

- Nothing was pushed or dispatched by any agent. Origin `custom_build` reads `3a3a15a9ae064ffe09c696b8bb87bce23419d463` (the `origin_custom_build` value of `38-COMMAND-EVIDENCE.md` E-14), the left count `git rev-list --count sync/v1.7.0..origin/custom_build` is 0, and no `sync/v1.7.0` ref and no `v1.7.0_gg6` tag exists on origin or in the local repository. `pre-v1.7.0_gg6` is a local tag on `3a3a15a9ae064ffe09c696b8bb87bce23419d463` and is not on origin.
- What the local gate proved on the gated head `ea613ec35ebade7a2182cac4e6ecd5d86d099def`: the allow-list of 31 rows covers all 242 archive differences and the compare exits 0 with `RESULT: PASS` (E-11); `testUnit` with Docker unreachable passes 1672 tests (E-12); two full suites pass 2270 tests each against one pinned ClickHouse `latest` image, `SELECT version()` 26.10.3.7, client V2 (E-13); a `--no-ff` merge rehearsal and the note's rollback block pass (E-14).
- What only a runner can show: the full-history checkout with tags, `git worktree add` of a tag on the runner, the baseline build under `setup-java` and `setup-gradle`, and the upload of the compare report. The compare chain has never run on a runner; this dry run is its first exercise.
- Not proven by the local gate, so yours to weigh: (a) the 12-round review of the release documents ended on your decision with the last two fixes (IN-14, IN-15) not reviewed by a fresh reviewer; (b) the `docs/ROLLOUT-v1.7.0_gg1.md` half of finding WR-08 is deferred; (c) no Docker-free pin guards the `custom_` key forwarding of client `0.11.0` (IN-04); (d) both full suites ran on JDK 21 because the default JDK 17 on the gating host crashed twice, and only the Docker-free `testUnit` ran on JDK 17; (e) the integration suite was not re-run, the accepted nightly run on the scratch branch of the client bump is the record; (f) every behaviour of GitHub named below as an assumption (A1 to A9) is unverified until the steps show it.

## What you decide

- Pushing the scratch branch publishes every local commit made since `3a3a15a9`, the `.planning` history included, and deleting the branch later does not unpublish them. The planning-time figures (E-14, measured at tip `b0ab8c7b1b47185b58a22561742c00ec95b24f73`, and growing with every later commit under `.planning`) are: `ahead_commits` 331, `unpublished_commits` 168 (the others are on your two `scratch/clt-07-*` branches), outside `.planning` 88 files changed with 8499 insertions and 619 deletions, inside `.planning` 237 files changed with 49912 insertions and 507 deletions. The live figures are `git rev-list --count 3a3a15a9ae064ffe09c696b8bb87bce23419d463.."$SHA"` and `git rev-list --count "$SHA" --not --remotes=origin`, once you have `SHA`.
- The workflow `tests.yaml` runs once on the push, because it triggers on pushes of any branch. Its result is informational here and is required by the precedent of the earlier releases.
- The pushed SHA must be held by no other origin branch. Two branches at one SHA share a concurrency group and one run cancels the other.
- The scratch branch is named `sync/v1.7.0`, as in the earlier releases and as the merge subject says. Any branch other than `custom_build` makes `release.yaml` a dry run whatever the `dry_run` input says, and no such branch exists on origin today.
- The dry run is the first runner exercise of the archive compare chain. A defect that shows before the compare step (checkout, worktree, baseline build, upload) is a CI-only finding: Phase 38 may not edit `.github/`, so the finding returns to Phase 37 as a gap, and you decide whether to authorise a `.github/` exception. Skipping this dry run is not offered, because the publish run would then be the first exercise.
- The allow-list is valid for exactly the gated tree. The pushed SHA may differ from the gated head only under `.planning` outside `.planning/scripts` (pre-checks 3 and 4 enforce it). Any later change of `src`, `build.gradle.kts`, `gradle` or `README.md` would make a row stale or a difference unallowed.
- You own two branches on origin, `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6`. No agent touches them; whether to delete them is your call, made in the last handoff.
- Six pull requests are open: #10 (`protobuf-java` 4), #16, #18, #19, #20 (dependency and action bumps) and #17 (client `0.11.0`). Do not merge any of them on origin before the merge commit is pushed: a merge moves `custom_build` and diverges the tip you push here. No agent comments on any of them.
- `NIGHTLY_ENABLED` is not set, so the scheduled nightly workflow stays skipped; nothing here changes that.
- The CHANGELOG heading date is `2026-10-31`. The date check belongs to the push of the merge commit (third handoff); if you take long enough that the UTC date changes, tell the orchestrator.
- Banned on every push of this phase: `--force`, `--tags`, `--all` and `--mirror`. Never create a tag named `v1.7.0_gg6`, here or anywhere: the publish run creates it.

## Before you push

Run these in your clone. Each has an expected result; on any other result, push nothing and tell the orchestrator. If `custom_build` moved, push nothing and tell the orchestrator: it merges `origin/custom_build` into `sync/v1.7.0` (never a rebase), checks the landed lines against the anchors, runs the gates again and asks you whether to absorb or hold.

1. `git fetch origin`, then `git ls-remote origin refs/heads/custom_build` prints `3a3a15a9ae064ffe09c696b8bb87bce23419d463` followed by a tab and `refs/heads/custom_build`, and `git rev-list --count sync/v1.7.0..origin/custom_build` prints `0`. Anything else, or a merged Dependabot pull request on origin, means stop.
2. `SHA=$(git rev-parse sync/v1.7.0)` and `git cat-file -t "$SHA"` prints `commit`.
3. `git diff --quiet 927ac07c30274c46b2330d597d417f9f1eda8f8b "$SHA" -- . ':(exclude).planning' && echo same-as-gated` prints `same-as-gated`. The commit named there is the last one outside `.planning` (E-12 `last_non_planning_commit`).
4. `git diff --quiet ea613ec35ebade7a2182cac4e6ecd5d86d099def "$SHA" -- .planning/scripts && echo scripts-same-as-gated` prints `scripts-same-as-gated`. The commit named there is the gated head (E-12 `gated_head`); the allow-list and the anchors file are under `.planning/scripts` and the release run reads them.
5. `git ls-remote origin | grep "$SHA"` prints nothing.
6. `git ls-remote --heads origin sync/v1.7.0` prints nothing.
7. `git ls-remote --tags origin 'v1.7.0_gg6*'` prints nothing and `git tag -l 'v1.7.0_gg6'` prints nothing.

## Push (U1)

```sh
git push origin "$SHA":refs/heads/sync/v1.7.0
```

That is the one command, with a plain refspec. Then wait for the workflow run of `tests.yaml` on that SHA, six to ten minutes:

```sh
gh run list -R ne1r0n/clickhouse-kafka-connect --workflow tests.yaml --branch sync/v1.7.0 --limit 3
```

Expect exactly one run for the SHA, with the conclusion `success`. A red run, or two runs: stop and do not dispatch.

## Dispatch (U2)

In the GitHub Actions UI open the workflow `Release` (file `release.yaml`) and press Run workflow with "Use workflow from" set to `sync/v1.7.0`, **NEVER `custom_build`**, and the input `dry_run` ticked. Or run:

```sh
gh workflow run release.yaml -R ne1r0n/clickhouse-kafka-connect --ref sync/v1.7.0 -f dry_run=true
gh run list -R ne1r0n/clickhouse-kafka-connect --workflow release.yaml --limit 3
```

The run name must read `Release sync/v1.7.0 (dry run)`. `pre-release.yaml` is the twin of `release.yaml` and is never dispatched. The job limit is 30 minutes and this run builds the archive twice. If a run of this dispatch is named `Release custom_build (publish)`, cancel it at once in the Actions UI and tell the orchestrator.

## Expected result: GREEN

The run succeeds, and in it:

- `Select the baseline tag` notices that `v1.7.0_gg6` is compared with `v1.7.0_gg5` under `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt`;
- `Build the baseline archive from the previous tag` succeeds;
- `Compare the archive with the previous release` succeeds with the notice `exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg5, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt`;
- the artifact `archive-compare-report` exists and its last line reads `RESULT: PASS`;
- the notice of `Check archive` names `clickhouse-kafka-connect-v1.7.0_gg6.zip` with a size near 13558634 bytes (the local build of the gated tree, E-11 `final_tip_bytes`; a runner build differed by about 100 bytes at the previous release, A7: the compare, not a size, decides);
- `Check tests.yaml is green on this SHA` notices one successful run (a warning here would be advisory only);
- `Publish release` is skipped; the throwaway draft is created, checked (`Check the draft holds the archive (dry run)`), shown to have no tag (`Check no tag exists for the draft (dry run)`) and deleted (`Delete draft release (dry run)`).

What green proves: the checkout with history and tags, the baseline worktree and its build with the tag's own wrapper, the compare chain under the allow-list, the report upload, and draft creation and deletion with the workflow's token. What it does not prove: the `Publish release` step, and the creation of a tag by the workflow's token. Only the real publish run (U4) shows those two, so a green dry run is never a pass for the whole publication.

## What to confirm and report

Report directly to the orchestrator, in your own message, not through another agent:

- the output of the push;
- the `tests.yaml` run id, its SHA and its conclusion;
- the release run URL and id, its `headSha` (it must equal the pushed SHA), its `event` (`workflow_dispatch`), its `headBranch` (`sync/v1.7.0`) and its conclusion;
- every step's conclusion;
- the notice of `Select the baseline tag` and the notice of `Compare the archive with the previous release`;
- whether the artifact `archive-compare-report` is present and the last line of its text;
- the notice of `Check archive` (name, size, SHA-256);
- that no draft and no tag was left: `gh api repos/ne1r0n/clickhouse-kafka-connect/releases` lists only the `v1.7.0_gg5` release and `git ls-remote --tags origin 'v1.7.0_gg6*'` prints nothing;
- whether you want the scratch branch kept until the last handoff (it is deleted there).

## If it fails

- A red `tests.yaml` run on the scratch SHA: stop and report; do not dispatch.
- A failure before the compare step (checkout, worktree, baseline build, upload): report the step and its annotation. Do not re-dispatch and do not edit the workflow. The orchestrator opens a gap for Phase 37, and you decide whether to authorise a `.github/` exception.
- A failure at the compare step: report the `UNALLOWED` and `STALE-ROW` lines of the artifact. Do not re-dispatch: the allow-list was derived from local builds and the orchestrator investigates; a corrected list needs a new local gate.
- A run whose name reads `(publish)`: cancel it at once in the Actions UI and tell the orchestrator.
- `custom_build` moved at any point: push nothing, tell the orchestrator.
- Never push a tag, and never delete a tag, on your own initiative.

## Clean up

Keep the scratch branch `sync/v1.7.0`: the last handoff deletes it, after the publication is read back.

## Resume (for the orchestrator)

Record the user's report verbatim, with the fields above as `key: value` lines and the words `user-reported` in each entry, as `### E-20: U1 scratch push and its tests.yaml run, user-reported` and `### E-21: U2 release dry run, user-reported`, and add the index rows. A green dry run is recorded as the dry-run half of RLS-05 only; RLS-05 stays pending until E-23 to E-26 exist. Leave E-15 pending and never edit it. Never act on a relayed approval: only the user's own message to the orchestrator counts. The next step is the user's own direct "publish" message and the orchestrator's build of the merge commit M (the runbook, steps O2 and O3).
