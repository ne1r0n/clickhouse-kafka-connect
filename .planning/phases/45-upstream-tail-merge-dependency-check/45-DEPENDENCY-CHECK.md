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

Resolved with `./gradlew -q dependencies --configuration testRuntimeClasspath` and `--configuration testFixturesRuntimeClasspath`; the rows are the union of the two minus section 1 (the same group, artifact and version is listed once; a coordinate whose version differs from section 1, such as commons-lang3 3.21.0 against the shipped 3.20.0, is its own row). 228 rows. Each note starts with the configurations the row resolves on. Neither classpath ships: only `clickhouseDependencies` reaches the Confluent archive.

| coordinate | resolved version | latest stable | stale | OSV advisories (count; ids) | GitHub advisories (count; ids) | note |
| --- | --- | --- | --- | --- | --- | --- |
| `ch.qos.logback:logback-classic` | 1.2.13 | 1.6.6 | yes | 0 | 0 | On testRuntimeClasspath. |
| `ch.qos.logback:logback-core` | 1.2.13 | 1.6.6 | yes | 6; GHSA-25qh-j22f-pwp8, GHSA-6v67-2wr5-gvf4, GHSA-jhq6-gfmj-v8fx, GHSA-p47f-322f-whfh, GHSA-pr98-23f8-jwxv, GHSA-qqpg-mvqg-649v | 6; GHSA-25qh-j22f-pwp8, GHSA-6v67-2wr5-gvf4, GHSA-jhq6-gfmj-v8fx, GHSA-p47f-322f-whfh, GHSA-pr98-23f8-jwxv, GHSA-qqpg-mvqg-649v | Pulled by logback-classic 1.2.13 <- zookeeper 3.8.4 <- io.confluent:kafka-schema-registry 7.9.1 and kafka_2.13 7.9.1-ccs. On testRuntimeClasspath. |
| `ch.qos.reload4j:reload4j` | 1.2.22 | 1.2.27 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `com.damnhandy:handy-uri-templates` | 2.1.8 | 2.1.8 | no | 0 | 0 | On testRuntimeClasspath. |
| `com.eclipsesource.minimal-json:minimal-json` | 0.9.5 | 0.9.5 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-csv` | 2.22.4 | 2.22.4 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` | 2.22.4 | 2.22.4 | no | 0 | 0 | On testRuntimeClasspath. |
| `com.fasterxml.jackson.datatype:jackson-datatype-guava` | 2.22.4 | 2.22.4 | no | 0 | 0 | On testRuntimeClasspath. |
| `com.fasterxml.jackson.datatype:jackson-datatype-jdk8` | 2.22.4 | 2.22.4 | no | 0 | 0 | On testRuntimeClasspath. |
| `com.fasterxml.jackson.datatype:jackson-datatype-joda` | 2.22.4 | 2.22.4 | no | 0 | 0 | On testRuntimeClasspath. |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` | 2.22.4 | 2.22.4 | no | 0 | 0 | On testRuntimeClasspath. |
| `com.fasterxml.jackson.jaxrs:jackson-jaxrs-base` | 2.22.4 | 2.22.4 | no | 0 | 0 | On testRuntimeClasspath. |
| `com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider` | 2.22.4 | 2.22.4 | no | 0 | 0 | On testRuntimeClasspath. |
| `com.fasterxml.jackson.module:jackson-module-blackbird` | 2.22.4 | 2.22.4 | no | 0 | 0 | On testRuntimeClasspath. |
| `com.fasterxml.jackson.module:jackson-module-jaxb-annotations` | 2.22.4 | 2.22.4 | no | 0 | 0 | On testRuntimeClasspath. |
| `com.fasterxml.jackson.module:jackson-module-parameter-names` | 2.22.4 | 2.22.4 | no | 0 | 0 | On testRuntimeClasspath. |
| `com.fasterxml.jackson.module:jackson-module-scala_2.13` | 2.22.4 | 2.22.4 | no | 0 | 0 | On testRuntimeClasspath. |
| `com.fasterxml:classmate` | 1.5.1 | 1.8.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `com.github.ben-manes.caffeine:caffeine` | 2.9.3 | 3.3.1 | yes | 0 | 0 | On testRuntimeClasspath. |
| `com.github.docker-java:docker-java-api` | 3.7.1 | 3.7.1 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `com.github.docker-java:docker-java-transport-zerodep` | 3.7.1 | 3.7.1 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `com.github.docker-java:docker-java-transport` | 3.7.1 | 3.7.1 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `com.github.erosb:everit-json-schema` | 1.14.3 | 1.14.6 | yes | 0 | 0 | On testRuntimeClasspath. |
| `com.github.erosb:json-sKema` | 0.19.0 | 0.32.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `com.google.api.grpc:proto-google-common-protos` | 2.22.1 | 2.79.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `com.google.code.findbugs:jsr305` | 3.0.2 | 3.0.2 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `com.google.crypto.tink:tink` | 1.15.0 | 1.24.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `com.google.protobuf:protobuf-java-util` | 3.25.5 | 4.36.2 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. Metadata `<release>` is the pre-release 4.37.0-RC1. |
| `com.google.protobuf:protobuf-java` | 3.25.9 | 4.36.2 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. Metadata `<release>` is the pre-release 4.37.0-RC1. |
| `com.google.re2j:re2j` | 1.6 | 1.8 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `com.squareup.okio:okio-jvm` | 3.9.0 | 3.18.2 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath. |
| `com.squareup.okio:okio` | 3.9.0 | 3.18.2 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath. |
| `com.squareup.wire:wire-runtime-jvm` | 5.1.0 | 7.1.2 | yes | 1; GHSA-7xpr-hc2w-34m9 | 1; GHSA-7xpr-hc2w-34m9 | Pulled by wire-schema-jvm 5.1.0 <- io.confluent:kafka-connect-protobuf-converter 7.9.1 and kafka-protobuf-provider 7.9.1. On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `com.squareup.wire:wire-runtime` | 5.1.0 | 7.1.2 | yes | 3; GHSA-7xpr-hc2w-34m9, GHSA-9rm7-3qhh-h2mc, GHSA-pwf9-q62p-v7wc | 3; GHSA-7xpr-hc2w-34m9, GHSA-9rm7-3qhh-h2mc, GHSA-pwf9-q62p-v7wc | Pulled by io.confluent:kafka-connect-protobuf-converter 7.9.1 and kafka-protobuf-provider 7.9.1 (catalog `kafkaPlatformProtobuf` and `kafkaPlatformSchemaRegistry`). On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `com.squareup.wire:wire-schema-jvm` | 5.1.0 | 7.1.2 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `com.squareup:javapoet` | 1.13.0 | 1.13.0 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `com.squareup:kotlinpoet-jvm` | 1.18.1 | 2.4.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `com.squareup:kotlinpoet` | 1.18.1 | 2.4.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `com.thoughtworks.paranamer:paranamer` | 2.8.3 | 2.8.3 | no | 0 | 0 | On testRuntimeClasspath. |
| `com.typesafe.scala-logging:scala-logging_2.13` | 3.9.5 | 3.9.6 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 4.0.0-RC1. |
| `com.yammer.metrics:metrics-core` | 2.2.0 | 2.2.0 | no | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 3.0.0-BETA1. |
| `commons-beanutils:commons-beanutils` | 1.9.4 | 1.11.0 | yes | 1; GHSA-wxr5-93ph-8wr9 | 1; GHSA-wxr5-93ph-8wr9 | Pulled (commons-validator 1.7 requests it) by kafka_2.13 7.9.1-ccs, everit-json-schema 1.14.3 and json-sKema 0.19.0 <- io.confluent:kafka-json-schema-provider 7.9.1. On testRuntimeClasspath. |
| `commons-cli:commons-cli` | 1.4 | 1.11.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `commons-collections:commons-collections` | 3.2.2 | 20040616 | yes | 0 | 0 | On testRuntimeClasspath. |
| `commons-digester:commons-digester` | 2.1 | 2.1 | no | 0 | 0 | On testRuntimeClasspath. |
| `commons-logging:commons-logging` | 1.3.2 | 1.4.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `commons-validator:commons-validator` | 1.9.0 | 1.11.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `eu.rekawek.toxiproxy:toxiproxy-java` | 2.1.11 | 2.1.11 | no | 0 | 0 | On testRuntimeClasspath, integrationTestRuntimeClasspath. |
| `io.confluent:common-utils` | 7.9.1 | 8.4.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `io.confluent:kafka-avro-serializer` | 7.9.1 | 8.4.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `io.confluent:kafka-connect-avro-converter` | 7.9.1 | 8.4.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `io.confluent:kafka-connect-avro-data` | 7.9.1 | 8.4.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `io.confluent:kafka-connect-protobuf-converter` | 7.9.1 | 8.4.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `io.confluent:kafka-json-schema-provider` | 7.9.1 | 8.4.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `io.confluent:kafka-protobuf-provider` | 7.9.1 | 8.4.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `io.confluent:kafka-protobuf-serializer` | 7.9.1 | 8.4.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `io.confluent:kafka-protobuf-types` | 7.9.1 | 8.4.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `io.confluent:kafka-schema-converter` | 7.9.1 | 8.4.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `io.confluent:kafka-schema-registry-client` | 7.9.1 | 8.4.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `io.confluent:kafka-schema-registry` | 7.9.1 | 8.4.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `io.confluent:kafka-schema-serializer` | 7.9.1 | 8.4.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `io.confluent:logredactor-metrics` | 1.0.12 | 1.0.23 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `io.confluent:logredactor` | 1.0.12 | 1.0.23 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `io.confluent:rest-utils` | 7.9.1 | 8.4.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `io.dropwizard.metrics:metrics-core` | 4.1.12.1 | 4.2.40 | yes | 0 | 0 | On testRuntimeClasspath. |
| `io.github.classgraph:classgraph` | 4.8.138 | 6.0.1 | yes | 0 | 0 | On testRuntimeClasspath. |
| `io.github.resilience4j:resilience4j-core` | 1.7.1 | 2.4.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `io.github.resilience4j:resilience4j-ratelimiter` | 1.7.1 | 2.4.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `io.kcache:kcache` | 4.0.11 | 5.2.6 | yes | 0 | 0 | On testRuntimeClasspath. |
| `io.netty:netty-buffer` | 4.1.115.Final | 4.2.20.Final | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 5.0.0.Alpha2. |
| `io.netty:netty-codec` | 4.1.115.Final | 4.2.20.Final | yes | 3; GHSA-3p8m-j85q-pgmj, GHSA-558v-64gr-wgg4, GHSA-mj4r-2hfc-f8p6 | 3; GHSA-3p8m-j85q-pgmj, GHSA-558v-64gr-wgg4, GHSA-mj4r-2hfc-f8p6 | Pulled by io.confluent:kafka-schema-registry 7.9.1 (4.1.115.Final, lifted from 4.1.105.Final requested by zookeeper 3.8.4). On testRuntimeClasspath. Metadata `<release>` is the pre-release 5.0.0.Alpha2. |
| `io.netty:netty-common` | 4.1.115.Final | 4.2.20.Final | yes | 1; GHSA-389x-839f-4rhx | 1; GHSA-389x-839f-4rhx | Lifted to 4.1.115.Final by netty-buffer/netty-codec/netty-transport requests from zookeeper 3.8.4 and io.confluent:kafka-schema-registry 7.9.1. On testRuntimeClasspath. Metadata `<release>` is the pre-release 5.0.0.Alpha2. |
| `io.netty:netty-handler` | 4.1.105.Final | 4.2.20.Final | yes | 6; GHSA-3qp7-7mw8-wx86, GHSA-4g8c-wm8x-jfhw, GHSA-c4c3-7fpv-j4q5, GHSA-c653-97m9-rcg9, GHSA-fccg-mwvh-qqg4, GHSA-x4gw-5cx5-pgmh | 6; GHSA-3qp7-7mw8-wx86, GHSA-4g8c-wm8x-jfhw, GHSA-c4c3-7fpv-j4q5, GHSA-c653-97m9-rcg9, GHSA-fccg-mwvh-qqg4, GHSA-x4gw-5cx5-pgmh | Pulled by zookeeper 3.8.4 and kafka_2.13 7.9.1-ccs <- io.confluent:kafka-schema-registry 7.9.1. On testRuntimeClasspath. Metadata `<release>` is the pre-release 5.0.0.Alpha2. |
| `io.netty:netty-resolver` | 4.1.115.Final | 4.2.20.Final | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 5.0.0.Alpha2. |
| `io.netty:netty-transport-classes-epoll` | 4.1.105.Final | 4.2.20.Final | yes | 0 | 0 | On testRuntimeClasspath. |
| `io.netty:netty-transport-native-epoll` | 4.1.105.Final | 4.2.20.Final | yes | 1; GHSA-w573-9ffj-6ff9 | 1; GHSA-w573-9ffj-6ff9 | Pulled by zookeeper 3.8.4 <- io.confluent:kafka-schema-registry 7.9.1. On testRuntimeClasspath. Metadata `<release>` is the pre-release 5.0.0.Alpha2. |
| `io.netty:netty-transport-native-unix-common` | 4.1.105.Final | 4.2.20.Final | yes | 0 | 0 | On testRuntimeClasspath. |
| `io.netty:netty-transport` | 4.1.115.Final | 4.2.20.Final | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 5.0.0.Alpha2. |
| `io.swagger.core.v3:swagger-annotations` | 2.1.10 | 3.0.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `io.swagger.core.v3:swagger-core` | 2.1.10 | 3.0.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `io.swagger.core.v3:swagger-models` | 2.1.10 | 3.0.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `io.vavr:vavr-match` | 0.10.2 | 1.0.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `io.vavr:vavr` | 0.10.2 | 1.1.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `io.yokota:mbknor-jackson-jsonschema-java8` | 1.0.39.2 | 1.0.39.3 | yes | 0 | 0 | On testRuntimeClasspath. |
| `jakarta.activation:jakarta.activation-api` | 1.2.2 | 2.1.4 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 2.2.0-M2. |
| `jakarta.annotation:jakarta.annotation-api` | 1.3.5 | 3.0.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `jakarta.el:jakarta.el-api` | 3.0.3 | 6.0.1 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 6.1.0-M2. |
| `jakarta.validation:jakarta.validation-api` | 2.0.2 | 3.1.1 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 4.0.0-M1. |
| `jakarta.ws.rs:jakarta.ws.rs-api` | 3.1.0 | 4.0.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. Metadata `<release>` is the pre-release 5.0.0-M1. |
| `jakarta.xml.bind:jakarta.xml.bind-api` | 2.3.3 | 4.0.5 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 4.1.0-M1. |
| `javax.activation:activation` | 1.1.1 | 1.1.1 | no | 0 | 0 | On testRuntimeClasspath. |
| `javax.annotation:javax.annotation-api` | 1.3.2 | 1.3.2 | no | 0 | 0 | On testRuntimeClasspath. |
| `javax.servlet:javax.servlet-api` | 3.1.0 | 4.0.1 | yes | 0 | 0 | On testRuntimeClasspath. |
| `javax.validation:validation-api` | 2.0.1.Final | 2.0.1.Final | no | 0 | 0 | On testRuntimeClasspath. |
| `javax.websocket:javax.websocket-api` | 1.0 | 1.1 | yes | 0 | 0 | On testRuntimeClasspath. |
| `javax.websocket:javax.websocket-client-api` | 1.0 | 1.1 | yes | 0 | 0 | On testRuntimeClasspath. |
| `javax.xml.bind:jaxb-api` | 2.2.12 | 2.3.1 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 2.4.0-b180830.0359. |
| `joda-time:joda-time` | 2.12.7 | 2.15.1 | yes | 0 | 0 | On testRuntimeClasspath. |
| `junit:junit` | 4.13.2 | 4.13.2 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `net.bytebuddy:byte-buddy-agent` | 1.17.7 | 1.18.17-jdk5 | yes | 0 | 0 | On testRuntimeClasspath. |
| `net.bytebuddy:byte-buddy` | 1.17.7 | 1.18.17-jdk5 | yes | 0 | 0 | On testRuntimeClasspath. |
| `net.java.dev.jna:jna` | 5.18.1 | 5.19.1 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `net.sf.jopt-simple:jopt-simple` | 5.0.4 | 5.0.4 | no | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 6.0-alpha-3. |
| `net.sourceforge.argparse4j:argparse4j` | 0.7.0 | 0.9.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.apache.avro:avro` | 1.11.4 | 1.12.2 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.apache.commons:commons-collections4` | 4.4 | 4.6.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.apache.commons:commons-lang3` | 3.21.0 | 3.21.0 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.apache.directory.api:api-asn1-api` | 2.1.5 | 2.1.9 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.apache.directory.api:api-asn1-ber` | 2.1.5 | 2.1.9 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.apache.directory.api:api-i18n` | 2.1.5 | 2.1.9 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.apache.directory.api:api-ldap-model` | 2.1.5 | 2.1.9 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.apache.directory.api:api-util` | 2.1.5 | 2.1.9 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.apache.kafka:connect-api` | 4.4.0 | 4.4.0 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.apache.kafka:connect-json` | 4.4.0 | 4.4.0 | no | 0 | 0 | On testRuntimeClasspath. |
| `org.apache.kafka:kafka-clients` | 7.9.1-ccs | 8.4.0-ccs | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. Confluent `-ccs` build of an Apache Kafka artifact: the databases index Apache versions, so the exact-version answer 0 is not comparable (see the note under this table). The resolver picks it over the 4.4.0 line because 7.9.1 sorts above 4.4.0. |
| `org.apache.kafka:kafka-group-coordinator-api` | 7.9.1-ccs | 8.4.0-ccs | yes | 0 | 0 | On testRuntimeClasspath. Confluent `-ccs` build of an Apache Kafka artifact: the databases index Apache versions, so the exact-version answer 0 is not comparable (see the note under this table). The resolver picks it over the 4.4.0 line because 7.9.1 sorts above 4.4.0. |
| `org.apache.kafka:kafka-group-coordinator` | 7.9.1-ccs | 8.4.0-ccs | yes | 0 | 0 | On testRuntimeClasspath. Confluent `-ccs` build of an Apache Kafka artifact: the databases index Apache versions, so the exact-version answer 0 is not comparable (see the note under this table). The resolver picks it over the 4.4.0 line because 7.9.1 sorts above 4.4.0. |
| `org.apache.kafka:kafka-metadata` | 7.9.1-ccs | 8.4.0-ccs | yes | 0 | 0 | On testRuntimeClasspath. Confluent `-ccs` build of an Apache Kafka artifact: the databases index Apache versions, so the exact-version answer 0 is not comparable (see the note under this table). The resolver picks it over the 4.4.0 line because 7.9.1 sorts above 4.4.0. |
| `org.apache.kafka:kafka-raft` | 7.9.1-ccs | 8.4.0-ccs | yes | 0 | 0 | On testRuntimeClasspath. Confluent `-ccs` build of an Apache Kafka artifact: the databases index Apache versions, so the exact-version answer 0 is not comparable (see the note under this table). The resolver picks it over the 4.4.0 line because 7.9.1 sorts above 4.4.0. |
| `org.apache.kafka:kafka-server-common` | 7.9.1-ccs | 8.4.0-ccs | yes | 0 | 0 | On testRuntimeClasspath. Confluent `-ccs` build of an Apache Kafka artifact: the databases index Apache versions, so the exact-version answer 0 is not comparable (see the note under this table). The resolver picks it over the 4.4.0 line because 7.9.1 sorts above 4.4.0. |
| `org.apache.kafka:kafka-server` | 7.9.1-ccs | 8.4.0-ccs | yes | 0 | 0 | On testRuntimeClasspath. Confluent `-ccs` build of an Apache Kafka artifact: the databases index Apache versions, so the exact-version answer 0 is not comparable (see the note under this table). The resolver picks it over the 4.4.0 line because 7.9.1 sorts above 4.4.0. |
| `org.apache.kafka:kafka-storage-api` | 7.9.1-ccs | 8.4.0-ccs | yes | 0 | 0 | On testRuntimeClasspath. Confluent `-ccs` build of an Apache Kafka artifact: the databases index Apache versions, so the exact-version answer 0 is not comparable (see the note under this table). The resolver picks it over the 4.4.0 line because 7.9.1 sorts above 4.4.0. |
| `org.apache.kafka:kafka-storage` | 7.9.1-ccs | 8.4.0-ccs | yes | 0 | 0 | On testRuntimeClasspath. Confluent `-ccs` build of an Apache Kafka artifact: the databases index Apache versions, so the exact-version answer 0 is not comparable (see the note under this table). The resolver picks it over the 4.4.0 line because 7.9.1 sorts above 4.4.0. |
| `org.apache.kafka:kafka-tools-api` | 7.9.1-ccs | 8.4.0-ccs | yes | 0 | 0 | On testRuntimeClasspath. Confluent `-ccs` build of an Apache Kafka artifact: the databases index Apache versions, so the exact-version answer 0 is not comparable (see the note under this table). The resolver picks it over the 4.4.0 line because 7.9.1 sorts above 4.4.0. |
| `org.apache.kafka:kafka-transaction-coordinator` | 7.9.1-ccs | 8.4.0-ccs | yes | 0 | 0 | On testRuntimeClasspath. Confluent `-ccs` build of an Apache Kafka artifact: the databases index Apache versions, so the exact-version answer 0 is not comparable (see the note under this table). The resolver picks it over the 4.4.0 line because 7.9.1 sorts above 4.4.0. |
| `org.apache.kafka:kafka_2.13` | 7.9.1-ccs | 8.4.0-ccs | yes | 0 | 0 | On testRuntimeClasspath. Confluent `-ccs` build of an Apache Kafka artifact: the databases index Apache versions, so the exact-version answer 0 is not comparable (see the note under this table). The resolver picks it over the 4.4.0 line because 7.9.1 sorts above 4.4.0. |
| `org.apache.mina:mina-core` | 2.2.3 | 2.2.10 | yes | 6; GHSA-76h9-2vwh-w278, GHSA-8297-v2rf-2p32, GHSA-995c-6rp3-4m4x, GHSA-f2wh-grmh-r6jm, GHSA-v3pr-hxpr-mfm8, GHSA-vf5j-865m-mq7c | 6; GHSA-76h9-2vwh-w278, GHSA-8297-v2rf-2p32, GHSA-995c-6rp3-4m4x, GHSA-f2wh-grmh-r6jm, GHSA-v3pr-hxpr-mfm8, GHSA-vf5j-865m-mq7c | Pulled by org.eclipse.jetty:jetty-jaas 9.4.57.v20241219 <- io.confluent:rest-utils 7.9.1 <- kafka-schema-registry 7.9.1. On testRuntimeClasspath. Metadata `<release>` is the pre-release 3.0.0-M2. |
| `org.apache.servicemix.bundles:org.apache.servicemix.bundles.antlr` | 2.7.7_5 | 3.5.3_1 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.apache.yetus:audience-annotations` | 0.12.0 | 0.15.1 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.apache.zookeeper:zookeeper-jute` | 3.8.4 | 3.9.7 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.apache.zookeeper:zookeeper` | 3.8.4 | 3.9.7 | yes | 2; GHSA-7xrh-hqfc-g7qr, GHSA-crhr-qqj8-rpxc | 2; GHSA-7xrh-hqfc-g7qr, GHSA-crhr-qqj8-rpxc | Pulled by io.confluent:kafka-schema-registry 7.9.1 and kafka_2.13 7.9.1-ccs. On testRuntimeClasspath. |
| `org.apiguardian:apiguardian-api` | 1.1.2 | 1.1.2 | no | 0 | 0 | On testRuntimeClasspath. |
| `org.bitbucket.b_c:jose4j` | 0.9.4 | 0.9.7 | yes | 1; GHSA-3677-xxcr-wjqv | 1; GHSA-3677-xxcr-wjqv | Pulled by io.confluent:kafka-schema-registry 7.9.1. On testRuntimeClasspath. |
| `org.checkerframework:checker-qual` | 3.19.0 | 4.4.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.conscrypt:conscrypt-openjdk-uber` | 2.5.2 | 2.9.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty.http2:http2-common` | 9.4.57.v20241219 | 11.0.26 | yes | 1; GHSA-mmxm-8w33-wc4h | 1; GHSA-mmxm-8w33-wc4h | Pulled by io.confluent:rest-utils 7.9.1 <- kafka-schema-registry 7.9.1. On testRuntimeClasspath. |
| `org.eclipse.jetty.http2:http2-hpack` | 9.4.57.v20241219 | 11.0.26 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty.http2:http2-server` | 9.4.57.v20241219 | 11.0.26 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty.websocket:javax-websocket-client-impl` | 9.4.57.v20241219 | 9.4.58.v20250814 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty.websocket:javax-websocket-server-impl` | 9.4.57.v20241219 | 9.4.58.v20250814 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty.websocket:websocket-api` | 9.4.57.v20241219 | 9.4.58.v20250814 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty.websocket:websocket-client` | 9.4.57.v20241219 | 9.4.58.v20250814 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty.websocket:websocket-common` | 9.4.57.v20241219 | 9.4.58.v20250814 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty.websocket:websocket-server` | 9.4.57.v20241219 | 9.4.58.v20250814 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty.websocket:websocket-servlet` | 9.4.57.v20241219 | 11.0.26 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-alpn-conscrypt-server` | 9.4.57.v20241219 | 12.1.14 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-alpn-java-server` | 9.4.57.v20241219 | 12.1.14 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-alpn-server` | 9.4.57.v20241219 | 12.1.14 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-annotations` | 9.4.57.v20241219 | 12.1.14 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-client` | 9.4.57.v20241219 | 12.1.14 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-continuation` | 9.4.57.v20241219 | 9.4.58.v20250814 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-http` | 9.4.57.v20241219 | 12.1.14 | yes | 3; GHSA-355h-qmc2-wpwf, GHSA-qh8g-58pp-2wxh, GHSA-wjpw-4j6x-6rwh | 3; GHSA-355h-qmc2-wpwf, GHSA-qh8g-58pp-2wxh, GHSA-wjpw-4j6x-6rwh | Pulled by jetty websocket and server modules <- io.confluent:rest-utils 7.9.1 <- kafka-schema-registry 7.9.1. On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-io` | 9.4.57.v20241219 | 12.1.14 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-jaas` | 9.4.57.v20241219 | 11.0.26 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-jmx` | 9.4.57.v20241219 | 12.1.14 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-jndi` | 9.4.57.v20241219 | 12.1.14 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-plus` | 9.4.57.v20241219 | 12.1.14 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-security` | 9.4.57.v20241219 | 12.1.14 | yes | 1; GHSA-2fvj-hgj9-j2gr | 1; GHSA-2fvj-hgj9-j2gr | Pulled by io.confluent:rest-utils 7.9.1 <- kafka-schema-registry 7.9.1. On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-server` | 9.4.57.v20241219 | 12.1.14 | yes | 1; GHSA-7p3p-8qv8-m2vh | 1; GHSA-7p3p-8qv8-m2vh | Pulled by io.confluent:rest-utils 7.9.1 and kafka-schema-registry 7.9.1. On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-servlet` | 9.4.57.v20241219 | 11.0.26 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-servlets` | 9.4.57.v20241219 | 11.0.26 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-util-ajax` | 9.4.57.v20241219 | 12.1.14 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-util` | 9.4.57.v20241219 | 12.1.14 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-webapp` | 9.4.57.v20241219 | 11.0.25 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.eclipse.jetty:jetty-xml` | 9.4.57.v20241219 | 12.1.14 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.glassfish.hk2.external:aopalliance-repackaged` | 2.6.1 | 4.0.3 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.glassfish.hk2.external:jakarta.inject` | 2.6.1 | 2.6.1 | no | 0 | 0 | On testRuntimeClasspath. |
| `org.glassfish.hk2:hk2-api` | 2.6.1 | 4.0.3 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.glassfish.hk2:hk2-locator` | 2.6.1 | 4.0.3 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.glassfish.hk2:hk2-utils` | 2.6.1 | 4.0.3 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.glassfish.hk2:osgi-resource-locator` | 1.0.3 | 3.0.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.glassfish.jersey.containers:jersey-container-servlet-core` | 2.36 | 3.1.12 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 4.0.0-M2. |
| `org.glassfish.jersey.containers:jersey-container-servlet` | 2.36 | 4.0.3 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 5.0.0-M1. |
| `org.glassfish.jersey.core:jersey-client` | 2.36 | 4.0.3 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 5.0.0-M1. |
| `org.glassfish.jersey.core:jersey-common` | 2.36 | 4.0.3 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 5.0.0-M1. |
| `org.glassfish.jersey.core:jersey-server` | 2.36 | 4.0.3 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 5.0.0-M1. |
| `org.glassfish.jersey.ext:jersey-bean-validation` | 2.36 | 4.0.3 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 5.0.0-M1. |
| `org.glassfish.jersey.inject:jersey-hk2` | 2.36 | 4.0.3 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 5.0.0-M1. |
| `org.glassfish:jakarta.el` | 3.0.4 | 4.0.2 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 5.0.0-M1. |
| `org.hamcrest:hamcrest-core` | 1.3 | 3.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.hamcrest:hamcrest` | 3.0 | 3.0 | no | 0 | 0 | On testRuntimeClasspath. |
| `org.hibernate.validator:hibernate-validator` | 6.2.0.Final | 9.1.6.Final | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 9.2.0.Beta1. |
| `org.javassist:javassist` | 3.25.0-GA | 3.33.0-GA | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.jboss.logging:jboss-logging` | 3.4.1.Final | 3.6.3.Final | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.jetbrains.kotlin:kotlin-reflect` | 2.0.0 | 2.4.21 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. Metadata `<release>` is the pre-release 2.5.0-Beta2. |
| `org.jetbrains.kotlin:kotlin-script-runtime` | 1.9.10 | 2.4.21 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 2.5.0-Beta2. |
| `org.jetbrains.kotlin:kotlin-scripting-common` | 1.9.10 | 2.4.21 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 2.5.0-Beta2. |
| `org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable` | 1.9.10 | 2.4.21 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 2.5.0-Beta2. |
| `org.jetbrains.kotlin:kotlin-scripting-compiler-impl-embeddable` | 1.9.10 | 2.4.21 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 2.5.0-Beta2. |
| `org.jetbrains.kotlin:kotlin-scripting-jvm` | 1.9.10 | 2.4.21 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 2.5.0-Beta2. |
| `org.jetbrains.kotlin:kotlin-stdlib` | 1.9.21 | 2.4.21 | yes | 0 | 0 | On testFixturesRuntimeClasspath. Metadata `<release>` is the pre-release 2.5.0-Beta2. |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.0.20 | 2.4.21 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 2.5.0-Beta2. |
| `org.jetbrains:annotations` | 17.0.0 | 26.1.0 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.json:json` | 20260814 | 20260814 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.junit.jupiter:junit-jupiter-api` | 5.14.4 | 6.1.3 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.junit.jupiter:junit-jupiter-engine` | 5.14.4 | 6.1.3 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.junit.jupiter:junit-jupiter-params` | 5.14.4 | 6.1.3 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.junit.jupiter:junit-jupiter` | 5.14.4 | 6.1.3 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.junit.platform:junit-platform-commons` | 1.14.4 | 6.1.3 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.junit.platform:junit-platform-engine` | 1.14.4 | 6.1.3 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.junit.platform:junit-platform-launcher` | 1.14.4 | 6.1.3 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.junit.platform:junit-platform-runner` | 1.14.4 | 1.14.4 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.junit.platform:junit-platform-suite-api` | 1.14.4 | 6.1.3 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.junit.platform:junit-platform-suite-commons` | 1.14.4 | 1.14.4 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. Metadata `<release>` is the pre-release 6.0.0-M1. |
| `org.junit:junit-bom` | 5.14.4 | 6.1.3 | yes | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.mockito:mockito-core` | 5.24.0 | 5.24.0 | no | 0 | 0 | On testRuntimeClasspath. |
| `org.mockito:mockito-junit-jupiter` | 5.24.0 | 5.24.0 | no | 0 | 0 | On testRuntimeClasspath. |
| `org.objenesis:objenesis` | 3.3 | 3.6 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.opentest4j:opentest4j` | 1.3.0 | 1.3.0 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.ow2.asm:asm-commons` | 9.7.1 | 9.11.1 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.ow2.asm:asm-tree` | 9.7.1 | 9.11.1 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.ow2.asm:asm` | 9.7.1 | 9.11.1 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.pcollections:pcollections` | 4.0.1 | 5.0.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.rnorth.duct-tape:duct-tape` | 1.0.8 | 1.0.8 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.scala-lang.modules:scala-collection-compat_2.13` | 2.10.0 | 2.14.0 | yes | 0 | 0 | On testRuntimeClasspath. |
| `org.scala-lang.modules:scala-java8-compat_2.13` | 1.0.2 | 1.0.2 | no | 0 | 0 | On testRuntimeClasspath. |
| `org.scala-lang:scala-library` | 2.13.18 | 3.10.0 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 3.10.1-RC2. |
| `org.scala-lang:scala-reflect` | 2.13.15 | 2.13.18 | yes | 0 | 0 | On testRuntimeClasspath. Metadata `<release>` is the pre-release 2.13.19-M1. |
| `org.slf4j:slf4j-api` | 2.0.20 | 2.0.20 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. Metadata `<release>` is the pre-release 2.1.0-alpha1. |
| `org.slf4j:slf4j-reload4j` | 2.0.20 | 2.0.20 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. Metadata `<release>` is the pre-release 2.1.0-alpha1. |
| `org.testcontainers:testcontainers-clickhouse` | 2.0.5 | 2.0.5 | no | 0 | 0 | On testRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.testcontainers:testcontainers-database-commons` | 2.0.5 | 2.0.5 | no | 0 | 0 | On testRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.testcontainers:testcontainers-jdbc` | 2.0.5 | 2.0.5 | no | 0 | 0 | On testRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.testcontainers:testcontainers-toxiproxy` | 2.0.5 | 2.0.5 | no | 0 | 0 | On testRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.testcontainers:testcontainers` | 2.0.5 | 2.0.5 | no | 0 | 0 | On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.xerial.snappy:snappy-java` | 1.1.10.5 | 1.1.10.12 | yes | 3; GHSA-gwq9-9ffj-mvrv, GHSA-q233-g92q-qxp6, GHSA-xqrx-qh46-34m7 | 3; GHSA-gwq9-9ffj-mvrv, GHSA-q233-g92q-qxp6, GHSA-xqrx-qh46-34m7 | Pulled by org.apache.kafka:kafka-clients 4.4.0 (through connect-api), io.confluent:kafka-schema-registry 7.9.1 and kafka-schema-registry-client 7.9.1. On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.yaml:snakeyaml` | 2.0 | 2.7 | yes | 0 | 0 | On testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. |
| `org.yaml:snakeyaml` | 2.5 | 2.7 | yes | 0 | 0 | On testRuntimeClasspath. |

