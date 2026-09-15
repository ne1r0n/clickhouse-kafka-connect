---
last_mapped_commit: 22f055c712de378404f73cffe198b641002ed248
last_mapped_at: 2026-09-15
---
# Testing Patterns

**Analysis Date:** 2026-09-15

## Test Framework

**Runner:**

- JUnit Jupiter (JUnit 5) version 5.11.3
- Config: `build.gradle.kts` line 175: `useJUnitPlatform()`

**Supporting Frameworks:**

- JUnit Platform Runner: 1.14.2 (line 123)
- Hamcrest matchers: 3.0 (line 70, 125)
- Mockito: 5.21.0 (line 71, 126)
- TestContainers: 1.21.4 (line 105, 129-130)
- TestContainers ClickHouse: 1.21.4 (line 129)
- TestContainers Kafka: 1.21.4 (line 130)
- TestContainers Toxiproxy: 1.21.4 (line 133)

**Run Commands:**

```bash
gradle test                  # Run all unit tests
gradle integrationTest       # Run integration tests (defined at line 156)
gradle check                 # Run all checks including tests (tasks depend on test at line 173)
```

**Test Configuration:**

- Max parallel forks: `(Runtime.getRuntime().availableProcessors() / 2)` (line 172)
- File encoding: `windows-1252` for test environment (line 174)
- Exception format: FULL (line 179)
- Test logging events: `passed, skipped, failed` (line 177)
- Summary printed after each test suite (lines 196-215)

## Test File Organization

**Location:**

- Unit tests: `src/test/java` mirrors `src/main/java` structure
- Integration tests: `src/integrationTest/java` separate source set (lines 151-155)
- Test resources: `src/test/resources`

**Naming:**

- Test class suffix: `Test` → `ClickHouseSinkTaskTest.java`, `ConvertToMapOrJsonTest.java`
- Test method prefix: `test` → `testExceptionHandling()`, `testStructToMapSimple()`

**Directory Structure:**

```
src/test/java/
├── com/clickhouse/kafka/connect/sink/
│   ├── ClickHouseBase.java           # Base class with setup/teardown
│   ├── ClickHouseSinkTaskTest.java
│   ├── ClickHouseSinkTaskWithSchemaTest.java
│   ├── ClickHouseSinkTaskSchemalessTest.java
│   ├── ClickHouseSinkTaskJsonInsertFormatTest.java
│   └── helper/
│       └── ClickHouseTestHelpers.java
├── transforms/
│   ├── ConvertToMapOrJsonTest.java
│   ├── HeadersToValueTest.java
│   ├── HeaderToValueTest.java
│   ├── KeyToValueTest.java
│   └── TableOperationFilterTest.java

src/integrationTest/java/
├── com/clickhouse/kafka/connect/sink/
│   ├── ClickHouseSinkConnectorIntegrationTest.java
│   ├── ExactlyOnceTest.java
│   └── ClickHouseCloudTest.java
```

## Test Structure

**Suite Organization:**

```java
// Example from ClickHouseSinkTaskTest.java
public class ClickHouseSinkTaskTest extends ClickHouseBase {
    
    @Test
    public void testExceptionHandling() {
        ClickHouseSinkTask task = new ClickHouseSinkTask();
        assertThrows(RuntimeException.class, () -> task.put(null));
        // Assertions follow
    }
    
    @Test
    public void testAnotherFeature() {
        // Arrange
        // Act
        // Assert
    }
}
```

**Patterns:**

### Setup/Teardown (Unit Tests)

```java
// ClickHouseBase.java - Static setup/teardown for shared resources
@BeforeAll
public static void setup() throws IOException {
    if (database == null) {
        database = String.format("kafka_connect_test_%s", System.currentTimeMillis());
    }
    if (isCloud) {
        initialPing();
        return;
    }
    db = new ClickHouseContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE)
        .withPassword("test_password")
        .withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1");
    db.start();
}

@AfterAll
protected static void tearDown() {
    if (isCloud) {
        if (database != null) {
            try {
                dropDatabase(database);
            } catch (Exception e) {
                LOGGER.error("Error dropping database", e);
            }
        }
    }
    if (db != null)
        db.stop();
}
```

### Setup/Teardown (Integration Tests)

