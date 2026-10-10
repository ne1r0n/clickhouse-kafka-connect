# Architecture Research — v1.7.0_gg2-hardening

**Domain:** Kafka Connect sink connector for ClickHouse (fork, `custom_build`) — integration map for an 11-item follow-up hardening and CI-hygiene milestone
**Researched:** 2026-10-10
**Baseline read:** `sync/v1.7.0` HEAD `9ff1affc` (21 commits ahead of `origin/custom_build` = `f328fd21` = tag `v1.7.0_gg1`). Line numbers below are for that tree. `.planning/codebase/` is stale for the writer, helper client and build, so none of it was used; everything here comes from live source.
**Confidence:** HIGH for file/line locations and CI/build mechanics. HIGH for the ClickHouse wire facts (probed live on 26.9.14.10, see 999.1). MEDIUM for the response-leg LZ4 assertion design and the 999.2 fix choice (designs, not yet executed).

---

## Standard Architecture

### System overview (only the parts this milestone touches)

```
 Kafka -> ClickHouseSinkTask -> ProxySinkTask -> Processing (state machine, UNTOUCHED)
                                                     |
                                                     v
 +--------------------------- ClickHouseWriter.doInsert -> doInsertBatch (L230-250) ----------------------+
 |  route key = SchemaType + csc.isBypassRowBinary() + shouldSkipTableMetadata() (L1741)                  |
 |                                                                                                         |
 |  SCHEMA / DEBEZIUM_CDC                                                                                  |
 |   |- rowbinary (default) --> doInsertRawBinary (L1155) -> writeRecordsRowBinary (L1275)                 |
 |   |                           -> doWriteCol (L1026) -> doWriteColValue (L612) -> doWritePrimitive (L881)|
 |   |                                ^ [WR-03 L1093-94]   ^ [999.1 Tuple L731-775]  ^ Enum L994-999       |
 |   |                           catch (Exception) L1176-1178  <- [WR-04]                                  |
 |   |- bypassRowBinary=true ------> doInsertJson -> writeRecordsJson (L1353)  <- [empty-CDC skip, else L1387]
 |   '- insertFormat=json explicit -> doInsertJson -> writeRecordsJson                                      |
 |                                     |- SCHEMA        -> MetadataFreeJsonWriter.structRow               |
 |                                     '- DEBEZIUM_CDC  -> skip-if-empty (L1365-1373) -> cdcRow           |
 |  SCHEMA_LESS -> doInsertJson     STRING_SCHEMA -> doInsertString                                        |
 |  doWriteDates (L412) ---- two ERROR logs L461, L515  <- [T-13-19]                                       |
 +---------------------------------------------------------------------------------------------------------+
        ^ mapping (Table, immutable, upstream-identical)
        |
 ClickHouseHelperClient.describeTableV1/V2 (L555-690)
   -> ClickHouseFieldDescriptor.fromJsonRow -> Column.extractColumn(fd)   <- [999.1: Column.java L185-294]
   -> warnOnceIfNullableEnumOrTuple (L515, called L579 / L650)             <- [999.1 consequence, IN-01..05]

 Build / CI plane (no runtime impact):
   .github/workflows/tests.yaml   build.gradle.kts   lombok.config   gradle/libs.versions.toml
```

### Component responsibilities (relevant subset)

| Component | Responsibility | File | Owned by fork? |
|---|---|---|---|
| `Column.extractColumn` | Parses a DESCRIBE type string into a `Column` (type, `isNullable`, children). The nullability flag is the only thing `doWriteCol` uses to decide whether to emit a RowBinary null marker | `sink/db/mapping/Column.java` | Mostly upstream; fork lines are the `Dynamic` dispatch (L176) and the Array-recursion argument order (L213) |
| `Table` | Builds root/all column maps and fills `Tuple`/`Map` children from DESCRIBE sub-column rows | `sink/db/mapping/Table.java` | **Byte-identical to upstream v1.7.0. Do not edit** |
| `ClickHouseWriter` | Route selection, RowBinary and JSON serialisation, null-marker rules, all fork guards | `sink/db/ClickHouseWriter.java` (1799 lines) | Hot, high-risk, 10 fork regions |
| `MetadataFreeJsonWriter` | Route-scoped JSON encoder for `insertFormat=json` Struct and CDC records | `sink/db/MetadataFreeJsonWriter.java` | Fork-only file |
| `ClickHouseHelperClient` | DESCRIBE/SHOW, client construction, `shouldSkipDescribeRow`, the 999.1 WARN | `sink/db/helper/ClickHouseHelperClient.java` | Fork patches in 7 places |
| `DebeziumRecordConvertor` | Produces `DEBEZIUM_CDC` records; returns an empty `jsonMap` for `op=t` / `op=null` / null before-or-after | `sink/data/convert/DebeziumRecordConvertor.java` L70-100 | Upstream, not adopted by default but used by `debeziumCDCEnabled=true` |
| `tests.yaml` | The only workflow that runs on push | `.github/workflows/tests.yaml` (74 lines) | Fork-rewritten |
| `fork-anchors.txt` + `verify-merge-invariants.sh` | Every fork construct has a grep-count anchor; **an anchor must move in the same commit as the code** because the script evaluates each commit in `pre-v1.7.0-sync..HEAD` | `.planning/scripts/` | Fork-only |

---

## Integration Map per Milestone Item

### 999.1 — `Column.extractColumn` drops `Nullable` for `Enum8` / `Enum16` / `Tuple`

**Where `Column` parses and loses nullability** (`Column.java`):