**Confluent `-ccs` rows (12 `org.apache.kafka` artifacts at `7.9.1-ccs`).** The answer 0 for these rows is not a clean bill: both databases index Apache Kafka's own version numbers, so the version string `7.9.1-ccs` matches no affected range. For the Apache lines of the same artifacts the two sources do return advisories: `kafka-clients` 3.9.0 has GHSA-5qcv-4rpc-jp93 (CVE-2026-35554, high, fixed in 3.9.2), GHSA-wf66-mphr-4c4r (CVE-2026-33558, medium, fixed in 3.9.2) and GHSA-vgq5-3255-v292 (CVE-2025-27817, medium, fixed in 3.9.1); `kafka_2.13` 3.9.0 has GHSA-76qp-h5mr-frr4 (CVE-2025-27818, high, fixed in 3.9.1); 3.9.2 and 4.4.0 return none. Which Apache release a Confluent `7.9.1-ccs` build corresponds to, and whether it carries those fixes, cannot be read from either database or from the jar (`kafka/kafka-version.properties` reads `7.9.1-ccs`). These 12 rows are therefore recorded as inconclusive and are not counted as open advisories under the exact-version rule; they are named in section 7 for the user. The test classpath resolves the `-ccs` artifacts rather than Apache's 4.4.0 because Gradle orders `7.9.1-ccs` above `4.4.0`.

