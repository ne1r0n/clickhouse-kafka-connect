# Rollout checklist: `v1.7.0_gg7` to `v1.7.0_gg8`

**The connector source changes in this release.** `git diff --shortstat v1.7.0_gg7 v1.7.0_gg8 -- src/main` reports 6 files changed, 428 insertions and 72 deletions. It was measured with `HEAD` in place of `v1.7.0_gg8` on the release tip, because the tag `v1.7.0_gg8` does not exist before the release is published, and so was every other command of this note that names `v1.7.0_gg8`. The five changes at a glance:

1. Two items of upstream's untagged tail are new: a serialization failure now logs one ERROR line with its record context and no record value, and a field name that contains `/` in a `DESCRIBE` response is pinned by tests (section A).
2. The insert timeout `clickhouseClientInsertTimeoutMs` now bounds more on both clients: on client V2 a body-write stall, on client V1 the wait for the insert response. Both end with a retriable error, and a timed-out insert can produce a duplicate (section B).
3. `ClickHouseSinkTask.stop()` stops the proxy task even when the delivery strategy's `stop()` throws; a `RetriableException` out of `put` is redelivered without bound, a limit by decision; and the retry-budget question for `additionalRetriableErrorCodes` is closed by decision (section C).
4. The public constant `DeclaredNullability.ESCAPED_NEWLINE` is removed from the connector jar (section D).
5. The release identity, which the JMX name and the client name carry, reads `v1.7.0_gg8`; the archive differs from the `v1.7.0_gg7` archive in connector classes and the manifest only; and the rollback is rehearsed (section E).

The fork's own changes are listed in [`../CHANGELOG.md`](../CHANGELOG.md), and [`FORK-DELTA.md`](FORK-DELTA.md) lists everything the fork patches on top of upstream. If you are coming from a release before `v1.7.0_gg7`, work through [`ROLLOUT-v1.7.0_gg7.md`](ROLLOUT-v1.7.0_gg7.md) first and return here afterwards.

This note names no host, connector, cluster or production system. Every environment-specific value is an angle-bracket value, such as `<connector-name>`, `<topic>`, `<group>` or `<dir>`; replace each one with your own value.

**How each claim is labelled.** Every effect of this release has one row in a table below, with a label for how the claim is known. There are three labels:

- `measured`: a test or a command ran, and the row names it.
- `read from the code`: the row names a file and a site; nothing ran for this claim.
- `derived`: the claim follows from other rows or from a stated mechanism; nothing ran for this claim itself.

A label may carry one parenthetical qualifier that narrows it, for example `measured (stub only)`. A row that makes claims of different strength says which label each claim has, in the form `claim: label; claim: label`.

**Which connectors a row concerns.** The column `Applies when` limits each row. A connector runs client V2 only when its configuration sets `client_version` to a value other than `V1`; a connector that does not set `client_version` runs client V1, the default (read from the code: `ClickHouseSinkConfig` reads `client_version` with the default `V1`, and `ClickHouseWriter` selects client V2 for any other value). Every statement that holds only on client V2 says `client V2`, and every statement that holds only on client V1 says `client V1`. The values are:

- `client V2`, `client V1`, `client V1 and client V2` and `either client`: the row holds on that client, or on both. A setting after a comma, for example `client V2, socket_timeout not set by the operator`, adds a setting the row needs.
- `any connector with a dead letter queue`, `any connector that sets additionalRetriableErrorCodes` and `anyone compiling against the connector jar`: the row concerns exactly that configuration or that reader.
- `this release`: the row describes the release as a whole. `CI only`: the row concerns the build and test workflows, not the archive. `as named in the row`: the row names its cases itself.

## E. Release identity, archive and rollback

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| D1 | The release identity, which the JMX `version=` segment and the client name carry, reads `v1.7.0_gg8`. Update matchers that spell out the old value. | identity: read from the code; the identity classes differ in the archive compare: measured | this release | `Version.ARTIFACT_VERSION` is generated from `VERSION` and is read by `ClickHouseHelperClient` (client name) and `SinkTaskStatistics` (MBean name). The archive compare lists `Version` and `SinkTaskStatistics` as changed, and `SinkTaskStatistics` has no source change since `v1.7.0_gg7`, so its difference is the inlined identity. |
| D2 | The archive holds 7932 inner entries for `v1.7.0_gg7` and 7935 for the release tip. Local rebuilds are 13564144 and 13572233 bytes; these are sizes of local rebuilds, not of a release asset. The archive compare lists 17 differing entries: 3 connector classes present only in the newer archive (`ClickHouseWriter$3`, `ClickHouseWriter$InsertBody` and `ClickHouseWriter$InsertWriteDeadline`), 13 connector classes that changed, and the manifest, which differs only in `Implementation-Version`. Its version table lists 18 libraries and differs for none, so no library changes. | measured | this release | `./gradlew createConfluentArchive` run on an export of tag `v1.7.0_gg7` and on an export of the release tip; the `verifyConfluentArchive:` line of each run states the inner entry count. `.github/scripts/compare_confluent_archives.py` run on the two archives with a comment-only allow-list, so that every difference is listed, reports `files-a 7512 files-b 7515 only-a 0 only-b 3 changed 14 same 7498`. |

### Release identity

The MBean name embeds the release version, and so does the client name that the connector sends on every request. Both read `v1.7.0_gg8` where they read `v1.7.0_gg7`:

- MBean: `com.clickhouse:type=ClickHouseKafkaConnector,connector=<connector-name>,name=SinkTask<id>,version=v1.7.0_gg8`, plus `,topic=<topic>` for per-topic beans.
- Client name: `clickhouse-kafka-connect/v1.7.0_gg8`, the prefix of the HTTP user agent.

What to do: change dashboard, alert and exporter matchers that spell out `version=v1.7.0_gg7` or `clickhouse-kafka-connect/v1.7.0_gg7`; matchers that do not spell out the version are unaffected.

### Rollback

This release changes no state format and no table format (`KeeperStateProvider` and `Processing` have no change since `v1.7.0_gg7`), so a rollback is a code rollback. Tag `pre-v1.7.0_gg8` names commit `94834763`, the published tip of `custom_build` before this release, which is also tag `v1.7.0_gg7`. The tag is created locally before the release and pushed to origin when the release is published. Step 2 needs it on origin: if `git fetch --tags origin` does not bring `pre-v1.7.0_gg8`, stop and ask the maintainer to publish it.

1. Start from the published tip with nothing uncommitted. Run `git fetch --tags origin`, `git checkout custom_build` and `git merge --ff-only origin/custom_build`, and check that `git status --porcelain` prints nothing. Set whatever it lists aside with `git stash -u`, keep it on its own branch, or work in a fresh clone, so that none of it becomes part of the rollback; do not commit it on `custom_build`, because step 2 branches from there and step 4 publishes that branch: the `git read-tree` of step 2 discards uncommitted and staged changes to tracked files without asking, and it overwrites untracked files at paths that the tag contains.
2. Restore the tree of the tag in one commit on a new branch, so that your local `custom_build` stays as published. This commit is `<rollback-commit>`:

```sh
git switch -c rollback-v1.7.0_gg8
git read-tree -u --reset pre-v1.7.0_gg8
git commit -m "Roll back v1.7.0_gg8"
git diff --quiet HEAD pre-v1.7.0_gg8 && echo ok
```

```text
ok
```

3. Redeploy the `v1.7.0_gg7` archive. If you kept the one you deployed before, use it. A GitHub release asset exists for `v1.7.0_gg7` (13564238 bytes, SHA-256 `3dbdcf03e5825073b7b0535718c9998a5e5754c0ae05a17502bbf4c6d69472af`). Otherwise download that asset rather than rebuilding it: a local rebuild of tag `v1.7.0_gg7` is 94 bytes smaller than the asset, and the archive compare lists six connector class files of it as different from the asset's (measured on one local rebuild; which compiler built the asset was not established).
4. Publish the rollback commit. Where `custom_build` accepts pushes, run `git push origin rollback-v1.7.0_gg8:custom_build`, which does not depend on which branch is checked out. Where it requires a pull request, run `git push origin rollback-v1.7.0_gg8`, open the pull request into `custom_build` and merge it with a merge commit, so the rollback commit keeps its SHA for the forward path below. Never force-push.

Rehearsed on a dry-run merge of the release branch `sync/v1.7.0` onto `94834763` (an earlier commit than the release tip; the block above has not changed since): the block above, run unchanged, printed `ok`; after the rollback commit the branch has `VERSION` `v1.7.0_gg7`, has no copy of this note, and has a tree equal to tag `pre-v1.7.0_gg8`, and `git revert <rollback-commit>` restores the tree you rolled back from. To go forward again, once the rollback commit is on `origin/custom_build`, update your local branch with `git switch custom_build` and `git pull --ff-only`, run `git revert <rollback-commit>` on it, and publish the revert as in step 4: push it, or put it on a new branch and open a pull request where `custom_build` requires one. The rollback removes this document from the branch, so read it with `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg8.md`.

Rolling back brings back:

- the `v1.7.0_gg7` MBean `version=` segment and client name, once the `v1.7.0_gg7` archive is deployed (row D1);
- rows U1 to U3 reversed: the `Record serialization failed` ERROR line and the value-free rebuild of a Jackson chain are gone, and the `DESCRIBE` reader is the one of `v1.7.0_gg7` (section A);
- rows T2, T3, T5 and T9 reversed: a client V2 body-write stall and the client V1 insert wait have no connector bound again, and the help text of `clickhouseClientInsertTimeoutMs` is the old one (section B);
- row S1 reversed: a delivery strategy whose `stop()` throws skips the proxy stop again (section C);
- row P1 reversed: the public constant `DeclaredNullability.ESCAPED_NEWLINE` is back in the jar (section D);
- on the branch, the `v1.7.0_gg8` documents are gone, this note included.

The row of this section that states the rollback:

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| D3 | Rollback: tag `pre-v1.7.0_gg8` names `94834763`, the published `custom_build` tip before this release, which is also tag `v1.7.0_gg7`. Redeploy the `v1.7.0_gg7` release asset, not a local rebuild. Rolling back reverses rows U1 to U3, T2, T3, T5 and T9, S1, P1 and D1. | rollback block on a dry-run merge: measured; what reverts: read from the code | this release | The block of step 2, extracted from this note and run unchanged on a dry-run merge of the release branch onto `94834763` (an earlier commit than the release tip), printed `ok`, left `VERSION` at `v1.7.0_gg7`, left no copy of this note and a tree equal to the tag, and a revert restored the merged tree. What reverts follows from `git diff v1.7.0_gg7 v1.7.0_gg8 -- src/main` and from the rows named. |
