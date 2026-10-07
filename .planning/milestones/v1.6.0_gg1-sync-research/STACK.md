# Stack Research — `v1.6.0_gg1-sync` Build Migration

**Domain:** Gradle build-system merge (fork `custom_build` onto upstream `v1.6.0`, JVM/Kafka Connect project)
**Researched:** 2026-09-18
**Confidence:** HIGH — every claim below is read directly from `git show <ref>:<path>` in this repo, not recalled. Version numbers cross-checked against `gradle/libs.versions.toml` at `v1.6.0`.

All commands below were run from `/Users/alexandr.chashnikov/repo/other/clickhouse-kafka-connect_my`. `main` = upstream `v1.2.9` (`bec167d`), `v1.6.0` = upstream (`789a57c`), `custom_build` = the fork (`v1.2.9_gg4`).

---

## 1. Structural changes in `build.gradle.kts`, `main` → `v1.6.0`

`git diff --stat main:build.gradle.kts v1.6.0:build.gradle.kts` → **216 insertions(+), 152 deletions(-)**, matches `PROJECT.md`'s "±368" figure exactly.

| Area | `main` (v1.2.9) | `v1.6.0` |
|---|---|---|
| Dependency declarations | Inline string literals (`"org.apache.kafka:connect-api:${project.extra["kafkaVersion"]}"`) | Version catalog `libs.*` aliases (`libs.kafka.connect.api`), catalog file `gradle/libs.versions.toml` (new, 68 lines) |
| Plugin declarations | `id("...") version "x.y.z"` inline | `alias(libs.plugins.spotless)`, `alias(libs.plugins.shadow)`, `alias(libs.plugins.protobuf)` (new) — catalog-driven |
| Plugin set | `idea`, `java-library`, `maven-publish`, `signing`, `com.github.gmazzo.buildconfig`, `com.diffplug.spotless`, `com.github.johnrengelman.shadow` | `idea`, `java-library`, **`jvm-test-suite`** (new), `maven-publish`, `spotless` (catalog), `shadow` (catalog, **new group ID** `com.gradleup.shadow` not `com.github.johnrengelman.shadow`), **`protobuf`** (new, `com.google.protobuf` plugin), **`java-test-fixtures`** (new). `signing` and `buildconfig` are **dropped**. |
| Java target | `java { sourceCompatibility = VERSION_11; targetCompatibility = VERSION_11 }`, `val defaultJdkVersion = 11` | Same defaults (`VERSION_11`), **no `java.toolchain{}` block at all** — compile-time JDK selection is whatever invokes Gradle; bytecode level enforced separately via `tasks.withType<JavaCompile> { options.release.set(11) }` (new) |
| Test execution | `useJUnitPlatform()` called manually inside `tasks.withType<Test>`; `javaLauncher.set(javaToolchains.launcherFor{...})` selects a **separate toolchain JDK** for running tests; `jdkHome` project-property override; custom `addTestListener{...}` summary block | `useJUnitPlatform()` **removed** from `tasks.withType<Test>` — replaced by `useJUnitJupiter()` declared once per suite inside the new `testing { suites { ... } }` DSL. **`javaLauncher`, `jdkHome` override, and `addTestListener` summary block are all removed.** Tests now simply run on whatever JDK invoked Gradle. |
| Source sets | `sourceSets.create("integrationTest")` manual wiring + `tasks.create("integrationTest", Test::class.java)` manual task | `testing.suites.integrationTest` registered via the `jvm-test-suite` plugin DSL (auto-creates source set + task); depends on `project.dependencies.testFixtures(project(":"))` |
| Test fixtures | none | New `testFixtures` source set (via `java-test-fixtures` plugin) carrying Protobuf/Avro/Schema-Registry test helpers, published via `publishing { ... from(components["java"]) }` (Gradle Module Metadata now carries the testFixtures variant) |
| Confluent archive tasks | `prepareConfluentArchive`, `createConfluentArchive`, `confluentJar` — present, using `$buildDir`-string API | Same three tasks, same purpose, migrated to `layout.buildDirectory.dir(...)` (the non-deprecated Gradle API) |
| New task | — | `runClickHouseVersionClientMatrix` (matrix runner script, `24.8,25.2,25.8,26.3,latest` × `V1,V2`) — irrelevant to this milestone (test target stays `latest`×V2 per locked constraint), but must compile cleanly since it lives in the same file |
| New task | — | `generateVersionFile` — writes `Version.java` with `ARTIFACT_VERSION` from `project.version`; replaces whatever the now-dropped `buildconfig` plugin *would* have done (see below) |
| Spotless config | **No `spotless {}` block, no `removeUnusedImports` task** — plugin applied but never configured (0-op) in `main` | Still no `spotless {}` block — upstream never configured it either |
| `jvm-test-suite` version floor | n/a | Stable since Gradle 7.6; not itself a reason to bump the wrapper (see §4) |

