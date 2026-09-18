# Phase 4: JSON Insert Path & Connector Plumbing - Pattern Map

**Mapped:** 2026-09-17
**Files analyzed:** 9 (1 main-code edit + 8 test files, 1 of which is an edit to an existing fork-only file)
**Analogs found:** 9 / 9

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|--------------------|------|-----------|-----------------|---------------|
| `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` (EDIT: extract `buildMBeanName`) | utility (package-private static extraction from a controller-adjacent class) | transform | `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:305-335` (`buildTableFilterClause` / `shouldSkipDescribeRow` extractions) | exact (same fork pattern: pull inline logic out of a `private` method into a package-private `static` helper, byte-for-byte output preserved) |
| New Docker-free unit test: MBean-name helper (JMX-01..03), plan 04-03 | test (unit) | transform (pure string formatting) | `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java` | exact (same shape: call a package-private static helper directly, table/enum-style assertions, no Docker) |
| New ClickHouse-backed live test: JMX-04 (multi-task MBean registration/unregistration), plan 04-03 | test (integration/live) | event-driven (register/unregister lifecycle) | `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java` (class shape) + `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java:85-97` (`cleanupLeakedMBeans`, the actual MBean-server query idiom) | role-match (live-class shape from the Live test; the concrete MBean-query mechanics come from `ClickHouseBase`) |
| New Docker-free unit test: `ClickHouseSinkConfig` parsing (COLS-01), plan 04-02 | test (unit) | transform (config string parsing) | `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java` | role-match (no existing `ClickHouseSinkConfig`-only test class exists; this is the closest Docker-free, non-`ClickHouseBase`, table/parameterized-assertion precedent) |
| New ClickHouse-backed live test: `jsonInsertColumns` (COLS-02..06), plan 04-02 | test (integration/live) | CRUD (insert + typed read-back) | `src/test/java/com/clickhouse/kafka/connect/sink/JsonDynamicRoundTripTest.java` | exact (same typed-read-back-via-`allRows` idiom, same `ClickHouseBase` lifecycle, same schemaless-record construction) |
| New Docker-free unit test: writer `cleanupExtraFields` bypass (JSON-03), plan 04-01 | test (unit) | transform | `src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java` | exact (bare `new ClickHouseWriter()`, calls `protected` methods directly, no `ClickHouseBase`) |
| New ClickHouse-backed live test: explicit JSON insert path (JSON-01, JSON-02, JSON-04), plan 04-01 | test (integration/live) | CRUD (insert + typed read-back) | `src/test/java/com/clickhouse/kafka/connect/sink/JsonDynamicRoundTripTest.java` (typed read-back) + `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java` (explicit-JSON prop setup, table-before-`start()` ordering) | exact (combination of both is the precise shape needed) |
| `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java` (EDIT for JSON-05), plan 04-01 | test (integration/live) | CRUD (insert + typed read-back) | itself (upgrade `countRows`-only assertions to typed read-back) + `JsonDynamicRoundTripTest.java`'s `allRows` helper | exact |
| New Docker-free unit test: `async_insert` config passthrough (ASYNC-01, ASYNC-02), plan 04-03 | test (unit) | transform (config string parsing) | `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java` | role-match (same rationale as the COLS-01 unit test — no dedicated config-test precedent exists yet) |

## Pattern Assignments

### `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` (main-code extraction, D-02)

**Analog:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:298-335`

**Current code being extracted** (`ProxySinkTask.java:71-79`, verbatim, must remain byte-for-byte identical output):
```java
private String getMBeanName() {
    String version = ClickHouseClientOption.class.getPackage().getImplementationVersion();
    if (version != null) {
        version = version.replace(':', '_');
    } else {
        version = "unknown";
    }
    return String.format("com.clickhouse:type=ClickHouseKafkaConnector,connector=%s,name=SinkTask%d,version=%s", connectorName, id, version);
}
```

**Extraction pattern to copy** (`ClickHouseHelperClient.java:298-335` — this fork's established shape for pulling inline logic into a package-private `static` helper with a Javadoc explaining the byte-for-byte-preservation intent):
```java
/**
 * Builds the {@code match(table, '...')} clause appended to the {@code system.columns} query
 * that backs {@link #showTables(String)}. Returns the empty string when {@code targetTableFilter}
 * is exactly the empty string (the connector default); otherwise doubles any single quote in the
 * filter before embedding it in the SQL literal. No backslash handling, no trim: this is a
 * byte-for-byte extraction of the previous inline ternary (D-19/FILT-01/FILT-03/FILT-04).
 */
