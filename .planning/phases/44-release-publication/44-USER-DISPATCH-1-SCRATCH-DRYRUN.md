# A release dry run from the scratch branch: Phase 44, handoffs U1 and U2 (user-gated)

This document asks for one push to origin and one workflow dispatch, and both are yours. No agent pushes, tags, dispatches or comments unless your answer to question (1) of `44-APPROVAL-BUNDLE.md` lets the orchestrator run the pushes, and then only on your direct word for each step; the publish dispatch (the third handoff) is always yours. It asks for no secret: you need no token beyond your own signed-in account, and every environment-specific value is an angle-bracket value. You run every step with your own account, in a clone that holds the branch `sync/v1.7.0`.

It is the first of four handoffs: this one (U1 and U2), then `44-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md` (U3), `44-USER-DISPATCH-3-PUBLISH.md` (U4) and `44-USER-DISPATCH-4-PRETAG-CLEANUP.md` (U5). Start only after you have given the orchestrator your own direct answer to `44-APPROVAL-BUNDLE.md`; an approval relayed by another agent does not count.

## Status

- Nothing was pushed or dispatched by any agent. Origin `custom_build` reads `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` (the `origin_custom_build` value of `44-COMMAND-EVIDENCE.md` E-14), the left count `git rev-list --count sync/v1.7.0..origin/custom_build` is 0, and no `sync/v1.7.0` ref and no `v1.7.0_gg7` tag exists on origin or in the local repository. `pre-v1.7.0_gg7` is a local tag on `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` and is not on origin.
- What the local gate proved on the gated head `b265f5a45a0752f314bb95315e3cdfbeaa2b78cd`: the allow-list of 18 rows covers all 120 archive differences and the compare exits 0 with `RESULT: PASS` (E-11); `testUnit` with Docker unreachable passes 1826 tests (E-12, E-20); two full suites pass 2425 tests each against one pinned ClickHouse `latest` image, `SELECT version()` 26.10.3.7, client V2 (E-13); a `--no-ff` merge rehearsal and the note's rollback block pass (E-14).
- What only a runner can show: the full-history checkout with tags, `git worktree add` of a tag on the runner, the baseline build under `setup-java` and `setup-gradle`, and the upload of the compare report. U1 (`tests.yaml`) and U2 (`release.yaml`) are the first GitHub runs of the `setup-gradle` 6.4.1 pin that this release carries. The compare chain ran on a runner for the previous release; this is its first run with the gg7 allow-list.
- Not proven by the local gate, so yours to weigh: (a) the review of the release documents ended after round 4 on your decision, and the last fixes were not read by a fresh reviewer; (b) finding IN-03 is deferred (the published gg6 note); (c) the F-2 progress-header claim is carried as `derived (mechanism, not measured)`; (d) the integration suite was not re-run; (e) the CHANGELOG heading date (question (5) of the bundle); (f) every GitHub behaviour named below as an assumption is unverified until the steps show it.

## What you decide

- Pushing the scratch branch publishes every local commit made since `72e8ccff`, the `.planning` history included, and deleting the branch later does not unpublish them. The figures (E-14, measured at tip `45cbc9db5bba2e5f367eff4bb7043251587e44e2`, and growing with every later commit under `.planning`) are: `ahead_of_gg6` 261, `unpublished_commits` 259 (the others are on your two `scratch/clt-07-*` branches), outside `.planning` 40 files changed with 3167 insertions and 146 deletions, inside `.planning` 301 files changed with 30107 insertions and 1427 deletions. The live figures are `git rev-list --count 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236.."${SHA}"` and `git rev-list --count "${SHA}" --not --remotes=origin`, once you have `SHA`.
- The workflow `tests.yaml` runs once on the push, because it triggers on pushes of any branch.
- The pushed SHA must be held by no other origin branch. Two branches at one SHA share a concurrency group and one run cancels the other.
- The scratch branch is named `sync/v1.7.0`, as in the earlier releases and as the merge subject says. Any branch other than `custom_build` makes `release.yaml` a dry run whatever the `dry_run` input says, and no such branch exists on origin today.
- A defect that shows before the compare step (checkout, worktree, baseline build, upload) is a CI-only finding: this phase edits no `.github/` file, so the finding returns to its phase as a gap, and you decide whether to authorise a `.github/` exception. Skipping this dry run is not offered, because the publish run would then be the first exercise of the new pin.
- The allow-list is valid for exactly the gated tree. The pushed SHA may differ from the gated head only under `.planning` outside `.planning/scripts` (pre-checks 4 and 5 enforce it). Any later change of `src`, `build.gradle.kts`, `gradle`, `docs`, `CHANGELOG.md` or `VERSION` voids the approval until the gates are re-run.
- You own two branches on origin, `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6`. No agent touches them; whether to delete them is your call.
- Two pull requests are open: #21 (`14be67ce`, an actions bump) and #22 (`b5cccbd3`, `gson` 2.15.0). Do not merge either on origin before the merge commit is pushed: a merge moves `custom_build` and diverges the tip you push here. No agent comments on either.
- The CHANGELOG heading date is `2026-11-02` and the UTC date has moved on; your answer to question (5) of the bundle covers it. Pre-check 9 prints both so you can tell the orchestrator what you see.
- Banned on every push of this phase: the force, tags, all and mirror options. Never create a tag named `v1.7.0_gg7`, here or anywhere: the publish run creates it.

