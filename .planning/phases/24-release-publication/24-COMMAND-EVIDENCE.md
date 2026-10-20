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

Matches text: yes

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

Matches text: yes

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

Matches text: yes

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

Matches text: yes

### E-10: Column trees, gg2 versus gg3 (D-09)

Backs: `docs/ROLLOUT-v1.7.0_gg3.md`, the in-flight batches item (the shape that encodes differently) and the rollback item (written in plan 24-03). Question: which batches does `v1.7.0_gg2` encode differently from `v1.7.0_gg3`? Bytes can differ only through the `isNullable` flags of the `Column` tree that discovery builds, so the real `ClickHouseHelperClient.describeTable` of each release was run over the same fixture tables on the same server and every node of every tree was dumped (name path, `isNullable`), together with the two `DeclaredNullability` WARN counts. The classes are the real ones: a worktree at tag `v1.7.0_gg2` (`a246ca01`, VERSION `v1.7.0_gg2`) and a worktree at the gg3 tip (`719c3ef1`, VERSION `v1.7.0_gg3`). The probe `Phase24RolloutProbe` (methods `describeDump` and `insertMatrix`) is scratch under `/tmp/gsd24/probe/`, copied untracked into each worktree, and was not committed.

Fixtures (44 tables, one fresh database per run): `d09_rNN_<style>` for the twelve roots r01..r12 and the three name styles `plain` (element `ab`), `bsn` (element name holding a literal backslash followed by `n`, DDL text `` `a\\nb` ``) and `lf` (element name holding a real line break, DDL text `` `a\nb` ``); four enum-label shapes `d09_e01..e04` (a backslash-n inside a label under `Nullable(Tuple)`, `Tuple` and `Array(Nullable(Tuple))`, and a label holding a comma); four `size` shapes `d09_s01..s04`. The twelve roots, with `n` the element under test: r01 `Tuple(n Int32, d String)`, r02 `Nullable(Tuple(n Int32, d String))`, r03 `Nullable(Tuple(n Nullable(Int32), d String))`, r04 `Array(Tuple(n Int32, d String))`, r05 `Array(Nullable(Tuple(n Int32, d String)))`, r06 `Map(String, Tuple(n Int32, d String))`, r07 `Map(String, Nullable(Tuple(n Int32, d String)))`, r08 `Tuple(x Tuple(n Int32, d String))`, r09 `Nullable(Tuple(x Tuple(n Int32, d String)))`, r10 `Tuple(x Nullable(Tuple(n Int32)))`, r11 `Tuple(a Array(Tuple(n Int32)))`, r12 `Tuple(n Int32, y Nullable(Tuple(c Int32)))`. Every table is `(id Int32, c <root>) ENGINE = MergeTree ORDER BY id`; no server setting is set.

```sh
cp /tmp/gsd24/probe/Phase24RolloutProbe.java /tmp/gsd24/wt-gg2/src/test/java/com/clickhouse/kafka/connect/sink/   # likewise wt-tip
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-gg2/gradlew -p /tmp/gsd24/wt-gg2 --console=plain test --tests '*Phase24RolloutProbe.describeDump' --tests '*Phase24RolloutProbe.insertMatrix' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-tip/gradlew -p /tmp/gsd24/wt-tip --console=plain test --tests '*Phase24RolloutProbe.describeDump' --tests '*Phase24RolloutProbe.insertMatrix' --rerun
cp <worktree>/build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.Phase24RolloutProbe.xml /tmp/gsd24/exec/d09-gg2.xml   # d09-gg3.xml for wt-tip
python3 -I /tmp/gsd24/probe/xml-probe-lines.py /tmp/gsd24/exec/d09-gg2.xml
```

Both XML files report `tests="2" skipped="0" failures="0" errors="0"`. Server `26.9.19.9` on both runs, client V2. In a PROBE line a backslash prints as two backslashes and a line break as `\n`, so a Column name that holds one backslash and an `n` prints as `a\\nb` and one that holds two backslashes and an `n` prints as `a\\\\nb`. The dump shows the connector's Column name equals the DDL text of the name in every style.

Result. All 44 tables were described by both releases (44 tables in each dump). The trees are identical for 30 of them: every `plain` table (all twelve roots), every non-`Nullable` root with a backslash-n or line-break name (r01, r04, r06, r08, r11 in both styles), the four enum-label shapes and the four `size` shapes. The trees differ for 14 tables, and for these only: the `bsn` and `lf` styles of r02, r03, r05, r07, r09, r10 and r12. In each of them the only difference is that gg2 has `isNullable = true` where gg3 has `false` (never the other way), on the nodes listed below. The plain `Nullable(Tuple)` root r02 differs for both name styles.

| table | root | gg2 fallback WARNs | gg3 fallback WARNs | tree nodes that flip (gg2 true, gg3 false) |
|---|---|---|---|---|
| `d09_r02_bsn` | `Nullable(Tuple(n Int32, d String))` | 1 | 0 | `c/c.a\\\\nb`, `c/c.d` |
| `d09_r02_lf` | `Nullable(Tuple(n Int32, d String))` | 1 | 0 | `c/c.a\\nb`, `c/c.d` |
| `d09_r03_bsn` | `Nullable(Tuple(n Nullable(Int32), d String))` | 1 | 0 | `c/c.d` |
| `d09_r03_lf` | `Nullable(Tuple(n Nullable(Int32), d String))` | 1 | 0 | `c/c.d` |
| `d09_r05_bsn` | `Array(Nullable(Tuple(n Int32, d String)))` | 1 | 0 | `c/[]/c.a\\\\nb`, `c/[]/c.d` |
| `d09_r05_lf` | `Array(Nullable(Tuple(n Int32, d String)))` | 1 | 0 | `c/[]/c.a\\nb`, `c/[]/c.d` |
| `d09_r07_bsn` | `Map(String, Nullable(Tuple(n Int32, d String)))` | 1 | 0 | `c/{}/c.values.a\\\\nb`, `c/{}/c.values.d` |
| `d09_r07_lf` | `Map(String, Nullable(Tuple(n Int32, d String)))` | 1 | 0 | `c/{}/c.values.a\\nb`, `c/{}/c.values.d` |
| `d09_r09_bsn` | `Nullable(Tuple(x Tuple(n Int32, d String)))` | 1 | 0 | `c/c.x`, `c/c.x/c.x.a\\\\nb`, `c/c.x/c.x.d` |
| `d09_r09_lf` | `Nullable(Tuple(x Tuple(n Int32, d String)))` | 1 | 0 | `c/c.x`, `c/c.x/c.x.a\\nb`, `c/c.x/c.x.d` |
| `d09_r10_bsn` | `Tuple(x Nullable(Tuple(n Int32)))` | 1 | 0 | `c/c.x/c.x.a\\\\nb` |
| `d09_r10_lf` | `Tuple(x Nullable(Tuple(n Int32)))` | 1 | 0 | `c/c.x/c.x.a\\nb` |
| `d09_r12_bsn` | `Tuple(n Int32, y Nullable(Tuple(c Int32)))` | 1 | 0 | `c/c.y/c.y.c` |
| `d09_r12_lf` | `Tuple(n Int32, y Nullable(Tuple(c Int32)))` | 1 | 0 | `c/c.y/c.y.c` |

WARN counts (the `DeclaredNullability` logger, per table): gg3 logged no `Could not take element nullability` WARN on any of the 44 tables. gg2 logged one on each of the 14 differing tables and also on the ten tables whose trees are identical (r01, r04, r06, r08, r11 in the `bsn` and `lf` styles; there the fallback keeps the flags DESCRIBE reported, which are already the declared ones, so no node moves); 24 in total. gg3 logged one `have no matching DESCRIBE sub-column` WARN on each of `d09_s01..s04` and nowhere else; gg2 has no such WARN. The differing tables are therefore exactly the ones where gg2's declared-type parse fell back and gg3's did not, restricted to roots that contain a `Nullable(Tuple)`.

