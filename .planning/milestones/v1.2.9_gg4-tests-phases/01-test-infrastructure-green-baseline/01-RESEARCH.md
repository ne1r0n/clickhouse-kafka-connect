# Phase 1: Test Infrastructure & Green Baseline - Research

**Researched:** 2026-09-15
**Domain:** Java/Gradle test-harness engineering — Testcontainers container lifecycle, JUnit 5 tagging, JMX MBean lifecycle, JaCoCo coverage wiring — for a Kafka Connect sink connector fork.
**Confidence:** HIGH — every claim below was either (a) read directly from this repo's source this session with file:line citations, (b) confirmed by running a read-only command in this repo/environment this session, or (c) confirmed against an official/primary source (Gradle docs, Lombok docs, JUnit docs, Testcontainers docs, GitHub API/issue tracker) fetched this session. Claims that rest only on prior research-doc text or training knowledge are explicitly tagged `[ASSUMED]` and listed in the Assumptions Log.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Baseline failure policy (INFRA-01)**
- **D-01:** Run the untouched suite first with `CLICKHOUSE_VERSION=latest`, `CLIENT_VERSION=V2`, and record the raw result before changing the harness. If an existing test fails only because its expectation is outdated for `latest` (type rendering, error text, defaults) and the connector is correct, update the assertion to match `latest` (test-only change; record before/after + reason in the phase summary).
- **D-02:** If a baseline failure comes from a genuine bug in upstream (non-fork) main code, make a small targeted main-code fix ("small" = a few lines, rebase-friendly, mirrors upstream's fix if one exists). Anything larger stops for a user decision — do not disable the test and move on.
- **D-03:** Leave every existing `@Disabled` test as-is: the 2 upstream Variant tests in `ClickHouseSinkTaskWithSchemaTest` ("requires a flag on the instance"), the proxy Variant test, the `JsonColumnTest` case (belongs to Phase 3), the perf tests (`InsertFormatPerformanceTest`, `ConvertToMapOrJsonPerformanceTestJson`). Do not re-enable them this phase.
- **D-04:** Flaky tests (timing, `Thread.sleep`, colima resource pressure): rerun to confirm the flake, then fix the root cause (poll instead of sleep, or lower fork count if resources are the cause). No test-retry plugin — JaCoCo stays the only new build dependency. Do not `@Disabled` a flaky test to get green.

**`latest` image, version smoke test, proxy tests, isolation (INFRA-02, INFRA-03)**
- **D-05:** Change the default image to `latest` (`CLICKHOUSE_VERSION_DEFAULT` in `ClickHouseTestHelpers`). Keep the `CLICKHOUSE_VERSION` env override. The shared container uses `PullPolicy.alwaysPull()`, so local `latest` matches CI. `clickhouse/clickhouse-server:latest` is not cached locally today; only `25.3` and `23.8` are.
- **D-06:** Server-version smoke test: numeric tag → `SELECT version()` must start with the tag; `latest` → `version()` must equal the version label/metadata of the pulled image. The test logs the actual server version so the baseline summary can record it. Covers the shared container only. *Research flag: check whether the ClickHouse image carries a usable version label; if not, propose the closest equivalent check.* (See Common Pitfalls — this could not be confirmed to exist.)
- **D-07:** The two Toxiproxy classes (`ClickHouseSinkTaskWithSchemaProxyTest`, `ClickHouseSinkTaskSchemalessProxyTest`) stay on ClickHouse 23.8, a documented exception to "`latest` only" (ClickHouse issue #58828, absolute-form URIs through Toxiproxy, still open). Give them their own container field and lifecycle — they must never assign, overwrite, or stop the shared `latest` singleton. Today both assign the static `db` in their own `@BeforeAll`, and the base `@AfterAll` stops it — that is the conflict to remove. Still ClickHouse-backed and tagged (D-12).
- **D-08:** Fresh database per test class on the shared container. `ClickHouseBase` creates a unique database in `@BeforeAll` and drops it in `@AfterAll`, replacing today's single static `kafka_connect_test_<ts>`. Reason: fixed table names are reused across classes that will share a container, and later table-mapping assertions must only see their own class's tables. No upstream table renames.

**Lifecycle retrofit scope (INFRA-04, INFRA-05, INFRA-06)**
- **D-09:** Base-class safety net plus a rule for new tests. Existing upstream test files are not retrofitted. An `@AfterEach` in `ClickHouseBase` unregisters any leftover `com.clickhouse:type=ClickHouseKafkaConnector,*` MBeans and logs each one. New and touched tests must wrap `start`/`put`/`stop` in try/finally (or `@AfterEach`) and use a unique connector name. A deliberate-failure test proves that a test failing between `start()` and `stop()` leaves no stale MBean for later tests. MBean ids come from a static `AtomicInteger` (`ProxySinkTask.java:31`), so leaks linger rather than crash. `ClickHouseSinkTaskTest` already has 2 `start()` calls and 1 `stop()`.
- **D-10:** Opt-in unique connector-name helper in `ClickHouseBase` (e.g. class name + method name + counter) that new tests call. `createProps()` stays unchanged so existing tests keep their current MBean names. Phase 4's JMX-02 test (`connector=null` when `name` prop absent) needs no workaround.
- **D-11:** UTF-8 now in test helpers only. Change: `ClickHouseTestHelpers.java:158` (`getAllRowsAsJson`) and `:252` (`validateRows`) get explicit `StandardCharsets.UTF_8`. Not changed in Phase 1: main-code readers `ClickHouseHelperClient.java:370`/`:417` — fixed in Phase 3 when TYPE-07/DESC tests prove the bug.
- **D-12:** A single `@Tag("clickhouse")` on `ClickHouseBase`. JUnit's `@Tag` is `@Inherited`, so all 14 current subclasses (proxy classes included) and future ones are covered without editing upstream test files. `testUnit` excludes this tag. Nothing outside `ClickHouseBase` subclasses starts a Testcontainer today.

**Fork-changed coverage view (COV-01, COV-02)**
- **D-13:** The fork-focused view uses a hand-picked, named include list in `build.gradle.kts`, mapped to the 11 fork additions (globs include inner classes, e.g. `Column*`). Update the list when the fork adds classes.
  - Transforms (`com/clickhouse/kafka/connect/transforms/`): `TableOperationFilter`, `HeadersToValue`, `HeaderToValue`, `HeaderToValueConfig`, `ConvertToMapOrJson`
  - `com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient`
  - `com/clickhouse/kafka/connect/sink/db/mapping/`: `Column`, `Type`, `Table`
  - `com/clickhouse/kafka/connect/sink/db/ClickHouseWriter`
  - `com/clickhouse/kafka/connect/sink/`: `ClickHouseSinkConfig`, `ProxySinkTask`, `ClickHouseSinkTask`
  - `com/clickhouse/kafka/connect/ClickHouseSinkConnector`
  - `com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils`

  Rejected: deriving the list from `git diff main...custom_build` (import-only noise; no `main` ref on shallow CI checkouts), and package globs (pull in upstream-only `KeyToValue`, `ExtractTopic`, `ClickHouseFieldDescriptor`).
- **D-14:** A second `JacocoReport` task reads the same `test` exec data, filtered to D-13, and writes its own HTML+XML (e.g. `build/reports/jacoco/fork/`). Runs automatically after `test`, next to the full `jacocoTestReport`. Phase 5 CI can publish or summarize its XML directly.
- **D-15:** Add a root `lombok.config` with `config.stopBubbling = true` and `lombok.addLombokGeneratedAnnotation = true`. JaCoCo then filters generated getters, setters, and builders via `@lombok.Generated`. Upstream has no `lombok.config`, so there is no rebase conflict. The shipped jar only gains a class-retention annotation; no runtime change. Verify by looking at `Column` in the generated report.
- **D-16:** JaCoCo toolVersion `0.8.15`. The agent runs on the `test` task only. Explicitly disable the agent on `testUnit` and `integrationTest`. Exclude `testUnit` from `check` — it is a subset of `test`; the existing `tasks.withType<Test>` loop in `build.gradle.kts` would otherwise add it. Wire the reports to `test` (`finalizedBy`), never to `check`. Leave the pre-existing `check` → `integrationTest` dependency from that loop untouched (upstream, out of scope).

### Claude's Discretion
- Shared-container cleanup: rely on Ryuk or add a JVM shutdown hook. Pick whatever works reliably with colima.
- Exact names: the unique connector-name helper, the fork report task (`jacocoForkReport` or similar), the per-class database naming scheme, and the report output directory.
- Where the version smoke test lives: a new tagged class, or a test in an existing base-derived class.
- How the proxy classes get their own container field: separate static field plus an overridden `createProps`/teardown, or a small base hook. Keep the diff minimal.
- How `testUnit` gets its test classpath and excludes the tag, and how it is kept out of `check` in the Kotlin DSL.
- Whether to lower `maxParallelForks` locally if colima resource pressure causes flakes (D-04). Record the evidence in the summary.

### Deferred Ideas (OUT OF SCOPE)
- Explicit UTF-8 in main-code `describeTable` readers (`ClickHouseHelperClient.java:370`, `:417`): Phase 3, once the TYPE-07/DESC non-ASCII tests prove the bug.
- Re-enabling upstream Variant `@Disabled` tests on `latest`: not a fork addition. Left as-is; could be a future upstream-sync item.
- Pre-existing `check` → `integrationTest` dependency (via the `tasks.withType<Test>` loop): upstream quirk, left untouched this milestone.
- Moving the Toxiproxy tests to `latest`: blocked by ClickHouse #58828 — **confirmed still open this session** (fetched 2026-09-15; title "does not support URI with absolute-form #58828", state: Open). Revisit when that issue is fixed.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| INFRA-01 | Baseline `./gradlew test` against `latest`×V2 run and recorded; pre-existing failures fixed | Pitfall 1 (`CLICKHOUSE_VERSION_DEFAULT`), D-01/D-02 code paths, Environment Availability (colima state verified this session) |
| INFRA-02 | ClickHouse-backed tests default to `latest`; smoke test asserts running server version matches requested image | Common Pitfalls (no usable version label exists — verified via GitHub issue #54224 and current `Dockerfile.ubuntu`), Code Examples (smoke test pattern), Don't Hand-Roll |
| INFRA-03 | `ClickHouseBase` reuses one container per JVM fork instead of per class | Architecture Pattern 1 (singleton guard), verified current per-class behavior in `ClickHouseBase.java:28-38` |
| INFRA-04 | ClickHouse-backed classes tagged; `testUnit` runs everything else without Docker | Architecture Pattern 2, verified `@Tag` `@Inherited` since JUnit 5.3.0 (project uses 6.0.2), Common Pitfall (testUnit swept into `check`) |
| INFRA-05 | New/touched sink-task tests always stop the task and use unique connector names; no MBean leak | Architecture Pattern 6, verified `MBeanServerUtils.java` throw/no-op behavior, verified `ProxySinkTask.java:31/71-79`, verified `JsonColumnTest.java` no-try-finally pattern |
| INFRA-06 | Test helpers decode ClickHouse responses as explicit UTF-8 | Verified exact line numbers `ClickHouseTestHelpers.java:158,252`, Code Examples |
| COV-01 | JaCoCo `0.8.15` on `test` only, HTML+XML | Verified JaCoCo 0.8.15 exists on Maven Central, verified current `build.gradle.kts` has no `jacoco` plugin today, Code Examples |
| COV-02 | Coverage excludes Lombok-generated code (verified via `Column`); fork-changed-class view | Verified `Column.java`/`Table.java`/`ClickHouseSinkConfig.java`/`ClickHouseHelperClient.java` Lombok annotations, verified Lombok 1.18.42 already defaults `addLombokGeneratedAnnotation=true` since 1.18.34 |
</phase_requirements>

## Summary

Phase 1 is pure test-harness engineering on a stack that's already fixed (JUnit Jupiter 6.0.2, Mockito, Testcontainers 1.21.4, Gradle 8.14 Kotlin DSL) — no new frameworks are needed, and the CONTEXT.md decisions (D-01 through D-16) already specify *what* to build. This research's job was to verify every load-bearing technical claim behind those decisions against the actual source and against external ground truth, since several of them turned out to need correction or a fallback:

The single biggest correction: **the ClickHouse Docker image carries no usable version label.** D-06 asked research to check this. Confirmed this session — the current `docker/server/Dockerfile.ubuntu` on ClickHouse's `master` branch has zero `LABEL` instructions, and GitHub issue #54224 documents that the `com.clickhouse.build.version` label users expect is simply absent from `docker inspect` output; `org.opencontainers.image.version`, where present at all, reflects the Ubuntu base image version, not the ClickHouse version. The smoke test therefore cannot compare `SELECT version()` against image metadata for the `latest` tag — see Common Pitfalls and Code Examples for the closest equivalent (a well-formed-version-string assertion plus logging, since that's what D-06 actually needs for the baseline summary).

Every other locked decision checked out against the real source: the container-per-class behavior, the single shared `kafka_connect_test_<ts>` database, the `MBeanServerUtils.registerMBean` hard-throw on collision, `ClickHouseSinkTaskTest`'s 2-start/1-stop imbalance, `JsonColumnTest`'s missing try/finally, the exact UTF-8-missing line numbers in `ClickHouseTestHelpers`, the 14 `ClickHouseBase` subclasses, and both proxy test classes' static-`db`-overwrite conflict were all confirmed by reading the files this session. One new risk was found that CONTEXT.md doesn't mention: the existing `tasks.withType<Test> { tasks.getByName("check").dependsOn(this) }` loop in `build.gradle.kts` (lines 171-173) will silently pull the new `testUnit` task into `./gradlew check` unless the loop is guarded by task name — this directly conflicts with D-16's "exclude `testUnit` from `check`" requirement and needs a one-line fix in the same loop, not a separate mechanism. A second favorable finding: Lombok 1.18.42 (already in this build) defaults `addLombokGeneratedAnnotation=true` since 1.18.34, so the `Column`/`Table`/`ClickHouseSinkConfig` Lombok-generated methods are likely **already** excludable by JaCoCo before `lombok.config` is even added — D-15's file still matters for `config.stopBubbling` and for pinning the behavior against a future Lombok downgrade, but the verification step ("inspect `Column` in the report") is the real gate, not the file's mere presence.

**Primary recommendation:** implement the 16 locked decisions as specified, using the container-singleton guard + `@Tag`/`testUnit` split + MBean `@AfterEach` sweep + `lombok.config` + dual `JacocoReport` tasks pattern below; treat the version smoke test as "assert well-formed + log", not "compare against image metadata"; and add the one-line `check`-loop guard so `testUnit` doesn't leak into `check`.

## Architectural Responsibility Map

This project has no browser/frontend/CDN tiers — it's a Kafka Connect connector plus its test harness. Tiers below are the domain-appropriate analogue.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Container lifecycle (start/reuse/stop) | Test Harness (`ClickHouseBase`) | External Service (Docker/colima) | JUnit lifecycle hooks (`@BeforeAll`/`@AfterAll`, shutdown hook) own *when* a container starts/stops; Docker just executes the request |
| Docker-free unit test selection (`testUnit`) | Build System (Gradle `Test` task + JUnit Platform tag filter) | Test Harness (`@Tag` placement on `ClickHouseBase`) | The exclusion logic lives in the Gradle task config; the tag that makes exclusion possible lives on the base test class |
| MBean leak prevention | Test Harness (`@AfterEach` sweep + try/finally discipline in new tests) | Connector Main Code (`MBeanServerUtils`, `ProxySinkTask`) | Main code's register/throw/unregister semantics are unchanged this phase (D-09 is explicit: base-class safety net, not a main-code fix) — the fix is entirely in the test tier |
| UTF-8 decoding of ClickHouse HTTP responses | Test Harness (`ClickHouseTestHelpers`) | — | Main-code readers (`ClickHouseHelperClient`) are explicitly deferred to Phase 3 (D-11) |
| Coverage instrumentation & reporting | Build System (JaCoCo Gradle plugin, `lombok.config`) | — | Zero runtime/main-code change; purely build-file and root-config additions |
| Server-version smoke test | Test Harness (new/existing tagged test class) | External Service (the pulled Docker image is the subject under test) | The assertion runs inside a JUnit test; what it's asserting *about* is the container's server |
| Per-class database isolation | Test Harness (`ClickHouseBase`) | External Service (ClickHouse executes `CREATE`/`DROP DATABASE`) | Database naming/lifecycle policy is a test-harness decision; the server just executes the DDL |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Gradle `jacoco` core plugin | bundled with Gradle 8.14 (project's pinned wrapper — `gradle/wrapper/gradle-wrapper.properties:3`, `[VERIFIED: gradle-wrapper.properties:3]`) | Line/branch coverage instrumentation, HTML/XML reports | Core plugin, `id("jacoco")` — no Maven coordinate to add; already ships with the pinned Gradle distribution |
| JaCoCo tool version override | `0.8.15` | The actual coverage engine the plugin drives | Gradle 8.14 bundles JaCoCo default `0.8.13` `[CITED: gradle/gradle JacocoPlugin.java source, per prior STACK.md research]`; `0.8.15` confirmed to exist on Maven Central this session (`[VERIFIED: repo1.maven.org/maven2/org/jacoco/org.jacoco.core/0.8.15/]`, directory listing returned `org.jacoco.core-0.8.15-javadoc.jar` dated 2026-06-04) |
| JUnit Jupiter | `6.0.2` (via `junit-bom`) | Test runner, `@Tag`, `@AfterEach` | Already the project's runner. `[VERIFIED: build.gradle.kts:68,121-122]` — `junitJupiterVersion = "6.0.2"`, `testImplementation(platform("org.junit:junit-bom:...6.0.2"))`. `@Tag` is `@Inherited` since JUnit 5.3.0 `[CITED: junit-team/junit5 issue #1158 + junit.org Tag Javadoc]` — 6.0.2 is far past that fix, so D-12's single-tag-on-base-class plan is sound |
| Testcontainers | `1.21.4` (existing) | ClickHouse/Toxiproxy containers | `[VERIFIED: build.gradle.kts:105,129,133]` — unchanged this phase per project constraint ("reuse existing Testcontainers setup") |
| `org.testcontainers.images.PullPolicy` | ships with Testcontainers 1.21.4 core | `PullPolicy.alwaysPull()` for the D-05 "local `latest` matches CI" guarantee | `[CITED: java.testcontainers.org/features/advanced_options/]` — `GenericContainer.withImagePullPolicy(PullPolicy.alwaysPull())`. `ClickHouseContainer extends JdbcDatabaseContainer<ClickHouseContainer>` `[CITED: testcontainers-java GitHub source, modules/clickhouse/.../ClickHouseContainer.java]`, and `JdbcDatabaseContainer` extends `GenericContainer`, so the fluent method is available on `db` unchanged |
| Lombok | `1.18.42` (existing) | `@Builder`/`@Getter`/`@Setter`/`@Accessors` on `Column`, `Table`, `ClickHouseSinkConfig`, `ClickHouseHelperClient` | `[VERIFIED: build.gradle.kts:93-94]`. `addLombokGeneratedAnnotation` defaults **true** since Lombok `1.18.34` `[CITED: projectlombok.org/api/lombok/ConfigurationKeys + search cross-reference]` — 1.18.42 > 1.18.34, so `@lombok.Generated` stamping is likely already active even before `lombok.config` is added (see Common Pitfalls) |

### Supporting
No new test library is needed — MBean assertions use the JDK's own `java.lang.management`/`javax.management` (already implicitly available, no new dependency), and JUnit tag filtering is built into `junit-platform-launcher` (already a transitive dependency of the existing `junit-jupiter`/`junit-platform-runner` test dependencies).

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Core `jacoco` Gradle plugin | Third-party JaCoCo wrapper plugins | Never — this is a single-module build; the core plugin needs zero new Maven coordinate |
| `PullPolicy.alwaysPull()` for `latest` | A manual `docker pull` step before Gradle runs | Adds a non-Gradle-managed step; `alwaysPull()` is declarative and colocated with the container definition (D-05 explicitly names this API) |
| JUnit `@Tag`/`excludeTags` for `testUnit` | A separate `src/testUnit/java` source set | Massive restructuring for zero benefit — `@Tag` is additive (one annotation on `ClickHouseBase`, inherited); a new source set would require moving/duplicating every existing test file |
| Test-retry plugin for flaky tests (D-04 explicitly rejects this) | `org.gradle.test-retry` plugin | Explicitly rejected by CONTEXT.md — "No test-retry plugin. JaCoCo stays the only new build dependency." Root-cause fixes only |

**Installation:** No `npm install`-equivalent. The only new artifacts are: (1) `jacoco { toolVersion = "0.8.15" }` in `build.gradle.kts` (core plugin, resolved by Gradle from Maven Central automatically), and (2) a new root `lombok.config` file (not a dependency — a Lombok config file).

**Version verification performed this session:**
```
$ curl -s "https://repo1.maven.org/maven2/org/jacoco/org.jacoco.core/0.8.15/" | grep -o 'org.jacoco.core-0.8.15[^"]*'
org.jacoco.core-0.8.15-javadoc.jar   (2026-06-04)
```
`[VERIFIED: repo1.maven.org]` — 0.8.15 is published and resolvable.

## Package Legitimacy Audit

This phase adds no npm/PyPI/crates dependencies — the only new artifact is the JaCoCo Gradle plugin, resolved from Maven Central by groupId `org.jacoco` (an ~18-year-old project maintained under the Eclipse Foundation, not a candidate for slopsquatting/hallucination risk). The automated `package-legitimacy check` seam targets npm/PyPI/crates ecosystems and does not cover Maven; a manual equivalent check was performed instead.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| `org.jacoco:org.jacoco.core` (+ agent/ant/report modules) | Maven Central | ~18 years (JaCoCo project began 2009; current releases active through 2026-06) | Not tracked per-artifact on Maven Central, but is the de facto standard JVM coverage tool bundled by Gradle itself | `github.com/jacoco/jacoco` | OK — `[VERIFIED: repo1.maven.org listing this session]` | Approved — resolved automatically via `id("jacoco")` + `toolVersion`, no explicit `dependencies{}` entry needed |

**Packages removed due to `[SLOP]` verdict:** none
**Packages flagged as suspicious `[SUS]`:** none

## Architecture Patterns

### System Architecture Diagram

```
                    ./gradlew test                          ./gradlew testUnit (NEW)
                          │                                          │
                          ▼                                          ▼
              ┌────────────────────────┐                 ┌───────────────────────┐
              │  Test task (existing)   │                 │  testUnit task (NEW)   │
              │  useJUnitPlatform()      │                │  excludeTags("clickhouse")│
              │  maxParallelForks=cores/2│                │  same source set,       │
              └───────────┬─────────────┘                 │  same runtimeClasspath  │
                          │                                └───────────┬────────────┘
                          ▼                                            │
      ┌───────────────────────────────────────┐                       │
      │ Gradle test JVM fork (1..N,             │                      │
      │ maxParallelForks slots)                 │                      │
      │                                          │                      │
      │  Fork picks up a class:                 │                      │
      │   ┌────────────────────────────────┐    │                      │
      │   │ @Tag("clickhouse") on            │    │  no @Tag("clickhouse")│
      │   │ ClickHouseBase (INHERITED)       │◀───┼──── excluded ────────┘
      │   │  → all 14 subclasses tagged      │    │
      │   └───────────┬──────────────────────┘    │
      │               │ first class in this fork   │
      │               ▼                            │
      │   ClickHouseBase.setup() [@BeforeAll]       │
      │     if (db == null) { start container }     │  ← Pattern 1: singleton guard
      │     fresh per-class database (D-08)          │  ← replaces shared kafka_connect_test_<ts>
      │               │                              │
      │               ▼                              │
      │   Test methods run:                          │
      │     start() → put() → stop()  [try/finally]  │  ← Pattern 6: MBean-safe lifecycle
      │     unique connector name (D-10)              │
      │               │                              │
      │               ▼                              │
      │   @AfterEach: sweep com.clickhouse:type=      │  ← Pattern 6: base-class safety net
      │     ClickHouseKafkaConnector,* MBeans          │
      │               │                              │
      │               ▼                              │
      │   Next class in same fork reuses `db`         │  ← no restart cost
      │               │                              │
      │               ▼                              │
      │   JVM exit → shutdown hook / Ryuk stops db     │
      └───────────────┬───────────────────────────────┘
                       │
                       ▼ (test task only — NOT testUnit, NOT integrationTest)
          JaCoCo agent writes build/jacoco/test.exec
                       │
          ┌────────────┴─────────────┐
          ▼                          ▼
  jacocoTestReport            jacocoForkReport (NEW, D-14)
  (all src/main classes,      (D-13's named include list only)
   Lombok-generated methods
   excluded via lombok.config)
          │                          │
          ▼                          ▼
  build/reports/jacoco/test/  build/reports/jacoco/fork/
```

### Recommended Project Structure
```
build.gradle.kts                    # + jacoco plugin/config, testUnit task, check-loop guard (MODIFIED)
lombok.config                       # NEW — root, config.stopBubbling=true, addLombokGeneratedAnnotation=true
src/test/java/com/clickhouse/kafka/connect/sink/
├── ClickHouseBase.java             # MODIFIED — singleton guard, per-class DB, @Tag, @AfterEach MBean sweep,
│                                    #   unique-connector-name helper
├── ClickHouseSinkTaskWithSchemaProxyTest.java   # MODIFIED — own container field/lifecycle (D-07)
├── ClickHouseSinkTaskSchemalessProxyTest.java   # MODIFIED — own container field/lifecycle (D-07)
├── helper/ClickHouseTestHelpers.java            # MODIFIED — CLICKHOUSE_VERSION_DEFAULT="latest",
│                                                 #   explicit UTF-8 at getAllRowsAsJson/validateRows
└── <new version-smoke-test class, name at Claude's discretion>   # NEW — D-06
```

### Pattern 1: Container-per-fork singleton (verified current behavior first)

**Current state, read directly this session (`ClickHouseBase.java:24-38`):**
```java
protected static ClickHouseContainer db = null;
...
@BeforeAll
public static void setup() throws IOException  {
    if (database == null) {
        database = String.format("kafka_connect_test_%s", System.currentTimeMillis());
    }
    if (isCloud) { initialPing(); return; }
    db = new ClickHouseContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE).withPassword("test_password")...;
    db.start();
}
```
`db` is unconditionally reassigned and restarted for **every** subclass's `@BeforeAll` (no `if (db == null)` guard exists today) — this is genuine container-per-class behavior, confirmed by reading the file, not inferred. `database`, however, **is** already guarded (`if (database == null)`) — so today's real bug is the *opposite* of what D-08 assumes for the shared container: the database name is already shared for the whole suite (single `kafka_connect_test_<ts>` for every class in a fork), which is exactly the problem D-08 describes and fixes.

**Target implementation (combines INFRA-03's singleton guard with D-05's `PullPolicy.alwaysPull()` and D-08's per-class database):**
```java
@BeforeAll
public static void setup() throws IOException {
    // D-08: unique database EVERY class, not just once per fork
    database = String.format("kafka_connect_test_%s_%s", getClass().getSimpleName(), System.currentTimeMillis());
    if (isCloud) { initialPing(); return; }
    if (db == null) {                                              // Pattern 1 guard
        db = new ClickHouseContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE)
                .withImagePullPolicy(PullPolicy.alwaysPull())      // D-05
                .withPassword("test_password")
                .withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1");
        db.start();
    }
}
```
Note `database = ...` (not `if (database == null)`) is now **unconditional** — this is the actual code change D-08 requires; the old guarded assignment is what made it a single shared name. `getClass().getSimpleName()` works here because `setup()` runs once per concrete subclass (JUnit calls the inherited static `@BeforeAll` once per test-class execution), giving a name unique per class without needing to thread the subclass name through explicitly.

**`@AfterAll tearDown()`** must stop dropping the per-class database (still needed, D-08 requires DROP in `@AfterAll`) but must **not** stop `db` anymore for the non-cloud path (Pattern 1 — the container now outlives any single class). Rely on Testcontainers' Ryuk (already confirmed working on this machine — see Environment Availability) or a JVM shutdown hook `[Claude's Discretion per CONTEXT.md]`.

### Pattern 2: JUnit tag exclusion + `testUnit`, with the `check`-loop trap (new finding this session)

**The trap:** `build.gradle.kts:171-173`, read this session:
```kotlin
tasks.withType<Test> {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    tasks.getByName("check").dependsOn(this)          // ← fires for EVERY Test-typed task, present or future
    systemProperty("file.encoding", "windows-1252")
    useJUnitPlatform()
    ...
}
```
`tasks.withType<Test> { ... }` is a lazy "configure-all-matching, including future" action. Once `testUnit` is registered as a `Test`-typed task, this loop's `tasks.getByName("check").dependsOn(this)` line fires for it too — silently pulling `testUnit` into `./gradlew check`, which directly violates D-16 ("Exclude `testUnit` from `check`. It is a subset of `test`; the existing `tasks.withType<Test>` loop... would otherwise add it."). This is CONTEXT.md's own stated risk, and the fix belongs in this loop, not in the `testUnit` registration block (registering `testUnit` cannot "opt out" of a `withType<Test>` action that already ran/will run against it):

```kotlin
tasks.withType<Test> {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    if (name != "testUnit") {                          // ← one-line guard, minimal diff
        tasks.getByName("check").dependsOn(this)
    }
    systemProperty("file.encoding", "windows-1252")
    useJUnitPlatform()
    ...
}
```
This leaves `test` and `integrationTest`'s existing `check` dependency completely untouched (D-16: "Leave the pre-existing `check` → `integrationTest` dependency... untouched").

**Registering `testUnit`** (picks up `maxParallelForks`/`file.encoding`/`javaLauncher`/logging from the `withType<Test>` block above automatically, since it's registered *after* that block in file order — Gradle applies configuration actions to a task in the order they were added, so the task-specific `useJUnitPlatform { excludeTags(...) }` call below runs *after* the generic no-arg `useJUnitPlatform()` call and correctly wins):
```kotlin
tasks.register<Test>("testUnit") {
    group = "verification"
    description = "Tests not tagged @Tag(\"clickhouse\") — no Docker required"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        excludeTags("clickhouse")
    }
}
```

**`@Tag("clickhouse")` inheritance** — confirmed this session: `@Tag` gained `@Inherited` in JUnit 5.3.0 `[CITED: github.com/junit-team/junit5 issue #1158, junit.org/junit5/docs/5.3.0/api/.../Tag.html]`; this project's JUnit BOM is `6.0.2` `[VERIFIED: build.gradle.kts:68]`, far past that fix, so a single `@Tag("clickhouse")` on `ClickHouseBase` (`ClickHouseBase.java`, currently un-annotated — verified by reading the file) propagates to all 14 confirmed subclasses:
```
$ grep -rln "extends ClickHouseBase" src/test/java | wc -l
14
```
`[VERIFIED: this session, matches D-12's "all 14 current subclasses" claim exactly]`.

### Pattern 3: Proxy-container isolation (D-07) — the exact conflict, verified

Both proxy classes assign the shared static `db` field directly in their own `@BeforeAll`, confirmed this session:
```
ClickHouseSinkTaskWithSchemaProxyTest.java:41:  db = new org.testcontainers.clickhouse.ClickHouseContainer(...CLICKHOUSE_FOR_PROXY_DOCKER_IMAGE)...
ClickHouseSinkTaskSchemalessProxyTest.java:31:  db = new ClickHouseContainer(...CLICKHOUSE_FOR_PROXY_DOCKER_IMAGE)...
```
Neither class overrides `@AfterAll` — so once Pattern 1's singleton guard (`if (db == null)`) lands in the base class, whichever of these two proxy classes runs *first* in a fork will set `db` to the 23.8 proxy image, and the guard will then skip starting the `latest` container for every subsequent `latest`-only class in that same fork (since `db != null`). This is the exact "must never assign, overwrite, or stop the shared `latest` singleton" conflict D-07 names. Fix: give the proxy classes their own static field (e.g. `proxyDb`) and their own `@BeforeAll`/`@AfterAll` pair that never touches `ClickHouseBase.db`, using `createProps()`/`getTestProperties()` overrides that point at `proxyDb` instead — `[Claude's Discretion per CONTEXT.md: "small base hook... keep the diff minimal"]`.

### Pattern 4: JMX MBean-safe test lifecycle (verified failure mode)

`MBeanServerUtils.registerMBean`, read this session (`MBeanServerUtils.java:17-29`):
```java
public static <T> T registerMBean(final T mBean, final String mBeanName) {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
        server.registerMBean(mBean, new ObjectName(mBeanName));
        return mBean;
    } catch (InstanceAlreadyExistsException e) {
        throw new RuntimeException(e);          // ← hard failure, not a warning
    } catch (Exception e) {
        LOGGER.warn("Unable to register MBean " + mBeanName, e);   // other exceptions only warn
        return mBean;
    }
}
```
`ProxySinkTask.java:31`, confirmed: `private static final AtomicInteger NEXT_ID = new AtomicInteger();` — MBean names embed this monotonically increasing `id`, so within one JVM fork, MBean name collisions only happen when two tasks share the *same* `connectorName` (id itself never repeats) — meaning the leak risk is specifically about a fixed/default connector name being reused across test methods, not about the id counter. `JsonColumnTest.runJsonTest` (`JsonColumnTest.java`, confirmed this session) does:
```java
ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
sinkTask.start(props);
sinkTask.put(transformedRecords);
sinkTask.stop();
```
— no try/finally. `ClickHouseSinkTaskTest.java` has 2 `.start(` calls and 1 `.stop(` call (`[VERIFIED: this session via grep]`), matching D-09's cited imbalance exactly.

**`@AfterEach` sweep for `ClickHouseBase` (D-09):**
```java
@AfterEach
void cleanupLeakedMBeans() {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
        ObjectName pattern = new ObjectName("com.clickhouse:type=ClickHouseKafkaConnector,*");
        for (ObjectName leaked : server.queryNames(pattern, null)) {
            LOGGER.warn("Unregistering leaked MBean from a previous test: {}", leaked);
            MBeanServerUtils.unregisterMBean(leaked.toString());
        }
    } catch (MalformedObjectNameException e) {
        throw new RuntimeException(e);
    }
}
```
`MBeanServerUtils.unregisterMBean` is already safe to call on a non-existent name (`server.isRegistered(objectName)` guard, confirmed `MBeanServerUtils.java:34`), so this sweep cannot itself throw on the common "nothing leaked" case.

**Unique connector-name helper (D-10, `[Claude's Discretion]` on exact naming):**
```java
protected String uniqueConnectorName(TestInfo testInfo) {
    return String.format("%s-%s-%d",
        getClass().getSimpleName(),
        testInfo.getTestMethod().map(java.lang.reflect.Method::getName).orElse("unknown"),
        System.nanoTime());
}
```
Requires the test method to accept a `TestInfo testInfo` parameter (JUnit 5 auto-injects it) and pass the result into `props.put(ClickHouseSinkConnector.NAME_PROP_KEY_OR_EQUIVALENT, uniqueConnectorName(testInfo))` — confirm the exact connector-name config key in `ClickHouseSinkConnector`/`ClickHouseSinkConfig` before wiring this (not verified in this research pass; a one-line `grep` in the plan/execute phase will confirm it).

### Pattern 5: JaCoCo wiring — `test` only, Lombok-excluded, dual reports

**Current state, verified this session:** `build.gradle.kts` has **no** `jacoco` plugin, no `jacoco {}` block, and no `JacocoReport` task at all — this is a from-scratch addition, not a modification of existing coverage config.

```kotlin
plugins {
    // ...existing plugins unchanged...
    jacoco
}

jacoco {
    toolVersion = "0.8.15"    // D-16
}

tasks.named<Test>("test") {
    finalizedBy(tasks.named("jacocoTestReport"), tasks.named("jacocoForkReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// D-16: explicitly disable the agent on testUnit and integrationTest
tasks.named<Test>("testUnit") {
    configure<org.gradle.testing.jacoco.plugins.JacocoTaskExtension> { isEnabled = false }
}
tasks.named<Test>("integrationTest") {
    configure<org.gradle.testing.jacoco.plugins.JacocoTaskExtension> { isEnabled = false }
}

// D-13/D-14: fork-changed-class-only view
val forkChangedClasses = listOf(
    "com/clickhouse/kafka/connect/transforms/TableOperationFilter*",
    "com/clickhouse/kafka/connect/transforms/HeadersToValue*",
    "com/clickhouse/kafka/connect/transforms/HeaderToValue*",
    "com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson*",
    "com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient*",
    "com/clickhouse/kafka/connect/sink/db/mapping/Column*",
    "com/clickhouse/kafka/connect/sink/db/mapping/Type*",
    "com/clickhouse/kafka/connect/sink/db/mapping/Table*",
    "com/clickhouse/kafka/connect/sink/db/ClickHouseWriter*",
    "com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig*",
    "com/clickhouse/kafka/connect/sink/ProxySinkTask*",
    "com/clickhouse/kafka/connect/sink/ClickHouseSinkTask*",
    "com/clickhouse/kafka/connect/ClickHouseSinkConnector*",
    "com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils*"
)
tasks.register<JacocoReport>("jacocoForkReport") {
    dependsOn(tasks.named("test"))
    executionData(tasks.named<Test>("test").get())
    sourceSets(sourceSets.main.get())
    classDirectories.setFrom(
        files(sourceSets.main.get().output.classesDirs.map { dir ->
            fileTree(dir) { include(forkChangedClasses) }
        })
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/fork"))
    }
}
```
All 11 class-file paths above were confirmed to exist this session (`find src/main/java/... -name "*.java"`), including the corrected `com/clickhouse/kafka/connect/transforms/` prefix (prior STACK.md research flagged a bare `transforms/**` glob as wrong for main classes — confirmed: `HeaderToValueConfig.java` and `KeyToValue.java` both live under that same package, so `HeaderToValue*` alone would NOT pull in `HeaderToValueConfig` unless the glob is `HeaderToValue*` — verify the include list actually matches `HeaderToValueConfig.class` at execution time by inspecting the generated report, since Gradle `Pattern`-style `include()` globs are prefix-matched on the class file name and `HeaderToValue*` does match `HeaderToValueConfig.class`).

**`@Generated`-based exclusion is automatic once the annotation is present** — JaCoCo excludes any method/field/class carrying an annotation whose retention includes `RUNTIME`/`CLASS` and whose simple name is `Generated`, since JaCoCo 0.8.2 `[CITED: jacoco/jacoco PR #495, "Exclude from a report methods annotated by lombok/Generated"]` — no extra `classDirectories`/`excludes` config is needed beyond making sure `@lombok.Generated` actually gets stamped, which is `lombok.config`'s job (D-15).

**New finding — `lombok.config`'s effect may already be present:** Lombok 1.18.42 (this project's pinned version, `[VERIFIED: build.gradle.kts:93-94]`) defaults `lombok.addLombokGeneratedAnnotation=true` since `1.18.34` `[CITED: projectlombok.org/api/lombok/ConfigurationKeys, cross-referenced via search]`. That means `Column`'s `@Builder`/`@Getter`-generated methods (`Column.java:23-24`, confirmed `@Builder @Getter public class Column`) are very likely **already** being stamped `@lombok.Generated` today, with or without a `lombok.config` file — so JaCoCo may already be excluding them even before this phase's change lands. D-15's `lombok.config` still matters (pins the behavior explicitly, documents intent for future Lombok downgrades, and `config.stopBubbling=true` prevents Lombok from scanning parent directories outside the repo) — but the plan's verification step (open the generated report, inspect `Column`) is what actually confirms COV-02, not the file's mere existence. Don't treat "we added lombok.config" as sufficient evidence on its own.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Ensuring `latest` is actually re-pulled | A pre-test shell script running `docker pull clickhouse/clickhouse-server:latest` | `db.withImagePullPolicy(PullPolicy.alwaysPull())` | Declarative, colocated with the container definition, works identically in CI and local colima; a shell script is an extra out-of-band step that can be skipped |
| Excluding Lombok boilerplate from coverage | A hand-maintained per-method JaCoCo `excludes` list for `Column`/`Table`/`ClickHouseSinkConfig` | `lombok.config` (`addLombokGeneratedAnnotation=true`) + JaCoCo's built-in `@Generated` detection | JaCoCo has excluded `@Generated`-annotated members automatically since 0.8.2 — a hand-maintained method-name exclude list drifts the moment a Lombok-annotated field is added/removed |
| Finding leaked MBeans across tests | A hand-tracked static `Set<String>` of registered MBean names in test code | `MBeanServer.queryNames(new ObjectName("com.clickhouse:type=ClickHouseKafkaConnector,*"), null)` | The platform MBeanServer is the actual source of truth for what's registered; a parallel hand-tracked set can drift from reality (e.g. if a test registers via a code path the tracker doesn't know about) |
| Running only Docker-free tests | Grepping test class names for a naming convention (`*IT.java` vs `*Test.java`) or a custom Gradle filter script | JUnit Platform's built-in `useJUnitPlatform { excludeTags("clickhouse") }` | Tag-based filtering is JUnit's own supported mechanism, inherited automatically via `@Tag`'s `@Inherited` meta-annotation — no custom classification logic to maintain |
| Comparing the running ClickHouse version against what was requested | A hand-rolled Docker registry API client that resolves `latest`'s digest and cross-references it against numeric-tag digests | `SELECT version()` string-prefix check for numeric tags; a well-formed-version assertion + logging for `latest` (see Common Pitfalls — no version label exists to compare against) | Building a registry-API cross-check is possible but adds a network dependency and real complexity for a check whose real purpose (per D-06's stated goal) is "log the actual version so the baseline summary can record it" — that's satisfiable without external metadata |

