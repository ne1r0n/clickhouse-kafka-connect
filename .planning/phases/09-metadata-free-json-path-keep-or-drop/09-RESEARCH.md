# Phase 9: Metadata-free JSON Path — Keep or Drop - Research

**Researched:** 2026-09-20
**Domain:** Java (Kafka Connect sink connector), ClickHouse client-v2 insert API, one fork-specific bypass re-applied onto an upstream-rewritten write path
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**The call is made: KEEP.** The metadata-free `insertFormat=json` bypass is re-applied (D-01), overriding Phase 7 D-10's inherited "dropped" default. This is a deliberate departure from the milestone's "prefer upstream" conflict policy, on the same footing as the JMX carve-out.

- **D-01: KEEP the metadata-free `insertFormat=json` bypass.** Operator fact 3 forces it: of the three things the bypass buys, the missing `DESCRIBE`/`SHOW` grant is the one no combination of upstream configuration reaches. Known cost, accepted: KEEP re-introduces R-01's silent data loss on the `Struct` route; JSONP-02 requires the loss stay pinned. Reversibility: costly — undoing it later means a production migration.
- **D-02: The decision is recorded here, not re-litigated at execution.** A planner may still place a cheap `checkpoint:decision` before the first code task — but it re-confirms D-01, it does not re-open it.
- **D-03: Re-apply as minimal early-return guards, not restored method bodies.** Four sites in `ClickHouseWriter.java`: `start()` (after ping/version check, before `updateMapping`), `updateMapping(String)` (return early — also kills background DESCRIBE traffic via `TableMappingRefresher`), `getTable(String, String)` (return the synthetic table resolved from `csc.getTopicToTableMap().getOrDefault(topic, topic)`), `cleanupExtraFields(Map, Table)` (`return m` untouched, OR'd with the existing `csc.isBypassFieldCleanup()` check). Every upstream line below each guard stays untouched. Reversibility: reversible.
- **D-04: The synthetic `Table` is `new Table(database, tableName, false, List.of(), 0)`.** Reproduces the pre-merge 2-arg constructor's semantics against upstream's immutable 5-arg shape. `hasDefaults=false` matters — `doInsertBatch:222` passes `table.hasDefaults()` into `doInsertRawBinary`. The empty `rootColumnsList` is R-01's mechanism, reproduced knowingly.
- **D-05 (Claude's discretion):** A single private `shouldSkipTableMetadata()` predicate (`csc != null && csc.isJsonInsertFormatExplicit()`) rather than four inlined calls — user constrained the diff *shape* (minimal early-returns), not whether the condition is named. A planner may inline with a stated reason.
- **D-06: Keep the column list server-side.** In `doInsertJsonV2`, call `client.insert(table.getName(), columns, dataWriter, JSONEachRow, insertSettings)` — the overload `javap` confirms exists. Rejected: a client-side field filter, because it silently swallows COLS-05's contract and is a no-op under D-01's bypass. Reversibility: reversible.
- **D-07: JSONP-03's rationale is corrected, not its goal.** True of the call site, false of the API. Append a dated correction note to `REQUIREMENTS.md` § JSONP-03 citing the `javap` output; do not rewrite the requirement text.
- **D-08 (planner constraint): branch on non-empty unless COLS-02 re-proves otherwise.** Call the 4-arg overload when `jsonInsertColumns` is empty and the 5-arg one when it is not, unless a live COLS-02 run proves unconditional is safe on the `DataStreamWriter` overload — a mis-rendered `INSERT INTO t ()` would break every default-config JSON insert in production.
- **D-09 (Claude's discretion): `doInsertJsonV1` stays untouched.** Pre-merge fork never wired `jsonInsertColumns` into V1; milestone tests client V2 only.
- **D-10: Re-enable all four `@Disabled` classes unchanged — delete the `@Disabled` line and nothing else.** Phase 7's compile fixes stay. R-01's `PIN - NOT AN ENDORSEMENT` Javadoc stays byte-identical.
- **D-11: Add two Docker-free regression pins to `MetadataFreeJsonInsertUnitTest`** — `getTable()` returns a synthetic `Table` resolved from `topic2TableMap` without touching the helper client, and `updateMapping()` short-circuits. `start()` stays live-only (it pings a server before reaching its guard). Makes the bypass a sixth entry in the must-not-silently-lose list, with a `testUnit`-visible canary.
- **D-12: `testUnit` green PLUS a full `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test`, zero-failure pass condition.** All 13 newly-enabled live test methods green, zero failures total (not re-baselined), every skip explained.
- **D-13 (Claude's discretion): six-commit sequence** — (1) four bypass guards + synthetic `Table`; (2) `jsonInsertColumns` server-side re-attachment; (3) four `@Disabled` deletions; (4) D-11's two Docker-free pins; (5) `TableTest` assertion fix; (6) checklist flips and Recorded Facts. `spotlessApply` scoped to only the files each commit touches.
- **D-14: Adopt the `TableTest.extractNullables()` carry-in and fix it here.** Change `3` → `2` at `TableTest.java:51` with a comment citing `DESC-01`/`HLP-02`. Test-only change outside `JSONP-01..04`, taken at the user's explicit decision. Flip Recorded Fact 13's disposition to resolved-in-Phase-9 with the commit SHA.

### Claude's Discretion

The user did not constrain the following. All reversible; a planner may override with a stated reason.

- **D-05** (named predicate vs. inlined condition), **D-09** (V1 untouched), **D-13** (commit sequence).
- **Wave shape.** The four guards are one connected edit and do not decompose. `jsonInsertColumns` (D-06) touches only `doInsertJsonV2` and is independent of the guards — could run in a parallel wave. The `@Disabled` deletions depend on both. D-12's live run is necessarily last.
- **JSONP-04's evidence act.** Discharged by reading `git diff v1.6.0 -- src/main/java/.../ClickHouseWriter.java` after re-application and stating in the SUMMARY that (a) the only hand-built `Table` uses the 5-arg constructor, (b) no JSON-path code assumes the old mutable shape, (c) the guards are the only fork additions on that path. A grep is the evidence; a green test is not sufficient, per the requirement's own wording.
- **No new metric.** This phase re-routes an existing insert path, it does not add one.

### Deferred Ideas (OUT OF SCOPE)

- **R-01 fail-fast — reschedule on new evidence.** Still out of scope (JSONP-02 requires the loss stay pinned under KEEP). Operator fact 2 says no live pipeline uses the `Struct` route, so the migration story Phase 4 rated prohibitive may now be empty. Worth raising at milestone close with this fact attached.
- **P4-WR-01** — `ClickHouseSinkTaskJsonInsertFormatTest`'s missing `try/finally` task-lifecycle pattern. D-10 re-enables the class unchanged, so the defect returns with it. Phase 10's HARN-02 sweep restoration mitigates it.
- **CR-01** — `.planning/scripts/verify-merge-invariants.sh:44` fails open on a `git grep` error. Carried from Phase 6, still unowned, must be fixed before Phase 11. Not Phase 9 work.
- **Naming `insertFormat` for what it does.** Its only effect in this fork is "skip table metadata"; `bypassRowBinary` selects the format. An operator-visible config change needing its own decision.
- **P3-WR-01** — `shouldSkipDescribeRow` walking more than one level up for deeply nested `JSON`/`Dynamic` children.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| JSONP-01 | The keep-or-drop decision for the metadata-free `insertFormat=json` path is made at a blocking-human checkpoint, on evidence from the merged code, and recorded with its rationale | Already discharged by CONTEXT.md D-01/D-02 (the checkpoint already happened in `/gsd-discuss-phase`). This research independently re-verified every piece of merged-code evidence the checkpoint relied on (guard sites, `Table` constructor, overload existence) and found it accurate — nothing here reopens the call |
| JSONP-02 | `JSON-01..05` and R-01's labelled test reflect the KEEP decision — the bypass works and R-01's loss is still pinned | Pattern 1 (guard re-application) + Pattern 3 (R-01's mechanism traced line-by-line) give the exact code shape needed so the bypass genuinely works and R-01 still reproduces; D-10/D-11 give the exact test-file mechanics |
| JSONP-03 | `jsonInsertColumns` works again, with `COLS-01..06` green | Pattern 4 gives the `javap`-verified overload signature and the exact call-site edit (D-06/D-08); Don't Hand-Roll explains why a client-side filter was rejected |
| JSONP-04 | No fork-only JSON-path code survives against a `Table` shape upstream no longer produces — verified by reading the resolved diff | Pattern 2 gives the exact "only two `new Table(` call sites exist today, both legitimate" baseline the post-change diff read should compare against |
</phase_requirements>

## Summary

The keep-or-drop call itself is **already made** (CONTEXT.md D-01: KEEP). This is not a "which library" research task — it is a "read the merged code precisely enough that four guard insertions and one overload swap compile against upstream's rewritten `ClickHouseWriter`/`Table` on the first try" task. Every site CONTEXT.md names was opened this session with `Read` and every quoted claim below was checked against the actual line, not against CONTEXT.md's restatement of it. All of CONTEXT.md's measured-starting-state claims hold: the bypass is genuinely absent from `src/main`, the four re-application sites are exactly where D-03 says, `Table`'s constructor is exactly the 5-arg shape D-04 assumes, and — the one claim genuinely worth independently re-verifying because a locked requirement (JSONP-03) asserted the opposite — `javap` on the actual `client-v2-0.9.6.jar` in this machine's Gradle cache confirms the `insert(String, List<String>, DataStreamWriter, ClickHouseFormat, InsertSettings)` overload D-06 depends on is really there.

Two things this session adds beyond CONTEXT.md's own evidence table: first, a precise trace of *why* R-01 fires only on the RowBinary route and never on the JSON route — `writeRecordsRowBinary`'s per-column loop (`ClickHouseWriter.java:1182`) is the sole consumer of `table.getRootColumnsList()` that isn't already guarded, and it silently emits zero bytes per record when that list is empty, while `writeRecordsJson` never reads column metadata at all (it serializes whatever the payload map contains, and only `cleanupExtraFields` — which D-03 explicitly guards — touches `getRootColumnsList()`). Second, a latent inconsistency in `MERGE-CHECKLIST.md` row 26 worth a one-line note in this phase's own commits: that row is marked `done` and lists "the synthetic `Table` constructor call backing the metadata-free JSON path" among the fork behaviours it covers, but its `adjudicating test` column (`WRT-01, WRT-02, WRT-03, WRT-05`) does not include anything from the JSON path — the metadata-free bypass was never actually re-applied under that row's "done" status. Left as-is, a future reader of that checklist could reasonably believe the bypass was already handled in Phase 7.

**Primary recommendation:** Follow CONTEXT.md's D-03/D-04/D-05/D-06/D-08/D-09/D-10/D-11/D-13/D-14 exactly as specified — every mechanical detail those decisions assume (line numbers, constructor shape, getter names, overload existence, call-site independence) is verified correct against the actual merged tree in this session. The only genuinely open technical question this research did not (and could not) resolve is D-08's branch condition — whether the 5-arg `client.insert(..., List<String> columns, ...)` overload renders `INSERT INTO t ()` for an empty list the same way the old `InputStream` overload does; this needs a live probe during Task execution, not a `javap` read.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Skip table metadata discovery (`start`/`updateMapping`/`getTable` guards) | API / Backend (connector) | Database / Storage (what it avoids calling) | The guards are pure connector-side control flow — they decide whether to call ClickHouse's `DESCRIBE`/`SHOW`, they never reach the network themselves when engaged |
| Synthetic column-less `Table` construction | API / Backend | — | In-memory value object, no I/O |
| `cleanupExtraFields` bypass | API / Backend | — | Field-filtering logic inside the connector before serialization |
| `jsonInsertColumns` server-side re-attachment | API / Backend → Database / Storage | — | The column list crosses the wire as part of the `INSERT ... (col, col) FORMAT JSONEachRow` statement; the connector builds the list, ClickHouse enforces it |
| R-01's loss mechanism (empty-column RowBinary write) | API / Backend | Database / Storage | The zero-byte write happens entirely in-process (`writeRecordsRowBinary`) before any bytes reach ClickHouse; ClickHouse's `Written Rows: 0` response is just confirming what the connector already produced |
| Test re-enablement / TableTest fix | Test / CI tier | — | No production code path; assertions only |

This phase touches exactly one file's production logic (`ClickHouseWriter.java`) plus test-file annotations — there is no browser, SSR, or CDN tier in this connector, so most rows collapse to "API/Backend" by construction. The map is included for completeness per the research contract, not because there was a live misassignment risk to catch.

## Standard Stack

No new external dependency is introduced by this phase. Every class, method, and overload this phase touches already exists in the merged tree or in the already-declared `com.clickhouse:client-v2:0.9.6` artifact (`gradle/libs.versions.toml`, unchanged since Phase 6). There is nothing to install.

### Core (existing, unchanged versions — confirmed via the already-resolved Gradle cache, not re-pinned by this phase)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.clickhouse:client-v2` | `0.9.6` | ClickHouse client-v2 API (`Client.insert(...)` overloads) | Fork-only pin (`REQUIREMENTS.md` locked decisions), kept over upstream's catalog version by explicit milestone decision |
| Lombok | (upstream-reconciled, unchanged this phase) | `@Getter` on `ClickHouseSinkConfig`/`Table`/`Column` — generates `isJsonInsertFormatExplicit()`/`getJsonInsertColumns()`/`getRootColumnsList()` this phase's guards call | Already in use throughout the class; D-05's predicate and D-11's pins both depend on the existing `@Getter` surface, not new annotations |
| JUnit 5 (Jupiter) | (upstream-reconciled) | The four `@Disabled` classes and `JsonInsertColumnsUnitTest` | Existing test framework, unchanged |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Server-side `client.insert(table, columns, dataWriter, format, settings)` overload (D-06) | A client-side field filter inside `writeRecordsJson` (what `PITFALLS.md` Pitfall 4 and JSONP-03's original wording both assumed was necessary) | Rejected by CONTEXT.md D-06/D-07 on the `javap` finding below — the overload never disappeared, only the call site's *choice* of overload changed. A client-side filter would silently downgrade COLS-05's loud non-retriable throw to a no-op, and composes badly with D-01's bypass since `cleanupExtraFields` is a no-op exactly when the bypass is engaged |

**Installation:** None required.

**Version verification:** `com.clickhouse:client-v2:0.9.6` confirmed present in the local Gradle module cache at `~/.gradle/caches/modules-2/files-2.1/com.clickhouse/client-v2/0.9.6/.../client-v2-0.9.6.jar` `[VERIFIED: local Gradle cache inspection, this session]` — this is the exact artifact the build already resolves; no version bump needed or proposed.

## Package Legitimacy Audit

**Not applicable.** This phase installs no new external packages — it only edits existing methods in `ClickHouseWriter.java` and flips `@Disabled` annotations on existing test files. No `Package Legitimacy Audit` table is required.

## Architecture Patterns

### System Architecture Diagram

```
Kafka record (SCHEMA_LESS Map, or SCHEMA Struct)
        |
        v
 ProxySinkTask.put() -> Processing.doLogic() -> ClickHouseWriter.doInsert()
        |
        v
 ClickHouseWriter.getTable(db, topic)  <-- GUARD SITE 3 (D-03)
        |                                    if shouldSkipTableMetadata(): return synthetic
        |                                    Table(db, name, false, List.of(), 0) resolved from
        |                                    csc.getTopicToTableMap().getOrDefault(topic, topic)
        |                                    else: normal mapping lookup -> updateMapping -> describeTable
        v
 doInsertBatch() switches on SchemaType:
   SCHEMA / DEBEZIUM_CDC + !bypassRowBinary -> doInsertRawBinary()
        |                                        writeRecordsRowBinary() loops
        |                                        table.getRootColumnsList() <-- EMPTY under
        |                                        the bypass = R-01 (zero bytes/record, no throw)
        |
   SCHEMA / DEBEZIUM_CDC + bypassRowBinary, or SCHEMA_LESS -> doInsertJson()
        |                                        writeRecordsJson() serializes the payload
        |                                        map directly, then calls
        |                                        cleanupExtraFields(data, table) <-- GUARD SITE 4 (D-03)
        |                                        if shouldSkipTableMetadata() (OR bypassFieldCleanup):
        |                                        return payload unchanged; else strip unknown keys
        |                                        using table.getRootColumnsList()
        v
 doInsertJsonV2(): client.insert(table.getName(), [jsonInsertColumns if non-empty,]
                   dataWriter, JSONEachRow, insertSettings)  <-- OVERLOAD SWAP SITE (D-06/D-08)
        |
        v
 ClickHouse server: INSERT INTO <table> [(col, col, ...)] FORMAT JSONEachRow
```

`start()` (GUARD SITE 1) and `updateMapping()` (GUARD SITE 2, also entered from the background `TableMappingRefresher` and from `getTable()`'s own fallback) sit upstream of this diagram — engaging them prevents `mapping` from ever being populated for a metadata-free connector, which is what makes `getTable()`'s guard the one that actually has to build the synthetic `Table` (nothing is ever cached there to find).

### Recommended Project Structure

No new files or directories. All work lands in:
```
src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java   # 4 guards + overload swap
src/test/java/com/clickhouse/kafka/connect/sink/...                        # @Disabled deletions + 2 new pins
src/test/java/.../db/mapping/TableTest.java                                # 1-line assertion fix (D-14)
.planning/REQUIREMENTS.md, .planning/MERGE-CHECKLIST.md                    # annotate-don't-flip bookkeeping
```

### Pattern 1: Minimal early-return guard, not a restored method body (D-03)

**What:** A single-line (or few-line) early return at the top of each of the four sites, keyed on one predicate, with every upstream line below the guard left untouched.
**When to use:** Exactly the four sites CONTEXT.md names — do not extend this pattern to any other method in `ClickHouseWriter.java` (see Anti-Patterns).
**Verified current code at each site (this session, `Read` + line numbers, quoted verbatim):**

`start()`, `ClickHouseWriter.java:124-125` — the guard's insertion point:
```java
        LOGGER.debug("Ping was successful.");
        this.updateMapping(csc.getDatabase());
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:124-125]` The guard goes between these two lines, before `updateMapping` is ever called, per D-03's "after the ping and version check, before `updateMapping`".

`updateMapping()`, `ClickHouseWriter.java:142-149`:
```java
    public boolean updateMapping(String database) {
        // Do not start a new update cycle if one is already in progress
        // Atomically compare and set isUpdateMappingRunning
        if (!this.isUpdateMappingRunning.compareAndSet(false, true)) {
            return false; // in progress
        }

        LOGGER.debug("Update table mapping.");
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:142-149]` D-03's guard returns early here — before the `compareAndSet`, which stays untouched below it. Confirmed separately: `TableMappingRefresher.run()` calls exactly `chWriter.updateMapping(database)` `[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/TableMappingRefresher.java, full file read this session]`, so guarding this one method really does kill the background DESCRIBE traffic as D-03 claims — no separate guard is needed on `startBackgroundTableSync` (`ClickHouseWriter.java:1623-1642`, confirmed gated only on `csc.getTableRefreshInterval() > 0`, unrelated to the bypass).

`getTable()`, `ClickHouseWriter.java:1592-1615`, quoted in full:
```java
    protected Table getTable(String database, String topic) {
        String tableName = Utils.getTableName(database, topic, csc.getTopicToTableMap());
        Table table = this.mapping.get(tableName);
        if (table == null) {
            if (this.updateMapping(database)) {
                table = this.mapping.get(tableName);//If null, update then do it again to be sure
            } else {
                String cleanTopicName = Utils.getMappedOrTopicTableName(topic, csc.getTopicToTableMap());
                table = chc.describeTable(database, cleanTopicName.replace("`", "").trim());
            }
        }

        if (table == null) {
            if (csc.isSuppressTableExistenceException()) {
                LOGGER.warn("Table [{}] does not exist, but error was suppressed.", tableName);
            } else {
                //TODO to pick the correct exception here
                LOGGER.error("Table [{}] does not exist - see docs for more details about table names and topic names.", tableName);
                throw new RuntimeException(String.format("Table %s does not exist", tableName));
            }
        }

        return table;//It'll only be null if we suppressed the error
    }
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1592-1615]` This matches CONTEXT.md's ":1596-1601 describeTable fallback" claim precisely (lines 1599-1600 here, off by CONTEXT.md's own line-number drift note — content is identical either way). D-03's guard returns the synthetic `Table` before line 1593's `mapping.get` lookup runs. `Utils.getMappedOrTopicTableName`, used both here and in the fallback, is confirmed to implement exactly `topicToTableMap.getOrDefault(topicName, topicName)` semantics: `[VERIFIED: src/main/java/com/clickhouse/kafka/connect/util/Utils.java:187-194]`
```java
    public static String getMappedOrTopicTableName(String topicName, Map<String, String> topicToTableMap) {
        String tableName = topicToTableMap.get(topicName);
        LOGGER.debug("Topic name: {}, Table Name: {}", topicName, tableName);
        if (tableName == null) {
            tableName = topicName;
        }
        return tableName;
    }
```
— confirming D-04's stated resolution mechanism (`csc.getTopicToTableMap().getOrDefault(topic, topic)`) is exactly what the existing helper already does; the guard should call this helper (or its already-escaped caller `Utils.getTableName`), not re-derive the lookup.

`cleanupExtraFields()`, `ClickHouseWriter.java:1387-1399`, quoted in full:
```java
    protected Map<String, Object> cleanupExtraFields(Map<String, Object> m, Table t) {
        if (csc.isBypassFieldCleanup()) {
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
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1387-1399]` Confirms D-03's "OR'd with the existing `csc.isBypassFieldCleanup()` check" is a real, already-present single-condition `if` — the guard becomes `if (csc.isBypassFieldCleanup() || shouldSkipTableMetadata()) { return m; }`. **This exact contract is independently pinned by `MetadataFreeJsonInsertUnitTest.cleanupExtraFieldsBypassDiffersByInsertFormat()`** (`[VERIFIED: src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java:93-118]`), which asserts that under `insertFormat=json`, `writer.cleanupExtraFields(payload, table)` returns the payload with **both** `known_col` and `unknown_col` intact (`assertEquals(payload, cleanedUnderJson)`), while under default config the same call strips `unknown_col`. Re-enabling this test unchanged (D-10) is itself the acceptance check for this guard's exact boolean shape — get the `||` wrong and this specific assertion fails.

### Pattern 2: Synthetic `Table` against upstream's immutable 5-arg constructor (D-04)

**What:** `new Table(database, tableName, false, List.of(), 0)`.
**Verified constructor signature**, `[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java:48]`:
```java
    public Table(String database, String name, boolean hasDefaults, List<Column> columns, int numColumns) {
```
— exact 5-arg order `(database, name, hasDefaults, columns, numColumns)` D-04 assumes. `hasDefaults()` is a plain getter over the constructor arg (`Table.java:62-64`), confirmed independent of `@Getter(AccessLevel.NONE)` on the field (`Table.java:35-36`) — i.e. `hasDefaults` is intentionally excluded from Lombok's generated getters and has its own hand-written `hasDefaults()` method, so `table.hasDefaults()` (the accessor `doInsertBatch:228` calls) is guaranteed to return exactly the constructor's third argument (`false`), not some Lombok-derived name variant.

**Evidence for JSONP-04's "the one place the fork built a `Table` by hand" claim:** grepping `src/main` for every `new Table(` call site today finds exactly two, both in the legitimate DESCRIBE-based path:
```
src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:533:            return new Table(database, tableName, hasDefaults, columns, numColumns);
src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:602:            return new Table(database, tableName, hasDefaults, columns, numColumns);
```
`[VERIFIED: grep -rn "new Table(" src/main/java, this session]` Today there is zero hand-built `Table` anywhere in `ClickHouseWriter.java` — matching CONTEXT.md's "entirely absent from merged src/main" claim exactly. After D-04 lands, `getTable()`'s guard becomes the **third** `new Table(...)` call site in `src/main` and the only one not backed by a real `DESCRIBE` response. This is the concrete "before" state the SUMMARY's JSONP-04 evidence act (a `git diff` read, per CONTEXT.md's Claude's Discretion note) should cite.

### Pattern 3: R-01's mechanism traced precisely — why RowBinary loses data and JSON does not

**What:** Two different consumers of `table.getRootColumnsList()` behave completely differently when the list is empty, and this asymmetry is exactly why operator fact 2 (no live pipeline takes the `Struct`/RowBinary route) makes KEEP survivable.

`validateDataSchema()`, `ClickHouseWriter.java:272-280` (the fail-fast gate `doInsertRawBinaryV2` calls first):
```java
    protected boolean validateDataSchema(Table table, Record record, boolean onlyFieldsName) {
        boolean validSchema = true;
        for (Column col : table.getRootColumnsList()) {
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:272-280]` — an empty `rootColumnsList` makes this loop's body never execute, so `validSchema` stays at its initial value `true`: the fail-fast gate that is supposed to catch a missing/mismatched column instead vacuously *passes*. This is not a separate bug D-04 introduces — it is the same mechanism, just at the validation step rather than the write step.

`writeRecordsRowBinary()`, `ClickHouseWriter.java:1178-1194`, the actual write loop:
```java
    private long writeRecordsRowBinary(List<Record> records, Table table, OutputStream stream, boolean supportDefaults) throws IOException {
        long pushStreamTime = 0;
        for (Record record : records) {
            if (record.getSinkRecord().value() != null) {
                for (Column col : table.getRootColumnsList()) {
                    LOGGER.debug("Writing column: {}", col.getName());
                    ...
                    doWriteCol(value, fieldExists, col, stream, supportDefaults);
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1178-1194]` — with an empty column list, this inner loop's body never runs for any record, so **zero bytes are written per record** while the outer loop still iterates and the RowBinary stream is still opened, sent, and closed successfully. `client.insert(...)` reports `Written Rows: 0` for a batch that contained real records — no exception anywhere in the call chain. This is the literal, line-level confirmation of R-01's mechanism, already asserted empirically (not just by code reading) in `ExplicitJsonInsertPathLiveTest.explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch` `[VERIFIED: src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java:306-347]`.

By contrast, `writeRecordsJson()` (`ClickHouseWriter.java:1257-1299`) never reads `table.getRootColumnsList()` at all — it builds its `Map<String, Object> data` straight from `record.getSinkRecord().value()` (for `SCHEMA_LESS`) or from `struct.schema().fields()` (for `SCHEMA`), and only the `cleanupExtraFields(data, table)` call at line 1281 touches column metadata — the call D-03 explicitly guards to become a no-op. This is exactly why operator fact 2 (production pipelines are `SCHEMA_LESS`) makes KEEP survivable in practice: the `SCHEMA_LESS` route through `doInsertJson` never hits the empty-column vacuous-loop problem at all, because nothing in that route reads the empty list except the already-guarded `cleanupExtraFields`.

### Pattern 4: `jsonInsertColumns` re-attachment via a surviving overload (D-06/D-08)

**Independently re-verified `javap` finding** (D-06/D-07 cite this same fact from the discussion; re-run in this session against the actual resolved artifact, not trusted from the discussion transcript):
```
$ javap com/clickhouse/client/api/Client.class   # unzipped from client-v2-0.9.6.jar in ~/.gradle cache
...
  insert(String, DataStreamWriter, ClickHouseFormat, InsertSettings)                       <- current call site (line 1379)
  insert(String, List<String>, DataStreamWriter, ClickHouseFormat, InsertSettings)          <- D-06's target overload
```
`[VERIFIED: javap on Client.class extracted from ~/.gradle/caches/modules-2/files-2.1/com.clickhouse/client-v2/0.9.6/c3c6346f6a3c248da67cafaa019e6771f9b09c18/client-v2-0.9.6.jar, this session]` Full relevant `javap` output for `com.clickhouse.client.api.Client`:
```
public java.util.concurrent.CompletableFuture<InsertResponse> insert(String, List<?>);
public java.util.concurrent.CompletableFuture<InsertResponse> insert(String, List<?>, InsertSettings);
public java.util.concurrent.CompletableFuture<InsertResponse> insert(String, InputStream, ClickHouseFormat);
public java.util.concurrent.CompletableFuture<InsertResponse> insert(String, List<String>, InputStream, ClickHouseFormat);
public java.util.concurrent.CompletableFuture<InsertResponse> insert(String, InputStream, ClickHouseFormat, InsertSettings);
public java.util.concurrent.CompletableFuture<InsertResponse> insert(String, List<String>, InputStream, ClickHouseFormat, InsertSettings);
public java.util.concurrent.CompletableFuture<InsertResponse> insert(String, DataStreamWriter, ClickHouseFormat, InsertSettings);
public java.util.concurrent.CompletableFuture<InsertResponse> insert(String, List<String>, DataStreamWriter, ClickHouseFormat, InsertSettings);
```
Both the 4-arg `DataStreamWriter` overload `doInsertJsonV2` currently calls (`ClickHouseWriter.java:1379`) and the 5-arg columns-taking sibling exist side by side — confirming D-06's target overload was never removed from the API, only unused at this specific call site since the merge. This directly falsifies JSONP-03's stated rationale ("the overload it attached to no longer exists at that call site") when read as a claim about the *API*, while leaving JSONP-03's *goal* intact — D-07's annotate-don't-flip correction is the right bookkeeping move, not a requirement rewrite.

**D-08's open question, genuinely unresolved by this research:** there is no 4-arg-with-empty-list variant for the `DataStreamWriter` family the way there is for the `InputStream` family (compare: `insert(String, InputStream, ClickHouseFormat, InsertSettings)` at 4 args vs. `insert(String, List<String>, InputStream, ClickHouseFormat, InsertSettings)` at 5 — both exist for `InputStream`; for `DataStreamWriter` only the two forms above exist, one with a column list and one without, and there is no third "explicit empty list" form to test against the two directly). `javap` cannot answer whether the ClickHouse server or the client-v2 request builder renders an *empty* `List<String>` passed to the 5-arg overload as `INSERT INTO t ()` (a syntax error) or as "no column clause" — that is server/client runtime behavior, not a signature fact, and needs the live probe D-08 already calls for during Task execution, not further code reading.

### Pattern 5: All four parked test classes and `JsonInsertColumnsUnitTest` compile today

`env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileJava compileTestJava` was run in this session against the current tree and completed with **zero errors** `[VERIFIED: gradle build output, this session — clean compile, no output on -q]`. This confirms:
- The starting point for this phase's work is a green compile, so D-13's per-commit sequencing has a clean baseline to build from.
- `@Disabled` does not exempt a class from compilation — all five JSON-path test classes (four `@Disabled` plus the always-on `JsonInsertColumnsUnitTest`) already compile against upstream's immutable `Table`, upstream's `ClickHouseSinkConfig`, and the reflective `csc` accessor `MetadataFreeJsonInsertUnitTest` uses. Re-enabling is genuinely a delete-one-annotation-line operation per D-10 — no compile-fix work is hiding behind the `@Disabled`.

### Anti-Patterns to Avoid

- **Restoring pre-merge method bodies wholesale:** Rejected explicitly by D-03. The pre-merge fork's `start()`/`updateMapping()`/`getTable()` bodies (visible at `pre-v1.6.0-sync`) predate three upstream improvements now on the same methods (`suppressTableExistenceException` handling, the `enableDbTopicSplit` empty-mapping branch, the `describeTable` fallback) — restoring the old bodies would silently drop all three.
- **A gate spread across four inlined `csc.isJsonInsertFormatExplicit()` calls with no shared predicate:** Technically valid (D-05 leaves this to planner discretion) but risks drift across four call sites touching every connector's startup and per-batch path — a single `shouldSkipTableMetadata()` matches the pre-merge shape and the codebase's established anti-drift pattern (Phase 7's shared RowBinary message builder, Phase 8's composed MBean names).
- **A client-side `jsonInsertColumns` filter inside `writeRecordsJson`:** Would have been the JSONP-03-literal approach; rejected by D-06 because it silently downgrades COLS-05's loud non-retriable throw to a no-op, and because under D-01's bypass `cleanupExtraFields` is guarded to a no-op, so a filter folded into that same method would be dead code in exactly the mode `jsonInsertColumns` is documented to be used with.
- **Treating `jsonInsertColumns` validation (checking a configured name against real columns before insert) as in-scope:** `JsonInsertColumnsUnitTest`'s own trailing comment states this explicitly: `[VERIFIED: src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsUnitTest.java:87-90]` — "Deliberately NOT covered ... normalizing, validating, or deduplicating any of them in main code would escalate under D-01 (`jsonInsertColumns` validation is locked Out of Scope in `PROJECT.md`)." COLS-05's loud failure comes from ClickHouse itself (`NO_SUCH_COLUMN_IN_TABLE`), not from connector-side pre-validation — do not add any.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Filtering a JSON insert to a named column subset | A client-side field filter that strips a `Map` before serialization | The client-v2 `insert(String, List<String>, DataStreamWriter, ClickHouseFormat, InsertSettings)` overload — confirmed present via `javap` above | Server enforces the column clause and produces ClickHouse's own loud `NO_SUCH_COLUMN_IN_TABLE` for a typo, which COLS-05 already pins; a hand-rolled filter would need to reinvent that validation to keep COLS-05 loud, and would be dead code under the metadata-free bypass anyway (see Anti-Patterns) |
| Detecting whether a table's metadata was fetched | A new boolean field/flag on `Table` | The existing empty `rootColumnsList`/`hasDefaults=false` combination the pre-merge fork already used, re-expressed against the new constructor (D-04) | Adding a new field to `Table` would touch a class every phase from 7 onward has kept a minimal, upstream-matching diff against (`MERGE-CHECKLIST.md` row 29: "fork's diff is Spotless import-reorder only") — growing that diff here works directly against `SUITE-03`'s "fork delta smaller after this milestone" goal |

**Key insight:** every piece of machinery this phase needs (the immutable `Table` constructor, the `List<String>`-taking insert overload, the `csc.isBypassFieldCleanup()` guard shape) already exists in the merged tree or the pinned client jar. The entire task is wiring four early-returns and one overload argument onto code that upstream's 581 commits already rewrote — there is no gap that justifies new abstractions.

## Common Pitfalls

### Pitfall 1: Guarding the wrong side of `cleanupExtraFields`'s existing condition

**What goes wrong:** Writing the guard as a separate `if (shouldSkipTableMetadata()) return m;` placed *before* the existing `if (csc.isBypassFieldCleanup())` block, rather than combining both into one `||` condition, is functionally equivalent but easy to get backwards (e.g., accidentally inverting to `&&`, which would require *both* flags set to skip cleanup — silently breaking `MetadataFreeJsonInsertUnitTest`'s JSON-only assertion).
**Why it happens:** Two independent boolean guards protecting the same early return look interchangeable but a single off-by-operator mistake (`&&` vs `||`) compiles cleanly and only fails at test time.
**How to avoid:** Write it as `if (csc.isBypassFieldCleanup() || shouldSkipTableMetadata()) return m;` and let `MetadataFreeJsonInsertUnitTest.cleanupExtraFieldsBypassDiffersByInsertFormat()` (re-enabled per D-10) be the check, not eyeballing.
**Warning signs:** That test passing under one flag but not the combination; or passing in isolation but failing once both flags happen to be set in a shared test fixture.

### Pitfall 2: Treating the `getTable()` guard as unconditional rather than `insertFormat=json`-specific

**What goes wrong:** `ExplicitJsonInsertPathLiveTest.unsetInsertFormatStillThrowsOnAbsentTable` (`[VERIFIED: ExplicitJsonInsertPathLiveTest.java:116-146]`) specifically asserts that with `insertFormat` **unset**, `getTable()` still throws `RuntimeException` containing "does not exist" for an absent table — i.e. the guard must not accidentally suppress the existence check for non-JSON connectors.
**Why it happens:** All four guard sites are on the path of *every* connector, not just JSON ones (CONTEXT.md's own Integration Points note). A predicate typo that evaluates `true` too broadly (e.g., checking `csc.getInsertFormat() == InsertFormats.JSON` instead of `csc.isJsonInsertFormatExplicit()`) would engage the bypass for the upstream default-branch typo case too — `ClickHouseSinkConfig.java:262-274`'s `getOrDefault(INSERT_FORMAT, "node")` falls through to `InsertFormats.JSON` on an **unset** key (a confirmed upstream quirk, pinned deliberately by `MetadataFreeJsonInsertUnitTest.unsetInsertFormatIsNotExplicitDespiteResolvingToJsonEnum`, `[VERIFIED: MetadataFreeJsonInsertUnitTest.java:79-91]`) — using `getInsertFormat()` instead of the dedicated `isJsonInsertFormatExplicit()` flag would silently widen the bypass to every connector with an unset `insertFormat`.
**How to avoid:** The predicate must be `csc.isJsonInsertFormatExplicit()` (the flag that is `false` unless the raw `insertFormat` string, trimmed, case-insensitively equals `"json"` — `ClickHouseSinkConfig.java:277-278`), never `csc.getInsertFormat() == InsertFormats.JSON`.
**Warning signs:** `unsetInsertFormatStillThrowsOnAbsentTable` (JSON-01) failing after re-enablement — that specific test exists to catch exactly this class of mistake.

### Pitfall 3: `MERGE-CHECKLIST.md` row 26 currently overstates Phase 7's coverage of the JSON path

**What goes wrong:** `MERGE-CHECKLIST.md:26` marks `ClickHouseWriter.java`'s conflict row `done` (owned by Phase 7) and its "fork behaviour at risk" column lists "the synthetic `Table` constructor call backing the metadata-free JSON path" — but that row's `adjudicating test` column is `WRT-01, WRT-02, WRT-03, WRT-05` only, none of which cover the JSON path. `[VERIFIED: .planning/MERGE-CHECKLIST.md:26, read this session]` A future reader skimming only the `status` column could reasonably conclude the JSON path was already re-applied in Phase 7, when CONTEXT.md's own measured-starting-state table (and this session's independent `grep` for `new Table(`) confirms it is not.
**Why it happens:** The row's descriptive text was generated to describe everything that conflict *path* originally touched pre-merge, not to scope exactly what Phase 7's slice of the work closed — a reasonable design for a single-row-per-file checklist, but one that leaves a stale-looking "done" next to still-open work until a later phase appends a note.
**How to avoid:** Phase 9's commits should append an annotate-don't-flip note to this row (same pattern as D-07's `REQUIREMENTS.md` note and D-14's Recorded Fact 13 resolution), clarifying that the JSON-path portion of row 26's description was closed in Phase 9, not Phase 7. Do not flip the row's owning phase or status field.
**Detection signal:** None automated — this is a documentation-only pitfall a reviewer would only catch by cross-referencing `MERGE-CHECKLIST.md` against `CONTEXT.md`'s measured-starting-state table, which is exactly how this research surfaced it.

### Pitfall 4: `TableTest.extractNullables()`'s stale assertion is a live-suite-only failure, invisible to `testUnit`

**What goes wrong:** `TableTest` extends `ClickHouseBase` (`[VERIFIED: src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/TableTest.java:17]`), which carries `@Tag("clickhouse")` — so `extractNullables()`'s stale `assertEquals(3, table.getAllColumnsList().size())` at line 51 never runs under `testUnit` and only surfaces in a full `./gradlew test` run. A planner or executor relying on `testUnit` staying green throughout D-13's commit sequence would not see this failure until the D-12 full-suite gate at the very end.
**Why it happens:** `shouldSkipDescribeRow` (a fork-only method, `HLP-02`) strips the structural `date_number.null` subcolumn from a `Nullable(Date)` column before it ever reaches `Table`'s constructor, so `allColumnsList` naturally contains 2 entries (`off16`, `date_number`), not the 3 the stale assertion (inherited from upstream's own `TableTest.java`, never reconciled against the fork's already-shipped `.null`-skipping design) expects.
**How to avoid:** Land D-14's one-line fix (`3` → `2`, with a comment citing `DESC-01`/`HLP-02`) as its own commit early in the sequence, not deferred to the end — otherwise it is easy to mistake this pre-existing carry-in failure for a regression introduced by this phase's own guard work during the D-12 full-suite run.
**Warning signs:** A full `./gradlew test` run showing exactly one failure in `TableTest`, with a diff that touches nothing in `Table.java` or `ClickHouseHelperClient.java` for this phase's own commits.

### Pitfall 5: The 13 re-enabled live tests have not executed once since the 581-commit merge

**What goes wrong:** `ExplicitJsonInsertPathLiveTest` (6 methods), `JsonInsertColumnsLiveTest` (5), and `ClickHouseSinkTaskJsonInsertFormatTest` (2) — 13 methods total — were `@Disabled` by Phase 7 before ever running against the merged tree (`[VERIFIED: `@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")` present verbatim in all three files at the lines cited above]`). Phase 7's own gap-closure already demonstrated once in this milestone that a green `testUnit` can sit on top of an entirely broken live behaviour (`targetTableFilter` appended after `group by`, STATE.md's Phase 07 entry) — there is no reason to assume these 13 methods are bug-free just because they compile and were green before the merge.
**Why it happens:** `@Disabled` classes are dead code from the compiler's perspective but not from the *behavior's* perspective — re-enabling them is the first live exercise of guard code that also sits on every non-JSON connector's `start()`/`updateMapping()`/`getTable()` path.
**How to avoid:** D-12's full-suite gate (not just a targeted run of the four JSON classes) is the right call precisely because these guards have blast radius beyond JSON connectors — a targeted run cannot see collateral damage to, say, a non-JSON `ClickHouseSinkTaskTest` that happens to share `start()`'s call path.
**Warning signs:** Any of the 13 newly-enabled methods failing on first live run — this is expected-possible, not evidence of a botched re-application; investigate each on its own merits rather than assuming the guard logic must be at fault.

## Code Examples

The following are the exact starting-state snippets a plan's tasks will diff against — quoted verbatim and line-cited above under Architecture Patterns 1–4, not repeated here. See:
- `start()` insertion point — Pattern 1, `ClickHouseWriter.java:124-125`
- `updateMapping()` insertion point — Pattern 1, `ClickHouseWriter.java:142-149`
- `getTable()` full method — Pattern 1, `ClickHouseWriter.java:1592-1615`
- `cleanupExtraFields()` full method — Pattern 1, `ClickHouseWriter.java:1387-1399`
- `Table`'s 5-arg constructor — Pattern 2, `Table.java:48`
- `doInsertJsonV2`'s current 4-arg call site — Pattern 4, `ClickHouseWriter.java:1379`: `client.insert(table.getName(), dataWriter, ClickHouseFormat.JSONEachRow, insertSettings)`

## State of the Art

| Old Approach (JSONP-03's stated assumption) | Current Approach (this session's `javap` finding) | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `client.insert(table, columns, data, format, settings)` — the overload `jsonInsertColumns` attached to pre-merge — is assumed gone from the client-v2 API | The overload's *name and slot* at the old call site is gone (upstream's `doInsertJsonV2` now calls a different 4-arg `DataStreamWriter` overload), but the **columns-taking sibling overload still exists** in `client-v2-0.9.6`'s `Client` class | Confirmed unchanged as of `0.9.6` (the version this project already pins) — this was never actually removed between the fork's pre-merge state and now; JSONP-03's rationale was simply wrong about the API, not about the call site | D-06 can re-attach `jsonInsertColumns` server-side with no client-jar version bump and no behavior change to COLS-01..06's contract |

**Deprecated/outdated:** `PITFALLS.md` Pitfall 4's recommendation ("[`jsonInsertColumns`] must be redesigned as a filter step inside `writeRecordsJson`'s field-selection logic") is superseded by this session's and D-06/D-07's `javap` finding — that recommendation was written before the overload's continued existence was checked and should not be followed literally by the planner, even though it remains useful context for *why* a redesign looked necessary at first glance.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The three "operator facts" in CONTEXT.md (at least one live connector sets `insertFormat=json`; those pipelines are `SCHEMA_LESS`; the strictest constraint set — including missing `DESCRIBE` grant — applies) are taken as given, locked user input, not independently re-verified against any production system in this session | Whole phase's premise (D-01's KEEP call) | These are stated by CONTEXT.md as `operator-confirmed` and are exactly the class of fact "the repository cannot supply" (CONTEXT.md's own framing) — this research had no live production system to check them against, and re-litigating them is explicitly out of scope per D-02. If wrong, the keep/drop call itself would need to be reopened, which is a decision-level concern for the user, not a code-level risk this plan's tasks could introduce |
| A2 | D-08's branch condition (call the 4-arg overload for an empty `jsonInsertColumns` list, the 5-arg for non-empty) is the correct choice absent a live re-proof that the `DataStreamWriter` 5-arg overload tolerates an empty `List<String>` the same way the `InputStream` overloads do | Pattern 4 / Code Examples | If a live COLS-02 probe during execution shows the 5-arg overload safely renders "no column clause" for an empty list, D-08 already permits switching to unconditional — this is explicitly flagged as needing a live check, not a documentation gap |

**Everything else in this document was independently opened and read with `Read` this session, or confirmed via `javap`/`grep`/a clean `compileJava`/`compileTestJava` run — no other claim rests on training-data recall of this specific fork's code.**

## Open Questions

1. **Does `client.insert(table, List.of(), dataWriter, format, settings)` render a mis-formed `INSERT INTO t ()` or correctly fall back to "no column clause"?**
   - What we know: the 5-arg `DataStreamWriter` overload exists (confirmed by `javap`); the pre-merge fork passed its old `InputStream`-based column list unconditionally and COLS-02 proved an empty list meant "no clause" **on that different overload**.
   - What's unclear: whether the same guarantee holds for the `DataStreamWriter` overload — `javap` gives signatures, not runtime behavior.
   - Recommendation: D-08 already resolves this procedurally (branch on non-empty by default; a live COLS-02 re-run may permit going unconditional). A planner should keep D-08's default and treat "prove unconditional is safe" as an optional stretch check inside the COLS-02 task, not a blocking prerequisite.

2. **Should `MERGE-CHECKLIST.md` row 26's description be edited in this phase, per Pitfall 3?**
   - What we know: the row's `status`/`owning phase` are Phase 7's to own (per the document's own rule — "do not flip a row you do not own"); its descriptive text is stale with respect to the JSON path specifically.
   - What's unclear: whether an annotate-don't-flip note on someone else's row needs the same "stops for a user decision" treatment D-14 got for its cross-scope test change, or whether it is squarely inside this phase's own "record the keep-or-drop disposition ... in `MERGE-CHECKLIST.md`" instruction (CONTEXT.md D-01's fourth bullet already directs exactly this).
   - Recommendation: Treat it as already in-scope — D-01 explicitly says to "Record it as such in `MERGE-CHECKLIST.md`" — and land it as part of D-13's commit 6 ("checklist flips and Recorded Facts"), appending rather than editing existing prose, consistent with every other cross-phase note in this document.

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | Unchanged by this phase — no auth code touched |
| V3 Session Management | No | N/A — connector has no session concept |
| V4 Access Control | Yes (accommodation, not enforcement) | This phase's entire premise (operator fact 3) is that the connector's ClickHouse user *lacks* `DESCRIBE`/`SHOW` privilege on target tables — the bypass is a least-privilege accommodation, not a privilege-escalation risk. No new grant, credential, or permission check is added or removed by the four guards; they only change whether the connector *attempts* metadata queries it may not have rights to run |
| V5 Input Validation | Yes (unchanged) | `jsonInsertColumns`' behavior on a nonexistent column name stays a loud, server-side, non-retriable failure (`NO_SUCH_COLUMN_IN_TABLE`, pinned by COLS-05) — `[VERIFIED: JsonInsertColumnsLiveTest.java:202-259]`. D-01 explicitly locks connector-side validation of that list out of scope (`JsonInsertColumnsUnitTest.java:87-90`); do not add any in this phase |
| V6 Cryptography | No | Not touched — no new credential handling, no new serialization of secrets |

### Known Threat Patterns for This Phase's Scope

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Silent data loss (R-01) — `SCHEMA` records land as zero written rows with no exception, for the entire lifetime of the KEEP decision | Repudiation / Integrity (CWE-390: Detection of Error Condition Without Action) | Not fixed in this phase by design (JSONP-02 requires the loss stay *pinned*, not fixed) — the mitigation is operational, not code: operator fact 2 (no live pipeline takes the `Struct` route today) is the accepted-risk basis, and `ExplicitJsonInsertPathLiveTest`'s R-01 pin is the detection contract that would catch a future pipeline drifting onto the lossy route. A genuine fix (fail-fast throw) is explicitly deferred (Deferred Ideas, R-01 fail-fast) |
| Table-name / identifier injection into the synthetic `Table`'s name | Tampering | Already mitigated upstream and unchanged by this phase — `Table.getName()` always routes through `Utils.escapeName(name)` (`Table.java:69-71`, `[VERIFIED: Table.java:69-71]`) regardless of whether the `Table` was hand-built (D-04's synthetic path) or `DESCRIBE`-sourced; the guard reuses the same `Utils.getTableName`/`getMappedOrTopicTableName` helpers every other code path already uses, so no new unescaped string reaches a query |
| `jsonInsertColumns` column-name list reaching the `INSERT` column clause unescaped | Tampering / Injection | Pre-existing behavior, unchanged by D-06 — the list already flows into `client.insert(...)`'s column-list parameter today via the pre-merge design this phase re-attaches; client-v2 constructs the column clause from the typed `List<String>` parameter (not string concatenation the connector controls), and COLS-01's parsing contract only trims/splits on commas, adding no new escaping responsibility to this phase's code |

**No new attack surface is introduced by this phase.** The four guards are pure control-flow (whether to call an already-existing, already-audited code path), the synthetic `Table` reuses upstream's constructor and the fork's own existing escaping helpers, and the overload swap in `doInsertJsonV2` re-attaches a parameter list to a different already-existing client-library method rather than constructing any new SQL. The one open item is fully understood and explicitly out of scope by decision, not by oversight: R-01's silent loss.

## Sources

### Primary (HIGH confidence — all read or executed directly in this session)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (1643 lines) — `start()`, `updateMapping()`, `doInsertBatch()`, `validateDataSchema()`, `doInsertJson()`/`doInsertJsonV1()`/`doInsertJsonV2()`, `writeRecordsJson()`, `cleanupExtraFields()`, `doInsertRawBinary()`/`doInsertRawBinaryV2()`, `writeRecordsRowBinary()`, `getTable()`, `startBackgroundTableSync()` — read in full/relevant sections
- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` (874 lines) — constant declarations, `jsonInsertColumns`/`jsonInsertFormatExplicit` fields and parsing, `@Getter` class annotation
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java` (245 lines) — read in full; constructor, `hasDefaults()`, `getFullName()`/`getName()`
- `src/main/java/com/clickhouse/kafka/connect/util/Utils.java:175-199` — `getTableName`/`getMappedOrTopicTableName`
- `src/main/java/com/clickhouse/kafka/connect/sink/db/TableMappingRefresher.java` — read in full
- `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java` (119 lines) — read in full
- `src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java` (384 lines) — read in full
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsUnitTest.java` (92 lines) — read in full
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java` (344 lines) — read in full
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java` (159 lines) — read in full
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/TableTest.java` (161 lines) — read in full
- `javap com/clickhouse/client/api/Client.class` extracted from `~/.gradle/caches/modules-2/files-2.1/com.clickhouse/client-v2/0.9.6/c3c6346f6a3c248da67cafaa019e6771f9b09c18/client-v2-0.9.6.jar` — executed this session
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileJava compileTestJava` — executed this session, clean pass
- `.planning/MERGE-CHECKLIST.md` — rows 23-40, Recorded Facts 10-13, closing paragraph — read this session
- `.planning/config.json` — `workflow.nyquist_validation: false`, `workflow.security_enforcement: true`, `security_asvs_level: 1` confirmed this session

### Secondary (MEDIUM confidence)
- `.planning/research/PITFALLS.md` Pitfall 4 (lines 104-123) and its summary-table row (lines 224-225) — pre-milestone research, partially superseded by this session's `javap` finding (see State of the Art)
- `.planning/research/SUMMARY.md` "Must-not-silently-lose list" (lines 135-145) — pre-milestone research, five-entry list D-11 proposes extending to six

### Tertiary (LOW confidence)
- None — every claim in this document traces to a file `Read` this session, a tool execution this session, or an explicit CONTEXT.md locked decision (marked as such, not re-derived).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new dependencies; existing artifact version confirmed present in local Gradle cache
- Architecture: HIGH — every guard site, the `Table` constructor, and the insert overload were read/executed directly, not inferred
- Pitfalls: HIGH for Pitfalls 1, 2, 4, 5 (each traces to a specific test assertion or a specific file/line already in the tree); MEDIUM for Pitfall 3 (a documentation-consistency observation, not a code-correctness one)

**Research date:** 2026-09-20
**Valid until:** Until the next commit touches `ClickHouseWriter.java`, `ClickHouseSinkConfig.java`, `Table.java`, or the pinned client-v2 version — this is code-archaeology research, not library-currency research, so it does not expire on a calendar schedule the way a "current best practices" finding would.
