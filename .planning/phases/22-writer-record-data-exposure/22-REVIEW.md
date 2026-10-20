---
phase: 22-writer-record-data-exposure
reviewed: 2026-10-20T00:00:00Z
depth: standard
files_reviewed: 16
files_reviewed_list:
  - docs/FORK-DELTA.md
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
  - src/main/java/com/clickhouse/kafka/connect/util/WithheldMessageException.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterNullMessageUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/ScriptedWriteFailureWriter.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterLogMaskingUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/dlq/InMemoryDLQ.java
  - src/test/java/com/clickhouse/kafka/connect/sink/util/ThrowableTexts.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ValueFreeDlqHeadersUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/WriterErrorPathLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/WriterFailureClassificationUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/util/WithheldMessageExceptionUnitTest.java
findings:
  critical: 0
  warning: 1
  info: 5
  total: 6
status: issues_found
---

# Phase 22: Code Review Report

**Reviewed:** 2026-10-20
**Depth:** standard
**Files Reviewed:** 16
**Status:** issues_found

## Summary

The change replaces the raw rethrow in `ClickHouseWriter.doWriteColValue`'s generic catch with a value-free `DataException` whose cause is a sanitized `WithheldMessageException` copy of the chain. I traced it against `Utils.handleException`, `doInsertRawBinary` (V1 text match and V2 `ServerException` branch), `Processing.doInsert` wrapping and the Kafka Connect DLQ header algorithm.

What holds up:
- Retry classification is preserved. Every retry-relevant shape (an `IOException` anywhere in the chain, `SocketTimeoutException`, `UnknownHostException`, `SocketException`, the "Read/Write timed out after" prefixes) is covered by the `IOException` pass-through. The only other `DataException` branch in `Utils` (line 106) is never reached for writer failures, because `Processing.doInsert` wraps them in a `RuntimeException` first. `WriterFailureClassificationUnitTest` pins this over a wide matrix.
- Nothing in `src/main` branches on a `DataException` other than `Utils`, so the changed exception type does not alter DLQ or state-machine routing.
- `WithheldMessageException` is correct. Suppression is disabled, the class name and frames are kept, no original text is read, and cycles and long chains terminate. `printStackTrace` renders it through `toString()`.
- The absence checks in the tests are made non-vacuous by self-checks against the raw JDK messages.
- No existing test was weakened. The `WriterLogMaskingUnitTest` flip is the one that pinned the residual to be flipped, and the `ClickHouseSinkTaskWithSchemaTest` change asserts the same strictness through `getOriginalClassName()`.

No blocker was found. One warning concerns a residual leak through the blanket `IOException` pass-through, which `FORK-DELTA.md` records as "not measured". The remaining items are diagnosability and test-hygiene points.

## Warnings

### WR-01: The blanket `IOException` pass-through lets Jackson's `JsonMappingException` carry record text unsanitized

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:640-651` (reachable through the JSON case at line 873)
**Issue:** `passesThroughUnchanged` returns true for any chain containing an `IOException`. `com.fasterxml.jackson.core.JsonProcessingException` (and so `JsonMappingException`) is an `IOException`. The `JSON` case calls `OBJECT_MAPPER.writeValueAsBytes(value)` when `binaryFormatWrtiteJsonAsString` is on. Jackson wraps any non-`IOException` failure of a nested serializer in a `JsonMappingException` whose message is the original exception's message plus `(through reference chain: ...)`. That path segment is a Map key or a list index. A bean-less value hitting `defaultSerializeValue` also produces an `InvalidDefinitionException` with the path.

On that route the pass-through therefore re-opens the leak this phase closes:
- The text reaches the Connect task status, the DLQ stack-trace header and the `Error inserting records` logs.
- A record text containing `ClickHouseException: Code: 33` or `Code: 131` could still trigger the V1 mapping refresh, which is the D-04 fix.

`FORK-DELTA.md` item 6 admits this was "not measured". None of the retry shapes depends on Jackson exceptions, so excluding them costs no classification.

**Fix:** Do not let a Jackson exception count as the retry-relevant `IOException`:
```java
for (Throwable t = e; t != null && seen.add(t); t = t.getCause()) {
    if (t instanceof IOException && !(t instanceof com.fasterxml.jackson.core.JsonProcessingException)) {
        return true;
    }
}
```
Add a test that serializes a Struct whose Map value cannot be serialized, using a sentinel map key. It should assert that the thrown chain and the logs carry no sentinel. Alternatively, measure and record the result in `FORK-DELTA.md`.

## Info

### IN-01: An unknown Enum label is reported as `java.lang.NullPointerException`

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1040-1045` (call sites) and `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java:524-530`
**Issue:** `convertEnumValues` returns null for a label that is not in `enumValues`, and the caller dereferences it with `.byteValue()` or `.intValue()`. The new message therefore reads `... could not be written as ClickHouse type Enum8: java.lang.NullPointerException`. `FORK-DELTA.md` lists "an unknown Enum label" among the cases that now get an actionable message, but the exception class an operator sees names a connector bug, not the cause. The phase goal is that the operator learns the real failure class.
**Fix:** Throw a typed, value-free exception when the lookup misses. Column metadata is safe to include:
```java
Integer code = enumValues.get(value);
if (code == null) {
    throw new IllegalArgumentException("label not found in enum of column " + name);
}
return code;
```
The generic catch then reports `IllegalArgumentException`. Update `UNKNOWN_ENUM_LABEL` expectations accordingly. The classification outcome stays NON_RETRIABLE.

