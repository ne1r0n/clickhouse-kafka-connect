# The real publish run of release.yaml: Phase 49, handoff U4 (user-gated)

This document asks for one workflow dispatch that PUBLISHES (word G5). By default (U-4) the orchestrator runs it only after your direct word for this exact step; you may run it yourself instead, with your own account. No agent acts on a relayed approval, a task notification or a summary. It asks for no secret, and every environment-specific value is an angle-bracket value. Start only after the push of the merge commit (the previous handoff, `49-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md`) was reported with exactly one green `tests.yaml` run, after the orchestrator has recorded that report, and after gate G5 of the bundle.

## Status

- Nothing was dispatched by any agent. Until the previous handoff, origin `custom_build` read `9483476322e0d331e81c5b92ac266a18f0f01450` (E-14 `origin_custom_build`); after it, it reads `${M}`, the merge commit `M'` that the orchestrator built and pushed.
- No `v1.7.0_gg8` tag and no `v1.7.0_gg8` release exists, and none may exist before this run: the step `Check the release tag is absent` refuses to publish over it. The latest published release is `v1.7.0_gg7` (release id 433237218, E-01 `releases_gg7`).
- The workflow's publish step has run for the previous releases: the tag and the release of `v1.7.0_gg7` were created by the workflow's own token (author and uploader read `github-actions[bot]` for the earlier release; for this one it is read back after the run). This run is the first with the gg8 allow-list.
- The compare chain ran on a runner in the dry run (E-24), if it was green.
- Not proven by the local gate: the two last review fixes no fresh reviewer read, `integrationTest` not re-run, and every GitHub behaviour named as an assumption.

## What you decide

- This run publishes. It creates the GitHub release `v1.7.0_gg8` with the archive attached, and it creates the tag `v1.7.0_gg8` at `${M}` on GitHub's side (the workflow's last step calls the GitHub CLI with the dispatched SHA as the target). Only you can undo it, by deleting the release and the tag.
- The release body is empty by design. Pasting the section of `CHANGELOG.md` into the release afterwards is optional and yours.
- You do NOT push a tag named `v1.7.0_gg8`, now or later. The tag-absent gate would refuse to publish, and a hand-pushed tag is the one mistake this document exists to prevent.
- A dispatch from any ref other than `custom_build`, or with `dry_run` ticked, publishes nothing. The `dry_run` input must be NOT ticked here, and the ref must be `custom_build`.
- The runner's archive is close in size to the local build (13572233 bytes, E-11 `final_tip_bytes`); the compare, not a size, decides.
- Banned on every push of this phase: the force, tags, all and mirror options.

## Before you dispatch

Each check has an expected result; on any other result, do not dispatch and tell the orchestrator. If `custom_build` moved, do not dispatch: the orchestrator reads the state and asks you again.

1. The `tests.yaml` run on `${M}` is recorded as exactly one run and `success` (the report of the previous handoff, recorded by the orchestrator as E-26).
2. `git ls-remote origin refs/heads/custom_build` prints `${M}`, a tab and `refs/heads/custom_build`. If it prints `9483476322e0d331e81c5b92ac266a18f0f01450`, the merge commit was not pushed and there is nothing to publish.
3. `git ls-remote --tags origin 'v1.7.0_gg8*'` prints nothing, and `git tag -l 'v1.7.0_gg8'` prints nothing.
4. No run of `release.yaml` is in progress: `gh run list -R ne1r0n/clickhouse-kafka-connect --workflow release.yaml --limit 3` shows none queued or running.

## Dispatch (U4, word G5)

In the GitHub Actions UI open the workflow `Release` (file `release.yaml`) and press Run workflow with "Use workflow from" set to `custom_build` and the input `dry_run` NOT ticked. Or run:

```sh
gh workflow run release.yaml -R ne1r0n/clickhouse-kafka-connect --ref custom_build
gh run list -R ne1r0n/clickhouse-kafka-connect --workflow release.yaml --limit 3
```

