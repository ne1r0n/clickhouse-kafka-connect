---
phase: 40-helper-client-closing
reviewed: 2026-11-02T00:00:00Z
depth: standard
files_reviewed: 10
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java
  - src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderClosingUnitTest.java
  - docs/FORK-DELTA.md
findings:
  critical: 0
  warning: 3
  info: 3
  total: 6
status: issues_found
---

# Phase 40: Code Review Report

**Reviewed:** 2026-11-02
**Depth:** standard
**Files Reviewed:** 10
**Status:** issues_found

## Summary

Reviewed the diff `52ce1cd4..HEAD` for the three helper-client close sites (writer `chc`, V1 per-batch helper, `KeeperStateProvider` own client) and the new tests.

The exactly-once read/write paths (`getStateRecord`, `setStateRecord`, `Processing`) are untouched. The close ordering is sound:
- The writer stops its executor and waits before it closes `chc`.
- The field is nulled before the close, so a second `stop()` is a no-op.
- `ClickHouseSinkTask.start()` assigns `proxySinkTask` before `start()`, so a failed start is still followed by `stop()` and `closeHelperClient()` runs.
- `ClickHouseHelperClient` always builds both a V1 node and a V2 client in its constructor, so closing the per-batch helper is correct on V1. The `ClickHouseNode` is a value object that survives the close.
- `TableMappingRefresher.run` catches `Exception`, so the NPE a late refresh gets from the nulled `chc` is contained, as FORK-DELTA claims.

No blockers. The findings below are error-path asymmetries, a leaked test client in a phase about leaking clients, and test-pinning concerns. The accepted items in the brief (5 s timeout race, Keeper constructor leak, `ClickHouseSinkTask.stop()` lacking try/finally, the `runWithWriter` seed) were not re-raised.

## Warnings

### WR-01: A throwing per-batch helper `close()` now fails the insert batch

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1821-1834`
**Issue:** `getMutationRequest` uses try-with-resources on `chcTmp`. `ClickHouseHelperClient.close()` delegates to the V2 `Client.close()`, and any `RuntimeException` from it propagates out of `getMutationRequest`.
- The call sits on the insert hot path of `doInsertRawBinary`, `doInsertJsonV1` and `doInsertString`, inside the exactly-once BEFORE/AFTER window.
- Before this phase the throwaway client was never closed, so no insert could fail on that step. A failure releasing a throwaway object now aborts a batch whose `ClickHouseNode` was already read successfully.
- The writer's own `closeHelperClient()` deliberately swallows and WARN-logs close failures. The per-batch path has no such protection, so the two sites disagree.

**Fix:** Release the per-batch helper through the same swallow-and-log path. Replace try-with-resources with:
```java
ClickHouseHelperClient chcTmp = createHelperClient(...);
ClickHouseNode server;
try {
    server = chcTmp.getServer();
} finally {
    closeQuietly(chcTmp);   // shared with closeHelperClient(): try { c.close(); } catch (Exception e) { LOGGER.warn(HELPER_CLIENT_CLOSE_FAILURE_LOG, e); }
}
```
Have `closeHelperClient()` call the same helper.

### WR-02: `KeeperStateProvider.close()` can throw, and `ProxySinkTask.stop()` then loses the primary exception

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java:104-111`, `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:77-87`
**Issue:** `close()` calls `client.close()` unguarded, unlike the writer. In `ProxySinkTask.stop()` it runs in the innermost `finally`.
- If `dbWriter.stop()` threw and then `stateProvider.close()` also throws, the second exception replaces the first. The writer failure is dropped with no `addSuppressed` and no log.
- FORK-DELTA documents "the last exception thrown reaches the caller" as intended, but the lost one is the root cause that an operator needs. `ProxySinkTaskStopUnitTest` pins only the case where one step throws.
- The nested `finally` chain means a throwing `unregisterMBean` also hides a writer failure.

