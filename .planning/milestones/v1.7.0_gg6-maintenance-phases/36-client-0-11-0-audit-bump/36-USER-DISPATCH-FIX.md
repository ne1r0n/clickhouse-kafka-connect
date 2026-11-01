# CLT-07: the third nightly dispatch, on client 0.11.0 with the Datagen harness fix (user-gated)

This is the third user-run handoff for CLT-07. The first nightly on client 0.11.0 (`36-USER-DISPATCH.md`, E-15) was red, and the pre-bump nightly (`36-USER-DISPATCH-PREBUMP.md`) was green. The investigation that followed found the cause in the integration-test harness and not in the client, you decided to keep 0.11.0 and have the harness fixed, and that decision is recorded as E-16. This handoff runs the same nightly once more on the fixed commit. It needs one more push to origin and one more workflow dispatch, and both are yours: no agent pushes, dispatches or comments. It asks for no secret, and every environment-specific value is an angle-bracket value.

## Status

- Run `40971181887` on `7da442f1b55163340d069b246b3281ac36c76770` (client 0.11.0) was red: attempt 1 with 5 failures, attempt 2 with 2 failures, all in `ClickHouseSinkConnectorIntegrationTest`.
- Run `41110577512` on `939e899298c2f12dec2cfe1e69b1a6b53b4f511e` (client 0.9.6, the commit before the catalog commit) was green.
- The cause is in the harness. The connector that never reaches `RUNNING` is the Datagen source connector. Its tasks finish their 100 iterations in about 5 s, and the first status poll of `ConfluentPlatform.createConnectorAndWaitUntilRunning` is also at 5 s and only checks that task 0 is `RUNNING`. In about one call in five task 0 was already `FAILED` with the message `done`, so the wait timed out after 5 minutes. `generateData` then threw before it deleted the connector, the leftover connector's finished tasks inflated the completion count (which spans all connectors), and every later `generateData` failed with `Data generation failed`.
- The fix is commit `0bb894a4b61a48f3f31cac38a5ee533f33eb11ad` (`test(36): wait for datagen completion instead of the RUNNING status and always delete the datagen connector`). It changes `src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java` and adds one row to `docs/FORK-DELTA.md`. `src/main` is unchanged. The sink path still goes through `createConnectorAndWaitUntilRunning`.
- Locally, the seven `stockGen*` tests of `ClickHouseSinkConnectorIntegrationTest` passed 7 of 7 on ClickHouse `latest` with client V2 on 0.11.0 (`BUILD SUCCESSFUL in 10m 18s`); the unmodified harness failed the same tests the same way in the same environment (E-16).
- CLT-07 is still not passed. Origin `custom_build` still reads `3a3a15a9ae064ffe09c696b8bb87bce23419d463`. The origin branches `scratch/clt-07-client-0.11.0` (tip `7da442f1b55163340d069b246b3281ac36c76770`) and `scratch/clt-07-prebump-0.9.6` (tip `939e899298c2f12dec2cfe1e69b1a6b53b4f511e`) exist and were kept by you.

## Purpose

One nightly on the fixed commit answers the open question of CLT-07: does the whole `integrationTest` task pass in CI on `latest` with client V2 on client 0.11.0. A green run closes CLT-07. A red run is read against the interpretation section below.

## What you decide

- This is a push to the existing scratch branch `scratch/clt-07-client-0.11.0`. It is a fast-forward of its tip `7da442f1`, so no force is involved. It publishes the commits after `7da442f1` only (the harness fix and the later documentation commits); the figure is `git rev-list --count 7da442f1b55163340d069b246b3281ac36c76770.."$SHA"` once you have `SHA` from the next section. Deleting the branch afterwards does not unpublish anything.
- `tests.yaml` (`ClickHouse Kafka Connect Tests CI`) runs once on the push, because it triggers on pushes of any branch. That run is informational.
- No other origin branch may hold the pushed SHA, or one run cancels the other through the shared concurrency group. The SHA is new, so none does.
- Nothing else is needed from you: no secret, no repository variable.

## Before you push