### IN-02: The fixed `No content in enum` text drops the column name, and the exact-`RuntimeException` pass-through delivers it as it is

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java:529`
**Issue:** The label is rightly removed (D-14), but the column name, which is safe metadata, is not added. This exception is of exact class `RuntimeException`, so it bypasses the new `DataException` and reaches Connect status as bare `No content in enum`. The operator then learns neither the column nor the type from the status, only from the preceding ERROR log. Only a hand-built `Column` reaches it, so the impact is small.
**Fix:** `throw new RuntimeException("No content in enum column " + name);`. Adjust `NO_ENUM_CONTENT_MESSAGE` in `WriterValueFreeFailureUnitTest`.

### IN-03: The metadata-derivation code is duplicated between the `ClassCastException` branch and the new helper

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:660-663` and `894-899`
**Issue:** `sourceType` and `valueClass` are computed identically in two places. The new `WRITE_FAILURE_COLUMN_PREFIX` constant is used only by the new format. The `ClassCastException` message still hard-codes `"Failed to write column `%s`..."`. `FORK-DELTA.md` promises a shared grep prefix, and nothing keeps the two in step. `org.apache.kafka.connect.errors.ConnectException` is spelled fully qualified at line 641 while `DataException` is imported.
**Fix:** Extract `private static String valueClassName(Data)` and `sourceTypeName(Data)`, build both messages from `WRITE_FAILURE_COLUMN_PREFIX`, and import `ConnectException`.

### IN-04: `schemaWithFixedStringMismatchTest` passes vacuously when `put` does not throw

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaTest.java:908-915`
**Issue:** The assertions sit in a `catch (RuntimeException e)` block with no `fail` after `put`. If the mismatch is ever accepted, the test still goes green. The hunk touched this block, so it is in scope. The rewritten assertion is as strict as before, but the pre-existing structural gap remains.
**Fix:** Use `RuntimeException e = assertThrows(RuntimeException.class, () -> chst.put(sr));` and keep the two assertions on `Utils.getRootCause(e)`.

### IN-05: Dead counter and planning-ID noise

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java:21,34,48`; `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:569,636-640`; `src/main/java/com/clickhouse/kafka/connect/util/WithheldMessageException.java:25`
**Issue:**
- `useClientV2Calls` is incremented and exposed but never asserted by any test, so it is dead.
- The production comments and Javadoc cite `.planning`-only identifiers (`D-01`, `D-03`, `D-07`, `D-08`, `WRT-13`, `D-05`, `D-06`). These do not resolve for a future reader of a fork that is rebased on upstream.
- The comment at line 569 is a single very long line.

AGENTS.md asks for comments that carry invariants, not references.
**Fix:** Remove `useClientV2Calls` (or assert it in the pin). Keep the invariant sentences and drop the ID tags from `src/main`.

---

_Reviewed: 2026-10-20_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
