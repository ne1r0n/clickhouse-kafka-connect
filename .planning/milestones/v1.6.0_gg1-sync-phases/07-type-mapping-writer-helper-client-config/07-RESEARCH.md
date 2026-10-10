# Phase 7: Type Mapping, Writer, Helper Client & Config - Research

**Researched:** 2026-09-19
**Domain:** Java merge-conflict re-expression — Kafka Connect sink connector type mapping, RowBinary writer, ClickHouse HTTP helper client, and connector config, against upstream `v1.6.0`'s rewritten `Column`/`Table`/`ClickHouseWriter`/`ClickHouseHelperClient`/`ClickHouseSinkConfig`
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**The Compile Blocker**

- **D-01:** `src/main` currently fails `compileJava` on **exactly one error**, and it is not in any Phase 7 file: `ClickHouseSinkTask.java:149` carries `@Override` on `onPartitionsRevoked(Collection<TopicPartition>)`, which **connect-api `4.1.1` does not declare** (removed from `SinkTask` in Kafka 3.0; verified by `javap` against the resolved jar). Upstream declares the method because its catalog pins connect-api `2.7.0`; the pre-merge fork never had it (upstream added it in `c36ab418`). **This is a new fork/upstream incompatibility created by the locked Kafka `4.1.1` override, not merge fallout.**

- **D-02:** The fix is to **delete the `@Override` annotation only** — one line — keeping upstream's method and body. Rationale: `connect-api` is declared `api(...)` and Connect workers serve those classes parent-first, so the bridge still fires if the connector is ever loaded by a pre-3.0 worker, and it costs nothing under `4.1.1`. Add a comment naming connect-api `4.1.1` as the reason. Do **not** delete the method and do **not** add a fork-only shim interface.
  - **Reversibility:** reversible — one annotation, one file.

- **D-03:** Phase 7 lands D-02 as a **named, separately-committed change**, but `MERGE-CHECKLIST.md`'s `ClickHouseSinkTask.java` row **stays `pending` and owned by Phase 8**. Phase 7 appends a note to the row recording that the one-line compile fix landed early and in which commit. Phase 11's zero-pending assertion is unaffected.

- **D-04:** The API gap is also recorded as a **named entry in `MERGE-CHECKLIST.md` § Recorded Facts** — connect-api `4.1.1` removed `SinkTask.onPartitionsRevoked`, upstream still declares it under its `2.7.0` pin, the fork carries a deliberate one-line divergence. Feeds Phase 11's SUITE-03 dropped-changes record and warns the next sync.

**The Test Gate and the Phase 10 Boundary**

- **D-05:** ROADMAP criterion #2's `./gradlew testUnit` gate is **kept**, reached by a **mechanical carve-out** from Phase 10 — not by downgrading the gate and not by fixing the whole test tree. Measured blast radius that forces this:
  - `uniqueConnectorName(TestInfo)` — used by **9** fork test files, **absent** from the merged `ClickHouseBase`
  - `@Tag("clickhouse")` — absent, so `testUnit` would start Docker
  - `FromVersionConditionExtension` moved to `com.clickhouse.kafka.connect.test.junit.extension` under `src/testFixtures` — **1** stale import (`JsonColumnTest.java`)
  - dead `Table` API — **4** fork test files (`ClickHouseHelperClientForkUnitTest` ×11 sites, `RowBinaryUnsupportedTypeGateTest`, `MetadataFreeJsonInsertUnitTest`, `ClickHouseHelperClientForkLiveTest`)

- **D-06:** From the fork's `ClickHouseBase`, Phase 7 restores **`@Tag("clickhouse")` and `uniqueConnectorName(TestInfo)` + its counter, and nothing else.** The `@AfterEach cleanupLeakedMBeans` sweep, the shared-container-per-JVM-fork strategy, `newTestDatabaseName`, and the helper methods all stay Phase 10. The fork's base is `static`-based while upstream's is `@TestInstance(PER_CLASS)` with instance methods, so a wholesale restore is not available anyway — but both restored members are shape-independent.

- **D-07:** `MERGE-CHECKLIST.md`'s `ClickHouseBase.java` row **stays `pending` and owned by Phase 10**; Phase 7 appends a note recording that the tag + connector-name sliver landed early and in which commit. Same bookkeeping pattern as D-03. Do not split the row and do not flip it.

- **D-08:** `shouldSkipDescribeRow` is re-expressed as **`shouldSkipDescribeRow(ClickHouseFieldDescriptor, Map<String, Column>)`**. Upstream's describe loop builds a local `List<Column>` and constructs the immutable `Table` only at the end, so the fork's `table.getAllColumnsMap()` lookup is unavailable; the loop maintains an incremental name→`Column` map alongside `columns`. The helper stays a pure static function with O(1) parent lookup, and `ClickHouseHelperClientForkUnitTest`'s 11 cases build the map directly instead of a `Table`. Rejected: an accumulating `List<Column>` with a linear scan per DESCRIBE row, and constructing an interim `Table` on every iteration.