```java
// ClickHouseSinkConnectorIntegrationTest.java
@BeforeAll
public static void setup() {
    Network network = Network.newNetwork();
    // ... container initialization
    db.start();
    toxiproxy.start();
}

@AfterAll
public static void tearDown() {
    db.stop();
    toxiproxy.stop();
    confluentPlatform.close();
}
```

### Test Method Structure

```java
@Test
public void testStructToMapSimple() {
    // Arrange
    Schema schema = SchemaBuilder.struct()
        .field("foo", Schema.STRING_SCHEMA)
        .field("bar", Schema.INT32_SCHEMA)
        .build();
    Struct struct = new Struct(schema).put("foo", "abc").put("bar", 123);
    SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
    
    // Act
    ConvertToMapOrJson<SinkRecord> xform = new ConvertToMapOrJson<>();
    xform.configure(new HashMap<>());
    SinkRecord result = xform.apply(record);
    
    // Assert
    assertTrue(result.value() instanceof Map);
    Map<?,?> map = (Map<?,?>) result.value();
    assertEquals("abc", map.get("foo"));
    assertEquals(123, map.get("bar"));
}
```

## Assertions

**Library:** JUnit Jupiter Assertions (`org.junit.jupiter.api.Assertions`)

**Common Assertions Used:**

- `assertEquals(expected, actual)` - equality check
- `assertTrue(condition)` - boolean assertion
- `assertThrows(ExceptionClass.class, () -> { code })` - exception verification
- `assertNotNull(object)` - null check
- `assertInstanceOf(Class, object)` - type assertion

**With Hamcrest:**

- `assertThat(value, is(expected))`
- Custom matchers available but not heavily used in this codebase

**Example from ConvertToMapOrJsonTest:**

```java
assertTrue(resultJackson.value() instanceof String);
assertEquals(mapJackson, mapGson);
assertTrue(jsonJackson.contains("\"date\":" + date.getTime()));
```

## Mocking

**Framework:** Mockito 5.21.0 (integrated via `mockito-junit-jupiter`)

**Patterns:**

- Limited mocking observed in current test suite
- Preference for real objects and test containers over mocks
- Kafka Connect objects (SinkRecord, Schema, Struct) created directly, not mocked

**Where Mocking is Used:**

- Test infrastructure setup: `ErrorReporter` interface mocked or replaced with lambdas

```java
// ClickHouseSinkTask.java
static ErrorReporter devNullErrorReporter() {
    return (record, e) -> {};
}
```

**NOT Mocking:**

- Database connections: TestContainers ClickHouse container used instead
- Kafka infrastructure: TestContainers Kafka container used instead
- Network conditions: Toxiproxy container used for proxy/failure simulation

## Test Data & Fixtures

**Test Data Creation:**

```java
// ClickHouseSinkTaskTest.java
public Collection<SinkRecord> createDBTopicSplit(int dbRange, long timeStamp, String topic, int partition, String splitChar) {
    Gson gson = new Gson();
    List<SinkRecord> array = new ArrayList<>();
    LongStream.range(0, dbRange).forEachOrdered(i -> {
        String newTopic = i + "_" + timeStamp + splitChar + topic;
        LongStream.range(0, DEFAULT_TOTAL_RECORDS).forEachOrdered(n -> {
            Map<String, Object> value_struct = new HashMap<>();
            value_struct.put("str", "num" + n);
            value_struct.put("p_int32", (int)n);
            value_struct.put("p_float64", (double)n*1.111111);
            value_struct.put("p_bool", true);
            // ... more fields
            
            SinkRecord sr = new SinkRecord(
                newTopic, partition, null, null, null, gsonString, n,
                System.currentTimeMillis(), TimestampType.CREATE_TIME
            );
            array.add(sr);
        });
    });
    return array;
}
```

**Schema Construction (Fixtures):**

```java
// HeadersToValueTest.java
private SinkRecord generateSampleRecord(String topic, int partition, long offset, ConnectHeaders headers) {
    Schema schema = SchemaBuilder.struct()
        .field("off16", Schema.INT16_SCHEMA)
        .field("timestamp_int64", Schema.INT64_SCHEMA)
        .field("date_date", Schema.INT32_SCHEMA)
        .build();
    Struct valueStruct = new Struct(schema)
        .put("off16", (short) new Random().nextInt(Short.MAX_VALUE + 1))
        .put("timestamp_int64", new Date().getTime())
        .put("date_date", (int) (new Date().getTime() / 1000));
    return new SinkRecord(topic, partition, null, null, schema, valueStruct, offset, 0L,
            TimestampType.NO_TIMESTAMP_TYPE, headers);
}
```