## Common Pitfalls

### Pitfall 1: `CLICKHOUSE_VERSION` silently defaults to `25.3`, not `latest`
**What goes wrong:** `ClickHouseTestHelpers.CLICKHOUSE_VERSION_DEFAULT = "25.3"` (`[VERIFIED: ClickHouseTestHelpers.java:41]`, read this session) and `getClickhouseVersion()` falls back to it whenever `CLICKHOUSE_VERSION` is unset. Any local run or CI job that forgets the env var silently tests `25.3`, not `latest`.
**Why it happens:** The constant predates the fork's `latest`-only constraint.
**How to avoid:** D-05 already specifies flipping the constant. Confirm after the change that `getClickhouseVersion()` (`ClickHouseTestHelpers.java:53-59`) still honors an explicit `CLICKHOUSE_VERSION` override for developers who intentionally want to test against a pinned version.
**Warning signs:** `./gradlew test` "passes" without ever setting `CLICKHOUSE_VERSION` in the shell.

### Pitfall 2: No usable version label exists on the ClickHouse Docker image (D-06's research flag — resolved this session)
**What goes wrong:** D-06 assumes the pulled image carries "version label/metadata" to compare `SELECT version()` against for the `latest` tag. Confirmed this session that this does not exist:
- ClickHouse's `docker/server/Dockerfile.ubuntu` on `master`, fetched this session, contains **zero `LABEL` instructions** (`[VERIFIED: raw.githubusercontent.com/ClickHouse/ClickHouse/master/docker/server/Dockerfile.ubuntu, fetched this session]`) — only `ARG VERSION="26.8.5.13"` (a build arg, not image metadata visible via `docker inspect`).
- GitHub issue #54224 ("No version in docker inspect output for clickhouse-server latest") documents that the `com.clickhouse.build.version` label users expect is **absent** from `docker inspect` output; where `org.opencontainers.image.version` is present at all (older/different image variants), it reflects the **Ubuntu base OS version**, not the ClickHouse semantic version `[CITED: github.com/ClickHouse/ClickHouse/issues/54224, fetched this session]`.
**Why it happens:** Docker Official Images tooling sometimes injects `org.opencontainers.image.*` labels automatically from the base image, which is easy to mistake for an application-specific version label.
**How to avoid (closest equivalent, per D-06's own instruction to propose one if no label exists):** For `latest`, assert the returned `SELECT version()` string is well-formed (matches ClickHouse's `MAJOR.MINOR.PATCH.BUILD` pattern, e.g. `^\d+\.\d+\.\d+\.\d+$`) and log it — this satisfies D-06's actual downstream need ("the baseline summary can record it"). Do not attempt an equality comparison against image-level metadata that doesn't exist. If stronger independent corroboration is wanted later, the only real option is a Docker Hub Registry API digest cross-check (resolve `latest`'s manifest digest, compare against the numeric tag matching the reported version) — flagged as optional/deferred, not required by any Phase 1 requirement.
**Warning signs:** A smoke test written against `docker inspect`'s `Labels` map for the `latest` tag will find the map empty or containing only OS-level labels, not a ClickHouse version.

### Pitfall 3: `windows-1252` test encoding is JVM-wide; most stream readers don't specify a charset
**What goes wrong:** `build.gradle.kts:174` sets `systemProperty("file.encoding", "windows-1252")` for all tests. `ClickHouseTestHelpers.getAllRowsAsJson` (line 158) and `validateRows` (line 252) both construct `new InputStreamReader(queryResponse.getInputStream())` with no explicit charset — confirmed by reading the file this session; both are exactly the two lines D-11 names.
**Why it happens:** `InputStreamReader`'s no-charset constructor silently uses the platform default, which is `windows-1252` under this Gradle config.
**How to avoid:** Wrap both sites with `new InputStreamReader(queryResponse.getInputStream(), StandardCharsets.UTF_8)` — this is a pure test-helper change, no main-code touch (Phase 1 scope per D-11).
**Warning signs:** A test asserting non-ASCII content (Cyrillic/CJK) added in a later phase fails only under `./gradlew test`, not from an IDE run with a UTF-8 default.

### Pitfall 4: `MBeanServerUtils.registerMBean` throws hard on name collision
**What goes wrong:** Confirmed this session (`MBeanServerUtils.java:17-29`) — `InstanceAlreadyExistsException` is rethrown as `RuntimeException` (all *other* exceptions are merely logged at WARN). Combined with `JsonColumnTest`'s missing try/finally (confirmed, `sinkTask.start/put/stop` with no wrapper) and `ClickHouseSinkTaskTest`'s 2-start/1-stop imbalance (confirmed via grep), a test that fails between `start()` and `stop()` leaves the MBean registered, and the next test using the same effective connector name crashes with `InstanceAlreadyExistsException` — a suite-order-dependent flake.
**Why it happens:** The lifecycle looks correct in the happy path; only a genuine mid-test failure (rare during initial development, common once new fork tests probe edge cases in later phases) exposes it.
**How to avoid:** D-09's `@AfterEach` sweep (Pattern 4 above) plus the try/finally discipline rule for new/touched tests. Existing upstream files are explicitly not retrofitted (D-09) — the sweep is the safety net for those.
**Warning signs:** A test passes in isolation but fails only when the full suite runs, and the failure is `InstanceAlreadyExistsException`.

### Pitfall 5: The `testUnit` task will be silently pulled into `check` unless the existing loop is guarded (new finding, not in CONTEXT.md)
**What goes wrong:** See Pattern 2 above — `tasks.withType<Test> { tasks.getByName("check").dependsOn(this) }` (`build.gradle.kts:171-173`) applies to every current and future `Test`-typed task, including the new `testUnit`.
**Why it happens:** `tasks.withType<Test>` is a lazy "all matching, including future" Gradle configuration action; registering `testUnit` afterward does not exempt it.
**How to avoid:** Guard the loop with `if (name != "testUnit")` (shown in Pattern 2) — a one-line change scoped to exactly the task this decision (D-16) names, leaving `test`/`integrationTest`'s existing `check` dependencies untouched.
**Warning signs:** `./gradlew check --dry-run` (or inspecting `check`'s task graph) shows `testUnit` as a dependency after adding it.

### Pitfall 6: Both proxy test classes overwrite the shared `db` singleton
**What goes wrong:** Confirmed this session — `ClickHouseSinkTaskWithSchemaProxyTest.java:41` and `ClickHouseSinkTaskSchemalessProxyTest.java:31` both do `db = new ClickHouseContainer(...CLICKHOUSE_FOR_PROXY_DOCKER_IMAGE)...` in their own `@BeforeAll`, with no `@AfterAll` override, so the base class's `tearDown()` stops whatever container `db` currently points to. Once Pattern 1's `if (db == null)` guard lands, whichever proxy class runs first in a fork "poisons" `db` for every subsequent `latest`-only class in that fork.
**Why it happens:** These two classes predate the singleton pattern and were written against the old "always start a fresh container" assumption, where overwriting `db` was harmless.
**How to avoid:** D-07's fix — separate container field entirely, never touching `ClickHouseBase.db` (Pattern 3 above).
**Warning signs:** Tests unrelated to the proxy classes fail with connection errors to a 23.8-versioned server, or with "table doesn't support JSON/Dynamic" errors, when run after a proxy test class in the same fork.

### Pitfall 7: ClickHouse issue #58828 (Toxiproxy absolute-form URI) is confirmed still open — don't attempt to "fix" the proxy version pin
**What goes wrong/confirms:** Fetched this session — issue state is **Open**, title "does not support URI with absolute-form #58828". D-07's rationale for pinning the proxy classes to `23.8` instead of `latest` is current and correct as of this research date.
**How to avoid:** Do not move the proxy classes to `latest` in this phase; that's explicitly deferred (CONTEXT.md Deferred Ideas) until the upstream issue closes.

## Code Examples

### Version smoke test (D-06, adjusted per Pitfall 2's finding)
```java
// Source: original — no official example exists for this exact assertion, since ClickHouse
// provides no image-level version label to compare against (see Common Pitfalls, Pitfall 2)
@Test
@Tag("clickhouse")
void serverVersionMatchesRequestedImage() {
    ClickHouseHelperClient chc = createClient(createProps());
    String serverVersion = /* query SELECT version() via chc, existing helper pattern */;
    String requested = ClickHouseTestHelpers.getClickhouseVersion();

    LOGGER.info("Running against ClickHouse server version: {} (requested image tag: {})", serverVersion, requested);

    if (!requested.equalsIgnoreCase("latest") && !requested.equalsIgnoreCase("cloud")) {
        assertTrue(serverVersion.startsWith(requested),
            () -> String.format("Expected server version to start with requested tag '%s', got '%s'", requested, serverVersion));
    } else {
        // No usable image-level version label exists for `latest` (confirmed: ClickHouse's
        // Dockerfile.ubuntu has no LABEL instructions; see GitHub issue #54224) — assert
        // well-formed instead of comparing against nonexistent metadata.
        assertTrue(serverVersion.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$"),
            () -> String.format("Expected a well-formed ClickHouse version string, got '%s'", serverVersion));
    }
}
```

### Container singleton + per-class database (Pattern 1, full diff shape)
```java
// Source: this session, derived from reading ClickHouseBase.java:24-38 directly
@BeforeAll
public static void setup() throws IOException {
    database = String.format("kafka_connect_test_%s_%s", ClickHouseBase.class, System.currentTimeMillis());
    if (isCloud) { initialPing(); return; }
    if (db == null) {
        db = new ClickHouseContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE)
                .withImagePullPolicy(org.testcontainers.images.PullPolicy.alwaysPull())
                .withPassword("test_password")
                .withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1");
        db.start();
    }
}
```
(Note: use the actual concrete subclass, not `ClickHouseBase.class`, for a per-class name — resolve via the JUnit `ExtensionContext`/`TestInfo` in `@BeforeAll`, or simplest: each subclass could override a `protected static String testClassTag()`-style hook if a purely static approach in the base class proves awkward — `[Claude's Discretion]`.)

### `check`-loop guard for `testUnit` (Pitfall 5 fix)
```kotlin
// Source: build.gradle.kts:171-173, modified this session's finding
tasks.withType<Test> {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    if (name != "testUnit") {
        tasks.getByName("check").dependsOn(this)
    }
    systemProperty("file.encoding", "windows-1252")
    useJUnitPlatform()
    // ...rest unchanged...
}
```

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `[Claude's Discretion]` code sketches (unique connector-name helper exact signature, per-class database naming via `getClass()`, proxy container field name, `testUnit` classpath wiring beyond the shown pattern) are illustrative, not verified-correct Java — they compile-check by inspection only, not by actually running `javac`/`gradle` this session (constraint: no `./gradlew` runs allowed for this research pass) | Architecture Patterns, Code Examples | Low-medium — the planner/executor must actually compile these; a signature mismatch (e.g., the real connector-name config key) will surface immediately at compile/test time, not silently |
| A2 | JUnit's `TestInfo`-based `@AfterEach`/method-name-based unique connector naming is compatible with the existing `createProps()` call pattern used across 14 subclasses without further changes | Pattern 4 | Low — this is additive (a new opt-in helper method per D-10), existing tests are explicitly not required to adopt it |
| A3 | The Docker Hub Registry API digest cross-check mentioned as a "stronger" fallback for the version smoke test was not actually implemented or tested this session — it's a design option only | Common Pitfalls (Pitfall 2), Don't Hand-Roll | Low — not required by any locked decision; flagged only as a future option if D-06's simpler well-formed-check proves insufficient |
| A4 | `withImagePullPolicy` is available on `ClickHouseContainer` via its `JdbcDatabaseContainer`/`GenericContainer` superclass chain — confirmed the class declaration (`extends JdbcDatabaseContainer<ClickHouseContainer>`) but not by compiling against the actual 1.21.4 jar (not cached locally; `find ~/.gradle ~/.m2` returned nothing this session) | Standard Stack, Pattern 1 | Low — `JdbcDatabaseContainer` extending `GenericContainer` and exposing this fluent method is standard, well-documented Testcontainers architecture across all its database-container modules (confirmed via the `PostgreSQLContainer` example in official docs), but should be confirmed by a successful compile in the execute phase |

**If this table is empty:** N/A — see above.

## Open Questions

1. **Is `./gradlew test` actually green against `latest`×V2 today?**
   - What we know: colima `vz` profile is running (`[VERIFIED: this session, colima list]`), Docker daemon reachable, `25.3`/`23.8` images cached but `latest` is not (`[VERIFIED: this session, docker images]`), Ryuk `0.11.0`/`0.12.0` cached. GitHub Actions is enabled and green as of `22f055c` (2026-01-29) per `PROJECT.md`, but that was against the matrix default, and `latest` has moved since.
   - What's unclear: whether any test currently fails against `latest`/V2 specifically — this requires actually running `./gradlew test -Dsystem...CLICKHOUSE_VERSION=latest`, which this research pass was explicitly instructed not to do.
   - Recommendation: D-01 already mandates this as the very first step of plan 01-01 — the executor should run it before making any other Phase 1 change, exactly as CONTEXT.md specifies, and record the raw result in the phase summary.

2. **What is the exact ClickHouse connector config key for "connector name" used in MBean naming?**
   - What we know: `ProxySinkTask.getMBeanName()` interpolates `connectorName` into the MBean's `connector=%s` segment (`ProxySinkTask.java:78`, confirmed this session), and `connectorName` is passed into `ProxySinkTask`'s constructor from wherever `ClickHouseSinkTask`/`ClickHouseSinkConnector` wires it up.
   - What's unclear: the exact `props` key a test must set to control this (not traced in this research pass — out of the files read this session).
   - Recommendation: a one-line `grep -rn "connectorName\|NAME_PROP\|\"name\"" src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java src/main/java/com/clickhouse/kafka/connect/ClickHouseSinkConnector.java` during planning/execution will resolve this quickly; not blocking for the research pass since D-10's helper is additive and its exact prop-key wiring is an implementation detail, not a design decision.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker daemon (via colima) | All `ClickHouseBase`-derived tests (INFRA-01/02/03) | ✓ `[VERIFIED: this session, docker info]` | Docker Engine 29.1.4 (client) / Server 27.4.0, colima profile `vz` running (aarch64, 8 CPU / 16 GiB) | — |
| `clickhouse/clickhouse-server:25.3` image | Current default test baseline | ✓ cached `[VERIFIED: this session, docker images]` | 858MB image, `c9e2963e7293` | — |
| `clickhouse/clickhouse-server:23.8` image | Toxiproxy proxy tests (D-07) | ✓ cached `[VERIFIED: this session, docker images]` | 1.31GB image, `512bb8a21483` | — |
| `clickhouse/clickhouse-server:latest` image | New default after D-05 | ✗ not cached `[VERIFIED: this session, docker images — absent]` | — | Will be pulled on first `PullPolicy.alwaysPull()` run — first `./gradlew test` after this phase's changes will incur a pull; note this in the baseline summary as a one-time cost, not a flake |
| `testcontainers/ryuk` | Container cleanup (auto-reap on JVM exit, supports Pattern 1's singleton lifecycle) | ✓ cached, two versions `[VERIFIED: this session, docker images]` | `0.11.0`, `0.12.0` | — |
| Gradle wrapper | All build/test commands | ✓ `[VERIFIED: this session, gradle-wrapper.properties:3]` | `8.14` (not `7.4.2` as `.claude/CLAUDE.md` states — documented drift, already flagged in CONTEXT.md "Specifics") | — |
| JDK 17 toolchain | Test JVM launcher | Not independently re-verified this session (relies on Gradle toolchain provisioning; `build.gradle.kts:14-19` pins `JavaLanguageVersion.of(17)`) | — | Gradle toolchain auto-provisioning if 17 isn't already installed |

**Missing dependencies with no fallback:** none — `latest`'s absence from the local cache is expected and self-resolving (Testcontainers pulls it), not a blocker.

**Missing dependencies with fallback:** `clickhouse/clickhouse-server:latest` — resolved automatically by `PullPolicy.alwaysPull()` on first run; the executor should expect the *first* `./gradlew test` invocation after D-05 lands to take noticeably longer (image pull) and should not mistake that for a hang/flake.

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | Test-only hardcoded credential (`test_password`) is pre-existing, unchanged this phase, and scoped to ephemeral Testcontainers instances — not a production credential path |
| V3 Session Management | No | No session-handling code touched this phase |
| V4 Access Control | No | No access-control code touched this phase |
| V5 Input Validation | No | No new user/external input paths — the only "input" this phase adds is the `CLICKHOUSE_VERSION` env var, already an existing, trusted (CI/developer-controlled) input |
| V6 Cryptography | No | No cryptographic code touched |
| V14 Configuration/Supply Chain (informal — ASVS V14 covers build/deploy config) | Yes | `PullPolicy.alwaysPull()` on a mutable `latest` tag is a deliberate, already-agreed project decision (D-05) — see Known Threat Patterns below for the tradeoff this accepts |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Pulling a mutable `latest` Docker tag on every test run (supply-chain non-reproducibility — a malicious or broken image could be published upstream to `clickhouse/clickhouse-server:latest` between runs) | Tampering | This is an accepted, explicit project constraint (D-05/PROJECT.md: "Test target: ClickHouse `latest`... matches production connectors"), not something Phase 1 should "fix" — the official `clickhouse/clickhouse-server` image is a Docker Official Image with its own publishing controls; no additional mitigation is in scope here. Worth noting for awareness only. |
| MBean leak enabling a later test to silently reuse a stale MBean's identity across test boundaries (test-isolation integrity, not a production security issue) | Tampering (test-integrity, not app-security) | D-09's `@AfterEach` sweep is itself the mitigation — already the subject of this phase's INFRA-05 requirement |
| Test-only hardcoded password (`test_password`) leaking into logs on test failure | Information Disclosure | Pre-existing pattern (`ClickHouseBase.java:36`), not modified this phase; the value is a throwaway Testcontainers-only credential with no production validity, so this is low-severity and out of scope for Phase 1 to change |

## Sources

### Primary (HIGH confidence — read/run directly this session)
- `build.gradle.kts` (full file read this session)
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` (full file read)
- `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` (full file read)
- `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` (lines 1-100 read)
- `src/main/java/com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.java` (full file read)
- `src/test/java/com/clickhouse/kafka/connect/sink/junit/extension/FromVersionConditionExtension.java` (full file read)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java`, `Table.java`, `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java`, `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` (targeted grep + context reads for Lombok annotations)
- `gradle/wrapper/gradle-wrapper.properties` (full file read — confirms Gradle 8.14)
- `.planning/PROJECT.md` (full file read)
- `docker images`, `docker info`, `colima status`, `colima list` — run this session (read-only, no containers started)
- `curl https://repo1.maven.org/maven2/org/jacoco/org.jacoco.core/0.8.15/` — run this session
- `raw.githubusercontent.com/ClickHouse/ClickHouse/master/docker/server/Dockerfile.ubuntu` — fetched this session
- `github.com/ClickHouse/ClickHouse/issues/54224` — fetched this session (version-label absence)
- `github.com/ClickHouse/ClickHouse/issues/58828` — fetched this session (Toxiproxy issue still open)
- `github.com/testcontainers/testcontainers-java` — `ClickHouseContainer.java` source fetched this session

### Secondary (MEDIUM confidence — official docs, web-search cross-checked)
- `java.testcontainers.org/features/advanced_options/` — `PullPolicy.alwaysPull()` usage
- `junit.org`/`github.com/junit-team/junit5` issue #1158 — `@Tag` `@Inherited` history
- `docs.gradle.org` (JaCoCo plugin, `JacocoTaskExtension.isEnabled`) — via prior STACK.md research + this session's search cross-check
- `projectlombok.org/api/lombok/ConfigurationKeys`, `projectlombok.org/features/configuration` — `lombok.config` semantics, `addLombokGeneratedAnnotation` default since 1.18.34
- `github.com/jacoco/jacoco` PR #495 / issue #643 — `@Generated`-annotation auto-exclusion behavior

### Tertiary (LOW confidence — carried from prior research docs, not independently re-verified this session)
- `.planning/research/ARCHITECTURE.md`, `.planning/research/PITFALLS.md`, `.planning/research/STACK.md`, `.planning/codebase/TESTING.md` — used as a starting map of the domain; every claim from them that mattered for Phase 1 planning was independently re-verified against source/external ground truth in this pass and is cited above with its own `[VERIFIED]`/`[CITED]` tag. Claims from those docs not re-verified here (e.g., CI workflow details, since CI is Phase 5 scope) were not carried into this document's load-bearing claims.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — every version number verified against Maven Central, the repo's own build file, or official docs this session
- Architecture: HIGH — every pattern grounded in source read this session, with the `check`-loop and version-label gaps found independently
- Pitfalls: HIGH — all pitfalls either reproduced from direct source reading or confirmed via a live external source (GitHub issues) this session

**Research date:** 2026-09-15
**Valid until:** ~30 days for the Gradle/JaCoCo/Lombok/JUnit version facts (stable ecosystem); the `latest`-tag-not-cached-locally and colima-running facts are point-in-time environment state and should be treated as stale the moment the executor's session differs from this one — re-check with `docker images`/`colima list` before relying on them.
