---
phase: 04-json-insert-path-connector-plumbing
reviewed: 2026-09-17T21:01:05Z
depth: standard
files_reviewed: 10
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
  - src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertSettingsUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/MBeanRegistrationLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java
findings:
  critical: 0
  warning: 1
  info: 1
  total: 2
status: issues_found
---

# Phase 04: Code Review Report

**Reviewed:** 2026-09-17T21:01:05Z
**Depth:** standard
**Files Reviewed:** 10
**Status:** issues_found

## Summary

This phase's only production change is the extraction of `ProxySinkTask.buildMBeanName(String, int,
String)` out of the private `getMBeanName()`, done specifically to make the null-version and
colon-in-version branches reachable from a Docker-free unit test. I diffed the extraction against
`b63420b6657a923425565a7315820a950220aea1^` line by line: the moved logic (`version != null` check,
`:` → `_` replace, the `String.format` call and its argument order) is byte-for-byte unchanged, only
relocated into a new `static` method that `getMBeanName()` now delegates to. This extraction is
behavior-preserving as claimed.

The remaining nine files are new tests. I read each one in full, cross-checked their claims against
the actual production code they pin (`ClickHouseSinkConfig`'s `jsonInsertColumns`/`insertFormat`
parsing, `ClickHouseWriter.cleanupExtraFields`/`shouldSkipTableMetadata`/`getTable`, and
`MBeanServerUtils`), and ran the four Docker-free unit-test classes (`MBeanNameUnitTest`,
`AsyncInsertSettingsUnitTest`, `JsonInsertColumnsUnitTest`, `MetadataFreeJsonInsertUnitTest` — 27
tests total) against the current build: all pass. I also ran `compileTestJava` and `spotlessJavaCheck`
— both clean for every file in this phase's scope (the one Spotless violation reported belongs to
`ClickHouseHelperClientForkUnitTest.java`, which is not in this phase's file list). I attempted to run
the Testcontainers-backed live tests but the local Colima Docker socket does not expose a routable
host IP for the Ryuk resource-reaper sidecar (`Docker host IP address is null`), so those tests could
not execute in this sandbox; this is a local Colima network-driver limitation, not a defect in the
code under review, and I do not have a way to work around it without touching machine-level Docker
configuration. I verified the live tests' claims by tracing the exact code paths they assert against
instead (see below).

Per the phase intent, `ExplicitJsonInsertPathLiveTest.explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch`
is a deliberate, human-approved pin of confirmed silent data loss (`insertFormat=json` + `Struct` +
default `bypassRowBinary`). Its Javadoc is headed "R-01 PIN - NOT AN ENDORSEMENT.", explains the
mechanism precisely (`doInsertRawBinaryV2` iterating an empty `table.getRootColumnsList()` for the
synthetic metadata-free `Table`), and explicitly defers a fail-fast fix to a future, separately
scoped decision. This framing is clear and unambiguous; I am not reporting it as a bug, per the
phase's explicit instruction.

Overall the test suite in this phase is well constructed: assertions consistently check typed column
values (not just row counts), tombstone/mixed-batch and DEFAULT-column edge cases are covered,
non-retriable vs. retriable exception classification is pinned explicitly (COLS-05), and the
`GenericRecord` mutable-iterator-aliasing pitfall is called out and correctly avoided in every helper
that reads rows back. I found one quality inconsistency (missing try/finally around task lifecycle in
two tests, mitigated by an existing `@AfterEach` MBean sweep) and one minor doc-quality nit. No
Critical findings.

## Warnings

### WR-01: Two live tests skip task lifecycle try/finally that every sibling test in this phase uses

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java:73-91`
**Issue:** `explicitJsonInsertSkipsTableCheckOnStart` (lines 73-91) and
`explicitJsonInsertIgnoresTargetTableFilter` (lines 108-122) call `chst.put(records)` and `chst.stop()`
as bare statements with no surrounding `try`/`finally`. Every other live test added in this phase
(`AsyncInsertLiveTest`, `ExplicitJsonInsertPathLiveTest`, `JsonInsertColumnsLiveTest`,
`MBeanRegistrationLiveTest`) wraps the task lifecycle in `try { ... } finally { task.stop(); ...
dropTable(...); }` specifically so a failed assertion or an unexpected exception from `put()` still
unregisters the task's MBean and drops its table. In these two tests, if `put()` throws for any reason
other than the one under test, `chst.stop()` is skipped, and the JMX MBean the constructor registered
in `ProxySinkTask`'s constructor is left registered until `ClickHouseBase`'s `@AfterEach
cleanupLeakedMBeans()` sweep unregisters it on the next test boundary — a real safety net, but one
this phase's other eight test classes correctly avoid needing at all. It's an inconsistency in an
otherwise carefully-hardened test suite (the sibling classes explicitly comment on why they guard
against exactly this).
**Fix:**
```java
ClickHouseSinkTask chst = new ClickHouseSinkTask();
assertDoesNotThrow(() -> chst.start(props));
try {
    createTable(chc, tableName);
    Collection<SinkRecord> records = createJsonRecords(topic, 0, RECORD_COUNT);
    chst.put(records);

    assertEquals(records.size(), ClickHouseTestHelpers.countRows(chc, tableName));
    // ... row assertions ...
} finally {
    chst.stop();
    ClickHouseTestHelpers.dropTable(chc, tableName);
}
```
(same pattern for `explicitJsonInsertIgnoresTargetTableFilter`).

## Info

### IN-01: `AsyncInsertLiveTest`'s class Javadoc describes a race it does not itself test

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertLiveTest.java:20-22`
**Issue:** The class Javadoc states "With `async_insert=1` alone the server acknowledges before the
data is queryable, so a read immediately after `put()` can legitimately see zero rows (D-15)" as
justification for why the single test in this class always sets `wait_for_async_insert=1`. That's a
correct and useful rationale for the test's own determinism, but as written it reads as though the
bare-`async_insert=1` race is itself pinned somewhere in this class — it isn't; no test in this file
exercises `async_insert=1` without `wait_for_async_insert=1`. Not a functional problem (the one test
present, ASYNC-03, is correctly named and asserts what it claims), just a documentation clarity nit.
**Fix:** Reword to make explicit that the race is cited only as motivation for requiring
`wait_for_async_insert=1` in this test, not as something this file pins, e.g.: "...so a read
immediately after `put()` can legitimately see zero rows (D-15) — this class does not test that
race directly, only the deterministic wait_for_async_insert=1 path."

---

_Reviewed: 2026-09-17T21:01:05Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
