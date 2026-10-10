# Phase 10: Test Harness Reconciliation - Research

**Researched:** 2026-09-20
**Domain:** JUnit 5 test-harness lifecycle (Testcontainers 2.0.5, `@TestInstance(PER_CLASS)`), JMX MBean cleanup, Gradle test source-set compilation
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Shared-container strategy (HARN-03)**
- **D-01: Restore one shared container per test JVM fork**, re-derived over upstream's `@TestInstance(PER_CLASS)` shape rather than taking the fallback HARN-03 permits. A static container plus a lazy start guard; `@BeforeAll setup()` stays an instance method and reuses the static container; `@AfterAll tearDown()` **drops the class database** instead of stopping the container.
  - The `tearDown()` database drop is **not optional**: `ClickHouseTestHarnessTest.classGetsFreshIsolatedDatabase` asserts that **zero other** `kafka_connect_test_%` databases exist on the server. Under per-class containers upstream got that for free by stopping the container; under sharing it must be explicit.
  - Rejected — keep upstream's per-class container and record the fallback (maxParallelForks resolves to 1 on a 2-core CI runner → 27 sequential starts against `timeout-minutes: 45`).
  - Rejected — measure both first, then decide (CI shape cannot be measured cheaply from here; fork contract already proven at Phase 1: 0 flakes at 6 forks).
  - **Reversibility:** reversible — the container field and the `tearDown` body; no production code, no published contract.