**Dead-plugin finding:** `git grep -n "buildConfig" main -- .` and the same at `custom_build` return **zero matches anywhere in the repo** (not build.gradle.kts, not `src/`). The `com.github.gmazzo.buildconfig` plugin is applied in both `main` and `custom_build` but **never configured and never consumed** — pure overhead carried since the Gradle `init` scaffold. `v1.6.0` drops it and replaces the one thing it could have been for (embedding a version string) with the standalone `generateVersionFile` task that hand-writes `com.clickhouse.kafka.connect.sink.Version.java`. **This is a clean loss, not a regression** — nothing in fork or upstream `src/main` ever referenced `BuildConfig`.

Similarly, `signing` is applied in both `main` and `custom_build` but there is no `signing {}` configuration block in either file — also dead weight, also dropped cleanly by `v1.6.0`.

---

## 2. Dependency version deltas, `v1.6.0` catalog vs. what the fork currently declares

Source: `git show v1.6.0:gradle/libs.versions.toml` vs `custom_build:build.gradle.kts`'s `extra.apply{}` block and inline dependency declarations.

| Dependency | Fork (`custom_build`) | Upstream `v1.6.0` catalog | Verdict |
|---|---|---|---|
| **Kafka Connect API** | `4.1.1` (`extra["kafkaVersion"]`) | `2.7.0` (`kafka` catalog key) | **Fork-only bump, upstream did NOT move.** See §7 OPEN DECISION — this is the single biggest divergence in the whole dependency set and is not mentioned in the locked three build additions. |
| **ClickHouse Java client** (`clickhouse-client`, `-http-client`, `-data`, `client-v2`, `-jdbc`) | `0.9.6` | `0.9.5` | Fork is one patch **ahead** of upstream. Recommend keeping `0.9.6` (fork's tests already pass against it; downgrading is a needless risk). Flagged as low-risk note in §7. |
| **Gson** | `2.13.2` | `2.13.1` | Fork ahead by a patch. Trivial either way — take upstream's catalog value for minimal delta, or keep fork's (no functional difference expected). |
| **Jackson** (core/databind/annotations) | `2.21.0` / `2.21.0` / `2.21` | `2.22.2` / `2.22.2` / `2.22` | Upstream newer. Per the milestone's conflict policy ("prefer upstream wherever it passes the fork's tests"), take upstream's. |
| **Apache HttpClient5** | `5.6` | `5.6.2` | Upstream newer, trivial patch bump. Take upstream's. |
| **Lombok** | `1.18.42` | `1.18.46` | Upstream newer. Take upstream's. |
| **JUnit Jupiter / Platform** | `extra["junitJupiterVersion"] = "6.0.2"` (BOM-driven) **plus** a stray direct pin `testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")` that the BOM's higher version silently wins over (Gradle's default highest-version conflict resolution) | `junitJupiter = "5.14.4"` (catalog), `junit-platform-runner` module (no separate platform version pinned in catalog) | The fork's declared `6.0.2` (JUnit's newer "6.x" umbrella train) is **already ahead** of upstream's `5.14.4`. The stray `5.11.3` direct pin is vestigial dead code (never wins resolution) — safe to drop when re-expressing on the catalog; do not port it forward. Decide `6.0.2` vs. `5.14.4` per §7. |
| **Hamcrest** | `3.0` | `3.0` | Identical, no action. |
| **Mockito** | `5.21.0` | `5.23.0` | Upstream newer. Take upstream's. |
| **Testcontainers** (`testcontainers`, `clickhouse`, `kafka`, `toxiproxy`) | `1.21.4` throughout | `2.0.5` (catalog `testcontainers` version) — **major version bump**, and artifact module renamed to `testcontainers-clickhouse` / `testcontainers-kafka` / `testcontainers-toxiproxy` (catalog aliases) instead of the fork's bare `org.testcontainers:clickhouse`/`kafka`/`toxiproxy` coordinates | **Testcontainers 1.x → 2.x is a major-version jump with its own migration notes** (package/API changes are possible). This directly touches `ClickHouseBase` and every ClickHouse-backed test the fork's suite depends on. Flag as the **highest-risk dependency bump in this merge** — needs its own verification pass once `src/test` is merged, not assumed compatible from the version number alone. |
| **OkHttp3** | `5.3.2` | `5.4.0` | Upstream newer, trivial. |
| **org.json:json** | `20251224` | `20260522` | Upstream newer, trivial. |
| **slf4j** | `2.0.17` via `slf4j-reload4j` (fork keeps the upstream `v1.2.9`-era logging backend; `PROJECT.md` still lists `SLF4J with Reload4j`) | `slf4j = "2.0.18"` catalog version, plus a **new `slf4j-simple` module** (`libs.slf4j.simple`) used only in `integrationTest` | `slf4j-reload4j` is **not in the `v1.6.0` catalog at all** — grep the catalog: no `reload4j` entry. Upstream's `integrationTest` suite now uses `slf4j-simple`. The main `implementation("org.slf4j:slf4j-reload4j:...")` line in the fork's `dependencies{}` block sits outside `libs.*` entirely (hand-written coordinate, marked `// TODO: need to remove ???` in the fork's own comment) — this line has **no catalog equivalent to migrate to**; it must be kept as a literal string dependency after the merge (or intentionally dropped if `.planning/codebase/CONCERNS.md`'s reload4j replacement is ever tackled — explicitly out of scope for this milestone per `PROJECT.md`). |
| **commons-lang3** | not declared | `3.20.0` (catalog, used by `testFixtures`) | New upstream-only dependency, needed for the testFixtures source set the merge inherits. No fork action needed beyond accepting it. |
| **Confluent schema-registry / kafka-connect-avro-converter / kafka-avro-serializer** | not declared | `kafkaPlatformSchemaRegistry = "7.9.1"` (catalog), used by `testImplementation` in the main `test` suite and `testFixturesApi` | New upstream-only test dependencies (used by upstream's new `DebeziumRecordConvertor` tests and schema-registry-backed integration tests). Accept whole per the locked "upstream's 56 test files accepted whole" decision. |
| **Protobuf** (`protobuf-java`, `kafka-protobuf-serializer`, `kafka-connect-protobuf-converter`) | not declared | `libprotobuf = "3.25.9"`, `kafkaPlatformProtobuf = "7.9.1"` (catalog), plus the new `com.google.protobuf` Gradle plugin (`protobufPlugin = "0.10.0"`) needing a `protoc` toolchain download (`protobuf { protoc { artifact = "com.google.protobuf:protoc:..." } }`) | New upstream capability, entirely additive. **Build-environment implication:** the `protobuf` plugin downloads a `protoc` binary at configure time — first `./gradlew` invocation after the merge needs network access to Maven Central for this artifact, same as any other dependency, but worth calling out since it's a native-binary download, not a jar. |
| **JaCoCo tool** | `0.8.15` (fork's own `jacoco { toolVersion = "0.8.15" }`) | plugin not applied at all | No upstream equivalent to reconcile against — pure fork addition, carried forward unchanged (§3). |

**Kafka client transitive note:** the fork's `resolutionStrategy.capabilitiesResolution.withCapability("org.lz4:lz4-java") { select("at.yawk.lz4:lz4-java:1.10.2") }` (custom_build only, absent from both `main` and `v1.6.0`) exists specifically to resolve a capability conflict on the LZ4 compression library, which `PROJECT.md`'s tech stack calls out as "LZ4 compression 1.10.2 (at.yawk variant)." Kafka Connect API 4.1.1's own transitive `kafka-clients` (fork-only, see above) is very likely what introduced this conflict in the first place (older Kafka client versions bundle `org.lz4:lz4-java` directly; newer ones may pull in both `org.lz4` and `at.yawk.lz4` providers via different transitive paths). **This resolutionStrategy block is coupled to the fork's Kafka-version bump and must travel with it** — see §3 for exact re-application.

---

## 3. The fork's build additions — where they live, and the re-application recipe

Confirmed via `git show custom_build:build.gradle.kts` (full file read, 386 lines) and `git diff main:build.gradle.kts custom_build:build.gradle.kts`.

### 3a. The three locked additions

**Addition 1 — JaCoCo reports.** Lives at `custom_build:build.gradle.kts` lines 37 (`jacoco` in `plugins{}`), 237–239 (`jacoco { toolVersion = "0.8.15" }`), 241–276 (`forkChangedClasses` list, 15 entries), 278–300 (`jacocoTestReport` config + `jacocoForkReport` task registration), 302–310 (`finalizedBy` wiring on `test`, `isEnabled = false` override on `integrationTest`).
**Re-application:** copy these blocks verbatim onto `v1.6.0`'s file. No structural conflict — `v1.6.0` has no `jacoco` plugin, no `JacocoReport`/`JacocoTaskExtension` imports, nothing to merge against. Add `jacoco` to the `plugins{}` block (plain `jacoco` core plugin, no catalog entry needed — it ships with Gradle). Add the two `import org.gradle.testing.jacoco.plugins.JacocoTaskExtension` / `import org.gradle.testing.jacoco.tasks.JacocoReport` lines the fork already has at the top of the file (`v1.6.0` doesn't need these removed, they're additive imports). `tasks.named<Test>("test")` and `tasks.named<Test>("integrationTest")` still resolve correctly under `v1.6.0`'s `jvm-test-suite`-based suite registration — the suite plugin still produces conventionally-named `test` and `integrationTest` `Test` tasks, verified by reading `v1.6.0`'s `testing { suites { val test by getting(...); val integrationTest by registering(...) } }` block, which relies on exactly that naming convention itself. **No class-name audit issue** — see §6, all 15 classes still resolve post-merge.

**Addition 2 — `testUnit` task.** Lives at `custom_build:build.gradle.kts` lines 226–235 (`tasks.register<Test>("testUnit") { ...; useJUnitPlatform { excludeTags("clickhouse") }; extensions.configure<JacocoTaskExtension> { isEnabled = false } }`) plus line 176 (`if (name != "testUnit") { tasks.getByName("check").dependsOn(this) }` inside `tasks.withType<Test>`, which is the mechanism that keeps `testUnit` out of `check`).
**Re-application:** copy the task registration verbatim. The `if (name != "testUnit")` guard must be re-inserted into `v1.6.0`'s `tasks.withType<Test>` block, which currently has **no** `check.dependsOn(this)` line at all (upstream relies on `jvm-test-suite`'s own implicit wiring of the main `test` suite into `check`, and never explicitly wires anything else). Because `v1.6.0` doesn't wire `Test` tasks into `check` itself, the fork does not strictly need the guard to protect `testUnit` from `check` — but re-adding the unconditional `tasks.getByName("check").dependsOn(this)` (unguarded) would be a **behavior change**: it would newly pull `testUnit` **and** the `jvm-test-suite`-registered `integrationTest` task into `check`, neither of which happens today in `main`/`custom_build` in a way that matters (nobody runs `./gradlew check` in this project's CI or docs) but is worth doing correctly. **Recommendation: port the `if (name != "testUnit")`-guarded line as-is** — it reproduces exactly today's fork behavior (main `test` wired into `check`, `testUnit` excluded) without silently also wiring in the new `integrationTest` suite task differently from how upstream intends it.
**References `sourceSets["test"]`** — confirmed this still resolves under `v1.6.0`'s `jvm-test-suite` DSL; the `test` suite is `getting(JvmTestSuite::class)` (i.e., adopting the pre-existing `test` source set Gradle's `java-library`/`java` plugin always creates), not a from-scratch suite, so `sourceSets["test"]` remains the same source set object.

