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
