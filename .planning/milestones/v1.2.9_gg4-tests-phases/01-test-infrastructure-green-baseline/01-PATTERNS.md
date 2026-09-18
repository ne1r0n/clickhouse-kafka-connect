# Phase 1: Test Infrastructure & Green Baseline - Pattern Map

**Mapped:** 2026-09-15
**Files analyzed:** 8 (6 modified, 2 new; baseline-fix edits under D-01/D-02 are unknown until the first `test` run and use the same analogs as their own file)
**Analogs found:** 8 / 8 (all self-analogs or sibling-file analogs — this is a test-harness/build-config phase, not new-feature code, so "closest analog" is usually the file's own current content plus one sibling file for the pattern being retrofitted)

All paths below were verified git-tracked via `git ls-files -- <path>` this session; none are gitignored mirrors.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|--------------------|------|-----------|-----------------|---------------|
| `build.gradle.kts` | config | batch (build-graph wiring) | itself, `tasks.withType<Test>` block (lines 171-216) and `integrationTest` registration (lines 151-165) | exact (self-modify) |
| `lombok.config` (NEW, repo root) | config | — | none in-repo (first Lombok config file); pattern taken from Lombok's own docs cross-referenced in RESEARCH.md | no analog — new artifact type |
| `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` | provider (test fixture / lifecycle base) | event-driven (JUnit `@BeforeAll`/`@AfterAll`/`@AfterEach` callbacks) | itself (current `setup()`/`tearDown()`/`createProps()`), plus `MBeanServerUtils.java` for the sweep's query/unregister idiom | exact (self-modify) |
| `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java` | test (ClickHouse-backed, integration) | request-response | `ClickHouseSinkTaskSchemalessProxyTest.java` (identical `@BeforeAll` shape — fix both the same way) | exact (sibling file, same bug) |
| `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java` | test (ClickHouse-backed, integration) | request-response | `ClickHouseSinkTaskWithSchemaProxyTest.java` (identical `@BeforeAll` shape) | exact (sibling file, same bug) |
| `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` | utility | file-I/O (HTTP response stream decoding) | itself, `getAllRowsAsJson` (lines 151-172) and `validateRows` (lines 226-274) | exact (self-modify) |
| `<new version-smoke-test class>` e.g. `ClickHouseServerVersionSmokeTest.java` (NEW) | test (ClickHouse-backed) | request-response | `ClickHouseSinkTaskTest.java` (class shell/`extends ClickHouseBase`/`@Test` shape) + `ClickHouseHelperClient.version()`/`versionV1()`/`versionV2()` (existing API to call, not reimplement) | role-match (shell) + exact (API to invoke) |
| `<deliberate-failure MBean test>`, e.g. a method in a NEW `ClickHouseBaseMBeanLeakTest.java` or similar (NEW) | test (ClickHouse-backed) | event-driven (failure-path lifecycle proof) | `JsonColumnTest.java` (anti-pattern: `start()`/`put()`/`stop()` with no try/finally, lines ~71-73) as "what leaks", `ClickHouseSinkTaskTest.java` (lines 136-153) as "what try/finally with unique name should look like" | role-match |

## Pattern Assignments

### `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` (provider, event-driven)

**Analog:** itself — current file read in full this session (237 lines).

**Current state to replace** (`ClickHouseBase.java:27-53`):
```java
@BeforeAll
public static void setup() throws IOException  {
    if (database == null) {
        database = String.format("kafka_connect_test_%s", System.currentTimeMillis());
    }
    if (isCloud) {
        initialPing();
        return;
    }
    db = new ClickHouseContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE).withPassword("test_password").withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1");
    db.start();
}

@AfterAll
protected static void tearDown() {
    if (isCloud) {//We need to clean up databases in the cloud, we can ignore the local database
        if (database != null) {
            try {
                dropDatabase(database);
            } catch (Exception e) {
                LOGGER.error("Error dropping database", e);
            }
        }
    }
    if (db != null)
        db.stop();
}
```
Two bugs to fix here, confirmed by reading the file (not inferred): (1) `database` uses `if (database == null)` — guarded, so it is actually already a *single shared* name across the whole fork, the opposite of "container-per-class" framing in some docs; D-08 needs this to become **unconditional** per-class. (2) `db` has **no** null guard at all — it restarts on every subclass's `@BeforeAll`, which is the real INFRA-03 bug; D-08/INFRA-03 needs a `if (db == null)` guard, and `tearDown()` must stop calling `db.stop()` unconditionally once the container is shared across classes in a fork (rely on Ryuk or a shutdown hook per Claude's Discretion).

