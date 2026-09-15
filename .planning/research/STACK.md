# Stack Research

**Domain:** Test coverage, coverage reporting, and CI for a Java 17 / Gradle Kotlin DSL Kafka Connect connector fork
**Researched:** 2026-09-15
**Confidence:** HIGH (versions verified directly against GitHub Releases API and Gradle's own plugin source; narrative/migration claims MEDIUM — web-search summarized)

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Gradle `jacoco` core plugin | built-in with Gradle 8.14 (no plugin-portal version to pin) | Line/branch coverage instrumentation + HTML/XML reports | It's a core Gradle plugin (`id("jacoco")`), not a Plugin Portal artifact — nothing to add to `pluginManagement`/`plugins{}` beyond the `id`. Ships bundled with the Gradle distribution the project already pins (`8.14`), so no toolchain/Gradle-version compatibility risk. |
| JaCoCo tool version override | `0.8.15` (latest stable, released 2026-06-05) | The actual coverage engine JaCoCo the Gradle plugin drives | Gradle 8.14's `jacoco` plugin defaults `toolVersion` to **`0.8.13`** (confirmed in Gradle's `JacocoPlugin.java` source: `DEFAULT_JACOCO_VERSION = "0.8.13"`). 0.8.13 already supports Java 17 bytecode fine, so this is not a hard requirement — but pinning explicitly to `0.8.15` is a one-line, zero-risk override that picks up JaCoCo's newest JDK-compatibility and bug fixes rather than silently trusting whatever Gradle 8.14 happened to bundle. |
| `gradle/actions/setup-gradle@v6` | `v6.3.0` (latest stable; `v6.4.0-rc.1` is a prerelease, avoid) | GitHub Actions step that provisions the Gradle wrapper's JDK/Gradle execution environment, caches `~/.gradle`, and writes a build-scan/cache job summary | `gradle/gradle-build-action` (the action currently implied as the "old" pattern) is **officially deprecated as of its own `v3`** — its README states it now transparently delegates to `gradle/actions/setup-gradle@v3`. Since this project's CI predates that migration (`.github/workflows/tests.yaml` still uses `gradle/gradle-build-action@v2`), move directly to the current `gradle/actions/setup-gradle@v6`, the maintained successor. |
| `actions/setup-java@v6` | `v6.0.1` (2026-09-09) | Provisions JDK 17 (and could provision 21) on the runner before Gradle runs | v6 is GitHub's current recommended major (v6.0.0 shipped 2026-08-24, superseding v5.x). The existing workflow pins `actions/setup-java@v3` with `distribution: 'adopt'` — both are stale: `v3` is multiple majors behind, and the `adopt` distribution was renamed/retired in favor of `temurin` years ago. Use `distribution: 'temurin'`. |
| `actions/checkout@v4` | v4.x | Checks out the repo before build | The workflow currently uses `@v3`; v4 is current and matches the setup-java/setup-gradle major bump — no reason to leave checkout on an old major while touching the rest of the workflow. |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| *(none — no new test library needed)* | — | Kafka Connect SMT tests and JMX MBean assertions | See "What NOT to Use" below — this domain doesn't need a new dependency; standard JUnit/Mockito (already present) and the JDK's own `java.lang.management` API cover it. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| `jacocoTestReport` (auto-created by `id("jacoco")`) | HTML + XML coverage report for the `test` task | Configure explicitly — Gradle's default only enables XML for some consumers; must set both `xml.required.set(true)` and `html.required.set(true)`. See config below. |
| Custom `jacocoIntegrationTestReport` task | Optional HTML + XML coverage report for the `integrationTest` Test task | Not auto-created because `integrationTest` is a hand-registered `Test` task, not part of Gradle's `jvm-test-suite` plugin. The `jacoco` plugin still auto-attaches a `JacocoTaskExtension` (coverage agent) to *every* `Test`-typed task, `integrationTest` included — so instrumentation happens for free; you only need a second `JacocoReport` task to turn its `.exec` data into a report. Given `integrationTest` only runs on `workflow_dispatch` (out of the push-CI critical path per PROJECT.md), this is optional/lower priority — wire it if useful, skip it without regret. |
| Custom fork-focused `JacocoReport` task (e.g. `jacocoForkReport`) | Coverage scoped to fork-added/fork-changed classes | JaCoCo's `JacocoReport.classDirectories` accepts a filtered `FileTree` via `include(...)` package globs. Use this to produce a second, smaller report over just the packages this project's fork touches (see config below) without losing the full-project report. |
| GitHub Actions artifact upload (`actions/upload-artifact@v4`) | Publish the HTML report as a downloadable CI artifact | Simplest, zero-new-dependency way to "publish the coverage report" — just archive `build/reports/jacoco/test/html` (and the XML) as a workflow artifact. No coverage-specific action needed for this alone. |
| `madrapps/jacoco-report@v1` *(optional)* | Posts a PR comment summarizing JaCoCo XML coverage (overall % and per-changed-file %) | Only relevant if the workflow should comment on PRs; this repo's workflow currently triggers on `push` only (no `pull_request` trigger), so this is an optional add-on, not a requirement. If added later, point it at the `jacocoTestReport` XML output. |
| `cicirello/jacoco-badge-generator` *(optional, not needed here)* | Coverage badges + job-summary coverage lines + coverage-threshold PR check | More machinery than this project needs (badge generation, threshold gating). Mentioned for completeness; skip unless the user later wants a badge in the README. |