## 3. Integration-test classpath (integrationTestRuntimeClasspath)

Resolved with `./gradlew -q dependencies --configuration integrationTestRuntimeClasspath`, minus sections 1 and 2. 7 rows.

| coordinate | resolved version | latest stable | stale | OSV advisories (count; ids) | GitHub advisories (count; ids) | note |
| --- | --- | --- | --- | --- | --- | --- |
| `com.squareup.okhttp3:okhttp-jvm` | 5.5.1 | 5.5.1 | no | 0 | 0 | On integrationTestRuntimeClasspath. |
| `com.squareup.okhttp3:okhttp` | 5.5.1 | 5.5.1 | no | 0 | 0 | On integrationTestRuntimeClasspath. |
| `com.squareup.okio:okio-jvm` | 3.18.1 | 3.18.2 | yes | 0 | 0 | On integrationTestRuntimeClasspath. |
| `com.squareup.okio:okio` | 3.18.1 | 3.18.2 | yes | 0 | 0 | On integrationTestRuntimeClasspath. |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.2.21 | 2.4.21 | yes | 0 | 0 | On integrationTestRuntimeClasspath. Metadata `<release>` is the pre-release 2.5.0-Beta2. |
| `org.slf4j:slf4j-simple` | 2.0.20 | 2.0.20 | no | 0 | 0 | On integrationTestRuntimeClasspath. Metadata `<release>` is the pre-release 2.1.0-alpha1. |
| `org.testcontainers:testcontainers-kafka` | 2.0.5 | 2.0.5 | no | 0 | 0 | On integrationTestRuntimeClasspath. |