**Imports pattern** (`ClickHouseBase.java:1-19`) — add `org.testcontainers.images.PullPolicy`, `javax.management.*`, `java.lang.management.ManagementFactory`, `org.junit.jupiter.api.AfterEach`, `org.junit.jupiter.api.Tag`, `org.junit.jupiter.api.TestInfo` to this existing block; keep the fully-qualified, no-alias style already used (`org.testcontainers.clickhouse.ClickHouseContainer` at line 19).

**MBean sweep pattern — build from `MBeanServerUtils.java` (full file, 43 lines), the actual query/unregister idiom to reuse:**
```java
// src/main/java/com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.java:30-41
public static void unregisterMBean(final String mBeanName) {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
        ObjectName objectName = new ObjectName(mBeanName);
        if (server.isRegistered(objectName)) {
            server.unregisterMBean(objectName);
        }
    } catch (Exception e) {
        // JMX might not be available
        LOGGER.warn("Unable to unregister MBean " + mBeanName, e);
    }
}
```
`unregisterMBean` is already safe to call on a name that isn't registered (`isRegistered` guard) — the new `@AfterEach` in `ClickHouseBase` should call `ManagementFactory.getPlatformMBeanServer().queryNames(new ObjectName("com.clickhouse:type=ClickHouseKafkaConnector,*"), null)` directly (there's no existing `queryNames`-wrapping helper to copy — this is the one genuinely new snippet in this file, add it inline in `ClickHouseBase`, not in `MBeanServerUtils`, since D-09 scopes the sweep to the test tier) and then call `MBeanServerUtils.unregisterMBean(name.toString())` per leaked name, logging each one (mirrors the `LOGGER.warn`/`LOGGER.error` level conventions already used at `ClickHouseBase.java:47` and `MBeanServerUtils.java:26/39`).

**MBean-name format to match in the sweep's `ObjectName` pattern and in the smoke/leak tests** (`src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:71-79`):
```java
private String getMBeanName() {
    String version = ClickHouseClientOption.class.getPackage().getImplementationVersion();
    if (version != null) {
        version = version.replace(':', '_');
    } else {
        version = "unknown";
    }
    return String.format("com.clickhouse:type=ClickHouseKafkaConnector,connector=%s,name=SinkTask%d,version=%s", connectorName, id, version);
}
```
`connectorName` is unused internally by `ProxySinkTask` beyond this string — it comes straight from `props.get("name")` in `ClickHouseSinkTask.start()` (`src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java:36`):
```java
connectorName = props.get("name");
...
this.proxySinkTask = new ProxySinkTask(clickHouseSinkConfig, errorReporter, connectorName);
```
So the D-10 unique-connector-name helper in `ClickHouseBase` must write into `props.put("name", <unique-value>)` (no `ClickHouseSinkConnector.NAME`-style constant exists for this key — `"name"` is the literal Kafka Connect standard key already used verbatim at `ClickHouseSinkTask.java:36`). This resolves RESEARCH.md's Open Question #2.

