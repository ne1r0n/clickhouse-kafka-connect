# Phase 32: Command evidence

- **Base commit:** `P32_BASE` = `8dcbbfc1ced091b49abc6d444b562d591ffc28f2` (HEAD when Phase 32 planning started, branch `sync/v1.7.0`; every "Phase 32 changed nothing here" check diffs non-`.planning` paths against it). "src/main is unchanged" means `git diff --quiet v1.7.0_gg4 HEAD -- src/main`.
- **Shell:** macOS zsh with BSD `grep`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...` because this shell sets that variable to the literal `null`; live runs set `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2` and unset `CLICKHOUSE_CLUSTER_NAME` on their own command line.
- **Docker:** colima profile `vz` (aarch64, 8 CPU, 16 GiB, 256 GiB disk, running); the docker CLI by absolute path `/opt/homebrew/opt/docker/bin/docker`.
- **Date (UTC):** 2026-10-26.
- **Rule:** a `Matches text: no` verdict means the document gets fixed, never the evidence. Entries that back no document text carry `Matches text: n/a (gate check)`. Entries whose document is not written yet carry `Matches text: pending (32-0N)`, and the plan that writes that text flips the verdict. Sentinels and constants only: no record data, host, credential or token appears in any entry.

### E-01: VERSION bump, clean build and the anchors mirror

Backs: nothing in a document; this is the CIG-02 gate check for the release identity. The bump commit changes `VERSION` and the `VERSION` row of `.planning/scripts/fork-anchors.txt` and nothing else.

```sh
git show --name-only --format='%h %s' a6bfd78f
git show -U0 --format= a6bfd78f -- VERSION .planning/scripts/fork-anchors.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean compileJava
grep -n ARTIFACT_VERSION build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java
# anchors mirror: for every row of fork-anchors.txt, grep -cF of the literal in the named file equals the row's count
```

```text
a6bfd78f chore(32-01): bump VERSION to v1.7.0_gg5 with its anchor row (CIG-02)

.planning/scripts/fork-anchors.txt
VERSION

-v1.7.0_gg4                                              (VERSION)
+v1.7.0_gg5
-VERSION	v1.7.0_gg4	1	BLD-07                       (fork-anchors.txt line 41)
+VERSION	v1.7.0_gg5	1	BLD-07

BUILD SUCCESSFUL in 4s   (clean compileJava, exit 0)
4:    public static final String ARTIFACT_VERSION = "v1.7.0_gg5";

rows=131 bad=0
ANCHORS-MIRROR-OK
```

The generated `Version.java` is not a Gradle task input, so the constant reads `v1.7.0_gg5` only after a `clean` build; the line above is from that clean build. `fork-anchor-exemptions.txt` and both invariant scripts were not touched.

Matches text: n/a (gate check)

### E-02: MBean name and client name, gg4 versus gg5

Backs: `docs/ROLLOUT-v1.7.0_gg5.md`, the MBean and client-name claim (written in plan 32-02), and the CHANGELOG line on the same change (32-03). The user-agent query is the gg1 note's item 7 query (`docs/ROLLOUT-v1.7.0_gg1.md`, "Item 7: JMX MBean name and client name"); the probe's own SQL differs only in the column alias `ua`, which the read-back helper needs. In each run a sink task is started with a unique connector name against a fresh table (created before `task.start()`), one record of constants (`id = 1`) is put, the `com.clickhouse:type=ClickHouseKafkaConnector,*` MBeans of that connector are listed while the task runs, the task is stopped, `SYSTEM FLUSH LOGS` runs as its own statement, and the user-agent query reads `system.query_log`.

Result: every MBean of the gg5 run (the per-task bean and the per-topic bean) carries `version=v1.7.0_gg5`, and the client name the connector sends starts `clickhouse-kafka-connect/v1.7.0_gg5`. The same probe on tag `v1.7.0_gg4` shows `v1.7.0_gg4` for both. The part of the user agent after the client name is identical in the two runs: `clickhouse-java-v2/0.9.6 (Mac OS X; jvm:17.0.9) Apache-HttpClient/5.6.5`. Both runs read the same server, `26.10.3.7`, from the same `latest` image.

Query copied from the gg1 note:

```sql
SELECT DISTINCT http_user_agent
FROM system.query_log
WHERE event_date >= today() - 1
  AND http_user_agent LIKE '%clickhouse-kafka-connect/%';
