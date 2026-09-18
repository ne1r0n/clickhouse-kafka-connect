# Phase 4: JSON Insert Path & Connector Plumbing - Research

**Researched:** 2026-09-17
**Domain:** Kafka Connect sink connector internals (ClickHouse client-v2 insert paths, JMX MBean registration, ClickHouse settings passthrough) — test-coverage phase, not feature development
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Exactly one main-code change is pre-approved: the `ProxySinkTask.buildMBeanName` extraction (D-02). Nothing else. Every other main-code edit stops for a user decision at a blocking checkpoint — explicitly including: a fail-fast throw in the `insertFormat=json` + Struct + RowBinary routing path (even if R-01 proves silent data loss); any `jsonInsertColumns` validation/normalization/quoting; any change to `Utils.handleException`'s retriable/non-retriable classification; making the String-record route honour `jsonInsertColumns`. Never disable or `@Disabled` a test to get green.
- **D-02:** Extract `buildMBeanName` from `ProxySinkTask`. Add `static String buildMBeanName(String connectorName, int id, String version)` (package-private) holding today's null/colon logic and the exact format string; `getMBeanName()` becomes a one-line delegate. Output identical byte-for-byte. Nothing else in `ProxySinkTask`/`MBeanServerUtils` changes.
- **D-03:** JMX-01, JMX-02, JMX-03 are Docker-free unit tests on the helper (connector name present → `connector=<name>`; absent → `connector=null`; version null → `version=unknown`; version containing `:` → `_`).
- **D-04:** JMX-04 is a live registration test only, no overlapping unit test. Filter to the test's own connector names on the platform MBean server; assert distinctness; `stop()` one and assert exactly that one disappeared.
- **D-05:** Plain connector names only. No coverage of ObjectName-hostile names.
- **D-06:** Schemaless-first for JSON-01..04 (schemaless `Map` records are the only route where metadata-free mode changes behavior). JSON-05 upgrades the two existing `ClickHouseSinkTaskJsonInsertFormatTest` tests (String records) from `countRows` to typed column-value assertions.
- **D-07:** The `insertFormat=json` + Struct + no-`bypassRowBinary` combination is probed before decided (R-01). If it silently loses data, a fail-fast fix escalates under D-01 — not pre-approved.
- **D-08:** JSON-03's `cleanupExtraFields` bypass is pinned Docker-free via the existing `protected void setSinkConfig(ClickHouseSinkConfig)` seam — no new seam needed.
- **D-09:** JSON-04 covers a mixed batch (tombstones skipped, normal rows land) and an all-tombstone batch (pin whatever `client.insert(...)` does with an empty JSONEachRow stream; if it throws, that's ground truth and any fix escalates under D-01).
- **D-10:** COLS-05 pins the server-side error for a nonexistent `jsonInsertColumns` entry, including its classification (retriable vs. task-failing) via `Utils.handleException`. A retriable classification would itself be an escalation, not an autonomous fix.
- **D-11:** COLS-02's research flag is **downgraded to a note / withdrawn**. Phase 3's `JsonDynamicRoundTripTest` round-trips already exercised `doInsertJsonV2` with the default empty `jsonInsertColumns` list and are green. COLS-02 is a plain live test now.
- **D-12:** COLS-03's fixture uses explicit non-trivial `DEFAULT` expressions (e.g. `DEFAULT 42`, `DEFAULT 'unset'`) so "unlisted columns get their DEFAULT" is distinguishable from "never written."
- **D-13:** COLS-06 also pins the column-clause-vs-cleanup interaction: with default config, `cleanupExtraFields` keeps any payload key that is a real table column even if it's outside the configured `jsonInsertColumns` list — only the INSERT column clause excludes it.
- **D-14:** ASYNC-01/02 are Docker-free config unit tests on `ClickHouseSinkConfig.getClickhouseSettings()`.
- **D-15:** ASYNC-03 runs with `async_insert=1,wait_for_async_insert=1` to keep the assertion deterministic (no polling).
- **D-16:** One new fork-only test class per concern, each owned by exactly one plan. Docker-free classes must not extend `ClickHouseBase` (inherited `@Tag("clickhouse")` would exclude them from `testUnit`).
- **D-17:** Prior test conventions carry forward: `REQ-ID: ` prefix in `@DisplayName`, `@ParameterizedTest` for genuine matrices only, negative tests assert exception type + message substring, typed SQL read-back (never `countRows` alone), tables created before `start()`, try/finally around `start()`/`stop()`, `uniqueConnectorName(TestInfo)`. No `build.gradle.kts` change — all four touched classes are already in the `jacocoForkReport` include list (verified this session, see Sources).

### Claude's Discretion

- Names/packages of new fork-only test classes and how they map onto 04-01/04-02/04-03.
- Signature details of `buildMBeanName` beyond "package-private static, connector name + id + version, byte-for-byte identical output."
- Live fixture table shapes, column types, record counts, whether tests within a class share a table.
- Which COLS-01 parsing inputs get parameterized.
- Which JSON-01 letter-case variants get parameterized and how the negative half is expressed.
- Exact `@DisplayName` phrasing beyond the `REQ-ID: ` prefix.

### Deferred Ideas (OUT OF SCOPE)

- `MBeanServerUtils.registerMBean` swallows `MalformedObjectNameException` — a connector named `cdc,prod` or `a=b` runs but silently publishes no JMX metrics. Declined under D-05.
- The String-record route silently ignores `jsonInsertColumns` — not pinned, not fixed (D-01).
- Metadata-free `getTable` bypasses `Utils.getTableName`, so `enableDbTopicSplit` is skipped entirely in explicit JSON mode. Noted, untested.
- Wire-level verification that no `async_insert` reaches the server via `system.query_log` — declined under D-14.
- COLS-01 parsing edges beyond the requirement (duplicates, backticked/quoted names, names with spaces) — left to discretion, none required.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| JSON-01 | `insertFormat=json` in any letter case enables metadata-free mode; unset does not | `ClickHouseSinkConfig.java:225-240` parsing quoted below; `shouldSkipTableMetadata()` quoted below |
| JSON-02 | Metadata-free mode resolves target table via topic-to-table mapping without fetched metadata | `ClickHouseWriter.getTable` `:1241-1266` quoted below |
| JSON-03 | Metadata-free mode: fields absent from the table pass through unmodified, known fields land correctly | `cleanupExtraFields` `:1039-1051` quoted below; `setSinkConfig` seam `:81-82` quoted below |
| JSON-04 | Tombstones in metadata-free mode are skipped without error | `EmptyRecordConvertor` + `doInsertJsonV2`'s null-value branch `:1023-1025` quoted below |
| JSON-05 | Existing explicit-JSON tests assert column values, not just row counts | `ClickHouseSinkTaskJsonInsertFormatTest.java` read in full — both tests use `countRows` only today |
| COLS-01 | `jsonInsertColumns` parsing: blank→empty, whitespace/empty entries dropped, order preserved | `ClickHouseSinkConfig.java:241-252` quoted below |
| COLS-02 | Default empty list produces correct values (withdrawn research flag, D-11) | `JsonDynamicRoundTripTest.jsonAndDynamicRoundTripThroughSchemalessRoute` — green, drives `doInsertJsonV2` with default empty list |
| COLS-03 | Explicit subset fills listed columns, unlisted get `DEFAULT` | `doInsertJsonV2`'s 5-arg `client.insert(...)` call `:1031-1032` quoted below |
| COLS-04 | Extra JSON keys outside the list don't break the insert | Same as COLS-03 — JSONEachRow body vs. column clause are independent |
| COLS-05 | Nonexistent column in the list produces a clear, non-partial, non-NPE error with pinned classification | `Utils.handleException` `:63-127` quoted below; traced end-to-end |
| COLS-06 | `jsonInsertColumns` also applies on the non-explicit JSON path, and interacts with `cleanupExtraFields` | `cleanupExtraFields` + `doInsertJsonV2` both quoted below |
| JMX-01 | MBean name includes `connector=<name>` when set | `getMBeanName()` `:71-79` quoted below |
| JMX-02 | Without a name prop, MBean name contains literal `connector=null` | `ClickHouseSinkTask.start()` `:36` quoted below feeds `null` straight through `String.format` |
| JMX-03 | Null version → `version=unknown`; `:` in version → `_` | `getMBeanName()` `:71-79` quoted below |
| JMX-04 | Multiple tasks/connectors register distinct MBeans; `stop()` unregisters only its own | `MBeanServerUtils.java:17-41` quoted below; `ProxySinkTask.stop()` `:81-83` |
| ASYNC-01 | Default config sends no `async_insert` setting | `ClickHouseSinkConfig.java:207-211` quoted below — line commented out |
| ASYNC-02 | User-supplied `async_insert` passes through unchanged | `clickhouseSettings` parsing `:195-206` + insert-settings copy loops `:992-994`/`:1147-1149` quoted below |
| ASYNC-03 | `async_insert=1` sink insert completes, data present | Same settings-copy path; `wait_for_async_insert=1` needed for determinism (D-15) |
</phase_requirements>

## Summary

This phase adds no new dependencies, no new architecture, and (with one pre-approved exception) no
main-code changes — it is purely a test-writing phase pinning behavior that already ships in
production. The research task was therefore to read the exact code paths the 18 requirements
exercise, quote the load-bearing lines verbatim so the planner can write byte-accurate assertions,
and — critically — resolve the one open blocking flag (R-01) empirically rather than by inference.

**R-01 is now resolved by a live run against ClickHouse `latest`** (this session, gradle test
executed against the colima `vz` Docker profile): `insertFormat=json` (metadata-free mode) combined
with a **Struct** (SCHEMA) record and `bypassRowBinary` left at its default `false` produces **silent
data loss** — `put()` returns with no exception, the client's own `InsertResponse` reports
`Written Rows: [0]`, and a subsequent `COUNT(*)` confirms `0` rows landed for a 5-record batch. This
is the worst of the three hypothesized outcomes (silent 0 rows, not an error and not partial data),
which per D-07 means: JSON-01..05 scope stays schemaless-first exactly as D-06 already decided (the
Struct+RowBinary combination is out of scope for this phase's *pin*, since pinning it would mean
writing a test that asserts data loss as correct behavior), and any fail-fast fix for this
combination is an **escalation under D-01**, not an autonomous fix — flag it as a blocking checkpoint
in the plan rather than attempting a fix.

The remaining findings are source-grounded, not just inferred: the `jsonInsertColumns` 5-arg
`client.insert(...)` overload is only reachable from `doInsertJsonV2` (never from
`doInsertStringV2`, which uses the 4-arg overload with no column list) — this is the exact mechanism
behind D-06's "String route never sees `jsonInsertColumns`" and behind why the two existing
explicit-JSON tests (String records) could stay green while `jsonInsertColumns` was completely
untested. `Utils.handleException`'s retriable-error switch statement contains no
column-not-found-family ClickHouse error code, so a `jsonInsertColumns` entry naming a nonexistent
column is guaranteed — by the deterministic shape of that switch, independent of the exact ClickHouse
error code raised — to fall through to the non-retriable `RuntimeException` wrapping path (COLS-05's
classification is pinnable with confidence even without a live run, though the plan should still run
one to nail the exact error code and message substring).

**Primary recommendation:** Write the tests exactly as D-06/D-08/D-09/D-11..17 already specify; treat
R-01's silent-data-loss finding as a blocking-checkpoint note in the 04-01 plan (not a fix task), and
have the 04-02 plan's COLS-05 test capture the live ClickHouse error class/code for the classification
record even though the non-retriable outcome is already inferable from `Utils.java`.

## Architectural Responsibility Map

This is a backend-only Kafka Connect sink connector; there is no browser, frontend-SSR, or CDN tier.
All requirements in this phase live in the connector/backend tier, with the ClickHouse server as the
downstream storage tier that ultimately enforces or violates the connector's assumptions.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Metadata-free JSON insert routing (`insertFormat=json`) | Backend/Connector (`ClickHouseWriter`, `ClickHouseSinkConfig`) | Database/Storage (ClickHouse server parses JSONEachRow / RowBinary) | Routing decision (`doInsert` switch) and table-metadata bypass live entirely in the connector; the server only sees whatever bytes the connector sends |
| `jsonInsertColumns` column-list plumbing | Backend/Connector (`ClickHouseSinkConfig` parses, `ClickHouseWriter.doInsertJsonV2` applies) | Database/Storage (server validates the column list exists and applies `DEFAULT`) | Parsing/passthrough is connector-owned; existence validation and `DEFAULT` application are server-owned (by design — COLS-05's "no pre-validation" is intentional, Out of Scope) |
| MBean naming and registration | Backend/Connector (`ProxySinkTask`, `MBeanServerUtils`) | JVM platform (`ManagementFactory.getPlatformMBeanServer()`) | Pure in-process JVM management concern; no network or storage tier involved |
| `async_insert` settings passthrough | Backend/Connector (`ClickHouseSinkConfig` builds the settings map) | Database/Storage (ClickHouse server interprets `async_insert`/`wait_for_async_insert` semantics) | Connector is a dumb passthrough (one commented-out forced-default line); all actual async-insert behavior is server-side |

## Standard Stack

No new dependencies. This phase reuses the existing JUnit 5 / Mockito / Testcontainers stack
established in Phase 1 and the ClickHouse-backed test pattern (`ClickHouseBase`,
`ClickHouseTestHelpers`) established in Phases 1-3. `build.gradle.kts` requires **no changes** (D-17):
`ProxySinkTask`, `ClickHouseSinkConfig`, `ClickHouseSinkTask`, and `ClickHouseWriter` are all already
present in the `jacocoForkReport` include list —

```
264:    "com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.class",
265:    "com/clickhouse/kafka/connect/sink/db/ClickHouseWriter\$*.class",
266:    "com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.class",
267:    "com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig\$*.class",
268:    "com/clickhouse/kafka/connect/sink/ProxySinkTask.class",
269:    "com/clickhouse/kafka/connect/sink/ProxySinkTask\$*.class",
270:    "com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.class",
271:    "com/clickhouse/kafka/connect/sink/ClickHouseSinkTask\$*.class",
```
`[VERIFIED: build.gradle.kts:264-271]` (read this session).

## Package Legitimacy Audit

**Not applicable.** This phase installs no new external packages. `build.gradle.kts` is unchanged
per D-17.

## Architecture Patterns

### System Architecture Diagram (this phase's slice)

```
                       ClickHouseSinkTask.start(props)
                                  |
                 ClickHouseSinkConfig(props)  <-- parses insertFormat, jsonInsertColumns,
                 |                                  clickhouseSettings (async_insert passthrough)
                 v
            ProxySinkTask(config, errorReporter, connectorName)
                 |
                 |-- MBeanServerUtils.registerMBean(stats, getMBeanName())  [JMX-01..04]
                 |        buildMBeanName(connectorName, id, version)  <-- D-02 extraction target
                 |
                 v
            ClickHouseWriter.start(csc)
                 |
                 |-- shouldSkipTableMetadata() == csc.isJsonInsertFormatExplicit()
                 |        true  -> skip updateMapping() entirely            [JSON-01, JSON-02]
                 |        false -> updateMapping() fetches real table columns
                 v
     ClickHouseSinkTask.put(records) -> ProxySinkTask.put -> Processing.doLogic -> ClickHouseWriter.doInsert
                 |
                 |-- getTable(database, topic)
                 |        metadata-free -> new Table(database, topicToTableMap.getOrDefault(topic, topic))
                 |                          (column-less, hasDefaults=false)          [JSON-02]
                 |        normal        -> real Table from `mapping` cache
                 |
                 |-- switch(first.getSchemaType())
                 |        SCHEMA      -> bypassRowBinary? doInsertJson : doInsertRawBinary  <-- R-01 combination
                 |        SCHEMA_LESS -> doInsertJson (always)                        [JSON-03, JSON-04, D-06]
                 |        STRING_SCHEMA -> doInsertString (jsonInsertColumns NEVER applied here)
                 |
                 v
     doInsertJsonV2: cleanupExtraFields(payload, table) -> gson.toJson -> JSONEachRow bytes
                 |        client.insert(table, jsonInsertColumns, data, JSONEachRow, settings)  [COLS-01..06]
                 v
            ClickHouse server (applies column list, DEFAULTs, input_format_skip_unknown_fields=1,
                                async_insert/wait_for_async_insert if present in settings)  [ASYNC-01..03]
```

### Pattern: Metadata-free short-circuit is a single boolean gate reused in three places

**What:** `shouldSkipTableMetadata()` gates `start()`'s `updateMapping()` call, `updateMapping()`'s
own body, `getTable()`'s branch, and `cleanupExtraFields()`'s branch. All four read the same
`csc.isJsonInsertFormatExplicit()` flag.

```java
// Source: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1268-1270 (read this session)
private boolean shouldSkipTableMetadata() {
    return csc != null && csc.isJsonInsertFormatExplicit();
}
```

**When to use (for test design):** Any JSON-01..04 test that wants to prove metadata-free behavior
must set `insertFormat=json` (case-insensitive) so `jsonInsertFormatExplicit` is `true` — not just an
unset `insertFormat` that happens to default to `InsertFormats.JSON` via the upstream typo (see
Specific Ideas below). The two are different booleans and only one gates metadata-free mode.

```java
// Source: src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:225-240 (read this session)
String insertFormatTmp = props.getOrDefault(INSERT_FORMAT, "node").toLowerCase();
switch (insertFormatTmp) {
    case "none":
        this.insertFormat = InsertFormats.NONE;
        break;
    case "csv":
        this.insertFormat = InsertFormats.CSV;
        break;
    case "tsv":
        this.insertFormat = InsertFormats.TSV;
        break;
    default:
        this.insertFormat = InsertFormats.JSON;
}
String insertFormatRaw = props.get(INSERT_FORMAT);
this.jsonInsertFormatExplicit = insertFormatRaw != null && insertFormatRaw.trim().equalsIgnoreCase("json");
```
This is JSON-01's exact mechanism: `insertFormat=JSON` / `Json` / `  json  ` (after `.trim()`) all set
`jsonInsertFormatExplicit=true` regardless of letter case (the `.toLowerCase()` on `insertFormatTmp`
is irrelevant to this flag — `jsonInsertFormatExplicit` is computed independently from the raw,
trimmed, case-insensitive-compared string). An unset `insertFormat` leaves `insertFormatRaw == null`,
so `jsonInsertFormatExplicit` stays `false` even though `this.insertFormat` silently becomes
`InsertFormats.JSON` via the `default:` branch (the upstream `"node"` typo — confirmed present on
`main`, not a fork addition).

### Pattern: `jsonInsertColumns` parsing (COLS-01)

```java
// Source: src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:241-252 (read this session)
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
Confirms COLS-01 exactly: unset/blank → empty `ArrayList` (never `null`); each comma-split token is
`.trim()`-ed and empty tokens (from `,,` or leading/trailing commas) are dropped via
`!trimmed.isEmpty()`; insertion order is preserved because `ArrayList` + `split(",")` preserve source
order — there is no sorting or dedup anywhere in this block.

### Pattern: the 5-arg vs. 4-arg `client.insert(...)` split (D-06's load-bearing mechanism)

```java
// Source: ClickHouseWriter.java:1031-1032 — doInsertJsonV2 (JSON/SCHEMA_LESS route, jsonInsertColumns APPLIES)
List<String> jsonInsertColumns = csc.getJsonInsertColumns();
try (InsertResponse insertResponse = client.insert(table.getName(), jsonInsertColumns, data, ClickHouseFormat.JSONEachRow, insertSettings).get()) {
```
```java
// Source: ClickHouseWriter.java:1195 — doInsertStringV2 (STRING_SCHEMA route, jsonInsertColumns NEVER applies)
try (InsertResponse insertResponse = client.insert(table.getName(), data, clickHouseFormat, insertSettings).get()) {
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1031-1032,1195]`
(read this session). This is the exact reason the two pre-existing
`ClickHouseSinkTaskJsonInsertFormatTest` tests — which build **String** `SinkRecord`s
(`ClickHouseSinkTaskJsonInsertFormatTest.java:39-49`, `SinkRecord(topic, partition, null, null, null,
json, n, ...)` where `json` is a `String`) — route through `doInsertStringV2` and therefore never
exercised `jsonInsertColumns` even though they set `insertFormat=json`. D-06 (schemaless-first) is the
direct fix for this test gap.

### Pattern: `cleanupExtraFields` runs independently of the column-clause list (COLS-06's surprising interaction)

```java
// Source: ClickHouseWriter.java:1039-1051 (read this session)
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
With `insertFormat=json` (metadata-free, `shouldSkipTableMetadata()==true`), this returns the payload
map **untouched** — JSON-03's exact mechanism for "fields absent from the table are sent unmodified."
With **default config** (`insertFormat` unset, normal table-mapping mode), it keeps any payload key
that matches a real table column, **regardless of whether that key is inside the configured
`jsonInsertColumns` list** — the two mechanisms (JSONEachRow body content vs. the 5-arg `insert(...)`
column clause) are independent, which is exactly D-13's COLS-06 interaction: a valid-column payload
key sitting outside the list still gets serialized into the body, but the column clause excludes it
from the actual write.

### Pattern: `async_insert` is a single commented-out line (ASYNC-01)

```java
// Source: ClickHouseSinkConfig.java:207-211 (read this session)
this.addClickHouseSetting("input_format_skip_unknown_fields", "1", false);
this.addClickHouseSetting("wait_end_of_query", "1", false);
//this.addClickHouseSetting("async_insert", "0", false);
//We set this so our ResponseSummary has actual data in it
this.addClickHouseSetting("send_progress_in_http_headers", "1", false);
```
Upstream forces `async_insert=0`; the fork's entire change is commenting out that one line. Both
insert paths copy every key of `csc.getClickhouseSettings()` into the request's settings —
```java
// Source: ClickHouseWriter.java:992-994 (doInsertJsonV2) — identical shape at :1147-1149 (doInsertStringV2)
for (String clickhouseSetting : csc.getClickhouseSettings().keySet()) {//THIS ASSUMES YOU DON'T ADD insert_deduplication_token
    insertSettings.serverSetting(clickhouseSetting, csc.getClickhouseSettings().get(clickhouseSetting));
}
```
— so ASYNC-01's assertion is purely a `ClickHouseSinkConfig` constructor unit test (no `async_insert`
key present by default), and ASYNC-02's is the same constructor with `clickhouseSettings=async_insert=1`
round-tripping to exactly `"1"` in the map (D-14's rationale, confirmed by reading both call sites).

### Anti-Patterns to Avoid

- **Don't build a Struct/SCHEMA test for JSON-01..05.** R-01 proved this combination silently drops
  data. A test that asserts rows landed would be pinning a bug as a feature; a test that asserts 0
  rows landed with no error would be pinning silent data loss as acceptable, which is exactly the
  scenario D-07 reserves for an escalation, not a routine pin. Stay schemaless per D-06.
- **Don't test `insertFormat` unset as if it triggers metadata-free mode.** The upstream `"node"`
  typo means an unset `insertFormat` still resolves `this.insertFormat` to `InsertFormats.JSON`, but
  `jsonInsertFormatExplicit` (the actual metadata-free gate) stays `false`. JSON-01's negative half
  must assert on behavior gated by `jsonInsertFormatExplicit` (no metadata skip), not on the
  `insertFormat` enum value.
- **Don't assert absolute MBean names or absolute `id` values in JMX-04.** `NEXT_ID` is a
  `static AtomicInteger` that never resets across the JVM fork; only relative claims (distinctness,
  "this one disappeared after `stop()`") are valid (per D-04).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Verifying MBean registration/unregistration | A custom JMX client or reflection harness | `ManagementFactory.getPlatformMBeanServer().queryNames(new ObjectName("com.clickhouse:type=ClickHouseKafkaConnector,*"), null)` (already used by `ClickHouseBase`'s `@AfterEach` sweep) | Same server instance the production code registers against; no fake/mock JMX needed |
| Typed ClickHouse read-back | Hand-parsed JSON string comparison | `chc.getClient().queryRecords(sql).get()` + `GenericRecord.getString(column)`, snapshotted into a `Map<String,String>` per row (the `allRows` helper pattern in `JsonDynamicRoundTripTest.java:573-586`) | `GenericRecord` is a mutable, reused row view (documented Phase 3 finding) — the existing helper already avoids the aliasing bug |
| Deterministic async-insert assertion | A polling loop waiting for the async buffer to flush | `async_insert=1,wait_for_async_insert=1` (D-15) | `wait_for_async_insert=1` makes the ClickHouse HTTP response block until the buffer is flushed, so a `countRows` immediately after `put()` is safe |

**Key insight:** every "don't hand-roll" here is already solved by an existing helper class or a
single ClickHouse setting — there is no new infrastructure to build for this phase.

## Common Pitfalls

### Pitfall 1: R-01's silent data loss looks like a green test if you only check for exceptions

**What goes wrong:** `insertFormat=json` + Struct record + `bypassRowBinary` unset produces **no
exception anywhere in the call chain** — `put()` returns normally, `stop()` succeeds, the log line
reads `Response Summary - Written Bytes: [0], Written Rows: [0]`. A test that only does
`assertDoesNotThrow(() -> chst.put(records))` would pass while silently proving nothing (or worse,
appearing to validate a broken path).

**Why it happens:** the column-less synthetic `Table` (`new Table(database, tableName)`, `hasDefaults`
defaulting to `false`, `rootColumnsList` empty) makes `validateDataSchema` vacuously pass (its `for`
loop over `table.getRootColumnsList()` has nothing to iterate) and makes the RowBinary column-writing
loop in `doInsertRawBinaryV2` (`for (Column col : table.getRootColumnsList())`) write zero bytes per
record, regardless of batch size.

**How to avoid:** Per D-06/D-07, this combination is **not** in scope for a JSON-01..05 pin test in
this phase — schemaless (`Map`) records are used instead, where this failure mode doesn't apply. If a
future phase does want to cover this combination, the test must assert on row count (0) or the raw
`InsertResponse.getWrittenRows()`, not merely on absence of an exception.

**Warning signs:** any new test in this phase's scope that constructs a `Struct`/`SchemaBuilder`-based
`SinkRecord` under `insertFormat=json` without also setting `bypassRowBinary=true` — flag it in review.

### Pitfall 2: `jsonInsertColumns` and `cleanupExtraFields` are independent gates (COLS-06)

**What goes wrong:** it's tempting to assume "a column not in `jsonInsertColumns`" means "a column
not sent to ClickHouse at all." In the **non-explicit** JSON path (default config, real table
mapping), `cleanupExtraFields` only removes keys that aren't real table columns — a real column
sitting outside the `jsonInsertColumns` list still gets serialized into the JSONEachRow body; only
the INSERT statement's column clause (the 5-arg `client.insert(...)` overload's second argument)
excludes it from being written.

**Why it happens:** the two mechanisms were added independently — `cleanupExtraFields` predates
`jsonInsertColumns` and serves a different purpose (stripping genuinely unknown fields before
`input_format_skip_unknown_fields` even needs to fire).

**How to avoid:** COLS-06's fixture (per D-13) must include a payload key that is (a) a real table
column and (b) outside the configured `jsonInsertColumns` list, and assert it does **not** land —
proving the column clause is what excludes it, not the body cleanup.

### Pitfall 3: an all-tombstone batch is untested territory for `doInsertJsonV2` (D-09)

**What goes wrong / inference from source (not live-verified this session):** `doInsertJsonV2`'s
per-record loop (`ClickHouseWriter.java:999-1026`) sends every record whose
`record.getSinkRecord().value() != null` into the JSONEachRow byte stream and otherwise hits the
`else` branch —
```java
// Source: ClickHouseWriter.java:1023-1025 (read this session)
} else {
    LOGGER.warn(String.format("Getting empty record skip the insert topic[%s] offset[%d]", record.getTopic(), record.getSinkRecord().kafkaOffset()));
}
```
For an all-tombstone batch every record hits this branch, so the resulting `stream` is empty (0
bytes) and `client.insert(table.getName(), jsonInsertColumns, data, ClickHouseFormat.JSONEachRow,
insertSettings)` is called with a zero-byte body. Unlike RowBinary, JSONEachRow is a self-delimited,
line-oriented format where an empty body is a well-formed "zero rows" input, so the likely outcome is
a clean 0-row insert with no error — but this is an **inference from the format's semantics, not an
empirical run**, and D-09 explicitly says to pin whatever the live test actually shows.

**How to avoid:** write the live test (04-01) and record the actual outcome; do not assume the
inference above without running it, since D-09 treats a throw as ground truth worth knowing (and
escalating) rather than something to code around in advance.

### Pitfall 4 (superseded): empty `jsonInsertColumns` list behavior

`.planning/research/PITFALLS.md` Pitfall 4 originally flagged the empty-list `insert(table, columns,
...)` overload as the project's highest-risk untested path. **This is now resolved by D-11**: Phase
3's `JsonDynamicRoundTripTest.jsonAndDynamicRoundTripThroughSchemalessRoute` test is green and drives
exactly this code path (`doInsertJsonV2` with the default empty `jsonInsertColumns` list) with correct
column values landing. COLS-02 in this phase is a confirmatory test, not a discovery test — do not
re-open it as a research question.

## Code Examples

### `buildMBeanName` extraction target (D-02) — exact current logic and format string

```java
// Source: src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:71-79 (read this session)
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
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:71-79]` (read this
session). The extraction (D-02) must preserve this exact format string and null/colon logic in a new
`static String buildMBeanName(String connectorName, int id, String version)`, with `getMBeanName()`
reduced to:
```java
private String getMBeanName() {
    return buildMBeanName(connectorName, id, ClickHouseClientOption.class.getPackage().getImplementationVersion());
}
```
(This exact replacement text is Claude's discretion per D-02/CONTEXT — the constraint is byte-for-byte
identical *output*, not a specific refactored form.)

`connectorName` is `final` and set from the constructor parameter (`ProxySinkTask.java:36,43`), which
`ClickHouseSinkTask.start()` populates with a plain `props.get("name")` —
```java
// Source: src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java:36 (read this session)
connectorName = props.get("name");
```
— so an absent `name` prop makes `connectorName == null`, and `String.format("...connector=%s...",
(Object) null, ...)` renders the literal text `connector=null` (Java's `%s` conversion of a `null`
argument is the string `"null"`), which is JMX-02's exact pinned behavior.

`NEXT_ID`/`id`:
```java
// Source: ProxySinkTask.java:31,40 (read this session)
private static final AtomicInteger NEXT_ID = new AtomicInteger();
...
private int id = NEXT_ID.getAndAdd(1);
```
Confirms JMX-04's constraint: `id` only ever increments across the JVM fork's lifetime, never resets
per test, so absolute values (`SinkTask0`) are unassertable — only relative distinctness claims hold.

### `MBeanServerUtils` (JMX-04's actual registration surface)

```java
// Source: src/main/java/com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.java:17-41 (read this session)
public static <T> T registerMBean(final T mBean, final String mBeanName) {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
        server.registerMBean(mBean, new ObjectName(mBeanName));
        return mBean;
    } catch (InstanceAlreadyExistsException e) {
        throw new RuntimeException(e);
    } catch (Exception e) {
        // JMX might not be available
        LOGGER.warn("Unable to register MBean " + mBeanName, e);
        return mBean;
    }
}
public static void unregisterMBean(final String mBeanName) {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
        ObjectName objectName = new ObjectName(mBeanName);
        if (server.isRegistered(objectName)) {
            server.unregisterMBean(objectName);
        }
    } catch (Exception e) {
        // JMX might not be available
        LOGGER.warn("Unable to unregister MBean " + mBeanName, e);
    }
}
```
Note `unregisterMBean` is idempotent (`isRegistered` guard) and swallows all exceptions with a WARN —
this is why JMX-04's "stop() unregisters only its own" assertion must query the platform MBean server
directly for presence/absence, not rely on any exception being thrown from `stop()` either way.

### `Utils.handleException`'s retriable-error switch (COLS-05's classification mechanism)

```java
// Source: src/main/java/com/clickhouse/kafka/connect/util/Utils.java:63-99 (read this session)
public static void handleException(Exception e, boolean errorsTolerance, Collection<SinkRecord> records) {
    ...
    Exception rootCause = Utils.getRootCause(e, true);
    if (rootCause instanceof ClickHouseException) {
        ClickHouseException clickHouseException = (ClickHouseException) rootCause;
        switch (clickHouseException.getErrorCode()) {
            case 3: // UNEXPECTED_END_OF_FILE
            case 107: // FILE_DOESNT_EXIST
            case 159: // TIMEOUT_EXCEEDED
            case 164: // READONLY
            case 202: // TOO_MANY_SIMULTANEOUS_QUERIES
            case 203: // NO_FREE_CONNECTION
            case 209: // SOCKET_TIMEOUT
            case 210: // NETWORK_ERROR
            case 241: // MEMORY_LIMIT_EXCEEDED
            case 242: // TABLE_IS_READ_ONLY
            case 252: // TOO_MANY_PARTS
            case 285: // TOO_FEW_LIVE_REPLICAS
            case 319: // UNKNOWN_STATUS_OF_INSERT
            case 425: // SYSTEM_ERROR
            case 999: // KEEPER_EXCEPTION
                throw new RetriableException(e);
            default:
                LOGGER.error("Error code [{}] wasn't in the acceptable list.", clickHouseException.getErrorCode());
                break;
        }
    }
    ...
    if (errorsTolerance) {
        LOGGER.warn(...);
    } else {
        LOGGER.error(...);
        if (records != null) {
            throw new RuntimeException(String.format("Number of records: %d", records.size()), e);
        } else {
            throw new RuntimeException("Records was null", e);
        }
    }
}
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/util/Utils.java:63-99]` (read this session).
This switch's case list is a fixed, exhaustive enumeration — no ClickHouse "unknown column" /
"no such column in table" family error code (e.g. `NO_SUCH_COLUMN_IN_TABLE`, `UNKNOWN_IDENTIFIER`)
appears in it. Therefore, **by the deterministic shape of this code**, a `jsonInsertColumns` entry
naming a nonexistent column falls through to `default:` (logged, not retried) and — with the
connector's default `errors.tolerance=none` — always reaches the final `else` branch, throwing a
task-failing `RuntimeException` (not `RetriableException`). This classification is derivable from
source alone with HIGH confidence; the live COLS-05 test should still run to confirm the exact
ClickHouse error code and to get a message substring for the assertion, per D-17's negative-test
convention.

### R-01 empirical result (this session's live probe — not a committed test)

A throwaway JUnit test (`ClickHouseSinkTask.start()` with `insertFormat=json`, a 5-record batch of
`SinkRecord`s carrying a `Struct` value against `off16 Int16, str String`, `bypassRowBinary` left
unset) was run via `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests
"com.clickhouse.kafka.connect.sink.ZZProbeR01Test"` against `clickhouse/clickhouse-server:latest`
(server version `26.8.6.5`, confirmed in the run's own log line) on the colima `vz` Docker profile,
then deleted (never committed — `git status` confirms no residue). Relevant log lines from that run:

```
[...] INFO com.clickhouse.kafka.connect.sink.db.ClickHouseWriter Skipping table mapping and validation because insertFormat is explicitly set to JSON.
[...] DEBUG com.clickhouse.kafka.connect.sink.db.ClickHouseWriter Trying to insert [5] records with schema type [SCHEMA] to table name [`r01_probe_...`] (QueryId: [...])
[...] DEBUG com.clickhouse.kafka.connect.sink.db.ClickHouseWriter Response Summary - Written Bytes: [0], Written Rows: [0] - (QueryId: [...])
[...] INFO com.clickhouse.kafka.connect.sink.ClickHouseSinkTask Put records: 5 in 112 ms
=== R-01 PROBE RESULT ===
Exception thrown by put(): NONE
Row count after insert: 0
=== END R-01 PROBE RESULT ===
```
`[VERIFIED: live gradle run, this session, ClickHouse latest / 26.8.6.5, client V2]`. **Finding:** no
exception anywhere in the call chain; the client's own insert response reports 0 written rows; a
follow-up `COUNT(*)` independently confirms 0 rows landed. This is silent data loss for the
`insertFormat=json` + Struct + `bypassRowBinary` unset combination — see D-07/Pitfall 1 for the
scope implication.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | An all-tombstone batch through `doInsertJsonV2` produces a clean 0-row insert with no error, based on JSONEachRow's empty-body-is-valid semantics — not empirically verified this session | Common Pitfalls, Pitfall 3 | If ClickHouse or client-v2 actually rejects a truly empty JSONEachRow insert body, the 04-01 plan's JSON-04 all-tombstone test would need to pin a thrown exception instead of a 0-row success; D-09 already anticipates this ("if it throws, that is ground truth ... escalates under D-01") so the risk is contained to test-design surprise, not a planning gap |
| A2 | The exact ClickHouse server error code for a `jsonInsertColumns` entry naming a nonexistent column was not captured live this session (COLS-05's classification is derived from `Utils.handleException`'s switch statement being exhaustive and code-complete, not from running the scenario) | Code Examples, `Utils.handleException` section | If the actual error code happens to be one of the 15 listed retriable codes (unlikely — none are column/identifier-related, but not verified against this specific ClickHouse version's exact error taxonomy), COLS-05 would need to pin a `RetriableException` instead, which is explicitly flagged in D-10 as requiring escalation rather than an autonomous test-only fix |

## Open Questions

1. **Exact ClickHouse error code/message for `jsonInsertColumns` naming a nonexistent column**
   - What we know: `Utils.handleException`'s switch has no column/identifier-related retriable code, so classification is non-retriable with HIGH confidence from source alone.
   - What's unclear: the literal error code and message text ClickHouse `latest` client-v2 actually raises for this scenario (likely `NO_SUCH_COLUMN_IN_TABLE` code 16, or `UNKNOWN_IDENTIFIER` code 47, based on general ClickHouse error taxonomy knowledge — `[ASSUMED]`, not verified against `latest` this session).
   - Recommendation: 04-02's COLS-05 live test naturally captures this — no separate research action needed, since the test itself is the verification instrument (per D-10, "records which exception type the connector surfaces").

2. **All-tombstone batch outcome for `doInsertJsonV2`**
   - What we know: source-level trace shows the byte stream will be empty for an all-tombstone batch.
   - What's unclear: whether ClickHouse's JSONEachRow parser (or client-v2's `insert(...)` call) treats a genuinely zero-byte body as "0 rows, success" vs. some other behavior (e.g. a format-detection error).
   - Recommendation: covered directly by 04-01's JSON-04 all-tombstone test per D-09; no research blocker.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker (via colima) | All ClickHouse-backed tests (04-02, 04-03 live classes; JSON-01..05 live tests) | ✓ | Docker Engine 27.4.0 inside colima `vz` (confirmed this session via `docker info`) | — |
| colima `vz` profile | Testcontainers' Ryuk reaper and the ClickHouse container | ✓ Running (confirmed this session: `colima list` shows `vz` Running; `default` profile Stopped) | colima context `colima-vz` | — |
| ClickHouse `latest` Docker image | All live tests in this phase | ✓ Pulled and started successfully this session | Resolved to server version `26.8.6.5` at run time | — |
| `TESTCONTAINERS_HOST_OVERRIDE` env var | Must be unset for Testcontainers/Ryuk to work | ✓ handled — must invoke gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` (STATE.md, confirmed working this session) | — | — |

**Missing dependencies with no fallback:** none — full live-test environment confirmed working this
session (the R-01 probe ran end-to-end against a real ClickHouse `latest` container).

## Security Domain

`security_enforcement` is on (ASVS level 1, default) but this phase adds no new attack surface: it
writes tests against existing, already-shipped connector code, with one pre-approved pure-refactor
extraction (`buildMBeanName`) that changes no logic. No new user-facing input path, no new
authentication/session/crypto code, and no schema change is introduced.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-------------------|
| V2 Authentication | No | No auth code touched this phase |
| V3 Session Management | No | Not applicable to a Kafka Connect sink task |
| V4 Access Control | No | No access-control code touched |
| V5 Input Validation | Informational only | `jsonInsertColumns` column names are passed unvalidated into `client.insert(table, columns, ...)` (existing, pinned, Out of Scope per PROJECT.md — "Validating `jsonInsertColumns` against the table schema ... locked Out of Scope"). This is operator-configured connector config, not end-user request input, so the standard Kafka Connect trust boundary (connector config is trusted, set by the operator deploying the connector) applies — no new mitigation is in scope for this phase. |
| V6 Cryptography | No | No crypto code touched |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Malformed connector `name` prop containing MBean-ObjectName-hostile characters (`,`, `=`) causing `MalformedObjectNameException` to be silently swallowed, publishing no JMX metrics | Denial of Service (observability blind spot, not data loss) | Already identified and deliberately deferred (CONTEXT.md Deferred Ideas, D-05) — out of scope for this phase; connector names are operator-configured, not attacker-controlled in a typical deployment |
| A `jsonInsertColumns` entry naming a nonexistent column causing an unhandled or infinitely-retried failure | Denial of Service (if misclassified retriable) | COLS-05/D-10 pins the current (non-retriable) classification; the fork's `Utils.handleException` switch statement is the control point, unchanged this phase |

## Sources

### Primary (HIGH confidence — read directly this session)

- `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/sink/data/convert/EmptyRecordConvertor.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/util/Utils.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` (partial, lines 1-60)
- `src/main/java/com/clickhouse/kafka/connect/sink/data/Record.java` (full file)
- `src/main/java/com/clickhouse/kafka/connect/ClickHouseSinkConnector.java` (full file)
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java` (full file)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java` (full file)
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` (full file)
- `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` (full file)
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonDynamicRoundTripTest.java` (full file)
- `build.gradle.kts` (grep for `jacocoForkReport` include list, lines 264-271)
- `.planning/phases/04-json-insert-path-connector-plumbing/04-CONTEXT.md`, `.planning/REQUIREMENTS.md`, `.planning/STATE.md`, `.planning/config.json`
- A live gradle test run (this session) against `clickhouse/clickhouse-server:latest` (server `26.8.6.5`) on the colima `vz` Docker profile, establishing R-01's ground truth; the throwaway test file was deleted after capturing the result and confirmed absent from `git status`

### Secondary (MEDIUM confidence)

- `.planning/research/PITFALLS.md` Pitfall 4 and Pitfall 5 (superseded/confirmed respectively by this session's direct source reading and Phase 3's green test evidence)

### Tertiary (LOW confidence / `[ASSUMED]`)

- The specific ClickHouse error code (`NO_SUCH_COLUMN_IN_TABLE` / `UNKNOWN_IDENTIFIER`) for a
  nonexistent `jsonInsertColumns` entry — not run live this session; see Open Questions #1 and
  Assumptions Log A2.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new dependencies, all touched classes already covered by existing JaCoCo config (verified by reading `build.gradle.kts`)
- Architecture: HIGH — every code path cited was read directly this session with exact line numbers and verbatim quotes
- R-01 (the phase's one blocking research flag): HIGH — resolved by an actual live gradle run against ClickHouse `latest`, not inference
- Pitfalls: HIGH for JSON-01..03/COLS-01..06/JMX-01..04/ASYNC-01..02 (all source-grounded); MEDIUM for the all-tombstone (JSON-04) and COLS-05 exact-error-code predictions, which are logically derived but not empirically run this session (left as Open Questions, both already covered by the corresponding plan's own live test)

**Research date:** 2026-09-17
**Valid until:** Effectively pinned to this exact commit of `custom_build` (`v1.2.9_gg4`) — re-verify
line numbers if any of the five touched files change before this phase is planned/executed.