**Existing `createProps()` to extend, not replace** (`ClickHouseBase.java:209-231`, keep unchanged per D-10 — new tests call the new opt-in helper on top of this, they don't modify `createProps()` itself):
```java
protected Map<String,String> createProps() {
    Map<String, String> props = new HashMap<>();
    String clientVersion = extractClientVersion();
    props.put(ClickHouseSinkConnector.CLIENT_VERSION, clientVersion);
    if (isCloud) {
        ...
    } else {
        props.put(ClickHouseSinkConnector.HOSTNAME, db.getHost());
        props.put(ClickHouseSinkConnector.PORT, db.getFirstMappedPort().toString());
        props.put(ClickHouseSinkConnector.DATABASE, ClickHouseTestHelpers.DATABASE_DEFAULT);
        props.put(ClickHouseSinkConnector.USERNAME, db.getUsername());
        props.put(ClickHouseSinkConnector.PASSWORD, db.getPassword());
        props.put(ClickHouseSinkConnector.SSL_ENABLED, "false");
    }
    return props;
}
```

**`@Tag` placement:** single `@Tag("clickhouse")` goes on the `ClickHouseBase` class declaration itself (`ClickHouseBase.java:21`, currently `public class ClickHouseBase {`) — `@Inherited` propagates to all 14 confirmed subclasses (`grep -rln "extends ClickHouseBase" src/test/java | wc -l` → 14, verified this session), no per-subclass edits needed.

---

### `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java` and `ClickHouseSinkTaskSchemalessProxyTest.java` (test, request-response)

**Analog:** each other — both classes have byte-for-byte identical `@BeforeAll` bodies, confirmed by reading both files in full this session.

**The conflict to remove, present in both files identically** (`ClickHouseSinkTaskWithSchemaProxyTest.java:37-46`, `ClickHouseSinkTaskSchemalessProxyTest.java:27-36`):
```java
@BeforeAll
public static void setup() throws IOException {
    Network network = Network.newNetwork();
    // Note: we are using a different version of ClickHouse for the proxy - https://github.com/ClickHouse/ClickHouse/issues/58828
    db = new org.testcontainers.clickhouse.ClickHouseContainer(ClickHouseTestHelpers.CLICKHOUSE_FOR_PROXY_DOCKER_IMAGE)
            .withNetwork(network)
            .withNetworkAliases("clickhouse")
            .withPassword("test_password")
            .withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1");
    db.start();

    toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.7.0").withNetwork(network).withNetworkAliases("toxiproxy");
    toxiproxy.start();

    ToxiproxyClient toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
    proxy = toxiproxyClient.createProxy("clickhouse-proxy", "0.0.0.0:8666", "clickhouse:" + ClickHouseProtocol.HTTP.getDefaultPort());
}
```
Both assign the inherited static `db` directly — once `ClickHouseBase`'s `if (db == null)` singleton guard lands (D-08/INFRA-03), whichever of these two runs first in a fork "poisons" `db` for the rest of that fork, and neither class has an `@AfterAll` override, so the base `tearDown()` stops whatever `db` currently points to. Fix (D-07, minimal diff per Claude's Discretion): rename this local assignment target to a new `private static ClickHouseContainer proxyDb` field declared in each of these two classes (not in the base), and add each class's own `@AfterAll` that stops `proxyDb` and `toxiproxy` — never touching `ClickHouseBase.db`.

**Existing `getTestProperties()` override to keep as the model for pointing at the isolated container** (`ClickHouseSinkTaskWithSchemaProxyTest.java:55-65`):
```java
private Map<String, String> getTestProperties() {
    Map<String, String> props = new HashMap<>();
    props.put(ClickHouseSinkConnector.DATABASE, "default");
    props.put(ClickHouseSinkConnector.USERNAME, db.getUsername());
    props.put(ClickHouseSinkConnector.PASSWORD, db.getPassword());
    props.put(ClickHouseSinkConnector.SSL_ENABLED, "false");
    props.put(ClickHouseSinkConfig.PROXY_TYPE, ClickHouseProxyType.HTTP.name());
    props.put(ClickHouseSinkConfig.PROXY_HOST, toxiproxy.getHost());
    props.put(ClickHouseSinkConfig.PROXY_PORT, String.valueOf(toxiproxy.getMappedPort(8666)));
    return props;
}
```
This already reads from the field it needs — once `db` is renamed to `proxyDb` in these two files, every `db.getUsername()`/`db.getPassword()` reference inside this method (and inside each `@Test`) needs the matching rename; both classes' test bodies (`arrayTypesTest`, `mapTypesTest`, etc. — 13 `@Test` methods in the WithSchema variant, confirmed by reading the full 375-line file) call `getTestProperties()` uniformly, so the rename is mechanical and localized to `@BeforeAll`/`@AfterAll`/this method.

**Error-handling / assertion style already established in this file to preserve** (`ClickHouseSinkTaskWithSchemaProxyTest.java:202-221`, `detectUnsupportedDataConversions`):
```java
ClickHouseSinkTask chst = new ClickHouseSinkTask();
chst.start(props);
try {
    chst.put(sr);
} catch (RuntimeException e) {
    assertTrue(Utils.getRootCause(e) instanceof DataException, "Did not detect wrong date conversion ");
}
chst.stop();
```
Note this file's own tests do NOT wrap `start()`/`stop()` in try/finally today (D-09 explicitly does not retrofit existing upstream test files) — leave this shape as-is; only the container-field rename is in scope here.

