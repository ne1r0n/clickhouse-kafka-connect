# Phase 31: Command evidence

- **Base commit:** `P31_BASE` = `41d965e2d6d04cd5fa335291b999fe6721fc53d3` (HEAD when plan 31-01 started, branch `sync/v1.7.0`; `.planning` commits follow the planning-time HEAD `e3ab7a8c`). "src/main is unchanged" means `git diff --quiet v1.7.0_gg4 HEAD -- src/main`.
- **Shell:** macOS zsh. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`; live runs set `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2` explicitly. From E-02 on, every Gradle run also sets `JAVA_HOME` to the Gradle-provisioned Temurin 17 (see the deviation in E-02); this is an invocation-time environment variable, not a repository change.
- **Docker:** colima profile `vz` (aarch64, 8 CPU, 16 GiB, 256 GiB disk), `DOCKER_HOST` pointing at the vz socket.
- **Date (UTC):** 2026-10-26.
- **Rule:** a `Matches text: no` verdict means the document gets fixed, never the evidence. Entries that back no document text carry `Matches text: n/a (gate check)`.

### E-01: phase base, Docker and toolchain environment, image inventory, latest pulled and SELECT version() (D-06)

Backs: criterion 1 (the measurement exists before any workflow file); D-06 (images and `latest` drift).

```sh
git rev-parse HEAD
git rev-parse --abbrev-ref HEAD
git diff --quiet v1.7.0_gg4 HEAD -- src/main; echo $?
colima list
docker version --format '{{.Client.Version}} {{.Server.Version}} {{.Server.Arch}}'
docker info --format '{{.NCPU}} {{.MemTotal}} {{.Architecture}} {{.OperatingSystem}}'
docker system df
sw_vers -productVersion
sysctl -n hw.ncpu hw.memsize
python3 --version
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain --version
# image inventory before the run, one `docker image inspect --format '{{.Id}}'` per reference
docker pull clickhouse/clickhouse-server:latest
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
```

```text
P31_BASE: 41d965e2d6d04cd5fa335291b999fe6721fc53d3
branch: sync/v1.7.0
git diff --quiet v1.7.0_gg4 HEAD -- src/main: exit status 0

PROFILE    STATUS     ARCH       CPUS    MEMORY    DISK      RUNTIME    ADDRESS
vz         Running    aarch64    8       16GiB     256GiB    docker     192.168.64.4

docker client 29.8.2, server 27.4.0, server arch arm64
docker info: 8 CPU, 16733589504 bytes memory, aarch64, Ubuntu 24.04.1 LTS
macOS 26.5.1; host hw.ncpu 12, hw.memsize 34359738368
Python 3.14.8
Gradle 9.8.1; launcher JVM and daemon JVM 21.0.12.1 (Homebrew), no JAVA_HOME set; JAVA_HOME unset in the shell
TESTCONTAINERS_HOST_OVERRIDE in the agent shell: 192.168.64.4 (unset for every Gradle run)

docker system df before the run:
TYPE            TOTAL     ACTIVE    SIZE      RECLAIMABLE
Images          23        2         13.51GB   19GB (140%)
Containers      2         1         313.1MB   313.1MB (99%)
Local Volumes   44        2         1.035GB   724.3MB (69%)
Build Cache     9         0         54.65kB   54.65kB

image inventory before the run (10 lines):
confluentinc/cp-zookeeper:7.9.6 absent
confluentinc/cp-server:7.9.6 absent
confluentinc/cp-schema-registry:7.9.6 absent
confluentinc/cp-enterprise-control-center:7.9.6 absent
confluentinc/cp-ksqldb-server:7.9.6 absent
confluentinc/cp-kafka-rest:7.9.6 absent
cnfldemos/cp-server-connect-datagen:0.6.4-7.6.0 absent
clickhouse/clickhouse-server:latest present sha256:bdb5c911a23d92fd364e246ab20b5a0a485f00112f3663fa626b083fb46c146b
ghcr.io/shopify/toxiproxy:2.7.0 present sha256:fc2d40f4904c9ab66ba7c49dca969b8116a71d441973d6f124a72d5c172d3b75
testcontainers/ryuk present 0.14.0

after `docker pull clickhouse/clickhouse-server:latest`:
Digest: sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
Status: Downloaded newer image for clickhouse/clickhouse-server:latest
clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
created 2026-10-26T00:06:54.855447882Z, architecture arm64

SELECT version() (entrypoint form `clickhouse local` worked; the `clickhouse-local` fallback was not needed):
26.10.3.7
```

The pull replaced the image that was present before the run (id `bdb5c911...`, 11 days old) with a newer `latest` (digest `739e099a...`, created 2026-10-26), so `latest` drifted between the last local pull and now, which is the CI condition (CI always pulls fresh). The six Confluent images and the datagen Connect image are absent locally: the full run in E-03 is a cold pull. The Docker server is 27.4.0 (the plan's text expected a 29.x server; the client is 29.8.2); recorded as measured.

Matches text: n/a (gate check)

### E-02: the integrationTest slice: the assumption-aborted cloud class is reported as skipped (D-04)

Backs: D-04 and research A4 (how Gradle reports a class aborted in `@BeforeAll`).

Deviation (Rule 3, environment, no repository change): the first attempt, with the Gradle prefix exactly as planned, did not reach a test. The Gradle daemon runs on Homebrew JDK 21 (no `JAVA_HOME`), the `integrationTest` toolchain resolves to an IBM Semeru JDK 17 (OpenJ9), and `build.gradle.kts` lines 197-198 copy every Gradle JVM system property, `java.home` among them, into the test JVM. The OpenJ9 17 test JVM then loads the JDK 21 class library and dies. Cause: a daemon JVM that differs from the toolchain JVM, which CI never has (`actions/setup-java` Temurin 17 sets both). The second attempt sets `JAVA_HOME` to the Gradle-provisioned Temurin 17 (`~/.gradle/jdks/adoptium-17-aarch64-hotspot-mac/Contents/Home`, 17.0.9+9, HotSpot), so the daemon and the test JVM are the same HotSpot JDK 17, the same vendor and family as CI. Pointing `JAVA_HOME` at a Semeru directory would also work and keep OpenJ9; it was not used.

```sh
rm -rf build/test-results/integrationTest build/reports/tests/integrationTest
# attempt 1 (exit 1, 5 s)
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain integrationTest --tests '*ClickHouseCloudTest'
# attempt 2 (exit 0, 13 s)
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE JAVA_HOME=$HOME/.gradle/jdks/adoptium-17-aarch64-hotspot-mac/Contents/Home ./gradlew --console=plain integrationTest --tests '*ClickHouseCloudTest'
```

```text
attempt 1: exit status 1
> Task :integrationTest FAILED
JVMCFRE199E bad major version 65.0 of class=java/lang/Object, the maximum supported major version is 61.0; offset=6
JVMJ9VM019E Unrecoverable error: Unable to find and initialize required class java/lang/Object
JVMJ9VM023I This may indicate that JAVA_HOME is incorrect, or that class libraries are not installed
Process 'Gradle Test Executor 14' finished with non-zero exit value 1

attempt 2: exit status 0
> Task :prepareConfluentArchive UP-TO-DATE
> Task :integrationTest
ClickHouseCloudTest > overlappingDataTest() SKIPPED
Test result: SUCCESS
Tests summary: 1 tests, 0 succeeded, 0 failed, 1 skipped
BUILD SUCCESSFUL in 12s

build/test-results/integrationTest/TEST-com.clickhouse.kafka.connect.sink.ClickHouseCloudTest.xml:
<testsuite name="com.clickhouse.kafka.connect.sink.ClickHouseCloudTest" tests="1" skipped="1" failures="0" errors="0" ...>
  <testcase name="overlappingDataTest()" classname="com.clickhouse.kafka.connect.sink.ClickHouseCloudTest" time="0.0"><skipped/></testcase>
  ... [Test worker] WARN ClickHouseCloudTest - clickhouse.host system property is required to connect to cloud, skipping tests
```

How Gradle reports the class aborted in `@BeforeAll` (research A4, now measured): the `TestAbortedException` from the assumption is written to the JUnit XML as a skipped testcase for the class's test method, so the class has an XML file with `tests=1 skipped=1 failures=0 errors=0`, the console prints `overlappingDataTest() SKIPPED`, and the build is green. It is neither absent from the XML nor a failure. `prepareConfluentArchive` was up to date from an earlier build (the archive build is exercised in E-03). No container was started.

Matches text: n/a (gate check)

### E-03: the cold integrationTest run on ClickHouse latest x client V2 (criterion 1)

Backs: criterion 1, D-04, D-05. Whole `integrationTest` task, no Gradle test filter, no `-D` ClickHouse property, no cluster-name variable, no cloud version.

```sh
rm -rf build/test-results/integrationTest build/reports/tests/integrationTest
git rev-parse HEAD
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE JAVA_HOME=$HOME/.gradle/jdks/adoptium-17-aarch64-hotspot-mac/Contents/Home ./gradlew --console=plain integrationTest
```

```text
run commit: 07fc1f31aa4373a89fd690515d88524f88c05358
start (UTC): 2026-10-26T11:41:44Z   end (UTC): 2026-10-26T11:55:06Z   wall: 802 s, W = ceil(802 / 60) = 14 min
Gradle: BUILD FAILED in 13m 14s; exit status 1
Test result: FAILURE
Tests summary: 55 tests, 0 succeeded, 54 failed, 1 skipped
55 tests completed, 54 failed, 1 skipped

| Class                                   | tests | failures | errors | skipped | time (s) |
|-----------------------------------------|-------|----------|--------|---------|----------|
| ClickHouseSinkConnectorIntegrationTest  | 54    | 54       | 0      | 0       | 788.525  |
| ExactlyOnceTest                         | 8     | 0        | 0      | 8       | 0.025    |
| ClickHouseCloudTest                     | 1     | 0        | 0      | 1       | 0.026    |

