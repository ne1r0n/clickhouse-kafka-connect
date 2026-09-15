---
last_mapped_commit: 22f055c712de378404f73cffe198b641002ed248
last_mapped_at: 2026-09-15
---
# Codebase Structure

**Analysis Date:** 2026-09-15

## Directory Layout

```
clickhouse-kafka-connect_my/
├── src/
│   ├── main/
│   │   ├── java/com/clickhouse/kafka/connect/
│   │   │   ├── ClickHouseSinkConnector.java      # Kafka Connect entry point
│   │   │   ├── sink/
│   │   │   │   ├── ClickHouseSinkConfig.java      # Configuration & validation
│   │   │   │   ├── ClickHouseSinkTask.java        # Task wrapper
│   │   │   │   ├── ProxySinkTask.java             # Core orchestrator
│   │   │   │   ├── data/
│   │   │   │   │   ├── Record.java                # Internal record format
│   │   │   │   │   ├── Data.java                  # Field value wrapper
│   │   │   │   │   ├── SchemaType.java            # Enum: SCHEMA, SCHEMALESS, STRING, EMPTY
│   │   │   │   │   ├── StructToJsonMap.java       # Struct → Map converter
│   │   │   │   │   └── convert/
│   │   │   │   │       ├── RecordConvertor.java   # Base interface
│   │   │   │   │       ├── SchemaRecordConvertor.java    # Kafka Struct converter
│   │   │   │   │       ├── SchemalessRecordConvertor.java # Map converter
│   │   │   │   │       ├── StringRecordConvertor.java     # JSON string converter
│   │   │   │   │       └── EmptyRecordConvertor.java      # Null converter
│   │   │   │   ├── db/
│   │   │   │   │   ├── DBWriter.java              # Interface
│   │   │   │   │   ├── ClickHouseWriter.java      # Implementation
│   │   │   │   │   ├── TableMappingRefresher.java # Periodic metadata reload
│   │   │   │   │   ├── helper/
│   │   │   │   │   │   ├── ClickHouseHelperClient.java  # Connection wrapper
│   │   │   │   │   │   └── ClickHouseFieldDescriptor.java
│   │   │   │   │   └── mapping/
│   │   │   │   │       ├── Table.java             # ClickHouse table metadata
│   │   │   │   │       ├── Column.java            # Column metadata & type mapping
│   │   │   │   │       └── Type.java              # Enum of supported types (+ DYNAMIC)
│   │   │   │   ├── processing/
│   │   │   │   │   └── Processing.java            # Dedup & state logic
│   │   │   │   ├── state/
│   │   │   │   │   ├── StateProvider.java         # Interface
│   │   │   │   │   ├── StateRecord.java           # Immutable offset range + state
│   │   │   │   │   ├── State.java                 # Enum: NONE, BEFORE_PROCESSING, AFTER_PROCESSING
│   │   │   │   │   └── provider/
│   │   │   │   │       ├── InMemoryState.java     # Volatile HashMap implementation
│   │   │   │   │       └── KeeperStateProvider.java  # ZK/Keeper implementation
│   │   │   │   ├── kafka/
│   │   │   │   │   ├── OffsetContainer.java       # Wraps topic, partition, offset
│   │   │   │   │   ├── RangeContainer.java        # Min/max offset tracker
│   │   │   │   │   └── RangeState.java            # Enum: SAME, NEW, CONTAINS, OVERLAPPING, ZERO, ERROR
│   │   │   │   ├── dedup/
│   │   │   │   │   └── [Dedup implementations]
│   │   │   │   └── dlq/
│   │   │   │       ├── ErrorReporter.java         # Interface for DLQ reporting
│   │   │   │       └── DuplicateException.java
│   │   │   ├── transforms/
│   │   │   │   ├── HeadersToValue.java            # Custom: headers → field (NEW - custom_build)
│   │   │   │   ├── HeaderToValue.java             # Single header → field
│   │   │   │   ├── ConvertToMapOrJson.java        # Struct/Map → JSON conversion
│   │   │   │   ├── ExtractTopic.java              # Extract Kafka topic → field
│   │   │   │   ├── KeyToValue.java                # Kafka key → field
│   │   │   │   ├── TableOperationFilter.java      # Filter by operation type
│   │   │   │   ├── HeaderToValueConfig.java       # Config for HeaderToValue
│   │   │   │   └── ExtractTopicConfig.java        # Config for ExtractTopic
│   │   │   └── util/
│   │   │       ├── Utils.java                     # Helper functions (DLQ, exceptions)
│   │   │       ├── Mask.java                      # Field masking
│   │   │       ├── QueryIdentifier.java           # Query context wrapper
│   │   │       ├── jmx/
│   │   │       │   ├── ExecutionTimer.java        # Perf instrumentation
│   │   │       │   ├── SinkTaskStatistics.java    # MBean for metrics
│   │   │       │   └── MBeanServerUtils.java      # JMX registration
│   │   │       └── reactor/
│   │   │           └── function/
│   │   │               ├── Tuple2.java            # Generic pair
│   │   │               ├── Tuple3.java            # Generic triple
│   │   │               └── Tuples.java            # Factory methods
│   │   └── resources/
│   │       └── META-INF/services/
│   │           ├── org.apache.kafka.connect.sink.SinkConnector        # Entry point registration
│   │           └── org.apache.kafka.connect.transforms.Transformation # Transform registrations
│   ├── test/
│   │   ├── java/com/clickhouse/kafka/connect/
│   │   │   └── sink/
│   │   │       ├── ClickHouseSinkTaskTest.java              # Main task tests
│   │   │       ├── ClickHouseSinkTaskWithSchemaTest.java
│   │   │       ├── ClickHouseSinkTaskSchemalessTest.java
│   │   │       ├── ClickHouseSinkTaskStringTest.java
│   │   │       ├── ClickHouseSinkJdbcPropertiesTest.java
│   │   │       ├── JsonColumnTest.java
│   │   │       ├── db/
│   │   │       │   ├── ClickHouseWriterTest.java
│   │   │       │   └── mapping/
│   │   │       │       └── [Mapping tests]
│   │   │       ├── dlq/
│   │   │       │   └── InMemoryDLQ.java             # Test DLQ stub
│   │   │       ├── helper/
│   │   │       │   ├── ClickHouseTestHelpers.java   # Test fixtures
│   │   │       │   ├── SchemaTestData.java          # Schema test payloads
│   │   │       │   └── SchemalessTestData.java      # Schemaless test payloads
│   │   │       ├── junit/
│   │   │       │   └── extension/
│   │   │       │       └── [JUnit 5 extensions]
│   │   │       ├── kafa/
│   │   │       │   └── RangeContainerTest.java     # Range logic tests
│   │   │       ├── processing/
│   │   │       │   └── ProcessingTest.java         # Dedup logic tests
│   │   │       ├── provider/
│   │   │       │   └── LocalProviderTest.java      # State provider tests
│   │   │       └── util/
│   │   │           ├── UtilsTest.java
│   │   │           └── MaskTest.java
│   │   ├── java/transforms/
│   │   │   ├── HeadersToValueTest.java             # Custom transform tests (NEW - custom_build)
│   │   │   ├── HeaderToValueTest.java
│   │   │   ├── KeyToValueTest.java
│   │   │   ├── TableOperationFilterTest.java
│   │   │   └── ConvertToMapOrJsonTest.java
│   │   └── resources/
│   │       └── [Test fixtures, config files]
│   └── integrationTest/
│       ├── java/com/clickhouse/kafka/connect/sink/
│       │   ├── ClickHouseSinkConnectorIntegrationTest.java
│       │   ├── ExactlyOnceTest.java                # State persistence tests
│       │   ├── ClickHouseCloudTest.java
│       │   └── helper/
│       │       ├── ClickHouseTestHelpers.java
│       │       ├── SchemaTestData.java
│       │       ├── SchemalessTestData.java
│       │       └── ConfluentPlatform.java          # Docker Compose orchestration
│       └── resources/
│           └── [Integration test config]
├── docs/
│   ├── [User guides, deployment docs, configuration reference]
├── config/
│   ├── [Sample connector configs]
├── gradle/
│   └── [Gradle wrapper & plugins]
├── build.gradle.kts                # Build configuration (Kotlin DSL)
├── settings.gradle.kts             # Root project config
├── VERSION                         # Version file (read by build)
├── README.md
├── CHANGELOG.md
├── CONTRIBUTING.md
├── LICENSE
├── SECURITY.md
└── .planning/
    └── codebase/
        ├── ARCHITECTURE.md
        └── STRUCTURE.md
```

