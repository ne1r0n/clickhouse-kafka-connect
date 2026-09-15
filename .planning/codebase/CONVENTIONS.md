---
last_mapped_commit: 22f055c712de378404f73cffe198b641002ed248
last_mapped_at: 2026-09-15
---
# Coding Conventions

**Analysis Date:** 2026-09-15

## Naming Patterns

**Files:**

- Class files use PascalCase: `ClickHouseSinkConnector.java`, `ClickHouseSinkTask.java`
- Test files suffix with `Test`: `ClickHouseSinkTaskTest.java`, `ConvertToMapOrJsonTest.java`
- Abstract base classes: `RecordConvertor.java`, `ClickHouseBase.java`
- Configuration/config classes: `ClickHouseSinkConfig.java`, `HeaderToValueConfig.java`
- Helper/utility classes: `Utils.java`, `Mask.java`, `QueryIdentifier.java`

**Classes:**

- PascalCase: `ClickHouseSinkConnector`, `ProxySinkTask`, `HeadersToValue`
- Inner classes use descriptive names with their purpose: `ConfigDef.Validator` implementations like `UTF8String`, `ZKPathValidator`, `InsertFormatValidatorAndRecommender`
- Enum classes defined inline: `InsertFormats` with values `NONE`, `CSV`, `TSV`, `JSON`

**Functions/Methods:**

- camelCase: `getRootCause()`, `handleException()`, `getTableName()`, `createDatabase()`
- Overridden methods: `start()`, `put()`, `stop()`, `apply()` follow Kafka Connect interface conventions
- Static factory methods: `createClient()`, `devNullErrorReporter()`
- Getters use `get` prefix: `getHostname()`, `getPort()`, `getDatabase()`
- Predicates/boolean methods: `isSslEnabled()`, `isCloud()`, `isErrorsTolerance()`, `isUseClientV2()`

**Variables:**

- camelCase for local and instance variables: `hostname`, `database`, `username`, `sinkRecord`, `errorReporter`
- Private fields with descriptive names: `proxySinkTask`, `clickHouseSinkConfig`, `errorReporter`
- Builder pattern parameters: `hostname`, `port`, `database`, `username`, `password`, `sslEnabled`

**Constants:**

- CONSTANT_CASE for public static final strings: `HOSTNAME`, `PORT`, `DATABASE`, `TABLE_MAPPING`, `ERRORS_TOLERANCE`
- Enum constant values: `RELEASE`, `FINAL`, `GA` (from dependency version checking)
- Magic numbers extracted: `MILLI_IN_A_SEC = 1000`, `DEFAULT_TOTAL_RECORDS = 1000`

**Package Organization:**

- Standard reverse-domain: `com.clickhouse.kafka.connect`
- Layered by function: `.sink`, `.sink.db`, `.sink.db.helper`, `.sink.data`, `.sink.data.convert`, `.sink.dedup`, `.sink.dlq`, `.transforms`, `.util`
- Test packages mirror source: `src/test/java` mirrors `src/main/java` structure
- Separate integration test source set: `src/integrationTest/java`

## Code Style

**Formatting:**

- Tool used: Spotless (gradle plugin version 8.1.0) - see `build.gradle.kts` lines 36, 280-286
- Import organization: `spotless { java { importOrder() removeUnusedImports() } }`
- Manual formatting task: `gradle removeUnusedImports` task (line 288-292)

**Linting:**

- Java compiler warnings enforced via `-Xlint:unchecked` and `-Xlint:deprecation` (line 168)
- Java version: JDK 17 (see `java.toolchain.languageVersion = 17` at line 17)

**Indentation:**

- 4-space indentation (standard Java)
- Opening braces on same line (Java standard)

**Line Length:**

- No explicit enforced limit observed, but generally kept reasonable (~120 chars)

## Import Organization

**Order:**

1. `java.` packages (JDK standard library)
2. `javax.` packages (JDK extensions)
3. Third-party libraries (alphabetical): `com.clickhouse`, `com.fasterxml.jackson`, `com.google.gson`, `lombok`, `org.apache.kafka`, `org.slf4j`
4. Internal project packages: `com.clickhouse.kafka.connect.*`

**Examples from codebase:**