static String buildTableFilterClause(String targetTableFilter) {
    return targetTableFilter.isEmpty()
            ? ""
            : String.format(" AND match(table, '%s')", targetTableFilter.replace("'", "''"));
}

static boolean shouldSkipDescribeRow(ClickHouseFieldDescriptor fieldDescriptor, Table table) {
    if (!fieldDescriptor.isSubcolumn()) {
        return false;
    }
    String name = fieldDescriptor.getName();
    if (name.endsWith(".null") || name.endsWith(".size0") || name.endsWith(".size")) {
        return true;
    }
    int lastDot = name.lastIndexOf('.');
    if (lastDot < 0) {
        return false;
    }
    String parentName = name.substring(0, lastDot);
    Column parent = table.getAllColumnsMap().get(parentName);
    return parent != null && (parent.getType() == Type.JSON || parent.getType() == Type.DYNAMIC);
}
```

**Target shape for `ProxySinkTask.java`** (package-private static extraction + one-line delegate, per D-02/RESEARCH.md "Code Examples"):
```java
static String buildMBeanName(String connectorName, int id, String version) {
    if (version != null) {
        version = version.replace(':', '_');
    } else {
        version = "unknown";
    }
    return String.format("com.clickhouse:type=ClickHouseKafkaConnector,connector=%s,name=SinkTask%d,version=%s", connectorName, id, version);
}

private String getMBeanName() {
    return buildMBeanName(connectorName, id, ClickHouseClientOption.class.getPackage().getImplementationVersion());
}
```
No other method, field, or import in `ProxySinkTask.java` changes. Import block (`ProxySinkTask.java:1-26`) stays as-is; no new imports needed since `ClickHouseClientOption` is already imported at line 3.

---

### New Docker-free unit test: MBean-name helper (JMX-01..03), plan 04-03

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java` and `src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java`

**Imports pattern** (`ClickHouseHelperClientForkUnitTest.java:1-14`):
```java
package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
...
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
```
For this new class, use `package com.clickhouse.kafka.connect.sink;` (same package as `ProxySinkTask`) since `buildMBeanName` is package-private — no cross-package visibility trick needed.

**Class-level Javadoc convention** (mirror `RowBinaryUnsupportedTypeGateTest.java:25-34`): state the requirement IDs covered, that this is a new fork-only class per D-16, and that it deliberately extends nothing so it stays in `testUnit`.

**Core pattern — direct static-helper calls, table-style assertions** (`ClickHouseHelperClientForkUnitTest.java:25-59`):
```java
@Test
@DisplayName("FILT-01: empty filter produces the empty clause")
void emptyFilterProducesEmptyClause() {
    assertEquals("", ClickHouseHelperClient.buildTableFilterClause(""));
}
```
Apply the same shape to `buildMBeanName`, e.g.:
```java
@Test
@DisplayName("JMX-01: connector name present renders connector=<name>")
void connectorNamePresentRendersInMBeanName() {
    String name = ProxySinkTask.buildMBeanName("my-connector", 3, "0.9.6");
    assertTrue(name.contains("connector=my-connector"), name);
}

@Test
@DisplayName("JMX-02: null connector name renders the literal connector=null")
void nullConnectorNameRendersLiteralNull() {
    String name = ProxySinkTask.buildMBeanName(null, 0, "0.9.6");
    assertTrue(name.contains("connector=null"), name);
}
```

