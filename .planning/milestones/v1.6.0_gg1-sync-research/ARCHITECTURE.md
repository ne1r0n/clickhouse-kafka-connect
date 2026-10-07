# Architecture Research: v1.6.0 Merge Collision Map

**Domain:** Kafka Connect sink connector — merging 581 upstream commits (`v1.2.9` → `v1.6.0`) into a fork with custom SMTs, table filtering, JSON/Dynamic column support, and a metadata-free JSON insert path.
**Researched:** 2026-09-18
**Confidence:** HIGH — every claim below is sourced from `git show <ref>:<path>` and a real `git merge-tree` run, not diff-stat estimation. No branches created, no merge run, working tree untouched.

**Merge-base confirmed:** `git merge-base custom_build v1.6.0` = `bec167d` = tag `v1.2.9` exactly (verified).

## Executive Summary

Upstream v1.6.0 didn't just grow the connector, it **inserted a new orchestration layer** (`DeliveryStrategy`/`ChunkFlusher`/buffer strategies) above `ProxySinkTask`, **moved JMX naming** out of `ProxySinkTask` into a new `util.jmx.SinkTaskStatistics`, **changed the `Table` constructor** from a mutable incremental builder to an immutable constructor-from-list, **changed `showTables()`'s return type** from `List<Table>` to `List<Table.TableDesc>`, and **relocated most test helpers** into a new `src/testFixtures` source set. None of these are line-level edits to functions the fork also touched — they are API-shape changes the fork's collision-file edits sit on top of. A textual 3-way merge will misattribute or silently drop fork logic in at least `ClickHouseWriter.getTable()` (2-arg `Table` constructor no longer exists), `ClickHouseHelperClient.showTables()` (return type changed), and the whole JMX `connector=` feature (home class deleted from `ProxySinkTask`).

