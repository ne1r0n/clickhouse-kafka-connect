# Architecture Research: v1.7.0_gg1-sync Integration

**Domain:** Brownfield fork sync (upstream ClickHouse Kafka Connect sink `v1.6.0` -> `v1.7.0` into the `custom_build` fork) plus follow-up hardening
**Researched:** 2026-10-07
**Confidence:** HIGH for the collision map and merge mechanics (measured by a real dry-run merge and a byte-level probe); MEDIUM for R-01 type-fidelity caveats (read from code, not run: no Docker on this machine); MEDIUM for "Nullable(Map/Tuple/Variant) is illegal in ClickHouse" (from memory of ClickHouse rules, not re-verified)

## Headline findings

1. **The merge is much smaller than the last one: 5 textual conflicts, in 5 files** (`VERSION`, `gradle/libs.versions.toml`, `ClickHouseSinkConfig`, `ClickHouseWriter`, `ClickHouseHelperClient`). 17 of 22 upstream paths merge clean, and 14 of those are byte-identical to upstream afterwards because the fork never touched them. Merge base is `v1.6.0` itself (`789a57c`).
2. **One silent semantic conflict that textual merge does not flag, and no existing gate catches until a live run.** The fork's early `value.getObject() == null -> writeNull` branch in `ClickHouseWriter`'s `case MAP:` auto-merges directly above upstream's #820 `mapTmp == null` branch and shadows it. Probe on the resolved tree: a null optional Map into a non-nullable `Map(String,String)` column writes `00 01` (a stray byte, stream desync) instead of `00` (RowBinary) / `00 00` (RowBinaryWithDefaults). Upstream's own new test `nullMapTypeTest` would fail live. Fix is one scoped condition, `value.getObject() == null && col.isNullable()`, verified in the worktree: bytes become `00` / `00 00`, and the fork's CR-02 pin `NestedNullableCompositeWriteTest` stays green (it uses `isNullable(true)`).
3. **The D-17 retirement is not a plain "take upstream".** Upstream's `setCompressionMethod` does three things, not one: gates the `lz4` setting behind a config flag (default `true`), **also turns response compression off** (`COMPRESS=false` for V1 options and node, `compressServerResponse(false)` for V2), and threads the flag through every `ClickHouseClientBuilder` call site (3 in `src/main`, 1 in `testFixtures`). Upstream's new test `testSetCompressionMethod_Disabled` asserts *no* `network_compression_method` key on the V2 client when the flag is off, so it cannot pass with the fork's unconditional `.serverSetting(...)`. Resolve to upstream, delete the fork's now-unused constants, rewrite the FORK-DELTA row.
4. **R-01's fix is a one-line routing change** at `ClickHouseWriter.doInsertBatch`: `if (csc.isBypassRowBinary() || shouldSkipTableMetadata())`. Everything downstream (`doInsertJson*`, `writeRecordsJson`, `jsonInsertColumns`, `cleanupExtraFields` bypass) already exists. The risk is not the routing, it is JSON-path type fidelity for non-primitive Struct fields (see section c).
5. **The previous sync's invariant script cannot be reused as-is, and its "take upstream wholesale for conflicted paths" shape should not be reused for the three Java files.** Doing so would delete ~277 lines of fork logic (`ClickHouseWriter` +114, `ClickHouseHelperClient` +121, `ClickHouseSinkConfig` +42) and require re-adding them, which is exactly the "silently vanishing behaviour" failure mode of the last sync. Use a hunk-level resolution with a line-provenance invariant instead (section b).

## (a) Per-file collision map

Evidence: `git diff --name-only v1.6.0 v1.7.0` (22 paths), `git diff --shortstat v1.6.0 HEAD -- <path>` (HEAD `94d98615`, whose `src/` tree is byte-identical to `custom_build` `c61d90ce`), `git merge-tree --write-tree --name-only HEAD v1.7.0`, and a real `git merge --no-commit v1.7.0` in a throwaway worktree (removed; `git worktree list` shows only the main tree).

"Fork delta" = `git diff --shortstat v1.6.0 HEAD -- <path>`. "Result" is the observed dry-run outcome, not a prediction.

### Production and build

