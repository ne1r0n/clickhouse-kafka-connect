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

### E-03: the Jackson Map-key residual through the connector (D-03)

Backs: `docs/ROLLOUT-v1.7.0_gg3.md` (the headline exception of the value-free error-text item), `docs/FORK-DELTA.md` Known-costs item 6 and `22-SECURITY.md` R-22-02 (written in plans 24-03 and 24-04). The committed live test `JsonAsStringKeyResidualLiveTest` drives `ClickHouseSinkTask.put()` against the real writer with `clickhouseSettings=input_format_binary_read_json_as_string=1`, a table `(id Int32, j JSON)` and a record `Struct{id, j: Struct{m: Map<String,String>, s: String}}` (the List case uses `l: Array<String>`). Distinct sentinels: Map key `SENTKEYa1b2`, a sibling Map value `SENTVALc3d4`, a sibling Struct string `SENTSTRe5f6`, a List element `SENTLSTg7h8`. The failure trigger is a content-free `java.lang.Object` put into the Map (or List) after both `Struct.put` calls, because Connect validates on `put`.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*JsonAsStringKeyResidualLiveTest' --rerun
```

```text
<testsuite name="com.clickhouse.kafka.connect.sink.JsonAsStringKeyResidualLiveTest" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-10-20T16:15:26.731Z" ...>
JSON_AS_STRING_RESIDUAL server version=26.9.19.9
```

Client V2, ClickHouse `26.9.19.9`. The excerpts below were captured by running a renamed scratch copy of the same test with extra `println` calls in the `wt-tip` worktree (the copy is not committed); the committed test asserts the same facts without printing.

| Destination | Map key (`SENTKEYa1b2`) | Any value (`SENTVALc3d4`, `SENTSTRe5f6`, `SENTLSTg7h8`) |
|-------------|-------------------------|------------------------------------------------------|
| Task status: the cause chain `put` throws, rendered with `printStackTrace` | yes | no |
| DLQ `__connect.errors.exception.message` header | no (it is the `Topic: [..], Partition: [..], MinOffset: [..], MaxOffset: [..], (QueryId: [..])` text) | no |
| DLQ `__connect.errors.exception.stacktrace` header | yes | no |
| `Error inserting records` ERROR throwable (every such event) | yes | no |
| `Error writing value of` line | no | no |

The DLQ `__connect.errors.exception.class.name` header is `java.lang.RuntimeException`. No value sentinel appears in any text the log capture saw (all events of `ClickHouseWriter`, messages and throwables).

The Map case (task status, DLQ stack-trace header and the `Error inserting records` throwable each carry this same line, sentinels only):

```text
Caused by: com.fasterxml.jackson.databind.exc.InvalidDefinitionException: No serializer found for class java.lang.Object and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS) (through reference chain: java.util.HashMap["m"]->java.util.HashMap["SENTKEYa1b2"])
```

`"m"` is the Struct field name (schema metadata); the Map key is record data. The `Error writing value of` line reads `Error writing value of STRUCT to the column `j` of type JSON: exception com.fasterxml.jackson.databind.exc.InvalidDefinitionException`, which names the column, the Kafka type and the exception class only. The List case, with an element Jackson cannot serialize, names an index, so no record text reaches any destination:

```text
Caused by: com.fasterxml.jackson.databind.exc.InvalidDefinitionException: No serializer found for class java.lang.Object and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS) (through reference chain: java.util.HashMap["l"]->java.util.ArrayList[1])
```

Non-vacuity: every case that claims the Jackson route was reached asserts `com.fasterxml.jackson.databind.exc.InvalidDefinitionException` in the cause chain. The control (same record, setting absent) fails earlier with `Column [j] has unsupported type [JSON] for RowBinary serialization. Use insertFormat=json or bypassRowBinary=true instead.` and no record text. Only the key-present assertions would have flipped to absence on branch 2; none had to.

Out of scope (D-14): a bean whose own getter or exception text carries data is not a Connect value type. The matrix holds Connect-typed values only, so D-04's third branch cannot fire on it. That case is recorded here as not measured and not added.

Deviation from the plan text: the plan expected the control to show `Writing JSON in binary is not supported yet`. That `RuntimeException` sits in the writer's JSON branch, but schema validation refuses the JSON column first when the setting is absent, so `put()` surfaces the validation message above; the test asserts the measured text and keeps the other string only in its class comment.

D-04-BRANCH=1

Matches text: pending (24-03, 24-04)
