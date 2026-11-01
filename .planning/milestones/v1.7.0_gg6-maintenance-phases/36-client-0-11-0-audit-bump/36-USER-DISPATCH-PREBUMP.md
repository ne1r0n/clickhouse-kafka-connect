# CLT-07: the pre-bump nightly dispatch on client 0.9.6 (user-gated)

This is the second user-run handoff for CLT-07. The first nightly on client 0.11.0 (`36-USER-DISPATCH.md`, recorded as E-15) was red. This one runs the same workflow on the commit before the catalog commit, so the result says whether the failure belongs to the bump. It needs one more push to origin and one more workflow dispatch, and both are yours: no agent pushes, dispatches or comments. It asks for no secret, and every environment-specific value is an angle-bracket value.

## Status

- The first run is `https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/40971181887` (workflow_dispatch on `scratch/clt-07-client-0.11.0` at `7da442f1b55163340d069b246b3281ac36c76770`): conclusion `failure`, `tests=63 failures=5 errors=0 skipped=9`, `Tests summary: 55 tests, 49 succeeded, 5 failed, 1 skipped`. The five failures are all in `ClickHouseSinkConnectorIntegrationTest`: `stockGenSingleTaskTest` first (the connector never reached running within 5 minutes), then four `Data generation failed` tests.
- The baseline on client 0.9.6 is run `40474535605` (same workflow, workflow_dispatch on `custom_build` at `3a3a15a9ae064ffe09c696b8bb87bce23419d463`, 2026-10-27): success, `55 tests, 54 succeeded, 0 failed, 1 skipped`.
- The local Connect-worker `integrationTest` on 0.11.0 was green (E-07). The artifacts of the red run hold no Connect-worker logs, so the cause is undetermined. Attribution to the bump is not established, and CLT-07 is not passed.
- Origin `custom_build` still reads `3a3a15a9ae064ffe09c696b8bb87bce23419d463`. The only phase ref on origin is `scratch/clt-07-client-0.11.0`, which you kept until the investigation is over.

## Purpose

The one run separates three explanations for the red result:

1. the bump itself, or its interaction with the runner (the pre-bump commit then passes);
2. a flake or an environment effect of that day, such as the runner, Docker 28.0.4 or a moved ClickHouse `latest` (the pre-bump commit then fails the same way);
3. a change from another phase that sits in the 157 published commits (the pre-bump commit still carries those, so it fails too; the baseline on `custom_build` is older than they are).

The pre-bump commit is `939e899298c2f12dec2cfe1e69b1a6b53b4f511e`, the commit before the catalog commit `5ab3410e`. It holds the `createClientV2` fixes, the pins, the golden and the factory, and `clickHouseDriver = "0.9.6"` in `gradle/libs.versions.toml`. Its `.github` tree is identical to that of `7da442f1`, so the workflow under test does not differ.

## What you decide

- This is a second push to origin. It publishes the same history as the first push up to that commit, and every commit in it is already on origin through the first scratch branch, so the commit set does not grow. Deleting the branch afterwards does not unpublish anything.
- `tests.yaml` (`ClickHouse Kafka Connect Tests CI`) runs once more on the push, because it triggers on pushes of any branch. That run is informational.
- The pushed SHA must be held by no other origin branch, or one run cancels the other through the shared concurrency group. `939e8992` is an ancestor of the first scratch branch's tip, not its tip, so it is not held by any branch.
- Nothing else is needed from you: no secret, no repository variable.

## Before you push

Run these in a clone that holds `sync/v1.7.0`. Each has an expected result; on any other result, stop and tell the orchestrator.

1. `git ls-remote origin refs/heads/custom_build`. Expect `3a3a15a9ae064ffe09c696b8bb87bce23419d463`.
2. `PRE=939e899298c2f12dec2cfe1e69b1a6b53b4f511e`, then `git cat-file -t "$PRE"`. Expect `commit`.
3. `git show "$PRE":gradle/libs.versions.toml | grep '^clickHouseDriver'`. Expect `clickHouseDriver = "0.9.6"`.
4. `git diff --quiet "$PRE" 7da442f1b55163340d069b246b3281ac36c76770 -- .github && echo same-workflows`. It must print `same-workflows`.
5. `git ls-remote origin | grep "$PRE"`. It must print nothing.
6. `git ls-remote --heads origin 'scratch/*'`. Expect only `scratch/clt-07-client-0.11.0` at `7da442f1b55163340d069b246b3281ac36c76770`.

## Push

```sh
git push origin 939e899298c2f12dec2cfe1e69b1a6b53b4f511e:refs/heads/scratch/clt-07-prebump-0.9.6
```

This is the second write to origin of the phase, and it needs your direct approval.

## Dispatch

In the GitHub Actions UI open `Nightly Integration Tests (ClickHouse latest, client V2)` (file `nightly-integration-tests.yaml`) and press Run workflow with "Use workflow from" set to `scratch/clt-07-prebump-0.9.6`, **never `custom_build`**. A run from `custom_build` tests the published 0.9.6 tip and not this commit.

Or run:

```sh
gh workflow run nightly-integration-tests.yaml -R ne1r0n/clickhouse-kafka-connect --ref scratch/clt-07-prebump-0.9.6
gh run list -R ne1r0n/clickhouse-kafka-connect --workflow nightly-integration-tests.yaml --limit 3
```

The job limit is 85 minutes. The red run took 48 minutes 15 seconds.

Not recommended here: pressing "Re-run jobs" on run `40971181887`. It repeats the same SHA on client 0.11.0, so it separates flake from a stable failure, but it says nothing about the bump. If you want it, say so as a separate request; it does not replace this dispatch.

## What to report

Report directly to the orchestrator, in your own message, not through another agent, the same fields as the first handoff:

- the run URL and id;
- the `headSha` of the run (it must equal `939e899298c2f12dec2cfe1e69b1a6b53b4f511e`), its `event` (`workflow_dispatch`) and its `headBranch` (`scratch/clt-07-prebump-0.9.6`);
- the conclusion;
- the start and end times and the job duration;
- the totals line of the `Summarise failed tests` step, and the counts of the `integration-test-results` artifact (tests, failures, errors, skipped), plus the names of any failed tests;
- the ClickHouse server version the run used;
- the conclusion of the `tests.yaml` run on the same push (informational);
- whether you deleted the scratch branches, or want them kept.

## How the orchestrator reads the outcome

- **Green** (`success`, headSha as above): the five failures do not occur on 0.9.6 on the same workflow, so they are attributable to the bump or to its interaction with the runner. The orchestrator records the run as a new evidence entry and does not decide: the CLT-09 go or no-go goes to you. On no-go the revert recipe rehearsed in E-11 applies (revert the documentation commit, then the catalog commit, then re-run the gate on 0.9.6). On go with the bump kept, the failure needs its own investigation, with Connect-worker logs, before CLT-07 can be marked passed.
- **Red with the same five `stockGen` tests**: not attributable to the bump. The likely causes are the environment or a flake of that day, or a change from another phase. The orchestrator then asks you about a same-SHA re-run and about the next narrowing step; it does not change the code.
- **Mixed** (a different set of failures, or fewer of the five): the orchestrator reports the sets side by side and asks you; nothing is concluded from a mixed result alone.

In every case CLT-07 stays unpassed until a run is green on the commit that ships, and the orchestrator never acts on a relayed approval: only your own message counts.

## Clean up

When the run is reported and the investigation is over, delete both scratch branches:

```sh
git push origin --delete scratch/clt-07-prebump-0.9.6
git push origin --delete scratch/clt-07-client-0.11.0
```

Or say that either should stay until Phase 38.