| Path | Upstream v1.7.0 delta | Fork delta vs v1.6.0 | Result | Notes |
|---|---|---|---|---|
| `VERSION` | +1/-1 | +1/-1 | **CONFLICT, trivial** | `v1.6.0_gg1` vs `v1.7.0`. Take upstream during the merge, set `v1.7.0_gg1` in the publication phase (it feeds JMX `version=` via `Version.ARTIFACT_VERSION`) |
| `gradle/libs.versions.toml` | +3/-2 | +2/-2 | **CONFLICT, trivial** (2 lines) | `clickHouseDriver` becomes `"0.9.6"` on both sides (the FORK OVERRIDE comment is now false: it is upstream's own value, drop the comment); `kafka = "4.1.1"` stays a FORK OVERRIDE (upstream `2.7.0`). Auto-merged: upstream adds `lz4Java = "1.10.4"`, `lz4-java` library alias, removes `clickhouse-jdbc` alias (fork never used it; checked, no remaining reference in `build.gradle.kts`) |
| `build.gradle.kts` | +19/-1 | +167/-9 | **Clean auto-merge, SILENTLY REDUNDANT** | Upstream adds a top-level `configurations.all { capabilitiesResolution.withCapability("org.lz4:lz4-java") { select at.yawk candidate } }` block, `implementation(libs.lz4.java)` and `clickhouseDependencies(libs.lz4.java)`, drops the integrationTest `clickhouse-jdbc`. The fork's own block (`select("at.yawk.lz4:lz4-java:1.10.2")`, "Temporary fix", BLD-06) survives beside it, so there are now **two** rules for the same capability and a hard-coded `1.10.2` that disagrees with the catalog's `1.10.4`. Remove the fork block; then prove the resolved version with `./gradlew dependencyInsight --dependency lz4-java` (I did not run this; compile succeeded offline, so at least one version was in the Gradle cache). Upstream's CHANGELOG also says the Confluent archive now shades `org.lz4` |
| `src/main/.../sink/ClickHouseSinkConfig.java` | +17 | +42/-1 | **CONFLICT x4, trivial (pure union)** | See hunk analysis below. Both sides append at the same anchor points (constant, field, constructor assignment, `configDef.define`) |
| `src/main/.../sink/db/ClickHouseWriter.java` | +17/-4 | +114/-11 | **CONFLICT x1 trivial + 1 SILENT SEMANTIC + 1 adjacent clean** | See hunk analysis below |
| `src/main/.../sink/db/helper/ClickHouseHelperClient.java` | +24 | +121/-7 | **CONFLICT x6 (4 trivial union, 2 resolve-to-upstream) + 2 leftovers** | See hunk analysis below |
| `src/main/.../sink/data/StructToJsonMap.java` | +6/-1 | +5/-6 (import reorder only, per MERGE-CHECKLIST) | Clean | #820: null optional Map becomes `new Data(schema, null)`. The fork has no logic here. Upstream's new `StructToJsonMapTest` is Docker-free and passes on the resolved tree |
| `src/main/.../sink/state/provider/KeeperStateProvider.java` | +7/-3 | identical to v1.6.0 | Clean, byte-identical to upstream after merge | Adds `.setCompressionMethod(csc.isSetCompressionMethod())` to its builder (so the fork's old unconditional lz4 on this client is replaced by the flag, default true) and changes the state read to count `response.stream()` rows instead of `getSummary().getResultRows()`. **Exactly-once high-risk file with no fork edits and no Docker-free test**: `exactlyOnceStateMismatchTest` x11 is skipped in standalone mode (named skip inventory), so nothing in the standard suite exercises the new read. Needs a deliberate verification step (see phase 12) |
| `src/main/.../sink/AtLeastOnceBufferStrategy.java` | +9/-1 | identical | Clean | #801. Fork does not use buffering. Take, do not adopt (`bufferCount` stays 0). Upstream's `ClickHouseSinkTaskBufferTest` (+36) still runs in the suite |
| `CHANGELOG.md` | +37 | identical to v1.6.0 | Clean | The fork has never added its own entries; publication phase must add a fork entry (project rule: bump `VERSION`, update `CHANGELOG.md`) |

### Tests, fixtures, resources (all fork-identical to v1.6.0 except one, so all clean)

| Path | Upstream delta | Fork delta | Result | What it exercises in fork code |
|---|---|---|---|---|
| `src/test/.../db/helper/ClickHouseHelperClientTest.java` | +64 | identical | Clean | `testSetCompressionMethod_DefaultIsTrue/_Disabled`, `testSinkConfig_SetCompressionMethod`. **These pin the D-17 resolution direction**: they require the V1 options, V1 node options and V2 configuration to carry no `network_compression_method` when the flag is off. Needs Docker (extends `ClickHouseBase`) |
| `src/test/.../ClickHouseSinkTaskWithSchemaTest.java` | +68 | identical | Clean | `nullMapTypeTest`, `nullMapTypeJsonInsertTest` (live). The first is what the silent MAP conflict breaks |
| `src/test/.../ClickHouseSinkTaskSchemalessTest.java` | +29 | identical | Clean | `nullMapTypesTest` (live, schemaless, JSON route, unaffected by the MAP branch) |
| `src/test/.../ClickHouseSinkTaskBufferTest.java` | +36 | identical | Clean | #801 buffered retry. Unrelated to fork code |
| `src/test/.../data/StructToJsonMapTest.java` | +52 (new) | n/a | Clean | Docker-free; passes in `testUnit` on the resolved tree |
| `src/testFixtures/.../helper/ClickHouseTestHelpers.java` | +1 | +24/-9 | Clean auto-merge | Upstream adds `.setCompressionMethod(csc.isSetCompressionMethod())` to `createClient`; the fork's `CLICKHOUSE_VERSION_DEFAULT = latest`, UTF-8 and always-pull edits are elsewhere in the file. **Re-check Recorded Fact 15 anyway** (`ClickHouseVersionDefaultUnitTest` is the canary, it ran green in `testUnit`) |
| `src/testFixtures/.../helper/SchemaTestData.java` | +40 | identical | Clean | `createNullableMapType` |
| `src/testFixtures/.../helper/SchemalessTestData.java` | +31 | identical | Clean | `createNullableMapType` |
| `src/testFixtures/avro/schemas/compatible/union_two_records.json` (A) and `.../incompatible/union_two_records.json` (D) | +145 / -39 | identical | Clean | #800 Avro record-union fixture moved. Take with the merge, do not adopt the Avro-union feature |
| `src/integrationTest/.../ClickHouseSinkConnectorIntegrationTest.java` | +94/-3 | identical | Clean | Compile-only gate (HARN-06). Uses `org.json.JSONArray` and `ClickHouseTestHelpers.getAllRowsAsJson[Cloud]`. `compileIntegrationTestJava` succeeded on the resolved tree |
| `src/integrationTest/resources/clickhouse_sink_avro_json.json` | +20 (new) | identical | Clean | Config fixture only |

**Dry-run gate results on the resolved tree (throwaway worktree, Gradle `--offline`):** `compileJava`, `compileTestJava`, `compileTestFixturesJava`, `compileIntegrationTestJava` all succeed. `testUnit` (Docker-free): 333 tests with my temporary probe class included; with the fork's early MAP branch removed exactly one fails (`NestedNullableCompositeWriteTest` CR-02, which pins that branch); with the scoped guard `value.getObject() == null && col.isNullable()` all pass. Live (Docker) tests were **not** run here; that is the first thing the sync phase must do.

### Hunk analysis: the three heavily forked files

#### `ClickHouseHelperClient` (6 conflict hunks, plus 2 silent leftovers)

| Hunk | Fork side | Upstream side | Resolution |
|---|---|---|---|
| Fields | `targetTableFilter` | `@Getter setCompressionMethod` | Union |
| Constructor assignment | `this.targetTableFilter = ...` | `this.setCompressionMethod = ...` | Union |
| `getDefaultClientOptions()` (V1) | unconditional `CUSTOM_SETTINGS = network_compression_method=lz4` | `if (setCompressionMethod) CUSTOM_SETTINGS = ...` plus `COMPRESS=false` | **Take upstream** (D-17 retired) |
| `createClientV2()` builder chain | `.setDefaultDatabase(db).serverSetting(NCM, lz4)` with comment "applied before jdbcConnectionProperties so operator wins" | `.compressServerResponse(false).setDefaultDatabase(db);` and, auto-merged *below* the conflict, `if (setCompressionMethod) clientBuilder.serverSetting(NCM, LZ4);` | **Take upstream.** Do not "keep both": the fork's `.serverSetting` would make the upstream `if` meaningless. Operator-wins ordering is preserved because upstream's `if` also sits before the `jdbcConnectionProperties` loop, so `NetworkCompressionPinUnitTest.operatorValueWinsOnV2` and `unrelatedOperatorOptionsKeepLz4` stay valid unchanged |
| Builder field | `targetTableFilter = ""` | `setCompressionMethod = ClickHouseSinkConfig.setCompressionMethodDefault` | Union |
| Builder setters | `setTargetTableFilter(...) { this.targetTableFilter = ...; return this; }` | `setCompressionMethod(...) { this.setCompressionMethod = ...; return this; }` | **Textual trap:** the two methods share the trailing `return this; }` lines, so git's conflict hunk interleaves the bodies. A naive "keep both sides" produces a method with no `return`. Write both methods out in full |
| Silent leftover 1 | `NETWORK_COMPRESSION_METHOD_SETTING` / `_LZ4` constants + "Remove once the pinned client decodes ZSTD" comment survive the merge outside any conflict | Upstream's public `NETWORK_COMPRESSION_METHOD` / `LZ4` constants are auto-merged next to the field block | Delete the fork's two constants. `grep` shows they are referenced nowhere outside this file, including tests (`NetworkCompressionPin*` use string literals) |
| Silent leftover 2 | The V1 node-options block (`createClientV1`) gets upstream's `options.put(NCM, LZ4)` + `COMPRESS=false` | — | Clean, correct. Pinned by upstream's `testSetCompressionMethod_*` |

Behaviour change to record, not just a refactor: with the flag at its default, **server response compression is now off for both clients** (V1 `COMPRESS=false`, V2 `compressServerResponse(false)`). That is upstream's actual fix for the ZSTD break; the `lz4` setting is belt and braces. `NetworkCompressionPinLiveTest`'s premise ("ClickHouse 26.9 answers `compress=1` with ZSTD") no longer reproduces the original failure on the default config, but its `getSetting('network_compression_method') = lz4` assertions still hold. With `setCompressionMethod=false` an operator removes both mitigations; say so in the rollout checklist.

#### `ClickHouseSinkConfig` (4 conflict hunks, all pure union)

| Hunk | Fork | Upstream |
|---|---|---|
| Key constants | `TARGET_TABLE_FILTER` | `SET_COMPRESSION_METHOD`, `NETWORK_COMPRESSION_METHOD`, `LZ4` (aliases to `ClickHouseHelperClient` constants, which creates a `ClickHouseSinkConfig` <-> `ClickHouseHelperClient` import cycle that already compiles, since the helper's builder already referenced `ClickHouseSinkConfig.setCompressionMethodDefault`) |
| Field declarations | `targetTableFilter`, `jsonInsertColumns`, `jsonInsertFormatExplicit` | `setCompressionMethod` |
| Constructor | `targetTableFilter = props.getOrDefault(...).trim()` | `setCompressionMethod = parseBoolean(props.getOrDefault(SET_COMPRESSION_METHOD, props.getOrDefault("set_compression_method", ...)))` |
| `configDef.define` | `TARGET_TABLE_FILTER` | `SET_COMPRESSION_METHOD` |

Outside the conflict, the fork's other edits (`async_insert` stays commented out, `jsonInsertColumns` parsing, `jsonInsertFormatExplicit`) and the `USERNAME` ConfigDef default bug are untouched by upstream. `AsyncInsertSettingsUnitTest` and the thirteen-option defaults pin still apply; upstream adds a fourteenth option, so the Docker-free "all new upstream options pinned at defaults" test (CFG-03) should gain `setCompressionMethod=true`.

#### `ClickHouseWriter` (1 textual conflict, 1 silent semantic, 2 clean adjacent edits)

| Site | What happens | Verdict |
|---|---|---|
| `start()` builder chain | Fork `.setTargetTableFilter(...)` vs upstream `.setCompressionMethod(...)`, adjacent lines | Trivial union |
| `getServer()`-style helper builder (`chcTmp`, ~line 1590) | Upstream adds `.setCompressionMethod(...)`; fork has no edit there (fork never passed the filter to this temporary client) | Clean |
| `case MAP:` in `doWriteColValue` | Fork inserted `if (value.getObject() == null) { writeNull; break; }` **before** `Map<?,?> mapTmp = ...`; upstream inserted `if (mapTmp == null) { if (defaultsSupport) writeVarInt(0); break; }` **after** it. Non-overlapping lines, so git merges both, and the fork's branch runs first and makes upstream's dead | **SILENT SEMANTIC CONFLICT** (probe above) |
| `doWriteCol` nullable-marker hunks (`colType == ARRAY || colType == MAP`) | Upstream only; fork has no edit there | Clean, correct: the preamble writes the "no default"/zero byte which doubles as the empty-map size when the MAP branch writes nothing (non-defaults) or `varint 0` (defaults) |
| `cleanupExtraFields` | Fork's guard line `csc.isBypassFieldCleanup() \|\| shouldSkipTableMetadata()` sits two lines above upstream's new "null non-nullable Map -> `emptyMap()`" loop body | Clean merge, correct. Note P9-IN-01 (operand order) is on the line immediately above upstream's hunk, so the fix must be made on the merged text |

**Why the MAP conflict matters beyond upstream's test.** In the probe, a null optional Map into a *non-nullable* `Map` column is the exact #820 scenario. The fork's CR-02 branch was written for the case "a `Nullable` child reached with a genuine null". A scoped guard `value.getObject() == null && col.isNullable()` keeps CR-02's pin and restores #820. The TUPLE and VARIANT early-null branches have no upstream counterpart and need no change for the merge. They do share WR-02's problem (see section d).

## (b) Reusing the previous sync's tooling

| Asset | Reusable? | What to change |
|---|---|---|
| `.planning/scripts/verify-merge-invariants.sh` | **Partly.** Invariant 2 (no conflict-start marker in any commit of `pre-tag..range-end` over `src build.gradle.kts settings.gradle.kts gradle .github VERSION .gitignore`) is generic | Constants `PRE_TAG="pre-v1.6.0-sync"` and `UPSTREAM_REF="v1.6.0"` are hard-coded; make them arguments or env vars (`pre-v1.7.0-sync` / `v1.7.0`). The marker-assembled-at-runtime trick should be kept |
| Invariant 1 ("every conflicted path is byte-identical to upstream at the merge commit") | **No for this merge.** It encodes the "mechanical commit takes upstream wholesale" policy, which only made sense when 40 paths conflicted and most were fully rewritten. Here it would delete the fork's logic from 3 Java files and force a large re-apply | Replace with **Invariant 1b (line provenance)** per conflicted path: every line the merge commit adds relative to `HEAD`(ours) must appear as an added line in `git diff v1.6.0 v1.7.0 -- <path>`, and every line it removes relative to ours must be in a short per-path allow-list (the D-17 lines: the fork's two constants, the `CUSTOM_SETTINGS` unconditional put, the `.serverSetting(...)` chain tail, and the shadowing MAP branch if it is fixed in the same commit). Scriptable with `git diff HEAD <merge> -- path` plus `grep -Fxf`. Keep the old byte-identical check for `VERSION` and `gradle/libs.versions.toml` only if those are resolved by taking upstream; `libs.versions.toml` is not (it keeps `kafka = 4.1.1`), so give it an expected-content check instead |
| `.planning/scripts/conflicted-paths.txt` | Regenerate | `git merge-tree --write-tree --name-only -z HEAD v1.7.0` yields 5 paths (the list in the first table). Do not hand-transcribe |
| New Invariant 3 (add) | Needed | The last sync's lesson was that a green build does not prove fork behaviour survived (seven silent vanishings). Add a script step that greps each row of FORK-DELTA's "What this fork still patches" for its anchor construct (`shouldSkipTableMetadata`, `jsonInsertFormatExplicit`, `Type.DYNAMIC`, `buildRowBinaryUnsupportedTypeMessage`, `shouldSkipDescribeRow`, `buildTableFilterClause`, `connector=` segment, `CLICKHOUSE_VERSION_LATEST`, ...) and fails if one is missing. Cheap, and it is exactly what caught nothing the last time because it did not exist |
| `.planning/MERGE-CHECKLIST.md` | **Shape reusable, content not.** Section 1 (per conflicted path: conflict type, resolution shape, owning phase, fork behaviour at risk, adjudicating test, status), Section 2 (merged-clean-but-read-anyway), numbered Recorded Facts | Section 1 shrinks to 5 rows. **Section 2 is where the value is this time**: `build.gradle.kts` (duplicate lz4 rule), `KeeperStateProvider` (exactly-once read change), the `ClickHouseWriter` MAP and `cleanupExtraFields` auto-merged hunks, `ClickHouseTestHelpers` (version-default canary), `StructToJsonMap`, `AtLeastOnceBufferStrategy`. Recorded Facts to add: resolved `lz4-java` version, the response-compression-off behaviour change, the MAP-shadow finding and its probe bytes, `kafka`/`clickHouseDriver` catalog state. Name it `MERGE-CHECKLIST-v1.7.0.md` (or version the existing one) so the v1.6.0 record stays intact |
| Process shape | Reuse | Scratch branch `sync/v1.7.0`, rollback tag `pre-v1.7.0-sync` -> `c61d90ce` (both `custom_build` and the `src/` of the current HEAD), one merge commit first (conflict resolution only, no behaviour edits except what is needed to compile and keep the tree internally consistent), re-apply by intent in isolated commits, non-force `--no-ff` merge into protected `custom_build` after human approval. Use `v1.7.0` or `refs/remotes/upstream/main`, never bare `upstream/main` (ambiguous with stale `refs/upstream/main`) |

Branch base note: `sync/v1.6.0` HEAD differs from `custom_build` only by `.planning/` commits (`git diff --stat c61d90ce HEAD -- . ':!.planning'` is empty), so scratch branch `sync/v1.7.0` can be cut from the current HEAD, and the final `--no-ff` merge into `custom_build` publishes the planning history in the same step (a stated milestone goal).

## (c) R-01 integration point in `ClickHouseWriter`

**Dispatch site:** `doInsertBatch(records, table, queryId)`, switching on `first.getSchemaType()` (about line 241 of the merged file):

```java
case SCHEMA:
case DEBEZIUM_CDC:
    if (csc.isBypassRowBinary()) {
        doInsertJson(records, table, queryId);
    } else {
        doInsertRawBinary(records, table, queryId, table.hasDefaults(), true);
    }
    break;
case SCHEMA_LESS:
    doInsertJson(records, table, queryId);
    break;
```

**How `shouldSkipTableMetadata()` gates things today:** a private `csc != null && csc.isJsonInsertFormatExplicit()` predicate read at five sites: `start()`, `updateMapping()`, the `auto.evolve` branch of `doInsert`, `cleanupExtraFields`, `getTable()`. `getTable()` returns a synthetic `new Table(database, mappedTopicName, false, List.of(), 0)` with zero columns. `SCHEMA_LESS` already goes to JSON, which is why the schemaless route works. A `SCHEMA` record reaches `doInsertRawBinary*`, whose per-column loop iterates `table.getRootColumnsList()` (empty), so each record serialises to zero bytes and `validateDataSchema`'s loop is vacuous. That is R-01.

**Minimal change:** make the predicate the sixth read site, at the dispatch:

```java
if (csc.isBypassRowBinary() || shouldSkipTableMetadata()) {   // no column metadata => RowBinary cannot work
    doInsertJson(records, table, queryId);
}
```

Why this is enough and nothing else needs to move:
- `doInsertJsonV1/V2` -> `writeRecordsJson` handles `SCHEMA` by copying top-level fields via `struct.get(field)` and serialising with `OBJECT_MAPPER`, which has a registered `Struct` serializer, so nested Structs work. It handles `DEBEZIUM_CDC` through `record.getJsonMap()`.
- `cleanupExtraFields` already returns the map untouched in metadata-free mode, so the synthetic empty `Table` is never consulted for columns.
- `jsonInsertColumns` is applied in `doInsertJsonV2` via the 5-argument `client.insert` overload, so the fork's column list still works for Struct records after the routing.
- The `doInsertRawBinary` retry-with-refreshed-mapping path (which re-fetches the table on error) becomes unreachable in metadata-free mode, which is correct since that mode must never DESCRIBE.
- No Kafka Connect API, no `Processing`/offset logic, no config key changes. `bypassRowBinary=true` + explicit JSON keeps working identically.

**Side effects the plan must cover (not in the one-liner):**
1. **Type fidelity of the JSON route for Struct fields.** `writeRecordsJson` does `struct.get(field)` and hands raw Java objects to Jackson. `DataJson`'s mapper has no date, decimal or bytes handling, so `Timestamp`/`Date` logical types (`java.util.Date`) serialise with Jackson's default epoch-millisecond number, `BigDecimal` as a number, `byte[]` as base64. A ClickHouse `DateTime` parsed from a JSONEachRow number reads it as *seconds*. I did not run this (no Docker), so it is MEDIUM confidence, but it is a real silent-wrongness risk, and it is the same behaviour upstream's own `bypassRowBinary=true` has. The R-01 plan should pin which Struct field types are guaranteed (the pin test's `Int32`/`String`) and state the limits in FORK-DELTA, rather than claim parity with RowBinary.
2. **Test flip.** `ExplicitJsonInsertPathLiveTest.explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch` (Javadoc headed "R-01 PIN - NOT AN ENDORSEMENT") must become a typed read-back test (rename; keep the origin note). Add a Docker-free routing unit beside `MetadataFreeJsonInsertUnitTest` (subclass the writer and override the two `protected` methods `doInsertJson` / `doInsertRawBinary`), plus a live test for `bypassRowBinary=true` + metadata-free, which FORK-DELTA says is not pinned today. `Processing`/exactly-once behaviour is unchanged since the write still produces one insert per batch with the same deduplication token.
3. **Docs.** FORK-DELTA: the metadata-free row goes from "five guard sites" to six, R-01 leaves "Known costs" and enters a changed-behaviour note; CHANGELOG entry; operator rollout item (previously silent-loss pipelines will start writing rows, including any backlog replayed from committed-offset resets).

## (d) Which follow-ups collide with the merge (must come after it)

| Follow-up | File / method | Touches a merge-changed region? | Ordering |
|---|---|---|---|
| D-17 retire (`setCompressionMethod`) | `ClickHouseHelperClient`, `ClickHouseSinkConfig`, `ClickHouseWriter`, `KeeperStateProvider`, `ClickHouseTestHelpers` | It *is* the merge region | In the sync phase (merge commit + a short reconcile commit) |
| WR-01 (null inside `Map(K, Nullable(V))`), WR-02 (loud failure for null in a non-nullable MAP/ARRAY slot) | `ClickHouseWriter.doWriteColValue` MAP/ARRAY cases | **Yes, same lines** as #820's MAP branch | After the merge. Finding: in the probe the WR-01 byte path is *already correct* (`Map(String, Nullable(String))` with `{k: null}` -> `01 01 6b 01`, right marker), so WR-01 is likely a coverage-only item. WR-02 is real: `Array(Map(String,String))` with a null element writes a short stream (`01` after the scoped fix, `01 01` before) with no exception. WR-02 needs a way to tell a top-level call from a nested one (e.g. `col.getParent()` or an extra parameter), and the CR-02 branches for TUPLE and VARIANT have the same shape; decide their fate in the same plan. Mind the "never weaken existing tests" rule: if CR-02 pins are re-scoped, say so explicitly |
| P9-IN-01 (`cleanupExtraFields` operand order) | `ClickHouseWriter.cleanupExtraFields` | Line directly above upstream's #820 hunk | After the merge (trivial) |
| P3-WR-03 (`INT128`/`INT256`/`UINT128`/`UINT256` fail-fast message) | `ClickHouseWriter.validateDataSchema` | Same file; no upstream hunk, but sits with the DYNAMIC/UNKNOWN gate | After the merge, same plan as the writer hardening |
| R-01 routing | `ClickHouseWriter.doInsertBatch` | Same file, no upstream hunk | After the merge (and ideally before WR-02 so the JSON path is settled first) |
| P3-WR-01 (`shouldSkipDescribeRow` at any depth) | `ClickHouseHelperClient` | Same file as the 6 conflicts | After the merge |
| `USERNAME` ConfigDef default (`passwordDefault` -> `usernameDefault`) | `ClickHouseSinkConfig` | Same file as the 4 conflicts | After the merge. Also touch the CFG default-pin test |
| `Column.extractColumn` argument swap | `Column.java` | **No** (upstream did not change `Column.java`) | Independent; group with metadata fixes for coherence, not for ordering |
| WR-03 (`HeaderToValue` collision contract), `JsonColumnTest` read-back, `ClickHouseSinkTaskJsonInsertFormatTest` try/finally, `ClickHouseBase.tearDown()` drop failure, stale Javadocs (Phase 11 IN-01, P4-IN-01) | transforms, tests, harness | No upstream edits to these files | Independent of the merge; can run in parallel with writer hardening. Do them after the sync anyway so the first full-suite run on the merged tree is not confounded by test edits |
| CI one-run-per-SHA, CI fork count | `.github/workflows/tests.yaml` | Not in upstream's v1.7.0 diff | Independent. Current key is `github.workflow-<pr number or github.sha>`; keying on `github.ref` too (or pushing tags separately) fixes the tag-cancels-branch-run problem |
| Dependency bumps (jackson 2.22.3, okhttp 5.5.0, org.json 20260814, mockito 5.24.0; lz4-java 1.10.4 arrives with the merge) | `gradle/libs.versions.toml` (a conflicted file) | Yes: the file the merge rewrites | After the merge and after hardening. Jackson sits on the R-01 serialisation path (`OBJECT_MAPPER`), so bumping it earlier would make a JSON-route regression ambiguous |
| `VERSION`, CHANGELOG, FORK-DELTA, rollout checklist | `VERSION` (a conflicted file) | Yes | Last |

## (e) Suggested build order (coarse, 4 phases, numbering continues from Phase 11)

```
Phase 12  Upstream v1.7.0 sync (merge + reconcile)
   |        gate: compile x4 source sets, testUnit, FULL live suite on latest x V2, upstream's 12 test files green
   v
Phase 13  Fork hardening on the merged tree (R-01, writer null-correctness, metadata/config fixes, test debt)
   |        gate: full live suite, FORK-DELTA rows re-grepped
   v
Phase 14  Dependency bumps + CI (concurrency key, fork count)
   |        gate: full suite after each bump group, one CI run per SHA
   v
Phase 15  Publication & rollout (VERSION, CHANGELOG, FORK-DELTA, merge to custom_build, tags, operator checklist)
```

### Phase 12: Upstream v1.7.0 sync

- Create rollback tag `pre-v1.7.0-sync` and scratch `sync/v1.7.0`; regenerate `conflicted-paths.txt`; parametrise the invariant script and add Invariants 1b and 3 (section b).
- **Commit 1, the merge commit:** resolve the 5 textual conflicts as specified in the hunk tables. Allowed extra edits inside this commit are only those that keep the tree self-consistent: delete the fork's duplicate lz4 capability block and its two stale constants. Every non-conflict auto-merge is listed in MERGE-CHECKLIST Section 2.
- **Commit 2, the by-intent reconcile:** scoped MAP guard (`&& col.isNullable()`), `setCompressionMethod` added to the CFG option-defaults pin, `docs/FORK-DELTA.md` rows (lz4 row retired in favour of upstream's flag; build row for the capability rule; dependency-pin row: client `0.9.6` is now upstream's own value, Kafka API `4.1.1` is the only remaining FORK OVERRIDE).
- **Gates, in this order:** (1) `testUnit` with Docker stopped, (2) the four compile tasks, (3) full `./gradlew test` on `latest` x V2 including upstream's new `nullMapTypeTest`, `nullMapTypesTest`, `nullMapTypeJsonInsertTest`, `testSetCompressionMethod_*`, `StructToJsonMapTest`, and the fork's `NetworkCompressionPin*`, (4) `./gradlew dependencyInsight --dependency lz4-java` recorded as a Recorded Fact, (5) a deliberate `KeeperStateProvider` read-path check: no standard test covers it. Options: run an exactly-once test against the cluster compose (`CLICKHOUSE_CLUSTER_NAME=one_shard_three_replicas`) or add a small `KeeperMap` live test that writes a state row and reads it back through `getStateRecord`.
- Take with the merge, do not adopt: #800 Avro unions, #801 buffering (`bufferCount` stays 0).
- Avoids: the "vanishing behaviour" and wrong-server failure modes from the last sync, and the false-green from the shadowed MAP branch.
- Stop point: human approval is not needed until Phase 15; keep the scratch branch unpushed or CI-only.

### Phase 13: Fork hardening on the merged tree

Order inside the phase: (1) R-01 routing and test flip (self-contained, decided already, sets the JSON-route baseline); (2) writer null-correctness WR-01 (tests only, probe says bytes are right) and WR-02 (needs the nested-vs-top-level discriminator; decide CR-02 TUPLE/VARIANT fate); (3) P9-IN-01, P3-WR-03 in the same file; (4) metadata and config fixes: P3-WR-01, `Column.extractColumn` swap, `USERNAME` default; (5) test debt (WR-03, `JsonColumnTest`, JSON-format try/finally, `tearDown()` drop handling, Javadocs). Items 1-3 serialise on `ClickHouseWriter`; 4 and 5 touch different files and can be planned as independent plans. Each fixes only what its test proves, and each is recorded in FORK-DELTA/CHANGELOG as the project's "genuine bug fix" rule requires. R-01 is a deliberate behaviour change and needs its own recorded decision (it was escalated at a blocking-human checkpoint in v1.2.9_gg4-tests and kept at v1.6.0).

### Phase 14: Dependency bumps and CI

- jackson 2.22.3, okhttp 5.5.0, org.json 20260814, mockito 5.24.0 in `libs.versions.toml`, one commit per group (runtime jackson separate from test-only okhttp/json/mockito), full suite after each. Not Kafka API, not client `0.10.0` (out of scope).
- `tests.yaml`: concurrency group so tag pushes do not cancel the branch run; read the real fork count from a log or record it as unmeasured.
- Independent of Phase 13's code, but sequenced after it so a dependency regression is attributable.

### Phase 15: Publication and rollout

- `VERSION=v1.7.0_gg1` (changes JMX `version=` again, D-07), CHANGELOG fork entry, final FORK-DELTA pass (Behaviour ledger, raw numbers, "Known costs" now without R-01 and WR-01), MERGE-CHECKLIST zero-pending assertion, invariants 1b/2/3 green over `pre-v1.7.0-sync..HEAD`.
- Non-force `--no-ff` merge of `sync/v1.7.0` into protected `custom_build` after recorded human approval; push branch and tags separately (so exactly one `tests.yaml` run exists per SHA); tags `v1.7.0_gg1`, `pre-v1.7.0-sync`; CI green on the published SHA; planning history published with the merge.
- Operator rollout checklist: MBean name `version=` change; `setCompressionMethod` default `true` and what `false` removes; **server response compression is now off**; R-01 now writes rows on pipelines that used to drop them; `lz4` forced setting is now flag-controlled; confirm no server profile constrains `network_compression_method` (T-11-38).

### Research flags

| Phase | Needs deeper research? |
|---|---|
| 12 | Standard, process is established. The open items are verification, not research: lz4 resolved version, `KeeperStateProvider` read path |
| 13 | **R-01 type fidelity** (Timestamp/Date/Decimal/bytes through the JSON route) and **WR-02 design** (how to discriminate top-level vs nested null in `doWriteColValue`) deserve a short spike before planning |
| 14 | Standard |
| 15 | Standard (mirrors Phase 11) |

## Anti-patterns for this milestone

- **"Keep both" on adjacent conflict hunks.** The `ClickHouseClientBuilder` setters share trailing lines and break if unioned mechanically.
- **Treating a clean `git merge` as proof of correct merge.** The MAP shadow merged without a marker. Only a byte-level probe or the live test finds it.
- **Taking upstream wholesale on the three Java files "to keep the commit mechanical".** It trades a 5-conflict merge for a 277-line re-application.
- **Declaring D-17 retired without checking the `Disabled` direction.** Upstream's flag-off path removes both the `lz4` pin and response compression; the fork's unit pins only exercise flag-on.
- **Bumping dependencies inside the sync commit.** It would hide whether a failure comes from upstream's changes or from the bump.
- **Relying on the standalone suite for exactly-once.** 11 `exactlyOnceStateMismatchTest` cases are skipped; `KeeperStateProvider` changed upstream.

## Sources

- `git diff --name-only v1.6.0 v1.7.0`, `git diff --shortstat v1.6.0 HEAD -- <path>` (all 22 paths), `git merge-tree --write-tree --name-only HEAD v1.7.0`, `git log v1.6.0..v1.7.0` (22 commits) (HIGH, measured)
- Real `git merge --no-commit v1.7.0` in a throwaway worktree plus Gradle `--offline` compile of all four source sets and `testUnit`; byte probes of `ClickHouseWriter.doWriteCol` for null/Nullable Map cases (HIGH for the outputs quoted; worktree removed, `git worktree list` clean)
- `docs/FORK-DELTA.md`, `.planning/MERGE-CHECKLIST.md`, `.planning/PROJECT.md`, `.planning/scripts/verify-merge-invariants.sh`, `.planning/scripts/conflicted-paths.txt` (HIGH, repo)
- Upstream v1.7.0 `CHANGELOG.md`, issues #820, #841, #800, #801 as cited there (MEDIUM, upstream's own description; not re-fetched)
- Not verified here: any Docker-backed test (no `docker` binary on this machine), the resolved `lz4-java` version, JSON-route date/decimal behaviour, ClickHouse's `Nullable(Map|Tuple|Variant)` restriction
