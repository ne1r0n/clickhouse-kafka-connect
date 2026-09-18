# Architecture Research

**Domain:** Test architecture for a forked Kafka Connect sink connector (Java, Gradle) — adding unit tests, Testcontainers-backed tests, JaCoCo coverage, and CI to an existing suite.
**Researched:** 2026-09-15
**Confidence:** HIGH — grounded directly in this repo's `build.gradle.kts`, `ClickHouseBase.java`, `ClickHouseTestHelpers.java`, existing test classes, and `.github/workflows/tests.yaml`; JaCoCo/Gradle wiring and Testcontainers singleton-container guidance cross-checked against current Gradle and Testcontainers documentation (see Sources).

This is not a "pick a framework" research doc — the stack is fixed (JUnit Jupiter, Mockito, Testcontainers, Gradle Kotlin DSL). The question is how new test code, coverage, and CI should slot into the **existing** structure without destabilizing it or entangling the fork with upstream.

## Standard Architecture

### System Overview

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         `./gradlew check` (CI entry point today: `test`)   │
├───────────────────────────────┬─────────────────────────┬─────────────────┤
│   `test` source set            │  `integrationTest`       │  `jacocoTestReport` │
│   (src/test/java)              │  source set (OUT OF      │  (new)          │
│                                 │  SCOPE this milestone)   │                 │
├─────────────────┬───────────────┴───────────────────────┴─────────────────┤
│ Pure unit tests  │  ClickHouseBase-backed tests (Testcontainers)           │
│ (no Docker)      │  (Docker/colima required)                               │
│                  │                                                          │
│ transforms/*     │ com/clickhouse/kafka/connect/sink/*Test.java            │
│ util/*Test       │   extends ClickHouseBase → @BeforeAll starts            │
│ processing/*Test │   ClickHouseContainer, @AfterAll stops it               │
│ kafa/*Test       │                                                          │
│ provider/*Test   │  helper/ClickHouseTestHelpers, SchemaTestData,          │
│                  │  SchemalessTestData (shared fixtures, same source set)  │
└─────────────────┴──────────────────────────────────────────────────────────┘
                              │
                              ▼
                 Gradle `Test` task, maxParallelForks = cores/2
                 (each fork = separate JVM process; forks run
                  concurrently, classes within a fork run sequentially)
                              │
                              ▼
                 JaCoCo agent attaches per fork → writes exec data
                              │
                              ▼
                 `jacocoTestReport` merges exec data → HTML/XML report
                              │
                              ▼
                 CI job (GitHub Actions) uploads report as artifact
```

### Component Responsibilities

| Component | Responsibility | Location in this repo |
|-----------|----------------|------------------------|
| Unit test layer | Fast, Docker-free tests of pure logic (SMTs, utils, state machine, range logic) | `src/test/java/transforms/`, `src/test/java/com/clickhouse/kafka/connect/sink/{processing,kafa,provider,util}/` |
| `ClickHouseBase` | Shared container lifecycle + connection/config factory for CH-backed tests | `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` |
| ClickHouse-backed test layer | End-to-behavior tests through `ClickHouseSinkTask`/`ClickHouseWriter` against a real server | `src/test/java/com/clickhouse/kafka/connect/sink/*Test.java` (extends `ClickHouseBase`) |
| Test helper/fixture layer | Reusable assertions, fixture builders, SQL helpers | `src/test/java/com/clickhouse/kafka/connect/sink/helper/{ClickHouseTestHelpers,SchemaTestData,SchemalessTestData}.java` |
| Version-gating extension | Skips tests that need a CH feature not in the running `CLICKHOUSE_VERSION` | `src/test/java/.../junit/extension/{SinceClickHouseVersion,FromVersionConditionExtension}.java` |
| `integrationTest` source set | Confluent-platform/Cloud E2E via Docker Compose — **out of scope this milestone** | `src/integrationTest/java/` |
| JaCoCo plugin (new) | Instruments the `test` task, produces coverage exec data + report | New: `build.gradle.kts` |
| CI workflow | Runs `test` (+ new `jacocoTestReport`), publishes coverage artifact | `.github/workflows/tests.yaml` |

## Recommended Project Structure

```
src/test/java/
├── transforms/                         # Pure unit tests for SMTs — no Docker. New SMT-gap
│   ├── TableOperationFilterTest.java   #   tests (truncate op, whitespace, invalid config) go HERE.
│   ├── HeadersToValueTest.java         #   New edge-case tests (null header, collision) go HERE.
│   ├── HeaderToValueTest.java
│   └── ConvertToMapOrJsonTest.java
├── com/clickhouse/kafka/connect/sink/
│   ├── ClickHouseBase.java             # DO NOT duplicate — extend it; see Container Lifecycle pattern below
│   ├── helper/
│   │   ├── ClickHouseTestHelpers.java  # Reuse directly — countRows, createTable, dropTable, col(),
│   │   │                               #   newDescriptor(). Add new static helpers here, not per-test-class.
│   │   ├── SchemaTestData.java         # Reuse for Struct-schema fixtures (add new payload builders here)
│   │   └── SchemalessTestData.java     # Reuse for Map/schemaless fixtures
│   ├── junit/extension/                # Reuse @SinceClickHouseVersion for JSON/Dynamic tests that
│   │   │                               #   only work on `latest` (guards against future matrix changes)
│   │   └── FromVersionConditionExtension.java
│   ├── db/mapping/                     # Pure unit tests for Column/Type parsing — NO Docker needed.
│   │   │                               #   describeTable sub-column skip logic (if extracted, see seams)
│   │   │                               #   and JSON(...)/Dynamic type-string parsing belong HERE as unit tests.
│   │   └── [new: TypeTest.java / ColumnParsingTest.java if not present]
│   ├── db/
│   │   └── ClickHouseWriterTest.java   # Existing — protected doWrite*/cleanupExtraFields/getTable already
│   │                                   #   accessible from same-package test; extend for new gaps here.
│   ├── ClickHouseSinkTaskMappingTest.java        # targetTableFilter integration cases go HERE (extends existing pattern)
│   ├── ClickHouseSinkTaskJsonInsertFormatTest.java  # jsonInsertColumns + explicit-json-path gaps go HERE
│   ├── JsonColumnTest.java             # JSON(...)/Dynamic real-insert cases go HERE (un-@Disable, extend)
│   └── ProxySinkTaskTest.java          # NEW — MBean name format tests (unit, no Docker; see seam below)
```

### Structure Rationale

- **Unit vs ClickHouse-backed split is not a directory split, it's an inheritance split.** Today's `com/clickhouse/kafka/connect/sink/` package already mixes both: classes like `ProcessingTest`, `RangeContainerTest`, `UtilsTest`, `MaskTest`, `LocalProviderTest` are plain unit tests in subpackages (`processing/`, `kafa/`, `provider/`, `util/`), while sibling classes at the top level extend `ClickHouseBase` and pull a container. **Follow this existing convention**: put a new test in the subpackage that mirrors its `src/main` counterpart, and only extend `ClickHouseBase` when the test needs a real server. Don't invent a new top-level `unit/` vs `integration/` folder split — it doesn't match the codebase's actual organizing principle (package mirrors `src/main`, container use is opt-in via inheritance).
- **`transforms/` stays outside `com.clickhouse...`.** This is an established, intentional deviation (SMTs are tested as standalone Kafka Connect `Transformation` implementations, not sink-internal classes) — keep new SMT tests here, not under `sink/`.
- **`helper/` is the fixture layer for the `test` source set only.** `ClickHouseTestHelpers`, `SchemaTestData`, `SchemalessTestData` are compiled once into `test` and reusable by every class in that source set. **Do not attempt to import them from `src/integrationTest/java`** — that source set's `compileClasspath` in `build.gradle.kts` includes `sourceSets["main"].output` and `testRuntimeClasspath` (dependency jars) but **not** `sourceSets["test"].output`, so `integrationTest` cannot compile against `src/test/java` classes. That's why `src/integrationTest/java/.../helper/` has its own duplicate copies of these three files today. This asymmetry is a pre-existing constraint, out of scope to fix this milestone (`integrationTest` is explicitly out of scope) — just don't fight it.
- **Naming**: keep the existing `[Class][Scenario]Test.java` convention (e.g., `ClickHouseSinkTaskJsonInsertFormatTest`) for CH-backed tests grouped by feature, and `[Class]Test.java` for one-to-one unit tests. Method names keep the `test`-prefixed or `camelCaseTest`-suffixed style already present (both appear in the codebase — match whichever convention the file you're extending already uses, don't introduce a third style).

## Architectural Patterns

### Pattern 1: Container-per-fork singleton (not container-per-class)

**What:** `ClickHouseBase.setup()` today unconditionally creates and starts a new `ClickHouseContainer` in every class's `@BeforeAll`, and `tearDown()` stops it in every class's `@AfterAll`. Because `db` is a `static` field declared on `ClickHouseBase` itself, it is genuinely one field shared by all subclasses *within a single JVM* — but the current code doesn't guard against re-creating it, so each new `ClickHouseBase`-derived test class pays a full container-startup cost (typically 10–20s) even though Gradle test forks run classes **sequentially within a fork**. As more CH-backed test classes are added (targetTableFilter, describeTable, JSON/Dynamic, jsonInsertColumns), this cost multiplies linearly and dominates suite wall-clock time.

**When to use:** Any time multiple test classes in the same forked JVM share the same container requirements (same image, same default DB). This is exactly this project's shape — all `ClickHouseBase` subclasses want the same `clickhouse/clickhouse-server:<CLICKHOUSE_VERSION>` container.

**Trade-offs:** Sharing one container per fork across classes means classes are no longer container-isolated — a test class that corrupts server-level state (not just table-level) could leak into the next class in the same fork. In practice this codebase already isolates by database name (`kafka_connect_test_<timestamp>`, computed once and shared) and by table (`createTopicName()` appends a millisecond timestamp), so cross-class table collisions were already avoided even before this change — the singleton pattern just stops discarding a perfectly reusable container.

**Recommended change** (minimal, additive, in `ClickHouseBase` only — no `src/main` touch):
```java
@BeforeAll
public static void setup() throws IOException {
    if (database == null) {
        database = String.format("kafka_connect_test_%s", System.currentTimeMillis());
    }
    if (isCloud) {
        initialPing();
        return;
    }
    if (db == null) {                          // <-- guard: only start once per fork
        db = new ClickHouseContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE)
                .withPassword("test_password")
                .withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1");
        db.start();
        Runtime.getRuntime().addShutdownHook(new Thread(db::stop));  // stop once, at fork JVM exit
    }
}
// remove the per-class db.stop() from tearDown() for the non-cloud path
```
This is the documented "singleton containers" pattern (Testcontainers/Docker guides — see Sources): start once in a common base class, rely on JVM-exit cleanup instead of per-class teardown. It does **not** conflict with `maxParallelForks`: each Gradle fork is a separate JVM/classloader, so each fork still gets exactly one container, started once, and forks remain fully isolated from each other (no cross-fork contention on the `db` field — it's fork-local by construction).

**Do this before** writing the bulk of the new ClickHouse-backed tests (see Build Order) — retrofitting it after 10+ new CH-backed classes exist is the same one-line change, but you pay the full N-container-startups cost throughout development in the meantime, and you should confirm the resulting suite is still green before layering more tests on top of a lifecycle change.

### Pattern 2: JUnit 5 tags for a fast, Docker-free local subset

**What:** Nothing in the current build lets a developer run "just the unit tests" without Docker — `./gradlew test` runs everything in `src/test/java`, unit and CH-backed alike, because they're one Gradle `Test` task over one source set. As CH-backed test count grows, local edit-test loops on SMT/mapping-logic changes get slower and require colima to be running even when irrelevant.

**When to use:** Once ClickHouse-backed test count grows past a handful (which this milestone explicitly does — at least 8–10 new CH-backed test cases across targetTableFilter, describeTable, JSON/Dynamic, jsonInsertColumns).

**Trade-offs:** Adds one more Gradle task to maintain; requires tagging discipline (every new `ClickHouseBase`-derived class must carry the tag) — cheap but easy to forget without a lint/convention check.

**Recommended, additive-only change:**
```java
@Tag("clickhouse")
public class ClickHouseSinkTaskJsonInsertFormatTest extends ClickHouseBase { ... }
```
```kotlin
// build.gradle.kts — new task, does NOT replace `test`, does NOT change CI's `test` invocation
tasks.register<Test>("testUnit") {
    group = "verification"
    description = "Fast unit tests only — excludes ClickHouse-backed (Docker-required) tests"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { excludeTags("clickhouse") }
}
```
Keep `test` (and CI) running the full suite unchanged — `testUnit` is purely a local convenience task, so it carries zero risk to the "fully green `./gradlew test`" done-criterion in `PROJECT.md`.

### Pattern 3: Minimal testability seams — visibility bumps and pure-function extraction, not refactors

**What:** Three concrete seams this fork needs, each a single-purpose, single-line-to-few-line diff that stays rebasable against upstream (upstream doesn't have these fork-only members, so touching them creates no merge conflict surface):

1. **`ProxySinkTask.getMBeanName()` is `private`.** To unit-test the MBean name format (connector name present/absent, null implementation version → `unknown`, `:` → `_` sanitization) without going through full task startup, widen visibility to package-private:
   ```java
   String getMBeanName() {  // was: private String getMBeanName()
   ```
   A new `ProxySinkTaskTest` in the same package (`com.clickhouse.kafka.connect.sink`) can then call it directly. This is a pure function of `connectorName`, `id`, and the loaded package's `Implementation-Version` — genuinely unit-testable once visible, no Docker needed.

2. **`ClickHouseWriter.shouldSkipTableMetadata()` is `private`; `getTable()` and `cleanupExtraFields()` are already `protected`.** Only `shouldSkipTableMetadata()` needs a bump (`protected` or package-private) — `getTable`/`cleanupExtraFields` are already accessible to `ClickHouseWriterTest` (same package `com.clickhouse.kafka.connect.sink.db`) with no change required. Confirm this against the current source before adding new seams here — don't widen what's already open.

3. **`ClickHouseHelperClient.showTables()` builds the `targetTableFilter` SQL clause inline** (`String filterClause = targetTableFilter.isEmpty() ? "" : String.format(" AND match(table, '%s')", targetTableFilter.replace("'", "''"))`). The quote-escaping logic is exactly the kind of pure string-transform that should be extracted into a small, independently unit-testable static method rather than only exercised end-to-end against a real container:
   ```java
   static String buildTableFilterClause(String targetTableFilter) {
       return targetTableFilter.isEmpty() ? "" : String.format(" AND match(table, '%s')", targetTableFilter.replace("'", "''"));
   }
   ```
   Then `showTables()` calls it, and a new fast unit test (no container) covers empty filter, no-quotes filter, and quote-escaping directly — reserving the ClickHouse-backed test for confirming the query actually filters real tables (the two levels of test PROJECT.md's Active scope calls for: "regex match/no-match... regex containing quotes is escaped, **integration against real tables**").

**When to use these:** Apply each seam in the same plan/phase as the test that needs it — don't batch them into a separate "refactor" phase. They're too small and too targeted to justify a dedicated phase, and batching them increases the diff surface reviewers (and future `git diff main...custom_build`) have to reason about in one place instead of alongside the behavior they pin.

**Trade-offs / what NOT to do:** Do not extract `Processing.doLogic()`'s state machine or `ClickHouseWriter.doInsert()`'s format dispatch into new classes to make them "more testable" — `PROJECT.md`'s Out of Scope explicitly rules out refactoring `ClickHouseWriter`/`Processing`/`ClickHouseSinkConfig` (high churn, rebase risk, not required for coverage of fork additions). The seams above are deliberately narrow: visibility bumps and one pure-function extraction, all confined to fork-authored or fork-touched code paths.

### Pattern 4: JaCoCo wired to `test`, kept off `integrationTest` and `check`

**What:** `build.gradle.kts` has a subtle trap for coverage wiring: `tasks.withType<Test> { tasks.getByName("check").dependsOn(this) }` runs for **every** `Test`-typed task, including the `integrationTest` task registered earlier in the file. That means `./gradlew check` today already (perhaps unintentionally) depends on `integrationTest`, which requires Confluent Platform/Docker Compose and is explicitly out of scope this milestone. If JaCoCo is wired via the conventional `check.dependsOn(jacocoTestReport)` idiom, `check` becomes an even larger, slower, environment-dependent entry point.

**When to use:** Apply JaCoCo only to the `test` task, and keep CI invoking `test` (+ report) directly — never `check` — until/unless `integrationTest`'s scope is separately revisited.

```kotlin
plugins {
    jacoco
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.named<Test>("test") {
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)   // for CI artifact / coverage tooling
        html.required.set(true)  // for human review
    }
    // Optional: scope to fork-touched classes if upstream noise needs excluding —
    // start unscoped; only add classDirectories filtering if report size/signal becomes a problem.
}
```
Do **not** add `finalizedBy` on `integrationTest`, and do not make `jacocoTestReport` a dependency of `check` — CI should call `./gradlew test jacocoTestReport` (or rely on `finalizedBy` alone) explicitly, not `./gradlew check`.

## Data Flow

### Test → Container → Assertions

```
Gradle Test JVM fork (1 per maxParallelForks slot)
   │
   ├─ ClickHouseBase.setup() [@BeforeAll, singleton-per-fork after Pattern 1]
   │     → docker/colima → ClickHouseContainer.start() → exposed port
   │
   ├─ Test class N: createProps()/createClient() → ClickHouseHelperClient
   │     → ClickHouseTestHelpers.createTable/dropTable (real DDL over the container)
   │     → SchemaTestData/SchemalessTestData → SinkRecord fixtures
   │     → ClickHouseSinkTask.start()/put()/stop() (real connector code path)
   │     → ClickHouseTestHelpers.countRows/getAllRowsAsJson/validateRows (real SELECT)
   │     → JUnit assertions against query results
   │
   ├─ Test class N+1: reuses same container (Pattern 1) instead of restarting
   │
   └─ JVM exit → shutdown hook stops the container
