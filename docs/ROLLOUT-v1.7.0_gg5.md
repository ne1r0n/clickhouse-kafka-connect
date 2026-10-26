# Rollout checklist: `v1.7.0_gg4` to `v1.7.0_gg5`

**No connector code changed.** The connector source (`src/main`) is identical to `v1.7.0_gg4`. This release changes the release workflows, adds a fork-owned nightly integration-test workflow and CI checks, and changes the release identity.

If you are coming from a release before `v1.7.0_gg4`, work through [`ROLLOUT-v1.7.0_gg4.md`](ROLLOUT-v1.7.0_gg4.md) first and return here afterwards. The fork's own changes are listed in [`../CHANGELOG.md`](../CHANGELOG.md), and [`FORK-DELTA.md`](FORK-DELTA.md) lists everything the fork patches on top of upstream.

This note names no host, connector, cluster or production system. Every environment-specific value is an angle-bracket stand-in, such as `<connector-name>` or `<rollback-commit>`; replace each one with your own value. Each of the five claims below shows the command that measured it and its output.

## 1. No connector code changed

`git diff v1.7.0_gg4 -- src/main` is empty between the two releases.

```sh
git diff --quiet v1.7.0_gg4 v1.7.0_gg5 -- src/main; echo $?
```

```text
0
```

Measured on the release tip before the tag existed, with `HEAD` for `v1.7.0_gg5`. The shipped archive differs from the `v1.7.0_gg4` archive only in the release identity: the version constant, the two classes that inline it and the manifest `Implementation-Version`. What the release does change: the release workflows, the nightly and its summary script, the CI guard tests, the integration-test harness, `dependabot.yml`, `VERSION` and the documents.

What to do: deploy the archive as before. Nothing in the connector's configuration changes.

## 2. The release archive

The release workflows build the archive with the project's Gradle wrapper through `setup-gradle`. `./gradlew createConfluentArchive` is finalized by `verifyConfluentArchive`, which fails the build on a nested jar, a bundled Kafka API class or a missing connector or client class. Measured by a clean build of the release tip, which wrote `clickhouse-kafka-connect-v1.7.0_gg5.zip`:

```sh
./gradlew clean createConfluentArchive
```

```text
> Task :verifyConfluentArchive
verifyConfluentArchive: 7801 inner entries, no nested jar, no Kafka API class
BUILD SUCCESSFUL in 8s
```

Two earlier dry runs of the release workflows, one each for `pre-release.yaml` and `release.yaml` (runs `40296330498` and `40296481765`), ran green with the `Wrapper Gradle` annotation "Gradle 9.8.1 from the wrapper ran createConfluentArchive and verifyConfluentArchive", at a commit whose release-path inputs equal this release's apart from `VERSION`. No GitHub release is published for `v1.7.0_gg5`: the release is its two tags.

What to do: build the archive from tag `v1.7.0_gg5` with `./gradlew createConfluentArchive`, as before.

## 3. MBean name and client name

The MBean name embeds the release version, and so does the client name that the connector sends on every request. Both read `v1.7.0_gg5` where they read `v1.7.0_gg4`:

- MBean: `com.clickhouse:type=ClickHouseKafkaConnector,connector=<connector-name>,name=SinkTask<id>,version=v1.7.0_gg5`, plus `,topic=<topic>` for per-topic beans.
- Client name: `clickhouse-kafka-connect/v1.7.0_gg5`, the prefix of the HTTP user agent. The rest of the user agent did not change.

