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

Matches text: yes

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

Matches text: yes

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

Matches text: yes

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

Matches text: yes

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

Matches text: yes

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

Matches text: yes (the close record states the same label, PR and freeze facts; ops04 stays open by nature and is re-read in 29-07)

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

Matches text: yes

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

Matches text: yes

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

Matches text: yes

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

Matches text: yes

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

Matches text: yes

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

Matches text: yes

### E-20: names and enum labels, gg3 versus gg4 (999.5)

Backs: `docs/ROLLOUT-v1.7.0_gg4.md`, the raw-identifier item (which tables and enum labels change, the rename precondition, the scope of the detection queries), written in plans 29-04 and 29-05. Method: the probe methods `nameDescribe` and `labelPut` (scratch `/tmp/gsd29/probe/Phase29RolloutProbe.java`, never committed) create, for each of the twelve name entries of `RawNameMatrix` (`ctl`, `bs`, `bsn`, `lf`, `tab`, `cr`, `sl`, `dq`, `bt`, `na`, `ap`, `btbsn`, character sequence `<c>`), a root table `n5_<e>_root (id Int32, <r<c>z> String DEFAULT 'DEFAULTED')`, an element table `n5_<e>_tuple (id Int32, t Tuple(<x<c>y> Int32, d String))` and an enum table `n5_<e>_enum (id Int32, e Enum8('<x<c>y>' = 1, 'z' = 2))`, all `ENGINE = MergeTree ORDER BY id` (36 tables, names backtick-quoted and labels single-quoted with ClickHouse SQL escaping by the probe's own helper, the helper has the same rules as `RawNameMatrix.sqlQuoted` in `RawNameMatrix.java`: a backslash and the quote character get a backslash in front, line feed, tab and carriage return are written `\n`, `\t` and `\r`). `nameDescribe` describes each through the production `ClickHouseHelperClient.describeTable` (client V2) in a fresh database and prints one `PROBE-COL` line per Column node, or `PROBE-UNMAPPED`; `labelPut` starts a V2 `ClickHouseSinkTask`, puts one record whose `e` is the raw label `x<c>y` (partition 0, offset 0) and reads back `CAST(e, 'Int8')`. The same class ran unchanged in the `v1.7.0_gg3` worktree (`/tmp/gsd29/wt-gg3`, `3376a18f`) and in the gg4 worktree (`/tmp/gsd29/wt-tip`, detached at `46b2cf15`; `git diff --stat 46b2cf15 HEAD` outside `.planning` is empty, so its `src/` is the tree of the current HEAD). Each run had its own container: server `26.9.19.9`, client V2, image `sha256:bdb5c911a23d92fd364e246ab20b5a0a485f00112f3663fa626b083fb46c146b` created 2026-10-14T18:05:21Z (the floating `latest`, as in E-10).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd29/wt-tip/gradlew -p /tmp/gsd29/wt-tip --console=plain test --tests '*Phase29RolloutProbe.nameDescribe' --tests '*Phase29RolloutProbe.labelPut' --tests '*Phase29RolloutProbe.nameRetryUpgrade' --rerun
cp /tmp/gsd29/wt-tip/build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.Phase29RolloutProbe.xml /tmp/gsd29/exec/n5-gg4.xml
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd29/wt-gg3/gradlew -p /tmp/gsd29/wt-gg3 --console=plain test --tests '*Phase29RolloutProbe.nameDescribe' --tests '*Phase29RolloutProbe.labelPut' --tests '*Phase29RolloutProbe.nameRetryRollback' --rerun
cp /tmp/gsd29/wt-gg3/build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.Phase29RolloutProbe.xml /tmp/gsd29/exec/n5-gg3.xml
python3 -I /tmp/gsd29-scripts/xml-probe-lines.py /tmp/gsd29/exec/n5-gg3.xml /tmp/gsd29/exec/n5-gg4.xml
```

Both runs ended `BUILD SUCCESSFUL` (XML `tests="3" skipped="0" failures="0" errors="0"` each; 182 `PROBE-COL` lines over both trees). Names below are in the escaped print form of the probe: a backslash is printed doubled and a line feed, tab and carriage return as `\n`, `\t`, `\r`, the same convention as the handoff table of `28-BEHAVIOUR-LEDGER.md` (so `r\\z` is the name `r`, two backslashes, `z`, and `r\z` the name `r`, one backslash, `z`). The names are the column name of the root table and the Tuple element name (without the `t.` prefix) of the element table.

| table | gg3 name | gg4 name | tree |
|-------|----------|----------|------|
| `n5_ctl_root` | `r_z` | `r_z` | same |
| `n5_ctl_tuple` | `t.x_y` | `t.x_y` | same |
| `n5_bs_root` | `r\\\\z` | `r\\z` | differs |
| `n5_bs_tuple` | `t.x\\\\y` | `t.x\\y` | differs |
| `n5_bsn_root` | `r\\\\nz` | `r\\nz` | differs |
| `n5_bsn_tuple` | `t.x\\\\ny` | `t.x\\ny` | differs |
| `n5_lf_root` | `r\\nz` | `r\nz` | differs |
| `n5_lf_tuple` | `t.x\\ny` | `t.x\ny` | differs |
| `n5_tab_root` | `r\\tz` | `r\tz` | differs |
| `n5_tab_tuple` | `t.x\\ty` | `t.x\ty` | differs |
| `n5_cr_root` | `r\\rz` | `r\rz` | differs |
| `n5_cr_tuple` | `t.x\\ry` | `t.x\ry` | differs |
| `n5_sl_root` | `r\\/z` | `r/z` | differs |
| `n5_sl_tuple` | `t.x\\/y` | `t.x/y` | differs |
| `n5_dq_root` | UNMAPPED | `r"z` | differs |
| `n5_dq_tuple` | UNMAPPED | `t.x"y` | differs |
| `n5_bt_root` | `r`z` | `r`z` | same |
| `n5_bt_tuple` | `t.x`y` | `t.x`y` | same |
| `n5_na_root` | `r\u00E9\u65E5\u672Cz` | `r\u00E9\u65E5\u672Cz` | same |
| `n5_na_tuple` | `t.x\u00E9\u65E5\u672Cy` | `t.x\u00E9\u65E5\u672Cy` | same |
| `n5_ap_root` | `r'z` | `r'z` | same |
| `n5_ap_tuple` | `t.x'y` | `t.x'y` | same |
| `n5_btbsn_root` | `r`\\\\nz` | `r`\\nz` | differs |
| `n5_btbsn_tuple` | `t.x`\\\\ny` | `t.x`\\ny` | differs |

The root and element tables of `ctl`, `bt`, `na` and `ap` have the same names on both trees; the 16 tables of `bs`, `bsn`, `lf`, `tab`, `cr`, `sl`, `dq` and `btbsn` differ. The two `dq` tables are not mapped at all on gg3 (`PROBE-UNMAPPED`), which counts as different; on gg4 they are mapped and carry the raw double quote. The remaining 20 tables (the enum tables and the four unchanged entries) show no difference in their column names; the enum labels are in the next table.

Enum labels (`labelPut` and the enum keys that `describeTable` builds; the key is the first label of the table, `z` is the second):

| entry | character | gg3 label key | gg4 label key | gg3 raw-label put | gg4 raw-label put | outcome |
|-------|-----------|---------------|---------------|-------------------|-------------------|---------|
| `ctl` | underscore (control) | `x_y` | `x_y` | stored (stored 1) | stored (stored 1) | same |
| `bs` | backslash | `x\\\\\\\\y` | `x\\y` | failed (stored -) | stored (stored 1) | differs |
| `bsn` | backslash, `n` | `x\\\\\\\\ny` | `x\\ny` | failed (stored -) | stored (stored 1) | differs |
| `lf` | line feed | `x\\\\ny` | `x\ny` | failed (stored -) | stored (stored 1) | differs |
| `tab` | tab | `x\\\\ty` | `x\ty` | failed (stored -) | stored (stored 1) | differs |
| `cr` | carriage return | `x\\\\ry` | `x\ry` | failed (stored -) | stored (stored 1) | differs |
| `sl` | slash | `x\\/y` | `x/y` | failed (stored -) | stored (stored 1) | differs |
| `dq` | double quote | UNMAPPED | `x"y` | failed (stored -) | stored (stored 1) | differs |
| `bt` | backtick | `x`y` | `x`y` | stored (stored 1) | stored (stored 1) | same |
| `na` | U+00E9 U+65E5 U+672C | `x\u00E9\u65E5\u672Cy` | `x\u00E9\u65E5\u672Cy` | stored (stored 1) | stored (stored 1) | same |
| `ap` | apostrophe | UNMAPPED | `x'y` | failed (stored -) | stored (stored 1) | differs |
| `btbsn` | backtick, backslash, `n` | `x`\\\\\\\\ny` | `x`\\ny` | failed (stored -) | stored (stored 1) | differs |

A put whose outcome is `failed` on gg3 stored 0 rows. For `bs`, `bsn`, `lf`, `tab`, `cr`, `sl` and `btbsn` the put failed with `WithheldMessageException` (`(message withheld: may contain record data)`, the Phase 27 lookup-miss form). For `dq` and `ap` the table is not mapped on gg3 and the put failed with `RuntimeException` `Connection to ClickHouse is not active.`; this text names a connection, not the label, so an operator who reads it should look for an unmapped table. On gg4 all twelve raw labels stored code 1.

D995-DIFFERING=n5_bs_root,n5_bs_tuple,n5_bsn_root,n5_bsn_tuple,n5_btbsn_root,n5_btbsn_tuple,n5_cr_root,n5_cr_tuple,n5_dq_root,n5_dq_tuple,n5_lf_root,n5_lf_tuple,n5_sl_root,n5_sl_tuple,n5_tab_root,n5_tab_tuple
LABEL-DIFFERING=ap,bs,bsn,btbsn,cr,dq,lf,sl,tab

Matches text: yes

### E-21: in-flight retries across the release change for the backslash entry (999.5, D-04)

Backs: `docs/ROLLOUT-v1.7.0_gg4.md`, the in-flight item for raw identifier names (PT-10 (c)), written in plan 29-04. Measured for the `bs` entry (one backslash) on client V2 only; the other eleven entries and client V1 were not run. The connector sends `insert_deduplication_token = '<topic>-<partition>-<min>-<max>'` (E-12), so a record at partition 0, offset 0 for the table `<table>` carries the token `<table>-0-0-0`. A row "landed by SQL" is an `INSERT ... SETTINGS insert_deduplication_token = '<table>-0-0-0' VALUES (...)` that the probe runs before the sink task puts the record; the connector of the other release did not write that row, and one server cannot be shared between the two trees' runs. Server `26.9.19.9`, client V2, `errors.tolerance` at its default (`none`), so a failed put throws; the DLQ route was not run. Image as in E-20.

Tables (`nameRetryUpgrade` in the gg4 worktree, `nameRetryRollback` in the `v1.7.0_gg3` worktree; the same Gradle runs as E-20). The tables are created with the probe's `shapeDdl` (the root and tuple shapes of E-20 for the `bs` entry; the statement text, as sent, escapes the backslash of the name with a second one):

```sql
CREATE TABLE up_bs_root_w (id Int32, `r\\z` String DEFAULT 'DEFAULTED') ENGINE = MergeTree ORDER BY id SETTINGS non_replicated_deduplication_window = 100
CREATE TABLE up_bs_tuple_w (id Int32, t Tuple(`x\\y` Int32, d String)) ENGINE = MergeTree ORDER BY id SETTINGS non_replicated_deduplication_window = 100
```

The server reads `\\` in a quoted identifier as one backslash, so the root column is named `r`, backslash, `z`. `_w` tables carry `SETTINGS non_replicated_deduplication_window = 100`, `_nw` tables have no window. The record is `id` 1 and, for a root table, the field named as below with the value `V1`; for an element table, `t` with the element named as below, value 7, and `d` `d`.

- Upgrade (gg3 to gg4): the field name is the old V2 form, the doubled form (E-20, the gg3 column): root `r\\z`, element `x\\y` (two backslashes each). `up_bs_root_w` and `up_bs_tuple_w` first receive the row the old release stored, by SQL (`(1, 'V1')` and `(1, (7, 'd'))`); `up_bs_root_nw` receives nothing.
- Rollback (gg4 to gg3): the field name is the raw form, root `r\z`, element `x\y` (one backslash each). `rb_bs_root_w` and `rb_bs_tuple_w` first receive the row the new release stores, by SQL (the same values); `rb_bs_root_nw` receives nothing.

```sh
python3 -I /tmp/gsd29-scripts/xml-probe-lines.py /tmp/gsd29/exec/n5-gg4.xml /tmp/gsd29/exec/n5-gg3.xml
```

(The commands that produced the two XML files are those of E-20.) Field names and failure texts are printed in the probe's escaped form, a backslash doubled, so `r\\z` in a `PROBE-RETRYFIELD` line is the one-backslash name and `r\\\\z` the two-backslash name.

```text
PROBE-VERSION tree=gg4 version=26.9.19.9
PROBE-LANDED tree=gg4 table=up_bs_root_w token=up_bs_root_w-0-0-0 rows=1
PROBE-RETRYFIELD tree=gg4 table=up_bs_root_w field=r\\\\z
PROBE-RETRYROW tree=gg4 table=up_bs_root_w row=V1
PROBE-RETRY tree=gg4 table=up_bs_root_w landed=gg3-row window=yes outcome=dropped code=- rows=1 value=landed
PROBE-LANDED tree=gg4 table=up_bs_tuple_w token=up_bs_tuple_w-0-0-0 rows=1
PROBE-RETRYFIELD tree=gg4 table=up_bs_tuple_w field=x\\\\y
PROBE-FAILCLASS tree=gg4 table=up_bs_tuple_w class=DataException
PROBE-FAILMSG tree=gg4 table=up_bs_tuple_w message=column 't.x\\y' (INT32): no value provided for a non-nullable column without a default; add the field, make the column Nullable, or give it a DEFAULT
PROBE-RETRYROW tree=gg4 table=up_bs_tuple_w row=(7,'d')
PROBE-RETRY tree=gg4 table=up_bs_tuple_w landed=gg3-row window=yes outcome=failed code=- rows=1 value=landed
PROBE-RETRYFIELD tree=gg4 table=up_bs_root_nw field=r\\\\z
PROBE-RETRYROW tree=gg4 table=up_bs_root_nw row=DEFAULTED
PROBE-RETRY tree=gg4 table=up_bs_root_nw landed=none window=no outcome=stored code=- rows=1 value=default
PROBE-VERSION tree=gg3 version=26.9.19.9
PROBE-LANDED tree=gg3 table=rb_bs_root_w token=rb_bs_root_w-0-0-0 rows=1
PROBE-RETRYFIELD tree=gg3 table=rb_bs_root_w field=r\\z
PROBE-RETRYROW tree=gg3 table=rb_bs_root_w row=V1
PROBE-RETRY tree=gg3 table=rb_bs_root_w landed=gg4-row window=yes outcome=dropped code=- rows=1 value=landed
PROBE-LANDED tree=gg3 table=rb_bs_tuple_w token=rb_bs_tuple_w-0-0-0 rows=1
PROBE-RETRYFIELD tree=gg3 table=rb_bs_tuple_w field=x\\y
PROBE-FAILCLASS tree=gg3 table=rb_bs_tuple_w class=DataException
PROBE-FAILMSG tree=gg3 table=rb_bs_tuple_w message=column 't.x\\\\y' (INT32): no value provided for a non-nullable column without a default; add the field, make the column Nullable, or give it a DEFAULT
PROBE-RETRYROW tree=gg3 table=rb_bs_tuple_w row=(7,'d')
PROBE-RETRY tree=gg3 table=rb_bs_tuple_w landed=gg4-row window=yes outcome=failed code=- rows=1 value=landed
PROBE-RETRYFIELD tree=gg3 table=rb_bs_root_nw field=r\\z
PROBE-RETRYROW tree=gg3 table=rb_bs_root_nw row=DEFAULTED
PROBE-RETRY tree=gg3 table=rb_bs_root_nw landed=none window=no outcome=stored code=- rows=1 value=default
```

`value=landed` means the table holds only the row landed by SQL; `value=default` that the stored row holds the root column's `DEFAULT`.

Verdicts, worded from these lines only:

- **Upgrade (gg3 to gg4), a batch retried under the old name form.** Root field, table with a window and the old release's row landed: the retry is dropped by the token, one row remains, and it is the landed one (`outcome=dropped`, `value=landed`). Element field, same setup: the put fails with `DataException` (`column 't.x\\y' (INT32): no value provided for a non-nullable column without a default; ...`), and the landed row remains (`outcome=failed`, `rows=1`). Root field, table without a window and nothing landed: the retry stores one row that holds `DEFAULTED` (`outcome=stored`, `value=default`).
- **Rollback (gg4 to gg3), a batch retried under the raw name form.** The same three outcomes in the same order: dropped with the landed row kept, failed with `DataException` on the element (`column 't.x\\\\y'` in the escaped print form) with the landed row kept, and one `DEFAULTED` row on the table without a window.
- Both directions: a name that does not match is not an error for a root column. With a window the token drops the batch before the default matters; without one the default is stored.

Matches text: yes

### E-22: the escaped-name queries, gg3 unchanged and the companion (999.5, PT-04)

Backs: `docs/ROLLOUT-v1.7.0_gg4.md`, the raw-identifier item's "Am I affected?" (which tables the rename precondition concerns), written in plan 29-05. Two read-only `SELECT`s over `system.columns`, `<db>` is the only placeholder of each. The first is the query of `docs/ROLLOUT-v1.7.0_gg3.md` item 3 (the second `sql` block, lines 203-214 of that note), copied byte for byte into `/tmp/gsd29/probe/sql/escaped-names.sql` (`cmp` against `sed -n 203,214p` of the note printed no difference). The second, the companion, lists a column whose name holds a carriage return or a double quote, or whose declared type text (single-quoted strings blanked first, the gg3 idiom) has a backtick-quoted name that holds a carriage-return escape or a double quote:

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

```sql
SELECT `table`, name AS `column`, type
FROM system.columns
WHERE database = '<db>'
  AND (
        position(name, '\r') > 0
        OR position(name, '"') > 0
        OR match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''), '`[^`]*(\\\\r|")[^`]*`')
      )
ORDER BY `table`;
```

Method (`nameQueries`, run in the gg4 worktree, server `26.9.19.9`, client V2, image as in E-20; XML `/tmp/gsd29/exec/n5-query.xml`, `tests="2" skipped="0" failures="0" errors="0"` together with `controlChars`): the probe creates the 36 `n5_` tables of E-20 and the six look-alikes in the database `p29_n5q` (the fixture database). The look-alike database `p29_n5l` holds the `ctl` and `na` tables (root, tuple, enum), the `bt` root and enum tables, the `ap` root table and the six look-alikes. The `bt` and `ap` Tuple tables stay in the fixture database only, because the server writes a backtick inside a quoted element name with a backslash escape, so the gg3 query may list them. The six look-alikes: `la_dot` (a root column named `a.b`), `la_comment` (`s String COMMENT` holding a backslash), `la_default` (`s String DEFAULT 'x/y'`), `la_tz` (`t DateTime64(3, 'Europe/Berlin')`), `la_enum` (`e Enum8('x_y' = 1)`) and `la_plain` (plain names, a Tuple with `c` and `d`). Each query ran with only `<db>` replaced and its trailing `;` removed. Substitution map: `<db>` = `p29_n5q` for the `db=fixture` results, `p29_n5l` for the `db=lookalike` results.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd29/wt-tip/gradlew -p /tmp/gsd29/wt-tip --console=plain test --tests '*Phase29RolloutProbe.nameQueries' --tests '*Phase29RolloutProbe.controlChars' --rerun
cp /tmp/gsd29/wt-tip/build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.Phase29RolloutProbe.xml /tmp/gsd29/exec/n5-query.xml
python3 -I /tmp/gsd29-scripts/xml-probe-lines.py /tmp/gsd29/exec/n5-query.xml
```

```text
PROBE-NAMEQ tree=gg4 query=gg3 db=fixture tables=n5_bs_root,n5_bs_tuple,n5_bsn_root,n5_bsn_tuple,n5_bt_tuple,n5_btbsn_root,n5_btbsn_tuple,n5_cr_tuple,n5_lf_root,n5_lf_tuple,n5_sl_root,n5_sl_tuple,n5_tab_root,n5_tab_tuple
PROBE-NAMEQ tree=gg4 query=gg3 db=lookalike tables=
PROBE-NAMEQ tree=gg4 query=companion db=fixture tables=n5_cr_root,n5_cr_tuple,n5_dq_root,n5_dq_tuple
PROBE-NAMEQ tree=gg4 query=companion db=lookalike tables=
```

```text
NAMEQ-OVERFLAG=n5_bt_tuple
```

Result. The gg3 query lists 14 fixture tables: 13 of the 16 differing tables of `D995-DIFFERING` (E-20), and the over-flag `n5_bt_tuple` (a Tuple element name holding a backtick; the table is not different between the releases). It does not list the root carriage-return table `n5_cr_root` and the two double-quote tables `n5_dq_root` and `n5_dq_tuple`. The companion lists `n5_cr_root`, `n5_cr_tuple`, `n5_dq_root` and `n5_dq_tuple`: the three tables the gg3 query misses, plus `n5_cr_tuple`, which both queries list. The union of the two lists is the 16 differing tables and `n5_bt_tuple`. Both queries return nothing on the look-alike database. `n5_ap_tuple` (an apostrophe in an element name) is listed by neither query, and `n5_bt_tuple` is the only over-flag of either. The queries read the declared type text, so a table they list is a suspect, not a finding: of the 17 tables in the union, 16 differ between the releases and one does not.

Matches text: yes

### E-23: the enum-label query (PT-05)

Backs: `docs/ROLLOUT-v1.7.0_gg4.md`, the raw-identifier item's label paragraph, written in plans 29-04 and 29-05. There was no label query before this plan. The query lists a column whose type holds `Enum8(` or `Enum16(` and contains a single-quoted string with a backslash, a double quote or a slash (the characters the server leaves unchanged or escapes in a label, E-20); `extractAll` returns the text between the quotes of each single-quoted string. It was written and run once, and no refinement was needed. Same fixture and look-alike databases and substitution map as E-22:

```sql
SELECT `table`, name AS `column`, type
FROM system.columns
WHERE database = '<db>'
  AND match(type, 'Enum(8|16)\\(')
  AND arrayExists(label -> match(label, '[\\\\"/]'), extractAll(type, '\'((?:[^\'\\\\]|\\\\.)*)\''))
ORDER BY `table`;
```

```text
PROBE-NAMEQ tree=gg4 query=label db=fixture tables=n5_ap_enum,n5_bs_enum,n5_bsn_enum,n5_btbsn_enum,n5_cr_enum,n5_dq_enum,n5_lf_enum,n5_sl_enum,n5_tab_enum
PROBE-NAMEQ tree=gg4 query=label db=lookalike tables=
```

```text
LABELQ-OVERFLAG=
```

Result. On the fixture database the query lists exactly the nine enum tables of `LABEL-DIFFERING` (E-20): `n5_ap_enum`, `n5_bs_enum`, `n5_bsn_enum`, `n5_btbsn_enum`, `n5_cr_enum`, `n5_dq_enum`, `n5_lf_enum`, `n5_sl_enum` and `n5_tab_enum`. It lists none of the `ctl`, `bt` and `na` enum tables, which have no second form, and no look-alike (`la_enum`, `la_tz`, whose time zone holds a slash outside an Enum, and the rest); it returns nothing on the look-alike database. There are no over-flags (the line above is empty on purpose). The query reads the declared type text only, so it is a suspect list: it was run against `Enum8` tables only, not against an `Enum16`, a `Nullable(Enum8(...))` or an enum nested in a Tuple.

Matches text: yes

### E-24: control characters in labels and element names (28-REVIEW IN-04 server half, PT-07)

Backs: `docs/FORK-DELTA.md` Known costs item 11 (what the server writes, written in plan 29-05) and the raw-identifier item's "not measured" sentence. Method (`controlChars`, gg4 worktree, same run as E-22): for each of four control characters, a table `n5ctl_label_<c> (id Int32, e Enum8('a<c>b' = 1, 'z' = 2))` and a table `n5ctl_el_<c> (id Int32, t Tuple(`a<c>b` Int32))`, created with the SQL escapes `\b` (`bsp`, backspace), `\f` (`ff`, form feed), `\0` (`nul`) and `\x07` (`bel`); the probe reads `system.columns.type`, the rows of `DESCRIBE TABLE ... FORMAT JSONEachRow` with `describe_include_subcolumns = 1` that belong to the column (the text as received, a raw control character shown as `<U+XXXX>`), and whether the production `describeTable` (client V2) maps the table. This differs from the plan text in one point: there is one label table per character instead of one table with four labels, so that a creation failure of one character cannot hide the others. No `CREATE` failed (A8 did not occur; the server accepted a NUL in a label and in an element name).

The eight `PROBE-CTL` lines as printed:

```text
PROBE-CTL tree=gg4 kind=label char=bsp created=yes type=Enum8('a\bb' = 1, 'z' = 2) describe={"name":"e","type":"Enum8('a\\bb' = 1, 'z' = 2)","default_type":"","default_expression":"","comment":"","codec_expression":"","ttl_expression":"","is_subcolumn":0} mapped=yes
PROBE-CTL tree=gg4 kind=label char=ff created=yes type=Enum8('a\fb' = 1, 'z' = 2) describe={"name":"e","type":"Enum8('a\\fb' = 1, 'z' = 2)","default_type":"","default_expression":"","comment":"","codec_expression":"","ttl_expression":"","is_subcolumn":0} mapped=yes
PROBE-CTL tree=gg4 kind=label char=nul created=yes type=Enum8('a\0b' = 1, 'z' = 2) describe={"name":"e","type":"Enum8('a\\0b' = 1, 'z' = 2)","default_type":"","default_expression":"","comment":"","codec_expression":"","ttl_expression":"","is_subcolumn":0} mapped=yes
PROBE-CTL tree=gg4 kind=label char=bel created=yes type=Enum8('a<U+0007>b' = 1, 'z' = 2) describe={"name":"e","type":"Enum8('a\u0007b' = 1, 'z' = 2)","default_type":"","default_expression":"","comment":"","codec_expression":"","ttl_expression":"","is_subcolumn":0} mapped=yes
PROBE-CTL tree=gg4 kind=element char=bsp created=yes type=Tuple(`a\bb` Int32) describe={"name":"t","type":"Tuple(\n    `a\\bb` Int32)","default_type":"","default_expression":"","comment":"","codec_expression":"","ttl_expression":"","is_subcolumn":0} || {"name":"t.a\bb","type":"Int32","default_type":"","default_expression":"","comment":"","codec_expression":"","ttl_expression":"","is_subcolumn":1} mapped=yes
PROBE-CTL tree=gg4 kind=element char=ff created=yes type=Tuple(`a\fb` Int32) describe={"name":"t","type":"Tuple(\n    `a\\fb` Int32)","default_type":"","default_expression":"","comment":"","codec_expression":"","ttl_expression":"","is_subcolumn":0} || {"name":"t.a\fb","type":"Int32","default_type":"","default_expression":"","comment":"","codec_expression":"","ttl_expression":"","is_subcolumn":1} mapped=yes
PROBE-CTL tree=gg4 kind=element char=nul created=yes type=Tuple(`a\0b` Int32) describe={"name":"t","type":"Tuple(\n    `a\\0b` Int32)","default_type":"","default_expression":"","comment":"","codec_expression":"","ttl_expression":"","is_subcolumn":0} || {"name":"t.a\u0000b","type":"Int32","default_type":"","default_expression":"","comment":"","codec_expression":"","ttl_expression":"","is_subcolumn":1} mapped=yes
PROBE-CTL tree=gg4 kind=element char=bel created=yes type=Tuple(`a<U+0007>b` Int32) describe={"name":"t","type":"Tuple(\n    `a\u0007b` Int32)","default_type":"","default_expression":"","comment":"","codec_expression":"","ttl_expression":"","is_subcolumn":0} || {"name":"t.a\u0007b","type":"Int32","default_type":"","default_expression":"","comment":"","codec_expression":"","ttl_expression":"","is_subcolumn":1} mapped=yes
```

Escape forms read off those lines (server `26.9.19.9`):

| kind | char | created | `system.columns.type` | mapped |
|------|------|---------|-----------------------|--------|
| label | bsp | yes | `Enum8('a\bb' = 1, 'z' = 2)` | yes |
| label | ff | yes | `Enum8('a\fb' = 1, 'z' = 2)` | yes |
| label | nul | yes | `Enum8('a\0b' = 1, 'z' = 2)` | yes |
| label | bel | yes | `Enum8('a<U+0007>b' = 1, 'z' = 2)` | yes |
| element | bsp | yes | `Tuple(`a\bb` Int32)` | yes |
| element | ff | yes | `Tuple(`a\fb` Int32)` | yes |
| element | nul | yes | `Tuple(`a\0b` Int32)` | yes |
| element | bel | yes | `Tuple(`a<U+0007>b` Int32)` | yes |

- **Backspace, form feed, NUL.** `system.columns.type` writes them as backslash escapes (`\b`, `\f`, `\0`) in the label and in the element name. In the DESCRIBE JSON the label's type string carries the same text with the backslash JSON-escaped (`\\b`, `\\f`, `\\0`), the parent Tuple's type string likewise, and the sub-column row's `name` is `t.a` followed by the JSON escape of the character itself (`\b`, `\f`, `\u0000`), which a JSON reader turns into the raw character.
- **BEL.** `system.columns.type` holds the raw character U+0007 (shown as `<U+0007>`), and the DESCRIBE JSON writes it as `\u0007` in the label's type string, in the parent Tuple's type string and in the sub-column name.
- **Mapping.** `describeTable` maps all eight tables. The enum keys it builds are the raw labels `a`, the character, `b` (`PROBE-ENUMKEYS`: `a\u0008b`, `a\u000Cb`, `a\u0000b`, `a\u0007b` in the probe's escaped print form) and the Tuple element names are `t.a`, the raw character, `b` (`PROBE-COL`). No insert was run: whether a record written under these names lands is not measured here, and the test-side pin for these four characters is still the one carried in FORK-DELTA Known costs item 11.

Matches text: yes

### E-25: KeeperMap state-table query and DELETE examples with a prefixed key (ROLL-05)

Backs: `docs/ROLLOUT-v1.7.0_gg1.md` (items 1 and 2), `docs/ROLLOUT-v1.7.0_gg2.md` (item 1) and `docs/ROLLOUT-v1.7.0_gg4.md` item 4 (the in-flight check), written in plans 29-03 and 29-04. The first block is the state-table query of `docs/ROLLOUT-v1.7.0_gg3.md` item 4 step 2, copied byte for byte (lines 251-255 of that note, `/tmp/gsd29/probe/sql/keeper-self-check.sql`; `cmp` against `sed -n 251,255p` printed no difference). The second block is the `DELETE` example block of item 2 of the gg1 note as it reads in this commit, extracted from the note by the script that wrote this entry; the two statements in it are the lines 105 and 108 of the gg1 note at `P29_BASE` (`/tmp/gsd29/probe/sql/keeper-delete-keys.sql` and `keeper-delete-topics.sql`), and only the comment lines changed. Placeholders: `<zkDatabase>`, `<topic>`, `<partition>`, `<db>` and `<dbTopicSplitChar>`.

```sql
SELECT `key`, minOffset, maxOffset, state
FROM `<zkDatabase>`
WHERE state != 'AFTER_PROCESSING'
  AND arrayStringConcat(arrayPopBack(splitByChar('-', `key`)), '-') IN ('<topic>', '<topic>')
ORDER BY `key`;
```

```sql
-- EXAMPLE: delete the state rows of the affected topic-partitions
-- (with enableDbTopicSplit=true, a topic that dbTopicSplitChar divides into exactly two parts is keyed '<topic>-<partition>'
-- and any other topic '<db><dbTopicSplitChar><topic>-<partition>'; read from the connector code, not measured)
DELETE FROM `<zkDatabase>` WHERE `key` IN ('<topic>-<partition>', '<topic>-<partition>');

-- EXAMPLE: or delete the state rows of every partition of the affected topics
-- (with enableDbTopicSplit=true, also list '<db><dbTopicSplitChar><topic>' as in item 1)
DELETE FROM `<zkDatabase>` WHERE arrayStringConcat(arrayPopBack(splitByChar('-', `key`)), '-') IN ('<topic>');
```

Method (`keeperSelfCheck`, gg4 worktree, server `26.9.19.9`, client V2, image as in E-20; XML `/tmp/gsd29/exec/keeper.xml`, `tests="1" skipped="0" failures="0" errors="0"`): `new KeeperStateProvider(new ClickHouseSinkConfig(props))` with `zkPath` `/kafka-connect` and `zkDatabase` `connect_state` against the server's embedded Keeper (the `keeper_map.xml` that `ClickHouseBase` mounts) creates the table; the provider then writes five state records: `orders` partition 0 `AFTER_PROCESSING` (offsets 0 to 9), `orders` partition 1 `BEFORE_PROCESSING` (10 to 19), `orders-v2` partition 0 `BEFORE_PROCESSING` (0 to 4), `db1.orders` partition 2 `BEFORE_PROCESSING` (20 to 29) and `other` partition 0 `BEFORE_PROCESSING` (0 to 4). The key `db1.orders-2` is the prefixed key form with `dbTopicSplitChar` `.`; the probe writes it as a state record itself, so which key the connector writes for a topic is not measured here but read from `RecordConvertor.convert` (a topic that splits into exactly two parts keeps its own name, `parts.length == 2`) and `Processing.doLogic` (`topic = database + dbTopicSplitChar + topic`). Substitution map for the query: `<zkDatabase>` = `connect_state`, the two `'<topic>'` entries = `'orders'` and `'db1.orders'`. The query runs in the connection database, where the table lives.

```sql
SELECT `key`, minOffset, maxOffset, state
FROM `connect_state`
WHERE state != 'AFTER_PROCESSING'
  AND arrayStringConcat(arrayPopBack(splitByChar('-', `key`)), '-') IN ('orders', 'db1.orders')
ORDER BY `key`;
```

Then `orders` partition 1 and `db1.orders` partition 2 are set to `AFTER_PROCESSING` and the query runs again. For the two `DELETE` examples the probe creates two further state tables (`connect_state_dk` at `zkPath` `/kafka-connect-dk` and `connect_state_dt` at `/kafka-connect-dt`), writes the same five records into each, runs one statement against each and lists the keys that remain (a key listing, `SELECT key FROM <table> ORDER BY key` with the identifiers backtick-quoted). The statements as run, with the placeholders replaced:

```sql
DELETE FROM `connect_state_dk` WHERE `key` IN ('orders-1', 'db1.orders-2');
DELETE FROM `connect_state_dt` WHERE arrayStringConcat(arrayPopBack(splitByChar('-', `key`)), '-') IN ('orders', 'db1.orders');
```

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd29/wt-tip/gradlew -p /tmp/gsd29/wt-tip --console=plain test --tests '*Phase29RolloutProbe.keeperSelfCheck' --rerun
cp /tmp/gsd29/wt-tip/build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.Phase29RolloutProbe.xml /tmp/gsd29/exec/keeper.xml
python3 -I /tmp/gsd29-scripts/xml-probe-lines.py /tmp/gsd29/exec/keeper.xml
```

```text
PROBE-VERSION tree=gg4 version=26.9.19.9
PROBE-KEEPER-TABLE tree=gg4 engine=KeeperMap('/kafka-connect') PRIMARY KEY key
PROBE-KEEPER case=in-flight keys=db1.orders-2,orders-1
PROBE-KEEPER case=all-after keys=
PROBE-KEEPER-DELETE form=keys outcome=ok
PROBE-KEEPER case=after-delete-keys keys=orders-0,orders-v2-0,other-0
PROBE-KEEPER-DELETE form=topics outcome=ok
PROBE-KEEPER case=after-delete-topics keys=orders-v2-0,other-0
```

Result. The query returns exactly the two in-flight keys of the two listed topic forms, `db1.orders-2` and `orders-1`. It does not return `orders-0` (already `AFTER_PROCESSING`), `orders-v2-0` (a topic that merely shares the prefix `orders`) or `other-0`, and it returns nothing once both ranges are `AFTER_PROCESSING`. Both `DELETE` examples succeeded on the KeeperMap table (`outcome=ok`; the lightweight `DELETE` is supported on this engine on this server). The first removed exactly `orders-1` and `db1.orders-2` and left `orders-0`, `orders-v2-0` and `other-0`. The second, run with the prefixed topic added as its comment says, removed every partition of `orders` and of `db1.orders` and left `orders-v2-0` and `other-0`. The state table is the one the connector's own `KeeperStateProvider` creates (`ENGINE = KeeperMap('/kafka-connect') PRIMARY KEY key`). The notes may therefore call the query and both examples run on a scratch state table, and must keep the key form as read from code.

Matches text: yes

### E-30: item 7 rollback on a dry-run merge of the tip (D-03)

Backs: `docs/ROLLOUT-v1.7.0_gg4.md`, item 7 (the rollback): `git fetch --tags origin`, `git checkout custom_build`, `git merge --ff-only origin/custom_build`, `git status --porcelain`, `git read-tree -u --reset pre-v1.7.0_gg4`, `git commit -m "Roll back v1.7.0_gg4"`, `git diff --quiet HEAD pre-v1.7.0_gg4 && echo ok`, `git revert <rollback-commit>` and `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg4.md` (written in plan 29-04). Date (UTC) 2026-10-23. Server version: not involved (git only).

Tag `pre-v1.7.0_gg4` is a local lightweight tag on `69cd680bf548bfb0329142752d37f8a6c3190c26` (`origin/custom_build`, the pre-merge tip; `git cat-file -t` prints `commit`). Its `tests.yaml` has the trigger `branches: ['**']` (a branch-push trigger only), and `git ls-remote --tags origin` does not list the tag (the grep count is 0). X is the tip that contains the finished frame, item 1 and item 7 of the note: `X = 48bf42d6946ac4a608384d51d7ec5af5a7676b04` (the note was committed first as `fae7e42a`; X is the follow-up commit that added the `gradlew.bat` exception to step 2.1 after the first run of this script showed it, so the script ran twice and the record is of the second run). Throwaway merge: `git worktree add --detach /tmp/gsd29/dry 69cd680bf548bfb0329142752d37f8a6c3190c26`, then in it `git -c user.name=demo -c user.email=demo@example.invalid merge --no-ff --no-edit X`, giving the merge commit `D = 74cc816d469e121905dddff8765c7dbb430802f2`. D has two parents, `D^1` is `69cd680b`, `D^2` is X, and `git diff --quiet D X` holds (no non-merge difference), which printed `MERGE-DRY-RUN-OK`. D was never pushed. Plans 29-07 and 29-08 re-run the script on the publication tip and on the real merge commit, which have a different tip from X (items 2 to 6 of the note are written after X). The dry-run worktree was removed and pruned afterwards.

The script lives outside the repository (`/tmp/gsd29/rollback-demo.sh <repo> <target-tip> <worktree-path>`), adapted from the Phase 24 script (`24-COMMAND-EVIDENCE.md` E-22). A throwaway branch that starts at `pre-v1.7.0_gg4` stands in for a stale local `custom_build`, so `git checkout custom_build` is the one command not run verbatim (a linked worktree cannot check out `custom_build` itself); the fast-forward target D stands in for `origin/custom_build`. The `-c user.name/user.email` options only give the throwaway commits an identity. Deviation from a published tag: `git fetch --tags origin` ran while the tag existed only locally, and it kept the local tag (the script prints it after the fetch). Changes against the Phase 24 script: the tag and the document path are the gg4 ones, the heading count is printed as `item-7 headings: <n>`, and one added line reports whether the corrected key rule of the gg1 note (`divides into exactly two parts`) is present in the rolled-back tree.

```sh
#!/bin/bash
# usage: rollback-demo.sh <repo> <target-tip> <worktree-path>
# Runs item 7 as written, on a throwaway branch standing in for a stale local custom_build
# (starts at pre-v1.7.0_gg4, then fast-forwards to <target-tip>, the stand-in for origin/custom_build).
set -u
REPO=$1; TARGET=$2; WT=$3; BR=gg4-rollback-demo-$$
ID=(-c user.name=demo -c user.email=demo@example.invalid)
git -C "$REPO" worktree add --quiet -b "$BR" "$WT" 'pre-v1.7.0_gg4^{commit}'
g() { git -C "$WT" "$@"; }
TGT=$(git -C "$REPO" rev-parse "$TARGET")
echo "target=$TGT stale-start=$(g rev-parse --short HEAD)"
echo "x" >> "$WT/README.md"; echo "dirty: status --porcelain lines=$(g status --porcelain | wc -l | tr -d ' ')"; g checkout -q -- README.md
g fetch -q --tags origin && echo "fetch --tags exit=0"
echo "local tag kept after fetch: $(g rev-parse --short 'pre-v1.7.0_gg4^{commit}')"
g merge -q --ff-only "$TARGET" && echo "merge --ff-only exit=0, HEAD==target: $([ "$(g rev-parse HEAD)" = "$TGT" ] && echo yes)"
echo "clean: status --porcelain lines=$(g status --porcelain | wc -l | tr -d ' ')"
g read-tree -u --reset pre-v1.7.0_gg4 && g "${ID[@]}" commit -q -m "Roll back v1.7.0_gg4" && echo "read-tree + commit exit=0"
RB=$(g rev-parse HEAD); echo "rollback-commit parent==target: $([ "$(g rev-parse HEAD^)" = "$TGT" ] && echo yes)"
g diff --quiet HEAD pre-v1.7.0_gg4 && echo ok
[ -z "$(g status --porcelain)" ] && echo "worktree clean after commit"
g ls-files --error-unmatch docs/ROLLOUT-v1.7.0_gg4.md >/dev/null 2>&1 || echo "rollout doc gone from branch"
g show "$RB^:docs/ROLLOUT-v1.7.0_gg4.md" | grep -c '^## Item 7: Rollback' | sed 's/^/git show <rollback-commit>^:docs\/ROLLOUT... item-7 headings: /'
g show HEAD:.github/workflows/tests.yaml | grep -qF "branches: ['**']" && echo "rolled-back tests.yaml keeps branches: ['**']"
g show HEAD:docs/ROLLOUT-v1.7.0_gg1.md | grep -qF 'divides into exactly two parts' && echo "gg1 corrected rule after rollback: present" || echo "gg1 corrected rule after rollback: absent"
g "${ID[@]}" revert --no-edit "$RB" >/dev/null && g diff --quiet HEAD "$TARGET" && echo "after git revert <rollback-commit>: tree(HEAD)==tree(target)"
git -C "$REPO" worktree remove --force "$WT" && git -C "$REPO" worktree prune && git -C "$REPO" branch -q -D "$BR"
```

```text
X=48bf42d6946ac4a608384d51d7ec5af5a7676b04
D=74cc816d469e121905dddff8765c7dbb430802f2
MERGE-DRY-RUN-OK
target=74cc816d469e121905dddff8765c7dbb430802f2 stale-start=69cd680b
dirty: status --porcelain lines=2
fetch --tags exit=0
local tag kept after fetch: 69cd680b
merge --ff-only exit=0, HEAD==target: yes
clean: status --porcelain lines=1
read-tree + commit exit=0
rollback-commit parent==target: yes
ok
rollout doc gone from branch
git show <rollback-commit>^:docs/ROLLOUT... item-7 headings: 1
rolled-back tests.yaml keeps branches: ['**']
gg1 corrected rule after rollback: absent
after git revert <rollback-commit>: tree(HEAD)==tree(target)
```

Two lines differ from the Phase 24 run, and both come from the same file. `dirty: ... lines=2` and `clean: ... lines=1` are one line more than in Phase 24, and the line `worktree clean after commit` is not printed. A fresh worktree of `69cd680b`, and the fast-forwarded one, list ` M gradlew.bat` in `git status --porcelain` before any edit (checked separately in a scratch worktree of the tag): `.gitattributes` has `*.bat text eol=crlf`, and the `gradlew.bat` blob committed by the Gradle wrapper bump has CRLF line endings (`git ls-files --eol` prints `i/crlf w/crlf attr/text eol=crlf`; the blob at tag `v1.7.0_gg3` has none). The note's step 2.1 therefore names that one line as expected. The rollback itself is not affected: `read-tree` and the commit succeed, `git diff --quiet HEAD pre-v1.7.0_gg4` holds and the revert restores the tree of D.

Afterwards `git worktree list` shows the main working tree and the two kept probe worktrees `wt-gg3` and `wt-tip` only (no `dry` and no `rb-demo` entry), no `gg4-rollback-demo-*` branch is left, and `git ls-remote --tags origin` lists no `pre-v1.7.0_gg4`.

Matches text: yes

### E-40: CHANGELOG section check

Backs: `CHANGELOG.md`, the `# 1.7.0_gg4` section (plan 29-05 Task 1). Date used in the heading (UTC, the day the section was written): 2026-10-23 (assumption A13: the publication day, compared again in the 29-07 bundle). The check (verify command 1 of Task 1, a Python read of `CHANGELOG.md` and of `git show c1dc6e59:CHANGELOG.md`, then `python3 -I /tmp/gsd29-scripts/consistency.py changelog`) asserts: a dated heading, everything from `# 1.7.0_gg3, 2026-10-20` down byte-identical to `P29_BASE`, the four sections in order, the rename notice as the first Important Changes bullet (`CREATE TABLE`, a rename instruction and `DEFAULT`), the links and facts of the section, no planning identifier, at least four Bug Fixes bullets and no line over 120 characters; the script then compares the note and the section on `v1.7.0_gg4`, the client name, `33.7.2-jre`, `5.6.5`, `cp-kafka-connect-base:7.9.7`, `KIP-1161` and `CREATE TABLE`.

```sh
bash /tmp/gsd29-scripts/p05/cmd1.sh
```

```text
heading True tail-unchanged True sections ['Important Changes', 'Known Issues', 'Bug Fixes', 'Build and CI'] rename-first True missing [] planning-ids [] bug-fix-bullets 4 long-lines []
consistency scope=changelog docs=['changelog', 'note'] failed=0
CHANGELOG-GG4-OK
```

Writing the section exposed one wrong sentence in the note, and it was fixed there (not in the evidence). Item 1 of `docs/ROLLOUT-v1.7.0_gg4.md` listed "a tab, carriage return, slash or double quote on client V1" as tables that were not mapped. `28-BEHAVIOUR-LEDGER.md` L-59 and the 72-row table behind it, and the table of the same paragraph, say that on client V1 only a tab and a carriage return left the table unmapped, while a slash or a double quote came back as the replacement character U+FFFD, so the table was mapped and no field matched. The sentence now reads that way. Plan 29-04's five verify commands were re-run after the edit and printed `NOTE-FRAME-OK`, `ITEMS-1-7-OK`, `ITEMS-2-4-OK` and `ITEMS-5-6-OK`; the address and secret scan of the fifth (server version masked) printed no hit.

Matches text: n/a (gate check)

### E-41: FORK-DELTA coverage audit

Backs: the "Pinned by" cells of `docs/FORK-DELTA.md` for the Phase 25 to 28 requirement IDs with a code, build or test effect (plan 29-05, task 2). Date (UTC) 2026-10-23. Method of E-31 in the Phase 24 evidence, re-created as `/tmp/gsd29-scripts/cov.py` (scratch, not committed): for each of the 26 IDs the script lists the table rows whose last cell (Pinned by) names it, and for each row of `.planning/scripts/fork-anchors.txt` whose backs-ID is one of them it checks that the ID is named in such a cell. The 26: BLD-15, BLD-16, CI-20, DEBT-13, DEBT-14, DEBT-15, DEBT-16, DEBT-17, DEPS-02, DEPS-03, SYNC-11, SYNC-12, DESC-06, DESC-07, DESC-08, DESC-09, WRT-16, WRT-17, WRT-18, WRT-19 and NAME-01 to NAME-06. Before the rows were edited the audit found no row for BLD-16 (only in row text), DEBT-13, DEBT-14, DEBT-16, DEPS-02, DEPS-03 and DESC-09; they were added to the Pinned-by cells of the `onPartitionsRevoked` row and the dependency-pins row (BLD-16, DEPS-02, DEPS-03), the CI row (DEBT-13, DEBT-14), the test-doubles row (DEBT-16) and the describe-builder row (DESC-09), each with the sentence or test class that backs it. HEAD is the Task 1 commit `aad30ee6` plus the uncommitted FORK-DELTA edits of this task.

```sh
python3 -I /tmp/gsd29-scripts/cov.py "$PWD"
```

```text
BLD-15 1 | `confluentJar` unpacks `clickhouseDependencies` 
BLD-16 2 | `onPartitionsRevoked(Collection<TopicPartition>); Fork dependency pins in the version catalog: Kaf
CI-20 2 | A single-job CI workflow: ClickHouse `latest` wi; `createConfluentArchive` is finalized by `verify
DEBT-13 1 | A single-job CI workflow: ClickHouse `latest` wi
DEBT-14 1 | A single-job CI workflow: ClickHouse `latest` wi
DEBT-15 1 | `org.slf4j:slf4j-reload4j` (resolved `2.0.20`) i
DEBT-16 1 | Hand-written test doubles instead of inline Mock
DEBT-17 1 | Five fork build additions, re-applied onto upstr
DEPS-02 1 | Fork dependency pins in the version catalog: Kaf
DEPS-03 1 | Fork dependency pins in the version catalog: Kaf
SYNC-11 1 | Upstream's guava override: `guava = "33.7.2-jre"
SYNC-12 2 | Upstream's guava override: `guava = "33.7.2-jre"; Upstream's httpclient5 pin: `httpclient = "5.6.5
DESC-06 2 | Both describe paths feed one fork-owned `Describ; `captureDescribeFixtures` (a `JavaExec` task, ru
DESC-07 2 | Both describe paths feed one fork-owned `Describ; `captureDescribeFixtures` (a `JavaExec` task, ru
DESC-08 3 | Both describe paths feed one fork-owned `Describ; `Table.updateParent`'s `ARRAY` branch ignores a ; Tuple element nullability comes from the declare
DESC-09 1 | Both describe paths feed one fork-owned `Describ
WRT-16 1 | `doWriteColValue`'s generic catch passes the ori
WRT-17 1 | `doWriteColValue`'s generic catch passes the ori
WRT-18 1 | `Column.convertEnumValues` reports a label that 
WRT-19 2 | `doWriteColValue`'s generic catch passes the ori; `SourcePlanningIdGuardUnitTest` fails `testUnit`
NAME-01 1 | Discovery matches a column or Tuple element unde
NAME-02 2 | Discovery matches a column or Tuple element unde; `RawNameMatrix` (twelve entries: the nine charac
NAME-03 3 | Discovery matches a column or Tuple element unde; Both describe paths, client V1 and client V2, re; `RawNameMatrix` (twelve entries: the nine charac
NAME-04 3 | Discovery matches a column or Tuple element unde; Tuple element nullability comes from the declare; `RawNameMatrix` (twelve entries: the nine charac
NAME-05 1 | Discovery matches a column or Tuple element unde
NAME-06 2 | Discovery matches a column or Tuple element unde; `RawNameMatrix` (twelve entries: the nine charac
anchor rows backed by a Phase 25-28 ID: 25 missing: 0
uncovered IDs: [] anchor rows missing: 0
```

Matches text: n/a (gate check)

### E-42: FORK-DELTA path resolution

Backs: every `.planning/` path that `docs/FORK-DELTA.md` cites (plan 29-05, task 2). Date (UTC) 2026-10-23. Same command as E-32 in the Phase 24 evidence. The documents named in the new text (`29-COMMAND-EVIDENCE.md`, the two new ledgers) are named by file only, because the phase folders move when the milestone is archived; the paths the document does spell out all resolve. The new paths against the Phase 24 list are `.planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md` (cited by the Phase 25 rows), `.planning/scripts/compare-confluent-archives.py`, `.planning/scripts/confluent-archive-allowlist-29.txt` and `.planning/scripts/verify-merge-invariants-selftest.sh`.

```sh
grep -o '\.planning/[A-Za-z0-9_./-]*[A-Za-z0-9_-]' docs/FORK-DELTA.md | sort -u | while read -r p; do [ -n "$(git ls-files -- "$p")" ] && echo "OK   $p" || echo "DEAD $p"; done
```

```text
OK   .planning/MERGE-CHECKLIST-v1.7.0.md
OK   .planning/MERGE-CHECKLIST.md
OK   .planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md
OK   .planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-REVIEW-DISPOSITION.md
OK   .planning/milestones/v1.7.0_gg1-sync-phases/14-dependency-bumps-ci/14-REVIEW-DISPOSITION.md
OK   .planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md
OK   .planning/REQUIREMENTS.md
OK   .planning/scripts/compare-confluent-archives.py
OK   .planning/scripts/confluent-archive-allowlist-29.txt
OK   .planning/scripts/fork-anchor-exemptions.txt
OK   .planning/scripts/fork-anchors.txt
OK   .planning/scripts/verify-merge-invariants-selftest.sh
OK   .planning/scripts/verify-merge-invariants.sh
```

No `DEAD` line.

Matches text: n/a (gate check)

### E-43: raw numbers, recount, skip inventory and consistency

Backs: `docs/FORK-DELTA.md` "The raw numbers" (two new `after gg4` rows, two new commands, the tail sentence, the comparison and "One number does bear" paragraphs), the Adopted-wholesale `src/main` row and the Named skip inventory (plan 29-05, task 2), and the agreement of the rollout note, the CHANGELOG section and FORK-DELTA (research Pitfall 9). Date (UTC) 2026-10-23. HEAD is the Task 1 commit `aad30ee6` plus the uncommitted FORK-DELTA edits of this task, on `sync/v1.7.0`. Nothing under `src`, `build.gradle.kts`, `gradle` or `.github` changed since `P29_BASE` (the last such change is Phase 28's `2575092a`), checked by the last command below. The tag `v1.7.0_gg4` does not exist yet, so the gg4 figures are measured against HEAD; plan 29-08 re-runs the two tag commands once the tag exists. The published gg1, gg2, gg3 and before rows reproduce on their tags and branch point.

```sh
git diff --shortstat v1.7.0 HEAD -- src build.gradle.kts gradle .github   # and the other scopes below
git diff --shortstat v1.7.0 a6f1df839c079180bc7c343b5823f205e204dafc      # the upstream tail, whole tree and wider scope
python3 -I /tmp/gsd29-scripts/recount.py "$PWD" HEAD v1.7.0
```

```text
$ git diff --shortstat v1.7.0 HEAD -- src build.gradle.kts gradle .github
 277 files changed, 29679 insertions(+), 643 deletions(-)
$ git diff --shortstat v1.7.0 HEAD -- src/main
 37 files changed, 2877 insertions(+), 241 deletions(-)
$ git diff --shortstat v1.6.0 pre-v1.7.0-sync -- src build.gradle.kts gradle .github
 84 files changed, 9476 insertions(+), 451 deletions(-)
$ git diff --shortstat v1.7.0 v1.7.0_gg1 -- src build.gradle.kts gradle .github
 95 files changed, 13587 insertions(+), 484 deletions(-)
$ git diff --shortstat v1.7.0 v1.7.0_gg2 -- src build.gradle.kts gradle .github
 119 files changed, 18802 insertions(+), 529 deletions(-)
$ git diff --shortstat v1.7.0 v1.7.0_gg3 -- src build.gradle.kts gradle .github
 130 files changed, 22783 insertions(+), 534 deletions(-)
$ git diff --shortstat v1.6.0 pre-v1.7.0-sync -- src/main
 30 files changed, 1368 insertions(+), 98 deletions(-)
$ git diff --shortstat v1.7.0 v1.7.0_gg1 -- src/main
 31 files changed, 1826 insertions(+), 130 deletions(-)
$ git diff --shortstat v1.7.0 v1.7.0_gg2 -- src/main
 32 files changed, 2042 insertions(+), 143 deletions(-)
$ git diff --shortstat v1.7.0 v1.7.0_gg3 -- src/main
 33 files changed, 2245 insertions(+), 145 deletions(-)
$ git diff --shortstat v1.7.0 HEAD -- src/test
 229 files changed, 26236 insertions(+), 106 deletions(-)
$ git diff --shortstat v1.7.0 HEAD -- src/testFixtures
 3 files changed, 68 insertions(+), 12 deletions(-)
$ git diff --shortstat v1.7.0 HEAD -- src/main
 37 files changed, 2877 insertions(+), 241 deletions(-)
$ git diff --shortstat v1.7.0 HEAD -- build.gradle.kts
 1 file changed, 248 insertions(+), 11 deletions(-)
$ git diff --shortstat v1.7.0 HEAD -- gradle
 3 files changed, 17 insertions(+), 16 deletions(-)
$ git diff --shortstat v1.7.0 HEAD -- .github
 4 files changed, 233 insertions(+), 257 deletions(-)
$ git diff --shortstat v1.7.0 a6f1df839c079180bc7c343b5823f205e204dafc -- src build.gradle.kts gradle .github
 2 files changed, 11 insertions(+), 9 deletions(-)
$ git diff --shortstat v1.7.0 a6f1df839c079180bc7c343b5823f205e204dafc
 3 files changed, 16 insertions(+), 9 deletions(-)
$ git diff --quiet c1dc6e59 HEAD -- src build.gradle.kts gradle .github; echo $?
0
```

The gg1, gg2, gg3 and before rows keep their published figures; the two new rows carry 277/29679/643 and 37/2877/241. The scopes add up: 229 + 3 + 37 + 1 + 3 + 4 = 277 files and 26236 + 68 + 2877 + 248 + 17 + 233 = 29679 insertions. The upstream tail `a6f1df83` differs from `v1.7.0` by 2 files, 11 insertions and 9 deletions in the wider scope, so it is inside the after-gg4 figure.

The byte-identical and import-order counts (a file is byte-identical when `git diff --quiet v1.7.0 HEAD -- <file>` exits 0; of the others it is import-order-only when its content without `import ` lines and blank lines equals upstream's), `/tmp/gsd29-scripts/recount.py` (the Phase 24 script, unchanged):

```text
files present at v1.7.0 : 63
byte-identical at HEAD : 36
differ: 27  (import-order only: 14 , carry fork logic: 13 )
absent at HEAD : 0 []
files only here (added since v1.7.0): 10
    MetadataFreeJsonWriter.java
    DescribeTableBuilder.java
    DeclaredNullability.java
    ParentShape.java
    ConvertToMapOrJson.java
    HeaderToValue.java
    HeaderToValueConfig.java
    HeadersToValue.java
    TableOperationFilter.java
    WithheldMessageException.java
differing src/main files in total: 37
import-only: ClickHouseSinkConnector.java, Record.java, StructToJsonMap.java, EmptyRecordConvertor.java, RecordConvertor.java, SchemaRecordConvertor.java, SchemalessRecordConvertor.java, DBWriter.java, InMemoryDBWriter.java, TableMappingRefresher.java, InMemoryState.java, ExtractTopic.java, ExtractTopicConfig.java, MBeanServerUtils.java
logic: ClickHouseSinkConfig.java, ClickHouseSinkTask.java, ProxySinkTask.java, Data.java, ClickHouseWriter.java, ClickHouseFieldDescriptor.java, ClickHouseHelperClient.java, Column.java, Table.java, Type.java, Utils.java, SinkTaskStatistics.java, org.apache.kafka.connect.transforms.Transformation
```

The same script at `v1.7.0_gg3` printed 38 byte-identical, 25 differing (14 import-only, 11 with fork logic) and 8 files only here, so since gg3 `ClickHouseFieldDescriptor` and `Table` moved from byte-identical to fork logic and `DescribeTableBuilder` and `ParentShape` were added. The Adopted-wholesale `src/main` row already read 36 of 63, 27 that differ, 14 import-only, 13 with fork logic and ten more files (Phase 26 wrote it), so it is unchanged; the "One number does bear" paragraph now says 37 files, 14 and 23.

The two Named-skip greps:

```sh
grep -rho '@Disabled' src/test/java | wc -l
grep -rho 'Assumptions.assumeTrue' src/test/java | wc -l
```

```text
       7
       1
```

Seven disabled methods plus the eleven parameterized cases behind the one assumption still make 18; the inventory table is unchanged.

Consistency of the three documents (the note, the CHANGELOG section and FORK-DELTA), `python3 -I /tmp/gsd29-scripts/consistency.py all`: `v1.7.0_gg4`, `33.7.2-jre`, `5.6.5` and `cp-kafka-connect-base:7.9.7` in each, the client name and `KIP-1161` in the note and the CHANGELOG, `CREATE TABLE` in the note's first 15 lines and in the first Important Changes bullet, and `pre-v1.7.0_gg4`, the nested-jar count (E-04 `PRE-NESTED-JARS=25`) and the server version (E-05) in the note and FORK-DELTA.

```text
consistency scope=all docs=['changelog', 'fork-delta', 'note'] failed=0
```

Matches text: n/a (gate check)


### E-50: gg1 DELETE example with the trailing-split-character comment (29-REVIEW IN-02)

Backs: `docs/ROLLOUT-v1.7.0_gg1.md` item 2, the `DELETE` example block, written in plan 29-06 (review round 1, fix IN-02). The only change against the block of E-25 is the second and third comment line of the first example, which now also names the key form without the trailing split characters that item 1 of the same note lists. Both statements are unchanged, so the run of E-25 applies to them; the probe `keeperSelfCheck` was run once more on the gg4 tip worktree against the same server to show that nothing differs (XML `/tmp/gsd29/exec/keeper-r2.xml`, `tests="1" skipped="0" failures="0" errors="0"`). The block as it reads in the note:

```sql
-- EXAMPLE: delete the state rows of the affected topic-partitions
-- (with enableDbTopicSplit=true, a topic that dbTopicSplitChar divides into exactly two parts is keyed '<topic>-<partition>'
-- and any other topic '<db><dbTopicSplitChar><topic>-<partition>'; a topic that ends with the split character can also be keyed
-- without the trailing characters, as in item 1; read from the connector code, not measured)
DELETE FROM `<zkDatabase>` WHERE `key` IN ('<topic>-<partition>', '<topic>-<partition>');

-- EXAMPLE: or delete the state rows of every partition of the affected topics
-- (with enableDbTopicSplit=true, also list '<db><dbTopicSplitChar><topic>' as in item 1)
DELETE FROM `<zkDatabase>` WHERE arrayStringConcat(arrayPopBack(splitByChar('-', `key`)), '-') IN ('<topic>');
```

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd29/wt-tip/gradlew -p /tmp/gsd29/wt-tip --console=plain test --tests '*Phase29RolloutProbe.keeperSelfCheck' --rerun
cp /tmp/gsd29/wt-tip/build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.Phase29RolloutProbe.xml /tmp/gsd29/exec/keeper-r2.xml
python3 -I /tmp/gsd29-scripts/xml-probe-lines.py /tmp/gsd29/exec/keeper-r2.xml
```

```text
PROBE-VERSION tree=gg4 version=26.9.19.9
PROBE-KEEPER-TABLE tree=gg4 engine=KeeperMap('/kafka-connect') PRIMARY KEY key
PROBE-KEEPER case=in-flight keys=db1.orders-2,orders-1
PROBE-KEEPER case=all-after keys=
PROBE-KEEPER-DELETE form=keys outcome=ok
PROBE-KEEPER case=after-delete-keys keys=orders-0,orders-v2-0,other-0
PROBE-KEEPER-DELETE form=topics outcome=ok
PROBE-KEEPER case=after-delete-topics keys=orders-v2-0,other-0
```

Result. The probe lines equal those of E-25 line for line: the first `DELETE` removed exactly `orders-1` and `db1.orders-2`, the second removed every partition of `orders` and of `db1.orders`, and `orders-v2-0` and `other-0` stayed. The key form without trailing split characters named in the new comment is read from the connector code and is not measured, as the comment says.

Matches text: yes