```

### Test → JaCoCo exec → Report → CI artifact

```
Gradle `test` task (JaCoCo agent attached per fork via javaagent)
   │
   ├─ each fork writes its own .exec fragment → build/jacoco/test.exec (merged by Gradle)
   │
   ▼
`jacocoTestReport` (finalizedBy `test`, depends on `test`)
   │
   ├─ merges exec data across all forks
   ├─ maps back to compiled classes under `src/main` (fork-changed classes are the ones
   │   this milestone cares about — see PROJECT.md "coverage of fork-changed classes")
   │
   ▼
build/reports/jacoco/test/{html/, jacocoTestReport.xml}
   │
   ▼
CI job: actions/upload-artifact@v4 uploads the report directory
   (and/or a coverage-summary step reads the XML for a job-summary comment)
```

**Key ordering constraint:** `jacocoTestReport` must run **after** `test` completes (not in parallel) because it reads the merged `.exec` file `test` produces — the `dependsOn`/`finalizedBy` pair above is what Gradle needs to serialize this correctly; omitting either half either skips the report or runs it against stale/empty exec data.

## CI Job Structure

**Current state** (`.github/workflows/tests.yaml`): one `build` job, matrix of `client: [V1, V2]` × `clickhouse: [23.7, 24.3, latest, cloud]` = 8 cells, running `gradle test` per cell (cloud cells skip via a credential-check step). No JaCoCo, no artifact upload.

**Recommended target state**, matching `PROJECT.md`'s explicit constraint ("Test target: ClickHouse `latest` + client V2 only"):

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    env:
      CLICKHOUSE_VERSION: latest
      CLIENT_VERSION: V2
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - name: Run tests with coverage
        run: ./gradlew test jacocoTestReport --no-daemon
      - name: Upload coverage report
        if: always()             # publish even on failure — coverage of a red run still diagnoses gaps
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: build/reports/jacoco/test/
```

