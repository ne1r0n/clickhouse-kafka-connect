---
phase: 09-metadata-free-json-path-keep-or-drop
reviewed: 2026-09-20T00:00:00Z
depth: standard
files_reviewed: 6
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/TableTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java
findings:
  critical: 1
  warning: 1
  info: 1
  total: 3
status: issues_found
---

# Phase 9: Code Review Report

**Reviewed:** 2026-09-20T00:00:00Z
**Depth:** standard
**Files Reviewed:** 6
**Status:** issues_found

## Summary

The four documented guard sites in `ClickHouseWriter` (`start`, `updateMapping`, `getTable`, `cleanupExtraFields`) are implemented exactly as `09-CONTEXT.md`/`09-01-PLAN.md` specify: one shared predicate (`shouldSkipTableMetadata()`) keyed only on `csc.isJsonInsertFormatExplicit()` — never on `csc.getInsertFormat()`, so the bypass is not widened to default-configured connectors; the `updateMapping` guard sits above `isUpdateMappingRunning.compareAndSet(false, true)`, so the in-progress flag can never be stranded; the synthetic `Table` uses the upstream five-argument constructor with `false, List.of(), 0` and resolves its name through the existing `Utils.getMappedOrTopicTableName` helper, so `Table.getName()`'s own `Utils.escapeName` call is the only escaping applied (no double-escaping); and `jsonInsertColumns` is re-attached in `doInsertJsonV2` only, branching on list emptiness (never on `insertFormat`), reading the getter once into a local, leaving the dedup-token/query-id construction and `doInsertJsonV1` untouched. The re-enabled test classes are unweakened, and the two new Docker-free pins in `MetadataFreeJsonInsertUnitTest` include the negative half (an `NullPointerException` under a non-explicit config) that stops the positive assertions from passing vacuously.

One genuine correctness gap was found that the phase's own four-guard model does not cover: `ClickHouseWriter.doInsert`'s `auto.evolve` branch calls `evolveTableSchema` unconditionally, with no `shouldSkipTableMetadata()` guard at all. Against the synthetic empty-column `Table`, every field in a `SCHEMA`/`DEBEZIUM_CDC` record is treated as a "missing column", which drives the code straight into `urgentTableUpdate` → `chc.describeTable(...)` — the exact metadata query the bypass exists to avoid (operator fact 3) — on every batch, with no caching. See CR-01.

## Critical Issues

### CR-01: `auto.evolve` bypasses the metadata-free guard and reaches `describeTable`/`ALTER TABLE` on every batch

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:210-222` (the `csc.isAutoEvolve()` branch in `doInsert`), with the unguarded consumer chain at `:1011` (`evolveTableSchema`), `:1050` (`refreshTableAfterDDL`) and `:1106` (`urgentTableUpdate`)

**Issue:**
`doInsert` calls `getTable(...)` (which correctly returns the synthetic, column-less `Table` under the bypass — D-04), but then, if `csc.isAutoEvolve()` is true, unconditionally calls `evolveTableSchema(table, lastFields)` with no `shouldSkipTableMetadata()` check:

```java
Table table = getTable(database, topic);
if (table == null) { return; }

