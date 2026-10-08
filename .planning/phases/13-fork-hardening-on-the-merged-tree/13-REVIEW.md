---
phase: 13-fork-hardening-on-the-merged-tree
reviewed: 2026-10-08T00:00:00Z
depth: standard
files_reviewed: 28
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
  - src/main/java/com/clickhouse/kafka/connect/sink/data/Data.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
  - src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBaseTearDownAttributionUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfigUsernameDefaultUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseVersionDefaultUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/MetadataFreeJsonTypeParityLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/NestedNullRowBinaryLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/WideIntegerTypeGateLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnArrayRecursionTest.java
  - src/test/java/transforms/HeaderToValueTest.java
  - src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RowSnapshots.java
findings:
  critical: 0
  warning: 5
  info: 4
  total: 9
status: issues_found
---

# Phase 13: Code Review Report

**Reviewed:** 2026-10-08
**Depth:** standard
**Files Reviewed:** 28
**Status:** issues_found

## Summary

Reviewed the Phase 13 diff (`0faac678..HEAD`) for the six production files and the 22 test and fixture files, reading the full files for context. I cross-checked `MetadataFreeJsonWriter` against `DebeziumRecordConvertor`, `StructToJsonMap` and `DataJson`. I also checked the nested-null changes in `doWriteColValue` against `doWriteCol` and `Column.extractColumn`.

No structural findings (fallow) were provided for this review.

The production changes are mostly sound:

- The `Column.extractColumn` argument swap is fixed correctly, and all other recursive calls pass `(isNull, hasDefault, isSubColumn)` in the right order.
- The `usernameDefault` fix is correct.
- `shouldSkipDescribeRow` terminates and walks ancestors correctly.
- `MetadataFreeJsonWriter` holds no mutable state, uses a private copy of the mapper, and gives deterministic output for retried batches. No exactly-once invariant is broken.
- No secrets or record values are logged. The new nested-null message is built from `Column` metadata only.

I found no BLOCKER-level defects. The warnings are mostly places where this phase fixed one route or one case and left a sibling inconsistent. Several of them are acknowledged in the behaviour ledger, but they are still real hazards for production CDC pipelines.

## Warnings

### WR-01: Empty-CDC-record skip guard exists only on the metadata-free route; the `bypassRowBinary=true` JSON route still emits all-defaults rows

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1365-1387`
**Issue:** `DebeziumRecordConvertor` returns an empty `jsonMap` for `op=t` and for a null `before`/`after` struct. The new code skips such records when `metadataFree` is true, with the comment that they "would otherwise land as an all-defaults row". The sibling branch (`bypassRowBinary=true`, `insertFormat` unset) takes the `else` path. There, `DEBEZIUM_CDC` builds an empty `data` map, `cleanupExtraFields` returns `{}`, and the writer emits `{}`. That inserts an all-defaults row (id 0, `_version` 0, `is_deleted` 0) into a ReplacingMergeTree. `doInsertBatch` routes both flags to the same `doInsertJson`, so the two JSON routes now disagree on identical input. A Debezium truncate event or a delete with a null `before` can therefore still inject a phantom row through the bypass route.
**Fix:** Hoist the empty-map guard out of the `metadataFree` branch so it applies to every `DEBEZIUM_CDC` record on the JSON path:
```java
if (record.getSchemaType() == SchemaType.DEBEZIUM_CDC
        && (record.getJsonMap() == null || record.getJsonMap().isEmpty())) {
    LOGGER.debug("Skipping empty CDC record ...");
    continue;
}
if (metadataFree && record.getSchemaType() == SchemaType.SCHEMA) { ... }
else if (metadataFree && record.getSchemaType() == SchemaType.DEBEZIUM_CDC) { ... }
else { ... }
```
If the asymmetry is intentional, pin it with a test and record it in the ledger.

### WR-02: `io.debezium.time.Timestamp` millisecond longs pass through unconverted and are silently misread as seconds

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriter.java:186-226` (`writeLogicalValue`)
**Issue:** The writer converts Connect `Timestamp`, `io.debezium.time.Date` and `io.debezium.time.MicroTimestamp`, but not `io.debezium.time.Timestamp`. That is the default MySQL `datetime(0..3)` mapping under Debezium's adaptive time precision. A raw epoch-millis long such as `1704067200123` goes into `DateTime64(3)` as an integer. ClickHouse reads a JSON integer as seconds, so the stored value is wrong, far in the future or out of range. The RowBinary route writes the same long as 3-tick units and stores the correct value, so the "RowBinary parity" claim of the phase does not hold for this type. The write succeeds, so the corruption is silent. The ledger (L-05) calls it a known gap, but it affects a very common CDC source, and nothing warns the operator at runtime.
**Fix:** Add `io.debezium.time.Timestamp` (and ideally `NanoTimestamp`) as a case in `writeLogicalValue`, using the same decimal-seconds encoding:
```java
private static final String DEBEZIUM_TIMESTAMP = "io.debezium.time.Timestamp";
...
case DEBEZIUM_TIMESTAMP:
    if (value instanceof Number millis) {
        gen.writeNumber(BigDecimal.valueOf(millis.longValue(), MILLIS_SCALE));
        return true;
    }
    return false;
```
The parity tests in `MetadataFreeJsonWriterTest` and `MetadataFreeJsonTypeParityLiveTest` currently pin the pass-through, so they must change with the code. At minimum, log a one-time WARN when this logical name is seen on the metadata-free route.

