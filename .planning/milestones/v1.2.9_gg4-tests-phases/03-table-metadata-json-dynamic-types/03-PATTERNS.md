# Phase 3: Table Metadata & JSON/Dynamic Types - Pattern Map

**Mapped:** 2026-09-17
**Files analyzed:** 7 (2 main-code files touched, 5 new fork-only test classes across the 03-01/03-02/03-03 plan split)
**Analogs found:** 7 / 7

All analog paths below were verified git-tracked via `git ls-files` (tracked-source gate, #3645) — none are
gitignored install/runtime mirrors.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|--------------------|------|-----------|-----------------|----------------|
| `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` (modify: extract `shouldSkipDescribeRow`, extract `buildTableFilterClause`, warn→error, conditional UTF-8) | service (DB metadata helper) | request-response | itself (same file, pre-change) | exact — editing in place, patterns below extracted from its own current code |
| `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (modify: `doWriteColValue` default-branch throw, `validateDataSchema` JSON/DYNAMIC/UNKNOWN check) | service (RowBinary writer) | streaming | itself (same file, pre-change) — see the existing `DataException` throw at `ClickHouseWriter.java:393-397` for the exact idiom to reuse | exact |
| New unit test: skip-rule + filter-clause (plan 03-01), e.g. `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ShouldSkipDescribeRowTest.java` | test (unit, Docker-free) | transform (pure function pin) | `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java` | exact — same package family, same `newDescriptor`/`extractColumn` seam, same plain-JUnit-no-base-class shape |
| New live test: FILT-01..04 + DESC-04 (plan 03-01), e.g. `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/TargetTableFilterLiveTest.java` | test (ClickHouse-backed) | request-response (DESCRIBE/SELECT queries) | `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientTest.java` | exact — `showTables`/`describeTable`/`ignoreArrayWithNestedTable` fixtures live in exactly this file today |
| New unit test: TYPE-01/02 type-string pin (plan 03-02), can live in the same skip-rule unit class or a sibling, e.g. `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/JsonDynamicTypeMappingTest.java` | test (unit, Docker-free) | transform | `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java` | exact |
| New live test: JSON/Dynamic round-trip + non-ASCII (plan 03-02), e.g. `src/test/java/com/clickhouse/kafka/connect/sink/JsonDynamicRoundTripTest.java` | test (ClickHouse-backed) | CRUD (insert + typed read-back) | `src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java` (insert/config shape) + `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` (`getAllRowsAsJson`, `countRows`) | role-match — `JsonColumnTest` covers the JSON *table*/SMT interaction, not typed read-back, so it is the closest structural analog but D-14's typed-query assertions are new |
| New tests: TYPE-06 unit gates + live fail-fast (plan 03-03), e.g. `src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryFailFastTest.java` (unit) + a live class extending `ClickHouseBase` | test (unit, Docker-free) / test (ClickHouse-backed) | streaming (protected-method pin) / CRUD (put→countRows) | `src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java` (direct calls to `doWriteColValue`/`doWritePrimitive`, byte-capturing `ClickHousePipedOutputStream` subclass) + `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskMappingTest.java` (put→countRows shape) | exact for the unit half, role-match for the live half |

Naming above is illustrative only — D-16's "Claude's Discretion" leaves exact class names/packages and the
03-01/03-02/03-03 file split to the planner/executor. What must not change: every new test class is **new**
(D-16), none extend or edit `ClickHouseHelperClientTest`, `ColumnTest`, or `ClickHouseSinkTaskMappingTest`.

## Pattern Assignments

### `ClickHouseHelperClient.java` — extract `shouldSkipDescribeRow` (D-06/D-07/D-08/D-20)

**Analog:** itself, current `describeTableV1`/`V2` skip block (duplicated verbatim)

**Current skip condition** (`ClickHouseHelperClient.java:379` and `:426`, byte-identical):
```java
if (fieldDescriptor.isSubcolumn() && (fieldDescriptor.getName().endsWith(".null") || fieldDescriptor.getName().endsWith(".size0")) || fieldDescriptor.getName().contains(".object_shared_data.")) {
    LOGGER.debug("Skipping column {} as it is a system sub-column", fieldDescriptor.getName());
    continue;
}
```
Two bugs to fix while extracting (both called out in D-07/D-20): the `.object_shared_data.` clause is not
guarded by `isSubcolumn()` (operator precedence), and it requires a trailing dot so a bare
`json_col.object_shared_data` row slips through.

**Call sites to replace** (both `describeTableV1` and `describeTableV2`, identical shape):
```java
// ClickHouseHelperClient.java:373 / :420 — inside the while(readLine) loop, right after the fromJsonRow parse
ClickHouseFieldDescriptor fieldDescriptor = ClickHouseFieldDescriptor.fromJsonRow(line);
if (fieldDescriptor.isAlias() || fieldDescriptor.isMaterialized() || fieldDescriptor.isEphemeral()) {
    LOGGER.debug("Skipping column {} as it is an alias or materialized view or ephemeral", fieldDescriptor.getName());
    continue;
}
// <-- shouldSkipDescribeRow(fieldDescriptor, table) replaces the two-bug block that follows here
```
`table` is already in scope at both call sites (`Table table = new Table(database, tableName);` a few lines
above), which is what makes the parent-type lookup possible via `table.getAllColumnsMap()` (class-level
`@Getter` on `Table`, confirmed at `Table.java:17,29`) — the parent column for a subcolumn named
`json_col.a.b` is `json_col`, added to `allColumnsMap` before its children are processed since `describeTable`
reads rows in DESCRIBE's parent-before-child order.

**New helper signature** (visibility is Claude's discretion, D-01 item 3 — package-private is enough for a
Docker-free unit test in the same package):
```java
static boolean shouldSkipDescribeRow(ClickHouseFieldDescriptor fieldDescriptor, Table table) {
    String name = fieldDescriptor.getName();
    if (!fieldDescriptor.isSubcolumn()) {
        return false;
    }
    if (name.endsWith(".null") || name.endsWith(".size0") || name.endsWith(".size")) {  // D-20: bare .size added
        return true;
    }
    String parentName = name.substring(0, name.lastIndexOf('.'));
    Column parent = table.getAllColumnsMap().get(parentName);
    return parent != null && (parent.getType() == Type.JSON || parent.getType() == Type.DYNAMIC);
}
```
This is prose guidance, not a literal diff to paste — the executor should read `Type.java` and `Column.java`
imports already present in `ClickHouseHelperClient.java` (it imports `Column`, not yet `Type`; add the import).
Note D-20's finding: **Map and Tuple sub-columns must still flow through** — this rule only skips
`.null`/`.size0`/`.size` suffixes and JSON/Dynamic-parented rows, nothing else, so `Table.updateParent`
(`Table.java:93-182`) keeps consuming Map/Tuple subcolumns exactly as today.

**Log-level change** (D-09, both methods):
```java
// ClickHouseHelperClient.java:391 and :438
LOGGER.warn("Unable to handle column: {}", fieldDescriptor.getName());
// becomes
LOGGER.error("Unable to handle column: {}", fieldDescriptor.getName());
```

### `ClickHouseHelperClient.java` — extract `buildTableFilterClause` (D-10/D-19)

**Analog:** itself, `showTables()`

**Current inline construction** (`ClickHouseHelperClient.java:297`):
```java
public List<Table> showTables(String database) {
    String filterClause = targetTableFilter.isEmpty() ? "" : String.format(" AND match(table, '%s')", targetTableFilter.replace("'", "''"));
    ...
```
**Extraction target** (D-19: escaping logic stays exactly `'` → `''`, no backslash change):
```java
static String buildTableFilterClause(String targetTableFilter) {
    return targetTableFilter.isEmpty()
            ? ""
            : String.format(" AND match(table, '%s')", targetTableFilter.replace("'", "''"));
}
```
`showTables()` then calls `buildTableFilterClause(targetTableFilter)` in place of the inline ternary. FILT-01
becomes `assertEquals("", buildTableFilterClause(""))`; FILT-03 becomes an exact-string assertion on the
escaped-quote case; FILT-04 is a **live pin only** (per D-19/R-03 — do not add a unit test asserting a doubled
backslash, since none ships).

### `ClickHouseHelperClient.java` — conditional UTF-8 fix (D-15, item 5)

**Analog:** itself, `ClickHouseTestHelpers.getAllRowsAsJson`/`validateRows` (already fixed under Phase 1 D-11)

**Current unguarded readers** (`ClickHouseHelperClient.java:370` and `:417`):
```java
try (BufferedReader br = new BufferedReader(new InputStreamReader(queryResponse.getInputStream()))) {
```
**Fixed form, mirroring the already-applied test-helper fix** (`ClickHouseTestHelpers.java:159-160`):
```java
BufferedReader reader = new BufferedReader(
        new InputStreamReader(queryResponse.getInputStream(), StandardCharsets.UTF_8));
```
`StandardCharsets` is not yet imported in `ClickHouseHelperClient.java` — add
`import java.nio.charset.StandardCharsets;`. Per D-15, only apply this if the TYPE-07 identifier test proves
the bug (a non-ASCII column name, `Enum` member, or DEFAULT expression comes back mangled under the
`windows-1252` test JVM) — write the test first, then decide.

---

### `ClickHouseWriter.java` — `doWriteColValue()` default-branch throw (D-02/D-03)

**Analog:** itself — the exact `DataException` idiom already used two methods above in the same file

**Existing idiom to copy** (`ClickHouseWriter.java:393-397`, inside `doWritePrimitive`'s date/time handling):
```java
if (unsupported) {
    String msg = String.format("(Potentially) Not implemented conversion from %s to %s", value.getFieldType(), type);
    LOGGER.error(msg);
    throw new DataException(msg);
}
```
`org.apache.kafka.connect.errors.DataException` needs importing in `ClickHouseWriter.java` (it is not currently
imported there — confirmed absent from the file's import block; it *is* already imported and used the same way
in `ClickHouseHelperClientTest.java:60,396,559` for reference on package/class name).

**Target block to replace** (`ClickHouseWriter.java:535-539`):
```java
default:
    // If you wonder, how NESTED works in JDBC:
    // https://github.com/ClickHouse/clickhouse-java/blob/6cbbd8fe3f86ac26d12a95e0c2b964f3a3755fc9/clickhouse-data/src/main/java/com/clickhouse/data/format/ClickHouseRowBinaryProcessor.java#L159
    LOGGER.error("Cannot serialize unsupported type {}", columnType);
```
Per D-03, this must throw for **every** `Type` that falls through (including `Type.UNKNOWN`, not just
`JSON`/`DYNAMIC`), and per D-05 the message must name the column, its type, and both escape hatches
(`insertFormat=json`, `bypassRowBinary=true`). Suggested shape (wording is Claude's discretion beyond those
four elements):
```java
default:
    String msg = String.format(
            "Column [%s] has unsupported type [%s] for RowBinary serialization. "
                    + "Use insertFormat=json or bypassRowBinary=true instead.",
            col.getName(), columnType);
    LOGGER.error(msg);
    throw new DataException(msg);
```

**Direct-call unit-test pattern** (this is exactly how D-06's Docker-free half must invoke the protected
method — copy the construction and output-stream-capturing shape, not the ClickHouse container setup):
```java
// Source: src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java:127-224
// (doWriteColValue_Tuples) — this test extends ClickHouseBase only to obtain a real table/topic name;
// the actual call under test needs no Docker:
ClickHouseWriter chw = new ClickHouseWriter();
Column column = Column.builder().name("json_col").type(Type.JSON).build();
Data data = new Data(schema, someValue);
ClickHousePipedOutputStream out = new ClickHousePipedOutputStream(null) { /* byte-capturing overrides, lines 171-214 */ };
DataException ex = assertThrows(DataException.class, () -> chw.doWriteColValue(column, out, data, true));
assertTrue(ex.getMessage().contains("insertFormat=json"));
```
The new Docker-free TYPE-06 unit class must **not** extend `ClickHouseBase` (that pulls in `@Tag("clickhouse")`
and excludes it from `testUnit`, per D-16/Architecture Pattern 2) — construct `Column`/`Data`/`Type.UNKNOWN`
directly the way `ColumnTest`/`ClickHouseWriterTest`'s `doWriteColValue_Tuples` build `Column`/`Data` objects,
skip the container-derived table/topic entirely.

### `ClickHouseWriter.java` — `validateDataSchema()` JSON/DYNAMIC/UNKNOWN check (D-04)

**Analog:** itself, the existing `switch (colTypeName)` inside `validateDataSchema`

**Current gate** (`ClickHouseWriter.java:747-748`, confirmed by R-02 to fire first under default config):
```java
if (!csc.isBypassSchemaValidation() && !validateDataSchema(table, first, false))
    throw new RuntimeException("Data schema validation failed.");
```
**Insertion point inside the loop** (`ClickHouseWriter.java:244-276`, the `switch (colTypeName)` that currently
has no `JSON`/`DYNAMIC`/`UNKNOWN` case and falls into `default:` doing a generic type-name string comparison):
```java
switch (colTypeName) {
    case "Date":
    case "Date32":
    ...
        break;
    default:
        if (!colTypeName.equals(dataTypeName)) {
            ...
            validSchema = false;
            LOGGER.error(String.format("Table column name [%s] type [%s] is not matching data column type [%s]", col.getName(), colTypeName, dataTypeName));
        }
}
```
Add a `JSON`/`DYNAMIC`/`UNKNOWN` case **before** the `default:` that throws the same D-05-worded message as the
writer-gate fix (share the message-building logic if convenient — same column name, same type, same two escape
hatches) rather than just flipping `validSchema = false` silently, per D-04's "same message" requirement.

**Live ground-truth to contrast against in the test's `@DisplayName`/comments** (R-02, exact text captured this
session — use as the "before" fixture in a code comment, not a literal string assertion since D-17 requires
substring-only checks):
```
RuntimeException: Number of records: 1
  caused by RuntimeException: Topic: [...], Partition: [0], MinOffset: [0], MaxOffset: [0], (QueryId: [...])
  caused by RuntimeException: Data schema validation failed.
```

---

### New unit test: `shouldSkipDescribeRow` + `buildTableFilterClause` (plan 03-01)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java`

**Imports pattern** (lines 1-8):
```java
package com.clickhouse.kafka.connect.sink.db.mapping;

import static com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers.col;
import static com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers.newDescriptor;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
```
Put the new `shouldSkipDescribeRow` unit test in package `com.clickhouse.kafka.connect.sink.db.helper` instead
(same package as `ClickHouseHelperClient`, so a package-private helper is directly callable) — a plain class,
no base class, no `@Tag`. Build fixtures with `ClickHouseFieldDescriptor.builder()` directly (already
`@Builder`-annotated, `ClickHouseFieldDescriptor.java:16-18`) using the exact shapes verified live in
RESEARCH.md Pattern 1 (`.null`, `.size0`, and the new `.size` case), plus a `Table` with one JSON/Dynamic
column pre-added via `table.addColumn(...)` for the parent-type-lookup cases.

**Core pattern** (`ColumnTest.java:12-16`, one assertion per fixture shape — this is the D-17 "genuine matrix"
candidate for `@ParameterizedTest` since the four skip suffixes are one input→boolean mapping):
```java
@Test
public void extractNullableColumn() {
    Column col = Column.extractColumn(newDescriptor("Nullable(String)"));
    assertEquals(Type.STRING, col.getType());
}
```

**`@DisplayName` / REQ-ID convention** (D-17, carried from Phase 2 — analog in
`src/test/java/transforms/TableOperationFilterTest.java:181-185`):
```java
@Test
@DisplayName("DESC-01: .null subcolumn of a Nullable column is skipped")
public void nullSubcolumnIsSkipped() { ... }

@ParameterizedTest(name = "DESC-03: {0} subcolumn is skipped")
@ValueSource(strings = {".null", ".size0", ".size"})
@DisplayName("DESC-03: structural + suffix skip rule catches every known subcolumn shape")
public void knownSubcolumnSuffixesAreSkipped(String suffix) { ... }
```

**Non-ASCII data rule** (D-18, if any fixture needs non-ASCII column/table names — analog
`TableOperationFilterTest.java:263-268`):
```java
String table = new String(new int[] {0x0442, 0x0430, 0x0431, 0x043B, 0x0438, 0x0446, 0x0430}, 0, 7);
```
Build from raw Unicode code points at runtime, never a `\u` escape literal in source (Phase 2 hit the editing
tool chain silently decoding those).

### New live test: FILT-01..04 + DESC-04 (plan 03-01)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientTest.java` (upstream — read-only reference, do not edit)

**Imports pattern** (lines 1-16):
```java
package com.clickhouse.kafka.connect.sink.db.helper;

import com.clickhouse.kafka.connect.sink.ClickHouseBase;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.junit.extension.FromVersionConditionExtension;
import com.clickhouse.kafka.connect.sink.junit.extension.SinceClickHouseVersion;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
```
The new class extends `ClickHouseBase` (inherits `@Tag("clickhouse")`, fresh per-class database per Phase 1
D-08) and uses the same `@BeforeEach setUp() { chc = createClient(createProps()); }` shape (lines 23-28).

**DESC-04 fixture to reuse verbatim (new class, not the existing one — D-16)**
(`ClickHouseHelperClientTest.java:65-79`):
```java
@Test
public void ignoreArrayWithNestedTable() {
    String topic = createTopicName("nested_table_test");
    ClickHouseTestHelpers.createTable(chc, topic,
            "CREATE TABLE %s ( `num` String, " +
                    "`nested` Array(Nested (innerInt Int32, innerString String))) " +
                    "Engine = MergeTree ORDER BY num");
    try {
        Table table = chc.describeTable(chc.getDatabase(), topic);
        Assertions.assertNull(table);
    } finally {
        ClickHouseTestHelpers.dropTable(chc, topic);
    }
}
```
`Column.extractColumn` returns `null` **only** for `Nested`/`Array(Nested(...))` (per RESEARCH.md and
`Column.java:215-218`), so this exact fixture shape is required for DESC-04 — no other column type reproduces
the null-return path.

**FILT-01..04 live shape** (analog: `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskMappingTest.java:243-278`, `tableFilterTest` — upstream, read-only reference):
```java
@Test
public void tableFilterTest() {
    Map<String, String> props = createProps();
    props.put(ClickHouseSinkConfig.SUPPRESS_TABLE_EXISTENCE_EXCEPTION, "true");
    props.put(ClickHouseSinkConfig.TARGET_TABLE_FILTER, "^filtered_.*");
    props.put(ClickHouseSinkConfig.TABLE_MAPPING, "topic1=filtered_table1, topic2=unfiltered_table2");
    ClickHouseHelperClient chc = createClient(props);
    ...
    ClickHouseSinkTask chst = new ClickHouseSinkTask();
    chst.start(props);
    chst.put(sr1);
    chst.put(sr2);
    chst.stop();

    assertEquals(sr1.size(), ClickHouseTestHelpers.countRows(chc, filteredTable));
    assertEquals(0, ClickHouseTestHelpers.countRows(chc, unfilteredTable));
}
```
Reuse this `props.put(ClickHouseSinkConfig.TARGET_TABLE_FILTER, ...)` + `SUPPRESS_TABLE_EXISTENCE_EXCEPTION`
shape for FILT-02(a)/(b) (both `suppress` values) and FILT-04 (contrast `tbl_\d+` vs `^tbl_\d+$` by exact
table identity, per D-19/Pitfall 3 — assert specific table names in the result set, not just a non-empty
match count). For FILT-01, call `chc.showTables(chc.getDatabase())` directly (as in
`ClickHouseHelperClientTest.showTables()`, lines 35-47) against a fresh per-class database with no filter set.

---

### New unit test: TYPE-01/02 type-string pin (plan 03-02)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java` (same package, do not edit — new sibling class)

**Core pattern to pin** (already-correct code, `Column.java:152-156` — no main-code change needed here, per
"Don't Hand-Roll" in RESEARCH.md):
```java
} else if (valueType.startsWith("JSON")) {
    type = Type.JSON;
} else if (valueType.startsWith("Dynamic")) {
    type = Type.DYNAMIC;
}
```
Test shape, directly mirroring `ColumnTest.extractNullableColumn` (`ColumnTest.java:12-16`):
```java
@ParameterizedTest(name = "TYPE-01: {0} maps to Type.JSON")
@ValueSource(strings = {"JSON", "JSON(max_dynamic_paths=10)"})
@DisplayName("TYPE-01: JSON and parameterized JSON(...) map to Type.JSON")
public void jsonTypeStringsMapToJson(String typeString) {
    Column col = Column.extractColumn(newDescriptor(typeString));
    assertEquals(Type.JSON, col.getType());
}
```

### New live test: JSON/Dynamic round-trip + non-ASCII (plan 03-02)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java` (table setup / SinkTask insert shape) + `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` (read-back helpers)

**Imports pattern** (`JsonColumnTest.java:1-15`):
```java
package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.*;

import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.junit.extension.FromVersionConditionExtension;
import com.clickhouse.kafka.connect.sink.junit.extension.SinceClickHouseVersion;
import java.util.*;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
```

**Table + insert shape to copy** (`JsonColumnTest.java:56-78`, extended per D-13 with the second
`bypassRowBinary` Struct route not present today):
```java
ClickHouseTestHelpers.createTable(chc, tableName,
    "CREATE TABLE %s (id UInt32, json_data JSON) ENGINE = MergeTree ORDER BY id");
...
ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
sinkTask.start(props);
sinkTask.put(transformedRecords);
sinkTask.stop();
assertEquals(RECORDS, ClickHouseTestHelpers.countRows(chc, tableName));
```
For the `bypassRowBinary` route, set `props.put("bypassRowBinary", "true")` and build `SinkRecord`s with a
`Schema`/`Struct` value (see `SchemaTestData.java:39-76` for the `SchemaBuilder.struct()` + `Struct(...).put(...)`
idiom) instead of the schemaless `Map` shape `JsonColumnTest` uses.

**D-14 typed read-back queries** (RESEARCH.md Pattern 2, live-verified this session — do not use `countRows`
alone):
```sql
SELECT id, json_col.a.b, toTypeName(json_col.a.b) FROM probe.t1 FORMAT JSONEachRow;
SELECT id, dynamicType(dynamic_col), dynamic_col FROM probe.t1 FORMAT JSONEachRow;
```
Run these via `chc.getClient().queryRecords(...)` / `ClickHouseTestHelpers.getAllRowsAsJson(chc, tableName)`
(`ClickHouseTestHelpers.java:152-174`, already UTF-8-safe per Phase 1 D-11) rather than inventing a new query
path.

**Non-ASCII values** (TYPE-07): reuse `ClickHouseTestHelpers.countRowsWithEmojis` (`ClickHouseTestHelpers.java:217-226`)
as the closest existing non-ASCII-assertion shape, and `TableOperationFilterTest.java:263-278`'s
code-point-construction idiom (D-18) for building Cyrillic/CJK values and identifiers at runtime.

---

### New tests: TYPE-06 unit gates + live fail-fast (plan 03-03)

**Analog (unit half):** `src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java`

**Direct protected-method-call pattern** (`ClickHouseWriterTest.java:46-98`, `writeUTF8StringPrimitive` — copy
the construction, not the Docker dependency):
```java
ClickHouseWriter writer = new ClickHouseWriter();
Column column = Column.extractColumn(newDescriptor("utf8String", "String"));
ClickHousePipedOutputStream out = new ClickHousePipedOutputStream(null) { /* byte-capturing overrides */ };
writer.doWritePrimitive(Type.STRING, Schema.Type.STRING, out, "value", column);
```
For TYPE-06, call `doWriteColValue`/`validateDataSchema` (both `protected`, callable from a test class in the
same `com.clickhouse.kafka.connect.sink.db` package with no subclassing needed) against a `Column`/`Table`
built directly with `Type.JSON`/`Type.DYNAMIC`/`Type.UNKNOWN`, and assert the thrown `DataException` message
contains `insertFormat=json` and `bypassRowBinary=true` (D-05, substring assertion per D-17).

**Analog (live half):** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskMappingTest.java` (put→countRows shape, upstream — read-only reference)

```java
ClickHouseSinkTask chst = new ClickHouseSinkTask();
chst.start(props);
chst.put(sr);
chst.stop();
assertEquals(sr.size(), ClickHouseTestHelpers.countRows(chc, tableName));
```
For TYPE-06's live half, invert the final assertion: build a `JSON`-column table, insert a single
Struct-schema record through the default (non-bypass) config, assert `put()` throws (wrap in
`assertThrows(RuntimeException.class, () -> chst.put(sr))` or let the sink task's own exception propagate per
`ClickHouseSinkTask.put()`'s existing catch/rethrow), then assert
`ClickHouseTestHelpers.countRows(chc, tableName) == 0` — the "no silent data loss" half D-06 requires the live
test (not the unit test) to prove.

## Shared Patterns

### `@DisplayName` REQ-ID convention (D-17, carried from Phase 2 D-10)
**Source:** `src/test/java/transforms/TableOperationFilterTest.java:181-185, 221-222, 325-328`
**Apply to:** every new test method across all three plans.
```java
@Test
@DisplayName("TOF-05: config() returns the shared CONFIG_DEF instance")
public void testConfigReturnsSharedConfigDefInstance() { ... }

@ParameterizedTest(name = "TOF-03: token ''{0}'' is ignored")
@ValueSource(strings = {"x", "r", "", "   ", "cu"})
@DisplayName("TOF-03: invalid, empty and whitespace tokens are ignored")
public void testInvalidSkippedOperationsTokenIsIgnored(String token) { ... }
```
Replace `TOF-xx`/`DESC-xx`/`FILT-xx`/`TYPE-xx` with this phase's requirement IDs.

### Docker-free vs ClickHouse-backed split by inheritance (D-16, Architecture Pattern 2)
**Source:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java:32-39` vs. any plain class (e.g. `ColumnTest.java` — no base class, no `@Tag`)
**Apply to:** every new test class — decide once per class whether it extends `ClickHouseBase` (live,
`@Tag("clickhouse")`, excluded from `testUnit`) or extends nothing (Docker-free, included in `testUnit`).
```java
@Tag("clickhouse")
public class ClickHouseBase {
    @BeforeAll
    public static void setup() throws IOException { ... }   // shared container, fresh per-class database
```

### Non-ASCII test data from Unicode code points (D-18)
**Source:** `src/test/java/transforms/TableOperationFilterTest.java:263-268`
**Apply to:** every TYPE-07 fixture and any DESC/FILT fixture using non-ASCII identifiers.
```java
String db = new String(new int[] {0x0431, 0x0430, 0x0437, 0x0430}, 0, 4);
```
Never write a literal `\u` escape or a raw non-ASCII byte in source — the editing tool chain silently decodes
`\u` escape text into real non-ASCII bytes (Phase 2 finding).

### Field/column fixture builders
**Source:** `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java:310-333`
**Apply to:** every Docker-free unit test needing a `ClickHouseFieldDescriptor` or `Column`.
```java
public static ClickHouseFieldDescriptor newDescriptor(String name, String valueType) {
    return ClickHouseFieldDescriptor.builder().name(name).type(valueType).isSubcolumn(name.contains(".")).build();
}
public static Column col(Type type) {
    return Column.builder().type(type).build();
}
```
This is an existing **shared** helper (not new) — call it via its static import
(`import static com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers.newDescriptor;`) rather than
duplicating the builder calls inline.

### `DataException` throw idiom (D-02/D-03/D-04, shared between the two `ClickHouseWriter` fixes)
**Source:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:393-397`
**Apply to:** both the `doWriteColValue` default branch and the new `validateDataSchema` JSON/DYNAMIC/UNKNOWN case.
```java
String msg = String.format("...", ...);
LOGGER.error(msg);
throw new DataException(msg);
```

## No Analog Found

None — every file in this phase's scope (2 main-code files, 5 new test-class groupings) has a strong,
git-tracked, same-package or same-file analog. The closest thing to a gap is D-14's typed-SQL-read-back
assertion style (`toTypeName`/`dynamicType`), which has no existing test-helper wrapper — RESEARCH.md Pattern 2
gives the exact verified SQL to run inline via `chc.getClient().queryRecords(...)`; no new shared helper is
required unless the executor finds the raw query repeated 3+ times.

## Metadata

**Analog search scope:** `src/main/java/com/clickhouse/kafka/connect/sink/db/**`,
`src/main/java/com/clickhouse/kafka/connect/sink/db/helper/**`,
`src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/**`,
`src/test/java/com/clickhouse/kafka/connect/sink/**`, `src/test/java/transforms/**`
**Files scanned:** `ClickHouseHelperClient.java`, `ClickHouseFieldDescriptor.java`, `ClickHouseWriter.java`,
`Table.java`, `ClickHouseBase.java`, `ClickHouseTestHelpers.java` (unit-test package),
`ClickHouseHelperClientTest.java`, `ColumnTest.java`, `ClickHouseWriterTest.java`,
`ClickHouseSinkTaskMappingTest.java`, `JsonColumnTest.java`, `TableOperationFilterTest.java`,
`TransformTestHelpers.java`, `SchemaTestData.java`
**Pattern extraction date:** 2026-09-17
