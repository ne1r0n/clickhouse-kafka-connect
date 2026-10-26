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