---

### `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` (utility, file-I/O)

**Analog:** itself — full 331-line file read this session.

**Change 1 — default image tag** (`ClickHouseTestHelpers.java:41`):
```java
public static final String CLICKHOUSE_VERSION_DEFAULT = "25.3";
```
→ `"latest"` (D-05). `getClickhouseVersion()` (`ClickHouseTestHelpers.java:53-59`) already honors the `CLICKHOUSE_VERSION` env override unchanged — no other edit needed in that method.

**Change 2 — explicit UTF-8, two call sites, both confirmed this session:**

`getAllRowsAsJson` (`ClickHouseTestHelpers.java:151-172`), the exact line to fix is 158:
```java
BufferedReader reader = new BufferedReader(new InputStreamReader(queryResponse.getInputStream()));
```
→ `new InputStreamReader(queryResponse.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)`.

`validateRows` (`ClickHouseTestHelpers.java:226-274`), the exact line to fix is 252 (identical pattern, same fix):
```java
BufferedReader reader = new BufferedReader(new InputStreamReader(queryResponse.getInputStream()));
```
Add `import java.nio.charset.StandardCharsets;` to the existing import block (`ClickHouseTestHelpers.java:21-37`, which already groups `java.io.*` imports at lines 21-24) rather than fully-qualifying inline, to match this file's own import style (no inline FQNs elsewhere in the file body).

**Container image field to add alongside the existing ones** (`ClickHouseTestHelpers.java:43-44`, unchanged, shown for context — `CLICKHOUSE_FOR_PROXY_DOCKER_IMAGE` already exists and stays pinned to `CLICKHOUSE_PROXY_VERSION_DEFAULT = "23.8"` per D-07, no change needed here):
```java
public static final String CLICKHOUSE_DOCKER_IMAGE = String.format("clickhouse/clickhouse-server:%s", getClickhouseVersion());
public static final String CLICKHOUSE_FOR_PROXY_DOCKER_IMAGE = String.format("clickhouse/clickhouse-server:%s", CLICKHOUSE_PROXY_VERSION_DEFAULT);
```

---

### `<new version-smoke-test class>` (test, request-response, NEW)