with no `dry_run` input. The run name must read `Release custom_build (publish)`. If it reads `(dry run)`, nothing is published: the dispatch was wrong, say so to the orchestrator. If it reads `(publish)` when you did not mean to publish, cancel the run at once in the Actions UI, before its step `Publish release`, and tell the orchestrator. `pre-release.yaml` is the twin of this workflow and is never dispatched. The job limit is 30 minutes.

## Expected result: GREEN, with a release

- Every step succeeds.
- `Select the baseline tag` notices `v1.7.0_gg8 is compared with v1.7.0_gg7 under .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt`.
- `Compare the archive with the previous release` shows the notice `exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg7, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt`, and the artifact `archive-compare-report` ends with `RESULT: PASS`.
- `Check tests.yaml is green on this SHA` succeeds (one successful run on `${M}`; an error would refuse the publish).
- `Check the release tag is absent` succeeds (it notices that tag `v1.7.0_gg8` is absent).
- `Publish release` succeeds, and the draft steps (`Create draft release (dry run)` and the three after it) are skipped.
- A release `v1.7.0_gg8` exists with exactly one asset, `clickhouse-kafka-connect-v1.7.0_gg8.zip`, and the tag `v1.7.0_gg8` exists on origin at `${M}`. Whether the author and the uploader read `github-actions[bot]` is part of what the orchestrator reads back.

## What to confirm and report

Report directly to the orchestrator, in your own message (or let the orchestrator read the same fields with GET calls after running the step on your word):

- the run URL and id, its `headSha` (it must equal `${M}`), its `event` (`workflow_dispatch`) and its `headBranch` (`custom_build`);
- the conclusion and every step's conclusion;
- the compare notice, the artifact and its last line;
- the conclusion of `Publish release`;
- the release URL;
- from these read-only calls: `gh api repos/ne1r0n/clickhouse-kafka-connect/git/ref/tags/v1.7.0_gg8` (the object type and SHA) and `gh api repos/ne1r0n/clickhouse-kafka-connect/releases/tags/v1.7.0_gg8` (the asset name, size, digest and the uploader's login).

## If it fails

- A failure before `Publish release`: nothing was created. Report the step and its annotation, and do not dispatch again until the orchestrator has read the state (contingency C-2 or C-3). A defect that only the runner shows goes back to its phase as a gap (this phase edits no `.github/`).
- A failure at or after `Publish release`: a tag or a draft may exist, and the tag gate refuses a second run. Report and change nothing (contingency C-6). The orchestrator reads the state with read-only calls and proposes either deleting the release and the tag (your action) followed by one more dispatch, or a manual release. You decide.
- A run named `(dry run)` when you meant to publish: nothing was published; report it, and dispatch again from `custom_build` with `dry_run` not ticked only after the orchestrator confirms.
- A `v1.7.0_gg8` tag that already exists (for instance hand-pushed): the gate refuses to publish (contingency C-8). You delete that tag and nothing else; the orchestrator re-reads before another dispatch.
- `custom_build` moved: do not dispatch, tell the orchestrator (contingency C-1).

Rollback, if you decide it after publication: the block of the Rollback section of `docs/ROLLOUT-v1.7.0_gg8.md` (it ran unchanged on the rehearsal merge and printed `ok`):

```sh
git switch -c rollback-v1.7.0_gg8
git read-tree -u --reset pre-v1.7.0_gg8
git commit -m "Roll back v1.7.0_gg8"
git diff --quiet HEAD pre-v1.7.0_gg8 && echo ok
```

It is forward only: a new commit on `custom_build`, never a force-push, and the `v1.7.0_gg7` release asset is redeployed. A release and a tag that were created stay until you delete them.

## Clean up

None.

## Resume (for the orchestrator)

Record the run as `### E-27: U4 publish dispatch`, with the fields above as `key: value` lines and the words `user-reported` when the user reported it, or `orchestrator-run on the user's word: <quoted word>` when you ran it on that word; add the index row when the entry exists. Then read the release, the tag, the asset, its uploader and the run back with the read-only calls of the runbook (step A1) and record `### E-28: The publish run read back: release, tag, asset, uploader and steps`. Leave E-17 pending and never edit it. Write `49-RELEASE-RECORD.md` on the local tail (runbook, step A1). Never act on a relayed approval.
