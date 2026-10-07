# Phase 8: Orchestration & JMX Re-plumbing - Research

**Researched:** 2026-09-19
**Domain:** JMX MBean naming re-plumb in a Kafka Connect sink connector (single-repo, no external library research needed)
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**JMX Name Carrier**
- **D-01:** Connector-aware naming lives in `SinkTaskStatistics`. It takes `connectorName` as a constructor argument and holds it as a field alongside the existing `taskId`; `getMBeanName()` and `getTopicMBeanName(topic)` become instance methods. A static `buildMBeanName(String connectorName, int taskId, String version)` is the pure function that holds the actual format string, the `null → "unknown"` version default and the `: → _` sanitization — this is what `MBeanNameUnitTest` re-targets at.
  - Rejected: keeping everything static with `connectorName` as a leading parameter.
  - Rejected: re-adding the carrier to `ProxySinkTask` and passing a finished name string down.
- **D-02:** The connector-blind static `getMBeanName(int taskId)` is deleted, and all four upstream call sites are updated to pass `null` explicitly: `ClickHouseSinkTaskTest.java:313` (sink-task name), `:334` (topic name, via `getTopicMBeanName`), `:392` (sink-task name); `FailureTest.java:75` (sink-task name). Neither file sets a `name` prop, so `null` is exactly what those tasks register. Both files are byte-identical to `v1.6.0` — `ClickHouseSinkTaskTest.java` is a Phase-10-owned `survives-as-is` checklist row (Phase 8 appends a note, does not flip); `FailureTest.java` is not in the conflict inventory at all.
- **D-03:** `connectorName` reaches `ProxySinkTask` by restoring the fork's pre-merge 3-arg constructor: `ProxySinkTask(ClickHouseSinkConfig, ErrorReporter, String connectorName)`, called from `ClickHouseSinkTask.start()` with a raw `props.get("name")`. Exactly one call site. Absent `name` yields the four-character string `null` in the rendered name (JMX-02); an empty one yields `connector=` followed immediately by `name=`.
  - Rejected: retaining `connectorName` on `ClickHouseSinkConfig`.
  - Rejected: a post-construction `setConnectorName()`.
- **D-04:** `MBeanNameUnitTest` is re-targeted by a mechanical carrier swap only: `ProxySinkTask.buildMBeanName` → `SinkTaskStatistics.buildMBeanName` in the five call sites and the Javadoc `@link`. Every assertion string stays byte-identical, including the `0.9.6` version literals. Version stays an explicit parameter.
  - Rejected: replacing the literals with `Version.ARTIFACT_VERSION`.
- **D-05:** `build.gradle.kts:448`'s `exclude("**/MBeanNameUnitTest.java")` is deleted by this phase. `exclude("**/ClickHouseTestHarnessTest.java")` at `:450` stays (Phase 10's to remove). Verify only one exclude remains afterwards.

**Version Segment**
- **D-06:** The `version=` segment is sourced from `Version.ARTIFACT_VERSION` (currently `v1.6.0_gg1`). The fork's pre-merge read of `ClickHouseClientOption.class.getPackage().getImplementationVersion()` (`0.9.6`, the client jar's manifest) is dropped.
  - **Consequence to carry, not to fix:** `ARTIFACT_VERSION` is a compile-time literal — never `null`, never contains a colon. JMX-03's two branches become unreachable from production while remaining a pinned contract of `buildMBeanName`. They stay; `MBeanNameUnitTest` keeps exercising them through the explicit parameter.
- **D-07:** The operator-visible name change (old `version=0.9.6` → new `version=v1.6.0_gg1`) is recorded as a named fact in `.planning/MERGE-CHECKLIST.md` § Recorded Facts — one row, old string → new string. Rejected: pinning it with a test (couples a test to the build-generated constant).

**Per-topic MBean (ORCH-03)**
- **D-08:** `getTopicMBeanName(topic)` is composed, not duplicated: `buildMBeanName(connectorName, taskId, version) + ",topic=" + topic`. Key order `type,connector,name,version,topic` falls out rather than being chosen.
  ```
  com.clickhouse:type=ClickHouseKafkaConnector,connector=my-connector,name=SinkTask3,version=v1.6.0_gg1
  com.clickhouse:type=ClickHouseKafkaConnector,connector=my-connector,name=SinkTask3,version=v1.6.0_gg1,topic=orders
  ```
  Rejected: a symmetric `buildTopicMBeanName(...)` static (two format strings, drift risk). Rejected: appending `,connector=…` to upstream's existing topic format (sink-task and topic names would disagree on where `connector` sits).
- **D-09:** ORCH-03's evidence is Docker-free whole-string cases added to `MBeanNameUnitTest`, landed in a second commit after D-04's mechanical carrier swap. No live assertion added — D-12's full-suite run already exercises registration through `ClickHouseSinkTaskTest`'s existing topic-MBean case.

**Orchestration, Utils & Processing (ORCH-01, ORCH-04, FREE-01)**
- **D-10:** ORCH-01 is discharged by verification, not code. `ProxySinkTask.java` and `Processing.java` are byte-identical to `v1.6.0`; `DeliveryStrategy`/`ChunkFlusher`/`DirectDeliveryStrategy`/`AtLeastOnceBufferStrategy`/`ExactlyOnceBufferStrategy` are new upstream files with no fork delta; a tree-wide grep for `new Processing(` finds only 5-arg call sites (1 in `src/main`, 15 in `ProcessingTest`). The plan should assert this by grep at execution time. D-03's constructor change is the only edit either file receives.
- **D-11:** ORCH-04 is discharged by reading both files and writing down what was read. `Processing.java` needs no edit. `Utils.java`'s entire 29-line delta against upstream is a Spotless import reorder — confirm and state that. `UtilsTest.java` and `ProcessingTest.java` are already on upstream's versions — confirm by diff, flip both Section 1 rows plus the two Section 2 rows for the production files. FREE-01 is already satisfied in the merged tree — `Utils.isServerError()` is `t instanceof ClickHouseException || t instanceof ServerException` and `handleException` reads the code off whichever matched — closed in `MERGE-CHECKLIST.md § Recorded Facts` citing upstream issue #818. No new test.

