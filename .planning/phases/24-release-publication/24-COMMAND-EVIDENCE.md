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

### E-04: the shipped archive at v1.7.0_gg2, the pre-merge tip and the gg3 tip (D-07, D-08)

Backs: `docs/ROLLOUT-v1.7.0_gg3.md` (the dependency item and the rollback artifact wording), `docs/FORK-DELTA.md` (the catalog and BLD-14 rows) and the 21-IN-03 disposition (written in plans 24-03, 24-04 and 24-05). The shipped artifact is the `createConfluentArchive` zip (`build.gradle.kts`). Three throwaway worktrees were built on the same UTC day (2026-10-20): `wt-gg2` at tag `v1.7.0_gg2` (`a246ca01`), `wt-pre` at `6473e24f` (`origin/custom_build`, the pre-merge tip) and `wt-tip` at `98b9e04d` (the gg3 tip after the bump commit and the D-03 test). Extracted content is untrusted data: each archive was unpacked into new, empty directories under `/tmp/gsd24/x-<id>/{zip,jar}`, the compare script lives in `/tmp/gsd24-scripts`, and Python ran with `-I`.

D-07: what separates tag `v1.7.0_gg2` from the pre-merge tip.

```sh
git diff --stat a246ca01b073faae86e2e92bb0288c07dd7eb0d7 6473e24f372a8b37dfd8cc8cf3124e6cff52edca
```

```text
 build.gradle.kts          | 2 +-
 gradle/libs.versions.toml | 4 ++--
 2 files changed, 3 insertions(+), 3 deletions(-)
```

The three lines are the Dependabot bumps (`slf4j-reload4j` 2.0.17 to 2.0.20, `lombok` 1.18.46 to 1.18.48, `spotless` 8.10.1 to 8.10.4; read from `git show v1.7.0_gg2:gradle/libs.versions.toml` and from the same file at `6473e24f`).