- **Single job, no matrix** — the 23.7/24.3/cloud/V1 cells are explicitly out of scope; removing them cuts CI runtime roughly 8x and removes the cloud-credential-skip complexity entirely.
- **`test jacocoTestReport`, never `check`** — see Pattern 4; `check` currently pulls in `integrationTest`.
- **Docker on GitHub-hosted `ubuntu-latest` runners is preinstalled** — no colima equivalent needed in CI; only local dev needs colima running before `./gradlew test`.
- **`maxParallelForks = cores/2` on GitHub-hosted runners** (typically 4 cores → 2 forks) is modest relative to a developer laptop (often 8–16 cores → 4–8 forks). This asymmetry means the container-singleton pattern (Pattern 1) matters *more* locally than in CI — a laptop running colima with 4–8 concurrent ClickHouse containers competing for colima's VM CPU/memory budget is the realistic flakiness risk this milestone should guard against, not CI.
- **Artifact, not a coverage gate.** Given `PROJECT.md`'s scope ("JaCoCo coverage report generated... showing coverage of fork-changed classes" — no numeric threshold mentioned), publish the report as an artifact for human review; do not add `jacocoTestCoverageVerification` with an enforced minimum unless a later milestone asks for it. Adding an enforced gate now risks blocking merges over upstream-inherited, untouched code that happens to have low coverage — out of scope per `PROJECT.md`.

