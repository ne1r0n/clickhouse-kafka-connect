# Rollout checklist: `v1.7.0_gg6` to `v1.7.0_gg7`

**The connector source changes in this release.** `git diff --shortstat v1.7.0_gg6 HEAD -- src/main` on the release tip reports 8 files changed, 194 insertions and 22 deletions. The five changes at a glance:

1. Every helper client the connector opens is now closed when a task stops (section A).
2. On client V2, an insert that gets no answer from the server now ends with a retriable error after `clickhouseClientInsertTimeoutMs`; this can produce a duplicate, and it does not reach client V1 (section B).
3. The in-memory map of inserted offsets reads the consumer's topic, which changes what `reportInsertedOffsets=true` reports under a topic-renaming SMT, and its limits under other topic-changing SMTs are stated (sections C and D).
4. A `RetriableException` anywhere in the cause chain of a failure is now retriable, and the connector logs one WARN at task start for a code in `additionalRetriableErrorCodes` that it does not treat as transient (sections E and F).
5. `gson` moves to `2.15.0`, and the release identity, which the JMX name and the client name carry, reads `v1.7.0_gg7` (section G, which also holds the rollback).

The fork's own changes are listed in [`../CHANGELOG.md`](../CHANGELOG.md), and [`FORK-DELTA.md`](FORK-DELTA.md) lists everything the fork patches on top of upstream. If you are coming from a release before `v1.7.0_gg6`, work through [`ROLLOUT-v1.7.0_gg6.md`](ROLLOUT-v1.7.0_gg6.md) first and return here afterwards.

This note names no host, connector, cluster or production system. Every environment-specific value is an angle-bracket value, such as `<connector-name>`, `<topic>`, `<group>` or `<dir>`; replace each one with your own value.

**How each claim is labelled.** Every effect of this release has one row in a table below, with a label for how the claim is known. There are four labels:

- `measured`: a test or a command ran, and the row names it.
- `read from the code`: the row names a file and a site; nothing ran for this claim.
- `derived`: the claim follows from other rows or from a stated mechanism; nothing ran for this claim itself.
- `derived (mechanism, not measured)`: the mechanism is stated and the effect was not observed.

A row that makes claims of different strength says which label each claim has, in the form `claim: label; claim: label`.

**Which connectors a row concerns.** The column `Applies when` limits each row. A connector runs client V2 only when its configuration sets `client_version` to a value other than `V1`; a connector that does not set `client_version` runs client V1, the default (read from the code: `ClickHouseSinkConfig` reads `client_version` with the default `V1`, and `ClickHouseWriter` selects client V2 for any other value). Every statement that holds only on client V2 says `client V2`. The values are:

- `client V2`, `client V1`, `client V1 and client V2` and `either client`: the row holds on that client, or on both. `client V2, async=false` and the like add a setting the row needs; `client V2, exactlyOnce=false` and `client V2, exactlyOnce=true` add the state mode.
- `exactlyOnce=true`: the row holds with that setting. A value can be followed by a parenthesis that says which claim of the row it covers.
- `reportInsertedOffsets=true, bufferCount 0, injective topic-renaming SMT`, `many-to-one rename or partition-changing SMT` and `any connector that sets additionalRetriableErrorCodes`: the row concerns exactly that configuration. `bufferCount 0` is the unbuffered delivery path, the default.
- `this release`: the row describes the release as a whole. `CI only`: the row concerns the build and test workflows, not the archive. `as named in the row`: the row names its cases itself.

## Before you upgrade

Work through these steps before you roll the new archive out. Each names the section it comes from.

1. On a connector that runs client V2 with `async=false`, compare the longest insert time you see against `clickhouseClientInsertTimeoutMs` (default four minutes), and decide whether to raise it or to set `socket_timeout` (section B). With `exactlyOnce=false`, expect a duplicate after an insert that timed out and that the server committed anyway.
2. A connector that runs client V1, which is every connector without `client_version`, has no connector-side insert bound in this release (sections A and B).
3. A connector with `reportInsertedOffsets=true` under a topic-changing SMT: check the limits before relying on the reported offsets (sections C and D).
4. A connector with `errors.tolerance=all`: a failure that carries a `RetriableException` no longer reaches the dead letter queue and is redelivered with no retry budget (section E).
5. A connector that sets `additionalRetriableErrorCodes`: expect one WARN at task start for any code outside the vetted set (section F).
6. Update dashboard, alert and exporter matchers that spell out `version=v1.7.0_gg6` or `clickhouse-kafka-connect/v1.7.0_gg6` (section G).

