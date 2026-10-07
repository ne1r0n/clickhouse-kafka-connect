# Phase 10: Test Harness Reconciliation - Pattern Map

**Mapped:** 2026-09-20
**Files analyzed:** 8 code files (+ 2 doc-only files noted separately)
**Analogs found:** 8 / 8

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` | test (harness/base class) | event-driven (JUnit lifecycle hooks) + CRUD (DB create/drop) | itself, pre-merge revision (`git show pre-v1.6.0-sync:…/ClickHouseBase.java`) | exact — same file, prior semantics to re-derive |
| `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` | config/utility | config (constant default) | itself, pre-merge revision, line 42 | exact — single-constant change |
| `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java` | test (canary) | request-response (drives real inserts + MBean queries) | `src/test/java/com/clickhouse/kafka/connect/sink/SmtSinkLandingTest.java` (API-redirection + local `createTable` convention) | role-match, current post-merge API |
| `build.gradle.kts` | config | config | itself, lines 434-451 (`test { java { exclude(...) } }` block) | exact — deletion only |
| `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java` | test (opt-out override) | event-driven (lifecycle override) | `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java` (sibling, near-identical `setup()`/`tearDown()` shape) | exact — sibling pair |
| `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java` | test (opt-out override) | event-driven | `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java` (sibling) | exact — sibling pair |
| New Docker-free canary for `CLICKHOUSE_VERSION_DEFAULT` (location at planner's discretion, e.g. `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseVersionDefaultUnitTest.java`) | test (unit canary) | config/transform (asserts a constant, no I/O) | `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java` | role-match — established "Docker-free canary for a silently-vanishing behaviour" pattern |
| `.planning/MERGE-CHECKLIST.md` / `.planning/REQUIREMENTS.md` | docs (row flips / annotate-don't-flip) | — | N/A — see "No Analog Found" | n/a |

## Pattern Assignments

### `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` (test harness, event-driven + CRUD)

**Analog:** itself, pre-merge revision — `git show pre-v1.6.0-sync:src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` — combined with the **current** post-merge file (143 lines, read in full this session) which supplies the `@TestInstance(PER_CLASS)` shape, `Network`, memory cap, and `keeper_map.xml` mount that must be preserved.

**Current file in full** (`src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java:1-143`) — this is the file being edited, not copied from; read it before editing since D-01/D-02/D-03/D-09a all land inside `setup()`/`setupContainer()`/`tearDown()`.

**Imports pattern** (current, lines 1-23):
```java
package com.clickhouse.kafka.connect.sink;

import com.clickhouse.client.config.ClickHouseClientOption;
import com.clickhouse.kafka.connect.ClickHouseSinkConnector;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseCluster;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.google.crypto.tink.internal.Random;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
```
New imports the D-01/D-09a work will need, all already present elsewhere in the tree: `org.junit.jupiter.api.AfterEach`, `java.lang.management.ManagementFactory`, `java.util.Set`, `javax.management.MalformedObjectNameException`, `javax.management.ObjectName`, `java.util.concurrent.atomic.AtomicInteger` (for `SHARED_CONTAINER_STARTS`), `com.clickhouse.kafka.connect.util.jmx.MBeanServerUtils` — pull the exact import lines from the pre-merge file above (its own imports block includes every one of these, lines 1-21).

**Current `setup()`/`setupContainer()`** (`ClickHouseBase.java:36-71`) — the D-01/D-02 target. `setupContainer` unconditionally starts a fresh `ClickHouseContainer` per class today; D-01 wraps the call site in a static lazy guard, D-02 branches it on the opt-out hook:
```java
@BeforeAll
public void setup() throws IOException {
    Assertions.assertFalse(isCluster && isCloud, ...);
    if (isCluster) {
        cluster = ClickHouseCluster.getClusterFromEnvVarOrThrow();
        cluster.start();
    } else if (!isCloud) {
        setupContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE);
    }
    try (var tmpClient = ClickHouseTestHelpers.createClient(getBaseProps())) {
        setDatabase(String.format("kafka_connect_test_%d_%s", Math.abs(Random.randInt()), System.currentTimeMillis()));
        ClickHouseTestHelpers.createDatabase(database, tmpClient);
        tmpClient.ping();
    }
}

