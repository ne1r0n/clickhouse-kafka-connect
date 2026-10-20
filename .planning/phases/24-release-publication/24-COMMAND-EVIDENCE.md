# Phase 24: Command evidence (D-03, D-04)

- **Base commit:** `P24_BASE` = `07bf1e7695cc32fa7239b0b9ee0feed06c079f63` (the tip when Phase 24 planning started; every "Phase 24 changed nothing here" check diffs non-`.planning` paths against it).
- **Shell:** macOS zsh, BSD `grep`. Gradle is always run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` because this shell sets that variable to the literal `null`. Log lines carry the local time zone (UTC+3); the date below is UTC.
- **Date (UTC):** 2026-10-20.
- **Rule:** a `Matches text: no` verdict means the doc text gets fixed, never the evidence. Entries that back no doc text carry `Matches text: n/a (gate check)`. Entries whose doc text is not written yet carry `Matches text: pending (24-0N)`, and the plan that writes that text flips the verdict.

### E-01: VERSION bump, clean build and the anchors mirror

Backs: nothing in a document; this is the PUB-09 gate check for the release identity. The bump commit changes `VERSION` and the `VERSION` row of `.planning/scripts/fork-anchors.txt` and nothing else.

```sh
git show --name-only --format='%h %s' 3c62abe2
git show -U0 --format= 3c62abe2 -- VERSION .planning/scripts/fork-anchors.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean compileJava
grep -n ARTIFACT_VERSION build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java
# anchors mirror: for every row of fork-anchors.txt, grep -cF of the literal in the named file equals the row's count
```

```text
3c62abe2 chore(24-01): bump VERSION to v1.7.0_gg3 with its anchor row (PUB-09)
.planning/scripts/fork-anchors.txt
VERSION

-v1.7.0_gg2                                              (VERSION)
+v1.7.0_gg3
-VERSION	v1.7.0_gg2	1	BLD-07                       (fork-anchors.txt line 41)
+VERSION	v1.7.0_gg3	1	BLD-07

BUILD SUCCESSFUL in 4s   (clean compileJava, exit 0)
4:    public static final String ARTIFACT_VERSION = "v1.7.0_gg3";

rows=69 bad=0
ANCHORS-MIRROR-OK
```

The generated `Version.java` is not a Gradle task input, so the constant reads `v1.7.0_gg3` only after a `clean` build; the line above is from that clean build. `fork-anchor-exemptions.txt` and `verify-merge-invariants.sh` were not touched.

Matches text: n/a (gate check)

### E-02: MBean name and client name, gg2 versus gg3

Backs: `docs/ROLLOUT-v1.7.0_gg3.md`, the MBean and client-name item (written in plan 24-03). The `user-agent` query is the gg1 note's item 7 query (`docs/ROLLOUT-v1.7.0_gg1.md`) copied verbatim; the probe's own SQL differs only in the column alias `ua`, which the read-back helper needs. In each run a sink task is started with a unique connector name, one record of constants is put through it, the `com.clickhouse:type=ClickHouseKafkaConnector,*` MBeans of that connector are listed while the task still runs, the task is stopped, `SYSTEM FLUSH LOGS` is executed as its own statement, and then the query reads `system.query_log` of the run's own container.

Result: every MBean of the gg3 run (the per-task bean and the per-topic bean) carries `version=v1.7.0_gg3`, and the client name the connector sends starts `clickhouse-kafka-connect/v1.7.0_gg3`. The same probe on tag `v1.7.0_gg2` shows `v1.7.0_gg2` for both. The rest of the user agent (`clickhouse-java-v2/0.9.6 (...) Apache-HttpClient/5.6.2`) is the client library and does not change between the two. Server version of both runs: `26.9.19.9`, client V2.

Query copied from the gg1 note:

```sql
SELECT DISTINCT http_user_agent
FROM system.query_log
WHERE event_date >= today() - 1
  AND http_user_agent LIKE '%clickhouse-kafka-connect/%';
```

No placeholders. The probe class `Phase24RolloutProbe` is scratch (`/tmp/gsd24/probe/`), copied untracked into a worktree of each tree and not committed.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-gg2/gradlew -p /tmp/gsd24/wt-gg2 --console=plain test --tests '*Phase24RolloutProbe.mbeanAndClientName' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-tip/gradlew -p /tmp/gsd24/wt-tip --console=plain test --tests '*Phase24RolloutProbe.mbeanAndClientName' --rerun
python3 -I /tmp/gsd24/probe/xml-probe-lines.py /tmp/gsd24/exec/probe-mbean-gg2.xml
python3 -I /tmp/gsd24/probe/xml-probe-lines.py /tmp/gsd24/exec/probe-mbean-gg3.xml
```

`wt-gg2` is a detached worktree at tag `v1.7.0_gg2` (`a246ca01`); `wt-tip` is a detached worktree at the gg3 tip after the bump commit (`3c62abe2`). Both XML files report `tests="1" skipped="0" failures="0" errors="0"`.

```text
PROBE-VERSION tree=gg2 version=26.9.19.9
PROBE-MBEAN tree=gg2 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase24RolloutProbe-mbeanAndClientName-1,name=SinkTask0,version=v1.7.0_gg2,topic=p24_mbean_1792512683430
PROBE-MBEAN tree=gg2 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase24RolloutProbe-mbeanAndClientName-1,name=SinkTask0,version=v1.7.0_gg2
PROBE-AGENT tree=gg2 agent=clickhouse-kafka-connect/v1.7.0_gg2 clickhouse-java-v2/0.9.6 (Mac OS X; jvm:17.0.9) Apache-HttpClient/5.6.2
PROBE-VERSION tree=gg3 version=26.9.19.9
PROBE-MBEAN tree=gg3 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase24RolloutProbe-mbeanAndClientName-1,name=SinkTask0,version=v1.7.0_gg3,topic=p24_mbean_1792512705113
PROBE-MBEAN tree=gg3 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase24RolloutProbe-mbeanAndClientName-1,name=SinkTask0,version=v1.7.0_gg3
PROBE-AGENT tree=gg3 agent=clickhouse-kafka-connect/v1.7.0_gg3 clickhouse-java-v2/0.9.6 (Mac OS X; jvm:17.0.9) Apache-HttpClient/5.6.2
```

Matches text: pending (24-03)
