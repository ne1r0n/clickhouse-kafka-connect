<!-- GSD:project-start source:PROJECT.md -->

## Project

**ClickHouse Kafka Connect — `custom_build` Fork Test Coverage**

The `custom_build` branch of `ne1r0n/clickhouse-kafka-connect` (version `v1.2.9_gg4`) is a fork of the official ClickHouse Kafka Connect sink, carrying custom SMTs, table filtering, JSON/Dynamic column support, a metadata-free JSON insert path, and JMX naming changes used in CDC (Debezium) pipelines into ClickHouse. This project audits the existing tests for everything the fork added on top of upstream, writes the missing unit and integration tests, and fixes any bugs those tests uncover — ending with a fully green suite, a coverage report, and CI running it.

**Core Value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.

### Constraints

- **Test target**: ClickHouse `latest` + client V2 only — matches production connectors; older versions lack JSON/Dynamic.
- **Tooling**: Reuse existing JUnit/Mockito/Testcontainers setup, `ClickHouseBase`, and test helpers; only new build dependency expected is the JaCoCo Gradle plugin — keeps the fork close to upstream conventions.
- **Behavior preservation**: Tests pin current fork behavior (including WIP JSON path and not forcing `async_insert`); code changes are limited to genuine bug fixes — the fork runs in production.
- **Environment**: Docker (colima) must be running for ClickHouse-backed tests locally and available on CI runners.
- **Rebasability**: Keep changes additive (new test files/cases, small targeted fixes) to minimize conflicts with future upstream syncs.

<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->

## Technology Stack

## Languages

- Java 17 - Main implementation language for Kafka Connect connector and transforms
- Kotlin DSL - Build configuration (`build.gradle.kts`, `settings.gradle.kts`)

## Runtime

- JDK 17 (configured via Gradle toolchain)
- Gradle 7.4.2 (build orchestration)
- Gradle 7.4.2 - Dependency resolution and build management
- Lockfile: Gradle dependency lock files not explicitly configured (Gradle cache manages versions)

## Frameworks

- Apache Kafka Connect API 4.1.1 - Sink connector framework and transforms
- ClickHouse Client 0.9.6 - Database driver with HTTP and JDBC variants
- JUnit 5 (Jupiter) 5.11.3 - Unit test framework
- JUnit Platform 1.14.2 - Test execution platform
- Mockito 5.21.0 - Mocking framework
- Hamcrest 3.0 - Assertion library
- TestContainers 1.21.4 - Integration testing with containerized services
- Spotless 8.1.0 - Code formatting (GoogleJavaFormat, import organization)
- Gradle Shadow Plugin 8.1.1 - Creating fat JARs for distribution
- Ben-Manes Gradle Versions Plugin 0.53.0 - Dependency update tracking
- Gradle BuildConfig Plugin 6.0.7 - Build-time configuration generation

## Key Dependencies

- Jackson Core 2.21.0 - JSON parsing and serialization
- Google Gson 2.13.2 - Lightweight JSON library (alternative JSON support)
- Apache HTTPComponents Client5 5.6 - HTTP client (used by ClickHouse connector)
- Project Lombok 1.18.42 - Boilerplate reduction (getters, setters, builders)
- SLF4J with Reload4j 2.0.17 - Logging abstraction and implementation
- OkHttp3 5.3.2 - HTTP client (used in integration tests)
- Apache Kafka API 4.1.1 - Consumer/Producer protocol support
- JSON library 20251224 - JSON parsing in tests
- LZ4 compression 1.10.2 (at.yawk variant) - Data compression for ClickHouse

## Configuration

- Hostname, port, database, username, password for ClickHouse connection
- Proxy configuration (type, host, port)
- ZooKeeper/Keeper configuration for distributed state
- SSL/TLS support for ClickHouse connections
- JDBC connection properties for advanced tuning
- `build.gradle.kts` - Gradle build configuration with Kotlin DSL
- `settings.gradle.kts` - Project structure definition
- `VERSION` - Version string (currently `v1.2.9_gg4`)
- Maven repositories:

## Platform Requirements

- JDK 17+ (toolchain enforced in build.gradle.kts)
- Gradle 7.4.2+
- Optional: IDE support for Kotlin DSL (IntelliJ IDEA)
- ClickHouse v22.5+ (as per manifest.json requirements)
- Apache Kafka 4.1.1 compatible broker
- JVM with JDK 17+ runtime
- Confluent Kafka Connect cluster or standalone worker
- Sufficient network connectivity to ClickHouse (HTTP port 8443 default)
- For exactly-once semantics: ZooKeeper or ClickHouse Keeper cluster

