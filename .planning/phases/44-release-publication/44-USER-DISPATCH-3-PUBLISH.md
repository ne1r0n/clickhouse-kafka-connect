# The real publish run of release.yaml: Phase 44, handoff U4 (user-gated)

This document asks for one workflow dispatch that PUBLISHES, and it is yours, always, whatever your answer to question (1) of `44-APPROVAL-BUNDLE.md` was. No agent pushes, tags, dispatches or comments. It asks for no secret, every environment-specific value is an angle-bracket value, and you run every step with your own account. Start only after the push of the merge commit (third handoff, `44-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md`) was reported with exactly one green `tests.yaml` run, after the orchestrator has recorded that report, and after gate G5 of the bundle.

## Status

- Nothing was dispatched by any agent. Until the previous handoff, origin `custom_build` read `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` (E-14 `origin_custom_build`); after it, it reads `<M>`, the merge commit the orchestrator built and you pushed.
- No `v1.7.0_gg7` tag and no `v1.7.0_gg7` release exists, and none may exist before this run: the step `Check the release tag is absent` refuses to publish over it. The releases on origin are `v1.7.0_gg6` (release id 430511927) and `v1.7.0_gg5` (426690002) (E-01 `releases_on_origin`).
- The workflow's publish step has run once: the publish run of `v1.7.0_gg6` completed its `Publish release` step and created that tag and release with the workflow's own token (the release author and the asset uploader read `github-actions[bot]`; `docs/FORK-DELTA.md`, open item 22, closed). This run is the second exercise of that step, with the gg7 allow-list and the `setup-gradle` 6.4.1 pin.
- The compare chain and the pin ran on a runner in the dry run (E-23), if it was green.
- Not proven by the local gate: the last review fixes no fresh reviewer read, the deferred finding IN-03, the F-2 claim carried as `derived (mechanism, not measured)`, and the integration suite not re-run.

## What you decide

- This run publishes. It creates the GitHub release `v1.7.0_gg7` with the archive attached, and it creates the tag `v1.7.0_gg7` at `<M>` on GitHub's side (`gh release create ... --target "$GITHUB_SHA"` inside the workflow). Only you can undo it, by deleting the release and the tag.
- The release body is empty by design. Pasting the section of `CHANGELOG.md` into the release afterwards is optional and yours.
- You do NOT push a tag named `v1.7.0_gg7`, now or later. The tag-absent gate would refuse to publish, and a hand-pushed tag is the one mistake this document exists to prevent.
- A dispatch from any branch other than `custom_build`, or with `dry_run` ticked, publishes nothing. The `dry_run` input must be NOT ticked here, and the ref must be `custom_build`.
- The runner's archive is close in size to the local build (13564145 bytes, E-11 `final_tip_bytes`); the compare, not a size, decides.
- Banned on every push of this phase: the force, tags, all and mirror options.

## Before you dispatch

Each check has an expected result; on any other result, do not dispatch and tell the orchestrator. If `custom_build` moved, do not dispatch: the orchestrator reads the state and asks you again.

1. The `tests.yaml` run on `<M>` is recorded as exactly one run and `success` (your report of the previous handoff, recorded by the orchestrator).
2. `git ls-remote origin refs/heads/custom_build` prints `<M>` and a tab and `refs/heads/custom_build`. If it prints `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`, the merge commit was not pushed and there is nothing to publish.
3. `git ls-remote --tags origin 'v1.7.0_gg7*'` prints nothing, and `git tag -l 'v1.7.0_gg7'` prints nothing.
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
- `Compare the archive with the previous release` shows the notice `exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg6, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt`, and the artifact `archive-compare-report` ends with `RESULT: PASS`.
- `Check tests.yaml is green on this SHA` succeeds (one successful run on `<M>`, an error would refuse the publish).
- `Check the release tag is absent` succeeds (it notices that tag `v1.7.0_gg7` is absent).
- `Publish release` succeeds, and the draft steps (`Create draft release (dry run)` and the three after it) are skipped.
- A release `v1.7.0_gg7` exists with exactly one asset, `clickhouse-kafka-connect-v1.7.0_gg7.zip`, and the tag `v1.7.0_gg7` exists on origin at `<M>`. Whether the author and the uploader read `github-actions[bot]` is part of what the orchestrator reads back.

## What to confirm and report

Report directly to the orchestrator, in your own message:

- the run URL and id, its `headSha` (it must equal `<M>`), its `event` (`workflow_dispatch`) and its `headBranch` (`custom_build`);
- the conclusion and every step's conclusion;
- the compare notice, the artifact and its last line;
- the conclusion of `Publish release`;
- the release URL;
- from these read-only calls, which you may run: `gh api repos/ne1r0n/clickhouse-kafka-connect/git/ref/tags/v1.7.0_gg7` (the object type and SHA) and `gh api repos/ne1r0n/clickhouse-kafka-connect/releases/tags/v1.7.0_gg7` (the asset name, size, digest and the uploader's login).

## If it fails

- A failure before `Publish release`: nothing was created. Report the step and its annotation, and do not dispatch again until the orchestrator has read the state (contingency C-2 or C-3). A defect that only the runner shows goes back to its phase as a gap (this phase edits no `.github/`).
- A failure at or after `Publish release`: a tag or a draft may exist, and the tag gate refuses a second run. Report and change nothing (contingency C-6). The orchestrator reads the state with read-only calls and proposes either deleting the release and the tag (your action) followed by one more dispatch, or a manual release. You decide.
- A run named `(dry run)` when you meant to publish: nothing was published; report it, and dispatch again from `custom_build` with `dry_run` not ticked only after the orchestrator confirms.
- A `v1.7.0_gg7` tag that already exists (for instance hand-pushed): the gate refuses to publish (contingency C-8). You delete that tag and nothing else; the orchestrator re-reads before another dispatch.
- `custom_build` moved: do not dispatch, tell the orchestrator (contingency C-1).

Rollback, if you decide it after publication: the block of the Rollback section of `docs/ROLLOUT-v1.7.0_gg7.md` (it ran unchanged on the rehearsal merge and printed `ok`):

```sh
git switch -c rollback-v1.7.0_gg7
git read-tree -u --reset pre-v1.7.0_gg7
git commit -m "Roll back v1.7.0_gg7"
git diff --quiet HEAD pre-v1.7.0_gg7 && echo ok
```

It is forward only: a new commit on `custom_build`, never a force-push, and the `v1.7.0_gg6` archive is redeployed. A release and a tag that were created stay until you delete them.

## Clean up

None.

## Resume (for the orchestrator)

Record the user's report as `### E-26: U4 publish dispatch, user-reported`, with the fields above as `key: value` lines and the words `user-reported`, and add the index row. Then read the release, the tag, the asset, its uploader and the run back with the read-only calls of the runbook (step O5) and record `### E-27: The publish run read back: release, tag, asset, uploader and steps`. Leave E-17 pending and never edit it. Write the release record `44-RELEASE-RECORD.md` on the local tail (the runbook, step O5). Never act on a relayed approval.