**Parameterized pattern for JMX-03's two branches** (mirror `RowBinaryUnsupportedTypeGateTest.java:59-79`'s `@ParameterizedTest`/`@EnumSource` shape, adapted to `@ValueSource` per `ClickHouseHelperClientForkUnitTest.java:73-84`):
```java
@ParameterizedTest(name = "DESC-02: {0} subcolumn of an Array parent is skipped")
@ValueSource(strings = {".size0", ".size"})
@DisplayName("DESC-02: array-length subcolumn suffixes (.size0 and the D-20 bare .size) are skipped")
void arrayLengthSuffixesAreSkipped(String suffix) { ... }
```
Use this exact `@ParameterizedTest(name = "...")` + `@ValueSource`/`@DisplayName` triad for JMX-03's null-version (`version=unknown`) and colon-in-version (`_` substitution) cases.

---

### New ClickHouse-backed live test: JMX-04 (multi-task registration/unregistration), plan 04-03

**Analog (class shape + lifecycle):** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java`

**Analog (actual MBean-query mechanics):** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java:85-97`
```java
@AfterEach
protected void cleanupLeakedMBeans() {
    try {
        Set<ObjectName> leaked = ManagementFactory.getPlatformMBeanServer()
                .queryNames(new ObjectName("com.clickhouse:type=ClickHouseKafkaConnector,*"), null);
        for (ObjectName name : leaked) {
            LOGGER.warn("Unregistering leaked MBean left by a test: {}", name);
            MBeanServerUtils.unregisterMBean(name.toString());
        }
    } catch (MalformedObjectNameException e) {
        throw new RuntimeException(e);
    }
}
```
Reuse `ManagementFactory.getPlatformMBeanServer().queryNames(new ObjectName("com.clickhouse:type=ClickHouseKafkaConnector,*"), null)` to enumerate registered MBeans; per D-04, **filter the returned set to this test's own connector names** (built via `uniqueConnectorName(TestInfo)`) before asserting distinctness, since `ClickHouseBase`'s own `@AfterEach` sweep runs against the same server and other tests' MBeans may transiently coexist.

**Imports pattern** (`ClickHouseHelperClientForkLiveTest.java:1-27`, adapted):
```java
package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.Set;
import javax.management.ObjectName;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
```