## Before you push

Run these in your clone. Each has an expected result; on any other result, push nothing and tell the orchestrator. If `custom_build` moved, push nothing and tell the orchestrator (contingency C-1): it merges `origin/custom_build` into `sync/v1.7.0` (never a rebase), checks the landed lines against the anchors, runs the gates again and asks you whether to absorb or hold.

1. `git fetch origin`, then `git ls-remote origin refs/heads/custom_build` prints `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` followed by a tab and `refs/heads/custom_build`. Anything else means stop.
2. `git rev-list --left-right --count origin/custom_build...sync/v1.7.0` prints `0` on the left (the right count is the number of local commits). A non-zero left count, or a merged Dependabot pull request on origin, means stop.
3. `SHA=$(git rev-parse sync/v1.7.0)` and `git cat-file -t "${SHA}"` prints `commit`.
4. `git diff --quiet a43ff32f39647dfc3afb9849f6729b251f2619d2 "${SHA}" -- . ':(exclude).planning' && echo same-as-gated` prints `same-as-gated`. The commit named there is the last one outside `.planning` (E-12 `last_non_planning_commit`).
5. `git diff --quiet b265f5a45a0752f314bb95315e3cdfbeaa2b78cd "${SHA}" -- .planning/scripts && echo scripts-same-as-gated` prints `scripts-same-as-gated`. The commit named there is the gated head (E-12 `gated_head`); the allow-list and the anchors file are under `.planning/scripts` and the release run reads them.
6. `git ls-remote origin | grep "${SHA}"` prints nothing.
7. `git ls-remote --heads origin sync/v1.7.0` prints nothing.
8. `git ls-remote --tags origin 'v1.7.0_gg7*'` prints nothing and `git tag -l 'v1.7.0_gg7'` prints nothing.
9. `date -u +%F` against `head -1 CHANGELOG.md` (it reads `# 1.7.0_gg7, 2026-11-02`): a difference is expected and was put to you in question (5) of the bundle; report both values. If you chose to hold for a date-fix, stop: the head changed and the bundle is void.

## Push (U1)

```sh
git push origin "${SHA}":refs/heads/sync/v1.7.0
```

That is the one command, with a plain refspec (the braces matter in zsh). Then wait for the workflow run of `tests.yaml` on that SHA, six to ten minutes:

```sh
gh run list -R ne1r0n/clickhouse-kafka-connect --workflow tests.yaml --branch sync/v1.7.0 --limit 3
```

Expect exactly one run for the SHA, with the conclusion `success`. A red run, or two runs: stop and do not dispatch. Poll in a bounded loop (there is no `timeout` binary on this host), for example twenty reads a minute apart.

## Dispatch (U2)

In the GitHub Actions UI open the workflow `Release` (file `release.yaml`) and press Run workflow with "Use workflow from" set to `sync/v1.7.0`, **NEVER `custom_build`**, and the input `dry_run` ticked. Or run:

```sh
gh workflow run release.yaml -R ne1r0n/clickhouse-kafka-connect --ref sync/v1.7.0 -f dry_run=true
gh run list -R ne1r0n/clickhouse-kafka-connect --workflow release.yaml --limit 3
```

