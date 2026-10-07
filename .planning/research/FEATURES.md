# Feature Research

**Domain:** Brownfield fork maintenance of a Kafka Connect ClickHouse sink (Java 17). Milestone `v1.7.0_gg1-sync`: upstream `v1.7.0` sync plus 17 open follow-ups plus publication.
**Researched:** 2026-10-07
**Confidence:** HIGH for everything read from code at `HEAD` (`94d98615`) or `git show v1.7.0:<path>` (`379b8d01`). MEDIUM where marked (behaviour predicted from library defaults, not yet observed live).

All `file:line` references are to the current tree (`custom_build` + v1.6.0 sync) unless prefixed `v1.7.0:`. "Writer" means `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`.

## Findings that correct or sharpen the milestone brief

The roadmap should treat these as inputs, because each changes scope or ordering.

1. **#800 (Avro record unions) has no `src/main` change.** `git diff v1.6.0 v1.7.0 -- src/main` touches 10 files (CHANGELOG, VERSION, build, `AtLeastOnceBufferStrategy`, `ClickHouseSinkConfig`, `StructToJsonMap`, Writer, `ClickHouseHelperClient`, `KeeperStateProvider`, catalog). #800 landed as fixtures, an integration test and a CHANGELOG line (`git log --stat v1.6.0..v1.7.0`, commits `792efb11`, `f2956fb5`). Nothing to "not adopt" in code. The merge carries test fixtures only.
2. **P3-WR-03 is narrower than recorded.** `INT128`/`UINT128` are now written by RowBinary (Writer:576-581 routes them to `doWritePrimitive`; Writer:916-925 writes them from `BigInteger` or `String`). Only `INT256`/`UINT256` still fall through `validateDataSchema`'s default branch (`colWidth == null`, so `validSchema = false`) and surface the bare `"Data schema validation failed."` (Writer:1139-1142, 1216-1219, 559-565). The fail-fast message applies to `INT256`/`UINT256` only. Telling users `INT128` is unsupported would be wrong.
3. **The fork's own null guards in `case MAP` and `case TUPLE` collide with #820 and are semantically wrong.** Writer:613-621 (`MAP` null -> `writeNull`) sits in the same hunk as upstream's `if (mapTmp == null)` branch (`git diff v1.6.0 v1.7.0`, Writer `@@ -554`). The fork's guard writes byte `0x01` for a null `Map`. ClickHouse does not allow `Nullable(Map(...))` or `Nullable(Tuple(...))` (HIGH from ClickHouse type rules; confirm live in the WR-02 test), so a null `Map` or `Tuple` reaching `doWriteColValue` is always a non-nullable slot, where `0x01` is read as a length or tuple byte and desynchronises the stream. That is WR-02's exact bug class. Resolve `MAP` to upstream's branch; turn `TUPLE` into the WR-02 throw. The `VARIANT` guard (Writer:695-702, writes discriminator 255) is correct and stays.
4. **P9-IN-01's operand swap is cosmetic.** `shouldSkipTableMetadata()` returns `false` for `csc == null` (Writer:1616-1618), so `shouldSkipTableMetadata() || csc.isBypassFieldCleanup()` still dereferences `csc` when it is null. The swap changes evaluation order only. Acceptance must be "behaviour-identical for all non-null `csc`" plus a Javadoc correction, not "null-safe".
5. **`USERNAME` default and `Column.extractColumn` swap are upstream bugs too.** Both are present verbatim in `v1.7.0` (`ClickHouseSinkConfig` line 418-420, `Column.java` line 211). Fixing them grows the fork delta by two lines in upstream-owned files. They are still worth fixing (see below), and are candidates for an upstream PR so the delta shrinks again.
6. **#841 is a larger behaviour change than "retire D-17".** Beyond `network_compression_method=lz4`, `v1.7.0` unconditionally sets V1 `COMPRESS=false` (`v1.7.0` helper client `getDefaultClientOptions`) and V1 node option `compress=false`, and V2 `.compressServerResponse(false)`, independent of the `setCompressionMethod` flag. Server response compression is therefore off for every operator. This is also why `NetworkCompressionPinLiveTest` (class Javadoc, lines 20-25: "answers `compress=1` requests with ZSTD") loses discriminating power: with `compress=false` the ZSTD response never occurs. See the D-17 row.
7. **R-01 routing depends on #820's `StructToJsonMap` fix.** `SchemaRecordConvertor.doConvert` builds `StructToJsonMap.toJsonMap(struct)` eagerly for every `SCHEMA` record (`SchemaRecordConvertor.java:20`), including ones that will only be written as JSON. At `HEAD`, a Struct with a null optional `Map` field throws an NPE there (`StructToJsonMap.java:86`: `new HashMap<>(struct.getMap(fieldName))`). So rerouting Structs into the JSON writer would still lose those records until #820 is merged. This forces "sync before R-01" (already the ordering constraint) and justifies one null-Map case in the R-01 test matrix.
8. **There is a pre-existing upstream asymmetry in nested null containers.** `doWriteColValue`'s `ARRAY` branch writes a marker only `if (defaultsSupport)` (Writer:641-645) and #820's `MAP` branch does the same. At the top level `doWriteCol` supplies the missing byte (Writer:965-966, 980-982), so top level is consistent. For a null `Array`/`Map` element nested inside another container, nothing supplies it when `defaultsSupport == false`. WR-02 has to handle this, not only scalar slots.

## Feature Landscape

### Table Stakes (must ship for `v1.7.0_gg1` to be publishable)

