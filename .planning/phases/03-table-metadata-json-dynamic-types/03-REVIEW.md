---
phase: 03-table-metadata-json-dynamic-types
reviewed: 2026-09-17T00:00:00Z
depth: standard
files_reviewed: 9
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonDynamicRoundTripTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryFailFastLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/JsonDynamicTypeMappingTest.java
findings:
  critical: 0
  warning: 3
  info: 1
  total: 4
status: issues_found
---

# Phase 03: Code Review Report

**Reviewed:** 2026-09-17T00:00:00Z
**Depth:** standard
**Files Reviewed:** 9
**Status:** issues_found

## Summary

Reviewed the two authorized production changes (`ClickHouseHelperClient`'s `buildTableFilterClause`/
`shouldSkipDescribeRow` extraction plus UTF-8 pinning, and `ClickHouseWriter`'s `doWriteColValue`
default-branch/`validateDataSchema` fail-fast) and the seven test files that pin them, at standard
depth, including tracing into `Table.java`/`Column.java`/`Utils.java` (not in the file list) where the
production code under review called into them.

Specific checks requested by the phase brief, and their outcomes:

- **`buildTableFilterClause` SQL-literal escaping** — verified sufficient. The phase's own research
  (`03-RESEARCH.md` R-03, cross-checked here) live-falsified the premise that ClickHouse's single-quoted
  string literal honors backslash escapes the way MySQL/C do; only `'`→`''` doubling is needed, and the
  code does exactly that. No injection or literal-breakage found for any input, including backslash-heavy
  regex filters. Not re-flagged as a bug.
- **`shouldSkipDescribeRow`'s DESCRIBE-row skip rule** — sufficient for the single-level case exercised by
  every test in this phase, but only inspects the *immediate* parent (`lastIndexOf('.')`), not the full
  ancestor chain. See **WR-01**.