Excerpt (r02, `bsn`): gg2 `tree:c nullable=true`, `tree:c/c.a\\\\nb nullable=true`, `tree:c/c.d nullable=true`; gg3 `tree:c nullable=true`, `tree:c/c.a\\\\nb nullable=false`, `tree:c/c.d nullable=false`. The element flags are doubled under gg2 (the root is `Nullable` and each element is also marked nullable), which is the cause of the byte difference measured in E-11. The full 451-line dumps are `/tmp/gsd24/exec/d09-gg2.lines` and `d09-gg3.lines`.

Research R3.2 expected exactly the `Nullable(Tuple)`-bearing roots and the 41 shapes of its own harness; this run matches that reading and adds r12 (`Tuple(n Int32, y Nullable(Tuple(c Int32)))`), where only the nested `y.c` node flips, and counts 24 gg2 fallback WARNs (research counted 23 on its shape set).

D09-DIFFERING=d09_r02_bsn,d09_r02_lf,d09_r03_bsn,d09_r03_lf,d09_r05_bsn,d09_r05_lf,d09_r07_bsn,d09_r07_lf,d09_r09_bsn,d09_r09_lf,d09_r10_bsn,d09_r10_lf,d09_r12_bsn,d09_r12_lf

Matches text: yes

### E-11: one live insert per shape, gg2 versus gg3 (D-09)

Backs: `docs/ROLLOUT-v1.7.0_gg3.md`, the in-flight batches item and the rollback item (written in plan 24-03). Same two worktrees, same server `26.9.19.9`, client V2, `exactlyOnce=false`, run by the `insertMatrix` method of the commands in E-10. For each of seven tables `d09i_<root>_<style>` with `(id Int32, c <root>)`, the table is created first, a `ClickHouseSinkTask` is started, one record is put (id 1, the element under test 7, `d` = `'x'`, the Kafka field for the element named exactly as the connector's Column name) and the row is read back with typed SQL (`SELECT id, toString(c)`) and compared inside the probe with the expected text. The expected text is `(7,'x')` for the Tuple roots, `[(7,'x')]` for the Array roots and `{'k':(7,'x')}` for the Map root.

| table | root | gg2 classes (`a246ca01`) | gg3 classes (`719c3ef1`) |
|-------|------|--------------------------|--------------------------|
| `d09i_r01_bsn` | `Tuple(n Int32, d String)`, literal backslash-n name | stored, read-back equals expected | stored, read-back equals expected |
| `d09i_r04_bsn` | `Array(Tuple(n Int32, d String))`, literal backslash-n name | stored, read-back equals expected | stored, read-back equals expected |
| `d09i_r02_plain` | `Nullable(Tuple(n Int32, d String))`, name `ab` | stored, read-back equals expected | stored, read-back equals expected |
| `d09i_r02_bsn` | `Nullable(Tuple(n Int32, d String))`, literal backslash-n name | **failed, code 33, 0 rows** | stored, read-back equals expected |
| `d09i_r05_bsn` | `Array(Nullable(Tuple(n Int32, d String)))`, literal backslash-n name | **failed, code 33, 0 rows** | stored, read-back equals expected |
| `d09i_r07_bsn` | `Map(String, Nullable(Tuple(n Int32, d String)))`, literal backslash-n name | **failed, code 33, 0 rows** | stored, read-back equals expected |
| `d09i_r02_lf` | `Nullable(Tuple(n Int32, d String))`, real line break in the name | **failed, code 33, 0 rows** | stored, read-back equals expected |

```text
PROBE-INSERT tree=gg2 table=d09i_r01_bsn outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg2 table=d09i_r02_bsn outcome=failed code=33 rows=0 match=-
PROBE-INSERT tree=gg2 table=d09i_r04_bsn outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg2 table=d09i_r05_bsn outcome=failed code=33 rows=0 match=-
PROBE-INSERT tree=gg2 table=d09i_r07_bsn outcome=failed code=33 rows=0 match=-
PROBE-INSERT tree=gg2 table=d09i_r02_lf outcome=failed code=33 rows=0 match=-
PROBE-INSERT tree=gg2 table=d09i_r02_plain outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg3 table=d09i_r01_bsn outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg3 table=d09i_r02_bsn outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg3 table=d09i_r04_bsn outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg3 table=d09i_r05_bsn outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg3 table=d09i_r07_bsn outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg3 table=d09i_r02_lf outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg3 table=d09i_r02_plain outcome=stored code=- rows=1 match=yes
```

Verdict per direction, worded from the measurements above (research R3.4 was the expected reading and it holds):

- **Upgrade, gg2 to gg3.** The trees differ only for `Nullable(Tuple)`-bearing roots with a backslash-n or line-break element name (E-10). For the four such shapes that were inserted (r02 in both styles, r05, r07) gg2 landed nothing: code 33 and 0 rows. A range of such a table that gg2 left in flight has therefore not stored a row, and the gg3 retry of the same range lands once with the declared bytes. The remaining differing roots (r03, r09, r10, r12) differ by the same flag doubling but were not inserted in this plan; their insert outcome is by analogy, not measured. A gg2 batch whose doubled null flags happened to parse and stored wrong rows was not measured either (the failing batches here all stopped with code 33).
- **Every other shape** (non-`Nullable` roots, plain names, enum labels, the `size` shapes) has identical trees on both releases and stored the record on both (E-11 controls r01, r04 and r02 with a plain name), so a batch of those shapes is byte for byte the same on both releases.
- **Rollback, gg3 to gg2:** measured in E-12.

Matches text: yes

### E-12: a gg2 retry of a range gg3 landed, differing shape versus control (D-15)

Backs: `docs/ROLLOUT-v1.7.0_gg3.md`, the rollback item and the in-flight item's shape-limited clean stop (written in plan 24-03). It is the measured basis for "stop cleanly before going back to `v1.7.0_gg2`, for tables of the differing shape". Method (`rollbackRetry`, run in the `v1.7.0_gg2` worktree only, server `26.9.19.9`, client V2): two tables `d15_r02_bsn` (the r02 shape `Nullable(Tuple(n Int32, d String))`, the element named with a literal backslash and `n`) and `d15_r02_plain` (the same shape, element `ab`), each `ENGINE = MergeTree ORDER BY id SETTINGS non_replicated_deduplication_window = 100`. For each table the correct row is inserted first as plain SQL under the token the gg3 connector would have sent for topic = table name, partition 0, offsets 0 to 0 (the connector's token is `<topic>-<partition>-<min>-<max>`, `QueryIdentifier.getDeduplicationToken`):

```sql
INSERT INTO `<db>`.`d15_r02_bsn` (id, c) SETTINGS insert_deduplication_token = 'd15_r02_bsn-0-0-0' VALUES (1, (7, 'x'))
```

Then a `v1.7.0_gg2` `ClickHouseSinkTask` (V2, `exactlyOnce=false`, which still sends the token) is started and the same record is put at partition 0, offset 0. The row the gg3 connector would have landed is therefore simulated by SQL under its token; the gg3 connector itself did not write it in this measurement.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-gg2/gradlew -p /tmp/gsd24/wt-gg2 --console=plain test --tests '*Phase24RolloutProbe.rollbackRetry' --rerun
cp /tmp/gsd24/wt-gg2/build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.Phase24RolloutProbe.xml /tmp/gsd24/exec/d15-gg2.xml
python3 -I /tmp/gsd24/probe/xml-probe-lines.py /tmp/gsd24/exec/d15-gg2.xml
```

```text
PROBE-VERSION tree=gg2 version=26.9.19.9
PROBE-RETRY tree=gg2 table=d15_r02_bsn first=landed outcome=failed code=33 rows=1
PROBE-RETRY tree=gg2 table=d15_r02_plain first=landed outcome=dropped code=- rows=1
```

(XML: `tests="1" skipped="0" failures="0" errors="0"`.) Result: for the differing shape the gg2 retry of the range fails with server code 33 and leaves exactly the one correct row; the control (plain element name, identical bytes under both releases, E-11) is dropped by the token, also with one row. So under `v1.7.0_gg2` the retry of a range of a differing-shape table is not deduplicated away: the server rejects the bytes first (E-11) and the connector surfaces the failure, even though the rows had landed. That is the one shape where a clean stop (no range of the table in `BEFORE_PROCESSING`) matters before a rollback; the check is the query of E-14, which names topics, not shapes, and the shape is found with the query of E-13.

Matches text: yes

### E-13: the query for the differing shape (D-15)

Backs: `docs/ROLLOUT-v1.7.0_gg3.md`, the in-flight item (which tables are of the shape that encodes differently) and the rollback item (written in plan 24-03). Doc-form query, one read-only `SELECT` over `system.columns`, `<db>` is its only placeholder (`/tmp/gsd24/probe/sql/escaped-nullable-tuple.sql`). It strips single-quoted enum labels first, then lists a column when its declared type text contains `Nullable(Tuple(` and a backtick-quoted name that holds a backslash followed by `n`. In the text `system.columns` shows, a name with a literal backslash-n prints as `\\n` and a name with a real line break prints as `\n`, so both styles of E-10 match; a name with a single backslash, a trailing backslash or a tab (printed `\\`, `\\` at the end and `\t`) does not.

```sql
SELECT `table`, name AS `column`, type
FROM system.columns
WHERE database = '<db>'
  AND position(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''), 'Nullable(Tuple(') > 0
  AND match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''), '`[^`]*\\\\n[^`]*`')
