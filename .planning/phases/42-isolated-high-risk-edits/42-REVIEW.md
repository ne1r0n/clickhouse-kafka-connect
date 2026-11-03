---
phase: 42-isolated-high-risk-edits
reviewed: 2026-11-02T00:00:00Z
depth: deep
files_reviewed: 8
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
  - src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java
  - src/main/java/com/clickhouse/kafka/connect/util/Utils.java
  - src/test/java/com/clickhouse/kafka/connect/sink/OriginalTopicInsertedOffsetsUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java
  - docs/FORK-DELTA.md
findings:
  critical: 0
  warning: 1
  info: 4
  total: 5
status: issues_found
---

# Phase 42: Code Review Report

**Reviewed:** 2026-11-02
**Depth:** deep
**Files Reviewed:** 8
**Status:** issues_found

## Summary

Reviewed the diff `1c808c02..HEAD` for the three production edits (RSK-01 `Processing.doLogic`, RSK-02 `Utils.handleException`, RSK-03 `ClickHouseSinkTask.start` WARN), the four new test classes and the FORK-DELTA rows. I also traced callers across modules (`ProxySinkTask.put`, `ChunkFlusher.putDirect`, `DirectDeliveryStrategy.preCommit`, `BaseStateProviderImpl`, `ClickHouseSinkConfig.parseErrorCodes`) and ran the three Docker-free test classes (`testUnit`, 65 of 65 pass).

**RSK-01 (Processing line 137):** no defect found. `originalTopic` is used only as the last constructor argument of `StateRecord` and in `stateRecord.setOriginalTopic(...)`. Its only reader is `BaseStateProviderImpl.onStateUpdate`, which keys `lastInsertedOffsets`. `topic` (the transformed topic) still drives `getStateRecord`/`setStateRecord`, `extractRange`, logging and the BEFORE/AFTER sequence. `KeeperStateProvider` never reads `originalTopic`, so no stored row or Keeper key moves. `SinkRecord.originalTopic()` is never null for records built by the Connect runtime (the constructors default it to `topic()`). `onPartitionRemoved` is now called with Connect's own `TopicPartition` keys and matches. The AFTER_PROCESSING SAME/CONTAINS branch calls `onStateUpdate(stateRecord)` after `setOriginalTopic`, so that path is also keyed correctly. The remaining many-to-one and partition-changing limits are the accepted OI-35-2/OI-35-3 limits and are not re-raised.

**RSK-03 (start WARN):** no defect found. Parsing happens in the `ClickHouseSinkConfig` constructor (`parseErrorCodes`, which trims, skips empty entries and rejects non-numeric or non-positive values), inside the `try` that already converts failures to `ConnectException`. `warnOnUnvettedRetriableCodes` runs after it, receives a non-null unmodifiable `Set<Integer>`, and does only a `TreeSet` copy, a `removeAll` and a log call, so it cannot throw. The message carries only integer codes and fixed text, no record data. The vetted set matches the doc: 15 default codes plus 216.

**RSK-02 (chain pass-through):** the placement is as documented, after the server-error block and before the DataException/tolerance block. One termination hazard and one logging defect are described below.

## Warnings

### WR-01: New cause-chain loop in `handleException` does not terminate on a cyclic cause chain that contains a ClickHouse server error

**File:** `src/main/java/com/clickhouse/kafka/connect/util/Utils.java:106-110`
**Issue:** `for (Throwable t = e; t != null; t = t.getCause())` has no cycle guard. `Throwable.initCause` only rejects a self-cause, so a longer cycle (`S -> R -> S`) can exist. The existing `getRootCause(e, true)` at line 93 stops at the first `ClickHouseException` or `ServerException`, so a cycle that contains a server error does not hang there. The server-error block then logs "wasn't in the acceptable list" for a non-retriable code and falls through to the new loop, which spins forever on the sink task thread with no `RetriableException` in the cycle. Before this change that input fell through to the tolerance handling. Without a server error in the cycle, `getRootCause` already hangs, which is why this is a narrow regression. The cycle is contrived, but the failure is a silent hang of a task thread inside the error handler, with no log.
**Fix:** Bound the walk, or track visited nodes by identity.
```java
Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
for (Throwable t = e; t != null && seen.add(t); t = t.getCause()) {
    if (t instanceof RetriableException) {
        throw t == e ? (RetriableException) e : new RetriableException(e);
    }
}
```
Add a unit case with a two-node cycle containing a `ServerException` of a non-retriable code, expecting a normal tolerance outcome instead of a hang.