```

No placeholders. The probe class `Phase32RolloutProbe` is scratch (`/tmp/gsd32/probe/`), copied untracked into a detached worktree of each tree and not committed. It uses only APIs present at both trees (`ClickHouseBase`, `ClickHouseTestHelpers.createClient`, `executeQueryIgnoreResult`, `dropTable`, `ClickHouseHelperClient.queryV2`, `ClickHouseSinkTask`, `Version.ARTIFACT_VERSION`).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE -u CLICKHOUSE_CLUSTER_NAME CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 /tmp/gsd32/wt-gg4/gradlew -p /tmp/gsd32/wt-gg4 --console=plain test --tests '*Phase32RolloutProbe.mbeanAndClientName' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE -u CLICKHOUSE_CLUSTER_NAME CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 /tmp/gsd32/wt-tip/gradlew -p /tmp/gsd32/wt-tip --console=plain test --tests '*Phase32RolloutProbe.mbeanAndClientName' --rerun
# PROBE lines are the system-out of TEST-com.clickhouse.kafka.connect.sink.Phase32RolloutProbe.xml of each run
/opt/homebrew/opt/docker/bin/docker image inspect clickhouse/clickhouse-server:latest --format '{{.Id}} {{.Created}}'
/opt/homebrew/opt/docker/bin/docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
```

`wt-gg4` is a detached worktree at tag `v1.7.0_gg4` (`21c51598`); `wt-tip` is a detached worktree at the gg5 tip after the bump commit (`a6bfd78f`). Both runs ended `BUILD SUCCESSFUL` (22 tasks executed) and both XML files report `tests="1" skipped="0" failures="0" errors="0"`.

```text
PROBE-VERSION tree=gg4 version=26.10.3.7
PROBE-ARTIFACT tree=gg4 artifact=v1.7.0_gg4
PROBE-MBEAN tree=gg4 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase32RolloutProbe-mbeanAndClientName-1,name=SinkTask0,version=v1.7.0_gg4,topic=p32_mbean_1793048452805
PROBE-MBEAN tree=gg4 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase32RolloutProbe-mbeanAndClientName-1,name=SinkTask0,version=v1.7.0_gg4
PROBE-AGENT tree=gg4 agent=clickhouse-kafka-connect/v1.7.0_gg4 clickhouse-java-v2/0.9.6 (Mac OS X; jvm:17.0.9) Apache-HttpClient/5.6.5
PROBE-VERSION tree=gg5 version=26.10.3.7
PROBE-ARTIFACT tree=gg5 artifact=v1.7.0_gg5
PROBE-MBEAN tree=gg5 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase32RolloutProbe-mbeanAndClientName-1,name=SinkTask0,version=v1.7.0_gg5,topic=p32_mbean_1793048475211
PROBE-MBEAN tree=gg5 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase32RolloutProbe-mbeanAndClientName-1,name=SinkTask0,version=v1.7.0_gg5
PROBE-AGENT tree=gg5 agent=clickhouse-kafka-connect/v1.7.0_gg5 clickhouse-java-v2/0.9.6 (Mac OS X; jvm:17.0.9) Apache-HttpClient/5.6.5
sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce 2026-10-26T00:06:54.855447882Z
26.10.3.7
```

The image id and creation date are the floating `latest` pin of this evidence (D-07): `sha256:739e099abd11...b1ce`, created 2026-10-26T00:06:54Z, `SELECT version()` = `26.10.3.7`. A red result later on unchanged code means checking this date first.

Matches text: pending (32-02)