<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->

## Conventions

## Naming Patterns

- Class files use PascalCase: `ClickHouseSinkConnector.java`, `ClickHouseSinkTask.java`
- Test files suffix with `Test`: `ClickHouseSinkTaskTest.java`, `ConvertToMapOrJsonTest.java`
- Abstract base classes: `RecordConvertor.java`, `ClickHouseBase.java`
- Configuration/config classes: `ClickHouseSinkConfig.java`, `HeaderToValueConfig.java`
- Helper/utility classes: `Utils.java`, `Mask.java`, `QueryIdentifier.java`
- PascalCase: `ClickHouseSinkConnector`, `ProxySinkTask`, `HeadersToValue`
- Inner classes use descriptive names with their purpose: `ConfigDef.Validator` implementations like `UTF8String`, `ZKPathValidator`, `InsertFormatValidatorAndRecommender`
- Enum classes defined inline: `InsertFormats` with values `NONE`, `CSV`, `TSV`, `JSON`
- camelCase: `getRootCause()`, `handleException()`, `getTableName()`, `createDatabase()`
- Overridden methods: `start()`, `put()`, `stop()`, `apply()` follow Kafka Connect interface conventions
- Static factory methods: `createClient()`, `devNullErrorReporter()`
- Getters use `get` prefix: `getHostname()`, `getPort()`, `getDatabase()`
- Predicates/boolean methods: `isSslEnabled()`, `isCloud()`, `isErrorsTolerance()`, `isUseClientV2()`
- camelCase for local and instance variables: `hostname`, `database`, `username`, `sinkRecord`, `errorReporter`
- Private fields with descriptive names: `proxySinkTask`, `clickHouseSinkConfig`, `errorReporter`
- Builder pattern parameters: `hostname`, `port`, `database`, `username`, `password`, `sslEnabled`
- CONSTANT_CASE for public static final strings: `HOSTNAME`, `PORT`, `DATABASE`, `TABLE_MAPPING`, `ERRORS_TOLERANCE`
- Enum constant values: `RELEASE`, `FINAL`, `GA` (from dependency version checking)
- Magic numbers extracted: `MILLI_IN_A_SEC = 1000`, `DEFAULT_TOTAL_RECORDS = 1000`
- Standard reverse-domain: `com.clickhouse.kafka.connect`
- Layered by function: `.sink`, `.sink.db`, `.sink.db.helper`, `.sink.data`, `.sink.data.convert`, `.sink.dedup`, `.sink.dlq`, `.transforms`, `.util`
- Test packages mirror source: `src/test/java` mirrors `src/main/java` structure
- Separate integration test source set: `src/integrationTest/java`

## Code Style

- Tool used: Spotless (gradle plugin version 8.1.0) - see `build.gradle.kts` lines 36, 280-286
- Import organization: `spotless { java { importOrder() removeUnusedImports() } }`
- Manual formatting task: `gradle removeUnusedImports` task (line 288-292)
- Java compiler warnings enforced via `-Xlint:unchecked` and `-Xlint:deprecation` (line 168)
- Java version: JDK 17 (see `java.toolchain.languageVersion = 17` at line 17)
- 4-space indentation (standard Java)
- Opening braces on same line (Java standard)
- No explicit enforced limit observed, but generally kept reasonable (~120 chars)

## Import Organization

- No aliases observed; fully qualified names used throughout

## Error Handling

### Exception Wrapping Strategy

- Custom logic in `com.clickhouse.kafka.connect.util.Utils.handleException()` (`src/main/java/com/clickhouse/kafka/connect/util/Utils.java`)
- Distinguishes between retriable and non-retriable errors based on ClickHouse error codes (lines 63-127)

### Exception Hierarchy

- `RetriableException` thrown for network/timeout/transient errors:
- `DataException` thrown for schema/data validation errors when errors tolerance disabled
- `RuntimeException` thrown for non-retriable application errors (wraps root cause)

### Root Cause Extraction

### DLQ (Dead Letter Queue) Pattern

- Errors sent to DLQ when errors tolerance enabled: `Utils.sendTODlq(errorReporter, record, exception)`
- See `ClickHouseSinkTask.put()` lines 54-61

### Try-Catch Usage

- Broad catch blocks wrapping business logic, then delegated to Utils.handleException
- TestContainers setup wrapped in try-with-resources (ClickHouseSinkTaskTest lines 90-96)

## Logging

