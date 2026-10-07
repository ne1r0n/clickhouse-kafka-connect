# Phase 8: Orchestration & JMX Re-plumbing - Pattern Map

**Mapped:** 2026-09-19
**Files analyzed:** 8 files to modify (2 read-only verification targets, 1 checklist doc)
**Analogs found:** 8 / 8 — every analog is either the file's own pre-merge (fork) history or a sibling file already on disk. No external/off-tree pattern needed.

**Note on analog nature:** This phase is a re-plumb, not new-feature work — almost every file being touched is itself the best analog for its own edit, because the *target* shape already existed in this repository's history before the `v1.6.0` merge deleted it (tag `pre-v1.6.0-sync`, present locally). Where the target shape is new (the constructor-arg + static-pure-function split in `SinkTaskStatistics`), the closest analog is the deleted `ProxySinkTask.buildMBeanName` carrier at that same tag, which the CONTEXT.md decisions (D-01/D-03/D-06) explicitly restore under new ownership.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|-----------------|---------------|
| `src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java` | service (JMX metrics/registration) | event-driven (MBean lifecycle) | `pre-v1.6.0-sync:src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` (deleted `buildMBeanName`/`getMBeanName`) | exact (pre-merge original of the method being restored, relocated per D-01) |
| `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` | controller/orchestrator | request-response | `pre-v1.6.0-sync:.../ProxySinkTask.java` (3-arg constructor) | exact (same file, pre-merge revision) |
| `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java` | controller (Kafka Connect `SinkTask`) | request-response | `pre-v1.6.0-sync:.../ClickHouseSinkTask.java` (`props.get("name")` threading) | exact (same file, pre-merge revision) |
| `build.gradle.kts` (test exclude block, ~line 448) | config | batch (build config) | itself — the adjacent, still-present `exclude("**/ClickHouseTestHarnessTest.java")` line at `:450` is the sibling entry this deletion leaves intact | exact (in-file sibling) |
| `src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java` | test | request-response (pure-function unit pins) | itself — file already on disk, already written against the target contract, currently pointing at the wrong carrier class | exact (mechanical rename target is fully specified by the file's own current content) |
| `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskTest.java` (`:313,334,392`) | test | request-response | `src/test/java/com/clickhouse/kafka/connect/sink/dlq/FailureTest.java:75` (same call-site shape, smaller blast radius) | exact (role+data-flow identical, same static being replaced) |
| `src/test/java/com/clickhouse/kafka/connect/sink/dlq/FailureTest.java` (`:75`) | test | request-response | `ClickHouseSinkTaskTest.java:313` (identical call-site shape) | exact |
| `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` (`:140,158,318`) | utility (test fixture, file-I/O) | file-I/O (charset-sensitive stream read) | itself — three occurrences of the identical unguarded `InputStreamReader` pattern in the same file | exact (self-referential, fix all three from one template) |
| `src/main/java/com/clickhouse/kafka/connect/util/Utils.java` | utility (error classification) | transform | **read-only** — no edit, see "No Analog Found / Verification-Only" below | n/a |
| `src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java` | service (state machine) | event-driven | **read-only** — no edit, verified via grep per D-10 | n/a |
| `.planning/MERGE-CHECKLIST.md` | config/doc (row flips + Recorded Facts) | batch | itself — existing "Recorded Facts" numbered-list format (items 1-9 already on disk) | exact (in-file format to continue) |

## Pattern Assignments

### `src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java` (service, event-driven)

**Analog:** the file's own current content (target: add `connectorName` field/ctor arg per D-01) **plus** `pre-v1.6.0-sync:src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` (the deleted carrier method whose exact contract `MBeanNameUnitTest` still pins).

**Current constructor and static-name pattern to replace** (`SinkTaskStatistics.java:34-48,162-168`):
```java
private final int taskId;
// ...
public SinkTaskStatistics(int taskId) {
    this.taskId = taskId;
    this.topicMBeans = new ConcurrentLinkedDeque<>();
    this.topicStatistics = new ConcurrentHashMap<>();
    // ...
}

public static String getMBeanName(int taskId) {
    return String.format("com.clickhouse:type=ClickHouseKafkaConnector,name=SinkTask%d,version=%s", taskId, Version.ARTIFACT_VERSION);
}

public static String getTopicMBeanName(int taskId, String topic) {
    return String.format("com.clickhouse:type=ClickHouseKafkaConnector,name=SinkTask%d,version=%s,topic=%s", taskId, Version.ARTIFACT_VERSION, topic);
}
```

**Pre-merge carrier being restored, relocated** (`pre-v1.6.0-sync:ProxySinkTask.java:75-88`, this is the exact null/colon-sanitization contract `MBeanNameUnitTest` pins byte-for-byte — do not alter the format string or the branch order):
```java
static String buildMBeanName(String connectorName, int id, String version) {
    if (version != null) {
        version = version.replace(':', '_');
    } else {
        version = "unknown";
    }
    return String.format("com.clickhouse:type=ClickHouseKafkaConnector,connector=%s,name=SinkTask%d,version=%s", connectorName, id, version);
}

private String getMBeanName() {
    return buildMBeanName(connectorName, id, ClickHouseClientOption.class.getPackage().getImplementationVersion());
}
```
Per D-06, the version source changes to `Version.ARTIFACT_VERSION` (already imported in `SinkTaskStatistics.java:4`) instead of the client-jar manifest read shown above — everything else about the pre-merge shape is the target.

**Call-site update pattern** — the three internal callers of the old statics (`registerMBean`, `unregisterMBean`, `createTopicStatistics`) at `SinkTaskStatistics.java:50-64,154-160` become instance-method calls (`getMBeanName()`/`getTopicMBeanName(topic)`) with no other logic change — the `LOGGER.info("Register MBean [{}]", name)` / `LOGGER.info("Unregister topic MBean [{}]", topicMBean)` parameterized-logging pattern stays exactly as-is (see Shared Patterns → Logging below).

**D-08 composition pattern** (new, not previously in either version — `getTopicMBeanName` must call `getMBeanName()` and append, not duplicate the format string):
```java
public String getTopicMBeanName(String topic) {
    return getMBeanName() + ",topic=" + topic;
}
```

---

### `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` (controller, request-response)

**Analog:** `pre-v1.6.0-sync:src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` (its own pre-merge revision).

**Current 2-arg constructor to extend** (`ProxySinkTask.java:43-55`):
```java
public ProxySinkTask(final ClickHouseSinkConfig clickHouseSinkConfig, final ErrorReporter errorReporter) {
    this.clickHouseSinkConfig = clickHouseSinkConfig;
    this.errorReporter = errorReporter;
    LOGGER.info("Enable ExactlyOnce? {}", clickHouseSinkConfig.isExactlyOnce());
    if ( clickHouseSinkConfig.isExactlyOnce() ) {
        this.stateProvider = new KeeperStateProvider(clickHouseSinkConfig);
    } else {
        this.stateProvider = new InMemoryState();
    }
    this.statistics = new SinkTaskStatistics(id);
    this.dbWriter = new ClickHouseWriter(this.statistics);
    processing = new Processing(stateProvider, dbWriter, errorReporter, clickHouseSinkConfig, statistics);
}
```

**Pre-merge 3-arg shape being restored** (`pre-v1.6.0-sync:ProxySinkTask.java:39-46`, param name and field to add — note pre-merge stored `connectorName` as a field for `getMBeanName()`; in the target shape it is passed straight through to `new SinkTaskStatistics(connectorName, id)` and does **not** need to become a field on `ProxySinkTask` itself per D-01/D-03):
```java
private final String connectorName;
// ...
public ProxySinkTask(final ClickHouseSinkConfig clickHouseSinkConfig, final ErrorReporter errorReporter, final String connectorName) {
    this.connectorName = connectorName;
    this.clickHouseSinkConfig = clickHouseSinkConfig;
    // ...
}
```
Target edit: add the third parameter, thread it into `new SinkTaskStatistics(connectorName, id)` at the current `:52` call site. `new Processing(stateProvider, dbWriter, errorReporter, clickHouseSinkConfig, statistics)` at `:54` stays 5-arg, untouched (D-10 — verified byte-identical to upstream, no edit expected beyond the constructor signature change above it).

---

### `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java` (controller, request-response)

**Analog:** `pre-v1.6.0-sync:src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java` (its own pre-merge revision).

**Current call site to extend** (`ClickHouseSinkTask.java:34-51`, `start()`):
```java
@Override
public void start(Map<String, String> props) {
    LOGGER.info("Start SinkTask: ");
    try {
        clickHouseSinkConfig = new ClickHouseSinkConfig(props);
        if (errorReporter == null) {
            errorReporter = createErrorReporter();
        }
    } catch (Exception e) {
        throw new ConnectException("Failed to start new task" , e);
    }

    validateBufferConfig(clickHouseSinkConfig);

    this.proxySinkTask = new ProxySinkTask(clickHouseSinkConfig, errorReporter);
    this.proxySinkTask.start();

    this.deliveryStrategy = createDeliveryStrategy(clickHouseSinkConfig, proxySinkTask, errorReporter);
}
```

**Pre-merge pattern being restored** (`pre-v1.6.0-sync:ClickHouseSinkTask.java:29-41`, the raw un-guarded `props.get("name")` read — D-03 is explicit that this stays a raw `get`, no null-check, no default):
```java
@Override
public void start(Map<String, String> props) {
    LOGGER.info("Start SinkTask: ");
    String connectorName;
    try {
        clickHouseSinkConfig = new ClickHouseSinkConfig(props);
        errorReporter = createErrorReporter();
        connectorName = props.get("name");
    } catch (Exception e) {
        throw new ConnectException("Failed to start new task" , e);
    }

    this.proxySinkTask = new ProxySinkTask(clickHouseSinkConfig, errorReporter, connectorName);
}
```
Target edit: add `String connectorName = props.get("name");` inside the existing `try` block (alongside, not replacing, the current `errorReporter == null` guard which the pre-merge version lacks but the current tree needs — keep that guard, it is unrelated Phase-6/7 behavior) and pass `connectorName` as the third constructor arg at the current `:47` call site. `validateBufferConfig`, `createDeliveryStrategy`, and everything else in this method is untouched (out of scope, belongs to Phase 7/upstream's delivery-strategy layering).

---

### `build.gradle.kts` (config, batch)

**Analog:** the in-file sibling exclude entry that stays.

**Current block** (`build.gradle.kts:436-448`, read this session):
```kotlin
test {
    java {
        // Phase 8 (JMX-01/02): ProxySinkTask.buildMBeanName
        exclude("**/MBeanNameUnitTest.java")
        // Phase 10 (HARN-04): ClickHouseBase shared-container contract
        exclude("**/ClickHouseTestHarnessTest.java")
    }
}
```
**Target edit (D-05):** delete the `exclude("**/MBeanNameUnitTest.java")` line and its preceding comment line. Leave the `ClickHouseTestHarnessTest.java` exclude and its comment untouched — it is Phase 10's. Verify afterward with `grep -c "exclude(" build.gradle.kts` inside the `test { java { ... } }` block returns 1, not 0 or 2.

---

### `src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java` (test, request-response)

**Analog:** itself — the file already encodes the exact target contract; only the carrier class name needs changing (D-04's "mechanical carrier swap").

**Current content, every call site** (`MBeanNameUnitTest.java:12,27,38,46,54,62`, full file read this session — 74 lines, all shown in this excerpt via the earlier read):
```java
/**
 * Docker-free unit pins for {@link ProxySinkTask#buildMBeanName(String, int, String)} - JMX-01, ...
 */
class MBeanNameUnitTest {
    @Test
    void connectorNamePresentRendersFullMBeanName() {
        assertEquals(
                "com.clickhouse:type=ClickHouseKafkaConnector,connector=my-connector,name=SinkTask3,version=0.9.6",
                ProxySinkTask.buildMBeanName("my-connector", 3, "0.9.6"));
    }
    // ...4 more call sites, identical shape, calling ProxySinkTask.buildMBeanName
}
```
**Target edit (commit 1, D-04):** change every `ProxySinkTask.buildMBeanName` to `SinkTaskStatistics.buildMBeanName` — the Javadoc `@link` at line 12 and the five call sites at 27/38/46/54/62. Add `import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;`. **Zero string literal changes** — every `assertEquals`/`assertTrue` expected value stays byte-identical, including the `0.9.6` version literals (D-04 explicitly rejects computing expectations from `Version.ARTIFACT_VERSION`).

**Target edit (commit 2, D-09 — new topic-name cases, composed per D-08):** add Docker-free whole-string assertions for `SinkTaskStatistics.getTopicMBeanName(topic)`'s composition, following the same `assertEquals(expected, actual)` style as the existing five tests — e.g. asserting that a topic-name call equals `getMBeanName() + ",topic=" + topic` for representative inputs. Since `getTopicMBeanName` is an *instance* method (D-01), these new cases need a `SinkTaskStatistics` instance (constructed with a known `connectorName`/`taskId`) rather than calling the static directly — follow this file's existing `assertEquals(literal, actual)` idiom, not a `new` `assertTrue` idiom.

---

### `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskTest.java` (test, request-response) and `src/test/java/com/clickhouse/kafka/connect/sink/dlq/FailureTest.java` (test, request-response)

**Analog:** each other — identical call-site shape, both calling the deleted connector-blind static.

**Current pattern, `FailureTest.java:74-78`:**
```java
final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
final String mbeanName = SinkTaskStatistics.getMBeanName(task.taskId());
ObjectName sinkMBean = new ObjectName(mbeanName);
Object sentToDQL = mBeanServer.getAttribute(sinkMBean, "MessagesSentToDLQ");
```

**Current pattern, `ClickHouseSinkTaskTest.java:312-313` (sink-task name), `:334` (topic name), `:392` (sink-task name):**
```java
final String mbeanName = SinkTaskStatistics.getMBeanName(taskId);
ObjectName sinkMBean = new ObjectName(mbeanName);
// ...
final ObjectName topicMbeanName = new ObjectName(SinkTaskStatistics.getTopicMBeanName(taskId, topic));
// ...
ObjectName topicMbeanName = new ObjectName(SinkTaskStatistics.getMBeanName(taskId));
```

**Target edit (D-02, per research's Pattern 2 — recommend Option A, the leaner static-only shape, consistent with the milestone's fork-delta-shrinking goal; a planner may choose Option B instead, see RESEARCH.md § Architecture Patterns → Pattern 2 Open Question):**
```java
// sink-task name, all four call sites needing the task-level name:
SinkTaskStatistics.buildMBeanName(null, taskId, Version.ARTIFACT_VERSION)

// topic name (ClickHouseSinkTaskTest.java:334 only):
SinkTaskStatistics.buildMBeanName(null, taskId, Version.ARTIFACT_VERSION) + ",topic=" + topic
```
Both test files need neither of these tasks to have set a `name` prop — `null` is exactly what a real task would register given today's config. Add `import com.clickhouse.kafka.connect.sink.Version;` to both files if not already present (verify via `grep -n "^import.*Version" <file>` at execution time).

**Bookkeeping, not code:** annotate (do not flip) the `ClickHouseSinkTaskTest.java` row in `.planning/MERGE-CHECKLIST.md` § Section 1 with a note recording this four-site edit and its commit hash — the row itself stays `pending`, owned by Phase 10 (D-02's explicit instruction). `FailureTest.java` has no checklist row at all; no bookkeeping needed there.

---

### `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` (utility, file-I/O)

**Analog:** itself — three occurrences of the identical unguarded pattern in the same file (`getAllRowsAsJson` at `:140`, `getAllRowsAsJsonCloud` at `:158`, and a third at `:318` inside the Debezium/mismatch-diagnostics helper).

**Current pattern, all three occurrences, byte-for-byte identical** (verified via grep this session — exactly 3 matches):
```java
BufferedReader reader = new BufferedReader(new InputStreamReader(queryResponse.getInputStream()));
```

**Target edit (D-13), apply at all three line numbers:**
```java
BufferedReader reader = new BufferedReader(
        new InputStreamReader(queryResponse.getInputStream(), StandardCharsets.UTF_8));
```
Requires adding `import java.nio.charset.StandardCharsets;` — **confirmed absent** from the file's current import block (`grep -n "^import" ClickHouseTestHelpers.java` shows `java.io.InputStreamReader` at line 24 but no `java.nio.charset.StandardCharsets` import anywhere in the file, checked this session). Add it alphabetically among the `java.*` imports per the project's `spotless { importOrder() }` convention (see AGENTS.md § Import Organization). After editing, grep for `new InputStreamReader(` with no `StandardCharsets.UTF_8` argument — must return zero matches (Pitfall 5 in RESEARCH.md).

**Bookkeeping, not code:** annotate (do not flip) the Phase-10/HARN-04 checklist row for this file in `.planning/MERGE-CHECKLIST.md` § Section 1, recording the early landing and its commit — same pattern as D-02's annotation above and Phase 7's D-03/D-07 precedent.

---

## No Analog Found / Verification-Only (ORCH-01, ORCH-04)

These two files are explicitly **read-only** this phase — the requirement is evidence (grep output + a stated confirmation in the plan/SUMMARY), not a code pattern:

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `src/main/java/com/clickhouse/kafka/connect/util/Utils.java` | utility (error classification) | transform | Byte-identical to upstream except a Spotless import reorder (29-line diff, confirmed import-only). FREE-01's evidence is the already-live code at `:60-62` (`isServerError`) and `:77-80` (`handleException`'s ternary) — quote these verbatim in the plan's verification step, do not edit them. See excerpt below. |
| `src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java` | service (state machine) | event-driven | Byte-identical to `v1.6.0` (0-line diff). ORCH-01's evidence is `grep -rn "new Processing(" src/ --include=*.java` returning exactly 16 call sites, all 5-arg (1 in `ProxySinkTask.java:54`, 15 in `ProcessingTest.java`) — this grep, run and its output pasted into the plan/SUMMARY, is the deliverable, not an edit to this file. |

**`Utils.java` FREE-01 verification excerpt** (`Utils.java:60-62,77-80`, quote verbatim in the plan's verification task, do not modify):
```java
private static boolean isServerError(Throwable t) {
    return t instanceof ClickHouseException || t instanceof ServerException;
}
// ...
if (isServerError(rootCause)) {
    int errorCode = rootCause instanceof ServerException
            ? ((ServerException) rootCause).getCode()
            : ((ClickHouseException) rootCause).getErrorCode();
```

## Shared Patterns

### Parameterized SLF4J logging around MBean lifecycle
**Source:** `SinkTaskStatistics.java:52,58,61,157` (existing, unchanged by this phase)
**Apply to:** any new logging in `SinkTaskStatistics`'s constructor/registration path (there should be none needed beyond what already exists)
```java
LOGGER.info("Register MBean [{}]", name);
LOGGER.info("Unregister MBean [{}]", name);
LOGGER.info("Unregister topic MBean [{}]", topicMBean);
LOGGER.info("Register topic MBean [{}]", topicMBeanName);
```
Per AGENTS.md § Logging: `LOGGER.info` for lifecycle events (registration/unregistration counts as lifecycle) — this convention is already followed in every touched call site; no new log level decisions needed this phase.

### `MBeanServerUtils` — untouched wrapper, not a pattern to copy but a contract to preserve
**Source:** `src/main/java/com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.java` (42 lines, read in full this session)
**Apply to:** every `SinkTaskStatistics` call site that registers/unregisters — the rethrow-on-`InstanceAlreadyExistsException` / swallow-with-WARN-on-`MalformedObjectNameException` behavior stays exactly as-is (explicit Claude's Discretion item in CONTEXT.md: "`MBeanServerUtils` is not touched"). Do not add a try/catch around `MBeanServerUtils.registerMBean(...)`/`unregisterMBean(...)` calls in `SinkTaskStatistics` — none exists today and none should be added.

### Static pure-function name builder + instance convenience methods (D-01)
**Source:** `pre-v1.6.0-sync:src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:75-88` (the deleted carrier, shown in full above)
**Apply to:** `SinkTaskStatistics.java` only — this is the one production pattern this phase actually writes. The shape is: a `static String buildMBeanName(String connectorName, int taskId, String version)` pure function with no field reads, plus two instance methods (`getMBeanName()`, `getTopicMBeanName(topic)`) that close over the instance's own `connectorName`/`taskId`/`Version.ARTIFACT_VERSION` and delegate to it. D-08 additionally requires `getTopicMBeanName` to be built from `getMBeanName()` by string concatenation (`,topic=` + topic), never a second format string.

### Test call-site migration off a deleted connector-blind static (D-02)
**Source:** `FailureTest.java:75` and `ClickHouseSinkTaskTest.java:313,334,392` — four call sites, one shared shape
**Apply to:** all four sites identically — replace `SinkTaskStatistics.getMBeanName(taskId)` / `SinkTaskStatistics.getTopicMBeanName(taskId, topic)` with `SinkTaskStatistics.buildMBeanName(null, taskId, Version.ARTIFACT_VERSION)` (+ `,topic=" + topic` for the one topic-name call at `:334`). Same literal `null` at every site — neither test file ever sets a `name` prop, so this is not a guess, it is what those tasks already register today.

## Metadata

**Analog search scope:** `src/main/java/com/clickhouse/kafka/connect/{sink,sink/processing,util,util/jmx}/`, `src/test/java/com/clickhouse/kafka/connect/sink/{,dlq}/`, `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/`, `build.gradle.kts`, `.planning/MERGE-CHECKLIST.md`, plus git history at tag `pre-v1.6.0-sync` for the three production files whose target shape existed pre-merge.
**Files scanned:** 11 (8 edited/verified this session's required reading + read tool calls, 3 read via `git show <tag>:<path>` for historical analog)
**Pattern extraction date:** 2026-09-19
**Tracked-source gate:** all 11 analog/target paths confirmed via `git ls-files` this session — none are gitignored mirrors; the three historical analogs are read via `git show pre-v1.6.0-sync:<path>`, which is a tracked tag on the same repository, not a filesystem mirror.