| # | Feature | Why expected | Complexity | Dependencies |
|---|---------|--------------|------------|--------------|
| T1 | Mechanical merge of upstream `v1.7.0` (ref `v1.7.0` = `379b8d01`; never bare `upstream/main`) in the `v1.6.0_gg1-sync` shape | Whole milestone premise. Collision files: Writer, `ClickHouseHelperClient`, `ClickHouseSinkConfig`, `ClickHouseTestHelpers` (+1 line), `build.gradle.kts`, `libs.versions.toml`, CHANGELOG, VERSION | MEDIUM | Must land first: v1.7.0 rewrites the lines that D-17, WR-01/02, P9-IN-01 and R-01 touch |
| T2 | Retire D-17; take upstream's `setCompressionMethod` implementation | See "D-17 retirement" below | MEDIUM | T1 |
| T3 | R-01 fixed by routing | See "R-01 routing" below. Only silent-data-loss item | MEDIUM | T1 (#820 `StructToJsonMap` fix), T2 not required |
| T4 | WR-01 + WR-02 writer null-correctness, reconciled with #820 | See "Null semantics" below | MEDIUM-HIGH | T1 |
| T5 | Metadata/config fixes: P3-WR-01, P3-WR-03 (narrowed), `extractColumn` swap, `USERNAME` default, P9-IN-01 | See follow-up table | LOW each | T1 for P9-IN-01 (adjacent hunk); others independent |
| T6 | Test debt: WR-03, `JsonColumnTest` read-back, P4-WR-01, `ClickHouseBase.tearDown`, IN-01, P4-IN-01 | Core Value: every fork behaviour pinned by a test that actually asserts it | LOW-MEDIUM | T4 for nothing; independent |
| T7 | CI: one `tests.yaml` run per published SHA; fork count read or recorded unmeasured | CI-06 had to filter `conclusion != cancelled` last time | LOW | None |
| T8 | Dependency bumps already user-approved: jackson 2.22.3, okhttp 5.5.0, org.json 20260814, mockito 5.24.0; lz4-java 1.10.4 arrives with the merge | `AGENTS.md` forbids unapproved bumps; this list is the approval. Nothing else is bumped | LOW | T1 (catalog file). Jackson bump sits under the JSON writer, re-run R-01 matrix after it |
| T9 | Publication: `VERSION=v1.7.0_gg1`, CHANGELOG, push, green CI on the published SHA, publish post-`c61d90ce` `.planning` history | Definition of done | LOW-MEDIUM | All |
| T10 | Operator rollout checklist | Three operator-visible changes (MBean version segment, compression, R-01) | LOW | T2, T3 |
| T11 | `docs/FORK-DELTA.md` brought to the new tree, each row re-grepped | The next syncer's only map; seven behaviours vanished silently last time | MEDIUM | All code items |

### R-01 routing: exact scope and observable output (Table stakes T3)

**Mechanism today.** `doInsert` -> `getTable` returns a synthetic, column-less `Table` under `shouldSkipTableMetadata()` (Writer:1620-1623: `new Table(database, mappedName, false, List.of(), 0)`). `doInsertBatch` sends `SCHEMA` and `DEBEZIUM_CDC` records to `doInsertRawBinary` unless `csc.isBypassRowBinary()` (Writer:235-241). `writeRecordsRowBinary` iterates `table.getRootColumnsList()` (Writer:1194), which is empty, so each record serialises to zero bytes. `validateDataSchema` is vacuous for the same reason (Writer:286). The client reports `Written Rows: [0]` and nothing throws.

**Routing rule (the whole change).** In `doInsertBatch`, `case SCHEMA` / `case DEBEZIUM_CDC`: take the JSON branch when `csc.isBypassRowBinary() || shouldSkipTableMetadata()`. One predicate already exists and is already null-safe (Writer:1616-1618); no new config key.

| Config | Record type | Before | After |
|--------|-------------|--------|-------|
| `insertFormat=json` (explicit), `bypassRowBinary=false` | `SCHEMA` (Struct) | RowBinary on empty table, **0 rows, no error** | `doInsertJson` -> `JSONEachRow`, rows land |
| same | `DEBEZIUM_CDC` (only if `debeziumCDCEnabled=true`, which the fork does not use) | same loss | same reroute, for consistency |
| `insertFormat=json`, `bypassRowBinary=true` | `SCHEMA` | JSON (already) | unchanged; **no test pins this combination in metadata-free mode today** (FORK-DELTA.md:120) |
| `insertFormat=json` | `SCHEMA_LESS` (Map) | JSON | unchanged; this is the operator-confirmed production route (MERGE-CHECKLIST Fact 14, fact 2) |
| `insertFormat=json` | `STRING_SCHEMA` | `doInsertString`, raw string as `JSONEachRow` (Writer:1424-1434) | unchanged |
| `insertFormat` unset, `none`, `csv`, `tsv` | any | unchanged | unchanged. `jsonInsertFormatExplicit` is false even though `InsertFormats` resolves to `JSON` through the `"node"` default typo (ClickHouseSinkConfig.java:262-278), so the bypass never widens |
| any, `auto.evolve=true` + explicit json | any | warn and skip evolve (Writer:222-226) | unchanged |

Only the first two rows change behaviour. Per the operator facts, no known production pipeline is in them, so the routing is a no-op for the deployment that motivated the fork and a fix for everyone else.

**How Struct -> JSON works (answer to "via StructToJsonMap").** It does not use `StructToJsonMap`. `writeRecordsJson` reads the `Struct` directly: `for (Field field : struct.schema().fields()) data.put(field.name(), struct.get(field))` (Writer:1277-1283, comment "Doesn't handle multi-level object depth"). The `DEBEZIUM_CDC` branch reads `record.getJsonMap()` (Writer:1284-1287). Nested Structs are handled by Jackson via `JacksonStructSerializer` (`DataJson.java:36-58`), which recurses field by field and writes explicit nulls. `StructToJsonMap` only builds `Record.jsonMap`, used by RowBinary and by `Tuple`; for JSON it is pure eager overhead plus the null-Map NPE in finding 7. `cleanupExtraFields` returns the payload untouched in metadata-free mode (Writer:1404).

**What the user observes after the change.** `{"id":1,"str":"row1"}` per record, `JSONEachRow`, into the `topic2TableMap`-resolved table, with `clickhouseSettings` applied (Writer:1381-1383) and `jsonInsertColumns` applied on client V2 (Writer:1391-1394; V1 ignores it, out of matrix). Differences from RowBinary that the test matrix must pin or document:

| Aspect | JSON-route behaviour | Confidence |
|--------|----------------------|------------|
| Validation | No client-side schema validation (Writer:1328-1329 comment). Type errors come from the server as a `ServerException`, not a `DataException`. Unknown fields skipped by server (already pinned: `ExplicitJsonInsertPathLiveTest.metadataFreeModeInsertsUnknownFieldWithoutThrowing`) | HIGH |
| Top-level nulls | `OBJECT_MAPPER` is `NON_NULL` (`DataJson.java:33`), so a null top-level field is **omitted**, not sent as `null`. Nullable column without `DEFAULT` -> NULL; column with a non-NULL `DEFAULT` -> the default. Nested Struct fields are written as explicit `null` (the Jackson serializer bypasses NON_NULL) | HIGH (code), MEDIUM (server result) |
| `Timestamp` / `Date` / `Time` logical types | `struct.get(field)` yields `java.util.Date`; the default `ObjectMapper` writes epoch **milliseconds**. Correct for `DateTime64(3)`; wrong or erroring for `DateTime` and `Date`, which read a number as seconds or days | MEDIUM: derived from Jackson/ClickHouse defaults, not observed. The R-01 matrix must observe it live |
| `Decimal` | `BigDecimal` -> JSON number; already round-trips (TYPE-05, `JsonDynamicRoundTripTest`) | HIGH |
| `bytes` | `byte[]` -> base64 string | HIGH (Jackson default) |
| Null optional `Map` | omitted, column default (empty map) | HIGH after #820 |

**Done looks like.**
- `ExplicitJsonInsertPathLiveTest.explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch` (lines 278-343) is **renamed and flipped**: 5 records, `assertEquals(5, countRows)` and typed read-back of `id`/`str`. The "R-01 PIN - NOT AN ENDORSEMENT" Javadoc is replaced, not left stale.
- A second test pins `explicit json + bypassRowBinary=true + Struct` (the unpinned combination).
- A mixed-type live matrix under metadata-free mode that mirrors TYPE-05 (`JsonDynamicRoundTripTest` lines 230-410, which already does it with metadata on the `bypassRowBinary=true` route): String, UInt32, Float64, Decimal, Nullable-with-null, a null optional `Map` (ties to #820), `Array`, nested Struct, and `DateTime64(3)`. The `DateTime`/`Date` outcome is **recorded as observed**, not fixed (anti-feature below).
- `MetadataFreeJsonInsertUnitTest` gains a Docker-free assertion that `doInsertBatch` takes the JSON branch under the predicate (the existing class is the canary for the guards, lines 98-250).
- FORK-DELTA.md: Known costs item 1 (lines 114-120) is deleted; the metadata-free row (line 33) is amended; CHANGELOG states the behaviour change.

### D-17 retirement (Table stakes T2): expected end state

| Aspect | Fork D-17 (HEAD) | After taking upstream #841 |
|--------|------------------|----------------------------|
| V2 | `.serverSetting(lz4)` unconditional, before the jdbc-properties loop (`ClickHouseHelperClient.java:185-187`) | Same, but guarded by `setCompressionMethod`; plus `.compressServerResponse(false)` |
| V1 | `CUSTOM_SETTINGS="network_compression_method=lz4"` unconditional (`:114-116`) | Same, guarded; plus `COMPRESS=false` and V1 node option `network_compression_method=lz4` |
| Config | none | `setCompressionMethod`, default `true`, alias `set_compression_method` (`v1.7.0` `ClickHouseSinkConfig`) |
| Escape hatch | operator's `jdbcConnectionProperties` value wins on V2 | same, plus `setCompressionMethod=false` |
| `KeeperStateProvider` | not passed | `.setCompressionMethod(csc.isSetCompressionMethod())` |
| Constants | package-private `NETWORK_COMPRESSION_METHOD_SETTING`/`_LZ4` (`:109-110`) | public `NETWORK_COMPRESSION_METHOD`, `LZ4` |

**Done looks like.** `ClickHouseHelperClient.java` is upstream's for these lines, with the fork comment block at `:106-110` deleted. `NetworkCompressionPinUnitTest` keeps passing: it asserts configuration values through `getClient().getConfiguration()` and `getDefaultClientOptions()`, not the private constant names, so it needs no edit and is retained as the fork's canary. `NetworkCompressionPinLiveTest` is **retained and extended**: add a `setCompressionMethod=false` case against `latest`. If it passes (likely, because `compress=false`), the Javadoc rationale (lines 20-25) must be rewritten, because D-17's ZSTD story is now only a second line of defence. If it fails, that is a rollout fact for the checklist. FORK-DELTA.md: the `network_compression_method` row (line 36) and the "eighth behaviour" paragraph (line 167) are replaced by a pointer to upstream's flag. The `at.yawk.lz4` capability resolution (FORK-DELTA.md:49) now duplicates upstream's identical block in `v1.7.0` `build.gradle.kts`; take upstream's, leaving five fork build additions, not six. The `0.9.6` row (line 51) shrinks to the Connect API pin.

### Null semantics: #820 vs WR-01 and WR-02 (Table stakes T4)

**What #820 changes** (`git diff v1.6.0 v1.7.0`):
- `StructToJsonMap` MAP case: a null optional Map becomes `new Data(schema, null)` instead of an NPE.
- Writer `case MAP`: `mapTmp == null` -> if `defaultsSupport`, write varint `0`; otherwise nothing.
- `doWriteCol`: the non-nullable null rule now treats `ARRAY || MAP` alike, writing a non-null marker (both branches).
- `cleanupExtraFields`: a null, non-nullable `MAP` column becomes `Collections.emptyMap()` on the JSON route.

Semantics: **a null value for a non-nullable top-level `Map` column is an empty map.** Lenient by design, same as `ARRAY`. Upstream tests `nullMapTypeTest`, `nullMapTypeJsonInsertTest`, `nullMapTypesTest` pin it.

**Verdict on overlap.**

| Item | Subsumed by #820? | Relationship |
|------|-------------------|--------------|
| WR-01 (null **entry** inside `Map(K, Nullable(V))`) | **No.** #820 handles a null whole map only | Code reading says the path is correct: the MAP loop skips the non-null marker when `mapValue == null` (Writer:629-631) and `doWritePrimitive` writes `0x01` for null (Writer:800-803), which is the Nullable null marker. It has simply never been run. Needs a live read-back test |
| WR-02 (null in a **non-nullable** element slot) | **No.** #820 is the lenient top-level case | WR-02 must carve out #820's top-level leniency, not contradict it |
| Fork `MAP`/`TUPLE` null guards | Collide textually with #820 | See finding 3 |
| P9-IN-01 | Adjacent hunk only | `cleanupExtraFields` body changes upstream, guard line changes in the fork; expect a clean merge, verify |

**"Fail loudly" consistent with `doWriteCol()`.** The existing contract at the top level is: null in a non-nullable column -> coerce for `ARRAY` (and now `MAP`) and `VARIANT` (discriminator 255), otherwise `RuntimeException("An attempt to write null into not nullable column '%s'")` (Writer:965-973, 980-988). WR-02 applies the same decision table to **element and value slots** of `ARRAY` and `MAP` (Writer:626-636, 648-657):

| Element/value slot type | Slot nullable | Null value | Behaviour |
|-------------------------|---------------|-----------|-----------|
| scalar (String, Int, Date, Decimal, FixedString, UUID, Enum) | yes | null | write Nullable null marker (today) |
| scalar | **no** | null | **throw**, same message shape, naming the column. Today `doWritePrimitive` writes `0x01`, desyncing the stream |
| `ARRAY`, `MAP` | no (cannot be Nullable) | null | coerce to empty container, **always** emitting the size varint regardless of `defaultsSupport` when nested (finding 8). At top level keep the current marker arithmetic |
| `VARIANT` | n/a | null | discriminator 255 (today) |
| `TUPLE` | no (cannot be Nullable) | null | **throw** (replaces the fork's `writeNull` guard) |

Recommended exception: `DataException` with `doWriteCol`'s wording. It is a `RuntimeException` subclass, so `assertThrows(RuntimeException.class, ...)` style assertions still hold, and `Utils.handleException` rethrows it as-is when tolerance is off (`Utils.java:106-108`) and it is DLQ-able when tolerance is on. Do not rewrite `doWriteCol`'s own `RuntimeException`; changing the top-level exception type is a behaviour change nobody asked for.

**Done looks like.**
- Byte-level Docker-free tests (a `ByteArrayOutputStream` through `doWriteColValue`) for: null element in `Array(String)` throws; null value in `Map(String,String)` throws; null element in `Array(Nullable(String))` writes `0x01`; null `Array(Map(...))` element writes exactly one `0x00` in both `defaultsSupport` modes; null `Tuple` element throws.
- Live read-back: `Map(String, Nullable(String))` with `{"a": null, "b": "x"}` returns `a -> NULL`, `b -> x` (closes WR-01 with typed read-back, matching Core Value rule "never `countRows`").
- Live: a top-level null optional `Map` still lands as an empty map on RowBinary, JSON (`bypassRowBinary=true`) and metadata-free routes, i.e. #820 is not weakened.
- A stream-desync regression guard: a record after the failing one is not corrupted (the throw happens before any partial write is flushed, or the batch is rejected whole).

### Remaining follow-ups: expected behaviour and testable acceptance (Table stakes T5-T7)

| Follow-up | Expected behaviour | Acceptance (testable) | Complexity |
|-----------|--------------------|-----------------------|------------|
| **P3-WR-01** `shouldSkipDescribeRow` at any depth (`ClickHouseHelperClient.java:461-476`) | Walk every dot-boundary ancestor of the row name, not only `lastIndexOf('.')`; skip when any ancestor present in `columnsSoFar` is `JSON` or `DYNAMIC`. Column names may contain dots, so test every prefix, not just the first segment. `Map`/`Tuple` ancestors alone still do not skip (Table's parent routing needs them) | Docker-free unit in `ClickHouseHelperClientForkUnitTest`: `j.a.b` with `{j: JSON}` -> skip; `j.a.b.c` -> skip; `t.x.y` with `{t: TUPLE}` -> no skip; `t.j.a.b` with `{t: TUPLE, t.j: JSON}` -> skip. Live: DESCRIBE a table with a typed JSON path if `latest` emits one | LOW |
| **P3-WR-03** fail-fast message (narrowed: `INT256`/`UINT256` only) | `validateDataSchema` throws the same `DataException` as `DYNAMIC`/`UNKNOWN` (Writer:331-336) for `INT256`/`UINT256`, via `buildRowBinaryUnsupportedTypeMessage` (Writer:543-548). **Do not** include `INT128`/`UINT128` | `RowBinaryUnsupportedTypeGateTest` gains `INT256` and `UINT256` cases asserting the message names the column, type and the two escape hatches; a negative case proves `INT128` is *not* rejected (existing coverage at `ClickHouseSinkTaskWithSchemaTest:1880`). Implementing 256-bit serialization is out of scope | LOW |
| **WR-03** `HeaderToValue` collision contract | Pin, do not change: schema path with `fields` naming a pre-existing non-STRING field throws `DataException` (wrapped "Failed to transform record", `HeaderToValue.java:94-97`) **only when the header is present**; header absent -> record passes through; pre-existing STRING field -> header value overwrites; schemaless path -> overwritten, no exception | Four tests in `HeaderToValueTest` mirroring `HeadersToValueTest` HDRS-05 (lines 377-415) | LOW |
| **P4-WR-01** `ClickHouseSinkTaskJsonInsertFormatTest` try/finally | Both tests (lines 78-130) stop the task in `finally`, as every sibling does | After a forced `put()` failure no MBean remains (the `@AfterEach` sweep logs nothing at WARN) | LOW |
| **P4-IN-01** `AsyncInsertLiveTest` Javadoc | State plainly that bare `async_insert=1` is **not** pinned (the class pins `async_insert=1` + `wait_for_async_insert=1` only) | Comment-only; reviewer read | LOW |
| **IN-01** `ClickHouseVersionDefaultUnitTest` Javadoc | Replace "CI pins `CLICKHOUSE_VERSION: latest`" (line 12) with the D-09 fact: CI sets no version env var, so the code default is the single source of truth | Comment-only; the assertion itself is untouched | LOW |
| **`Column.extractColumn` swap** (`Column.java:213`) | Pass `(…, false, hasDefaultValue, isSubColumn, arrayDepth + 1)` in the right order | `ColumnTest` Docker-free: `Array(String)` with `hasDefault=true,isSub=false` yields an array child with `hasDefault()==true`, `isSubColumn()==false`; and the inverse. Array children are never fed to `Table.addColumn` (`Table.java:85` reads root columns only), so no behaviour moves; run `TableTest` + the full `ColumnTest` to confirm | LOW |
| **`USERNAME` ConfigDef default** (`ClickHouseSinkConfig.java:433-435`) | `usernameDefault` (`"default"`), not `passwordDefault` | `ClickHouseSinkConfig.CONFIG.configKeys().get("username").defaultValue` equals `"default"` (Docker-free). Runtime unchanged: `username = props.getOrDefault(USERNAME, usernameDefault)` (`:218`) and tasks receive raw props, not parsed defaults | LOW |
| **P9-IN-01** guard order (`Writer:1404`) | `shouldSkipTableMetadata() \|\| csc.isBypassFieldCleanup()`; correct the `shouldSkipTableMetadata()` Javadoc so it no longer implies call sites are null-safe (finding 4) | Existing `MetadataFreeJsonInsertUnitTest` JSON-03 test (line 98) stays green; behaviour identical for non-null `csc` | LOW |
| **JsonColumnTest read-back** | The five enabled configs read back `json_data.nested_str`, `json_data.nested_int`, and `nested_bytes` per config (base64 string when stringified, otherwise the observed shape) with aggregate queries, e.g. `countIf(json_data.nested_str = concat('test', toString(id))) = RECORDS` | Replace the `assertEquals(RECORDS, countRows)` at line 82 with the aggregate assertions; the disabled `compareAllConfigurations` stays disabled | LOW-MEDIUM (bytes shape per config must be observed) |
| **`ClickHouseBase.tearDown` drop failure** (`ClickHouseBase.java:142-146`) | Retry the drop (small bounded retry), then **fail the class** with an attributable message ("database X not dropped; later classes in this fork lose isolation") instead of logging and carrying on | A Docker-free test over an extracted drop helper (or overridable hook) proving: success on retry passes, exhaustion throws with the database name. The non-shared path (`ch.stop()`) is untouched | LOW-MEDIUM |
| **CI concurrency** (`tests.yaml:11-13`) | Today the group is `github.sha`, so a tag at the branch SHA cancels the branch run. Restrict the trigger so a tag push starts no run (`on: push: branches: ['**']` or `tags-ignore`) **and** key the group on `github.ref`. Keying on `ref` alone would yield two completed runs per SHA, which is still not "exactly one" | Push branch and tag at one SHA; the Actions API shows exactly one run, none `cancelled` | LOW |
| **CI fork count** (`build.gradle.kts:192-194`: `availableProcessors()/2`) | Record the measured value, not the inferred one. Cheapest durable fix: write `nproc` and the resolved `maxParallelForks` to `$GITHUB_STEP_SUMMARY` so every run self-documents; otherwise record "unmeasured" in FORK-DELTA/verification. Public `ubuntu-latest` is believed to be 4 vCPU (so 2 forks), MEDIUM | Either a summary line in a green run, or an explicit "unmeasured" sentence with the inference stated | LOW |

### Upstream v1.7.0 features for fork users

| Upstream change | Fork users get it? | Table stakes to carry | Not adopted / no action |
|-----------------|--------------------|-----------------------|--------------------------|
| #841 `setCompressionMethod` (and `compress=false`) | Yes, default on | Carry; document flag, alias and the response-compression side effect in CHANGELOG and rollout checklist | Do not default it to `false` in the fork |
| #820 null optional Map -> empty map | Yes | Carry; extend with WR-01/02 on top | Do not make top-level null Map throw |
| `KeeperStateProvider` counts streamed rows (`response.stream().collect`, `v1.7.0` diff) | Yes, only affects `exactlyOnce=true` | Take byte-identical to `v1.7.0` (no fork edits exist in this file). It replaces `getSummary().getResultRows()` by materialising the stream, so memory cost is one state row. The embedded Keeper in `ClickHouseBase` (lines 117-121) lets upstream's exactly-once tests run on standalone, so the suite does exercise it; confirm those tests are in the executed set, since 11 `exactlyOnceStateMismatchTest` cases skip standalone | Do not touch. AGENTS.md rates this file consistency-critical |
| #801 buffered-sink duplicate | Yes, only if `bufferCount > 0` (the fork does not use buffering) | Carry silently | Buffering stays un-adopted (FORK-DELTA §5) |
| #800 Avro record unions as JSON | Fixtures and an integration test only (finding 1) | Nothing in `src/main` to carry | Do not adopt the Avro/Debezium paths |
| Client `0.9.5` -> `0.9.6`, `lz4-java 1.10.4`, `clickhouse-jdbc` dropped from catalog | Yes | The fork's `0.9.6` FORK OVERRIDE becomes upstream's value; `lz4-java` bump and capability resolution come with the merge | Client `0.10.0` stays out |
| Shaded `org.lz4` in the Confluent archive | Yes, packaging only | Verify the shadow jar still builds (`shadowJar`), since the fork pins Connect API 4.1.1 and keeps its own LZ4 handling | n/a |

### Rollout checklist contents (Table stakes T10)

The checklist is an operator-facing document, one section per change, each with "what changed", "how to check you are affected", "what to do". It must contain:

1. **JMX MBean name (D-07, repeats every release).** Names are `com.clickhouse:type=ClickHouseKafkaConnector,connector=<name>,name=SinkTask<id>,version=<VERSION>[,topic=<t>]`; `version=` is `Version.ARTIFACT_VERSION`, which becomes `v1.7.0_gg1` and **changes again on every `VERSION` bump** (MERGE-CHECKLIST Fact 11). Scrapers or dashboards keyed on `v1.6.0_gg1` or on the pre-fork `0.9.6` go blank. Fix: wildcard the `version` and `connector` attributes in the exporter pattern.
2. **Compression.** `setCompressionMethod` defaults on and forces `network_compression_method=lz4`; **server response compression is now off regardless**. Check: no server profile or constraint forbids changing `network_compression_method` (T-11-38; a constraint violation fails every request). Escape hatch: `setCompressionMethod=false`. Check `jdbcConnectionProperties` does not set `compress`/`network_compression_method` expecting different behaviour. Expect slightly larger responses for `DESCRIBE` and state reads.
3. **R-01 behaviour change.** Who is affected: connectors with `insertFormat=json` set explicitly **and** Struct values (Avro, Protobuf, or JSON converter with `schemas.enable=true`). Before: zero rows, no error. After: rows land, and failures are now server-side parse errors. Actions: (a) identify affected connectors by config; (b) compare `count()` with the committed Kafka offsets to measure the gap; (c) decide on an offset reset to backfill, remembering that replays duplicate under `exactlyOnce=false`; (d) review the JSON-route caveats (null fields omitted so non-NULL `DEFAULT`s apply; Timestamp logical types as epoch millis, so use `DateTime64(3)`; bytes as base64). Connectors that produce schemaless `Map` values are unaffected.
4. **Null `Map` semantics (#820).** A null optional `Map` now lands as `{}` instead of failing the task with an NPE. Affects downstream queries that distinguished "failed" from "empty".
5. **WR-02 strictness.** A null in a non-nullable `Array`/`Map` element now raises a `DataException` (DLQ-able under `errors.tolerance=all`) instead of silently corrupting the insert. Tell operators what the message looks like.
6. **Config UI default for `username`** now reads `default` (cosmetic).
7. **Rollback.** Name the rollback tag for this sync (pattern: `pre-v1.7.0-sync`) and the `git revert -m 1` route for the single `--no-ff` merge.
8. **Publication facts.** `custom_build` is protected, so the post-`c61d90ce` `.planning` history goes out by the route the branch rules allow; tag pushes must not start a second CI run (see T7).

### Differentiators (valuable, not required)

| Feature | Value | Complexity | Notes |
|---------|-------|------------|-------|
| One INFO log at start when metadata-free mode routes schema'd records to JSON | Makes the R-01 behaviour change visible in the operator's own logs on first run after upgrade | LOW | Mirror the existing start-up log at Writer:126 |
| `NetworkCompressionPinLiveTest` case for `setCompressionMethod=false` on `latest` | Turns "is D-17's rationale still true?" into a recorded fact; informs removal of the flag's relevance | LOW | See D-17 section |
| Upstream PRs for `USERNAME` default and `extractColumn` swap | Shrinks the fork delta back by two lines; next sync is cheaper | LOW | Both bugs exist in `v1.7.0` |
| A tiny `FORK-DELTA.md` consistency script (grep each "Where it lives" claim) | Seven behaviours vanished silently in the last sync; a mechanical check is cheaper than a human re-grep | MEDIUM | `MERGE-CHECKLIST` already has a self-testing invariant script; extend, do not invent a second tool |
| Summary-line fork count in CI (see CI follow-up) | Removes a recurring "unmeasured" caveat permanently | LOW | Preferred over a one-off log read |

### Anti-Features (explicitly do NOT build)

| Anti-feature | Why requested | Why problematic | Do instead |
|--------------|---------------|-----------------|-----------|
| Adopt client `0.10.0` | Upstream dependabot branch exists; ZSTD might decode | A client bump is a wire-behaviour change on a fork with no production soak; explicitly out of scope in PROJECT.md | Stay on `0.9.6`; `setCompressionMethod` covers ZSTD |
| Revisit the KEEP decision for the metadata-free JSON path | R-01 was KEEP's accepted cost; fixing it might tempt removal | Removal needs production grants or table pre-creation (Fact 14); the missing `DESCRIBE`/`SHOW` grant is unreachable by any upstream option | Route, as decided |
| Throw (fail-fast) for Struct + explicit JSON | The earlier suggested fix | Breaks loudly any pipeline that is running; user chose routing | Route Structs to the JSON writer |
| Convert `Timestamp`/`Date` logical types for the JSON route, or build a custom Struct-to-JSON serializer | The JSON route has date caveats | New capability on a rebase-sensitive file; widens the delta; the same caveat already exists for `bypassRowBinary=true` | Pin the observed behaviour, document `DateTime64(3)` |
| Implement 256-bit integer serialization | P3-WR-03 mentions the types | New feature, not a bug fix | Fail-fast message only |
| Make top-level null `Map` throw, or revert #820's leniency | "Fail loudly" read too broadly | Contradicts upstream's tests and #820's stated semantics | Throw only in non-nullable element slots |
| Rewrite `doWriteCol`'s `RuntimeException` to `DataException` | Consistency | Changes existing error classification for everyone | Use `DataException` only on the new paths |
| Refactor `ClickHouseWriter` null handling wholesale | WR-02 touches several branches | Writer is already the main conflict surface (±1,289 lines last sync) | Smallest hunks, one shared helper for the null decision table |
| Adopt `DebeziumRecordConvertor`, `auto.evolve`, buffering, `retryOnSocketException` | They arrive with the merge | Not fork features; unpinned; FORK-DELTA §5 | Leave un-adopted; keep the written note |
| Modify `KeeperStateProvider` or `Processing` | Exactly-once code arrived in the merge | Consistency-critical (AGENTS.md); fork has no edits there | Byte-identical to `v1.7.0` |
| Default `setCompressionMethod=false` in the fork | Avoid a behaviour change | Brings back the exact ZSTD risk on 26.9+ if anything else asks for compressed responses | Keep upstream's default |
| Bump any dependency beyond the approved list | `dependencyUpdates` plugin will list more | AGENTS.md: ask first | Approved: jackson 2.22.3, okhttp 5.5.0, org.json 20260814, mockito 5.24.0, lz4-java 1.10.4 |
| Force `async_insert` or add a client-side `jsonInsertColumns` filter | "Hardening" | Both are recorded KEEP/behaviour-preserving decisions (ASYNC-01..03, JSONP D-06) | Leave as pinned |
| Re-add a V1 or older-server test matrix | A new `ClickHouseHelperClient` V1 line (`COMPRESS`) tempts it | Out of scope per Constraints; V1 is only config-asserted | Docker-free assertions on V1 options |

## Feature Dependencies

```
T1 merge v1.7.0
    ├──required by──> T2 retire D-17 (same lines as #841)
    ├──required by──> T3 R-01 routing (needs #820 StructToJsonMap fix, finding 7)
    ├──required by──> T4 WR-01/WR-02 (same hunks as #820; resolve fork MAP/TUPLE guards)
    ├──required by──> P9-IN-01 (adjacent hunk in cleanupExtraFields)
    └──required by──> T8 dependency bumps (catalog + lz4 block merge)

T3 R-01 routing ──enhanced by──> T4 (null-Map case in R-01 matrix)
T3 ──requires──> FORK-DELTA/CHANGELOG/rollout (T9-T11) to record a deliberate behaviour change
T2 ──requires──> rollout item "compression" + live pin extension
T8 jackson bump ──touches──> JSON writer used by T3: re-run the R-01 matrix after the bump

P3-WR-01, P3-WR-03, extractColumn, USERNAME, WR-03, P4-WR-01, P4-IN-01, IN-01, JsonColumnTest,
tearDown, CI x2 ──independent of each other──> any order after T1 (no src/main overlap with v1.7.0)

T9 publish ──requires──> CI concurrency fix (else the tag push re-cancels the branch run)
T11 FORK-DELTA ──requires──> every code item landed (rows are grep-confirmed against the final tree)
```

### Dependency Notes

- **T1 before everything:** unchanged from the brief. The additional reason is the Writer hunk collision in `case MAP`.
- **Mechanical merge discipline:** the sync should repeat the v1.6.0 shape (scratch branch, rollback tag, one mechanical merge commit, fork behaviour re-applied by intent). Every fork behaviour that can vanish silently must have a canary before the merge: `NetworkCompressionPin*`, `ClickHouseVersionDefaultUnitTest`, `MetadataFreeJsonInsertUnitTest`, `MBeanNameUnitTest`, `RowBinaryUnsupportedTypeGateTest` already exist; confirm each fails when its guard is deleted.
- **Test-tree merge hazard:** upstream changed `ClickHouseTestHelpers` by one line (and `SchemaTestData`, `SchemalessTestData` by +40/+31). The fork carries UTF-8 and `CLICKHOUSE_VERSION_DEFAULT` edits in that file. The `latest` -> pinned reversion happened silently there last time; check `ClickHouseVersionDefaultUnitTest` first after the merge.
- **R-01 and WR-02 share `writeRecordsJson` only through #820**; they do not need to be in the same phase, but R-01's null-Map case must wait for T1.

## MVP Definition

### Launch With (everything below is `v1.7.0_gg1`)

- [ ] T1 mechanical merge, fork behaviour re-applied by intent, `FORK-DELTA` rows re-grepped
- [ ] T2 D-17 retired onto upstream's flag, pins retained
- [ ] T3 R-01 routed, pin flipped to typed read-back, matrix and `bypassRowBinary=true` combination pinned
- [ ] T4 WR-01 live read-back and WR-02 loud failure, reconciled with #820
- [ ] T5 and T6 the nine small fixes and test-debt items
- [ ] T7 CI concurrency and fork-count outcome
- [ ] T8 approved dependency bumps
- [ ] T9-T11 publication, CHANGELOG, rollout checklist, FORK-DELTA

### Suggested phase grouping

1. **Sync** (T1, T8 catalog parts): mechanical merge, suite as acceptance gate.
2. **Re-apply and retire** (T2, finding 3's MAP-guard resolution, P9-IN-01): small, `ClickHouseHelperClient` and Writer guards.
3. **Writer correctness** (T3, T4): shares Writer and the live matrix; do R-01 first (no dependency on WR-02), then WR-01/02.
4. **Metadata/config and test debt** (T5, T6): parallel-safe, mostly Docker-free.
5. **CI, docs, publication** (T7, T9-T11).

### Future Consideration (defer)

- Upstream PRs for the two upstream bugs (nice, not blocking).
- Removing the `setCompressionMethod` dependency entirely once a client that decodes ZSTD is adopted (needs its own milestone; `0.10.0` is currently out of scope).
- A fork-owned `Struct`-to-JSON date/bytes converter (anti-feature now; revisit only if an operator needs `DateTime` columns on the metadata-free route).

## Feature Prioritization Matrix

| Feature | User value | Cost | Priority |
|---------|-----------|------|----------|
| T1 merge | HIGH | MEDIUM | P1 |
| T3 R-01 routing | HIGH (silent data loss) | MEDIUM | P1 |
| T2 D-17 retirement | MEDIUM (smaller delta) | MEDIUM | P1 (same lines as T1) |
| T4 WR-02 loud failure | HIGH (silent stream corruption) | MEDIUM-HIGH | P1 |
| T4 WR-01 live test | MEDIUM | LOW | P1 |
| T10 rollout checklist | HIGH | LOW | P1 |
| T11 FORK-DELTA | HIGH (next sync) | MEDIUM | P1 |
| T7 CI concurrency | MEDIUM | LOW | P1 |
| P3-WR-01, P3-WR-03, `extractColumn`, `USERNAME`, P9-IN-01 | LOW-MEDIUM | LOW | P2 |
| WR-03, `JsonColumnTest`, P4-WR-01, `tearDown` | MEDIUM (test trust) | LOW-MEDIUM | P2 |
| IN-01, P4-IN-01 | LOW | LOW | P3 |
| CI fork count | LOW | LOW | P3 |
| Differentiators above | LOW-MEDIUM | LOW-MEDIUM | P3 |

**Priority key:** P1 must ship for publication; P2 should ship, all 17 are in scope; P3 comment-level or optional.

## Code-backed risk notes for the roadmap

- **Exactly-once is not touched** by any item, but `KeeperStateProvider` changes in the merge; keep it byte-identical to `v1.7.0` and verify by `git diff v1.7.0 -- src/main/.../KeeperStateProvider.java` being empty.
- **Batch-composition rule (AGENTS.md):** none of the routing or null changes alter a batch after `BEFORE` is set; they decide serialisation of a given batch deterministically from its records, so retries still rebuild identical blocks. Worth a one-line assertion in the plan.
- **R-01 hot path:** the new condition runs once per batch (`doInsertBatch`), not per record; no allocation concern.
- **`doInsertBatch` inspects only the first record's `SchemaType`** (Writer:234). Mixed batches already route by the first record; unchanged and not in scope.

## Sources

- `.planning/PROJECT.md` (Current Milestone, Requirements -> Active, Key Decisions); `docs/FORK-DELTA.md`; `.planning/MERGE-CHECKLIST.md` Facts 9, 11, 14 (HIGH, project records).
- Source reads at `HEAD`: `ClickHouseWriter.java` (lines cited inline), `ClickHouseSinkConfig.java`, `ClickHouseHelperClient.java`, `Column.java`, `StructToJsonMap.java`, `SchemaRecordConvertor.java`, `DataJson.java`, `HeaderToValue.java`, `ClickHouseBase.java`, `.github/workflows/tests.yaml`, `build.gradle.kts` (HIGH).
- Upstream: `git diff v1.6.0 v1.7.0` over `src/main`, `src/test`, `src/testFixtures`, `CHANGELOG.md`, `build.gradle.kts`, `gradle/libs.versions.toml`; `git log --stat v1.6.0..v1.7.0` (HIGH).
- Tests read: `ExplicitJsonInsertPathLiveTest`, `MetadataFreeJsonInsertUnitTest`, `JsonDynamicRoundTripTest`, `NetworkCompressionPinUnitTest`, `NetworkCompressionPinLiveTest`, `JsonColumnTest`, `ClickHouseSinkTaskJsonInsertFormatTest`, `AsyncInsertLiveTest`, `ClickHouseVersionDefaultUnitTest`.
- No external web research was needed; every claim is about this repository or its upstream tag. Jackson date serialisation, `NON_NULL` effects on server defaults, `Nullable(Map|Tuple)` being illegal, and the 4 vCPU public-runner size are domain knowledge (MEDIUM until confirmed by the live tests named above).

---
*Feature research for: `v1.7.0_gg1-sync` (ClickHouse Kafka Connect fork)*
*Researched: 2026-10-07*