**Location:**

- Test data creation methods within test classes
- Helper classes: `ClickHouseTestHelpers.java`, `ClickHouseAPI.java`
- Resource files: `src/test/resources` for JSON/configuration templates

## Coverage

**Requirements:** None enforced; coverage not configured in build

**View Coverage:**

```bash
gradle test jacocoTestReport  # If Jacoco plugin added
```

**Current State:**

- No coverage plugin configured in `build.gradle.kts`
- Tests focus on integration and acceptance rather than line coverage

## Test Types

**Unit Tests:**

- Scope: Individual components, transforms, utilities
- Approach: Direct instantiation, assertion on behavior
- No external services required
- Location: `src/test/java/com/clickhouse/kafka/connect/` and `src/test/java/transforms/`
- Examples:
  - `ConvertToMapOrJsonTest` - Transform logic
  - `HeadersToValueTest` - Header transformation
  - `TableOperationFilterTest` - Filter transform

**Integration Tests:**

- Scope: Full connector flow with Kafka, ClickHouse, network simulation
- Approach: TestContainers for services, real Kafka Connect framework
- External services: ClickHouse, Kafka, Toxiproxy
- Location: `src/integrationTest/java/`
- Examples:
  - `ClickHouseSinkConnectorIntegrationTest` - End-to-end data flow
  - `ExactlyOnceTest` - Deduplication semantics
  - `ClickHouseCloudTest` - Cloud deployment

**E2E Tests:**

- Not formally separated; integration tests serve as E2E

## Common Patterns

**Async Testing (Not Used):**

- JUnit Jupiter supports `@Timeout` annotation for async/slow tests
- Current tests are synchronous
- Example pattern if needed:

```java
@Test
@Timeout(5)  // 5-second timeout
void testAsync() {
    // Test code
}
```

**Error Testing:**

```java
// ClickHouseSinkTaskTest.java
@Test
public void testExceptionHandling() {
    ClickHouseSinkTask task = new ClickHouseSinkTask();
    assertThrows(RuntimeException.class, () -> task.put(null));
    
    try {
        task.put(null);
    } catch (Exception e) {
        assertEquals(e.getClass(), RuntimeException.class);
        assertTrue(e.getCause() instanceof NullPointerException);
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        assertTrue(sw.toString().contains("com.clickhouse.kafka.connect.util.Utils.handleException"));
    }
}
```

**Resource Management (Try-With-Resources):**

```java
// HeadersToValueTest.java
@Test
public void applySchemalessTest() {
    try (HeadersToValue<SinkRecord> headersToValue = new HeadersToValue<>()) {
        Map<String, Object> configs = new HashMap<>();
        configs.put("field", "headers_map");
        headersToValue.configure(configs);
        // Test assertions
    }
}
```

**TestContainer Lifecycle:**

```java
// ClickHouseSinkConnectorIntegrationTest.java
@BeforeAll
public static void setup() {
    Network network = Network.newNetwork();
    db = new ClickHouseContainer(...)
        .withNetwork(network)
        .withNetworkAliases("clickhouse");
    db.start();
    
    toxiproxy = new ToxiproxyContainer(...)
        .withNetwork(network)
        .withNetworkAliases("toxiproxy");
    toxiproxy.start();
}

@AfterAll
public static void tearDown() {
    db.stop();
    toxiproxy.stop();
    confluentPlatform.close();
}
```

## Test Execution Environment

**System Properties Set (build.gradle.kts lines 174, 186):**

- `file.encoding=windows-1252` - Windows encoding for tests
- `com.clickhouse.test.uri` - Optional test URI from system property

**JDK Toolchain:**

- Configurable via `-P javaVersion=<version>` (default: 17)
- Configurable via `-P jdkHome=<path>` for custom JDK home
- Launcher selected based on `languageVersion` (lines 182-184)

**Parallel Execution:**

- Forks limited to half available processor count
- Respects `maxParallelForks` configuration
- Each test run tracks `TestResult`: testCount, successfulTestCount, failedTestCount, skippedTestCount

---

*Testing analysis: 2026-09-15*