## Anti-Patterns

### Anti-Pattern 1: Refactoring for testability

**What people do:** See a hard-to-test private method or a monolithic `doInsert()`/`doLogic()` and extract interfaces, introduce DI, or split classes "to make it testable properly."
**Why it's wrong:** This fork must stay rebasable against upstream (`PROJECT.md` constraint, explicit Out-of-Scope entries for `ClickHouseWriter`/`Processing`/`ClickHouseSinkConfig`). Structural refactors create merge-conflict surface against every future upstream sync and aren't needed — this milestone's job is to test existing fork behavior, not improve its design.
**Do this instead:** Visibility bumps (`private` → package-private/`protected`) and single pure-function extractions only, scoped to fork-authored code, applied in the same change as the test that needs them (Pattern 3).

### Anti-Pattern 2: One container per test class, unguarded

**What people do:** Copy the existing `ClickHouseBase.setup()` pattern verbatim into every new CH-backed test file, or worse, hand-roll a fresh `@Testcontainers`/`@Container` setup per class instead of extending `ClickHouseBase`.
**Why it's wrong:** Multiplies container-startup latency linearly with test-class count (this milestone adds ~8–10 new CH-backed classes), and duplicated setup code drifts from `ClickHouseBase`'s conventions (env var handling, cloud-vs-local branching, V1/V2 client selection) over time.
**Do this instead:** Always extend `ClickHouseBase`; apply the singleton guard (Pattern 1) once, centrally, rather than working around slow setup per new test file.

