# Phase 45: Dependency and CVE check (DEP-01, DEP-02)

**Query date (UTC):** 2026-11-04
**Tree:** branch `sync/v1.7.0`, after the upstream tail merge M (`12e2cf17`) and the plan 45-01..45-05 work; the version catalog is `gradle/libs.versions.toml` at that tip.
**Rule:** record-only (user decision U-4). Nothing in this file bumps a dependency, plugin, wrapper or workflow action. A bump needs an open advisory and then the user's own words per bump.

## How this was measured

- **OSV.dev:** `POST https://api.osv.dev/v1/query` with `{"package":{"name":"<group>:<artifact>","ecosystem":"Maven"},"version":"<resolved version>"}`; withdrawn entries are excluded; the ids listed are OSV's primary ids.
- **GitHub advisory database:** `gh api "advisories?ecosystem=maven&affects=<group>:<artifact>@<version>&per_page=100"`, keeping advisories whose `withdrawn_at` is null (`ecosystem=actions` for workflow actions).
- **Latest stable:** the newest version in the repository's `maven-metadata.xml` that carries no pre-release qualifier (alpha, beta, rc, milestone, snapshot). Where `<release>` in the metadata is a pre-release, the note says so and the newest stable is kept.
- **Control (proves both sources are live):** `com.fasterxml.jackson.core:jackson-databind@2.9.0` returned 74 advisories from OSV.dev and 74 from the GitHub advisory database. A source that returned 0 here would make every 0 below meaningless.
- **Dependabot alerts are disabled for this repository** (`gh api repos/ne1r0n/clickhouse-kafka-connect/dependabot/alerts` answers HTTP 403 "Dependabot alerts are disabled for this repository."), so the two databases above are the only advisory sources; there is no alert feed to read.
- **Exact-version caveat:** a clean answer means neither database records an advisory that affects that exact version today. It does not mean the artifact was audited, and a database can lag a disclosure. Gradle itself, the JDK and the container images have no Maven advisory feed and are recorded without one.
- **Scope order (U-5):** the shipped `clickhouseDependencies` set first (section 1), then the test, test-fixture and integration-test classpaths (sections 2 and 3), the Gradle plugins, wrapper and JDK (section 4), the workflow actions (section 5) and the test container images (section 6); section 7 holds the DEP-02 disposition.

## 1. Shipped runtime (clickhouseDependencies)

Resolved with `./gradlew -q dependencies --configuration clickhouseDependencies` (27 unique coordinates after conflict resolution; the right side of each `->` is taken). The Confluent archive built by `createConfluentArchive` (`build/confluent/clickhouse-kafka-connect-v1.7.0_gg7.zip`, its `verifyConfluentArchive` finalizer passed: 7932 inner entries, no nested jar, no Kafka API class) holds exactly one jar, the shadow jar `lib/clickhouse-kafka-connect-v1.7.0_gg7-confluent.jar`, with every dependency unpacked into it. The archive therefore has no per-dependency jar file names to compare; the cross-check reads inside the shadow jar instead:

- 18 `META-INF/maven/<group>/<artifact>/pom.properties` entries, each of whose version equals the resolved version of the same coordinate (`archive_jars_not_in_resolution: none`: no pom.properties names a coordinate the resolution lacks, and none names a different version).
- The remaining coordinates have no pom.properties and are matched by their package roots or native files (named per row): the four ClickHouse client artifacts, zstd-jni, jspecify, asm and RoaringBitmap. `jackson-bom` is a POM and contributes no jar.
- Every top-level path in the shadow jar maps to a resolved coordinate or to the connector itself (`com/clickhouse/kafka`): `com`, `net` (lz4-java), `org` (commons, httpclient5/httpcore5, jspecify, asm, RoaringBitmap, slf4j, the public-suffix list shipped by httpclient5), `darwin` and `linux` (zstd-jni natives), `META-INF`, `module-info.class` and `clickhouse-client-version.properties`. No package exists that no resolved coordinate explains, so no extra row is needed.

