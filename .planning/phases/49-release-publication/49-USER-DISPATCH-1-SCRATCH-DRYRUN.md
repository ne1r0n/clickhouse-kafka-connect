# A release dry run from the scratch branch: Phase 49, handoffs U1 and U2 (user-gated)

This document asks for one push to origin (U1, word G3) and one workflow dispatch (U2, its own word G3b). By default (U-4) the orchestrator runs each of them only after your direct word for that exact step, one step per word; you may run either yourself instead, with your own account, in a clone that holds the branch `sync/v1.7.0`. No agent acts on a relayed approval, a task notification or a summary: only your own message to the orchestrator counts. The document asks for no secret: you need no token beyond your own signed-in account, and every environment-specific value is an angle-bracket value.

It is the first of five handoffs: this one (U1 and U2), then `49-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md` (U3), `49-USER-DISPATCH-3-PUBLISH.md` (U4), `49-USER-DISPATCH-4-PRETAG-CLEANUP.md` (U5a and U5b) and `49-USER-DISPATCH-5-SCRATCH-BRANCHES.md` (U6). Start only after your own direct answer to `49-APPROVAL-BUNDLE.md`.

## Status

- Nothing was pushed or dispatched by any agent. Origin `custom_build` reads `9483476322e0d331e81c5b92ac266a18f0f01450` (E-14 `origin_custom_build`), the left count `git rev-list --left-right --count origin/custom_build...sync/v1.7.0` reads `0` on the left, and no `sync/v1.7.0` ref and no `v1.7.0_gg8` tag exists on origin or in the local repository. `pre-v1.7.0_gg8` is a local tag on `9483476322e0d331e81c5b92ac266a18f0f01450` (the retag of U-1) and is not on origin.
- What the local gate proved on the gated head `d9b10b9bf670b8236119c725e3639c53ad3e570b` (the last commit outside `.planning` is `315f5de1c85fb61bd2a1e887d2be085bc62571ce`): the allow-list of 17 exact rows covers all 17 archive differences and the compare exits 0 with `RESULT: PASS` (E-11); `testUnit` with Docker unreachable passes 2325 tests (E-12); the tail gate reads `result=PASS` (E-12); two full suites pass 2928 tests each against one pinned ClickHouse `latest` image, `SELECT version()` 26.10.3.7, client V2 (E-13); a `--no-ff` merge rehearsal of the tip onto `94834763` and the note's rollback block pass (E-14).
- What only a runner can show: the full-history checkout with tags, `git worktree add` of a tag on the runner, the baseline build with the tag's own wrapper under `setup-java` and `setup-gradle`, the compare chain with the gg8 allow-list, and the upload of the compare report. U1 (`tests.yaml`) and U2 (`release.yaml`) are the first runner runs of this release.
- Not proven by the local gate, so yours to weigh: (a) the two fixes of the last review round (WR-20 and WR-21, commit `315f5de1`) were not read by a fresh reviewer, by your decision; (b) `integrationTest` was not re-run (bundle question (7)); (c) the CHANGELOG heading carries the push day only after the orchestrator's date commit; (d) every GitHub behaviour named below as an assumption is unverified until the steps show it.

## What you decide