ClickHouseSinkConnectorIntegrationTest: 25 avroSchemaTest + 22 protoSchemaTest invocations + 7 stockGen* methods = 54 testcases, all failed.
Sum of its testcase times: 330.3 s; the other 458 s of the class time are class setup (cold image pulls and container start).
```

How the two assumption-aborted classes were reported (measured, extends E-02): `ClickHouseCloudTest` has one XML testcase, skipped, and a console `ClickHouseCloudTest > overlappingDataTest() SKIPPED` line. `ExactlyOnceTest` has no console line at all and is absent from the console `Tests summary:` (55 = 54 + 1), but its XML file lists all 8 of its parameterized methods as skipped (`tests=8 skipped=8 failures=0 errors=0`). So the console summary undercounts: the XML (63 testcases) is the source for the nightly's reports. Both classes are skipped, not failed (D-04).

All 54 failures end at the same line: `AssertionFailedError: expected: <true> but was: <false>` at `ConfluentPlatform.createConnectorAndWaitUntilRunning(ConfluentPlatform.java:360)`, the `Assertions.assertTrue(code >= 200 && code < 300)` on the response code of `POST /connectors`. They are called from `setupAvroConnector` (avro, line 231), `setupProtobufConnector` (proto, line 245) and `setupConnector` (stockGen, line 218). No test reached the 5-minute RUNNING wait. No OpenJ9 or JVM crash: the test JVM was HotSpot (E-02), exit status 1 came from the 54 failed tests. No `core.*`, `javacore.*`, `Snap.*` or `jitdump.*` file appeared.

Matches text: n/a (gate check)

### E-04: images, the worker and the capture (D-06)

Backs: D-06 (image list, tags and `latest` drift) and the criterion-1 differences record.

```sh
docker image inspect --format '{{index .RepoTags 0}} {{.Id}} {{index .RepoDigests 0}} {{.Architecture}} {{.Created}}' <ref>   # ten refs, after the run
docker run --rm --entrypoint bash cnfldemos/cp-server-connect-datagen:0.6.4-7.6.0 -c 'java -version 2>&1; ls /usr/share/java/kafka/ | grep -E "^(connect-api|kafka-clients)-"'
python3 -I  # zipfile: bytes 4-8 of com/clickhouse/kafka/connect/ClickHouseSinkConnector.class in build/confluentArchive/**/lib/*-confluent.jar
docker system df
```

```text
tag                                                  image id = repo digest (identical for every image: containerd image store, single-platform manifests)      arch   created
confluentinc/cp-zookeeper:7.9.6                      sha256:d3294ad2b353ac5a155ad9abe37884f75c482f387af553679210303b7118a0d3   arm64  2026-03-16
confluentinc/cp-server:7.9.6                         sha256:7021128926d83f34edb8be6196f202bf512e5c1ab998b8fbb163a53122ecebae   arm64  2026-03-16
confluentinc/cp-schema-registry:7.9.6                sha256:1efa1ed67e6937e4c82ae34d761eee7354e6b79df5d459ff387735af0a2849d1   arm64  2026-03-16
confluentinc/cp-enterprise-control-center:7.9.6      sha256:024e3b9ed086ce445d32ced8c062983b364d133d59a7685d5182850ca9098a46   arm64  2026-03-16
confluentinc/cp-ksqldb-server:7.9.6                  sha256:8d9573347642c9405b7c10566e29bd83e0b866f13029b343bc59337903f9f14d   arm64  2026-03-16
confluentinc/cp-kafka-rest:7.9.6                     sha256:b1dbbf701f7a60c3597cc0419f99faf6a8c5239cd541ab593ec00be553117ae0   arm64  2026-03-16
cnfldemos/cp-server-connect-datagen:0.6.4-7.6.0      sha256:0ed7bc9785bcd81595460e1908795e2e0772582a4f79359ad5daaab1001012d3   arm64  2024-03-05
clickhouse/clickhouse-server:latest                  sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce   arm64  2026-10-26
ghcr.io/shopify/toxiproxy:2.7.0                      sha256:fc2d40f4904c9ab66ba7c49dca969b8116a71d441973d6f124a72d5c172d3b75   arm64  2023-10-25
testcontainers/ryuk:0.14.0 (the Ryuk tag seen in the run)  sha256:7c1a8a9a47c780ed0f983770a662f80deb115d95cce3e2daa3d12115b8cd28f0   arm64  2025-09-26

Repo digest of the ClickHouse image used by the run: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
  = the digest pulled in E-01 just before the slice and the run (no drift between the pull and the run).

Connect worker (cnfldemos/cp-server-connect-datagen:0.6.4-7.6.0):
openjdk version "11.0.21" 2023-10-17 LTS
OpenJDK Runtime Environment Zulu11.68+17-CA (build 11.0.21+9-LTS)
connect-api-7.6.0-ce.jar
kafka-clients-7.6.0-ce.jar

Connector under test: com/clickhouse/kafka/connect/ClickHouseSinkConnector.class in build/confluentArchive/clickhouse-kafka-connect-v1.7.0_gg4/lib/clickhouse-kafka-connect-v1.7.0_gg4-confluent.jar: major=61 minor=0 (Java 17)
upstream v1.7.0 and upstream/main build.gradle.kts: sourceCompatibility = targetCompatibility = VERSION_11 and options.release.set(11), i.e. class file 55 (git show v1.7.0:build.gradle.kts)
this fork's build.gradle.kts: java toolchain 17, no release or source/target compatibility line

Capture loop (docker stats --no-stream every 60 s, 13 snapshots, portainer excluded; container names are random and are not mapped to images):
peak concurrent memory of the run's containers: 5122 MiB with 10 containers (the largest single container peaked at 1617 MiB)
cold pull and start: the first snapshot with all 10 containers was the 9th, at about 8 minutes into the run (class setup took about 7.6 minutes)

docker system df, before the run (E-01) and after the run:
before: Images 23 / 13.51GB, Containers 2, Local Volumes 44 / 1.035GB
after:  Images 31 / 24.98GB, Containers 2, Local Volumes 44 / 1.035GB   (images grew by about 11.5 GB; no container or volume was left behind by the run)