public void setupContainer(String clickhouseDockerImage) {
    Network network = Network.newNetwork();
    this.db = new ClickHouseContainer(clickhouseDockerImage)
            .withNetwork(network)
            .withNetworkAliases(ClickHouseTestHelpers.CLICKHOUSE_DB_NETWORK_ALIAS)
            .withPassword("test_password")
            .withCreateContainerCmdModifier(cmd -> {
                cmd.getHostConfig().withMemory(1024 * 1024 * 1024 * 2L);
            })
            .withExposedPorts(8123)
            .withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("clickhouse_keeper_map.xml"),
                    "/etc/clickhouse-server/config.d/keeper_map.xml");
    db.start();
}
```
**Pre-merge lazy-guard + counter reference** (`git show pre-v1.6.0-sync:…/ClickHouseBase.java`, lines ~35-67):
```java
private static final AtomicInteger SHARED_CONTAINER_STARTS = new AtomicInteger();
protected static ClickHouseContainer db = null;

protected static int sharedContainerStarts() {
    return SHARED_CONTAINER_STARTS.get();
}

@BeforeAll
public static void setup() throws IOException {
    database = newTestDatabaseName();
    if (isCloud) { initialPing(); return; }
    if (db == null) {
        db = new ClickHouseContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE)
                .withImagePullPolicy(PullPolicy.alwaysPull())
                .withPassword("test_password")
                .withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1");
        LOGGER.info("Starting shared ClickHouse container {} for this test JVM", ...);
        db.start();
        SHARED_CONTAINER_STARTS.incrementAndGet();
    }
}
```
Copy the `if (sharedDb == null) { ...; SHARED_CONTAINER_STARTS.incrementAndGet(); }` guard shape onto the **current** `setupContainer(...)` call site (which retains `Network`/memory-cap/`keeper_map.xml` that pre-merge never had) — do not copy the pre-merge body verbatim, re-derive per D-01's "re-express rather than restore" instruction. `PullPolicy.alwaysPull()` should only apply per D-05 (floating tag only), not unconditionally as pre-merge did.

**`tearDown()` pattern — drop-not-stop** (current, `ClickHouseBase.java:73-88`, stops unconditionally today):
```java
@AfterAll
protected void tearDown() {
    if (isCluster) {
        cluster.stop();
    } else if (!isCloud) {
        ClickHouseContainer ch = getDb();
        if (ch != null) {
            LOGGER.info("Stopping db container: id={}, port={}", ch.getContainerId(), ch.getMappedPort(8123));
            ch.copyFileFromContainer("/var/log/clickhouse-server/clickhouse-server.log",
                    "./build/reports/tests/server_" + db.getMappedPort(8123) + ".log");
            ch.copyFileFromContainer("/var/log/clickhouse-server/clickhouse-server.err.log",
                    "./build/reports/tests/server-err_" + db.getMappedPort(8123) + ".log");
            ch.stop();
        }
    }
}
```
**Pre-merge drop-database shape** (the D-01 target for the shared branch):
```java
@AfterAll
protected static void tearDown() {
    if (database != null && (isCloud || db != null)) {
        try {
            dropDatabase(database);
        } catch (Exception e) {
            LOGGER.error("Error dropping database", e);
        }
    }
}
```
`ClickHouseTestHelpers.dropDatabase(chc, database)` already exists and handles the cluster `ON CLUSTER` clause (`ClickHouseTestHelpers.java:410-418`) — use it instead of hand-rolled SQL:
```java
public static void dropDatabase(ClickHouseHelperClient chc, String database) {
    String dropDatabaseQuery = "DROP DATABASE IF EXISTS `" + database + "`" + getClusterClauseOrEmpty();
    try (Records ignored = chc.queryV2(dropDatabaseQuery)) {
        // success
    } ...
}
```
Keep `ch.stop()` in the **opt-out** branch of `tearDown()` — the two proxy classes' own overrides call `super.tearDown()` and depend on their own container being stopped, not merely having its database dropped (see Common Pitfall 3 in RESEARCH.md).

**MBean sweep pattern — restore verbatim** (absent from current file; pre-merge shape, `git show pre-v1.6.0-sync:…`, ~lines 78-93):
```java
/**
 * Safety net for D-09: unregisters any {@code com.clickhouse:type=ClickHouseKafkaConnector}
 * MBean left registered by a test that failed between {@code start()} and {@code stop()},
 * so a leak never crosses into a later test.
 */
