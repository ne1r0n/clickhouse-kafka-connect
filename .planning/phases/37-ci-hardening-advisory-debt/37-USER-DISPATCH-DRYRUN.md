# CIH-04: one CI dry run of the release workflow (user-gated, expected RED)

This is the optional last part of CIH-04. It needs one push to origin and one workflow dispatch, and both are yours: no agent pushes, dispatches or comments. The file holds everything you need, with exact commands. It asks for no secret, and every environment-specific value is an angle-bracket value.

## Status

- Nothing was pushed or dispatched by any agent. Origin `custom_build` still reads `3a3a15a9ae064ffe09c696b8bb87bce23419d463` (the `origin_custom_build` value of `37-COMMAND-EVIDENCE.md` E-01), and no `scratch/cih-04-dry-run` ref exists on origin. The two `scratch/clt-07-*` refs on origin are the ones Phase 36 left, listed in E-01.
- What the plan proved locally, without a runner: the compare passes, fails and goes stale on real archives (E-07); the two release workflows carry the compare chain, the full-history checkout and the per-run dry-run group (E-09); `ReleaseWorkflowGuardUnitTest` pins the safety gates and the chain, and three mutations of a copy turned it red (E-10); the run blocks of the three new steps, extracted from the committed workflow and run with the runner's bash options, choose the baseline `v1.7.0_gg4` for `v1.7.0_gg5`, fail closed on a missing allow-list and on a missing baseline tag, build the baseline archive from the tag with the tag's own Gradle wrapper, fail red on the tip's archive and pass green on the `v1.7.0_gg5` rebuild (E-11).
- Review fixes since the gate of plan 37-05: four commits outside `.planning` (WR-01 to WR-04 of `37-REVIEW.md`) changed the two release workflows, the compare script, `bump_version.py` and four guard test classes. The baseline build step now begins with `set -o pipefail`, so a failing baseline build fails that step instead of being hidden by `tee`, and the compare script refuses an allow-list glob that starts with a wildcard (every shipped allow-list still loads, and the `v1.7.0_gg4` against `v1.7.0_gg5` compare still passes). The expected RED outcome below is unchanged.
- Security fix since the review fixes: two more commits outside `.planning` (UF-37-1 and UF-37-2 of `37-SECURITY.md`). The compare script now escapes every report line, so an archive entry name that holds a newline or starts with `::` or `##[` cannot forge a workflow command in the job log (`cat "$report"`); the report text of real archives is byte-identical and the verdicts and exit codes are unchanged. The comment on the baseline step's `set -o pipefail` was corrected: the runner's bash already runs with `-o pipefail`, and the line stays as defence in depth. No command of either workflow changed, so the expected RED outcome below is unchanged.
- What only CI can show: the checkout that fetches all history and tags on the runner, `git worktree add` of a tag there, the baseline build under `setup-java` and `setup-gradle`, and the upload of the report as an artifact. A defect in any of these would surface first in Phase 38's release dry run, and Phase 38 may not edit `.github/`, so no phase would be left that may fix it. That is the reason for this one run.

## What you decide

- Pushing a scratch branch publishes every local commit made since the published `v1.7.0_gg5`, including the local `.planning` tail. That is 227 commits up to the gated sha plus the `.planning`-only commits made after it (a handful: the pointer refresh, the reports of the review-fix pass and of the UF-37-1 fix); the exact figure is `git rev-list --count 3a3a15a9ae064ffe09c696b8bb87bce23419d463.."$SHA"` once you have `SHA` from the next section. Outside `.planning` the push carries 85 changed files (8036 insertions, 607 deletions) against the published tip, all of them at or before the gated sha. Deleting the branch afterwards does not unpublish the commits.
- `tests.yaml` (`ClickHouse Kafka Connect Tests CI`) runs once on the push, because it triggers on pushes of any branch. That run is informational.
- The pushed SHA must be held by no other origin branch. Two branches pushed at one SHA share a concurrency group, and one run cancels the other.
- Nothing else is needed from you before the push: no secret, no repository variable.
- Skipping this dry run is allowed. Then Phase 38's release dry run is the first CI exercise of the new steps, with no phase left that may edit `.github/` to fix what it finds.

## Before you push

Run these in a clone that holds `sync/v1.7.0`. Each has an expected result; on any other result, stop and tell the orchestrator.