The run name must read `Release sync/v1.7.0 (dry run)`. `pre-release.yaml` is the twin of `release.yaml` and is never dispatched. The job limit is 30 minutes and this run builds the archive twice. If a run of this dispatch is named `Release custom_build (publish)`, cancel it at once in the Actions UI and tell the orchestrator.

## Expected result: GREEN

The run succeeds, and in it:

- `Select the baseline tag` notices that `v1.7.0_gg7` is compared with `v1.7.0_gg6` under `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt`;
- `Build the baseline archive from the previous tag` succeeds;
- `Compare the archive with the previous release` succeeds with the notice `exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg6, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt` (the E-11 `compare_summary`);
- the artifact `archive-compare-report` exists and its last line reads `RESULT: PASS`;
- the notice of `Check archive` names `clickhouse-kafka-connect-v1.7.0_gg7.zip` with a size near 13564145 bytes (the local build of the gated tree, E-11 `final_tip_bytes`; a runner build differs by some bytes: the compare, not a size, decides);
- `Check tests.yaml is green on this SHA` notices one successful run (a warning here would be advisory only);
- `Publish release` is skipped; the throwaway draft is created, checked (`Check the draft holds the archive (dry run)`), shown to have no tag (`Check no tag exists for the draft (dry run)`) and deleted (`Delete draft release (dry run)`).

What green proves: the checkout with history and tags, the baseline worktree and its build with the tag's own wrapper, the compare chain under the gg7 allow-list, the report upload, the `setup-gradle` 6.4.1 pin on a runner, and draft creation and deletion with the workflow's token. What it does not prove: the `Publish release` step of this release and the tag the workflow's token creates; the previous release's publish run proved that step once, and only the real publish run (U4) shows it for this one. A green dry run is never a pass for the whole publication.

## What to confirm and report

Report directly to the orchestrator, in your own message, not through another agent:

- the output of the push;
- the `tests.yaml` run id, its SHA and its conclusion;
- the release run URL and id, its `headSha` (it must equal the pushed SHA), its `event` (`workflow_dispatch`), its `headBranch` (`sync/v1.7.0`) and its conclusion;
- every step's conclusion;
- the notice of `Select the baseline tag` and the notice of `Compare the archive with the previous release`;
- whether the artifact `archive-compare-report` is present and the last line of its text;
- the notice of `Check archive` (name, size, SHA-256);
- that no draft and no tag was left: `gh api repos/ne1r0n/clickhouse-kafka-connect/releases` lists only the `v1.7.0_gg6` and `v1.7.0_gg5` releases and `git ls-remote --tags origin 'v1.7.0_gg7*'` prints nothing;
- the two values of pre-check 9 (the heading date and the UTC date);
- whether you want the scratch branch kept until the last handoff (it is deleted there).

## If it fails

- A red `tests.yaml` run on the scratch SHA: stop and report; do not dispatch (contingency C-4).
- A failure before the compare step (checkout, worktree, baseline build, upload): report the step and its annotation. Do not re-dispatch and do not edit the workflow (contingency C-2). The orchestrator opens a gap for its phase, and you decide whether to authorise a `.github/` exception.
- A failure at the compare step: report the `UNALLOWED` and `STALE-ROW` lines of the artifact (contingency C-3). Do not re-dispatch: the allow-list was derived from local builds and the orchestrator investigates; a corrected list needs a new local gate.
- A run whose name reads `(publish)`: cancel it at once in the Actions UI and tell the orchestrator (contingency C-5).
- `custom_build` moved at any point: push nothing, tell the orchestrator (contingency C-1).
- Never push a tag, and never delete a tag, on your own initiative.

## Clean up

Keep the scratch branch `sync/v1.7.0`: the last handoff deletes it, after the publication is read back.

## Resume (for the orchestrator)

Record the user's report verbatim, with the fields above as `key: value` lines and the words `user-reported` in each entry, as `### E-22: U1 scratch push and its tests.yaml run, user-reported` and `### E-23: U2 release dry run, user-reported` (E-21 holds the user's answers to the bundle; E-20 is already the plan 44-04 hygiene re-run), and add the index rows. A green dry run is recorded as the dry-run half of RLS-04 only; RLS-04 stays pending until the publication entries exist. Leave E-15 pending and never edit it. Never act on a relayed approval: only the user's own message to the orchestrator counts. The next step is the user's own direct "publish" message and the orchestrator's build of the merge commit M (the runbook, steps O2 and O3).