- **D-02: Shared by default, with an explicit opt-out seam.** Add an overridable hook on `ClickHouseBase` (shape at planner's discretion, e.g. `protected boolean usesSharedContainer()` defaulting to `true`). `ClickHouseSinkTaskSchemalessProxyTest` and `ClickHouseSinkTaskWithSchemaProxyTest` override it to `false` and get their own container + `Network`; 25 of 27 classes use the shared one.
  - **Why a seam and not luck:** both proxy classes bind Toxiproxy to `db.getNetwork()` and both claim the alias `"toxiproxy"`. Under one shared container they land on the same network with a colliding alias whenever Gradle assigns them to the same fork — nondeterministic run to run.
  - **Constraint for the planner:** an opt-out class's container **must not** increment the shared-start counter, or `assertEquals(1, sharedContainerStarts())` becomes order-dependent.
  - Rejected — attach the shared container to each proxy class's own `Network` at class setup (live network surgery on a running container).
  - **Reversibility:** reversible.

- **D-03: Keep upstream's server-log extraction, keyed by test-class name.** Copy `clickhouse-server.log` / `.err.log` into `build/reports/tests/server_<SimpleClassName>.log` while the shared container is still alive, rather than upstream's `server_<mappedPort>.log` written just before `ch.stop()`.
  - No JVM shutdown hook — Phase 1 explicitly ruled one out.
  - Accepted cost: 27 cumulative snapshots of the same growing log; the last one written is the complete record.
  - Rejected — drop the copy; rejected — keep it only for opt-out classes.
  - **Reversibility:** reversible.

**ClickHouse image default — the seventh silently-vanishing fork behaviour**
- **D-04: Restore `CLICKHOUSE_VERSION_DEFAULT = "latest"` and add a Docker-free canary that pins it.** The canary asserts the default value itself, not the composed image name.
  - **Why it hid:** CI pins `CLICKHOUSE_VERSION: latest` in `tests.yaml:33`; only local runs — every GSD gate since the merge — silently moved to `25.8.18.1`.
  - Rejected — keep upstream's LTS pin and lean on CI's env.
  - **Reversibility:** reversible.

- **D-05: `alwaysPull()` for floating tags only.** Apply `PullPolicy.alwaysPull()` when the resolved version is a floating tag (`latest`); default cached pull when `CLICKHOUSE_VERSION` names a pinned version.
  - Rejected — restore unconditionally; rejected — leave it off.
  - **Reversibility:** reversible.

- **D-06 (ownership rule): Phase 10 owns what `latest` surfaces, with a two-server comparison run for attribution.** The gate runs on `latest` at zero failures. If a class is red on `latest` where `25.8.18.1` is green, re-run that class pinned to `25.8.18.1` to separate "the harness broke it" from "the server changed", then decide fix-here vs hand-to-Phase-11 at a checkpoint, on that evidence.
  - Sits under the milestone's locked rule ("bug fixes live in the phase whose tests expose them") but adds the attribution step.
  - Rejected — re-baseline the gate and hand server-version failures forward unconditionally.
  - **Reversibility:** reversible.

**The `ClickHouseTestHarnessTest` un-exclude**
- **D-07: Adapt the canary to the post-merge API; do not re-open `ClickHouseBase`.** Redirect `createProps()` → `getBaseProps()` and `createClient(props)` → `ClickHouseTestHelpers.createClient(props)`, exactly as Phase 7 did for 12 sibling files. Then delete `exclude("**/ClickHouseTestHarnessTest.java")` from `build.gradle.kts:448`. `ClickHouseBase` gains only `sharedContainerStarts()`, which D-01 requires anyway.
  - **The tension, recorded:** this canary is what proves the harness contract, so rewriting it means the proof and the thing proved change in the same commit. Accepted knowingly — commit separately (D-11).
  - Rejected — restore `createProps()`/`createClient()` on `ClickHouseBase`; rejected — thin deprecated delegates.
  - **Reversibility:** reversible.

- **D-08: Extend the canary to pin the opt-out hook and the per-topic MBean sweep.**
  - **Opt-out hook:** a class overriding the hook to `false` gets its own container and the shared-start counter does not move.
  - **Per-topic sweep:** register a sink task with a topic, leak it deliberately, assert the `…,topic=<t>` MBean is gone after `@AfterEach`.
  - **Not pinned:** the class-name-keyed log copy (D-03) — a diagnostic convenience, not a contract.
  - **Reversibility:** reversible.

**MBean leak sweep (HARN-02)**
- **D-09a: Restore the sweep verbatim — WARN, unregister, continue.** `@AfterEach` querying `com.clickhouse:type=ClickHouseKafkaConnector,*`, logging each leak at WARN, unregistering via `MBeanServerUtils.unregisterMBean`.
  - **The per-topic extension may cost nothing:** the `,*` pattern already matches `…,connector=X,name=SinkTask0,version=V,topic=T`. Confirm by command, then discharge HARN-02's "extended to cover per-topic" with D-08's proving test rather than a pattern change.
  - Rejected — unregister then fail the test (would red `ClickHouseSinkTaskJsonInsertFormatTest`'s two P4-WR-01 methods); rejected — silent unregister at DEBUG.
  - **Reversibility:** reversible.
- **HARN-02's second half needs no new machinery.** `MBeanServerUtils.java:22-23` rethrows `InstanceAlreadyExistsException` as a `RuntimeException`, so a name collision is already a test failure — discharged by a green full suite.

**Acceptance gate**
- **D-09: `testUnit` with Docker genuinely stopped, plus TWO consecutive clean full `./gradlew test` runs on `latest`.** Pass condition for each live run: zero failures, zero errors, every skip explained. D-01 makes 27 classes order-dependent within a fork for the first time, and fork assignment varies run to run — one green run proves less here than it did in Phase 9. "Docker genuinely stopped" means stopped, not merely untouched.
  - **Reversibility:** n/a — a gate, not an artifact.

### Claude's Discretion

- **D-10: Discharging `HARN-04`/`05`/`06`/`07`.** All four are already true (see Measured Starting State below). Discharge as recorded evidence acts — re-run each command at execution time rather than citing this document.
- **D-11: Commit sequence**, per one-behaviour-per-commit: (1) shared container + opt-out seam + `tearDown` database drop + class-name-keyed log copy; (2) `@AfterEach` MBean sweep; (3) `latest` default + floating-tag `alwaysPull` + its Docker-free canary; (4) canary's API redirection **and** `build.gradle.kts:448` un-exclude; (5) canary's two new pins; (6) 17 checklist flips and Recorded Facts. Run `./gradlew spotlessApply` over **only the files each commit touches**.
- **The opt-out hook's exact shape** — `protected boolean` override, a JUnit `@Tag`, or an annotation. The constraint that matters is D-02's counter rule.
- **Wave shape.** The sweep (D-09a) and image default (D-04/D-05) are independent of the container work (D-01/D-02/D-03) and of each other. The canary un-exclude (D-07/D-08) depends on all three. The gate is necessarily last.
- **Cloud and cluster branches.** `isCloud`/`isCluster` already bypass `setupContainer` entirely; this milestone tests neither. Leave both paths untouched.
- **No new metric.** This phase changes no production insert path.

### Deferred Ideas (OUT OF SCOPE)

- Should `tests.yaml` stop setting `CLICKHOUSE_VERSION` explicitly once D-04 makes the default correct? `tests.yaml` is Phase 11's file.
- **P4-WR-01** — `ClickHouseSinkTaskJsonInsertFormatTest`'s missing `try/finally`. D-09a deliberately keeps the sweep permissive so this stays an accepted follow-up.
- **CR-01** — `.planning/scripts/verify-merge-invariants.sh:44` fails open. Not Phase 10 work; must be fixed before Phase 11.
- `/gsd-secure-phase 07` and `09` — due before Phase 11's publication gate.
- `maxParallelForks = availableProcessors() / 2` untuned for the post-merge suite — worth revisiting in Phase 11 with a measured number.
- **R-01's fail-fast fix** — unscheduled, unaffected by this phase.
- Reopening `Column.extractColumn`'s swapped args, `USERNAME` ConfigDef default, `JsonColumnTest`'s row-count-only assertions — not harness work.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| HARN-01 | `ClickHouseBase` carries `@Tag("clickhouse")` again, proven by `./gradlew testUnit` completing with Docker stopped | **Already true** — verified by reading `ClickHouseBase.java:25` this session (`@Tag("clickhouse")`) and `build.gradle.kts`'s `testUnit` task (`excludeTags("clickhouse")`). Proof act only: re-run `testUnit` with Docker stopped as the phase's first verification step. |
| HARN-02 | `ClickHouseBase`'s `@AfterEach` MBean leak sweep restored and extended to cover upstream's per-topic MBeans, zero `InstanceAlreadyExistsException` in a full-suite run | Confirmed absent this session (no `@AfterEach` anywhere in `ClickHouseBase.java`). `MBeanServerUtils.java:17-29` (read this session) shows `registerMBean` rethrows `InstanceAlreadyExistsException` as `RuntimeException` — see Code Examples for the exact restore shape and the `,*` pattern-match analysis. |
| HARN-03 | `uniqueConnectorName(TestInfo)` available again; shared-container strategy re-derived over `@TestInstance(PER_CLASS)` | `uniqueConnectorName` **already present** (`ClickHouseBase.java:139-142`, read this session). Shared-container re-derivation is new work — see Architecture Patterns § Shared Container Over PER_CLASS. |
| HARN-04 | Exactly one `ClickHouseTestHelpers.java`, under `src/testFixtures`, every fork import updated, verified by grep | **Already true.** `git ls-files` shows exactly one path this session: `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` (461 lines, read in full this session). Discharge as a grep-and-record evidence act. |
| HARN-05 | `SchemaTestData`/`SchemalessTestData` exist only under `src/testFixtures`; `FromVersionConditionExtension` package move absorbed | **Already true** per CONTEXT.md's measured-state table (re-verify by grep at execution time — not independently re-verified this session beyond the `src/testFixtures/.../helper/` directory listing already confirmed via `ClickHouseTestHelpers.java`'s sibling files). |
| HARN-06 | `./gradlew compileIntegrationTestJava` succeeds so the out-of-scope integration source set does not rot | **Re-verified this session.** `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileTestJava compileTestFixturesJava compileIntegrationTestJava` → `BUILD SUCCESSFUL`, all tasks `UP-TO-DATE`. This is the pre-Phase-10 baseline; re-run after the harness changes to prove it still holds. |
| HARN-07 | Testcontainers `2.0.5` verified to start a ClickHouse container through `ClickHouseBase` before fork tests are adapted | `gradle/libs.versions.toml:23` → `testcontainers = "2.0.5"` (verified this session). Phase 9's last full run was 645 tests / 0 failures in 3m01s, already exercising `2.0.5` through the current (per-class) `ClickHouseBase`. The sequencing clause ("before … adapted") is moot — re-affirm via one live container start as a proof act, not new work. |
</phase_requirements>

## Summary

Phase 10's shape is unusual: four of its seven requirements (HARN-04/05/06/07) are **already satisfied** by the Phase 6 merge and Phase 7's early sliver — this session re-confirmed HARN-04 (exactly one `ClickHouseTestHelpers.java`, read in full) and HARN-06 (`compileTestJava compileTestFixturesJava compileIntegrationTestJava` → `BUILD SUCCESSFUL` against the current tree) directly. The real work is three behaviours on `ClickHouseBase.java` (currently 143 lines, `@TestInstance(PER_CLASS)`, per-class container, no `@AfterEach`) — re-deriving the pre-merge shared-container-per-JVM-fork pattern over upstream's per-class rewrite, restoring the `@AfterEach` MBean sweep, and fixing a silent version-default regression (`CLICKHOUSE_VERSION_DEFAULT` is currently `"25.8.18.1"`, confirmed at `ClickHouseTestHelpers.java:30` this session, not `"latest"`) — plus un-excluding and adapting `ClickHouseTestHarnessTest.java`, the canary that proves all of it.

This session's own reading of `ClickHouseTestHarnessTest.java` surfaced one gap CONTEXT.md's D-07 does not name: the canary currently calls `ClickHouseTestHelpers.createTable(chc, tableName, ddl)` (lines 123-124, 179) and `ClickHouseTestHelpers.query(chc, sql)` (line 180) — **neither method exists** on the 461-line `ClickHouseTestHelpers.java` this session read in full. D-07 only anticipates redirecting `createProps()`→`getBaseProps()` and `createClient(props)`→`ClickHouseTestHelpers.createClient(props)`; it does not mention these two. Six other fork test files (`RowBinaryFailFastLiveTest`, `JsonDynamicRoundTripTest`, `SmtSinkLandingTest`, `MBeanRegistrationLiveTest`, `AsyncInsertLiveTest`, `ClickHouseHelperClientForkLiveTest`) each carry their own private `createTable(ClickHouseHelperClient, String, String)` static helper using the identical `chc.queryV2(String.format(ddl, tableName))` body — the established per-file pattern is a **local** private static method, not a shared public one. The planner should follow that convention for `createTable` in the canary, and redirect `.query(chc, sql)` to the existing `ClickHouseTestHelpers.executeQueryIgnoreResult(chc, sql)` (identical semantics — runs a query, discards the result, read this session at `ClickHouseTestHelpers.java:88-95`).

**Primary recommendation:** Treat `ClickHouseBase.java` as one atomic re-composition (not a per-line merge), landed as the D-11 commit sequence: shared container + opt-out seam first (independent of the other two work items), the MBean sweep second, the image-default fix third, then the canary redirection — which additionally needs a local `createTable` helper and an `executeQueryIgnoreResult` redirect that no existing decision names — and finally the two-consecutive-clean-full-run gate.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Container lifecycle (start/stop/share) | Test infrastructure (`ClickHouseBase`) | — | Owns the Testcontainers `ClickHouseContainer` instance; all 27 test classes depend on it transitively |
| Database isolation per test class | Test infrastructure (`ClickHouseBase.setup()`) | ClickHouse server (via `CREATE DATABASE`) | Isolation is achieved server-side (`kafka_connect_test_<n>` per class), not via separate containers |
| MBean registration/cleanup | JVM platform MBean server (`MBeanServerUtils`, production) | Test infrastructure (`ClickHouseBase`'s `@AfterEach` sweep) | Production registers MBeans; test harness is responsible for not letting test-induced leaks cross into the next test — MBeans are JVM-scoped, not container-scoped, so the sweep's semantics are unaffected by container sharing |
| Docker-free gate enforcement | Build tooling (`build.gradle.kts`'s `testUnit` task + `@Tag("clickhouse")`) | — | `excludeTags("clickhouse")` is the mechanism; `@Tag` on `ClickHouseBase` is what every live test inherits |
| Toxiproxy network wiring (2 classes) | Test infrastructure (opt-out container path) | — | Structurally cannot share a network alias with 24 other classes without a seam; isolated by design |

## Standard Stack

No new external dependencies are introduced by this phase — it re-composes existing test infrastructure only. All libraries below are already pinned in `gradle/libs.versions.toml` and confirmed present in the resolved dependency graph by earlier phases (Phase 6 Recorded Fact 1-2).

### Core (already present, verified this session)
| Library | Version | Purpose | Verification |
|---------|---------|---------|---------------|
| `org.testcontainers:testcontainers-clickhouse` | 2.0.5 | `ClickHouseContainer` used by `ClickHouseBase` | `[VERIFIED: gradle/libs.versions.toml:23]` — read this session |
| `org.testcontainers:testcontainers-toxiproxy` | 2.0.5 | `ToxiproxyContainer` used by the 2 opt-out proxy tests | `[VERIFIED: gradle/libs.versions.toml:23,64]` — read this session |
| JUnit Jupiter | 5.14.4 (per REQUIREMENTS.md's locked decision table) | `@TestInstance(PER_CLASS)`, `@BeforeAll`/`@AfterAll`/`@AfterEach`, `TestExecutionExceptionHandler` extension point | `[CITED: REQUIREMENTS.md locked-decisions table]` |

### Package Legitimacy Audit

**Not applicable.** This phase adds zero new packages/dependencies — it is a pure re-composition of test-only Java source against already-resolved, already-verified artifacts (Testcontainers `2.0.5` was verified in Phase 6/7's dependency-graph read). No `package-legitimacy check` run was needed.

## Architecture Patterns

### System Architecture Diagram

```
                    ┌─────────────────────────────────────────┐
                    │   Gradle test JVM fork (1 of N)          │
                    │                                           │
   Test class A ───▶│  ClickHouseBase.setup() [@BeforeAll]     │
   Test class B ───▶│    if (sharedContainer == null)          │──▶ ClickHouseContainer.start()
   Test class C ───▶│        start it once, count it            │      (Testcontainers 2.0.5,
       ...          │    createDatabase(uniqueName)              │       one per JVM fork)
   Test class Y ───▶│  ── each class gets its OWN database ──   │
                    │                                           │
                    │  [@Test methods run against shared db]    │
                    │                                           │
                    │  ClickHouseBase.tearDown() [@AfterAll]    │
                    │    dropDatabase(uniqueName)  ◀── NOT       │
                    │    (container stays up for next class)     │    ch.stop()
                    │                                           │
                    │  [@AfterEach] sweepLeakedMBeans()          │──▶ MBeanServerUtils
                    │    query "com.clickhouse:...,*"            │      .unregisterMBean()
                    │    WARN + unregister each match            │
                    └─────────────────────────────────────────┘
                                      │
                    ┌─────────────────┴─────────────────┐
                    │  Opt-out classes (2 of 27):        │
                    │  ClickHouseSinkTaskSchemalessProxyTest │
                    │  ClickHouseSinkTaskWithSchemaProxyTest │
                    │  usesSharedContainer() → false      │
                    │  → own ClickHouseContainer + Network│
                    │  → own Toxiproxy on that Network    │
                    │  → does NOT increment shared counter│
                    └─────────────────────────────────────┘
```

A reader following the primary path: any of the 25 shared-container test classes reaches `setup()`, which lazily starts the one static container the first time any class needs it, then always creates a fresh per-class database. `tearDown()` drops only that database — the container survives for the next class in the same fork. `@AfterEach` runs after every test method regardless of pass/fail, sweeping any MBean a test forgot to unregister (e.g. because it threw between `start()` and `stop()`).

### Recommended Project Structure

No new directories — this phase edits existing files in place:
```
src/test/java/com/clickhouse/kafka/connect/sink/
├── ClickHouseBase.java                          # shared container, opt-out hook, sweep, tearDown drop
├── ClickHouseTestHarnessTest.java                # canary: API redirect + 2 new pins
├── ClickHouseSinkTaskSchemalessProxyTest.java    # opt-out override only
└── ClickHouseSinkTaskWithSchemaProxyTest.java    # opt-out override only
src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/
└── ClickHouseTestHelpers.java                    # CLICKHOUSE_VERSION_DEFAULT -> "latest"
build.gradle.kts                                  # delete the exclude("**/ClickHouseTestHarnessTest.java") line
```

### Pattern 1: Lazy static shared container over `@TestInstance(PER_CLASS)`

**What:** A `static volatile`-or-synchronized container field plus a start guard, layered on top of JUnit 5's per-class instance lifecycle (which upstream already uses). `@BeforeAll` stays an **instance** method (required by `PER_CLASS` for `@BeforeAll`/`@AfterAll` to be non-static), but the container itself is a class-level (`static`) resource shared across every test class that runs in the same JVM fork.

**When to use:** Any test class that needs a real ClickHouse instance but does not need network isolation from its siblings.

**Example (pattern, not a literal diff — re-derived from `git show pre-v1.6.0-sync:.../ClickHouseBase.java`, the reference for exact pre-merge semantics per CONTEXT.md's Reusable Assets, adapted onto the currently-read `ClickHouseBase.java:1-143`):**
```java
// Source: this session's read of ClickHouseBase.java (current, 143 lines) + pre-merge reference
private static final AtomicInteger SHARED_CONTAINER_STARTS = new AtomicInteger();
private static volatile ClickHouseContainer sharedDb;

protected boolean usesSharedContainer() {           // D-02's opt-out seam
    return true;
}

@BeforeAll
public void setup() throws IOException {
    // existing isCluster/isCloud guards unchanged (ClickHouseBase.java:38-44)
    if (!isCluster && !isCloud) {
        if (usesSharedContainer()) {
            synchronized (ClickHouseBase.class) {
                if (sharedDb == null) {
                    setupContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE);
                    sharedDb = this.db;
                    SHARED_CONTAINER_STARTS.incrementAndGet();   // only on the FIRST shared start
                } else {
                    this.db = sharedDb;                          // reuse, no counter increment
                }
            }
        } else {
            setupContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE); // opt-out: own container, counter untouched
        }
    }
    // existing tmpClient / setDatabase / createDatabase block unchanged (ClickHouseBase.java:46-50)
}

public static int sharedContainerStarts() {
    return SHARED_CONTAINER_STARTS.get();
}
```

### Pattern 2: `tearDown()` drops the database, does not stop the shared container

**What:** Under sharing, `@AfterAll` must not call `ch.stop()` for the 25 shared classes (that would kill the container for every subsequent class in the fork) but **must** drop the class's own database — `ClickHouseTestHarnessTest.classGetsFreshIsolatedDatabase` (read this session, lines 77-107) asserts zero *other* `kafka_connect_test_%` databases exist, which only holds if every prior class cleaned up after itself.

**Example:**
```java
// Source: this session's read of ClickHouseBase.java:73-88 (current tearDown, stops container unconditionally)
@AfterAll
protected void tearDown() {
    if (isCluster) {
        cluster.stop();
    } else if (!isCloud) {
        if (usesSharedContainer()) {
            try (var tmpClient = ClickHouseTestHelpers.createClient(getBaseProps())) {
                ClickHouseTestHelpers.dropDatabase(tmpClient, database);   // D-01: drop, don't stop
            }
            copyServerLogs("build/reports/tests/server_" + getClass().getSimpleName() + ".log"); // D-03
        } else {
            ClickHouseContainer ch = getDb();                              // opt-out: still owns its container
            if (ch != null) {
                ch.copyFileFromContainer(/* ... existing log copy ... */);
                ch.stop();
            }
        }
    }
}
```
Note: `ClickHouseTestHelpers.dropDatabase(chc, database)` already exists (`ClickHouseTestHelpers.java:410-418`, read this session) — no new helper needed for the drop itself.

### Pattern 3: `@AfterEach` MBean leak sweep, restored verbatim from the pre-merge shape

**What:** Query the platform MBean server for every registered connector MBean after each test method, WARN and unregister anything still present (a leak from a test that threw between `start()` and `stop()`), continue — do not fail the test.

**Example:**
```java
// Source: MBeanServerUtils.java:17-41 (read this session — registerMBean/unregisterMBean already exist,
// unmodified by this phase) + CONTEXT.md D-09a's restored-verbatim instruction
@AfterEach
protected void sweepLeakedMBeans() {
    try {
        Set<ObjectName> leaked = ManagementFactory.getPlatformMBeanServer()
                .queryNames(new ObjectName("com.clickhouse:type=ClickHouseKafkaConnector,*"), null);
        for (ObjectName name : leaked) {
            LOGGER.warn("Leaked MBean detected after test, unregistering: {}", name);
            MBeanServerUtils.unregisterMBean(name.toString());
        }
    } catch (MalformedObjectNameException e) {
        throw new RuntimeException(e);
    }
}
```
The `,*` wildcard suffix already matches `SinkTaskStatistics`'s per-topic composition (`getMBeanName() + ",topic=" + topic`, read this session at `SinkTaskStatistics.java:188-189`) without any pattern change — confirm this by running `ClickHouseTestHarnessTest`'s new per-topic pin (D-08) rather than widening the pattern speculatively.

### Pattern 4: Canary API redirection (D-07) plus the two gaps this session found

**What:** `ClickHouseTestHarnessTest.java` (193 lines, read in full this session) currently calls four things that do not exist on `ClickHouseBase`/`ClickHouseTestHelpers`: `createProps()`, `createClient(props)` (unqualified), `ClickHouseTestHelpers.createTable(...)`, `ClickHouseTestHelpers.query(...)`. D-07 names the first two; this session's read found the other two.

**Example:**
```java
// Before (current, does not compile - file is excluded from compileTestJava per build.gradle.kts:448)
Map<String, String> props = createProps();
ClickHouseHelperClient chc = createClient(props);
ClickHouseTestHelpers.createTable(chc, "harness_mbean_probe_table", "CREATE TABLE IF NOT EXISTS %s (id UInt8) ENGINE = MergeTree ORDER BY id");
ClickHouseTestHelpers.query(chc, String.format("INSERT INTO `%s` VALUES (1, unhex('...'))", table));

// After (D-07's redirect + this session's two additional gaps)
Map<String, String> props = getBaseProps();                              // D-07
ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);  // D-07
createTable(chc, "harness_mbean_probe_table",                            // new local private static helper,
        "CREATE TABLE IF NOT EXISTS %s (id UInt8) ENGINE = MergeTree ORDER BY id"); // matching the pattern in
                                                                           // SmtSinkLandingTest.java:39-45 et al.
ClickHouseTestHelpers.executeQueryIgnoreResult(chc,                      // existing helper,
        String.format("INSERT INTO `%s` VALUES (1, unhex('...'))", table)); // ClickHouseTestHelpers.java:88-95
```
The local `createTable` helper body (copy verbatim from any of the six sibling files, e.g. `SmtSinkLandingTest.java:39-45`, read this session):
```java
private static void createTable(ClickHouseHelperClient chc, String tableName, String createTableQuery) {
    try (Records ignored = chc.queryV2(String.format(createTableQuery, tableName))) {
        // table created
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
```

### Anti-Patterns to Avoid

- **Re-opening `ClickHouseBase` with `createProps()`/`createClient()` shims:** 12 fork test files were already migrated off these names in Phase 7 (D-06/D-09). Adding them back creates two ways to do one thing on the most conflict-prone test file in the tree.
- **Adding `ClickHouseTestHelpers.createTable` as a new public static method for a single caller:** six other files already duplicate a private `createTable` helper locally; following that established (if imperfect) convention keeps the diff isolated to the one file this phase actually needs to touch, consistent with AGENTS.md's "additive, rebasable" framing for this milestone. (If the planner judges de-duplicating all seven copies into one shared helper is worth doing, that is a larger, separate refactor outside this phase's stated file list — CONTEXT.md's "Files this phase touches directly" does not list the six sibling files.)
- **Widening the MBean sweep's ObjectName pattern speculatively:** the existing `,*` wildcard already matches per-topic names structurally (confirmed by reading `SinkTaskStatistics.java` this session) — change the pattern only if D-08's new pin actually demonstrates a miss.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Detecting a leaked MBean | A custom JMX diff/snapshot mechanism | `ManagementFactory.getPlatformMBeanServer().queryNames(ObjectName, null)` | Already the pattern used everywhere else in this codebase (`ClickHouseTestHarnessTest.mbeanCountForConnector`, read this session) |
| Running a query and discarding the result | A new "fire and forget" query helper | `ClickHouseTestHelpers.executeQueryIgnoreResult(chc, sql)` (already exists, `ClickHouseTestHelpers.java:88-95`) | Identical semantics to what the canary's non-existent `.query(...)` call needs |
| Dropping a per-class test database | Manual `DROP DATABASE` SQL string construction | `ClickHouseTestHelpers.dropDatabase(chc, database)` (already exists, `ClickHouseTestHelpers.java:410-418`, handles the cluster `ON CLUSTER` clause too) | Avoids duplicating the cluster-clause logic that already lives in one place |

**Key insight:** Nearly every primitive this phase needs (client creation, database create/drop, query execution, MBean query/unregister) already exists somewhere in the codebase — read this session — either on `ClickHouseTestHelpers` or `MBeanServerUtils`. The actual work is composition (wiring these into `ClickHouseBase`'s lifecycle correctly under sharing) and one small gap-fill (the canary's `createTable`/`.query` calls), not new infrastructure.

## Common Pitfalls

### Pitfall 1: `tearDown()`'s database drop is load-bearing, not tidiness (from CONTEXT.md, re-confirmed this session)
**What goes wrong:** If a shared-container class's `@AfterAll` fails to drop its database (e.g., an exception in the drop swallowed silently, or the drop simply omitted), the next class in the same fork inherits a server with extra `kafka_connect_test_%` databases still present.
**Why it happens:** Under D-01, `tearDown()` no longer gets isolation "for free" from stopping the container — the drop is now the only isolation mechanism between classes sharing a fork.
**How to avoid:** `ClickHouseTestHarnessTest.classGetsFreshIsolatedDatabase` (already written, `:77-107`, read this session) directly asserts `SELECT count() FROM system.databases WHERE name LIKE 'kafka_connect_test_%' AND name != '<mine>'` is zero. This is the acceptance test for the drop — it doesn't need to be re-invented.
**Warning signs:** A class-ordering-dependent failure (passes alone, fails when run after certain other classes in the same fork) is the signature symptom.

### Pitfall 2: The opt-out counter must not move for opt-out classes
**What goes wrong:** If `setupContainer()` is called unconditionally inside the opt-out branch using the same `SHARED_CONTAINER_STARTS.incrementAndGet()` call as the shared path, `ClickHouseTestHarnessTest.sharedContainerIsStartedOncePerJvmAndIsTheRequestedImage`'s `assertEquals(1, sharedContainerStarts())` becomes order-dependent — true only if the two proxy classes happen to land in a different fork than the harness test.
**Why it happens:** Copy-paste of the start logic without branching the counter increment.
**How to avoid:** The opt-out branch in Pattern 1 above calls `setupContainer(...)` directly, bypassing `SHARED_CONTAINER_STARTS.incrementAndGet()` entirely — verified structurally correct by inspection of the example, but must be pinned by D-08's new test, not just eyeballed.

### Pitfall 3: The two proxy classes' own `tearDown()` overrides call `super.tearDown()` then additionally stop Toxiproxy
**What goes wrong (session finding):** `ClickHouseSinkTaskSchemalessProxyTest.java:73-84` and `ClickHouseSinkTaskWithSchemaProxyTest.java` (both read this session) override `@AfterAll tearDown()`, call `super.tearDown()`, then stop their own `toxiproxy` container. If `super.tearDown()` is rewritten under D-01 to branch on `usesSharedContainer()` (Pattern 2 above), these two overrides continue to work unchanged **only if** the opt-out branch in the rewritten `tearDown()` still calls `ch.stop()` for their own container (not just drops the database) — otherwise their container leaks for the lifetime of the JVM fork.
**Why it happens:** Easy to write `tearDown()`'s opt-out branch as "same as before" without checking both call sites that depend on it.
**How to avoid:** Pattern 2's example above explicitly keeps `ch.stop()` in the opt-out branch — verify this by running the two proxy test classes as part of the phase's acceptance gate, not just the harness canary.

### Pitfall 4: `ClickHouseHelperClientTest.java:105-118` mutates server-global state (CREATE USER + GRANT ON *.*)
**What goes wrong:** Under container sharing, this class's `CREATE USER`/`GRANT ON *.*` statements now accumulate on a server that outlives the class (previously the container was thrown away after this one class finished).
**Why it happens:** Names are timestamped via `createTestUsername` so they don't collide, but nothing drops them.
**How to avoid:** Out of this phase's stated scope (CONTEXT.md does not list this file under "Files this phase touches directly") — recorded here as a known, accepted cost of D-01, not a defect to fix in Phase 10. Flag as a discretionary follow-up if the planner wants to add a cleanup, but do not treat it as blocking.

### Pitfall 5: Testcontainers 2.0.5's package/artifact rename is already absorbed
**What goes wrong (checked, not found):** Testcontainers 2.0 renamed module artifact IDs (`testcontainers-` prefix) and relocated container classes into per-module packages (e.g., `org.testcontainers.postgresql.PostgreSQLContainer` replacing the old `org.testcontainers.containers.PostgreSQLContainer`), and removed JUnit 4 support. `[CITED: testcontainers-java GitHub releases / migration blog posts, web search this session]`
**Why it doesn't apply here:** `ClickHouseBase.java`'s current imports (`org.testcontainers.clickhouse.ClickHouseContainer`, `org.testcontainers.containers.Network`) and the proxy tests' (`org.testcontainers.toxiproxy.ToxiproxyContainer`) already use the 2.0-era per-module packages — confirmed by reading all three files this session. This codebase runs pure JUnit 5, so the JUnit-4-removal change is a non-issue. No code path in this phase needs any Testcontainers-2.0-specific migration.
**Detection signal:** N/A — checked and ruled out this session; recorded so the planner doesn't spend time re-verifying it.

## Code Examples

See Architecture Patterns 1-4 above — all four are grounded in files read in full this session (`ClickHouseBase.java`, `ClickHouseTestHelpers.java`, `ClickHouseTestHarnessTest.java`, `MBeanServerUtils.java`, `SinkTaskStatistics.java` lines 160-200, `SmtSinkLandingTest.java` lines 25-45, both proxy test files' setup/teardown).

## State of the Art

| Old Approach (pre-merge fork) | Current Approach (post-merge, pre-Phase-10) | When Changed | Impact |
|--------------------------------|----------------------------------------------|---------------|--------|
| `@BeforeAll public static void setup()` — static shared container across the whole JVM | `@TestInstance(PER_CLASS)` + `@BeforeAll public void setup()` — one container per class, stopped in `@AfterAll` | Phase 6 merge (commit `673d29c3`) | Performance regression: 27 sequential container starts on a 2-core CI runner instead of 1; Phase 10 re-derives sharing on top of the new per-class instance lifecycle rather than reverting to the old static-lifecycle shape |
| `CLICKHOUSE_VERSION_DEFAULT = "latest"` | `CLICKHOUSE_VERSION_DEFAULT = "25.8.18.1"` | Phase 6 merge | Every local `./gradlew test` run since the merge (including Phases 8 and 9's green closes) ran against `25.8.18.1`, not `latest` — invisible because `tests.yaml:33` explicitly pins `latest` in CI, so only local runs drifted |
| No `PullPolicy` set (implicit default cached pull) | Same — `alwaysPull()` absent from merged `setupContainer` | Phase 6 merge | A floating `latest` tag is never re-pulled locally once cached — D-05 restores `alwaysPull()` conditionally |

**Deprecated/outdated:** None — this phase does not touch any deprecated API surface.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | HARN-05 (`SchemaTestData`/`SchemalessTestData` single-copy, `FromVersionConditionExtension` package move) is fully satisfied — re-affirmed from CONTEXT.md's measured-state table but not independently re-verified by this session's own `git ls-files`/grep pass (only `ClickHouseTestHelpers.java`'s sibling directory was directly observed) | Phase Requirements table, HARN-05 row | Low — if stale, the planner's own D-10 "re-run each command at execution time" instruction catches it before the phase closes; not a design risk either way |
| A2 | No production connector combines the shared-container change with anything security-sensitive beyond the already-known `ClickHouseHelperClientTest` global-state mutation (Pitfall 4) | Common Pitfalls, Security Domain | Low — this phase touches test-only code; the one known caveat is already carved out as accepted cost per CONTEXT.md's scope boundary |

**If this table is empty:** N/A — two low-risk items logged above, both explicitly deferred to execution-time re-verification per D-10's own instruction, not blocking planning.

## Open Questions

1. **Should the canary's local `createTable` helper be de-duplicated across the seven files that now carry a copy (six existing + this canary), given AGENTS.md's DRY guidance?**
   - What we know: Six sibling files already duplicate this exact 5-line helper; CONTEXT.md's "Files this phase touches directly" list does not include any of the six.
   - What's unclear: Whether landing a seventh copy (matching convention) or a shared `ClickHouseTestHelpers.createTable(...)` (breaking convention, touching one more file than CONTEXT.md scoped) is the better call for this phase specifically.
   - Recommendation: Match the established convention (local private copy) to stay inside CONTEXT.md's stated file list; leave de-duplication as a separate, later cleanup if desired — it's orthogonal to any of the seven HARN requirements.

2. **Exact form of the opt-out hook (`protected boolean` vs. `@Tag` vs. annotation) — explicitly left to the planner by CONTEXT.md's Claude's Discretion.**
   - What we know: The constraint that matters is D-02's counter rule (opt-out classes must not increment `SHARED_CONTAINER_STARTS`).
   - What's unclear: Nothing blocking — this is a genuinely open style choice.
   - Recommendation: A `protected boolean usesSharedContainer()` override (Pattern 1 above) is the smallest diff and matches the existing override style already used for `setup()`/`tearDown()` in the two proxy classes.

## Sources

### Primary (HIGH confidence — direct repository reads, this session)
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` — read in full (143 lines)
- `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` — read in full (461 lines)
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java` — read in full (193 lines)
- `src/main/java/com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.java` — read in full (43 lines)
- `src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java` — lines 160-200 read
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java` and `ClickHouseSinkTaskWithSchemaProxyTest.java` — setup/teardown regions read (lines 1-100)
- `src/test/java/com/clickhouse/kafka/connect/sink/SmtSinkLandingTest.java` — `createTable` helper pattern, lines 25-45
- `build.gradle.kts` — lines 180-260 (test task config, `testUnit`), 420-450 (`ClickHouseTestHarnessTest.java` exclusion)
- `gradle/libs.versions.toml` — Testcontainers `2.0.5` pin, lines 23, 62-65
- `.planning/MERGE-CHECKLIST.md` — Section 1 conflict rows, `Recorded Facts` 1-11
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileTestJava compileTestFixturesJava compileIntegrationTestJava` — run this session, `BUILD SUCCESSFUL`

### Secondary (MEDIUM confidence)
- `.planning/phases/10-test-harness-reconciliation/10-CONTEXT.md` — this phase's locked decisions (D-01 through D-11), the majority of this research's architectural direction
- `.planning/REQUIREMENTS.md` — HARN-01..07 wording, locked decisions table
- `.planning/STATE.md` — historical fact confirmation (six/seven silently-vanishing behaviours list)
- `.planning/research/PITFALLS.md` — Pitfall 11 (`ClickHouseBase.java` contract loss), Pitfall 12 (helper duplication), Pitfall 13 (integrationTest compile rot) — all pre-dating and consistent with this session's direct reads
- WebSearch: "Testcontainers 2.0.5 Java release notes breaking changes migration from 1.x" and "Testcontainers 2.0 major version breaking changes migration guide" — package rename / JUnit-4-removal facts, cross-checked against this repo's already-2.0-style imports

### Tertiary (LOW confidence)
- `.planning/codebase/TESTING.md` — describes the **pre-merge** static-lifecycle `ClickHouseBase` shape (stale relative to the current merged tree); used only as historical context for what "restore" means, not as a description of current state

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new dependencies, all versions read directly from `gradle/libs.versions.toml` this session
- Architecture: HIGH — every pattern grounded in a file read this session; the two gaps (canary's missing `createTable`/`.query`) were discovered, not assumed
- Pitfalls: HIGH for Pitfalls 1-4 (grounded in files read this session and CONTEXT.md's already-locked reasoning); MEDIUM for Pitfall 5 (Testcontainers 2.0 general migration notes are web-sourced, cross-checked against this repo's imports but not against the specific 2.0.5 changelog)

**Research date:** 2026-09-20
**Valid until:** Effectively immediate — this research describes the exact current state of a tree mid-milestone; any commit landing before the plan executes invalidates the line-number citations (re-read the files at execution time rather than trusting line numbers verbatim).

## Runtime State Inventory

**Not applicable — this is test-infrastructure work, not a rename/refactor/migration of production identifiers.** No stored data, live service config, OS-registered state, secrets, or build artifacts carry a renamed string as part of this phase. The one adjacent runtime-state concern (`ClickHouseHelperClientTest`'s accumulating `CREATE USER`/`GRANT` statements under a now-shared container) is documented under Common Pitfalls § Pitfall 4 as an accepted, out-of-scope cost rather than a migration this phase must perform.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker (via colima) | All 27 `ClickHouseBase`-derived live tests | ✓ (this session — `docker info` succeeded; colima itself reported not running via its own CLI, but the Docker daemon it manages answered) | — | — |
| JDK 17 toolchain | Test compilation and execution (`javaLauncher` in `build.gradle.kts`) | ✓ | — | — |
| Gradle wrapper | Build orchestration | ✓ (`./gradlew` ran successfully this session) | 9.6.1 (per Phase 6 Recorded Facts) | — |
| `TESTCONTAINERS_HOST_OVERRIDE` unset | Ryuk reaper functioning correctly | ⚠ Known hazard — this shell sets it to the literal string `"null"` | — | Always run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` (already codified in `.planning/config.json`'s `build_command`/`test_command`) |

**Missing dependencies with no fallback:** None.

**Missing dependencies with fallback:** `TESTCONTAINERS_HOST_OVERRIDE` — mitigated by the `env -u` prefix already standard in this repo's tooling.

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-------------------|
| V2 Authentication | No | This phase touches no authentication code path — test-harness-only |
| V3 Session Management | No | N/A |
| V4 Access Control | Marginal | `ClickHouseHelperClientTest.java:105-118`'s `CREATE USER`/`GRANT ON *.*` runs against an ephemeral test-only ClickHouse container — no production access-control surface is touched. Documented as an accepted cost of container sharing (Pitfall 4), not fixed in this phase |
| V5 Input Validation | No | No new user-facing input paths introduced |
| V6 Cryptography | No | No cryptographic code touched |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|----------------------|
| Server-global state (users/grants) accumulating across test classes under a shared container | Information Disclosure (low severity — test-only, ephemeral container destroyed at JVM-fork end) | Timestamped usernames already prevent collision (`createTestUsername`, existing); no production exposure since the container never survives past the test run |
| MBean leak crossing into an unrelated subsequent test | Denial of Service (test-suite-local only — `InstanceAlreadyExistsException` fails the *next* test, not a production system) | The `@AfterEach` sweep this phase restores (D-09a) is exactly this mitigation |

**Note:** This phase changes zero production code paths (confirmed — `.planning/phases/10-test-harness-reconciliation/10-CONTEXT.md`'s "Files this phase touches directly" lists only test sources, one test-fixture constant, and `build.gradle.kts`'s exclusion list). `security_enforcement: true` is satisfied by the above scoped review; a full `/gsd-secure-phase 10` pass is not expected to find production-facing threats given this scope, but remains available if the user wants explicit sign-off.