| Branch | Lines | Takes `isNull`? |
|---|---|---|
| `Enum*` | 202-211 | **No** — builder never calls `.isNullable(isNull)` |
| `Array` | 212-223 | No (Array cannot be Nullable in ClickHouse); element recursion passes `false`, but `Array(Nullable(X))` re-enters through the `Nullable` branch with `true` |
| `Map` | 224-231 | No (cannot be Nullable) |
| `Tuple` | 232-236 | **No** |
| `Variant` | 241-271 | No (cannot be Nullable) |
| `LowCardinality` / `SimpleAggregateFunction` | 272-273, 276-283 | Passes `isNull` through correctly |
| `Nullable(...)` | 274-275 | Recurses with `true` |
| primitive tail | 285-293 | **Sets** `.isNullable(isNull)` — the only place it is set |

**How nullability flows into the writer.** `ClickHouseWriter.doWriteCol` (L1026-1097) is the single consumer for top-level columns: it writes the `Nullable` NonNull marker only if `col.isNullable()` (L1038, L1061, L1079), so a Nullable Enum/Tuple column is written without its marker. Nested slots go through `doWriteColValue` ARRAY (L703-728), MAP (L659-701) and TUPLE (L731-775), all of which gate on `child.isNullable()`. The Phase 13 nested-null guards (`!isNullable()` → `DataException`, via `buildNestedNullMessage` L565) therefore throw on legal `Nullable(Enum)` / `Nullable(Tuple)` slots. `Enum` null writes already work once the flag is right: `doWritePrimitive` (L881) writes `writeNull` for a null value (L884-886) and Enum8/16 are routed there (L634-635).

**Verified wire facts (live probe, ClickHouse 26.9.14.10 via `clickhouse-local`, 2026-10-10)** — this is what decides the scope split:

| Column type | RowBinary bytes | DESCRIBE (with `describe_include_subcolumns=1`) |
|---|---|---|
| `Nullable(Enum8('a'=1,'b'=2))` | `[flag][int8]`; NULL = `[01]` | single row, no children |
| `Nullable(Tuple(a Int32, b String))` | `[flag][a: plain Int32][b: plain String]`; NULL = `[01]` and **nothing else** | `c Nullable(Tuple(...))`, `c.null UInt8`, `c.a Nullable(Int32)`, `c.b Nullable(String)`, `c.b.size Nullable(UInt64)` |
| `Nullable(Tuple(a Nullable(Int32), b String))` | `[flag][a: marker+Int32][b: plain String]` | `c.a Nullable(Int32)`, `c.a.null Nullable(UInt8)`, `c.b Nullable(String)` — **same `c.a`/`c.b` types as the previous row** |
| `Array(Nullable(Tuple(a Int32)))` | `[size]` then per element `[flag][plain Int32]` | `d.size0`, `d.null Array(UInt8)`, `d.a Array(Nullable(Int32))` |

(`CREATE TABLE ... Nullable(Tuple(...))` works on 26.9 without `allow_experimental_nullable_tuple_type`, confirmed.)

Consequences, in decreasing importance:

1. **`Enum8`/`Enum16` is a one-line fix.** Add `.isNullable(isNull)` to the builder at L209. No children, so no wire surprise. Every nested form (`Array(Nullable(Enum8))`, `Map(K, Nullable(Enum16))`) already recurses through the `Nullable` branch with `isNull=true`, so it is fixed by the same line.
2. **`Tuple` is NOT a one-line fix, and the naive fix silently corrupts data that works today.** DESCRIBE sub-column rows mark every Tuple child `Nullable(...)` whether or not the user declared it (the nullable-tuple wrapper leaks into the sub-column type), but the wire carries a child marker only if the child was *declared* Nullable. Today a single-field `Nullable(Tuple(a Int32))` works by coincidence: the child's synthetic `Nullable(Int32)` marker byte doubles as the tuple flag. Setting `.isNullable(isNull)` on the Tuple branch alone makes the writer emit flag **and** child marker (`00 00 <int32>`), which the server reads as `flag=0, Int32 shifted by one byte` — a silent wrong value for currently-correct data. Multi-field Tuples fail today for the mirror reason (`ATTEMPT_TO_READ_AFTER_EOF`).
3. A correct Tuple fix needs the **declared** element nullability, which exists only in the raw parent type string. It must be parsed in `Column.extractColumn` (`splitUnlessInBrackets` already exists, L341-366) and consulted in the writer's TUPLE case (L747-770), not in `Table.java` (upstream-identical; `TableTest` is upstream and must stay unmodified).
4. `validateDataSchema` (L285-) uses `col.isNullable()` too: after the Enum fix, a record that omits a `Nullable(Enum)` field passes validation and writes NULL, where today it fails "Table column name [x] was not found". This is a legal improvement but it is an operator-visible behaviour change; list it in the rollout note.

**Tests that pin today's behaviour and must be flipped or rewritten (never deleted, per AGENTS.md "only add"):**

| Test | What it pins | Disposition |
|---|---|---|
| `sink/db/NestedNullableCompositeWriteTest` L264-280 (D-16, `premise: extractColumn drops Tuple nullability`) | `Array(Nullable(Tuple(a Int32)))` null element throws | Stays valid until the Tuple fix; then re-pin to the new contract (null element → NULL) |
| `sink/NestedNullRowBinaryLiveTest` L417-440 (D-16) | Same, live | Same |
| `NestedNullRowBinaryLiveTest` / `NestedNullableCompositeWriteTest` Enum-related L-13 pins | `Array/Map(Nullable(Enum8/16))` null entries throw | Flip with the Enum fix (null entry → NULL) |
| `sink/db/helper/ClickHouseHelperClientForkUnitTest` L264-324 | `hasNullableEnumOrTuple` pattern and warn-once (D-02) | Narrow to Tuple-only after Enum fix; delete rows only if the code is deleted — then keep an equivalent regression (see anchors) |
| `sink/db/helper/ClickHouseHelperClientForkLiveTest` L395-433 | describeTable registers the WARN for `e`, `m`, `t` | Same |
| `ColumnTest` L176 (upstream, byte-identical) | `Enum8` extraction | Stays green: it asserts type and values, not nullability. Add a **new fork-only** `ColumnNullabilityTest` (pattern: `ColumnArrayRecursionTest`, Docker-free) |

