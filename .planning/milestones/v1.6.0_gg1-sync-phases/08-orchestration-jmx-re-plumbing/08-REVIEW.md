---
phase: 08-orchestration-jmx-re-plumbing
reviewed: 2026-09-19T00:00:00Z
depth: standard
files_reviewed: 15
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java
  - src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
  - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
  - src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/dlq/FailureTest.java
  - src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java
  - build.gradle.kts
  - src/test/java/com/clickhouse/kafka/connect/sink/db/AvroDecimalLogicalTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/AvroNullableUnionVariantTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterColumnContextTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java
findings:
  critical: 0
  warning: 2
  info: 0
  total: 2
status: issues_found
---

# Phase 8: Code Review Report

**Reviewed:** 2026-09-19T00:00:00Z
**Depth:** standard
**Files Reviewed:** 15
**Status:** issues_found

## Summary

This phase restores the JMX `connector=<name>` ObjectName segment that the v1.6.0 merge deleted, by
threading `connectorName` through `ClickHouseSinkTask.start()` -> `ProxySinkTask` ->
`SinkTaskStatistics`, and relocating `buildMBeanName` as a pure static formatter on
`SinkTaskStatistics`.

**The naming chain was traced end-to-end and is intact and correct:**
- `ClickHouseSinkTask.start()` assigns `connectorName = props.get("name")` inside the same `try`
  block as config construction, before `ProxySinkTask` is constructed; the only call site of `new
  ProxySinkTask(...)` in the tree passes it through (verified via `grep -rn "new ProxySinkTask("`).
- `ProxySinkTask`'s constructor passes `connectorName` straight into `new
  SinkTaskStatistics(connectorName, id)`; `id` is a field-initializer (`NEXT_ID.getAndAdd(1)`) that
  Java guarantees runs before the constructor body that consumes it, so no ordering hazard exists.
- `connectorName` and `taskId` are both `private final` on `SinkTaskStatistics`; the instance is
  never registered with the JMX server until `ProxySinkTask.start()` calls
  `statistics.registerMBean()`, strictly after full construction — final-field safe-publication
  rules mean no cross-thread visibility hazard between the task thread that constructs the object
  and the JMX server thread that later reads its attributes.
- `getTopicMBeanName(topic)` is composed from `getMBeanName() + ",topic=" + topic`, so the
  per-topic name is provably a strict superset/prefix extension of the task-level name and can
  never equal it (confirmed by `grep` — there is exactly one literal
  `"com.clickhouse:type=ClickHouseKafkaConnector,connector=%s,..."` format string in `src/main`;
  no second, independently-drifting format string exists anywhere in the reviewed tree).
- All 16 "Rule 3" auto-fixed test call sites (the ones not enumerated in the original plan) were
  checked individually: none of the touched test classes ever set a `name` prop or extend
  `ClickHouseBase`, so passing `null` explicitly is correct in every case — no call site discards a
  real connector name that was actually available.
- The `ClickHouseTestHelpers.java` UTF-8 change is a pure charset-argument addition to three
  pre-existing `InputStreamReader` constructions; no control flow, resource-ownership, or
  try-with-resources structure was altered by this phase.

No blocking defects were found in the reviewed files. Two pre-existing, non-regression quality
issues were surfaced while tracing the files this phase touches; they are reported below because
they sit directly on the code paths this phase's review focus called out (the JMX naming class and
the touched stream-reader helper), even though neither was introduced or worsened by this phase's
diff.

## Warnings

### WR-01: `SinkTaskStatistics`'s logger is bound to the wrong class

**File:** `src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java:17`
**Issue:** `LOGGER` is created via `LoggerFactory.getLogger(ProxySinkTask.class)` inside
`SinkTaskStatistics`, not `SinkTaskStatistics.class`. Every `registerMBean`/`unregisterMBean`/topic
MBean log line in this class (lines 53, 59, 62-63, 158) is therefore emitted under the
`com.clickhouse.kafka.connect.sink.ProxySinkTask` logger category instead of
`com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics`. This predates Phase 8 (the line is
untouched by this phase's diff, only reordered along with the surrounding imports), but it directly
affects the class this phase's review focus is centered on: an operator trying to isolate
`SinkTaskStatistics`'s MBean registration/unregistration logs by logger name (e.g. to debug a
missing `connector=` segment in production) will not find them under that class's logger, and
anyone raising `ProxySinkTask`'s log level independently of `SinkTaskStatistics`'s will
inadvertently capture or suppress the wrong set of messages.
**Fix:**
```java
private static final Logger LOGGER = LoggerFactory.getLogger(SinkTaskStatistics.class);
```

### WR-02: Charset-fixed stream readers in `ClickHouseTestHelpers` are still not closed

**File:** `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java:140-141, 159-160, 320-321`
**Issue:** The three `InputStreamReader`/`BufferedReader` pairs this phase pinned to
`StandardCharsets.UTF_8` (`getAllRowsAsJson`, `getAllRowsAsJsonCloud`, `validateRows`) are local
variables, never explicitly closed, and not declared in the enclosing `try (QueryResponse
queryResponse = ...)` resource list. Closing is currently only implicit, via
`queryResponse.close()` closing the underlying `InputStream` the reader wraps — this happens to
work today because `BufferedReader`/`InputStreamReader` hold no OS resources of their own beyond
the wrapped stream, but it is a latent hazard: if the underlying `QueryResponse`/`InputStream`
implementation is ever changed to hand back a stream that isn't automatically closed by
`queryResponse.close()` (or if a decoder-side buffer needs an explicit `close()` to flush/release
in a future client version), the leak will not surface as a compile error or an obvious test
failure — only as a slow resource exhaustion under sustained integration/load-test runs. This
predates Phase 8 (the pattern was `new InputStreamReader(stream)` with the same absence of
try-with-resources before this diff; the phase only added the charset argument), but since this
phase specifically touched these three call sites, it was the natural point to also make the
closing explicit.
**Fix:**
```java
try (QueryResponse queryResponse = chc.getClient().query(query, querySettings).get();
     BufferedReader reader = new BufferedReader(
             new InputStreamReader(queryResponse.getInputStream(), StandardCharsets.UTF_8))) {
    List<JSONObject> jsonObjects = new ArrayList<>();
    String line;
    while ((line = reader.readLine()) != null) {
        jsonObjects.add(new JSONObject(line));
    }
    return jsonObjects;
} catch (Exception e) {
    throw new RuntimeException(e);
}
```

---

_Reviewed: 2026-09-19T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