## Directory Purposes

**`src/main/java/com/clickhouse/kafka/connect/`**

- Purpose: Kafka Connect SPI entry point
- Contains: `ClickHouseSinkConnector` (task factory)
- Committed: Yes

**`src/main/java/com/clickhouse/kafka/connect/sink/`**

- Purpose: Core sink logic (tasks, config, orchestration)
- Contains: Task class, config validation, ProxySinkTask orchestrator
- Committed: Yes

**`src/main/java/com/clickhouse/kafka/connect/sink/data/`**

- Purpose: Record conversion from Kafka to internal format
- Contains: Record, Data, RecordConvertor implementations
- Committed: Yes

**`src/main/java/com/clickhouse/kafka/connect/sink/db/`**

- Purpose: Database writing and schema introspection
- Contains: DBWriter interface, ClickHouseWriter impl, type mapping, metadata
- Committed: Yes

**`src/main/java/com/clickhouse/kafka/connect/sink/processing/`**

- Purpose: Deduplication logic and state-driven replay detection
- Contains: Processing class with state machine for insert routing
- Committed: Yes

**`src/main/java/com/clickhouse/kafka/connect/sink/state/`**

- Purpose: Offset state management (volatile and persistent)
- Contains: StateProvider interface, StateRecord, InMemoryState, KeeperStateProvider
- Committed: Yes