## 4. Gradle plugins, wrapper, JDK toolchain

Gradle plugins from `./gradlew -q buildEnvironment` (the buildscript classpath, 43 resolved artifacts; plugin marker POMs are not listed): spotless 8.10.4, shadow 9.6.1, protobuf 0.10.0 and the versions plugin 0.66.0; the build has no buildconfig plugin on its classpath. The four JaCoCo tool artifacts at the `toolVersion` of `build.gradle.kts` are added to the same table (47 rows in all).

| coordinate | resolved version | latest stable | stale | OSV advisories (count; ids) | GitHub advisories (count; ids) | note |
| --- | --- | --- | --- | --- | --- | --- |
| `com.diffplug.durian:durian-collect` | 1.2.0 | 1.2.0 | no | 0 | 0 |  |
| `com.diffplug.durian:durian-core` | 1.2.0 | 1.2.0 | no | 0 | 0 |  |
| `com.diffplug.durian:durian-io` | 1.2.0 | 1.2.0 | no | 0 | 0 |  |
| `com.diffplug.durian:durian-swt.os` | 4.3.0 | 5.2.0 | yes | 0 | 0 |  |
| `com.diffplug.spotless:spotless-lib-extra` | 4.10.4 | 4.11.0 | yes | 0 | 0 |  |
| `com.diffplug.spotless:spotless-lib` | 4.10.4 | 4.11.0 | yes | 0 | 0 |  |
| `com.diffplug.spotless:spotless-plugin-gradle` | 8.10.4 | 8.11.0 | yes | 0 | 0 | Plugin `com.diffplug.spotless` (catalog `spotless = 8.10.4`). |
| `com.fasterxml.woodstox:woodstox-core` | 7.1.1 | 7.3.0 | yes | 0 | 0 |  |
| `com.google.code.findbugs:jsr305` | 3.0.2 | 3.0.2 | no | 0 | 0 |  |
| `com.googlecode.concurrent-trees:concurrent-trees` | 2.6.1 | 2.6.1 | no | 0 | 0 |  |
| `com.googlecode.javaewah:JavaEWAH` | 1.2.3 | 1.2.3 | no | 0 | 0 |  |
| `com.squareup.moshi:moshi-kotlin` | 1.15.2 | 1.15.2 | no | 0 | 0 |  |
| `com.squareup.moshi:moshi` | 1.15.2 | 1.15.2 | no | 0 | 0 |  |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | 5.5.1 | yes | 0 | 0 |  |
| `com.squareup.okio:okio-jvm` | 3.7.0 | 3.18.2 | yes | 0 | 0 |  |
| `com.squareup.okio:okio` | 3.7.0 | 3.18.2 | yes | 0 | 0 |  |
| `commons-codec:commons-codec` | 1.22.1 | 1.23.0 | yes | 0 | 0 |  |
| `commons-io:commons-io` | 2.22.0 | 2.22.0 | no | 0 | 0 |  |
| `dev.equo.ide:solstice` | 1.8.2 | 1.8.2 | no | 0 | 0 |  |
| `io.github.ben-manes:gradle-versions-plugin` | 0.66.0 | 0.67.0 | yes | 0 | 0 | Plugin `com.github.ben-manes.versions` 0.66.0. |
| `kr.motd.maven:os-maven-plugin` | 1.7.1 | 1.7.1 | no | 0 | 0 |  |
| `org.apache.ant:ant-launcher` | 1.10.17 | 1.10.18 | yes | 0 | 0 |  |
| `org.apache.ant:ant` | 1.10.17 | 1.10.18 | yes | 0 | 0 |  |
| `org.apache.logging.log4j:log4j-api` | 2.26.1 | 2.26.1 | no | 0 | 0 | Metadata `<release>` is the pre-release 3.0.0-beta2. |
| `org.apache.logging.log4j:log4j-core` | 2.26.1 | 2.26.1 | no | 0 | 0 | Held at 2.26.1 by the shadow plugin's dependency and a `require 2.17.1` constraint on the buildscript classpath. Metadata `<release>` is the pre-release 3.0.0-beta3. |
| `org.apache.maven:maven-api-annotations` | 4.0.0-rc-5 | None | unknown | 0 | 0 | No stable release is published (only release candidates), so no latest stable is recorded. |
| `org.apache.maven:maven-api-xml` | 4.0.0-rc-5 | None | unknown | 0 | 0 | No stable release is published (only release candidates), so no latest stable is recorded. |
| `org.apache.maven:maven-xml` | 4.0.0-rc-5 | None | unknown | 0 | 0 | No stable release is published (only release candidates), so no latest stable is recorded. |
| `org.codehaus.plexus:plexus-utils` | 4.0.3 | 4.1.0 | yes | 0 | 0 |  |
| `org.codehaus.plexus:plexus-xml` | 4.1.1 | 4.2.0 | yes | 0 | 0 |  |
| `org.codehaus.woodstox:stax2-api` | 4.2.2 | 4.3.2 | yes | 0 | 0 |  |
| `org.eclipse.jgit:org.eclipse.jgit` | 7.8.0.202609011348-r | 7.8.0.202609011348-r | no | 0 | 0 |  |
| `org.eclipse.platform:org.eclipse.osgi` | 3.24.300 | 3.24.300 | no | 0 | 0 |  |
| `org.jacoco:org.jacoco.agent` | 0.8.15 | 0.8.16 | yes | 0 | 0 | JaCoCo tool version `toolVersion = "0.8.15"` (the test JVM's agent). |
| `org.jacoco:org.jacoco.ant` | 0.8.15 | 0.8.16 | yes | 0 | 0 | JaCoCo tool version `toolVersion = "0.8.15"`. |
| `org.jacoco:org.jacoco.core` | 0.8.15 | 0.8.16 | yes | 0 | 0 | JaCoCo tool version `toolVersion = "0.8.15"` in `build.gradle.kts`. |
| `org.jacoco:org.jacoco.report` | 0.8.15 | 0.8.16 | yes | 0 | 0 | JaCoCo tool version `toolVersion = "0.8.15"`. |
| `org.jdom:jdom2` | 2.0.6.1 | 2.0.6.1 | no | 0 | 0 |  |
| `org.jetbrains:annotations` | 13.0 | 26.1.0 | yes | 0 | 0 |  |
| `org.jetbrains.kotlin:kotlin-metadata-jvm` | 2.4.10 | 2.4.21 | yes | 0 | 0 | Metadata `<release>` is the pre-release 2.5.0-Beta2. |
| `org.jetbrains.kotlin:kotlin-reflect` | 2.4.10 | 2.4.21 | yes | 0 | 0 | Metadata `<release>` is the pre-release 2.5.0-Beta2. |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk7` | 1.8.21 | 2.4.21 | yes | 0 | 0 | Metadata `<release>` is the pre-release 2.5.0-Beta2. |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk8` | 1.8.21 | 2.4.21 | yes | 0 | 0 | Metadata `<release>` is the pre-release 2.5.0-Beta2. |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.4.10 | 2.4.21 | yes | 0 | 0 | Metadata `<release>` is the pre-release 2.5.0-Beta2. |
| `org.slf4j:slf4j-api` | 2.0.18 | 2.0.20 | yes | 0 | 0 | Metadata `<release>` is the pre-release 2.1.0-alpha1. |
| `org.tukaani:xz` | 1.9 | 1.12 | yes | 0 | 0 |  |
| `org.vafer:jdependency` | 2.16 | 2.17 | yes | 0 | 0 |  |

| item | resolved | latest | stale | advisory source | note |
| --- | --- | --- | --- | --- | --- |
| Gradle wrapper | 9.8.2 (`gradle-wrapper.properties` `distributionUrl`) | 9.8.2 (`https://services.gradle.org/versions/current`) | no | no Maven advisory feed | Gradle itself is not a Maven artifact; the plugin classpath above is. |
| JDK compile toolchain | Java 17 (`java.toolchain.languageVersion`) | not queried | not assessed | no feed | The local toolchain registry holds IBM Semeru 17.0.9 builds (OpenJ9) and Homebrew OpenJDK 21.0.12.1; `tests.yaml` uses `actions/setup-java` with temurin 17. |
| JDK the tests run on | Java 17 (`defaultJdkVersion`; `-PjavaVersion` overrides) | not queried | not assessed | no feed | The Gradle daemon itself runs on Homebrew OpenJDK 21.0.12.1. |

## 5. Workflow actions

Every distinct `uses:` value across `.github/workflows/*` (29 call sites, 7 distinct values). GitHub advisories were read with `gh api "advisories?ecosystem=actions&affects=<owner>/<repo>@<version>&per_page=100"`; the actions ecosystem was proven live with a control (`tj-actions/changed-files@45.0.7` returned GHSA-mrrh-fwg8-r2c3). Workflow states are from `gh api repos/ne1r0n/clickhouse-kafka-connect/actions/workflows` (the repository also lists GitHub's own `dynamic/dependabot/dependabot-updates` workflow, which is not in the tree). Every fork-owned workflow pins every action to a full SHA with a version comment (`WorkflowActionPinGuardUnitTest` discovers them from the directory); no fork-owned workflow is byte-pinned. The one tag-pinned file, upstream's `integrationTests.yaml`, is byte-identical to upstream's copy and `UpstreamIntegrationWorkflowBytesGuardUnitTest` compares its raw bytes.

| action | called by | pin kind | exact version | latest release | stale | workflow state | guard test | GitHub advisories (exact version) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `actions/checkout` | tests, nightly-integration-tests, pre-release, release, increment-version | SHA `3d3c42e5aac5ba805825da76410c181273ba90b1` with comment `# v7.0.1`; tag v7.0.1 resolves to the same commit | v7.0.1 | v7.0.1 | no | active (all five) | `WorkflowActionPinGuardUnitTest` | 0 |
| `actions/setup-java` | tests, nightly-integration-tests, pre-release, release | SHA `b843ebf9b913146b38da2eaa7aeddf76074ae1ad` with comment `# v6.1.0`; tag v6.1.0 resolves to the same commit | v6.1.0 | v6.1.0 | no | active (all four) | `WorkflowActionPinGuardUnitTest` | 0 |
| `gradle/actions` (`setup-gradle`) | tests, nightly-integration-tests, pre-release, release | SHA `8da46b539cd8fe6ab27ca75128919a18d757604c` with comment `# v6.4.1`; the annotated tag v6.4.1 dereferences to the same commit | v6.4.1 | v6.4.1 | no | active (all four) | `WorkflowActionPinGuardUnitTest` | 0 |
| `actions/upload-artifact` | tests, nightly-integration-tests, pre-release, release | SHA `cf430e030ddbb5b0abf93d22962f4752f3646cd9` with comment `# v7.0.2`; tag v7.0.2 resolves to the same commit | v7.0.2 | v7.0.2 | no | active (all four) | `WorkflowActionPinGuardUnitTest` | 0 |
| `actions/checkout@v3` | integrationTests | tag (floating major) | v3.7.0 today (`a37ce9120846195fa4ece8f58b268e6043cb2f26`) | v7.0.1 | yes | disabled_manually | `UpstreamIntegrationWorkflowBytesGuardUnitTest` (raw bytes) | 0 |
| `actions/setup-java@v3` | integrationTests | tag (floating major) | v3.14.2 today (`e9fbacdec3bb3b6036605a3e6f7995d66773a8c6`) | v6.1.0 | yes | disabled_manually | `UpstreamIntegrationWorkflowBytesGuardUnitTest` (raw bytes) | 0 |
| `gradle/gradle-build-action@v2` | integrationTests | tag (floating major) | v2.12.0 today (`a8f75513eafdebd8141bd1cd4e30fcd194af8dfa`; the annotated tag v2 dereferences to it) | v3.5.0 (the repository is archived; its successor is `gradle/actions`) | yes | disabled_manually | `UpstreamIntegrationWorkflowBytesGuardUnitTest` (raw bytes) | 0 |

The three tag-pinned actions live only in `integrationTests.yaml`, which GitHub keeps disabled (`disabled_manually`); no active workflow has a tag-pinned action.

## 6. Test container images

The images the tests start, from `src/testFixtures`, `src/integrationTest` and `src/test`. These are not Maven artifacts and have no advisory feed in either database; the tag and what it resolves to are recorded, nothing more is claimed.

| image | where it is named | tag | stale | note |
| --- | --- | --- | --- | --- |
| `clickhouse/clickhouse-server` | `ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE` (`CLICKHOUSE_VERSION`, default `latest`) and `src/testFixtures/docker/clickhouse/cluster/docker-compose.yml` (`${CH_VERSION:-latest}`) | `latest` | not applicable | A floating tag; on 2026-11-04 it resolves to 26.10.3.7, digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (E-01). `src/test/resources/describe-fixtures/SERVER_VERSION.txt` records the image the DESCRIBE fixtures were captured from (26.9.19.9). |
| `ghcr.io/shopify/toxiproxy` | `ClickHouseTestHelpers.TOXIPROXY_DOCKER_IMAGE_NAME` | `2.7.0` | yes (the repository's latest release is v2.13.0) | Used with the Toxiproxy test container. |
| `confluentinc/cp-zookeeper`, `cp-server`, `cp-schema-registry`, `cp-server-connect`, `cp-kafka-rest`, `cp-enterprise-control-center`, `cp-ksqldb-server` | `src/integrationTest/.../ConfluentPlatform.java` (`CONFLUENT_VERSION`) | `7.9.6` | not assessed | Seven images at one tag; integration tests only. The newest 7.9.x Maven build of the Confluent platform libraries is 7.9.9. |
| `testcontainers/ryuk` | started implicitly by Testcontainers 2.0.5 | `0.14.0` (the local image tag) | not assessed | The library's own resource reaper; not named in the repository. |
| `confluentinc/cp-kafka-connect-base` | `src/test/resources/connect-api-references.txt` (a provenance comment) | `7.9.7`, digest-pinned | not applicable | Not started by any test; the file records which image the Connect API reference jars were extracted from. |

## 7. DEP-02 proposals and decisions

**Rule (U-4):** no bump without an open advisory, and each bump is the user's decision in the user's own words. Nothing was changed by this plan so far: `build.gradle.kts`, `gradle/` and `.github/` are byte-unchanged since the merge commit M.

**Stale, no advisory: not proposed (U-4).** Every row marked `stale: yes` in sections 1 to 5 that is not one of the proposals below (the shipped list is in section 1's table and in E-16; the rest are in the tables above) is recorded and left alone.

### Open advisories: 16 proposals (all test-scope, none ships)

On 2026-11-04 16 coordinates resolved on the test classpaths carry at least one open advisory in both sources (the two sources agree on every one, row by row). All come from the test and test-fixture classpaths and all trace to Confluent platform libraries at 7.9.1 (`io.confluent:kafka-schema-registry`, `rest-utils`, `kafka-protobuf-*`) or to Kafka's `kafka-clients`; none is one of the 27 coordinates of section 1, so the shipped archive is not affected and no proposal changes `confluent-archive-allowlist-v1.7.0_gg8.txt`. The shipped set, the Gradle plugins, the JaCoCo tools and the seven workflow actions have no open advisory. Because most rows are transitive, the mechanism for a bump (a version constraint in `build.gradle.kts`, or a bump of the owning catalog entry `kafkaPlatformSchemaRegistry` or `kafkaPlatformProtobuf`) is part of what the user decides; the rows are listed per coordinate as the plan requires, and a decision may cover several rows at once.

#### P1. `ch.qos.logback:logback-core` 1.2.13

- **Scope and shipping:** On testRuntimeClasspath. Pulled by logback-classic 1.2.13 <- zookeeper 3.8.4 <- io.confluent:kafka-schema-registry 7.9.1 and kafka_2.13 7.9.1-ccs. It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (6, highest severity medium):** GHSA-25qh-j22f-pwp8 (CVE-2025-11226, medium); GHSA-6v67-2wr5-gvf4 (CVE-2024-12801, low); GHSA-jhq6-gfmj-v8fx (CVE-2026-10532, low); GHSA-p47f-322f-whfh (CVE-2026-9828, low); GHSA-pr98-23f8-jwxv (CVE-2024-12798, medium); GHSA-qqpg-mvqg-649v (CVE-2026-1225, low).
- **Summaries:** GHSA-25qh-j22f-pwp8: QOS.CH logback-core is vulnerable to Arbitrary Code Execution through file processing | GHSA-6v67-2wr5-gvf4: QOS.CH logback-core Server-Side Request Forgery vulnerability | GHSA-jhq6-gfmj-v8fx: Logback vulnerable to Object Injection through HardenedObjectInputStream modules | GHSA-p47f-322f-whfh: QOS.CH Sarl logback logback-core has a deserialization of untrusted data vulnerability | GHSA-pr98-23f8-jwxv: QOS.CH logback-core Expression Language Injection vulnerability | GHSA-qqpg-mvqg-649v: Logback allows an attacker to instantiate classes already present on the class path.
- **Newest stable:** 1.6.6. **Lowest version that clears every listed advisory:** 1.5.34 (same major, different minor line (1.2 to 1.5); no 1.2.x release is listed as fixed, the listed fixes are 1.3.15, 1.3.16 and 1.5.13 to 1.5.34).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

#### P2. `com.squareup.wire:wire-runtime-jvm` 5.1.0

- **Scope and shipping:** On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. Pulled by wire-schema-jvm 5.1.0 <- io.confluent:kafka-connect-protobuf-converter 7.9.1 and kafka-protobuf-provider 7.9.1. It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (1, highest severity high):** GHSA-7xpr-hc2w-34m9 (CVE-2026-45799, high).
- **Summaries:** GHSA-7xpr-hc2w-34m9: Wire: skipGroup() missing negative-length check allows 10-byte payload to crash any Wire-decoding service .
- **Newest stable:** 7.1.2. **Lowest version that clears every listed advisory:** 6.3.0 (different major line (5 to 6)).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

#### P3. `com.squareup.wire:wire-runtime` 5.1.0

- **Scope and shipping:** On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. Pulled by io.confluent:kafka-connect-protobuf-converter 7.9.1 and kafka-protobuf-provider 7.9.1 (catalog `kafkaPlatformProtobuf` and `kafkaPlatformSchemaRegistry`). It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (3, highest severity high):** GHSA-7xpr-hc2w-34m9 (CVE-2026-45799, high); GHSA-9rm7-3qhh-h2mc (CVE-2026-63126, high); GHSA-pwf9-q62p-v7wc (CVE-2024-58103, medium).
- **Summaries:** GHSA-7xpr-hc2w-34m9: Wire: skipGroup() missing negative-length check allows 10-byte payload to crash any Wire-decoding service  | GHSA-9rm7-3qhh-h2mc: Wire: Unauthenticated decoder crash via 32-bit length integer overflow in ByteArrayProtoReader32 (incomplete fix of CVE-2026-45799) | GHSA-pwf9-q62p-v7wc: Wire has Uncontrolled Recursion on Nested Groups.
- **Newest stable:** 7.1.2. **Lowest version that clears every listed advisory:** 6.4.5 (different major line (5 to 6); 5.2.0 fixes only GHSA-pwf9-q62p-v7wc, the other two need 6.3.0 and 6.4.5).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

#### P4. `commons-beanutils:commons-beanutils` 1.9.4

- **Scope and shipping:** On testRuntimeClasspath. Pulled (commons-validator 1.7 requests it) by kafka_2.13 7.9.1-ccs, everit-json-schema 1.14.3 and json-sKema 0.19.0 <- io.confluent:kafka-json-schema-provider 7.9.1. It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (1, highest severity high):** GHSA-wxr5-93ph-8wr9 (CVE-2025-48734, high).
- **Summaries:** GHSA-wxr5-93ph-8wr9: Apache Commons Improper Access Control vulnerability.
- **Newest stable:** 1.11.0. **Lowest version that clears every listed advisory:** 1.11.0 (same major line (1.9 to 1.11)).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

#### P5. `io.netty:netty-codec` 4.1.115.Final

- **Scope and shipping:** On testRuntimeClasspath. Pulled by io.confluent:kafka-schema-registry 7.9.1 (4.1.115.Final, lifted from 4.1.105.Final requested by zookeeper 3.8.4). It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (3, highest severity high):** GHSA-3p8m-j85q-pgmj (CVE-2025-58057, medium); GHSA-558v-64gr-wgg4 (CVE-2026-59901, high); GHSA-mj4r-2hfc-f8p6 (CVE-2026-42583, high).
- **Summaries:** GHSA-3p8m-j85q-pgmj: Netty's decoders vulnerable to DoS via zip bomb style attack | GHSA-558v-64gr-wgg4: Netty: [Bzip2Decoder] Infinite Loop in RLE State Machine Leads to Event-Loop Thread Hang | GHSA-mj4r-2hfc-f8p6: Netty Lz4FrameDecoder is vulnerable to resource exhaustion .
- **Newest stable:** 4.2.20.Final. **Lowest version that clears every listed advisory:** 4.1.136.Final (same 4.1 line).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

#### P6. `io.netty:netty-common` 4.1.115.Final

- **Scope and shipping:** On testRuntimeClasspath. Lifted to 4.1.115.Final by netty-buffer/netty-codec/netty-transport requests from zookeeper 3.8.4 and io.confluent:kafka-schema-registry 7.9.1. It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (1, highest severity medium):** GHSA-389x-839f-4rhx (CVE-2025-25193, medium).
- **Summaries:** GHSA-389x-839f-4rhx: Denial of Service attack on windows app using Netty.
- **Newest stable:** 4.2.20.Final. **Lowest version that clears every listed advisory:** 4.1.118.Final (same 4.1 line).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

#### P7. `io.netty:netty-handler` 4.1.105.Final

- **Scope and shipping:** On testRuntimeClasspath. Pulled by zookeeper 3.8.4 and kafka_2.13 7.9.1-ccs <- io.confluent:kafka-schema-registry 7.9.1. It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (6, highest severity critical):** GHSA-3qp7-7mw8-wx86 (CVE-2026-44249, high); GHSA-4g8c-wm8x-jfhw (CVE-2025-24970, high); GHSA-c4c3-7fpv-j4q5 (CVE-2026-75595, critical); GHSA-c653-97m9-rcg9 (CVE-2026-50010, high); GHSA-fccg-mwvh-qqg4 (CVE-2026-75596, medium); GHSA-x4gw-5cx5-pgmh (CVE-2026-45416, high).
- **Summaries:** GHSA-3qp7-7mw8-wx86: Netty has an IPv6 Subnet Filter Bypass via Incorrect Comparator Masking | GHSA-4g8c-wm8x-jfhw: SslHandler doesn't correctly validate packets which can lead to native crash when using native SSLEngine | GHSA-c4c3-7fpv-j4q5: Netty: SNI Routing Bypass via Fragmented TLS ClientHello Causing Fallback to Default SslContext | GHSA-c653-97m9-rcg9: Netty: Wrapping plain trust manager silently disables hostname verification | GHSA-fccg-mwvh-qqg4: Netty: Fragmented ClientHello records trigger quadratic pre-handshake reassembly in default SNI parsing | GHSA-x4gw-5cx5-pgmh: Netty: SNI handler pre-allocates up to 16 MiB from nine attacker bytes.
- **Newest stable:** 4.2.20.Final. **Lowest version that clears every listed advisory:** 4.1.137.Final (same 4.1 line).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

#### P8. `io.netty:netty-transport-native-epoll` 4.1.105.Final

- **Scope and shipping:** On testRuntimeClasspath. Pulled by zookeeper 3.8.4 <- io.confluent:kafka-schema-registry 7.9.1. It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (1, highest severity medium):** GHSA-w573-9ffj-6ff9 (CVE-2026-45536, medium).
- **Summaries:** GHSA-w573-9ffj-6ff9: Netty: Unix-socket fd receive leaks descriptors when peer sends two at once.
- **Newest stable:** 4.2.20.Final. **Lowest version that clears every listed advisory:** 4.1.135.Final (same 4.1 line).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

#### P9. `org.apache.mina:mina-core` 2.2.3

- **Scope and shipping:** On testRuntimeClasspath. Pulled by org.eclipse.jetty:jetty-jaas 9.4.57.v20241219 <- io.confluent:rest-utils 7.9.1 <- kafka-schema-registry 7.9.1. It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (6, highest severity critical):** GHSA-76h9-2vwh-w278 (CVE-2024-52046, critical); GHSA-8297-v2rf-2p32 (CVE-2026-41635, critical); GHSA-995c-6rp3-4m4x (CVE-2026-42778, critical); GHSA-f2wh-grmh-r6jm (CVE-2026-41409, critical); GHSA-v3pr-hxpr-mfm8 (CVE-2026-47065, critical); GHSA-vf5j-865m-mq7c (CVE-2026-42779, critical).
- **Summaries:** GHSA-76h9-2vwh-w278: Apache MINA Deserialization RCE Vulnerability | GHSA-8297-v2rf-2p32: Apache MINA vulnerable to Deserialization of Untrusted Data | GHSA-995c-6rp3-4m4x: Apache MINA vulnerable to Deserialization of Untrusted Data (CVE-2026-41409 Incomplete Fix) | GHSA-f2wh-grmh-r6jm: Apache MINA Vulnerable to Deserialization of Untrusted Data (CVE-2024-52046 Incomplete Fix) | GHSA-v3pr-hxpr-mfm8: Apache MINA: Critical Deserialization Allow-list Bypass via resolveProxyClass | GHSA-vf5j-865m-mq7c: Apache MINA vulnerable to Deserialization of Untrusted Data (CVE-2026-41635 Incomplete Fix).
- **Newest stable:** 2.2.10. **Lowest version that clears every listed advisory:** 2.2.8 (same 2.2 line).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

#### P10. `org.apache.zookeeper:zookeeper` 3.8.4

- **Scope and shipping:** On testRuntimeClasspath. Pulled by io.confluent:kafka-schema-registry 7.9.1 and kafka_2.13 7.9.1-ccs. It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (2, highest severity high):** GHSA-7xrh-hqfc-g7qr (CVE-2026-24281, high); GHSA-crhr-qqj8-rpxc (CVE-2026-24308, high).
- **Summaries:** GHSA-7xrh-hqfc-g7qr: Apache ZooKeeper: Reverse-DNS fallback enables hostname verification bypass in ZooKeeper ZKTrustManager | GHSA-crhr-qqj8-rpxc: Apache ZooKeeper has improper handling of configuration values.
- **Newest stable:** 3.9.7. **Lowest version that clears every listed advisory:** 3.8.6 (same 3.8 line).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

#### P11. `org.bitbucket.b_c:jose4j` 0.9.4

- **Scope and shipping:** On testRuntimeClasspath. Pulled by io.confluent:kafka-schema-registry 7.9.1. It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (1, highest severity high):** GHSA-3677-xxcr-wjqv (CVE-2024-29371, high).
- **Summaries:** GHSA-3677-xxcr-wjqv: jose4j is vulnerable to DoS via compressed JWE content.
- **Newest stable:** 0.9.7. **Lowest version that clears every listed advisory:** 0.9.6 (same 0.9 line).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

#### P12. `org.eclipse.jetty.http2:http2-common` 9.4.57.v20241219

- **Scope and shipping:** On testRuntimeClasspath. Pulled by io.confluent:rest-utils 7.9.1 <- kafka-schema-registry 7.9.1. It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (1, highest severity high):** GHSA-mmxm-8w33-wc4h (CVE-2025-5115, high).
- **Summaries:** GHSA-mmxm-8w33-wc4h: Eclipse Jetty affected by MadeYouReset HTTP/2 vulnerability.
- **Newest stable:** 11.0.26. **Lowest version that clears every listed advisory:** 9.4.58 (same 9.4 line).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

#### P13. `org.eclipse.jetty:jetty-http` 9.4.57.v20241219

- **Scope and shipping:** On testRuntimeClasspath. Pulled by jetty websocket and server modules <- io.confluent:rest-utils 7.9.1 <- kafka-schema-registry 7.9.1. It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (3, highest severity high):** GHSA-355h-qmc2-wpwf (CVE-2026-2332, high); GHSA-qh8g-58pp-2wxh (CVE-2024-6763, medium); GHSA-wjpw-4j6x-6rwh (CVE-2025-11143, low).
- **Summaries:** GHSA-355h-qmc2-wpwf: Jetty has HTTP Request Smuggling via Chunked Extension Quoted-String Parsing | GHSA-qh8g-58pp-2wxh: Eclipse Jetty URI parsing of invalid authority | GHSA-wjpw-4j6x-6rwh: org.eclipse.jetty:jetty-http has different parsing of invalid URIs.
- **Newest stable:** 12.1.14. **Lowest version that clears every listed advisory:** 12.0.33 (different major line (9 to 12); on the 9.4 line only GHSA-355h-qmc2-wpwf has a listed fix (9.4.60), while GHSA-qh8g-58pp-2wxh and GHSA-wjpw-4j6x-6rwh list none).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

#### P14. `org.eclipse.jetty:jetty-security` 9.4.57.v20241219

- **Scope and shipping:** On testRuntimeClasspath. Pulled by io.confluent:rest-utils 7.9.1 <- kafka-schema-registry 7.9.1. It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (1, highest severity high):** GHSA-2fvj-hgj9-j2gr (CVE-2026-10050, high).
- **Summaries:** GHSA-2fvj-hgj9-j2gr: Eclipse Jetty Digest Authentication: ISO-8859-1 lossy encoding allows authentication bypass via character substitution.
- **Newest stable:** 12.1.14. **Lowest version that clears every listed advisory:** 9.4.63 (same 9.4 line).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

#### P15. `org.eclipse.jetty:jetty-server` 9.4.57.v20241219

- **Scope and shipping:** On testRuntimeClasspath. Pulled by io.confluent:rest-utils 7.9.1 and kafka-schema-registry 7.9.1. It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (1, highest severity medium):** GHSA-7p3p-8qv8-m2vh (CVE-2026-6790, medium).
- **Summaries:** GHSA-7p3p-8qv8-m2vh: Eclipse Jetty: HTTP Authority/Host mismatch.
- **Newest stable:** 12.1.14. **Lowest version that clears every listed advisory:** 12.0.35 (different major line (9 to 12); GHSA-7p3p-8qv8-m2vh lists no fixed 9.4 release).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

#### P16. `org.xerial.snappy:snappy-java` 1.1.10.5

- **Scope and shipping:** On testRuntimeClasspath, testFixturesRuntimeClasspath, integrationTestRuntimeClasspath. Pulled by org.apache.kafka:kafka-clients 4.4.0 (through connect-api), io.confluent:kafka-schema-registry 7.9.1 and kafka-schema-registry-client 7.9.1. It is not one of the 27 shipped coordinates of section 1, so it does not reach the Confluent archive.
- **Advisories (3, highest severity high):** GHSA-gwq9-9ffj-mvrv (CVE-2026-93451, high); GHSA-q233-g92q-qxp6 (CVE-2026-108106, high); GHSA-xqrx-qh46-34m7 (CVE-2026-116590, medium).
- **Summaries:** GHSA-gwq9-9ffj-mvrv: snappy-java: Heap buffer overflow in Snappy.uncompress*Array typed-array methods | GHSA-q233-g92q-qxp6: snappy-java: Unbounded memory allocation from attacker-declared uncompressed lengtha | GHSA-xqrx-qh46-34m7: snappy-java: JVM crash from unchecked ranges in Snappy.arrayCopy and SnappyOutputStream.rawWrite.
- **Newest stable:** 1.1.10.12. **Lowest version that clears every listed advisory:** 1.1.10.9 (same 1.1.10 line).
- **Phase 49 consequence:** none for the shipped archive: the coordinate is not packed into it, so the archive's jar set and `confluent-archive-allowlist-v1.7.0_gg8.txt` do not change.
- **Decision:** declined (record only, backlog); nothing changed.

### Inconclusive: Confluent `-ccs` Kafka artifacts

The 12 `org.apache.kafka` artifacts at `7.9.1-ccs` in section 2 return 0 for the exact version, but the version is Confluent's and not comparable to Apache's advisories (see the note under the section 2 table). The Apache lines 3.9.0 and 3.9.1 of `kafka-clients` and 3.9.0 of `kafka_2.13` do have advisories fixed in 3.9.1 and 3.9.2. This is raised for the user's information; it is not counted among the 16 proposals.

### Decisions

Recorded 2026-11-04. These answers reached this plan through the coordinator, which relayed them as the user's AskUserQuestion answers; the exact wording is not available here, so they are recorded in spirit, not verbatim. No bump was proposed for approval by an agent and none was approved.

1. **P1 to P16: record only.** All sixteen are declined; nothing is bumped. The list stays in this file as backlog for a later maintenance phase. None of the sixteen ships: they resolve only on the test and test-fixture classpaths and are not among the 27 shipped coordinates of section 1.
2. **The 12 Confluent `7.9.1-ccs` Kafka artifacts: note and do not chase.** What Apache release a `7.9.1-ccs` build corresponds to is an open unknown: the databases index Apache version numbers, so the exact-version answer 0 does not show that these builds are clean, and Apache's 3.9.0 and 3.9.1 lines carry advisories. No further investigation was done.

**Disposition:** `dep02_proposals: 16`, none approved, `dep02_bump_commits: 0`, `build.gradle.kts`, `gradle/` and `.github/` byte-unchanged since the merge commit M.