@AfterEach
protected void cleanupLeakedMBeans() {
    try {
        Set<ObjectName> leaked = ManagementFactory.getPlatformMBeanServer()
                .queryNames(new ObjectName("com.clickhouse:type=ClickHouseKafkaConnector,*"), null);
        for (ObjectName name : leaked) {
            LOGGER.warn("Unregistering leaked MBean left by a test: {}", name);
            MBeanServerUtils.unregisterMBean(name.toString());
        }
    } catch (MalformedObjectNameException e) {
        throw new RuntimeException(e);
    }
}
```
This is copy-paste exact per D-09a ("restore the sweep verbatim") — the `,*` wildcard already structurally matches `SinkTaskStatistics`'s per-topic name composition (see Shared Patterns § MBean Sweep below), so no pattern edit is needed.

**Opt-out hook (D-02), new — no direct pre-merge analog** (pre-merge had no opt-out concept since the container was JVM-wide with no per-class network). Smallest-diff shape, matching the existing override style already used for `setup()`/`tearDown()` in the two proxy classes:
```java
protected boolean usesSharedContainer() {
    return true;
}
```
`ClickHouseSinkTaskSchemalessProxyTest`/`ClickHouseSinkTaskWithSchemaProxyTest` override this to `false` (see their own Pattern Assignment below) — their branch of `setup()`/`setupContainer()`/`tearDown()` must call `setupContainer(...)`/`ch.stop()` directly, bypassing both `sharedDb` reuse and `SHARED_CONTAINER_STARTS.incrementAndGet()`.

---

### `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` (config/utility)

**Analog:** itself, pre-merge revision, line 42 (`CLICKHOUSE_VERSION_DEFAULT = "latest"`).

**Current state** (`ClickHouseTestHelpers.java:30-31`):
```java
public static final String CLICKHOUSE_VERSION_DEFAULT = "25.8.18.1"; // keep this as close to the latest LTS version
public static final String CLICKHOUSE_DOCKER_IMAGE = String.format("clickhouse/clickhouse-server:%s", getClickhouseVersion());
```
**D-04's target:**
```java
public static final String CLICKHOUSE_VERSION_DEFAULT = "latest";
```
Remove or rewrite the stale `// keep this as close to the latest LTS version` comment — it describes the value this phase reverts away from. `getClickhouseVersion()` (`:55-61`) is the reader and needs no change:
```java
public static String getClickhouseVersion() {
    String clickHouseVersion = System.getenv(CLICKHOUSE_VERSION);
    if (clickHouseVersion == null) {
        clickHouseVersion = CLICKHOUSE_VERSION_DEFAULT;
    }
    return clickHouseVersion;
}
```
D-05's conditional `alwaysPull()` reads this same value at the `ClickHouseBase.setupContainer()` call site — apply `PullPolicy.alwaysPull()` only when `ClickHouseTestHelpers.getClickhouseVersion().equals("latest")` (a floating tag), leaving pinned versions (e.g. a `25.8.18.1` comparison run per D-06) on the default cached pull.

---

### `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java` (test canary, request-response)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/SmtSinkLandingTest.java` (established API-redirection + local-`createTable` convention; also extends `ClickHouseBase` and is currently excluded from nothing, i.e. it compiles clean against the post-merge API).

