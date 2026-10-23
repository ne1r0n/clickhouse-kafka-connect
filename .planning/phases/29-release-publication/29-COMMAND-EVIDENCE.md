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

### E-03: the shipped archive, tag v1.7.0_gg3 versus the gg4 tip (PUB-11, ROLL-04)

Backs: `docs/ROLLOUT-v1.7.0_gg4.md` (the dependencies item and the "the archive you deploy" sentences) and the `CHANGELOG.md` and `docs/FORK-DELTA.md` archive numbers (written in plans 29-04 and 29-05). Archive A is `createConfluentArchive` built at tag `v1.7.0_gg3` (`3376a18f`, worktree `/tmp/gsd29/wt-gg3`); archive B is built at the gg4 tip after the bump commit (`547c37a1`, worktree `/tmp/gsd29/wt-tip`). Both were built with `clean createConfluentArchive`, each in its own worktree, and each build directory held exactly one zip, copied into its own new, empty directory. Both builds: `BUILD SUCCESSFUL` (gg3 in 5s on Gradle 9.6.1, the gg3 wrapper; tip in 10s on Gradle 9.8.1).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd29/wt-gg3/gradlew -p /tmp/gsd29/wt-gg3 --console=plain clean createConfluentArchive
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd29/wt-tip/gradlew -p /tmp/gsd29/wt-tip --console=plain clean createConfluentArchive
shasum -a 256 /tmp/gsd29/archives/gg3/*.zip /tmp/gsd29/archives/tip/*.zip
python3 -I .planning/scripts/compare-confluent-archives.py /tmp/gsd29/archives/gg3/*.zip /tmp/gsd29/archives/tip/*.zip .planning/scripts/confluent-archive-allowlist-25.txt
python3 -I .planning/scripts/compare-confluent-archives.py /tmp/gsd29/archives/gg3/*.zip /tmp/gsd29/archives/tip/*.zip .planning/scripts/confluent-archive-allowlist-29.txt
```

```text
91c5fa657087e4171d40dc1902e985de8753011da3b6acf327107dd3f067d7aa  clickhouse-kafka-connect-v1.7.0_gg3.zip   (A, tag v1.7.0_gg3)
1365e5040bfbbc41150033a284208a647ff88deb13018e99745c3916719fa405  clickhouse-kafka-connect-v1.7.0_gg4.zip   (B, gg4 tip)
```

First compare, with the unchanged Phase 25 allow-list (exit 1, as expected): `RESULT: FAIL`, 22 `UNALLOWED` lines, no `STALE-ROW`. The 22 are `META-INF/MANIFEST.MF` and 21 `com/clickhouse/` paths, nothing else:

```text
UNALLOWED CHANGED META-INF/MANIFEST.MF
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/Version.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$1.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$2.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor$ClickHouseFieldDescriptorBuilder.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient$ClickHouseClientBuilder.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.class
UNALLOWED ONLY-B com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder$RowDecision.class
UNALLOWED ONLY-B com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/mapping/Column$1.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/mapping/Column$ColumnBuilder.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/mapping/Column.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability$1.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability$Assignment.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.class
UNALLOWED ONLY-B com/clickhouse/kafka/connect/sink/db/mapping/ParentShape.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/mapping/Table$1.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/mapping/Table$TableDesc.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/db/mapping/Table.class
UNALLOWED CHANGED com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.class
```

This is the research R5.1 set (20 fork classes) plus `Version.class` and the manifest, as the plan expected (flagged assumption A3). `git diff --name-only v1.7.0_gg3 HEAD -- src/main` lists 12 files: `ClickHouseSinkTask`, `ClickHouseWriter`, `ClickHouseFieldDescriptor`, `ClickHouseHelperClient`, `DescribeTableBuilder`, `Column`, `DeclaredNullability`, `ParentShape`, `Table`, `HeaderToValue`, `WithheldMessageException`, `SinkTaskStatistics`. Every non-`Version` row below names a class whose outer source file is in that list; `ClickHouseSinkTask`, `HeaderToValue` and `WithheldMessageException` changed comments in place and moved no line, so their class files are byte-identical (no row).

The Phase 29 allow-list `.planning/scripts/confluent-archive-allowlist-29.txt` (50 rows) is the 28 non-comment rows of `confluent-archive-allowlist-25.txt` unchanged and in order, then 21 exact-path `com/clickhouse/` rows (each reason names its requirement: DESC-07/DESC-08 for the size-name classifier and `DescribeTableBuilder`, NAME-03/NAME-04 for the column and element name match, NAME-06/NAME-07 for the Enum labels, WRT-17/WRT-18/WRT-19 for the writer, the comment-only line shift for `SinkTaskStatistics`, the release identity for `Version.class`), then `META-INF/MANIFEST.MF`. Neither the compare script nor the Phase 25 allow-list was edited. Second compare, with the Phase 29 allow-list (exit 0; the output is 2067 lines, sha256 `2b680121df5407787eb9691862c73b84fa11d5576364d895a1ddddd24a278c2f`; the summary block is quoted in full):

```text
VERSION at.yawk.lz4:lz4-java 1.12.0 -> 1.12.0
VERSION com.fasterxml.jackson.core:jackson-annotations 2.22 -> 2.22
VERSION com.fasterxml.jackson.core:jackson-core 2.22.3 -> 2.22.4
VERSION com.fasterxml.jackson.core:jackson-databind 2.22.3 -> 2.22.4
VERSION com.google.code.gson:gson 2.13.1 -> 2.14.0
VERSION com.google.errorprone:error_prone_annotations 2.38.0 -> 2.50.0
VERSION com.google.guava:failureaccess 1.0.3 -> 1.0.3
VERSION com.google.guava:guava 33.4.6-jre -> 33.7.2-jre
VERSION com.google.guava:listenablefuture 9999.0-empty-to-avoid-conflict-with-guava -> 9999.0-empty-to-avoid-conflict-with-guava
VERSION com.google.j2objc:j2objc-annotations 3.0.0 -> 3.1
VERSION commons-codec:commons-codec 1.19.0 -> 1.19.0
VERSION commons-io:commons-io 2.20.0 -> 2.20.0
VERSION org.apache.commons:commons-compress 1.28.0 -> 1.28.0
VERSION org.apache.commons:commons-lang3 3.20.0 -> 3.20.0
VERSION org.apache.httpcomponents.client5:httpclient5 5.6.2 -> 5.6.5
VERSION org.apache.httpcomponents.core5:httpcore5 5.4.3 -> 5.4.4
VERSION org.apache.httpcomponents.core5:httpcore5-h2 5.4.3 -> 5.4.4
VERSION org.slf4j:slf4j-api 2.0.7 -> 2.0.7
MANIFEST.MF: differs only in Implementation-Version
totals files-a 7376 files-b 7399 only-a 91 only-b 114 changed 1839 same 5446
nested jars in b: 0
kafka classes in b: 0
RESULT: PASS
```

The `VERSION` lines equal the Phase 25 E-22 lines for the third-party half; the guava `33.4.6-jre -> 33.7.2-jre`, httpclient5 `5.6.2 -> 5.6.5`, httpcore5 and httpcore5-h2 `5.4.3 -> 5.4.4`, jackson-core and jackson-databind `2.22.3 -> 2.22.4`, gson `2.13.1 -> 2.14.0`, j2objc `3.0.0 -> 3.1` and error_prone `2.38.0 -> 2.50.0` changes are the ones the note's dependency item may state; lz4-java, slf4j-api, commons-lang3 and the others are unchanged. No `UNALLOWED` and no `STALE-ROW` line.

Matches text: pending (29-04, 29-05)

### E-04: the archive of the rollback tree pre-v1.7.0_gg4 (D-03)

Backs: `docs/ROLLOUT-v1.7.0_gg4.md`, item 7 (rolling back brings back; the warning against building an archive from the rolled-back tip) (plan 29-04). The rollback target is `69cd680bf548bfb0329142752d37f8a6c3190c26` (`Merge pull request #14 from ne1r0n/dependabot/gradle/jackson-2.22.4`, `origin/custom_build`); the tag `pre-v1.7.0_gg4` does not exist yet (it is pushed at publication). Its archive was built in a throwaway worktree at that SHA (`/tmp/gsd29/wt-pre`, removed afterwards with `git worktree remove --force` and `git worktree prune`), with `clean createConfluentArchive`. The inner jars of the three zips were read in memory with Python `zipfile` under `python3 -I`; nothing was extracted.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd29/wt-pre/gradlew -p /tmp/gsd29/wt-pre --console=plain clean createConfluentArchive
python3 -I -   # zipfile read of the inner *-confluent.jar of /tmp/gsd29/archives/{pre,gg3,tip}/*.zip, in memory
git diff --stat v1.7.0_gg3 69cd680bf548bfb0329142752d37f8a6c3190c26
git show 69cd680bf548bfb0329142752d37f8a6c3190c26:.github/workflows/tests.yaml | grep -n -i -E "archive|createConfluent|gradle-version"
git show 69cd680bf548bfb0329142752d37f8a6c3190c26:.github/workflows/release.yaml | grep -n -E "createConfluentArchive|gradle-version|arguments"
git show 69cd680bf548bfb0329142752d37f8a6c3190c26:.github/workflows/pre-release.yaml | grep -n -E "createConfluentArchive|gradle-version|arguments"
```

```text
BUILD SUCCESSFUL in 2s   (build-pre.log; Gradle exit 0)
4b7856c3fa104c9fba3c23fc68dd3d5770b313342b1706eca8b6f2b9ac44aa94  clickhouse-kafka-connect-v1.7.0_gg3.zip   (pre: the zip is still named v1.7.0_gg3 because VERSION at 69cd680b is v1.7.0_gg3)

INNER tree=pre file_entries=135 jar_entries=25 connector_class=True
INNER tree=gg3 file_entries=7376 jar_entries=0 connector_class=True
INNER tree=tip file_entries=7399 jar_entries=0 connector_class=True
```

The 25 nested entries of the `pre` inner jar are: `RoaringBitmap-1.0.6.jar`, `asm-9.7.jar`, `clickhouse-client-0.9.6.jar`, `clickhouse-data-0.9.6.jar`, `clickhouse-http-client-0.9.6.jar`, `client-v2-0.9.6.jar`, `commons-codec-1.19.0.jar`, `commons-compress-1.28.0.jar`, `commons-io-2.20.0.jar`, `commons-lang3-3.20.0.jar`, `error_prone_annotations-2.48.0.jar`, `failureaccess-1.0.3.jar`, `gson-2.14.0.jar`, `guava-33.4.6-jre.jar`, `httpclient5-5.6.5.jar`, `httpcore5-5.4.4.jar`, `httpcore5-h2-5.4.4.jar`, `j2objc-annotations-3.0.0.jar`, `jackson-annotations-2.22.jar`, `jackson-core-2.22.4.jar`, `jackson-databind-2.22.4.jar`, `jspecify-1.0.0.jar`, `listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar`, `lz4-java-1.12.0.jar`, `slf4j-api-2.0.7.jar`. That is the Shadow 9.6.1 two-argument `from(...)` outcome that Phase 25 repaired: the build is green, the connector class is present, and the jar holds the dependencies as nested jars instead of merged classes (135 file entries against 7376 for the archive at tag `v1.7.0_gg3`). The archive built at tag `v1.7.0_gg3` holds none, and so does the gg4 tip's (E-03).

PRE-NESTED-JARS=25

`git diff --stat v1.7.0_gg3 69cd680bf548bfb0329142752d37f8a6c3190c26` (what the rollback tree differs from the tag in; the tail, 22 files changed, 1087 insertions, 175 deletions):

```text
 .planning/PROJECT.md | .planning/REQUIREMENTS.md | .planning/ROADMAP.md | .planning/STATE.md | the Phase 24 planning files
 .planning/state.json                               |  10 +-
 CHANGELOG.md                                       |  11 +-
 build.gradle.kts                                   |   2 +-
 docs/FORK-DELTA.md                                 |   6 +-
 docs/ROLLOUT-v1.7.0_gg3.md                         |  38 ++--
 gradle/libs.versions.toml                          |  16 +-
 gradle/wrapper/gradle-wrapper.jar                  | Bin 48462 -> 48749 bytes
 gradle/wrapper/gradle-wrapper.properties           |   2 +-
 gradlew.bat                                        | 194 ++++++++++++---------
 22 files changed, 1087 insertions(+), 175 deletions(-)
```

Workflow facts at `69cd680b`:

```text
tests.yaml:      no line matches "archive", "createConfluent" or "gradle-version" (grep exit 1): the push workflow builds no archive.
release.yaml:    31:      - name: Execute 'createConfluentArchive' task
                 32:        run: ./gradlew createConfluentArchive          (no gradle-version line: it uses the wrapper)
pre-release.yaml: 28:      - name: Setup and execute Gradle 'createConfluentArchive' task
                 31:          arguments: createConfluentArchive
                 32:          gradle-version: '7.4.2'
```

So, on the rolled-back tree, a build is green and the archive is wrong: `release.yaml` and `pre-release.yaml` would both build it, and `tests.yaml` would not notice. The only sound artifact step on rollback is to redeploy the archive built at tag `v1.7.0_gg3` (D-03); an archive built from the rolled-back branch tip must not be deployed.

Matches text: pending (29-04, 29-05)

### E-05: phase base on the bump commit (PT-14)

Backs: nothing in a document; this is the reference the 29-07 suite gate compares against (decisions PT-14 and PT-15). Both runs started on the tree of the bump commit `112fbdb5` plus the 29-01 evidence and allow-list commits (HEAD `ebc1740a`; only `.planning/` differs from the bump commit), before the cumulative gate of E-09, so the machine was not shared with the gate. Both commands set the declared matrix on their own command line: ClickHouse `latest`, client V2, the cluster variable unset.

```sh
rm -rf build/test-results/test build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE -u CLICKHOUSE_CLUSTER_NAME CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 ./gradlew --console=plain testUnit --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE -u CLICKHOUSE_CLUSTER_NAME CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 ./gradlew --console=plain test --rerun
cp -R build/test-results/testUnit /tmp/gsd29/exec/testunit-base ; cp -R build/test-results/test /tmp/gsd29/exec/suite-base
```

```text
testunit run: BUILD SUCCESSFUL in 13s     (2026-10-23T17:29:58Z to 17:30:11Z)
suite run:    BUILD SUCCESSFUL in 2m 36s  (suite XML timestamps 2026-10-23T17:30:17Z to 17:32:48Z)
testunit_base: 1278 tests, 0 failed, 2 skipped
suite_base: 1865 tests, 103 xml, 0 failed, 18 skipped
testunit_base_class_counts_sha256: 6f209d0b95b008fbf1fa3daeb75fcaee5a4a743b0b355b7affeef25a2557a254
suite_base_class_counts_sha256: 3d849967dc1c29e230820a5c94f2a22626b6f9fe758ad868ca8f30fcce0d980f
server_version: 26.9.19.9
image: sha256:bdb5c911a23d92fd364e246ab20b5a0a485f00112f3663fa626b083fb46c146b 2026-10-14T18:05:21.432177888Z
suite_env: CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 CLICKHOUSE_CLUSTER_NAME=unset
suite_jvm_clickhouse_version: latest
```

The class-count digest is the SHA-256 of the sorted lines `<xml file name> <tests> <skipped>` joined by a line feed, over `TEST-*.xml` of the run's result directory (59 XML files for `testUnit`, 103 for the full suite); the 29-07 gate recomputes it over its own XML and compares. The suite XML logs exactly one `server version=` value (`26.9.19.9`) and six `ClickHouseTestHelpers Version:` lines, all `latest`, so the pinned `CLICKHOUSE_VERSION` reached the test JVM. `CLIENT_VERSION=V2` was set by the command and has no log line in the test JVM: its arrival is inferred from the same `env` invocation, and V2 is also the code default (`extractClientVersion()` returns V2 for anything but `V1`). `integrationTest` is not part of the base (PT-16): the source set is accepted as compile-only, and `compileIntegrationTestJava` runs in the 29-07 gate and in `tests.yaml`. `ConnectApiReferencesUnitTest` passed in this `testUnit` run (3 tests, 0 failures); E-06 cites that as the OPS-06 input.

Totals agree with `28-VERIFICATION` (1865 tests in the full suite); the E-13 totals of Phase 28 (1835) predate the 28 review-fix tests and are not used (PT-14).

Matches text: n/a (gate check)
