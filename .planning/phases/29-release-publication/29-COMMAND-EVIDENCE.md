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

### E-09: cumulative anchors gate on the bump commit (D-01, PT-01)

Backs: nothing in a document; this is the D-01 gate check on the bump commit. The 29-07 task runs the same two commands again on the frozen release tip. It sits right after E-05 in time (entries are found by heading, never by position).

Both commands ran in the default environment (`PRE_TAG=pre-v1.7.0-sync`, `UPSTREAM_REF=refs/tags/v1.7.0`, the v1.7.0 conflicted-paths file; no variable was set, checked with `env | grep`), started together in the background after E-05 was committed, with nothing committed until both had ended. HEAD at the start: `503b70f8290c7c5fa0eb3fc2fd7b16c6be2cff9b` (the E-05 commit; only `.planning/` differs from the bump commit `112fbdb5`). Started 2026-10-23T17:33:24Z, both ended by 2026-10-23T17:48:47Z (about 15 min 23 s). Neither script, `fork-anchor-exemptions.txt` nor `fork-anchors.txt` (beyond the bump row) was edited.

```sh
bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD > /tmp/gsd29/exec/gate-29-01.log 2>&1       # exit 0
bash .planning/scripts/verify-merge-invariants-selftest.sh > /tmp/gsd29/exec/selftest-29-01.log 2>&1        # exit 0
tail -n 1 /tmp/gsd29/exec/gate-29-01.log
tail -n 1 /tmp/gsd29/exec/selftest-29-01.log
```

```text
invariants: provenance_paths=21 anchor_commits=766 anchor_rows=94 marker_commits=825 anchor_exempt=19 result=PASS
selftest: no failures
```

The gate log is that single line; the self-test log has 17 lines (16 `PASS` lines, 0 `FAIL`, then the last line above).

Matches text: n/a (gate check)

### E-06: Kafka 4.4.0 upgrade notes, KIP-1161 and the production worker (D-10, OPS-06)

Backs: `docs/ROLLOUT-v1.7.0_gg4.md`, the dependencies item (Connect API 4.4.0, the production worker, the oldest verified worker, KIP-1161) (plan 29-04). Read 2026-10-23, unauthenticated, downloads saved under `/tmp/gsd29/exec/dl/` and only grepped. Source labels are given with each fact.

```sh
curl -s -o kafka-upgrade-440.md -w '%{http_code}' https://raw.githubusercontent.com/apache/kafka/4.4.0/docs/getting-started/upgrade.md
curl -s -o kafka-site-44.html   -w '%{http_code}' https://kafka.apache.org/44/getting-started/upgrade/
curl -s -o connect-api-metadata.xml -w '%{http_code}' https://repo1.maven.org/maven2/org/apache/kafka/connect-api/maven-metadata.xml
curl -s -o archive-listing.html -w '%{http_code}' https://archive.apache.org/dist/kafka/4.4.0/
curl -s -o /dev/null            -w '%{http_code}' https://api.github.com/repos/apache/kafka/git/ref/tags/4.4.0
grep -n "^## Upgrading to" kafka-upgrade-440.md ; grep -n "KIP-1161\|KIP-1209\|KIP-909" kafka-upgrade-440.md
python3 -I   # counts over src/main/java: "ConfigDef.Type.LIST" and "ValidList"
```

```text
HTTP 200  raw.githubusercontent.com/apache/kafka/4.4.0/docs/getting-started/upgrade.md   (source tree at tag 4.4.0; 67051 bytes)
HTTP 404  kafka.apache.org/44/getting-started/upgrade/                                   (the website page does not exist yet)
HTTP 200  repo1.maven.org/maven2/org/apache/kafka/connect-api/maven-metadata.xml         (lists <version>4.4.0</version>; <latest>4.4.0</latest>, <release>4.4.0</release>, lastUpdated 20261020124053)
HTTP 200  archive.apache.org/dist/kafka/4.4.0/                                           (RELEASE_NOTES.html and .asc dated 2026-10-06 16:20)
HTTP 200  api.github.com/repos/apache/kafka/git/ref/tags/4.4.0                           (the tag 4.4.0 exists)
```

Section map of the source file (`grep -n "^## Upgrading to"`): `29: ## Upgrading to 4.4.0`, `64: ## Upgrading to 4.3.2`, `76: ## Upgrading to 4.3.1`, `85: ## Upgrading to 4.3.0`, `114: ## Upgrading to 4.2.1`, `122: ## Upgrading to 4.2.0`.

- **`## Upgrading to 4.4.0` (lines 29-62), Connect-facing lines.** Line 55: Kafka Connect distributed workers now support `internal.topics.automatic.creation.enable` (default `true`; when `false`, Connect fails at startup if its internal topics are missing; new `connect-internal-topics.sh` tool), linked to KIP-1209. Line 59: the `bootstrap.resolve.timeout.ms` bullet (KIP-909) says Connect does not support asynchronous bootstrap resolution yet and forces the value to `0` for every client it creates. Both are worker-side. No line of the section is about `connect-api`, `SinkTask`, `ConfigDef`, transformations or converters. [Source: apache/kafka@4.4.0 `docs/getting-started/upgrade.md:55,59`]
- **KIP-1161 sits under `## Upgrading to 4.2.0`** (section starts at line 122), not under 4.4.0: lines 168-173 are the bullets. Line 168: "Improvements have been made to the validation rules and default values of LIST-type configurations (KIP-1161)". Then: LIST-type configurations now enforce stricter validation; null values are no longer accepted for most LIST-type configurations (exceptions: configurations whose default is null); most LIST-type configurations no longer accept duplicate entries (duplicates are ignored with a logged warning for backward compatibility); for certain configurations an empty list is no longer allowed. [Source: apache/kafka@4.4.0 `docs/getting-started/upgrade.md:122,168-173`]
- **The connector's own LIST keys.** Counted with a Python reader over the 71 files of `src/main/java`:

```text
type_list_keys: 7
validlist_in_src_main: 0
```

  The seven `ConfigDef.Type.LIST` declarations are `ClickHouseSinkConfig.java:525,534,682`, `HeaderToValue.java:49,54` and `HeaderToValueConfig.java:23,28`; `ValidList` appears nowhere in `src/main`. [Source: grep of the tree at HEAD `503b70f8`] So the connector's own keys carry no validator that KIP-1161 would tighten; a Kafka 4.2 or newer worker applies the strictness to its own and other plugins' configurations. The production worker is Kafka 3.9 (below).