The good news: `git merge-tree` shows only **35 content conflicts + 5 modify/delete conflicts = 40 real conflicts** out of the whole tree, and of the 12 `src/main` collision files named in the milestone table, **5 have zero functional fork changes** (`Record.java`, `Data.java`, `StructToJsonMap.java`, the record convertors, `Processing.java`, `Utils.java`, `KeyToValue.java`, `Table.java`, `InMemoryState.java`, `StateProvider.java`, `StateRecord.java`, `KeeperStateProvider.java`, `RangeContainer.java` — all fork "changes" there are Spotless import reordering only). Two of the fork's Active follow-ups close for free: upstream's `Utils.handleException` now checks `ServerException` alongside `ClickHouseException`, fixing the client-V2 dead-code bug fork Phase 4 flagged; and upstream independently fixed the exact `Map(K, Nullable(V))` RowBinary null-marker bug the fork fixed in Phase 2 (making that one line of the fork's `ClickHouseWriter.java` diff redundant).

The one loud blocker: the fork's JMX `connector=<name>` naming (JMX-01..04, a fully-tested fork feature) has **no home in v1.6.0's class structure**. `SinkTaskStatistics.getMBeanName(int taskId)` is a static method in a different package that builds the exact same connector-less MBean name string the fork replaced in Phase 4. This isn't a merge conflict Git will even show — it is a silent feature loss unless it is deliberately re-plumbed.

## New Data Flow at v1.6.0

### Diagram

```
Kafka Connect Framework
        │
        ▼
ClickHouseSinkTask.start()                              (ClickHouseSinkTask.java:34-50)
  ├─ new ProxySinkTask(config, errorReporter)                          :47
  ├─ proxySinkTask.start()
  └─ deliveryStrategy = createDeliveryStrategy(...)                    :50, :58-73
        │  picks ONE of three, from bufferCount/bufferFlushTime/exactlyOnce:
        │    bufferCount == 0                → DirectDeliveryStrategy      :66
        │    bufferCount > 0, exactlyOnce     → ExactlyOnceBufferStrategy  :69
        │    bufferCount > 0, !exactlyOnce    → AtLeastOnceBufferStrategy  :71
        ▼
ClickHouseSinkTask.put(records)                                         :125-126
  └─ deliveryStrategy.put(records)     ← NEW dispatch layer, did not exist at v1.2.9
        │
        ├─ DirectDeliveryStrategy.put()        → flusher.putDirect(records)   [no buffering,
        │                                          same as pre-buffer v1.2.9 behavior]
        ├─ AtLeastOnceBufferStrategy.put()      → buffers flat list; flusher.flush(buffer)
        │                                          once bufferCount/bufferFlushTime trip
        └─ ExactlyOnceBufferStrategy.put()      → per-partition buckets; flusher.flush(chunk)
                                                     only on exact bufferCount-sized chunks
        │
        ▼
ChunkFlusher.putDirect() / .flush()                          (ChunkFlusher.java:45-71)
  └─ proxySinkTask.put(records)                                        :63
        │
        ▼
ProxySinkTask.put(records)                                    (ProxySinkTask.java:72-116)
  ├─ Record.convert(v, isEnableDbTopicSplit, dbTopicSplitChar,
  │                  database, isDebeziumCDCEnabled)     ← NEW 5-arg signature (was 1-arg)
  ├─ group by batchKey() (topic/topic+partition/db-prefixed)
  └─ for each group: processing.doLogic(rec)                            :98
        │
        ▼
Processing.doLogic()                          (processing/Processing.java:131, unchanged shape)
  └─ dbWriter.doInsert(records, queryId, errorReporter)          :68, :92 — SAME signature as
        │                                                          v1.2.9 (DBWriter interface
        │                                                          already had this overload)
        ▼
ClickHouseWriter.doInsert()                          (db/ClickHouseWriter.java:191)
  ├─ getTable(database, topic)                                   :1520 (signature unchanged,
  │                                                                  body heavily rewritten)
  ├─ SCHEMA/SCHEMALESS/STRING → doInsertRawBinary/-V1/-V2 or doInsertJson    :225, :231
  └─ ClickHouse server
```

### Where responsibilities moved

| Responsibility | v1.2.9 (fork's base) | v1.6.0 |
|---|---|---|
| Record buffering / flush triggers | Did not exist — `put()` always inserted immediately | New `DeliveryStrategy` implementations (`DirectDeliveryStrategy`, `AtLeastOnceBufferStrategy`, `ExactlyOnceBufferStrategy`) own it; `ClickHouseSinkTask.put()` now only dispatches |
| Offset commit contract (`preCommit`) | `ClickHouseSinkTask` computed it directly (fork touched this — JMX only, offset logic untouched) | Delegated to `DeliveryStrategy.preCommit()`; buffered strategies read from `ChunkFlusher.drainFlushedOffsets()` |
| The actual `proxySinkTask.put()` call + per-call exception → DLQ routing | Inline in `ClickHouseSinkTask.put()` | Extracted into `ChunkFlusher.putDirect()`, shared by all three strategies |
| MBean name construction (`connector=`, version sanitizing) | `ProxySinkTask.getMBeanName()` — **the fork's Phase 4 target** | `SinkTaskStatistics.getMBeanName(int taskId)`, a **static method in `util.jmx`**, using `Version.ARTIFACT_VERSION` (a generated constant) instead of `ClickHouseClientOption.class.getPackage().getImplementationVersion()` |
| Table mapping metadata | `Table` built incrementally via public `addColumn()` | `Table` is now immutable: `addColumn()` is `private`, all columns passed to the constructor as a `List<Column>`; a new `Table.TableDesc` nested class stands in for the pre-describe listing |
| `showTables()`'s return shape | `List<Table>` (partially-built `Table` objects used only for their names) | `List<Table.TableDesc>` (a purpose-built value object with `getCleanName()`/`getFullName()`/`getNumColumns()`) |
| Error classification root-cause type | Only checked `instanceof ClickHouseException` (dead code under client V2 — Phase 4 finding) | `Utils.isServerError()` checks `ClickHouseException` **or** `ServerException`; `getRootCause(e, true)` prioritizes drilling to either |

This is exactly why a line-level/textual merge is misleading here: three of the fork's touch points (`ProxySinkTask` JMX naming, `ClickHouseWriter.getTable()`'s synthetic `Table`, `ClickHouseHelperClient.showTables()`'s filter clause) sit on APIs whose *shape*, not just body, changed. Git's 3-way merge will produce a file that compiles-looking but is semantically wrong (e.g., silently keeping the fork's old `getMBeanName()` while `SinkTaskStatistics` builds a second, connector-less MBean name nobody calls) or will not compile at all (2-arg `Table` constructor gone).

## Per-Collision-File Resolution Shape

Legend: **(a) survives as-is** — apply fork's hunk almost unchanged; **(b) re-express** — same intent, different attachment point/signature; **(c) redundant** — upstream already does this, drop the fork's version.

| File | Fork's real delta | Shape | Attach to upstream symbol |
|---|---|---|---|
| `ClickHouseWriter.java` | JSON/DYNAMIC fail-fast in `validateDataSchema` + `doWriteColValue` default; MAP null marker; `shouldSkipTableMetadata()`/`getTable()` synthetic Table; `jsonInsertColumns` passthrough to `client.insert(...)`; `setTargetTableFilter()` on builder | **(b) re-express**, see deep dive below | `doWriteColValue` default case (:669-673), `getTable()` (:1520), new `Table(db, name, hasDefaults, columns, numColumns)` ctor |
| `ClickHouseHelperClient.java` | `targetTableFilter` clause building, `shouldSkipDescribeRow`, UTF-8 readers | **(b) re-express**, see deep dive below | `showTables()`/`showTablesV1/V2` (now take `database`, return `List<Table.TableDesc>`; :333-377), `describeTableV1/V2` (columns collected into a `List<Column>` before `new Table(...)`, :387-508) |
| `ClickHouseSinkConfig.java` | `JSON_INSERT_COLUMNS`, `TARGET_TABLE_FILTER` config keys + fields; `//` commented-out `async_insert=0` forcing | **(a) survives as-is** — additive `ConfigDef.define()` blocks plus one line-comment; upstream still unconditionally does `this.addClickHouseSetting("async_insert", "0", false)` at the same spot (`v1.6.0:…ClickHouseSinkConfig.java:240`), so the fork's one-line removal reapplies cleanly | New config keys are additive; the `async_insert` line is a same-location one-line diff |
| `Column.java` | `valueType.startsWith("JSON")` → `Type.JSON`; `valueType.startsWith("Dynamic")` → `Type.DYNAMIC` | **JSON: (c) redundant** — upstream independently added `startsWith("JSON")` → `Type.JSON` (identical intent, `v1.6.0:…Column.java:173-175`). **DYNAMIC: (b) re-express** — upstream has no Dynamic branch; add `else if (valueType.startsWith("Dynamic")) type = Type.DYNAMIC;` next to upstream's JSON branch | `extractColumn()`'s `startsWith` chain |
| `Table.java` | None (import reorder only) | **(c) redundant** | n/a — but downstream code that called `new Table(database, name)` (2-arg) or public `addColumn()` **breaks**: constructor is now `Table(database, name, hasDefaults, columns, numColumns)` and `addColumn()` is `private` |
| `Type.java` | Added `JSON` (already existed upstream at base — false alarm) and `DYNAMIC` (new) | **(b) re-express** — one-line enum addition, `DYNAMIC` only (`JSON` already present both sides, unchanged) | Enum literal list |
| `ProxySinkTask.java` | `connectorName` field, ctor param, `buildMBeanName(String, int, String)` static extraction | **(b) re-express, structurally relocated** — see "Structural Blocker" below; the class this logic must move into (`SinkTaskStatistics`) is in a different package | `SinkTaskStatistics.getMBeanName(int taskId)` (`util/jmx/SinkTaskStatistics.java:162-167`) |
| `Utils.java` | None (import reorder only) | **(c) redundant, and the fix arrives for free** — upstream's rewritten `handleException`/`isServerError`/`getRootCause` already checks `ServerException`, closing the fork's own Active follow-up ("Error classification is dead code on the V2 path") | n/a |
| `ClickHouseSinkTask.java` | `connectorName = props.get("name")` passed into `ProxySinkTask` ctor | **(b) re-express** — upstream's `ProxySinkTask` ctor is 2-arg (`config, errorReporter`); the fork's ctor is 3-arg. Re-attach by adding a 3rd param again, but the value now needs to reach `SinkTaskStatistics`, not `ProxySinkTask` itself, for the MBean name (`ProxySinkTask` no longer builds the name — see JMX blocker) | `ClickHouseSinkTask.start()` (`v1.6.0:34-51`) |
| `Processing.java` | None (import reorder only) | **(c) redundant** | n/a |
| `state/provider/*`, `StateProvider.java`, `StateRecord.java`, `RangeContainer.java` | ~0 (one blank-line diff in `InMemoryState.java`, everything else import reorder) | **(c) redundant** | n/a — take upstream's `BaseStateProviderImpl` refactor wholesale |
| `KeyToValue.java` (upstream SMT, not fork's own) | None (import reorder only) — PROJECT.md's "candidate for removal" is validated: fork carries zero logic here | **(c) redundant** | n/a — take upstream's rewritten `KeyToValue` (+90) wholesale |
| `Record.java`, `Data.java`, `StructToJsonMap.java`, `SchemaRecordConvertor.java`, `SchemalessRecordConvertor.java`, `StringRecordConvertor.java` | None (import reorder only) | **(c) redundant** | n/a — `Record.convert()`'s new 5-arg signature (db-topic-split, Debezium CDC) is purely additive to callers |

### Deep dive: `ClickHouseWriter.java`

Upstream rewrote this file almost top to bottom (±1,289 upstream vs. the fork's targeted +85/-9). Concretely, the fork's hunks land as follows:

1. **JSON/DYNAMIC fail-fast in `validateDataSchema`'s inner `switch (colTypeName)`** (fork added `case "JSON": case "DYNAMIC": case "UNKNOWN":` throwing `DataException`, `v1.2.9→custom_build` diff). Upstream's `v1.6.0` version of this switch has **no such cases** — a JSON/DYNAMIC/UNKNOWN column still falls to `default:` and does bit-width comparison, which will misbehave the same way the fork's TYPE-06 fix addressed. **Re-express**, but **narrow it**: upstream now has a real, working `case JSON:` inside `doWriteColValue` (`v1.6.0:651-668`) that writes JSON as a string when `csc.isBinaryFormatWrtiteJsonAsString()` is true, else throws its own `RuntimeException`. Dropping `"JSON"` from the fork's early `validateDataSchema` gate and letting upstream's native path own it is the smaller, more correct change; **keep `"DYNAMIC"` and `"UNKNOWN"`** in the gate — upstream has no case for either, so both still silently corrupt the RowBinary stream via the untouched `default:` branch below.

2. **The same fail-fast in `doWriteColValue`'s default case** (fork's `buildRowBinaryUnsupportedTypeMessage` + `throw new DataException(msg)`, replacing a bare `LOGGER.error` with no throw). Confirmed upstream's `default:` at `v1.6.0:669-673` is **still** `LOGGER.error(...)` with **no throw** — the exact silent-corruption bug TYPE-06 fixed is present, unfixed, in the new file. **Re-express directly**, same shape, at the new default case's location (which now sits after upstream's new `case JSON:` and `case VARIANT:` blocks, not immediately after `case ARRAY:` as at v1.2.9).

3. **MAP branch RowBinary null marker** (fork's 3-line fix for `Map(K, Nullable(V))`). **Redundant** — upstream independently added the identical fix at `v1.6.0:562-564` (`if (col.getMapValueType() != null && col.getMapValueType().isNullable() && mapValue != null) BinaryStreamUtils.writeNonNull(stream);`), with one extra null-safety check the fork's version lacks. Take upstream's version; the fork's commit `3929ce6` and its pinning test both become no-ops on the new code (test should still pass — assert it does, don't delete it, since it is the suite's contract for this behavior surviving the sync).

4. **`shouldSkipTableMetadata()` / `getTable()` synthetic `Table` for the metadata-free JSON path.** Fork's `getTable()` shortcut does `new Table(database, tableName)` — the two-arg constructor. That constructor **no longer exists**: `Table`'s only constructor at v1.6.0 is `Table(String database, String name, boolean hasDefaults, List<Column> columns, int numColumns)` (`v1.6.0:…Table.java`, the `addColumn()`-per-call pattern the fork used previously is now unavailable — `addColumn()` is `private`). **Re-express** as `new Table(database, tableName, false, Collections.emptyList(), 0)`. `getTable()`'s body and signature (`v1.6.0:1520-1543`) are otherwise structurally the same shape as v1.2.9's, so the surrounding `shouldSkipTableMetadata()` early-return re-attaches cleanly once the constructor call is fixed.

5. **`jsonInsertColumns` passed to `client.insert(...)`** and **`cleanupExtraFields()`'s `shouldSkipTableMetadata()` bypass.** Both are additive one-liners next to code upstream did not touch in the same way (`doInsertJsonV2` exists in a new shape, `cleanupExtraFields` unchanged in signature). **Re-express**, locate the new `doInsertJsonV2` (`v1.6.0:1272`) and re-attach the `jsonInsertColumns` argument to its `client.insert(...)` call.

6. **`setTargetTableFilter(csc.getTargetTableFilter())` on the builder in `start()`.** Additive, one line, no upstream collision at that call site. **Survives as-is.**

### Deep dive: `ClickHouseHelperClient.java`

1. **`buildTableFilterClause()` + `targetTableFilter` field/builder setter.** No upstream equivalent exists. **Re-express against a changed return type**: at v1.2.9, `showTables(database)` built its own query string per V1/V2 method and returned `List<Table>`. At v1.6.0, `showTables(database)`/`showTablesV1(database)`/`showTablesV2(database)` (`v1.6.0:333-377`) each build `String.format("select database, table, count(*) as col_count from system.columns where database = '%s' group by database, table", database)` inline and return `List<Table.TableDesc>`. The fork's approach (build the query with the filter clause once in `showTables()`, pass the finished query string into `showTablesV1(query)`/`showTablesV2(query)`) must be re-applied against the new signatures — either restructure upstream's methods to accept the built query again (closest to the fork's existing shape, minimal diff) or inject `buildTableFilterClause(targetTableFilter)` into each of the two inline `String.format` calls (matches upstream's structure, more diff-resistant to a *future* sync). Recommend the latter, since upstream is now the party more likely to keep touching this method shape.

2. **`shouldSkipDescribeRow()` + its call sites in both `describeTableV1`/`describeTableV2`.** This is the harder one. At v1.2.9, columns were added to a live `Table` object one at a time via `table.addColumn(column)` inside the read loop, so `shouldSkipDescribeRow()` could look up an already-added parent via `table.getAllColumnsMap().get(parentName)`. At v1.6.0, both `describeTableV1` and `describeTableV2` (`v1.6.0:387-508`) accumulate a **local `List<Column> columns`** during the loop and only construct the `Table` once, at the very end, via `new Table(database, tableName, hasDefaults, columns, numColumns)`. There is no `Table` object to query mid-loop. **Re-express** `shouldSkipDescribeRow()`'s parent lookup against the local `columns` list built so far (build a `Map<String, Column>` alongside it in the loop, or linear-scan `columns` for the matching name — DESCRIBE TABLE with `describe_include_subcolumns` is documented to emit a parent before its children, matching the existing "child of a JSON/DYNAMIC column" assumption). Insert the skip check at the same point in the loop upstream already skips alias/materialized/ephemeral columns (right after that check, before `columns.add(column)`).

3. **UTF-8 `InputStreamReader`s (D-15/TYPE-07).** `describeTableV1` never used a raw reader (parses `ClickHouseValue.asString()` from `ClickHouseRecord`, unaffected). `describeTableV2` at v1.6.0 **still** does `new InputStreamReader(queryResponse.getInputStream())` with **no charset** (`v1.6.0:466`) — the exact platform-default-charset bug the fork's D-15 fixed is present, unfixed, in the new file (upstream never picked up this bug independently, unlike the MAP-null-marker case). **Re-express directly**: add `, StandardCharsets.UTF_8` at the same call site.

4. **`showTables()`/`describeTable()` numColumns semantics changed alongside the constructor.** Not a fork collision, but load-bearing context: upstream's new `Table.TableDesc` javadoc explicitly documents that `numColumns` must count "top-level columns, including aliases, materialized and ephemeral ones" so it stays comparable across `extractTablesMapping`'s cache-staleness check (`v1.6.0:…Table.java`, `TableDesc` class). If the re-expressed `shouldSkipDescribeRow` also skips a row that should count toward `numColumns`, the cache-staleness comparison silently breaks. Increment `numColumns` **before** any skip check, matching upstream's own alias/materialized/ephemeral columns (`v1.6.0:387-475`, `numColumns++` happens unconditionally per top-level `fieldDescriptor`, before the alias/materialized/ephemeral skip).

## Test-Infrastructure Architecture

| Fork class | Upstream v1.6.0 status | Verdict |
|---|---|---|
| `ClickHouseBase` (`src/test/java/.../sink/ClickHouseBase.java`) | **Rewritten** (262 lines touched, 77 insertions/185 deletions vs. v1.2.9). New shape: `@TestInstance(PER_CLASS)`, **non-static** `db` field (one new container **per test class**, not shared across the JVM fork), adds cluster-mode (`ClickHouseCluster`) and cloud-mode branches, mounts `clickhouse_keeper_map.xml` to enable embedded Keeper for EO tests without a real ZK ensemble, moves `createClient`/`createDatabase`/`countRows`-style helpers out to `ClickHouseTestHelpers` | **Rebuild on top of upstream's shape, keep the fork's container-lifecycle strategy.** The fork's version is a locked Key Decision ("one shared ClickHouse container per test JVM fork ... 0 flakes observed at 6 forks") that upstream's per-class-container model would directly regress (581 commits' worth of new test classes × a fresh container each × Testcontainers startup cost). Port forward: `@Tag("clickhouse")`, the static shared `db`/`database` fields, `@AfterEach` MBean leak sweep, `uniqueConnectorName(TestInfo)`. Take from upstream: the Keeper-config mount (useful for real EO test coverage without external ZK — check if the fork's `KeeperStateProvider` tests already require live ZK and could benefit), skip cluster/cloud modes (out of scope per PROJECT.md). |
| `ClickHouseTestHelpers` | **Moved** to `src/testFixtures/java/.../sink/helper/ClickHouseTestHelpers.java`; `git merge-tree` reports it as a **modify/delete conflict** at the old `src/test/java` path (upstream deleted it there) | **Rebuild in the new location.** The fork's helper additions (whatever they are — grep the fork's copy for CLICKHOUSE_VERSION default `latest`, `extractClientVersion`, etc.) must be re-applied against the `src/testFixtures` copy, not the deleted `src/test` one. `testFixtures` is exactly the mechanism `build.gradle.kts`'s `testFixturesApi`/`testFixturesImplementation` block wires up — code here becomes reusable as `testFixtures(project(":"))` from any source set, matching PROJECT.md's "build.gradle.kts taken from upstream wholesale" decision. |
| `SchemaTestData`, `SchemalessTestData` | **Moved** to `src/testFixtures/java/.../sink/helper/`; both are also duplicated at `src/integrationTest/java/.../sink/helper/` in the fork and flagged as modify/delete conflicts there too (upstream deleted the integrationTest copies, presumably now importing the testFixtures ones) | **Rebuild in `src/testFixtures`, delete the fork's `src/integrationTest` duplicates** once integrationTest is repointed at the testFixtures copy — matches upstream's own consolidation. |
| `InMemoryDLQ` (`src/test/java/.../sink/dlq/InMemoryDLQ.java`) | **Untouched by upstream** — zero-diff `v1.2.9` → `v1.6.0` | **Carry forward unchanged.** No conflict, no rebuild. |
| `FromVersionConditionExtension` | **Moved and repackaged**: `src/test/java/.../sink/junit/extension/` → `src/testFixtures/java/.../test/junit/extension/` (note the package changes from `...sink.junit.extension` to `...test.junit.extension`, not just the source root) | **Rebuild at the new path+package.** `git merge-tree` shows this one auto-merges cleanly (no CONFLICT line) at the `src/testFixtures` destination — but only because the fork's copy at the *old* path is simply absent from the 3-way compare at that path (Git sees "upstream moved a file the fork never touched" as no conflict). Any fork test that imports the old package (`com.clickhouse.kafka.connect.sink.junit.extension.FromVersionConditionExtension`) needs its import updated to the new package. |

**Does `testFixtures` change where shared test code should live? Yes.** Upstream's introduction of `java-test-fixtures` (confirmed in `build.gradle.kts`'s `testFixturesApi`/`testFixturesImplementation` dependency blocks and the `sourceSets { … }` block referencing `testFixtures(project(":"))`) is the new idiom for anything shared between `src/test` and `src/integrationTest`. Any *new* fork test helper written during this milestone (or after) that needs to be visible from both source sets belongs in `src/testFixtures`, not duplicated into both as the fork currently does for `SchemaTestData`/`SchemalessTestData`.

## Recommended Merge Order

Ordering is dependency-driven, not size-driven: each stage must produce a **compilable** tree before the next stage's manual resolution can be meaningfully tested.

```
Stage 0  Build plumbing            build.gradle.kts, VERSION, .gitignore, gradle-wrapper.properties
              │  (nothing compiles without this; JaCoCo/testUnit/JDK17 must be re-applied
              │   on top of upstream's version-catalog + testFixtures structure)
              ▼
Stage 1  Type/mapping layer         Type.java, Column.java, Table.java
              │  (ClickHouseWriter and ClickHouseHelperClient both depend on the new
              │   Table constructor and Column.extractColumn's startsWith chain — resolve
              │   this leaf layer first so the two big files have a stable target)
              ▼
Stage 2  Core writer + helper       ClickHouseWriter.java, ClickHouseHelperClient.java,
                                     ClickHouseSinkConfig.java
              │  (the two hardest files; ClickHouseSinkConfig's new keys are additive and
              │   low-risk, sequence it alongside the writer/helper since both read from it)
              ▼
Stage 3  Orchestration layer        ProxySinkTask.java, ClickHouseSinkTask.java,
                                     Processing.java (mechanical), Utils.java (mechanical)
              │  (depends on Stage 2's DBWriter/ClickHouseHelperClient being resolved;
              │   this is where the JMX connector= re-plumbing happens — see blocker below)
              ▼
Stage 4  SMTs and remaining         KeyToValue.java (take upstream wholesale), transforms/
         src/main collisions        package untouched files (TableOperationFilter,
                                     HeadersToValue, HeaderToValue, ConvertToMapOrJson —
                                     not in the collision table, confirm zero upstream
                                     touch before skipping)
              ▼
Stage 5  Test infrastructure        ClickHouseBase.java, ClickHouseTestHelpers +
                                     SchemaTestData + SchemalessTestData + Schema-
                                     lessTestData → src/testFixtures, FromVersionCondition-
                                     Extension package move
              │  (depends on Stage 0's testFixtures wiring existing, and on Stage 1-3's
              │   production API shapes being final, since test helpers call them)
              ▼
Stage 6  Fork's 371 tests           ClickHouseSinkTaskTest, ClickHouseSinkTaskWithSchema-
                                     Test/-ProxyTest, ClickHouseSinkTaskSchemalessTest/
                                     -ProxyTest, ClickHouseSinkTaskStringTest,
                                     ClickHouseWriterTest, ClickHouseHelperClientTest,
                                     ColumnTest, TableTest, RangeContainerTest,
                                     ProcessingTest, UtilsTest, all fork-specific test
                                     classes (JsonInsertColumnsUnitTest, MBeanNameUnitTest,
                                     AsyncInsertSettingsUnitTest, JsonDynamicRoundTripTest,
                                     the SMT test suites, etc. — not flagged by merge-tree
                                     since upstream never touched them, but they call the
                                     production APIs resolved in Stages 1-4)
              ▼
Stage 7  CI + publication           tests.yaml, VERSION → v1.6.0_gg1, push
```

**Why this order and not "biggest files first":** `ClickHouseWriter.java` cannot be meaningfully resolved before `Table.java`'s new constructor exists, because `getTable()`'s synthetic `Table` call literally will not compile against the old 2-arg constructor. `ClickHouseHelperClient.java` cannot be resolved before `Column.java`/`Type.java` for the same reason (`Type.DYNAMIC` must exist before `shouldSkipDescribeRow` can reference `Type.DYNAMIC`). `ProxySinkTask.java`'s JMX re-plumbing cannot be finished before `ClickHouseWriter`/`ClickHouseHelperClient` compile, because `ProxySinkTask` constructs both. Test infrastructure (Stage 5) depends on every production API being in its final shape, because `ClickHouseTestHelpers.createClient()` and friends call the builder methods (`setTargetTableFilter`, etc.) whose signatures Stage 2 fixes. Stage 6 (the fork's actual test suite) is last among code stages because it is the widest fan-in — every earlier stage's decisions are exercised there.

## Verification Checkpoints

| After stage | Cheapest signal | How to run |
|---|---|---|
| 0 (build plumbing) | `./gradlew help` succeeds; `./gradlew dependencies` resolves without errors | No compilation needed yet — just confirms the version catalog + testFixtures source set + re-applied JaCoCo/testUnit config parse |
| 1 (Type/mapping) | `./gradlew compileJava` on `src/main` alone | Fast, Docker-free; catches constructor/enum-shape breakage immediately |
| 2 (writer/helper/config) | `./gradlew compileJava` full; then `./gradlew testUnit` | **`testUnit` is the fork's Docker-free gate (`excludeTags("clickhouse")`, deliberately kept out of `check`) — this is the single cheapest broad signal in the whole repo and is usable the moment `src/main` and any non-`@Tag("clickhouse")` unit tests compile.** Run it here even before test infrastructure (Stage 5) is fully rebuilt, since many unit tests (`ColumnTest`, `TableTest`, config-parsing tests) don't depend on `ClickHouseBase` at all. |
| 3 (orchestration/JMX) | `./gradlew testUnit` again, plus a targeted read of `MBeanNameUnitTest` (Docker-free per the fork's inventory) to confirm `connector=` naming didn't silently vanish | The JMX blocker (see below) will **not** show up as a compile error or even a merge conflict — it shows up as `MBeanNameUnitTest` asserting a string upstream's `SinkTaskStatistics.getMBeanName()` no longer produces the same way. Treat this test's pass/fail as the specific canary for the blocker. |
| 4 (SMTs) | `./gradlew testUnit` — SMT unit tests (`TableOperationFilterTest`, `HeadersToValueTest`, etc.) are Docker-free per the fork's inventory table | |
| 5 (test infrastructure) | `./gradlew compileTestJava compileTestFixturesJava` | Confirms `ClickHouseBase` and its `testFixtures` dependents compile against the Stage 1-3 API shapes before running anything against a real container |
| 6 (full fork suite) | `./gradlew test` against ClickHouse `latest` × client V2 (colima running) — the milestone's actual acceptance gate | This is where the two hardest-file re-expressions (JSON/DYNAMIC fail-fast, `targetTableFilter`/`shouldSkipDescribeRow`) get their real signal: `TypeCoverage`/`JsonDynamicRoundTripTest`, `DESC-01..04`-equivalent tests, `FILT-01..04`-equivalent tests, `JMX-01..04`, `ASYNC-01..03` |
| 7 (CI) | `tests.yaml` green on the pushed SHA, located by `head_sha` + workflow `path` per the fork's existing verification pattern (DEC-F) | |

`testUnit`'s value here is structural, not incidental: because Stages 1-4 are all `src/main` + Docker-free-test-eligible changes, `testUnit` is runnable *before* the Docker-dependent `ClickHouseBase` rebuild (Stage 5) is even started, giving 4 of 7 stages a fast, no-Docker feedback loop.

## Structural Blockers — Flag Loudly

### 1. JMX `connector=` naming has no home in v1.6.0 (confirmed, not speculative)

The fork's entire Phase 4 JMX feature — `ProxySinkTask.buildMBeanName(String connectorName, int id, String version)`, extracted specifically so JMX-01..04 could exercise the null-version and colon-in-version branches — depended on `ProxySinkTask` owning MBean name construction. At v1.6.0, `ProxySinkTask` no longer constructs `SinkTaskStatistics` with a name string at all; `SinkTaskStatistics` builds its own MBean name via a **static** `getMBeanName(int taskId)` in a **different package** (`util.jmx`), using `Version.ARTIFACT_VERSION` — a build-time-generated constant from `VERSION` file content, not the runtime jar-manifest lookup the fork's null/colon branches were built to handle. Concretely:

- `com.clickhouse:type=ClickHouseKafkaConnector,name=SinkTask%d,version=%s` (v1.6.0's format, **no `connector=`**) vs. the fork's `com.clickhouse:type=ClickHouseKafkaConnector,connector=%s,name=SinkTask%d,version=%s`.
- There is also a **second**, topic-level MBean name method (`SinkTaskStatistics.getMBeanName(int taskId, String topic)`, upstream-new, not present in the fork at all) that would need the same `connector=` treatment for consistency if the feature is to mean what it meant before.
- `Version.ARTIFACT_VERSION` is generated from the `VERSION` file (confirmed: `build.gradle.kts`'s `generateVersionFile` task writes `public static final String ARTIFACT_VERSION = "${project.version}";`), so it can **never be null** — the fork's null-version branch becomes structurally unreachable through this path (though still worth keeping defensively, since `String.replace(':', '_')` is cheap and the VERSION file's format is not contractually guaranteed colon-free forever).

This will not surface as a `git merge-tree` conflict (the two sides touch different files/classes) and will not fail to compile — it will silently ship a connector-less MBean name unless someone deliberately re-plumbs `connectorName` from `ClickHouseSinkTask.start()` through `ProxySinkTask`'s constructor into `SinkTaskStatistics`'s constructor and both `getMBeanName` overloads. Budget real design time for this in Stage 3, not just a mechanical patch-reapply.

### 2. `bypassRowBinary`'s escape-hatch message needs a rewrite, not a straight carry-forward

Upstream added a **third** JSON-handling path (`binaryFormatWrtiteJsonAsString`, `v1.6.0:…ClickHouseSinkConfig.java:129,327`) that the fork's TYPE-06 message (`"Use insertFormat=json or bypassRowBinary=true instead."`) doesn't know about. Once `case JSON:` is dropped from the re-expressed fail-fast gate (see deep dive above), the remaining `DYNAMIC`/`UNKNOWN` message text should be reviewed — it's still accurate for those two types, but a maintainer reading it after this merge should not conclude JSON columns are still RowBinary-fatal, since upstream partially fixed that for JSON specifically.

### 3. Two follow-ups close for free — verify, then delete from the Active backlog

- **`Utils.handleException`'s dead-code error classification** (fork's own Phase 4 finding) is fixed upstream via `ServerException` handling in `isServerError()`/`getRootCause()`. No fork action needed beyond confirming the fork's existing test for this behavior (if any) passes against the new code.
- **`ClickHouseWriter`'s MAP-branch null-entry path** (WR-01/WR-02 follow-ups) — upstream's independent fix uses the same pattern as the fork's Phase 2 fix, with an added `col.getMapValueType() != null` guard the fork's version lacked. Re-verify WR-01 (actual `null` entry inside a `Map(K, Nullable(V))` column, never exercised through RowBinary per the fork's own admission) against upstream's version once merged — it may still be open, since the *null-marker-for-non-null-values* bug is fixed but the *actual-null-value* path was explicitly called out as unimplemented in both versions.

## Real Conflict Inventory (from `git merge-tree`, not estimated)

`git merge-tree --write-tree custom_build v1.6.0` (git 2.50.1) against merge-base `bec167d`, run read-only, no branches created, no merge run:

**35 content conflicts:**
```
.github/workflows/tests.yaml                                                   (trivial — CI rewrite from both sides)
.gitignore                                                                     (trivial — additive on both sides)
VERSION                                                                        (trivial — v1.2.9_gg4 vs v1.6.0)
build.gradle.kts                                                               (Stage 0)
gradle/wrapper/gradle-wrapper.properties                                       (trivial — wrapper version bump)
src/integrationTest/java/.../sink/ClickHouseCloudTest.java                     (out of scope — no cloud creds)
src/integrationTest/java/.../sink/ClickHouseSinkConnectorIntegrationTest.java  (integrationTest, workflow_dispatch only)
src/integrationTest/java/.../sink/ExactlyOnceTest.java                        (integrationTest, workflow_dispatch only)
src/integrationTest/java/.../sink/helper/ConfluentPlatform.java               (integrationTest)
src/main/java/.../sink/ClickHouseSinkConfig.java                              (Stage 2)
src/main/java/.../sink/ClickHouseSinkTask.java                                (Stage 3)
src/main/java/.../sink/ProxySinkTask.java                                     (Stage 3 — JMX blocker)
src/main/java/.../sink/db/ClickHouseWriter.java                               (Stage 2 — hardest file)
src/main/java/.../sink/db/helper/ClickHouseHelperClient.java                  (Stage 2 — hardest file)
src/main/java/.../sink/db/mapping/Column.java                                 (Stage 1)
src/main/java/.../sink/db/mapping/Table.java                                  (Stage 1 — constructor shape change)
src/main/java/.../sink/db/mapping/Type.java                                   (Stage 1)
src/main/java/.../sink/processing/Processing.java                            (Stage 3, mechanical — import reorder only)
src/main/java/.../transforms/KeyToValue.java                                 (Stage 4 — take upstream wholesale)
src/test/java/.../sink/ClickHouseBase.java                                    (Stage 5)
src/test/java/.../sink/ClickHouseSinkTaskSchemalessProxyTest.java             (Stage 6)
src/test/java/.../sink/ClickHouseSinkTaskSchemalessTest.java                  (Stage 6)
src/test/java/.../sink/ClickHouseSinkTaskStringTest.java                     (Stage 6)
src/test/java/.../sink/ClickHouseSinkTaskTest.java                           (Stage 6)
src/test/java/.../sink/ClickHouseSinkTaskWithSchemaProxyTest.java            (Stage 6)
src/test/java/.../sink/ClickHouseSinkTaskWithSchemaTest.java                 (Stage 6)
src/test/java/.../sink/db/ClickHouseWriterTest.java                          (Stage 6)
src/test/java/.../sink/db/helper/ClickHouseHelperClientTest.java             (Stage 6)
src/test/java/.../sink/db/mapping/ColumnTest.java                            (Stage 6)
src/test/java/.../sink/db/mapping/TableTest.java                             (Stage 6)
src/test/java/.../sink/kafa/RangeContainerTest.java                          (Stage 6, mechanical)
src/test/java/.../sink/processing/ProcessingTest.java                        (Stage 6, mechanical)
src/test/java/.../sink/util/UtilsTest.java                                    (Stage 6, mechanical)
src/testFixtures/java/.../sink/helper/SchemaTestData.java                     (Stage 5)
src/testFixtures/java/.../sink/helper/SchemalessTestData.java                (Stage 5)
```

**5 modify/delete conflicts** (fork edited a path upstream deleted/relocated — the fork's version is what `git merge-tree` leaves in the tree; must be manually relocated, not just kept):
```
src/integrationTest/java/.../sink/helper/ClickHouseAPI.java          (upstream deleted; fork's copy survives naively — verify still needed)
src/integrationTest/java/.../sink/helper/SchemaTestData.java         (upstream moved to src/testFixtures)
src/integrationTest/java/.../sink/helper/SchemalessTestData.java     (upstream moved to src/testFixtures)
src/test/java/.../sink/helper/ClickHouseTestHelpers.java             (upstream moved to src/testFixtures)
src/test/java/transforms/KeyToValueTest.java                         (upstream moved+repackaged to src/test/java/.../transforms/KeyToValueTest.java)
```

**Auto-merged cleanly despite 3-way stages present** (both sides touched the file, Git's textual merge succeeded without markers — still worth a manual read since "compiles" ≠ "correct"):
```
src/main/java/.../sink/data/Record.java                (new 5-arg convert() signature — additive, no fork logic lost)
src/main/java/.../sink/state/provider/InMemoryState.java
src/main/java/.../util/Utils.java                      (fork made zero functional changes here — auto-merge trivially correct)
src/test/java/.../sink/ClickHouseSinkJdbcPropertiesTest.java
src/test/java/.../sink/ClickHouseSinkTaskMappingTest.java             (contains the targetTableFilter live test — re-verify by hand regardless of clean auto-merge)
src/test/java/.../sink/provider/LocalProviderTest.java
src/testFixtures/java/.../test/junit/extension/FromVersionConditionExtension.java  (new path; fork's old-path copy is simply absent from this 3-way comparison)
```

Total real conflict surface: **40 files Git flags**, plus 7 files that merge cleanly but carry fork logic worth a manual read, against a total upstream delta of 91 files (35 `src/main` + 56 test files per PROJECT.md's baseline). The overwhelming majority of upstream's 91-file delta (the other ~44 test files, `AtLeastOnceBufferStrategy`/`ExactlyOnceBufferStrategy`/`ChunkFlusher`/`DeliveryStrategy`/`DirectDeliveryStrategy`/`DebeziumRecordConvertor`/`BaseStateProviderImpl`/`DataJson`/`SimpleMovingAverage`/`SinkTaskStatisticsMBean`/`TopicStatistics`/`TopicStatisticsMBean` and every upstream test file for them) are **new files with no fork counterpart at all** — pure additions, zero conflict risk, adopted wholesale by definition of the merge.

## Sources

- `git show v1.6.0:src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java`, `ProxySinkTask.java`, `DeliveryStrategy.java`, `DirectDeliveryStrategy.java`, `ChunkFlusher.java`, `AtLeastOnceBufferStrategy.java`, `ExactlyOnceBufferStrategy.java`
- `git show v1.6.0:src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`, `db/helper/ClickHouseHelperClient.java`, `db/mapping/{Column,Table,Type}.java`
- `git show v1.6.0:src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java`, `util/Utils.java`, `util/jmx/SinkTaskStatistics.java`
- `git diff bec167d custom_build -- <path>` for every file in the milestone's collision table, plus `Record.java`, `Data.java`, `StructToJsonMap.java`, `Processing.java`, `Table.java`, `InMemoryState.java`, `KeyToValue.java`, convertors, `ClickHouseBase.java`, `InMemoryDLQ.java`
- `git diff bec167d v1.6.0 -- <path>` for the same set, to isolate upstream's independent changes
- `git merge-tree --write-tree custom_build v1.6.0` (git 2.50.1) — full output archived at `/tmp/mergetree_out.txt` during this research session, not persisted to the repo
- `git ls-tree -r --name-only <ref> -- src/main/java` for both `bec167d` and `v1.6.0` — full new/removed file inventory
- `build.gradle.kts` at `v1.6.0` — `generateVersionFile` task, `testFixtures*` dependency blocks, `sourceSets` block

---
*Architecture research for: clickhouse-kafka-connect `custom_build` → `v1.6.0` merge*
*Researched: 2026-09-18*