**Core pattern — start multiple tasks, query, assert distinctness, stop one, assert removal** (structural analog: `ClickHouseHelperClientForkLiveTest.java:88-157`'s try/finally-around-multiple-tasks shape):
```java
Map<String, String> propsA = createProps();
propsA.put("name", uniqueConnectorName(testInfo) + "-A");
ClickHouseSinkTask taskA = new ClickHouseSinkTask();
taskA.start(propsA);
try {
    Map<String, String> propsB = createProps();
    propsB.put("name", uniqueConnectorName(testInfo) + "-B");
    ClickHouseSinkTask taskB = new ClickHouseSinkTask();
    taskB.start(propsB);
    try {
        Set<ObjectName> mine = ManagementFactory.getPlatformMBeanServer()
                .queryNames(new ObjectName("com.clickhouse:type=ClickHouseKafkaConnector,*"), null);
        // filter `mine` down to names containing propsA/propsB's connector names, assert 2 distinct entries
        taskB.stop();
        // re-query, assert B's entry is gone and A's is still present
    } finally {
        taskB.stop(); // idempotent per MBeanServerUtils.unregisterMBean's isRegistered guard
    }
} finally {
    taskA.stop();
}
```

**try/finally + table-before-start() convention** (D-17, from `ClickHouseSinkTaskJsonInsertFormatTest.java:59-78` and `JsonDynamicRoundTripTest.java:71-99`): create any table before `start()`, wrap `start()`/`put()`/`stop()` interaction in `try/finally` so `stop()` always runs even on assertion failure.

---

### New Docker-free unit test: `ClickHouseSinkConfig` parsing (COLS-01), plan 04-02

**Analog (shape only — no dedicated config-test class exists in the repo today):** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java`

**Imports pattern:**
```java
package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
```

**Core pattern — construct `ClickHouseSinkConfig` directly from a `Map<String,String>`, no Docker, no `ClickHouseBase`:**
```java
Map<String, String> props = new HashMap<>();
props.put(ClickHouseSinkConnector.HOSTNAME, "localhost");
props.put(ClickHouseSinkConnector.PORT, "8443");
props.put(ClickHouseSinkConnector.DATABASE, "default");
props.put(ClickHouseSinkConfig.JSON_INSERT_COLUMNS, "  col_a, , col_b ,col_c  ");
ClickHouseSinkConfig csc = new ClickHouseSinkConfig(props);
assertEquals(List.of("col_a", "col_b", "col_c"), csc.getJsonInsertColumns());
```
Source contract being pinned (`ClickHouseSinkConfig.java:241-252`, read this session):
```java
String jsonInsertColumnsRaw = props.getOrDefault(JSON_INSERT_COLUMNS, "").trim();
List<String> jsonInsertColumnsTmp = new ArrayList<>();
if (!jsonInsertColumnsRaw.isBlank()) {
    String[] stringSplit = jsonInsertColumnsRaw.split(",");
    for (String column : stringSplit) {
        String trimmed = column.trim();
        if (!trimmed.isEmpty()) {
            jsonInsertColumnsTmp.add(trimmed);
        }
    }
}
this.jsonInsertColumns = jsonInsertColumnsTmp;
```
Use `@ParameterizedTest`/`@ValueSource` (mirroring `ClickHouseHelperClientForkUnitTest.java:73-84`) for the blank/whitespace-only/unset cases that must all yield an empty (never `null`) list.

**Note on minimal required props:** `ClickHouseSinkConfig`'s constructor validates hostname/port/database presence — check the constructor (`ClickHouseSinkConfig.java`, full file already read this session) for the minimum prop set needed to avoid an unrelated `ConfigException`, and prefer reusing `ClickHouseBase.createProps()`-shaped literal values (not `ClickHouseBase` itself) to stay Docker-free.

---

### New ClickHouse-backed live test: `jsonInsertColumns` (COLS-02..06), plan 04-02

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/JsonDynamicRoundTripTest.java` (full file read this session)

**Imports pattern** (`JsonDynamicRoundTripTest.java:1-27`):
```java
package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
```

**Core pattern — schemaless record + typed read-back** (`JsonDynamicRoundTripTest.java:38-100`, the `jsonAndDynamicRoundTripThroughSchemalessRoute` test — this is D-06's exact template: build a `SinkRecord` whose value is a `Map<String,Object>`, not a `Struct`):
```java
String tableName = createTopicName("cols_explicit_subset");
Map<String, String> props = createProps();
ClickHouseHelperClient chc = createClient(props);
ClickHouseTestHelpers.createTable(chc, tableName,
        "CREATE TABLE %s (id UInt32, listed Int32, extra Int32 DEFAULT 42, tag String DEFAULT 'unset') ENGINE = MergeTree ORDER BY id");
props.put("name", uniqueConnectorName(testInfo));
props.put(ClickHouseSinkConfig.JSON_INSERT_COLUMNS, "id,listed");

try {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("id", 1);
    value.put("listed", 7);
    List<SinkRecord> records = List.of(new SinkRecord(tableName, 0, null, null, null, value, 1));

    ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
    sinkTask.start(props);
    sinkTask.put(records);
    sinkTask.stop();

    List<Map<String, String>> rows = allRows(chc,
            String.format("SELECT id, listed, extra, tag FROM `%s` ORDER BY id", tableName),
            "id", "listed", "extra", "tag");
    assertEquals(1, rows.size());
    assertEquals("7", rows.get(0).get("listed"));
    assertEquals("42", rows.get(0).get("extra"));   // D-12: non-trivial DEFAULT proves it was never written
    assertEquals("unset", rows.get(0).get("tag"));
} finally {
    ClickHouseTestHelpers.dropTable(chc, tableName);
}
```

**Typed read-back helper to copy verbatim (D-14, "never `countRows` alone")** — `JsonDynamicRoundTripTest.java:573-586`:
```java
/**
 * Snapshots each row's requested columns into a plain {@code Map<String, String>} while the
 * row is still the iterator's current element. The client-v2 {@link GenericRecord} returned
 * by {@link Records}'s iterator is a mutable, reused view over the underlying binary reader
 * buffer - collecting the {@code GenericRecord} references themselves (rather than reading
 * their values immediately) leaves every collected reference pointing at whatever row the
 * iterator most recently advanced to, silently aliasing every "row" to the last one read.
 */
private static List<Map<String, String>> allRows(ClickHouseHelperClient chc, String sql, String... columns)
        throws Exception {
    List<Map<String, String>> rows = new ArrayList<>();
    try (Records records = chc.getClient().queryRecords(sql).get()) {
        for (GenericRecord r : records) {
            Map<String, String> row = new LinkedHashMap<>();
            for (String column : columns) {
                row.put(column, r.getString(column));
            }
            rows.add(row);
        }
    }
    return rows;
}
```

**Negative-test pattern for COLS-05** (nonexistent column, D-10/D-17 — exception type + message substring): mirror `ClickHouseHelperClientForkLiveTest.java:88-109`'s `assertThrows(RuntimeException.class, () -> chst.put(records))` + `messageChainContains(ex, "...")` idiom:
```java
RuntimeException ex = assertThrows(RuntimeException.class, () -> chst.put(records));
assertTrue(messageChainContains(ex, "no_such_column"), "message chain: " + ex);
assertEquals(0, ClickHouseTestHelpers.countRows(chc, tableName)); // no partial insert
```
Copy the `messageChainContains` helper verbatim from `ClickHouseHelperClientForkLiveTest.java:306-316`:
```java
private static boolean messageChainContains(Throwable t, String substring) {
    while (t != null) {
        String message = t.getMessage();
        if (message != null && message.contains(substring)) {
            return true;
        }
        t = t.getCause();
    }
    return false;
}
```

---

### New Docker-free unit test: writer `cleanupExtraFields` bypass (JSON-03), plan 04-01

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java` (full file read this session)

**Imports pattern** (`RowBinaryUnsupportedTypeGateTest.java:1-23`):
```java
package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
```

**Core pattern — bare `new ClickHouseWriter()`, drive the `protected` seam directly, no `ClickHouseBase`** (structural analog: `RowBinaryUnsupportedTypeGateTest.java:37-57`; the seam itself is `ClickHouseWriter.java:81-82`):
```java
// Source: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:81-82
// protected void setSinkConfig(ClickHouseSinkConfig csc) { this.csc = csc; }

ClickHouseWriter writer = new ClickHouseWriter();
Table table = new Table("test_db", "test_table");
table.addColumn(Column.builder().name("known_col").type(Type.STRING).build());

Map<String, Object> payload = new HashMap<>();
payload.put("known_col", "value");
payload.put("unknown_col", "should_be_stripped_by_default");

// Default config (no insertFormat=json): unknown keys stripped.
Map<String, String> defaultProps = new HashMap<>(minimalRequiredProps());
writer.setSinkConfig(new ClickHouseSinkConfig(defaultProps));
Map<String, Object> cleanedDefault = writer.cleanupExtraFields(payload, table);
assertEquals(Map.of("known_col", "value"), cleanedDefault);

// insertFormat=json (metadata-free): payload passed through untouched (JSON-03's mechanism).
Map<String, String> jsonProps = new HashMap<>(minimalRequiredProps());
jsonProps.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
writer.setSinkConfig(new ClickHouseSinkConfig(jsonProps));
Map<String, Object> cleanedJson = writer.cleanupExtraFields(payload, table);
assertEquals(payload, cleanedJson);
```
Source contract being pinned (`ClickHouseWriter.java:1039-1051`, verbatim, read this session):
```java
protected Map<String, Object> cleanupExtraFields(Map<String, Object> m, Table t) {
    if (csc.isBypassFieldCleanup() || shouldSkipTableMetadata()) {
        return m;
    }

    Map<String, Object> cleaned = new HashMap<>();
    for (Column c : t.getRootColumnsList()) {
        if (m.containsKey(c.getName())) {
            cleaned.put(c.getName(), m.get(c.getName()));
        }
    }
    return cleaned;
}
```

---

### New ClickHouse-backed live test: explicit JSON insert path (JSON-01, JSON-02, JSON-04), plan 04-01

**Analog (typed read-back + class shape):** `src/test/java/com/clickhouse/kafka/connect/sink/JsonDynamicRoundTripTest.java`
**Analog (explicit-JSON prop setup + table-before-start ordering):** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java`

**`insertFormat=json` props pattern** (`ClickHouseSinkTaskJsonInsertFormatTest.java:24-28`):
```java
private Map<String, String> createJsonInsertProps() {
    Map<String, String> props = createProps();
    props.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
    return props;
}
```
For JSON-01's letter-case matrix, parameterize the literal value (`"json"`, `"JSON"`, `"Json"`, `"  json  "`) using `@ParameterizedTest`/`@ValueSource` (mirror `ClickHouseHelperClientForkUnitTest.java:73-84`), and assert on `new ClickHouseSinkConfig(props).isJsonInsertFormatExplicit()` directly — a Docker-free assertion co-located in this live class is fine, but the metadata-skip *behavior* (JSON-02) needs the live path below.

**Table-before-`start()` and metadata-skip pattern** (`ClickHouseSinkTaskJsonInsertFormatTest.java:59-78` — this is D-17's ordering requirement in its purest existing form: table dropped, `start()` called and does NOT throw even though the table doesn't exist yet, table created only afterward, then `put()`):
```java
@Test
public void explicitJsonInsertSkipsTableCheckOnStart() {
    Map<String, String> props = createJsonInsertProps();
    ClickHouseHelperClient chc = createClient(props);

    String topic = createTopicName("json_insert_skip_mapping_test");
    String tableName = topic;
    ClickHouseTestHelpers.dropTable(chc, tableName);

    ClickHouseSinkTask chst = new ClickHouseSinkTask();
    assertDoesNotThrow(() -> chst.start(props));

    createTable(chc, tableName);
    Collection<SinkRecord> records = createJsonRecords(topic, 0, RECORD_COUNT);

    chst.put(records);
    chst.stop();

    assertEquals(records.size(), ClickHouseTestHelpers.countRows(chc, tableName));
}
```
For the new class, replace the String-record `createJsonRecords` helper with **schemaless `Map`-record construction** per D-06 (see the `JsonDynamicRoundTripTest.java:49-69` value-map-building style above), since only the schemaless route actually exercises metadata-free `getTable`/`cleanupExtraFields` behavior — the String route in the existing test never touches either mechanism.

**Tombstone / mixed-batch pattern for JSON-04:** build a `SinkRecord` list where at least one record has `null` value (`new SinkRecord(topic, partition, null, null, null, null, offset)`), asserting via `EmptyRecordConvertor` semantics (`src/main/java/com/clickhouse/kafka/connect/sink/data/convert/EmptyRecordConvertor.java`, verified this session to convert tombstones to a `SCHEMA_LESS` record with a null value) that `put()` does not throw and only the non-null rows land (typed read-back, not `countRows` alone, per D-14). For the all-tombstone case, D-09 requires pinning whatever `client.insert(...)` actually does with an empty JSONEachRow body — write the test to assert on the *actual* outcome (0 rows, no throw, per the research session's inference) rather than assuming it.

---

### `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java` (EDIT for JSON-05), plan 04-01

**Analog:** itself (current state, full file read this session, reproduced above) + the `allRows` helper from `JsonDynamicRoundTripTest.java:573-586` (copy verbatim into this file or a shared location the plan decides).

**Current state to change** (`ClickHouseSinkTaskJsonInsertFormatTest.java:76-77` and `:98-99`):
```java
assertEquals(records.size(), ClickHouseTestHelpers.countRows(chc, tableName));
```
appears twice — once in `explicitJsonInsertSkipsTableCheckOnStart` and once in `explicitJsonInsertIgnoresTargetTableFilter`. **Both** must gain typed column-value assertions (D-06/JSON-05) in addition to (not instead of) the existing row-count check, e.g.:
```java
List<Map<String, String>> rows = allRows(chc,
        String.format("SELECT off16, str FROM `%s` ORDER BY off16", tableName), "off16", "str");
assertEquals(RECORD_COUNT, rows.size());
for (int i = 0; i < RECORD_COUNT; i++) {
    assertEquals(String.valueOf(i), rows.get(i).get("off16"));
    assertEquals("num" + i, rows.get(i).get("str"));
}
```
This is a **fork-only file edit**, not a new file — per D-16 this is the single named exception where 04-01 edits an existing test class.

---

### New Docker-free unit test: `async_insert` config passthrough (ASYNC-01, ASYNC-02), plan 04-03

**Analog (shape):** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java`

**Core pattern — assert on `ClickHouseSinkConfig.getClickhouseSettings()` directly, no Docker:**
```java
Map<String, String> props = minimalRequiredProps();
ClickHouseSinkConfig defaultConfig = new ClickHouseSinkConfig(props);
assertFalse(defaultConfig.getClickhouseSettings().containsKey("async_insert")); // ASYNC-01

Map<String, String> asyncProps = new HashMap<>(props);
asyncProps.put(ClickHouseSinkConfig.CLICKHOUSE_SETTINGS, "async_insert=1");
ClickHouseSinkConfig asyncConfig = new ClickHouseSinkConfig(asyncProps);
assertEquals("1", asyncConfig.getClickhouseSettings().get("async_insert")); // ASYNC-02
```
Source contract being pinned (`ClickHouseSinkConfig.java:207-211`, verbatim, read this session):
```java
this.addClickHouseSetting("input_format_skip_unknown_fields", "1", false);
this.addClickHouseSetting("wait_end_of_query", "1", false);
//this.addClickHouseSetting("async_insert", "0", false);
//We set this so our ResponseSummary has actual data in it
this.addClickHouseSetting("send_progress_in_http_headers", "1", false);
```
ASYNC-03's live half (`async_insert=1,wait_for_async_insert=1`) belongs in the same live class used for JSON-01/02/04 or a small dedicated live test reusing the `JsonDynamicRoundTripTest.java` typed-read-back pattern — no polling loop, per D-15.

## Shared Patterns

### `ClickHouseBase` lifecycle contract (fresh database per class, `uniqueConnectorName`, MBean-leak sweep)

**Source:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java`
**Apply to:** every live test class in this phase (JMX-04, COLS-02..06, JSON-01/02/04, and the edited `ClickHouseSinkTaskJsonInsertFormatTest`)

```java
// ClickHouseBase.java:51-67 — one shared container per JVM fork, fresh database name per class
@BeforeAll
public static void setup() throws IOException {
    database = newTestDatabaseName();
    ...
    if (db == null) {
        db = new ClickHouseContainer(ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE)
                .withImagePullPolicy(PullPolicy.alwaysPull())
                .withPassword("test_password")
                .withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1");
        db.start();
        SHARED_CONTAINER_STARTS.incrementAndGet();
    }
}

// ClickHouseBase.java:99-106 — opt-in unique connector name for new/touched tests (D-10)
protected String uniqueConnectorName(TestInfo testInfo) {
    String methodName = testInfo.getTestMethod().map(java.lang.reflect.Method::getName).orElse("unknown");
    return getClass().getSimpleName() + "-" + methodName + "-" + CONNECTOR_NAME_COUNTER.incrementAndGet();
}

// ClickHouseBase.java:85-97 — safety-net MBean sweep; JMX-04 queries the SAME server this uses,
// so JMX-04 MUST filter to its own connector names rather than counting globally
@AfterEach
protected void cleanupLeakedMBeans() { ... }
```
Every new live test must call `createProps()` → `createClient(props)` → create the table → set `props.put("name", uniqueConnectorName(testInfo))` before `start()`.

### Typed SQL read-back (D-14) — never `countRows` alone

**Source:** `src/test/java/com/clickhouse/kafka/connect/sink/JsonDynamicRoundTripTest.java:573-586` (`allRows` helper, quoted in full above)
**Apply to:** every live test asserting insert correctness in this phase (JSON-01/02/04/05, COLS-02..06, ASYNC-03) — a bare `ClickHouseTestHelpers.countRows(...)` call is never sufficient on its own per D-17.

### try/finally around `start()`/`stop()`, table created before `start()` (D-17)

**Source:** `src/test/java/com/clickhouse/kafka/connect/sink/JsonDynamicRoundTripTest.java:40-100`, `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java:88-157`
**Apply to:** every live test in this phase.
```java
ClickHouseTestHelpers.createTable(chc, tableName, ddl); // table exists before start(), unless insertFormat=json (JSON-02's exception)
...
ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
sinkTask.start(props);
try {
    sinkTask.put(records);
} finally {
    sinkTask.stop();
}
```

### `REQ-ID: ` `@DisplayName` prefix and negative-test shape (Phase 2 D-10/D-16, carried by D-17)

**Source:** every existing test file read this session (`ClickHouseHelperClientForkUnitTest.java`, `ClickHouseHelperClientForkLiveTest.java`, `RowBinaryUnsupportedTypeGateTest.java`)
**Apply to:** every new test method in this phase — `@DisplayName("JSON-01: ...")`, `@DisplayName("COLS-05: ...")`, etc.
```java
@Test
@DisplayName("FILT-03: a targetTableFilter containing a single quote parses without a ClickHouse syntax error")
```
Negative tests assert exception type **plus** a message substring, never full-string equality:
```java
RuntimeException ex = assertThrows(RuntimeException.class, () -> chst.put(records));
assertTrue(messageChainContains(ex, "does not exist"), "message chain: " + ex);
```

### `ClickHouseWriter.setSinkConfig` seam (D-08's Docker-free enabler)

**Source:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:81-82`
**Apply to:** the JSON-03 `cleanupExtraFields` unit test only.
```java
protected void setSinkConfig(ClickHouseSinkConfig csc) {
    this.csc = csc;
}
```
Already `protected` and already exists — no main-code change needed to reach it from a same-package test class (`com.clickhouse.kafka.connect.sink.db`).

## No Analog Found

None. Every file in scope has at least a role-match analog; the two "role-match" (not "exact") entries — the `ClickHouseSinkConfig`-parsing unit tests for COLS-01 and ASYNC-01/02 — have no dedicated prior `ClickHouseSinkConfig` test class to copy from (none exists in the repo), but the Docker-free unit-test *shape* (`ClickHouseHelperClientForkUnitTest`) transfers directly since the pattern (construct a POJO from a `Map<String,String>`, assert on a getter) is identical regardless of which class is under test.

## Metadata

**Analog search scope:** `src/test/java/com/clickhouse/kafka/connect/sink/` (and its `db`, `db/helper`, `db/mapping`, `helper` subpackages), `src/main/java/com/clickhouse/kafka/connect/sink/` (`ProxySinkTask.java`, `db/ClickHouseWriter.java`, `db/helper/ClickHouseHelperClient.java`, `ClickHouseSinkConfig.java`, `ClickHouseSinkTask.java`), `src/main/java/com/clickhouse/kafka/connect/util/` (`Utils.java`, `jmx/MBeanServerUtils.java`)
**Files scanned:** 15 (full reads) — `ProxySinkTask.java`, `ClickHouseHelperClient.java` (partial, lines 290-340), `RowBinaryUnsupportedTypeGateTest.java`, `ClickHouseHelperClientForkUnitTest.java`, `ClickHouseHelperClientForkLiveTest.java`, `ClickHouseSinkTaskJsonInsertFormatTest.java`, `ClickHouseBase.java`, `JsonDynamicRoundTripTest.java` (partial), `ClickHouseWriter.java` (partial, lines 75-90), `ClickHouseSinkConfig.java` (grep + partial reads), plus everything already fully read during the phase's own RESEARCH.md session (`MBeanServerUtils.java`, `ClickHouseSinkTask.java`, `Utils.java`, `EmptyRecordConvertor.java`)
**Pattern extraction date:** 2026-09-17
**Tracked-source gate:** All analog paths verified via `git ls-files` this session — every path cited above is git-tracked source, none are `.gsd/`-mirror paths.