- **D-09:** **HARN-04 is already satisfied by the mechanical merge** — exactly one `ClickHouseTestHelpers.java` exists, under `src/testFixtures`, in package `com.clickhouse.kafka.connect.sink.helper` (the fork's import path is unchanged). Phase 7 does not touch it; Phase 10 confirms by grep as its requirement states.

**The JSON-Path Default Phase 9 Inherits**

- **D-10:** Phase 7 **re-applies nothing of the metadata-free `insertFormat=json` bypass** — not `shouldSkipTableMetadata`, not the synthetic `Table` in `getTable`, not the `start()` / `updateMapping()` short-circuits, not the `cleanupExtraFields` bypass. The mechanical merge already removed all of it; `src/main` is pure upstream there today. **Phase 9's blocking JSONP-01 checkpoint therefore inherits "dropped" as its default**, and "keep" is the option that costs work. This matches the milestone's conflict policy (prefer upstream unless the fork's tests fail) and means Phase 7 never writes code Phase 9 might delete.
  - **Reversibility:** reversible within the milestone — the fork's version is recoverable from `pre-v1.6.0-sync` at any time; Phase 9 owns the call.

- **D-11:** The fork's JSON-path tests — `MetadataFreeJsonInsertUnitTest`, `ClickHouseSinkTaskJsonInsertFormatTest`, `ExplicitJsonInsertPathLiveTest`, `JsonInsertColumnsLiveTest`, and R-01's labelled loss test — are made to **compile against the new `Table`, then `@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")`**. Nothing is deleted and no assertion is weakened; Phase 9 either removes the annotation or deletes the test outright. Phase 11's skip accounting sees a named reason. Rejected: tagging them `clickhouse` to dodge `testUnit` (hides a red test behind a tag that means something else) and leaving them failing (gives up a binary gate for Phases 7–8).

- **D-12:** Phase 7's SUMMARY carries a short **evidence brief for Phase 9's checkpoint**: that upstream's `writeRecordsJson` inserts via `client.insert(tableName, dataWriter, format, settings)` at `ClickHouseWriter.java:1307` (the `(table, columns, data, …)` overload `jsonInsertColumns` attached to is gone from that call site, confirming JSONP-03's premise), that `jsonInsertColumns` must be redesigned as a field filter inside `writeRecordsJson`, and which fork tests are `@Disabled` pending the call. JSONP-01 opens on evidence rather than a fresh investigation.

**Configuration**

- **D-13:** Both config keys and all their parsing land in Phase 7, per CFG-01: `TARGET_TABLE_FILTER` and `JSON_INSERT_COLUMNS` `configDef.define` entries, and the `targetTableFilter` / `jsonInsertColumns` / `jsonInsertFormatExplicit` fields with their parsing. `jsonInsertColumns` and `jsonInsertFormatExplicit` are briefly read by nothing (their consumer returns in Phase 9 if JSONP-01 says keep) — that is a one-phase gap, not a defect. `JsonInsertColumnsUnitTest` and `AsyncInsertSettingsUnitTest` pass here.

- **D-14:** CFG-02: `this.addClickHouseSetting("async_insert", "0", false)` is back at `ClickHouseSinkConfig.java:240` after the merge and must be **commented out again**, exactly as the fork had it.

- **D-15:** CFG-03 is proved by **a Docker-free parametrized defaults test plus a written inventory**. The test builds a default `ClickHouseSinkConfig` and asserts each of the thirteen getters returns upstream's default (catching a silently-dropped `configDef.define` on the next sync); the SUMMARY table names each option, its default, and its consumer (which a getter assertion cannot show). The thirteen, enumerated from the merged file: `auto.evolve`, `auto.evolve.ddl.refresh.retries`, `auto.evolve.struct.to.json`, `bufferCount`, `bufferFlushTime`, `clickhouseClientInsertTimeoutMs`, `clusterName`, `debeziumCDCEnabled`, `enableReplicaPinning`, `errors.retry.timeout`, `reportInsertedOffsets`, `retryOnSocketException`, `ssl_socket_sni`.

**Fork Delta: What Comes Back**

- **D-16:** All four of the fork's non-behavioural tweaks are **re-applied**. No test anywhere asserts a log level (`grep` for `ListAppender` / `LogCaptor` / `Appender` across `src/test` and `src/testFixtures` returns nothing), so each was judged on merit:

  | Site | Upstream today | Re-applied as | Why |
  |---|---|---|---|
  | `ClickHouseHelperClient.java:315` | `System.out.println("query " + query + " retry …")` | `LOGGER.debug` | unconditional stdout on every query retry in a production Connect worker |
  | `ClickHouseWriter.java:1355` | `LOGGER.debug(String.format("data: %s", data))` | `LOGGER.trace` | `AGENTS.md`: do not log data in plaintext; `CONVENTIONS.md` reserves TRACE for per-record detail |
  | `ClickHouseHelperClient.java:437,497` | `LOGGER.warn("Unable to handle column")` | `LOGGER.error` | an unmappable column makes `describeTable` return null and the whole table is dropped from the mapping — louder than WARN implies; deliberate Phase 3 decision |
  | `ClickHouseWriter.java:220` | `"Trying to insert [{}] records to table …"` | adds `schema type [{}]` | first thing wanted when diagnosing a wrong-format insert |

- **D-17:** HLP-04 is satisfied by **patching the single remaining `InputStreamReader` (`ClickHouseHelperClient.java:466`, V2 describe path) with `StandardCharsets.UTF_8` and grepping `InputStreamReader(` across the whole resolved file** to prove no other lacks a charset — exactly as the requirement words it. The fork's rewrite of `describeTableV1` into a UTF-8 `BufferedReader` is **dropped**: upstream's V1 path now reads via `response.records()`, and V1 is out of both the test matrix and production, so re-expressing it would grow the delta on a path nobody runs. No standing charset-regression test is added.

- **D-18:** FREE-02 and FREE-03 are **already answerable from the merged tree and are recorded in `MERGE-CHECKLIST.md` § Recorded Facts** — the same home as Phase 6's SYNC-04 / SYNC-05 facts and D-04's new entry, and the one place Phase 11's SUITE-03 record reads from. `PROJECT.md` → Active is updated at the phase transition.
  - **FREE-02 / WRT-04:** upstream's `Map(K, Nullable(V))` null-marker fix is present at `ClickHouseWriter.java:562` **with an extra `col.getMapValueType() != null` guard** the fork's patch lacked; the fork's duplicate was already dropped by the mechanical merge. WR-01's status (whether the actual-`null`-entry path is now covered) must be stated either way.
  - **FREE-03 / P3-WR-02:** upstream's `describeTableV2` now builds `DESCRIBE TABLE \`%s\`.\`%s\`` from the **passed-in `database` parameter**, so the fork's dormant follow-up is closed by upstream. Record it; do not fix anything.

**Commit Shape**

- **D-19:** Because the mechanical merge left `src/main` as pure upstream, every fork behaviour Phase 7 re-applies can be an isolated diff against `v1.6.0` — the property D-02 of Phase 6 was built to preserve. Phase 7 therefore commits **one fork behaviour per commit**: unblock-compile → `Type.DYNAMIC` + `Column` → RowBinary fail-fast → `targetTableFilter` → describe skip + UTF-8 → config. Each is its own reviewable diff against upstream (which is exactly the record SUITE-03 must produce in Phase 11), and a bisect lands on one behaviour. Not one commit per file and not one commit per plan.

### Claude's Discretion

The user did not constrain the following; these are the calls made, all reversible and open to a planner overriding them with a reason:

- **WRT-02's gate contents:** `validateDataSchema`'s fail-fast enumerates `DYNAMIC` and `UNKNOWN` only. `JSON` is removed from the fork's gate in favour of upstream's native `case JSON:` at `ClickHouseWriter.java:651`, and `TYPE-06`'s JSON assertions are rewritten to upstream's exception type and message rather than the fork's — per the milestone's locked "RowBinary + JSON" decision.
- **WRT-01's shape:** `doWriteColValue`'s `default:` at `ClickHouseWriter.java:672` is still upstream's silent `LOGGER.error("Cannot serialize unsupported type {}")`. Re-apply the fork's `buildRowBinaryUnsupportedTypeMessage` helper so the writer gate and the validate gate cannot drift in wording, and throw a `DataException`. The message names only the column, its type, and the two escape hatches — never any part of the record payload.
- **`Column.java` is a two-line change.** Upstream already dispatches `valueType.startsWith("JSON") → Type.JSON` at `Column.java:173`; the fork's only behavioural addition is the `startsWith("Dynamic") → Type.DYNAMIC` branch immediately after it. Everything else in the fork's `Column` diff is Spotless import reordering. Upstream's `SimpleAggregateFunction` unwrapping and Avro/Protobuf union handling are adopted untouched (MAP-02).
- **`Table.java` needs no production edit at all.** Its checklist row is `redundant`; the fork's diff was Spotless-plus-brace-style. MAP-03 is satisfied by rewriting call sites in `ClickHouseWriter` / `ClickHouseHelperClient` / the four fork test files to the 5-arg constructor — the constructor is never re-opened and `addColumn` stays private.
- **HLP-03 is nearly free.** Upstream already increments `numColumns` for non-subcolumn rows *before* its alias/materialized/ephemeral skips (`ClickHouseHelperClient.java:411-413`, `:471-473`). The fork's skip applies only to subcolumns, so inserting it after that increment satisfies HLP-03 by construction.
- **The four conflicted Phase 7 test files need nothing restored.** `ColumnTest`, `TableTest`, `ClickHouseWriterTest` and `ClickHouseHelperClientTest` were resolved to upstream by the mechanical merge, and the fork's deltas on them were **pure Spotless import reorders** — verified by `git diff bec167d pre-v1.6.0-sync`. Their checklist rows are flippable to `done` on verification alone. The fork's real canaries are the fork-only files that survived untouched.
- **Spotless:** run `./gradlew spotlessApply` before each commit, per `AGENTS.md`.

### Deferred Ideas (OUT OF SCOPE)

- **A standing charset-regression test** that fails when any `InputStreamReader` in `ClickHouseHelperClient` lacks an explicit charset. Considered under D-17 and not adopted — the grep proves HLP-04 as worded. Worth revisiting if a future sync reintroduces an unguarded reader.
- **Re-expressing the fork's `describeTableV1` UTF-8 `BufferedReader` rewrite.** Dropped under D-17 because V1 is outside both the test matrix and production. If client V1 ever re-enters the matrix, this returns with it.
- **Splitting `MERGE-CHECKLIST.md`'s `ClickHouseBase.java` row** into a Phase 7 half and a Phase 10 half. Rejected under D-07 in favour of a note, to avoid editing the generated Section 1 structure.
- **Sweeping `src/test` / `src/testFixtures` for further Kafka `2.7.0`-vs-`4.1.1` API gaps** beyond `onPartitionsRevoked`. Not pursued as its own investigation; any such gap surfaces as a compile error under D-05's `compileTestJava` requirement and gets fixed there.
- **Adopting `retryOnSocketException`** — `REQUIREMENTS.md` § Future Requirements calls it "worth deliberately enabling" against a client-V2 persistent-connection failure mode the fork is exposed to today. CFG-03 keeps it at its upstream default; enabling it is feature work, out of scope.
- **Fixing CR-01** (`.planning/scripts/verify-merge-invariants.sh:44` treats a `git grep` error as "no match", so Invariant 2 can report clean on a broken pathspec). Carried from Phase 6's security audit; must be fixed before Phase 11, which gates publication on that script running unattended. Not Phase 7 work.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| MAP-01 | `Type.DYNAMIC` exists alongside upstream's `Type.JSON`; `Column` dispatches `Dynamic` strings to it, never silently to `UNKNOWN` | Pattern 1 — exact current `Type.java`/`Column.java` content quoted, fork's pre-merge target quoted, `JsonDynamicTypeMappingTest` identified as the non-compiling canary |
| MAP-02 | Upstream's rewritten `Column.java` adopted for everything else (`SimpleAggregateFunction`, Avro/Protobuf unions), `TYPE-01..05` passing | Pattern 1 — confirmed upstream's union/aggregate-function handling already present and untouched by this phase's edit |
| MAP-03 | Upstream's immutable `Table` adopted as-is; every fork call site rewritten to the 5-arg constructor, constructor never re-opened | Pattern 1/4 — exact constructor signature quoted from `Table.java:48`, confirmed `addColumn` private; call sites needing rewrite enumerated (ClickHouseHelperClientForkUnitTest ×11, RowBinaryUnsupportedTypeGateTest, MetadataFreeJsonInsertUnitTest) |
| WRT-01 | `doWriteColValue`'s `default:` throws the fork's actionable `DataException` instead of upstream's silent `LOGGER.error` | Pattern 2 — current silent default quoted (`ClickHouseWriter.java:669-673`), fork's throwing target quoted |
| WRT-02 | `validateDataSchema`'s fail-fast gate covers `DYNAMIC`/`UNKNOWN`; `JSON` removed from the gate in favour of upstream's `case JSON:` | Pattern 2 — current gate's absent case labels quoted (`:291-300`), fork's pre-merge 3-case gate quoted with the JSON-removal discretion noted, Pitfall 2 |
| WRT-03 | Upstream's flag-gated JSON-as-string RowBinary write available; `TYPE-06` JSON assertions rewritten to its exception type/message | Pattern 2 — upstream's `case JSON:` block quoted in full (`:651-668`), including the exact `RuntimeException` messages to assert against |
| WRT-04 | Fork's duplicate `Map(K, Nullable(V))` patch dropped in favour of upstream's (extra null guard); pinning test still passes | Pitfall 7 / FREE-02 — upstream's guarded line quoted verbatim (`:562`), confirmed no code change needed |
| WRT-05 | `setTargetTableFilter(...)` still reaches the helper-client builder from `ClickHouseWriter.start()` | Pattern 3 — current `start()` builder chain quoted (`:84-96`), confirmed no `.setTargetTableFilter(...)` call exists yet |
| HLP-01 | `targetTableFilter` still restricts table discovery server-side via `match(table, '<regex>')` over `List<Table.TableDesc>`; `FILT-01..04` green | Pattern 3 — current `showTables`/`showTablesV1`/`showTablesV2` quoted in full, fork's `buildTableFilterClause` quoted, injection point recommended |
| HLP-02 | `shouldSkipDescribeRow`'s structural rule re-expressed against the local-`List<Column>` loop; `DESC-01..04` green, bare `.size` covered | Pattern 4 — both describe loops quoted in full, fork's method quoted, `Table.SIZE_FIELD_MATCHER`'s regex gap documented (Don't Hand-Roll) |
| HLP-03 | `numColumns` incremented before any skip decision, matching upstream's alias/materialized/ephemeral treatment | Pattern 4 / Pitfall 4 — increment-ordering confirmed already correct upstream, no code change required, only insertion-point discipline |
| HLP-04 | Every `InputStreamReader` in the resolved file specifies UTF-8; `TYPE-07` green under `windows-1252` | Pattern 5 — exact line quoted (`:466`), confirmed sole occurrence via grep this session, V1's absence of any reader confirmed |
| CFG-01 | `targetTableFilter`/`jsonInsertColumns` survive as config keys with parsing intact | Pattern 6 — confirmed absent from current file, fork's constants/fields/parsing/ConfigDef.define quoted in full |
| CFG-02 | No `async_insert` setting sent by default; `ASYNC-01..03` green | Pattern 6 — exact current forcing line quoted (`:240`), fork's commented-out form quoted |
| CFG-03 | All thirteen new upstream config options present, reach consumers at upstream defaults | Pattern 6 — all thirteen constant names and line numbers cross-checked this session, Lombok `@Getter` mechanism noted |
| FREE-02 | `Map(K, Nullable(V))` fix confirmed present with extra guard; fork's duplicate confirmed dropped; WR-01 status recorded | Pitfall 7 — verbatim quote of the guarded line, confirmed no `src/main` change needed, only a `MERGE-CHECKLIST.md` record |
| FREE-03 | Whether upstream issue #580 subsumes P3-WR-02 checked and recorded | Pitfall 7 — verbatim quote of `describeTableV2`'s parameter usage confirming the fix is present |
</phase_requirements>