| coordinate | resolved version | latest stable | stale | OSV advisories (count; ids) | GitHub advisories (count; ids) | note |
| --- | --- | --- | --- | --- | --- | --- |
| `at.yawk.lz4:lz4-java` | 1.12.1 | 1.13.0 | yes | 0 | 0 | Version from the catalog's `lz4Java`; upstream's `org.lz4` capability rule selects this artifact over the legacy coordinate. Archive evidence: `META-INF/maven/.../pom.properties` reads 1.12.1. |
| `com.clickhouse:clickhouse-client` | 0.11.0 | 0.11.0 | no | 0 | 0 | Archive evidence: classes under `com/clickhouse/` (`clickhouse-client-version.properties` reads 0.11.0). |
| `com.clickhouse:clickhouse-data` | 0.11.0 | 0.11.0 | no | 0 | 0 | Archive evidence: classes under `com/clickhouse/` (`clickhouse-client-version.properties` reads 0.11.0). |
| `com.clickhouse:clickhouse-http-client` | 0.11.0 | 0.11.0 | no | 0 | 0 | Archive evidence: classes under `com/clickhouse/` (`clickhouse-client-version.properties` reads 0.11.0). |
| `com.clickhouse:client-v2` | 0.11.0 | 0.11.0 | no | 0 | 0 | Archive evidence: classes under `com/clickhouse/` (`clickhouse-client-version.properties` reads 0.11.0). |
| `com.fasterxml.jackson:jackson-bom` | 2.22.4 | 2.22.4 | no | 0 | 0 | POM only (a BOM): it constrains the three Jackson artifacts and contributes no jar to the archive. Archive evidence: none (pom only, no jar). |
| `com.fasterxml.jackson.core:jackson-annotations` | 2.22 | 2.22 | no | 0 | 0 | Catalog `jacksonAnnotations = 2.22` (annotations carry no patch level); metadata `<release>` is the pre-release 3.0-rc5, 2.22 is the newest stable. Archive evidence: `META-INF/maven/.../pom.properties` reads 2.22. |
| `com.fasterxml.jackson.core:jackson-core` | 2.22.4 | 2.22.4 | no | 0 | 0 | Archive evidence: `META-INF/maven/.../pom.properties` reads 2.22.4. |
| `com.fasterxml.jackson.core:jackson-databind` | 2.22.4 | 2.22.4 | no | 0 | 0 | Archive evidence: `META-INF/maven/.../pom.properties` reads 2.22.4. |
| `com.github.luben:zstd-jni` | 1.5.7-23 | 1.5.7-25 | yes | 0 | 0 | Transitive of client-v2; carries the native libraries (darwin, linux). Archive evidence: native libraries `libzstd-jni-1.5.7-23` under `darwin/` and `linux/`. |
| `com.google.code.gson:gson` | 2.15.0 | 2.15.0 | no | 0 | 0 | Archive evidence: `META-INF/maven/.../pom.properties` reads 2.15.0. |
| `com.google.errorprone:error_prone_annotations` | 2.50.0 | 2.52.0 | yes | 0 | 0 | Archive evidence: `META-INF/maven/.../pom.properties` reads 2.50.0. |
| `com.google.guava:failureaccess` | 1.0.3 | 1.0.3 | no | 0 | 0 | Archive evidence: `META-INF/maven/.../pom.properties` reads 1.0.3. |
| `com.google.guava:guava` | 33.7.2-jre | 33.8.0-jre | yes | 0 | 0 | Declared in `build.gradle.kts` with the comment `// temp to override client's with CVE`; the resolved version equals the one client-v2 requests. Archive evidence: `META-INF/maven/.../pom.properties` reads 33.7.2-jre. |
| `com.google.guava:listenablefuture` | 9999.0-empty-to-avoid-conflict-with-guava | 9999.0-empty-to-avoid-conflict-with-guava | no | 0 | 0 | Guava's empty placeholder jar. Archive evidence: `META-INF/maven/.../pom.properties` reads 9999.0-empty-to-avoid-conflict-with-guava. |
| `com.google.j2objc:j2objc-annotations` | 3.1 | 3.1 | no | 0 | 0 | Archive evidence: `META-INF/maven/.../pom.properties` reads 3.1. |
| `commons-codec:commons-codec` | 1.19.0 | 1.23.0 | yes | 0 | 0 | Archive evidence: `META-INF/maven/.../pom.properties` reads 1.19.0. |
| `commons-io:commons-io` | 2.20.0 | 2.22.0 | yes | 0 | 0 | Archive evidence: `META-INF/maven/.../pom.properties` reads 2.20.0. |
| `org.apache.commons:commons-compress` | 1.28.0 | 1.28.0 | no | 0 | 0 | Archive evidence: `META-INF/maven/.../pom.properties` reads 1.28.0. |
| `org.apache.commons:commons-lang3` | 3.20.0 | 3.21.0 | yes | 0 | 0 | 3.20.0 is the ClickHouse client 0.11.0's own pin on the shipped classpath; the catalog's `commonsLang3 = 3.21.0` reaches only the test and test-fixture classpaths (E-14). Archive evidence: `META-INF/maven/.../pom.properties` reads 3.20.0. |
| `org.apache.httpcomponents.client5:httpclient5` | 5.6.5 | 5.6.5 | no | 0 | 0 | `maven-metadata.xml` `<release>` is the pre-release 5.7-beta1; 5.6.5 is the newest stable line entry. Archive evidence: `META-INF/maven/.../pom.properties` reads 5.6.5. |
| `org.apache.httpcomponents.core5:httpcore5` | 5.4.4 | 5.4.4 | no | 0 | 0 | Metadata `<release>` is the pre-release 5.5-beta4; 5.4.4 is the newest stable. Archive evidence: `META-INF/maven/.../pom.properties` reads 5.4.4. |
| `org.apache.httpcomponents.core5:httpcore5-h2` | 5.4.4 | 5.4.4 | no | 0 | 0 | Metadata `<release>` is the pre-release 5.5-beta4; 5.4.4 is the newest stable. Archive evidence: `META-INF/maven/.../pom.properties` reads 5.4.4. |
| `org.jspecify:jspecify` | 1.0.1 | 1.0.1 | no | 0 | 0 | Archive evidence: classes under `org/jspecify/`. |
| `org.ow2.asm:asm` | 9.7 | 9.11.1 | yes | 0 | 0 | Archive evidence: classes under `org/objectweb/asm/`. |
| `org.roaringbitmap:RoaringBitmap` | 1.0.6 | 1.6.29 | yes | 0 | 0 | Archive evidence: classes under `org/roaringbitmap/`. |
| `org.slf4j:slf4j-api` | 2.0.7 | 2.0.20 | yes | 0 | 0 | Conflict resolution picks 2.0.7 (client-v2's request) over the 1.7.36 that httpclient5 requests; metadata `<release>` is the pre-release 2.1.0-alpha1. The catalog's `slf4j = 2.0.20` applies to `slf4j-reload4j` (an `implementation` dependency outside the archive), not to this coordinate. Archive evidence: `META-INF/maven/.../pom.properties` reads 2.0.7. |

## 2. Test and test-fixture classpaths (testRuntimeClasspath, testFixturesRuntimeClasspath)

Recorded in Task 2 (E-17).

## 3. Integration-test classpath (integrationTestRuntimeClasspath)

Recorded in Task 2 (E-17).

## 4. Gradle plugins, wrapper, JDK toolchain

Recorded in Task 2 (E-17).

## 5. Workflow actions

Recorded in Task 2 (E-17).

## 6. Test container images

Recorded in Task 2 (E-17).

## 7. DEP-02 proposals and decisions

Recorded in Task 2 (E-17).