**Acceptance Gate**
- **D-12:** The phase closes on `testUnit` green plus a full `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` run, re-baselined pass condition:
  1. Phase 8's own two failures (`MBeanRegistrationLiveTest` JMX-04 ×2, currently NPE) are gone.
  2. Zero new failures relative to the 7-failure baseline recorded at the end of Phase 7.
  3. Every remaining failure is named with its owning phase in the phase SUMMARY.
- **D-13 (Claude's discretion — user said "реши сам"):**
  - The 4 UTF-8 mojibake failures are **fixed here**: three unguarded `new InputStreamReader(stream)` calls in `ClickHouseTestHelpers.java` (verified at lines 140 `getAllRowsAsJson`, 158 `getAllRowsAsJsonCloud`, 318) decode UTF-8 bytes under the test JVM's `file.encoding=windows-1252`. Clears `JsonDynamicRoundTripTest` TYPE-07 ×2 plus `SmtSinkLandingTest` HDRS-09/CONV-08. Annotate (do not flip) the relevant Phase-10/HARN-04 checklist row with the early landing and its commit.
  - `TableTest.extractNullables()` (expected 3, got 2) is triaged, not fixed. `Table.java` is byte-identical to upstream and out of Phase 7's scope — name a cause and an owning phase, write it down.
  - A planner may override either half with a stated reason; both are reversible.

**Commit Shape**
- **D-14:** One fork behaviour per commit, per Phase 7's D-19. Natural sequence: JMX re-plumb (`SinkTaskStatistics` + `ProxySinkTask` + `ClickHouseSinkTask` + the 4 call sites + the `build.gradle.kts` exclude removal + D-04's mechanical test swap) → D-09's topic-name test cases → D-13's UTF-8 fixture fix → the checklist flips and Recorded Facts.
- **D-15:** Run `./gradlew spotlessApply` before each commit, only over the files the commit touches — not tree-wide (an unrelated 52-file reformat is pending since the merge). Verify touched files pass `spotlessCheck` individually instead.

### Claude's Discretion
- D-13's carry-in split (above) — explicitly delegated by the user.
- Wave shape: the JMX re-plumb is one connected edit across four production/test files with a single call site each — does not decompose into parallel work. D-09's topic cases depend on D-08's composition existing; D-13's fixture fix is independent and could run in parallel if the planner wants a second wave. D-12's full-suite run is necessarily last.
- `MBeanServerUtils` is not touched. Its `registerMBean` still rethrows `InstanceAlreadyExistsException` as a `RuntimeException` and still swallows `MalformedObjectNameException` with a WARN. Both behaviours are load-bearing for existing tests and neither is in this phase's requirements.
- No new metric is added. This phase changes only how MBeans are named.

### Deferred Ideas (OUT OF SCOPE)
- ObjectName-hostile connector names (comma, `=`, or quote in `name` → swallowed `MalformedObjectNameException`, silent zero-metrics). Deliberately uncovered since Phase 4's D-05.
- A standing test that the production MBean name uses `ARTIFACT_VERSION` — rejected under D-07 (couples a test to a build-generated constant).
- `TableTest.extractNullables()` — triaged by D-13, fixed by whichever phase owns `Table`/mapping coverage.
- Extending `ClickHouseBase`'s `@AfterEach` MBean leak sweep to per-topic MBeans — Phase 10 / HARN-02.
- `ClickHouseSinkTask.version()` returning hard-coded `"0.0.1"` — unrelated to `Version.ARTIFACT_VERSION`, untouched.
- Fixing CR-01 (`verify-merge-invariants.sh:44` git-grep-error-as-no-match) — must be fixed before Phase 11, not Phase 8 work.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| ORCH-01 | `ProxySinkTask`/`ClickHouseSinkTask` resolved against upstream's `DeliveryStrategy`/`ChunkFlusher` layering, 5-arg `Processing` construction, no stale 4-arg call site | Verified this session: `ProxySinkTask.java` and `Processing.java` are 0-line diff vs `v1.6.0` tag; `DeliveryStrategy.java`/`ChunkFlusher.java` are also 0-line diff (new upstream files, no fork delta); tree-wide grep for `new Processing(` finds exactly 16 call sites, all 5-arg (1 in `ProxySinkTask.java:54`, 15 in `ProcessingTest.java`). See "Verified Starting State" section below. |
| ORCH-02 | JMX ObjectName carries `connector=<name>` again, re-plumbed from `ClickHouseSinkTask.start()` into `SinkTaskStatistics`, colon sanitization retained, `JMX-01..04` green | See "Architecture Patterns" — current `SinkTaskStatistics.java` (read in full this session) has no `connector=` segment and no `connectorName` field; design target given in D-01/D-03/D-04. |
| ORCH-03 | Per-topic MBean gets the same `connector=<name>` treatment | D-08's composition rule; see "Code Examples". |
| ORCH-04 | `Utils.java`/`Processing.java` explicitly reviewed; `UtilsTest`/`ProcessingTest` on upstream's versions | Both files read in full this session — see "Verified Starting State". |
| FREE-01 | `Utils.handleException` confirmed to classify `ServerException` alongside `ClickHouseException`, closed citing upstream issue #818 | Verified this session by reading `Utils.java:60-62,77-80` — quoted verbatim below. |
</phase_requirements>

## Summary

This phase has almost no new code to write. The "measured starting state" table in `08-CONTEXT.md` was re-verified line-for-line this session by reading every named file and running `git diff v1.6.0 -- <path> | wc -l`: `ProxySinkTask.java`, `Processing.java`, `ProcessingTest.java`, and `UtilsTest.java` are all byte-identical (0-line diff) to the upstream `v1.6.0` tag; `Utils.java`'s 29-line delta is confirmed to be an import reorder only (the production logic — `isServerError()` checking both `ClickHouseException` and `ServerException` — is upstream's, unmodified); a tree-wide grep for `new Processing(` finds 16 call sites, every one 5-arg. `DeliveryStrategy.java` and `ChunkFlusher.java` were also independently confirmed to be 0-line diff against `v1.6.0` (new upstream files with zero fork delta). `testUnit` runs green today (306 tests, 27 files) with the JMX test class excluded from compilation.