- **Production worker (cited, not re-measured here).** `confluentinc/cp-kafka-connect-base:7.9.7` (Confluent Platform 7.9, Apache Kafka 3.9), image digest `sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055`; the three API jars from `/usr/share/java/kafka/` are `connect-api-7.9.7-ccs.jar`, `connect-runtime-7.9.7-ccs.jar`, `kafka-clients-7.9.7-ccs.jar`, byte-identical on the amd64 and arm64 images. [Source: `25-COMMAND-EVIDENCE.md` E-12; the worker line is the user's statement of 2026-10-21, REQUIREMENTS OPS-06]
- **Oldest verified worker (decision PT-08).** Apache Kafka 3.9, by a static bytecode reference check, not by a run on a live worker: the compiled connector's 176 `org.apache.kafka` references (39 classes, 137 members) are all present in the 7.9.7 jars (`REFERENCE worker classes-missing 0 members-missing 0`), in `connect-api` 4.1.1 and in 4.4.0 (same zeros), and the shipped archive holds 0 `org/apache/kafka/` entries (E-03: `kafka classes in b: 0`). [Source: `25-COMMAND-EVIDENCE.md` E-12, E-13] The check is pinned in the suite by `ConnectApiReferencesUnitTest`, which passed in the testUnit run of E-05 (3 tests, 0 failures). The worker's `SinkTask` still declares `onPartitionsRevoked`; the 4.1.1 and 4.4.0 jars do not, which is why the connector keeps the method without `@Override`. [Source: `25-COMMAND-EVIDENCE.md` E-12]

Matches text: pending (29-04)

### E-07: advisory identity by source, guava and httpclient5 (D-10)

Backs: `docs/ROLLOUT-v1.7.0_gg4.md` (the dependencies item) and the `docs/FORK-DELTA.md` advisory row (plans 29-04 and 29-05). Read 2026-10-23 (the OSV record of the guava CVE shows `modified` `2026-10-23T17:27:08.804507804Z`), unauthenticated, 5 of 60 GitHub REST requests used by this entry (60 available before). Responses saved under `/tmp/gsd29/exec/dl/`. No exposure assessment is made: each fact is the source's own statement.

```sh
curl -s -H 'Accept: application/json' 'https://services.nvd.nist.gov/rest/json/cves/2.0?cveId=CVE-2026-102554'
curl -s -H 'Accept: application/json' 'https://services.nvd.nist.gov/rest/json/cves/2.0?cveId=CVE-2026-64607'
curl -s https://api.osv.dev/v1/vulns/CVE-2026-102554 ; curl -s https://api.osv.dev/v1/vulns/GHSA-xxph-c9ww-hj94 ; curl -s https://api.osv.dev/v1/vulns/GHSA-hjcp-jmpx-g3qm
curl -s -X POST https://api.osv.dev/v1/query -d '{"package":{"name":"com.google.guava:guava","ecosystem":"Maven"},"version":"33.4.6-jre"}'   # and 33.7.2-jre
curl -s -X POST https://api.osv.dev/v1/query -d '{"package":{"name":"org.apache.httpcomponents.client5:httpclient5","ecosystem":"Maven"},"version":"5.6.2"}'   # and 5.6.5
curl -s https://api.github.com/advisories/GHSA-xxph-c9ww-hj94 ; curl -s https://api.github.com/advisories/GHSA-hjcp-jmpx-g3qm
curl -s -o /dev/null -w '%{http_code}' https://github.com/google/guava/security/advisories/GHSA-xxph-c9ww-hj94
```

**Guava: `CVE-2026-102554` = `GHSA-xxph-c9ww-hj94`. gg3 ships guava `33.4.6-jre`, gg4 ships `33.7.2-jre` (E-03 `VERSION` lines).**

```text
HTTP 200  NVD cves/2.0?cveId=CVE-2026-102554
          vulnStatus "Awaiting Analysis"; published 2026-10-09T16:17:20.410; lastModified 2026-10-09T18:17:01.380;
          source cve-coordination@google.com; weaknesses CWE-502 and CWE-770; no CPE configuration;
          metric cvssMetricV40 (source cve-coordination@google.com) baseScore 8.2 HIGH;
          description: "... Google Guava versions 4.0 through 33.7.1 allows an attacker to cause a Denial of Service via OutOfMemoryError.
          When deserializing CompactHashMap, CompactHashSet, or MapMakerInternalMap instances, Guava eagerly allocates an array based on a
          caller-specified size parameter without throttling ..."
HTTP 200  OSV  /v1/vulns/CVE-2026-102554
          id CVE-2026-102554; aliases ["GHSA-xxph-c9ww-hj94"]; published 2026-10-09T15:07:16.130Z; modified 2026-10-23T17:27:08.804507804Z;
          summary "Denial of Service via Eager Array Allocation During Deserialization in Guava";
          affected: one GIT range (repo https://github.com/google/guava, introduced 0, fixed b931fe9d6d5cf00bc55714ad3308d086f71850fe), no Maven range;
          database_specific.unresolved_ranges: "introduced 4.0, fixed 33.7.2" (source AFFECTED_FIELD) and "introduced 4.0, last_affected 33.7.1" (source DESCRIPTION)
HTTP 404  OSV  /v1/vulns/GHSA-xxph-c9ww-hj94
          {"code":5,"message":"Vulnerability not found, but the following aliases were: CVE-2026-102554"}
HTTP 200  OSV  POST /v1/query Maven com.google.guava:guava 33.4.6-jre  ->  {}
HTTP 200  OSV  POST /v1/query Maven com.google.guava:guava 33.7.2-jre  ->  {}
HTTP 404  api.github.com/advisories/GHSA-xxph-c9ww-hj94     (GitHub's global advisory database does not hold it)
HTTP 200  github.com/google/guava/security/advisories/GHSA-xxph-c9ww-hj94    (a repository advisory of google/guava)
```

Net for guava: the pair and the fixed version `33.7.2` are as in `25-COMMAND-EVIDENCE.md` E-22. The two machine sources disagree about gg3's `33.4.6-jre`: the NVD text range ("4.0 through 33.7.1") includes it, OSV's Maven query returns nothing for it (the OSV record carries a GIT range and unresolved ranges, no Maven range). `33.7.2-jre` is outside the NVD text range and returns nothing at OSV.

**httpclient5: `CVE-2026-64607` = `GHSA-hjcp-jmpx-g3qm`. gg3 ships httpclient5 `5.6.2`, gg4 ships `5.6.5` (E-03).**

```text
HTTP 200  NVD cves/2.0?cveId=CVE-2026-64607
          vulnStatus "Modified"; published 2026-07-31T11:17:11.710; lastModified 2026-08-13T17:17:33.733; source security@apache.org;
          weakness CWE-772; has a CPE configuration; metric cvssMetricV31 baseScore 5.3;
          description: classic i/o model HttpClient fails to release the connection on an invalid or unsupported Content-Encoding header
          value; "does not affect HttpClient based on the async i/o model"; "This issue affects Apache HttpComponents Client: from 5.0-alpha1 through 5.6.2."
HTTP 200  api.github.com/advisories/GHSA-hjcp-jmpx-g3qm
          cve_id CVE-2026-64607; severity medium; published_at 2026-07-31T12:30:30Z; updated_at 2026-08-13T18:40:19Z; withdrawn_at null;
          summary "Apache HttpComponents Client: Connection Leak on Content-Encoding Decode Error Leads to Pool Exhaustion DoS";
          package maven org.apache.httpcomponents.client5:httpclient5; vulnerable_version_range ">= 5.0-alpha1, < 5.6.3"; first_patched_version 5.6.3
HTTP 200  OSV  /v1/vulns/GHSA-hjcp-jmpx-g3qm
          aliases ["CVE-2026-64607"]; published 2026-07-31T12:30:30Z; modified 2026-09-10T03:50:12.358752934Z;
          ECOSYSTEM range introduced 5.0-alpha1, fixed 5.6.3; the explicit versions list ends at 5.6.2 (contains 5.6.2, does not contain 5.6.5)
HTTP 200  OSV  /v1/vulns/CVE-2026-64607    (aliases ["GHSA-hjcp-jmpx-g3qm"]; modified 2026-10-21T02:58:57.044251076Z; GIT range, fixed 13408e3f33dfc3763c44fe96e0d00fa6f6ecc65d)
HTTP 200  OSV  POST /v1/query Maven org.apache.httpcomponents.client5:httpclient5 5.6.2  ->  {"vulns":[{"id":"GHSA-hjcp-jmpx-g3qm", ...}]}   (re-read 2026-10-23T17:50Z, twice, with and without Content-Type: application/json; the purl forms pkg:maven/org.apache.httpcomponents.client5/httpclient5@5.6.2 and the separate-version form give the same id)
HTTP 200  OSV  POST /v1/query Maven org.apache.httpcomponents.client5:httpclient5 5.6.5  ->  {}   (same re-reads; the purl forms give {"vulns":[]} or {})
```

Net for httpclient5: the identity (`CVE-2026-64607` / `GHSA-hjcp-jmpx-g3qm`, medium, fixed `5.6.3`) is as researched. gg3's `5.6.2` is inside every source's range (NVD text "through 5.6.2", GitHub `< 5.6.3`, OSV range and versions list); gg4's `5.6.5` is above the fixed version `5.6.3`.

**Correction of the first read (recorded, not worded away).** The first OSV version queries of this entry, made at about 2026-10-23T17:31Z while the OSV record of the guava CVE was being re-indexed (its `modified` is `2026-10-23T17:27:08Z`), returned `{}` for httpclient5 `5.6.2` in the name form and in the purl form, which differed from research R2.5 (the id for `5.6.2`, `{}` for `5.6.5`). That first version of this entry said so and drew a wording limit from it. The same queries repeated at 2026-10-23T17:50Z, twice each and in three request forms, return `GHSA-hjcp-jmpx-g3qm` for `5.6.2` and nothing for `5.6.5`, which is the research answer; the guava queries returned `{}` for both versions in every read. The repeated, later reads are the record, and this entry was corrected in a follow-up commit for that reason. The document may say that OSV's version query lists the httpclient5 advisory for `5.6.2` and not for `5.6.5`, labelled by source and read date. The guava record's `modified` timestamp differs from the research capture (the 2026-10-21T09:31:55Z value is now only the `vanir_signatures_modified` field), with no change to the identity or the fixed version.

Matches text: pending (29-04, 29-05)

### E-08: OPS-04, OPS-01 and the OPS-05 first read

Backs: `CHANGELOG.md`, the milestone close record and the 29-07 and 29-08 approval bundles (plan 29-05 and later). First read of the origin freeze, before any document of Phase 29 is written; 29-07 and 29-08 re-read it. Read 2026-10-23, unauthenticated GitHub REST (`ne1r0n/clickhouse-kafka-connect`; 55 of 60 requests were left before these reads) and `git` over the configured `origin`.

```sh
curl -s https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/labels/dependencies
curl -s 'https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/pulls?state=all&per_page=100&sort=created&direction=desc'
curl -s https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/integrationTests.yaml
curl -s 'https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/integrationTests.yaml/runs?event=schedule&per_page=100'
git fetch --quiet origin --no-tags
git ls-remote origin refs/heads/custom_build
git rev-list --left-right --count origin/custom_build...HEAD
git merge-base --is-ancestor 69cd680bf548bfb0329142752d37f8a6c3190c26 HEAD
git ls-remote --heads origin ; git ls-remote --tags origin
```

```text
HTTP 200  labels/dependencies   name "dependencies", color d40926, description "Dependency updates (Dependabot)"
HTTP 200  pulls                 15 PRs in total (#1 to #15), every one opened by dependabot[bot]; newest #15 created 2026-10-21T06:29:01Z;
                                open: #4 (clickHouseDriver-0.10.0) and #10 (com.google.protobuf-protobuf-java-4.36.2); none carries a label
HTTP 200  workflows/integrationTests.yaml   id 384641581, name "Integration Tests", state "disabled_manually", updated_at 2026-10-16T09:38:23.000Z
HTTP 200  workflows/integrationTests.yaml/runs?event=schedule   total_count 2: 38881153720 (2026-10-16T09:32:28Z, failure) and 38714528739 (2026-10-15T09:39:18Z, failure); 0 created after 2026-10-16T09:38:23Z

ops04: pending (no PR was opened by dependabot[bot] after the label's creation at 2026-10-21T15:11:05Z; the newest of the 15 PRs, #15, is from 2026-10-21T06:29:01Z, so the next monthly run is the first that can show whether the "labels could not be found" comment is gone)
ops01: disabled_manually, 0 schedule runs after the disable
origin_custom_build_tip: 69cd680bf548bfb0329142752d37f8a6c3190c26
left_count: 0

git rev-list --left-right --count origin/custom_build...HEAD   ->  0	211     (left 0, right 211, measured with HEAD at d56ef46a)
git merge-base --is-ancestor 69cd680b HEAD                      ->  PRE is an ancestor of HEAD (exit 0)

git ls-remote --heads origin
69cd680bf548bfb0329142752d37f8a6c3190c26	refs/heads/custom_build
ca52b81ad1a188e1e288336a2231d9d3092d45fe	refs/heads/dependabot/gradle/clickHouseDriver-0.10.0
011bc42bfe8d8230c430ec8001d481885daec5fa	refs/heads/dependabot/gradle/com.google.protobuf-protobuf-java-4.36.2
bec167d85918e77c11621063000acc6e031ae2fa	refs/heads/main

git ls-remote --tags origin
6ac2d20cc0c7feacc315b21b322325b4fee25784	refs/tags/pre-v1.6.0-sync
c61d90ce5dfb32ea5876dffacc8bb54d8846739a	refs/tags/pre-v1.7.0-sync
be6b8861c25f3d7611194e7150b18362c5c75e32	refs/tags/pre-v1.7.0_gg2
6473e24f372a8b37dfd8cc8cf3124e6cff52edca	refs/tags/pre-v1.7.0_gg3
c61d90ce5dfb32ea5876dffacc8bb54d8846739a	refs/tags/v1.6.0_gg1
f328fd218dce5b9c652f8872042e922f9686b87f	refs/tags/v1.7.0_gg1
a246ca01b073faae86e2e92bb0288c07dd7eb0d7	refs/tags/v1.7.0_gg2
3376a18fd057dba329ceae7d8c74d1ba4e3d36f6	refs/tags/v1.7.0_gg3
```

`custom_build` has not moved since the DEPS-02 merge: no Phase 29 decision under OPS-05 is open. Origin has no `sync/v1.7.0` branch, no `pre-v1.7.0_gg4` tag and no `v1.7.0_gg4` tag. Nothing was pushed, merged, tagged or commented on by this plan; PRs #4 and #10 were only read.

Matches text: pending (29-05)

### E-10: Column trees over the 47 catalog tables, gg3 versus gg4 (999.4)

Backs: `docs/ROLLOUT-v1.7.0_gg4.md`, the 999.4 items (scope: which tables encode differently, the WARN counts, the log-grep proof), written in plan 29-04. Method: the probe method `sizeDescribe` (scratch `/tmp/gsd29/probe/Phase29RolloutProbe.java`, never committed) creates the 47 tables of `DescribeFixtureCatalog` (the list is read from `/tmp/gsd29/probe/catalog.tsv`, produced by `/tmp/gsd29-scripts/catalog-tsv.py` from the tip's catalog class, because the class does not exist at `v1.7.0_gg3`) as `CREATE TABLE p29_desc.<key> (id Int32, <columnsDdl>) ENGINE = MergeTree ORDER BY id` in a fresh database of the run's Testcontainers ClickHouse, describes each through the production `ClickHouseHelperClient.describeTable` (client V2) with `LogCapture` attached to `ClickHouseHelperClient` and `DeclaredNullability`, and prints one `PROBE-COL` line per Column node (path = the node names and types joined by `/` for a Tuple element, `[]` for an Array element, `{}` for a Map value, plus the nullable flag), one `PROBE-WARNS` line per table (counts of WARN events with `have no matching DESCRIBE sub-column` and `Could not read the declared type`) and, on gg3, the first unpaired WARN of `s4_a01`. The same class ran unchanged in a worktree of tag `v1.7.0_gg3` (`/tmp/gsd29/wt-gg3`, `3376a18f`) and in a worktree at the gg4 tip (`/tmp/gsd29/wt-tip`, detached at `46b2cf15`; only `.planning/` differs from the bump commit `547c37a1`, and `src/` is as at `P29_BASE`). Each run had its own container: server `26.9.19.9`, client V2, image `sha256:bdb5c911a23d92fd364e246ab20b5a0a485f00112f3663fa626b083fb46c146b` created 2026-10-14T18:05:21Z (the floating `latest`).

```sh
python3 -I /tmp/gsd29-scripts/catalog-tsv.py src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCatalog.java /tmp/gsd29/probe/catalog.tsv
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd29/wt-gg3/gradlew -p /tmp/gsd29/wt-gg3 --console=plain test --tests '*Phase29RolloutProbe.sizeDescribe' --tests '*Phase29RolloutProbe.sizeInsert' --rerun
cp /tmp/gsd29/wt-gg3/build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.Phase29RolloutProbe.xml /tmp/gsd29/exec/d994-gg3.xml
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd29/wt-tip/gradlew -p /tmp/gsd29/wt-tip --console=plain test --tests '*Phase29RolloutProbe.sizeDescribe' --tests '*Phase29RolloutProbe.sizeInsert' --rerun
cp /tmp/gsd29/wt-tip/build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.Phase29RolloutProbe.xml /tmp/gsd29/exec/d994-gg4.xml
python3 -I /tmp/gsd29-scripts/xml-probe-lines.py /tmp/gsd29/exec/d994-gg3.xml /tmp/gsd29/exec/d994-gg4.xml
```

Both runs ended `BUILD SUCCESSFUL` (XML `tests="2" skipped="0" failures="0" errors="0"` each; 179 `PROBE-COL` lines on gg3, one per Column node; the gg4 run has more).

```text
PROBE-VERSION tree=gg3 version=26.9.19.9
PROBE-VERSION tree=gg4 version=26.9.19.9
PROBE-WARNS tree=gg3 table=<each of the 25 differing keys> unpaired=1 unreadable=0
PROBE-WARNS tree=gg3 table=<each of the other 22 keys, x08 and the over-flag s4_o01 included> unpaired=0 unreadable=0
PROBE-WARNS tree=gg4 table=<each of the 47 keys> unpaired=0 unreadable=0
PROBE-UNMAPPED lines: none on either tree (all 47 tables were described to a Table)
PROBE-WARNLINE tree=gg3 table=s4_a01 text=Declared Tuple element(s) [c.size] of column c of table p29_desc.s4_a01 (declared type Tuple(\\n    size UInt16,\\n    v String)) have no matching DESCRIBE sub-column; applying the declared nullability to the elements that paired
```

(The probe prints a backslash as two, so the `\\n` in the `PROBE-WARNLINE` is the two characters backslash and `n` that the connector's own log line holds: DESCRIBE returns the declared type with line breaks as the text `\n`, and the WARN carries it unchanged, as the gg3 note says. The log line proper is `Declared Tuple element(s) [c.size] of column c of table p29_desc.s4_a01 (declared type Tuple(\n    size UInt16,\n    v String)) have no matching DESCRIBE sub-column; applying the declared nullability to the elements that paired` with `\n` as backslash and `n`.)

Per-table verdict, from the `PROBE-COL` lines of both XMLs. In every differing table the gg4 tree holds the nodes listed and the gg3 tree holds no node that gg4 lacks (a node that gg3 dropped is a Tuple element named `size`, `size0` or `size<digits>`; its parent keeps the remaining elements):

| Table | Verdict | Nodes only in the gg4 tree |
|-------|---------|----------------------------|
| s4_a01 | differing | `c:TUPLE/c.size:UINT16` |
| s4_a02 | differing | `c:TUPLE/c.size0:UINT8` |
| s4_a03 | differing | `c:TUPLE/c.size:UINT8` |
| s4_a04 | differing | `c:ARRAY[]c:TUPLE/c.size:UINT16` |
| s4_a05 | differing | `c:MAP{}c.values:TUPLE/c.values.size:UINT8` |
| s4_a06 | differing | `c:TUPLE/c.x:TUPLE/c.x.size:UINT8` |
| s4_a07 | differing | `c:TUPLE/c.x:ARRAY[]c.x:TUPLE/c.x.size0:UINT8` |
| s4_a08 | differing | `c:ARRAY[]c:TUPLE/c.size1:UINT16` |
| s4_a09 | differing | `c:ARRAY[]c:TUPLE/c.size12:UINT8` |
| s4_a10 | differing | `c:ARRAY[]c:ARRAY[]c:TUPLE/c.size1:UINT8` |
| s4_a11 | differing | `c:TUPLE/c.x:ARRAY[]c.x:TUPLE/c.x.size1:UINT8` |
| s4_a12 | differing | `c:ARRAY[]c:TUPLE/c.x:TUPLE/c.x.size1:UINT8` |
| s4_a13 | differing | `c:ARRAY[]c:TUPLE/c.size1:UINT16` |
| n01 | differing | `c.size0:UINT8`, `c.size1:UINT8` under `ARRAY[]ARRAY[]TUPLE` |
| n02 | differing | `c:ARRAY[]c:TUPLE/c.size0:TUPLE` and its child `c.size0.a:UINT8` |
| n03 | differing | `c.size0:STRING`, `c.size:STRING` under `ARRAY[]TUPLE` |
| n04 | differing | `c.values.size0:UINT8`, `c.values.size:UINT8` under `ARRAY[]MAP{}TUPLE` |
| x01 | differing | `c:TUPLE/c.size:TUPLE` and its children `c.size.a:UINT8`, `c.size.b:STRING` |
| x02 | differing | `c:ARRAY[]c:TUPLE/c.size:TUPLE` and its child `c.size.a:UINT8` |
| x03 | differing | `size:TUPLE/size.size0:STRING`, `size:TUPLE/size.size:UINT8` |
| x04 | differing | `c:ARRAY[]c:TUPLE/c.size1:UINT64` |
| x05 | differing | `c.size0:UINT64`, `c.size1:UINT64` under `ARRAY[]ARRAY[]TUPLE` |
| x06 | differing | `c:ARRAY[]c:TUPLE/c.size0:UINT8` |
| x07 | differing | `c:MAP{}c.values:ARRAY[]c.values:TUPLE/c.values.size:UINT8` |
| r01 | differing | `c:TUPLE/c.size:UINT32` |
| s4_u01..s4_u19 (19 tables) | identical | none |
| s4_o01 (the over-flag shape `Array(Map(String, Tuple(size1 UInt8)))`) | identical | none |
| n05 (`Tuple(keys UInt8, values String)`) | identical | none |
| x08 (`JSON(a Tuple(size UInt8, v String))`) | identical | none (both trees keep only `id` and `j:JSON`, DESC-05) |

```text
D994-DIFFERING=n01,n02,n03,n04,r01,s4_a01,s4_a02,s4_a03,s4_a04,s4_a05,s4_a06,s4_a07,s4_a08,s4_a09,s4_a10,s4_a11,s4_a12,s4_a13,x01,x02,x03,x04,x05,x06,x07
```

Result: the two trees differ for exactly the 25 affected keys (`s4_a01`..`s4_a13`, `n01`..`n04`, `x01`..`x07`, `r01`) and for none of the 19 look-alikes, `s4_o01`, `n05` or `x08`. Warnings: on gg3 every one of the 25 differing tables logged exactly one unpaired-element WARN and no table logged an unreadable-parent WARN (the text does not exist there); on gg4 no table of the 47 logged either WARN, `x08` included (its counts, 0 and 0, are the same on both trees). This is the affected set that research R4 and Phase 26 E-02 give; it was measured here, not copied.

Matches text: pending (29-04)

### E-11: one live insert per shape, gg3 versus gg4 (999.4)

Backs: `docs/ROLLOUT-v1.7.0_gg4.md`, the 999.4 items (what an affected shape did on gg3 and does on gg4; the shifted-row shape), written in plan 29-04. Method (`sizeInsert`, same two runs and the same XMLs as E-10): for each of ten shapes the table is created from its catalog DDL before `task.start()`, a `ClickHouseSinkTask` (V2, `exactlyOnce` unset) puts one record of constants at partition 0, offset 0 (topic = table name), and the row is read back with typed SQL (`toString(c)`; for `r01` the tuple element `size` and `hex` of `v`) and compared inside the probe against the record. Records: `s4_a01` element `size` 7 and `v` `ab`; `s4_a04` one such element in the array; `s4_a08` and `s4_a13` the same with `size1` (in `s4_a13` after `v`); `n03` one element with `size0` `p` and `size` `q`; `x01` `size` a Tuple of `a` 1 and `b` `q`; `r01` `size` 7 and `v` the four bytes `abc`, U+0003, `xyz` (the Phase 26 E-08 record); controls `s4_u01` `size1` 7 and `v` `ab`, `s4_u03` one Map entry with key `k` and that Tuple, `s4_u05` `sizes` 7 and `v` `ab`.

```text
PROBE-INSERT tree=gg3 table=s4_a01 outcome=failed code=33 rows=0 match=-
PROBE-INSERT tree=gg3 table=s4_a04 outcome=failed code=33 rows=0 match=-
PROBE-INSERT tree=gg3 table=s4_a08 outcome=failed code=33 rows=0 match=-
PROBE-INSERT tree=gg3 table=s4_a13 outcome=failed code=33 rows=0 match=-
PROBE-INSERT tree=gg3 table=n03 outcome=failed code=32 rows=0 match=-
PROBE-INSERT tree=gg3 table=x01 outcome=failed code=33 rows=0 match=-
PROBE-INSERT tree=gg3 table=r01 outcome=stored code=- rows=1 match=no
PROBE-ROW tree=gg3 table=r01 size=1667391751 vhex=78797A
PROBE-INSERT tree=gg3 table=s4_u01 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg3 table=s4_u03 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg3 table=s4_u05 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg4 table=s4_a01 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg4 table=s4_a04 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg4 table=s4_a08 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg4 table=s4_a13 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg4 table=n03 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg4 table=x01 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg4 table=r01 outcome=stored code=- rows=1 match=yes
PROBE-ROW tree=gg4 table=r01 size=7 vhex=6162630378797A
PROBE-INSERT tree=gg4 table=s4_u01 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg4 table=s4_u03 outcome=stored code=- rows=1 match=yes
PROBE-INSERT tree=gg4 table=s4_u05 outcome=stored code=- rows=1 match=yes
```

| Table (shape) | gg3 | gg4 |
|---------------|-----|-----|
| s4_a01 `Tuple(size UInt16, v String)` | failed, code 33, 0 rows | stored, equals the record |
| s4_a04 `Array(Tuple(size UInt16, v String))` | failed, code 33, 0 rows | stored, equals the record |
| s4_a08 `Array(Tuple(size1 UInt16, v String))` | failed, code 33, 0 rows | stored, equals the record |
| s4_a13 `Array(Tuple(v String, size1 UInt16))` | failed, code 33, 0 rows | stored, equals the record |
| n03 `Array(Tuple(size0 String, size String))` | failed, code 32, 0 rows | stored, equals the record |
| x01 `Tuple(size Tuple(a UInt8, b String))` | failed, code 33, 0 rows | stored, equals the record |
| r01 `Tuple(size UInt32, v String)` | stored one row that differs from the record: `size` 1667391751, `v` hex `78797A` (the three bytes `xyz`) | stored the record: `size` 7, `v` hex `6162630378797A` |
| s4_u01, s4_u03, s4_u05 (controls) | stored, equal to the record | stored, equal to the record |

Verdicts. gg3 to gg4 is an upgrade for these shapes: the six failing shapes stored nothing on gg3 (so no row of them exists from gg3), and `r01` shows that a shorter write which parses stores a wrong value (gg3 writes only the string, so the server reads the `UInt32` size from the first four bytes it finds: the length byte 7 and `abc`, which is 1667391751 in little-endian order, and then reads `xyz` as `v` from the remaining bytes, the length byte 3 and `xyz`), the one case of this kind that was measured. The controls store identically on both. The server codes are the ones this server printed for these records: 33 for five shapes and 32 for `n03`; they are not the assignment per shape that the gg3 note gives (code 32 for `Tuple(size UInt16, v String)`, `Array(Tuple(size UInt16, v String))` and `Array(Tuple(size1 UInt16, v String))`, code 33 for `Array(Tuple(v String, size1 UInt16))`), so a document that names a code per shape would contradict this measurement; the code is 32 or 33, depending on where the parse runs out of bytes, which depends on the record. Neither measurement shows a different outcome class (a failed insert with 0 rows).

Matches text: pending (29-04)

### E-12: in-flight retries across the release change, both directions (999.4, D-04)

Backs: `docs/ROLLOUT-v1.7.0_gg4.md`, the in-flight item and the rollback item for the size-named element fix (PT-10 (a) and (b)), written in plan 29-04. The connector sends `insert_deduplication_token = '<topic>-<partition>-<min>-<max>'` (`QueryIdentifier.getDeduplicationToken`), also with `exactlyOnce=false`; the topic is the table name, so offset range 0-0 on partition 0 gives `<table>-0-0-0`. A row "landed by SQL" is an `INSERT ... SETTINGS insert_deduplication_token = '<table>-0-0-0' VALUES (...)` that the probe runs before the sink task puts the record (the Phase 24 E-12 method); the connector of the other release did not write that row in this measurement, and one server cannot be shared between the two trees' runs. Server `26.9.19.9`, client V2, `errors.tolerance` at its default (`none`), so a failed put throws; the DLQ route was not run. Image as in E-10.

Rollback direction (`sizeRetryRollback`, run in the `v1.7.0_gg3` worktree only). Two tables, each `(id Int32, c <type>) ENGINE = MergeTree ORDER BY id SETTINGS non_replicated_deduplication_window = 100`: `rb_a01` with `c Tuple(size UInt16, v String)` (the affected shape) and `rb_u01` with `c Tuple(size1 UInt8, v String)` (the look-alike). The row the gg4 connector stores for the record (id 1, element 7, `v` `ab`) is landed by SQL:

```sql
INSERT INTO `rb_a01` (id, c) SETTINGS insert_deduplication_token = 'rb_a01-0-0-0' VALUES (1, (7, 'ab'))
INSERT INTO `rb_u01` (id, c) SETTINGS insert_deduplication_token = 'rb_u01-0-0-0' VALUES (1, (7, 'ab'))
```

Then a gg3 `ClickHouseSinkTask` puts that record at partition 0, offset 0.

Upgrade direction (`sizeRetryUpgrade`, run in the gg4 tip worktree only). Two tables `(id Int32, c Tuple(size UInt32, v String)) ENGINE = MergeTree ORDER BY id`: `up_r01_w` with `SETTINGS non_replicated_deduplication_window = 100` and `up_r01_nw` without. The row gg3 stored for `r01` (E-11: `size=1667391751 vhex=78797A`, read from `/tmp/gsd29/probe/r01-gg3-row.txt`) is landed by SQL:

```sql
INSERT INTO `up_r01_w` (id, c) SETTINGS insert_deduplication_token = 'up_r01_w-0-0-0' VALUES (1, (1667391751, unhex('78797A')))
INSERT INTO `up_r01_nw` (id, c) SETTINGS insert_deduplication_token = 'up_r01_nw-0-0-0' VALUES (1, (1667391751, unhex('78797A')))
```

Then a gg4 sink task puts the `r01` record of E-11 (`size` 7, `v` = `abc`, U+0003, `xyz`) at partition 0, offset 0.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd29/wt-gg3/gradlew -p /tmp/gsd29/wt-gg3 --console=plain test --tests '*Phase29RolloutProbe.sizeRetryRollback' --rerun
cp /tmp/gsd29/wt-gg3/build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.Phase29RolloutProbe.xml /tmp/gsd29/exec/d994-retry-gg3.xml
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd29/wt-tip/gradlew -p /tmp/gsd29/wt-tip --console=plain test --tests '*Phase29RolloutProbe.sizeRetryUpgrade' --rerun
cp /tmp/gsd29/wt-tip/build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.Phase29RolloutProbe.xml /tmp/gsd29/exec/d994-retry-gg4.xml
python3 -I /tmp/gsd29-scripts/xml-probe-lines.py /tmp/gsd29/exec/d994-retry-gg3.xml /tmp/gsd29/exec/d994-retry-gg4.xml
```

```text
PROBE-VERSION tree=gg3 version=26.9.19.9
PROBE-LANDED tree=gg3 table=rb_a01 token=rb_a01-0-0-0 rows=1
PROBE-FAILCLASS tree=gg3 table=rb_a01 class=ServerException
PROBE-RETRY tree=gg3 table=rb_a01 landed=gg4-row window=yes outcome=failed code=33 rows=1 value=landed
PROBE-LANDED tree=gg3 table=rb_u01 token=rb_u01-0-0-0 rows=1
PROBE-RETRY tree=gg3 table=rb_u01 landed=gg4-row window=yes outcome=dropped code=- rows=1 value=landed
PROBE-VERSION tree=gg4 version=26.9.19.9
PROBE-LANDED tree=gg4 table=up_r01_w token=up_r01_w-0-0-0 rows=1
PROBE-RETRY tree=gg4 table=up_r01_w landed=gg3-row window=yes outcome=dropped code=- rows=1 value=landed
PROBE-LANDED tree=gg4 table=up_r01_nw token=up_r01_nw-0-0-0 rows=1
PROBE-RETRY tree=gg4 table=up_r01_nw landed=gg3-row window=no outcome=stored code=- rows=2 value=both
```

(Both XMLs: `tests="1" skipped="0" failures="0" errors="0"`.) `value=landed` means the table holds only the row landed by SQL; `value=both` means that row and the row of the record.

Verdicts, worded from these lines only:

- **Rollback (gg4 to gg3).** For the affected shape the gg3 retry of a range whose full row had already landed fails with server code 33 (`ServerException`, the same parse failure as an insert of E-11) and leaves exactly the one landed row: the server rejects the shorter bytes before the token decides anything, so the put throws although the row is in the table. For the look-alike `Tuple(size1 UInt8, v String)` the gg3 bytes are identical to gg4's (E-11), and the retry is dropped by its token with one row left. This confirms research R4's mechanism (a parse failure, not a deduplication miss); the code on this record is 33 (E-11 shows 32 or 33 depending on the shape and record).
- **Upgrade (gg3 to gg4), failing shapes.** The shapes that failed on gg3 stored nothing (E-11, rows 0), so there is no earlier row for a gg4 retry to meet; it stores the row once.
- **Upgrade (gg3 to gg4), the shape that stored a shifted row.** With the gg3 `r01` row landed under the token, the gg4 retry of the same range is dropped on a table with a deduplication window (outcome `dropped`, one row, `value=landed`): the shifted row stays and the correct row is not stored. On a table without a window the retry is stored as a second row (two rows, `value=both`): the shifted row and the correct one. Whether a window applies is the table's own setting; no window is assumed here.

Matches text: pending (29-04)

### E-13: the size-element suspects query on the catalog and on look-alikes (999.4)

Backs: `docs/ROLLOUT-v1.7.0_gg4.md`, the 999.4 item's "Am I affected?" query and its over-flag sentence, written in plan 29-04. The query is the one of `docs/ROLLOUT-v1.7.0_gg3.md` item 2, copied byte for byte (lines 130-138 of that note, `/tmp/gsd29/probe/sql/size-suspects.sql`; `cmp` against `sed -n 130,138p` of the note printed no difference). One read-only `SELECT` over `system.columns`; `<db>` is its only placeholder:

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

Method (`sizeQuery`, run in the gg4 tip worktree, server `26.9.19.9`, client V2, image as in E-10; XML `/tmp/gsd29/exec/d994-query.xml`, `tests="1" skipped="0" failures="0" errors="0"`): the probe creates the 47 catalog tables in the database `p29_fixture` and the 19 look-alikes `s4_u01`..`s4_u19` plus `n05` in the database `p29_lookalike`, and runs the query with only the trailing `;` removed and `<db>` replaced by `p29_fixture` and `p29_lookalike` respectively. Substitution map: `<db>` = `p29_fixture` for the first result, `p29_lookalike` for the second.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd29/wt-tip/gradlew -p /tmp/gsd29/wt-tip --console=plain test --tests '*Phase29RolloutProbe.sizeQuery' --rerun
cp /tmp/gsd29/wt-tip/build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.Phase29RolloutProbe.xml /tmp/gsd29/exec/d994-query.xml
python3 -I /tmp/gsd29-scripts/xml-probe-lines.py /tmp/gsd29/exec/d994-query.xml
```

```text
PROBE-VERSION tree=gg4 version=26.9.19.9
PROBE-SIZEQ tree=gg4 db=fixture tables=n01,n02,n03,n04,r01,s4_a01,s4_a02,s4_a03,s4_a04,s4_a05,s4_a06,s4_a07,s4_a08,s4_a09,s4_a10,s4_a11,s4_a12,s4_a13,s4_o01,x01,x02,x03,x04,x05,x06,x07,x08
PROBE-SIZEQ tree=gg4 db=lookalike tables=
```

```text
SIZEQ-OVERFLAG=s4_o01,x08
```

Result. On the catalog database the query lists 27 tables: the 25 of `D994-DIFFERING` (E-10) and two over-flags, `s4_o01` (`Array(Map(String, Tuple(size1 UInt8)))`, the one the gg3 note names) and `x08` (`JSON(a Tuple(size UInt8, v String))`: the type text holds `(size UInt8`, but a JSON column's typed paths are dropped from the table description by design, so its Column tree is identical on gg3 and gg4 and neither logs the WARN, E-10). It lists none of the 19 `s4_u` look-alikes and nothing for `n05`, and it returns an empty list on the database that holds only `s4_u01`..`s4_u19` and `n05`, which is what the note's "Empty result" sentence needs. The query reads the declared type text only, so a table it lists is a suspect, not a finding: of the 27, 25 differ between the releases and 2 do not.

Matches text: pending (29-04)

### E-14: WARN greps for the size-element fix

Backs: `docs/ROLLOUT-v1.7.0_gg4.md`, the 999.4 item's log checks (the unpaired-element WARN of a gg3 connector, and the unreadable-parent WARN that only gg4 can write), written in plan 29-04. The two commands in doc form, each with `<connector-log-file>` as its only placeholder:

```sh
grep -F 'have no matching DESCRIBE sub-column' <connector-log-file>
```

```sh
grep -F 'Could not read the declared type' <connector-log-file>
```

Scratch files (never committed): `/tmp/gsd29/logs/connect.log` holds four lines: an unrelated INFO line, the unpaired-element WARN message captured from the real gg3 run of E-10 (`PROBE-WARNLINE`, the probe's doubled backslash turned back into the single backslash and `n` that the connector's message holds), a line with the text of `DescribeTableBuilder.UNREADABLE_PARENT_WARNING` with each `{}` replaced by a placeholder word (`T`, `col`, `db`, `tbl`, `reason`; this line was built from the source constant, not captured, because E-10 shows that no catalog table logs it), and a second unrelated INFO line. `/tmp/gsd29/logs-empty/connect.log` holds the two unrelated lines only. Both commands were run on each file from `/tmp/gsd29`:

```sh
grep -F 'have no matching DESCRIBE sub-column' logs/connect.log; echo "exit=$?"
grep -F 'Could not read the declared type' logs/connect.log; echo "exit=$?"
grep -F 'have no matching DESCRIBE sub-column' logs-empty/connect.log; echo "exit=$?"
grep -F 'Could not read the declared type' logs-empty/connect.log; echo "exit=$?"
```

```text
Declared Tuple element(s) [c.size] of column c of table p29_desc.s4_a01 (declared type Tuple(\n    size UInt16,\n    v String)) have no matching DESCRIBE sub-column; applying the declared nullability to the elements that paired
exit=0
Could not read the declared type T of column col of table db.tbl (reason); its sub-columns named size, size0 or size followed by digits are treated as structural, as earlier releases did
exit=0
exit=1
exit=1
```

Result. On the file that holds the lines, the first command printed exactly the unpaired-element line and the second exactly the unreadable-parent line (exit 0 each, and neither printed the other's line or an unrelated line); on the file with the unrelated lines only, both printed nothing (exit 1). The unpaired-element line is a real message of the gg3 connector (E-10); on gg4 neither WARN was logged for any of the 47 catalog tables (E-10), so on a gg4 log both commands are expected to print nothing for these shapes. The unreadable-parent line has been shown to match its grep, not to occur in a run.

Matches text: pending (29-04)

### E-15: alert-rule and config greps for the writer-text changes

Backs: `docs/ROLLOUT-v1.7.0_gg4.md`, the items on the writer failure texts that gg4 no longer produces (ledger rows L-52 and L-54: a Jackson message with `through reference chain` or `No serializer found` on the JSON-as-string route, and `NullPointerException` for an unknown Enum label) and on the connectors that use the JSON-as-string route (written in plan 29-04). The two commands in doc form, each with its directory as the only placeholder:

```sh
grep -rnF -e 'through reference chain' -e 'No serializer found' -e 'NullPointerException' <alert-rule-directory>
```

```sh
grep -rnF 'input_format_binary_read_json_as_string' <connector-config-directory>
```

Scratch directories (never committed). `/tmp/gsd29/alerts/` holds five rule files: `rule-reference-chain.yml`, `rule-no-serializer.yml` and `rule-npe.yml` (one matcher each for `through reference chain`, `No serializer found` and `NullPointerException`), `rule-unrelated.yml` (a matcher on `Task is being killed`), and `rule-escaped.yml`, whose matcher writes the first text with `\s` for each space (`through\sreference\schain`). `/tmp/gsd29/alerts-empty/` holds `rule-unrelated.yml` only. `/tmp/gsd29/conncfg/` holds `connector-a.properties` (`clickhouseSettings=input_format_binary_read_json_as_string=1`), `connector-b.json` (`"clickhouseSettings": "input_format_binary_read_json_as_string=true"`) and `connector-c.properties` (`clickhouseSettings=async_insert=0`); `/tmp/gsd29/conncfg-empty/` holds `connector-c.properties` only. Run from `/tmp/gsd29`:

```sh
grep -rnF -e 'through reference chain' -e 'No serializer found' -e 'NullPointerException' alerts; echo "exit=$?"
grep -rnF -e 'through reference chain' -e 'No serializer found' -e 'NullPointerException' alerts-empty; echo "exit=$?"
grep -rnF 'input_format_binary_read_json_as_string' conncfg; echo "exit=$?"
grep -rnF 'input_format_binary_read_json_as_string' conncfg-empty; echo "exit=$?"
```

```text
alerts/rule-no-serializer.yml:2:expr: log_line =~ "No serializer found"
alerts/rule-reference-chain.yml:2:expr: log_line =~ "through reference chain"
alerts/rule-npe.yml:2:expr: log_line =~ "NullPointerException"
exit=0
exit=1
conncfg/connector-a.properties:3:clickhouseSettings=input_format_binary_read_json_as_string=1
conncfg/connector-b.json:4:    "clickhouseSettings": "input_format_binary_read_json_as_string=true"
exit=0
exit=1
```

Result. The alert-rule command listed each of the three texts once (one line per rule file, in the order the directory is read), printed nothing for the directory that held only the unrelated rule (exit 1), and did not list `rule-escaped.yml`, whose matcher writes the space as `\s`: the search compares literal text, so a rule that writes a character of these texts with a regular-expression escape is not found and has to be read by hand. The config command listed both files that set the option (the `1` and the `true` form) and not the unrelated configuration, and printed nothing for the directory that held only the unrelated one (exit 1).

Matches text: pending (29-04)