### Anti-Pattern 3: Wiring JaCoCo/coverage to `check`

**What people do:** Follow generic JaCoCo tutorials that wire `check.dependsOn(jacocoTestReport)` and `jacocoTestReport.dependsOn(test)`.
**Why it's wrong:** In this specific `build.gradle.kts`, `check` already transitively depends on **every** `Test`-typed task including `integrationTest` (via the `tasks.withType<Test> { check.dependsOn(this) }` loop), so anything hooked through `check` drags in Confluent/Cloud integration tests this milestone explicitly excludes.
**Do this instead:** Hook JaCoCo to `test` only (`test.finalizedBy(jacocoTestReport)`), and have CI invoke `test jacocoTestReport` directly, never `check` (Pattern 4).

### Anti-Pattern 4: Treating `src/integrationTest/java`'s duplicate helpers as a bug to fix

**What people do:** Notice `ClickHouseTestHelpers`/`SchemaTestData`/`SchemalessTestData` are duplicated between `src/test/java` and `src/integrationTest/java` and try to de-duplicate by sharing source sets.
**Why it's wrong:** `integrationTest` is explicitly out of scope this milestone, and de-duplicating requires a `sourceSets["integrationTest"].compileClasspath` change (adding `sourceSets["test"].output`) that's a build-structure change to a component this milestone isn't touching — unnecessary risk for zero milestone value.
**Do this instead:** Leave the duplication alone. Add new fixtures to `src/test/java/.../helper/` only; don't touch the `integrationTest` copies.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| ClickHouse (Testcontainers) | `ClickHouseContainer` from `org.testcontainers:clickhouse`, started per-fork (Pattern 1) | Image tag from `CLICKHOUSE_VERSION` env, default `25.3` locally, pin to `latest` in CI per scope |
| Docker / colima (local) | Must be running before `./gradlew test`; not managed by Gradle | `PROJECT.md` notes colima was not running at project init — verify as part of establishing the green baseline (Build Order step 1) |
| Docker (CI) | Preinstalled on `ubuntu-latest` GitHub-hosted runners | No setup step needed beyond the workflow itself |
| GitHub Actions artifact storage | `actions/upload-artifact@v4` | Coverage report only; no external coverage service (Codecov, etc.) implied by current scope |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Unit tests ↔ `src/main` | Direct method calls, same JVM, no network | Fast; should stay the majority of new SMT/mapping/util test cases |
| CH-backed tests ↔ `ClickHouseBase` | Inheritance (`extends ClickHouseBase`) | The only sanctioned way to get a container/client; don't hand-roll |
| CH-backed tests ↔ real ClickHouse server | HTTP (client V1 or V2, selected by `CLIENT_VERSION` env, default V2 per `extractClientVersion()`) | Constraint: only V2 matters per `PROJECT.md` — still exercise `ClickHouseBase.extractClientVersion()`'s default path, don't need to test V1 branches |
| `test` source set ↔ `integrationTest` source set | **One-directional and incomplete** — `integrationTest` can compile against `main` but not `test` output | Do not attempt cross-source-set reuse (Anti-Pattern 4) |
| `test` task ↔ `jacocoTestReport` task | Gradle task graph (`dependsOn`/`finalizedBy`), not a network boundary | Ordering matters — see Data Flow |

