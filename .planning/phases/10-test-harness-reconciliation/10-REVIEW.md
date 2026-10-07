---
phase: 10-test-harness-reconciliation
reviewed: 2026-09-20T15:59:20Z
depth: standard
files_reviewed: 8
files_reviewed_list:
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java
  - src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseHarnessOptOutContainerTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseVersionDefaultUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java
  - build.gradle.kts
findings:
  critical: 0
  warning: 1
  info: 2
  total: 3
status: issues_found
---

# Phase 10: Code Review Report

**Reviewed:** 2026-09-20T15:59:20Z
**Depth:** standard
**Files Reviewed:** 8
**Status:** issues_found

## Summary

Reviewed the re-derived `ClickHouseBase` shared-container harness, its opt-out seam, the restored `@AfterEach` MBean sweep, the `CLICKHOUSE_VERSION_DEFAULT`/pull-policy restoration, the new canary test classes, the two Toxiproxy classes' opt-out overrides, and the `build.gradle.kts` `testUnit` task / exclusion removal.

The shared-container lifecycle logic is sound: the `synchronized(ClickHouseBase.class)` lazy-start guard and the single `SHARED_CONTAINER_STARTS` increment site are correctly paired (write and read both happen under the same monitor or through the `volatile sharedDb`/`SHARED_CONTAINER_STARTS` fields), and since Gradle's `maxParallelForks` maps to separate JVM processes (not intra-JVM thread parallelism, and no JUnit 5 parallel-execution config is present anywhere in the repo), the static shared state is never actually raced within a fork — the locking is correct defensive practice, not a no-op. The opt-out seam (`usesSharedContainer()`) is minimal, correctly isolates the two Toxiproxy classes' colliding `toxiproxy` network alias from the shared container's network, and does not perturb the shared-start counter, verified independently against `SinkTaskStatistics.getMBeanName()`/`getTopicMBeanName()` in `src/main` (the sweep's `com.clickhouse:type=ClickHouseKafkaConnector,*` pattern does provably cover both the task-level and per-topic MBean names, confirmed by reading the actual production format strings rather than trusting the harness test's own comment). The `CLICKHOUSE_VERSION_DEFAULT`/`CLICKHOUSE_VERSION_LATEST` restoration and the conditional `PullPolicy.alwaysPull()` are both correctly implemented and match their documented intent. `build.gradle.kts` no longer excludes `ClickHouseTestHarnessTest` from `compileTestJava`, and no other silent test-source exclusion was found; `testUnit`'s `excludeTags("clickhouse")` correctly picks up the new `ClickHouseVersionDefaultUnitTest` (it extends nothing, so it never inherits `@Tag("clickhouse")`) while correctly excluding `ClickHouseHarnessOptOutContainerTest` (which does inherit the tag via `ClickHouseBase`).

One real robustness gap was found in the shared-container teardown path (`ClickHouseBase.tearDown()`): a failed database drop is swallowed silently, and the only test that depends on that drop having succeeded (`classGetsFreshIsolatedDatabase`) has no way to distinguish "this class's isolation is broken" from "an earlier class's teardown silently failed." Two lower-severity diagnostics issues are also noted.

## Warnings