```java
// ClickHouseSinkTask.java
import com.clickhouse.kafka.connect.sink.dlq.ErrorReporter;
import com.clickhouse.kafka.connect.util.Utils;
import java.util.Collection;
import java.util.Map;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.ErrantRecordReporter;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

**Path Aliases:**

- No aliases observed; fully qualified names used throughout

## Error Handling

**Patterns:**

### Exception Wrapping Strategy

- Custom logic in `com.clickhouse.kafka.connect.util.Utils.handleException()` (`src/main/java/com/clickhouse/kafka/connect/util/Utils.java`)
- Distinguishes between retriable and non-retriable errors based on ClickHouse error codes (lines 63-127)

### Exception Hierarchy

- `RetriableException` thrown for network/timeout/transient errors:
  - ClickHouse error codes: 3, 107, 159, 164, 202, 203, 209, 210, 241, 242, 252, 285, 319, 425, 999
  - `SocketTimeoutException`, `UnknownHostException`
  - IOException with specific timeout messages

- `DataException` thrown for schema/data validation errors when errors tolerance disabled

- `RuntimeException` thrown for non-retriable application errors (wraps root cause)

### Root Cause Extraction

```java
// Utils.getRootCause(Exception e, Boolean prioritizeClickHouseException)
// Drills down exception chain to first ClickHouseException if requested
Exception rootCause = Utils.getRootCause(e, true);
```

### DLQ (Dead Letter Queue) Pattern

- Errors sent to DLQ when errors tolerance enabled: `Utils.sendTODlq(errorReporter, record, exception)`
- See `ClickHouseSinkTask.put()` lines 54-61

### Try-Catch Usage

- Broad catch blocks wrapping business logic, then delegated to Utils.handleException
- TestContainers setup wrapped in try-with-resources (ClickHouseSinkTaskTest lines 90-96)

## Logging

**Framework:** SLF4J with SLF4J Reload4j backend (version 2.0.17)

**Logger Declaration Pattern:**

```java
private static final Logger LOGGER = LoggerFactory.getLogger(ClassName.class);
```

**Logging Levels Used:**

- `LOGGER.info()` - High-level operations: "Starting SinkConnect...", "Setting up connector..."
- `LOGGER.warn()` - Warnings and error decisions: "Deciding how to handle exception...", "Sending records to DLQ..."
- `LOGGER.error()` - Errors: "Error dropping database", "Errors tolerance is disabled..."
- `LOGGER.debug()` - Debug info: "Topic name: X, Table Name: Y", username addition
- `LOGGER.trace()` - Low-level trace: "Found exception:", "Passing the exception to the exception handler"

**Patterns:**

```java
LOGGER.info("Start SinkTask: ");
LOGGER.warn("Deciding how to handle exception: {}", e.getLocalizedMessage());
LOGGER.error("Error code [{}] wasn't in the acceptable list.", clickHouseException.getErrorCode());
LOGGER.debug("Topic name: {}, Table Name: {}", topicName, tableName);
```

## Comments

**When to Comment:**

- Complex exception handling logic: "Will drill down to the first ClickHouseException in the exception chain" (Utils.java line 36-38)
- Clarify non-obvious code: Comments on protocol construction, error code switching
- TODO/FIXME markers: "// TODO: need to remove ???" (build.gradle.kts line 102), "// TODO: can be removed ss" (ClickHouseSinkTask.java line 66)
- Design decisions: "// We have to use Throwable because of the getCause() signature" (Utils.java line 46)

**JSDoc/JavaDoc:**

- Minimal usage; not a primary convention in this codebase
- Where used, describes the "why": 

```java
/**

 * This will drill down to the first ClickHouseException in the exception chain
 *
 * @param e Exception to drill down
 * @return ClickHouseException or null if none found
 */
public static Exception getRootCause(Exception e, Boolean prioritizeClickHouseException)
```

## Function Design

**Size:** Methods are compact, typically 10-40 lines; larger methods break logic into helpers

**Parameters:**

- Constructor injection preferred: `ClickHouseHelperClient(ClickHouseClientBuilder builder)`
- Maps for configuration: `Map<String, String> props`
- Use of builder pattern for complex object construction: `ClickHouseClientBuilder`

**Return Values:**

- Void for side-effect operations: `put()`, `start()`, `stop()`
- Objects for queries: `getTableName()` returns `String`
- Collections for batch operations: `taskConfigs()` returns `List<Map<String, String>>`
- Boolean for predicates: `isSslEnabled()`, `isCloud()`

**Static Utility Methods:**

- Grouping in `Utils` class for shared logic: `escapeName()`, `getRootCause()`, `handleException()`, `getTableName()`
- No instance state for utility methods

## Module Design

**Exports:**

- Kafka Connect API: `ClickHouseSinkConnector` extends `SinkConnector`, `ClickHouseSinkTask` extends `SinkTask`
- Public configuration classes: `ClickHouseSinkConfig`, `HeaderToValueConfig`
- Transforms: `HeadersToValue`, `HeaderToValue`, `ConvertToMapOrJson`, `TableOperationFilter`, `ExtractTopic`

**Barrel Files:**

- Not used; imports are explicit and fully qualified

**Builder Pattern:**

- Used for complex object construction: `ClickHouseHelperClient.ClickHouseClientBuilder`
- Fluent API: `.setDatabase()`, `.setUsername()`, `.setPassword()`, `.sslEnable()`, `.setTimeout()`, `.useClientV2()`

**Inheritance:**

- Abstract base class for converters: `RecordConvertor` with abstract `doConvert()` method
- Test base class for setup/teardown: `ClickHouseBase` with static `@BeforeAll setup()` and `@AfterAll tearDown()`
- Kafka Connect framework classes extended: `SinkConnector`, `SinkTask`, `SingleMessageTransforms`

**Lombok Annotations:**

- `@Getter` for auto-generated getters: `@Getter private final String database;`
- Minimal use; not pervasive throughout codebase
- Located at line 19 in `ClickHouseSinkConfig`, lines 44, 50, 52, 58 in `ClickHouseHelperClient`

## Configuration Management

**Approach:**

- String-based configuration maps: `Map<String, String> props`
- Configuration class wraps and validates: `ClickHouseSinkConfig(Map<String, String> props)`
- Constants for configuration keys defined in connector: `HOSTNAME`, `PORT`, `DATABASE`, etc.
- Defaults defined as class constants: `portDefault`, `usernameDefault`, `databaseDefault`

**Validation:**

- Custom `ConfigDef.Validator` implementations: `UTF8String`, `ZKPathValidator`, `InsertFormatValidatorAndRecommender`
- Apache Kafka `ConfigDef` used for schema and validation
- Exception throwing on invalid config: `throw new ConfigException("zkPath cannot be empty and must begin with a forward slash")`

---

*Convention analysis: 2026-09-15*
