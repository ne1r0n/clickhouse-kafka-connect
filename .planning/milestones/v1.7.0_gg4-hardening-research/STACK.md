# Stack Research

**Domain:** Kafka Connect sink fork (Java 17, Gradle 9.x, version catalog) — milestone `v1.7.0_gg4-hardening`, dependency absorption
**Researched:** 2026-10-21
**Confidence:** HIGH on Shadow, connect-api, guava and the upstream tail (read from source, released jars, poms and commits). MEDIUM on protobuf PR #10 (its CI is red and the failing test names could not be read). MEDIUM on the "what ships" set (derived from `build.gradle.kts` and published poms, not from a built jar).

Scope: only what the NEW work needs. The ClickHouse client stays `0.9.6` (PR #4 untouched). The baseline is `origin/custom_build` at `69cd680b`, which is 2 commits past the `d20bae63` named in PROJECT.md (Dependabot #14 jackson `2.22.3`→`2.22.4` and #15 slf4j-simple `2.0.18`→`2.0.20` landed 2026-10-21 10:22 UTC). Where the milestone brief says jackson `2.22.3`, the catalog now says `2.22.4`.

## Verdict (read this first)

| # | Item | Needs code change? | Verdict |
|---|------|--------------------|---------|
| 1 | `connect-api` 4.1.1 → 4.4.0 (#9, merged) | No in `src/main`. Yes in docs, tests' comments and the invariant anchor | Safe. Public API of every class the fork imports is unchanged; one new interface (`ConnectPlugin`) |
| 2 | Shadow 8.3.5 → 9.6.1 (#11, merged) | **YES, blocking.** `confluentJar` is silently broken | `from(clickhouseDependencies)` no longer unzips jars in Shadow 9. The shipped Confluent jar now contains nested `.jar` files instead of classes. CI cannot see it |
| 3 | Gradle wrapper 9.6.1 → 9.8.1 (#8, merged) | No | Safe. 9.8.1 also carries three security fixes in Gradle itself |
| 4 | protobuf-java 3.25.9 → 4.36.2 (#10, open) | Not mergeable as-is | PR's own CI run is **red**. No security driver (3.25.9 has no advisory). Do not merge this milestone |
| 5 | Upstream tail (guava, lz4, httpclient5) | Yes, small, one conflicted hunk in the catalog | Guava CVE is CVE-2026-102554 (fixed in 33.7.2). Keep fork's `lz4Java=1.12.0`, not upstream's `1.11.4` |
| 6 | Other CVEs | No | Shipped set is clean in OSV as of today. Guava is the one new advisory (not yet indexed by OSV) |

## 1. Kafka `connect-api` 4.1.1 → 4.4.0

### What changed (verified against the released jars, not release prose)

I downloaded `connect-api-4.1.1.jar` and `connect-api-4.4.0.jar` from Maven Central and diffed `javap -public -s` output over all classes, then diffed the public signatures of the `kafka-clients` classes the fork imports.

| Finding | Evidence | Impact on the fork |
|---------|----------|--------------------|
| The only new class in `connect-api` is `org.apache.kafka.connect.components.ConnectPlugin` (`extends Versioned`, declares abstract `ConfigDef config()`). It first appears in 4.3.0 (404 at the 4.2.0 tag, 200 at 4.3.0) | `diff` of class lists | None at compile time |
| `Connector`, `Transformation`, `Converter`, `HeaderConverter`, `Predicate`, `ConnectRestExtension`, `ConnectorClientConfigOverridePolicy` now extend `ConnectPlugin` | `javap` diff | `SinkConnector` (via `Connector`) and all 6 fork SMTs (`implements Transformation`) still compile. `Transformation.config()` is still abstract (now inherited). `Transformation.version()` is a new `default` returning `"undefined"` |
| `SinkTask`, `SinkRecord`, `SinkConnector`, `SinkTaskContext`, `SinkConnectorContext`, `ErrantRecordReporter`, `ConnectRecord`, `Schema`, `Struct`, `Field`, `SchemaBuilder`, `Timestamp`/`Time`/`Date`/`Decimal`, `Header`, `DataException`/`RetriableException`/`ConnectException`: no signature change (only an `@InterfaceAudience.Public` annotation was added) | `javap` + source diff | None |
| `SinkTask.onPartitionsRevoked(Collection<TopicPartition>)` is still absent | `SinkTask` diff = annotation only | The reason for the fork's missing `@Override` (FORK-DELTA connector-source row, Recorded Fact 8) still holds. Re-verified, so the "re-verify if the pin changes" note can be closed |
| `ConfigDef`: no change to any public overload the fork calls. Only `ValidList` validator semantics changed (KIP-1161: no nulls, no duplicates, no empty lists) | source diff 4.1.1 vs 4.4; Kafka 4.2 upgrade notes | The fork's two `ConfigDef.Type.LIST` keys (`clickhouseSettings`, `topic2TableMap`, `ClickHouseSinkConfig.java` lines ~525 and ~534) use no `ValidList` validator, so unaffected |
| `kafka-clients` classes the fork imports (`OffsetAndMetadata`, `TopicPartition`, `AbstractConfig`, `Config`, `ConfigException`, `ConfigDef` and its nested types, `common.cache.{Cache,LRUCache,SynchronizedCache}`): public signatures identical except one new constant `AbstractConfig.CONFIG_PROVIDERS_DOC` | `javap -public` diff of the two `kafka-clients` jars | None. `common.cache.*` is internal API that `KeyToValue` (upstream-owned) uses; it still exists in 4.4.0 |
| Class-file level: `connect-api` 4.4.0 is Java 17 bytecode (major 61) | read class header | Fine with the JDK 17 toolchain. The Connect worker must be Java 17+, already true since Kafka 4.0 ("brokers, connect and tools now require Java 17", 4.0 upgrade notes) |
| DLQ header rendering in `connect-runtime` (`DeadLetterQueueReporter`) is unchanged between 4.1.1 and 4.4 (an import rename and `Set.of` only) | source diff | The `InMemoryDLQ.connectHeaders` / `ThrowableTexts` test doubles that model "Connect 4.1.1" stay faithful |
| Kafka 4.2 deprecates `PrincipalConnectorClientConfigOverridePolicy`; Kafka 4.3 deprecates `kafka-streams-scala`; 4.4 adds KIP-1265 (opt-in build-time internal-API checker, "no runtime behaviour changes") and a worker config for internal topic creation | Kafka 4.2/4.3 upgrade notes, KIP-1265 page, 4.4.0 release notes | None: all worker-side or opt-in |
| Not found | The 4.4.0 upgrade page on kafka.apache.org returned 404 on 2026-10-21 (release is ~1 day old; Maven Central metadata `lastUpdated 20261020`). The jar and source diffs above stand in for it | Re-check the official 4.4 upgrade notes when published |

`connect-json` and `connect-transforms` are not declared by the fork and not imported by `src/main`; they reach the build only through the Confluent test stack. Nothing to absorb there.

### Things that DO need touching (no behaviour change)

| File | What | Why |
|------|------|-----|
| `.planning/scripts/fork-anchors.txt` line 48 | `gradle/libs.versions.toml	kafka = "4.1.1"	1	BLD-08` → `kafka = "4.4.0"` | `verify-merge-invariants.sh` reports `expected 1 got 0` otherwise |
| `.planning/scripts/verify-merge-invariants-selftest.sh` lines 138, 156, 167 | Replace the `4.1.1` needle and the `sed 's/4.1.1/4.1.2/'` fixture | Self-test hard-codes the old pin |
| `src/main/.../ClickHouseSinkTask.java` lines ~150-153 | Comment text "connect-api 4.1.1 removed ... costs nothing under 4.1.1" | Comment only. Rewrite to "4.4.0" and keep the method without `@Override` |
| `InMemoryDLQ.java`, `ThrowableTexts.java`, `ValueFreeDlqHeadersUnitTest.java` | Javadoc says "Connect 4.1.1" | Comment only; behaviour verified unchanged |
| `docs/FORK-DELTA.md` pins row and the `onPartitionsRevoked` row; rollout note | State `4.4.0` and the real meaning of the pin | See next point |

### Correct the FORK-DELTA claim about what the pin means

FORK-DELTA says "Production runs the shaded jar built here, so `4.1.1` is the production Connect API version." The Confluent jar is built from `clickhouseDependencies`, which lists the ClickHouse client, httpclient5, lz4, gson, jackson (and guava after the tail). It does not list Kafka. So `connect-api` is **not** inside the shipped jar. The pin only fixes the compile-time API and the test classpath. At runtime the connector uses the worker's own Connect API. Because the signatures the fork calls are identical, the compiled bytecode references the same members as before, so no new minimum worker version is introduced. This is worth one sentence in the rollout note ("compiled against 4.4.0; no member added after 4.1 is referenced"). To prove it rather than assert it, grep `javap -c -p` output of the built classes for `ConnectPlugin` and `InterfaceAudience` (expect no matches).

### Does the Confluent 7.9.1 test stack still resolve against 4.4.0?

Evidence says yes: `tests.yaml` runs on `d20bae63` (contains #9) and on `69cd680b` are `success` (runs 39555414987 and 39584176889). There is one latent point to know, not a new defect:

- `io.confluent:kafka-schema-registry-client:7.9.1` declares `org.apache.kafka:kafka-clients` at `7.9.1-ccs` (parent `common-parent:7.9.1` sets `kafka.version=7.9.1-ccs`). Gradle orders `7.9.1-ccs` above `4.4.0` (7 > 4), so on the **test** classpaths `kafka-clients` very probably resolves to the Confluent 3.9-line build while `connect-api` stays 4.4.0. That mix was already true with 4.1.1.
- It is the reason `org.lz4:lz4-java:1.8.0` still shows up on `testRuntimeClasspath`: `kafka-clients:7.9.1-ccs` depends on it. So the fork's `testRuntimeClasspath` exclude (DEBT-07) and the `org.lz4:lz4-java` capability rule **both stay necessary**.
- Cheap confirmation (planner, not run by me): `./gradlew dependencyInsight --configuration testRuntimeClasspath --dependency kafka-clients`.
- `kafka-clients:4.4.0` itself now depends on `at.yawk.lz4:lz4-java:1.11.2` (was `org.lz4:lz4-java:1.8.0` in 4.1.1; read from both poms). On `runtimeClasspath` there is therefore no `org.lz4` candidate any more; the capability rule only matters on test classpaths.

Confidence: HIGH for the API facts (jars). MEDIUM for the test-classpath resolution (reasoned from poms and Gradle's ordering, corroborated by green CI, not by a `dependencyInsight` run).

### Optional, not required

`Transformation.version()` now defaults to `"undefined"`, so a 4.3+ worker reports the six SMTs as version `undefined` in `/connector-plugins`. Returning `Version.ARTIFACT_VERSION` from each SMT would fix that. Do not do it in this milestone (new behaviour, six more files in the fork delta).

## 2. Shadow `com.gradleup.shadow` 8.3.5 → 9.6.1 — a blocking defect in `confluentJar`

Source: Shadow changelog and `docs/changes/README.md` (9.0.0 entry), plus the 9.6.1 source of `ShadowJar.kt` and `ShadowCopyAction.kt`, plus the 8.3.5 `ShadowCopyAction.groovy` from the local Gradle cache.

### What breaks

The fork registers `tasks.register<ShadowJar>("confluentJar") { archiveClassifier.set("confluent"); from(clickhouseDependencies, sourceSets.main.get().output) }` (`build.gradle.kts` lines 362-365 on `origin/custom_build`).

- Shadow 8.x `ShadowCopyAction` treated every archive that came through `from(...)` as something to unzip (`isArchive(fileDetails)` → `processArchive`). So `from(<configuration of jars>)` produced a fat jar.
- Shadow 9.0.0: "BREAKING CHANGE: Align the behavior of `ShadowTask.from` with Gradle's `AbstractCopyTask.from` ... it will not unzip the files, only copy the files as-is." (PR #1233). The 9.6.1 `ShadowCopyAction` has no archive detection at all. Unzipping now happens only for the task's `configurations` property, via `addIncludedDependencies()` → `from(archiveOperations.zipTree(file))`.
- `configurations` has no default on a task you register yourself. Only the plugin's own `shadowJar` task gets `configurations.convention(runtimeClasspath)` (`ShadowJavaPlugin.configureShadowJar`). So `confluentJar` has an empty `includedDependencies`, and each ClickHouse client jar lands as an opaque nested `*.jar` entry.

Result: `createConfluentArchive` still succeeds, the zip still has the right name, and the connector fails at class load in a Connect worker (`NoClassDefFoundError` for `com.clickhouse.client...`) because Connect's plugin classloader does not open nested jars.

### Why nothing caught it

`.github/workflows/tests.yaml` runs `reportTestForks`, `compileIntegrationTestJava`, `test`. None of them realises `confluentJar`. Only the `integrationTest` task depends on `prepareConfluentArchive`, and CI does not run it (the nightly `integrationTests.yaml` is disabled). `tasks.register` is lazy, so the task's configuration block is not even evaluated on a green run. No test references the Confluent jar (grep over `src/` finds none). PR #11's green CI therefore says nothing about this jar. The only precedent check is the manual jar comparison in `24-COMMAND-EVIDENCE.md` E-04 (7375 entries compared with `cmp.py`).

### Fix (recommended: idiomatic Shadow 9)

```kotlin
tasks.register<ShadowJar>("confluentJar") {
    archiveClassifier.set("confluent")
    // Shadow 9: from(<jars>) copies jars as-is; only `configurations` are unzipped.
    configurations.add(clickhouseDependencies)
    from(sourceSets.main.get().output)
    // Keep the manifest as v1.7.0_gg3 shipped it (see "Differences to expect").
    addMultiReleaseAttribute.set(false)
}
```

Notes:
- Inside the lambda `configurations` resolves to `ShadowJar.configurations` (the receiver wins over the project-level container). `clickhouseDependencies` is the script-level `val`. Keep a short comment so nobody "simplifies" it.
- Import path `com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar` and plugin id `com.gradleup.shadow` are unchanged in 9.6.1 (verified in source). `archiveClassifier`, the `tasks.withType<Jar>` manifest block, and the output file name `…-confluent.jar` consumed by `prepareConfluentArchive` are unchanged.
- Shadow's dependency filter walks `resolvedConfiguration.firstLevelModuleDependencies` recursively, so the transitive closure matches what `from(configuration)` produced.
- Fallback if the entry diff shows an unwanted difference: reproduce 8.x exactly with `from(provider { clickhouseDependencies.files.map { zipTree(it) } }, sourceSets.main.get().output)`. Same ordering as before (dependencies first), no `Multi-Release` attribute.

### Differences to expect in the new jar (use these as the expected delta in the entry diff)

| Difference | Cause | Decision |
|------------|-------|----------|
| `MANIFEST.MF` gains `Multi-Release: true` | Shadow 9.0.0 injects it when any included dependency's manifest has it. Checked on Central: jackson-core, jackson-databind, gson, commons-lang3/-io/-compress/-codec, RoaringBitmap, slf4j-api and all four ClickHouse client jars have it; httpclient5, lz4-java, guava do not | `addMultiReleaseAttribute.set(false)` preserves `v1.7.0_gg3`. Turning it on would activate `META-INF/versions/**` classes in production. That is behaviour change, so leave it off this milestone |
| Entry order: main classes now precede dependency entries (dependencies are added at copy time) | Shadow 9 adds `includedDependencies` after the declared `from` | With `duplicatesStrategy = EXCLUDE` (default in both versions, restored in 9.0.1) first wins. The fork's own `META-INF/services/org.apache.kafka.connect.transforms.Transformation` has a unique name, so no overlap is expected. Confirm with the diff |
| Guava and its dependencies added (see section 5) | Upstream tail | Expected |

### Other Shadow 9 changes checked and found not to apply

`relocate`, `minimize`, `mergeServiceFiles`, transformers, `ShadowSpec` removal, `Transformer`→`ResourceTransformer`, `isEnableRelocation` rename: the build uses none of them. `@ShadowDsl` (9.6.0) restricts implicit outer-receiver calls only between nested Shadow-annotated receivers; the `confluentJar` block has one Shadow receiver and calls `sourceSets` on `Project`, which is not annotated. Minimum Java is 17 (9.2.1) and minimum Gradle 9.2.0 (9.5.0); both met.

### One side effect worth a one-line setting

Since 9.0.0, `assemble` depends on `shadowJar`. The plugin creates a default `shadowJar` (`*-all.jar` of `runtimeClasspath`), so `./gradlew build` or `assemble` now also builds a fat jar that is not the shipped artifact. CI does not run either. If you want to avoid a stray `-all.jar` and the extra build time locally, add `shadow { addShadowJarToAssembleLifecycle.set(false) }` (option added in 9.4.0). Optional.

### Guard so this cannot recur silently

Add to the milestone, as user-approved CI hygiene like the earlier CI changes: a CI step `./gradlew createConfluentArchive` followed by a check that the jar contains no nested `*.jar` entries and does contain `com/clickhouse/kafka/connect/ClickHouseSinkConnector.class` and `com/clickhouse/client/api/Client.class`. About 30 s on the runner. Without it the next Shadow bump reopens the same hole.

### Verification recipe (same method as E-04)

Build `clean createConfluentArchive` on the pre-merge tip (`6a2a87a5`) and on the absorbed tree in separate throwaway worktrees, unpack each jar into fresh empty directories under `/tmp`, compare entry names and SHA-256 per entry with a script run via `python3 -I`. Pass criteria: zero `.jar` entries; only-in-old empty except nothing; only-in-new limited to the guava set and `MANIFEST.MF` content equal to the old one (with `addMultiReleaseAttribute` off).

## 3. Gradle wrapper 9.6.1 → 9.8.1

Source: Gradle 9.8.1 release notes and the 9.x upgrade guide.

| Item | Relevance |
|------|-----------|
| 9.8.1 fixes three high-rated Gradle vulnerabilities: GHSA-mvvg-497x-hmj8 (worker-to-daemon deserialization), GHSA-xwqc-3h47-hg64 (client-daemon deserialization), GHSA-j5m7-59rp-24f5 (repository SSL failure can expose builds to malicious artifacts) | Good reason to keep 9.8.1. CI runners and developer machines should not stay on 9.6.1 |
| 9.8.0 changes the default JaCoCo `toolVersion` to 0.8.15 | The fork already pins `toolVersion = "0.8.15"` in `build.gradle.kts`. No effect; the explicit pin is now redundant but harmless |
| 9.6.0 deprecates all Kotlin DSL property delegates (`by creating`, `by registering`, `by getting`, `by extra`) with removal in Gradle 10 | The fork's script uses `val clickhouseDependencies by configurations.creating`, `val test by getting(JvmTestSuite::class)`, `val integrationTest by registering(JvmTestSuite::class)`, `val releaseDate by extra(...)`. Warnings only today (already present at the 9.6.1 wrapper). Not this milestone. Run once with `--warning-mode all` to record the list for a future cleanup |
| 9.6.0 deprecates implicit parent-project lookup and execution-time access to injected `Project` services | Single-project build, so no parent lookup. `generateVersionFile` reads `project.version` in `doFirst`; that is the pattern the guide says will error in Gradle 10, not 9.x. Not this milestone |
| 9.7.0 upgrades Kotlin to 2.4.0 (K1 removed), 9.8.0 to 2.4.10 | Build script compiled green in CI on 9.8.1 |
| `distributionSha256Sum` is absent from `gradle-wrapper.properties` | Optional hardening for the user to decide; not required |

No build-script change is needed for the wrapper bump.

## 4. protobuf-java 3.25.9 → 4.36.2 (open PR #10)

PR #10 changes one line, `libprotobuf = "3.25.9"` → `"4.36.2"`. That catalog entry feeds **two** things: `testFixturesApi(libs.protobuf.java)` and `protoc { artifact = "com.google.protobuf:protoc:${libs.versions.libprotobuf.get()}" }`. So the bump moves the runtime and the code generator together. Both artifacts exist on Central for 4.36.2 (protoc binaries for `osx-aarch_64`, `osx-x86_64`, `linux-x86_64`, `linux-aarch_64`).

### Findings

| Finding | Source | Confidence |
|---------|--------|------------|
| **PR #10's own CI run is red.** Run 39501799049 (head `011bc42b`, 2026-10-20 21:37 UTC): step "Run tests" `failure`, "Generate coverage summary" `failure`. Runs on `custom_build` at the same minute were green. The log needs authentication; the `gradle-test-reports` artifact (43 MB) was unexpired at query time. I did not read the failing test names | GitHub API (unauthenticated) | HIGH that it is red; cause UNKNOWN |
| 3.25.9 has no advisory; neither has 4.36.2 | OSV query 2026-10-21 | HIGH. No security reason to bump |
| Protobuf's cross-version rule: gencode from major V works with runtime V and V+1, so 3.x gencode runs on a 4.x runtime. 4.x gencode needs a runtime at least as new as its protoc (poison pill since 26.0). Here protoc and runtime move together, so gencode/runtime is consistent | protobuf.dev cross-version guarantee | HIGH |
| Confluent 7.9.1 is built on protobuf `3.25.5` (`common-parent:7.9.1` property `protobuf.version`). It also depends on `protobuf-java-util`, which the fork never declares. Gradle would raise `protobuf-java` to 4.36.2 but leave `protobuf-java-util` at 3.25.x from Confluent, a mixed-major pair. The cross-version page says nothing about `protobuf-java-util`; the 4.x util artifacts pin the same-version core | Confluent poms, protobuf.dev | MEDIUM. This is my leading suspect for the red run, unproven |
| Confluent only moved to protobuf 4.x with Platform 8.x (`8.0.0` → `4.29.3`, `8.3.2` → `4.34.0`, read from `common-parent`) | Confluent poms | HIGH |
| Plugin `com.google.protobuf` 0.10.0 requires Gradle 7.6+ / Java 11+; 0.9.6 fixed a Gradle 9.1 deprecation. Nothing found about protoc 4.x | plugin release notes | MEDIUM (notes are silent) |
| Blast radius is test-only. `protobuf-java` is not in `clickhouseDependencies`, so the shipped Confluent jar is unaffected. The testFixtures variant is published to `mavenLocal` for a JMH benchmark project, which would inherit the major | build file | HIGH |
| Tests that exercise it: `ProtobufIngestTest`, `ClickHouseSinkTaskWithSchemaTest`, `ColumnTest`, `SchemaTestData` (generated classes from `src/testFixtures/proto`) | grep | HIGH |

### Recommendation

Leave PR #10 open and unmerged in this milestone. Reasons: red CI, no CVE driver, and the only supported route to protobuf 4 is the Confluent 8.x line (which also aligns `kafka-clients` with the 4.x `connect-api`), a test-stack migration of its own. If the user wants it anyway, the planner should (a) download the failing run's `gradle-test-reports` artifact and name the failing tests before anything else, (b) try adding `testFixturesApi("com.google.protobuf:protobuf-java-util:${libs.versions.libprotobuf.get()}")` so the pair moves together, (c) accept that `ProtobufIngestTest` and `ClickHouseSinkTaskWithSchemaTest` are the pass criteria. Do not close the PR with "ignore this dependency" without the user's call.

## 5. Upstream untagged tail (`v1.7.0..upstream/main`, 12 commits)

Note: the local ref `refs/upstream/main` is ambiguous with `upstream/main` (git warns). Use `refs/remotes/upstream/main`. The planned deletion of the stale ref fixes this.

### The three commits

| Commit | Change (read with `git show`) |
|--------|-------------------------------|
| `30521f74` "Overriden version of guava to fix CVE" | Catalog: `guava = "33.7.2-jre"` and library `guava = { module = "com.google.guava:guava", version.ref = "guava" }`. Build: `implementation(libs.guava)` and `clickhouseDependencies(libs.guava) // temp to override client's with CVE`. Also +5 lines in `.gitignore` |
| `fc82b263` "Upgraded lz4-java version" | `lz4Java = "1.10.4"` → `"1.11.4"` |
| `bc896506` "upgraded httpclient5 version to 5.6.5" | `httpclient` → `5.6.5`; **removes** `httpclientTest` and the `httpclient5-test` library; the `integrationTest` suite now uses `libs.httpclient5`. The other 9 commits are the four dependabot bumps the fork already took in Phase 14 plus merges |

PR #859 (`a6f1df83`) states: httpclient5 `5.6.2`→`5.6.5`, lz4-java `1.10.4`→`1.11.4`, "Overridden: Guava `33.7.2-jre`".

### The guava CVE

The commit message does not name it. It is **CVE-2026-102554** (GHSA `GHSA-xxph-c9ww-hj94`), fixed in Guava `33.7.2`: the Java deserialization of `CompactHashMap`, `CompactHashSet` and `MapMakerInternalMap` trusts the element count in the stream and allocates up front, so a ~72-byte stream forces a ~4 GB allocation (denial of service via `OutOfMemoryError`). Affected: before 33.7.2. Severity is disputed (HeroDevs: medium; Snyk: high 8.2). Published 2026-10-16; the Guava 33.7.2 release notes link the GHSA. Sources: Guava GitHub releases page, HeroDevs advisory page, Snyk. Confidence MEDIUM (secondary sources; OSV and the GitHub advisory API had not indexed it at query time, and the `github.com/advisories` page 404ed for me).

Exposure in this connector: the vulnerable path needs `ObjectInputStream.readObject()` on attacker-controlled bytes with these Guava collections. The connector does no Java deserialization of record data, so practical exposure is very low. The value of the bump is a clean scanner result and a closed finding on the record. Say that in the rollout note rather than claiming an exploitable hole.

### Is guava already in the fork's catalog?

No. The fork's catalog has no `guava`. Guava reaches the shipped jar **transitively** today: `com.clickhouse:client-v2:0.9.6` declares `com.google.guava:guava:33.4.6-jre` at compile scope (read from the pom), so the Confluent jar currently ships 33.4.6-jre. Declaring it directly raises it to 33.7.2-jre on `runtimeClasspath` and in `clickhouseDependencies`.

Side effect to expect: client-v2's pom **excludes** guava's own dependencies (`failureaccess`, `listenablefuture`, `jspecify`, `error_prone_annotations`, `j2objc-annotations`). A direct, exclusion-free declaration brings them in: Guava 33.7.2's module metadata lists `failureaccess 1.0.3`, `listenablefuture 9999.0-empty-to-avoid-conflict-with-guava`, `jspecify 1.0.1`, `error_prone_annotations 2.50.0`, `j2objc-annotations 3.1`. The Confluent jar therefore also gains those small jars' classes. Harmless, but list them as the expected delta in the entry diff so a reviewer does not read them as an accident. Do not add exclusions or relocate guava; that would diverge from upstream for no benefit.

### Merge mechanics (dry-run with `git merge-tree`, no working-tree change)

Merge base is `379b8d01` (`v1.7.0`). Merging `refs/remotes/upstream/main` into `origin/custom_build`:

| File | Result |
|------|--------|
| `build.gradle.kts` | Clean. Guava lines and the `libs.httpclient5` swap apply without conflict |
| `.gitignore` | Clean |
| `gradle/libs.versions.toml` | **One conflict block** covering the whole top of `[versions]` (`kafka` … `lz4Java`, `guava`). The `[libraries]` hunk merges cleanly (drops `httpclient5-test`, adds `guava`) |

Resolution of the one block, entry by entry:

| Key | Take | Why |
|-----|------|-----|
| `kafka` | fork `4.4.0` (keep the `FORK OVERRIDE` comment) | upstream `2.7.0` is not the fork's pin |
| `gson` `2.14.0`, `jackson` `2.22.4`, `lombok` `1.18.48` | fork | upstream still has `2.13.1`, `2.22.3`, `1.18.46` |
| `httpclient` `5.6.5` | either (identical) | |
| `httpclientTest` | **delete** | Upstream removed it. The `[libraries]` hunk already removed `httpclient5-test` and `build.gradle.kts` already uses `libs.httpclient5`, so a surviving `httpclientTest` is an orphan key that Dependabot would keep bumping |
| `lz4Java` | **fork `1.12.0`** | see below |
| `guava` | **add `guava = "33.7.2-jre"`** | If the resolver keeps "ours" for the whole block it drops this line and the build fails with an unknown `libs.guava` accessor; this is the trap in the conflict |

### lz4-java: no collision, keep `1.12.0`

- Upstream's `1.11.4` vs the fork's `1.12.0` is only a textual conflict on one line. The fork's value is higher and advisory-free: OSV lists `GHSA-343h-94h5-c4wr`, `GHSA-gm45-99xc-r7wv`, `GHSA-mcr4-qmvw-px4g` on `at.yawk.lz4:lz4-java 1.11.3` (and more on 1.10.2 and `org.lz4 1.8.0`), none on 1.11.4 or 1.12.0. Both 1.11.4 and 1.12.0 exist on Central (latest is 1.12.0). This matches FORK-DELTA, which already says 1.11.4 is the advisory-free floor.
- The capability rule in `build.gradle.kts` (`withCapability("org.lz4:lz4-java")` choosing the candidate whose group is `at.yawk.lz4`) is group-based and version-agnostic, so no version number can collide with it. Version selection among `at.yawk.lz4` versions is Gradle's normal highest-wins, and the direct `implementation(libs.lz4.java)` pins 1.12.0 above the client's `1.10.2` and Kafka 4.4's `1.11.2`.
- Anchor `withCapability("org.lz4:lz4-java")` expected count stays 1.

### httpclient5 5.6.5 closes a real advisory

OSV: `GHSA-hjcp-jmpx-g3qm` (CVE-2026-64607, connection leak on a bad `Content-Encoding` leading to pool exhaustion, classic client, CVSS A:L) affects httpclient5 up to and including `5.6.2`, fixed in `5.6.3`. The published `v1.7.0_gg3` shipped `5.6.2`. `5.6.5` is clean, and it brings `httpcore5`/`httpcore5-h2` `5.4.4` (clean; `5.3.4` from client-v2's pom is affected by `GHSA-hf6x-8p5f-cgmf` / `GHSA-v3jc-474w-2wm6`, fixed in 5.4.3, and Gradle picks the higher). Keep the explicit `implementation(libs.httpclient5)` pin; dropping it would let client-v2 pull `5.4.4` (affected). Put this in the rollout note next to the guava line.

## 6. Other current CVEs on the resulting set

OSV batch query on 2026-10-21 (Maven ecosystem). "None" means OSV returned no vulnerability for that exact version.

| Component | Version | Ships in Confluent jar? | OSV |
|-----------|---------|-------------------------|-----|
| gson | 2.14.0 | yes | none |
| httpclient5 | 5.6.5 | yes | none (5.6.2 and client-v2's 5.4.4 are affected) |
| httpcore5 / httpcore5-h2 | 5.4.4 | yes (transitive) | none |
| jackson-core / -databind | 2.22.4 | yes | none |
| lz4-java (`at.yawk.lz4`) | 1.12.0 | yes | none |
| guava | 33.7.2-jre | yes (after tail) | none; 33.4.6-jre also "none" in OSV but is affected by CVE-2026-102554 per Guava's own notes. OSV lags here |
| commons-compress 1.28.0, commons-codec 1.19.0, commons-io 2.20.0, RoaringBitmap 1.0.6 | transitive from client-v2 | yes | none |
| commons-lang3 | 3.21.0 | **no** (only `testFixturesImplementation`). The jar still carries client-v2's transitive `3.20.0` | none for 3.20.0 or 3.21.0 |
| kafka-clients / connect-api / connect-json | 4.4.0 | no (worker-provided) | none |
| protobuf-java | 3.25.9 and 4.36.2 | no (testFixtures) | none for both |
| `kafka-clients` `7.9.1-ccs` → `org.lz4:lz4-java 1.8.0` | test classpath | no | 8 advisories, neutralised on `testRuntimeClasspath` by the fork's exclude |

Two honest limits. The "ships" column comes from the build file and published poms, not from unpacking a built jar. And OSV had not indexed the Guava advisory, so a clean OSV result is not proof of absence for a days-old CVE; re-query before the release note is written.

## Recommended Stack (the deltas this milestone applies)

### Core versions after absorption

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Kafka `connect-api` | 4.4.0 (fork override, `api(...)`) | Compile-time Connect API | Public API the fork uses is identical to 4.1.1; Java 17 bytecode matches the toolchain |
| Shadow `com.gradleup.shadow` | 9.6.1 | Builds `confluentJar` | Needs the `configurations.add(...)` fix in section 2 |
| Gradle wrapper | 9.8.1 | Build | Includes three Gradle security fixes |
| ClickHouse client | 0.9.6 (unchanged) | Driver | Out of scope |
| guava | 33.7.2-jre (new catalog entry) | Closes CVE-2026-102554 in the shipped jar | Upstream's choice; keeps the fork's delta zero for this line |
| lz4-java `at.yawk.lz4` | 1.12.0 (fork pin kept) | LZ4 | Ahead of upstream's 1.11.4, advisory-free |
| httpclient5 | 5.6.5 | HTTP | Closes CVE-2026-64607; keep the explicit pin |

### Installation (build-file deltas, not shell commands)

```toml
# gradle/libs.versions.toml, [versions]: add
guava = "33.7.2-jre"
# remove the orphan after the upstream merge
# httpclientTest = "5.6.5"
# [libraries]: add
guava = { module = "com.google.guava:guava", version.ref = "guava" }
```

```kotlin
// build.gradle.kts, dependencies { }: add (as upstream does)
implementation(libs.guava)
clickhouseDependencies(libs.guava) // temp to override client's with CVE
// build.gradle.kts, confluentJar: replace the from(...) line, see section 2
```

## Alternatives Considered

| Recommended | Alternative | Why not |
|-------------|-------------|---------|
| `configurations.add(clickhouseDependencies)` in `confluentJar` | Keep `from(clickhouseDependencies)` | Produces a jar of nested jars under Shadow 9 |
| `configurations.add(...)` | `from(provider { …map { zipTree(it) } })` | Reproduces 8.x exactly but reaches around Shadow's model; keep as fallback only |
| Keep `kafka = 4.4.0` | Roll back #9 | No API reason to; pin only affects compile and tests. Rolling back would also undo the Kafka-side move to `at.yawk.lz4` |
| Leave PR #10 open | Merge it, or ignore the dependency | Red CI, no security driver, and Confluent 7.9.1 is a 3.25.5 stack |
| Adopt upstream's removal of `httpclientTest` | Keep the fork's two-key setup | A second key that must be bumped in lockstep, for nothing |
| Declare guava directly like upstream | Gradle `constraints { }` or a resolution-strategy force | Diverges from upstream; the direct declaration also puts guava into `clickhouseDependencies`, which a constraint would not |

## What NOT to Add

| Avoid | Why | Instead |
|-------|-----|---------|
| Bumping the ClickHouse client to 0.10.0 | Out of scope; PR #4 stays open | Leave it |
| `kafka-clients`, `connect-json` or `connect-transforms` pins | Not used by `src/main`; worker provides them | Nothing |
| Guava relocation or exclusions | Shadow relocation changes class names; exclusions diverge from upstream | Take upstream's two lines |
| `mergeServiceFiles()` / `minimize` / `relocate` on `confluentJar` | Not used before; would change jar content beyond this milestone's scope | Keep behaviour identical, prove it with the entry diff |
| `Multi-Release: true` in the Confluent manifest | Activates versioned classes in production | `addMultiReleaseAttribute.set(false)` |
| `protobuf-java` 4.x with Confluent 7.9.1 | Mixed-major stack, red CI | Stay on 3.25.9 until a Confluent 8.x test-stack move |
| A `shadowJar`-based publish path | `shadowJar` is the plugin's default task, not the shipped artifact | Keep `confluentJar` |

## Version Compatibility

| Package A | Compatible with | Notes |
|-----------|-----------------|-------|
| Shadow 9.6.1 | Gradle 9.8.1 | Shadow needs Gradle ≥ 9.2.0 and Java ≥ 17; CI green on 9.8.1 |
| connect-api 4.4.0 | JDK 17 toolchain | Class major 61 |
| connect-api 4.4.0 | `kafka-clients` 4.4.0 declared; test classpaths likely `7.9.1-ccs` | Confirm with `dependencyInsight`; not new |
| kafka-clients 4.4.0 | `at.yawk.lz4:lz4-java 1.11.2` → 1.12.0 | Capability conflict no longer arises on runtime classpaths |
| guava 33.7.2-jre | client-v2 0.9.6 (declares 33.4.6-jre) | Direct dependency wins; guava adds its five small dependencies |
| httpclient5 5.6.5 | httpcore5 5.4.4 | From `httpclient5-parent` 5.6.5 |
| protobuf-java 4.36.2 | protoc 4.36.2 (same catalog key) | Consistent pair; Confluent 7.9.1 `protobuf-java-util` 3.25.x is the mismatch |

## Sources

- Shadow changelog and `docs/changes/README.md` (9.0.0 breaking changes, 9.x entries through 9.6.1); `ShadowJar.kt`, `ShadowJavaPlugin.kt`, `ShadowCopyAction.kt` at tag 9.6.1 on GitHub (HIGH); Shadow 8.3.5 `ShadowCopyAction.groovy` from the local Gradle cache (HIGH).
- Maven Central: `connect-api` 4.1.1 and 4.4.0 jars and poms, `kafka-clients` 4.1.1 and 4.4.0 jars and poms, `guava` 33.7.2-jre pom and module metadata, `client-v2`/`clickhouse-*` 0.9.6 poms, `httpclient5` 5.6.5 pom, `protobuf-java`/`protoc`/`protobuf-java-util` metadata, `at.yawk.lz4` metadata (HIGH).
- Apache Kafka 4.2/4.3 upgrade notes, 4.2/4.3/4.4 release notes, KIP-1265 page, Kafka source at tags 4.1.1, 4.2.0, 4.3.0 and branch 4.4 (HIGH; the 4.4 upgrade page was not yet published).
- Gradle 9.8.1 release notes and 9.x upgrade guide (HIGH).
- OSV API (`api.osv.dev`) batch queries, 2026-10-21 (HIGH for what it returns; lags on very new advisories).
- Guava GitHub releases, HeroDevs `cve-2026-102554` page, Snyk, camunda and graphql-java threads (MEDIUM, secondary).
- protobuf.dev cross-version runtime guarantee; protobuf-gradle-plugin release notes (HIGH/MEDIUM).
- Confluent `common-parent`, `kafka-schema-registry-parent`, `kafka-schema-registry-client` poms for 7.9.1, 8.0.0, 8.3.2 (HIGH).
- Upstream and fork git objects: `git show 30521f74 fc82b263 bc896506 a6f1df83`, `git merge-tree` dry run, `git diff sync/v1.7.0...origin/custom_build`; GitHub API for PR #10, PR #859 and workflow runs (HIGH).
- Repo: `.planning/milestones/v1.7.0_gg3-hardening-phases/24-release-publication/24-COMMAND-EVIDENCE.md` E-04 (entry-comparison precedent), `docs/FORK-DELTA.md`, `.planning/scripts/fork-anchors.txt`.