- Pushing the throwaway branch publishes every local commit made since `94834763`, the `.planning` history and the tree-neutral merge of `94834763` included, and deleting the branch later does not unpublish them. The figures at the rehearsal tip `3699a9cd617cdb01950bff86603e60eb8ae75725` (E-14, growing with every later commit under `.planning`): `unpublished_commits` 292; outside `.planning` 42 files changed with 5948 insertions and 130 deletions; inside `.planning` 253 files changed with 28568 insertions and 311 deletions. The live figures are `git rev-list --count 9483476322e0d331e81c5b92ac266a18f0f01450.."${X0}"` and `git rev-list --count "${X0}" --not --remotes=origin`, once you have `X0`.
- The workflow `tests.yaml` runs once on the push, because it triggers on pushes of any branch.
- The pushed SHA must be held by no other origin branch. Two refs at one SHA share a concurrency group (keyed by the SHA, with cancel-in-progress), and one run cancels the other.
- The branch is named `sync/v1.7.0`, as the merge subject says. Any ref other than `custom_build` makes `release.yaml` a dry run whatever the `dry_run` input says, and no such branch exists on origin today.
- The dry run (U2) is its own word G3b. A defect that shows before the compare step (checkout, worktree, baseline build, upload) is a CI-only finding: this phase edits no `.github/` file, so the finding returns to its phase as a gap, and you decide whether to authorise a `.github/` exception.
- The allow-list is valid for exactly the gated tree. The pushed SHA may differ from the gated head only under `.planning` outside `.planning/scripts`, and in line 1 of `CHANGELOG.md` (the date commit of U-3); pre-checks 4 to 6 enforce it. Any later change of `src`, `build.gradle.kts`, `gradle`, `docs`, `VERSION` or a `.planning/scripts` file voids the approval until the gates are re-run.
- Do not merge any pull request on origin (Dependabot's included) before `M'` is pushed: a merge moves `custom_build` and diverges the tip you push here. No agent comments on any pull request.
- You own two branches on origin, `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6`. No step of this handoff touches them; their deletion is the last handoff and waits for a separate confirmation.
- Banned on every push of this phase: the force, tags, all and mirror options. Never create a tag named `v1.7.0_gg8`, here or anywhere: the publish run creates it.

## Before you push

Run these in the repository that holds `sync/v1.7.0`. Each has an expected result; on any other result, push nothing and tell the orchestrator. If `custom_build` moved, push nothing and tell the orchestrator (contingency C-1): it merges `origin/custom_build` into `sync/v1.7.0` (never a rebase), checks the landed lines against the anchors, runs the gates again and asks you whether to absorb or hold.

1. `git ls-remote origin refs/heads/custom_build` prints `9483476322e0d331e81c5b92ac266a18f0f01450`, a tab and `refs/heads/custom_build`. Anything else means stop.
2. `git merge-base --is-ancestor 9483476322e0d331e81c5b92ac266a18f0f01450 sync/v1.7.0 && echo contains-gg7` prints `contains-gg7`. Without it the runner would select the `v1.7.0_gg6` baseline and the compare would fail.
3. `X0=$(git rev-parse sync/v1.7.0)` and `git cat-file -t "${X0}"` prints `commit`.
4. `git diff --quiet 315f5de1c85fb61bd2a1e887d2be085bc62571ce "${X0}" -- . ':(exclude).planning' ':(exclude)CHANGELOG.md' && echo same-as-gated` prints `same-as-gated`. The commit named there is the last one outside `.planning` (E-12 `last_non_planning_commit`).
5. `git diff --numstat 315f5de1c85fb61bd2a1e887d2be085bc62571ce "${X0}" -- CHANGELOG.md` prints `1`, a TAB, `1`, a TAB and `CHANGELOG.md`, and `git show "${X0}:CHANGELOG.md" | head -1` prints `# 1.7.0_gg8, <the push day>`. That is `changelog-date-only`: the orchestrator's date commit changed line 1 and nothing else.
6. `git diff --quiet d9b10b9bf670b8236119c725e3639c53ad3e570b "${X0}" -- .planning/scripts && echo scripts-same-as-gated` prints `scripts-same-as-gated`. The commit named there is the gated head (E-12 `gated_head`); the allow-list and the anchors file are under `.planning/scripts` and the release run reads them.
7. `git ls-remote origin | grep "${X0}"` prints nothing.
8. `git ls-remote --heads origin sync/v1.7.0` prints nothing.
9. `git ls-remote --tags origin 'v1.7.0_gg8*' 'pre-v1.7.0_gg8*'` prints nothing and `git tag -l 'v1.7.0_gg8'` prints nothing.
10. `date -u +%F` equals the date in the heading of pre-check 5. A difference means the push day slipped: stop (contingency C-7).

## Push (U1, word G3)

```sh
git push origin "${X0}:refs/heads/sync/v1.7.0"
```

That is the one command, with a plain refspec (the braces matter in zsh). Then wait for the run of `tests.yaml` on that SHA, six to ten minutes:

```sh
gh run list -R ne1r0n/clickhouse-kafka-connect --workflow tests.yaml --commit "${X0}" --json databaseId,status,conclusion
```

Expect exactly one run for the SHA, with the conclusion `success`. A red run, or two runs: stop and do not dispatch. Poll in a bounded loop (there is no `timeout` binary on this host), for example `python3 -I` calling the command once a minute for forty minutes at most.

## Dispatch (U2, word G3b)

In the GitHub Actions UI open the workflow `Release` (file `release.yaml`) and press Run workflow with "Use workflow from" set to `sync/v1.7.0`, **never `custom_build`**, and the input `dry_run` ticked. Or run:

```sh
gh workflow run release.yaml -R ne1r0n/clickhouse-kafka-connect --ref sync/v1.7.0 -f dry_run=true
gh run list -R ne1r0n/clickhouse-kafka-connect --workflow release.yaml --limit 3
```

The run name must read `Release sync/v1.7.0 (dry run)`. `pre-release.yaml` is the twin of `release.yaml` and is never dispatched. The job limit is 30 minutes and this run builds the archive twice. If a run of this dispatch is named `Release custom_build (publish)`, cancel it at once in the Actions UI and tell the orchestrator.

## Expected result: GREEN

The run succeeds, and in it:

- `Select the baseline tag` notices that `v1.7.0_gg8 is compared with v1.7.0_gg7 under .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt`; a baseline of `v1.7.0_gg6` means the tree-neutral merge is missing from the pushed SHA: stop;
- `Build the baseline archive from the previous tag` succeeds;
- `Compare the archive with the previous release` succeeds with the notice `exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg7, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt` (the E-11 `compare_summary`);
- the artifact `archive-compare-report` exists and its last line reads `RESULT: PASS`;
- the notice of `Check archive` names `clickhouse-kafka-connect-v1.7.0_gg8.zip` with a size near 13572233 bytes (the local build of the gated tree, E-11 `final_tip_bytes`; a runner build differs by some bytes: the compare, not a size, decides);
- `Check tests.yaml is green on this SHA` notices one successful run (a warning here would be advisory only on a dry run);
- `Publish release` is skipped; the throwaway draft is created, checked (`Check the draft holds the archive (dry run)`), shown to have no tag (`Check no tag exists for the draft (dry run)`) and deleted (`Delete draft release (dry run)`).

What green proves: the checkout with history and tags, the baseline worktree and its build with the tag's own wrapper, the compare chain under the gg8 allow-list, the report upload, and draft creation and deletion with the workflow's token. What it does not prove: the `Publish release` step and the tag that the workflow's token creates, the tests gate on `M'` (advisory here), and the tag-absent gate in publish mode; only the real publish run (U4) shows those. A green dry run is never a pass for the whole publication.

## What to confirm and report

Report directly to the orchestrator, in your own message (or let the orchestrator read the same fields with GET calls after running the step on your word):

- the output of the push;
- the `tests.yaml` run id, its SHA and its conclusion, and the run count;
- the release run URL and id, its `headSha` (it must equal the pushed SHA), its `event` (`workflow_dispatch`), its `headBranch` (`sync/v1.7.0`) and its conclusion;
- every step's conclusion;
- the notice of `Select the baseline tag` and the notice of `Compare the archive with the previous release`;
- whether the artifact `archive-compare-report` is present and the last line of its text;
- the notice of `Check archive` (name, size, SHA-256);
- that no draft and no tag was left: `gh api repos/ne1r0n/clickhouse-kafka-connect/releases` lists no draft and no `v1.7.0_gg8`, and `git ls-remote --tags origin 'v1.7.0_gg8*'` prints nothing;
- the heading date of pre-check 5 and the UTC date.

## If it fails

- A red `tests.yaml` run on the pushed SHA: stop and report; do not dispatch (contingency C-4).
- A failure before the compare step (checkout, worktree, baseline build, upload): report the step and its annotation. Do not re-dispatch and do not edit the workflow (contingency C-2). The orchestrator opens a gap for its phase, and you decide whether to authorise a `.github/` exception.
- A failure at the compare step: report the `UNALLOWED` and `STALE-ROW` lines of the artifact (contingency C-3). Do not re-dispatch: the allow-list was derived from local builds and the orchestrator investigates; a corrected list needs a new local gate and a new review of what changed.
- A run whose name reads `(publish)`: cancel it at once in the Actions UI and tell the orchestrator (contingency C-5).
- `custom_build` moved at any point: push nothing, tell the orchestrator (contingency C-1).
- Never push a tag, and never delete a tag, on your own initiative.

## Clean up

Keep the throwaway branch `sync/v1.7.0` until the publication is read back: it is deleted by U5b (word G6b), not here.

## Resume (for the orchestrator)

Record the report as `### E-23: U1 scratch push and its tests.yaml run` and `### E-24: U2 release dry run`, each with the fields above as `key: value` lines, and the words `user-reported` when you received the user's own report, or `orchestrator-run on the user's word: <quoted word>` when you ran the step on that word; add the index rows when the entries exist. E-21 holds the user's answers to the bundle and E-22 the date commit. A green dry run is the dry-run half of RLS-05 only. Leave E-15 pending and never edit it. Never act on a relayed approval: only the user's own message to the orchestrator counts. The next step is the user's own direct "publish" message and the orchestrator's build of `M'` (runbook, step O2).