### WR-01: Silent `tearDown()` database-drop failure can cause a later, unrelated class's isolation assertion to fail with no attributable cause

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java:139-146`
**Issue:** Under the shared-container path, `tearDown()`'s database drop is the *only* mechanism (per the class's own D-01 comment at line 140-141) that prevents a class's database from remaining visible to the next class sharing the same container. If `ClickHouseTestHelpers.dropDatabase(...)` throws — a transient network blip, a Keeper hiccup, the shared container being momentarily overloaded by 27+ classes' worth of prior activity — the exception is caught, logged once at `LOGGER.error`, and silently discarded:

```java
try (var tmpClient = ClickHouseTestHelpers.createClient(getBaseProps())) {
    ClickHouseTestHelpers.dropDatabase(tmpClient, database);
} catch (Exception e) {
    LOGGER.error("Error dropping database", e);
}
```

`ClickHouseTestHarnessTest.classGetsFreshIsolatedDatabase` (`ClickHouseTestHarnessTest.java:123-137`) directly depends on every prior class in the fork having successfully dropped its database — it asserts `0` rows from `system.databases WHERE name LIKE 'kafka_connect_test_%' AND name != '<this class's database>'`. If any earlier class's drop silently failed, this assertion fails in a class that did nothing wrong, with a stack trace that only shows "system.databases has an unexpected row," not which class or teardown produced it. The `LOGGER.error` line for the actual failure will be scattered somewhere earlier in the log of a different, already-passed test class, making this a hard-to-diagnose, order-dependent flake purely as a byproduct of container sharing.
**Fix:** At minimum, make the failure discoverable at the point it happens rather than only downstream: include the class name and the target database name in the log line (already partially present via `database`, but not logged), and consider a bounded retry (the codebase already has a retry-with-sleep pattern in `ClickHouseTestHelpers.dropTable`) before giving up. If a drop failure must remain non-fatal (reasonable, to avoid making an unrelated class's teardown flake the build), at least emit a distinctive, greppable marker (e.g. a dedicated JUnit `TestWatcher`/`ExtensionContext.Store` note, or a stray-database `@AfterAll`-in-summary check) so a later isolation-assertion failure can be traced back to the class whose drop actually failed, instead of only the innocent class that observed it.

## Info

### IN-01: Class-name-keyed server log extraction is misleading under container sharing because the underlying log content is cumulative

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java:155-170`
**Issue:** `copyServerLogs()` is correctly documented as "diagnostic convenience only," but under the shared-container model every class's `tearDown()` now copies the *entire* `clickhouse-server.log`/`clickhouse-server.err.log` accumulated since the container started — not just the log lines produced during that class's own tests — into a file named `server_<ThisClass>.log`. For any class other than the first to run in a given fork, `server_<ThisClass>.log` will contain every earlier class's server-side log lines too. A CI engineer triaging a failure by opening the log file named after the failing class may reasonably assume its contents are scoped to that class, and could misattribute an earlier class's server-side warning/error to the class under investigation (or, conversely, miss that the real error is buried far earlier in a much longer file than expected).
**Fix:** Either note in the class comment that the file is a cumulative snapshot (not a per-class delta) so future readers don't assume scoping that doesn't exist, or track a log-offset (e.g. via `docker logs --since` / byte offset before the class's first test) and extract only the delta per class.

### IN-02: Opt-out seam has no automated guard against a future network-sharing class forgetting to opt out

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java:55-64`
**Issue:** `usesSharedContainer()` defaults to `true`, which is the safe default for an ordinary new test class (reviewed and confirmed: a class that adds nothing beyond extending `ClickHouseBase` lands cleanly on the shared container, exactly per review priority #4). However, the specific failure mode the opt-out seam exists to prevent — two classes binding an auxiliary container to the shared `Network` under the same alias (as the two Toxiproxy classes do) — has no compile-time or run-time guard; it relies entirely on a future author reading the `[D-02]` comments on the two existing opt-out classes and remembering to add a third override. Nothing in `ClickHouseBase` detects or fails fast on an alias collision (e.g. Testcontainers will simply let two containers claim the same alias on the same network until one of them fails to resolve the other at connection time, which surfaces as a confusing connection failure rather than a clear "alias already claimed" error).
**Fix:** No code change required for the current two classes (both correctly opt out, verified). Consider, as a low-cost future safeguard, a static registry of network aliases claimed against the shared network (assert-on-add) so a future class that forgets to opt out fails fast with an explicit "alias X already claimed by class Y" message instead of an opaque container-networking failure.

---

_Reviewed: 2026-09-20T15:59:20Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