- **`doWriteColValue`'s new throw** — correctly typed as `org.apache.kafka.connect.errors.DataException`
  (non-retriable, matches `Utils.handleException`'s `e instanceof DataException` branch), thrown before any
  byte reaches the stream (confirmed via `RowBinaryUnsupportedTypeGateTest`'s `out.size() == 0` assertion),
  and — because it sits in the `default:` case of a type switch rather than an enumerated allow-list — it
  automatically covers every `Type` the switch doesn't otherwise handle (including `INT128`/`INT256`/
  `UINT128`/`UINT256`, which were never explicitly named as in-scope for this phase but get the same
  protection for free). Good defensive design; no bug here.
- **`validateDataSchema`'s new case**, however, does *not* share that generality — it enumerates
  `"JSON"`/`"DYNAMIC"`/`"UNKNOWN"` explicitly rather than reusing the same "everything not otherwise
  recognized" default `doWriteColValue` relies on. See **WR-03**.

No Critical-severity issues were found: every path that can lose or corrupt inserted data was already
closed off correctly by this phase's `DataException` fixes. The Warnings below are about the two
fail-fast gates not being fully equivalent to each other, and about a structural skip rule and a
pre-existing `database`-parameter inconsistency that are narrower than their documentation/tests imply.

## Warnings

### WR-01: `shouldSkipDescribeRow` only checks the immediate parent, not the full ancestor chain

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:320-335`

**Issue:** The D-07 structural rule resolves `parentName` via `name.lastIndexOf('.')` and looks it up in
`table.getAllColumnsMap()`. That map is only populated for rows that were *not* skipped and therefore
reached `table.addColumn(...)` (`Table.java:65-80`). For a subcolumn one level below a JSON/Dynamic root
(e.g. `json_col.a`), this works: `json_col` was already added, the lookup succeeds, the row is skipped.

For a subcolumn *two or more* levels deep — which DESCRIBE TABLE emits for a JSON column that declares an
explicit typed path, e.g. `json_col JSON(a.b UInt32)`, producing a row named `json_col.a.b` — the lookup
uses `parentName = "json_col.a"`. That intermediate path was never itself emitted as its own DESCRIBE row
(ClickHouse emits the full typed leaf path directly), so it was never added to `table`, so
`table.getAllColumnsMap().get("json_col.a")` returns `null`, so the row is **not** skipped. It then flows
into `Column.extractColumn` and `Table.addColumn` → `handleNonRoot`, whose own immediate-parent lookup also
fails, logging `LOGGER.error("Got non-root column, but its parent was not found to be updated. {}", column)`
(`Table.java:86`) on every `describeTable()` call for that table, and leaving the column registered in
`allColumnsMap`/`allColumnsList` but orphaned from both `rootColumnsList` and the JSON column's own
structure.

This does not lose data for the JSON column itself (it is still written as a whole JSON blob, not
per-path), but it does produce persistent ERROR-level log noise (this phase raised that same log from WARN
to ERROR, D-09, making the noise louder) and an inconsistency between the javadoc's claim ("when it is a
child of a column whose resolved type is JSON or DYNAMIC") and what the one-hop implementation actually
covers. No test in this phase (`DESC-01`..`DESC-03`, `ClickHouseHelperClientForkLiveTest`,
`ClickHouseHelperClientForkUnitTest`) exercises a two-level-deep JSON/Dynamic subcolumn — every fixture is
either a plain `JSON`/`Dynamic` column (which the phase's own research at `03-RESEARCH.md:267` found emits
*no* subcolumn rows at all via `describe_include_subcolumns=1`, even after inserting nested data) or a
single-level parameterized case.

**Fix:** Either walk the full ancestor chain (try progressively shorter prefixes up to the first dot, not
just the last one, and skip if any registered ancestor resolves to `JSON`/`DYNAMIC`), or thread a
`Set<String> skippedStructuralPrefixes` through the describe loop that `shouldSkipDescribeRow` can consult
so a multi-level descendant of an already-skipped JSON/Dynamic row is recognized even though its immediate
parent was never registered:

```java
static boolean shouldSkipDescribeRow(ClickHouseFieldDescriptor fieldDescriptor, Table table) {
    if (!fieldDescriptor.isSubcolumn()) {
        return false;
    }
    String name = fieldDescriptor.getName();
    if (name.endsWith(".null") || name.endsWith(".size0") || name.endsWith(".size")) {
        return true;
    }
    // Walk every ancestor prefix, not just the immediate parent.
    int dot = name.indexOf('.');
    while (dot >= 0) {
        String ancestorName = name.substring(0, dot);
        Column ancestor = table.getAllColumnsMap().get(ancestorName);
        if (ancestor != null && (ancestor.getType() == Type.JSON || ancestor.getType() == Type.DYNAMIC)) {
            return true;
        }
        dot = name.indexOf('.', dot + 1);
    }
    return false;
}
```

### WR-02: `describeTableV2` builds its DESCRIBE query from `this.database`, not the `database` parameter (pre-existing, dormant)

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:446-489`

**Issue:** `describeTableV2(String database, String tableName)` builds the query text with
`String.format("DESCRIBE TABLE \`%s\`.\`%s\`", this.database, tableName)` — the client's own configured
database, not the `database` argument the method was actually called with — while the returned `Table`
object (`new Table(database, tableName)`) and `settings.setDatabase(database)` both correctly use the
parameter. `describeTableV1` (same file, line 393-444) does not have this bug: it consistently uses the
`database` parameter for the query text. This asymmetry is not part of this phase's diff (unchanged by
commit `ebca05a`), but the file is in full-file review scope and the discrepancy is directly relevant to
"table discovery," which is this phase's stated focus.

Currently dormant: every production and test caller (`extractTablesMapping` at line 507,
`ClickHouseHelperClientTest`, `ClickHouseHelperClientForkLiveTest`, `TableTest`) invokes
`describeTable`/`describeTableV2` with `database == this.database` (via `chc.getDatabase()` or
`this.database` directly), so the bug never currently surfaces. It is nonetheless a latent violation of the
method's own parameter contract: if `describeTable` is ever called with a database different from the
client's configured default (e.g. a future multi-database feature, or a test written against the public
API taking the parameter at face value), the DESCRIBE query silently targets the wrong database while the
returned `Table` claims to represent the requested one.

**Fix:** Use the parameter consistently, matching `describeTableV1`:

```java
String describeQuery = String.format("DESCRIBE TABLE `%s`.`%s`", database, tableName);
```

