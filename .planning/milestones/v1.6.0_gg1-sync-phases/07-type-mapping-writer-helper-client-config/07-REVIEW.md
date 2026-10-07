---
phase: 07-type-mapping-writer-helper-client-config
reviewed: 2026-09-19T00:27:23Z
depth: standard
files_reviewed: 24
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
  - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Type.java
  - build.gradle.kts
  - src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseServerVersionSmokeTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskMappingTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/InsertFormatPerformanceTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonDynamicRoundTripTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/MBeanRegistrationLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/NewUpstreamConfigDefaultsUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryFailFastLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/SmtSinkLandingTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
findings:
  critical: 2
  warning: 3
  info: 3
  total: 8
status: issues_found
---

# Phase 07: Code Review Report

**Reviewed:** 2026-09-19T00:27:23Z
**Depth:** standard
**Files Reviewed:** 24
**Status:** issues_found

## Summary

This phase re-applies five fork behaviours (`targetTableFilter` table discovery, JSON/Dynamic
subcolumn skipping, DYNAMIC/UNKNOWN RowBinary fail-fast, several new upstream config options, and
a payload-log level fix) onto upstream's rewritten `ClickHouseWriter`/`ClickHouseHelperClient`/
`ClickHouseSinkConfig`. The phase-7-specific diff (`buildTableFilterClause`, `shouldSkipDescribeRow`,
the DYNAMIC/UNKNOWN gate, the DEBUG→TRACE payload-log fix) is well tested and its intentional
trade-offs (D-01 `insertFormat` typo, D-15 async_insert removal, D-19 no-backslash-escaping) are
explicitly pinned by unit/live tests with decision references, so those are **not** re-flagged here.

Two genuine defects were found, one newly introduced by this phase (SQL injection surface in the
new `buildTableFilterClause`/`showTables*` code path) and one pre-existing but squarely inside the
highest-risk file in scope (`ClickHouseWriter`'s RowBinary writer NPEs on legal nested-null
composite values). Three further issues degrade robustness/maintainability without being
immediately exploitable. Test files reviewed are solid; only minor observations are noted.

## Critical Issues