**Analog (class shell):** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskTest.java` — class declaration and `@Test` shape.

**Class shell pattern** (`ClickHouseSinkTaskTest.java:28`, plus the file's own imports at the top):
```java
public class ClickHouseSinkTaskTest extends ClickHouseBase {
    ...
    @Test
    public void simplifiedBatchingSchemaless() {
        Map<String, String> props = createProps();
        ClickHouseHelperClient chc = createClient(props);
        ...
    }
}
```
New smoke test class should follow this exact shell: `extends ClickHouseBase`, one `@Test` method, `Map<String, String> props = createProps(); ClickHouseHelperClient chc = createClient(props);` to get a live client against whatever container the base class started.

**Version API to call — do not hand-roll a `SELECT version()` query; it already exists** (`src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:202-208`):
```java
public String version() {
    if (useClientV2) {
        return versionV2();
    } else {
        return versionV1();
    }
}
```
Call `chc.version()` directly — it already dispatches on the client version the test's `CLIENT_VERSION` env/prop selected, matching this phase's "client V2 only" constraint without extra branching in the new test.

**Assertion shape (from RESEARCH.md's Code Examples section, adjusted for the confirmed absence of an image version label — Pitfall 2):** for a numeric `CLICKHOUSE_VERSION`, assert `serverVersion.startsWith(requested)`; for `latest`/unset, assert the string is well-formed (`^\d+\.\d+\.\d+\.\d+$`) and `LOGGER.info` it so the baseline summary can record the actual pulled version — there is no existing in-repo assertion of this shape to copy verbatim (RESEARCH.md confirms no ClickHouse image version label exists to compare against), so this is the one genuinely new assertion in the phase; base it on `getClickhouseVersion()` (`ClickHouseTestHelpers.java:53-59`) for the "requested" side of the comparison.

---

### `<deliberate-failure MBean test>` (test, event-driven, NEW)

**Analog (anti-pattern to prove the sweep fixes):** `src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java:71-73` (confirmed via grep this session, no try/finally around the three calls):
```java
sinkTask.start(props);
...
sinkTask.stop();
```
(`put()` call sits between them in the actual file — the point is there's no `finally` guaranteeing `stop()` on a mid-test exception.)

**Analog (correct try/finally + unique name shape to model the new test's "control" assertion on):** `ClickHouseSinkTaskTest.java:149-153` shows the un-guarded-but-matched start/stop pair to build from; wrap it in `try { ... } finally { chst.stop(); }` and pass a `props.put("name", uniqueConnectorName(testInfo))` (per the new D-10 helper in `ClickHouseBase`) so the deliberate-failure test can assert, after intentionally throwing between `start()` and `put()`, that `ManagementFactory.getPlatformMBeanServer().queryNames(new ObjectName("com.clickhouse:type=ClickHouseKafkaConnector,connector=" + uniqueName + ",*"), null)` is empty post-`@AfterEach`.

**MBean-name construction to assert against** — reuse the exact format string from `ProxySinkTask.getMBeanName()` (`ProxySinkTask.java:78`, already quoted above under `ClickHouseBase.java`'s pattern block) rather than re-deriving it.

---

### `build.gradle.kts` (config, batch)

**Analog:** itself — full 292-line file read this session; no `jacoco` plugin, no `JacocoReport` task, no `testUnit` task exist today (confirmed via grep — zero hits for any of those terms before this phase).

**The trap to fix in the existing loop** (`build.gradle.kts:171-175`):
```kotlin
tasks.withType<Test> {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    tasks.getByName("check").dependsOn(this)
    systemProperty("file.encoding", "windows-1252") // run tests with different encoding
    useJUnitPlatform()
    ...
}
```
Guard line 173 with `if (name != "testUnit") { tasks.getByName("check").dependsOn(this) }` (D-16/Pitfall 5) — this is a real, confirmed-in-source trap, not a hypothetical: `tasks.withType<Test>` is a lazy "all current and future `Test` tasks" configuration block, so registering `testUnit` after this block still gets swept in unless guarded here.

**`integrationTest` registration to model `testUnit`'s registration on** (`build.gradle.kts:151-165`):
```kotlin
sourceSets.create("integrationTest") {
    java.srcDir("src/integrationTest/java")
    compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
    runtimeClasspath += output + compileClasspath + sourceSets["test"].runtimeClasspath
}
tasks.create("integrationTest", Test::class.java) {
    description = "Runs the integration tests"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    outputs.upToDateWhen { false }
    dependsOn("prepareConfluentArchive")
    mustRunAfter("test")
    systemProperties = System.getProperties() as Map<String, Any>
}
```
`testUnit`, unlike `integrationTest`, reuses the **existing** `test` source set (`sourceSets["test"]`) rather than creating a new one — it's a `useJUnitPlatform { excludeTags("clickhouse") }` filter over the same classes, per RESEARCH.md's Pattern 2. Use `tasks.register<Test>("testUnit") { ... }` (Gradle's lazy API, matching this project's `tasks.register<ShadowJar>("confluentJar")` idiom at line 227) rather than `tasks.create(...)` (the older eager API this file already uses inconsistently at line 156) — prefer `register` since it's registered after the `withType<Test>` block, per Gradle's documented "actions apply in registration order" behavior RESEARCH.md verified.

**Where to place the new `jacoco {}` / `JacocoReport` blocks:** immediately after the `tasks.withType<Test> { ... }` block (ends line 216) and before the `ShadowJar` section (starts line 218) — keeps all `Test`-task-adjacent config contiguous, matching this file's existing section-by-section grouping (imports → plugins → `java`/`buildscript` → dependencies → `integrationTest` → `tasks.withType<Test>` → `ShadowJar` → Confluent archive → `spotless`).

**`spotless` block to leave untouched, confirmed present** (`build.gradle.kts:280-286`):
```kotlin
spotless {
    java {
        importOrder()
        removeUnusedImports()
        // googleJavaFormat()
    }
}
```
No interaction with JaCoCo/`testUnit` — listed here only to confirm it does not need touching.

---

### `lombok.config` (config, NEW)

**No in-repo analog** — this is the first Lombok config file in the project (confirmed: `test -f lombok.config` → absent this session). Content per D-15, taken directly from CONTEXT.md's locked decision text (not from any existing file):
```
config.stopBubbling = true
lombok.addLombokGeneratedAnnotation = true
```
Existing Lombok usage to verify the effect against post-change (`src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java:17-26`):
```java
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
...
@Builder
@Getter
public class Column {
```
D-15's own verification step: open the generated `jacocoTestReport` HTML for `Column` and confirm `@Builder`/`@Getter`-generated methods are excluded from coverage counting. Note Lombok `1.18.42` (`build.gradle.kts:93-94`) already defaults `addLombokGeneratedAnnotation=true` since `1.18.34`, so this file mainly pins `config.stopBubbling=true` and documents intent — the report inspection, not the file's presence, is the real gate (RESEARCH.md's "New finding").

## Shared Patterns

### Container singleton + per-class database lifecycle
**Source:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java:27-53` (current, to be modified)
**Apply to:** `ClickHouseBase.java` itself only — all 14 subclasses inherit the fix automatically; no other file needs this pattern copied into it.
```java
// current — no db-null guard, database name guarded (backwards from what's needed)
@BeforeAll
public static void setup() throws IOException  {
    if (database == null) {
        database = String.format("kafka_connect_test_%s", System.currentTimeMillis());
    }
    if (isCloud) { initialPing(); return; }
    db = new ClickHouseContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE).withPassword("test_password").withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1");
    db.start();
}
```

### MBean-safe start/stop lifecycle + unique connector naming
**Source:** `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:71-83` (MBean name format, unregister-on-stop), `src/main/java/com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.java:17-41` (register/unregister semantics), `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java:30-42` (`connectorName = props.get("name")`)
**Apply to:** the new/touched files that call `start()`/`put()`/`stop()` directly — `ClickHouseSinkTaskWithSchemaProxyTest.java`, `ClickHouseSinkTaskSchemalessProxyTest.java` (existing calls, left as-is per D-09 since they're upstream files not being retrofitted beyond the container-field fix), and the new deliberate-failure test (must use try/finally + `uniqueConnectorName(testInfo)`).
```java
// ClickHouseSinkTask.java:36 — the literal props key the helper must write to
connectorName = props.get("name");
```

### `@Tag`/`testUnit` Docker exclusion
**Source:** `build.gradle.kts:171-216` (existing `Test`-task config block to extend), plus JUnit's `@Inherited` `@Tag` semantics (no in-repo file demonstrates `@Tag` yet — this is a net-new annotation on `ClickHouseBase`)
**Apply to:** `ClickHouseBase.java` (the one `@Tag("clickhouse")` placement) and `build.gradle.kts` (the `testUnit` task registration + `check`-loop guard). No individual subclass file needs editing for this.

### Explicit UTF-8 decoding
**Source:** `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java:158,252`
**Apply to:** `ClickHouseTestHelpers.java` only — main-code readers (`ClickHouseHelperClient.java:370,417`) are explicitly out of scope this phase (D-11, deferred to Phase 3).

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `lombok.config` | config | — | First Lombok config file in the project; content is fixed by D-15's locked decision text, not derived from any existing file. |
| Version smoke-test's well-formed-version assertion | test (assertion body only) | request-response | No existing test in this codebase asserts a version-string shape; RESEARCH.md confirmed no ClickHouse image label exists to compare against instead, so this exact assertion has no precedent to copy — see Pattern Assignments above for the closest constructed shape. |
| `jacocoForkReport` task (D-14) | config | batch | No JaCoCo task of any kind exists in `build.gradle.kts` today (grep confirmed zero `jacoco` hits pre-phase) — this is a from-scratch Gradle task, built from RESEARCH.md's Code Examples section (verified against the plugin's documented API, not against any in-repo precedent). |

## Metadata

**Analog search scope:** `build.gradle.kts`; `src/test/java/com/clickhouse/kafka/connect/sink/` (`ClickHouseBase.java`, both proxy test classes, `ClickHouseSinkTaskTest.java`, `JsonColumnTest.java`, `helper/ClickHouseTestHelpers.java`, `junit/extension/FromVersionConditionExtension.java`); `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java`, `ClickHouseSinkTask.java`, `db/helper/ClickHouseHelperClient.java`, `db/mapping/Column.java`; `src/main/java/com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.java`; `gradle/wrapper/gradle-wrapper.properties`.
**Files scanned:** 13 read in full or targeted-range this session (all confirmed git-tracked via `git ls-files`), plus `grep -rln "extends ClickHouseBase" src/test/java` (14 hits) and `grep -n "jacoco\|testUnit\|withType<Test>" build.gradle.kts` for gap-confirmation.
**Pattern extraction date:** 2026-09-15