The actual production work is one connected edit: give `SinkTaskStatistics` a `connectorName` field (via a new 3-arg constructor), extract a static pure-function `buildMBeanName(connectorName, taskId, version)` that owns the format string, turn `getMBeanName()`/`getTopicMBeanName(topic)` into instance methods built on top of it, delete the connector-blind static `getMBeanName(int taskId)`, restore `ProxySinkTask`'s 3-arg constructor, and thread `props.get("name")` through from `ClickHouseSinkTask.start()`. Four test call sites that currently call the deleted static must be updated to call the new static explicitly with `null`. `MBeanNameUnitTest` (currently excluded from test compilation, and currently calling a `ProxySinkTask.buildMBeanName` method that does not exist anywhere in the tree today) needs a mechanical rename to `SinkTaskStatistics.buildMBeanName`, plus new topic-name assertions in a second commit, plus deletion of its `build.gradle.kts` exclude line.

Everything else — the buffering/delivery-strategy layering, the 5-arg `Processing` construction, `Utils`'s error classification — already matches upstream and needs verification-by-reading, not implementation. D-13's UTF-8 fixture fix (three `InputStreamReader` calls, all three exact line numbers re-verified this session at 140/158/318) is a small, independent, mechanical addition to the same phase.

**Primary recommendation:** Treat this phase as one JMX-naming implementation task plus a disciplined verification/recording pass — do not invent extra work for ORCH-01/ORCH-04, the requirement there is evidence (grep output + a stated confirmation), not code.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Connector-name-aware MBean naming | Backend (in-process JMX layer, `util.jmx` package) | Sink-task lifecycle (`ClickHouseSinkTask`/`ProxySinkTask`) | `SinkTaskStatistics` is the JMX registration owner; it must hold the name since it alone calls `MBeanServerUtils.registerMBean`/`unregisterMBean` and creates per-topic MBeans internally (`createTopicStatistics`) |
| Connector name sourcing (`props.get("name")`) | Sink-task lifecycle (`ClickHouseSinkTask.start()`) | — | Only `start()` has the raw Kafka Connect `props` map; `ClickHouseSinkConfig` is a `ConfigDef`-validated object and `name` is a Connect-framework key, not a connector-defined config option (per D-03's rejection of storing it on `ClickHouseSinkConfig`) |
| Delivery/buffering strategy selection | Sink-task lifecycle (`ClickHouseSinkTask`) | Backend (`DeliveryStrategy` implementations) | Already upstream's layering, unmodified by this phase — included for completeness since ORCH-01 verification touches this code path |
| Exactly-once state machine / dedup | Backend (`Processing`, `StateProvider`) | — | Byte-identical to upstream; this phase reads, does not touch |
| Error classification (retriable vs. fatal) | Backend (`Utils.handleException`) | — | Byte-identical to upstream (minus import order); this phase reads, does not touch |
| Test-fixture JSON read-back charset | Test infrastructure (`testFixtures`) | — | D-13's UTF-8 fix — infra-layer, not production behavior |

## Verified Starting State

All rows re-confirmed this session against the `v1.6.0` tag present in this repo (`git tag` shows `v1.6.0` and `pre-v1.6.0-sync`).

| Path | `git diff v1.6.0 -- <path> \| wc -l` | Read in full this session? | Consequence |
|---|---|---|---|
| `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` | **0** [VERIFIED: git diff v1.6.0, this session] | Yes | 2-arg constructor at `:43`; 5-arg `Processing` at `:54`; no `connectorName` field, no `buildMBeanName` method |
| `src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java` | **0** [VERIFIED: git diff v1.6.0, this session] | No (per D-11, this phase reads-not-edits it; content not needed for the JMX work) | ORCH-01's "zero stale 4-arg call sites" confirmed independently by grep |
| `src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java` | **0** [VERIFIED: git diff v1.6.0, this session] | Yes, full 178 lines | Statics `getMBeanName(int)` (`:162-164`) / `getTopicMBeanName(int, String)` (`:166-168`); version from `Version.ARTIFACT_VERSION`; **no `connector=` anywhere in either format string** |
| `src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingTest.java` | **0** [VERIFIED: git diff v1.6.0, this session] | No | Already upstream's version — ORCH-04's test half confirmed |
| `src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsTest.java` | **0** [VERIFIED: git diff v1.6.0, this session] | No | Already upstream's version |
| `src/main/java/com/clickhouse/kafka/connect/util/Utils.java` | **29**, confirmed import-reorder only [VERIFIED: read full file this session] | Yes, full 304 lines | `isServerError()` (`:60-62`): `` `return t instanceof ClickHouseException || t instanceof ServerException;` `` — `handleException` (`:77-80`) reads the error code off whichever matched via a ternary: `` `int errorCode = rootCause instanceof ServerException ? ((ServerException) rootCause).getCode() : ((ClickHouseException) rootCause).getErrorCode();` `` — **FREE-01 confirmed true in the tree today, verbatim** |
| `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java` | **16** | Yes, full 206 lines | Phase 7's `@Override` removal on `onPartitionsRevoked` (`:153`, comment explains why) plus the `DeliveryStrategy` construction machinery; `start()` at `:47` calls `new ProxySinkTask(clickHouseSinkConfig, errorReporter)` — **`props.get("name")` is confirmed absent** from this call |
| `src/main/java/com/clickhouse/kafka/connect/sink/DeliveryStrategy.java` | **0** [VERIFIED: git diff v1.6.0, this session] | No | New upstream file, zero fork delta — confirms half of ORCH-01's "no stale call site" claim |
| `src/main/java/com/clickhouse/kafka/connect/sink/ChunkFlusher.java` | **0** [VERIFIED: git diff v1.6.0, this session] | No | New upstream file, zero fork delta |
| `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java` | **0** [VERIFIED: git diff v1.6.0, this session] | No | Confirms D-13's premise that `TableTest.extractNullables()`'s failure is not a fork regression |

**`new Processing(` call-site grep** [VERIFIED: grep this session, `src/` excluding `build/`]:
```
src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:54:  new Processing(stateProvider, dbWriter, errorReporter, clickHouseSinkConfig, statistics)
src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingTest.java: 15 call sites, all 5-arg
```
Total 16 call sites, 0 four-arg. This is the exact evidence ORCH-01/D-10 asks the plan to assert.

**`testUnit` current status** [VERIFIED: ran `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit -q` this session, Docker not required for this task]: green, 306 tests across 27 XML result files. `--tests "*MBean*"` matches zero tests (`MBeanNameUnitTest` is excluded from the test *compile* source set at `build.gradle.kts` per D-05's premise; `MBeanRegistrationLiveTest` extends `ClickHouseBase`, tagged `clickhouse`, excluded from `testUnit` by `excludeTags`).

**`build.gradle.kts` exclude block** [VERIFIED: read this session, lines ~436-451]:
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
Two excludes present today, matching D-05's premise exactly. Deleting only the first line is this phase's job.

**`Version.ARTIFACT_VERSION`** [VERIFIED: read generated file this session, `build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java`]:
```java
public static final String ARTIFACT_VERSION = "v1.6.0_gg1";
```
A `public static final String` literal — confirms D-06's "can never be null, contains no colon" claim structurally (it is baked in at compile time by the `generateVersionFile` Gradle task reading the `VERSION` file, not read at runtime from any manifest).

## Standard Stack

Not applicable in the conventional sense — this phase adds no new dependency, no new library, no new package. All work is internal re-plumbing of existing classes (`SinkTaskStatistics`, `ProxySinkTask`, `ClickHouseSinkTask`) and existing test infrastructure (`MBeanNameUnitTest`, `MBeanRegistrationLiveTest`, JUnit 5 + `javax.management`). No `## Package Legitimacy Audit` is required — no external packages are installed by this phase.

## Architecture Patterns

### System Architecture Diagram (JMX naming data flow, target state)

```
Kafka Connect worker
   │  props map (includes "name" = connector name, set by worker from connector config)
   ▼
ClickHouseSinkTask.start(props)
   │  props.get("name")  ──────────────────────────────┐
   ▼                                                    │ connectorName (String, may be null/empty)
new ProxySinkTask(config, errorReporter, connectorName) │
   │                                                     │
   ▼                                                     │
new SinkTaskStatistics(connectorName, taskId) ◄──────────┘
   │  constructor stores connectorName + taskId as fields
   │
   ├─ statistics.registerMBean()
   │     name = getMBeanName()                    [instance method]
   │          = buildMBeanName(connectorName, taskId, Version.ARTIFACT_VERSION)  [static, pure]
   │          "com.clickhouse:type=ClickHouseKafkaConnector,connector=<name>,name=SinkTask<id>,version=<v>"
   │     MBeanServerUtils.registerMBean(this, name)
   │
   └─ (per topic, lazily via computeIfAbsent) createTopicStatistics(topic)
         topicMBeanName = getTopicMBeanName(topic)         [instance method]
                        = getMBeanName() + ",topic=" + topic     [D-08 composition]
         topicMBeans.add(topicMBeanName)
         MBeanServerUtils.registerMBean(topicStatistics, topicMBeanName)

Platform MBeanServer (javax.management.ManagementFactory.getPlatformMBeanServer())
   ▲ queried by:
   │  - operators/Grafana/JConsole (production)
   │  - MBeanRegistrationLiveTest.registeredFor() — queryNames + getKeyProperty("connector") filter (test)
   │  - ClickHouseBase's @AfterEach MBean leak sweep (Phase 10, not yet restored)
```

### Recommended Structure — this phase touches, does not restructure

No new files or folders. Every file this phase edits already exists at its current path (see "Files this phase touches" in `08-CONTEXT.md` § canonical_refs, independently re-verified above).

### Pattern 1: Static pure-function name builder + instance convenience methods

**What:** `SinkTaskStatistics` gets a new static method that is a pure function of its three inputs (no field reads), plus thin instance methods that supply the instance's own `connectorName`/`taskId`/`Version.ARTIFACT_VERSION` to it.

**When to use:** Whenever a class needs both (a) a unit-testable formatting contract with no object construction overhead, and (b) ergonomic call sites for its own internal use. This is exactly the shape `ProxySinkTask.buildMBeanName` had pre-merge (per the Javadoc already sitting in `MBeanNameUnitTest`, which documents a method signature `buildMBeanName(String, int, String)` that currently exists nowhere in the tree — confirmed by reading `ProxySinkTask.java` in full this session).

**Example (recommended production shape, consistent with every constraint in D-01/D-03/D-04/D-06/D-08 — not itself pinned by a source, since this is the class the phase is about to write):**
```java
// src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java

private final String connectorName;
private final int taskId;
// ...existing fields unchanged...

public SinkTaskStatistics(String connectorName, int taskId) {
    this.connectorName = connectorName;
    this.taskId = taskId;
    // ...existing field initialization unchanged...
}

public void registerMBean() {
    String name = getMBeanName();
    LOGGER.info("Register MBean [{}]", name);
    MBeanServerUtils.registerMBean(this, name);
}

public void unregisterMBean() {
    String name = getMBeanName();
    LOGGER.info("Unregister MBean [{}]", name);
    MBeanServerUtils.unregisterMBean(name);
    for (String topicMBean : topicMBeans) {
        LOGGER.info("Unregister topic MBean [{}]", topicMBean);
        MBeanServerUtils.unregisterMBean(topicMBean);
    }
}

private TopicStatistics createTopicStatistics(String topic) {
    TopicStatistics topicStatistics = new TopicStatistics();
    String topicMBeanName = getTopicMBeanName(topic);
    LOGGER.info("Register topic MBean [{}]", topicMBeanName);
    topicMBeans.add(topicMBeanName);
    return MBeanServerUtils.registerMBean(topicStatistics, topicMBeanName);
}

// D-01: pure function, this is what MBeanNameUnitTest re-targets at
public static String buildMBeanName(String connectorName, int taskId, String version) {
    String safeVersion = version == null ? "unknown" : version.replace(':', '_');
    return String.format(
            "com.clickhouse:type=ClickHouseKafkaConnector,connector=%s,name=SinkTask%d,version=%s",
            connectorName, taskId, safeVersion);
}

// D-01: instance method, internal convenience
public String getMBeanName() {
    return buildMBeanName(connectorName, taskId, Version.ARTIFACT_VERSION);
}

// D-08: composed from getMBeanName(), not a second format string
public String getTopicMBeanName(String topic) {
    return getMBeanName() + ",topic=" + topic;
}
```

Two aspects to double-check against the exact literal `MBeanNameUnitTest` assertions before writing the plan (the format string above is inferred from D-01/D-04's requirement that "every assertion string stays byte-identical" — the literal assertions in the file, read in full this session, are: `"com.clickhouse:type=ClickHouseKafkaConnector,connector=my-connector,name=SinkTask3,version=0.9.6"`, `"...,connector=null,name=SinkTask0,version=0.9.6"`, `"...,connector=,name=SinkTask0,version=0.9.6"`, `"...,connector=c,name=SinkTask1,version=unknown"`, and the colon-suffix `endsWith` check):
1. The `null → "unknown"` substitution and `:` → `_` replacement must happen on the version argument exactly as shown — `MBeanNameUnitTest`'s four assertions (quoted verbatim above from the file read in full this session) pin this precisely.
2. `%s` formatting of a `null` `connectorName` argument produces the literal four characters `null` (Java's `String.format` behavior for `%s` on a null argument) — this is what D-02/JMX-02 rely on and is not something to special-case.

### Pattern 2: D-02's "pass null explicitly" test-call-site update

**Open question the plan must resolve — flagged rather than guessed** (see `## Open Questions` below): the four call sites at `ClickHouseSinkTaskTest.java:313,334,392` and `FailureTest.java:75` currently call the two-arg statics `SinkTaskStatistics.getMBeanName(taskId)` (verified: read this session, all three line numbers in `ClickHouseSinkTaskTest.java` and the one in `FailureTest.java` match exactly) and `SinkTaskStatistics.getTopicMBeanName(taskId, topic)` (verified: `:334`). Once `getMBeanName(int)`/`getTopicMBeanName(int,String)` are deleted per D-01 (replaced by *instance* methods the test has no `SinkTaskStatistics` instance to call), these four call sites must be rewritten against whatever static entry point remains. The two literal options, both consistent with every locked decision:

- **Option A** — call the pure static directly: `SinkTaskStatistics.buildMBeanName(null, taskId, Version.ARTIFACT_VERSION)` for the sink-task name, and `SinkTaskStatistics.buildMBeanName(null, taskId, Version.ARTIFACT_VERSION) + ",topic=" + topic` for the topic name (D-08's composition, inlined at the call site since there is no instance to call `getTopicMBeanName` on).
- **Option B** — add a small **static** overload that mirrors D-08's composition (e.g. a `getTopicMBeanName(String connectorName, int taskId, String topic)` static wrapping `buildMBeanName` + `,topic=`), giving the test call sites a named method to call instead of string concatenation at the call site.

Both compile, both satisfy "pass null explicitly," and neither is closed off by the CONTEXT.md decisions — D-01 only says `getTopicMBeanName(topic)` becomes an *instance* method, it does not forbid an additional static. The planner should pick one and write it into the plan explicitly (Option A is simpler and has fewer public-API surface additions; Option B keeps `ClickHouseSinkTaskTest`'s test code as method calls rather than string concatenation, closer to its current shape). This does not block planning — either shape satisfies ORCH-02/ORCH-03/D-02 — but the plan's task description for the four call-site edits should pick one and say so, since D-02's phrase ":334 (topic name, via `getTopicMBeanName`)" is ambiguous about whether that's describing the pre-edit or post-edit code.

### Anti-Patterns to Avoid
- **Duplicating the format string for the topic name** — D-08 explicitly rejects a second `buildTopicMBeanName` static; compose from `getMBeanName()`/`buildMBeanName(...)` instead.
- **Reading `Version.ARTIFACT_VERSION` at the `MBeanNameUnitTest` assertion site** — D-04 explicitly rejects this; keep the version literals (`0.9.6`, `unknown`, the colon cases) exactly as they are today, since the test's job is to pin the *format function*, independent of what the build currently sets `ARTIFACT_VERSION` to.
- **Storing `connectorName` on `ClickHouseSinkConfig`** — D-03 explicitly rejects this (wrong ownership — `name` is a Connect-framework key, not a `ConfigDef` option; would also grow the highest-rebase-risk file in the codebase for a single consumer).
- **Tree-wide `./gradlew spotlessApply`** — D-15 explicitly forbids this; scope Spotless to touched files only (an unrelated 52-file reformat has been pending since the Phase 6 merge and must not be swept into this phase's diff).

## Don't Hand-Roll

Not applicable — this phase does not introduce any new capability with a known off-the-shelf solution (no new serialization, no new HTTP client, no new auth flow). The only "solved problem" surface is `javax.management` MBean registration, which the codebase already wraps in `MBeanServerUtils` (verified: read in full this session) — this phase must not touch that wrapper (explicit Claude's Discretion item in CONTEXT.md).

## Runtime State Inventory

Not applicable — this is a naming/re-plumbing phase inside a single build, not a rename/refactor/migration of persisted or externally-registered state. The JMX ObjectName string that changes (`version=0.9.6` → `version=v1.6.0_gg1`, plus the new `connector=<name>` segment) is **operator-visible** (Grafana/JConsole dashboards scraping the old name), which is why D-07 requires it be recorded as a named fact in `.planning/MERGE-CHECKLIST.md § Recorded Facts` rather than silently changed — but this is a documentation/communication requirement, not a data-migration one; there is no datastore, external service config, OS-registered state, secret, or build artifact keyed on the old MBean name that this phase must migrate.

## Common Pitfalls

### Pitfall 1: `MBeanRegistrationLiveTest`'s topic-vs-task MBean counting hazard
**What goes wrong:** Once topic MBeans also carry `connector=<name>`, `MBeanRegistrationLiveTest.registeredFor()` — which filters the entire `com.clickhouse:type=ClickHouseKafkaConnector,*` space by the `connector` key alone — starts returning **both** task-level and topic-level MBeans for any task that has called `put()`.
**Why it happens:** The filter only checks `getKeyProperty("connector")`, not `getKeyProperty("topic")`. Today the two existing `MBeanRegistrationLiveTest` cases (verified: read in full this session) never call `put()`, so `newlyAdded()`'s "expected exactly one newly registered MBean" assertion stays correct by omission, not by design.
**How to avoid:** This is called out explicitly in `08-CONTEXT.md` § code_context as a "counting hazard for the planner" — any *new* live JMX test case this phase or a later one adds that inserts records must filter on the `topic` key too, or count differently.
**Warning signs:** A live JMX test that calls `put()` and then asserts `registeredFor(...).size()` will intermittently or permanently over-count once ORCH-03 lands.

### Pitfall 2: `ProxySinkTask.id`'s static counter breaks absolute-name assertions
**What goes wrong:** `ProxySinkTask.id` comes from a static `AtomicInteger NEXT_ID` (verified: read in full this session, `ProxySinkTask.java:33,40`) that never resets inside a JVM fork. A test asserting an absolute name like `SinkTask3` will pass once and fail on every subsequent JVM-fork-shared run, depending on test execution order.
**Why it happens:** JUnit/Gradle test JVM forks are shared across multiple test classes; `NEXT_ID` increments monotonically for the life of the fork.
**How to avoid:** `MBeanRegistrationLiveTest`'s own Javadoc (verified: read in full this session, lines 26-29) already documents this and restricts itself to relative claims only (distinctness, presence/absence). `MBeanNameUnitTest` sidesteps the problem entirely by calling `buildMBeanName` directly with explicit `taskId` arguments rather than constructing real tasks.
**Warning signs:** A flaky JMX test that passes in isolation (`--tests "*MBeanRegistrationLiveTest"`) but fails when run as part of the full suite.

### Pitfall 3: `MalformedObjectNameException` is swallowed with only a WARN log
**What goes wrong:** `MBeanServerUtils.registerMBean` (verified: read in full this session, lines 17-29) catches all non-`InstanceAlreadyExistsException` exceptions — including a malformed `ObjectName` string from a connector name containing a comma, `=`, or quote — logs a WARN, and returns the bean anyway. The connector runs perfectly and silently publishes zero JMX metrics.
**Why it happens:** Deliberate design (per Phase 4's D-05, referenced in this phase's Deferred Ideas) — the alternative (quoting the name) would change ObjectNames operators already scrape.
**How to avoid:** Do not attempt to fix this in Phase 8 — it is explicitly deferred. Do not write a test asserting a hostile connector name renders cleanly; `MBeanNameUnitTest`'s own trailing comment (verified: read in full this session, lines 69-73) explicitly says such names are "deliberately NOT covered here."
**Warning signs:** A reviewer proposing to quote/escape `connectorName` inside `buildMBeanName` — that is out of scope and reopens an operator-visibility decision that needs its own discussion.

### Pitfall 4: `Version.ARTIFACT_VERSION` making two `MBeanNameUnitTest` branches "dead" is a feature, not a bug to fix
**What goes wrong:** A future contributor notices JMX-03's null-version and colon-sanitization branches are unreachable from `SinkTaskStatistics`'s real call path (since `Version.ARTIFACT_VERSION` is a non-null compile-time literal with no colon — verified this session by reading the generated `Version.java`) and "cleans up" by deleting them from `buildMBeanName` and/or `MBeanNameUnitTest`.
**Why it happens:** Without D-06's rationale on hand, the branches look like dead code.
**How to avoid:** D-06 explicitly records this as a "consequence to carry, not to fix" — `buildMBeanName` is a general formatter, and ROADMAP criterion 1 requires the sanitization retained. Leave the branches and their tests in place.
**Warning signs:** A code-review comment or Spotless/static-analysis flag calling out unreachable branches in `buildMBeanName`.

### Pitfall 5: UTF-8 fixture fix — three occurrences, not one
**What goes wrong:** Fixing only the `getAllRowsAsJson`/`getAllRowsAsJsonCloud` occurrences (the two that the failing test names explicitly reference) and missing the third `InputStreamReader` at line 318.
**Why it happens:** The two failing test classes (`JsonDynamicRoundTripTest`, `SmtSinkLandingTest`) only exercise the first two call sites; the third is easy to miss without a full-file grep.
**How to avoid:** Verified this session — `grep -n "InputStreamReader" ClickHouseTestHelpers.java` returns exactly three matches: lines 140, 158, 318. Fix all three in the same commit; grep after editing to confirm zero `new InputStreamReader(` calls remain without an explicit `StandardCharsets.UTF_8` argument.
**Warning signs:** A grep for `InputStreamReader` post-fix that still shows a bare (uncharset) constructor call.

## Code Examples

### D-13's UTF-8 fixture fix (verified exact locations this session)
```java
// src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java
// Current (all three occurrences, byte-for-byte, verified this session):
BufferedReader reader = new BufferedReader(new InputStreamReader(queryResponse.getInputStream()));

// Fix (apply at lines 140, 158, and 318):
BufferedReader reader = new BufferedReader(
        new InputStreamReader(queryResponse.getInputStream(), StandardCharsets.UTF_8));
```
Requires adding `import java.nio.charset.StandardCharsets;` if not already imported (verify via `grep -n "^import" ClickHouseTestHelpers.java` before editing — not checked this session since it is a plan-execution-time detail, not a planning blocker).

### `MBeanNameUnitTest`'s current byte-for-byte assertions (verified this session, full file read — quoted here so the mechanical rename in D-04 has an exact target)
```java
// JMX-01
assertEquals(
        "com.clickhouse:type=ClickHouseKafkaConnector,connector=my-connector,name=SinkTask3,version=0.9.6",
        ProxySinkTask.buildMBeanName("my-connector", 3, "0.9.6"));

// JMX-02 (null connector name)
assertEquals(
        "com.clickhouse:type=ClickHouseKafkaConnector,connector=null,name=SinkTask0,version=0.9.6",
        ProxySinkTask.buildMBeanName(null, 0, "0.9.6"));

// JMX-02 (empty connector name)
assertEquals(
        "com.clickhouse:type=ClickHouseKafkaConnector,connector=,name=SinkTask0,version=0.9.6",
        ProxySinkTask.buildMBeanName("", 0, "0.9.6"));

// JMX-03 (null version)
assertEquals(
        "com.clickhouse:type=ClickHouseKafkaConnector,connector=c,name=SinkTask1,version=unknown",
        ProxySinkTask.buildMBeanName("c", 1, null));

// JMX-03 (colon sanitization, parameterized over {"1.2:3", "a:b:c"})
String expectedVersionSuffix = "version=" + versionWithColons.replace(':', '_');
String name = ProxySinkTask.buildMBeanName("c", 2, versionWithColons);
assertTrue(name.endsWith(expectedVersionSuffix), name);
```
All five call sites (four `assertEquals`-style calls plus the `@ParameterizedTest`) plus the class Javadoc `@link` reference `ProxySinkTask.buildMBeanName` — D-04's "mechanical carrier swap" means changing every one of these to `SinkTaskStatistics.buildMBeanName`, with **zero** change to any string literal.

### `Utils.java`'s FREE-01 evidence (verified this session, full file read)
```java
// src/main/java/com/clickhouse/kafka/connect/util/Utils.java:60-62
private static boolean isServerError(Throwable t) {
    return t instanceof ClickHouseException || t instanceof ServerException;
}

// :77-80
if (isServerError(rootCause)) {
    int errorCode = rootCause instanceof ServerException
            ? ((ServerException) rootCause).getCode()
            : ((ClickHouseException) rootCause).getErrorCode();
```
`com.clickhouse.client.api.ServerException` is client V2's exception type; `com.clickhouse.client.ClickHouseException` is client V1's. Both branches are live in the merged tree today — this is what makes FREE-01 a record-only requirement.

## State of the Art

| Old Approach (fork, pre-merge) | Current Approach (post-`v1.6.0` merge) | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `ProxySinkTask.buildMBeanName(connectorName, taskId, version)` — carrier + format string co-located on the sink-task orchestrator | Upstream deleted the carrier entirely; `SinkTaskStatistics` owns naming via non-connector-aware statics `getMBeanName(int)`/`getTopicMBeanName(int,String)` | v1.6.0 merge (Phase 6, commit `673d29c3`) | `connector=<name>` segment vanished from every MBean with no compile error and no conflict marker — one of the five "silently vanishing" fork behaviours named in `research/SUMMARY.md` |
| MBean `version=` sourced from `ClickHouseClientOption.class.getPackage().getImplementationVersion()` (client jar manifest, `0.9.6`) | Sourced from `Version.ARTIFACT_VERSION`, a build-generated compile-time constant (`v1.6.0_gg1`) | This phase (D-06) | Operator-visible dashboards keyed on `version=0.9.6` break; recorded as a named fact per D-07 |
| 4-arg `Processing` constructor | 5-arg `Processing(stateProvider, dbWriter, errorReporter, clickHouseSinkConfig, statistics)` | v1.6.0 merge | Already fully adopted — zero stale call sites, verified this session |
| Monolithic `put()` handling inside `ClickHouseSinkTask` | `DeliveryStrategy` interface with `DirectDeliveryStrategy`/`AtLeastOnceBufferStrategy`/`ExactlyOnceBufferStrategy` implementations + `ChunkFlusher` | v1.6.0 merge (upstream commit `a523e9dc`, "[REFACTOR] split delivery flows into DeliveryStrategy implementations") | New upstream capability (buffered delivery), already fully adopted with zero fork delta; out of this phase's requirement scope beyond verification |

**Deprecated/outdated:**
- Reading version from the ClickHouse client jar's manifest (`getImplementationVersion()`) — replaced by the connector's own `VERSION`-file-derived constant, which is the semantically correct source for "which build of the *connector* is this."

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The exact production method signatures for `SinkTaskStatistics.buildMBeanName`/`getMBeanName()`/`getTopicMBeanName(topic)` shown in "Pattern 1" are the correct target shape | Architecture Patterns → Pattern 1 | Low — every constraint (constructor arg, static pure function, instance convenience methods, D-08 composition) comes directly from locked decisions D-01/D-03/D-04/D-08; only the exact parameter/method naming is not textually pinned anywhere, so a planner could reasonably choose slightly different method names as long as `MBeanNameUnitTest`'s `SinkTaskStatistics.buildMBeanName(String, int, String)` call shape (three args, in that order, static) is satisfied — that specific shape **is** verified, since the test's own literal calls (currently against the non-existent `ProxySinkTask.buildMBeanName`) pin the exact parameter list via D-04's "mechanical" swap requirement |
| A2 | The resolution to Pattern 2's "Option A vs Option B" open question does not affect requirement satisfaction either way | Architecture Patterns → Pattern 2, Open Questions | Low — flagged explicitly as a planner decision point with both options spelled out; neither is precluded by CONTEXT.md |

**On this phase's scope:** every other claim in this document is `[VERIFIED]` — confirmed this session by reading the named file in full and/or running the named git/grep command against the live tree. No `[CITED]` or `[ASSUMED]` claims about external libraries or documentation exist in this research, because this phase introduces no new external dependency.

## Open Questions

1. **D-02's exact post-edit call-site shape at the four test files (Option A vs Option B in Pattern 2).**
   - What we know: `getMBeanName(int taskId)` is deleted; the four call sites must "pass null explicitly" to whatever replaces it.
   - What's unclear: Whether the replacement is a direct call to `SinkTaskStatistics.buildMBeanName(null, taskId, Version.ARTIFACT_VERSION)` (Option A) inlined with `,topic=`+topic string concatenation for the topic case, or a new static `getTopicMBeanName(String, int, String)` overload that keeps the test call sites reading as named-method calls (Option B).
   - Recommendation: The planner should pick one in the plan's task description for the four call-site edits (D-02's file/line list). Either satisfies ORCH-02/ORCH-03 and every locked decision; Option A adds zero new public API surface (leaner diff, consistent with the milestone's fork-delta-shrinking goal), Option B keeps the test reading closer to its current form. This is a five-minute decision at plan-writing time, not a blocker.

2. **Whether `Version.ARTIFACT_VERSION` needs to be imported in `SinkTaskStatistics.java` for use in the new `getMBeanName()` instance method, or is already imported.**
   - What we know: `Version.ARTIFACT_VERSION` is already imported and used at `SinkTaskStatistics.java:163,167` (verified this session, full file read) in the current two static methods being replaced.
   - What's unclear: Nothing — this is resolved; noted here only so the plan doesn't spend a task step re-discovering an import that already exists.
   - Recommendation: No action needed; the existing `import com.clickhouse.kafka.connect.sink.Version;` (verified: `SinkTaskStatistics.java:4`) stays as-is.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 17 toolchain | Compiling/running the whole suite | ✓ [VERIFIED: `./gradlew testUnit` ran successfully this session] | Project-pinned per `AGENTS.md` | — |
| Docker (colima, profile `vz`) | D-12's full `./gradlew test` run (JMX-04 live tests, `MBeanRegistrationLiveTest`) | Not probed this session (`testUnit` alone does not require it) | — | D-12's full-suite run must be executed with Docker running; `testUnit` alone (Docker-free) is insufficient to prove ORCH-02/ORCH-03/JMX-04, per this phase's own D-12 rationale |
| `git` remotes `origin`/`upstream` | Confirming byte-identity against the `v1.6.0` tag | ✓ [VERIFIED: `git remote -v`, `git tag` this session — both remotes and the `v1.6.0`/`pre-v1.6.0-sync` tags are present locally] | — | — |

**Missing dependencies with no fallback:** none identified for the Docker-free portion of this phase (JMX naming code + `MBeanNameUnitTest`). D-12's full-suite gate needs Docker/colima running — this was a known project-level blocker already tracked in `STATE.md § Blockers/Concerns` (`TESTCONTAINERS_HOST_OVERRIDE` hazard), not new to this phase.

## Security Domain

> `.planning/config.json` sets `workflow.security_enforcement: true` explicitly (`security_asvs_level: 1`, `security_block_on: "high"`), confirmed by reading the file this session — section included per that setting.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | This phase touches no authentication path |
| V3 Session Management | No | N/A |
| V4 Access Control | No | N/A |
| V5 Input Validation | Marginal | `connectorName` (`props.get("name")`) is operator-supplied config, not attacker-controlled network input; it flows into a `String.format` used to build an `ObjectName` string — already-known failure mode is `MalformedObjectNameException` on hostile characters (comma/`=`/quote), which is explicitly deferred (Pitfall 3) rather than validated in this phase |
| V6 Cryptography | No | N/A |
| V7 Error Handling and Logging | Yes | `MBeanServerUtils` already logs registration failures at WARN without throwing (except `InstanceAlreadyExistsException`) — unchanged by this phase, per Claude's Discretion item "`MBeanServerUtils` is not touched" |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Operator-supplied `name` config value containing characters that break `javax.management.ObjectName` parsing (comma, `=`, quote) | Denial of Service (of the JMX metrics surface specifically — the connector itself keeps running) | Not mitigated in this phase by design (Pitfall 3 / Phase 4's D-05) — `MalformedObjectNameException` is caught and logged, never thrown, so the connector's actual data path is unaffected; only observability degrades silently. This is a locked, deferred decision, not a gap this phase should close. |
| Log injection via `connectorName` reaching `LOGGER.info("Register MBean [{}]", name)` | Tampering (of log output) | SLF4J parameterized logging (`{}`) is already used throughout `SinkTaskStatistics` (verified: read in full this session) — this does not prevent a malicious operator-supplied name from containing newlines/control characters in the *value*, but this is standard-library-string-interpolation-level risk already accepted for every other config value in the codebase (e.g. table names via `Utils.escapeName`), and `name` is an operator/deployer-controlled Connect worker config, not attacker-reachable data-plane input |

## Sources

### Primary (HIGH confidence — files read in full, this session)
- `src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java` (178 lines)
- `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` (141 lines)
- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java` (206 lines)
- `src/main/java/com/clickhouse/kafka/connect/util/Utils.java` (304 lines)
- `src/main/java/com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.java` (42 lines)
- `src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java` (74 lines)
- `src/test/java/com/clickhouse/kafka/connect/sink/MBeanRegistrationLiveTest.java` (185 lines)
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskTest.java` (lines 290-409, the `statisticsTest`/`receiveLagTimeTest` methods containing the D-02 call sites)
- `src/test/java/com/clickhouse/kafka/connect/sink/dlq/FailureTest.java` (lines 60-89, the D-02 call site)
- `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` (lines 125-169, the D-13 UTF-8 fix locations)
- `build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java`
- `build.gradle.kts` (lines 430-455, the `generateVersionFile`/test-exclude block)
- `.planning/phases/08-orchestration-jmx-re-plumbing/08-CONTEXT.md` (full)
- `.planning/REQUIREMENTS.md` (full)
- `.planning/STATE.md` (full)
- `.planning/MERGE-CHECKLIST.md` (grepped for Phase-8-owned rows)

### Secondary (MEDIUM confidence)
- None — no external documentation was consulted; this phase is a pure internal re-plumb with no new library.

### Tertiary (LOW confidence)
- None.

## Metadata

**Confidence breakdown:**
- Standard stack: N/A — no new dependency
- Architecture: HIGH — every file in the data-flow path was read in full this session; the target design is fully specified by CONTEXT.md's locked decisions, with only two narrow implementation-detail questions flagged in Open Questions
- Pitfalls: HIGH — all five pitfalls are drawn from comments/Javadoc already written into the existing test files and CONTEXT.md, verified by reading the source this session, not inferred

**Research date:** 2026-09-19
**Valid until:** Until this phase's plan is executed and merged — this research is tied to the exact current tree state (verified via `git diff v1.6.0`), and any commit to the touched files before execution invalidates the "0-line diff" claims and should trigger a re-check.