## G. Dependencies, release identity and rollback

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| D1 | The shipped archive carries `gson` `2.15.0` where the `v1.7.0_gg6` archive carries `2.14.0`. The archive compare lists 120 differing entries: under `com/google/gson/` 101 changed and 1 present only in the older archive (`com/google/gson/internal/LinkedTreeMap$1.class`), 3 more gson entries under `META-INF`, 14 connector classes and the manifest. Its version table lists 18 libraries and differs for `gson` only (`com.google.code.gson:gson 2.14.0 -> 2.15.0`). | measured | this release | `.github/scripts/compare_confluent_archives.py` run on a local rebuild of tag `v1.7.0_gg6` and a local rebuild of the release tip, with a comment-only allow-list so that every difference is listed; the version is the `gson` entry of `gradle/libs.versions.toml`. |
| D2 | `gradle/actions/setup-gradle` moves to `6.4.1` in the four fork workflows that use it. This concerns CI only and changes nothing in the archive; no vulnerability is claimed closed by it. | read from the code | CI only | The `uses:` lines of `nightly-integration-tests.yaml`, `pre-release.yaml`, `release.yaml` and `tests.yaml` in `.github/workflows`, each pinned to a full commit with the version in a comment. |
| D3 | The release identity, which the JMX `version=` segment and the client name carry, reads `v1.7.0_gg7`. Update matchers that spell out the old value. | identity: read from the code; the identity classes differ in the archive compare: measured | this release | `Version.ARTIFACT_VERSION` is generated from `VERSION` and is used by `ClickHouseHelperClient` (client name) and `SinkTaskStatistics` (MBean name). The archive compare lists `Version` and `SinkTaskStatistics` as changed, and `SinkTaskStatistics` has no source change since `v1.7.0_gg6`, so its difference is the inlined identity. |
| D4 | The archive holds 7933 inner entries for `v1.7.0_gg6` and 7932 for the release tip. Local rebuilds are 13558633 and 13564144 bytes. These are sizes of local rebuilds, not of a release asset. | measured | this release | `./gradlew createConfluentArchive` run on an export of tag `v1.7.0_gg6` and on an export of the release tip; the `verifyConfluentArchive:` line of each run states the inner entry count. |

### Release identity

The MBean name embeds the release version, and so does the client name that the connector sends on every request. Both read `v1.7.0_gg7` where they read `v1.7.0_gg6`:

- MBean: `com.clickhouse:type=ClickHouseKafkaConnector,connector=<connector-name>,name=SinkTask<id>,version=v1.7.0_gg7`, plus `,topic=<topic>` for per-topic beans.
- Client name: `clickhouse-kafka-connect/v1.7.0_gg7`, the prefix of the HTTP user agent.

What to do: change dashboard, alert and exporter matchers that spell out `version=v1.7.0_gg6` or `clickhouse-kafka-connect/v1.7.0_gg6`; matchers that do not spell out the version are unaffected.

### Rollback

The release workflow creates the release and its tag at the merged commit and attaches the archive. Nothing in the data format changes, so a rollback is a code rollback. Tag `pre-v1.7.0_gg7` names commit `72e8ccff`, the published tip of `custom_build` before this release, which is also tag `v1.7.0_gg6`. The tag is published together with this release.