Measured on ClickHouse 26.10.3.7 with client V2: the per-task bean and the per-topic bean of `v1.7.0_gg5` both carry `version=v1.7.0_gg5`, and those of `v1.7.0_gg4` carried `version=v1.7.0_gg4`. The query of [item 7 of the gg1 note](ROLLOUT-v1.7.0_gg1.md#item-7-jmx-mbean-name-and-client-name) lists the client names your server has seen:

```sql
SELECT DISTINCT http_user_agent
FROM system.query_log
WHERE event_date >= today() - 1
  AND http_user_agent LIKE '%clickhouse-kafka-connect/%';
```

```text
com.clickhouse:type=ClickHouseKafkaConnector,connector=<connector-name>,name=SinkTask0,version=v1.7.0_gg5,topic=<topic>
com.clickhouse:type=ClickHouseKafkaConnector,connector=<connector-name>,name=SinkTask0,version=v1.7.0_gg5
clickhouse-kafka-connect/v1.7.0_gg5 clickhouse-java-v2/0.9.6 (Mac OS X; jvm:17.0.9) Apache-HttpClient/5.6.5
```

What to do: change dashboard, alert and exporter matchers that spell out `version=v1.7.0_gg4` or `clickhouse-kafka-connect/v1.7.0_gg4`; wildcard forms keep working.

## 4. Turning the nightly on and off

The workflow `Nightly Integration Tests (ClickHouse latest, client V2)` runs the integration tests on ClickHouse `latest` with client V2. A manual run (Actions tab, the workflow, `Run workflow` on `custom_build`) always runs the job. The schedule (03:17 UTC daily) runs the job only while the repository variable `NIGHTLY_ENABLED` is `true`; otherwise each scheduled run shows its job as skipped.

- On: set `NIGHTLY_ENABLED` to `true` under Settings, Secrets and variables, Actions, Variables, after the first manual run is green.
- Off: delete the variable or set it to `false`.

The workflow uses no repository secret and does not touch the archive you deploy; upstream's `integrationTests.yaml` stays disabled.

```sh
grep -nE "cron:|workflow_dispatch:|NIGHTLY_ENABLED" .github/workflows/nightly-integration-tests.yaml
```

```text
16:    - cron: '17 3 * * *'
17:  workflow_dispatch:
25:    if: github.event_name == 'workflow_dispatch' || vars.NIGHTLY_ENABLED == 'true'
```

What to do: nothing for the connector; enable the schedule only when you want the daily signal.

## 5. Rollback

Nothing in the data format changes, so a rollback is a code and workflow rollback. Tag `pre-v1.7.0_gg5` names commit `21c51598`, the published tip of `custom_build` before this release, which is also tag `v1.7.0_gg4`. The tag is published together with this release.

1. Start from the published tip with nothing uncommitted. Run `git fetch --tags origin`, `git checkout custom_build` and `git merge --ff-only origin/custom_build`, and check that `git status --porcelain` prints nothing, with one expected exception: a fresh checkout lists ` M gradlew.bat` without any change of yours, because the committed `gradlew.bat` has Windows line endings and `.gitattributes` asks git to normalise `*.bat` files. Commit whatever else it lists, set it aside with `git stash -u`, or work in a fresh clone: the next command discards uncommitted changes to tracked files without asking.
2. Restore the tree of the tag in one commit. This commit is `<rollback-commit>`:

```sh
git read-tree -u --reset pre-v1.7.0_gg5
git commit -m "Roll back v1.7.0_gg5"
git diff --quiet HEAD pre-v1.7.0_gg5 && echo ok
```

```text
ok
```

3. Redeploy the archive built at tag `v1.7.0_gg4`, the same commit. It builds green with the archive check of section 2, and differs from the `v1.7.0_gg5` archive only in the release identity.

Rehearsed on a dry-run merge of the release tip onto `21c51598`: after the rollback commit the branch has `VERSION` `v1.7.0_gg4` and no nightly workflow file, and `git revert <rollback-commit>` restores the tree you rolled back from. To go forward again, revert the rollback commit; the rollback removes this document from the branch, so read it with `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg5.md`.

Rolling back brings back:

- the `v1.7.0_gg4` MBean `version=` segment and client name, once the `v1.7.0_gg4` archive is deployed (section 3);
- on the branch, the `v1.7.0_gg4` release workflows and `dependabot.yml`;
- no nightly workflow file: its manual and scheduled runs stop, and `NIGHTLY_ENABLED` has no effect (section 4);
- on the branch, the `v1.7.0_gg5` guard tests and documents are gone, this note included;
- connector behaviour does not change.