### WR-03: `validateDataSchema`'s new fail-fast case is a narrower allow-list than `doWriteColValue`'s, so the two "TYPE-06 gates" can drift for types neither enumerates

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:238-309` (case added at 267-281), compare with the `default:` at 563-568

**Issue:** `doWriteColValue`'s fix throws the actionable `DataException` from its `default:` case — i.e.
for *any* `Type` not explicitly handled above it in the switch, present or future. `validateDataSchema`'s
fix instead adds three explicit case labels (`"JSON"`, `"DYNAMIC"`, `"UNKNOWN"`) ahead of its own
`default:`, which still runs the old comparison-based validation (`colTypeName.equals(dataTypeName)`, with
a handful of unconditional `continue`s for `STRING`/`BYTES`, `TUPLE`/`STRUCT`, `UINT8`..`UINT64`, and
`DECIMAL`). Any `Type` that reaches `doWriteColValue`'s generic `default:` throw but is *not* one of
`JSON`/`DYNAMIC`/`UNKNOWN` — concretely, `INT128`, `INT256`, `UINT128`, `UINT256` today, none of which are
handled in either switch's non-default cases, and any type added to the `Type` enum in the future without
being taught to both switches — will instead fall into `validateDataSchema`'s `default:` case. Kafka
Connect has no native 128/256-bit integer schema type, so `dataTypeName` for such a field will essentially
never equal `colTypeName` (e.g. `"INT128"`), so `validSchema` is set to `false` rather than throwing
directly. The caller then hits the pre-existing, unrelated

```java
if (!csc.isBypassSchemaValidation() && !validateDataSchema(table, first, false))
    throw new RuntimeException("Data schema validation failed.");
```

at `ClickHouseWriter.java:777` / `:838` — the exact bare, unactionable failure this phase's commit message
(`0d51354`) describes fixing ("today throws a bare, unactionable failure ... Data schema validation
failed.") — for a non-nullable `INT128`/`INT256`/`UINT128`/`UINT256` root column, when schema validation is
not bypassed. No data is lost (the throw still happens before any bytes are written), but the phase's own
stated goal — one shared, actionable message for every RowBinary-unsupported type — is only achieved for
JSON/Dynamic/Unknown, not for the full set of types `doWriteColValue` itself now protects.

**Fix:** Make `validateDataSchema`'s gate check against the same authority `doWriteColValue` uses instead
of a second, independently-maintained allow-list — e.g. extract the "does `doWriteColValue` support this
`Type` without special per-value validation" question into one shared predicate (or reuse the same switch's
case list via an `EnumSet` of RowBinary-primitive-comparable types) so a type added to one gate is
automatically covered by the other.

## Info

### IN-01: FILT-01's "single-space filter is not empty" unit pin is unreachable via the real config path

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java:31-35`, compare
`src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:293`

**Issue:** `singleSpaceFilterProducesNonEmptyClause` calls `ClickHouseHelperClient.buildTableFilterClause(" ")`
directly and asserts the result is non-empty, correctly pinning that the extracted function does an exact
`isEmpty()` check rather than trimming. However, in the actual production path, `ClickHouseSinkConfig`
already does `props.getOrDefault(TARGET_TABLE_FILTER, "").trim()` before the value ever reaches
`ClickHouseHelperClient` (via `ClickHouseWriter.start()` → `ClickHouseClientBuilder.setTargetTableFilter(csc.getTargetTableFilter())`).
A configured `targetTableFilter=" "` is therefore always reduced to `""` before `buildTableFilterClause`
ever sees it — the scenario this test exercises can never occur through the connector's real config
plumbing. The test is not wrong about the unit under test, but it pins a code path that is unreachable in
production, which could mislead a future reader into thinking a single-space filter behaves differently
from an empty one in practice.

**Fix:** Either keep the test but add a one-line comment noting `ClickHouseSinkConfig` trims upstream so
this exercises `buildTableFilterClause`'s own contract only (not an end-to-end reachable scenario), or fold
it into a config-level test that goes through `ClickHouseSinkConfig` to make the reachable behavior (a
whitespace-only filter is always treated as "no filter") explicit instead.

---

_Reviewed: 2026-09-17T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