**Companion code that is removed or narrowed by 999.1:** `ClickHouseHelperClient.java` L490-527 (`NULLABLE_ENUM_OR_TUPLE` pattern, WARN text, `WARNED_NULLABLE_ENUM_OR_TUPLE` set, `hasNullableEnumOrTuple`, `warnOnceIfNullableEnumOrTuple`) and the two call sites L579, L650. After the Enum fix the regex `Nullable\((?:Enum8|Enum16|Tuple)\(` must lose `Enum8|Enum16`; after a full Tuple fix the whole block is deleted. **15-REVIEW IN-01..IN-05 are all about this block and its tests**, so they are resolved by 999.1, not separately: IN-01 (WARN fires on routes where the defect cannot occur), IN-03 (unevicted, delimiter-ambiguous key), IN-04 (wording unpinned), IN-05 (constants mid-class) disappear on deletion, or are fixed in passing on narrowing; IN-02 (V1-only hook) disappears with the hook. **IN-06** (`MetadataFreeJsonWriterTest` L325-336) is independent.

**Anchors and docs that must move in the same commit:** `.planning/scripts/fork-anchors.txt` row `ClickHouseHelperClient.java  static boolean hasNullableEnumOrTuple(  1  PUB-02` (L57); `docs/FORK-DELTA.md` rows for the WARN (L43), `Column` (L44), Known costs item 1; behaviour ledger L-11, L-13, L-21; ROLLOUT item 4 (its "affected columns" query).

### Empty-CDC skip (13-REVIEW WR-01)

`DebeziumRecordConvertor` L70-100 returns an empty `jsonMap` for `op=t`, `op=null` and a null before/after struct. `ClickHouseWriter.writeRecordsJson` (L1353-1405) is the only place that skips them, and only inside the `metadataFree && DEBEZIUM_CDC` branch (L1365-1377). The `else` branch (L1378-1394) builds `data` from `getJsonMap()` (L1387-1389), `cleanupExtraFields` returns `{}`, and `{}` is written as an all-defaults row (id 0, `_version` 0, `is_deleted` 0 into a ReplacingMergeTree).

- **One shared helper, hoisted above the three-way branch**, exactly as 13-REVIEW WR-01 proposes: `if (record.getSchemaType() == DEBEZIUM_CDC && isEmptyCdc(record)) { log; continue; }` placed immediately after the tombstone check at L1355. Extract `isEmptyCdc` (null-or-empty `getJsonMap()`) and the DEBUG log into one private method so the two routes cannot drift again.
- **Do not call `shouldSkipTableMetadata()` in the helper.** `fork-anchors.txt` pins that string at exactly 8 occurrences in `ClickHouseWriter.java` (JSONP-01); an extra call changes the count and fails the invariant script at that commit.
- **A third route has the same defect and WR-01 does not name it:** default `DEBEZIUM_CDC` + RowBinary (`doInsertBatch` L237-241 → `doInsertRawBinary` → `writeRecordsRowBinary` L1275-1290). `validateDataSchema` is skipped for CDC (L1224, L1301) and every `fieldExists` is false, so an empty map writes an all-defaults row there too. **Decision for requirements:** (a) skip it in `writeRecordsRowBinary` as well (consistent, same helper, small), or (b) pin it as a known asymmetry in the ledger. The milestone text only covers the JSON route; recommend (a) behind the same helper, because the exactly-once invariant is unaffected either way (the skip is a pure function of the record, so a replay builds the identical batch).
- **All-skipped batches:** an insert whose rows are all skipped sends an empty body. `ExplicitJsonInsertPathLiveTest` already pins an all-tombstone batch on the explicit route; add the all-empty-CDC case for the bypass route.
- **Tests today:** `ExplicitJsonInsertPathLiveTest`, `MetadataFreeJsonInsertUnitTest`, `DebeziumRecordConvertorTest` (op=t cases). Add a live test for `bypassRowBinary=true` + `debeziumCDCEnabled=true` + an `op=t` record that reads back zero rows, and a twin-parity assertion against the metadata-free route.
- **Docs:** FORK-DELTA metadata-free row (L35, "(L-06)"), Known costs item 2 (closed), ledger L-03/L-06.

### WR-03 — `doWriteCol` message-less exception

`ClickHouseWriter.java` L1092-1094:
```java
LOGGER.error("Column {} is not nullable and no value is provided", name);
throw new RuntimeException();
```
Replace with a data-free `DataException` naming the column and type, built from `Column` metadata only (same rule as `buildNestedNullMessage`, L565: the text reaches Connect status, DLQ headers and JMX, so it must never carry a value). The sibling at L1056/L1071 (`An attempt to write null into not nullable column`) is a different branch (value present but null) and stays as is (L-12). Reachability: since COLS-07 a missing `Array(Tuple)` field with no default hits it, and the TUPLE case calls `doWriteCol(innerData, containsKey, column, stream, false)` at L768 for every tuple field. No existing test asserts the bare `RuntimeException`; a new unit test (pattern: `NestedNullableCompositeWriteTest.assertNestedNullThrows`) should assert `DataException`, the column name, and the absence of record values. Operator-visible effect: the root-cause type behind `put()` changes from `RuntimeException` to `DataException`; note it in the rollout doc.

### WR-04 — `getMessage()` null check order