Run these in a clone that holds `sync/v1.7.0`. Each has an expected result; on any other result, stop and tell the orchestrator.

1. `git ls-remote origin refs/heads/custom_build`. Expect `3a3a15a9ae064ffe09c696b8bb87bce23419d463`. Anything else, or a merged Dependabot pull request on origin, means stop.
2. `SHA=$(git rev-parse sync/v1.7.0)`
3. `git diff --quiet 0bb894a4b61a48f3f31cac38a5ee533f33eb11ad "$SHA" -- . ':(exclude).planning' && echo same-as-fix`. It must print `same-as-fix`: the pushed SHA may differ from the harness-fix commit only under `.planning`.
4. `git merge-base --is-ancestor 7da442f1b55163340d069b246b3281ac36c76770 "$SHA" && echo fast-forward`. It must print `fast-forward`.
5. `git ls-remote origin | grep "$SHA"`. It must print nothing: no origin branch holds this SHA yet.
6. `git ls-remote --heads origin 'scratch/*'`. Expect `scratch/clt-07-client-0.11.0` at `7da442f1b55163340d069b246b3281ac36c76770` and `scratch/clt-07-prebump-0.9.6` at `939e899298c2f12dec2cfe1e69b1a6b53b4f511e`.

## Push

```sh
git push origin "$SHA":refs/heads/scratch/clt-07-client-0.11.0
```

This is a write to origin and needs your direct approval. It triggers `tests.yaml` once.

## Dispatch

In the GitHub Actions UI open `Nightly Integration Tests (ClickHouse latest, client V2)` (file `nightly-integration-tests.yaml`) and press Run workflow with "Use workflow from" set to `scratch/clt-07-client-0.11.0`, **never `custom_build`**. A run from `custom_build` tests the published 0.9.6 tip and proves nothing about this commit.

Or run:

```sh
gh workflow run nightly-integration-tests.yaml -R ne1r0n/clickhouse-kafka-connect --ref scratch/clt-07-client-0.11.0
gh run list -R ne1r0n/clickhouse-kafka-connect --workflow nightly-integration-tests.yaml --limit 3
```

The job limit is 85 minutes (75 for the test step). The red nightly took 48 minutes 15 seconds; the local run of the whole task took 31 minutes 30 seconds.

## What to report

Report directly to the orchestrator, in your own message, not through another agent, the same fields as the earlier handoffs:

- the run URL and id;
- the `headSha` of the run (it must equal the pushed SHA), its `event` (`workflow_dispatch`) and its `headBranch` (`scratch/clt-07-client-0.11.0`);
- the conclusion, and the run attempt number;
- the start and end times and the job duration;
- the totals line of the `Summarise failed tests` step, and the counts of the `integration-test-results` artifact (tests, failures, errors, skipped), plus the names of any failed tests. The earlier green runs had `55 tests, 54 succeeded, 0 failed, 1 skipped`;
- the ClickHouse server version the run used;
- the conclusion of the `tests.yaml` run on the same push (informational);
- whether you deleted the scratch branches, or want them kept.

## How the orchestrator reads the outcome

- **Green** (`success`, headSha as above): CLT-07 passes on client 0.11.0 and the orchestrator records it as a new evidence entry, E-17. The phase's open item is then closed.
- **Red with the Datagen pattern again** (a `stockGen*` failure, `Data generation failed`, or a connector that does not finish): the harness is still racy. The orchestrator reports it to you and asks for the next narrowing step. The optional Connect-worker diagnostics patch exists at `/tmp/gsd36/invest/connect-diagnostics.patch`; it is not applied, and you may ask for it.
- **Red elsewhere** (any other test or class): the orchestrator reports the failed names and the artifact counts to you and asks. Nothing is concluded from the result alone.

In every case CLT-07 stays unpassed until a run is green on the commit that ships, and the orchestrator never acts on a relayed approval: only your own message counts.

## Clean up

When the run is reported and the investigation is over, delete both scratch branches:

```sh
git push origin --delete scratch/clt-07-client-0.11.0
git push origin --delete scratch/clt-07-prebump-0.9.6
```

Or say that either should stay until Phase 38.