Builds (house prefix `env -u TESTCONTAINERS_HOST_OVERRIDE` and `-p <worktree>`; logs in `/tmp/gsd24/exec/build-<gg2|pre|tip>.log`, all exit 0):

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-gg2/gradlew -p /tmp/gsd24/wt-gg2 --console=plain createConfluentArchive
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-pre/gradlew -p /tmp/gsd24/wt-pre --console=plain clean createConfluentArchive
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-tip/gradlew -p /tmp/gsd24/wt-tip --console=plain clean createConfluentArchive
for id in gg2 pre tip; do shasum -a 256 /tmp/gsd24/wt-$id/build/confluent/*.zip /tmp/gsd24/x-$id/zip/*/lib/*.jar; done
python3 -I /tmp/gsd24-scripts/cmp.py /tmp/gsd24/x-gg2/jar /tmp/gsd24/x-pre/jar
python3 -I /tmp/gsd24-scripts/cmp.py /tmp/gsd24/x-pre/jar /tmp/gsd24/x-tip/jar
```

```text
2e3583a9a0bd20b12172cc0efae6a1adb461e23dbbb9b3fe08b06dfca46689fb  wt-gg2/build/confluent/clickhouse-kafka-connect-v1.7.0_gg2.zip
95256b62c11231d2c5a45ed14e7c69a39c9876dcd218257930fd611434defaa3  x-gg2/zip/clickhouse-kafka-connect-v1.7.0_gg2/lib/clickhouse-kafka-connect-v1.7.0_gg2-confluent.jar
2e3583a9a0bd20b12172cc0efae6a1adb461e23dbbb9b3fe08b06dfca46689fb  wt-pre/build/confluent/clickhouse-kafka-connect-v1.7.0_gg2.zip
95256b62c11231d2c5a45ed14e7c69a39c9876dcd218257930fd611434defaa3  x-pre/zip/clickhouse-kafka-connect-v1.7.0_gg2/lib/clickhouse-kafka-connect-v1.7.0_gg2-confluent.jar
fc4a0511fc90f101823a2c275ed325432bbf61e60b3fb82fe9889c19ea370464  wt-tip/build/confluent/clickhouse-kafka-connect-v1.7.0_gg3.zip
c2514401f76729d17f61bab3d405288801c2b162dba10a58eead3d20e264b36e  x-tip/zip/clickhouse-kafka-connect-v1.7.0_gg3/lib/clickhouse-kafka-connect-v1.7.0_gg3-confluent.jar

cmp.py gg2 vs pre:
files: 7375 7375
only-in-a: []
only-in-b: []
changed: 0

cmp.py pre vs tip:
files: 7375 7376
only-in-a: []
only-in-b: ['com/clickhouse/kafka/connect/util/WithheldMessageException.class']
changed: 11
   META-INF/MANIFEST.MF
   com/clickhouse/kafka/connect/sink/Version.class
   com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$1.class
   com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$2.class
   com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.class
   com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.class
   com/clickhouse/kafka/connect/sink/db/mapping/Column.class
   com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability$1.class
   com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability$Assignment.class
   com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.class
   com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.class
```

Result: the archive built at tag `v1.7.0_gg2` and at the pre-merge tip is the same file (identical whole-zip and inner-jar SHA-256, 7375 entries, 0 only-in-a, 0 only-in-b, 0 changed). So the three bumps change no entry of the shipped jar.

Surprise recorded, not waved through (research assumption A3): the plan's allowed set for the pre-merge to gg3 compare named nine changed entries; the measurement shows eleven. The two extra entries, `ClickHouseHelperClient.class` and `SinkTaskStatistics.class`, are the classes that read `Version.ARTIFACT_VERSION`, a `static final String` literal that javac inlines (`ClickHouseHelperClient.CONNECT_CLIENT_NAME = "clickhouse-kafka-connect/" + Version.ARTIFACT_VERSION`, and `SinkTaskStatistics` builds the MBean name from it). They hold `v1.7.0_gg2` in the pre-merge jar and `v1.7.0_gg3` in the gg3 jar (byte search of both class files), which is the MBean `version=` and client-name change of E-02. Control: a fourth throwaway worktree at the same commit with only `VERSION` set back to `v1.7.0_gg2` (`wt-ver`, removed afterwards) built the archive and compared against the pre-merge jar:

```text
files: 7375 7376
only-in-a: []
only-in-b: ['com/clickhouse/kafka/connect/util/WithheldMessageException.class']
changed: 7
   com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$1.class
   com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$2.class
   com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.class
   com/clickhouse/kafka/connect/sink/db/mapping/Column.class
   com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability$1.class
   com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability$Assignment.class
   com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.class
```

So the four entries that move with the version are `Version.class`, `META-INF/MANIFEST.MF` (`Implementation-Version` only), `ClickHouseHelperClient.class` and `SinkTaskStatistics.class`; everything else that changes is the seven Phase 22-23 classes plus the one added class `util/WithheldMessageException.class`. The artifact check passes with those two classes added to the allowed set (`/tmp/gsd24-scripts/artifact-diff-check.py`); with the plan's original nine-entry set it exits 1 because of exactly them.

Manifest, dependencies and the logging jar:

```sh
diff /tmp/gsd24/x-gg2/zip/*/manifest.json /tmp/gsd24/x-pre/zip/*/manifest.json
diff /tmp/gsd24/x-pre/zip/*/manifest.json /tmp/gsd24/x-tip/zip/*/manifest.json
grep -nE '"version"|"release_date"' /tmp/gsd24/x-{gg2,pre,tip}/zip/*/manifest.json
ls /tmp/gsd24/x-tip/jar/META-INF/maven/*/*/pom.properties | wc -l
find /tmp/gsd24/x-tip -iname '*reload4j*'
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-tip/gradlew -p /tmp/gsd24/wt-tip -q dependencies --configuration clickhouseDependencies | grep -iE 'slf4j|reload4j'
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-tip/gradlew -p /tmp/gsd24/wt-tip -q dependencies --configuration runtimeClasspath | grep -iE 'slf4j|reload4j'
```

```text
gg2 vs pre manifest.json: identical
pre vs tip manifest.json: 3c3 "version" : "v1.7.0_gg2" -> "v1.7.0_gg3"   (no other line differs)
"release_date": "2026-10-20"  in all three (the build date, 3 builds on the same UTC day)
pom.properties files in the gg3 jar: 18 (lz4-java, jackson x3, gson, error_prone_annotations, guava x3, j2objc, commons-codec, commons-io, commons-compress, commons-lang3, httpclient5, httpcore5-h2, httpcore5, slf4j-api 2.0.7)
find -iname '*reload4j*': (empty)
clickhouseDependencies: org.slf4j:slf4j-api:1.7.36 -> 2.0.7 (no reload4j)
runtimeClasspath: org.slf4j:slf4j-reload4j:2.0.20 -> ch.qos.reload4j:reload4j:1.2.22, slf4j-api 2.0.20
```

The META-INF/MANIFEST.MF of the inner jar differs between the pre-merge and gg3 jars in one line, `Implementation-Version: v1.7.0_gg2` to `v1.7.0_gg3`.

Sentence the documents may use: none of the three bumps changes an entry of the shipped jar. `slf4j-reload4j` and `reload4j` are on `runtimeClasspath` (tests and local runs) and are not in the Confluent archive, whose only logging classes are `slf4j-api` 2.0.7; Lombok 1.18.48 generated class files byte-identical to those of 1.18.46; spotless is build-time only. `manifest.json` carries `version` and `release_date` besides the fixed fields, and `release_date` is the build date, so whole-zip equality holds only for builds of the same day (the 2026-10-20 builds above agree; across days only the per-entry compare is claimed). Between the pre-merge tip and the gg3 tip the jar gains `WithheldMessageException.class` and changes the seven Phase 22-23 classes, `ClickHouseHelperClient.class`, `SinkTaskStatistics.class`, `Version.class` and `META-INF/MANIFEST.MF`.

Matches text: pending (24-03, 24-04)

### E-05: BLD-14 re-established for Lombok 1.18.48 (21-IN-03)

Backs: `docs/FORK-DELTA.md`, the `lombok.config` row (BLD-14), and the 21-IN-03 disposition (written in plans 24-04 and 24-05). Default `javac` prints at most 100 warnings, so a normal build log cannot show a Lombok warning. The build below lifts the cap with an init script (`allprojects { tasks.withType(JavaCompile).configureEach { options.compilerArgs += ['-Xmaxwarns', '100000'] } }`). The gg2 tree uses Lombok 1.18.46, the gg3 tip and the negative control 1.18.48. The negative control is a throwaway worktree at the same commit as the gg3 tip with only the line `lombok.jacksonized.jacksonVersion += 2` deleted from `lombok.config` (`wt-neg`, removed afterwards).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-gg2/gradlew -p /tmp/gsd24/wt-gg2 --console=plain -I /tmp/gsd24-scripts/maxwarns.gradle clean compileJava
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-tip/gradlew -p /tmp/gsd24/wt-tip --console=plain -I /tmp/gsd24-scripts/maxwarns.gradle clean compileJava
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-neg/gradlew -p /tmp/gsd24/wt-neg --console=plain -I /tmp/gsd24-scripts/maxwarns.gradle clean compileJava
```

```text
gg2  (Lombok 1.18.46): warnings 273, "Ambiguous: Jackson2 and Jackson3" 0
tip  (Lombok 1.18.48): warnings 273, "Ambiguous: Jackson2 and Jackson3" 0
neg  (Lombok 1.18.48, line removed): warnings 274, "Ambiguous: Jackson2 and Jackson3" 1
/private/tmp/gsd24/wt-neg/src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java:18: warning: Ambiguous: Jackson2 and Jackson3 exist; define which variant(s) you want in 'lombok.config'. See https://projectlombok.org/features/experimental/Jacksonized
```

Result: with the cap lifted, the gg2 and gg3 trees compile with more than 100 warnings and none says `Ambiguous: Jackson2 and Jackson3`; the control without `lombok.jacksonized.jacksonVersion += 2` prints that warning for `ClickHouseFieldDescriptor.java`, so the absence is the setting's doing. Lombok-generated class bytes are identical between 1.18.46 and 1.18.48 (E-04), which is the stronger form of an older `javap` comparison. `lombok.config`, `build.gradle.kts` and `gradle/` are unchanged since `P24_BASE`.

Matches text: pending (24-04)