## Suggested Build Order

Dependencies between work items, coarsest-useful granularity (3–5 phases):

**1. Baseline + tooling (must come first, blocks everything else)**
   - Confirm Docker/colima running; run `./gradlew test` as-is against `CLICKHOUSE_VERSION=latest`, `CLIENT_VERSION=V2`; fix or document any pre-existing failures before writing new tests (otherwise new-test failures can't be distinguished from pre-existing ones).
   - Add JaCoCo plugin, wire to `test` only (Pattern 4).
   - Trim CI workflow to the single-job, `latest`×`V2` structure (CI Job Structure section); wire coverage-artifact upload.
   - Apply the container-singleton guard to `ClickHouseBase` (Pattern 1) — do this now, before the class count grows, so every subsequent phase benefits from the faster suite and no phase has to re-verify a lifecycle change against a much larger set of tests later.
   - **Why first:** every later phase's "did I break anything" signal depends on a known-green baseline, a working coverage report, and a CI job that actually reflects the target matrix. The container-singleton change is cheap now and expensive to retrofit once 10+ new CH-backed classes exist.

**2. Fan out: independent test-writing tracks (parallelizable once step 1 lands)**
   - **Track A — SMT/transform unit tests** (`transforms/*`): `TableOperationFilter` truncate/whitespace/invalid-config gaps, `HeadersToValue` null/collision/missing-config gaps, `HeaderToValue` multi-valued/config-parsing gaps, `ConvertToMapOrJson` null/nested gaps. No Docker; fastest track; touches only `transforms/` test files.
   - **Track B — table metadata & type unit+CH-backed tests**: `targetTableFilter` (unit test for the extracted `buildTableFilterClause` seam + CH-backed match/no-match/integration cases), `describeTable` sub-column skipping, `JSON(...)`/`Dynamic` type mapping and real inserts. Needs the container-singleton pattern from step 1 to stay fast as this is the largest new CH-backed cluster.
   - **Track C — explicit JSON insert path & `jsonInsertColumns`**: startup-without-target-table, `getTable`/`cleanupExtraFields` bypass assertions, `jsonInsertColumns` parsing and subset-insert cases. Shares `ClickHouseBase` infra with Track B but touches different main-src files (`ClickHouseWriter`'s JSON-insert branch vs. metadata/mapping code) — independent enough to plan in parallel.
   - **Track D — connector plumbing**: MBean name format (unit, needs the `getMBeanName` visibility seam), `async_insert` non-forcing assertion (config-level unit test + one CH-backed confirmation). Smallest track; can run alongside any of the above.
   - These four tracks are independent by file-touch surface (no two tracks edit the same main-src or test file), so they can be separate plans executed in parallel waves, or separate phases run in any order relative to each other — but all depend on step 1's baseline/tooling/container-singleton work.

**3. Bug-fix-in-place (interleaved with step 2, not a separate phase)**
   - Per `PROJECT.md`'s "fix bugs found by tests in the same phase" decision, any bug a Track A–D test surfaces gets fixed in that same track's plan — don't batch fixes into a later phase. This keeps each track's commit history self-contained (test + fix together) and avoids a "known failing test committed, fixed later" state that complicates bisection.

**4. Final verification (must come last, depends on all of step 2)**
   - Full `./gradlew test` green across every track's additions together (not just per-track — catches cross-track interference, e.g. two tracks both mutating shared fixtures).
   - Review the JaCoCo report for fork-changed classes specifically (not a blanket coverage number).
   - Confirm the CI job runs green end-to-end on the trimmed single-job workflow and the coverage artifact is actually published.

## Sources

- [The JaCoCo Plugin — Gradle User Guide](https://docs.gradle.org/current/userguide/jacoco_plugin.html) — `test`/`jacocoTestReport` task wiring (`dependsOn`/`finalizedBy`), confirmed against this repo's existing `tasks.withType<Test>` block structure.
- [Definitive Guide to the JaCoCo Gradle Plugin — reflectoring.io](https://reflectoring.io/jacoco/) — multi-source-set JaCoCo wiring patterns (relevant to why `integrationTest` must be kept separate here).
- [Singleton containers pattern — Docker/Testcontainers guides](https://docs.docker.com/guides/testcontainers-java-lifecycle/singleton-containers/) — base-class singleton pattern, JVM-shutdown-hook cleanup, informs Pattern 1.
- [Jupiter / JUnit 5 — Testcontainers for Java](https://java.testcontainers.org/test_framework_integration/junit_5/) and [testcontainers-java#1495](https://github.com/testcontainers/testcontainers-java/issues/1495) — confirms the `@Testcontainers`/`@Container` JUnit5 extension is untested/unsupported under parallel execution; this repo's manual `@BeforeAll`/static-field pattern (not the annotation-driven extension) sidesteps that specific risk because Gradle forks are separate JVM processes, not intra-JVM parallel threads.
- This repo: `build.gradle.kts`, `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java`, `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java`, `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java`, `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`, `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java`, `.github/workflows/tests.yaml`, `.planning/codebase/{ARCHITECTURE,TESTING,STRUCTURE}.md`, `.planning/PROJECT.md`.

---
*Architecture research for: test coverage, coverage reporting, and CI for a forked Kafka Connect sink connector*
*Researched: 2026-09-15*