Connect worker log excerpts (errors only; `docker logs` of the one Connect container of the class, captured mid-run):
[2026-10-26 11:46:37,935] ERROR Failed to discover SinkConnector in /usr/share/confluent-hub-components/clickhouse-kafka-connect-v1.7.0_gg4 (org.apache.kafka.connect.runtime.isolation.PluginScanner)
java.lang.UnsupportedClassVersionError: com/clickhouse/kafka/connect/ClickHouseSinkConnector has been compiled by a more recent version of the Java Runtime (class file version 61.0), this version of the Java Runtime only recognizes class file versions up to 55.0
(the same error for ExtractTopic$Value, ExtractTopic$Key, KeyToValue, HeaderToValue, HeadersToValue, ConvertToMapOrJson and TableOperationFilter: the plugin scanner finds no connector and no transformation of the fork)
"POST /connectors HTTP/1.1" 500 2000   (48 times in the mid-run snapshot, once per failed test seen so far; 2 other POSTs returned 201: the Datagen source connectors)
org.apache.kafka.connect.errors.ConnectException: Failed to find any class that implements Connector and which name matches com.clickhouse.kafka.connect.ClickHouseSinkConnector, available connectors are: PluginDesc{klass=class io.confluent.kafka.connect.datagen.DatagenConnector, ...
(unrelated worker start-up noise present in the same log: LinkageError for the Confluent JsonConverter in /usr/share/java/confluent-metadata-service)
```

Matches text: n/a (gate check)

### E-05: measurement record (criterion 1)

Backs: criterion 1 (ROADMAP), D-04, D-05, D-06, D-09. Sources: E-01 to E-04 and the XML of the cold run.

#### Server

server-version: 26.10.3.7

Image `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (`latest` pulled 2026-10-26 just before the run, created 2026-10-26T00:06:54Z, arm64). `SELECT version()` was run with `clickhouse local` from that image (E-01). The server was started by the tests but never reached by a passing test: every test failed before an insert (see below), so the version is that of the image, not of a tested insert path.

#### Test classes

| Class | Status | tests | failures | errors | skipped | time (s) |
|-------|--------|-------|----------|--------|---------|----------|
| ClickHouseSinkConnectorIntegrationTest | ran | 54 | 54 | 0 | 0 | 788.525 |
| ExactlyOnceTest | skipped | 8 | 0 | 0 | 8 | 0.025 |
| ClickHouseCloudTest | skipped | 1 | 0 | 0 | 1 | 0.026 |

`ClickHouseCloudTest` and `ExactlyOnceTest` abort in `@BeforeAll` through assumptions and are reported as skipped in the XML, not failed (D-04 confirmed by measurement, E-02 and E-03). The console summary counts neither `ExactlyOnceTest` nor its 8 methods; the XML does.

#### Failures and proposed causes

skip-candidates: 54

All 54 failed testcases share one cause, one failing line and one worker-side error: the fork's connector jar is Java 17 bytecode (class file 61) and the harness's Connect worker (`cnfldemos/cp-server-connect-datagen:0.6.4-7.6.0`, CP 7.6.0) runs Java 11 (class files up to 55), so the worker cannot load `ClickHouseSinkConnector` and `POST /connectors` returns 500 (E-04). The tests fail at `ConfluentPlatform.java:360` in their own setup call, not in `@BeforeAll`. The fork's Java 17 level follows from its Kafka pin: `connect-api` 4.4.0 declares Java 17 as its minimum, and a probe that forced `options.release.set(11)` through a scratch Gradle init script (not a repository change) stopped at dependency resolution with "'org.apache.kafka:connect-api:4.4.0' is only compatible with JVM runtime version 17 or newer". So lowering the bytecode level is not available while that pin stands. Upstream's v1.7.0 build is Java 11 (class file 55); that is a fork difference, not a harness fault shared with upstream. A Java 17 worker is the way through, and the tag list of `cnfldemos/cp-server-connect-datagen` on Docker Hub (read-only API, 2026-10-26) shows `0.6.4-7.6.0` as the newest tag with an arm64 image and `0.6.7-8.0.0` (CP 8.0, amd64 only) as the newest tag overall. Whatever fixes the worker, the first cause masks everything behind it: no test got as far as the connector starting, so a binary mismatch between connect-api 4.4.0 and the worker's `connect-api-7.6.0-ce.jar`, or any ClickHouse-side failure, is not yet observable.

| test (class#method) | kind | invocations failed/total | proposed category | evidence | proposed one-line reason | proposed re-open trigger |
|---------------------|------|--------------------------|-------------------|----------|--------------------------|--------------------------|
| ClickHouseSinkConnectorIntegrationTest#avroSchemaTest | AssertionFailedError | 25/25 | harness | ConfluentPlatform.java:360 via setupAvroConnector (line 231); worker UnsupportedClassVersionError 61 vs 55 (E-04) | Connect worker image runs Java 11 and cannot load the Java 17 connector jar, so POST /connectors returns 500 | the harness uses a Connect worker with Java 17 or newer |
| ClickHouseSinkConnectorIntegrationTest#protoSchemaTest | AssertionFailedError | 22/22 | harness | ConfluentPlatform.java:360 via setupProtobufConnector (line 245); same worker error | same: Java 11 worker cannot load the Java 17 connector jar | same |
| ClickHouseSinkConnectorIntegrationTest#stockGenSingleTaskTest | AssertionFailedError | 1/1 | harness | ConfluentPlatform.java:360 via setupConnector (line 218) | same | same |
| ClickHouseSinkConnectorIntegrationTest#stockGenSingleTaskSchemalessTest | AssertionFailedError | 1/1 | harness | same | same | same |
| ClickHouseSinkConnectorIntegrationTest#stockGenMultiTaskTopicTest | AssertionFailedError | 1/1 | harness | same | same | same |
| ClickHouseSinkConnectorIntegrationTest#stockGenMultiTaskSchemalessTest | AssertionFailedError | 1/1 | harness | same | same | same |
| ClickHouseSinkConnectorIntegrationTest#stockGenSingleTaskInterruptTest | AssertionFailedError | 1/1 | harness | same; 131.2 s (longer than the others, cause not examined) | same | same |
| ClickHouseSinkConnectorIntegrationTest#stockGenMultiTaskInterruptTest | AssertionFailedError | 1/1 | harness | same; 131.2 s | same | same |
| ClickHouseSinkConnectorIntegrationTest#stockGenWithJdbcPropSingleTaskTest | AssertionFailedError | 1/1 | harness | same | same | same |

The category is `harness` because the artifact is correct for its production target (CP 7.9.x workers) and the test image is what is too old; if the user instead holds that the connector must load on Java 11 workers, the same rows are `connector-defect`, and the only fix then is the Kafka pin or the Java level, a decision about `build.gradle.kts` and the version catalog and not a `src/main` edit (D-09). Two of the rows are parameterized (`avroSchemaTest` 25 invocations, `protoSchemaTest` 22 invocations): a `class#method` entry drops all invocations of the method, and the failing fixtures are all of them. If every proposed entry were excluded, 0 tests of `ClickHouseSinkConnectorIntegrationTest` would still execute, and 0 tests in the whole task: the nightly would run nothing but 9 assumption-skipped testcases.

#### Why upstream's standalone latest x V2 job fails (run 38881153720)

Run 38881153720 is the fork's own scheduled run #2 of the upstream-identical `integrationTests.yaml` on fork code (SHA `a246ca01`), so the local run compares directly with it. Compared with the facts in research (Gradle step failed after 29.6 minutes; red since upstream's 2026-06-22 merge of the Protobuf integration tests; log unreadable without a token):

- Hypothesis 1 (a failing test added with the Protobuf tests): refuted as the cause of the fork's failure. Avro, Protobuf and all seven stockGen tests fail identically at one line, so no single test is at fault.
- Hypothesis 2 (binary skew between connector and worker): supported, in the form of a class-file level mismatch (61 against 55), measured in the worker log and the jar. API-level skew behind it is untested.
- Hypothesis 3 (runner capacity): not supported locally (peak 5.1 GiB of 16 GiB, no OOM); CI memory and disk are untested.
- Hypothesis 4 (a `latest` ClickHouse change): not supported; the server is never reached by a failing test.

The local failure site, `createConnectorAndWaitUntilRunning` with a non-2xx POST, equals the death site recorded in memory for the fork's red nightly. Not explained: the CI Gradle step took 29.6 minutes against 13.4 minutes here, although a 500 on POST returns at once and no 5-minute wait is reached; a slower 4 vCPU runner and cold pulls may account for it, or the CI failure has an additional cause. Upstream's own daily red cannot be this cause, because upstream compiles for Java 11. The CI log would settle both points (token route, available at the Task 3 checkpoint).

upstream-red: reproduced
(reproduced for the fork's run 38881153720 at the symptom level: all connector-dependent tests: avroSchemaTest, protoSchemaTest and the seven stockGen* tests, fail at ConfluentPlatform.java:360 because the Java 11 worker cannot load the Java 17 connector; not confirmed against that run's log)

#### Timeout (D-05)

Cold wall time W = 14 minutes (802 s, E-03). The run was cold for the Confluent images (all absent, about 11.5 GB pulled, about 7.6 minutes of class setup) and the ClickHouse image was freshly pulled.

nightly-timeouts: step=30 job=40 (cold local wall 14 min x 2, rounded up to a multiple of 5; job = step + 10)

CI references (research V8, V9): about 20 minutes for a healthy standalone `latest` x V2 job on a hosted runner; 30 to 52 minutes for red runs, and 29.6 minutes for the fork's own red run 38881153720. The measured 14 minutes is a red run that failed fast: no test waited on a connector, so a green run (real ingestion and read-back, several connectors) will take longer than 14 minutes locally. Can a red CI run reach S = 30? Yes: a run where connectors accept the POST but never reach RUNNING waits 5 minutes per test and would be cut off by the step timeout; the `always()` report steps still run in the 10 minutes between step and job timeout (PT-02). The 29.6-minute red run sits just under S. If the user wants more room, the Task 3 answer can amend the timeouts.

#### Images (D-06)

The ten images with tag, image id, repo digest and architecture are in E-04. Only `clickhouse/clickhouse-server:latest` floats; its digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` equals the one pulled just before the run. `latest` drifts: the image present before the pull (id `bdb5c911...`, 11 days old) differs from the one used. Drift is accepted (D-06); this run pins nothing.

#### Differences from the runner

- Docker: arm64 colima, Docker server 27.4.0 (client 29.8.2), against x86 `ubuntu-24.04` with Docker 28.0.4. All used images are multi-arch.
- `TESTCONTAINERS_HOST_OVERRIDE`: unset for the run (`env -u`); containers and Ryuk were reachable with it unset, so the retry with the VM address (research A11) was not needed.
- Resources: 8 CPU, 16 GiB RAM, 256 GiB disk locally against 4 vCPU, 16 GB RAM and 14 GB SSD on the runner. The run added about 11.5 GB of images.
- Test forks: the build sets `maxParallelForks` only on the `test` task (lines 218 and 220); `integrationTest` runs one test JVM at the Gradle default locally and on the runner.
- JVM: Gradle and the test JVM ran on Temurin 17.0.9 (HotSpot) through `JAVA_HOME` (E-02), the same family as CI's `actions/setup-java` Temurin 17. OpenJ9 was not used, so the OpenJ9 difference does not apply to this run. Without `JAVA_HOME`, this machine's default fails before any test (E-02).
- Client version: `CLIENT_VERSION=V2` reaches the test-side helper client and the three connector configs that set `client_version` (`clickhouse_sink_avro.json`, `clickhouse_sink_avro_json.json`, `clickhouse_sink_protobuf.json`); the other connector configs use the connector's own default, `props.getOrDefault(CLIENT_VERSION, "V1")` (`ClickHouseSinkConfig.java` line 331). The JSON files are not changed, because that would change what is tested.

#### CI-only risks for the first dispatch

- [ ] Disk: about 11.5 GB of image layers on a 14 GB SSD. Diagnosed by the capacity probe step (`df -h /`) and the Docker server line in the step summary.
- [ ] Memory: ten containers peaked at 5.1 GiB here plus the test JVM; the runner has 16 GB. Diagnosed by the capacity probe (`free -m`) and the uploaded XML (an OOM-killed container shows as a connector that never starts).
- [ ] x86 against arm64: the images are multi-arch, but nothing was run on x86. Diagnosed by the failed-test summary and the uploaded XML.
- [ ] Anonymous Docker Hub pull limits on shared runner addresses (ten pulls per run). Diagnosed by the Gradle step log in the uploaded report and a pull error in the XML.
- [ ] `latest` drift: a new ClickHouse build between this measurement and the dispatch. Diagnosed by the digest in the capacity step and the server version in the XML output.
- [ ] Step timeout: S = 30 minutes may cut off a slow or red run (see the timeout section). Diagnosed by a cancelled Gradle step with the `always()` upload steps still present.
- [ ] The Java 11 worker (this record's finding) is independent of the runner and fails the first dispatch the same way unless the harness question is settled.

run-commit: 07fc1f31aa4373a89fd690515d88524f88c05358

Matches text: n/a (gate check)

### E-06: the user's disposition of the measurement (D-07, D-08, D-09)

Backs: plan 31-01 Task 3 (blocking-human decision); read by plan 31-02's tracer.

user_answer_received_by: orchestrator (direct)

user_answer: fix-harness (Recommended)

disposition: fix-harness

```text
skip-entries: pending
```

No later plan of this phase starts until a gap plan (`/gsd-plan-phase 31 --gaps`) has re-measured and recorded `### E-06b` with a numeric `skip-entries:` line. The proposed timeouts (step 30, job 40) and the upstream-red explanation were not amended; the E-05 `nightly-timeouts:` line stands pending the re-measurement.

Matches text: n/a (gate check)

### E-17: worker candidates, the broker-free plugin probe and the harness tracer (D-06, D-09)

Backs: D-09 (the harness fix and its reason), D-06 (images), criterion 1 (a harness that can load the fork's connector).

```sh
docker pull confluentinc/cp-server-connect:7.9.6
docker image inspect --format '{{index .RepoTags 0}} {{.Id}} {{index .RepoDigests 0}} {{.Architecture}} {{.Created}}' confluentinc/cp-server-connect:7.9.6
docker run --rm --entrypoint bash <worker image> -c 'java -version 2>&1; ls /usr/share/java/kafka/ | grep -E "^(connect-api|connect-runtime|kafka-clients)-"'   # both workers
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE JAVA_HOME=$HOME/.gradle/jdks/adoptium-17-aarch64-hotspot-mac/Contents/Home ./gradlew --console=plain prepareConfluentArchive
docker run --rm -v <archive dir>:/usr/share/confluent-hub-components/clickhouse-kafka-connect:ro --entrypoint connect-plugin-path <worker image> list --plugin-location /usr/share/confluent-hub-components/clickhouse-kafka-connect   # both workers
docker run --rm --entrypoint bash cnfldemos/cp-server-connect-datagen:0.6.4-7.6.0 -c 'ls -d /usr/share/confluent-hub-components/confluentinc-kafka-connect-datagen; ls /usr/share/confluent-hub-components/confluentinc-kafka-connect-datagen/lib | grep "^kafka-connect-datagen-"'
docker history --no-trunc --format '{{.CreatedBy}}' cnfldemos/cp-server-connect-datagen:0.6.4-7.6.0 | head -3
curl -s 'https://hub.docker.com/v2/repositories/cnfldemos/cp-server-connect-datagen/tags?page_size=100'   # read-only, unauthenticated
curl -s 'https://hub.docker.com/v2/repositories/confluentinc/cp-server-connect/tags/7.9.6'
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE JAVA_HOME=$HOME/.gradle/jdks/adoptium-17-aarch64-hotspot-mac/Contents/Home ./gradlew --console=plain compileIntegrationTestJava
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE JAVA_HOME=$HOME/.gradle/jdks/adoptium-17-aarch64-hotspot-mac/Contents/Home ./gradlew --console=plain integrationTest --tests '*ClickHouseSinkConnectorIntegrationTest.stockGenSingleTaskTest'
bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD
```

```text
Candidate workers (the connector under test is build/confluentArchive/clickhouse-kafka-connect-v1.7.0_gg4, class file 61):

| image | JVM | Kafka jars | architectures (Docker Hub) | plugins loadable of total | UnsupportedClassVersionError | verdict |
|-------|-----|------------|----------------------------|---------------------------|------------------------------|---------|
| cnfldemos/cp-server-connect-datagen:0.6.4-7.6.0 (current worker) | OpenJDK Zulu 11.0.21 | connect-api, connect-runtime, kafka-clients 7.6.0-ce | amd64, arm64 | 0 of 8 | 8 | cannot load the fork |
| confluentinc/cp-server-connect:7.9.6 | OpenJDK Temurin 17.0.18 | connect-api, connect-runtime, kafka-clients 7.9.6-ce | amd64, arm64 | 8 of 8 | 0 | loads the fork and its seven SMTs |
| cnfldemos/cp-server-connect-datagen:0.6.7-8.0.0 (option B) | not run | not run | amd64 only | not run | not run | rejected |

confluentinc/cp-server-connect:7.9.6 image: sha256:b0a23182108658bbe097d05b416f4c591574abcec1b5d4409b6852c09d5949ce, repo digest identical, arm64, created 2026-03-16T08:40:31Z.
Loadable in the 7.9.6 image: ClickHouseSinkConnector (v1.7.0_gg4), HeaderToValue, HeadersToValue, ConvertToMapOrJson, TableOperationFilter, KeyToValue, ExtractTopic$Key, ExtractTopic$Value.
Old worker error (first line, per plugin): java.lang.UnsupportedClassVersionError: com/clickhouse/kafka/connect/ClickHouseSinkConnector has been compiled by a more recent version of the Java Runtime (class file version 61.0), this version of the Java Runtime only recognizes class file versions up to 55

Datagen plugin source (cnfldemos/cp-server-connect-datagen:0.6.4-7.6.0):
  plugin directory: /usr/share/confluent-hub-components/confluentinc-kafka-connect-datagen
  jar in lib/: kafka-connect-datagen-0.6.4.jar
  history line 1: RUN |1 KAFKA_CONNECT_DATAGEN_VERSION=0.6.4 /bin/sh -c confluent-hub install --no-prompt confluentinc/kafka-connect-datagen:$KAFKA_CONNECT_DATAGEN_VERSION # buildkit
  (the datagen image is the Confluent server Connect image plus a confluent-hub install of the datagen plugin)

Decision rule (PT-14), four results:
  (i)   confluentinc/cp-server-connect:7.9.6 runs Java 17.0.18 (>= 17): holds
  (ii)  Docker Hub lists amd64 and arm64 for confluentinc/cp-server-connect:7.9.6: holds
  (iii) connect-plugin-path: Loadable plugins 8, no UnsupportedClassVersionError: holds
  (iv)  datagen plugin directory and kafka-connect-datagen-0.6.4.jar present in the pinned datagen image: holds
  -> option A is taken.

Rejected options (Docker Hub, read-only, 2026-10-26):
  B  cnfldemos/cp-server-connect-datagen:0.6.7-8.0.0   "amd64" only; the colima VM of criterion 1 is aarch64, so it cannot run natively there; it also moves the worker to CP 8.0
  C  confluent-hub install at test time: a Confluent Hub download on every run, a network source D-06 does not list
  Tags of the datagen repository (newest first, with architectures): 0.6.7-8.0.0 amd64; 0.6.4-7.6.0 amd64,arm64; 0.6.2-7.5.0 amd64,arm64; 0.6.0-7.3.0 amd64,arm64; 0.6.0-7.2.1 amd64,arm64; older tags amd64 only. There is no datagen tag for CP 7.7 to 7.9.
  confluentinc/cp-server-connect:7.9.6 amd64,arm64

The edit (src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java): CONFLUENT_VERSION, DATAGEN_VERSION and CP_DATA_GEN keep their values; new CP_SERVER_CONNECT, DATAGEN_PLUGIN_DIR, DATAGEN_STAGE and connectWorkerImage(); the container field cp_data_gen is built from connectWorkerImage() instead of from CP_DATA_GEN. Removed lines: exactly one, `cp_data_gen = new GenericContainer<>(CP_DATA_GEN)`. The CLASSPATH env line was not changed (the new worker started without it being a cause of failure).
Image-build form that built: the primary form (a raw `FROM <datagen image> AS datagen` statement, `from(<Connect image>)`, and a raw `COPY --from=datagen <plugin dir> <plugin dir>` statement); the fallback form was not needed.

compileIntegrationTestJava: BUILD SUCCESSFUL, no warning line naming ConfluentPlatform.java.

Slice (two runs; the first passed but its capture loop recorded no worker log):
  Deviation (Rule 3, scratch tooling, no repository change): the plan's capture filter `docker ps --filter publish=8083` matches the HOST port, not the container port (measured with a throwaway container: `-p 18083:8083` matched publish=18083 only), so attempt 1 captured no worker log. capture-h.sh now selects the container whose published-ports column holds `->8083/tcp`, and takes the worker log and status every 15 s instead of 60 s. Attempt 1: exit 0, 1 test, 1 succeeded (kept in scratch, attempt1/).
  attempt 2 (the record): exit status 0, BUILD SUCCESSFUL in 1m 50s
  Test result: SUCCESS
  Tests summary: 1 tests, 1 succeeded, 0 failed, 0 skipped
  testcase stockGenSingleTaskTest() classname com.clickhouse.kafka.connect.sink.ClickHouseSinkConnectorIntegrationTest time 33.929 s, no failure, error or skipped element; suite time 105.707 s
  Worker (generated image localhost/testcontainers/wfvazv8tld1wrb8g:latest, removed when the test JVM exited): log 27598 lines, UnsupportedClassVersionError count 0
  Worker /connectors?expand=status: {"ClickHouseSinkConnector": connector RUNNING, task 0 RUNNING, type sink}
  "POST /connectors" 201 twice (the datagen source and the ClickHouse sink); no 500
  Datagen source tasks end with ConnectException "Stopping connector: generated the configured 100 number of messages", which is the datagen connector's normal end after its configured iterations.

ClickHouse used: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce, the same digest as E-01 (no latest drift). SELECT version(): 26.10.3.7

anchors mirror: rows=126 bad=0 confluent-platform-rows=3 (saved count before the edit 123, plus 3)
cumulative gate, last line: invariants: provenance_paths=21 anchor_commits=872 anchor_rows=126 marker_commits=931 anchor_exempt=19 result=PASS

Spotless (PT-19), spotlessApply in a throwaway worktree at the plan base c7965a7d08d9377bf5e0a0b3463f392d3e516574 and at the harness commit:
  changed files at the base 48, at the harness commit 47. The only difference is gradlew.bat, a line-ending rewrite that the Gradle run itself makes in a fresh checkout (the main tree has carried the same uncommitted gradlew.bat change since 31-01) and which has nothing to do with Java formatting; without it both sets hold 47 identical files. ConfluentPlatform.java is in both sets (it already violated the format before this plan: wildcard imports and others), and its formatted import set grows by exactly import org.testcontainers.images.builder.ImageFromDockerfile; and import org.testcontainers.images.builder.dockerfile.statement.RawStatement; with no import lost.
  Deviation (Rule 3, check mechanics): the plan's check compares the two file lists with gradlew.bat included and so reads 48 against 47; the comparison was recomputed from the same two lists with gradlew.bat excluded. No repository change.
```

harness-commit: 781fd52683d6adb7f49468ed6eb88374053a9c1c

Matches text: n/a (gate check)

### E-18: the integrationTest re-measurement on latest x V2 with the Java 17 worker (criterion 1, D-04, D-05, D-06, D-08)

Backs: criterion 1, D-04, D-05, D-06, D-08. Sources: E-17, the XML and console log of the run, the capture loop (`docker stats` and `docker ps` every 60 s, worker log and status every 15 s).

#### Run

```sh
rm -rf build/test-results/integrationTest build/reports/tests/integrationTest
git rev-parse HEAD
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE JAVA_HOME=$HOME/.gradle/jdks/adoptium-17-aarch64-hotspot-mac/Contents/Home ./gradlew --console=plain integrationTest
```

```text
run commit: c4b409db87d36d7599e472d38f3a4bc5a118c646 (contains the harness commit 781fd526; src, build.gradle.kts, settings.gradle.kts and gradle are unchanged since it)
start (UTC): 2026-10-26T15:40:47Z   end (UTC): 2026-10-26T16:10:15Z   T = 1768 s (29.5 min)
Gradle: BUILD SUCCESSFUL in 29m 26s; exit status 0
Test result: SUCCESS
Tests summary: 55 tests, 54 succeeded, 0 failed, 1 skipped
```

The whole task ran with no test filter, no `-D` ClickHouse property and no cluster-name variable. No hang, no OpenJ9 or JVM crash (HotSpot Temurin 17.0.9 through `JAVA_HOME`, E-02); no `core.*` file. The Docker images of the run were already local (E-03, E-17), so only the worker image build and container starts were paid at run time.

#### Server

server-version: 26.10.3.7

Image `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`, pulled just before the run ("Image is up to date"): the same digest as E-01 and E-17, so no `latest` drift between 31-01 and this run. `SELECT version()` was run with `clickhouse local` from that image. Unlike E-05, the server was reached by passing tests: 54 connector tests inserted into it and read back.

#### Test classes

| Class | Status | tests | failures | errors | skipped | time (s) |
|-------|--------|-------|----------|--------|---------|----------|
| ClickHouseSinkConnectorIntegrationTest | ran | 54 | 0 | 0 | 0 | 1759.792 |
| ExactlyOnceTest | skipped | 8 | 0 | 0 | 8 | 0.022 |
| ClickHouseCloudTest | skipped | 1 | 0 | 0 | 1 | 0.024 |

`ClickHouseSinkConnectorIntegrationTest` is 25 `avroSchemaTest` plus 22 `protoSchemaTest` invocations plus 7 `stockGen*` methods, all passed. `ClickHouseCloudTest` and `ExactlyOnceTest` abort in `@BeforeAll` through assumptions (no cloud properties, no cluster) and are reported as skipped in the XML, not failed; the console summary counts only `ClickHouseCloudTest` (55 = 54 + 1) while the XML lists all 63 testcases (D-04, as measured in E-02 and E-03).

#### Failures and proposed causes

skip-candidates: 0

No testcase failed or errored.

#### Timeout (D-05)

T = 1768 s (`runh.end` minus `runh.start`). From the XML of `ClickHouseSinkConnectorIntegrationTest`: suite time 1759.792 s, sum of its testcase times 1665.1 s, so U = 94.7 s of class setup (image build of the worker, container starts, ClickHouse and Toxiproxy start). E-03's fully cold class setup was 458 s, so P = max(0, 458 - 94.7) = 363.3 s. W = ceil((1768 + 363.3) / 60) = 36 min. S = ((2 x 36 + 4) div 5) x 5 = 75, J = S + 10 = 85. The slowest testcases are the two interrupt tests (`stockGenMultiTaskInterruptTest` 211.1 s, `stockGenSingleTaskInterruptTest` 160.2 s); every other testcase is under 40 s.

nightly-timeouts: step=75 job=85 (cold-equivalent wall 36 min x 2, rounded up to a multiple of 5; job = step + 10)

CI references (research V8, V9): about 20 minutes for a healthy standalone `latest` x V2 job on a hosted runner, 30 to 52 minutes for red runs, 29.6 minutes for the fork's own red run 38881153720. This local green run took 29.5 minutes warm on 8 CPUs, so a cold hosted runner with 4 vCPU and ten image pulls can be expected to take longer than 20 minutes. The line above supersedes E-05's `step=30 job=40` because plan 31-04 reads the last such line; the user may amend it at Task 3 (the derivation is PT-02 applied to a cold-equivalent wall, so it is conservative: it prices in a cold pull of about 6 minutes of setup that this run did not pay).

#### Images (D-06)

Tag, image id (equal to the repo digest in this containerd image store), repo digest, architecture, created, after the run:

```text
confluentinc/cp-zookeeper:7.9.6                  sha256:d3294ad2b353ac5a155ad9abe37884f75c482f387af553679210303b7118a0d3  arm64  2026-03-16
confluentinc/cp-server:7.9.6                     sha256:7021128926d83f34edb8be6196f202bf512e5c1ab998b8fbb163a53122ecebae  arm64  2026-03-16
confluentinc/cp-schema-registry:7.9.6            sha256:1efa1ed67e6937e4c82ae34d761eee7354e6b79df5d459ff387735af0a2849d1  arm64  2026-03-16
confluentinc/cp-enterprise-control-center:7.9.6  sha256:024e3b9ed086ce445d32ced8c062983b364d133d59a7685d5182850ca9098a46  arm64  2026-03-16
confluentinc/cp-ksqldb-server:7.9.6              sha256:8d9573347642c9405b7c10566e29bd83e0b866f13029b343bc59337903f9f14d  arm64  2026-03-16
confluentinc/cp-kafka-rest:7.9.6                 sha256:b1dbbf701f7a60c3597cc0419f99faf6a8c5239cd541ab593ec00be553117ae0  arm64  2026-03-16
confluentinc/cp-server-connect:7.9.6             sha256:b0a23182108658bbe097d05b416f4c591574abcec1b5d4409b6852c09d5949ce  arm64  2026-03-16   (new: the worker base, E-17)
cnfldemos/cp-server-connect-datagen:0.6.4-7.6.0  sha256:0ed7bc9785bcd81595460e1908795e2e0772582a4f79359ad5daaab1001012d3  arm64  2024-03-05   (now the datagen plugin source only)
clickhouse/clickhouse-server:latest              sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce  arm64  2026-10-26   (floats)
ghcr.io/shopify/toxiproxy:2.7.0                  sha256:fc2d40f4904c9ab66ba7c49dca969b8116a71d441973d6f124a72d5c172d3b75  arm64  2023-10-25
testcontainers/ryuk:0.14.0                       sha256:7c1a8a9a47c780ed0f983770a662f80deb115d95cce3e2daa3d12115b8cd28f0  arm64  2025-09-26
generated worker image: localhost/testcontainers/ua9mfoswj5c4b4dt:latest (built by Testcontainers from the two Confluent images; removed when the test JVM exited, so its id cannot be read afterwards; it adds the 9 MB datagen plugin directory on top of cp-server-connect)
```

Only `clickhouse/clickhouse-server:latest` floats; its digest equals E-01's, so no drift occurred between the measurements. Drift is accepted (D-06); this run pins nothing.

#### What the fix changes for upstream's red

With a Java 17 worker, no failure remains, so nothing shows a cause behind the class-file level. In particular the API-level part of research hypothesis 2 (the fork compiles against `connect-api` 4.4.0 while the worker runs `connect-api-7.9.6-ce.jar`) produced no failure in this run: all 54 connector tests, including the Avro, Protobuf and interrupt tests, passed, and the captured worker log tail holds no `NoSuchMethodError`, `AbstractMethodError` or `UnsupportedClassVersionError` (the captured file is the last 15-second snapshot of a `json-file` container log: 534111 lines, starting at 16:08:12, so the earlier part of the run is not in it; the passing tests are the evidence for the rest). The tail does hold 47 `ERROR` lines: datagen source tasks ending with "Stopping connector: generated the configured 100 number of messages" (the connector's normal end after its iterations) and `RetriableException from SinkTask` / "Error inserting records" from the interrupt tests, which cut the ClickHouse proxy on purpose and passed. E-05's verdict for run 38881153720 stands: its symptom (all connector-dependent tests failing at `ConfluentPlatform.java:360`) is explained by the Java 11 worker, and is gone locally once the worker is Java 17. That does not prove the CI run goes green; the CI-only risks of E-05 and below remain untested.

#### Differences from the runner and CI risks

E-05's "Differences from the runner" and "CI-only risks for the first dispatch" lists apply unchanged, except that the last item of the risks list (the Java 11 worker) is resolved by E-17. What changed:

- One image build per run of the class: Testcontainers builds the worker image at `@BeforeAll` from `cp-server-connect:7.9.6` and the datagen image (class setup U = 94.7 s with every image local, including this build).
- One more image pull on a cold runner: `confluentinc/cp-server-connect:7.9.6` (2.71 GB uncompressed), while `cnfldemos/cp-server-connect-datagen:0.6.4-7.6.0` (2.04 GB) is still pulled as the plugin source. Image sizes as reported by Docker for the ten images: zookeeper 0.53 GB, cp-server 1.86, schema-registry 2.04, control-center 1.09, ksqldb 1.40, kafka-rest 1.86, cp-server-connect 2.71, datagen 2.04, clickhouse 0.28, toxiproxy 0.01; sum 13.83 GB (E-04 measured about 11.5 GB of growth for the nine images before this change). Against the runner's 14 GB SSD this is the largest CI risk of the change: the capacity probe of plan 31-04 and the failed-test summary diagnose it in the first dispatch. `docker system df` of the run (all images already local) read Images 28.22 GB before and 28.24 GB after, Containers 8 to 9 (not left by the run: the other containers belong to an unrelated local Superset stack), Local Volumes 47 in both.
- Memory: peak concurrent memory of the run's containers 5121 MiB with 10 containers (the largest single container 1876 MiB), against 16 GB on the runner; the unrelated local containers are excluded.
- Wall time: 29.5 minutes for a green run against E-03's 13.4 minutes for a run that failed fast.

run-commit: c4b409db87d36d7599e472d38f3a4bc5a118c646

Matches text: n/a (gate check)

### E-06b: the user's disposition of the re-measurement (D-05, D-08, D-09)

Backs: plan 31-06 Task 3 (blocking-human decision); read by plan 31-02's tracer.

user_answer_received_by: orchestrator (direct)

user_answer: accept (Recommended)

disposition: accept

```text
skip-entries: 0
```

carried-to-phase-32: FORK-DELTA row for the harness's Java 17 Connect worker in ConfluentPlatform.java (harness commit 781fd52683d6adb7f49468ed6eb88374053a9c1c) (CIG-02)

The E-18 `nightly-timeouts: step=75 job=85` line stands unamended.

Matches text: n/a (gate check)

### E-07: no skip list is needed (D-07)

Backs: plan 31-02 Task 1 (the D-07 branch rule), Tasks 2 and 3 (skipped).

The authoritative disposition is the last evidence entry whose heading starts `### E-06` and that holds a numeric `skip-entries:` line. That is `### E-06b: the user's disposition of the re-measurement (D-05, D-08, D-09)` (`user_answer: accept (Recommended)`, `user_answer_received_by: orchestrator (direct)`). The earlier `### E-06` entry holds `skip-entries: pending`, which is not numeric, so it was superseded by E-06b. E-06b holds:

```text
skip-entries: 0
```

The E-18 re-measurement behind it is green: 55 tests, 54 succeeded, 0 failed, 1 skipped, on `latest` x V2 with the Java 17 Connect worker (`skip-candidates: 0`). No failure remains, so there is nothing to classify.

Consequences, per the D-07 branch rule ("if any skip list is needed"):

- No file outside this evidence changes in plan 31-02: `gradle/integration-test-skip-list.tsv` is not created, `build.gradle.kts` gets no reader, `.planning/scripts/fork-anchors.txt` gets no row (anchors mirror: rows=126 bad=0, saved to `/tmp/gsd31/exec/anchors-before-31-02.txt`), `.planning/REQUIREMENTS.md` gets no Future Requirements bullet, and `src/main` is identical to `v1.7.0_gg4`.
- Task 2 (the Docker-free guard `IntegrationSkipListGuardUnitTest`) is skipped: there is no list or reader to guard. The Task 2 verify prints `SKIP-GUARD-NOT-NEEDED`.
- Task 3 (the phase-gate live run with the list) is skipped: the phase-gate `integrationTest` run is 31-06's green re-measurement, E-18 (gate commit `c4b409db87d36d7599e472d38f3a4bc5a118c646`, `server-version: 26.10.3.7`, same image digest as E-01). The 31-01 run in E-03 was red, so E-18 is the green one. No E-08 or E-09 is written.
- Probe A3 (an include filter naming a method that `excludeTest` removes) is not exercised: there is no excluded method to probe.

Matches text: n/a (gate check)

### E-10: the byte guard red on a one-byte edit of the real file and green after the restore (NIT-02)

Backs: plan 31-03 Task 1; ROADMAP criterion 3 (a Docker-free guard fails on a single-byte difference from upstream's copy).

```sh
F=.github/workflows/integrationTests.yaml
git rev-parse HEAD:$F; git rev-parse v1.7.0:$F; git rev-parse a6f1df83:$F; git rev-parse refs/remotes/upstream/main:$F
git hash-object $F; shasum -a 256 $F; wc -c < $F
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*UpstreamIntegrationWorkflowBytesGuardUnitTest' --rerun   # unchanged file
python3 -I (first byte n -> N, in place)                                                                                                   # name: -> Name:
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*UpstreamIntegrationWorkflowBytesGuardUnitTest' --rerun   # red
git checkout -- $F; git hash-object $F
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*UpstreamIntegrationWorkflowBytesGuardUnitTest' --rerun   # green
```

```text
git blob of .github/workflows/integrationTests.yaml:
HEAD                    3e9b9be050fa05805227b2eee0d1fe8c72df56c0
v1.7.0                  3e9b9be050fa05805227b2eee0d1fe8c72df56c0
a6f1df83                3e9b9be050fa05805227b2eee0d1fe8c72df56c0
refs/remotes/upstream/main  3e9b9be050fa05805227b2eee0d1fe8c72df56c0 (ref exists)
working file: git hash-object 3e9b9be050fa05805227b2eee0d1fe8c72df56c0
              sha256 07dff3d0973ae821a259544632d161a270126ff8054b45adbf64849edd2d5109
              wc -c  4812 (ASCII, LF, no CR, ends with a newline, first line "name: Integration Tests")

run 0, unchanged file, before the commit (bytes-pre.log):
exit status 0; Tests summary: 9 tests, 9 succeeded, 0 failed, 0 skipped

run 1, first byte changed (name: -> Name:; hash-object a96fb8e5716481cf571d31fd5b880aca5878e944; bytes-red.log, bytes-red.rc, bytes-red.xml):
exit status 1; XML tests=9 failures=1 errors=0 skipped=0; Tests summary: 9 tests, 8 succeeded, 1 failed, 0 skipped
failing test: integrationTests.yaml is byte-identical to upstream at v1.7.0 and a6f1df83 (NIT-02)
message: integrationTests.yaml must stay byte-identical to upstream: expected 4812 bytes / 07dff3d0973ae821a259544632d161a270126ff8054b45adbf64849edd2d5109, found 4812 bytes / 346e55ca9b8292571d673357d1358c3eb69a30c744e008e1092dcda169709e4c; if an upstream sync changed this file on purpose, confirm the new bytes equal the upstream tag's copy (git rev-parse TAG:.github/workflows/integrationTests.yaml, with the synced upstream tag for TAG) and replace EXPECTED_SHA256 and EXPECTED_LENGTH in the same commit

restore: git checkout -- .github/workflows/integrationTests.yaml
git hash-object after the restore: 3e9b9be050fa05805227b2eee0d1fe8c72df56c0; git status --porcelain of the file: empty

run 2, restored file (bytes-green.log, bytes-green.rc, bytes-green.xml):
exit status 0; XML tests=9 failures=0 errors=0 skipped=0; Tests summary: 9 tests, 9 succeeded, 0 failed, 0 skipped

the seven fixtures, each rejected by the byte rule (all passed in run 2):
fixture: the byte rule rejects the first byte flipped (NIT-02)
fixture: the byte rule rejects a middle byte flipped (NIT-02)
fixture: the byte rule rejects the last byte flipped (NIT-02)
fixture: the byte rule rejects one byte appended (NIT-02)
fixture: the byte rule rejects the final newline removed (NIT-02)
fixture: the byte rule rejects LF line endings replaced by CRLF (NIT-02)
fixture: the byte rule rejects an empty file (NIT-02)
```

The one-byte edit kept the length at 4812 (an in-place byte change), so the message differs from the expected values in the hash only; the length-changing edits are covered by the appended-byte, removed-newline, CRLF and empty fixtures. The edit lived only in the working tree between run 1 and the restore; the guard commit is `3d0e3d6f8ce6802288c71efcbcf199650061d9f3` and touches only the guard class. The runs used the default Gradle JVM (Homebrew JDK 21 daemon, no `JAVA_HOME`): `testUnit` is Docker-free and its test JVM started normally, so the E-02 `JAVA_HOME` override was not needed.

Matches text: n/a (gate check)

### E-11: integrationTests.yaml disabled on GitHub with no run after 2026-10-16, and the suite gates (NIT-02)

Backs: plan 31-03 Task 2; ROADMAP criterion 3 (the Actions API reports the workflow disabled with no run since 2026-10-16). Phase 32 repeats these reads for its criterion 4.

```sh
curl -sS https://api.github.com/rate_limit
curl -sS https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/integrationTests.yaml
curl -sS 'https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/integrationTests.yaml/runs?per_page=100'
curl -sS 'https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/integrationTests.yaml/runs?created=%3E2026-10-16T10%3A00%3A00Z&per_page=100'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun
git worktree add --detach /tmp/gsd31/wt-fmt HEAD   # then, inside it: ./gradlew -q spotlessApply; cmp the guard with the main tree's; git worktree remove --force
git diff --quiet v1.7.0_gg4 HEAD -- src/main; echo $?
```

```text
rate limit before the reads (unauthenticated, core): limit 60, remaining 60; after the three reads: remaining 57
bodies saved under /tmp/gsd31/exec/api/: rate_limit.json, workflow.json, runs.json, runs-after.json

workflow.json:
  id          384641581
  path        .github/workflows/integrationTests.yaml
  state       disabled_manually
  updated_at  2026-10-16T09:38:23.000Z

runs.json: total_count 2, workflow_runs length 2
  id 38881153720  event schedule  created_at 2026-10-16T09:32:28Z  conclusion failure
  id 38714528739  event schedule  created_at 2026-10-15T09:39:18Z  conclusion failure

runs-after.json (created > 2026-10-16T10:00:00Z): total_count 0, workflow_runs []

full testUnit (testunit-31-03.log): exit status 0
Test result: SUCCESS
Tests summary: 1308 tests, 1306 succeeded, 0 failed, 2 skipped
byte guard XML (TEST-...UpstreamIntegrationWorkflowBytesGuardUnitTest.xml): tests=9 failures=0 errors=0 skipped=0
no core.*, javacore.*, Snap.* or jitdump.* file; no OpenJ9 crash (default Gradle JVM, no JAVA_HOME)

Spotless: spotlessApply in a detached worktree at HEAD (fmt-31-03.log) exit status 0; cmp of the worktree's guard class with the main tree's: identical (cmp exit 0).
spotlessApply rewrote 48 other tracked files in that worktree (the inherited violations; gradlew.bat included, because the worktree has its own checkout of it); none is the guard class and none is changed in the main tree.

git diff --quiet v1.7.0_gg4 HEAD -- src/main: exit status 0
git hash-object .github/workflows/integrationTests.yaml: 3e9b9be050fa05805227b2eee0d1fe8c72df56c0
```

The disabled state and the absence of a run after the cut-off are read from GitHub, not from the repository: the byte guard (E-10) pins the file's content offline, and this entry is the online half of NIT-02. 31-05 and Phase 32 re-read it.

Matches text: n/a (gate check)

### E-12: the nightly red on the missing file, then written and green, the script smoke and the anchors (NIT-01, D-01, D-05, D-10)

Backs: plan 31-04 Task 1; ROADMAP criterion 2 (the nightly passes the CIG-01 guard with no guard edit; ubuntu-24.04, a timeout, read-only permissions, no persisted credentials, reports uploaded on failure, no cloud secret, variable or job, triggers dispatch plus a gated schedule).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*NightlyWorkflowGuardUnitTest' --rerun   # red: the workflow does not exist yet
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*NightlyWorkflowGuardUnitTest' --tests '*WorkflowActionPinGuardUnitTest' --tests '*TestsWorkflowPinGuardUnitTest' --rerun   # green
ruby -ryaml -e 'd = YAML.load_file(ARGV[0]); raise "not a mapping" unless d.is_a?(Hash); puts "yaml-ok"' .github/workflows/nightly-integration-tests.yaml
python3 -I /tmp/gsd31-scripts/shape-31-04.py <evidence file>   # the plan's shape check, run from a scratch file
python3 -I /tmp/gsd31-scripts/smoke-31-04.py                   # the plan's script smoke, run from a scratch file
# anchors mirror: grep -cF -e <pattern> <path> against every row of .planning/scripts/fork-anchors.txt
```

```text
red run (nightly-red.log, nightly-red.rc, nightly-red.xml): exit status 1
Tests summary: 3 tests, 0 succeeded, 3 failed, 0 skipped
XML tests=3 failures=3 errors=0 skipped=0
failure message of each: "The nightly workflow does not exist: .github/workflows/nightly-integration-tests.yaml"
failing tests:
  the nightly workflow exists and passes the shared tests.yaml rules: ubuntu-24.04 runner, read-only token, checkout without persisted credentials, ./gradlew integrationTest in command position (NIT-01)
  the nightly job is gated: a manual dispatch always runs and the schedule runs only when NIGHTLY_ENABLED is 'true' (D-01)
  the failed-test summary step runs the stdlib script on the integrationTest results, and the script exists (D-10)

green run after the workflow and the script were written (nightly-green.log): exit status 0
Tests summary: 63 tests, 63 succeeded, 0 failed, 0 skipped
per class XML: NightlyWorkflowGuardUnitTest 3/0/0/0, WorkflowActionPinGuardUnitTest 21/0/0/0, TestsWorkflowPinGuardUnitTest 39/0/0/0 (tests/failures/errors/skipped)

timeouts read from the last "nightly-timeouts: step=" line of this file (E-18): step 75, job 85; E-06b: "The E-18 nightly-timeouts line stands unamended"
YAML parse: yaml-ok (top-level keys name, permissions, on (parsed as boolean true by YAML 1.1, as for every workflow), concurrency, jobs)
shape check: "timeouts 75 85 missing [] upload-pins 2 forbidden []" then NIGHTLY-SHAPE-OK
script smoke (one passing and one failing testcase in a fixture report, GITHUB_STEP_SUMMARY set): rc 0, annotations ['::error title=Integration tests failed::1 failed: com.example.SmokeTest#fails()']; the summary file held the heading, "tests=2 failures=1 errors=0 skipped=0" and the table row "| com.example.SmokeTest#fails() | failure | expected 1 but was 2 |"; SUMMARY-SMOKE-OK; the script is executable
anchors mirror before the edit: rows=126 bad=0 (saved to /tmp/gsd31/exec/anchors-before-31-04.txt; 123 at planning time, +3 from plan 31-06, plan 31-02 added none)
anchors mirror after the five rows: rows=131 bad=0
```

The five rows pin, for `.github/workflows/nightly-integration-tests.yaml`: the D-01 expression (count 1), `./gradlew integrationTest` (1), `persist-credentials: false` (1), `secrets.` (0) and `--tests` (0). No comment in the workflow repeats an anchored literal; the header describes them by concept. The workflow was committed with its guard, the script and the rows in one green commit. `WorkflowActionPinGuardUnitTest`, `.github/dependabot.yml` and the other workflows were not edited. The capacity step reads `free -m`, `df -h /` and `docker version --format` of the server and tolerates a failing probe (`|| echo unavailable`) so that a probe failure cannot stop the run before the tests.

Matches text: n/a (gate check)

### E-13: the nightly guard rules and their 44 rejected fixtures (NIT-01)

Backs: plan 31-04 Task 2; ROADMAP criterion 2 (a Docker-free guard keeps the nightly's gate, triggers, timeouts, Gradle step, cloud-free references and always-run reporting from regressing).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*NightlyWorkflowGuardUnitTest' --tests '*TestsWorkflowPinGuardUnitTest' --tests '*WorkflowActionPinGuardUnitTest' --rerun
python3 -I /tmp/gsd31-scripts/guard-31-04.py     # the plan's XML count check, run from a scratch file
git diff HEAD~1 HEAD -- src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
# real-file mutation: five edits of the committed workflow (python3 -I scratch script), testUnit on the nightly guard, then git checkout -- <file>
```

```text
exit status 0; Tests summary: 118 tests, 118 succeeded, 0 failed, 0 skipped
per class XML (tests/failures/errors/skipped): NightlyWorkflowGuardUnitTest 58/0/0/0, TestsWorkflowPinGuardUnitTest 39/0/0/0, WorkflowActionPinGuardUnitTest 21/0/0/0
nightly guard: 8 real-file tests + 6 accept fixtures + 44 reject fixtures = 58
rejects per rule: gate 7, trigger 10, timeout 6, Gradle step 9, cloud reference 5, always-run step 7; accepts 6

reject fixtures, as named in the XML (each is one edit of the valid nightly; every one passed, i.e. its rule returned a problem):
gate (D-01):
  a job without the gate | the gate inside an expression block | and instead of or | a negated variable check | a second job | the gate on a step only | two gate lines
trigger (NIT-01, D-01):
  a push trigger | a pull_request trigger | a pull_request_target trigger | a workflow_run trigger | a repository_dispatch trigger | a workflow_call trigger | a flow list of triggers | a dispatch input | no schedule | no workflow_dispatch
timeout (D-05):
  no job timeout | no Gradle-step timeout | a step timeout equal to the job's | a step timeout above the job's | a timeout that is not a number | a job timeout above 360
Gradle step (D-04, NIT-01):
  no ClickHouse version | the cloud ClickHouse version | no client version | another client version | a test filter | a ClickHouse system property | a cluster name | error tolerance on the Gradle step | a condition on the Gradle step
cloud reference (NIT-01):
  a secret in the step env | another variable in a step condition | an expression block in a run line | a cloud host property | a job name mentioning ClickHouse Cloud
always-run step (D-10):
  a summary step without always() | a results upload without always() | a report upload without always() | an upload only on failure | the wrong results path | the wrong report path | a summary of another directory

two-word diff of TestsWorkflowPinGuardUnitTest.java (git diff HEAD~1 HEAD, commit feb97802): 1 file changed, 2 insertions(+), 2 deletions(-)
-    private static final Pattern CONTINUE_ON_ERROR =
+    static final Pattern CONTINUE_ON_ERROR =
-    private static final Pattern TRAILING_COMMENT = Pattern.compile("\\s#.*$");
+    static final Pattern TRAILING_COMMENT = Pattern.compile("\\s#.*$");

real-file mutation (the committed workflow edited five ways in the working tree: a push trigger added, the job timeout removed, CLIENT_VERSION V1, the job name ending in ClickHouse Cloud, the summary step pointed at build/test-results/test): the nightly guard ran 29 cases, 13 succeeded, 16 failed, exit status 1; the six real-file tests R3 to R8 (summary step, trigger, timeout, Gradle step, cloud reference and always-run) failed while R1 and R2 stayed green (those two rules were not touched). The rule fixtures derive from the real file, so with some anchors gone five of the six reject sources (gate, timeout, Gradle step, cloud reference, always-run) also failed at initialisation, as did the accept fixtures of the rules whose lines changed, which is the loud direction. `git checkout -- .github/workflows/nightly-integration-tests.yaml` restored it: git hash-object 0e0bcd6849450ee06f8f5ec6190dbcba17148685 equals HEAD's blob, and git status of the file was empty. The 118-case run above came after the restore.
```

Matches text: n/a (gate check)

### E-14: the failed-test summary script on fixture reports and the suite gates (D-10)

Backs: plan 31-04 Task 3; ROADMAP criterion 2 (failed test names reach the step summary and one safe annotation, because job logs are not readable without admin rights).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*FailedTestsSummaryScriptUnitTest' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun
git worktree add --detach /tmp/gsd31/wt-fmt HEAD   # then, inside it: ./gradlew -q spotlessApply; cmp the three Java files with the main tree's; git worktree remove --force
git diff --quiet v1.7.0_gg4 HEAD -- src/main; echo $?
# script mutation: ANNOTATION_CAP raised to 100000 and the LF escape removed, testUnit on the script test, then git checkout -- <script>
```

```text
FailedTestsSummaryScriptUnitTest: exit status 0; XML tests=8 failures=0 errors=0 skipped=0 (python3 3.14.8, run as python3 -I from the repository root)
the 8 cases:
  a failure and an error across two report files are listed by class#method in the summary and in one error annotation (D-10)
  a run without failures writes a notice and 'No failed tests.', also without GITHUB_STEP_SUMMARY (D-10)
  a missing results directory exits 0 with a warning annotation and a summary line (D-10)
  a malformed report file is named in a warning and the other files are still read (D-10)
  percent, CR, LF, pipe and backtick in a test name are escaped for the annotation and the Markdown table (D-10)
  more failing names than the cap keep the annotation to one capped line, and the summary lists every name (D-10)
  no argument exits 2 with a usage line on stderr
  skipped tests are counted, never listed as failed (D-04)
script fix: none. All 8 cases passed against the script as committed in the first commit of this plan.

script mutation (ANNOTATION_CAP = 100000, and .replace("\n", "%0A") removed from escape_command): exit status 1, Tests summary: 8 tests, 6 succeeded, 2 failed, 0 skipped; the two failures are the escaping case and the cap case (Pitfall 11). git checkout -- .github/scripts/failed_tests_summary.py restored it: git hash-object c1624052c82ba7ebb57f44d60694b80f3e52e626 equals HEAD's blob, mode 755, git status of the file empty.

full testUnit (testunit-31-04.log, after the restore): exit status 0
Test result: SUCCESS
Tests summary: 1374 tests, 1372 succeeded, 0 failed, 2 skipped
(1308 tests at the end of plan 31-03, plus 58 nightly guard cases and 8 script cases)
no core.*, javacore.*, Snap.* or jitdump.* file; no OpenJ9 crash (default Gradle JVM, no JAVA_HOME)

per class XML (tests/failures/errors/skipped):
  NightlyWorkflowGuardUnitTest 58/0/0/0
  FailedTestsSummaryScriptUnitTest 8/0/0/0
  WorkflowActionPinGuardUnitTest 21/0/0/0
  TestsWorkflowPinGuardUnitTest 39/0/0/0
  UpstreamIntegrationWorkflowBytesGuardUnitTest 9/0/0/0

Spotless: spotlessApply in a detached worktree at HEAD (fmt-31-04.log) exit status 0; cmp of the worktree's NightlyWorkflowGuardUnitTest.java, FailedTestsSummaryScriptUnitTest.java and TestsWorkflowPinGuardUnitTest.java with the main tree's: identical (cmp exit 0 for each). spotlessApply rewrote 48 other tracked files in that worktree (the inherited violations, gradlew.bat included); none is under src/test/java/.../ci and none is changed in the main tree. The worktree was removed.

git diff --quiet v1.7.0_gg4 HEAD -- src/main: exit status 0
git hash-object .github/workflows/integrationTests.yaml: 3e9b9be050fa05805227b2eee0d1fe8c72df56c0 (unchanged, equals E-10)
git diff --stat e3ab7a8c HEAD -- .github/dependabot.yml .github/workflows: one file changed, nightly-integration-tests.yaml, 79 insertions (no other workflow, no dependabot.yml edit)
```

Matches text: n/a (gate check)

### E-15: the Phase 31 gate, both workflows on GitHub, and the proposed NIT-03 move (D-02)

Backs: the ROADMAP Phase 31 phase gate (cache-busted `testUnit`, the anchors invariant, an empty `src/main` diff, one local `integrationTest` run on `latest` x V2); criterion 3 (the Actions API still reports `integrationTests.yaml` disabled); D-01, D-02, D-03 (the nightly is not on `custom_build`, so it has no run, scheduled or dispatched).

```sh
git rev-parse HEAD                                                                 # the gated HEAD
bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD                    # background
bash .planning/scripts/verify-merge-invariants-selftest.sh                         # background, after the gate
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun
git diff --quiet v1.7.0_gg4 HEAD -- src/main; echo $?
git hash-object .github/workflows/integrationTests.yaml
# anchors mirror: grep -cF -e <pattern> <path> against every row of .planning/scripts/fork-anchors.txt
git diff --quiet c4b409db87d36d7599e472d38f3a4bc5a118c646 HEAD -- src/main src/integrationTest src/testFixtures build.gradle.kts settings.gradle.kts gradle; echo $?
curl -sS https://api.github.com/rate_limit
curl -sS https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/integrationTests.yaml
curl -sS 'https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/integrationTests.yaml/runs?created=%3E2026-10-16T10%3A00%3A00Z&per_page=100'
curl -s -o /dev/null -w '%{http_code}' 'https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/contents/.github/workflows/nightly-integration-tests.yaml?ref=custom_build'
curl -s -o /dev/null -w '%{http_code}' https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/nightly-integration-tests.yaml
git ls-remote origin custom_build
```

#### Phase gate

```text
gated HEAD: f0f213bf5a11dcc43195e3993dda8bd896d185eb (branch sync/v1.7.0; nothing was committed between the start of the gate and its end)
gate start (UTC) 2026-10-26T17:36:45Z; selftest end 2026-10-26T18:11:25Z

cache-busted testUnit (testunit-31-05.log, started at the same time as the gate): exit status 0
Test result: SUCCESS
Tests summary: 1374 tests, 1372 succeeded, 0 failed, 2 skipped
ci guards, XML tests/failures/errors/skipped:
  NightlyWorkflowGuardUnitTest                   58/0/0/0
  FailedTestsSummaryScriptUnitTest                8/0/0/0
  WorkflowActionPinGuardUnitTest                 21/0/0/0
  TestsWorkflowPinGuardUnitTest                  39/0/0/0
  UpstreamIntegrationWorkflowBytesGuardUnitTest   9/0/0/0
no gradle/integration-test-skip-list.tsv (no skip list, E-07), so there is no IntegrationSkipListGuardUnitTest
no core.*, javacore.*, Snap.* or jitdump.* file; no OpenJ9 crash (default Gradle JVM, no JAVA_HOME)

anchors mirror: rows=131 bad=0 (126 after plan 31-06, plan 31-02 added none, plan 31-04 added five)

cumulative gate, last line (gate-31.log, exit status 0):
invariants: provenance_paths=21 anchor_commits=893 anchor_rows=131 marker_commits=952 anchor_exempt=19 result=PASS

self-test, last line (selftest-31.log, exit status 0, 16 PASS lines):
selftest: no failures

git diff --quiet v1.7.0_gg4 HEAD -- src/main: exit status 0
git hash-object .github/workflows/integrationTests.yaml: 3e9b9be050fa05805227b2eee0d1fe8c72df56c0
gate scripts and the exemptions file: unchanged since the planning base e3ab7a8c (git diff --quiet exit 0)

The phase gate's live run is the green re-measurement of plan 31-06, E-18 (55 tests, 54 succeeded, 0 failed, 1 skipped; 63 testcases in the XML; ClickHouse latest 26.10.3.7; client V2; BUILD SUCCESSFUL in 29m 26s). E-03 was red (54 failed on the Java 11 worker) and plan 31-02 built no list, so there is no E-09:
gate-run: E-18 c4b409db87d36d7599e472d38f3a4bc5a118c646
tree check: git diff --quiet c4b409db87d36d7599e472d38f3a4bc5a118c646 HEAD -- src/main src/integrationTest src/testFixtures build.gradle.kts settings.gradle.kts gradle: exit status 0 (the commit is an ancestor of HEAD; the harness commit 781fd526 is inside it)
```

#### Workflows on GitHub

```text
rate limit (unauthenticated, core): limit 60, remaining 56 before the reads, 52 after; bodies under /tmp/gsd31/exec/api31/

integrationTests.yaml (workflow.json):
  id          384641581
  path        .github/workflows/integrationTests.yaml
  state       disabled_manually
  updated_at  2026-10-16T09:38:23.000Z
runs created after 2026-10-16T10:00:00Z (runs-after.json): total_count 0

nightly-integration-tests.yaml:
  contents API on ref custom_build (nightly-contents.code): 404
  Actions workflow API (nightly-workflow.code): 404
  -> the nightly is not on the default branch, so GitHub has no workflow, no scheduled run and no dispatchable file for it

git ls-remote origin custom_build (origin-custom_build.txt): 21c51598b06b6f5af58430765d57b1e5f6747c5a refs/heads/custom_build
  = tag v1.7.0_gg4 = the planning-time value; no Dependabot merge landed on origin, so Phase 32 has nothing to absorb first from this read
```

NIT-02 is intact: the workflow is still `disabled_manually` and has had no run since the cut-off. NIT-01's schedule cannot have fired: the file does not exist on origin.

#### Carried to Phase 32

CI-only risks for the first dispatch (E-05's checklist, with the status after E-17 and E-18):

- [ ] Disk: about 13.8 GB of image layers (E-18: ten images, 13.83 GB, with `cp-server-connect:7.9.6` added) on a 14 GB runner SSD. This is the largest CI risk. Diagnosed by the capacity probe step (`df -h /`) and the Docker server line in the step summary.
- [ ] Memory: ten containers peaked at 5.1 GiB here plus the test JVM; the runner has 16 GB. Diagnosed by the capacity probe (`free -m`) and the uploaded XML.
- [ ] x86 against arm64: the images are multi-arch (the worker base `confluentinc/cp-server-connect:7.9.6` lists amd64 and arm64, E-17), but nothing was run on x86. Diagnosed by the failed-test summary and the uploaded XML.
- [ ] Anonymous Docker Hub pull limits on shared runner addresses (eleven pulls per run now). Diagnosed by the Gradle step log in the uploaded report and a pull error in the XML.
- [ ] `latest` drift: a new ClickHouse build between the measurement and the dispatch (E-18 used digest `739e099a...`, 26.10.3.7). Diagnosed by the digest in the capacity step and the server version in the XML output.
- [ ] Step timeout: step 75 and job 85 minutes (E-18, E-06b) against a green local wall of 29.5 minutes warm; a slow cold runner may still approach it. Diagnosed by a cancelled Gradle step with the `always()` upload steps still present.
- [x] The Java 11 worker of E-05 is resolved by E-17 and E-18 (Java 17.0.18 worker, 54 of 54 connector tests green locally). Not a CI risk any more; carried only as a FORK-DELTA row (E-06b: the harness's Java 17 Connect worker in `ConfluentPlatform.java`, harness commit 781fd52683d6adb7f49468ed6eb88374053a9c1c, CIG-02).

Ordering question (research open question 2): the dispatch can follow only the `--no-ff` merge, because GitHub dispatches a workflow only from the default branch. Whether Phase 32 dispatches before or after pushing the tags is left to Phase 32's planning unless the user states it at the 31-05 checkpoint. A red dispatch would need a skip list, a build change or a `.github/` change, which Phase 32 may not make (the Phase 29 D-05 standing rule), so it returns to Phase 31 as a gap and gives the release tip a new SHA to re-gate.

Open question 4: the five anchor rows landed here (plan 31-04); the FORK-DELTA prose for the nightly and the two guards (CIG-01, NIT-02) is written once, in Phase 32 (CIG-02). This is the default.

#### Proposed NIT-03 move (D-02)

`{date}` stands for the decision's UTC date. Each edit is one scoped `Edit`; nothing else in either file changes. The Phase 30 section, the Phase 31 Plans list, and the text from `## Sync Mechanics` to `## Progress` stay byte-identical to the planning base `e3ab7a8c7e32948f51ef067ced6ccce2f5af7d8b`.

**REQUIREMENTS.md** (four edits):

1. Traceability row.
   - Current: `| NIT-03 | Phase 31 | Pending |`
   - Replacement: `| NIT-03 | Phase 32 | Pending |`
2. Coverage line.
   - Current: `- Mapped to phases: 11 (Phase 30: 6, Phase 31: 3, Phase 32: 2)`
   - Replacement: `- Mapped to phases: 11 (Phase 30: 6, Phase 31: 2, Phase 32: 3)`
3. The note paragraph.
   - Current: `NIT-03 is mapped to Phase 31. It may have to move to Phase 32 if GitHub cannot dispatch the new nightly before it is on `custom_build`; Phase 31 research settles this and the user decides (see ROADMAP.md, Phase 31 research flag).`
   - Replacement: `NIT-03 moved to Phase 32 on {date} by the user's decision (D-02, Phase 31 E-16): GitHub runs workflow_dispatch only for a workflow file on the default branch, so the nightly's first manual run can happen only after Phase 32 publishes it. Phase 31 delivered the measurement, the workflow with its schedule gate, the guards and any skip list.`
4. The closing line.
   - Current: `*Last updated: 2026-10-24 after roadmap creation (Phases 30-32, 11/11 mapped)*`
   - Replacement: `*Last updated: {date} after moving NIT-03 to Phase 32 (D-02)*`

**ROADMAP.md** (nine edits):

1. Phase 31 milestone bullet (`- [ ] **Phase 31: ...`).
   - Current text: `and its first manual run is green or every failure is named in a skip list`
   - Replacement: `and its first manual run follows publication in Phase 32 (NIT-03)`
2. Phase 32 milestone bullet (`- [ ] **Phase 32: ...`), which ends `...and one green CI run on the published SHA before the tags`.
   - Addition before its end: `, then the nightly's first manual run (NIT-03)`
3. Phase 31 Goal.
   - Current text: `Its first manual run is green, or every failing test is named in a skip list with its cause.`
   - Replacement: `Its first manual run happens after publication, in Phase 32 (NIT-03, moved by D-02).`
4. Phase 31 requirements.
   - Current: `**Requirements**: NIT-01, NIT-02, NIT-03`
   - Replacement: `**Requirements**: NIT-01, NIT-02`
5. Phase 31 success criterion 4.
   - Current: `  4. *[CI, operator]* A manual dispatch of the nightly finishes green, or every failing test is listed by name in a skip list with its cause from criterion 1. The list names tests, not patterns, and it deletes or weakens no existing test. The schedule starts only after that run is recorded, and no scheduled run precedes it (NIT-03).`
   - Replacement: `  4. *[record]* The schedule is gated by the repository variable `NIGHTLY_ENABLED`, which stays unset, so no scheduled run executes a job before the dispatch run. The dispatch run itself moved to Phase 32 with NIT-03 on {date} (D-02), because GitHub dispatches a workflow only from the default branch. Every failure of the measured run is named in the skip list with its cause, or the run was green.`
6. Phase 31 Note bullet.
   - Current: `- The user dispatches the run from GitHub's `Run workflow` control and reports the result directly.`
   - Replacement: `- The dispatch run is an operator action in Phase 32 (NIT-03): the user starts it from GitHub's `Run workflow` control and reports the result directly.`
7. Phase 32 requirements.
   - Current: `**Requirements**: CIG-02, CIG-03`
   - Replacement: `**Requirements**: CIG-02, CIG-03, NIT-03`
8. Phase 32 gains a criterion 5, after criterion 4 (which ends `...that the plan did not intend.`).
   - Added: `  5. *[CI, operator]* After the `--no-ff` merge to `custom_build`, the user dispatches the nightly from GitHub's `Run workflow` control and reports the result directly. The run finishes green, or every failing test is listed by name in the skip list with its cause from Phase 31's measurement; the list names tests, not patterns, and deletes or weakens no existing test. `NIGHTLY_ENABLED` is set only after that run is recorded, and no scheduled run with a conclusion other than `skipped` precedes it. A fix the run needs is a Phase 31 gap, because this phase edits no build, `gradle/` or `.github/` file (NIT-03).`
9. The Structure Notes bullet.
   - Current: `- **NIT-03 sits in Phase 31,** but GitHub may not dispatch the new file before it is on `custom_build`. The Phase 31 research settles this, and the user decides whether NIT-03 moves.`
   - Replacement: `- **NIT-03 moved to Phase 32 on {date} (D-02).** GitHub dispatches a workflow only from the default branch, and the nightly reaches `custom_build` only at publication. Phase 31 delivered everything before the dispatch: the measurement, the gated workflow, the guards and any skip list.`

(Plan 31-05 calls these "the eight ROADMAP edits" in Task 3's acceptance criterion, but its list holds nine items; this record lists all nine and Task 3 applies each as its own scoped `Edit`. Nothing in the list was dropped or added.)

**STATE.md** (one line appended to `### Blockers/Concerns`, on move):
`- [Roadmap] {date}: NIT-03 moved to Phase 32 by the user's decision (D-02, 31-COMMAND-EVIDENCE.md E-16). Phase 31 delivered the measurement, the gated nightly, the NIT-02 byte guard and any skip list. The nightly reaches custom_build only with Phase 32's publication, and NIGHTLY_ENABLED stays unset until the dispatch run is recorded.`

Research open question 1: the Phase 32 criterion says "no scheduled run with a conclusion other than `skipped` precedes it". After the merge the cron still ticks while `NIGHTLY_ENABLED` is unset, and GitHub likely lists those ticks as skipped runs (research A1; not measurable before the merge, because the workflow has no run yet).

Matches text: pending (31-05)