### WR-03: The "not nullable and no value" failure throws a message-less `RuntimeException`, and this phase makes it reachable for `Array(Tuple)`

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1094`
**Issue:** `doWriteCol` ends with `LOGGER.error("Column {} is not nullable and no value is provided", name); throw new RuntimeException();`. Ledger L-17 says the COLS-07 fix means a missing `Array(Tuple)` field now "fails loudly in doWriteColValue's 'not nullable and no value' path". The exception carries no message, so Connect status, the DLQ headers and `Utils.handleException`'s wrapper show nothing about which column failed. The column name appears only in the connector log. This conflicts with AGENTS.md ("Error messages must be clear, actionable") and with the actionable `DataException`s this phase added for the sibling nested-null cases. Because this phase made the path reachable for more tables, it should be fixed alongside.
**Fix:**
```java
String msg = String.format("column '%s' (%s) is not nullable, has no default, and no value was provided", name, colType);
LOGGER.error(msg);
throw new DataException(msg);
```
Keep the top-level null behaviour (D-11) unchanged. This is a different branch, where the field is absent rather than null.

### WR-04: Null-deref on `e.getMessage()` in the `doInsertRawBinary` catch block can mask the real failure (pre-existing, in a high-risk file)

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1177-1178`
**Issue:** The stream filter `e.getMessage().contains(code)` runs before the `e.getMessage() != null` guard on the next line. Any exception with a null message that reaches this block throws an NPE from inside the catch handler and replaces the original error. This phase added more serialization-time exception types: the nested-null `DataException`s, the Tuple-field check, and the message-less exception in WR-03. They are normally wrapped by the client or `ExecutionException`, which gives a non-null message, but nothing guarantees that for every wrapping or for the V1 client. The line is outside the phase diff, but the phase adds exceptions that flow through it.
**Fix:**
```java
String message = e.getMessage();
Optional<String> updateTableException = message == null
        ? Optional.empty()
        : UPDATE_TABLE_EXCEPTION_STR_TO_ERROR_CODE.keySet().stream().filter(message::contains).findFirst();
if (updateTableException.isPresent() && retry) { ... }
```