**Current (broken, excluded) canary calls** (`ClickHouseTestHarnessTest.java`, read in full this session, 193 lines) — four call sites that do not exist on the post-merge API:
```java
// :80-81, :119-120, :151-152, :175
Map<String, String> props = createProps();
ClickHouseHelperClient chc = createClient(props);
// :123-124, :178-179
ClickHouseTestHelpers.createTable(chc, "harness_mbean_probe_table",
        "CREATE TABLE IF NOT EXISTS %s (id UInt8) ENGINE = MergeTree ORDER BY id");
// :180-183
ClickHouseTestHelpers.query(chc, String.format("INSERT INTO `%s` VALUES (1, unhex('...'))", table));
```
Confirmed this session: `ClickHouseTestHelpers.java` (461 lines, grepped in full) has **no** `createTable` or `query` static method — only `dropTable(chc, tableName)` (`:97`), `executeQueryIgnoreResult(chc, sql)` (`:88-95`), `dropDatabase(chc, database)` (`:410-418`), and `createClient(props)` (`:420`). `ClickHouseBase` has no `createProps()` — only `getBaseProps()` (`ClickHouseBase.java:102-125`).

**Redirect pattern, sourced from `SmtSinkLandingTest.java` and the 12 files Phase 7 migrated (D-06/D-09):**
```java
// Before                                    // After
createProps()                             -> getBaseProps()
createClient(props)                       -> ClickHouseTestHelpers.createClient(props)
ClickHouseTestHelpers.query(chc, sql)     -> ClickHouseTestHelpers.executeQueryIgnoreResult(chc, sql)
ClickHouseTestHelpers.createTable(...)    -> local private static createTable(...) helper (below)
```
**Local `createTable` helper — copy verbatim from `SmtSinkLandingTest.java:40-46`** (established per-file convention; six other fork test files each carry an identical copy):
```java
private static void createTable(ClickHouseHelperClient chc, String tableName, String createTableQuery) {
    try (Records ignored = chc.queryV2(String.format(createTableQuery, tableName))) {
        // table created
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
```
Requires the import `com.clickhouse.client.api.query.Records` (already present in `SmtSinkLandingTest.java:5`). `ClickHouseTestHelpers.dropTable(chc, table)` (already used at `ClickHouseTestHarnessTest.java:178`) needs **no** redirect — it already exists and matches the call shape used.

**Two new D-08 pins to add** (no existing analog — new assertions, not a copy-paste pattern): follow the file's own established `@Order`-sequenced, `DeliberateMidTestFailure`/`SwallowDeliberateMidTestFailure`-driven style already present at `:36-133` for both the opt-out-hook pin and the per-topic MBean-sweep pin. Reuse `mbeanCountForConnector(String)` (`:53-64`) as the query primitive; for the per-topic pin, query `…,topic=<t>` instead of the bare `connector=<name>,*` pattern, per `SinkTaskStatistics.getMBeanName()`/`getMBeanNameForTopic(topic)` composition below.

---

### `build.gradle.kts` (config)

**Analog:** itself — the surrounding `sourceSets { test { java { exclude(...) } } }` block (`build.gradle.kts:434-451`).

**Current:**
```kotlin
sourceSets {
    main { java { srcDir(generateVersionFile) } }
    // TEMPORARY (Phase 7, ...): this file references an API reserved for a later phase ...
    test {
        java {
            // Phase 10 (HARN-04): ClickHouseBase shared-container contract
            exclude("**/ClickHouseTestHarnessTest.java")
        }
    }
}
```
D-07's change: delete the `exclude(...)` line and its two comment lines (`:440-444`, `:447`) once the canary compiles clean against the redirected API — leave the `test { java { ... } }` block structurally empty or remove it entirely if nothing else needs excluding (grep for other `exclude(` calls in the same block first).

**Unrelated, read-only context for the gate step** — `testUnit` task (`:239-248`) is what HARN-01's Docker-free proof runs against; no change needed here, just re-verify `excludeTags("clickhouse")` still reads `@Tag("clickhouse")` off `ClickHouseBase.java:25`.