`ClickHouseWriter.java` L1176-1178, inside `doInsertRawBinary`'s `catch (Exception e)`:
```java
Optional<String> updateTableException = UPDATE_TABLE_EXCEPTION_STR_TO_ERROR_CODE.keySet().stream()
        .filter(code -> e.getMessage().contains(code)).findFirst();   // NPE when message is null
if (e.getMessage() != null && updateTableException.isPresent() && retry) {
```
Read `e.getMessage()` once into a local, short-circuit to `Optional.empty()` when null, then test `isPresent() && retry`. **WR-03 and WR-04 are coupled:** the message-less `RuntimeException` of WR-03 is exactly an exception with a null message, and on client V1 `writeRecordsRowBinary` runs in the caller's thread (`doInsertRawBinaryV1` L1330-1335, not inside a client future), so today it can reach this block unwrapped and the NPE replaces the real error. V2 wraps via the client future, which is why the suite never saw it. Fix both in one plan; test WR-04 Docker-free by calling `doInsertRawBinary` with a subclass whose V1/V2 hook throws `new RuntimeException()` (the class already exposes protected `doInsertRawBinaryV1/V2`, so a spy needs no production change).

### T-13-19 — raw DateTime values in two upstream ERROR logs

`ClickHouseWriter.doWriteDates` (starts L412): L461 `LOGGER.error("Error parsing date time string: {}", value.getObject());` (DateTime, STRING branch) and L515 `LOGGER.error("Error parsing date time string: {}, exception: {}", value.getObject(), e.getMessage());` (DateTime64, STRING branch). **L515 leaks twice:** `e` is a `DateTimeParseException` whose `getMessage()` embeds the parsed text (`Text '...' could not be parsed`). Mask both: log column name (`columnName` is already a parameter) and Kafka field type, and log the exception class only, never `getMessage()`. Keep the `unsupported = true` flow and the `DataException` at L525 unchanged (it is already data-free). Both lines are byte-identical to upstream v1.7.0, so this is a fork patch in an upstream region: it must get a FORK-DELTA row and a Docker-free test that captures log output for an unparseable string and asserts the secret substring is absent (no log-capture helper is documented; check `src/test` for an existing appender before adding one).

### Lz4RoundTripLiveTest response-leg assertion (14 WR-01)

`sink/db/helper/Lz4RoundTripLiveTest` owns its own `com.clickhouse.client.api.Client` (L49-62, `compressClientRequest(true)`, `compressServerResponse(true)`, `useHttpCompression(false)`, `serverSetting("network_compression_method","lz4")`), not `ClickHouseHelperClient`, so it is independent of how `setCompressionMethod` is set in production (`ClickHouseHelperClient` L117, L150, L193-196, builder L846/L917). Today it asserts the request leg only (`ProfileEvents['CompressedReadBufferBlocks'] > 0`, ~L167-172). The response leg is proved only by read-back, which passes even if `compress=1` is silently dropped.

Recommended design (MEDIUM — needs a short spike): the review's `query_log` bytes comparison is fragile (`NetworkSendBytes` also counts HTTP framing). A deterministic wire-level alternative: issue one raw `java.net.http` GET to the same endpoint with `compress=1` and `network_compression_method=lz4` for a highly compressible `SELECT`, read the response body, and assert the first 16 bytes are a checksum followed by LZ4's method byte `0x82` and a compressed size far below the uncompressed size, while the same query with `compress=0` returns plain text. That asserts the server compressed the response with LZ4 independent of the client under test, and the existing read-back through client-v2 then proves the client's decompression path. If the spike fails, the review's fallback is explicit: narrow the `@DisplayName` and Javadoc to "request leg verified, response leg by read-back" — never leave the "both legs" claim unbacked. The file is also where the 999.2 pin could live, but put the 999.2 pin in a **new Docker-free class** so this file stays Docker-tagged and the two items do not serialize on it.

### 999.2 — duplicate `org.lz4:lz4-java:1.8.0` on `testRuntimeClasspath`

