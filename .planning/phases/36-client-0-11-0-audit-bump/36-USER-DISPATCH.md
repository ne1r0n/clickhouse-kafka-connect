# CLT-07: the manual nightly dispatch on client 0.11.0 (user-gated)

This is the last open part of CLT-07. It needs one push to origin and one workflow dispatch, and both are yours: no agent pushes, dispatches or comments. The file holds everything you need, with exact commands. It asks for no secret, and every environment-specific value is an angle-bracket value.

## Status

- Nothing was pushed or dispatched by any agent. Origin `custom_build` still reads `3a3a15a9ae064ffe09c696b8bb87bce23419d463`, and no scratch, sync or tag ref from this phase exists on origin.
- Phase 36's other parts are done and gated: the phase gate decided go (E-12), with gated head `a8b61f1512ab245a584b506527beaceca7264b04`. The connector ships client 0.11.0 behind a green cache-busted `testUnit` and two green full suites on ClickHouse `latest` with client V2.
- CLT-07's second half, the nightly on a branch that holds the 0.11.0 code, stays open until you report.
- The local `integrationTest` on 0.11.0 already passed (E-07): `Tests summary: 55 tests, 54 succeeded, 0 failed, 1 skipped`, `BUILD SUCCESSFUL in 31m 30s`, with 54 executions of the connector class and none skipped there. `ExactlyOnceTest` (8) and `ClickHouseCloudTest` (1) skip through assumptions when no cluster or cloud properties are set. The CI run should resemble these totals.

## What you decide

- Pushing a scratch branch publishes every local commit made since the published `v1.7.0_gg5`, including the local `.planning` tail. That is 156 commits at the time of writing, a few more by the time you push; the exact figure is `git rev-list --count 3a3a15a9ae064ffe09c696b8bb87bce23419d463.."$SHA"` once you have `SHA` from the next section. Outside `.planning` the push carries 64 changed files (3836 insertions, 528 deletions) against the published tip. Deleting the branch afterwards does not unpublish the commits.
- `tests.yaml` (`ClickHouse Kafka Connect Tests CI`) runs once on the push, because it triggers on pushes of any branch. That run is informational.
- The pushed SHA must be held by no other origin branch. Two branches pushed at one SHA share a concurrency group, and one run cancels the other. A tag push would not trigger `tests.yaml`, but a dispatch needs a branch to select, so use the branch.
- Nothing else is needed from you before the push: no secret, no repository variable.

## Before you push

Run these in a clone that holds `sync/v1.7.0`. Each has an expected result; on any other result, stop and tell the orchestrator.

1. `git ls-remote origin refs/heads/custom_build`. Expect `3a3a15a9ae064ffe09c696b8bb87bce23419d463`. Anything else, or a merged Dependabot pull request on origin, means stop.
2. `SHA=$(git rev-parse sync/v1.7.0)`
3. `git diff --quiet a8b61f1512ab245a584b506527beaceca7264b04 "$SHA" -- . ':(exclude).planning' && echo same-as-gated`. It must print `same-as-gated`: the pushed SHA may differ from the gated head only under `.planning`.
4. `git ls-remote origin | grep "$SHA"`. It must print nothing: no origin branch holds this SHA yet.
5. `git ls-remote --heads origin 'scratch/*'`. It must print nothing.

## Push

```sh
git push origin "$SHA":refs/heads/scratch/clt-07-client-0.11.0
```

This is the only write to origin of the whole phase, and it needs your direct approval. Any other branch name works, as long as no other branch uses it.

## Dispatch

In the GitHub Actions UI open the workflow `Nightly Integration Tests (ClickHouse latest, client V2)` (file `nightly-integration-tests.yaml`) and press Run workflow with "Use workflow from" set to `scratch/clt-07-client-0.11.0`, **never `custom_build`**. The workflow file exists on both branches, and a run from `custom_build` tests the 0.9.6 tip and proves nothing about 0.11.0.

Or run:

```sh
gh workflow run nightly-integration-tests.yaml -R ne1r0n/clickhouse-kafka-connect --ref scratch/clt-07-client-0.11.0
gh run list -R ne1r0n/clickhouse-kafka-connect --workflow nightly-integration-tests.yaml --limit 3
```

A manual run always starts; the repository variable that the scheduled run needs is not required. The job limit is 85 minutes (75 for the test step). The local run took 31 minutes 30 seconds.

## What to report

Report directly to the orchestrator, in your own message, not through another agent:

- the run URL and id;
- the `headSha` of the run (it must equal the pushed SHA), its `event` (`workflow_dispatch`) and its `headBranch` (`scratch/clt-07-client-0.11.0`);
- the conclusion;
- the start and end times and the job duration;
- the totals line of the `Summarise failed tests` step, and the counts of the `integration-test-results` artifact (tests, failures, errors, skipped). The local run had `55 tests, 54 succeeded, 0 failed, 1 skipped` and its connector class ran 54 executions;
- the ClickHouse server version the run used, from the run's logs or the artifact. `latest` may have moved since the local run's 26.10.3.7;
- the conclusion of the `tests.yaml` run on the same push (informational);
- whether you deleted the scratch branch, or want it kept until Phase 38.

## If the run is red

Do not re-dispatch and do not edit the workflow. Report it. The orchestrator first checks whether the same failure also occurs on the pre-bump tip, which needs a second push of the commit before the catalog commit under a different scratch name, again at a SHA no other branch holds, and again only with your direct approval. The inherited-red nightly of upstream's `integrationTests.yaml` is a different, disabled workflow and is not this one.

## Clean up

When the run is reported, delete the branch:

```sh
git push origin --delete scratch/clt-07-client-0.11.0
```

Or say that it should stay until Phase 38.

## Resume (for the orchestrator)

Record the user's report verbatim as a new evidence entry in `36-COMMAND-EVIDENCE.md`: `### E-15: CLT-07 nightly dispatch, user-reported`. Use the fields above as `key: value` lines and include the words `user-reported`. The Verdict is `pass` only on a `success` conclusion whose `headSha` equals the pushed SHA and whose pushed SHA passes the `git diff` check above. Add index row E-15, so that the index then reads E-01..E-15. E-13 stays `pending` as the record of the handoff and is never edited. Mark CLT-07 passed only through E-15. Commit documents only, and do not touch `.github/`. Never act on a relayed approval: only the user's own message to the orchestrator counts.