---

### `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java` (opt-out override)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java` (sibling class, near-identical `setup()`/`tearDown()`/Toxiproxy-wiring shape — read this session's setup/teardown region).

**Current `setup()`/`tearDown()`** (grepped this session):
```java
@BeforeAll
public void setup() throws IOException {
    super.setup();
    toxiproxy = new ToxiproxyContainer(ClickHouseTestHelpers.TOXIPROXY_DOCKER_IMAGE_NAME)
            .withNetwork(isCluster || isCloud ? Network.newNetwork() : db.getNetwork())
            .withNetworkAliases(ClickHouseTestHelpers.TOXIPROXY_NETWORK_ALIAS);
    ...
    toxiproxy.start();
    ...
}

@AfterAll
public void tearDown() {
    super.tearDown();
    ...
    toxiproxy.stop();
}
```
**D-02's addition — only the opt-out override, Toxiproxy wiring stays untouched:**
```java
@Override
protected boolean usesSharedContainer() {
    return false;
}
```
Placing this override means `super.setup()`'s branch inside `ClickHouseBase` (see that file's Pattern Assignment above) takes the opt-out path: `setupContainer(...)` runs directly for this class's **own** container (not the shared `sharedDb`), and `db.getNetwork()` on line referenced above continues to resolve to a network no other of the 25 shared classes is on — which is the entire point of D-02 (both proxy classes claim network alias `"toxiproxy"`; isolating each on its own container's `Network` removes the collision by construction). No other line in this file changes.

---

### `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java` (opt-out override)

**Analog:** `ClickHouseSinkTaskSchemalessProxyTest.java` (sibling, same shape — see above). Apply the identical one-method addition:
```java
@Override
protected boolean usesSharedContainer() {
    return false;
}
```
Same reasoning and same "Toxiproxy wiring itself stays upstream's" constraint from CONTEXT.md's "Files this phase touches directly" list.

---

### New Docker-free canary: `CLICKHOUSE_VERSION_DEFAULT` pin (D-04) — new file, test/unit, config

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java` (Phase 9 D-11's canary for a different silently-vanishing fork behaviour — the established shape for "Docker-free canary for a silently-vanishing behaviour" per RESEARCH.md § Established Patterns).

**Structural pattern to copy — deliberately extends nothing, not `ClickHouseBase`:**
```java
package com.clickhouse.kafka.connect.sink.db;
// (or wherever the planner places it — must stay reachable from testUnit)

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Docker-free unit pin for [D-04]: the fork's {@code CLICKHOUSE_VERSION_DEFAULT} must stay
 * "latest" ... New fork-only class: it does not extend or modify any upstream test class, and
 * it deliberately extends nothing - not {@code ClickHouseBase}, whose inherited
 * {@code @Tag("clickhouse")} would drop it out of {@code testUnit}.
 */
public class ClickHouseVersionDefaultUnitTest {

    @Test
    void clickhouseVersionDefaultIsLatest() {
        assertEquals("latest", ClickHouseTestHelpers.CLICKHOUSE_VERSION_DEFAULT);
    }
}
```
**Why this shape, not `ClickHouseTestHarnessTest`'s `assertEquals(CLICKHOUSE_DOCKER_IMAGE, db.getDockerImageName())`:** per CONTEXT.md D-04, the canary must assert the **default value itself**, not the composed image name — the composed-image assertion is self-consistent whatever the default is and would not catch this regression. Model the class-level Javadoc framing (the "New fork-only class" / "deliberately extends nothing" language) directly on `MetadataFreeJsonInsertUnitTest.java:27-41`'s Javadoc block — same justification, different constant.

## Shared Patterns

### Container lifecycle sharing (D-01/D-02/D-03)
**Source:** pre-merge `ClickHouseBase.java` (`git show pre-v1.6.0-sync:src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java`) re-derived over the current post-merge file
**Apply to:** `ClickHouseBase.java` only (all 27 dependent test classes inherit it transitively; only the two proxy classes need a local override)
```java
private static volatile ClickHouseContainer sharedDb;
private static final AtomicInteger SHARED_CONTAINER_STARTS = new AtomicInteger();

