# Phase 29: Command evidence

- **Base commit:** `P29_BASE` = `c1dc6e5983fe93745526cecd89d3b7e7cff63c7d` (the tip when Phase 29 planning started; every "Phase 29 changed nothing here" check diffs non-`.planning` paths against it).
- **Shell:** macOS zsh, BSD `grep`. Gradle is always run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` because this shell sets that variable to the literal `null`. Log lines carry the local time zone (UTC+3); the date below is UTC.
- **Date (UTC):** 2026-10-23.
- **Rule:** a `Matches text: no` verdict means the document gets fixed, never the evidence. Entries that back no document text carry `Matches text: n/a (gate check)`. Entries whose document is not written yet carry `Matches text: pending (29-0N)`, and the plan that writes that text flips the verdict.

### E-01: VERSION bump, clean build and the anchors mirror

Backs: nothing in a document; this is the PUB-11 gate check for the release identity. The bump commit changes `VERSION` and the `VERSION` row of `.planning/scripts/fork-anchors.txt` and nothing else.

```sh
git show --name-only --format='%h %s' 112fbdb5
git show -U0 --format= 112fbdb5 -- VERSION .planning/scripts/fork-anchors.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean compileJava
grep -n ARTIFACT_VERSION build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java
# anchors mirror: for every row of fork-anchors.txt, grep -cF of the literal in the named file equals the row's count
```

```text
112fbdb5 chore(29-01): bump VERSION to v1.7.0_gg4 with its anchor row (PUB-11)
.planning/scripts/fork-anchors.txt
VERSION

-v1.7.0_gg3                                              (VERSION)
+v1.7.0_gg4
-VERSION	v1.7.0_gg3	1	BLD-07                       (fork-anchors.txt line 41)
+VERSION	v1.7.0_gg4	1	BLD-07

BUILD SUCCESSFUL in 4s   (clean compileJava, exit 0)
4:    public static final String ARTIFACT_VERSION = "v1.7.0_gg4";

rows=94 bad=0
ANCHORS-MIRROR-OK
```

The generated `Version.java` is not a Gradle task input, so the constant reads `v1.7.0_gg4` only after a `clean` build; the line above is from that clean build. `fork-anchor-exemptions.txt` and `verify-merge-invariants.sh` were not touched.

Matches text: n/a (gate check)

### E-02: MBean name and client name, gg3 versus gg4

Backs: `docs/ROLLOUT-v1.7.0_gg4.md`, the MBean and client-name item (written in plan 29-04). The `user-agent` query is the gg1 note's item 7 query (`docs/ROLLOUT-v1.7.0_gg1.md`, "Item 7: JMX MBean name and client name") copied verbatim; the probe's own SQL differs only in the column alias `ua`, which the read-back helper needs. In each run a sink task is started with a unique connector name, one record of constants (`{"id": 1}`, schemaless) is put through it into a fresh table, the `com.clickhouse:type=ClickHouseKafkaConnector,*` MBeans of that connector are listed while the task still runs, the task is stopped, `SYSTEM FLUSH LOGS` is executed as its own statement, and then the query reads `system.query_log` of the run's own container. Each run has its own container.

Result: every MBean of the gg4 run (the per-task bean and the per-topic bean) carries `version=v1.7.0_gg4`, and the client name the connector sends starts `clickhouse-kafka-connect/v1.7.0_gg4`. The same probe on tag `v1.7.0_gg3` shows `v1.7.0_gg3` for both. The part of the user agent after the client name is the client library and the HTTP client: `clickhouse-java-v2/0.9.6 (Mac OS X; jvm:17.0.9)` is the same in both runs, and the `Apache-HttpClient` token changes from `5.6.2` (gg3) to `5.6.5` (gg4), following the httpclient5 bump of Phase 25. Server version of both runs: `26.9.19.9`, client V2. Image: `sha256:bdb5c911a23d92fd364e246ab20b5a0a485f00112f3663fa626b083fb46c146b`, created `2026-10-14T18:05:21.432177888Z`.

Query copied from the gg1 note:

```sql
SELECT DISTINCT http_user_agent
FROM system.query_log
WHERE event_date >= today() - 1
  AND http_user_agent LIKE '%clickhouse-kafka-connect/%';
```

No placeholders. The probe class `Phase29RolloutProbe` is scratch (`/tmp/gsd29/probe/`), copied untracked into a worktree of each tree and not committed. It uses only APIs present at both trees (`ClickHouseBase`, `ClickHouseTestHelpers.createClient`, `executeQueryIgnoreResult`, `dropTable`, `ClickHouseHelperClient.queryV2`, `ClickHouseSinkTask`, `Version.ARTIFACT_VERSION`, all present at `v1.7.0_gg3`).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd29/wt-gg3/gradlew -p /tmp/gsd29/wt-gg3 --console=plain test --tests '*Phase29RolloutProbe.mbeanAndClientName' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd29/wt-tip/gradlew -p /tmp/gsd29/wt-tip --console=plain test --tests '*Phase29RolloutProbe.mbeanAndClientName' --rerun
python3 -I /tmp/gsd29-scripts/xml-probe-lines.py /tmp/gsd29/exec/probe-mbean-gg3.xml
python3 -I /tmp/gsd29-scripts/xml-probe-lines.py /tmp/gsd29/exec/probe-mbean-gg4.xml
```

`wt-gg3` is a detached worktree at tag `v1.7.0_gg3` (`3376a18f`); `wt-tip` is a detached worktree at the gg4 tip after the bump commit (`112fbdb5`). Both XML files report `tests="1" skipped="0" failures="0" errors="0"`.

```text
PROBE-VERSION tree=gg3 version=26.9.19.9
PROBE-ARTIFACT tree=gg3 artifact=v1.7.0_gg3
PROBE-MBEAN tree=gg3 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase29RolloutProbe-mbeanAndClientName-1,name=SinkTask0,version=v1.7.0_gg3
PROBE-MBEAN tree=gg3 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase29RolloutProbe-mbeanAndClientName-1,name=SinkTask0,version=v1.7.0_gg3,topic=p29_mbean_1792776413643
PROBE-AGENT tree=gg3 agent=clickhouse-kafka-connect/v1.7.0_gg3 clickhouse-java-v2/0.9.6 (Mac OS X; jvm:17.0.9) Apache-HttpClient/5.6.2
PROBE-VERSION tree=gg4 version=26.9.19.9
PROBE-ARTIFACT tree=gg4 artifact=v1.7.0_gg4
PROBE-MBEAN tree=gg4 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase29RolloutProbe-mbeanAndClientName-1,name=SinkTask0,version=v1.7.0_gg4
PROBE-MBEAN tree=gg4 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase29RolloutProbe-mbeanAndClientName-1,name=SinkTask0,version=v1.7.0_gg4,topic=p29_mbean_1792776440356
PROBE-AGENT tree=gg4 agent=clickhouse-kafka-connect/v1.7.0_gg4 clickhouse-java-v2/0.9.6 (Mac OS X; jvm:17.0.9) Apache-HttpClient/5.6.5
```

Matches text: pending (29-04)