if (csc.isAutoEvolve()) {
    Record last = records.get(records.size() - 1);
    Map<String, Schema> lastFields = new LinkedHashMap<>();
    if (last.getFields() != null) {
        for (Field f : last.getFields()) {
            lastFields.put(f.name(), f.schema());
        }
    }
    table = evolveTableSchema(table, lastFields);   // <-- no bypass guard
}
```

For a `SCHEMA`/`DEBEZIUM_CDC` record (`last.getFields() != null`), `evolveTableSchema` calls `table.getMissingColumns(allFields.keySet())`. Because the synthetic `Table`'s `rootColumnsMap` is empty (`List.of()`, per D-04), **every field name in the record's schema comes back as "missing"** — this is not an edge case, it is the guaranteed outcome any time `auto.evolve=true` is combined with `insertFormat=json` and a schema'd record, which the config layer does not prevent (`ClickHouseSinkConfig` validates only the `bufferCount`/`bufferFlushTime` vs `exactlyOnce` combination; there is no check preventing `auto.evolve=true` with an explicit JSON insert format).

`evolveTableSchema` then:
1. Issues `chc.alterTableAddColumns(...)` (`ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...`) for every field in the schema, on every batch — DDL traffic the bypass was never meant to generate, requiring `ALTER` privilege the operator-constrained connector user (operator fact 3) may equally lack.
2. Calls `refreshTableAfterDDL(table, missingColumns)` → `urgentTableUpdate(table)`. Inside `urgentTableUpdate`, `updateMapping(table.getDatabase())` returns `false` immediately (the guard added in this phase), so the method falls through to its `else` branch and calls `chc.describeTable(table.getDatabase(), table.getCleanName())` **directly** — the one metadata query D-01's entire rationale (operator fact 3: "the connector user may not be able to `DESCRIBE`/`SHOW` them") says must never happen.

Two concrete failure modes follow, both in production for the exact audience this phase was built for:
- If the connector user genuinely lacks `DESCRIBE`, `chc.describeTable` returns `null`, `urgentTableUpdate` throws `IllegalStateException("Unable to refresh table mapping for ...")`, and **every single batch fails** for that topic — a hard outage introduced by re-enabling `auto.evolve`, not a graceful degradation.
- If the connector user does have `DESCRIBE` (but chose the bypass for a different reason, e.g. the table not existing yet at `start()`), the connector silently reintroduces the exact per-batch `DESCRIBE` traffic D-03's guard on `updateMapping` was specifically designed to eliminate ("This also kills the background DESCRIBE traffic" — 09-CONTEXT.md D-03), and does so on the hot path, unbounded, every batch, with no caching (the synthetic `Table` returned by `getTable` is rebuilt fresh every call, so nothing here is ever memoized into `this.mapping`).

This is a fifth consumer of the empty-column synthetic `Table` that 09-CONTEXT.md's own enumeration ("⚠ `getTable` returns a table whose `getRootColumnsList()` is empty, and three downstream consumers read it: `validateDataSchema`, `doInsertRawBinaryV2`'s write loop (R-01), `cleanupExtraFields` (guarded)") does not list, and it was not addressed by any of the four guard sites in this plan. It is not covered by any of the "KNOWN AND ACCEPTED" items in the phase's review brief (those only cover the Struct+RowBinary R-01 loss, `updateMapping`'s `false` contract, the emptiness-only keying of the `jsonInsertColumns` branch, and the absence of connector-side `jsonInsertColumns` validation) — this is a distinct, previously unidentified defect.

**Fix:** Guard the `auto.evolve` branch with the same predicate used everywhere else, so the metadata-free promise holds under this combination too:

```java
if (csc.isAutoEvolve() && !shouldSkipTableMetadata()) {
    Record last = records.get(records.size() - 1);
    Map<String, Schema> lastFields = new LinkedHashMap<>();
    if (last.getFields() != null) {
        for (Field f : last.getFields()) {
            lastFields.put(f.name(), f.schema());
        }
    }
    table = evolveTableSchema(table, lastFields);
} else if (csc.isAutoEvolve()) {
    LOGGER.warn("auto.evolve is enabled but insertFormat is explicitly set to JSON (metadata-free mode); "
            + "schema evolution is skipped because it would require table metadata the bypass exists to avoid.");
}
```
Consider also rejecting the combination at config/`start()` time (loud, at startup, rather than silent per-batch) so operators discover the incompatibility immediately rather than on the first schema drift. Either way, `urgentTableUpdate`'s `describeTable` fallback should not be reachable while `shouldSkipTableMetadata()` is true; the fix belongs at the `doInsert` call site so the metadata-free contract stays enforced in exactly one place, matching D-05's single-predicate design.

## Warnings

### WR-01: Stale "Disabled pending..." class Javadoc left on three re-enabled live test classes

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java:37-41`, `src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java:35-38`, `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java:25-28`

**Issue:** All three classes have their `@Disabled` annotation removed and now run live inside the test suite, but each class-level Javadoc still reads, verbatim, "Disabled pending the JSONP-01 keep/drop decision (Phase 9, D-11): ... has not been re-applied to `src/main` in this phase. Every assertion below is left intact so Phase 9 can re-enable this class unchanged if the decision is keep." This is now factually wrong: the decision was made (KEEP), the bypass was re-applied, and the class is enabled. This is a deliberate, documented trade-off (D-10: "delete the `@Disabled` line and nothing else" so the diff itself proves the contract did not move) — `MetadataFreeJsonInsertUnitTest`'s own Javadoc was explicitly rewritten to describe its enabled state (Task 2 Step F), so the pattern for fixing this was already established once in this phase, just not applied to the other three classes. Left as-is, the next reader (or the next upstream sync) has to cross-reference `09-CONTEXT.md` to learn these classes are in fact live, and the stale text actively suggests re-enablement is still pending.

**Fix:** In each of the three classes, replace the "Disabled pending..." paragraph with the same pattern already used in `MetadataFreeJsonInsertUnitTest`'s Javadoc, e.g.: "Re-enabled per the JSONP-01 KEEP decision (Phase 9, D-01/D-10): the metadata-free `insertFormat=json` bypass this class exercises was re-applied to `src/main` in Phase 9; every assertion below is unchanged from the parked version." This is a doc-only, zero-risk follow-up and does not require reopening D-10's minimal-diff rule for the code itself.

## Info

### IN-01: `cleanupExtraFields`'s disjunction relies on evaluation order for null-safety, unlike the shared predicate it calls

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1400`

**Issue:** `shouldSkipTableMetadata()` is deliberately written as `csc != null && csc.isJsonInsertFormatExplicit()` so a constructed-but-never-started writer can never NPE from this predicate (per-file Javadoc and the plan's Task 2 acceptance criteria). `cleanupExtraFields`'s guard, `if (csc.isBypassFieldCleanup() || shouldSkipTableMetadata())`, evaluates `csc.isBypassFieldCleanup()` first, so if `csc` were ever null at this call site the method would NPE directly rather than benefiting from the predicate's null-check. Today this is unreachable — `cleanupExtraFields` is only invoked from `writeRecordsJson`, itself only reachable through `doInsertJson{V1,V2}`, both of which already assume a started (`csc`-populated) writer — but the inconsistency means the null-safety guarantee documented on `shouldSkipTableMetadata()` doesn't actually hold at every call site that uses it.

**Fix:** No functional change needed today; if this method is ever called from a path where `csc` could be unset, evaluate `shouldSkipTableMetadata()` first: `if (shouldSkipTableMetadata() || csc.isBypassFieldCleanup())`.

---

_Reviewed: 2026-09-20T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