**`src/main/java/com/clickhouse/kafka/connect/sink/kafka/`**

- Purpose: Kafka offset and range tracking
- Contains: OffsetContainer (topic/partition/offset), RangeContainer, RangeState enum
- Committed: Yes

**`src/main/java/com/clickhouse/kafka/connect/transforms/`**

- Purpose: Optional Single Message Transforms (SMTs)
- Contains: HeadersToValue (custom), ConvertToMapOrJson, ExtractTopic, KeyToValue, etc.
- Committed: Yes

**`src/main/java/com/clickhouse/kafka/connect/util/`**

- Purpose: Shared utilities (error handling, masking, JMX metrics)
- Contains: Utils, Mask, QueryIdentifier, JMX instrumentation
- Committed: Yes

**`src/test/java/` and `src/integrationTest/java/`**

- Purpose: Unit and integration test suites
- Contains: Test classes mirroring main structure, test helpers, fixtures
- Committed: Yes

**`docs/`**

- Purpose: User documentation and deployment guides
- Generated: No
- Committed: Yes

**`build/` and `.gradle/`**

- Purpose: Build artifacts and Gradle caches
- Generated: Yes
- Committed: No

## Key File Locations

**Entry Points:**

- `src/main/java/com/clickhouse/kafka/connect/ClickHouseSinkConnector.java`: Kafka Connect SPI entry (extends SinkConnector)
- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java`: Task implementation (extends SinkTask)
- `src/main/resources/META-INF/services/org.apache.kafka.connect.sink.SinkConnector`: Connector registration

**Configuration:**

- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java`: Config definitions, validators, defaults

**Core Logic:**