**Fix:** Either make `KeeperStateProvider.close()` log and swallow like `closeHelperClient()`, or accumulate in `ProxySinkTask.stop()`:
```java
RuntimeException failure = null;
try { dbWriter.stop(); } catch (RuntimeException e) { failure = e; }
try { statistics.unregisterMBean(); } catch (RuntimeException e) { failure = merge(failure, e); }
try { stateProvider.close(); } catch (RuntimeException e) { failure = merge(failure, e); }
if (failure != null) throw failure;
```
`merge` returns the first exception and adds the later one with `addSuppressed`. Add a test where two steps throw and the first is reachable from the thrown exception.

### WR-03: `ClickHouseWriterTest.runWithWriter` leaks its `seedClient`

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java:129-141`
**Issue:** The phase adds `ClickHouseHelperClient seedClient = ClickHouseTestHelpers.createClient(props)` and never closes it. `ClickHouseHelperClient` is `AutoCloseable` and builds a V2 `Client`, so every call of `runWithWriter` leaks one, in the phase whose goal is that nothing leaks a helper client.
- If `SINGLE_INT16_TABLE...execute(seedClient)` throws, the client leaks as well, because the call sits outside the `try/finally`.
- The seed table drop only runs once the writer has been created, so a failed seed `execute` leaves the table behind.

**Fix:** Wrap the whole method body:
```java
try (ClickHouseHelperClient seedClient = ClickHouseTestHelpers.createClient(props)) {
    String seedTable = createTopicName("writer_start_seed");
    SINGLE_INT16_TABLE.get().tableName(seedTable).execute(seedClient);
    ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
    try {
        assertTrue(writer.start(new ClickHouseSinkConfig(props)));
        test.accept(writer);
    } finally {
        writer.stop();
        ClickHouseTestHelpers.dropTable(seedClient, seedTable);
    }
}
```

## Info

### IN-01: `ownedClient` duplicates `chc`, and the "injected client stays with its owner" clause has no behavioural pin

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java:30-31,66`
**Issue:** `ownedClient` is assigned from `chc` on one line of the only constructor that can finish. The injected-client constructor `KeeperStateProvider(ClickHouseHelperClient)` NPEs in `init()` because `csc` is null, so it cannot produce an instance, as `KeeperStateProviderClosingUnitTest` itself shows. The field therefore guards a path that cannot be built.

`injectedClientConstructorStillFailsBeforeAnyInstanceExistsTripwire` asserts a pre-existing defect (`NullPointerException`) as a contract. A repair of that constructor, which the test message invites, fails this test and not the invariant it names. The tripwire message does say to add a case in that event.

**Fix:** Acceptable as is. Consider repairing the constructor (set `csc`, or reject it) in a later phase and replacing the tripwire with a real "close leaves an injected client open" case. Neither field is `volatile`; that is fine only while start and stop run on the task thread, as Connect does.

### IN-02: After `stop()` a late `updateMapping` fails with an uninformative message

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:179,214-218,229`
**Issue:** `closeHelperClient()` nulls `chc`, so a refresh that outlives the 5 s wait throws an NPE at `this.chc.extractTablesMapping`. `TableMappingRefresher` logs only `e.getMessage()`, which is `null` for an NPE, so the log shows `Update mapping Error: null`.

The public `getServer()` also NPEs after `stop()` where it used to return the node. It has no callers in `src/main`.

**Fix:** Optional. Snapshot `chc` into a local at the top of `updateMapping` and return `false` with a DEBUG log when it is null.

### IN-03: `ProxySinkTaskStopUnitTest` does not discriminate every branch of the nested `finally` chain

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java:111-140`
**Issue:** Only the writer-throws and the all-succeed cases exist. Nothing covers `unregisterMBean` throwing while still closing the provider, or `close()` throwing. The test also swaps `stateProvider` by reflection, so it never exercises a real `KeeperStateProvider` through `ProxySinkTask`. It would still pass if a refactor flattened the nested `finally` into sequential statements in the case where only one step throws before the provider.

**Fix:** Add a case with a throwing `StateProvider.close()` and one with a throwing statistics unregister, and assert the later steps still run. This pairs with WR-02.

---

_Reviewed: 2026-11-02_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