**Addition 3 — JDK 17 + `windows-1252` test settings.** This is the addition requiring the most care, because **upstream deleted the mechanism the fork's JDK pinning depends on**, not just the number.
- `custom_build:build.gradle.kts` line 16: `val defaultJdkVersion = 17`
- lines 17–21: `java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }`
- line 179: `systemProperty("file.encoding", "windows-1252")` inside `tasks.withType<Test>`
- lines 185–189: `javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(javaVersion)) })` — selects a JDK-17 **toolchain launcher specifically for running the test JVM**, independent of whatever JDK is running Gradle itself (this is exactly the mechanism `PROJECT.md`'s "Local environment: JDK 21 on PATH; Gradle toolchain targets 17" note describes).
- lines 193–199: `jdkHome` project-property override (lets a caller point at an arbitrary JDK install by path — dev convenience, not required for CI)
- lines 201–220: `addTestListener{...}` custom pass/fail/skip summary printed to the Gradle console — dev convenience, not required for correctness.

`v1.6.0`'s equivalent block (`tasks.withType<Test>` at lines 163–179 of `/tmp/v160_build.gradle.kts`) has **none of `javaLauncher`, `jdkHome`, or `addTestListener`** — upstream removed all three in the same refactor that introduced `jvm-test-suite`. Upstream's `java {}` extension block also carries no `toolchain{}` — only `sourceCompatibility`/`targetCompatibility = VERSION_11`, with the actual JDK selection for compiling delegated to `options.release.set(11)` in `tasks.withType<JavaCompile>` (bytecode-level target, works with any host JDK ≥ 11).

**Re-application recipe:**
1. `val defaultJdkVersion = 11` → `17`.
2. Replace `v1.6.0`'s `java { sourceCompatibility = VERSION_11; targetCompatibility = VERSION_11 }` with the fork's `java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }` block, **or** the lighter-weight equivalent of bumping `options.release.set(11)` → `17` and dropping `sourceCompatibility`/`targetCompatibility` (functionally equivalent for compile output, but does not auto-provision a JDK-17 toolchain for compiling — only CI's `setup-java@v6: java-version: 17` step and each developer's own JDK guarantee that). Given the constraint that Docker/colima and JDK availability are already locally managed (`PROJECT.md`: "JDK 21 on PATH; Gradle toolchain targets 17"), **recommend keeping the toolchain block** — it is what currently makes `./gradlew test` deterministic regardless of which JDK is on a given machine's `PATH`, and costs nothing extra since Gradle toolchain auto-provisioning already works today.
3. Re-insert `javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(javaVersion)) })` into `v1.6.0`'s `tasks.withType<Test>` block — **this is not optional**, it is the actual mechanism enforcing "tests run on JDK 17" independent of the invoking JDK, and upstream no longer has any equivalent.
4. Re-insert `systemProperty("file.encoding", "windows-1252")` into the same block.
5. `jdkHome` override and `addTestListener` summary block are **optional carry-overs** — zero conflict risk, cheap to keep, but not load-bearing for any fork requirement (`PROJECT.md`'s constraints don't mention either). Recommend keeping both for dev-experience parity with today, unless the roadmap wants a smaller diff.
6. `options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))` in `tasks.withType<JavaCompile>` is unaffected — identical in `main`/`custom_build`/`v1.6.0`, needs no change.

### 3b. Two additional fork build customizations NOT covered by the locked three

These exist in `custom_build:build.gradle.kts` but are absent from **both** `main` and `v1.6.0`, so they are not accounted for by the "re-apply JaCoCo / testUnit / JDK17" framing in `PROJECT.md`. Flagging them here so the roadmap phase that does the `build.gradle.kts` migration doesn't silently drop them:

- **LZ4 capability resolution** (lines 145–151): `configurations.configureEach { resolutionStrategy.capabilitiesResolution.withCapability("org.lz4:lz4-java") { select("at.yawk.lz4:lz4-java:1.10.2") } }`. Tied to the fork's Kafka-4.1.1 bump (§2, §7) — must travel together. If §7's OPEN DECISION resolves to keeping Kafka Connect API at upstream's `2.7.0`, **re-verify whether this resolutionStrategy is still needed** (`./gradlew dependencies` after the version decision, watch for an `lz4-java` capability conflict error) rather than carrying it forward unconditionally.
- **`com.github.ben-manes.versions` dependency-update-tracking plugin** (line 41, plus lines 44–54 configuring `dependencyUpdates` to reject unstable candidate versions). `PROJECT.md`'s own tech-stack section lists this ("Ben-Manes Gradle Versions Plugin 0.53.0 — Dependency update tracking") as part of the fork's current stack, but it is not one of the three named build additions and needs the same "port it onto `v1.6.0`" treatment: add `id("com.github.ben-manes.versions") version "0.53.0"` to `plugins{}` (or promote to a catalog alias for consistency with `v1.6.0`'s style) and re-add the `tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates").configure {...}` block verbatim — this block references a plugin-supplied Kotlin type, so it will fail to *compile* the build script (not just misbehave) if the plugin ID is missing.

---

## 4. Gradle wrapper

`git show <ref>:gradle/wrapper/gradle-wrapper.properties`:

| Ref | `distributionUrl` | Extra properties |
|---|---|---|
| `main` (v1.2.9) | `gradle-7.4.2-bin.zip` | none |
| `custom_build` (fork today) | `gradle-8.14-bin.zip` | none |
| `v1.6.0` | `gradle-9.6.1-bin.zip` | `networkTimeout=10000`, `retries=1`, `retryBackOffMs=500`, `validateDistributionUrl=true` |

**The wrapper needs bumping.** The fork is currently on Gradle 8.14; upstream's `v1.6.0` build file was authored and tested against Gradle 9.6.1 (version catalogs, `jvm-test-suite`, `java-test-fixtures`, and the `com.google.protobuf` plugin are all exercised together only at that Gradle version upstream-side). The locked decision to take `build.gradle.kts` "wholesale" from upstream implies taking the wrapper alongside it — a build file this different has only been verified by upstream against its own wrapper version.

**Does a newer Gradle break anything the fork relies on?**
- Gradle 9.x requires a JVM in the **17–26** range to *run* Gradle itself (verified: Gradle's own compatibility matrix). The environment already satisfies this — `PROJECT.md`: "JDK 21 on PATH." CI's `setup-java@v6` step in the fork's `tests.yaml` sets JDK 17, also inside the 17–26 floor. No blocker.
- JaCoCo `0.8.15` (the fork's pinned `toolVersion`) is itself built for and targets JDK 17 (verified via web search of the JaCoCo 0.8.15 release notes: "JaCoCo build now requires JDK 17... depends on ASM 9.6"), consistent with running under Gradle 9 on a JDK-17 test toolchain. One documented caveat: 0.8.15 has an open Gradle issue (`gradle/gradle#39145`) around `ArrayIndexOutOfBoundsException` instrumenting certain large obfuscated Java-17 class files — not a pattern expected in this codebase (no bytecode obfuscation step), noted for completeness rather than as a real risk.
- Version catalogs (`gradle/libs.versions.toml`) have been supported since Gradle 7.0 — no floor-raising need beyond what upstream already requires.
- **Recommendation:** bump the wrapper to `9.6.1` in the same change that adopts `v1.6.0`'s `build.gradle.kts`, rather than trying to run the new build file under the fork's existing 8.14 wrapper (untested combination upstream never exercises, and mixing "upstream's build file" with "fork's old wrapper" works against the stated goal of minimizing fork-specific drift).

---

## 5. CI (`.github/workflows/`) — what survives, what needs rework

`git ls-tree -r <ref> --name-only -- .github/workflows/` is **identical file-for-file** across `main`, `v1.6.0`, and `custom_build`: `increment-version.yml`, `integrationTests.yaml`, `pre-release.yaml`, `release.yaml`, `test-head.yaml`, `tests.yaml`. Only `tests.yaml` matters to this milestone (the fork's Phase 5 already rewrote it; the others are untouched upstream automation the fork doesn't run).

**Upstream `v1.6.0`'s `tests.yaml`** is a substantial rewrite of the `main`/v1.2.9-era single-matrix job (`git show v1.6.0:.github/workflows/tests.yaml`, read in full):
- New `setup` job that resolves either a published ClickHouse-Java-client version or builds one from a branch ref, driven by `workflow_dispatch`/`workflow_call` inputs.
- `standalone-tests` job: matrix of `client: [V1, V2]` × `clickhouse: [24.8, 25.2, 25.8, 26.3, latest]` (resolved dynamically from `setup`) × `java: [17, 21, 25]` — a 3-dimensional matrix, triggered on `push`/`pull_request` to `main` (not bare `on: [push]`).
- `cloud-tests` job (credential-gated, same `SKIP_STEP` pattern the fork's pre-v1.6.0 baseline already had).
- New `cluster-tests` job (`three_shards_one_replica_each` / `one_shard_three_replicas`) — did not exist at `main`/v1.2.9.
- A new composite action `.github/actions/use-clickhouse-java/action.yaml` (confirmed present at `v1.6.0`, absent from `main`) that overrides the resolved ClickHouse Java client artifact/build for every job.
- Newer/different action pins than the fork's rewrite in places (`actions/checkout@v3` vs. the fork's `@v4`; `actions/setup-java@v5`/`@v3` vs. the fork's `@v6`; `gradle/actions/setup-gradle@<pinned-sha>` vs. the fork's `@v6` tag) — **the fork's action versions are already ahead of upstream's `v1.6.0` in every case**, so no upgrade is needed there.

**The fork's `custom_build:.github/workflows/tests.yaml`** (already fully rewritten in Phase 5, per `PROJECT.md`) is a **single job**, bare `on: [push]`, `CLICKHOUSE_VERSION=latest`/`CLIENT_VERSION=V2` only, running `./gradlew test`, then `.github/scripts/jacoco_summary.py`, then two `upload-artifact@v4` steps. It has zero dependency on `build.gradle.kts`'s internal structure — it only shells out to `./gradlew test`, so **it survives the build-file migration unchanged**, provided `./gradlew test` still succeeds after §3's re-application work (which is the actual gate, not the workflow file itself).

**What needs rework vs. survives unchanged:**
- Survives unchanged: the fork's single-job structure, trigger (`on: [push]`), env vars (`CLICKHOUSE_VERSION=latest`, `CLIENT_VERSION=V2`), the `./gradlew test` invocation itself, both artifact-upload steps, and `.github/scripts/jacoco_summary.py` (see §6 — its hardcoded XML paths match the JaCoCo tasks being re-applied byte-for-byte).
- Needs a one-line touch: `actions/setup-java@v6` already requests `java-version: '17'` — no change needed there either, since §3's JDK-17 re-application keeps the toolchain at 17.
- Nothing in the fork's `tests.yaml` references `use-clickhouse-java`, the cloud-tests job, the cluster-tests job, or the standalone matrix — none of upstream's new CI machinery needs to be merged into the fork's workflow. It can be dropped/ignored during the merge's conflict resolution for this file, consistent with the locked "test matrix stays `latest`×V2 only" constraint.
- **One real risk to verify, not a workflow-file change:** the fork's CI job has a `timeout-minutes: 45`. With Testcontainers bumped 1.21.4 → 2.0.5 (§2) and the test suite growing by upstream's 56 new test files (`PROJECT.md`: "+9,726/−4,609" across test sources), first-CI-run wall-clock time after the merge is unverified and could approach or exceed 45 minutes. Not a build-stack defect, but a number worth watching on the first green run.

---

## 6. `jacocoForkReport`'s 15-class list — audited against `v1.6.0`

Checked each of the 15 `src/main/java` file paths the class list encodes with `git cat-file -e v1.6.0:<path>`:

| # | Class | Exists at `v1.6.0`? | Note |
|---|---|---|---|
| 1 | `transforms/TableOperationFilter` | **No** | Fork-only file — confirmed absent from `main` too (`git cat-file -e main:...` fails identically). Not a rename/move; upstream never had this class. Survives the merge as a net-new fork file (no conflict surface), and remains listed in `jacocoForkReport` unchanged. |
| 2 | `transforms/HeadersToValue` | **No** | Same as above — fork-only, confirmed absent from `main`. |
| 3 | `transforms/HeaderToValue` | **No** | Same. |
| 4 | `transforms/HeaderToValueConfig` | **No** | Same. |
| 5 | `transforms/ConvertToMapOrJson` | **No** | Same. |
| 6 | `sink/db/helper/ClickHouseHelperClient` | **Yes** | Exists at `v1.6.0`; upstream applied a large rewrite here too (`PROJECT.md`: "+359"). |
| 7 | `sink/db/mapping/Column` | **Yes** | |
| 8 | `sink/db/mapping/Type` | **Yes** | |
| 9 | `sink/db/mapping/Table` | **Yes** | |
| 10 | `sink/db/ClickHouseWriter` | **Yes** | Upstream's biggest rewrite of the merge (`PROJECT.md`: "±1,289 — effectively rewritten"). Class still exists at this path; content divergence is a merge-conflict-resolution problem, not a build/coverage-report problem. |
| 11 | `sink/ClickHouseSinkConfig` | **Yes** | |
| 12 | `sink/ProxySinkTask` | **Yes** | |
| 13 | `sink/ClickHouseSinkTask` | **Yes** | |
| 14 | `ClickHouseSinkConnector` | **Yes** | |
| 15 | `util/jmx/MBeanServerUtils` | **Yes**, and **byte-identical in content** to `main` — `git diff main:.../MBeanServerUtils.java v1.6.0:.../MBeanServerUtils.java` produces no output. Upstream never touched this class between v1.2.9 and v1.6.0. |

**Verdict: the class list needs zero edits.** No class was renamed, moved, or deleted upstream. The 5 "missing" entries are missing because they are fork-only additions that don't exist upstream at any version (not a v1.6.0-specific gap), and they will exist again immediately once the merge lands the fork's own files — at which point all 15 paths resolve identically to how they do in `custom_build` today. `.github/scripts/jacoco_summary.py`'s `FORK_CLASSES_IN_ORDER` list (which mirrors `build.gradle.kts`'s `forkChangedClasses`, minus the `$*.class` wildcard entries) needs no edits for the same reason, and its two hardcoded XML paths (`build/reports/jacoco/test/jacocoTestReport.xml`, `build/reports/jacoco/fork/jacocoForkReport.xml`) match exactly what §3a's re-applied `jacocoTestReport`/`jacocoForkReport` task blocks will produce, since those blocks are copied verbatim.

---

## 7. Open decisions

These are stack-level forks in the road the user has not resolved. None contradicts a locked decision — they're one layer more specific than what's already been decided.

### OPEN DECISION 1 — Kafka Connect API version: keep the fork's `4.1.1`, or drop to upstream's `2.7.0`?

**Finding:** `v1.6.0`'s `gradle/libs.versions.toml` still pins `kafka = "2.7.0"` — upstream has **not** moved this dependency across 581 commits. The fork's own bump to `4.1.1` (via three intermediate WIP commits: `2.7.0 → 7.9.0-ce → 4.0.0 → 4.1.1`, confirmed by `git log -p main..custom_build -- build.gradle.kts`) is entirely fork-driven, presumably to track the newer Kafka brokers the fork's CDC/Debezium production pipelines actually run against.

**Options:**
- **A. Keep `4.1.1`** (current fork behavior). Preserves the fork's existing tested/production API surface. Requires overriding the catalog's `kafka` version key (one line in `gradle/libs.versions.toml`) and keeping the LZ4 `resolutionStrategy` block (§3b) since it's very likely coupled to this newer Kafka client's transitive dependencies.
- **B. Adopt upstream's `2.7.0`.** Shrinks fork delta (the milestone's own conflict-resolution policy: "prefer upstream wherever its implementation passes the fork's tests"). Risk: the fork's own `ProxySinkTask` JMX-naming code and any Kafka-Connect-API-surface usage the fork added (`connector=` MBean naming, custom SMTs implementing `Transformation<R>`) were built and tested against 4.1.1's API shape; downgrading to 2.7.0's older `connect-api` could break compilation of fork-added transform classes if any 4.x-only API was used, or silently change runtime behavior against production Kafka Connect workers running newer brokers.

**Recommendation:** Keep `4.1.1` (Option A). This is a deliberate production-driven fork decision (not build-scaffold cruft like `buildconfig`/`signing`), the milestone's stated conflict-resolution policy is about *implementation* conflicts on files both sides touched, not about silently downgrading a fork-only dependency version that upstream simply hasn't reached yet, and the risk of B breaking either compilation or runtime Kafka-broker compatibility is higher than the cost of A (one catalog-version override plus the already-existing LZ4 workaround). Flag for explicit user confirmation before the `build.gradle.kts` merge phase is planned, since it directly affects whether §3b's LZ4 `resolutionStrategy` block is still needed.

### OPEN DECISION 2 — Testcontainers 1.21.4 → 2.0.5: verify before trusting

**Finding:** `v1.6.0`'s catalog pins `testcontainers = "2.0.5"` (major-version bump from the fork's current `1.21.4`), with module coordinates renamed to `testcontainers-clickhouse` / `testcontainers-kafka` / `testcontainers-toxiproxy` (catalog aliases) rather than the fork's bare `org.testcontainers:clickhouse`/`kafka`/`toxiproxy`. This directly underpins `ClickHouseBase` (one shared container per test JVM fork, per-class databases, the MBean-leak-sweep pattern) — infrastructure the fork's entire 371-test suite depends on.

**Options:**
- **A. Accept upstream's `2.0.5` wholesale** (consistent with "upstream's 56 test files accepted whole" and "`ClickHouseBase`... rewritten test infrastructure" already named in `PROJECT.md`'s target features). This is almost certainly what the milestone already intends, since `PROJECT.md` explicitly names `ClickHouseBase`/`ClickHouseTestHelpers`/`SchemaTestData` as infrastructure the fork's 371 tests will be adapted onto.
- **B. Pin Testcontainers back to `1.21.4` while adopting everything else from `v1.6.0`.** Smaller step, but fights the version catalog (would require overriding a catalog value the rest of upstream's new test infra was authored against) and risks incompatibility between `1.x` Testcontainers APIs and whatever `2.x`-authored test helper code (`ClickHouseTestHelpers`, `SchemaTestData`) ships in upstream's 56 test files.

**Recommendation:** Option A — this isn't really open given the milestone's already-stated target features, but it's called out here as a **build-stack risk to size explicitly into the roadmap's test-adaptation phase**, not something to discover mid-merge. This is the single highest-uncertainty dependency bump in the whole build migration and deserves its own verification checkpoint (does `ClickHouseBase` compile and start a container correctly under Testcontainers 2.0.5 before any of the 371 fork tests are touched).

### OPEN DECISION 3 — JUnit Jupiter: fork's `6.0.2` vs. catalog's `5.14.4`

**Finding:** the fork's `extra["junitJupiterVersion"] = "6.0.2"` (BOM-driven, `PROJECT.md` lists the still-lower `5.11.3` — that number is the vestigial direct pin described in §2, not what actually resolves) is ahead of `v1.6.0`'s catalog `junitJupiter = "5.14.4"`. Low risk either direction (JUnit 5→6 migration notes are minor for typical Jupiter usage), but worth a conscious pick rather than an accidental one, since the catalog value silently becomes the effective version if the fork's `extra` override isn't ported forward.

**Recommendation:** Take upstream's `5.14.4` unless there's a specific reason the fork needs `6.x` — smaller diff, matches the milestone's stated conflict-resolution policy, and the fork's stray unused `5.11.3` direct pin should simply be deleted (not migrated) since it was dead code even before this merge.

### OPEN DECISION 4 — LZ4 `resolutionStrategy`: keep or re-derive

Directly downstream of OPEN DECISION 1. If Kafka Connect API stays at `4.1.1`, keep the `at.yawk.lz4:lz4-java:1.10.2` capability-resolution block (§3b) as-is. If the roadmap somehow chooses Option B in Decision 1, re-run `./gradlew dependencies` after the version drop and only keep the block if the conflict still reproduces — don't carry it forward by default in that branch.

---

## Sources

- `git show main:build.gradle.kts`, `git show v1.6.0:build.gradle.kts`, `git show custom_build:build.gradle.kts` — full file reads, this repo, `bec167d` / `789a57c` / fork tip.
- `git diff --stat main:build.gradle.kts v1.6.0:build.gradle.kts` and `... custom_build:build.gradle.kts` — line-count verification against `PROJECT.md`'s claimed deltas (both matched exactly: 216+/152− and 162+/33−).
- `git show v1.6.0:gradle/libs.versions.toml` — full catalog read, version-by-version cross-check.
- `git show <ref>:gradle/wrapper/gradle-wrapper.properties` for `main`, `v1.6.0`, `custom_build`.
- `git show <ref>:settings.gradle.kts` for `main`, `v1.6.0`, `custom_build`.
- `git show custom_build:.github/workflows/tests.yaml`, `git show v1.6.0:.github/workflows/tests.yaml`, `git show main:.github/workflows/tests.yaml` — full reads.
- `git show custom_build:.github/scripts/jacoco_summary.py` — full read of the class list and XML-path constants.
- `git cat-file -e v1.6.0:<path>` for all 15 `jacocoForkReport` class source files, plus `main`/`custom_build` cross-checks for the 5 fork-only transform classes.
- `git log -p main..custom_build -- build.gradle.kts` — commit-level trace of the Kafka version bump (`2.7.0 → 7.9.0-ce → 4.0.0 → 4.1.1`) and confirmation of the `[WIP] bump versions` commits responsible.
- `git grep -n "buildConfig\|BuildConfig"` and `git grep -n "ben-manes\|dependencyUpdates"` across `main`/`custom_build`/`v1.6.0` — dead-plugin and undocumented-addition verification.
- Web search: JaCoCo 0.8.15 / Gradle 9.6 / JDK 17 compatibility — confidence MEDIUM (community/issue-tracker sources, not vendor docs), used only to corroborate the wrapper-bump recommendation in §4, not as the basis for any locked claim.

---
*Stack research for: `v1.6.0_gg1-sync` build-and-dependency migration*
*Researched: 2026-09-18*
