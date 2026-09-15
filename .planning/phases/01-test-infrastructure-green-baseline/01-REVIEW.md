---
phase: 01-test-infrastructure-green-baseline
reviewed: 2026-09-16T00:00:00Z
depth: standard
files_reviewed: 8
files_reviewed_list:
  - build.gradle.kts
  - lombok.config
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseServerVersionSmokeTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java
findings:
  critical: 0
  warning: 3
  info: 2
  total: 5
status: issues_found
---

# Phase 01: Code Review Report

**Reviewed:** 2026-09-16T00:00:00Z
**Depth:** standard
**Files Reviewed:** 8
**Status:** issues_found

## Summary

This phase touches only test infrastructure and the Gradle build (no `src/main` changes): a shared `ClickHouseContainer` singleton per JVM fork, per-test-class isolated databases, an `@AfterEach` MBean-leak sweep, a `uniqueConnectorName()` helper, UTF-8-explicit response decoding in `ClickHouseTestHelpers`, a new harness/smoke test pair, a Docker-free `testUnit` Gradle task, and JaCoCo wired onto `test` only with a fork-changed-class report.

The core mechanics are sound and consistent with the stated intent: `@Tag("clickhouse")` on `ClickHouseBase` is `@Inherited` (verified against the JUnit Jupiter Jar's annotation metadata), so `testUnit`'s `excludeTags("clickhouse")` correctly skips every Docker-backed subclass; the two proxy test classes were correctly changed to use a new `proxyDb` field instead of clobbering the now-shared `db` field, which would otherwise have broken the singleton; the `tearDown()` null-guard (`db != null`) correctly prevents an NPE if container startup itself failed; and the JaCoCo fork-changed-class include list lines up with the classes this phase's harness/smoke tests actually exercise.

No Critical/BLOCKER-level issues were found — there is no `src/main` change in this phase's scope, so no production-code correctness risk. The Warnings below are about latent fragility that isn't exercised by the current (sequential, non-parallel) test configuration but would silently break the new isolation guarantees if that configuration ever changes, plus one CI-reliability concern and one dead/misleading line of test code left over from the `db` → `proxyDb` split.

## Warnings

### WR-01: Shared container/database singleton state has no synchronization or `volatile`, relying entirely on an undocumented sequential-execution assumption

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java:38-41, 51-67`
**Issue:** `db`, `database`, and `chc` are plain (non-`volatile`) static fields, and the singleton lazy-init in `setup()`:
```java
if (db == null) {
    db = new ClickHouseContainer(...)...;
    db.start();
    SHARED_CONTAINER_STARTS.incrementAndGet();
}
```
is a classic check-then-act race with no `synchronized` guard. The whole "one shared container per JVM fork" and "per-class isolated database" design this phase implements depends on JUnit Jupiter test classes within a fork running strictly sequentially — true today (no `junit-platform.properties` enables parallel execution, confirmed via repo search), but not enforced or documented anywhere. If parallel class execution is ever turned on (a common, easy Gradle/JUnit5 speed optimization), two classes' `@BeforeAll` could both observe `db == null`, race to start two containers, or one thread could read a not-yet-fully-published `db`/`database` reference due to the missing `volatile`, silently defeating both the singleton and the per-class isolation guarantees `ClickHouseTestHarnessTest` asserts.
**Fix:** Mark `db`, `database`, `chc` `volatile` and/or synchronize the lazy-init block, e.g.:
```java
private static final Object CONTAINER_LOCK = new Object();
...
synchronized (CONTAINER_LOCK) {
    if (db == null) {
        db = new ClickHouseContainer(...);
        db.start();
        SHARED_CONTAINER_STARTS.incrementAndGet();
    }
}
```
and/or add a `src/test/resources/junit-platform.properties` pinning `junit.jupiter.execution.parallel.enabled=false` with a comment explaining why, so the assumption is enforced rather than implicit.

### WR-02: `PullPolicy.alwaysPull()` forces a fresh registry pull of `latest` on every JVM fork, with no local-cache fallback

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java:59-60`
**Issue:**
```java
db = new ClickHouseContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE)
        .withImagePullPolicy(PullPolicy.alwaysPull())
```
Since `maxParallelForks` (`build.gradle.kts:175`) can spawn several JVM forks, and each fork independently triggers exactly one `alwaysPull()` at its first `ClickHouseBase` subclass, a full local or CI run can issue several concurrent anonymous Docker Hub pulls of `clickhouse/clickhouse-server:latest` in a short window. Combined with Docker Hub's anonymous-pull rate limiting, this is a known source of CI flakiness, and it defeats any benefit of an already-cached local image even when the tag hasn't changed since the last run.
**Fix:** Consider `PullPolicy.ageBased(Duration.ofHours(...))` (pull only if the cached image is older than N hours) instead of unconditional `alwaysPull()`, or pull once explicitly in a CI setup step and rely on Testcontainers' default (cached) pull policy for the per-fork container starts.

### WR-03: Dead `database = newTestDatabaseName();` assignment in `ClickHouseSinkTaskSchemalessProxyTest.setup()` implies isolation that doesn't exist

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java:31`
**Issue:** Every test method in this class calls `createClient(props, false)` (`withDatabase=false`, e.g. lines 73, 83, 100, 117, 134, 154, 176, 196, 214, 233), so `ClickHouseBase.database` is never read, never created, and never dropped by this class (its own `tearDown()` at line 50 only stops `proxyDb`/`toxiproxy`, not `ClickHouseBase.dropDatabase`). The `database = newTestDatabaseName();` line added by this phase is therefore inert: it sets shared static state that nothing in this class consumes, and its presence next to the sibling `ClickHouseSinkTaskWithSchemaProxyTest` (which *does* use `createClient(props)` with the default `withDatabase=true` and therefore genuinely uses `ClickHouseBase.database`) could mislead a future maintainer into believing this class's tables are isolated per-run, when in fact every test here still writes into the fixed `"default"` database on `proxyDb`.
**Fix:** Remove the unused assignment (isolation for this class already comes from its dedicated, per-class `proxyDb` container), or, if per-class database isolation is actually wanted here too, switch these test methods to `createClient(props, true)` and drop the isolated database in `tearDown()` before stopping the container.

## Info

### IN-01: Redundant fully-qualified type name alongside a newly added import

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaProxyTest.java:36, 46`
**Issue:** This phase adds `import org.testcontainers.clickhouse.ClickHouseContainer;` (for the new `private static ClickHouseContainer proxyDb` field), but the constructor call at line 46 still uses the fully-qualified name: `proxyDb = new org.testcontainers.clickhouse.ClickHouseContainer(...)`. The sibling `ClickHouseSinkTaskSchemalessProxyTest` (same change, same phase) uses the simple name consistently.
**Fix:** `proxyDb = new ClickHouseContainer(ClickHouseTestHelpers.CLICKHOUSE_FOR_PROXY_DOCKER_IMAGE)` for consistency with the import and the sibling file.

### IN-02: Harness test's isolation assertions depend on an undocumented cross-class sequential-execution guarantee

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java:99-107`
**Issue:** `classGetsFreshIsolatedDatabase` asserts `SELECT count() FROM system.databases WHERE name LIKE 'kafka_connect_test_%' AND name != '<mine>'` is `0`. This is correct only because every other `ClickHouseBase`-derived test class in the same JVM fork fully completes its lifecycle (including `@AfterAll` dropping its own database) before this class's `@BeforeAll` runs — true under Gradle/JUnit5's default sequential execution, but not asserted or documented anywhere in this file or `ClickHouseBase`. This is the same underlying assumption as WR-01; noted separately here because it's the specific test whose correctness (not just design intent) silently depends on it.
**Fix:** A short comment on this test (or a `@ResourceLock`/`@Execution(SAME_THREAD)` guard) documenting the sequential-execution dependency would make the coupling to WR-01 explicit for future maintainers.

---

_Reviewed: 2026-09-16T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
