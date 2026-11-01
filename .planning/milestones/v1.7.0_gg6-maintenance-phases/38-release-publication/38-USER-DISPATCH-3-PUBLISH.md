# Phase 38, handoff U4: the real publish run of release.yaml (user-gated)

This document asks for one workflow dispatch that PUBLISHES, and it is yours. No agent pushes, tags, dispatches or comments, and no agent runs a write call on GitHub. It asks for no secret, every environment-specific value is an angle-bracket value, and you run every step with your own account. Start only after the push of the merge commit (third handoff) was reported with exactly one green `tests.yaml` run, and after the orchestrator has recorded that report.

## Status

- Nothing was dispatched by any agent. Until the previous handoff, origin `custom_build` read `3a3a15a9ae064ffe09c696b8bb87bce23419d463` (E-14 `origin_custom_build`); after it, it reads `<M>`, the merge commit the orchestrator built and you pushed.
- No `v1.7.0_gg6` tag and no `v1.7.0_gg6` release exists, and none may exist before this run: the step `Check the release tag is absent` refuses to publish over it. The only release on origin is `v1.7.0_gg5` (release id 426690002, E-01 `releases_on_origin`).
- Earlier dry runs of `release.yaml` proved that the workflow's token can create and delete a draft release. Nothing has ever run its `Publish release` step to the end (the one earlier publish-mode run was cancelled before that step), and nothing has shown the workflow's token creating a tag. This run is that first exercise; the earlier caveat stays open until this run succeeds and is read back.
- The compare chain was exercised on a runner by the dry run (E-21), if it was green.
- Not proven by the local gate: the unreviewed last two review fixes (IN-14, IN-15), the deferred `docs/ROLLOUT-v1.7.0_gg1.md` half of WR-08, the missing pin of the `custom_` forwarding (IN-04), the JDK 21 record of the two full suites, and the GitHub behaviours below (A3 above all) until this run shows them.

## What you decide

- This run publishes. It creates the GitHub release `v1.7.0_gg6` with the archive attached, and it creates the tag `v1.7.0_gg6` at `<M>` on GitHub's side (`gh release create ... --target "$GITHUB_SHA"` inside the workflow). Only you can undo it, by deleting the release and the tag.
- The release body is empty by design. Pasting the section of `CHANGELOG.md` into the release afterwards is optional and yours.
- You do NOT push a tag named `v1.7.0_gg6`, now or later. The tag-absent gate would refuse to publish, and a hand-pushed tag is the one mistake this document exists to prevent.
- A dispatch from any branch other than `custom_build`, or with `dry_run` ticked, publishes nothing. The `dry_run` input must be NOT ticked here, and the ref must be `custom_build`.
- Assumption A3 (unverified): `gh release create --target <SHA>`, run with the workflow's own token, creates a lightweight tag and a release authored by `github-actions[bot]`. If it does not, the run fails at the last step with the tag absent, and you decide what to do (see "If it fails").
- Assumption A7 (unverified): the runner's archive is close in size to the local build (13558634 bytes); the compare, not a size, decides.
- Banned on every push of this phase: `--force`, `--tags`, `--all` and `--mirror`.
- `NIGHTLY_ENABLED` is not set; this run does not change that.

## Before you dispatch

Each check has an expected result; on any other result, do not dispatch and tell the orchestrator. If `custom_build` moved, do not dispatch: the orchestrator reads the state and asks you again.

1. The `tests.yaml` run on `<M>` is recorded as exactly one run and `success` (your report of the previous handoff, recorded by the orchestrator as E-23).
2. `git ls-remote origin refs/heads/custom_build` prints `<M>` and a tab and `refs/heads/custom_build`. If it prints `3a3a15a9ae064ffe09c696b8bb87bce23419d463`, the merge commit was not pushed and there is nothing to publish.
3. `git ls-remote --tags origin 'v1.7.0_gg6*'` prints nothing, and `git tag -l 'v1.7.0_gg6'` prints nothing.
4. No run of `release.yaml` is in progress: `gh run list -R ne1r0n/clickhouse-kafka-connect --workflow release.yaml --limit 3` shows none queued or running.

## Dispatch (U4)

In the GitHub Actions UI open the workflow `Release` (file `release.yaml`) and press Run workflow with "Use workflow from" set to `custom_build` and the input `dry_run` NOT ticked. Or run:

```sh
gh workflow run release.yaml -R ne1r0n/clickhouse-kafka-connect --ref custom_build
gh run list -R ne1r0n/clickhouse-kafka-connect --workflow release.yaml --limit 3
```

with no `dry_run` input. The run name must read `Release custom_build (publish)`. If it reads `(dry run)`, nothing is published: the dispatch was wrong, say so to the orchestrator. If it reads `(publish)` when you did not mean to publish, cancel the run at once in the Actions UI, before its step `Publish release`, and tell the orchestrator. `pre-release.yaml` is the twin of this workflow and is never dispatched. The job limit is 30 minutes.

## Expected result: GREEN, with a release

- Every step succeeds.
- `Compare the archive with the previous release` shows the notice `exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg5, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt`, and the artifact `archive-compare-report` ends with `RESULT: PASS`.
- `Check tests.yaml is green on this SHA` succeeds (one successful run on `<M>`, an error would refuse the publish).
- `Check the release tag is absent` succeeds (it notices that tag `v1.7.0_gg6` is absent).
- `Publish release` succeeds, and the draft steps (`Create draft release (dry run)` and the three after it) are skipped.
- A release `v1.7.0_gg6` exists with exactly one asset, `clickhouse-kafka-connect-v1.7.0_gg6.zip`, and the tag `v1.7.0_gg6` exists on origin at `<M>`. Whether the author and the uploader read `github-actions[bot]` is part of what the orchestrator reads back (A3).

## What to confirm and report

Report directly to the orchestrator, in your own message:

- the run URL and id, its `headSha` (it must equal `<M>`), its `event` (`workflow_dispatch`) and its `headBranch` (`custom_build`);
- the conclusion and every step's conclusion;
- the compare notice, the artifact and its last line;
- the conclusion of `Publish release`;
- the release URL;
- from these read-only calls, which you may run: `gh api repos/ne1r0n/clickhouse-kafka-connect/git/ref/tags/v1.7.0_gg6` (the object type and SHA) and `gh api repos/ne1r0n/clickhouse-kafka-connect/releases/tags/v1.7.0_gg6` (the asset name, size, digest and the uploader's login).

## If it fails

- A failure before `Publish release`: nothing was created. Report the step and its annotation, and do not dispatch again until the orchestrator has read the state. A defect that only the runner shows goes back to Phase 37 as a gap (Phase 38 may not edit `.github/`).
- A failure at or after `Publish release`: a tag or a draft may exist, and the tag gate refuses a second run. Report and change nothing. The orchestrator reads the state with read-only calls and proposes either deleting the release and the tag (your action) followed by one more dispatch, or a manual release, in which case the earlier caveat stays open. You decide.
- A run named `(dry run)` when you meant to publish: nothing was published; report it, and dispatch again from `custom_build` with `dry_run` not ticked only after the orchestrator confirms.
- A `v1.7.0_gg6` tag that already exists (for instance hand-pushed): the gate refuses to publish. You delete that tag and nothing else; the orchestrator re-reads before another dispatch.
- `custom_build` moved: do not dispatch, tell the orchestrator.

Rollback, if you decide it after publication: the block of the Rollback section of `docs/ROLLOUT-v1.7.0_gg6.md` (it ran unchanged on the rehearsal merge and printed `ok`):

```sh
git switch -c rollback-v1.7.0_gg6
git read-tree -u --reset pre-v1.7.0_gg6
git commit -m "Roll back v1.7.0_gg6"
git diff --quiet HEAD pre-v1.7.0_gg6 && echo ok
```

It is forward only: a new commit on `custom_build`, never a force-push, and the `v1.7.0_gg5` archive is redeployed. A release and a tag that were created stay until you delete them. After the block, `git switch` and `git revert` need git's attributes switched off (`git --attr-source="$(git hash-object -t tree /dev/null)" revert <rollback-commit>`), because of the line endings of `gradlew.bat`.

## Clean up

None.

## Resume (for the orchestrator)

Record the user's report as `### E-24: U4 publish dispatch, user-reported`, with the fields above as `key: value` lines and the words `user-reported`, and add the index row. Then read the release, the tag, the asset, its uploader and the run back with the read-only calls of the runbook (step O5) and record `### E-25: The publish run read back: release, tag, asset, uploader and steps (ADV-03)`. Only a successful E-24 together with E-25 closes the earlier caveat that no run exercised the publish path; a failed or cancelled run leaves it open. Leave E-17 pending and never edit it. Write the release record on the local tail (the runbook, step O5). Never act on a relayed approval.