## Summary

This phase has almost no open design questions left — `07-CONTEXT.md`'s 19 decisions (D-01..D-19) already specify what to build, where, and why, to a granularity the planner can lift into tasks directly. This RESEARCH.md's job is narrower and mechanical: independently re-verify every load-bearing claim in that context against the actual working tree (branch `sync/v1.6.0`, currently `git diff`-identical to upstream tag `v1.6.0` for every `src/main` file this phase touches — confirmed this session, not assumed) and hand the planner exact, quoted, line-numbered current-state code plus the fork's pre-merge target shape for each of the six re-expressions.

Every one of `git diff v1.6.0 HEAD -- <path>` for `ClickHouseSinkConfig.java`, `ClickHouseSinkTask.java`, `ProxySinkTask.java`, `ClickHouseWriter.java`, `ClickHouseHelperClient.java`, `Column.java`, `Table.java`, `Type.java` returned **zero lines** this session — the working tree really is pure upstream `v1.6.0` in `src/main`, exactly as D-19 asserts. That means every fork behavior this phase re-applies lands as a clean, isolated diff against a known-good upstream baseline; there is no textual merge conflict to resolve anywhere in this phase's scope, only additive re-expression. The one and only `compileJava` error was reproduced independently this session (`env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileJava`): `ClickHouseSinkTask.java:149: error: method does not override or implement a method from a supertype` on `@Override public void onPartitionsRevoked(...)`, and independently via `javap` against the resolved `connect-api-4.1.1.jar` (`org.apache.kafka.connect.sink.SinkTask` has no `onPartitionsRevoked`/`onPartitionsAssigned` member at all in that jar) — D-01/D-02's fix (delete the `@Override` annotation only) is confirmed correct and sufficient to unblock `compileJava`.