- `LOGGER.info()` - High-level operations: "Starting SinkConnect...", "Setting up connector..."
- `LOGGER.warn()` - Warnings and error decisions: "Deciding how to handle exception...", "Sending records to DLQ..."
- `LOGGER.error()` - Errors: "Error dropping database", "Errors tolerance is disabled..."
- `LOGGER.debug()` - Debug info: "Topic name: X, Table Name: Y", username addition
- `LOGGER.trace()` - Low-level trace: "Found exception:", "Passing the exception to the exception handler"

## Comments

- Complex exception handling logic: "Will drill down to the first ClickHouseException in the exception chain" (Utils.java line 36-38)
- Clarify non-obvious code: Comments on protocol construction, error code switching
- TODO/FIXME markers: "// TODO: need to remove ???" (build.gradle.kts line 102), "// TODO: can be removed ss" (ClickHouseSinkTask.java line 66)
- Design decisions: "// We have to use Throwable because of the getCause() signature" (Utils.java line 46)
- Minimal usage; not a primary convention in this codebase
- Where used, describes the "why": 

## Function Design

- Constructor injection preferred: `ClickHouseHelperClient(ClickHouseClientBuilder builder)`
- Maps for configuration: `Map<String, String> props`
- Use of builder pattern for complex object construction: `ClickHouseClientBuilder`
- Void for side-effect operations: `put()`, `start()`, `stop()`
- Objects for queries: `getTableName()` returns `String`
- Collections for batch operations: `taskConfigs()` returns `List<Map<String, String>>`
- Boolean for predicates: `isSslEnabled()`, `isCloud()`
- Grouping in `Utils` class for shared logic: `escapeName()`, `getRootCause()`, `handleException()`, `getTableName()`
- No instance state for utility methods

## Module Design

- Kafka Connect API: `ClickHouseSinkConnector` extends `SinkConnector`, `ClickHouseSinkTask` extends `SinkTask`
- Public configuration classes: `ClickHouseSinkConfig`, `HeaderToValueConfig`
- Transforms: `HeadersToValue`, `HeaderToValue`, `ConvertToMapOrJson`, `TableOperationFilter`, `ExtractTopic`
- Not used; imports are explicit and fully qualified
- Used for complex object construction: `ClickHouseHelperClient.ClickHouseClientBuilder`
- Fluent API: `.setDatabase()`, `.setUsername()`, `.setPassword()`, `.sslEnable()`, `.setTimeout()`, `.useClientV2()`
- Abstract base class for converters: `RecordConvertor` with abstract `doConvert()` method
- Test base class for setup/teardown: `ClickHouseBase` with static `@BeforeAll setup()` and `@AfterAll tearDown()`
- Kafka Connect framework classes extended: `SinkConnector`, `SinkTask`, `SingleMessageTransforms`
- `@Getter` for auto-generated getters: `@Getter private final String database;`
- Minimal use; not pervasive throughout codebase
- Located at line 19 in `ClickHouseSinkConfig`, lines 44, 50, 52, 58 in `ClickHouseHelperClient`

## Configuration Management

- String-based configuration maps: `Map<String, String> props`
- Configuration class wraps and validates: `ClickHouseSinkConfig(Map<String, String> props)`
- Constants for configuration keys defined in connector: `HOSTNAME`, `PORT`, `DATABASE`, etc.
- Defaults defined as class constants: `portDefault`, `usernameDefault`, `databaseDefault`
- Custom `ConfigDef.Validator` implementations: `UTF8String`, `ZKPathValidator`, `InsertFormatValidatorAndRecommender`
- Apache Kafka `ConfigDef` used for schema and validation
- Exception throwing on invalid config: `throw new ConfigException("zkPath cannot be empty and must begin with a forward slash")`

<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->

## Architecture

## System Overview

```text

```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| ClickHouseSinkConnector | Kafka Connect entrypoint; validates config; spawns tasks | `src/main/java/com/clickhouse/kafka/connect/ClickHouseSinkConnector.java` |
| ClickHouseSinkTask | Task wrapper; receives records; manages error reporting | `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java` |
| ProxySinkTask | Orchestrates record processing; batching; state coordination | `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` |
| Processing | Deduplication logic; state-based replay detection; range tracking | `src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java` |
| Record | Immutable data structure representing a Kafka message ready for insert | `src/main/java/com/clickhouse/kafka/connect/sink/data/Record.java` |
| DBWriter | Interface defining database write contract | `src/main/java/com/clickhouse/kafka/connect/sink/db/DBWriter.java` |
| ClickHouseWriter | Manages ClickHouse connections; builds INSERT statements; type conversion | `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` |
| StateProvider | Persists/retrieves offset state per topic-partition | `src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java` |
| InMemoryState | Volatile state implementation for at-least-once semantics | `src/main/java/com/clickhouse/kafka/connect/sink/state/provider/InMemoryState.java` |
| KeeperStateProvider | Persistent state via ZK/Keeper for exactly-once semantics | `src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java` |
| ClickHouseSinkConfig | Configuration validation and property binding | `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` |