## Installation

```kotlin
// build.gradle.kts — add alongside the existing plugin block
plugins {
    idea
    `java-library`
    `maven-publish`
    signing
    jacoco   // <-- new: core Gradle plugin, no version coordinate needed
    id("com.github.gmazzo.buildconfig") version "6.0.7"
    id("com.diffplug.spotless") version "8.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.ben-manes.versions") version "0.53.0"
}

jacoco {
    toolVersion = "0.8.15"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)   // machine-readable, for CI-published summaries / future PR-comment actions
        html.required.set(true)  // human-readable, uploaded as a CI artifact
    }
}
tasks.check { dependsOn(tasks.jacocoTestReport) }

// Optional: fork-focused report scoped to fork-added packages/classes.
// Adjust the include globs to match wherever the audit lands the fork's SMTs
// and modified classes (transforms/, ClickHouseHelperClient, Column/Type mapping, etc).
val forkChangedClasses = listOf(
    "transforms/**",
    "com/clickhouse/kafka/connect/sink/helper/ClickHouseHelperClient*",
    "com/clickhouse/kafka/connect/sink/db/mapping/**",
)
tasks.register<JacocoReport>("jacocoForkReport") {
    dependsOn(tasks.test)
    executionData(tasks.test.get())
    sourceSets(sourceSets.main.get())
    classDirectories.setFrom(
        files(sourceSets.main.get().output.classesDirs.map { dir ->
            fileTree(dir) { include(forkChangedClasses) }
        })
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// Optional: coverage for the integrationTest Test task (agent attaches automatically;
// this just turns its execution data into a report). Low priority — integrationTest
// is workflow_dispatch-only today.
tasks.register<JacocoReport>("jacocoIntegrationTestReport") {
    dependsOn("integrationTest")
    executionData(tasks.named("integrationTest").get() as Test)
    sourceSets(sourceSets.main.get())
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
```

```yaml
# .github/workflows/tests.yaml — minimal modernization (keep matrix, swap actions)
- uses: actions/checkout@v4
- name: Set up JDK 17
  uses: actions/setup-java@v6
  with:
    java-version: '17'
    distribution: 'temurin'
    architecture: x64
- name: Setup Gradle
  uses: gradle/actions/setup-gradle@v6
- name: Run tests with coverage
  run: ./gradlew test jacocoTestReport
  env:
    CLICKHOUSE_VERSION: ${{ matrix.clickhouse }}
    CLIENT_VERSION: ${{ matrix.client }}
- name: Upload JaCoCo report
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: jacoco-report-${{ matrix.clickhouse }}-${{ matrix.client }}
    path: build/reports/jacoco/test
```