### CR-01: `targetTableFilter` is embedded into the `system.columns` discovery query with escaping that can be broken by a trailing backslash (SQL injection)

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:344-348` (used by `showTablesV1` at `:365-366` and `showTablesV2` at `:383-384`)

**Issue:** `buildTableFilterClause` (new in this phase) escapes the operator-supplied
`targetTableFilter` regex by doubling single quotes only, and — per its own doc comment and the
`FILT-04`/D-19 test — **deliberately does not escape backslashes**, to keep regex character
classes like `\d+` intact for `match()`:

```java
static String buildTableFilterClause(String targetTableFilter) {
    return targetTableFilter.isEmpty()
            ? ""
            : String.format(" AND match(table, '%s')", targetTableFilter.replace("'", "''"));
}
```

ClickHouse's SQL string literals use C-style backslash escaping, so a value containing a
backslash immediately followed by a single quote breaks out of the string literal *before* the
`replace("'", "''")` doubling can help, because the backslash consumes exactly one of the two
characters produced by the doubling. Concretely, for `targetTableFilter = "x\' OR 1=1-- "`:

1. `.replace("'", "''")` turns the one literal quote into two: `x\'' OR 1=1-- `.
2. Embedded: `... AND match(table, 'x\'' OR 1=1-- ')`.
3. ClickHouse parses `\'` as an escaped (literal) quote, so the string content is `x'`, and the
   *second* quote (the un-escaped one from the doubling) closes the literal early.
4. Everything after that — ` OR 1=1-- ')` — is now raw SQL, and `--` opens a line comment that
   swallows the rest of the query line (including the connector's own trailing `)`).

The result is a genuine SQL injection point into the internal `system.columns` discovery query,
reachable by anyone able to set connector config (`targetTableFilter`) — a lower-trust boundary
than an end record, but still real, newly introduced, and untested (the existing `FILT-03`
live/unit tests only cover a lone `'` with no backslash, and `FILT-04` only covers a bare `\d+`
with no adjacent quote — the backslash+quote combination is not exercised anywhere).

**Fix:** Escape backslashes as well, in the correct order (backslash first, then quote), or —
better — do not build the filter into an interpolated SQL string at all. ClickHouse's HTTP
interface supports query parameters (`{targetTableFilter:String}` + a bound param), which sidesteps
string-escaping entirely and is the safer fix here:

```java
static String buildTableFilterClause(String targetTableFilter) {
    return targetTableFilter.isEmpty()
            ? ""
            : String.format(" AND match(table, '%s')",
                    targetTableFilter.replace("\\", "\\\\").replace("'", "\\'"));
}
```
(or migrate `showTables*`/`extractTablesMapping` to bound query parameters).

### CR-02: `doWriteColValue` NPEs instead of writing NULL for a null MAP/TUPLE/VARIANT value nested inside an Array or Map column

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:576-577` (MAP), `:616-617` (TUPLE), `:639-640` (VARIANT); triggered via the recursive calls at `:585` (Map values), `:606` (Array elements)

**Issue:** When a nullable **primitive** child of an `Array`/`Map` is `null` (e.g.
`Array(Nullable(String))`, `Map(String, Nullable(Int32))`), the ARRAY and MAP cases correctly
special-case it: the ARRAY branch explicit-null-checks `arrObject`/its elements, and
`doWritePrimitive`/`doWriteFixedString`/`doWriteDates`/the `Decimal` case all null-check
`value.getObject()` up front and call `BinaryStreamUtils.writeNull(stream)`.

But when the nullable child's type is itself a **composite** type — `MAP`, `TUPLE`, or `VARIANT`
— the recursive call goes straight to `doWriteColValue(childCol, stream, new Data(schema, v), ...)`
(bypassing `doWriteCol`'s own null guard, which is only invoked for *root* record columns), and
none of these three `case` blocks null-checks before dereferencing:

```java
case MAP:
    Map<?, ?> mapTmp = (Map<?, ?>) value.getObject();
    int mapSize = mapTmp.size();                       // NPE if value.getObject() == null
    ...
case TUPLE:
    Object underlyingObject = value.getObject();
    if (underlyingObject.getClass() != Struct.class) {  // NPE if value.getObject() == null
    ...
case VARIANT:
    mapTmp = (Map<?, ?>) value.getObject();
    Optional<Data> variantValueOption = mapTmp.values().stream()...  // NPE if value.getObject() == null
```

So a legal ClickHouse schema such as `Array(Nullable(Tuple(a String, b String)))`,
`Map(String, Nullable(Map(String, String)))`, or a `Variant` nested one level inside an `Array`/
`Map` with an actual `null` entry crashes the RowBinary writer with an unhandled
`NullPointerException` mid-stream (after some bytes have already been pushed into the piped
HTTP body), instead of failing with the actionable `DataException` this same phase just added for
`DYNAMIC`/`UNKNOWN` columns, or simply encoding the null marker like every other type does.

**Fix:** Null-check `value.getObject()` at the top of the `MAP`, `TUPLE`, and `VARIANT` cases and
write the appropriate null encoding (mirroring the `ARRAY` case's `arrObject == null` handling)
before doing any casting/dereferencing:

```java
case MAP:
    if (value.getObject() == null) {
        BinaryStreamUtils.writeVarInt(stream, 0); // or the format's actual empty/null map encoding
        break;
    }
    Map<?, ?> mapTmp = (Map<?, ?>) value.getObject();
    ...
```
(confirm the correct on-the-wire representation for a null nested Map/Tuple/Variant against the
RowBinary spec before picking the exact bytes — the key fix is guarding against the NPE, not any
specific byte sequence.)

## Warnings

### WR-01: `Column.extractColumn`'s `Array` recursion swaps the `hasDefaultValue`/`isSubColumn` arguments

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java:213`

**Issue:** The method signature is
`extractColumn(String name, String valueType, boolean isNull, boolean hasDefaultValue, boolean isSubColumn, int arrayDepth)`,
but the `Array` branch calls it as:

```java
Column arrayType = extractColumn(name, valueType.substring(...), false, isSubColumn, hasDefaultValue, arrayDepth + 1);
```

Positionally, `hasDefaultValue` and `isSubColumn` are swapped for the recursive (element) call:
the array element's `hasDefault` is populated from the *parent's* `isSubColumn`, and the element's
`isSubColumn` is populated from the *parent's* `hasDefaultValue`. This currently produces no
observable bug because nothing reads `hasDefault()`/`isSubColumn()` off a nested `arrayType`
`Column` (only top-level columns are consulted for those flags in `ClickHouseWriter`/`Table`), but
it is a landmine: the moment any future code path inspects `col.getArrayType().hasDefault()` or
`.isSubColumn()` (e.g. supporting `DEFAULT` on array elements, or the same JSON/Dynamic subcolumn
routing this phase just added elsewhere), it will silently read the wrong flag.

**Fix:** Correct the argument order to match the parameter order:
```java
Column arrayType = extractColumn(name, valueType.substring(...), false, hasDefaultValue, isSubColumn, arrayDepth + 1);
```

### WR-02: `ConfigDef` default for `USERNAME` is `passwordDefault` ("") instead of `usernameDefault` ("default")

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:433-441`

**Issue:**
```java
configDef.define(USERNAME,
        ConfigDef.Type.STRING,
        passwordDefault,      // should be usernameDefault
        ConfigDef.Importance.LOW,
        "username",
        ...
```
The actual runtime default is correct (`ClickHouseSinkConfig`'s constructor reads
`props.getOrDefault(USERNAME, usernameDefault)` at line 218, and `ClickHouseSinkConnector.taskConfigs()`
passes raw `props` through without going via `ConfigDef.parse()`), so this does not change
connector behavior. It does, however, make the Kafka Connect framework's own config metadata
wrong: `connector-plugins/{name}/config/validate`, Connect UI tooling, and `ConfigDef.toHtml()`
docs will all report `username` as defaulting to an empty string rather than `"default"`.

**Fix:**
```java
configDef.define(USERNAME,
        ConfigDef.Type.STRING,
        usernameDefault,
        ...
```

### WR-03: `validateDataSchema`'s default branch can NPE on `objSchema.name()` when a `jsonMap` entry has no backing `Field`

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:281-327`

**Issue:**
```java
Map<String, Schema> schemaMap = record.getFields().stream().collect(Collectors.toMap(Field::name, Field::schema));
var objSchema = schemaMap.get(colName);           // null if record.getFields() has no matching Field
Data obj = record.getJsonMap().get(colName);
if (obj == null) { ... }
else if (!onlyFieldsName) {
    ...
    default:
        if (!colTypeName.equals(dataTypeName)) {
            LOGGER.debug("Data schema name: {}", objSchema.name());   // NPE if objSchema == null
```
This path is reachable whenever `record.getJsonMap()` has an entry for a column but
`record.getFields()` does not carry a `Field` of the same name — exactly the situation this
phase's own `RowBinaryUnsupportedTypeGateTest` had to work around by manually constructing a
matching `fields` list, as its in-line comment documents:

> "an empty fields list here ... leaves that lookup null and NPEs on `Schema#name()`"

The only known production trigger (Debezium's injected `_version`/`is_deleted` columns) is
currently protected because `doInsertRawBinaryV1`/`V2` skip `validateDataSchema` entirely for
`SchemaType.DEBEZIUM_CDC`. But this is a narrow, easily-forgotten guard rather than a fix in
`validateDataSchema` itself — any other future path that populates `jsonMap` with a synthetic
column not backed by a `Field` (schema evolution, additional SMT-injected columns, etc.) will hit
this same NPE with no actionable message, unlike the DYNAMIC/UNKNOWN gate this phase added right
next to it.

**Fix:** Null-check `objSchema` before dereferencing, and fail with the same kind of actionable
message the DYNAMIC/UNKNOWN branch now uses, e.g.:
```java
if (objSchema == null) {
    validSchema = false;
    LOGGER.error("Column [{}] has a value but no matching Kafka Connect field schema", colName);
} else if (!colTypeName.equals(dataTypeName)) {
    ...
}
```

## Info

### IN-01: Commented-out `async_insert` setting left in source instead of removed

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:246`

**Issue:** `//this.addClickHouseSetting("async_insert", "0", false);` is left commented out rather
than deleted. The removal itself is an intentional, tested behavior change (`AsyncInsertLiveTest`,
D-15), but AGENTS.md's code-style guidance disallows commented-out code, and a bare commented line
gives no reader-facing explanation of *why* it's gone (that context currently lives only in the
test file/decision log).

**Fix:** Delete the line, or replace it with a real comment explaining the D-15 decision inline
(e.g. `// D-15: no longer force async_insert=0; see AsyncInsertLiveTest`).

### IN-02: `versionV2()` comment does not match the code

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:271`

**Issue:** `return record.getString(1); // string column col3` — the query is `SELECT VERSION()`
(a single column), and `getString(1)` correctly reads the first (only) column, but the trailing
comment ("col3") doesn't correspond to anything in this query and is likely left over from a
different query shape. Harmless but confusing for future maintainers touching this method.

**Fix:** Remove or correct the comment.

### IN-03: `JsonColumnTest` only asserts row counts, not the JSON payload content

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java:81` (all six `@Test` methods via `runJsonTest`)

**Issue:** AGENTS.md's testing guidance states "When writing records to ClickHouse, always read
back the data and `assertEquals` against the original records." `runJsonTest` only checks
`assertEquals(RECORDS, ClickHouseTestHelpers.countRows(chc, tableName))` — it never reads back
`json_data` and compares `nested_str`/`nested_int`/`nested_bytes` against the source records, so a
regression that silently corrupts JSON-column content (byte-array encoding, base64 handling, etc.
— precisely what this test class is meant to cover across six format/library combinations) would
not be caught. Pre-existing (not introduced by this phase, which only touched two import lines
here), but within the reviewed file set.

**Fix:** Read back `json_data` per row and assert on the nested fields (at minimum
`nested_str`/`nested_int`; `nested_bytes` per whatever base64/JSON encoding each config variant
produces), consistent with the other new phase-7 live tests (`SmtSinkLandingTest`,
`RowBinaryFailFastLiveTest`) which already follow this pattern correctly.

---

_Reviewed: 2026-09-19T00:27:23Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