## Pattern Overview

- **Task-based parallelism**: One `ProxySinkTask` per Kafka worker thread; batches records by topic-partition
- **State-driven deduplication**: Tracks offset ranges per topic-partition to detect replays and overlaps
- **Type-aware conversion**: Introspects Kafka schema (or schemaless JSON) and maps to ClickHouse columns
- **Pluggable storage backends**: InMemoryState for at-least-once; KeeperStateProvider for exactly-once
- **Record transforms**: SMT integrations (HeadersToValue, ConvertToMapOrJson, KeyToValue) before write
- **Error tolerance**: Configurable error handling with Dead Letter Queue (DLQ) routing

## Layers

- Purpose: Receive records from Kafka; manage task lifecycle; apply SMTs
- Location: `src/main/java/com/clickhouse/kafka/connect/`
- Contains: `ClickHouseSinkConnector`, `ClickHouseSinkTask`
- Depends on: Kafka Connect APIs, `ClickHouseSinkConfig`
- Used by: Kafka Connect runtime
- Purpose: Transform SinkRecord into internal Record format; extract schema/data
- Location: `src/main/java/com/clickhouse/kafka/connect/sink/data/` and `src/main/java/com/clickhouse/kafka/connect/sink/data/convert/`
- Contains: `Record`, `RecordConvertor` (variants: Schema, Schemaless, String, Empty), `Data`, `StructToJsonMap`, `SchemaType`
- Depends on: Kafka Connect data structures (Struct, Schema, Field)
- Used by: `ProxySinkTask`, `Processing`
- Purpose: Manage deduplication, replay detection, state persistence
- Location: `src/main/java/com/clickhouse/kafka/connect/sink/processing/` and `src/main/java/com/clickhouse/kafka/connect/sink/state/`
- Contains: `Processing` (orchestrates insert logic), `StateProvider` (interface), `StateRecord`, `RangeContainer`, `RangeState`
- Depends on: Record, `ClickHouseSinkConfig`
- Used by: `ProxySinkTask`
- Key decision: `Processing.doLogic()` examines state (NONE, BEFORE_PROCESSING, AFTER_PROCESSING) and range overlaps (SAME, NEW, CONTAINS, OVERLAPPING, ZERO) to decide if insert proceeds or is deduped
- Purpose: Build and execute INSERT statements; manage connections; handle type conversions
- Location: `src/main/java/com/clickhouse/kafka/connect/sink/db/`
- Contains: `DBWriter` (interface), `ClickHouseWriter`, `ClickHouseHelperClient`, `Column`, `Table`, `Type`, mapping helpers
- Depends on: ClickHouse client (JDBC/HTTP), Record, `ClickHouseSinkConfig`
- Used by: `Processing.doInsert()`
- Purpose: Define, validate, and expose connector settings
- Location: `src/main/java/com/clickhouse/kafka/connect/sink/`
- Contains: `ClickHouseSinkConfig`, validators (UTF8String, ZKPathValidator, InsertFormatValidator)
- Depends on: Kafka Connect ConfigDef
- Used by: All components
- Purpose: Mutate record structure before processing (optional)
- Location: `src/main/java/com/clickhouse/kafka/connect/transforms/`
- Contains: `HeadersToValue`, `ConvertToMapOrJson`, `ExtractTopic`, `KeyToValue`, `HeaderToValue`, `TableOperationFilter`
- Depends on: Kafka Connect Transformation API
- Used by: Kafka Connect framework (applied before `ClickHouseSinkTask.put()`)

## Data Flow

### Primary Request Path (Put Record → Insert to ClickHouse)

### State Management Path (Exactly-Once Mode)

### Record Conversion Path (Schema Selection)

- **Volatile (InMemoryState)**: ConcurrentHashMap per task; lost on restart → at-least-once semantics (may replay)
- **Persistent (KeeperStateProvider)**: ZK/Keeper coordination; survives restarts → exactly-once semantics (dedup via offset range)
- **State Transitions**: NONE → BEFORE_PROCESSING → AFTER_PROCESSING (or error states)

## Key Abstractions

