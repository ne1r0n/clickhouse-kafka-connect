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

### E-03: the shipped archive, tag v1.7.0_gg4 versus the gg5 tip (D-03 claims 1 and 2, PT-05, PT-10)

Backs: `docs/ROLLOUT-v1.7.0_gg5.md` claims 1 and 2 (written in plan 32-02) and the CHANGELOG and FORK-DELTA lines on the archive (32-03). Archive A is built with `clean createConfluentArchive` in a detached worktree at tag `v1.7.0_gg4` (`21c51598`, which is `PRE`, the rollback target `pre-v1.7.0_gg5`); archive B is built the same way at the gg5 tip after the bump commit (`a6bfd78f`). Both builds ran the `verifyConfluentArchive` finalizer. `compare-confluent-archives.py` reads the two zips entry by entry (SHA-256 of every file of the inner `-confluent.jar` and of the other outer entries) under the new allow-list `.planning/scripts/confluent-archive-allowlist-32.txt`.

The allow-list holds four exact-path rows and no wildcard (32-AMENDMENT-01). The first run of the compare with the planned two-row list (`Version.class`, `META-INF/MANIFEST.MF`) ended `RESULT: FAIL` with two `UNALLOWED CHANGED` lines, `ClickHouseHelperClient.class` and `SinkTaskStatistics.class`, and totals `only-a 0 only-b 0 changed 4`. `src/main` is byte-identical between the trees; the cause is that javac inlines the compile-time constant `Version.ARTIFACT_VERSION` (a `static final String` initialised with a literal in the generated `Version.java`) into every class that reads it. The `javap` proof below shows that only the `v1.7.0_gg4` -> `v1.7.0_gg5` string differs in all three classes. The user chose the four-row list (decision recorded in `32-AMENDMENT-01.md`); no wildcard and no catch-all row was added.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE -u CLICKHOUSE_CLUSTER_NAME /tmp/gsd32/wt-gg4/gradlew -p /tmp/gsd32/wt-gg4 --console=plain clean createConfluentArchive
env -u TESTCONTAINERS_HOST_OVERRIDE -u CLICKHOUSE_CLUSTER_NAME /tmp/gsd32/wt-tip/gradlew -p /tmp/gsd32/wt-tip --console=plain clean createConfluentArchive
grep -n '^> Task :verifyConfluentArchive\|^verifyConfluentArchive:\|^BUILD' /tmp/gsd32/exec/build-gg4.log /tmp/gsd32/exec/build-tip.log
shasum -a 256 /tmp/gsd32/archives/gg4/clickhouse-kafka-connect-v1.7.0_gg4.zip /tmp/gsd32/archives/tip/clickhouse-kafka-connect-v1.7.0_gg5.zip
python3 -I .planning/scripts/compare-confluent-archives.py /tmp/gsd32/archives/gg4/clickhouse-kafka-connect-v1.7.0_gg4.zip /tmp/gsd32/archives/tip/clickhouse-kafka-connect-v1.7.0_gg5.zip .planning/scripts/confluent-archive-allowlist-32.txt
```

```text
build-gg4.log:626:> Task :verifyConfluentArchive
build-gg4.log:627:verifyConfluentArchive: 7801 inner entries, no nested jar, no Kafka API class
build-gg4.log:637:BUILD SUCCESSFUL in 11s
build-tip.log:626:> Task :verifyConfluentArchive
build-tip.log:627:verifyConfluentArchive: 7801 inner entries, no nested jar, no Kafka API class
build-tip.log:637:BUILD SUCCESSFUL in 8s
455df230fd808ba4ced8d1461add5df930e24d83fbbe246d58ad17892bf33088  /tmp/gsd32/archives/gg4/clickhouse-kafka-connect-v1.7.0_gg4.zip
ab37b0a2cc9bb95848ae1e4d97f5aff942909ca24c604c68a76cfa5198ba682e  /tmp/gsd32/archives/tip/clickhouse-kafka-connect-v1.7.0_gg5.zip
```

The compare output under the four-row allow-list (exit status 0):

```text
CHANGED META-INF/MANIFEST.MF c35b231f2826502d29a6c12d5d0b5084ad30c626f1ed0a6a599f73bff52e8c55 328d6041604985984e59917f9893dd5fda67cbc1a620645a43f81a449c2787a0
CHANGED com/clickhouse/kafka/connect/sink/Version.class a09b1e9b003545a5877e28ec3a80c3d2ca6f1e24bd7aa6fb507e95f14e846c61 1fa7f26414a876317d591e9ec689f643d1f5b9aeb252b798a0c0f8eb060d71b2
CHANGED com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.class 833dbdaafde6c58e2e08b5eb94af42fa1e45ba1099233e75738389270858ddfe f76f141f6fe06d2d4c2b0fb082e888a0645e7d4defc702ea364c3fb119aa5a90
CHANGED com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.class 9dcefaf81856c22b2730838f9f8bb39dbde9ea7fc0579e05af5b0e1cea02bb92 41fc6f5373f48c31b686b221e6e5bfa63a14769d809af1f9b92c9bb0dd05eac1
VERSION at.yawk.lz4:lz4-java 1.12.0 -> 1.12.0
VERSION com.fasterxml.jackson.core:jackson-annotations 2.22 -> 2.22
VERSION com.fasterxml.jackson.core:jackson-core 2.22.4 -> 2.22.4
VERSION com.fasterxml.jackson.core:jackson-databind 2.22.4 -> 2.22.4
VERSION com.google.code.gson:gson 2.14.0 -> 2.14.0
VERSION com.google.errorprone:error_prone_annotations 2.50.0 -> 2.50.0
VERSION com.google.guava:failureaccess 1.0.3 -> 1.0.3
VERSION com.google.guava:guava 33.7.2-jre -> 33.7.2-jre
VERSION com.google.guava:listenablefuture 9999.0-empty-to-avoid-conflict-with-guava -> 9999.0-empty-to-avoid-conflict-with-guava
VERSION com.google.j2objc:j2objc-annotations 3.1 -> 3.1
VERSION commons-codec:commons-codec 1.19.0 -> 1.19.0
VERSION commons-io:commons-io 2.20.0 -> 2.20.0
VERSION org.apache.commons:commons-compress 1.28.0 -> 1.28.0
VERSION org.apache.commons:commons-lang3 3.20.0 -> 3.20.0
VERSION org.apache.httpcomponents.client5:httpclient5 5.6.5 -> 5.6.5
VERSION org.apache.httpcomponents.core5:httpcore5 5.4.4 -> 5.4.4
VERSION org.apache.httpcomponents.core5:httpcore5-h2 5.4.4 -> 5.4.4
VERSION org.slf4j:slf4j-api 2.0.7 -> 2.0.7
MANIFEST.MF: differs only in Implementation-Version
totals files-a 7399 files-b 7399 only-a 0 only-b 0 changed 4 same 7395
nested jars in b: 0
kafka classes in b: 0
RESULT: PASS
```

The four allow-list rows (exact paths, each reason "inlined compile-time constant `Version.ARTIFACT_VERSION` v1.7.0_gg4 -> v1.7.0_gg5"):

```text
com/clickhouse/kafka/connect/sink/Version.class
com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.class   (CONNECT_CLIENT_NAME)
com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.class             (MBean version segment)
META-INF/MANIFEST.MF                                                       (Implementation-Version)
```

The `javap` proof. Each of the three classes was extracted from the inner `-confluent.jar` of each archive into its own new directory and disassembled with `javap -c -p -constants`; the diff of the two listings (A on the left, B on the right) follows. The listings have the same line count in each pair (10, 2153 and 454 lines).

```sh
unzip -q -j <archive>.zip '*-confluent.jar' -d <new dir>
unzip -q <new dir>/*-confluent.jar com/clickhouse/kafka/connect/sink/Version.class com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.class com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.class -d <class dir A|B>
javap -c -p -constants <class> > <Name>.a.txt   # run in class dir A; .b.txt in class dir B
diff Version.a.txt Version.b.txt; diff ClickHouseHelperClient.a.txt ClickHouseHelperClient.b.txt; diff SinkTaskStatistics.a.txt SinkTaskStatistics.b.txt
```

```text
=== Version.class
3c3
<   public static final java.lang.String ARTIFACT_VERSION = "v1.7.0_gg4";
---
>   public static final java.lang.String ARTIFACT_VERSION = "v1.7.0_gg5";
=== ClickHouseHelperClient.class
51c51
<   public static final java.lang.String CONNECT_CLIENT_NAME = "clickhouse-kafka-connect/v1.7.0_gg4";
---
>   public static final java.lang.String CONNECT_CLIENT_NAME = "clickhouse-kafka-connect/v1.7.0_gg5";
174c174
<       12: ldc           #121                // String clickhouse-kafka-connect/v1.7.0_gg4
---
>       12: ldc           #121                // String clickhouse-kafka-connect/v1.7.0_gg5
420c420
<      120: ldc           #121                // String clickhouse-kafka-connect/v1.7.0_gg4
---
>      120: ldc           #121                // String clickhouse-kafka-connect/v1.7.0_gg5
=== SinkTaskStatistics.class
414c414
<        8: ldc           #244                // String v1.7.0_gg4
---
>        8: ldc           #244                // String v1.7.0_gg5
```

In prose: archive A is also the rollback target's archive. Tag `v1.7.0_gg4` and `pre-v1.7.0_gg5` name `PRE` (`21c51598`; see E-05), A was built from that tag with `clean createConfluentArchive` and its `verifyConfluentArchive` finalizer ended green (7801 inner entries, no nested jar, no Kafka API class), so the gg4 warning against building from the rolled-back tip does not carry forward (PT-10). The archive Phase 30's dry runs built on CI (`5a8522a8...638c`, 12453663 bytes, see E-04) is a different build of the gg4 tree from a different machine and is not compared byte for byte with A (12453558 bytes); the claim rests on the entry-by-entry compare above, not on whole-zip equality.

Matches text: pending (32-02, 32-03)

### E-04: the release path since the Phase 30 dry runs (D-01, D-03 claim 2)

Backs: `docs/ROLLOUT-v1.7.0_gg5.md` claim 2 (written in plan 32-02) and the CHANGELOG line on the release path (32-03). No release workflow is dispatched in this phase (D-01). The claim rests on Phase 30's two green dry runs at `b847fbdb`, on the release-path inputs being unchanged since that commit except `VERSION`, and on the local `clean createConfluentArchive` at the tip of E-03, which ran `verifyConfluentArchive`. The run bodies, the jobs and the annotations are read again from the unauthenticated REST API (rate limit read first: 60 of 60 remaining) and saved under `/tmp/gsd32/exec/api/` (`run-<id>.json`, `jobs-<id>.json`, `annotations-<id>.json`, the annotations of the one `build_release` job joined per run). Each run has 17 annotations.

```sh
curl -s https://api.github.com/rate_limit
curl -s https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs/<id>            > /tmp/gsd32/exec/api/run-<id>.json
curl -s https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs/<id>/jobs       > /tmp/gsd32/exec/api/jobs-<id>.json
curl -s https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/check-runs/<job id>/annotations > /tmp/gsd32/exec/api/ann-<job id>.json   # joined into annotations-<id>.json
D=b847fbdbf54a42a94738bb7d1fb0c6668a58d5c2
git diff --name-only $D HEAD -- .github/workflows/release.yaml .github/workflows/pre-release.yaml build.gradle.kts settings.gradle.kts gradle gradlew src/main src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java config
git diff --name-only $D HEAD -- VERSION
grep -n 'setup-gradle\|\./gradlew createConfluentArchive' .github/workflows/release.yaml .github/workflows/pre-release.yaml
```

```text
(first diff: no output)
VERSION
.github/workflows/pre-release.yaml:3:# archive is built with the project wrapper's Gradle through setup-gradle, so the verifyConfluentArchive
.github/workflows/pre-release.yaml:52:        uses: gradle/actions/setup-gradle@3f5f9adaf7d9fecd50b5935e54106014257a94e6 # v6.4.0
.github/workflows/pre-release.yaml:70:          ./gradlew createConfluentArchive --console=plain | tee "$RUNNER_TEMP/archive-build.log"
.github/workflows/release.yaml:3:# archive is built with the project wrapper's Gradle through setup-gradle, so the verifyConfluentArchive
.github/workflows/release.yaml:52:        uses: gradle/actions/setup-gradle@3f5f9adaf7d9fecd50b5935e54106014257a94e6 # v6.4.0
.github/workflows/release.yaml:70:          ./gradlew createConfluentArchive --console=plain | tee "$RUNNER_TEMP/archive-build.log"
```

| Run | Workflow path | name | head_sha | event | conclusion | job |
|-----|---------------|------|----------|-------|------------|-----|
| 40296330498 | `.github/workflows/pre-release.yaml` | Pre-release sync/v1.7.0 (dry run) | `b847fbdbf54a42a94738bb7d1fb0c6668a58d5c2` | workflow_dispatch | success | `build_release` (121483182473) success |
| 40296481765 | `.github/workflows/release.yaml` | Release sync/v1.7.0 (dry run) | `b847fbdbf54a42a94738bb7d1fb0c6668a58d5c2` | workflow_dispatch | success | `build_release` (121484137199) success |

The `Wrapper Gradle` annotation of each run (notice level, identical text):

```text
Wrapper Gradle | notice | Gradle 9.8.1 from the wrapper ran createConfluentArchive and verifyConfluentArchive
```

Other annotations of both runs: `Archive` (`clickhouse-kafka-connect-v1.7.0_gg4.zip 12453663 bytes sha256=5a8522a8167f3c039c6033b70ed13967643cf61001765efe8a39f2d8d7a5638c`), `tests.yaml on this SHA` (1 successful run on `b847fbdb`), `Draft asset attached`, `No tag for the draft`, `Dry run cleaned up` (no draft and no tag remain), and the advisory `Release tag` warning (tag `v1.7.0_gg4` already exists, advisory on a dry run), plus ten compiler deprecation warnings.

`VERSION` is the only release-path input that changed since the dry runs. `git diff --name-only $D HEAD` over `src/main`, `build.gradle.kts`, `settings.gradle.kts`, `gradle`, `gradlew`, `ConfluentArchiveCheck.java`, `config` and both release workflows is empty, and each workflow holds one `./gradlew createConfluentArchive` line. Nothing was dispatched and nothing was pushed.

Matches text: pending (32-02, 32-03)

### E-05: origin, the merge shape, src/main, the integration gate run and GitHub's state (D-02, D-08)

Backs: `docs/ROLLOUT-v1.7.0_gg5.md` (the rollback target, the merge shape, claim 1 `src/main`, the integration gate) written in plan 32-02, and the CHANGELOG and FORK-DELTA lines on the same facts and on the workflows, Dependabot and the nightly (32-03). Measured at commit `a3b7c236` (the tip after E-03 and E-04), 2026-10-26 about 21:09 UTC. `sync/v1.7.0` continues from `GG4X` (`3d847009`), not from `PRE`, so `git rev-list --left-right --count origin/custom_build...HEAD` shows 1 on the left and the merge check uses the merge shape, never ancestry of `PRE` (PT-02). The GitHub bodies are unauthenticated REST reads (rate limit 60, 54 remaining after the six reads) saved under `/tmp/gsd32/exec/api/` and parsed from there as the expected types.

```sh
PRE=21c51598b06b6f5af58430765d57b1e5f6747c5a
git fetch --quiet origin --no-tags
git ls-remote --heads origin
git ls-remote --tags origin
git rev-list HEAD..origin/custom_build
git rev-parse $PRE^2
git diff --quiet $PRE 3d84700929f298493e9aeb3ca1dc210608594b42; echo $?
git merge-base --is-ancestor 3d84700929f298493e9aeb3ca1dc210608594b42 HEAD; echo $?
git merge-tree --write-tree $PRE HEAD
git rev-parse 'HEAD^{tree}'
git rev-parse 'v1.7.0_gg4^{commit}'
git rev-list --left-right --count origin/custom_build...HEAD
```

```text
21c51598b06b6f5af58430765d57b1e5f6747c5a	refs/heads/custom_build
ca52b81ad1a188e1e288336a2231d9d3092d45fe	refs/heads/dependabot/gradle/clickHouseDriver-0.10.0
011bc42bfe8d8230c430ec8001d481885daec5fa	refs/heads/dependabot/gradle/com.google.protobuf-protobuf-java-4.36.2
bec167d85918e77c11621063000acc6e031ae2fa	refs/heads/main
6ac2d20cc0c7feacc315b21b322325b4fee25784	refs/tags/pre-v1.6.0-sync
c61d90ce5dfb32ea5876dffacc8bb54d8846739a	refs/tags/pre-v1.7.0-sync
be6b8861c25f3d7611194e7150b18362c5c75e32	refs/tags/pre-v1.7.0_gg2
6473e24f372a8b37dfd8cc8cf3124e6cff52edca	refs/tags/pre-v1.7.0_gg3
69cd680bf548bfb0329142752d37f8a6c3190c26	refs/tags/pre-v1.7.0_gg4
c61d90ce5dfb32ea5876dffacc8bb54d8846739a	refs/tags/v1.6.0_gg1
f328fd218dce5b9c652f8872042e922f9686b87f	refs/tags/v1.7.0_gg1
a246ca01b073faae86e2e92bb0288c07dd7eb0d7	refs/tags/v1.7.0_gg2
3376a18fd057dba329ceae7d8c74d1ba4e3d36f6	refs/tags/v1.7.0_gg3
21c51598b06b6f5af58430765d57b1e5f6747c5a	refs/tags/v1.7.0_gg4
21c51598b06b6f5af58430765d57b1e5f6747c5a
3d84700929f298493e9aeb3ca1dc210608594b42
0
0
bf54369d9109b953a135fcf2a1eed0c58d5cb10d
bf54369d9109b953a135fcf2a1eed0c58d5cb10d
21c51598b06b6f5af58430765d57b1e5f6747c5a
1	75
```

Reading: `custom_build` on origin is `PRE`; `HEAD..origin/custom_build` is exactly `PRE`; `PRE^2` is `GG4X` and an ancestor of HEAD; `PRE` and `GG4X` have equal trees (exit 0); the merge of `PRE` and HEAD produces HEAD's tree (`bf54369d` at `a3b7c236`), so the release merge is clean and publishes the tip's tree. Tag `v1.7.0_gg4` names `PRE`. Neither `v1.7.0_gg5` nor `pre-v1.7.0_gg5` exists on origin yet.

`src/main` against `v1.7.0_gg4`, what gg5 changes outside `.planning`, and the integration gate inputs against the commit of Phase 31's green run (E-18 there):

```sh
git diff --quiet v1.7.0_gg4 HEAD -- src/main; echo $?
git diff --stat v1.7.0_gg4 HEAD -- src/main
git diff --name-only v1.7.0_gg4 HEAD -- . ':(exclude).planning'
git diff --quiet c4b409db87d36d7599e472d38f3a4bc5a118c646 HEAD -- src/main src/integrationTest src/testFixtures build.gradle.kts settings.gradle.kts gradle; echo $?
sed -n '414p;421p' .planning/phases/31-fork-owned-nightly-integration-tests/31-COMMAND-EVIDENCE.md
```

```text
0
(no stat output: src/main is byte-identical)
.github/dependabot.yml
.github/scripts/failed_tests_summary.py
.github/workflows/increment-version.yml
.github/workflows/nightly-integration-tests.yaml
.github/workflows/pre-release.yaml
.github/workflows/release.yaml
VERSION
docs/FORK-DELTA.md
src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java
src/test/java/com/clickhouse/kafka/connect/ci/FailedTestsSummaryScriptUnitTest.java
src/test/java/com/clickhouse/kafka/connect/ci/NightlyWorkflowGuardUnitTest.java
src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
src/test/java/com/clickhouse/kafka/connect/ci/UpstreamIntegrationWorkflowBytesGuardUnitTest.java
src/test/java/com/clickhouse/kafka/connect/ci/WorkflowActionPinGuardUnitTest.java
0
Tests summary: 55 tests, 54 succeeded, 0 failed, 1 skipped
server-version: 26.10.3.7
```

The run those two lines come from is Phase 31's E-18: commit `c4b409db87d36d7599e472d38f3a4bc5a118c646`, `integrationTest` on `latest` x V2 with the Java 17 worker, `BUILD SUCCESSFUL in 29m 26s`, image `sha256:739e099abd11...b1ce` (the image of E-02). The `integrationTest` inputs (`src/main`, `src/integrationTest`, `src/testFixtures`, `build.gradle.kts`, `settings.gradle.kts`, `gradle`) are unchanged since that commit (exit 0 above). The 14 paths outside `.planning` are the whole gg5 change set against `v1.7.0_gg4`; `src/main` is not among them. `src/integrationTest` appears in the first list only through `ConfluentPlatform.java`, which is Phase 31's harness change that sits before `c4b409db` (so it is inside the green run, not after it).

GitHub's state:

```sh
R=https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect
curl -s $R                                                                              > repo.json
curl -s $R/actions/workflows/integrationTests.yaml                                      > integration-workflow.json
curl -s "$R/actions/workflows/integrationTests.yaml/runs?event=schedule&per_page=100"   > integration-schedule-runs.json
curl -s "$R/releases?per_page=100"                                                      > releases.json
curl -s "$R/pulls?state=open&per_page=100"                                              > pulls.json
curl -s -o /dev/null -w '%{http_code}' "$R/contents/.github/workflows/nightly-integration-tests.yaml?ref=custom_build" > nightly-contents.code
grep -nE "cron:|workflow_dispatch:|NIGHTLY_ENABLED" .github/workflows/nightly-integration-tests.yaml
grep -n NIGHTLY_ENABLED .planning/scripts/fork-anchors.txt
```

```text
repo.json: default_branch is custom_build (fork: true, private: false)
integrationTests.yaml: state disabled_manually, path .github/workflows/integrationTests.yaml
schedule runs of integrationTests.yaml: total_count 2, created 2026-10-16T09:32:28Z and 2026-10-15T09:39:18Z (none after 2026-10-16T09:38:23Z)
releases.json: empty array (0 published)
open pulls: #10 dependabot/gradle/com.google.protobuf-protobuf-java-4.36.2 -> custom_build; #4 dependabot/gradle/clickHouseDriver-0.10.0 -> custom_build
nightly-integration-tests.yaml on custom_build: HTTP 404
16:    - cron: '17 3 * * *'
17:  workflow_dispatch:
25:    if: github.event_name == 'workflow_dispatch' || vars.NIGHTLY_ENABLED == 'true'
.github/workflows/nightly-integration-tests.yaml	github.event_name == 'workflow_dispatch' || vars.NIGHTLY_ENABLED == 'true'	1	NIT-01
```

So the nightly is absent from `custom_build` until the release merge brings it (it answers 404), it runs on a manual dispatch regardless of the variable and on its schedule only when the repository variable `NIGHTLY_ENABLED` equals `true`; upstream's `integrationTests.yaml` is disabled by hand and has not run on a schedule since 2026-10-16.

origin_custom_build: 21c51598b06b6f5af58430765d57b1e5f6747c5a
left_commits: 21c51598b06b6f5af58430765d57b1e5f6747c5a
merge_tree_equals_tip: yes
src_main_vs_gg4: 0
gate31_inputs_unchanged: yes
default_branch: custom_build
integrationTests_state: disabled_manually
nightly_on_custom_build: 404
releases: 0
open_prs: #4 dependabot/gradle/clickHouseDriver-0.10.0, #10 dependabot/gradle/com.google.protobuf-protobuf-java-4.36.2
nightly_toggle_grep: 16:    - cron: '17 3 * * *' / 17:  workflow_dispatch: / 25:    if: github.event_name == 'workflow_dispatch' || vars.NIGHTLY_ENABLED == 'true'

Matches text: pending (32-02, 32-03)

### E-10: claims 1 and 4 of the gg5 note re-run on the tip (D-03)

Backs: `docs/ROLLOUT-v1.7.0_gg5.md` section 1 (`src/main` is empty against `v1.7.0_gg4`) and section 4 (how the nightly is turned on and off), written in plan 32-02. Measured on the tip `b7592d1d72546c29bee09e0ecca1ff0eb8734075` (HEAD at the start of plan 32-02), before the tag `v1.7.0_gg5` exists, so the note's command is shown with `HEAD` for `v1.7.0_gg5`.

```sh
git diff --quiet v1.7.0_gg4 HEAD -- src/main; echo $?
git diff --stat v1.7.0_gg4 HEAD -- src/main
```

```text
0
(no stat output: src/main is byte-identical)
```

Matches text: yes
