# Technology Stack

**Project:** ClickHouse Kafka Connect `custom_build` fork - milestone `v1.7.0_gg1-sync`
**Researched:** 2026-10-07
**Scope:** Delta only. The fork's v1.6.0 stack (Gradle 9.6.1, version catalog, Testcontainers 2.0.5, JUnit 5.14.4, JaCoCo, `testUnit`, JDK 17 pin) is validated and not re-researched.
**Overall confidence:** HIGH for build mechanics (verified by simulating the merge in a throwaway clone and resolving the real dependency graph). MEDIUM for upstream release-note claims (read through a page summariser, not the raw files).

## Method

- Read-only git on the real repo (`git diff`, `git show`, `git grep`, `git log`, `git for-each-ref`).
- A throwaway `git clone --local` under `/tmp/sim-parent/sim` (since deleted) to run `git merge v1.7.0` and real Gradle resolution. The real repo was never checked out, merged or edited.
- Gradle 9.6.1 `dependencyInsight` / `dependencies` / `confluentJar` / `compileIntegrationTestJava` were run in that clone only.

## Summary

The build-file delta of `v1.7.0` is small and merges almost cleanly. Five things matter:

1. `v1.7.0` changes the build in exactly three ways: `clickHouseDriver` 0.9.5 -> 0.9.6 (already the fork's value), a new `lz4Java = "1.10.4"` catalog entry used as `implementation` and `clickhouseDependencies`, and a group-based `org.lz4:lz4-java` capability rule. It also drops `clickhouse-jdbc` from the catalog and from the `integrationTest` suite.
2. **The auto-merge keeps both capability rules and raises no conflict.** Upstream's rule lands near line 40 and the fork's `select("at.yawk.lz4:lz4-java:1.10.2")` block stays near line 136. Resolution still works, but the fork block is now dead weight (see Part 1).
3. `clickhouse-jdbc` is not used anywhere in the fork. Removing it is safe (Part 2).
4. All four dependabot branches are one-line catalog edits. Jackson is runtime, shipped in the Confluent jar. The other three are test-only. None is breaking. All resolved and compiled in the simulation (Part 3).
5. lz4-java 1.10.2 -> 1.10.4 has no functional or security fix. 1.10.4 is itself behind several moderate advisories fixed in 1.11.x and 1.12.0 (Part 4).

## Part 1 - Build file delta

### 1a. `git diff v1.6.0 v1.7.0 -- build.gradle.kts gradle/` (upstream's whole change)

| File | Change | Effect |
|------|--------|--------|
| `gradle/libs.versions.toml` | `clickHouseDriver` `0.9.5` -> `0.9.6` | Fork already runs `0.9.6` as a `# FORK OVERRIDE`. This becomes upstream's own value. |
| `gradle/libs.versions.toml` | adds `lz4Java = "1.10.4"` and `lz4-java = { module = "at.yawk.lz4:lz4-java", ... }` | New direct dependency. |
| `gradle/libs.versions.toml` | removes `clickhouse-jdbc = { ... }` | Part 2. |
| `build.gradle.kts` | `import org.gradle.api.artifacts.component.ModuleComponentIdentifier` | Needed by the new rule. |
| `build.gradle.kts` | new `configurations.all { resolutionStrategy.capabilitiesResolution.withCapability("org.lz4:lz4-java") { select the candidate whose group is "at.yawk.lz4" } }` | Group-based and version-agnostic. |
| `build.gradle.kts` | `implementation(libs.lz4.java)` and `clickhouseDependencies(libs.lz4.java)` | Direct dependency on 1.10.4 for the runtime classpath and the Confluent jar. |
| `build.gradle.kts` | removes `implementation(libs.clickhouse.jdbc)` from the `integrationTest` suite | Part 2. |

Upstream also bumped `VERSION` to `v1.7.0` and `CHANGELOG.md`. No shadow relocation was added. Upstream's CHANGELOG line "includes shaded `org.lz4`" really means *bundled in the Confluent archive*. I confirmed the jar holds `net/jpountz/lz4/*` (105 entries) unrelocated, with `META-INF/maven/at.yawk.lz4/lz4-java` at version 1.10.4.

### 1b. `git diff v1.6.0 HEAD -- build.gradle.kts gradle/` (the fork's delta, the thing to preserve)

Catalog: `clickHouseDriver = "0.9.6"` and `kafka = "4.1.1"`, both marked `# FORK OVERRIDE`.

`build.gradle.kts`:
- JDK 17 toolchain, `defaultJdkVersion = 17`, no `options.release.set(11)`.
- `jacoco` plugin, `jacocoTestReport`, `jacocoForkReport` (15 classes), `jacoco { toolVersion = "0.8.15" }`.
- `testUnit` task, the `check` guard, `file.encoding=windows-1252`, the test `javaLauncher`, and the summary `TestListener`.
- ben-manes `versions` plugin plus the `dependencyUpdates` stable-only filter.
- `implementation("org.slf4j:slf4j-reload4j:2.0.17")`.
- Spotless `importOrder()` + `removeUnusedImports()` and the `removeUnusedImports` task.
- The "Temporary fix" capability block: `select("at.yawk.lz4:lz4-java:1.10.2")`.

### 1c. Simulated merge result (`git merge --no-commit v1.7.0` in the clone)

| Path | Result | Action needed |
|------|--------|---------------|
| `build.gradle.kts` | **Auto-merged, no conflict.** Both capability rules present. `implementation(libs.lz4.java)` and `clickhouseDependencies(libs.lz4.java)` added. `clickhouse.jdbc` line gone. All fork additions intact. | Remove the fork's duplicate capability block (below). |
| `gradle/libs.versions.toml` | **Conflict** in the header lines only (`clickHouseDriver`, `kafka`). `lz4Java` added and `clickhouse-jdbc` removed cleanly. | Keep fork's `kafka = "4.1.1"  # FORK OVERRIDE`. For the driver line, take `clickHouseDriver = "0.9.6"` and **drop the `# FORK OVERRIDE (upstream 0.9.5)` comment**, since it is now false. |
| `VERSION` | Conflict (`v1.6.0_gg1` vs `v1.7.0`) | Resolve to `v1.7.0_gg1`. |
| `ClickHouseSinkConfig`, `ClickHouseWriter`, `ClickHouseHelperClient` | Conflicts (source, outside this file's scope) | Phase work. See the D-17 note in Part 5. |

### LZ4 capability resolution: how the fork's rule and upstream's interact

I resolved `runtimeClasspath` three ways in the clone. Every variant resolved `at.yawk.lz4:lz4-java:1.10.4`, and `org.lz4:lz4-java:1.8.0` from `kafka-clients:4.1.1` was replaced by it.

| Variant | Result |
|---------|--------|
| Both rules (what git produces) | OK. Reason shown: "On capability org.lz4:lz4-java client-v2 uses the maintained at.yawk fork" (upstream's `because`). Upstream's rule fires first because it appears earlier in the file. |
| Fork rule only, with the catalog at 1.10.4 | OK. Gradle's `select("g:a:1.10.2")` matched the candidate module and the final version still came from conflict resolution (1.10.4). |
| Upstream rule only | OK. |

The risk is not breakage. It is that two rules for one capability exist, the fork's says "1.10.2" in its notation, and the next reader will believe 1.10.2 is pinned. It isn't.

**Recommendation: delete the fork's "Temporary fix" `configurations.configureEach { ... select("at.yawk.lz4:lz4-java:1.10.2") }` block and keep upstream's.**
- Upstream's rule is version-agnostic (matches on group) and has an accurate comment.
- The fork's reason for its own rule (the Kafka `4.1.1` override pulling `org.lz4` 1.8.0) is covered by upstream's rule, as the three runs show.
- This is one fewer fork build addition. FORK-DELTA's "Six fork build additions" row shrinks to five, and the `1.10.2` wording in it, in `.planning/MERGE-CHECKLIST.md` (lines 14, 74) and in `.claude/CLAUDE.md` / `.planning/codebase/STACK.md` becomes wrong.
- Keep the Kafka `4.1.1` coupling note. The capability rule still has to exist because `kafka-clients:4.1.1` still depends on `org.lz4:lz4-java:1.8.0`.

How lz4 reaches the Confluent jar: `confluentJar` is `from(clickhouseDependencies, main.output)`. Before `v1.7.0`, `client-v2:0.9.6` already carried `at.yawk.lz4:lz4-java:1.10.2` into that configuration (confirmed by `dependencyInsight` with the explicit dependency removed). `v1.7.0` changes the jar's lz4 from **1.10.2 to 1.10.4** and makes it explicit. The same pin also goes into the runtime classpath, which is where the Kafka/`org.lz4` conflict arises.

### Build-time quirk found during the simulation

`generateVersionFile` declares outputs only (no inputs), so Gradle treats it as up to date when `build/generated/.../Version.java` exists. After a `VERSION` bump in a dirty tree, `Version.ARTIFACT_VERSION` can stay stale. I saw it keep merge-conflict text. That feeds the JMX `version=` segment and the client name. CI builds are clean and unaffected. Locally run `./gradlew clean` (or delete `build/generated`) after the `VERSION=v1.7.0_gg1` edit before trusting any test that reads the version.

## Part 2 - Does the fork use `clickhouse-jdbc`?

**No. Removing the catalog entry is safe.**

- `git grep` at HEAD for `clickhouse-jdbc`, `libs.clickhouse.jdbc`, `com.clickhouse.jdbc`, `jdbc:`, `DriverManager`, `ClickHouseDriver` across `src`, `build.gradle.kts`, `.github`: the only live reference is `integrationTest { implementation(libs.clickhouse.jdbc) }` at `build.gradle.kts:161`, plus the catalog line. The other hits are a `docs/DESIGN.md` link, a code comment (`ClickHouseWriter.java:752`, "how NESTED works in JDBC") and `java.sql.Date` / `java.sql.Timestamp` (JDK classes, unrelated).
- Classpath diff, HEAD vs merged build, `integrationTest` compile and runtime classpaths: only `com.clickhouse:clickhouse-jdbc`, `com.clickhouse:jdbc-v2` and `org.antlr:antlr4-runtime` disappear. Nothing is added.
- `compileTestFixturesJava` and `compileIntegrationTestJava` succeed in the merged clone with jdbc removed and all four bumps applied.
- Note: the integration suite is `workflow_dispatch` only in CI and the main `test` task does not compile it. Add `compileIntegrationTestJava` to the verification commands, otherwise a break here would be invisible.

## Part 3 - Approved dependency bumps

All four dependabot branches (`refs/remotes/upstream/dependabot/gradle/*`) are based on `379b8d01` (= `v1.7.0`) and contain **exactly one line** in `gradle/libs.versions.toml`. No source changes.

| Bump | Catalog line | Scope | Where it lands |
|------|--------------|-------|----------------|
| jackson `2.22.2` -> `2.22.3` | `jackson = "2.22.3"` | **Runtime** | `jackson-core` + `jackson-databind` in `implementation` and `clickhouseDependencies`, so it ships in the Confluent jar. Used in `ConvertToMapOrJson`, `ClickHouseHelperClient`, `ClickHouseFieldDescriptor`, `DataJson`. |
| okhttp `5.4.0` -> `5.5.0` | `okhttp = "5.5.0"` | **Test only** (`integrationTest` suite) | Single importer: `src/integrationTest/.../helper/ConfluentPlatform.java`. |
| org.json `20260522` -> `20260814` | `orgJson = "20260814"` | **Test only** (`testFixturesImplementation` and `integrationTest`) | About 10 test/fixture files import it. |
| mockito `5.23.0` -> `5.24.0` | `mockito = "5.24.0"` | **Test only** | Only `mockito-junit-jupiter` (pulls `mockito-core`). |

Out of scope and untouched: `clickHouseDriver-0.10.0` branch.

Resolution check (clone, `dependencies` before vs after): Jackson's BOM aligns every `jackson-*` module (including the older Confluent/schema-registry transitives pinned at 2.12-2.16) to 2.22.3 in `testRuntimeClasspath`, `integrationTestRuntimeClasspath` and `clickhouseDependencies`. `jackson-annotations` stays `2.22` (annotations ship only as `2.22`). okhttp 5.5.0 raises transitive `okio` 3.17.0 -> 3.18.1 on the integration-test classpath. No other version moves. JDK 17 is fine for all four (Mockito 5.x needs 11+, OkHttp 5.x and org.json run on 8+/17).

### Per-bump notes

**Jackson 2.22.3 (released 2026-09-21)** - patch release. Per the Jackson wiki release notes: hardening in `jackson-core` (`NumberInput.looksLikeValidNumber`, `UTF8DataInputJsonParser` honouring `maxErrorTokenLength`), a `WriterBasedJsonGenerator` AIOOBE fix, `UTF32Reader` surrogate-pair fix, databind bounds on `TypeDeserializer` type-id cache growth, quadratic forward-reference handling in Collection/Map/ObjectArray deserializers, and bracketed IPv6 `InetSocketAddress` serialization. The wiki lists several CVE IDs for these. I am treating them as informational because the IDs came through a summariser and I did not cross-check them against NVD. Risk: low. The only behaviour change someone could notice is the IPv6 serialization format, which the connector does not use.
- *Why take it:* it is the one bump that changes shipped bits, and it is a security-hardening patch.
- *Verification:* the existing `ConvertToMapOrJson*` tests and the full `test` run cover the fork's Jackson use. No new tests needed.

**OkHttp 5.5.0 (released 2026-08-16)** - minor release. New: opt-in Encrypted Client Hello, a redesigned DNS API, `DnsOverHttps`/`AndroidDns`, artifact signing key change. Fixes: hostname verifier rejects hosts failing IP canonicalisation, multipart part closure, HTTP/1 request-body timeouts, QUERY redirects, proxy-selector fallback. Dependency: Okio 3.18.1. No source-breaking change documented for the blocking `OkHttpClient` API that `ConfluentPlatform` uses.
- *Verification:* `compileIntegrationTestJava` passed in the clone. Runtime behaviour is only exercised by the Confluent-platform integration tests, which CI does not run.

**org.json 20260814 (released 2026-08-14)** - this jump spans two releases: 20260719 and 20260814. 20260814 has one change (`XML.unescape` for valid whitespace characters, PR #1070; the fork does not use org.json's XML). 20260719 added a fix for CVE-2026-59171 (bounds checking on BigDecimal -> BigInteger expansion in `objectToBigInteger`), a **new configurable max length for BigInteger/BigDecimal numbers**, restored lenient consecutive commas in `JSONArray`, and CDL changes.
- *Residual risk (LOW confidence, could not read the PR):* I could not determine the default for the new number-length limit. If a test builds or parses a very large integer/decimal through org.json (for example `Int256`/`UInt256`/`Decimal256` fixtures), it could behave differently. This is test-only, and the first full `test` run is the check.
- *Verification:* any failure shows up as a test failure in `ClickHouseTestHelpers` / `SchemaTestData` consumers. Bisect by reverting just `orgJson`.

**Mockito 5.24.0 (released 2026-09-23)** - minor release. Added the `SpyStatic` API, a `@Spy` mock-maker option, a data-race fix in `InvocationContainerImpl.invocationForStubbing`, a fix for non-deterministic `AssertionError` with the SUBCLASS mock maker, and `clearInlineMocks` restoring method parameters. No breaking change or ByteBuddy/Objenesis bump stated. Only one test file imports Mockito, so exposure is minimal.

### How to apply

Edit the four version strings in the merged `libs.versions.toml` as a single commit (`jackson`, `okhttp`, `orgJson`, `mockito`), after the sync merge commit. Do not `cherry-pick` the dependabot commits. They were authored against upstream's catalog (`kafka = "2.7.0"`), whose context lines will not apply cleanly against the fork's `kafka = "4.1.1"  # FORK OVERRIDE` line. Do not touch `jacksonAnnotations`.

## Part 4 - lz4-java 1.10.4 vs 1.10.2

| Version | Notable content (GitHub release notes via summariser) |
|---------|-------------------------------------------------------|
| 1.10.1 | Fix for CVE-2025-66566 (info leak from the Java decompressors' uninitialised output buffer). Already in 1.10.2. |
| 1.10.2 | Reproducible build, OSGi manifest headers fixed, Windows JNI library renamed. |
| 1.10.3 | Docs; NPE fix in `LZ4FrameInputStream#available`. |
| 1.10.4 | Linux native build flags tuned and xxhash compilation fixed (performance regression fix). |

- **No functional or security difference that matters for this connector.** No API or package change (`net.jpountz.*` is the same), no new JDK requirement. The clone's `confluentJar`, runtime resolution and compile all work on 1.10.4.
- The practical value of this bump is alignment with upstream, so the next sync produces no `lz4Java` diff. The native-flags fix concerns the JNI path, which the connector's client uses only via `LZ4Factory.fastestInstance()` when the native library loads.
- **Flag for the user (AGENTS.md: notify, do not upgrade unasked):** `at.yawk.lz4:lz4-java` has published moderate and low advisories fixed after 1.10.4. Maven Central lists 1.11.0 to 1.11.4 and 1.12.0 (latest, 2026-09-25). They cover `LZ4BlockInputStream` unvalidated compressed length (fixed in 1.11.2), native XXHash JVM crash on bad ranges (fixed in 1.11.1), `LZ4DecompressorWithLength`, `LZ4FrameInputStream` buffer reallocation, `LZ4BlockInputStream` recursion, and a native-library extraction temp-dir race (late September advisories). Affected-version ranges for the later ones were not readable, so assume 1.10.4 is affected. The ClickHouse client ships its own `ClickHouseLZ4InputStream` and calls the `LZ4Factory` decompressors, so the vulnerable `LZ4BlockInputStream` classes are probably not on its path. I did not verify reachability, so confidence is LOW. Taking 1.10.4 as approved is fine. A one-line follow-up to raise `lz4Java` to at least 1.11.2 (or 1.12.0) would need the user's explicit approval and a ClickHouse-client compatibility check on `0.9.6`.

## Part 5 - What NOT to change

| Do not | Reason |
|--------|--------|
| Adopt client `0.10.0` (dependabot branch) | Explicitly out of scope. Also the client is the thing that decides whether ZSTD decoding works, so it would change the D-17 story. |
| Revert `kafka = "4.1.1"  # FORK OVERRIDE` to upstream's `2.7.0` | The fork needs Connect API 4.1.1. `onPartitionsRevoked` has no `@Override` for that reason. The `org.lz4` capability rule must stay for the same reason (`kafka-clients:4.1.1` -> `org.lz4:lz4-java:1.8.0`). |
| Keep the stale `# FORK OVERRIDE (upstream 0.9.5)` comment | After the merge, `0.9.6` is upstream's own value. Update the comment or the FORK-DELTA "Fork dependency pins" row will lie. |
| Keep both capability rules | Redundant (Part 1). Keep upstream's. |
| Pin anything to `1.10.2` | The merged build must show 1.10.4 on `runtimeClasspath` and in the Confluent jar's `pom.properties`. |
| Re-add `clickhouse-jdbc` | No code needs it (Part 2). |
| Touch the fork's JaCoCo, `testUnit`, JDK 17 `javaLauncher`, `windows-1252`, Spotless, `dependencyUpdates`, `slf4j-reload4j:2.0.17` or the `forkChangedClasses` list | All auto-merged intact. They are the fork's reason for existing. If `jacocoForkReport` classes change names in the sync, update the list. |
| Bump Gradle, Shadow, Testcontainers, JUnit, JaCoCo, Spotless, protobuf or any other dependency | Outside the approved list. Dependabot may have more open branches, so check `refs/remotes/upstream/dependabot/gradle/*` and ignore everything outside the four. |
| Add a version-catalog entry for `slf4j-reload4j` | Intentionally a literal (BLD-08). |
| Add a shadow `relocate` for `net.jpountz` | Upstream did not, and a relocation would be a behaviour change in the Confluent archive. |

### D-17 retirement - what it means for the stack (for the roadmap)

Upstream's flag is broader than the fork's pin. When `setCompressionMethod=true` (default) it sends `network_compression_method=lz4` on V1 (`CUSTOM_SETTINGS`) and V2 (`serverSetting`), like D-17. **It also sets `compress=false` on V1 (`ClickHouseClientOption.COMPRESS`) and `compressServerResponse(false)` on V2, in both modes (not gated on the flag).** So server responses stop being compressed at all. This is why upstream's approach avoids the ZSTD failure independently of the pinned client. Consequences:
- The fork's pin is a subset of upstream's behaviour. Retiring it needs no extra stack change, only the source conflict resolution in `ClickHouseHelperClient` (constant names differ: the fork has package-private `NETWORK_COMPRESSION_METHOD_SETTING` / `NETWORK_COMPRESSION_METHOD_LZ4`, upstream has public `NETWORK_COMPRESSION_METHOD` / `LZ4`).
- `NetworkCompressionPinUnitTest` reads `getDefaultClientOptions().get(CUSTOM_SETTINGS)`. That should still hold with the flag on, but the tests need adapting to the flag and to the new `COMPRESS=false` option. Add a flag-off assertion.
- Response size grows (uncompressed DESCRIBE/SHOW/insert-response bodies). These are small, but note it in the operator rollout checklist.
- The `FORK-DELTA.md` D-17 row and its "For the next syncer" paragraph (the "eighth behaviour") must be rewritten. The removal condition ("once the client decodes ZSTD") no longer applies.

## Recommended stack after the milestone

| Item | Version | Source | Note |
|------|---------|--------|------|
| ClickHouse client (`clickhouse-client`, `-http-client`, `-data`, `client-v2`) | 0.9.6 | upstream `v1.7.0` | Drop the FORK OVERRIDE comment. |
| Kafka Connect API | 4.1.1 | fork override | Kept. |
| `at.yawk.lz4:lz4-java` | 1.10.4 | upstream `v1.7.0` | Replaces 1.10.2. Single capability rule (upstream's). |
| Jackson (`core`, `databind`) | 2.22.3 | approved bump | Runtime, shipped. `jackson-annotations` stays 2.22. |
| OkHttp | 5.5.0 | approved bump | `integrationTest` only. |
| org.json | 20260814 | approved bump | Tests/fixtures only. |
| mockito-junit-jupiter | 5.24.0 | approved bump | Tests only. |
| `clickhouse-jdbc` | removed | upstream `v1.7.0` | Unused. |
| Gradle, JUnit, Testcontainers, JaCoCo, Spotless, Shadow, protobuf | unchanged | | Out of scope. |

## Verification commands for the roadmap

```bash
./gradlew clean dependencyInsight --dependency lz4-java --configuration runtimeClasspath    # expect at.yawk.lz4:lz4-java:1.10.4, no org.lz4 left
./gradlew dependencyInsight --dependency lz4-java --configuration clickhouseDependencies    # expect 1.10.4 (was 1.10.2 via client-v2)
./gradlew compileTestFixturesJava compileIntegrationTestJava                                # catches a jdbc/okhttp/org.json break the main test task misses
./gradlew confluentJar && unzip -p build/libs/*-confluent.jar META-INF/maven/at.yawk.lz4/lz4-java/pom.properties   # version=1.10.4
./gradlew test testUnit                                                                     # the acceptance gate; watch org.json number-handling tests
grep -n "capabilitiesResolution" build.gradle.kts                                           # expect exactly one
```

## Confidence

| Claim | Confidence | Basis |
|-------|------------|-------|
| Build diffs, auto-merge behaviour, double capability rule, jdbc removal safe, resolved versions, Confluent jar contents, `generateVersionFile` staleness | HIGH | Reproduced in a throwaway clone with real Gradle runs and `git grep`. |
| Dependabot branches are single-line and scoped as stated | HIGH | `git diff` on each ref. |
| Release-note content for Jackson, OkHttp, org.json, Mockito, lz4-java | MEDIUM | GitHub/Jackson wiki pages through a summariser. CVE IDs not cross-checked against NVD. |
| org.json 20260719 number-length limit default, lz4 advisory reachability from the client | LOW | Details unreadable. Covered by the test run and flagged to the user. |

## Sources

- Local: `git diff v1.6.0 v1.7.0 -- build.gradle.kts gradle/`, `git diff v1.6.0 HEAD -- build.gradle.kts gradle/`, `git log v1.6.0..v1.7.0`, `v1.7.0:CHANGELOG.md`, `git diff` for each `refs/remotes/upstream/dependabot/gradle/*`, and Gradle 9.6.1 output from the simulation clone (MEDIUM-HIGH).
- [Jackson 2.22.3 release notes](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.22.3) (MEDIUM)
- [OkHttp CHANGELOG.md](https://raw.githubusercontent.com/square/okhttp/master/CHANGELOG.md) (MEDIUM)
- [JSON-java releases 20260719](https://github.com/stleary/JSON-java/releases/tag/20260719) and [20260814](https://github.com/stleary/JSON-java/releases/tag/20260814) (MEDIUM / LOW)
- [Mockito v5.24.0](https://github.com/mockito/mockito/releases/tag/v5.24.0) (MEDIUM)
- [yawkat/lz4-java releases](https://github.com/yawkat/lz4-java/releases), [security advisories](https://github.com/yawkat/lz4-java/security/advisories), [GHSA-4v53-57pg-c464](https://github.com/yawkat/lz4-java/security/advisories/GHSA-4v53-57pg-c464), [GHSA-xx22-p4ch-683r](https://github.com/yawkat/lz4-java/security/advisories/GHSA-xx22-p4ch-683r), [Maven Central metadata](https://repo1.maven.org/maven2/at/yawk/lz4/lz4-java/maven-metadata.xml) (MEDIUM)