## Info

### IN-01: The pass-through branch logs nothing, and the server-error ERROR line contradicts the outcome when a RetriableException overrides it

**File:** `src/main/java/com/clickhouse/kafka/connect/util/Utils.java:102-110`
**Issue:** For `RetriableException` over a non-retriable server code (Known cost 17, accepted behaviour), line 102 logs at ERROR `Error code [60] wasn't in the acceptable list.` and the code then throws a `RetriableException`. An operator reading the log sees an "unacceptable" code followed by endless redelivery with no explanation. Every other retriable branch in the method logs a WARN naming the reason before it throws. This one does not.
**Fix:** Log a WARN in the new branch before the throw, for example `LOGGER.warn("RetriableException found in the cause chain, keeping the failure retriable: {}", e.getLocalizedMessage());`. Do not include record data.

### IN-02: The vetted retriable-code set is defined in two places

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java:22-23` (and `src/main/java/com/clickhouse/kafka/connect/util/Utils.java:65-81`)
**Issue:** `QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING = 216` is a private constant in `ClickHouseSinkTask`, while the other vetted codes live in `Utils.DEFAULT_RETRIABLE_ERROR_CODES`. The next person to edit the vetted set has to know to touch both files. The WARN text is also a long `String` constant in the task class.
**Fix:** Move the 216 constant (or a `VETTED_ADDITIONAL_CODES` set) next to `DEFAULT_RETRIABLE_ERROR_CODES` in `Utils`, so there is a single definition of "vetted".

### IN-03: `UnvettedRetriableCodesWarnUnitTest` has a port race and misses the blank-entry shapes

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java:41-47, 91-110`
**Issue:** (a) `closedPort()` opens a `ServerSocket(0)`, closes it and returns the number. Another process can bind that port before `start()` connects, and the test then fails with an unexpected exception or hangs on a live listener. That is a rare flake, but it is avoidable. (b) The cases cover unset, vetted, and unvetted lists with spaces, but not the entries the review brief called out: blank or whitespace-only (`" "`), empty entries (`"159,,60"`, `"60,"`), duplicates (`"60,60"`), and a mixed vetted and unvetted list with a trailing comma. `parseErrorCodes` handles all of them today, so the WARN output would be right. Nothing pins that, and a change to `parseErrorCodes` could silently alter the WARN.
**Fix:** Add `" "`, `","`, `"159,,60"`, `"60,60"` and `"60,"` to `vettedSettings` and `unvettedSettings` with their expected lists (`[]` produces no WARN, `[60]` for the rest). For the port, use an unroutable reserved address, or keep the `ServerSocket` bound without calling `accept` and close it only after the assertion.

### IN-04: `RetriableExceptionPassThroughUnitTest` carries an unused outcome and has no DataException control

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java:63-69, 134-136, 160`
**Issue:** `Outcome.DATA_EXCEPTION`, its `classify` branch and the `assertSame` arm for it are never selected by `directMatrix`. Nothing in this class checks that a plain `DataException` with no `RetriableException` in its chain is still rethrown as itself under `none` and handed on under `all`. The class header says unchanged shapes are pinned by `_CONTROL` rows, but the DataException branch, which the new loop was deliberately placed in front of, has no control row here (it is covered only in `WrappedDataExceptionClassificationPinUnitTest`). A regression that moved the loop and also broke the DataException branch would not show up in this matrix.
**Fix:** Add a `PLAIN_DATA_EXCEPTION_CONTROL` shape with rows `(none, DATA_EXCEPTION)` and `(all, RETURNS)`, or delete the unused enum value and branch.

---

_Reviewed: 2026-11-02_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_