The six re-expressions, in the dependency order the planner should sequence them (matching D-19's commit shape): (1) `Type.DYNAMIC` + `Column`'s `Dynamic` dispatch, (2) RowBinary fail-fast (`validateDataSchema` gate + `doWriteColValue` default-case throw), (3) `targetTableFilter` server-side filtering, (4) `shouldSkipDescribeRow` re-expressed over a local `Map<String, Column>` + UTF-8 reader, (5) config keys (`TARGET_TABLE_FILTER`, `JSON_INSERT_COLUMNS`) + `async_insert` un-forcing + CFG-03's defaults test, (6) the four non-behavioral log-level/message tweaks (D-16) plus FREE-02/FREE-03 recording. This session independently confirmed the exact current line numbers, verbatim current content, and the fork's pre-merge (`pre-v1.6.0-sync`) target implementation for all six.

**Primary recommendation:** Treat `07-CONTEXT.md` as the design document and this file as its evidence base — port the fork's pre-merge implementations (quoted below, from `git show pre-v1.6.0-sync:<path>`) onto the exact attachment points quoted below (from the current working tree), one behavior per commit, compiling after each.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Type-string → `Type` enum dispatch (`Dynamic`) | Type/Mapping layer (`sink.db.mapping`) | — | Pure data-shape mapping, no I/O; `Column.extractColumn` is the single dispatch point |
| RowBinary fail-fast for unsupported types | Writer (`sink.db.ClickHouseWriter`) | Type/Mapping layer (reads `Type` enum) | Writer owns serialization; must reject before any bytes reach the stream |
| Server-side table-name filtering (`targetTableFilter`) | Helper Client (`sink.db.helper.ClickHouseHelperClient`) | Config (`ClickHouseSinkConfig` supplies the regex) | Helper client owns all outbound SQL construction against ClickHouse |
| DESCRIBE-row subcolumn skip (`shouldSkipDescribeRow`) | Helper Client | Type/Mapping layer (`Column`/`Table` receive the filtered result) | Same describe loop as the filter; must run before `Column.extractColumn` |
| UTF-8 describe readers | Helper Client | — | Charset correctness for the V2 HTTP response stream, isolated to `describeTableV2` |
| Config keys + parsing (`targetTableFilter`, `jsonInsertColumns`, `async_insert`) | Config (`ClickHouseSinkConfig`) | — | Single source of truth for connector settings; Writer/Helper Client only consume already-parsed fields |
| Compile-blocker fix (`onPartitionsRevoked`) | Orchestration (`sink.ClickHouseSinkTask`) — out of this phase's *design* scope, in its *unblock* scope per D-02/D-03 | — | One-line annotation deletion; owned by Phase 8 for everything else in the file |

## Standard Stack

**Not applicable to this phase.** No new external dependency is introduced — every change is a re-expression of existing fork logic (already present at `pre-v1.6.0-sync`) onto upstream `v1.6.0`'s rewritten classes, using only `java.nio.charset.StandardCharsets`, `org.apache.kafka.connect.errors.DataException`, and other libraries already on the classpath. `build.gradle.kts` and `gradle/libs.versions.toml` are Phase 6's completed scope and are not touched here.

## Package Legitimacy Audit

**Not applicable — no packages introduced or upgraded in this phase.** Skip the Package Legitimacy Gate; there is nothing to run `gsd_run query package-legitimacy check` against.

## Architecture Patterns

### System Architecture Diagram

```
ClickHouseSinkConfig (parses connector properties)
        │  targetTableFilter, jsonInsertColumns, jsonInsertFormatExplicit
        │  async_insert stays absent from clickhouseSettings map
        ▼
ClickHouseWriter.start()
        │  builds ClickHouseHelperClient via ClickHouseClientBuilder
        │    .setTargetTableFilter(csc.getTargetTableFilter())   ← WRT-05/HLP-01/CFG-01 chain
        ▼
ClickHouseHelperClient
        │
        ├─ showTables(database) ──► showTablesV1/V2(database)
        │     SELECT database, table, count(*) FROM system.columns
        │       WHERE database = '<db>' [AND match(table, '<targetTableFilter>')]
        │       GROUP BY database, table
        │     returns List<Table.TableDesc>                       ← HLP-01
        │
        └─ describeTable(database, tableName) ──► describeTableV1/V2(database, tableName)
              per DESCRIBE TABLE row:
                numColumns++ (unconditional, top-level only)       ← HLP-03 (already correct upstream)
                skip if alias/materialized/ephemeral                (upstream, unchanged)
                skip if shouldSkipDescribeRow(descriptor, columnsSoFarByName)   ← HLP-02 (fork, to re-add)
                  (.null / .size0 / bare .size / child of JSON|DYNAMIC parent)
                Column.extractColumn(descriptor)                    ← MAP-01/02
                  "Dynamic..." → Type.DYNAMIC   (NEW — this phase)
                  "JSON..."    → Type.JSON      (upstream, unchanged)
              new Table(database, name, hasDefaults, columns, numColumns)   ← MAP-03 (upstream ctor, adopted as-is)
              V2 path: BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8))  ← HLP-04 (add charset)
        ▼
ClickHouseWriter.doInsertBatch() / validateDataSchema() / doWriteColValue()
        │  validateDataSchema: switch(colTypeName) case "DYNAMIC": case "UNKNOWN": → throw DataException   ← WRT-02
        │  doWriteColValue default: → throw DataException (was: LOGGER.error, no throw)                    ← WRT-01
        │  case JSON: unchanged upstream flag-gated write-as-string / RuntimeException                     ← WRT-03 (adopt as-is)
        ▼
ClickHouse server (RowBinary / JSONEachRow insert)
```

### Recommended "Files Touched" Structure

```
src/main/java/com/clickhouse/kafka/connect/sink/
├── db/mapping/Type.java                 # + DYNAMIC constant (MAP-01)
├── db/mapping/Column.java               # + startsWith("Dynamic") branch after JSON (MAP-01/02)
├── db/mapping/Table.java                # NO EDIT — call sites elsewhere rewritten (MAP-03)
├── db/ClickHouseWriter.java             # validateDataSchema gate, doWriteColValue throw,
│                                         #   start()'s .setTargetTableFilter(...), log tweaks (WRT-01..05, D-16)
├── db/helper/ClickHouseHelperClient.java  # buildTableFilterClause, shouldSkipDescribeRow,
│                                         #   showTables* filter wiring, UTF-8 reader, log tweaks (HLP-01..04, D-16)
├── ClickHouseSinkConfig.java             # TARGET_TABLE_FILTER / JSON_INSERT_COLUMNS keys+fields,
│                                         #   comment out async_insert forcing (CFG-01/02)
└── ClickHouseSinkTask.java               # delete @Override only, line 149 (D-02, unblock-compile)
```

### Pattern 1: Additive enum + `startsWith` dispatch branch (MAP-01/MAP-02)

**What:** Add one enum constant and one `else if` branch to an existing `startsWith`-chain dispatcher — no structural change.
**When to use:** `Type.java` and `Column.java`'s `dispatchPrimitive` only; this is the pattern's full scope for this phase.
**Current state (verified — `Read` this session, upstream `v1.6.0`, byte-identical to working tree):**

`src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Type.java` — full file, 35 lines. Last two enum constants:
```java
    Enum8,
    Enum16,
    JSON
}
```
No `DYNAMIC` constant exists. `Column.java:163-178` (verbatim, `Read` this session):
```java
            default:
                if (valueType.startsWith("DateTime64")) {
                    // Need to understand why DateTime64(3)
                    type = Type.DateTime64;
                } else if (valueType.startsWith("Decimal")) {
                    // FIXME: Map keys doesn't support Decimal:
                    //        Type of Map key must be a type, that can be represented by integer or String or FixedString (possibly LowCardinality) or UUID or IPv6, but Decimal(5, 0) given.
                    type = Type.Decimal;
                } else if (valueType.startsWith("FixedString")) {
                    type = Type.FIXED_STRING;
                } else if (valueType.startsWith("JSON")) {
                    // There are some parameters that we ignore because server will handle them for us
                    type = Type.JSON;
                }

                break;
```

**Fork's pre-merge target (verified — `git show pre-v1.6.0-sync:...Column.java` this session, lines 148-156):**
```java
                    type = Type.Decimal;
                } else if (valueType.startsWith("FixedString")) {
                    type = Type.FIXED_STRING;
                } else if (valueType.startsWith("JSON")) {
                    type = Type.JSON;
                } else if (valueType.startsWith("Dynamic")) {
                    type = Type.DYNAMIC;
                }

                break;
```
Fork's `Type.java` (pre-merge) ends `..., JSON, DYNAMIC }`. **Re-express:** add `DYNAMIC` after `JSON` in `Type.java`, and add exactly the `else if (valueType.startsWith("Dynamic")) { type = Type.DYNAMIC; }` branch to `Column.java`'s existing `else if` chain — no other change to either file (MAP-02's `SimpleAggregateFunction` unwrapping and Avro/Protobuf union handling are already upstream's, unmodified, elsewhere in `Column.java`; confirmed present via the file's `SIMPLE_AGGREGATE_FUNCTION_TYPE_PATTERN` field and `AVRO_UNION_SCHEMA_NAME`/`PROTOBUF_UNION_SCHEMA_PREFIX` constants, `Read` this session, lines 38-48).

**Canary that currently fails to compile without this change** — `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/JsonDynamicTypeMappingTest.java` (`Read` this session) references `Type.DYNAMIC` directly in two `@ParameterizedTest`s and one `@Test` (`dynamicTypeStringsMapToDynamic`, `dynamicTypeDispatchIgnoresNonAsciiColumnName`) — this file will not compile until `Type.DYNAMIC` exists. `ColumnTest.java` (`Read`/`Grep` this session) has **zero** existing `Dynamic`/`DYNAMIC` references — it does not currently pin this behavior at all; `JsonDynamicTypeMappingTest` is the sole canary.

### Pattern 2: RowBinary fail-fast — two attachment points sharing one message builder (WRT-01/WRT-02/WRT-03)

**What:** A private message-builder method plus two call sites: an early `validateDataSchema` gate (fails before any bytes are written) and `doWriteColValue`'s `default:` case (last-resort guard if the gate is ever bypassed, e.g. `bypassSchemaValidation=true`).
**When to use:** Both must throw the *same* `DataException` message shape so the two gates cannot drift in wording (locked in the fork's original Javadoc, reproduced below).

**Current state, `validateDataSchema`'s `switch(colTypeName)`** (`Read` this session, `ClickHouseWriter.java:291-300`, verbatim):
```java
                    switch (colTypeName) {
                        case "Date":
                        case "Date32":
                        case "DateTime":
                        case "DateTime64":
                        case "UUID":
                        case "FIXED_STRING":
                        case "Enum8":
                        case "Enum16":
                            break;//I notice we just break here, rather than actually validate the type
```
No `case "DYNAMIC":`/`case "UNKNOWN":` exists — both fall through to `default:` (line 318), which does a bit-width comparison (`CLICKHOUSE_TYPE_BIT_WIDTHS.get(type)` — returns `null` for `DYNAMIC`/`UNKNOWN`, since neither is a key in that map) and ends by setting `validSchema = false` with only a `LOGGER.error` — no exception. Callers (`ClickHouseWriter.java:1057`, `:1134`, both `Read` this session, verbatim) do:
```java
        if (!csc.isBypassSchemaValidation()
                && first.getSchemaType() != SchemaType.DEBEZIUM_CDC
                && !validateDataSchema(table, first, false))
            throw new RuntimeException("Data schema validation failed.");
```
So today a `DYNAMIC`/`UNKNOWN` column reaching `validateDataSchema` produces a **generic, unactionable** `RuntimeException("Data schema validation failed.")` — not the fork's specific `DataException` naming the column and type.

**`doWriteColValue`'s `default:` case** (`Read` this session, `ClickHouseWriter.java:669-673`, verbatim — the exact line CONTEXT.md's canonical_refs cites):
```java
                default:
                    // If you wonder, how NESTED works in JDBC:
                    // https://github.com/ClickHouse/clickhouse-java/blob/6cbbd8fe3f86ac26d12a95e0c2b964f3a3755fc9/clickhouse-data/src/main/java/com/clickhouse/data/format/ClickHouseRowBinaryProcessor.java#L159
                    LOGGER.error("Cannot serialize unsupported type {}", columnType);
```
No throw — confirmed still the silent-corruption path Phase 3 of the previous milestone fixed.

**Upstream's own `case JSON:`, immediately above the `default:`** (`Read` this session, `ClickHouseWriter.java:651-668`, verbatim):
```java
                case JSON:
                    if (value.getObject() == null) {
                        BinaryStreamUtils.writeNull(stream);
                        break;
                    }
                    if (csc.isBinaryFormatWrtiteJsonAsString()) {
                        if (value.getFieldType() == Schema.Type.STRUCT) {
                            byte[] jsonBytes = OBJECT_MAPPER.writeValueAsBytes(value);
                            BinaryStreamUtils.writeString(stream, jsonBytes);
                        } else if (value.getFieldType() == Schema.Type.STRING) {
                            BinaryStreamUtils.writeString(stream, ((String) value.getObject()).getBytes(StandardCharsets.UTF_8));
                        } else {
                            throw new RuntimeException("Unsupported field type: " + value.getFieldType() + " for column type [JSON]");
                        }
                        break;
                    } else {
                        throw new RuntimeException("Writing JSON in binary is not supported yet. Use `input_format_binary_read_json_as_string=1` in clickhouse settings to allow writing as string");
                    }
```
Note the method name typo is upstream's own — `isBinaryFormatWrtiteJsonAsString()` (confirmed exact spelling, `Read` this session) — grep for the misspelling, not "Write", if searching this codebase later. **This confirms WRT-03: adopt as-is, no change.** `validateDataSchema` at `ClickHouseWriter.java:351-358` (`Read` this session) already has a matching `if (type == Type.JSON) { if (csc.isBinaryFormatWrtiteJsonAsString() && ...) continue; }` branch — upstream's own JSON handling is present at both gates already; only `DYNAMIC`/`UNKNOWN` are missing.

**Fork's pre-merge target for both attachment points** (`git show pre-v1.6.0-sync:...ClickHouseWriter.java`, this session):

Message builder (fork's pre-merge lines 225-235):
```java
    /**
     * Builds the D-05-worded, actionable message shared by the {@code doWriteColValue} default
     * branch and the {@code validateDataSchema} JSON/DYNAMIC/UNKNOWN case (TYPE-06), so the two
     * gates cannot drift apart in wording. Names only the column, its type, and the two escape
     * hatches - never any part of the record payload (T-03-21).
     */
    private String buildRowBinaryUnsupportedTypeMessage(String columnName, Type columnType) {
        return String.format(
                "Column [%s] has unsupported type [%s] for RowBinary serialization. "
                        + "Use insertFormat=json or bypassRowBinary=true instead.",
                columnName, columnType);
    }
```
`validateDataSchema` gate (fork's pre-merge lines 267-282 — **note**: per D-WRT-02 discretion, drop the `"JSON"` case label from this re-expression; upstream's `case JSON:` at the write path plus the existing `if (type == Type.JSON)` branch in this same switch's `default:` already own JSON):
```java
                        case "JSON":
                        case "DYNAMIC":
                        case "UNKNOWN":
                            // ... (rationale comment, port forward, drop "JSON" per WRT-02 discretion)
                            String unsupportedTypeMsg = buildRowBinaryUnsupportedTypeMessage(colName, type);
                            LOGGER.error(unsupportedTypeMsg);
                            throw new DataException(unsupportedTypeMsg);
```
`doWriteColValue` default (fork's pre-merge lines 562-568, unchanged shape, re-attach after upstream's `case JSON:` block instead of immediately after `case ARRAY:`):
```java
            default:
                // If you wonder, how NESTED works in JDBC: ...
                String msg = buildRowBinaryUnsupportedTypeMessage(col.getName(), columnType);
                LOGGER.error(msg);
                throw new DataException(msg);
```

**Canary, currently non-compiling against the new `Table`:** `src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java` (`Read`/`Grep` this session) — uses `new Table("test_db", "test_table")` (2-arg, gone, line 88) and asserts a `Type.JSON` column throws `DataException` with the fork's message (lines 39-48) — **this JSON assertion must be rewritten per WRT-02's discretion** to expect upstream's `RuntimeException` (`"Writing JSON in binary is not supported yet. Use ...`", the flag-off default) instead, since `"JSON"` is dropped from the gate and upstream's `case JSON:` now owns that path exclusively.

### Pattern 3: `targetTableFilter` re-threaded onto `showTables()`'s new `List<Table.TableDesc>` return type (WRT-05/HLP-01/CFG-01)

**Current state — `showTables`/`showTablesV1`/`showTablesV2`** (`Read` this session, `ClickHouseHelperClient.java:333-377`, verbatim):
```java
    public List<Table.TableDesc> showTables(String database) {
        if (useClientV2) {
            return showTablesV2(database);
        } else {
            return showTablesV1(database);
        }
    }

    public List<Table.TableDesc> showTablesV1(String database) {
        List<Table.TableDesc> tables = new ArrayList<>();
        try (ClickHouseClient client = ClickHouseClient.builder()
                .options(getDefaultClientOptions())
                .nodeSelector(ClickHouseNodeSelector.of(ClickHouseProtocol.HTTP))
                .build();
             ClickHouseResponse response = client.read(server)
                     .query(String.format("select database, table, count(*) as col_count from system.columns where database = '%s' group by database, table", database))
                     .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
                     .executeAndWait()) {
            for (ClickHouseRecord r : response.records()) {
                String databaseName = r.getValue(0).asString();
                String tableName = r.getValue(1).asString();
                int colCount = r.getValue(2).asInteger();
                tables.add(new Table.TableDesc(databaseName, tableName, colCount));
            }
        } catch (ClickHouseException e) {
            LOGGER.error("Failed in show tables", e);
        }
        return tables;
    }

    public List<Table.TableDesc> showTablesV2(String database) {
        List<Table.TableDesc> tablesList = new ArrayList<>();
        try (Records records = queryV2(String.format("select database, table, count(*) as col_count from system.columns where database = '%s' group by database, table", database))) {
            for (GenericRecord record : records) {
                String databaseName = record.getString(1);
                String tableName = record.getString(2);
                int colCount = record.getInteger(3);
                LOGGER.debug("table name: {}", tableName);
                tablesList.add(new Table.TableDesc(databaseName, tableName, colCount));
            }
        } catch (Exception e) {
            LOGGER.error("Failed in show tables", e);
        }
        return tablesList;
    }
```
No `targetTableFilter` field exists on the class (`Grep` this session, zero hits) and neither `showTablesV1`/`showTablesV2` takes a pre-built query string — both now build their own `String.format(...)` inline and take `database` directly, confirming Architecture research's Deep-dive #1 claim exactly.

**Fork's pre-merge target** (`git show pre-v1.6.0-sync:...ClickHouseHelperClient.java`, this session, verbatim):
```java
    /**
     * Builds the {@code match(table, '...')} clause appended to the {@code system.columns} query
     * that backs {@link #showTables(String)}. Returns the empty string when {@code targetTableFilter}
     * is exactly the empty string (the connector default); otherwise doubles any single quote in the
     * filter before embedding it in the SQL literal. No backslash handling, no trim: this is a
     * byte-for-byte extraction of the previous inline ternary (D-19/FILT-01/FILT-03/FILT-04).
     */
    static String buildTableFilterClause(String targetTableFilter) {
        return targetTableFilter.isEmpty()
                ? ""
                : String.format(" AND match(table, '%s')", targetTableFilter.replace("'", "''"));
    }
```
Fork's pre-merge `showTables`/`showTablesV1`/`showTablesV2` built ONE query string up front (`showTables` built `filterClause` + full SQL, then called `showTablesV1(query)`/`showTablesV2(query)` — the *query string*, not `database`). **07-CONTEXT.md's Architecture deep-dive recommends the opposite of the fork's original shape** ("inject `buildTableFilterClause(targetTableFilter)` into each of the two inline `String.format` calls" — matches upstream's now-per-method-inline-query structure, smaller diff against a file upstream is more likely to keep touching). Concretely: append `+ buildTableFilterClause(targetTableFilter)` inside each of `showTablesV1`'s and `showTablesV2`'s own `String.format(...)` call (both currently end `"...group by database, table"` with no filter clause) rather than restoring the fork's `showTables(String query)` single-build-point shape.

Field + builder setter (fork's pre-merge, `git show pre-v1.6.0-sync`, verbatim):
```java
    private final String targetTableFilter;         // instance field, set from builder.targetTableFilter
    // ...
        private String targetTableFilter = "";        // ClickHouseClientBuilder field, default ""
    // ...
        public ClickHouseClientBuilder setTargetTableFilter(String targetTableFilter) {
            this.targetTableFilter = targetTableFilter;
            return this;
        }
```
**Current builder** (`Read` this session, `ClickHouseHelperClient.java:663-751`) has no `targetTableFilter` field and no `setTargetTableFilter` method — add both, following the exact pattern of the adjacent `setSslSocketSni`/`setClusterClause` methods already present (lines 732-745).

**`ClickHouseWriter.start()`'s builder chain** currently ends at `.enableReplicaPinning(csc.isEnableReplicaPinning()).build();` (`Read` this session, `ClickHouseWriter.java:84-96`) — no `.setTargetTableFilter(...)` call. Add it to this chain (WRT-05), reading `csc.getTargetTableFilter()` (CFG-01's new field).

**Canary, non-compiling:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java` extends `ClickHouseBase` and exercises `targetTableFilter` end-to-end — Docker-tagged, not part of `testUnit`, but must still compile for `compileTestJava`.

### Pattern 4: `shouldSkipDescribeRow` re-expressed over a local `Map<String, Column>`, not a `Table` (HLP-02, D-08)

**Current state — both describe loops** (`Read` this session, `ClickHouseHelperClient.java:387-508`, both verbatim in full above and below). Key structural fact for D-08: **there is no `Table` object during the loop** — `columns` is a bare `List<Column>` (V1: line 406; V2: line 464), and `new Table(database, tableName, hasDefaults, columns, numColumns)` is constructed only once, after the loop (V1: line 443; V2: line 503). `Table.getAllColumnsMap()` (the fork's original lookup target, `Table.java:33`, `Read` this session) is a `@Getter`-exposed instance field populated incrementally by `addColumn` — **not accessible mid-loop against the new shape**, confirming D-08's `shouldSkipDescribeRow(ClickHouseFieldDescriptor, Map<String, Column>)` re-signature is required, not optional.

`numColumns` increment ordering, V1 (`ClickHouseHelperClient.java:411-414`, verbatim) and V2 (`:471-474`, verbatim, identical shape):
```java
                // Count what system.columns counts: top-level columns, including the ones skipped below
                if (!fieldDescriptor.isSubcolumn()) {
                    numColumns++;
                }

                if (fieldDescriptor.isAlias() || fieldDescriptor.isMaterialized() || fieldDescriptor.isEphemeral()) {
```
The increment already happens **before** the alias/materialized/ephemeral skip, for every top-level (non-subcolumn) row, unconditionally — **HLP-03 requires no code change**, only that the new fork skip (which only ever applies to subcolumns, since `shouldSkipDescribeRow` returns `false` immediately for `!fieldDescriptor.isSubcolumn()`) is inserted *after* this point, which it structurally must be since it needs the `Column`s already added to the map/list so far.

**Fork's pre-merge `shouldSkipDescribeRow`** (`git show pre-v1.6.0-sync:...ClickHouseHelperClient.java`, this session, verbatim):
```java
    /**
     * Decides whether a DESCRIBE TABLE row should be skipped before it reaches {@link Column#extractColumn}.
     * Root (non-subcolumn) rows are never skipped. A subcolumn is skipped when its name ends with
     * {@code .null}, {@code .size0} or the bare {@code .size} suffix (D-20), or when it is a child of a
     * column whose resolved type is {@link Type#JSON} or {@link Type#DYNAMIC} (D-07's structural rule,
     * which replaces the previous {@code .object_shared_data.} literal clause). Map and Tuple subcolumns
     * are intentionally left un-skipped so {@link Table#addColumn} keeps routing them to
     * {@code updateParent} (DESC-01..03).
     */
    static boolean shouldSkipDescribeRow(ClickHouseFieldDescriptor fieldDescriptor, Table table) {
        if (!fieldDescriptor.isSubcolumn()) {
            return false;
        }
        String name = fieldDescriptor.getName();
        if (name.endsWith(".null") || name.endsWith(".size0") || name.endsWith(".size")) {
            return true;
        }
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0) {
            return false;
        }
        String parentName = name.substring(0, lastDot);
        Column parent = table.getAllColumnsMap().get(parentName);
        return parent != null && (parent.getType() == Type.JSON || parent.getType() == Type.DYNAMIC);
    }
```
**Re-express** replacing `Table table` with `Map<String, Column> columnsSoFar`, and `table.getAllColumnsMap().get(parentName)` with `columnsSoFar.get(parentName)` — everything else unchanged. Both `describeTableV1`/`V2` already maintain the ingredient this needs: build an incremental `Map<String, Column> columnsByName` alongside the existing `List<Column> columns` (or reuse `columns` with a linear scan — D-08 explicitly rejects the linear scan in favor of the O(1) map).

**Insert point:** in both loops, immediately after the existing alias/materialized/ephemeral `continue` block (V1 ends at line 428, V2 at line 488) and before `Column column = Column.extractColumn(fieldDescriptor);` (V1 line 434, V2 line 494) — call `shouldSkipDescribeRow(fieldDescriptor, columnsByName)` and `continue` (adding to `skippedCols` too, matching the existing pattern) if it returns `true`; otherwise proceed to `extractColumn` and put the result into both `columns` and `columnsByName`.

**Canary, non-compiling against old signature:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java` (`Read`/`Grep` this session) — **11 confirmed call sites** using `new Table("db", "t1")` (2-arg, gone) and `ClickHouseHelperClient.shouldSkipDescribeRow(descriptor, table)` (`Table`-typed second arg). Every site must be rewritten to build a `Map<String, Column>` directly instead of a `Table` (matching D-08's plan exactly — e.g. line 114's `new Table("db", "t1"); // parent "json_col" intentionally never added` becomes an empty `Map<String, Column>`).

### Pattern 5: UTF-8 describe reader (HLP-04, D-17)

**Current state** (`Read` this session, `ClickHouseHelperClient.java:465-466`, verbatim — exact line CONTEXT.md cites):
```java
            try (QueryResponse queryResponse = client.query(describeQuery, settings).get();
                 BufferedReader br = new BufferedReader(new InputStreamReader(queryResponse.getInputStream()))) {
```
No charset argument — confirmed the platform-default-charset bug is still present, unfixed, in upstream `v1.6.0`. `describeTableV1` (`Read` this session, full method `:387-448`) never constructs an `InputStreamReader` at all — it parses via `ClickHouseValue.asString()` from a `ClickHouseRecord` (line 408-410) — confirming D-17's "V1 never used a raw reader" claim; no V1 change is needed or possible at this call shape.

**Re-express:** `new InputStreamReader(queryResponse.getInputStream(), StandardCharsets.UTF_8)`. `java.nio.charset.StandardCharsets` is not currently imported in this file (`Grep` this session confirms the file's `import java.io.BufferedReader; import java.io.InputStreamReader;` block at lines 34-35 has no `StandardCharsets` import) — add it.

**Grep-verification requirement (HLP-04's exact wording):** after the edit, `grep -n "InputStreamReader(" src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` currently returns **exactly one match** (line 466) — confirmed this session. Re-run the same grep after the fix and confirm the sole match now specifies `StandardCharsets.UTF_8`; this is the phase's own acceptance check for HLP-04, not a hypothetical.

### Pattern 6: Config keys, `async_insert` un-forcing, and the thirteen-defaults test (CFG-01/02/03)

**`async_insert` forcing — current state** (`Read` this session, `ClickHouseSinkConfig.java:238-240`, verbatim — exact line CONTEXT.md cites):
```java
        this.addClickHouseSetting("input_format_skip_unknown_fields", "1", false);
        this.addClickHouseSetting("wait_end_of_query", "1", false);
        this.addClickHouseSetting("async_insert", "0", false);
```
**Re-express:** comment out line 240 exactly (`//this.addClickHouseSetting("async_insert", "0", false);`), matching the fork's pre-merge form verbatim (`git show pre-v1.6.0-sync:...ClickHouseSinkConfig.java` line 209, confirmed this session: `//this.addClickHouseSetting("async_insert", "0", false);`).

**`TARGET_TABLE_FILTER`/`JSON_INSERT_COLUMNS` — confirmed absent from the current file** (`Grep` this session, zero hits for `TARGET_TABLE_FILTER|JSON_INSERT_COLUMNS|targetTableFilter|jsonInsertColumns|jsonInsertFormatExplicit` anywhere in `ClickHouseSinkConfig.java`). Fork's pre-merge constants/fields/parsing/`ConfigDef.define` blocks (`git show pre-v1.6.0-sync:...ClickHouseSinkConfig.java`, this session, verbatim):
```java
    public static final String JSON_INSERT_COLUMNS = "jsonInsertColumns";
    // ...
    public static final String TARGET_TABLE_FILTER = "targetTableFilter";
    // ... (field declarations)
    private final String targetTableFilter;
    private final boolean jsonInsertFormatExplicit;
    private final List<String> jsonInsertColumns;
    // ... (parsing, inside the constructor)
    this.jsonInsertFormatExplicit = insertFormatRaw != null && insertFormatRaw.trim().equalsIgnoreCase("json");
    String jsonInsertColumnsRaw = props.getOrDefault(JSON_INSERT_COLUMNS, "").trim();
    List<String> jsonInsertColumnsTmp = new ArrayList<>();
    if (!jsonInsertColumnsRaw.isBlank()) {
        String[] stringSplit = jsonInsertColumnsRaw.split(",");
        for (String s : stringSplit) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                jsonInsertColumnsTmp.add(trimmed);
            }
        }
    }
    this.jsonInsertColumns = jsonInsertColumnsTmp;
    // ...
    this.targetTableFilter = props.getOrDefault(TARGET_TABLE_FILTER, "").trim();
    // ... (ConfigDef.define blocks)
    configDef.define(JSON_INSERT_COLUMNS,
            ConfigDef.Type.STRING, "", ConfigDef.Importance.LOW,
            "Comma-separated list of column names to use for JSON inserts. Whitespace is ignored.",
            group, ++orderInGroup, ConfigDef.Width.LONG, "JSON insert columns."
    );
    configDef.define(TARGET_TABLE_FILTER,
            ConfigDef.Type.STRING, "", ConfigDef.Importance.LOW,
            "Optional filter for destination table names using a regex pattern.",
            group, ++orderInGroup, ConfigDef.Width.MEDIUM, "Target Table Filter (regex pattern).");
```
Re-apply verbatim (D-13: "Both config keys and all their parsing land in Phase 7... `jsonInsertColumns` and `jsonInsertFormatExplicit` are briefly read by nothing... that is a one-phase gap, not a defect").

**Confirmed non-compiling today without this change:** `src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsUnitTest.java` (`Read` this session) directly references `ClickHouseSinkConfig.JSON_INSERT_COLUMNS` and `ClickHouseSinkConfig.CLICKHOUSE_SETTINGS` in its `@Test` bodies — cannot compile until the key exists.

**CFG-03's thirteen options** — cross-checked this session, all thirteen constant names confirmed present via `Grep` of `ClickHouseSinkConfig.java` (byte-identical to `v1.6.0` per the earlier whole-file diff): `auto.evolve` (`AUTO_EVOLVE`, line 63), `auto.evolve.ddl.refresh.retries` (`AUTO_EVOLVE_DDL_REFRESH_RETRIES`, line 64), `auto.evolve.struct.to.json` (`AUTO_EVOLVE_STRUCT_TO_JSON`, line 65), `bufferCount` (`BUFFER_COUNT`, line 56), `bufferFlushTime` (`BUFFER_FLUSH_TIME`, line 57), `clickhouseClientInsertTimeoutMs` (`CLICKHOUSE_CLIENT_INSERT_TIMEOUT_MS`, line 58), `clusterName` (`CLUSTER_NAME`, line 67), `debeziumCDCEnabled` (`DEBEZIUM_CDC_ENABLED`, line 60), `enableReplicaPinning` (`ENABLE_REPLICA_PINNING`, line 68), `errors.retry.timeout` (`CONNECTOR_RETRY_TIMEOUT`, line 66), `reportInsertedOffsets` (`REPORT_INSERTED_OFFSETS`, line 59), `retryOnSocketException` (`RETRY_ON_SOCKET_EXCEPTION`, line 39), `ssl_socket_sni` (`SSL_SOCKET_SNI`, line 71). The class carries a class-level `@Getter` (`ClickHouseSinkConfig.java:20`, `Read` this session) — Lombok auto-generates all field getters (`isAutoEvolve()`, `getBufferCount()`, `isReportInsertedOffsets()`, etc.) from the `private final` fields; CFG-03's defaults test should call these generated getters directly, not re-derive names.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| RowBinary fail-fast message wording | A new message format for this phase | The fork's exact pre-merge `buildRowBinaryUnsupportedTypeMessage` (quoted above) | Prevents the two gates (`validateDataSchema`, `doWriteColValue`) from drifting in wording — this was the whole point of extracting it as a shared method originally |
| `targetTableFilter` SQL clause construction | A hand-rolled `Pattern`-based Java-side filter | ClickHouse's own `match(table, '<regex>')`, server-side, via `buildTableFilterClause` | Filtering server-side avoids pulling every table's column count over the wire just to discard rows client-side; `match()` uses RE2 semantics, not `java.util.regex` — testing must go through a live server (`FILT-01..04`), not `Pattern` |
| Subcolumn detection for `.null`/`.size0`/`.size` | A regex against `Table.SIZE_FIELD_MATCHER` extended to also catch bare `.size` | `shouldSkipDescribeRow`'s explicit `name.endsWith(".size")` check, kept separate from `Table.SIZE_FIELD_MATCHER` | `Table.SIZE_FIELD_MATCHER` upstream is `.+\.size[0-9]+$` (`Table.java:24`, `Read` this session) — it does **not** match a bare `.size` with no trailing digit; conflating the two regexes risks silently breaking the fix for the case that needed it |
| CFG-03's defaults verification | A test that inspects `ConfigDef` metadata for each of the 13 keys | A parametrized test constructing a default `ClickHouseSinkConfig` and asserting each Lombok-generated getter | The requirement is "reach their consumers at upstream defaults" — a getter assertion tests the actual consumed value, a `ConfigDef` metadata check only tests the declared default string |

**Key insight:** every "don't hand-roll" item in this phase is really "don't re-derive from first principles — the fork already built and tested this exact logic at `pre-v1.6.0-sync`; port it forward verbatim onto the new attachment points." The risk in this phase is not invention, it's transcription drift.

## Common Pitfalls

### Pitfall 1: Losing the `throw` while re-attaching `doWriteColValue`'s default case
**What goes wrong:** The default case is a two-line diff sitting immediately below upstream's ~20-line, newly-added `case JSON:` block. It is easy to re-type the comment and the `LOGGER.error` call from memory and forget the `throw new DataException(msg);` — exactly reproducing the bug this phase exists to fix.
**Why it happens:** The surrounding switch is large and mostly-unfamiliar upstream code; the fix itself is small and easy to lose inside it.
**How to avoid:** Treat "the default case throws" as its own named checklist item verified by `RowBinaryUnsupportedTypeGateTest`'s `Type.DYNAMIC`/`Type.UNKNOWN` cases specifically, not just "the switch compiles."
**Warning signs:** `RowBinaryUnsupportedTypeGateTest` reports "no exception thrown" instead of `DataException`.

### Pitfall 2: Re-adding `"JSON"` to the `validateDataSchema` gate out of habit
**What goes wrong:** The fork's pre-merge gate had three case labels (`"JSON"`, `"DYNAMIC"`, `"UNKNOWN"`) sharing one throw. Copying the whole `case` block including `"JSON"` re-introduces a case label upstream's own `switch` doesn't need (upstream's `if (type == Type.JSON) { ... continue; }` already handles the JSON case a few lines below in the same `default:` branch) and — worse — makes RowBinary-JSON fail-fast again for the flag-on path upstream now supports, directly contradicting the locked "RowBinary + JSON: adopt upstream's flag-gated write-as-string" decision.
**Why it happens:** Copy-paste from the fork's pre-merge source without re-reading WRT-02's discretion note.
**How to avoid:** The re-expressed gate's case labels are `case "DYNAMIC": case "UNKNOWN":` only — verify by grep that `"JSON"` does not appear as a case label anywhere in the re-expressed switch.
**Warning signs:** `RowBinaryUnsupportedTypeGateTest`'s (rewritten) JSON assertion fails because a `DataException` is thrown where a `RuntimeException` (upstream's) is now expected.

### Pitfall 3: Attaching `shouldSkipDescribeRow`'s parent lookup to a `Table` instead of the loop-local map
**What goes wrong:** Every fork test and the fork's original method signature use `Table`. It is tempting to construct a throwaway `Table` mid-loop (`new Table(database, tableName, false, columns, numColumns)`) just to call `.getAllColumnsMap()` on it, rather than re-signing the method to take `Map<String, Column>` directly.
**Why it happens:** Minimizes the visible diff against the fork's original method signature.
**How to avoid:** D-08 is explicit and locked: re-sign to `shouldSkipDescribeRow(ClickHouseFieldDescriptor, Map<String, Column>)`. Constructing a `Table` per DESCRIBE row inside the loop is quadratic (rebuilds the whole column tree via `addColumn`'s `handleNonRoot`/`updateParent` dispatch on every row) and was explicitly rejected in D-08's decision text.
**Warning signs:** Noticeable `describeTable` latency regression on wide tables in later live testing (Phase 11); not caught by `testUnit` since it's a performance issue, not a correctness one.

### Pitfall 4: Assuming `numColumns` needs a code change for HLP-03
**What goes wrong:** Spending time modifying the increment-ordering logic in `describeTableV1`/`V2` when it is already correct.
**Why it happens:** HLP-03's requirement text ("numColumns is incremented before any skip decision") reads like something that needs fixing.
**How to avoid:** Confirmed this session (quoted above, `ClickHouseHelperClient.java:411-414` and `:471-474`) — the increment already happens unconditionally before the alias/materialized/ephemeral skip. HLP-03 is satisfied automatically as long as the new `shouldSkipDescribeRow` check is inserted *after* this point (which it structurally must be, since it needs `columnsByName` populated so far) — no separate code change to the increment itself.
**Warning signs:** None expected if the insertion point from Pattern 4 above is followed; a regression here would show as `extractTablesMapping`'s cache-staleness check (`ClickHouseHelperClient.java:562`, `cache.get(escapedTableName).getNumColumns() < tableDesc.getNumColumns()`) never triggering a re-describe when it should.

### Pitfall 5: Forgetting the UTF-8 grep is over the *whole resolved file*, not just the one known call site
**What goes wrong:** Patching line 466 and considering HLP-04 done, without re-running the grep across the entire file to confirm no *other* `InputStreamReader(` construction exists (e.g., introduced by a later upstream commit this research didn't specifically enumerate).
**Why it happens:** The historical fork fix touched exactly one call site, so "the one call site" becomes the mental model.
**How to avoid:** This session's grep (`grep -n "InputStreamReader(" ClickHouseHelperClient.java`) returned **exactly one match** at line 466 — confirmed the file's only occurrence. Re-run the identical grep as the phase's own acceptance step after the edit; do not rely on memory of "the two call sites the fork originally patched" (that phrasing describes the *previous* fork version, before upstream's rewrite collapsed the V1 raw-reader path away entirely — V1 no longer has one at all).
**Warning signs:** `TYPE-07`'s non-ASCII assertions pass locally (IDE default UTF-8) but fail specifically under `./gradlew test`'s `windows-1252` JVM setting — the documented false-positive trap from the previous milestone.

### Pitfall 6: Skipping D-16's non-behavioral log tweaks because "no test asserts a log level"
**What goes wrong:** Treating the four log-level/message changes (D-16) as optional polish and deferring them, since — correctly — no test in the suite currently asserts log output.
**Why it happens:** They're genuinely the lowest-risk, lowest-priority item in the phase.
**How to avoid:** They're locked decisions (D-16), not optional — land them in the same commit as their neighboring behavioral change (e.g., the `LOGGER.debug` retry-message tweak lands with the `targetTableFilter` commit since both touch `ClickHouseHelperClient.java`'s query-retry region) rather than a separate pass, per D-19's one-behavior-per-commit shape. `ClickHouseWriter.java:220`'s current text is confirmed this session: `"Trying to insert [{}] records to table name [{}] (QueryId: [{}])"` — add `schema type [{}]` per D-16, sourcing the value from `first.getSchemaType()` (already in scope at that call site, `ClickHouseWriter.java:219`).
**Warning signs:** None functional — this is a code-review-time miss, not a test failure.

### Pitfall 7: Treating FREE-02/FREE-03 as requiring a code change
**What goes wrong:** Re-deriving or re-applying the fork's `Map(K, Nullable(V))` null-marker patch, or "fixing" `describeTableV2`'s database parameter, because the requirement text sounds like a fix.
**Why it happens:** FREE-01/02/03 read like ordinary requirements in the traceability table, without the "verify and record, never fix" framing visible outside `REQUIREMENTS.md`'s own callout comment.
**How to avoid:** Both are **already correct upstream**, confirmed this session:
- `ClickHouseWriter.java:562` (verbatim): `if (col.getMapValueType() != null && col.getMapValueType().isNullable() && mapValue != null) { BinaryStreamUtils.writeNonNull(stream); }` — has the extra `!= null` guard the fork's version lacked. **No edit — record the fact.**
- `describeTableV2`'s query is built from the method's own `database` parameter (`ClickHouseHelperClient.java:453`, verbatim: `String.format("DESCRIBE TABLE \`%s\`.\`%s\`", database, tableName)`), not `this.database` — confirming P3-WR-02 is closed. **No edit — record the fact.**
Both belong in `MERGE-CHECKLIST.md § Recorded Facts` per D-18, not in a code diff.
**Warning signs:** A stray diff to either line in code review that isn't explained by one of the six named re-expressions above should prompt a "why is this changing?" question.

## Code Examples

All examples above (Patterns 1-6) are drawn verbatim from `git show`/`Read` this session and are the authoritative code examples for this phase — no additional synthetic examples are needed given the phase's re-expression (not greenfield-design) nature.

## State of the Art

| Old Approach (fork @ `pre-v1.6.0-sync`) | Current Approach (upstream `v1.6.0`, this working tree) | When Changed | Impact |
|---|---|---|---|
| `Table` built incrementally via public `addColumn()`, 2-arg constructor `Table(database, name)` | `Table` immutable, 5-arg constructor `Table(database, name, hasDefaults, columns, numColumns)`, `addColumn` private | Upstream `v1.6.0` | Every fork call site that built a `Table` (writer's now-dropped metadata-free path, all fork test files) needs the new constructor; no more mid-loop mutation |
| `showTables(database)` returned `List<Table>` (partially-built, names only) | `showTables(database)` returns `List<Table.TableDesc>` (purpose-built value object) | Upstream `v1.6.0` | Cache-staleness comparisons and any code consuming `showTables()`'s result must use `TableDesc.getCleanName()`/`getFullName()`/`getNumColumns()` |
| RowBinary JSON columns always fail-fast (fork's own `TYPE-06` fix) | RowBinary JSON columns fail-fast **unless** `csc.isBinaryFormatWrtiteJsonAsString()` is true, in which case a `Struct`/`String` value is written as a JSON string | Upstream `v1.6.0` (new capability) | `TYPE-06`'s JSON-specific assertions must be rewritten to upstream's exception type (`RuntimeException`) and message, per WRT-02's locked discretion |
| `Utils.handleException` only recognized `ClickHouseException` (dead code under client V2) | `isServerError()`/`getRootCause()` also recognize `com.clickhouse.client.api.ServerException` | Upstream, some version between `v1.2.9` and `v1.6.0` | Out of this phase's scope (FREE-01, Phase 8) but relevant context: `Utils.java` merged silently, zero diff |

**Deprecated/outdated:** The old `ClickHouseHelperClient.showTables(String query)`/`showTablesV1(String query)`/`showTablesV2(String query)` shape (fork pre-merge, query-string parameter) — do not restore this signature; the new `(String database)` signature with the filter clause built inline inside each method (per this file's Pattern 3) is the shape to build on, since it's the shape upstream is more likely to keep touching in future syncs.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `07-CONTEXT.md`'s decisions (D-01..D-19) accurately reflect a prior research/discussion session's investigation of this exact codebase, and were not independently re-derived from a different repo state | Throughout | Low — every claim quoted from CONTEXT.md in this file was independently re-verified this session via `git show`/`Read`/`Grep`/actual `gradlew compileJava` execution against the live working tree; no CONTEXT.md claim was taken on faith without a matching verification here |
| A2 | The 11 call sites in `ClickHouseHelperClientForkUnitTest` and the exact rewrite shape (map literal instead of `Table`) are mechanical and low-risk to port | Pattern 4 | Low — confirmed via `Grep` that all 11 sites follow the identical `new Table("db","t1")` + `shouldSkipDescribeRow(descriptor, table)` pattern; no site was found with divergent shape |
| A3 | No `src/main` file this phase touches has drifted from `v1.6.0` in a way this session's `git diff v1.6.0 HEAD` (0 lines for all seven files checked) didn't already rule out | Summary | None — this is a directly-executed verification, not an assumption; listed here only for completeness since it underpins every other claim in this document |

**This table is intentionally sparse.** Because this phase's raw material (the fork's pre-merge implementation, the current working tree's exact content) is 100% directly readable via `git show`/`Read`, almost nothing here rests on training-data memory of upstream's API — every code excerpt above carries a `Read`/`git show`/`Grep`/`gradlew` provenance tag inline.

## Open Questions

None blocking. Two items are explicitly **not** questions for this phase (already answered by locked CONTEXT.md decisions, restated here only so the planner doesn't re-open them):
1. **Should the metadata-free JSON path be re-applied?** No — D-10 locks this as Phase 9's decision; Phase 7 re-applies nothing of it, confirmed this session that `ClickHouseWriter.java` has no `shouldSkipTableMetadata`/`jsonInsertColumns`-in-writer trace to even consider re-applying.
2. **Should `ClickHouseBase.java` get its full test-harness contract back?** No — D-06 locks Phase 7 to exactly `@Tag("clickhouse")` + `uniqueConnectorName(TestInfo)`, both confirmed absent from the current file this session; the rest (MBean sweep, shared-container strategy) is Phase 10.

## Sources

### Primary (HIGH confidence — direct repository reads this session)
- `Read`/`Grep` of working-tree files (branch `sync/v1.6.0`): `Type.java`, `Column.java`, `Table.java`, `ClickHouseWriter.java` (multiple ranges: 70-170, 195-235, 271-370, 540-690, 1040-1140, 1260-1340, 1515-1570), `ClickHouseHelperClient.java` (1-95, 280-520, 552-640, 640-752), `ClickHouseSinkConfig.java` (1-100, 225-250), `ClickHouseSinkTask.java` (130-160), `ClickHouseBase.java` (imports/class decl), `ClickHouseFieldDescriptor.java`, `JsonDynamicTypeMappingTest.java`, `ColumnTest.java`, `RowBinaryUnsupportedTypeGateTest.java`, `MetadataFreeJsonInsertUnitTest.java`, `ClickHouseHelperClientForkUnitTest.java`, `ClickHouseHelperClientForkLiveTest.java`, `JsonColumnTest.java`, `AsyncInsertSettingsUnitTest.java`, `JsonInsertColumnsUnitTest.java`
- `git show pre-v1.6.0-sync:<path>` for the fork's pre-merge target implementation: `ClickHouseHelperClient.java` (buildTableFilterClause, shouldSkipDescribeRow, showTables*, builder), `ClickHouseWriter.java` (buildRowBinaryUnsupportedTypeMessage, validateDataSchema gate, doWriteColValue default), `Column.java` (Dynamic dispatch), `Type.java` (DYNAMIC constant), `ClickHouseSinkConfig.java` (config keys/fields/parsing/ConfigDef.define, async_insert comment)
- `git diff v1.6.0 HEAD -- <path>` (0 lines for all seven `src/main` files this phase touches) — confirms the working tree is byte-identical to upstream `v1.6.0` in this phase's scope, this session
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileJava` — actually executed this session; reproduced the exact single compile error at `ClickHouseSinkTask.java:149`
- `javap -classpath <resolved connect-api-4.1.1.jar> org.apache.kafka.connect.sink.SinkTask` — actually executed this session; confirmed `onPartitionsRevoked`/`onPartitionsAssigned` are absent from the compiled class

### Secondary (MEDIUM confidence)
- `.planning/phases/07-type-mapping-writer-helper-client-config/07-CONTEXT.md` — prior session's locked decisions (D-01..D-19), cross-checked against this session's direct reads above; every load-bearing claim in it was independently re-verified, not merely trusted
- `.planning/research/ARCHITECTURE.md`, `.planning/research/PITFALLS.md` — prior milestone-level research (pre-Phase-6 merge planning), used for pitfall framing; claims about upstream `v1.6.0` code shape were cross-checked against this session's direct reads where cited
- `.planning/MERGE-CHECKLIST.md` — Section 1 rows owned by Phase 7, cross-checked field-by-field against this session's compile/diff results

### Tertiary (LOW confidence)
None — no WebSearch or training-data-only claim was needed for this phase; the entire domain is this repository's own git history.

## Metadata

**Confidence breakdown:**
- Standard stack: N/A — no new dependency
- Architecture (the six re-expressions): HIGH — every current-state and target-state code excerpt is a direct `Read`/`git show` quote from this session, not a paraphrase
- Pitfalls: HIGH — grounded in the same direct reads plus the prior PITFALLS.md research, itself sourced from `git show`/`git diff` against the same repo
- Compile-blocker (D-01/D-02): HIGH — independently reproduced via both `gradlew compileJava` and `javap` this session, not merely cited from CONTEXT.md

**Research date:** 2026-09-19
**Valid until:** Until `sync/v1.6.0` is merged into `custom_build` or rebased against a newer upstream tag — the working tree's byte-identity to `v1.6.0` (this document's core grounding fact) would then no longer hold. Effectively valid for the remainder of this milestone.
