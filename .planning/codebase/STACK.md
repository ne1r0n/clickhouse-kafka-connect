---
last_mapped_commit: 22f055c712de378404f73cffe198b641002ed248
last_mapped_at: 2026-09-15
---
# Technology Stack

**Analysis Date:** 2026-09-15

## Languages

**Primary:**

- Java 17 - Main implementation language for Kafka Connect connector and transforms
- Kotlin DSL - Build configuration (`build.gradle.kts`, `settings.gradle.kts`)

## Runtime

**Environment:**

- JDK 17 (configured via Gradle toolchain)
- Gradle 7.4.2 (build orchestration)

**Package Manager:**

- Gradle 7.4.2 - Dependency resolution and build management
- Lockfile: Gradle dependency lock files not explicitly configured (Gradle cache manages versions)

## Frameworks

**Core:**

- Apache Kafka Connect API 4.1.1 - Sink connector framework and transforms
- ClickHouse Client 0.9.6 - Database driver with HTTP and JDBC variants
  - `com.clickhouse:clickhouse-client:0.9.6`
  - `com.clickhouse:clickhouse-http-client:0.9.6`
  - `com.clickhouse:clickhouse-data:0.9.6`
  - `com.clickhouse:client-v2:0.9.6` (newer client variant)
  - `com.clickhouse:clickhouse-jdbc:0.9.6` (JDBC driver)

**Testing:**

- JUnit 5 (Jupiter) 5.11.3 - Unit test framework
- JUnit Platform 1.14.2 - Test execution platform
- Mockito 5.21.0 - Mocking framework
- Hamcrest 3.0 - Assertion library
- TestContainers 1.21.4 - Integration testing with containerized services
  - `org.testcontainers:clickhouse:1.21.4` (ClickHouse container)
  - `org.testcontainers:kafka:1.21.4` (Kafka container)
  - `org.testcontainers:toxiproxy:1.21.4` (Network proxy for chaos testing)

**Build/Dev:**

- Spotless 8.1.0 - Code formatting (GoogleJavaFormat, import organization)
- Gradle Shadow Plugin 8.1.1 - Creating fat JARs for distribution
- Ben-Manes Gradle Versions Plugin 0.53.0 - Dependency update tracking
- Gradle BuildConfig Plugin 6.0.7 - Build-time configuration generation

## Key Dependencies

**Critical:**

- Jackson Core 2.21.0 - JSON parsing and serialization
  - `com.fasterxml.jackson.core:jackson-core:2.21.0`
  - `com.fasterxml.jackson.core:jackson-databind:2.21.0`
  - `com.fasterxml.jackson.core:jackson-annotations:2.21`
- Google Gson 2.13.2 - Lightweight JSON library (alternative JSON support)
- Apache HTTPComponents Client5 5.6 - HTTP client (used by ClickHouse connector)

**Infrastructure:**

- Project Lombok 1.18.42 - Boilerplate reduction (getters, setters, builders)
- SLF4J with Reload4j 2.0.17 - Logging abstraction and implementation
- OkHttp3 5.3.2 - HTTP client (used in integration tests)
- Apache Kafka API 4.1.1 - Consumer/Producer protocol support
- JSON library 20251224 - JSON parsing in tests
- LZ4 compression 1.10.2 (at.yawk variant) - Data compression for ClickHouse

## Configuration

**Environment:**

- Hostname, port, database, username, password for ClickHouse connection
- Proxy configuration (type, host, port)
- ZooKeeper/Keeper configuration for distributed state
- SSL/TLS support for ClickHouse connections
- JDBC connection properties for advanced tuning

**Build:**

- `build.gradle.kts` - Gradle build configuration with Kotlin DSL
- `settings.gradle.kts` - Project structure definition
- `VERSION` - Version string (currently `v1.2.9_gg4`)
- Maven repositories:
  - Maven Central (`mavenCentral()`)
  - Confluent Maven (`https://packages.confluent.io/maven/`)
  - JitPack (`https://jitpack.io`)
  - Sonatype Snapshots (`https://s01.oss.sonatype.org/content/repositories/snapshots/`)

## Platform Requirements

**Development:**

- JDK 17+ (toolchain enforced in build.gradle.kts)
- Gradle 7.4.2+
- Optional: IDE support for Kotlin DSL (IntelliJ IDEA)

**Production:**

- ClickHouse v22.5+ (as per manifest.json requirements)
- Apache Kafka 4.1.1 compatible broker
- JVM with JDK 17+ runtime
- Confluent Kafka Connect cluster or standalone worker
- Sufficient network connectivity to ClickHouse (HTTP port 8443 default)
- For exactly-once semantics: ZooKeeper or ClickHouse Keeper cluster

---

*Stack analysis: 2026-09-15*