protected boolean usesSharedContainer() { return true; }

public static int sharedContainerStarts() { return SHARED_CONTAINER_STARTS.get(); }
```

### MBean leak sweep (D-09a / HARN-02)
**Source:** pre-merge `ClickHouseBase.cleanupLeakedMBeans()` + `MBeanServerUtils.unregisterMBean` (`src/main/java/com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.java:30-41`, read-only, unmodified this phase)
**Apply to:** `ClickHouseBase.java` only — inherited by every one of the 27 live test classes
```java
public static void unregisterMBean(final String mBeanName) {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
        ObjectName objectName = new ObjectName(mBeanName);
        if (server.isRegistered(objectName)) {
            server.unregisterMBean(objectName);
        }
    } catch (Exception e) {
        LOGGER.warn("Unable to unregister MBean " + mBeanName, e);
    }
}
```
The wildcard pattern `"com.clickhouse:type=ClickHouseKafkaConnector,*"` already matches per-topic names — confirmed this session by reading `SinkTaskStatistics.java:180-189`:
```java
public String getMBeanName() { ... }  // :180, base pattern com.clickhouse:type=...,connector=%s,name=SinkTask%d,version=%s
// :185-189 — per-topic name is the base + ",topic=" + topic, composed, not a second format string
```
No pattern widening needed unless D-08's new per-topic pin proves otherwise.

### API redirection off `ClickHouseBase`'s removed `createProps()`/`createClient()` (D-07)
**Source:** the 12 fork test files Phase 7 already migrated (e.g. `SmtSinkLandingTest.java`), established at Phase 7 D-06/D-09
**Apply to:** `ClickHouseTestHarnessTest.java` only (the 13th and last file needing this redirect)
```java
Map<String, String> props = getBaseProps();
ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
```

### Docker-free canary for a silently-vanishing fork behaviour (D-04)
**Source:** `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java` (Phase 9 D-11)
**Apply to:** the new `CLICKHOUSE_VERSION_DEFAULT` canary — extend nothing (not `ClickHouseBase`), assert the raw constant, keep it reachable from `testUnit` (no `@Tag("clickhouse")` anywhere in its ancestry).

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `.planning/MERGE-CHECKLIST.md` (17 row flips) | docs | — | Not source code; follow the existing "annotate-don't-flip" convention already used at rows 33/37/44 (append a note with the commit SHA) rather than a code pattern. No analog needed — this is bookkeeping, not implementation. |
| `.planning/REQUIREMENTS.md` § HARN | docs | — | Same as above — HARN-04/05/06/07 close on evidence (D-10), not code; record the verifying command's output inline. |

## Metadata

**Analog search scope:** `src/test/java/com/clickhouse/kafka/connect/sink/`, `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/`, `src/main/java/com/clickhouse/kafka/connect/util/jmx/`, `build.gradle.kts`, plus the `pre-v1.6.0-sync` git tag for the pre-merge reference implementation.
**Files scanned (read in full or via targeted grep this session):** `ClickHouseBase.java` (143 lines, full), `ClickHouseTestHarnessTest.java` (193 lines, full), `MBeanServerUtils.java` (43 lines, full), `ClickHouseTestHelpers.java` (grepped for method signatures + lines 1-100 read), `SinkTaskStatistics.java` (grepped for MBean-name composition), `SmtSinkLandingTest.java` (lines 1-50), `MetadataFreeJsonInsertUnitTest.java` (250 lines, full), `ClickHouseSinkTaskSchemalessProxyTest.java` (grepped for setup/teardown), `build.gradle.kts` (lines 180-260, 420-450), pre-merge `ClickHouseBase.java` (via `git show`, ~160 lines read).
**All analog paths verified git-tracked via `git ls-files`** — no gitignored/mirror paths involved (this repository has no `.gsd/capabilities` install mirror in play for this phase).
**Pattern extraction date:** 2026-09-20