1. Start from the published tip with nothing uncommitted. Run `git fetch --tags origin`, `git checkout custom_build` and `git merge --ff-only origin/custom_build`, and check that `git status --porcelain` prints nothing; on the dry run below, the merge of the release tip printed nothing (measured). Set whatever it lists aside with `git stash -u`, keep it on its own branch, or work in a fresh clone, so that none of it becomes part of the rollback; do not commit it on `custom_build`, because step 2 branches from there and step 4 publishes that branch: the `git read-tree` of step 2 discards uncommitted and staged changes to tracked files without asking, and it overwrites untracked files at paths that the tag contains.
2. Restore the tree of the tag in one commit on a new branch, so that your local `custom_build` stays as published. This commit is `<rollback-commit>`:

```sh
git switch -c rollback-v1.7.0_gg7
git read-tree -u --reset pre-v1.7.0_gg7
git commit -m "Roll back v1.7.0_gg7"
git diff --quiet HEAD pre-v1.7.0_gg7 && echo ok
```

```text
ok
```

   After this step `git status --porcelain` printed nothing on the dry run (measured, git 2.50.1; no other git version was tried).

3. Redeploy the `v1.7.0_gg6` archive. If you kept the one you deployed before, use it. A GitHub release asset exists for `v1.7.0_gg6` (13558692 bytes, SHA-256 `e745e7d5363fca8067ddfcc80f3ebea034b93abb4f2912e514492201a729c2af`). Otherwise download that asset rather than rebuilding it: a local rebuild of tag `v1.7.0_gg6` is 59 bytes smaller than the asset, and the archive compare lists six connector class files of it as different from the asset's (measured on one local rebuild; for two of the six, `javap` shows a difference in how a string concatenation is compiled; which compiler built the asset was not established).
4. Publish the rollback commit. Where `custom_build` accepts pushes, run `git push origin rollback-v1.7.0_gg7:custom_build`, which does not depend on which branch is checked out. Where it requires a pull request, run `git push origin rollback-v1.7.0_gg7`, open the pull request into `custom_build` and merge it with a merge commit, so the rollback commit keeps its SHA for the forward path below. Never force-push.

Rehearsed on a dry-run merge of the release tip onto `72e8ccff`: the block above, run unchanged, printed `ok`; after the rollback commit the branch has `VERSION` `v1.7.0_gg6`, has no copy of this note, and has a tree equal to tag `pre-v1.7.0_gg7`, and `git revert <rollback-commit>` restores the tree you rolled back from. To go forward again, once the rollback commit is on `origin/custom_build`, update your local branch with `git switch custom_build` and `git pull --ff-only`, run `git revert <rollback-commit>` on it, and publish the revert as in step 4: push it, or put it on a new branch and open a pull request where `custom_build` requires one. The rollback removes this document from the branch, so read it with `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg7.md`.

Rolling back brings back:

- the `v1.7.0_gg6` MBean `version=` segment and client name, once the `v1.7.0_gg6` archive is deployed (row D3);
- rows F1 to F4 reversed: a stopped task leaves its helper clients open again (section A);
- the insert bound of rows T2 to T5 reversed: on client V2 with `async=false`, an insert can again wait for ever on a server that stops answering (section B);
- rows R1 and R4 to R7 reversed: the inserted-offset map reads the renamed topic again, a `RetriableException` below the top of a cause chain is no longer retriable, and the start-up WARN is gone (sections C, E and F);
- row D1 reversed: `gson` goes back to `2.14.0`;
- on the branch, the `v1.7.0_gg7` documents are gone, this note included.

The row of this section that states the rollback:

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| D5 | Rollback: tag `pre-v1.7.0_gg7` names `72e8ccff`, the published `custom_build` tip before this release, which is also tag `v1.7.0_gg6`. Redeploy the `v1.7.0_gg6` archive. Rolling back reverses rows F1 to F4, the insert bound of rows T2 to T5, rows R1 and R4 to R7, and rows D1 and D3. | rollback block on a dry-run merge: measured; what reverts: read from the code | this release | The block of step 2, extracted and run unchanged on a dry-run merge of the release tip onto `72e8ccff`, printed `ok`, left `VERSION` at `v1.7.0_gg6`, left no copy of this note and a tree equal to the tag, and a revert restored the merged tree. What reverts follows from `git diff v1.7.0_gg6 HEAD -- src/main` and, for `gson`, from row D1. |