### WR-05: Nested nulls in `Array(Nullable(Enum))`, `Map(K, Nullable(Enum))` and `Array(Nullable(Tuple))` now fail the whole batch, including arrays that are entirely null (regression for previously working data)

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:711-724`, `:732-742` and `:683-693`
**Issue:** `Column.extractColumn` drops nullability for Enum and Tuple (D-16, L-11, L-13). The new guards (`v == null && !arrayType.isNullable()`, the map-value equivalent, and the TUPLE `!col.isNullable()` check) therefore treat a legal `Nullable(Enum8)` or `Nullable(Tuple)` slot as non-nullable and throw `DataException`. Before this phase, mixed arrays were already corrupt, but an all-null `Array(Nullable(Enum8))` or a single-field `Array(Nullable(Tuple(a Int32)))` round-tripped NULL. Now they stop the task under `errors.tolerance=none`, or DLQ the whole batch under `all`. The ledger documents this as an accepted rollout cost, so I rate it WARNING and not BLOCKER. The root cause (nullability dropped in the `Column` extractor) is known and logged to backlog 999.1, and until that is fixed this phase turns a silent mis-encoding into a hard failure for legal schemas. Consider fixing the root cause in this phase rather than shipping the guards on top of it. Whether the Enum and Tuple branches should carry `isNull` is a small change, but it needs live tests on Enum and Tuple, which is why it was deferred.
**Fix:** Propagate `isNull` in the Enum branch of `extractColumn` (`builder.type(type).isNullable(isNull)`). For Tuple, set the flag there and add the matching `NonNull` marker handling in the ARRAY/MAP/TUPLE writers. If the deferral stands, add an explicit WARN-level startup log naming the affected columns when DESCRIBE yields `Nullable(Enum*)` or `Nullable(Tuple)`, so the operator learns of the limitation before the first null arrives.

## Info

### IN-01: Nested-null check logic is duplicated three times (ARRAY, MAP, TUPLE)

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:683-693`, `:711-724`, `:757-767`
**Issue:** The condition "slot null, not nullable, not VARIANT, then coerce container to empty or throw `DataException`" is spelled out separately for the Map value and the Array element. The Tuple field uses a third, slightly different spelling. AGENTS.md says DRY, and the three copies can drift (the Tuple site does not check `getType() == VARIANT` the same way).
**Fix:** Extract one helper, for example `boolean writeNestedNullIfAny(Column container, Column slot, String slotKind, OutputStream stream)`, that returns true when it consumed the null by writing an empty container and throws otherwise. The three call sites then become one line each.

### IN-02: Expected data-quality failures are logged at ERROR with a full stack trace, once per recursion level

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:853-856` (the final `catch (Exception e)` in `doWriteColValue`)
**Issue:** The nested-null `DataException` is thrown from inside a recursive `doWriteColValue`. Each enclosing level of `Array(Array(...))` or `Map(K, Array(...))` catches it, logs `LOGGER.error("Error writing value of ...", e)` with the stack, and rethrows. A single bad record therefore produces several identical ERROR entries with stacks. The log line is also emitted for each retry of the same batch under `errors.tolerance=none`.
**Fix:** Rethrow `DataException` before the generic catch (`catch (DataException e) { throw e; }`), or log only at the outermost level.

### IN-03: `NESTED_NULL_*` constants and helpers are declared mid-class, between unrelated methods

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:554-559`
**Issue:** The new static constants sit between `buildRowBinaryUnsupportedTypeMessage` and the rest of the helpers, away from the file's other constants. AGENTS.md asks for static string constants at the top level of the class. This is a minor readability point.
**Fix:** Move the constants next to the existing constants at the top of the class. The helper methods can stay where they are.

### IN-04: `writeRecordsJson` now has three nested serialization strategies in one loop body

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1355-1400`
**Issue:** The loop body mixes the metadata-free SCHEMA path, the metadata-free CDC path (with a `continue`), and the legacy `switch`. The `beforeSerialize` timer is assigned in four places. The `continue` in the middle of an if/else chain makes the control flow harder to verify, which matters in a file AGENTS.md lists as high risk.
**Fix:** Extract a `serializeJsonRow(Record, Table, boolean metadataFree)` method that returns `byte[]`, with null meaning "skip". The loop then reduces to one call plus `writeJsonRow`, and the timing is captured once around that call.

---

_Reviewed: 2026-10-08_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