- Purpose: Internal representation of a Kafka message after conversion; immutable
- Examples: `src/main/java/com/clickhouse/kafka/connect/sink/data/Record.java`
- Pattern: Factory method `Record.convert()` with polymorphic convertors
- Purpose: Strategy pattern for converting SinkRecord → Record based on input type
- Examples: `SchemaRecordConvertor`, `SchemalessRecordConvertor`, `StringRecordConvertor`, `EmptyRecordConvertor`
- Pattern: Interface + implementations in `src/main/java/com/clickhouse/kafka/connect/sink/data/convert/`
- Purpose: Immutable snapshot of offset range and processing state for a topic-partition
- Examples: `src/main/java/com/clickhouse/kafka/connect/sink/state/StateRecord.java`
- Pattern: Value object holding (topic, partition, minOffset, maxOffset, State enum)
- Purpose: Track min/max offset in current batch; compute overlap relationship with stored state
- Examples: `src/main/java/com/clickhouse/kafka/connect/sink/kafka/RangeContainer.java`, `RangeState` enum
- Pattern: Enum (SAME, NEW, CONTAINS, OVERLAPPING, ZERO, ERROR) for decision routing
- Purpose: Metadata about ClickHouse table columns; enumerates supported types
- Examples: `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java`, `Type` enum
- Pattern: Reflection-based introspection of ClickHouse schema; recursive for complex types (Map, Array, Tuple, Variant)
- Purpose: Unique identifier for an INSERT statement (topic, partition, offset range, UUID)
- Examples: `src/main/java/com/clickhouse/kafka/connect/util/QueryIdentifier.java`
- Pattern: Immutable tuple; passed through error handling chain for debugging

## Entry Points

- Location: `src/main/java/com/clickhouse/kafka/connect/ClickHouseSinkConnector.java:53`
- Triggers: Kafka Connect framework on connector startup
- Responsibilities: Logs version; stores configuration map
- Location: `src/main/java/com/clickhouse/kafka/connect/ClickHouseSinkConnector.java:60`
- Triggers: Kafka Connect framework during task creation
- Responsibilities: Returns `ClickHouseSinkTask.class` for worker threads
- Location: `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java:30`
- Triggers: Kafka Connect worker on task spawn
- Responsibilities: Creates `ClickHouseSinkConfig`; instantiates error reporter; creates `ProxySinkTask`
- Location: `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java:46`
- Triggers: Kafka Connect worker when records are ready for processing
- Responsibilities: Delegates to `ProxySinkTask.put()`; catches exceptions; routes to DLQ on error tolerance
- Location: `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:85`
- Triggers: `ClickHouseSinkTask.put()`
- Responsibilities: Converts SinkRecord → Record; batches by topic-partition; invokes `Processing.doLogic()`
- Location: `src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java:106`
- Triggers: `ProxySinkTask.put()` per topic-partition batch
- Responsibilities: State lookup; dedup routing; insert invocation

## Architectural Constraints

- **Threading:** Single-threaded per task (no internal parallelism). ProxySinkTask processes one batch at a time. `RangeContainer` and state checks are non-blocking.
- **Global state:** `ClickHouseWriter.mapping` (table metadata cache) is instance-level; shared across all records in a task. `ProxySinkTask.statistics` is task-level. State provider is thread-safe (InMemoryState uses ConcurrentHashMap; KeeperStateProvider uses ZK locks).
- **Circular imports:** None detected. Clear dependency hierarchy: Connector → Task → Processing + StateProvider + DBWriter → Record + Config.
- **Connection pooling:** ClickHouseHelperClient manages connection via ClickHouseClient; one client per ClickHouseWriter instance (task-scoped). No connection reuse across tasks.
- **State consistency:** KeeperStateProvider enforces atomicity via ZK transactions; InMemoryState is process-scoped only. Multi-task deployments require KeeperStateProvider for consistency.
- **Record immutability:** Record is immutable after construction; avoids mutation bugs during batching.

## Anti-Patterns

### Overly Complex State Transitions

```java

```

### Type Conversion Scattered Across ClickHouseWriter

### Column Metadata Refresh via Timer

## Error Handling

- Exception wrapping with context: `throw new RuntimeException(queryId.toString(), e)` preserves offset range for debugging.
- DLQ routing: Errant records are reported via `ErrorReporter.report()` (Kafka Connect's DLQ mechanism).
- Config-based tolerance: `errors.tolerance=all` enables DLQ routing; `errors.tolerance=none` (default) fails the task.

## Cross-Cutting Concerns

- Config validation via `ClickHouseSinkConfig` constructor and custom validators (UTF8String, ZKPathValidator, InsertFormatValidator).
- Schema validation (optional) via `bypassSchemaValidation` flag; if disabled, columns must exist in target table.
- Record validation: `RecordConvertor` throws `DataException` if conversion fails (e.g., no convertor for type).

<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->

## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->

## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:

- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->

## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