No `npm install`-equivalent step beyond the Gradle build-file edit above — everything here is either a core Gradle plugin (no external coordinate) or a GitHub Actions workflow reference (no local install).

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|--------------------------|
| Core `jacoco` Gradle plugin | JaCoCo Gradle Plugin Portal wrapper artifacts (e.g. third-party convenience plugins) | Never for this project — the core plugin is already bundled with the pinned Gradle 8.14 distribution, is what Gradle documents/supports, and needs zero new Maven coordinate. Only reach for a wrapper plugin if you need multi-module aggregate reports across many subprojects; this is a single-module build. |
| Keep Testcontainers `1.21.4` | Upgrade to Testcontainers `2.0.5` (latest, GA since 2025-10-14) | Upgrade only if the project later needs a 2.x-only feature. 2.x renames every module artifact to a `testcontainers-<module>` prefix (`org.testcontainers:testcontainers-clickhouse`, `testcontainers-kafka`, `testcontainers-toxiproxy`, `testcontainers-junit-jupiter`) and drops JUnit4 support — a real, if mechanical, `build.gradle.kts` diff. PROJECT.md explicitly scopes this milestone to "reuse existing Testcontainers setup" with JaCoCo as the only new dependency; 1.21.4 already runs `ClickHouseBase` against `clickhouse/clickhouse-server:latest` with client V2 successfully today, so there is no functional gap to justify the migration risk on a fork that must stay rebasable against upstream. |
| `gradle/actions/setup-gradle@v6` | Pin to `@v6.3.0` exact SHA/tag instead of the floating major | Pin to an exact tag (or commit SHA) if the project wants fully reproducible/pinned CI actions (common security hardening ask); float on `@v6` if the team is fine taking minor/patch updates automatically. Either is reasonable — default to the floating major for lower maintenance overhead. |
| `actions/upload-artifact@v4` for coverage | `madrapps/jacoco-report` or `cicirello/jacoco-badge-generator` for PR-comment coverage | Add one of these once the workflow gains a `pull_request` trigger and the team wants coverage visible directly on PRs instead of as a downloadable artifact. Not needed for the stated goal ("CI runs the suite... and publishes the coverage report" is satisfied by artifact upload). |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|--------------|
| `gradle/gradle-build-action@v2` (current CI) | Officially deprecated by its own maintainers as of `v3` — the repo's README states it now just delegates to `gradle/actions/setup-gradle@v3`, and no further `gradle-build-action` releases have shipped since (last tag `v3.5.0`, mid-2024). Staying on `@v2` means an unmaintained, two-major-old action. | `gradle/actions/setup-gradle@v6` |
| `actions/setup-java@v3` with `distribution: 'adopt'` (current CI) | `v3` is three majors behind current (`v6`); the `adopt` distribution id is a legacy alias for the discontinued AdoptOpenJDK project naming — Eclipse Temurin is the maintained successor and the distribution GitHub's own docs recommend. | `actions/setup-java@v6` with `distribution: 'temurin'` |
| A new "Kafka Connect test-kit" / SMT test-helper library | None of the fork's SMTs need anything beyond what's already in the build: `org.apache.kafka:connect-api` gives you `SinkRecord`/`SchemaBuilder`/`Struct`/`ConnectHeaders` directly, and the existing tests already construct these by hand (see `TESTING.md` patterns). No community library adds meaningful value over direct construction for transform unit tests — adding one would be a dependency for boilerplate the codebase already has working conventions for. | Keep using JUnit Jupiter + direct `SinkRecord`/`Struct`/`ConnectHeaders` construction, matching existing `transforms/*Test.java` patterns. |
| A JMX assertion library (e.g. some `jmx-test-utils`-style artifact) | JMX MBean assertions need nothing beyond the JDK's own `java.lang.management.ManagementFactory.getPlatformMBeanServer()` + `javax.management.ObjectName`/`MBeanServer.queryNames(...)`, which is standard library, already on the classpath, and sufficient to assert the fork's `connector=<name>` MBean-naming behavior (`describe the MBean's ObjectName string and its attributes directly`). | `java.lang.management` + `javax.management` APIs, called directly from a JUnit test — zero new dependency. |
| `cicirello/jacoco-badge-generator` for this milestone | Solves badge generation and coverage-threshold gating — capability this milestone doesn't ask for ("done" is a coverage report + CI run, not a badge or a merge gate). Adding it now is scope creep against a fork whose constraint is minimal new tooling. | `actions/upload-artifact@v4` uploading the JaCoCo HTML/XML report is sufficient; revisit if the team later wants a badge. |