Re-measured now: `dependencyInsight --configuration testRuntimeClasspath --dependency lz4-java` shows `at.yawk.lz4:lz4-java:1.12.0` (selected by conflict resolution with client-v2's `1.10.2`) **and** an unredirected `org.lz4:lz4-java:1.8.0` whose only parent is `org.apache.kafka:kafka-clients:7.9.1-ccs`, the Confluent build pulled by `kafka-schema-registry*` and the Confluent converters. The capability rule at `build.gradle.kts` L59-72 (`configurations.all { ... withCapability("org.lz4:lz4-java") }`) redirects it on `runtimeClasspath` and `integrationTestRuntimeClasspath` but not here, because the Confluent module is a POM-based module that does not declare the capability and Gradle does not treat the two as conflicting. The rule cannot be fixed by tweaking the selection closure.

Fix options, in order of preference:
1. `dependencySubstitution` scoped to the test configurations: `substitute(module("org.lz4:lz4-java")).using(module(libs.lz4.java.get().toString()))`. It shows up as a redirect in `dependencyInsight`, keeps one jar, and survives the next Confluent bump.
2. `configurations.testRuntimeClasspath { exclude(group = "org.lz4", module = "lz4-java") }`. Simpler; relies on at.yawk staying on the path from client-v2 (it does).

Constraints: do not repeat the literal `withCapability("org.lz4:lz4-java")` anywhere — `fork-anchors.txt` pins it at count 1 (BLD-09). Pin the fix with a Docker-free test that asserts `ClassLoader.getResources("net/jpountz/lz4/LZ4Factory.class")` yields exactly one URL and it contains `at.yawk.lz4`; `Lz4RoundTripLiveTest.lz4FactoryLoadsFromTheMaintainedFork` already checks the code source but would not notice a second jar further down the path. Run `dependencyInsight` on `testRuntimeClasspath`, `runtimeClasspath`, `integrationTestRuntimeClasspath` and `clickhouseDependencies` in the plan's verification (all four must show a single headline).

### CI hygiene

| Item | Where | Notes |
|---|---|---|
| Node 24 majors | `tests.yaml` L28 (`checkout@v4`), L62 and L70 (`upload-artifact@v4`) | Checked 2026-10-10: latest `actions/checkout` v7.0.1 and `actions/upload-artifact` v7.0.2 both declare `using: node24`; `setup-java` v6.0.1 and `gradle/actions/setup-gradle` v6.4.0 are already node24 |
| Runner pin | L24 `runs-on: ubuntu-latest` → `ubuntu-24.04` | `ubuntu-latest` moves to Ubuntu 26 from 2026-10-19 (nine days out). This is the only time-boxed item in the milestone |
| SHA pinning | every `uses:` in `tests.yaml` (4 distinct actions, 5 uses) | Resolve with `git ls-remote https://github.com/<repo> 'refs/tags/<tag>' 'refs/tags/<tag>^{}'`. **`gradle/actions` v6.4.0 is an annotated tag**: `b9bee63e…` is the tag object, the commit to pin is the peeled `3f5f9ada…`. The other three tags are lightweight (`checkout` `3d3c42e5…`, `upload-artifact` `cf430e03…`, `setup-java` `de7274f0…`). Keep a trailing `# vX.Y.Z` comment on each pin |
| `permissions:` | new top-level block | `contents: read` is sufficient for checkout, setup-java, setup-gradle (dependency-graph submission is off by default) and upload-artifact (uses the runtime token). A workflow-level block cannot widen anything, so it is safe for `test-head.yaml` too |
| Fork-count step | `tests.yaml` L39-48 duplicates the formula at `build.gradle.kts` L201-205 | Replace with a Gradle-reported value: add a tiny `reportTestForks` task in `build.gradle.kts` that prints `tasks.test.get().maxParallelForks` and `Runtime.getRuntime().availableProcessors()`, and have the step run `./gradlew -q reportTestForks` and feed `$GITHUB_STEP_SUMMARY` and the `::notice`. This keeps the pre-test timing and the "readable without admin" annotation (CI-09) while reading the very value the `Test` task uses (Gradle's JVM, so container-aware). Drop the dead `CLICKHOUSE_CLUSTER_NAME` branch |
| Lombok warning | `lombok.config` (fork-owned, 2 lines) | `ClickHouseFieldDescriptor.java:18` uses `@Jacksonized`; Lombok 1.18.44+ warns "Ambiguous: Jackson2 and Jackson3 exist". Add `lombok.jacksonized.jacksonVersion += 2`. Not a source change, so no FORK-DELTA conflict risk |

**Anchors that the workflow edit must not break:** `fork-anchors.txt` row `.github/workflows/tests.yaml  timeout-minutes: 45  1  CI-05`. FORK-DELTA's CI row (L51) describes the workflow in detail and must be rewritten in the same plan.

**Out-of-scope observation (verify, do not fix here):** `.github/workflows/test-head.yaml` calls `./.github/workflows/tests.yaml` as a reusable workflow with `with:` inputs and `secrets: inherit`, but `tests.yaml` declares only `on: push` (no `workflow_call`, no inputs). It is scheduled nightly, so on the fork's default branch it very likely errors every night. Other workflows (`release`, `pre-release`, `integrationTests`, `increment-version`) still use `@v3`/`@v2` actions. The milestone text says "every action SHA-pinned": recommend scoping that to `tests.yaml` (the only push-triggered workflow, the one T-14-16 was about) and recording the others in FORK-DELTA Known costs, rather than rewriting four dispatch-only upstream workflows.

---

## New vs Modified Files

### Modified (`src/main`, `build`, `CI`)

| File | Items | Risk |
|---|---|---|
| `src/main/.../sink/db/ClickHouseWriter.java` | WR-03 (L1093-94), WR-04 (L1177-78), T-13-19 (L461, L515), empty-CDC (L1355-1377 + optional L1275-1290), 999.1 Tuple (L747-770) | **High-risk file** (AGENTS.md). Four disjoint regions; serialise |
| `src/main/.../sink/db/mapping/Column.java` | 999.1 Enum (L209), 999.1 Tuple (L232-236 + declared-nullability parse) | Medium; upstream file, 2 fork lines today |
| `src/main/.../sink/db/helper/ClickHouseHelperClient.java` | 999.1 consequence: narrow or delete L490-527, call sites L579, L650 | Medium |
| `build.gradle.kts` | 999.2 (near L59-72), `reportTestForks` task (near L200-205) | Medium; fork re-applied additions live here |
| `.github/workflows/tests.yaml` | Node 24, runner pin, SHA pins, `permissions:`, fork-count step | Low code risk, cannot be tested locally |
| `lombok.config` | Jacksonized version | Low |
| `VERSION` | `v1.7.0_gg2` (needs `clean` build; anchor row `VERSION v1.7.0_gg1 1 BLD-07` moves in the same commit) | Low |
| `CHANGELOG.md`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg1.md` | docs (see below) | Review before the approval gate (Phase 15 lesson) |
| `.planning/scripts/fork-anchors.txt` | `hasNullableEnumOrTuple(` row, `VERSION` row, any new anchor | Must move with the code commit |

### Modified tests (additive only; upstream test files stay byte-identical)

`NestedNullableCompositeWriteTest`, `NestedNullRowBinaryLiveTest` (flip D-16 and L-13 pins), `ClickHouseHelperClientForkUnitTest`, `ClickHouseHelperClientForkLiveTest`, `Lz4RoundTripLiveTest`, `MetadataFreeJsonWriterTest` (IN-06), `ExplicitJsonInsertPathLiveTest` or a new sibling for the bypass-route CDC skip.

### New files

| File | Purpose |
|---|---|
| `src/test/.../sink/db/mapping/ColumnNullabilityTest.java` | Docker-free: Enum8/16 and (after the spike) Tuple nullability, including nested `Array(Nullable(Enum8))`; extends nothing so it stays in `testUnit` |
| `src/test/.../sink/NullableEnumRoundTripLiveTest.java` (name free) | Live typed read-back for top-level and nested `Nullable(Enum8/16)` with value, NULL and absent-field cases; plus a Tuple twin if it ships |
| `src/test/.../sink/db/ClickHouseWriterErrorPathTest.java` (name free) | WR-03 `DataException` and WR-04 null-message path, Docker-free |
| `src/test/.../sink/db/DateTimeParseLogMaskingTest.java` (name free) | T-13-19: secret substring absent from captured logs |
| `src/test/.../build/Lz4SingleJarClasspathTest.java` (name free) | 999.2 pin, Docker-free |
| `src/test/.../sink/CdcEmptyRecordBypassRouteLiveTest.java` (name free) | WR-01 live pin |
| `docs/ROLLOUT-v1.7.0_gg2.md` | Operator note for the behaviour changes below (or a clearly marked addendum to the gg1 doc) |
| `.planning/phases/16..20-*/` | GSD artifacts |

`MetadataFreeJsonWriter.java`, `Table.java`, `Processing.java`, `KeyToValue.java`, `DebeziumRecordConvertor.java` and every upstream test class are **not** modified by any item.

---

## Data Flow Changes

```
BEFORE  DESCRIBE row 'e Nullable(Enum8(...))' -> Column{Enum8, isNullable=false}
        -> doWriteCol: no marker -> value byte lands where the server expects the flag
        -> stored NULL / shifted, no error                                     (silent wrong value)
AFTER   -> Column{Enum8, isNullable=true} -> [flag][int8], NULL -> [01]       (round-trips)

BEFORE  Nullable(Tuple(a Int32)) works only because child marker == tuple flag
AFTER   (only with declared-nullability parsing) -> [flag][plain children], null -> [01]

BEFORE  DEBEZIUM_CDC op=t on bypassRowBinary=true -> data={} -> cleanup -> '{}' -> all-defaults row
AFTER   skipped before serialisation on every CDC route (one helper, replay-deterministic)

BEFORE  missing non-nullable Array(Tuple) field -> RuntimeException() (no message) -> on V1: NPE in catch
AFTER   DataException(column '<n>' (<type>) is not nullable, has no default, no value provided)

BEFORE  unparseable DateTime string -> ERROR log contains the record's raw value (+ parse exception text)
AFTER   ERROR log carries column name, field type, exception class only
```

**Exactly-once invariant check (AGENTS.md):** none of the changes touch `Processing`, `KeeperStateProvider`, `RangeContainer` or batch composition after `BEFORE`. The empty-CDC skip and the Enum marker change alter *bytes of a batch*, but both are pure functions of the record plus the cached `Table`, so a retry after a crash builds the identical block and ClickHouse dedup still holds. The one subtle case is a `Table` cache refresh between the first attempt and the retry changing `isNullable` on an Enum column mid-flight (a schema change, which already breaks dedup for any column). Note it; do not engineer around it.

---

## Patterns to Follow

### Pattern 1: anchor-with-code
Any change to a pinned construct updates `fork-anchors.txt` in the same commit (the script evaluates every commit since `pre-v1.7.0-sync`; a later "fix-up" commit does not rescue an earlier red one). For this no-sync milestone run it as `verify-merge-invariants.sh 54e63648 HEAD` at the end of each phase (invariants 0-2 are sync-shaped and already green on that merge; invariant 3 is the live one).

### Pattern 2: fork-only test classes beside upstream ones
Never edit `ColumnTest`, `TableTest`, `ProcessingTest` and other byte-identical upstream tests. Add `Column*Test` / `*LiveTest` classes (precedent: `ColumnArrayRecursionTest`, `NestedNullRowBinaryLiveTest`). Docker-free classes extend nothing; ClickHouse-backed ones extend `ClickHouseBase`.

### Pattern 3: data-free messages
Anything that reaches logs, Connect status, DLQ headers or JMX is built from `Column` metadata only (`buildNestedNullMessage` L565). WR-03 and T-13-19 follow it.

### Pattern 4: one predicate, one helper per decision
`shouldSkipTableMetadata()` is the model: the empty-CDC decision becomes one private `isEmptyCdc`-style helper reused by `writeRecordsJson` (and `writeRecordsRowBinary` if option (a) is chosen), not two inline copies.

## Anti-Patterns to Avoid

1. **Setting `isNullable` on `Tuple` without parsing declared child nullability.** Silent corruption of currently-correct single-field tuples (see 999.1). Do not ship the Tuple half without the live wire tests from the probe table above (flat, declared-nullable child, `Array(Nullable(Tuple))`, `Map(K, Nullable(Tuple))`, null element, multi-field).
2. **Editing `Table.java` to adjust tuple children.** It is adopted wholesale and byte-identical to upstream; any edit makes the next sync a conflict.
3. **Fixing 999.2 inside the capability closure.** The closure already runs; the Confluent module never enters the conflict. Substitute or exclude instead.
4. **Pinning an annotated tag's tag-object SHA.** `gradle/actions` v6.4.0 needs the peeled commit.
5. **Verifying CI edits only locally.** Workflow behaviour needs a real run; plan one approved scratch-branch push.
6. **Asserting tests with `countRows`.** Project rule: typed read-back via `RowSnapshots`.

---

## Build Order

### Execution model constraint
This repo runs executors **sequentially on one working tree** (dispatch isolation `none`; see the sentinel note in the project memory), so "parallel phases" means *order-independent*, not concurrently executed. The cost to minimise is gate repetition and re-touching the same lines, not git merge conflicts.

### Shared-file conflict matrix

| Shared file | Items that touch it | Rule |
|---|---|---|
| `ClickHouseWriter.java` | WR-03, WR-04, T-13-19, empty-CDC, 999.1 Tuple | Disjoint regions, but one high-risk file: one phase for the four small fixes, then 999.1 after it |
| `docs/FORK-DELTA.md` | every code item (each patch must be recorded) | Each plan edits only its own rows; the raw-numbers refresh is **release-only** |
| `.planning/scripts/fork-anchors.txt` | 999.1, VERSION, any new anchor | Same commit as the code |
| `build.gradle.kts` | 999.2, `reportTestForks` | Same plan or adjacent plans in one phase |
| `Lz4RoundTripLiveTest.java` | response-leg only (999.2 pin goes to a new class) | No serialisation needed |
| `ClickHouseHelperClient*Test` + `NestedNull*Test` | 999.1 only | One phase |
| `CHANGELOG.md`, `VERSION` | release only | Last |

### Recommended phases (numbers continue from Phase 15)

1. **Phase 16 — Early documentation publication (docs-only).** User-decided first. `--no-ff` merge of `sync/v1.7.0` into `custom_build` so `505e4981` (corrected rollout doc, FORK-DELTA L-05) reaches `origin`. Recommend folding **15-REVIEW IN-07..IN-10** (docs-only: ROLLOUT item 2 `exactlyOnce` qualification and closing sentence, item 8 re-merge consequence of `git revert -m 1`, FORK-DELTA stale "future tag" wording and skip-count command, planning-internal IDs in operator text) in *before* the merge: they are the same files, and the Phase 15 lesson is to review operator docs before the approval gate. Gate: no full suite needed (tree outside `docs/`, `CHANGELOG.md` and `.planning/` is identical to the gated `f328fd21` tree; assert it by command). Exit: one green `tests.yaml` run on the merge SHA.
2. **Phase 17 — CI, build and test-infra hygiene.** `tests.yaml` (Node 24, `ubuntu-24.04`, SHA pins, `permissions:`, Gradle-reported fork count), `lombok.config`, 999.2 + its Docker-free pin, `Lz4RoundTripLiveTest` response leg, IN-06. No `src/main` change, so none of it can regress behaviour. Do it second because of the 2026-10-19 runner switch and because the later phases' CI evidence should come from the hardened workflow. Gate: `testUnit`, targeted live classes, `compileIntegrationTestJava`, and **one approved scratch-branch push** to watch the new workflow run (precedent D-13; push the branch only, explicit refspec, no tags; delete it afterwards, and never have two branches at one SHA).
3. **Phase 18 — Writer correctness I (small `ClickHouseWriter` fixes).** Plans in order: WR-03 (message), WR-04 (null check, same area, shared test), T-13-19 (log masking), empty-CDC skip (helper + live pin; decide the RowBinary-route question first). Each plan is independent of the others; sequence only because of the shared file. Gate: full `./gradlew test` on `latest` × V2 once at phase end (cache-busted), anchors script.
4. **Phase 19 — 999.1 `Nullable(Enum8|Enum16|Tuple)`.** Plan 1 is a **spike pinned to the probe table above** (DESCRIBE shape and wire bytes for each form, recorded in a Recorded Fact). Plan 2 ships the Enum fix with flipped pins, the WARN narrowed to Tuple, IN-01..05 disposition. Plan 3 is the Tuple fix with declared-nullability parsing **or** an explicit decision to keep Tuple fail-loud plus WARN and carry it as a Known cost. Make that a go/no-go at the end of the spike so the release is not held hostage to Tuple. Last code phase because it is the riskiest and depends on Phase 18's `doWriteCol` message. Gate: full suite on `latest` × V2 (cache-busted), anchors script, a live matrix over flat, nested, Array and Map slots with typed read-back.
5. **Phase 20 — Release and publication.** `VERSION=v1.7.0_gg2` (+ `clean`), CHANGELOG, `ROLLOUT-v1.7.0_gg2.md` (docs reviewed *before* the gate), FORK-DELTA raw numbers refreshed with the four documented commands, ledger L-23.. entries. Gates: two consecutive full runs (`clean test`, then `test --rerun`), `testUnit` with Docker stopped, anchors script. Then the publication runbook below.

**What can be reordered freely:** Phase 17 and Phase 18 share no `src` file and could swap. **What must serialise:** 16 first (user decision); 18 before 19 (shared file and `doWriteCol`); 20 last. Do not interleave 999.1 into Phase 18 — a failed Tuple spike would block four unrelated fixes behind it.

### Publication runbook (derived from Phases 11 and 15 and the memory notes)

**Phase 16 (docs-only):**
1. Pre-flight by command: `git diff --name-only f328fd21 sync/v1.7.0 -- . ':!.planning'` lists only `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg1.md` (plus `CHANGELOG.md` if IN-10 is folded in); `.planning/.gsd-ws-arg` untracked and absent from the tree; commit or stash the modified `.planning/state.json` first, or merge from a clean worktree.
2. Merge in a throwaway worktree so the main tree stays on `sync/v1.7.0`: `git worktree add /tmp/gsd16/cb custom_build`, `git merge --no-ff -F <msg> sync/v1.7.0` (message names the files, the review IDs, and `git revert -m 1`). Merge base is `ae8520ed` (second parent of `f328fd21`), so the result tree equals the `sync/v1.7.0` tip tree; assert `git diff --quiet <merge> sync/v1.7.0`.
3. Approval gate: **orchestrator-inline**, not a dispatched executor (executors refuse relayed approvals). Present the diff stat and the docs review result; wait for the user's direct answer.
4. Push `refs/heads/custom_build:refs/heads/custom_build` only, no force, no tags (`push.followTags` unset). It worked as an owner push for `c61d90ce` and `f328fd21`; confirm protection rules allow it before the gate rather than discovering it after.
5. Wait for exactly one `tests.yaml` run on that SHA (REST API, unauthenticated; `gh` is not installed). Success is `conclusion=success` with none cancelled. No tag is pushed; `v1.7.0_gg1` (`f328fd21`) remains the rollback anchor.
6. `git fetch --no-tags origin`; keep working on local `sync/v1.7.0`. Post-publication `.planning` commits stay local (D-14) until Phase 20.

**Phase 20 (release):** same shape plus a rehearsal and a tag. Push `sync/v1.7.0` once to `origin` as a scratch branch (explicit refspec, approval) and wait for green; merge into `custom_build` with `--no-ff` from the throwaway worktree (second parent = the rehearsed SHA; the merge-tree assertion `git diff --quiet <merge> <rehearsed>`); push `custom_build`; wait for its single run; **only then** push the tag by name (`refs/tags/v1.7.0_gg2`), because a tag onto an already-pushed SHA used to cancel the branch run (the branch-only trigger now prevents a new run, but verify); re-check the SHA has exactly one non-cancelled run; delete the scratch branch from `origin`. The old `pre-v1.7.0-sync` tag still runs the old workflow if re-pushed; do not re-push it. The release's rollback point is the Phase 16 merge commit (`git revert -m 1 <release merge>`), no new `pre-` tag needed.

---

## Behaviour Changes the Release Must Disclose (feeds `ROLLOUT-v1.7.0_gg2.md`)

| Change | Who is affected | Self-check |
|---|---|---|
| `Nullable(Enum8/16)` slots now store the right value and NULL; rows already stored wrong are not repaired | Any table with a `Nullable(Enum)` column written through RowBinary | The existing ROLLOUT item-4 query for affected columns |
| A record omitting a `Nullable(Enum)` field now writes NULL instead of failing validation | Schema'd pipelines relying on the failure | Same query |
| (If Tuple ships) `Nullable(Tuple)` null/multi-field behaviour changes | Tables with `Nullable(Tuple)` | Same query, plus a read-back of a sample |
| Missing non-nullable `Array(Tuple)` field now a `DataException` naming the column (was message-less `RuntimeException`) | DLQ and Connect status consumers | Grep status/DLQ headers |
| Empty CDC records (`op=t`, null before/after) no longer write phantom rows on `bypassRowBinary=true` (and the RowBinary CDC route if option (a)) | Debezium pipelines with `debeziumCDCEnabled=true` | Count rows with all-default keys in the target table; cleaning old phantoms is a manual step |
| DateTime parse-failure ERROR logs no longer include the value | Log-based alerting on that text | Grep for `Error parsing date time string` |
| MBean `version=` segment is now `v1.7.0_gg2` | JMX scrapers with exact `version=` matchers (see gg1 item 7 for the regex caveat) | Existing item-7 check |

---

## Scaling Considerations

Not applicable: no throughput-affecting change. The one hot-path edit (empty-CDC helper in the per-record loop, and the Enum marker write) adds one null/empty check and one byte per nullable Enum value; stay allocation-free (no `Optional`/lambda in the per-record path, per AGENTS.md hot-path rules).

---

## Open Questions for Requirements and Roadmap

1. **Tuple scope.** Ship Enum only (safe, one line) and carry Tuple as a Known cost, or fund the declared-nullability parser? Recommendation: spike first; ship Enum unconditionally; make Tuple a go/no-go.
2. **Third CDC route.** Include `writeRecordsRowBinary` in the empty-CDC skip (recommended) or only the JSON route as worded?
3. **IN-07..IN-10 timing.** Fold into Phase 16 (recommended) or leave for the release phase.
4. **SHA-pin scope.** `tests.yaml` only (recommended) or every workflow.
5. **`test-head.yaml` is likely broken** (reusable-workflow call to a workflow without `workflow_call`). Confirm in the Actions UI; fix or delete in a later milestone.
6. **Response-leg assertion design** needs a 30-minute spike (raw HTTP `compress=1` and the LZ4 method byte) before planning; fall back to narrowing the claim.
7. **Local `ClickHouse latest` drift.** A red full suite on unchanged code means check the image date first (the 26.9 ZSTD break precedent); the probe used 26.9.14.10.

## Sources

- Live source at `sync/v1.7.0` `9ff1affc`: `Column.java`, `ClickHouseWriter.java`, `ClickHouseHelperClient.java`, `Table.java`, `DebeziumRecordConvertor.java`, `Utils.java`, `tests.yaml`, `test-head.yaml`, `build.gradle.kts`, `lombok.config`, `gradle/libs.versions.toml`, `Lz4RoundTripLiveTest.java`, `.planning/scripts/fork-anchors.txt` (HIGH)
- `docs/FORK-DELTA.md`, `.planning/PROJECT.md`, `.planning/ROADMAP.md` backlog 999.1/999.2 (HIGH)
- `.planning/milestones/v1.7.0_gg1-sync-phases/13-*/13-RESEARCH.md` Spike b2/b3, `13-REVIEW.md` WR-01/03/04/05, `13-SECURITY.md` T-13-19, `14-REVIEW.md` WR-01/02, `14-SECURITY.md` T-14-16, `14-RESEARCH.md` Pitfall 2, `15-REVIEW.md` IN-01..10, `15-08-SUMMARY.md` (publication mechanics) (HIGH)
- Live ClickHouse probe, `clickhouse/clickhouse-server:latest` = 26.9.14.10, `clickhouse-local` DESCRIBE and RowBinary dumps for the four forms in the 999.1 table, 2026-10-10 (HIGH, reproducible)
- `./gradlew dependencyInsight --configuration testRuntimeClasspath --dependency lz4-java`, 2026-10-10 (HIGH)
- GitHub: `actions/checkout` v7.0.1, `actions/upload-artifact` v7.0.2, `actions/setup-java` v6.0.1, `gradle/actions` v6.4.0 — release API, `git ls-remote` tag SHAs, and `action.yml` `using:` values fetched 2026-10-10 (HIGH for versions and SHAs; re-resolve at execution time)
- Lombok `@Jacksonized` Jackson 2/3 ambiguity warning and `lombok.jacksonized.jacksonVersion` ([Project Lombok changelog](https://projectlombok.org/changelog), [OpenRewrite recipe](https://docs.openrewrite.org/recipes/java/jackson/lombokjacksonizedconfig)) (MEDIUM: confirm the exact option spelling against Lombok 1.18.46 when implementing)
- Project memory: Phase 11 and 15 orchestration gotchas, dispatch-isolation sentinel, colima `vz` profile

---
*Architecture research for: v1.7.0_gg2-hardening integration map and build order*
*Researched: 2026-10-10*