ORDER BY `table`;
```

Substitution map: `<db>` = the fixture database of that run (`<test database>_sq`, 44 `d09_*` tables of E-10) and, for the second run, a database holding only look-alikes (`<test database>_sql`: every `_plain` table, the non-`Nullable` roots r01, r04, r06, r08, r11 in all three name styles, and the four enum-label shapes). Run in the gg3 worktree (the method `shapeQuery`), same server `26.9.19.9`:

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-tip/gradlew -p /tmp/gsd24/wt-tip --console=plain test --tests '*Phase24RolloutProbe.shapeQuery' --tests '*Phase24RolloutProbe.keeperSelfCheck' --rerun
cp /tmp/gsd24/wt-tip/build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.Phase24RolloutProbe.xml /tmp/gsd24/exec/d15-gg3.xml
```

```text
PROBE-SHAPEQ tree=gg3 db=fixture tables=d09_r02_bsn,d09_r02_lf,d09_r03_bsn,d09_r03_lf,d09_r05_bsn,d09_r05_lf,d09_r07_bsn,d09_r07_lf,d09_r09_bsn,d09_r09_lf,d09_r10_bsn,d09_r10_lf,d09_r12_bsn,d09_r12_lf
PROBE-SHAPEQ tree=gg3 db=lookalike tables=
```

(XML: `tests="2" skipped="0" failures="0" errors="0"`.) The fixture result is exactly the 14 tables of `D09-DIFFERING` (E-10), no plain-name, non-`Nullable`, enum-label or `size` table is listed, and the look-alike-only database returns an empty list, which is what the note's "Empty result" sentence promises.

SHAPEQ-OVERFLAG=

Why the predicate says "backslash followed by `n`" and not "a backslash". The plan's first reading was "a backtick-quoted name holding a backslash" (the broad variant, `/tmp/gsd24/probe/sql/escaped-nullable-tuple-broad.sql`, which differs in one line: the regex `` `[^`]*\\\\[^`]*` `` in place of `` `[^`]*\\\\n[^`]*` ``). A supplementary run outside the plan's fixture set (method `shapeExtra`, both trees, XML `/tmp/gsd24/exec/d15x-gg2.xml` and `d15x-gg3.xml`, each `tests="1" skipped="0" failures="0" errors="0"`) put `c Nullable(Tuple(<name> Int32, d String))` over more names. The two releases' trees differ only where the declared type text holds a backslash directly followed by `n`: the `normalise` of `v1.7.0_gg2` replaces every such pair by a space, and that of `v1.7.0_gg3` leaves a pair inside a quoted name alone. The measurements:

| table | element name | trees gg2 vs gg3 | gg2 insert | gg3 insert | broad query | query above |
|-------|--------------|------------------|------------|------------|-------------|-------------|
| `x_bs1` | one backslash (`a\b`) | identical | stored | stored | lists | not listed |
| `x_bs2` | trailing backslash (`e\`) | identical | stored | stored | lists | not listed |
| `x_tab` | tab | identical | stored | stored | lists | not listed |
| `x_bst` | literal backslash and `t` | identical | stored | stored | lists | not listed |
| `x_sp` | space (`a b`) | identical | stored | stored | not listed | not listed |
| `x_bbn` | two backslashes and `n` | **differ** | **failed, code 33, 0 rows** | stored | lists | lists |
| `x_nl` | backslash-n on a non-`Nullable` root with `Nullable` leaves | identical | stored | stored | n/a (no `Nullable(Tuple(`) | not listed |
| `x_sl` | slash (`a/b`) | identical | **failed, code 33, 0 rows** | **failed, code 33, 0 rows** | not listed | not listed |

```text
PROBE-SHAPEQX tree=gg3 tables=x_bbn
PROBE-SHAPEQX-BROAD tree=gg3 tables=x_bbn,x_bs1,x_bs2,x_bst,x_tab
```

The broad variant would have flagged four tables whose bytes are the same on both releases. The query above is the one kept. The slash row is a different known limit (E-16): a `Nullable(Tuple)` with a slash in an element name fails on both releases, so it is not a rollback difference and this query does not list it.

Matches text: yes

### E-14: KeeperMap self-check on a gg3-created state table (D-15)

Backs: `docs/ROLLOUT-v1.7.0_gg3.md`, the rollback item's clean-stop check (written in plan 24-03). It is the query of item 1 of `docs/ROLLOUT-v1.7.0_gg2.md`, copied verbatim, now run against a state table that the gg3 connector's own code creates. Method (`keeperSelfCheck`, gg3 worktree, server `26.9.19.9`): `new KeeperStateProvider(new ClickHouseSinkConfig(props))` with `zkPath` `/kafka-connect` and `zkDatabase` `connect_state` against the server's embedded Keeper creates the table; the provider then writes three state records: `orders` partition 0 `AFTER_PROCESSING` (offsets 0 to 9), `orders` partition 1 `BEFORE_PROCESSING` (10 to 19) and `orders-v2` partition 0 `BEFORE_PROCESSING` (0 to 4). The query runs for topic `orders`; then `orders` partition 1 is set to `AFTER_PROCESSING` and the query runs again.

```sql
SELECT `key`, minOffset, maxOffset, state
FROM `<zkDatabase>`
WHERE state != 'AFTER_PROCESSING'
  AND arrayStringConcat(arrayPopBack(splitByChar('-', `key`)), '-') IN ('<topic>', '<topic>')
ORDER BY `key`;
```

Substitution map: `<zkDatabase>` = `connect_state`, `<topic>` = `orders` (both occurrences). The query runs in the database the state table lives in (the probe's connection database). Commands as in E-13 (the same Gradle run, method `keeperSelfCheck`).

```text
PROBE-KEEPER case=in-flight keys=orders-1
PROBE-KEEPER case=all-after keys=
```

Result: the first run returns exactly the one in-flight key of the listed topic, `orders-1`; it does not return `orders-0` (already `AFTER_PROCESSING`) and does not return `orders-v2-0`, which is `BEFORE_PROCESSING` under a topic that merely shares the prefix. The second run returns nothing. Same result as Phase 20's E-17 on the gg2 classes, now on the gg3 classes.

Matches text: yes

### E-15: Tuple elements named size, the WARN oracle and the suspects query (D-01, D-12)

Backs: `docs/ROLLOUT-v1.7.0_gg3.md`, the item for a Tuple element named `size` or `size0` (the unpaired-element WARN of ledger row L-51), `docs/FORK-DELTA.md` Known-costs item (backlog 999.4) and the CHANGELOG known-issue line (written in plans 24-03 and 24-04). Server `26.9.19.9`, client V2, run in both worktrees (methods `sizeQuery` and `escapeNames` in one Gradle run each, XML `/tmp/gsd24/exec/q-gg3.xml` for the gg3 tip and `q-gg2.xml` for tag `v1.7.0_gg2`; both `tests="2" skipped="0" failures="0" errors="0"`).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-tip/gradlew -p /tmp/gsd24/wt-tip --console=plain test --tests '*Phase24RolloutProbe.sizeQuery' --tests '*Phase24RolloutProbe.escapeNames' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd24/wt-gg2/gradlew -p /tmp/gsd24/wt-gg2 --console=plain test --tests '*Phase24RolloutProbe.sizeQuery' --tests '*Phase24RolloutProbe.escapeNames' --rerun
python3 -I /tmp/gsd24/probe/xml-probe-lines.py /tmp/gsd24/exec/q-gg3.xml
```

Fixtures (every table `(id Int32, c <type>)` unless noted, `ENGINE = MergeTree ORDER BY id`). Expected affected, thirteen tables: `s4_a01` `Tuple(size UInt16, v String)`; `s4_a02` `Tuple(size0 UInt8, v String)`; `s4_a03` `Nullable(Tuple(size UInt8, v String))`; `s4_a04` `Array(Tuple(size UInt16, v String))`; `s4_a05` `Map(String, Tuple(size UInt8, v String))`; `s4_a06` `Tuple(x Tuple(size UInt8, v String))`; `s4_a07` `Tuple(x Array(Tuple(size0 UInt8)))`; `s4_a08` `Array(Tuple(size1 UInt16, v String))`; `s4_a09` `Array(Tuple(size12 UInt8, v String))`; `s4_a10` `Array(Array(Tuple(size1 UInt8, v String)))`; `s4_a11` `Tuple(x Array(Tuple(size1 UInt8, v String)))`; `s4_a12` `Array(Tuple(x Tuple(size1 UInt8, v String)))`; `s4_a13` `Array(Tuple(v String, size1 UInt16))`. Expected unaffected, nineteen look-alikes: `s4_u01` `Tuple(size1 UInt8, v String)`; `s4_u02` `Nullable(Tuple(size1 UInt8, v String))`; `s4_u03` `Map(String, Tuple(size1 UInt8, v String))`; `s4_u04` `Tuple(a Array(UInt8), size1 UInt8)`; `s4_u05` `Tuple(sizes UInt8, v String)`; `s4_u06` `Tuple(size_x UInt8, v String)`; `s4_u07` `Tuple(size_1 UInt8, v String)`; `s4_u08` `Tuple(sizeA UInt8, v String)`; `s4_u09` `Tuple(Size UInt8, v String)`; `s4_u10` `Tuple(xsize UInt8, v String)`; `s4_u11` a Tuple element named `my size` in backticks; `s4_u12` a plain column `size UInt8`; `s4_u13` a plain column `size0 UInt8`; `s4_u14` a column `size Array(UInt8)`; `s4_u15` `Nested(size UInt8, v String)`; `s4_u16` `Enum8('size' = 1, 'size0' = 2)`; `s4_u17` `Array(Tuple(e Enum8('x, size1 UInt8' = 1), v String))`; `s4_u18` `Tuple(e Enum8('x, size UInt8' = 1), v String)`; `s4_u19` an unnamed `Tuple(UInt8, String)`. The known over-flag: `s4_o01` `Array(Map(String, Tuple(size1 UInt8)))`.

The oracle is the connector's own WARN: the tables whose `describeTable` logs `have no matching DESCRIBE sub-column` on the gg3 tip. The suspects query (`/tmp/gsd24/probe/sql/size-suspects.sql`, one read-only `SELECT` over `system.columns`, `<db>` its only placeholder):

```sql
SELECT `table`, name AS `column`, type
FROM system.columns
WHERE database = '<db>'
  AND (
        match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''), '[(,] ?size0? [A-Za-z]')
        OR match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''),
                 'Array\\((?:[^()]|\\((?:[^()]|\\((?:[^()]|\\([^()]*\\))*\\))*\\)|\\()*?[(,] ?size[0-9]+ [A-Za-z]')
      )
ORDER BY `table`;
```

Substitution map: `<db>` = the fixture database of that run (`<test database>_sf`: the 13 affected, the 19 look-alikes and `s4_o01`) and, for the second run, a database holding only the 19 look-alikes (`<test database>_su`).

```text
PROBE-SIZE-ORACLE tree=gg3 tables=s4_a01,s4_a02,s4_a03,s4_a04,s4_a05,s4_a06,s4_a07,s4_a08,s4_a09,s4_a10,s4_a11,s4_a12,s4_a13
PROBE-SIZE-ORACLE tree=gg2 tables=
PROBE-SIZE-QUERY tree=gg3 db=fixture tables=s4_a01,s4_a02,s4_a03,s4_a04,s4_a05,s4_a06,s4_a07,s4_a08,s4_a09,s4_a10,s4_a11,s4_a12,s4_a13,s4_o01
PROBE-SIZE-QUERY tree=gg3 db=lookalike tables=
PROBE-SIZE-QUERY tree=gg2 db=fixture tables=s4_a01,s4_a02,s4_a03,s4_a04,s4_a05,s4_a06,s4_a07,s4_a08,s4_a09,s4_a10,s4_a11,s4_a12,s4_a13,s4_o01
PROBE-SIZE-QUERY tree=gg2 db=lookalike tables=
```

Result. The WARN fires on exactly the thirteen affected tables and on none of the nineteen look-alikes or on `s4_o01`. The query lists the thirteen plus `s4_o01`, a known over-flag: `Array(Map(String, Tuple(size1 UInt8)))` has `size1` after an `Array(` in the type text, but the connector does not drop that element and the table does not log the WARN. It lists no look-alike, and returns an empty list on the database that holds only look-alikes, which is what the note's "Empty result" sentence promises. The query reads the declared type text, so a listed table is a suspect; the log line is the exact check, because it comes from the same code path as the insert. `v1.7.0_gg2` has no such WARN (its oracle is empty) and its query result is the same, so the defect itself is older than the WARN: the WARN is what gg3 adds.

Captured WARN line (gg3, a line break in the declared type prints as `\n`; the greppable phrase is `have no matching DESCRIBE sub-column`), plan 24-03 builds its log-grep proof from it:

```text
PROBE-SIZE-WARN tree=gg3 line=Declared Tuple element(s) [c.size] of column c of table kafka_connect_test_1278298601_1792514361103_sf.s4_a01 (declared type Tuple(\\n    size UInt16,\\n    v String)) have no matching DESCRIBE sub-column; applying the declared nullability to the elements that paired
```

Inserts (one record of constants, `put()` through a V2 task, typed read-back; identical on both trees):

```text
PROBE-INSERT tree=gg3 table=s4_a01 outcome=failed code=32 rows=0 match=-
PROBE-INSERT tree=gg3 table=s4_a04 outcome=failed code=32 rows=0 match=-
PROBE-INSERT tree=gg3 table=s4_a08 outcome=failed code=32 rows=0 match=-
PROBE-INSERT tree=gg3 table=s4_a13 outcome=failed code=33 rows=0 match=-
PROBE-INSERT tree=gg3 table=s4_u01 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg3 table=s4_u03 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg3 table=s4_u05 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg2 table=s4_a01 outcome=failed code=32 rows=0 match=-
PROBE-INSERT tree=gg2 table=s4_a04 outcome=failed code=32 rows=0 match=-
PROBE-INSERT tree=gg2 table=s4_a08 outcome=failed code=32 rows=0 match=-
PROBE-INSERT tree=gg2 table=s4_a13 outcome=failed code=33 rows=0 match=-
PROBE-INSERT tree=gg2 table=s4_u01 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg2 table=s4_u03 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg2 table=s4_u05 outcome=stored code=- rows=1 match=yes
PROBE-SIZE-NULL tree=gg3 code=36
PROBE-SIZE-NULL tree=gg2 code=36
```

Inserts into the affected shapes fail with code 32 (`ATTEMPT_TO_READ_AFTER_EOF`) or 33 (`CANNOT_READ_ALL_DATA`) and store 0 rows, on both releases; `Tuple(size1 ...)`, `Map(String, Tuple(size1 ...))` and `Tuple(sizes ...)` store their row. A Tuple element named `null` is rejected by the server at `CREATE TABLE` with code 36, so no such table exists to be affected. A silently wrong stored value, reachable when the shorter bytes happen to parse (research, 23-EVIDENCE.md R-01), was not measured here.

Matches text: yes

### E-16: names with a backslash, a line break, a slash, a tab or a double quote (D-05, D-13)

Backs: `docs/ROLLOUT-v1.7.0_gg3.md` (the known-limitation paragraph beside the backslash-n item) and `docs/FORK-DELTA.md` Known-costs item (backlog 999.5), written in plans 24-03 and 24-04. Same runs and commands as E-15 (method `escapeNames`). Fixtures in the database `<test database>_e5`: roots `e5_e01` a name with one backslash (`a\b`), `e5_e02` a literal backslash followed by `n`, `e5_e03` a real line break, `e5_e04` `a/b`, `e5_e05` a tab, each `(id Int32, <name> String DEFAULT 'DEFAULTED')`; elements `e5_e06` .. `e5_e10` the same five characters as the first element name in `c Tuple(<name> Int32, d String)`; `e5_e11` a root name holding a double quote. Look-alikes (also in the look-alike-only database `<test database>_e5l`): `e5_l01` a root named `a.b`, `e5_l02` `Tuple(e Enum8('a\b' = 1), d String)`, `e5_l03` `s String DEFAULT 'a\b'`, `e5_l04` a backslash only in the column COMMENT, `e5_l05` roots named `\u00e9` and `\u65e5\u672c` (non-ASCII), `e5_l06` plain names, `e5_l07` `Tuple(e Enum8('a/b' = 1), d String)`, `e5_l08` `s String DEFAULT 'x/y'`.

What the connector calls a column. Equality of a name is defined by the connector's Column name, which is the server's name with every JSON escape doubled (`fromJsonRow` doubles each backslash and each line break before parsing). Measured per character class; in the Column name column, a backslash is a real backslash:

| character in the server name | server name | connector Column name | record field `natural` (the server name) | record field `escaped` (the Column name) |
|------------------------------|-------------|-----------------------|-------------------------------------------|-------------------------------------------|
| backslash | `a\b` | `a\\b` (two backslashes) | stores the column DEFAULT, no error | stores the value |
| backslash and `n` | `a\nb` (backslash, `n`) | `a\\nb` (two backslashes, `n`) | stores the column DEFAULT, no error | stores the value |
| line break | `a`, LF, `b` | `a\nb` (backslash, `n`) | stores the column DEFAULT, no error | stores the value |
| slash | `a/b` | `a\/b` | stores the column DEFAULT, no error | stores the value |
| tab | `a`, TAB, `b` | `a\tb` (backslash, `t`) | stores the column DEFAULT, no error | stores the value |
| double quote | `a"b` | none: the table is not mapped (`describeTable` returns no table) | n/a | n/a |
| non-ASCII (`\u00e9`, `\u65e5\u672c`), space, dot | unchanged | same as the server name | n/a | n/a |

The rows for the first five classes are the root-name fixtures `e5_e01`..`e5_e05`, and the result is identical on `v1.7.0_gg2` and on the gg3 tip. For an element name (`e5_e09`, a slash inside a Tuple) the natural field name makes the put fail (`failed`) instead of storing a default, and the Column-name field stores the value, again identical on both trees. The set of Column names (34 names over the mapped fixture tables, listed by `PROBE-ESC-NAME`) is the same on both trees. The root-name rows are the silent ones: a record whose field uses the server's name loses that field to the DEFAULT without any error.

```text
PROBE-ESC-NAME tree=gg3 table=e5_e01 column=a\\\\b
PROBE-ESC-NAME tree=gg3 table=e5_e02 column=a\\\\nb
PROBE-ESC-NAME tree=gg3 table=e5_e03 column=a\\nb
PROBE-ESC-NAME tree=gg3 table=e5_e04 column=a\\/b
PROBE-ESC-NAME tree=gg3 table=e5_e05 column=a\\tb
PROBE-ESC-NAME tree=gg3 table=e5_e09 column=c.a\\/b
PROBE-ESC-UNMAPPED tree=gg3 table=e5_e11 mapped=no
PROBE-ESC-UNMAPPED tree=gg3 table=e5_l06 mapped=yes
PROBE-ESC-PUT tree=gg3 table=e5_e01 field=natural stored=default
PROBE-ESC-PUT tree=gg3 table=e5_e01 field=escaped stored=value
PROBE-ESC-PUT tree=gg3 table=e5_e04 field=natural stored=default
PROBE-ESC-PUT tree=gg3 table=e5_e04 field=escaped stored=value
PROBE-ESC-PUT tree=gg3 table=e5_e09 field=natural stored=failed
PROBE-ESC-PUT tree=gg3 table=e5_e09 field=escaped stored=value
```

(In a PROBE line a backslash prints as two backslashes, so `a\\\\b` is the Column name `a\\b` with two backslashes. The same lines with `tree=gg2` are in `q-gg2.lines`; the whole 34-name list and the ten put lines are equal on both trees.)

The detection query (`/tmp/gsd24/probe/sql/escaped-names.sql`, one read-only `SELECT` over `system.columns`, `<db>` its only placeholder). It lists a table when a root column name holds a backslash, a line break, a slash or a tab, or when a backtick-quoted element name in the declared type holds a backslash or a slash (the type text prints a backslash, a line break and a tab as a backslash escape, so the backslash test covers an element name with a line break or a tab). Single-quoted enum labels are stripped first. The `/` and tab predicates were kept because this run showed them exact: no look-alike is listed.

```sql
SELECT `table`, name AS `column`, type
FROM system.columns
WHERE database = '<db>'
  AND (
        position(name, '\\') > 0
        OR position(name, '\n') > 0
        OR position(name, '/') > 0
        OR position(name, '\t') > 0
        OR match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''), '`[^`]*\\\\[^`]*`')
        OR match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''), '`[^`]*/[^`]*`')
      )
ORDER BY `table`;
```

Substitution map: `<db>` = the fixture database (`<test database>_e5`, 11 `e5_e*` tables and the 8 look-alikes) and, for the second run, the look-alike-only database (`<test database>_e5l`).

```text
PROBE-ESC-QUERY tree=gg3 db=fixture tables=e5_e01,e5_e02,e5_e03,e5_e04,e5_e05,e5_e06,e5_e07,e5_e08,e5_e09,e5_e10
PROBE-ESC-QUERY tree=gg3 db=lookalike tables=
PROBE-ESC-QUERY tree=gg2 db=fixture tables=e5_e01,e5_e02,e5_e03,e5_e04,e5_e05,e5_e06,e5_e07,e5_e08,e5_e09,e5_e10
PROBE-ESC-QUERY tree=gg2 db=lookalike tables=
```

Result: exactly the ten backslash, line-break, slash and tab fixtures, none of the look-alikes, and an empty list on the look-alike-only database ("Empty result" in the note). The double-quote table `e5_e11` is not listed and is not meant to be: the query reads names, and that table is not a name-equality case but an unmapped one (`describeTable` returns no table on both releases). A root or Tuple with a slash in an element name is listed (`e5_e04`, `e5_e09`). A slash element name under a `Nullable(Tuple)` was not run through this query, but E-13 (row `x_sl`) shows that such a table fails with code 33 on both releases, because the declared-type pairing falls back; that is the same name-equality limit seen from the nullability side.

Matches text: yes

### E-20: connector-config self-check for the JSON-as-string route (D-14)

Backs: `docs/ROLLOUT-v1.7.0_gg3.md`, item 1 (the first "Am I affected?" check, written in plan 24-03). The command lists the connector configuration files that mention the option the exception of E-03 depends on. It uses `-F` so the name needs no escaping, and it names the option itself, not the flag the connector derives from it (`ClickHouseSinkConfig.java:355-356`; there is no connector key of that derived name).

Scratch directory `/tmp/gsd24/conncfg/` (never committed, constants only): `connector-a.properties` with `clickhouseSettings=input_format_binary_read_json_as_string=1`, `connector-b.json` with `"clickhouseSettings": "input_format_binary_read_json_as_string=true"` and `connector-c.json` with `"clickhouseSettings": "async_insert=0"`. `/tmp/gsd24/conncfg-empty/` holds only `connector-c.json`.

Doc-form command (the note quotes exactly this, placeholder included):

```sh
grep -rnF 'input_format_binary_read_json_as_string' <connector-config-directory>
```

Substitution: `<connector-config-directory>` = `/tmp/gsd24/conncfg` and `/tmp/gsd24/conncfg-empty` for the two runs; nothing else changed. Date (UTC) 2026-10-20, macOS BSD `grep`.

```text
$ grep -rnF 'input_format_binary_read_json_as_string' /tmp/gsd24/conncfg
/tmp/gsd24/conncfg/connector-a.properties:3:clickhouseSettings=input_format_binary_read_json_as_string=1
/tmp/gsd24/conncfg/connector-b.json:3:  "clickhouseSettings": "input_format_binary_read_json_as_string=true"
exit=0
$ grep -rnF 'input_format_binary_read_json_as_string' /tmp/gsd24/conncfg-empty
exit=1
```

Result: both files that set the option are listed, in either spelling of the value, and the unrelated file is not. The directory that holds only the unrelated file prints nothing and exits 1, which is what an operator sees when no connector configuration mentions the option. The search sees files only, which the note says.

Matches text: yes

### E-21: alert-rule grep for library messages (L-42, L-45)

Backs: `docs/ROLLOUT-v1.7.0_gg3.md`, item 1 (the second "Am I affected?" check, written in plan 24-03). The command lists matchers in `<alert-rule-directory>` that use one of the two library message fragments the writer no longer lets through for a value-conversion failure (`Invalid UUID string`, `For input string`). It uses `-F` and fragments without quotes or colons, so the search does not depend on a matcher's own quoting.

Scratch directory `/tmp/gsd24/alerts/` (never committed): `rule-uuid.yml` with a matcher `log_message =~ "Invalid UUID string: .*"`, `rule-number.yml` with `log_message =~ "For input string: .*"` and `rule-unrelated.yml` (`kafka_consumergroup_lag > 1000`). `/tmp/gsd24/alerts-empty/` holds only the unrelated file. `/tmp/gsd24/alerts-escaped/` holds one matcher that writes the spaces of the first text as a regular-expression escape (`Invalid\\sUUID\\sstring` in the rule file, which is `Invalid\sUUID\sstring` once the YAML string is read).

Doc-form command (the note quotes exactly this, placeholder included):

```sh
grep -rnF -e 'Invalid UUID string' -e 'For input string' <alert-rule-directory>
```

Substitution: `<alert-rule-directory>` = `/tmp/gsd24/alerts`, `/tmp/gsd24/alerts-empty` and `/tmp/gsd24/alerts-escaped` for the three runs; nothing else changed. Date (UTC) 2026-10-20, macOS BSD `grep`.

```text
$ grep -rnF -e 'Invalid UUID string' -e 'For input string' /tmp/gsd24/alerts
/tmp/gsd24/alerts/rule-number.yml:2:  expr: log_message =~ "For input string: .*"
/tmp/gsd24/alerts/rule-uuid.yml:2:  expr: log_message =~ "Invalid UUID string: .*"
exit=0
$ grep -rnF -e 'Invalid UUID string' -e 'For input string' /tmp/gsd24/alerts-empty
exit=1
$ grep -rnF -e 'Invalid UUID string' -e 'For input string' /tmp/gsd24/alerts-escaped
exit=1
```

Result: each old text is listed once and the unrelated file is not. The directory holding only the unrelated file prints nothing and exits 1, which is what an operator sees when no matcher uses an old text. A matcher that escapes the spaces is not listed, because `-F` compares literal text; the note says so.

Matches text: yes

### E-22: item 7 rollback on a dry-run merge of the tip

Backs: `docs/ROLLOUT-v1.7.0_gg3.md`, item 7 (the rollback): `git fetch --tags origin`, `git checkout custom_build`, `git merge --ff-only origin/custom_build`, `git status --porcelain` printing nothing, `git read-tree -u --reset pre-v1.7.0_gg3`, `git commit -m "Roll back v1.7.0_gg3"`, `git diff --quiet HEAD pre-v1.7.0_gg3 && echo ok`, `git revert <rollback-commit>` and `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg3.md` (written in plan 24-03). Date (UTC) 2026-10-20.

Tag `pre-v1.7.0_gg3` is a local lightweight tag on `6473e24f372a8b37dfd8cc8cf3124e6cff52edca` (`origin/custom_build`, the pre-merge tip: the published `v1.7.0_gg2` tree plus the three dependency updates); its `tests.yaml` has the trigger `branches: ['**']` (a branch-push trigger only) and `git ls-remote --tags origin` does not list the tag. X is the tip that contains the finished frame, item 1 and item 7 of the note: `X = 1ea720ede4b0e04abc6b89fdb338288be8912ffd` (the commit of plan 24-03 Task 1 that added the note). Throwaway merge: `git worktree add --detach /tmp/gsd24/dry 6473e24f372a8b37dfd8cc8cf3124e6cff52edca`, then in it `git -c user.name=demo -c user.email=demo@example.invalid merge --no-ff --no-edit X`, giving the merge commit `D = 02e0e5f057409cc15cfd2169496c928b05bfd746`. D has two parents, `D^1` is `6473e24f`, `D^2` is X, and `git diff --quiet D X` holds (no non-merge difference), which printed `MERGE-DRY-RUN-OK`. D was never pushed. Plans 24-06 and 24-07 re-run the script on the publication tip and on the real merge commit, which have a different tip from X (items 2 to 6 of the note are written after X). The dry-run worktree was removed and pruned afterwards.

The script lives outside the repository (`/tmp/gsd24/rollback-demo.sh <repo> <target> <worktree>`), adapted from Phase 20's E-31. A throwaway branch that starts at `pre-v1.7.0_gg3` stands in for a stale local `custom_build`, so `git checkout custom_build` is the one command not run verbatim (a linked worktree cannot check out `custom_build` itself); the fast-forward target D stands in for `origin/custom_build`. The `-c user.name/user.email` options only give the throwaway commits an identity. Deviation from a published tag: `git fetch --tags origin` ran while the tag existed only locally, and it kept the local tag (the script prints it after the fetch).

```sh
#!/bin/bash
# usage: rollback-demo.sh <repo> <target-tip> <worktree-path>
# Runs item 7 as written, on a throwaway branch standing in for a stale local custom_build
# (starts at pre-v1.7.0_gg3, then fast-forwards to <target-tip>, the stand-in for origin/custom_build).
set -u
REPO=$1; TARGET=$2; WT=$3; BR=gg3-rollback-demo-$$
ID=(-c user.name=demo -c user.email=demo@example.invalid)
git -C "$REPO" worktree add --quiet -b "$BR" "$WT" 'pre-v1.7.0_gg3^{commit}'
g() { git -C "$WT" "$@"; }
TGT=$(git -C "$REPO" rev-parse "$TARGET")
echo "target=$TGT stale-start=$(g rev-parse --short HEAD)"
echo "x" >> "$WT/README.md"; echo "dirty: status --porcelain lines=$(g status --porcelain | wc -l | tr -d ' ')"; g checkout -q -- README.md
g fetch -q --tags origin && echo "fetch --tags exit=0"
echo "local tag kept after fetch: $(g rev-parse --short 'pre-v1.7.0_gg3^{commit}')"
g merge -q --ff-only "$TARGET" && echo "merge --ff-only exit=0, HEAD==target: $([ "$(g rev-parse HEAD)" = "$TGT" ] && echo yes)"
echo "clean: status --porcelain lines=$(g status --porcelain | wc -l | tr -d ' ')"
g read-tree -u --reset pre-v1.7.0_gg3 && g "${ID[@]}" commit -q -m "Roll back v1.7.0_gg3" && echo "read-tree + commit exit=0"
RB=$(g rev-parse HEAD); echo "rollback-commit parent==target: $([ "$(g rev-parse HEAD^)" = "$TGT" ] && echo yes)"
g diff --quiet HEAD pre-v1.7.0_gg3 && echo ok
[ -z "$(g status --porcelain)" ] && echo "worktree clean after commit"
g ls-files --error-unmatch docs/ROLLOUT-v1.7.0_gg3.md >/dev/null 2>&1 || echo "rollout doc gone from branch"
g show "$RB^:docs/ROLLOUT-v1.7.0_gg3.md" | grep -c '^## Item 7: Rollback' | sed 's/^/git show <rollback-commit>^:docs\/ROLLOUT... item-7 headings: /'
g show HEAD:.github/workflows/tests.yaml | grep -qF "branches: ['**']" && echo "rolled-back tests.yaml keeps branches: ['**']"
g "${ID[@]}" revert --no-edit "$RB" >/dev/null && g diff --quiet HEAD "$TARGET" && echo "after git revert <rollback-commit>: tree(HEAD)==tree(target)"
git -C "$REPO" worktree remove --force "$WT" && git -C "$REPO" worktree prune && git -C "$REPO" branch -q -D "$BR"
```

```text
X=1ea720ede4b0e04abc6b89fdb338288be8912ffd
D=02e0e5f057409cc15cfd2169496c928b05bfd746
MERGE-DRY-RUN-OK
target=02e0e5f057409cc15cfd2169496c928b05bfd746 stale-start=6473e24f
dirty: status --porcelain lines=1
fetch --tags exit=0
local tag kept after fetch: 6473e24f
merge --ff-only exit=0, HEAD==target: yes
clean: status --porcelain lines=0
read-tree + commit exit=0
rollback-commit parent==target: yes
ok
worktree clean after commit
rollout doc gone from branch
git show <rollback-commit>^:docs/ROLLOUT... item-7 headings: 1
rolled-back tests.yaml keeps branches: ['**']
after git revert <rollback-commit>: tree(HEAD)==tree(target)
```

Afterwards `git worktree list` shows only the main working tree, no `gg3-rollback-demo-*` branch is left, and `git ls-remote --tags origin` lists no `pre-v1.7.0_gg3`.

Matches text: yes

### E-23: log grep for the unpaired-element WARN (L-51)

Backs: `docs/ROLLOUT-v1.7.0_gg3.md`, item 2 (the exact check, written in plan 24-03). The command lists the lines of a connector log that carry the greppable phrase of the unpaired-element WARN. The scratch log `/tmp/gsd24/logs/connect.log` (never committed, constants only) holds the WARN line captured in E-15 (`PROBE-SIZE-WARN`, with the probe's doubled backslash printed once, as a log appender prints a line break in the declared type) between two unrelated lines; `/tmp/gsd24/logs-empty/connect.log` holds only the two unrelated lines.

Doc-form command (the note quotes exactly this, placeholder included):

```sh
grep -F 'have no matching DESCRIBE sub-column' <connector-log-file>
```

Substitution: `<connector-log-file>` = `/tmp/gsd24/logs/connect.log` and `/tmp/gsd24/logs-empty/connect.log` for the two runs; nothing else changed. Date (UTC) 2026-10-20, macOS BSD `grep`.

```text
$ grep -F 'have no matching DESCRIBE sub-column' /tmp/gsd24/logs/connect.log
2026-10-20 19:00:00,002 WARN  [task-thread-0] DeclaredNullability - Declared Tuple element(s) [c.size] of column c of table kafka_connect_test_1278298601_1792514361103_sf.s4_a01 (declared type Tuple(\n    size UInt16,\n    v String)) have no matching DESCRIBE sub-column; applying the declared nullability to the elements that paired
exit=0
$ grep -F 'have no matching DESCRIBE sub-column' /tmp/gsd24/logs-empty/connect.log
exit=1
```

Result: the captured WARN line is printed and the two unrelated lines are not. The file with only unrelated lines prints nothing and exits 1, which is what an operator sees when no table has an unpaired element. The oracle behind the phrase is E-15: the WARN fired on exactly the thirteen affected tables of that run and on none of the nineteen look-alikes.

Matches text: yes

### E-30: CHANGELOG section check

Backs: the `# 1.7.0_gg3, 2026-10-20` section of `CHANGELOG.md` (plan 24-04, task 1). Date used for the heading (UTC): 2026-10-20, the day the section was written. Plan 24-06's bundle compares it with the publication date. The check compares everything from `# 1.7.0_gg2, 2026-10-14` down with `07bf1e7695cc32fa7239b0b9ee0feed06c079f63:CHANGELOG.md` byte for byte, requires the four sections in order, the Map-key exception of D-04 branch 1 (`D-04-BRANCH=1` in E-03), the size-element scope with codes 32 and 33 in Known Issues, the links, the gg3 identity and the three dependency versions, and rejects planning IDs, an escaped-name line, the derived flag name and any line over 120 characters.

```text
heading True tail-unchanged True sections ['Important Changes', 'Known Issues', 'Bug Fixes', 'Build and CI'] exception True known-issues-ok True missing [] planning-ids [] escaped-name-line False derived-flag False long-lines []
CHANGELOG-GG3-OK
```

Matches text: n/a (gate check)

### E-31: FORK-DELTA coverage audit

Backs: the "Pinned by" cells of `docs/FORK-DELTA.md` for the Phase 21 to 23 requirement IDs with a code, build or test effect (plan 24-04, task 2). Date (UTC) 2026-10-20. Method of E-41 in the Phase 20 evidence, re-created as `/tmp/gsd24-scripts/cov.py` (scratch, not committed): for each of the twelve IDs the script lists the table rows whose last cell (Pinned by) names it, and for each row of `.planning/scripts/fork-anchors.txt` whose backs-ID is one of them it checks that the ID is named in such a cell. The twelve: DEPS-01, CI-18, CI-19, WRT-13, WRT-14, WRT-15, HARN-08, COLS-12, COLS-13, COLS-14, DEBT-10, DEBT-11. The first audit, before the rows were edited, had no row for CI-18, CI-19, DEPS-01, HARN-08, DEBT-10 and DEBT-11 (research R6.2); they were added to the Pinned-by cells of the CI row, the slf4j-reload4j and catalog rows, the new Test harness row and the declared-nullability row.

```sh
python3 -I /tmp/gsd24-scripts/cov.py "$PWD"
```

```text
DEPS-01 2 | `org.slf4j:slf4j-reload4j:2.0.20` is declared as; Fork dependency pins in the version catalog: Kaf
CI-18 1 | A single-job CI workflow: ClickHouse `latest` wi
CI-19 1 | A single-job CI workflow: ClickHouse `latest` wi
WRT-13 2 | `doWriteColValue`'s generic catch passes the ori; `Column.convertEnumValues` on a `Column` without
WRT-14 1 | `doWriteColValue`'s generic catch passes the ori
WRT-15 1 | `doWriteColValue`'s generic catch passes the ori
HARN-08 1 | Hand-written test doubles instead of inline Mock
COLS-12 1 | Tuple element nullability comes from the declare
COLS-13 1 | Tuple element nullability comes from the declare
COLS-14 1 | Tuple element nullability comes from the declare
DEBT-10 1 | Tuple element nullability comes from the declare
DEBT-11 1 | Tuple element nullability comes from the declare
anchor rows backed by a Phase 21-23 ID: 10 missing: 0
uncovered IDs: [] anchor rows missing: 0
```

Matches text: n/a (gate check)

### E-32: FORK-DELTA path resolution

Backs: every `.planning/` path that `docs/FORK-DELTA.md` cites (plan 24-04, task 2). Date (UTC) 2026-10-20. Same command as E-42 in the Phase 20 evidence. The documents named in the new text (`22-UAT.md`, `22-SECURITY.md`, `24-COMMAND-EVIDENCE.md`, the three ledgers) are named by file only, because the phase folders move when the milestone is archived; the paths the document does spell out all resolve.

```sh
grep -o '\.planning/[A-Za-z0-9_./-]*[A-Za-z0-9_-]' docs/FORK-DELTA.md | sort -u | while read -r p; do [ -n "$(git ls-files -- "$p")" ] && echo "OK   $p" || echo "DEAD $p"; done
```

```text
OK   .planning/MERGE-CHECKLIST-v1.7.0.md
OK   .planning/MERGE-CHECKLIST.md
OK   .planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md
OK   .planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-REVIEW-DISPOSITION.md
OK   .planning/milestones/v1.7.0_gg1-sync-phases/14-dependency-bumps-ci/14-REVIEW-DISPOSITION.md
OK   .planning/REQUIREMENTS.md
OK   .planning/ROADMAP.md
OK   .planning/scripts/fork-anchor-exemptions.txt
OK   .planning/scripts/fork-anchors.txt
OK   .planning/scripts/verify-merge-invariants.sh
```

No `DEAD` line. The two new paths are `.planning/ROADMAP.md` (the Phase 999.4 and 999.5 backlog entries that Known-costs items 10 and 11 point at) and `.planning/scripts/fork-anchor-exemptions.txt`.

Matches text: n/a (gate check)

### E-33: raw numbers, recount, skip inventory and cross-document consistency

Backs: `docs/FORK-DELTA.md` "The raw numbers" (two new `after gg3` rows, two new commands, the comparison and "One number does bear" paragraphs), the Adopted-wholesale `src/main` row and the Named skip inventory (plan 24-04, task 3), and the agreement of the rollout note, the CHANGELOG section and FORK-DELTA (research Pitfall 9). Date (UTC) 2026-10-20. HEAD is the commit that carries the rows, on `sync/v1.7.0`. Nothing under `src`, `build.gradle.kts`, `gradle` or `.github` changed after `98b9e04d` (the last such commit, the Jackson Map-key live test of plan 24-01), checked with `git diff --quiet 98b9e04d HEAD -- src build.gradle.kts gradle .github`, which exits 0. The tag `v1.7.0_gg3` does not exist yet, so the gg3 figures are measured against HEAD; plan 24-07 re-runs the two tag commands once the tag exists. Tags `v1.7.0_gg1` and `v1.7.0_gg2` exist, and the two published rows of each scope reproduce.

```sh
git diff --shortstat v1.7.0 HEAD -- src build.gradle.kts gradle .github
git diff --shortstat v1.7.0 HEAD -- src/main
git diff --shortstat v1.7.0 v1.7.0_gg2 -- src build.gradle.kts gradle .github
git diff --shortstat v1.7.0 v1.7.0_gg2 -- src/main
git diff --shortstat v1.6.0 pre-v1.7.0-sync -- src build.gradle.kts gradle .github
git diff --shortstat v1.7.0 v1.7.0_gg1 -- src build.gradle.kts gradle .github
git diff --shortstat v1.6.0 pre-v1.7.0-sync -- src/main
git diff --shortstat v1.7.0 v1.7.0_gg1 -- src/main
for s in src/test src/testFixtures src/main build.gradle.kts gradle .github; do git diff --shortstat v1.7.0 HEAD -- $s; done
```

```text
 130 files changed, 22783 insertions(+), 534 deletions(-)
 33 files changed, 2245 insertions(+), 145 deletions(-)
 119 files changed, 18802 insertions(+), 529 deletions(-)
 32 files changed, 2042 insertions(+), 143 deletions(-)
 84 files changed, 9476 insertions(+), 451 deletions(-)
 95 files changed, 13587 insertions(+), 484 deletions(-)
 30 files changed, 1368 insertions(+), 98 deletions(-)
 31 files changed, 1826 insertions(+), 130 deletions(-)
src/test:  88 files changed, 20064 insertions(+), 103 deletions(-)
src/testFixtures:  3 files changed, 68 insertions(+), 12 deletions(-)
src/main:  33 files changed, 2245 insertions(+), 145 deletions(-)
build.gradle.kts:  1 file changed, 177 insertions(+), 9 deletions(-)
gradle:  1 file changed, 8 insertions(+), 8 deletions(-)
.github:  4 files changed, 221 insertions(+), 257 deletions(-)
```

The gg1, gg2 and before rows keep their published figures; the two new rows carry 130/22783/534 and 33/2245/145. The catalog (`gradle`) moved by two more lines than at gg2 (6/6 to 8/8: the Lombok and Spotless bumps), and the `slf4j-reload4j` bump replaces a line in `build.gradle.kts` (177/9 as at gg2).

The byte-identical and import-order counts, E-40 method of the Phase 20 evidence (a file is byte-identical when `git diff --quiet v1.7.0 HEAD -- <file>` exits 0; of the others it is import-order-only when its content without `import ` lines and blank lines equals upstream's), re-created as `/tmp/gsd24-scripts/recount.py` (scratch, not committed):

```sh
python3 -I /tmp/gsd24-scripts/recount.py "$PWD" HEAD v1.7.0
```

```text
files present at v1.7.0 : 63
byte-identical at HEAD : 38
differ: 25  (import-order only: 14 , carry fork logic: 11 )
absent at HEAD : 0 []
files only here (added since v1.7.0): 8
    MetadataFreeJsonWriter.java
    DeclaredNullability.java
    ConvertToMapOrJson.java
    HeaderToValue.java
    HeaderToValueConfig.java
    HeadersToValue.java
    TableOperationFilter.java
    WithheldMessageException.java
differing src/main files in total: 33
import-only: ClickHouseSinkConnector.java, Record.java, StructToJsonMap.java, EmptyRecordConvertor.java, RecordConvertor.java, SchemaRecordConvertor.java, SchemalessRecordConvertor.java, DBWriter.java, InMemoryDBWriter.java, TableMappingRefresher.java, InMemoryState.java, ExtractTopic.java, ExtractTopicConfig.java, MBeanServerUtils.java
logic: ClickHouseSinkConfig.java, ClickHouseSinkTask.java, ProxySinkTask.java, Data.java, ClickHouseWriter.java, ClickHouseHelperClient.java, Column.java, Type.java, Utils.java, SinkTaskStatistics.java, org.apache.kafka.connect.transforms.Transformation
```

The same script on `v1.7.0_gg2` gives 38 byte-identical, 14 import-order only and 11 with fork logic, so the only change in the split since gg2 is the eighth new file, `WithheldMessageException`. 25 differing upstream files plus 8 new files make the 33 of the shortstat.

Named skip inventory, the two sources re-counted:

```sh
grep -rho '@Disabled' src/test/java | wc -l
grep -rho 'Assumptions.assumeTrue' src/test/java | wc -l
```

```text
       7
       1
```

7 `@Disabled` methods plus the 11 parameterized cases behind the one standalone-mode assumption stay 18; the Phase 24 live test adds no skip, and the inventory text is unchanged.

Cross-document consistency (Pitfall 9), `/tmp/gsd24-scripts/consistency.py` (scratch): every prose unit of the rollout note, the CHANGELOG gg3 section and FORK-DELTA that mentions `size0` also names codes 32 and 33; each of the three names `input_format_binary_read_json_as_string`, the words Map key and `v1.7.0_gg3`; the note and the CHANGELOG name `1.18.48`, `8.10.4`, `2.0.20` and `clickhouse-kafka-connect/v1.7.0_gg3`; FORK-DELTA names the three versions in its rows; the note and FORK-DELTA name `pre-v1.7.0_gg3`; the server version of E-03 appears in the note and in FORK-DELTA item 6; the MBean row no longer calls `v1.7.0_gg2` current.

```text
note: 2 units mention size0
changelog: 1 units mention size0
fork-delta: 3 units mention size0
fork-delta rows naming 1.18.48: 2
fork-delta rows naming 8.10.4: 1
fork-delta rows naming 2.0.20: 1
server version: 26.9.19.9
consistency failures: 0
```

The first run of the script failed on three units that mentioned `size0` without the codes (the rollback bullet of the note, and the `shouldSkipDescribeRow` and declared-nullability rows of FORK-DELTA). Each was fixed at its source document (the note's item 7 bullet now says "with code 32 or 33"; the two FORK-DELTA rows now name the codes and Known costs item 10), and plan 24-03's five checks (`ROLLOUT-FRAME-OK`, `ITEMS-1-7-OK`, `ITEMS-2-3-OK`, `ROLLOUT-T3-OK`, `ROLLOUT-FINAL-OK`) were re-run after that edit.

Matches text: n/a (gate check)