## Stack Patterns by Variant

**If the team later wants PR-visible coverage (not just artifacts):**
- Add a `pull_request` trigger to the workflow and wire `madrapps/jacoco-report@v1` (or similar) against the `jacocoTestReport` XML output.
- Because the current workflow is `on: [push]` only, this is a deliberate scope decision to defer, not a gap in this research.

**If integration-test coverage becomes a priority (not just unit `test`):**
- Use the `jacocoIntegrationTestReport` task pattern above — the agent is already attached to `integrationTest` automatically by the core `jacoco` plugin (it instruments every `Test`-typed task), so only a report task is missing.
- Keep this out of the push-triggered CI matrix, matching the existing `mustRunAfter("test")` / `workflow_dispatch`-only posture for `integrationTest`.

**If Docker/Testcontainers ever fails on GitHub-hosted `ubuntu-latest` runners:**
- It shouldn't need extra setup: GitHub's `ubuntu-latest` runner images ship Docker Engine pre-installed and running by default (true for the current `ubuntu-24.04`-backed `ubuntu-latest`, and Ubuntu 22.04-backed images before the Sept 2026 deprecation window). No `docker/setup-*` action or `--privileged` step is required for Testcontainers to talk to the runner's Docker daemon.
- Locally (developer machines running colima instead of Docker Desktop), Testcontainers 1.21.4 already auto-negotiates the Docker Engine API version against whatever colima/Docker Engine exposes — this is unrelated to the Testcontainers *library* version and doesn't motivate a 1.x → 2.x upgrade.

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|------------------|-------|
| Gradle `8.14` (project's pinned wrapper) | `jacoco` core plugin, default `toolVersion 0.8.13` | Confirmed directly from Gradle's `JacocoPlugin.java` source (`DEFAULT_JACOCO_VERSION = "0.8.13"`). Overriding to `0.8.15` is safe — both versions understand Java 17 class file format (major version 61), which is all this project's `test` task toolchain produces. |
| Java 17 toolchain (test JVM launcher) + JDK 21 on `PATH` (Gradle daemon JVM) | JaCoCo `0.8.13`–`0.8.15` | No conflict: the JaCoCo Gradle plugin resolves/loads the `org.jacoco:org.jacoco.agent` and `org.jacoco.ant` jars using whatever JVM runs the Gradle daemon (here, JDK 21 from `PATH`, since no Gradle-level toolchain is forced for the daemon itself), while the coverage *agent* attaches to the forked test JVM via `-javaagent`, which is launched under the project's `javaLauncher` (Java 17 toolchain, `build.gradle.kts` lines 180–184). JaCoCo has supported Java 21+ class file analysis since `0.8.8`/`0.8.11`, so both JVMs in play are within its supported range. |
| Testcontainers `1.21.4` | `clickhouse/clickhouse-server:latest` image + ClickHouse Java client `0.9.6` (client V2) | Already proven in this codebase today (`ClickHouseBase`, `CLICKHOUSE_VERSION`/`CLIENT_VERSION` env-driven matrix per `TESTING.md`) — this research found no reason to change it. Testcontainers 2.x is GA (`2.0.5`, 2026-04-20) but is a mechanical artifact-rename migration with no functional benefit here; defer per PROJECT.md's "reuse existing Testcontainers setup" constraint. |
| `gradle/actions/setup-gradle@v6` | Gradle `8.14` wrapper | `setup-gradle` invokes whatever Gradle version the project's own wrapper (`gradle-wrapper.properties`, currently `8.14`) specifies — the action version and the Gradle version are independent; `v6` of the action works with any recent Gradle including `8.14`. Its "Enhanced Caching" (Develocity-backed) is opt-in and not required — the action's default/open-source "Basic Cache Provider" (since action `v6.1.0`) already caches `~/.gradle` without any additional configuration or account. |
| `actions/setup-java@v6` | Java 17 (`java-version: '17'`, `distribution: 'temurin'`) | Straightforward; v6 supports Temurin distribution for Java 17 with no caveats found. Note the workflow does not need to separately provision JDK 21 — only the Gradle *daemon* on developer machines happens to run under JDK 21 from local `PATH`; CI only needs the JDK matching the toolchain used to launch tests (17) plus whatever JDK the `setup-gradle` action itself needs to run Gradle (any JDK ≥ 8 works for running Gradle; the toolchain resolves the actual test-launcher JDK separately). |

## Sources

- Gradle 8.14 `JacocoPlugin.java` source (raw GitHub, `gradle/gradle` repo, tag `v8.14.0`) — confirmed `DEFAULT_JACOCO_VERSION = "0.8.13"` — HIGH confidence (primary source)
- `https://api.github.com/repos/jacoco/jacoco/releases` — confirmed latest JaCoCo `0.8.15` (2026-06-05) — HIGH confidence (GitHub Releases API, primary source)
- `https://api.github.com/repos/testcontainers/testcontainers-java/releases` — confirmed `2.0.5` latest (2026-04-20), `2.0.0` GA (2025-10-14), `1.21.4` still published (2025-12-15) — HIGH confidence (GitHub Releases API)
- `https://api.github.com/repos/gradle/actions/releases` — confirmed `v6.3.0` latest stable (2026-08-02), `v6.4.0-rc.1` prerelease (2026-09-08) — HIGH confidence (GitHub Releases API)
- `https://api.github.com/repos/gradle/gradle-build-action/releases` — confirmed no releases since `v3.5.0` (2024-07-15) — HIGH confidence (GitHub Releases API)
- `https://api.github.com/repos/actions/setup-java/releases` — confirmed `v6.0.1` latest (2026-09-09) — HIGH confidence (GitHub Releases API)
- `gradle/gradle-build-action` GitHub README — confirmed official deprecation notice pointing to `gradle/actions/setup-gradle` — HIGH confidence (primary source, fetched directly)
- WebSearch: Testcontainers 2.x module-rename/migration details (artifact prefix, JUnit4 removal, package-import stability) — MEDIUM confidence (search-summarized, cross-referenced with OpenRewrite's official `testcontainers2migration` recipe docs)
- WebSearch: `madrapps/jacoco-report` and `cicirello/jacoco-badge-generator` feature descriptions — MEDIUM confidence (search-summarized from each project's own README/marketplace listing)
- WebSearch: GitHub `runner-images` ubuntu-latest / Docker preinstalled status, Ubuntu 22.04 deprecation timeline (2026-09-17 start) — MEDIUM confidence (search-summarized from `actions/runner-images` GitHub issue + GitHub changelog)
- Project's own `build.gradle.kts` and `gradle/wrapper/gradle-wrapper.properties` — HIGH confidence (read directly, not researched)

---
*Stack research for: JaCoCo coverage + Testcontainers ClickHouse + GitHub Actions CI on a Java 17 / Gradle Kotlin DSL Kafka Connect connector fork*
*Researched: 2026-09-15*
