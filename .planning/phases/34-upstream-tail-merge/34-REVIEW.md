---
phase: 34-upstream-tail-merge
reviewed: 2026-10-29T00:00:00Z
depth: standard
files_reviewed: 5
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
  - src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskConstructorUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsNullMessageClassificationUnitTest.java
  - src/test/resources/connect-api-references.txt
findings:
  critical: 0
  warning: 1
  info: 3
  total: 4
status: issues_found
---

# Phase 34: Code Review Report

**Reviewed:** 2026-10-29
**Depth:** standard
**Files Reviewed:** 5
**Status:** issues_found

## Summary

Scope was the fork-authored delta from `adaf8c4e` to HEAD. The upstream merge itself was not reviewed.

**`ProxySinkTask` repair.** Behaviour is preserved for production. The public `(config, errorReporter, connectorName)` constructor delegates to the 4-arg core with `dbWriter == null`. That builds `new ClickHouseWriter(statistics)` and passes the same instance to `Processing`, exactly as before (verified against `adaf8c4e`). The one subtle change is `Processing` now receiving `this.dbWriter` instead of the parameter, which is correct. The only production caller, `ClickHouseSinkTask:48`, is unaffected. The in-package callers that pass a literal `null` third argument do not exist. `WriterFailureClassificationUnitTest` and `ValueFreeDlqHeadersUnitTest` pass a typed `CONNECTOR_NAME`.

**Test run.** I ran the three test classes under `./gradlew testUnit --offline`. They pass.

**Test quality.** The tests are largely sound. The oracle set is a literal copy rather than derived from the production constant. Constructor visibility is checked. The default-set sweep covers V1 and V2 server errors. The one real defect is JVM-global JMX state leaking from the seam-built tasks.

No critical findings.

## Warnings

### WR-01: Seam-built tasks that call `put()` leak topic MBeans into the platform MBean server

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskConstructorUnitTest.java:101-112`, `src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java:275-301`

**Issue:** `ProxySinkTask.put()` runs `Processing.doLogic`. That calls `statistics.recordTopicStats` or `recordTopicStatsOnFailure`, which call `createTopicStatistics`. This registers a `com.clickhouse:type=ClickHouseKafkaConnector,connector=null,name=SinkTask<id>,version=...,topic=<topic>` MBean on the JVM-global platform MBean server (`SinkTaskStatistics.java:155-161`).

These tests never call `stop()`, and `stop()` is the only thing that unregisters those MBeans. The affected tests are `dbWriterSeamConstructorWiresTheSuppliedWriterIntoProcessingAndRoutesInsertsToIt`, the strict task and the tolerant task in `writerDataExceptionFailsTheTaskNonRetriablyThroughProxySinkTaskWithCodesConfigured`.

Both classes deliberately extend nothing, so they do not get `ClickHouseBase.cleanupLeakedMBeans()` (`@AfterEach`). That sweep removes every `ClickHouseKafkaConnector,*` MBean. The leaked beans therefore survive in the shared test JVM. They are then either:

- silently swept, with WARN lines, by whichever `ClickHouseBase` test runs next, or
- visible to any global-count assertion.

`MBeanRegistrationLiveTest` filters by connector name, so it is safe today. This is state pollution between tests in the same fork, and it depends on test ordering.

The accepted known issue is the `connector=null` name. This finding is different. The beans are never cleaned up, and the new tests could unregister them.

**Fix:** Call `task.stop()` in a `finally` (or `@AfterEach`) for every `ProxySinkTask` that has run `put()`. `stop()` calls `InMemoryDBWriter.stop()`, which is a no-op, and then `unregisterMBean()`, which also removes the topic MBeans.

```java
ProxySinkTask task = new ProxySinkTask(config(), NO_ERROR_REPORTER, writer);
try {
    task.put(records);
    assertEquals(RECORD_COUNT, writer.recordsInserted());
} finally {
    task.stop();
}
```

Apply the same to `strictTask` and `tolerantTask`. A small helper that tracks created tasks and stops them in `@AfterEach` would avoid repeating this.

## Info

### IN-01: The 4-arg core silently substitutes a real `ClickHouseWriter` when `dbWriter` is null

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:62`

**Issue:** `dbWriter != null ? dbWriter : new ClickHouseWriter(...)` makes null mean "build the production writer". A test using the package-private seam with an accidentally null writer variable would quietly get a real `ClickHouseWriter`. Its `start()` would then try to reach a real server.

The seam ctor only passes a typed parameter, so this cannot happen in production. It is a small foot-gun for in-package test authors. The Javadoc-style comment above the core explains the ambiguity but not this null-means-default contract.

**Fix:** Either document the contract on the 4-arg constructor ("null `dbWriter` selects the production writer"), or make the seam constructor reject null with `Objects.requireNonNull(dbWriter, "dbWriter")` before delegating.

### IN-02: Constructor-set pin is brittle and contains a redundant assertion

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskConstructorUnitTest.java:79-97`

**Issue:** `assertEquals(3, constructors.length)` plus set equality fails on any legitimate future constructor addition, for example a upstream sync adding a 2-arg convenience constructor. That is intended as a tripwire, but the failure message does not say how to proceed.

The loop's `assertFalse(constructor.getParameterCount() == 2, ...)` is fully implied by the set-equality assertion above it, so it adds nothing. It also does not match the `DisplayName`.

**Fix:** Drop the redundant `assertFalse`. Add a message to the count assertion, such as "ProxySinkTask constructor set changed: re-check the connectorName/DBWriter overload ambiguity (TAL-02) before updating this pin".

### IN-03: The unset-key sweep checks the tolerance=all branch for V2 only

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java:157-178`

**Issue:** The sweep runs V2 and V1 server errors under `errors.tolerance=none`. The `errors.tolerance=all` branch is only run for V2 (`new RuntimeException(v2ServerError(code))`). The server-code branch in `Utils.handleException` is shared between V1 and V2, so this is probably fine today. Still, the test's own DisplayName claims "on client V2 and V1", and a V1-only regression under tolerance=all would pass.

The sweep also emits roughly 3,300 `handleException` WARN/ERROR log lines per run, which makes any real failure output hard to find.

**Fix:** Loop the tolerance=all assertion over both exception types, as the none branch does. Optionally narrow the sweep to the neighbourhood of the 15 codes plus a handful of sentinels (0, 1, 1100) to cut log noise.

---

_Reviewed: 2026-10-29_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