- `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java`: Record batching, state coordination
- `src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java`: Deduplication state machine (200+ lines)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`: Type conversion, SQL building, insert execution

**Record Conversion:**

- `src/main/java/com/clickhouse/kafka/connect/sink/data/Record.java`: Immutable record wrapper
- `src/main/java/com/clickhouse/kafka/connect/sink/data/convert/SchemaRecordConvertor.java`: Kafka Struct → Record
- `src/main/java/com/clickhouse/kafka/connect/sink/data/convert/SchemalessRecordConvertor.java`: Map → Record
- `src/main/java/com/clickhouse/kafka/connect/sink/data/convert/StringRecordConvertor.java`: JSON string → Record

**State Management:**

- `src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java`: Interface
- `src/main/java/com/clickhouse/kafka/connect/sink/state/provider/InMemoryState.java`: Volatile HashMap (at-least-once)
- `src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java`: ZK/Keeper (exactly-once)

**Type Mapping:**

- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Type.java`: Enum of 35+ ClickHouse types (including DYNAMIC)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java`: Metadata + recursive type parsing

**Custom Transforms (custom_build branch):**

- `src/main/java/com/clickhouse/kafka/connect/transforms/HeadersToValue.java`: NEW — copies Kafka headers into record value field
- `src/test/java/transforms/HeadersToValueTest.java`: Test for HeadersToValue

**Testing Helpers:**

- `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java`: Fixtures, container setup
- `src/test/java/com/clickhouse/kafka/connect/sink/helper/SchemaTestData.java`: Schema payloads
- `src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java`: Docker Compose orchestration

## Naming Conventions

**Files:**

- Interface: `[Name].java` (no prefix/suffix, e.g., `DBWriter.java`, `StateProvider.java`, `RecordConvertor.java`)
- Implementation: `[Concrete][Interface].java` (e.g., `ClickHouseWriter.java`, `InMemoryState.java`, `SchemaRecordConvertor.java`)
- Test: `[Class]Test.java` or `[Class][Scenario]Test.java` (e.g., `ClickHouseSinkTaskTest.java`, `ProcessingTest.java`)
- Config: `[Name]Config.java` (e.g., `ClickHouseSinkConfig.java`, `HeaderToValueConfig.java`)
- Enum: `[Name].java` (e.g., `State.java`, `RangeState.java`, `Type.java`, `SchemaType.java`)

**Directories:**

- Package name: lowercase with underscores for compound words (e.g., `com.clickhouse.kafka.connect.sink.db.mapping`)
- Logical grouping: singular noun (e.g., `state`, `data`, `db`, `processing`, `kafka`, `transforms`, `util`)

**Classes:**

- Connector/Task/Config: CamelCase ending with `Connector`, `Task`, `Config` (e.g., `ClickHouseSinkConnector`, `ClickHouseSinkTask`, `ClickHouseSinkConfig`)
- Impl classes: CamelCase with domain prefix (e.g., `ClickHouseWriter`, `KeeperStateProvider`, `InMemoryState`)
- Enum values: UPPER_SNAKE_CASE (e.g., `BEFORE_PROCESSING`, `SCHEMALESS`, `CONTAINS`)

## Where to Add New Code

**New Feature (e.g., new SMT or data type support):**

- Primary code: `src/main/java/com/clickhouse/kafka/connect/sink/[module]/[Name].java`
- Tests: `src/test/java/com/clickhouse/kafka/connect/sink/[module]/[Name]Test.java`
- If it's a transform: `src/main/java/com/clickhouse/kafka/connect/transforms/[TransformName].java`

**New Component/Module (e.g., new state provider backend):**

- Implementation: `src/main/java/com/clickhouse/kafka/connect/sink/[new-module]/[Name].java`
- Interface (if applicable): `src/main/java/com/clickhouse/kafka/connect/sink/[module]/[Name].java` (one level up)
- Tests: Mirror structure under `src/test/java/`

**Utilities and Helpers:**

- Shared helpers: `src/main/java/com/clickhouse/kafka/connect/util/[Name].java`
- JMX metrics: `src/main/java/com/clickhouse/kafka/connect/util/jmx/[Name].java`
- Reactor utilities: `src/main/java/com/clickhouse/kafka/connect/util/reactor/[Name].java`

**Configuration:**

- Config class: `src/main/java/com/clickhouse/kafka/connect/sink/[Name]Config.java` (co-located with using module)
- Validators: Inline as static inner classes in the config class

**Tests:**

- Unit tests: `src/test/java/` (mirror main structure)
- Integration tests: `src/integrationTest/java/` (if requires testcontainers, Docker)
- Test fixtures: `src/test/java/com/clickhouse/kafka/connect/sink/helper/[Name].java`

## Special Directories

**`src/main/resources/META-INF/services/`**

- Purpose: Kafka Connect SPI service provider descriptors
- Generated: No (hand-maintained)
- Committed: Yes
- Contains: 
  - `org.apache.kafka.connect.sink.SinkConnector` → `com.clickhouse.kafka.connect.ClickHouseSinkConnector`
  - `org.apache.kafka.connect.transforms.Transformation` → List of all transform classes

**`.planning/codebase/`**

- Purpose: Codebase analysis documents (architecture, structure, conventions, testing patterns, concerns)
- Generated: Yes (by `/gsd-map-codebase` and `/gsd-plan-phase`)
- Committed: Yes

**`build/` and `.gradle/`**

- Purpose: Gradle build artifacts and caches
- Generated: Yes
- Committed: No

**`docs/`**

- Purpose: User-facing documentation (deployment, configuration, examples)
- Generated: No (hand-authored)
- Committed: Yes

**`config/`**

- Purpose: Sample connector configuration files
- Generated: No
- Committed: Yes

---

*Structure analysis: 2026-09-15*