1. `git ls-remote origin refs/heads/custom_build`. Expect `3a3a15a9ae064ffe09c696b8bb87bce23419d463`. Anything else, or a merged Dependabot pull request on origin, means stop.
2. `SHA=$(git rev-parse sync/v1.7.0)`
3. `git diff --quiet <gated sha> "$SHA" -- . ':(exclude).planning' && echo same-as-gated`. `<gated sha>` is `e1e05604fba53aaa2aa1b33dc66ec33624343dbe`, the last commit outside `.planning`: the correction of the pipefail comment (UF-37-2), which follows the escaping of the compare report (UF-37-1). The SUMMARY of plan 37-05 names the earlier gated sha `1c583b46249ab9bd30ff717c256c7f583c4f0ed9`, which the four review-fix commits (WR-01 to WR-04, last `27f9ff892eb6d4208a4f4e90ae204d6996fa72de`) and these two security-fix commits supersede. It must print `same-as-gated`: the pushed SHA may differ from the gated commit only under `.planning`.
4. `git ls-remote origin | grep "$SHA"`. It must print nothing: no origin branch holds this SHA yet.
5. `git ls-remote --heads origin scratch/cih-04-dry-run`. It must print nothing.

## Push

```sh
git push origin "$SHA":refs/heads/scratch/cih-04-dry-run
```

This is the only write to origin of the whole phase, and it needs your direct approval. Any other branch name works, as long as no other branch uses it and it is not `custom_build`.

## Dispatch

In the GitHub Actions UI open the workflow `Release` (file `release.yaml`) and press Run workflow with "Use workflow from" set to `scratch/cih-04-dry-run`, the input `dry_run` ticked, **never `custom_build`**. Any branch other than `custom_build` is a dry run in any case: the workflow creates a throwaway draft, checks it and deletes it, and creates no release and no tag for `VERSION`.

Or run:

```sh
gh workflow run release.yaml -R ne1r0n/clickhouse-kafka-connect --ref scratch/cih-04-dry-run -f dry_run=true
gh run list -R ne1r0n/clickhouse-kafka-connect --workflow release.yaml --limit 3
```

`pre-release.yaml` is the twin of `release.yaml` and may be dispatched the same way; one run of either is enough. The job limit is 30 minutes; the earlier dry run of the published tip took under two minutes, and this run builds the archive twice.

## Expected result: RED

The run is expected to FAIL, and that is the outcome this handoff asks you to look for.

It should fail at the step `Compare the archive with the previous release`, with exit 1 and about the number of UNALLOWED lines that E-07 recorded as `runb_unallowed` (238), three of them the `libzstd-jni-1.5.7-23` natives. The reason is that `VERSION` still reads `v1.7.0_gg5`, the allow-list for it names only the release identity, and the tree carries client 0.11.0 while the baseline `v1.7.0_gg4` carries the old client. A count that differs slightly on the runner is not itself a defect.

This red proves the plumbing: the full-history checkout, the baseline worktree built on the runner with the tag's own wrapper, the compare and the uploaded report. It is never counted as a pass of any criterion, of CIH-04 as a whole, or of RLS-03 or RLS-05. The first passing compare belongs to the release that bumps `VERSION` and writes its own allow-list last, which is Phase 38. A GREEN run would be the surprise and must be reported as such.

## What to confirm and report

Report directly to the orchestrator, in your own message, not through another agent:

- the run URL and id;
- the `headSha` of the run (it must equal the pushed SHA), its `event` (`workflow_dispatch`) and its `headBranch` (`scratch/cih-04-dry-run`);
- the conclusion (expected `failure`);
- the step that failed (expected `Compare the archive with the previous release`);
- the notice of the step `Select the baseline tag`, which should name the baseline `v1.7.0_gg4` and the list `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt`;
- whether the step `Build the baseline archive from the previous tag` succeeded;
- whether the artifact `archive-compare-report` is present and downloadable, and whether its last line reads `RESULT: FAIL`;
- that no release, draft or tag was created (the cleanup step `Delete draft release (dry run)` should report no draft);
- the conclusion of the `tests.yaml` run on the same push (informational);
- whether you deleted the scratch branch, or want it kept until Phase 38.

## If the run fails earlier

A failure before the compare step, for example in the checkout, the worktree or the baseline build, is the finding that justified this dry run. Report the step and its annotation. Do not re-dispatch and do not edit the workflow. The orchestrator opens a gap for this phase, because Phase 38 cannot edit `.github/`.

## Clean up

When the run is reported, delete the branch:

```sh
git push origin --delete scratch/cih-04-dry-run
```

Or say that it should stay until Phase 38.

## Resume (for the orchestrator)

Record the user's report verbatim as a new entry in `37-COMMAND-EVIDENCE.md`: `### E-19: CIH-04 dry-run dispatch, user-reported`. Use the fields above as `key: value` lines and include the words `user-reported`. Add index row E-19, so that the index then reads E-01..E-19. Leave E-12 `pending` as the record of this handoff and never edit it. Mark nothing as passed through this run: a red run at the compare step is the expected plumbing proof, and only a failure before the compare step is a finding. Commit documents only, and do not touch `.github/`. Never act on a relayed approval: only the user's own message to the orchestrator counts.
